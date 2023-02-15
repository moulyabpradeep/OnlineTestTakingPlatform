package com.merittrac.apollo.rps.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTimeComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.entities.acs.ACSAuditEnum;
import com.merittrac.apollo.common.entities.acs.AttendanceExportEntity;
import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.common.entities.acs.CandidateSectionLanguagesDO;
import com.merittrac.apollo.common.entities.acs.FeedBackResponse;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.bean.CandidateLoginDetail;
import com.merittrac.apollo.data.bean.CombinedBeanForPlayBack;
import com.merittrac.apollo.data.bean.RpsAcsDetail;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsBrLr;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateMIFDetails;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsCandidateResponseLite;
import com.merittrac.apollo.data.entity.RpsCandidateResponseLiteQP;
import com.merittrac.apollo.data.entity.RpsMasterAssociation;
import com.merittrac.apollo.data.entity.RpsMasterAssociationLiteForCandidateAudits;
import com.merittrac.apollo.data.entity.RpsMasterAssociationLiteForCandidateFeedback;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsQuestionPaperPack;
import com.merittrac.apollo.data.entity.RpsReportLog;
import com.merittrac.apollo.data.entity.RpsRpackComponent;
import com.merittrac.apollo.data.repository.RpsCandidateResponseRepository;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsCandidateMIFDetailsService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsCandidateService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.data.service.RpsQpTemplateService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsQuestionService;
import com.merittrac.apollo.data.service.RpsReportLogService;
import com.merittrac.apollo.data.service.RpsRpackComponentService;
import com.merittrac.apollo.data.service.RpsVenueService;
import com.merittrac.apollo.qpd.qpgenentities.LayoutRulesExportEntity;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.reportexport.service.ExportToExcelService;
import com.merittrac.apollo.rps.ui.entity.AdminAuditLogViewerEntity;
import com.merittrac.apollo.rps.ui.entity.AttendanceViewerEntity;
import com.merittrac.apollo.rps.ui.entity.CandidateFeedBackEntity;
import com.merittrac.apollo.rps.ui.entity.CandidateLogsViewEntity;
import com.merittrac.apollo.rps.ui.entity.CityVenueAcsEntity;
import com.merittrac.apollo.rps.ui.entity.EODLogViewerEntity;
import com.merittrac.apollo.rps.ui.entity.ExportFileEntity;
import com.merittrac.apollo.rps.ui.entity.ExportTwoFilesEntity;
import com.merittrac.apollo.rps.ui.entity.FeedBackFormJson;
import com.merittrac.apollo.rps.ui.entity.FeedBackFormJsonSMU;
import com.merittrac.apollo.rps.ui.entity.FeedBackItem;
import com.merittrac.apollo.rps.ui.entity.FeedBackItems;
import com.merittrac.apollo.rps.ui.entity.MalpracticeReportLogsEntity;
import com.merittrac.apollo.rps.ui.entity.RPSCandidateFeedBackEntity;
import com.merittrac.apollo.rps.ui.entity.RPSCandidateQuestionFeedBackEntity;
import com.merittrac.apollo.rps.ui.entity.ReportLogViewerEntity;
import com.merittrac.apollo.rps.ui.entity.ResponseCandQuestionEntity;
import com.merittrac.apollo.rps.ui.entity.ResponseStatus;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;

import au.com.bytecode.opencsv.CSVWriter;

public class LogViewerService {

	private static Logger logger = LoggerFactory.getLogger(LogViewerService.class);
	private static CryptUtil cryptUtil = new CryptUtil();

	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	@Autowired
	RpsBatchService rpsBatchService;

	@Autowired
	ExcelExportService excelExportService;

	@Autowired
	RpsCandidateMIFDetailsService rpsCandidateMIFDetailsService;

	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsCandidateResponseRepository rpsCandidateResponseRepository;

	@Autowired
	RpsPackService rpsPackService;

	@Autowired
	Gson gson;

	@Autowired
	RpsCandidateService rpsCandidateService;

	@Autowired
	RpsCandidateResponseService rpsCandidateResponseService;

	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;

	@Autowired
	RpsRpackComponentService rpsRpackComponentService;

	@Autowired
	RpsQpTemplateService rpsQpTemplateService;

	@Autowired
	RpsAcsServerServices rpsAcsServerServices;

	@Autowired
	RpsReportLogService rpsReportLogService;

	@Autowired
	AttendanceExtractService attendanceExtractService;

	@Autowired
	RpsAttendanceReconciliationService rpsAttendanceReconciliationService;

	@Autowired
	RpsQuestionService rpsQuestionService;

	@Autowired
	RpsVenueService rpsVenueService;

	@Autowired
	RpsEventService rpsEventService;

	@Autowired
	ExportToExcelService exportToExcelService;

	@Value("${apollo_home_dir}")
	private String apolloHome;

	@Value("${isCandAuditEncrypted}")
	private String isCandAuditEncrypted;

	@Value("${CandAuditDelimiterForSwitchOptionActions}")
	private String candAuditDelimiterForSwitchOptionActions;

	private final String exportFolder = "export";

