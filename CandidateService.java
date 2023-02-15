package com.merittrac.apollo.acs.services;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.constants.CandidateBlockingStatus;
import com.merittrac.apollo.acs.constants.CandidateExtendedTimeStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateFeedBackStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateRemoteProctoringStatus;
import com.merittrac.apollo.acs.constants.CandidateTestStatusEnum;
import com.merittrac.apollo.acs.constants.DeploymentModeEnum;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.AssessmentDurationDO;
import com.merittrac.apollo.acs.dataobject.AuditCandidateResponseDO;
import com.merittrac.apollo.acs.dataobject.CandIPBatchAssessmentCodesDO;
import com.merittrac.apollo.acs.dataobject.CandidateActionDO;
import com.merittrac.apollo.acs.dataobject.CandidateAllRCQidDO;
import com.merittrac.apollo.acs.dataobject.CandidateExtensionInfoDO;
import com.merittrac.apollo.acs.dataobject.CandidateHeartBeatResponseDO;
import com.merittrac.apollo.acs.dataobject.CandidateIdDO;
import com.merittrac.apollo.acs.dataobject.CandidateReEnableDO;
import com.merittrac.apollo.acs.dataobject.CandidateResponsesDO;
import com.merittrac.apollo.acs.dataobject.CandidateSectionLanguagesDO;
import com.merittrac.apollo.acs.dataobject.CandidateSectionTimingDetailsDOWrapper;
import com.merittrac.apollo.acs.dataobject.CandidateStatusDO;
import com.merittrac.apollo.acs.dataobject.CandidateTimeExtensionDetailsDO;
import com.merittrac.apollo.acs.dataobject.ForceSubmitCandidateDO;
import com.merittrac.apollo.acs.dataobject.NetworkSpeedDetailsDO;
import com.merittrac.apollo.acs.dataobject.QuestionPaperMetadata;
import com.merittrac.apollo.acs.dataobject.RCMetadata;
import com.merittrac.apollo.acs.dataobject.RcQuestionInfo;
import com.merittrac.apollo.acs.dataobject.TpQuestionPaperMetadata;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.dataobject.remoteproctoring.RPEventCommunicationDO;
import com.merittrac.apollo.acs.dataobject.tp.AbstractAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.AuditOfNavigationDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateDetailsDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateLastViewedDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateResourcesAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.ChangeOptionAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.GeneralAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.MarkReviewAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.QuestionNavigationAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.SectionNavigationAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.SubmitAnswerAuditDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidatAudits;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCandidateResponses;
import com.merittrac.apollo.acs.entities.AcsCandidateResponsesId;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.AcsProperties;
import com.merittrac.apollo.acs.entities.AcsQuestion;
import com.merittrac.apollo.acs.entities.AcsQuestionPaper;
import com.merittrac.apollo.acs.exception.ACSException;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.exception.InvalidIPRangeException;
import com.merittrac.apollo.acs.heartbeat.CandidateHeartBeat;
import com.merittrac.apollo.acs.heartbeat.PlayerHeartBeat;
import com.merittrac.apollo.acs.heartbeat.SystemHeartBeat;
import com.merittrac.apollo.acs.quartz.jobs.AttendancePackUploader;
import com.merittrac.apollo.acs.quartz.jobs.AttendanceReportGenerator;
import com.merittrac.apollo.acs.quartz.jobs.CandidateForceSubmit;
import com.merittrac.apollo.acs.quartz.jobs.CandidateInformationNotifier;
import com.merittrac.apollo.acs.quartz.jobs.CandidateLiveStatusUpdater;
import com.merittrac.apollo.acs.quartz.jobs.EODReportUploader;
import com.merittrac.apollo.acs.quartz.jobs.IsAllCandidatesSubmittedTest;
import com.merittrac.apollo.acs.quartz.jobs.QpackInformationNotifier;
import com.merittrac.apollo.acs.quartz.jobs.RPackGeneratorAtAET;
import com.merittrac.apollo.acs.quartz.jobs.RPackGeneratorAtBET;
import com.merittrac.apollo.acs.quartz.jobs.ReportPackGenerator;
import com.merittrac.apollo.acs.quartz.jobs.ReportPackUploader;
import com.merittrac.apollo.acs.quartz.jobs.RpackGenerationInitiator;
import com.merittrac.apollo.acs.quartz.jobs.RpackGeneratorForEndedCandidates;
import com.merittrac.apollo.acs.quartz.jobs.RpackUploader;
import com.merittrac.apollo.acs.quartz.jobs.ScoreCalculator;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.cdb.entity.CandidateStatus;
import com.merittrac.apollo.common.EventRule;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.HBMStatusEnum;
import com.merittrac.apollo.common.entities.acs.QpackInformationDO;
import com.merittrac.apollo.common.entities.acs.ResourceUsageInfo;
import com.merittrac.apollo.common.entities.acs.ResponseMarkBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.RequestStatus;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.MarksAndScoreRules;
import com.merittrac.apollo.reports.ProvisionalCertificateEntity;
import com.merittrac.apollo.tp.mif.MIFForm;

public class CandidateService extends BasicService implements ICandidateService {
	private static Logger gen_logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	// TODO: Madhu:make the static variable private
	// public static ConcurrentHashMap<Integer, CandidateStatusDO> candidateData =
	// new ConcurrentHashMap<Integer, CandidateStatusDO>();
	private static BatchService bs = null;
	private static CandidateService candidateService = null;
	private static CDEAService cdeas = null;
	private static AuthService authService = null;
	private static String isAuditEnable = null;
	private static boolean isSingleQRCSupportNeed = false;
	private static BatchCandidateAssociationService batchCandidateAssociationService = null;
	private static QuestionService questionService = null;
	private static BrLrService brlrService = null;
	private static CandidateAuditService candidateAuditService = null;
	static ACSPropertyUtil acsPropertyUtil = null;
	private static ACSEventRequestsService acsEventRequestsService = null;
	private static Gson gson = null;
	private static CustomerBatchService customerBatchService = null;
	private static ScoreCalculator scoreCalculator = null;
	static {
		if (acsEventRequestsService == null)
			acsEventRequestsService = new ACSEventRequestsService();
		if (acsPropertyUtil == null)
			acsPropertyUtil = new ACSPropertyUtil();
		bs = BatchService.getInstance();
		authService = new AuthService();
		cdeas = CDEAService.getInstance();
		batchCandidateAssociationService = new BatchCandidateAssociationService();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
		isSingleQRCSupportNeed = AutoConfigure.getRCSupportConfigureValue();
		questionService = QuestionService.getInstance();
		brlrService = BrLrService.getInstance();
		candidateAuditService = CandidateAuditService.getInstance();
		gson = new Gson();
		customerBatchService = new CustomerBatchService();
		scoreCalculator = new ScoreCalculator();
	}

	private CandidateService() {
	}

	/**
	 * To access the static service
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final CandidateService getInstance() {
		if (candidateService == null) {
			synchronized (CandidateService.class) {
				if (candidateService == null) {
					candidateService = new CandidateService();
				}
			}
		}
		return candidateService;
	}

	/**
	 * @return the gen_logger
	 */
	public static Logger getLogger() {
		return gen_logger;
	}

	@Override
	public void insertCandidateAuditData(Integer batchCandidateAssociationId, String auditMessage)
			throws GenericDataModelException {

		AcsCandidatAudits candidateAuditTrailTO = new AcsCandidatAudits();
		candidateAuditTrailTO.setAcsbatchcandidateassociation(batchCandidateAssociationId);
		candidateAuditTrailTO.setMessage(auditMessage);
		candidateAuditTrailTO.setTimestamp(Calendar.getInstance());
		session.save(candidateAuditTrailTO);
	}

