package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.entities.acs.PackSubTypeEnum;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.bean.CDEDetailsForPlayBack;
import com.merittrac.apollo.data.bean.CandidateDetailsForPlayBack;
import com.merittrac.apollo.data.bean.CombinedBeanForPlayBack;
import com.merittrac.apollo.data.bean.PlayBackResponseBean;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCumulativeResponses;
import com.merittrac.apollo.data.entity.RpsCustomer;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsManualPacksHistory;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociation;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsQuestionPaperLite;
import com.merittrac.apollo.data.entity.RpsQuestionPaperPack;
import com.merittrac.apollo.data.entity.RpsRpackComponent;
import com.merittrac.apollo.data.entity.RpsVenue;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsCumulativeResponsesService;
import com.merittrac.apollo.data.service.RpsCustomerService;
import com.merittrac.apollo.data.service.RpsDivisionService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsManualPacksHistoryService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.data.service.RpsQuestionAssociationService;
import com.merittrac.apollo.data.service.RpsQuestionPaperPackService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsQuestionService;
import com.merittrac.apollo.data.service.RpsRpackComponentService;
import com.merittrac.apollo.data.service.RpsVenueService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.utility.ImageWriterUtility;
import com.merittrac.apollo.rps.common.utility.PDFWriterUtility;
import com.merittrac.apollo.rps.reportexport.entity.Parameters;
import com.merittrac.apollo.rps.reports.ExportTiffService;
import com.merittrac.apollo.rps.rpack.dataobject.DescriptiveDetails;
import com.merittrac.apollo.rps.ui.entity.AnswerKeyStatusInfoRow;
import com.merittrac.apollo.rps.ui.entity.AnswerStatus;
import com.merittrac.apollo.rps.ui.entity.PackStatusInfoRow;
import com.merittrac.apollo.rps.ui.entity.PacksChangeHistory;
import com.merittrac.apollo.rps.ui.entity.ProcessResultInfoRow;
import com.merittrac.apollo.rps.ui.entity.QPackStatusInfoRow;
import com.merittrac.apollo.rps.ui.entity.ResultParams;
import com.merittrac.apollo.rps.ui.entity.RpsCustomerInfo;
import com.merittrac.apollo.rps.ui.entity.RpsDivisionInfo;
import com.merittrac.apollo.rps.ui.entity.RpsEventInfo;


public class RpsUIDataService implements IRpsUIDataService {

	@Autowired
	RpsCustomerService rpsCustomerService;

	@Autowired
	RpsDivisionService rpsDivisionService;

	@Autowired
	RpsEventService rpsEventService;

	@Autowired
	RpsBatchService rpsBatchService;

	@Autowired
	RpsAcsServerServices rpsAcsServerServices;

	@Autowired
	RpsPackService rpsPackService;

	@Autowired
	RpsRpackComponentService rpsRpackComponentService;

	@Autowired
	RpsQuestionPaperPackService rpsQuestionPaperPackService;

	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;

	@Autowired
	RpsQuestionService rpsQuestionService;

	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsCumulativeResponsesService rpsCumulativeResponsesService;

	@Autowired
	ResultComputationService resultComputationService;

	@Autowired
	RpsAssessmentService rpsAssessmentService;

	@Autowired
	QPackPerQPIDService qPackPerQPIDService;

	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	@Autowired
	RpsManualPacksHistoryService rpsManualPacksHistoryService;

	@Autowired
	RpsCandidateResponseService rpsCandidateResponseService;

	@Autowired
	ImageWriterUtility imageWriter;

	@Autowired
	RpsQuestionAssociationService rpsQuestionAssociationService;

	@Autowired
	RpsVenueService rpsVenueService;

	@Autowired
	ExportTiffService exportTiffService;

	@Autowired
	LogViewerService logViewerService;
	@Autowired
	CryptUtil cryptUtil;


	private Logger logger = LoggerFactory.getLogger(RpsUIDataService.class);

	private final String EMPTY_JSON = "[]";

	private final String VERSION = "_Version";

	private final String RPACK_VERSION = "_V";

	private final String NA = "NA";

	@Value("${xmlLocation}")
	private String xmlLocation;

	@Value("${soeLocation}")
	private String soeLocation;

	@Value("${apollo_home_dir}")
	private String apolloHome;

	@Value("${rps_download_url_playback}")
	private String rpsDownloadUrl;

	@Value("${playback_uri}")
	private String playbackUri;

	@Value("${playback_password}")
	private String playbackPassword;

	public String getAllCustomers() {
		Gson gson = new Gson();
		// create customers info list
		List<RpsCustomerInfo> rpsCustomersInfoList = new ArrayList<RpsCustomerInfo>();
		// get list of customers from database
		List<RpsCustomer> rpsCustomersList = rpsCustomerService.getAllRpsCustomers();
		if (rpsCustomersList == null) {
			logger.error("no list of Customers is available");
			return "";
		}
		Iterator<RpsCustomer> it = rpsCustomersList.iterator();

		// populate customers info list
		while (it.hasNext()) {
			RpsCustomerInfo rpsCustomerInfo = new RpsCustomerInfo();
			RpsCustomer rpsCustomer = it.next();

			rpsCustomerInfo.setCustomerCode(rpsCustomer.getCustomerCode());
			rpsCustomerInfo.setCustomerName(rpsCustomer.getCustomerName());

			// add RpsCustomerInfo object to the list
			rpsCustomersInfoList.add(rpsCustomerInfo);
		}

		// generate JSON string of RpsCustomerInfo list objects
		String customers = gson.toJson(rpsCustomersInfoList);
		return customers;
	}

	public String getAllDivisionsByCustomer(String customerCode) {
		Gson gson = new Gson();
		// create list of rps divsion info
		List<RpsDivisionInfo> rpsDivisionsInfoList = new ArrayList<RpsDivisionInfo>();
		// get divisions list per customer code from database
		List<RpsDivision> rpsDivisionsList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(customerCode);
		if (rpsDivisionsList == null) {
			logger.error("There are no divisions for selected customer");
			return "";
		}
		Iterator<RpsDivision> it = rpsDivisionsList.iterator();

		// populate list of rps divsion info
		while (it.hasNext()) {
			RpsDivisionInfo rpsDivisionInfo = new RpsDivisionInfo();
			RpsDivision rpsDivision = it.next();

			rpsDivisionInfo.setDivisionCode(rpsDivision.getDivisionCode());
			rpsDivisionInfo.setDivisionName(rpsDivision.getDivisionName());

			rpsDivisionsInfoList.add(rpsDivisionInfo);
		}
		String divisions = gson.toJson(rpsDivisionsInfoList);
		return divisions;
	}

	public String getAllEventsByDivision(String divisionCode) {
		// create list of RpsEventInfo
		List<RpsEventInfo> rpsEventsInfoList = new ArrayList<RpsEventInfo>();
		Gson gson = new Gson();
		// get RpsEvent list per customer code from database
		List<RpsEvent> rpsEventsList = rpsEventService.getAllRpsEventsPerDivisionCode(divisionCode);
		if (rpsEventsList == null) {
			logger.error("There are no events for selected division");
			return "";
		}
		Iterator<RpsEvent> it = rpsEventsList.iterator();

		// populate list of rps Event info
		while (it.hasNext()) {
			RpsEventInfo rpsEventInfo = new RpsEventInfo();
			RpsEvent rpsEvent = it.next();

			rpsEventInfo.setEventCode(rpsEvent.getEventCode());
			rpsEventInfo.setEventName(rpsEvent.getEventName());
			// convert date to string format
			String startDate = convertToSpecificFormat(rpsEvent.getEventStartDate());
			String endDate = convertToSpecificFormat(rpsEvent.getEventEndDate());
			rpsEventInfo.setEventStartDate(startDate);
			rpsEventInfo.setEventEndDate(endDate);

			rpsEventsInfoList.add(rpsEventInfo);
		}

		String events = gson.toJson(rpsEventsInfoList);
		return events;
	}

	private String convertToSpecificFormat(Calendar date) {
		String tempDate = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT);

