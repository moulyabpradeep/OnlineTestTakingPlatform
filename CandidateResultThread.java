/**
 * Name : MOULYA BANGALORE PRADEEP
 *
 * This complete file is coded by me.
 * This file is part of a large project.
 * I am not able to import complete project to github at one shot.
 * GitHub is asking me to upload only 100 files at once whereas this project has 8000 files in total.
 * Hence I am uploading only relevant files.
 *
 * Explanation of Code: This project is about the result calculation of online test taking platform.
 * This file has a cron job which pulls all candidates' responses whose scores are not evaluated from database.
 * Each and every question type has different kind of evaluation technique.
 * This file calculates the scores and saves to database.
 *
 */
package com.merittrac.apollo.rps.core;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.ApolloUtility;
import com.merittrac.apollo.common.BehavioralTestType;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ScoringLogicBean;
import com.merittrac.apollo.common.ScoringLogicForAllQuestionTypes;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.entities.acs.ResponseMarkBean;
import com.merittrac.apollo.common.resultcomputation.entity.SectionalScoreData;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsBehaviouralTest;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateResponseLite;
import com.merittrac.apollo.data.entity.RpsCandidateResponses;
import com.merittrac.apollo.data.entity.RpsMasterAssociationLite;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociationLite;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsSectionCandidateResponse;
import com.merittrac.apollo.data.entity.RpsWetScoreEvaluation;
import com.merittrac.apollo.data.repository.CandidateResponseLiteRepository;
import com.merittrac.apollo.data.repository.RpsCandidateResponsesRepository;
import com.merittrac.apollo.data.repository.RpsQpSectionRepository;
import com.merittrac.apollo.data.repository.RpsQuestionAssociationRepository;
import com.merittrac.apollo.data.repository.RpsSectionCandidateResponseRepository;
import com.merittrac.apollo.data.repository.RpsWetDataEvaluationRepository;
import com.merittrac.apollo.qpd.qpgenentities.DefaultAnswerOptionEnum;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.MarksAndScoreRules;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.SectionLevelRules;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.TimeBasedRules;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.reports.ExportTiffService;
import com.merittrac.apollo.rps.services.BehavioralScoringLogicService;
import com.merittrac.apollo.rps.services.CGTScoringService;
import com.merittrac.apollo.rps.services.bean.TypingTestEvaluationResultBean;
import com.merittrac.apollo.rps.ui.openevent.entity.TypingTestEntity;
import com.merittrac.apollo.rps.utility.CompareTextUtility;
import com.merittrac.apollo.rps.utility.TextDiffMatchPatch;
import com.merittrac.apollo.rps.utility.TextToUnicode;

public class CandidateResultThread implements Callable<RpsCandidateResponseLite> {
	private Map<String, RpsQuestion> globalQuestionsMap;
	private RpsCandidateResponseLite candidateResponse;
	private String resultComputationStatus;
	private RpsCandidateResponsesRepository candidateResponsesRepository;
	private List<String> candidateShuffleSeq;
	private RpsMasterAssociationLite masterAssociation;
	private RpsBatchAcsAssociation batchAcsAssociation;
	private RpsCandidate rpscandidate;
	private RpsQuestionPaper questionPaper;
	private List<RpsQuestionAssociationLite> questionAssociationList;
	private RpsAssessment assessment;
	private ExportTiffService exportTiffService;
	private CandidateResponseLiteRepository candidateResponseLiteRepository;
	private RpsSectionCandidateResponseRepository rpsSectionCandidateResponseRepository;
	private RpsQpSectionRepository rpsQpSectionRepository;
	private RpsWetDataEvaluationRepository rpsWetDataEvaluationRepository;
	private DefaultAnswerOptionEnum defaultAnswerOption;
	private Map<String, ArrayList<String>> optionShuffleSeq;
	private RpsBehaviouralTest rpsBehaviouralTest;
	private Map<String, SectionLevelRules> sectionLevelRulesMap;
	private Map<String, String> questionSectionMap = new LinkedHashMap<String, String>();
	private boolean isMarkedForReviewQuestionsToBeEvaluated;
	private CGTScoringService cgtScoringService;
	private BehavioralScoringLogicService behavioralScoringLogicService;

	private static Logger logger = LoggerFactory.getLogger(CandidateResultThread.class);
	public static int CRONCOMPLETED = 1;
	public static int CRONFAILED = 3;
	public static int INVALIDSTATUS = 4;

	public CandidateResultThread(Map<String, RpsQuestion> globalQuestionsMap, RpsCandidateResponseLite candidateResponse) {
		this.globalQuestionsMap = globalQuestionsMap;
		this.candidateResponse = candidateResponse;
	}

	public CandidateResultThread(Map<String, RpsQuestion> globalQuestionsMap,
			RpsCandidateResponseLite candidateResponse, String resultComputationStatus) {
		this.globalQuestionsMap = globalQuestionsMap;
		this.candidateResponse = candidateResponse;
		this.resultComputationStatus = resultComputationStatus;
	}

	public CandidateResultThread(Map<String, RpsQuestion> globalQuestionsMap,
			RpsCandidateResponseLite candidateResponse, String resultComputationStatus,
			RpsCandidateResponsesRepository candidateResponsesRepository,
			CandidateResponseLiteRepository candidateResponseLiteRepository, List<String> candidateShuffleSeq,
			RpsMasterAssociationLite masterAssociation, RpsBatchAcsAssociation batchAcsAssociation,
			RpsCandidate rpscandidate, RpsQuestionPaper questionPaper,
			List<RpsQuestionAssociationLite> questionAssociationList, RpsAssessment assessment,
			ExportTiffService exportTiffService,
			RpsSectionCandidateResponseRepository rpsSectionCandidateResponseRepository,
			RpsWetDataEvaluationRepository rpsWetDataEvaluationRepository, DefaultAnswerOptionEnum defaultAnswerOption,
			Map<String, ArrayList<String>> optionShuffleSeq, RpsBehaviouralTest rpsBehaviouralTest,
			RpsQuestionAssociationRepository rpsQuestionAssociationRepository,
			Map<String, SectionLevelRules> sectionLevelRulesMap, Map<String, String> questionSectionMap,
			boolean isMarkedForReviewQuestionsToBeEvaluated, CGTScoringService cgtScoringService,
			RpsQpSectionRepository rpsQpSectionRepository,
			BehavioralScoringLogicService behavioralScoringLogicService) {
		super();
		this.globalQuestionsMap = globalQuestionsMap;
		this.candidateResponse = candidateResponse;
		this.resultComputationStatus = resultComputationStatus;
		this.candidateResponsesRepository = candidateResponsesRepository;
		this.candidateResponseLiteRepository = candidateResponseLiteRepository;
		this.candidateShuffleSeq = candidateShuffleSeq;
		this.masterAssociation = masterAssociation;
		this.batchAcsAssociation = batchAcsAssociation;
		this.rpscandidate = rpscandidate;
		this.questionPaper = questionPaper;
		this.questionAssociationList = questionAssociationList;
		this.assessment = assessment;
		this.exportTiffService = exportTiffService;
		this.rpsSectionCandidateResponseRepository = rpsSectionCandidateResponseRepository;
		this.rpsWetDataEvaluationRepository = rpsWetDataEvaluationRepository;
		this.defaultAnswerOption = defaultAnswerOption;
		this.optionShuffleSeq = optionShuffleSeq;
		this.rpsBehaviouralTest = rpsBehaviouralTest;
		// this.rpsQuestionAssociationRepository = rpsQuestionAssociationRepository;
		this.sectionLevelRulesMap = sectionLevelRulesMap;
		this.questionSectionMap = questionSectionMap;
		this.isMarkedForReviewQuestionsToBeEvaluated = isMarkedForReviewQuestionsToBeEvaluated;
		this.cgtScoringService = cgtScoringService;
		this.rpsQpSectionRepository=rpsQpSectionRepository;
		this.behavioralScoringLogicService = behavioralScoringLogicService;
	}

