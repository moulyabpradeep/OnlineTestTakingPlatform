package com.merittrac.apollo.rps.services;

import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.service.*;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.ui.entity.AttendanceReconcileDataEntity;
import com.merittrac.apollo.rps.ui.entity.ExportFileEntity;
import com.merittrac.apollo.rps.ui.entity.ReconciliationGridEntity;
import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

public class ExcelExportService {

    @Autowired
    RpsEventService rpsEventService;

    @Autowired
    RpsAttendanceReconciliationService rpsAttendanceReconciliationService;

    @Autowired
    RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

    @Autowired
    RpsCustomerService rpsCustomerService;

    @Autowired
    RpsBatchService rpsBatchService;

    @Autowired
    RpsDivisionService rpsDivisionService;

    private static Logger logger = LoggerFactory.getLogger(ExcelExportService.class);

    public ExportFileEntity exportReconciliationGridData(String acsCode,
                                                         String eventCode, String batchDate) {

        ExportFileEntity exportFileEntity = null;
        boolean isEverySheetEmpty = true;
        final HSSFWorkbook workbook = new HSSFWorkbook();
        String userHome = System.getProperty(RpsConstants.USER_HOME);
        File folder = new File(userHome + File.separator + RpsConstants.ATTENDANCE_RECONCILIATION);
        if (!folder.exists())
            folder.mkdirs();
        final File fileObj = new File(folder + File.separator + RpsConstants.ATTENDANCE_RECONCILIATION + Calendar.getInstance().getTimeInMillis() + RpsConstants.XLS);
        List<AttendanceReconcileDataEntity> attendanceReconcileDataEntityList = rpsAttendanceReconciliationService.getReconciliationGridData(acsCode, batchDate);

        if (attendanceReconcileDataEntityList == null || attendanceReconcileDataEntityList.isEmpty()) {
            logger.warn("Attendance Reconciliation grid has no data to be exported for acsCode: " + acsCode + " for date range {" + batchDate + "}");
            exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "Attendance Reconciliation Grid has no data to be exported", null);
            return exportFileEntity;
        }

        RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
        CellStyle style = workbook.createCellStyle();
        for (AttendanceReconcileDataEntity attendanceReconcileDataEntity : attendanceReconcileDataEntityList) {

            String batchCode = attendanceReconcileDataEntity.getBatchCode();
            Sheet sheet = workbook.createSheet(this.getUniqueSheetName(batchCode));
            // get header from workbook's sheet
            CellStyle style1 = workbook.createCellStyle();
            // Create a cell and put a value in it.
            int rowNum = getHeading(rpsEvent, batchCode, acsCode, sheet, style1, workbook);
            //leave one row as blank
            rowNum++;
            Row headerRow = sheet.createRow(++rowNum);
            this.setHeaders(headerRow, style);
            List<ReconciliationGridEntity> reconciliationGridEntities = attendanceReconcileDataEntity.getReconciliationGridEntities();
            if (reconciliationGridEntities != null && !reconciliationGridEntities.isEmpty()) {
                this.setContents(reconciliationGridEntities, sheet, rowNum, style);
                isEverySheetEmpty = false;
            }

        }

