package com.merittrac.apollo.acs.services;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.dataobject.CDEDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateCredentialDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateLoginCredentialsDO;
import com.merittrac.apollo.acs.dataobject.CandidateLoginCredentialsGenerationDO;
import com.merittrac.apollo.acs.dataobject.ExpiredLoginIdsDO;
import com.merittrac.apollo.acs.dataobject.GeneratedLoginIdsStatusDO;
import com.merittrac.apollo.acs.dataobject.LoginIdGenerationDashboardDO;
import com.merittrac.apollo.acs.dataobject.UnUsedLoginCredentialsDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.CandidateLoginCredentialsTO;
import com.merittrac.apollo.acs.entities.LoginIdAssessmentAssociationTO;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.CandidateLoginCredentialsGeneratorUtility;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateIdType;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * API's related candidate login id generation.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 */
public class CandidateLoginCredentialsGeneratorService extends BasicService {
	private static CandidateLoginCredentialsGeneratorService candidateLoginCredentialsGeneratorService = null;
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static ACSPropertyUtil acsPropertyUtil = null;
	private static CandidateService candidateService = null;
	private static BatchCandidateAssociationService batchCandidateAssociationService = null;
	private static CDEAService cdeaService = null;
	private static PackDetailsService packDetailsService = null;
	private static String isAuditEnable = null;
	private static CryptUtil cryptUtil = null;//
	private static BatchService batchService = null;

	static {
		acsPropertyUtil = new ACSPropertyUtil();
		candidateService = CandidateService.getInstance();
		batchCandidateAssociationService = new BatchCandidateAssociationService();
		cdeaService = new CDEAService();
		cryptUtil = new CryptUtil();
		packDetailsService = new PackDetailsService();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
		batchService = BatchService.getInstance();
	}

	private CandidateLoginCredentialsGeneratorService() {

	}

	/**
	 * Using double check singleton pattern
	 * 
	 * @return instance of batch service
	 * @since Apollo v2.0
	 */
	public static final CandidateLoginCredentialsGeneratorService getInstance() {
		if (candidateLoginCredentialsGeneratorService == null) {
			synchronized (CandidateLoginCredentialsGeneratorService.class) {
				if (candidateLoginCredentialsGeneratorService == null) {
					candidateLoginCredentialsGeneratorService = new CandidateLoginCredentialsGeneratorService();
				}
			}
		}
		return candidateLoginCredentialsGeneratorService;
	}