	/**
	 * Worker thread Api that computes the individual question score and hence the total score for a given candidate.
	 * The thread is supplied with the candidate responses which are validated against the correct answers in the DB.
	 *
	 * @throws RpsException
	 *             and the processing is stopped.
	 */
	@Override
	public RpsCandidateResponseLite call() throws Exception {
		String candidateJsonResponse = candidateResponse.getResponse();
		boolean descriptiveQuestion=false;
		Map<String, SectionalScoreData> sectionScoreMap = new HashMap<String, SectionalScoreData>();
		double totalScore = 0.0;
		int totalCorrect = 0;
		boolean isScoreRegenerate = true;
		boolean isCronFailedOrInvalid = false;
		Map<String, CandidateResponseEntity> parentResponseMap = new HashMap<String, CandidateResponseEntity>();
		Map<String, CandidateResponseEntity> reGenParentResponseMap = new HashMap<String, CandidateResponseEntity>();
		// check if the score is re-generated
		isScoreRegenerate =
				resultComputationStatus.equalsIgnoreCase(RpsConstants.RESULT_COMPUTE_STATUS.RESULT_COMPUTED.toString());

		try {
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);

			Map<String, CandidateResponseEntity> questToCandRespMap = new HashMap<>();
			Map<String, CandidateResponseEntity> questToCandRespForMCQWMap = new HashMap<>();
			if (candidateResponseEntityList != null && !candidateResponseEntityList.isEmpty()) {
				for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntityList) {
					if (!(!isMarkedForReviewQuestionsToBeEvaluated && candidateResponseEntity.isMarkForReview())) {
						// marked for review is false and get only unmarked questions
						questToCandRespMap.put(candidateResponseEntity.getQuestionID(), candidateResponseEntity);
					}
					if (candidateResponseEntity.getQuestionType().equalsIgnoreCase(
							QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString()))
						if (!(!isMarkedForReviewQuestionsToBeEvaluated && candidateResponseEntity.isMarkForReview()))
							questToCandRespForMCQWMap.put(candidateResponseEntity.getQuestionID(),
									candidateResponseEntity);

					if (rpsBehaviouralTest != null
							&& (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSG.toString()) || rpsBehaviouralTest
											.getBtId().equalsIgnoreCase(BehavioralTestType.IDSB.toString())
									|| rpsBehaviouralTest.getBtId()
											.equalsIgnoreCase(BehavioralTestType.IDSA.toString()))
							&& candidateResponseEntity.getQuestionType().equalsIgnoreCase(
									QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
						if (!(!isMarkedForReviewQuestionsToBeEvaluated && candidateResponseEntity.isMarkForReview()))
							questToCandRespForMCQWMap.put(candidateResponseEntity.getQuestionID(),
									candidateResponseEntity);

					}
				}
			}

			String finalResponse = "";
			int count = 0;

