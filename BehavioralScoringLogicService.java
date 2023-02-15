package com.merittrac.apollo.rps.services;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.BehavioralTestType;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.entities.acs.ResponseMarkBean;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralAdaptabilityInterpretationEntity;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralCompetencyWeightageEntity;
import com.merittrac.apollo.data.entity.RpsBehaviouralTest;
import com.merittrac.apollo.data.entity.RpsBehaviouralTestCharacteristic;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociationLite;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.repository.RpsQuestionAssociationRepository;
import com.merittrac.apollo.qpd.qpgenentities.DefaultAnswerOptionEnum;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.core.CandidateResultThread;

/**
 * Service for Behavioral Scoring Logic
 *
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class BehavioralScoringLogicService {


	@Autowired
	private RpsQuestionAssociationRepository rpsQuestionAssociationRepository;

		private MessagesReader messagesReader = new MessagesReader();
		private Gson gson = new Gson();
		private static Logger logger = LoggerFactory.getLogger(CandidateResultThread.class);


	/**
	 * evaluateBehaviouralTest
	 * 
	 * @param questToCandRespMap
	 * @param rpsBehaviouralTest
	 * @param questionPaper
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, Double> evaluateBehaviouralTest(Map<String, CandidateResponseEntity> questToCandRespMap,
			RpsBehaviouralTest rpsBehaviouralTest, RpsQuestionPaper questionPaper,
			DefaultAnswerOptionEnum defaultAnswerOption, Map<String, ArrayList<String>> optionShuffleSeq) {
		Map<String, Double> btScore = new HashMap<>();
		List<RpsQuestionAssociationLite> sortedQuestionAssociationList =
				rpsQuestionAssociationRepository.getAllSortedLiteAssosicationForQuestionPaper(questionPaper.getQpId());
		if (sortedQuestionAssociationList != null && !sortedQuestionAssociationList.isEmpty()) {
			for (RpsQuestionAssociationLite rpsQuestionAssociationLite : sortedQuestionAssociationList) {
				Set<RpsBehaviouralTestCharacteristic> rpsBehaviouralTestCharacteristics =
						rpsBehaviouralTest.getRpsBehaviouralTestCharacteristics();
				if (!rpsBehaviouralTestCharacteristics.isEmpty()) {
					for (RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic : rpsBehaviouralTestCharacteristics) {
						if (rpsBehaviouralTestCharacteristic.getQuestionNumbers() != null) {
							Type listType = new TypeToken<List<Integer>>() {
							}.getType();
							List<Integer> listOfQNumbs = new Gson()
									.fromJson(rpsBehaviouralTestCharacteristic.getQuestionNumbers(), listType);
							if (listOfQNumbs.contains(rpsQuestionAssociationLite.getQuestionSequence()))
								evaluateBTCharstic(questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
										rpsBehaviouralTestCharacteristic, btScore, defaultAnswerOption,
										optionShuffleSeq);
						}
					}
				}
			}
		}
		return btScore;
	}

	/**
	 * evaluateBehaviouralTest
	 * 
	 * @param questToCandRespMap
	 * @param rpsBehaviouralTest
	 * @param questionPaper
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, Double> evaluateBehaviouralTestForD8(
			Map<String, CandidateResponseEntity> questToCandRespMap,
			RpsBehaviouralTest rpsBehaviouralTest, RpsQuestionPaper questionPaper,
			DefaultAnswerOptionEnum defaultAnswerOption, Map<String, ArrayList<String>> optionShuffleSeq) {
		Map<String, Double> btScore = new HashMap<>();
		List<RpsQuestionAssociationLite> sortedQuestionAssociationList =
				rpsQuestionAssociationRepository.getAllSortedLiteAssosicationForQuestionPaper(questionPaper.getQpId());
		if (sortedQuestionAssociationList != null && !sortedQuestionAssociationList.isEmpty()) {
			for (RpsQuestionAssociationLite rpsQuestionAssociationLite : sortedQuestionAssociationList) {
				Set<RpsBehaviouralTestCharacteristic> rpsBehaviouralTestCharacteristics =
						rpsBehaviouralTest.getRpsBehaviouralTestCharacteristics();
				if (!rpsBehaviouralTestCharacteristics.isEmpty()) {
					for (RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic : rpsBehaviouralTestCharacteristics) {
						if (rpsBehaviouralTestCharacteristic.getQuestionNumbers() != null) {
							Type listType = new TypeToken<List<String>>() {
							}.getType();
							List<String> listOfQNumbs = new Gson()
									.fromJson(rpsBehaviouralTestCharacteristic.getQuestionNumbers(), listType);
							Integer seq = rpsQuestionAssociationLite.getQuestionSequence();
							// if A, true
							if (listOfQNumbs.contains(seq + "A")) {
								evaluateBTCharsticForD8(questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
										rpsBehaviouralTestCharacteristic, btScore, defaultAnswerOption,
										optionShuffleSeq, true);
							}
							// if B, false
							else if (listOfQNumbs.contains(seq + "B")) {
								evaluateBTCharsticForD8(questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
										rpsBehaviouralTestCharacteristic, btScore, defaultAnswerOption,
										optionShuffleSeq, false);
								// if only plain questions
							} else if (listOfQNumbs.contains(seq + "")) {
								evaluateBTCharsticForD8(questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
										rpsBehaviouralTestCharacteristic, btScore, defaultAnswerOption,
										optionShuffleSeq, null);
							}
						}
					}
				}
			}
		}
		return btScore;
	}

	/**
	 * evaluateBTCharstic
	 * 
	 * @param questToCandRespMap
	 * @param rpsQuestion
	 * @param rpsBehaviouralTestCharacteristic
	 * @param btScore
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @param isAnsweredA
	 * @param listOfQNumbs
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void evaluateBTCharsticForD8(Map<String, CandidateResponseEntity> questToCandRespMap,
			RpsQuestion rpsQuestion, RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic,
			Map<String, Double> btScore, DefaultAnswerOptionEnum defaultAnswerOption,
			Map<String, ArrayList<String>> optionShuffleSeq, Boolean isAnsweredA) {
		String candidateResponseString = null;
		CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(rpsQuestion.getQuestId());
		candidateResponseString = this.parseCandidateResponse(rpsQuestion, candidateResponseString,
				candidateResponseEntity, defaultAnswerOption, optionShuffleSeq);

		if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
			logger.info("------evaluateBTCharstic Methods Info  -----");
			// parsing mcqw question qAns
			Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
			}.getType();
			String responseMarkBeansJson = rpsQuestion.getQans();
			List<ResponseMarkBean> responseMarkBeans = new Gson().fromJson(responseMarkBeansJson, responseMarkType);
			if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
				for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
					if (responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString) && isAnsweredA != null
							&& isAnsweredA
							&& responseMarkBean.getResponse().equalsIgnoreCase("CHOICE1")) {
						Double charsticScore = btScore.get(rpsBehaviouralTestCharacteristic.getcId());
						if (charsticScore == null) {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									responseMarkBean.getResponsePositiveMarks());
						} else {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									charsticScore + responseMarkBean.getResponsePositiveMarks());
						}
						break;
					} else if (responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString)
							&& isAnsweredA != null && !isAnsweredA
							&& responseMarkBean.getResponse().equalsIgnoreCase("CHOICE2")) {
						Double charsticScore = btScore.get(rpsBehaviouralTestCharacteristic.getcId());
						if (charsticScore == null) {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									responseMarkBean.getResponsePositiveMarks());
						} else {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									charsticScore + responseMarkBean.getResponsePositiveMarks());
						}
						break;
					}
					else if (isAnsweredA == null
							&& responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString)) {
						Double charsticScore = btScore.get(rpsBehaviouralTestCharacteristic.getcId());
						if (charsticScore == null) {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									responseMarkBean.getResponsePositiveMarks());
						} else {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									charsticScore + responseMarkBean.getResponsePositiveMarks());
						}
						break;
					}
				}
			}

		}
	}
	/**
	 * evaluateBTCharstic
	 * 
	 * @param questToCandRespMap
	 * @param rpsQuestion
	 * @param rpsBehaviouralTestCharacteristic
	 * @param btScore
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void evaluateBTCharstic(Map<String, CandidateResponseEntity> questToCandRespMap, RpsQuestion rpsQuestion,
			RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic, Map<String, Double> btScore,
			DefaultAnswerOptionEnum defaultAnswerOption, Map<String, ArrayList<String>> optionShuffleSeq) {
		String candidateResponseString = null;
		CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(rpsQuestion.getQuestId());
		candidateResponseString =
				this.parseCandidateResponse(rpsQuestion, candidateResponseString, candidateResponseEntity,
						defaultAnswerOption, optionShuffleSeq);

		if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
			logger.info("------evaluateBTCharstic Methods Info  -----");
			// parsing mcqw question qAns
			Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
			}.getType();
			String responseMarkBeansJson = rpsQuestion.getQans();
			List<ResponseMarkBean> responseMarkBeans = new Gson().fromJson(responseMarkBeansJson, responseMarkType);
			if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
				for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
					if (responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString)) {
						Double charsticScore = btScore.get(rpsBehaviouralTestCharacteristic.getcId());
						if (charsticScore == null) {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									responseMarkBean.getResponsePositiveMarks());
						} else {
							btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
									charsticScore + responseMarkBean.getResponsePositiveMarks());
						}
						break;
					}
				}
			}

		}
	}

	/**
	 * scoringForIDS
	 * 
	 * @param questToCandRespMap
	 * @param candidateResponseId
	 * @param rpsBehaviouralTest
	 * @param questionPaper
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, Double> scoringForIDS(Map<String, CandidateResponseEntity> questToCandRespMap,
			Integer candidateResponseId, RpsBehaviouralTest rpsBehaviouralTest, RpsQuestionPaper questionPaper,
			DefaultAnswerOptionEnum defaultAnswerOption, Map<String, ArrayList<String>> optionShuffleSeq) {
		logger.debug("--IN-- scoringForIDS() for cand resp id ={} with questToCandRespMap = {} ",
				candidateResponseId, questToCandRespMap);
		Map<String, Double> btScore =
				getQuestionWiseScoresForIDS(questToCandRespMap, candidateResponseId, rpsBehaviouralTest, questionPaper,
						defaultAnswerOption, optionShuffleSeq);
		logger.debug("btScore for cand resp id ={} after getQuestionWiseScoresForIDS() is = {}",
				candidateResponseId, btScore);
		getCompetencyScores(btScore, candidateResponseId, rpsBehaviouralTest);
		logger.debug("--OUT-- scoringForIDS() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
		return btScore;
	}

	/**
	 * getCompetencyScores
	 * 
	 * @param btScore
	 * @param candidateResponseId
	 * @param rpsBehaviouralTest
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void getCompetencyScores(Map<String, Double> btScore, Integer candidateResponseId,
			RpsBehaviouralTest rpsBehaviouralTest) {
		logger.debug("--IN-- getCompetencyScores() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
		scoringForAdaptibilityCompetency(btScore, rpsBehaviouralTest, candidateResponseId);
		logger.debug("btScore for cand resp id ={} after scoringForAdaptibilityCompetency() is = {}",
				candidateResponseId, btScore);
		List<BehaviouralCompetencyWeightageEntity> sortedRIASECscorelist =
				getSortedScoresForRIASEC(btScore, rpsBehaviouralTest, candidateResponseId);
		logger.debug("sortedRIASECscorelist for cand resp id ={} after getSortedScoresForRIASEC() is = {}",
				candidateResponseId, sortedRIASECscorelist);
		scoringForOtherCompetencies(btScore, sortedRIASECscorelist, candidateResponseId, rpsBehaviouralTest);

		logger.debug("--OUT-- getCompetencyScores() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
	}

	/**
	 * scoringForOtherCompetencies
	 * 
	 * @param btScore
	 * @param sortedRIASECscorelist
	 * @param candidateResponseId
	 * @param rpsBehaviouralTest
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void scoringForOtherCompetencies(Map<String, Double> btScore,
			List<BehaviouralCompetencyWeightageEntity> sortedRIASECscorelist, Integer candidateResponseId,
			RpsBehaviouralTest rpsBehaviouralTest) {
		logger.debug(
				"--IN-- scoringForOtherCompetencies() for cand resp id ={} with btScore = {}  and with sortedRIASECscorelist = {}",
				candidateResponseId, btScore, sortedRIASECscorelist);
		Map<String, BehaviouralCompetencyWeightageEntity> sortedRIASECscoreMap =
				new HashMap<String, BehaviouralCompetencyWeightageEntity>();
		for (BehaviouralCompetencyWeightageEntity behaviouralCompetencyWeightageEntity : sortedRIASECscorelist) {
			sortedRIASECscoreMap.put(behaviouralCompetencyWeightageEntity.getNameOfParam(),
					behaviouralCompetencyWeightageEntity);
		}

		Double average = 0.0;
		Map<String, Double> percentages = BehaviouralCompetencyWeightageEntity.getPercentageMap(sortedRIASECscorelist);

		// IDSA new pecentage calculation results
		Map<String, Double> percentagesIDSA =
				BehaviouralCompetencyWeightageEntity.getPercentageMap(sortedRIASECscorelist);

		Set<RpsBehaviouralTestCharacteristic> rpsBehaviouralTestCharacteristics =
				rpsBehaviouralTest.getRpsBehaviouralTestCharacteristics();
		for (RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic : rpsBehaviouralTestCharacteristics) {

			switch (rpsBehaviouralTestCharacteristic.getcId()) {
				case "IDSGA":
					Double score = 0.0;
					// count on J's marks assigned = 0
					// count on P's marks assigned = 1
					score = ((btScore.get(RpsConstants.COUNT_ON_J) == null ? RpsConstants.DOUBLE_ZERO_VALUE
							: btScore.get(RpsConstants.COUNT_ON_J)) * RpsConstants.MARKS_ON_COUNT_ON_J)
							+ ((btScore.get(RpsConstants.COUNT_ON_P) == null ? RpsConstants.DOUBLE_ZERO_VALUE
									: btScore.get(RpsConstants.COUNT_ON_P)) * RpsConstants.MARKS_ON_COUNT_ON_P)
							+ (btScore
									.get(RpsConstants.IDSB_ADAPTIBILITY + RpsConstants.IDS_ADAPTIBILITY_MEASURE) == null
											? RpsConstants.DOUBLE_ZERO_VALUE
											: btScore.get(RpsConstants.IDSB_ADAPTIBILITY
													+ RpsConstants.IDS_ADAPTIBILITY_MEASURE));
					// 59 is the total marks for perc
					score = (score / RpsConstants.MAX_PERC_ON_IDSBA_PARAMS) * 100;
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", score);
					break;
				case "IDSGS":

					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%",
							percentages.get(RpsConstants.PERCENT_ON_S));
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGTW":
					average = ((percentages.get(RpsConstants.PERCENT_ON_C) + percentages.get(RpsConstants.PERCENT_ON_S)
							+ percentages.get(RpsConstants.PERCENT_ON_E)) / 3);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGP":
					average = ((percentages.get(RpsConstants.PERCENT_ON_R) + percentages.get(RpsConstants.PERCENT_ON_C))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_R))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGC":
					average = ((percentages.get(RpsConstants.PERCENT_ON_I) + percentages.get(RpsConstants.PERCENT_ON_A))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_I))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_A))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGO":
					average = ((percentages.get(RpsConstants.PERCENT_ON_I) + percentages.get(RpsConstants.PERCENT_ON_C))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_I))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGPS":
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%",
							percentages.get(RpsConstants.PERCENT_ON_E));
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSGCON":
					average = ((percentages.get(RpsConstants.PERCENT_ON_C) + percentages.get(RpsConstants.PERCENT_ON_S))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBA":
					score = 0.0;
					// count on J's marks assigned = 0
					// count on P's marks assigned = 1
					score = ((btScore.get(RpsConstants.COUNT_ON_J) == null ? RpsConstants.DOUBLE_ZERO_VALUE
							: btScore.get(RpsConstants.COUNT_ON_J)) * RpsConstants.MARKS_ON_COUNT_ON_J)
							+ ((btScore.get(RpsConstants.COUNT_ON_P) == null ? RpsConstants.DOUBLE_ZERO_VALUE
									: btScore.get(RpsConstants.COUNT_ON_P)) * RpsConstants.MARKS_ON_COUNT_ON_P)
							+ (btScore
									.get(RpsConstants.IDSB_ADAPTIBILITY + RpsConstants.IDS_ADAPTIBILITY_MEASURE) == null
											? RpsConstants.DOUBLE_ZERO_VALUE
											: btScore.get(RpsConstants.IDSB_ADAPTIBILITY
													+ RpsConstants.IDS_ADAPTIBILITY_MEASURE));
					// 59 is the total marks for perc
					score = (score / RpsConstants.MAX_PERC_ON_IDSBA_PARAMS) * 100;
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", score);
					break;
				case "IDSBS":
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%",
							percentages.get(RpsConstants.PERCENT_ON_S));
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBTW":
					average = ((percentages.get(RpsConstants.PERCENT_ON_C) + percentages.get(RpsConstants.PERCENT_ON_S)
							+ percentages.get(RpsConstants.PERCENT_ON_E)) / 3);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBP":
					average = ((percentages.get(RpsConstants.PERCENT_ON_R) + percentages.get(RpsConstants.PERCENT_ON_C))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_R))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBC":
					average = ((percentages.get(RpsConstants.PERCENT_ON_I) + percentages.get(RpsConstants.PERCENT_ON_A))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_I))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_A))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBO":
					average = ((percentages.get(RpsConstants.PERCENT_ON_I) + percentages.get(RpsConstants.PERCENT_ON_C))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_I))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBPS":
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%",
							percentages.get(RpsConstants.PERCENT_ON_E));
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_E))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;
				case "IDSBCON":
					average = ((percentages.get(RpsConstants.PERCENT_ON_C) + percentages.get(RpsConstants.PERCENT_ON_S))
							/ 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					if (getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_S))
							|| getRankedCompetencyEntity(sortedRIASECscoreMap.get(RpsConstants.COUNT_ON_C))) {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
					} else {
						btScore.put(rpsBehaviouralTestCharacteristic.getcId(),
								Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
					}
					break;

				// adding the pecentage for IDSA

				case "IDSAA":
					score = 0.0;
					// count on J's marks assigned = 0
					// count on P's marks assigned = 1
					score = ((btScore.get(RpsConstants.COUNT_ON_J) == null ? RpsConstants.DOUBLE_ZERO_VALUE
							: btScore.get(RpsConstants.COUNT_ON_J)) * RpsConstants.MARKS_ON_COUNT_ON_J)
							+ ((btScore.get(RpsConstants.COUNT_ON_P) == null ? RpsConstants.DOUBLE_ZERO_VALUE
									: btScore.get(RpsConstants.COUNT_ON_P)) * RpsConstants.MARKS_ON_COUNT_ON_P)
							+ (btScore
									.get(RpsConstants.IDSA_ADAPTIBILITY + RpsConstants.IDS_ADAPTIBILITY_MEASURE) == null
											? RpsConstants.DOUBLE_ZERO_VALUE
											: btScore.get(RpsConstants.IDSA_ADAPTIBILITY
													+ RpsConstants.IDS_ADAPTIBILITY_MEASURE));
					// 59 is the total marks for perc
					score = (score / RpsConstants.MAX_PERC_ON_IDSBA_PARAMS) * 100;
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", score);
					break;
				case "IDSAS":
					average = (percentagesIDSA.get(RpsConstants.PERCENT_ON_S_A));
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					break;
				case "IDSATW":
					average = ((percentagesIDSA.get(RpsConstants.PERCENT_ON_C_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_S_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_E_A)) / 3);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);

					break;
				case "IDSAP":
					average = ((percentagesIDSA.get(RpsConstants.PERCENT_ON_R_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_C_A)) / 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);

					break;
				case "IDSAC":
					average = ((percentagesIDSA.get(RpsConstants.PERCENT_ON_I_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_A_A)) / 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);

					break;
				case "IDSAO":
					average = ((percentagesIDSA.get(RpsConstants.PERCENT_ON_I_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_C_A)) / 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);

					break;
				case "IDSAPS":
					average = (percentagesIDSA.get(RpsConstants.PERCENT_ON_E_A));
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);
					break;
				case "IDSACON":
					average = ((percentagesIDSA.get(RpsConstants.PERCENT_ON_C_A)
							+ percentagesIDSA.get(RpsConstants.PERCENT_ON_S_A)) / 2);
					btScore.put(rpsBehaviouralTestCharacteristic.getcId() + "%", average);

					break;

				default:
					break;
			}
		}
		logger.debug(
				"--OUT-- scoringForOtherCompetencies() for cand resp id ={} with btScore = {}  and with sortedRIASECscorelist = {}",
				candidateResponseId, btScore, sortedRIASECscorelist);
	}

	/**
	 * getRankedCompetencyEntity
	 * 
	 * @param behaviouralCompetencyWeightageEntity
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean
			getRankedCompetencyEntity(BehaviouralCompetencyWeightageEntity behaviouralCompetencyWeightageEntity) {
		if (behaviouralCompetencyWeightageEntity.getRankingOnParam() <= 3) {
			return true;
		} else if (behaviouralCompetencyWeightageEntity.getRankingOnParam() > 3) {
			return false;
		}
		return false;
	}

	/**
	 * getSortedScoresForRIASEC
	 * 
	 * @param btScore
	 * @param rpsBehaviouralTest
	 * @param candidateResponseId
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<BehaviouralCompetencyWeightageEntity> getSortedScoresForRIASEC(Map<String, Double> btScore,
			RpsBehaviouralTest rpsBehaviouralTest, Integer candidateResponseId) {
		logger.debug("--IN-- getSortedScoresForRIASEC : btScore = {}", btScore);
		List<BehaviouralCompetencyWeightageEntity> list = null;
		if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSB.toString())
				|| rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSG.toString())
				|| rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSA.toString())) {
			list = getSortedScoresForRIASECByCountDifference(btScore, rpsBehaviouralTest, candidateResponseId);
			list = getSortedScoresForRIASECByPercentage(btScore, list, candidateResponseId);
			/* list.addAll(getAdvanceScoresForRIASECByPercentage(btScore, list)); */
		} else {
			logger.debug("--OUT-- getSortedScoresForRIASEC : Invalid rpsBehaviouralTest = {}",
					rpsBehaviouralTest.getBtId());
		}
		return list;
	}

	/**
	 * getSortedScoresForRIASECByCountDifference
	 * 
	 * @param btScore
	 * @param rpsBehaviouralTest
	 * @param candidateResponseId
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<BehaviouralCompetencyWeightageEntity>
			getSortedScoresForRIASECByCountDifference(Map<String, Double> btScore,
					RpsBehaviouralTest rpsBehaviouralTest, Integer candidateResponseId) {
		logger.debug("--IN-- getSortedScoresForRIASEC() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
		Double countOnR1 = btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_ONE);
		Double countOnR2 = btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_TWO);
		Double countOnI1 = btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_ONE);
		Double countOnI2 = btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_TWO);
		Double countOnA1 = btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_ONE);
		Double countOnA2 = btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_TWO);
		Double countOnS1 = btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_ONE);
		Double countOnS2 = btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_TWO);
		Double countOnE1 = btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_ONE);
		Double countOnE2 = btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_TWO);
		Double countOnC1 = btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_ONE);
		Double countOnC2 = btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_TWO);

		// Difference

		Double countOnR1_R2 = (countOnR1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR1)
				- (countOnR2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR2);
		Double countOnI1_I2 = (countOnI1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI1)
				- (countOnI2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI2);
		Double countOnA1_A2 = (countOnA1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA1)
				- (countOnA2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA2);
		Double countOnS1_S2 = (countOnS1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS1)
				- (countOnS2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS2);
		Double countOnE1_E2 = (countOnE1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE1)
				- (countOnE2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE2);
		Double countOnC1_C2 = (countOnC1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC1)
				- (countOnC2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC2);

		if (countOnR1_R2 < 0.0)
			countOnR1_R2 = Math.abs(countOnR1_R2);
		if (countOnI1_I2 < 0.0)
			countOnI1_I2 = Math.abs(countOnI1_I2);
		if (countOnA1_A2 < 0.0)
			countOnA1_A2 = Math.abs(countOnA1_A2);
		if (countOnS1_S2 < 0.0)
			countOnS1_S2 = Math.abs(countOnS1_S2);
		if (countOnE1_E2 < 0.0)
			countOnE1_E2 = Math.abs(countOnE1_E2);
		if (countOnC1_C2 < 0.0)
			countOnC1_C2 = Math.abs(countOnC1_C2);

		List<BehaviouralCompetencyWeightageEntity> list = new ArrayList<BehaviouralCompetencyWeightageEntity>();
		BehaviouralCompetencyWeightageEntity R =
				new BehaviouralCompetencyWeightageEntity(countOnR1_R2, RpsConstants.COUNT_ON_R, countOnR2);
		BehaviouralCompetencyWeightageEntity I =
				new BehaviouralCompetencyWeightageEntity(countOnI1_I2, RpsConstants.COUNT_ON_I, countOnI2);
		BehaviouralCompetencyWeightageEntity A =
				new BehaviouralCompetencyWeightageEntity(countOnA1_A2, RpsConstants.COUNT_ON_A, countOnA2);
		BehaviouralCompetencyWeightageEntity S =
				new BehaviouralCompetencyWeightageEntity(countOnS1_S2, RpsConstants.COUNT_ON_S, countOnS2);
		BehaviouralCompetencyWeightageEntity E =
				new BehaviouralCompetencyWeightageEntity(countOnE1_E2, RpsConstants.COUNT_ON_E, countOnE2);
		BehaviouralCompetencyWeightageEntity C =
				new BehaviouralCompetencyWeightageEntity(countOnC1_C2, RpsConstants.COUNT_ON_C, countOnC2);
		list.add(R);
		list.add(I);
		list.add(E);
		list.add(A);
		list.add(S);
		list.add(C);
		Collections.sort(list);
		Integer rank = 1;
		Double prevCountOnTwos = null;
		Double prevCountOnParam = null;
		for (BehaviouralCompetencyWeightageEntity behaviouralCompetencyWeightageEntity : list) {
			if (prevCountOnTwos == null && prevCountOnParam == null) {
				prevCountOnTwos = behaviouralCompetencyWeightageEntity.getCountOnTwosParam();
				prevCountOnParam = behaviouralCompetencyWeightageEntity.getCountOnParam();
				behaviouralCompetencyWeightageEntity.setRankingOnParam(rank++);
			} else if ((Double.doubleToLongBits(prevCountOnParam) == Double
					.doubleToLongBits(behaviouralCompetencyWeightageEntity.getCountOnParam()))
					&& (Double.doubleToLongBits(prevCountOnTwos) == Double
							.doubleToLongBits(behaviouralCompetencyWeightageEntity.getCountOnTwosParam()))) {
				behaviouralCompetencyWeightageEntity.setRankingOnParam(rank - 1);
			} else {
				behaviouralCompetencyWeightageEntity.setRankingOnParam(rank++);
			}
			prevCountOnTwos = behaviouralCompetencyWeightageEntity.getCountOnTwosParam();
			prevCountOnParam = behaviouralCompetencyWeightageEntity.getCountOnParam();
		}

		logger.debug("--OUT-- getSortedScoresForRIASEC() for cand resp id ={} with Sorted list = {} ",
				candidateResponseId, list);
		return list;

	}

	/**
	 * getSortedScoresForRIASECByPercentage
	 * 
	 * @param btScore
	 * @param list
	 * @param candidateResponseId
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<BehaviouralCompetencyWeightageEntity> getSortedScoresForRIASECByPercentage(Map<String, Double> btScore,
			List<BehaviouralCompetencyWeightageEntity> list, Integer candidateResponseId) {
		logger.debug("--IN-- getSortedScoresForRIASEC() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
		Double countOnR1 = btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_ONE);
		Double countOnR2 = btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_R + RpsConstants.COUNT_ON_TWO);
		Double countOnI1 = btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_ONE);
		Double countOnI2 = btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_I + RpsConstants.COUNT_ON_TWO);
		Double countOnA1 = btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_ONE);
		Double countOnA2 = btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_A + RpsConstants.COUNT_ON_TWO);
		Double countOnS1 = btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_ONE);
		Double countOnS2 = btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_S + RpsConstants.COUNT_ON_TWO);
		Double countOnE1 = btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_ONE);
		Double countOnE2 = btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_E + RpsConstants.COUNT_ON_TWO);
		Double countOnC1 = btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_ONE) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_ONE);
		Double countOnC2 = btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_TWO) == null
				? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(RpsConstants.COUNT_ON_C + RpsConstants.COUNT_ON_TWO);

		// Difference

		Double countOnR1_R2 = (countOnR1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR1)
				- (countOnR2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR2);
		Double countOnI1_I2 = (countOnI1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI1)
				- (countOnI2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI2);
		Double countOnA1_A2 = (countOnA1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA1)
				- (countOnA2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA2);
		Double countOnS1_S2 = (countOnS1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS1)
				- (countOnS2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS2);
		Double countOnE1_E2 = (countOnE1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE1)
				- (countOnE2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE2);
		Double countOnC1_C2 = (countOnC1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC1)
				- (countOnC2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC2);

		if (countOnR1_R2 < 0.0)
			countOnR1_R2 = Math.abs(countOnR1_R2);
		if (countOnI1_I2 < 0.0)
			countOnI1_I2 = Math.abs(countOnI1_I2);
		if (countOnA1_A2 < 0.0)
			countOnA1_A2 = Math.abs(countOnA1_A2);
		if (countOnS1_S2 < 0.0)
			countOnS1_S2 = Math.abs(countOnS1_S2);
		if (countOnE1_E2 < 0.0)
			countOnE1_E2 = Math.abs(countOnE1_E2);
		if (countOnC1_C2 < 0.0)
			countOnC1_C2 = Math.abs(countOnC1_C2);

		// Calculate the percentage based on the count.
		// Considering 25 is the maximum marks one can get.
		// Need to move the maximum marks to config file.
		Double percentageOnR1_R2 = ((countOnR1_R2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);
		Double percentageOnI1_I2 = ((countOnI1_I2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);
		Double percentageOnA1_A2 = ((countOnA1_A2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);
		Double percentageOnS1_S2 = ((countOnS1_S2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);
		Double percentageOnE1_E2 = ((countOnE1_E2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);
		Double percentageOnC1_C2 = ((countOnC1_C2 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS) * 100);

		// List<BehaviouralCompetencyWeightageEntity> list = new ArrayList<BehaviouralCompetencyWeightageEntity>();
		BehaviouralCompetencyWeightageEntity R =
				new BehaviouralCompetencyWeightageEntity(percentageOnR1_R2, RpsConstants.PERCENT_ON_R);
		BehaviouralCompetencyWeightageEntity I =
				new BehaviouralCompetencyWeightageEntity(percentageOnI1_I2, RpsConstants.PERCENT_ON_I);
		BehaviouralCompetencyWeightageEntity A =
				new BehaviouralCompetencyWeightageEntity(percentageOnA1_A2, RpsConstants.PERCENT_ON_A);
		BehaviouralCompetencyWeightageEntity S =
				new BehaviouralCompetencyWeightageEntity(percentageOnS1_S2, RpsConstants.PERCENT_ON_S);
		BehaviouralCompetencyWeightageEntity E =
				new BehaviouralCompetencyWeightageEntity(percentageOnE1_E2, RpsConstants.PERCENT_ON_E);
		BehaviouralCompetencyWeightageEntity C =
				new BehaviouralCompetencyWeightageEntity(percentageOnC1_C2, RpsConstants.PERCENT_ON_C);
		list.add(R);
		list.add(I);
		list.add(E);
		list.add(A);
		list.add(S);
		list.add(C);
		Double countOnR1_R2_A = (countOnR1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR1)
				- (countOnR2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnR2);
		;
		Double countOnI1_I2_A = (countOnI1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI1)
				- (countOnI2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnI2);
		;
		Double countOnA1_A2_A = (countOnA1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA1)
				- (countOnA2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnA2);
		;
		Double countOnS1_S2_A = (countOnS1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS1)
				- (countOnS2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnS2);
		;
		Double countOnE1_E2_A = (countOnE1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE1)
				- (countOnE2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnE2);
		;
		Double countOnC1_C2_A = (countOnC1 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC1)
				- (countOnC2 == null ? RpsConstants.DOUBLE_ZERO_VALUE : countOnC2);
		// Calculate the percentage based on the count.
		// Considering 25 is the maximum marks one can get.
		// Need to move the maximum marks to config file.
		Double percentageOnR1_R2_A = 0.0;
		Double percentageOnI1_I2_A = 0.0;
		Double percentageOnA1_A2_A = 0.0;
		Double percentageOnS1_S2_A = 0.0;
		Double percentageOnE1_E2_A = 0.0;
		Double percentageOnC1_C2_A = 0.0;
		Double compareDouble = 0.0;
		if (countOnR1_R2_A < 0.0) {
			percentageOnR1_R2_A = (50 + (countOnR1_R2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnR1_R2_A > 0) {
			percentageOnR1_R2_A = (50 + (countOnR1_R2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnR1_R2_A, compareDouble) == 0) {
			percentageOnR1_R2_A = 50.00;
		}

		if (countOnI1_I2_A < 0.0) {
			percentageOnI1_I2_A = (50 + (countOnI1_I2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnI1_I2_A > 0) {
			percentageOnI1_I2_A = (50 + (countOnI1_I2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnI1_I2_A, compareDouble) == 0) {
			percentageOnI1_I2_A = 50.00;
		}
		if (countOnA1_A2_A < 0.0) {
			percentageOnA1_A2_A = (50 + (countOnA1_A2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnA1_A2_A > 0) {
			percentageOnA1_A2_A = (50 + (countOnA1_A2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnA1_A2_A, compareDouble) == 0) {
			percentageOnA1_A2_A = 50.00;
		}
		if (countOnS1_S2_A < 0.0) {
			percentageOnS1_S2_A = (50 + (countOnS1_S2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnS1_S2_A > 0) {
			percentageOnS1_S2_A = (50 + (countOnS1_S2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnS1_S2_A, compareDouble) == 0) {
			percentageOnS1_S2_A = 50.00;
		}
		if (countOnE1_E2_A < 0.0) {
			percentageOnE1_E2_A = (50 + (countOnE1_E2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnE1_E2_A > 0) {
			percentageOnE1_E2_A = (50 + (countOnE1_E2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnE1_E2_A, compareDouble) == 0) {
			percentageOnE1_E2_A = 50.00;
		}

		if (countOnC1_C2_A < 0.0) {
			percentageOnC1_C2_A = (50 + (countOnC1_C2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (countOnC1_C2_A > 0) {
			percentageOnC1_C2_A = (50 + (countOnC1_C2_A * 100 / RpsConstants.MAX_PERC_ON_IDSB_PARAMS_ADVANCE));
		}
		if (Double.compare(countOnC1_C2_A, compareDouble) == 0) {
			percentageOnC1_C2_A = 50.00;
		}

		// List<BehaviouralCompetencyWeightageEntity> list = new ArrayList<BehaviouralCompetencyWeightageEntity>();
		BehaviouralCompetencyWeightageEntity R_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnR1_R2_A, RpsConstants.PERCENT_ON_R_A);
		BehaviouralCompetencyWeightageEntity I_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnI1_I2_A, RpsConstants.PERCENT_ON_I_A);
		BehaviouralCompetencyWeightageEntity A_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnA1_A2_A, RpsConstants.PERCENT_ON_A_A);
		BehaviouralCompetencyWeightageEntity S_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnS1_S2_A, RpsConstants.PERCENT_ON_S_A);
		BehaviouralCompetencyWeightageEntity E_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnE1_E2_A, RpsConstants.PERCENT_ON_E_A);
		BehaviouralCompetencyWeightageEntity C_A =
				new BehaviouralCompetencyWeightageEntity(percentageOnC1_C2_A, RpsConstants.PERCENT_ON_C_A);
		list.add(R_A);
		list.add(I_A);
		list.add(E_A);
		list.add(A_A);
		list.add(S_A);
		list.add(C_A);

		logger.debug("--OUT-- getSortedScoresForRIASEC() for cand resp id ={} with Sorted list = {} ",
				candidateResponseId, list);
		return list;
	}

	/**
	 * scoringForAdaptibilityCompetency
	 * 
	 * @param btScore
	 * @param rpsBehaviouralTest
	 * @param candidateResponseId
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void scoringForAdaptibilityCompetency(Map<String, Double> btScore, RpsBehaviouralTest rpsBehaviouralTest,
			Integer candidateResponseId) {
		logger.debug("--IN-- scoringForAdaptibilityCompetency() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
		String type = "";
		if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSB.toString()))
			type = RpsConstants.IDSB_ADAPTIBILITY;
		else if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSG.toString()))
			type = RpsConstants.IDSG_ADAPTIBILITY;
		else if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSA.toString()))
			type = RpsConstants.IDSA_ADAPTIBILITY;

		String measure1Grade =
				measure1Interpret(btScore.get(type) == null ? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(type));
		Double adaptibilityScore = btScore.get(type) == null ? RpsConstants.DOUBLE_ZERO_VALUE : btScore.get(type);
		btScore.put(type + RpsConstants.IDS_ADAPTIBILITY_MEASURE, adaptibilityScore);
		String measure2Grade = measure2Interpret(
				btScore.get(RpsConstants.COUNT_ON_J) == null ? RpsConstants.DOUBLE_ZERO_VALUE
						: btScore.get(RpsConstants.COUNT_ON_J),
				btScore.get(RpsConstants.COUNT_ON_P) == null ? RpsConstants.DOUBLE_ZERO_VALUE
						: btScore.get(RpsConstants.COUNT_ON_P));

		Type gradeType = new TypeToken<ArrayList<BehaviouralAdaptabilityInterpretationEntity>>() {
		}.getType();
		String json = messagesReader.getProperty("IDS_ADAPTIBILITY_INTERP");
		List<BehaviouralAdaptabilityInterpretationEntity> gradeList = new Gson().fromJson(json, gradeType);
		String scoreGrade = RpsConstants.NA;

		if (gradeList != null && measure1Grade != null && measure2Grade != null) {
			for (BehaviouralAdaptabilityInterpretationEntity gradingSchemeDo : gradeList) {
				if (gradingSchemeDo.getMeasure1().equalsIgnoreCase(measure1Grade)
						&& gradingSchemeDo.getMeasure2().equalsIgnoreCase(measure2Grade)) {
					scoreGrade = gradingSchemeDo.getAdaptabilityInterpretation();
					break;
				}
			}
		}
		if (scoreGrade.equalsIgnoreCase(messagesReader.getProperty("IDS_LIKELY_WEAKNESS"))) {
			btScore.put(type, Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_WEAKNESS_VALUE")));
		} else if (scoreGrade.equalsIgnoreCase(messagesReader.getProperty("IDS_LIKELY_STRENGTH"))) {
			btScore.put(type, Double.parseDouble(messagesReader.getProperty("IDS_LIKELY_STRENGTH_VALUE")));
		}
		logger.debug("--OUT-- scoringForAdaptibilityCompetency() for cand resp id ={} with btScore = {} ",
				candidateResponseId, btScore);
	}

	/**
	 * measure2Interpret
	 * 
	 * @param countOnJ
	 * @param countOnP
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String measure2Interpret(Double countOnJ, Double countOnP) {
		String scoreGrade = RpsConstants.NA;
		if (countOnJ > countOnP) {
			scoreGrade = RpsConstants.BEHAVIOURAL_LOW;
		} else if (countOnJ < countOnP) {
			scoreGrade = RpsConstants.BEHAVIOURAL_HIGH;
		} else if (Double.doubleToLongBits(countOnJ) == Double.doubleToLongBits(countOnP)) {
			scoreGrade = RpsConstants.BEHAVIOURAL_DISREGARD;
		}
		return scoreGrade;
	}

	/**
	 * measure1Interpret
	 * 
	 * @param score
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String measure1Interpret(Double score) {
		String scoreGrade = RpsConstants.NA;
		if (score < RpsConstants.IDS_MEASURING_SCORE_LIMIT) {
			scoreGrade = RpsConstants.BEHAVIOURAL_LOW;
		}
		if (score >= RpsConstants.IDS_MEASURING_SCORE_LIMIT) {
			scoreGrade = RpsConstants.BEHAVIOURAL_HIGH;
		}
		return scoreGrade;
	}

	/**
	 * getQuestionWiseScoresForIDS
	 * 
	 * @param questToCandRespMap
	 * @param candidateResponseId
	 * @param rpsBehaviouralTest
	 * @param questionPaper
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private Map<String, Double> getQuestionWiseScoresForIDS(Map<String, CandidateResponseEntity> questToCandRespMap,
			Integer candidateResponseId, RpsBehaviouralTest rpsBehaviouralTest, RpsQuestionPaper questionPaper,
			DefaultAnswerOptionEnum defaultAnswerOption, Map<String, ArrayList<String>> optionShuffleSeq) {
		logger.debug("--IN-- getQuestionWiseScoresForIDS() for cand resp id ={} ",
				candidateResponseId);
		Map<String, Double> btScore = new HashMap<>();
		List<RpsQuestionAssociationLite> sortedQuestionAssociationList =
				rpsQuestionAssociationRepository.getAllSortedLiteAssosicationForQuestionPaper(questionPaper.getQpId());
		Set<RpsBehaviouralTestCharacteristic> rpsBehaviouralTestCharacteristics =
				rpsBehaviouralTest.getRpsBehaviouralTestCharacteristics();
		if (sortedQuestionAssociationList != null && !sortedQuestionAssociationList.isEmpty()) {
			for (RpsQuestionAssociationLite rpsQuestionAssociationLite : sortedQuestionAssociationList) {
				if (rpsQuestionAssociationLite.getQuestionSequence() >= RpsConstants.LOWER_LIMIT_SET2_Qs
						&& rpsQuestionAssociationLite.getQuestionSequence() <= RpsConstants.UPPER_LIMIT_SET2_Qs) {
					evaluateJAndPPairs(btScore, questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
							rpsQuestionAssociationLite.getQuestionSequence(), candidateResponseId, defaultAnswerOption,
							optionShuffleSeq);
				} else if (rpsQuestionAssociationLite.getQuestionSequence() >= RpsConstants.LOWER_LIMIT_SET3_Qs
						&& rpsQuestionAssociationLite.getQuestionSequence() <= RpsConstants.UPPER_LIMIT_SET3_Qs
						&& !rpsQuestionAssociationLite.getRpsQuestion().getQuestionType()
								.equalsIgnoreCase(QuestionType.SURVEY.name())) {
					evaluateRIASECPairs(btScore, questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
							rpsQuestionAssociationLite.getQuestionSequence(), candidateResponseId, defaultAnswerOption,
							optionShuffleSeq);
				} else {
					if (!rpsBehaviouralTestCharacteristics.isEmpty()) {
						for (RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic : rpsBehaviouralTestCharacteristics) {
							if (rpsBehaviouralTestCharacteristic.getcId()
									.equalsIgnoreCase(RpsConstants.IDSG_ADAPTIBILITY)
									|| rpsBehaviouralTestCharacteristic.getcId()
											.equalsIgnoreCase(RpsConstants.IDSB_ADAPTIBILITY)
									|| rpsBehaviouralTestCharacteristic.getcId()
											.equalsIgnoreCase(RpsConstants.IDSA_ADAPTIBILITY)) {
								evaluateBTCharstic(questToCandRespMap, rpsQuestionAssociationLite.getRpsQuestion(),
										rpsBehaviouralTestCharacteristic, btScore, defaultAnswerOption,
										optionShuffleSeq);
							}
						}
					}
				}
			}
		}
		logger.debug("--OUT-- getQuestionWiseScoresForIDS() returning for cand resp id ={} with btScore = {}",
				candidateResponseId, btScore);
		return btScore;
	}

	/**
	 * evaluateJAndPPairs
	 * 
	 * @param btScore
	 * @param questToCandRespMap
	 * @param rpsQuestion
	 * @param questionSequence
	 * @param candidateResponseId
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void evaluateJAndPPairs(Map<String, Double> btScore,
			Map<String, CandidateResponseEntity> questToCandRespMap, RpsQuestion rpsQuestion,
			Integer questionSequence, Integer candidateResponseId, DefaultAnswerOptionEnum defaultAnswerOption,
			Map<String, ArrayList<String>> optionShuffleSeq) {
		logger.debug("--IN-- evaluateJAndPPairs() for cand resp id ={} and btScore = {} and questionSequence = {} ",
				candidateResponseId, btScore, questionSequence);
		String candidateResponseString = null;
		CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(rpsQuestion.getQuestId());
		candidateResponseString =
				this.parseCandidateResponse(rpsQuestion, candidateResponseString, candidateResponseEntity,
						defaultAnswerOption, optionShuffleSeq);
		if (candidateResponseString != null) {
			switch (questionSequence) {
				case 12:
				case 14:

					if (candidateResponseString.equalsIgnoreCase(RpsConstants.CHOICE1)) {
						Double countOnJ = btScore.get(RpsConstants.COUNT_ON_J);
						if (countOnJ == null) {
							btScore.put(RpsConstants.COUNT_ON_J, RpsConstants.DOUBLE_ONE_VALUE);
						} else {
							btScore.put(RpsConstants.COUNT_ON_J, countOnJ + RpsConstants.DOUBLE_ONE_VALUE);
						}
					}
					if (candidateResponseString.equalsIgnoreCase(RpsConstants.CHOICE2)) {
						Double countOnP = btScore.get(RpsConstants.COUNT_ON_P);
						if (countOnP == null) {
							btScore.put(RpsConstants.COUNT_ON_P, RpsConstants.DOUBLE_ONE_VALUE);
						} else {
							btScore.put(RpsConstants.COUNT_ON_P, countOnP + RpsConstants.DOUBLE_ONE_VALUE);
						}
					}
					break;

				case 13:
				case 15:
					if (candidateResponseString.equalsIgnoreCase(RpsConstants.CHOICE2)) {
						Double countOnJ = btScore.get(RpsConstants.COUNT_ON_J);
						if (countOnJ == null) {
							btScore.put(RpsConstants.COUNT_ON_J, RpsConstants.DOUBLE_ONE_VALUE);
						} else {
							btScore.put(RpsConstants.COUNT_ON_J, countOnJ + RpsConstants.DOUBLE_ONE_VALUE);
						}
					}
					if (candidateResponseString.equalsIgnoreCase(RpsConstants.CHOICE1)) {
						Double countOnP = btScore.get(RpsConstants.COUNT_ON_P);
						if (countOnP == null) {
							btScore.put(RpsConstants.COUNT_ON_P, RpsConstants.DOUBLE_ONE_VALUE);
						} else {
							btScore.put(RpsConstants.COUNT_ON_P, countOnP + RpsConstants.DOUBLE_ONE_VALUE);
						}
					}
					break;

				default:
					break;
			}
		}
		logger.debug("--OUT-- evaluateJAndPPairs() for cand resp id ={} and btScore = {} and questionSequence = {} ",
				candidateResponseId, btScore, questionSequence);
	}

	/**
	 * evaluateRIASECPairs
	 * 
	 * @param btScore
	 * @param questToCandRespMap
	 * @param rpsQuestion
	 * @param questionSequence
	 * @param candidateResponseId
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void evaluateRIASECPairs(Map<String, Double> btScore,
			Map<String, CandidateResponseEntity> questToCandRespMap, RpsQuestion rpsQuestion,
			Integer questionSequence, Integer candidateResponseId, DefaultAnswerOptionEnum defaultAnswerOption,
			Map<String, ArrayList<String>> optionShuffleSeq) {
		logger.debug("--IN-- evaluateRIASECPairs() for cand resp id ={} and btScore = {} and questionSequence = {} ",
				candidateResponseId, btScore, questionSequence);
		String candidateResponseString = null;
		CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(rpsQuestion.getQuestId());
		candidateResponseString =
				this.parseCandidateResponse(rpsQuestion, candidateResponseString, candidateResponseEntity,
						defaultAnswerOption, optionShuffleSeq);
		if (candidateResponseString != null) {
			switch (candidateResponseString) {
				case RpsConstants.CHOICE1:
					Double countOnR = btScore.get(RpsConstants.COUNT_ON_R + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnR == null) {
						btScore.put(RpsConstants.COUNT_ON_R + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_R + rpsQuestion.getchildQuestionSeqNumber(),
								countOnR + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;
				case RpsConstants.CHOICE2:
					Double countOnI = btScore.get(RpsConstants.COUNT_ON_I + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnI == null) {
						btScore.put(RpsConstants.COUNT_ON_I + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_I + rpsQuestion.getchildQuestionSeqNumber(),
								countOnI + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;
				case RpsConstants.CHOICE3:
					Double countOnA = btScore.get(RpsConstants.COUNT_ON_A + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnA == null) {
						btScore.put(RpsConstants.COUNT_ON_A + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_A + rpsQuestion.getchildQuestionSeqNumber(),
								countOnA + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;
				case RpsConstants.CHOICE4:
					Double countOnS = btScore.get(RpsConstants.COUNT_ON_S + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnS == null) {
						btScore.put(RpsConstants.COUNT_ON_S + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_S + rpsQuestion.getchildQuestionSeqNumber(),
								countOnS + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;
				case RpsConstants.CHOICE5:
					Double countOnE = btScore.get(RpsConstants.COUNT_ON_E + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnE == null) {
						btScore.put(RpsConstants.COUNT_ON_E + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_E + rpsQuestion.getchildQuestionSeqNumber(),
								countOnE + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;
				case RpsConstants.CHOICE6:
					Double countOnC = btScore.get(RpsConstants.COUNT_ON_C + rpsQuestion.getchildQuestionSeqNumber());
					if (countOnC == null) {
						btScore.put(RpsConstants.COUNT_ON_C + rpsQuestion.getchildQuestionSeqNumber(),
								RpsConstants.DOUBLE_ONE_VALUE);
					} else {
						btScore.put(RpsConstants.COUNT_ON_C + rpsQuestion.getchildQuestionSeqNumber(),
								countOnC + RpsConstants.DOUBLE_ONE_VALUE);
					}
					break;

				default:
					break;
			}
		}
		logger.debug("--OUT-- evaluateRIASECPairs() for cand resp id ={} and btScore = {} and questionSequence = {} ",
				candidateResponseId, btScore, questionSequence);
	}

	/**
	 * parseCandidateResponse
	 * 
	 * @param rpsQuestion
	 * @param candidateResponseString
	 * @param candidateResponseEntity
	 * @param defaultAnswerOption
	 * @param optionShuffleSeq
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String parseCandidateResponse(RpsQuestion rpsQuestion, String candidateResponseString,
			CandidateResponseEntity candidateResponseEntity, DefaultAnswerOptionEnum defaultAnswerOption,
			Map<String, ArrayList<String>> optionShuffleSeq) {
		if (candidateResponseEntity != null) {
			Map<String, String> choicesMap = candidateResponseEntity.getResponse();
			if (choicesMap != null && !choicesMap.isEmpty()) {
				Set<String> choiceOptions = choicesMap.keySet();
				if (choiceOptions != null && !choiceOptions.isEmpty()) {
					Iterator<String> chIt = choiceOptions.iterator();
					candidateResponseString = choicesMap.get(chIt.next());
				}

			}
		} else {
			// if auto-attempted is enabled then Default Answer assigned to candidateResponseString from BR
			// Rule
			if (defaultAnswerOption != null) {
				List<String> optionsForQuest = optionShuffleSeq.get(rpsQuestion.getQuestId());
				candidateResponseString = getDefaultAnswer(optionsForQuest.size(), defaultAnswerOption);
			}
		}
		return candidateResponseString;
	}

	/**
	 * getDefaultAnswer
	 * 
	 * @param optionCount
	 * @param defaultAnswerOption
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getDefaultAnswer(int optionCount, DefaultAnswerOptionEnum defaultAnswerOption) {
		String defaultAnswer = "";
		switch (defaultAnswerOption) {
			case FIRST:
				defaultAnswer = RpsConstants.CHOICE + defaultAnswerOption.getValue();
				break;
			case LAST:
				defaultAnswer = RpsConstants.CHOICE + (optionCount + defaultAnswerOption.getValue());
				break;
			case SECOND:
				defaultAnswer = RpsConstants.CHOICE + defaultAnswerOption.getValue();
				break;
			case SECONDLAST:
				defaultAnswer = RpsConstants.CHOICE + (optionCount + defaultAnswerOption.getValue());
				break;
		}
		return defaultAnswer;
	}
}
