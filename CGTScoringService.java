package com.merittrac.apollo.rps.services;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.common.StdDevUtil;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralTestScoresEntity;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociationLite;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.repository.RpsQuestionAssociationRepository;
import com.merittrac.apollo.rps.ui.openevent.utilities.SharedMethodsUtility;

/**
 * Service for CGT Scoring Logic
 *
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class CGTScoringService {

	@Autowired
	private SharedMethodsUtility sharedMethodsUtility;

	@Autowired
	private RpsQuestionAssociationRepository rpsQuestionAssociationRepository;

	private MessagesReader messagesReader = new MessagesReader();
	private Gson gson = new Gson();
	private Type unqCodeType = new TypeToken<HashMap<String, String>>() {
	}.getType();
	private String cgtUniqCodesJson = messagesReader.getProperty("CGT_UNIQUE_CODES");
	private Map<String, String> mapForUniqueCodes = gson.fromJson(cgtUniqCodesJson, unqCodeType);
	private Type unqCodeSumType = new TypeToken<ArrayList<String>>() {
	}.getType();
	private String cgtUniqCodesSummationJson = messagesReader.getProperty("CGT_UNIQUE_CODES_SUMMATION");
	private List<String> listOfSumOfUniqueCodes = gson.fromJson(cgtUniqCodesSummationJson, unqCodeSumType);
	private Type unqCodeParamType = new TypeToken<HashMap<String, ArrayList<String>>>() {
	}.getType();
	private String cgtUniqCodesToParamJson = messagesReader.getProperty("CGT_MAP_UNIQCODES_PARAMETERS");
	private Map<String, ArrayList<String>> listOfParamToUniqueCodes =
			gson.fromJson(cgtUniqCodesToParamJson, unqCodeParamType);

	public String processCandidateResponsesOnUniqueCodes(Integer uniqCandId,
			Map<String, CandidateResponseEntity> questToCandRespForMCQWMap,
			RpsQuestionPaper questionPaper, Map<String, Integer> mapSummationValuesToUniqCodes,
			Map<String, Integer> mapSummationValuesToParam, Map<String, Double> mapStdDevValuesToParam,
			List<String> listOfCandidateResponses) {
		
		// get all questions with sequence in sorted order
		List<RpsQuestionAssociationLite> sortedQuestionAssociationList =
				rpsQuestionAssociationRepository.getAllSortedLiteAssosicationForQuestionPaper(questionPaper.getQpId());
		List<String> candidateResponseUniquecodeList = new ArrayList<String>();
		for (RpsQuestionAssociationLite rpsQuestionAssociationLite : sortedQuestionAssociationList) {
			// iterate and find the candidate response match with uniquecodes
			CandidateResponseEntity candidateResponseEntity =
					questToCandRespForMCQWMap.get(rpsQuestionAssociationLite.getRpsQuestion().getQuestId());
			String candidateResponseUniquecode = evaluateCandidateResponsesOnUniqCode(candidateResponseEntity,
					rpsQuestionAssociationLite.getRpsQuestion(), rpsQuestionAssociationLite.getQuestionSequence(),
					listOfCandidateResponses);
			// list of unique codes attempted by candidate
			candidateResponseUniquecodeList.add(candidateResponseUniquecode);
		}
		sumOfUniquecodes(candidateResponseUniquecodeList, mapSummationValuesToUniqCodes);
		totalOfParameters(mapSummationValuesToUniqCodes, mapSummationValuesToParam, mapStdDevValuesToParam);
		return populateBehaviouralTestScores(mapSummationValuesToUniqCodes, mapSummationValuesToParam,
				mapStdDevValuesToParam, listOfCandidateResponses);
	}

	private String populateBehaviouralTestScores(Map<String, Integer> mapSummationValuesToUniqCodes,
			Map<String, Integer> mapSummationValuesToParam, Map<String, Double> mapStdDevValuesToParam,
			List<String> listOfCandidateResponses) {
		BehaviouralTestScoresEntity behaviouralTestScoresEntity = new BehaviouralTestScoresEntity();
		behaviouralTestScoresEntity.setMapStdDevValuesToParam(gson.toJson(mapStdDevValuesToParam));
		behaviouralTestScoresEntity.setListOfCandidateResponses(gson.toJson(listOfCandidateResponses));
		behaviouralTestScoresEntity.setMapSummationValuesToParam(gson.toJson(mapSummationValuesToParam));
		behaviouralTestScoresEntity.setMapSummationValuesToUniqCodes(gson.toJson(mapSummationValuesToUniqCodes));
		return gson.toJson(behaviouralTestScoresEntity);
	}

	

	private void totalOfParameters(Map<String, Integer> mapSummationValuesToUniqCodes,
			Map<String, Integer> mapSummationValuesToParam, Map<String, Double> mapStdDevValuesToParam) {
		// mapSummationValuesToUniqCodes => sum like S1AR, S1BR, S2AR, S2BR, S3R, S1AC, S1BC, S2AC, S2BC, S3C... ->
		if (mapSummationValuesToUniqCodes != null) {
			// add all values for each param like R A I....
			for (Map.Entry<String, ArrayList<String>> param : listOfParamToUniqueCodes.entrySet()) {
				String parameter = param.getKey();
				ArrayList<String> listOfuniqCodeToParam = param.getValue();
				// create array
				double[] arrayOfTotScoreParams = new double[5];
				int i = 0;
				for (String eachSectionUniqCode : listOfuniqCodeToParam) {
					// add all values for each param like count of R,count of A...
					if (mapSummationValuesToUniqCodes.containsKey(eachSectionUniqCode)) {
						// code for suming
						if (mapSummationValuesToParam.containsKey(parameter)) {
							mapSummationValuesToParam.put(parameter, mapSummationValuesToParam.get(parameter)
									+ mapSummationValuesToUniqCodes.get(eachSectionUniqCode));
						} else {
							mapSummationValuesToParam.put(parameter,
									mapSummationValuesToUniqCodes.get(eachSectionUniqCode));
						}
						// add to array
						arrayOfTotScoreParams[i] = mapSummationValuesToUniqCodes.get(eachSectionUniqCode);
					}
					i++;
				}
				// get std dev here and update param-stdDev map
				
				// code for std dev
				double stddev = findStdDevForScores(arrayOfTotScoreParams);
				mapStdDevValuesToParam.put(parameter, stddev);

				// NO count for parameters itself i.e R=0 or S=0 ... etc
				if (!mapSummationValuesToParam.containsKey(parameter)) {
					mapSummationValuesToParam.put(parameter, 0);
				}

				// NO std dev values for parameters itself i.e R=0 or S=0 ... etc
				if (!mapStdDevValuesToParam.containsKey(parameter)) {
					mapStdDevValuesToParam.put(parameter, 0.0);
				}
				// BehaviouralParamtersEntity behaviouralParamtersEntity = new BehaviouralParamtersEntity(
				// mapSummationValuesToParam.get(parameter), parameter, mapStdDevValuesToParam.get(parameter));
				// behaviouralParamtersEntities.add(behaviouralParamtersEntity);
			}
		}
	}

	// double[] data = { score1, score2, score3, score4 };
	// stdDev = findStdDevForScores(data);
	public double findStdDevForScores(double[] data) {
		StdDevUtil stdDevUtil = new StdDevUtil(data);
		double stddev = Double.parseDouble(sharedMethodsUtility.decimalFormat(stdDevUtil.getStdDev()));
		return stddev;
	}

	private void sumOfUniquecodes(List<String> candidateResponseUniquecodeList,
			Map<String, Integer> mapSummationValuesToUniqCodes) {
		if (candidateResponseUniquecodeList != null) {
			// get unique code for summation

			for (String sumOfUniqueCodes : listOfSumOfUniqueCodes) {

				// check for 1,2,3,4,5 values for each uniq codes i.e S1AR-1,S1AR-2...
				for (int i = 1; i <= 5; i++) {
					// i.e S1AR-1,S1AR-2...
					if (candidateResponseUniquecodeList.contains(sumOfUniqueCodes + i)) {
						// contains key then add one to it else add new obj to Map
						if (mapSummationValuesToUniqCodes.containsKey(sumOfUniqueCodes)) {
							mapSummationValuesToUniqCodes.put(sumOfUniqueCodes,
									mapSummationValuesToUniqCodes.get(sumOfUniqueCodes) + 1);
						} else {
							mapSummationValuesToUniqCodes.put(sumOfUniqueCodes, 1);
						}
					} else {
						// no responses with these codes, hence always zero 0
						if (!mapSummationValuesToUniqCodes.containsKey(sumOfUniqueCodes))
							mapSummationValuesToUniqCodes.put(sumOfUniqueCodes, 0);
					}
				}
			}
		}
	}

	private String evaluateCandidateResponsesOnUniqCode(CandidateResponseEntity candidateResponseEntity,
			RpsQuestion rpsQuestion, Integer questionSequence, List<String> listOfCandidateResponses) {
		String uniquecode = null;
		String candidateResponseString = parseCandidateResponse(candidateResponseEntity);
		String response = getResponseAsAorB(candidateResponseString);
		if (response != null) {
			response = questionSequence + response;
			listOfCandidateResponses.add(response);
			uniquecode = mapForUniqueCodes.get(response);
		}
		return uniquecode;
	}

	private String getResponseAsAorB(String candidateResponseString) {
		String response = null;
		if (candidateResponseString != null) {
			switch (candidateResponseString) {
				case "CHOICE1":
					response = "A";
					break;
				case "CHOICE2":
					response = "B";
					break;

				default:
					break;
			}
		}
		return response;
	}

	private String parseCandidateResponse(CandidateResponseEntity candidateResponseEntity) {
		String candidateResponseString = null;
		if (candidateResponseEntity != null) {
			Map<String, String> choicesMap = candidateResponseEntity.getResponse();
			if (choicesMap != null && !choicesMap.isEmpty()) {
				Set<String> choiceOptions = choicesMap.keySet();
				if (choiceOptions != null && !choiceOptions.isEmpty()) {
					Iterator<String> chIt = choiceOptions.iterator();
					candidateResponseString = choicesMap.get(chIt.next());
				}

			}
		} // disabling default answer, it wont work for default answer
		return candidateResponseString;
	}
	public static void main(String[] args) throws IOException {

		// test file is located in your project path
		FileInputStream fileIn = new FileInputStream("D:/Apollo_Home/rps/config/CGT.xlsx");
		// read file
		POIFSFileSystem fs = new POIFSFileSystem(fileIn);
		HSSFWorkbook filename = new HSSFWorkbook(fs);
		// open sheet 0 which is first sheet of your worksheet
		HSSFSheet sheet = filename.getSheetAt(0);

		// we will search for column index containing string "Your Column Name" in the row 0 (which is first row of a
		// worksheet
		String columnWanted = "Your Column Name";
		Integer columnNo = null;
		// output all not null values to the list
		List<Cell> cells = new ArrayList<Cell>();

		Row firstRow = sheet.getRow(0);

		for (Cell cell : firstRow) {
			if (cell.getStringCellValue().equals(columnWanted)) {
				columnNo = cell.getColumnIndex();
			}
		}

		if (columnNo != null) {
			for (Row row : sheet) {
				Cell c = row.getCell(columnNo);
				if (c == null || c.getCellType() == Cell.CELL_TYPE_BLANK) {
					// Nothing in the cell in this row, skip it
				} else {
					cells.add(c);
				}
			}
		} else {
			System.out.println("could not find column " + columnWanted + " in first row of " + fileIn.toString());
		}

	}
}
