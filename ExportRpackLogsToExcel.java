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

import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.data.bean.RpsAcsDetail;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.rps.common.RpsConstants;

/**
 * @author mayank_g
 *
 */
public class ExportRpackLogsToExcel {

	/**
	 * 
	 * @param acsDetail
	 * @param batchCode
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	public static File excelGenerator(List<RpsAcsDetail> acsDetail, String batchCode)
			throws DocumentException, IOException {

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet spreadsheet = workbook.createSheet(batchCode);
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

		excelRowHeader(workbook, spreadsheet, headerCell, messagesReader, column, style7);
		int i = RpsConstants.row_iteration1;
		for (RpsAcsDetail AcsDetail : acsDetail) {
			XSSFRow row1 = spreadsheet.createRow((short) i);
			int column5 = RpsConstants.column5;
			Cell headerCell1;
			row1.setHeight((short) 500);
			headerCell1 = getNormalCell(column5++, (AcsDetail.getScheduled().toString()), row1);
			headerCell1 = getNormalCell(column5++, (AcsDetail.getLoggedIn().toString()), row1);
			headerCell1 = getNormalCell(column5++, (AcsDetail.getStarted().toString()), row1);
			headerCell1 = getNormalCell(column5++, (AcsDetail.getResponseAvailable().toString()), row1);
			headerCell1 = getNormalCell(column5++, (AcsDetail.getAcsServerName().toString()), row1);
			
			headerCell1 = getNormalCell(column5++, (AcsDetail.getBatchAcsId().toString()), row1);
			if(AcsDetail.getQpPack()!=null)
			headerCell1 = getNormalCell(column5++, (RpsConstants.AnsAvailable.toString()), row1);
			else
				headerCell1 = getNormalCell(column5++, (RpsConstants.AnsNotAvailable).toString(), row1);
			if(AcsDetail.getBpack()!=null)
			headerCell1 = getNormalCell(column5++, (RpsConstants.AnsAvailable.toString()), row1);
			else
				headerCell1 = getNormalCell(column5++, (RpsConstants.AnsNotAvailable).toString(), row1);	
			headerCell1 = getNormalCell(column5++, (AcsDetail.getLastRecievedRpack().toString()), row1);
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
			MessagesReader messagesReader, int column, XSSFCellStyle style7) throws DocumentException, IOException {
        
		XSSFRow row = spreadsheet.createRow((short) 1);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL1")), row);
		spreadsheet.setColumnWidth(1, 4000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL2")), row);
		spreadsheet.setColumnWidth(2, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL17")), row);
		spreadsheet.setColumnWidth(2, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL3")), row);
		spreadsheet.setColumnWidth(3, 4000);
		spreadsheet.setColumnWidth(4, 4000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL4")), row);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL18")), row);

		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL5")), row);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL6")), row);
		spreadsheet.setColumnWidth(6, 5000);
		headerCell = getNormalCell1(column++, style7, (messagesReader.getProperty("EXPORT_EXCEL7")), row);
		spreadsheet.setColumnWidth(8, 6000);
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