			// Collection<String> questIdList = candidateShuffleSeq.values();
			Set<String> shuffleSequenceSet = null;
			Map<Integer, String> shuffleSequenceMap = null;
			int questionsAttempted = 0;
			RpsCandidateResponses candidateRes = new RpsCandidateResponses();
			if (candidateShuffleSeq != null && !candidateShuffleSeq.isEmpty())
				shuffleSequenceSet = new LinkedHashSet<String>(candidateShuffleSeq);
			else if (candidateShuffleSeq.isEmpty() && questToCandRespMap.isEmpty())
				shuffleSequenceSet = new LinkedHashSet<String>(candidateShuffleSeq);
			else if (candidateShuffleSeq == null || candidateShuffleSeq.isEmpty()) {
				shuffleSequenceSet = new LinkedHashSet<String>();
				shuffleSequenceMap = new HashMap<Integer, String>();
				for (RpsQuestionAssociationLite rpsQuestionAssociation : this.questionAssociationList) {
					shuffleSequenceSet.add(rpsQuestionAssociation.getRpsQuestion().getQuestId());
					if (rpsBehaviouralTest != null)
						shuffleSequenceMap.put(rpsQuestionAssociation.getQuestionSequence(), rpsQuestionAssociation
								.getRpsQuestion().getQuestId());
				}
			}
			if (shuffleSequenceSet != null && !shuffleSequenceSet.isEmpty()) {
				for (String questionId : shuffleSequenceSet) {
					count++;
					double individualQuestionScore = 0.0;
					String candidateResponseString = null;

					RpsQuestion question = globalQuestionsMap.get(questionId);
					if (question == null) {
						logger.error("No question found in DB by question code::" + questionId
								+ " for the question paper with DB Id :: " + candidateResponse.getRpsQuestionPaper()
								+ " and candidate response id :: " + candidateResponse.getCandidateResponseId());
						candidateResponse.setCronStatus(INVALIDSTATUS);
						isCronFailedOrInvalid = true;
						break;
					}

					CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(questionId);
					boolean isCandResponse = false;
					if (candidateResponseEntity != null) {
						isCandResponse = true;
						Map<String, String> choicesMap = candidateResponseEntity.getResponse();
						if (choicesMap != null && !choicesMap.isEmpty()) {
							Set<String> choiceOptions = choicesMap.keySet();
							if (choiceOptions != null && !choiceOptions.isEmpty()) {
								Iterator<String> chIt = choiceOptions.iterator();
								// In case of MOQ, iterate
								while(chIt.hasNext()) {
									if (candidateResponseString == null)
										candidateResponseString = choicesMap.get(chIt.next());
									else
										candidateResponseString = candidateResponseString  + ", " + choicesMap.get(chIt.next());
								}
							}

						}
					} else {
						// if auto-attempted is enabled then Default Answer assigned to candidateResponseString from BR
						// Rule

						if (defaultAnswerOption != null) {
							List<String> optionsForQuest = optionShuffleSeq.get(questionId);

							int noOfOptions = 0;
							if (optionsForQuest != null)
								noOfOptions = optionsForQuest.size();
							else
								noOfOptions = 4;
							candidateResponseString = getDefaultAnswer(noOfOptions, defaultAnswerOption);

						}
					}

					String responseDetail = question.getQuestId().substring(1) + "--" + question.getQuestId() + "--";
					String sectionid = null;
					if (candidateResponseEntity == null) {
						sectionid = questionSectionMap.get(question.getQuestId());
						candidateResponseEntity = new CandidateResponseEntity();
						isCandResponse = false;
						// candidateResponseEntity.setCandResponse(false);
						Map<String, String> candidateResponseMap = new HashMap<>();
						if (candidateResponseString != null)
							candidateResponseMap.put(question.getQuestId(), candidateResponseString);
						candidateResponseEntity.setResponse(candidateResponseMap);
						candidateResponseEntity.setSectionID(sectionid);
						if (question.getParentqID() != null)
							candidateResponseEntity.setParentQuestionID(question.getParentqID().getQuestId());
					} else
						sectionid = candidateResponseEntity.getSectionID();
					SectionalScoreData sectionalScoreData = sectionScoreMap.get(sectionid);
					if (sectionalScoreData == null)
						sectionalScoreData = new SectionalScoreData();
					// try {
					if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.READING_COMPREHENSION.toString())) {
						reGenParentResponseMap.put(questionId, candidateResponseEntity);
						if (count == candidateShuffleSeq.size()) {
							candidateResponse.setFinalResult(finalResponse);
						}
						continue;
					} else if (question.getQuestionType().equalsIgnoreCase(QuestionType.SURVEY.toString())) {
						reGenParentResponseMap.put(questionId, candidateResponseEntity);
						if (count == candidateShuffleSeq.size()) {
							candidateResponse.setFinalResult(finalResponse);
						}
						continue;
					} else if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString())) {
						logger.info("-- IN MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION--");
						if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
							questionsAttempted++;
							sectionalScoreData
									.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--"
											+ candidateResponseString;
							Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
							}.getType();
							String responseMarkBeansJson = question.getQans();
							List<ResponseMarkBean> responseMarkBeans =
									new Gson().fromJson(responseMarkBeansJson, responseMarkType);

							if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
								for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
									if (responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString)) {
										sectionalScoreData.setSecQuestionsCorrect(sectionalScoreData
												.getSecQuestionsCorrect() + 1);
										individualQuestionScore = responseMarkBean.getResponsePositiveMarks();
										totalCorrect++;
										break;
									}
								}
							} else
								logger.error("responseMarkBeans is null or empty--" + responseMarkBeans);
						} else {
							responseDetail = question.getQuestId().substring(1) + "--" + question.getQuestId() + "--NA";
						}

						totalScore += individualQuestionScore;
						if (candidateResponseEntity != null) {
							candidateResponseEntity.setScore(individualQuestionScore);
							sectionalScoreData.setCandSecScore(individualQuestionScore
									+ sectionalScoreData.getCandSecScore());
							sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);
							String parentQid = candidateResponseEntity.getParentQuestionID();
							if (parentQid == null || parentQid.isEmpty()) {
								logger.info("current question is normal question and not a RC child as parent id is null or empty for the question-- "
										+ questionId);
							} else
								logger.error("MCQW Question can not have parentQid----");
						}

						if (count == candidateShuffleSeq.size())
							finalResponse = finalResponse + responseDetail;
						else
							finalResponse = finalResponse + responseDetail + ", ";

						if (count == candidateShuffleSeq.size()) {
							candidateResponse.setFinalResult(finalResponse);
						}
						logger.info("-- IN MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION--");
						continue;
					} else if (question.getQuestionType()
							.equalsIgnoreCase(QuestionType.WRITTEN_ENGLISH_TEST.toString())) {
						logger.info("-- IN WRITTEN_ENGLISH_TEST--");
						if (candidateResponseEntity.getCandResponse() != null) {
							questionsAttempted++;
						}
						sectionalScoreData.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
						sectionalScoreData.setSecQuestionsCorrect(sectionalScoreData.getSecQuestionsCorrect() + 1);
						responseDetail =
								question.getQuestId().substring(1) + "--" + question.getQuestId() + "--" + "WETA";

						RpsWetScoreEvaluation rpsWetScoreEvaluation =
								rpsWetDataEvaluationRepository.getByUniquecandidateIdAndQId(
										candidateResponse.getRpsMasterAssociation(), question.getQuestId());
						if (rpsWetScoreEvaluation != null
								&& rpsWetScoreEvaluation.getEvaluated() == RpsConstants.EVALUATED) {
							totalScore += rpsWetScoreEvaluation.getScore();
							if (candidateResponseEntity != null) {
								sectionalScoreData.setCandSecScore(
										rpsWetScoreEvaluation.getScore() + sectionalScoreData.getCandSecScore());
								sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);
							}
						}
						logger.info("-- OUT WRITTEN_ENGLISH_TEST--");
						continue;
					} else if (question.getQuestionType().equalsIgnoreCase(QuestionType.TYPING_TEST.toString())) {
						logger.info("-- IN TYPING_TEST--");

						String questText = question.getQtext();
						String responseText = null;
						Map<String, String> responseMap = candidateResponseEntity.getResponse();
						if (responseMap != null)
							responseText = responseMap.get(RpsConstants.TYPING_TEST_KEY);
						if (responseText != null) {
							questionsAttempted++;
							sectionalScoreData
									.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
							int sectionDuration = 0;

							if (sectionLevelRulesMap != null) {
								SectionLevelRules sectionLevelRules =
										sectionLevelRulesMap.get(candidateResponseEntity.getSectionID());
								if (sectionLevelRules != null) {
									TimeBasedRules timeBasedRules = sectionLevelRules.getTimeBasedRules();
									if (timeBasedRules != null) {
										sectionDuration = timeBasedRules.getSectionDuration() == null ? 0
												: timeBasedRules.getSectionDuration().intValue();
									}
								}
							}
							String typingTestEvaluationJson =
									this.typingTestEvaluation(questText, responseText, sectionDuration);
							candidateResponse.setTypingTestParamsJson(typingTestEvaluationJson);

							logger.info("-- OUT TYPING_TEST--");
							continue;
						}
						continue;
					} else if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.DATA_ENTRY.toString())) {
						if (candidateResponseEntity != null && isCandResponse) {
							questionsAttempted++;
							sectionalScoreData
									.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--"
											+ candidateResponseString;

							Type listType = new TypeToken<List<ResponseMarkBean>>() {
							}.getType();

							List<ResponseMarkBean> responseMarkBeans;
							try {
								responseMarkBeans = new Gson().fromJson(question.getQans(), listType);
							} catch (Exception e) {
								logger.error("Unable to Parse Candidate Response for Data Entry Question");
								String errMsg =
										"Could not process results for candidate with candidateResponseRow::"
												+ candidateResponse.getCandidateResponseId();
								logger.error(errMsg, e);
								candidateResponse.setCronStatus(CRONFAILED);
								break;
							}

							for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
								if (candidateResponseEntity.getResponse().containsKey(responseMarkBean.getResponse())) {
									Map<String, String> responseMap = candidateResponseEntity.getResponse();
									String candidateAns = responseMap.get(responseMarkBean.getResponse());
									String responseAnswer = responseMarkBean.getResponseAnswer();
									Double answerKey = 0.0;
									boolean isDoubleValue = false;
									try {
										answerKey = Double.parseDouble(responseAnswer);
										isDoubleValue = true;
									} catch (NumberFormatException e) {
										// its String value
										isDoubleValue = false;
									}
									if (isDoubleValue) {
										Double cAns = 0.0;
										try {
											cAns = Double.parseDouble(candidateAns);
										} catch (NumberFormatException e) {
											// its wrong answer, expecting double value
											individualQuestionScore += getNegativeMarkingForResponse(question,
													candidateResponseEntity, responseMarkBean);
											continue;
										}
										if (cAns.doubleValue() == answerKey.doubleValue()) {
											sectionalScoreData.setSecQuestionsCorrect(
													sectionalScoreData.getSecQuestionsCorrect() + 1);
											individualQuestionScore += responseMarkBean.getResponsePositiveMarks();
											totalCorrect++;
										} else
											individualQuestionScore += getNegativeMarkingForResponse(question,
													candidateResponseEntity, responseMarkBean);
									} else {
										if (candidateAns.equalsIgnoreCase(responseAnswer)) {
											sectionalScoreData.setSecQuestionsCorrect(
													sectionalScoreData.getSecQuestionsCorrect() + 1);
											individualQuestionScore += responseMarkBean.getResponsePositiveMarks();
											totalCorrect++;
										} else
											individualQuestionScore += getNegativeMarkingForResponse(question,
													candidateResponseEntity, responseMarkBean);
									}
								}
								// else
								// individualQuestionScore +=
								// getNegativeMarkingForResponse(question, candidateResponseEntity,
								// responseMarkBean);
							}
						} else {
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--DENA";
						}
						totalScore += individualQuestionScore;
						if (candidateResponseEntity != null) {
							candidateResponseEntity.setScore(individualQuestionScore);
							sectionalScoreData.setCandSecScore(individualQuestionScore
									+ sectionalScoreData.getCandSecScore());
							sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);
							String parentQid = candidateResponseEntity.getParentQuestionID();
							if (parentQid == null || parentQid.isEmpty()) {
								logger.info("current question is normal question and not a RC child as parent id is null or empty for the question-- "
										+ questionId);
							} else {
								logger.info("current question is a RC child as parent id is exists for the question in responses-- "
										+ questionId);
								RpsQuestion parentRpsQuestion = globalQuestionsMap.get(parentQid);
								if (parentRpsQuestion == null) {
									logger.error("No question found in DB by question code::" + parentQid
											+ " for the question paper with DB Id "
											+ candidateResponse.getRpsQuestionPaper());
									candidateResponse.setCronStatus(INVALIDSTATUS);
									isCronFailedOrInvalid = true;
									break;
								} else {
									if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
											QuestionType.READING_COMPREHENSION.toString()))
										createParentResponses(parentResponseMap, parentQid, individualQuestionScore);
									else if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
											QuestionType.SURVEY.toString()))
										createParentResponsesForSurvey(parentResponseMap, parentQid,
												individualQuestionScore);
									else {
										logger.error("question : " + parentQid + " has question type : "
												+ parentRpsQuestion.getQuestionType()
												+ " but question type is expected as READING_COMPREHENSION/SURVEY");
										candidateResponse.setCronStatus(INVALIDSTATUS);
										isCronFailedOrInvalid = true;
										break;
									}
								}
							}
						}

						if (count == candidateShuffleSeq.size())
							finalResponse = finalResponse + responseDetail;
						else
							finalResponse = finalResponse + responseDetail + ", ";

						if (count == candidateShuffleSeq.size()) {
							candidateResponse.setFinalResult(finalResponse);
						}

						continue;
					} else if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.MULTIPLE_OPTIONS_QUESTION.toString())) { 
						if (candidateResponseEntity != null) {
							if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
								int secQuestionsCorrect = sectionalScoreData.getSecQuestionsCorrect();
								questionsAttempted++;
								MarksAndScoreRules marksAndScoreRules =
										getMarksAndScoreRules(question, candidateResponseEntity);
								ScoringLogicBean scoringLogicBean = ScoringLogicForAllQuestionTypes.getScoreForMOQ(
										candidateResponseEntity.getResponse(), question.getQans(), totalCorrect,
										secQuestionsCorrect, marksAndScoreRules, question.getQuestionType(),
										question.getQid(), question.getScore(), question.getNegativeScore());

								if (scoringLogicBean != null) {
									individualQuestionScore += scoringLogicBean.getScore();
									totalCorrect = scoringLogicBean.getTotalCorrect();
									candidateResponseEntity.setScore(individualQuestionScore);
									sectionalScoreData.setSecQuestionsCorrect(scoringLogicBean.getSecQuestionsCorrect());
									sectionalScoreData.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
									sectionalScoreData.setCandSecScore(scoringLogicBean.getScore() + sectionalScoreData.getCandSecScore());
									totalScore += scoringLogicBean.getScore();
								}
								sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);

								responseDetail =
										question.getQuestId().substring(1) + "--" + question.getQuestId() + "--"
												+ candidateResponseString;
							} else {
								responseDetail = question.getQuestId().substring(1) + "--" + question.getQuestId() + "--NA";
							}

						}
						continue;
					}
					else if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.FILL_IN_THE_BLANK.toString())) {
						if (candidateResponseEntity != null && isCandResponse) {
							sectionalScoreData
									.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--FITBA";

							questionsAttempted++;// = candidateResponseEntity.getResponse().size();
							int secQuestionsCorrect = sectionalScoreData.getSecQuestionsCorrect();
							MarksAndScoreRules marksAndScoreRules =
									getMarksAndScoreRules(question, candidateResponseEntity);

							// fix for :Score is not matching in ACS & RPS for IVT, if response has symbols
							Type listType = new TypeToken<List<ResponseMarkBean>>() {
							}.getType();

							if (question.getQans() != null && !question.getQans().isEmpty()) {
								List<ResponseMarkBean> responseMarkBeans =
										new Gson().fromJson(question.getQans(), listType);
								for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
									// unescape the responseAnswer
									responseMarkBean.setResponseAnswer(StringEscapeUtils.unescapeXml(responseMarkBean
											.getResponseAnswer()));
								}
								// set it back with unescaped responseAnswer
								question.setQans(new Gson().toJson(responseMarkBeans));
							}
							//
							ScoringLogicBean scoringLogicBean =
									ScoringLogicForAllQuestionTypes.scoringForFITB(
											candidateResponseEntity.getResponse(), question.getQans(), totalCorrect,
											secQuestionsCorrect, marksAndScoreRules, question.getQuestionType(),
											question.getQid(), question.getScore(), question.getNegativeScore());
							if (scoringLogicBean != null) {
								individualQuestionScore += scoringLogicBean.getScore();
								totalCorrect = scoringLogicBean.getTotalCorrect();
								secQuestionsCorrect = scoringLogicBean.getSecQuestionsCorrect();
								sectionalScoreData.setSecQuestionsCorrect(secQuestionsCorrect);
							}
							// else add zero to individualQuestionScore
						} else {
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--FITBNA";
						}
						totalScore += individualQuestionScore;
						if (candidateResponseEntity != null) {
							candidateResponseEntity.setScore(individualQuestionScore);
							sectionalScoreData.setCandSecScore(individualQuestionScore
									+ sectionalScoreData.getCandSecScore());
							sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);
							String parentQid = candidateResponseEntity.getParentQuestionID();
							if (parentQid == null || parentQid.isEmpty()) {
								logger.info("current question is normal question and not a RC child as parent id is null or empty for the question-- "
										+ questionId);
							} else {
								logger.info("current question is a RC child as parent id is exists for the question in responses-- "
										+ questionId);
								RpsQuestion parentRpsQuestion = globalQuestionsMap.get(parentQid);
								if (parentRpsQuestion == null) {
									logger.error("No question found in DB by question code::" + parentQid
											+ " for the question paper with DB Id "
											+ candidateResponse.getRpsQuestionPaper());
									candidateResponse.setCronStatus(INVALIDSTATUS);
									isCronFailedOrInvalid = true;
									break;
								} else {
									if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
											QuestionType.READING_COMPREHENSION.toString()))
										createParentResponses(parentResponseMap, parentQid, individualQuestionScore);
									else if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
											QuestionType.SURVEY.toString()))
										createParentResponsesForSurvey(parentResponseMap, parentQid,
												individualQuestionScore);
									else {
										logger.error("question : " + parentQid + " has question type : "
												+ parentRpsQuestion.getQuestionType()
												+ " but question type is expected as READING_COMPREHENSION");
										candidateResponse.setCronStatus(INVALIDSTATUS);
										isCronFailedOrInvalid = true;
										break;
									}
								}
							}
						}

						if (count == candidateShuffleSeq.size())
							finalResponse = finalResponse + responseDetail;
						else
							finalResponse = finalResponse + responseDetail + ", ";

						if (count == candidateShuffleSeq.size()) {
							candidateResponse.setFinalResult(finalResponse);
						}

						continue;
					}

					String correctAnswer = question.getQans().toUpperCase();
					String alternateCorrectAnswers = question.getAlternateQAns();
					if (alternateCorrectAnswers != null)
						alternateCorrectAnswers = alternateCorrectAnswers.toUpperCase();
					String[] alternateCorrectAnswersArray = null;
					if (alternateCorrectAnswers != null)
						alternateCorrectAnswersArray = alternateCorrectAnswers.split(",");
					List<String> correctAnswers = new ArrayList<>();
					correctAnswers.add(correctAnswer);
					if(alternateCorrectAnswersArray != null && alternateCorrectAnswersArray.length > 0)
						correctAnswers.addAll(Arrays.asList(alternateCorrectAnswersArray));

					if (correctAnswer == null && correctAnswers.size() > 0
							&& !(QuestionType.DESCRIPTIVE_QUESTION.toString().equalsIgnoreCase(question
									.getQuestionType()))) {
						individualQuestionScore = 0.0;
						candidateResponse.setCronStatus(INVALIDSTATUS);
						isCronFailedOrInvalid = true;
						break;
					} else if (candidateResponseString != null
							&& correctAnswers.contains(candidateResponseString.toUpperCase())) {
						if (question.getScore() == null) {
							logger.error("Score cannot be null");
							candidateResponse.setCronStatus(INVALIDSTATUS);
							isCronFailedOrInvalid = true;
							break;
						}
						individualQuestionScore = question.getScore();
						totalCorrect++;
						sectionalScoreData.setSecQuestionsCorrect(sectionalScoreData.getSecQuestionsCorrect() + 1);
						sectionalScoreData.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
					} else if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
						sectionalScoreData.setSecQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted() + 1);
						individualQuestionScore =
								getNegativeMarkingForResponse(question, candidateResponseEntity, null);
					}
					totalScore += individualQuestionScore;
					if (candidateResponseEntity != null) {
						candidateResponseEntity.setScore(individualQuestionScore);
						sectionalScoreData.setCandSecScore(individualQuestionScore
								+ sectionalScoreData.getCandSecScore());
						sectionScoreMap.put(candidateResponseEntity.getSectionID(), sectionalScoreData);
						String parentQid = candidateResponseEntity.getParentQuestionID();

						if (parentQid == null || parentQid.isEmpty()) {
							logger.info("current question is normal question and not a RC child as parent id is null or empty for the question-- "
									+ questionId);
						} else {
							logger.info("current question is a RC child as parent id is exists for the question in responses-- "
									+ questionId);
							RpsQuestion parentRpsQuestion = globalQuestionsMap.get(parentQid);
							if (parentRpsQuestion == null) {
								logger.error("No question found in DB by question code::" + parentQid
										+ " for the question paper with DB Id "
										+ candidateResponse.getRpsQuestionPaper());
								candidateResponse.setCronStatus(INVALIDSTATUS);
								isCronFailedOrInvalid = true;
								break;
							} else {
								if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
										QuestionType.READING_COMPREHENSION.toString()))
									createParentResponses(parentResponseMap, parentQid, individualQuestionScore);
								else if (parentRpsQuestion.getQuestionType().equalsIgnoreCase(
										QuestionType.SURVEY.toString()))
									createParentResponsesForSurvey(parentResponseMap, parentQid,
											individualQuestionScore);
								else {
									logger.error("question : " + parentQid + " has question type : "
											+ parentRpsQuestion.getQuestionType()
											+ " but question type is expected as READING_COMPREHENSION");
									candidateResponse.setCronStatus(INVALIDSTATUS);
									isCronFailedOrInvalid = true;
									break;
								}
							}
						}
					}

					if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.DESCRIPTIVE_QUESTION.toString())) {
						descriptiveQuestion=true;
						if (candidateResponseString != null
								&& ExportTiffService.findWordCount(candidateResponseString) > 0) {
							questionsAttempted++;
							responseDetail = question.getQuestId().substring(1) + "--" + question.getQuestId() + "--DA";
						} else {
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--DNA";
						}
					} else if (question.getQuestionType().equalsIgnoreCase(
							QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
						if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
							questionsAttempted++;
							responseDetail =
									question.getQuestId().substring(1) + "--" + question.getQuestId() + "--"
											+ candidateResponseString;
						} else {
							if (candidateResponseString != null && !candidateResponseString.isEmpty())
								questionsAttempted++;
							responseDetail = question.getQuestId().substring(1) + "--" + question.getQuestId() + "--NA";
						}
					}
					if (count == candidateShuffleSeq.size())
						finalResponse = finalResponse + responseDetail;
					else
						finalResponse = finalResponse + responseDetail + ", ";

					if (count == candidateShuffleSeq.size()) {
						candidateResponse.setFinalResult(finalResponse);
					}
				}
				String batchCode = batchAcsAssociation.getRpsBatch().getBatchName();
				String acsServerName = batchAcsAssociation.getRpsAcsServer().getAcsServerName();
				Date batchStartTime = batchAcsAssociation.getRpsBatch().getBatchStartTime().getTime();
				RpsAcsServer rpsAcsServer = batchAcsAssociation.getRpsAcsServer();

				DateFormat dateFormatter = new SimpleDateFormat("ddMMyyyy");
				DateFormat timeFormatter = new SimpleDateFormat("hh:mma");
				String date = dateFormatter.format(batchStartTime);
				String time = timeFormatter.format(batchStartTime);
				String batchName =
						acsServerName + "_" + date + "_" + batchCode + "_" + time + "_"
								+ assessment.getAssessmentCode();
				candidateRes.setBatchName(batchName);

				candidateRes.setAssessmentId(questionPaper.getUniqueQPID());
				candidateRes.setBatchCode(batchCode);
				candidateRes.setExamCenterCode(rpsAcsServer.getRpsVenue().getVenueCode());
				candidateRes.setExamDate(batchAcsAssociation.getRpsBatch().getBatchStartTime());
				candidateRes.setLoginName(masterAssociation.getLoginID() == null ? "Not available" : masterAssociation
						.getLoginID());
				candidateRes.setRpsMasterAssociation(masterAssociation.getUniqueCandidateId());
				candidateRes.setScheduleUserId(rpscandidate.getScheduledId());
				candidateRes.setResponses(finalResponse);
				int eventcodeIndex =
						assessment.getAssessmentCode().lastIndexOf("_" + rpscandidate.getRpsEvent().getEventCode());
				String assessmentCode = assessment.getAssessmentCode().substring(0, eventcodeIndex);
				if(descriptiveQuestion)
					exportTiffService.exportDescriptiveTiffsForCandidate(rpscandidate, candidateResponse,
							masterAssociation, batchAcsAssociation, assessmentCode, questionPaper,
							questionAssociationList, globalQuestionsMap);
			}
			this.updateRpsSectionCandidateResponse(sectionScoreMap,assessment.getAssessmentCode());
			candidateResponse.setCandidateScore(totalScore);
			candidateResponse.setTotalCorrect(totalCorrect);
			if (isScoreRegenerate) {
				// adjust the score of parent question
				adjustParentScore(parentResponseMap, reGenParentResponseMap);
			} else if (!parentResponseMap.isEmpty())
				candidateResponseEntityList.addAll(parentResponseMap.values());
			String updatedCandidateResponse = new Gson().toJson(candidateResponseEntityList);
			candidateResponse.setResponse(updatedCandidateResponse);

			if (!isCronFailedOrInvalid) {
				candidateResponse.setQuestionsAttempted(questionsAttempted);
				candidateResponse.setCronStatus(CRONCOMPLETED);
				this.updateCandidateResponses(candidateRes);
			}

			// evaluate BT
			if (rpsBehaviouralTest != null) {
				logger.info("BehavioralTestType -- " + rpsBehaviouralTest.getBtId());
				if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSG.toString())
						|| rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSB.toString())
						|| rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.IDSA.toString())) {
					candidateResponse.setBehaviouralTestScores(new Gson()
							.toJson(behavioralScoringLogicService.scoringForIDS(questToCandRespForMCQWMap,
									candidateResponse.getCandidateResponseId(), rpsBehaviouralTest, questionPaper,
									defaultAnswerOption, optionShuffleSeq)));
				} else if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.CGT.toString())) {
					// sum like S1AR, S1BR, S2AR, S2BR, S3R,... -> 3,4,1,2,0,... ->
					Map<String, Integer> mapSummationValuesToUniqCodes = new HashMap<String, Integer>();
					// private List<BehaviouralParamtersEntity> behaviouralParamtersEntities = new ArrayList<>();
					// sum like R I A S E C ->
					Map<String, Integer> mapSummationValuesToParam = new HashMap<String, Integer>();
					// std dev like R I A S E C ->
					Map<String, Double> mapStdDevValuesToParam = new HashMap<String, Double>();
					List<String> listOfCandidateResponses = new ArrayList<>(75);
					candidateResponse.setBehaviouralTestScores(cgtScoringService
							.processCandidateResponsesOnUniqueCodes(candidateResponse.getRpsMasterAssociation(),
									questToCandRespForMCQWMap, questionPaper, mapSummationValuesToUniqCodes,
									mapSummationValuesToParam, mapStdDevValuesToParam, listOfCandidateResponses));
					// candidateResponse
					// .setBehaviouralTestScores(new Gson().toJson(""));
				} else if (rpsBehaviouralTest.getBtId().equalsIgnoreCase(BehavioralTestType.Dimension8.toString())) {
					// all other behavioral test types
					candidateResponse.setBehaviouralTestScores(new Gson()
							.toJson(behavioralScoringLogicService.evaluateBehaviouralTestForD8(
									questToCandRespForMCQWMap,
									rpsBehaviouralTest, questionPaper, defaultAnswerOption, optionShuffleSeq)));
				} else {
					// all other behavioral test types
					candidateResponse.setBehaviouralTestScores(new Gson()
							.toJson(behavioralScoringLogicService.evaluateBehaviouralTest(questToCandRespForMCQWMap,
									rpsBehaviouralTest, questionPaper, defaultAnswerOption, optionShuffleSeq)));
				}
			}

		} catch (Exception e) {
			String errMsg =
					"Could not process results for candidate with candidateResponseRow::"
							+ candidateResponse.getCandidateResponseId();
			logger.error(errMsg, e);
			candidateResponse.setCronStatus(CRONFAILED);
		}

		/*
		 * RpsCandidateResponse rpsCandidateResponse =
		 * rpsCandidateResponseRepository.findOne(candidateResponse.getCandidateResponseId());
		 * rpsCandidateResponse.setFinalResult(candidateResponse.getFinalResult());
		 * rpsCandidateResponse.setCandidateScore(candidateResponse.getCandidateScore());
		 * rpsCandidateResponse.setTotalCorrect(candidateResponse.getTotalCorrect());
		 * rpsCandidateResponse.setCronStatus(candidateResponse.getCronStatus()); logger.info("Processed result for {}",
		 * rpsCandidateResponse.getCandidateResponseId()); rpsCandidateResponse =
		 * rpsCandidateResponseRepository.save(rpsCandidateResponse);
		 */
		candidateResponseLiteRepository.save(candidateResponse);

		return candidateResponse;
	}

	private double getNegativeMarkingForResponse(RpsQuestion question, CandidateResponseEntity candidateResponseEntity,
			ResponseMarkBean responseMarkBean) {
		MarksAndScoreRules marksAndScoreRules = getMarksAndScoreRules(question, candidateResponseEntity);
		return ScoringLogicForAllQuestionTypes.getNegativeMarkingForResponse(marksAndScoreRules, responseMarkBean,
				question.getQuestionType(), question.getQid(), question.getScore(), question.getNegativeScore());
	}

	private MarksAndScoreRules getMarksAndScoreRules(RpsQuestion question,
			CandidateResponseEntity candidateResponseEntity) {
		MarksAndScoreRules marksAndScoreRules = null;
		if (candidateResponseEntity != null && sectionLevelRulesMap != null) {
			SectionLevelRules sectionLevelRules = sectionLevelRulesMap.get(candidateResponseEntity.getSectionID());
			if (sectionLevelRules != null) {
				marksAndScoreRules = sectionLevelRules.getMarksAndScoreRules();
			}
		}
		return marksAndScoreRules;
	}

	private void updateCandidateResponses(RpsCandidateResponses candidateResponses) {
		if (candidateResponses.getRpsMasterAssociation() != null)
			candidateResponsesRepository.save(candidateResponses);
	}

	private void updateRpsSectionCandidateResponse(Map<String, SectionalScoreData> sectionScoreMap, String assessmentCode) {
		logger.info("------updateRpsSectionCandidateResponse :: IN-----");
		// check section count
		List<String> qpSections = rpsQpSectionRepository.findAllQpSectionNamesByAssmentCode(assessmentCode);
		Set<String> key = sectionScoreMap.keySet();
		if(qpSections.size()!=key.size()){

			Collection<String> similar = new HashSet<String>( qpSections );
			Collection<String> different = new HashSet<String>();
			different.addAll( qpSections );
			different.addAll( key );

			similar.retainAll( key );
			different.removeAll( similar );
			for (String sectionID : different) {
				//insert empty row
				insertUnAttemptedSection(sectionID);
			}



		}
		for (String sectionID : key) {
			RpsSectionCandidateResponse rpsSectionCandidateResponse =
					rpsSectionCandidateResponseRepository.findRpsSecCandRespByRpsCandRespAndSection(candidateResponse,
							sectionID);
			if (rpsSectionCandidateResponse == null)
				rpsSectionCandidateResponse = new RpsSectionCandidateResponse();

			rpsSectionCandidateResponse.setRpsCandidateResponse(candidateResponse);
			rpsSectionCandidateResponse.setSecIdentifier(sectionID);
			SectionalScoreData sectionalScoreData = sectionScoreMap.get(sectionID);
			rpsSectionCandidateResponse.setScore(sectionalScoreData.getCandSecScore());
			rpsSectionCandidateResponse.setQuestionsAttempted(sectionalScoreData.getSecQuestionsAttempted());
			rpsSectionCandidateResponse.setQuestionsCorrect(sectionalScoreData.getSecQuestionsCorrect());
			rpsSectionCandidateResponseRepository.save(rpsSectionCandidateResponse);

		}
		logger.info("RpsSectionCandidateResponse has been successfully updated:: ");
		logger.info("------updateRpsSectionCandidateResponse :: OUT-----");
	}

	private void insertUnAttemptedSection(String sectionID) {
		RpsSectionCandidateResponse rpsSectionCandidateResponse =
				rpsSectionCandidateResponseRepository.findRpsSecCandRespByRpsCandRespAndSection(candidateResponse,
						sectionID);
		if (rpsSectionCandidateResponse == null)
			rpsSectionCandidateResponse = new RpsSectionCandidateResponse();

		rpsSectionCandidateResponse.setRpsCandidateResponse(candidateResponse);
		rpsSectionCandidateResponse.setSecIdentifier(sectionID);
		rpsSectionCandidateResponse.setScore(0.0);
		rpsSectionCandidateResponse.setQuestionsAttempted(0);
		rpsSectionCandidateResponse.setQuestionsCorrect(0);
		rpsSectionCandidateResponseRepository.save(rpsSectionCandidateResponse);
	}

	private void createParentResponses(Map<String, CandidateResponseEntity> parentResponseMap, String parentQid,
			double individualQuestionScore) {
		CandidateResponseEntity parentResp = parentResponseMap.get(parentQid);
		if (parentResp == null) {
			// create a new parent response object
			parentResp = new CandidateResponseEntity();
			parentResp.setQuestionID(parentQid);
			parentResp.setQuestionType(QuestionType.READING_COMPREHENSION.toString());
			parentResp.setScore(0.0);
			parentResponseMap.put(parentQid, parentResp);
		}
		parentResp.setScore(parentResp.getScore() + individualQuestionScore);

	}

	private void createParentResponsesForSurvey(Map<String, CandidateResponseEntity> parentResponseMap,
			String parentQid, double individualQuestionScore) {
		CandidateResponseEntity parentResp = parentResponseMap.get(parentQid);
		if (parentResp == null) {
			// create a new parent response object
			parentResp = new CandidateResponseEntity();
			parentResp.setQuestionID(parentQid);
			parentResp.setQuestionType(QuestionType.SURVEY.toString());
			parentResp.setScore(0.0);
			parentResponseMap.put(parentQid, parentResp);
		}
		parentResp.setScore(parentResp.getScore() + individualQuestionScore);

	}

	private void adjustParentScore(Map<String, CandidateResponseEntity> parentResponseMap,
			Map<String, CandidateResponseEntity> reGenParentResponseMap) {
		if (!parentResponseMap.isEmpty() && !reGenParentResponseMap.isEmpty()) {
			Iterator<String> reGenQIt = reGenParentResponseMap.keySet().iterator();
			while (reGenQIt.hasNext()) {
				String questionId = reGenQIt.next();
				CandidateResponseEntity candResp = parentResponseMap.get(questionId);
				CandidateResponseEntity reCandResp = reGenParentResponseMap.get(questionId);
				reCandResp.setScore(candResp.getScore());
			}
		}

	}

	/**
	 * @param optionCount
	 * @param defaultAnswerOption
	 * @return
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

	/**
	 * Calculation Method GROSS SPEED *- Gross, or Raw WPM (Words Per Minute) is a calculation of exactly how fast you
	 * type with no error penalties. NET WPM SPEED *- A Net WPM calculation is preferred for measuring typing speed as
	 * opposed to the Gross WPM computation since *- including mistakes will give a more complete picture of your true
	 * typing abilities. ACCURACY *- Typing accuracy is defined as the percentage of correct entries out of the total
	 * entries typed.
	 *
	 * @param questionText
	 * @param responseText
	 * @return
	 */
	public String typingTestEvaluation(String questionText, String responseText, int sectionDuration) {
		logger.debug("--IN-- typingTestEvaluation() with params questionText: {}, responseText: {}, "
				+ "sectionDuration(in mins): {}", questionText, responseText, sectionDuration);
		String typingTestEvaluationJson = null;
		String altOriginalText = null;
		if (questionText == null || responseText == null)
			return null;
		TypingTestEvaluationResultBean typingTestEvaluationResultBean = null;
		TextDiffMatchPatch dmp = new TextDiffMatchPatch();
		if (questionText.contains("kruti")) {
			try {
				String convertedHindiString =
						TextToUnicode.convertToAsciiFromKrutiDev(ApolloUtility.getPlainText(questionText));
				String convertedHindiResponse =
						TextToUnicode.convertToAsciiFromKrutiDev(ApolloUtility.getPlainText(responseText));
				LinkedList<TextDiffMatchPatch.Diff> altDifference = dmp.diffMain(convertedHindiString, convertedHindiResponse, false);
				for (int i = 0; i < altDifference.size(); i++) {
					TextDiffMatchPatch.Diff current = altDifference.get(i);
					if (current.operation.equals(TextDiffMatchPatch.DiffOperation.INSERT) || current.operation.equals(TextDiffMatchPatch.DiffOperation.EQUAL)) {
						if (altOriginalText != null) {
							if (current.text.length() == 1) {
								List<String> singleSymbolWrapper = Arrays.asList(TextToUnicode.singleSymbol);
								if (singleSymbolWrapper.contains(current.text))
									altOriginalText = altOriginalText.concat("$");
								else
									altOriginalText = altOriginalText.concat(current.text);
							} else
								altOriginalText = altOriginalText.concat(current.text);
						} else altOriginalText = current.text;
					}
				}
				typingTestEvaluationResultBean =
						CompareTextUtility.compareText(convertedHindiString, altOriginalText);
			} catch (Exception e) {
				logger.error("Error in parsing KrutiDev Font  " + e.getStackTrace());
			}
		} else
			typingTestEvaluationResultBean =
					CompareTextUtility.compareText(ApolloUtility.getPlainText(questionText),
							ApolloUtility.getPlainText(responseText));

		int noOfTypedWords = typingTestEvaluationResultBean.getNoOfTypedWords();
		// int errorWord = noOfTypedWords - typingTestEvaluationResultBean.getTotalCorrectWords();
		double grossWPMSpeed = 0;
		double netWPMSpeed = 0;
		double accuracy = 0;
		int WPM = 0;
		/**
		 * Gross = Number of words typed in one minute Net score = Number of words typed correctly in one minute
		 * Accuracy (reported as a percentage) = (Net score/Gross) *100
		 */
		if (sectionDuration != 0) {
			WPM = Math.round((float) typingTestEvaluationResultBean.getNoOfTypedWords() / sectionDuration);
			grossWPMSpeed = (double) typingTestEvaluationResultBean.getNoOfTypedWords() / sectionDuration;
			netWPMSpeed = (double) typingTestEvaluationResultBean.getTotalCorrectWords() / sectionDuration;
		}
		accuracy = (netWPMSpeed / grossWPMSpeed) * 100;
		TypingTestEntity typingTestEntity = new TypingTestEntity();
		typingTestEntity.setCorrectWord(typingTestEvaluationResultBean.getTotalCorrectWords());
		if (Double.isNaN(accuracy))
			typingTestEntity.setAccuracy(0.0);
		else
			typingTestEntity.setAccuracy(Double.parseDouble(new DecimalFormat("##.##").format(accuracy)));
		typingTestEntity.setErrorWord(noOfTypedWords - typingTestEvaluationResultBean.getTotalCorrectWords());
		typingTestEntity.setGrossSpeed(grossWPMSpeed);
		typingTestEntity.setNetSpeed(netWPMSpeed);
		typingTestEntity.setNoOfTotalWords(typingTestEvaluationResultBean.getTotalSourceWords());
		typingTestEntity.setNoOfTypedWords(noOfTypedWords);
		typingTestEntity.setSectionDuration(sectionDuration);
		typingTestEntity.setWPM(WPM);
		typingTestEvaluationJson = new Gson().toJson(typingTestEntity);

		logger.debug("--OUT--typingTestEvaluation() with params typingTestEvaluationJson:{}", typingTestEvaluationJson);
		return typingTestEvaluationJson;
	}

}