	public String getAttendanceLogViewerGrid(String acsCode, String startDate, String endDate) {

		logger.info("---IN--- attendanceLogViewer()");
		List<AttendanceViewerEntity> attendanceViewerEntitiesList = new ArrayList<>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(attendanceViewerEntitiesList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(attendanceViewerEntitiesList);
		}

		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
				.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);
		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
					+ " - " + endDate + " }");
			return gson.toJson(attendanceViewerEntitiesList);
		}

		attendanceViewerEntitiesList = fetchAttendanceViewerEntities(rpsBatchAcsAssociationsList);
		logger.info("---OUT--- attendanceLogViewer()");
		return gson.toJson(attendanceViewerEntitiesList);
	}

	/**
	 * @param acsCode
	 * @param startDate
	 * @param endDate
	 * @return
	 */

	public String getIncidentAuditLogViewerGrid(String acsCode, String startDate, String endDate) {
		logger.info("-- IN--- IncidentAuditLogViewer--------");
		List<ReportLogViewerEntity> reportLogViewerEntityList = new ArrayList<ReportLogViewerEntity>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(reportLogViewerEntityList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();

		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(reportLogViewerEntityList);
		}

		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
				.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);
		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
					+ " - " + endDate + " }");
			return gson.toJson(reportLogViewerEntityList);
		}

		reportLogViewerEntityList = this.getIncidentAuditLogViewerEntity(rpsBatchAcsAssociationsList);
		logger.info("-----OUT---getIncidentAuditLogViewerGrid-----");
		return gson.toJson(reportLogViewerEntityList);
	}

	public String getAdminAuditLogViewerGrid(String acsCode, String startDate, String endDate) {
		logger.info("-- IN--- AdminAuditLogViewer--------");
		Set<AdminAuditLogViewerEntity> adminAuditLogViewerEntitieList = new HashSet<AdminAuditLogViewerEntity>();
		Map<String, Integer> map = new HashMap<String, Integer>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(adminAuditLogViewerEntitieList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();

		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(adminAuditLogViewerEntitieList);
		}

		boolean sameDateRange = false;
		Calendar rangeStDate = (Calendar) rangeStartDate.clone();
		Calendar rangeMidDate = (Calendar) rangeStDate.clone();
		rangeMidDate.set(Calendar.HOUR_OF_DAY, 23);
		rangeMidDate.set(Calendar.MINUTE, 59);
		rangeMidDate.set(Calendar.SECOND, 59);
		while (true) {
			if (DateTimeComparator.getDateOnlyInstance().compare(rangeStDate, rangeEndDate) == 0
					|| DateTimeComparator.getDateOnlyInstance().compare(rangeMidDate, rangeEndDate) == 0) {
				sameDateRange = true;
			}

			List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStDate, rangeMidDate);

			if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
				logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
						+ " - " + endDate + " }");
			}

			map = this.getAdminAuditLogViewerEntity(rpsBatchAcsAssociationsList, rangeStDate);

			Set<String> set = map.keySet();
			for (String date : set) {
				AdminAuditLogViewerEntity adminAuditLogViewerEntity = new AdminAuditLogViewerEntity();
				// adminAuditLogViewerEntity.setCurrentDate(this.convertStringToDateWithoutTime(date));
				adminAuditLogViewerEntity.setDate(date);
				adminAuditLogViewerEntity.setRpackComponentId(map.get(date));
				adminAuditLogViewerEntitieList.add(adminAuditLogViewerEntity);
			}

			rangeMidDate.add(Calendar.DATE, 1);
			if (sameDateRange) {
				sameDateRange = false;
				break;
			} else
				rangeStDate.add(Calendar.DATE, 1);
		}

		return gson.toJson(adminAuditLogViewerEntitieList);
	}

	/**
	 * @param date
	 * @return
	 */

	private String convertTimeToStringWithoutTime(Date date) {

		String time = "";
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITHOUT_TIME);
		if (date != null)
			time = sdf.format(date);
		return time;
	}

	/**
	 * @param rpsBatchAcsAssociationsList
	 * @return
	 */
	private Map<String, Integer> getAdminAuditLogViewerEntity(List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList,
			Calendar rangeStartdate) {
		Map<String, Integer> componentMap = new HashMap<String, Integer>();

		List<AdminAuditLogViewerEntity> adminViewerEntities = new ArrayList<AdminAuditLogViewerEntity>();

		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {

			List<RpsPack> rpPackList = rpsPackService.getListOfRpsPackByAssociationAndPackType(rpsBatchAcsAssociation,
					RpsConstants.PackType.RPACK.toString());
			List<RpsPack> deltaRpsPackList = rpsPackService.getListOfRpsPackByAssociationAndPackType(
					rpsBatchAcsAssociation, RpsConstants.PackType.DELTA_RPACK.toString());

			if (rpPackList != null) {
				for (RpsPack rpsPack : rpPackList) {
					RpsRpackComponent rpsRpackComponent = rpsRpackComponentService.getComponentByPackIdAndTypeAndStatus(
							rpsPack.getPid(), RpsConstants.RpackComponents.ADMIN_LOG.toString(),
							RpsConstants.packStatus.PACKS_RECEIVED.toString());
					if (rpsRpackComponent != null) {
						AdminAuditLogViewerEntity auditLogViewerEntity = new AdminAuditLogViewerEntity();
						auditLogViewerEntity.setRpackComponentId(rpsRpackComponent.getRpackComponentId());
						auditLogViewerEntity.setCurrentDate(rpsRpackComponent.getCreationDate());
						adminViewerEntities.add(auditLogViewerEntity);
					}
				}
			}

			if (deltaRpsPackList != null) {
				for (RpsPack rpsPack : deltaRpsPackList) {
					RpsRpackComponent rpsRpackComponent = rpsRpackComponentService.getComponentByPackIdAndTypeAndStatus(
							rpsPack.getPid(), RpsConstants.RpackComponents.ADMIN_LOG.toString(),
							RpsConstants.packStatus.PACKS_RECEIVED.toString());
					if (rpsRpackComponent != null) {
						AdminAuditLogViewerEntity auditLogViewerEntity = new AdminAuditLogViewerEntity();
						auditLogViewerEntity.setRpackComponentId(rpsRpackComponent.getRpackComponentId());
						auditLogViewerEntity.setCurrentDate(rpsRpackComponent.getCreationDate());
						adminViewerEntities.add(auditLogViewerEntity);
					}
				}
			}

			Collections.sort(adminViewerEntities);
			if (!adminViewerEntities.isEmpty()) {
				AdminAuditLogViewerEntity auditLogViewerEntity = adminViewerEntities.get(0);
				String stringDate = this.convertTimeToStringWithoutTime(rangeStartdate.getTime());
				if (auditLogViewerEntity != null) {
					componentMap.put(stringDate, auditLogViewerEntity.getRpackComponentId());

				}
			}
		}
		return componentMap;
	}

	private List<ReportLogViewerEntity>
			getIncidentAuditLogViewerEntity(List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList) {
		List<ReportLogViewerEntity> reportLogViewerEntityList = new ArrayList<ReportLogViewerEntity>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {
			List<RpsRpackComponent> rpsRpackComponentList =
					rpsRpackComponentService.getComponentByBatchAcsAssoAndTypeAndStatus(rpsBatchAcsAssociation,
							RpsConstants.RpackComponents.INCIDENTAUDIT_LOG.toString(),
							RpsConstants.packStatus.PACKS_RECEIVED.toString());
			if (rpsRpackComponentList == null || rpsRpackComponentList.isEmpty()) {
				// this batch acs doesn't have Incident logs so continue for next bacth- acs
				continue;
			}
			ReportLogViewerEntity reportLogViewerEntity = new ReportLogViewerEntity();
			reportLogViewerEntity.setBatchAcsId(rpsBatchAcsAssociation.getBatchAcsId());
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			reportLogViewerEntity.setBatchCode(rpsBatch.getBatchCode());
			reportLogViewerEntity.setBatchStartTime(convertTimeToString(rpsBatch.getBatchStartTime().getTime()));
			reportLogViewerEntity.setBatchEndTime(this.getBatchEndTime(rpsBatch, rpsBatchAcsAssociation));

			reportLogViewerEntityList.add(reportLogViewerEntity);
		}
		return reportLogViewerEntityList;
	}

	private List<AttendanceViewerEntity>
			fetchAttendanceViewerEntities(List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList) {
		logger.info("---IN--- fetchAttendanceViewerEntities()");
		List<AttendanceViewerEntity> attendanceViewerEntities = new ArrayList<>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {
			boolean isAttReportAvailable = true;
			AttendanceViewerEntity attendanceViewerEntity = new AttendanceViewerEntity();
			attendanceViewerEntity.setBatchAcsId(rpsBatchAcsAssociation.getBatchAcsId());
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			attendanceViewerEntity.setBatchCode(rpsBatch.getBatchCode());
			attendanceViewerEntity.setBatchStartTime(convertTimeToString(rpsBatch.getBatchStartTime().getTime()));
			attendanceViewerEntity.setBatchEndTime(this.getBatchEndTime(rpsBatch, rpsBatchAcsAssociation));
			if (rpsBatchAcsAssociation.getAttendanceDetails() == null)
				isAttReportAvailable = false;
			this.setCandidatePresenceAbsenceStatus(attendanceViewerEntity, rpsBatchAcsAssociation,
					isAttReportAvailable);
			attendanceViewerEntities.add(attendanceViewerEntity);
		}
		logger.info("---OUT--- fetchAttendanceViewerEntities()");
		return attendanceViewerEntities;
	}

	private void setCandidatePresenceAbsenceStatus(AttendanceViewerEntity attendanceViewerEntity,
			RpsBatchAcsAssociation rpsBatchAcsAssociation, boolean isAttReportAvailable) {

		logger.info("---IN--- setCandidatePresenceAbsenceStatus()");
		// List<RpsMasterAssociation> rpsMasterAssociationsList=
		// rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
		List<Boolean> rpsList = rpsMasterAssociationService
				.findISPresentByrpsBatchAcsAssociationID(rpsBatchAcsAssociation.getBatchAcsId());
		int presentCount = 0;
		int absentCount = 0;
		/*
		 * if(rpsMasterAssociationsList!=null && !rpsMasterAssociationsList.isEmpty()){ for(RpsMasterAssociation
		 * rpsMasterAssociation: rpsMasterAssociationsList){ if(rpsMasterAssociation.isPresent()) presentCount++; else
		 * absentCount++; } }
		 */

		if (rpsList != null && !rpsList.isEmpty()) {
			for (Boolean isPresent : rpsList) {
				if (isPresent)
					presentCount++;
				else
					absentCount++;
			}
		}

		if (isAttReportAvailable) {
			attendanceViewerEntity.setPresentCount(presentCount + "");
			attendanceViewerEntity.setAbsentCount(absentCount + "");
		} else {
			attendanceViewerEntity.setAbsentCount("SCHEDULED");
			attendanceViewerEntity.setPresentCount("SCHEDULED");
		}

		logger.info("---OUT--- setCandidatePresenceAbsenceStatus()");
	}

	private static Map<String, Integer>
			getCandidatePresenceAbsenceStatus(RpsBatchAcsAssociation rpsBatchAcsAssociation) {
		Map<String, Integer> presentAbsentMap = new HashMap<>();
		logger.info("---IN--- getCandidatePresenceAbsenceStatus()");
		Type response = new TypeToken<AttendanceExportEntity>() {
		}.getType();
		if (rpsBatchAcsAssociation.getAttendanceDetails() != null) {
			AttendanceExportEntity responseMarkBeans =
					new Gson().fromJson(rpsBatchAcsAssociation.getAttendanceDetails(), response);
			Integer presentCount = 0;
			Integer absentCount = 0;

			for (AttendanceReportEntity attendanceReportEntity : responseMarkBeans.getAttendanceReportEntities()) {
				if (attendanceReportEntity.getIsPresent().contains("Y")) {
					presentCount++;
				} else
					absentCount++;
			}

			presentAbsentMap.put("PRESENT", presentCount);
			presentAbsentMap.put("ABSENT", absentCount);
			return presentAbsentMap;
		}
		logger.info("---OUT--- setCandidatePresenceAbsenceStatus()");
		return null;
	}

	private String getBatchEndTime(RpsBatch rpsBatch, RpsBatchAcsAssociation rpsBatchAcsAssociation) {
		Date batchEndTime = rpsBatch.getBatchEndTime().getTime();

		if (rpsBatchAcsAssociation.getBatchExtensionTime() != null)
			batchEndTime = new Date((rpsBatchAcsAssociation.getBatchExtensionTime() * 60000) + batchEndTime.getTime());

		return convertTimeToString(batchEndTime);
	}

	private String getBatchEndTime(Calendar batchEndTimeCal, Integer batchExtensionTime) {
		Date batchEndTime = batchEndTimeCal.getTime();

		if (batchExtensionTime != null)
			batchEndTime = new Date((batchExtensionTime * 60000) + batchEndTime.getTime());

		return convertTimeToString(batchEndTime);
	}

	private String convertTimeToString(Date date) {

		String time = "";
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		if (date != null)
			time = sdf.format(date);
		return time;
	}

	public String viewAttendanceLogs(Integer batchAcsId) {
		logger.info("---IN--- viewAttendanceLogs()");
		String json = null;
		if (batchAcsId == null) {
			logger.error("Missing mandatory agruments-- batchAcsId : " + batchAcsId);
			return json;
		}

		RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.find(batchAcsId);
		if (rpsBatchAcsAssociation == null) {
			logger.error("There is no RpsBatchAcsAssociation present in database for batchAcsId :" + batchAcsId);
			return json;
		}

		json = rpsBatchAcsAssociation.getAttendanceDetails();
		if (json == null) {
			json = RpsConstants.EMPTY_JSON;
			return json;
		}

		Type mapType1 = new TypeToken<AttendanceExportEntity>() {
		}.getType();
		gson = new GsonBuilder().serializeNulls().create();
		AttendanceExportEntity attendanceExportEntity = gson.fromJson(json, mapType1);
		List<AttendanceReportEntity> attendanceReportEntityList = attendanceExportEntity.getAttendanceReportEntities();
		// attendanceReportEntityList = attendanceExtractService.adjustNullValuesInReport(attendanceReportEntityList);
		json = gson.toJson(attendanceReportEntityList);
		logger.info("---OUT--- viewAttendanceLogs()");
		return json;
	}

	public List<AttendanceReportEntity> getAttendanceLogsByBatchAcsId(Integer batchAcsId) {
		logger.info("---IN--- viewAttendanceLogs()");
		String json = null;
		if (batchAcsId == null) {
			logger.error("Missing mandatory agruments-- batchAcsId : " + batchAcsId);
			return null;
		}

		RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.find(batchAcsId);
		if (rpsBatchAcsAssociation == null) {
			logger.error("There is no RpsBatchAcsAssociation present in database for batchAcsId :" + batchAcsId);
			return null;
		}

		json = rpsBatchAcsAssociation.getAttendanceDetails();
		if (json == null) {
			json = RpsConstants.EMPTY_JSON;
			return null;
		}

		Type mapType1 = new TypeToken<AttendanceExportEntity>() {
		}.getType();
		gson = new GsonBuilder().serializeNulls().create();
		AttendanceExportEntity attendanceExportEntity = gson.fromJson(json, mapType1);
		return attendanceExportEntity.getAttendanceReportEntities();

	}

	public String exportAttendanceLogs(Integer batchAcsId) {
		logger.info("---IN--- exportAttendanceLogs()");
		ExportFileEntity exportFileEntity = null;
		List<AttendanceReportEntity> attendanceReportEntities = this.getAttendanceLogsByBatchAcsId(batchAcsId);
		if (attendanceReportEntities == null || attendanceReportEntities.isEmpty()) {
			logger.error("Attendance Logs is empty for batchAcsId :" + batchAcsId);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}
		File excelFilePath = excelExportService.exportAttendanceLogsToExcel(attendanceReportEntities, batchAcsId);
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, excelFilePath.getAbsolutePath());
		logger.info("---OUT--- exportAttendanceLogs()");
		return gson.toJson(exportFileEntity);

	}

	/**
	 * @param batchCode
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	public String exportAcsDetail(String batchCode) throws DocumentException, IOException {
		logger.info("---IN--- exportAcsDetail()");
		ExportFileEntity exportFileEntity = null;
		List<RpsAcsDetail> acsDetailJson =rpsMasterAssociationService.findAcsDetailsByBatchCode(batchCode);;
		File excelFilePath = ExportRpackLogsToExcel.excelGenerator(acsDetailJson, batchCode);
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, excelFilePath.getAbsolutePath());
		logger.info("---OUT--- exportAcsDetail()");
		return gson.toJson(exportFileEntity);

	}

	
	/**
	 * @param batchAcsId
	 * @return
	 * @throws DocumentException
	 * @throws IOException
	 */
	public String exportCandidateLoginDetail(Integer batchAcsId) throws DocumentException, IOException {
		logger.info("---IN--- exportCandidateLoginDetail()");
		ExportFileEntity exportFileEntity = null;
		List<CandidateLoginDetail> candidateLoginJson=rpsMasterAssociationService.findCandidateDetailByBatchAcsid(batchAcsId);;
		CandidateDetailExportToExcel candidateDetailExportToExcel=new CandidateDetailExportToExcel();
		File excelFilePath = candidateDetailExportToExcel.exportCandidateDetail(candidateLoginJson, batchAcsId);
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, excelFilePath.getAbsolutePath());
		logger.info("---OUT--- exportCandidateLoginDetail()");
		return gson.toJson(exportFileEntity);

	}

	/**
	 * @param batchAcsId
	 * @return incidentAuditLog as json
	 */
	public String viewIncidentAuditLogs(Integer batchAcsId) {
		logger.info("---IN--- viewIncidentAuditLogs()");
		String json = null;
		if (batchAcsId == null) {
			logger.error("Missing mandatory agruments-- batchAcsId : " + batchAcsId);
			return json;
		}

		RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.find(batchAcsId);
		if (rpsBatchAcsAssociation == null) {
			logger.error("There is no RpsBatchAcsAssociation present in database for batchAcsId :" + batchAcsId);
			return json;
		}

		json = rpsBatchAcsAssociation.getIncidentAuditDetails();
		logger.info("---OUT--- viewIncidentAuditLogs()");
		return json;
	}

	/**
	 * @param batchAcsId
	 * @return
	 */
	public String exportIncidentAuditLogs(Integer batchAcsId) {
		logger.info("---IN--- exportIncidentAuditLogs()");
		ExportFileEntity exportFileEntity = null;
		String json = this.viewIncidentAuditLogs(batchAcsId);
		if (json == null || json.isEmpty()) {
			logger.info("IncidentAudit Log is empty for batchAcsId :: {} " + batchAcsId);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File logsFolder = new File(
				apolloHome + File.separator + RpsConstants.RPSLOG + File.separator + RpsConstants.INCIDENTAUDIT_LOGS);
		if (!logsFolder.exists())
			logsFolder.mkdirs();

		File logFile =
				new File(logsFolder + File.separator + RpsConstants.INCIDENTAUDIT + batchAcsId + RpsConstants.LOG);
		FileOutputStream fop = null;
		// FileWriter writer;
		try {

			if (json.contains("\n"))
				json = json.replaceAll("\n", "\r\n");
			fop = new FileOutputStream(logFile);
			byte[] byteArray = json.getBytes();

			if (!logFile.exists())
				logFile.createNewFile();

			fop.write(byteArray);
			fop.flush();
			fop.close();

			/*
			 * writer = new FileWriter(logFile); writer.write(json); writer.close();
			 */
		} catch (IOException e) {
			logger.error("Error while writing incident logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e.getLocalizedMessage(), null);
			return gson.toJson(exportFileEntity);
		} finally {
			if (fop != null)
				try {
					fop.close();
				} catch (IOException e) {
					logger.error("ERROR:: while closing the fileOutputStream :: ", e);
				}
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportIncidentAuditLogs()");
		return gson.toJson(exportFileEntity);
	}

	/**
	 * @param rpsComponentID
	 * @return
	 */
	public String viewAdminAuditLogs(Integer rpsComponentID) {
		logger.info("---IN--- viewAdminAuditLogs()");
		String json = null;
		if (rpsComponentID == null) {
			logger.error("Missing mandatory agruments-- rpsComponentID : " + rpsComponentID);
			return json;
		}

		RpsRpackComponent rpsRpackComponent = rpsRpackComponentService.getRpackComponentByID(rpsComponentID);

		if (rpsRpackComponent == null) {
			logger.error("There is no Admin Audit Log present in database for rpsComponentID :" + rpsComponentID);
			return json;
		}
		json = rpsRpackComponent.getAdminAuditDetails();
		logger.info("---OUT--- viewIncidentAuditLogs()");
		return json;
	}

	public String exportAdminAuditLog(Integer rpsComponentID) {

		logger.info("---IN--- exportAdminAuditLogs()");
		ExportFileEntity exportFileEntity = null;
		String json = this.viewAdminAuditLogs(rpsComponentID);
		if (json == null || json.isEmpty()) {
			logger.info("AdminAudit Log is empty for rpsComponentID :: {} " + rpsComponentID);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File logsFolder = new File(
				apolloHome + File.separator + RpsConstants.RPSLOG + File.separator + RpsConstants.ADMINAUDIT_LOGS);
		if (!logsFolder.exists())
			logsFolder.mkdirs();

		File logFile =
				new File(logsFolder + File.separator + RpsConstants.ADMINAUDIT + rpsComponentID + RpsConstants.LOG);
		FileOutputStream fop = null;
		FileWriter writer = null;

		try {
			if (json.contains("\n"))
				json = json.replaceAll("\n", "\r\n");
			fop = new FileOutputStream(logFile);
			byte[] byteArray = json.getBytes();

			if (!logFile.exists())
				logFile.createNewFile();

			fop.write(byteArray);
			fop.flush();
			fop.close();

			/*
			 * writer = new FileWriter(logFile); writer.write(json); writer.close();
			 */
		} catch (IOException e) {
			logger.error("Error while writing Admin Audit logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e.getLocalizedMessage(), null);
			return gson.toJson(exportFileEntity);
		} finally {
			if (fop != null)
				try {
					fop.close();
				} catch (IOException e) {
					logger.error("ERROR:: while closing the fileOutputStream :: ", e);
				}
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportAdminAuditLogs()");
		return gson.toJson(exportFileEntity);

	}

	public String getBatchListByAcsInDateRange(String acsCode, String startDate, String endDate) {
		logger.info("---IN--- getBatchListByAcsInDateRange()");
		Set<String> batchCodes = new LinkedHashSet<>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory parameters--acsCode or startDate or endDate");
			return gson.toJson(batchCodes);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(batchCodes);
		}

		List<RpsBatchAcsAssociation> batchAcsAssoList = rpsBatchAcsAssociationService
				.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);
		if (batchAcsAssoList == null || batchAcsAssoList.isEmpty()) {
			logger.error("There are no batches for acsCode :" + acsCode + " in Date Range {" + startDate + " - "
					+ endDate + " }");
			return gson.toJson(batchCodes);
		}

		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : batchAcsAssoList) {
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			batchCodes.add(rpsBatch.getBatchCode());
		}
		logger.info("---OUT--- getBatchListByAcsInDateRange()");
		return gson.toJson(batchCodes);
	}

	/**
	 * @param eventCode
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public String getBatchListByEventInDateRange(String eventCode, String startDate, String endDate) {
		logger.info("---IN--- getBatchListByEventInDateRange()");
		Set<String> batchCodes = new LinkedHashSet<>();
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory parameters--eventCode or startDate or endDate");
			return gson.toJson(batchCodes);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(batchCodes);
		}

		List<RpsBatchAcsAssociation> batchAcsAssoList = rpsBatchAcsAssociationService
				.getAllBatchIdByEventCodeAndInDateRange(eventCode, rangeStartDate, rangeEndDate);
		if (batchAcsAssoList == null || batchAcsAssoList.isEmpty()) {
			logger.error("There are no batches for eventCode :" + eventCode + " in Date Range {" + startDate + " - "
					+ endDate + " }");
			return gson.toJson(batchCodes);
		}

		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : batchAcsAssoList) {
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			batchCodes.add(rpsBatch.getBatchCode());
		}
		logger.info("---OUT--- getBatchListByEventInDateRange()");
		return gson.toJson(batchCodes);
	}

	public String getCandidateLogsGridData(String acsCode, String batchCode, String imageFolder) {
		logger.info("---IN--- getCandidateLogsGridData()");
		List<CandidateLogsViewEntity> candidateLogsViewEntities = new ArrayList<>();
		if (batchCode == null || batchCode.isEmpty() || acsCode == null || acsCode.isEmpty()) {
			logger.error("Missing mandatory arguments-- batchCode or acsCode");
			return gson.toJson(candidateLogsViewEntities);
		}

		RpsBatchAcsAssociation rpsBatchAcsAssociation =
				rpsBatchAcsAssociationService.getAssociationByBatchCodeAndAcsId(batchCode, acsCode);
		if (rpsBatchAcsAssociation == null) {
			logger.error("There is no batch acs association for batch code :" + batchCode + " and acsCode :" + acsCode);
			return gson.toJson(candidateLogsViewEntities);
		}

		List<RpsMasterAssociation> rpsMasterAssociationsList =
				rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
		if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
			logger.error("No candidate data is available for batch code :" + batchCode + " and acsCode :" + acsCode);
			return gson.toJson(candidateLogsViewEntities);
		}

		candidateLogsViewEntities =
				this.getCandidateLogsViewEntities(rpsBatchAcsAssociation, rpsMasterAssociationsList, imageFolder);
		logger.info("---OUT--- getCandidateLogsGridData()");
		return gson.toJson(candidateLogsViewEntities);
	}

	private List<CandidateLogsViewEntity> getCandidateLogsViewEntities(RpsBatchAcsAssociation rpsBatchAcsAssociation,
			List<RpsMasterAssociation> rpsMasterAssociationsList, String imageFolder) {

		logger.info("---IN--- getCandidateLogsViewEntities()");
		List<CandidateLogsViewEntity> candidateLogsViewEntities = new ArrayList<>();
		Map<Integer, String> qpIdToQPaperMap = new LinkedHashMap<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList) {
			List<RpsCandidateResponse> rpsCandidateResponseList =
					rpsCandidateResponseService.getCandidateResponsesByMasterAsson(rpsMasterAssociation);
			if (rpsCandidateResponseList != null && !rpsCandidateResponseList.isEmpty()) {
				for (RpsCandidateResponse rpsCandidateResponse : rpsCandidateResponseList) {
					CandidateLogsViewEntity candidateLogsViewEntity = new CandidateLogsViewEntity();
					candidateLogsViewEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
					RpsCandidate rpsCandidate =
							rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
					candidateLogsViewEntity.setCandidateName(
							this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId()));
					candidateLogsViewEntity.setCandidateID(rpsCandidate.getCandidateId1());
					candidateLogsViewEntity.setCandidatePhoto(this.getCandidatePhotoPath(rpsCandidate, imageFolder));
					if (rpsMasterAssociation.getLoginID() == null
							|| rpsMasterAssociation.getLoginID().equalsIgnoreCase("null"))
						candidateLogsViewEntity.setLoginID("");
					else
						candidateLogsViewEntity.setLoginID(rpsMasterAssociation.getLoginID());
					candidateLogsViewEntity
							.setAssessmentID(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
					Integer qpId = rpsCandidateResponse.getRpsQuestionPaper().getQpId();
					String qpCode = qpIdToQPaperMap.get(qpId);
					if (qpCode == null) {
						RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
						qpCode = rpsQuestionPaper.getQpCode();
						qpIdToQPaperMap.put(qpId, qpCode);
					}

					candidateLogsViewEntity.setAssessmentSetID(qpCode);
					candidateLogsViewEntities.add(candidateLogsViewEntity);
				}
			}

		}
		logger.info("---OUT--- getCandidateLogsViewEntities()");
		return candidateLogsViewEntities;
	}

	private List<CandidateLogsViewEntity> getCandidateLogsViewEntities(
			List<CandidateLogsViewEntity> candidateLogsViewEntities, RpsBatchAcsAssociation rpsBatchAcsAssociation,
			List<RpsMasterAssociation> rpsMasterAssociationsList, String imageFolder) {

		logger.info("---IN--- getCandidateLogsViewEntities()");
		Map<Integer, String> qpIdToQPaperMap = new LinkedHashMap<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList) {
			List<RpsCandidateResponse> rpsCandidateResponseList =
					rpsCandidateResponseService.getCandidateResponsesByMasterAsson(rpsMasterAssociation);
			if (rpsCandidateResponseList != null && !rpsCandidateResponseList.isEmpty()) {
				for (RpsCandidateResponse rpsCandidateResponse : rpsCandidateResponseList) {
					CandidateLogsViewEntity candidateLogsViewEntity = new CandidateLogsViewEntity();
					candidateLogsViewEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
					RpsCandidate rpsCandidate =
							rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
					candidateLogsViewEntity.setCandidateName(
							this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId()));
					candidateLogsViewEntity.setCandidateID(rpsCandidate.getCandidateId1());
					candidateLogsViewEntity.setCandidatePhoto(this.getCandidatePhotoPath(rpsCandidate, imageFolder));
					if (rpsMasterAssociation.getLoginID() == null
							|| rpsMasterAssociation.getLoginID().equalsIgnoreCase("null"))
						candidateLogsViewEntity.setLoginID("");
					else
						candidateLogsViewEntity.setLoginID(rpsMasterAssociation.getLoginID());
					candidateLogsViewEntity
							.setAssessmentID(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
					Integer qpId = rpsCandidateResponse.getRpsQuestionPaper().getQpId();
					String qpCode = qpIdToQPaperMap.get(qpId);
					if (qpCode == null) {
						RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
						qpCode = rpsQuestionPaper.getQpCode();
						qpIdToQPaperMap.put(qpId, qpCode);
					}

					candidateLogsViewEntity.setAssessmentSetID(qpCode);
					candidateLogsViewEntities.add(candidateLogsViewEntity);
				}
			}

		}
		logger.info("---OUT--- getCandidateLogsViewEntities()");
		return candidateLogsViewEntities;
	}

	private String getCandidatePhotoPath(RpsCandidate rpsCandidate, String imageFolder) {

		String photoPath = "";
		String imageName = rpsCandidate.getImageName();
		byte[] photo = rpsCandidate.getPhoto();
		if (photo != null && photo.length != 0) {
			try {
				File candidatePhotoPath = new File(imageFolder);
				if (!candidatePhotoPath.exists())
					candidatePhotoPath.mkdirs();
				File imageFile = new File(candidatePhotoPath + File.separator + imageName);
				FileOutputStream fout = new FileOutputStream(imageFile);
				fout.write(photo);
				fout.close();
				photoPath = FilenameUtils.separatorsToSystem(imageFile.getAbsolutePath());
			} catch (IOException e) {
				logger.error("Error in reading photo for candidate :" + rpsCandidate.getCandidateId1());
			}
		}

		return photoPath;
	}

	private String getCandidateFullName(RpsCandidate rpsCandidate, Integer uniquecanddiateid) {
		String fullName = "";
		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniquecanddiateid);
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			if (rpsCandidate.getFirstName() != null && !rpsCandidate.getFirstName().isEmpty()) {
				fullName = fullName.concat(rpsCandidate.getFirstName());
			}
			if (rpsCandidate.getMiddleName() != null && !rpsCandidate.getMiddleName().isEmpty()) {
				fullName = fullName.concat(" " + rpsCandidate.getMiddleName());
			}
			if (rpsCandidate.getLastName() != null && !rpsCandidate.getLastName().isEmpty()) {
				fullName = fullName.concat(" " + rpsCandidate.getLastName());
			}
		} else {
			PersonalInfo pInfo = mifForm.getPersonalInfo();
			if (pInfo != null) {
				if (pInfo.getFirstName() != null && !pInfo.getFirstName().isEmpty()) {
					fullName = fullName.concat(pInfo.getFirstName());
				}
				if (pInfo.getMiddleName() != null && !pInfo.getMiddleName().isEmpty()) {
					fullName = fullName.concat(" " + pInfo.getMiddleName());
				}
				if (pInfo.getLastName() != null && !pInfo.getLastName().isEmpty()) {
					fullName = fullName.concat(" " + pInfo.getLastName());
				}
			}
		}
		return fullName;
	}

	public String viewCandidateLogs(Integer uniqueCandidateId) {

		logger.info("---IN--- viewCandidateLogs()");
		String logs = "";
		RpsMasterAssociation rpsMasterAssociation =
				rpsMasterAssociationService.findByUniqueCandidateId(uniqueCandidateId);
		String candidateName = "";
		if (rpsMasterAssociation != null) {
			logs = rpsMasterAssociation.getCandidateLogs();
			RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
			candidateName = this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId());
		}

		String[] lines = logs.split("\\r?\\n");
		if (lines != null && lines.length != 0) {
			// logs= processCandidateLogsContents(lines);
			try {
				logs = processCandidateLogsContentsSMUFFormat(lines, candidateName, rpsMasterAssociation);
			} catch (ApolloSecurityException e) {
				e.printStackTrace();
			}
		}
		logger.info("---OUT--- viewCandidateLogs()");
		return logs;
	}

	private String processCandidateLogsContentsSMUFFormat(String[] lines, String candidateName,
			RpsMasterAssociation rpsMasterAssociation) throws ApolloSecurityException {

		logger.info("---IN--- processCandidateLogsContents()");
		int index = 0;
		String lineSeparator = System.getProperty(RpsConstants.LINE_SEPARATOR);
		List<String> processedLines = new ArrayList<>();
		// ading new headers for SMU requirements
		processedLines.add("Participant Name : " + candidateName);
		processedLines.add("Login ID : " + rpsMasterAssociation.getLoginID());
		processedLines.add("Logged In At : " + this.convertTimeToString(rpsMasterAssociation.getLoginTime()));
		processedLines.add("Test Starts At :" + this.convertTimeToString(rpsMasterAssociation.getTestStartTime()));

		// adding one extra line between header and content
		processedLines.add("");

		// load all ACS specific ActionTypes
		Map<String, List<String>> actionValuesMap = loadAcsActionTypes();
		int step = 0;

		while (index < lines.length) {
			// skip all the headers information
			index = skipCandidateLogsHeader(index, lines);
			// read through all other content of the log

			while (index < lines.length) {
				String line = lines[index++];
				// Getting property flag for decryption isCandAuditEncrypted
				// Delimiter for same kind of actions
				String[] tokensOnSameActions = line.split(candAuditDelimiterForSwitchOptionActions);
				for (String lineToken : tokensOnSameActions) {
					String[] tokens = lineToken.split(RpsConstants.LOG_CONTENT_DELIMITER);
					if (tokens == null || tokens.length == 0) {
						// adding all empty rows
						continue;
					}
					processedLines.add("Step " + ++step + ":");
					line = decodeLogsByLineSMUFormat(tokens, actionValuesMap);
					processedLines.add(line);
				}
			}
		}
		String processedText = StringUtils.join(processedLines, lineSeparator);
		logger.info("---OUT--- processCandidateLogsContents()");
		return processedText;
	}

	private int skipCandidateLogsHeader(int index, String[] lines) {
		String header = "ServerTime";
		while (index < lines.length) {
			// to return body index match header with ClientTime
			if (header.contains("ClientTime"))
				break;

			String line = lines[index++];
			String[] tokens = line.split(RpsConstants.LOG_HEADER_DELIMITER);
			if (tokens == null || tokens.length == 0) {
				// adding all empty rows
				continue;
			}
			header = tokens[0].trim();
		}
		return index;
	}

	private Map<String, List<String>> loadAcsActionTypes() {

		logger.info("---IN--- loadAcsActionTypes()");
		Map<String, List<String>> actionValuesMap = new HashMap<>();
		ACSAuditEnum[] values = ACSAuditEnum.values();
		if (values != null && values.length != 0) {
			for (ACSAuditEnum acsAuditEnum : values) {
				List<String> list = new ArrayList<>();
				list.add(acsAuditEnum.getActionType());
				list.add(acsAuditEnum.getActionMessage());
				actionValuesMap.put(acsAuditEnum.name(), list);
			}
		}
		logger.info("---OUT--- loadAcsActionTypes()");
		return actionValuesMap;
	}

	private String decodeLogsByLineSMUFormat(String[] tokens, Map<String, List<String>> actionValuesMap)
			throws ApolloSecurityException {
		logger.info("---IN--- decodeLogsByLineSMUFormat()");

		// Getting property flag for decryption isCandAuditEncrypted
		String processedLine = "";
		if (tokens.length == RpsConstants.CANDIDATE_LOG_LINE_LENGTH) {
			String logActionType = null, acsServerTime = null;
			for (String token : tokens) {
				if (token.trim().contains("ActionType"))
					logActionType = token;
				if (token.trim().contains("ACSServerTime"))
					acsServerTime = token;
			}
			// String logActionType = tokens[1].trim();
			String logAuditMsgJson = tokens[5].trim();
			String[] actionType = logActionType.split(RpsConstants.LOG_ACTION_DELIMITER);

			String decryptedLogAuditMsgJson = logAuditMsgJson;
			// decrypting logs here isCandAuditEncrypted
			if (isCandAuditEncrypted.equalsIgnoreCase("yes")) {
				decryptedLogAuditMsgJson = cryptUtil.decryptTextUsingAES(logAuditMsgJson,
						RpsConstants.SALT_FOR_ENCRYPTING_AND_DECRYPTING_ADMIN_CREDENTIALS);
			}

			Type type = new TypeToken<Object[]>() {
			}.getType();
			Object[] values = gson.fromJson(decryptedLogAuditMsgJson, type);

			List<String> list = actionValuesMap.get(actionType[1].trim());
			if (list != null && !list.isEmpty() && list.size() == 2) {
				String acsActionType = list.get(0);
				String acsAuditMsg = list.get(1);
				tokens[1] = acsActionType;
				tokens[5] = MessageFormat.format(acsAuditMsg, values);
			}
			String[] actionTime = acsServerTime.split(RpsConstants.LOG_ACTION_DELIMITER, 2);

			// form a string using all the tokens
			processedLine = StringUtils.join(tokens[5], "( Time :", actionTime[1], " )");
		}
		logger.info("---OUT--- decodeLogsByLine()");
		return processedLine;
	}

	public String exportCandidateLogs(Integer uniqueCandidateId, String candidateId) {

		logger.info("---IN--- exportCandidateLogs()");
		String logs = this.viewCandidateLogs(uniqueCandidateId);
		ExportFileEntity exportFileEntity = null;

		if (logs == null || logs.isEmpty()) {
			logger.error("Candidate Logs is empty for uniqueCandidateId :" + uniqueCandidateId);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File candidateLogsFolder = new File(apolloHome + File.separator + RpsConstants.CANDIDATE_LOGS);
		if (!candidateLogsFolder.exists())
			candidateLogsFolder.mkdirs();
		File logFile = new File(candidateLogsFolder + File.separator + candidateId + RpsConstants.LOG);
		try {
			FileOutputStream fout = new FileOutputStream(logFile);
			byte[] bytes = logs.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (IOException e) {
			logger.error("ERROR in writing candidate logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportCandidateLogs()");
		return gson.toJson(exportFileEntity);
	}

	public String getCandidateFeedbackGridData(String acsCode, String batchCode, String imageFolder) {

		logger.info("---IN--- getCandidateFeedbackGridData()");
		List<CandidateLogsViewEntity> candidatFeedbackEntities = new ArrayList<>();
		if (batchCode == null || batchCode.isEmpty() || acsCode == null || acsCode.isEmpty()) {
			logger.error("Missing mandatory arguments-- batchCode or acsCode");
			return gson.toJson(candidatFeedbackEntities);
		}

		RpsBatchAcsAssociation rpsBatchAcsAssociation =
				rpsBatchAcsAssociationService.getAssociationByBatchCodeAndAcsId(batchCode, acsCode);
		if (rpsBatchAcsAssociation == null) {
			logger.error("There is no batch acs association for batch code :" + batchCode + " and acsCode :" + acsCode);
			return gson.toJson(candidatFeedbackEntities);
		}

		List<RpsMasterAssociation> rpsMasterAssociationsList =
				rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
		if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
			logger.error("No candidate data is available for batch code :" + batchCode + " and acsCode :" + acsCode);
			return gson.toJson(candidatFeedbackEntities);
		}

		candidatFeedbackEntities =
				this.getCandidatFeedbackEntities(rpsBatchAcsAssociation, rpsMasterAssociationsList, imageFolder);
		logger.info("---OUT--- getCandidateFeedbackGridData()");
		return gson.toJson(candidatFeedbackEntities);
	}

	private List<CandidateLogsViewEntity> getCandidatFeedbackEntities(RpsBatchAcsAssociation rpsBatchAcsAssociation,
			List<RpsMasterAssociation> rpsMasterAssociationsList, String imageFolder) {

		logger.info("---IN--- getCandidatFeedbackEntities()");
		List<CandidateLogsViewEntity> candidateFeedbackEntities = new ArrayList<>();
		Map<Integer, String> qpIdToQPaperMap = new LinkedHashMap<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList) {
			// if candidate has not given any feedback, move on to any other candidates
			if (rpsMasterAssociation.getCandidateFeedback() == null
					|| rpsMasterAssociation.getCandidateFeedback().isEmpty())
				continue;
			logger.info("rpsMasterAssociation={}", rpsMasterAssociation);
			CandidateLogsViewEntity candidateFeedbackViewEntity = new CandidateLogsViewEntity();
			candidateFeedbackViewEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
			RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
			candidateFeedbackViewEntity.setCandidateName(
					this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId()));
			candidateFeedbackViewEntity.setCandidateID(rpsCandidate.getCandidateId1());
			if (imageFolder != null)
				candidateFeedbackViewEntity.setCandidatePhoto(this.getCandidatePhotoPath(rpsCandidate, imageFolder));
			if (rpsMasterAssociation.getLoginID() == null || rpsMasterAssociation.getLoginID().equalsIgnoreCase("null"))
				candidateFeedbackViewEntity.setLoginID("");
			else
				candidateFeedbackViewEntity.setLoginID(rpsMasterAssociation.getLoginID());
			candidateFeedbackViewEntity.setAssessmentID(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
			List<RpsCandidateResponse> rpsCandidateResponseList =
					rpsCandidateResponseService.getCandidateResponsesByMasterAsson(rpsMasterAssociation);

			if (rpsCandidateResponseList != null && !rpsCandidateResponseList.isEmpty()) {
				RpsCandidateResponse rpsCandidateResponse = rpsCandidateResponseList.get(0);
				Integer qpId = rpsCandidateResponse.getRpsQuestionPaper().getQpId();
				String qpCode = qpIdToQPaperMap.get(qpId);
				if (qpCode == null) {
					RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
					qpCode = rpsQuestionPaper.getQpCode();
					qpIdToQPaperMap.put(qpId, qpCode);
				}
				candidateFeedbackViewEntity.setAssessmentSetID(qpCode);
			}
			logger.info("candidateFeedbackViewEntity={}", candidateFeedbackViewEntity);
			candidateFeedbackEntities.add(candidateFeedbackViewEntity);
			logger.info("candidateFeedbackEntities={}", candidateFeedbackEntities);
		}
		logger.info("---OUT--- getCandidatFeedbackEntities()");
		return candidateFeedbackEntities;
	}

	private List<CandidateLogsViewEntity> getCandidatFeedbackEntities(
			List<CandidateLogsViewEntity> candidateFeedbackEntities, RpsBatchAcsAssociation rpsBatchAcsAssociation,
			List<RpsMasterAssociation> rpsMasterAssociationsList, String imageFolder) {

		logger.info("---IN--- getCandidatFeedbackEntities()");
		Map<Integer, String> qpIdToQPaperMap = new LinkedHashMap<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList) {
			// if candidate has not given any feedback, move on to any other candidates
			if (rpsMasterAssociation.getCandidateFeedback() == null
					|| rpsMasterAssociation.getCandidateFeedback().isEmpty())
				continue;

			CandidateLogsViewEntity candidateFeedbackViewEntity = new CandidateLogsViewEntity();
			candidateFeedbackViewEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
			RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
			candidateFeedbackViewEntity.setCandidateName(
					this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId()));
			candidateFeedbackViewEntity.setCandidateID(rpsCandidate.getCandidateId1());
			if (imageFolder != null)
				candidateFeedbackViewEntity.setCandidatePhoto(this.getCandidatePhotoPath(rpsCandidate, imageFolder));
			if (rpsMasterAssociation.getLoginID() == null || rpsMasterAssociation.getLoginID().equalsIgnoreCase("null"))
				candidateFeedbackViewEntity.setLoginID("");
			else
				candidateFeedbackViewEntity.setLoginID(rpsMasterAssociation.getLoginID());
			candidateFeedbackViewEntity.setAssessmentID(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
			List<RpsCandidateResponse> rpsCandidateResponseList =
					rpsCandidateResponseService.getCandidateResponsesByMasterAsson(rpsMasterAssociation);

			if (rpsCandidateResponseList != null && !rpsCandidateResponseList.isEmpty()) {
				RpsCandidateResponse rpsCandidateResponse = rpsCandidateResponseList.get(0);
				Integer qpId = rpsCandidateResponse.getRpsQuestionPaper().getQpId();
				String qpCode = qpIdToQPaperMap.get(qpId);
				if (qpCode == null) {
					RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
					qpCode = rpsQuestionPaper.getQpCode();
					qpIdToQPaperMap.put(qpId, qpCode);
				}
				candidateFeedbackViewEntity.setAssessmentSetID(qpCode);
			}

			candidateFeedbackEntities.add(candidateFeedbackViewEntity);

		}
		logger.info("---OUT--- getCandidatFeedbackEntities()");
		return candidateFeedbackEntities;
	}

	public String viewCandidateFeedback(Integer uniqueCandidateId, String assessmentID, String assessmentSetID) {

		logger.info("---IN--- viewCandidateLogs()");
		String feedback = "";
		ResponseStatus responseStatus = new ResponseStatus();
		RpsQuestionPaper rpsQuestionPaper =
				rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentID, assessmentSetID);
		if (rpsQuestionPaper == null) {
			logger.error("Question Paper is not available in database for qpCode :" + assessmentSetID);
			responseStatus.setStatus(RpsConstants.FAILED_STATUS);
			responseStatus.setErrorMsg(
					"Question Paper is not available, Please download it before fetching the Candidate Feedback");
			return gson.toJson(responseStatus);
		} else {
			if (rpsQuestionPaper.getQpStatus().equalsIgnoreCase(RpsConstants.QPAPER_STATUS.AVAILABLE.toString())
					|| rpsQuestionPaper.getQpStatus()
							.equalsIgnoreCase(RpsConstants.QPAPER_STATUS.DOWNLOAD_SUCCESSFUL.toString()))
				logger.info("Question Paper is available in database for qpCode :" + assessmentSetID);
			else {
				logger.error("Question Paper is not available in database for qpCode :" + assessmentSetID);
				responseStatus.setStatus(RpsConstants.FAILED_STATUS);
				responseStatus.setErrorMsg(
						"Question Paper is not available, Please download it before fetching the Candidate Feedback");
				return gson.toJson(responseStatus);
			}
		}
		RpsMasterAssociation rpsMasterAssociation =
				rpsMasterAssociationService.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateFeedback() != null) {
			feedback =
					getFeedBackLogsWithDesc(rpsMasterAssociation.getCandidateFeedback(), assessmentID, assessmentSetID);
		}
		logger.info("---OUT--- viewCandidateLogs()");
		return feedback;

	}

	private String getFeedBackLogsWithDesc(String feedBackResponseJson, String assessmentID, String assessmentSetID) {

		logger.info("---IN--- getFeedBackLogsWithDesc()");
		ResponseStatus responseStatus = new ResponseStatus();

		RpsQuestionPaperPack rpsQuestionPaperPack =
				rpsQuestionPaperService.getQpPackByAssessmentAndqpCode(assessmentID, assessmentSetID);
		if (rpsQuestionPaperPack == null) {
			logger.error("Question Paper Pack is not available in database for qpCode :" + assessmentSetID);
			responseStatus.setStatus(RpsConstants.FAILED_STATUS);
			responseStatus.setErrorMsg(
					"Question Paper is not available, Please download it before fetching the Candidate Feedback");
			return gson.toJson(responseStatus);
		}

		RpsBrLr rpsBrLr = rpsQpTemplateService.getRpsBrLrByAssmntAndQpPack(assessmentID, rpsQuestionPaperPack);
		if (rpsBrLr == null) {
			logger.error("There are No Business and Layout Rules available for qpCode :" + assessmentSetID);
			responseStatus.setStatus(RpsConstants.FAILED_STATUS);
			responseStatus.setErrorMsg("Busniness and Layout Rules are not available");
			return gson.toJson(responseStatus);
		}

		String lrRules = rpsBrLr.getLrRules();
		LayoutRulesExportEntity layoutRulesExportEntity = gson.fromJson(lrRules, LayoutRulesExportEntity.class);
		if (layoutRulesExportEntity == null) {
			logger.error("There are No Layout Rules available for qpCode :" + assessmentSetID);
			responseStatus.setStatus(RpsConstants.FAILED_STATUS);
			responseStatus.setErrorMsg("Layout Rules are not available");
			return gson.toJson(responseStatus);
		}

		if (layoutRulesExportEntity.getFeedBackFormJson() == null
				|| layoutRulesExportEntity.getFeedBackFormJson().isEmpty()) {
			logger.error(
					"There are No Candidate Feedback Items available in Layout Rules for qpCode :" + assessmentSetID);
			responseStatus.setStatus(RpsConstants.FAILED_STATUS);
			responseStatus.setErrorMsg(
					"There are No Candidate Feedback Items available in Layout Rules for qpCode :" + assessmentSetID);
			return gson.toJson(responseStatus);
		}

		List<CandidateFeedBackEntity> CandidateFeedBackEntityList =
				processCandidateFeedback(layoutRulesExportEntity.getFeedBackFormJson(), feedBackResponseJson);
		responseStatus.setStatus(RpsConstants.SUCCESS_STATUS);
		responseStatus.setResponseJson(CandidateFeedBackEntityList);
		logger.info("---OUT--- getFeedBackLogsWithDesc()");
		return gson.toJson(responseStatus);
	}

	private List<CandidateFeedBackEntity> processCandidateFeedback(String feedBackFormJson,
			String feedBackResponseJson) {

		logger.info("---IN--- processCandidateFeedback()");
		Map<String, String> feedbackItemsMap = new HashMap<>();
		FeedBackFormJson feedBackFormJsonEntity = gson.fromJson(feedBackFormJson, FeedBackFormJson.class);
		List<CandidateFeedBackEntity> candidateFeedBackEntityList = new ArrayList<>();
		if (feedBackFormJsonEntity.getLayout().contains("SMU")) {
			FeedBackFormJsonSMU feedBackFormJsonSMU = gson.fromJson(feedBackFormJson, FeedBackFormJsonSMU.class);
			for (FeedBackItem feedBackItem : feedBackFormJsonSMU.getFeedBackItems())
				feedbackItemsMap.put(feedBackItem.getItemId(), feedBackItem.getValue());
		} else {
			for (FeedBackItems feedBackItem : feedBackFormJsonEntity.getFeedBackItems()) {
				if (feedBackItem.getOptions() != null)
					feedbackItemsMap.putAll(feedBackItem.getOptions());
			}
		}
		Type type = new TypeToken<List<FeedBackResponse>>() {
		}.getType();
		List<FeedBackResponse> feedBackResponseList = gson.fromJson(feedBackResponseJson, type);
		if (feedBackResponseList != null && !feedBackResponseList.isEmpty()) {
			for (FeedBackResponse feedBackResponse : feedBackResponseList) {
				CandidateFeedBackEntity candidateFeedBackEntity = new CandidateFeedBackEntity();
				candidateFeedBackEntity.setQuestionId(feedBackResponse.getQuestionId());
				List<FeedBackItem> feedBackItems = new ArrayList<>();
				String[] feedBackArray = feedBackResponse.getFeedBack();
				if (feedBackArray != null && feedBackArray.length != 0) {
					for (String feedBack : feedBackArray) {
						FeedBackItem feedBackItem = new FeedBackItem();
						feedBackItem.setItemId(feedBack);
						feedBackItem.setValue(feedbackItemsMap.get(feedBack));
						feedBackItems.add(feedBackItem);
					}
				}
				candidateFeedBackEntity.setFeedBackItems(feedBackItems);
				candidateFeedBackEntityList.add(candidateFeedBackEntity);
			}
		}
		logger.info("---OUT--- processCandidateFeedback()");
		return candidateFeedBackEntityList;
	}

	public String exportCandidateFeedback(Integer uniqueCandidateId, String candidateId, String assessmentID,
			String assessmentSetID) {

		logger.info("---IN--- exportCandidateFeedback()");
		String responseJson = this.viewCandidateFeedback(uniqueCandidateId, assessmentID, assessmentSetID);
		ResponseStatus responseStatus = gson.fromJson(responseJson, ResponseStatus.class);
		ExportFileEntity exportFileEntity = null;
		String feedback = "";
		if (responseStatus.getStatus().equalsIgnoreCase(RpsConstants.FAILED_STATUS)) {
			logger.error(responseStatus.getErrorMsg());
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, responseStatus.getErrorMsg(), null);
			return gson.toJson(exportFileEntity);
		} else {
			feedback = gson.toJson(responseStatus.getResponseJson());
		}

		File candidateLogsFolder = new File(apolloHome + File.separator + RpsConstants.CANDIDATE_FEEDBACK);
		if (!candidateLogsFolder.exists())
			candidateLogsFolder.mkdirs();
		File logFile = new File(candidateLogsFolder + File.separator + candidateId + RpsConstants.LOG);
		try {
			FileOutputStream fout = new FileOutputStream(logFile);
			byte[] bytes = feedback.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (IOException e) {
			logger.error("ERROR in writing candidate feedback", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportCandidateFeedback()");
		return gson.toJson(exportFileEntity);
	}

	/**
	 * @param acsCode
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public String getEODLogViewerGrid(String acsCode, String startDate, String endDate) {
		logger.info("-----IN getEODLogViewerGrid------");
		List<EODLogViewerEntity> eodList = new ArrayList<EODLogViewerEntity>();

		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(eodList);
		}
		RpsAcsServer rpsAcsServer = rpsAcsServerServices.findByAcsServerId(acsCode);

		if (rpsAcsServer == null) {
			logger.error("There is no ACS Code in database where acsCode :: {} ", acsCode);
			return gson.toJson(eodList);
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date sDate = null;
		Date eDate = null;
		String stDate = "";
		String edate = "";
		try {
			sDate = simpleDateFormat.parse(startDate);
			eDate = simpleDateFormat.parse(endDate);
			stDate = simpleDateFormat.format(sDate);
			edate = simpleDateFormat.format(eDate);
		} catch (ParseException e) {
			logger.error("ERROR :: While parsing the date", e);
		}

		String packType = RpsConstants.EOD_REPORT;
		List<RpsReportLog> rpsReportLogs =
				rpsReportLogService.findByAcsAndDateAndPackType(rpsAcsServer, stDate, edate, packType);

		for (RpsReportLog rpsReportLog : rpsReportLogs) {
			EODLogViewerEntity eodLogViewerEntity = new EODLogViewerEntity();
			eodLogViewerEntity.setDate(rpsReportLog.getLogDate());
			eodLogViewerEntity.setReportLogID(rpsReportLog.getReportLogId());
			eodList.add(eodLogViewerEntity);
		}
		logger.info("-----OUT getEODLogViewerGrid------");
		return gson.toJson(eodList);
	}

	/**
	 * @param reportLogId
	 * @return
	 */
	public String viewEODLogs(Integer reportLogId) {
		logger.info("------IN viewEODLogs-----");
		String json = "";
		if (reportLogId == null) {
			logger.error("Missing mandatory agruments-- reportLogId : " + reportLogId);
			return json;
		}

		RpsReportLog rpsReportLog = rpsReportLogService.findRpsReportLogbyID(reportLogId);

		if (rpsReportLog == null) {
			logger.error("There is no EOD Log present in database where reportLogId :: {} ", reportLogId);
			return json;
		}

		json = rpsReportLog.getReportLogDetails();

		logger.info("EOD Logs :: {} ", json);

		logger.info("------OUT viewEODLogs-----");
		return json;
	}

	/**
	 * @param reportLogId
	 * @return
	 */
	public String exportEODLogs(Integer reportLogId) {
		logger.info("---IN--- exportEODLogs()");
		ExportFileEntity exportFileEntity = null;
		String json = this.viewEODLogs(reportLogId);
		if (json == null || json.isEmpty()) {
			logger.info("EOD Log is empty for reportLogId :: {} " + reportLogId);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File logsFolder =
				new File(apolloHome + File.separator + RpsConstants.RPSLOG + File.separator + RpsConstants.EOD_LOGS);
		if (!logsFolder.exists())
			logsFolder.mkdirs();

		File logFile = new File(logsFolder + File.separator + RpsConstants.EOD + reportLogId + RpsConstants.LOG);
		FileOutputStream fop = null;
		// FileWriter writer;
		try {

			fop = new FileOutputStream(logFile);
			byte[] byteArray = json.getBytes();

			if (!logFile.exists())
				logFile.createNewFile();

			fop.write(byteArray);
			fop.flush();
			fop.close();
			/*
			 * writer = new FileWriter(logFile); writer.write(json); writer.close();
			 */
		} catch (IOException e) {
			logger.error("Error while writting EOD logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e.getLocalizedMessage(), null);
			return gson.toJson(exportFileEntity);
		} finally {
			if (fop != null)
				try {
					fop.close();
				} catch (IOException e) {
					logger.error("ERROR:: while closing the fileOutputStream :: ", e);
				}
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportEODLogs()");
		return gson.toJson(exportFileEntity);
	}

	/**
	 * @param acsCode
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public String getMalpracticeLogViewerGrid(String acsCode, String startDate, String endDate) {
		logger.info("-- IN--- getMalpracticeLogViewerGrid--------");
		List<ReportLogViewerEntity> incidentAuditLogViewerEntitieList = new ArrayList<ReportLogViewerEntity>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(incidentAuditLogViewerEntitieList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();

		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(incidentAuditLogViewerEntitieList);
		}

		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
				.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);
		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
					+ " - " + endDate + " }");
			return gson.toJson(incidentAuditLogViewerEntitieList);
		}

		incidentAuditLogViewerEntitieList = this.getMalPracticeLogViewerEntity(rpsBatchAcsAssociationsList);
		logger.info("-----OUT---getMalpracticeLogViewerGrid-----");
		return gson.toJson(incidentAuditLogViewerEntitieList);

	}

	/**
	 * @param rpsBatchAcsAssociationsList
	 * @return
	 */
	private List<ReportLogViewerEntity>
			getMalPracticeLogViewerEntity(List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList) {
		logger.info("-----IN getMalPracticeLogViewerEntity()------");
		List<ReportLogViewerEntity> reportLogViewerEntityList = new ArrayList<ReportLogViewerEntity>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {

			List<RpsPack> rpsPackList = rpsPackService.getAllRpsPackIDByAssociation(
					rpsBatchAcsAssociation.getBatchAcsId(), RpsConstants.PackType.MALPRACTICE_PACK.toString());

			if (rpsPackList == null || rpsPackList.isEmpty()) {
				logger.info("MalPractice Pack is not available in database");
				continue;
			}

			// find the latest pack from list
			Collections.sort(rpsPackList, new Comparator<RpsPack>() {

				@Override
				public int compare(RpsPack o1, RpsPack o2) {
					if (o1.getLastModifiedDate() != null && o2.getLastModifiedDate() != null)
						return o1.getLastModifiedDate().compareTo(o2.getLastModifiedDate()) * (-1);
					else
						return 0;
				}
			});
			Integer rpsPackID = rpsPackList.get(0).getPid();

			RpsRpackComponent rpsRpackComponent = rpsRpackComponentService.getComponentByBatchIDsAndTypeAndStatus(
					rpsPackID, RpsConstants.RpackComponents.MALPRACTICE_LOG.toString(),
					RpsConstants.packStatus.PACKS_RECEIVED.toString());
			if (rpsRpackComponent != null) {
				ReportLogViewerEntity reportLogViewerEntity = new ReportLogViewerEntity();
				reportLogViewerEntity.setBatchAcsId(rpsRpackComponent.getRpackComponentId());
				RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
				reportLogViewerEntity.setBatchCode(rpsBatch.getBatchCode());
				reportLogViewerEntity.setBatchStartTime(convertTimeToString(rpsBatch.getBatchStartTime().getTime()));
				reportLogViewerEntity.setBatchEndTime(this.getBatchEndTime(rpsBatch, rpsBatchAcsAssociation));

				reportLogViewerEntityList.add(reportLogViewerEntity);
			}
		}
		logger.info("-----OUT getMalPracticeLogViewerEntity()------");
		return reportLogViewerEntityList;
	}

	public String viewMalpracticeLogs(Integer rpackComponentId) {
		logger.info("---IN--- viewMalpracticeLogs()");
		String json = "";
		if (rpackComponentId == null || rpackComponentId == 0) {
			logger.error("Missing mandatory agruments-- rpackComponentId : " + rpackComponentId);
			return json;
		}
		RpsRpackComponent rpsRpackComponent = rpsRpackComponentService.getRpackComponentByID(rpackComponentId);

		if (rpsRpackComponent == null) {
			logger.error("There is no MalPractice Log present in database for rpackComponentId :" + rpackComponentId);
			return json;
		}

		json = json + "<br>" + rpsRpackComponent.getAdminAuditDetails();
		logger.info("---OUT--- viewMalpracticeLogs()");
		return json;
	}

	/**
	 * @param rpsComponentID
	 * @return
	 */
	public String exportMalpracticeLogs(Integer rpsComponentID) {
		logger.info("---IN--- exportMalpracticeLogs()");
		ExportFileEntity exportFileEntity = null;
		String json = this.viewMalpracticeLogs(rpsComponentID);
		if (json == null || json.isEmpty()) {
			logger.info("IncidentAudit Log is empty for batchAcsId :: {} " + rpsComponentID);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File logsFolder = new File(
				apolloHome + File.separator + RpsConstants.RPSLOG + File.separator + RpsConstants.MALPRACTICE_LOGS);
		if (!logsFolder.exists())
			logsFolder.mkdirs();

		File logFile =
				new File(logsFolder + File.separator + RpsConstants.MALPRACTICE + rpsComponentID + RpsConstants.LOG);
		FileOutputStream fop = null;
		// FileWriter writer;
		try {
			if (json.contains("<br>"))
				json = json.replaceAll("<br>", "\n");

			if (json.contains("\n"))
				json = json.replaceAll("\n", "\r\n");

			fop = new FileOutputStream(logFile);
			byte[] byteArray = json.getBytes();

			if (!logFile.exists())
				logFile.createNewFile();

			fop.write(byteArray);
			fop.flush();
			fop.close();
			/*
			 * writer = new FileWriter(logFile); writer.write(json); writer.close();
			 */
		} catch (IOException e) {
			logger.error("Error while writing Malpractice logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e.getLocalizedMessage(), null);
			return gson.toJson(exportFileEntity);
		} finally {
			if (fop != null)
				try {
					fop.close();
				} catch (IOException e) {
					logger.error("ERROR:: while closing the fileOutputStream :: ", e);
				}
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportMalpracticeLogs()");
		return gson.toJson(exportFileEntity);
	}

	/**
	 * @param acsCode
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public String getExternalDeviceAuditLogGrid(String acsCode, String startDate, String endDate) {
		logger.info("-----IN getExternalDeviceAuditLog------");
		List<EODLogViewerEntity> eodList = new ArrayList<EODLogViewerEntity>();

		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return gson.toJson(eodList);
		}
		RpsAcsServer rpsAcsServer = rpsAcsServerServices.findByAcsServerId(acsCode);

		if (rpsAcsServer == null) {
			logger.error("There is no ACS Code in database where acsCode :: {} ", acsCode);
			return gson.toJson(eodList);
		}

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date sDate = null;
		Date eDate = null;
		String stDate = "";
		String edate = "";
		try {
			sDate = simpleDateFormat.parse(startDate);
			eDate = simpleDateFormat.parse(endDate);
			stDate = simpleDateFormat.format(sDate);
			edate = simpleDateFormat.format(eDate);
		} catch (ParseException e) {
			logger.error("ERROR :: While parsing the date", e);
		}

		List<RpsReportLog> rpsReportLogs = rpsReportLogService.findByAcsAndDateAndPackType(rpsAcsServer, stDate, edate,
				RpsConstants.RpackComponents.EXTERNALDEVICEAUDIT_LOG.toString());

		for (RpsReportLog rpsReportLog : rpsReportLogs) {
			EODLogViewerEntity eodLogViewerEntity = new EODLogViewerEntity();
			eodLogViewerEntity.setDate(rpsReportLog.getLogDate());
			eodLogViewerEntity.setReportLogID(rpsReportLog.getReportLogId());
			eodList.add(eodLogViewerEntity);
		}
		logger.info("-----OUT getExternalDeviceAuditLog------");
		return gson.toJson(eodList);
	}

	/**
	 * @param reportLogId
	 * @return
	 */
	public String viewExternalDeviceAuditLogs(Integer reportLogId) {
		logger.info("------IN viewExternalDeviceAuditLogs-----");
		String json = "";
		if (reportLogId == null) {
			logger.error("Missing mandatory agruments-- reportLogId : " + reportLogId);
			return json;
		}

		RpsReportLog rpsReportLog = rpsReportLogService.findRpsReportLogbyID(reportLogId);

		if (rpsReportLog == null) {
			logger.error("There is no ExternalDeviceAudit Log present in database where reportLogId :: {} ",
					reportLogId);
			return json;
		}

		json = rpsReportLog.getReportLogDetails();

		logger.info("ExternalDeviceAudit Logs :: {} ", json);

		logger.info("------OUT viewExternalDeviceAuditLogs-----");
		return json;
	}

	/**
	 * @param reportLogId
	 * @return
	 */
	public String exportExternalDeviceAuditLogs(Integer reportLogId) {
		logger.info("---IN--- exportExternalDeviceAuditLogs()");
		ExportFileEntity exportFileEntity = null;
		String json = this.viewExternalDeviceAuditLogs(reportLogId);
		if (json == null || json.isEmpty()) {
			logger.info("ExternalDeviceAudit Log is empty for reportLogId :: {} " + reportLogId);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		File logsFolder = new File(apolloHome + File.separator + RpsConstants.RPSLOG + File.separator
				+ RpsConstants.EXTERNALDEVICEAUDIT_LOG);
		if (!logsFolder.exists())
			logsFolder.mkdirs();

		File logFile = new File(
				logsFolder + File.separator + RpsConstants.EXTERNALDEVICEAUDIT + reportLogId + RpsConstants.LOG);
		FileOutputStream fop = null;
		// FileWriter writer;
		try {

			if (json.contains("\n"))
				json = json.replaceAll("\n", "\r\n");
			fop = new FileOutputStream(logFile);
			byte[] byteArray = json.getBytes();

			if (!logFile.exists())
				logFile.createNewFile();

			fop.write(byteArray);
			fop.flush();
			fop.close();
			/*
			 * writer = new FileWriter(logFile); writer.write(json); writer.close();
			 */
		} catch (IOException e) {
			logger.error("Error while writting ExternalDeviceAudit logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, e.getLocalizedMessage(), null);
			return gson.toJson(exportFileEntity);
		} finally {
			if (fop != null)
				try {
					fop.close();
				} catch (IOException e) {
					logger.error("ERROR:: while closing the fileOutputStream :: ", e);
				}
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportExternalDeviceAuditLogs()");
		return gson.toJson(exportFileEntity);
	}

	public String exportCandidateFeedbackForBatchAcs(String batchCode, String acsCode) {

		logger.info("---IN--- exportCandidateFeedbackForBatchAcs()");
		ExportFileEntity exportFileEntity = null;
		Map<String, List<CandidateFeedBackEntity>> candToFeedbackMap = new HashMap<>();

		RpsBatchAcsAssociation rpsBatchAcsAssociation =
				rpsBatchAcsAssociationService.getAssociationByBatchCodeAndAcsId(batchCode, acsCode);
		if (rpsBatchAcsAssociation == null) {
			logger.info("Candidate Feedback is empty");
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		List<RpsMasterAssociation> rpsMasterAssociationsList =
				rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
		if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
			logger.info("Candidate Feedback is empty");
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		List<CandidateLogsViewEntity> candidateLogsViewEntities =
				getCandidatFeedbackEntities(rpsBatchAcsAssociation, rpsMasterAssociationsList, null);
		if (candidateLogsViewEntities == null || candidateLogsViewEntities.isEmpty()) {
			logger.info("Candidate Feedback is empty");
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		for (CandidateLogsViewEntity candidateLogsViewEntity : candidateLogsViewEntities) {
			String responseStatusJson = viewCandidateFeedback(candidateLogsViewEntity.getUniqueCandidateId(),
					candidateLogsViewEntity.getAssessmentID(), candidateLogsViewEntity.getAssessmentSetID());
			ResponseStatus responseStatus = gson.fromJson(responseStatusJson, ResponseStatus.class);
			if (responseStatus.getStatus().equalsIgnoreCase(RpsConstants.SUCCESS_STATUS)) {
				candToFeedbackMap.put(candidateLogsViewEntity.getCandidateID(), responseStatus.getResponseJson());
			}
		}

		if (candToFeedbackMap == null || candToFeedbackMap.isEmpty()) {
			logger.info("Candidate Feedback is empty");
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}

		String filePath;
		try {
			filePath = writeFeedbackToCSV(candToFeedbackMap);
		} catch (IOException e) {
			logger.info("Error in exporting Candidate Feedback");
			exportFileEntity =
					new ExportFileEntity(RpsConstants.FAILED_STATUS, "Error in exporting Candidate Feedback", null);
			return gson.toJson(exportFileEntity);
		}
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, filePath);
		logger.info("---OUT--- exportCandidateFeedbackForBatchAcs()");
		return gson.toJson(exportFileEntity);
	}

	private String writeFeedbackToCSV(Map<String, List<CandidateFeedBackEntity>> candToFeedbackMap) throws IOException {
		File folder = new File(apolloHome + File.separator + exportFolder);
		if (!folder.exists())
			folder.mkdirs();

		String filePath = folder + File.separator + RpsConstants.CANDIDATE_FEEDBACK
				+ Calendar.getInstance().getTimeInMillis() + ".CSV";
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));
		CSVWriter csvWriter = new CSVWriter(bufferedWriter);
		String[] headers = new String[] { RpsConstants.FEEDBACK_HEADERS.CANDIDATE_ID.toString(),
				RpsConstants.FEEDBACK_HEADERS.QUESTION_ID.toString(),
				RpsConstants.FEEDBACK_HEADERS.FEEDBACK_ID.toString(),
				RpsConstants.FEEDBACK_HEADERS.FEEDBACK_VALUE.toString() };
		csvWriter.writeNext(headers);

		Set<String> candidateIdSet = candToFeedbackMap.keySet();
		for (String candidateId : candidateIdSet) {
			List<CandidateFeedBackEntity> candidateFeedBackEntityList = candToFeedbackMap.get(candidateId);
			for (CandidateFeedBackEntity candidateFeedBackEntity : candidateFeedBackEntityList) {
				String questionId = candidateFeedBackEntity.getQuestionId();
				List<FeedBackItem> feedBackItems = candidateFeedBackEntity.getFeedBackItems();
				if (feedBackItems != null && !feedBackItems.isEmpty()) {
					for (FeedBackItem feedBackItem : feedBackItems) {
						List<String> line = new ArrayList<>();
						line.add(candidateId);
						line.add(questionId);
						line.add(feedBackItem.getItemId());
						line.add(feedBackItem.getValue());
						String[] values = line.toArray(new String[line.size()]);
						csvWriter.writeNext(values);
					}
				}
			}
		}

		csvWriter.close();
		return filePath;
	}

	public String getCandidateFeedbackGridDataOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate, String imageFolder) {
		List<CandidateLogsViewEntity> candidatFeedbackEntities = null;

		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(candidatFeedbackEntities);
		}

		logger.info("---IN--- getCandidateFeedbackGridData()");

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(candidatFeedbackEntities);
		}
		List<RpsMasterAssociationLiteForCandidateFeedback> rpsMasterAssociationsList = null;
		if (acsCode.equals("0"))
			rpsMasterAssociationsList = rpsMasterAssociationService.getAllRpsMasterassociationByEventCode(eventCode,
					rangeStartDate, rangeEndDate);
		else
			rpsMasterAssociationsList = rpsMasterAssociationService.getAllRpsMasterassociationByAcsCode(acsCode,
					rangeStartDate, rangeEndDate);

		if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
			logger.error("No candidate data is available for acsCode :" + acsCode);
			return gson.toJson(candidatFeedbackEntities);
		}

		candidatFeedbackEntities = new ArrayList<>();
		for (RpsMasterAssociationLiteForCandidateFeedback rpsMasterAssociation : rpsMasterAssociationsList) {
			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			CandidateLogsViewEntity candidateFeedbackViewEntity = new CandidateLogsViewEntity();
			candidateFeedbackViewEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
			candidateFeedbackViewEntity
					.setCandidateName(this.getCandidateFullName(rpsMasterAssociation.getRpsCandidate(),
							rpsMasterAssociation.getUniqueCandidateId()));
			candidateFeedbackViewEntity.setCandidateID(rpsMasterAssociation.getRpsCandidate().getCandidateId1());
			if (imageFolder != null)
				candidateFeedbackViewEntity.setCandidatePhoto(
						this.getCandidatePhotoPath(rpsMasterAssociation.getRpsCandidate(), imageFolder));
			if (rpsMasterAssociation.getLoginID() == null || rpsMasterAssociation.getLoginID().equalsIgnoreCase("null"))
				candidateFeedbackViewEntity.setLoginID("");
			else
				candidateFeedbackViewEntity.setLoginID(rpsMasterAssociation.getLoginID());
			candidateFeedbackViewEntity.setAssessmentID(rpsMasterAssociation.getAssessmentCode());
			RpsCandidateResponseLiteQP rpsCandidateResponse = rpsCandidateResponseService
					.getCandidateResponsesByMasterAsson(rpsMasterAssociation.getUniqueCandidateId());
			candidateFeedbackViewEntity.setQuestionLangugaeMap(rpsCandidateResponseService.getQuestionLanguageShuffleSequense(responseLite.getResponse()));
			candidateFeedbackViewEntity
					.setAssessmentSetID(rpsCandidateResponse.getRpsQuestionPaper().getQpCode() == null ? ""
							: rpsCandidateResponse.getRpsQuestionPaper().getQpCode());
			candidatFeedbackEntities.add(candidateFeedbackViewEntity);

		}
		logger.info("---OUT--- getCandidateFeedbackGridData()");

		return gson.toJson(candidatFeedbackEntities);
	}

	public String getAttendanceLogViewerGridOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate) {
		List<AttendanceViewerEntity> attendanceViewerEntitiesList = null;
		logger.info("---IN--- attendanceLogViewer()");

		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, eventCode, startDate or endDate");
			return gson.toJson(attendanceViewerEntitiesList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(attendanceViewerEntitiesList);
		}
		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = null;
		if (acsCode.equals("0"))
			rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByEventCodeAndInDateRange(eventCode, rangeStartDate, rangeEndDate);
		else
			rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);

		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
					+ " - " + endDate + " }");
			return gson.toJson(attendanceViewerEntitiesList);
		}

		logger.info("---IN--- fetchAttendanceViewerEntities()");
		attendanceViewerEntitiesList = new ArrayList<>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {
			boolean isAttReportAvailable = true;
			AttendanceViewerEntity attendanceViewerEntity = new AttendanceViewerEntity();
			attendanceViewerEntity.setBatchAcsId(rpsBatchAcsAssociation.getBatchAcsId());
			attendanceViewerEntity.setBatchCode(rpsBatchAcsAssociation.getRpsBatch().getBatchCode());
			attendanceViewerEntity.setBatchStartTime(
					convertTimeToString(rpsBatchAcsAssociation.getRpsBatch().getBatchStartTime().getTime()));
			attendanceViewerEntity
					.setBatchEndTime(this.getBatchEndTime(rpsBatchAcsAssociation.getRpsBatch().getBatchEndTime(),
							rpsBatchAcsAssociation.getBatchExtensionTime()));
			if (rpsBatchAcsAssociation.getAttendanceDetails() == null)
				isAttReportAvailable = false;
			Map<String, Integer> attendanseMap = this.getCandidatePresenceAbsenceStatus(rpsBatchAcsAssociation);
			if (attendanseMap != null) {
				attendanceViewerEntity.setPresentCount(String.valueOf(attendanseMap.get("PRESENT").intValue()));
				attendanceViewerEntity.setAbsentCount(String.valueOf(attendanseMap.get("ABSENT").intValue()));
			} else {
				attendanceViewerEntity.setPresentCount("N/A");
				attendanceViewerEntity.setAbsentCount("N/A");
			}

			attendanceViewerEntitiesList.add(attendanceViewerEntity);
		}
		logger.info("---OUT--- fetchAttendanceViewerEntities()");
		return gson.toJson(attendanceViewerEntitiesList);
	}

	public String getMalpracticeLogViewerGridOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate) {
		List<ReportLogViewerEntity> reportLogViewerEntitiesList = null;
		logger.info("-- IN--- getMalpracticeLogViewerGridOnAcsCodes--------");
		if (eventCode == null || eventCode.isEmpty() || acsCode == null || acsCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.error("Missing mandatory agruments..eventCode, acsCode, startDate or endDate");
			return gson.toJson(reportLogViewerEntitiesList);
		}
		List<RpsRpackComponent> rpsRpackComponents = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();
		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(reportLogViewerEntitiesList);
		}
		if (acsCode.equals("0"))
			rpsRpackComponents = rpsRpackComponentService.getAllComponentsByEventCodeInDateRange(eventCode,
					rangeStartDate, rangeEndDate, RpsConstants.PackType.MALPRACTICE_PACK.toString(),
					RpsConstants.RpackComponents.MALPRACTICE_LOG.toString(),
					RpsConstants.packStatus.PACKS_RECEIVED.toString());
		else
			rpsRpackComponents = rpsRpackComponentService.getAllComponentsByAcsCodeInDateRange(acsCode, rangeStartDate,
					rangeEndDate, RpsConstants.PackType.MALPRACTICE_PACK.toString(),
					RpsConstants.RpackComponents.MALPRACTICE_LOG.toString(),
					RpsConstants.packStatus.PACKS_RECEIVED.toString());

		reportLogViewerEntitiesList = new ArrayList<>();
		if (rpsRpackComponents != null && !rpsRpackComponents.isEmpty()) {
			for (RpsRpackComponent rpsRpackComponent : rpsRpackComponents) {
				ReportLogViewerEntity reportLogViewerEntity = new ReportLogViewerEntity();
				reportLogViewerEntity.setRpackComponentId(rpsRpackComponent.getRpackComponentId());
				reportLogViewerEntity
						.setBatchAcsId(rpsRpackComponent.getRpsPack().getRpsBatchAcsAssociation().getBatchAcsId());
				reportLogViewerEntity.setBatchCode(
						rpsRpackComponent.getRpsPack().getRpsBatchAcsAssociation().getRpsBatch().getBatchCode());
				reportLogViewerEntity.setBatchStartTime(convertTimeToString(rpsRpackComponent.getRpsPack()
						.getRpsBatchAcsAssociation().getRpsBatch().getBatchStartTime().getTime()));
				reportLogViewerEntity.setBatchEndTime(this.getBatchEndTime(
						rpsRpackComponent.getRpsPack().getRpsBatchAcsAssociation().getRpsBatch().getBatchEndTime(),
						rpsRpackComponent.getRpsPack().getRpsBatchAcsAssociation().getBatchExtensionTime()));
				reportLogViewerEntitiesList.add(reportLogViewerEntity);
			}
		}
		logger.info("-----OUT---getMalpracticeLogViewerGridOnAcsCodes-----");
		return gson.toJson(reportLogViewerEntitiesList);
	}

	public String getExternalDeviceAuditLogGridOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate) {
		logger.info("-----IN getExternalDeviceAuditLog------");
		List<EODLogViewerEntity> externalAuditLogList = null;
		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(externalAuditLogList);
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date sDate = null;
		Date eDate = null;
		String stDate = "";
		String edate = "";
		try {
			sDate = simpleDateFormat.parse(startDate);
			eDate = simpleDateFormat.parse(endDate);
			stDate = simpleDateFormat.format(sDate);
			edate = simpleDateFormat.format(eDate);
		} catch (ParseException e) {
			logger.error("ERROR :: While parsing the date", e);
			return gson.toJson(externalAuditLogList);
		}
		List<RpsReportLog> rpsReportLogs = null;
		if (acsCode.equals("0"))
			rpsReportLogs = rpsReportLogService.findAllByAcsAndDateAndPackType(eventCode, stDate, edate,
					RpsConstants.RpackComponents.EXTERNALDEVICEAUDIT_LOG.toString());
		else
			rpsReportLogs = rpsReportLogService.findByAcsAndDateAndPackType(acsCode, stDate, edate,
					RpsConstants.RpackComponents.EXTERNALDEVICEAUDIT_LOG.toString());

		externalAuditLogList = new ArrayList<>();
		if (rpsReportLogs != null && !rpsReportLogs.isEmpty()) {
			for (RpsReportLog rpsReportLog : rpsReportLogs) {
				EODLogViewerEntity eodLogViewerEntity = new EODLogViewerEntity();
				eodLogViewerEntity.setDate(rpsReportLog.getLogDate());
				eodLogViewerEntity.setReportLogID(rpsReportLog.getReportLogId());
				externalAuditLogList.add(eodLogViewerEntity);
			}
		}
		logger.info("-----OUT getExternalDeviceAuditLog------");
		return gson.toJson(externalAuditLogList);
	}

	public String getIncidentAuditLogViewerGridOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate) {
		List<ReportLogViewerEntity> reportLogViewerEntitiesList = null;
		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(reportLogViewerEntitiesList);
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();

		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return gson.toJson(reportLogViewerEntitiesList);
		}
		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = null;
		if (acsCode.equals("0"))
			rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByEventCodeAndInDateRange(eventCode, rangeStartDate, rangeEndDate);
		else
			rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStartDate, rangeEndDate);
		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
					+ " - " + endDate + " }");
			return gson.toJson(reportLogViewerEntitiesList);
		}
		reportLogViewerEntitiesList = new ArrayList<>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {
			List<RpsRpackComponent> rpsRpackComponentList =
					rpsRpackComponentService.getComponentByBatchAcsAssoAndTypeAndStatus(rpsBatchAcsAssociation,
							RpsConstants.RpackComponents.INCIDENTAUDIT_LOG.toString(),
							RpsConstants.packStatus.PACKS_RECEIVED.toString());
			if (rpsRpackComponentList == null || rpsRpackComponentList.isEmpty()) {
				// this batch acs doesn't have Incident logs so continue for next bacth- acs
				continue;
			}
			ReportLogViewerEntity reportLogViewerEntity = new ReportLogViewerEntity();
			reportLogViewerEntity.setBatchAcsId(rpsBatchAcsAssociation.getBatchAcsId());
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			reportLogViewerEntity.setBatchCode(rpsBatch.getBatchCode());
			reportLogViewerEntity.setBatchStartTime(convertTimeToString(rpsBatch.getBatchStartTime().getTime()));
			reportLogViewerEntity
					.setBatchEndTime(this.getBatchEndTime(rpsBatchAcsAssociation.getRpsBatch().getBatchEndTime(),
							rpsBatchAcsAssociation.getBatchExtensionTime()));

			reportLogViewerEntitiesList.add(reportLogViewerEntity);
		}
		logger.info("-----OUT---getIncidentAuditLogViewerGrid-----");

		return gson.toJson(reportLogViewerEntitiesList);
	}

	public String getAdminAuditLogViewerGridOnAcsCodes(String acsCode, String eventCode, String startDate,
			String endDate) {
		Set<AdminAuditLogViewerEntity> adminAuditLogViewerEntitiesList = new HashSet<>();
		Set<String> acsCodes = new HashSet<>();
		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(adminAuditLogViewerEntitiesList);
		}
		if (acsCode.equals("0")) {
			acsCodes = this.getAllAcsOneventCode(eventCode, startDate, endDate);
		} else {
			acsCodes.add(acsCode);
		}
		if (acsCodes != null) {
			for (String acsCodeItr : acsCodes) {

				if (acsCodeItr != null && !acsCodeItr.isEmpty()) {
					adminAuditLogViewerEntitiesList =
							getAdminAuditGridData(adminAuditLogViewerEntitiesList, acsCodeItr, startDate, endDate);
					logger.info("new IncidentAuditLogViewerEntitiesList list={} of acs code={}",
							adminAuditLogViewerEntitiesList, acsCodeItr);
				}
			}
		}
		return gson.toJson(adminAuditLogViewerEntitiesList);
	}

	public Set<AdminAuditLogViewerEntity> getAdminAuditGridData(
			Set<AdminAuditLogViewerEntity> adminAuditLogViewerEntitieList, String acsCode, String startDate,
			String endDate) {
		logger.info("-- IN--- AdminAuditLogViewer--------");
		Map<String, Integer> map = new HashMap<String, Integer>();
		if (acsCode == null || acsCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory agruments.. acsCode, startDate or endDate");
			return adminAuditLogViewerEntitieList;
		}

		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		Calendar rangeStartDate = Calendar.getInstance();
		Calendar rangeEndDate = Calendar.getInstance();

		try {
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
		} catch (ParseException e) {
			logger.error("Error in parsing range date--", e);
			return adminAuditLogViewerEntitieList;
		}

		boolean sameDateRange = false;
		Calendar rangeStDate = (Calendar) rangeStartDate.clone();
		Calendar rangeMidDate = (Calendar) rangeStDate.clone();
		rangeMidDate.set(Calendar.HOUR_OF_DAY, 23);
		rangeMidDate.set(Calendar.MINUTE, 59);
		rangeMidDate.set(Calendar.SECOND, 59);
		while (true) {
			if (DateTimeComparator.getDateOnlyInstance().compare(rangeStDate, rangeEndDate) == 0
					|| DateTimeComparator.getDateOnlyInstance().compare(rangeMidDate, rangeEndDate) == 0) {
				sameDateRange = true;
			}

			List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = rpsBatchAcsAssociationService
					.getAllBatchAssociationsByAcsCodeAndInDateRange(acsCode, rangeStDate, rangeMidDate);

			if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
				logger.warn("There are no batches associated with acsCode--" + acsCode + " in date range-{" + startDate
						+ " - " + endDate + " }");
			}

			map = this.getAdminAuditLogViewerEntity(rpsBatchAcsAssociationsList, rangeStDate);

			Set<String> set = map.keySet();
			for (String date : set) {
				AdminAuditLogViewerEntity adminAuditLogViewerEntity = new AdminAuditLogViewerEntity();
				// adminAuditLogViewerEntity.setCurrentDate(this.convertStringToDateWithoutTime(date));
				adminAuditLogViewerEntity.setDate(date);
				adminAuditLogViewerEntity.setRpackComponentId(map.get(date));
				adminAuditLogViewerEntitieList.add(adminAuditLogViewerEntity);
			}

			rangeMidDate.add(Calendar.DATE, 1);
			if (sameDateRange) {
				sameDateRange = false;
				break;
			} else
				rangeStDate.add(Calendar.DATE, 1);
		}

		return adminAuditLogViewerEntitieList;
	}

	public String getCandidateLogsGridDataOnAcsCodes(String acsCode, String eventCode, String startDate, String endDate,
			String imageFolder) {
		Set<String> acsCodes = new HashSet<>();
		List<CandidateLogsViewEntity> candidatFeedbackEntities = new ArrayList<>();
		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(candidatFeedbackEntities);
		}

		if (acsCode.equals("0")) {
			CityVenueAcsEntity cityVenueAcsEntity = gson.fromJson(
					rpsAttendanceReconciliationService.getAcsListOneventCode(eventCode, startDate, endDate),
					CityVenueAcsEntity.class);
			acsCodes = cityVenueAcsEntity.getAcsCodes();

		} else {
			acsCodes.add(acsCode);
		}
		for (String acsCodeItr : acsCodes) {

			if (acsCodeItr != null && !acsCodeItr.isEmpty()) {
				candidatFeedbackEntities = this.getCandidateLogsGridDataOnAcsCode(candidatFeedbackEntities, acsCodeItr,
						imageFolder, startDate, endDate);
				logger.info("new candidatFeedbackEntities list={} of acs code={}", candidatFeedbackEntities,
						acsCodeItr);
			}
		}
		return gson.toJson(candidatFeedbackEntities);
	}

	public List<CandidateLogsViewEntity> getCandidateLogsGridDataOnAcsCode(
			List<CandidateLogsViewEntity> candidateLogsViewEntities, String acsCode, String imageFolder,
			String startDate, String endDate) {
		logger.info("---IN--- getCandidateLogsGridData()");

		Set<String> batchCodes = gson.fromJson(this.getBatchListByAcsInDateRange(acsCode, startDate, endDate),
				new TypeToken<Set<String>>() {
				}.getType());
		logger.info("---OUT--- getBatchListByAcsInDateRange()");
		if (batchCodes != null && !batchCodes.isEmpty()) {
			for (String batchCode : batchCodes) {

				if (batchCode == null || batchCode.isEmpty() || acsCode == null || acsCode.isEmpty()) {
					logger.error("Missing mandatory arguments-- batchCode or acsCode");
					continue;
					// return candidateLogsViewEntities;
				}

				RpsBatchAcsAssociation rpsBatchAcsAssociation =
						rpsBatchAcsAssociationService.getAssociationByBatchCodeAndAcsId(batchCode, acsCode);
				if (rpsBatchAcsAssociation == null) {
					logger.error("There is no batch acs association for batch code :" + batchCode + " and acsCode :"
							+ acsCode);
					continue;
					// return candidateLogsViewEntities;
				}

				List<RpsMasterAssociation> rpsMasterAssociationsList =
						rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
				if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
					logger.error(
							"No candidate data is available for batch code :" + batchCode + " and acsCode :" + acsCode);
					continue;
					// return candidateLogsViewEntities;
				}

				candidateLogsViewEntities = this.getCandidateLogsViewEntities(candidateLogsViewEntities,
						rpsBatchAcsAssociation, rpsMasterAssociationsList, imageFolder);
				logger.info("---OUT--- getCandidateLogsGridData()");
			}
		}
		return candidateLogsViewEntities;
	}

	public String getEODLogViewerGridOnAcsCodes(String acsCode, String eventCode, String startDate, String endDate) {
		logger.info("-----IN getEODLogViewerGridOnAcsCodes------");
		List<EODLogViewerEntity> EODLogViewerEntitiesList = null;
		if (acsCode == null || acsCode.isEmpty() || eventCode == null || eventCode.isEmpty() || startDate == null
				|| startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + " ,acsCode : " + acsCode
					+ ",  startDate :" + startDate + " ,endDate : " + endDate);
			return gson.toJson(EODLogViewerEntitiesList);
		}
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date sDate = null;
		Date eDate = null;
		String stDate = "";
		String edate = "";
		try {
			sDate = simpleDateFormat.parse(startDate);
			eDate = simpleDateFormat.parse(endDate);
			stDate = simpleDateFormat.format(sDate);
			edate = simpleDateFormat.format(eDate);
		} catch (ParseException e) {
			logger.error("ERROR :: While parsing the date", e);
			return gson.toJson(EODLogViewerEntitiesList);
		}
		List<RpsReportLog> rpsReportLogs = null;
		String packType = RpsConstants.EOD_REPORT;
		if (acsCode.equals("0"))
			rpsReportLogs = rpsReportLogService.findAllByAcsAndDateAndPackType(eventCode, stDate, edate, packType);
		else
			rpsReportLogs = rpsReportLogService.findByAcsAndDateAndPackType(acsCode, stDate, edate, packType);
		EODLogViewerEntitiesList = new ArrayList<>();
		if (rpsReportLogs != null && !rpsReportLogs.isEmpty()) {
			for (RpsReportLog rpsReportLog : rpsReportLogs) {
				EODLogViewerEntity eodLogViewerEntity = new EODLogViewerEntity();
				eodLogViewerEntity.setDate(rpsReportLog.getLogDate());
				eodLogViewerEntity.setReportLogID(rpsReportLog.getReportLogId());
				EODLogViewerEntitiesList.add(eodLogViewerEntity);
			}
		}
		logger.info("-----OUT getEODLogViewerGridOnAcsCodes------");
		return gson.toJson(EODLogViewerEntitiesList);
	}

	private String writeCandidateFeedbackToCSV(List<Map<String, List<RPSCandidateFeedBackEntity>>> candFeedbackMapList)
			throws IOException {
		File folder = new File(apolloHome + File.separator + exportFolder);
		if (!folder.exists())
			folder.mkdirs();
		String filePath = folder + File.separator + RpsConstants.CANDIDATE_WISE_FEEDBACK
				+ Calendar.getInstance().getTimeInMillis() + ".CSV";
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));
		CSVWriter csvWriter = new CSVWriter(bufferedWriter);
		String[] headers = new String[] { RpsConstants.CANDIDATE_FEEDBACK_HEADERS.AssessmentName.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.AssessmentID.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.LoginName.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.QuestionID.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.QuestionLabel.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.AnswerKey.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.Complaint.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.BatchName.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.ScheduleStartDate.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.BookID.toString() };
		csvWriter.writeNext(headers);
		List<RPSCandidateFeedBackEntity> candFeedBackEntityList = new ArrayList<>();
		for (Map<String, List<RPSCandidateFeedBackEntity>> candidateLogsViewEntity : candFeedbackMapList) {
			Set<String> candidateIdSet = candidateLogsViewEntity.keySet();
			for (String candidateId : candidateIdSet) {
				List<RPSCandidateFeedBackEntity> candidateFeedBackEntityList = candidateLogsViewEntity.get(candidateId);
				for (RPSCandidateFeedBackEntity candidateFeedBackEntity : candidateFeedBackEntityList) {
					candFeedBackEntityList.add(candidateFeedBackEntity);
					List<FeedBackItem> feedBackItems = candidateFeedBackEntity.getFeedBackItems();
					if (feedBackItems != null && !feedBackItems.isEmpty()) {
						for (FeedBackItem feedBackItem : feedBackItems) {
							List<String> line = new ArrayList<>();
							line.add(candidateFeedBackEntity.getAssessmentName());
							line.add(candidateFeedBackEntity.getAssessmentID());
							line.add(candidateFeedBackEntity.getLoginName());
							line.add(candidateFeedBackEntity.getQuestionID());
							line.add(candidateFeedBackEntity.getQuestionLabel());// q label
							line.add(candidateFeedBackEntity.getAnswerKey());
							line.add(feedBackItem.getValue());
							line.add(candidateFeedBackEntity.getBatchName());
							line.add(candidateFeedBackEntity.getScheduleStartDate());
							line.add(" ");
							String[] values = line.toArray(new String[line.size()]);
							csvWriter.writeNext(values);
						}
					}
				}
			}
		}
		csvWriter.close();
		logger.info("filePath={}", filePath);
		return filePath;
	}

	private ResponseCandQuestionEntity appendCandidateQuestionFeedbackToCSV(
			List<Map<String, List<RPSCandidateQuestionFeedBackEntity>>> candQuestFeedbackMapList) throws IOException {
		ResponseCandQuestionEntity entity = new ResponseCandQuestionEntity();
		Map<String, String> qfeedback = new TreeMap<String, String>();
		Map<String, Integer> qfeedbackCount = new TreeMap<String, Integer>();

		List<RPSCandidateQuestionFeedBackEntity> candQuestArrayList =
				new ArrayList<RPSCandidateQuestionFeedBackEntity>();
		for (Map<String, List<RPSCandidateQuestionFeedBackEntity>> candidateLogsViewEntity : candQuestFeedbackMapList) {
			Set<String> candidateIdSet = candidateLogsViewEntity.keySet();
			for (String candidateId : candidateIdSet) {
				List<RPSCandidateQuestionFeedBackEntity> candidateFeedBackEntityList =
						candidateLogsViewEntity.get(candidateId);
				for (RPSCandidateQuestionFeedBackEntity candidateFeedBackEntity : candidateFeedBackEntityList) {
					if (candidateFeedBackEntity.getQuestionID() == null)
						continue;
					if (!qfeedback.containsKey(candidateFeedBackEntity.getQuestionID())) {
						candQuestArrayList.add(candidateFeedBackEntity);
					}
					List<FeedBackItem> feedBackItems = candidateFeedBackEntity.getFeedBackItems();
					if (feedBackItems != null && !feedBackItems.isEmpty()) {
						for (FeedBackItem feedBackItem : feedBackItems) {
							String value = feedBackItem.getValue();
							logger.info("value={}", feedBackItem.getValue());
							if (qfeedback.containsKey(candidateFeedBackEntity.getQuestionID())) {
								value = qfeedback.get(candidateFeedBackEntity.getQuestionID());
								if (!value.trim().equalsIgnoreCase(feedBackItem.getValue().trim()))
									value = value + "," + feedBackItem.getValue();
								qfeedback.put(candidateFeedBackEntity.getQuestionID(), value);
								int count = qfeedbackCount.get(candidateFeedBackEntity.getQuestionID());
								qfeedbackCount.put(candidateFeedBackEntity.getQuestionID(), count + 1);
							} else {
								qfeedback.put(candidateFeedBackEntity.getQuestionID(), value);
								qfeedbackCount.put(candidateFeedBackEntity.getQuestionID(), 1);
							}
						}
					}
					logger.info("no value={}");

				}
			}
		}
		entity.setCandQuestionList(candQuestArrayList);
		entity.setEntity(candQuestFeedbackMapList);
		entity.setqFeedBackCount(qfeedbackCount);

		entity.setQuestionfeedbackentity(qfeedback);
		return entity;
	}

	private String writeCandidateQuestionFeedbackToCSV(ResponseCandQuestionEntity entity) throws IOException {
		File folder = new File(apolloHome + File.separator + exportFolder);
		if (!folder.exists())
			folder.mkdirs();
		String filePath = folder + File.separator + RpsConstants.QUESTION_WISE_FEEDBACK
				+ Calendar.getInstance().getTimeInMillis() + ".CSV";
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));
		CSVWriter csvWriter = new CSVWriter(bufferedWriter);
		String[] headers = new String[] { RpsConstants.CANDIDATE_FEEDBACK_HEADERS.AssessmentName.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.AssessmentID.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.QuestionID.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.QuestionLabel.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.Complaint.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.CorrectChoice.toString(),
				RpsConstants.CANDIDATE_FEEDBACK_HEADERS.TotalComplaints.toString() };
		csvWriter.writeNext(headers);
		List<RPSCandidateQuestionFeedBackEntity> candFeedBackEntityList = new ArrayList<>();
		Map<String, String> qfb = entity.getQuestionfeedbackentity();
		Map<String, Integer> qcount = entity.getqFeedBackCount();
		List<RPSCandidateQuestionFeedBackEntity> candidateFeedBackEntityList = entity.getCandQuestionList();
		for (RPSCandidateQuestionFeedBackEntity candidateFeedBackEntity : candidateFeedBackEntityList) {
			candFeedBackEntityList.add(candidateFeedBackEntity);
			List<String> line = new ArrayList<>();
			line.add(candidateFeedBackEntity.getAssessmentName());
			line.add(candidateFeedBackEntity.getAssessmentID());
			line.add(candidateFeedBackEntity.getQuestionID());
			line.add(candidateFeedBackEntity.getQuestionLabel());// q label
			if (qfb.containsKey(candidateFeedBackEntity.getQuestionID())) {
				String v = qfb.get(candidateFeedBackEntity.getQuestionID());
				line.add(v);
			}
			line.add(candidateFeedBackEntity.getAnswerKey());
			if (qcount.containsKey(candidateFeedBackEntity.getQuestionID())) {
				Integer count = qcount.get(candidateFeedBackEntity.getQuestionID());
				line.add(Integer.toString(count));
			}
			String[] values = line.toArray(new String[line.size()]);
			csvWriter.writeNext(values);
		}
		csvWriter.close();
		logger.info("filePath={}", filePath);
		return filePath;
	}

	public String exportRPSMalpracticeLogsForBatchAcs(String acsCode, String eventCode, String startDate,
			String endDate) {
		String filePath = "";
		ExportFileEntity exportFileEntity = null;
		SimpleDateFormat formatter1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		SimpleDateFormat formatter3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat formatter2 = new SimpleDateFormat("ddMMyyyy");

		try {
			String response = this.getMalpracticeLogViewerGridOnAcsCodes(acsCode, eventCode, startDate, endDate);
			if (response == null || response.isEmpty()) {
				logger.info("Error in exporting Malpractice Feedback Report");
				exportFileEntity =
						new ExportFileEntity(RpsConstants.FAILED_STATUS, "Error in exporting Candidate Feedback", null);
				return gson.toJson(exportFileEntity);
			}
			List<ReportLogViewerEntity> reportLogViewerEntityList =
					gson.fromJson(response, new TypeToken<List<ReportLogViewerEntity>>() {
					}.getType());
			List<MalpracticeReportLogsEntity> malpracticeReportLogsEntities = new ArrayList<>();
			for (ReportLogViewerEntity reportLogViewerEntity : reportLogViewerEntityList) {

				MalpracticeReportLogsEntity malpracticeReportLogsEntity = new MalpracticeReportLogsEntity();
				RpsRpackComponent rpsRpackComponent =
						rpsRpackComponentService.getRpackComponentByID(reportLogViewerEntity.getRpackComponentId());
				if (rpsRpackComponent == null) {
					logger.error("There is no MalPractice Log present in database for batchAcsId :");
				}
				RpsBatchAcsAssociation rpsBatchAcsAssociation =
						rpsBatchAcsAssociationService.findByRpackComponentId(rpsRpackComponent.getRpackComponentId());
				String json = rpsRpackComponent.getAdminAuditDetails();
				if (json != null && !json.isEmpty()) {
					if (json.contains("<br>"))
						json = json.replaceAll("<br>", "\r\n");
					String[] jsonSplit = json.split("\r\n");
					if (jsonSplit[0].contains(":")) {
						String[] loginName = jsonSplit[0].split(":");
						malpracticeReportLogsEntity.setLoginName(loginName[1].trim());
					}
					malpracticeReportLogsEntity.setRemarks(jsonSplit[1]);
					if (jsonSplit[1].equals(RpsConstants.MALPRACTICE_OTHER_MSG) && jsonSplit[2] != null
							&& !jsonSplit[2].isEmpty())
						malpracticeReportLogsEntity
								.setRemarks(malpracticeReportLogsEntity.getRemarks() + " -- " + jsonSplit[2]);
				}
				if (rpsBatchAcsAssociation != null) {
					RpsBatch rpsbatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
					if (rpsbatch != null && rpsbatch.getBatchDate() != null && rpsbatch.getBatchEndTime() != null
							&& rpsbatch.getBatchStartTime() != null
							&& rpsBatchAcsAssociation.getRpsAcsServer().getAcsId() != null) {

						Date d = formatter1.parse(formatter1.format(rpsbatch.getBatchStartTime().getTime()));
						DateFormat f2 = new SimpleDateFormat("hmma");

						malpracticeReportLogsEntity
								.setBookedDate(formatter1.format(rpsRpackComponent.getCreationDate()));
						malpracticeReportLogsEntity
								.setScheduleEndDate(formatter1.format(rpsbatch.getBatchEndTime().getTime()));
						malpracticeReportLogsEntity
								.setScheduleStartDate(formatter1.format(rpsbatch.getBatchStartTime().getTime()));

						RpsAcsServer rpsAcsServer =
								rpsAcsServerServices.findById(rpsBatchAcsAssociation.getRpsAcsServer().getAcsId());

						if (rpsAcsServer != null && rpsAcsServer.getAcsServerName() != null) {
							malpracticeReportLogsEntity.setTestCenterName(rpsAcsServer.getAcsServerName());
						}
						RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationService.findByLoginIdBatchAcsId(
								malpracticeReportLogsEntity.getLoginName(), rpsBatchAcsAssociation.getBatchAcsId());
						malpracticeReportLogsEntity
								.setBatchName(rpsBatchAcsAssociation.getRpsAcsServer().getAcsServerName() + "_"
										+ formatter2.format(rpsbatch.getBatchDate().getTime()) + "_"
										+ rpsbatch.getBatchName() + "_" + f2.format(d).toUpperCase());
						if (rpsMasterAssociation != null
								&& rpsMasterAssociation.getRpsAssessment().getAssessmentCode() != null)
							malpracticeReportLogsEntity.setBatchName(malpracticeReportLogsEntity.getBatchName() + "_"
									+ rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
						List<AttendanceReportEntity> attendanceReportEntityList =
								getAttendanceLogsByBatchAcsId(rpsBatchAcsAssociation.getBatchAcsId());
						if (attendanceReportEntityList != null && !attendanceReportEntityList.isEmpty()) {
							for (AttendanceReportEntity attendanceReportEntity : attendanceReportEntityList) {
								if (malpracticeReportLogsEntity.getLoginName()
										.equals(attendanceReportEntity.getLoginID())) {
									if (attendanceReportEntity.getActualTestEndTime() != null && !attendanceReportEntity
											.getActualTestEndTime().trim().equalsIgnoreCase("null"))
										malpracticeReportLogsEntity.setTestEndTime(formatter1.format(
												formatter3.parse(attendanceReportEntity.getActualTestEndTime())));
									if (attendanceReportEntity.getTestStartTime() != null && !attendanceReportEntity
											.getTestStartTime().trim().equalsIgnoreCase("null"))
										malpracticeReportLogsEntity.setTestStartTime(formatter1
												.format(formatter3.parse(attendanceReportEntity.getTestStartTime())));
									if (attendanceReportEntity.getActualTestEndTime() == null || attendanceReportEntity
											.getActualTestEndTime().trim().equalsIgnoreCase("null"))
										malpracticeReportLogsEntity.setTestEndTime("Not Ended Test");
									if (attendanceReportEntity.getTestStartTime() == null || attendanceReportEntity
											.getTestStartTime().trim().equalsIgnoreCase("null"))
										malpracticeReportLogsEntity.setTestStartTime("Not Started Test");
								}
							}
						} else {
							malpracticeReportLogsEntity.setTestStartTime("Not Started Test");
							malpracticeReportLogsEntity.setTestEndTime("Not Ended Test");
						}
					}
				}
				malpracticeReportLogsEntities.add(malpracticeReportLogsEntity);
			}
			filePath = writeMalpracticeFeedbackToCSV(malpracticeReportLogsEntities);
		} catch (IOException ex) {
			logger.info("Error in exporting Candidate Feedback");
			exportFileEntity =
					new ExportFileEntity(RpsConstants.FAILED_STATUS, "Error in exporting Candidate Feedback", null);
			return gson.toJson(exportFileEntity);
		} catch (ParseException ex) {
			logger.info("Error in parsing date");
			exportFileEntity =
					new ExportFileEntity(RpsConstants.FAILED_STATUS, "Error in exporting Candidate Feedback", null);
			return gson.toJson(exportFileEntity);
		}

		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, filePath);
		logger.info("---OUT--- exportRPSMalpracticeLogsForBatchAcs()");
		return gson.toJson(exportFileEntity);
	}

	private String writeMalpracticeFeedbackToCSV(List<MalpracticeReportLogsEntity> entity) throws IOException {
		File folder = new File(apolloHome + File.separator + exportFolder);
		if (!folder.exists())
			folder.mkdirs();
		String filePath = folder + File.separator + RpsConstants.MALPRACTICE_REPORT
				+ Calendar.getInstance().getTimeInMillis() + ".CSV";
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(new File(filePath)));
		CSVWriter csvWriter = new CSVWriter(bufferedWriter);
		String[] headers = new String[] { RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.TestCenterName.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.BatchName.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.ScheduleStartDate.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.ScheduleEndDate.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.TestStartTime.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.TestEndTime.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.LoginName.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.Remarks.toString(),
				RpsConstants.MALPRACTICE_FEEDBACK_HEADERS.BookedDate.toString() };
		csvWriter.writeNext(headers);
		for (MalpracticeReportLogsEntity candidateFeedBackEntity : entity) {
			List<String> line = new ArrayList<>();
			line.add(candidateFeedBackEntity.getTestCenterName());
			line.add(candidateFeedBackEntity.getBatchName());
			line.add(candidateFeedBackEntity.getScheduleStartDate());
			line.add(candidateFeedBackEntity.getScheduleEndDate());
			line.add(candidateFeedBackEntity.getTestStartTime());
			line.add(candidateFeedBackEntity.getTestEndTime());
			line.add(candidateFeedBackEntity.getLoginName());
			line.add(candidateFeedBackEntity.getRemarks());
			line.add(candidateFeedBackEntity.getBookedDate());
			String[] values = line.toArray(new String[line.size()]);
			csvWriter.writeNext(values);
		}
		csvWriter.close();
		logger.info("filePath={}", filePath);
		return filePath;
	}

	public String exportRPSCandidateFeedbackForBatchAcs(String acsCode, String eventCode, String startDate,
			String endDate) {
		ExportTwoFilesEntity exportFileEntity = null;
		String filePath, filePath2;
		List<CandidateLogsViewEntity> CandidateLogsViewEntities =
				gson.fromJson(this.getCandidateFeedbackGridDataOnAcsCodes(acsCode, eventCode, startDate, endDate, null),
				new TypeToken<List<CandidateLogsViewEntity>>() {
				}.getType());
		List<Map<String, List<RPSCandidateFeedBackEntity>>> candToFeedbackMapList = new ArrayList<>();
		List<Map<String, List<RPSCandidateQuestionFeedBackEntity>>> questToFeedbackMapList = new ArrayList<>();
		try {
			for (CandidateLogsViewEntity candidateLogsViewEntity : CandidateLogsViewEntities) {
				Map<String, List<RPSCandidateFeedBackEntity>> candToFeedbackMap = new HashMap<>();
				Map<String, List<RPSCandidateQuestionFeedBackEntity>> questToFeedbackMap = new HashMap<>();
				ResponseStatus responseStatus =
						gson.fromJson(this.viewCandidateFeedback(candidateLogsViewEntity.getUniqueCandidateId(),
								candidateLogsViewEntity.getAssessmentID(),
						candidateLogsViewEntity.getAssessmentSetID()), new TypeToken<ResponseStatus>() {
						}.getType());
				List<CandidateFeedBackEntity> candFeedback = responseStatus.getResponseJson();
				List<RPSCandidateFeedBackEntity> rpscandFeedback = new ArrayList<>();
				List<RPSCandidateQuestionFeedBackEntity> rpsquestFeedback = new ArrayList<>();
				for (CandidateFeedBackEntity cFeedBackEntity : candFeedback) {
					RPSCandidateFeedBackEntity rpsCandidateFeedBackEntity = new RPSCandidateFeedBackEntity();
					RPSCandidateQuestionFeedBackEntity rpsCandidateQuestionFeedBackEntity =
							new RPSCandidateQuestionFeedBackEntity();
					rpsCandidateFeedBackEntity.setQuestionLabel(cFeedBackEntity.getQuestionId());
					rpsCandidateQuestionFeedBackEntity.setQuestionLabel(cFeedBackEntity.getQuestionId());
					rpsCandidateFeedBackEntity.setFeedBackItems(cFeedBackEntity.getFeedBackItems());
					rpsCandidateQuestionFeedBackEntity.setFeedBackItems(cFeedBackEntity.getFeedBackItems());
					RpsQuestionPaper rpsQuestionPaper = rpsCandidateResponseService
							.getUniqueQpidByCandUniqueId(candidateLogsViewEntity.getUniqueCandidateId());

					if (rpsQuestionPaper != null && rpsQuestionPaper.getUniqueQPID() != null) {
						rpsCandidateQuestionFeedBackEntity
								.setAssessmentID(Integer.toString(rpsQuestionPaper.getUniqueQPID()));
						rpsCandidateFeedBackEntity.setAssessmentID(Integer.toString(rpsQuestionPaper.getUniqueQPID()));
						rpsCandidateFeedBackEntity.setAssessmentName(
								candidateLogsViewEntity.getAssessmentID() + "-" + rpsQuestionPaper.getSetCode());
						rpsCandidateQuestionFeedBackEntity.setAssessmentName(
								candidateLogsViewEntity.getAssessmentID() + "-" + rpsQuestionPaper.getSetCode());
					}
					RpsQuestion rpsQuestion = rpsQuestionService.findByQIDAndAssessmentCodeByLanguage(
							cFeedBackEntity.getQuestionId(), candidateLogsViewEntity.getAssessmentID(),
							candidateLogsViewEntity.getQuestionLangugaeMap().get(cFeedBackEntity.getQuestionId()));
					if (rpsQuestion != null && rpsQuestion.getQid() != null) {
						rpsCandidateFeedBackEntity.setAnswerKey(rpsQuestion.getQans());
						rpsCandidateFeedBackEntity.setQuestionID(Integer.toString(rpsQuestion.getQid()));
						rpsCandidateQuestionFeedBackEntity.setQuestionID(Integer.toString(rpsQuestion.getQid()));
						rpsCandidateQuestionFeedBackEntity.setAnswerKey(rpsQuestion.getQans());

					}
					RpsBatch rpsBatch = rpsBatchService
							.findBatchCodeByUniqueCandidateId(candidateLogsViewEntity.getUniqueCandidateId());

					RpsAcsServer rpsAcsServer = rpsAcsServerServices
							.findACSServerCodeByUniqueCandidateId(candidateLogsViewEntity.getUniqueCandidateId());

					if (rpsBatch != null) {
						SimpleDateFormat formatter1 = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
						Date bst = rpsBatch.getBatchStartTime().getTime();
						String bStartTime = "";
						if (bst != null)
							bStartTime = formatter1.format(bst);
						rpsCandidateFeedBackEntity.setScheduleStartDate(bStartTime);
						SimpleDateFormat formatter2 = new SimpleDateFormat("ddMMyyyy");
						Date d = formatter1.parse(bStartTime);
						DateFormat f2 = new SimpleDateFormat("hmma");

						// batchname of format= acscode_dateddmmyyyy_batchname_batchstarttimetttam/pm_asscode
						rpsCandidateFeedBackEntity.setBatchName(rpsAcsServer.getAcsServerName() + "_"
								+ formatter2.format(rpsBatch.getBatchDate().getTime()) + "_" + rpsBatch.getBatchName()
								+ "_" + f2.format(d).toUpperCase() + "_" + candidateLogsViewEntity.getAssessmentID());
						RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationService
								.findByUniqueCandidateId(candidateLogsViewEntity.getUniqueCandidateId());
						rpsCandidateFeedBackEntity.setLoginName(rpsMasterAssociation.getLoginID());
					}
					// set other values
					rpscandFeedback.add(rpsCandidateFeedBackEntity);
					rpsquestFeedback.add(rpsCandidateQuestionFeedBackEntity);
				}
				candToFeedbackMap.put(Integer.toString(candidateLogsViewEntity.getUniqueCandidateId()),
						rpscandFeedback);
				candToFeedbackMapList.add(candToFeedbackMap);

				questToFeedbackMap.put(Integer.toString(candidateLogsViewEntity.getUniqueCandidateId()),
						rpsquestFeedback);
				questToFeedbackMapList.add(questToFeedbackMap);
			}
			filePath = writeCandidateFeedbackToCSV(candToFeedbackMapList);
			ResponseCandQuestionEntity candQuestFeedbackMapList =
					appendCandidateQuestionFeedbackToCSV(questToFeedbackMapList);
			logger.info("candQuestFeedbackMapList={}", candQuestFeedbackMapList);
			filePath2 = writeCandidateQuestionFeedbackToCSV(candQuestFeedbackMapList);
		} catch (IOException e) {
			logger.info("Error in exporting Candidate Feedback");
			exportFileEntity = new ExportTwoFilesEntity(RpsConstants.FAILED_STATUS,
					"Error in exporting Candidate Feedback", null, null);
			return gson.toJson(exportFileEntity);
		} catch (ParseException ex) {
			logger.info("Error in Date formatting");
			exportFileEntity =
					new ExportTwoFilesEntity(RpsConstants.FAILED_STATUS, "Error in Date formatting", null, null);
			return gson.toJson(exportFileEntity);
		}
		exportFileEntity = new ExportTwoFilesEntity(RpsConstants.SUCCESS_STATUS, null, filePath, filePath2);
		logger.info("---OUT--- exportCandidateFeedbackForBatchAcs()");
		return gson.toJson(exportFileEntity);
	}

	public Set<String> getAllAcsOneventCode(String eventCode, String startDate, String endDate) {
		logger.info("----IN--- getAllAcsOneventCode");
		Set<String> acsCodes = null;
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + ",  startDate :" + startDate
					+ " ,endDate : " + endDate);
			return acsCodes;
		}
		List<RpsAcsServer> rpsAcsServersList = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		try {
			Calendar rangeStartDate = Calendar.getInstance();
			Calendar rangeEndDate = Calendar.getInstance();
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
			// fetch list of acsServers for eventCode
			rpsAcsServersList = rpsBatchAcsAssociationService.getAllAcsServersByEventInDateRange(eventCode,
					rangeStartDate, rangeEndDate);
		} catch (ParseException e) {
			logger.error("Error in parsing date range selected--", e);
			return acsCodes;
		}

		if (rpsAcsServersList == null || rpsAcsServersList.isEmpty()) {

			logger.warn("List of acsServers is empty for-- eventCode: " + eventCode);
			return acsCodes;
		}

		acsCodes = new HashSet<>();
		for (RpsAcsServer rpsAcsServer : rpsAcsServersList)
			acsCodes.add(rpsAcsServer.getAcsServerId());
		logger.info("----OUT--- getAllAcsOneventCode");
		return acsCodes;
	}
