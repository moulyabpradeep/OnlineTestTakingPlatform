package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.data.bean.CandidateLoginDetail;
import com.merittrac.apollo.data.bean.ExcelsheetName;
import com.merittrac.apollo.data.repository.RpsBatchAcsAssociationRepository;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.rps.common.RpsConstants;

/**
 * @author mayank_g
 *
 */
public class CandidateDetailExportToExcel {

	@Autowired
	RpsBatchAcsAssociationRepository rpsBatchAcsAssociationRepository;

	static ApplicationContext ac = new ClassPathXmlApplicationContext("/META-INF/spring/applicationContext.xml");
	static RpsBatchAcsAssociationService rpsBatchAcsAssociationService = (RpsBatchAcsAssociationService) ac
			.getBean("rpsBatchAcsAssociationService");

	public File exportCandidateDetail(List<CandidateLoginDetail> candidateDetail, Integer batchAcsId)
			throws DocumentException, IOException {

		XSSFWorkbook workbook = new XSSFWorkbook();
		ExcelsheetName rpsBatchAcsAssociation = rpsBatchAcsAssociationService.findByBatchAcsId(batchAcsId);
		XSSFSheet spreadsheet = workbook.createSheet(rpsBatchAcsAssociation.getBatchName().toString());
		XSSFFont font5 = workbook.createFont();
		font5.setBold(true);
		MessagesReader messagesReader = new MessagesReader();

		XSSFCellStyle style7 = workbook.createCellStyle();
		style7.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style7.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style7.setFont(font5);
		spreadsheet.setColumnWidth(0, 5000);
		style7.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		style7.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		int column = RpsConstants.column;
		Cell headerCell = null;
		excelRowHeader(workbook, spreadsheet, headerCell, messagesReader, column, style7, batchAcsId);
		int i = RpsConstants.row_iteration1;
		for (CandidateLoginDetail candidateLoginDetail : candidateDetail) {

			XSSFRow row1 = spreadsheet.createRow((short) i);
			int column5 = RpsConstants.column5;
			Cell headerCell1;
			row1.setHeight((short) 500);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getUniqueCandidateId().toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getIpAddress().toString()), row1);
			StringBuffer candidateName = new StringBuffer("");
			if (!candidateLoginDetail.getFirstName().equals("NA"))
				candidateName.append(candidateLoginDetail.getFirstName());
			else
				candidateName.append("");

			if (!candidateLoginDetail.getMiddleName().equals("NA"))
				candidateName.append(" " + candidateLoginDetail.getMiddleName());
			else
				candidateName.append("");

			if (!candidateLoginDetail.getLastName().equals("NA"))
				candidateName.append(" " + candidateLoginDetail.getLastName());
			else
				candidateName.append("");
			headerCell1 = getNormalCell(column5++, (candidateName.toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getLoginID().toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getLoginTime().toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getTestStartTime().toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getTestEndTime().toString()), row1);
			if (candidateLoginDetail.getLoginTime() == RpsConstants.AnsNotAvailable)
				headerCell1 = getNormalCell(column5++, (RpsConstants.AnsNotAvailable.toString()), row1);
			else
				headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getResponseAvailable().toString()), row1);
			headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getQuestionsAttempted().toString()), row1);
			if (candidateLoginDetail.getLoginTime() == RpsConstants.AnsNotAvailable)
				headerCell1 = getNormalCell(column5++, RpsConstants.AnsNotAvailable.toString(), row1);
			else{
				headerCell1 = getNormalCell(column5++, (candidateLoginDetail.getCandidateScore().toString()), row1);
			}
			if(candidateLoginDetail.getTestEndTime()!=RpsConstants.AnsNotAvailable){
				headerCell1 = getNormalCell(column5++, (RpsConstants.completed.toString()), row1);	
			}
			else if(candidateLoginDetail.getTestStartTime()!=RpsConstants.AnsNotAvailable){
				headerCell1 = getNormalCell(column5++, (RpsConstants.started.toString()), row1);
			}
			else if(candidateLoginDetail.getLoginTime()!=RpsConstants.AnsNotAvailable){
				headerCell1 = getNormalCell(column5++, (RpsConstants.loggedIn.toString()), row1);
			}
			else
				headerCell1 = getNormalCell(column5++, RpsConstants.AnsNotAvailable.toString(), row1);

			i++;
		}
		try {
			String resultReport = System.getenv("APOLLO_HOME") + File.separator + "rps" + File.separator
					+ RpsConstants.REPORT_FOLDER + File.separator;
			if (!new File(resultReport).exists()) {
				FileUtils.forceMkdir(new File(resultReport));
			}
			resultReport = resultReport + "EXCEL_REPORT_" + Calendar.getInstance().getTimeInMillis() + ".xlsx";
			File resultExcel = new File(resultReport);
			FileOutputStream out = new FileOutputStream(resultExcel);
			workbook.write(out);
			out.close();
			return resultExcel;
		} catch (IOException e) {
			e.getMessage();
		}
		return null;
	}

	public static void excelRowHeader(XSSFWorkbook workbook, XSSFSheet spreadsheet, Cell headerCell,
			MessagesReader messagesReader, int column, XSSFCellStyle style7, Integer batchAcsId)
			throws DocumentException, IOException {
		
		XSSFRow row1 = spreadsheet.createRow((short) 0);
		ExcelsheetName rpsBatchAcsAssociation = rpsBatchAcsAssociationService.findByBatchAcsId(batchAcsId);
		headerCell = getNormalCell1(0, style7, (messagesReader.getProperty("EXPORT_EXCEL20")), row1);
		headerCell = getNormalCell1(1, style7, (rpsBatchAcsAssociation.getAcsServerName().toString()), row1);

		XSSFRow row = spreadsheet.createRow((short) 1);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL19")), row);
		spreadsheet.setColumnWidth(1, 4000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL8")), row);
		spreadsheet.setColumnWidth(1, 4000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL9")), row);
		spreadsheet.setColumnWidth(2, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL10")), row);
		spreadsheet.setColumnWidth(3, 5000);
		spreadsheet.setColumnWidth(4, 6000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL11")), row);
		spreadsheet.setColumnWidth(5, 6000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL12")), row);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL13")), row);
		spreadsheet.setColumnWidth(6, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL14")), row);
		spreadsheet.setColumnWidth(7, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL15")), row);
		spreadsheet.setColumnWidth(8, 6000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL16")), row);
		spreadsheet.setColumnWidth(7, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL21")), row);
		spreadsheet.setColumnWidth(7, 5000);
	}

	private static Cell getNormalCell(int createCell, String value, XSSFRow row) {
		Cell headerCell1;
		headerCell1 = row.createCell(createCell);
		headerCell1.setCellValue(value);
		// headerCell1.setCellStyle(style);
		return headerCell1;
	}

	private static Cell getNormalCell1(int createCell, CellStyle style, String value, XSSFRow row) {
		Cell headerCell1;
		headerCell1 = row.createCell(createCell);
		headerCell1.setCellValue(value);
		headerCell1.setCellStyle(style);
		return headerCell1;
	}
}
