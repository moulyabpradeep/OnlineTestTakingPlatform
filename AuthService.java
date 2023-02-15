package com.merittrac.apollo.acs.services;

import java.io.File;
import java.lang.reflect.Type;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

import org.jasypt.util.password.BasicPasswordEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSFilePaths;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.constants.CandidateBlockingStatus;
import com.merittrac.apollo.acs.constants.CandidateFeedBackStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateStateEnum;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.AcsBatchCandidateAssociationQPInfoDO;
import com.merittrac.apollo.acs.dataobject.AssessmentDurationDO;
import com.merittrac.apollo.acs.dataobject.BatchesInfoForLandingPage;
import com.merittrac.apollo.acs.dataobject.CandidateAssessmentDO;
import com.merittrac.apollo.acs.dataobject.CandidateCredentialsDetailsDO;
import com.merittrac.apollo.acs.dataobject.CustomerEventCodesDO;
import com.merittrac.apollo.acs.dataobject.LoginCredentials;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.AbstractAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.GeneralAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.QPInfo;
import com.merittrac.apollo.acs.entities.AcsAdmins;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.AcsProperties;
import com.merittrac.apollo.acs.entities.AcsQuestionPaper;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.BrLrUtility;
import com.merittrac.apollo.acs.utility.JSONToMapConverter;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.EventRule;
import com.merittrac.apollo.common.entities.acs.BatchTypeEnum;
import com.merittrac.apollo.common.entities.acs.HBMStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to candidate and ACS admin authentication.
 * 
 * @author Shankar_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class AuthService extends BasicService implements IAuthService {
	private static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static BatchService bs = null;
	private static CandidateService cs = null;
	private static CDEAService cdea = null;
	private static QuestionService qs = null;
	private static String isAuditEnable = null;
	private static BatchCandidateAssociationService bcas = null;
	static CustomerBatchService customerBatchService = null;
	static ACSPropertyUtil acsPropertyUtil = null;
	private static NetworkUtility networkUtility = null;
	static PackDetailsService pds = null;
	private static ACSEventRequestsService acsers = null;
	static String ipAddress = null;
	// created to handle parallel login requests for same candidate
	private static Map<String, SecretKeySpec> eventCodeToKey = new HashMap<>();

	static {
		bs = BatchService.getInstance();
		cs = CandidateService.getInstance();
		cdea = CDEAService.getInstance();
		qs = QuestionService.getInstance();
		bcas = new BatchCandidateAssociationService();
		networkUtility = new NetworkUtility();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
		customerBatchService = new CustomerBatchService();
		acsPropertyUtil = new ACSPropertyUtil();
		pds = new PackDetailsService();
		acsers = new ACSEventRequestsService();
		ipAddress = networkUtility.getIp();
	}

	// Method which authenticates a candidates user-name and password
	@Override
	public boolean validateCandidateCredentials(CandidateCredentialsDetailsDO candCredentialsDetails)
			throws GenericDataModelException, CandidateRejectedException {
		if (authenticateCandidate(candCredentialsDetails) != null)
			return true;
		else
			return false;
	}

	public CandidateAssessmentDO authenticateCandidateForDryRun (CandidateCredentialsDetailsDO candCredentialsDetails) throws GenericDataModelException, CandidateRejectedException {
		final AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) bs.session.get(candCredentialsDetails.getBcaId(),
						AcsBatchCandidateAssociation.class.getCanonicalName());
		List<AcsBatchCandidateAssociation> acsBatchCandidateAssociationList = new ArrayList<AcsBatchCandidateAssociation>();
		acsBatchCandidateAssociationList.add(batchCandidateAssociation);
		
		AcsBatchCandidateAssociationQPInfoDO associationQPInfoDO = validateBatchCandAssociationByLoginIdForDryRun(acsBatchCandidateAssociationList);
		

		// Get the path to question paper which candidate is suppose to take
		String qpPath = null;
		QPInfo finalQpInfo = null;

		String preFixDownloadURL = getPreFixDownloadURL();
		String moduleName =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
		if (moduleName == null)
			moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;
		preFixDownloadURL = preFixDownloadURL + moduleName + "/";

		String secondaryQpPath = null;

		QPInfo qpInfo = associationQPInfoDO.getQpInfo();
		if (qpInfo != null) {
			if (qpInfo.getQpFileName() != null && qpInfo.getQpFileName() != null) {
				qpPath = batchCandidateAssociation.getBatchCode() + File.separator + qpInfo.getQpFileName();
				qpPath = qpPath.replace("\\", "/");
			}
			// Secondary QP
			if (qpInfo.getSecondaryQpFileName() != null && qpInfo.getSecondaryQpFileName() != null) {
				secondaryQpPath =
						batchCandidateAssociation.getBatchCode() + File.separator
								+ qpInfo.getSecondaryQpFileName();
				secondaryQpPath = secondaryQpPath.replace("\\", "/");
			}

			finalQpInfo =
					new QPInfo(qpInfo.getQpaperCode(), qpPath, qpInfo.getSecondaryLanguage(),
							qpInfo.getSecondaryQpaperCode(), secondaryQpPath);
			finalQpInfo.setIsPracticeTestRequired(qpInfo.getIsPracticeTestRequired());
			finalQpInfo.setIsPracticeTestTaken(qpInfo.getIsPracticeTestTaken());
			finalQpInfo.setPracticeTestPathForPrimary(qpInfo.getPracticeTestPathForPrimary());
			finalQpInfo.setPracticeTestPathForSecondary(qpInfo.getPracticeTestPathForSecondary());
			finalQpInfo.setMapOfSecondaryToPrimaryQuestion(qpInfo.getMapOfSecondaryToPrimaryQuestion());
		}
		
		AcsQuestionPaper questionPaper = qs.getQPandPackIdByIdentifier(batchCandidateAssociation.getQpaperCode());
		AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCandidateAssociation.getBatchCode());
		AcsCandidate candidate = cs.getCandidateDetailsFromCandId(batchCandidateAssociation.getCandidateId());
		AcsAssessment assessmentDetails = cdea.getAssessmentDetailsByAssessmentCode(batchCandidateAssociation.getAssessmentCode());
		AcsBussinessRulesAndLayoutRules brlrRules = cdea.getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(batchDetails.getBatchCode(),
						assessmentDetails.getAssessmentCode());
		CustomerEventCodesDO customerEventCodes = cdea.getCustomerEventCodesByBatchId(batchDetails.getBatchCode());
		
		if (questionPaper != null)
			qpPath = batchDetails.getBatchCode() + File.separator + questionPaper.getQpFilename();

		if (qpPath != null) {
			qpPath = qpPath.replace("\\", "/");
		}

		// Retrieving candidate image path for TP
		String candImagePath = getCandidatePhoto(candidate);

		// Retrieve the customer logo information
		// new changes for customer logo
		String custLogoPath = getCustomerLogoPathFromBrRules(brlrRules);

		// update the candidate login time post successful authentication
		Calendar cal = Calendar.getInstance();

		AcsCandidateStatus cso = new AcsCandidateStatus();
		cso.setCsid(batchCandidateAssociation.getBcaid());
		cso.setLastHeartBeatTime(cal);
		batchCandidateAssociation.setLoginTime(cal);
		batchCandidateAssociation.setHostAddress(candCredentialsDetails.getIp());
		cso.setPlayerStatus(HBMStatusEnum.GREEN);
		cso.setSystemStatus(HBMStatusEnum.GREEN);
		cso.setPeripheralStatus(HBMStatusEnum.BLACK);
		batchCandidateAssociation.setClearLogin(false);
		cso.setLastCandActionTime(cal);

		AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
		abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
		abstractAuditActionDO.setActionType(CandidateActionsEnum.AUTHCANDIDATE);
		abstractAuditActionDO.setHostAddress(candCredentialsDetails.getIp());
		abstractAuditActionDO.setActionTime(TimeUtil.convertTimeAsString(
				candCredentialsDetails.getClientTime(), TimeUtil.DISPLAY_DATE_FORMAT));
		abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(
				candCredentialsDetails.getClientTime(), TimeUtil.DISPLAY_DATE_FORMAT));

		AuditTrailLogger.auditSave(abstractAuditActionDO);

		CandidateAssessmentDO candAssessment =
				new CandidateAssessmentDO(batchCandidateAssociation.getCandidateId(), qpPath,
						assessmentDetails.getAssessmentCode(), assessmentDetails.getAssessmentName(),
						candImagePath, batchDetails.getBatchCode());
		candAssessment.setBatchCandidateAssociationId(batchCandidateAssociation.getBcaid());
		candAssessment.setCandidateRemoteProctoringStatus(
				batchCandidateAssociation.getCandidateRemoteProctoringStatus().name());
		candAssessment.setCustomerLogopath(custLogoPath);
		candAssessment.setPreFixDownloadURL(preFixDownloadURL);
		candAssessment.setAssessmentCode(assessmentDetails.getAssessmentCode());
		candAssessment.setBatchCode(batchDetails.getBatchCode());
		candAssessment.setStatusId(cso.getCsid());
		candAssessment.setApplicationNumber(candidate.getIdentifier1());
		candAssessment.setQpInfo(finalQpInfo);
		candAssessment.setLandingPageEnabled(false);
		
		// Adding shuffle sequence. will have value in case of crash scenario.
		candAssessment.setShuffleSequence(batchCandidateAssociation.getShuffleSequence());
		if (batchCandidateAssociation.getCandSelectedLanguages()!=null && !batchCandidateAssociation.getCandSelectedLanguages().isEmpty()) {
			candAssessment.setQpCandLanguages(batchCandidateAssociation.getCandSelectedLanguages());
		}else{
			if (qpInfo!=null) {
				candAssessment.setQpCandLanguages(qpInfo.getQpLanguages());
			}
		}
		
		if (customerEventCodes != null) {
			candAssessment.setCustomerCode(customerEventCodes.getCustomerCode());
			candAssessment.setEventCode(customerEventCodes.getEventCode());
		}
		
		cs.saveOrUpdateCandidateStatus(cso);

		batchCandidateAssociation.setAcscandidatestatus(cso);
		bcas.saveOrUpdateBatchCandidateAssociation(batchCandidateAssociation);
		return candAssessment;
	}
	
	/*
	 * This method will return the numeric candidate identifier once the candidate is successfully authenticated.
	 * In-case if the candidate authentication fails we return "0" since no candidate will have identifier with "0"
	 */
	@Override
	public CandidateAssessmentDO authenticateCandidate(CandidateCredentialsDetailsDO candCredentialsDetails)
			throws GenericDataModelException, CandidateRejectedException {
		if (candCredentialsDetails.isDryRun()) {
			return  authenticateCandidateForDryRun(candCredentialsDetails);
		}
		logger.debug("authenticateCandidate for Username:{}", candCredentialsDetails.getUsername());
		boolean isLandingPageEnabled = false;
		AcsBatchCandidateAssociationQPInfoDO associationQPInfoDO = null;
		if (candCredentialsDetails.getBcaId() == 0) {
			List<AcsBatchCandidateAssociation> batchCandidateAssociations=null;
			
			if(candCredentialsDetails.isLoginCrendentialsEncrypted()){
				 batchCandidateAssociations =
							bs.getBatchCandidateAssociationsForEncryptedUserName(candCredentialsDetails.getUsername());
			}else{
				batchCandidateAssociations =
						bs.getBatchCandidateAssociationsForUserName(candCredentialsDetails.getUsername());
			}
				
			if (batchCandidateAssociations == null) {
				logger.debug("No batch exists at this time instance for the provided credentials for Username:{}",
						candCredentialsDetails.getUsername());
				throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
						"No batch exists at this time instance for the provided credentials");
			}

			// get Current Running Bca By Active Batches, if not active then remove bca from List ..
			// shorten the current running batches, then check for landing page enabled or not, its mismatching with old
			// candidates and non enabled landing page
			getCurrentRunningBcaByActiveBatches(batchCandidateAssociations);

			AcsBatchCandidateAssociation batchCandidateAssociationForLandingPageCheck =
					batchCandidateAssociations.get(0);

			if (cs.isLandingPageRequired(batchCandidateAssociationForLandingPageCheck)) {
				// Landing page is enabled when the call is not from landing page and Event rule
				isLandingPageEnabled = true;
				logger.debug("Landing page is enabled for bcaId:{}",
						batchCandidateAssociationForLandingPageCheck.getBcaid());
				CandidateAssessmentDO infoForLandingPage =
						getInfoForLandingPage(batchCandidateAssociations, candCredentialsDetails);
				logger.debug("Landing page info for username:{} is {}", candCredentialsDetails.getUsername(),
						infoForLandingPage);
				return infoForLandingPage;
			}

			associationQPInfoDO = validateBatchCandAssociationByLoginId(batchCandidateAssociations);
		} else {
			final AcsBatchCandidateAssociation batchCandidateAssociation =
					(AcsBatchCandidateAssociation) bs.session.get(candCredentialsDetails.getBcaId(),
							AcsBatchCandidateAssociation.class.getCanonicalName());
			associationQPInfoDO = validateBatchCandAssociationByLoginId(new ArrayList<AcsBatchCandidateAssociation>() {
				private static final long serialVersionUID = 1735398901075284088L;

				{
					add(batchCandidateAssociation);
				}
			});

		}

		if (associationQPInfoDO.getAcsBatchCandidateAssociation() == null) {
			logger.debug("Candidate login rejected due to Invalid Credentials for Username:{}",
					candCredentialsDetails.getUsername());
			throw new CandidateRejectedException(ACSExceptionConstants.BATCH_CAND_ASSOCIATION_NOT_FOUND,
					"Candidate login rejected due to Invalid Credentials");
		}

		AcsBatchCandidateAssociation batchCandidateAssociation = associationQPInfoDO.getAcsBatchCandidateAssociation();

		if (batchCandidateAssociation.getCandidateBlockingStatus().equals(CandidateBlockingStatus.BLOCKED)) {
			logger.debug("Candidate login blocked because of multiple invalid attempts for Username:{}",
					candCredentialsDetails.getUsername());
			throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_BLOCKED,
					"Candidate login blocked because of multiple invalid attempts");
		}
		CandidateAssessmentDO candAssessment = null;

		try {
			AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCandidateAssociation.getBatchCode());
			if (batchDetails.getBatchType().equals(BatchTypeEnum.OPEN)
					&& (batchCandidateAssociation.getExpiryDateTime() == null || batchCandidateAssociation
							.getExpiryDateTime().before(Calendar.getInstance()))) {
				logger.debug(
						"Candidate login rejected because login Id not activated or its expired for Username:{} and expirydate={}",
						candCredentialsDetails.getUsername(), batchCandidateAssociation.getExpiryDateTime());
				throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_LOGIN_ID_EXPIRED,
						"Candidate login rejected because login Id not activated or its expired");
			}
			if (batchDetails.getMaxBatchEndTime().before(Calendar.getInstance())) {
				boolean isCandidateBatchEndTimeExtended = false;
				logger.info("Batch is elapsed. Considering candidate batch End time:{}",
						batchCandidateAssociation.getExtendedBatchEndTimePerCandidate());
				// check for Candidate Level BatchEndTime whether it is elapsed or not
				if (batchCandidateAssociation.getExtendedBatchEndTimePerCandidate() != null
						&& batchCandidateAssociation.getExtendedBatchEndTimePerCandidate().before(
								Calendar.getInstance())) {
					logger.debug(
							"Candidate login not allowed after extended candidate batch end time for Username:{} and ExtendedBatchEndTimePerCandidate={}",
							candCredentialsDetails.getUsername(),
							batchCandidateAssociation.getExtendedBatchEndTimePerCandidate());
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_BATCH_END_TIME,
							"Candidate login not allowed after extended candidate batch end time");
				} else if (batchCandidateAssociation.getExtendedBatchEndTimePerCandidate() == null) {
					logger.info("No Candidate batch Extension exists");
					isCandidateBatchEndTimeExtended = false;
				} else {
					logger.info("Candidate batch Extension exists for candidate:{} till {}",
							batchCandidateAssociation.getCandidateId(),
							batchCandidateAssociation.getExtendedBatchEndTimePerCandidate());
					isCandidateBatchEndTimeExtended = true;
				}

				if (!isCandidateBatchEndTimeExtended) {
					logger.debug(
							"Candidate login not allowed after batch end time for Username:{} and isCandidateBatchEndTimeExtended={}",
							candCredentialsDetails.getUsername(), isCandidateBatchEndTimeExtended);
					throw new CandidateRejectedException(
							ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_BATCH_END_TIME,
							"Candidate login not allowed after batch end time");
				}
			}

			AcsAssessment assessmentDetails =
					cdea.getAssessmentDetailsByAssessmentCode(batchCandidateAssociation.getAssessmentCode());
			// fetch late login time for the specified assessment
			int lateLoginTime = ACSConstants.DEFAULT_POST_TEST_START_ALLOWED_LOGIN_TIME;
			int noOfAllowedLoginAttempts = ACSConstants.DEFAULT_NUMBER_OF_ALLOWED_LOGIN_ATTEMPT;
			AcsBussinessRulesAndLayoutRules brlrRules =
					cdea.getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(batchDetails.getBatchCode(),
							assessmentDetails.getAssessmentCode());
			if (brlrRules != null) {
				lateLoginTime = brlrRules.getLateLoginTime();
				noOfAllowedLoginAttempts = brlrRules.getMaxNumberOfValidLoginAttempts();
			} else {
				logger.debug("Rules doesn't exists for batchcode:{} and AssessmentCode={}",
						batchDetails.getBatchCode(), assessmentDetails.getAssessmentCode());
				throw new CandidateRejectedException(ACSExceptionConstants.RULES_NOT_FOUND, "Rules doesn't exists");
			}
			Calendar actualEndTimeForLogin = null;
			// late login processing
			AcsEvent acsEvent = bs.getEventDetailsByBatchCode(batchDetails.getBatchCode());
			boolean explicitEvent = acsPropertyUtil.isExplicitEvent(acsEvent.getEventCode());

			// @author Guddu_K
			// lateLoginRequiredValue is to be set in event rule for late login value require or not.
			boolean lateLoginRequiredValue = acsPropertyUtil.isLateLoginCheckRequired(acsEvent.getEventCode());

			// restricting this calculation for open event
			if (!batchDetails.getBatchType().equals(BatchTypeEnum.OPEN) && !(explicitEvent) && lateLoginRequiredValue) {
				Calendar startTime =
						getMaxCandidateBatchStartTime(batchCandidateAssociation.getExtendedBatchStartTimePerCandidate(),
								batchDetails.getMaxBatchStartTime());
				actualEndTimeForLogin =
						customerBatchService.getMaxLateLoginTime(
								batchCandidateAssociation.getExtendedLateLoginTimePerCandidate(), lateLoginTime,
								startTime, batchDetails.getExtendedLateLoginTime());
			}

			// fetch candidate status
			AcsCandidateStatus cso = cs.getCandidateStatus(batchCandidateAssociation.getBcaid());

			// fetch the set of identifiers associated to the candidate
			AcsCandidate candidate = cs.getCandidateDetailsFromCandId(batchCandidateAssociation.getCandidateId());
			String candidateIdentifier = candidate.getIdentifier1();

			if (cso != null && !batchCandidateAssociation.getClearLogin()) {
				// check whether candidate is entitled for auto clear login or not if enabled
				if (autoClearLoginIfRequired(batchCandidateAssociation, cso, batchDetails.getBatchCode(),
						candidateIdentifier)) {
					IncidentAuditActionDO incidentAuditAction =
							new IncidentAuditActionDO(batchDetails.getBatchCode(), candidateIdentifier,
									batchCandidateAssociation.getHostAddress(), TimeUtil.convertTimeAsString(Calendar
											.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
									IncidentAuditLogEnum.AUTO_CLEAR_LOGIN,
									batchCandidateAssociation.getClearLoginCount() + 1);

					AuditTrailLogger.incidentAudit(incidentAuditAction);
					batchCandidateAssociation.setClearLogin(true);
					batchCandidateAssociation.setClearLoginCount(batchCandidateAssociation.getClearLoginCount() + 1);
				}
			} else {
				logger.info(
						"clear login flag is already set to true or candidate hasn't logged in yet hence skipping auto clear login "
								+ " for candidate with login id = {}", candCredentialsDetails.getUsername());
			}

			// first thing to check is..Is candidate allowed to login?
			if (!cs.isCandidateAllowedForLogin(batchCandidateAssociation, cso, batchDetails, actualEndTimeForLogin)) {
				logger.debug(
						"Candidate not allowed to login as per the batch timings based on the bussiness rules for Username:{} "
								+ " and actualEndTimeForLogin={}", candCredentialsDetails.getUsername(),
						actualEndTimeForLogin);
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_EARLY_START_NOTALLOWED,
						"Candidate not allowed to login as per the batch timings based on the bussiness rules");
			}
			CustomerEventCodesDO customerEventCodes = cdea.getCustomerEventCodesByBatchId(batchDetails.getBatchCode());
			// validate the password
			if (isValidPassword(candCredentialsDetails.getPassword(), batchCandidateAssociation.getPassword(),
					customerEventCodes.getEventCode(),candCredentialsDetails.isLoginCrendentialsEncrypted())) {
				String preFixDownloadURL = null;
				// Get the path to question paper which candidate is suppose to take
				String qpPath = null;
				QPInfo finalQpInfo = null;

				preFixDownloadURL = getPreFixDownloadURL();
				String moduleName =
						PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
				if (moduleName == null)
					moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;
				preFixDownloadURL = preFixDownloadURL + moduleName + "/";

				String secondaryQpPath = null;

				QPInfo qpInfo = associationQPInfoDO.getQpInfo();
				if (qpInfo != null) {
					if (qpInfo.getQpFileName() != null && qpInfo.getQpFileName() != null) {
						qpPath = batchCandidateAssociation.getBatchCode() + File.separator + qpInfo.getQpFileName();
						qpPath = qpPath.replace("\\", "/");
					}
					// Secondary QP
					if (qpInfo.getSecondaryQpFileName() != null && qpInfo.getSecondaryQpFileName() != null) {

						secondaryQpPath =
								batchCandidateAssociation.getBatchCode() + File.separator
										+ qpInfo.getSecondaryQpFileName();
						secondaryQpPath = secondaryQpPath.replace("\\", "/");

					}

					finalQpInfo =
							new QPInfo(qpInfo.getQpaperCode(), qpPath, qpInfo.getSecondaryLanguage(),
									qpInfo.getSecondaryQpaperCode(), secondaryQpPath);
					finalQpInfo.setIsPracticeTestRequired(qpInfo.getIsPracticeTestRequired());
					finalQpInfo.setIsPracticeTestTaken(qpInfo.getIsPracticeTestTaken());
					finalQpInfo.setPracticeTestPathForPrimary(qpInfo.getPracticeTestPathForPrimary());
					finalQpInfo.setPracticeTestPathForSecondary(qpInfo.getPracticeTestPathForSecondary());
					finalQpInfo.setMapOfSecondaryToPrimaryQuestion(qpInfo.getMapOfSecondaryToPrimaryQuestion());
					finalQpInfo.setMultiLingualQP(qpInfo.isMultiLingualQP());
					finalQpInfo.setQpLanguages(qpInfo.getQpLanguages());
				}
				AcsQuestionPaper questionPaper =
						qs.getQPandPackIdByIdentifier(batchCandidateAssociation.getQpaperCode());
				if (questionPaper != null)
					qpPath = batchDetails.getBatchCode() + File.separator + questionPaper.getQpFilename();

				if (qpPath != null) {
					qpPath = qpPath.replace("\\", "/");
				}

				// Retrieving candidate image path for TP
				String candImagePath = getCandidatePhoto(candidate);

				// Retrieve the customer logo information
				// new changes for customer logo
				String custLogoPath = getCustomerLogoPathFromBrRules(brlrRules);

				// update the candidate login time post successful authentication
				Calendar cal = Calendar.getInstance();
				if (cso == null) {
					cso = new AcsCandidateStatus();
					cso.setCsid(batchCandidateAssociation.getBcaid());
					cso.setLastHeartBeatTime(cal);
					batchCandidateAssociation.setLoginTime(cal);
					// feedback changes
					boolean isFeedBackExists = false;
					long feedBackTimerValue = 0;
					String lrRules = brlrRules.getLrRules();

					Map<String, Object> map = JSONToMapConverter.parseJSONtoMap(lrRules);
					if (map.containsKey(ACSConstants.FEED_BACK_ALLOWED)) {
						isFeedBackExists = (Boolean) map.get(ACSConstants.FEED_BACK_ALLOWED);
						String feedBackFormJson = (String) map.get("feedBackFormJson");
						if (isFeedBackExists && feedBackFormJson != null && !feedBackFormJson.isEmpty()) {
							Map<String, Object> feedbackmap = JSONToMapConverter.parseJSONtoMap(feedBackFormJson);
							if (feedbackmap.containsKey(ACSConstants.FEED_BACK_TIMER)) {
								feedBackTimerValue =
										Long.parseLong((String) feedbackmap.get(ACSConstants.FEED_BACK_TIMER));
								feedBackTimerValue = feedBackTimerValue * 60;
							}
						}

					}
					if (!isFeedBackExists)
						batchCandidateAssociation.setFeedBackStatus(CandidateFeedBackStatusEnum.NOT_APPLICABLE);

					batchCandidateAssociation.setFeedbackDuration(feedBackTimerValue);
					batchCandidateAssociation.setAllotedFeedbackDuration(feedBackTimerValue);
					// end feedback changes
				} else {
					// July SMU drive, multiple logins happening for same candidate,
					// we are saving LastCrashedTime into DB to calculate crashtime
					if (cso.getLastHeartBeatTime() != null){
						cso.setLastCrashedTime(cso.getLastHeartBeatTime());
						logger.debug("Update Last HBM to Crash time = {} ", cso.getLastCrashedTime().getTime());
					}
					// Candidate already login once in-case if clear login is not true reject the login..
					if (!batchCandidateAssociation.getClearLogin()) {
						logger.debug(
								"You are not authorized to login since exam is already started i.e ClearLogion is False "
										+ " for Username:{} ", candCredentialsDetails.getUsername());
						throw new CandidateRejectedException(ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED,
								"You are not authorized to login since exam is already started");
					}
				}
				batchCandidateAssociation.setHostAddress(candCredentialsDetails.getIp());
				cso.setPlayerStatus(HBMStatusEnum.GREEN);
				cso.setSystemStatus(HBMStatusEnum.GREEN);
				cso.setPeripheralStatus(HBMStatusEnum.BLACK);
				batchCandidateAssociation.setClearLogin(false);
				cso.setLastCandActionTime(cal);

				AbstractAuditActionDO abstractAuditActionDO = new GeneralAuditDO();
				abstractAuditActionDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
				abstractAuditActionDO.setActionType(CandidateActionsEnum.AUTHCANDIDATE);
				abstractAuditActionDO.setHostAddress(candCredentialsDetails.getIp());
				abstractAuditActionDO.setActionTime(TimeUtil.convertTimeAsString(
						candCredentialsDetails.getClientTime(), TimeUtil.DISPLAY_DATE_FORMAT));
				abstractAuditActionDO.setClientTime(TimeUtil.convertTimeAsString(
						candCredentialsDetails.getClientTime(), TimeUtil.DISPLAY_DATE_FORMAT));

				AuditTrailLogger.auditSave(abstractAuditActionDO);

				candAssessment =
						new CandidateAssessmentDO(batchCandidateAssociation.getCandidateId(), qpPath,
								assessmentDetails.getAssessmentCode(), assessmentDetails.getAssessmentName(),
								candImagePath, batchDetails.getBatchCode());
				candAssessment.setBatchCandidateAssociationId(batchCandidateAssociation.getBcaid());
				candAssessment.setCandidateRemoteProctoringStatus(
						batchCandidateAssociation.getCandidateRemoteProctoringStatus().name());
				candAssessment.setCustomerLogopath(custLogoPath);
				candAssessment.setPreFixDownloadURL(preFixDownloadURL);
				candAssessment.setAssessmentCode(assessmentDetails.getAssessmentCode());
				candAssessment.setBatchCode(batchDetails.getBatchCode());
				candAssessment.setStatusId(cso.getCsid());
				candAssessment.setApplicationNumber(candidateIdentifier);
				candAssessment.setQpInfo(finalQpInfo);
				candAssessment.setLandingPageEnabled(isLandingPageEnabled);
				
				// Adding shuffle sequence. will have value in case of crash scenario.
				candAssessment.setShuffleSequence(batchCandidateAssociation.getShuffleSequence());
				if (batchCandidateAssociation.getCandSelectedLanguages()!=null && !batchCandidateAssociation.getCandSelectedLanguages().isEmpty()) {
					candAssessment.setQpCandLanguages(batchCandidateAssociation.getCandSelectedLanguages());
				}else{
					if (qpInfo!=null) {
						candAssessment.setQpCandLanguages(qpInfo.getQpLanguages());
					}
				}
				
				if (batchCandidateAssociation.getActualTestStartedTime() != null
						&& batchCandidateAssociation.getClearLoginCount() > 0)
					candAssessment.setCrashed(true);
				else
					candAssessment.setCrashed(false);

				// checks whether mif data is completely saved for this candidate or not.
				if (candidate.getIsCompleteMif() && candidate.getMifData() != null && !candidate.getMifData().isEmpty())
					candAssessment.setMifDataExists(true);
				else
					candAssessment.setMifDataExists(false);

				// feedback changes
				if (cso != null
						&& batchCandidateAssociation.getActualTestEndTime() != null
						&& batchCandidateAssociation.getFeedBackStatus().equals(
								CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED)) {
					candAssessment.setIsFeedBackSubmitted(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED.toString());
				}
				// ends here

				if (customerEventCodes != null) {
					candAssessment.setCustomerCode(customerEventCodes.getCustomerCode());
					candAssessment.setEventCode(customerEventCodes.getEventCode());

				}
				
				HashMap<String, Object> propertiesMap = new HashMap<>(1);
				AcsProperties eventproperty = acsers.getAcsPropertiesOnPropNameAndEventCode(
						EventRule.REMOTE_PROCTORING_ENABLED.toString(), acsEvent.getEventCode());
				if (eventproperty!=null) {
					propertiesMap.put(eventproperty.getPropertyName(), eventproperty.getPropertyValue());
					candAssessment.setProperties(propertiesMap);
				}

				// update the number of valid login attempts count
				if (batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts() != 0) {
					noOfAllowedLoginAttempts = batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts();
				}

				// increment the valid attempt and set the status
				batchCandidateAssociation =
						addValidLoginAttempts(noOfAllowedLoginAttempts, batchCandidateAssociation, candidateIdentifier,
								batchDetails.getBatchCode(), batchCandidateAssociation.getHostAddress());
			} else {
				// increment the invalid attempt and set the status
				batchCandidateAssociation =
						addInvalidLoginAttempts(noOfAllowedLoginAttempts, batchCandidateAssociation);
				logger.debug("Candidate login rejected due to Invalid Credentials for Username:{} ",
						candCredentialsDetails.getUsername());
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_INVALID_CREDENTIALS,
						"Candidate login rejected due to Invalid Credentials");
			}

			batchCandidateAssociation.setAcscandidatestatus(cso);
			return candAssessment;
		} finally {
			try {
				bcas.saveOrUpdateBatchCandidateAssociation(batchCandidateAssociation);
				logger.debug("Updated bca.CS for bcaid:{} and cs= {} ",batchCandidateAssociation.getBcaid(),
						batchCandidateAssociation.getAcscandidatestatus());
			} catch (Exception e) {
				logger.info("already a request for the specified candidate id = {} is under process hence ignoring it",
						batchCandidateAssociation.getCandidateId());
				throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_LOGIN_REQ_IN_PROGRESS,
						"Ignoring candidate login request because already a request is under processing");
			}
		}
	}

	/**
	 * gets the maximum of batch start time(as batch start time extension can be possible from ACS)
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	public Calendar getMaxBatchStartTime(Calendar batchStartTime, Calendar extendedBatchStartTime) {
		Calendar oldBatchStartTime = batchStartTime;
		Calendar newBatchStartTime = extendedBatchStartTime;
		if (newBatchStartTime != null) {
			if (oldBatchStartTime.before(newBatchStartTime)) {
				return newBatchStartTime;
			}
		}
		return oldBatchStartTime;
	}

	/**
	 * gets the maximum of batch start time(as batch start time extension can be possible from ACS)
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	public Calendar getMaxCandidateBatchStartTime(Calendar extendedBatchStartTimePerCandidate,
			Calendar maxBatchStartTime) {
		Calendar candidateBatchStartTime = extendedBatchStartTimePerCandidate;
		if (candidateBatchStartTime != null) {
			if (maxBatchStartTime.before(candidateBatchStartTime))
				return candidateBatchStartTime;
		}
		return maxBatchStartTime;
	}

	/**
	 * get Current Running BCA with active Batches
	 * 
	 * @param batchCandidateAssociations
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void getCurrentRunningBcaByActiveBatches(List<AcsBatchCandidateAssociation> batchCandidateAssociations)
			throws GenericDataModelException, CandidateRejectedException {
		// check whether there are any batches running or not
		List<AcsBatch> batch = bs.getBatchesbyTimeInstance();
		if (batch == null)
			throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
					"Not Batch exists at this time instance");
		List<String> batchCodes = new ArrayList<>();
		for (Iterator<AcsBatch> batchIterator = batch.iterator(); batchIterator.hasNext();) {
			batchCodes.add(batchIterator.next().getBatchCode());
		}
		// get Current Running Bca By Active Batches, if not active then remove bca from List ..
		ListIterator<AcsBatchCandidateAssociation> iterator = batchCandidateAssociations.listIterator();
		while (iterator.hasNext()) {
			AcsBatchCandidateAssociation bca = iterator.next();
			if (!batchCodes.contains(bca.getBatchCode()))
				iterator.remove();
		}
	}

	private String getCandidatePhoto(AcsCandidate candidate) {
		String candImagePath = candidate.getImageName();
		if (candImagePath != null) {
			candImagePath = ACSConstants.ACS_IMAGES_DIR + File.separator + candImagePath;
			candImagePath = candImagePath.replace("\\", "/");
		}
		return candImagePath;
	}

	private String getCustomerLogoPathFromBrRules(AcsBussinessRulesAndLayoutRules brlrRules) {
		String custLogoPath = null;
		if (brlrRules.getLrRules() != null) {
			Type mapType = new TypeToken<Map<String, Object>>() {
			}.getType();
			Map<String, Object> customerLogoJsonMap = new Gson().fromJson(brlrRules.getLrRules(), mapType);
			if (customerLogoJsonMap.containsKey(ACSConstants.CUSTOMER_LOGO_PATH)) {
				custLogoPath = ((String) customerLogoJsonMap.get(ACSConstants.CUSTOMER_LOGO_PATH));
				if (custLogoPath == null || custLogoPath.trim().equalsIgnoreCase("")) {
					custLogoPath = null;
				}
			}
		}

		// ends here
		if (custLogoPath != null) {
			custLogoPath = ACSConstants.ACS_IMAGES_DIR + File.separator + custLogoPath;
			custLogoPath = custLogoPath.replace("\\", "/");
		}
		return custLogoPath;
	}

	/**
	 * @param batchCandidateAssociations
	 * @param candCredentialsDetails
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private CandidateAssessmentDO getInfoForLandingPage(List<AcsBatchCandidateAssociation> batchCandidateAssociations,
			CandidateCredentialsDetailsDO candCredentialsDetails) throws GenericDataModelException,
			CandidateRejectedException {
		AcsBatchCandidateAssociation batchCandidateAssociation = batchCandidateAssociations.get(0);
		CustomerEventCodesDO customerEventCodes =
				cdea.getCustomerEventCodesByBatchId(batchCandidateAssociation.getBatchCode());
		if (!isValidPassword(candCredentialsDetails.getPassword(), batchCandidateAssociation.getPassword(),
				customerEventCodes.getEventCode(),candCredentialsDetails.isLoginCrendentialsEncrypted())) {
			logger.debug("Candidate login rejected due to Invalid Credentials for Username:{} ",
					candCredentialsDetails.getUsername());
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_INVALID_CREDENTIALS,
					"Candidate login rejected due to Invalid Credentials");
		}

		CandidateAssessmentDO candAssessment = new CandidateAssessmentDO();
		candAssessment.setLandingPageEnabled(true);
		// candAssessment.setCandidateInfoForLandingPages(getAllBatchInfoForCandidate(batchCandidateAssociations));

		String preFixDownloadURL = getPreFixDownloadURL();
		String moduleName =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
		if (moduleName == null)
			moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;
		preFixDownloadURL = preFixDownloadURL + moduleName + "/";

		AcsCandidate candidate = cs.getCandidateDetailsFromCandId(batchCandidateAssociation.getCandidateId());

		AcsBussinessRulesAndLayoutRules brlrRules =
				cdea.getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(
						batchCandidateAssociation.getBatchCode(), batchCandidateAssociation.getAssessmentCode());

		String photoDownloadPath = getCandidatePhoto(candidate);
		String candidateIdentifier = candidate.getIdentifier1();

		candAssessment.setPhotoDownloadPath(photoDownloadPath);
		candAssessment.setCustomerLogopath(getCustomerLogoPathFromBrRules(brlrRules));
		candAssessment.setPreFixDownloadURL(preFixDownloadURL);
		candAssessment.setApplicationNumber(candidateIdentifier);
		candAssessment.setCandID(batchCandidateAssociation.getCandidateId());
		candAssessment.setAssessmentCode(batchCandidateAssociation.getAssessmentCode());
		candAssessment.setBatchCode(batchCandidateAssociation.getBatchCode());

		AcsCandidateStatus acscandidatestatus = batchCandidateAssociation.getAcscandidatestatus();

		// checks whether mif data is completely saved for this candidate or not.
		if (candidate != null && candidate.getIsCompleteMif() && candidate.getMifData() != null
				&& !candidate.getMifData().isEmpty())
			candAssessment.setMifDataExists(true);
		else
			candAssessment.setMifDataExists(false);

		// feedback changes
		if (acscandidatestatus != null && batchCandidateAssociation.getActualTestEndTime() != null
				&& batchCandidateAssociation.getFeedBackStatus().equals(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED)) {
			candAssessment.setIsFeedBackSubmitted(CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED.toString());
		}
		// ends here

		// CustomerEventCodesDO customerEventCodes = cdea.getCustomerEventCodesByBatchId(batchId);
		if (customerEventCodes != null) {
			candAssessment.setCustomerCode(customerEventCodes.getCustomerCode());
			candAssessment.setEventCode(customerEventCodes.getEventCode());

		}
		logger.debug("getInfoForLandingPage is returning {}", candAssessment);
		return candAssessment;
	}

	/**
	 * decrypts the password from the database and compares with the password provided
	 * 
	 * @param candProvidedPassword
	 * @param password
	 * @param eventCode
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean isValidPassword(String candProvidedPassword, String password, String eventCode, boolean isLoginCrendentialsEncrypted) {
		boolean isvalid = false;
		try {
			CryptUtil cryptUtil = new CryptUtil(128);
			SecretKeySpec keySpec;
			if (eventCodeToKey.isEmpty() || !eventCodeToKey.containsKey(eventCode)) {
				keySpec = cryptUtil.generateKeySpec(eventCode);
				eventCodeToKey.put(eventCode, keySpec);
			} else
				keySpec = eventCodeToKey.get(eventCode);
			String decryptedPasword = cryptUtil.decryptTextUsingAES(password, keySpec);
			
			if (isLoginCrendentialsEncrypted) {
				try {
					decryptedPasword = this.getStringToMd5(decryptedPasword);
				} catch (NoSuchAlgorithmException e) {
					logger.error("NoSuchAlgorithmException while executing isValidPassword...", e);
				}
			}
			
			isvalid = candProvidedPassword.equals(decryptedPasword);
		} catch (ApolloSecurityException e) {
			logger.error("ApolloSecurityException while executing isValidPassword...", e);
		} finally {
			logger.info("password validation {}", isvalid);
		}
		return isvalid;

	}

	public String getStringToMd5(String password) throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(password.getBytes());

		byte byteData[] = md.digest();

		// convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
			sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	/**
	 * increments the login attempts and sets the status to ABOUT_TO_BLCOK and BLOCKED based on the allowed login
	 * attempts. If the candidate has only 2 more attempts to reach the allowed login attemts, then candidate will be
	 * marked as ABOUT_TO_BLOCK to take caution by the admin.
	 * 
	 * @param noOfAllowedLoginAttempts
	 * @param batchCandidateAssocication
	 * @throws GenericDataModelException
	 * @since Apollo v2.0
	 * @see
	 */
	private AcsBatchCandidateAssociation addInvalidLoginAttempts(int noOfAllowedLoginAttempts,
			AcsBatchCandidateAssociation batchCandidateAssocication) throws GenericDataModelException {
		int currentCountOfInvalidLoginAttempts = batchCandidateAssocication.getNumberOfInvalidLoginAttempts() + 1;
		CandidateBlockingStatus blockingStatus = CandidateBlockingStatus.ALLOWED;
		logger.debug("Candidate with userName {} has made {} invalid attempts",
				batchCandidateAssocication.getCandidateLogin(), currentCountOfInvalidLoginAttempts);
		if (noOfAllowedLoginAttempts == 0) {
			logger.debug("No need to validate the status as number of allowed logins is zero");
			return batchCandidateAssocication;
		}
		batchCandidateAssocication.setNumberOfInvalidLoginAttempts(currentCountOfInvalidLoginAttempts);
		batchCandidateAssocication.setCandidateBlockingStatus(blockingStatus);

		bcas.updateInvalidLoginAttempts(batchCandidateAssocication.getBcaid(), currentCountOfInvalidLoginAttempts,
				blockingStatus);
		return batchCandidateAssocication;
	}

	/**
	 * increments the login attempts and sets the status to ABOUT_TO_BLCOK and BLOCKED based on the allowed login
	 * attempts. If the candidate has only 2 more attempts to reach the allowed login attemts, then candidate will be
	 * marked as ABOUT_TO_BLOCK to take caution by the admin.
	 * 
	 * @param noOfAllowedLoginAttempts
	 * @param batchCandidateAssocication
	 * @throws GenericDataModelException
	 * @since Apollo v2.0
	 * @see
	 */
	private AcsBatchCandidateAssociation
			addValidLoginAttempts(int noOfAllowedLoginAttempts,
					AcsBatchCandidateAssociation batchCandidateAssocication, String candIdentifier, String batchCode,
					String ip) throws GenericDataModelException {
		int currentCountOfValidLoginAttempts = batchCandidateAssocication.getNumberOfValidLoginAttempts() + 1;
		CandidateBlockingStatus blockingStatus = CandidateBlockingStatus.ALLOWED;
		logger.debug("Candidate with userName {} has made {} valid attempts",
				batchCandidateAssocication.getCandidateLogin(), currentCountOfValidLoginAttempts);
		if (noOfAllowedLoginAttempts == 0) {
			logger.debug("No need to validate the status as number of allowed logins is zero");
			return batchCandidateAssocication;
		}
		if (currentCountOfValidLoginAttempts == noOfAllowedLoginAttempts) {
			blockingStatus = CandidateBlockingStatus.BLOCKED;
			logger.debug("Candidate with userName {} has crossed the allowed login attemts: {} ",
					batchCandidateAssocication.getCandidateLogin(), noOfAllowedLoginAttempts);
			// audit for incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(batchCode, candIdentifier, ip, TimeUtil.convertTimeAsString(Calendar
							.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
							IncidentAuditLogEnum.BLOCK_CANDIDATE);
			incidentAuditAction.setMaxNumberOfValidLoginAttempts(noOfAllowedLoginAttempts);
			incidentAuditAction.setNumberOfValidLoginAttempts(currentCountOfValidLoginAttempts);
			AuditTrailLogger.incidentAudit(incidentAuditAction);
		} else if (currentCountOfValidLoginAttempts >= noOfAllowedLoginAttempts - 2) {
			blockingStatus = CandidateBlockingStatus.ABOUT_TO_BLOCK;
			logger.debug("Candidate with userName {} has only 2 more invalid login attemts: {} ",
					batchCandidateAssocication.getCandidateLogin(), noOfAllowedLoginAttempts);
		}
		batchCandidateAssocication.setNumberOfValidLoginAttempts(currentCountOfValidLoginAttempts);
		batchCandidateAssocication.setCandidateBlockingStatus(blockingStatus);
		return batchCandidateAssocication;
		// bca.updateValidLoginAttempts(batchCandidateAssocication.getBcaid(), currentCountOfValidLoginAttempts,
		// blockingStatus);
	}

	/**
	 * gets the download url configured in properties file
	 */
	private String getPreFixDownloadURL() {
		String preFixDownloadURL =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_PREFIX_DOWNLOAD_URL);
		if (preFixDownloadURL == null || preFixDownloadURL.isEmpty()) {
			// String ipAddress = networkUtility.getIp();
			if (ipAddress == null) {
				preFixDownloadURL = ACSConstants.DEFAULT_ACS_DOWNLOAD_URL;
			} else {
				preFixDownloadURL = ACSConstants.HTTP + ipAddress + ACSConstants.DEFAULT_ACS_RELATIVE_DOWNLOAD_URL;
			}
		}
		return preFixDownloadURL;
	}

	/*
	 * Will authenticate an administrator login into ACS. As of now it support only local admin user account.
	 */
	public int authenticateAdmin(String adminUName, String givenPasswd) throws GenericDataModelException {
		logger.info("authenticateAdmin : adminUName=" + adminUName + " and Password is encrypted "/* + givenPasswd */);
		int result = 0;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.USER_NAME, adminUName);
		String query = ACSQueryConstants.QUERY_AUTHENTICATE_ADMIN;
		AcsAdmins admin = (AcsAdmins) session.getByQuery(query, params);
		if (admin == null) {
			logger.info("authenticateAdmin : No AcsAdmin found for username = " + adminUName + "hence returning");
			return result;
		}
		BasicPasswordEncryptor basicPasswordEncryptor = new BasicPasswordEncryptor();
		if (admin.getAdminUserName().equalsIgnoreCase(adminUName)) {
			// Decrypter decrypter = new MeritracCipher.Decrypter();
			// BASE64Decoder decoder = new BASE64Decoder();
			try {
				// byte[] byteArrayToDecrypt = Hex.decodeHex(cand.getPassword().toCharArray());
				// byte[] decodedBytes = decoder.decodeBuffer(admin.getAdminPassword());
				// actualPasword = new String(decrypter.decrypt(decodedBytes));
				if (basicPasswordEncryptor.checkPassword(givenPasswd, admin.getAdminPassword())) {
					if (isAuditEnable != null && isAuditEnable.equals("true")) {
						HashMap<String, String> logbackParams = new HashMap<String, String>();
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
								AdminAuditActionEnum.AUTHADMIN.toString());
						logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, adminUName);
						Object[] aparams = {};
						AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
								logbackParams, ACSConstants.AUDIT_ADMIN_AUTH_MSG, aparams);
						// cs.insertAdminAuditData(adminAuditTO);
					}
					result = admin.getId();
					logger.info("authenticateAdmin password matched!!");
				} else {
					logger.info("authenticateAdmin password did not match!!");
				}
				logger.info("authenticateAdmin returning : result=" + result);
				return result;
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
				return result;
			}
		} else {
			logger.info("authenticateAdmin : username doesn't match = " + adminUName + "hence returning");
			return result;
		}
	}

	/*
	 * public BatchCandidateAssociationTO validateBatchCandAssociationByLoginId(List<Integer> batchIds, String loginId)
	 * throws GenericDataModelException, CandidateRejectedException { // need to fetch candidate status objects related
	 * to the current running batches if not exists then proceed as // it is if exists and if the candidate is not ended
	 * the test yet then return the batch candidate association // related to that batch. StatsCollector statsCollector
	 * = StatsCollector.getInstance(); long startTime = statsCollector.begin(); CandidateStatusTO candidateStatusTO =
	 * cs.getCandidatestatusDetailsForNonEndedCandidates(loginId, batchIds); if (candidateStatusTO != null) {
	 * logger.info(
	 * "Candidate already logged in for one of the overlapped batches hence, authenticatind under that batch where candidateStatusTO= {}"
	 * , candidateStatusTO); statsCollector.log(startTime, "validateBatchCandAssociationByLoginId", "AuthService",
	 * candidateStatusTO.getCandidateId()); return
	 * bca.getBatchCandidateAssociationBybatchIdAndCandidateId(candidateStatusTO.getBatchId(),
	 * candidateStatusTO.getCandidateId()); }
	 * 
	 * List<BatchCandidateAssociationTO> batchCandidateAssociations =
	 * bca.getBatchCandAssociationsByBatchIdsAndLoginId(batchIds, loginId); if (batchCandidateAssociations == null ||
	 * batchCandidateAssociations.isEmpty()) { throw new
	 * CandidateRejectedException(ACSExceptionConstants.BATCH_CAND_ASSOCIATION_NOT_FOUND,
	 * "Candidate login rejected due to Invalid Credentials"); } for (Iterator iterator =
	 * batchCandidateAssociations.iterator(); iterator.hasNext();) { BatchCandidateAssociationTO
	 * batchCandidateAssociation = (BatchCandidateAssociationTO) iterator.next();
	 * 
	 * CandidateStatusTO candidateStatus = cs.getCandidateStatus(batchCandidateAssociation.getCandidateId(),
	 * batchCandidateAssociation.getBatchId()); if (candidateStatus != null) { // Candidate already login once in-case
	 * if clear login is not true // reject the login.. if (candidateStatus.getActualTestEndTime() == null) {
	 * statsCollector.log(startTime, "validateBatchCandAssociationByLoginId", "AuthService",
	 * candidateStatus.getCandidateId()); return batchCandidateAssociation; } else { if (iterator.hasNext()) {
	 * statsCollector.log(startTime, "validateBatchCandAssociationByLoginId", "AuthService",
	 * candidateStatus.getCandidateId()); continue; // return (BatchCandidateAssociationTO) iterator.next(); } else {
	 * throw new CandidateRejectedException( ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_END_TEST,
	 * "Candidate not allowed to login, candidate has already ended the test"); } } } else { BatchDetailsTO batchDetails
	 * = bs.getBatchDetailsByBatchId(batchCandidateAssociation.getBatchId()); if (batchDetails != null) { int
	 * lateLoginTime = cdea.getLateLoginTimeByAssessmnetIdAndBatchId(batchCandidateAssociation.getAssessmentId(),
	 * batchDetails.getBatchId());
	 * 
	 * if (lateLoginTime >= 0) { Calendar actualEndTimeForLogin = customerBatchService.getMaxLateLoginTime(
	 * batchCandidateAssociation.getExtendedLateLoginTimePerCandidate(), lateLoginTime,
	 * batchDetails.getBatchStartTime(), batchDetails.getExtendedLateLoginTime());
	 * 
	 * if (actualEndTimeForLogin.after(Calendar.getInstance())) { statsCollector.log(startTime,
	 * "validateBatchCandAssociationByLoginId", "AuthService", batchCandidateAssociation.getCandidateId()); return
	 * batchCandidateAssociation; } } else { throw new CandidateRejectedException(ACSExceptionConstants.RULES_NOT_FOUND,
	 * "Rules doesn't exists"); } } } } throw new
	 * CandidateRejectedException(ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED,
	 * "Candidate login rejected due to Invalid Credentials"); }
	 */

	public boolean auditUserAuthentication(String userName, String ipAddress) {
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.AUTHADMIN.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
			Object[] aparams = {};
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_ADMIN_AUTH_MSG, aparams);
			// cs.insertAdminAuditData(adminAuditTO);
		}
		return true;
	}

	/**
	 * getCustomerLogoPathLocation API used to get the customer logo path location based on assessmentId.
	 * 
	 * @param assessmentId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getCustomerLogoPathLocation(String assessmentCode, String batchCode) throws GenericDataModelException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		AcsBussinessRulesAndLayoutRules brlr =
				cdea.getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(assessmentCode, batchCode);
		String custLogoPath = (String) BrLrUtility.getLrRule(brlr.getLrRules(), ACSConstants.CUSTOMER_LOGO_PATH_JSON);
		if (custLogoPath != null && !custLogoPath.isEmpty()) {
			custLogoPath = custLogoPath.replace("{", "");
			custLogoPath = custLogoPath.replace("}", "");
			AcsEvent entity = cdea.getEventDetailsByAssessmentID(assessmentCode);
			String s[] = custLogoPath.split(":");
			custLogoPath =
					ACSFilePaths.getACSDownloadDirectory() + File.separator + entity.getEventCode() + File.separator
							+ s[1];
			custLogoPath = custLogoPath.replace("\"", "");
			custLogoPath = custLogoPath.replace("\\", "/");
		} else
			custLogoPath = "";

		statsCollector.log(startTime, "getCustomerLogoPathLocation", "AuthService", 0);
		return custLogoPath;
	}

	/**
	 * clear login the candidate if crashed and reaches the clear login time frame specified in properties file assuming
	 * clear login time frame is always greater than the heart beat time frame
	 * 
	 * @param candidateStatus
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean autoClearLoginIfRequired(AcsBatchCandidateAssociation batchCandidateAssociation,
			AcsCandidateStatus candidateStatus, String batchCode, String candidateIdentifier)
			throws GenericDataModelException {
		logger.info("initiated autoClearLoginIfRequired where candidateStatus= {}", candidateStatus);

		// continue only if all of the following conditions satisfy 1.candidate status is not null, 2.last heart beat
		// time is not null and 3.auto clear login is enabled
		if (candidateStatus.getLastHeartBeatTime() != null && acsPropertyUtil.isAutoClearLoginEnabled()) {
			// fetch current time
			long currentTime = Calendar.getInstance().getTimeInMillis();

			// fetch last heart beat time
			long lastHeartBeatTime = candidateStatus.getLastHeartBeatTime().getTimeInMillis();
			logger.info("candidate lastHeartBeatTime = {}", lastHeartBeatTime);

			// calculate the difference
			long inActiveTime = (currentTime - lastHeartBeatTime);
			logger.info("candidate inActiveTime= {}", inActiveTime);

			// fetch the auto clear login time frame
			long autoClearLoginTimeFrame = (acsPropertyUtil.getAutoClearLoginTimeFrame() * 60 * 1000);
			logger.info("candidate autoClearLoginTimeFrame = {}", autoClearLoginTimeFrame);

			// if the inActiveTime is greater than the autoClearLoginTimeFrame clear login that candidate
			if (inActiveTime >= autoClearLoginTimeFrame) {
				// clear candidate login
				logger.info("autoClearLoginTimeFrame is elapsed hence, clear logging the candidate");

				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("batchCandidateAssociationId", /* ACSConstants.BATCH_CANDIDATE_ASSOCIATION_ID, */
						batchCandidateAssociation.getBcaid());
				params.put(ACSConstants.CLEAR_LOGIN, true);
				params.put(ACSConstants.CLEAR_LOGIN_COUNT, (batchCandidateAssociation.getClearLoginCount() + 1));

				IncidentAuditActionDO incidentAuditAction =
						new IncidentAuditActionDO(batchCode, candidateIdentifier,
								batchCandidateAssociation.getHostAddress(), TimeUtil.convertTimeAsString(Calendar
										.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
								IncidentAuditLogEnum.AUTO_CLEAR_LOGIN,
								batchCandidateAssociation.getClearLoginCount() + 1);

				AuditTrailLogger.incidentAudit(incidentAuditAction);
				return true;
			} else {
				logger.info("skipping auto clear login as candidate status has invalid data");
			}
		}
		return false;
	}

	/**
	 * validates client ip address with server ip address and based on the configured property of
	 * isCandidateLoginAllowedInACS. if the property is set to false and clent ip matches with server ip then block the
	 * authentication
	 * 
	 * @param clientIpAddress
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean isCandidateLoggingInACS(String clientIpAddress) {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		boolean isCandidateLoggingInACS = false;
		logger.info("initiated isCandidateLoggingInACS where clentIpAddress = {}", clientIpAddress);

		// fetch configured property for candidate login allowed in acs or not.
		boolean isCandidateAllowedForLoginInACS = acsPropertyUtil.isCandidateLoginAllowedInACS();
		logger.info("isCandidateAllowedForLoginInACS = {}", isCandidateAllowedForLoginInACS);

		if (!isCandidateAllowedForLoginInACS) {
			// check whether client ip is default ip or not
			if (clientIpAddress.equalsIgnoreCase(ACSConstants.DEFAULT_LOCAL_IP_ADDRESS)) {
				isCandidateLoggingInACS = true;
			}

			String serverIpAddress = networkUtility.getIp();
			logger.info("server ip address = {}", serverIpAddress);

			// if client ip is not default ip then validate it against server ip
			if (serverIpAddress != null && serverIpAddress.equalsIgnoreCase(clientIpAddress)) {
				isCandidateLoggingInACS = true;
			} else {
				logger.info("server ip address is not matched with client ip address");
			}
		} else {
			logger.info("ignoring isCandidateAllowedForLoginInACS validation as it is set to true");
		}
		statsCollector.log(startTime, "isCandidateLoggingInACS", "AuthService", 0);
		return isCandidateLoggingInACS;
	}

	public AcsBatchCandidateAssociationQPInfoDO validateBatchCandAssociationByLoginIdForDryRun(
			List<AcsBatchCandidateAssociation> batchCandidateAssociations) throws GenericDataModelException,
			CandidateRejectedException {
		AcsBatchCandidateAssociation batchCandidateAssociation = batchCandidateAssociations.get(0);
		String qpId = qs.getQPForCandidate(batchCandidateAssociation);
		if (qpId == null) {
			logger.debug("Question paper is not available for bcaid {}", batchCandidateAssociation.getBcaid());
			throw new CandidateRejectedException(ACSExceptionConstants.QUESTION_PAPER_NOT_FOUND,
					"Question paper is not available");
		}
		AcsBatchCandidateAssociationQPInfoDO associationQPInfoDO = new AcsBatchCandidateAssociationQPInfoDO();
		QPInfo qpInfo = qs.makeQPInfo(qpId, false, batchCandidateAssociation.getAssessmentCode());
		qpInfo.setQpaperCode(qpId);
		associationQPInfoDO.setQpInfo(qpInfo);
		batchCandidateAssociation.setQpaperCode(qpId);
		associationQPInfoDO.setAcsBatchCandidateAssociation(batchCandidateAssociation);
		return associationQPInfoDO;
	}

	public AcsBatchCandidateAssociationQPInfoDO validateBatchCandAssociationByLoginId(
			List<AcsBatchCandidateAssociation> batchCandidateAssociations) throws GenericDataModelException,
			CandidateRejectedException {
		AcsBatchCandidateAssociationQPInfoDO associationQPInfoDO = new AcsBatchCandidateAssociationQPInfoDO();
		for (Iterator<AcsBatchCandidateAssociation> iterator = batchCandidateAssociations.iterator(); iterator
				.hasNext();) {
			AcsBatchCandidateAssociation batchCandidateAssociation = iterator.next();
			// Candidate already login once in-case if clear login is not true reject the login..
			if (batchCandidateAssociation.getLoginTime() != null
					&& batchCandidateAssociation.getActualTestEndTime() == null) {
				QPInfo qpInfo =
						qs.makeQPInfo(batchCandidateAssociation.getQpaperCode(), batchCandidateAssociation
								.getAcscandidatestatus().getIsPracticeTestTaken(), batchCandidateAssociation
								.getAssessmentCode());

				associationQPInfoDO.setQpInfo(qpInfo);
				associationQPInfoDO.setAcsBatchCandidateAssociation(batchCandidateAssociation);
				logger.debug("validateCandidateAndGetLoginData is returning {}", batchCandidateAssociation);
				return associationQPInfoDO;
			}
		}
		for (Iterator<AcsBatchCandidateAssociation> iterator = batchCandidateAssociations.iterator(); iterator
				.hasNext();) {
			AcsBatchCandidateAssociation batchCandidateAssociation = iterator.next();

			// IIBF Change
			String qpId = qs.getQPForCandidate(batchCandidateAssociation);
			if (qpId == null) {
				logger.debug("Question paper is not available for bcaid {}", batchCandidateAssociation.getBcaid());
				throw new CandidateRejectedException(ACSExceptionConstants.QUESTION_PAPER_NOT_FOUND,
						"Question paper is not available");
			}
			QPInfo qpInfo = qs.makeQPInfo(qpId, false, batchCandidateAssociation.getAssessmentCode());
			qpInfo.setQpaperCode(qpId);
			associationQPInfoDO.setQpInfo(qpInfo);
			batchCandidateAssociation.setQpaperCode(qpId);
			associationQPInfoDO.setAcsBatchCandidateAssociation(batchCandidateAssociation);
			if (batchCandidateAssociation.getLoginTime() != null) {
				if (iterator.hasNext()) {
					continue;
				} else {
					logger.debug(
							"Candidate not allowed to login, candidate has already ended the test,  bcaid = {} and time = {}",
							batchCandidateAssociation.getBcaid(), batchCandidateAssociation.getLoginTime());
					throw new CandidateRejectedException(ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_END_TEST,
							"Candidate not allowed to login, candidate has already ended the test");
				}
			} else {
				if (batchCandidateAssociation.getAssessmentCode() != null) {
					AcsBatch batch = bs.getBatchDetailsByBatchCode(batchCandidateAssociation.getBatchCode());
					// As candidate loginId is unique across the events, ideally there should be only one
					// batchCandidateAssociation at a particular time instance hence returning the mapping without any
					// validation
					if (batch.getBatchType().equals(BatchTypeEnum.OPEN)) {
						if (batchCandidateAssociation.getExpiryDateTime() != null
								&& batchCandidateAssociation.getExpiryDateTime().after(Calendar.getInstance())) {
							return associationQPInfoDO;
						} else {
							logger.debug("Candidate login rejected because login Id not activated "
									+ " or Id used is expired for bcaid {} and time = {}",
									batchCandidateAssociation.getBcaid(), batchCandidateAssociation.getExpiryDateTime());
							throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_LOGIN_ID_EXPIRED,
									"Candidate login rejected because login Id not activated or Id used is expired");
						}
					} else {
						AcsEvent acsEvent = bs.getEventDetailsByBatchCode(batch.getBatchCode());
						boolean explicitEvent = acsPropertyUtil.isExplicitEvent(acsEvent.getEventCode());
						logger.debug("isExplicitEvent:{} for eventcode:{}", explicitEvent, acsEvent.getEventCode());

						boolean lateLoginRequiredValue =
								acsPropertyUtil.isLateLoginCheckRequired(acsEvent.getEventCode());
						// for RESCHEDULED and SCHEDULED candidates apply Late Login time
						if ((!batchCandidateAssociation.getCandidateType().equals(CandidateType.WALKIN))
								&& !explicitEvent && lateLoginRequiredValue) {
							int lateLoginTime =
									cdea.getLateLoginTimeByAssessmnetCodeAndBatchCode(
											batchCandidateAssociation.getAssessmentCode(),
											batchCandidateAssociation.getBatchCode());
							if (lateLoginTime >= 0) {
								Calendar startTime = getMaxCandidateBatchStartTime(
										batchCandidateAssociation.getExtendedBatchStartTimePerCandidate(),
										batch.getMaxBatchStartTime());
								Calendar actualEndTimeForLogin =
										customerBatchService.getMaxLateLoginTime(
												batchCandidateAssociation.getExtendedLateLoginTimePerCandidate(),
												lateLoginTime, startTime,
												batch.getExtendedLateLoginTime());

								if (actualEndTimeForLogin.after(Calendar.getInstance())) {
									return associationQPInfoDO;
								}
							} else {
								logger.debug("Rules doesn't exists " + " for bcaid {} and lateLoginTime In mins = {}",
										batchCandidateAssociation.getBcaid(), lateLoginTime);
								throw new CandidateRejectedException(ACSExceptionConstants.RULES_NOT_FOUND,
										"Rules doesn't exists");
							}
						} // for WALKIN candidates and EXPLICIT event candidates =>
						else {
							// No Late login Time, the candidate can login till Batch-End-Time, Hence returning
							if (batch.getMaxBatchEndTime().after(Calendar.getInstance()))
								return associationQPInfoDO;
							else {
								// Batch is expired
								logger.debug("The Batch is Expired " + " for bcaid {} and BatchEndTime = {}",
										batchCandidateAssociation.getBcaid(), batch.getMaxBatchEndTime().getTime());
								throw new CandidateRejectedException(ACSExceptionConstants.BATCH_EXPIRED,
										"The Batch is Expired");
							}
						}
					}
				} else {
					logger.debug("Assessment doesn't exist" + " for bcaid = {} and AssessmentCode = {}",
							batchCandidateAssociation.getBcaid(), batchCandidateAssociation.getAssessmentCode());
					throw new CandidateRejectedException(ACSExceptionConstants.ASSESSMENT_NOT_FOUND,
							"Assessment doesn't exist");
				}
			}
		}
		logger.debug("No BCA Association and Candidate login rejected due to Invalid Credentials");
		throw new CandidateRejectedException(ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED,
				"Candidate login rejected due to Invalid Credentials");
	}

	/**
	 * @param candCredentialsDetails
	 * @param batchCandidateAssociations
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchesInfoForLandingPage> getAllBatchInfoForCandidate(
			List<AcsBatchCandidateAssociation> batchCandidateAssociations) throws GenericDataModelException {

		List<BatchesInfoForLandingPage> candidateInfoForLandingPages = new ArrayList<>();
		boolean isAnExamIsInProgress = false;
		for (AcsBatchCandidateAssociation batchCandidateAssociation : batchCandidateAssociations) {
			AcsBatch batch = bs.getBatchDetailsByBatchCode(batchCandidateAssociation.getBatchCode());
			AcsAssessment assessment =
					cdea.getAssessmentDetailsByAssessmentCode(batchCandidateAssociation.getAssessmentCode());
			Calendar startTime = getMaxCandidateBatchStartTime(
					batchCandidateAssociation.getExtendedBatchStartTimePerCandidate(), batch.getMaxBatchStartTime());
			BatchesInfoForLandingPage candidateInfoForLandingPage =
					new BatchesInfoForLandingPage(batch.getBatchName(), batch.getBatchCode(),
							startTime, batch.getMaxBatchEndTime(), assessment.getAssessmentName(),
							assessment.getAssessmentCode(), batchCandidateAssociation.getBcaid());
			try {

				AcsBussinessRulesAndLayoutRules brlrRules =
						cdea.getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(
								batchCandidateAssociation.getBatchCode(), batchCandidateAssociation.getAssessmentCode());
				AssessmentDurationDO acsAssessmentDurationDO =
						new Gson().fromJson(brlrRules.getAssessmentDuration(), AssessmentDurationDO.class);
				candidateInfoForLandingPage.setAssessmentDuration(acsAssessmentDurationDO.getAssessmentDuration());
				// Check already he has started exam
				if (batchCandidateAssociation.getLoginTime() != null
						&& batchCandidateAssociation.getActualTestEndTime() == null) {
					isAnExamIsInProgress = true;
					candidateInfoForLandingPage.setCandidateStateEnum(CandidateStateEnum.STARTED);
					// If section level duration is present, assessmentDuration need not to be given
					if (acsAssessmentDurationDO.getSectionLevelDuration() == null
							|| acsAssessmentDurationDO.getSectionLevelDuration().isEmpty())
						candidateInfoForLandingPage.setRemainingTime(cs
								.getRemainingTimeForOnGoingExam(batchCandidateAssociation.getBcaid()));
					String qpaperCode = batchCandidateAssociation.getQpaperCode();
					if (qpaperCode != null && !qpaperCode.isEmpty()) {
						candidateInfoForLandingPage.setQuestionCount(qs.getQuestionCountForQPWithoutRC(qpaperCode));
						candidateInfoForLandingPage.setQpInfo(getQpInfo(batchCandidateAssociation, qpaperCode));
					}
					logger.debug("candidateInfoForLandingPage added: {}", candidateInfoForLandingPage);
					candidateInfoForLandingPages.add(candidateInfoForLandingPage);
					continue;
				} else if (batchCandidateAssociation.getActualTestEndTime() != null) {
					candidateInfoForLandingPage.setCandidateStateEnum(CandidateStateEnum.ENDED);
					String qpaperCode = batchCandidateAssociation.getQpaperCode();
					if (qpaperCode != null && !qpaperCode.isEmpty()) {
						candidateInfoForLandingPage.setQuestionCount(qs.getQuestionCountForQPWithoutRC(qpaperCode));
						candidateInfoForLandingPage.setQpInfo(getQpInfo(batchCandidateAssociation, qpaperCode));
					}

					logger.debug("candidateInfoForLandingPage added: {}", candidateInfoForLandingPage);
					candidateInfoForLandingPages.add(candidateInfoForLandingPage);
					continue;
				}

				// First check if the candidate is tied to any batch which is having an assessment if not throw an
				// exception..

				String qPaperCode = qs.getQPForCandidate(batchCandidateAssociation);
				if (qPaperCode == null) {
					throw new CandidateRejectedException(ACSExceptionConstants.QUESTION_PAPER_NOT_FOUND,
							"Question paper is not available");
				}
				if (qPaperCode != null && !qPaperCode.isEmpty()) {
					candidateInfoForLandingPage.setQuestionCount(qs.getQuestionCountForQPWithoutRC(qPaperCode));
					candidateInfoForLandingPage.setQpInfo(getQpInfo(batchCandidateAssociation, qPaperCode));
				}
				if (batchCandidateAssociation.getCandidateBlockingStatus().equals(CandidateBlockingStatus.BLOCKED))
					throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_BLOCKED,
							"Candidate login blocked because of multiple invalid attempts");

				if (batch.getBatchType().equals(BatchTypeEnum.OPEN)
						&& (batchCandidateAssociation.getExpiryDateTime() == null || batchCandidateAssociation
								.getExpiryDateTime().before(Calendar.getInstance()))) {
					throw new CandidateRejectedException(ACSExceptionConstants.CANDIDATE_LOGIN_ID_EXPIRED,
							"Candidate login rejected because login Id not activated or its expired");
				}
				if (batch.getMaxBatchEndTime().before(Calendar.getInstance())) {
					boolean isCandidateBatchEndTimeExtended = false;
					logger.info("Batch is elapsed. Considering candidate batch End time:{}",
							batchCandidateAssociation.getExtendedBatchEndTimePerCandidate());
					// check for Candidate Level BatchEndTime whether it is elapsed or not
					if (batchCandidateAssociation.getExtendedBatchEndTimePerCandidate() != null
							&& batchCandidateAssociation.getExtendedBatchEndTimePerCandidate().before(
									Calendar.getInstance())) {
						throw new CandidateRejectedException(
								ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_BATCH_END_TIME,
								"Candidate login not allowed after extended candidate batch end time");
					} else if (batchCandidateAssociation.getExtendedBatchEndTimePerCandidate() == null) {
						logger.info("No Candidate batch Extension exists");
						isCandidateBatchEndTimeExtended = false;
					} else {
						logger.info("Candidate batch Extension exists for candidate:{} till {}",
								batchCandidateAssociation.getCandidateId(),
								batchCandidateAssociation.getExtendedBatchEndTimePerCandidate());
						isCandidateBatchEndTimeExtended = true;
					}

					if (!isCandidateBatchEndTimeExtended)
						throw new CandidateRejectedException(
								ACSExceptionConstants.CAND_LOGIN_NOT_ALLOWED_AFTER_BATCH_END_TIME,
								"Candidate login not allowed after batch end time");
				}

				candidateInfoForLandingPage.setCandidateStateEnum(CandidateStateEnum.CAN_START);

				if (batchCandidateAssociation.getActualTestStartedTime() != null
						&& batchCandidateAssociation.getClearLoginCount() > 0)
					candidateInfoForLandingPage.setCrashed(true);
				else
					candidateInfoForLandingPage.setCrashed(false);
				candidateInfoForLandingPages.add(candidateInfoForLandingPage);

			} catch (Exception ex) {
				candidateInfoForLandingPage.setCandidateStateEnum(CandidateStateEnum.CANNOT_START);
				candidateInfoForLandingPage.setMessage(ex.getMessage());
				candidateInfoForLandingPages.add(candidateInfoForLandingPage);

			}

		}
		// If one of the exam is in progress, mark the rest of the batches as cannot start
		// execute this loop every time
		if (isAnExamIsInProgress) {
			for (BatchesInfoForLandingPage batchesInfoForLandingPage : candidateInfoForLandingPages) {
				if (batchesInfoForLandingPage.getCandidateStateEnum().equals(CandidateStateEnum.CAN_START)) {
					String msg = "As an exam is already started, " + batchesInfoForLandingPage.getBatchCode()
							+ " batch exam cannot be started";
					logger.debug(msg);
					batchesInfoForLandingPage.setCandidateStateEnum(CandidateStateEnum.CANNOT_START);
					batchesInfoForLandingPage.setMessage(msg);
				}
			}
		}
		BatchInfoComparer batchInfoComparer = new BatchInfoComparer();

		// sort the list based on batch start time
		Collections.sort(candidateInfoForLandingPages, batchInfoComparer);
		return candidateInfoForLandingPages;
	}

	private QPInfo getQpInfo(AcsBatchCandidateAssociation batchCandidateAssociation, String qpId) {
		QPInfo qpInfo = qs.makeQPInfo(qpId, false, batchCandidateAssociation.getAssessmentCode());
		String preFixDownloadURL = getPreFixDownloadURL();
		String moduleName =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
		if (moduleName == null)
			moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;
		preFixDownloadURL = preFixDownloadURL + moduleName + "/";

		// Get the path to question paper which candidate is suppose to take
		String qpPath = null;

		QPInfo finalQpInfo = null;
		String secondaryQpPath = null;

		if (qpInfo != null) {
			if (qpInfo.getQpFileName() != null && qpInfo.getQpFileName() != null) {
				qpPath = batchCandidateAssociation.getBatchCode() + File.separator + qpInfo.getQpFileName();
				qpPath = qpPath.replace("\\", "/");
			}
			// Secondary QP
			if (qpInfo.getSecondaryQpFileName() != null && qpInfo.getSecondaryQpFileName() != null) {

				secondaryQpPath =
						batchCandidateAssociation.getBatchCode() + File.separator + qpInfo.getSecondaryQpFileName();
				secondaryQpPath = secondaryQpPath.replace("\\", "/");

				// try {
				// qs.saveOrUpdateQpCandAssociation(candidateAssosicationTO);
				// } catch (GenericDataModelException ex) {
				// throw new CandidateRejectedException(ACSExceptionConstants.QUESTION_PAPER_NOT_FOUND,
				// "QP candidate association save failed");
				// }
			}

			finalQpInfo =
					new QPInfo(qpInfo.getQpaperCode(), qpPath, qpInfo.getSecondaryLanguage(),
							qpInfo.getSecondaryQpaperCode(), secondaryQpPath);
			finalQpInfo.setIsPracticeTestRequired(qpInfo.getIsPracticeTestRequired());
			finalQpInfo.setIsPracticeTestTaken(qpInfo.getIsPracticeTestTaken());
			finalQpInfo.setPracticeTestPathForPrimary(qpInfo.getPracticeTestPathForPrimary());
			finalQpInfo.setPracticeTestPathForSecondary(qpInfo.getPracticeTestPathForSecondary());
			finalQpInfo.setMapOfSecondaryToPrimaryQuestion(qpInfo.getMapOfSecondaryToPrimaryQuestion());
			finalQpInfo.setQptName(qpInfo.getQptName());
		}
		return finalQpInfo;
	}

	/**
	 * @param candidateCredentialsDetailsDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchesInfoForLandingPage> getLandingPagesInfo(
			CandidateCredentialsDetailsDO candidateCredentialsDetailsDO) throws GenericDataModelException {
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				bs.getBatchCandidateAssociationsForUserName(candidateCredentialsDetailsDO.getUsername());
		return getAllBatchInfoForCandidate(batchCandidateAssociations);
	}

	/**
	 * Comparator for sorting the BatchesInfoForLandingPage
	 * 
	 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
	 * @since Apollo v2.0
	 * @see
	 */
	public class BatchInfoComparer implements Comparator<BatchesInfoForLandingPage> {

		@Override
		public int compare(BatchesInfoForLandingPage o1, BatchesInfoForLandingPage o2) {
			return o1.getBatchStartTime().compareTo(o2.getBatchStartTime());
		}

	}

	public LoginCredentials getLoginCredentialsforCurrentDryRunBatch() throws GenericDataModelException, ApolloSecurityException {
		AcsBatchCandidateAssociation abca = bcas.getCurrentUnusedBatchCandidateAssociation();
		LoginCredentials loginCredentials = null;
		if (abca != null) {
			loginCredentials = populateUNandPWD(abca, loginCredentials);
		}
		return loginCredentials;
	}

	private LoginCredentials populateUNandPWD(AcsBatchCandidateAssociation abca, LoginCredentials loginCredentials)
			throws GenericDataModelException, ApolloSecurityException {
		AcsEvent event = bs.getEventDetailsByBatchCode(abca.getBatchCode());
		if (abca != null) {
			CryptUtil cryptUtil = new CryptUtil(128);
			SecretKeySpec keySpec = null;
			if (eventCodeToKey.isEmpty() || !eventCodeToKey.containsKey(event.getEventCode())) {
				keySpec = cryptUtil.generateKeySpec(event.getEventCode());
				eventCodeToKey.put(event.getEventCode(), keySpec);
			} else
				keySpec = eventCodeToKey.get(event.getEventCode());
			String decryptedPasword = cryptUtil.decryptTextUsingAES(abca.getPassword(), keySpec);
			loginCredentials = new LoginCredentials(abca.getCandidateLogin(), decryptedPasword, abca.getBcaid());
		}
		return loginCredentials;
	}

	public LoginCredentials getUserNameAndPasswordOnBatchAndAssessmentCodes(String batchCode, String assessmentCode,
			String setId, String ip)
			throws GenericDataModelException, ApolloSecurityException, CandidateRejectedException {
		AcsBatchCandidateAssociation bca =
				bcas.getCurrentUnusedBatchCandidateAssociationOnBatchAndAssessment(batchCode, assessmentCode, setId);
		LoginCredentials loginCredentials = null;
		if (bca == null)
			throw new CandidateRejectedException(ACSExceptionConstants.NO_LOGIN_IDS_AVAILABLE,
					"No Login IDs available for this batch and assessment");

		loginCredentials = populateUNandPWD(bca, loginCredentials);
		CandidateCredentialsDetailsDO candidateCredentialsDetailsDO =
				new CandidateCredentialsDetailsDO(loginCredentials.getLoginName(), loginCredentials.getPassword(), ip,
						Calendar.getInstance().getTimeInMillis(), 0, false);
		CandidateAssessmentDO candidateAssessmentDO;
		candidateAssessmentDO = authenticateCandidate(candidateCredentialsDetailsDO);
		loginCredentials.setCandidateAssessmentDO(candidateAssessmentDO);
		return loginCredentials;
	}
}