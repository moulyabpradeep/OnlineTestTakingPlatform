package com.merittrac.apollo.acs.services;

import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.constants.CandidateTestStatusEnum;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.CandidateReEnableDO;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.AbstractAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.GeneralAuditDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.common.entities.acs.ExtensionTypeEnum;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateInfoBean;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateInfoRequestBean;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateLateLoginTimeExtensionDO;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.RequestStatus;
import com.merittrac.apollo.common.exception.HttpClientException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Contain api's related to late login time extension at candidate level
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class CandidateLateLoginTimeExtensionService extends BasicService implements ICandidateLateLoginTimeExtension {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static BatchCandidateAssociationService batchCandidateAssociationService = null;
	static CustomerBatchService customerBatchService = null;
	static CDEAService cdeaService = null;
	static CandidateService candidateService = null;
	static BatchService batchService = null;
	static AuthService authService = null;
	private static String isAuditEnable = null;
	private static CandidateLateLoginTimeExtensionService candidateLateLoginTimeExtensionService = null;
	private static ACSService acss = null;
	private static Gson gson = null;

	static {
		batchCandidateAssociationService = new BatchCandidateAssociationService();
		customerBatchService = new CustomerBatchService();
		cdeaService = new CDEAService();
		authService = new AuthService();
		candidateService = CandidateService.getInstance();
		acss = ACSService.getInstance();
		gson = new Gson();
		batchService = BatchService.getInstance();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
	}

	public static Logger getLogger() {
		return logger;
	}

	/**
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final CandidateLateLoginTimeExtensionService getInstance() {
		if (candidateLateLoginTimeExtensionService == null) {
			synchronized (CandidateLateLoginTimeExtensionService.class) {
				if (candidateLateLoginTimeExtensionService == null) {
					candidateLateLoginTimeExtensionService = new CandidateLateLoginTimeExtensionService();
				}
			}
		}
		return candidateLateLoginTimeExtensionService;
	}

	/**
	 * extends the late login time for a candidate.This API is wrapper to fecilitate the Call from PHP
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 */
	public List<CandidateLateLoginTimeExtensionDO> extendCandidateLateLoginTime(
			List<CandidateLateLoginTimeExtensionDO> candidateLateLoginTimeExtensionDOs, String userName,
			String ipAddress) throws GenericDataModelException, CandidateRejectedException {
		return extendCandidateLateLoginTime(candidateLateLoginTimeExtensionDOs, userName, ipAddress, false);
	}

	/**
	 * extends the late login time for a candidate
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @param isRequestFromDX
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 */
	public List<CandidateLateLoginTimeExtensionDO> extendCandidateLateLoginTime(
			List<CandidateLateLoginTimeExtensionDO> candidateLateLoginTimeExtensionDOs, String userName,
			String ipAddress, boolean isRequestFromDX) throws GenericDataModelException, CandidateRejectedException {
		logger.info("initiated extendCandidateLateLoginTime where candidateLateLoginTimeExtensionDOs= {}",
				candidateLateLoginTimeExtensionDOs.toString());
		List<CandidateLateLoginTimeExtensionDO> returningCandidateLateLoginTimeExtensionDOs = new ArrayList<>();
		Map<String, AcsBatch> batchDetails = new HashMap<String, AcsBatch>();
		Map<String, AcsBatch> batchDetailsByBatchCode = new HashMap<String, AcsBatch>();
		Map<String, Calendar> batchLevelMaxLateLoginTimes = new HashMap<String, Calendar>();
		Map<String, Integer> lateLoginTimePerBatchMap = new HashMap<String, Integer>();

		// iterate over each CandidateLateLoginTimeExtensionDO object
		for (Object candidateLateLoginTimeExtensionDOObject : candidateLateLoginTimeExtensionDOs) {
			CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO =
					(CandidateLateLoginTimeExtensionDO) candidateLateLoginTimeExtensionDOObject;
			candidateLateLoginTimeExtensionDO.setExtensionType(ExtensionTypeEnum.CANDIDATE_LATE_LOGIN_EXTENSION);
			// handling invalid input
			if (candidateLateLoginTimeExtensionDO.getExtendedLateLoginTimeInMins() <= 0) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO
						.setErrorMessage("Invalid input (value greater than 0 is expected for late login time extension)");
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}

			AcsBatch batch = null;
			Calendar startTime = null;
			Calendar candidateBatchEndTime = null;
			// get the batch information
			if (!isRequestFromDX) {
				String batchCode = candidateLateLoginTimeExtensionDO.getBatchCode();
				// fetch candidate details
				if (!batchDetails.containsKey(batchCode)) {
					// fetch assessment details by assessmnetId
					batch = (AcsBatch) batchService.get(batchCode, AcsBatch.class.getCanonicalName());
					// cache it in a map
					batchDetails.put(batchCode, batch);
				} else {
					batch = batchDetails.get(batchCode);
				}
				logger.info("batchdetails = {} for batch with code = {}", batch, batchCode);

				AcsBatchCandidateAssociation batchCandidateAssociationTO =
						batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandIdentifier(
								batch.getBatchCode(), candidateLateLoginTimeExtensionDO.getIdentifier1());
				if (batchCandidateAssociationTO == null) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Batch-Candidate assosication not present. Please check pack activation status");
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
				startTime = authService.getMaxCandidateBatchStartTime(
						batchCandidateAssociationTO.getExtendedBatchStartTimePerCandidate(),
						batch.getMaxBatchStartTime());
				candidateLateLoginTimeExtensionDO
						.setBatchCandidateAssociationId(batchCandidateAssociationTO.getBcaid());
			} else {
				// If the request is from DX, batchID will not be present.
				String batchCode = candidateLateLoginTimeExtensionDO.getBatchCode();
				// fetch candidate details
				if (!batchDetailsByBatchCode.containsKey(batchCode)) {
					// fetch assessment details by assessmnetId
					batch = batchService.getBatchDetailsByBatchCode(batchCode);
					// cache it in a map
					batchDetailsByBatchCode.put(batchCode, batch);
				} else {
					batch = batchDetailsByBatchCode.get(batchCode);
				}
				candidateLateLoginTimeExtensionDO.setBatchName(batch.getBatchName());

				if (!candidateService.isCandidateExists(candidateLateLoginTimeExtensionDO.getIdentifier1())) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("No candidate exists with the specified identifier : "
									+ candidateLateLoginTimeExtensionDO.getIdentifier1());
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}

				// get assessment and current late login info as it is not provided by DX
				AcsBatchCandidateAssociation batchCandidateAssociationTO =
						batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandIdentifier(
								batch.getBatchCode(), candidateLateLoginTimeExtensionDO.getIdentifier1());
				if (batchCandidateAssociationTO == null) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Batch-Candidate assosication not present. Please check pack activation status");
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
				String assessmentCode = batchCandidateAssociationTO.getAssessmentCode();
				candidateBatchEndTime = batchCandidateAssociationTO.getExtendedBatchEndTimePerCandidate();
				candidateLateLoginTimeExtensionDO.setAssessmentCode(assessmentCode);
				candidateLateLoginTimeExtensionDO.setCandidateId(batchCandidateAssociationTO.getCandidateId());
				candidateLateLoginTimeExtensionDO.setBatchCode(batch.getBatchCode());
				candidateLateLoginTimeExtensionDO
						.setBatchCandidateAssociationId(batchCandidateAssociationTO.getBcaid());
				// fetch assessment details
				if (!lateLoginTimePerBatchMap.containsKey(assessmentCode)) {
					// fetch assessment details by assessmnetId
					AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
							cdeaService.getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(batch.getBatchCode(),
									assessmentCode);
					if (bussinessRulesAndLayoutRulesTO != null) {
						// cache it in a map
						lateLoginTimePerBatchMap.put(assessmentCode, bussinessRulesAndLayoutRulesTO.getLateLoginTime());
					} else {
						// cache it in a map
						lateLoginTimePerBatchMap.put(assessmentCode, 0);
					}
				}

				// get the assessment details
				int lateLoginTime = lateLoginTimePerBatchMap.get(assessmentCode);
				logger.info("lateLoginTime = {} for assessment with id = {}", lateLoginTime, assessmentCode);

				startTime = authService.getMaxCandidateBatchStartTime(
						batchCandidateAssociationTO.getExtendedBatchStartTimePerCandidate(),
						batch.getMaxBatchStartTime());
				// calculate the current late login time
				Calendar currentLateLoginTime =
						customerBatchService.getMaxLateLoginTime(
								batchCandidateAssociationTO.getExtendedLateLoginTimePerCandidate(), lateLoginTime,
								startTime, batch.getExtendedLateLoginTime());

				String currentLateLoginTimeinString =
						TimeUtil.convertTimeAsString(currentLateLoginTime.getTimeInMillis(),
								TimeUtil.DISPLAY_DATE_FORMAT);
				candidateLateLoginTimeExtensionDO.setCurrentLateLoginTime(currentLateLoginTimeinString);
				Calendar proposedLateLoginTime = Calendar.getInstance();
				proposedLateLoginTime.setTimeInMillis(startTime.getTimeInMillis());
				proposedLateLoginTime.add(Calendar.MINUTE,
						candidateLateLoginTimeExtensionDO.getExtendedLateLoginTimeInMins());
				if (proposedLateLoginTime.before(currentLateLoginTime)) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO.setErrorMessage("Proposed late login time "
							+ TimeUtil.convertTimeAsString(proposedLateLoginTime.getTimeInMillis())
							+ " is less than the existing late login time : "
							+ TimeUtil.convertTimeAsString(currentLateLoginTime.getTimeInMillis()));
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;

				}

			}

			// check whether candidate is already logged-in for 1st time
			// fetch candidate status details
			AcsCandidateStatus cs =
					candidateService.getCandidateStatus(candidateLateLoginTimeExtensionDO
							.getBatchCandidateAssociationId());
			if (cs != null) {
				String msg = "Candidate Already logged-in, hence skipping lateLoginExtension";
				logger.info(msg
						+ " for this candidate, candidate status details = {} where candidateId = {} and batchId = {}",
						cs, candidateLateLoginTimeExtensionDO.getCandidateId(),
						candidateLateLoginTimeExtensionDO.getBatchCode());
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO.setErrorMessage(msg);
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			// calculate end time for login based on extended time in mins
			// Sample date From DX = 11-Aug-2015 11:15:00
			// Sample date From UI = 2015-08-10 16:00:00
			// Date format changed
			Calendar endTimeForLogin = null;
			if (isRequestFromDX) {
				endTimeForLogin =
						TimeUtil.convertStringToCalender(candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime());
			} else {
				endTimeForLogin =
						TimeUtil.convertStringToCalender(candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime(),
								TimeUtil.YYYY_MM_DD_HH_MM_SS);
			}
			if (endTimeForLogin == null) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO
						.setErrorMessage("unable to convert current late login time to calender object");
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}

			if (isRequestFromDX) {
				endTimeForLogin = (Calendar) startTime.clone();
			}
			endTimeForLogin.add(Calendar.MINUTE, candidateLateLoginTimeExtensionDO.getExtendedLateLoginTimeInMins());

			// check whether extended late login time is within current time or not
			if (endTimeForLogin.before(Calendar.getInstance())) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO.setErrorMessage("Extended late login time is elapsed : "
						+ TimeUtil.convertTimeAsString(endTimeForLogin.getTimeInMillis()));
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}

			// check whether new late login time is within batch end time or not
			if (candidateBatchEndTime == null) {
				if (batch.getMaxBatchEndTime().before(Calendar.getInstance())) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Late login time extension not allowed as batch end time is elapsed : "
									+ TimeUtil.convertTimeAsString(batch.getMaxBatchEndTime().getTimeInMillis()));
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
			} else {
				if (candidateBatchEndTime.before(endTimeForLogin)) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Late login time extension not allowed beyond candidate batch end time : "
									+ TimeUtil.convertTimeAsString(candidateBatchEndTime.getTimeInMillis()));
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
			}

			// check whether new late login time is within delat rpack generation time or not
			if (batch.getMaxDeltaRpackGenerationTime().before(endTimeForLogin)) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO
						.setErrorMessage("Late login time extension not allowed beyond delta rpack generation time : "
								+ TimeUtil
										.convertTimeAsString(batch.getMaxDeltaRpackGenerationTime().getTimeInMillis()));
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
				// throw new CandidateRejectedException(ACSExceptionConstants.CAND_LATE_LOGIN_EXTENSION_NOT_ALLOWED,
				// "Late login time extension not allowed beyond delta rpack generation time");
			}

			String currentLateLoginTime =
					TimeUtil.convertTimeAsString(endTimeForLogin.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);

			IncidentAuditLogEnum incidentAuditLogEnum =
					(isRequestFromDX) ? IncidentAuditLogEnum.CANDIDATE_LEVEL_LATE_LOGIN_EXTENSION_BY_DX
							: IncidentAuditLogEnum.CANDIDATE_LEVEL_LATE_LOGIN_EXTENSION;
			// audit in incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(batch.getBatchCode(), candidateLateLoginTimeExtensionDO.getIdentifier1(),
							ipAddress, TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
									TimeUtil.DISPLAY_DATE_FORMAT), incidentAuditLogEnum);
			incidentAuditAction.setPrevLateLoginTime(candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime());
			incidentAuditAction.setCurrentLateLoginTime(currentLateLoginTime);
			AuditTrailLogger.incidentAudit(incidentAuditAction);

			// admin audit
			if (isAuditEnable != null && isAuditEnable.equals("true") && !isRequestFromDX) {
				Object[] paramArray =
						{ candidateLateLoginTimeExtensionDO.getIdentifier1(), batch.getBatchCode(),
								candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime(),
								TimeUtil.convertTimeAsString(endTimeForLogin.getTimeInMillis()) };

				HashMap<String, String> logbackParams = new HashMap<String, String>();
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
						AdminAuditActionEnum.CANDIDATE_LATE_LOGIN_EXTENSION.toString());
				logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");

				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_CANDIDATE_LEVEL_LATE_LOGIN_EXTENSION_MSG, paramArray);
			}

			// candidate audit trail
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setCandID(candidateLateLoginTimeExtensionDO.getCandidateId());
			abstractAuditActionDO.setBatchCandidateAssociation(candidateLateLoginTimeExtensionDO
					.getBatchCandidateAssociationId());
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CANDIDATE_LATE_LOGIN_EXTENSION);
			abstractAuditActionDO.setCandApplicationNumber(candidateLateLoginTimeExtensionDO.getIdentifier1());
			abstractAuditActionDO.setBatchCode(batch.getBatchCode());
			String hostAddress = "FromDx";
			if (!isRequestFromDX)
				hostAddress = ipAddress;
			abstractAuditActionDO.setHostAddress(hostAddress);
			abstractAuditActionDO.setClientTime("N/A");
			abstractAuditActionDO.setPrevLateLoginTime(candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime());
			abstractAuditActionDO.setCurrentLateLoginTime(currentLateLoginTime);

			AuditTrailLogger.auditSave(abstractAuditActionDO);

			// calculate max candidate level late login time
			if (batchLevelMaxLateLoginTimes.get(batch.getBatchCode()) == null
					|| batchLevelMaxLateLoginTimes.get(batch.getBatchCode()).before(endTimeForLogin)) {
				batchLevelMaxLateLoginTimes.put(batch.getBatchCode(), endTimeForLogin);
			}

			if (batchCandidateAssociationService.updateCandLevelLateLoginTimeByBatchIdAndCandidateId(
					candidateLateLoginTimeExtensionDO.getBatchCode(),
					candidateLateLoginTimeExtensionDO.getCandidateId(),
					candidateLateLoginTimeExtensionDO.getAssessmentCode(), endTimeForLogin) <= 0) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO
						.setErrorMessage("Late login time extension failed because of internal database error");
				// throw new CandidateRejectedException(ACSExceptionConstants.CAND_LATE_LOGIN_EXTENSION_NOT_ALLOWED,
				// "Late login time extension failed because of internal database error");
			} else {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
			}

			returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
		}

		// reschedule the dependent jobs
		if (!batchLevelMaxLateLoginTimes.isEmpty()) {
			// iterate over the map and reshedule the dependent jobs accordingly
			for (Map.Entry<String, Calendar> entry : batchLevelMaxLateLoginTimes.entrySet()) {
				resescheduleLateLoginDependentJobs(entry.getKey(), entry.getValue());
			}
		}

		return returningCandidateLateLoginTimeExtensionDOs;
	}



	/**
	 * calculates and sends the current late login time
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public CandidateLateLoginTimeExtensionDO[] initiateCandidateLateLoginTimeExtension(
			CandidateLateLoginTimeExtensionDO[] candidateLateLoginTimeExtensionDOs) throws GenericDataModelException {
		logger.info("initiated initiateCandidateLateLoginTimeExtension where candidateLateLoginTimeExtensionDOs={}",
				candidateLateLoginTimeExtensionDOs.toString());

		// maps used for caching
		Map<String, Integer> lateLoginTimePerBatchMap = new HashMap<String, Integer>();
		Map<Integer, String> candidateDetails = new HashMap<Integer, String>();
		Map<String, AcsBatch> batchDetails = new HashMap<String, AcsBatch>();

		// iterate over the each CandidateLateLoginTimeExtensionDO object
		for (CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO : candidateLateLoginTimeExtensionDOs) {
			int bcaid = candidateLateLoginTimeExtensionDO.getBatchCandidateAssociationId();
			int candidateId = candidateLateLoginTimeExtensionDO.getCandidateId();
			String batchCode = candidateLateLoginTimeExtensionDO.getBatchCode();
			String assessmentCode = candidateLateLoginTimeExtensionDO.getAssessmentCode();
			int lateLoginTime = 0;
			String candidateIdentifier1 = null;

			// fetch assessment details
			if (!lateLoginTimePerBatchMap.containsKey(assessmentCode)) {
				// fetch assessment details by assessmnetId
				AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
						cdeaService.getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(batchCode, assessmentCode);
				if (bussinessRulesAndLayoutRulesTO != null) {
					// cache it in a map
					lateLoginTimePerBatchMap.put(assessmentCode, bussinessRulesAndLayoutRulesTO.getLateLoginTime());
				} else {
					// cache it in a map
					lateLoginTimePerBatchMap.put(assessmentCode, 0);
				}
			}

			// get the assessment details
			lateLoginTime = lateLoginTimePerBatchMap.get(assessmentCode);
			logger.info("lateLoginTime = {} for assessment with id = {}", lateLoginTime, assessmentCode);

			// fetch candidate details
			if (!candidateDetails.containsKey(candidateId)) {
				// fetch assessment details by assessmnetId
				AcsCandidate candidate =
						(AcsCandidate) candidateService.get(candidateId, AcsCandidate.class.getCanonicalName());
				// cache it in a map
				candidateDetails.put(candidateId, candidate.getIdentifier1());
			}

			// get the candidate information
			candidateIdentifier1 = candidateDetails.get(candidateId);
			logger.info("candidateIdentifier1 = {} for candidate with id = {}", candidateIdentifier1, candidateId);

			// fetch candidate details
			if (!batchDetails.containsKey(batchCode)) {
				// fetch assessment details by assessmnetId
				AcsBatch batch = (AcsBatch) batchService.get(batchCode, AcsBatch.class.getCanonicalName());
				// cache it in a map
				batchDetails.put(batchCode, batch);
			}

			// get the batch information
			AcsBatch batch = batchDetails.get(batchCode);
			logger.info("batchdetails = {} for batch with id = {}", batch, batchCode);

			// fetch batch candidate association
			AcsBatchCandidateAssociation batchCandidateAssociation =
					(AcsBatchCandidateAssociation) batchCandidateAssociationService.get(bcaid,
							AcsBatchCandidateAssociation.class.getCanonicalName());
			logger.info("batchCandidateAssociations = {} for candidate with candidateId={} in batch with id = {}",
					batchCandidateAssociation, candidateId, batchCode);
			Calendar startTime = authService.getMaxCandidateBatchStartTime(
					batchCandidateAssociation.getExtendedBatchStartTimePerCandidate(), batch.getMaxBatchStartTime());
			// calculate the current late login time
			Calendar currentLateLoginTime =
					customerBatchService.getMaxLateLoginTime(
							batchCandidateAssociation.getExtendedLateLoginTimePerCandidate(), lateLoginTime,
							startTime, batch.getExtendedLateLoginTime());

			candidateLateLoginTimeExtensionDO.setCurrentLateLoginTime(TimeUtil.convertTimeAsString(currentLateLoginTime
					.getTimeInMillis()));
			candidateLateLoginTimeExtensionDO.setIdentifier1(candidateIdentifier1);

			// if (Calendar.getInstance().before(currentLateLoginTime)) {
			// candidateLateLoginTimeExtensionDO
			// .setMessage("Current late login time for candidate is not elapsed hence, extension not allowed");
			// }
		}
		return candidateLateLoginTimeExtensionDOs;
	}

	/**
	 * If there is any late login extension, then the attendance report generation time and isAllCandidateSubmittedTest
	 * job time should be changed to new late login time.
	 * 
	 * @param batchDetailsTo
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void resescheduleLateLoginDependentJobs(String batchCode, Calendar lateloginTime)
			throws GenericDataModelException {
		logger.info("resescheduling the late login dependent jobs...");

		candidateService.startAttendanceReportGeneratorJob(batchCode, lateloginTime, false);
	}

	/**
	 * Initiates the request to Dx for any late logging information
	 * 
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateLateLoginTimeExtensionDO> initiateLateLoginRequestForDX() throws Exception {
		getLogger().info("initiated candidate Late login request from DX");
		List<CandidateLateLoginTimeExtensionDO> extendCandidateLateLoginTimeInfo = null;
		try {
			Set<String> macsSet = NetworkUtility.getAllMacs();
			String acsCode = cdeaService.getACSServerCode();
			List<AcsBatch> runningBatches = batchService.getBatchesbyTimeInstance();

			List<String> batchCodes = new ArrayList<>();
			for (Iterator<AcsBatch> iteratorBatch = runningBatches.iterator(); iteratorBatch.hasNext();) {
				batchCodes.add(iteratorBatch.next().getBatchCode());
			}
			CandidateInfoRequestBean candidateInfoRequestBean =
					new CandidateInfoRequestBean(new ArrayList<>(macsSet), acsCode, batchCodes);

			HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
			String url = acss.formURL();
			logger.info("Configured DM URL's = {}", url);
			if (url != null && !url.isEmpty()) {

				String param = gson.toJson(candidateInfoRequestBean);
				logger.info("Sending Mac address = {} and requesting DM = {} for candidate Late login info", param, url);

				// Gets the JSON representation of PackExportEntity as response from DM.
				String candidateInfoJson =
						httpClientFactory.requestPostWithJson(url,
								ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_CANDIDATE_INFO_REQUESTOR, param);
				logger.info("candidateInfo as response from DM = {}", candidateInfoJson);
				if (candidateInfoJson == null || candidateInfoJson.isEmpty()) {
					logger.error("Empty reponse from DX");
					throw new Exception("No candidate Extension to process");

				}
				CandidateInfoBean candidateInfoBean = gson.fromJson(candidateInfoJson, CandidateInfoBean.class);
				if (candidateInfoBean != null) {
					if (candidateInfoBean.getCandidateLateLoginTimeExtensionDOs() == null
							&& candidateInfoBean.getCandidateBatchTimeExtensionDOs() == null) {
						logger.error("Empty reponse from DX");
						throw new Exception("No candidate Extension to process");

					}
					if (candidateInfoBean.getCandidateLateLoginTimeExtensionDOs() != null
							&& !candidateInfoBean.getCandidateLateLoginTimeExtensionDOs().isEmpty()) {
						List<CandidateLateLoginTimeExtensionDO> candidateLateLoginTimeExtensionDOs =
								candidateInfoBean.getCandidateLateLoginTimeExtensionDOs();

						extendCandidateLateLoginTimeInfo =
								extendCandidateLateLoginTime(candidateLateLoginTimeExtensionDOs, "DX", "DX", true);
					}
					if (candidateInfoBean.getCandidateBatchTimeExtensionDOs() != null
							&& !candidateInfoBean.getCandidateBatchTimeExtensionDOs().isEmpty()) {
						List<CandidateLateLoginTimeExtensionDO> candidateBatchTimeExtensionDOs =
								candidateInfoBean.getCandidateBatchTimeExtensionDOs();

						extendCandidateLateLoginTimeInfo
								.addAll(extendCandidateBatchTime(candidateBatchTimeExtensionDOs));
					}
				} else {
					logger.info("candidateInfoBean has invalid data = {}", candidateInfoBean);
					throw new Exception("candidateInfoBean has invalid data ");
				}
			} else {
				logger.error("DX not configured");
				throw new Exception("DX not configured");

			}

		} catch (SocketException | GenericDataModelException | UnsupportedEncodingException | HttpClientException
				| CandidateRejectedException e) {
			getLogger().error("Exception while executing initiateLateLoginRequestForDX...", e);
			throw e;
		}

		return extendCandidateLateLoginTimeInfo;
	}

	/**
	 * @param candidateBatchTimeExtensionDOs
	 * @param userName
	 * @param ipAddress
	 * @param isRequestFromDX
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateLateLoginTimeExtensionDO> extendCandidateBatchTime(
			List<CandidateLateLoginTimeExtensionDO> candidateBatchTimeExtensionDOs) throws GenericDataModelException,
			CandidateRejectedException {
		logger.info("initiated extendCandidateBatchTime where candidateLateLoginTimeExtensionDOs= {}",
				candidateBatchTimeExtensionDOs.toString());
		List<CandidateLateLoginTimeExtensionDO> returningCandidateBatchTimeExtensionDOs = new ArrayList<>();
		Map<String, AcsBatch> batchDetailsByBatchCode = new HashMap<String, AcsBatch>();
		Map<String, Calendar> batchLevelMaxLateLoginTimes = new HashMap<String, Calendar>();

		// iterate over each CandidateLateLoginTimeExtensionDO object
		for (Object candidateBatchTimeExtensionDOObject : candidateBatchTimeExtensionDOs) {
			CandidateLateLoginTimeExtensionDO candidateBatchTimeExtensionDO =
					(CandidateLateLoginTimeExtensionDO) candidateBatchTimeExtensionDOObject;
			candidateBatchTimeExtensionDO.setExtensionType(ExtensionTypeEnum.CANDIDATE_BATCH_ENDTIME_EXTENSION);
			// handling invalid input
			if (candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins() <= 0) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Invalid input (value greater than 0 is expected for Candidate batch end time extension)");
				logger.info("Invalid input value greater than 0 is expected for Candidate batch end time extension");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			AcsBatch batch = null;
			// get the batch information

			// If the request is from DX, batchID will not be present.
			String batchCode = candidateBatchTimeExtensionDO.getBatchCode();
			// fetch candidate details
			if (!batchDetailsByBatchCode.containsKey(batchCode)) {
				// fetch assessment details by assessmnetId
				batch = batchService.getBatchDetailsByBatchCode(batchCode);
				// cache it in a map
				batchDetailsByBatchCode.put(batchCode, batch);
			} else {
				batch = batchDetailsByBatchCode.get(batchCode);
			}
			candidateBatchTimeExtensionDO.setBatchName(batch.getBatchName());

			if (!candidateService.isCandidateExists(candidateBatchTimeExtensionDO.getIdentifier1())) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage("No candidate exists with the specified identifier : "
						+ candidateBatchTimeExtensionDO.getIdentifier1());
				logger.info("No candidate exists with the specified identifier : "
						+ candidateBatchTimeExtensionDO.getIdentifier1());
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			// get assessment and current late login info as it is not provided by DX
			AcsBatchCandidateAssociation batchCandidateAssociationTO =
					batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandIdentifier(
							batch.getBatchCode(), candidateBatchTimeExtensionDO.getIdentifier1());
			if (batchCandidateAssociationTO == null) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Batch-Candidate assosication not present. Please check pack activation status");
				logger.info("Batch-Candidate assosication not present. Please check pack activation status");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			logger.info("batchCandidateAssociationTO = {}", batchCandidateAssociationTO);
			candidateBatchTimeExtensionDO.setAssessmentCode(batchCandidateAssociationTO.getAssessmentCode());
			candidateBatchTimeExtensionDO.setCandidateId(batchCandidateAssociationTO.getCandidateId());
			candidateBatchTimeExtensionDO.setBatchCode(batch.getBatchCode());

			// calculate previous batch end time
			Calendar prevBatchEndTime =
					(batchCandidateAssociationTO.getExtendedBatchEndTimePerCandidate() == null ? batch
							.getMaxBatchEndTime() : batchCandidateAssociationTO.getExtendedBatchEndTimePerCandidate());
			String prevBatchEndTimeinString =
					TimeUtil.convertTimeAsString(prevBatchEndTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);
			candidateBatchTimeExtensionDO.setCurrentLateLoginTime(prevBatchEndTimeinString);

			// calculate end time for login based on extended time in mins
			Calendar batchEndTimeForCandidate = (Calendar) batch.getBatchEndTime().clone();
			logger.info("Current batch End Time",
					TimeUtil.convertTimeAsString(batchEndTimeForCandidate.getTimeInMillis()));
			batchEndTimeForCandidate.add(Calendar.MINUTE,
					candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins());

			// check whether extended Candidate batch end time is within current time or not
			if (batchEndTimeForCandidate.before(Calendar.getInstance())) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage("Extended Candidate batch end time is elapsed : "
						+ TimeUtil.convertTimeAsString(batchEndTimeForCandidate.getTimeInMillis()));
				logger.info("Extended Candidate batch end time is elapsed : "
						+ TimeUtil.convertTimeAsString(batchEndTimeForCandidate.getTimeInMillis()));
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			// check whether new Candidate batch end time is within batch end time or not
			if (batchEndTimeForCandidate.before(batch.getMaxBatchEndTime())) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Candidate batch end time extension not allowed as it is before actual batch end time is elapsed : "
								+ TimeUtil.convertTimeAsString(batch.getMaxBatchEndTime().getTimeInMillis()));
				logger.info("Candidate batch end time extension not allowed as it is before actual batch end time is elapsed : "
						+ TimeUtil.convertTimeAsString(batch.getMaxBatchEndTime().getTimeInMillis()));
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			// check whether new Candidate batch end time is within delat rpack generation time or not
			if (batch.getMaxDeltaRpackGenerationTime().before(batchEndTimeForCandidate)) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Candidate batch end time extension not allowed beyond delta rpack generation time : "
								+ TimeUtil
										.convertTimeAsString(batch.getMaxDeltaRpackGenerationTime().getTimeInMillis()));
				logger.info("Candidate batch end time extension not allowed beyond delta rpack generation time = {}"
						+ TimeUtil.convertTimeAsString(batch.getMaxDeltaRpackGenerationTime().getTimeInMillis()));
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			String currentLateLoginTime =
					TimeUtil.convertTimeAsString(batchEndTimeForCandidate.getTimeInMillis(),
							TimeUtil.DISPLAY_DATE_FORMAT);

			IncidentAuditLogEnum incidentAuditLogEnum = IncidentAuditLogEnum.CANDIDATE_LEVEL_BATCH_EXTENTION_FROM_DX;
			// audit in incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(batch.getBatchCode(), candidateBatchTimeExtensionDO.getIdentifier1(),
							"From Dx", TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
									TimeUtil.DISPLAY_DATE_FORMAT), incidentAuditLogEnum);
			incidentAuditAction.setPrevLateLoginTime(candidateBatchTimeExtensionDO.getCurrentLateLoginTime());
			incidentAuditAction.setCurrentLateLoginTime(currentLateLoginTime);
			AuditTrailLogger.incidentAudit(incidentAuditAction);

			// admin audit
			if (isAuditEnable != null && isAuditEnable.equals("true")) {
				Object[] paramArray =
						{ candidateBatchTimeExtensionDO.getIdentifier1(), batch.getBatchCode(),
								candidateBatchTimeExtensionDO.getCurrentLateLoginTime(),
								TimeUtil.convertTimeAsString(batchEndTimeForCandidate.getTimeInMillis()) };

				HashMap<String, String> logbackParams = new HashMap<String, String>();
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
						AdminAuditActionEnum.CANDIDATE_BATCH_ENDTIME_EXTENSION.toString());
				logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, "From Dx");
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, "From Dx");
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");

				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_CANDIDATE_LEVEL_BATCH_EXTENSION_MSG, paramArray);
			}

			// candidate audit trail
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setCandID(batchCandidateAssociationTO.getCandidateId());
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CANDIDATE_BATCH_ENDTIME_EXTENSION);
			abstractAuditActionDO.setCandApplicationNumber(candidateBatchTimeExtensionDO.getIdentifier1());
			abstractAuditActionDO.setBatchCode(batch.getBatchCode());
			String hostAddress = "FromDx";
			abstractAuditActionDO.setHostAddress(hostAddress);
			abstractAuditActionDO.setClientTime("N/A");
			abstractAuditActionDO.setPrevLateLoginTime(candidateBatchTimeExtensionDO.getCurrentLateLoginTime());
			abstractAuditActionDO.setCurrentLateLoginTime(currentLateLoginTime);

			AuditTrailLogger.auditSave(abstractAuditActionDO);

			// calculate max Candidate batch end time
			if (batchLevelMaxLateLoginTimes.get(batch.getBatchCode()) == null
					|| batchLevelMaxLateLoginTimes.get(batch.getBatchCode()).before(batchEndTimeForCandidate)) {
				batchLevelMaxLateLoginTimes.put(batch.getBatchCode(), batchEndTimeForCandidate);
			}
			try {
				batchCandidateAssociationTO.setExtendedBatchEndTimePerCandidate(batchEndTimeForCandidate);
				batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(batchCandidateAssociationTO);
				logger.info("Candidate Batch time extension success for candidate = {}",
						candidateBatchTimeExtensionDO.getCandidateId());
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
			} catch (Exception e) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Candidate Batch time extension failed because of internal database error");
				logger.info("Candidate Batch time extension failed because of internal database error");
			}
			returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
		}

		// reschedule the dependent jobs
		if (!batchLevelMaxLateLoginTimes.isEmpty()) {
			// iterate over the map and reshedule the dependent jobs accordingly
			for (Map.Entry<String, Calendar> entry : batchLevelMaxLateLoginTimes.entrySet()) {
				resescheduleLateLoginDependentJobs(entry.getKey(), entry.getValue());
			}
		}
		logger.info("returning CandidateBatchTimeExtensionDOs", returningCandidateBatchTimeExtensionDOs);
		return returningCandidateBatchTimeExtensionDOs;
	}

	/**
	 * extends the late login time for a candidate
	 * 
	 * @param candidateBatchTimeExtensionDOs
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @param isRequestFromDX
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 */
	public List<CandidateLateLoginTimeExtensionDO> extendCandidateLateLoginTimeAndUpdatePostponeBatchStartOnBCAIdS(AcsBatch batch,
			List<AcsBatchCandidateAssociation> bcas,
			List<CandidateLateLoginTimeExtensionDO> candidateBatchTimeExtensionDOs)
			throws GenericDataModelException, CandidateRejectedException {
		logger.info("initiated extendCandidateLateLoginTime where candidateLateLoginTimeExtensionDOs= {}",
				bcas.toString());
		List<CandidateLateLoginTimeExtensionDO> returningCandidateLateLoginTimeExtensionDOs = new ArrayList<>();
		Map<String, Calendar> batchLevelMaxLateLoginTimes = new HashMap<String, Calendar>();

		// get the batch information
		String batchCode = bcas.get(0).getBatchCode();
		// AcsBatch batch = (AcsBatch) batchService.get(batchCode, AcsBatch.class.getCanonicalName());

		logger.info("batchdetails = {} for batch with code = {}", batch, batchCode);
		Map<Integer, CandidateLateLoginTimeExtensionDO> candidateDOs = new HashMap<>();
		// iterate and make map
		for (CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO : candidateBatchTimeExtensionDOs) {
			candidateDOs.put(candidateLateLoginTimeExtensionDO.getBatchCandidateAssociationId(),
					candidateLateLoginTimeExtensionDO);
		}
		// iterate over each CandidateLateLoginTimeExtensionDO object
		for (AcsBatchCandidateAssociation batchCandidateAssociationTO : bcas) {
			CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO =
					candidateDOs.get(batchCandidateAssociationTO.getBcaid());
			if (candidateLateLoginTimeExtensionDO.getStatus() != null
					&& candidateLateLoginTimeExtensionDO.getStatus().equals(ACSConstants.MSG_FAILURE)) {
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			Calendar startTime = null;
			Calendar candidateBatchEndTime = null;
			candidateBatchEndTime = batchCandidateAssociationTO.getExtendedBatchEndTimePerCandidate();
			startTime = authService.getMaxCandidateBatchStartTime(
					batchCandidateAssociationTO.getExtendedBatchStartTimePerCandidate(), batch.getMaxBatchStartTime());
			candidateLateLoginTimeExtensionDO.setBatchCandidateAssociationId(batchCandidateAssociationTO.getBcaid());

			AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
					cdeaService.getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(batch.getBatchCode(),
							batchCandidateAssociationTO.getAssessmentCode());
			int lateLoginTime = bussinessRulesAndLayoutRulesTO.getLateLoginTime();
			// calculate the current late login time
			Calendar currentLateLoginTime = customerBatchService.getMaxLateLoginTime(
					batchCandidateAssociationTO.getExtendedLateLoginTimePerCandidate(), lateLoginTime,
					batch.getMaxBatchStartTime(),
					batch.getExtendedLateLoginTime());

			// simply extend till after new batch start time
			Calendar proposedLateLoginTime =
					customerBatchService.getMaxLateLoginTime(null, lateLoginTime, startTime, null);
			String prevLateLoginTimeinString =
					TimeUtil
							.convertTimeAsString(currentLateLoginTime.getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS);
			String proposedLateLoginTimeinString =
					TimeUtil.convertTimeAsString(proposedLateLoginTime.getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS);
			// no need to extend again
			if (currentLateLoginTime.after(proposedLateLoginTime)) {
				// update extended time
				batchCandidateAssociationService.updateBatchCandidateAssociation(batchCandidateAssociationTO);
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
				candidateLateLoginTimeExtensionDO.setErrorMessage("Successfully Processed");
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			// check whether candidate is already logged-in for 1st time
			// fetch candidate status details
			AcsCandidateStatus cs = candidateService
					.getCandidateStatus(candidateLateLoginTimeExtensionDO.getBatchCandidateAssociationId());
			if (cs != null) {
				String msg = "Candidate Already logged-in, hence skipping lateLoginExtension";
				logger.info(
						msg + " for this candidate, candidate status details = {} where candidateId = {} and batchId = {}",
						cs, candidateLateLoginTimeExtensionDO.getCandidateId(),
						candidateLateLoginTimeExtensionDO.getBatchCode());
				// update extended time
				batchCandidateAssociationService.updateBatchCandidateAssociation(batchCandidateAssociationTO);
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
				candidateLateLoginTimeExtensionDO.setErrorMessage("Successfully Processed");
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			Calendar endTimeForLogin = proposedLateLoginTime;
			// check whether extended late login time is within current time or not
			if (endTimeForLogin.before(Calendar.getInstance())) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO.setErrorMessage("Extended late login time is elapsed : "
						+ TimeUtil.convertTimeAsString(endTimeForLogin.getTimeInMillis()));
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			// check whether new late login time is within batch end time or not
			if (candidateBatchEndTime == null) {
				if (batch.getMaxBatchEndTime().before(Calendar.getInstance())) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Late login time extension not allowed as batch end time is elapsed : "
									+ TimeUtil.convertTimeAsString(batch.getMaxBatchEndTime().getTimeInMillis()));
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
			} else {
				if (candidateBatchEndTime.before(endTimeForLogin)) {
					candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateLateLoginTimeExtensionDO
							.setErrorMessage("Late login time extension not allowed beyond candidate batch end time : "
									+ TimeUtil.convertTimeAsString(candidateBatchEndTime.getTimeInMillis()));
					returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
					continue;
				}
			}
			// check whether new late login time is within delat rpack generation time or not
			if (batch.getMaxDeltaRpackGenerationTime().before(endTimeForLogin)) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO.setErrorMessage(
						"Late login time extension not allowed beyond delta rpack generation time : " + TimeUtil
								.convertTimeAsString(batch.getMaxDeltaRpackGenerationTime().getTimeInMillis()));
				returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			candidateLateLoginTimeExtensionDO.setCurrentLateLoginTime(proposedLateLoginTimeinString);
			IncidentAuditLogEnum incidentAuditLogEnum = IncidentAuditLogEnum.CANDIDATE_LEVEL_LATE_LOGIN_EXTENSION;
			// audit in incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(batch.getBatchCode(), batchCandidateAssociationTO.getCandidateLogin(),
							ACSConstants.DEFAULT_ADMIN_HOST_ADDRESS,
							TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
									TimeUtil.DISPLAY_DATE_FORMAT),
							incidentAuditLogEnum);
			incidentAuditAction.setPrevLateLoginTime(prevLateLoginTimeinString);
			incidentAuditAction.setCurrentLateLoginTime(proposedLateLoginTimeinString);
			AuditTrailLogger.incidentAudit(incidentAuditAction);
			// admin audit
			if (isAuditEnable != null && isAuditEnable.equals("true")) {
				Object[] paramArray = { batchCandidateAssociationTO.getCandidateLogin(), batch.getBatchCode(),
						prevLateLoginTimeinString,
						TimeUtil.convertTimeAsString(endTimeForLogin.getTimeInMillis()) };

				HashMap<String, String> logbackParams = new HashMap<String, String>();
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
						AdminAuditActionEnum.CANDIDATE_LATE_LOGIN_EXTENSION.toString());
				logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, ACSConstants.DEFAULT_ADMIN_USER_NAME);
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ACSConstants.DEFAULT_ADMIN_HOST_ADDRESS);
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_CANDIDATE_LEVEL_LATE_LOGIN_EXTENSION_MSG, paramArray);
			}
			// candidate audit trail
			AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
			abstractAuditActionDO.setCandID(candidateLateLoginTimeExtensionDO.getCandidateId());
			abstractAuditActionDO
					.setBatchCandidateAssociation(candidateLateLoginTimeExtensionDO.getBatchCandidateAssociationId());
			abstractAuditActionDO.setActionType(CandidateActionsEnum.CANDIDATE_LATE_LOGIN_EXTENSION);
			abstractAuditActionDO.setCandApplicationNumber(candidateLateLoginTimeExtensionDO.getIdentifier1());
			abstractAuditActionDO.setBatchCode(batch.getBatchCode());
			String hostAddress = ACSConstants.DEFAULT_ADMIN_HOST_ADDRESS;
			abstractAuditActionDO.setHostAddress(hostAddress);
			abstractAuditActionDO.setClientTime("N/A");
			abstractAuditActionDO.setPrevLateLoginTime(prevLateLoginTimeinString);
			abstractAuditActionDO.setCurrentLateLoginTime(proposedLateLoginTimeinString);
			AuditTrailLogger.auditSave(abstractAuditActionDO);
			// calculate max candidate level late login time
			if (batchLevelMaxLateLoginTimes.get(batch.getBatchCode()) == null
					|| batchLevelMaxLateLoginTimes.get(batch.getBatchCode()).before(endTimeForLogin)) {
				batchLevelMaxLateLoginTimes.put(batch.getBatchCode(), endTimeForLogin);
			}
			if (batchCandidateAssociationService.updateCandLevelLateLoginTimeByBatchIdAndCandidateId(
					candidateLateLoginTimeExtensionDO.getBatchCode(),
					candidateLateLoginTimeExtensionDO.getCandidateId(),
					candidateLateLoginTimeExtensionDO.getAssessmentCode(), endTimeForLogin) <= 0) {
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateLateLoginTimeExtensionDO
						.setErrorMessage("Late login time extension failed because of internal database error");
			} else {
				// update extended time
				batchCandidateAssociationTO.setExtendedLateLoginTimePerCandidate(endTimeForLogin);
				batchCandidateAssociationTO.setExtendedLateLoginTimePerCandidateCount(
						batchCandidateAssociationTO.getExtendedLateLoginTimePerCandidateCount() + 1);
				batchCandidateAssociationService.updateBatchCandidateAssociation(batchCandidateAssociationTO);
				candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
				candidateLateLoginTimeExtensionDO.setErrorMessage("Successfully Processed");

			}
			returningCandidateLateLoginTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
		}
		// reschedule the dependent jobs
		if (!batchLevelMaxLateLoginTimes.isEmpty()) {
			// iterate over the map and reshedule the dependent jobs accordingly
			for (Map.Entry<String, Calendar> entry : batchLevelMaxLateLoginTimes.entrySet()) {
				resescheduleLateLoginDependentJobs(entry.getKey(), entry.getValue());
			}
		}
		return returningCandidateLateLoginTimeExtensionDOs;
	}


	/**
	 * enable Candidate to login again And Extend Assessment Time
	 * 
	 * @param candidateBatchTimeExtensionDOs
	 * @param ipaddress
	 * @param username
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateLateLoginTimeExtensionDO> enableCandidateAndExtendAssessmentTime(
			List<CandidateLateLoginTimeExtensionDO> candidateBatchTimeExtensionDOs, String username, String ipaddress,
			String reason, boolean isDX) throws GenericDataModelException, CandidateRejectedException {
		logger.info("initiated enableCandidateAndExtendAssessmentTime where candidateLateLoginTimeExtensionDOs= {}",
				gson.toJson(candidateBatchTimeExtensionDOs));
		List<CandidateLateLoginTimeExtensionDO> returningCandidateBatchTimeExtensionDOs = new ArrayList<>();

		// iterate over each CandidateLateLoginTimeExtensionDO object
		for (Object candidateBatchTimeExtensionDOObject : candidateBatchTimeExtensionDOs) {
			CandidateLateLoginTimeExtensionDO candidateBatchTimeExtensionDO =
					(CandidateLateLoginTimeExtensionDO) candidateBatchTimeExtensionDOObject;
			candidateBatchTimeExtensionDO.setExtensionType(ExtensionTypeEnum.CANDIDATE_RE_ENABLE_WITH_TIME_EXTENSION);
			// handling invalid input
			int extendedReLoginTimeInMins = candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins();
			if (extendedReLoginTimeInMins <= 0) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Invalid input (value greater than 0 is expected for Candidate time extension)");
				logger.info("Invalid input value greater than 0 is expected for Candidate batch end time extension");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}

			// get the batch information
			// If the request is from DX, batchCode will not be present.
			String batchCode = candidateBatchTimeExtensionDO.getBatchCode();
			AcsBatch batch = batchService.getBatchDetailsByBatchCode(batchCode);
			candidateBatchTimeExtensionDO.setBatchName(batch.getBatchName());


			String identifier1 = candidateBatchTimeExtensionDO.getIdentifier1();
			// check multiple or single candidate
			String[] cands = identifier1.trim().split(",");
			for (String candidateIdentifier : cands) {
				candidateIdentifier = candidateIdentifier.trim();
				// get BCA
				AcsBatchCandidateAssociation batchCandidateAssociationTO =
					batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandIdentifier(
								batch.getBatchCode(), candidateIdentifier);
			if (batchCandidateAssociationTO == null) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Batch-Candidate assosication not present. Please check pack activation status");
				logger.info("Batch-Candidate assosication not present. Please check pack activation status");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			logger.info("batchCandidateAssociationTO = {}", batchCandidateAssociationTO);
			candidateBatchTimeExtensionDO.setAssessmentCode(batchCandidateAssociationTO.getAssessmentCode());
			candidateBatchTimeExtensionDO.setCandidateId(batchCandidateAssociationTO.getCandidateId());
			candidateBatchTimeExtensionDO.setBatchCode(batch.getBatchCode());

			// Time is greater than batch end time
			Calendar currentTime=Calendar.getInstance();
			currentTime.add(Calendar.MINUTE, extendedReLoginTimeInMins);
				if (currentTime.after(batch.getMaxDeltaRpackGenerationTime())) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Invalid input (value greater than Delta Rpack Generation Time for Candidate re-enable and time extension)");
				logger.info(
						"Invalid input (value greater than Delta Rpack Generation Time for Candidate re-enable and time extension)");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			AcsCandidateStatus acsCandidateStatus = batchCandidateAssociationTO.getAcscandidatestatus();
			if (!(acsCandidateStatus.getCurrentStatus().equals(CandidateTestStatusEnum.TEST_COMPLETED)
					|| acsCandidateStatus.getCurrentStatus().equals(CandidateTestStatusEnum.ENDED))) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Invalid input Candidate time extension is possible only for Ended candidates");
				logger.info(
						"Invalid input Candidate time extension is possible only for Ended candidates");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			try {
				// enable
				// end time = null, status = 1
				if (batchCandidateAssociationTO.getActualTestEndTime() != null) {
					batchCandidateAssociationTO.setClearLogin(true);
					batchCandidateAssociationTO.setActualTestEndTime(null);
					batchCandidateAssociationTO.setIsForceSubmitted(false);// default
					acsCandidateStatus.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);

					// extension json
					CandidateReEnableDO candidateReEnableDO =
							new CandidateReEnableDO(null, (extendedReLoginTimeInMins * 60), 0, RequestStatus.NEW);

					String candidateReEnableDOJson = gson.toJson(candidateReEnableDO);
					batchCandidateAssociationTO.setIsRpackGenarated(false);
					batchCandidateAssociationTO.setReenablecandidatejson(candidateReEnableDOJson);
					batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(batchCandidateAssociationTO);
				}

				logger.info("Candidate Batch time extension success for candidate = {}",
						candidateBatchTimeExtensionDO.getCandidateId());
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
					// Incident Log
					IncidentAuditLogEnum incidentAuditLogEnum =
							(isDX) ? IncidentAuditLogEnum.CANDIDATE_RE_ENABLE_WITH_TIME_EXTENSION_BY_DX
									: IncidentAuditLogEnum.CANDIDATE_RE_ENABLE_WITH_TIME_EXTENSION_BY_ACS;
					// audit in incident log
					IncidentAuditActionDO incidentAuditAction = new IncidentAuditActionDO(batch.getBatchCode(),
							candidateIdentifier, ipaddress,
							TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
									TimeUtil.DISPLAY_DATE_FORMAT),
							incidentAuditLogEnum);
					// incidentAuditAction
					// .setPrevLateLoginTime(candidateLateLoginTimeExtensionDO.getCurrentLateLoginTime());
					incidentAuditAction
							.setCurrentLateLoginTime(
									String.valueOf(candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins()));
					AuditTrailLogger.incidentAudit(incidentAuditAction);
				// Admin Audit
				// check whether admin audit is enable or not if enabled audit it
				if (isAuditEnable != null && isAuditEnable.equals("true")) {
					adminAuditForTimeExtensionWithReEnabledCandidate(username, ipaddress, reason, isDX,
							candidateBatchTimeExtensionDO, batch,
							batchCandidateAssociationTO);
						generalAuditLogs(candidateIdentifier,
							candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins(),
							CandidateActionsEnum.CAND_RE_ENABLED_WITH_TIME, batchCandidateAssociationTO.getBcaid());
				}
			} catch (Exception e) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Candidate time extension failed because of internal database error");
				logger.info("Candidate time extension failed because of internal database error");
			}
			returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
			}
		}
		logger.info("returning CandidateBatchTimeExtensionDOs", returningCandidateBatchTimeExtensionDOs);
		return returningCandidateBatchTimeExtensionDOs;
	}

	private void generalAuditLogs(String candIdentifier, int timeExtended,
			CandidateActionsEnum candidateActionsEnum, int bcaid) {
		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		Calendar presentTime = Calendar.getInstance();
		long clientTime = presentTime.getTimeInMillis();
		abstractAuditActionDO.setActionType(candidateActionsEnum);
		abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		abstractAuditActionDO.setCandApplicationNumber(candIdentifier);
		abstractAuditActionDO.setCurrentLateLoginTime(String.valueOf(timeExtended));
		abstractAuditActionDO.setBatchCandidateAssociation(bcaid);
		abstractAuditActionDO.setActionTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		AuditTrailLogger.auditSave(abstractAuditActionDO);
	}

	private void adminAuditForTimeExtensionWithReEnabledCandidate(String username, String ipaddress, String reason,
			boolean isDX,
			CandidateLateLoginTimeExtensionDO candidateBatchTimeExtensionDO, AcsBatch batch,
			AcsBatchCandidateAssociation batchCandidateAssociationTO) throws GenericDataModelException {
		String eventCode = cdeaService.getEventCodeByBatchCode(batch.getBatchCode());
		HashMap<String, String> logbackParams = new HashMap<String, String>();
		logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
				AdminAuditActionEnum.CANDIDATE_RE_ENABLED_WITH_EXTRA_TIME.toString());
		if (isDX) {
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, ACSConstants.FROM_DX);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ACSConstants.DEFAULT_LOCAL_IP_ADDRESS);
		} else {
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, username);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipaddress);
		}
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_REASON, reason);
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE, eventCode);
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_BATCH_CODE, batch.getBatchCode());
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_CAND_IDENTIFIER,
				batchCandidateAssociationTO.getCandidateLogin());
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_EXTENDED_DURATION_TIME,
				String.valueOf(candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins()));
		Object[] params = { batchCandidateAssociationTO.getCandidateLogin(),
				String.valueOf(candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins()),
				batchCandidateAssociationTO.getBatchCode(), eventCode, reason };
		AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
				ACSConstants.AUDIT_ADMIN_CANDIDATE_RE_ENABLE_WITH_DURATION_MSG, params);
	}

	/**
	 * enable Candidate to login again And Extend Assessment Time
	 * 
	 * @param batchMetaDataBean
	 * 
	 * @param candidateBatchTimeExtensionDOs
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateLateLoginTimeExtensionDO>
			enableAllCandidateAndExtendAssessmentTimeInABatch(BatchMetaDataBean batchMetaDataBean)
					throws GenericDataModelException {
		logger.info(
				"initiated enableAllCandidateAndExtendAssessmentTimeInABatch where candidateLateLoginTimeExtensionDOs= {}",
				gson.toJson(batchMetaDataBean));
		List<CandidateLateLoginTimeExtensionDO> returningCandidateBatchTimeExtensionDOs = new ArrayList<>();
		AcsBatch batch = batchService.getBatchDetailsByBatchCode(batchMetaDataBean.getBatchCode());

		List<AcsBatchCandidateAssociation> bcas =
				batchCandidateAssociationService.getBatchCandAssociationsByBatchCode(batchMetaDataBean.getBatchCode());
		int extendedTime = batchMetaDataBean.getAddOnAssessmentTimeInMin();
		boolean isSuccess = false;
		// iterate over each BCAs object
		for (AcsBatchCandidateAssociation batchCandidateAssociationTO : bcas) {
			CandidateLateLoginTimeExtensionDO candidateBatchTimeExtensionDO = new CandidateLateLoginTimeExtensionDO();
			candidateBatchTimeExtensionDO.setExtensionType(ExtensionTypeEnum.CANDIDATE_RE_ENABLE_WITH_TIME_EXTENSION);
			// handling invalid input
			candidateBatchTimeExtensionDO.setExtendedLateLoginTimeInMins(extendedTime);
			if (extendedTime <= 0) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Invalid input (value greater than 0 is expected for Candidate time extension)");
				logger.info("Invalid input value greater than 0 is expected for Candidate batch end time extension");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			// get the batch information
			candidateBatchTimeExtensionDO.setBatchName(batch.getBatchName());

			logger.info("batchCandidateAssociationTO = {}", batchCandidateAssociationTO);
			candidateBatchTimeExtensionDO.setAssessmentCode(batchCandidateAssociationTO.getAssessmentCode());
			candidateBatchTimeExtensionDO.setCandidateId(batchCandidateAssociationTO.getCandidateId());
			candidateBatchTimeExtensionDO.setBatchCode(batch.getBatchCode());

			// Time is greater than batch end time
			Calendar currentTime = Calendar.getInstance();
			currentTime.add(Calendar.MINUTE, extendedTime);
			if (currentTime.after(batch.getMaxDeltaRpackGenerationTime())) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO.setErrorMessage(
						"Invalid input (value greater than batch end time is expected for Candidate time extension)");
				logger.info(
						"Invalid input value greater than batch end time is expected for Candidate batch end time extension");
				returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
				continue;
			}
			try {
				// enable
				// end time = null, status = 1
				if (batchCandidateAssociationTO.getActualTestEndTime() != null) {
					batchCandidateAssociationTO.setActualTestEndTime(null);
					batchCandidateAssociationTO.setIsForceSubmitted(false);// default
					AcsCandidateStatus acsCandidateStatus = batchCandidateAssociationTO.getAcscandidatestatus();
					acsCandidateStatus.setCurrentStatus(CandidateTestStatusEnum.IN_PROGRESS);

					// extension json
					CandidateReEnableDO candidateReEnableDO =
							new CandidateReEnableDO(null, (extendedTime * 60), 0, RequestStatus.NEW);
					String candidateReEnableDOJson = gson.toJson(candidateReEnableDO);
					batchCandidateAssociationTO.setReenablecandidatejson(candidateReEnableDOJson);
					batchCandidateAssociationTO.setAcscandidatestatus(acsCandidateStatus);
					batchCandidateAssociationService.saveOrUpdateBatchCandidateAssociation(batchCandidateAssociationTO);
					String candidateIdentifier = candidateService
							.getCandidateIdentifierFromCandId(batchCandidateAssociationTO.getCandidateId());

					// Incident Log
					IncidentAuditLogEnum incidentAuditLogEnum =
							IncidentAuditLogEnum.CANDIDATE_RE_ENABLE_WITH_TIME_EXTENSION_BY_DX;
					// audit in incident log
					IncidentAuditActionDO incidentAuditAction =
							new IncidentAuditActionDO(batch.getBatchCode(), candidateIdentifier, "DX",
									TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(),
											TimeUtil.DISPLAY_DATE_FORMAT),
									incidentAuditLogEnum);
					incidentAuditAction.setCurrentLateLoginTime(
							String.valueOf(candidateBatchTimeExtensionDO.getExtendedLateLoginTimeInMins()));
					AuditTrailLogger.incidentAudit(incidentAuditAction);
					// general
					generalAuditLogs(candidateIdentifier, extendedTime,
							CandidateActionsEnum.CAND_RE_ENABLED_WITH_TIME, batchCandidateAssociationTO.getBcaid());
				}

				logger.info("Candidate enabling and time extension success for candidate = {}",
						batchCandidateAssociationTO.getBcaid());
				isSuccess = true;
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);


			} catch (Exception e) {
				candidateBatchTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				candidateBatchTimeExtensionDO
						.setErrorMessage("Candidate time extension failed because of internal database error");
				logger.info("Candidate time extension failed because of internal database error");
			}
			returningCandidateBatchTimeExtensionDOs.add(candidateBatchTimeExtensionDO);
		}
		// Admin Audit
		// check whether admin audit is enable or not if enabled audit it
		if (isSuccess && isAuditEnable != null && isAuditEnable.equals("true")) {
			adminAuditforAllReEnabledCandidates(batchMetaDataBean, batch);
		}
		logger.info("returning enableAllCandidateAndExtendAssessmentTimeInABatch",
				returningCandidateBatchTimeExtensionDOs);
		return returningCandidateBatchTimeExtensionDOs;
	}

	private void adminAuditforAllReEnabledCandidates(BatchMetaDataBean batchMetaDataBean, AcsBatch batch)
			throws GenericDataModelException {
		String eventCode = cdeaService.getEventCodeByBatchCode(batch.getBatchCode());
		HashMap<String, String> logbackParams = new HashMap<String, String>();
		logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
				AdminAuditActionEnum.ALL_CANDIDATES_RE_ENABLED_IN_BATCH_WITH_EXTRA_TIME.toString());
		logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, ACSConstants.SUPER_ADMIN_USER_NAME);
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ACSConstants.DEFAULT_LOCAL_IP_ADDRESS);
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE, eventCode);
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_BATCH_CODE, batch.getBatchCode());
		logbackParams.put(ACSConstants.AUDIT_LOGKEY_EXTENDED_DURATION_TIME,
				String.valueOf(batchMetaDataBean.getAddOnAssessmentTimeInMin()));
		Object[] params = {
				// batchCandidateAssociationTO.getCandidateLogin(),
				String.valueOf(batchMetaDataBean.getAddOnAssessmentTimeInMin()), batch.getBatchCode(), eventCode };
		AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
				ACSConstants.AUDIT_ADMIN_ALL_CANDIDATES_RE_ENABLE_WITH_DURATION_MSG, params);
	}

}
