package com.merittrac.apollo.rps.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.qtitools.qti.node.item.AssessmentItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.cembeans.AssessmentBean;
import com.merittrac.apollo.common.cembeans.CustomerBean;
import com.merittrac.apollo.common.cembeans.DivisionBean;
import com.merittrac.apollo.common.cembeans.EventBean;
import com.merittrac.apollo.qpdqbmcommonlib.SoeRequestEntity;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.answerkeyupload.AnswerKeyDetails;
import com.merittrac.apollo.rps.common.answerkeyupload.AnswerKeyUpdateActions;
import com.merittrac.apollo.rps.common.answerkeyupload.PersistAnswerKeys;
import com.merittrac.apollo.rps.common.answerkeyupload.QuestionTypesDefined;
import com.merittrac.apollo.rps.jqti.dao.JQTIQuestionEntity;
import com.merittrac.apollo.rps.jqti.utility.JQTIApolloUtility;
import com.merittrac.apollo.rps.jqti.utility.JQTIUtility;

import au.com.bytecode.opencsv.CSVReader;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;

public class ReadAnswerKeysService implements IReadAnswerKeysService {
    private Logger LOGGER = LoggerFactory.getLogger(ReadAnswerKeysService.class);

    @Autowired
    AnswerKeyUpdateActions answerKeyUpdateActions;

    @Autowired
    PersistAnswerKeys persistAnswerKeys;

    @Autowired
    JQTIUtility jqtiUtility;

    @Autowired
    JQTIApolloUtility jqtiApolloUtility;

    @Value("${apollo_home_dir}")
    private String apolloHome;

    @Value("${isSOETextupload}")
    private String isSOETextupload;

    public ReadAnswerKeysService() throws IOException {
        super();
    }

    public List<String> readFile(String fileName, String eventCode, String[] assessmentCodes,
                                 String actionCode, String password, boolean isRemovalApplicable) throws IOException {
        List<String> errorList = null;
        LOGGER.info("File to Read " + fileName);
        String filePath = "";
        try {
            filePath = FilenameUtils.separatorsToSystem(this.getAnswerKeyFilePath(fileName, password));
        } catch (RpsException e) {
            LOGGER.info("ERROR :: while extracting the zip file" + e);
            errorList = new ArrayList<String>();
            errorList.add("FAILED :: Either pack is corrupted or password is incorrect");
            return errorList;
        }
        String fileExtn = GetFileExtension(filePath);
        fileExtn = fileExtn.toUpperCase();
        switch (fileExtn) {
            case "CSV":
                errorList = readCSV(filePath, eventCode, actionCode, assessmentCodes, isRemovalApplicable);
                break;
            case "XLS":
            case "XLSX":
                errorList = readExcel(filePath, eventCode, actionCode, assessmentCodes, isRemovalApplicable);
                break;
            case "JSON":
                errorList = readJsonWithXML(filePath, eventCode, actionCode, assessmentCodes, isRemovalApplicable);
                break;
            default:
                //add logger for file format not supported
                LOGGER.error(fileExtn + ": This file format is not supported");
        }
		String tempFolder = apolloHome + File.separator + "RPSTemp";
		if (new File(tempFolder).isDirectory() || new File(tempFolder).isFile()) {
			FileUtils.deleteQuietly(new File(tempFolder));
        }
        return errorList;
    }

