package com.merittrac.apollo.acs.services;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateBlockingStatus;
import com.merittrac.apollo.acs.constants.CandidateFeedBackStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateRemoteProctoringStatus;
import com.merittrac.apollo.acs.dataobject.AssessmentDetailsDO;
import com.merittrac.apollo.acs.dataobject.BatchCandidateDO;
import com.merittrac.apollo.acs.dataobject.CandidateDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateLoginDO;
import com.merittrac.apollo.acs.dataobject.CandidateSectionTimingDetailsDOWrapper;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.entities.acs.CandidateAnswersEntity;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;
import com.merittrac.apollo.reports.ProvisionalCertificateEntity;

/**
 * Api's related to BatchCandidateAssociation table.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class BatchCandidateAssociationService extends BasicService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static CDEAService cdeaService = null;
	static BatchService batchService = BatchService.getInstance();

	static {
		if (cdeaService == null) {
			cdeaService = new CDEAService();
		}
	}

	public boolean setBatchCandidateAssociation(AcsBatchCandidateAssociation batchCandidateAssociation)
			throws GenericDataModelException {
		session.persist(batchCandidateAssociation);
		return true;
	}

	public boolean updateBatchCandidateAssociation(AcsBatchCandidateAssociation batchCandidateAssociation)
			throws GenericDataModelException {
		session.merge(batchCandidateAssociation);
		return true;
	}

	public boolean deleteBatchCandidateAssociationsByBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		session.updateByQuery(ACSQueryConstants.QUERY_DELETE_BCA_FOR_BATCHCODE, params);
		return true;
	}

	public boolean isBatchCandidateAssociationExists(String batchCode, int candId, String assessmentCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchCode);
		params.put(ACSConstants.CAND_ID, candId);
		params.put(ACSConstants.ASSESSMENT_ID, assessmentCode);
		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_CANDID_ASSESSMENTCODE, params);
		if (batchCandidateAssociation == null) {
			return false;
		}
		return true;
	}

	public AcsBatchCandidateAssociation getBatchCandidateAssociationById(int batchCandidateAssociationId)
			throws GenericDataModelException {
		return (AcsBatchCandidateAssociation) session.get(batchCandidateAssociationId,
				AcsBatchCandidateAssociation.class.getCanonicalName());
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBatchIdsAndLoginId(List<String> batchCodes,
			String loginId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_IDS, batchCodes);
		params.put(ACSConstants.USER_NAME, loginId.toUpperCase());

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_AND_LOGINID, params);
		if (batchCandidateAssociations.equals(Collections.<Object> emptyList()))
			return null;
		return batchCandidateAssociations;
	}

	public boolean updateInvalidLoginAttempts(int id, int currentCountOfInvalidLoginAttempts,
			CandidateBlockingStatus blockingStatus) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("currentCountOfInvalidLoginAttempts", currentCountOfInvalidLoginAttempts);
		params.put("candidateBlockingStatus", blockingStatus);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_BCA_INVALID_LOGINATTEMPTS, params);
		return count > 0 ? true : false;
	}

	public boolean updateValidLoginAttempts(int id, int currentCountOfValidLoginAttempts,
			CandidateBlockingStatus blockingStatus, int extendedNumberOfValidLoginAttempts,
			int extendedNumberOfValidLoginAttemptsCount) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("currentCountOfValidLoginAttempts", currentCountOfValidLoginAttempts);
		params.put("candidateBlockingStatus", blockingStatus);
		params.put("extendedNumberOfValidLoginAttempts", extendedNumberOfValidLoginAttempts);
		params.put("extendedNumberOfValidLoginAttemptsCount", extendedNumberOfValidLoginAttempts);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_NUMBER_OF_VALIED_LOGIN_ATTEMPLS, params);
		return count > 0 ? true : false;
	}

	public boolean updateValidLoginAttempts(int id, int currentCountOfValidLoginAttempts,
			CandidateBlockingStatus blockingStatus) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("currentCountOfValidLoginAttempts", currentCountOfValidLoginAttempts);
		params.put("candidateBlockingStatus", blockingStatus);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_BCA_VALID_LOGINATTEMPTS, params);
		return count > 0 ? true : false;
	}

	public boolean updateCandidateBlockingStatus(int id, CandidateBlockingStatus blockingStatus)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", id);
		params.put("candidateBlockingStatus", blockingStatus);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CANDIDATE_BLOCKINGSTATUS, params);
		return count > 0 ? true : false;
	}

	/**
	 * Get {@link AcsBatchCandidateAssociation} for batch code and candidate identifier.
	 * 
	 * @param batchId
	 * @param identifier1
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsBatchCandidateAssociation
			getBatchCandAssociationByBatchIdAndCandId(String batchCode, Integer candidateId)
					throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateId", candidateId);

		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_FOR_BATCH_AND_CANDIDATE_ID, params);
		return batchCandidateAssociation;
	}

	/**
	 * Get {@link AcsBatchCandidateAssociation} for batch code and candidate identifier.
	 * 
	 * @param batchId
	 * @param identifier1
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsBatchCandidateAssociation
			getBatchCandAssociationByBatchIdAndCandLoginId(String batchCode, String loginId)
					throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("loginId", loginId);

		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_FOR_BATCH_AND_CANDIDATE_LOGIN_ID, params);
		return batchCandidateAssociation;
	}

	/**
	 * Get {@link AcsBatchCandidateAssociation} for batch code and candidate identifier.
	 * 
	 * @param batchId
	 * @param identifier1
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsBatchCandidateAssociation getBatchCandAssociationByBatchIdAndCandIdentifier(String batchCode,
			String identifier1) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("identifier1", identifier1);

		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_FOR_BATCH_AND_CANDIDATE_INDENTIFIER, params);
		return batchCandidateAssociation;
	}

	/**
	 * Resets the login attempts to Zero as the candidate has made successfull login.
	 * 
	 * @param batchCandidateAssociation
	 * @param maxNumberOfValidLoginAttempts
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean resetValidLoginAttempts(AcsBatchCandidateAssociation batchCandidateAssociation,
			Integer extendedNumberOfValidLoginAttempts) throws GenericDataModelException {
		// int extendedNumberOfValidLoginAttempts = 0;
		if (batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts() != 0) {

			extendedNumberOfValidLoginAttempts += batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts();
		} else {
			AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
					cdeaService.getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(
							batchCandidateAssociation.getBatchCode(), batchCandidateAssociation.getAssessmentCode());
			int maxNumberOfValidLoginAttempts = bussinessRulesAndLayoutRulesTO.getMaxNumberOfValidLoginAttempts();
			extendedNumberOfValidLoginAttempts += maxNumberOfValidLoginAttempts;
		}
		int currentCountOfValidLoginAttempts = batchCandidateAssociation.getNumberOfValidLoginAttempts();
		CandidateBlockingStatus blockingStatus = CandidateBlockingStatus.ALLOWED;

		logger.debug("resetting the candidates login status to = {} and valid login attempts = {}", blockingStatus,
				currentCountOfValidLoginAttempts);

		// updates the candidate blocking status and reset the count to 0
		return updateValidLoginAttempts(batchCandidateAssociation.getBcaid(), currentCountOfValidLoginAttempts,
				blockingStatus, extendedNumberOfValidLoginAttempts,
				batchCandidateAssociation.getExtendedNumberOfValidLoginAttemptsCount() + 1);
	}

	/*
	 * public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBatchIdAndIdentifiers(int batchId,
	 * List<String> identifiers) throws GenericDataModelException { String query =
	 * "FROM BatchCandidateAssociationTO WHERE batchId = (:batchId) and candidateId IN (SELECT candidateId FROM CandidateTO WHERE identifier1 IN (:identifiers))"
	 * ; HashMap<String, Object> params = new HashMap<String, Object>(); params.put(ACSConstants.BATCH_ID, batchId);
	 * params.put("identifiers", identifiers);
	 * 
	 * List<AcsBatchCandidateAssociation> batchCandidateAssociations = (List<AcsBatchCandidateAssociation>)
	 * session.getResultAsListByQuery(query, params, 0); if (batchCandidateAssociations.isEmpty()) {
	 * batchCandidateAssociations = null; } return batchCandidateAssociations; }
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBatchIdAndCandidateIds(String batchCode,
			List<Integer> candidateIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateIds", candidateIds);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_AND_CANDIDATEID, params, 0);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE, params, 0);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public AcsBatchCandidateAssociation getBatchCandAssociationByBatchIdCandIdAndAssessmentId(String batchCode,
			int candId, String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put(ACSConstants.CAND_ID, candId);
		params.put("assessmentCode", assessmentCode);

		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_CANDID_ASSESSMENTCODE, params);
		return batchCandidateAssociation;
	}

	public int updateCandLevelLateLoginTimeByBatchIdAndCandidateId(String batchCode, int candidateId,
			String assessmentId, Calendar extendedLateLoginTimePerCandidate) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateId", candidateId);
		params.put("assessmentCode", assessmentId);
		params.put("extendedLateLoginTimePerCandidate", extendedLateLoginTimePerCandidate);

		return session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CANDLATELOGIN_BY_BATCHCODE_ASSESSMENTCODE_CANDID,
				params);
	}

	public AcsBatchCandidateAssociation
			getBatchCandidateAssociationBybatchIdAndCandidateId(String batchCode, int candId)
					throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put(ACSConstants.CAND_ID, candId);
		AcsBatchCandidateAssociation batchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_CANDID, params);
		return batchCandidateAssociation;
	}

	/**
	 * @param batchId
	 * @param candidateId
	 * @param assessmentId
	 * @param extendedBatchEndTimePerCandidate
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int updateCandLevelBatchEndTimeByBatchIdAndCandidateId(String batchCode, int candidateId,
			String assessmentCode, Calendar extendedBatchEndTimePerCandidate) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateId", candidateId);
		params.put("assessmentCode", assessmentCode);
		params.put("extendedBatchEndTimePerCandidate", extendedBatchEndTimePerCandidate);

		return session.updateByQuery(
				ACSQueryConstants.QUERY_UPDATE_CANDBATCH_ENDTIME_FOR_BATCHCODE_ASSESSMENTCODE_CANDID, params);
	}

	public List<AcsBatchCandidateAssociation> getCurrentBatchCandidateAssociation(String loginName)
			throws GenericDataModelException {
		String propValue = null;
		int preTestBufferTime = ACSConstants.DEFAULT_PRE_TEST_START_ALLOWED_LOGIN_TIME;
		propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PRE_TEST_START_ALLOWED_LOGIN_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			preTestBufferTime = Integer.parseInt(propValue);
		}

		int postTestBufferTime = ACSConstants.DEFAULT_POST_TEST_BUFFER_TIME;
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.POST_TEST_BUFFER_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			postTestBufferTime = Integer.parseInt(propValue);
		}

		Calendar currentDateTime = Calendar.getInstance();

		Calendar preTestBufferTimeCalendar = (Calendar) currentDateTime.clone();
		preTestBufferTimeCalendar.add(Calendar.MINUTE, preTestBufferTime * -1);

		Calendar postTestBufferTimeCalendar = (Calendar) currentDateTime.clone();
		postTestBufferTimeCalendar.add(Calendar.MINUTE, postTestBufferTime);
		// currentDateTime.set(2013, Calendar.JANUARY, 4, 10, 58);

		Map<String, Object> params = new HashMap<>();
		params.put(ACSConstants.PRE_TEST_BUFFER_TIME, preTestBufferTimeCalendar);
		params.put(ACSConstants.POST_TEST_BUFFER_TIME_CONST, postTestBufferTimeCalendar);
		params.put(ACSConstants.DATE_TIME, currentDateTime);
		params.put(ACSConstants.CANDIDATE_LOGINNAME, loginName);

		List<AcsBatchCandidateAssociation> result =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_GET_CURRENTBATCH_BCA_FOR_CANDIDATELOGINID,
						params, 0);
		session.getSession().close();
		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else
			return result;
	}

	/**
	 * Get {@link AcsBatchCandidateAssociation} by its identifier 'bcaid'.
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsBatchCandidateAssociation loadBatchCandAssociation(Integer bcaId) throws GenericDataModelException {
		return (AcsBatchCandidateAssociation) session.get(bcaId, AcsBatchCandidateAssociation.class.getName());
	}

	public void saveOrUpdateBatchCandidateAssociation(AcsBatchCandidateAssociation batchCandidateAssociationTO)
			throws GenericDataModelException {
		session.saveOrUpdate(batchCandidateAssociationTO);
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationByBatchIdAndIdentifier(String batchCode)
			throws GenericDataModelException {
		Map<String, Object> params = new HashMap<>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AcsBatchCandidateAssociation> result =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE, params, 0);

		return result;
	}

	/**
	 * Get {@link CandidateAnswersEntity} for {@link AcsBatchCandidateAssociation} identifier.
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public CandidateAnswersEntity getEventBatchCustDetailsByBatchIdAndCandId(AcsBatchCandidateAssociation bca)
			throws GenericDataModelException {
		AcsEvent acsEvent = cdeaService.getEventDetailsByBatchCode(bca.getBatchCode());

		CandidateAnswersEntity candidateAnswersEntity =
				new CandidateAnswersEntity(acsEvent.getAcsdivisiondetails().getAcscustomerdetails().getCustomerCode(),
						acsEvent.getAcsdivisiondetails().getDivisionCode(), acsEvent.getEventCode(),
						bca.getAssessmentCode());

		return candidateAnswersEntity;
	}

	/**
	 * @param batchId
	 * @param assessmentCode
	 * @param setId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int updateQuestionPaper(String batchCode, String assessmentCode, String setId, String qpaperCode)
			throws GenericDataModelException {
		logger.debug("Setting question paper for batch:{} Assessment:{} and Set:{}");
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("qpaperCode", qpaperCode);
		params.put("assessmentCode", assessmentCode);
		params.put("setID", setId);
		return session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_QUESTIONPAPER, params);

	}

	/**
	 * Get {@link AcsBatchCandidateAssociation} for {@link AcsCandidateStatus} id (csid).
	 * 
	 * @param candStatusId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsBatchCandidateAssociation getBCAforCandidateStatus(Integer candStatusId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("csid", candStatusId);
		AcsBatchCandidateAssociation acsBatchCandidateAssociation =
				(AcsBatchCandidateAssociation) session.getByQuery(ACSQueryConstants.QUERY_FETCH_BCA_FOR_CANDSTATUS,
						params);
		return acsBatchCandidateAssociation;
	}

	/**
	 * Get {@link AcsCandidateStatus} for {@link AcsBatchCandidateAssociation} id.
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsCandidateStatus getCandStatusForBCAId(int bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("bcaid", bcaId);
		AcsCandidateStatus acsCandidateStatus =
				(AcsCandidateStatus) session
						.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDIDATESTATUS_FOR_BCAID, params);
		return acsCandidateStatus;
	}

	/**
	 * Get batch code by BCA id.
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getBatchCodeByBCAId(int bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("bcaid", bcaId);
		String batchCode = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_BATCHCODE_FOR_BCAID, params);
		return batchCode;
	}

	/**
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationForCandidatesWithoutScores()
			throws GenericDataModelException {
		// String query="SELECT a.* FROM tcm.acsbatchcandidateassociation a," +
		// "tcm.acscandidatestatus c,tcm.acsresultprocreport r where c.actualtestendtime is not null and" +
		// " a.candidateid=c.candidateid and (r.Candid!=c.candidateid)";
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_BCA_FOR_SCORES_NOT_CALCULATE, null, AcsBatchCandidateAssociation.class);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationByBatchIdAndCandidatesWithoutScores(
			String batchCode) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_BCA_FOR_BATCHID_AND_CAND_WITHOUT_SCORES, params,
						AcsBatchCandidateAssociation.class);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * @param candidateIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationByCandidatesWithoutScores(
			List<Integer> candidateIds) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("candidateIds", candidateIds);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_BCA_FOR_CANDS_WITHOUT_SCORES, params, AcsBatchCandidateAssociation.class);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<ProvisionalCertificateEntity> getProvisionalCertificateEntityByBatchId(String batchCode)
			throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		List<ProvisionalCertificateEntity> provisionalCertificateEntities =
				(List<ProvisionalCertificateEntity>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_PROVISIONAL_CERTIFICATE_ENTITY_ON_BATCHID, params,
						ProvisionalCertificateEntity.class);
		if (provisionalCertificateEntities.isEmpty()) {
			provisionalCertificateEntities = null;
		}
		return provisionalCertificateEntities;
	}

	/**
	 * @param batchId
	 * @param candidateIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<ProvisionalCertificateEntity> getProvisionalCertificateEntityByCandidateIds(String batchCode,
			List<Integer> candidateIds) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("candidateIds", candidateIds);
		params.put(ACSConstants.BATCH_CODE, batchCode);
		List<ProvisionalCertificateEntity> provisionalCertificateEntities =
				(List<ProvisionalCertificateEntity>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_PROVISIONAL_CERTIFICATE_ENTITY_ON_CANDIDS, params,
						ProvisionalCertificateEntity.class);
		if (provisionalCertificateEntities.isEmpty()) {
			provisionalCertificateEntities = null;
		}
		return provisionalCertificateEntities;
	}

	/**
	 * @param batchId
	 * @param candidateId
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updateAttendanceByBatchIdCandidateId(String batchCode, int candidateId)
			throws GenericDataModelException {
		String query =
				"UPDATE AcsBatchCandidateAssociation SET isAttendanceMarkedManually=(:isManaul) WHERE batchCode = (:batchCode) and candidateId = (:candidateId)";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateId", candidateId);
		params.put("isManaul", true);
		session.updateByQuery(query, params);
	}

	public List<String> getAssessmentsByEventCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<String> assessments =
				(List<String>) session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTS_BY_BATCHCODE,
						params, 0);
		if (assessments.isEmpty()) {
			assessments = null;
		}
		return assessments;
	}

	public List<String> getAssessmentsByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<String> assessments =
				(List<String>) session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTS_BY_BATCHCODE,
						params, 0);
		if (assessments.isEmpty()) {
			assessments = null;
		}
		return assessments;
	}

	public List<String> getAssessmentsByBatchCodeOnCandidateLoginTime(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<String> assessments = (List<String>) session
				.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTS_BY_BATCHCODE_LOGINTIME, params, 0);
		if (assessments.isEmpty()) {
			assessments = null;
		}
		return assessments;
	}
	public List<AssessmentDetailsDO> getAssessmentListByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AssessmentDetailsDO> assessments =
				(List<AssessmentDetailsDO>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_ASSESSMENTS_LIST_BY_BATCHCODE, params, AssessmentDetailsDO.class);

		if (assessments.isEmpty()) {
			assessments = null;
		}
		return assessments;
	}

	public List<String> getSetIdByBatchCodeAssessmentCode(String batchCode, String assessmentCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		List<String> sets =
				(List<String>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_SETS_BY_BATCHCODE_ASSESSMENTCODE, params, 0);
		if (sets.isEmpty()) {
			sets = null;
		}
		return sets;
	}

	public List<String> getQpaperCodesByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		// params.put("assessmentCode", assessmentCode);
		List<String> sets = (List<String>) session
				.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QPAPERCODES_BY_BATCHCODE_ASSESSMENTCODE_LOGINTIME,
						params, 0);
		if (sets.isEmpty()) {
			sets = null;
		}
		return sets;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBatchCodeAndAssessmentCode(String batchCode,
			String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);

		List<CandidateDetailsDO> batchCandidateAssociations =
				session.getResultListByNativeSQLQuery(
						ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE, params,
						CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssociationByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE, params, CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBatchCode(String batchCode, Calendar startDate,
			Calendar endDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);

		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE_WITH_TESTENDTIME, params,
				CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBCAID(List<Integer> bcaids)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaids", bcaids);
		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BCAID, params,
				CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBatchCodeAssessmentCode(String batchCode,
			String assessmentCode, Calendar startDate, Calendar endDate) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_WITH_TESTENDTIME, params,
				CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBatchCodeAssessmentCodeSetId(String batchCode,
			String assessmentCode, String setId, Calendar startDate, Calendar endDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("setId", setId);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_SETID_WITH_TESTENDTIME, params,
				CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationByBatchCodeAndAssessmentCode(String batchCode,
			String assessmentCode, Calendar startDate, Calendar endDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_WITH_TEST_START_AND_END_TIME,
						params, 0 /* offSet, count */);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<CandidateDetailsDO> getBatchCandAssoDetailsByBatchCodeAssessmentCodeSetId(String batchCode,
			String assessmentCode, String setId) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("setId", setId);
		List<CandidateDetailsDO> batchCandidateAssociations = session.getResultListByNativeSQLQuery(
				ACSQueryConstants.NATIVE_QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_SETID, params,
				CandidateDetailsDO.class);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationByBatchCodeAssessmentCodeSetId(String batchCode,
			String assessmentCode, String setId, Calendar startDate, Calendar endDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("setId", setId);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session
						.getResultAsListByQuery(
								ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_SETID_WITH_TEST_START_AND_END_TIME,
								params, 0 /* offSet, count */);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * Get BatchCandidateAssociationTO ; BatchDetailsTO batchDetails; AssessmentDetailsTO assessmentDetails;
	 * BussinessRulesAndLayoutRulesTO brlrRules; CandidateStatusTO candidateStatus; EventDetailsTO eventDetail;
	 * 
	 * @param batchId
	 * @param candidateId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v1.0
	 * @see
	 */
	public List<CandidateLoginDO> getDataForCandidateLogin(List<String> batchCode, String candidateLogin)
			throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("candidateLogin", candidateLogin);
		List<CandidateLoginDO> candidateLoginData =
				session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_STATUS_BRLR_ASSESSMENT_EVENT_BATCH_FOR_CANDIDATE_BATCH,
						params, CandidateLoginDO.class);

		return candidateLoginData;
	}

	/**
	 * @param batchIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchCandidateDO> getAttendanceGridBatchWise(String[] batchCodes) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchIds", Arrays.asList(batchCodes));

		List<BatchCandidateDO> batchCandidateDOs =
				(List<BatchCandidateDO>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_BATCH_CAND_DO_FOR_BATCHS, params, BatchCandidateDO.class);
		if (batchCandidateDOs.isEmpty()) {
			batchCandidateDOs = null;
		}
		return batchCandidateDOs;
	}

	public AcsBatchCandidateAssociation getBatchCandAssoDisabledWalkinCandidates(String batchCode,
			String assessmentCode, String setID) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();

		// consider setId if present else don't consider setId for Login Id generation
		StringBuffer query =
				new StringBuffer("FROM  " + AcsBatchCandidateAssociation.class.getName()
						+ " bca WHERE bca.batchCode=(:batchCode) and "
						+ " bca.assessmentCode = (:assessmentCode) and bca.candidateType = "
						+ " (:candidateType) and bca.isEnable = (:isEnable) ");

		if (setID != null && !setID.isEmpty() && !setID.equalsIgnoreCase("ALL")) {
			query.append("and bca.setID = (:setID)");
			params.put("setID", setID);
		}

		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("candidateType", CandidateType.WALKIN);
		params.put("isEnable", false);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(query.toString(), params, 1);
		if (batchCandidateAssociations == null || batchCandidateAssociations.isEmpty()) {
			return null;
		} else {
			return batchCandidateAssociations.get(0);
		}

	}

	/**
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBatchCodeForEndedCandidates(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_FOR_ENDED_CANDIDATES, params, 0);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * @param batchCode
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Integer
			getCountLoginIdsByBatchCodeAssessmentCodeSetId(String batchCode, String assessmentCode, String setId)
					throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		String hql_query = ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE;
		if (setId != null && !setId.isEmpty() && !setId.equalsIgnoreCase(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);
			hql_query = ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_SETID;
		}
		Integer result = session.getResultCountByQuery(hql_query, params);
		return result;

	}

	/**
	 * @param currentDateTime
	 * @param allotedDuration
	 * @param bcaId
	 *            TODO
	 * @return
	 * @throws GenericDataModelException
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateSectionTiming(Calendar actualSectionStartedTime, long allotedDuration, int bcaId)
			throws GenericDataModelException {
		// QUERY_UPDATE_SECTION_DURATION
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("id", bcaId);
		params.put("allotedDuration", allotedDuration);
		params.put("actualSectionStartedTime", actualSectionStartedTime);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_SECTION_DURATION, params);
		return count > 0 ? true : false;

	}

	public boolean
			updateFeedbackDetails(int bcaId, String feedBackFormData, CandidateFeedBackStatusEnum feedBackStatus)
					throws GenericDataModelException {
		logger.debug("Updating feedback details for the candidate with bcaId:{} as feedbackStatus:{} feedbackData:{}",
				bcaId, feedBackStatus, feedBackFormData);
		// QUERY_UPDATE_SECTION_DURATION
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);
		params.put("feedBackStatus", feedBackStatus);
		params.put("feedBackFormData", feedBackFormData);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_FEEDBACK_DETAILS, params);
		return count > 0 ? true : false;

	}

	/**
	 * @param testEndTime
	 * @param forceSubmit
	 * @param feedBackStatusEnum
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateTestEndDetails(int bcaId, Calendar actualTestEndTime, boolean isForceSubmitted,
			CandidateFeedBackStatusEnum feedBackStatus, long timeSpentInSecs) throws GenericDataModelException {
		logger.debug(
				"Updating TestEndDetails for the candidate with bcaId:{} as testEndTime:{} forceSubmit:{} feedBackStatusEnum:{} timeSpentInSecs:{}",
				bcaId, actualTestEndTime, isForceSubmitted, feedBackStatus, timeSpentInSecs);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);
		params.put("actualTestEndTime", actualTestEndTime);
		params.put("isForceSubmitted", isForceSubmitted);
		params.put("feedBackStatus", feedBackStatus);
		params.put("timeSpentInSecs", timeSpentInSecs);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_TEST_END_DETAILS, params);
		logger.debug("{} rows updated for bcaid:{} for ending the test", count, bcaId);
		return count > 0 ? true : false;

	}

	public boolean updateTestFeedBackStatus(int bcaId, boolean isForceSubmitted,
			CandidateFeedBackStatusEnum feedBackStatus, long timeSpentInSecs) throws GenericDataModelException {
		logger.debug(
				"Updating FeedBackStatus for the candidate with bcaId:{} as forceSubmit:{} feedBackStatusEnum:{}",
				bcaId, isForceSubmitted, feedBackStatus);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);
		params.put("isForceSubmitted", isForceSubmitted);
		params.put("feedBackStatus", feedBackStatus);
		params.put("timeSpentInSecs", timeSpentInSecs);
		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_FEEDBACK_STATUS, params);
		logger.debug("{} rows updated for bcaid:{} for ending the test", count, bcaId);
		return count > 0 ? true : false;

	}

	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationForCandidatesWithoutScoresForABatch(
			String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
						ACSQueryConstants.GET_BCA_FOR_SCORES_NOT_CALCULATE_FOR_BATCH, params,
						AcsBatchCandidateAssociation.class);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * Get {@link AcsCandidateStatus} for {@link AcsBatchCandidateAssociation} id.
	 * 
	 * @param bcaId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getNetworkSpeedOfCandidateByBCAId(int bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("bcaid", bcaId);
		String networkSpeed =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDIDATE_NETWORK_SPEED_BY_BCAID, params);
		return networkSpeed;
	}

	public boolean updateCandRemoteProctorStatusByBCAIds(List<Integer> bcaids,
			CandidateRemoteProctoringStatus candidateRemoteProctoringStatus)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaids", bcaids);
		params.put("status", candidateRemoteProctoringStatus);
		int rows = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_CANDLATE_REMOTE_PROCTOR_BY_BCAID,
				params);
		if (rows > 0) {
			logger.info("updated CandidateRemoteProctoringStatusBy BCAIDs= {} and status={} ", bcaids,
					candidateRemoteProctoringStatus);
			return true;
		}
		logger.info("update FAILED CandidateRemoteProctoringStatusBy BCAIDs= {} and status={} ", bcaids,
				candidateRemoteProctoringStatus);
		return false;
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationByBatchCodeAndAssessmentCode(String batchCode,
			String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_WITH_TESTENDTIME, params, 0);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public List<AcsBatchCandidateAssociation> getBatchCandAssociationByBatchCodeAssessmentCodeSetId(String batchCode,
			String assessmentCode, String setId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("assessmentCode", assessmentCode);
		params.put("setId", setId);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_BY_BATCHCODE_ASSESSMENTCODE_SETID_WITH_TESTENDTIME, params,
						0);

		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	public AcsBatchCandidateAssociation getCurrentUnusedBatchCandidateAssociation() throws GenericDataModelException {
		List<AcsBatch> batches = batchService.getBatchesbyTimeInstance();
		AcsBatchCandidateAssociation bca = null;
		if(batches != null) {
			for (AcsBatch batch : batches) {
				if (batch.isDryRun()) {
					Map<String, Object> paramMap = new HashMap<>();
					paramMap.put("batchCode", batch.getBatchCode());
					String query = "FROM AcsBatchCandidateAssociation bca WHERE bca.batchCode = :batchCode AND bca.loginTime is null";
					bca = (AcsBatchCandidateAssociation) session.getByQuery(query, paramMap);
					if(bca != null)
						break;
				}
			}
		}
		return bca;
	}

	public AcsBatchCandidateAssociation getCurrentUnusedBatchCandidateAssociationOnBatchAndAssessment(String batchCode,
			String assessmentCode,String setId) throws GenericDataModelException {
		AcsBatchCandidateAssociation bca = null;
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("batchCode", batchCode);
		paramMap.put("assessmentCode", assessmentCode);
		paramMap.put("currentDateTime", Calendar.getInstance());
		paramMap.put("isPresent", "N");
		//
		String query = null;
		if (setId != null && !setId.equalsIgnoreCase("ALL")) {
			paramMap.put("setId", setId);
			query = "FROM AcsBatchCandidateAssociation bca WHERE bca.batchCode = (:batchCode) and bca.assessmentCode=(:assessmentCode) "
					+ " and bca.setID=(:setId) "
					+ " and bca.loginTime is null and bca.expiryDateTime>=(:currentDateTime) and "
					+ " bca.isPresent=(:isPresent)";
		} else {
			query =
					"FROM AcsBatchCandidateAssociation bca WHERE bca.batchCode = (:batchCode) and bca.assessmentCode=(:assessmentCode) "
							+ " and bca.loginTime is null and bca.expiryDateTime>=(:currentDateTime) and "
							+ " bca.isPresent=(:isPresent)";
		}

		bca = (AcsBatchCandidateAssociation) session.getByQuery(query, paramMap);
		return bca;
	}

	public CandidateSectionTimingDetailsDOWrapper getCandidateSectionExtensionByBcaId(int bcaid)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BCA_ID, bcaid);

		CandidateSectionTimingDetailsDOWrapper candidateSectionTimingDetailsDOWrapper =
				(CandidateSectionTimingDetailsDOWrapper) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_CANDIDATE_SECTION_TIMING_EXTENSIONS, params,
						CandidateSectionTimingDetailsDOWrapper.class);
		if (candidateSectionTimingDetailsDOWrapper != null
				&& candidateSectionTimingDetailsDOWrapper.getExtendedSectionTimingJson() != null) {
			String json = candidateSectionTimingDetailsDOWrapper.getExtendedSectionTimingJson();
			Type type = new TypeToken<Map<String, Long>>() {
			}.getType();
			Map<String, Long> map = new Gson().fromJson(json, type);
			candidateSectionTimingDetailsDOWrapper.setCandidateSectionTimingDetailsDOMap(map);
		}
		return candidateSectionTimingDetailsDOWrapper;
	}
	


	public List<AcsBatchCandidateAssociation> getBatchCandAssociationsByBcaIds(List<Integer> bcaIds)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaIds", bcaIds);

		List<AcsBatchCandidateAssociation> batchCandidateAssociations = (List<AcsBatchCandidateAssociation>) session
				.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_BY_BCAID, params, 0);
		if (batchCandidateAssociations.isEmpty()) {
			batchCandidateAssociations = null;
		}
		return batchCandidateAssociations;
	}

	/**
	 * get Count of LoginIds By BatchCode
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean getCountOfLoggedInCandidatesByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		String query = ACSQueryConstants.QUERY_FETCH_CANDIDATE_LOGGEDIN_COUNT_BY_BATCHCODE;
		int result = session.getResultCountByQuery(query, params);
		if (result > 0)
			return false;
		else
			return true;

	}


}