	/**
	 * populates the default data related to the selected assessment.
	 * 
	 * @param candidateLoginCredentialsGenerationDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LoginIdGenerationDashboardDO initiateCandidateLoginCredentialsGeneration(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		logger.info("initiated initiateCandidateLoginCredentialsGeneration where input={}",
				candidateLoginCredentialsGenerationDO);

		LoginIdGenerationDashboardDO loginIdGenerationDashboardDO =
				this.getAssessmentInfoForLoginIdGeneration(candidateLoginCredentialsGenerationDO.getAssessmentCode(),
						candidateLoginCredentialsGenerationDO.getBatchCode(),
						candidateLoginCredentialsGenerationDO.getSetId());

		logger.info("returning from initiateCandidateLoginCredentialsGeneration where output={}",
				loginIdGenerationDashboardDO);
		return loginIdGenerationDashboardDO;
	}

	/**
	 * starts the generation of candidate login credentials per assessment level. LoginIdAssessmentAssociationTO will be
	 * filled during BPACK activation and entries related to generatedLoginCount and lastGeneratedLoginId
	 * 
	 * @param candidateLoginCredentialsGenerationDOs
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 */
	public synchronized CandidateLoginCredentialsGenerationDO startCandidateLoginCredentialsGeneration(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO) throws Exception {
		logger.info("initiated startCandidateLoginCredentialsGeneration where input={}",
				candidateLoginCredentialsGenerationDO);

		// checks whether the packs for the specified batch are activated or not
		if (!packDetailsService.isAllPacksActivatedForBatch(candidateLoginCredentialsGenerationDO.getBatchCode())) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("Mandatory packs are not activated please check...");
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		// get the max number of login ids and validate with the generated count
		LoginIdAssessmentAssociationTO loginIdAssessmentAssociation =
				getLoginIdAssessmentAssociation(candidateLoginCredentialsGenerationDO.getAssessmentCode(),
						candidateLoginCredentialsGenerationDO.getBatchCode(),
						candidateLoginCredentialsGenerationDO.getSetId());

		long maxNumberOfAllowedLoginCredentials = loginIdAssessmentAssociation.getMaxNumberOfAllowedLoginIds();
		long generatedLoginCredentials = loginIdAssessmentAssociation.getGeneratedLoginIdCount();
		long requiredLoginCredentialsCount = candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount();

		if (generatedLoginCredentials == maxNumberOfAllowedLoginCredentials) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("Maximum limit = "
					+ loginIdAssessmentAssociation.getMaxNumberOfAllowedLoginIds() + " reached");
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		if ((generatedLoginCredentials + requiredLoginCredentialsCount) > maxNumberOfAllowedLoginCredentials) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("can not initiate login id generation as count = "
					+ requiredLoginCredentialsCount + " specified is greater than available limit = "
					+ (maxNumberOfAllowedLoginCredentials - generatedLoginCredentials));
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		String loginIdPrefix =
				new StringBuffer(acsPropertyUtil.getCandidateLoginIdPrefix()).append(acsPropertyUtil.getServerId())
						.toString();

		List<CandidateLoginCredentialsDO> candidateLoginCredentialsDOs =
				generateCandidateLoginCredentials(requiredLoginCredentialsCount, loginIdPrefix,
						loginIdAssessmentAssociation, candidateLoginCredentialsGenerationDO.getEventCode(),
						TimeUtil.convertStringToCalender(candidateLoginCredentialsGenerationDO.getExpiryTime()));

		// get division and customer info
		CDEDetailsDO cdeDetailsDO =
				cdeaService.getCustomerDivisionEventCodesByEventCode(candidateLoginCredentialsGenerationDO
						.getEventCode());

		// check whether admin audit is enable or not if enabled audit it
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams
					.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.LOGIN_IDS_GENERATION.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, candidateLoginCredentialsGenerationDO.getUserName());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, candidateLoginCredentialsGenerationDO.getIp());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE,
					candidateLoginCredentialsGenerationDO.getEventCode());

			if (candidateLoginCredentialsGenerationDO.getSetId() != null
					&& !candidateLoginCredentialsGenerationDO.getSetId().isEmpty()
					&& !candidateLoginCredentialsGenerationDO.getSetId().equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
				Object[] params =
						{
								candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount(),
								cdeaService.getAssessmentNameByAssessmentCode(candidateLoginCredentialsGenerationDO
										.getAssessmentCode()), candidateLoginCredentialsGenerationDO.getSetId(),
								candidateLoginCredentialsGenerationDO.getEventCode(), cdeDetailsDO.getDivisionName(),
								cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_SET_LEVEL_LOGIN_IDS_GENERATION_MSG, params);
			} else {
				Object[] params =
						{
								candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount(),
								cdeaService.getAssessmentNameByAssessmentCode(candidateLoginCredentialsGenerationDO
										.getAssessmentCode()), candidateLoginCredentialsGenerationDO.getEventCode(),
								cdeDetailsDO.getDivisionName(), cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_ASSESSMENT_LEVEL_LOGIN_IDS_GENERATION_MSG, params);
			}
		}

		candidateLoginCredentialsGenerationDO.setCandidateLoginCredentialsDOs(candidateLoginCredentialsDOs);
		candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_SUCCESS);

		logger.info("returning from startCandidateLoginCredentialsGeneration where output={}",
				candidateLoginCredentialsGenerationDO);
		return candidateLoginCredentialsGenerationDO;
	}

	/**
	 * enables the generated of candidate login credentials per assessment level. LoginIdAssessmentAssociationTO will be
	 * filled during BPACK activation and entries related to generatedLoginCount and lastGeneratedLoginId
	 * 
	 * @param candidateLoginCredentialsGenerationDOs
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 */
	public synchronized CandidateLoginCredentialsGenerationDO enableGeneratedLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO) throws Exception {
		logger.info("initiated startCandidateLoginCredentialsGeneration where input={}",
				candidateLoginCredentialsGenerationDO);

		// checks whether the packs for the specified batch are activated or not
		if (!packDetailsService.isAllPacksActivatedForBatch(candidateLoginCredentialsGenerationDO.getBatchCode())) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("Mandatory packs are not activated please check...");
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		// get the max number of login ids and validate with the generated count
		LoginIdAssessmentAssociationTO loginIdAssessmentAssociation =
				getLoginIdAssessmentAssociation(candidateLoginCredentialsGenerationDO.getAssessmentCode(),
						candidateLoginCredentialsGenerationDO.getBatchCode(),
						candidateLoginCredentialsGenerationDO.getSetId());

		if (loginIdAssessmentAssociation == null) {
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLoginCredentialsGenerationDO
					.setErrorMessage("LoginIdAssessmentAssociation has no candidates for walkin");
			return candidateLoginCredentialsGenerationDO;
		}
		long maxNumberOfAllowedLoginCredentials = loginIdAssessmentAssociation.getMaxNumberOfAllowedLoginIds();
		long generatedLoginCredentials = loginIdAssessmentAssociation.getGeneratedLoginIdCount();
		long requiredLoginCredentialsCount = candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount();
		String assessmentName =
				cdeaService
						.getAssessmentNameByAssessmentCode(candidateLoginCredentialsGenerationDO.getAssessmentCode());
		candidateLoginCredentialsGenerationDO
				.setAssessmentName(assessmentName == null ? candidateLoginCredentialsGenerationDO.getAssessmentCode()
						: assessmentName);
		if (generatedLoginCredentials == maxNumberOfAllowedLoginCredentials) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("Maximum limit = "
					+ loginIdAssessmentAssociation.getMaxNumberOfAllowedLoginIds() + " reached");
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		if ((generatedLoginCredentials + requiredLoginCredentialsCount) > maxNumberOfAllowedLoginCredentials) {
			candidateLoginCredentialsGenerationDO.setErrorMessage("can not initiate login id generation as count = "
					+ requiredLoginCredentialsCount + " specified is greater than available limit = "
					+ (maxNumberOfAllowedLoginCredentials - generatedLoginCredentials));
			candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateLoginCredentialsGenerationDO;
		}

		// String loginIdPrefix =
		// new StringBuffer(acsPropertyUtil.getCandidateLoginIdPrefix()).append(acsPropertyUtil.getServerId())
		// .toString();

		List<CandidateLoginCredentialsDO> candidateLoginCredentialsDOs =
				enableCandidateLoginCredentials(requiredLoginCredentialsCount, loginIdAssessmentAssociation,
						candidateLoginCredentialsGenerationDO.getAssessmentCode(),
						candidateLoginCredentialsGenerationDO.getBatchCode(),
						candidateLoginCredentialsGenerationDO.getSetId(),
						candidateLoginCredentialsGenerationDO.getEventCode(),
						TimeUtil.convertStringToCalender(candidateLoginCredentialsGenerationDO.getExpiryTime()));

		// get division and customer info
		CDEDetailsDO cdeDetailsDO =
				cdeaService.getCustomerDivisionEventCodesByEventCode(candidateLoginCredentialsGenerationDO
						.getEventCode());

		// check whether admin audit is enable or not if enabled audit it
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams
					.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.LOGIN_IDS_GENERATION.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, candidateLoginCredentialsGenerationDO.getUserName());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, candidateLoginCredentialsGenerationDO.getIp());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE,
					candidateLoginCredentialsGenerationDO.getEventCode());

			if (candidateLoginCredentialsGenerationDO.getSetId() != null
					&& !candidateLoginCredentialsGenerationDO.getSetId().isEmpty()
					&& !candidateLoginCredentialsGenerationDO.getSetId().equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
				Object[] params =
						{ candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount(), assessmentName,
								candidateLoginCredentialsGenerationDO.getSetId(),
								candidateLoginCredentialsGenerationDO.getEventCode(), cdeDetailsDO.getDivisionName(),
								cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_SET_LEVEL_LOGIN_IDS_GENERATION_MSG, params);
			} else {
				Object[] params =
						{ candidateLoginCredentialsGenerationDO.getRequiredLoginCredentialsCount(), assessmentName,
								candidateLoginCredentialsGenerationDO.getEventCode(), cdeDetailsDO.getDivisionName(),
								cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_ASSESSMENT_LEVEL_LOGIN_IDS_GENERATION_MSG, params);
			}
		}

		candidateLoginCredentialsGenerationDO.setCandidateLoginCredentialsDOs(candidateLoginCredentialsDOs);
		candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_SUCCESS);

		logger.info("returning from startCandidateLoginCredentialsGeneration where output={}",
				candidateLoginCredentialsGenerationDO);
		return candidateLoginCredentialsGenerationDO;
	}

	/**
	 * gets the loginId and assessment association information based on the batchId and assessmentId
	 * 
	 * @param assessmentCode
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public LoginIdAssessmentAssociationTO getLoginIdAssessmentAssociation(String assessmentCode, String batchCode,
			String setId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);

		String query =
				"FROM LoginIdAssessmentAssociationTO WHERE assessmentCode=(:assessmentCode) and batchCode=(:batchCode)";
		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"FROM LoginIdAssessmentAssociationTO WHERE assessmentCode=(:assessmentCode) and batchCode=(:batchCode) and setId=(:setId)";
		}

		LoginIdAssessmentAssociationTO candidateAssessmentAssociation =
				(LoginIdAssessmentAssociationTO) session.getByQuery(query, params);
		return candidateAssessmentAssociation;
	}

	/**
	 * gets the loginId and assessment association information based on the batchId and assessmentId
	 * 
	 * @param assessmentCode
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<LoginIdGenerationDashboardDO> getLoginIdAssessmentAssociationOnEventCode(String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);

		String query =
				"select l.assessmentcode as assessmentCode,a.assessmentname as assessmentName,  "
						+ " e.eventenddate as eventEndTime, acs.assessmentservername as serverName, l.batchcode as batchCode, b.batchName, "
						+ " l.generatedloginidcount as generatedLoginIdCount, l.maxnumberofallowedloginids as maxNumberOfAllowedLoginIds, "
						+ " setid as setId, qptname as qptName, l.loginIdAssessmentAssociationId as uniqueQPTIdentifier FROM acsloginidassessmentassociation l, acseventdetails e, acsassessmentserverconfig acs, "
						+ " acsassessmentdetails a, acsbatchdetails b WHERE l.batchcode =b.batchCode and b.eventCode =(:eventCode)"
						+ " and e.eventcode = (:eventCode) and e.eventcode=acs.eventcode and l.assessmentcode=a.assessmentcode";
		List<LoginIdGenerationDashboardDO> loginIdAssessmentAssociationTOs =
				(List<LoginIdGenerationDashboardDO>) session.getResultListByNativeSQLQuery(query, params,
						LoginIdGenerationDashboardDO.class);
	
		return loginIdAssessmentAssociationTOs;
	}

	/**
	 * generates the candidate login ids based on the specified count.
	 * 
	 * @param requiredLoginCredentialsCount
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	private List<CandidateLoginCredentialsDO> generateCandidateLoginCredentials(long requiredLoginCredentialsCount,
			String loginIdPrefix, LoginIdAssessmentAssociationTO loginIdAssessmentAssociation, String eventCode,
			Calendar expiryDateTime) throws GenericDataModelException {
		List<CandidateLoginCredentialsDO> candidateLoginCredentialsDOs = new ArrayList<CandidateLoginCredentialsDO>();
		Set<CandidateLoginCredentialsTO> candidateLoginCredentialsTOs =
				new LinkedHashSet<CandidateLoginCredentialsTO>();

		CandidateLoginCredentialsGeneratorUtility candidateLoginIdsGenerator =
				CandidateLoginCredentialsGeneratorUtility.getInstance();

		String sExpiryDateTime = TimeUtil.convertTimeAsString(expiryDateTime.getTimeInMillis());
		String lastGeneratedLoginId = getLastGeneratedLoginId();
		String password = null;
		for (long count = requiredLoginCredentialsCount; count > 0; count--) {
			try {
				lastGeneratedLoginId = candidateLoginIdsGenerator.generateNextLoginId(lastGeneratedLoginId);
				password = candidateLoginIdsGenerator.generateRandomPassword();

				CandidateLoginCredentialsDO candidateLoginCredentialsDO =
						new CandidateLoginCredentialsDO(
								(new StringBuffer(loginIdPrefix).append(lastGeneratedLoginId)).toString(), password);
				candidateLoginCredentialsDO.setExpiryDateTime(sExpiryDateTime);
				candidateLoginCredentialsDOs.add(candidateLoginCredentialsDO);

				CandidateLoginCredentialsTO candidateLoginCredentialsTO =
						new CandidateLoginCredentialsTO(/* loginIdPrefix, */lastGeneratedLoginId, password);
				candidateLoginCredentialsTO.setExpiryDateTime(expiryDateTime);
				// Maintaining Generated Date Time for Candidate Login Ids
				candidateLoginCredentialsTO.setGeneratedDateTime(new Date());
				candidateLoginCredentialsTOs.add(candidateLoginCredentialsTO);

				populateCandidateData(loginIdAssessmentAssociation, candidateLoginCredentialsDO, eventCode,
						expiryDateTime);
			} catch (Exception e) {
				System.out.println("Exception while executing generateLoginIds..." + e);
			}
		}

		loginIdAssessmentAssociation.setGeneratedLoginIdCount(loginIdAssessmentAssociation.getGeneratedLoginIdCount()
				+ requiredLoginCredentialsCount);
		// commenting coz candidate login ids are generated from DM
		// if (loginIdAssessmentAssociation.getCandidateLoginCredentials() != null
		// && !loginIdAssessmentAssociation.getCandidateLoginCredentials().isEmpty()) {
		// loginIdAssessmentAssociation.getCandidateLoginCredentials().addAll(candidateLoginCredentialsTOs);
		// } else {
		// loginIdAssessmentAssociation.setCandidateLoginCredentials(candidateLoginCredentialsTOs);
		// }

		updateLoginIdAssessmentAssociation(loginIdAssessmentAssociation);
		return candidateLoginCredentialsDOs;
	}

	/**
	 * enables the candidate login ids based on the specified count.
	 * 
	 * @param requiredLoginCredentialsCount
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * @throws ApolloSecurityException
	 * 
	 * @since Apollo v2.0
	 */
	private List<CandidateLoginCredentialsDO> enableCandidateLoginCredentials(long requiredLoginCredentialsCount,
			LoginIdAssessmentAssociationTO loginIdAssessmentAssociation, String assessmentCode, String batchCode,
			String setId, String eventCode, Calendar expiryDateTime) throws GenericDataModelException,
			CandidateRejectedException, ApolloSecurityException {
		List<CandidateLoginCredentialsDO> candidateLoginCredentialsDOs = new ArrayList<CandidateLoginCredentialsDO>();
		Set<CandidateLoginCredentialsTO> candidateLoginCredentialsTOs =
				new LinkedHashSet<CandidateLoginCredentialsTO>();

		String sExpiryDateTime = TimeUtil.convertTimeAsString(expiryDateTime.getTimeInMillis());

		for (long count = requiredLoginCredentialsCount; count > 0; count--) {

			AcsBatchCandidateAssociation acsBatchCandidateAssociation =
					batchCandidateAssociationService.getBatchCandAssoDisabledWalkinCandidates(batchCode,
							assessmentCode, setId);
			if (acsBatchCandidateAssociation == null) {
				throw new CandidateRejectedException(ACSExceptionConstants.NO_CANDIDATE_EXISTS,
						"No Candidate Present in BatchCandidateAssociation");
			}
			String candidateLoginId = acsBatchCandidateAssociation.getCandidateLogin();
			String password = acsBatchCandidateAssociation.getPassword();
			CandidateLoginCredentialsDO candidateLoginCredentialsDO =
					new CandidateLoginCredentialsDO(candidateLoginId.toString(), cryptUtil.decryptTextUsingAES(
							password, eventCode));
			candidateLoginCredentialsDO.setExpiryDateTime(sExpiryDateTime);
			candidateLoginCredentialsDO.setCandidateId(acsBatchCandidateAssociation.getCandidateId());
			candidateLoginCredentialsDOs.add(candidateLoginCredentialsDO);

			CandidateLoginCredentialsTO candidateLoginCredentialsTO =
					new CandidateLoginCredentialsTO(candidateLoginId, password);
			candidateLoginCredentialsTO.setExpiryDateTime(expiryDateTime);
			// Maintaining Generated Date Time for Candidate Login Ids
			candidateLoginCredentialsTO.setGeneratedDateTime(new Date());
			candidateLoginCredentialsTOs.add(candidateLoginCredentialsTO);

			updateBatchCandidateAsso(acsBatchCandidateAssociation, expiryDateTime);

		}

		loginIdAssessmentAssociation.setGeneratedLoginIdCount(loginIdAssessmentAssociation.getGeneratedLoginIdCount()
				+ requiredLoginCredentialsCount);
		// if (loginIdAssessmentAssociation.getCandidateLoginCredentials() != null
		// && !loginIdAssessmentAssociation.getCandidateLoginCredentials().isEmpty()) {
		// loginIdAssessmentAssociation.getCandidateLoginCredentials().addAll(candidateLoginCredentialsTOs);
		// } else {
		// loginIdAssessmentAssociation.setCandidateLoginCredentials(candidateLoginCredentialsTOs);
		// }

		updateLoginIdAssessmentAssociation(loginIdAssessmentAssociation);
		return candidateLoginCredentialsDOs;
	}

	/**
	 * updates the login id to assessment association
	 * 
	 * @param loginIdAssessmentAssociationTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean updateLoginIdAssessmentAssociation(LoginIdAssessmentAssociationTO loginIdAssessmentAssociationTO)
			throws GenericDataModelException {
		session.merge(loginIdAssessmentAssociationTO);
		return true;
	}

	/**
	 * persist the login id to assessment association
	 * 
	 * @param loginIdAssessmentAssociationTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean saveLoginIdAssessmentAssociation(LoginIdAssessmentAssociationTO loginIdAssessmentAssociationTO)
			throws GenericDataModelException {
		session.persist(loginIdAssessmentAssociationTO);
		return true;
	}

	/**
	 * populates the default data in candidate related tables based on the generated login ids.
	 * 
	 * @param loginIdAssessmentAssociationTO
	 * @param candidateLoginCredentialsDO
	 * @param eventCode
	 * @param expiryDateTime
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean populateCandidateData(LoginIdAssessmentAssociationTO loginIdAssessmentAssociationTO,
			CandidateLoginCredentialsDO candidateLoginCredentialsDO, String eventCode, Calendar expiryDateTime)
			throws GenericDataModelException {

		AcsCandidate candidate = new AcsCandidate();

		candidate.setFirstName(ACSConstants.NOT_APPLICABLE);
		candidate.setLastName(ACSConstants.NOT_APPLICABLE);
		candidate.setAcseventdetails(cdeaService.getEventDetailsByEventCode(eventCode));
		candidate.setIdentifier1(candidateLoginCredentialsDO.getLoginId());
		candidate.setIdentifierType1(CandidateIdType.APPLICATION_NUMBER);
		// candidate.setDob(Calendar.getInstance());
		candidate.setGender("");
		candidateService.setCandidate(candidate);

		try {
			AcsBatchCandidateAssociation batchCandidateAssociation =
					new AcsBatchCandidateAssociation(candidateLoginCredentialsDO.getLoginId(),
							cryptUtil.encryptTextUsingAES(candidateLoginCredentialsDO.getPassword(), eventCode),
							loginIdAssessmentAssociationTO.getAssessmentCode(),
							loginIdAssessmentAssociationTO.getBatchCode(), candidate.getCandidateId());
			batchCandidateAssociation.setSetID(loginIdAssessmentAssociationTO.getSetId());
			batchCandidateAssociation.setCandidateType(CandidateType.WALKIN);
			batchCandidateAssociation.setExpiryDateTime(expiryDateTime.getTime());
			batchCandidateAssociationService.setBatchCandidateAssociation(batchCandidateAssociation);
			return true;
		} catch (ApolloSecurityException e) {
			logger.error("ApolloSecurityException while executing encryptPassword...", e);
		}

		return false;
	}

	/**
	 * updates the isenable and expirydate into BatchCandidateAsso table based on the generated login ids
	 * 
	 * @param batchCandidateAssociation
	 * @param expiryDateTime
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateBatchCandidateAsso(AcsBatchCandidateAssociation batchCandidateAssociation,
			Calendar expiryDateTime) throws GenericDataModelException {

		batchCandidateAssociation.setEnable(true);
		batchCandidateAssociation.setEnabledDateTime(Calendar.getInstance().getTime());
		batchCandidateAssociation.setExpiryDateTime(expiryDateTime.getTime());
		batchCandidateAssociationService.updateBatchCandidateAssociation(batchCandidateAssociation);
		return true;

	}

	/**
	 * fetches the last generated login id
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public String getLastGeneratedLoginId() throws GenericDataModelException {
		String lastGeneratedLoginId =
				(String) session.getUniqueResultByNativeSQLQuery(
						"SELECT loginId FROM acsCandidateLoginCredentials ORDER BY loginId DESC", null);
		return lastGeneratedLoginId;
	}

	/**
	 * gets the loginId and assessment association information based on the batchId and assessmentId
	 * 
	 * @param assessmentCode
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public LoginIdGenerationDashboardDO getAssessmentInfoForLoginIdGeneration(String assessmentCode, String batchCode,
			String setId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);

		String query =
				"SELECT l.batchcode as batchCode, b.batchName, l.assessmentcode as assessmentCode, l.qptname as qpaperCode, a.assessmentname as assessmentName,l.setid "
						+ " as setId,l.maxnumberofallowedloginids as maxNumberOfAllowedLoginIds,l.generatedloginidcount as "
						+ " generatedLoginIdCount,STR_TO_DATE(greatest(IFNULL(b.deltarpackgenerationtime,0),IFNULL "
						+ " (b.deltarpackgenerationtimebyacs,0)),'%Y-%m-%d %T') as maxAllowedExpiryDateTime FROM "
						+ " acsloginidassessmentassociation l , acsassessmentdetails a,acsbatchdetails b  where l.assessmentcode= "
						+ " (:assessmentCode) and l.batchcode=(:batchCode) and l.assessmentcode=a.assessmentcode and b.batchcode=l.batchcode";
		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"SELECT l.batchcode as batchCode,l.assessmentcode as assessmentCode, l.qptname as qpaperCode, a.assessmentname as assessmentName, "
							+ " l.setid as setId,l.maxnumberofallowedloginids as maxNumberOfAllowedLoginIds,l.generatedloginidcount as "
							+ " generatedLoginIdCount,STR_TO_DATE(greatest(IFNULL(b.deltarpackgenerationtime,0),IFNULL "
							+ " (b.deltarpackgenerationtimebyacs,0)),'%Y-%m-%d %T') as maxAllowedExpiryDateTime FROM "
							+ " acsloginidassessmentassociation l , acsassessmentdetails a,acsbatchdetails b  where l.assessmentcode= "
							+ " (:assessmentCode) and l.batchcode=(:batchCode) and l.assessmentcode=a.assessmentcode and b.batchcode= "
							+ " l.batchcode and l.setid=(:setId)";
		}

		LoginIdGenerationDashboardDO loginIdGenerationDashboardDO =
				(LoginIdGenerationDashboardDO) session.getUniqueResultByNativeSQLQuery(query, params,
						LoginIdGenerationDashboardDO.class);
		// if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
		// List<String> qpapercodes = new ArrayList<>();
		// qpapercodes.add(questionService.getQpForSet(assessmentCode, setId));
		// loginIdGenerationDashboardDO.setQpaperCode(qpapercodes);
		// } else if (setId == null || setId.isEmpty() || setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS))
		// loginIdGenerationDashboardDO.setQpaperCode(questionService.getQpsForAssessment(assessmentCode));

		return loginIdGenerationDashboardDO;
	}

	/**
	 * fetches the list of unused login ids.
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public UnUsedLoginCredentialsDO getUnUsedCandidateLoginCredentials(String batchCode, String assessmentCode,
			String setId, int pageNumber, int pageCount, String searchData, String sortingColumn, String orderType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("assessmentCode", assessmentCode);

		String query =
				"SELECT l.batchcode as batchCode,l.assessmentcode as assessmentCode,a.assessmentname as assessmentName,l.setid as "
						+ " setId, a.eventcode as eventCode, bca.candidatelogin as loginId,bca.password as password,bca.expirydatetime as expiryTime,(case when "
						+ " bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) as status FROM "
						+ " acsloginidassessmentassociation "
						+ " l join acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and "
						+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
						+ "  l.assessmentcode=(:assessmentCode) and "
						+ " l.batchcode=(:batchCode) and  bca.logintime is null and bca.isenable=true";

		String queryCount =
				"SELECT count(*)  FROM "
						+ " acsloginidassessmentassociation "
						+ " l join acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and "
						+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
						+ "  l.assessmentcode=(:assessmentCode) and "
						+ " l.batchcode=(:batchCode) and  bca.logintime is null and bca.isenable=true";

		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"SELECT l.batchcode as batchCode,l.assessmentcode as assessmentCode,a.assessmentname as assessmentName, "
							+ " a.eventcode as eventCode, l.setid as "
							+ " setId,bca.candidatelogin as loginId,bca.password as password,bca.expirydatetime as expiryTime,(case when "
							+ " bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) as status FROM "
							+ " acsloginidassessmentassociation l join "
							+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and bca.setid=l.setid and "
							+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
							+ " l.assessmentcode=(:assessmentCode) and "
							+ " l.batchcode=(:batchCode) and bca.logintime is null and bca.isenable=true"
							+ " and l.setid=(:setId) ";

			queryCount =
					"SELECT count(*) FROM "
							+ " acsloginidassessmentassociation l join "
							+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and bca.setid=l.setid and "
							+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
							+ " l.assessmentcode=(:assessmentCode) and "
							+ " l.batchcode=(:batchCode) and bca.logintime is null and bca.isenable=true"
							+ " and l.setid=(:setId) ";
		}

		if (searchData != null && !searchData.isEmpty()) {
			query =
					query + " and (bca.candidatelogin like '%" + searchData + "%' or bca.password like '%" + searchData
							+ "%' or " + " bca.expirydatetime like'%" + searchData
							+ "%' or (case when bca.expirydatetime<=(:currentDateTime) Then "
							+ " 'Expired' else 'Active' end)like '%" + searchData + "%' )";
			queryCount =
					queryCount + " and (bca.candidatelogin like '%" + searchData + "%' or bca.password like '%"
							+ searchData + "%' or " + " bca.expirydatetime like'%" + searchData
							+ "%' or (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' "
							+ " else 'Active' end) like '%" + searchData + "%')";
		}

		if (sortingColumn != null && orderType != null) {
			query = query + " order by " + sortingColumn + " " + orderType;
		}
		int tCount = ((BigInteger) session.getUniqueResultByNativeSQLQuery(queryCount, params)).intValue();
		// pagination process check
		params.put("currentDateTime", Calendar.getInstance());
		List<UnUsedLoginCredentialsDO> unUsedLoginCredentialsDOs =
				session.getResultListByNativeSQLQuery(query, params, pageNumber, pageCount,
						UnUsedLoginCredentialsDO.class);
		if (unUsedLoginCredentialsDOs != null) {
			try {
				for (UnUsedLoginCredentialsDO unUsedLoginCredentialsDO : unUsedLoginCredentialsDOs) {
					unUsedLoginCredentialsDO.setPassword(cryptUtil.decryptTextUsingAES(
							unUsedLoginCredentialsDO.getPassword(), unUsedLoginCredentialsDO.getEventCode()));
				}
			} catch (ApolloSecurityException e) {
				logger.error("ApolloSecurityException while executing getUnUsedCandidateLoginCredentials...", e);
			}
		}
		UnUsedLoginCredentialsDO unUsedLoginCredentialsDOsCount =
				new UnUsedLoginCredentialsDO(unUsedLoginCredentialsDOs, tCount);

		return unUsedLoginCredentialsDOsCount;

	}

	/**
	 * fetches the list of unused login ids.
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<UnUsedLoginCredentialsDO> getActiveCandidateLoginCredentials(String batchCode, String assessmentCode,
			String setId, int pageNumber, int pageCount, String searchData, String sortingColumn, String orderType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("currentDateTime", Calendar.getInstance());

		String query =
				"SELECT l.batchcode as batchCode,l.assessmentcode as assessmentCode,a.assessmentname as assessmentName,l.setid as "
						+ " setId,a.eventcode as eventCode,bca.candidatelogin as loginId,bca.password as password,bca.expirydatetime "
						+ " as expiryTime, bca.enableddatetime as generatedTime, 'Active' as status FROM acsloginidassessmentassociation l "
						+ " join acsbatchcandidateassociation "
						+ " bca on bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode join acsassessmentdetails a "
						+ " on bca.assessmentcode=a.assessmentcode where "
						+ "  l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.expirydatetime>(:currentDateTime) and "
						+ " bca.logintime is null";
		String queryCount =
				"SELECT count(*) FROM acsloginidassessmentassociation l join acsbatchcandidateassociation "
						+ " bca on bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode join acsassessmentdetails a "
						+ " on bca.assessmentcode=a.assessmentcode where "
						+ "  l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.expirydatetime>(:currentDateTime) and "
						+ " bca.logintime is null";

		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"SELECT l.batchcode as batchCode,l.assessmentcode as assessmentCode,a.assessmentname as assessmentName,l.setid as "
							+ " setId,a.eventcode as eventCode,bca.candidatelogin as loginId,bca.password as password,bca.expirydatetime as expiryTime, "
							+ " bca.enableddatetime as generatedTime,  'Active' as "
							+ " status FROM acsloginidassessmentassociation l join acsbatchcandidateassociation "
							+ " bca on bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode and bca.setid=l.setid "
							+ " join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
							+ "  l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.expirydatetime>(:currentDateTime) and "
							+ " bca.logintime is null and l.setid=(:setId) ";
			queryCount =
					"SELECT count(*) FROM acsloginidassessmentassociation l join acsbatchcandidateassociation "
							+ " bca on bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode and bca.setid=l.setid "
							+ " join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode where "
							+ "  l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.expirydatetime>(:currentDateTime) and "
							+ " bca.logintime is null and l.setid=(:setId) ";
		}

		if (searchData != null && !searchData.isEmpty()) {
			query =
					query + "and (a.assessmentname like '%" + searchData + "%' or l.setid like '%" + searchData
							+ "%' or bca.candidatelogin " + " like '%" + searchData + "%' or bca.password like '%"
							+ searchData + "%' or bca.expirydatetime like '%" + searchData + "%' "
							+ " or 'Active' like '%" + searchData + "%' )";

			queryCount =
					queryCount + "and( a.assessmentname like '%" + searchData + "%' or l.setid like '%" + searchData
							+ "%' or " + " bca.candidatelogin like '%" + searchData + "%' or bca.password like '%"
							+ searchData + "%' or bca.expirydatetime like " + " '%" + searchData
							+ "%' or 'Active' like '%" + searchData + "%') ";
		}

		if (sortingColumn != null && orderType != null) {
			query = query + " order by " + sortingColumn + " " + orderType;

		}

		int tCount = ((BigInteger) session.getUniqueResultByNativeSQLQuery(queryCount, params)).intValue();
		// pagination process check
		List<UnUsedLoginCredentialsDO> unUsedLoginCredentialsDOs =
				session.getResultListByNativeSQLQuery(query, params, pageNumber, pageCount,
						UnUsedLoginCredentialsDO.class);

		if (unUsedLoginCredentialsDOs != null) {
			try {
				for (UnUsedLoginCredentialsDO unUsedLoginCredentialsDO : unUsedLoginCredentialsDOs) {
					unUsedLoginCredentialsDO.setPassword(cryptUtil.decryptTextUsingAES(
							unUsedLoginCredentialsDO.getPassword(), unUsedLoginCredentialsDO.getEventCode()));
				}
			} catch (ApolloSecurityException e) {
				logger.error("ApolloSecurityException while executing getUnUsedCandidateLoginCredentials...", e);
			}
		}
		List<UnUsedLoginCredentialsDO> unUsedLoginCredentialsDOCount = new ArrayList<>();
		unUsedLoginCredentialsDOCount.add(new UnUsedLoginCredentialsDO(unUsedLoginCredentialsDOs, tCount));
		return unUsedLoginCredentialsDOCount;
	}

	/**
	 * get expired login ids information
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ExpiredLoginIdsDO getExpiredCandidateLoginCredentials(String batchCode, String assessmentCode, String setId,
			int pageNumber, int pageCount, String searchData, String sortingColumn, String orderType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("currentDateTime", Calendar.getInstance());

		String query =
				"SELECT a.assessmentname as assessmentName,l.setid as setId,bca.candidatelogin as loginId,bca.expirydatetime "
						+ " as expiryTime, 'Expired' as status FROM acsloginidassessmentassociation l join "
						+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and "
						+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode "
						+ " where l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.logintime is null "
						+ " and bca.expirydatetime<=  (:currentDateTime)";
		String queryCount =
				"SELECT count(*) FROM acsloginidassessmentassociation l join "
						+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and "
						+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode "
						+ " where l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.logintime is null "
						+ " and bca.expirydatetime<=  (:currentDateTime)";

		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"SELECT a.assessmentname as assessmentName,l.setid as setId,bca.candidatelogin as loginId,bca.expirydatetime "
							+ " as expiryTime, 'Expired' as status FROM acsloginidassessmentassociation l join "
							+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and bca.setid=l.setid and "
							+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode "
							+ " where l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.logintime is null "
							+ " and bca.expirydatetime<=  (:currentDateTime) and l.setid=(:setId)";
			queryCount =
					"SELECT count(*) FROM acsloginidassessmentassociation l join "
							+ " acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and bca.setid=l.setid and "
							+ " bca.batchcode=l.batchcode join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode "
							+ " where l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) and bca.logintime is null "
							+ " and bca.expirydatetime<=  (:currentDateTime) and l.setid=(:setId)";

		}

		if (searchData != null && !searchData.isEmpty()) {

			query =
					query + " and ( bca.candidatelogin like '%" + searchData + "%' or  bca.expirydatetime like '%"
							+ searchData + "%' or " + " 'Expired'  like '%" + searchData + "%' )";
			queryCount =
					queryCount + " and (bca.candidatelogin like '%" + searchData + "%' or  bca.expirydatetime like "
							+ " '%" + searchData + "%' or  'Expired'  like '%" + searchData + "%' )";
		}

		if (sortingColumn != null && orderType != null) {

			query = query + " order by " + sortingColumn + " " + orderType;
		}

		int tCount = ((BigInteger) session.getUniqueResultByNativeSQLQuery(queryCount, params)).intValue();
		// pagination process check
		List<ExpiredLoginIdsDO> expiredLoginIdsDOs =
				session.getResultListByNativeSQLQuery(query, params, pageNumber, pageCount, ExpiredLoginIdsDO.class);

		ExpiredLoginIdsDO expiredLoginIdsDOsCount = new ExpiredLoginIdsDO(expiredLoginIdsDOs, tCount);
		return expiredLoginIdsDOsCount;

	}

	/**
	 * gets the list of generated login ids with the status like started, not started, expired
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public GeneratedLoginIdsStatusDO getGeneratedCandidateLoginCredentials(String batchCode, String assessmentCode,
			String setId, int pageNumber, int pageCount, String searchData, String sortingColumn, String orderType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("currentDateTime", Calendar.getInstance());

		String query =
				"SELECT concat(c.firstname,' ',c.lastname) as candidateName,c.identifier1 as candidateId, "
						+ " a.assessmentname as assessmentName,l.setid as setId, "
						+ " bca.candidatelogin as loginId,bca.password as   "
						+ "	  password,a.eventcode as eventCode,bca.expirydatetime as expiryDateTime,  "
						+ "	  (case when bca.logintime is not null Then 'Used' else  "
						+ "	  (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) as status FROM "
						+ "	  acsloginidassessmentassociation l  "
						+ " join acsbatchcandidateassociation bca on bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode "
						+ " join acscandidate c on bca.candidateid=c.candidateid      "
						+ " join acsassessmentdetails a on bca.assessmentcode=a.assessmentcode      "
						+ " WHERE l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode)    "
						+ "	 and bca.isenable=true ";

		String queryCount =
				"SELECT count(case when bca.logintime is not null Then 'Used' else "
						+ " (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) as status FROM "
						+ " acsloginidassessmentassociation l join acsbatchcandidateassociation bca on "
						+ " bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode "
						+ " join acscandidate c on bca.candidateid=c.candidateid      "
						+ " join acsassessmentdetails a on "
						+ " bca.assessmentcode=a.assessmentcode WHERE l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) "
						+ "  and bca.isenable=true";
		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			query =
					"SELECT concat(c.firstname,' ',c.lastname) as candidateName,c.identifier1 as candidateId, "
							+ "  a.assessmentname as assessmentName,a.eventcode as eventCode,l.setid as setId,bca.candidatelogin as loginId,bca.password as "
							+ " password,bca.expirydatetime as expiryDateTime,(case when bca.logintime is not null Then 'Used' else "
							+ " (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) as status FROM "
							+ " acsloginidassessmentassociation l join acsbatchcandidateassociation bca on "
							+ " bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode and bca.setid=l.setid "
							+ " join acscandidate c on bca.candidateid=c.candidateid      "
							+ " join acsassessmentdetails a on "
							+ " bca.assessmentcode=a.assessmentcode WHERE l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) "
							+ "  and bca.isenable=true and l.setid=(:setId)";
			queryCount =
					"SELECT count(case when bca.logintime is not null Then 'Used' else "
							+ " (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) as status FROM "
							+ " acsloginidassessmentassociation l join acsbatchcandidateassociation bca on "
							+ " bca.assessmentcode=l.assessmentcode and bca.batchcode=l.batchcode and bca.setid=l.setid "
							+ " join acscandidate c on bca.candidateid=c.candidateid      "
							+ " join acsassessmentdetails a on "
							+ " bca.assessmentcode=a.assessmentcode WHERE l.assessmentcode=(:assessmentCode) and l.batchcode=(:batchCode) "
							+ "  and bca.isenable=true and l.setid=(:setId)";
		}
		// int count = 0;
		if (searchData != null && !searchData.isEmpty()) {

			query =
					query
							+ " and (bca.candidatelogin like '%"
							+ searchData
							+ "%' or bca.password like '%"
							+ searchData
							+ "%' or concat(c.firstname,' ',c.lastname) like '%" + searchData
							+ "%' or "
							+ " bca.expirydatetime like '%"
							+ searchData
							+ "%' or (case when (bca.logintime is not null) Then "
							+ " 'Used' else (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) "
							+ " like '%" + searchData + "%' )";
			queryCount =
					queryCount
							+ " and (bca.candidatelogin like '%"
							+ searchData
							+ "%' or bca.password like '%"
							+ searchData + "%' or concat(c.firstname,' ',c.lastname) like '%" + searchData
							+ "%' "
							+ " or bca.expirydatetime like '%"
							+ searchData
							+ "%' or (case when (bca.logintime is not null) Then "
							+ " 'Used' else (case when bca.expirydatetime<=(:currentDateTime) Then 'Expired' else 'Active' end) end) "
							+ " like '%" + searchData + "%' )";

		}
		if (sortingColumn != null && orderType != null) {
			query = query + " order by " + sortingColumn + " " + orderType;

		}

		int tCount = ((BigInteger) session.getUniqueResultByNativeSQLQuery(queryCount, params)).intValue();
		// pagination process check
		List<GeneratedLoginIdsStatusDO> generatedLoginIdsStatusDOs =
				session.getResultListByNativeSQLQuery(query, params, pageNumber, pageCount,
						GeneratedLoginIdsStatusDO.class);

		if (generatedLoginIdsStatusDOs != null) {
			try {
				for (GeneratedLoginIdsStatusDO generatedLoginIdsStatusDO : generatedLoginIdsStatusDOs) {
					generatedLoginIdsStatusDO.setPassword(cryptUtil.decryptTextUsingAES(
							generatedLoginIdsStatusDO.getPassword(), generatedLoginIdsStatusDO.getEventCode()));
				}
			} catch (ApolloSecurityException e) {
				logger.error("ApolloSecurityException while executing getUnUsedCandidateLoginCredentials...", e);
			}
		}

		GeneratedLoginIdsStatusDO generatedLoginIdsStatusDOsCount =
				new GeneratedLoginIdsStatusDO(generatedLoginIdsStatusDOs, tCount);

		return generatedLoginIdsStatusDOsCount;
	}

	/**
	 * get Candidate Credential Details
	 * 
	 * @param candidateCredentialDetailsDO
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public CandidateCredentialDetailsDO getCandidateCredentialDetails(
			CandidateCredentialDetailsDO candidateCredentialDetailsDO) {
		logger.debug("--IN-- getCandidateCredentialDetails with candidateCredentialDetailsDO : {}",
				candidateCredentialDetailsDO);
		try {
		if (candidateCredentialDetailsDO == null) {
			logger.debug("getCandidateCredentialDetails with null values with candidateCredentialDetailsDO : {}",
					candidateCredentialDetailsDO);
			candidateCredentialDetailsDO =
					new CandidateCredentialDetailsDO(ACSConstants.MSG_FAILURE,
							"getCandidateCredentialDetails with null values");
			return candidateCredentialDetailsDO;
		}
		String batchCode = candidateCredentialDetailsDO.getBatchCode();
		AcsEvent acsEvent = cdeaService.getEventDetailsByBatchCode(batchCode);
		if (acsEvent == null) {
			logger.debug("getCandidateCredentialDetails no event details found candidateCredentialDetailsDO : {}",
					candidateCredentialDetailsDO);
			candidateCredentialDetailsDO.setErrorMessage("No Event details found with specified batch code");
			candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateCredentialDetailsDO;
		}
			candidateCredentialDetailsDO = getNewlyGeneratedCandidateDetails(candidateCredentialDetailsDO, acsEvent);
		} catch (Exception e) {
			candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateCredentialDetailsDO.setErrorMessage(e.getMessage());
		}
		return candidateCredentialDetailsDO;
	}

	/**
	 * get Newly Generated Candidate Details
	 * 
	 * @param candidateCredentialDetailsDO
	 * @param acsEvent
	 * @return
	 * @throws GenericDataModelException
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private CandidateCredentialDetailsDO getNewlyGeneratedCandidateDetails(
			CandidateCredentialDetailsDO candidateCredentialDetailsDO, AcsEvent acsEvent)
			throws GenericDataModelException, Exception {
		logger.debug("--IN-- getNewlyGeneratedCandidateDetails() with candidateCredentialDetailsDO : {}",
				candidateCredentialDetailsDO);

		String assessmentCode = candidateCredentialDetailsDO.getAssessmentCode();
		String batchCode = candidateCredentialDetailsDO.getBatchCode();

		// acsEvent.getEventEndDate()
		// 25-Jan-2016 15:04:14
		// dd-MMM-yyyy HH:mm:ss
		Calendar eventEndTime = acsEvent.getEventEndDate();
		String expiryTime =
				TimeUtil.convertTimeAsString(eventEndTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);

		CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO =
				new CandidateLoginCredentialsGenerationDO(acsEvent.getEventCode(),
						batchCode, assessmentCode, "ALL",
						ACSConstants.CANDIDATES_COUNT_TO_ENABLE, expiryTime,
						ACSConstants.HGS_CUSTOMER_NAME_FOR_AUDIT_LOGS, NetworkUtility.getIp());
		candidateLoginCredentialsGenerationDO = enableGeneratedLoginIds(candidateLoginCredentialsGenerationDO);

		if (candidateLoginCredentialsGenerationDO == null
				|| candidateLoginCredentialsGenerationDO.getStatus() == null
				|| !candidateLoginCredentialsGenerationDO.getStatus().equalsIgnoreCase(ACSConstants.MSG_SUCCESS)
				|| candidateLoginCredentialsGenerationDO.getCandidateLoginCredentialsDOs() == null
				|| candidateLoginCredentialsGenerationDO.getCandidateLoginCredentialsDOs().isEmpty()) {
			logger.debug("failure in login id generation with candidateLoginCredentialsGenerationDO : {}",
					candidateLoginCredentialsGenerationDO);
			candidateCredentialDetailsDO.setErrorMessage("failure in login id generation");
			candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateCredentialDetailsDO;
		}
		// get of zero coz CANDIDATES_COUNT_TO_ENABLE=1
		CandidateLoginCredentialsDO candidateLoginCredentialsDO =
				candidateLoginCredentialsGenerationDO.getCandidateLoginCredentialsDOs().get(0);
		String tpLoginUrl = acsPropertyUtil.getTPUrlToLogin();
		candidateCredentialDetailsDO.setAcsLoginPath(tpLoginUrl);
		candidateCredentialDetailsDO.setLoginName(candidateLoginCredentialsDO.getLoginId());
		candidateCredentialDetailsDO.setLoginPwd(candidateLoginCredentialsDO.getPassword());
		
		AcsBatch batchDetails = batchService.getBatchDetailsByBatchId(batchCode);
		// AcsCandidate candidateTO =
		// candidateService.getCandidateDetailsFromCandId(candidateLoginCredentialsDO.getCandidateId());

		if (batchDetails == null) {
			logger.debug("batchDetails is not available with specified batchCode : {} ", batchCode);
			candidateCredentialDetailsDO.setErrorMessage("batchDetails is not available with specified code");
			candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_FAILURE);
			return candidateCredentialDetailsDO;
		}

		// if (candidateTO == null) {
		// logger.debug("candidate Details is not available with specified candidate Id = {} ",
		// candidateLoginCredentialsDO.getCandidateId());
		// candidateCredentialDetailsDO.setErrorMessage("candidate Details is not available with specified Id ");
		// candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_FAILURE);
		// return candidateCredentialDetailsDO;
		// }
		// set candidate application Numb
		// candidateCredentialDetailsDO.setApplicationId(candidateTO.getIdentifier1());

		// test start time and test end time will be null for new candidate.
		// Hence setting batch start time and batch end time
		Calendar testEndTime = batchDetails.getBatchEndTime();
		Calendar testStartTime = batchDetails.getMaxBatchStartTime();
		String testEndTimeInString =
					TimeUtil.convertTimeAsString(testEndTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);

		String testStartTimeInString =
					TimeUtil.convertTimeAsString(testStartTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);

		candidateCredentialDetailsDO.setTestStartTime(testStartTimeInString);
		candidateCredentialDetailsDO.setTestEndTime(testEndTimeInString);

		logger.debug("--OUT-- getNewlyGeneratedCandidateDetails() with candidateCredentialDetailsDO = {}",
				candidateCredentialDetailsDO);
		candidateCredentialDetailsDO.setErrorMessage("getCandidateCredentialDetails() is success");
		candidateCredentialDetailsDO.setStatus(ACSConstants.MSG_SUCCESS);
		return candidateCredentialDetailsDO;
	}


	/**
	 * @param loginIdAssessmentAssociationId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LoginIdAssessmentAssociationTO getLoginIdAssessmentAssociationById(int loginIdAssessmentAssociationId)
			throws GenericDataModelException {
		LoginIdAssessmentAssociationTO loginIdAssessmentAssociation =
				(LoginIdAssessmentAssociationTO) session.get(loginIdAssessmentAssociationId,
				LoginIdAssessmentAssociationTO.class.getCanonicalName());
		return loginIdAssessmentAssociation;
		}
}
