package com.merittrac.apollo.acs.services;

import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.dataobject.AssessmentDetailsDO;
import com.merittrac.apollo.acs.dataobject.CDEDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateRawScoreDetailsDO;
import com.merittrac.apollo.acs.dataobject.CandidateRawScoreDetailsLiteDO;
import com.merittrac.apollo.acs.dataobject.QPSectionsDO;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsConfig;
import com.merittrac.apollo.acs.entities.AcsProperties;
import com.merittrac.apollo.acs.entities.ResultProcReportTO;
import com.merittrac.apollo.acs.entities.SectionReportTO;
import com.merittrac.apollo.acs.utility.ACSCommonUtility;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.EventRule;
import com.merittrac.apollo.common.entities.acs.BatchExtensionInformationEntity;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;
import com.merittrac.apollo.excel.CandidateSectionScoreData;
import com.merittrac.apollo.excel.CandidateSectionScoreDataLite;
import com.merittrac.apollo.excel.ExcelConstants;
import com.merittrac.apollo.excel.ExportToExcelUtility;
import com.merittrac.apollo.excel.FileExportExportEntity;
import com.merittrac.apollo.excel.ScoreReportEntity;
import com.merittrac.apollo.excel.ScoreReportEntityLite;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.GroupWeightageCutOffEntity;
import com.merittrac.apollo.qpd.qpgenentities.SectionWeightageCutOffEntity;
import com.merittrac.apollo.rps.resultprocessingrule.entity.GroupSectionsWeightageScore;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.rps.exception.ReportOperationException;
import com.merittrac.rps.service.ReportService;
import com.merittrac.rps.service.impl.ReportServiceImpl;

/**
 * Class for generating candidate raw score report
 * 
 * @author Moulya_P
 * 
 */

public class CandidateRawScoreReportService extends BasicService {
	private QuestionService questionService;
	private BatchService batchService;
	private CandidateService candidateService;
	private BatchCandidateAssociationService batchCandidateAssociationService;
	private CDEAService cdeaService;
	private ResultProcStatusService procStatusService;
	private ExportToExcelUtility exportToExcelUtility;
	private static ACSCommonUtility acsCommonUtility;
	private static ACSEventRequestsService acsEventRequestsService;
	private static final Logger logger = LoggerFactory.getLogger(CandidateRawScoreReportService.class);
	private static CustomerBatchService customerBatchService;
	private static ACSPropertyUtil acsPropertyUtil = null;
	DecimalFormat df = new DecimalFormat("#.00");

	public CandidateRawScoreReportService() {
		acsEventRequestsService = new ACSEventRequestsService();
		questionService = QuestionService.getInstance();
		batchService = BatchService.getInstance();
		candidateService = CandidateService.getInstance();
		cdeaService = CDEAService.getInstance();
		procStatusService = new ResultProcStatusService();
		batchCandidateAssociationService = new BatchCandidateAssociationService();
		exportToExcelUtility = new ExportToExcelUtility();
		acsCommonUtility = new ACSCommonUtility();
		customerBatchService = new CustomerBatchService();
		if (acsPropertyUtil == null)
			acsPropertyUtil = new ACSPropertyUtil();
	}

	/**
	 * 
	 * @param batchCode
	 * @throws GenericDataModelException
	 */
	public FileExportExportEntity getCandidateRawScoreReport(List<String> batchCodes, String assessmentCode,
			String setID) {

		FileExportExportEntity exportEntity = null;
		CandidateRawScoreDetailsDO candidateRawScoreDetailsDO = new CandidateRawScoreDetailsDO();
		Set<String> sectionIdentifierSet = new LinkedHashSet<String>();
		logger.debug("getCandidateRawScoreReport initiated with batchCode:{} assessmentCode:{} setId:{}", batchCodes,
				assessmentCode, setID);
		if (batchCodes.size() == 0) {
			logger.error("list of batchCodes is empty:" + batchCodes);
			return exportEntity;
		}
		String customerCode = "";
		try {
			for (String batchCode : batchCodes) {
				if (!batchCode.equals("0") && !assessmentCode.equals("0") && !setID.equals("0")) {
					candidateRawScoreDetailsDO =
							getCandidateRawScoreCardforQPSets(batchCode, assessmentCode, setID,
									candidateRawScoreDetailsDO, sectionIdentifierSet);
				} else if (!batchCode.equals("0") && !assessmentCode.equals("0") && setID.equals("0")) {
					candidateRawScoreDetailsDO =
							getCandidateRawScoreCardforAssessments(batchCode, assessmentCode,
									candidateRawScoreDetailsDO, sectionIdentifierSet);
				} else if (!batchCode.equals("0") && assessmentCode.equals("0")) {
					candidateRawScoreDetailsDO =
							getCandidateRawScoreCardforBatch(batchCode, candidateRawScoreDetailsDO,
									sectionIdentifierSet);
				}
			}

			// Create a Rawscore directory where raw score files should reside
			File path =
					new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
							+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
							+ ACSConstants.ACS_RAW_SCORE_DIR);
			if (!path.exists()) {
				path.mkdirs();
			}

			if (candidateRawScoreDetailsDO != null) {
				if ((candidateRawScoreDetailsDO.getScoreReportEntities() != null)
						&& (candidateRawScoreDetailsDO.getMaxSectionsCount() != 0)
						&& (candidateRawScoreDetailsDO.getSectionIdentifier() != null)) {

					Set<String> brRulesJsonSet =
							getBrRulesForAllAssessments(candidateRawScoreDetailsDO.getScoreReportEntities());

					String eventCode =
							candidateRawScoreDetailsDO.getEventCode() == null ? "" : candidateRawScoreDetailsDO
									.getEventCode();
					CDEDetailsDO cdeDetailsDO = cdeaService.getCustomerDivisionEventCodesByEventCode(eventCode);
					customerCode = cdeDetailsDO.getCustomerCode();
					boolean rule = getRemoteProctoringEventRules(eventCode);
					exportEntity =
							exportToExcelUtility.exportScoreReportGridDataForAcsMif(candidateRawScoreDetailsDO
									.getScoreReportEntities(), cdeDetailsDO.getCustomerCode(), cdeDetailsDO
											.getDivisionCode(),
									eventCode, cdeDetailsDO.getCustomerName(),
									cdeDetailsDO.getDivisionName(), cdeDetailsDO.getEventName(),
									candidateRawScoreDetailsDO.getMaxSectionsCount(), candidateRawScoreDetailsDO
											.getSectionIdentifier(),
									new File(path + File.separator
											+ ACSConstants.SCORE_REPORT_SHEET_NAME + System.currentTimeMillis()
											+ ExcelConstants.XLSX),
									candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() == null ? 0
											: candidateRawScoreDetailsDO.getTrainingDetailsMaxCount(),
									candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() == null ? 0
											: candidateRawScoreDetailsDO.getForeignLanguagesMaxCount(),
									brRulesJsonSet, rule);

				} else {
					exportEntity =
							new FileExportExportEntity("0", "",
									"Problem in Generating Candidate Raw  Score Report due to some null values");
				}
			}
		} catch (GenericDataModelException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IntrospectionException e) {
			logger.error("Error occured while generating the raw score report :" + e);
		}

