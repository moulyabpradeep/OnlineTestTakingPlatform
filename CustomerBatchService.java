package com.merittrac.apollo.acs.services;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSFilePaths;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.BatchCompletionStatusEnum;
import com.merittrac.apollo.acs.constants.BatchExtensionEnum;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.constants.CandidateBlockingStatus;
import com.merittrac.apollo.acs.constants.CandidateFeedBackStatusEnum;
import com.merittrac.apollo.acs.constants.CandidateTestStatusEnum;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.AssessmentDetailsDO;
import com.merittrac.apollo.acs.dataobject.AttendanceDashboardStats;
import com.merittrac.apollo.acs.dataobject.BatchAssessmentDO;
import com.merittrac.apollo.acs.dataobject.BatchCandidateDO;
import com.merittrac.apollo.acs.dataobject.BatchCodesDO;
import com.merittrac.apollo.acs.dataobject.BatchDetailsDO;
import com.merittrac.apollo.acs.dataobject.BatchExtensionDO;
import com.merittrac.apollo.acs.dataobject.BatchInformationDO;
import com.merittrac.apollo.acs.dataobject.BlockedCandidateListDO;
import com.merittrac.apollo.acs.dataobject.BlockedCandidatesInfoDo;
import com.merittrac.apollo.acs.dataobject.BpackInformationDO;
import com.merittrac.apollo.acs.dataobject.CDEDetailsDO;
import com.merittrac.apollo.acs.dataobject.CDEInfoDO;
import com.merittrac.apollo.acs.dataobject.CandidateActionDO;
import com.merittrac.apollo.acs.dataobject.CandidateAttendenceDO;
import com.merittrac.apollo.acs.dataobject.CandidateIdDO;
import com.merittrac.apollo.acs.dataobject.CandidateInfoDO;
import com.merittrac.apollo.acs.dataobject.CandidateLoginCredentialsDO;
import com.merittrac.apollo.acs.dataobject.CandidateLoginCredentialsGenerationDO;
import com.merittrac.apollo.acs.dataobject.CandidateReEnableDO;
import com.merittrac.apollo.acs.dataobject.CandidateReportDO;
import com.merittrac.apollo.acs.dataobject.ConnectedTerminalDo;
import com.merittrac.apollo.acs.dataobject.CustomerDO;
import com.merittrac.apollo.acs.dataobject.CustomerDetailsDO;
import com.merittrac.apollo.acs.dataobject.DivisionDetailsDO;
import com.merittrac.apollo.acs.dataobject.EventBatchDateTimesDO;
import com.merittrac.apollo.acs.dataobject.EventDetailsDO;
import com.merittrac.apollo.acs.dataobject.ExpiredLoginIdsDO;
import com.merittrac.apollo.acs.dataobject.GeneratedLoginIdsStatusDO;
import com.merittrac.apollo.acs.dataobject.LiveDashboardDetailsDO;
import com.merittrac.apollo.acs.dataobject.LiveDashboardStatsDO;
import com.merittrac.apollo.acs.dataobject.LoginIdGenerationDashboardDO;
import com.merittrac.apollo.acs.dataobject.LoginIdStatusStatisticsDO;
import com.merittrac.apollo.acs.dataobject.LoginIdsExportDO;
import com.merittrac.apollo.acs.dataobject.MIFDataDO;
import com.merittrac.apollo.acs.dataobject.ManualPacksUploadDetailsDO;
import com.merittrac.apollo.acs.dataobject.ManualPacksUploadDetailsStats;
import com.merittrac.apollo.acs.dataobject.PackStatistics;
import com.merittrac.apollo.acs.dataobject.PackStatusdetailsDO;
import com.merittrac.apollo.acs.dataobject.PacketInformationDO;
import com.merittrac.apollo.acs.dataobject.PaginationDataDO;
import com.merittrac.apollo.acs.dataobject.RpackInformationDO;
import com.merittrac.apollo.acs.dataobject.ScheduleDashboardStatsDO;
import com.merittrac.apollo.acs.dataobject.ScheduledBatchDataDO;
import com.merittrac.apollo.acs.dataobject.ScoreCalculationDO;
import com.merittrac.apollo.acs.dataobject.SectionReportDO;
import com.merittrac.apollo.acs.dataobject.UnUsedLoginCredentialsDO;
import com.merittrac.apollo.acs.dataobject.UnblockCandidateDO;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.dataobject.tp.MIFAuditDO;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.entities.ResultProcReportTO;
import com.merittrac.apollo.acs.entities.SectionReportTO;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.exception.UserRejectedException;
import com.merittrac.apollo.acs.quartz.jobs.RPackGenerator;
import com.merittrac.apollo.acs.quartz.jobs.ScoreCalculator;
import com.merittrac.apollo.acs.utility.ACSCommonUtility;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.ExcelWriterUtility;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.cdb.dataobject.ACSPackDetailsDo;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.cemexportentities.constants.EventType;
import com.merittrac.apollo.common.entities.acs.BatchTypeEnum;
import com.merittrac.apollo.common.entities.acs.HBMStatusEnum;
import com.merittrac.apollo.common.entities.acs.IPackInformationDO;
import com.merittrac.apollo.common.entities.acs.PackSubTypeEnum;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.acs.QpackInformationDO;
import com.merittrac.apollo.common.entities.acs.QuestionPaperStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.MIFFieldsValidationFlags;
import com.merittrac.apollo.tp.mif.ContactInfo;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;