    /**
     * @param filePath
     * @param eventCode
     * @param actionCode
     * @param assessmentCodes
     * @return
     */
    private List<String> readJsonWithXML(String filePath, String eventCode,
                                         String actionCode, String[] assessmentCodes, boolean isRemovalApplicable) {
        // TODO Auto-generated method stub
        List<String> list = new ArrayList<String>();
        try {
            int lastIndex = filePath.lastIndexOf("/");
            if (lastIndex < 0)
                lastIndex = filePath.lastIndexOf("\\");

            String questionPoolPath = filePath.substring(0, lastIndex);
            String questionZipFilePath = "";
            File questionPoolpathFile = new File(questionPoolPath);

//			String questionPoolFolderName = "";
            boolean isZipAvailable = false;
            for (String fileName : questionPoolpathFile.list()) {
                if (fileName.contains(".zip")) {
                    questionZipFilePath = questionPoolPath + File.separator + fileName;
//					questionPoolFolderName = fileName.substring(0, fileName.lastIndexOf(".zip"));
//					isZipAvailable = true;
                }/*else
                    questionPoolPath = questionPoolPath + RpsConstants.FileDelimiter + fileName;*/
            }

			/*if(!isZipAvailable)
            {
				File questionPoolpathFile2 = new File(questionPoolPath);
				for(String fileName : questionPoolpathFile2.list())
				{
					if(fileName.contains(".zip"))
					{
						questionZipFilePath = questionPoolPath + RpsConstants.FileDelimiter + fileName;
						questionPoolFolderName = fileName.substring(0, fileName.lastIndexOf(".zip"));
					}
				}				
			}*/
//			SoeRequestEntity soeRequestEntity = new Gson().fromJson(new FileReader(filePath), SoeRequestEntity.class);
            SoeRequestEntity soeRequestEntity = this.getObjectFromJSON(filePath);
            //getting the CEM details from metadata json file.

            CustomerBean customerBean = soeRequestEntity.getCustomer();
            String customerCode = customerBean.getCustomerCode();
            DivisionBean divisionBean = customerBean.getDivisionBean();
            String divisionCode = divisionBean.getDivisionCode();
            EventBean eventBean = divisionBean.getEventBean();
            String evntCode = eventBean.getEventCode();
            AssessmentBean assessmentBean = eventBean.getAssessmentBean();
            String assessmentCode = assessmentBean.getAssessmentCode();

			/*if(!assessmentCodes[0].contains(assessmentCode))
            {
				LOGGER.info("AssessmentCode is incorrect in Json file, AssessmentCode :: {} " + assessmentCode);
				list.add("AssessmentCode is incorrect in Json file, AssessmentCode :: {} " + assessmentCode);
				return list;
			}*/

			String localImagePath = apolloHome + File.separator + "rps";
            localImagePath = localImagePath + customerCode + File.separator + divisionCode + File.separator + evntCode + File.separator + assessmentCode;

            List<Integer> questionIdList = soeRequestEntity.getQuestionIds();

            ZipFile zipFile = new ZipFile(questionZipFilePath);
            zipFile.extractAll(questionPoolPath);

//			questionPoolPath = questionPoolPath + RpsConstants.FileDelimiter + questionPoolFolderName;
            List<AnswerKeyDetails> answerKeyList = new ArrayList<AnswerKeyDetails>();
            for (Integer qID : questionIdList) {
                String questionID = "Q" + qID + ".xml";
                localImagePath = FilenameUtils.separatorsToSystem(localImagePath + File.separator + questionID + "images");
                JQTIQuestionEntity jqtiQuestionEntity = this.readJQTIXML(FilenameUtils.separatorsToSystem(questionPoolPath + File.separator + questionID), localImagePath);
                AnswerKeyDetails answerKeyDetails = new AnswerKeyDetails();
                answerKeyDetails.setAssessmentCode(assessmentCode);
                answerKeyDetails.setQuestionID(jqtiQuestionEntity.getQpCode());
                answerKeyDetails.setAnswerKey(jqtiQuestionEntity.getResponse());
                //checking SOEText should be update into database
                if (isSOETextupload.equalsIgnoreCase("yes")) {
                    answerKeyDetails.setSoeText(jqtiQuestionEntity.getText());
                }

				QuestionType questionType = QuestionType.DESCRIPTIVE_QUESTION;
                answerKeyDetails.setQuestionType(questionType);
                answerKeyList.add(answerKeyDetails);

            }
            list = persistAnswerKeys.updateAnswerKeys(answerKeyList, eventCode, actionCode, new String[]{assessmentCode}, isRemovalApplicable);

        } catch (JsonSyntaxException e) {
            LOGGER.info("Metada syntax ERROR :: {} " + e);
            list.add("Metadata Syntax Problem");
        } catch (JsonIOException e) {
            LOGGER.info("Metada syntax ERROR :: {} " + e);
            list.add("Metadata Syntax Problem");
        } catch (FileNotFoundException e) {
            LOGGER.info("ERROR :: metadata is not available in pack" + e);
            list.add("Metadata Json file is not available in pack");
            return list;
        } catch (ZipException e) {
            LOGGER.info("ERROR :: while extracting question pool" + e);
            list.add("Either Question Pool is corrupted or not a proper format");
            return list;
        } catch (RpsException e) {
            LOGGER.info("ERROR :: While reading the JQTI Xml file." + e);
            list.add("JQTI xml is not proper format");
            return list;
        } catch (Exception e) {
            LOGGER.info("ERROR :: " + e);
            list.add("JQTI xml is not proper format");
            return list;
        }

        return list;
    }

