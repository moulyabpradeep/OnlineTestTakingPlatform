package com.merittrac.apollo.acs.services;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.qtitools.qti.node.expression.general.BaseValue;
import org.qtitools.qti.node.item.AssessmentItem;
import org.qtitools.qti.node.item.response.processing.ResponseCondition;
import org.qtitools.qti.node.item.response.processing.ResponseIf;
import org.qtitools.qti.node.item.response.processing.ResponseProcessing;
import org.qtitools.qti.node.item.response.processing.SetOutcomeValue;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSConstants.resultLevel;
import com.merittrac.apollo.acs.constants.ACSFilePaths;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.dataobject.ComputeResultProcDO;
import com.merittrac.apollo.acs.dataobject.ResultGenStatusDO;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsQuestion;
import com.merittrac.apollo.acs.entities.AcsQuestionPaper;
import com.merittrac.apollo.acs.quartz.jobs.ResultProcessing;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class ResultService extends BasicService implements IResultService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	Map<String, AssessmentItem> quesIdAssessmentMap;
	Map<Integer, AcsQuestionPaper> qpidQuestionPaperMap;
	Map<String, Double> qpIdScoreMap;
	private static String isAuditEnable = null;
	private static BatchService bs = null;
	private static QuestionService qs = null;

	public ResultService() {
		quesIdAssessmentMap = new ConcurrentHashMap<String, AssessmentItem>();
		qpidQuestionPaperMap = new ConcurrentHashMap<Integer, AcsQuestionPaper>();
		qpIdScoreMap = new ConcurrentHashMap<String, Double>();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
		{
			bs = BatchService.getInstance();
		}
		if (qs == null) {
			qs = new QuestionService();
		}
	}

	// private Double calcResultPerQuesPerCandidate(CandidateResponsesTO candResp) throws GenericDataModelException {
	// Double result = 0.0;
	// String quesId = candResp.getQuestionId();
	// // logger.info("Received QuestionId = {} ", quesId);
	// AssessmentItem assessmentItem = (AssessmentItem) quesIdAssessmentMap.get(quesId);
	// if (assessmentItem == null) {
	// assessmentItem = new AssessmentItem("testitem", "learning", false, false);
	// String acsQuesPath = computeAbsolutePath(candResp.getQuestionId());
	// // logger.info("Received Path = {} where QuestionPaper has been placed", acsQuesPath);
	// File file = new File(acsQuesPath);
	// assessmentItem.load(file);
	// quesIdAssessmentMap.put(quesId, assessmentItem);
	// }
	// String[] responseOption = candResp.getResponseOptions().split(",");
	// String choice = null;
	// Map<String, List<String>> responseMap = new HashMap<String, List<String>>();
	// String response = "RESPONSE";
	// if (responseOption.length > 1) {
	// responseMap.put(response, Arrays.asList(responseOption));
	// } else {
	// choice = responseOption[0];
	// responseMap.put(response, Arrays.asList(new String[] { choice }));
	// }
	//
	// // now test the item
	// assessmentItem.initialize(null);
	// if (candResp.getResponseOptions() != null && choice != null) {
	// assessmentItem.setResponses(responseMap);
	// assessmentItem.processResponses();
	// if (assessmentItem.getResponseProcessing() != null) {
	// result = Double.parseDouble(assessmentItem.getOutcomeValue("SCORE").toString());
	// if (assessmentItem.isIncorrect()) {
	// result = -result;
	// }
	// } else {
	// if (assessmentItem.isCorrect())
	// result = 1.0;
	// if (assessmentItem.isIncorrect())
	// result = 0.0;
	// }
	// }
	// return result;
	// }

	public Double getMaxScoreForQuestion(AssessmentItem assessmentItem) {
		ResponseProcessing responseProcessing = assessmentItem.getResponseProcessing();
		if (responseProcessing == null) {
			logger.info("Response processing relateg tags are missing in the XML file");
			return 1.0;
		}
		ResponseCondition responseCondition = (ResponseCondition) responseProcessing.getResponseRules().get(0);
		ResponseIf responseIf = responseCondition.getResponseIf();
		SetOutcomeValue setOutcomeValue_If = (SetOutcomeValue) responseIf.getResponseRules().get(0);
		BaseValue baseValue_If = (BaseValue) setOutcomeValue_If.getExpression();
		Double score = Double.parseDouble(baseValue_If.getSingleValue().toString());
		return score;
	}

	private String computeAbsolutePath(String QuestionId) throws GenericDataModelException {
		String path = ACSFilePaths.getACSContentDirectory();

		// query relative path from questionTO
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_QUESTIONID;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qusID", QuestionId);

		AcsQuestion ques = (AcsQuestion) session.getByQuery(hql_query, params);

		return path + File.separator + ques.getQtiFileName();
	}

	/*
	 * private void addResultProcStatus(ResultProcStatusTO rps) throws GenericDataModelException {
	 * rps.setStatus("Success"); rps.setMessage("Result Processing done!"); session.persist(rps); }
	 */

	@Override
	public void calcResultPerCandidateInQP(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		// int candidateId = cresultentity.getCandidateId();
		// int quesPaperId = cresultentity.getQuestionPaperId();
		// ResultProcReportTO resultProcReport = new ResultProcReportTO();
		//
		// QuestionPaperTO qpt = qpidQuestionPaperMap.get(quesPaperId);
		//
		// if (qpt == null)
		// {
		// String hql_query_qpaper = ACSQueryConstants.QUERY_FETCH_QPTO;
		// HashMap<String, Integer> params = new HashMap<String, Integer>();
		// params.put("qpID", quesPaperId);
		// qpt = (QuestionPaperTO) session.getByQuery(hql_query_qpaper, params);
		// if (qpt != null)
		// {
		// qpidQuestionPaperMap.put(quesPaperId, qpt);
		// }
		// }
		//
		// String qPaperId = qpt.getQpaperID();
		//
		// // calculate max score for QP
		// if (!qpIdScoreMap.containsKey(qPaperId))
		// {
		// List<String> quesIds = qs.getQuestionIdsInQP(qPaperId);
		// if (quesIds != null && !quesIds.isEmpty())
		// {
		// Double qpTotalScore = 0.0;
		// for (Iterator iterator = quesIds.iterator(); iterator.hasNext();)
		// {
		// String quesId = (String) iterator.next();
		// AssessmentItem assessmentItem = (AssessmentItem) quesIdAssessmentMap.get(quesId);
		// if (assessmentItem == null)
		// {
		// assessmentItem = new AssessmentItem("testitem", "learning", false, false);
		// String acsQuesPath = computeAbsolutePath(quesId);
		// // logger.info("Received Path = {} where QuestionPaper has been placed", acsQuesPath);
		// File file = new File(acsQuesPath);
		// assessmentItem.load(file);
		// quesIdAssessmentMap.put(quesId, assessmentItem);
		// }
		// Double quesMaxScore = getMaxScoreForQuestion(assessmentItem);
		// qpTotalScore = qpTotalScore + quesMaxScore;
		// }
		// qpIdScoreMap.put(qPaperId, qpTotalScore);
		// }
		// }
		//
		// String hql_query = ACSQueryConstants.QUERY_FETCH_PER_CANDID_PER_QPID;
		// String sameScorequery = ACSQueryConstants.QUERY_UPDATE_SAME_SCORE_PER_CANDID;
		// String diffScorequery = ACSQueryConstants.QUERY_UPDATE_DIFF_SCORE_PER_CANDID;
		//
		// HashMap<String, Object> params = new HashMap<String, Object>();
		// params.put("candID", candidateId);
		// params.put("qpID", qPaperId);
		// params.put("batchId", cresultentity.getBatchId());
		//
		// ArrayList<CandidateResponsesTO> candRepList = (ArrayList<CandidateResponsesTO>)
		// session.getListByQuery(hql_query, params);
		//
		// List<String> questionIdList = new ArrayList<String>();
		// List<Double> scoreList = new ArrayList<Double>();
		// HashMap<String, Double> sectionReport = new HashMap<String, Double>();
		// Double totalscorePerCand = 0.0;
		// Double sameScoreperQuest = 0.0;
		//
		// HashMap<String, Object> updateSameScoreparams = new HashMap<String, Object>();
		// HashMap<String, Object> updateDiffScoreparams = new HashMap<String, Object>();
		// boolean countTemp = true;
		// if (candRepList != null && !candRepList.isEmpty())
		// {
		// for (CandidateResponsesTO cr : candRepList)
		// {
		// Double candScore = calcResultPerQuesPerCandidate(cr);
		// totalscorePerCand = totalscorePerCand + candScore;
		//
		// Double value = candScore;
		// if (sectionReport.containsKey(cr.getSectionIdent()))
		// {
		// value = (value + sectionReport.get(cr.getSectionIdent()));
		// }
		//
		// sectionReport.put(cr.getSectionIdent(), value);
		//
		// if (countTemp)
		// {
		// resultProcReport.setCandName(cr.getCandidate().getFirstName());
		// resultProcReport.setCandIdentifier(cr.getCandidate().getIdentifier1());
		// scoreList.add(candScore);
		// countTemp = false;
		// }
		// if (scoreList.contains(candScore))
		// {
		// scoreList.add(candScore);
		// sameScoreperQuest = candScore;
		// questionIdList.add(cr.getQuestionId());
		// }
		// else
		// {
		// updateDiffScoreparams.put("cqsId", cr.getQuestionId());
		// updateDiffScoreparams.put("candID", candidateId);
		// updateDiffScoreparams.put("respscore", candScore);
		// session.updateByQuery(diffScorequery, updateDiffScoreparams);
		// }
		// }
		// updateSameScoreparams.put("cqsId", questionIdList);
		// updateSameScoreparams.put("candID", candidateId);
		// updateSameScoreparams.put("respscore", sameScoreperQuest);
		// session.updateByQuery(sameScorequery, updateSameScoreparams);
		// }
		// else
		// {
		// CandidateTO candidate = (CandidateTO) session.get(candidateId, CandidateTO.class.getCanonicalName());
		// if (candidate != null)
		// {
		// resultProcReport.setCandName(candidate.getFirstName());
		// resultProcReport.setCandIdentifier(candidate.getIdentifier1());
		// }
		// }
		//
		// if (cresultentity.getAssessmentId() == 0)
		// {
		// int assessmentId = qs.getAssessmentIdByQpIdAndBatchId(cresultentity.getQuestionPaperId(),
		// cresultentity.getBatchId());
		// cresultentity.setAssessmentId(assessmentId);
		// }
		//
		// resultProcReport.setAssessmentID(cresultentity.getAssessmentId());
		// resultProcReport.setBatchID(cresultentity.getBatchId());
		// resultProcReport.setEventID(cresultentity.getEventId());
		// resultProcReport.setCandID(candidateId);
		// if (qpIdScoreMap.get(qPaperId) != null)
		// {
		// resultProcReport.setTotalMarks(qpIdScoreMap.get(qPaperId));
		// if (qpIdScoreMap.get(qPaperId) == 0.0)
		// {
		// resultProcReport.setPercntgeMarks(0.0);
		// }
		// else
		// {
		// resultProcReport.setPercntgeMarks((totalscorePerCand / (qpIdScoreMap.get(qPaperId))) * 100);
		// }
		// }
		//
		// List<String> sectionNames = qs.getSectionNamesByQPIdentifier(qPaperId);
		// logger.info("section names = {} for question paper with id = {}", sectionNames, qPaperId);
		//
		// for (Iterator iterator = sectionNames.iterator(); iterator.hasNext();)
		// {
		// String sectionName = (String) iterator.next();
		// if (!sectionReport.keySet().contains(sectionName))
		// {
		// sectionReport.put(sectionName, 0.0);
		// }
		// }
		//
		// Set<SectionReportTO> sectionReports = new HashSet<SectionReportTO>();
		// for (Iterator iterator = sectionReport.keySet().iterator(); iterator.hasNext();)
		// {
		// String sectionIdent = (String) iterator.next();
		// SectionReportTO sectionReportTO = new SectionReportTO(sectionIdent, sectionReport.get(sectionIdent));
		// sectionReports.add(sectionReportTO);
		// }
		// resultProcReport.setSectionReports(sectionReports);
		//
		// ResultProcReportTO resultProcReportTO = getResultProcReportIfExists(resultProcReport);
		// if (resultProcReportTO == null)
		// {
		// session.merge(resultProcReport);
		// }
		// else
		// {
		// if (qpIdScoreMap.get(qPaperId) != null)
		// {
		// resultProcReportTO.setTotalMarks(qpIdScoreMap.get(qPaperId));
		// if (qpIdScoreMap.get(qPaperId) == 0.0)
		// {
		// resultProcReportTO.setPercntgeMarks(0.0);
		// }
		// else
		// {
		// resultProcReportTO.setPercntgeMarks((totalscorePerCand / (qpIdScoreMap.get(qPaperId))) * 100);
		// }
		// }
		//
		// if (resultProcReportTO.getSectionReports() != null && !resultProcReportTO.getSectionReports().isEmpty())
		// {
		// for (Iterator iterator = resultProcReportTO.getSectionReports().iterator(); iterator.hasNext();)
		// {
		// SectionReportTO sectionReportTO = (SectionReportTO) iterator.next();
		// session.delete(sectionReportTO);
		// }
		// }
		// resultProcReportTO.setSectionReports(sectionReports);
		// session.merge(resultProcReportTO);
		// }
	}

	/*
	 * public ResultProcReportTO getResultProcReportIfExists(ResultProcReportTO resultProcReport) { HashMap<String,
	 * Object> params = new HashMap<String, Object>(); params.put("candID", resultProcReport.getCandID());
	 * params.put("assessmentID", resultProcReport.getAssessmentID()); params.put("batchID",
	 * resultProcReport.getBatchID()); params.put("eventID", resultProcReport.getEventID());
	 * 
	 * ResultProcReportTO obj = null; try { obj = (ResultProcReportTO) session .getByQuery(
	 * "FROM ResultProcReportTO WHERE eventID=(:eventID) and batchID=(:batchID) and assessmentID=(:assessmentID) and candID=(:candID)"
	 * , params); } catch (GenericDataModelException ex) { return obj; } return obj; }
	 */

	@Override
	public void calcResultPerAssessmentInBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		String assessmentCode = cresultentity.getAssessmentCode();
		String batchCode = cresultentity.getBatchCode();
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_ASSESSMENTID_PER_BATCHID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);

		List<AcsBatchCandidateAssociation> candidateList = session.getListByQuery(hql_query, params);
		for (AcsBatchCandidateAssociation bca : candidateList) {
			cresultentity.setCandidateId(bca.getCandidateId());
			calcResultPerCandidateInBatch(cresultentity);
		}
	}

	@Override
	public void calcResultPerCandidateInBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		String batchCode = cresultentity.getBatchCode();
		int candId = cresultentity.getCandidateId();
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_BATCHID_PER_CANDID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("candId", candId);

		/*
		 * List<QPCandidateAssosicationTO> qpCandList = session.getListByQuery(hql_query, params); for
		 * (QPCandidateAssosicationTO qpCand : qpCandList) { cresultentity.setQuestionPaperId(qpCand.getQpID());
		 * calcResultPerCandidateInQP(cresultentity); }
		 */
	}

	@Override
	public void calcResultPerBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		String batchCode = cresultentity.getBatchCode();
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_BATCHID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		/*
		 * List<QPCandidateAssosicationTO> qpCandList = session.getListByQuery(hql_query, params); ResultProcStatusTO
		 * rps = new ResultProcStatusTO(); rps.setBatchID(batchId); Calendar startTime = Calendar.getInstance();
		 * Calendar endTime = null; for (QPCandidateAssosicationTO qpCand : qpCandList) {
		 * cresultentity.setCandidateId(qpCand.getCandID()); cresultentity.setQuestionPaperId(qpCand.getQpID());
		 * calcResultPerCandidateInQP(cresultentity); endTime = Calendar.getInstance(); } rps.setStartTime(startTime);
		 * rps.setEndTime(endTime); addResultProcStatus(rps);
		 */
	}

	@Override
	public void calcResultPerBatches(String[] batchCodes, ComputeResultProcDO cresultentity)
			throws GenericDataModelException {
		for (String batchCode : batchCodes) {
			cresultentity.setBatchCode(batchCode);
			calcResultPerBatch(cresultentity);
		}
	}

	@Override
	public void calcResultPerAssessment(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_ASSESSMENTID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentID", cresultentity.getAssessmentCode());

		/*
		 * List<BatchCandidateAssociationTO> batchCandidateAssociations = session.getListByQuery(hql_query, params);
		 * ResultProcStatusTO rps = new ResultProcStatusTO(); rps.setAssessmentID(cresultentity.getAssessmentId());
		 * addResultProcStatus(rps); for (BatchCandidateAssociationTO batchCandidateAssociation :
		 * batchCandidateAssociations) { cresultentity.setBatchId(batchCandidateAssociation.getBatchId());
		 * cresultentity.setCandidateId(batchCandidateAssociation.getCandidateId());
		 * calcResultPerCandidateInBatch(cresultentity); }
		 */
	}

	@Override
	public void calcResultPerEvent(ComputeResultProcDO cresultentity) throws GenericDataModelException {
		String eventId = cresultentity.getEventCode();
		String hql_query = ACSQueryConstants.QUERY_FETCH_PER_EVENT;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("evntID", eventId);

		/*
		 * List<EventDetailsTO> events = session.getListByQuery(hql_query, params); for (EventDetailsTO event : events)
		 * { cresultentity.setEventId(event.getEventId()); Set<AssessmentDetailsTO> assessmentDetails =
		 * event.getAssessmentDetails(); for (AssessmentDetailsTO assessment : assessmentDetails) {
		 * cresultentity.setAssessmentId(assessment.getAssessmentId()); calcResultPerAssessment(cresultentity); } }
		 */
	}

	/*
	 * public void createcanddetails(QPCandidateAssosicationTO qca) throws GenericDataModelException {
	 * session.persist(qca); }
	 */

	/* will start ResultProcessing immediately */
	public boolean startResultProcNow(ComputeResultProcDO resultEntity, resultLevel resultProcType)
			throws GenericDataModelException {
		JobDetail job = null;
		Scheduler scheduler = null;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(ResultProcessing.class)
							.withIdentity("ResultProcessing" + resultEntity.getBatchCode(), "ResultProcessingGroup")
							.storeDurably(false).build();
			job.getJobDataMap().put(ACSConstants.RESULT_ENTITY, resultEntity);
			job.getJobDataMap().put(ACSConstants.RESULT_PROCESSING_LEVEL, resultProcType);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey("ResultProcessing" + resultEntity.getBatchCode(),
							"ResultProcessingGroup"));

			if (trigger != null) {
				logger.info("Already a trigger exists for computing Results for specified batchId = {} ",
						resultEntity.getBatchCode());
				return false;
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity("ResultProcessing" + resultEntity.getBatchCode(), "ResultProcessingGroup")
								.startNow().build();
				scheduler.scheduleJob(job, trigger);
			}
		} catch (SchedulerException ex) {
			logger.error(ex.getMessage(), ex);
			return false;
		}
		return true;
	}

	// This API will trigger result processing on click on generate button.
	// a) Has to update status column to "Inprogress" and on complete of result processing it has to update
	// status to 1.
	@Override
	public Object generateResultsPerBatch(int eventID, int batchID) throws GenericDataModelException {
		/*
		 * ResultGenStatusDO resultGenStatus = new ResultGenStatusDO(); String batchCodeAudit =
		 * bs.getBatchCodebyBatchId(batchID); if (batchID != 0 && eventID != 0) { ComputeResultProcDO resultEntity = new
		 * ComputeResultProcDO(batchID); resultEntity.setEventId(eventID); BatchDetailsTO batchdetails =
		 * (BatchDetailsTO) session.get(batchID, BatchDetailsTO.class.getCanonicalName()); if (batchdetails == null) {
		 * return getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_INTERNAL_ERROR,
		 * ResultStatusEnum.RESULTPROCESSING_FAILED.getType()); } else { if (isAuditEnable != null &&
		 * isAuditEnable.equals("true")) { Object[] paramArray = { eventID, batchCodeAudit }; HashMap<String, String>
		 * logbackParams = new HashMap<String, String>(); if (batchdetails.getStatus() == null ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_NOTSTARTED)) {
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "CREATE");
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.GENERATE_RESULT.toString());
		 * AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData( logbackParams,
		 * ACSConstants.GENERATE_RESULT_MSG, paramArray); } else { logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY,
		 * "UPDATE"); logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
		 * AdminAuditActionEnum.RE_GENERATE_RESULT.toString());
		 * AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData( logbackParams,
		 * ACSConstants.RE_GENERATE_RESULT_MSG, paramArray); } } Calendar testStartTime = (Calendar)
		 * batchdetails.getBatchStartTime().clone(); long startTime = testStartTime.getTimeInMillis(); Calendar
		 * testEndTime = (Calendar) batchdetails.getMaxBatchEndTime().clone(); long endTime =
		 * testEndTime.getTimeInMillis(); long currentTime = Calendar.getInstance().getTimeInMillis(); if (endTime >=
		 * currentTime && currentTime > startTime) { return getResultGenStatus(false,
		 * ACSServerStatusConstants.RESULT_PROC_PER_BATCH_UNDERPROGRESS, null); } else if (currentTime < startTime) {
		 * return getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_PER_BATCH_NOTSTARTED, null); } else {
		 * if (batchdetails.getStatus() == null ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_SUCCESS) ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_FAILED)) { if
		 * (startResultProcNow(resultEntity, resultLevel.batch)) { // quesIdAssessMap = null; // resultProcReport =
		 * null; return getResultGenStatus(true, 0, ResultStatusEnum.RESULTPROCESSING_SUCCESS.getType()); } else {
		 * return getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_INTERNAL_ERROR,
		 * ResultStatusEnum.RESULTPROCESSING_FAILED.getType()); } } else if
		 * (batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_PER_BATCH_IN_PROGRESS)) { return
		 * getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_INPROGRESS,
		 * ResultStatusEnum.RESULTPROCESSING_PER_BATCH_IN_PROGRESS.getType()); } } } } return resultGenStatus;
		 */
		return null;
	}

	@Override
	public Object generateResultsPerAssessmentInBatch(int eventID, int batchID, int assessmentID)
			throws GenericDataModelException {
		/*
		 * ResultGenStatusDO resultGenStatus = new ResultGenStatusDO(); String batchCodeAudit =
		 * bs.getBatchCodebyBatchId(batchID); if (eventID != 0 && batchID != 0 && assessmentID != 0) {
		 * ComputeResultProcDO resultEntity = new ComputeResultProcDO(); resultEntity.setEventId(eventID);
		 * resultEntity.setBatchId(batchID); resultEntity.setAssessmentId(assessmentID);
		 * 
		 * BatchDetailsTO batchdetails = (BatchDetailsTO) session.get(batchID, BatchDetailsTO.class.getCanonicalName());
		 * if (batchdetails == null) { return getResultGenStatus(false,
		 * ACSServerStatusConstants.RESULT_PROC_INTERNAL_ERROR, ResultStatusEnum.RESULTPROCESSING_FAILED.getType()); }
		 * else { if (isAuditEnable != null && isAuditEnable.equals("true")) { Object[] paramArray = { assessmentID,
		 * batchCodeAudit, eventID }; HashMap<String, String> logbackParams = new HashMap<String, String>(); if
		 * (batchdetails.getStatus() == null ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_NOTSTARTED)) {
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "CREATE");
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.GENERATE_RESULT.toString());
		 * AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData( logbackParams,
		 * ACSConstants.GENERATE_RESULT_ASS_MSG, paramArray); } else {
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
		 * logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.RE_GENERATE_RESULT.toString());
		 * AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData( logbackParams,
		 * ACSConstants.RE_GENERATE_RESULT_ASS_MSG, paramArray); } } Calendar testStartTime = (Calendar)
		 * batchdetails.getBatchStartTime().clone(); long startTime = testStartTime.getTimeInMillis(); Calendar
		 * testEndTime = (Calendar) batchdetails.getMaxBatchEndTime().clone(); long endTime =
		 * testEndTime.getTimeInMillis(); long currentTime = Calendar.getInstance().getTimeInMillis(); if (endTime >=
		 * currentTime && currentTime > startTime) { return getResultGenStatus(false,
		 * ACSServerStatusConstants.RESULT_PROC_PER_BATCH_UNDERPROGRESS, null); } else if (currentTime < startTime) {
		 * return getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_PER_BATCH_NOTSTARTED, null); } else {
		 * if (batchdetails.getStatus() == null ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_SUCCESS) ||
		 * batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_FAILED)) { if
		 * (startResultProcNow(resultEntity, resultLevel.perAssessmentInBatch)) { // quesIdAssessMap = null; //
		 * resultProcReport = null; return getResultGenStatus(true, 0,
		 * ResultStatusEnum.RESULTPROCESSING_SUCCESS.getType()); } else { return getResultGenStatus(false,
		 * ACSServerStatusConstants.RESULT_PROC_INTERNAL_ERROR, ResultStatusEnum.RESULTPROCESSING_FAILED.getType()); } }
		 * else if (batchdetails.getStatus().equals(ResultStatusEnum.RESULTPROCESSING_PER_BATCH_IN_PROGRESS)) { return
		 * getResultGenStatus(false, ACSServerStatusConstants.RESULT_PROC_INPROGRESS,
		 * ResultStatusEnum.RESULTPROCESSING_PER_BATCH_IN_PROGRESS.getType()); } } } } return resultGenStatus;
		 */
		return null;
	}

	private ResultGenStatusDO getResultGenStatus(boolean genStatus, int errorCode, String errorMessage) {
		/*
		 * ResultGenStatusDO resultGenStatusDO = new ResultGenStatusDO(); resultGenStatusDO.setGenStatus(genStatus);
		 * resultGenStatusDO.setErrorMessage(errorMessage); resultGenStatusDO.setErrorCode(errorCode); return
		 * resultGenStatusDO;
		 */
		return null;
	}

	public String getResultProcessingStatusForBatch(int batchId) throws GenericDataModelException {
		/*
		 * BatchDetailsTO batchdetails = (BatchDetailsTO) session.get(batchId, BatchDetailsTO.class.getCanonicalName());
		 * if (batchdetails.getStatus() == null) return ResultStatusEnum.RESULTPROCESSING_NOTSTARTED.getType(); return
		 * batchdetails.getStatus().getType();
		 */
		return null;
	}
}