/**
 * This class contains all the ACS dashboard related api's.
 * 
 * @author Siddhant_S Customer batch and candidate details
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
@SuppressWarnings("unchecked")
public class CustomerBatchService extends BasicService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static BatchService bs = null;
	private static CDEAService cdeas = null;
	private static PackDetailsService pds = null;
	private static FeedbackFormService feedbackFormService = null;
	private static ACSPropertyUtil acsPropertyUtil = null;
	private static AuthService authService = null;
	private static CandidateService candidateService = null;
	private static BatchCandidateAssociationService batchCandidateAssociationService = null;
	private static String isAuditEnable = null;
	private static AbstractPasswordFetchService abstractPasswordFetchService = null;
	private static Gson gson = null;
	private static QuestionService questionService = null;
	private static ScoreCalculator scoreCalculator = null;
	private static ACSCommonUtility acsCommonUtility = null;
	private static ResultProcStatusService resultProcStatusService = null;
	private static CryptUtil cryptUtil = null;

	public CustomerBatchService() {
		if (cryptUtil == null)
			cryptUtil = new CryptUtil();
		if (resultProcStatusService == null)
			resultProcStatusService = new ResultProcStatusService();
		if (acsCommonUtility == null)
			acsCommonUtility = new ACSCommonUtility();
		if (scoreCalculator == null)
			scoreCalculator = new ScoreCalculator();
		bs = BatchService.getInstance();
		authService = new AuthService();
		if (cdeas == null)
			cdeas = new CDEAService();
		if (pds == null)
			pds = new PackDetailsService();
		if (feedbackFormService == null)
			feedbackFormService = new FeedbackFormService();
		if (candidateService == null)
			candidateService = CandidateService.getInstance();
		if (batchCandidateAssociationService == null)
			batchCandidateAssociationService = new BatchCandidateAssociationService();
		acsPropertyUtil = new ACSPropertyUtil();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
		if (abstractPasswordFetchService == null) {
			abstractPasswordFetchService = new AbstractPasswordFetchService() {
			};
		}
		if (gson == null) {
			gson = new Gson();
		}
		if (questionService == null) {
			questionService = new QuestionService();
		}
	}

	/**
	 * Get details about all the customers specific to ACS.
	 * 
	 * @return list of {@link CustomerDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<CustomerDetailsDO> getCustomerDetails() throws GenericDataModelException {
		List<CustomerDetailsDO> customerDetails =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CUSTOMERS, null);
		logger.info("customer details = {}", customerDetails);
		if (customerDetails.equals(Collections.<Object> emptyList()))
			return null;
		return customerDetails;
	}

	/**
	 * Get details about all the customers specific to ACS.
	 * 
	 * @return list of {@link CustomerDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<CustomerDetailsDO> getCustomerDetailsForEvents(String[] eventCodes) throws GenericDataModelException {
		List<String> eventCodesList = Arrays.asList(eventCodes);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCodesList);
		// List<CDEDetailsDO> cdeDetailsDOs =
		// (List<CDEDetailsDO>) session.getResultAsListByQuery(query, params, 0, CDEDetailsDO.class);
		List<CustomerDetailsDO> customerDetails =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_CUSTOMERDETAILS_FOR_EVENT, params);
		logger.info("customer details = {}", customerDetails);
		if (customerDetails.equals(Collections.<Object> emptyList()))
			return null;
		return customerDetails;
	}

	/**
	 * Get details about all the customers specific to ACS on event codes.
	 * 
	 * @return list of {@link CustomerDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<CDEDetailsDO> getCustomerDetailsOnEventCodes(String[] eventCodes) throws GenericDataModelException {
		logger.info("get customer details On EventCodes= " + eventCodes);
		List<String> eventCodeList = Arrays.asList(eventCodes);
		List<CDEDetailsDO> cdeDetailsDOs = cdeas.getCustomerDivisionAndEventDetailsByEventCodes(eventCodeList);
		logger.info("customer details = {}", cdeDetailsDOs);
		if (cdeDetailsDOs.equals(Collections.<Object> emptyList()))
			return null;
		return cdeDetailsDOs;
	}

	/**
	 * Get details about all the divisions specific to a customer.
	 * 
	 * @param customerId
	 *            auto generated id for customer
	 * @return list of {@link DivisionDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<DivisionDetailsDO> getDivisionDetails(String customerId) throws GenericDataModelException {
		logger.info("specified input : customerId = {}", customerId);
		if (customerId == null)
			return null;
		Map<String, Object> params = new HashMap<>();
		params.put("customerCode", customerId);
		List<DivisionDetailsDO> divisionDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_DIVISIONS_BY_CUSTID, params, 0,
						DivisionDetailsDO.class);
		logger.info("division details = {} ", divisionDetails);
		if (divisionDetails.equals(Collections.<Object> emptyList()))
			return null;
		return divisionDetails;
	}

	/**
	 * get details about divisions based on customer and event..
	 * 
	 * @param customerId
	 * @param eventCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<DivisionDetailsDO> getDivisionDetailsForEvent(String customerId, String[] eventCodes)
			throws GenericDataModelException {
		logger.info("inside getDivisionDetailsForEvents() --> for customer   " + customerId);
		List<String> eventCodesList = Arrays.asList(eventCodes);

		if (customerId == null)
			return null;
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("customerCode", customerId);
		params.put("eventCode", eventCodesList);
		List<DivisionDetailsDO> divisionDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_DIVISIONS_BY_CUSTID_EVENTCODE, params, 0,
						DivisionDetailsDO.class);

		logger.info("division details = {} ", divisionDetails);
		if (divisionDetails.equals(Collections.<Object> emptyList()))
			return null;
		return divisionDetails;

	}

	/**
	 * Gets details about all events within a division and also within the specified date range for a customer.
	 * 
	 * @param customerCode
	 *            auto generated id for customer
	 * @param divisionCode
	 *            auto generated id for division
	 * @param startDate
	 *            start date in the specified date range
	 * @param endDate
	 *            end date in the specified date range
	 * @return list of {@link EventDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<EventDetailsDO> getEventDetails(String customerCode, String divisionCode, Calendar startDate,
			Calendar endDate) throws GenericDataModelException {
		logger.info("specified input : customerId = {} , divisionId = {}  , startDate = {} and endDate = {}",
				new Object[] { customerCode, divisionCode, startDate.getTime(), endDate.getTime() });
		if (customerCode == null || divisionCode == null || startDate == null || endDate == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("customerId", customerCode);
		params.put("divisionId", divisionCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);

		List<EventDetailsDO> eventDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_EVENTS_BY_CUSTID_DIVID_DATE, params, 0,
						EventDetailsDO.class);
		logger.info("event details = {} ", eventDetails);
		if (eventDetails.equals(Collections.<Object> emptyList()))
			return null;
		return eventDetails;
	}

	/**
	 * Gets details about all events within a division and for a customer.
	 * 
	 * @param customerCode
	 *            auto generated id for customer
	 * @param divisionCode
	 *            auto generated id for division
	 * @return list of {@link EventDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<EventDetailsDO> getEventDetails(String customerCode, String divisionCode)
			throws GenericDataModelException {
		logger.info("specified input : customerId = {} , divisionId = {} ", new Object[] { customerCode, divisionCode });
		if (customerCode == null || divisionCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("customerId", customerCode);
		params.put("divisionId", divisionCode);

		List<EventDetailsDO> eventDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_EVENTS_BY_CUSTID_DIVID, params, 0,
						EventDetailsDO.class);
		logger.info("event details = {} ", eventDetails);
		if (eventDetails.equals(Collections.<Object> emptyList()))
			return null;
		return eventDetails;
	}

	/**
	 * getting events based on customer, division and event details
	 * 
	 * @param customerCode
	 * @param divisionCode
	 * @param eventCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<EventDetailsDO>
			getEventDetailsForNotAdmin(String customerCode, String divisionCode, String[] eventCodes)
					throws GenericDataModelException {
		List<String> eventCodesList = Arrays.asList(eventCodes);
		logger.info("specified input : customerId = {} , divisionId = {} ", new Object[] { customerCode, divisionCode });
		if (customerCode == null || divisionCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("customerId", customerCode);
		params.put("divisionId", divisionCode);
		params.put("eventCode", eventCodesList);

		List<EventDetailsDO> eventDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_EVENTS_BY_CUSTID_DIVID_EVENTID, params, 0,
						EventDetailsDO.class);
		logger.info("event details = {} ", eventDetails);
		if (eventDetails.equals(Collections.<Object> emptyList()))
			return null;
		return eventDetails;
	}

	/**
	 * Gets batch related details specific to a customer, division and event within the specified date range (date range
	 * applicable only to events and batches).
	 * 
	 * @param customerId
	 *            auto generated id for customer
	 * @param divisionId
	 *            auto generated id for division
	 * @param eventId
	 *            auto generated id for event
	 * @param startDate
	 *            start date in the specified date range
	 * @param endDate
	 *            end date in the specified date range
	 * @return list of {@link BatchDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<BatchDetailsDO> getBatchDetails(String customerCode, String divisionCode, String eventCode,
			Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info(
				"specified input : customerId = {} , divisionId = {} ,eventId = {}, startDate = {} and endDate = {}",
				new Object[] { customerCode, divisionCode, eventCode, startDate.getTime(), endDate.getTime() });
		if (customerCode == null || divisionCode == null || eventCode == null || startDate == null || endDate == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("customerCode", customerCode);
		params.put("divisionCode", divisionCode);
		params.put("eventCode", eventCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);

		List<BatchDetailsDO> batchDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHES_BY_CUSTID_DIVID_EVEID, params, 0,
						BatchDetailsDO.class);
		logger.info("batch details = {}", batchDetails);
		if (batchDetails.equals(Collections.<Object> emptyList()))
			return null;
		return batchDetails;
	}

	/**
	 * Gets start and end times of event and batch for the specified event and batch identifiers.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param eventId
	 *            auto generated id for event
	 * @return list of {@link EventBatchDateTimesDO}
	 * @throws GenericDataModelException
	 */
	public List<EventBatchDateTimesDO> getEventBatchDateTimes(String batchCode, String eventCode)
			throws GenericDataModelException {
		logger.info("specified input : batchId = {} and eventId = {}", batchCode, eventCode);
		if (batchCode == null || eventCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("eventCode", eventCode);

		List<EventBatchDateTimesDO> eventBatchDateTimes =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHDETAILS_BY_EVEID_BATCHID, params, 0,
						EventBatchDateTimesDO.class);
		logger.info("eventBatchDateTimes = {}", eventBatchDateTimes);
		if (eventBatchDateTimes == null || eventBatchDateTimes.equals(Collections.<Object> emptyList())) {
			return null;
		} else {
			for (Iterator iterator = eventBatchDateTimes.iterator(); iterator.hasNext();) {
				EventBatchDateTimesDO eventBatchDateTimesDO = (EventBatchDateTimesDO) iterator.next();
				if (eventBatchDateTimesDO != null
						&& eventBatchDateTimesDO.getBatchEndDateTimeByACS() != null
						&& eventBatchDateTimesDO.getBatchEndDateTimeByACS().after(
								eventBatchDateTimesDO.getBatchEndDateTime())) {
					eventBatchDateTimesDO.setBatchEndDateTime(eventBatchDateTimesDO.getBatchEndDateTimeByACS());
				}
			}
		}
		return eventBatchDateTimes;
	}

	/**
	 * Gets assessment details specific to a customer, division, event and batch.
	 * 
	 * @param customerCode
	 *            auto generated id for customer
	 * @param divisionCode
	 *            auto generated id for division
	 * @param eventCode
	 *            auto generated id for event
	 * @param batchCode
	 *            auto generated id for batch
	 * @return list of {@link AssessmentDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<AssessmentDetailsDO> getAssessmentDetails(String eventCode) throws GenericDataModelException {
		logger.info("specified input : eventId = {}", new Object[] { eventCode });
		if (eventCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);

		List<AssessmentDetailsDO> assessmentDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTDETAILS_BY_EVENT_ID, params, 0,
						AssessmentDetailsDO.class);
		logger.info("assessmentDetails = {}", assessmentDetails);
		if (assessmentDetails.equals(Collections.<Object> emptyList()))
			return null;
		return assessmentDetails;
	}

	/**
	 * Gets all the candidateId's for specified batchId.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @return list of {@link CandidateIdDO}
	 * @throws GenericDataModelException
	 */
	public List<CandidateIdDO> getCandIdsbyBatchId(String batchCode) throws GenericDataModelException {
		logger.info("specified input : batchCode = {}", batchCode);
		if (batchCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_IDS_BY_BATCHID, params, 0,
						CandidateIdDO.class);
		// logger.info("candidateIds = {}", candidateIds);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	/**
	 * Gets all the candidateIds for specified assessmentId.
	 * 
	 * @param assessmentId
	 *            auto generated id for assessment
	 * @return list of {@link CandidateIdList}
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public List<CandidateIdDO> getCandIdsbyAssessmentId(Integer assessmentId) throws GenericDataModelException {
		logger.info("specified input : assessmentId = {}", assessmentId);
		if (assessmentId == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentId", assessmentId);

		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_IDS_BY_ASSESSMENTID, params, 0,
						CandidateIdDO.class);
		// logger.info("candidateIds = {}", candidateIds);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	/**
	 * Gets batch and assessment identifiers (auto generated) for specified candidateId.
	 * 
	 * @param candidateId
	 *            auto generated id for candidate
	 * @return list of {@link BatchAssessmentDO}
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public List<BatchAssessmentDO> getBatchAssessmentIdsbyCandId(Integer candidateId) throws GenericDataModelException {
		logger.info("specified input : candidateId = {}", candidateId);
		if (candidateId == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candidateId", candidateId);

		List<BatchAssessmentDO> batchAssessmentIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHID_ASSESSMENTID_BY_CAND_ID, params,
						0, BatchAssessmentDO.class);
		logger.info("batchAssessmentIds = {}", batchAssessmentIds);
		if (batchAssessmentIds.equals(Collections.<Object> emptyList()))
			return null;
		return batchAssessmentIds;
	}

	/**
	 * Gets candidateIds for specified batchId and assessmentId.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param assessmentId
	 *            auto generated id for assessment
	 * @return list of {@link CandidateIdDO}
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public List<CandidateIdDO> getCandIdsbyBatchIdAndAssessmentId(Integer batchId, Integer assessmentId)
			throws GenericDataModelException {
		logger.info("specified input : batchId = {} and assessmentId = {}", batchId, assessmentId);
		if (batchId == null || assessmentId == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentId", assessmentId);
		params.put("batchId", batchId);

		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_IDS_BY_BATCHID_ASSESSMENTID, params,
						0, CandidateIdDO.class);
		// logger.info("candidateIds = {}", candidateIds);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	/**
	 * Gets the candidate responses for the specified candidateId's associated to the specified batch.
	 * 
	 * @param candidateIds
	 *            list of auto generated candidateId's
	 * @param batchId
	 *            auto generated id for batch
	 * @param startIndex
	 *            specifies the start index from which the subset of result set should start with ( added for pagination
	 *            purpose)
	 * @param count
	 *            specifies the sub set count of result set to be fetched
	 * @return PaginationDataDO
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public PaginationDataDO getCanidateResponses(List<CandidateIdDO> candidateIds, int batchId, int startIndex,
			int count) throws GenericDataModelException {
		int maxScore = 0;
		String qpIdent = null;
		List<Integer> candIds = new ArrayList<Integer>();
		for (Iterator<CandidateIdDO> iterator = candidateIds.iterator(); iterator.hasNext();) {
			candIds.add(iterator.next().getCandidateId());
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candidateIds", candIds);
		params.put("batchId", batchId);

		List<Object> candidateResponses =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_REPORT_DASHBOARD_DATA,
						params, startIndex, count, CandidateReportDO.class);
		int totalResultsCount =
				session.getResultCountByQuery(
						ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_REPORT_DASHBOARD_DATA_COUNT, params);
		logger.info("candidateResponses = {}", candidateResponses);
		if (candidateResponses.equals(Collections.<Object> emptyList()))
			return null;

		for (Object candidateResponseObject : candidateResponses) {
			CandidateReportDO candidateResponse = (CandidateReportDO) candidateResponseObject;
			Integer candidateId = candidateResponse.getCandidateId();

			params.clear();
			params.put("candidateId", candidateId);
			params.put("batchId", batchId);
			Integer qpId = (Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_QPID_FOR_BATCHCANDIDATE, params);
			if (qpId != null) {
				params.clear();
				params.put("qpId", qpId);
				qpIdent = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_QPAPERID_FOR_QPID, params);
				if (qpIdent != null) {
					params.clear();
					params.put("qpIdent", qpIdent);
					maxScore = questionService.getQuestionCountForQP(qpIdent);

					params.clear();
					HashMap<String, Object> param = new HashMap<String, Object>();
					param.put("candidateId", candidateId);
					param.put("qpIdent", qpIdent);

					List<SectionReportDO> attemptedSectionReports =
							session.getResultAsListByQuery(
									ACSQueryConstants.QUERY_FETCH_SECTION_SCORE_DETAILS_FOR_REPORT_DASHBOARD_DATA,
									param, 0, SectionReportDO.class);
					List<String> attemptedSectionNames = new ArrayList<String>();
					for (Iterator iterator = attemptedSectionReports.iterator(); iterator.hasNext();) {
						SectionReportDO sectionReportDO = (SectionReportDO) iterator.next();
						attemptedSectionNames.add(sectionReportDO.getSection());
					}

					// List<String> sectionNames = qs.getSectionNamesByQPIdentifier(qpIdent);
					// logger.info("section names = {} for question paper with id = {}", sectionNames,
					// qpIdent);
					//
					// List<SectionReportDO> sectionReport = new ArrayList<SectionReportDO>();
					// for (Iterator iterator = sectionNames.iterator(); iterator.hasNext();)
					// {
					// String sectionName = (String) iterator.next();
					// if (!attemptedSectionNames.contains(sectionName))
					// {
					// SectionReportDO sectionReportDO = new SectionReportDO();
					// sectionReportDO.setScore(0);
					// sectionReportDO.setSection(sectionName);
					// attemptedSectionReports.add(sectionReportDO);
					// }
					// }

					candidateResponse.setSectionReport(new HashSet<SectionReportDO>(attemptedSectionReports));
					// candidateResponse.setMaxScore(maxScore);
				}
			}
		}
		PaginationDataDO paginationData = new PaginationDataDO(totalResultsCount, candidateResponses);
		return paginationData;
	}

	/**
	 * generates live dash board related data for the current batch if exists.
	 * 
	 * @return list of {@link LiveDashboardDetailsDO}
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public LiveDashboardStatsDO getLiveDashboardData(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD,
						params, LiveDashboardDetailsDO.class);

		// checks whether live candidates info is available or not
		if (liveDashboardDetails.isEmpty())
			return null;

		// check whether ACS need to add candidate lost time automatically or not
		boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
		logger.info("isLostTimeAddedAutomatically falg = {} for batch with id = {}", isLostTimeAddedAutomatically,
				batchCode);

		for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
			// get candidate id
			int candidateId = liveDashboardDetailsDO.getCandidateId();

			// calculate remaining time for the candidate
			long remainingTime;
			if (liveDashboardDetailsDO.getActualSectionStartedTime() == null) {
				remainingTime = 0;
				if (liveDashboardDetailsDO.getTestEndTime() == null
						&& liveDashboardDetailsDO.getTestStartTime() != null
						&& liveDashboardDetailsDO.getLastHeartBeatTime() != null) {
					Calendar currentTime = Calendar.getInstance();
					long lastHeartBeatTimeInMillis = liveDashboardDetailsDO.getLastHeartBeatTime().getTimeInMillis();
					long currentTimeInMillis = currentTime.getTimeInMillis();

					int hbmTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
					logger.info("configured heart beat time interval = {}", hbmTimeInterval);

					long inActiveTime = (currentTimeInMillis - lastHeartBeatTimeInMillis);
					logger.info("inActive time for heart beat = {}", inActiveTime);

					if (inActiveTime > hbmTimeInterval) {
						currentTime.setTime(liveDashboardDetailsDO.getLastHeartBeatTime().getTime());
						logger.info(
								"heart beat is elapsed according to the configured value hence, considering the candidate with identifier = {} as crashed and considering lastHeartBeatTime = {} as current time",
								liveDashboardDetailsDO.getIdentificationNumber(), liveDashboardDetailsDO
										.getLastHeartBeatTime().getTime());
					} else {
						logger.info(
								"heart beat is according to the configured value hence, not considering the candidate with identifier = {} as crashed and considering current time = {} as it is",
								liveDashboardDetailsDO.getIdentificationNumber(), currentTime.getTime());
					}

					// calculate remaining time and check whether it is non negative if not trim it based on the batch
					// end
					// time else send 0
					remainingTime =
							(liveDashboardDetailsDO.getAllotedDuration().longValue() - ((currentTime.getTimeInMillis()
									- liveDashboardDetailsDO.getTestStartTime().getTimeInMillis()) / 1000));
					logger.info("spent time = {} for candidate with id = {} in batch with id = {}", remainingTime,
							candidateId, batchCode);
					if (isLostTimeAddedAutomatically) {
						remainingTime = remainingTime + liveDashboardDetailsDO.getPrevLostTime().longValue();
						logger.info(
								"automatically adding the lost time = {} for candidate with id = {} in batch with id = {}",
								liveDashboardDetailsDO.getPrevLostTime(), candidateId, batchCode);
					} else {
						logger.info("automatic addition of lost time is disabled for batch with id = {} ", batchCode);
					}
					// add the previous successful extended time's for the candidate if any
					logger.info(
							"adding previous successful extension time for candidate with id = {} in batch with id = {}",
							liveDashboardDetailsDO.getTotalExtendedExamDuration(), candidateId, batchCode);
					remainingTime = remainingTime + (liveDashboardDetailsDO.getTotalExtendedExamDuration().longValue());

					if (remainingTime > 0) {
						logger.info(
								"remaining time is greater than 0 hence, initiating trimming the duration based on batch end time");
						Calendar maxDeltaRpackGenTime = liveDashboardDetailsDO.getBatchEndTime();
						if (liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS() != null && liveDashboardDetailsDO
								.getDeltaRpackGenarationTimeByACS().after(maxDeltaRpackGenTime)) {
							maxDeltaRpackGenTime = liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS();
						}

						// trim the remaining time based on the batch end time
						remainingTime = trimDurationOnBatchEndTime(currentTime, remainingTime, maxDeltaRpackGenTime);
					} else {
						remainingTime = 0;
					}
					logger.info("remaining time = {} for candidate with id = {}", remainingTime, candidateId);
				}
				liveDashboardDetailsDO.setRemainingTime(remainingTime);
			} else {
				liveDashboardDetailsDO.setRemainingTime(ACSConstants.NEGATIVE_VALUE);
			}
		}
		LiveDashboardStatsDO liveDashboardStatsDO = new LiveDashboardStatsDO(liveDashboardDetails);
		return liveDashboardStatsDO;
	}

	/**
	 * generates live dash board related data for the current batch if exists.
	 * 
	 * @return list of {@link LiveDashboardDetailsDO}
	 * @throws GenericDataModelException
	 */
	public LiveDashboardStatsDO getLiveDashboardData(String batchCode, int pageNumber, int pageCount,
			String searchData, String sortingColumn, String orderType, String columnName, String columnValue)
			throws GenericDataModelException {// , String filterOn, String filterType
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		ArrayList<String> parentChildQuestions = new ArrayList<>();
		parentChildQuestions.add(QuestionType.READING_COMPREHENSION.toString());
		parentChildQuestions.add(QuestionType.SURVEY.toString());
		params.put("questionType", parentChildQuestions);

		Date d = new Date();
		d.setHours(23);
		d.setMinutes(59);
		d.setSeconds(59);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String endDate = simpleDateFormat.format(d.getTime());
		d.setHours(0);
		d.setMinutes(0);
		d.setSeconds(0);
		String startDate = simpleDateFormat.format(d.getTime());
		// params.put("startDate", startDate);
		// params.put("endDate", endDate);
		String query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD_PAGGINATION;
		String queryCount = ACSQueryConstants.QUERY_FETCH_CAND_COUNT_FOR_LIVE_DASHBOARD_PAGGINATION;

		Long count1 = null;
		if (searchData != null && !searchData.isEmpty()) {
			String sysQuery = "";

			int eLength = HBMStatusEnum.values().length;
			for (int x = 0; x < eLength; x++) {
				if (HBMStatusEnum.values()[x].toString().toLowerCase().contains(searchData.toString().toLowerCase())) {
					sysQuery =
							sysQuery + " or cs.playerstatus =" + x + " or cs.peripheralstatus =" + x
									+ " or cs.systemstatus=" + x;
				}
			}

			int eLengthCS = CandidateTestStatusEnum.values().length;
			for (int y = 0; y < eLengthCS; y++) {
				if (CandidateTestStatusEnum.values()[y].toString().toLowerCase()
						.contains(searchData.toString().toLowerCase())) {
					sysQuery = sysQuery + " or cs.currentstatus =" + y;
				}
			}

			query =
					query + " and (c.firstname like '%" + searchData + "%' or bca.hostaddress like '%" + searchData
							+ "%' or c.identifier1 like '%" + searchData + "%' or a.assessmentcode like '%"
							+ searchData + "%' or bca.logintime like '%" + searchData
							+ "%' or bca.actualteststartedtime like '%" + searchData
							+ "%' or bca.actualtestendtime like '%" + searchData + "%' or bca.candidatelogin like '%"
							+ searchData + "%'" + sysQuery + ")";
			queryCount =
					queryCount + " and (c.firstname like '%" + searchData + "%' or bca.hostaddress like '%"
							+ searchData + "%' or c.identifier1 like '%" + searchData
							+ "%' or a.assessmentcode like '%" + searchData + "%' or bca.logintime like '%"
							+ searchData + "%' or bca.actualteststartedtime like '%" + searchData
							+ "%' or bca.actualtestendtime like '%" + searchData + "%' or bca.candidatelogin like '%"
							+ searchData + "%'" + sysQuery + ")";

		}

		if (columnName != null && columnValue != null) {
			String newValue = "'" + columnValue + "'";
			if (columnName.equalsIgnoreCase("currentStatus")) {
				newValue = String.valueOf(CandidateTestStatusEnum.valueOf(columnValue).ordinal());
			} else if (columnName.equalsIgnoreCase("playerstatus") || columnName.equalsIgnoreCase("systemStatus")
					|| columnName.equalsIgnoreCase("peripheralStatus")) {
				newValue = String.valueOf(HBMStatusEnum.valueOf(columnValue).ordinal());
			} else if (columnName.equalsIgnoreCase("testStatus")) {
				columnName = "cs.currentstatus";
				newValue = String.valueOf(CandidateTestStatusEnum.valueOf(columnValue).ordinal());
			} else if (columnName.equalsIgnoreCase("testStartTime")) {
				columnName = "actualtestStartedtime";
			} else if (columnName.equalsIgnoreCase("testEndTime")) {
				columnName = "actualtestEndtime";
			}
			query = query + " and " + columnName + " = " + newValue;

			queryCount = queryCount + " and " + columnName + " = " + newValue;
		}
		int count = 0;

		logger.debug("Query to be executed for count:{}", queryCount);
		List liveDashboardDetails1 = session.getResultListByNativeSQLQuery(queryCount, params);
		int countN = Integer.parseInt(liveDashboardDetails1.get(0).toString());

		if (sortingColumn != null && orderType != null) {
			query = query + " group by cs.csid order by " + sortingColumn + " " + orderType;
		}

		// int startCount = pageNumber * pageCount;

		params.put("startCount", pageNumber);
		params.put("endCount", pageCount);
		query = query + " limit :startCount, :endCount";

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(query, params, LiveDashboardDetailsDO.class);

		// checks whether live candidates info is available or not
		if (liveDashboardDetails.isEmpty())
			return null;

		// check whether ACS need to add candidate lost time automatically or
		// not
		boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
		logger.info("isLostTimeAddedAutomatically falg = {} for batch with id = {}", isLostTimeAddedAutomatically,
				batchCode);

		int rpackGenerationFrequency = acsPropertyUtil.getRpackGenerationFrequency();
		int summaryPageTimeout = acsPropertyUtil.getSummaryPageDefaultTimeout();
		int hbmTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
		for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {

			// check whether the automatic rpack generation time is elapsed or
			// not
			Calendar testEndTime = liveDashboardDetailsDO.getTestEndTime();
			if (!liveDashboardDetailsDO.isRpackGenarated() && testEndTime != null) {
				Calendar rpackGenTime = (Calendar) testEndTime.clone();
				rpackGenTime
						.add(Calendar.MINUTE, (summaryPageTimeout + rpackGenerationFrequency + (liveDashboardDetailsDO
								.getFeedbackDuration() != null ? (liveDashboardDetailsDO.getFeedbackDuration()
								.intValue() / 60) : 0)));

				if (Calendar.getInstance().after(rpackGenTime)) {
					liveDashboardDetailsDO.setEnableRpackGeneration(true);
				}
			}


			Calendar maxDeltaRpackGenarationTime = liveDashboardDetailsDO.getBatchEndTime();
			if (liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS() != null) {
				if (liveDashboardDetailsDO.getBatchEndTime().before(liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS())) {
					maxDeltaRpackGenarationTime = liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS();
				}
			}
			
			// calculate remaining time for the candidate
			long remainingTime;
			if (liveDashboardDetailsDO.getActualSectionStartedTime() == null) {
				remainingTime = 0;

				Integer prevLostTime = liveDashboardDetailsDO.getPrevLostTime();
				Calendar testStartTime = liveDashboardDetailsDO.getTestStartTime();
				Calendar lastHeartBeatTime = liveDashboardDetailsDO.getLastHeartBeatTime();
					Integer allotedDuration = liveDashboardDetailsDO.getAllotedDuration();
				if (liveDashboardDetailsDO.getReenablecandidatejson() != null) {
					// Re birth flow
					CandidateReEnableDO candidateReEnableDO =
							gson.fromJson(liveDashboardDetailsDO.getReenablecandidatejson(), CandidateReEnableDO.class);
					testStartTime = candidateReEnableDO.getActualTestStartedTime();
					allotedDuration = (int) candidateReEnableDO.getAllotedDuration();
					prevLostTime = (int) candidateReEnableDO.getPrevLostTime();
					if (testStartTime == null)
						remainingTime = allotedDuration;
				}
					if (testEndTime == null && testStartTime != null && lastHeartBeatTime != null) {
					Calendar lastCrashedTime = liveDashboardDetailsDO.getLastCrashedTime();
					Calendar prevHeartBeatTime = liveDashboardDetailsDO.getPrevHeartBeatTime();
					Integer totalExtendedExamDuration = liveDashboardDetailsDO.getTotalExtendedExamDuration();
						remainingTime = candidateService.getRemainingTimeForCandidateInLiveDashboard(testStartTime,
								lastCrashedTime, prevLostTime, lastHeartBeatTime, prevHeartBeatTime,
								maxDeltaRpackGenarationTime, allotedDuration, totalExtendedExamDuration,
								hbmTimeInterval);
					}

				liveDashboardDetailsDO.setRemainingTime(remainingTime);
			} else {
				liveDashboardDetailsDO.setRemainingTime(ACSConstants.NEGATIVE_VALUE);
			}
		}

		LiveDashboardStatsDO liveDashboardStatsDO = new LiveDashboardStatsDO(liveDashboardDetails, countN);

		return liveDashboardStatsDO;
	}

	/**
	 * This method is used to check whether ACS need to add lost time automatically or not.
	 * 
	 * @return {@link Boolean} : boolean value which states whether automatic addition of lost time for candidate is
	 *         enabled or not
	 * 
	 * @since Apollo v2.0
	 */
	public boolean isLostTimeAddedAutomatically() {
		// default property for automatic addition of lost time for candidate
		boolean isCandLostTimeAddByACS = ACSConstants.DEFAULT_ADD_CAND_LOST_TIME_BY_ACS;

		// gets the configured property for automatic addition of lost time for candidate
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.IS_CAND_LOST_TIME_ADD_BY_ACS);
		if (propValue != null) {
			if (propValue.equalsIgnoreCase(Boolean.FALSE.toString())) {
				isCandLostTimeAddByACS = false;
			}
			logger.info("configured property for automatic addition of lost time for candidate = {}",
					isCandLostTimeAddByACS);
		} else {
			logger.info("default property for automatic addition of lost time for candidate = {}",
					isCandLostTimeAddByACS);
		}
		return isCandLostTimeAddByACS;
	}

	/**
	 * trims the duration based on the batch end time
	 * 
	 * @param currentDateTime
	 * @param allotedDuration
	 * @param batchEndTime
	 * @return {@link Long} : candidate remaining time in seconds
	 * 
	 * @since Apollo v2.0
	 */
	public long trimDurationOnBatchEndTime(Calendar currentDateTime, long allotedDuration, Calendar batchEndTime) {
		logger.info(
				"initiated trimDurationOnBatchEndTime where input params : currentDateTime = {} ,allotedDuration = {},batchEndTime= {}",
				currentDateTime, allotedDuration, batchEndTime);
		long proposedCandidateTestEndTime = currentDateTime.getTimeInMillis() + (allotedDuration * 1000);
		logger.info("proposedCandidateTestEndTime = {}", proposedCandidateTestEndTime);

		// check whether proposedCandidateTestEndTime exceeds batch end time or not if so trim it based accordingly
		if (proposedCandidateTestEndTime > batchEndTime.getTimeInMillis()) {
			proposedCandidateTestEndTime =
					((currentDateTime.getTimeInMillis() + (allotedDuration * 1000)) - batchEndTime.getTimeInMillis()) / 1000;
			allotedDuration = allotedDuration - proposedCandidateTestEndTime;
			logger.info("remaining time after trim = {}", allotedDuration);
		}
		return allotedDuration;
	}

	/**
	 * Generates attendance report for the specified batch.
	 * 
	 * @param batchCode
	 *            auto generated id for batch
	 * @param startIndex
	 *            specifies the start index from which the subset of result set should start with ( added for pagination
	 *            purpose)
	 * @param count
	 *            specifies the sub set count of result set to be fetched
	 * @return {@link PaginationDataDO}
	 * @throws GenericDataModelException
	 * @see CandidateAttendenceDO
	 */
	public PaginationDataDO getAttendenceReport(String batchCode, int startIndex, int count)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchCode);

		List<CandidateAttendenceDO> candidateAttendenceDOs =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ATTENDENCE_DASHBOARD_DATA, params,
						startIndex, count, CandidateAttendenceDO.class);

		// return if there are no candidates
		if (candidateAttendenceDOs.isEmpty()) {
			return null;
		}

		List<Object> attendance = new ArrayList<Object>();
		for (Iterator<CandidateAttendenceDO> iterator = candidateAttendenceDOs.iterator(); iterator.hasNext();) {
			CandidateAttendenceDO candidateAttendenceDO = (CandidateAttendenceDO) iterator.next();
			if (candidateAttendenceDO.getBatchType().equalsIgnoreCase(BatchTypeEnum.OPEN.toString())
					&& !candidateAttendenceDO.isCandidateEnabled()) {
				continue;
			}
			// Calculate login attempts count, fetch batch candidate association to fetch candidate max and current
			// login attempts
			String loginAttempts =
					getLoginAttemptsInfo(candidateAttendenceDO.getNumberOfValidLoginAttempts(),
							candidateAttendenceDO.getExtendedNumberOfValidLoginAttempts(),
							candidateAttendenceDO.getMaxNumberOfValidLoginAttempts());
			logger.info("login attempts = {} for candidate with id = {} in batch with id = {}", loginAttempts,
					candidateAttendenceDO.getCandidateId(), batchCode);
			Calendar maxBatchStartTime = authService.getMaxBatchStartTime(candidateAttendenceDO.getBatchStartTime(),
					candidateAttendenceDO.getExtendedBatchStartTime());
			Calendar maxCandidateBatchStartTime = authService.getMaxCandidateBatchStartTime(
					candidateAttendenceDO.getExtendedCandidateBatchStartTime(),
					maxBatchStartTime);
			Calendar lateLoginTime =
					getMaxLateLoginTime(candidateAttendenceDO.getExtendedLateLoginTimePerCandidate(),
							candidateAttendenceDO.getLateLoginTimeInMins(), maxCandidateBatchStartTime,
							candidateAttendenceDO.getExtendedLateLoginTime());
			logger.info("late login time = {} for candidate with id = {} in batch with id = {}", lateLoginTime,
					candidateAttendenceDO.getCandidateId(), batchCode);

			Calendar maxDeltaRpackGenTime = candidateAttendenceDO.getDeltaRpackGenerationTime();
			if (candidateAttendenceDO.getDeltaRpackGenerationTimeByACS() != null
					&& candidateAttendenceDO.getDeltaRpackGenerationTimeByACS().after(maxDeltaRpackGenTime)) {
				maxDeltaRpackGenTime = candidateAttendenceDO.getDeltaRpackGenerationTimeByACS();
			}

			// check whether the batch is active or not if active set true
			if ((candidateAttendenceDO.getLogoutTime() != null && !(candidateAttendenceDO.getLogoutTime()
					.equalsIgnoreCase("null"))) || Calendar.getInstance().after(maxDeltaRpackGenTime)) {
				candidateAttendenceDO.setLateLoginExtensionAllowed(false);
			}

			candidateAttendenceDO.setLoginAttemts(loginAttempts);
			candidateAttendenceDO.setLateLoginTime(TimeUtil.convertTimeAsString(lateLoginTime.getTimeInMillis(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS));
			try {
				candidateAttendenceDO.setPassword(cryptUtil.decryptTextUsingAES(candidateAttendenceDO.getPassword(),
						candidateAttendenceDO.getEventCode()));
			} catch (ApolloSecurityException e) {
				logger.error("ApolloSecurityException while executing getAttendenceReport...", e);
			}
			attendance.add(candidateAttendenceDO);
		}

		PaginationDataDO candidateAttendence = new PaginationDataDO(0, attendance);
		return candidateAttendence;
	}

	/**
	 * generates the section wise report for all the candidates who attempted the test for the specified batchId and
	 * assessmentId
	 * 
	 * @param batchCode
	 *            auto generated id for batch
	 * @param assessmentCode
	 *            auto generated id for assessment
	 * @param startIndex
	 *            specifies the start index from which the subset of result set should start with ( added for pagination
	 *            purpose)
	 * @param count
	 *            specifies the sub set count of result set to be fetched
	 * @return {@link PaginationDataDO}
	 * @throws GenericDataModelException
	 * @see CandidateReportDO
	 */

	public PaginationDataDO getSectionReport(String batchCode, String assessmentCode, int startIndex, int count)
			throws GenericDataModelException {
		logger.info("specified input : batchCode = {},assessmentCode = {}, startIndex = {} and count = {}",
				new Object[] { batchCode, assessmentCode, startIndex, count });
		if (batchCode == null || assessmentCode == null) {
			return null;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);

		int totalResultsCount =
				session.getResultCountByQuery("FROM " + ResultProcReportTO.class.getName()
						+ " WHERE batchCode=(:batchCode) and assessmentCode=(:assessmentCode)", params);
		List<ResultProcReportTO> resultProcReports =
				session.getResultAsListByQuery("FROM " + ResultProcReportTO.class.getName()
						+ "  WHERE batchCode=(:batchCode) and assessmentCode=(:assessmentCode)", params, startIndex,
						count);
		if (resultProcReports.equals(Collections.<Object> emptyList()))
			return null;

		List<Object> candidateReports = new ArrayList<Object>();
		for (Iterator resultProcReportIterator = resultProcReports.iterator(); resultProcReportIterator.hasNext();) {
			ResultProcReportTO resultProcReport = (ResultProcReportTO) resultProcReportIterator.next();
			Set<SectionReportDO> secReports = new HashSet<SectionReportDO>();
			for (Iterator secReportIterator = resultProcReport.getSectionReports().iterator(); secReportIterator
					.hasNext();) {
				SectionReportTO sectionReport = (SectionReportTO) secReportIterator.next();

				SectionReportDO secReport = new SectionReportDO();
				secReport.setSection(sectionReport.getSection());
				secReport.setScore(sectionReport.getMarksObtained());
				secReports.add(secReport);
			}

			CandidateReportDO candidateReport = new CandidateReportDO();
			candidateReport.setApplicationNumber(resultProcReport.getCandIdentifier());
			candidateReport.setCandidateId(resultProcReport.getCandID());
			candidateReport.setFirstName(resultProcReport.getCandName());
			candidateReport.setMaxScore(resultProcReport.getTotalMarks());
			candidateReport.setSectionReport(secReports);

			candidateReports.add(candidateReport);
		}

		PaginationDataDO paginationData = new PaginationDataDO(totalResultsCount, candidateReports);
		return paginationData;
	}

	/**
	 * Returns all the candidateIds who ended their exam completely for specified batch if forceStart flag is false.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param forceStart
	 *            if true returns all the candidateIds whether an Rpack is already generated for a candidate or not (set
	 *            it to true only if u want to generate Rpack forcibly for all the candidates)
	 * @return list of {@link CandidateIdDO}
	 * @throws GenericDataModelException
	 */
	public List<AcsBatchCandidateAssociation> getCandIdsbyBatchIdForTestEnds(String batchCode, boolean forceStart)
			throws GenericDataModelException {
		logger.info("specified input : batchId = {} and forceStart = {}", batchCode, forceStart);
		if (batchCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		List<AcsBatchCandidateAssociation> candidateIds = new ArrayList<AcsBatchCandidateAssociation>();
		params.put("batchCode", batchCode);
		// feedback changes
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		// ends feedback changes
		if (forceStart) {
			candidateIds =
					session.getListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CAND_IDS_BY_BATCHID_FOR_TEST_ENDS, params);
		} else {
			params.put("status", false);
			candidateIds =
					session.getListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_IDS_BY_BATCHID_FOR_TEST_ENDS, params);
		}
		// logger.info("candidateId's = {}", candidateIds);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	/**
	 * Gets the candidateId's of those candidates who logged in under the specified batch.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @return list of {@link CandidateIdDO}
	 * @throws GenericDataModelException
	 */
	@Deprecated
	public List<CandidateIdDO> getAllCandIdsbyBatchCode(String batchCode) throws GenericDataModelException {
		logger.info("specified input : batchId = {}", batchCode);
		if (batchCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		List<CandidateIdDO> candidateIds = new ArrayList<CandidateIdDO>();
		candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CAND_IDS_BY_BATCHID, params, 0,
						CandidateIdDO.class);
		// logger.info("candidateId's = {}", candidateIds);
		if (candidateIds.equals(Collections.<Object> emptyList()))
			return null;
		return candidateIds;
	}

	public ManualPacksUploadDetailsStats getManualPacksUploadData(String eventCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isManualUpload", true);
		params.put("eventCode", eventCode);
		List<AcsPacks> packRequestors =
				(List<AcsPacks>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_MANUALUPLOAD_PACKREQUESTOR_ON_EVENT_CODE, params, 0);
		if (packRequestors.equals(Collections.<Object> emptyList())) {
			logger.info("There are no packs uploaded manually");
			return null;
		}

		List<ManualPacksUploadDetailsDO> manualPacksUploadDetails = new ArrayList<ManualPacksUploadDetailsDO>();
		for (Iterator iterator = packRequestors.iterator(); iterator.hasNext();) {
			AcsPacks packRequestor = (AcsPacks) iterator.next();
			String status = getManualPackStatus(packRequestor.getPackStatus());
			ManualPacksUploadDetailsDO manualPacksUploadDetailsDO =
					new ManualPacksUploadDetailsDO(packRequestor.getPackCode(), packRequestor.getVersionNumber(),
							packRequestor.getPackType(), packRequestor.getManualUploadDateTime(), status);

			// get packId associated to specified packCode and version
			/*
			 * int packId = pds.getPackIdbyPackIdentifierAndVersion(packRequestor.getPackIdentifier(),
			 * packRequestor.getVersionNumber());
			 */

			// String eventCode = null;
			StringBuffer batchCodesBuffer = null;
			if (packRequestor.getPackIdentifier() != null) {
				// gets the list of batch codes for which the this pack belongs to
				List<String> batchCodes = bs.getBatchCodesByPackIdentifier(packRequestor.getPackIdentifier());

				if (batchCodes.equals(Collections.<Object> emptyList())) {
					logger.info("There are no batches are associated to pack with code = {}",
							packRequestor.getPackIdentifier());
				} else {
					// get the event code for which these batches belongs to (ideally for now a pack can not
					// have
					// multiple events hence all the batches should belong to same event so fetch the event
					// code
					// for
					// the first batch)
					// eventCode = cdeas.getEventCodeByBatchCode(batchCodes.get(0));
					batchCodesBuffer = new StringBuffer();
					boolean first = true;
					for (Iterator batchCodesIterator = batchCodes.iterator(); batchCodesIterator.hasNext();) {
						String string = (String) batchCodesIterator.next();
						if (!first) {
							string = " , " + string;
						} else {
							first = false;
						}
						batchCodesBuffer.append(string);
					}
				}
			} else {
				logger.info("Actual pack info is not available for specified packCode = {} and version = {}",
						packRequestor.getPackIdentifier(), packRequestor.getVersionNumber());
			}

			manualPacksUploadDetailsDO.setEventCode(eventCode);
			if (batchCodesBuffer == null) {
				manualPacksUploadDetailsDO.setBatchCodes(null);
			} else {
				manualPacksUploadDetailsDO.setBatchCodes(batchCodesBuffer.toString());
			}

			manualPacksUploadDetails.add(manualPacksUploadDetailsDO);
		}
		ManualPacksUploadDetailsStats manualPacksUploadDetailsStats =
				new ManualPacksUploadDetailsStats(manualPacksUploadDetails);

		return manualPacksUploadDetailsStats;
	}

	public String getManualPackStatus(PacksStatusEnum packRequestorStatusEnum) {

		String statusMessage = "Uploaded";
		switch (packRequestorStatusEnum) {
			case EVENT_TIME_ELAPSED:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case BATCH_TIME_ELAPSED:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case BATCH_START_TIME_ELAPSED:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case REJECTED:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case INVALID_VERSION:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case NEWER_VERSION_EXISTS:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case VERSION_EXISTS_AND_ACTIVATED:
				statusMessage = packRequestorStatusEnum.getType();
				break;

			case DOWNLOAD_FAILED:
				statusMessage = "Upload failed";
				break;
			case UPLOAD_FAILED:
				statusMessage = packRequestorStatusEnum.getType();
				break;
			case IGNORE_AS_APACK_RELATED_TO_CURRENT_RUNNING_BATCH:
				statusMessage = packRequestorStatusEnum.getType();

			default:
				logger.info("default status message:{} is shown for status = {}", statusMessage,
						packRequestorStatusEnum.getType());
				break;
		}
		return statusMessage;

	}

	public PaginationDataDO getscheduledBatchData(String customerCode, String divisionCode, String eventCode,
			Calendar fromDate, Calendar toDate, int startIndex, int count) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		Map<String, Enum<?>> enumMap = new HashMap<String, Enum<?>>();
		params.put(ACSConstants.EVENT_ID, eventCode);
		List<AcsBatch> batchDetailsList = null;
		List<ACSPackDetailsDo> packDetailList = null;
		Map<String, Map<String,Integer>> maxVersionDetails = new HashMap<String, Map<String,Integer>>();
		long totalResultsCount = 0l;
		List<Object> scheduledBatchDataList = new ArrayList<Object>();
		AcsEvent acsEvent = cdeas.getEventDetailsByEventCode(eventCode);
		if (((acsEvent.getEventType() == EventType.DISTRIBUTED_WALKIN
				|| acsEvent.getEventType() == EventType.CENTRALIZED_WALKIN))) {
			List<PackContent> packList = new ArrayList<>();
			packList.add(PackContent.valueOf(PackContent.Apack.name().toString()));
			packList.add(PackContent.valueOf(PackContent.Bpack.name().toString()));
			packList.add(PackContent.valueOf(PackContent.Qpack.name().toString()));

			params.put("packList", packList);
		
			String query = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_PACKTYPE;
			String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_COUNT;
			//long startTime = System.currentTimeMillis();
			if (fromDate != null && toDate != null) {
				params.put(ACSConstants.FROM_DATE, fromDate);
				params.put(ACSConstants.TO_DATE, toDate);
				
				query = query
						+ " and b.batchStartTime <= (:toDate) and (b.deltaRpackGenerationTimeByAcs >= (:fromDate) or b.deltaRpackGenerationTime >= (:fromDate))";

				resultsCountQuery = resultsCountQuery
						+ " and b.batchStartTime <= (:toDate) and (b.deltaRpackGenerationTimeByAcs >= (:fromDate) or b.deltaRpackGenerationTime >= (:fromDate))";

			}
			
			query = query + " order by b.batchStartTime asc";
		    batchDetailsList = session.getResultAsListByQuery(query, params, startIndex, count);
	       /*params.remove(ACSConstants.FROM_DATE);
			params.remove(ACSConstants.TO_DATE);*/
			params.remove("packList");
	       
			totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
			 String query2=ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_VERSION;
			 packDetailList=session.getResultAsListByQuery(query2, params, startIndex, count,ACSPackDetailsDo.class);
			 for (ACSPackDetailsDo maxVersion : packDetailList) {
				  Map<String, Integer> packVersionDetails= maxVersionDetails.get(maxVersion.getBatchCode());
				  if (packVersionDetails==null) {
					  packVersionDetails =new HashMap<String,Integer>();
				  }
				  
				  packVersionDetails.put((PackContent.Rpack.equals(maxVersion.getPackTypes())?maxVersion.getPackSubType().toString(): maxVersion.getPackTypes().toString()), maxVersion.getVersionNumber());
				  maxVersionDetails.put(maxVersion.getBatchCode(), packVersionDetails);
			  }
			
		  } else {

			String query = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO;
			String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_COUNT;
			if (fromDate != null && toDate != null) {
				params.put(ACSConstants.FROM_DATE, fromDate);
				params.put(ACSConstants.TO_DATE, toDate);
				query = query
						+ " and b.batchStartTime <= (:toDate) and (b.deltaRpackGenerationTimeByAcs >= (:fromDate) or b.deltaRpackGenerationTime >= (:fromDate))";

				resultsCountQuery = resultsCountQuery
						+ " and b.batchStartTime <= (:toDate) and (b.deltaRpackGenerationTimeByAcs >= (:fromDate) or b.deltaRpackGenerationTime >= (:fromDate))";

			}
			query = query + " order by b.batchStartTime asc";

			batchDetailsList = session.getResultAsListByQuery(query, params, startIndex, count);

			totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
		
			String query2=ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_VERSION;
			packDetailList=session.getResultAsListByQuery(query2, params, startIndex, count,ACSPackDetailsDo.class);
			
			for (ACSPackDetailsDo maxVersion : packDetailList) {
				  Map<String, Integer> packVersionDetails= maxVersionDetails.get(maxVersion.getBatchCode());
				  if (packVersionDetails==null) {
					  packVersionDetails =new HashMap<String,Integer>();
				  }
				  
				  packVersionDetails.put((PackContent.Rpack.equals(maxVersion.getPackTypes())?maxVersion.getPackSubType().toString(): maxVersion.getPackTypes().toString()), maxVersion.getVersionNumber());
				  maxVersionDetails.put(maxVersion.getBatchCode(), packVersionDetails);
			  }
		}
		if (!batchDetailsList.equals(Collections.<Object> emptyList())) {
			for (Iterator<AcsBatch> batchIterator = batchDetailsList.iterator(); batchIterator.hasNext();) {
				AcsBatch batchDetails = batchIterator.next();
				
				params = new HashMap<String, Object>();
				params.put(ACSConstants.BATCH_CODE, batchDetails.getBatchCode());

				if (batchDetails.getAcspackdetailses() != null && !batchDetails.getAcspackdetailses().isEmpty()) {

					List<String> information = new ArrayList<String>();
					PackStatusdetailsDO bpackStatusdetails = null;
					PackStatusdetailsDO apackStatusdetails = null;
					PackStatusdetailsDO qpackStatusdetails = null;
					PackStatusdetailsDO attendancePackStatusdetails = null;

					// checks whether batch is extended or not if extended add
					// the necessary
					// info to the information list
					if (!batchDetails.getIsBatchExtended().equals(BatchExtensionEnum.NOT_EXTENDED)) {
						information.add(batchDetails.getIsBatchExtended().getType());
					}

					// checks whether late login time is extended for the batch
					// or not if extended add the necessary
					// info to the information list
					if (batchDetails.getIsLateLoginExtended() && batchDetails.getExtendedLateLoginTime() != null) {
						information.add(ACSConstants.LATE_LOGIN_TIME__EXTENDED + TimeUtil
								.convertTimeAsString(batchDetails.getExtendedLateLoginTime().getTimeInMillis()));
					}

					ScheduledBatchDataDO scheduledBatchData = new ScheduledBatchDataDO(batchDetails.getBatchDate(),
							batchDetails.getBatchStartTime(), batchDetails.getMaxBatchEndTime(),
							batchDetails.getCandidateCount());
					scheduledBatchData.setBatchCode(batchDetails.getBatchCode());
					scheduledBatchData.setBatchName(batchDetails.getBatchName());
					scheduledBatchData.setBatchCancelled(batchDetails.getIsBatchCancelled());
					scheduledBatchData.setInformation(information);
					Set<PackStatusdetailsDO> packStatusdetailsDOs = new HashSet<PackStatusdetailsDO>();
					logger.debug("batchDetails.getAcspackdetailses() : " + batchDetails.getAcspackdetailses().size());
					for (Iterator packsIterator = batchDetails.getAcspackdetailses().iterator(); packsIterator
							.hasNext();) {
						AcsPacks packDetails = (AcsPacks) packsIterator.next();
						String packtype =(PackContent.Rpack.equals(packDetails.getPackType())?packDetails.getPackSubType().toString(): packDetails.getPackType().toString());
						
						if(maxVersionDetails.get(batchDetails.getBatchCode()).get(packtype) == packDetails.getVersionNumber()){
					  	 
						switch (packDetails.getPackType()) {
						case Bpack:
							bpackStatusdetails = new PackStatusdetailsDO(packDetails.getPackType(),
									packDetails.getPackStatus(), packDetails.getActualDownloadStartTime(),
									packDetails.getActualActivationStartTime(), packDetails.getDownloadTime(),
									packDetails.getActivationTime(), packDetails.getVersionNumber());
							bpackStatusdetails.setPackCode(packDetails.getPackIdentifier());
							break;
						case Apack:
							apackStatusdetails = new PackStatusdetailsDO(packDetails.getPackType(),
									packDetails.getPackStatus(), packDetails.getActualDownloadStartTime(),
									packDetails.getActualActivationStartTime(), packDetails.getDownloadTime(),
									packDetails.getActivationTime(), packDetails.getVersionNumber());
							apackStatusdetails.setPackCode(packDetails.getPackIdentifier());
							break;
						case Qpack:
							qpackStatusdetails = new PackStatusdetailsDO(packDetails.getPackType(),
									packDetails.getPackStatus(), packDetails.getActualDownloadStartTime(),
									packDetails.getActualActivationStartTime(), packDetails.getDownloadTime(),
									packDetails.getActivationTime(), packDetails.getVersionNumber());
							qpackStatusdetails.setPackCode(packDetails.getPackIdentifier());

							if (batchDetails.getMaxDeltaRpackGenerationTime().after(Calendar.getInstance())) {
								// check for manual trigger link to be
								// enabled or not
								qpackStatusdetails.setEnableLink(getEnableLinkStatus(packDetails));
							}

							break;
						case Rpack:
							PackStatusdetailsDO rpackStatusdetails = new PackStatusdetailsDO(packDetails.getPackType(),
									packDetails.getPackStatus(), packDetails.getActualDownloadStartTime(),
									packDetails.getActualActivationStartTime(), packDetails.getDownloadTime(),
									packDetails.getActivationTime(), packDetails.getVersionNumber());
							rpackStatusdetails.setPackSubType(packDetails.getPackSubType());
							packStatusdetailsDOs.add(rpackStatusdetails);
							break;

						case Attendancepack:
							attendancePackStatusdetails = new PackStatusdetailsDO(packDetails.getPackType(),
									packDetails.getPackStatus(), packDetails.getActualDownloadStartTime(),
									packDetails.getActualActivationStartTime(), packDetails.getDownloadTime(),
									packDetails.getActivationTime(), packDetails.getVersionNumber());
							break;

						default:
							logger.info("There is no such packType = {} defined", packDetails.getPackType());
							break;
						}
					  	  
					}
					}
					if (bpackStatusdetails != null) {
						packStatusdetailsDOs.add(bpackStatusdetails);
					}
					if (apackStatusdetails != null) {
						packStatusdetailsDOs.add(apackStatusdetails);
					}
					if (qpackStatusdetails != null) {
						packStatusdetailsDOs.add(qpackStatusdetails);
					}
					if (attendancePackStatusdetails != null) {
						packStatusdetailsDOs.add(attendancePackStatusdetails);
					}
					scheduledBatchData.setPackStatusDetails(packStatusdetailsDOs);
					scheduledBatchDataList.add(scheduledBatchData);
				}
			}
		}
			
		PaginationDataDO paginationData = new PaginationDataDO(totalResultsCount, scheduledBatchDataList);
		return paginationData;
		
	}

	/**
	 * gets the statistics for the different status available in live dashboard.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public LiveDashboardStatsDO getLivedashboardStatistics(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		// params.put("questionType", QUESTIONTYPE.READING_COMPREHENSION.toString());

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD,
						params, LiveDashboardDetailsDO.class);

		if (liveDashboardDetails.isEmpty())
			return null;

		int candidateIdleCount = 0;
		int candidateInProgressCount = 0;
		int candidateEndedCount = 0;
		int candidateNotStartedCount = 0;
		int playerInActiveCount = 0;
		int systemNotRespondingCount = 0;
		int peripheralsDetectedCount = 0;
		int absentCount = 0;

		Calendar firstStartTime = null;
		Calendar lastStartTime = null;
		Calendar firstEndTime = null;
		Calendar lastEndTime = null;

		for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
			switch (liveDashboardDetailsDO.getTestStatus()) {
				case IDLE:
					candidateIdleCount++;
					break;
				// case IN_PROGRESS:
				// candidateInProgressCount++;
				// break;
				case ENDED:
				case TEST_COMPLETED:
					candidateEndedCount++;
					break;
				case NOT_YET_STARTED:
					candidateNotStartedCount++;
					break;
				case ABSENT:
					absentCount++;
					break;

				default:
					logger.info("No such status type = " + liveDashboardDetailsDO.getTestStatus());
					break;
			}
			if (liveDashboardDetailsDO.getPlayerStatus().equals(HBMStatusEnum.RED)) {
				playerInActiveCount++;
			}
			if (liveDashboardDetailsDO.getSystemStatus().equals(HBMStatusEnum.RED)) {
				systemNotRespondingCount++;
			}
			if (liveDashboardDetailsDO.getPeripheralStatus().equals(HBMStatusEnum.RED)) {
				peripheralsDetectedCount++;
			}

			// calculate first start time and last start time
			if (liveDashboardDetailsDO.getTestStartTime() != null) {
				if (firstStartTime == null || firstStartTime.after(liveDashboardDetailsDO.getTestStartTime()))
					firstStartTime = liveDashboardDetailsDO.getTestStartTime();
				if (lastStartTime == null || lastStartTime.before(liveDashboardDetailsDO.getTestStartTime()))
					lastStartTime = liveDashboardDetailsDO.getTestStartTime();
			}

			// calculate first end time and last end time
			if (liveDashboardDetailsDO.getTestEndTime() != null) {
				if (firstEndTime == null || firstEndTime.after(liveDashboardDetailsDO.getTestEndTime()))
					firstEndTime = liveDashboardDetailsDO.getTestEndTime();
				if (lastEndTime == null || lastEndTime.before(liveDashboardDetailsDO.getTestEndTime()))
					lastEndTime = liveDashboardDetailsDO.getTestEndTime();
			}

			if (liveDashboardDetailsDO.getTestStartTime() != null && liveDashboardDetailsDO.getTestEndTime() == null
					&& !liveDashboardDetailsDO.getTestStatus().equals(CandidateTestStatusEnum.TEST_COMPLETED)
					&& !liveDashboardDetailsDO.getTestStatus().equals(CandidateTestStatusEnum.ENDED))
				candidateInProgressCount++;
		}
		LiveDashboardStatsDO liveDashboardStatsDO =
				new LiveDashboardStatsDO(candidateIdleCount, candidateInProgressCount, candidateEndedCount,
						candidateNotStartedCount, playerInActiveCount, systemNotRespondingCount,
						peripheralsDetectedCount, null);
		liveDashboardStatsDO.setFirstEndTime(firstEndTime);
		liveDashboardStatsDO.setLastEndTime(lastEndTime);
		liveDashboardStatsDO.setFirstStartTime(firstStartTime);
		liveDashboardStatsDO.setLastStartTime(lastStartTime);
		liveDashboardStatsDO.setCandidateAbsentCount(absentCount);
		return liveDashboardStatsDO;
	}

	/**
	 * gets the latest version pack details of all pack types related to the specified batch.
	 * 
	 * @param batchDetailsTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public Set<AcsPacks> getAllLatestVersionsOfPacks(AcsBatch batchDetails) throws GenericDataModelException {
		int prevAttendancePackId = 0;
		AcsPacks prevAttendancePackDetails = null;
		Set<AcsPacks> packDetailsTOs = new HashSet<AcsPacks>();

		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchDetails.getBatchCode());

		/*
		 * List<AcsPacks> acspacks =
		 * session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_PACKS, params, 0, 0);
		 */

		// iterate over the set pack details associated to the specified batch.
		for (Iterator iterator = batchDetails.getAcspackdetailses().iterator(); iterator.hasNext();) {
			AcsPacks packDetailsTO = (AcsPacks) iterator.next();
			switch (packDetailsTO.getPackType()) {
			// as we are maintaining all the versions of attendance pack in pack details table we need to consider only
			// the latest pack and for all the other packs we are maintaining only the latest pack information in pack
			// details table.
				case Attendancepack:
					if (prevAttendancePackId < (packDetailsTO.getVersionNumber())) {
						prevAttendancePackId = (packDetailsTO.getVersionNumber());
						prevAttendancePackDetails = packDetailsTO;
					}
					break;

				default:
					packDetailsTOs.add(packDetailsTO);
					break;
			}
		}

		// finally add latest attendance pack info if exist.
		if (prevAttendancePackDetails != null) {
			packDetailsTOs.add(prevAttendancePackDetails);
		}
		return packDetailsTOs;
	}

	public PaginationDataDO getScheduleDashboardStatistics(String customerId, String divisionId, String eventId,
			Calendar fromDate, Calendar toDate) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		// params.put(ACSConstants.CUSTOMER_ID, customerId);
		// params.put(ACSConstants.DIVISION_ID, divisionId);
		params.put(ACSConstants.EVENT_ID, eventId);

		String query = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO;

		if (fromDate != null && toDate != null) {
			params.put(ACSConstants.FROM_DATE, fromDate);
			params.put(ACSConstants.TO_DATE, toDate);
			query =
					query
							+ " and b.batchStartTime <= (:toDate) and (b.deltaRpackGenerationTimeByAcs >= (:fromDate) or b.deltaRpackGenerationTime >= (:fromDate))";

			// " and b.batchDate >= (:fromDate) and b.batchDate <= (:toDate)";
		}
		query = query + " order by b.batchStartTime asc";
		List<AcsBatch> batchDetailsList = session.getResultAsListByQuery(query, params, 0, 0);

		int notApplicableCount = 0;
		int downloadYetToStartCount = 0;
		int downloadInProgressCount = 0;
		int downloadedCount = 0;
		int downloadFailedCount = 0;
		int activationInProgressCount = 0;
		int activatedCount = 0;
		int activationFailedCount = 0;
		int uploadInProgressCount = 0;
		int uploadedCount = 0;
		int uploadFailedCount = 0;
		int passwordFetchFailedCount = 0;
		int passwordFetchedCount = 0;
		int passwordFetchInProgressCount = 0;

		int rpackUploadedCount = 0;
		int rpackUploadFailedCount = 0;
		int rpackUploadInProgressCount = 0;

		int deltaRpackUploadedCount = 0;
		int deltaRpackUploadFailedCount = 0;
		int deltaRpackUploadInProgressCount = 0;

		int attendancePackUploadedCount = 0;
		int attendancePackUploadFailedCount = 0;
		int attendancePackUploadInProgressCount = 0;

		int aPckDownloadedCount = 0;
		int aPackDownloadFailedCount = 0;
		int aPackDownloadInProgressCount = 0;
		int aPackActivatedCount = 0;
		int aPackActivationFailedCount = 0;
		int aPackDownloadYetToStartCount = 0;
		int aPackActivationInProgressCount = 0;
		int aPackPasswordFetchFailedCount = 0;
		int aPackPasswordFetchedCount = 0;
		int aPackPasswordFetchInProgressCount = 0;

		int bPckDownloadedCount = 0;
		int bPackDownloadFailedCount = 0;
		int bPackDownloadInProgressCount = 0;
		int bPackActivatedCount = 0;
		int bPackActivationFailedCount = 0;
		int bPackDownloadYetToStartCount = 0;
		int bPackActivationInProgressCount = 0;
		int bPackPasswordFetchFailedCount = 0;
		int bPackPasswordFetchedCount = 0;
		int bPackPasswordFetchInProgressCount = 0;

		int qPckDownloadedCount = 0;
		int qPackDownloadFailedCount = 0;
		int qPackDownloadInProgressCount = 0;
		int qPackActivatedCount = 0;
		int qPackActivationFailedCount = 0;
		int qPackDownloadYetToStartCount = 0;
		int qPackActivationInProgressCount = 0;
		int qPackPasswordFetchFailedCount = 0;
		int qPackPasswordFetchedCount = 0;
		int qPackPasswordFetchInProgressCount = 0;

		if (!batchDetailsList.equals(Collections.<Object> emptyList())) {
			for (Iterator<AcsBatch> batchIterator = batchDetailsList.iterator(); batchIterator.hasNext();) {
				AcsBatch batchDetails = batchIterator.next();

				params = new HashMap<String, Object>();
				params.put(ACSConstants.BATCH_CODE, batchDetails.getBatchCode());

				/*
				 * List<AcsPacks> acspacks =
				 * session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_PACKS, params, 0,
				 * 0);
				 */

				if (batchDetails != null && batchDetails.getAcspackdetailses() != null
						&& !batchDetails.getAcspackdetailses().isEmpty()) {
					Set<AcsPacks> packDetailsTOs = getAllLatestVersionsOfPacks(batchDetails);
					if (packDetailsTOs.isEmpty()) {
						continue;
					}
					for (Iterator<AcsPacks> packsIterator = packDetailsTOs.iterator(); packsIterator.hasNext();) {
						AcsPacks packDetails = packsIterator.next();
						// calculate statistics based on different statuses available for schedule dashboard
						switch (packDetails.getPackStatus()) {
							case NOT_APPLICABLE:
								notApplicableCount++;
								break;
							case DOWNLOAD_YET_TO_START:
								downloadYetToStartCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackDownloadYetToStartCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackDownloadYetToStartCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackDownloadYetToStartCount++;

								}
								break;
							case DOWNLOAD_IN_PROGRESS:
								downloadInProgressCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackDownloadInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackDownloadInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackDownloadInProgressCount++;

								}
								break;
							case DOWNLOADED:
							case ACTIVATION_START_TRIGGERED:
								downloadedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPckDownloadedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPckDownloadedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPckDownloadedCount++;

								}
								break;
							case DOWNLOAD_FAILED:
								downloadFailedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackDownloadFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackDownloadFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackDownloadFailedCount++;

								}
								break;
							case ACTIVATION_IN_PROGRESS:
								if (!packDetails.getIsManuallyGenerated())
									activationInProgressCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackActivationInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackActivationInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackActivationInProgressCount++;

								}
								break;
							case ACTIVATED:
								if (!packDetails.getIsManuallyGenerated())
									activatedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackActivatedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackActivatedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackActivatedCount++;

								}
								break;
							case ACTIVATION_FAILED:
								if (!packDetails.getIsManuallyGenerated())
									activationFailedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackActivationFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackActivationFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackActivationFailedCount++;

								}
								break;
							case UPLOAD_IN_PROGRESS:
								if (!packDetails.getIsManuallyGenerated())
									uploadInProgressCount++;
								if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedRpack)) {
									rpackUploadInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedDeltaRpack)) {
									deltaRpackUploadInProgressCount++;
								} else if (packDetails.getPackType().equals(PackContent.Attendancepack)) {
									attendancePackUploadInProgressCount++;
								}
								break;
							case UPLOAD_FAILED:
								if (!packDetails.getIsManuallyGenerated())
									uploadFailedCount++;
								if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedRpack)) {
									rpackUploadFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedDeltaRpack)) {
									deltaRpackUploadFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Attendancepack)) {
									attendancePackUploadFailedCount++;
								}
								break;
							case UPLOADED:
								if (!packDetails.getIsManuallyGenerated())
									uploadedCount++;
								if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedRpack)) {
									rpackUploadedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Rpack)
										&& !packDetails.getIsManuallyGenerated()
										&& packDetails.getPackSubType().equals(PackSubTypeEnum.AutomatedDeltaRpack)) {
									deltaRpackUploadedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Attendancepack)) {
									attendancePackUploadedCount++;
								}
								break;
							case PASSWORDFETCH_IN_PROGRESS:
								passwordFetchInProgressCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackPasswordFetchInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackPasswordFetchInProgressCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackPasswordFetchInProgressCount++;

								}
								break;
							case PASSWORD_FETCHING_FAILED:
								passwordFetchFailedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackPasswordFetchFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackPasswordFetchFailedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackPasswordFetchFailedCount++;

								}
								break;
							case PASSWORD_FETCHED:
								passwordFetchedCount++;
								if (packDetails.getPackType().equals(PackContent.Apack)) {
									aPackPasswordFetchedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Bpack)) {
									bPackPasswordFetchedCount++;

								} else if (packDetails.getPackType().equals(PackContent.Qpack)) {
									qPackPasswordFetchedCount++;

								}
								break;

							default:
								logger.info("No such type = {}", packDetails.getPackStatus());
								break;
						}
					}
				}
			}
		}
		ScheduleDashboardStatsDO scheduleDashboardStatsDO =
				new ScheduleDashboardStatsDO(notApplicableCount, downloadYetToStartCount, downloadInProgressCount,
						downloadedCount, downloadFailedCount, activationInProgressCount, activatedCount,
						activationFailedCount, uploadInProgressCount, uploadedCount, uploadFailedCount);
		scheduleDashboardStatsDO.setPasswordFetchedCount(passwordFetchedCount);
		scheduleDashboardStatsDO.setPasswordFetchFailedCount(passwordFetchFailedCount);
		scheduleDashboardStatsDO.setPasswordFetchInProgressCount(passwordFetchInProgressCount);

		scheduleDashboardStatsDO.setRpackUploadedCount(rpackUploadedCount);
		scheduleDashboardStatsDO.setRpackUploadFailedCount(rpackUploadFailedCount);
		scheduleDashboardStatsDO.setRpackUploadInProgressCount(rpackUploadInProgressCount);

		scheduleDashboardStatsDO.setDeltaRpackUploadedCount(deltaRpackUploadedCount);
		scheduleDashboardStatsDO.setDeltaRpackUploadFailedCount(deltaRpackUploadFailedCount);
		scheduleDashboardStatsDO.setDeltaRpackUploadInProgressCount(deltaRpackUploadInProgressCount);

		scheduleDashboardStatsDO.setAttendancePackUploadedCount(attendancePackUploadedCount);
		scheduleDashboardStatsDO.setAttendancePackUploadFailedCount(attendancePackUploadFailedCount);
		scheduleDashboardStatsDO.setAttendancePackUploadInProgressCount(attendancePackUploadInProgressCount);

		scheduleDashboardStatsDO.setaPackActivatedCount(aPackActivatedCount);
		scheduleDashboardStatsDO.setaPackActivationFailedCount(aPackActivationFailedCount);
		scheduleDashboardStatsDO.setaPackDownloadFailedCount(aPackDownloadFailedCount);
		scheduleDashboardStatsDO.setaPackDownloadInProgressCount(aPackDownloadInProgressCount);
		scheduleDashboardStatsDO.setaPackDownloadYetToStartCount(aPackDownloadYetToStartCount);
		scheduleDashboardStatsDO.setaPckDownloadedCount(aPckDownloadedCount);
		scheduleDashboardStatsDO.setaPackActivationInProgressCount(aPackActivationInProgressCount);
		scheduleDashboardStatsDO.setaPackPasswordFetchedCount(aPackPasswordFetchedCount);
		scheduleDashboardStatsDO.setaPackPasswordFetchFailedCount(aPackPasswordFetchFailedCount);
		scheduleDashboardStatsDO.setaPackPasswordFetchInProgressCount(aPackPasswordFetchInProgressCount);

		scheduleDashboardStatsDO.setbPackActivatedCount(bPackActivatedCount);
		scheduleDashboardStatsDO.setbPackActivationFailedCount(bPackActivationFailedCount);
		scheduleDashboardStatsDO.setbPackDownloadFailedCount(bPackDownloadFailedCount);
		scheduleDashboardStatsDO.setbPackDownloadInProgressCount(bPackDownloadInProgressCount);
		scheduleDashboardStatsDO.setbPackDownloadYetToStartCount(bPackDownloadYetToStartCount);
		scheduleDashboardStatsDO.setbPckDownloadedCount(bPckDownloadedCount);
		scheduleDashboardStatsDO.setbPackActivationInProgressCount(bPackActivationInProgressCount);
		scheduleDashboardStatsDO.setbPackPasswordFetchedCount(bPackPasswordFetchedCount);
		scheduleDashboardStatsDO.setbPackPasswordFetchFailedCount(bPackPasswordFetchFailedCount);
		scheduleDashboardStatsDO.setbPackPasswordFetchInProgressCount(bPackPasswordFetchInProgressCount);

		scheduleDashboardStatsDO.setqPackActivatedCount(qPackActivatedCount);
		scheduleDashboardStatsDO.setqPackActivationFailedCount(qPackActivationFailedCount);
		scheduleDashboardStatsDO.setqPackDownloadFailedCount(qPackDownloadFailedCount);
		scheduleDashboardStatsDO.setqPackDownloadInProgressCount(qPackDownloadInProgressCount);
		scheduleDashboardStatsDO.setqPackDownloadYetToStartCount(qPackDownloadYetToStartCount);
		scheduleDashboardStatsDO.setqPckDownloadedCount(qPckDownloadedCount);
		scheduleDashboardStatsDO.setqPackActivationInProgressCount(qPackActivationInProgressCount);
		scheduleDashboardStatsDO.setqPackPasswordFetchedCount(qPackPasswordFetchedCount);
		scheduleDashboardStatsDO.setqPackPasswordFetchFailedCount(qPackPasswordFetchFailedCount);
		scheduleDashboardStatsDO.setqPackPasswordFetchInProgressCount(qPackPasswordFetchInProgressCount);
		PaginationDataDO paginationData = new PaginationDataDO(scheduleDashboardStatsDO);
		return paginationData;
	}

	public PaginationDataDO getAttendanceDashboardStatistics(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchCode);

		List<Object> candidateAttendence =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ATTENDENCE_DASHBOARD_DATA, params,
						0, 0, CandidateAttendenceDO.class);

		// return if there are no candidates
		if (candidateAttendence.isEmpty()) {
			return null;
		}
		int presentCandsCount = 0;
		int absentCandsCount = 0;
		for (Iterator iterator = candidateAttendence.iterator(); iterator.hasNext();) {
			CandidateAttendenceDO candidateAttendenceDO = (CandidateAttendenceDO) iterator.next();
			if (candidateAttendenceDO.getBatchType().equalsIgnoreCase(BatchTypeEnum.OPEN.toString())
					&& !candidateAttendenceDO.isCandidateEnabled()) {
				continue;
			}
			if (candidateAttendenceDO.getAttendence().equals(ACSConstants.IS_PRESENT)) {
				presentCandsCount++;
			} else {
				absentCandsCount++;
			}
		}
		AttendanceDashboardStats attendanceDashboardStats =
				new AttendanceDashboardStats(presentCandsCount, absentCandsCount);
		PaginationDataDO paginationData = new PaginationDataDO(attendanceDashboardStats);
		return paginationData;
	}

	public ManualPacksUploadDetailsStats getManualUploadDashboardStatistics(String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isManualUpload", true);
		params.put("eventCode", eventCode);
		List<AcsPacks> packRequestors =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_MANUALUPLOAD_PACKREQUESTOR_ON_EVENT_CODE,
						params, 0);
		if (packRequestors.equals(Collections.<Object> emptyList())) {
			logger.info("There are no packs uploaded manually");
			return null;
		}
		int batchStartTimeElapsedCount = 0;
		int batchExpiredCount = 0;
		int rejectedCount = 0;
		int eventExpiredCount = 0;
		int downloadFailedCount = 0;
		int invalidVersionCount = 0;
		int newerVersionExistsCount = 0;
		int versionExistsAndActivatedCount = 0;
		int downloadedCount = 0;
		int activatedCount = 0;
		int activationStartTriggeredCount = 0;
		int activationFailedCount = 0;
		int uploadFailedCount = 0;
		int uploadSuccessfull = 0;

		for (Iterator iterator = packRequestors.iterator(); iterator.hasNext();) {
			AcsPacks packRequestor = (AcsPacks) iterator.next();

			switch (packRequestor.getPackStatus()) {
				case EVENT_TIME_ELAPSED:
					eventExpiredCount++;
					uploadFailedCount++;
					break;
				case BATCH_TIME_ELAPSED:
					batchExpiredCount++;
					uploadFailedCount++;
					break;
				case BATCH_START_TIME_ELAPSED:
					batchStartTimeElapsedCount++;
					uploadFailedCount++;
					break;
				case REJECTED:
					rejectedCount++;
					uploadFailedCount++;
					break;
				case INVALID_VERSION:
					invalidVersionCount++;
					uploadFailedCount++;
					break;
				case NEWER_VERSION_EXISTS:
					newerVersionExistsCount++;
					uploadFailedCount++;
					break;
				case VERSION_EXISTS_AND_ACTIVATED:
					versionExistsAndActivatedCount++;
					uploadFailedCount++;
					break;
				case DOWNLOADED:
					downloadedCount++;
					uploadSuccessfull++;
					break;
				case DOWNLOAD_FAILED:
					downloadFailedCount++;
					uploadFailedCount++;
					break;
				case ACTIVATION_START_TRIGGERED:
					activationStartTriggeredCount++;
					uploadSuccessfull++;
					break;
				case ACTIVATION_FAILED:
					activationFailedCount++;
					uploadSuccessfull++;
					break;
				case ACTIVATED:
					activatedCount++;
					uploadSuccessfull++;
					break;

				case META_DATA_RECIEVED:
					uploadSuccessfull++;
					break;
				case PASSWORDFETCH_IN_PROGRESS:
					uploadSuccessfull++;
					break;
				case PASSWORD_FETCHED:
					uploadSuccessfull++;
					break;
				case PASSWORD_FETCHING_FAILED:
					uploadSuccessfull++;
					break;
				case UPLOADED:
					uploadSuccessfull++;
					break;
				case UPLOAD_FAILED:
					uploadFailedCount++;
					break;
				case IGNORE_AS_APACK_RELATED_TO_CURRENT_RUNNING_BATCH:
					uploadFailedCount++;
					break;
				case ACTIVATION_INITIATION_FAILED:
					uploadFailedCount++;
					break;
				default:
					logger.info("No such type exists = {}", packRequestor.getPackStatus());
					break;
			}
		}
		ManualPacksUploadDetailsStats manualPacksUploadDetailsStats =
				new ManualPacksUploadDetailsStats(batchStartTimeElapsedCount, batchExpiredCount, rejectedCount,
						eventExpiredCount, downloadFailedCount, invalidVersionCount, newerVersionExistsCount,
						versionExistsAndActivatedCount, downloadedCount, activatedCount, activationStartTriggeredCount,
						activationFailedCount, uploadFailedCount, uploadSuccessfull);

		return manualPacksUploadDetailsStats;
	}

	/**
	 * The getLiveStatusData API used to get the live status(idle and red cases only) data for the current running
	 * batch.
	 * 
	 * @param latestTimeStamp
	 * @return
	 * @throws GenericDataModelException
	 */
	public LiveDashboardStatsDO getLiveStatusData(String batchCode, Calendar latestTimeStamp)
			throws GenericDataModelException {
		String query = null;
		LiveDashboardStatsDO liveDashboardStatsDO = null;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchCode);
		params.put("playerStatus", HBMStatusEnum.RED.ordinal());
		params.put("systemStatus", HBMStatusEnum.RED.ordinal());

		if (latestTimeStamp != null) {
			params.put("fromTimeStamp", latestTimeStamp);
			params.put("currentTimeStamp", Calendar.getInstance());
			query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_WITHIN_SPECIFIED_TIME;
		} else {
			query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_WITHOUT_SPECIFIED_TIME;
		}

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(query, params, LiveDashboardDetailsDO.class);

		if (!liveDashboardDetails.isEmpty()) {
			liveDashboardStatsDO = new LiveDashboardStatsDO(liveDashboardDetails);
			liveDashboardStatsDO.setLatestModifiedStatusTime(liveDashboardDetails.get(0).getLatestModifiedStatusTime());
		}
		return liveDashboardStatsDO;
	}

	/**
	 * gets the list of current running batch information
	 * 
	 * @param string
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<BatchCodesDO> getCurrentBatches(String eventCode) throws GenericDataModelException {
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

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("dateTime", Calendar.getInstance());
		params.put("postTestBufferTime", postTestBufferTime);
		params.put("preTestBufferTime", preTestBufferTime);
		params.put("eventCode", eventCode);

		List<BatchCodesDO> batchCodesDOs =
				session.getResultListByNativeSQLQuery(
						ACSQueryConstants.QUERY_BATCHDETAILS_FOR_LIVE_DASHBOARD_FETCH_BATCHID_BY_TIMEINSTANCE, params,
						BatchCodesDO.class);
		return batchCodesDOs;
	}

	/**
	 * gets the list of current running batch information
	 * 
	 * @param string
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<BatchDetailsDO> getAllCurrentRunningBatches() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("dateTime", Calendar.getInstance());

		List<BatchDetailsDO> batchDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CURRENT_BATCHES, params, 0,
						BatchDetailsDO.class);

		return batchDetails;
	}

	/**
	 * This API scans the network and provides the information about the connected terminals. This is ued in ACS Ui
	 * configuration page
	 * 
	 * @return {@link ConnectedTerminalDo}
	 * 
	 * @since Apollo v2.0
	 * 
	 */
	public ConnectedTerminalDo scanLAN() {
		logger.trace("Scanning the network..");
		NetworkUtility networkUtility = new NetworkUtility();
		ConnectedTerminalDo connectedTerminalDo = networkUtility.getConnectedTerminalInfo();
		logger.trace("The scanLAN returning {}", connectedTerminalDo);
		return connectedTerminalDo;
	}

	public PacketInformationDO getPackInfoByBatchId(String batchCode, String packType) throws GenericDataModelException {
		logger.info("initiated getPackInfoByBatchId where batchId= {} and packType={}", batchCode, packType);

		PacketInformationDO packetInformation = null;
		AcsBatch batch = bs.getBatchDetailsByBatchCode(batchCode);
		BatchInformationDO batchInformation = new BatchInformationDO();
		if (batch != null)
			batchInformation =
					new BatchInformationDO(batch.getBatchCode(), batch.getBatchDate(), batch.getMaxBatchStartTime(),
							batch.getBatchEndTime());
		// bs.getBatchInformationByBatchId(batchCode);
		if (batchInformation != null) {
			packetInformation = new PacketInformationDO();
			packetInformation.setBatchInformation(batchInformation);

			switch (packType) {
				case "Bpack":
					logger.info("initiated fetching contents of bpack");

					packetInformation.setPackType(PackContent.Bpack);
					packetInformation.setPacketInformation(getBpackInfoByBatchId(batchCode));

					break;
				case "Qpack":
					logger.info("initiated fetching contents of qpack");

					// get the pack details
					AcsPacks qpackDetails = pds.getPackDetailsByBatchCodeAndPackType(batchCode, PackContent.Qpack);

					// get the qpack status
					PacksStatusEnum qpackStatus = qpackDetails.getPackStatus();

					// set the pack type and status
					packetInformation.setPackType(PackContent.Qpack);
					packetInformation.setPackStatus(qpackStatus);

					if ((qpackStatus.equals(PacksStatusEnum.DOWNLOADED)
							|| qpackStatus.equals(PacksStatusEnum.PASSWORD_FETCHED)
							|| qpackStatus.equals(PacksStatusEnum.PASSWORD_FETCHING_FAILED) || qpackStatus
								.equals(PacksStatusEnum.PASSWORDFETCH_IN_PROGRESS))) {
						logger.info("pack is not in acivated mode hence,processing only the pack level assessmnets");

						// get pack level assessments
						/*
						 * packetInformation.setPacketInformation(getAssessmentsInformation(
						 * qpackDetails.getAssessmentCodes(), qpackDetails.getPackIdentifier()));
						 */
					} else if (qpackStatus.equals(PacksStatusEnum.ACTIVATED)) {
						logger.info("pack is in acivated mode hence,processing comple pack information");

						// get complete pack information
						packetInformation.setPacketInformation(getQpackInfoByBatchId(batchCode,
								qpackDetails.getPackCode()));
					} else {
						logger.info("pack level assessments information is not available");
					}

					break;

				case "Rpack1":
					logger.info("initiated fetching contents of rpack");

					packetInformation.setPackType(PackContent.Rpack);
					packetInformation.setPacketInformation(getRpackInfoByBatchIdAndPackSubType(batchCode,
							PackSubTypeEnum.AutomatedRpack));

					break;

				case "Rpack2":
					logger.info("initiated fetching contents of delta rpack");

					packetInformation.setPackType(PackContent.Rpack);
					packetInformation.setPacketInformation(getRpackInfoByBatchIdAndPackSubType(batchCode,
							PackSubTypeEnum.AutomatedDeltaRpack));

					break;

				default:
					logger.info("No such pack type exists = {}", packType);
					break;
			}
		} else {
			logger.info("No batch info avilable for the specified batchId = {}", batchCode);
		}
		return packetInformation;
	}

	/**
	 * gets the bpack information for the specified batch
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<IPackInformationDO> getBpackInfoByBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchCode);
		params.put(ACSConstants.PACK_TYPE, PackContent.Bpack);
		List<IPackInformationDO> bpackInformation =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHINFO_FOR_BATCHCODE, params, 0,
						BpackInformationDO.class);
		return bpackInformation;
	}

	/**
	 * gets the qpack information for the specified batch
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<IPackInformationDO> getQpackInfoByBatchId(String batchId, String packCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchId);
		params.put("packCode", packCode);
		params.put(ACSConstants.PACK_TYPE, PackContent.Qpack);
		// params.put("qpStatus", QuestionPaperStatusEnum.DISABLED);
		List<IPackInformationDO> qpackInformation =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QPACK_INFORMATION_BY_BATCH_ID, params, 0,
						QpackInformationDO.class);
		return qpackInformation;
	}

	/**
	 * gets the qpack information which will be notified to cdb
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Deprecated
	public List<QpackInformationDO> getQpackInfoByBatchIdForCDB(int batchId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchId);
		params.put(ACSConstants.PACK_TYPE, PackContent.Qpack);
		params.put("qpStatus", QuestionPaperStatusEnum.DISABLED);
		List<QpackInformationDO> qpackInformation =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QPACK_INFORMATION_BY_BATCH_ID_CDB, params,
						0, QpackInformationDO.class);
		return qpackInformation;
	}

	/**
	 * gets the qpack information which will be notified to cdb
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<QpackInformationDO> getQpackInfoByPackCodeForCDB(String packCode, String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packCode", packCode);
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put(ACSConstants.PACK_TYPE, PackContent.Qpack);
		List<QpackInformationDO> qpackInformation =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_QPACK_INFORMATION_BY_BATCH_ID, params, 0,
						QpackInformationDO.class);

		return qpackInformation;
	}

	/**
	 * This API provides the list of candidates blocked because of multiple invalid attempts and their status
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Deprecated
	public BlockedCandidateListDO getBlockedCandidates(String batchCode) throws GenericDataModelException {
		logger.debug("Fetching blocked candidates Info for batchId {}", batchCode);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchCode);
		List<BlockedCandidatesInfoDo> blockedCandidatesInfoDos =
				session.getResultAsListByQuery(
						"select candidateLogin as candidateLogin, candidateBlockingStatus as candidateBlockingStatus,numberOfInvalidAttempts as numberOfAttemptsMade from AcsBatchCandidateAssociation where batchId=(:batchId)",
						params, 0, BlockedCandidatesInfoDo.class);
		// String batchCode = bs.getBatchCodebyBatchId(batchId);
		BlockedCandidateListDO blockedCandidateListDO =
				new BlockedCandidateListDO(batchCode, 0, blockedCandidatesInfoDos);
		return blockedCandidateListDO;
	}

	/**
	 * Generates attendance report for the specified event.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param startIndex
	 *            specifies the start index from which the subset of result set should start with ( added for pagination
	 *            purpose)
	 * @param count
	 *            specifies the sub set count of result set to be fetched
	 * @return {@link PaginationDataDO}
	 * @throws GenericDataModelException
	 * @see CandidateAttendenceDO
	 */
	@Deprecated
	public PaginationDataDO getAttendenceReportByEvent(String eventCode, int startIndex, int count)
			throws GenericDataModelException {
		logger.info("Specified input : eventId = {} , startIndex = {} and count = {}", new Object[] { eventCode,
				startIndex, count });

		List<String> batchCodes = getBatchIdsListByEvent(eventCode);

		for (String batchCode : batchCodes) {
			getAttendenceReport(batchCode, startIndex, count);

		}
		return null;

	}

	@Deprecated
	public List<String> getBatchIdsListByEvent(String eventCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);

		List<String> batchIds = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHES_FOR_EVENT, params);
		if (batchIds.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return batchIds;
	}

	/**
	 * Generates attendance report for the specified list of batches.
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param startIndex
	 *            specifies the start index from which the subset of result set should start with ( added for pagination
	 *            purpose)
	 * @param count
	 *            specifies the sub set count of result set to be fetched
	 * @return {@link PaginationDataDO}
	 * @throws GenericDataModelException
	 * @see CandidateAttendenceDO
	 */
	public PaginationDataDO getAttendenceReportForSpecifiedBatchIds(String[] batchCodes, int startIndex, int count)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchIds", Arrays.asList(batchCodes));

		List<CandidateAttendenceDO> candidateAttendenceDOs =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ALL_ATTENDENCE_DASHBOARD_DATA,
						params, startIndex, count, CandidateAttendenceDO.class);

		if (candidateAttendenceDOs.isEmpty()) {
			return null;
		}

		List<Object> attendance = new ArrayList<Object>();
		for (CandidateAttendenceDO candidateAttendenceDO : candidateAttendenceDOs) {
			// Calculate login attempts count, fetch batch candidate association to fetch candidate max and current
			// login attempts

			String loginAttempts =
					getLoginAttemptsInfo(candidateAttendenceDO.getNumberOfValidLoginAttempts(),
							candidateAttendenceDO.getExtendedNumberOfValidLoginAttempts(),
							candidateAttendenceDO.getMaxNumberOfValidLoginAttempts());
			logger.info("login attempts = {} for candidate with id = {} in batch with id = {}", loginAttempts,
					candidateAttendenceDO.getCandidateId(), candidateAttendenceDO.getBatchCode());
			Calendar maxBatchStartTime = authService.getMaxBatchStartTime(candidateAttendenceDO.getBatchStartTime(),
					candidateAttendenceDO.getExtendedBatchStartTime());
			Calendar maxCandidateBatchStartTime = authService.getMaxCandidateBatchStartTime(
					candidateAttendenceDO.getExtendedCandidateBatchStartTime(), maxBatchStartTime);
			Calendar lateLoginTime =
					getMaxLateLoginTime(candidateAttendenceDO.getExtendedLateLoginTimePerCandidate(),
							candidateAttendenceDO.getLateLoginTimeInMins(), maxCandidateBatchStartTime,
							candidateAttendenceDO.getExtendedLateLoginTime());
			logger.info("late login time = {} for candidate with id = {} in batch with id = {}", lateLoginTime,
					candidateAttendenceDO.getCandidateId(), candidateAttendenceDO.getBatchCode());

			Calendar maxDeltaRpackGenTime = candidateAttendenceDO.getDeltaRpackGenerationTime();
			if (candidateAttendenceDO.getDeltaRpackGenerationTimeByACS() != null
					&& candidateAttendenceDO.getDeltaRpackGenerationTimeByACS().after(maxDeltaRpackGenTime)) {
				maxDeltaRpackGenTime = candidateAttendenceDO.getDeltaRpackGenerationTimeByACS();
			}

			// check whether the batch is active or not if active set true
			if ((candidateAttendenceDO.getLogoutTime() != null && !(candidateAttendenceDO.getLogoutTime()
					.equalsIgnoreCase("null"))) || Calendar.getInstance().after(maxDeltaRpackGenTime)) {
				candidateAttendenceDO.setLateLoginExtensionAllowed(false);
			}
			ResultProcReportTO resultProcReportTO =
					resultProcStatusService.findByBatchIDCandID(candidateAttendenceDO.getBatchCode(),
							candidateAttendenceDO.getCandidateId());
			if (resultProcReportTO != null && resultProcReportTO.getPdfGenerationTime() != null) {
				candidateAttendenceDO.setPdfGenerated(true);
				candidateAttendenceDO.setPdfFilePath(resultProcReportTO.getPdfFilePath());
			} else {
				candidateAttendenceDO.setPdfGenerated(false);
				candidateAttendenceDO.setPdfFilePath("");
			}
			candidateAttendenceDO.setLoginAttemts(loginAttempts);
			candidateAttendenceDO.setLateLoginTime(TimeUtil.convertTimeAsString(lateLoginTime.getTimeInMillis(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS));

			attendance.add(candidateAttendenceDO);
		}

		// List<CandidateIdDO> candidateIds = getAllCandIdsbyBatchIds(batchIdsToFetchData);
		PaginationDataDO candidateAttendence = new PaginationDataDO(0, attendance);
		return candidateAttendence;
	}

	/**
	 * getAllCandIdsbyBatchIds API used to get all the candidateIds for specified batchid's
	 * 
	 * @param batchIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Deprecated
	public List<CandidateIdDO> getAllCandIdsbyBatchIds(List<Integer> batchIds) throws GenericDataModelException {
		logger.info("specified input : batchId = {}", batchIds);
		if (batchIds == null) {
			logger.info("Skipping getAllCandIdsbyBatchIds since no batch details found ");
			return null;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchIds);

		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CAND_IDS_BY_ALL_BATCHIDs, params, 0,
						CandidateIdDO.class);
		if (candidateIds.equals(Collections.<Object> emptyList())) {
			logger.info("Skipping getAllCandIdsbyBatchIds since no candId's found ");
			return null;
		}
		return candidateIds;
	}

	/**
	 * Get all {@link AcsBatchCandidateAssociation} for batch code.
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateIdDO> getAllCandBatchAssociationIdsForBatch(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		List<CandidateIdDO> candidateIds =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_CANDBATCHASSOCN_IDS_BY_ALL_BATCHCODE,
						params, 0, CandidateIdDO.class);
		return candidateIds;
	}

	/**
	 * Generates attendance report statistics for the specified list of batches.
	 * 
	 * @param batchCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public PaginationDataDO getAttendanceDashboardStatisticsForSpecifiedBatchIds(String[] batchCodes)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchIds", Arrays.asList(batchCodes));

		List<Object> candidateAttendence =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ALL_ATTENDENCE_DASHBOARD_DATA,
						params, 0, 0, CandidateAttendenceDO.class);

		// return if there is no candidates
		if (candidateAttendence.isEmpty()) {
			return null;
		}

		int presentCandsCount = 0;
		int absentCandsCount = 0;
		for (Iterator iterator = candidateAttendence.iterator(); iterator.hasNext();) {
			CandidateAttendenceDO candidateAttendenceDO = (CandidateAttendenceDO) iterator.next();
			if (candidateAttendenceDO.getAttendence().equals(ACSConstants.IS_PRESENT)) {
				presentCandsCount++;
			} else {
				absentCandsCount++;
			}
		}

		AttendanceDashboardStats attendanceDashboardStats =
				new AttendanceDashboardStats(presentCandsCount, absentCandsCount);
		PaginationDataDO paginationData = new PaginationDataDO(attendanceDashboardStats);
		return paginationData;
	}

	/**
	 * getCandLoginIdsbyBatchId API used to get the Candidate login id's for given BatchId
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateInfoDO> getCandLoginIdsbyBatchCode(String batchCode) throws GenericDataModelException {
		logger.info("specified input : batchCode = {}", batchCode);
		if (batchCode == null) {
			logger.info("skiping getCandLoginIdsbyBatchCode API since batchIds are found null");
			return null;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		List<CandidateInfoDO> candidateInfo =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_LOGIN_IDS_BY_BATCHID, params, 0,
						CandidateInfoDO.class);
		// logger.info("candidateIds = {}", candidateIds);
		if (candidateInfo.equals(Collections.<Object> emptyList()))
			return null;
		return candidateInfo;
	}

	/**
	 * getPresentCandLoginIdsbyBatchCode API used to get the present Candidate login id's for given BatchId
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateInfoDO> getPresentCandLoginIdsbyBatchCode(String batchCode) throws GenericDataModelException {
		logger.info("specified input : batchCode = {}", batchCode);
		if (batchCode == null) {
			logger.info("skiping getPresentCandLoginIdsbyBatchCode API since batchIds are found null");
			return null;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		List<CandidateInfoDO> candidateInfo =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_PRESENT_CAND_LOGIN_IDS_BY_BATCHID, params,
						0, CandidateInfoDO.class);
		// logger.info("candidateIds = {}", candidateIds);
		if (candidateInfo.equals(Collections.<Object> emptyList()))
			return null;
		return candidateInfo;
	}

	/**
	 * getAllBatchDataForDashboardStats API used to get all the batch data statistics for dash board.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LiveDashboardStatsDO getAllBatchDataForDashboardStats(String[] batchCodes) throws GenericDataModelException {
		logger.info("initiated getAllBatchDataForDashboardStats API");
		if (batchCodes == null) {
			logger.info("skiping getAllBatchDataForDashboardStats API since batchIds are found null");
			return null;

		}
		List<String> batchCodesList = Arrays.asList(batchCodes);// getAllBatchIds();
		if (batchCodesList == null) {
			logger.info("skiping getAllBatchDataForDashboardStats API since batchIds are found null");
			return null;

		}
		logger.info("starts getAllBatchDataForDashboardStats API  for fetching following batchId's={}",
				batchCodesList.toString());
		LiveDashboardStatsDO liveDashboardStatsDO = getLivedashboardStatisticsForAllBatches(batchCodesList);
		if (liveDashboardStatsDO == null) {
			logger.info("skiping getAllBatchDataForDashboardStats API since data found null");
			return null;

		}

		return liveDashboardStatsDO;

	}

	/**
	 * getAllBatchDataForDashboard API used to get all the batch data for dash board.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LiveDashboardStatsDO getAllBatchDataForDashboard(String[] batchCodes) throws GenericDataModelException {
		logger.info("initiated getAllBatchDataForDashboard API");
		if (batchCodes == null) {
			logger.info("skiping getAllBatchDataForDashboard API since batchIds are found null");
			return null;

		}
		List<String> batchCodesList = Arrays.asList(batchCodes);// getAllBatchIds();
		if (batchCodesList == null) {
			logger.info("skiping getAllBatchDataForDashboard API since batchIds are found null");
			return null;

		}
		logger.info("starts getAllBatchDataForDashboard API for fetching following batchId's={}",
				batchCodesList.toString());
		LiveDashboardStatsDO liveDashboardStatsDO = getLiveDashboardDataForAllBatches(batchCodesList);
		if (liveDashboardStatsDO == null) {
			logger.info("skiping getAllBatchDataForDashboard API since data found null");
			return null;

		}

		return liveDashboardStatsDO;

	}

	/**
	 * getAllBatchIdsInfo API used to list all batchId's
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchCodesDO> getAllBatchIdsInfo(Calendar fromDate, Calendar toDate) throws GenericDataModelException {
		logger.info("initiated getAllBatchIdsInfo API");

		String query = ACSQueryConstants.QUERY_FETCH_ALL_BATCHIDS;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("fromDate", fromDate);
		params.put("toDate", toDate);

		List<BatchCodesDO> batchInfo = session.getResultAsListByQuery(query, params, 0, BatchCodesDO.class);

		if (batchInfo.equals(Collections.<Object> emptyList())) {
			logger.info("getAllBatchIdsInfo API list out null");
			return null;
		}
		logger.info("getAllBatchIdsInfo API list out following Info={}", batchInfo.toString());
		return batchInfo;
	}

	/**
	 * Unblock candidate which will allow the candidate to login again where we can specify the number of max login
	 * attempts to be allowed for the candidate
	 * 
	 * @param identifier1
	 * @param batchCode
	 * @param userName
	 * @param ipAddress
	 * @param extendedNumberOfValidLoginAttempts
	 *            TODO
	 * @param extendedNumberOfValidLoginAttempts
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean unblockCandidateByIdentifier1(String identifier1, String batchCode, String userName,
			String ipAddress, String eventCode, Integer extendedNumberOfValidLoginAttempts)
			throws GenericDataModelException, CandidateRejectedException {
		logger.info(
				"initiated unblockCandidateByLoginId where identifier1 = {}, batchId={}, userName={} and ipAddress={}",
				identifier1, batchCode, userName, ipAddress);
		// if batchId is 0 then throw an exception
		if (batchCode == null) {
			throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
					"Not Batch exists at this time instance");
		}

		// fetch batch candidate association
		AcsBatchCandidateAssociation batchCandidateAssociation =
				batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandIdentifier(batchCode,
						identifier1);
		logger.info("batchCandidateAssociation = {} for candidate with identifier1={} in batch with id = {}",
				batchCandidateAssociation, identifier1, batchCode);

		if (batchCandidateAssociation != null) {
			// fetch candidate status details
			AcsCandidateStatus candidateStatus =
					candidateService.getCandidateStatus(batchCandidateAssociation.getBcaid());
			logger.info("candidate status details = {} for candidate with id = {} and batch with id = {}",
					candidateStatus, batchCandidateAssociation.getCandidateId(),
					batchCandidateAssociation.getBatchCode());

			// not allowing to clear login when test end time is available
			if (candidateStatus != null && batchCandidateAssociation.getActualTestEndTime() != null) {
				throw new CandidateRejectedException(ACSExceptionConstants.CAND_UN_BLOCK_NOT_ALLOWED,
						"Unblock candidate not allowed as candidate already ended the test");
			}

			if (batchCandidateAssociationService.resetValidLoginAttempts(batchCandidateAssociation,
					extendedNumberOfValidLoginAttempts)) {
				// get batchCode based on batchId
				// String batchCode = bs.getBatchCodebyBatchId(batchCode);
				logger.info("batchCode = {} for batch with id = {}", batchCode, batchCode);

				if (isAuditEnable != null && isAuditEnable.equals("true")) {
					Object[] paramArray =
							{ identifier1, batchCode,
									batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts(),
									(batchCandidateAssociation.getExtendedNumberOfValidLoginAttemptsCount() + 1) };

					HashMap<String, String> logbackParams = new HashMap<String, String>();
					logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
							AdminAuditActionEnum.UNBLOCK_CANDIDATE.toString());
					logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
					logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
					logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
					logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE, eventCode);

					AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
							ACSConstants.AUDIT_UNBLOCK_CANDIDATE_MSG, paramArray);

					// audit for incident log
					IncidentAuditActionDO incidentAuditAction =
							new IncidentAuditActionDO(batchCode, identifier1, ipAddress, TimeUtil.convertTimeAsString(
									Calendar.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
									IncidentAuditLogEnum.UNBLOCK_CANDIDATE);
					incidentAuditAction.setExtendedNumberOfValidLoginAttempts(batchCandidateAssociation
							.getExtendedNumberOfValidLoginAttempts());
					incidentAuditAction.setExtendedNumberOfValidLoginAttemptsCount(batchCandidateAssociation
							.getExtendedNumberOfValidLoginAttemptsCount() + 1);

					AuditTrailLogger.incidentAudit(incidentAuditAction);
				}
				return true;
			} else {
				return false;
			}
		} else {
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_NOT_FOUND,
					"No valid Candidate with the given identifier for the specified batch");
		}
	}

	/**
	 * This api will send the necessary information required to display in UI for unblocking the candidate
	 * 
	 * @param candidateIds
	 * @param batchId
	 * @throws CandidateRejectedException
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public Map<Integer, UnblockCandidateDO> initiateUnblockCandidate(Integer[] candidateIds, String batchCode)
			throws CandidateRejectedException, GenericDataModelException {
		logger.info("initiated initiateUnblockCandidate where candidateIds = {}, batchCode={}", candidateIds, batchCode);
		// if batchId is 0 then throw an exception
		if (batchCode.equals("0") || batchCode.isEmpty()) {
			throw new CandidateRejectedException(ACSExceptionConstants.BATCH_NOT_FOUND,
					"Not Batch exists at this time instance");
		}

		// convert array to list
		List<Integer> candIds = Arrays.asList(candidateIds);

		// fetch batch candidate association
		List<AcsBatchCandidateAssociation> batchCandidateAssociations =
				batchCandidateAssociationService.getBatchCandAssociationsByBatchIdAndCandidateIds(batchCode, candIds);
		logger.info("batchCandidateAssociations = {} for candidates with candidateIds={} in batchCode = {}",
				batchCandidateAssociations, candidateIds, batchCode);

		// fetch candidate identifiers
		List<AcsCandidate> candidateTOs = candidateService.getCandidateByCandIds(candIds);
		logger.info("candidateTos= {}", candidateTOs);

		if (batchCandidateAssociations != null && !batchCandidateAssociations.isEmpty() && candidateTOs != null
				&& !candidateTOs.isEmpty()) {
			// iterate and form a map with candidateIds and identifier1's
			Map<Integer, String> candIdsToIdentifierMap = new HashMap<Integer, String>();
			for (Iterator iterator = candidateTOs.iterator(); iterator.hasNext();) {
				AcsCandidate candidateTO = (AcsCandidate) iterator.next();
				candIdsToIdentifierMap.put(candidateTO.getCandidateId(), candidateTO.getIdentifier1());
			}

			Map<Integer, UnblockCandidateDO> candIdsToUnblockDOMap = new HashMap<Integer, UnblockCandidateDO>();

			// iterate over the batch candidate associations
			for (Iterator iterator = batchCandidateAssociations.iterator(); iterator.hasNext();) {
				AcsBatchCandidateAssociation batchCandidateAssociation = (AcsBatchCandidateAssociation) iterator.next();

				// fetch max allowed login attempts fetches extended no of valid login attempts if already specified
				// otherwise considers the value which is part of br rules
				int maxNumberOfValidLoginAttempts = batchCandidateAssociation.getExtendedNumberOfValidLoginAttempts();
				if (maxNumberOfValidLoginAttempts == 0) {
					maxNumberOfValidLoginAttempts =
							candidateService.getMaxNumberOfValidLoginAttempts(
									batchCandidateAssociation.getAssessmentCode(),
									batchCandidateAssociation.getBatchCode());
				}
				logger.info("maxNumberOfValidLoginAttempts = {} for candidate with id = {} and batchCode = {}",
						maxNumberOfValidLoginAttempts, batchCandidateAssociation.getCandidateId(), batchCode);

				UnblockCandidateDO unblockCandidateDO =
						new UnblockCandidateDO(batchCandidateAssociation.getCandidateId(),
								batchCandidateAssociation.getNumberOfValidLoginAttempts(),
								maxNumberOfValidLoginAttempts);
				unblockCandidateDO
						.setIdentifier1(candIdsToIdentifierMap.get(batchCandidateAssociation.getCandidateId()));

				if (maxNumberOfValidLoginAttempts != 0) {
					// will allow unblock only for blocked candidates
					if (unblockCandidateDO.getNumberOfValidLoginAttempts() != unblockCandidateDO
							.getMaxNumberOfValidLoginAttempts()
							|| !(batchCandidateAssociation.getCandidateBlockingStatus()
									.equals(CandidateBlockingStatus.BLOCKED))) {
						unblockCandidateDO.setMessage(ACSConstants.UNBLOCK_INITIATION_FAILURE_MESSAGE);
					}
				} else {
					unblockCandidateDO.setMessage(ACSConstants.UNBLOCK_NOT_ALLOWED_MESSAGE);
				}

				// put it in a map
				candIdsToUnblockDOMap.put(batchCandidateAssociation.getCandidateId(), unblockCandidateDO);
			}
			return candIdsToUnblockDOMap;
		} else {
			throw new CandidateRejectedException(ACSExceptionConstants.CAND_NOT_FOUND,
					"No Valid Candidates with the given identifier");
		}
	}

	/**
	 * 
	 * @param batchCodeList
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LiveDashboardStatsDO getLiveDashboardDataForAllBatches(List<String> batchCodeList)
			throws GenericDataModelException {
		logger.info("current batchCodeList = {}", batchCodeList);
		if (batchCodeList == null)
			return null;

		HashMap<String, Object> params = new HashMap<String, Object>();
		// params.put("questionType", QUESTIONTYPE.READING_COMPREHENSION.toString());

		List<LiveDashboardDetailsDO> liveDashboardDetailsList = new ArrayList<LiveDashboardDetailsDO>();

		for (String batchCode : batchCodeList) {
			params.put("batchCode", batchCode);

			List<LiveDashboardDetailsDO> liveDashboardDetails =
					session.getResultListByNativeSQLQuery(
							ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD, params,
							LiveDashboardDetailsDO.class);

			if (liveDashboardDetails.isEmpty()) {
				continue;
			}

			Map<String, AcsAssessment> assessments = new HashMap<String, AcsAssessment>();

			// check whether ACS need to add candidate lost time automatically or not
			boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
			logger.info("isLostTimeAddedAutomatically falg = {} for batch with id = {}", isLostTimeAddedAutomatically,
					batchCodeList);

			for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
				int candidateId = liveDashboardDetailsDO.getCandidateId();
				// String batchCode = liveDashboardDetailsDO.getBatchCode();

				AcsAssessment assessmentDetails = null;
				String assessmentCode = cdeas.getAssessmentIdbyCandIDAndBatchID(candidateId, batchCode);
				if (assessmentCode != null && !assessments.containsKey(assessmentCode)) {

					assessmentDetails = cdeas.getAssessmentDetailsByAssessmentCode(assessmentCode);
					assessments.put(assessmentCode, assessmentDetails);
				}
				liveDashboardDetailsDO.setAssessmentName(assessments.get(assessmentCode).getAssessmentName());

				// calculate remaining time for the candidate
				long remainingTime;
				if (liveDashboardDetailsDO.getActualSectionStartedTime() == null) {
					remainingTime = 0;
					if (liveDashboardDetailsDO.getTestEndTime() == null
							&& liveDashboardDetailsDO.getTestStartTime() != null) {
						Calendar currentTime = Calendar.getInstance();
						long lastHeartBeatTimeInMillis =
								liveDashboardDetailsDO.getLastHeartBeatTime().getTimeInMillis();
						long currentTimeInMillis = currentTime.getTimeInMillis();

						int hbmTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
						logger.info("configured heart beat time interval = {}", hbmTimeInterval);

						long inActiveTime = (currentTimeInMillis - lastHeartBeatTimeInMillis);
						logger.info("inActive time for heart beat = {}", inActiveTime);

						if (inActiveTime > hbmTimeInterval) {
							currentTime.setTime(liveDashboardDetailsDO.getLastHeartBeatTime().getTime());
							logger.info(
									"heart beat is elapsed according to the configured value hence, considering the candidate with identifier = {} as crashed and considering lastHeartBeatTime = {} as current time",
									liveDashboardDetailsDO.getIdentificationNumber(), liveDashboardDetailsDO
											.getLastHeartBeatTime().getTime());
						} else {
							logger.info(
									"heart beat is according to the configured value hence, not considering the candidate with identifier = {} as crashed and considering current time = {} as it is",
									liveDashboardDetailsDO.getIdentificationNumber(), currentTime.getTime());
						}

						// calculate remaining time and check whether it is non negative if not trim it based on the
						// batch
						// end
						// time else send 0
						remainingTime =
								(liveDashboardDetailsDO.getAllotedDuration().longValue() - ((currentTime
										.getTimeInMillis() - liveDashboardDetailsDO.getTestStartTime()
										.getTimeInMillis()) / 1000));
						logger.info("spent time = {} for candidate with id = {} in batch with id = {}", remainingTime,
								candidateId, batchCode);
						if (isLostTimeAddedAutomatically) {
							remainingTime = remainingTime + liveDashboardDetailsDO.getPrevLostTime().longValue();
							logger.info(
									"automatically adding the lost time = {} for candidate with id = {} in batch with id = {}",
									liveDashboardDetailsDO.getPrevLostTime(), candidateId, batchCode);
						} else {
							logger.info("automatic addition of lost time is disabled for batch with id = {} ",
									batchCode);
						}
						// add the previous successful extended time's for the candidate if any
						logger.info(
								"adding previous successful extension time for candidate with id = {} in batch with id = {}",
								liveDashboardDetailsDO.getTotalExtendedExamDuration(), candidateId, batchCode);
						remainingTime =
								remainingTime + (liveDashboardDetailsDO.getTotalExtendedExamDuration().longValue());

						if (remainingTime > 0) {
							logger.info("remaining time is greater than 0 hence, initiating trimming the duration based on batch end time");
							Calendar maxDeltaRpackGenTime = liveDashboardDetailsDO.getBatchEndTime();
							if (liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS() != null
									&& liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS().after(
											maxDeltaRpackGenTime)) {
								maxDeltaRpackGenTime = liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS();
							}

							// trim the remaining time based on the batch end time
							remainingTime =
									trimDurationOnBatchEndTime(currentTime, remainingTime, maxDeltaRpackGenTime);
						} else {
							remainingTime = 0;
						}
						logger.info("remaining time = {} for candidate with id = {}", remainingTime, candidateId);
					}
					liveDashboardDetailsDO.setRemainingTime(remainingTime);
				} else {
					liveDashboardDetailsDO.setRemainingTime(ACSConstants.NEGATIVE_VALUE);
				}
				liveDashboardDetailsList.add(liveDashboardDetailsDO);
			}
		}

		LiveDashboardStatsDO liveDashboardStatsDO = new LiveDashboardStatsDO(liveDashboardDetailsList);
		return liveDashboardStatsDO;
	}

	/**
	 * 
	 * @param batchIdList
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public LiveDashboardStatsDO getLivedashboardStatisticsForAllBatches(List<String> batchIdList)
			throws GenericDataModelException {
		logger.info("current batchId = {}", batchIdList);
		if (batchIdList == null) {
			logger.info("batchId = {}, which is invalid", batchIdList);
			return null;
		}

		LiveDashboardStatsDO liveDashboardStatsDO = new LiveDashboardStatsDO();

		for (Iterator iterator = batchIdList.iterator(); iterator.hasNext();) {
			String batchCode = (String) iterator.next();

			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("batchCode", batchCode);
			// params.put("questionType", QUESTIONTYPE.READING_COMPREHENSION.toString());
			List<LiveDashboardDetailsDO> liveDashboardDetails =
					session.getResultListByNativeSQLQuery(
							ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD, params,
							LiveDashboardDetailsDO.class);
			// logger.info("liveDashboardDetails = {}", liveDashboardDetails);
			if (liveDashboardDetails.equals(Collections.<Object> emptyList()))
				continue;

			int candidateIdleCount = 0;
			int candidateInProgressCount = 0;
			int candidateEndedCount = 0;
			int candidateNotStartedCount = 0;
			int absenteeCount = 0;

			int playerInActiveCount = 0;
			int systemNotRespondingCount = 0;
			int peripheralsDetectedCount = 0;

			for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
				switch (liveDashboardDetailsDO.getTestStatus()) {
					case IDLE:
						candidateIdleCount++;
						break;
					case IN_PROGRESS:
						candidateInProgressCount++;
						break;
					case ENDED:
						candidateEndedCount++;
						break;
					case NOT_YET_STARTED:
						candidateNotStartedCount++;
						break;
					case ABSENT:
						absenteeCount++;
						break;

					default:
						logger.info("No such status type = " + liveDashboardDetailsDO.getTestStatus());
						break;
				}
				if (liveDashboardDetailsDO.getPlayerStatus().equals(HBMStatusEnum.RED)) {
					playerInActiveCount++;
				}
				if (liveDashboardDetailsDO.getSystemStatus().equals(HBMStatusEnum.RED)) {
					systemNotRespondingCount++;
				}
				if (liveDashboardDetailsDO.getPeripheralStatus().equals(HBMStatusEnum.RED)) {
					peripheralsDetectedCount++;
				}

			}

			liveDashboardStatsDO.setCandidateIdleCount(candidateIdleCount
					+ liveDashboardStatsDO.getCandidateIdleCount());
			liveDashboardStatsDO.setCandidateInProgressCount(candidateInProgressCount
					+ liveDashboardStatsDO.getCandidateInProgressCount());
			liveDashboardStatsDO.setCandidateEndedCount(candidateEndedCount
					+ liveDashboardStatsDO.getCandidateEndedCount());
			liveDashboardStatsDO.setCandidateNotStartedCount(candidateNotStartedCount
					+ liveDashboardStatsDO.getCandidateNotStartedCount());
			liveDashboardStatsDO.setPlayerInActiveCount(playerInActiveCount
					+ liveDashboardStatsDO.getPlayerInActiveCount());
			liveDashboardStatsDO.setSystemNotRespondingCount(systemNotRespondingCount
					+ liveDashboardStatsDO.getSystemNotRespondingCount());
			liveDashboardStatsDO.setPeripheralsDetectedCount(peripheralsDetectedCount
					+ liveDashboardStatsDO.getPeripheralsDetectedCount());
			liveDashboardStatsDO
					.setCandidateAbsentCount(absenteeCount + liveDashboardStatsDO.getCandidateAbsentCount());

		}
		return liveDashboardStatsDO;
	}

	/**
	 * gets the login attempts information for a candidate where assessment level login attempts count takes priority if
	 * there is no candidate level extended login attempts exist
	 * 
	 * @param batchCandidateAssociationTO
	 * @param assessmentDetailsTO
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	@Deprecated
	public String getLoginAttemptsInfo(AcsBatchCandidateAssociation batchCandidateAssociationTO, int maxLoginAttempts) {
		// fetch the login attempts display format
		String loginAttempts =
				ACSConstants.LOGIN_ATTEMPTS_FORMAT.replace(ACSConstants.LOGIN_ATTEMPTS_NUMERATOR,
						String.valueOf(batchCandidateAssociationTO.getNumberOfValidLoginAttempts()));
		logger.info("login attempts format = {}", loginAttempts);

		if (batchCandidateAssociationTO.getExtendedNumberOfValidLoginAttempts() != 0) {
			logger.info("extended login attempts exists");

			// consider extended login attempts if exists
			loginAttempts =
					loginAttempts.replace(ACSConstants.LOGIN_ATTEMPTS_DENOMINATOR,
							String.valueOf(batchCandidateAssociationTO.getExtendedNumberOfValidLoginAttempts()));
		} else if (maxLoginAttempts != 0) {
			logger.info("max login attempts are not zero");

			// consider the actual login attempts count from QPD if not zero
			loginAttempts =
					loginAttempts.replace(ACSConstants.LOGIN_ATTEMPTS_DENOMINATOR, String.valueOf(maxLoginAttempts));
		} else {
			loginAttempts = "N/A";
		}
		return loginAttempts;
	}

	/**
	 * gets the login attempts information for a candidate where assessment level login attempts count takes priority if
	 * there is no candidate level extended login attempts exist
	 * 
	 * @param batchCandidateAssociationTO
	 * @param assessmentDetailsTO
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	public String getLoginAttemptsInfo(int numberOfValidLoginAttempts, int extendedNumberOfValidLoginAttempts,
			int maxLoginAttempts) {
		// fetch the login attempts display format
		String loginAttempts =
				ACSConstants.LOGIN_ATTEMPTS_FORMAT.replace(ACSConstants.LOGIN_ATTEMPTS_NUMERATOR,
						String.valueOf(numberOfValidLoginAttempts));
		logger.info("login attempts format = {}", loginAttempts);

		if (extendedNumberOfValidLoginAttempts != 0) {
			logger.info("extended login attempts exists");

			// consider extended login attempts if exists
			loginAttempts =
					loginAttempts.replace(ACSConstants.LOGIN_ATTEMPTS_DENOMINATOR,
							String.valueOf(extendedNumberOfValidLoginAttempts));
		} else if (maxLoginAttempts != 0) {
			logger.info("max login attempts are not zero");

			// consider the actual login attempts count from QPD if not zero
			loginAttempts =
					loginAttempts.replace(ACSConstants.LOGIN_ATTEMPTS_DENOMINATOR, String.valueOf(maxLoginAttempts));
		} else {
			loginAttempts = "N/A";
		}
		return loginAttempts;
	}

	/**
	 * calculates max late login time basically batch level late login time takes priority than assessment level late
	 * login time and candidate level late login time extension happens incrementally compared to the existing
	 * prioritized late login time hence , need to compare candidate and batch level extension if both exists and take
	 * the max value to display in UI as batch level extension is handled by DX and we don't have the necessary info to
	 * find out which is latest
	 * 
	 * @param candidateAttendenceDO
	 * @param batchCandidateAssociationTO
	 * @param assessmentDetailsTO
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	public Calendar getMaxLateLoginTime(Calendar extendedLateLoginTimePerCandidate, int assessmentLateLoginTime,
			Calendar batchStartTime, Calendar extendedLateLoginTimePerBatch) {
		logger.info(
				"initiated getMaxLateLoginTime where candidate level late login time = {} and batch level late login time = {}",
				extendedLateLoginTimePerCandidate, extendedLateLoginTimePerBatch);
		Calendar lateLoginTime = null;
		if (extendedLateLoginTimePerBatch == null) {
			if (extendedLateLoginTimePerCandidate == null) {
				logger.info("batch and candidate level late login times are null");

				// if both candidate and batch level late login times are null then consider assessment level late login
				// time
				Calendar assessmentLevelLLT = (Calendar) batchStartTime.clone();
				assessmentLevelLLT.add(Calendar.MINUTE, assessmentLateLoginTime);

				lateLoginTime = assessmentLevelLLT;
			} else {
				logger.info("batch level late login time is null but candidate level late login time is not null");

				// if batch level late login time is null and candidate level late login time is not null then consider
				// candidate level late login time because candidate level late login happens on top of assessment late
				// login time
				lateLoginTime = extendedLateLoginTimePerCandidate;
			}
		} else {
			if (extendedLateLoginTimePerCandidate == null) {
				logger.info("batch level late login time is not null but candidate level late login time is null");

				// if batch level late login time is not null and candidate level late login time is null then consider
				// batch level late login time because batch level late login time is prioritized than assessment late
				// login time
				lateLoginTime = extendedLateLoginTimePerBatch;
			} else {
				logger.info("batch and candidate level late login times are not null");

				// if both candidate and batch level late login times are not null then consider max of them
				if (extendedLateLoginTimePerCandidate.after(extendedLateLoginTimePerBatch)) {
					lateLoginTime = extendedLateLoginTimePerCandidate;
				} else {
					lateLoginTime = extendedLateLoginTimePerBatch;
				}
			}
		}
		return lateLoginTime;
	}

	/**
	 * gets all active and future batch information i.e current running batches and batches which are not started
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<BatchExtensionDO> getActiveAndFutureBatches(String eventCode) throws GenericDataModelException {
		logger.info("getActiveAndFutureBatches called with eventcode = {} " + eventCode);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("currentDateTime", Calendar.getInstance());
		params.put("eventCode", eventCode);

		boolean isBatchCompletionEnabled = acsPropertyUtil.isBatchCompletionEnabled(eventCode);
		logger.debug("Batch completion enalbed is {} for event:{}", isBatchCompletionEnabled, eventCode);

		List<BatchExtensionDO> batchExtensionDOs =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_FUTUREBATCHES, params, 0,
						BatchExtensionDO.class);
		if (batchExtensionDOs != null && !batchExtensionDOs.isEmpty()) {
			for (Iterator iterator = batchExtensionDOs.iterator(); iterator.hasNext();) {
				BatchExtensionDO batchExtensionDO = (BatchExtensionDO) iterator.next();

				if (!isBatchCompletionEnabled) {
					batchExtensionDO.setBatchCompletionStatus(BatchCompletionStatusEnum.BATCH_COMPLETION_DISABLED);
				}

				Calendar batchEndTime =
						TimeUtil.convertStringToCalender(batchExtensionDO.getBatchEndTime(),
								TimeUtil.YYYY_MM_DD_HH_MM_SS);
				batchExtensionDO.setMaxBatchEndTime(batchEndTime);

				if (batchExtensionDO.getBatchEndTimeByACS() != null) {
					Calendar batchEndTimeAtACS =
							TimeUtil.convertStringToCalender(batchExtensionDO.getBatchEndTimeByACS(),
									TimeUtil.YYYY_MM_DD_HH_MM_SS);

					if (batchEndTimeAtACS.after(batchEndTime)) {
						batchExtensionDO.setMaxBatchEndTime(batchEndTimeAtACS);
					}
				}

				Calendar batchStartTime = TimeUtil.convertStringToCalender(batchExtensionDO.getBatchStartTime(),
						TimeUtil.YYYY_MM_DD_HH_MM_SS);
				batchExtensionDO.setBatchStartTime(batchStartTime);

				if (batchExtensionDO.getPostponedBatchStartTime() != null) {
					Calendar postponedBatchStartTime = TimeUtil.convertStringToCalender(
							batchExtensionDO.getPostponedBatchStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS);

					if (postponedBatchStartTime.after(batchStartTime)) {
						batchExtensionDO.setBatchStartTime(postponedBatchStartTime);
					}
				}

			}
		}
		logger.info("getActiveAndFutureBatches returning with batchExtensionDOs = {} " + batchExtensionDOs);
		return batchExtensionDOs;
	}

	/**
	 * Gets batch related details specific to a customer, division and event within the specified date range (date range
	 * applicable only to events and batches).
	 * 
	 * @param customerCode
	 *            auto generated id for customer
	 * @param divisionCode
	 *            auto generated id for division
	 * @param eventCode
	 *            auto generated id for event
	 * @param startDate
	 *            start date in the specified date range
	 * @param endDate
	 *            end date in the specified date range
	 * @return list of {@link BatchDetailsDO}
	 * @throws GenericDataModelException
	 */
	public List<BatchDetailsDO> getBatchDetailsForLogReport(String customerCode, String divisionCode, String eventCode,
			Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info(
				"specified input : customerCode = {} , divisionCode = {} ,eventCode = {}, startDate = {} and endDate = {}",
				new Object[] { customerCode, divisionCode, eventCode, startDate.getTime(), endDate.getTime() });
		if (customerCode == null || divisionCode == null || eventCode == null || startDate == null || endDate == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		params.put("packType", PackContent.Bpack);
		params.put("packStatus", PacksStatusEnum.ACTIVATED);

		List<BatchDetailsDO> batchDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CDE_BATCHES_BY_EVENTCODE_FOR_LOG_REPORT,
						params, 0, BatchDetailsDO.class);
		logger.info("batch details = {}", batchDetails);
		if (batchDetails.equals(Collections.<Object> emptyList()))
			return null;
		return batchDetails;
	}

	/**
	 * checks the link for manual password pop up or activation to be enabled or not.
	 * 
	 * @param packDetailsTO
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean getEnableLinkStatus(AcsPacks packDetailsTO) {
		boolean status = false;
		switch (packDetailsTO.getPackStatus()) {
			case PASSWORD_FETCHING_FAILED:
				if (packDetailsTO.getActivationTime() == null
						|| Calendar.getInstance().after(
								abstractPasswordFetchService.buildPasswordFetcherEndTime(packDetailsTO
										.getActivationTime())) || packDetailsTO.getIsManualUpload()) {
					status = true;
				}
				break;
			case PASSWORD_FETCHED:
			case DOWNLOADED:
			case ACTIVATION_FAILED:
			case ACTIVATION_START_TRIGGERED:
				if (packDetailsTO.getActivationTime() == null
						|| packDetailsTO.getActivationTime().before(Calendar.getInstance())) {
					status = true;
				}
				break;

			default:
				break;
		}
		return status;
	}

	/**
	 * gets the list of applicable assessment codes for the specified pack code if exists
	 * 
	 * @param assessmentCodes
	 * @param packCode
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Deprecated
	public List<IPackInformationDO> getAssessmentsInformation(String assessmentCodes, String packCode) {
		logger.info("intiated getAssessmentsInformation where assessmentCodes = {} and packCode = {}", assessmentCodes,
				packCode);
		List<IPackInformationDO> packInformationDOs = new ArrayList<IPackInformationDO>();

		// check whether pack level assessments exists or not
		if (assessmentCodes != null && !assessmentCodes.isEmpty()) {
			logger.info("pack level assessments exist = {}", assessmentCodes);

			// convert the json to list and iterate over the list
			List<String> assessments = gson.fromJson(assessmentCodes, List.class);

			for (Iterator iterator = assessments.iterator(); iterator.hasNext();) {
				String assessmentCode = (String) iterator.next();

				QpackInformationDO qpackInformationDO = new QpackInformationDO();
				qpackInformationDO.setAssessmentCode(assessmentCode);
				qpackInformationDO.setPackCode(packCode);
				qpackInformationDO.setQpStatus(QuestionPaperStatusEnum.LOADED.name());
				packInformationDOs.add(qpackInformationDO);
			}
		} else {
			logger.info("pack level assessments doesn't exist = {}", assessmentCodes);

			QpackInformationDO qpackInformationDO = new QpackInformationDO();
			qpackInformationDO.setPackCode(packCode);

			packInformationDOs.add(qpackInformationDO);
		}

		logger.info("returning = {}", packInformationDOs);
		return packInformationDOs;
	}

	/**
	 * gets the rpack content for the specified batch and packSubType
	 * 
	 * @param batchId
	 * @param packSubType
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<IPackInformationDO> getRpackInfoByBatchIdAndPackSubType(String batchId, PackSubTypeEnum packSubType)
			throws GenericDataModelException {
		logger.info("initiated getRpackInfoByBatchIdAndPackSubType where batchId={} and packSubType={}", batchId,
				packSubType);
		List<IPackInformationDO> rpackInformationDOs = new ArrayList<IPackInformationDO>();

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchId);
		params.put(ACSConstants.PACK_TYPE, PackContent.Rpack);
		params.put("packSubType", packSubType);

		String rpackInformation =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_PACKCONTENT_FOR_BATCHCODE_AND_PACKTYPE,
						params);

		if (rpackInformation != null && !rpackInformation.isEmpty()) {
			RpackInformationDO rpackInformationDO = new Gson().fromJson(rpackInformation, RpackInformationDO.class);
			rpackInformationDOs.add(rpackInformationDO);
		}

		logger.info("returning rpackInformationDOs={}", rpackInformationDOs);
		return rpackInformationDOs;
	}

	/**
	 * gets the list of applicable assessment codes for the specified pack code if exists
	 * 
	 * @param assessmentCodes
	 * @param packCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<QpackInformationDO> getQpackAssessmentsInformationForCDB(String batchCode)
			throws GenericDataModelException {
		logger.info("intiated getQpackAssessmentsInformationForCDB where batchId = {}", batchCode);
		List<QpackInformationDO> packInformationDOs = new ArrayList<QpackInformationDO>();

		// get the pack details
		AcsPacks qpackDetails = pds.getPackDetailsByBatchCodeAndPackType(batchCode, PackContent.Qpack);
		List<String> assessments = batchCandidateAssociationService.getAssessmentsByBatchCode(batchCode);
		// check whether pack level assessments exists or not
		if (qpackDetails != null && assessments != null && !assessments.isEmpty()) {
			logger.info("pack level assessments exist = {}", assessments);

			// convert the json to list and iterate over the list
			// List<String> assessments = gson.fromJson(qpackDetails.getAssessmentCodes(), List.class);

			for (Iterator iterator = assessments.iterator(); iterator.hasNext();) {
				String assessmentCode = (String) iterator.next();

				QpackInformationDO qpackInformationDO = new QpackInformationDO();
				qpackInformationDO.setAssessmentCode(assessmentCode);
				qpackInformationDO.setPackCode(qpackDetails.getPackIdentifier());
				qpackInformationDO.setQpStatus(QuestionPaperStatusEnum.LOADED.toString());
				packInformationDOs.add(qpackInformationDO);
			}
		} else {
			logger.info("pack level assessments doesn't exist = {}", assessments);

			QpackInformationDO qpackInformationDO = new QpackInformationDO();
			qpackInformationDO.setPackCode(qpackDetails.getPackIdentifier());

			packInformationDOs.add(qpackInformationDO);
		}

		logger.info("returning = {}", packInformationDOs);
		return packInformationDOs;
	}

	/**
	 * @param candidateDOs
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean markAttendanceManually(List<CandidateActionDO> candidateActionDOs) throws GenericDataModelException {
		if (candidateActionDOs.size() == 0)
			return false;
		else {
			for (CandidateActionDO candidateActionDO : candidateActionDOs) {
				batchCandidateAssociationService.updateAttendanceByBatchIdCandidateId(candidateActionDO.getBatchCode(),
						candidateActionDO.getCandID());
			}
			return true;
		}
	}

	public List<ScoreCalculationDO> scoreCalculationForBatch(String batchCode) throws GenericDataModelException {
		logger.info("score Calculation and PDF Generation Processing called..{}", batchCode);
		List<ScoreCalculationDO> scoreCalculationDOs = scoreCalculator.calculateScoresForBatch(batchCode);
		logger.info("score Calculation and PDF Generation Processing returning {}", scoreCalculationDOs);
		return scoreCalculationDOs;
	}

	public List<ScoreCalculationDO> scoreCalculationForCandidates(String batchCode, List<Integer> candidateIds)
			throws GenericDataModelException {
		logger.info("score Calculation and PDF Generation Processing called..{}");
		List<ScoreCalculationDO> scoreCalculationDOs =
				scoreCalculator.calculateScoresForCandidates(batchCode, candidateIds);
		logger.info("score Calculation and PDF Generation Processing returning {}", scoreCalculationDOs);
		return scoreCalculationDOs;
	}

	/**
	 * populates the default data related to the selected assessment.
	 * 
	 * @param candidateLoginCredentialsGenerationDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public LoginIdGenerationDashboardDO initiateLoginIdsGeneration(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		return CandidateLoginCredentialsGeneratorService.getInstance().initiateCandidateLoginCredentialsGeneration(
				candidateLoginCredentialsGenerationDO);
	}

	/**
	 * fetches the list of unused login ids information including the expiry status.
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public UnUsedLoginCredentialsDO getUnUsedLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		return CandidateLoginCredentialsGeneratorService.getInstance().getUnUsedCandidateLoginCredentials(
				candidateLoginCredentialsGenerationDO.getBatchCode(),
				candidateLoginCredentialsGenerationDO.getAssessmentCode(),
				candidateLoginCredentialsGenerationDO.getSetId(),
				candidateLoginCredentialsGenerationDO.getPageNumber(),
				candidateLoginCredentialsGenerationDO.getPageCount(),
				candidateLoginCredentialsGenerationDO.getSearchData(),
				candidateLoginCredentialsGenerationDO.getSortingColumn(),
				candidateLoginCredentialsGenerationDO.getOrderType());
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
	public CandidateLoginCredentialsGenerationDO startLoginIdsGeneration(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO) throws Exception {
		return CandidateLoginCredentialsGeneratorService.getInstance().startCandidateLoginCredentialsGeneration(
				candidateLoginCredentialsGenerationDO);
	}

	/**
	 * starts the candidate login ids enabling per assessment level. LoginIdAssessmentAssociationTO will be filled
	 * during BPACK activation and entries related to generatedLoginCount and lastGeneratedLoginId
	 * 
	 * @param candidateLoginCredentialsGenerationDOs
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 */
	public CandidateLoginCredentialsGenerationDO enableGeneratedLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO) throws Exception {
		candidateLoginCredentialsGenerationDO =
				CandidateLoginCredentialsGeneratorService.getInstance().enableGeneratedLoginIds(
						candidateLoginCredentialsGenerationDO);
		candidateLoginCredentialsGenerationDO
				.setDownloadURL(generateExcelWithGeneratedLoginIds(candidateLoginCredentialsGenerationDO));
		return candidateLoginCredentialsGenerationDO;
	}

	/**
	 * generate excel with specified password
	 * 
	 * @param password
	 * @return
	 * @throws GenericDataModelException
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws InvalidFormatException
	 * 
	 * @since Apollo v2.0
	 */
	public String generateExcelWithUnUsedLoginIds(LoginIdsExportDO loginIdsExportDO) throws GenericDataModelException,
			IOException, InvalidFormatException, GeneralSecurityException {
		String downloadURL = null;
		List<UnUsedLoginCredentialsDO> unUsedLoginCredentialsDOs =
				CandidateLoginCredentialsGeneratorService.getInstance().getActiveCandidateLoginCredentials(
						loginIdsExportDO.getBatchCode(), loginIdsExportDO.getAssessmentCode(),
						loginIdsExportDO.getSetId(), loginIdsExportDO.getPageNumber(), loginIdsExportDO.getPageCount(),
						loginIdsExportDO.getSearchData(), loginIdsExportDO.getSortingColumn(),
						loginIdsExportDO.getOrderType());

		CDEInfoDO cdeInfoDO = cdeas.getCustomerDivisionEventInfoByBatchCode(loginIdsExportDO.getBatchCode());
		List<String> headerValues = getHeaderValuesForExcelWithGenerationTime();

		List<List<Object>> rowValues = new ArrayList<List<Object>>();
		// Taking values from getUnUsedLoginIds List and setting it
		for (UnUsedLoginCredentialsDO unUsedLoginCredentialsDO : unUsedLoginCredentialsDOs) {
			for (UnUsedLoginCredentialsDO unUsedLoginCredentials : unUsedLoginCredentialsDO.getGetUnUsedLoginIds()) {
				List<Object> cellValues = new ArrayList<Object>();
				cellValues.add(cdeInfoDO.getCustomerName());
				cellValues.add(cdeInfoDO.getDivisionName());
				cellValues.add(cdeInfoDO.getEventName());
				cellValues.add(unUsedLoginCredentials.getAssessmentName());
				cellValues.add(unUsedLoginCredentials.getSetId());
				cellValues.add(unUsedLoginCredentials.getLoginId());
				cellValues.add(unUsedLoginCredentials.getPassword());
				cellValues.add(unUsedLoginCredentials.getGeneratedTime() == null ? "" : unUsedLoginCredentials
						.getGeneratedTime());
				cellValues.add(unUsedLoginCredentials.getExpiryTime());
				cellValues.add(unUsedLoginCredentials.getStatus());
				rowValues.add(cellValues);
			}
		}

		// write values to excel and return URl of file
		downloadURL = writeExcelForLoginIds(loginIdsExportDO.getPassword(), downloadURL, headerValues, rowValues);

		// check whether admin audit is enable or not if enabled audit it
		auditExportLoginIds(loginIdsExportDO);

		return downloadURL;
	}

	/**
	 * get Header Values For Excel Export
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<String> getHeaderValuesForExcel() {
		logger.info("--IN-- getHeaderValuesForExcel");
		List<String> headerValues = new ArrayList<String>();
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_CUSTOMER_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_DIVISION_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_EVENT_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_ASSESSMENT_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_SET_ID);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_LOGIN_ID);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_PASSWORD);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_EXPIRY_TIME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_STATUS);
		logger.info("--OUT-- return getHeaderValuesForExcel with : " + headerValues);
		return headerValues;
	}

	/**
	 * get Header Values For Excel Export
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<String> getHeaderValuesForExcelWithGenerationTime() {
		logger.info("--IN-- getHeaderValuesForExcel");
		List<String> headerValues = new ArrayList<String>();
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_CUSTOMER_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_DIVISION_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_EVENT_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_ASSESSMENT_NAME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_SET_ID);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_LOGIN_ID);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_PASSWORD);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_GENERATED_TIME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_EXPIRY_TIME);
		headerValues.add(ACSConstants.LOGIN_IDS_EXPORT_STATUS);
		logger.info("--OUT-- return getHeaderValuesForExcel with : " + headerValues);
		return headerValues;
	}

	/**
	 * auditing Export Login Ids
	 * 
	 * @param loginIdsExportDO
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditExportLoginIds(LoginIdsExportDO loginIdsExportDO) throws GenericDataModelException {
		logger.info("--IN-- auditExportLoginIds with : " + loginIdsExportDO);
		// get division and customer info
		CDEDetailsDO cdeDetailsDO = cdeas.getCustomerDivisionEventCodesByEventCode(loginIdsExportDO.getEventCode());

		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.LOGIN_IDS_EXPORT.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, loginIdsExportDO.getUserName());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, loginIdsExportDO.getIp());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE, loginIdsExportDO.getEventCode());

			if (loginIdsExportDO.getSetId() != null) {
				Object[] params =
						{ cdeas.getAssessmentNameByAssessmentCode(loginIdsExportDO.getAssessmentCode()),
								loginIdsExportDO.getSetId(), loginIdsExportDO.getEventCode(),
								cdeDetailsDO.getDivisionName(), cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_SET_LEVEL_LOGIN_IDS_EXPORT_MSG, params);
			} else {
				Object[] params =
						{ cdeas.getAssessmentNameByAssessmentCode(loginIdsExportDO.getAssessmentCode()),
								loginIdsExportDO.getEventCode(), cdeDetailsDO.getDivisionName(),
								cdeDetailsDO.getCustomerName() };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_ASSESSMENT_LEVEL_LOGIN_IDS_EXPORT_MSG, params);
			}
		}
		logger.info("--OUT-- auditExportLoginIds");
	}

	/**
	 * gets the list of generated login ids with the status like started, not started, expired
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public GeneratedLoginIdsStatusDO getGeneratedLoginIdsStatus(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		return CandidateLoginCredentialsGeneratorService.getInstance().getGeneratedCandidateLoginCredentials(
				candidateLoginCredentialsGenerationDO.getBatchCode(),
				candidateLoginCredentialsGenerationDO.getAssessmentCode(),
				candidateLoginCredentialsGenerationDO.getSetId(),
				candidateLoginCredentialsGenerationDO.getPageNumber(),
				candidateLoginCredentialsGenerationDO.getPageCount(),
				candidateLoginCredentialsGenerationDO.getSearchData(),
				candidateLoginCredentialsGenerationDO.getSortingColumn(),
				candidateLoginCredentialsGenerationDO.getOrderType());
	}

	/**
	 * get expired login ids information
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ExpiredLoginIdsDO getExpiredLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		return CandidateLoginCredentialsGeneratorService.getInstance().getExpiredCandidateLoginCredentials(
				candidateLoginCredentialsGenerationDO.getBatchCode(),
				candidateLoginCredentialsGenerationDO.getAssessmentCode(),
				candidateLoginCredentialsGenerationDO.getSetId(),
				candidateLoginCredentialsGenerationDO.getPageNumber(),
				candidateLoginCredentialsGenerationDO.getPageCount(),
				candidateLoginCredentialsGenerationDO.getSearchData(),
				candidateLoginCredentialsGenerationDO.getSortingColumn(),
				candidateLoginCredentialsGenerationDO.getOrderType());
	}

	/**
	 * saves the candidate mandatory information in data base.
	 * 
	 * @param candidateId
	 * @param batchCode
	 * @param mifData
	 * @param ip
	 * @param stepIdentifier
	 *            TODO
	 * @return
	 * @throws GenericDataModelException
	 * @throws ParseException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean saveCandidateMandatoryInformation(int batchCandidateAssociationId, String identifier,
			String mifData, String ip, long clientTime, boolean isCompleteMif, String stepIdentifier)
			throws GenericDataModelException, ParseException {
		logger.debug("Initiated saveCandidateMandatoryInformation, BcaId={} isCompleteMif:{} stepIdentifier:{}",
				batchCandidateAssociationId, isCompleteMif, stepIdentifier);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.MIF_DATA, mifData);
		params.put("isCompleteMif", isCompleteMif);

		AcsBatchCandidateAssociation batchCandidateAssociation =
				batchCandidateAssociationService.loadBatchCandAssociation(batchCandidateAssociationId);

		MIFAuditDO mifAuditDO = new MIFAuditDO();
		mifAuditDO.setHostAddress(ip);
		mifAuditDO.setClientTime(TimeUtil.convertTimeAsString(clientTime, TimeUtil.DISPLAY_DATE_FORMAT));
		mifAuditDO.setBatchCandidateAssociation(batchCandidateAssociation.getBcaid());
		
		// if its already saved complete MIF then skip
		if(checkCompletelyMifSaved(batchCandidateAssociation.getCandidateId()))
			return true;
		
		/**
		 * sometimes TP fails to send right isCompleteMif flag(cud not reproduce),so no update candidate FN LN, Hence
		 * additional update
		 **/
		updateMifData(identifier, mifData, params);

		if (isCompleteMif) {
			logger.debug("candidate has Complete Mif, saving MIF and validating ---- candidate bcaId="
					+ batchCandidateAssociationId);
			BRulesExportEntity brulesExportEntity = this.getBRulesExportEntity(batchCandidateAssociation.getBatchCode(),
					batchCandidateAssociation.getAssessmentCode());
			logger.debug("brulesExportEntity=" + brulesExportEntity);

			if (brulesExportEntity != null) {
				MIFFieldsValidationFlags mifFieldsValidationFlags = brulesExportEntity.getMifFieldsValidationFlags();
				logger.debug("mifFieldsValidationFlags=" + mifFieldsValidationFlags);
				// validate mandatory fields on MIF
				boolean isAllowedToAttendExam = true;
				if (mifFieldsValidationFlags != null) {
					isAllowedToAttendExam =
							validateCandidateReAttemptWithMIFFieldValidations(mifData, identifier,
									mifFieldsValidationFlags, batchCandidateAssociation);
					logger.debug("isAllowedToAttendExam=" + isAllowedToAttendExam + "---- bcaId="
							+ batchCandidateAssociation.getBcaid());
				}
				if (isAllowedToAttendExam) {
					updateMifData(identifier, mifData, params);
					batchCandidateAssociation.setLastAttempted(Calendar.getInstance());
					batchCandidateAssociationService.updateBatchCandidateAssociation(batchCandidateAssociation);
					logger.debug("candidate is allowed to login---- candidate=" + identifier);
				} else {
					// candidate has already attempted the exam in those no. Of days, Re-Attempt is not allowed
					logger.debug("Returning saveCandidateMandatoryInformation, candidate has already attempted the exam in those no. Of days, Re-Attempt is not allowed, "
							+ " candidate is not allowed to login=" + identifier);
					return false;
				}
				mifAuditDO.setActionType(CandidateActionsEnum.SUBMIT_MIF);
				AuditTrailLogger.auditSave(mifAuditDO);

			}

		} else {
			logger.debug("candidate has In-Complete Mif, Hence SAVING MIF and SKIPPING MIF VALIDATION ---- candidate="
					+ identifier);
			if(checkCompletelyMifSaved(batchCandidateAssociation.getCandidateId()))
				return true;
			// audit the candidate action
			mifAuditDO.setActionType(CandidateActionsEnum.MIF_PARTIAL_SUBMIT);
			mifAuditDO.setStepIdentifier(stepIdentifier);
			AuditTrailLogger.auditSave(mifAuditDO);
			updateMifData(identifier, mifData, params);
		}

		logger.debug("Returning saveCandidateMandatoryInformation, candidate is allowed to login---- candidate="
				+ identifier);
		return true;

	}
	
	private boolean checkCompletelyMifSaved(int candidateId) throws GenericDataModelException{
		AcsCandidate acsCandidate =
				(AcsCandidate) session.get(candidateId,
						AcsCandidate.class.getCanonicalName());
		if (acsCandidate.getIsCompleteMif()) {
			logger.error("MIF for candidate with bcaID:{} already saved in database!", candidateId);
			return true;
		}
		return false;
	}

	/**
	 * update Mif Data
	 * 
	 * @param identifier
	 * @param mifData
	 * @param params
	 * @param batchCandidateAssociation
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void updateMifData(String identifier, String mifData, HashMap<String, Object> params)
			throws GenericDataModelException {
		logger.debug("Saving candidate MIF ---- candidate=" + identifier);
		MIFForm mifForm = gson.fromJson(mifData, MIFForm.class);
		if (mifForm != null) {
			PersonalInfo personalInfo = mifForm.getPersonalInfo();
			ContactInfo permanentAddress = mifForm.getPermanentAddress();
			params.putAll(getMifDetailsMap(personalInfo, permanentAddress, identifier));
			// add the object to the database or update if entry is
			// available
			String queryForCandidate =
					"UPDATE "
							+ AcsCandidate.class.getName()
							+ "  SET firstName=(:firstName), lastName=(:lastName), emailId1=(:emailId), dob=(:dob), phoneNumber1=(:mobileNo),"
							+ "permanentCity=(:permanentCity), mifData=(:mifData), isCompleteMif=(:isCompleteMif) WHERE identifier1=(:identifier1)";
			session.updateByQuery(queryForCandidate, params);

		}
		logger.debug("Candidate MIF is updated successfully ---- candidate=" + identifier);
	}

	private HashMap<String, Object> getMifDetailsMap(PersonalInfo personalInfo, ContactInfo permanentAddress,
			String identifier) {
		HashMap<String, Object> mifPrams = new HashMap<String, Object>();
		if (personalInfo != null) {
			mifPrams.put(ACSConstants.FIRST_NAME, personalInfo.getFirstName());
			mifPrams.put(ACSConstants.LAST_NAME, personalInfo.getLastName());
			if (personalInfo.getDob() != null)
				mifPrams.put(ACSConstants.DOB, TimeUtil.convertStringToCalender(personalInfo.getDob(), "yyyy-MMM-dd"));
		} else {
			mifPrams.put(ACSConstants.FIRST_NAME, ACSConstants.NOT_APPLICABLE);
			mifPrams.put(ACSConstants.LAST_NAME, ACSConstants.NOT_APPLICABLE);
		}
		if (permanentAddress != null) {
			mifPrams.put(ACSConstants.EMAIL_ID, permanentAddress.getEmailId1());
			mifPrams.put(ACSConstants.MOBILE_NO, permanentAddress.getMobile());
			mifPrams.put(ACSConstants.PERMANENT_CITY, permanentAddress.getCity());
		}
		mifPrams.put(ACSConstants.IDENTIFIER1, identifier);
		// mifPrams.put(ACSConstants.LAST_ATTEMPTED, Calendar.getInstance());
		return mifPrams;
	}

	public BRulesExportEntity getBRulesExportEntity(String batchCode, String assessmentCode)
			throws GenericDataModelException {
		logger.debug("CustomerBatchService.getBRulesExportEntity() start---");
		BRulesExportEntity bRulesExportEntity = null;
		String bRulesInformation = null;

		/**
		 * TODO :: Its a hack.Need to see why assessmentId is not set for CandidateTO for DISTRIBUTED_WALKIN.
		 */

		logger.debug("candidate.getAssessmentcODE()=" + assessmentCode);

		AcsBussinessRulesAndLayoutRules brlr =
				cdeas.getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(assessmentCode, batchCode);

		if (brlr != null && brlr.getBrRules() != null && !brlr.getBrRules().isEmpty())
			bRulesInformation = brlr.getBrRules();

		logger.debug("bRulesInformation=" + bRulesInformation);

		if (bRulesInformation != null && !bRulesInformation.isEmpty()) {
			bRulesExportEntity = new Gson().fromJson(bRulesInformation, BRulesExportEntity.class);
		}

		return bRulesExportEntity;
	}

	/**
	 * validate Candidate Re Attempt with validations for all the mif fields
	 * 
	 * @param mifData
	 * @param identifier
	 * @param mifFieldsValidationFlags
	 * @param bcaForCurrentLoggedInCandidate
	 * @return
	 * @throws GenericDataModelException
	 * @throws ParseException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean validateCandidateReAttemptWithMIFFieldValidations(String mifData, String identifier,
			MIFFieldsValidationFlags mifFieldsValidationFlags,
			AcsBatchCandidateAssociation bcaForCurrentLoggedInCandidate) throws GenericDataModelException,
			ParseException {

		boolean isNewAttempted = true;
		Gson gson = new Gson();
		if (mifData == null || mifData.isEmpty()) {
			logger.info("MIF fields are empty hence skipping MIF validations");
			return isNewAttempted;
		}

		// paramsSet is using for adding field on basis of check ON/OFF
		HashMap<String, Object> paramsSet = new HashMap<String, Object>();

		MIFForm mifForm = gson.fromJson(mifData, MIFForm.class);
		if (mifForm != null) {
			PersonalInfo personalInfo = mifForm.getPersonalInfo();
			ContactInfo permanentAddress = mifForm.getPermanentAddress();
			if (personalInfo != null && permanentAddress != null) {
				// check if candidate has already taken the exam on basis of
				// field enable true/false
				StringBuffer queryForCheckFields = new StringBuffer("FROM AcsCandidate c WHERE (1=1)");
				// creating query according to required fields check
				if (mifFieldsValidationFlags.isFirstNameRequired() && personalInfo.getFirstName() != null
						&& !personalInfo.getFirstName().isEmpty()) {
					queryForCheckFields.append(" and c.firstName=(:firstName)");
					paramsSet.put(ACSConstants.FIRST_NAME, personalInfo.getFirstName());
				}
				if (mifFieldsValidationFlags.isLastNameRequired() && personalInfo.getLastName() != null
						&& !personalInfo.getLastName().isEmpty()) {
					queryForCheckFields.append(" and c.lastName=(:lastName)");
					paramsSet.put(ACSConstants.LAST_NAME, personalInfo.getLastName());
				}
				if (mifFieldsValidationFlags.isEmailIdRequired() && permanentAddress.getEmailId1() != null
						&& !permanentAddress.getEmailId1().isEmpty()) {
					queryForCheckFields.append(" and c.emailId1=(:emailId)");
					paramsSet.put(ACSConstants.EMAIL_ID, permanentAddress.getEmailId1());
				}
				if (mifFieldsValidationFlags.isDobRequired() && personalInfo.getDob() != null
						&& !personalInfo.getDob().isEmpty()) {
					queryForCheckFields.append(" and c.dob=(:dob)");
					paramsSet.put(ACSConstants.DOB,
							TimeUtil.convertStringToCalender(personalInfo.getDob(), "yyyy-MMM-dd"));
				}
				if (mifFieldsValidationFlags.isMobileNoRequired() && permanentAddress.getMobile() != null
						&& !permanentAddress.getMobile().isEmpty()) {
					queryForCheckFields.append(" and c.phoneNumber1=(:mobileNo)");
					paramsSet.put(ACSConstants.MOBILE_NO, permanentAddress.getMobile());
				}
				if (mifFieldsValidationFlags.isPermanentCityRequired() && permanentAddress.getCity() != null
						&& !permanentAddress.getCity().isEmpty()) {
					queryForCheckFields.append(" and c.permanentCity=(:permanentCity)");
					paramsSet.put(ACSConstants.PERMANENT_CITY, permanentAddress.getCity());
				}

				logger.debug("making query on basis of enable fields is " + queryForCheckFields.toString()
						+ " ---paramsSet=" + paramsSet.values().toString());
				List<AcsBatchCandidateAssociation> batchCandAssoList = null;
				List<AcsCandidate> candidateTOList = null;
				try {
					candidateTOList = session.getListByQuery(queryForCheckFields.toString(), paramsSet);
					if (candidateTOList == null || candidateTOList.isEmpty()) {
						logger.info("CustomerBatchService.validateCandidateReAttempt() => logging for first time for Identifier = "
								+ identifier);
					} else {
						logger.info("CustomerBatchService.validateCandidateReAttempt() => candidate record found, for Identifier = "
								+ identifier);
						List<Integer> candidateIdList = new ArrayList<>();
						for (AcsCandidate candidate : candidateTOList) {
							candidateIdList.add(candidate.getCandidateId());
						}
						// Integer[] candidateIds = candidateIdList.toArray(new Integer[candidateIdList.size()]);
						HashMap<String, Object> paramsForBca = new HashMap<String, Object>();
						paramsForBca.put("assessmentCode", bcaForCurrentLoggedInCandidate.getAssessmentCode());
						paramsForBca.put("candidateId", candidateIdList);
						batchCandAssoList =
								session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_FOR_ASSESSMENT_CANDIDATE_ID,
										paramsForBca);
					}
				} catch (GenericDataModelException e) {
					logger.error("CustomerBatchService.validateCandidateReAttempt() inside GenericDataModelException=",
							e);
					throw e;
				}

				if (batchCandAssoList == null || batchCandAssoList.isEmpty()) {
					logger.info("CustomerBatchService.validateCandidateReAttempt() => logging for first time for Identifier = "
							+ identifier);
				} else {
					logger.info("CustomerBatchService.validateCandidateReAttempt() => "
							+ " candidate record found for same assessment and taken Exam Before, "
							+ " Hence calculating no. of days since he has logged-in....., for Identifier = "
							+ identifier);
					for (AcsBatchCandidateAssociation batchCandAsso : batchCandAssoList) {
						isNewAttempted = validateCandidateReAttempt(mifFieldsValidationFlags, batchCandAsso);
						if (!isNewAttempted)
							return isNewAttempted;
					}

				}

			}
		} else
			logger.info("MIF fields are empty hence skipping validations");
		logger.info(
				"Returning CustomerBatchService.validateCandidateReAttempt() => Identifier = {} and is Allowed Login = {} ",
				identifier, isNewAttempted);
		return isNewAttempted;
	}

	/**
	 * validate Candidate ReAttempt no. of days
	 * 
	 * @param mifData
	 * @param identifier
	 * @param mifFieldsValidationFlags
	 * @param batchCandidateAssociation
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean validateCandidateReAttempt(MIFFieldsValidationFlags mifFieldsValidationFlags,
			AcsBatchCandidateAssociation batchCandidateAssociation) throws GenericDataModelException {
		boolean isAllowedToAttend = true;

		int noOfDaysFromMif = mifFieldsValidationFlags.getNoOfDaysCandReAttemptTest();

		Calendar lastAttempted = batchCandidateAssociation.getLastAttempted();
		int days = 0;
		if (lastAttempted != null) {
			long timeDiff = Calendar.getInstance().getTimeInMillis() - lastAttempted.getTimeInMillis();
			if (timeDiff > 0)
				days = (int) (timeDiff / (1000 * 60 * 60 * 24));
		} else {
			logger.debug("lastAttempted is null, Attempting 1st time for bcaid = "
					+ batchCandidateAssociation.getBcaid());
			return isAllowedToAttend;
		}
		logger.debug("bcaId=" + batchCandidateAssociation.getBcaid() + "   -----days=" + days);
		// 2 < 15 //20 < 15
		if (days < noOfDaysFromMif) {
			isAllowedToAttend = false;
			logger.debug("lastAttempted days={} is less than noOfDaysSpecified={}, So NOT allowed to attempt exam",
					days, noOfDaysFromMif);
		} else
			logger.debug("lastAttempted days={} is more than noOfDaysSpecified={}, So allow to attempt exam", days,
					noOfDaysFromMif);

		return isAllowedToAttend;
	}

	/**
	 * gets the customer, division and events information related to the specified event codes.
	 * 
	 * @param eventCodes
	 * @throws GenericDataModelException
	 * @throws UserRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Deprecated
	public Map<String, CustomerDO> getCustomerDivisionAndEventInformationByCEMResponse(String cemResponse)
			throws GenericDataModelException, UserRejectedException {
		Type type = new TypeToken<List<String>>() {
		}.getType();
		List<String> eventCodes = new Gson().fromJson(cemResponse, type);

		Map<String, CustomerDO> customerDOs = null;

		List<CDEDetailsDO> cdeDetailsDOs = cdeas.getCustomerDivisionAndEventDetailsByEventCodes(eventCodes);
		if (cdeDetailsDOs != null && !cdeDetailsDOs.isEmpty()) {
			customerDOs = cdeas.parseCDEDetails(cdeDetailsDOs);
		} else {
			logger.error("No events exist with the specified codes...", eventCodes);
			throw new UserRejectedException(ACSExceptionConstants.USER_REJECTED_AS_EVENT_INFO_NOT_AVAILABLE,
					"User Login not allowed. No events exist with the specified codes");
		}

		return customerDOs;
	}

	public List<UnUsedLoginCredentialsDO> getActiveLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException {
		return CandidateLoginCredentialsGeneratorService.getInstance().getActiveCandidateLoginCredentials(
				candidateLoginCredentialsGenerationDO.getBatchCode(),
				candidateLoginCredentialsGenerationDO.getAssessmentCode(),
				candidateLoginCredentialsGenerationDO.getSetId(),
				candidateLoginCredentialsGenerationDO.getPageNumber(),
				candidateLoginCredentialsGenerationDO.getPageCount(),
				candidateLoginCredentialsGenerationDO.getSearchData(),
				candidateLoginCredentialsGenerationDO.getSortingColumn(),
				candidateLoginCredentialsGenerationDO.getOrderType());
	}

	/**
	 * gets the list assessment related information along with generated login id statistics for open event.
	 * 
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<LoginIdGenerationDashboardDO> getOpenBatchAssessmentsAndStatistics(String eventCode)
			throws GenericDataModelException {
		logger.info("initiated getOpenBatchAssessmentsStatistics where input = {}", eventCode);

		List<LoginIdGenerationDashboardDO> loginIdGenerationDashboardDOs =
				cdeas.getAssessmentInformationOfOpenEventByEventCode(eventCode);
		logger.info("list of loginIdGenerationDashboardDOs = {}", loginIdGenerationDashboardDOs);

		if (loginIdGenerationDashboardDOs != null && !loginIdGenerationDashboardDOs.isEmpty()) {
			for (Iterator iterator = loginIdGenerationDashboardDOs.iterator(); iterator.hasNext();) {
				LoginIdGenerationDashboardDO loginIdGenerationDashboardDO =
						(LoginIdGenerationDashboardDO) iterator.next();
				// fetch candidate status statistics
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put(ACSConstants.BATCH_CODE, loginIdGenerationDashboardDO.getBatchCode());
				params.put("assessmentCode", loginIdGenerationDashboardDO.getAssessmentCode());

				String query =
						"select count(*) as count,c.currentstatus as candidateTestStatus from acscandidatestatus c , "
								+ " acsbatchcandidateassociation bca where  bca.bcaid=c.csid  and bca.batchcode=(:batchCode) and "
								+ " bca.assessmentcode=(:assessmentCode) group by c.currentstatus";
				if (loginIdGenerationDashboardDO.getSetId() != null
						&& !loginIdGenerationDashboardDO.getSetId().isEmpty()
						&& !loginIdGenerationDashboardDO.getSetId().equals(ACSConstants.ALL_ASSESSMENTS)) {
					params.put("setId", loginIdGenerationDashboardDO.getSetId());
					query =
							"select count(*) as count,c.currentstatus as candidateTestStatus from acscandidatestatus c , "
									+ " acsbatchcandidateassociation bca where  bca.bcaid=c.csid  and bca.batchcode=(:batchCode) and "
									+ " bca.assessmentcode=(:assessmentCode) and bca.setid=(:setId) group by c.currentstatus";
				}

				List<LoginIdStatusStatisticsDO> loginIdStatusStatisticsDOs =
						(List<LoginIdStatusStatisticsDO>) session.getResultListByNativeSQLQuery(query, params,
								LoginIdStatusStatisticsDO.class);

				for (Iterator loginIdStatusStatisticsIterator = loginIdStatusStatisticsDOs.iterator(); loginIdStatusStatisticsIterator
						.hasNext();) {
					LoginIdStatusStatisticsDO loginIdStatusStatisticsDO =
							(LoginIdStatusStatisticsDO) loginIdStatusStatisticsIterator.next();

					switch (loginIdStatusStatisticsDO.getCandidateTestStatus()) {
						case ENDED:
						case TEST_COMPLETED:
							loginIdGenerationDashboardDO.setCompletedCount(loginIdGenerationDashboardDO
									.getCompletedCount() + loginIdStatusStatisticsDO.getCount());
							break;
						case IDLE:
						case IN_PROGRESS:
						case NOT_YET_STARTED:
							loginIdGenerationDashboardDO.setStartedCount(loginIdGenerationDashboardDO.getStartedCount()
									+ loginIdStatusStatisticsDO.getCount());
							break;

						default:
							logger.info("Ignoring the status = {}", loginIdStatusStatisticsDO.getCandidateTestStatus());
							break;
					}
				}

				loginIdGenerationDashboardDO.setNotStartedCount((loginIdGenerationDashboardDO
						.getGeneratedLoginIdCount())
						- (loginIdGenerationDashboardDO.getStartedCount() + loginIdGenerationDashboardDO
								.getCompletedCount()));

				// remaining is nothing but the difference of max and generated
				// loginIds.
				loginIdGenerationDashboardDO.setRemainingLoginIdCount(loginIdGenerationDashboardDO
						.getMaxNumberOfAllowedLoginIds() - loginIdGenerationDashboardDO.getGeneratedLoginIdCount());
				HashMap<String, Object> packParams = new HashMap<String, Object>();
				packParams.put("batchCode", loginIdGenerationDashboardDO.getBatchCode());
				packParams.put("packstatus", PacksStatusEnum.ACTIVATED.toString());
				String packCountQuery =
						"select pack.packtype as packType, count(*) as packCount from acspackdetails pack where pack.packidentifier in "
								+ " (select assn.packidentifier from acspackdetailsbatchassociation assn where assn.batchcode=(:batchCode))"
								+ "and pack.packstatus=(:packstatus) group by pack.packtype";

				List<PackStatistics> resultList =
						session.getResultListByNativeSQLQuery(packCountQuery, packParams, PackStatistics.class);

				if (resultList == null || resultList.isEmpty() || resultList.size() < 3) {
					loginIdGenerationDashboardDO.setIDGenerationAllowed(false);

				} else {
					boolean isBpack = false, isApack = false, isQpack = false;
					for (PackStatistics array : resultList) {
						switch (array.getPackType()) {
							case 0:
								isQpack = (array.getPackCount() > 0 ? true : false);
								break;
							case 1:
								isApack = (array.getPackCount() > 0 ? true : false);
								break;
							case 2:
								isBpack = (array.getPackCount() > 0 ? true : false);
								break;
						}
					}
					loginIdGenerationDashboardDO.setIDGenerationAllowed(isQpack && isApack && isBpack);
				}
			}

			logger.info("returning loginIdGenerationDashboardDOs={}", loginIdGenerationDashboardDOs);
			return loginIdGenerationDashboardDOs;
		} else {
			logger.info("specified event is not open event");
			return null;
		}
	}

	/**
	 * getCandLoginIdsbyBatchId API used to get the Candidate login id's for given BatchId
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<CandidateInfoDO> getCandLoginIdsbyBatchIdAndDates(String batchCode, String eventType,
			Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info("specified input : batchCode = {}", batchCode);
		if (batchCode == null) {
			logger.info("skiping getCandLoginIdsbyBatchId API since batchIds are found null");
			return null;
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		List<CandidateInfoDO> candidateInfo = null;
		params.put("batchCode", batchCode);
		if (eventType.equals(EventType.DISTRIBUTED_WALKIN.name())) {
			candidateInfo =
					session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_CAND_LOGIN_IDS_BY_BATCHID, params, 0,
							CandidateInfoDO.class);
		} else if (eventType.equals(EventType.CENTRALIZED_WALKIN.name())) {
			params.put("startDate", startDate);
			params.put("endDate", endDate);
			candidateInfo =
					session.getResultListByNativeSQLQuery(
							ACSQueryConstants.QUERY_FETCH_CAND_LOGIN_IDS_BY_BATCH_AND_DATE, params,
							CandidateInfoDO.class);
		}
		if (candidateInfo.equals(Collections.<Object> emptyList()) || candidateInfo == null)
			return null;
		return candidateInfo;
	}

	/**
	 * generates Rpack for selected candidates
	 * 
	 * @param candidateIds
	 * @param batchCode
	 * @throws Exception
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean generateRpackForCandidate(Integer candidateId, String identifier, String batchCode,
			String eventCode, String userName, String ipAddress) throws Exception {
		try {
			AcsBatchCandidateAssociation batchCandidateAssociation =
					batchCandidateAssociationService.getBatchCandAssociationByBatchIdAndCandId(batchCode, candidateId);
			// generates the rpack for the specified candidates
			RPackGenerator.generateRpack(batchCode, batchCandidateAssociation.getBcaid());

			// check whether admin audit is enable or not if enabled audit it
			if (isAuditEnable != null && isAuditEnable.equals("true")) {
				HashMap<String, String> logbackParams = new HashMap<String, String>();
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
						AdminAuditActionEnum.CANDIDATE_LEVEL_RPACK_GENERATION.toString());
				logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_EVENT_CODE, eventCode);
				Object[] aparams = { identifier, eventCode };
				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_ADMIN_CAND_LEVEL_RPACK_GENERATION_MSG, aparams);
			}
			return true;
		} catch (Exception e) {
			throw new Exception("Exception occured while generating rpack : " + e.getMessage());
		}
	}

	/**
	 * gets the mif data based on the specified candidate id and batch id
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public MIFDataDO getMifData(int candidateId) throws GenericDataModelException {
		AcsCandidate acsCandidate = (AcsCandidate) session.get(candidateId, AcsCandidate.class.getCanonicalName());
		if (acsCandidate != null && acsCandidate.getMifData() != null) {
			MIFForm mifForm = gson.fromJson(acsCandidate.getMifData(), MIFForm.class);

			MIFDataDO mifDataDO = new MIFDataDO();
			mifDataDO.setDob(mifForm.getPersonalInfo().getDob());
			mifDataDO.setEmailId1(mifForm.getPermanentAddress().getEmailId1());
			mifDataDO.setEmailId2(mifForm.getPermanentAddress().getEmailId2());
			mifDataDO.setFirstName(mifForm.getPersonalInfo().getFirstName());
			mifDataDO.setLastName(mifForm.getPersonalInfo().getLastName());
			mifDataDO.setMiddleName(mifForm.getPersonalInfo().getMiddleName());
			mifDataDO.setMobile(mifForm.getPermanentAddress().getMobile());
			mifDataDO.setStdCodeLandline(mifForm.getPermanentAddress().getStdCodeLandline());

			return mifDataDO;
		} else {
			logger.info("candidateStatusTO is null");
			return null;
		}
	}

	/**
	 * generates live dash board related data for the current batch if exists.
	 * 
	 * @return list of {@link LiveDashboardDetailsDO}
	 * @throws GenericDataModelException
	 */
	// From Corporate During code merge
	public LiveDashboardStatsDO getCandidateStatusData(String batchCode, String assesmentCode, String setId,
			Integer status, int pageNumber, int pageCount, String searchData, String sortingColumn, String orderType)
			throws GenericDataModelException {
		logger.info("current batchId = {}", batchCode);
		if (batchCode == null)
			return null;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("assesmentCode", assesmentCode);
		params.put("questionType", QuestionType.READING_COMPREHENSION.toString());

		String query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BA_ID;
		String queryCount = ACSQueryConstants.QUERY_FETCH_COUNT_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BA_ID;
		if (setId != null && !setId.isEmpty() && !setId.equals(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);

			query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BAS_ID;
			queryCount = ACSQueryConstants.QUERY_FETCH_COUNT_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BAS_ID;
		}

		List<Integer> candStatus = new ArrayList<Integer>();
		if (status == 1) {
			candStatus.add(CandidateTestStatusEnum.NOT_YET_STARTED.ordinal());
			candStatus.add(CandidateTestStatusEnum.IN_PROGRESS.ordinal());
			candStatus.add(CandidateTestStatusEnum.IDLE.ordinal());
		} else {
			candStatus.add(CandidateTestStatusEnum.ENDED.ordinal());
			candStatus.add(CandidateTestStatusEnum.TEST_COMPLETED.ordinal());
		}
		params.put("candStatus", candStatus);

		if (searchData != null && !searchData.isEmpty()) {
			String sysQuery = "";

			int eLength = HBMStatusEnum.values().length;
			for (int x = 0; x < eLength; x++) {
				if (HBMStatusEnum.values()[x].toString().toLowerCase().contains(searchData.toString().toLowerCase())) {
					sysQuery =
							sysQuery + " or cs.playerStatus =" + x + " or cs.peripheralStatus =" + x
									+ " or cs.systemStatus=" + x;
				}
			}

			int eLengthCS = CandidateTestStatusEnum.values().length;
			for (int y = 0; y < eLengthCS; y++) {
				if (CandidateTestStatusEnum.values()[y].toString().toLowerCase()
						.contains(searchData.replaceAll("_", " ").toLowerCase())) {
					sysQuery = sysQuery + " or cs.currentStatus =" + y;
				}
			}

			query =
					query + " and (c.firstName like '%" + searchData + "%' or bca.hostAddress like '%" + searchData
							+ "%' or c.identifier1 like '%" + searchData + "%' or bca.assessmentCode like '%"
							+ searchData + "%' or bca.loginTime like '%" + searchData
							+ "%' or bca.actualTestStartedTime like '%" + searchData
							+ "%' or bca.actualTestEndTime like '%" + searchData + "%' or bca.candidateLogin like '%"
							+ searchData + "%'" + sysQuery + ")";
			queryCount =
					queryCount + " and (c.firstName like '%" + searchData + "%' or bca.hostAddress like '%"
							+ searchData + "%' or c.identifier1 like '%" + searchData
							+ "%' or bca.assessmentCode like '%" + searchData + "%' or bca.loginTime like '%"
							+ searchData + "%' or bca.actualTestStartedTime like '%" + searchData
							+ "%' or bca.actualTestEndTime like '%" + searchData + "%' or bca.candidateLogin like '%"
							+ searchData + "%'" + sysQuery + ")";

		}

		List liveDashboardDetails1 = session.getResultListByNativeSQLQuery(queryCount, params);
		int countN = Integer.parseInt(liveDashboardDetails1.get(0).toString());

		if (sortingColumn != null && orderType != null) {
			if (sortingColumn.equals("loginId")) {
				sortingColumn = "candidateLogin";
			}
			query = query + " order by " + sortingColumn + " " + orderType;
		}
		/*
		 * List obj = session.getResultAsListByQuery(queryCount, params, 0); int countN =
		 * Integer.parseInt(obj.get(0).toString());
		 */
		// int startCount = pageNumber * pageCount;

		params.put("startCount", pageNumber);
		params.put("endCount", pageCount);
		query = query + " limit :startCount, :endCount";

		logger.debug("Query to be executed:{}", query);

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(query, params, LiveDashboardDetailsDO.class);
		// logger.info("liveDashboardDetails = {}", liveDashboardDetails);
		if (liveDashboardDetails.equals(Collections.<Object> emptyList()))
			return null;

		Map<String, AcsAssessment> assessments = new HashMap<String, AcsAssessment>();

		// check whether ACS need to add candidate lost time automatically or
		// not
		boolean isLostTimeAddedAutomatically = isLostTimeAddedAutomatically();
		logger.info("isLostTimeAddedAutomatically falg = {} for batch with id = {}", isLostTimeAddedAutomatically,
				batchCode);

		int rpackGenerationFrequency = acsPropertyUtil.getRpackGenerationFrequency();
		int summaryPageTimeout = acsPropertyUtil.getSummaryPageDefaultTimeout();

		for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
			// check whether the automatic rpack generation time is elapsed or
			// not
			if (!liveDashboardDetailsDO.isRpackGenarated() && liveDashboardDetailsDO.getTestEndTime() != null) {
				Calendar rpackGenTime = (Calendar) liveDashboardDetailsDO.getTestEndTime().clone();
				rpackGenTime
						.add(Calendar.MINUTE, (summaryPageTimeout + rpackGenerationFrequency + (liveDashboardDetailsDO
								.getFeedbackDuration() != null ? (liveDashboardDetailsDO.getFeedbackDuration()
								.intValue() / 60) : 0)));

				if (Calendar.getInstance().after(rpackGenTime)) {
					liveDashboardDetailsDO.setEnableRpackGeneration(true);
				}
			}

			int candidateId = liveDashboardDetailsDO.getCandidateId();
			params.clear();
			params.put("candidateId", candidateId);
			params.put("batchCode", batchCode);

			AcsAssessment assessmentDetails = null;
			String assessmentCode = cdeas.getAssessmentIdbyCandIDAndBatchID(candidateId, batchCode);
			if (assessmentCode != null && !assessments.containsKey(assessmentCode)) {

				assessmentDetails = cdeas.getAssessmentDetailsByAssessmentCode(assessmentCode);
				assessments.put(assessmentCode, assessmentDetails);
			}
			liveDashboardDetailsDO.setAssessmentName(assessments.get(assessmentCode).getAssessmentName());

			// fetch batch candidate association to fetch candidate max and
			// current login attempts
			AcsBatchCandidateAssociation batchCandidateAssociationTO =
					batchCandidateAssociationService.getBatchCandAssociationByBatchIdCandIdAndAssessmentId(batchCode,
							candidateId, assessmentCode);
			if (batchCandidateAssociationTO != null) {
				liveDashboardDetailsDO.setLoginId(batchCandidateAssociationTO.getCandidateLogin());
			}

			// calculate remaining time for the candidate
			long remainingTime;
			if (liveDashboardDetailsDO.getActualSectionStartedTime() == null) {
				remainingTime = 0;
				if (liveDashboardDetailsDO.getTestEndTime() == null
						&& liveDashboardDetailsDO.getTestStartTime() != null
						&& liveDashboardDetailsDO.getLastHeartBeatTime() != null) {
					Calendar currentTime = Calendar.getInstance();
					long lastHeartBeatTimeInMillis = liveDashboardDetailsDO.getLastHeartBeatTime().getTimeInMillis();
					long currentTimeInMillis = currentTime.getTimeInMillis();

					int hbmTimeInterval = acsPropertyUtil.getHeartBeatTimeInterval();
					logger.info("configured heart beat time interval = {}", hbmTimeInterval);

					long inActiveTime = (currentTimeInMillis - lastHeartBeatTimeInMillis);
					logger.info("inActive time for heart beat = {}", inActiveTime);

					if (inActiveTime > hbmTimeInterval) {
						currentTime.setTime(liveDashboardDetailsDO.getLastHeartBeatTime().getTime());
						logger.info(
								"heart beat is elapsed according to the configured value hence, considering the candidate with identifier = {} as crashed and considering lastHeartBeatTime = {} as current time",
								liveDashboardDetailsDO.getIdentificationNumber(), liveDashboardDetailsDO
										.getLastHeartBeatTime().getTime());
					} else {
						logger.info(
								"heart beat is according to the configured value hence, not considering the candidate with identifier = {} as crashed and considering current time = {} as it is",
								liveDashboardDetailsDO.getIdentificationNumber(), currentTime.getTime());
					}

					// calculate remaining time and check whether it is non negative
					// if not trim it based on the batch end
					// time else send 0
					remainingTime =
							(liveDashboardDetailsDO.getAllotedDuration().longValue() - ((currentTime.getTimeInMillis() - liveDashboardDetailsDO
									.getTestStartTime().getTimeInMillis()) / 1000));
					logger.info("spent time = {} for candidate with id = {} in batch with id = {}", remainingTime,
							candidateId, batchCode);
					if (isLostTimeAddedAutomatically) {
						remainingTime = remainingTime + liveDashboardDetailsDO.getPrevLostTime().longValue();
						logger.info(
								"automatically adding the lost time = {} for candidate with id = {} in batch with id = {}",
								liveDashboardDetailsDO.getPrevLostTime(), candidateId, batchCode);
					} else {
						logger.info("automatic addition of lost time is disabled for batch with id = {} ", batchCode);
					}
					// add the previous successful extended time's for the candidate
					// if any
					logger.info(
							"adding previous successful extension time for candidate with id = {} in batch with id = {}",
							liveDashboardDetailsDO.getTotalExtendedExamDuration(), candidateId, batchCode);
					remainingTime = remainingTime + (liveDashboardDetailsDO.getTotalExtendedExamDuration().longValue());

					if (remainingTime > 0) {
						logger.info("remaining time is greater than 0 hence, initiating trimming the duration based on batch end time");
						Calendar maxDeltaRpackGenTime = liveDashboardDetailsDO.getBatchEndTime();
						if (liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS() != null
								&& liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS()
										.after(maxDeltaRpackGenTime)) {
							maxDeltaRpackGenTime = liveDashboardDetailsDO.getDeltaRpackGenarationTimeByACS();
						}

						// trim the remaining time based on the batch end time
						remainingTime = trimDurationOnBatchEndTime(currentTime, remainingTime, maxDeltaRpackGenTime);
					} else {
						remainingTime = 0;
					}
					logger.info("remaining time = {} for candidate with id = {}", remainingTime, candidateId);
				}
				liveDashboardDetailsDO.setRemainingTime(remainingTime);
			} else {
				liveDashboardDetailsDO.setRemainingTime(ACSConstants.NEGATIVE_VALUE);
			}
		}

		LiveDashboardStatsDO liveDashboardStatsDO = new LiveDashboardStatsDO(liveDashboardDetails, countN);
		return liveDashboardStatsDO;
	}

	/**
	 * 
	 * @param batchCode
	 * @param assesmentCode
	 * @param setId
	 * @param status
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	// From Corporate During code merge
	public LiveDashboardStatsDO getCandidateStatusStatistics(String batchCode, String assesmentCode, String setId,
			Integer status) throws GenericDataModelException {
		logger.info("current batchId = {}", batchCode);
		if (batchCode == null || batchCode.isEmpty()) {
			logger.info("batchId = {}, which is invalid", batchCode);
			return null;
		}

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("assesmentCode", assesmentCode);
		params.put("questionType", QuestionType.READING_COMPREHENSION.toString());

		String query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BA_ID;

		if (setId != null && !setId.isEmpty() && !setId.equals(ACSConstants.ALL_ASSESSMENTS)) {
			params.put("setId", setId);

			query = ACSQueryConstants.QUERY_FETCH_CAND_DETAILS_FOR_LIVE_DASHBOARD_BY_BAS_ID;
		}
		List<Integer> candStatus = new ArrayList<Integer>();
		if (status == 1) {
			candStatus.add(CandidateTestStatusEnum.NOT_YET_STARTED.ordinal());
			candStatus.add(CandidateTestStatusEnum.IN_PROGRESS.ordinal());
			candStatus.add(CandidateTestStatusEnum.IDLE.ordinal());
		} else {
			candStatus.add(CandidateTestStatusEnum.ENDED.ordinal());
			candStatus.add(CandidateTestStatusEnum.TEST_COMPLETED.ordinal());
		}
		params.put("candStatus", candStatus);

		List<LiveDashboardDetailsDO> liveDashboardDetails =
				session.getResultListByNativeSQLQuery(query, params, LiveDashboardDetailsDO.class);
		// logger.info("liveDashboardDetails = {}", liveDashboardDetails);
		if (liveDashboardDetails.equals(Collections.<Object> emptyList()))
			return null;

		int candidateIdleCount = 0;
		int candidateInProgressCount = 0;
		int candidateEndedCount = 0;
		int candidateNotStartedCount = 0;

		int playerInActiveCount = 0;
		int systemNotRespondingCount = 0;
		int peripheralsDetectedCount = 0;

		Calendar firstStartTime = null;
		Calendar lastStartTime = null;
		Calendar firstEndTime = null;
		Calendar lastEndTime = null;

		for (LiveDashboardDetailsDO liveDashboardDetailsDO : liveDashboardDetails) {
			switch (liveDashboardDetailsDO.getTestStatus()) {
				case IDLE:
					candidateIdleCount++;
					break;
				case IN_PROGRESS:
					candidateInProgressCount++;
					break;
				case ENDED:
					candidateEndedCount++;
					break;
				case NOT_YET_STARTED:
					candidateNotStartedCount++;
					break;

				default:
					logger.info("No such status type = " + liveDashboardDetailsDO.getTestStatus());
					break;
			}
			if (liveDashboardDetailsDO.getPlayerStatus().equals(HBMStatusEnum.RED)) {
				playerInActiveCount++;
			}
			if (liveDashboardDetailsDO.getSystemStatus().equals(HBMStatusEnum.RED)) {
				systemNotRespondingCount++;
			}
			if (liveDashboardDetailsDO.getPeripheralStatus().equals(HBMStatusEnum.RED)) {
				peripheralsDetectedCount++;
			}

			// calculate first start time and last start time
			if (liveDashboardDetailsDO.getTestStartTime() != null) {
				if (firstStartTime == null || firstStartTime.after(liveDashboardDetailsDO.getTestStartTime()))
					firstStartTime = liveDashboardDetailsDO.getTestStartTime();
				if (lastStartTime == null || lastStartTime.before(liveDashboardDetailsDO.getTestStartTime()))
					lastStartTime = liveDashboardDetailsDO.getTestStartTime();
			}

			// calculate first end time and last end time
			if (liveDashboardDetailsDO.getTestEndTime() != null) {
				if (firstEndTime == null || firstEndTime.after(liveDashboardDetailsDO.getTestEndTime()))
					firstEndTime = liveDashboardDetailsDO.getTestEndTime();
				if (lastEndTime == null || lastEndTime.before(liveDashboardDetailsDO.getTestEndTime()))
					lastEndTime = liveDashboardDetailsDO.getTestEndTime();
			}
		}
		LiveDashboardStatsDO liveDashboardStatsDO =
				new LiveDashboardStatsDO(candidateIdleCount, candidateInProgressCount, candidateEndedCount,
						candidateNotStartedCount, playerInActiveCount, systemNotRespondingCount,
						peripheralsDetectedCount, null);
		liveDashboardStatsDO.setFirstEndTime(firstEndTime);
		liveDashboardStatsDO.setLastEndTime(lastEndTime);
		liveDashboardStatsDO.setFirstStartTime(firstStartTime);
		liveDashboardStatsDO.setLastStartTime(lastStartTime);
		return liveDashboardStatsDO;
	}

	/**
	 * @param batchCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchCandidateDO> getAttendanceGridBatchWise(String[] batchCodes) throws GenericDataModelException {
		return batchCandidateAssociationService.getAttendanceGridBatchWise(batchCodes);
	}

	/**
	 * gets the partially saved mif data based on the specified candidate id and batch id
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public MIFForm getPartialMifData(String batchCode, int candidateId) throws GenericDataModelException {
		AcsCandidate acsCandidate = (AcsCandidate) session.get(candidateId, AcsCandidate.class.getCanonicalName());
		// AcsCandidateStatus candidateStatusTO =
		// candidateService.getCandidateStatus(batchCandidateAssociation.getBcaid());
		MIFForm mifForm = null;
		if (acsCandidate != null && acsCandidate.getMifData() != null)
			mifForm = gson.fromJson(acsCandidate.getMifData(), MIFForm.class);
		else if (acsCandidate != null && acsCandidate.getMifData() == null) {
			mifForm = new MIFForm();
			if (acsCandidate != null) {
				PersonalInfo personalInfo = new PersonalInfo();
				ContactInfo communicationAddress = new ContactInfo();
				if (acsCandidate.getDob() != null)
					personalInfo.setDob(TimeUtil.convertTimeAsString(acsCandidate.getDob().getTimeInMillis(),
							"yyyy-MM-dd"));
				personalInfo.setFirstName(acsCandidate.getFirstName());
				personalInfo.setGender(acsCandidate.getGender());
				personalInfo.setLastName(acsCandidate.getLastName());
				// personalInfo.setUniqueIdNumber(acsCandidate.getIdentifier1());
				communicationAddress.setEmailId1(acsCandidate.getEmailId1());
				communicationAddress.setEmailId2(acsCandidate.getEmailId2());
				communicationAddress.setMobile(acsCandidate.getPhoneNumber1());
				mifForm.setPersonalInfo(personalInfo);
				mifForm.setCommunicationAddress(communicationAddress);
			}
		}
		logger.info("getPartialMifData returning = {}", mifForm);
		return mifForm;
	}

	/**
	 * generate excel with specified password
	 * 
	 * @param candidateLoginCredentialsGenerationDO
	 * @return
	 * @throws GenericDataModelException
	 * @throws IOException
	 * @throws InvalidFormatException
	 * @throws GeneralSecurityException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String generateExcelWithGeneratedLoginIds(
			CandidateLoginCredentialsGenerationDO candidateLoginCredentialsGenerationDO)
			throws GenericDataModelException, IOException, InvalidFormatException, GeneralSecurityException {
		logger.debug("--IN-- generateExcelWithGeneratedLoginIds with : " + candidateLoginCredentialsGenerationDO);
		String downloadURL = null;

		if (candidateLoginCredentialsGenerationDO == null || candidateLoginCredentialsGenerationDO.getStatus() == null
				|| !candidateLoginCredentialsGenerationDO.getStatus().equalsIgnoreCase(ACSConstants.MSG_SUCCESS)) {
			return downloadURL;
		}
		// get CDE details
		CDEInfoDO cdeInfoDO =
				cdeas.getCustomerDivisionEventInfoByBatchCode(candidateLoginCredentialsGenerationDO.getBatchCode());
		List<String> headerValues = getHeaderValuesForExcel();
		List<List<Object>> rowValues = new ArrayList<List<Object>>();
		// Taking values from getGeneratedLoginIds List and setting it
		for (CandidateLoginCredentialsDO candidateLoginCredentialsDO : candidateLoginCredentialsGenerationDO
				.getCandidateLoginCredentialsDOs()) {
			List<Object> cellValues = new ArrayList<Object>();
			cellValues.add(cdeInfoDO.getCustomerName());
			cellValues.add(cdeInfoDO.getDivisionName());
			cellValues.add(cdeInfoDO.getEventName());
			cellValues
					.add(candidateLoginCredentialsGenerationDO.getAssessmentName() == null ? candidateLoginCredentialsGenerationDO
							.getAssessmentCode() : candidateLoginCredentialsGenerationDO.getAssessmentName());
			cellValues.add(candidateLoginCredentialsGenerationDO.getSetId());
			cellValues.add(candidateLoginCredentialsDO.getLoginId());
			cellValues.add(candidateLoginCredentialsDO.getPassword());
			cellValues.add(candidateLoginCredentialsDO.getExpiryDateTime());
			if (TimeUtil.convertStringToCalender(candidateLoginCredentialsGenerationDO.getExpiryTime()).before(
					Calendar.getInstance()))
				cellValues.add(ACSConstants.EXPIRED.toUpperCase());
			else
				cellValues.add(ACSConstants.ACTIVE_PROFILE.toUpperCase());
			rowValues.add(cellValues);
		}

		downloadURL =
				writeExcelForLoginIds(candidateLoginCredentialsGenerationDO.getPasswordForExcel(), downloadURL,
						headerValues, rowValues);
		logger.debug("--OUT-- returning generateExcelWithGeneratedLoginIds with : " + downloadURL);
		return downloadURL;
	}

	/**
	 * write Excel For Login Ids
	 * 
	 * @param passwordForExcel
	 * @param downloadURL
	 * @param headerValues
	 * @param rowValues
	 * @return
	 * @throws IOException
	 * @throws GeneralSecurityException
	 * @throws InvalidFormatException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String writeExcelForLoginIds(String passwordForExcel, String downloadURL, List<String> headerValues,
			List<List<Object>> rowValues) throws IOException, GeneralSecurityException, InvalidFormatException {
		String fileRelativePath =
				File.separator + ACSConstants.LOGIN_IDS_EXPORT + File.separator + ACSConstants.LOGIN_IDS
						+ Calendar.getInstance().getTimeInMillis() + ACSConstants.XLSX_EXTENSION;
		String filePath = ACSFilePaths.getACSDownloadDirectory() + fileRelativePath;

		ExcelWriterUtility excelWriterUtility = new ExcelWriterUtility();
		excelWriterUtility.createExcel(filePath, headerValues, rowValues, passwordForExcel);

		File file = new File(filePath);
		if (file.exists()) {
			downloadURL = acsCommonUtility.getDownloadURL(fileRelativePath);
		} else {
			logger.info("No such file exist = {}", file.getAbsoluteFile());
			// candidateLoginCredentialsGenerationDO.setStatus(ACSConstants.MSG_FAILURE);
			// candidateLoginCredentialsGenerationDO
			// .setErrorMessage("Failure in generation of Excel With Generated LoginIds");
		}
		return downloadURL;
	}

	/**
	 * Returns all the AcsBatchCandidateAssociation List who ended their exam completely for specified batch, start-end
	 * date if forceStart flag is false .
	 * 
	 * @param batchId
	 *            auto generated id for batch
	 * @param forceStart
	 *            if true returns all the candidateIds whether an Rpack is already generated for a candidate or not (set
	 *            it to true only if u want to generate Rpack forcibly for all the candidates)
	 * @return list of {@link CandidateIdDO}
	 * @throws GenericDataModelException
	 */
	public List<AcsBatchCandidateAssociation> getBCAIdsbyBatchCodeStartEndDatesForTestEnds(String batchCode,
			boolean forceStart, Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info("specified input : batchId = {} and forceStart = {}", batchCode, forceStart);
		if (batchCode == null)
			return null;
		HashMap<String, Object> params = new HashMap<String, Object>();
		List<AcsBatchCandidateAssociation> bcas = new ArrayList<AcsBatchCandidateAssociation>();
		params.put("batchCode", batchCode);
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		// feedback changes
		params.put("feedBackStatus", CandidateFeedBackStatusEnum.NOT_YET_SUBMITTED);
		// ends feedback changes
		if (forceStart) {
			bcas = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_BCA_IDS_BY_BATCHID_FOR_TEST_ENDS, params);
		} else {
			params.put("status", false);
			bcas = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_IDS_BY_BATCHID_FOR_TEST_ENDS, params);
		}
		// logger.info("candidateId's = {}", candidateIds);
		if (bcas.equals(Collections.<Object> emptyList()))
			return null;
		return bcas;
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
	public String getSectionLanguagesOfCandidateByBCAId(int bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("bcaid", bcaId);
		String sectionLanguages =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_SECTION_LANGUAGES_BY_BCAID, params);
		return sectionLanguages;
	}
}