		if (date != null) {
			tempDate = sdf.format(date.getTime());
		}
		return tempDate;
	}

	@Transactional(readOnly = true)
	public List<RpsBatch> getAllBatchesInDateRange(String eventCode, String startDate, String endDate) {
		List<RpsBatch> rpsBatchList = null;
		if (startDate == null || endDate == null || startDate.equals("") && endDate.equals("")) {

			rpsBatchList = rpsBatchService.getAllRpsBatchForDateRange(eventCode);
		} else {
			// convert string dates to calendar objects
			Calendar batchStartDate = Calendar.getInstance();
			Calendar batchEndDate = Calendar.getInstance();
			try {
				String dateTimeFormat = RpsConstants.DATE_FORMAT_WITH_TIME;
				batchStartDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(startDate));
				batchEndDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(endDate));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			// get RpsEvent list per customer code from database
			rpsBatchList = rpsBatchService.getAllRpsBatchForDateRange(eventCode, batchStartDate, batchEndDate);
		}

		return rpsBatchList;

	}

	public List<Integer> getAllBatchesIdsList(String eventCode, String startDate, String endDate) {
		List<Integer> rpsBatchList = null;
		if (startDate == null || endDate == null || startDate.equals("") && endDate.equals("")) {

			rpsBatchList = rpsBatchService.getBatchIdsByEventCode(eventCode);
		} else {
			// convert string dates to calendar objects
			Calendar batchStartDate = Calendar.getInstance();
			Calendar batchEndDate = Calendar.getInstance();
			try {
				String dateTimeFormat = RpsConstants.DATE_FORMAT_WITH_TIME;
				batchStartDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(startDate));
				batchEndDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(endDate));
			} catch (ParseException e) {
				e.printStackTrace();
			}

			// get RpsEvent list per customer code from database
			rpsBatchList = rpsBatchService.getBatchIdsByEventCode(eventCode, batchStartDate, batchEndDate);
		}

		return rpsBatchList;

	}

	private Integer getIntegerVersionNo(RpsPack rpsPack) {

		String version = rpsPack.getVersionNumber();
		Integer versionNo = null;
		try {
			versionNo = Integer.parseInt(version);
		} catch (Exception e) {
			versionNo = 0;
		}
		return versionNo;
	}

	public void updateComponentStatusMap(Map<String, String> componentStatusMap, RpsPack rpsPack,
			PackStatusInfoRow packStatusInfoRow) {

		List<RpsRpackComponent> rpackComponentsList = rpsRpackComponentService.getAllRpackComponents(rpsPack);
		String packsReceived = RpsConstants.packStatus.PACKS_RECEIVED.toString();
		String packType = this.getReadablePackTypes(rpsPack.getPackSubType());
		if (rpackComponentsList != null) {
			Iterator<RpsRpackComponent> rpackComponentIterator = rpackComponentsList.iterator();
			// iterate through Rpack components and set status
			while (rpackComponentIterator.hasNext()) {
				RpsRpackComponent rpComponent = rpackComponentIterator.next();
				String componentName = rpComponent.getRpackComponentName();
				// set component status for tool tip only when component is
				// received
				if (packsReceived.equalsIgnoreCase(rpComponent.getStatus())) {
					if (RpsConstants.RpackComponents.CANDIDATE_LOG.toString().equalsIgnoreCase(componentName)) {
						Set<String> candResStatusToolTipSet = packStatusInfoRow.getCandResStatusToolTip();
						candResStatusToolTipSet.add(packType);
						packStatusInfoRow.setCandAuditStausToolTip(candResStatusToolTipSet);
					}

					if (RpsConstants.RpackComponents.ADMIN_LOG.toString().equalsIgnoreCase(componentName)) {
						Set<String> adminAuditStatusToolTipSet = packStatusInfoRow.getAdminAuditStatusToolTip();
						adminAuditStatusToolTipSet.add(packType);
						packStatusInfoRow.setAdminAuditStatusToolTip(adminAuditStatusToolTipSet);
					}

					if (RpsConstants.RpackComponents.ATTENDANCE_JSON.toString().equalsIgnoreCase(componentName)) {
						Set<String> attendanceReportStatusToolTipSet = packStatusInfoRow
								.getAttendanceReportStatusToolTip();
						attendanceReportStatusToolTipSet.add(packType);
						packStatusInfoRow.setAttendanceReportStatusToolTip(attendanceReportStatusToolTipSet);
					}

					if (RpsConstants.RpackComponents.RESPONSE_JSON.toString().equalsIgnoreCase(componentName)) {
						Set<String> candResStatusToolTipSet = packStatusInfoRow.getCandResStatusToolTip();
						candResStatusToolTipSet.add(packType);
						packStatusInfoRow.setCandResStatusToolTip(candResStatusToolTipSet);
					}
				}

				String status = componentStatusMap.get(componentName);
				if (status == null) {
					status = rpComponent.getStatus();
				} else {
					status = getComponentStatus(status, rpComponent.getStatus());
				}
				componentStatusMap.put(rpComponent.getRpackComponentName(), status);

			}
		}

	}

	private String getReadablePackTypes(String packSubType) {

		String packType = "";
		switch (packSubType) {
		case "AUTOMATEDRPACK":
			packType = "Automated Rpack";
			break;

		case "AUTOMATEDDELTARPACK":
			packType = "Automated Delta-Rpack";
			break;

		case "MANUALRPACK":
			packType = "Manual Rpack";
			break;

		}
		return packType;
	}

	private String getComponentStatus(String status1, String status2) {
		String status = "";
		if (status1.equalsIgnoreCase(RpsConstants.packStatus.PACKS_RECEIVED.toString())
				|| status2.equalsIgnoreCase(RpsConstants.packStatus.PACKS_RECEIVED.toString()))
			status = RpsConstants.packStatus.PACKS_RECEIVED.toString();
		else if (status1.equalsIgnoreCase(RpsConstants.packStatus.UNKNOWN.toString())
				&& status2.equalsIgnoreCase(RpsConstants.packStatus.UNKNOWN.toString()))
			status = RpsConstants.packStatus.UNKNOWN.toString();
		else if (status1.equalsIgnoreCase(RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString())
				&& status2.equalsIgnoreCase(RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString()))
			status = RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString();
		else if ((status1.equalsIgnoreCase(RpsConstants.packStatus.UNKNOWN.toString())
				&& status2.equals(RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString()))
				|| (status1.equalsIgnoreCase(RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString())
						&& status2.equals(RpsConstants.packStatus.UNKNOWN.toString())))
			status = RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString();
		return status;
	}

	public void updateRPackComponentStatus(RpsPack rpsPack, PackStatusInfoRow packStatusInfoRow) {
		List<RpsRpackComponent> rpsRpackComponentsList = rpsRpackComponentService.getAllRpackComponents(rpsPack);
		Iterator<RpsRpackComponent> componentIterator = rpsRpackComponentsList.iterator();
		while (componentIterator.hasNext()) {
			RpsRpackComponent rpsRpackComponent = componentIterator.next();
			String componentName = rpsRpackComponent.getRpackComponentName();
			if (componentName == null || componentName.isEmpty())
				continue;
			componentName = componentName.toUpperCase();
			if (componentName.equals(RpsConstants.RpackComponents.ATTENDANCE_JSON.toString())) {
				packStatusInfoRow.setAttendanceReportStatus(rpsRpackComponent.getStatus());
			}
			if (componentName.equals(RpsConstants.RpackComponents.RESPONSE_JSON.toString())) {
				packStatusInfoRow.setCandResStatus(rpsRpackComponent.getStatus());
			}
			if (componentName.equals(RpsConstants.RpackComponents.ADMIN_LOG.toString())) {
				packStatusInfoRow.setAdminAuditStatus(rpsRpackComponent.getStatus());
			}
			if (componentName.equals(RpsConstants.RpackComponents.CANDIDATE_LOG.toString())) {
				packStatusInfoRow.setCandAuditStaus(rpsRpackComponent.getStatus());
			}

		}

	}

	public String getAllQPackStatusInDateRange(String eventCode, String startDate, String endDate, Integer pageNo,
			Integer size) {
		Gson gson = new Gson();
		// create set of QPackStatusInfoRows
		Set<QPackStatusInfoRow> qPackStatusInfoRowSet = new LinkedHashSet<QPackStatusInfoRow>();

		List<RpsBatch> rpsBatchList = null;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesInDateRange(eventCode, startDate, endDate);
		if (rpsBatchList == null) {
			logger.error("There are no batches for event and selected date range");
			return "";
		}
		Iterator<RpsBatch> batchIterator = rpsBatchList.iterator();
		while (batchIterator.hasNext()) {
			RpsBatch rpsBatch = batchIterator.next();
			List<RpsQuestionPaper> rpsQuestionPaperList = rpsQuestionPaperService
					.getAllQuestionPapersPerBatch(rpsBatch);
			if (rpsQuestionPaperList != null) {
				Iterator<RpsQuestionPaper> qpIterator = rpsQuestionPaperList.iterator();
				while (qpIterator.hasNext()) {
					RpsQuestionPaper rpsQuestionPaper = qpIterator.next();
					QPackStatusInfoRow qPackStatusInfoRow = new QPackStatusInfoRow();
					qPackStatusInfoRow.setEventCode(eventCode);
					qPackStatusInfoRow.setQpaperName(rpsQuestionPaper.getQpCode());
					qPackStatusInfoRow.setQpaperStatus(rpsQuestionPaper.getQpStatus());
					String answerKeyStatus = "";
					// long count=0;
					if (rpsQuestionPaper.isIsAnswerKeyAvailable()) { // when
						// answerkey
						// is
						// available
						// then
						// check
						// for
						// how
						// many
						// questions
						// it is
						// available
						/*
						 * count= rpsQuestionService.getAnswerKeyStatus(
						 * rpsQuestionPaper ); //Integer diff=
						 * Integer.parseInt(s); if(count>0)
						 */
						answerKeyStatus = RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString();
					} else
						answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();
					qPackStatusInfoRow.setAnswerKeyStatus(answerKeyStatus);
					qPackStatusInfoRowSet.add(qPackStatusInfoRow);
				} // close while for qpIterator
			} // close if for rpsQuestionPaperList not null

		}
		String qpackRow = gson.toJson(qPackStatusInfoRowSet);
		return qpackRow;
	}

	/*
	 * public void getQPaperforQPID(final String qpId, String assessmentCode) {
	 * Map<String, String> pack = new HashMap<>(); pack.put("qpId", qpId);
	 * qPackPerQPIDService.downloadQpackFile(pack, assessmentCode); }
	 */

	/*
	 * public void getQPaperForGlobalAction(String[] qpIdArr, String
	 * assessmentCode) { for (String qpId : qpIdArr) { if (qpId != null &&
	 * !qpId.isEmpty()) { this.getQPaperforQPID(qpId, assessmentCode); } } }
	 */

	public String getAllAnswerKeysStatus(String eventCode, String startDate, String endDate, Integer pageNo,
			Integer size, String sortOrder, String sortColumnName) {
		Gson gson = new Gson();
		// get the event description
		RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
		// create set of AnswerKeyStatusInfoRow
		Set<AnswerKeyStatusInfoRow> answerKeyStatusInfoRowSet = new LinkedHashSet<AnswerKeyStatusInfoRow>();
		List<RpsBatch> rpsBatchList = null;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesInDateRange(eventCode, startDate, endDate);
		if (rpsBatchList == null) {
			logger.error("There are batches for event and selected date range");
			return "";
		}
		Iterator<RpsBatch> batchIterator = rpsBatchList.iterator();
		while (batchIterator.hasNext()) {
			RpsBatch rpsBatch = batchIterator.next();
			List<RpsAssessment> rpsAssessmentList = rpsMasterAssociationService.getAssesmentsByBatch(rpsBatch);
			if (rpsAssessmentList != null) {
				Iterator<RpsAssessment> assessmentIterator = rpsAssessmentList.iterator();
				while (assessmentIterator.hasNext()) {
					RpsAssessment rpsAssessment = assessmentIterator.next();
					AnswerKeyStatusInfoRow answerKeyStatusInfoRow = new AnswerKeyStatusInfoRow();
					answerKeyStatusInfoRow.setEventCode(eventCode);
					answerKeyStatusInfoRow.setSubjectCode(rpsAssessment.getAssessmentCode());
					answerKeyStatusInfoRow.setSubjectName(rpsAssessment.getAssessmentName());
					answerKeyStatusInfoRow.setDescription(rpsEvent.getEventName());
					String answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();

					// get List of question papers for the assessment
					List<RpsQuestionPaper> qpList = rpsQuestionPaperService
							.getAllQuestionPapersPerAssessment(rpsAssessment);
					if (qpList != null) {
						Iterator<RpsQuestionPaper> qpIt = qpList.iterator();
						RpsQuestionPaper rpsQuestionPaper = qpIt.next();
						boolean isAnswerKeysAvailable = rpsQuestionPaper.isIsAnswerKeyAvailable();
						while (qpIt.hasNext()) {
							// apply AND operation for answer keys against all
							// Assessments
							rpsQuestionPaper = qpIt.next();
							isAnswerKeysAvailable = isAnswerKeysAvailable && rpsQuestionPaper.isIsAnswerKeyAvailable();
						}
						if (isAnswerKeysAvailable)
							answerKeyStatus = RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString();
					} // close if for qpList is not null
					answerKeyStatusInfoRow.setAnswerKeyStatus(answerKeyStatus);
					// add answerKeyStatusInfoRow to set
					answerKeyStatusInfoRowSet.add(answerKeyStatusInfoRow);
				} // close assessmentIterator while loop
			} // close if rpsAssessmentList is not null
		} // close batchIterator while loop

		String answerKeys = gson.toJson(answerKeyStatusInfoRowSet);
		return answerKeys;
	}

	public void computeResultForTheBatchAndQp(List<ResultParams> resultsParameters) {
		String argsJson = null;

	}

	/*
	 * public void setErrorStatus(String code, JsonFromRpsUI jsonFromRpsUI) {
	 * String s= RpsConstants.ERROR_CODE.get(code); String[] array=
	 * s.split(":"); jsonFromRpsUI.setErrorCode(code);
	 * jsonFromRpsUI.setErrorMsg(array[0]); jsonFromRpsUI.setStatus(array[1]); }
	 */
	// ----------------------------------------------
	@Transactional(readOnly = true)
	public String getAllQPackStatusInDateRange(List<RpsEvent> rpsEventList, String startDate, String endDate,
			Integer pageNo, Integer size, String sortingOrder, String sortColumnName) {
		Gson gson = new Gson();
		// create set of QPackStatusInfoRows
		Set<QPackStatusInfoRow> qPackStatusInfoRowSet = new LinkedHashSet<QPackStatusInfoRow>();

		List<Integer> rpsBatchList = null;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesInDateRange(rpsEventList, startDate, endDate);
		if (rpsBatchList == null || rpsBatchList.isEmpty()) {
			logger.error("There are no batches for event and selected date range");
			return gson.toJson(rpsBatchList);
		}

		Sort sort = null;
		if (sortingOrder.equalsIgnoreCase(ASC)) {
			sort = new Sort(new Order(Direction.ASC, questionPaperName));
		} else
			sort = new Sort(new Order(Direction.DESC, questionPaperName));

		List<RpsQuestionPaperLite> rpsQuestionPaperList = rpsQuestionPaperService
				.getAllQuestionPapersPerBatches(rpsBatchList, new PageRequest(pageNo, size, sort));

		Object object = rpsQuestionPaperService.getTotalRecordsPerBatches(rpsBatchList);
		Long totalRecords = (Long) object;
		if (rpsQuestionPaperList != null) {
			Iterator<RpsQuestionPaperLite> qpIterator = rpsQuestionPaperList.iterator();
			while (qpIterator.hasNext()) {
				RpsQuestionPaperLite rpsQuestionPaper = qpIterator.next();
				QPackStatusInfoRow qPackStatusInfoRow = new QPackStatusInfoRow();
				// String eventCode=
				// rpsEventService.findEventCodeByBatch(rpsBatch);
				// qPackStatusInfoRow.setEventCode(eventCode);
				qPackStatusInfoRow.setQpaperName(rpsQuestionPaper.getQpCode());
				qPackStatusInfoRow.setAssessmentCode(rpsQuestionPaper.getAssessmentCode());
				qPackStatusInfoRow.setQpaperStatus(rpsQuestionPaper.getQpStatus());
				String answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();
				long count = 0;
				qPackStatusInfoRow.setQuestionsCount(this.getChildQuestionsCount(rpsQuestionPaper));

				List<Integer> qids = rpsQuestionAssociationService.getQuestionsIdsForQp(rpsQuestionPaper.getQpId());
				if (qids != null && qids.size() > 0)
					count = rpsQuestionService.getAnswerKeyStatus(qids,null);

				// count =
				// rpsQuestionService.getAnswerKeyStatus(rpsQuestionPaper.getQpId());
				qPackStatusInfoRow.setAnswerKeysCount(count);
				if (qPackStatusInfoRow.getQuestionsCount() == qPackStatusInfoRow.getAnswerKeysCount()
						&& qPackStatusInfoRow.getAnswerKeysCount() > 0)
					answerKeyStatus = RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString();
				else
					answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();
				qPackStatusInfoRow.setAnswerKeyStatus(answerKeyStatus);
				qPackStatusInfoRowSet.add(qPackStatusInfoRow);
			} // close while for qpIterator
		} // close if for rpsQuestionPaperList not null

		JSONObject jsonObject = new JSONObject();
		String qpackRow = gson.toJson(qPackStatusInfoRowSet);
		jsonObject.put(RpsConstants.totalRecords, totalRecords);
		jsonObject.put("data", qpackRow);
		return jsonObject.toJSONString();
	}

	@Transactional(readOnly = true)
	public String getAllQPackStatusInDateRangeUsingEvent(List<RpsEvent> rpsEventList, String startDate, String endDate,
			Integer pageNo, Integer size, String sortingOrder, String sortColumnName) {
		Gson gson = new Gson();
		// create set of QPackStatusInfoRows
		Set<QPackStatusInfoRow> qPackStatusInfoRowSet = new LinkedHashSet<QPackStatusInfoRow>();
		List<String> eventCodeList = new ArrayList<>();
		for (RpsEvent rpsEvent : rpsEventList)
			eventCodeList.add(rpsEvent.getEventCode());
		/**
		 * qp.qpId, qp.qpCode, qp.assessmentCode, qp.qpStatus,
		 */
		List<Object[]> objectList = rpsQuestionService.findQpPackAvailabilityByEventCode(eventCodeList);
		for (Object[] dataOfQp : objectList) {

			QPackStatusInfoRow qPackStatusInfoRow = new QPackStatusInfoRow();
			qPackStatusInfoRow.setQpaperName(String.valueOf(dataOfQp[1]));
			qPackStatusInfoRow.setAssessmentCode(String.valueOf(dataOfQp[2]));
			qPackStatusInfoRow.setQpaperStatus(String.valueOf(dataOfQp[3]));
			String answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.name();
			long count = Integer.valueOf(String.valueOf(dataOfQp[5]));
			qPackStatusInfoRow.setQuestionsCount(Integer.valueOf(String.valueOf(dataOfQp[4])));
			qPackStatusInfoRow.setAnswerKeysCount(count);
			if (qPackStatusInfoRow.getQuestionsCount() == qPackStatusInfoRow.getAnswerKeysCount()
					&& qPackStatusInfoRow.getAnswerKeysCount() > 0)
				answerKeyStatus = RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString();
			else
				answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();
			qPackStatusInfoRow.setAnswerKeyStatus(answerKeyStatus);
			qPackStatusInfoRowSet.add(qPackStatusInfoRow);
		} // close if for rpsQuestionPaperList not null

		JSONObject jsonObject = new JSONObject();
		String qpackRow = gson.toJson(qPackStatusInfoRowSet);
		jsonObject.put(RpsConstants.totalRecords, objectList.size());
		jsonObject.put("data", qpackRow);
		return jsonObject.toJSONString();
	}

	private long getChildQuestionsCount(RpsQuestionPaperLite rpsQuestionPaper) {
		long count = 0;
		List<RpsQuestion> rpsQuestions = rpsQuestionAssociationService
				.getParentQuestionsForQp(rpsQuestionPaper.getQpId());
		Set<String>questionSet=new HashSet<>();
		if (rpsQuestions != null && !rpsQuestions.isEmpty()) {
			for (RpsQuestion rpsQuestion : rpsQuestions) {
				if (rpsQuestion.getQuestionType()
						.equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())
						|| rpsQuestion.getQuestionType()
								.equalsIgnoreCase(QuestionType.DESCRIPTIVE_QUESTION.toString()))
					questionSet.add(rpsQuestion.getQuestId());
			}
		}
		return questionSet.size();
	}

	@Override
	public String getAllAnswerKeysStatus(List<RpsEvent> rpsEventList, String startDate, String endDate, Integer pageNo,
			Integer size, String sortOrder, String sortColumnName) {

		Gson gson = new Gson();

		// get the event description
		// List<RpsEvent> rpsEvent=
		// rpsEventService.getrpsEventDetails(rpsEventList);
		// create set of AnswerKeyStatusInfoRow
		Set<AnswerKeyStatusInfoRow> answerKeyStatusInfoRowSet = new LinkedHashSet<AnswerKeyStatusInfoRow>();
		List<Integer> rpsBatchList = null;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesInDateRange(rpsEventList, startDate, endDate);
		if (rpsBatchList == null || rpsBatchList.isEmpty()) {
			logger.error("There are batches for event and selected date range");
			return gson.toJson(rpsBatchList);
		}
		// get list of all assessments for the batch using pagination
		Sort sort = null;
		if (sortColumnName.equalsIgnoreCase(subjectCode)) {
			String assessmentCode = answerKeyStatusColumn.subjectCode.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, assessmentCode);
			} else {
				sort = new Sort(Direction.DESC, assessmentCode);
			}
		} else if (sortColumnName.equalsIgnoreCase(subjectName)) {
			String assessmentCode = answerKeyStatusColumn.subjectName.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, assessmentCode);
			} else {
				sort = new Sort(Direction.DESC, assessmentCode);
			}
		}

		List<RpsAssessment> rpsAssessmentList = rpsMasterAssociationService.getAllAssessmentsByBatchId(rpsBatchList,
				new PageRequest(pageNo, size, sort));

		Long totalRecords = rpsMasterAssociationService.getTotalAssessmentsByBatchId(rpsBatchList);

		if (rpsAssessmentList == null || rpsAssessmentList.isEmpty()) {
			return gson.toJson(rpsAssessmentList);
		}

		Map<String, RpsEvent> assessmentEventMap = new HashMap<>();
		Map<String, Boolean> qpCodeHashMap = new HashMap<String, Boolean>();
		if (rpsAssessmentList != null) {
			Iterator<RpsAssessment> assessmentIterator = rpsAssessmentList.iterator();
			while (assessmentIterator.hasNext()) {
				RpsAssessment rpsAssessment = assessmentIterator.next();
				RpsEvent rpsEvent = assessmentEventMap.get(rpsAssessment.getAssessmentCode());
				if (rpsEvent == null) {
					rpsEvent = rpsAssessmentService.getEventByAssessment(rpsAssessment);
					assessmentEventMap.put(rpsAssessment.getAssessmentCode(), rpsEvent);
				}
				AnswerKeyStatusInfoRow answerKeyStatusInfoRow = new AnswerKeyStatusInfoRow();
				answerKeyStatusInfoRow.setSubjectCode(rpsAssessment.getAssessmentCode());
				answerKeyStatusInfoRow.setSubjectName(rpsAssessment.getAssessmentName());
				answerKeyStatusInfoRow.setEventCode(rpsEvent.getEventCode());
				answerKeyStatusInfoRow.setDescription(rpsEvent.getEventName());
				// get List of question papers for the assessment
				List<RpsQuestionPaper> qpList = rpsQuestionPaperService
						.getAllQuestionPapersPerAssessment(rpsAssessment);
				String answerKeyStatus = RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString();
				long answerKeysCount = 0;
				long quesPaperCount = 0;
				long qAnsCount = 0;
				boolean isAnswerKeyAvailable;
				if (qpList != null && !qpList.isEmpty()) {

					Iterator<RpsQuestionPaper> qpIt = qpList.iterator();
					while (qpIt.hasNext()) {
						RpsQuestionPaper rpsQuestionPaper = qpIt.next();

						if (qpCodeHashMap.get(rpsQuestionPaper.getQpCode()) == null) {
							qpCodeHashMap.put(rpsQuestionPaper.getQpCode(), true);
						} else {
							// if qpCode is already considered continue for next
							// question paper
							continue;
						}
						quesPaperCount++;
						AnswerStatus answerStatus = this.getAnswerKeyStatusForQPaper(rpsQuestionPaper);
						isAnswerKeyAvailable = answerStatus.isAnswerKeyAvailable();
						qAnsCount = qAnsCount + answerStatus.getqAnsCount();
						if (isAnswerKeyAvailable)
							answerKeysCount++;
					}
				} // close if for qpList is not null

				if (answerKeysCount == quesPaperCount && answerKeysCount > 0)
					answerKeyStatus = RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString();

				// set the button status, if atleast one answer key is available
				if (qAnsCount > 0)
					answerKeyStatusInfoRow.setUploadStatus(RpsConstants.AVAILABLE_STATUS.AVAILABLE.toString());
				else
					answerKeyStatusInfoRow.setUploadStatus(RpsConstants.AVAILABLE_STATUS.NOT_AVAILABLE.toString());

				answerKeyStatusInfoRow.setQuestionPapersCount(quesPaperCount);
				answerKeyStatusInfoRow.setAnswerKeysCount(answerKeysCount);
				answerKeyStatusInfoRow.setAnswerKeyStatus(answerKeyStatus);
				// generateScore only in case of cron complete
				answerKeyStatusInfoRow.setGenerateScore(rpsAssessment.isEnableManualCronTrigger());
				//
				// add answerKeyStatusInfoRow to set
				answerKeyStatusInfoRowSet.add(answerKeyStatusInfoRow);
			} // close assessmentIterator while loop
		} // close if rpsAssessmentList is not null

		String answerKeys = gson.toJson(answerKeyStatusInfoRowSet);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(RpsConstants.totalRecords, totalRecords);
		jsonObject.put("data", answerKeys);

		return jsonObject.toJSONString();

	}

	private AnswerStatus getAnswerKeyStatusForQPaper(RpsQuestionPaper rpsQuestionPaper) {

		boolean isAnswerKeyAvailable = false;
		Set<Integer>qCount=new HashSet<>();
		Set<Integer>aCount=new HashSet<>();
		// List<RpsQuestion> rpsQuestions = rpsQuestionAssociationService
		// .getQuestionsFromQp(rpsQuestionPaper);

		List<RpsQuestion> rpsQuestions = rpsQuestionAssociationService
				.getQuestionsLiteForQp(rpsQuestionPaper.getQpId());

		if (rpsQuestions != null && !rpsQuestions.isEmpty()) {
			for (RpsQuestion rpsQuestion : rpsQuestions) {
				if (rpsQuestion.getQuestionType()
						.equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())
						|| rpsQuestion.getQuestionType()
								.equalsIgnoreCase(QuestionType.DESCRIPTIVE_QUESTION.toString()))
					qCount.add(rpsQuestion.getQid());

				if (rpsQuestion.getQans() != null && !rpsQuestion.getQans().isEmpty())
					aCount.add(rpsQuestion.getQid());
			}
		}

		if ((aCount.size() == qCount.size()) && aCount.size() > 0)
			isAnswerKeyAvailable = true;

		AnswerStatus answerStatus = new AnswerStatus(isAnswerKeyAvailable, aCount.size());
		return answerStatus;
	}

	@Override
	public String getAllProcessResultsStatus(List<RpsEvent> eventCodes, String startDate, String endDate) {

		Gson gson = new Gson();
		// create set of ProcessResultInfoRow
		Set<ProcessResultInfoRow> processResultInfoRowList = new LinkedHashSet<>();
		List<Integer> rpsBatchList = null;
		// get all batches for the event in date range
		logger.info("fetching batch list for all events--IN--");
		rpsBatchList = getAllBatchesInDateRange(eventCodes, startDate, endDate);
		logger.info("fetching batch list for all events--OUT--");

		if (rpsBatchList == null || rpsBatchList.isEmpty()) {
			logger.error("There are no batches for event and selected date range");
			return EMPTY_JSON;
		}

		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationList = rpsBatchAcsAssociationService
				.getAssociationsListByBatchIDList(rpsBatchList);

		if (rpsBatchAcsAssociationList == null || rpsBatchAcsAssociationList.isEmpty()) {
			logger.error("There are no acsServers batch association for the selected date range");
			return EMPTY_JSON;
		}

		Iterator<RpsBatchAcsAssociation> assnIt = rpsBatchAcsAssociationList.iterator();
		while (assnIt.hasNext()) {
			RpsBatchAcsAssociation rpsBatchAcsAssociation = assnIt.next();
			RpsBatch rpsBatch = rpsBatchAcsAssociation.getRpsBatch();
			RpsAcsServer rpsAcsServer = rpsBatchAcsAssociation.getRpsAcsServer();
			// get list of rpacks per batch and acserserver
			logger.info("fetching pack list for each batcha and acs--IN--");
			List<RpsPack> rpsPackList = rpsPackService.getAllRpsPackByAssociation(rpsBatchAcsAssociation);
			if (rpsPackList != null && !rpsPackList.isEmpty()) {
				logger.info("There are no packs for acs server and batch combination");
				String rpackStatus1 = "";
				String rpackStatus2 = "";
				String rpackStatus = "";
				Iterator<RpsPack> rpackIterator = rpsPackList.iterator();
				RpsRpackComponent component = null;
				logger.info("While loop, rpack--- IN---");
				while (rpackIterator.hasNext()) {

					RpsPack rpsPack = rpackIterator.next();
					if (rpsPack.getPackType().equalsIgnoreCase(RpsConstants.PackType.RPACK.toString())) {
						rpackStatus1 = rpsPack.getPackStatus();
						component = rpsRpackComponentService.getRpsRpackComponentByRpackAndType(rpsPack,
								RpsConstants.RpackComponents.RESPONSE_JSON.toString());
						if (component != null) {
							if (!component.getStatus().equals(rpackStatus1))
								rpackStatus1 = RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString();
						}
					}
					if (rpsPack.getPackSubType().equalsIgnoreCase(PackSubTypeEnum.AutomatedDeltaRpack.toString())) {
						rpackStatus2 = rpsPack.getPackStatus();
						component = rpsRpackComponentService.getRpsRpackComponentByRpackAndType(rpsPack,
								RpsConstants.RpackComponents.RESPONSE_JSON.toString());
						if (component != null) {
							if (!component.getStatus().equals(rpackStatus1))
								rpackStatus2 = RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString();
						}
					}
				}
				logger.info("While loop, rpack--- OUT---");
				// there is only one Rpack
				if (rpackStatus1.equalsIgnoreCase(RpsConstants.packStatus.UNPACKED.toString())
						&& rpackStatus2.equals(""))
					rpackStatus = RpsConstants.packStatus.PACKS_RECEIVED.toString();
				// there are both rpack and delta rpack
				else if (rpackStatus1.equalsIgnoreCase(RpsConstants.packStatus.UNPACKED.toString())
						&& rpackStatus2.equalsIgnoreCase(RpsConstants.packStatus.UNPACKED.toString()))
					rpackStatus = RpsConstants.packStatus.PACKS_RECEIVED.toString();
				else
					rpackStatus = RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString();
				// get venue name for acs server
				RpsVenue rpsVenue = rpsAcsServerServices.getVenueByAcsServer(rpsAcsServer);

				// get list of assessments per acsSever and batchID
				logger.info("fetching ASSESSMENT list for all events--IN--");
				List<RpsAssessment> rpsAssessmentList = rpsMasterAssociationService
						.getAllAssessmentsBatchAcsAssociation(rpsBatchAcsAssociation);
				logger.info("fetching ASSESSMENT list for all events--OUT--");

				Iterator<RpsAssessment> assessmentIterator = rpsAssessmentList.iterator();
				while (assessmentIterator.hasNext()) {
					RpsAssessment rpsAssessment = assessmentIterator.next();
					// get list of questionpapers per assessments
					List<RpsQuestionPaper> rpsQuestionPaperList = rpsQuestionPaperService
							.getAllQuestionPapersPerAssessment(rpsAssessment);
					if (rpsQuestionPaperList == null) {
						logger.info("There is no questionPaper available for the assessment");
						break;
					}
					Iterator<RpsQuestionPaper> qpIterator = rpsQuestionPaperList.iterator();
					while (qpIterator.hasNext()) {
						RpsQuestionPaper rpsQuestionPaper = qpIterator.next();
						// create ProcessResultInfoRow
						ProcessResultInfoRow processResultInfoRow = new ProcessResultInfoRow();
						String eventCode = rpsEventService.findEventCodeByBatch(rpsBatch);
						processResultInfoRow.setEventCode(eventCode);
						processResultInfoRow.setBatchCode(rpsBatch.getBatchCode());
						processResultInfoRow.setBatchName(rpsBatch.getBatchName());
						processResultInfoRow.setAcsServerId(rpsAcsServer.getAcsServerId());
						if (rpsAcsServer.getAcsServerName() == null || rpsAcsServer.getAcsServerName().isEmpty())
							processResultInfoRow.setAcsServerName("");
						else
							processResultInfoRow.setAcsServerName(rpsAcsServer.getAcsServerName());
						processResultInfoRow.setVenue(rpsVenue.getVenueName());
						processResultInfoRow.setRpackStatus(rpackStatus);
						processResultInfoRow.setAssessmentName(rpsAssessment.getAssessmentName());
						// processResultInfoRow.setSetCode(rpsQuestionPaper.getSetCode());
						processResultInfoRow.setSetCode(rpsQuestionPaper.getQpCode());
						RpsCumulativeResponses responses = rpsCumulativeResponsesService
								.getResponseByUniqueIDs(rpsBatchAcsAssociation, rpsAssessment, rpsQuestionPaper);
						if (responses != null) {
							if (responses.getIsResultComputed() == null
									|| responses.getIsResultComputed().length() == 0)
								processResultInfoRow.setIsScoreCalculated(
										RpsConstants.RESULT_COMPUTE_STATUS.RESULT_NOT_COMPUTED.toString());
							else
								processResultInfoRow.setIsScoreCalculated(responses.getIsResultComputed());
							processResultInfoRowList.add(processResultInfoRow);
						}
					}
				} // while
			} // if
		} // while outer

		String processResults = gson.toJson(processResultInfoRowList);
		return processResults;
	}

	public String getAllPackStatusInDateRange(List<RpsEvent> eventCodes, String startDate, String endDate,
			Integer pageNo, Integer size, String sortingOrder, String columnName) {

		Gson gson = new Gson();

		List<Integer> rpsBatchList = null;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesInDateRange(eventCodes, startDate, endDate);
		if (rpsBatchList == null || rpsBatchList.isEmpty()) {
			logger.error("BatchList is empty for the Event selection");
			return EMPTY_JSON;
		}
		// get rpsBatchAcsAssociation list
		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationList = null;
		if (sortingOrder == null && columnName == null) {
			rpsBatchAcsAssociationList = rpsBatchAcsAssociationService.getAssociationsListByBatchIDList(rpsBatchList,
					new PageRequest(pageNo, size));
		} else {
			Sort sort = null;
			;
			if (columnName.equalsIgnoreCase(batchCode)) {
				if (sortingOrder.equalsIgnoreCase(ASC)) {
					sort = new Sort(new Order(Direction.ASC, dashBoardColumnName.batchCode.getColumnName()));
				} else
					sort = new Sort(new Order(Direction.DESC, dashBoardColumnName.batchCode.getColumnName()));

			} else if (columnName.equalsIgnoreCase(batchName)) {
				if (sortingOrder.equalsIgnoreCase(ASC)) {
					sort = new Sort(new Order(Direction.ASC, dashBoardColumnName.batchName.getColumnName()));
				} else
					sort = new Sort(new Order(Direction.DESC, dashBoardColumnName.batchName.getColumnName()));

			} else if (columnName.equalsIgnoreCase(acsServerId)) {
				if (sortingOrder.equalsIgnoreCase(ASC)) {
					sort = new Sort(new Order(Direction.ASC, dashBoardColumnName.acsServerId.getColumnName()));
				} else
					sort = new Sort(new Order(Direction.DESC, dashBoardColumnName.acsServerId.getColumnName()));

			} else if (columnName.equalsIgnoreCase(acsServerName)) {
				if (sortingOrder.equalsIgnoreCase(ASC)) {
					sort = new Sort(new Order(Direction.ASC, dashBoardColumnName.acsServerName.getColumnName()));
				} else
					sort = new Sort(new Order(Direction.DESC, dashBoardColumnName.acsServerName.getColumnName()));

			}
			rpsBatchAcsAssociationList = rpsBatchAcsAssociationService.getAssociationsListByBatchIDList(rpsBatchList,
					new PageRequest(pageNo, size, sort));
		}

		Object object = rpsBatchAcsAssociationService.getTotalCountByBatchList(rpsBatchList);
		Long totalRecords = (Long) object;
		if (rpsBatchAcsAssociationList == null || rpsBatchAcsAssociationList.isEmpty()) {
			logger.error("There are no ACSServers Mapped in RpsBatchAcsAssociation table against the batches");
			return EMPTY_JSON;
		}
		// maintain hashMap of batchCode and eventCode
		Map<String, RpsEvent> batchEventMap = new HashMap<>();
		// create Set of packStatusInfoRow
		Set<PackStatusInfoRow> packStatusInfoRowSet = new LinkedHashSet<PackStatusInfoRow>();
		Iterator<RpsBatchAcsAssociation> assnIt = rpsBatchAcsAssociationList.iterator();
		while (assnIt.hasNext()) {
			RpsBatchAcsAssociation rpsBatchAcsAssociation = assnIt.next();
			RpsBatch rpsBatch = rpsBatchAcsAssociation.getRpsBatch();
			RpsEvent rpsEvent = batchEventMap.get(rpsBatch.getBatchCode());
			if (rpsEvent == null) {
				rpsEvent = rpsBatchService.getEventByBatch(rpsBatch);
				batchEventMap.put(rpsBatch.getBatchCode(), rpsEvent);
			}
			RpsAcsServer rpsAcsServer = rpsBatchAcsAssociation.getRpsAcsServer();
			// create packStatusInfoRow
			PackStatusInfoRow packStatusInfoRow = new PackStatusInfoRow();
			packStatusInfoRow.setEventCode(rpsEvent.getEventCode());
			packStatusInfoRow.setBatchCode(rpsBatch.getBatchCode());
			packStatusInfoRow.setBatchName(rpsBatch.getBatchName());
			packStatusInfoRow.setAcsServerId(rpsAcsServer.getAcsServerId());
			int manualRpackCount = 0;

			Integer batchExtendedTime = rpsBatchAcsAssociation.getBatchExtensionTime();
			if (batchExtendedTime != null)
				packStatusInfoRow.setBatchExtendedTime(batchExtendedTime);

			if (rpsBatchAcsAssociation.isIsActive())
				packStatusInfoRow.setIsBatchActive(1);
			else
				packStatusInfoRow.setIsBatchActive(0);

			if (rpsAcsServer.getAcsServerName() == null || rpsAcsServer.getAcsServerName().isEmpty())
				packStatusInfoRow.setAcsServerName("");
			else
				packStatusInfoRow.setAcsServerName(rpsAcsServer.getAcsServerName());
			List<RpsPack> rpsPackList = rpsPackService.getAllRpsPackByAssociation(rpsBatchAcsAssociation);
			if (rpsPackList != null && !rpsPackList.isEmpty()) {

				Map<String, String> componentStatusMap = new HashMap<>();
				Iterator<RpsPack> packIterator = rpsPackList.iterator();
				while (packIterator.hasNext()) {

					RpsPack rpsPack = packIterator.next();
					String packType = rpsPack.getPackType();
					packType = packType.toUpperCase();
					switch (packType) {
					case "BPACK":
						packStatusInfoRow.setbPackStatus(rpsPack.getPackStatus());
						break;
					case "RPACK":
						if (rpsPack.getPackSubType().equals(PackSubTypeEnum.AutomatedRpack.toString().toUpperCase())) {
							packStatusInfoRow.setrPackStatus(rpsPack.getPackStatus());
							if (rpsPack.getIsDeltaPackExists() == null || !(rpsPack.getIsDeltaPackExists())) {
								// there is no Delta Rpack expected for the
								// Rpack, update rpack status to NA
								packStatusInfoRow.setrDeltaPackStatus(RpsConstants.packStatus.NA.toString());
							}
							updateComponentStatusMap(componentStatusMap, rpsPack, packStatusInfoRow);
						} else if (rpsPack.getPackSubType()
								.equals(PackSubTypeEnum.AutomatedDeltaRpack.toString().toUpperCase())) {
							// set delta rpack status
							packStatusInfoRow.setrDeltaPackStatus(rpsPack.getPackStatus());
							updateComponentStatusMap(componentStatusMap, rpsPack, packStatusInfoRow);
						} else if (rpsPack.getPackSubType()
								.equals(PackSubTypeEnum.ManualRpack.toString().toUpperCase())) {
							manualRpackCount++;
							updateComponentStatusMap(componentStatusMap, rpsPack, packStatusInfoRow);
						}
						break;
					case "ATTENDANCEPACK":
						packStatusInfoRow.setAttendancePackStatus(rpsPack.getPackStatus());
						break;
					default:

					}// close switch
				} // close packIterator while loop
					// iterate through componentStatusMap and update status
					// accordingly
				if (componentStatusMap != null && !componentStatusMap.isEmpty()) {
					for (String componentName : componentStatusMap.keySet()) {
						if (RpsConstants.RpackComponents.ATTENDANCE_JSON.toString().equalsIgnoreCase(componentName))
							packStatusInfoRow.setAttendanceReportStatus(componentStatusMap.get(componentName));

						if (RpsConstants.RpackComponents.CANDIDATE_LOG.toString().equalsIgnoreCase(componentName))
							packStatusInfoRow.setCandAuditStaus(componentStatusMap.get(componentName));

						if (RpsConstants.RpackComponents.RESPONSE_JSON.toString().equalsIgnoreCase(componentName))
							packStatusInfoRow.setCandResStatus(componentStatusMap.get(componentName));

						if (RpsConstants.RpackComponents.ADMIN_LOG.toString().equalsIgnoreCase(componentName))
							packStatusInfoRow.setAdminAuditStatus(componentStatusMap.get(componentName));
					}
				}
			}

			packStatusInfoRow.setrManualPackCount(manualRpackCount);
			packStatusInfoRowSet.add(packStatusInfoRow);
		} // close batchIterator while loop

		String packStatusJson = gson.toJson(packStatusInfoRowSet);

		JSONObject jsonObject = new JSONObject();
		jsonObject.put(RpsConstants.totalRecords, totalRecords);
		jsonObject.put("data", packStatusJson);
		// totalRecords: , data:
		return jsonObject.toJSONString();
	}

	@Transactional(readOnly = true)
	public List<Integer> getAllBatchesInDateRange(List<RpsEvent> rpsEventList, String startDate, String endDate) {
		List<Integer> rpsBatchList = null;
		if (startDate == null || endDate == null || startDate.equals("") && endDate.equals("")) {

			rpsBatchList = rpsBatchService.getAllRpsBatchIdsForDateRange(rpsEventList);
		} else {
			// convert string dates to calendar objects
			Calendar batchStartDate = Calendar.getInstance();
			Calendar batchEndDate = Calendar.getInstance();
			try {
				String dateTimeFormat = RpsConstants.DATE_FORMAT_WITH_TIME;
				batchStartDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(startDate));
				batchEndDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(endDate));
			} catch (ParseException e) {
				logger.error("Date Format could not be parsed for EventList" + e.getStackTrace());
			}

			// get RpsEvent list per customer code from database
			rpsBatchList = rpsBatchService.getRpsBatchIdsListInDateRange(rpsEventList, batchStartDate);
			// rpsBatchList =
			// rpsBatchService.getAllRpsBatchIdsForDateRange(rpsEventList,
			// batchStartDate, batchEndDate);

		}

		return rpsBatchList;
	}

	@Transactional(readOnly = true)
	public String getAllQPackStatusInDateRange(String customerCode, String divisionCode, String eventCode,
			String startDate, String endDate, Integer pageNo, Integer size, String sortingOrder,
			String sortColumnName) {
		String jsonStatus = EMPTY_JSON;
		List<String> rpsCustomerList = new ArrayList<String>();
		List<String> rpsDivisionList = new ArrayList<String>();
		List<RpsEvent> rpsEventList = new ArrayList<RpsEvent>();

		if (customerCode != null && divisionCode != null && eventCode != null && customerCode.length() != 0
				&& divisionCode.length() != 0 && eventCode.length() != 0) {
			RpsCustomer rpsCustomer = rpsCustomerService.findByCustomerCode(customerCode);

			if (rpsCustomer != null) {
				RpsDivision rpsDivision = rpsDivisionService
						.findByRpsCustomerCustomerCodeAndDivisionCode(rpsCustomer.getCustomerCode(), divisionCode);
				if (rpsDivision != null) {
					RpsEvent rpsEvent = rpsEventService.findByRpsDivisionAndEventCode(rpsDivision, eventCode);
					rpsEventList.add(rpsEvent);

					jsonStatus = this.getAllQPackStatusInDateRangeUsingEvent(rpsEventList, startDate, endDate, pageNo,
							size, sortingOrder, sortColumnName);

				} else {
					logger.warn("division information is not available in database----");
				}
			} else {
				logger.warn("customer information is not available in database----");
			}

		} else if (customerCode == null || customerCode.length() == 0) {
			rpsCustomerList = rpsCustomerService.findAllRpsCustomerCode();

			if (rpsCustomerList != null && rpsCustomerList.size() != 0) {
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
			} else {
				logger.warn("customer information is not available in database----");
			}

			if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
				rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
				jsonStatus = this.getAllQPackStatusInDateRangeUsingEvent(rpsEventList, startDate, endDate, pageNo, size,
						sortingOrder, sortColumnName);

			} else {
				logger.warn("division information is not available in database----");
			}
		} else {
			if (divisionCode == null || divisionCode.length() != 0) {
				rpsCustomerList.add(customerCode);
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
				if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);

					jsonStatus = this.getAllQPackStatusInDateRangeUsingEvent(rpsEventList, startDate, endDate, pageNo,
							size, sortingOrder, sortColumnName);

				} else {
					logger.warn("division information is not available in database----");
				}
			} else {
				if (eventCode == null || eventCode.length() != 0) {
					rpsDivisionList.add(divisionCode);
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);

					jsonStatus = this.getAllQPackStatusInDateRangeUsingEvent(rpsEventList, startDate, endDate, pageNo,
							size, sortingOrder, sortColumnName);

				} else {
					RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
					rpsEventList.add(rpsEvent);
					if (rpsEventList != null || rpsEventList.size() != 0) {

						jsonStatus = this.getAllQPackStatusInDateRangeUsingEvent(rpsEventList, startDate, endDate,
								pageNo, size, sortingOrder, sortColumnName);

					}
				}
			}
		}

		return jsonStatus;
	}

	@Transactional(readOnly = true)
	public String getAllAnswerKeysStatus(String customerCode, String divisionCode, String eventCode, String startDate,
			String endDate, Integer pageNo, Integer size, String sortOrder, String sortColumnName) {
		String jsonStatus = EMPTY_JSON;
		List<String> rpsCustomerList = new ArrayList<String>();
		List<String> rpsDivisionList = new ArrayList<String>();
		List<RpsEvent> rpsEventList = new ArrayList<RpsEvent>();

		if (customerCode == null || customerCode.isEmpty()) {
			rpsCustomerList = rpsCustomerService.findAllRpsCustomerCode();

			if (rpsCustomerList != null && rpsCustomerList.size() != 0) {
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
			} else {
				logger.warn("customer information is not available in database----");
			}

			if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
				rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
				jsonStatus = this.getAllAnswerKeysStatus(rpsEventList, startDate, endDate, pageNo, size, sortOrder,
						sortColumnName);
			} else {
				logger.warn("division information is not available in database----");
			}
		} else {
			if (divisionCode == null || divisionCode.isEmpty()) {
				rpsCustomerList.add(customerCode);
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
				if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
					jsonStatus = this.getAllAnswerKeysStatus(rpsEventList, startDate, endDate, pageNo, size, sortOrder,
							sortColumnName);
				} else {
					logger.warn("division information is not available in database----");
				}
			} else {
				if (eventCode == null || eventCode.isEmpty()) {
					rpsDivisionList.add(divisionCode);
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
					jsonStatus = this.getAllAnswerKeysStatus(rpsEventList, startDate, endDate, pageNo, size, sortOrder,
							sortColumnName);
				} else {
					RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
					rpsEventList.add(rpsEvent);
					if (rpsEventList != null || rpsEventList.size() != 0) {
						jsonStatus = this.getAllAnswerKeysStatus(rpsEventList, startDate, endDate, pageNo, size,
								sortOrder, sortColumnName);
					}
				}
			}
		}

		return jsonStatus;
	}

	@Transactional(readOnly = true)
	public String getAllProcessResultsStatus(String customerCode, String divisionCode, String eventCode,
			String startDate, String endDate, Integer pageNo, Integer size, String sortOrder, String sortColumnName) {
		String jsonStatus = EMPTY_JSON;
		long totalRecords = 0;
		List<String> rpsCustomerList = new ArrayList<String>();
		List<String> rpsDivisionList = new ArrayList<String>();
		List<RpsEvent> rpsEventList = new ArrayList<RpsEvent>();

		if (customerCode == null || customerCode.isEmpty()) {
			rpsCustomerList = rpsCustomerService.findAllRpsCustomerCode();

			if (rpsCustomerList != null && rpsCustomerList.size() != 0) {
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
			} else {
				logger.warn("customer information is not available in database----");

			}

			if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
				rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);

			} else {
				logger.warn("division information is not available in database----");

			}
		} else {
			if (divisionCode == null || divisionCode.isEmpty()) {
				rpsCustomerList.add(customerCode);
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
				if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);

				} else {
					logger.warn("division information is not available in database----");

				}
			} else {
				if (eventCode == null || eventCode.isEmpty()) {
					rpsDivisionList.add(divisionCode);
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);

				} else {
					RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
					rpsEventList.add(rpsEvent);
				}
			}
		}

		Set<ProcessResultInfoRow> processResultInfoRowSet = new HashSet<>();

		if (rpsEventList != null) {
			Iterator<RpsEvent> eveIt = rpsEventList.iterator();
			while (eveIt.hasNext()) {
				RpsEvent rpsEvent = eveIt.next();
				totalRecords = totalRecords + this.getAllProcessTabResultsStatus(rpsEvent.getEventCode(), startDate,
						endDate, processResultInfoRowSet, pageNo, size, sortOrder, sortColumnName);
			}
		}
		Gson gson = new Gson();
		jsonStatus = gson.toJson(processResultInfoRowSet);
		JSONObject jsonObject = new JSONObject();
		if (totalRecords == 0)
			jsonObject.put(RpsConstants.totalRecords, 0);
		else
			jsonObject.put(RpsConstants.totalRecords, totalRecords);
		jsonObject.put("data", jsonStatus);
		return jsonObject.toJSONString();

	}

	@Transactional(readOnly = true)
	public String getAllPackStatusInDateRange(String customerCode, String divisionCode, String eventCode,
			String startDate, String endDate, Integer pageNo, Integer size, String sortingOrder, String columnName) {
		String jsonStatus = EMPTY_JSON;
		List<String> rpsCustomerList = new ArrayList<String>();
		List<String> rpsDivisionList = new ArrayList<String>();
		List<RpsEvent> rpsEventList = new ArrayList<RpsEvent>();

		if (customerCode == null || customerCode.length() == 0) {
			rpsCustomerList = rpsCustomerService.findAllRpsCustomerCode();

			if (rpsCustomerList != null && rpsCustomerList.size() != 0) {
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
			} else {
				logger.warn("customer information is not available in database----");
			}

			if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
				rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
				if (size > 0)
					jsonStatus = this.getAllPackStatusInDateRange(rpsEventList, startDate, endDate, pageNo, size,
							sortingOrder, columnName);
			} else {
				logger.warn("division information is not available in database----");
			}
		} else {
			if (divisionCode == null || divisionCode.length() == 0) {
				rpsCustomerList.add(customerCode);
				rpsDivisionList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(rpsCustomerList);
				if (rpsDivisionList != null && rpsDivisionList.size() != 0) {
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
					if (size > 0)
						jsonStatus = this.getAllPackStatusInDateRange(rpsEventList, startDate, endDate, pageNo, size,
								sortingOrder, columnName);
				} else {
					logger.warn("division information is not available in database----");
				}
			} else {
				if (eventCode == null || eventCode.length() == 0) {
					rpsDivisionList.add(divisionCode);
					rpsEventList = rpsEventService.getAllRpsEventsPerDivisionCode(rpsDivisionList);
					if (size > 0)
						jsonStatus = this.getAllPackStatusInDateRange(rpsEventList, startDate, endDate, pageNo, size,
								sortingOrder, columnName);
				} else {
					RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
					rpsEventList.add(rpsEvent);
					if (rpsEventList != null || rpsEventList.size() != 0) {
						if (size > 0)
							jsonStatus = this.getAllPackStatusInDateRange(rpsEventList, startDate, endDate, pageNo,
									size, sortingOrder, columnName);
					}
				}
			}
		}

		return jsonStatus;

	}

	public long getAllProcessTabResultsStatus(String eventCode, String startDate, String endDate,
			Set<ProcessResultInfoRow> processResultInfoRowSet, Integer pageNo, Integer size, String sortOrder,
			String sortColumnName) {
		List<Integer> rpsBatchList = null;
		long totalRecords = 0;
		// get all batches for the event in date range
		rpsBatchList = getAllBatchesIdsList(eventCode, startDate, endDate);
		if (rpsBatchList == null || rpsBatchList.isEmpty()) {
			logger.error("There are no batches for event and selected date range");
			return totalRecords;
		}

		Sort sort = null;
		if (sortColumnName.equalsIgnoreCase(batchCode)) {
			String batchCode = processResultColumn.batchCode.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, batchCode);
			} else {
				sort = new Sort(Direction.DESC, batchCode);
			}
		} else if (sortColumnName.equalsIgnoreCase(batchName)) {
			String batchName = processResultColumn.batchName.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, batchName);
			} else {
				sort = new Sort(Direction.DESC, batchName);
			}
		} else if (sortColumnName.equalsIgnoreCase(acsServerName)) {
			String acsServerName = processResultColumn.acsServerName.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, acsServerName);
			} else {
				sort = new Sort(Direction.DESC, acsServerName);
			}
		} else if (sortColumnName.equalsIgnoreCase(venue)) {
			String venue = processResultColumn.venue.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, venue);
			} else {
				sort = new Sort(Direction.DESC, venue);
			}
		} else if (sortColumnName.equalsIgnoreCase(assessmentName)) {
			String assessmentName = processResultColumn.assessmentName.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, assessmentName);
			} else {
				sort = new Sort(Direction.DESC, assessmentName);
			}
		} else if (sortColumnName.equalsIgnoreCase(setCode)) {
			String qpaperCode = processResultColumn.setCode.getColumnName();
			if (sortOrder.equalsIgnoreCase(ASC)) {
				sort = new Sort(Direction.ASC, qpaperCode);
			} else {
				sort = new Sort(Direction.DESC, qpaperCode);
			}
		}

		List<RpsCumulativeResponses> rpsCumulativeResponses = rpsCumulativeResponsesService
				.getAllCumulativeResponsesByBatch(rpsBatchList, new PageRequest(pageNo, size, sort));

		if (rpsCumulativeResponses == null || rpsCumulativeResponses.isEmpty()) {
			logger.warn("There are no cumulative responses for the event " + eventCode);
			return totalRecords;
		}

		totalRecords = rpsCumulativeResponsesService.getTotalNoCumulativeResponsesByBatchList(rpsBatchList);

		Iterator<RpsCumulativeResponses> cumuIt = rpsCumulativeResponses.iterator();
		while (cumuIt.hasNext()) {
			RpsCumulativeResponses response = cumuIt.next();
			ProcessResultInfoRow processResultInfoRow = new ProcessResultInfoRow();
			RpsAcsServer rpsAcsServer = response.getRpsBatchAcsAssociation().getRpsAcsServer();
			processResultInfoRow.setAcsServerId(rpsAcsServer.getAcsServerId());
			String acsServerName = rpsAcsServer.getAcsServerName();
			if (acsServerName == null)
				processResultInfoRow.setAcsServerName("");
			else
				processResultInfoRow.setAcsServerName(acsServerName);
			RpsBatch rpsBatch = response.getRpsBatchAcsAssociation().getRpsBatch();
			processResultInfoRow.setBatchCode(rpsBatch.getBatchCode());
			processResultInfoRow.setBatchName(rpsBatch.getBatchName());
			boolean isBatchActive = response.getRpsBatchAcsAssociation().isIsActive();
			if (isBatchActive)
				processResultInfoRow.setIsBatchActive(1);
			else
				processResultInfoRow.setIsBatchActive(0);
			processResultInfoRow.setEventCode(eventCode);
			processResultInfoRow.setIsScoreCalculated(response.getIsResultComputed());
			processResultInfoRow.setRpackStatus(RpsConstants.packStatus.UNPACKED.toString());
			processResultInfoRow.setAssessmentCode(response.getRpsAssessment().getAssessmentCode());
			processResultInfoRow.setAssessmentName(response.getRpsAssessment().getAssessmentName());
			processResultInfoRow.setSetCode(response.getRpsQuestionPaper().getQpCode());
			processResultInfoRow.setQpId(response.getRpsQuestionPaper().getQpId());
			String qpStatus = response.getRpsQuestionPaper().getQpStatus();
			boolean isGenerationAllowed = false;
			if (qpStatus != null && (qpStatus.equalsIgnoreCase(RpsConstants.QPAPER_STATUS.AVAILABLE.toString())
					|| qpStatus.equalsIgnoreCase(RpsConstants.QPAPER_STATUS.DOWNLOAD_SUCCESSFUL.toString()))) {
				isGenerationAllowed = true;
			}
			boolean isAnswerKeysAvailable = false;
			if (response.getRpsQuestionPaper() != null) {
				AnswerStatus answerStatus = this.getMCQAnswerKeyStatusForQPaper(response.getRpsQuestionPaper());
				isAnswerKeysAvailable = answerStatus.isAnswerKeyAvailable();
			}
			isGenerationAllowed = isGenerationAllowed && isAnswerKeysAvailable;
			processResultInfoRow.setGenerationAllowed(isGenerationAllowed);
			RpsVenue rpsVenue = rpsVenueService.findVenueDetailsByVenueID(rpsAcsServer.getRpsVenue().getVenueId());
			processResultInfoRow.setVenue(rpsVenue.getVenueCode());
			processResultInfoRowSet.add(processResultInfoRow);
		}
		return totalRecords;

	}

	private AnswerStatus getMCQAnswerKeyStatusForQPaper(RpsQuestionPaper rpsQuestionPaper) {

		boolean isAnswerKeyAvailable = false;
		Set<String>qCount=new HashSet<>();
		Set<String>aCount=new HashSet<>();
		// List<RpsQuestion> rpsQuestions = rpsQuestionAssociationService
		// .getQuestionsFromQp(rpsQuestionPaper);

		List<RpsQuestion> rpsQuestions = rpsQuestionAssociationService
				.getQuestionsLiteForQp(rpsQuestionPaper.getQpId());

		if (rpsQuestions != null && !rpsQuestions.isEmpty()) {
			for (RpsQuestion rpsQuestion : rpsQuestions) {
				if (rpsQuestion.getQuestionType()
						.equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
					qCount.add(rpsQuestion.getQuestId());
					if (rpsQuestion.getQans() != null && !rpsQuestion.getQans().isEmpty())
						aCount.add(rpsQuestion.getQuestId());
				}

			}
		}

		if ((aCount.size()) == (qCount.size()) && aCount.size() > 0)
			isAnswerKeyAvailable = true;

		AnswerStatus answerStatus = new AnswerStatus(isAnswerKeyAvailable, aCount.size());
		return answerStatus;
	}

	@Override
	public void computeResultForTheBatchAndQp(String argsJson) {
		// TODO Auto-generated method stub
		final String eventCode = "eventCode";
		final String batchCode = "batchCode";
		final String acsServerCode = "acsServerCode";
		final String assessmentCode = "assessmentCode";
		final String qpCode = "qpCode";

		String event = "";
		String batch = "";
		String acsServer = "";
		String assessment = "";
		String qpaper;
		Integer qpId = null;
		JsonParser parser = new JsonParser();
		JsonArray array = (JsonArray) parser.parse(argsJson);
		Iterator<JsonElement> it = array.iterator();

		while (it.hasNext()) {
			JsonObject obj = (JsonObject) it.next();
			event = obj.get(eventCode).getAsString();
			batch = obj.get(batchCode).getAsString();
			acsServer = obj.get(acsServerCode).getAsString();
			assessment = obj.get(assessmentCode).getAsString();
			qpaper = obj.get(qpCode).getAsString();
			RpsQuestionPaper paper = rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessment, qpaper);
			if (paper != null)
				qpId = paper.getQpId();
			try {
				resultComputationService.computeResultForTheBatchAndQp(event, batch, acsServer, assessment, qpId);
			} catch (RpsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void computeResultForTheBatchAssessmentAndQpCode(String eventCode, String batchCode, String acsServerCode,
			String assessmentCode, String qpCode) {
		RpsQuestionPaper paper = rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentCode, qpCode);

		try {
			resultComputationService.computeResultForTheBatchAndQp(eventCode, batchCode, acsServerCode, assessmentCode,
					paper.getQpId());
		} catch (RpsException e) {
			// TODO Auto-generated catch block
			logger.error("Result Computation error :: " + e.getMessage());
		}
	}

	public void computeResultForListOfTheBatchAssessmentAndQpCode(List<Parameters> parametersList) {
		Iterator<Parameters> parameterIterator = parametersList.listIterator();
		while (parameterIterator.hasNext()) {
			Parameters parameterDetails = parameterIterator.next();
			this.computeResultForTheBatchAssessmentAndQpCode(parameterDetails.getEventCode(),
					parameterDetails.getBatchCode(), parameterDetails.getAcsCode(),
					parameterDetails.getAssessmentCode(), parameterDetails.getSetCode());
		}
	}

	public String getCustomerNameForCustomerCode(String customerCode) {
		Gson gson = new Gson();
		String customerName = "";
		RpsCustomer rpsCustomer = rpsCustomerService.findByCustomerCode(customerCode);
		if (rpsCustomer != null)
			customerName = gson.toJson(rpsCustomer.getCustomerName(), String.class);
		else
			logger.warn("Customer information is not available in database for particular customerCode = {}"
					+ customerCode);
		return customerName;
	}

	public String getDivisionNameForDivisionCode(String divisionCode) {
		Gson gson = new Gson();
		String divisionName = "";
		RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(divisionCode);
		if (rpsDivision != null)
			divisionName = gson.toJson(rpsDivision.getDivisionName(), String.class);
		else
			logger.warn("Division information is not available in database for particular divisionCode = {}"
					+ divisionCode);
		return divisionName;
	}

	public String getEventNameForEventCode(String eventCode) {
		Gson gson = new Gson();
		String eventName = "";
		RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
		if (rpsEvent != null)
			eventName = gson.toJson(rpsEvent.getEventName());
		else
			logger.warn("Event information is not available in database for particular eventCode = {}", eventCode);
		return eventName;
	}

	public String getAllPacksChangeHistory(String eventCode) {
		Set<PacksChangeHistory> packsChangeHistorySet = new TreeSet<>();
		// get all Qpacks change history
		// getQpackChangeHistory(packsChangeHistorySet, eventCode);
		getBpackRpackChangeHistory(packsChangeHistorySet, eventCode);
		return new Gson().toJson(packsChangeHistorySet);
	}

	private void getBpackRpackChangeHistory(Set<PacksChangeHistory> packsChangeHistorySet, String eventCode) {

		/*
		 * List<RpsPack> rpsPackList= null; if(eventCode!=null &&
		 * !eventCode.equalsIgnoreCase("0")) { rpsPackList =
		 * rpsPackService.getAllPacksByEventCodeAndReceiveMode(eventCode,
		 * RpsConstants.packReceiveMode.MANUAL_UPLOAD.toString()); }else{
		 * rpsPackList =
		 * rpsPackService.getAllPacksByReceiveMode(RpsConstants.packReceiveMode
		 * .MANUAL_UPLOAD.toString()); }
		 */

		List<RpsManualPacksHistory> rpsPackList = null;
		if (eventCode != null) {
			rpsPackList = rpsManualPacksHistoryService.getAllManualPacksHistories();
		}
		if (rpsPackList != null && !rpsPackList.isEmpty()) {
			Map<String, TreeSet<RpsManualPacksHistory>> packCodePacksMap = new HashMap<String, TreeSet<RpsManualPacksHistory>>();
			Iterator<RpsManualPacksHistory> packIt = rpsPackList.iterator();
			// iterate through every pack and create map of packcode and packs
			// having same name with different versions
			while (packIt.hasNext()) {
				RpsManualPacksHistory pack = packIt.next();
				PacksChangeHistory packHistory = new PacksChangeHistory();
				SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
				String changeTime = sdf.format((pack.getCreatedTime()));
				packHistory.setChangeTime(changeTime);
				packHistory.setPackCode(pack.getPackCode());
				packHistory.setPackType(pack.getPackType());
				packHistory.setOlderVersion("NA");
				packHistory.setCurrentVersion(pack.getVersion() + "");
				packHistory.setUser("SYSTEM");
				packHistory.setComments(pack.getComments());

				RpsPack rpsPack = rpsPackService.getPacksByPackCode(pack.getPackCode());
				/*
				 * RpsBatchAcsAssociation rpsBatchAcsAssociation =
				 * rpsPack.getRpsBatchAcsAssociation();
				 * rpsBatchAcsAssociationService.get RpsAcsServer rpsAcsServer =
				 * rpsBatchAcsAssociation.getRpsAcsServer(); RpsEvent rpsEvent =
				 * rpsAcsServer.getRpsEvent(); RpsDivision rpsDivision =
				 * rpsEvent.getRpsDivision(); RpsCustomer rpsCustomer =
				 * rpsDivision.getRpsCustomer();
				 * packHistory.setCustomerCode(rpsCustomer.getCustomerCode());
				 * packHistory.setCustomerName(rpsCustomer.getCustomerName());
				 * packHistory.setDivisionCode(rpsDivision.getDivisionCode());
				 * packHistory.setDivisionName(rpsDivision.getDivisionName());
				 * packHistory.setEventCode(rpsEvent.getEventCode());
				 * packHistory.setEventName(rpsEvent.getEventName());
				 */
				// packHistory.setEventName(pack.getRpsBatchAcsAssociation().getRpsBatch().getRpsEvent().getEventName());
				packHistory.setPackStatus(pack.getPackStatus());
				packsChangeHistorySet.add(packHistory);
			}

		}

	}

	private void getQpackChangeHistory(Set<PacksChangeHistory> packsChangeHistorySet, String eventCode) {
		List<RpsQuestionPaperPack> qPacks = rpsQuestionPaperPackService.getAllQPacksByEventCodeAndReceiveMode(eventCode,
				RpsConstants.packReceiveMode.MANUAL_UPLOAD.toString());

		if (qPacks != null && !qPacks.isEmpty()) {
			Map<String, TreeSet<RpsQuestionPaperPack>> packCodeQPacksMap = new HashMap<String, TreeSet<RpsQuestionPaperPack>>();
			Iterator<RpsQuestionPaperPack> qPackIt = qPacks.iterator();
			// iterate through every qpack and create map of packcode and qpacks
			// having same name with different versions
			while (qPackIt.hasNext()) {
				RpsQuestionPaperPack qPack = qPackIt.next();
				String packCode = qPack.getPackId();
				int index = packCode.lastIndexOf(VERSION);
				String packCodeWtVersion = packCode.substring(0, index);
				// check if packCodeWtVersion is already added to the map
				TreeSet<RpsQuestionPaperPack> qPacksSet = packCodeQPacksMap.get(packCodeWtVersion);
				if (qPacksSet == null || qPacksSet.isEmpty()) {
					qPacksSet = new TreeSet<>();
					qPacksSet.add(qPack);
					packCodeQPacksMap.put(packCodeWtVersion, qPacksSet);
				} else {
					// if packCode is already added to the map, update the Set
					// of packs
					qPacksSet.add(qPack);
				}
			}

			// iterate through the map and update packsChangeHistorySet
			Iterator<String> keyIt = packCodeQPacksMap.keySet().iterator();
			while (keyIt.hasNext()) {
				String key = keyIt.next();
				TreeSet<RpsQuestionPaperPack> rpsQuestionPaperPackSet = packCodeQPacksMap.get(key);
				String olderVersion = NA;
				if (rpsQuestionPaperPackSet != null && !rpsQuestionPaperPackSet.isEmpty()) {
					Iterator<RpsQuestionPaperPack> packIt = rpsQuestionPaperPackSet.iterator();
					while (packIt.hasNext()) {
						RpsQuestionPaperPack pack = packIt.next();
						PacksChangeHistory packHistory = new PacksChangeHistory();
						SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
						String changeTime = sdf.format(pack.getLastModifiedDate());
						packHistory.setChangeTime(changeTime);
						packHistory.setPackCode(pack.getPackId());
						packHistory.setPackType(RpsConstants.PackType.QP_PACK.toString());
						packHistory.setOlderVersion(olderVersion);
						packHistory.setCurrentVersion(pack.getVersionNumber());
						packHistory.setUser(pack.getCreatedBy());
						packHistory.setPackStatus(pack.getPackStatus());
						packHistory.setComments("");
						packHistory.setEventName(pack.getRpsEvent().getEventName());
						packsChangeHistorySet.add(packHistory);
						olderVersion = pack.getVersionNumber();
					}

				}

			}
		}

	}

	/**
	 * Export descriptive answer tiff images and create xml with tiff ref
	 *
	 * @param batchCode
	 * @param batchName
	 * @param eventCode
	 * @param acsCode
	 * @param assessmentCode
	 * @param assessmentName
	 * @param qpCode
	 * @return
	 */
	@Transactional
	public String exportDescriptiveQTiffs(String batchCode, String batchName, String eventCode, String acsCode,
			String assessmentCode, String assessmentName, String qpCode) {
		return RpsConstants.SUCCESS_STATUS;
	}

	/**
	 * Export descriptive SOE PDF
	 *
	 * @param qpCode
	 * @param assessmentCode
	 * @return
	 */

	@Transactional(readOnly = true)
	public String exportDescriptiveSOE(String qpCode, String assessmentCode) {

		RpsQuestionPaper paper = rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentCode, qpCode);

		if (paper.isIsAnswerKeyAvailable() == false) {
			logger.info(" Answer key is not available for the question paper " + paper.getQpCode());
			return RpsConstants.FAILED_STATUS;
		}

		Set<RpsQuestionAssociation> questionAssociations = paper.getRpsQuestionAssociations();
		logger.debug("Question paper set : " + qpCode);
		List<RpsQuestionAssociation> questionAssociationList = new ArrayList<RpsQuestionAssociation>(
				questionAssociations);

		Collections.sort(questionAssociationList, new Comparator<RpsQuestionAssociation>() {
			@Override
			public int compare(RpsQuestionAssociation o1, RpsQuestionAssociation o2) {

				if (o1.getQuestionSequence() == o2.getQuestionSequence()) {
					return 0;
				}

				if (o1.getQuestionSequence() == null) {
					return -1;
				}

				if (o2.getQuestionSequence() == null) {
					return 1;
				}

				logger.debug("Comparing {} and {}", o1.getQuestionSequence(), o2.getQuestionSequence());
				return (o1.getQuestionSequence()).compareTo(o2.getQuestionSequence());
			}
		});

		Iterator<RpsQuestionAssociation> questionAssociationItr = questionAssociationList.iterator();

		Map<String, RpsQuestion> descQuestionMap = new LinkedHashMap<String, RpsQuestion>();
		Map<String, Integer> descQuestionSequenceMap = new LinkedHashMap<String, Integer>();

		while (questionAssociationItr != null && questionAssociationItr.hasNext()) {
			RpsQuestionAssociation rpsQAssociation = questionAssociationItr.next();
			RpsQuestion question = rpsQAssociation.getRpsQuestion();
			if (question.getQuestionType()
					.equalsIgnoreCase(QuestionType.DESCRIPTIVE_QUESTION.toString())) {
				descQuestionMap.put(question.getQuestId(), question);
				descQuestionSequenceMap.put(question.getQuestId(), rpsQAssociation.getQuestionSequence());
			}

		}

		if (descQuestionMap != null && descQuestionMap.size() <= 0) {
			logger.debug("There are no descriptive questions in the question paper set");
			return RpsConstants.NON_DESCRIPTIVE;
		}

		logger.debug("No of questions in the question paper set : " + descQuestionMap.size());
		Set<String> descQuestionKeySet = descQuestionMap.keySet();
		Iterator<String> descQuestionKeySetIterator = descQuestionKeySet.iterator();
		List<DescriptiveDetails> list = new ArrayList<DescriptiveDetails>();
		while (descQuestionKeySetIterator.hasNext()) {
			String descQuestionId = descQuestionKeySetIterator.next();
			RpsQuestion question = descQuestionMap.get(descQuestionId);

			/*
			 * descQuestions.put(rpsQAssociation.getQuestionSequence(),
			 * question);
			 */

			DescriptiveDetails descDetails = new DescriptiveDetails();
			descDetails.setAssessmentSetID(qpCode);
			RpsAssessment assessment = paper.getRpsAssessment();
			String assessmentName = assessment.getAssessmentName();
			descDetails.setAssessmentName(assessmentName);
			descDetails.setQuestionID(question.getQuestId());
			descDetails.setAnswer(question.getQans());
			descDetails.setQuestion(question.getQtext());
			descDetails.setScore(question.getScore() == null ? 0 : question.getScore());
			descDetails.setPageNumber(descQuestionSequenceMap.get(descQuestionId));
			list.add(descDetails);
		}
		logger.debug("No of questions in the question paper set : " + descQuestionMap.size());
		PDFWriterUtility pdfUtility = new PDFWriterUtility();
		String path = "";
		try {
			path = soeLocation + qpCode;
			File file = new File(path);
			file.mkdirs();
			pdfUtility.PDFWriterGenerator(FilenameUtils.separatorsToSystem(path + File.separator + qpCode + ".pdf"),
					list);
		} catch (FileNotFoundException e) {
			logger.error(" PDF file already created and opened due to FileNotFoundException ", e);
			File fileDir = new File(path);
			FileUtils.deleteQuietly(fileDir);
			return RpsConstants.FILE_OPENED;

		} catch (Exception e) {
			logger.error(" PDF creation failed due to Exception ", e);
			File fileDir = new File(path);
			try {
				FileUtils.deleteDirectory(fileDir);
			} catch (IOException e1) {
				logger.error(" Unable to delete the directory ", e1);
			}
			return RpsConstants.FAILED_STATUS;
		}

		return RpsConstants.SUCCESS_STATUS;

	}

	private int findWordCount(String answer) {
		int count = 0;
		if (answer != null) {
			String[] answers = answer.split("[\\p{Punct}\\s]+");
			/*
			 * for(String answer1:answers) System.out.println(answer1);
			 */
			count = answers.length;
		}
		return count;
	}

	public String getAllCustomers(List<String> eventList, boolean isSuperUser) {
		// create customers info list
		List<RpsCustomerInfo> rpsCustomersInfoList = new LinkedList<RpsCustomerInfo>();
		List<RpsCustomer> rpsCustomersList = null;
		// check if user is super user
		if (isSuperUser) {
			// load all the customers data from database
			rpsCustomersList = rpsCustomerService.getAllRpsCustomers();
		} else if (eventList != null && !eventList.isEmpty()) {
			// load only those customers, which have eventList
			rpsCustomersList = rpsEventService.findAllCustomersByEventList(eventList);
		}
		if (rpsCustomersList == null) {
			logger.error("no list of Customers is available");
			return "";
		}
		// populate customers info list
		for (RpsCustomer rpsCustomer : rpsCustomersList) {
			RpsCustomerInfo rpsCustomerInfo = new RpsCustomerInfo();
			rpsCustomerInfo.setCustomerCode(rpsCustomer.getCustomerCode());
			rpsCustomerInfo.setCustomerName(rpsCustomer.getCustomerName());

			// add RpsCustomerInfo object to the list
			rpsCustomersInfoList.add(rpsCustomerInfo);
		}

		// generate JSON string of RpsCustomerInfo list objects
		return new Gson().toJson(rpsCustomersInfoList);
	}

	public String getAllDivisionsPerCustomer(String customerCode, List<String> eventList, boolean isSuperUser) {
		// create list of rps divsion info
		List<RpsDivisionInfo> rpsDivisionsInfoList = new LinkedList<>();
		// get divisions list per customer code from database
		List<RpsDivision> rpsDivisionsList = null;
		if (isSuperUser) {
			rpsDivisionsList = rpsDivisionService.getAllRpsDivisionsPerCustomerCode(customerCode);
		} else {
			rpsDivisionsList = rpsDivisionService.findDivisionByEventCodeAndCustomerCode(eventList, customerCode);
		}
		if (rpsDivisionsList == null) {
			logger.error("There are no division for selected Customer");
			return "";
		}
		RpsDivisionInfo rpsDivisionInfo;
		for (RpsDivision rpsDivision : rpsDivisionsList) {
			rpsDivisionInfo = new RpsDivisionInfo();
			rpsDivisionInfo.setDivisionCode(rpsDivision.getDivisionCode());
			rpsDivisionInfo.setDivisionName(rpsDivision.getDivisionName());
			rpsDivisionsInfoList.add(rpsDivisionInfo);
		}
		return new Gson().toJson(rpsDivisionsInfoList);
	}

	public String getAllEventsPerDivision(String divisionCode, List<String> eventList, boolean isSuperUser) {
		// create list of RpsEventInfo
		List<RpsEventInfo> rpsEventsInfoList = new LinkedList<>();
		// get RpsEvent list per customer code from database

		List<RpsEvent> rpsEventsList;
		if (isSuperUser) {
			// check if event filter is applied, based on user roles
			rpsEventsList = rpsEventService.getAllRpsEventsPerDivisionCode(divisionCode);
		} else {
			rpsEventsList = rpsEventService.findCustomersByEventCodeAndDivisionCode(eventList, divisionCode);
		}

		if (rpsEventsList == null) {
			logger.error("There are no events for selected division");
			return "";
		}
		// populate list of rps Event info
		for (RpsEvent rpsEvent : rpsEventsList) {
			RpsEventInfo rpsEventInfo = new RpsEventInfo();
			rpsEventInfo.setEventCode(rpsEvent.getEventCode());
			rpsEventInfo.setEventName(rpsEvent.getEventName());
			// convert date to string format
			String startDate = convertToSpecificFormat(rpsEvent.getEventStartDate());
			String endDate = convertToSpecificFormat(rpsEvent.getEventEndDate());
			rpsEventInfo.setEventStartDate(startDate);
			rpsEventInfo.setEventEndDate(endDate);
			rpsEventInfo.setEventType(rpsEvent.getEventType());

			rpsEventsInfoList.add(rpsEventInfo);
		}

		return new Gson().toJson(rpsEventsInfoList);
	}

	public void getBachDetailsByBatchDate() {
		logger.info("---IN--- getBachDetailsByBatchDate()");
		List<String> rpsBatchList = null;
		String startDate = this.convertToSpecificFormat(Calendar.getInstance()) + " 00:00:00";
		String endDate = this.convertToSpecificFormat(Calendar.getInstance()) + " 23:59:59";
		// convert string dates to calendar objects
		Calendar batchStartDate = Calendar.getInstance();
		Calendar batchEndDate = Calendar.getInstance();
		try {
			String dateTimeFormat = RpsConstants.DATE_FORMAT_WITH_TIME;
			batchStartDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(startDate));
			batchEndDate.setTime(new SimpleDateFormat(dateTimeFormat).parse(endDate));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		// get rps batch list based on date range
		rpsBatchList = rpsBatchService.getAllRpsBatchFByBatchDate(batchStartDate, batchEndDate);

		if (!(rpsBatchList.isEmpty() && rpsBatchList == null)) {
			for (String rpsBatch : rpsBatchList) {
				this.getAllDetailsForPlayBackBasedOnDate(rpsBatch);
			}

		}
		logger.info("---OUT--- getBachDetailsByBatchDate()");
	}

	public void getAllDetailsForPlayBackBasedOnDate(String rpsBatchList) {
		logger.info("---IN--- getAllDetailsForPlayBackBasedOnDate()");

		List<Object[]> rpsMasterAssociation = null;
		CDEDetailsForPlayBack cDEDetailsForPlayBack = new CDEDetailsForPlayBack();
		Set<CandidateDetailsForPlayBack> candidateDetails = new HashSet<CandidateDetailsForPlayBack>();
		CandidateDetailsForPlayBack candidateDetail = null;
		CombinedBeanForPlayBack combinedBeanForPlayBack = null;
		Set<String> qPacksDelivered = new HashSet<String>();
		rpsMasterAssociation = rpsMasterAssociationService.findByBatchCode(rpsBatchList);
		// Integer uniqueCandidateId, String candidateLogs, String packId, String eventId,
		// String batchCode, String customerCode, String divisionCode//String auditFileName
		for (Object[] object : rpsMasterAssociation) {
			combinedBeanForPlayBack = new CombinedBeanForPlayBack(
					object[0] == null ? 0 : Integer.parseInt(String.valueOf(object[0])),
					object[3] == null ? null : String.valueOf(object[3]),
					object[4] == null ? "NA" : String.valueOf(object[4]),
					object[7] == null ? "NA" : String.valueOf(object[7]),
					object[9] == null ? "NA" : String.valueOf(object[9]),
					object[10] == null ? "NA" : String.valueOf(object[10]),
					object[11] == null ? "NA" : String.valueOf(object[11]),
					object[5] == null ? "NA" : String.valueOf(object[5]));

			// export candidate logs and packs
			String candidateLogsFileName = apolloHome + File.separator + RpsConstants.CANDIDATE_PLAY_BACK_SCOREREPORTS_FOLDER
					+ File.separator + RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator
					+ RpsConstants.CANDIDATE_LOGS_PLAY_BACK_AUDIT_LOG;
			
			String candidateQpackFileName = apolloHome + File.separator + RpsConstants.CANDIDATE_PLAY_BACK_SCOREREPORTS_FOLDER
					+ File.separator + RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator
					+ RpsConstants.CANDIDATE_LOGS_PLAY_BACK_QPACKS;
			
			this.exportCandidateLogsForPlayBack(combinedBeanForPlayBack, qPacksDelivered, candidateLogsFileName,
					candidateQpackFileName);
			qPacksDelivered.add(combinedBeanForPlayBack.getPackId());

			cDEDetailsForPlayBack.setBatchCode(combinedBeanForPlayBack.getBatchCode());
			cDEDetailsForPlayBack.setEventCode(combinedBeanForPlayBack.getEventId());
			cDEDetailsForPlayBack.setCustomerCode(combinedBeanForPlayBack.getCustomerCode());
			cDEDetailsForPlayBack.setDivisionCode(combinedBeanForPlayBack.getDivisionCode());

			try {
				// SELECT ma.uniqueCandidateId,ma.loginID,ma.loginPwd,ma.candidateLogs,qpp.packId,rc.candidateId1 as
				// applicationNumber, qp.qpCode,
				// qpp.eventId,mif.mifFormJson,b.batchCode,rcu.customerCode,rd.divisionCode,
				// rc.firstName,rc.middleName,rc.lastName,rc.dob,res.response
				cryptUtil=new CryptUtil(128);
				candidateDetail = new CandidateDetailsForPlayBack(object[1] == null ? "NA" : String.valueOf(object[1]),
						cryptUtil.decryptTextUsingAES(object[2] == null ? "NA" : String.valueOf(object[2]),
								cDEDetailsForPlayBack.getEventCode()),
						object[6] == null ? "NA" : String.valueOf(object[6]),
						object[4] == null ? "NA" : String.valueOf(object[4]),
						object[5] == null ? "NA" : String.valueOf(object[5]) + RpsConstants.LOG,
						object[8] == null ? "NA" : String.valueOf(object[8]),
						object[12] == null ? "NA" : String.valueOf(object[12]),
						object[14] == null ? "NA" : String.valueOf(object[14]),
						object[16] == null ? "NA" : String.valueOf(object[16]),
						object[5] == null ? "NA" : String.valueOf(object[5]));
			} catch (NumberFormatException | ApolloSecurityException e) {
				e.printStackTrace();
				logger.error("exception gererated when decrypting text : "+e);
			}
			candidateDetails.add(candidateDetail);
		}

		cDEDetailsForPlayBack.setCandidateDetailsForTest(candidateDetails);
		File file = new File(apolloHome + File.separator + RpsConstants.CANDIDATE_PLAY_BACK_SCOREREPORTS_FOLDER + File.separator
				+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE);
		try {
			FileUtils.writeStringToFile(
					new File(apolloHome + File.separator + RpsConstants.CANDIDATE_PLAY_BACK_SCOREREPORTS_FOLDER + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator + "metaData.json"),
					new Gson().toJson(cDEDetailsForPlayBack));

			PlayBackResponseBean playBackResponseBean = new PlayBackResponseBean();
			// ZipUtility.archiveFiles(file.toArray(new
			// String[file.size()],destinationPath);
			this.zipFolder(file.getAbsolutePath());

			String reportURL = rpsDownloadUrl + FilenameUtils.getName(file.getName() + RpsConstants.ZIP);
			playBackResponseBean.setFilepath(reportURL);
			playBackResponseBean.setPassword(playbackPassword);
			this.publishPlayBackDetails(playBackResponseBean);
		} catch (IOException e) {
			
			logger.error("exception gererated when publishPlayBackDetails  : "+e);
			e.printStackTrace();
		} finally {

		//	File decryptedFile = new File(file.getAbsolutePath() + RpsConstants.ZIP);
		//	decryptedFile.delete();
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				logger.error("exception gererated when deleting directory  : "+e);
				e.printStackTrace();
			}

		}

		logger.info("---OUT--- getAllDetailsForPlayBackBasedOnDate()");
	}

	// export candidate Audit Logs
	public String exportCandidateLogsForPlayBack(CombinedBeanForPlayBack combinedBeanForPlayBack,
			Set<String> qPacksDelivered, String candidateLogsFileName, String candidateQpackFileName) {
		logger.info("---IN--- exportCandidateLogsForPlayBack()");
		String logs = logViewerService.exportCandidateLogsForPlayBack(combinedBeanForPlayBack, qPacksDelivered,
				candidateLogsFileName, candidateQpackFileName);
		logger.info("---OUT--- exportCandidateLogsForPlayBack()");
		return logs;
	}

	public String zipFolder(String folder) throws IOException {
		logger.info("---IN--- zipFolder()");
		ZipUtility.archiveFiles(new String[] { folder }, folder);
		logger.info("---OUT--- zipFolder()");
		return folder + RpsConstants.ZIP;
	}

	public boolean publishPlayBackDetails(final PlayBackResponseBean playBackResponseBean) {
		logger.info("---In--- publishPlayBackDetails()");
		// Build headers.
		final HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		// Build request body.
		final HttpEntity<String> body = new HttpEntity<String>(new Gson().toJson(playBackResponseBean), headers);
		logger.info("play back response we forwarding :"+body.getBody());
		// Build URI.
		final String uri = playbackUri;
		// Get response.
		RestTemplate restTemplate = new RestTemplate();
		final ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.POST, body, String.class);
		logger.info("Response from tp  :" + response.getStatusCode() + " complete response :" + response);
		logger.info("---OUT--- publishPlayBackDetails()");
		return true;
	}

	public String generatePlayBackZipForCandidate(String assessmentCode, String loginID) {
		logger.info("---IN--- generatePlayBackZipForCandidate()");
		List<Object[]> rpsMasterAssociation = null;
		CDEDetailsForPlayBack cDEDetailsForPlayBack = new CDEDetailsForPlayBack();
		Set<CandidateDetailsForPlayBack> candidateDetails = new HashSet<CandidateDetailsForPlayBack>();
		CandidateDetailsForPlayBack candidateDetail = null;
		CombinedBeanForPlayBack combinedBeanForPlayBack = null;
		Set<String> qPacksDelivered = new HashSet<String>();
		rpsMasterAssociation =
				rpsMasterAssociationService.findPlayBackDetailsByUniqueCandidateID(assessmentCode, loginID);
		// Integer uniqueCandidateId, String candidateLogs, String packId, String eventId,
		// String batchCode, String customerCode, String divisionCode//String auditFileName
		int uniqueCandidateId = 0;
		for (Object[] object : rpsMasterAssociation) {
			uniqueCandidateId = (object[0] == null ? 0 : Integer.parseInt(String.valueOf(object[0])));
			combinedBeanForPlayBack =
					new CombinedBeanForPlayBack(uniqueCandidateId,
							object[3] == null ? null : String.valueOf(object[3]),
							object[4] == null ? "NA" : String.valueOf(object[4]),
							object[7] == null ? "NA" : String.valueOf(object[7]),
							object[9] == null ? "NA" : String.valueOf(object[9]),
							object[10] == null ? "NA" : String.valueOf(object[10]),
							object[11] == null ? "NA" : String.valueOf(object[11]),
							object[5] == null ? "NA" : String.valueOf(object[5]));

			// export candidate logs and packs
			String candidateLogsFileName =
					apolloHome + File.separator + "ScoreReports" + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS + File.separator
							+ uniqueCandidateId + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator
							+ RpsConstants.CANDIDATE_LOGS_PLAY_BACK_AUDIT_LOG;

			String candidateQpackFileName =
					apolloHome + File.separator + "ScoreReports" + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS + File.separator
							+ uniqueCandidateId + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator
							+ RpsConstants.CANDIDATE_LOGS_PLAY_BACK_QPACKS;

			this.exportCandidateLogsForPlayBack(combinedBeanForPlayBack, qPacksDelivered, candidateLogsFileName,
					candidateQpackFileName);
			qPacksDelivered.add(combinedBeanForPlayBack.getPackId());

			cDEDetailsForPlayBack.setBatchCode(combinedBeanForPlayBack.getBatchCode());
			cDEDetailsForPlayBack.setEventCode(combinedBeanForPlayBack.getEventId());
			cDEDetailsForPlayBack.setCustomerCode(combinedBeanForPlayBack.getCustomerCode());
			cDEDetailsForPlayBack.setDivisionCode(combinedBeanForPlayBack.getDivisionCode());

			try {
				// SELECT ma.uniqueCandidateId,ma.loginID,ma.loginPwd,ma.candidateLogs,qpp.packId,rc.candidateId1 as
				// applicationNumber, qp.qpCode,
				// qpp.eventId,mif.mifFormJson,b.batchCode,rcu.customerCode,rd.divisionCode,
				// rc.firstName,rc.middleName,rc.lastName,rc.dob,res.response
				cryptUtil = new CryptUtil(128);
				candidateDetail = new CandidateDetailsForPlayBack(object[1] == null ? "NA" : String.valueOf(object[1]),
						cryptUtil.decryptTextUsingAES(object[2] == null ? "NA" : String.valueOf(object[2]),
								cDEDetailsForPlayBack.getEventCode()),
						object[6] == null ? "NA" : String.valueOf(object[6]),
						object[4] == null ? "NA" : String.valueOf(object[4]),
						object[5] == null ? "NA" : String.valueOf(object[5]) + RpsConstants.LOG,
						object[8] == null ? "NA" : String.valueOf(object[8]),
						object[12] == null ? "NA" : String.valueOf(object[12]),
						object[14] == null ? "NA" : String.valueOf(object[14]),
						object[16] == null ? "NA" : String.valueOf(object[16]),
						object[5] == null ? "NA" : String.valueOf(object[5]));
			} catch (NumberFormatException | ApolloSecurityException e) {
				e.printStackTrace();
				logger.error("exception gererated when decrypting text : " + e);
			}
			candidateDetails.add(candidateDetail);
		}

		cDEDetailsForPlayBack.setCandidateDetailsForTest(candidateDetails);
		File file = new File(apolloHome + File.separator + "ScoreReports" + File.separator
				+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS + File.separator
				+ uniqueCandidateId + File.separator
				+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE);
		try {
			FileUtils.writeStringToFile(
					new File(apolloHome + File.separator + "ScoreReports" + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS + File.separator
							+ uniqueCandidateId + File.separator
							+ RpsConstants.CANDIDATE_PLAY_BACK_DETAILS_RESPONSE + File.separator + "metaData.json"),
					new Gson().toJson(cDEDetailsForPlayBack));

			PlayBackResponseBean playBackResponseBean = new PlayBackResponseBean();
			this.zipFolder(file.getAbsolutePath());
			/// apollo/apollo_home/rps/ScoreReports/PlayBack/120881
			String reportURL = rpsDownloadUrl + RpsConstants.CANDIDATE_PLAY_BACK_DETAILS + "/" + uniqueCandidateId + "/"
					+ FilenameUtils.getName(file.getAbsolutePath() + RpsConstants.ZIP);
			playBackResponseBean.setFilepath(reportURL);// (file.getAbsolutePath() + ".zip");
			playBackResponseBean.setPassword(playbackPassword);
			return new Gson().toJson(playBackResponseBean);
		} catch (IOException e) {
			logger.error("exception gererated when publishPlayBackDetails  : " + e);
			e.printStackTrace();
		} finally {
			try {
				FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				logger.error("exception gererated when deleting directory  : " + e);
				e.printStackTrace();
			}

		}

		logger.info("---OUT--- generatePlayBackZipForCandidate()");
		return null;
	}
}
