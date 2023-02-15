package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.util.CellRangeAddress;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.reports.behaviouralReports.BehaviouralExcelReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.BehaviouralParamtersEntity;
import com.merittrac.apollo.rps.common.RpsConstants;
/**
 * @author mayankg
 *
 */
public class excelReport 
{
	public static void excelGenerator(List<BehaviouralExcelReportEntity> behaviouralReportEntities, String fileName)
			throws DocumentException, IOException {
		

		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFFont font5= workbook.createFont();
		font5.setBold(true);
		MessagesReader messagesReader = new MessagesReader();
		XSSFSheet spreadsheet = workbook.createSheet("cell types");
		XSSFRow row = spreadsheet.createRow((short) 1);
		XSSFCellStyle style7= workbook.createCellStyle();
		style7.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style7.setFillPattern(CellStyle.SOLID_FOREGROUND);
		style7.setFont(font5);
		spreadsheet.setColumnWidth(0, 5000);
		style7.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		style7.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		int column1= RpsConstants.column16;
		Cell headerCell1;

		headerCell1 = row.createCell(column1++);

		headerCell1.setCellStyle(style7);
		spreadsheet.addMergedRegion(new CellRangeAddress(1, 1, 9,10));
		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER9"));


		row = spreadsheet.createRow((short)2);
		XSSFCellStyle style12= workbook.createCellStyle();
		style12.setFillForegroundColor(IndexedColors.WHITE.getIndex());
		style12.setFillPattern(CellStyle.SOLID_FOREGROUND);
		row.setHeight((short) 800);
		spreadsheet.setColumnWidth(0, 5000);
		style12.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		style12.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		int column4= RpsConstants.column17;
		Cell headerCell3;
		headerCell3 = row.createCell(column4++);
		headerCell3.setCellStyle(style7);
		
		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER25"));
		headerCell3 = row.createCell(column4++);
		spreadsheet.setColumnWidth(189,3500);
		headerCell3.setCellStyle(style7);
		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER26"));
		headerCell3 = row.createCell(column4++);
		spreadsheet.setColumnWidth(193,3500);
		headerCell3.setCellStyle(style7);
		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER27"));
		headerCell3 = row.createCell(column4++);
		headerCell3.setCellStyle(style7);
		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER28"));
		headerCell3 = row.createCell(column4++);
		headerCell3.setCellStyle(style7);
		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER29"));
		headerCell3 = row.createCell(column4++);
		headerCell3.setCellStyle(style7);

		headerCell3.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER30"));

		row = spreadsheet.createRow((short) 3);
		XSSFCellStyle style10= workbook.createCellStyle();
		style10.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style10.setFillPattern(CellStyle.SOLID_FOREGROUND);
		spreadsheet.setColumnWidth(0, 5000);
		style10.setAlignment(XSSFCellStyle.ALIGN_CENTER);
		style10.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		int column2=RpsConstants.column3;
		Cell headerCell2;
		headerCell2 = row.createCell(column2++);
		headerCell2.setCellStyle(style10);
		int startColomn1=RpsConstants.column3;
		int endColumn2=RpsConstants.column4;
		
		for(column2=startColomn1;column2<=endColumn2;){

			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++, startColomn1++));//(3,3,column2,column2++)
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3, startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
			spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,startColomn1++,startColomn1++));
           
			int startColomn7=RpsConstants.column18;
			int endColumn7=RpsConstants.column19;
			for(int i=startColomn7;i<=endColumn7;i++){

				headerCell1 = row.createCell(column2++);
				headerCell1.setCellStyle(style7);
				headerCell1.setCellValue( i/2);
			}
		}

		headerCell1 = row.createCell(158);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,158,163));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER18"));

		headerCell1 = row.createCell(164);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,164,169));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER19"));

		headerCell1 = row.createCell(170);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,170,175));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER20"));
		headerCell1 = row.createCell(176);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,176,181));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER21"));

		headerCell1 = row.createCell(182);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,182,187));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER22"));

		headerCell1 = row.createCell(188);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,188,193));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER23"));

		headerCell1 = row.createCell(194);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,194,199));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER24"));

		headerCell1 = row.createCell(200);
		spreadsheet.addMergedRegion(new CellRangeAddress(3, 3,200,203));
		headerCell1.setCellStyle(style7);

		headerCell1.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER_31"));


		row = spreadsheet.createRow((short) 4);
		XSSFCellStyle style6 = workbook.createCellStyle();
		XSSFFont font= workbook.createFont();
		font.setBold(true);
		style6.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
		style6.setFillPattern(CellStyle.SOLID_FOREGROUND);
		spreadsheet.setColumnWidth(0, 5000);
		style6.setAlignment(XSSFCellStyle.ALIGN_LEFT);
		style6.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		style6.setFont(font);
		int column = RpsConstants.column;
		Cell headerCell;
		row.setHeight((short) 500);
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style6);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER1"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(1, 5000);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER2"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(2, 3000);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER3"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(3, 3000);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER4"));
		spreadsheet.setColumnWidth(4, 3000);
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER5"));
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER6"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(6, 5000);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER7"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(7, 5000);
		headerCell.setCellStyle(style6);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER8"));


		XSSFCellStyle style8 = workbook.createCellStyle();
		XSSFFont font2= workbook.createFont();
		font2.setBold(true);
		style8.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		style8.setFillPattern(CellStyle.SOLID_FOREGROUND);
		spreadsheet.setColumnWidth(0, 5000);
		style8.setAlignment(XSSFCellStyle.ALIGN_LEFT);
		style8.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		style8.setFont(font);
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style8);
		XSSFCellStyle style9 = workbook.createCellStyle();
		XSSFFont font1= workbook.createFont();
		font1.setBold(true);
		style9.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
		style9.setFillPattern(CellStyle.SOLID_FOREGROUND);
		spreadsheet.setColumnWidth(0, 5000);
		style9.setAlignment(XSSFCellStyle.ALIGN_LEFT);
		style9.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		style9.setFont(font);
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style9);
		int startColomn3=RpsConstants.column6;
		int endColumn3=RpsConstants.column7;
		
		for(column=startColomn3;column<=endColumn3;){


			headerCell = row.createCell(column++);
			headerCell.setCellStyle(style8);
			headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER10"));
			headerCell = row.createCell(column++);
			headerCell.setCellStyle(style9);
			headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER11"));
		}

		XSSFCellStyle style11= workbook.createCellStyle();
		XSSFFont font6= workbook.createFont();
		font6.setBold(true);
		style11.setFillForegroundColor(IndexedColors.SKY_BLUE.getIndex());
		style11.setFillPattern(CellStyle.SOLID_FOREGROUND);
		//spreadsheet.setColumnWidth(161,3000);
		style11.setAlignment(XSSFCellStyle.ALIGN_LEFT);
		style11.setFont(font);
		style11.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
		headerCell = row.createCell(column++);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(158,1000);
		//spreadsheet.setColumnWidth(1, 1000);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(159,1000);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(160,1000);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(161,1000);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(162,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));

		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(163,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(164,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(165,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(166,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(167,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(168,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(169,1000);
		headerCell.setCellStyle(style11);



		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(170,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(171,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(172,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(173,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(174,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));

		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(175,1000);
		headerCell.setCellStyle(style11);



		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(176,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(177,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(178,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(179,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(180,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));

		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(181,1000);
		headerCell.setCellStyle(style11);



		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(182,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(183,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(184,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(185,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(186,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(187,1000);
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
	
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER12"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER13"));
		headerCell = row.createCell(column++);
	
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER14"));
		headerCell = row.createCell(column++);
	
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER15"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER16"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);

		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER17"));

		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER31"));
		headerCell = row.createCell(column++);
		
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER32"));
		headerCell = row.createCell(column++);
	
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER33"));
		headerCell = row.createCell(column++);
		spreadsheet.setColumnWidth(203,4000);
		headerCell.setCellStyle(style11);
		headerCell.setCellValue(messagesReader.getProperty("CGT_EXCEL_HEADER34"));




		int i=RpsConstants.row_iteration;
		for (BehaviouralExcelReportEntity behaviouralExcelReportEntity : behaviouralReportEntities) {

			row= spreadsheet.createRow((short) i);

			XSSFCellStyle style14 = workbook.createCellStyle();
			style14.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			style14.setFillPattern(CellStyle.SOLID_FOREGROUND);
			spreadsheet.setColumnWidth(0, 5000);
			style14.setAlignment(XSSFCellStyle.ALIGN_LEFT);
			style14.setVerticalAlignment(XSSFCellStyle.VERTICAL_TOP);
			int column5 =RpsConstants.column5;
			Cell headerCell5;
			row.setHeight((short) 500);
			
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getMerittracId() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getMerittracId());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getCandidateName() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getCandidateName());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getDob() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getDob());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getQpId() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getQpId());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getTestStartDate() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getTestStartDate());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getTestCenter() == null ? RpsConstants.NA
					: behaviouralExcelReportEntity.getTestCenter());
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getVerbalScore() == null ? RpsConstants.NA
					: String.valueOf(behaviouralExcelReportEntity.getVerbalScore()));
			headerCell = row.createCell(column5++);
			headerCell.setCellStyle(style6);
			headerCell.setCellValue(behaviouralExcelReportEntity.getAnalyticalScore() == null ? RpsConstants.NA
					: String.valueOf(behaviouralExcelReportEntity.getAnalyticalScore()));
			
			if (behaviouralExcelReportEntity.isFullResponse()) {
				Type aType = new TypeToken<ArrayList<String>>() {
				}.getType();

				String listOfCandidateResponses = behaviouralExcelReportEntity.getListOfCandidateResponses();
				List<String> listValues = new Gson().fromJson(listOfCandidateResponses, aType);

				int startColomn = RpsConstants.column1;
				int endColumn = RpsConstants.column2;


				for (column = startColomn; column <= endColumn;) {

					for (String responseList : listValues) {

						if (responseList.endsWith("A")) {

							headerCell = row.createCell(column++);
							headerCell.setCellStyle(style6);
							headerCell.setCellValue("1");
						} else {

							headerCell = row.createCell(column++);
							headerCell.setCellStyle(style6);
							headerCell.setCellValue(" ");
						}
						if (responseList.endsWith("B")) {
							headerCell = row.createCell(column++);
							headerCell.setCellStyle(style6);
							headerCell.setCellValue("1");

						} else {
							headerCell = row.createCell(column++);
							headerCell.setCellStyle(style6);
							headerCell.setCellValue(" ");

						}

					}

				}
				Type pType = new TypeToken<ArrayList<String>>() {
				}.getType();
				Type paramType = new TypeToken<HashMap<String, Integer>>() {
				}.getType();

				String cgtUniqCodesJson = behaviouralExcelReportEntity.getMapSummationValuesToUniqCodes();
				Map<String, Integer> mapForUniqueCodes = new Gson().fromJson(cgtUniqCodesJson, paramType);
				String cgtUniqCodesSummation = messagesReader.getProperty("CGT_UNIQUE_CODES_SUMMATION");
				List<String> listValues1 = new Gson().fromJson(cgtUniqCodesSummation, pType);
				int startColomn4 = RpsConstants.column8;
				int endColumn4 = RpsConstants.column9;

				for (column = startColomn4; column <= endColumn4;) {

					for (String listValue1 : listValues1) {
						headerCell = row.createCell(column++);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(mapForUniqueCodes.get(listValue1));
					}

				}
				Type sumType = new TypeToken<HashMap<String, Integer>>() {
				}.getType();
				Type pType1 = new TypeToken<ArrayList<String>>() {
				}.getType();
				String sumValues = behaviouralExcelReportEntity.getMapSummationValuesToParam();
				Map<String, Integer> mapSummationValuesToParam = new Gson().fromJson(sumValues, sumType);
				String cgtUniqCodesSummation1 = messagesReader.getProperty("CGT_PARAMNAME_TO_PARAMCODE1");
				List<String> listValues2 = new Gson().fromJson(cgtUniqCodesSummation1, pType1);
				int startColomn5 = RpsConstants.column10;
				int endColumn5 = RpsConstants.column11;

				for (column = startColomn5; column <= endColumn5;) {
					for (String listValue1 : listValues2) {
						headerCell = row.createCell(column++);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(mapSummationValuesToParam.get(listValue1));
					}
				}
				Type pType2 = new TypeToken<ArrayList<String>>() {
				}.getType();
				Type sdType = new TypeToken<HashMap<String, Double>>() {
				}.getType();
				String stdValues = behaviouralExcelReportEntity.getMapStdDevValuesToParam();
				Map<String, Double> mapStdDevValuesToParam = new Gson().fromJson(stdValues, sdType);
				String cgtUniqCodesSummation2 = messagesReader.getProperty("CGT_PARAMNAME_TO_PARAMCODE1");
				List<String> listValues3 = new Gson().fromJson(cgtUniqCodesSummation1, pType2);
				int startColomn6 = RpsConstants.column12;
				int endColumn6 = RpsConstants.column13;

				for (column = startColomn6; column <= endColumn6;) {
					for (String listValue1 : listValues3) {
						headerCell = row.createCell(column++);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(mapStdDevValuesToParam.get(listValue1));
					}
				}


				List<BehaviouralParamtersEntity> behaviouralParamtersEntities =
						behaviouralExcelReportEntity.getBehaviouralParamtersEntities();
				for (BehaviouralParamtersEntity behaviouralParamtersEntity : behaviouralParamtersEntities) {



					if (behaviouralParamtersEntity.getRankingOnParam() == 1) {
						String code1 = (behaviouralParamtersEntity.getCodeOfParam());
						headerCell = row.createCell(column++);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(code1);
					}
					if (behaviouralParamtersEntity.getRankingOnParam() == 2) {
						String code2 = (behaviouralParamtersEntity.getCodeOfParam());
						headerCell = row.createCell(column++);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(code2);
					}
					if (behaviouralParamtersEntity.getRankingOnParam() == 3) {
						String code3 = (behaviouralParamtersEntity.getCodeOfParam());
						int startColomn7 = RpsConstants.column14;
						headerCell = row.createCell(startColomn7);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(code3);
					}


				}
				StringBuilder code4 = new StringBuilder();
				for (BehaviouralParamtersEntity behaviouralParamtersEntity1 : behaviouralParamtersEntities) {
					if (behaviouralParamtersEntity1.getRankingOnParam() <= 3) {
						code4 = code4.append(behaviouralParamtersEntity1.getCodeOfParam());
						int startColomn8 = RpsConstants.column15;
						String code5 = code4.toString();
						headerCell = row.createCell(startColomn8);
						headerCell.setCellStyle(style6);
						headerCell.setCellValue(code5);
						;
					}
				}
			}
			else {
				column = 8;
				while (column < 204) {
					headerCell = row.createCell(column++);
					headerCell.setCellStyle(style6);
					headerCell.setCellValue("NA");
				}
			}
			i++;
		}
		FileOutputStream out = new FileOutputStream(
				new File(fileName));
		workbook.write(out);
		out.flush();
		out.close();

	}           
}	