        // if all the sheets have no data
        if (isEverySheetEmpty) {
            logger.warn("Attendance Reconciliation grid has no data to be exported for acsCode: " + acsCode + " for date {" + batchDate + "}");
            exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "Attendance Reconciliation Grid has no data to be exported", null);
            return exportFileEntity;
        }
        // write into the file
        return writeDataToFile(fileObj, workbook);
    }

    public File exportAttendanceLogsToExcel(List<AttendanceReportEntity> attendanceReportEntities, Integer batchAcsId) {
        RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.find(batchAcsId);
        RpsEvent rpsEvent = rpsBatchService.getEventByBatch(rpsBatchAcsAssociation.getRpsBatch());
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet(rpsBatchAcsAssociation.getRpsBatch().getBatchName());
        HSSFFont font = workbook.createFont();
        font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
        HSSFCellStyle style = workbook.createCellStyle();
        int rowNum = getHeading(rpsEvent, rpsBatchAcsAssociation.getRpsBatch().getBatchCode(),
                rpsBatchAcsAssociation.getRpsAcsServer().getAcsServerId(), sheet, style, workbook);
        // Create a cell and put a value in it.
        style.setFont(font);
        rowNum = rowNum + 2;

        HSSFRow headerRow = sheet.createRow(rowNum++);
        int cellNum = 0;

        for (String obj : RpsConstants.ATTENDANCE_LOGS_COLUMNS) {
            sheet.setColumnWidth(cellNum, 5000);
            Cell cell = headerRow.createCell(cellNum++);
            cell.setCellValue(obj);
            cell.setCellStyle(style);
        }

        for (AttendanceReportEntity attendanceReportEntity : attendanceReportEntities) {
            HSSFRow row = sheet.createRow(rowNum);
            cellNum = 0;
            if (attendanceReportEntity.getCandidateIdentifier() != null) {
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getCandidateIdentifier());
            } else
                row.createCell(cellNum++).setCellValue("   ");
            if (attendanceReportEntity.getLoginID() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getLoginID());
            else
                row.createCell(cellNum++).setCellValue(" ");
            if (attendanceReportEntity.getAssessmentCode() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getAssessmentCode());
            else
                row.createCell(cellNum++).setCellValue("   ");
            if (attendanceReportEntity.getLoginTime() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getLoginTime());
            else
                row.createCell(cellNum++).setCellValue("   ");
            if (attendanceReportEntity.getIpAddress() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getIpAddress());
            else
                row.createCell(cellNum++).setCellValue("   ");
            if (attendanceReportEntity.getActualTestEndTime() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getTestStartTime());
            else
                row.createCell(cellNum++).setCellValue("   ");
            if (attendanceReportEntity.getIsPresent() != null)
                row.createCell(cellNum++).setCellValue(attendanceReportEntity.getIsPresent());
            else
                row.createCell(cellNum++).setCellValue("   ");
            rowNum++;
        }

        try {
            String resultReport = System.getenv("APOLLO_HOME") + File.separator + "rps" + File.separator
                    + RpsConstants.REPORT_FOLDER + File.separator;
            if (!new File(resultReport).exists()) {
                FileUtils.forceMkdir(new File(resultReport));
            }
            resultReport = resultReport + "AUDIT_REPORT_" +
                    Calendar.getInstance().getTimeInMillis() + ".xls";
            File resultExcel = new File(resultReport);
            FileOutputStream out =
                    new FileOutputStream(resultExcel);
            workbook.write(out);
            out.close();
            logger.info("Excel written successfully.. to location :-" + resultReport);
            return resultExcel;
        } catch (IOException e) {
            logger.error("Error during excel processing = " + e.getMessage());
            e.getMessage();
        }

        return null;
    }

    private void setContents(
            List<ReconciliationGridEntity> reconciliationGridEntities,
            Sheet sheet, int rowNum, CellStyle style) {

        int totalColumn = 0;
        for (ReconciliationGridEntity reconciliationGridEntity : reconciliationGridEntities) {
            Row row = sheet.createRow(++rowNum);
            int cellnum = 0;
            Cell cell = null;
            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getCandidateScheduledID());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getCandidatePresentID());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getCandidateAssessedID());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getStatus());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getAssessmentID());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getAssessmentSetID());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getLoginTime());

            cell = row.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(reconciliationGridEntity.getLogoutTime());

            totalColumn = cellnum;
        }
        //make all the columns auto size
        for (int column = 0; column < totalColumn; column++)
            sheet.autoSizeColumn(column);

    }


    private void setHeaders(Row headerRow, CellStyle style) {

        int cellnum = 0;
        String[] headerLabelArray = {RpsConstants.CANDIDATE_SCHEDULED, RpsConstants.CANDIDATE_PRESENT, RpsConstants.CANDIDATE_ASSESSED, RpsConstants.STATUS, RpsConstants.ASSESSMENT_ID, RpsConstants.ASSESSMENT_SET_ID
                , RpsConstants.LOGIN_TIME, RpsConstants.LOGOUT_TIME};

        Cell cell = null;
        for (String headerLabel : headerLabelArray) {
            cell = headerRow.createCell(cellnum++);
            setThinBorderStyle(cell, style);
            cell.setCellValue(headerLabel);
        }


    }


    private String getUniqueSheetName(String batchCode) {
        String uniqueName = "";
        if (batchCode.length() > 31) {
            uniqueName = batchCode.substring(0, 13);
            uniqueName = uniqueName.concat(".....");
            uniqueName = uniqueName.concat(batchCode.substring(batchCode.length() - 13, batchCode.length()));
        } else
            uniqueName = batchCode;

        return uniqueName;
    }

    private int getHeading(RpsEvent rpsEvent, String batchCode,
                           String acsCode, Sheet sheet, CellStyle style, HSSFWorkbook workbook) {

        sheet.setDefaultRowHeight((short) 300);
        Font font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName("Courier New");
        font.setBoldweight((short) 4);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);

        RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(rpsEvent.getRpsDivision().getDivisionCode());

        short rowNum = 0;
        String seperator = " :-";
        Row head = sheet.createRow(rowNum++);
        head.createCell(0).setCellValue(RpsConstants.MERITTRAC);
        head.getCell(0).setCellStyle(style);

        Row row = sheet.createRow(++rowNum);
        row.createCell(0, CellStyle.ALIGN_LEFT).setCellValue(
                RpsConstants.CUSTOMER_CODE + seperator);
        row.getCell(0).setCellStyle(style);
        row.createCell(1, CellStyle.ALIGN_LEFT).setCellValue(
                rpsDivision.getRpsCustomer().getCustomerCode());
        row.getCell(1).setCellStyle(style);

        row = sheet.createRow(++rowNum);
        row.createCell(0, CellStyle.ALIGN_LEFT).setCellValue(
                RpsConstants.DIVISION_CODE + seperator);
        row.createCell(1, CellStyle.ALIGN_LEFT).setCellValue(
                rpsDivision.getDivisionCode());

        row = sheet.createRow(++rowNum);
        row.createCell(0, CellStyle.ALIGN_LEFT).setCellValue(
                RpsConstants.EVENT_CODE + seperator);
        row.getCell(0).setCellStyle(style);
        row.createCell(1, CellStyle.ALIGN_LEFT).setCellValue(
                rpsEvent.getEventCode());
        row.getCell(1).setCellStyle(style);

        row = sheet.createRow(++rowNum);
        row.createCell(0, CellStyle.ALIGN_LEFT).setCellValue(
                RpsConstants.ACS_CODE + seperator);
        row.createCell(1, CellStyle.ALIGN_LEFT).setCellValue(acsCode);

        row = sheet.createRow(++rowNum);
        row.createCell(0, CellStyle.ALIGN_LEFT).setCellValue(
                RpsConstants.BATCH_CODE + seperator);
        row.getCell(0).setCellStyle(style);
        row.createCell(1, CellStyle.ALIGN_LEFT).setCellValue(batchCode);
        row.getCell(1).setCellStyle(style);

        return rowNum;
    }

    private static void setThinBorderStyle(Cell cell, CellStyle style) {
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.BLUE.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.BLUE.getIndex());
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.BLUE.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.BLUE.getIndex());
        cell.setCellStyle(style);
    }

    // write to the file
    public static synchronized ExportFileEntity writeDataToFile(File fileObj,
                                                                Workbook workbook) {

        ExportFileEntity exportFileEntity = null;
        try {
            FileOutputStream out = new FileOutputStream(fileObj);
            workbook.write(out);
            out.close();
            logger.info("Excel written successfully..");
            exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, fileObj.getAbsolutePath());
        } catch (Exception e1) {
            logger.error("Error During Creation of File" + e1);
            exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e1.getLocalizedMessage(), null);
        }

        return exportFileEntity;
    }

    @PreDestroy
    public void close() {
        this.rpsAttendanceReconciliationService = null;
        this.rpsBatchAcsAssociationService = null;
        this.rpsCustomerService = null;
        this.rpsBatchService = null;
        this.rpsDivisionService = null;
    }

}