	@Override
	public AcsCandidateStatus getCandidateStatus(Integer batchCandidateAssociationId) throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		AcsCandidateStatus cs =
				(AcsCandidateStatus) session.get(batchCandidateAssociationId,
						AcsCandidateStatus.class.getCanonicalName());
		return cs;
	}

	@Override
	public boolean startCandidateExam(int batchCandidateAssociationId, int candidateId, long clientTime,
			String batchCode) throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("initiated startCandidateExam where candidateId = {} and batchCode = {}", candidateId,
				batchCode);

		// current time instance
		Calendar cal = Calendar.getInstance();

		// fetch batch details
		AcsBatch bd = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
		gen_logger.info("batch details = {} where batchId = {}", bd, batchCode);
		AcsBatchCandidateAssociation bca = (AcsBatchCandidateAssociation) get(batchCandidateAssociationId,
				AcsBatchCandidateAssociation.class.getCanonicalName());
		if (bd != null) {
			Calendar batchStartTime = authService.getMaxCandidateBatchStartTime(
					bca.getExtendedBatchStartTimePerCandidate(), bd.getMaxBatchStartTime());
			if (batchStartTime.after(cal)) {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_EARLY_START_NOTALLOWED,
						"Candidate rejected due to early start of exam");
			}

			if (cal.after(bd.getMaxDeltaRpackGenerationTime())) {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_LATE_START_NOTALLOWED,
						"Candidate rejected due to late start of exam");
			}
		}


		// fetch candidate status details
		AcsCandidateStatus cs = bca.getAcscandidatestatus();
		gen_logger.info("candidate status details = {} where candidateId = {} and batchCode = {}", cs, candidateId,
				batchCode);
		if (bca.getActualTestEndTime() != null)
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_UNAUTHORIZED,
					"Candidate Login Rejected because of duplicate login..");
		gen_logger.info("previous heart beat time = {}", cs.getLastHeartBeatTime());

		// fetch candidate primary identifier
		String candIdentifier =
				candidateService.getCandidateIdentifierFromCandId(bca.getCandidateId());
		Calendar actualTestStartedTime = null;
		CandidateReEnableDO candidateReEnableDO = null;
		if(bca.getReenablecandidatejson()==null)
			actualTestStartedTime = bca.getActualTestStartedTime();
		else {
			candidateReEnableDO =
					gson.fromJson(bca.getReenablecandidatejson(), CandidateReEnableDO.class);
			actualTestStartedTime = candidateReEnableDO.getActualTestStartedTime();
		}

		if (actualTestStartedTime != null) {
			if (cs.getLastHeartBeatTime() != null) {
				// audit for incident log
				IncidentAuditActionDO incidentAuditAction =
						new IncidentAuditActionDO(candIdentifier, bca.getHostAddress(), TimeUtil.convertTimeAsString(
								cal.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
								IncidentAuditLogEnum.TP_OR_SEB_CRASH, bd.getBatchCode(),
								TimeUnit.MILLISECONDS.toSeconds(cal.getTimeInMillis()
										- cs.getLastHeartBeatTime().getTimeInMillis()));

				AuditTrailLogger.incidentAudit(incidentAuditAction);
			} else {
				gen_logger.info("candidate last heart beat is null, hence skipping the incident report log");
			}
		} else {
			gen_logger
					.info("candidate actual test started time is not null hence, considering candidate start exam as fresh start");
		}

		// Added for Audit Trail Report
		// String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(candidateId, batchId);
		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociationId);
		abstractAuditActionDO.setActionType(CandidateActionsEnum.STARTEXAM);
		// abstractAuditActionDO.setAssessmentID(assessmentTypeValue);
		abstractAuditActionDO.setActionTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		abstractAuditActionDO.setCandID(candidateId);
		// abstractAuditActionDO.setCandApplicationNumber(candIdentifier);
		abstractAuditActionDO.setBatchCode(batchCode);
		// abstractAuditActionDO.setBatchCode(bs.getBatchCodebyBatchId(batchId));
		abstractAuditActionDO.setHostAddress(bca.getHostAddress());
		abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));

		AuditTrailLogger.auditSave(abstractAuditActionDO);

		// end Audit Trail Report

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("lastHeartBeatTime", cal);
		params.put("clearLogin", false);
		params.put("lastCandActionTime", cal);
		params.put("currentStatus", CandidateTestStatusEnum.IN_PROGRESS);

		if (actualTestStartedTime == null) {
			params.put("actualTestStartedTime", cal);
			params.put("isPresent", ACSConstants.IS_PRESENT);
		}

		params.put("csid", batchCandidateAssociationId);

		if (actualTestStartedTime == null) {
			cs.setLastHeartBeatTime(cal);
			cs.setPrevHeartBeatTime(cal);
			cs.setIsPracticeTestTaken(true);
			bca.setClearLogin(false);
			cs.setLastCandActionTime(cal);
			cs.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
			if (candidateReEnableDO == null)
				bca.setActualTestStartedTime(cal);
			else {
				candidateReEnableDO.setActualTestStartedTime(cal);
				String reenablecandidatejson = gson.toJson(candidateReEnableDO);
				bca.setReenablecandidatejson(reenablecandidatejson);
			}
			bca.setIsPresent(ACSConstants.IS_PRESENT);
		} else {
			cs.setLastCrashedTime(cs.getLastHeartBeatTime());
			cs.setLastHeartBeatTime(cal);
			cs.setPrevHeartBeatTime(cal);
			cs.setIsPracticeTestTaken(true);
			bca.setClearLogin(false);
			cs.setLastCandActionTime(cal);
			cs.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
		}

		bca.setAcscandidatestatus(cs);
		batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(bca);

		StatsCollector.getInstance().log(startTime, "startCandidateExam", "CandidateService", candidateId);
		return true;
	}

	/**
	 * endCandidateExamTP API will initiates when there is a force submit happens from ACS,here @param acsCall indicates
	 * call from ACS or TP,Since this API used only for ACS force submit so value here always true..
	 */
	@Override
	public boolean endCandidateExam(int batchCandidateAssociationId, String batchCode, boolean forceSubmit)
			throws GenericDataModelException, CandidateRejectedException {
		return endCandidateExamTP(batchCandidateAssociationId, forceSubmit, 0, true, batchCode,0,0);
	}

	/*
	 * To end candidate exam.
	 */
	@Override
	public boolean endCandidateExamTP(int batchCandidateAssociationId, boolean forceSubmit, long clientTime,
			boolean acsCall, String batchCode,int timeTaken,int qid) throws GenericDataModelException, CandidateRejectedException {

		String query = " update " + AcsCandidateStatus.class.getName() + " set ";
		HashMap<String, Object> params = new HashMap<>();
		long startTime = StatsCollector.getInstance().begin();
		Calendar presentTime = Calendar.getInstance();
		CandidateFeedBackStatusEnum feedBackStatusEnum = null;
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				(AcsBatchCandidateAssociation) get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());

		if (!acsCall && forceSubmit && acsPropertyUtil.isRemainingTimeCheckRequiredForEndTest()
				&& acsBatchCandidateAssociation.getActualSectionStartedTime() == null) {
			gen_logger.debug("Remaining time check will be performed.");
			long remainingTimeForOnGoingExam = getRemainingTimeForOnGoingExam(batchCandidateAssociationId);
			if (remainingTimeForOnGoingExam > ACSConstants.BUFFERTIME_FOR_ENDCANDIDATEEXAM_IN_SEC) {
				gen_logger.error("Remaining time for candidate with bcaID {} is {}. Hence force submit is not allowed",
						batchCandidateAssociationId, remainingTimeForOnGoingExam);
				throw new CandidateRejectedException(ACSExceptionConstants.FORCE_SUBMIT_NOT_ALLOWED,
						"Candidate still has time. Hence force submit is not allowed");
			}
		}

		AcsCandidateStatus cs = acsBatchCandidateAssociation.getAcscandidatestatus();
		batchCode = acsBatchCandidateAssociation.getBatchCode();

		boolean isTestEndNull = false;
		if (acsBatchCandidateAssociation.getActualTestEndTime() == null)
			isTestEndNull = true;

		if (cs == null) {
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_NOEND_WITHOUT_START,
					"Can't end test without starting the exam..");
		} else {
			acsBatchCandidateAssociation.setIsForceSubmitted(forceSubmit);
			acsBatchCandidateAssociation.setClearLogin(false);
			// added this for default query to update
			query = query + " latestModifiedStatusTime=(:latestModifiedStatusTime) ";
			params.put("latestModifiedStatusTime", null);
			// tp call, candidate clicked on end exam button OR force submit by tp
			if (!acsCall && acsBatchCandidateAssociation.getActualTestEndTime() == null) {
				acsBatchCandidateAssociation.setActualTestEndTime(presentTime);
				cs.setLastHeartBeatTime(presentTime);
				query = query + " , lastHeartBeatTime=(:lastHeartBeatTime) ";
				params.put("lastHeartBeatTime", presentTime);
				cs.setLastCandActionTime(presentTime);
				query = query + " , lastCandActionTime =(:lastCandActionTime )";
				params.put("lastCandActionTime", presentTime);
			}

			// acs call, force submit by ACS
			if (forceSubmit && acsCall && acsBatchCandidateAssociation.getActualTestEndTime() == null)
				acsBatchCandidateAssociation.setActualTestEndTime(presentTime);

			feedBackStatusEnum = acsBatchCandidateAssociation.getFeedBackStatus();
			// feed back changes
			/**
			 * added additional feedback status because in case TP fails to call API =
			 * "auditCandidateEventsDuringFeedBack" and candidate test end status is not updated.
			 */
			if (feedBackStatusEnum.equals(CandidateFeedBackStatusEnum.NOT_APPLICABLE)
					|| feedBackStatusEnum.equals(CandidateFeedBackStatusEnum.FEEDBACK_NOT_CHOOSEN)) {
				cs.setCurrentStatus(CandidateTestStatusEnum.ENDED);
				query = query + " , currentStatus =(:currentStatus) ";
				params.put("currentStatus", CandidateTestStatusEnum.ENDED);
			} else {
				cs.setCurrentStatus(CandidateTestStatusEnum.TEST_COMPLETED);
				// query = query + " , currentStatus = " + CandidateTestStatusEnum.TEST_COMPLETED;
				query = query + " , currentStatus = (:currentStatus) ";
				params.put("currentStatus", CandidateTestStatusEnum.TEST_COMPLETED);
			}
			if (!acsCall) {
				cs.setPlayerStatus(HBMStatusEnum.GREEN);
				cs.setSystemStatus(HBMStatusEnum.GREEN);

				query = query + " , playerStatus = (:playerStatus)";
				query = query + " , systemStatus = (:systemStatus )";

				params.put("playerStatus", HBMStatusEnum.GREEN);
				params.put("systemStatus", HBMStatusEnum.GREEN);
			}
		}

		// Added for Audit Trail Report

		String assessmentTypeValue = acsBatchCandidateAssociation.getAssessmentCode();
		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociationId);
		if (acsCall == true && forceSubmit == true) {
			clientTime = presentTime.getTimeInMillis();
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FORCEENDACS);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		} else if (acsCall == false && forceSubmit == true) {
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FORCEENDTP);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));

		} else {
			abstractAuditActionDO.setActionType(CandidateActionsEnum.ENDEXAM);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		}

		abstractAuditActionDO.setAssessmentID(assessmentTypeValue);
		abstractAuditActionDO.setCandID(batchCandidateAssociationId);
		String candidateIdentifier =
				candidateService.getCandidateIdentifierFromCandId(acsBatchCandidateAssociation.getCandidateId());
		abstractAuditActionDO.setCandApplicationNumber(candidateIdentifier);
		abstractAuditActionDO.setBatchCode(batchCode);
		abstractAuditActionDO.setHostAddress(acsBatchCandidateAssociation.getHostAddress());

		AuditTrailLogger.auditSave(abstractAuditActionDO);
		// end Audit Trail Report

		// added for feedBack status to update
		if ((acsCall) && (forceSubmit) && (feedBackStatusEnum.equals(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED))) {
			gen_logger.info(" feedBack status = {} for batch candidate id ={} of batch ={} and forceSubmit "
					+ "value={} and acsCall value={}", feedBackStatusEnum, batchCandidateAssociationId, batchCode,
					forceSubmit, acsCall);
			feedBackStatusEnum = CandidateFeedBackStatusEnum.FORCE_SUBMITTED_BY_ACS;
			acsBatchCandidateAssociation.setFeedBackStatus(feedBackStatusEnum);
			cs.setCurrentStatus(CandidateTestStatusEnum.ENDED);

			query = query + " , currentStatus = (:currentStatus) ";
			params.put("currentStatus", CandidateTestStatusEnum.ENDED);
			// added code for Audit
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FEEDBACK_FORCE_SUBMITTED_BY_ACS);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
					TimeUtil.DISPLAY_DATE_FORMAT));
			AuditTrailLogger.auditSave(abstractAuditActionDO);
			// ends here
		}
		// ends here

		// session.saveOrUpdate(cs);
		query = query + " where csid = (:csid) ";
		params.put("csid", cs.getCsid());

		session.updateByQuery(query, params);

		// update time spent by candidate
		long prevSpentTime = acsBatchCandidateAssociation.getTimeSpentInSecs() == null ? 0
				: acsBatchCandidateAssociation.getTimeSpentInSecs();


		String reEnableJson = acsBatchCandidateAssociation.getReenablecandidatejson();
		// long currSpentTime = 0;
		long timeSpentInSecs = 0;
		if(prevSpentTime==0 && reEnableJson==null)
			timeSpentInSecs = this.getCandidateSpentTime(acsBatchCandidateAssociation);
		else
			timeSpentInSecs += prevSpentTime;
		if (reEnableJson != null && !reEnableJson.isEmpty()) {
			// Re birth flow
			CandidateReEnableDO candidateReEnableDO =
					gson.fromJson(reEnableJson, CandidateReEnableDO.class);
			timeSpentInSecs += this.getReEnabledCurrentTimeSpent(acsBatchCandidateAssociation, candidateReEnableDO);
		}

		gen_logger.debug("updating Candidate Status of csid:{} = {}", cs.getCsid(), cs.getCurrentStatus());
		if (isTestEndNull)
			batchCandidateAssociationService.updateTestEndDetails(batchCandidateAssociationId, presentTime, forceSubmit,
					feedBackStatusEnum, timeSpentInSecs);
		else
			batchCandidateAssociationService.updateTestFeedBackStatus(batchCandidateAssociationId, forceSubmit,
					feedBackStatusEnum, timeSpentInSecs);


		gen_logger.debug("updated Candidate Status successfully = {}", acsBatchCandidateAssociation);

		// update the candidate time taken for the question. The time captured is for the previous question.
		if(timeTaken > 0){
			params.clear();
			params.put("bcaid",batchCandidateAssociationId);
			params.put("qid", qid);
			params.put("markedforreview", false);
			params.put("score",0);
			params.put("timetaken",timeTaken);
			
			int count = session.insertByNativeSql(ACSQueryConstants.QUERY_UPDATE_CANDIDATE_TIME_TAKEN_BY_QUESTION_ID, params);
		}
		
		if (acsPropertyUtil.isScoreCalculationRequiredAtEndExam()) {
			long startTimeForScoreCalculation = StatsCollector.getInstance().begin();
			ScoreCalculator calculator = new ScoreCalculator();
			calculator.calculateScoreForACandidate(acsBatchCandidateAssociation);
			StatsCollector.getInstance().log(startTimeForScoreCalculation, "ScoreCalculation", "CandidateService",
					batchCandidateAssociationId);
		}


		// session.saveOrUpdate(cs);
		StatsCollector.getInstance().log(startTime, "endCandidateExamTP", "CandidateService",
				batchCandidateAssociationId);
		return true;
	}

	private long getReEnabledCurrentTimeSpent(AcsBatchCandidateAssociation batchCandidateAssociation,
			CandidateReEnableDO candidateReEnableDO) throws GenericDataModelException {

		long spentTime = 0;
		AcsCandidateStatus candidatestatus = batchCandidateAssociation.getAcscandidatestatus();
		if (candidateReEnableDO.getActualTestStartedTime() != null) {
			Calendar actualTestEndTime = batchCandidateAssociation.getActualTestEndTime();
			if (batchCandidateAssociation.getIsForceSubmitted()) {
				actualTestEndTime = candidatestatus.getLastHeartBeatTime();
			}
			if (batchCandidateAssociation.getClearLoginCount() == 0) {
				// Not a crashed scenario.
				spentTime = actualTestEndTime.getTimeInMillis()
						- candidateReEnableDO.getActualTestStartedTime().getTimeInMillis();
			} else if (batchCandidateAssociation.getClearLoginCount() > 0) {
				// Crashed scenario.
				spentTime = (actualTestEndTime.getTimeInMillis()
						- candidateReEnableDO.getActualTestStartedTime().getTimeInMillis())
						// as total prev lost time in seconds, hence convert to milis
						- (candidateReEnableDO.getTotalLostTime() * 1000);
			}
		} else {
			gen_logger.info("Candidate hasn't started the test");
		}
		// spent time in seconds
		spentTime = (spentTime / 1000);

		// actual extended time
		long extendedTime=(batchCandidateAssociation.getTotalExtendedExamDuration()+candidateReEnableDO.getAllotedDuration());

		// truncate spent time to assessment duration time if exceeds
		// don't truncate if Extended time from ACS
		spentTime = truncateCandidateSpentTime(batchCandidateAssociation.getAssessmentCode(),
				batchCandidateAssociation.getBatchCode(), spentTime,
				extendedTime);

		return (spentTime);

	}

	@Override
	public boolean isExamEnded(int batchCandidateAssociationId) throws GenericDataModelException {
		AcsBatchCandidateAssociation bca =
				(AcsBatchCandidateAssociation) session.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());
		if (bca.getActualTestEndTime() != null)
			return true;
		else
			return false;
	}

	@Override
	public boolean isExamStarted(int batchCandidateAssociationId) throws GenericDataModelException {
		AcsBatchCandidateAssociation bca =
				(AcsBatchCandidateAssociation) session.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());
		if (bca.getActualTestStartedTime() != null)
			return true;
		else
			return false;
	}

	@Override
	public void setCandidate(AcsCandidate candidateDetails) throws GenericDataModelException {
		session.persist(candidateDetails);
	}

	/*
	 * This method is used to create the quartz job for candidate heart beat module. Once the job is created it
	 * recursively run every 3 mins..
	 * 
	 * RESTful URI for initiating heart beat jobs : http://localhost:8090/mtacs/rest/acs/startHeartBeatJobs(URI to start
	 * candidate heart beat related jobs)
	 */
	@Override
	public boolean
			startCandidateHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
					throws GenericDataModelException {
		if (endTime == null || startTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null) {
				startTime = batchDetails.getMaxBatchStartTime();
				endTime = batchDetails.getMaxDeltaRpackGenerationTime();
			} else {
				gen_logger.info("batchDetails = {} hence unable to start CandidateHeartBeatJob", batchDetails);
				return false;
			}
		}

		if (endTime.before(Calendar.getInstance())) {
			gen_logger.info("Not initiating candidateHeartBeat job because endTime is elapsed...");
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;

		String CAND_HB_NAME = ACSConstants.CAND_HB_NAME;
		String CAND_HB_GRP = ACSConstants.CAND_HB_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(CAND_HB_NAME + batchCode, CAND_HB_GRP);
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
			if (triggers != null && !triggers.isEmpty()) {
				for (Iterator iterator = triggers.iterator(); iterator.hasNext();) {
					Trigger trigger = (Trigger) iterator.next();
					if (trigger != null) {
						if (reschedule) {
							scheduler.unscheduleJob(TriggerKey.triggerKey(CAND_HB_NAME + batchCode, CAND_HB_GRP));
							break;
						} else {
							gen_logger.info(
									"Already a trigger exists for candidateHeartBeat for specified batchId = {} ",
									batchCode);
							return false;
						}
					}
				}
			}

			job =
					JobBuilder.newJob(CandidateHeartBeat.class).withIdentity(CAND_HB_NAME + batchCode, CAND_HB_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSConstants.BATCH_START_TIME, startTime);
			job.getJobDataMap().put(ACSConstants.BATCH_END_TIME, endTime);

			Trigger trigger = null;
			// Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(CAND_HB_NAME + batchId, CAND_HB_GRP));
			// gen_logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

			// if (trigger != null) {
			// if (reschedule) {
			// scheduler.unscheduleJob(TriggerKey.triggerKey(CAND_HB_NAME + batchId, CAND_HB_GRP));
			// } else {
			// gen_logger.info("Already a trigger exists for candidateHeartBeat for specified batchId = {} ",
			// batchId);
			// return false;
			// }
			// }
			if (startTime != null && startTime.after(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CAND_HB_NAME + batchCode, CAND_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(startTime.getTime()).endAt(endTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CAND_HB_NAME + batchCode, CAND_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().endAt(endTime.getTime()).build();
			}
			gen_logger.trace("Trigger for Candidate Heart Beat Processing = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startCandidateHeartBeatJob...", ex);
			return false;
		}
		return true;
	}

	@Override
	public boolean clearAnswer(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		boolean response = submitAnswer(candAction);
		statsCollector.log(startTime, "clearAnswer", "CandidateService", candAction.getCandID());
		return response;
	}

	// API used for storing candidate response for an answered question
	@Override
	public boolean submitAnswer(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		int bcaId = candAction.getBatchCandidateAssociationId();
		int candID = candAction.getCandID();
		String qID = candAction.getqID();
		String qpID = candAction.getQpID();
		String secID = candAction.getSecID();
		String respID = candAction.getRespID();
		String secondaryQpID = candAction.getSecondaryQpID();
		String secondaryQID = candAction.getSecondaryQID();
		String language = candAction.getLanguage();
		// setting null so that it is marked as clear answer and all response counts are not wrong
		// Added check for DC:null since during End test call the "GetResponses" returns right statistics (submit
		// response and clear are async)
		if (respID == null || respID.isEmpty() || respID.equals("{\"DC\":null}"))
			respID = null;
		Calendar cal = Calendar.getInstance();

		AcsCandidateStatus status =
				(AcsCandidateStatus) session.get(bcaId, AcsCandidateStatus.class.getCanonicalName());
		status.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
		status.setLastCandActionTime(cal);

		/*AcsCandidateResponses resp_existed =
				new AcsCandidateResponses(new AcsCandidateResponsesId(bcaId, candAction.getqIdentifier()),
						candAction.isMark(), respID, 0);
		resp_existed.setSecondaryQID(secondaryQID);
		resp_existed.setSecondaryQpID(secondaryQpID);
		resp_existed.setLanguage(language);
		// Update time taken by the candidate for a question.
		if(candAction.getTimeSpentInLastQuestion() > 0){
			resp_existed.setTimeTaken(resp_existed.getTimeTaken() + candAction.getTimeSpentInLastQuestion());
		}
		session.saveOrUpdate(resp_existed);*/
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaId);
		params.put("qid", candAction.getqIdentifier());
		params.put("markedforreview", candAction.isMark());
		params.put("responseoptions", respID);
		params.put("score", 0);
		params.put("secondaryqid", secondaryQID);
		params.put("secondaryqpid", secondaryQpID);
		params.put("language", language);
		params.put("timetaken", candAction.getTimeSpentInLastQuestion());
		session.insertByNativeSql(ACSQueryConstants.QUERY_INSERT_CANDIDATE_RESPONSE, params);

		/**
		 * Get response count and set it to AcsCandidateStatus.
		 */
		int responseCount = getAttemptedQuestions(bcaId);
		status.setResponseCount(responseCount);
		session.saveOrUpdate(status);

		long mill = candAction.getDurationMillisecs();

		SubmitAnswerAuditDO submitAnswerAuditDO = new SubmitAnswerAuditDO();
		submitAnswerAuditDO.setCandID(candID);
		submitAnswerAuditDO.setBatchCandidateAssociation(bcaId);
		// TODO : commented for time being need to revert
		// submitAnswerAuditDO.setCandApplicationNumber(cand.getIdentifier1());
		// submitAnswerAuditDO.setBatchID(batchId);
		submitAnswerAuditDO.setBatchCode(candAction.getBatchCode());
		// TODO: TP has to send the ip address
		// submitAnswerAuditDO.setHostAddress(cs.getHostAddress());
		submitAnswerAuditDO.setResponse(respID);
		submitAnswerAuditDO.setqID(qID);
		submitAnswerAuditDO.setQpID(qpID);
		submitAnswerAuditDO.setSecID(secID);
		submitAnswerAuditDO.setTpQID(candAction.getTpQId());// new param
		submitAnswerAuditDO.setParentQuestionId(candAction.getParentQuestionId());
		submitAnswerAuditDO.setQuestionType(candAction.getQuestionType());
		submitAnswerAuditDO.setClientTime(TimeUtil.convertTimeAsString(candAction.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));
		submitAnswerAuditDO.setAutoSave(candAction.isAutoSave());

		if (respID != null)
			submitAnswerAuditDO.setActionType(CandidateActionsEnum.SUBMITANSWER);
		else
			submitAnswerAuditDO.setActionType(CandidateActionsEnum.CLEARANSWER);

		if (candAction.getDurationMillisecs() != 0)
			submitAnswerAuditDO.setActionTime(TimeUtil.formatTime(mill));
		submitAnswerAuditDO.setSecondaryQID(secondaryQID);
		submitAnswerAuditDO.setSecondaryQpID(secondaryQpID);
		submitAnswerAuditDO.setLanguage(language);
		AuditTrailLogger.auditSave(submitAnswerAuditDO);

		statsCollector.log(startTime, "submitAnswer", "CandidateService", candID);
		return true;
	}
	
	/*
	 * Can be used to mark and unmark an item for review for a candidate. If you send mark as true then the question is
	 * marked if you mark false then the question is unmarked.
	 */
	@Override
	public boolean markItemForReview(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		int bcaId = candAction.getBatchCandidateAssociationId();
		long startTime = statsCollector.begin();
		int candID = candAction.getCandID();
		String qID = candAction.getqID();
		String qpID = candAction.getQpID();
		String secID = candAction.getSecID();
		boolean mark = candAction.isMark();
		String secondaryQpID = candAction.getSecondaryQpID();
		String secondaryQID = candAction.getSecondaryQID();
		String language = candAction.getLanguage();

		Calendar cal = Calendar.getInstance();

		AcsCandidateStatus status =
				(AcsCandidateStatus) session.get(bcaId, AcsCandidateStatus.class.getCanonicalName());
		status.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
		status.setCurrentViewedQuestion(qID);
		status.setLastCandActionTime(cal);
		session.saveOrUpdate(status);

		// Updates the lastHeartBeatTime.
		// commented the code since TP heart beat enabled
		// updateCandidatelastHeartBeatTimeByBatch(candID, batchId);
		// ends here
		String responseOption =
				(candAction.getRespID() == null || candAction.getRespID().isEmpty()) ? null : candAction.getRespID();

		AcsCandidateResponses resp_existed =
				new AcsCandidateResponses(new AcsCandidateResponsesId(bcaId, candAction.getqIdentifier()),
						candAction.isMark(), responseOption, 0);
		resp_existed.setSecondaryQID(secondaryQID);
		resp_existed.setSecondaryQpID(secondaryQpID);
		resp_existed.setLanguage(language);
		session.saveOrUpdate(resp_existed);

		// Added for Audit Trail Report
		long mill = candAction.getDurationMillisecs();
		// String assessmentTypeValue = candAction.getAssessmentCode();
		MarkReviewAuditDO markReviewAuditDO = new MarkReviewAuditDO();
		// markReviewAuditDO.setAssessmentID(assessmentTypeValue);
		markReviewAuditDO.setBatchCandidateAssociation(bcaId);
		markReviewAuditDO.setCandID(candID);
		// markReviewAuditDO.setCandApplicationNumber(cand.getIdentifier1());
		// markReviewAuditDO.setBatchID(batchId);
		markReviewAuditDO.setBatchCode(candAction.getBatchCode());
		markReviewAuditDO.setHostAddress(candAction.getIp());
		markReviewAuditDO.setMarkItem(mark);
		markReviewAuditDO.setqID(qID);
		markReviewAuditDO.setQpID(qpID);
		markReviewAuditDO.setSecID(secID);
		markReviewAuditDO.setTpQID(candAction.getTpQId());// new param
		// added new params
		markReviewAuditDO.setQuestionType(candAction.getQuestionType());
		markReviewAuditDO.setParentQuestionId(candAction.getParentQuestionId());
		// ends
		markReviewAuditDO.setClientTime(TimeUtil.convertTimeAsString(candAction.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));

		if (mark)
			markReviewAuditDO.setActionType(CandidateActionsEnum.MARKFORREVIEW);
		else
			markReviewAuditDO.setActionType(CandidateActionsEnum.UNMARKFORREVIEW);
		if (candAction.getDurationMillisecs() != 0)
			markReviewAuditDO.setActionTime(TimeUtil.formatTime(mill));
		AuditTrailLogger.auditSave(markReviewAuditDO);
		statsCollector.log(startTime, "markItemForReview", "CandidateService", candID);
		return true;
	}

	@Override
	public CandidateResponsesDO[] getCandidateResponses(int batchCandidateAssociationId)
			throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("Initiated get responses with input batchCandidateAssociationId = {}",
				batchCandidateAssociationId);
		HashMap<String, Boolean> hashMap = new HashMap<String, Boolean>();
		List<CandidateResponsesDO> finalResult = new ArrayList<CandidateResponsesDO>();

		String hql_query = ACSQueryConstants.QUERY_FETCH_CAND_RESPONSES;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", batchCandidateAssociationId);

		// List<AcsCandidateResponses> responses =
		if (!isSingleQRCSupportNeed) {

			List<CandidateResponsesDO> result =
					session.getResultAsListByQuery(hql_query, params, 0, CandidateResponsesDO.class);

			if (result != null && !result.equals(Collections.<Object> emptyList())) {

				for (Iterator iterator = result.iterator(); iterator.hasNext();) {
					CandidateResponsesDO candidateResponsesDO1 = (CandidateResponsesDO) iterator.next();
					// checks for RC if so maintains its parent question id
					if (candidateResponsesDO1.getParentQuestionId() == null
							&& candidateResponsesDO1.getQuestionType().equalsIgnoreCase(
									QuestionType.READING_COMPREHENSION.toString())) {
						hashMap.put(candidateResponsesDO1.getQuestionId(), candidateResponsesDO1.isMarkedForReview());
					}
				}

				for (Iterator iterator1 = result.iterator(); iterator1.hasNext();) {
					CandidateResponsesDO candidateResponsesDO = (CandidateResponsesDO) iterator1.next();
					//
					// if (candidateResponsesDO.getParentQuestionId() == null
					// && candidateResponsesDO.getQuestionType().equalsIgnoreCase(
					// QUESTIONTYPE.READING_COMPREHENSION.toString())) {
					// continue;
					// }
					if (hashMap.get(candidateResponsesDO.getParentQuestionId()) != null) {
						candidateResponsesDO
								.setMarkedForReview(hashMap.get(candidateResponsesDO.getParentQuestionId()));
					}

					finalResult.add(candidateResponsesDO);
				}
				gen_logger.info("set of responses = {}", finalResult);
				StatsCollector.getInstance().log(startTime, "getCandidateResponses", "CandidateService",
						batchCandidateAssociationId);
				return finalResult.toArray(new CandidateResponsesDO[finalResult.size()]);
			} else {
				StatsCollector.getInstance().log(startTime, "getCandidateResponses", "CandidateService",
						batchCandidateAssociationId);
				return null;
			}
		} else {
			List<CandidateResponsesDO> result =
					session.getResultAsListByQuery(hql_query, params, 0, CandidateResponsesDO.class);
			if (result != null && !result.equals(Collections.<Object> emptyList())) {
				StatsCollector.getInstance().log(startTime, "getCandidateResponses", "CandidateService",
						batchCandidateAssociationId);
				return result.toArray(new CandidateResponsesDO[result.size()]);
			} else {
				StatsCollector.getInstance().log(startTime, "getCandidateResponses", "CandidateService",
						batchCandidateAssociationId);
				return null;
			}
		}
	}

	@Override
	public int getAttemptedQuestions(int bcaId) throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		// int batchId = bs.getBatchIdbyCandId(candID);
		String hql_query = ACSQueryConstants.QUERY_FETCH_CAND_ATTEMPTED;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BCA_ID, bcaId);

		int result = session.getResultCountByQuery(hql_query, params);
		StatsCollector.getInstance().log(startTime, "getAttemptedQuestions", "CandidateService", bcaId);
		return result;
	}

	@Override
	public CandidateDetailsDO getCandidateByID(int candID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CANDIDATE_ID, candID);

		AcsCandidate acsCandidate = (AcsCandidate) session.get(candID, AcsCandidate.class.getCanonicalName());
		String middleName = "";
		if (acsCandidate.getMifData() != null) {
			MIFForm mifForm = new Gson().fromJson(acsCandidate.getMifData(), MIFForm.class);
			if (mifForm != null && mifForm.getPersonalInfo() != null)
				middleName = mifForm.getPersonalInfo().getMiddleName();
		}
		CandidateDetailsDO candidateDetails =
				new CandidateDetailsDO(acsCandidate.getCandidateId(), acsCandidate.getFirstName(), middleName,
						acsCandidate.getLastName(), acsCandidate.getIdentifier1(), acsCandidate.getIdentifier2(),
						acsCandidate.getGender(), acsCandidate.getDob() == null ? null : acsCandidate.getDob()
								.getTime(), null);
		return candidateDetails;
	}

	@Override
	public AcsCandidateStatus updateCandidatelastHeartBeatTime(int batchCandidateAssociationId)
			throws GenericDataModelException, CandidateRejectedException {
		// int batchId = bs.getBatchIdbyCandId(candID);
		return updateCandidatelastHeartBeatTimeByBatch(batchCandidateAssociationId, Calendar.getInstance());
	}

	private AcsCandidateStatus updateCandidatelastHeartBeatTimeByBatch(AcsCandidateStatus cs, Calendar lastActionTime)
			throws GenericDataModelException {

		Calendar currentDate = Calendar.getInstance();
		// cs.setPrevHeartBeatTime(cs.getLastHeartBeatTime());
		cs.setLastHeartBeatTime(currentDate);
		cs.setPlayerStatus(HBMStatusEnum.GREEN);
		cs.setSystemStatus(HBMStatusEnum.GREEN);

		if (lastActionTime != null)
			cs.setLastCandActionTime(lastActionTime);

		session.saveOrUpdate(cs);
		return cs;
	}

	private AcsCandidateStatus updateCandidatelastHeartBeatTimeByBatch(int batchCandidateAssociationId,
			Calendar lastActionTime) throws GenericDataModelException {
		// update candidate hb time in DB
		AcsCandidateStatus cs = getCandidateStatus(batchCandidateAssociationId);
		return updateCandidatelastHeartBeatTimeByBatch(cs, lastActionTime);
	}

	@Override
	public int getCandidateIDbyHallTicket(String htID) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CANDID_BY_HALLTICK;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.HALL_TICKET_ID, htID);

		Integer result = (Integer) session.getByQuery(query, params);
		if (result == null)
			return 0;
		else
			return result;
	}

	@Override
	public int getCandidateIDbyApplicationNumber(String appNum) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CANDID_BY_APPNUM;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.APPLICATION_NUMBER, appNum);

		Integer result = (Integer) session.getByQuery(query, params);
		if (result == null)
			return 0;
		else
			return result;
	}

	@Override
	public long getRemainingTimeForCandidate(int batchCandidateAssociationId) throws GenericDataModelException,
			CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		long currentSpendTime = 0, allotedDuration = 0, reminaingTime = 0, prevLostTime = 0;
		Calendar actualTestStartedTime = null;
		getLogger().debug("getRemainingTimeForCandidate initiated with {}", batchCandidateAssociationId);

		Calendar currentDateTime = Calendar.getInstance();

		AcsBatchCandidateAssociation bca =
				(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());
		AcsCandidateStatus candidateStatus = bca.getAcscandidatestatus();
		AcsBatch batch = bs.getBatchDetailsByBatchCode(bca.getBatchCode());
		CandidateReEnableDO candidateReEnableDO = null;
		if (bca.getReenablecandidatejson() == null) {
			// Normal flow
			actualTestStartedTime = bca.getActualTestStartedTime();
			allotedDuration = bca.getAllotedDuration();

			currentSpendTime =
					Math.abs(currentDateTime.getTimeInMillis() - actualTestStartedTime.getTimeInMillis()) / 1000;
			prevLostTime = candidateStatus.getPrevLostTime();
		} else {
			// Re birth flow
			candidateReEnableDO =
					gson.fromJson(bca.getReenablecandidatejson(), CandidateReEnableDO.class);
			actualTestStartedTime = candidateReEnableDO.getActualTestStartedTime();
			allotedDuration = candidateReEnableDO.getAllotedDuration();

			if (candidateReEnableDO.getStatus().name().equals(RequestStatus.NEW.name())) {
				candidateReEnableDO.setStatus(RequestStatus.ACKNOWLEDGED);
				reminaingTime = (candidateReEnableDO.getAllotedDuration());
				String reenablecandidatejson = gson.toJson(candidateReEnableDO);
				bca.setReenablecandidatejson(reenablecandidatejson);
				session.saveOrUpdate(bca);
				getLogger().debug("getRemainingTimeForCandidate for {} is {}", batchCandidateAssociationId,
						reminaingTime);
				return reminaingTime;
			}
			currentSpendTime =
					Math.abs(currentDateTime.getTimeInMillis() - actualTestStartedTime.getTimeInMillis()) / 1000;
			prevLostTime = candidateReEnableDO.getPrevLostTime();
		}

		reminaingTime = remainingTimeExtendedMethod(bca, actualTestStartedTime, allotedDuration,
				currentDateTime, batch, currentSpendTime, prevLostTime, candidateReEnableDO);

		StatsCollector.getInstance().log(startTime, "getRemainingTimeForCandidate", "CandidateService",
				batchCandidateAssociationId);

		return reminaingTime;
	}

	private long remainingTimeExtendedMethod(AcsBatchCandidateAssociation bca,
			Calendar actualTestStartedTime, long allotedDuration,
			Calendar currentDateTime, AcsBatch batch, long currentSpendTime, long prevLostTime,
			CandidateReEnableDO candidateReEnableDO)
			throws GenericDataModelException {
		int batchCandidateAssociationId = bca.getBcaid();
		getLogger().debug(
				"ActualTestStartTime: {} currentTime: {} . Hence currentSpendTime for bcaId:{} is {} seconds",
				actualTestStartedTime.getTime(), currentDateTime.getTime(), batchCandidateAssociationId,
				currentSpendTime);
		String propValue = null;
		long crashTime = 0;
		int hbmTimeInterval = (ACSConstants.DEFAULT_HBM_TIME_INTERVAL_IN_SECS);
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.HBM_TIME_INTERVAL);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			hbmTimeInterval = Integer.parseInt(propValue);
		}

		AcsCandidateStatus candidateStatus = bca.getAcscandidatestatus();

		// we are no more using prevheartbeat time to calculate crashtime, instead using LastCrashedTime
		if (candidateStatus.getLastCrashedTime() != null) {
			crashTime =
					Math.abs(currentDateTime.getTimeInMillis() - candidateStatus.getLastCrashedTime().getTimeInMillis())
							/ 1000;
			getLogger().debug("Last Crashed time was {}. Hence the time spent in last crash is {}",
					candidateStatus.getLastCrashedTime().getTime(), crashTime);
		}

		// current spend time lesser than 5 seconds with an assumption that there is 5 sec delay in the start exam
		// first time login hence alloted duration is zero
		if (allotedDuration == 0) {
			// check whether the candidate is crashed or not if crashed before starting the exam then consider his login
			// time instead of current time
			Calendar currentTime = (Calendar) currentDateTime.clone();
			if (bca.getClearLoginCount() > 0) {
				currentTime.setTime(bca.getLoginTime().getTime());
			}

			Map<String, Object> brRules =
					brlrService.getBrMapForBatchAndAssessment(bca.getBatchCode(), bca.getAssessmentCode());
			long remTime = ((Double) brRules.get(ACSConstants.ASSESSMENT_DURATION_BR_PROPERTY)).intValue() * 60;
			getLogger().debug("allotted duration for bcaId:{} before trimming against batch End time: {}",
					batchCandidateAssociationId, remTime);
			remTime = trimDurationOnBatchEndTime(currentDateTime, remTime, batch.getMaxDeltaRpackGenerationTime());
			// this piece of code for reducing assessment duration if candidate logged in late
			long lateInSecs = 0;
			AcsEvent acsEvent = bs.getEventDetailsByBatchCode(batch.getBatchCode());

			String eventCode = acsEvent.getEventCode();
			// get prop value for REDUCE_TIME_FOR_LATE_LOGIN
			AcsProperties acsProperties = acsEventRequestsService
					.getAcsPropertiesOnPropNameAndEventCode(EventRule.REDUCE_TIME_FOR_LATE_START.toString(), eventCode);
			if (acsProperties != null && acsProperties.getPropertyValue() != null
					&& Boolean.valueOf(acsProperties.getPropertyValue())) {
				// Calendar batchStartTime = batch.getMaxBatchStartTime();
				Calendar batchStartTime = authService.getMaxCandidateBatchStartTime(
						bca.getExtendedBatchStartTimePerCandidate(), batch.getMaxBatchStartTime());
				if (currentTime.after(batchStartTime)) {
					// late time in seconds and not negation
					lateInSecs = (currentTime.getTimeInMillis() - batchStartTime.getTimeInMillis()) / 1000;
					remTime = remTime - lateInSecs;
					gen_logger.info(
							"allocated duration for property reduce time enabled= {} for batchcandidateassociation with id = {}",
							remTime, batchCandidateAssociationId);
				}
			}
			// ends here
			bca.setAllotedDuration(remTime);

			candidateStatus.setLastHeartBeatTime(currentDateTime);
			session.saveOrUpdate(bca);

			getLogger()
					.debug("getRemainingTimeForCandidate for {} is {}", batchCandidateAssociationId, remTime);
			return remTime;
		} else if (crashTime > hbmTimeInterval) {
			Calendar lastHeartBeatTime = candidateStatus.getLastHeartBeatTime();
			Calendar prevHeartBeatTime = candidateStatus.getPrevHeartBeatTime();

			gen_logger.info(
					"batchCandidateAssociationId:{} lastHeartBeatTime = {} LastCrashedTime:{} prevLostTime:{} seconds",
					batchCandidateAssociationId, lastHeartBeatTime.getTime(), candidateStatus
							.getLastCrashedTime().getTime(), prevLostTime);

			long currentLostTime = 0;

			gen_logger.info(
					"batchCandidateAssociationId:{} lastHeartBeatTime = {}  -  LastCrashedTime:{} = currentLostTime:{} seconds and "
							+ " prevHeartBeatTime = {}",
					batchCandidateAssociationId, lastHeartBeatTime.getTime(),
					candidateStatus.getLastCrashedTime().getTime(), currentLostTime,
					prevHeartBeatTime.getTime());

			if (prevHeartBeatTime.equals(lastHeartBeatTime)) {
				currentLostTime = Math.abs(
						lastHeartBeatTime.getTimeInMillis() - candidateStatus.getLastCrashedTime().getTimeInMillis())
						/ 1000;

				gen_logger.info(
						"batchCandidateAssociationId:{} prevLostTime = {} + currentLostTime = {} = prevLostTime={} seconds",
						batchCandidateAssociationId, prevLostTime, currentLostTime, prevLostTime + currentLostTime);

				if (candidateReEnableDO == null) {
					candidateStatus.setPrevLostTime(prevLostTime + currentLostTime);
					// total lost and prev lost will be same for assessment level timing
					candidateStatus.setTotalLostTime(candidateStatus.getPrevLostTime());
				} else {
					// prevLostTime = (prevLostTime + currentLostTime);
					candidateReEnableDO.setPrevLostTime(prevLostTime + currentLostTime);
					// total lost and prev lost will be same for assessment level timing
					candidateReEnableDO.setTotalLostTime(candidateReEnableDO.getPrevLostTime());
					String candidateReEnableDOJson = gson.toJson(candidateReEnableDO);
					bca.setReenablecandidatejson(candidateReEnableDOJson);
					// candidateStatus.setTotalLostTime(
					// candidateStatus.getTotalLostTime() + candidateReEnableDO.getPrevLostTime());
				}
			}

			long duration = 0;
			// added code for handling lost time
			boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
			if (isLostTimeAddedAutomatically) {
				long totalLostTime = currentLostTime + prevLostTime;
				gen_logger.debug(
						"batchCandidateAssociationId:{} allotted duration:{} currentSpendTime: {} totalLostTime:{} ",
						batchCandidateAssociationId, allotedDuration, currentSpendTime, totalLostTime);
				duration = (allotedDuration - Math.abs(currentSpendTime - totalLostTime));
			} else {
				duration = (allotedDuration - Math.abs(currentSpendTime));
			}
			duration = duration + (bca.getTotalExtendedExamDuration());
			getLogger().debug("allotted duration for bcaId:{} before trimming against batch End time: {}",
					batchCandidateAssociationId, duration);

			// ends here
			long remTime =
					trimDurationOnBatchEndTime(currentDateTime, duration, batch.getMaxDeltaRpackGenerationTime());
			gen_logger.info("remaining time = {} for batchCandidateAssociation with id = {}", remTime,
					batchCandidateAssociationId);

			candidateStatus.setLastHeartBeatTime(currentDateTime);

			session.saveOrUpdate(bca);
			getLogger()
					.debug("getRemainingTimeForCandidate for {} is {}", batchCandidateAssociationId, remTime);

			return remTime;
		} else {
			getLogger().debug("For bcaid:{} Trimming allotted duration:{} based on current spend time:{}",
					allotedDuration, currentSpendTime, batchCandidateAssociationId);
			long remTime = (allotedDuration) - currentSpendTime;
			gen_logger.info("allocated duration = {} for batchCandidateAssociation with id = {}", remTime,
					batchCandidateAssociationId);

			candidateStatus.setLastHeartBeatTime(currentDateTime);
			session.saveOrUpdate(bca);

			return remTime;
		}
	}
	
	public long getRemainingTimeForCandidateInLiveDashboard(Calendar actualTestStartedTime, Calendar lastCrashedTime,Integer prevLostTime,Calendar lastHeartBeatTime,Calendar prevHeartBeatTime, 
			Calendar maxDeltaRpackGenerationTime, Integer allotedDurationForCand, Integer totalextendedExamDuration,int hbmTimeInterval)  {
		long startTime = StatsCollector.getInstance().begin();
		long currentSpendTime = 0;
		long crashTime = 0;
		String propValue = null;

		// This is to stop the timer in live dashboard till the candidate re login.
		Calendar currentDateTime = Calendar.getInstance();
		
		if (Math.abs(currentDateTime.getTimeInMillis()-lastHeartBeatTime.getTimeInMillis())>((hbmTimeInterval+1)*1000)) {
			currentSpendTime =
					Math.abs(lastHeartBeatTime.getTimeInMillis() - actualTestStartedTime.getTimeInMillis()) / 1000;
		}else{
			currentSpendTime =
				Math.abs(currentDateTime.getTimeInMillis() - actualTestStartedTime.getTimeInMillis()) / 1000;
		}
		// July SMU drive, multiple logins happening for same candidate,
		// we are no more using prevheartbeat time to calculate crashtime, instead using LastCrashedTime
		if (lastCrashedTime != null) {
			crashTime =
					Math.abs(currentDateTime.getTimeInMillis() - lastCrashedTime.getTimeInMillis()) / 1000;
		}

		// current spend time lesser than 5 seconds with an assumption that there is 5 sec delay in the start exam
		// first time login hence alloted duration is zero
		if (crashTime > hbmTimeInterval) {
			long currentLostTime = 0;
			if (prevHeartBeatTime.equals(lastHeartBeatTime)) {
				currentLostTime = Math.abs(
						lastHeartBeatTime.getTimeInMillis() - lastCrashedTime.getTimeInMillis())
						/ 1000;
			}

			long duration = 0;
			// added code for handling lost time
			boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
			if (isLostTimeAddedAutomatically) {
				long totalLostTime = currentLostTime + prevLostTime;
				duration = (allotedDurationForCand - Math.abs(currentSpendTime - totalLostTime));
			} else {
				duration = (allotedDurationForCand - Math.abs(currentSpendTime));
			}
			duration = duration + (totalextendedExamDuration);
			// ends here
			long allotedDuration =
					trimDurationOnBatchEndTime(currentDateTime, duration, maxDeltaRpackGenerationTime);
			return (allotedDuration+3);
		} else {
			long allotedDuration = (allotedDurationForCand) - currentSpendTime;
			// Buffer time for loading the QP.
			return (allotedDuration+3);
		}
	}

	@Override
	public long trimDurationOnBatchEndTime(Calendar currentDateTime, long allotedDuration, Calendar batchEndTime) {
		long time = Math.abs(currentDateTime.getTimeInMillis() + allotedDuration * 1000);
		if (time > batchEndTime.getTimeInMillis()) {
			time =
					Math.abs((currentDateTime.getTimeInMillis() + allotedDuration * 1000)
							- batchEndTime.getTimeInMillis()) / 1000;
			allotedDuration = allotedDuration - time;
		}
		return allotedDuration;
	}

	@Override
	public Calendar getActualTestStartTimebyCandId(int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);

		Calendar actualTestStartTime =
				(Calendar) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACTUAL_TEST_START_TIME_BY_CANDID, params);
		return actualTestStartTime;
	}

	@Override
	public boolean isCandidateAllowedForLogin(AcsBatchCandidateAssociation batchCandidateAssociation,
			AcsCandidateStatus candidateStatus, AcsBatch batchDetails, Calendar actualEndTimeForLogin)
			throws GenericDataModelException, CandidateRejectedException {
		if (candidateStatus != null) {
			// Candidate already login once in-case if clear login is not true
			// reject the login..
			if (!batchCandidateAssociation.getClearLogin()) {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_RE_LOGIN_NOT_ALLOWED,
						"You are not authorized to login with out clear login");
			}
			long differenceBasedOnHeartBeatInMillis =
					Calendar.getInstance().getTimeInMillis() - candidateStatus.getLastHeartBeatTime().getTimeInMillis();
			long differenceBasedOnActionTimeInMillis =
					Calendar.getInstance().getTimeInMillis()
							- candidateStatus.getLastCandActionTime().getTimeInMillis();

			int hbmFailPings = acsPropertyUtil.getHBMFailPings();
			int heartBeatTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
			int gapOfHBMPingsInMillis = heartBeatTimeInterval * hbmFailPings * 1000;
			if (differenceBasedOnHeartBeatInMillis < gapOfHBMPingsInMillis) {
				gen_logger
						.error("Difference between Last heartbeat and current time is {} seconds which is less than {} seconds.  Hence login is not allowed for {}",
								differenceBasedOnHeartBeatInMillis / 1000, gapOfHBMPingsInMillis / 1000,
								batchCandidateAssociation.getCandidateLogin());
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_RE_LOGIN_NOT_ALLOWED,
						"You are not authorized to login with out clear login. CandidateStatus is still RED");
			}
			gen_logger
					.debug("Last action time of candidate:{} is {}. differenceBasedOnActionTimeInMillis:{} gapOfHBMPingsInMillis:{}",
							batchCandidateAssociation.getBcaid(),
							new Gson().toJson(candidateStatus.getLastCandActionTime()),
							differenceBasedOnActionTimeInMillis, gapOfHBMPingsInMillis);
			if (differenceBasedOnActionTimeInMillis < gapOfHBMPingsInMillis) {
				gen_logger
						.error("Difference between LAST ACTION and current time is {} seconds which is less than {} seconds. Hence login is not allowed for {}",
								differenceBasedOnActionTimeInMillis / 1000, gapOfHBMPingsInMillis / 1000,
								batchCandidateAssociation.getCandidateLogin());
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_RE_LOGIN_NOT_ALLOWED,
						"You are not authorized to login with out clear login. CandidateStatus is still ACTIVE");
			}
			return true;
		} else {
			Calendar currentDateTime = Calendar.getInstance();

			// check whether the batch is already over or not
			// if (batchDetails.getMaxBatchEndTime().before(currentDateTime)) {
			// throw new CandidateRejectedException(ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_BATCH_END_TIME,
			// "Candidate login not allowed after batch end time");
			// }

			// fetch the buffer time for allowing logins before batch start time
			int preTestBufferTime = ACSConstants.DEFAULT_PRE_TEST_START_ALLOWED_LOGIN_TIME;
			String propValue =
					PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
							ACSConstants.PRE_TEST_START_ALLOWED_LOGIN_TIME);
			if (propValue != null && StringUtils.isNumeric(propValue)) {
				preTestBufferTime = Integer.parseInt(propValue);
			}

			// check whether acs crashed or not if crashed give the configured the buffer time for candidates to login
			if (batchDetails.getIsAcsCrashed()) {
				actualEndTimeForLogin = Calendar.getInstance();
				actualEndTimeForLogin.setTime(batchDetails.getLastBootUpTime().getTime());
				propValue =
						PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
								ACSConstants.ENHANCED_LATE_LOGIN_TIME_ON_ACS_CRASH_IN_MINS);
				if (propValue != null && StringUtils.isNumeric(propValue)) {
					actualEndTimeForLogin.add(Calendar.MINUTE, Integer.parseInt(propValue));
				} else {
					actualEndTimeForLogin.add(Calendar.MINUTE,
							ACSConstants.DEFAULT_ENHANCED_LATE_LOGIN_TIME_ON_ACS_CRASH_IN_MINS);
				}
			}

			// check whether the actual end time for login is before batch end time or not
			if (actualEndTimeForLogin != null
					&& actualEndTimeForLogin.after(batchDetails.getMaxDeltaRpackGenerationTime())) {
				return false;
			}

			// calculate actual start time for login based on the preTestLogin buffer time
			Calendar startTime = authService.getMaxCandidateBatchStartTime(
					batchCandidateAssociation.getExtendedBatchStartTimePerCandidate(),
					batchDetails.getMaxBatchStartTime());
			Calendar actualStartTimeForLogin = (Calendar) startTime.clone();
			long time = actualStartTimeForLogin.getTimeInMillis() - (preTestBufferTime * 60 * 1000);
			actualStartTimeForLogin.setTime(new Date(time));

			// if current time is equals or after batch start time and before than the allowed time to start the exam
			// if ((currentDateTime.equals(batchDetails.getBatchStartTime()) ||
			// currentDateTime.after(batchDetails.getBatchStartTime())) &&
			// currentDateTime.before(actualEndTimeForLogin))
			if (currentDateTime.after(actualStartTimeForLogin)
					&& (actualEndTimeForLogin == null || currentDateTime.before(actualEndTimeForLogin))) {
				return true;
			}
			return false;
		}
	}

	@Override
	public boolean updatePlayerStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException {

		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setPlayerStatus(candidateStatusDO.getPlayerStatus());
		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public boolean updatePlayerStatus(AcsCandidateStatus candidateStatusTO, int batchCandidateAssociationId)
			throws GenericDataModelException {

		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setPlayerStatus(candidateStatusTO.getPlayerStatus());
		if (candidateStatusTO.getPlayerStatus().equals(HBMStatusEnum.RED)) {
			status.setLatestModifiedStatusTime(Calendar.getInstance());
		}
		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public boolean updateCandTestStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException {
		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setCurrentStatus(candidateStatusDO.getCandTestStatus());
		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public boolean updateCandTestStatus(AcsCandidateStatus candidateStatusTO, int batchCandidateAssociationId)
			throws GenericDataModelException {
		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setCurrentStatus(candidateStatusTO.getCurrentStatus());
		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public boolean updateSystemStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException {

		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setSystemStatus(candidateStatusDO.getSystemStatus());

		if (candidateStatusDO.getSystemStatus().equals(HBMStatusEnum.RED)) {
			status.setPlayerStatus(candidateStatusDO.getPlayerStatus());
			status.setLatestModifiedStatusTime(Calendar.getInstance());
		}

		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public boolean updateSystemStatus(AcsCandidateStatus candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException {

		AcsCandidateStatus status = getCandidateStatus(batchCandidateAssociationId);
		status.setSystemStatus(candidateStatusDO.getSystemStatus());

		if (candidateStatusDO.getSystemStatus().equals(HBMStatusEnum.RED)) {
			status.setPlayerStatus(candidateStatusDO.getPlayerStatus());
			status.setLatestModifiedStatusTime(Calendar.getInstance());
		}
		session.saveOrUpdate(status);
		return true;
	}

	@Override
	public String getCandHostAddress(String batchCode, int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchCode);
		params.put(ACSConstants.CAND_ID, candId);

		String hostAddress =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_HOSTADDRESS_BY_BID_CANDID, params);
		return hostAddress;
	}

	/*
	 * 1. Should validate candidate IP Address if the IP Address falls in the range configured. 2. Should validate if IP
	 * Address is used by some other candidate and in-case if it is used is the other candidate ended the test then only
	 * allow the candidate
	 */

	@Override
	public boolean validateCandidateIPAddress(String ipAddress) throws GenericDataModelException,
			CandidateRejectedException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		boolean result = false;
		List<String> list = null; // session.getListByQuery(ACSQueryConstants.QUERY_FETCH_HOSTADDRESS_RANGE, null);
		if (list.isEmpty()) {
			result = true;
		}
		for (String string : list) {
			try {
				result = NetworkUtility.isIPAddressInRange(ipAddress, string);
			} catch (UnknownHostException | InvalidIPRangeException e) {
				throw new CandidateRejectedException(ACSExceptionConstants.INVALID_IP_ADDRESS, e.getMessage());
			}
			if (result == true)
				break;
		}
		statsCollector.log(startTime, "validateCandidateIPAddress", "CandidateService", 0);
		return result;
	}

	public List<AcsBatchCandidateAssociation> getAvailableCandidateStatus(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AcsBatchCandidateAssociation> acsBatchCandidateAssociations =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_AVAILABLE_CANDIDATES, params, 0);
		if (acsBatchCandidateAssociations.equals(Collections.<Object> emptyList()))
			return null;
		return acsBatchCandidateAssociations;
	}

	public List<AcsBatchCandidateAssociation> getAvailableCandidatesForPlayerStatus(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AcsBatchCandidateAssociation> acsBatchCandidateAssociations =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_AVAILABLE_CANDIDATES_FOR_PLAYER_STATUS,
						params, 0);
		if (acsBatchCandidateAssociations.equals(Collections.<Object> emptyList()))
			return null;
		return acsBatchCandidateAssociations;
	}

	/**
	 * Gives list of {@link AcsBatchCandidateAssociation} ids(bcaid) for batch code.
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateIdDO> getAvailableCandidatesIdsForForceSubmit(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);

		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_AVAILABLE_CAND_IDS_FOR_FORCE_SUBMIT,
						params, 0, CandidateIdDO.class);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	/*
	 * Starts a job at batch start time and get ended at batch end time which continuously monitor system (Test
	 * terminals) status.
	 */
	@Override
	public boolean startSystemHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
			throws GenericDataModelException {
		if (endTime == null || startTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null) {
				startTime = batchDetails.getMaxBatchStartTime();
				endTime = batchDetails.getMaxDeltaRpackGenerationTime();
			} else {
				gen_logger.info("batchDetails = {} hence unable to start SystemHeartBeatJob", batchDetails);
				return false;
			}
		}

		if (endTime.before(Calendar.getInstance())) {
			gen_logger.info("Not initiating systemHeartBeat job because endTime is elapsed...");
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		String SYS_HB_NAME = ACSConstants.SYS_HB_NAME;
		String SYS_HB_GRP = ACSConstants.SYS_HB_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(SYS_HB_NAME + batchCode, SYS_HB_GRP);
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
			if (triggers != null && !triggers.isEmpty()) {
				for (Iterator iterator = triggers.iterator(); iterator.hasNext();) {
					Trigger trigger = (Trigger) iterator.next();
					if (trigger != null) {
						if (reschedule) {
							scheduler.unscheduleJob(TriggerKey.triggerKey(SYS_HB_NAME + batchCode, SYS_HB_GRP));
							break;
						} else {
							gen_logger.info("Already a trigger exists for playerHeartBeat for specified batchId = {} ",
									batchCode);
							return false;
						}
					}
				}
			}

			job =
					JobBuilder.newJob(SystemHeartBeat.class).withIdentity(SYS_HB_NAME + batchCode, SYS_HB_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSConstants.BATCH_START_TIME, startTime);
			job.getJobDataMap().put(ACSConstants.BATCH_END_TIME, endTime);

			Trigger trigger = null;
			// Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(SYS_HB_NAME + batchId, SYS_HB_GRP));
			// gen_logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

			// if (trigger != null) {
			// if (reschedule) {
			// scheduler.unscheduleJob(TriggerKey.triggerKey(SYS_HB_NAME + batchId, SYS_HB_GRP));
			// } else {
			// gen_logger.info("Already a trigger exists for systemHeartBeat for specified batchId = {}", batchId);
			// return false;
			// }
			// }
			if (startTime != null && startTime.after(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(SYS_HB_NAME + batchCode, SYS_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(startTime.getTime()).endAt(endTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(SYS_HB_NAME + batchCode, SYS_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().endAt(endTime.getTime()).build();
			}
			gen_logger.trace("Trigger for system Heart Beat Processing = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startSystemHeartBeatJob...", ex);
			return false;
		}
		return true;
	}

	/*
	 * Starts a job at batch start time and get ended at batch end time which continuously monitor player (Test player)
	 * status.
	 */
	@Override
	public boolean startPlayerHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
			throws GenericDataModelException {
		DeploymentModeEnum deploymentMode = acsPropertyUtil.getDeploymentMode();
		if (deploymentMode != null && DeploymentModeEnum.CENTRALIZED.toString().equalsIgnoreCase(deploymentMode.toString())) {
			gen_logger.info("Skipping the creation of player HBM job in centraized mode of deployment instance");
			return true;
		}

		if (endTime == null || startTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null) {
				startTime = batchDetails.getMaxBatchStartTime();
				endTime = batchDetails.getMaxDeltaRpackGenerationTime();
			} else {
				gen_logger.info("batchDetails = {} hence unable to start PlayerHeartBeatJob", batchDetails);
				return false;
			}
		}

		if (endTime.before(Calendar.getInstance())) {
			gen_logger.info("Not initiating playerHeartBeat job because endTime is elapsed...");
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		String PLAYER_HB_NAME = ACSConstants.PLAYER_HB_NAME;
		String PLAYER_HB_GRP = ACSConstants.PLAYER_HB_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(PLAYER_HB_NAME + batchCode, PLAYER_HB_GRP);
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
			if (triggers != null && !triggers.isEmpty()) {
				for (Iterator iterator = triggers.iterator(); iterator.hasNext();) {
					Trigger trigger = (Trigger) iterator.next();
					if (trigger != null) {
						if (reschedule) {
							scheduler.unscheduleJob(TriggerKey.triggerKey(PLAYER_HB_NAME + batchCode, PLAYER_HB_GRP));
							break;
						} else {
							gen_logger.info("Already a trigger exists for playerHeartBeat for specified batchId = {} ",
									batchCode);
							return false;
						}
					}
				}
			}

			job =
					JobBuilder.newJob(PlayerHeartBeat.class).withIdentity(PLAYER_HB_NAME + batchCode, PLAYER_HB_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSConstants.BATCH_START_TIME, startTime);
			job.getJobDataMap().put(ACSConstants.BATCH_END_TIME, endTime);

			Trigger trigger = null;
			// Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey(PLAYER_HB_NAME + batchId, PLAYER_HB_GRP));
			// gen_logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

			// if (trigger != null) {
			// if (reschedule) {
			// scheduler.unscheduleJob(TriggerKey.triggerKey(PLAYER_HB_NAME + batchId, PLAYER_HB_GRP));
			// } else {
			// gen_logger.info("Already a trigger exists for playerHeartBeat for specified batchId = {}", batchId);
			// return false;
			// }
			// }
			if (startTime != null && startTime.after(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PLAYER_HB_NAME + batchCode, PLAYER_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(startTime.getTime()).endAt(endTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PLAYER_HB_NAME + batchCode, PLAYER_HB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().endAt(endTime.getTime()).build();
			}
			gen_logger.trace("Trigger for player Heart Beat Processing = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startPlayerHeartBeatJob...", ex);
			return false;
		}
		return true;
	}

	/*
	 * Starts a job at batch end time which will force submit those candidates who are not yet ended their test.
	 */
	@Override
	public boolean startCandidateForceSubmitJob(Calendar batchEndTime, String batchCode, boolean reschedule)
			throws GenericDataModelException {
		if (batchEndTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null)
				batchEndTime = batchDetails.getMaxDeltaRpackGenerationTime();
			else {
				gen_logger.info("batchDetails = {} hence unable to start CandidateForceSubmitJob", batchDetails);
				return false;
			}
		}

		if (batchEndTime.before(Calendar.getInstance())) {
			gen_logger.info("Not initiating candidateForceSubmit job because endTime is elapsed...");
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		String CAND_FORCE_SUBMIT_NAME = ACSConstants.CAND_FORCE_SUBMIT_NAME;
		String CAND_FORCE_SUBMIT_GRP = ACSConstants.CAND_FORCE_SUBMIT_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(CandidateForceSubmit.class)
							.withIdentity(CAND_FORCE_SUBMIT_NAME + batchCode, CAND_FORCE_SUBMIT_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(CAND_FORCE_SUBMIT_NAME + batchCode,
							CAND_FORCE_SUBMIT_GRP));

			if (trigger != null) {
				if (reschedule || (trigger.getStartTime().compareTo(batchEndTime.getTime()) < 0)) {
					scheduler.unscheduleJob(TriggerKey.triggerKey(CAND_FORCE_SUBMIT_NAME + batchCode,
							CAND_FORCE_SUBMIT_GRP));
				} else {
					gen_logger.info("Already a trigger exists for CandidateForceSubmitJob for specified batchId = {}",
							batchCode);
					return false;
				}
			}

			if (batchEndTime != null && batchEndTime.after(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CAND_FORCE_SUBMIT_NAME + batchCode, CAND_FORCE_SUBMIT_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(batchEndTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CAND_FORCE_SUBMIT_NAME + batchCode, CAND_FORCE_SUBMIT_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			}

			gen_logger.trace("Trigger for candidate force submitt Processing = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startCandidateForceSubmitJob...", ex);
			return false;
		}
		return true;
	}

	@Override
	public boolean updateCandCurrentViewedQuestion(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		// int batchId = bs.getBatchIdbyCandId(candAction.getCandID());
		// int batchId = candAction.getBatchID();
		AcsCandidateStatus cso = getCandidateStatus(candAction.getBatchCandidateAssociationId());
		if (cso == null)
			return false;
		String secondaryQpID = candAction.getSecondaryQpID();
		String secondaryQID = candAction.getSecondaryQID();
		String language = candAction.getLanguage();

		// QuestionTO qTO = questionService.getQuestionByIdAndEventCode(cso.getCurrentViewedQuestion(), eventCode);
		int cId = candAction.getCandID();
		// String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(cId, batchId);
		// String assessmentTypeValue = candAction.getAssessmentCode();
		QuestionNavigationAuditDO questionNavigationAuditDO = new QuestionNavigationAuditDO();
		questionNavigationAuditDO.setBatchCandidateAssociation(candAction.getBatchCandidateAssociationId());
		// questionNavigationAuditDO.setAssessmentID(assessmentTypeValue);
		questionNavigationAuditDO.setBatchCode(candAction.getBatchCode());
		// questionNavigationAuditDO.setBatchCode(candAction.getBatchCode());
		questionNavigationAuditDO.setCandID(cId);
		// questionNavigationAuditDO.setCandApplicationNumber(candAction.getApplicationNumber());
		questionNavigationAuditDO.setHostAddress(candAction.getIp());
		questionNavigationAuditDO.setActionType(CandidateActionsEnum.NAVIGATED);
		questionNavigationAuditDO.setNextQuestID(candAction.getqID());
		questionNavigationAuditDO.setNextSecID(candAction.getSecID());
		questionNavigationAuditDO.setPrevSecID(cso.getCurrentViewedSection());
		questionNavigationAuditDO.setPrevQuestID(cso.getCurrentViewedQuestion());
		questionNavigationAuditDO.setQpID(candAction.getQpID());
		questionNavigationAuditDO.setTpPrevQuestID(cso.getCurrentViewedQuestionTP());// new param
		questionNavigationAuditDO.setTpNextQuestID(candAction.getTpQId());// new param
		questionNavigationAuditDO.setLanguage(language);
		// added new params for RC type questions
		questionNavigationAuditDO.setParentQuestionId(candAction.getParentQuestionId());
		if (cso.getCurrentViewedQuestion() != null) {
			questionNavigationAuditDO.setPrevQuestIDType(questionService.getQuestionTypeByQuestId(cso
					.getCurrentViewedQuestion()));
		} else
			questionNavigationAuditDO.setPrevQuestIDType("");

		questionNavigationAuditDO.setNextParentQuestID(candAction.getParentQuestionId());
		questionNavigationAuditDO.setNextQuestIDType(candAction.getQuestionType());
		questionNavigationAuditDO.setSecondaryQID(secondaryQID);
		questionNavigationAuditDO.setSecondaryQpID(secondaryQpID);

		// ends
		questionNavigationAuditDO.setClientTime(TimeUtil.convertTimeAsString(candAction.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));

		long mill = candAction.getDurationMillisecs();
		if (candAction.getDurationMillisecs() != 0)
			questionNavigationAuditDO.setActionTime(TimeUtil.formatTime(mill));
		AuditTrailLogger.auditSave(questionNavigationAuditDO);
		// }
		// end audit trail
		// params.put("currentViewedQuestion", candAction.getqID());
		// params.put("currentViewedSection", candAction.getSecID());

		/*
		 * CandidateStatusDO csd = candidateData.get(cId); if (csd != null) {
		 * csd.setCandTestStatus(CandidateTestStatusEnum.IN_PROGRESS); }
		 */

		cso.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
		cso.setCurrentViewedQuestion(candAction.getqID());
		cso.setCurrentViewedSection(candAction.getSecID());
		cso.setLastCandActionTime(Calendar.getInstance());
		cso.setCurrentViewedQuestionTP(candAction.getTpQId());// new param
		session.merge(cso);
		
		// update the candidate time taken for the question. The time captured is for the previous question.
		if(candAction.getTimeSpentInLastQuestion() > 0){
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("bcaid", candAction.getBatchCandidateAssociationId());
			params.put("qid", candAction.getqIdentifier());
			params.put("markedforreview", false);
			params.put("score",0);
			params.put("timetaken", candAction.getTimeSpentInLastQuestion());
			int count = session.insertByNativeSql(ACSQueryConstants.QUERY_UPDATE_CANDIDATE_TIME_TAKEN_BY_QUESTION_ID, params);
		}
		
		StatsCollector.getInstance().log(startTime, "updateCandCurrentViewedQuestion", "CandidateService", cId);
		return true;
		// if
		// (session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_CURRENT_VIEWED_QUESTION,
		// params) !=
		// 0)
		// return true;
		// else
		// return false;
	}
	
	@Override
	public CandidateLastViewedDO getCandidateCurrentQuestionViewedByBatch(Integer batchCandidateAssociationId)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("initiated getCandidateCurrentQuestionViewedByBatch where batchCandidateAssociationID={}",
				batchCandidateAssociationId);

		CandidateLastViewedDO candidateLastViewedDO = null;
		AcsBatchCandidateAssociation batchCandAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(batchCandidateAssociationId);
		AcsCandidateStatus candidateStatus = batchCandAssociation.getAcscandidatestatus();
		if (candidateStatus != null && candidateStatus.getCurrentViewedQuestion() != null
				&& !candidateStatus.getCurrentViewedQuestion().isEmpty()) {
			// QuestionTO qTO =
			// getQuestionTypeAndParentQidByQId(candidateStatus.getCurrentViewedQuestion());
			AcsQuestion qTO =
					questionService.getQuestionByIdAndAssessment(candidateStatus.getCurrentViewedQuestion(),
							batchCandAssociation.getAssessmentCode());
			if (qTO != null) {
				candidateLastViewedDO =
						new CandidateLastViewedDO(candidateStatus.getCurrentViewedQuestion(),
								candidateStatus.getCurrentViewedSection(), candidateStatus.getResourceUsageInfo());
				// TODO:Lazy loading - Done
				candidateLastViewedDO.setParentQID(questionService.getParentQuestionIdForQuestion(qTO.getQid()));
			} else {
				gen_logger.info("AcsQuestion doesn't exist with specified questId={}",
						candidateStatus.getCurrentViewedQuestion());
			}
		} else {
			gen_logger.info("AcsCandidateStatus doesn't exist with specified bcaId={} ", batchCandidateAssociationId);
		}
		StatsCollector.getInstance().log(startTime, "getCandidateCurrentQuestionViewedByBatch", "CandidateService",
				batchCandidateAssociationId);
		return candidateLastViewedDO;

	}

	@Override
	public CandidateLastViewedDO getCandidateCurrentQuestionViewed(Integer batchCandidateAssociationId)
			throws GenericDataModelException {
		// int batchId = bs.getBatchIdsbyTimeInstance();

		AcsCandidateStatus candidateStatus = getCandidateStatus(batchCandidateAssociationId);
		if (candidateStatus != null) {
			return new CandidateLastViewedDO(candidateStatus.getCurrentViewedQuestion(),
					candidateStatus.getCurrentViewedSection(), candidateStatus.getResourceUsageInfo());
		}

		return null;
	}

	@Override
	public boolean candidateClearLogin(int batchCandidateAssociationId) throws GenericDataModelException,
			CandidateRejectedException {
		AcsBatchCandidateAssociation bca =
				(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());
		bca.setClearLogin(true);
		bca.setClearLoginCount(bca.getClearLoginCount() + 1);
		session.saveOrUpdate(bca);
		return true;
	}

	@Override
	public String candidateClearLoginByApplicationNum(int batchCandidateAssociationId, String candAppNum,
			String batchCode, String userName, String ipAddress) throws GenericDataModelException,
			CandidateRejectedException {
		AcsBatchCandidateAssociation bca =
				(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());
		batchCode = bca.getBatchCode();
		if (bca.getCandidateBlockingStatus().equals(CandidateBlockingStatus.BLOCKED)) {
			throw new CandidateRejectedException(ACSExceptionConstants.CLEAR_LOGIN_NOT_ALLOWED_FOR_BLOCKED_CANDIDATE,
					"Clear login not allowed as login is blocked for candidate with loginId : "
							+ bca.getCandidateLogin());
		}

		// gen_logger.info("candidateStatus = {}", candidateStatus);

		if (bca.getActualTestEndTime() != null)
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_CLEAR_LOGIN_NOT_ALLOWED,
					"Clear login not allowed for ended candidate with loginId : " + bca.getCandidateLogin());
		int heartBeatTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
		AcsCandidateStatus candidateStatus = getCandidateStatus(batchCandidateAssociationId);
		long difference =
				Calendar.getInstance().getTimeInMillis() - candidateStatus.getLastHeartBeatTime().getTimeInMillis();
		if (difference / 1000 < heartBeatTimeInterval) {
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_CLEAR_LOGIN_NOT_ALLOWED,
					"Clear login not allowed as candidate is still active : " + bca.getCandidateLogin());
		}
		if (!bca.getClearLogin()) {
			bca.setClearLogin(true);
			bca.setClearLoginCount(bca.getClearLoginCount() + 1);
			session.saveOrUpdate(bca);
		}
		// audit clear login event under admin actions
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			// fetch batch code
			Object[] paramArray = { candAppNum, batchCode };
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.CLEAR_LOGIN.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_CLEAR_LOGIN_MSG, paramArray);

			// audit for incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(batchCode, candAppNum, bca.getHostAddress(),
							TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
									TimeUtil.DISPLAY_DATE_FORMAT), IncidentAuditLogEnum.CLEAR_LOGIN,
							bca.getClearLoginCount());

			AuditTrailLogger.incidentAudit(incidentAuditAction);
		}
		return bca.getCandidateLogin();
	}

	@Override
	public boolean candidateCancelLogin(int bcaId) throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("initiated candidateCancelLogin where bcaId={} ", bcaId);

		AcsBatchCandidateAssociation acsbatchcandidateassociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		gen_logger.info("AcsBatchCandidateAssociation = {}", acsbatchcandidateassociation);

		if (acsbatchcandidateassociation != null) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put(ACSConstants.BCA_ID, bcaId);

			// if the candidate starts the test then reset the clear login flag else delete the candidate status record
			// itself
			if (acsbatchcandidateassociation.getActualTestStartedTime() == null) {
				session.updateByQuery(ACSQueryConstants.QUERY_DELETE_CAND_CANCEL_LOGIN, params);
			} else {
				acsbatchcandidateassociation.setClearLogin(true);
				// session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_CANCEL_LOGIN, params);
			}

			// revert the login attempt count
			int numberOfValidLoginAttempts = acsbatchcandidateassociation.getNumberOfValidLoginAttempts();
			if (numberOfValidLoginAttempts <= 0) {
				acsbatchcandidateassociation.setNumberOfValidLoginAttempts(0);
			} else {
				acsbatchcandidateassociation.setNumberOfValidLoginAttempts(numberOfValidLoginAttempts - 1);
			}
			acsbatchcandidateassociation.setCandidateBlockingStatus(CandidateBlockingStatus.ALLOWED);
			batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(acsbatchcandidateassociation);
			// batchCandidateAssociationService.updateValidLoginAttemptsByBatchIdAndCandidateId(bcaId);

			// audit candidate cancellation event
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CANCEL_LOGIN);
			String candidateIdentifier =
					candidateService.getCandidateIdentifierFromCandId(acsbatchcandidateassociation.getCandidateId());
			abstractAuditActionDO.setBatchCandidateAssociation(acsbatchcandidateassociation.getBcaid());
			abstractAuditActionDO.setCandApplicationNumber(candidateIdentifier);
			abstractAuditActionDO.setBatchCode(acsbatchcandidateassociation.getBatchCode());
			abstractAuditActionDO.setHostAddress(acsbatchcandidateassociation.getHostAddress());
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
					TimeUtil.DISPLAY_DATE_FORMAT));

			AuditTrailLogger.auditSave(abstractAuditActionDO);
		}
		StatsCollector.getInstance().log(startTime, "candidateCancelLogin", "CandidateService", bcaId);
		return true;
	}

	@Override
	public String getCandidateImage(int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);

		String candImage = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDIDATE_IMAGE_BY_CANDID, params);
		return candImage;
	}

	@Override
	public List<CandidateIdDO> getCandIdsbyApplicationNumber(String appNum) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CANDID_BY_APPNUM;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.APPLICATION_NUMBER, appNum);

		List<CandidateIdDO> candidateIds = session.getResultAsListByQuery(query, params, 0, CandidateIdDO.class);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		else
			return candidateIds;
	}

	@Override
	public Integer getCandIdByAppNumAndBatchCode(String candAppNum, String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put(ACSConstants.APPLICATION_NUMBER, candAppNum);
		// params.put("candIds", candIds);
		Integer candID =
				(Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDID_BY_BATCHID_AND_APP_NUM, params);
		if (candID == null) {
			return 0;
		}
		return candID;
	}

	@Override
	public boolean isCandAllowedToStartExam(Integer candId, String batchCode) throws GenericDataModelException,
			CandidateRejectedException {
		if (candId != null) {
			// int batchId = bs.getBatchIdbyCandId(candId);
			/*
			 * AcsCandidateStatus candidateStatus = getCandidateStatus(candId, batchCode); if (candidateStatus != null
			 * && candidateStatus.getActualTestStartedTime() != null) { return true; }
			 */
			return false;
		} else {
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_UNAUTHORIZED,
					"Candidate is not authorized for current batch");
		}
	}

	/*
	 * This API is used to audit changing options into candidate audit trail.
	 */
	@Override
	public void auditCandidateRespone(AuditCandidateResponseDO auditCandidateResponse)
			throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		int candID = auditCandidateResponse.getCandID();
		String batchCode = auditCandidateResponse.getBatchCode();
		String secondaryQpID = auditCandidateResponse.getSecondaryQpID();
		String secondaryQID = auditCandidateResponse.getSecondaryQID();
		String language = auditCandidateResponse.getLanguage();
		// CandidateStatusTO cs = getCandidateStatus(candID, batchId);
		// String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(candID, batchId);
		long mill = auditCandidateResponse.getDurationMillisecs();

		ChangeOptionAuditDO changeOptionAuditDO = new ChangeOptionAuditDO();
		changeOptionAuditDO.setBatchCandidateAssociation(auditCandidateResponse.getBatchCandidateAssociationId());
		// changeOptionAuditDO.setAssessmentID(assessmentTypeValue);
		changeOptionAuditDO.setCandID(candID);
		// changeOptionAuditDO.setCandApplicationNumber(getApplicationNumberByCandidateId(candID));
		changeOptionAuditDO.setBatchCode(batchCode);
		// changeOptionAuditDO.setBatchCode(bs.getBatchCodebyBatchId(batchId));
		changeOptionAuditDO.setHostAddress(auditCandidateResponse.getIp());
		changeOptionAuditDO.setPrevOptionID(auditCandidateResponse.getPreviousOption());
		changeOptionAuditDO.setNewOptionID(auditCandidateResponse.getCurrentOption());
		changeOptionAuditDO.setqID(auditCandidateResponse.getqID());
		changeOptionAuditDO.setQpID(auditCandidateResponse.getQpID());
		changeOptionAuditDO.setSecID(auditCandidateResponse.getSecID());
		changeOptionAuditDO.setTpQID(auditCandidateResponse.getTpQId());// new param
		// added new params
		changeOptionAuditDO.setQuestionType(auditCandidateResponse.getQuestionType());
		changeOptionAuditDO.setParentQuestionId(auditCandidateResponse.getParentQuestionId());
		changeOptionAuditDO.setSecondaryQID(secondaryQID);
		changeOptionAuditDO.setSecondaryQpID(secondaryQpID);
		changeOptionAuditDO.setLanguage(language);
		// ends
		changeOptionAuditDO.setClientTime(TimeUtil.convertTimeAsString(auditCandidateResponse.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));

		if (auditCandidateResponse.getPreviousOption() != null) {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.SWITCHOPTION);
		} else if (auditCandidateResponse.getPreviousOption() == null
				|| auditCandidateResponse.getPreviousOption().trim() == "") {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.OPTIONSELECTED);
		}

		if (auditCandidateResponse.getDurationMillisecs() != 0)
			changeOptionAuditDO.setActionTime(TimeUtil.formatTime(mill));
		AuditTrailLogger.auditSave(changeOptionAuditDO);
		StatsCollector.getInstance().log(startTime, "auditCandidateRespone", "CandidateService", candID);
	}

	public void saveSectionLevelLanguages(int bcaId, String qpCandLanguages, long clientTime, String batchCode,
			String ip, String assessmentCode) throws GenericDataModelException, CandidateRejectedException {
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		gen_logger.debug(
				"--IN-- saveSectionLevelLanguages : bcaid = {}, acsBatchCandidateAssociation={}, qpCandLanguages={} ",
				bcaId, acsBatchCandidateAssociation != null, qpCandLanguages != null);
		if (acsBatchCandidateAssociation != null && qpCandLanguages!=null && !qpCandLanguages.isEmpty()) {
			List<CandidateSectionLanguagesDO> candidateSectionLanguagesDO= gson.fromJson(qpCandLanguages,new TypeToken<List<CandidateSectionLanguagesDO>>(){}.getType());
			if (candidateSectionLanguagesDO==null ||  candidateSectionLanguagesDO.isEmpty() || candidateSectionLanguagesDO.size()==0) {
				// Throw exception.
				throw new CandidateRejectedException(1,"Invalid data for candidate selected languages");
			}
			acsBatchCandidateAssociation.setCandSelectedLanguages(qpCandLanguages);

			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setBatchCandidateAssociation(bcaId);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CAND_SELECTED_SECTION_LANGUAGE); //
			abstractAuditActionDO.setAssessmentID(acsBatchCandidateAssociation.getAssessmentCode());
			abstractAuditActionDO.setCandID(acsBatchCandidateAssociation.getCandidateId()); //
			abstractAuditActionDO
					.setCandApplicationNumber(getApplicationNumberByCandidateId(acsBatchCandidateAssociation
							.getCandidateId()));
			abstractAuditActionDO.setBatchCode(acsBatchCandidateAssociation.getBatchCode());
			abstractAuditActionDO.setHostAddress(ip);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
			abstractAuditActionDO.setShuffleSequence(qpCandLanguages);
			AuditTrailLogger.auditSave(abstractAuditActionDO);
			// ends Audit trail

			batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(acsBatchCandidateAssociation);
		}else{
			// Throw exception.
			throw new CandidateRejectedException(1,"Invalid data for candidate selected languages");
		}
	}
	
	public void auditQuestionLevelLanguageSelection(int bcaId, String selectedLanguage, long clientTime,
			String ip,String questionId) throws GenericDataModelException, CandidateRejectedException {
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		if (acsBatchCandidateAssociation != null) {

			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setBatchCandidateAssociation(bcaId);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CAND_SELECTED_QUESTION_LANGUAGE); //
			abstractAuditActionDO.setAssessmentID(acsBatchCandidateAssociation.getAssessmentCode());
			abstractAuditActionDO.setCandID(acsBatchCandidateAssociation.getCandidateId()); //
			abstractAuditActionDO
					.setCandApplicationNumber(getApplicationNumberByCandidateId(acsBatchCandidateAssociation
							.getCandidateId()));
			abstractAuditActionDO.setBatchCode(acsBatchCandidateAssociation.getBatchCode());
			abstractAuditActionDO.setHostAddress(ip);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
			abstractAuditActionDO.setSelectedLanguage(selectedLanguage);
			abstractAuditActionDO.setQuestionId(questionId);
			AuditTrailLogger.auditSave(abstractAuditActionDO);
			// ends Audit trail
		}
	}
	
	@Override
	public void saveCandSuffleSequence(int candID, String shuffleSequence, long clientTime, String batchCode,
			String ip, int bcaId, String assessmentCode) throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();

		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		if (acsBatchCandidateAssociation != null) {
			acsBatchCandidateAssociation.setShuffleSequence(new Gson().toJson(shuffleSequence));

			// added for Audit trail // String assessmentCode =
			// cdeas.getAssessmentCodebyCandIDAndBatchID(candID,batchId); AbstractAuditActionDO abstractAuditActionDO =
			// new GeneralAuditDO();
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setBatchCandidateAssociation(bcaId);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.SHUFFLESEQUENCE); //
			abstractAuditActionDO.setAssessmentID(acsBatchCandidateAssociation.getAssessmentCode());
			abstractAuditActionDO.setCandID(acsBatchCandidateAssociation.getCandidateId()); //
			abstractAuditActionDO
					.setCandApplicationNumber(getApplicationNumberByCandidateId(acsBatchCandidateAssociation
							.getCandidateId()));
			abstractAuditActionDO.setBatchCode(acsBatchCandidateAssociation.getBatchCode());
			abstractAuditActionDO.setHostAddress(ip);
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
			abstractAuditActionDO.setShuffleSequence(shuffleSequence);
			AuditTrailLogger.auditSave(abstractAuditActionDO);
			// ends Audit trail

			batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(acsBatchCandidateAssociation);
			StatsCollector.getInstance().log(startTime, "saveCandSuffleSequence", "CandidateService",
					acsBatchCandidateAssociation.getCandidateId());
		}

	}

	/*
	 * public QPCandidateAssosicationTO getQpCandidateAssociation(int candID, int batchId) throws
	 * GenericDataModelException { HashMap<String, Object> params = new HashMap<String, Object>();
	 * params.put(ACSConstants.BATCH_ID, batchId); params.put(ACSConstants.CANDIDATE_ID, candID);
	 * QPCandidateAssosicationTO qpCandidateAssosication = (QPCandidateAssosicationTO) session.getByQuery(
	 * "FROM QPCandidateAssosicationTO WHERE candID=(:candID) and batchID=(:batchId)", params); return
	 * qpCandidateAssosication; }
	 */

	@Override
	public String getApplicationNumberByCandidateId(int candId) throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		String query = ACSQueryConstants.QUERY_FETCH_APPNUM_BY_CANDID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);

		String result = (String) session.getByQuery(query, params);
		if (result == null) {
			StatsCollector.getInstance()
					.log(startTime, "getApplicationNumberByCandidateId", "CandidateService", candId);
			return null;
		} else {
			StatsCollector.getInstance()
					.log(startTime, "getApplicationNumberByCandidateId", "CandidateService", candId);
			return result;
		}
	}

	/*
	 * @Override public void insertAdminAuditData(AcsAdminAudits auditTO) throws GenericDataModelException {
	 * session.persist(auditTO); }
	 */
	@Override
	public boolean startRpackGenerationInitiatorJob(String batchCode, Calendar batchStartTime, Calendar batchEndTime)
			throws GenericDataModelException {
		if (batchStartTime == null || batchEndTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null) {
				batchStartTime = batchDetails.getMaxBatchStartTime();
				batchEndTime = batchDetails.getMaxDeltaRpackGenerationTime();
			} else {
				gen_logger.info("batchDetails = {} hence unable to start RpackGenerationInitiatorJob", batchDetails);
				return false;
			}
		}

		if (batchEndTime.before(Calendar.getInstance())) {
			gen_logger.info("Not initiating rpackGenerationInitiator job because endTime is elapsed...");
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		String RESPONSE_REPORT_GENERATOR_INITIATOR_NAME = ACSConstants.RESPONSE_REPORT_GENERATOR_INITIATOR_NAME;
		String RESPONSE_REPORT_GENERATOR_INITIATOR_GRP = ACSConstants.RESPONSE_REPORT_GENERATOR_INITIATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(RpackGenerationInitiator.class)
							.withIdentity(RESPONSE_REPORT_GENERATOR_INITIATOR_NAME + batchCode,
									RESPONSE_REPORT_GENERATOR_INITIATOR_GRP).storeDurably(false).requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);
			job.getJobDataMap().put(ACSConstants.BATCH_START_TIME, batchStartTime);
			job.getJobDataMap().put(ACSConstants.BATCH_END_TIME, batchEndTime);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_INITIATOR_NAME + batchCode,
							RESPONSE_REPORT_GENERATOR_INITIATOR_GRP));

			if (trigger != null) {
				gen_logger
						.info("Already a trigger exists for RpackGenerationInitiatorJob for specified batchId = {} hence, unscheduling existing trigger and initiating new trigger",
								batchCode);
				scheduler.unscheduleJob(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_INITIATOR_NAME + batchCode,
						RESPONSE_REPORT_GENERATOR_INITIATOR_GRP));
			}
			if (batchStartTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RESPONSE_REPORT_GENERATOR_INITIATOR_NAME + batchCode,
										RESPONSE_REPORT_GENERATOR_INITIATOR_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RESPONSE_REPORT_GENERATOR_INITIATOR_NAME + batchCode,
										RESPONSE_REPORT_GENERATOR_INITIATOR_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(batchStartTime.getTime()).build();
			}
			gen_logger.trace("Trigger for initiating Rpack related jobs = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startRpackGenerationInitiatorJob...", ex);
			return false;
		}
		return true;
	}

	@Override
	public boolean startAttendanceReportGeneratorJob(String batchCode, Calendar startTime, boolean forceStart) {
		gen_logger.info("Attendence report generation for batchId = {} to happen at = {} ", batchCode,
				startTime.getTime());
		if (startTime != null) {
			JobDetail job = null;
			Scheduler scheduler = null;
			String ATTENDANCE_REPORT_GENARATOR_NAME = ACSConstants.ATTENDANCE_REPORT_GENARATOR_NAME;
			String ATTENDANCE_REPORT_GENARATOR_GRP = ACSConstants.ATTENDANCE_REPORT_GENARATOR_GRP;
			try {
				scheduler = new StdSchedulerFactory().getScheduler();
				scheduler.start();

				job =
						JobBuilder
								.newJob(AttendanceReportGenerator.class)
								.withIdentity(ATTENDANCE_REPORT_GENARATOR_NAME + batchCode,
										ATTENDANCE_REPORT_GENARATOR_GRP).storeDurably(false).requestRecovery(true)
								.build();
				job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
				job.getJobDataMap().put(ACSConstants.FORCE_START, forceStart);

				Trigger trigger =
						scheduler.getTrigger(TriggerKey.triggerKey(ATTENDANCE_REPORT_GENARATOR_NAME + batchCode,
								ATTENDANCE_REPORT_GENARATOR_GRP));
				if (trigger != null) {
					if (trigger.getStartTime().compareTo(startTime.getTime()) < 0) {
						gen_logger
								.info("Already a trigger exists for AttendanceReportGeneratorJob for specified batchId = {} hence, Unscheduling it and initiating new job",
										batchCode);
						scheduler.unscheduleJob(TriggerKey.triggerKey(ATTENDANCE_REPORT_GENARATOR_NAME + batchCode,
								ATTENDANCE_REPORT_GENARATOR_GRP));
					} else {
						gen_logger.info("already a trigger with max late login time exists");
						return false;
					}
				}

				if (startTime.before(Calendar.getInstance())) {
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(ATTENDANCE_REPORT_GENARATOR_NAME + batchCode,
											ATTENDANCE_REPORT_GENARATOR_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionFireNow()).startNow().build();
				} else {
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(ATTENDANCE_REPORT_GENARATOR_NAME + batchCode,
											ATTENDANCE_REPORT_GENARATOR_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionFireNow())
									.startAt(startTime.getTime()).build();
				}
				gen_logger.trace("Trigger for attendance report generation = {} " + trigger);
				scheduler.scheduleJob(job, trigger);
			} catch (SchedulerException ex) {
				gen_logger.error("SchedulerException while executing startAttendanceReportGeneratorJob...", ex);
				return false;
			}
			return true;
		} else {
			gen_logger.info("startTime = {} hence, unable to start AttendanceReportGeneratorJob", startTime);
		}
		return false;
	}

	@Override
	public boolean isAllCandidatesSubmittedTest(String batchCode, Calendar startTime, Calendar endTime)
			throws GenericDataModelException {
		if (startTime.after(endTime) || endTime.before(Calendar.getInstance())) {
			gen_logger
					.info("Not initiating isAllCandidatesSubmittedTest job because endTime is after startTime or end time is elapsed...");
			return false;
		}

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String IS_ALL_CANDS_SUBMITTED_TEST_NAME = ACSConstants.IS_ALL_CANDS_SUBMITTED_TEST_NAME;
		String IS_ALL_CANDS_SUBMITTED_TEST_GRP = ACSConstants.IS_ALL_CANDS_SUBMITTED_TEST_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(IsAllCandidatesSubmittedTest.class)
							.withIdentity(IS_ALL_CANDS_SUBMITTED_TEST_NAME + batchCode, IS_ALL_CANDS_SUBMITTED_TEST_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(IS_ALL_CANDS_SUBMITTED_TEST_NAME + batchCode,
							IS_ALL_CANDS_SUBMITTED_TEST_GRP));
			if (trigger != null) {
				gen_logger.info(
						"Already a trigger exists for isAllCandidatesSubmittedTest job for specified batchId = {} ",
						batchCode);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(IS_ALL_CANDS_SUBMITTED_TEST_NAME
				// + batchId,
				// IS_ALL_CANDS_SUBMITTED_TEST_GRP));
			}
			if (startTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(IS_ALL_CANDS_SUBMITTED_TEST_NAME + batchCode,
										IS_ALL_CANDS_SUBMITTED_TEST_GRP)
								.startNow()
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInMinutes(5)).endAt(endTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(IS_ALL_CANDS_SUBMITTED_TEST_NAME + batchCode,
										IS_ALL_CANDS_SUBMITTED_TEST_GRP)
								.startAt(startTime.getTime())
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInMinutes(5)).endAt(endTime.getTime()).build();
			}
			gen_logger.trace("Trigger for isAllCandidatesSubmittedTest = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing isAllCandidatesSubmittedTest ...", ex);
			return false;
		}
		return true;
	}

	@Override
	public boolean startRpackReportGenerationJobAtAET(String batchCode, Calendar assessmentEndTime)
			throws GenericDataModelException {
		if (assessmentEndTime.before(Calendar.getInstance())) {
			gen_logger.info(
					"Not initiating rpackReportGenerationJobAtAET job because assessmentEndTime:{} is elapsed...",
					assessmentEndTime);
			return false;
		}

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String RESPONSE_REPORT_GENERATOR_AT_AET_NAME = ACSConstants.RESPONSE_REPORT_GENERATOR_AT_AET_NAME;
		String RESPONSE_REPORT_GENERATOR_AT_AET_GRP = ACSConstants.RESPONSE_REPORT_GENERATOR_AT_AET_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(RPackGeneratorAtAET.class)
							.withIdentity(RESPONSE_REPORT_GENERATOR_AT_AET_NAME + batchCode,
									RESPONSE_REPORT_GENERATOR_AT_AET_GRP).storeDurably(false).requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_AT_AET_NAME + batchCode,
							RESPONSE_REPORT_GENERATOR_AT_AET_GRP));
			if (trigger != null) {
				gen_logger
						.info("Already a trigger exists for RpackReportGenerationJobAtAET job for specified batchId = {} hence unscheduling it and scheduling new job ",
								batchCode);
				scheduler.unscheduleJob(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_AT_AET_NAME + batchCode,
						RESPONSE_REPORT_GENERATOR_AT_AET_GRP));
			}
			if (assessmentEndTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RESPONSE_REPORT_GENERATOR_AT_AET_NAME + batchCode,
										RESPONSE_REPORT_GENERATOR_AT_AET_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RESPONSE_REPORT_GENERATOR_AT_AET_NAME + batchCode,
										RESPONSE_REPORT_GENERATOR_AT_AET_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(assessmentEndTime.getTime()).build();
			}
			gen_logger.trace("Trigger for RpackReportGenerationJobAtAET = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startRpackReportGenerationJobAtAET ...", ex);
			return false;
		}
		return true;
	}

	/*
	 * Starts a job for generating Rpack with additional flexibility of generating rpack for all the candidates forcibly
	 */
	@Override
	public boolean startRpackReportGenerationJobAtBET(String batchCode, boolean forceStart)
			throws GenericDataModelException {

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String RESPONSE_REPORT_GENERATOR_AT_BET_NAME = ACSConstants.RESPONSE_REPORT_GENERATOR_AT_BET_NAME;
		String RESPONSE_REPORT_GENERATOR_AT_BET_GRP = ACSConstants.RESPONSE_REPORT_GENERATOR_AT_BET_GRP;

		String FORCE_RPACK_REPORT_GENERATOR_NAME = ACSConstants.FORCE_RPACK_REPORT_GENERATOR_NAME;
		String FORCE_RPACK_REPORT_GENERATOR_GRP = ACSConstants.FORCE_RPACK_REPORT_GENERATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			if (forceStart) {
				job =
						JobBuilder
								.newJob(RPackGeneratorAtBET.class)
								.withIdentity(FORCE_RPACK_REPORT_GENERATOR_NAME + batchCode,
										FORCE_RPACK_REPORT_GENERATOR_GRP).storeDurably(false).requestRecovery(true)
								.build();
				job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);
				job.getJobDataMap().put(ACSConstants.FORCE_START, forceStart);

				trigger =
						scheduler.getTrigger(TriggerKey.triggerKey(FORCE_RPACK_REPORT_GENERATOR_NAME + batchCode,
								FORCE_RPACK_REPORT_GENERATOR_GRP));
				if (trigger != null) {
					gen_logger
							.info("Already a forcible trigger exists for RpackReportGenerationJobAtBET job for specified batchId = {} ",
									batchCode);
					return false;
					// scheduler.unscheduleJob(TriggerKey.triggerKey(FORCE_RPACK_REPORT_GENERATOR_NAME
					// +
					// batchId, FORCE_RPACK_REPORT_GENERATOR_GRP));
				} else {
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(FORCE_RPACK_REPORT_GENERATOR_NAME + batchCode,
											FORCE_RPACK_REPORT_GENERATOR_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionFireNow()).startNow().build();
				}
			} else {
				job =
						JobBuilder
								.newJob(RPackGeneratorAtBET.class)
								.withIdentity(RESPONSE_REPORT_GENERATOR_AT_BET_NAME + batchCode,
										RESPONSE_REPORT_GENERATOR_AT_BET_GRP).storeDurably(false).requestRecovery(true)
								.build();
				job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);
				job.getJobDataMap().put(ACSConstants.FORCE_START, forceStart);

				trigger =
						scheduler.getTrigger(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_AT_BET_NAME + batchCode,
								RESPONSE_REPORT_GENERATOR_AT_BET_GRP));
				if (trigger != null) {
					gen_logger
							.info("Already a trigger exists for RpackReportGenerationJobAtBET job for specified batchId = {} ",
									batchCode);
					return false;
					// scheduler.unscheduleJob(TriggerKey.triggerKey(RESPONSE_REPORT_GENERATOR_AT_BET_NAME
					// +
					// batchId, RESPONSE_REPORT_GENERATOR_AT_BET_GRP));
				} else {
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(RESPONSE_REPORT_GENERATOR_AT_BET_NAME + batchCode,
											RESPONSE_REPORT_GENERATOR_AT_BET_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionFireNow()).startNow().build();
				}
			}
			gen_logger.trace("Trigger for RpackReportGenerationJobAtBET = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startRpackReportGenerationJobAtBET ...", ex);
			return false;
		}
		return true;
	}

	/*
	 * Will update candidate RPack status to true so that it is an indication that an RPack is created for this
	 * candidate already so that when we don't repeat candidate's data in multiple RPack's
	 */
	@Override
	public boolean updateCandRpackStatus(List<Integer> bcaIDs, boolean status, String batchId)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaIDs);
		params.put(ACSConstants.STATUS, status);

		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_RPACK_STATUS, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	@Override
	public boolean isAllCandsSubmittedTestByBatch(String batchCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_STATUS_FOR_RPACK_TO_SEND_OR_NOT;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		// feedback changes
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		// ends feedback changes

		Long count = (Long) session.getByQuery(query, params);
		if (count == 0)
			return true;
		else
			return false;
	}

	/**
	 * The startRpackUploader API is used to start the job for uploading R-pack related info on JMS.
	 * 
	 */
	@Override
	public boolean startRpackUploader(String batchCode, String packIdentifier, String path)
			throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String RPACK_UPLOADER_NAME = ACSConstants.RPACK_UPLOADER_NAME;
		String RPACK_UPLOADER_GRP = ACSConstants.RPACK_UPLOADER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(RpackUploader.class)
							.withIdentity(RPACK_UPLOADER_NAME + packIdentifier, RPACK_UPLOADER_GRP).storeDurably(false)
							.requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.PATH_TO_UPLOAD, path);
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);

			trigger =
					scheduler.getTrigger(TriggerKey
							.triggerKey(RPACK_UPLOADER_NAME + packIdentifier, RPACK_UPLOADER_GRP));
			if (trigger != null) {
				gen_logger.info(
						"Already a trigger exists for RpackUploader job for specified batchId = {} and packId = {} ",
						batchCode, packIdentifier);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(RPACK_UPLOADER_NAME + packIdentifier, RPACK_UPLOADER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.startNow().build();

			gen_logger.trace("Trigger for Rpack Uploader = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startRpackUploader ...", ex);
			return false;
		}
		return true;
	}

	public AcsCandidate getCandidateByCandIdentifiers(String identifier1, String identifier2, String identifier3)
			throws GenericDataModelException {
		StringBuffer query = new StringBuffer("FROM AcsCandidate c WHERE (1=1) ");
		HashMap<String, Object> params = new HashMap<String, Object>();
		if (identifier1 == null)
			query.append(" and c.identifier1 is null ");
		else {
			query.append(" and c.identifier1=(:identifier1) ");
			params.put(ACSConstants.IDENTIFIER1, identifier1);
		}
		if (identifier2 == null)
			query.append(" and c.identifier2 is null ");
		else {
			query.append(" and c.identifier2=(:identifier2) ");
			params.put(ACSConstants.IDENTIFIER2, identifier2);
		}
		if (identifier3 == null)
			query.append(" and c.identifier3 is null ");
		else {
			query.append(" and c.identifier3=(:identifier3) ");
			params.put(ACSConstants.IDENTIFIER3, identifier3);
		}
		AcsCandidate candidateDetails = (AcsCandidate) session.getByQuery(query.toString(), params);
		return candidateDetails;
	}

	public boolean updateCandidateDetails(AcsCandidate candidateDetails) throws GenericDataModelException {
		session.saveOrUpdate(candidateDetails);
		return true;
	}

	/**
	 * The startCandidtaeLiveStatusIniatator API is used to start the job for generating the candidate live status data
	 * required for CDB.
	 * 
	 * RESTful URI for initiating this job : http://localhost:8090/mtacs/rest/acs
	 * /startCandidateLiveStatusNotifierJob(URI to start CandidtaeLiveStatus notifier job)
	 */
	public boolean startCandidtaeLiveStatusInitiator(String batchCode, Calendar batchStartTime, Calendar batchEndTime,
			boolean reschedule) throws GenericDataModelException {
		DeploymentModeEnum deploymentMode = acsPropertyUtil.getDeploymentMode();
		if (deploymentMode != null && DeploymentModeEnum.CENTRALIZED.toString().equalsIgnoreCase(deploymentMode.toString())) {
			gen_logger.info("Skipping the creation of candidate live status notification job in centraized mode of deployment instance");
			return true;
		}

		// CDB Notification job creation based on the property isCDBCandidateLiveNotificationRequired
		if (!acsPropertyUtil.isCDBCandidateLiveNotificationRequired()) {
			gen_logger.error("");
			return false;
		}

		if (batchCode == null || batchStartTime == null || batchEndTime == null) {
			AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
			if (batchDetails != null) {
				batchCode = batchDetails.getBatchCode();
				batchStartTime = batchDetails.getMaxBatchStartTime();
				batchEndTime = batchDetails.getMaxDeltaRpackGenerationTime();
			} else {
				gen_logger.info("batchDetails = {} hence unable to start CandidtaeLiveStatusIniatator", batchDetails);
				return false;
			}
		}
		if (batchStartTime.after(batchEndTime)) {
			gen_logger.info("BatchStartTime is after BatchEndTime which is not valid");
			return false;
		}

		if (batchEndTime.before(Calendar.getInstance())) {
			gen_logger.info("BatchEndTime is elapsed");
			return false;
		}

		Calendar liveStatusStartTime = (Calendar) batchStartTime.clone();
		liveStatusStartTime.add(Calendar.MINUTE, -ACSPropertyUtil.getCDBLiveStatusStartBuffer());

		gen_logger.info("Candidate live status notification will start at {}", liveStatusStartTime.getTime());

		// Reading repeat count from prop file.
		int repeatInterval = ACSConstants.DEFAULT_REPEAT_INTERVAL_FOR_CDB_CAND_LIVE_STATUS_JOB_IN_SEC;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.REPEAT_INTERVAL_FOR_CDB_CAND_LIVE_STATUS_JOB_IN_SEC);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			repeatInterval = Integer.parseInt(propValue);
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		String CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME = ACSConstants.CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME;
		String CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP = ACSConstants.CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(CandidateLiveStatusUpdater.class)
							.withIdentity(CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME + batchCode,
									CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP).storeDurably(false).requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME + batchCode,
							CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP));
			if (trigger != null) {
				if (reschedule || (trigger.getEndTime().compareTo(batchEndTime.getTime()) < 0)) {
					scheduler.unscheduleJob(TriggerKey.triggerKey(CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME + batchCode,
							CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP));
				} else {
					gen_logger
							.info("Already a trigger exists for CandidtaeLiveStatusIniatator job for specified batchId = {} ",
									batchCode);
					return false;
				}
			}
			if (liveStatusStartTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME + batchCode,
										CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP)
								.startNow()
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInSeconds(repeatInterval)).endAt(batchEndTime.getTime())
								.build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(CDB_CANDIDATE_LIVE_STATUS_INITIATOR_NAME + batchCode,
										CDB_CANDIDATE_LIVE_STATUS_INITIATOR_GRP)
								.startAt(liveStatusStartTime.getTime())
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInSeconds(repeatInterval)).endAt(batchEndTime.getTime())
								.build();
			}
			gen_logger.trace("Trigger for CandidtaeLiveStatusIniatator = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startCandidtaeLiveStatusIniatator...", ex);
			return false;
		}
		return true;
	}

	public boolean checkCandidateAttendence(String batchCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CHECK_CAND_ATTENDENCE;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		Long count = (Long) session.getByQuery(query, params);
		if (count == 0)
			return true;
		else
			return false;
	}

	/**
	 * 
	 * @param questionId
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsQuestion getQuestionTypeAndParentQidByQId(String questionId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_QIDTYPE_PARENTQID_BY_QID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("questID", questionId);

		AcsQuestion questionTO = (AcsQuestion) session.getByQuery(query, params);
		if (questionTO != null)
			return questionTO;
		else
			return null;
	}

	public boolean deleteParentQuestionResponse(CandidateActionDO candAction) throws GenericDataModelException {
		// List<String> questionIds = new ArrayList<String>();
		// questionIds.add(candAction.getqID());
		// questionIds.add(candAction.getParentQuestionId());

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CANDIDATE_ID, candAction.getCandID());
		params.put(ACSConstants.QUES_ID, candAction.getParentQuestionId());
		params.put(ACSConstants.QP_ID, candAction.getQpID());
		params.put(ACSConstants.SEC_ID, candAction.getSecID());
		params.put(ACSConstants.BATCH_CODE, candAction.getBatchCode());

		session.updateByQuery(ACSQueryConstants.QUERY_DELETE_PARENTQUESTION_RESPONSE, params);
		return true;
	}

	public boolean isChildResponsesExists(CandidateActionDO candAction, boolean isMarkedForReview)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.PARENT_QUES_ID, candAction.getParentQuestionId());
		params.put(ACSConstants.CAND_ID, candAction.getCandID());
		params.put(ACSConstants.BATCH_ID, candAction.getBatchCode());
		params.put(ACSConstants.QP_ID, candAction.getQpID());

		String query = ACSQueryConstants.QUERY_FETCH_CAND_RESPONSE_TO_CHECK_IF_IT_EXISTS;
		if (!isMarkedForReview) {
			params.put(ACSConstants.QUES_ID, candAction.getqID());
			query = ACSQueryConstants.QUERY_FETCH_CAND_RESPONSE_TO_CHECK_IF_IT_EXISTS_FOR_NOT_MARKEDFORREVIEW;
		}

		List<AcsCandidateResponses> candidateResponses = session.getResultAsListByQuery(query, params, 0);
		if (candidateResponses != null && !candidateResponses.isEmpty() && candidateResponses.size() > 0) {
			return true;
		}
		return false;
	}

	public boolean isParentMarkedForReview(CandidateActionDO candAction) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.PARENT_QUES_ID, candAction.getParentQuestionId());
		params.put(ACSConstants.CAND_ID, candAction.getCandID());
		params.put(ACSConstants.BATCH_ID, candAction.getBatchCode());
		params.put(ACSConstants.QP_ID, candAction.getQpID());

		List<AcsCandidateResponses> candidateResponses =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_RESPOVSE_FOR_PARENT_MARKEDFORREVIEW,
						params, 0);
		if (candidateResponses != null && !candidateResponses.isEmpty() && candidateResponses.size() > 0) {
			return true;
		}
		return false;
	}

	/**
	 * This API is used to get all the RC QID's by candidate in key value pairs where key is parent question id and
	 * values are list of child question ids
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public String getcandidateAllRCQids(String qpCode, String assessmentCode, String batchCode)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("Initiated to get all RC QID's for a qpCode : {}", qpCode);
		/**
		 * Map containing key as parent question identifier and values as list of child <br>
		 * question identifier.
		 */
		HashMap<String, RCMetadata> mapQids = new HashMap<String, RCMetadata>();
		List<QuestionPaperMetadata> questionPaperMetadatas = getQuestionsByQpCode(qpCode, assessmentCode);
		HashMap<Integer, String> questIdQidMapForParentQuest = new HashMap<>();
		if (questionPaperMetadatas != null) {
			/**
			 * Contains key as questId and value as QuestionPaperMetadata.
			 */
			Map<String, QuestionPaperMetadata> questionPaperMetadataMap = new HashMap<>();
			///
			// fetch BR-rules for assessmentcode and batchcode
			BRulesExportEntity bRulesExportEntity =
					customerBatchService.getBRulesExportEntity(batchCode, assessmentCode);
			gen_logger.debug("fetch BR-rules for assessmentcode and batchcode" + bRulesExportEntity);
			// Get Marks and Score BR rule
			Map<Integer, MarksAndScoreRules> marksAndScoreRulesMap =
					scoreCalculator.getMarksAndScoreRulesMap(bRulesExportEntity, qpCode);

			///
			/**
			 * map containing key as section id and value as section name.
			 */
			Map<Integer, String> sectionIdNameMap = new HashMap<>();
			for (Iterator<QuestionPaperMetadata> metadataIterator = questionPaperMetadatas.iterator(); metadataIterator
					.hasNext();) {
				QuestionPaperMetadata questionPaperMetadata = metadataIterator.next();
				MarksAndScoreRules marksAndScoreRules = scoreCalculator.getMarksAndScoreRules(marksAndScoreRulesMap,
						questionPaperMetadata.getsId());
				double negativeScore = this.getNegativeMarkForQuestion(marksAndScoreRules,
						questionPaperMetadata.getScore(), questionPaperMetadata.getNegativeScore());
				questionPaperMetadata.setNegativeScore(negativeScore);
				if (questionPaperMetadata.getQuestionType().equals(QuestionType.READING_COMPREHENSION.toString())) {
					questIdQidMapForParentQuest.put(questionPaperMetadata.getqId(), questionPaperMetadata.getQuestId());
				}
				questionPaperMetadataMap.put(questionPaperMetadata.getQuestId(), questionPaperMetadata);
				sectionIdNameMap.put(questionPaperMetadata.getsId(), questionPaperMetadata.getSectionIdent());
			}

			/**
			 * Get RC Child mapping
			 */
			List<AcsQuestion> acsQuestions =
					questionService.getQuestionsForParentQids(new ArrayList<Integer>(questIdQidMapForParentQuest
							.keySet()));

			if (acsQuestions != null) {
				for (Iterator<AcsQuestion> acsqIterator = acsQuestions.iterator(); acsqIterator.hasNext();) {
					AcsQuestion acsQuestion = acsqIterator.next();
					int parentQId = acsQuestion.getAcsquestion().getQid();
					String parentQuestId = questIdQidMapForParentQuest.get(parentQId);
					RCMetadata metadata = mapQids.get(parentQuestId);
					if (metadata == null) {
						metadata = new RCMetadata();
						metadata.setChildQuestIds(new ArrayList(
								Arrays.asList(new String[] { acsQuestion.getQuestId() })));
						metadata.setShuffleQuestion(acsQuestion.getAcsquestion().getShuffleQuestion());
						mapQids.put(parentQuestId, metadata);
					} else {
						List<String> childQuestions = metadata.getChildQuestIds();
						childQuestions.add(acsQuestion.getQuestId());
						metadata.setChildQuestIds(childQuestions);
						mapQids.put(parentQuestId, metadata);
					}
					// convert

					Type listType = new TypeToken<List<ResponseMarkBean>>() {
					}.getType();
					List<ResponseMarkBean> responseMarkBeans = null;
					if (acsQuestion.getQans() != null && !acsQuestion.getQans().isEmpty()) {
						try {
//							new ArrayList<>();
							responseMarkBeans = new Gson().fromJson(acsQuestion.getQans(), listType);
						} catch (Exception e) {
							gen_logger.error("Unable to Parse Candidate Response for FITB Question");
							String errMsg =
									"Could not process results for candidate with candidateResponseRow::";
							gen_logger.error(errMsg, e);
						}
					}
					MarksAndScoreRules marksAndScoreRules =
							scoreCalculator.getMarksAndScoreRules(marksAndScoreRulesMap,
									questionPaperMetadataMap.get(parentQuestId).getsId());
					double negativeScore = this.getNegativeMarkForQuestion(marksAndScoreRules, acsQuestion.getMarks(),
							acsQuestion.getNegativeMarks());
					questionPaperMetadataMap.put(
							acsQuestion.getQuestId(),
							new QuestionPaperMetadata(acsQuestion.getQid(), acsQuestion.getQuestId(),
									questionPaperMetadataMap.get(parentQuestId).getsId(),
									questionPaperMetadataMap.get(parentQuestId).getSectionIdent(), parentQId,
									parentQuestId, acsQuestion.getQuestionType(), negativeScore));
				}
			}
			/**
			 * Set child question details in questionPaperMetadataMap.
			 */
			AcsQuestionPaper questionPaper =
					(AcsQuestionPaper) questionService.session.get(qpCode, AcsQuestionPaper.class.getCanonicalName());
			TpQuestionPaperMetadata tpQuestionPaperMetadata =
					new TpQuestionPaperMetadata(mapQids, questionPaperMetadataMap, sectionIdNameMap);
			tpQuestionPaperMetadata.setCiqQuestionSequence(questionPaper.getCiqQuestionSequence());
			String jsonValue = new Gson().toJson(tpQuestionPaperMetadata);
			gen_logger.info("RC QID's for a question paper code : {} are :{}", new Object[] { qpCode, jsonValue });
			return jsonValue;
		}
		gen_logger.info("RC QID's for a qpCode : {} are null", qpCode);
		StatsCollector.getInstance().log(startTime, "getcandidateAllRCQids", "CandidateService", 0);
		return null;
	}

	/**
	 * Return Negative mark for the question based on type of negative mark enum
	 * 
	 * @param marksAndScoreRules
	 * @param responseMarkBean
	 * @param questionType
	 * @param qid
	 * @param qScore
	 * @param qNegativeScore
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private double getNegativeMarkForQuestion(MarksAndScoreRules marksAndScoreRules, Double qScore,
			Double qNegativeScore) {
		gen_logger.debug("--IN-- applyNegativeMarking() for question   qScore = " + qScore + " qNegativeScore = "
				+ qNegativeScore);
		double negativeScore = 0;

		if (qScore == null)
			return negativeScore;

		if (qNegativeScore == null)
			return negativeScore;

		if (marksAndScoreRules == null)
			return qNegativeScore;

		if (marksAndScoreRules != null) {
			switch (marksAndScoreRules.getNegativeMarkEnum()) {
				case PERCENTAGE:
					if (qScore != null && marksAndScoreRules.getNegativeMark() != null)
						negativeScore = ((qScore * marksAndScoreRules.getNegativeMark()) / 100);
					break;

				case CUSTOM:
					if (marksAndScoreRules.getNegativeMark() != null)
						negativeScore = (marksAndScoreRules.getNegativeMark());
					break;

				default:
					if (qNegativeScore != null)
						negativeScore = (qNegativeScore);
					break;
			}
		}
		gen_logger.debug("Return apply Negative Marking -> question for  negativeScore = " + negativeScore);

		return negativeScore;
	}
	/**
	 * This API is used to get all the RC QID's by candidate in key value pairs where key is parent question id and
	 * values are list of child question ids
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public HashMap<String, Object> getcandidateAllRCQids(int candId, int batchId, String qpCode)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("Initiated to get all RC QID's for a candidate : {} and batchId : {}", candId, batchId);
		List<RcQuestionInfo> rcQuestionInfos = new ArrayList<>();
		HashMap<String, List<CandidateAllRCQidDO>> mapQids = new HashMap<String, List<CandidateAllRCQidDO>>();
		List<CandidateAllRCQidDO> candidateAllRCQidDO = getRCChildQuestionDetailsForCandidate(qpCode);
		if (candidateAllRCQidDO == null) {
			gen_logger.info("RC QID's for a candidate : {} and batchId : {} are null", candId, batchId);
			// return null;
		} else {
			for (Iterator<CandidateAllRCQidDO> iterator = candidateAllRCQidDO.iterator(); iterator.hasNext();) {
				CandidateAllRCQidDO candidateAllRCQidDO2 = iterator.next();
				if (mapQids.containsKey(candidateAllRCQidDO2.getParentQuestionId())) {
					mapQids.get(candidateAllRCQidDO2.getParentQuestionId()).add(candidateAllRCQidDO2);
				} else {
					List<CandidateAllRCQidDO> listOfChildQids = new ArrayList<CandidateAllRCQidDO>();
					listOfChildQids.add(candidateAllRCQidDO2);
					mapQids.put(candidateAllRCQidDO2.getParentQuestionId(), listOfChildQids);
				}
			}
			List<CandidateAllRCQidDO> rcQuestionsForAPaper =
					questionService.getRCChildQuestionDetailsForCandidate(qpCode);

			for (CandidateAllRCQidDO rcQuestion : rcQuestionsForAPaper) {
				String parentQuestionId = rcQuestion.getQuestionId();
				if (mapQids.containsKey(parentQuestionId)) {
					RcQuestionInfo rcQuestionInfo =
							new RcQuestionInfo(parentQuestionId, rcQuestion.getShuffleQuestion(),
									rcQuestion.getShuffleOptions(), mapQids.get(parentQuestionId));
					rcQuestionInfos.add(rcQuestionInfo);
				}

			}
		}
		AcsQuestionPaper questionPaper =
				(AcsQuestionPaper) questionService.session.get(qpCode, AcsQuestionPaper.class.getCanonicalName());
		HashMap<String, Object> finalReturnData = new HashMap<String, Object>();
		finalReturnData.put("RCData", rcQuestionInfos);
		finalReturnData.put("CIQData", questionPaper.getCiqQuestionSequence());
		gen_logger.info("RC QID's for a candidate : {} and batchId : {} are :{}", new Object[] { candId, batchId,
				rcQuestionInfos, questionPaper.getCiqQuestionSequence() });
		StatsCollector.getInstance().log(startTime, "getcandidateAllRCQids", "CandidateService", candId);
		return finalReturnData;
	}

	/**
	 * Get question infor for question paper.
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<QuestionPaperMetadata> getQuestionsByQpCode(String qpCode, String assessmentCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qpaperCode", qpCode);
		params.put("assessmentCode", assessmentCode);
		List<QuestionPaperMetadata> paperMetadatas =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QUESTIONMETADATA, params, 0,
						QuestionPaperMetadata.class);
		if (paperMetadatas.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return paperMetadatas;
	}

	/**
	 * The startAttendancePackUploader API is used to start the job for uploading Attendance-pack related info on JMS.
	 * 
	 */
	@Override
	public boolean startAttendancePackUploader(String batchCode, String packIdentifier, String path)
			throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String ATTENDANCE_PACK_UPLOADER_NAME = ACSConstants.ATTENDANCE_PACK_UPLOADER_NAME;
		String ATTENDANCE_PACK_UPLOADER_GRP = ACSConstants.ATTENDANCE_PACK_UPLOADER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(AttendancePackUploader.class)
							.withIdentity(ATTENDANCE_PACK_UPLOADER_NAME + packIdentifier, ATTENDANCE_PACK_UPLOADER_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.PATH_TO_UPLOAD, path);
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);
			job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(ATTENDANCE_PACK_UPLOADER_NAME + packIdentifier,
							ATTENDANCE_PACK_UPLOADER_GRP));
			if (trigger != null) {
				gen_logger
						.info("Already a trigger exists for AttendancePackUploade job for specified batchId = {} and packId = {} ",
								batchCode, packIdentifier);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(ATTENDANCE_PACK_UPLOADER_NAME + packIdentifier, ATTENDANCE_PACK_UPLOADER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.startNow().build();

			gen_logger.trace("Trigger for AttendancePack Uploader = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing AttendancePackUploader ...", ex);
			return false;
		}
		return true;
	}

	/**
	 * The startEODReportUploader API is used to start the job for uploading EODReport related info on JMS.
	 * 
	 */

	public boolean startEODReportUploader(int reportId, String reportIdentifier, String path)
			throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String EOD_REPORT_UPLOADER_NAME = ACSConstants.EOD_REPORT_UPLOADER_NAME;
		String EOD_REPORT_UPLOADER_GRP = ACSConstants.EOD_REPORT_UPLOADER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(EODReportUploader.class)
							.withIdentity(EOD_REPORT_UPLOADER_NAME + reportId, EOD_REPORT_UPLOADER_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.PATH_TO_UPLOAD, path);
			job.getJobDataMap().put(ACSConstants.REPORT_ID, reportId);
			job.getJobDataMap().put(ACSConstants.REPORT_IDENTIFIER, reportIdentifier);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(EOD_REPORT_UPLOADER_NAME + reportId,
							EOD_REPORT_UPLOADER_GRP));
			if (trigger != null) {
				gen_logger.info("Already a trigger exists for EODReportUploader job for specified identifier = {} ",
						reportIdentifier);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(EOD_REPORT_UPLOADER_NAME + reportId, EOD_REPORT_UPLOADER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.startNow().build();

			gen_logger.trace("Trigger for EODReport Uploader = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing EODReportUploader ...", ex);
			return false;
		}
		return true;
	}

	/**
	 * This API is used to start the job for uploading ReportPack related info on JMS.
	 * 
	 */

	public boolean startReportPackUploader(String reportIdentifier, String path) throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String REPORT_PACK_UPLOADER_NAME = ACSConstants.REPORT_PACK_UPLOADER_NAME;
		String REPORT_PACK_UPLOADER_GROUP = ACSConstants.REPORT_PACK_UPLOADER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(ReportPackUploader.class)
							.withIdentity(REPORT_PACK_UPLOADER_NAME + reportIdentifier, REPORT_PACK_UPLOADER_GROUP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.PATH_TO_UPLOAD, path);
			job.getJobDataMap().put(ACSConstants.REPORT_IDENTIFIER, reportIdentifier);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(REPORT_PACK_UPLOADER_NAME + reportIdentifier,
							REPORT_PACK_UPLOADER_GROUP));
			if (trigger != null) {
				gen_logger.info("Already a trigger exists for EODReportUploader job for specified identifier = {} ",
						reportIdentifier);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(REPORT_PACK_UPLOADER_NAME + reportIdentifier, REPORT_PACK_UPLOADER_GROUP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.startNow().build();

			gen_logger.trace("Trigger for EODReport Uploader = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing EODReportUploader ...", ex);
			return false;
		}
		return true;
	}

	/**
	 * The initiateEODReportGenerator API is used to start the job for initiating EODReportGenerator
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	@Deprecated
	public boolean initiateEODReportGenerator(String batchCode) throws GenericDataModelException {

		AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCode);
		if (batchDetails == null) {
			gen_logger.debug("Batch details doesnot exists for batchId={}", batchCode);
			return false;
		}
		String eventCode = cdeas.getEventCodeByBatchCode(batchCode);
		Calendar currentDate = Calendar.getInstance();
		currentDate.set(Calendar.HOUR_OF_DAY, 0);
		currentDate.set(Calendar.MINUTE, 0);
		currentDate.set(Calendar.SECOND, 0);
		currentDate.set(Calendar.MILLISECOND, 0);

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		DateFormat dFormat = new SimpleDateFormat(ACSConstants.DATE_FORMAT);
		String timeStampFormat = dFormat.format(new Date());
		String EOD_REPORT_GENERATOR_NAME = ACSConstants.EOD_REPORT_GENERATOR_NAME;
		String EOD_REPORT_GENERATOR_GRP = ACSConstants.EOD_REPORT_GENERATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(ReportPackGenerator.class)
							.withIdentity(EOD_REPORT_GENERATOR_NAME + timeStampFormat, EOD_REPORT_GENERATOR_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put("eventCode", eventCode);
			job.getJobDataMap().put("batchCode", batchCode);
			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(EOD_REPORT_GENERATOR_NAME + timeStampFormat,
							EOD_REPORT_GENERATOR_GRP));
			if (trigger != null) {
				gen_logger.info("Already a trigger exists for EODReportGenerator job for specified identifier = {} ",
						timeStampFormat);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}
			if (batchDetails.getBatchDate().before(currentDate)) {
				job.getJobDataMap().put("forceStart", true);

				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(EOD_REPORT_GENERATOR_NAME + timeStampFormat, EOD_REPORT_GENERATOR_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			} else {
				job.getJobDataMap().put("forceStart", false);
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(EOD_REPORT_GENERATOR_NAME + timeStampFormat, EOD_REPORT_GENERATOR_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(batchDetails.getMaxDeltaRpackGenerationTime().getTime()).build();

			}

			gen_logger.trace("Trigger for EODReport Uploader = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing EODReportGenerator ...", ex);
			return false;
		}
		return true;
	}

	/**
	 * The initiateEODReportGenerator API is used to start the job for initiating EODReportGenerator
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public boolean initiateReportPackGenerator(String batchCode) throws GenericDataModelException {

		AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCode);
		// TODO:Lazy loading - Done
		String eventCode = cdeas.getEventCodeByBatchCode(batchCode);
		if (batchDetails == null) {
			gen_logger.debug("Batch details doesnot exists for batchId={}", batchCode);
			return false;
		}
		Calendar currentDate = Calendar.getInstance();
		currentDate.set(Calendar.HOUR_OF_DAY, 0);
		currentDate.set(Calendar.MINUTE, 0);
		currentDate.set(Calendar.SECOND, 0);
		currentDate.set(Calendar.MILLISECOND, 0);

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		DateFormat dFormat = new SimpleDateFormat(ACSConstants.DATE_FORMAT);
		String timeStampFormat = dFormat.format(new Date());
		String REPORT_PACK_GENERATOR_NAME = ACSConstants.REPORT_PACK_GENERATOR_NAME;
		String REPORT_PACK_GENERATOR_GROUP = ACSConstants.REPORT_PACK_GENERATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(ReportPackGenerator.class)
							.withIdentity(REPORT_PACK_GENERATOR_NAME + timeStampFormat, REPORT_PACK_GENERATOR_GROUP)
							.storeDurably(false).requestRecovery(true).build();

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(REPORT_PACK_GENERATOR_NAME + timeStampFormat,
							REPORT_PACK_GENERATOR_GROUP));
			if (trigger != null) {
				gen_logger.info("Already a trigger exists for ReportPackGenerator job for specified identifier = {} ",
						timeStampFormat);
				return false;
				// scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_UPLOADER_NAME
				// + batchId,
				// RPACK_UPLOADER_GRP));
			}
			if (batchDetails.getBatchDate().before(currentDate)) {
				job.getJobDataMap().put("forceStart", true);
				job.getJobDataMap().put("eventCode", eventCode);
				job.getJobDataMap().put("batchCode", batchCode);

				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(REPORT_PACK_GENERATOR_NAME + timeStampFormat, REPORT_PACK_GENERATOR_GROUP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			} else {
				job.getJobDataMap().put("forceStart", false);
				job.getJobDataMap().put("eventCode", eventCode);
				job.getJobDataMap().put("batchCode", batchCode);
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(REPORT_PACK_GENERATOR_NAME + timeStampFormat, REPORT_PACK_GENERATOR_GROUP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(batchDetails.getMaxDeltaRpackGenerationTime().getTime()).build();

			}

			gen_logger.trace("Trigger for Report pack generator = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing initiateReportPackGenerator ...", ex);
			return false;
		}
		return true;
	}

	public long getInstructionSheetTimer(String assessmentCode, String batchCode) throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		long instructionSheetTimer = 0;

		// get batch details based on the specified batchId
		AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCode);
		if (batchDetails != null) {
			// get actual instruction sheet timer from bussiness rules
			Double time =
					(Double) cdeas.getBRRulePropertybyAssessmentIdAndBatchId(assessmentCode, batchCode,
							ACSConstants.INSTRUCTION_SHEET_TIME);
			if (time != null && time.intValue() > 0) {
				instructionSheetTimer = time.intValue() * 60 * 1000;
			}
			// calculate the buffer time based on current time and batch start time, if exists add it to instruction
			// sheet timer
			long currentTime = Calendar.getInstance().getTimeInMillis();
			long batchStartTime = batchDetails.getMaxBatchStartTime().getTimeInMillis();
			long bufferTime = batchStartTime - currentTime;
			if (bufferTime > 0 && bufferTime > instructionSheetTimer) {
				instructionSheetTimer = bufferTime;
			}
		} else {
			gen_logger.info("No batch info exists for specified batchId = {}", batchCode);
		}
		StatsCollector.getInstance().log(startTime, "getInstructionSheetTimer", "CandidateService", 0);
		return instructionSheetTimer;
	}

	/**
	 * @param bcaid
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public long getInstructionSheetTimerForCandidate(int bcaid)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		long instructionSheetTimer = 0;

		// get batch details based on the specified batchId

		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.getBatchCandidateAssociationById(bcaid);
		if (acsBatchCandidateAssociation != null) {
			String batchCode = acsBatchCandidateAssociation.getBatchCode();
			AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCode);
			Calendar candidateBatchStartTime = authService.getMaxCandidateBatchStartTime(
					acsBatchCandidateAssociation.getExtendedBatchStartTimePerCandidate(),
					batchDetails.getMaxBatchStartTime());

			// calculate the buffer time based on current time and batch start time, if exists add it to instruction
			// sheet timer
			long currentTime = Calendar.getInstance().getTimeInMillis();
			long batchStartTime = candidateBatchStartTime.getTimeInMillis();
			long bufferTime = batchStartTime - currentTime;
			instructionSheetTimer = bufferTime;
		} else {
			gen_logger.info("No candidate info exists for specified bcaid = {}", bcaid);
		}
		StatsCollector.getInstance().log(startTime, "getInstructionSheetTimer", "CandidateService", 0);
		return instructionSheetTimer;
	}

	/**
	 * 
	 * @param candAppNum
	 * @param extensionDurationInMinutes
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 */
	public boolean extendCandidateExamTimeByApplicationNum(String candAppNum, long extensionDuration, String batchCode,
			String remarks, String userName, String ipAddress) throws GenericDataModelException,
			CandidateRejectedException {
		gen_logger.info("Candidate exam duration extension initiated for specified candAppNum = {} with duration ={} ",
				candAppNum, extensionDuration);
		// String batchCodeAudit = bs.getBatchCodebyBatchId(batchId);
		if (extensionDuration > 0) {

			if (batchCode == null) {
				throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
						"Not Batch exists at this time instance");
			}

			int bcaId = getCandIdByAppNumAndBatchCode(candAppNum, batchCode);
			if (bcaId != 0) {

				// not allowing to extend exam duration when test end time is
				// available
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("bcaid", bcaId);
				int candcount = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_CAND_TEST_END_TIME, params);
				if (candcount <= 0)
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_TEST_ENDED,
							"Candidate exam duration extension not allowed, since candidate already ended test.");
				// end
				// validating not yet started exam candidates
				int candcountNotStarted =
						session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_CAND_TEST_NOT_YET_STARTED, params);
				if (candcountNotStarted > 0)
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_TEST_NOT_YET_STARTED,
							"Candidate exam duration extension not allowed, since candidate test not yet started.");
				// ends here

				// check extension count

				if (getCandidateTimeExtensionCount(bcaId) >= candidateTimeExtensionCheck()) {
					gen_logger
							.debug("Candidate ={} exam duration extension not allowed since it exceeded more than mentioned attempts = {}",
									candAppNum, candidateTimeExtensionCheck());
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_EXCEEDS_ATEMPTS,
							"Candidate exam duration extension not allowed since it exceeded more than mentioned attempts");
				}

				// end here

				// check previous value communicated to TP or not
				if (allowExtendedDurationOrNot(bcaId))
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_PENDING_PREVIOUS_STATUS,
							"Candidate exam duration extension not allowed at this instance try after some time, since his previous extended duration communicated to TP and waiting for the status");

				// end here

				//
				Calendar cal = Calendar.getInstance();
				AcsBatchCandidateAssociation acsBatchCandidateAssociation =
						batchCandidateAssociationService.loadBatchCandAssociation(bcaId);

				AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(acsBatchCandidateAssociation.getBatchCode());

				if (batchDetails != null) {
					// validating batch end time

					if (batchDetails.getMaxBatchEndTime().before(Calendar.getInstance())) {

						gen_logger
								.info("extendCandidateExamTimeByApplicationNum API failed for candidate:{} of batchId={} since batch expired",
										candAppNum, batchCode);

						throw new CandidateRejectedException(ACSExceptionConstants.BATCH_EXPIRED, "Batch Expired");

					}
					// ends here

					// fetch candidate status details by candidate id and batch id, for ip address of candidate.
					AcsCandidateStatus candidateStatusTO = acsBatchCandidateAssociation.getAcscandidatestatus();
					gen_logger.info("candidateStatusTO = {} for candidate with acs batch candidate association id = {}",
							candidateStatusTO, bcaId);

					cal.setTime(cal.getTime());
					cal.add(Calendar.SECOND, (int) extensionDuration);
					// Checking whether the batch is expired now
					if (batchDetails.getMaxDeltaRpackGenerationTime().before(cal)) {
						throw new CandidateRejectedException(
								ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_BATCH_EXPIRED,
								"Candidate exam duration extension not allowed since batch endtime elapsed");

					} else {
						params.put(ACSConstants.CANDIDATE_DURATION_EXTENSION, extensionDuration);
						params.put(ACSConstants.CANDIDATE_DURATION_EXTENSION_FLAG,
								CandidateExtendedTimeStatusEnum.NOT_YET_SEND_TO_TP);
						if (session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_DURATION_EXTENSION, params) != 0) {

							// added for admin audit trail
							if (isAuditEnable != null && isAuditEnable.equals("true")) {
								// String batchCodeAudit = bs.getBatchCodebyBatchId(batchCode);
								Object[] paramArray = { extensionDuration, candAppNum, batchCode, remarks };
								HashMap<String, String> logbackParams = new HashMap<String, String>();
								logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
										AdminAuditActionEnum.CANDIDATE_TIME_EXTENSION.toString());
								logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
								logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
								AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
										logbackParams, ACSConstants.AUDIT_CANDIDATE_TIME_EXTENSION_MSG, paramArray);
							}
							// end for admin audit trail

							// incident audit trail
							// construct the incident log object for logging
							IncidentAuditActionDO incidentAuditActionDO =
									new IncidentAuditActionDO(candAppNum, TimeUtil.convertTimeAsString(Calendar
											.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
											IncidentAuditLogEnum.CANDIDATE_TIME_EXTENSION, batchDetails.getBatchCode(),
											extensionDuration, remarks);
							// set the candidate ip address
							if (acsBatchCandidateAssociation != null
									&& acsBatchCandidateAssociation.getHostAddress() != null) {
								incidentAuditActionDO.setIp(acsBatchCandidateAssociation.getHostAddress());
							}
							AuditTrailLogger.incidentAudit(incidentAuditActionDO);
							// end for incident audit trail

							gen_logger
									.info("Candidate exam duration extension successfully updated for specified candAppNum = {} with duration ={} of batch ={}",
											candAppNum, extensionDuration, batchDetails.getBatchCode());
							return true;

						} else {
							gen_logger
									.info("Candidate exam duration extension failed to update for specified candAppNum = {} with duration ={} of batch ={}",
											candAppNum, extensionDuration, batchDetails.getBatchCode());
							throw new CandidateRejectedException(ACSExceptionConstants.FAILED_TO_UPDATE,
									"Not able to update, please check with Admin");
						}
					}
				} else
					throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
							"Not Batch exists at this time instance");

				// ends here

			} else {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_NOT_FOUND,
						"No Valid Candidate with the given Application Number");
			}

		} else {
			gen_logger.info(
					"Candidate exam duration extension skipped for specified candAppNum = {} with duration ={} ",
					candAppNum, extensionDuration);

			throw new CandidateRejectedException(ACSExceptionConstants.INVALID_INPUT, "Inavlid Input");
		}
	}

	/**
	 * 
	 * @param candAppNum
	 * @param extensionDurationInMinutes
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 */
	public boolean extendCandidateExamTimeByBcaId(int bcaid, long extensionDuration, String remarks, String userName,
			String ipAddress) throws GenericDataModelException, CandidateRejectedException {
		gen_logger.info("Candidate exam duration extension initiated for specified bcaId = {} with duration ={} ",
				bcaid, extensionDuration);
		// String batchCodeAudit = bs.getBatchCodebyBatchId(batchId);
		if (extensionDuration > 0) {

			if (bcaid != 0) {

				AcsBatchCandidateAssociation acsBatchCandidateAssociation =
						batchCandidateAssociationService.loadBatchCandAssociation(bcaid);

				String candAppNum = acsBatchCandidateAssociation.getCandidateLogin();

				String batchCode = acsBatchCandidateAssociation.getBatchCode();

				// not allowing to extend exam duration when test end time is
				// available
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("bcaid", bcaid);
				int candcount = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_CAND_TEST_END_TIME, params);
				if (candcount <= 0)
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_TEST_ENDED,
							"Candidate exam duration extension not allowed, since candidate already ended test.");
				// end
				// validating not yet started exam candidates
				int candcountNotStarted =
						session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_CAND_TEST_NOT_YET_STARTED, params);
				if (candcountNotStarted > 0)
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_TEST_NOT_YET_STARTED,
							"Candidate exam duration extension not allowed, since candidate test not yet started.");
				// ends here

				// check extension count

				if (getCandidateTimeExtensionCount(bcaid) >= candidateTimeExtensionCheck()) {
					gen_logger
							.debug("Candidate with bcaId ={} exam duration extension not allowed since it exceeded more than mentioned attempts = {}",
									bcaid, candidateTimeExtensionCheck());
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_EXCEEDS_ATEMPTS,
							"Candidate exam duration extension not allowed since it exceeded more than mentioned attempts");
				}

				// end here

				// check previous value communicated to TP or not
				if (allowExtendedDurationOrNot(bcaid))
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_PENDING_PREVIOUS_STATUS,
							"Candidate exam duration extension not allowed at this instance try after some time, since his previous extended duration communicated to TP and waiting for the status");

				// end here

				//
				Calendar cal = Calendar.getInstance();

				AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(acsBatchCandidateAssociation.getBatchCode());

				if (batchDetails != null) {
					// validating batch end time

					if (batchDetails.getMaxBatchEndTime().before(Calendar.getInstance())) {

						gen_logger
								.info("extendCandidateExamTimeByApplicationNum API failed for candidate:{} of batchId={} since batch expired",
										acsBatchCandidateAssociation.getCandidateLogin(), batchCode);

						throw new CandidateRejectedException(ACSExceptionConstants.BATCH_EXPIRED, "Batch Expired");

					}
					// ends here
					// fetch candidate status details by candidate id and batch id, for ip address of candidate.
					AcsCandidateStatus candidateStatusTO = acsBatchCandidateAssociation.getAcscandidatestatus();
					gen_logger.info("candidateStatusTO = {} for candidate with acs batch candidate association id = {}",
							candidateStatusTO, bcaid);

					cal.setTime(cal.getTime());
					cal.add(Calendar.SECOND, (int) extensionDuration);
					// Checking whether the batch is expired now
					if (batchDetails.getMaxDeltaRpackGenerationTime().before(cal)) {
						throw new CandidateRejectedException(
								ACSExceptionConstants.CAND_EXAM_DURATION_EXTENSION_NOT_ALLOWED_BATCH_EXPIRED,
								"Candidate exam duration extension not allowed since batch endtime elapsed");

					} else {
						params.put(ACSConstants.CANDIDATE_DURATION_EXTENSION, extensionDuration);
						params.put(ACSConstants.CANDIDATE_DURATION_EXTENSION_FLAG,
								CandidateExtendedTimeStatusEnum.NOT_YET_SEND_TO_TP);
						if (session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_DURATION_EXTENSION, params) != 0) {

							// added for admin audit trail
							if (isAuditEnable != null && isAuditEnable.equals("true")) {
								// String batchCodeAudit = bs.getBatchCodebyBatchId(batchCode);
								Object[] paramArray = { extensionDuration, candAppNum, batchCode, remarks };
								HashMap<String, String> logbackParams = new HashMap<String, String>();
								logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
										AdminAuditActionEnum.CANDIDATE_TIME_EXTENSION.toString());
								logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
								logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
								AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
										logbackParams, ACSConstants.AUDIT_CANDIDATE_TIME_EXTENSION_MSG, paramArray);
							}
							// end for admin audit trail

							// incident audit trail
							// construct the incident log object for logging
							IncidentAuditActionDO incidentAuditActionDO =
									new IncidentAuditActionDO(candAppNum, TimeUtil.convertTimeAsString(Calendar
											.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
											IncidentAuditLogEnum.CANDIDATE_TIME_EXTENSION, batchDetails.getBatchCode(),
											extensionDuration, remarks);
							// set the candidate ip address
							if (acsBatchCandidateAssociation != null
									&& acsBatchCandidateAssociation.getHostAddress() != null) {
								incidentAuditActionDO.setIp(acsBatchCandidateAssociation.getHostAddress());
							}
							AuditTrailLogger.incidentAudit(incidentAuditActionDO);
							// end for incident audit trail

							gen_logger
									.info("Candidate exam duration extension successfully updated for specified candAppNum = {} with duration ={} of batch ={}",
											candAppNum, extensionDuration, batchDetails.getBatchCode());
							return true;

						} else {
							gen_logger
									.info("Candidate exam duration extension failed to update for specified candAppNum = {} with duration ={} of batch ={}",
											candAppNum, extensionDuration, batchDetails.getBatchCode());
							throw new CandidateRejectedException(ACSExceptionConstants.FAILED_TO_UPDATE,
									"Not able to update, please check with Admin");
						}
					}
				} else
					throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
							"Not Batch exists at this time instance");

				// ends here

			} else {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_NOT_FOUND,
						"No Valid Candidate with the given Application Number");
			}

		} else {
			gen_logger.info("Candidate exam duration extension skipped for specified bcaId = {} with duration ={} ",
					bcaid, extensionDuration);

			throw new CandidateRejectedException(ACSExceptionConstants.INVALID_INPUT, "Inavlid Input");
		}
	}

	/**
	 * 
	 * @param candidateActionDO
	 * @throws CandidateRejectedException
	 * @throws GenericDataModelException
	 */
	public String getCandidateSuggestedRemainingTime(int[] batchCandidateAssociationIds)
			throws CandidateRejectedException, GenericDataModelException {
		gen_logger.info("getCandidateRemainingTime details initiated for batchCandidateAssociationIds:{} ",
				batchCandidateAssociationIds);

		// AcsBatch batchdetails = bs.getBatchDetailsByBatchId(batchId);
		// if (batchdetails != null) {

		long suggestedTime;
		CandidateTimeExtensionDetailsDO candidateTimeExtensionDetailsDO = new CandidateTimeExtensionDetailsDO();
		boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
		List<CandidateExtensionInfoDO> candidateSuggestedExtensionDOs = new ArrayList<>();
		for (int batchCandidateAssociationId : batchCandidateAssociationIds) {
			CandidateExtensionInfoDO candidateInfoDO = new CandidateExtensionInfoDO();
			AcsBatchCandidateAssociation bca =
					(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
							AcsBatchCandidateAssociation.class.getCanonicalName());
			// int candId = getCandIdByAppNumAndBatchId(candAppNumber, batchId);
			if (bca == null) {
				gen_logger.info("No Valid Candidate with the given batchCandidateAssociationId = {} ",
						batchCandidateAssociationId);
				continue;
			}
			AcsCandidateStatus candidateStatusTO = getCandidateStatus(batchCandidateAssociationId);

			if (candidateStatusTO == null) {
				gen_logger.info("No Valid Candidate with the given batchCandidateAssociationId = {}",
						batchCandidateAssociationId);
				continue;
			}

			if (isLostTimeAddedAutomatically) {
				suggestedTime = 0;

			} else {
				suggestedTime = getSuggestedTime(candidateStatusTO, bca);
			}

			// set candidateInfoDO
			candidateInfoDO.setSuggestedTime(suggestedTime);
			candidateInfoDO.setBatchCandidateAssociationId(batchCandidateAssociationId);
			candidateInfoDO.setCandidateId(bca.getCandidateId());
			candidateInfoDO.setCandidateLoginId(bca.getCandidateLogin());

			Calendar cal = Calendar.getInstance();
			AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(bca.getBatchCode());
			long remainingBatchTime =
					batchDetails.getMaxDeltaRpackGenerationTime().getTimeInMillis() - cal.getTimeInMillis();
			long remainingBatchTimeInSec = TimeUnit.MILLISECONDS.toSeconds(remainingBatchTime);
			candidateTimeExtensionDetailsDO.setBatchCode(batchDetails.getBatchCode());
			// candidateTimeExtensionDetailsDO.setSuggestedTime(remainingTimeDetails);

			if (batchDetails.getMaxDeltaRpackGenerationTime().before(Calendar.getInstance())) {

				gen_logger.info(
						"batchRemainingTime details set zero for batchCandidateAssociationId = {} since batch expired",
						batchCandidateAssociationId);
				candidateTimeExtensionDetailsDO.setRemainingBatchTime(0);

			}

			else {
				candidateTimeExtensionDetailsDO.setRemainingBatchTime(remainingBatchTimeInSec);
			}
			candidateSuggestedExtensionDOs.add(candidateInfoDO);
		}
		candidateTimeExtensionDetailsDO.setCandidateSuggestedExtensionDOs(candidateSuggestedExtensionDOs);
		return new Gson().toJson(candidateTimeExtensionDetailsDO);
		// }
		// else {
		// throw new
		// CandidateRejectedException(ACSExceptionConstants.CAND_NOT_FOUND,
		// "No Valid Candidate with the given Application Number");
		// }

	}

	/**
	 * getAllCandIdByAppNumAndBatchId API used to get the all candidates Id's by candAppNum
	 */
	public List<Integer> getAllCandIdByAppNumAndBatchId(String[] candAppNums, int batchId)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchId);
		params.put(ACSConstants.APPLICATION_NUMBER, candAppNums);
		// params.put("candIds", candIds);
		List<Integer> candIDs =
				(List<Integer>) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CANDID_BY_BATCHID_AND_APP_NUM,
						params);
		if (candIDs == null) {
			return null;
		}
		return candIDs;
	}

	/**
	 * isLostTimeAddedAutomatically API used to check weather ACS added lost time or not
	 * 
	 * @return
	 */
	public boolean isLostTimeAddedAutomatically() {

		boolean isCandLostTimeAddByACS = ACSConstants.DEFAULT_ADD_CAND_LOST_TIME_BY_ACS;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.IS_CAND_LOST_TIME_ADD_BY_ACS);
		if (propValue != null) {
			if (propValue.equalsIgnoreCase(Boolean.FALSE.toString())) {
				isCandLostTimeAddByACS = false;
			}
		}
		return isCandLostTimeAddByACS;

	}

	/**
	 * candidateTimeExtensionCheck API used to check the count to perform candidate time extension.
	 * 
	 * @return
	 */
	public int candidateTimeExtensionCheck() {

		int countCandidateTimeExtension = ACSConstants.DEFAULT_CAND_TIME_EXTENSION_COUNT;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.CAND_TIME_EXTENSION_COUNT);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			countCandidateTimeExtension = Integer.parseInt(propValue);
		}
		return countCandidateTimeExtension;

	}

	/**
	 * getCandidateApplicationNumberByCandId API used to get the candidate Application no by candidate Id.
	 * 
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getCandidateApplicationNumberByCandId(int candId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CAND_APPNUM_BY_CANDID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candidateId", candId);

		String result = (String) session.getByQuery(query, params);
		if (result == null)
			return null;
		else
			return result;
	}

	/**
	 * getCandidateTimeExtensionCount API used to get the candidate Time extension count from Db.
	 * 
	 * @param batchId
	 * @param candID
	 * @return
	 * @throws GenericDataModelException
	 */
	public int getCandidateTimeExtensionCount(int bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaId);

		Integer candidateExtendedTimeCount =
				(Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EXTENDED_EXAM_DURATION_COUNT, params);
		return candidateExtendedTimeCount;

	}

	/**
	 * getSuggestedTime API used to get the suggested time to extend the duration of a candidate.
	 * 
	 * @param candidateStatusTO
	 * @return
	 */
	public long getSuggestedTime(AcsCandidateStatus candidateStatusTO, AcsBatchCandidateAssociation bca) {

		/*
		 * long suggestedTime; long lostTime = candidateStatusTO.getPrevLostTime() +
		 * candidateStatusTO.getLostExtendedExamDuration(); long currentExtendedTime =
		 * candidateStatusTO.getExtendedExamDuration(); long totalExtendedTime =
		 * candidateStatusTO.getTotalExtendedExamDuration();
		 * 
		 * if (lostTime <= 0) { suggestedTime = 0;
		 * 
		 * } else if ((lostTime - ((totalExtendedTime + currentExtendedTime))) >= 0) { suggestedTime = lostTime -
		 * ((totalExtendedTime + currentExtendedTime));
		 * 
		 * } else { suggestedTime = 0; }
		 * 
		 * return suggestedTime;
		 */

		long suggestedTime;
		long totalTPCrashlostTime = candidateStatusTO.getPrevLostTime();
		long totalExtendedTime = bca.getTotalExtendedExamDuration();
		if (totalTPCrashlostTime <= 0) {
			suggestedTime = 0;
		} else if ((totalTPCrashlostTime - totalExtendedTime) > 0) {

			suggestedTime = totalTPCrashlostTime - totalExtendedTime;
		} else {
			suggestedTime = 0;
		}
		return suggestedTime;

	}

	/**
	 * updateCandlastHeartBeatandgetExtendedTime API used to update the heart beat status of candidate.
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public String updateCandlastHeartBeatAndgetExtendedTime(Integer batchCandidateAssociationId, long tpRemainingTime,
			String ip, boolean isPanelHBM, String networkSpeed) throws GenericDataModelException {

		long startTime = StatsCollector.getInstance().begin();
		gen_logger
				.debug("initiated API for updating candidate Heart beat and get the extended duration if any for a batchcandidateAssociation = {}",
						batchCandidateAssociationId);
		CandidateHeartBeatResponseDO beatResponseDO = new CandidateHeartBeatResponseDO();

		// update candidate hb time in DB

		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());

		AcsCandidateStatus candidateStatusTO = batchCandidateAssociation.getAcscandidatestatus();

		if (candidateStatusTO == null) {
			gen_logger.debug("Candidate Start Time Recieved as null from DB hence we are not supposed to process "
					+ "candidate heartbeat for batchcandidateAssociation = {}", batchCandidateAssociationId);
			return null;
		}

		if (networkSpeed != null) {
			List<NetworkSpeedDetailsDO> networkSpeedDetailsDOs = null;

			NetworkSpeedDetailsDO networkSpeedDetails = new NetworkSpeedDetailsDO();
			networkSpeedDetails.setNetworkSpeed(networkSpeed);
			networkSpeedDetails.setDateTime(Calendar.getInstance().getTimeInMillis());

			if (candidateStatusTO.getNetworkSpeed() != null) {
				String speed = candidateStatusTO.getNetworkSpeed();

				// Convert the object to a JSON string
				Type type = new TypeToken<ArrayList<NetworkSpeedDetailsDO>>() {
				}.getType();
				networkSpeedDetailsDOs = gson.fromJson(speed, type);
				networkSpeedDetailsDOs.add(networkSpeedDetails);
			} else {
				networkSpeedDetailsDOs = new ArrayList<NetworkSpeedDetailsDO>();
				networkSpeedDetailsDOs.add(networkSpeedDetails);
			}
			String json = gson.toJson(networkSpeedDetailsDOs);
			candidateStatusTO.setNetworkSpeed(json);
		}

		HashMap<String, Object>params=new HashMap<>();
		params.put("bcaid", batchCandidateAssociationId);
		params.put("eventStartTime", candidateStatusTO.getLastHeartBeatTime());
		// commented as we are not recovering the candidate if he/she crashes in feedback
		// update the candidate feedback alloted duration based on heart beat(reduce the alloted feedback duration by
		// candidate last active duration).
		// candidateStatusTO = updateCandFeedbackAllotedDuarionOnHeartBeat(candidateStatusTO, candId, batchId);
		if(isPanelHBM)
			candidateStatusTO.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);
		candidateStatusTO = updateCandidatelastHeartBeatTimeByBatch(candidateStatusTO, null);
		// batchCandidateAssociation.setAcscandidatestatus(candidateStatusTO);
		
		params.put("eventEndTime", candidateStatusTO.getLastHeartBeatTime());
		List<RPEventCommunicationDO> rpseventCommunication =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_REMOTE_EVENT_DETAILS, params, 0, RPEventCommunicationDO.class);
		
		beatResponseDO.setRpEventCommunicationDO(rpseventCommunication);
		beatResponseDO.setHeartBeatResponse(true);
		gen_logger
				.debug("successfully updated API for updating candidate Heart beat and get the extended duration if any for batchCandidateAssociation ={}",
						batchCandidateAssociationId);
		
		if (batchCandidateAssociation.getActualTestStartedTime() != null
				&& batchCandidateAssociation.getActualTestEndTime() == null
				&& batchCandidateAssociation.getExtendedTimeStatus().equals(
						CandidateExtendedTimeStatusEnum.NOT_YET_SEND_TO_TP) && isPanelHBM) {
			gen_logger.debug("Time extended for batchCandidateAssociation {} by {}",
					batchCandidateAssociation.getBcaid(), batchCandidateAssociation.getExtendedExamDuration());

			// calculated extended duration
			// calculatedCandidateStatus.setLostExtendedExamDuration(candidateExtensionTimeLeftSeconds);
			// calculatedCandidateStatus.setExtendedExamDuration(currentExtendedDuration);
			candidateStatusTO = allowedDurationForTP(batchCandidateAssociation, candidateStatusTO, tpRemainingTime, ip);
			// if (calculatedStatus != null) {
			if (batchCandidateAssociation.getExtendedExamDuration() > 0) {
				beatResponseDO.setExtendedDuration(batchCandidateAssociation.getExtendedExamDuration());
				batchCandidateAssociation
						.setExtendedTimeStatus(CandidateExtendedTimeStatusEnum.TP_AUTHENTICATION_PENDING);
			} else {
				beatResponseDO.setExtendedDuration(0);
				batchCandidateAssociation.setExtendedTimeStatus(CandidateExtendedTimeStatusEnum.NOT_ALLOWED);
			}
			// } else {
			// StatsCollector.getInstance().log(startTime, "updateCandlastHeartBeatAndgetExtendedTime",
			// "CandidateService", candId);
			// return null;
			// }
			// TODO: Change the below saveOrUpdate to update query
			session.saveOrUpdate(batchCandidateAssociation);
			gen_logger.debug(
					"Waiting for TP response in case of extended time status for a batchCandidateAssociationId = {}",
					batchCandidateAssociationId);
			// ends here

		} else {
			beatResponseDO.setExtendedDuration(0);
		}
		
		return new Gson().toJson(beatResponseDO);
	}

	/**
	 * updateExtendedTimeStatusFromTP API used to update the status that TP received extended duration
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean updateExtendedTimeStatusFromTP(CandidateActionDO candAction) throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		int candID = candAction.getCandID();

		gen_logger.debug("initiated updateExtendedTimeStatusFromTP for bcaId ={} ",
				candAction.getBatchCandidateAssociationId());

		// added for Audit trail
		// String assessmentCode = cdeas.getAssessmentCodebyCandIDAndBatchID(candID, batchId);
		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		abstractAuditActionDO.setBatchCandidateAssociation(candAction.getBatchCandidateAssociationId());
		abstractAuditActionDO.setActionType(CandidateActionsEnum.EXTENDED_EXAM_DURATION);
		abstractAuditActionDO.setCandID(candID);
		abstractAuditActionDO.setBatchCode(candAction.getBatchCode());
		abstractAuditActionDO.setHostAddress(candAction.getIp());
		abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(candAction.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));
		abstractAuditActionDO.setExtendedDuration(candAction.getExtendedDuration());
		if (candAction.getDurationMillisecs() != 0)
			abstractAuditActionDO.setActionTime(TimeUtil.formatTime(candAction.getDurationMillisecs()));
		AuditTrailLogger.auditSave(abstractAuditActionDO);
		// ends Audit trail



		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", candAction.getBatchCandidateAssociationId());
		params.put(ACSConstants.CANDIDATE_ACTUAL_DURATION_EXTENSION, candAction.getExtendedDuration());
		params.put(ACSConstants.CANDIDATE_DURATION_EXTENSION_FLAG, CandidateExtendedTimeStatusEnum.TP_AUTHENTICATED);

		// fetch current viewed section and section timing json
		Map<String, Long> map = null;
		CandidateSectionTimingDetailsDOWrapper candidateSectionTimingDetailsDOWrapper = batchCandidateAssociationService
				.getCandidateSectionExtensionByBcaId(candAction.getBatchCandidateAssociationId());
		map = candidateSectionTimingDetailsDOWrapper.getCandidateSectionTimingDetailsDOMap();
		String currentViewedSection = candidateSectionTimingDetailsDOWrapper.getCurrentViewedSection();
		if (map == null)
			map = new HashMap<String, Long>();

		if (!map.containsKey(currentViewedSection))
			map.put(currentViewedSection, candAction.getExtendedDuration());
		else
			map.put(currentViewedSection, map.get(currentViewedSection) + candAction.getExtendedDuration());

		String candidateSectionTimingDetailsJson = gson.toJson(map);
		params.put("extendedExamDurationForSection", candidateSectionTimingDetailsJson);
		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_TIME_EXTENSION_STATUS_FROM_TP, params);
		if (count == 0) {
			gen_logger.debug("failed to update updateExtendedTimeStatusFromTP API for bcaId ={}",
					candAction.getBatchCandidateAssociationId());
			return false;
		}
		gen_logger.debug("successfully updated updateExtendedTimeStatusFromTP API for bcaId ={} ",
				candAction.getBatchCandidateAssociationId());
		StatsCollector.getInstance().log(startTime, "updateExtendedTimeStatusFromTP", "CandidateService",
				candAction.getBatchCandidateAssociationId());
		return true;

	}

	/**
	 * allowExtendedDurationOrNot API used to check Extended duration not to be done during previous extendedTime status
	 * pending from TP of that candidate
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	private boolean allowExtendedDurationOrNot(int bcaId) throws GenericDataModelException {
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		if (acsBatchCandidateAssociation.getExtendedTimeStatus().equals(
				CandidateExtendedTimeStatusEnum.TP_AUTHENTICATION_PENDING)) {
			return true;
		}
		return false;

	}

	/**
	 * allowedDurationForTP API used to get actual duration left for a candidate on adding extended duration.
	 * 
	 * @param extendeDuration
	 * @param TPRemainingTime
	 * @param batchID
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsCandidateStatus allowedDurationForTP(AcsBatchCandidateAssociation batchCandidateAssociation,
			AcsCandidateStatus candidateStatus, long TPRemainingTime, /* int batchID, int candID, */String ip)
			throws GenericDataModelException {
		long extendeDuration = batchCandidateAssociation.getExtendedExamDuration();
		gen_logger.debug("TPRemainingTime---{}", TimeUtil.formatTime(TPRemainingTime));
		gen_logger.debug(
				"Actual extendeDuration value={} and TPRemainingTime value = {} for batchCandidateAssociationId = {}",
				extendeDuration, TPRemainingTime, batchCandidateAssociation.getBcaid());

		long TPRemainingseconds = TimeUnit.MILLISECONDS.toSeconds(TPRemainingTime);
		long totalDuration = extendeDuration + TPRemainingseconds;
		gen_logger.debug("Total Duration value(extendeDuration+TPRemainingTime) in seconds={}", totalDuration);

		// commented as we are using only the delta rpack gen time
		// BatchDetailsTO batchDetails = (BatchDetailsTO) session.get(batchID, BatchDetailsTO.class.getCanonicalName());
		long maxDeltaRpackGenTime = bs.getMaxDeltaRpackGenerationTime(batchCandidateAssociation.getBatchCode());

		if (maxDeltaRpackGenTime != 0) {
			if (candidateStatus == null) {
				candidateStatus = new AcsCandidateStatus();
				candidateStatus.setCsid(batchCandidateAssociation.getBcaid());
			}
			Calendar currentDateTime = Calendar.getInstance();
			// calculated candidate end time is the sum of current time +remaining time + extension
			long calculatedCandidateEndTime = Math.abs(currentDateTime.getTimeInMillis() + (totalDuration * 1000));
			gen_logger.debug("calculatedCandidateEndTime with including current time in milliseconds={}",
					calculatedCandidateEndTime);
			// if the calculated time is crossing the batch end time, then check the extra time
			if (calculatedCandidateEndTime > maxDeltaRpackGenTime) {
				long candidateExtensionTimeLeftMilliseconds =
						Math.abs((currentDateTime.getTimeInMillis() + (totalDuration * 1000)) - maxDeltaRpackGenTime);
				gen_logger.debug("candidateExtensionTimeLeftMilliseconds with comparing batch end time={}",
						candidateExtensionTimeLeftMilliseconds);
				// actualTimeLeft = totalDuration - time;
				long candidateExtensionTimeLeftSeconds =
						TimeUnit.MILLISECONDS.toSeconds(candidateExtensionTimeLeftMilliseconds);
				gen_logger.debug("candidateExtensionTimeLeftSeconds with comparing batch end time={}",
						candidateExtensionTimeLeftSeconds);
				long currentExtendedDuration = (extendeDuration - candidateExtensionTimeLeftSeconds);
				if (currentExtendedDuration >= 0) {
					batchCandidateAssociation.setLostExtendedExamDuration(candidateExtensionTimeLeftSeconds);
					batchCandidateAssociation.setExtendedExamDuration(currentExtendedDuration);

				} else {
					batchCandidateAssociation.setLostExtendedExamDuration(extendeDuration);
					batchCandidateAssociation.setExtendedExamDuration(0);
				}

				// added for Audit trail
				// TP will send it
				AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
				abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
				abstractAuditActionDO.setActionType(CandidateActionsEnum.EXTENDED_DURATION_LOST);
				abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
				abstractAuditActionDO.setHostAddress(ip);
				abstractAuditActionDO.setExtendedDuration(batchCandidateAssociation.getExtendedExamDuration());
				abstractAuditActionDO.setLostExtendedDuration(batchCandidateAssociation.getLostExtendedExamDuration());

				AuditTrailLogger.auditSave(abstractAuditActionDO);
				// ends Audit trail
			} else {
				batchCandidateAssociation.setLostExtendedExamDuration(0);
				batchCandidateAssociation.setExtendedExamDuration(extendeDuration);
			}

			return candidateStatus;

		} else
			return candidateStatus;
	}

	/**
	 * This method is to audit all resource usage and pop up acceptance done on test player by candidate
	 * 
	 * @param candidateResourcesAuditDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean auditCandidateResourcesEvents(CandidateResourcesAuditDO candidateResourcesAuditDO)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("input from Tp for auditing resource and pop ups clicks = {}", candidateResourcesAuditDO);
		if (candidateResourcesAuditDO != null && candidateResourcesAuditDO.getActionType() != null) {
			// fetch the necessary information for audit trail i.e candidate identifier, batch code, assessment code and
			// test player ip
			// HashMap<String, Object> params = new HashMap<String, Object>();
			// params.put(ACSConstants.BATCH_ID, candidateResourcesAuditDO.getBatchCode());
			// params.put(ACSConstants.CAND_ID, candidateResourcesAuditDO.getCandID());

			// CandIPBatchAssessmentCodesDO candIPBatchAssessmentCodesDO =
			// (CandIPBatchAssessmentCodesDO) session
			// .getByQuery(
			// "select b.batchCode as batchCode, ad.assessmentCode as assessmentCode, cs.hostAddress as ip, c.identifier1 as candIdentifier FROM CandidateStatusTO cs,BatchCandidateAssociationTO bca,AssessmentDetailsTO ad,BatchDetailsTO b,AcsCandidate cd join cd.candidate c WHERE cs.batchId=(:batchId) and cs.candidateId=(:candId) and bca.batchId=cs.batchId and bca.candidateId=cs.candidateId and bca.batchId=b.batchId and bca.assessmentId = ad.assessmentId and bca.candidateId = c.candidateId",
			// params, CandIPBatchAssessmentCodesDO.class);
			// gen_logger
			// .info("fetched required information for auditing resource usage and pop up acceptance = {} based on candId = {} and batchId = {}",
			// candIPBatchAssessmentCodesDO, candidateResourcesAuditDO.getCandID(),
			// candidateResourcesAuditDO.getBatchID());
			// if (candIPBatchAssessmentCodesDO != null) {
			// form the audit object
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();

			// abstractAuditActionDO.setCandApplicationNumber(candIPBatchAssessmentCodesDO.getCandIdentifier());
			// abstractAuditActionDO.setBatchCode(candIPBatchAssessmentCodesDO.getBatchCode());
			abstractAuditActionDO.setBatchCandidateAssociation(candidateResourcesAuditDO.getBcaId());
			abstractAuditActionDO.setActionType(candidateResourcesAuditDO.getActionType());
			abstractAuditActionDO.setHostAddress(candidateResourcesAuditDO.getIp());
			abstractAuditActionDO.setContext(candidateResourcesAuditDO.getContext());
			abstractAuditActionDO.setBatchCode(candidateResourcesAuditDO.getBatchCode());
			abstractAuditActionDO.setCandID(candidateResourcesAuditDO.getCandID());
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(candidateResourcesAuditDO.getClientTime(),
					TimeUtil.DISPLAY_DATE_FORMAT));

			long candRemainingTime = candidateResourcesAuditDO.getCandRemainingTime();
			if (candRemainingTime != 0)
				abstractAuditActionDO.setActionTime(TimeUtil.formatTime(candRemainingTime));

			// save to audit log file
			AuditTrailLogger.auditSave(abstractAuditActionDO);

			gen_logger.info(
					"audited resource usage and acceptance pop ups for candidate with id= {} under batch with id = {}",
					candidateResourcesAuditDO.getCandID(), candidateResourcesAuditDO.getBatchCode());
			// } else {
			// gen_logger.info(
			// "Not able to fetch candidate identification number, ip, batchCode and assessmentCode = {}",
			// candIPBatchAssessmentCodesDO);
			// return false;
			// }
		} else {
			gen_logger.info("Invalid input = {}", candidateResourcesAuditDO);
			return false;
		}
		StatsCollector.getInstance().log(startTime, "auditCandidateResourcesEvents", "CandidateService",
				candidateResourcesAuditDO.getCandID());
		return true;
	}

	/**
	 * returns the feedback duration for a candidate if the duration goes beyond batch end time trim duartion based on
	 * batch end time
	 * 
	 * @param candidateActionDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public Map<Long, String> getFeedbackRemainingTimeAndData(CandidateActionDO candidateActionDO)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("initiated getFeedbackRemainingTime where input = {}", candidateActionDO);

		long feedbackDuration = 0;
		String feedBackResponse = null;
		Calendar currentTime = Calendar.getInstance();
		Map<Long, String> feedBackData = new HashMap<Long, String>();

		// fetch batch details
		AcsBatch batchDetailsTO = bs.getBatchDetailsByBatchCode(candidateActionDO.getBatchCode());
		gen_logger.info("batchDetails object object for fetching feedbackDuration = {}", batchDetailsTO);

		// check whether batch end time is over or not
		if (batchDetailsTO != null && batchDetailsTO.getMaxDeltaRpackGenerationTime().after(currentTime)) {
			// fetch the candidate status details based on the batch id and candidate id
			AcsBatchCandidateAssociation bca =
					(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(
							candidateActionDO.getBatchCandidateAssociationId(),
							AcsBatchCandidateAssociation.class.getCanonicalName());
			gen_logger.info("AcsBatchCandidateAssociation object for fetching feedbackDuration = {}", bca);

			// check whether candidate has ended his test or not
			if (bca != null && bca.getActualTestEndTime() != null
					&& bca.getFeedBackStatus().equals(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED)) {
				// check whether feedback duration is already calculated for the candidate or not
				if (bca.getAllotedFeedbackDuration() != 0) {
					gen_logger
							.info("feedbatck duration is already calculated for the candidate with id = {}, hence considering him as crashed candidate",
									candidateActionDO.getCandID());

					// send alloted feedback duration
					feedbackDuration = bca.getAllotedFeedbackDuration();
					gen_logger.info("alloted feedback duration = {}", feedbackDuration);

					// trim the feedback duration base on the batch end time
					feedbackDuration =
							trimDurationOnBatchEndTime(currentTime, feedbackDuration,
									batchDetailsTO.getMaxDeltaRpackGenerationTime());
					feedBackResponse = bca.getFeedBackFormData();
					gen_logger.info("entitled feedback duration = {} and feedBackData ={}", feedbackDuration,
							feedBackResponse);
					feedBackData.put(feedbackDuration, feedBackResponse);

				} else if (bca.getFeedbackDuration() != 0) {
					// feedbackDuration will be in seconds
					feedbackDuration = bca.getFeedbackDuration();
					gen_logger.info("actual feedback duraion = {}", feedbackDuration);

					// trim the feedback duration base on the batch end time
					feedbackDuration =
							trimDurationOnBatchEndTime(currentTime, feedbackDuration,
									batchDetailsTO.getMaxDeltaRpackGenerationTime());
					feedBackResponse = bca.getFeedBackFormData();
					gen_logger.info("entitled feedback duration = {} and feedBackData ={}", feedbackDuration,
							feedBackResponse);
					feedBackData.put(feedbackDuration, feedBackResponse);

					// update allocated feedback duration in data base
					bca.setAllotedFeedbackDuration(feedbackDuration);
					batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(bca);
					/**
					 * Commenting out update query and updating object.
					 */
					// boolean updateAllotedDurationStatus =
					// updateAllotedFeedbackDuartion(feedbackDuration, candidateActionDO.getBatchCode(),
					// candidateActionDO.getCandID());
					gen_logger.info("updation alloted feedback duration = {}");
				}
			} else {
				feedBackData.put(feedbackDuration, feedBackResponse);
				gen_logger
						.info("Candidate with id = {} hasn't ended his test or already submitted feedback for batch with id = {}",
								candidateActionDO.getCandID(), candidateActionDO.getBatchCode());
			}
		} else {
			feedBackData.put(feedbackDuration, feedBackResponse);
			gen_logger.info(
					"batch end time has elapsed for batch with id = {},hence, not processing getFeedbackRemainingTime",
					candidateActionDO.getBatchCode());
		}
		StatsCollector.getInstance().log(startTime, "getFeedbackRemainingTimeAndData", "CandidateService",
				candidateActionDO.getCandID());
		return feedBackData;
	}

	/**
	 * gets the configured HBM time interval from the properties file if it doesn't exist in properties file then
	 * considers default value as mentioned in ACSConstants file
	 * 
	 * @return {@link Integer} : configured or default HBM time interval
	 * @since Apollo v2.0
	 */
	public int getConfiguredHBMTimeInterval() {
		gen_logger.info("initiated getConfiguredHBMTimeInterval");

		// read the default hbmTimeInterval
		int hbmTimeInterval = ACSConstants.DEFAULT_HBM_TIME_INTERVAL_IN_SECS;

		// read the configured hbmTimeInterval from properties file
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.HBM_TIME_INTERVAL);
		// checks whether the configured value in properties file is numeric or not
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			hbmTimeInterval = Integer.parseInt(propValue);
			gen_logger.info("configured HBM time interval = {}", hbmTimeInterval);
		} else {
			gen_logger.info("default HBM time interval = {}", hbmTimeInterval);
		}
		return hbmTimeInterval;
	}

	/**
	 * This api will initiate a job which will send the candidate information to cdb for a particular batch
	 * 
	 * @param batchId
	 * @param batchCode
	 * @param candidateStatusList
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean startCandidateInformationNotifierInitiator(String batchCode,
			List<CandidateStatus> candidateStatusList) throws GenericDataModelException {
		gen_logger
				.info("initiated startCandidateInformationNotifierInitiator where batchId = {}, batchCode = {} and candidateStatusList = {}",
						batchCode, candidateStatusList);
		JobDetail job = null;
		Scheduler scheduler = null;

		String CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME = ACSConstants.CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME;
		String CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP = ACSConstants.CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			// create a job instance and add required information to job data map
			job =
					JobBuilder
							.newJob(CandidateInformationNotifier.class)
							.withIdentity(CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);
			job.getJobDataMap().put(ACSConstants.CANDIDATE_STATUS_LIST, candidateStatusList);

			// check whether already a trigger exists for the specified batch or not
			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME + batchCode,
							CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP));
			gen_logger.info("trigger with specified key = {} and group = {}", CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME
					+ batchCode, CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP);

			// if trigger already exists unschedule the existing one and create a new trigger
			if (trigger != null) {
				scheduler.unscheduleJob(TriggerKey.triggerKey(CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME + batchCode,
						CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP));
				gen_logger
						.info("Already a trigger exists for CandidateInformationNotifier job for specified batchId = {} hence, unscheduling it and initiating new trigger",
								batchCode);
			}

			// create a trigger new instance
			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(CDB_CANDIDATE_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_CANDIDATE_INFORMATION_NOTIFIER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule()
											.withMisfireHandlingInstructionNextWithRemainingCount()).startNow().build();

			// associate the trigger and job and add them to the scheduler
			scheduler.scheduleJob(job, trigger);
			gen_logger.trace("Trigger for CandidateInformationNotifierInitiator = {} " + trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing CandidateInformationNotifierInitiator...", ex);
			return false;
		}
		return true;
	}

	/**
	 * The saveExternalDeviceStatusByHostAddress API used to save the peripheral status
	 * 
	 * @param hostAddress
	 * @param actionType
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@SuppressWarnings("unchecked")
	public int saveExternalDeviceStatusByHostAddress(String hostAddress, int actionType)
			throws GenericDataModelException {
		gen_logger.info("initiated saveExternalDeviceStatusByHostAddress API for hostAddress ={} and actionType={}",
				hostAddress, actionType);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("hostAddress", hostAddress);
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		List<AcsCandidateStatus> candidateStatusTO =
				session.getListByQuery(ACSQueryConstants.QUERY_CAND_STATUS_INFO_BY_HOSTADDRESS, params);

		if (candidateStatusTO == null) {
			gen_logger
					.info("Not allowing to update the external device status since there is no record with IPAddress ={} in database with taking test at time instance ={}",
							hostAddress, Calendar.getInstance());
			return 0;

		}

		if (candidateStatusTO.size() == 1) {
			// TODO:Lazy loading - Done
			Integer candStatusId = candidateStatusTO.get(0).getCsid();
			AcsBatchCandidateAssociation acsBatchCandidateAssociation =
					batchCandidateAssociationService.getBCAforCandidateStatus(candStatusId);
			String batchId = acsBatchCandidateAssociation.getBatchCode();
			int candId = acsBatchCandidateAssociation.getCandidateId();
			params.clear();
			params.put(ACSQueryConstants.PARAM_CAND_ID, candId);
			params.put(ACSQueryConstants.PARAM_BATCH_ID, batchId);
			CandidateActionsEnum candidateAction = null;
			if (actionType == 10) {
				params.put("peripheralStatus", HBMStatusEnum.RED);
				candidateAction = CandidateActionsEnum.EXTERNAL_DEVICE_INSERTED;

			} else if (actionType == 11) {
				candidateAction = CandidateActionsEnum.EXTERNAL_DEVICE_REMOVED;
				params.put("peripheralStatus", HBMStatusEnum.GREEN);

			}
			int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_PERIPHERAL_STATUS, params);
			if (count > 0) {
				gen_logger
						.info("Successfully executed and updated saveExternalDeviceStatusByHostAddress API for hostAddress ={} and actionType={}",
								hostAddress, actionType);
				auditExternalDeviceinCandidateLog(batchId, candId, hostAddress, candidateAction);

			}

			return count;

		} else if (candidateStatusTO.size() > 1) {
			gen_logger
					.info("Not allowing to update the external device status since with same IPAddress ={} there are more than one record in database with taking test at time instance ={}",
							hostAddress, Calendar.getInstance());
			return 0;

		}
		return 0;

	}

	/**
	 * auditExternalDeviceinCandidateLog API used to audit external device status in candidate audit log file.
	 * 
	 * @param batchCode
	 * @param candidateId
	 * @param HostAddress
	 * @param candidateAction
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditExternalDeviceinCandidateLog(String batchCode, int candidateId, String HostAddress,
			CandidateActionsEnum candidateAction) throws GenericDataModelException {

		String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(candidateId, batchCode);
		String candIdentifier = getApplicationNumberByCandidateId(candidateId);
		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		if (candidateAction != null) {
			abstractAuditActionDO.setActionType(candidateAction);
		}

		abstractAuditActionDO.setAssessmentID(assessmentTypeValue);
		AcsBatchCandidateAssociation batchCandidateAssociation =
				batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandId(batchCode, candidateId);
		abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
		abstractAuditActionDO.setCandID(candidateId);
		abstractAuditActionDO.setCandApplicationNumber(candIdentifier);
		abstractAuditActionDO.setBatchCode(batchCode);
		abstractAuditActionDO.setHostAddress(HostAddress);

		AuditTrailLogger.auditSave(abstractAuditActionDO);
	}

	/**
	 * fetch max allowed login attempts fetches extended no of valid login attempts if already specified otherwise
	 * considers the value which is part of br rules
	 * 
	 * @param candidateId
	 * @param batchId
	 * @param assessmnetId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int getMaxNumberOfValidLoginAttempts(String assessmentCode, String batchCode)
			throws GenericDataModelException {
		gen_logger.info("initiated getMaxNumberOfValidLoginAttempts where  assessmnetId={}", assessmentCode);

		int maxNumberOfValidLoginAttempts = 0;
		AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
				cdeas.getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(batchCode, assessmentCode);
		if (bussinessRulesAndLayoutRulesTO != null) {
			maxNumberOfValidLoginAttempts = bussinessRulesAndLayoutRulesTO.getMaxNumberOfValidLoginAttempts();
			gen_logger.info("maxNumberOfValidLoginAttempts from br rules = {}", maxNumberOfValidLoginAttempts);
		}
		return maxNumberOfValidLoginAttempts;
	}

	public List<AcsCandidate> getCandidateByCandIds(List<Integer> candidateIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candidateIds", candidateIds);

		List<AcsCandidate> candidateTOs =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ACSCANDIDATE, params, 0);
		if (candidateTOs.isEmpty()) {
			candidateTOs = null;
		}
		return candidateTOs;
	}

	/**
	 * This api will initiate a job which will send the qpack information to cdb for a particular batch
	 * 
	 * @param batchId
	 * @param batchCode
	 * @param candidateStatusList
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean
			startQpackInformationNotifierInitiator(String batchCode, List<QpackInformationDO> qpackInformationDOs)
					throws GenericDataModelException {
		gen_logger.info(
				"initiated startQpackInformationNotifierInitiator where batchCode = {} and qpackInformationDOs = {}",
				batchCode, qpackInformationDOs);
		JobDetail job = null;
		Scheduler scheduler = null;

		String CDB_QPACK_INFORMATION_NOTIFIER_NAME = ACSConstants.CDB_QPACK_INFORMATION_NOTIFIER_NAME;
		String CDB_QPACK_INFORMATION_NOTIFIER_GRP = ACSConstants.CDB_QPACK_INFORMATION_NOTIFIER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			// create a job instance and add required information to job data map
			job =
					JobBuilder
							.newJob(QpackInformationNotifier.class)
							.withIdentity(CDB_QPACK_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_QPACK_INFORMATION_NOTIFIER_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);
			job.getJobDataMap().put(ACSConstants.QPACK_INFORMATION_LIST, qpackInformationDOs);

			// check whether already a trigger exists for the specified batch or not
			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(CDB_QPACK_INFORMATION_NOTIFIER_NAME + batchCode,
							CDB_QPACK_INFORMATION_NOTIFIER_GRP));
			gen_logger.info("trigger with specified key = {} and group = {}", CDB_QPACK_INFORMATION_NOTIFIER_NAME
					+ batchCode, CDB_QPACK_INFORMATION_NOTIFIER_GRP);

			// if trigger already exists unschedule the existing one and create a new trigger
			if (trigger != null) {
				scheduler.unscheduleJob(TriggerKey.triggerKey(CDB_QPACK_INFORMATION_NOTIFIER_NAME + batchCode,
						CDB_QPACK_INFORMATION_NOTIFIER_GRP));
				gen_logger
						.info("Already a trigger exists for startQpackInformationNotifierInitiator job for specified batchId = {} hence, unscheduling it and initiating new trigger",
								batchCode);
			}

			// create a trigger new instance
			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(CDB_QPACK_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_QPACK_INFORMATION_NOTIFIER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule()
											.withMisfireHandlingInstructionNextWithRemainingCount()).startNow().build();

			// associate the trigger and job and add them to the scheduler
			scheduler.scheduleJob(job, trigger);
			gen_logger.trace("Trigger for startQpackInformationNotifierInitiator = {} " + trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing startQpackInformationNotifierInitiator...", ex);
			return false;
		}
		return true;
	}

	/**
	 * getCandidateAuditLogs API used to get candidate Audit logs from DB.
	 * 
	 * @param candidateAppNumb
	 *            , batchcode
	 * @throws GenericDataModelException
	 */
	public List<AcsCandidatAudits> getCandidateAuditLogs(int bcaid, String batchStartDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		// List<AcsCandidatAudits> candidateAuditsTOs =
		// session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CANDSTATUS_FOR_BCAID, params, 0);
		// if (candidateAuditsTOs.isEmpty()) {
		// candidateAuditsTOs = null;
		// }
		// return candidateAuditsTOs;

		String query = ACSQueryConstants.QUERY_GET_CAND_AUDIT_DATA;

		query = query.replaceAll("seqdate", batchStartDate);

		List<AcsCandidatAudits> candidateAuditsTOs =
				session.getResultListByNativeSQLQuery(query, params, 0, 0, AcsCandidatAudits.class);
		if (candidateAuditsTOs.isEmpty()) {
			candidateAuditsTOs = null;
		}
		return candidateAuditsTOs;

	}

	public AcsBatchCandidateAssociation getCandidateTypebyCandId(int candId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CAND_TYPE_BY_CAND_ID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);

		AcsBatchCandidateAssociation bca = (AcsBatchCandidateAssociation) session.getByQuery(query, params);

		return bca;
	}

	public AcsCandidate getCandidateDetailsFromCandId(int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);
		AcsCandidate candidateTO =
				(AcsCandidate) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_BY_CANDID, params);

		return candidateTO;
	}

	public String getCandidateIdentifierFromCandId(int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CAND_ID, candId);
		String candidateTO =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CAND_IDEFTIFIER_BY_CANDID, params);

		return candidateTO;
	}
	/**
	 * ckecks whether there is a candidate with the specified identifier
	 * 
	 * @param identifier1
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean isCandidateExists(String identifier1) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFIER1, identifier1);
		AcsCandidate candidateTO =
				(AcsCandidate) session.getByQuery(ACSQueryConstants.QUERY_IS_CANDIDATE_EXISTS_BY_IDENTIFIER1, params);
		if (candidateTO == null) {
			return false;
		} else {
			return true;
		}
	}

	public boolean updateCandidateStatus(AcsCandidateStatus candidateStatusTO) throws GenericDataModelException {
		session.saveOrUpdate(candidateStatusTO);
		return true;
	}

	/**
	 * @param candidate
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveOrUpdate(AcsCandidate candidate) throws GenericDataModelException {
		session.saveOrUpdate(candidate);
	}

	/**
	 * SaveorUpdate {@link AcsCandidateStatus}
	 * 
	 * @param acsCandidateStatus
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveOrUpdateCandidateStatus(AcsCandidateStatus acsCandidateStatus) throws GenericDataModelException {
		session.saveOrUpdate(acsCandidateStatus);
	}

	/**
	 * 
	 * @param batchId
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsCandidateResponses> getCandidateResponsesByBcaId(int bcaid) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		List<AcsCandidateResponses> candidateResponses =
				(List<AcsCandidateResponses>) session.getResultAsListByQuery(
						"FROM AcsCandidateResponses cr WHERE cr.acsbatchcandidateassociation.bcaid=(:bcaid) ", params,
						0);
		if (candidateResponses.isEmpty()) {
			candidateResponses = null;
		}
		return candidateResponses;
	}

	/**
	 * 
	 * @param batchId
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<CandidateResponsesDO> getCandidateResponsesForSectionByBCAId(int bcaid, String sectionId,
			String qpaperCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		params.put("sectionId", sectionId);
		params.put("qpaperCode", qpaperCode);
		List<CandidateResponsesDO> candidateResponses =
				session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_CANDIDATERESPONSES_FOR_SECTION_BY_BCAID, params, 0, 0,
						CandidateResponsesDO.class);

		if (candidateResponses.isEmpty()) {
			candidateResponses = null;
		}
		return candidateResponses;
	}

	/**
	 * @param candidateResponses
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateCandidateResponses(AcsCandidateResponses candidateResponses) throws GenericDataModelException {
		session.merge(candidateResponses);
		return true;
	}

	/**
	 * This API audits the option navigations and 'currentViewed question' for next navigatation
	 * 
	 * @param auditOfNavigationDO
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean auditConsolidated(AuditOfNavigationDO auditOfNavigationDO) throws GenericDataModelException,
			CandidateRejectedException {
		boolean isSucces = true;

		// navigation logging
		List<AuditCandidateResponseDO> candidateResponseDOs = auditOfNavigationDO.getCandidateResponseDOs();
		if (candidateResponseDOs != null && !candidateResponseDOs.isEmpty()) {
			int batchId = 0;
			int candidateId = 0;
			StringBuffer dbMsg = new StringBuffer();
			String tempDelim = "";
			for (AuditCandidateResponseDO auditCandidateResponseDO : candidateResponseDOs) {
				batchId = auditCandidateResponseDO.getBatchID();
				candidateId = auditCandidateResponseDO.getCandID();
				String auditMessage = auditCandidateResponse(auditCandidateResponseDO);
				dbMsg.append(tempDelim);
				tempDelim = ACSConstants.DELIM_FOR_AUDIT_MESSAGE;
				dbMsg.append(auditMessage);
			}
			gen_logger.trace("Audit log batchId:{} candidateId:{} :{}", batchId, candidateId, dbMsg.toString());
			candidateAuditService.auditCandidateActions(candidateResponseDOs.get(0).getBatchCandidateAssociationId(),
					dbMsg.toString());
		}

		// audit current Viewed question
		List<CandidateActionDO> currentViewedQuestionDOs = auditOfNavigationDO.getCurrentViewedQuestionDOs();
		if (currentViewedQuestionDOs != null && !currentViewedQuestionDOs.isEmpty()) {
			for (CandidateActionDO candidateActionDO : currentViewedQuestionDOs) {
				if (!updateCandCurrentViewedQuestion(candidateActionDO)) {
					isSucces = false;
					return isSucces;
				}
			}
		}

		return isSucces;

	}

	/*
	 * This API is used to audit changing options into candidate audit trail.
	 */
	@Override
	public String auditCandidateResponse(AuditCandidateResponseDO auditCandidateResponse)
			throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		int candID = auditCandidateResponse.getCandID();
		// int batchId = bs.getBatchIdbyCandId(candID);
		String secondaryQpID = auditCandidateResponse.getSecondaryQpID();
		String secondaryQID = auditCandidateResponse.getSecondaryQID();
		String language = auditCandidateResponse.getLanguage();

		// CandidateStatusTO cs = getCandidateStatus(candID, batchId);
		// String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(candID, batchId);
		long mill = auditCandidateResponse.getDurationMillisecs();

		ChangeOptionAuditDO changeOptionAuditDO = new ChangeOptionAuditDO();
		// changeOptionAuditDO.setAssessmentID(assessmentTypeValue);
		changeOptionAuditDO.setCandID(candID);
		// changeOptionAuditDO.setCandApplicationNumber(getApplicationNumberByCandidateId(candID));
		changeOptionAuditDO.setBatchCode(auditCandidateResponse.getBatchCode());
		// changeOptionAuditDO.setBatchCode(bs.getBatchCodebyBatchId(batchId));
		changeOptionAuditDO.setHostAddress(auditCandidateResponse.getIp());
		changeOptionAuditDO.setPrevOptionID(auditCandidateResponse.getPreviousOption());
		changeOptionAuditDO.setNewOptionID(auditCandidateResponse.getCurrentOption());
		changeOptionAuditDO.setqID(auditCandidateResponse.getqID());
		changeOptionAuditDO.setQpID(auditCandidateResponse.getQpID());
		changeOptionAuditDO.setSecID(auditCandidateResponse.getSecID());
		changeOptionAuditDO.setTpQID(auditCandidateResponse.getTpQId());// new param
		// added new params
		changeOptionAuditDO.setQuestionType(auditCandidateResponse.getQuestionType());
		changeOptionAuditDO.setParentQuestionId(auditCandidateResponse.getParentQuestionId());
		changeOptionAuditDO.setSecondaryQID(secondaryQID);
		changeOptionAuditDO.setSecondaryQpID(secondaryQpID);
		changeOptionAuditDO.setLanguage(language);
		// ends
		changeOptionAuditDO.setClientTime(TimeUtil.convertTimeAsString(auditCandidateResponse.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));

		if (auditCandidateResponse.getPreviousOption() != null) {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.SWITCHOPTION);
		} else if (auditCandidateResponse.getPreviousOption() == null
				|| auditCandidateResponse.getPreviousOption().trim() == "") {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.OPTIONSELECTED);
		}

		if (auditCandidateResponse.getDurationMillisecs() != 0)
			changeOptionAuditDO.setActionTime(TimeUtil.formatTime(mill));

		String auditMessage = AuditTrailLogger.getAuditMessage(changeOptionAuditDO);
		StatsCollector.getInstance().log(startTime, "auditCandidateRespone", "CandidateService", candID);
		return auditMessage;
	}

	/**
	 * Provides {@link ProvisionalCertificateEntity}
	 * 
	 * @param candidateID
	 * @param batchCode
	 * @return
	 * @throws ACSException
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ProvisionalCertificateEntity getProvisionalCertificateData(int candidateID, String batchCode)
			throws ACSException, GenericDataModelException {
		gen_logger.debug("getProvisionalCertificateData is initiated with CandidateId: {} BatchId:{}", candidateID,
				batchCode);
		if (candidateID == 0 || batchCode.equals("0") || batchCode.isEmpty())
			throw new ACSException("Invalid data");
		List<Integer> candidateIds = new ArrayList<Integer>();
		candidateIds.add(candidateID);
		List<ProvisionalCertificateEntity> provisionalCertificateEntity =
				batchCandidateAssociationService.getProvisionalCertificateEntityByCandidateIds(batchCode, candidateIds);
		gen_logger.debug("provisionalCertificateEntity for candidate:{} is {}", candidateID,
				provisionalCertificateEntity);
		if (provisionalCertificateEntity != null && !provisionalCertificateEntity.isEmpty())
			return provisionalCertificateEntity.get(0);
		else
			return null;
	}

	/**
	 * Prints {@link ProvisionalCertificateEntity} generated earlier
	 * 
	 * @param candidateID
	 * @param batchCode
	 * @return
	 * @throws ACSException
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean printProvisionalCertificateData(int candidateID, String batchCode) throws ACSException,
			GenericDataModelException {
		gen_logger.debug("printProvisionalCertificateData is initiated with CandidateId: {} BatchId:{}", candidateID,
				batchCode);
		if (candidateID == 0 || batchCode.equals("0") || batchCode.isEmpty())
			throw new ACSException("Invalid data");
		CandidatePDFGeneratorService candidatePDFGeneratorService = new CandidatePDFGeneratorService();
		List<Integer> candidateIds = new ArrayList<>();
		candidateIds.add(candidateID);
		boolean isPDFGenerated =
				candidatePDFGeneratorService.generateProvisionalScoreCardForCandidates(batchCode, candidateIds, true);
		gen_logger.debug("printProvisionalCertificateData for candidate:{} is {}", candidateID, isPDFGenerated);
		return isPDFGenerated;
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
		params.put("qpCode", qpCode);
		params.put("questionType", QuestionType.MULTIPLE_CHOICE_QUESTION.toString());
		List<CandidateAllRCQidDO> candidateAllRCQidDO =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QUESTION_FOR_PAPER, params, 0,
						CandidateAllRCQidDO.class);
		if (candidateAllRCQidDO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return candidateAllRCQidDO;
	}

	@Override
	public boolean startRpackGeneratorForEndedCandidatesJob(String batchCode, Calendar startTime, Calendar endTime,
			int repeatInterval) throws GenericDataModelException {
		DeploymentModeEnum deploymentMode = acsPropertyUtil.getDeploymentMode();
		if (deploymentMode != null && DeploymentModeEnum.CENTRALIZED.toString().equalsIgnoreCase(deploymentMode.toString())) {
			gen_logger.info("Skipping the creation of candidate live status notification job in centraized mode of deployment instance");
			return true;
		}
		
		if (startTime.after(endTime) || endTime.before(Calendar.getInstance())) {
			gen_logger
					.info("Not initiating startRpackGeneratorForEndedCandidatesJob job because endTime is after startTime or end time is elapsed...");
			return false;
		}

		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME = ACSConstants.RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME;
		String RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP = ACSConstants.RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(RpackGeneratorForEndedCandidates.class)
							.withIdentity(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
									RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP).storeDurably(false).requestRecovery(true)
							.build();
			// job.getJobDataMap().put(ACSConstants.BATCH_ID, batchId);
			job.getJobDataMap().put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);
			job.getJobDataMap().put("isEndTimeFire", false);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
							RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP));
			if (trigger != null) {
				gen_logger.info(
						"Already a trigger exists for isAllCandidatesSubmittedTest job for specified batchCode = {} ",
						batchCode);
				scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
						RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP));
			}
			if (startTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
										RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP)
								.startNow()
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInMinutes(repeatInterval)
												.withMisfireHandlingInstructionIgnoreMisfires())
								.endAt(endTime.getTime()).build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
										RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP)
								.startAt(startTime.getTime())
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withRepeatCount(-1)
												.withIntervalInMinutes(repeatInterval)
												.withMisfireHandlingInstructionIgnoreMisfires())
								.endAt(endTime.getTime()).build();
			}
			gen_logger.trace("Trigger for isAllCandidatesSubmittedTest = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing isAllCandidatesSubmittedTest ...", ex);
			return false;
		}
		return true;
	}

	public boolean startRpackGeneratorForEndedCandidatesJob(String batchCode, Calendar startTime)
			throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME =
				ACSConstants.RPACK_GENERATOR_FOR_ENDED_CANDIDATES_AT_END_TIME_NAME;
		String RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP =
				ACSConstants.RPACK_GENERATOR_FOR_ENDED_CANDIDATES_AT_END_TIME_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder
							.newJob(RpackGeneratorForEndedCandidates.class)
							.withIdentity(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
									RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP).storeDurably(false).requestRecovery(true)
							.build();
			// job.getJobDataMap().put(ACSConstants.BATCH_ID, batchId);
			job.getJobDataMap().put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);
			job.getJobDataMap().put("isEndTimeFire", true);

			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
							RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP));
			if (trigger != null) {
				gen_logger.info(
						"Already a trigger exists for isAllCandidatesSubmittedTest job for specified batchCode = {} ",
						batchCode);
				scheduler.unscheduleJob(TriggerKey.triggerKey(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
						RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(RPACK_GENERATOR_FOR_ENDED_CANDIDATES_NAME + batchCode,
									RPACK_GENERATOR_FOR_ENDED_CANDIDATES_GRP)
							.startAt(startTime.getTime())
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.build();
			gen_logger.trace("Trigger for isAllCandidatesSubmittedTest = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing isAllCandidatesSubmittedTest ...", ex);
			return false;
		}
		return true;
	}

	/**
	 * checks whether there are any candidates who has ended the test.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean isEndedCandidatesExists(String batchCode) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		int count =
				((BigInteger) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_ENDED_CANDIDATE_COUNT_FOR_RPACK_GENERATION, params)).intValue();
		if (count > 0)
			return true;
		else
			return false;
	}
	
	/**
	 * checks whether there are any candidates who has ended the test.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<String> getBatchCodesOfEndedCandidates(List<String> batchCodes) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODES, batchCodes);
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		List<String> batchCodesList =
				((List<String>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_ENDED_CANDIDATE_COUNT_FOR_RPACK_GENERATION_BY_BATCH_IDS, params));
		
		return batchCodesList;
	}

	/**
	 * @param batchCode
	 * @param deltaRpackGenerationTime
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean initiateReportPackGenerator(String batchCode, Calendar deltaRpackGenerationTime)
			throws GenericDataModelException {
		DeploymentModeEnum deploymentMode = acsPropertyUtil.getDeploymentMode();
		if (deploymentMode != null && DeploymentModeEnum.CENTRALIZED.toString().equalsIgnoreCase(deploymentMode.toString())) {
			gen_logger.info("Skipping the creation of candidate live status notification job in centraized mode of deployment instance");
			return true;
		}
		
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String eventCode = cdeas.getEventCodeByBatchCode(batchCode);
		String REPORT_PACK_GENERATOR_NAME = ACSConstants.REPORT_PACK_GENERATOR_NAME;
		String REPORT_PACK_GENERATOR_GROUP = ACSConstants.REPORT_PACK_GENERATOR_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(ReportPackGenerator.class)
							.withIdentity(REPORT_PACK_GENERATOR_NAME + batchCode, REPORT_PACK_GENERATOR_GROUP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put("forceStart", true);
			job.getJobDataMap().put("eventCode", eventCode);
			job.getJobDataMap().put("batchCode", batchCode);
			trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(REPORT_PACK_GENERATOR_NAME + batchCode,
							REPORT_PACK_GENERATOR_GROUP));
			if (trigger != null) {
				gen_logger.info("Already a trigger exists for ReportPackGenerator job for specified identifier = {} ",
						REPORT_PACK_GENERATOR_NAME + batchCode);
				scheduler.unscheduleJob(TriggerKey.triggerKey(REPORT_PACK_GENERATOR_NAME + batchCode,
						REPORT_PACK_GENERATOR_GROUP));
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(REPORT_PACK_GENERATOR_NAME + batchCode, REPORT_PACK_GENERATOR_GROUP)
							.withSchedule(
									CronScheduleBuilder.cronSchedule(acsPropertyUtil.getCronExpressionForReportPack())
											.withMisfireHandlingInstructionDoNothing()).startNow()
							.endAt(deltaRpackGenerationTime.getTime()).build();

			gen_logger.trace("Trigger for Report pack generator = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			gen_logger.error("SchedulerException while executing initiateReportPackGenerator ...", ex);
			return false;
		}
		return true;
	}

	public long getSectionRemainingTimeForCandidate(int batchCandidateAssociationId, String sectionId,
			boolean isForceNavigation) throws GenericDataModelException, CandidateRejectedException {
		gen_logger.info("getSectionRemainingTimeForCandidate() => bcaid={}, section={}, isForceNavigation={}",
				batchCandidateAssociationId, sectionId, isForceNavigation);
		Calendar currentDateTime = Calendar.getInstance();
		long currentSpendTime = 0;
		long crashTime = 0;
		long remTime = 0;
		String propValue = null;

		// get BCA
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(batchCandidateAssociationId,
						AcsBatchCandidateAssociation.class.getCanonicalName());

		// get Batch details
		AcsBatch batchDetails =
				(AcsBatch) session.get(acsBatchCandidateAssociation.getBatchCode(), AcsBatch.class.getCanonicalName());
		String batchCode = acsBatchCandidateAssociation.getBatchCode();
		int candId = acsBatchCandidateAssociation.getCandidateId();

		// get Candidate Status Details
		AcsCandidateStatus candidateStatus = this.getCandidateStatus(acsBatchCandidateAssociation.getBcaid());

		// Get BR-LR rules
		AcsBussinessRulesAndLayoutRules brlr =
				cdeas.getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(
						acsBatchCandidateAssociation.getAssessmentCode(), batchCode);

		// Calculate heart beat Time interval
		int hbmTimeInterval = (ACSConstants.DEFAULT_HBM_TIME_INTERVAL_IN_SECS);
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.HBM_TIME_INTERVAL);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			hbmTimeInterval = Integer.parseInt(propValue);
		}

		// get section duration
		String sectionDuration = brlr.getAssessmentDuration().replaceAll("\\s", "");
		AssessmentDurationDO assessmentDuration = new Gson().fromJson(sectionDuration, AssessmentDurationDO.class);

		// If section level is not mentioned, provide the section id of latest section viewed
		if (sectionId == null || sectionId.isEmpty()) {
			sectionId = candidateStatus.getCurrentViewedSection();
			getLogger().debug("sectionId is not defined. Hence using CurrentViewedSection:{} and bcaid={}", sectionId,
					batchCandidateAssociationId);
			// If the candidate is crashed and hence no current viewed section is present, assign first section
			if (sectionId == null || sectionId.isEmpty()) {
				Map<String, Integer> sectionLevelDuration = assessmentDuration.getSectionLevelDuration();
				Entry<String, Integer> entry = sectionLevelDuration.entrySet().iterator().next();
				sectionId = entry.getKey();
				getLogger().debug("CurrentViewedSection is not present. Hence assigning first section:{} and bcaid={}",
						sectionId, batchCandidateAssociationId);
			}
		}

		Calendar actualSectionStartedTime = null;
		long allotedDurationFromBCA = 0;
		long prevLostTime = 0;
		CandidateReEnableDO candidateReEnableDO = null;
		if (acsBatchCandidateAssociation.getReenablecandidatejson() == null) {
			// Normal flow
			actualSectionStartedTime = acsBatchCandidateAssociation.getActualSectionStartedTime();
			allotedDurationFromBCA = acsBatchCandidateAssociation.getAllotedDuration();
			prevLostTime = candidateStatus.getPrevLostTime();
			// calculate currentSpendTime
			if (actualSectionStartedTime != null) {
				currentSpendTime =
						Math.abs(currentDateTime.getTimeInMillis() - actualSectionStartedTime.getTimeInMillis()) / 1000;
				getLogger().debug(
						"ActualSectionStartedTime: {} currentTime: {} . Hence currentSpendTime for bcaId:{} is {} seconds",
						actualSectionStartedTime.getTime(), currentDateTime.getTime(), batchCandidateAssociationId,
						currentSpendTime);

			} else
				getLogger().debug(
						"currentSpendTime for bcaId:{} is {} because candidate has not started any new section",
						batchCandidateAssociationId, currentSpendTime);
		} else {
			// Re birth flow
			candidateReEnableDO =
					gson.fromJson(acsBatchCandidateAssociation.getReenablecandidatejson(), CandidateReEnableDO.class);
			actualSectionStartedTime = candidateReEnableDO.getActualTestStartedTime();
			allotedDurationFromBCA = candidateReEnableDO.getAllotedDuration();
			prevLostTime = candidateReEnableDO.getPrevLostTime();
			// calculate currentSpendTime
			if (actualSectionStartedTime != null) {
				currentSpendTime =
						Math.abs(currentDateTime.getTimeInMillis() - actualSectionStartedTime.getTimeInMillis()) / 1000;
				getLogger().debug(
						"ActualSectionStartedTime: {} currentTime: {} . Hence currentSpendTime for bcaId:{} is {} seconds",
						actualSectionStartedTime.getTime(), currentDateTime.getTime(), batchCandidateAssociationId,
						currentSpendTime);

			} else
				getLogger().debug(
						"currentSpendTime for bcaId:{} is {} because candidate has not started any new section",
						batchCandidateAssociationId, currentSpendTime);

			if (candidateReEnableDO.getStatus().name().equals(RequestStatus.NEW.name())) {
				candidateReEnableDO.setStatus(RequestStatus.ACKNOWLEDGED);
				remTime = (candidateReEnableDO.getAllotedDuration());
				candidateStatus.setLastHeartBeatTime(currentDateTime);
				String reenablecandidatejson = gson.toJson(candidateReEnableDO);
				acsBatchCandidateAssociation.setReenablecandidatejson(reenablecandidatejson);
				session.saveOrUpdate(acsBatchCandidateAssociation);
				getLogger().debug("getRemainingTimeForCandidate for {} is {}", batchCandidateAssociationId,
						remTime);
				return remTime;
			}
		}



		// In case of SMU drive, multiple logins happening for same candidate,
		// we are no more using prevheartbeat time to calculate crashtime, instead using LastCrashedTime
		if (candidateStatus.getLastCrashedTime() != null) {
			crashTime =
					Math.abs(currentDateTime.getTimeInMillis() - candidateStatus.getLastCrashedTime().getTimeInMillis()) / 1000;
			getLogger().debug("crashTime for bcaId:{} is {}", batchCandidateAssociationId, crashTime);
		}

		remTime =
				sectionLevelRemainingTimeExtendedMethod(batchCandidateAssociationId, sectionId, currentDateTime, currentSpendTime,
						crashTime, acsBatchCandidateAssociation, batchDetails, candidateStatus,
						hbmTimeInterval, assessmentDuration, actualSectionStartedTime, allotedDurationFromBCA,
						prevLostTime, candidateReEnableDO);

		gen_logger
				.info("Returning getSectionRemainingTimeForCandidate i.e allotedDuration = {} for section = {} for bcaid = {} ",
						remTime, sectionId, batchCandidateAssociationId);


		// audit trail
		// fetch the necessary information for audit trail i.e candidate identifier, batch code, assessment code and
		// test player ip
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", batchCandidateAssociationId);

		CandIPBatchAssessmentCodesDO candIPBatchAssessmentCodesDO =
				(CandIPBatchAssessmentCodesDO) session
						.getByQuery(
								"select b.batchCode as batchCode, ad.assessmentCode as assessmentCode, "
										+ " bca.hostAddress as ip, c.identifier1 as candIdentifier FROM AcsCandidateStatus cs, "
										+ " AcsBatchCandidateAssociation bca,AcsAssessment ad,AcsBatch b,AcsCandidate c WHERE "
										+ " cs.csid=(:bcaid)  and bca.bcaid=cs.csid and bca.batchCode=b.batchCode and "
										+ " bca.assessmentCode = ad.assessmentCode and bca.candidateId = c.candidateId",
								params, CandIPBatchAssessmentCodesDO.class);
		gen_logger.info("fetched required information for auditing resource usage and pop up acceptance = {} "
				+ " based on candId = {} and batchCandidateAssociationId = {}", candIPBatchAssessmentCodesDO, candId,
				batchCandidateAssociationId);
		if (candIPBatchAssessmentCodesDO != null) {
			// form the audit object
			gen_logger.info("Auditing resource = {} based on candId = {} and batchCandidateAssociationId = {}",
					candIPBatchAssessmentCodesDO, candId, batchCandidateAssociationId);
			SectionNavigationAuditDO abstractAuditActionDO = new SectionNavigationAuditDO();
			abstractAuditActionDO.setSectionId(sectionId);
			abstractAuditActionDO.setForceNavigation(isForceNavigation);
			abstractAuditActionDO.setCandApplicationNumber(candIPBatchAssessmentCodesDO.getCandIdentifier());
			abstractAuditActionDO.setBatchCode(candIPBatchAssessmentCodesDO.getBatchCode());
			abstractAuditActionDO.setCandID(candId);
			abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociationId);

			if (isForceNavigation) {
				abstractAuditActionDO.setActionType(CandidateActionsEnum.FORCE_SECTION_NAVIGATION);
			} else {
				abstractAuditActionDO.setActionType(CandidateActionsEnum.SECTION_NAVIGATION);
			}

			abstractAuditActionDO.setHostAddress(candIPBatchAssessmentCodesDO.getIp());
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
					TimeUtil.DISPLAY_DATE_FORMAT));

			// save to audit log file
			AuditTrailLogger.auditSave(abstractAuditActionDO);
		} else {
			gen_logger.info("Not able to fetch candidate identification number, ip, batchCode and assessmentCode = {}",
					candIPBatchAssessmentCodesDO);
		}
		gen_logger
				.info("Returning getSectionRemainingTimeForCandidate i.e allotedDuration = {} for section = {} for bcaid = {} ",
						remTime, sectionId, batchCandidateAssociationId);
		// if allotted duration is in negative then round of it zero.
		if (remTime < 0) {
			return 0;
		}
		return remTime;
	}

	private long sectionLevelRemainingTimeExtendedMethod(int batchCandidateAssociationId, String sectionId, Calendar currentDateTime,
			long currentSpendTime, long crashTime, AcsBatchCandidateAssociation acsBatchCandidateAssociation,
			AcsBatch batchDetails, AcsCandidateStatus candidateStatus, int hbmTimeInterval,
			AssessmentDurationDO assessmentDuration, Calendar actualSectionStartedTime, long allotedDurationFromBCA,
			long prevLostTime, CandidateReEnableDO candidateReEnableDO) throws GenericDataModelException {
		long remTime = 0;
		// If candidate starting new section, then allot full timing for that section
		if (!sectionId.equals(candidateStatus.getCurrentViewedSection())) {
			gen_logger.info("bcaid={} started New Section :{}", batchCandidateAssociationId, sectionId);
			// removing white spaces as it was not able to convert it to Duration
			String trimmedSection = sectionId.replaceAll("\\s", "");
			if (assessmentDuration.getSectionLevelDuration() != null
					&& !assessmentDuration.getSectionLevelDuration().isEmpty()) {
				for (Map.Entry<String, Integer> entry : assessmentDuration.getSectionLevelDuration().entrySet()) {
					if (entry.getKey().equalsIgnoreCase(trimmedSection)) {
						remTime = entry.getValue().intValue() * 60;
						break;
					}
				}
			} else {
				gen_logger.info("assessmentDuration for SectionLevelDuration is empty, Hence allotedDuration = {}",
						remTime);
			}

			// started new section so PrevLostTime and LastCrashedTime are RESETTED,
			// and updating ActualSectionStartedTime and AllotedDuration with New Section Started time
			candidateStatus.setPrevLostTime(0);
			candidateStatus.setLastCrashedTime(null);
			acsBatchCandidateAssociation.setActualSectionStartedTime(currentDateTime);
			acsBatchCandidateAssociation.setAllotedDuration(remTime);
			// clear re enable on section wise
			if (acsBatchCandidateAssociation.getReenablecandidatejson() != null)
				acsBatchCandidateAssociation.setReenablecandidatejson(null);

			batchCandidateAssociationService.updateSectionTiming(currentDateTime, remTime,
					batchCandidateAssociationId);

			remTime =
					trimDurationOnBatchEndTime(currentDateTime, remTime,
							batchDetails.getMaxDeltaRpackGenerationTime());

			gen_logger.info("Candidate started New Section :{} and allotedDuration={} for bcaid={}", sectionId,
					remTime, batchCandidateAssociationId);
		}
		// If the candidate has crashed in the current section
		else if (sectionId.equals(candidateStatus.getCurrentViewedSection())) {

			if (crashTime > hbmTimeInterval) {

				Calendar lastHeartBeatTime = candidateStatus.getLastHeartBeatTime();
				Calendar prevHeartBeatTime = candidateStatus.getPrevHeartBeatTime();

				gen_logger.info(
						"batchCandidateAssociationId:{} lastHeartBeatTime = {} LastCrashedTime:{} prevLostTime:{} seconds",
						batchCandidateAssociationId, lastHeartBeatTime.getTime(),
						candidateStatus.getLastCrashedTime().getTime(), prevLostTime);
				
				long currentLostTime = 0;

				 gen_logger.info(
						"batchCandidateAssociationId:{} lastHeartBeatTime = {} - LastCrashedTime:{} = currentLostTime:{} "
								+ " seconds and  prevHeartBeatTime = {}",
				 batchCandidateAssociationId, lastHeartBeatTime.getTime(),
				 candidateStatus.getLastCrashedTime().getTime(), currentLostTime,
				 prevHeartBeatTime.getTime());

				if (prevHeartBeatTime.equals(lastHeartBeatTime)) {
					currentLostTime = Math.abs(lastHeartBeatTime.getTimeInMillis()
							- candidateStatus.getLastCrashedTime().getTimeInMillis()) / 1000;
					gen_logger.info(
							"batchCandidateAssociationId:{} prevLostTime = {} + currentLostTime = {} = prevLostTime={} seconds",
							batchCandidateAssociationId, prevLostTime, currentLostTime, prevLostTime + currentLostTime);

					candidateStatus.setPrevLostTime(prevLostTime + currentLostTime);

					// calculate total lost time
					long totalLostTime = candidateStatus.getTotalLostTime();
					totalLostTime += currentLostTime;
					candidateStatus.setTotalLostTime(totalLostTime);

					if (candidateReEnableDO == null) {
						candidateStatus.setPrevLostTime(prevLostTime + currentLostTime);
					} else {
						candidateReEnableDO.setPrevLostTime(prevLostTime + currentLostTime);
						candidateReEnableDO.setTotalLostTime(totalLostTime);
						String candidateReEnableDOJson = gson.toJson(candidateReEnableDO);
						acsBatchCandidateAssociation.setReenablecandidatejson(candidateReEnableDOJson);
					}




				}

				long duration = 0;
				// added code for handling lost time
				boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
				if (isLostTimeAddedAutomatically) {
					long totalLostTimeInCrashes = currentLostTime + prevLostTime;
					gen_logger
							.debug("batchCandidateAssociationId:{} allotted duration:{} currentSpendTime: {} totalLostTimeInCrashes:{} ",
									batchCandidateAssociationId, allotedDurationFromBCA,
									currentSpendTime, totalLostTimeInCrashes);
					duration = (allotedDurationFromBCA
							- Math.abs(currentSpendTime - totalLostTimeInCrashes));
				} else {
					duration = (allotedDurationFromBCA - Math.abs(currentSpendTime));
					getLogger().debug("allotedDuration for bcaId:{} is {} and Not handled Lost Time  ",
							batchCandidateAssociationId, duration);
				}

				if (acsBatchCandidateAssociation.getExtendedExamDurationForSection() != null) {
					String json = acsBatchCandidateAssociation.getExtendedExamDurationForSection();
					Type type = new TypeToken<Map<String, Long>>() {
					}.getType();
					Map<String, Long> map = new Gson().fromJson(json, type);
					long extendedDuration = map.get(sectionId) == null ? 0 : map.get(sectionId);
					duration += (extendedDuration);
					getLogger().debug("allotedDuration for bcaId:{} is {} and Section = {} , extendedDuration = {} ",
							batchCandidateAssociationId, sectionId, extendedDuration);
				}

				getLogger().debug("allotted duration for bcaId:{} before trimming against batch End time: {}",
						batchCandidateAssociationId, duration);

				remTime =
						trimDurationOnBatchEndTime(currentDateTime, duration,
								batchDetails.getMaxDeltaRpackGenerationTime());
				gen_logger.info("allocated duration = {} for batchCandidateAssociation with id = {}", remTime,
						batchCandidateAssociationId);
			} else {
				// case when candidate has crashed in the current section
				// and crashTime is not Greater than Heart Beat Interval(10 secs)
				getLogger().debug("For bcaid:{} Trimming allotted duration:{} based on current spend time:{}",
						batchCandidateAssociationId, allotedDurationFromBCA,
						currentSpendTime);
				remTime = allotedDurationFromBCA - currentSpendTime;
				gen_logger.info("allocated duration = {} for batchCandidateAssociation with id = {}", remTime,
						batchCandidateAssociationId);
			}
			getLogger().debug("allotted duration for bcaId:{} before trimming against batch End time: {}",
					batchCandidateAssociationId, remTime);
		}
		candidateStatus.setLastHeartBeatTime(currentDateTime);
		session.saveOrUpdate(acsBatchCandidateAssociation);
		return remTime;
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
	public int getCorrectNumberOfQuestions(int bcaId) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);
		params.put("questionType", QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString());
		int correctQuestionCount =
				((BigInteger) session.getUniqueResultByNativeSQLQuery(
						ACSQueryConstants.QUERY_CANDIDATERESPONSE_FOR_NO_CORRECT_QUESTION_COUNT, params)).intValue();
		return correctQuestionCount;
	}

	/**
	 * @param loginId
	 * @param batchCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsCandidateStatus getCandidatestatusDetailsForNonEndedCandidates(String loginId, List<String> batchCodes)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_IDS, batchCodes);
		params.put(ACSConstants.USER_NAME, loginId.toUpperCase());

		AcsCandidateStatus candidateStatusTO =
				(AcsCandidateStatus) session
						.getByQuery(
								"SELECT cs FROM BatchCandidateAssociationTO bc,CandidateStatusTO cs where bc.batchId = cs.batchId and bc.candidateId=cs.candidateId and upper(bc.candidateLogin) = (:userName) and bc.batchId IN (:batchIds) and cs.actualTestEndTime is null",
								params);

		return candidateStatusTO;
	}

	/**
	 * @param batchCandidateAssociationForLandingPageCheck
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean isLandingPageRequired(AcsBatchCandidateAssociation batchCandidateAssociation)
			throws GenericDataModelException {

		getLogger().debug("Considering batchCandidateAssociation: {} for LandingPage check", batchCandidateAssociation);
		AcsEvent eventDetailsByBatchCode = bs.getEventDetailsByBatchCode(batchCandidateAssociation.getBatchCode());
		return acsPropertyUtil.isLandingPageRequired(eventDetailsByBatchCode.getEventCode());
	}

	/**
	 * This API is to provide remaining time when the exam is going on
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public long getRemainingTimeForOnGoingExam(int bcaId) throws GenericDataModelException {
		gen_logger.debug("getRemainingTimeAtEndExam is initiated with bcaId: {} ", bcaId);
		boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
		gen_logger.info("isLostTimeAddedAutomatically flag = {} ", isLostTimeAddedAutomatically);
		// get candidate id
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.get(bcaId, AcsBatchCandidateAssociation.class.getName());
		AcsCandidateStatus acscandidatestatus = acsBatchCandidateAssociation.getAcscandidatestatus();
		// calculate remaining time for the candidate
		long remainingTime = 0;
		Calendar lastHeartBeatTime = acscandidatestatus.getLastHeartBeatTime();
		long totalExtendedExamDuration = acsBatchCandidateAssociation.getTotalExtendedExamDuration();
		long prevLostTime = acscandidatestatus.getPrevLostTime();
		Calendar actualTestEndTime = acsBatchCandidateAssociation.getActualTestEndTime();
		Calendar actualTestStartedTime = acsBatchCandidateAssociation.getActualTestStartedTime();
		int candidateId = acsBatchCandidateAssociation.getCandidateId();
		long allotedDuration = acsBatchCandidateAssociation.getAllotedDuration();
		String batchCode = acsBatchCandidateAssociation.getBatchCode();

		if (acsBatchCandidateAssociation.getReenablecandidatejson() != null) {
			// Re birth flow
			CandidateReEnableDO candidateReEnableDO =
					gson.fromJson(acsBatchCandidateAssociation.getReenablecandidatejson(), CandidateReEnableDO.class);
			actualTestStartedTime = candidateReEnableDO.getActualTestStartedTime();
			allotedDuration = candidateReEnableDO.getAllotedDuration();
			prevLostTime = candidateReEnableDO.getPrevLostTime();
		}

		remainingTime = getOnGoingRemainingTimeForCandidate(bcaId, isLostTimeAddedAutomatically, remainingTime,
				lastHeartBeatTime, totalExtendedExamDuration, prevLostTime, actualTestEndTime, actualTestStartedTime,
				candidateId, allotedDuration, batchCode);

		return remainingTime;
	}

	private long getOnGoingRemainingTimeForCandidate(int bcaId, boolean isLostTimeAddedAutomatically,
			long remainingTime,
			Calendar lastHeartBeatTime, long totalExtendedExamDuration, long prevLostTime, Calendar actualTestEndTime,
			Calendar actualTestStartedTime, int candidateId, long allotedDuration, String batchCode)
			throws GenericDataModelException {
		if (actualTestEndTime == null
				&& actualTestStartedTime != null && lastHeartBeatTime != null) {
			Calendar currentTime = Calendar.getInstance();
			long lastHeartBeatTimeInMillis = lastHeartBeatTime.getTimeInMillis();
			long currentTimeInMillis = currentTime.getTimeInMillis();


			int hbmTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
			gen_logger.info("configured heart beat time interval = {}", hbmTimeInterval);

			long inActiveTime = (currentTimeInMillis - lastHeartBeatTimeInMillis);
			gen_logger.info("inActive time for heart beat = {}", inActiveTime);

			if (inActiveTime > hbmTimeInterval) {
				currentTime.setTime(lastHeartBeatTime.getTime());
				gen_logger
						.info("heart beat is elapsed according to the configured value hence, considering the "
								+ " candidateId = {} as crashed and considering lastHeartBeatTime = {} as current time",
								candidateId, lastHeartBeatTime.getTime());
			} else {
				gen_logger
						.info("heart beat is according to the configured value hence, not considering the "
								+ " candidateId = {} as crashed and considering current time = {} as it is",
								candidateId, currentTime.getTime());
			}

			// calculate remaining time and check whether it is non negative if not trim it based on the batch end
			// time else send 0

			remainingTime =
					(allotedDuration - ((currentTime.getTimeInMillis() - actualTestStartedTime.getTimeInMillis()) / 1000));
			gen_logger.info("spent time = {} for candidate with bcaid = {} ", bcaId);

			if (isLostTimeAddedAutomatically) {

				remainingTime = remainingTime + prevLostTime;
				gen_logger.info(
						"automatically adding the lost time = {} for candidate with id = {} in batch with id = {}",
						prevLostTime, candidateId, batchCode);
			} else {
				gen_logger.info("automatic addition of lost time is disabled for batch with id = {} ",
						batchCode);
			}
			// add the previous successful extended time's for the candidate if any

			gen_logger.info(
					"adding previous successful extension time for candidate with id = {} in batch with id = {}",
					totalExtendedExamDuration, candidateId,
					batchCode);
			remainingTime = remainingTime + totalExtendedExamDuration;

			if (remainingTime > 0) {
				gen_logger
						.info("remaining time is greater than 0:{} hence, initiating trimming the duration based on batch end time",
								remainingTime);
				AcsBatch acsBatch = bs.getBatchDetailsByBatchCode(batchCode);
				Calendar maxDeltaRpackGenTime = acsBatch.getDeltaRpackGenerationTime();
				Calendar deltaRpackGenerationTimeByAcs = acsBatch.getDeltaRpackGenerationTimeByAcs();
				if (deltaRpackGenerationTimeByAcs != null && deltaRpackGenerationTimeByAcs.after(maxDeltaRpackGenTime)) {
					maxDeltaRpackGenTime = deltaRpackGenerationTimeByAcs;
				}

				// trim the remaining time based on the batch end time
				long remainingTimeAfterTrim =
						trimDurationOnBatchEndTime(currentTime, remainingTime, maxDeltaRpackGenTime);
				gen_logger.error("Trimming allotted duration  for candidate {} based on batch end time from {} to {}",
						candidateId, remainingTime, remainingTimeAfterTrim);
				remainingTime = remainingTimeAfterTrim;

			} else {
				remainingTime = 0;
			}
			gen_logger.info("remaining time = {} for candidate with id = {}", remainingTime, candidateId);
		}
		return remainingTime;
	}

	/**
	 * @param candidateResponses
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateCandidateScoreForResponses(CandidateResponsesDO candidateResponse)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", candidateResponse.getBcaid());
		params.put("qid", candidateResponse.getQid());
		params.put("score", candidateResponse.getScore());
		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CANDIDATE_SCORE_FOR_RESPONSES, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	/**
	 * @param candidateResponses
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateCandidateScoreForResponses(AcsCandidateResponses candidateResponse)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", candidateResponse.getId().getBcaid());
		params.put("qid", candidateResponse.getId().getQid());
		params.put("score", candidateResponse.getScore());
		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CANDIDATE_SCORE_FOR_RESPONSES, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 
	 * @param batchId
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsCandidateResponses> getCandidateResponsesByBcaIdAndQid(int bcaid, List<Integer> qids)
			throws GenericDataModelException {
		if (qids.isEmpty())
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("qids", qids);
		params.put("bcaid", bcaid);
		List<AcsCandidateResponses> candidateResponses =
				(List<AcsCandidateResponses>) session
						.getResultAsListByQuery(
								"FROM AcsCandidateResponses cr WHERE cr.acsbatchcandidateassociation.bcaid=(:bcaid) and cr.id.qid in (:qids) ",
								params, 0);
		if (candidateResponses.isEmpty()) {
			candidateResponses = null;
		}
		return candidateResponses;
	}

	/**
	 * get Eligible Candidates For ForceSubmit On BcaIds
	 * 
	 * @param bcaIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<ForceSubmitCandidateDO> getEligibleCandidatesForForceSubmit(String eventCode, Integer[] arrayOfBcaIds)
			throws GenericDataModelException {
		getLogger().info(
				"getEligibleCandidatesForForceSubmit called  eventcode= " + eventCode + " bcaIds= " + arrayOfBcaIds);
		HashMap<String, Object> params = new HashMap<String, Object>();

		// get prop value for HEART BEAT TIME INTERVAL FOR FORCE SUBMIT
		AcsProperties acsProperties =
				acsEventRequestsService.getAcsPropertiesOnPropNameAndEventCode(
						EventRule.IDLE_TIME_LIMIT_FOR_FORCE_SUBMIT.toString(), eventCode);
		if (acsProperties != null)
			params.put("intervalTime", acsProperties.getPropertyValue());
		else {
			getLogger().error("Force Submit is not allowed for this Event, because No event rule exists for this");
			throw new GenericDataModelException(ACSExceptionConstants.FORCE_SUBMIT_NOT_ALLOWED,
					"Force Submit is not allowed for this Event");
		}
		if (arrayOfBcaIds != null && arrayOfBcaIds.length != 0) {
			List<Integer> bcaIds = Arrays.asList(arrayOfBcaIds);
			if (bcaIds != null && !bcaIds.isEmpty())
				params.put("bcaids", bcaIds);
		} else {
			getLogger().error("No BcaIds exists");
			throw new GenericDataModelException(ACSExceptionConstants.NO_CANDIDATE_EXISTS, "No BcaIds exists");
		}
		// params.put("batchType", BatchTypeEnum.OPEN.toString());

		// add candidate statuses
		List<Integer> candidateStatus = new ArrayList<>();
		candidateStatus.add(CandidateTestStatusEnum.IN_PROGRESS.ordinal());
		candidateStatus.add(CandidateTestStatusEnum.TEST_COMPLETED.ordinal());
		Calendar currDateTime = Calendar.getInstance();

		params.put("currDateTime",
				TimeUtil.convertTimeAsString(currDateTime.getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS));

		params.put("candidateStatus", candidateStatus);

		String query = ACSQueryConstants.QUERY_FETCH_CANDIDATES_FOR_FORCE_SUBMIT;

		List<ForceSubmitCandidateDO> forceSubmitCandidateDOs =
				session.getResultListByNativeSQLQuery(query, params, 0, 0, ForceSubmitCandidateDO.class);

		getLogger().info("getEligibleCandidatesForForceSubmit returning {}:", forceSubmitCandidateDOs);
		if (forceSubmitCandidateDOs.isEmpty()) {
			forceSubmitCandidateDOs = null;
		}
		return forceSubmitCandidateDOs;
	}

	/**
	 * get InProgress Candidates for batch
	 * 
	 * @param batchCode
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<String> getInProgressCandidates(String batchCode) throws GenericDataModelException {
		getLogger().debug("getting in progress canddiates for batchCode:{}", batchCode);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		List<CandidateTestStatusEnum> candidateStatus = new ArrayList<>();
		candidateStatus.add(CandidateTestStatusEnum.NOT_YET_STARTED);
		candidateStatus.add(CandidateTestStatusEnum.IN_PROGRESS);
		candidateStatus.add(CandidateTestStatusEnum.IDLE);
		params.put("candidateStatus", candidateStatus);
		List<String> candidateList =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_INPROGRESS_CANDIDATES, params);
		getLogger().debug("getting in progress canddiates for batchCode:{}", candidateList);
		if (candidateList.isEmpty())
			candidateList = null;
		return candidateList;

	}

	/**
	 * This provides the candidates who are in-progress, idle, not yet started state
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateIdDO> getEligibleCandidatesForForceSubmit(String batchCode) throws GenericDataModelException {
		getLogger().debug("getting in progress canddiates for batchCode:{}", batchCode);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		List<CandidateTestStatusEnum> candidateStatus = new ArrayList<>();
		candidateStatus.add(CandidateTestStatusEnum.NOT_YET_STARTED);
		candidateStatus.add(CandidateTestStatusEnum.IN_PROGRESS);
		candidateStatus.add(CandidateTestStatusEnum.IDLE);
		params.put("candidateStatus", CandidateTestStatusEnum.ENDED);
		@SuppressWarnings("unchecked")
		List<CandidateIdDO> candidateList =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_INPROGRESS_CANDIDATES_DETAILS, params, 0,
						CandidateIdDO.class);
		getLogger().debug("getting in progress canddiates for batchCode:{}", candidateList);
		if (candidateList.equals(Collections.<Object> emptyList()))
			return null;
		else
			return candidateList;

	}

	/**
	 * updates the current status of the candidate to provided value
	 * 
	 * @param currentStatus
	 * @param csid
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateCandidateCurrentStatus(CandidateTestStatusEnum currentStatus, int csid)
			throws GenericDataModelException {

		gen_logger.debug("Updating the Current status of the candidate with csid:{} to {}", csid, currentStatus);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("csid", csid);
		params.put("currentStatus", currentStatus);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_CURRENT_STATUS, params);
		return count > 0 ? true : false;

	}

	/**
	 * @param candidateActionDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateResourceUsage(CandidateActionDO candidateActionDO) throws GenericDataModelException {

		if (candidateActionDO != null && candidateActionDO.getResourceUsageInfo() != null) {

			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();

			ResourceUsageInfo resourceUsageInfo =
					new Gson().fromJson(candidateActionDO.getResourceUsageInfo(), ResourceUsageInfo.class);

			abstractAuditActionDO.setBatchCandidateAssociation(candidateActionDO.getBatchCandidateAssociationId());
			abstractAuditActionDO.setActionType(CandidateActionsEnum.RESOURCE_STATUS);
			abstractAuditActionDO.setHostAddress(candidateActionDO.getIp());
			abstractAuditActionDO.setResourceUsageAction(resourceUsageInfo.getResourceUsageAction());
			abstractAuditActionDO.setResourceUsageTypeEnum(resourceUsageInfo.getResourceUsageType());
			abstractAuditActionDO.setIdentifier(resourceUsageInfo.getIdentifier());
			abstractAuditActionDO.setBatchCode(candidateActionDO.getBatchCode());
			abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(candidateActionDO.getClientTime(),
					TimeUtil.DISPLAY_DATE_FORMAT));

			// long candRemainingTime = candidateActionDO.getCandRemainingTime();
			// if (candRemainingTime != 0)
			// abstractAuditActionDO.setActionTime(TimeUtil.formatTime(candRemainingTime));

			// save to audit log file
			AuditTrailLogger.auditSave(abstractAuditActionDO);

			gen_logger.info("audited resource usage for candidate with bcaid= {} ",
					candidateActionDO.getBatchCandidateAssociationId());
			// } else {
			// gen_logger.info(
			// "Not able to fetch candidate identification number, ip, batchCode and assessmentCode = {}",
			// candIPBatchAssessmentCodesDO);
			// return false;
			// }
		} else {
			gen_logger.info("Invalid input = {}", candidateActionDO);
			return false;
		}
		AcsCandidateStatus acsCandidateStatus = getCandidateStatus(candidateActionDO.getBatchCandidateAssociationId());
		if (acsCandidateStatus == null)
			return false;
		String resourceusageinfo = candidateActionDO.getResourceUsageInfo();
		// TODO: Audit logging
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("csid", acsCandidateStatus.getCsid());
		params.put("resourceusageinfo", resourceusageinfo);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_RESOURCE_USAGE, params);
		return count > 0 ? true : false;
	}

	public boolean applyCandidateRemoteProctoringStatusByBCAID(List<Integer> batchCandidateAssociationIds,
			boolean status) throws GenericDataModelException {
		gen_logger.info("applyCandidateRemoteProctoringStatusBy BCAIDs= {} and status={} ",
				batchCandidateAssociationIds, status);
		CandidateRemoteProctoringStatus proctorStatus = CandidateRemoteProctoringStatus.UNDEFINED;
		if (status)
			proctorStatus = CandidateRemoteProctoringStatus.ENABLED;
		else
			proctorStatus = CandidateRemoteProctoringStatus.DISABLED;
		return batchCandidateAssociationService.updateCandRemoteProctorStatusByBCAIds(batchCandidateAssociationIds,
				proctorStatus);
	}

	public boolean updateCandidateTPRemoteProctoringId(int batchCandidateAssociationId, String remoteProctoringId)
			throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", batchCandidateAssociationId);
		params.put("candidateRemoteProctoringId", remoteProctoringId);

		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CAND_TP_ID, params);
		if (count == 0)
			return false;
		else
			return true;

	}

	public long getCandidateSpentTime(int bcaId) throws GenericDataModelException {
		AcsBatchCandidateAssociation batchCandidateAssociation =
				this.batchCandidateAssociationService.getBatchCandidateAssociationById(bcaId);
		return this.getCandidateSpentTime(batchCandidateAssociation);
	}

	public long getCandidateSpentTime(AcsBatchCandidateAssociation batchCandidateAssociation)
			throws GenericDataModelException {

		long spentTime = 0;

		AcsCandidateStatus candidateStatus = batchCandidateAssociation.getAcscandidatestatus();
		if (batchCandidateAssociation.getActualTestStartedTime() != null && candidateStatus != null) {
			Calendar actualTestEndTime = batchCandidateAssociation.getActualTestEndTime();
			if (batchCandidateAssociation.getIsForceSubmitted()) {
				actualTestEndTime = candidateStatus.getLastHeartBeatTime();
			}
			if (batchCandidateAssociation.getClearLoginCount() == 0) {
				// Not a crashed scenario.
				spentTime = actualTestEndTime.getTimeInMillis()
						- batchCandidateAssociation.getActualTestStartedTime().getTimeInMillis();
			} else if (batchCandidateAssociation.getClearLoginCount() > 0) {
				// Crashed scenario.
				spentTime = (actualTestEndTime.getTimeInMillis()
						- batchCandidateAssociation.getActualTestStartedTime().getTimeInMillis())
						// as total prev lost time in seconds, hence convert to milis
						- (candidateStatus.getTotalLostTime() * 1000);
			}
		} else {
			gen_logger.info("Candidate hasn't started the test");
		}
		// spent time in seconds
		spentTime = (spentTime / 1000);

		// truncate spent time to assessment duration time if exceeds
		// don't truncate if Extended time from ACS
		spentTime = truncateCandidateSpentTime(batchCandidateAssociation.getAssessmentCode(),
				batchCandidateAssociation.getBatchCode(), spentTime,
				batchCandidateAssociation.getTotalExtendedExamDuration());

		return (spentTime);

	}

	/**
	 * @param candidateDetails
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public long getCandidateSpentTime(com.merittrac.apollo.acs.dataobject.CandidateDetailsDO candidateDetails)
			throws GenericDataModelException {
		long spentTime = 0;
		if (candidateDetails.getActualTestStartedTime() != null) {
			Calendar actualTestEndTime = candidateDetails.getActualTestEndTime();
			if (candidateDetails.isForceSubmitted()) {
				actualTestEndTime = candidateDetails.getLastHeartBeatTime();
			}
			if (candidateDetails.getClearLoginCount() == 0) {
				// Not a crashed scenario.
				spentTime = actualTestEndTime.getTimeInMillis()
						- candidateDetails.getActualTestStartedTime().getTimeInMillis();
			} else if (candidateDetails.getClearLoginCount() > 0) {
				// Crashed scenario.
				spentTime = (actualTestEndTime.getTimeInMillis()
						- candidateDetails.getActualTestStartedTime().getTimeInMillis())
						// as total prev lost time in seconds, hence convert to milis
						- (candidateDetails.getTotalLostTime() * 1000);
			}
		} else {
			gen_logger.info("Candidate hasn't started the test");
		}
		// spent time in seconds
		spentTime = (spentTime / 1000);

		// truncate spent time to assessment duration time if exceeds
		// don't truncate if Extended time from ACS
		spentTime = truncateCandidateSpentTime(candidateDetails.getAssessmentCode(), candidateDetails.getBatchCode(),
				spentTime, candidateDetails.getTotalExtendedExamDuration());

		return (spentTime);

	}

	/**
	 * truncate spent time to assessment duration time if exceeds
	 * 
	 * @param candidateDetails
	 * @param spentTime
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private long truncateCandidateSpentTime(String assessmentCode, String batchCode, long spentTime, long extendedTime)
			throws GenericDataModelException {
		// truncate spent time to assessment duration time if exceeds
		AcsBussinessRulesAndLayoutRules brlr =
				cdeas.getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(assessmentCode, batchCode);
		// get section duration
		String sectionDuration = brlr.getAssessmentDuration().replaceAll("\\s", "");
		AssessmentDurationDO assessmentDurationDO = new Gson().fromJson(sectionDuration, AssessmentDurationDO.class);
		// convert assessmentDuration mins to secs
		int assessmentDurationInSecs = (assessmentDurationDO.getAssessmentDuration() * 60);

		if (extendedTime > 0)
			assessmentDurationInSecs += (extendedTime);

		// if exceeds, truncate
		if (spentTime > assessmentDurationInSecs)
			spentTime = assessmentDurationInSecs;

		return spentTime;
	}
	
	public boolean updateCandLastCrashTime(int batchCandidateAssociationId, Calendar lastCrashedTime) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("bcaid", batchCandidateAssociationId);
		params.put("lastCrashedTime", lastCrashedTime);
		int value=session.updateByQuery( ACSQueryConstants.QUERY_UPDATE_CAND_LAST_CRASH_TIME, params);
		if(value>0)
			return true;
		else
			return false;
	}
}