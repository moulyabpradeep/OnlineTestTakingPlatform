package com.merittrac.apollo.acs.services;

import java.io.IOException;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateTestStatusEnum;
import com.merittrac.apollo.acs.constants.ReportStatusEnum;
import com.merittrac.apollo.acs.dataobject.ActiveEventsInfoDO;
import com.merittrac.apollo.acs.dataobject.CDEDetailsDO;
import com.merittrac.apollo.acs.dataobject.CDEInfoDO;
import com.merittrac.apollo.acs.dataobject.CandidateIdDO;
import com.merittrac.apollo.acs.dataobject.CustomerDO;
import com.merittrac.apollo.acs.dataobject.CustomerEventCodesDO;
import com.merittrac.apollo.acs.dataobject.CustomerEventDivisionDO;
import com.merittrac.apollo.acs.dataobject.CustomerEventDivisionDetailsDO;
import com.merittrac.apollo.acs.dataobject.DivisionDO;
import com.merittrac.apollo.acs.dataobject.EventDetailsDO;
import com.merittrac.apollo.acs.dataobject.LoginIdGenerationDashboardDO;
import com.merittrac.apollo.acs.dataobject.PackExportInfoDO;
import com.merittrac.apollo.acs.dataobject.tp.BatchAssessmentSetDO;
import com.merittrac.apollo.acs.dataobject.tp.BusinessLayoutRulesDO;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsConfig;
import com.merittrac.apollo.acs.entities.AcsCustomer;
import com.merittrac.apollo.acs.entities.AcsDivision;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.entities.AcsReportDetails;
import com.merittrac.apollo.acs.entities.AcsTpExitSeq;
import com.merittrac.apollo.acs.entities.LoginIdAssessmentAssociationTO;
import com.merittrac.apollo.acs.entities.TPSpecifications;
import com.merittrac.apollo.acs.seb.dataobjects.SystemConfig;
import com.merittrac.apollo.acs.utility.JSONToMapConverter;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.cdb.entity.CandidateStatus;
import com.merittrac.apollo.cdb.entity.PackNotificationEntity;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.AttendanceMetaDataExportEntity;
import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.common.entities.acs.BatchExtensionInformationEntity;
import com.merittrac.apollo.common.entities.acs.CandidateIdentifiersEntity;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.acs.RPackExportEntity;
import com.merittrac.apollo.common.entities.acs.RpackMetaDataExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * This class contains all service level customer,division,event and assessment related api's.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class CDEAService extends BasicService implements ICDEAService {
	/**
	 * 
	 */

	private static BatchService bs = null;
	private static CDEAService cdeaService = null;
	private static Gson gson = null;
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static CandidateLoginCredentialsGeneratorService candidateLoginCredentialsGeneratorService = null;
	static {
		candidateLoginCredentialsGeneratorService = CandidateLoginCredentialsGeneratorService.getInstance();
		bs = BatchService.getInstance();
		gson = new Gson();
	}

	public CDEAService() {

	}

	/**
	 * Using double check singleton pattern
	 * 
	 * @return instance of batch service
	 * 
	 * @since Apollo v2.0
	 */
	public static final CDEAService getInstance() {
		if (cdeaService == null) {
			synchronized (QuestionService.class) {
				if (cdeaService == null) {
					cdeaService = new CDEAService();
				}
			}
		}
		return cdeaService;
	}

	/*
	 * @Override public boolean setCustomerDetails(AcsCustomer customerDetails) throws GenericDataModelException { //
	 * Persists in database. session.persist(customerDetails); return true; }
	 * 
	 * @Override public int getCustomerDetailsCount(String customerName, String divisionName, String eventName) throws
	 * GenericDataModelException { // Map of input parameters to the query. HashMap<String, Object> params = new
	 * HashMap<String, Object>(); params.put("customerName", customerName); params.put("divisionName", divisionName);
	 * params.put("eventName", eventName);
	 * 
	 * int count = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_CUSTOMER_COUNT_BY_CDE_NAMES, params);
	 * return count; }
	 * 
	 * @Override public int getAssessmentIdbyCDEANames(String customerName, String divisionName, String eventName,
	 * String assessmentName) throws GenericDataModelException { HashMap<String, Object> params = new HashMap<String,
	 * Object>(); params.put("customerName", customerName); params.put("divisionName", divisionName);
	 * params.put("eventName", eventName); params.put("assessmentName", assessmentName);
	 * 
	 * Integer result = (Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENT_ID__BY_CDEA_NAMES,
	 * params); if (result == null) return 0; else return result; }
	 * 
	 * @Override public AcsEvent getEventDetails(String customerName, String divisionName, String eventName) throws
	 * GenericDataModelException { HashMap<String, Object> params = new HashMap<String, Object>();
	 * params.put("customerName", customerName); params.put("divisionName", divisionName); params.put("eventName",
	 * eventName);
	 * 
	 * AcsEvent eventDetails = (AcsEvent) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENTDETAILS_BY_CDE_NAMES,
	 * params); return eventDetails; }
	 * 
	 * @Override public List<AcsEvent> getAllEventDetails() throws GenericDataModelException { String event_query =
	 * " from com.merittrac.apollo.acs.database.tableobject.EventDetailsTO"; List<AcsEvent> events =
	 * session.getListByQuery(event_query, null); return events; }
	 */

	@Override
	public List<String> getAllEventCodes() throws GenericDataModelException {
		List<String> eventCodes = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_EVENTCODES, null);
		return eventCodes;
	}

	@Override
	public AcsCustomer getCustomerDetailsByAssessmentCode(String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);

		AcsCustomer custDetails =
				(AcsCustomer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CUSTOMER_BY_ASSESSMENTCODE, params);
		return custDetails;
	}

	/**
	 * Will retrieve event details based on the assessment ID.
	 */
	@Override
	public AcsEvent getEventDetailsByAssessmentID(String assessmentId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentId", assessmentId);

		AcsAssessment assessmentDetails =
				(AcsAssessment) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENT_BY_ASSESSMENTID, params);
		if (assessmentDetails != null)
			return assessmentDetails.getAcseventdetails();
		else
			return null;
	}

	/**
	 * Will retrieve the exitSequence for an given assessmentID there by fetching based on event code.
	 */
	public String getExitSequenceByAssessmentID(String assessmentId) throws GenericDataModelException {
		AcsEvent event = getEventDetailsByAssessmentID(assessmentId);
		if (event != null) {
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("eventCode", event.getEventCode());
			AcsTpExitSeq exitseq =
					(AcsTpExitSeq) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EXIT_SEQ_BY_EVENT_CODE, params);
			if (exitseq != null)
				return exitseq.getExitSequence();
		}
		return ACSConstants.DEFAULT_EXIT_SEQUENCE;
	}

	@Override
	public String getCustomerDetailsLogoByAssessmentID(String assessmentId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentId", assessmentId);

		String logoPath =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CUSTOMER_LOGO_BY_ASSESSMENTID, params);
		return logoPath;
	}

	@Override
	public void updateEventDetails(AcsEvent eventDetails) throws GenericDataModelException {
		/*
		 * if (eventDetails != null && eventDetails.getBatchDetails() != null &&
		 * !eventDetails.getBatchDetails().isEmpty()) { for (Iterator batchIterator =
		 * eventDetails.getBatchDetails().iterator(); batchIterator.hasNext();) { AcsBatch batchDetails = (AcsBatch)
		 * batchIterator.next(); batchDetails = bs.updateBatchDetails(batchDetails, false, null); } }
		 */

		session.merge(eventDetails);
	}

	/*
	 * @Override public AcsAssessment getAssessmentDetailsbyAssessmentName(String customerName, String divisionName,
	 * String eventName, String assessmentName) throws GenericDataModelException { HashMap<String, Object> params = new
	 * HashMap<String, Object>(); params.put("customerName", customerName); params.put("divisionName", divisionName);
	 * params.put("eventName", eventName); params.put("assessmentName", assessmentName); AcsAssessment assessmentDetails
	 * = (AcsAssessment) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTDETAILS_BY_CDEA_NAMES, params);
	 * return assessmentDetails; }
	 */
	@Override
	public AcsAssessment updateAssessmentDetails(AcsAssessment assessmentDetails) throws GenericDataModelException {
		assessmentDetails = (AcsAssessment) session.merge(assessmentDetails);
		return assessmentDetails;
	}

	@Override
	public List<String> getBussinessRulesJSONByAssessmentIdAndBatchId(String assessmentId, String batchId)
			throws GenericDataModelException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentId);
		params.put("batchCode", batchId);

		List<String> list = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BRRULES_BY_ASSESSMENTID, params);
		if (list.equals(Collections.<Object> emptyList())) {
			statsCollector.log(startTime, "getBussinessRulesJSONByAssessmentIdAndBatchId", "CDEAService", 0);
			return null;
		}
		statsCollector.log(startTime, "getBussinessRulesJSONByAssessmentIdAndBatchId", "CDEAService", 0);
		return list;
	}
	
	public BusinessLayoutRulesDO getBussinessRulesLayoutRulesForCurrentRunningBatchesByUniqueIdentifier(
			Integer uniqueIdentifier) throws GenericDataModelException, IOException {

		if (uniqueIdentifier == null) {
			return new BusinessLayoutRulesDO("Invalid Input");
		}

		// get Unique Bean by ID
		LoginIdAssessmentAssociationTO loginIdAssessmentAssociation =
				candidateLoginCredentialsGeneratorService.getLoginIdAssessmentAssociationById(uniqueIdentifier);
		if (loginIdAssessmentAssociation == null) {
			return new BusinessLayoutRulesDO("No current Running Batches with entered ID");
		}

		// check whether there are any batches running or not
		List<AcsBatch> currentRunningAllBatches = bs.getBatchesbyTimeInstance();
		if (currentRunningAllBatches == null) {
			return new BusinessLayoutRulesDO("No current Running Batches");
			// no active batches
		}

		List<String> allCurrentRunningBatchCodes = new ArrayList<>();
		for (Iterator<AcsBatch> batchIterator = currentRunningAllBatches.iterator(); batchIterator.hasNext();) {
			allCurrentRunningBatchCodes.add(batchIterator.next().getBatchCode());
		}

		// list of batches for this assessment
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", loginIdAssessmentAssociation.getBatchCode());
		params.put("assessmentCode", loginIdAssessmentAssociation.getAssessmentCode());
		params.put("candidateType", CandidateType.WALKIN.ordinal());
		String query = null;
		String setId = loginIdAssessmentAssociation.getSetId();
		if (setId != null) {
			params.put("setid", setId);
			query = ACSQueryConstants.QUERY_FETCH_ACTIVE_BATCHCODES_BY_ASSESSMENTCODE_SETID;
		} else
			query = ACSQueryConstants.QUERY_FETCH_ACTIVE_BATCHCODES_BY_ASSESSMENTCODE;


		List<BatchAssessmentSetDO> batchCodesListWithSpecificToAssessment =
				(List<BatchAssessmentSetDO>) session.getResultListByNativeSQLQuery(
						query, params, BatchAssessmentSetDO.class);

		if (batchCodesListWithSpecificToAssessment.equals(Collections.<Object> emptyList())) {
			return new BusinessLayoutRulesDO("No current Running Batches with specified Assessment");
		}

		// get Current Running batches By Active Batches, and compare with batches-assessment, remove batch-assessment
		// from List if not current
		ListIterator<BatchAssessmentSetDO> iterator = batchCodesListWithSpecificToAssessment.listIterator();
		while (iterator.hasNext()) {
			BatchAssessmentSetDO batchAssessmentSetDO = iterator.next();
			String batchCode = batchAssessmentSetDO.getBatchCode();
			if (!allCurrentRunningBatchCodes.contains(batchCode))
				iterator.remove();
		}
		if (batchCodesListWithSpecificToAssessment == null || batchCodesListWithSpecificToAssessment.isEmpty()) {
			return new BusinessLayoutRulesDO("No current Running Batches with specified Assessment");
		}
		// get any batch with this assessment
		BatchAssessmentSetDO batchAssessmentSetDO = batchCodesListWithSpecificToAssessment.get(0);
		String assessmentCode = batchAssessmentSetDO.getAssessmentCode();
		String setCode = batchAssessmentSetDO.getSetId();
		String batchCode = batchAssessmentSetDO.getBatchCode();

		AcsBussinessRulesAndLayoutRules acsBussinessRulesAndLayoutRules =
				getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(assessmentCode, batchCode);
		BusinessLayoutRulesDO brlr = new BusinessLayoutRulesDO(acsBussinessRulesAndLayoutRules.getBrRules(),
				acsBussinessRulesAndLayoutRules.getLrRules(), batchCode, assessmentCode, setCode);
		return (brlr);
	}

	@Override
	public AcsBussinessRulesAndLayoutRules getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(
			String assessmentCode, String batchCode) throws GenericDataModelException {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);

		AcsBussinessRulesAndLayoutRules brlr =
				(AcsBussinessRulesAndLayoutRules) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_LRRULES_BY_ASSESSMENTID, params);
		statsCollector.log(startTime, "getLayoutRulesJSONByAssessmentIdAndBatchId", "CDEAService", 0);
		return brlr;
	}

	@Override
	public Object getBRRulePropertybyAssessmentIdAndBatchId(String asessmentId, String batchId, String propertyName)
			throws GenericDataModelException {
		List<String> brRules = this.getBussinessRulesJSONByAssessmentIdAndBatchId(asessmentId, batchId);
		if (brRules == null)
			return null;
		Object ruleContent = null;
		for (Iterator iterator = brRules.iterator(); iterator.hasNext();) {
			Map<String, Object> map = JSONToMapConverter.parseJSONtoMap((String) iterator.next());
			if (map.containsKey(propertyName)) {
				ruleContent = map.get(propertyName);
				break;
			}
		}
		return ruleContent;
	}

	@Override
	public Object getPropertyValueFromJson(String jsonString, String propertyName) {
		Map<String, Object> map = JSONToMapConverter.parseJSONtoMap(jsonString);
		if (map.containsKey(propertyName)) {
			return map.get(propertyName);
		}
		return null;
	}

	@Override
	public AcsAssessment getAssessmentDetailsbyCandIDAndBatchID(int candID, String batchID)
			throws GenericDataModelException {
		String aid = this.getAssessmentIdbyCandIDAndBatchID(candID, batchID);
		if (aid == null)
			return null;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("aid", aid);
		AcsAssessment assessmentDetails =
				(AcsAssessment) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTDETAILS_BY_CANDID_BATCHID,
						params);
		return assessmentDetails;
	}

	@Override
	public String getAssessmentIdbyCandIDAndBatchID(int candID, String batchID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candID", candID);
		params.put("batchID", batchID);
		String assessmentCode =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTID_BY_CANDID_BATCHID, params);
		return assessmentCode;
	}

	@Override
	public String getAssessmenNamebyCandIDAndBatchID(int candID, String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candID", candID);
		params.put("batchID", batchCode);

		Integer aid =
				(Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTID_BY_CANDID_BATCHID, params);
		if (aid == null)
			return null;
		else {
			AcsAssessment assessmentDetails = (AcsAssessment) session.get(aid, AcsAssessment.class.getCanonicalName());
			if (assessmentDetails != null)
				return assessmentDetails.getAssessmentName();
			else
				return null;
		}
	}

	@Override
	public int getBatchCandAssociationIdbyCandIDBatchIDAndAssessmentID(int candID, String batchID, String assessmentID)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candID", candID);
		params.put("batchID", batchID);
		params.put("assessmentID", assessmentID);

		Integer id =
				(Integer) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BATCHCANDASSOCIATION_ID_BY_CANDID_BATCHID_ASSESSMENTID, params);
		if (id == null)
			return 0;
		else
			return id;
	}

	@Override
	public boolean saveBrAndLrRules(AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRules)
			throws GenericDataModelException {
		session.persist(bussinessRulesAndLayoutRules);
		return true;
	}

	/*
	 * @Override public int getAssessmentCountbyCDEANames(String customerName, String divisionName, String eventName,
	 * String assessmentName) throws GenericDataModelException { // Map of input parameters to the query.
	 * HashMap<String, Object> params = new HashMap<String, Object>(); params.put("customerName", customerName);
	 * params.put("divisionName", divisionName); params.put("eventName", eventName); params.put("assessmentName",
	 * assessmentName);
	 * 
	 * int count = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENT_COUNT_BY_CDEA_NAMES, params);
	 * return count; }
	 */

	@Override
	public boolean saveAssessmentDetails(AcsAssessment assessmentDetails) throws GenericDataModelException {
		session.persist(assessmentDetails);
		return true;
	}

	@Override
	public List<AcsBussinessRulesAndLayoutRules> getBrAndLrRulesbyCDEAnames(String customerName, String divisionName,
			String eventName, String assessmentName) throws GenericDataModelException {
		// Map of input parameters to the query.
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("customerName", customerName);
		params.put("divisionName", divisionName);
		params.put("eventName", eventName);
		params.put("assessmentName", assessmentName);

		List<AcsBussinessRulesAndLayoutRules> bussinessRulesAndLayoutRules =
				(List<AcsBussinessRulesAndLayoutRules>) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BR_LR_RULES_BY_CDEA_NAMES, params);

		if (bussinessRulesAndLayoutRules != null && !bussinessRulesAndLayoutRules.isEmpty())
			return bussinessRulesAndLayoutRules;
		else
			return null;
	}

	@Override
	public boolean deleteBrAndLrRules(String brAndLrRulesId, String assessmentId) throws GenericDataModelException {
		AcsAssessment assessmentDetails =
				(AcsAssessment) session.get(assessmentId, AcsAssessment.class.getCanonicalName());
		AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRules =
				(AcsBussinessRulesAndLayoutRules) session.get(brAndLrRulesId,
						AcsBussinessRulesAndLayoutRules.class.getCanonicalName());
		// assessmentDetails.getBrAndLrRules().remove(bussinessRulesAndLayoutRules);
		session.update(assessmentDetails);
		session.delete(brAndLrRulesId, AcsBussinessRulesAndLayoutRules.class.getCanonicalName());
		return true;
	}

	@Override
	public List<AttendanceReportEntity> getCandidateAttendence(List<CandidateIdDO> candidateIds, String batchCode)
			throws GenericDataModelException {
		List<Integer> candIds = new ArrayList<Integer>();
		for (Iterator<CandidateIdDO> iterator = candidateIds.iterator(); iterator.hasNext();) {
			candIds.add(iterator.next().getCandidateId());
		}
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", candIds);
		List<AttendanceReportEntity> attendanceReportEntities = null;
		/*
		 * Added By Thyagu. To Support Open Event Attendance pack generation. Earlier Attendance pack was generated for
		 * all candidate / ids generated in the batch, by this implementation same will generated for Candidates
		 * respective to current RPack.
		 */

		logger.info("Get attendanceReportEntities for Regualar Event..");
		attendanceReportEntities =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ATTENDENCE_REPORT_DATA, params,
						AttendanceReportEntity.class);
		// }
		if (attendanceReportEntities.equals(Collections.<Object> emptyList()))
			return null;
		return attendanceReportEntities;
	}

	public List<CandidateResponseEntity> getCandidateResponsesByBatchId(int bcaid, String qpaperCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", bcaid);
		params.put("qpaperCode", qpaperCode);
		List<CandidateResponseEntity> candidateResponseEntity =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_CAND_RESPONSES_BY_BATCHID, params,
						CandidateResponseEntity.class);
		if (candidateResponseEntity.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return candidateResponseEntity;
	}

	public RPackExportEntity getRPackExportEntityByBatchId(String batchCode) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_BATCH_DETAILS_BY_BATCHID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		RPackExportEntity rPackExportEntity =
				(RPackExportEntity) session.getByQuery(query, params, RPackExportEntity.class);

		return rPackExportEntity;
	}

	public AcsCustomer getCustomerDetailsByCustomerCode(String customerCode) throws GenericDataModelException {
		AcsCustomer customer = (AcsCustomer) session.get(customerCode, AcsCustomer.class.getName());
		return customer;
	}

	public AcsDivision getDivisionDetailsByDivisionCode(String divisionCode) throws GenericDataModelException {
		AcsDivision division = (AcsDivision) session.get(divisionCode, AcsDivision.class.getName());
		return division;
	}

	public AcsEvent getEventDetailsByEventCode(String eventCode) throws GenericDataModelException {
		AcsEvent eventDetails = (AcsEvent) session.get(eventCode, AcsEvent.class.getCanonicalName());
		return eventDetails;
	}

	/*
	 * public boolean updateCustomerDetails(AcsCustomer customerDetails) throws GenericDataModelException { if
	 * (customerDetails != null && customerDetails.getDivision() != null &&
	 * !customerDetails.getDivisionDetails().isEmpty()) { // Session s = session.getSession(); for (Iterator
	 * divisionIterator = customerDetails .getDivisionDetails().iterator(); divisionIterator .hasNext();) { AcsDivision
	 * divisionDetails = (AcsDivision) divisionIterator.next(); if (divisionDetails != null &&
	 * divisionDetails.getEventDetails() != null && !divisionDetails.getEventDetails().isEmpty()) {
	 * 
	 * for (Iterator eventIterator = divisionDetails.getEventDetails().iterator(); eventIterator.hasNext();) { AcsEvent
	 * eventDetails = (AcsEvent) eventIterator.next(); if (eventDetails != null && eventDetails.getBatchDetails() !=
	 * null && !eventDetails.getBatchDetails().isEmpty()) { for (Iterator batchIterator = eventDetails
	 * .getBatchDetails().iterator(); batchIterator .hasNext();) { AcsBatch batchDetails = (AcsBatch)
	 * batchIterator.next(); batchDetails = bs.updateBatchDetails(batchDetails, false, null); } } } } } // s.close(); }
	 * 
	 * session.merge(customerDetails); return true; }
	 */

	public boolean updateDivisionDetails(AcsDivision divisionDetails) throws GenericDataModelException {
		/*
		 * if (divisionDetails != null && divisionDetails.getEventDetails() != null &&
		 * !divisionDetails.getEventDetails().isEmpty()) { for (Iterator eventIterator =
		 * divisionDetails.getEventDetails().iterator(); eventIterator.hasNext();) { AcsEvent eventDetails = (AcsEvent)
		 * eventIterator.next(); if (eventDetails != null && eventDetails.getBatchDetails() != null &&
		 * !eventDetails.getBatchDetails().isEmpty()) { for (Iterator batchIterator =
		 * eventDetails.getBatchDetails().iterator(); batchIterator.hasNext();) { AcsBatch batchDetails = (AcsBatch)
		 * batchIterator.next(); batchDetails = bs.updateBatchDetails(batchDetails, false, null); } } } }
		 */

		session.merge(divisionDetails);
		return true;
	}

	@Override
	public int getAssessmentIdByAssessmentCode(String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		Integer assessmentId =
				(Integer) session.getByQuery(
						"SELECT assessmentId FROM AcsAssessment WHERE assessmentCode = (:assessmentCode)", params);
		if (assessmentId == null) {
			return 0;
		}
		return assessmentId;
	}

	/**
	 * The getAssessmentDetailsByAssessmentCode API is used to get the Assessment details for a given assessment code.
	 * 
	 * @param assessmentCode
	 *            indicates unique Code which identifies Assessment.
	 * @return AssessmentDetailsTO object.
	 * @throws GenericDataModelException
	 */
	@Override
	public AcsAssessment getAssessmentDetailsByAssessmentCode(String assessmentCode) throws GenericDataModelException {
		AcsAssessment assessment = (AcsAssessment) session.get(assessmentCode, AcsAssessment.class.getName());
		return assessment;
	}

	@Override
	public CandidateIdentifiersEntity getCandidateIDList(int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candId", candId);
		CandidateIdentifiersEntity candidateIDList =
				(CandidateIdentifiersEntity) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDIDS_LIST_BY_CANDID,
						params, CandidateIdentifiersEntity.class);
		if (candidateIDList.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return candidateIDList;
	}

	@Override
	public CandidateStatus getCandidateLiveStatusData(int candId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_CAND_LIVE_DATA_BY_BATCHID_CANDID;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candidateId", candId);
		params.put("idleStatus", CandidateTestStatusEnum.IDLE.ordinal());
		CandidateStatus candidateStatus1 = (CandidateStatus) session.getByQuery(query, params, CandidateStatus.class);

		return candidateStatus1;
	}

	@Override
	public PackNotificationEntity getPackNotificationData(String packId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_PACK_STATUS;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);

		PackNotificationEntity packNotificationEntity =
				(PackNotificationEntity) session.getByQuery(query, params, PackNotificationEntity.class);

		return packNotificationEntity;
	}

	/**
	 * @deprecated There will be more than one ACS codes if multiple events exists
	 */
	@Override
	@Deprecated
	public String getACSServerCode() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ACS_SERVER_CODE;
		String acsServerCode = (String) session.getByQuery(query, null);

		return acsServerCode;
	}

	/**
	 * Get acs server code by event code.
	 * 
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getAcsCodeForEventCode(String eventCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("eventCode", eventCode);
		String acsServerCode =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACS_SERVER_CODE_BY_EVENTCODE, params);
		return acsServerCode;
	}

	@Override
	public PacksStatusEnum getPackStatus(String packId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_PACK_STATUS_DETAILS;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);

		PacksStatusEnum packStatusdetails = (PacksStatusEnum) session.getByQuery(query, params);

		return packStatusdetails;
	}

	@Override
	public String getAssessmentCodebyCandIDAndBatchID(int candID, String batchID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("candID", candID);
		params.put("batchID", batchID);

		String assessmentCode =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ASSESSMENTCODE_BY_CANDID_BATCHID, params);
		return assessmentCode;
	}

	@Override
	public List<String> getBatchCodesListByPackID(String packID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packID", packID);

		List<String> batchCodesList =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_BATCHCODES_BY_PACKID, params);
		if (batchCodesList.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return batchCodesList;
	}

	@Override
	public Long getCandidateResponseCount(String batchId, int candId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchId);
		params.put("candId", candId);
		params.put("questionType", QuestionType.READING_COMPREHENSION.toString());

		Long countResponses =
				(Long) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CANDIDATE_RESPONSES_COUNT, params);
		if (countResponses == null) {
			countResponses = (long) 0;
		}
		return countResponses;
	}

	/**
	 * Get event code for batch code (batch not cancelled).
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getEventCodeByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		String eventCode = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENTCODE_FOR_BATCHCODE, params);
		return eventCode;
	}

	/**
	 * Provides the ACS code based on the event of the batch provided
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getACSCodeByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		String acsCode = (String) session.getByQuery(ACSQueryConstants.GET_ACSCODE_BY_BATCHCODE, params);
		return acsCode;
	}

	/**
	 * Provides the ACS code based on the event of the batch provided
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsConfig getACSConfigByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		AcsConfig acsConfig = (AcsConfig) session.getByQuery(ACSQueryConstants.GET_ACSCONFIG_BY_BATCHCODE, params);
		return acsConfig;
	}

	public RpackMetaDataExportEntity getEventDivCusDetailsbyBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		RpackMetaDataExportEntity rpackMetaDataExportEntity =
				(RpackMetaDataExportEntity) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BY_BID,
						params, RpackMetaDataExportEntity.class);
		if (rpackMetaDataExportEntity.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return rpackMetaDataExportEntity;
	}

	/**
	 * The getQpIdByBatchIdAndCandId API is used to get the question paper Id and QP-shuffle sequence based on the
	 * candidate id and batchId.
	 * 
	 * @param batchId
	 * @param candId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getQpIdByBatchIdAndCandId(String batchId, int candId) throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_QPID_BY_CID_BID;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchId);
		params.put("candId", candId);
		String qpId = (String) session.getByQuery(query, params);

		return qpId;
	}

	public String getAssessmentNameByAssessmentCode(String assessmentCode) throws GenericDataModelException {
		AcsAssessment assessmentDetails =
				(AcsAssessment) session.get(assessmentCode, AcsAssessment.class.getCanonicalName());
		if (assessmentDetails != null)
			return assessmentDetails.getAssessmentName();
		else
			return null;
	}

	public CustomerEventCodesDO getCustomerEventCodesByBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		CustomerEventCodesDO customerEventIds =
				(CustomerEventCodesDO) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CUST_EVENT_CODE_FOR_BATCHCODE,
						params, CustomerEventCodesDO.class);
		return customerEventIds;
	}

	public RpackMetaDataExportEntity getEventDivCusDetailsbyBatchCode(String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);
		RpackMetaDataExportEntity rpackMetaDataExportEntity =
				(RpackMetaDataExportEntity) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BY_EVENTCODE, params,
						RpackMetaDataExportEntity.class);
		if (rpackMetaDataExportEntity.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return rpackMetaDataExportEntity;
	}

	/**
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AttendanceMetaDataExportEntity getAttendenceMetadataByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		AttendanceMetaDataExportEntity attendanceMetaDataExportEntity =
				(AttendanceMetaDataExportEntity) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BY_BATCHCODE, params,
						AttendanceMetaDataExportEntity.class);
		if (attendanceMetaDataExportEntity.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return attendanceMetaDataExportEntity;
	}

	/**
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public CustomerEventDivisionDO getEventDivisionCusDetailsbyBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		CustomerEventDivisionDO customerEventDivisionDO =
				(CustomerEventDivisionDO) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BATCH_BY_BATCHCODE, params,
						CustomerEventDivisionDO.class);
		if (customerEventDivisionDO == null || customerEventDivisionDO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return customerEventDivisionDO;
	}

	/**
	 * 
	 * @param batchID
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public CustomerEventDivisionDO getEventDivisionCusDetailsbyBatchID(String batchID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchID);

		CustomerEventDivisionDO customerEventDivisionDO =
				(CustomerEventDivisionDO) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BATCH_BY_BATCHCODE, params,
						CustomerEventDivisionDO.class);
		if (customerEventDivisionDO == null || customerEventDivisionDO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return customerEventDivisionDO;
	}

	/**
	 * getPackInfoForCurrentDate API used to get the pack info for current date.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsPacks> getPackInfoForCurrentDate() throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Calendar cal = Calendar.getInstance();
		params.put("currentDate", dateFormat.format(cal.getTime()));

		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_PACK_DATA_FOR_CURRENT_DATE, params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;

		// List<PackExportInfoDO> packDetails =
		// session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_PACK_INFO_FOR_CURRENTDATE,
		// params, 0);
		// System.out.println(new Gson().toJson(packDetails));
	}

	/**
	 * getAllPackInfoTillCurrentDate API used to get all pack info till current date
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsPacks> getAllPackInfoTillCurrentDate() throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();

		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_PACK_DATA_TILL_CURRENT_DATE, params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	/**
	 * getPackInfoWithinSpecifiedTime API used to get the pack info with specified range.
	 * 
	 * @param lastEODGeneratedTime
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsPacks> getPackInfoWithinSpecifiedTime(Calendar lastEODGeneratedTime)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		Calendar cal = Calendar.getInstance();
		params.put("currentDate", cal);
		params.put("lastEODGeneratedTime", lastEODGeneratedTime);

		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_ALL_PACKS_DATA_WITHIN_SPECIFIED_TIME_RANGE, params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	public AcsBatch getBatchDetailsList(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODES_CONST, batchCode);

		// String query =
		// "FROM BatchDetailsTO b WHERE b.batchCode IN (:batchCodesList)";
		String query = ACSQueryConstants.QUERY_FETCH_ALL_BATCH_DETAILS_LIST;

		AcsBatch batchDetails = (AcsBatch) session.getByQuery(query, params);
		if (batchDetails.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return batchDetails;
	}

	public AcsReportDetails getReportDetails(String reportIdentifier) throws GenericDataModelException {
		AcsReportDetails reportDetails =
				(AcsReportDetails) session.get(reportIdentifier, AcsReportDetails.class.getCanonicalName());
		return reportDetails;
	}

	public List<AcsReportDetails> getReportDetailsByDateRange(Calendar startDate, Calendar endDate)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		String query = ACSQueryConstants.QUERY_FETCH_ALL_REPORT_DETAILS_BY_DATE_RANGE;
		List<AcsReportDetails> reportDetails = session.getListByQuery(query, params);
		if (reportDetails.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return reportDetails;
	}

	/**
	 * updateReportStatusByReportId API used to update the report status.
	 * 
	 * @param reportId
	 * @param reportStatus
	 * @param errorMsg
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateReportStatusByReportId(String reportId, ReportStatusEnum reportStatus, String errorMsg)
			throws GenericDataModelException {
		String query = ACSQueryConstants.UPDATE_REPORTDETAILS;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportId", reportId);
		params.put("reportStatus", reportStatus);
		params.put("errorMsg", errorMsg);

		switch (reportStatus) {
			case UPLOADED:
				params.put("actionTime", Calendar.getInstance());
				query = ACSQueryConstants.UPDATE_REPORTDETAIL_FOR_UPLOADED;
				break;
			case GENERATED:
				params.put("actionTime", Calendar.getInstance());
				query = ACSQueryConstants.UPDATE_REPORTDETAILS_FOR_GENERATED;
				break;

			case UPLOAD_STARTED:
				params.put("actionTime", Calendar.getInstance());
				query = ACSQueryConstants.UPDATE_REPORTDETAILS_FOR_UPLOADSTARTED;
				break;

			default:
				break;
		}

		session.updateByQuery(query, params);
		return true;
	}

	public Object getBRRulePropertybyCandIDAndBatchID(int candID, String batchID, String propertyName)
			throws GenericDataModelException {
		Object value = null;
		String aid = this.getAssessmentIdbyCandIDAndBatchID(candID, batchID);
		if (aid != null) {
			value = getBRRulePropertybyAssessmentIdAndBatchId(aid, batchID, propertyName);
		}
		return value;
	}

	public boolean saveReportDetails(AcsReportDetails reportDetailsTO) throws GenericDataModelException {
		session.persist(reportDetailsTO);
		return true;
	}

	/**
	 * @param reportIdentifier
	 * @param uploadInProgress
	 * @param errorMsg
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateReportStatusByReportIdentifier(String reportIdentifier, ReportStatusEnum reportStatus,
			String errorMsg) throws GenericDataModelException {
		String query =
				"UPDATE AcsReportDetails set reportStatus=(:reportStatus) , errorMessage=(:errorMsg) WHERE reportIdentifier=(:reportIdentifier)";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportIdentifier", reportIdentifier);
		params.put("reportStatus", reportStatus);
		params.put("errorMsg", errorMsg);

		switch (reportStatus) {
			case UPLOADED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsReportDetails set reportStatus=(:reportStatus) , errorMessage=(:errorMsg) ,uploadEndTime=(:actionTime)  WHERE reportIdentifier=(:reportIdentifier)";
				break;
			case GENERATED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsReportDetails set reportStatus=(:reportStatus) , errorMessage=(:errorMsg) ,generatedEndTime=(:actionTime)  WHERE reportIdentifier=(:reportIdentifier)";
				break;
			case UPLOAD_STARTED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsReportDetails set reportStatus=(:reportStatus) , errorMessage=(:errorMsg) ,uploadStartTime=(:actionTime)  WHERE reportIdentifier=(:reportIdentifier)";
				break;

			default:
				break;
		}

		session.updateByQuery(query, params);
		return true;
	}

	/**
	 * getLayoutRulePropertybyAssessmentId API used to read the layout rules with property
	 * 
	 * @param asessmentId
	 * @param propertyName
	 * @return
	 * @throws GenericDataModelException
	 */
	/*
	 * public Object getLayoutRulePropertybyAssessmentIdAndBatchId(String assessmentId, String batchId, String
	 * propertyName) throws GenericDataModelException { List<String> layOutRules =
	 * this.getLayoutRulesJSONByAssessmentIdAndBatchId(assessmentId, batchId); if (layOutRules == null) return null;
	 * Object ruleContent = null; for (Iterator iterator = layOutRules.iterator(); iterator.hasNext();) { Map<String,
	 * Object> map = JSONToMapConverter.parseJSONtoMap((String) iterator.next()); if (map.containsKey(propertyName)) {
	 * ruleContent = map.get(propertyName); break; } } return ruleContent; }
	 */
	/**
	 * getLayoutRulePropertybyCandIDAndBatchID API used to read the layout rules with property and candid,batchid.
	 * 
	 * @param candID
	 * @param batchID
	 * @param propertyName
	 * @return
	 * @throws GenericDataModelException
	 */
	public Object getLayoutRulePropertybyCandIDAndBatchID(int candID, String batchID, String propertyName)
			throws GenericDataModelException {
		Object value = null;
		String aid = this.getAssessmentIdbyCandIDAndBatchID(candID, batchID);
		/*
		 * if (aid != null) { value = getLayoutRulePropertybyAssessmentIdAndBatchId(aid, batchID, propertyName); }
		 */
		return value;
	}

	/**
	 * getDefaultBufferTimeToBeAddToRpackAETJob API used to get defaultBufferTimeToAddRpackAETJobInMinutes property
	 * value
	 * 
	 * @return
	 */
	public int getDefaultBufferTimeToBeAddToRpackAETJob() {
		int value = ACSConstants.DEFAULT_BUFFER_TIME_TO_ADD_RPACK_AET_JOB_MIN;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.BUFFER_TIME_TO_ADD_RPACK_AET_JOB_MIN);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			value = Integer.parseInt(propValue);
		}
		return value;
	}

	public AttendanceMetaDataExportEntity getEventDivCusDetailsbyBatchCodeForAttendance(String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);
		AttendanceMetaDataExportEntity attendanceMetaDataExportEntity =
				(AttendanceMetaDataExportEntity) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BY_EVENTCODE, params,
						AttendanceMetaDataExportEntity.class);
		if (attendanceMetaDataExportEntity.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return attendanceMetaDataExportEntity;
	}

	/**
	 * gets the property value from the business rules json, can be sent multiple properties as well
	 * 
	 * @param assessmentId
	 * @param propertyNames
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public Map<String, Object> getMultiplePropertyValuesFromJSON(String jsonString, List<String> propertyNames)
			throws GenericDataModelException {
		logger.info("initiated getMultiplePropertyValuesFromJSON where jsonString={} and list of property names = {}",
				jsonString, propertyNames);

		Map<String, Object> ruleContent = new HashMap<String, Object>();
		if (jsonString != null) {
			// convert the business rules json into map
			Map<String, Object> map = JSONToMapConverter.parseJSONtoMap(jsonString);
			logger.info("map representation of business rules json = {}", map);

			// iterate over the map and find the values for the required
			// properties
			for (Iterator propertyNamesIterator = propertyNames.iterator(); propertyNamesIterator.hasNext();) {
				String propertyName = (String) propertyNamesIterator.next();
				if (map.containsKey(propertyName)) {
					ruleContent.put(propertyName, map.get(propertyName));
				}
			}
		} else {
			logger.info("invalid json input = {}", jsonString);
		}
		return ruleContent;
	}

	public int getLateLoginTimeByAssessmnetIdAndBatchId(String assessmentId, String batchId)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentId", assessmentId);
		params.put("batchId", batchId);

		Integer lateLoginTime =
				(Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_LATELOGIN_FOR_BATCH_AND_ASSESSMENT, params);
		if (lateLoginTime == null) {
			lateLoginTime = -1;
		}
		return lateLoginTime;
	}

	public int getLateLoginTimeByAssessmnetCodeAndBatchCode(String assessmentCode, String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);
		Integer lateLoginTime =
				(Integer) session.getByQuery(ACSQueryConstants.QUERY_FETCH_LATELOGINTIME_BY_ASSESSMENTANDBATCHCODE,
						params);
		if (lateLoginTime == null) {
			lateLoginTime = -1;
		}
		return lateLoginTime;
	}

	/**
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<PackExportInfoDO> getRPackManaullyGeneratedtDetails(Calendar startDate, Calendar endDate,
			String eventCode) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		String query = "";
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		params.put("eventCode", eventCode);

		params.put("PackTypes", PackContent.Rpack);
		query = ACSQueryConstants.QUERY_FETCH_RPACKS_MANUAL_GENERATED_INFO_WITHIN_SPECIFIED_TIME;

		List<PackExportInfoDO> packDetails = session.getResultAsListByQuery(query, params, 0, PackExportInfoDO.class);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	/**
	 * Gets active event information which includes customer, division along with event information
	 * 
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<ActiveEventsInfoDO> getActiveEventInfoByTimeInstance() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("currentDateTime", Calendar.getInstance());

		List<ActiveEventsInfoDO> activeEventsInfoDOs =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ACTIVE_EVENT_DIV_CUS_BY_TIME_INSTANCE,
						params, 0, ActiveEventsInfoDO.class);
		if (activeEventsInfoDOs.isEmpty()) {
			return null;
		}
		return activeEventsInfoDOs;
	}

	public BatchExtensionInformationEntity getCustomerDivisionEventCodesByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		BatchExtensionInformationEntity batchExtensionInformationEntity =
				(BatchExtensionInformationEntity) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CDE_FOR_BATCH,
						params, BatchExtensionInformationEntity.class);
		return batchExtensionInformationEntity;
	}

	/**
	 * Get {@link AcsBussinessRulesAndLayoutRules} for assessment code and batch code.
	 * 
	 * @param batchCode
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsBussinessRulesAndLayoutRules getBussinessRulesAndLayoutRulesByBatchIdAndAssessmentId(String batchCode,
			String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("assessmentCode", assessmentCode);

		AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
				(AcsBussinessRulesAndLayoutRules) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BRLR_FOR_ASSESSMENT_AND_BATCH_CODE, params);
		return bussinessRulesAndLayoutRulesTO;
	}

	public List<String> getAssessmentIdsByBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("batchCode", batchCode);
		List<String> assessmentIds = session.getListByQuery(ACSQueryConstants.GET_ASSESSMENTID_BY_BATCHID, params);
		return assessmentIds;
	}

	/**
	 * Update {@link AcsBussinessRulesAndLayoutRules}
	 * 
	 * @param acsBussinessRulesAndLayoutRules
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean updateBrAndLrRules(AcsBussinessRulesAndLayoutRules acsBussinessRulesAndLayoutRules)
			throws GenericDataModelException {
		session.merge(acsBussinessRulesAndLayoutRules);
		return true;
	}

	public AcsEvent getEventDetailsByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		AcsEvent eventDetailsTO =
				(AcsEvent) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENT_DETAILS_BY_BATCH_ID, params);
		if (eventDetailsTO == null || eventDetailsTO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return eventDetailsTO;
	}

	/**
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public CustomerEventDivisionDetailsDO getEventDivisionCusAllDetailsbyBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		CustomerEventDivisionDetailsDO customerEventDivisionDO =
				(CustomerEventDivisionDetailsDO) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_EVENT_DIV_CUS_BATCH_BY_BATCHCODE, params,
						CustomerEventDivisionDetailsDO.class);
		if (customerEventDivisionDO == null || customerEventDivisionDO.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return customerEventDivisionDO;
	}

	/**
	 * Get brlr details for assessment code.
	 * 
	 * @param assessmentId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Set<AcsBussinessRulesAndLayoutRules> getBussinessRulesAndLayoutRulesByAssessmentCode(String assessmentCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		List<AcsBussinessRulesAndLayoutRules> acsBussinessRulesAndLayoutRulesList =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BRLR_FOR_ASSESSMENTCODE, params, 0);
		Set<AcsBussinessRulesAndLayoutRules> bussinessRulesAndLayoutRulesSet = null;
		if (!acsBussinessRulesAndLayoutRulesList.isEmpty() || acsBussinessRulesAndLayoutRulesList != null) {
			bussinessRulesAndLayoutRulesSet =
					new HashSet<AcsBussinessRulesAndLayoutRules>(acsBussinessRulesAndLayoutRulesList);
		}
		return bussinessRulesAndLayoutRulesSet;
	}

	/**
	 * Get {@link AcsEvent} by eventCode.
	 * 
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsEvent loadEvent(String eventCode) throws GenericDataModelException {
		return (AcsEvent) session.get(eventCode, AcsEvent.class.getName());
	}

	/**
	 * Get {@link AcsAssessment} for assessment code.
	 * 
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsAssessment loadAssessment(String assessmentCode) throws GenericDataModelException {
		return (AcsAssessment) session.get(assessmentCode, AcsAssessment.class.getName());
	}

	public AcsBussinessRulesAndLayoutRules getBussinessRulesAndLayoutRulesByBatchCodeAndAssessmentCode(
			String batchCode, String assessmentCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("assessmentCode", assessmentCode);

		AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRulesTO =
				(AcsBussinessRulesAndLayoutRules) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_BRLR_BY_ASSESSMENTANDBATCHCODE, params);
		return bussinessRulesAndLayoutRulesTO;
	}

	public void saveOrUpdateDivision(AcsDivision divisionDetails) throws GenericDataModelException {
		session.saveOrUpdate(divisionDetails);
	}

	public void saveOrUpdateEvent(AcsEvent eventDetails) throws GenericDataModelException {
		session.saveOrUpdate(eventDetails);
	}

	public void saveOrUpdateCustomer(AcsCustomer customerDetails) throws GenericDataModelException {
		session.saveOrUpdate(customerDetails);
	}

	public void saveOrUpdateAssessments(List<AcsAssessment> assessmentDetails) throws GenericDataModelException {
		session.saveOrUpdate(assessmentDetails);
	}

	public void saveOrUpdateAssessmentServer(AcsConfig assessmentServer) throws GenericDataModelException {
		session.saveOrUpdate(assessmentServer);
	}

	/**
	 * @param bussinessRulesAndLayoutRulesSet
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void bulkInsertBrLrRules(Set<AcsBussinessRulesAndLayoutRules> bussinessRulesAndLayoutRulesSet)
			throws GenericDataModelException {
		ArrayList<AcsBussinessRulesAndLayoutRules> arrayList =
				new ArrayList<AcsBussinessRulesAndLayoutRules>(bussinessRulesAndLayoutRulesSet);
		session.saveOrUpdate(arrayList);

	}

	public CDEDetailsDO getCustomerDivisionEventCodesByEventCode(String eventCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);

		CDEDetailsDO cdeDetailsDO =
				(CDEDetailsDO) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CDE_FOR_EVENT, params,
						CDEDetailsDO.class);
		return cdeDetailsDO;
	}

	/**
	 * get the list of customer, division and event details based on the list of specified event codes.
	 * 
	 * @param eventCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<CDEDetailsDO> getCustomerDivisionAndEventDetailsByEventCodes(List<String> eventCodes)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCodes);
		String query = ACSQueryConstants.QUERY_FETCH_CDE_FOR_EVENT;

		List<CDEDetailsDO> cdeDetailsDOs =
				(List<CDEDetailsDO>) session.getResultAsListByQuery(query, params, 0, CDEDetailsDO.class);
		return cdeDetailsDOs;
	}

	/**
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public CDEInfoDO getCustomerDivisionEventInfoByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		CDEInfoDO cdeInfoDO =
				(CDEInfoDO) session.getByQuery(ACSQueryConstants.QUERY_FETCH_CDE_INFO_FOR_BATCH, params,
						CDEInfoDO.class);
		return cdeInfoDO;
	}

	/**
	 * checks whether the event associated to the specified event id is open event or not
	 * 
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<LoginIdGenerationDashboardDO> getAssessmentInformationOfOpenEventByEventCode(String eventCode)
			throws GenericDataModelException {

		List<LoginIdGenerationDashboardDO> loginIdAssessmentAssociationTOs =
				candidateLoginCredentialsGeneratorService.getLoginIdAssessmentAssociationOnEventCode(eventCode);
		if (loginIdAssessmentAssociationTOs != null && !loginIdAssessmentAssociationTOs.isEmpty()) {
			for (LoginIdGenerationDashboardDO loginIdGenerationDashboardDO : loginIdAssessmentAssociationTOs) {
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("batchCode", loginIdGenerationDashboardDO.getBatchCode());
				params.put("assessmentCode", loginIdGenerationDashboardDO.getAssessmentCode());
				params.put("candType", CandidateType.WALKIN.ordinal());
				String query =
						"SELECT count(bcaid) as expiredcount FROM acsbatchcandidateassociation a "
								+ " WHERE candidatetype=(:candType) and logintime is null "
								+ " and expirydatetime<=now() and batchcode in (:batchCode) "
								+ " and assessmentcode in (:assessmentCode)";
				if (loginIdGenerationDashboardDO.getSetId() != null
						&& !loginIdGenerationDashboardDO.getSetId().isEmpty()) {
					params.put("setId", loginIdGenerationDashboardDO.getSetId());
					query =
							"SELECT count(bcaid) as expiredcount FROM acsbatchcandidateassociation a "
									+ " WHERE candidatetype=(:candType) and logintime is null "
									+ " and expirydatetime<=now() and batchcode in (:batchCode) "
									+ " and assessmentcode in (:assessmentCode) and setid in (:setId)";
				}
				BigInteger expiredCount = (BigInteger) session.getUniqueResultByNativeSQLQuery(query, params);
				loginIdGenerationDashboardDO.setExpiredCount(expiredCount);
			}
		}
		return loginIdAssessmentAssociationTOs;
	}

	/**
	 * parse the details fetched from db and order according to the customer
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, CustomerDO> parseCDEDetails(List<CDEDetailsDO> cdeDetailsDOs) {
		Map<String, CustomerDO> customerDOs = new LinkedHashMap<String, CustomerDO>();

		for (Iterator iterator = cdeDetailsDOs.iterator(); iterator.hasNext();) {
			CDEDetailsDO cdeDetailsDO = (CDEDetailsDO) iterator.next();

			EventDetailsDO eventDetailsDO = new EventDetailsDO();
			eventDetailsDO.setEventCode(cdeDetailsDO.getEventCode());
			eventDetailsDO.setEventDate(cdeDetailsDO.getEventStartDate());
			eventDetailsDO.setEventEndDate(cdeDetailsDO.getEventEndDate());
			eventDetailsDO.setEventName(cdeDetailsDO.getEventName());
			eventDetailsDO.setEventType(cdeDetailsDO.getEventType());

			if (customerDOs.containsKey(cdeDetailsDO.getCustomerCode())) {
				if (customerDOs.get(cdeDetailsDO.getCustomerCode()).getDivisionDOs()
						.containsKey(cdeDetailsDO.getDivisionCode())) {
					customerDOs.get(cdeDetailsDO.getCustomerCode()).getDivisionDOs()
							.get(cdeDetailsDO.getDivisionCode()).getEventDetailsDOs()
							.put(cdeDetailsDO.getEventCode(), eventDetailsDO);
				} else {
					Map<String, EventDetailsDO> eventDetailsDOs = new LinkedHashMap<String, EventDetailsDO>();
					eventDetailsDOs.put(cdeDetailsDO.getEventCode(), eventDetailsDO);

					DivisionDO divisionDO = new DivisionDO();
					divisionDO.setDivisionCode(cdeDetailsDO.getDivisionCode());
					divisionDO.setDivisionName(cdeDetailsDO.getDivisionName());
					divisionDO.setEventDetailsDOs(eventDetailsDOs);

					customerDOs.get(cdeDetailsDO.getCustomerCode()).getDivisionDOs()
							.put(cdeDetailsDO.getDivisionCode(), divisionDO);
				}
			} else {
				Map<String, EventDetailsDO> eventDetailsDOs = new LinkedHashMap<String, EventDetailsDO>();
				eventDetailsDOs.put(cdeDetailsDO.getEventCode(), eventDetailsDO);

				DivisionDO divisionDO = new DivisionDO();
				divisionDO.setDivisionCode(cdeDetailsDO.getDivisionCode());
				divisionDO.setDivisionName(cdeDetailsDO.getDivisionName());
				divisionDO.setEventDetailsDOs(eventDetailsDOs);

				Map<String, DivisionDO> divisionDOs = new LinkedHashMap<String, DivisionDO>();
				divisionDOs.put(cdeDetailsDO.getDivisionCode(), divisionDO);

				CustomerDO customerDO = new CustomerDO();
				customerDO.setCustomerCode(cdeDetailsDO.getCustomerCode());
				customerDO.setCustomerName(cdeDetailsDO.getCustomerName());
				customerDO.setDivisionDOs(divisionDOs);

				customerDOs.put(cdeDetailsDO.getCustomerCode(), customerDO);
			}
		}

		return customerDOs;
	}

	/**
	 * Get event code for batch code (batch not cancelled).
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getEventCodeByBcaId(Integer bcaId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaId", bcaId);

		String eventCode = (String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENTCODE_FOR_BCAID, params);
		return eventCode;
	}

	public boolean saveTPProfile(SystemConfig config) throws GenericDataModelException {
		logger.debug("--IN-- saveTPProfile");
		TPSpecifications tps = getTPSpecificationDetailsByMACandIP(config.getMacAddress(), config.getIpAddress());
		if (tps == null) {
			tps = new TPSpecifications(config.getMacAddress(), config.getIpAddress(),
					config.getSystemInformation() == null ? null : gson.toJson(config.getSystemInformation()),
					config.getOS() == null ? null : gson.toJson(config.getOS()),
					config.getProcessor() == null ? null : gson.toJson(config.getProcessor()),
					config.getMemory() == null ? null : gson.toJson(config.getMemory()),
					config.getStorages() == null ? null : gson.toJson(config.getStorages()),
					config.getNics() == null ? null : gson.toJson(config.getNics()),
					config.getNetworkParameters() == null ? null : gson.toJson(config.getNetworkParameters()),
					config.getUsbDevices() == null ? null : gson.toJson(config.getUsbDevices()));
			session.persist(tps);
		}
		return true;
	}

	public TPSpecifications getTPSpecificationDetailsByMACandIP(String mac, String ip)
			throws GenericDataModelException {
		logger.debug("--IN-- getTPSpecificationDetailsByMACandIP");
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("macAddress", mac);
		params.put("ipAddress", ip);
		TPSpecifications tpSpecifications =
				(TPSpecifications) session.getByQuery(ACSQueryConstants.QUERY_FETCH_TPSPECIFICATIONS_BY_MAC_IP, params);
		return tpSpecifications;
	}
}
