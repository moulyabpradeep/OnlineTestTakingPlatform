package com.merittrac.apollo.acs.services;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.constants.CandidateFeedBackStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateTestStatusEnum;
import com.merittrac.apollo.acs.dataobject.tp.AbstractAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateResourcesAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.GeneralAuditDO;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.BrLrUtility;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.entities.acs.FeedBackResponse;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Scope of this class is to Store candidate FeedBack
 * 
 * @author V_Praveen - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class FeedbackFormService extends BasicService {
	private static Logger gen_logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static CDEAService cdeas = null;
	private static CandidateService cs = null;
	private static BatchService bs = null;
	private static FeedbackFormService feedbackFormService = null;
	private static BatchCandidateAssociationService batchCandidateAssociationService = null;

	static {
		cdeas = new CDEAService();
		cs = CandidateService.getInstance();
		bs = BatchService.getInstance();
		batchCandidateAssociationService = new BatchCandidateAssociationService();
	}

	public FeedbackFormService() {

	}

	/**
	 * Using double check singleton pattern
	 * 
	 * @return instance of batch service
	 * 
	 * @since Apollo v2.0
	 */
	public static final FeedbackFormService getInstance() {
		if (feedbackFormService == null) {
			synchronized (FeedbackFormService.class) {
				if (feedbackFormService == null) {
					feedbackFormService = new FeedbackFormService();
				}
			}
		}
		return feedbackFormService;
	}

	/**
	 * saveCandidateFeedBackData API used to save the candidate feedback data into DB
	 * 
	 * @param feedBackDataJson
	 * @param candID
	 * @param batchID
	 * @param forceSubmit
	 * @param clientTime
	 * @param finalSubmit
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean saveCandidateFeedBackData(String feedBackDataJson, int candID, String batchCode,
			boolean forceSubmit, long clientTime, boolean finalSubmit, long actionTimeTP, int bcaId)
			throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger
				.debug("saveCandidateFeedBackData API initiated successfully for candidateId ={} and batchId ={} with feedBackJson ={} and forceSubmit value={} and finalSubmit value={}",
						candID, batchCode, feedBackDataJson, forceSubmit, finalSubmit);

		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(bcaId);
		AcsCandidateStatus acsCandidateStatus = acsBatchCandidateAssociation.getAcscandidatestatus();

		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		abstractAuditActionDO.setBatchCandidateAssociation(acsBatchCandidateAssociation.getBcaid());
		// HashMap<String, Object> params = new HashMap<String, Object>();
		// params.put("candidateID", candID);
		// params.put("batchId", batchCode);
		// params.put("feedBackFormData", feedBackDataJson);
		//
		// String query = ACSQueryConstants.QUERY_UPDATE_CAND_FEEDBACK_DATA;

		acsBatchCandidateAssociation.setFeedBackFormData(feedBackDataJson);

		if (forceSubmit) {
			// added for heartbeat updating
			// if (CandidateService.candidateData.containsKey(candID)) {
			// CandidateStatusDO cao = CandidateService.candidateData.get(candID);
			// CandidateService.candidateData.remove(candID);
			// }
			// // end here

			// query = ACSQueryConstants.QUERY_UPDATE_CAND_FEEDBACK_DATA_AND_CURRENT_STATUS;
			// params.put("currentStatus", CandidateTestStatusEnum.ENDED);
			// params.put("feedBackStatus", CandidateFeedBackStatusEnum.FORCE_SUBMITTED_BY_TP);

			acsBatchCandidateAssociation.setFeedBackStatus(CandidateFeedBackStatusEnum.FORCE_SUBMITTED_BY_TP);
			acsCandidateStatus.setCurrentStatus(CandidateTestStatusEnum.ENDED);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FEEDBACK_FORCE_SUBMITTED_BY_TP);

		} else if (finalSubmit) {

			// added for heartbeat updating
			// if (CandidateService.candidateData.containsKey(candID)) {
			// CandidateStatusDO cao = CandidateService.candidateData.get(candID);
			// CandidateService.candidateData.remove(candID);
			// }
			// // end here

			// query = ACSQueryConstants.QUERY_UPDATE_CAND_FEEDBACK_DATA_AND_CURRENT_STATUS;
			// params.put("currentStatus", CandidateTestStatusEnum.ENDED);
			// params.put("feedBackStatus", CandidateFeedBackStatusEnum.SUBMITTED);

			acsBatchCandidateAssociation.setFeedBackStatus(CandidateFeedBackStatusEnum.SUBMITTED);
			acsCandidateStatus.setCurrentStatus(CandidateTestStatusEnum.ENDED);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FEEDBACK_SUBMITTED);
		} else {
			// params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);

			acsBatchCandidateAssociation.setFeedBackStatus(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
			abstractAuditActionDO.setActionType(CandidateActionsEnum.FEEDBACK_AUTO_SAVED_BY_TP);
		}

		/**
		 * Update AcsBatchCandidateAssociation.
		 */
		// acsBatchCandidateAssociation.setAcscandidatestatus(acsCandidateStatus);
		session.merge(acsCandidateStatus);
		batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(acsBatchCandidateAssociation);
		// added code for audit trail
		abstractAuditActionDO.setActionTime(TimeUtil.formatTime(actionTimeTP));

		abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		String assessmentTypeValue = acsBatchCandidateAssociation.getAssessmentCode();
		abstractAuditActionDO.setAssessmentID(assessmentTypeValue);
		abstractAuditActionDO.setCandID(candID);
		abstractAuditActionDO.setCandApplicationNumber(cs.getApplicationNumberByCandidateId(candID));
		abstractAuditActionDO.setBatchCode(batchCode);
		abstractAuditActionDO.setHostAddress(acsBatchCandidateAssociation.getHostAddress());

		AuditTrailLogger.auditSave(abstractAuditActionDO);
		// ends here

		// int count = session.updateByQuery(query, params);
		// if (count == 0) {
		// gen_logger
		// .debug("saveCandidateFeedBackData API not able to update for candidateId ={} and batchId ={} with feedBackJson ={} and forceSubmit value={} and finalSubmit value={}",
		// candID, batchCode, feedBackDataJson, forceSubmit, finalSubmit);
		// return false;
		// }

		gen_logger
				.debug("saveCandidateFeedBackData API updated successfully for candidateId ={} and batchId ={} with feedBackJson ={} and forceSubmit value={} and finalSubmit value={}",
						candID, batchCode, feedBackDataJson, forceSubmit, finalSubmit);
		StatsCollector.getInstance().log(startTime, "saveCandidateFeedBackData", "FeedbackFormService", candID);
		return true;

	}

	/**
	 * checkFeedBackAllowedByBatchId API used to check weather feedBack form allowed or not by batchId.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean checkFeedBackAllowedByBatchId(String batchCode) throws GenericDataModelException {
		gen_logger.info("checkFeedBackAllowedByBatchId API initiated for a batch={}", batchCode);
		Object value = null;
		List<String> assessmentCodesByBatchCode = cdeas.getAssessmentIdsByBatchId(batchCode);
		// BatchDetailsTO batchDetails = (BatchDetailsTO) bs.session.get(batchId,
		// BatchDetailsTO.class.getCanonicalName());
		gen_logger.info("Recieved assessments: {} for batchId={} ", assessmentCodesByBatchCode, batchCode);
		for (String assessmentCode : assessmentCodesByBatchCode) {
			gen_logger.info("bussiness rule for feedBack = {} for a batchId={} of AssessmentID={}", batchCode,
					assessmentCode);

			AcsBussinessRulesAndLayoutRules brlrRules =
					cdeas.getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(batchCode, assessmentCode);
			value = BrLrUtility.getLrRule(brlrRules.getLrRules(), ACSConstants.FEED_BACK_ALLOWED);

			if (value != null && value.equals(Boolean.TRUE)) {
				break;
			}
		}
		gen_logger.info("bussiness rule for feedBack = {} for a batchId={}", value, batchCode);
		if (value != null && value.equals(Boolean.TRUE)) {
			return (boolean) value;
		}

		return false;
	}

	/**
	 * checkFeedBackAllowedByBatchIdCandId API used to check weather feedBack form allowed or not by batchId and candId.
	 * 
	 * @param batchId
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean checkFeedBackAllowedByBatchIdCandId(String batchCode, int candId) throws GenericDataModelException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		gen_logger.info("checkFeedBackAllowedByBatchIdCandId API initiated for a candId={} of batch={}", candId,
				batchCode);
		Object value = cdeas.getLayoutRulePropertybyCandIDAndBatchID(candId, batchCode, ACSConstants.FEED_BACK_ALLOWED);

		gen_logger.info("bussiness rule for feedBack = {} for a candid={} of batchId={}", value, candId, batchCode);
		if (value != null && value.equals(Boolean.TRUE)) {
			statsCollector.log(startTime, "checkFeedBackAllowedByBatchIdCandId", "FeedbackFormService", candId);
			return (boolean) value;
		}
		statsCollector.log(startTime, "checkFeedBackAllowedByBatchIdCandId", "FeedbackFormService", candId);
		return false;

	}

	/**
	 * getFeedBackFormTimerValueByCandIdBatchId API used to get feedback Timer value.
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */

	public int getFeedBackFormTimerValueByCandIdBatchId(int candId, String batchCode) throws GenericDataModelException {
		gen_logger.info("getFeedBackFormTimerValueByCandIdBatchId API initiated for a candId={} of batch={}", candId,
				batchCode);
		boolean isFeedBackExists = checkFeedBackAllowedByBatchIdCandId(batchCode, candId);
		if (!isFeedBackExists) {
			return 0;
		}
		Double feedBackTimerValue =
				(Double) cdeas.getLayoutRulePropertybyCandIDAndBatchID(candId, batchCode, ACSConstants.FEED_BACK_TIMER);
		gen_logger.info("feedBack Timer value={} for a candidateId ={} of batchId={} ", feedBackTimerValue, candId,
				batchCode);
		if (feedBackTimerValue != null) {
			return feedBackTimerValue.intValue();

		}
		return 0;

	}

	/**
	 * getFeedBackResponseByCandIdBatchId API used to get the feedBack data from DB by candidateId and batchId.
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getFeedBackResponseByCandIdBatchId(int bcaId, String batchCode)
			throws GenericDataModelException {
		gen_logger
				.info("getFeedBackResponseByCandIdBatchId API initiated for batch candidate association id ={} and batch code={}",
						bcaId, batchCode);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaId);
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_APPLICABLE);
		String feedBackDataJson =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_FEEDBACK_DATA_BY_CANDBATCHASSOCIATIONID,
						params);
		gen_logger.info("feedBackData json ={} for batch candidate association id ={} and batch code ={} ",
				feedBackDataJson, bcaId, batchCode);
		// if (feedBackDataJson != null) {
		// return formatFeedbackJson(feedBackDataJson);
		// } else

		// => change feedback format, as rpack generation is failing coz of that
		return feedBackDataJson;

	}

	private List<FeedBackResponse> formatFeedbackJson(String feedBackDataJson) {
		try {
			Type type = new TypeToken<List<FeedBackResponse>>() {
			}.getType();
			List<FeedBackResponse> feedBackResponse = new Gson().fromJson(feedBackDataJson, type);
			return feedBackResponse;
		} catch (IllegalStateException ex) {
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();
			HashMap<String, String> feedBackResponse = new Gson().fromJson(feedBackDataJson, type);
			return null;// feedBackResponse;
		}
	}

	/**
	 * getFeedBackFormTimerValueFromJsonByCandIdBatchId API used to get feedback Timer value. value.
	 * 
	 * @param candId
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public long getFeedBackFormTimerValueFromJsonByCandIdBatchId(int candId, String batchCode)
			throws GenericDataModelException {
		gen_logger.info("getFeedBackFormTimerValueByCandIdBatchId API initiated for a candId={} of batch={}", candId,
				batchCode);
		boolean isFeedBackExists = checkFeedBackAllowedByBatchIdCandId(batchCode, candId);
		gen_logger.info("isFeedBackExists value for a candId={} of batch={}", candId, batchCode);
		if (!isFeedBackExists) {
			return 0;
		}
		String feedBackFormJson =
				(String) cdeas.getLayoutRulePropertybyCandIDAndBatchID(candId, batchCode,
						ACSConstants.FEED_BACK_FORM_JSON);
		if (feedBackFormJson == null) {
			return 0;
		}

		Type mapType = new TypeToken<Map<String, Object>>() {
		}.getType();
		Map<String, Object> feedBackFormJsonMap = new Gson().fromJson(feedBackFormJson, mapType);
		Double feedBackTimerValue = null;
		if (feedBackFormJsonMap.containsKey(ACSConstants.FEED_BACK_TIMER)) {
			feedBackTimerValue = Double.parseDouble((String) feedBackFormJsonMap.get(ACSConstants.FEED_BACK_TIMER));
		}

		gen_logger.info("feedBack Timer value={} for a candidateId ={} of batchId={} ", feedBackTimerValue, candId,
				batchCode);
		if (feedBackTimerValue != null) {
			long feedBackTimerInSeconds = TimeUnit.MINUTES.toSeconds(feedBackTimerValue.longValue());
			gen_logger.info("feedBack Timer value in seconds ={} for a candidateId ={} of batchId={} ",
					feedBackTimerInSeconds, candId, batchCode);
			return feedBackTimerInSeconds;
		}
		return 0;

	}

	/**
	 * auditCandidateEventsDuringFeedBack API used to audit feedBack events.
	 * 
	 * @param candidateResourcesAuditDO
	 * @param isFeedBackExitEvent
	 * @param isFeedBackCancelEvent
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean auditCandidateEventsDuringFeedBack(CandidateResourcesAuditDO candidateResourcesAuditDO,
			boolean isFeedBackExitEvent, boolean isFeedBackCancelEvent) throws GenericDataModelException {
		long startTime = StatsCollector.getInstance().begin();
		gen_logger.info("input from Tp for auditing resource and pop ups clicks = {}", candidateResourcesAuditDO);
		if (candidateResourcesAuditDO != null && candidateResourcesAuditDO.getActionType() != null) {
			// fetch the necessary information for audit trail i.e candidate identifier, batch code, assessment code and
			// test player ip
			// HashMap<String, Object> params = new HashMap<String, Object>();
			// params.put(ACSConstants.BATCH_ID, candidateResourcesAuditDO.getBatchID());
			// params.put(ACSConstants.CAND_ID, candidateResourcesAuditDO.getCandID());
			// params.put("currentStatus", CandidateTestStatusEnum.ENDED);

			// CandIPBatchAssessmentCodesDO candIPBatchAssessmentCodesDO =
			// (CandIPBatchAssessmentCodesDO) session
			// .getByQuery(
			// "select b.batchCode as batchCode, ad.assessmentCode as assessmentCode, cs.hostAddress as ip, c.identifier1 as candIdentifier FROM CandidateStatusTO cs,BatchCandidateAssociationTO bca,AssessmentDetailsTO ad,BatchDetailsTO b,CandidateDetailsTO cd join cd.candidate c WHERE cs.currentStatus!=(:currentStatus) and cs.batchId=(:batchId) and cs.candidateId=(:candId) and bca.batchId=cs.batchId and bca.candidateId=cs.candidateId and bca.batchId=b.batchId and bca.assessmentId = ad.assessmentId and bca.candidateId = c.candidateId",
			// params, CandIPBatchAssessmentCodesDO.class);
			// gen_logger
			// .info("fetched required information for auditing resource usage and pop up acceptance = {} based on candId = {} and batchId = {}",
			// candIPBatchAssessmentCodesDO, candidateResourcesAuditDO.getCandID(),
			// candidateResourcesAuditDO.getBatchID());
			// if (candIPBatchAssessmentCodesDO != null) {
			// form the audit object

			// changes done to update the feedBack status to DB
			// if (isFeedBackCancelEvent) {
			// // added for player heartbeat updating
			// if (CandidateService.candidateData.containsKey(candidateResourcesAuditDO.getCandID())) {
			// CandidateStatusDO cao = CandidateService.candidateData.get(candidateResourcesAuditDO.getCandID());
			// CandidateService.candidateData.remove(candidateResourcesAuditDO.getCandID());
			// }
			// // end here
			// }

			if (isFeedBackCancelEvent || isFeedBackExitEvent
					|| candidateResourcesAuditDO.getActionType().equals(CandidateActionsEnum.AUTO_REDIRECT)) {

				batchCandidateAssociationService.updateFeedbackDetails(candidateResourcesAuditDO.getBcaId(), null,
						CandidateFeedBackStatusEnum.FEEDBACK_NOT_CHOOSEN);
				cs.updateCandidateCurrentStatus(CandidateTestStatusEnum.ENDED, candidateResourcesAuditDO.getBcaId());

				// AcsCandidateStatus acsCandidateStatus = cs.getCandidateStatus(candidateResourcesAuditDO.getBcaId());
				// acsCandidateStatus.setPlayerStatus(HBMStatusEnum.GREEN);
				// acsCandidateStatus.setCurrentStatus(CandidateTestStatusEnum.ENDED);
				// session.merge(acsCandidateStatus);
				// acsBatchCandidateAssociation.setAcscandidatestatus(acsCandidateStatus);
				gen_logger.info("feedBack status updated successfully  for candId ={} of batchId={}",
						candidateResourcesAuditDO.getCandID(), candidateResourcesAuditDO.getBatchCode());

				// HashMap<String, Object> params = new HashMap<String, Object>();
				// params.put(ACSConstants.BATCH_ID, candidateResourcesAuditDO.getBatchCode());
				// params.put(ACSConstants.CAND_ID, candidateResourcesAuditDO.getCandID());
				// params.put("feedBackStatus", CandidateFeedBackStatusEnum.FEEDBACK_NOT_CHOOSEN);
				// params.put("currentStatus", CandidateTestStatusEnum.ENDED);
				// params.put("playerStatus", HBMStatusEnum.GREEN);
				// params.put("feedBackFormData", null);
				// String query = ACSQueryConstants.QUERY_UPDATE_CAND_FEEDBACK_AND_CURRENT_STATUS_;
				// int count = session.updateByQuery(query, params);
				// if (count == 0) {
				// gen_logger.info("feedBack status not updated for candId ={} of batchId={}",
				// candidateResourcesAuditDO.getCandID(), candidateResourcesAuditDO.getBatchCode());
				// } else
				// gen_logger.info("feedBack status updated successfully  for candId ={} of batchId={}",
				// candidateResourcesAuditDO.getCandID(), candidateResourcesAuditDO.getBatchCode());
			}

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
		} else {
			gen_logger.info("Invalid input = {}", candidateResourcesAuditDO);
			return false;
		}
		StatsCollector.getInstance().log(startTime, "auditCandidateEventsDuringFeedBack", "FeedbackFormService",
				candidateResourcesAuditDO.getCandID());
		return true;
	}
}