		try {
			if (exportEntity != null && exportEntity.getStatus().equalsIgnoreCase(ExcelConstants.SUCCESS_STATUS)) {
				if (exportEntity.getFileLocation() != null && new File(exportEntity.getFileLocation()).exists()) {
					if (isCustomerSpecificReportRequired(exportEntity, customerCode)) {
						logger.debug(exportEntity + " Customer Specific Template Report is required");
						getCustomerSpecificTemplateReport(exportEntity, customerCode);
					} else {
						logger.debug(exportEntity + " Customer Specific Template Report not required");
						return exportEntity;
					}
					return exportEntity;
				}
			}
		} catch (ReportOperationException e) {
			logger.error("ReportOperationException while executing generateRawScoreReport On batchcode...", e);
			exportEntity.setErrorMsg(e.getLocalizedMessage());
		}
		return exportEntity;
	}

	private Set<String> getBrRulesForAllAssessments(List<ScoreReportEntity> scoreReportEntities)
			throws GenericDataModelException {
		Set<String> brRulesJsonSet = new HashSet<String>();
		if (scoreReportEntities == null || scoreReportEntities.isEmpty()) {
			return brRulesJsonSet;
		}
		Set<String> assessmentCodes = new HashSet<String>();
		for (ScoreReportEntity scoreReportEntity : scoreReportEntities) {
			AcsBussinessRulesAndLayoutRules brlr =
					cdeaService.getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(
							scoreReportEntity.getAssessmentCode(), scoreReportEntity.getBatch());

			if (brlr != null && brlr.getBrRules() != null && !brlr.getBrRules().isEmpty())
				brRulesJsonSet.add(brlr.getBrRules());
		}
		if (assessmentCodes == null || assessmentCodes.isEmpty()) {
			return brRulesJsonSet;
		}
		return brRulesJsonSet;
	}

	/**
	 * 
	 * @param batchCode
	 * @throws GenericDataModelException
	 */
	public String getCandidateRawScoreReportForGridData(List<String> batchCodes, String assessmentCode, String setID,
			Calendar startDate, Calendar endDate, int startIndex, int count) throws GenericDataModelException {

		CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO = null;
		// for server side pagination
		int offSet = 0;// count * (startIndex - 1);

		if (batchCodes.size() == 0) {
			logger.error("list of batchCodes is empty");
			throw new GenericDataModelException(99, "list of batchCodes is empty");
		}

		try {
			for (String batchCode : batchCodes) {
				if (!batchCode.equals("0") && !assessmentCode.equals("0") && !setID.equals("0")) {
					candidateRawScoreDetailsDO =
							getCandidateRawScoreCardforQPSets(batchCode, assessmentCode, setID, startDate, endDate);
				} else if (!batchCode.equals("0") && !assessmentCode.equals("0") && setID.equals("0")) {
					candidateRawScoreDetailsDO =
							getCandidateRawScoreCardforAssessments(batchCode, assessmentCode, startDate, endDate);
				} else
				if (!batchCode.equals("0") && assessmentCode.equals("0")) {
					candidateRawScoreDetailsDO = getCandidateRawScoreCardforBatch(batchCode, startDate, endDate);
				}
			}

		} catch (GenericDataModelException e) {
			logger.error("Error occured while generating the raw score report :" + e.getMessage());
		}

		return new Gson().toJson(candidateRawScoreDetailsDO);
	}

	/**
	 * 
	 * @throws GenericDataModelException
	 */
	public String getCandidateRawScoreReportForGridDataOnBCAIds(List<Integer> bcaids) throws GenericDataModelException {
		logger.debug("--IN-- getCandidateRawScoreReportForGridDataOnBCAIds() for bcaids :{}", bcaids);
		CandidateRawScoreDetailsDO candidateRawScoreDetailsDO = new CandidateRawScoreDetailsDO();
		Set<String> sectionIdentifierSet = new LinkedHashSet<String>();

		if (bcaids.size() == 0) {
			logger.error("list of batchCodes is empty");
			throw new GenericDataModelException(99, "list of batchCodes is empty");
		}

		try {
			List<CandidateDetailsDO> batchCandidateAssociations =
					batchCandidateAssociationService.getBatchCandAssoDetailsByBCAID(bcaids);
			logger.debug("fetched batchCandidateAssociations for bcaids :{}", batchCandidateAssociations);
			// fill Grid Data On BCAs
			fillGridDataOnBCAs(candidateRawScoreDetailsDO, sectionIdentifierSet, batchCandidateAssociations);
		} catch (GenericDataModelException e) {
			logger.error("Error occured while generating the raw score report :" + e.getMessage());
		}
		logger.debug("--OUT-- getCandidateRawScoreReportForGridDataOnBCAIds() for returning :{}",
				candidateRawScoreDetailsDO);
		return new Gson().toJson(candidateRawScoreDetailsDO);
	}

	private CandidateRawScoreDetailsDO getCandidateRawScoreCardforBatch(String batchCode,
			CandidateRawScoreDetailsDO candidateRawScoreDetailsDO, Set<String> sectionIdentifierSet)
			throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforBatch.. " + batchCode);
		List<CandidateDetailsDO> candidateDetailsDOList =
				batchCandidateAssociationService.getBatchCandAssociationByBatchCode(batchCode);

		// fill Grid Data On BCAs
		fillGridDataOnBCAs(candidateRawScoreDetailsDO, sectionIdentifierSet, candidateDetailsDOList);
		logger.info("--OUT-- getCandidateRawScoreCardforBatch.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;

	}

	private CandidateRawScoreDetailsLiteDO getCandidateRawScoreCardforBatch(String batchCode, Calendar startDate,
			Calendar endDate) throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforBatch.. " + batchCode);
		List<CandidateDetailsDO> candidateDetails =
				batchCandidateAssociationService.getBatchCandAssoDetailsByBatchCode(batchCode, startDate, endDate);

		// fill Grid Data On BCAs
		CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO = fillLiteGridDataOnBCAs(candidateDetails);
		logger.info("--OUT-- getCandidateRawScoreCardforBatch.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;

	}

	private CandidateRawScoreDetailsDO getCandidateRawScoreCardforAssessments(String batchCode, String assessmentCode,
			CandidateRawScoreDetailsDO candidateRawScoreDetailsDO, Set<String> sectionIdentifierSet)
			throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforAssessments.. " + batchCode, assessmentCode);
		List<CandidateDetailsDO> candidateDetailsDOList = 
				batchCandidateAssociationService.getBatchCandAssoDetailsByBatchCodeAndAssessmentCode(batchCode,
						assessmentCode);

		// fill Grid Data On BCAs
		fillGridDataOnBCAs(candidateRawScoreDetailsDO, sectionIdentifierSet, candidateDetailsDOList);
		logger.info("--OUT-- getCandidateRawScoreCardforAssessments.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;
	}

	private CandidateRawScoreDetailsLiteDO getCandidateRawScoreCardforAssessments(String batchCode,
			String assessmentCode, Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforAssessments.. " + batchCode, assessmentCode);
		List<CandidateDetailsDO> candidateDetails = batchCandidateAssociationService
				.getBatchCandAssoDetailsByBatchCodeAssessmentCode(batchCode, assessmentCode, startDate, endDate);

		// fill Grid Data On BCAs
		CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO = fillLiteGridDataOnBCAs(candidateDetails);
		logger.info("--OUT-- getCandidateRawScoreCardforBatch.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;

	}

	private CandidateRawScoreDetailsDO getCandidateRawScoreCardforQPSets(String batchCode, String assessmentCode,
			String setId, CandidateRawScoreDetailsDO candidateRawScoreDetailsDO, Set<String> sectionIdentifierSet)
			throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforQPSets.. " + batchCode, assessmentCode, setId);
		List<CandidateDetailsDO> candidateDetailsDoList = null;
		if (setId == null || setId == "0") {
			logger.debug("setId is null hence just assessmnet and batch are considered");
			candidateDetailsDoList =
					batchCandidateAssociationService.getBatchCandAssoDetailsByBatchCodeAndAssessmentCode(batchCode,
							assessmentCode);
		} else
			candidateDetailsDoList =
					batchCandidateAssociationService.getBatchCandAssoDetailsByBatchCodeAssessmentCodeSetId(batchCode,
							assessmentCode, setId);
		// fill Grid Data On BCAs
		fillGridDataOnBCAs(candidateRawScoreDetailsDO, sectionIdentifierSet, candidateDetailsDoList);
		logger.info("--OUT-- getCandidateRawScoreCardforQPSets.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;
	}

	private CandidateRawScoreDetailsLiteDO getCandidateRawScoreCardforQPSets(String batchCode, String assessmentCode,
			String setId, Calendar startDate, Calendar endDate) throws GenericDataModelException {
		logger.info("inside getCandidateRawScoreCardforQPSets.. " + batchCode, assessmentCode, setId);
		List<CandidateDetailsDO> candidateDetails = null;
		if (setId == null || setId == "0") {
			logger.debug("setId is null hence just assessmnet and batch are considered");
			candidateDetails = batchCandidateAssociationService
					.getBatchCandAssoDetailsByBatchCodeAssessmentCode(batchCode, assessmentCode, startDate, endDate);

		} else
			candidateDetails =
					batchCandidateAssociationService.getBatchCandAssoDetailsByBatchCodeAssessmentCodeSetId(batchCode,
							assessmentCode, setId, startDate, endDate);
		// fill Grid Data On BCAs
		CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO = fillLiteGridDataOnBCAs(candidateDetails);
		logger.info("--OUT-- getCandidateRawScoreCardforBatch.. returning candidateRawScoreDetailsDO = {} ",
				candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;
	}

	private void fillGridDataOnBCAs(CandidateRawScoreDetailsDO candidateRawScoreDetailsDO,
			Set<String> sectionIdentifierSet, List<CandidateDetailsDO> candidateDetails)
			throws GenericDataModelException {
		logger.debug("--IN-- fillGridDataOnBCAs() for batchCandidateAssociations :{}", candidateDetails);
		if (candidateDetails != null && !candidateDetails.isEmpty()) {
			// get eventcode
			String batchCode = candidateDetails.get(0).getBatchCode();
			String eventCode = cdeaService.getEventCodeByBatchCode(batchCode);
			// get ACS details
			AcsConfig assessmentServerCode = cdeaService.getACSConfigByBatchCode(batchCode);
			candidateRawScoreDetailsDO.setEventCode(eventCode);
			candidateRawScoreDetailsDO.setReportType(extractACSEventReportType(eventCode));
			// fetch candidate identifiers
			List<Integer> candIds = new ArrayList<Integer>();
			for (CandidateDetailsDO acsBatchCandidateAssociation : candidateDetails) {
				candIds.add(acsBatchCandidateAssociation.getCandidateId());
			}
			List<AcsCandidate> candidateTOs = candidateService.getCandidateByCandIds(candIds);
			List<ResultProcReportTO> resultProcReportTOs = procStatusService.findByBatchIDCandIDs(batchCode, candIds);
			Map<Integer, AcsCandidate> candidateMap = new HashMap<Integer, AcsCandidate>();
			Map<Integer, ResultProcReportTO> resultProcReportMap = new HashMap<Integer, ResultProcReportTO>();
			for (AcsCandidate candidate : candidateTOs) {
				candidateMap.put(candidate.getCandidateId(), candidate);
			}
			for (ResultProcReportTO resultProcReportTO : resultProcReportTOs) {
				resultProcReportMap.put(resultProcReportTO.getCandID(), resultProcReportTO);
			}
			resultProcReportTOs = null;
			candidateTOs = null;
			candIds = null;
			Map<String, List<QPSectionsDO>> mapOfSectionLists = getMapOfSectionsForBatch(batchCode);

			for (CandidateDetailsDO candidateDetail : candidateDetails) {
				// filling all details of a candidate
				List<QPSectionsDO> qpsections = mapOfSectionLists.get(candidateDetail.getQpaperCode());
				getScoreDetailsFortheCandidate(candidateRawScoreDetailsDO, sectionIdentifierSet,
						candidateDetail, resultProcReportMap,
						assessmentServerCode, qpsections);
			}
			List<String> sectionIdentifier = new ArrayList<String>(sectionIdentifierSet);

			int maxSectionsCount = sectionIdentifier.size();
			if (null != candidateRawScoreDetailsDO && candidateRawScoreDetailsDO.getScoreReportEntities() != null) {
				Iterator<ScoreReportEntity> scoreIterator =
						candidateRawScoreDetailsDO.getScoreReportEntities().iterator();
				List<CandidateSectionScoreData> cList = null;
				while (scoreIterator.hasNext()) {
					ScoreReportEntity sReportEntity = scoreIterator.next();
					cList = sReportEntity.getCandidateSectionScoreDataList();
					if (maxSectionsCount == cList.size()) {
						break;
					}
				}
			}
			candidateRawScoreDetailsDO.setMaxSectionsCount(maxSectionsCount);
			candidateRawScoreDetailsDO.setSectionIdentifier(sectionIdentifier);

		}
		logger.debug("--OUT-- fillGridDataOnBCAs() returning :{}", candidateRawScoreDetailsDO);
	}

	private CandidateRawScoreDetailsLiteDO fillLiteGridDataOnBCAs(List<CandidateDetailsDO> candidateDetails)
			throws GenericDataModelException {
		logger.debug("--IN-- fillGridDataOnBCAs() for candidateDetails size :{}", candidateDetails.size());
		CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO = new CandidateRawScoreDetailsLiteDO();
		Set<String> sectionIdentifierSet = new LinkedHashSet<String>();
		if (candidateDetails != null && !candidateDetails.isEmpty()) {
			String batchCode = candidateDetails.get(0).getBatchCode();
			// get eventcode
			String eventCode = candidateDetails.get(0).getEventCode();
			candidateRawScoreDetailsDO.setEventCode(eventCode);
			// report type
			candidateRawScoreDetailsDO.setReportType(extractACSEventReportType(eventCode));

			// fetch candidate identifiers
			Map<Integer, ResultProcReportTO> resultProcReportMap = extractResultProcTOs(candidateDetails, batchCode);

			Map<String, List<QPSectionsDO>> mapOfSectionLists = getMapOfSectionsForBatch(batchCode);

			for (CandidateDetailsDO candidateInfo : candidateDetails) {
				// filling all details of a candidate
				getLiteScoreDetailsFortheCandidate(candidateRawScoreDetailsDO, sectionIdentifierSet,
						candidateInfo, resultProcReportMap, mapOfSectionLists.get(candidateInfo.getQpaperCode()));
			}
			List<String> sectionIdentifier = new ArrayList<String>(sectionIdentifierSet);

			int maxSectionsCount = sectionIdentifierSet.size();
			if (null != candidateRawScoreDetailsDO && candidateRawScoreDetailsDO.getScoreReportEntities() != null) {
				Iterator<ScoreReportEntityLite> scoreIterator =
						candidateRawScoreDetailsDO.getScoreReportEntities().iterator();
				List<CandidateSectionScoreDataLite> cList = null;
				while (scoreIterator.hasNext()) {
					ScoreReportEntityLite sReportEntity = scoreIterator.next();
					cList = sReportEntity.getCandidateSectionScoreDataList();
					if (maxSectionsCount == cList.size()) {
						break;
					}
				}
			}
			candidateRawScoreDetailsDO.setMaxSectionsCount(maxSectionsCount);
			candidateRawScoreDetailsDO.setSectionIdentifier(sectionIdentifier);

		}
		logger.debug("--OUT-- fillGridDataOnBCAs() returning :{}", candidateRawScoreDetailsDO);
		return candidateRawScoreDetailsDO;
	}

	private Map<String, List<QPSectionsDO>> getMapOfSectionsForBatch(String batchCode)
			throws GenericDataModelException {
		List<String> qpaperCodes = batchCandidateAssociationService.getQpaperCodesByBatchCode(batchCode);

		List<QPSectionsDO> allQPSections =
				questionService.getSidsFromQpaperCodes(new ArrayList<>(qpaperCodes));
		// map of sections
		Map<String, List<QPSectionsDO>> mapOfSectionLists = new HashMap<>();
		for (QPSectionsDO sections : allQPSections) {
			String key = sections.getQpaperCode();
			if (mapOfSectionLists.containsKey(key)) {
				List<QPSectionsDO> qpSections = mapOfSectionLists.get(key);
				qpSections.add(sections);
			} else {
				List<QPSectionsDO> qpSections = new ArrayList<QPSectionsDO>();
				qpSections.add(sections);
				mapOfSectionLists.put(key, qpSections);
			}
		}
		return mapOfSectionLists;
	}

	private String extractACSEventReportType(String eventCode)
			throws GenericDataModelException {
		String reporttype = null;
		AcsProperties acsProperties = acsEventRequestsService
				.getAcsPropertiesOnPropNameAndEventCode(EventRule.PRO_CERTIFICATE_FORMAT.toString(), eventCode);
		if (acsProperties != null)
			reporttype = (acsProperties.getPropertyValue());
		else {
			logger.debug(" ACS properties EVENT RULES is NULL for eventcode:{} , hence displaying IIBF", eventCode);
			reporttype = (ACSConstants.IIBF);
		}
		return reporttype;
	}

	private Map<Integer, ResultProcReportTO> extractResultProcTOs(List<CandidateDetailsDO> candidateDetails,
			String batchCode)
			throws GenericDataModelException {
		List<Integer> candIds = new ArrayList<Integer>();
		for (CandidateDetailsDO candidateDetailsDO : candidateDetails) {
			candIds.add(candidateDetailsDO.getCandidateId());
		}
		List<ResultProcReportTO> resultProcReportTOs = procStatusService.findByBatchIDCandIDs(batchCode, candIds);
		Map<Integer, ResultProcReportTO> resultProcReportMap = new HashMap<Integer, ResultProcReportTO>();
		for (ResultProcReportTO resultProcReportTO : resultProcReportTOs) {
			resultProcReportMap.put(resultProcReportTO.getCandID(), resultProcReportTO);
		}
		resultProcReportTOs = null;
		candIds = null;
		return resultProcReportMap;
	}

	private void getScoreDetailsFortheCandidate(CandidateRawScoreDetailsDO candidateRawScoreDetailsDO,
			Set<String> sectionIdentifierSet, CandidateDetailsDO candidateDetails,
			Map<Integer, ResultProcReportTO> resultProcReportMap,
			AcsConfig assessmentServerCode, List<QPSectionsDO> qpSections)
			throws GenericDataModelException {
		logger.debug("--IN-- getScoreDetailsFortheCandidate() for acsBatchCandidateAssociation :{}",
				candidateDetails);
		List<CandidateSectionScoreData> candidateSectionScoreDataList = new ArrayList<CandidateSectionScoreData>();
		int candidateId = candidateDetails.getCandidateId();
		String batchCode = candidateDetails.getBatchCode();
		String assessmentCode = candidateDetails.getAssessmentCode();
		String assessmentName = getAssessmentNameFromAssessmentCode(assessmentCode);

		// adding section identifiers for all the candidates
		ResultProcReportTO procReportTO = resultProcReportMap.get(candidateId);
		int count = 0;
		double totalMarks = 0;
		if (null != qpSections && !qpSections.isEmpty() && procReportTO != null) {
			for (QPSectionsDO qpSectionsDO : qpSections) {
				count += qpSectionsDO.getQuestionsCount();
				totalMarks += qpSectionsDO.getMarks();
				// get Section Level Scores
				getSectionLevelScores(sectionIdentifierSet, candidateSectionScoreDataList, procReportTO, qpSectionsDO,
						candidateDetails.getQpaperCode(), candidateDetails.getBcaid());
			}
		}

		int totalQuestions = count;

		// add weightage details score
		Map<String, GroupSectionsWeightageScore> groupWeightageMap = getGroupWeightageScores(
				candidateSectionScoreDataList,
				getGroupWeightageMapFromBrRule(candidateDetails.getBatchCode(), candidateDetails.getAssessmentCode()));

		ScoreReportEntity scoreReportEntity = new ScoreReportEntity();

		// fill Score Report Entity
		fillScoreReportEntity(batchCode, candidateRawScoreDetailsDO, candidateDetails,
				candidateSectionScoreDataList, assessmentName, procReportTO, totalQuestions, scoreReportEntity,
				qpSections, assessmentServerCode, totalMarks);
		scoreReportEntity.setGroupWeightageMap(groupWeightageMap);
		candidateRawScoreDetailsDO.getScoreReportEntities().add(scoreReportEntity);

		logger.debug("--OUT-- getScoreDetailsFortheCandidate() returning :{}", candidateRawScoreDetailsDO);
	}

	private void getLiteScoreDetailsFortheCandidate(CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO,
			Set<String> sectionIdentifierSet, CandidateDetailsDO candidateInfo,
			Map<Integer, ResultProcReportTO> resultProcReportMap, List<QPSectionsDO> qpSections)
			throws GenericDataModelException {
		logger.debug("--IN-- getScoreDetailsFortheCandidate() for candidateInfo bcaid :{}", candidateInfo.getBcaid());
		List<CandidateSectionScoreDataLite> candidateSectionScoreDataList =
				new ArrayList<CandidateSectionScoreDataLite>();
		int candidateId = candidateInfo.getCandidateId();
		String batchCode = candidateInfo.getBatchCode();
		String assessmentCode = candidateInfo.getAssessmentCode();
		String assessmentName = getAssessmentNameFromAssessmentCode(assessmentCode);
		// adding section identifiers for all the candidates
		ResultProcReportTO procReportTO = resultProcReportMap.get(candidateId);
		int count = 0;
		if (null != qpSections && !qpSections.isEmpty() && procReportTO != null) {
			for (QPSectionsDO qpSectionsDO : qpSections) {
				count = count + qpSectionsDO.getQuestionsCount();
				sectionIdentifierSet.add(qpSectionsDO.getSecTitle());
				// get Section Level Scores
				getLiteSectionLevelScores(candidateSectionScoreDataList, procReportTO,
						qpSectionsDO,
						candidateInfo.getQpaperCode(), candidateInfo.getBcaid());
			}
		}

		int totalQuestions = count;

		ScoreReportEntityLite scoreReportEntity = new ScoreReportEntityLite();
		scoreReportEntity.setTotalQuestions(totalQuestions);
		// fill Score Report Entity
		fillScoreReportEntityLite(batchCode, candidateRawScoreDetailsDO, candidateInfo,
				candidateSectionScoreDataList, assessmentName, procReportTO, scoreReportEntity);
		candidateRawScoreDetailsDO.getScoreReportEntities().add(scoreReportEntity);

		logger.debug("--OUT-- getScoreDetailsFortheCandidate() returning :{}", candidateRawScoreDetailsDO);
	}

	/**
	 * get Group Weightage Scores
	 * 
	 * @param candidateSectionScoreDataList
	 * @param map
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private Map<String, GroupSectionsWeightageScore> getGroupWeightageScores(
			List<CandidateSectionScoreData> candidateSectionScoreDataList,
			Map<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntitymap) {
		Map<String, GroupSectionsWeightageScore> groupWeightageMap = null;
		if (candidateSectionScoreDataList == null || sectionWeightageCutOffEntitymap == null
				|| candidateSectionScoreDataList.isEmpty() || sectionWeightageCutOffEntitymap.isEmpty()) {
			return groupWeightageMap;
		}

		Map<String, CandidateSectionScoreData> candidateSectionScoreDataMap =
				new HashMap<String, CandidateSectionScoreData>();
		for (CandidateSectionScoreData candidateSectionScoreData : candidateSectionScoreDataList) {
			candidateSectionScoreDataMap.put(candidateSectionScoreData.getSectionIdentifier(),
					candidateSectionScoreData);
		}
		groupWeightageMap = new HashMap<String, GroupSectionsWeightageScore>();
		for (Map.Entry<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntities : sectionWeightageCutOffEntitymap
				.entrySet()) {
			double weightagePerc = 0;
			GroupSectionsWeightageScore groupSectionsWeightageScore = new GroupSectionsWeightageScore();
			List<SectionWeightageCutOffEntity> sectionWeightageCutOffValues = sectionWeightageCutOffEntities.getValue();
			String groupName = sectionWeightageCutOffEntities.getKey();
			for (SectionWeightageCutOffEntity sectionWeightageCutOffEntity : sectionWeightageCutOffValues) {
				String sectionId = sectionWeightageCutOffEntity.getSectionId();
				Float weightage = sectionWeightageCutOffEntity.getWeightage();
				CandidateSectionScoreData candidateSectionScoreData = candidateSectionScoreDataMap.get(sectionId);
				double candScorePerc = Double.parseDouble(candidateSectionScoreData.getScorePercentage());
				weightagePerc += candScorePerc * (weightage / 100);
			}
			groupSectionsWeightageScore.setScore(df.format(weightagePerc));
			groupWeightageMap.put(groupName, groupSectionsWeightageScore);
		}
		return groupWeightageMap;
	}

	private void fillScoreReportEntity(String batchCode, CandidateRawScoreDetailsDO candidateRawScoreDetailsDO,
			CandidateDetailsDO candidateDetails,
			List<CandidateSectionScoreData> candidateSectionScoreDataList, String assessmentName,
			ResultProcReportTO procReportTO, int totalQuestions, ScoreReportEntity scoreReportEntity,
			List<QPSectionsDO> qpSections, AcsConfig assessmentServerCode, double totalMarks)
			throws GenericDataModelException {
		logger.debug(
				"--IN-- fillScoreReportEntity() for acsBatchCandidateAssociation :{} and candidateRawScoreDetailsDO = {}",
				candidateDetails, candidateRawScoreDetailsDO);
		scoreReportEntity.setBcaid(candidateDetails.getBcaid());
		if (acsPropertyUtil.getRemoteProctoringURL() != null) {
			scoreReportEntity.setCandidateRemoteProctoringId(acsPropertyUtil.getRemoteProctoringURL()
					+ candidateDetails.getCandidateRemoteProctoringId());
		}
		scoreReportEntity.setBatch(batchCode);
		if (candidateDetails.getMifData() != null
				&& !(candidateDetails.getMifData().isEmpty())) {
			fillMIFDetails(candidateRawScoreDetailsDO, candidateDetails, scoreReportEntity);
		} else {
			scoreReportEntity.setUniqueCandidateId("");
			scoreReportEntity.setFirstName(candidateDetails.getFirstName() == null ? ""
					: candidateDetails.getFirstName());
			scoreReportEntity.setLastName(candidateDetails.getLastName() == null ? ""
					: candidateDetails.getLastName());
			scoreReportEntity.setEmailId(candidateDetails.getEmailId1() == null ? ""
					: candidateDetails.getEmailId1());
			scoreReportEntity.setMobileNo(candidateDetails.getPhoneNumber1() == null ? ""
					: candidateDetails.getPhoneNumber1());
			scoreReportEntity.setTestCity("");
			scoreReportEntity.setMIFAvailable(false);
		}
		scoreReportEntity.setAssessmentCode(candidateDetails.getAssessmentCode());
		scoreReportEntity.setAssessmentName(assessmentName);
		scoreReportEntity.setDbCandidateId(String.valueOf(candidateDetails.getCandidateId()));
		scoreReportEntity.setCandidateId(candidateDetails.getIdentifier1());
		scoreReportEntity.setLoginId(candidateDetails.getCandidateLogin());
		scoreReportEntity.setSetCode(candidateDetails.getSetID());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		scoreReportEntity.setMifJson(candidateDetails.getMifData());

		if (candidateDetails.getActualTestStartedTime() != null) {
			Date testStartDate = candidateDetails.getActualTestStartedTime().getTime();
			scoreReportEntity.setTestStartDate(dateFormat.format(testStartDate));
		} else {
			scoreReportEntity.setTestStartDate(dateFormat.format(new Date()));
		}
		if (candidateDetails.getActualTestEndTime() != null) {
			Date testEndDate = candidateDetails.getActualTestEndTime().getTime();
			scoreReportEntity.setTestEndDate(dateFormat.format(testEndDate));
		} else {

			scoreReportEntity.setTestEndDate(dateFormat.format(new Date()));
		}
		// time taken
		Long time = candidateDetails.getTimeSpentInSecs();
		if (time == null)
			time = candidateService.getCandidateSpentTime(candidateDetails);
		int[] convertedTime = TimeUtil.convertSecsTimeToMinsAndSecs(time);
		StringBuilder line = new StringBuilder();
		if (convertedTime[0] > 0)
			line.append(convertedTime[0] + " Hours ");
		if (convertedTime[1] > 0)
			line.append(convertedTime[1] + " Minutes ");
		if (convertedTime[2] > 0)
			line.append(convertedTime[2] + " Seconds ");

		if (line.toString().isEmpty())
			scoreReportEntity.setCandidateTimeTaken(ACSConstants.NA);
		scoreReportEntity.setCandidateTimeTaken(line.toString());

		scoreReportEntity.setNumbOfTimesCrashed(String.valueOf(candidateDetails.getClearLoginCount()));

		scoreReportEntity.setTotalQuestions(totalQuestions);
		scoreReportEntity.setCandidateSectionScoreDataList(candidateSectionScoreDataList);
		if (procReportTO != null) {
			scoreReportEntity.setAttemptedQuestionsCount(procReportTO.getAttemptedCount() == null ? 0 : procReportTO
					.getAttemptedCount());
			scoreReportEntity.setCorrectQuestionsCount(procReportTO.getCorrectCount() == null ? 0 : procReportTO
					.getCorrectCount());


			scoreReportEntity
					.setTotalScore(df.format(procReportTO.getTotalMarks()));
			scoreReportEntity
					.setScorePercentage(df.format(procReportTO.getPercntgeMarks()));
		} else {
			scoreReportEntity.setTotalScore("0.00");
			scoreReportEntity.setScorePercentage("0.00");
			scoreReportEntity.setAttemptedQuestionsCount(0);
			scoreReportEntity.setCorrectQuestionsCount(0);
		}
		Integer cutOffMark = getCutOffMarkFromBrRule(candidateDetails, totalMarks);
		scoreReportEntity.setTotalQualifyingMark(Integer.toString(cutOffMark));

		if (assessmentServerCode != null) {
			scoreReportEntity.setTestCenter(
					assessmentServerCode.getVenueName() == null ? " " : assessmentServerCode.getVenueName());
			scoreReportEntity.setAcsServerName(assessmentServerCode.getAssessmentServerName() == null ? " "
					: assessmentServerCode.getAssessmentServerName());
		}
		if (candidateRawScoreDetailsDO.getScoreReportEntities() == null) {
			List<ScoreReportEntity> scoreReportEntities = new ArrayList<ScoreReportEntity>();
			candidateRawScoreDetailsDO.setScoreReportEntities(scoreReportEntities);
		}
		logger.debug("--OUT-- fillScoreReportEntity() returning :{}", scoreReportEntity);
	}

	private void fillScoreReportEntityLite(String batchCode, CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO,
			CandidateDetailsDO candidateDetailsDO,
			List<CandidateSectionScoreDataLite> candidateSectionScoreDataList, String assessmentName,
			ResultProcReportTO procReportTO, ScoreReportEntityLite scoreReportEntity) throws GenericDataModelException {
		logger.debug(
				"--IN-- fillScoreReportEntity() for acsBatchCandidateAssociation :{} and candidateRawScoreDetailsDO = {}",
				candidateDetailsDO, candidateRawScoreDetailsDO);
		scoreReportEntity.setBcaid(candidateDetailsDO.getBcaid());
		if (acsPropertyUtil.getRemoteProctoringURL() != null) {
			scoreReportEntity.setCandidateRemoteProctoringId(acsPropertyUtil.getRemoteProctoringURL()
					+ candidateDetailsDO.getCandidateRemoteProctoringId());
		}
		if (candidateDetailsDO.getMifData() != null && !(candidateDetailsDO.getMifData().isEmpty())) {
			fillLiteMIFDetails(candidateRawScoreDetailsDO, candidateDetailsDO, scoreReportEntity);
		} else {
			scoreReportEntity.setUniqueCandidateId("");
			scoreReportEntity
					.setFirstName(candidateDetailsDO.getFirstName() == null ? "" : candidateDetailsDO.getFirstName());
			scoreReportEntity
					.setLastName(candidateDetailsDO.getLastName() == null ? "" : candidateDetailsDO.getLastName());
			scoreReportEntity
					.setEmailId(candidateDetailsDO.getEmailId1() == null ? "" : candidateDetailsDO.getEmailId1());
			scoreReportEntity.setMobileNo(
					candidateDetailsDO.getPhoneNumber1() == null ? "" : candidateDetailsDO.getPhoneNumber1());
			scoreReportEntity.setTestCity("");
		}
		scoreReportEntity.setAssessmentName(assessmentName);
		scoreReportEntity.setDbCandidateId(String.valueOf(candidateDetailsDO.getCandidateId()));
		scoreReportEntity.setCandidateId(candidateDetailsDO.getIdentifier1());
		scoreReportEntity.setLoginId(candidateDetailsDO.getCandidateLogin());
		scoreReportEntity.setSetCode(candidateDetailsDO.getSetID());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		if (candidateDetailsDO.getActualTestStartedTime() != null) {
			Date testStartDate = candidateDetailsDO.getActualTestStartedTime().getTime();
			scoreReportEntity.setTestStartDate(dateFormat.format(testStartDate));
		} else {
			scoreReportEntity.setTestStartDate(dateFormat.format(new Date()));
		}
		if (candidateDetailsDO.getActualTestEndTime() != null) {
			Date testEndDate = candidateDetailsDO.getActualTestEndTime().getTime();
			scoreReportEntity.setTestEndDate(dateFormat.format(testEndDate));
		} else {

			scoreReportEntity.setTestEndDate(dateFormat.format(new Date()));
		}


		scoreReportEntity.setCandidateSectionScoreDataList(candidateSectionScoreDataList);
		if (procReportTO != null) {
			scoreReportEntity.setAttemptedQuestionsCount(
					procReportTO.getAttemptedCount() == null ? 0 : procReportTO.getAttemptedCount());
			scoreReportEntity.setCorrectQuestionsCount(
					procReportTO.getCorrectCount() == null ? 0 : procReportTO.getCorrectCount());

			scoreReportEntity.setTotalScore(df.format(procReportTO.getTotalMarks()));
			scoreReportEntity.setScorePercentage(df.format(procReportTO.getPercntgeMarks()));
		} else {
			scoreReportEntity.setTotalScore("0.00");
			scoreReportEntity.setScorePercentage("0.00");
			scoreReportEntity.setAttemptedQuestionsCount(0);
			scoreReportEntity.setCorrectQuestionsCount(0);
		}

		if (candidateRawScoreDetailsDO.getScoreReportEntities() == null) {
			List<ScoreReportEntityLite> scoreReportEntities = new ArrayList<ScoreReportEntityLite>();
			candidateRawScoreDetailsDO.setScoreReportEntities(scoreReportEntities);
		}
		logger.debug("--OUT-- fillScoreReportEntity() returning :{}", scoreReportEntity);
	}

	private Integer
			getCutOffMarkFromBrRule(CandidateDetailsDO candidateDetails, double totalMarks)
					throws GenericDataModelException {
		Integer cutOffMark = 0;
		BRulesExportEntity bRulesExportEntity = customerBatchService
				.getBRulesExportEntity(candidateDetails.getBatchCode(), candidateDetails.getAssessmentCode());
		if (bRulesExportEntity != null && bRulesExportEntity.getResultProcessingRules() != null) {
			Map<String, GroupWeightageCutOffEntity> groupDetailsMap =
					bRulesExportEntity.getResultProcessingRules().getGroupDetailsMap();
			if (groupDetailsMap != null) {
				GroupWeightageCutOffEntity cutOffEntity = groupDetailsMap.get(ACSConstants.GROUP_LEVEL_CUT_OFF_KEY);
				if (cutOffEntity != null && cutOffEntity.getCutOff() != null) {
					double score = (totalMarks * ((double) cutOffEntity.getCutOff())) / 100;
					double roundOff = Math.round(score);
					cutOffMark = (int) roundOff;
				}
			}
		}
		return cutOffMark;
	}

	private Map<String, List<SectionWeightageCutOffEntity>> getGroupWeightageMapFromBrRule(String batchCode,
			String assessmentCode) throws GenericDataModelException {
		Map<String, List<SectionWeightageCutOffEntity>> groupSectionDetailsMap = null;
		BRulesExportEntity bRulesExportEntity = customerBatchService.getBRulesExportEntity(batchCode, assessmentCode);
		if (bRulesExportEntity != null && bRulesExportEntity.getResultProcessingRules() != null) {
			groupSectionDetailsMap =
					bRulesExportEntity.getResultProcessingRules().getGroupSectionMap();
		}
		return groupSectionDetailsMap;
	}

	private void getSectionLevelScores(Set<String> sectionIdentifierSet,
			List<CandidateSectionScoreData> candidateSectionScoreDataList, ResultProcReportTO procReportTO,
			QPSectionsDO qpSectionsDO, String qpapercode, int bcaid) throws GenericDataModelException {
		logger.debug("--IN-- getSectionLevelScores() for procReportTO :{}", procReportTO);
		sectionIdentifierSet.add(qpSectionsDO.getSecTitle());
		Double candSecScore = 0.00;
		double totalSecScore = qpSectionsDO.getMarks();
		Set<SectionReportTO> sectionReports = procReportTO.getSectionReports();
		if (sectionReports != null && !sectionReports.isEmpty()) {
			for (SectionReportTO sectionReportTO : sectionReports) {
				if (qpSectionsDO.getSecTitle().equals(sectionReportTO.getSection())) {
					candSecScore = sectionReportTO.getMarksObtained();
				}
			}
		}
		int totalQuestionsPerSection = qpSectionsDO.getQuestionsCount();
		int attemptedQuestionsPerSection =
				questionService.getSectionLevelAttemptedQuestions(bcaid, qpSectionsDO.getSid(), qpapercode);
		int correctQuestionsPerSection =
				questionService.getCorrectNumberOfQuestionsPerSection(qpSectionsDO.getSid(), bcaid);
		Double scorePercentage = 0.00;
		if (totalSecScore > 0) {
			scorePercentage = (candSecScore / totalSecScore) * (ACSConstants.PERCENTAGE);
			DecimalFormat df = new DecimalFormat("#.00");
			CandidateSectionScoreData scoreData =
					new CandidateSectionScoreData(String.valueOf(qpSectionsDO.getSid()), qpSectionsDO.getSecTitle(),
							qpSectionsDO.getSecIdent(),
							df.format(candSecScore), df.format(scorePercentage), df.format(totalSecScore),
							String.valueOf(totalQuestionsPerSection), String.valueOf(attemptedQuestionsPerSection));
			if (candSecScore <= 0)
				scoreData.setScorePercentage("0.00");
			scoreData.setSectionCorrectQuestionsCount(String.valueOf(correctQuestionsPerSection));
			candidateSectionScoreDataList.add(scoreData);
		}

		logger.debug("--OUT-- getSectionLevelScores() returning :{}", candidateSectionScoreDataList);
	}

	private void getLiteSectionLevelScores(List<CandidateSectionScoreDataLite> candidateSectionScoreDataList,
			ResultProcReportTO procReportTO,
			QPSectionsDO qpSectionsDO, String qpapercode, int bcaid) throws GenericDataModelException {
		logger.debug("--IN-- getSectionLevelScores() for procReportTO :{}", procReportTO);

		Double candSecScore = 0.00;
		double totalSecScore = qpSectionsDO.getMarks();
		Set<SectionReportTO> sectionReports = procReportTO.getSectionReports();
		if (sectionReports != null && !sectionReports.isEmpty()) {
			for (SectionReportTO sectionReportTO : sectionReports) {
				if (qpSectionsDO.getSecTitle().equals(sectionReportTO.getSection())) {
					candSecScore = sectionReportTO.getMarksObtained();
				}
			}
		}
		Double scorePercentage = 0.00;
		if (totalSecScore > 0) {
			scorePercentage = (candSecScore / totalSecScore) * (ACSConstants.PERCENTAGE);
			DecimalFormat df = new DecimalFormat("#.00");
			CandidateSectionScoreDataLite scoreData = new CandidateSectionScoreDataLite(
					String.valueOf(qpSectionsDO.getSid()),
					qpSectionsDO.getSecTitle(), qpSectionsDO.getSecIdent(), df.format(candSecScore),
					df.format(scorePercentage));
			if (candSecScore <= 0)
				scoreData.setScorePercentage("0.00");
			candidateSectionScoreDataList.add(scoreData);
		}

		logger.debug("--OUT-- getSectionLevelScores() returning :{}", candidateSectionScoreDataList);
	}

	private void fillMIFDetails(CandidateRawScoreDetailsDO candidateRawScoreDetailsDO, CandidateDetailsDO acsCandidate,
			ScoreReportEntity scoreReportEntity) {
		scoreReportEntity.setMIFAvailable(true);
		MIFForm mif = new Gson().fromJson(acsCandidate.getMifData(), MIFForm.class);
		if (mif != null) {
			if (mif.getPersonalInfo() != null) {
				scoreReportEntity.setUniqueCandidateId(mif.getPersonalInfo().getUniqueIdNumber() == null ? " " : mif
						.getPersonalInfo().getUniqueIdNumber());
				if (mif.getPersonalInfo().getFirstName() == null) {
					scoreReportEntity.setFirstName(acsCandidate.getFirstName() == null ? " " : acsCandidate
							.getFirstName());
				} else
					scoreReportEntity.setFirstName(mif.getPersonalInfo().getFirstName());
				if (mif.getPersonalInfo().getLastName() == null) {
					scoreReportEntity
							.setLastName(acsCandidate.getLastName() == null ? " " : acsCandidate.getLastName());
				} else
					scoreReportEntity.setLastName(mif.getPersonalInfo().getLastName());
			} else {
				scoreReportEntity.setUniqueCandidateId("");
				scoreReportEntity.setFirstName(acsCandidate.getFirstName() == null ? "" : acsCandidate.getFirstName());
				scoreReportEntity.setLastName(acsCandidate.getLastName() == null ? "" : acsCandidate.getLastName());
			}
			if (mif.getPermanentAddress() != null) {
				scoreReportEntity.setEmailId(mif.getPermanentAddress().getEmailId1() == null ? " " : mif
						.getPermanentAddress().getEmailId1());
				scoreReportEntity.setMobileNo(mif.getPermanentAddress().getMobile() == null ? " " : mif
						.getPermanentAddress().getMobile());
				scoreReportEntity.setTestCity(mif.getPermanentAddress().getTestCity() == null ? " " : mif
						.getPermanentAddress().getTestCity());
			} else {
				scoreReportEntity.setEmailId("");
				scoreReportEntity.setMobileNo("");
				scoreReportEntity.setTestCity("");
			}
			if (mif.getTrainingDetails() != null) {
				if (candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() == null)
					candidateRawScoreDetailsDO.setTrainingDetailsMaxCount(mif.getTrainingDetails().size());
				else if (candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() < mif.getTrainingDetails().size())
					candidateRawScoreDetailsDO.setTrainingDetailsMaxCount(mif.getTrainingDetails().size());
			}
			if (mif.getQualification() != null) {
				if (candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() == null) {
					if (mif.getQualification().getKnowForeignLanguage() != null
							&& mif.getQualification().getKnowForeignLanguage().equalsIgnoreCase("yes")
							&& mif.getQualification().getForeignLanguages() != null) {
						candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(mif.getQualification()
								.getForeignLanguages().size());
					} else {
						candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(0);
					}
				} else if (candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() != null
						&& mif.getQualification().getForeignLanguages() != null
						&& candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() < mif.getQualification()
								.getForeignLanguages().size()) {
					if (mif.getQualification().getKnowForeignLanguage() != null
							&& mif.getQualification().getKnowForeignLanguage().equalsIgnoreCase("yes")
							&& mif.getQualification().getForeignLanguages() != null)
						candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(mif.getQualification()
								.getForeignLanguages().size());
				}
			}
		}
	}

	private void fillLiteMIFDetails(CandidateRawScoreDetailsLiteDO candidateRawScoreDetailsDO,
			CandidateDetailsDO candidateDetailsDO,
			ScoreReportEntityLite scoreReportEntity) {
		MIFForm mif = new Gson().fromJson(candidateDetailsDO.getMifData(), MIFForm.class);
		if (mif != null) {
			if (mif.getPersonalInfo() != null) {
				scoreReportEntity.setUniqueCandidateId(mif.getPersonalInfo().getUniqueIdNumber() == null ? " "
						: mif.getPersonalInfo().getUniqueIdNumber());
				if (mif.getPersonalInfo().getFirstName() == null) {
					scoreReportEntity
							.setFirstName(candidateDetailsDO.getFirstName() == null ? " "
									: candidateDetailsDO.getFirstName());
				} else
					scoreReportEntity.setFirstName(mif.getPersonalInfo().getFirstName());
				if (mif.getPersonalInfo().getLastName() == null) {
					scoreReportEntity
							.setLastName(
									candidateDetailsDO.getLastName() == null ? " " : candidateDetailsDO.getLastName());
				} else
					scoreReportEntity.setLastName(mif.getPersonalInfo().getLastName());
			} else {
				scoreReportEntity.setUniqueCandidateId("");
				scoreReportEntity.setFirstName(
						candidateDetailsDO.getFirstName() == null ? "" : candidateDetailsDO.getFirstName());
				scoreReportEntity
						.setLastName(candidateDetailsDO.getLastName() == null ? "" : candidateDetailsDO.getLastName());
			}
			if (mif.getPermanentAddress() != null) {
				scoreReportEntity.setEmailId(mif.getPermanentAddress().getEmailId1() == null ? " "
						: mif.getPermanentAddress().getEmailId1());
				scoreReportEntity.setMobileNo(
						mif.getPermanentAddress().getMobile() == null ? " " : mif.getPermanentAddress().getMobile());
				scoreReportEntity.setTestCity(mif.getPermanentAddress().getTestCity() == null ? " "
						: mif.getPermanentAddress().getTestCity());
			} else {
				scoreReportEntity.setEmailId("");
				scoreReportEntity.setMobileNo("");
				scoreReportEntity.setTestCity("");
			}
		}
	}

	/**
	 * gets assessments from batch Code for ui
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */

	public List<AssessmentDetailsDO> getAssessmentsFromBatchCode(String batchCode) throws GenericDataModelException {
		return batchCandidateAssociationService.getAssessmentListByBatchCode(batchCode);
	}

	/**
	 * get set ids from batchCode and assessmentCode
	 * 
	 * @param batchCode
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 */

	public List<String> getSetIdsFromBatchCodeAssessmentCode(String batchCode, String assessmentCode)
			throws GenericDataModelException {
		return batchCandidateAssociationService.getSetIdByBatchCodeAssessmentCode(batchCode, assessmentCode);
	}

	/**
	 * get the list of batches for given date range
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsBatch> getListOfBatchesWithDateRange(String startDate, String endDate)
			throws GenericDataModelException {
		List<AcsBatch> batchDetails = null;
		if (startDate == null || startDate.isEmpty() || endDate == null || endDate.isEmpty()) {
			logger.error("missing mandatory parameters startDate: " + startDate + ", endDate: " + endDate
					+ ", returning batchDetails: " + batchDetails);
			return batchDetails;
		} else {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			Calendar rangeStartDate = Calendar.getInstance();
			Calendar rangeEndDate = Calendar.getInstance();

			try {
				rangeStartDate.setTime(dateFormat.parse(startDate));
				rangeEndDate.setTime(dateFormat.parse(endDate));
			} catch (ParseException e) {
				logger.error("error parsing date range..");
			}

			batchDetails = batchService.getListOfBatchesWithDateRange(rangeStartDate, rangeEndDate);
			return batchDetails;
		}

	}

	/**
	 * get list of batch candidate association from batch id
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateDetails(String batchCode)
			throws GenericDataModelException {
		List<AcsBatchCandidateAssociation> batchCandidateDetails =
				batchCandidateAssociationService.getBatchCandAssociationsByBatchCode(batchCode);
		return batchCandidateDetails;
	}

	/**
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getAssessmentNameFromAssessmentCode(String assessmentCode) throws GenericDataModelException {
		AcsAssessment assessment = cdeaService.getAssessmentDetailsByAssessmentCode(assessmentCode);
		return assessment.getAssessmentName();
	}

	public FileExportExportEntity generateRawScoreReportOnCandidateIds(Integer[] arrayOfBcaIds)
			throws GenericDataModelException {

		FileExportExportEntity exportEntity = null;
		CandidateRawScoreDetailsDO candidateRawScoreDetailsDO = new CandidateRawScoreDetailsDO();
		BatchExtensionInformationEntity extensionInformationEntity = null;
		List<Integer> bcaIds = new ArrayList<>();
		if (arrayOfBcaIds != null && arrayOfBcaIds.length != 0)
			bcaIds = Arrays.asList(arrayOfBcaIds);
		logger.debug("generateRawScoreReportOnCandidateIds initiated with BcaIds:{}", bcaIds);
		if (bcaIds.size() == 0 || bcaIds == null) {
			logger.error("CandidateIds cannot empty");
			return exportEntity;
		}
		Set<String> sectionIdentifierSet = new LinkedHashSet<>();
		List<String> sectionIdentifier = new ArrayList<>();
		if (bcaIds.size() > 0) {
			List<CandidateDetailsDO> candidateDetailsDOList =
					batchCandidateAssociationService.getBatchCandAssoDetailsByBCAID(bcaIds);

			// get BatchCode
			String batchCode = candidateDetailsDOList.get(0).getBatchCode();
			// get eventcode
			String eventCode = candidateDetailsDOList.get(0).getEventCode();
			candidateRawScoreDetailsDO.setEventCode(eventCode);
			// assessmentName
			String assessmentName = getAssessmentNameFromAssessmentCode(candidateDetailsDOList.get(0).getAssessmentCode());
			// report type
			candidateRawScoreDetailsDO.setReportType(extractACSEventReportType(eventCode));
			// fetch candidate identifiers
			Map<Integer, ResultProcReportTO> resultProcReportMap = extractResultProcTOs(candidateDetailsDOList, batchCode);

			Map<String, List<QPSectionsDO>> mapOfSectionLists = getMapOfSectionsForBatch(batchCode);

			for (CandidateDetailsDO candidateDetails : candidateDetailsDOList) {

				List<CandidateSectionScoreData> candidateSectionScoreDataList =
						new ArrayList<CandidateSectionScoreData>();
				int candidateId = candidateDetails.getCandidateId();

				List<QPSectionsDO> qpSections = mapOfSectionLists.get(candidateDetails.getQpaperCode());
				// adding section identifiers for all the candidates
				ResultProcReportTO procReportTO = resultProcReportMap.get(candidateId);
				int count = 0;
				if (!qpSections.isEmpty() && null != qpSections && procReportTO != null) {
					for (QPSectionsDO qpSectionsDO : qpSections) {
						count += qpSectionsDO.getQuestionsCount();
						getSectionLevelScores(sectionIdentifierSet, candidateSectionScoreDataList, procReportTO,
								qpSectionsDO, candidateDetails.getQpaperCode(), candidateDetails.getBcaid());
					}
				}

				int totalQuestions = count;

				ScoreReportEntity scoreReportEntity = new ScoreReportEntity();

				// add weightage details score
				Map<String, GroupSectionsWeightageScore> groupWeightageMap = getGroupWeightageScores(
						candidateSectionScoreDataList, getGroupWeightageMapFromBrRule(candidateDetails.getBatchCode(),
								candidateDetails.getAssessmentCode()));
				scoreReportEntity.setGroupWeightageMap(groupWeightageMap);
				if (acsPropertyUtil.getRemoteProctoringURL() != null) {
					scoreReportEntity.setCandidateRemoteProctoringId(acsPropertyUtil.getRemoteProctoringURL()
							+ candidateDetails.getCandidateRemoteProctoringId());
				}
				scoreReportEntity.setBatch(batchCode);
				if (candidateDetails.getMifData() != null && !(candidateDetails.getMifData().isEmpty())) {
					scoreReportEntity.setMIFAvailable(true);
					MIFForm mif = new Gson().fromJson(candidateDetails.getMifData(), MIFForm.class);
					if (mif != null) {
						if (mif.getPersonalInfo() != null) {
							scoreReportEntity.setUniqueCandidateId(mif.getPersonalInfo().getUniqueIdNumber() == null
									? " " : mif.getPersonalInfo().getUniqueIdNumber());
							if (mif.getPersonalInfo().getFirstName() == null) {
								scoreReportEntity.setFirstName(
										candidateDetails.getFirstName() == null ? " " : candidateDetails.getFirstName());
							} else
								scoreReportEntity.setFirstName(mif.getPersonalInfo().getFirstName());

							if (mif.getPersonalInfo().getMiddleName() == null) {
								scoreReportEntity.setMiddleName(" ");
							} else
								scoreReportEntity.setMiddleName(mif.getPersonalInfo().getMiddleName());

							if (mif.getPersonalInfo().getLastName() == null) {
								scoreReportEntity.setLastName(
										candidateDetails.getLastName() == null ? " " : candidateDetails.getLastName());
							} else
								scoreReportEntity.setLastName(mif.getPersonalInfo().getLastName());
						} else {
							scoreReportEntity.setUniqueCandidateId("");
							scoreReportEntity.setFirstName(candidateDetails.getFirstName() == null ? "" : candidateDetails
									.getFirstName());
							scoreReportEntity.setLastName(candidateDetails.getLastName() == null ? "" : candidateDetails
									.getLastName());
						}
						if (mif.getPermanentAddress() != null) {
							scoreReportEntity.setEmailId(mif.getPermanentAddress().getEmailId1() == null ? " "
									: mif.getPermanentAddress().getEmailId1());
							scoreReportEntity.setMobileNo(mif.getPermanentAddress().getMobile() == null ? " "
									: mif.getPermanentAddress().getMobile());
							scoreReportEntity.setTestCity(mif.getPermanentAddress().getTestCity() == null ? " "
									: mif.getPermanentAddress().getTestCity());
						} else {
							scoreReportEntity.setEmailId("");
							scoreReportEntity.setMobileNo("");
							scoreReportEntity.setTestCity("");
						}
						if (mif.getTrainingDetails() != null) {
							if (candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() == null)
								candidateRawScoreDetailsDO.setTrainingDetailsMaxCount(mif.getTrainingDetails().size());
							else if (candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() < mif.getTrainingDetails()
									.size())
								candidateRawScoreDetailsDO.setTrainingDetailsMaxCount(mif.getTrainingDetails().size());
						}
						if (mif.getQualification() != null) {
							if (candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() == null) {
								if (mif.getQualification().getKnowForeignLanguage() != null
										&& mif.getQualification().getKnowForeignLanguage().equalsIgnoreCase("yes")
										&& mif.getQualification().getForeignLanguages() != null) {
									candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(
											mif.getQualification().getForeignLanguages().size());
								} else {
									candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(0);
								}
							} else if (candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() != null
									&& mif.getQualification().getForeignLanguages() != null
									&& candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() < mif.getQualification()
											.getForeignLanguages().size()) {
								if (mif.getQualification().getKnowForeignLanguage() != null
										&& mif.getQualification().getKnowForeignLanguage().equalsIgnoreCase("yes")
										&& mif.getQualification().getForeignLanguages() != null)
									candidateRawScoreDetailsDO.setForeignLanguagesMaxCount(
											mif.getQualification().getForeignLanguages().size());
							}
						}
					}
				} else {
					scoreReportEntity.setUniqueCandidateId("");
					scoreReportEntity
							.setFirstName(candidateDetails.getFirstName() == null ? "" : candidateDetails.getFirstName());
					scoreReportEntity.setLastName(candidateDetails.getLastName() == null ? "" : candidateDetails
							.getLastName());
					scoreReportEntity.setEmailId(candidateDetails.getEmailId1() == null ? "" : candidateDetails
							.getEmailId1());
					scoreReportEntity.setMobileNo(
							candidateDetails.getPhoneNumber1() == null ? "" : candidateDetails
									.getPhoneNumber1());
					if (candidateDetails.getCandidateType() != CandidateType.WALKIN.ordinal())
						scoreReportEntity.setGender(candidateDetails.getGender() == null ? "" : candidateDetails
								.getGender());
					scoreReportEntity.setTestCity("");
					scoreReportEntity.setMIFAvailable(false);
				}
				scoreReportEntity.setAssessmentCode(candidateDetails.getAssessmentCode());
				scoreReportEntity.setAssessmentName(assessmentName);
				scoreReportEntity.setDbCandidateId(String.valueOf(candidateDetails.getCandidateId()));
				scoreReportEntity.setCandidateId(candidateDetails.getIdentifier1());
				scoreReportEntity.setLoginId(candidateDetails.getCandidateLogin());
				scoreReportEntity.setSetCode(candidateDetails.getSetID());

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				scoreReportEntity.setMifJson(candidateDetails.getMifData());

				if (candidateDetails.getActualTestStartedTime() != null) {
					Date testStartDate = candidateDetails.getActualTestStartedTime().getTime();
					scoreReportEntity.setTestStartDate(dateFormat.format(testStartDate));
				} else {
					scoreReportEntity.setTestStartDate(dateFormat.format(new Date()));
				}
				if (candidateDetails.getActualTestEndTime() != null) {
					Date testEndDate = candidateDetails.getActualTestEndTime().getTime();
					scoreReportEntity.setTestEndDate(dateFormat.format(testEndDate));
				} else {

					scoreReportEntity.setTestEndDate(dateFormat.format(new Date()));
				}
				// time taken
				Long time = candidateDetails.getTimeSpentInSecs();
				if (time == null)
					time = candidateService.getCandidateSpentTime(candidateDetails);
				int[] convertedTime = TimeUtil.convertSecsTimeToMinsAndSecs(time);
				StringBuilder line = new StringBuilder();
				if (convertedTime[0] > 0)
					line.append(convertedTime[0] + " Hours ");
				if (convertedTime[1] > 0)
					line.append(convertedTime[1] + " Minutes ");
				if (convertedTime[2] > 0)
					line.append(convertedTime[2] + " Seconds ");

				if (line.toString().isEmpty())
					scoreReportEntity.setCandidateTimeTaken(ACSConstants.NA);

				scoreReportEntity.setCandidateTimeTaken(line.toString());
				scoreReportEntity.setNumbOfTimesCrashed(String.valueOf(candidateDetails.getClearLoginCount()));
				scoreReportEntity.setTotalQuestions(totalQuestions);

				scoreReportEntity.setCandidateSectionScoreDataList(candidateSectionScoreDataList);
				if (procReportTO != null) {
					scoreReportEntity.setTotalScore(df.format(procReportTO.getTotalMarks()));
					scoreReportEntity.setScorePercentage(df.format(procReportTO.getPercntgeMarks()));
					scoreReportEntity.setAttemptedQuestionsCount(
							procReportTO.getAttemptedCount() == null ? 0 : procReportTO.getAttemptedCount());
					scoreReportEntity.setCorrectQuestionsCount(
							procReportTO.getCorrectCount() == null ? 0 : procReportTO.getCorrectCount());
				} else {
					scoreReportEntity.setTotalScore("0.00");
					scoreReportEntity.setScorePercentage("0.00");
					scoreReportEntity.setAttemptedQuestionsCount(0);
					scoreReportEntity.setCorrectQuestionsCount(0);
				}
				AcsConfig assessmentServerCode = cdeaService.getACSConfigByBatchCode(batchCode);
				if (assessmentServerCode != null) {
					scoreReportEntity.setTestCenter(
							assessmentServerCode.getVenueName() == null ? " " : assessmentServerCode.getVenueName());
					scoreReportEntity.setAcsServerName(assessmentServerCode.getAssessmentServerName() == null ? " "
							: assessmentServerCode.getAssessmentServerName());
				}

				if (candidateRawScoreDetailsDO.getScoreReportEntities() == null) {
					List<ScoreReportEntity> scoreReportEntities = new ArrayList<ScoreReportEntity>();
					candidateRawScoreDetailsDO.setScoreReportEntities(scoreReportEntities);
				}
				candidateRawScoreDetailsDO.getScoreReportEntities().add(scoreReportEntity);
				extensionInformationEntity =
						cdeaService.getCustomerDivisionEventCodesByBatchCode(candidateDetails
								.getBatchCode());

			}
			sectionIdentifier = new ArrayList<String>(sectionIdentifierSet);

			int maxSectionsCount = sectionIdentifier.size();
			if (null != candidateRawScoreDetailsDO && candidateRawScoreDetailsDO.getScoreReportEntities() != null) {
				Iterator<ScoreReportEntity> scoreIterator =
						candidateRawScoreDetailsDO.getScoreReportEntities().iterator();
				List<CandidateSectionScoreData> cList = null;
				while (scoreIterator.hasNext()) {
					ScoreReportEntity sReportEntity = scoreIterator.next();
					cList = sReportEntity.getCandidateSectionScoreDataList();
					if (maxSectionsCount == cList.size()) {
						break;
					}
				}
			}
			candidateRawScoreDetailsDO.setMaxSectionsCount(maxSectionsCount);
			candidateRawScoreDetailsDO.setSectionIdentifier(sectionIdentifier);
		}
		File path =
				new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR + File.separator
						+ ACSConstants.ACS_DOWNLOAD_DIR + File.separator + ACSConstants.ACS_RAW_SCORE_DIR);
		if (!path.exists()) {
			path.mkdirs();
		}
		String customerCode = "";
		try {
			if (candidateRawScoreDetailsDO != null) {
				if (extensionInformationEntity != null && (candidateRawScoreDetailsDO.getScoreReportEntities() != null)
						&& (candidateRawScoreDetailsDO.getMaxSectionsCount() != 0)
						&& (candidateRawScoreDetailsDO.getSectionIdentifier() != null)) {
					CDEDetailsDO cdeDetailsDO =
							cdeaService.getCustomerDivisionEventCodesByEventCode(extensionInformationEntity
									.getEventCode());
					customerCode = cdeDetailsDO.getCustomerCode();

					String eventCode = cdeDetailsDO.getEventCode();
					boolean rule = getRemoteProctoringEventRules(eventCode);

					Set<String> brRulesJsonSet =
							getBrRulesForAllAssessments(candidateRawScoreDetailsDO.getScoreReportEntities());
					logger.debug("calling Raw score report service initiated with candidate details:{}",
							candidateRawScoreDetailsDO.getScoreReportEntities());
					exportEntity =
							exportToExcelUtility.exportScoreReportGridDataForAcsMif(candidateRawScoreDetailsDO
									.getScoreReportEntities(), cdeDetailsDO.getCustomerCode(), cdeDetailsDO
											.getDivisionCode(),
									eventCode, cdeDetailsDO.getCustomerName(),
									cdeDetailsDO.getDivisionName(), cdeDetailsDO.getEventName(),
									candidateRawScoreDetailsDO.getMaxSectionsCount(), candidateRawScoreDetailsDO
											.getSectionIdentifier(),
									new File(path + File.separator
											+ ACSConstants.SCORE_REPORT_SHEET_NAME + System.currentTimeMillis()
											+ ExcelConstants.XLSX),
									candidateRawScoreDetailsDO.getTrainingDetailsMaxCount() == null ? 0
											: candidateRawScoreDetailsDO.getTrainingDetailsMaxCount(),
									candidateRawScoreDetailsDO.getForeignLanguagesMaxCount() == null ? 0
											: candidateRawScoreDetailsDO.getForeignLanguagesMaxCount(),
									brRulesJsonSet, rule);
				} else {
					exportEntity =
							new FileExportExportEntity(ExcelConstants.FAILED_STATUS, "",
									"Problem in Generating Candidate Raw  Score Report due to some null values");
					logger.debug("Problem in Generating Candidate Raw  Score Report due to some null values" + bcaIds);
				}
			}

			if (exportEntity != null && exportEntity.getStatus().equalsIgnoreCase(ExcelConstants.SUCCESS_STATUS)) {
				if (exportEntity.getFileLocation() != null && new File(exportEntity.getFileLocation()).exists()) {
					if (isCustomerSpecificReportRequired(exportEntity, customerCode)) {
						logger.debug(exportEntity + " Customer Specific Template Report is required");
						getCustomerSpecificTemplateReport(exportEntity, customerCode);
					} else {
						logger.debug(exportEntity + " Customer Specific Template Report not required");
						return exportEntity;
					}
					return exportEntity;
				}
			} else {
				logger.debug("Problem in Generating Candidate Raw  Score Report");
			}
		} catch (GenericDataModelException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | IntrospectionException | ReportOperationException e) {
			logger.error("ReportOperationException while executing generateRawScoreReportOnCandidateIds...", e);
			exportEntity.setErrorMsg(e.getLocalizedMessage());
		}
		return exportEntity;
	}

	private boolean getRemoteProctoringEventRules(String eventCode) throws GenericDataModelException {
		AcsProperties acsProperties = acsEventRequestsService
				.getAcsPropertiesOnPropNameAndEventCode(EventRule.REMOTE_PROCTORING_ENABLED.toString(), eventCode);
		if (acsProperties != null && acsProperties.getPropertyValue() != null)
			return Boolean.parseBoolean(acsProperties.getPropertyValue());
		else {
			logger.debug(" ACS properties EVENT RULES is NULL for eventcode:{} , hence returninf FALSE", eventCode);
			return false;
		}
	}

	boolean isCustomerSpecificReportRequired(FileExportExportEntity exportEntity, String customerCode) {
		// check whether Customer folder present in content folder
		logger.debug("check whether Customer folder present in content folder :{}", exportEntity + "customer code: "
				+ customerCode);
		File path = new File(getScoreTemplateDirectory());
		if (!path.exists() && customerCode.isEmpty()) {
			// path does not exist so returning raw score report
			logger.debug(path + "  does not exist so returning raw score report for customercode: " + customerCode
					+ " hence isCustomerSpecificReportRequired = FALSE");
			exportEntity
					.setErrorMsg(
							"CustomerSpecificReport path does not exist in Apollo home  so returning raw score report for the customercode");
			return false;
		} else {
			// get customer code folder
			String customerSpecificFolder = path.getAbsolutePath() + File.separator + customerCode;
			if (new File(customerSpecificFolder).exists()) {
				logger.debug(customerSpecificFolder
						+ " = customerSpecificFolder exists hence isCustomerSpecificReportRequired = TRUE");
				if (new File(customerSpecificFolder).isDirectory()) {
					logger.debug(customerSpecificFolder
							+ " = customerSpecificFolder is a folder hence isCustomerSpecificReportRequired = TRUE");
					return true;
				} else {
					logger.debug(customerSpecificFolder
							+ " = customerSpecificFolder is not a folder hence isCustomerSpecificReportRequired = FALSE");
					exportEntity
							.setErrorMsg(
									"Customer Specific Folder is not a folder so returning raw score report for the customercode");

					return false;
				}
			} else {
				logger.debug(customerSpecificFolder
						+ " = customerSpecificFolder doesn't exists hence isCustomerSpecificReportRequired = FALSE");
				exportEntity.setErrorMsg("Customer Specific Folder doesn't exists  for the customercode");
				return false;
			}
		}

	}

	/**
	 * create Customer Specific Template Report
	 * 
	 * @throws ReportOperationException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public FileExportExportEntity getCustomerSpecificTemplateReport(FileExportExportEntity exportEntity,
			String customerCode) throws ReportOperationException {
		// check whether Customer folder present in content folder
		logger.debug("check whether Customer folder present in content folder :{}", exportEntity);
		String rawScoreReportFile = exportEntity.getFileLocation();
		File path = new File(getScoreTemplateDirectory());
		String generatedFile = "";
		// get customer code folder
		String customerSpecificFolder = path.getAbsolutePath() + File.separator + customerCode;
		String generationPath = new File(rawScoreReportFile).getParent();
		// customer Specific Template to generate report
		String customerSpecificTemplate =
				customerSpecificFolder + File.separator + ACSConstants.ACS_SCORE_TEMPLATE_EXCEL_SHEET;
		if (new File(customerSpecificTemplate).exists()) {
			ReportService reportServiceImpl = new ReportServiceImpl();
			generatedFile =
					reportServiceImpl.generateReportByCustomerTemplate(rawScoreReportFile, generationPath,
							customerSpecificTemplate);
			logger.debug(generatedFile + " = generated customer specific report");
			if (new File(generatedFile).exists()) {
				exportEntity.setFileLocation(generatedFile);
				exportEntity.setErrorMsg(null);
				exportEntity.setStatus(ExcelConstants.SUCCESS_STATUS);
				return exportEntity;
			} else {
				logger.debug(generatedFile
						+ " = generated customer specific report doesn't exists hence returning raw score report");
				exportEntity
						.setErrorMsg(
								"Generated customer specific report doesn't exists hence returning raw score report");
				return exportEntity;
			}
		} else {
			logger.debug(customerSpecificTemplate
					+ " = customer specific Template doesn't exists hence returning raw score report");
			exportEntity
					.setErrorMsg(
							"customer specific Template doesn't exists in Apollo-Home hence returning raw score report");
			return exportEntity;
		}
	}

	private String getScoreTemplateDirectory() {
		return acsCommonUtility.getAcsConfigDirectory() + File.separator + ACSConstants.ACS_SCORE_TEMPLATE_DIR;
	}
}