// offline code for parse audit logs
	public List<List<String>> viewCandidateLang(List<Integer> uniqueCandidateIds) {
		logger.info("---IN--- viewCandidateLogs()");

		List<List<String>> candidates = new ArrayList<List<String>>(uniqueCandidateIds.size());
		Type rulesList = new TypeToken<List<CandidateSectionLanguagesDO>>() {
		}.getType();
		List<RpsMasterAssociationLiteForCandidateAudits> rpsMasterAssociations =
				rpsMasterAssociationService
				.findAllLiteMasterAssociationsByUniqueIds(uniqueCandidateIds);
		System.out.println("cand details fetched from db");
		if (rpsMasterAssociations != null) {
			List<String> header = new ArrayList<String>(2);
			header.add("Unique Candidate Id");
			header.add("Login Id");
			header.add("Assessment Code");
			header.add("Section Details");
			candidates.add(header);
			int i = 0;
			for (RpsMasterAssociationLiteForCandidateAudits rpsMasterAssociation : rpsMasterAssociations) {
				List<String> details = new ArrayList<String>(10);
				String logs = "";
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateLogs() != null) {
					logs = rpsMasterAssociation.getCandidateLogs();
					String[] lines = logs.split("\\r?\\n");
					if (lines != null && lines.length != 0) {
						try {
							details.add(rpsMasterAssociation.getUniqueCandidateId().toString());
							details.add(rpsMasterAssociation.getLoginID());
							details.add(rpsMasterAssociation.getAssessmentCode());
							logs = processCandidateLangsContentsSMUFFormat(lines);
							List<CandidateSectionLanguagesDO> beanList = new Gson().fromJson(logs, rulesList);
							for (CandidateSectionLanguagesDO cand : beanList) {
								details.add(cand.getSectionIdentifier() + " : " + cand.getCandSelectedLanguage());
							}
						} catch (ApolloSecurityException e) {
							e.printStackTrace();
						}
					}
				}
				candidates.add(details);
				System.out.println("fetched candidate details count " + i++);
			}
		}
		logger.info("---OUT--- viewCandidateLogs()");
		rpsMasterAssociations = null;
		uniqueCandidateIds = null;
		return candidates;
	}

	private String processCandidateLangsContentsSMUFFormat(String[] lines)
			throws ApolloSecurityException {

		logger.info("---IN--- processCandidateLogsContents()");
		int index = 0;
		// String processedText = null;
		// String lineSeparator = System.getProperty(RpsConstants.LINE_SEPARATOR);
		// List<String> processedLines = new ArrayList<>();

		// adding one extra line between header and content
		// processedLines.add("");

		// load all ACS specific ActionTypes
		Map<String, List<String>> actionValuesMap = loadAcsActionTypes();
		// int step = 0;

		while (index < lines.length) {
			// skip all the headers information
			index = skipCandidateLogsHeader(index, lines);
			// read through all other content of the log

			while (index < lines.length) {
				String line = lines[index++];
				// Getting property flag for decryption isCandAuditEncrypted
				// Delimiter for same kind of actions
				String[] tokensOnSameActions = line.split(candAuditDelimiterForSwitchOptionActions);
				for (String lineToken : tokensOnSameActions) {
					String[] tokens = lineToken.split(RpsConstants.LOG_CONTENT_DELIMITER);
					if (tokens == null || tokens.length == 0) {
						// adding all empty rows
						continue;
					}
					// processedLines.add("Step " + ++step + ":");
					line = decodeLangsByLineSMUFormat(tokens, actionValuesMap);
					if (line != null)
						return line;

				}
			}
		}
		// String processedText = StringUtils.join(processedLines, lineSeparator);
		logger.info("---OUT--- processCandidateLogsContents()");
		return null;
	}

	private String decodeLangsByLineSMUFormat(String[] tokens, Map<String, List<String>> actionValuesMap)
			throws ApolloSecurityException {
		logger.info("---IN--- decodeLogsByLineSMUFormat()");

		// Getting property flag for decryption isCandAuditEncrypted
		String processedLine = null;
		if (tokens.length == RpsConstants.CANDIDATE_LOG_LINE_LENGTH) {
			String logActionType = tokens[1].trim();
			String logAuditMsgJson = tokens[5].trim();
			String[] actionType = logActionType.split(RpsConstants.LOG_ACTION_DELIMITER);
			if (!(actionType[1].trim().equals(ACSAuditEnum.A54.toString()))) {
				return processedLine;
			}
			String decryptedLogAuditMsgJson = logAuditMsgJson;
			// decrypting logs here isCandAuditEncrypted
			if (isCandAuditEncrypted.equalsIgnoreCase("yes")) {
				decryptedLogAuditMsgJson = cryptUtil.decryptTextUsingAES(logAuditMsgJson,
						RpsConstants.SALT_FOR_ENCRYPTING_AND_DECRYPTING_ADMIN_CREDENTIALS);
			}

			Type type = new TypeToken<Object[]>() {
			}.getType();
			Object[] values = gson.fromJson(decryptedLogAuditMsgJson, type);

			List<String> list = actionValuesMap.get(actionType[1].trim());
			if (list != null && !list.isEmpty() && list.size() == 2) {
				String acsActionType = list.get(0);
				String acsAuditMsg = list.get(1);
				tokens[1] = acsActionType;
				tokens[5] = MessageFormat.format(acsAuditMsg, values);
			}
			// form a string using all the tokens
			processedLine = tokens[5];
			processedLine = processedLine.substring(32);
		}
		logger.info("---OUT--- decodeLogsByLine()");
		return processedLine;

	}

	public String exportCandidateLogsForPlayBack(CombinedBeanForPlayBack combinedBeanForPlayBack,
			Set<String> qPacksDelivered, String candidateLogsFileName, String candidateQpackFileName) {

		logger.info("---IN--- exportCandidateLogsForPlayBack()");
		List<String> logs = this.viewCandidateLogsForPlayBack(combinedBeanForPlayBack.getCandidateLogs());
		ExportFileEntity exportFileEntity = null;
		String finalResponse = null;
		if (logs == null || logs.isEmpty()) {
			logger.error("Candidate Logs is empty for uniqueCandidateId :"
					+ combinedBeanForPlayBack.getUniqueCandidateId());
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}
		List<Object> response = new ArrayList<>();
		response.add(logs);
		// response.add(rpsMasterAssociationIterator.getResponse());
		finalResponse = gson.toJson(response);

		File candidateLogsFolder = new File(candidateLogsFileName);

		File candidateQpacksFolder = new File(candidateQpackFileName);
		File rpsQpackPath = new File(apolloHome + File.separator + combinedBeanForPlayBack.getCustomerCode()
				+ File.separator + combinedBeanForPlayBack.getDivisionCode() + File.separator
				+ combinedBeanForPlayBack.getEventId() + File.separator + RpsConstants.PackType.QPACK
				+ File.separator + combinedBeanForPlayBack.getPackId());

		
		if (!candidateQpacksFolder.exists())
			candidateQpacksFolder.mkdirs();
		try {
			// TODO make a validation if already Qpack is there Skip this code
			cryptUtil = new CryptUtil(256);
			if(!qPacksDelivered.contains(combinedBeanForPlayBack.getPackId())){
			cryptUtil.decryptFileUsingAES(
					new File(rpsQpackPath + File.separator + combinedBeanForPlayBack.getPackId()
							+ RpsConstants.ZIP),
					new File(candidateQpacksFolder + File.separator + combinedBeanForPlayBack.getPackId()
							+ RpsConstants.ZIP),
					combinedBeanForPlayBack.getEventId());
			}
		} catch (IOException e1) {

			e1.printStackTrace();
		} catch (ApolloSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (!candidateLogsFolder.exists())
			candidateLogsFolder.mkdirs();
		File logFile = new File(candidateLogsFolder + File.separator
				+ combinedBeanForPlayBack.getAuditFileName() + RpsConstants.LOG);
		try {
			FileOutputStream fout = new FileOutputStream(logFile);
			byte[] bytes = finalResponse.getBytes();
			fout.write(bytes);
			fout.close();
		} catch (IOException e) {
			logger.error("ERROR in writing candidate logs", e);
			exportFileEntity = new ExportFileEntity(RpsConstants.FAILED_STATUS, "There is no data to export", null);
			return gson.toJson(exportFileEntity);
		}
		exportFileEntity = new ExportFileEntity(RpsConstants.SUCCESS_STATUS, null, logFile.getAbsolutePath());
		logger.info("---OUT--- exportCandidateLogsForPlayBack()");
		return gson.toJson(exportFileEntity);
	}

	public List<String> viewCandidateLogsForPlayBack(String logs) {
		
		logger.info("---IN--- viewCandidateLogsForPlayBack()");
		List<String> processedLines = null;
		String candidateName = "";
		if (logs != null) {
			String[] lines = logs.split("\\r?\\n");
			if (lines != null && lines.length != 0) {
				// logs= processCandidateLogsContents(lines);
				try {
					processedLines = processCandidateLogsContentsSMUFFormat(lines, candidateName);
					// , rpsMasterAssociation
				} catch (ApolloSecurityException e) {
					e.printStackTrace();
				}
			}
		}
		logger.info("---OUT--- viewCandidateLogsForPlayBack()");
		return processedLines;
	}

	private List<String> processCandidateLogsContentsSMUFFormat(String[] lines, String candidateName)
			// , RpsMasterAssociation rpsMasterAssociation
			throws ApolloSecurityException {

		logger.info("---IN--- processCandidateLogsContentsSMUFFormat()");
		int index = 0;
		String lineSeparator = "\n";
		List<String> processedLines = new ArrayList<>();

		// skip all the headers information
		String line = lines[index++];

		while (index < lines.length) {

			// to return body index match header with ClientTime
			if (line.startsWith("ClientTime")) {
				break;
			} else {
				processedLines.add(line);
			}
			line = lines[index++];
		}
		// read through all other content of the log

		while (index <= lines.length) {

			// Getting property flag for decryption isCandAuditEncrypted
			String candAuditDelimiterForSwitchOptionActions = RpsConstants.AUDIT_SEPERATOR;
			// Delimiter for same kind of actions
			String[] tokens = line.split(candAuditDelimiterForSwitchOptionActions);

			String logAuditMsgJson = tokens[5].trim();

			String decryptedLogAuditMsgJson = logAuditMsgJson;
			// decrypting logs here isCandAuditEncrypted
			if (isCandAuditEncrypted.equalsIgnoreCase("yes")) {
				cryptUtil = new CryptUtil(128);
				decryptedLogAuditMsgJson = cryptUtil.decryptTextUsingAES(logAuditMsgJson,
						RpsConstants.SALT_FOR_ENCRYPTING_AND_DECRYPTING_ADMIN_CREDENTIALS);
				line = line.replace(logAuditMsgJson, decryptedLogAuditMsgJson);
			}

			processedLines.add(line);
			if (index < lines.length)
				line = lines[index++];
			else
				index++;

		}
		logger.info("---OUT--- processCandidateLogsContentsSMUFFormat()");
		return processedLines;
	}

}
