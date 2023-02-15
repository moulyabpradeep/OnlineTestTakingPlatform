package com.merittrac.apollo.acs.services;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.dataobject.AcsQuestionDO;
import com.merittrac.apollo.acs.dataobject.CandidateAllRCQidDO;
import com.merittrac.apollo.acs.dataobject.QPSectionsDO;
import com.merittrac.apollo.acs.dataobject.tp.QPInfo;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsQpQuestionAssociation;
import com.merittrac.apollo.acs.entities.AcsQpSections;
import com.merittrac.apollo.acs.entities.AcsQuestion;
import com.merittrac.apollo.acs.entities.AcsQuestionPaper;
import com.merittrac.apollo.acs.entities.PracticeTestQuestionPaperTo;
import com.merittrac.apollo.acs.exception.ACSException;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.QpCandAllocation;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ShuffleSequenceGeneratorUtility;
import com.merittrac.apollo.common.entities.acs.Language;
import com.merittrac.apollo.common.entities.acs.QuestionPaperStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.SectionLevelRules;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.ShufflingRules;

public class QuestionService extends BasicService implements IQuestionService {

	private final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	private static String DUMMY_INST_SHEET =
			"The computer skill test examines the computer proficiency and knowledge of computer including usage of office suites and database for which questions pertaining to a passage/paragraph of about 100 words in MS-word, One Power Point Presentation slide in MS-Power Point and Table in MS-Excel will have to be answered within the time limits";

	private static Gson gson = null;
	private static BatchService bs = null;
	private static QuestionService questionService = null;
	private static ShuffleSequenceGeneratorUtility shuffleSequenceGeneratorUtility = null;
	private static CDEAService cdeaService = null;
	private static ACSPropertyUtil acsPropertyUtil = null;
	private static CustomerBatchService customerBatchService = null;
	static {
		bs = BatchService.getInstance();
		if (acsPropertyUtil == null) {
			acsPropertyUtil = new ACSPropertyUtil();
		}
		if (cdeaService == null) {
			cdeaService = new CDEAService();
		}
		if (shuffleSequenceGeneratorUtility == null) {
			shuffleSequenceGeneratorUtility = new ShuffleSequenceGeneratorUtility();
		}

		if (gson == null) {
			gson = new Gson();
		}
		if (customerBatchService == null)
			customerBatchService = new CustomerBatchService();
	}

	public QuestionService() {

	}

	/**
	 * Using double check singleton pattern
	 * 
	 * @return instance of batch service
	 * 
	 * @since Apollo v2.0
	 */
	public static final QuestionService getInstance() {
		if (questionService == null) {
			synchronized (QuestionService.class) {
				if (questionService == null) {
					questionService = new QuestionService();
				}
			}
		}
		return questionService;
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	@Override
	public void getAllQuestionsForQPID(int qpid) {

	}

	@Override
	public void getAllQuestionsInSection(int qpid, int sectionID) {

	}

	/*
	 * Retrieves the QP DB Identifier for a given alpha-numeric QP identifier
	 */
	@Override
	public int getQPIdByIdentifier(String qpIdent) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QPID_BYIDENT;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpID", qpIdent);

		Integer result = (Integer) session.getByQuery(query, params);
		if (result == null)
			return 0;
		else
			return result;
	}

	public AcsQuestionPaper getQPandPackIdByIdentifier(String qpaperCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QPAPER_AND_PACKDETAILSES;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpaperCode);