    public SoeRequestEntity getObjectFromJSON(String fileName) throws IOException {
        SoeRequestEntity soeRequestEntity = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(fileName));
            Gson gson = new GsonBuilder().create();
            soeRequestEntity = gson.fromJson(bufferedReader, SoeRequestEntity.class);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (bufferedReader != null)
            bufferedReader.close();
        return soeRequestEntity;

    }

    private JQTIQuestionEntity readJQTIXML(String questionPath, String localImagePath) throws RpsException {

        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getDescriptiveQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
        return jqtiQuestionEntity;
    }

    public JQTIQuestionEntity readJQTIXMLForQuestionType(QuestionType questionType,
                                                         String questionPath, String localImagePath) throws RpsException {
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        JQTIQuestionEntity jqtiQuestionEntity = null;
        switch (questionType) {
            case MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION:
                jqtiQuestionEntity = jqtiApolloUtility.getDataEntryQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
                break;
            case DATA_ENTRY:
                jqtiQuestionEntity = jqtiApolloUtility.getDataEntryQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
                break;
            case FILL_IN_THE_BLANK:
                jqtiQuestionEntity = jqtiApolloUtility.getFillInTheBlankQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
                break;
        }
        return jqtiQuestionEntity;
    }

    public List<JQTIQuestionEntity> readJQTIXMLForMultipleQuestionType(QuestionType questionType,
                                                                       String questionPath, String localImagePath) throws RpsException {
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        List<JQTIQuestionEntity> jqtiQuestionEntities = null;
        switch (questionType) {
            case READING_COMPREHENSION:
                jqtiQuestionEntities = jqtiApolloUtility.getReadingComprehensionInfo(assessmentItem);
                break;
            case SURVEY:
                jqtiQuestionEntities = jqtiApolloUtility.getReadingComprehensionInfo(assessmentItem);

                break;
        }
        return jqtiQuestionEntities;
    }

    public JQTIQuestionEntity readJQTIXMLForQuestionType(String questionPath, String localImagePath) throws RpsException {
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        JQTIQuestionEntity jqtiQuestionEntity = null;
        if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.name()))
            jqtiQuestionEntity = jqtiApolloUtility.getMCQWithWeightedOptionInfo(assessmentItem);
        else if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.DATA_ENTRY.name()))
            jqtiQuestionEntity = jqtiApolloUtility.getDataEntryQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
        else if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.FILL_IN_THE_BLANK.name()))
            jqtiQuestionEntity = jqtiApolloUtility.getFillInTheBlankQuestionInfo(assessmentItem, questionPath, new File(localImagePath));
        return jqtiQuestionEntity;
    }

    public List<JQTIQuestionEntity> readJQTIXMLForMultipleQuestionType(String questionPath) throws RpsException {
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        List<JQTIQuestionEntity> jqtiQuestionEntity = null;
        if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.READING_COMPREHENSION.name()))
            jqtiQuestionEntity = jqtiApolloUtility.getReadingComprehensionInfo(assessmentItem);
        else if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.SURVEY.name()))
            jqtiQuestionEntity = jqtiApolloUtility.getReadingComprehensionInfo(assessmentItem);

        return jqtiQuestionEntity;
    }

    private List<String> readCSV(String fileName, String eventCode,
                                 String actionCode, String[] assessmentCodes, boolean isRemovalApplicable) throws IOException {
        // TODO Auto-generated method stub
        List<AnswerKeyDetails> answerKeyList = new ArrayList<AnswerKeyDetails>();
        CSVReader csvReader = new CSVReader(new FileReader(fileName));
        String[] header = csvReader.readNext();
        if (header == null) {
            throw new RuntimeException("no header in CSV file");
        }
        String[] row;
        AnswerKeyDetails answerKey = null;
        while ((row = csvReader.readNext()) != null) {
            answerKey = new AnswerKeyDetails();

            //set question ID
            answerKey.setQuestionID(row[0].trim());

            //set question types
            String quesType = row[1].trim();
            QuestionType questionType = QuestionTypesDefined.getQuestionType(quesType);
            answerKey.setQuestionType(questionType);

            //set answer key
            answerKey.setAnswerKey(row[2].trim());

            //add questions to list
            answerKeyList.add(answerKey);

        }
        return persistAnswerKeys.updateAnswerKeys(answerKeyList, eventCode, actionCode, assessmentCodes, isRemovalApplicable);
    }

    @Override
    public void removeAllKeys(String eventCode, String[] assessmentCodes) {
        for (String assessmentCode : assessmentCodes)
            answerKeyUpdateActions.removeAllAnswerKeys(assessmentCode);

    }

    public List<String> readExcel(String fileName, String eventCode, String actionCode, String[] assessmentCodes, boolean isRemovalApplicable) throws IOException {
        String fileExtn = GetFileExtension(fileName);
        Workbook wb_xssf = null; //Declare XSSF WorkBook for XLSX docs
        Workbook wb_hssf = null; //Declare HSSF WorkBook for XLS docs
        Sheet sheet = null; // sheet can be used as common for XSSF and HSS WorkBook
        InputStream inp = new FileInputStream(fileName);

        if (fileExtn.equalsIgnoreCase("xlsx")) {
            wb_xssf = new XSSFWorkbook(inp);

            //log("xlsx="+wb_xssf.getSheetName(0));
            sheet = wb_xssf.getSheetAt(0);
        }

        if (fileExtn.equalsIgnoreCase("xls")) {

            POIFSFileSystem fs = null;
            fs = new POIFSFileSystem(inp);
            wb_hssf = new HSSFWorkbook(fs);
            sheet = wb_hssf.getSheetAt(0);

        }

        Iterator<Row> rowsIt = sheet.rowIterator(); // Now we have rows ready from the sheet
        //Avoiding CEM Information from Excel

			/*try {
                this.avoidingCEMDetail(rowsIt, assessmentCodes);
			} catch (RpsException e) {
				LOGGER.info("ERROR:: AssessmentCode is Incorrect in Excel file" + e);
				List<String> list = new ArrayList<String>();
				list.add("AssessmentCode is Incorrect in Excel file, AssessmentCode :: {}"
						+ assessmentCodes[0]);
				return list;
			}*/

        //read header labels
        //Row headerRow= (Row) rowsIt.next();
        String assessmentCode = getAssessmentCode(rowsIt);
        List<String> errorList;
        if (!Arrays.asList(assessmentCodes).contains(assessmentCode)) {
            errorList = new ArrayList<>();
            errorList.add("Invalid Key for Assessment");
            return errorList;
        }
        Map<String, Integer> keyHeaders = readHeaderLabels(rowsIt);
        //read excel data
        errorList = readExcelData(rowsIt, keyHeaders, eventCode, actionCode, new String[]{assessmentCode}, isRemovalApplicable);
        inp.close();

        return errorList;

    }

    private void avoidingCEMDetail(Iterator<Row> rowsIt, String[] assessmentCodes) throws RpsException {
        String cellValue = "";
        String assessmentCode = "";
        while (rowsIt.hasNext()) {
            Row row = (Row) rowsIt.next();
            Iterator<Cell> celliterator = row.cellIterator();

            while (celliterator.hasNext()) {
                Cell cell = (Cell) celliterator.next();
                cellValue = cell.getRichStringCellValue().getString().trim();
                //if cell value as a key is assessment code, read its corrresponding value
                if (cellValue != null && !cellValue.isEmpty() && cellValue.equalsIgnoreCase("Assessment Code")) {
                    cell = (Cell) celliterator.next();
                    assessmentCode = cell.getRichStringCellValue().getString().trim();
                }
            }
            if (!assessmentCode.isEmpty()) {
                if (assessmentCodes[0].contains(assessmentCode)) {
                    LOGGER.info("Assessment Code in the Excel is correct");
                    break;
                } else
                    throw new RpsException("AssessmentCode is Incorrect in Excel file");
            }
        }
    }

    private String getAssessmentCode(Iterator<Row> rowsIt) {
        String cellValue = "";
        String assessmentCode = "";

        outerloop:
        while (rowsIt.hasNext()) {
            Row row = (Row) rowsIt.next();
            Iterator<Cell> celliterator = row.cellIterator();

            while (celliterator.hasNext()) {
                Cell cell = (Cell) celliterator.next();
                cellValue = cell.getRichStringCellValue().getString().trim();
                //if cell value as a key is assessment code, read its corrresponding value
                if (cellValue != null && !cellValue.isEmpty() && cellValue.equalsIgnoreCase("Assessment Code")) {
                    cell = (Cell) celliterator.next();
                    assessmentCode = cell.getRichStringCellValue().getString().trim();
                    break outerloop;
                }
            }
        }

        return assessmentCode;
    }

    private List<String> readExcelData(Iterator<Row> rows, Map<String, Integer> keyHeaders, String eventCode, String actionCode, String[] assessmentCodes, boolean isRemovalApplicable) {
        // TODO Auto-generated method stub
        List<AnswerKeyDetails> answerKeyList = new ArrayList<>();
        List<String> errorsList = new ArrayList<>();
        List<String> uniqueQuestID = new ArrayList<>();
        while (rows.hasNext()) {
            String questID = null;
            AnswerKeyDetails answerKey = new AnswerKeyDetails();
            Row row = (Row) rows.next();
            Iterator<Cell> cells = row.cellIterator();
            while (cells.hasNext()) {
                String cellValue = "";
                Cell cell = (Cell) cells.next();
                cell.setCellType(Cell.CELL_TYPE_STRING);
                cellValue = cell.getRichStringCellValue().toString();
                if (!cellValue.equals("")) { //read Question IDs
                    if (cell.getColumnIndex() == keyHeaders.get(RpsConstants.ANSWERKEY_EXCEL_COLUMN.QUESTION_ID.toString())) {
                        questID = cellValue;
                        answerKey.setQuestionID(cellValue);
                    }

                    //read Question types
                    if (cell.getColumnIndex() == keyHeaders.get(RpsConstants.ANSWERKEY_EXCEL_COLUMN.QUESTION_TYPE.toString())) {
                        if (cellValue.contains(".")) {
                            String cellValueAfterDot = cellValue.substring(cellValue.lastIndexOf("."), cellValue.length());
                            if (!cellValueAfterDot.contains("0")) {
                                errorsList.add("QuestionType is not proper format");
                                return errorsList;
                            }
                            cellValue = cellValue.substring(0, cellValue.lastIndexOf("."));
                        }

                        QuestionType type = QuestionTypesDefined.getQuestionType(cellValue);
                        answerKey.setQuestionType(type);
                    }
                    //read answer keys
                    if (cell.getColumnIndex() == keyHeaders.get(RpsConstants.ANSWERKEY_EXCEL_COLUMN.ANSWER_KEY.toString())) {
                        answerKey.setAnswerKey(cellValue);
                    }
                    
                    if (keyHeaders.get(RpsConstants.ANSWERKEY_EXCEL_COLUMN.ALTERNATE_ANSWER_KEY.toString()) != null && 
                    		cell.getColumnIndex() == keyHeaders.get(RpsConstants.ANSWERKEY_EXCEL_COLUMN.ALTERNATE_ANSWER_KEY.toString())) {
                        answerKey.setAlternateAnswerKey(cellValue);
                    }
                }
            }//inner while
			if (questID != null) {
				if (uniqueQuestID.contains(questID))
					errorsList.add("Error- Duplicate Question found--" + questID);
				else {
					answerKeyList.add(answerKey);
					uniqueQuestID.add(questID);
				}
            }
        }//outer while

        if (errorsList != null && !errorsList.isEmpty()) {
            //problem in the excel itself
            errorsList.add(0, "Import of Excel Sheet aborted for reason below--");
        } else
            errorsList.addAll(persistAnswerKeys.updateAnswerKeys(answerKeyList, eventCode, actionCode, assessmentCodes, isRemovalApplicable));

        return errorsList;
    }


    private Map<String, Integer> readHeaderLabels(Iterator<Row> rowsIt) {
        Map<String, Integer> headersMap = new HashMap<String, Integer>();
        boolean isHeaderRow = false;
        while (rowsIt.hasNext()) {
            Row row = rowsIt.next();
            Iterator<Cell> headerCells = row.cellIterator();
            while (headerCells.hasNext()) {
                String cellValue = "";
                Cell cell = (Cell) headerCells.next();
                cellValue = cell.getRichStringCellValue().getString().trim();
                if (!cellValue.isEmpty()) {
                    headersMap.put(cellValue.toUpperCase(), cell.getColumnIndex());
                    isHeaderRow = true;
                }
            }
            //move out of the loop once header row is read completely
            if (isHeaderRow)
                break;
        }

        return headersMap;
    }


    private static String GetFileExtension(String fileName) {
        String ext = "";
        int mid = fileName.lastIndexOf(".");
        ext = fileName.substring(mid + 1, fileName.length());
        return ext;
    }

    /**
     * @param filePath
     * @param password
     * @return
     * @throws RpsException
     */
    public String getAnswerKeyFilePath(String filePath, String password) throws RpsException {
		String extractFilePath = apolloHome + File.separator + "RPSTemp" + File.separator + "AnswerKey"
				+ Calendar.getInstance().getTimeInMillis();
		String outerFolderPath = null;
        String excelFilePath = "";
        try {
            ZipFile zipFile = new ZipFile(filePath);
            if (zipFile.isEncrypted())
                zipFile.setPassword(password);
            zipFile.extractAll(extractFilePath);

//			excelFilePath = extractFilePath + RpsConstants.FileDelimiter + this.getFileName(filePath);
            File tempFile = new File(extractFilePath);
            int fileLength = tempFile.list().length;
            if (fileLength == 1)
            	outerFolderPath = extractFilePath + File.separator + tempFile.list()[0];
            
            if (outerFolderPath != null && new File(outerFolderPath).list().length == 1)
            	excelFilePath = outerFolderPath + File.separator + new File(outerFolderPath).list()[0];

			/*for(String excelFileName : tempFile.list())
			{
				if(!excelFileName.contains(".zip"))
					excelFilePath = extractFilePath + RpsConstants.FileDelimiter + excelFileName;
			}
			*/

			// File excelFile = new File(excelFilePath);
			// if (excelFile.list().length == 1)
			// excelFilePath = excelFilePath + File.separator + excelFile.list()[0];
            else {
				for (String excelFileName : tempFile.list()) {
                    if (excelFileName.contains(".json"))
                        excelFilePath = excelFilePath + File.separator + excelFileName;
                }
            }

        } catch (ZipException e) {
            LOGGER.info("ERROR :: while processing the AnswerkeyPack....");
            throw new RpsException("ERROR :: while processing the AnswerkeyPack....");
        }
        return FilenameUtils.separatorsToSystem(excelFilePath);
    }

    public List<String> readAnswerkeyForAssessment(String filePath,
                                                   String eventCode, String[] assessmentCodes, String actionCode,
                                                   String password) {

        LOGGER.info("File to Read " + filePath);
        List<String> errorsList = new ArrayList<>();
        String tempFolder = apolloHome + File.separator + "RPSTemp";
        File extractedFolder = null;
        boolean isAssessmentLevelZip = false;
        //check whether ZIP is Group level or Assessment Level
        try {
            ZipFile zipFile = new ZipFile(new File(filePath));
            if (zipFile.isEncrypted())
                zipFile.setPassword(password);
            List fileHeadersList = zipFile.getFileHeaders();
            for (Object obj : fileHeadersList) {
                FileHeader fileHeader = (FileHeader) obj;
                LOGGER.info("File to Read " + fileHeader.getFileName());
                if (fileHeader.getFileName().contains(".zip") && !fileHeader.getFileName().contains(RpsConstants.questions)) {
                    isAssessmentLevelZip = true;
                    break;
                }
            }
        } catch (ZipException e1) {
            errorsList.add("Error in opening Answerkey ZIP : " + e1.getMessage());
            return errorsList;
        }

        boolean isRemovalApplicable = true;
        if (isAssessmentLevelZip) {
            try {
                ZipUtility.extractAllOptimized(filePath, tempFolder, false);
                extractedFolder = new File(tempFolder + File.separator + new File(tempFolder).list()[0]);
                String[] groupZips = extractedFolder.list();
                if (groupZips == null || groupZips.length == 0) {
                    errorsList.add("Assessment Level Zip is empty");
                    return errorsList;
                }

                for (String groupZipName : groupZips) {
                    List<String> groupZipErrors = readFile(extractedFolder + File.separator + groupZipName, eventCode, assessmentCodes, actionCode, password, isRemovalApplicable);
                    isRemovalApplicable = false;
                    if (groupZipErrors != null && !groupZipErrors.isEmpty()) {
                        errorsList.add("Errors in processing answerkey for ZIP file - " + groupZipName);
                        errorsList.addAll(groupZipErrors);
                        errorsList.add("");
                    }
                }
            } catch (IOException | ZipException e) {
                errorsList.add("Error in processing Assessment Level ZIP : " + e.getMessage());
                return errorsList;
            } finally {
                if (extractedFolder != null && extractedFolder.canWrite()) {
                    LOGGER.info("Deleting the Path" + extractedFolder.getAbsolutePath());
                    FileUtils.deleteQuietly(extractedFolder);
                }
                if (!extractedFolder.exists())
                    LOGGER.info("Unable to Delete The Path" + extractedFolder.getAbsolutePath());
            }
        } else {
            try {
                errorsList = readFile(filePath, eventCode, assessmentCodes, actionCode, password, isRemovalApplicable);
            } catch (IOException e) {
                errorsList.add("Error in processing zip file" + e.getMessage());
            }
        }
        LOGGER.info("Returning Error List :-" + errorsList.size());
        return errorsList;
    }
}