		AcsQuestionPaper questionPaper = (AcsQuestionPaper) session.getByQuery(query, params);
		return questionPaper;

	}

	@Override
	public String getInstructionSheetByQPId(String assessmentIdent) {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		statsCollector.log(startTime, "getInstructionSheetByQPId", "QuestionService", 0);
		return DUMMY_INST_SHEET;
	}

	@Override
	public int getQuestionCountForQP(String qpaperCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QUESTIONS_COUNT_FOR_PAPER;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpaperCode);
		// Integer result = (Integer) session.getByQuery(query, params);
		int result = session.getResultCountByQuery(query, params);
		// if (result == null)
		// return 0;
		// else
		return result;

	}

	public int getQuestionCountForQPWithoutRC(String qpaperCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QUESTIONS_COUNT_FOR_PAPER_WITHOUT_RC;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpaperCode);
		int result = ((BigInteger) session.getUniqueResultByNativeSQLQuery(query, params)).intValue();
		return result;

	}

	public List<Integer> getUnDisabledQPIdsbyBatchIdAndAssessmentId(int batchID, int assessmentID)
			throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_UN_DISABLED_QP_IDS_BY_BATCHID_ASSESSMENTID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSQueryConstants.PARAM_ASSESSMENT_ID, assessmentID);
		params.put(ACSQueryConstants.PARAM_BATCH_ID, batchID);
		params.put("qpStatus", QuestionPaperStatusEnum.DISABLED);

		List<Integer> qpIds = session.getResultAsListByQuery(query, params, 0);
		if (qpIds.equals(Collections.<Object> emptyList()))
			return null;
		return qpIds;
	}

	/*
	 * updates the question papers status for a given batch Id..
	 */
	public boolean updateQPsStatusByBatchId(int batchId) throws GenericDataModelException {
		// updates the qp status to disabled if the qp's are not allocated to any candidate
		updateNonAllocatedQPIdsStatusbyBatchId(batchId);

		// updates the qp status to allocated but disabled if the qp's are allocated to any candidate
		updateAllocatedQpIdsStatusByBatchId(batchId);

		return true;
	}

	public List<String> getSectionNamesByQPIdentifier(String qpIdent) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpIdent", qpIdent);
		String query = "SELECT s.secIdent FROM QuestionPaperTO q join q.qpSec s WHERE q.qpaperID = (:qpIdent)";

		List<String> sectionNames = session.getListByQuery(query, params);
		return sectionNames;
	}

	public int getAssessmentIdByQpIdAndBatchId(int qpId, int batchId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpId", qpId);
		params.put("batchId", batchId);

		Integer assessmnetId =
				(Integer) session
						.getByQuery(
								"SELECT assessmentID FROM QpAssessmentAssociationTO WHERE qpID=(:qpId) and batchID = (:batchId)",
								params);
		if (assessmnetId == null) {
			return 0;
		}
		return assessmnetId;
	}

	/**
	 * Get question vs option count mapping for all questions of question paper.
	 * 
	 * @param questionPaperIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, Integer> getOptionCountPerQuestionInQP(String questionPaperIdentifier)
			throws GenericDataModelException {
		Map<String, Integer> optionCountPerQuestionMap = new HashMap<String, Integer>();

		List<AcsQuestionDO> questions = getQuestionsForQpaper(questionPaperIdentifier);
		if (questions != null && !questions.isEmpty()) {
			for (Iterator<AcsQuestionDO> itQuestions = questions.iterator(); itQuestions.hasNext();) {
				AcsQuestionDO acsQuestion = itQuestions.next();
				if (acsQuestion != null
						&& !acsQuestion.getQuestionType().equalsIgnoreCase(
								QuestionType.READING_COMPREHENSION.toString())) {

					optionCountPerQuestionMap.put(acsQuestion.getQuestID(), acsQuestion.getOptionCount());
				} else {
					logger.info("Found question as null or READING_COMPREHENSION");
				}
			}
		} else {
			logger.info("No question paper and question exists with specified questionPaperIdentifier = {}",
					questionPaperIdentifier);
		}
		return optionCountPerQuestionMap;
	}

	/**
	 * Get question vs option count mapping for all questions of question paper, return Map of <QuestId and QuestionDO>
	 * 
	 * @param questionPaperIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, AcsQpQuestionAssociation> getOptionCountPerQuestion(String questionPaperIdentifier)
			throws GenericDataModelException {
		Map<String, AcsQpQuestionAssociation> optionCountPerQuestionMap =
				new HashMap<String, AcsQpQuestionAssociation>();

		// List<AcsQuestionDO> questions = getQuestionsForQpaper(questionPaperIdentifier);
		List<String> questionType = new ArrayList<>();
		questionType.add(QuestionType.MULTIPLE_CHOICE_QUESTION.toString());
		questionType.add(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString());
		questionType.add(QuestionType.MULTIPLE_OPTIONS_QUESTION.toString());
		List<AcsQpQuestionAssociation> qpQuestionAssos =
				getQPQuestionAssoByQpaperCodeAndQType(questionPaperIdentifier, questionType);
		if (qpQuestionAssos != null && !qpQuestionAssos.isEmpty()) {
			for (AcsQpQuestionAssociation acsQpQuestionAssociation : qpQuestionAssos) {
				AcsQuestion acsQuestion = acsQpQuestionAssociation.getAcsquestion();
				if (acsQuestion != null
						&& (acsQuestion.getQuestionType().equalsIgnoreCase(
								QuestionType.MULTIPLE_CHOICE_QUESTION.toString()) || 
								acsQuestion.getQuestionType().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString()) ||
								acsQuestion.getQuestionType().equalsIgnoreCase(QuestionType.MULTIPLE_OPTIONS_QUESTION.toString()))) {

					optionCountPerQuestionMap.put(acsQuestion.getQuestId(), acsQpQuestionAssociation);
				} else {
					logger.info("Found question as null or READING_COMPREHENSION");
				}
			}
		} else {
			logger.info("No question paper and question exists with specified questionPaperIdentifier = {}",
					questionPaperIdentifier);
		}
		return optionCountPerQuestionMap;
	}

	/**
	 * Get list for given paper code using which has <br>
	 * composite primary key consisting of qpaperCode.
	 * 
	 * @param paperCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsQuestionDO> getQuestionsForQpaper(String qpaperCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("qpaperCode", qpaperCode);
		List<AcsQuestionDO> acsQuestions =
				(List<AcsQuestionDO>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_QUESTIONS_FOR_PAPER, params, AcsQuestionDO.class);
		return acsQuestions;
	}

	public AcsQpSections getSectionsOfPaper(String paperCode, String secIdent) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("qpaperCode", paperCode);
		params.put("secIdent", secIdent);
		AcsQpSections acsQpSections =
				(AcsQpSections) session.getByQuery(ACSQueryConstants.QUERY_FETCH_SECTIONS_BY_PAPER_IDENT, params);

		return acsQpSections;
	}

	/**
	 * apply Option Level Shuffling Sequence Based on Rules
	 * 
	 * @param candIdentifier
	 * @param bca
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, List<String>> getOptionLevelShuffleSequence(String candIdentifier,
			AcsBatchCandidateAssociation bca) throws GenericDataModelException {
		Boolean isShuffleEnabledOnAssessmentOrSectionLevel = false;
		Map<String, List<String>> sequenceMap = new HashMap<String, List<String>>();
		String questionPaperIdentifier = bca.getQpaperCode();
		// gets the set of questions and their option count associated to the specified qp.
		Map<String, AcsQpQuestionAssociation> questionDOMap = getOptionCountPerQuestion(questionPaperIdentifier);
		// gets Option Shuffle Is Enabled for sections
		Map<Integer, Boolean> shufflingRulesRulesMap = getOptionShuffleSeqIsEnabled(bca);

		logger.info("optionCountPerQuestionMap For Shuffling On QLevel = {} for qpId = {} ", questionDOMap,
				questionPaperIdentifier);

		if (shufflingRulesRulesMap.size() == 1 && shufflingRulesRulesMap.containsKey(ACSConstants.ZERO))
			// Assessment level Option Shuffling
			isShuffleEnabledOnAssessmentOrSectionLevel = shufflingRulesRulesMap.get(ACSConstants.ZERO);

		if (questionDOMap != null && !questionDOMap.isEmpty()) {
			for (Map.Entry<String, AcsQpQuestionAssociation> optionCountPerQuestionAssoMap : questionDOMap.entrySet()) {
				AcsQpQuestionAssociation acsQpQuestionAssociation = optionCountPerQuestionAssoMap.getValue();
				if (!shufflingRulesRulesMap.containsKey(ACSConstants.ZERO))
					isShuffleEnabledOnAssessmentOrSectionLevel =
							shufflingRulesRulesMap.get(acsQpQuestionAssociation.getAcsqpsections().getSid());
				AcsQuestion questionDO = acsQpQuestionAssociation.getAcsquestion();
				Boolean isShuffleOptions = questionDO.getShuffleOptions();
				logger.info("is Option Shuffling enabled On QLevel = {} for question = {} ", isShuffleOptions,
						questionDO.getQuestId());
				if(questionDO.getAcsquestion()!=null){
					isShuffleOptions=questionDO.getAcsquestion().getShuffleOptions();
					logger.info("is Option Shuffling enabled On Parent QLevel = {} for parent question = {} ",isShuffleOptions, questionDO.getAcsquestion());
				}
				if (isShuffleOptions != null && isShuffleOptions) {
					if (isShuffleEnabledOnAssessmentOrSectionLevel != null
							&& isShuffleEnabledOnAssessmentOrSectionLevel) {
						// generates the shuffle sequence for options per question based on
						// seed = candIdentifier + questionId
						int optionCount = questionDO.getOptionCount();
						sequenceMap.putAll(shuffleSequenceGeneratorUtility.generateOptionShuffleSequence(
								optionCountPerQuestionAssoMap.getKey(), optionCount, candIdentifier));
					} else {
						// generates the default shuffle sequence for options per question for all questions.
						int optionCount = questionDO.getOptionCount();
						sequenceMap.putAll(shuffleSequenceGeneratorUtility.generateDefaultOptionShuffleSequence(
								optionCountPerQuestionAssoMap.getKey(), optionCount));
					}
				} else {
					int optionCount = questionDO.getOptionCount();
					sequenceMap.putAll(shuffleSequenceGeneratorUtility.generateDefaultOptionShuffleSequence(
							optionCountPerQuestionAssoMap.getKey(), optionCount));
				}
			}
		}
		return sequenceMap;
	}

	public Map<String, List<String>> getQuestionLevelShuffleSequence(AcsQuestionPaper questionPaper,
			String candIdentifier) throws GenericDataModelException {
		Map<String, List<String>> sequenceMap = new LinkedHashMap<String, List<String>>();

		if (questionPaper != null) {
			Type type = new TypeToken<LinkedHashMap<String, List<String>>>() {
			}.getType();

			Map<String, List<String>> questionIdsPerSectionMap =
					gson.fromJson(questionPaper.getQuestionSequence(), type);
			logger.info("questionIdsPerSectionMap = {} for qpId = {}", questionIdsPerSectionMap,
					questionPaper.getQpaperCode());

			if (questionIdsPerSectionMap != null && !questionIdsPerSectionMap.isEmpty()) {
				// generates the shuffle sequence for questions per section based on seed = candIdentifier.
				sequenceMap =
						shuffleSequenceGeneratorUtility.generateQuestionShuffleSequence(questionIdsPerSectionMap,
								candIdentifier);
			}
		}
		return sequenceMap;
	}

	public Map<String, List<String>> getDefaultQuestionOrderPerSection(String qpIdentifier)
			throws GenericDataModelException {
		// gets the set of questions and their option count associated to the specified qp.
		// fetch the question paper object based on the specified qpIdentifier
		AcsQuestionPaper acsQuestionPaper =
				(AcsQuestionPaper) session.get(qpIdentifier, AcsQuestionPaper.class.getName());
		Map<String, List<String>> questionIdsPerSectionMap = null;
		if (acsQuestionPaper != null) {
			Type type = new TypeToken<LinkedHashMap<String, List<String>>>() {
			}.getType();
			logger.error("Question sequence for acsQuestionPaper.getQuestionSequence() = {} for qpId = {}",
					acsQuestionPaper.getQuestionSequence(), qpIdentifier);
			questionIdsPerSectionMap = gson.fromJson(acsQuestionPaper.getQuestionSequence(), type);
			logger.info("questionIdsPerSectionMap = {} for qpId = {}", questionIdsPerSectionMap, qpIdentifier);
		}
		return questionIdsPerSectionMap;
	}

	public void updateNonAllocatedQPIdsStatusbyBatchId(int batchId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_UPDATE_NON_ALLOCATED_QP_IDS_STATUS_BY_BATCHID_ASSESSMENTIDS;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSQueryConstants.PARAM_BATCH_ID, batchId);
		params.put("qpStatus", QuestionPaperStatusEnum.DISABLED);

		session.updateByQuery(query, params);
	}

	public void updateAllocatedQpIdsStatusByBatchId(int batchId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_UPDATE_ALLOCATED_QP_IDS_STATUS_PER_BATCHID;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSQueryConstants.PARAM_BATCH_ID, batchId);
		params.put("qpStatus", QuestionPaperStatusEnum.ALLOCATED_BUT_DISABLED);

		session.updateByQuery(query, params);
	}

	public List<String> getEnabledQPIdsbyBatchIdAndAssessmentId(String batchCode, String assessmentCode)
			throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ENABLED_QP_IDS_BY_BATCHCODE_ASSESSMENTCODE;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);
		params.put("packStatus", QuestionPaperStatusEnum.ACTIVATED);
		params.put("packType", PackContent.Qpack);
		List<String> qpIds = (List<String>) session.getResultAsListByQuery(query, params, 0);
		if (qpIds.equals(Collections.<Object> emptyList()))
			return null;
		return qpIds;
	}

	/**
	 * gets the list of child question ids based on the input parent question id.
	 * 
	 * @param parentQuestionId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	@Deprecated
	public List<AcsQuestion> getChildQuestionsByParentQuestionId(String parentQuestionId)
			throws GenericDataModelException {
		getLogger().info("initiated getChildQuestionsByParentQuestionId where parentQuestionId={}", parentQuestionId);

		String query = "FROM AcsQuestion WHERE parentQuestionId=(:parentQuestionId) order by qid asc";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("parentQuestionId", parentQuestionId);

		// get the child questions if any.
		List<AcsQuestion> questionTOs = session.getResultAsListByQuery(query, params, 0);
		getLogger().info("list of child questions = {} for the specified parent question id = {}", questionTOs,
				parentQuestionId);

		if (questionTOs.isEmpty()) {
			return null;
		}
		return questionTOs;
	}

	/**
	 * gets the list of child question ids based on the input parent question id.
	 * 
	 * @param parentQuestionId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<AcsQuestion> getChildQuestionsByParentQuestionIdAndAssessment(String parentQuestionId,
			String assessmentCode) throws GenericDataModelException {
		getLogger().info("initiated getChildQuestionsByParentQuestionId where parentQuestionId={}, assessmentCode={}",
				parentQuestionId, assessmentCode);

		String query =
				"FROM AcsQuestion q WHERE q.acsquestion.questId=(:parentQuestionId) and q.acsassessmentdetails.assessmentCode=(:assessmentCode) order by qid asc";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("parentQuestionId", parentQuestionId);
		params.put("assessmentCode", assessmentCode);

		// get the child questions if any.
		List<AcsQuestion> questionTOs = session.getResultAsListByQuery(query, params, 0);
		getLogger().info("list of child questions = {} for the specified parent question id = {}", questionTOs,
				parentQuestionId);

		if (questionTOs.isEmpty()) {
			return null;
		}
		return questionTOs;
	}

	/**
	 * inserts the question paper relative path in database
	 * 
	 * @param qpaperID
	 * @param qpFilename
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updateQpFileName(String qpaperID, String qpFilename) throws GenericDataModelException {
		getLogger().info("initiated updateQpFileName where qpaperID={} and qpFilename={}", qpaperID, qpFilename);

		String query = "UPDATE AcsQuestionPaper set qpFilename=(:qpFilename) WHERE qpaperCode=(:qpaperCode)";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpFilename", qpFilename);
		params.put("qpaperCode", qpaperID);

		// get the child questions if any.
		int count = session.updateByQuery(query, params);
		getLogger().info("updated row count = {}", count);
	}

	public Map<String, List<String>> getShuffleSequence(int CandidateId, String qpId) throws GenericDataModelException {
		AcsQuestionPaper acsQuestionPaper = (AcsQuestionPaper) session.get(qpId, AcsQuestionPaper.class.getName());

		return this.getQuestionLevelShuffleSequence(acsQuestionPaper, CandidateService.getInstance()
				.getApplicationNumberByCandidateId(CandidateId));
	}

	public String getQPIdByBatchIdAndCandId(int batchId, int qpId, int candId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QPID_BY_BATCHID_CANDID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchID", batchId);
		params.put("candid", candId);

		String qpaperID = (String) session.getByQuery(query, params);
		return qpaperID;

	}

	/**
	 * @param questID
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsQuestion getQuestionByIdAndEventCode(String questID, String eventCode) throws GenericDataModelException {
		getLogger().info("initiated getQuestionByIdAndEventCode where identifier={} and eventCode={}", questID,
				eventCode);

		String query = "FROM QuestionTO WHERE questID=(:questID) and eventCode=(:eventCode)  order by qid asc";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("questID", questID);
		params.put("eventCode", eventCode);

		// get the child questions if any.
		AcsQuestion questionTo = (AcsQuestion) session.getByQuery(query, params);
		getLogger().info("Question = {} for the specified question id = {}", questionTo, questID);
		return questionTo;

	}

	public AcsQuestion getQuestionByIdAndAssessment(String questionId, String assessmentCode)
			throws GenericDataModelException {
		getLogger().info("initiated getQuestionByIdAndEventCode where identifier={} and assessmentCode={}", questionId,
				assessmentCode);

		String query =
				"FROM AcsQuestion q WHERE q.questId=(:qid) and q.acsassessmentdetails.assessmentCode=(:assessmentCode)  order by q.qid asc";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qid", questionId);
		params.put("assessmentCode", assessmentCode);

		// get the child questions if any.
		AcsQuestion acsQuestion = (AcsQuestion) session.getByQuery(query, params);
		getLogger().info("Question = {} for the specified question id = {}", acsQuestion, questionId);
		return acsQuestion;

	}

	/**
	 * Get parent question's questId(not qId) for question.
	 * 
	 * @param qId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getParentQuestionIdForQuestion(int qId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("qid", qId);
		String questId = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_PARENT_QUESTID_FOR_QUESTION, params);
		return questId;
	}

	/**
	 * Get {@link AcsQuestion} for parent question ids.
	 * 
	 * @param parentQids
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsQuestion> getQuestionsForParentQids(List<Integer> parentQids) throws GenericDataModelException {
		if (parentQids.isEmpty())
			return null;
		HashMap<String, Object> params = new HashMap<>();
		params.put("parentQids", parentQids);
		List<AcsQuestion> acsQuestions =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QUESTIONS_FOR_PARENTQIDS, params, 0);
		return acsQuestions;
	}

	/**
	 * For every available assessments, one practice QP entry is made copying the paths from existing practice QP This
	 * method expects atleast one practice QP exists in database.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * @throws ACSException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@SuppressWarnings("null")
	public List<String> makePracticeQp() throws GenericDataModelException, ACSException {

		String query = "FROM AssessmentDetailsTO";

		HashMap<String, Object> params = new HashMap<String, Object>();
		List<String> practiceQpsMade = new ArrayList<>();

		PracticeTestQuestionPaperTo modelPracticeQpForAssessment = null;
		// get the child questions if any.
		List<AcsAssessment> assessmentDetailsTOs = session.getListByQuery(query, params);
		for (AcsAssessment assessmentDetailsTO : assessmentDetailsTOs) {
			String assessmentCode = assessmentDetailsTO.getAssessmentCode();
			PracticeTestQuestionPaperTo practiceQpForAssessment = getPracticeQpForAssessment(assessmentCode);
			if (practiceQpForAssessment != null) {
				modelPracticeQpForAssessment = practiceQpForAssessment;
			} else {
				// Practice Paper does not exists for this Assessment. Copy the model QP and persist it.
				if (modelPracticeQpForAssessment == null) {
					String msg = "No practice Test QP Exists!";
					getLogger().error(msg);
					throw new ACSException(msg);
				}
				PracticeTestQuestionPaperTo newPracticeTestQuestionPaperTo = new PracticeTestQuestionPaperTo();
				practiceQpForAssessment.setAssessmentCode(assessmentCode);
				practiceQpForAssessment.setPtQpaperId(modelPracticeQpForAssessment.getPtQpaperId());
				practiceQpForAssessment.setSecondaryPTQpaperId(modelPracticeQpForAssessment.getSecondaryPTQpaperId());
				session.persist(newPracticeTestQuestionPaperTo);
				practiceQpsMade.add(assessmentCode);
			}
		}
		logger.debug("Practice QP are made for assessments {}", practiceQpsMade);
		return practiceQpsMade;
	}

	/**
	 * 
	 * @param qpId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v1.0
	 * @see
	 */
	public List<CandidateAllRCQidDO> getRCChildQuestionDetailsForCandidate(String qpCode)
			throws GenericDataModelException {
		Map<String, Object> params = new HashMap<>();
		params.put("qpId", qpCode);
		params.put("questionType", QuestionType.READING_COMPREHENSION.toString());
		List<CandidateAllRCQidDO> candidateAllRCQidDO =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_RC_QUESTION_FOR_PAPER, params, 0,
						CandidateAllRCQidDO.class);
		if (candidateAllRCQidDO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return candidateAllRCQidDO;
	}

	/**
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public PracticeTestQuestionPaperTo getPracticeQpForAssessment(String assessmentCode)
			throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_PRACTICE_QPID_BY_ASSESSMENT_CODE;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		PracticeTestQuestionPaperTo qpaperID = (PracticeTestQuestionPaperTo) session.getByQuery(query, params);
		return qpaperID;

	}

	/**
	 * 
	 * @param questionId
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getQuestionTypeByQuestId(String questId) throws GenericDataModelException {
		String query =
				"select case when q.parentqid is null then q.questiontype else "
						+ " (select pq.questiontype from acsquestion pq where pq.qid=q.parentqid) end "
						// as questiontype
						+ " from acsquestion q where q.questid=(:questId)";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("questId", questId);
		String questiontype = (String) session.getUniqueResultByNativeSQLQuery(query, params);
		return questiontype == null ? "" : questiontype;
	}

	/**
	 * get Questions By List of QuestIds
	 * 
	 * @param questionIds
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQuestion> getQuestionsByQuestIds(List<Integer> qIds) throws GenericDataModelException {
		if (qIds.isEmpty())
			return null;
		String query = "FROM AcsQuestion WHERE qid in (:qIds) ";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qIds", qIds);
		List<AcsQuestion> acsQuestions = session.getResultAsListByQuery(query, params, 0);
		return acsQuestions;
	}

	/**
	 * 
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQpSections> getSidsFromQpaperCode(String qpCode) throws GenericDataModelException {

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("qpCode", qpCode);
		List<AcsQpSections> qpSections =
				(List<AcsQpSections>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_SECTION_DETAILS_FETCH_BY_QPCODE, params, 0);

		return qpSections;

	}

	/**
	 * 
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<QPSectionsDO> getSidsFromQpaperCodes(List<String> qpCodes) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("qpCode", qpCodes);
		params.put("questionType", "READING_COMPREHENSION");
		List<QPSectionsDO> qpSections = (List<QPSectionsDO>) session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_SECTION_DETAILS_FETCH_BY_QPCODE, params, QPSectionsDO.class);

		return qpSections;

	}

	/**
	 * 
	 * @param qpSectionsDOs
	 * @return
	 * @throws GenericDataModelException
	 */
	public double getSectionLevelMarksForAllSections(List<AcsQpSections> qpSectionsDOs)
			throws GenericDataModelException {
		double totalMarks = 0;
		for (AcsQpSections qpSectionsDO : qpSectionsDOs) {
			Integer sidInt = new Integer(qpSectionsDO.getSid());
			Map<String, Integer> params = new HashMap<String, Integer>();
			params.put("sid", sidInt);
			Double sectionwiseMarks =
					((Double) session.getUniqueResultByNativeSQLQuery(
							ACSQueryConstants.NATIVE_QUERY_QPSECQUESTIONASSOCIATION_FETCH_SUMOFQUESTION_BY_SECTIONID,
							params));
			totalMarks = totalMarks + sectionwiseMarks;
		}
		return totalMarks;
	}

	/**
	 * 
	 * @param qpSectionsDOs
	 * @return
	 * @throws GenericDataModelException
	 */
	public double getSectionLevelMarksForASection(Integer sidInt) throws GenericDataModelException {
		double sectionwiseMarks = 0;
//		if (qpSectionsDO == null)
//			return sectionwiseMarks;
//		Integer sidInt = new Integer(qpSectionsDO.getSid());
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("sid", sidInt);
		sectionwiseMarks =
				((Double) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.NATIVE_QUERY_QPSECQUESTIONASSOCIATION_FETCH_SUMOFQUESTION_BY_SECTIONID,
						params)).doubleValue();
		return sectionwiseMarks;
	}

	/**
	 * @param sid
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Double getSectionLevelScoreOfQuestions(int sid, int bcaId) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("sid", sid);
		params.put("bcaid", bcaId);
		Double sectionwiseMarks =
				((Double) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.NATIVE_QUERY_QPSECQUESTIONASSOCIATION_FETCH_SCOREOFQUESTION_BY_SECTIONID,
						params)).doubleValue();
		return sectionwiseMarks;
	}

	/**
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int getCorrectNumberOfQuestionsPerSection(int sid, int bcaId) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);
		params.put("sid", sid);
		params.put("questionType", QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString());
		int correctQuestionCount =
				((BigInteger) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.QUERY_CANDIDATERESPONSE_FOR_NO_CORRECT_QUESTION_COUNT_PER_SECTION, params))
						.intValue();
		return correctQuestionCount;
	}

	/**
	 * 
	 * @param qpSectionsDOs
	 * @return
	 * @throws GenericDataModelException
	 */
	public int getTotalNumberOfQuestions(List<AcsQpSections> qpSectionsDOs) throws GenericDataModelException {
		int count = 0;
		for (AcsQpSections qpSectionsDO : qpSectionsDOs) {
			Integer sidInt = new Integer(qpSectionsDO.getSid());
			Map<String, Integer> params = new HashMap<String, Integer>();
			params.put("sid", sidInt);
			int sidCount =
					((BigInteger) session.getUniqueResultByNativeSQLQuery(
							ACSQueryConstants.NATIVE_QUERY_QPSECQUESTIONASSOCIATION_FETCH_COUNTOFQUESTION_BY_SECTIONID,
							params)).intValue();
			count = count + sidCount;
		}
		return count;
	}

	/**
	 * gives the attempted question count for a candidate
	 * 
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 */
	public int getAttemptedQuestions(int bcaId) throws GenericDataModelException {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("bcaId", bcaId);
		int attemptedQuestions =
				((BigInteger) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.NATIVE_QUERY_CANDIDATERESPONSES_FETCH_COUNTOFQUESTION_BY_BATCHANDCANDIDATEID,
						params)).intValue();
		return attemptedQuestions;
	}

	/**
	 * gives the attempted question count for a candidate On section
	 * 
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 */
	public int getSectionLevelAttemptedQuestions(int bcaid, int sid, String qpapercode)
			throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		params.put("sid", sid);
		params.put("qpapercode", qpapercode);
		int attemptedQuestions =
				((BigInteger) session.getUniqueResultByNativeSQLQuery(
								ACSQueryConstants.NATIVE_QUERY_CANDIDATERESPONSES_FETCH_SECTION_LEVEL_COUNT_OF_QUESTION_BY_BATCHANDCANDIDATEID,
						params)).intValue();
		return attemptedQuestions;
	}

	/**
	 * provides the List of language variant QPinfo objects for the given qpId
	 * 
	 * @param qpId
	 * @param isPracticeTestTaken
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public QPInfo makeQPInfo(String qpaperCode, Boolean isPracticeTestTaken, String assessmentCode) {
		logger.debug("getQPInfo is initiated with {}", qpaperCode);
		QPInfo qpInfo = null;
		try {
			AcsQuestionPaper qp =
					(AcsQuestionPaper) session.get("qpaperCode", qpaperCode, AcsQuestionPaper.class.getCanonicalName());
			if (qp == null) {
				String message = "Question paper information not available";
				logger.error(message);
				throw new CandidateRejectedException(ACSExceptionConstants.QUESTION_PAPER_NOT_FOUND, message);
			}

			AcsQuestionPaper secondaryQP = getSecondaryQP(qp.getQpaperCode() + ".xml");
			if (secondaryQP != null)
				qpInfo =
						new QPInfo(qpaperCode, qp.getQpFilename(), Language.HINDI, secondaryQP.getQpaperCode(),
								secondaryQP.getQpFilename());
			else
				qpInfo = new QPInfo(qpaperCode, qp.getQpFilename());
			qpInfo.setIsPracticeTestTaken(isPracticeTestTaken);
			qpInfo.setQptName(qp.getQpTitle());
			qpInfo.setQpLanguages(qp.getQpLanguages());
			if (qp.getQpLanguages()!=null && !qp.getQpLanguages().isEmpty()) {
				qpInfo.setMultiLingualQP(true);
			}
			
			AcsAssessment assessmentDetails = cdeaService.getAssessmentDetailsByAssessmentCode(assessmentCode);
			String languageInfoJson = assessmentDetails.getLanguageInfo();
			if (languageInfoJson != null) {
				Type type = new TypeToken<LinkedHashMap<String, String>>() {
				}.getType();
				Map<String, String> secondaryToPrimaryQuestion = gson.fromJson(languageInfoJson, type);
				qpInfo.setMapOfSecondaryToPrimaryQuestion(secondaryToPrimaryQuestion);
			}
			// cs.isLandingPageRequired(batchCandidateAssociationForLandingPageCheck)
			boolean practiceTestRequired = acsPropertyUtil.isIIBFExam();
			if (practiceTestRequired && !isPracticeTestTaken) {
				PracticeTestQuestionPaperTo practiceQp = getPracticeQpForAssessment(assessmentCode);
				String practiceQpPath = null;
				if (practiceQp != null && practiceQp.getPtQpaperId() != null && !practiceQp.getPtQpaperId().isEmpty())
					practiceQpPath = ACSConstants.PRACTICE_TEST_DIR + File.separator + practiceQp.getPtQpaperId();
				practiceQpPath = practiceQpPath.replace("\\", "/");
				logger.debug("Practice Qp Exists and located at {}", practiceQpPath);
				qpInfo.setPracticeTestPathForPrimary(practiceQpPath);

				if (practiceQp != null && practiceQp.getSecondaryPTQpaperId() != null
						&& !practiceQp.getSecondaryPTQpaperId().isEmpty())
					practiceQpPath =
							ACSConstants.PRACTICE_TEST_DIR + File.separator + practiceQp.getSecondaryPTQpaperId();
				practiceQpPath = practiceQpPath.replace("\\", "/");
				logger.debug("Secondary Practice Qp Exists and located at {}", practiceQpPath);
				qpInfo.setPracticeTestPathForSecondary(practiceQpPath);

			} else
				qpInfo.setIsPracticeTestRequired(practiceTestRequired);

		} catch (Exception e) {
			// Dunking the exception.
			getLogger().error("Exception while executing getQPInfo...", e);

		}
		logger.debug("getQPInfo is returning {}", qpInfo);
		return qpInfo;
	}

	private AcsQuestionPaper getSecondaryQP(String primaryQp) throws Exception {
		String query = ACSQueryConstants.QUERY_GET_SECONDARY_QP;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("primaryQp", primaryQp);

		// get the child questions if any.
		List<AcsQuestionPaper> questionPaperTOs = session.getListByQuery(query, params);
		if (questionPaperTOs != null && !questionPaperTOs.isEmpty()) {
			return questionPaperTOs.get(0);
		} else {
			return null;
		}

	}

	public void updateLanguageInfo(AcsAssessment assessmentDetailsTO,
			Map<String, String> mapOfSecondaryToPrimaryQuestion) throws GenericDataModelException {
		logger.debug("updateLanguageInfo is initiated with Assessment:{} map:{}",
				assessmentDetailsTO.getAssessmentCode(), mapOfSecondaryToPrimaryQuestion);
		String languageInfoJson = assessmentDetailsTO.getLanguageInfo();
		Type type = new TypeToken<LinkedHashMap<String, String>>() {
		}.getType();
		Map<String, String> secondaryToPrimaryQuestion = gson.fromJson(languageInfoJson, type);
		if (secondaryToPrimaryQuestion == null || secondaryToPrimaryQuestion.isEmpty()) {
			logger.debug("No language Info for assessment:", assessmentDetailsTO.getAssessmentCode());
			assessmentDetailsTO.setLanguageInfo(gson.toJson(mapOfSecondaryToPrimaryQuestion));
		} else {
			logger.debug("Language Info for assessment:{} is{} ", assessmentDetailsTO.getAssessmentCode(),
					secondaryToPrimaryQuestion);
			secondaryToPrimaryQuestion.putAll(mapOfSecondaryToPrimaryQuestion);
			assessmentDetailsTO.setLanguageInfo(gson.toJson(secondaryToPrimaryQuestion));
		}
		session.merge(assessmentDetailsTO);
	}

	/*
	 * Will return the relative file path of the question paper for a given candID and batchCode..
	 */
	public String getQPForCandidate(AcsBatchCandidateAssociation batchCandidateAssociation)
			throws GenericDataModelException, CandidateRejectedException {
		logger.debug("getQPForCandidate for BCAID:{} ", batchCandidateAssociation.getBcaid());
		// check for the candidate whether the Qp is already allocated... May be a second login
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();

		String qpCode = batchCandidateAssociation.getQpaperCode();

		// QP is not allocated..Hence
		if (qpCode == null) {
			String setID = batchCandidateAssociation.getSetID();
			if (setID != null) {
				logger.debug("Set ID {} exists for the candidate {}. Hence allocating using existing QP", setID,
						batchCandidateAssociation.getBcaid());
				qpCode = getQpForSet(batchCandidateAssociation.getAssessmentCode(), setID);
				logger.debug("getQPForCandidate, getQpForSet qpCode={} for BCAID:{}",
						batchCandidateAssociation.getBcaid(), qpCode);
			} else {
				logger.debug(
						"getQPForCandidate, qpCode is null for BCAID:{}, Hence Automatic QP Allocation is happening for the Candidate ",
						batchCandidateAssociation.getBcaid());
				// if static allocation disabled then ACS allocating Automatically Question paper to a candidate
				qpCode =
						QpCandAllocation
								.automaticQpCandAllocation(batchCandidateAssociation.getCandidateId(),
										batchCandidateAssociation.getBatchCode(),
										batchCandidateAssociation.getAssessmentCode());
				logger.debug("getQPForCandidate, Automatic QP Allocated qpCode={} for BCAID:{}",
						batchCandidateAssociation.getBcaid(), qpCode);
			}
		}
		statsCollector.log(startTime, "getQPForCandidate", "QuestionService",
				batchCandidateAssociation.getCandidateId());
		return qpCode;
	}

	/**
	 * @param assessmentCode
	 * @param setID
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getQpForSet(String assessmentCode, String setID) throws GenericDataModelException {
		logger.debug("Getting QP for Assessment:{} and setId:{}", assessmentCode, setID);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		// get QP which contains SetID, Hence %
		params.put("qpaperCode", "%" + setID);
		String qpCode = (String) session.getByQuery(ACSQueryConstants.QUERY_GET_QP_FOR_SET, params);
		return qpCode;
	}

	/**
	 * @param assessmentCode
	 * @param setID
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<String> getQpsForAssessment(String assessmentCode) throws GenericDataModelException {
		logger.debug("Getting QP for Assessment:{} ", assessmentCode);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		List<String> qpCodes = session.getListByQuery(ACSQueryConstants.QUERY_GET_QP_FOR_ASSESSMENT, params);
		return qpCodes;
	}

	/**
	 * get Questions By List of QuestIds
	 * 
	 * @param questionIds
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQpQuestionAssociation> getQPQuestionAssoByQIds(List<Integer> qIds) throws GenericDataModelException {
		if (qIds.isEmpty())
			return null;
		String query = ACSQueryConstants.QUERY_FETCH_QPQUESTASSO_BY_QIDS;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qIds", qIds);
		List<AcsQpQuestionAssociation> acsQuestions =
				(List<AcsQpQuestionAssociation>) session.getResultAsListByQuery(query, params, 0);
		return acsQuestions;
	}

	/**
	 * get Questions By List of QuestIds
	 * 
	 * @param questionIds
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQpQuestionAssociation> getQPQuestionAssoBySIDandQpaperCode(String qpaperCode, String sectionId)
			throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QUESTIONS_FOR_SECTION;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpaperCode);
		params.put("sectionId", sectionId);
		List<AcsQpQuestionAssociation> acsQuestions =
		/* (List<AcsQpQuestionAssociation>) */session.getResultAsListByQuery(query, params, 0);
		return acsQuestions;
	}

	/**
	 * get Questions By List of QpaperCode
	 * 
	 * @param questionIds
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQpQuestionAssociation> getQPQuestionAssoByQpaperCodeAndQType(String qpaperCode,
			List<String> questionType) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QUESTIONS_FOR_QPCODE_QTYPE;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpaperCode);
		params.put("questionType", questionType);
		List<AcsQpQuestionAssociation> acsQuestions = session.getResultAsListByQuery(query, params, 0);
		return acsQuestions;
	}

	/**
	 * get Questions which are not in this List of QuestIds
	 * 
	 * @param questionIds
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsQpQuestionAssociation> getQPQuestionAssoForUnAttemptedQIds(int bcaid, String qpaperCode)
			throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QPQUESTASSO_FOR_UNATTEMPTED_QIDS;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		params.put("qpaperCode", qpaperCode);
		List<AcsQpQuestionAssociation> acsQuestions =
				(List<AcsQpQuestionAssociation>) session.getResultAsListByQuery(query, params, 0);
		return acsQuestions;
	}

	/**
	 * @param questionPaper
	 * @param sectionToQuestionMap
	 * @param mapOfCIQQuestion
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updateQSequenceForQuestionPaper(AcsQuestionPaper questionPaper, String questionSequence,
			String ciqQuestionSequence,String qpLanguages) throws GenericDataModelException {
		StringBuffer query = new StringBuffer("UPDATE " + AcsQuestionPaper.class.getName() + " q set ");
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", questionPaper.getQpaperCode());
		params.put("qpDescription", questionPaper.getQpDescription());
		query.append(" q.qpDescription=(:qpDescription) ");
		if (!questionSequence.isEmpty()) {
			params.put("questionSequence", questionSequence);
			query.append(" , q.questionSequence=(:questionSequence) ");
		}
		if (!ciqQuestionSequence.isEmpty()) {
			params.put("ciqQuestionSequence", ciqQuestionSequence);
			query.append(" , q.ciqQuestionSequence=(:ciqQuestionSequence) ");
		}
		if (!qpLanguages.isEmpty()) {
			params.put("qpLanguages", qpLanguages);
			query.append(" , q.qpLanguages=(:qpLanguages) ");
		}
		query.append(" where q.qpaperCode=(:qpaperCode) ");
		session.updateByQuery(query.toString(), params);

	}

	/**
	 * get Option Shuffle Is Enabled or Not, for Sections
	 * 
	 * @param bRulesExportEntity
	 * @param qpaperCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private Map<Integer, Boolean> getOptionShuffleSeqIsEnabled(AcsBatchCandidateAssociation bca)
			throws GenericDataModelException {
		logger.debug("Inside  getOptionShuffleSeqIsEnabled for bcaId = {}  ", bca.getBcaid());
		BRulesExportEntity bRulesExportEntity =
				customerBatchService.getBRulesExportEntity(bca.getBatchCode(), bca.getAssessmentCode());
		Map<Integer, Boolean> shufflingRulesRulesMap = null;
		String qpaperCode = bca.getQpaperCode();
		if (bRulesExportEntity != null) {
			shufflingRulesRulesMap = new HashMap<Integer, Boolean>();
			List<AcsQpSections> qpSections = getSidsFromQpaperCode(qpaperCode);
			if (qpSections != null && bRulesExportEntity.getSectionLevelRulesMap() != null
					&& !bRulesExportEntity.getSectionLevelRulesMap().isEmpty()) {
				for (AcsQpSections acsQpSections : qpSections) {
					Map<String, SectionLevelRules> sectionLevelRulesMap = bRulesExportEntity.getSectionLevelRulesMap();
					SectionLevelRules sectionLevelRules = sectionLevelRulesMap.get(acsQpSections.getSecIdent());
					if (sectionLevelRules != null) {
						ShufflingRules shufflingRules = sectionLevelRules.getShufflingRules();
						logger.debug("shufflingRules for SectionIdent {}", shufflingRules, acsQpSections.getSecIdent());
						if (shufflingRules != null)
							shufflingRulesRulesMap.put(acsQpSections.getSid(), shufflingRules.isShuffleOptions());
						else {
							shufflingRulesRulesMap.put(acsQpSections.getSid(), bRulesExportEntity.isShuffleOptions());
							logger.debug("No shufflingRules for section Hence AssessmentShufflingRules which applied "
									+ " for section = {} and rule = {} ", acsQpSections.getSid(),
									bRulesExportEntity.isShuffleOptions());
						}
					} else {
						shufflingRulesRulesMap.put(acsQpSections.getSid(), bRulesExportEntity.isShuffleOptions());
						logger.debug("No shufflingRules for section Hence AssessmentShufflingRules which applied "
								+ " for section = {} and rule = {} ", acsQpSections.getSid(),
								bRulesExportEntity.isShuffleOptions());
					}

				}
			}
			if (shufflingRulesRulesMap.isEmpty()) {
				// If NO SectionLevelRules Then apply AssessmentLevelShufflingRules
				shufflingRulesRulesMap = new HashMap<Integer, Boolean>();
				shufflingRulesRulesMap.put(ACSConstants.ZERO, bRulesExportEntity.isShuffleOptions());
				logger.debug(
						"No shufflingRules for section Hence returning AssessmentShufflingRules which applied for all the sections"
								+ " = {} for qpaperCode = {} ", shufflingRulesRulesMap, qpaperCode);
			}
		}
		logger.debug("Returning {} shufflingRulesRulesMap for qpaperCode = {}  ", shufflingRulesRulesMap, qpaperCode);
		return shufflingRulesRulesMap;
	}

}
