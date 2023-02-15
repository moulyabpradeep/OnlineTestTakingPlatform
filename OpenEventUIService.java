package com.merittrac.apollo.rps.services;



import java.beans.IntrospectionException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.JAXBException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.codehaus.plexus.util.Base64;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.acsrps.candidatescore.entities.AptitudeTestScore;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateEducationDetails;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateQuestionAndAnswer;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateScoreCard;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateSectionScoreDetails;
import com.merittrac.apollo.acsrps.candidatescore.entities.ReportSectionWeightageCutOffEntity;
import com.merittrac.apollo.acsrps.candidatescore.services.CandidateScoreCardGenerator;
import com.merittrac.apollo.common.BehavioralTestType;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.GradingSchemeDo;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.common.MessagesReader;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ResultProcessingRuleDo;
import com.merittrac.apollo.common.StdDevUtil;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.calendarUtil.CalendarUtil;
import com.merittrac.apollo.common.calendarUtil.TimeUtil;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.entities.acs.ResponseMarkBean;
import com.merittrac.apollo.common.excelService.ExcelModel;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralCandidateAlignmentEntity;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralGragingSchemaEntity;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralProficiencyLevelEntity;
import com.merittrac.apollo.common.resultcomputation.entity.BehaviouralTestScoresEntity;
import com.merittrac.apollo.data.bean.CandidateDetails;
import com.merittrac.apollo.data.bean.CandidateDetailsForResponseReport;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBehaviouralTest;
import com.merittrac.apollo.data.entity.RpsBehaviouralTestCharacteristic;
import com.merittrac.apollo.data.entity.RpsBrLr;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateMIFDetails;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsCandidateResponseLite;
import com.merittrac.apollo.data.entity.RpsCandidateResponseStatusForThirdParty;
import com.merittrac.apollo.data.entity.RpsCandidateResponses;
import com.merittrac.apollo.data.entity.RpsCustomer;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsMasterAssociation;
import com.merittrac.apollo.data.entity.RpsMasterAssociationLite;
import com.merittrac.apollo.data.entity.RpsQpSection;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociation;
import com.merittrac.apollo.data.entity.RpsQuestionAssociationLite;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsSectionCandidateResponse;
import com.merittrac.apollo.data.entity.RpsSectionCandidateResponseLite;
import com.merittrac.apollo.data.entity.RpsWetScoreEvaluation;
import com.merittrac.apollo.data.repository.RpsAssessmentRepository;
import com.merittrac.apollo.data.repository.RpsBehaviouralTestRepository;
import com.merittrac.apollo.data.repository.RpsCandidateResponseRepository;
import com.merittrac.apollo.data.repository.RpsCandidateResponseStatusForThirdPartyRepository;
import com.merittrac.apollo.data.repository.RpsCandidateResponsesRepository;
import com.merittrac.apollo.data.repository.RpsQpSectionRepository;
import com.merittrac.apollo.data.repository.RpsQuestionAssociationRepository;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBehaviouralTestCharacteristicService;
import com.merittrac.apollo.data.service.RpsCandidateMIFDetailsService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsCandidateResponseStatusForThirdPartyService;
import com.merittrac.apollo.data.service.RpsCandidateService;
import com.merittrac.apollo.data.service.RpsDivisionService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsQpSectionService;
import com.merittrac.apollo.data.service.RpsQpTemplateService;
import com.merittrac.apollo.data.service.RpsQuestionAssociationService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsQuestionService;
import com.merittrac.apollo.data.service.RpsSectionCandidateResponseService;
import com.merittrac.apollo.data.service.RpsWetDataEvaluationService;
import com.merittrac.apollo.excel.ExcelConstants;
import com.merittrac.apollo.excel.TypingTestEntity;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.DefaultAnswerOptionEnum;
import com.merittrac.apollo.qpd.qpgenentities.GroupWeightageCutOffEntity;
import com.merittrac.apollo.qpd.qpgenentities.LayoutRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.SectionWeightageCutOffEntity;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.MarksAndScoreRules;
import com.merittrac.apollo.qpd.qpgenentities.sectionrules.SectionLevelRules;
import com.merittrac.apollo.reports.CandidateQPDetailsPDFGenerator;
import com.merittrac.apollo.reports.CandidateTypingTestDetailsPDFGenerator;
import com.merittrac.apollo.reports.ProvisionalCertificateEntity;
import com.merittrac.apollo.reports.behaviouralReports.BehaviouralExcelReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.BehaviouralParamtersEntity;
import com.merittrac.apollo.reports.behaviouralReports.BehaviouralReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.BlueCollaredJobsPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.CGTpdfgenerator;
import com.merittrac.apollo.reports.behaviouralReports.CPABasicAndAdvancedPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.CSOQPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.CSPBehaviouralReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.CSPPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.Dimension8BehaviouralReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.Dimension8PDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.GPQPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.IDSAPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.IDSBOBPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.IDSPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.LSQPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.SSPQGenericBehaviouralReportEntity;
import com.merittrac.apollo.reports.behaviouralReports.SSPQGenericPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.SSPQNormalPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.SSPQPharmaPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.WTLPDFGenerator;
import com.merittrac.apollo.reports.behaviouralReports.WTSPDFGenerator;
import com.merittrac.apollo.rps.common.ABCDXOptionsEnum;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.qpd.dataobject.FileDownloadEntityDO;
import com.merittrac.apollo.rps.reportexport.service.BehaviouralReport;
import com.merittrac.apollo.rps.reportexport.service.CandidateScheduleReport;
import com.merittrac.apollo.rps.reportexport.service.PDFReportService;
import com.merittrac.apollo.rps.reportexport.service.TopicWiseExcelReport;
import com.merittrac.apollo.rps.reports.MigrationStatusService;
import com.merittrac.apollo.rps.rest.service.CandidateResultAcknowledgementEntities;
import com.merittrac.apollo.rps.rest.service.CandidateResultAcknowledgementEntity;
import com.merittrac.apollo.rps.rest.service.CandidateResultAcknowledgementStatus;
import com.merittrac.apollo.rps.rest.service.CandidateResultScoreEntity;
import com.merittrac.apollo.rps.rest.service.CandidateResultWrapScoresEntity;
import com.merittrac.apollo.rps.rest.service.CandidateSectionScoresEntity;
import com.merittrac.apollo.rps.rest.service.entity.CandidateBehaviouralScoresEntity;
import com.merittrac.apollo.rps.rest.service.entity.CandidateBehaviouralSectionScoresEntity;
import com.merittrac.apollo.rps.resultprocessingrule.entity.GroupSectionsWeightageScore;
import com.merittrac.apollo.rps.resultprocessingrule.entity.SectionCutoffWithStatusData;
import com.merittrac.apollo.rps.ui.openevent.entity.AssessmentSetsForEventEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.BehaviouralReportPageEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.BehaviouralReportWrapper;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateReportViewPageEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateReportViewPageEntityWrapper;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateScoreDetailsForGraph;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateScoreEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateScoreInfoEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.CandidateSectionScoreData;
import com.merittrac.apollo.rps.ui.openevent.entity.FileExportExportEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.ResponseMatrixEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.ResponseReportWrapper;
import com.merittrac.apollo.rps.ui.openevent.entity.RpsAcsBatchEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.ScoreReportEntity;
import com.merittrac.apollo.rps.ui.openevent.entity.ScoreReportWrapper;
import com.merittrac.apollo.rps.ui.openevent.entity.SectionLevelInfoEntity;
import com.merittrac.apollo.rps.ui.openevent.utilities.ExportToExcelUtility;
import com.merittrac.apollo.rps.ui.openevent.utilities.ResponseReportUtility;
import com.merittrac.apollo.rps.ui.openevent.utilities.ScoreReportUtility;
import com.merittrac.apollo.rps.ui.openevent.utilities.SharedMethodsUtility;
import com.merittrac.apollo.tp.mif.ContactInfo;
import com.merittrac.apollo.tp.mif.Experience;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;
import com.merittrac.apollo.tp.mif.Qualification;
import com.merittrac.apollo.tp.mif.QualificationDetails;
import com.merittrac.apollo.tp.mif.TrainingDetails;


/**
 * The Class OpenEventUIService.
 *
 * @author Moulya_P
 */
public class OpenEventUIService {
	/**
	 * The gson.
	 */
	@Autowired
	Gson gson;
	/**
	 * The gson.
	 */
	@Autowired
	ExcelModel excelModel;
	@Autowired
	PDFReportService pdfReportService;
	/**
	 * The rps batch acs association service.
	 */
	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	/**
	 * The rps master association service.
	 */
	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsCandidateResponseStatusForThirdPartyService rpsCandidateResponseStatusForThirdPartyService;

	@Autowired
	RpsCandidateResponseStatusForThirdPartyRepository rpsCandidateResponseStatusForThirdPartyRepository;
	/**
	 * The rps candidate response service.
	 */
	@Autowired
	RpsCandidateResponseService rpsCandidateResponseService;

	@Autowired
	RpsCandidateMIFDetailsService rpsCandidateMIFDetailsService;

	/**
	 * The rps question paper service.
	 */
	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;

	@Autowired
	RpsAssessmentRepository rpsAssessmentRepository;

	/**
	 * The rps candidate service.
	 */
	@Autowired
	RpsCandidateService rpsCandidateService;

	/**
	 * The rps section candidate response service.
	 */
	@Autowired
	RpsSectionCandidateResponseService rpsSectionCandidateResponseService;

	/**
	 * The rps qp section service.
	 */
	@Autowired
	RpsQpSectionService rpsQpSectionService;

	/**
	 * The score report utility.
	 */
	@Autowired
	ScoreReportUtility scoreReportUtility;

	/**
	 * The response report utility.
	 */
	@Autowired
	ResponseReportUtility responseReportUtility;

	/**
	 * The shared methods utility.
	 */
	@Autowired
	SharedMethodsUtility sharedMethodsUtility;

	/**
	 * The export to excel utility.
	 */
	@Autowired
	ExportToExcelUtility exportToExcelUtility;

	/**
	 * The rps event service.
	 */
	@Autowired
	RpsEventService rpsEventService;

	/**
	 * The rps assessment service.
	 */
	@Autowired
	RpsAssessmentService rpsAssessmentService;

	@Autowired
	RpsDivisionService rpsDivisionService;

	@Autowired
	RpsQuestionService rpsQuestionService;

	@Autowired
	RpsQpTemplateService rpsQpTemplateService;

	@Autowired
	RpsBehaviouralTestRepository rpsBehaviouralTestRepository;

	@Autowired
	RpsQuestionAssociationRepository rpsQuestionAssociationRepository;

	@Autowired
	RpsCandidateResponseRepository rpsCandidateResponseRepository;

	@Autowired
	RpsCandidateResponsesRepository rpsCandidateResponsesRepository;

	@Autowired
	RpsWetDataEvaluationService rpsWetDataEvaluationService;

	@Autowired
	RpsQuestionAssociationService rpsQuestionAssociationService;

	@Autowired
	BehaviouralReport behaviouralReport;

	@Autowired
	TopicWiseExcelReport topicWiseExcelReport;

	@Autowired
	MigrationStatusService migrationStatusService;

	@Autowired
	CandidateScheduleReport candidateScheduleReport;

	@Autowired
	RpsQpSectionRepository rpsQpSectionRepository;

	/**
	 * The user home.
	 */

	@Value("${apollo_home_dir}")
	private String userHome;

	@Value("${exportDocxURI}")
	private String exportDocxURI;

	@Value("${exportDocxMethodName}")
	private String exportDocxMethodName;

	@Value("${windowsExportQPPath}")
	private String windowsExportQPPath;

	@Value("${deleteHtmlFiles}")
	private String deleteHtmlFiles;

	@Value("${VerbalSectionName}")
	private String verbalSectionName;

	@Value("${LogicalSectionName}")
	private String logicalSectionName;

	@Value("${TechnicalSectionName}")
	private String technicalSectionName;

	@Value("${QuantitativeSectionName}")
	private String quantitativeSectionName;

	@Value("${EventCodeForCronJob}")
	private String eventCode;

	@Value("${isBehaviouralSectionScoresRequired}")
	private String isBehaviouralSectionScoresRequired;

	@Value("${urlToPostCandidateResultScores}")
	private String urlToPostCandidateResultScores;

	@Value("${methodUrlToPostCandidateResultScores}")
	private String methodUrlToPostCandidateResultScores;

	@Value("${countOfFailureAttemptsForTestScoresDelivery}")
	private String countOfFailureAttemptsForTestScoresDelivery;

	@Value("${SourceNameForWipro}")
	private String sourceNameForWipro;

	@Value("${UserNameForWipro}")
	private String userNameForWipro;

	@Value("${PasswordForWipro}")
	private String passwordForWipro;

	@Value("${proxyHost}")
	private String proxyHost;

	@Value("${proxyPort}")
	private Integer proxyPort;

	@Value("${DomainNameForWiproWithCombinedAnalyticalScores}")
	private String domainNameForWiproWithCombinedAnalyticalScores;

	@Autowired
	RpsBehaviouralTestCharacteristicService rpsBehaviouralTestCharacteristicService;

	@Value("${is_markedForReviewQuestions_Evaluated}")
	private boolean isMarkedForReviewQuestionsToBeEvaluated;


	@Value("${is_enabled_best_scores_rule}")
	private boolean isEnabledBestScoresRule;

	@Value("#{'${assessment_list_best_score}'.split(',')}")
	private List<String> listOfAssessmentsForBestScoreRule;

	@Value("${section_partA}")
	private String sectionPartAIdentification;

	@Value("${section_partB}")
	private String sectionPartBIdentification;

	@Value("${best_scores_section_partA}")
	private Integer sectionCountForBestScoresPartA;

	@Value("${best_scores_section_partB}")
	private Integer sectionCountForBestScoresPartB;

	@Value("${sort_subjects_on_tie_breaking}")
	private String sortSubjectsOnTieBreaking;


	// for Candidate Response Download Report Config
	@Value("${score_obtained}")
	private boolean scoreObtained;

	@Value("${correct_answer}")
	private boolean correctAnswer;

	@Value("${candidate_answer}")
	private boolean candidateAnswer;

	@Value("${htmltopdftool}")
	private String htmltopdftool;


	@Autowired
	public WTSService wtsService;

	private Logger logger = LoggerFactory.getLogger(OpenEventUIService.class);
	private Map<String, List<RpsQpSection>> rQpSectionMap = new HashMap<>();
	private static Map<String, String> customerLogoPathMapOnAssCode = new HashMap<>();
	private ResultProcessingRuleDo resultProcessingRuleDo = null;
	private List<GradingSchemeDo> gradingSchemeDos = null;
	private boolean gradingEnabled = false;
	private Map<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntityMap = null;
	private Map<String, GroupWeightageCutOffEntity> groupWeightageCutOffEntityMap = null;
	private boolean sectionGroupEnabled = false;
	private DecimalFormat decimalFormat = new DecimalFormat(RpsConstants.DECIMAL_FORMAT);
	private static final String MAX_SECTION_COUNT = "maxSectionCount";
	private static final String WET_QUESTION_COUNT = "wetQuestionCount";
	private static MessagesReader messagesReader = new MessagesReader();
	// created to handle parallel login requests for same candidate
	private static Map<String, SecretKeySpec> eventCodeToKey = new HashMap<>();

	/**
	 * Gets the assessment sets for event.
	 *
	 * @param eventCode
	 *            the event code
	 * @return the assessment sets
	 */
	public String getAssessmentSetsForEvent(String eventCode) {
		logger.debug("---IN--getAssessmentSetsForEvent()-----");
		String assessmentSetsJson = RpsConstants.EMPTY_JSON;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode");
			return assessmentSetsJson;
		}

		Map<String, AssessmentSetsForEventEntity> assessmentSetsForEventEntityMap = new HashMap<>();
		List<RpsAssessment> rpsAssessmentsList = rpsAssessmentService.getAssessmentsForEvent(eventCode);
		if (rpsAssessmentsList == null || rpsAssessmentsList.isEmpty()) {
			logger.error("No assessment have taken for selected event");
			return assessmentSetsJson;
		}

		for (RpsAssessment rpsAssessment : rpsAssessmentsList) {
			AssessmentSetsForEventEntity assessmentSetsForEventEntity = new AssessmentSetsForEventEntity();
			final List<RpsQuestionPaper> rpsQuestionPaperList = rpsQuestionPaperService
					.findByAssessmentCode(rpsAssessment.getAssessmentCode());
			final List<Object[]> masterAssociations = rpsMasterAssociationService
					.findUniqueBatchAcsByAssessmentCodeWithJoinFetchAssocition(rpsAssessment.getAssessmentCode());
			Map<Integer, String> setsForAssessmentMap = null;
			if (rpsQuestionPaperList != null) {
				setsForAssessmentMap = new HashMap<Integer, String>();
				for (RpsQuestionPaper rpsQuestionPaper : rpsQuestionPaperList)
					setsForAssessmentMap.put(rpsQuestionPaper.getQpId(), rpsQuestionPaper.getSetCode());

			}
			Map<Integer, RpsAcsBatchEntity> acsBatchCodes = null;
			if (masterAssociations != null) {
				acsBatchCodes = new HashMap<>();
				for (Object[] rpsMasterAssociation : masterAssociations) {
					RpsAcsBatchEntity acsBatchEntity = new RpsAcsBatchEntity();
					acsBatchEntity.setRpsAcsServerName(String.valueOf(rpsMasterAssociation[0]));
					acsBatchEntity.setRpsAcsServerId(String.valueOf(rpsMasterAssociation[1]));
					acsBatchEntity.setAcsId(Integer.parseInt(String.valueOf(rpsMasterAssociation[2])));
					acsBatchEntity.setRpsBatchName(String.valueOf(rpsMasterAssociation[3]));
					acsBatchEntity.setRpsBatchId(String.valueOf(rpsMasterAssociation[4]));
					acsBatchEntity.setBid(Integer.parseInt(String.valueOf(rpsMasterAssociation[5])));
					acsBatchCodes.put(Integer.parseInt(String.valueOf(rpsMasterAssociation[6])), acsBatchEntity);
				}
			}
			assessmentSetsForEventEntity.setAssessmentName(rpsAssessment.getAssessmentName());
			assessmentSetsForEventEntity.setSetCodesMap(setsForAssessmentMap);
			assessmentSetsForEventEntity.setAcsBatchCodes(acsBatchCodes);
			assessmentSetsForEventEntityMap.put(rpsAssessment.getAssessmentCode(), assessmentSetsForEventEntity);
		}

		assessmentSetsJson = gson.toJson(assessmentSetsForEventEntityMap);
		logger.debug("---OUT--getAssessmentSetsForEvent()-----");

		return assessmentSetsJson;

	}

	/**
	 * Gets the score report.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the score report
	 */
	@Transactional
	public String getScoreReport(String eventCode, String startDate, String endDate, String assessmentCode,
			Integer qpId, Integer acsId, Integer bId, int start, int end, String orderType, String column) {
		logger.debug(
				"---IN--getScoreReport()----- with param eventCode: {}, startDate: {}, endDate: {}, assessmentCode: {}, qpId: {}",
				eventCode, startDate, endDate, assessmentCode, qpId);
		String scoreReportJson = RpsConstants.EMPTY_JSON;
		boolean isDefaultGroup = false;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			return scoreReportJson;
		}

		// for customer logo
		RpsEvent rpsEvent = rpsEventService.getRpsEventPerEventCode(eventCode);
		String divisionCode = rpsEvent.getRpsDivision().getDivisionCode();
		RpsCustomer rpsCustomer = rpsDivisionService.findRpsCustomerByDivCode(divisionCode);
		String customerCode = rpsCustomer.getCustomerCode();
		Map<String, List<String>> qtypeSectionMap = null;
		try {
			RpsQuestionPaper rpPaper = null;
			if (qpId != null && qpId != 0)
				rpPaper = rpsQuestionPaperService.findOne(qpId);
			else
				rpPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);

			// new implementation
			qtypeSectionMap = rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());

			if (qpId == null || qpId == 0)
				resultProcessingRuleDo = scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(null,
						assessmentCode);
			else
				resultProcessingRuleDo = scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper,
						assessmentCode);

			gradingEnabled = false;
			sectionGroupEnabled = false;
			gradingSchemeDos = null;
			sectionWeightageCutOffEntityMap = null;
			if (resultProcessingRuleDo != null) {
				gradingSchemeDos = resultProcessingRuleDo.getGradingSchemeDos();
				sectionWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupSectionMap();
				groupWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupDetailsMap();
			}

			if (gradingSchemeDos != null && !gradingSchemeDos.isEmpty())
				gradingEnabled = true;
			if (sectionWeightageCutOffEntityMap != null && !sectionWeightageCutOffEntityMap.isEmpty())
				sectionGroupEnabled = true;

			if (sectionWeightageCutOffEntityMap != null && sectionWeightageCutOffEntityMap.size() == 1) {
				if (sectionWeightageCutOffEntityMap.containsKey("DEFAULTGROUP"))
					isDefaultGroup = true;
			}

			RpsBrLr rpsBrLr = rpsQpTemplateService.getRpsBrLrByAssessment(assessmentCode);
			String lrRules = rpsBrLr.getLrRules();
			LayoutRulesExportEntity layoutRulesExportEntity = gson.fromJson(lrRules, LayoutRulesExportEntity.class);
			String customerLogoFullPath = layoutRulesExportEntity.getCustomerLogo();
			logger.debug("in getScoreReport customerLogoFullPath=" + customerLogoFullPath);
			Map<String, String> customerLogMap = new HashMap<>();
			Type type = new TypeToken<HashMap<String, String>>() {
			}.getType();
			customerLogMap = new Gson().fromJson(customerLogoFullPath, type);
			logger.debug("in getScoreReport customerLogMap=" + customerLogMap);
			String customerLogMapValue = customerLogMap.get("customerLogoLocation");
			String customerLogoImage = FilenameUtils.getName(customerLogMapValue);
			String customerLogoPath = FilenameUtils.separatorsToSystem(userHome + File.separator + customerCode
					+ File.separator + divisionCode + File.separator + eventCode + File.separator + assessmentCode
					+ File.separator + "customerLogo" + File.separator + customerLogoImage);
			customerLogoPathMapOnAssCode.put(assessmentCode, customerLogoPath);
			logger.debug("in getScoreReport customerLogoPath=" + customerLogoPath);
		} catch (Exception e) {
			logger.error("ERROR : getScoreReport -- for finding customer logo Exception--" + e.getMessage());
		}

		List<CandidateDetails> rpsCandidateDetailsList = null;
		if (qpId == 0)
			rpsCandidateDetailsList = getCandidateInfoByDateAssessmentBatchAcs(startDate, endDate, assessmentCode,
					acsId, bId, start, end, orderType, column);
		else
			rpsCandidateDetailsList = findRpsCandidateInfoByAssessmentDateQpIdsByLimit(startDate, endDate,
					assessmentCode, qpId, start, end, orderType, column);

		if (rpsCandidateDetailsList == null || rpsCandidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			return scoreReportJson;
		}

		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new LinkedHashMap<>();
		for (CandidateDetails rpsMasterAssociation : rpsCandidateDetailsList)
			uniqueIdToMasterAssnMap.put(rpsMasterAssociation.getUniqueCandidateId(), rpsMasterAssociation);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return scoreReportJson;
		}
		ScoreReportWrapper scoreReportWrapper = new ScoreReportWrapper();
		List<ScoreReportEntity> scoreReportEntitiesList = new LinkedList<>();

		// fetch score report grid data, returns maximum sections count across
		// all rows
		Map<String, Integer> maxSectionsCount = scoreReportUtility.getScoreReportGridData(rpsCandidateResponseLitesList,
				uniqueIdToMasterAssnMap, scoreReportEntitiesList, gradingSchemeDos, sectionWeightageCutOffEntityMap,
				qtypeSectionMap, null, false);
		rpsCandidateResponseLitesList = null;
		scoreReportWrapper.setMaxSectionsCount(maxSectionsCount.get(MAX_SECTION_COUNT).intValue());
		scoreReportWrapper.setMaxWetCount(maxSectionsCount.get(WET_QUESTION_COUNT).intValue());
		scoreReportWrapper.setSectionQuestionMap(qtypeSectionMap);
		scoreReportWrapper.setGradingEnabled(gradingEnabled);
		scoreReportWrapper.setSectionCutoffEnabled(sectionGroupEnabled);
		scoreReportWrapper.setDefaultGroup(isDefaultGroup);
		scoreReportWrapper.setScoreReportEntitiesList(scoreReportEntitiesList);
		scoreReportJson = gson.toJson(scoreReportWrapper);
		logger.debug("---OUT--getScoreReport()-----");
		return scoreReportJson;
	}

	@Transactional
	public String getScoreReportByLoginIdAndAssessment(String assessmentCode, Set<String> loginId) {
		logger.debug("---IN--getScoreReportByLoginIdAndAssessment()----- with  assessmentCode:" + assessmentCode);
		String scoreReportJson = RpsConstants.EMPTY_JSON;
		boolean isDefaultGroup = false;
		// check for the mandatory parameters
		if (assessmentCode == null || assessmentCode.isEmpty() || assessmentCode == null) {
			logger.error("Missing mandatory parameters assessmentCode");
			return scoreReportJson;
		}

		// for customer logo

		Map<String, List<String>> qtypeSectionMap = null;
		try {
			RpsQuestionPaper rpPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);
			qtypeSectionMap = rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());
			resultProcessingRuleDo = scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper,
					assessmentCode);

			gradingEnabled = false;
			sectionGroupEnabled = false;
			gradingSchemeDos = null;
			sectionWeightageCutOffEntityMap = null;
			if (resultProcessingRuleDo != null) {
				gradingSchemeDos = resultProcessingRuleDo.getGradingSchemeDos();
				sectionWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupSectionMap();
			}

			if (gradingSchemeDos != null && !gradingSchemeDos.isEmpty())
				gradingEnabled = true;
			if (sectionWeightageCutOffEntityMap != null && !sectionWeightageCutOffEntityMap.isEmpty())
				sectionGroupEnabled = true;

			if (sectionWeightageCutOffEntityMap != null && sectionWeightageCutOffEntityMap.size() == 1) {
				if (sectionWeightageCutOffEntityMap.containsKey("DEFAULTGROUP"))
					isDefaultGroup = true;
			}

		} catch (Exception e) {
			logger.error("ERROR : getScoreReport -- for finding customer logo Exception--" + e.getMessage());
		}

		List<CandidateDetails> rpsCandidateDetailsList = rpsMasterAssociationService
				.findRpsCandidateDetailsInfoByCandidateLoginIdAndAssessmentCode(loginId, assessmentCode);
		if (rpsCandidateDetailsList == null || rpsCandidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			return scoreReportJson;
		}

		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new LinkedHashMap<>();
		for (CandidateDetails rpsMasterAssociation : rpsCandidateDetailsList)
			uniqueIdToMasterAssnMap.put(rpsMasterAssociation.getUniqueCandidateId(), rpsMasterAssociation);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		rpsCandidateResponseLitesList = rpsCandidateResponseService
				.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return scoreReportJson;
		}
		ScoreReportWrapper scoreReportWrapper = new ScoreReportWrapper();
		List<ScoreReportEntity> scoreReportEntitiesList = new LinkedList<>();

		// fetch score report grid data, returns maximum sections count across
		// all rows
		Map<String, Integer> maxSectionsCount = scoreReportUtility.getScoreReportGridData(rpsCandidateResponseLitesList,
				uniqueIdToMasterAssnMap, scoreReportEntitiesList, gradingSchemeDos, sectionWeightageCutOffEntityMap,
				qtypeSectionMap, null, false);
		rpsCandidateResponseLitesList = null;
		scoreReportWrapper.setMaxSectionsCount(maxSectionsCount.get(MAX_SECTION_COUNT).intValue());
		scoreReportWrapper.setMaxWetCount(maxSectionsCount.get(WET_QUESTION_COUNT).intValue());
		scoreReportWrapper.setSectionQuestionMap(qtypeSectionMap);
		scoreReportWrapper.setGradingEnabled(gradingEnabled);
		scoreReportWrapper.setSectionCutoffEnabled(sectionGroupEnabled);
		scoreReportWrapper.setDefaultGroup(isDefaultGroup);
		scoreReportWrapper.setScoreReportEntitiesList(scoreReportEntitiesList);
		scoreReportJson = gson.toJson(scoreReportWrapper);
		logger.debug("---OUT--getScoreReport()-----");
		return scoreReportJson;
	}

	/**
	 * get RpsMasterAsso On Acs Id And Batch Id
	 */
	public List<RpsMasterAssociation> getRpsMasterAssoOnAcsAndBatchIds(String eventCode, Date rangeStartDate,
			Date rangeEndDate, String assessmentCode, Integer acsId, Integer bId) {
		List<RpsMasterAssociation> rpsMasterAssociations = null;
		if ((acsId == null && bId == null) || (acsId == 0 && bId == 0)) {
			logger.debug("---IN--getRpsMasterAssoOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id = null");
			rpsMasterAssociations = rpsMasterAssociationService.findAllMAssonsByEventAssCodeInDateRange(eventCode,
					rangeStartDate, rangeEndDate, assessmentCode);
		} else if (acsId != null && acsId != 0 && (bId == 0 || bId == null)) {
			logger.debug(
					"---IN--getRpsMasterAssoOnAcsAndBatchIds()-----" + " with batch Id = null and Acs Id : " + acsId);
			rpsMasterAssociations = rpsMasterAssociationService.findAllMAssonsByEventAssCodeInDateRangeOnAcsId(
					eventCode, rangeStartDate, rangeEndDate, assessmentCode, acsId);
		} else if (bId != null && bId != 0 && (acsId == 0 || acsId == null)) {
			logger.debug(
					"---IN--getRpsMasterAssoOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id : " + bId);
			rpsMasterAssociations = rpsMasterAssociationService.findAllMAssonsByEventAssCodeInDateRangeOnBatchId(
					eventCode, rangeStartDate, rangeEndDate, assessmentCode, bId);
		} else if (acsId != null && bId != null && acsId != 0 && bId != 0) {
			logger.debug("---IN--getRpsMasterAssoOnAcsAndBatchIds()-----" + " with Acs Id : " + acsId
					+ " and batch Id : " + bId);
			rpsMasterAssociations = rpsMasterAssociationService.findAllMAssonsByEventAssCodeInDateRangeOnACSAndBatchIds(
					eventCode, rangeStartDate, rangeEndDate, assessmentCode, acsId, bId);
		}
		return rpsMasterAssociations;
	}

	/**
	 * get RpsMasterAsso On Acs Id And Batch Id
	 */
	public List<Integer> getRpsMasterAssoUniqueCandidateIdsOnAcsAndBatchIds(String eventCode, Date rangeStartDate,
			Date rangeEndDate, String assessmentCode, Integer acsId, Integer bId) {
		List<Integer> rpsMasterAssoUniqueCandidateIds = null;
		if ((acsId == null && bId == null) || (acsId == 0 && bId == 0)) {
			logger.debug(
					"---IN--getRpsMasterAssoLiteOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id = null");
			rpsMasterAssoUniqueCandidateIds = rpsMasterAssociationService.findAllUniqueCandIdsByEventAssCodeInDateRange(
					eventCode, rangeStartDate, rangeEndDate, assessmentCode);
		} else if (acsId != null && acsId != 0 && (bId == 0 || bId == null)) {
			logger.debug("---IN--getRpsMasterAssoLiteOnAcsAndBatchIds()-----" + " with batch Id = null and Acs Id : "
					+ acsId);
			rpsMasterAssoUniqueCandidateIds = rpsMasterAssociationService
					.findAllUniqueCandIdsByEventAssCodeInDateRangeOnAcsId(eventCode, rangeStartDate, rangeEndDate,
							assessmentCode, acsId);
		} else if (bId != null && bId != 0 && (acsId == 0 || acsId == null)) {
			logger.debug(
					"---IN--getRpsMasterAssoLiteOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id : " + bId);
			rpsMasterAssoUniqueCandidateIds = rpsMasterAssociationService
					.findAllUniqueCandIdsByEventAssCodeInDateRangeOnBatchId(eventCode, rangeStartDate, rangeEndDate,
							assessmentCode, bId);
		} else if (acsId != null && bId != null && acsId != 0 && bId != 0) {
			logger.debug("---IN--getRpsMasterAssoLiteOnAcsAndBatchIds()-----" + " with Acs Id : " + acsId
					+ " and batch Id : " + bId);
			rpsMasterAssoUniqueCandidateIds = rpsMasterAssociationService
					.findAllUniqueCandIdsByEventAssCodeInDateRangeOnACSAndBatchIds(eventCode, rangeStartDate,
							rangeEndDate, assessmentCode, acsId, bId);
		}
		return rpsMasterAssoUniqueCandidateIds;
	}

	/**
	 * get RpsMasterAsso On Acs Id And Batch Id
	 */
	public List<CandidateDetails> findRpsCandidateInfoByAssessmentDateQpIdsByLimit(String rangeStartDate,
			String rangeEndDate, String assessmentCode, Integer qpid, int start, int end, String orderType,
			String column) {
		List<CandidateDetails> candidateDetailsList = null;
		if (qpid != 0) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentDateQpIdsByLimit()-----"
					+ " with Acs Id = null and batch Id = null");
			candidateDetailsList = rpsMasterAssociationService.findRpsCandidateInfoByAssessmentDateQpIdsByLimit(
					rangeStartDate, rangeEndDate, assessmentCode, qpid, start, end, orderType, column);
		}
		return candidateDetailsList;
	}

	public List<CandidateDetails> getCandidateInfoByDateAssessmentBatchAcs(String rangeStartDate, String rangeEndDate,
			String assessmentCode, Integer acsId, Integer bId, int start, int end, String orderType, String column) {
		List<CandidateDetails> candidateDetailsList = null;
		if ((acsId == null && bId == null) || (acsId == 0 && bId == 0)) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentCodeDate()-----"
					+ " with Acs Id = null and batch Id = null");
			candidateDetailsList = rpsMasterAssociationService.findRpsCandidateInfoByAssessmentAndTimeByLimit(
					rangeStartDate, rangeEndDate, assessmentCode, start, end, orderType, column);
		} else if (acsId != null && acsId != 0 && (bId == 0 || bId == null)) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentCodeDateAndAcs()-----"
					+ " with batch Id = null and Acs Id : " + acsId);
			candidateDetailsList = rpsMasterAssociationService.findRpsCandidateInfoByAssessmentDateAcsByLimit(
					rangeStartDate, rangeEndDate, assessmentCode, acsId, start, end, orderType, column);
		} else if (bId != null && bId != 0 && (acsId == 0 || acsId == null)) {
			logger.debug("---IN--findRMAByAssessmentCodeBatchIdAndTimeAndLimitByOrder()-----"
					+ " with Acs Id = null and batch Id : " + bId);
			Set<Integer> batchIdList = new HashSet<>();
			batchIdList.add(bId);
			candidateDetailsList = rpsMasterAssociationService.findRMAByAssessmentCodeBatchIdAndTimeAndLimitByOrder(
					assessmentCode, batchIdList, rangeStartDate, rangeEndDate, start, end, orderType, column);
		} else if (acsId != null && bId != null && acsId != 0 && bId != 0) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentCodeDateAcsBatchIdByLimit()-----" + " with Acs Id : "
					+ acsId + " and batch Id : " + bId);
			candidateDetailsList = rpsMasterAssociationService
					.findRpsCandidateInfoByAssessmentCodeDateAcsBatchIdByLimit(rangeStartDate, rangeEndDate,
							assessmentCode, acsId, bId, start, end, orderType, column);
		}
		return candidateDetailsList;
	}

	/**
	 * get RpsMasterAsso On Acs Id And Batch Id
	 */
	public BigInteger getCandidateCountByDateAssessmentBatchAcs(String rangeStartDate, String rangeEndDate,
			String assessmentCode, Integer acsId, Integer bId) {
		BigInteger candidateDetailsCount = null;
		if ((acsId == null && bId == null) || (acsId == 0 && bId == 0)) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentCodeDate()-----"
					+ " with Acs Id = null and batch Id = null");
			candidateDetailsCount = rpsMasterAssociationService
					.findCandidateCountByAssessmentCodeAndTime(assessmentCode, rangeStartDate, rangeEndDate);
		} else if (acsId != null && acsId != 0 && (bId == 0 || bId == null)) {
			logger.debug("---IN--findRpsCandidateCountByAssessmentCodeDateAndAcs()-----"
					+ " with batch Id = null and Acs Id : " + acsId);
			candidateDetailsCount = rpsMasterAssociationService.findRpsCandidateCountByAssessmentCodeDateAndAcs(
					rangeStartDate, rangeEndDate, assessmentCode, acsId);
		} else if (bId != null && bId != 0 && (acsId == 0 || acsId == null)) {
			logger.debug("---IN--findRpsCandidateInfoByAssessmentCodeDateBatchId()-----"
					+ " with Acs Id = null and batch Id : " + bId);
			candidateDetailsCount = rpsMasterAssociationService.findCountByAssessmentCodeBatchAndAcsIdAndTime(
					assessmentCode, acsId, bId, rangeStartDate, rangeEndDate);
		} else if (acsId != null && bId != null && acsId != 0 && bId != 0) {
			logger.debug("---IN--findAllMAssonsByEventAssCodeInDateRangeOnACSAndBatchIds()-----" + " with Acs Id : "
					+ acsId + " and batch Id : " + bId);
			candidateDetailsCount = rpsMasterAssociationService.findRpsCandidateCountByAssessmentCodeDateBatchId(
					rangeStartDate, rangeEndDate, assessmentCode, bId);
		}
		return candidateDetailsCount;
	}

	/**
	 * get RpsMasterAsso On Event Id
	 */
	private List<Integer> getBatchAcsAssonLite(String eventCode) {
		List<Integer> batchAcsIdList = rpsBatchAcsAssociationService.findAllBatchAcsAssonLiteByEventCode(eventCode);
		return batchAcsIdList;
	}
	/**
	 * get RpsMasterAsso On Acs Id And Batch Id
	 */
	private List<Integer> getBatchAcsAssonLiteOnAcsAndBatchIds(String eventCode, Integer acsId, Integer bId) {
		List<Integer> batchAcsIdList = null;
		if ((acsId == null && bId == null) || (acsId == 0 && bId == 0)) {
			logger.debug(
					"---IN--getBatchAcsAssonLiteOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id = null");
			batchAcsIdList = rpsBatchAcsAssociationService.findAllBatchAcsAssonLiteByEventCode(eventCode);
		} else if (acsId != null && acsId != 0 && (bId == 0 || bId == null)) {
			logger.debug("---IN--getBatchAcsAssonLiteOnAcsAndBatchIds()-----" + " with batch Id = null and Acs Id : "
					+ acsId);
			batchAcsIdList = rpsBatchAcsAssociationService.findAllBatchAcsAssonLiteByEventCodeOnACSId(eventCode, acsId);
		} else if (bId != null && bId != 0 && (acsId == 0 || acsId == null)) {
			logger.debug(
					"---IN--getBatchAcsAssonLiteOnAcsAndBatchIds()-----" + " with Acs Id = null and batch Id : " + bId);
			batchAcsIdList = rpsBatchAcsAssociationService.findAllBatchAcsAssonLiteByEventCodeOnBatchId(eventCode, bId);
		} else if (acsId != null && bId != null && acsId != 0 && bId != 0) {
			logger.debug("---IN--getBatchAcsAssonLiteOnAcsAndBatchIds()-----" + " with Acs Id : " + acsId
					+ " and batch Id : " + bId);
			batchAcsIdList = rpsBatchAcsAssociationService
					.findAllBatchAcsAssonLiteByEventCodeOnACSAndBatchIds(eventCode, acsId, bId);
		}
		return batchAcsIdList;
	}
	public String getCandidateFullName(RpsCandidate rpsCandidate, Integer uniquecanddiateid) {
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

	public CandidateDetails getFillCandidateDetailsFromMIF(CandidateDetails candidateDetails) {
		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(candidateDetails.getUniqueCandidateId());
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm != null) {
			PersonalInfo pInfo = mifForm.getPersonalInfo();
			if (pInfo != null) {
				if (pInfo.getFirstName() != null && !pInfo.getFirstName().isEmpty()) {
					candidateDetails.setFirstName(pInfo.getFirstName());
				}
				if (pInfo.getMiddleName() != null && !pInfo.getMiddleName().isEmpty()) {
					candidateDetails.setMiddleName(pInfo.getMiddleName());
				}
				if (pInfo.getLastName() != null && !pInfo.getLastName().isEmpty()) {
					candidateDetails.setLastName(pInfo.getLastName());
				}
				if (pInfo.getDob() != null && !pInfo.getDob().isEmpty()) {
					candidateDetails.setDob(pInfo.getDob());
				}
				if (pInfo.getGender() != null && !pInfo.getGender().isEmpty()) {
					candidateDetails.setGender(pInfo.getGender());
				}
			}
			ContactInfo cInfo = mifForm.getPermanentAddress();
			if (cInfo != null) {
				if (cInfo.getEmailId1() != null && !cInfo.getEmailId1().isEmpty()) {
					candidateDetails.setEmailID1(cInfo.getEmailId1());
				}
				if (cInfo.getMobile() != null && !cInfo.getMobile().isEmpty()) {
					candidateDetails.setPhone1(cInfo.getMobile());
				}
				if (cInfo.getTestCity() != null && !cInfo.getTestCity().isEmpty()) {
					candidateDetails.setTestCenterCity(cInfo.getTestCity());
				}
			}
		}
		return candidateDetails;
	}
	/**
	 * Gets the individual Candidate Score Details For Graph form data.
	 *
	 * @return the individual Candidate Score Details form data
	 * @throws ParseException
	 * @throws RpsException
	 */
	@Transactional
	public String getCandidateScoreDetailsForGraph(int uniqueCandidateId) throws ParseException, RpsException {
		logger.debug("---IN--getCandidateScoreForGraph()-----");
		String candidateScoreDetailsJson = RpsConstants.EMPTY_JSON;
		List<RpsQpSection> rpsQpSectionList = null;
		double maxScore = 0;
		double score = 0;
		double questionsAttempted = 0;
		double numOfQuestions = 0;
		double questionsCorrect = 0;
		RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationService
				.findByUniqueCandidateId(uniqueCandidateId);
		CandidateScoreDetailsForGraph candidateScoreDetailsForGraph = new CandidateScoreDetailsForGraph();
		Map<String, CandidateScoreEntity> candidateScoreDetailsForGraphMap = new TreeMap<>();
		RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());

		String candidateFullName = getCandidateFullName(rpsCandidate, uniqueCandidateId);

		candidateScoreDetailsForGraph.setCandidateName(candidateFullName.toString());
		candidateScoreDetailsForGraph.setCandidateId(rpsCandidate.getCandidateId1());

		RpsEvent rpsEvent = rpsEventService.getRpsEventPerEventCode(rpsCandidate.getRpsEvent().getEventCode());
		candidateScoreDetailsForGraph.setEventName((rpsEvent != null) ? rpsEvent.getEventName() : "");

		RpsCustomer rpsCustomer = rpsDivisionService
				.findRpsCustomerByDivCode(rpsEvent.getRpsDivision().getDivisionCode());
		candidateScoreDetailsForGraph.setCustomerName(rpsCustomer.getCustomerName());

		RpsAssessment rpsAssessment = rpsAssessmentService
				.findByCode(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
		if (rpsAssessment != null) {
			candidateScoreDetailsForGraph.setTestName(rpsAssessment.getAssessmentName());
			candidateScoreDetailsForGraph.setTestId(rpsAssessment.getAssessmentCode());
			rpsQpSectionList = rpsQpSectionService.findAllQpSectionsByAssmentCode(rpsAssessment.getAssessmentCode());
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM YYYY");
		String convertedDate = dateFormat.format(rpsMasterAssociation.getTestStartTime());
		candidateScoreDetailsForGraph.setTestDate(convertedDate);

		RpsCandidateResponse rpsCandidateResponse = rpsCandidateResponseService
				.findByUniqueCandidateId(uniqueCandidateId);

		RpsQuestionPaper rpsQuestionPaper = rpsCandidateResponse.getRpsQuestionPaper();
		String jsonForSecWiseMarks = rpsQuestionPaper.getJsonForSecWiseMarks();
		Type type = new TypeToken<HashMap<String, Double>>() {
		}.getType();
		Map<String, Double> jsonForSecWiseMarksMap = new Gson().fromJson(jsonForSecWiseMarks, type);

		Map<String, RpsSectionCandidateResponse> secIdentifierToCandRespMap = new HashMap<>();
		List<RpsSectionCandidateResponse> rpsSectionCandidateResponseList = rpsSectionCandidateResponseService
				.findAllSecCandidateRespByCandRespId(rpsCandidateResponse.getCandidateResponseId());
		if (rpsSectionCandidateResponseList != null && !rpsSectionCandidateResponseList.isEmpty()) {
			for (RpsSectionCandidateResponse rpsSectionCandidateResponse : rpsSectionCandidateResponseList)
				secIdentifierToCandRespMap.put(rpsSectionCandidateResponse.getSecIdentifier(),
						rpsSectionCandidateResponse);
		}

		Map<String, RpsQpSection> secIdentifierToSectionMap = new HashMap<>();
		if (rpsQpSectionList != null && !rpsQpSectionList.isEmpty()) {
			for (RpsQpSection rpsQpSection : rpsQpSectionList)
				secIdentifierToSectionMap.put(rpsQpSection.getSecIdentifier(), rpsQpSection);
		}

		for (String secIdentifier : secIdentifierToSectionMap.keySet()) {
			double noOfQsPerSection = 0;
			CandidateScoreEntity candidateScoreEntity = new CandidateScoreEntity();
			RpsSectionCandidateResponse rpsSectionCandidateResponse = secIdentifierToCandRespMap.get(secIdentifier);
			RpsQpSection rpsQpSection = secIdentifierToSectionMap.get(secIdentifier);
			if (rpsQpSection != null) {
				Long numberOfQuestionsByQPAndSections = rpsQuestionAssociationRepository
						.getNumberOfQuestionsByQPAndSections(rpsQuestionPaper.getQpId(), rpsQpSection.getQpSecId(),
								QuestionType.READING_COMPREHENSION.toString());
				noOfQsPerSection = numberOfQuestionsByQPAndSections == null ? 0
						: numberOfQuestionsByQPAndSections.doubleValue();
				if (rpsQpSection.getSecScore() != null) {
					candidateScoreEntity.setMaxScore(decimalFormat.format(rpsQpSection.getSecScore()));
					maxScore += rpsQpSection.getSecScore().doubleValue();
				} else
					candidateScoreEntity.setMaxScore(RpsConstants.DECIMAL_FORMAT);
				numOfQuestions += noOfQsPerSection;
			} else {
				candidateScoreEntity.setMaxScore(RpsConstants.DECIMAL_FORMAT);
			}
			Double secScore = null;
			if (jsonForSecWiseMarksMap != null && rpsQpSection != null) {
				secScore = jsonForSecWiseMarksMap.get(secIdentifier);
				if (secScore != null) {
					candidateScoreEntity.setMaxScore(decimalFormat.format(secScore));
					maxScore += secScore.doubleValue();
				} else
					candidateScoreEntity.setMaxScore(RpsConstants.DECIMAL_FORMAT);

				// if (rpsQpSection.getNumOfQuestions() != null)
				// numOfQuestions +=
				// rpsQpSection.getNumOfQuestions().doubleValue();
			} else {
				candidateScoreEntity.setMaxScore(RpsConstants.DECIMAL_FORMAT);
			}

			if (rpsSectionCandidateResponse != null) {
				if (rpsSectionCandidateResponse.getScore() != null) {
					candidateScoreEntity.setScore(decimalFormat.format(rpsSectionCandidateResponse.getScore()));
					score += rpsSectionCandidateResponse.getScore().doubleValue();
				} else
					candidateScoreEntity.setScore(RpsConstants.DECIMAL_FORMAT);

				candidateScoreEntity.setPercentageCorrect(sharedMethodsUtility.calculatePercentage(
						rpsSectionCandidateResponse.getQuestionsCorrect().doubleValue(),
						rpsSectionCandidateResponse.getQuestionsAttempted().doubleValue()));

				questionsAttempted += rpsSectionCandidateResponse.getQuestionsAttempted().doubleValue();
				questionsCorrect += rpsSectionCandidateResponse.getQuestionsCorrect().doubleValue();
			} else {
				candidateScoreEntity.setScore(RpsConstants.DECIMAL_FORMAT);
				candidateScoreEntity.setPercentageCorrect(RpsConstants.DECIMAL_FORMAT);
			}

			if (rpsQpSection != null && rpsSectionCandidateResponse != null) {
				candidateScoreEntity.setPercentageScore(
						sharedMethodsUtility.calculatePercentage(rpsSectionCandidateResponse.getScore(), secScore));
				candidateScoreEntity.setPercentageAttempted(sharedMethodsUtility.calculatePercentage(
						rpsSectionCandidateResponse.getQuestionsAttempted().doubleValue(), noOfQsPerSection));
			} else {
				candidateScoreEntity.setPercentageScore(RpsConstants.DECIMAL_FORMAT);
				candidateScoreEntity.setPercentageAttempted(RpsConstants.DECIMAL_FORMAT);
			}

			// candidateScoreDetailsForGraphMap.put(secIdentifier,
			// candidateScoreEntity);
			// section Id should be title instead of section1 or section2 for
			// section wise report generation
			candidateScoreDetailsForGraphMap.put(rpsQpSection != null ? rpsQpSection.getTitle() : RpsConstants.NA,
					candidateScoreEntity);
		}
		candidateScoreDetailsForGraph.setSectionwiseScoreDetails(candidateScoreDetailsForGraphMap);
		candidateScoreDetailsForGraph.setMaxScore(decimalFormat.format(maxScore));
		candidateScoreDetailsForGraph.setScore(decimalFormat.format(score));
		candidateScoreDetailsForGraph.setPercentageScore(sharedMethodsUtility.calculatePercentage(score, maxScore));
		candidateScoreDetailsForGraph
				.setPercentageAttempted(sharedMethodsUtility.calculatePercentage(questionsAttempted, numOfQuestions));
		candidateScoreDetailsForGraph
				.setPercentageCorrect(sharedMethodsUtility.calculatePercentage(questionsCorrect, questionsAttempted));
		logger.debug("OpenEventUIService.getCandidateScoreDetailsForGraph() maxScore=" + maxScore + " score=" + score
				+ " questionsAttempted=" + questionsAttempted + " numOfQuestions=" + numOfQuestions
				+ " questionsCorrect=" + questionsCorrect);

		try {
			Map<String, Map<String, Map<String, CandidateScoreInfoEntity>>> mapForBothTopicAndDifficulty = getCandidateTopicLevelScoreDetailsForReport(
					uniqueCandidateId);
			candidateScoreDetailsForGraph.setTopicwiseScoreDetails(mapForBothTopicAndDifficulty.get("TopicWiseInfo"));
			candidateScoreDetailsForGraph
					.setDifficultywiseScoreDetails(mapForBothTopicAndDifficulty.get("DifficultyWiseInfo"));
		} catch (RpsException e) {
			throw new RpsException("Invalid data...");
		}
		candidateScoreDetailsJson = gson.toJson(candidateScoreDetailsForGraph);
		logger.debug("---OUT--getCandidateScoreForGraph()-----");

		return candidateScoreDetailsJson;
	}

	public Map<String, Map<String, Map<String, CandidateScoreInfoEntity>>> getCandidateTopicLevelScoreDetailsForReport(
			int uniqueCandidateId) throws ParseException, RpsException {
		logger.debug("---IN--getCandidateTopicLevelScoreDetailsForReport()-----");

		RpsCandidateResponse rpsCandidateResponse = rpsCandidateResponseService
				.findByUniqueCandidateId(uniqueCandidateId);

		String candidateJsonResponse = rpsCandidateResponse.getResponse();
		String candidateJsonShuffle = rpsCandidateResponse.getShuffleSequence();
		Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
		}.getType();
		List<CandidateResponseEntity> candidateResponseEntityList = null;
		// if candidate response comes null when candidate did not attempted any
		// question on TP.
		if (candidateJsonResponse != null)
			candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
		else
			candidateResponseEntityList = new ArrayList<>();

		Type maptype = new TypeToken<Map<String, ArrayList<String>>>() {
		}.getType();
		Map<String, List<String>> candidateShuffleList = new Gson().fromJson(candidateJsonShuffle, maptype);
		Set<String> responseQuestIdsSet = new HashSet<>();
		Set<String> qusSecKeySet = candidateShuffleList.keySet();
		Iterator<String> qusSecKeyIt = qusSecKeySet.iterator();
		while (qusSecKeyIt.hasNext()) {
			List<String> shuffleList = candidateShuffleList.get(qusSecKeyIt.next());
			if (shuffleList != null)
				responseQuestIdsSet.addAll(shuffleList);
		}
		// responseQuestIdsSet.addAll(qusSecKeySet);
		Map<String, String> candidateResponseMap = new HashMap<>();

		for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntityList) {
			Map<String, String> choicesMap = candidateResponseEntity.getResponse();
			if (choicesMap != null && !choicesMap.isEmpty()) {
				Set<String> choiceOptions = choicesMap.keySet();
				if (choiceOptions != null && !choiceOptions.isEmpty()) {
					Iterator<String> chIt = choiceOptions.iterator();
					if (!(!isMarkedForReviewQuestionsToBeEvaluated && candidateResponseEntity.isMarkForReview()))
						candidateResponseMap.put(candidateResponseEntity.getQuestionID(), choicesMap.get(chIt.next()));
				}
			}
		}

		RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationService
				.findByUniqueCandidateId(uniqueCandidateId);
		Map<String, RpsQuestion> questionMap = rpsCandidateResponseService
				.getQuestionsForCandidateByShuffleWithoutParentQuestion(rpsCandidateResponse.getResponse(),
						rpsCandidateResponse.getShuffleSequence(),
						rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
		List<RpsQuestion> questionsList = new ArrayList<>(questionMap.values());
		;
		if (questionsList == null || questionsList.isEmpty()) {
			throw new RpsException("question list is empty");
		}
		if (questionsList.size() != responseQuestIdsSet.size()) {
			throw new RpsException(
					"candidate question count is not matching with DB question count filtered by given assessmentCode");
		}
		Iterator<RpsQuestion> quesit = questionsList.iterator();
		Map<String, Map<String, Map<String, CandidateScoreInfoEntity>>> mapForBothTopicAndDifficulty = new HashMap<>();
		Map<String, Map<String, CandidateScoreInfoEntity>> topicLevelReportMap = new HashMap<>();
		Map<String, Map<String, CandidateScoreInfoEntity>> difficultyLevelReportMap = new HashMap<>();
		while (quesit.hasNext()) {
			String secNameIdentifier = null;
			RpsQuestion rpsQuestion = quesit.next();
			if (rpsQuestion == null) {
				throw new RpsException("rpsQuestion can not be null");
			}
			String topic = rpsQuestion.getTopic();
			String difficultyLevel = rpsQuestion.getDifficultyLevel();
			if (topic == null || topic.isEmpty()) {
				throw new RpsException("topic can not be null or empty");
			}
			if (difficultyLevel == null || difficultyLevel.isEmpty()) {
				throw new RpsException("difficultyLevel can not be null or empty");
			}
			// As we are taking questions list from shuffel sequence and shuffel
			// sequence does not have RC Question, So, no need to check below
			// condition
			if (rpsQuestion.getQuestionType()
					.equalsIgnoreCase(QuestionType.READING_COMPREHENSION.toString())
					|| rpsQuestion.getQuestionType().equalsIgnoreCase(QuestionType.SURVEY.toString())) {
				continue;
			}
			Set<RpsQuestionAssociation> questAssociationSet = rpsQuestion.getRpsQuestionAssociations();
			Iterator<RpsQuestionAssociation> quesAssit = questAssociationSet.iterator();
			while (quesAssit.hasNext()) {
				// secNameIdentifier =
				// quesAssit.next().getRpsQpSection().getSecIdentifier();
				secNameIdentifier = quesAssit.next().getRpsQpSection().getTitle();
				break;
			}

			// topic level score calculation
			if (!topicLevelReportMap.containsKey(secNameIdentifier)) {
				topicLevelReportMap.put(secNameIdentifier, new HashMap<String, CandidateScoreInfoEntity>());
			}
			Map<String, CandidateScoreInfoEntity> candidateTopicScoreDetailsEntity = topicLevelReportMap
					.get(secNameIdentifier);

			if (!candidateTopicScoreDetailsEntity.containsKey(topic)) {
				candidateTopicScoreDetailsEntity.put(topic, new CandidateScoreInfoEntity());
			}

			// for default answer
			RpsBrLr rpsBrLr = rpsQpTemplateService.getRpsBrLrByAssmntAndQpPackId(
					rpsCandidateResponse.getRpsQuestionPaper().getRpsAssessment().getAssessmentCode(),
					rpsCandidateResponse.getRpsQuestionPaper().getRpsQuestionPaperPack().getQpPackId());

			Map<String, SectionLevelRules> sectionLevelRulesMap = null;
			if (rpsBrLr != null) {
				String brRules = rpsBrLr.getBrRules();
				BRulesExportEntity bRulesExportEntity = gson.fromJson(brRules, BRulesExportEntity.class);
				if (bRulesExportEntity != null) {
					// section level time duration
					sectionLevelRulesMap = bRulesExportEntity.getSectionLevelRulesMap();
				}
			}
			if (!candidateResponseMap.containsKey(rpsQuestion.getQuestId())) {
				// && candidateResponseMap.get(rpsQuestion.getQuestId()) != null
				// &&
				// !candidateResponseMap.get(rpsQuestion.getQuestId()).isEmpty())

				DefaultAnswerOptionEnum defaultAnswerOption = null;

				if (rpsBrLr != null) {
					String brRules = rpsBrLr.getBrRules();
					BRulesExportEntity bRulesExportEntity = gson.fromJson(brRules, BRulesExportEntity.class);
					boolean isDefaultAnswer = false;

					if (bRulesExportEntity != null) {
						isDefaultAnswer = bRulesExportEntity.isDefaultAnswer();
						if (isDefaultAnswer)
							defaultAnswerOption = bRulesExportEntity.getDefaultAnswerOption();

						// section level time duration
						sectionLevelRulesMap = bRulesExportEntity.getSectionLevelRulesMap();
					}
				}
				if (defaultAnswerOption != null) {
					Type mapType = new TypeToken<Map<String, ArrayList<String>>>() {
					}.getType();
					Map<String, ArrayList<String>> optionShuffleSeq = gson
							.fromJson(rpsCandidateResponse.getOptionShuffleSequence(), mapType);
					List<String> optionsForQuest = optionShuffleSeq.get(rpsQuestion.getQuestId());

					int noOfOptions = 0;
					if (optionsForQuest != null)
						noOfOptions = optionsForQuest.size();
					else
						noOfOptions = 4;
					String candidateResponseString = getDefaultAnswer(noOfOptions, defaultAnswerOption);
					candidateResponseMap.put(rpsQuestion.getQuestId(), candidateResponseString);

				}
			}
			CandidateResponseEntity candidateResponseEntity = null;
			for (CandidateResponseEntity responseEntity : candidateResponseEntityList) {
				if (responseEntity.getQuestionID().equals(rpsQuestion.getQuestId()))
					candidateResponseEntity = responseEntity;
			}

			CandidateScoreInfoEntity topicScoreEntity = candidateTopicScoreDetailsEntity.get(topic);
			topicScoreEntity = getCandidateScoreInfoEntity(topicScoreEntity, candidateResponseMap, rpsQuestion,
					uniqueCandidateId, candidateResponseEntity, sectionLevelRulesMap);
			candidateTopicScoreDetailsEntity.put(topic, topicScoreEntity);
			// topicLevelReportMap.put(secNameIdentifier,
			// candidateTopicScoreDetailsEntity);
			// section Id should be title instead of section1 or section2 for
			// advance report generation
			topicLevelReportMap.put(secNameIdentifier, candidateTopicScoreDetailsEntity);

			// difficulty level score calculation
			if (!difficultyLevelReportMap.containsKey(secNameIdentifier)) {
				difficultyLevelReportMap.put(secNameIdentifier, new HashMap<String, CandidateScoreInfoEntity>());
			}
			Map<String, CandidateScoreInfoEntity> candidateDificultyScoreDetailsEntity = difficultyLevelReportMap
					.get(secNameIdentifier);

			if (!candidateDificultyScoreDetailsEntity.containsKey(difficultyLevel)) {
				candidateDificultyScoreDetailsEntity.put(difficultyLevel, new CandidateScoreInfoEntity());
			}

			CandidateScoreInfoEntity difficultyScoreEntity = candidateDificultyScoreDetailsEntity.get(difficultyLevel);
			difficultyScoreEntity = getCandidateScoreInfoEntity(difficultyScoreEntity, candidateResponseMap,
					rpsQuestion, uniqueCandidateId, candidateResponseEntity, sectionLevelRulesMap);
			candidateDificultyScoreDetailsEntity.put(difficultyLevel, difficultyScoreEntity);
			// difficultyLevelReportMap.put(secNameIdentifier,
			// candidateDificultyScoreDetailsEntity);
			// section Id should be title instead of section1 or section2 for
			// advance report generation
			difficultyLevelReportMap.put(secNameIdentifier, candidateDificultyScoreDetailsEntity);
		}
		mapForBothTopicAndDifficulty.put("TopicWiseInfo", topicLevelReportMap);
		mapForBothTopicAndDifficulty.put("DifficultyWiseInfo", difficultyLevelReportMap);

		logger.debug("---OUT--getCandidateTopicLevelScoreDetailsForReport()-----");

		return mapForBothTopicAndDifficulty;
	}

	/**
	 * get Candidate Score Info Entity
	 *
	 * @param currentEntity
	 * @param candidateResponseMap
	 * @param rpsQuestion
	 * @param uniqueCandidateId
	 * @param candidateResponseEntity
	 * @param sectionLevelRulesMap
	 * @return
	 */
	public CandidateScoreInfoEntity getCandidateScoreInfoEntity(CandidateScoreInfoEntity currentEntity,
			Map<String, String> candidateResponseMap, RpsQuestion rpsQuestion, int uniqueCandidateId,
			CandidateResponseEntity candidateResponseEntity, Map<String, SectionLevelRules> sectionLevelRulesMap) {
		logger.debug("---IN--getCandidateScoreInfoEntity()-----");

		Type listType = new TypeToken<List<ResponseMarkBean>>() {
		}.getType();
		List<ResponseMarkBean> responseMarkBeansForFitbDe = new ArrayList<>();
		if ((rpsQuestion.getQuestionType().equals(QuestionType.FILL_IN_THE_BLANK.toString())
				|| rpsQuestion.getQuestionType().equals(QuestionType.DATA_ENTRY.toString()))
				&& rpsQuestion.getQans() != null) {
			try {
				responseMarkBeansForFitbDe = new Gson().fromJson(rpsQuestion.getQans(), listType);
				currentEntity.setTotalQuestion(currentEntity.getTotalQuestion() + responseMarkBeansForFitbDe.size());
			} catch (Exception e) {
				logger.error("Unable to Parse Candidate Response for " + rpsQuestion.getQuestionType() + " Question ");
				String errMsg = "Could not process results for candidate with uniqueCandidateId::" + uniqueCandidateId;
				logger.error(errMsg, e);
				// return;
			}
		} else {
			currentEntity.setTotalQuestion(currentEntity.getTotalQuestion() + 1);
		}

		if (candidateResponseMap.containsKey(rpsQuestion.getQuestId())
				&& candidateResponseMap.get(rpsQuestion.getQuestId()) != null
				&& !candidateResponseMap.get(rpsQuestion.getQuestId()).isEmpty()) {
			currentEntity.setAttempted(currentEntity.getAttempted() + 1);
			if (rpsQuestion.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
				if (candidateResponseMap.get(rpsQuestion.getQuestId()).equalsIgnoreCase(rpsQuestion.getQans())) {
					currentEntity.setCorrect(currentEntity.getCorrect() + 1);
					currentEntity.setScore(currentEntity.getScore() + rpsQuestion.getScore());
				} else {
					negativeMarkingForEachResponse(currentEntity, rpsQuestion, candidateResponseEntity,
							sectionLevelRulesMap, null);
				}
				currentEntity.setMaxScore(currentEntity.getMaxScore() + rpsQuestion.getScore());
				currentEntity.setMinScore(0.0);
			} else if (rpsQuestion.getQuestionType()
					.equals(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString())) {
				Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
				}.getType();
				String responseMarkBeansJson = rpsQuestion.getQans();
				List<ResponseMarkBean> responseMarkBeans = new Gson().fromJson(responseMarkBeansJson, responseMarkType);
				double maxScore = 0.0;
				double minScore = 9999.0;
				if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
					for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
						if (responseMarkBean.getResponse()
								.equalsIgnoreCase(candidateResponseMap.get(rpsQuestion.getQuestId()))) {
							currentEntity.setCorrect(currentEntity.getCorrect() + 1);
							currentEntity
									.setScore(currentEntity.getScore() + responseMarkBean.getResponsePositiveMarks());
							// break;
						}
						// get min and max score for each question in a
						// topic-section
						if (responseMarkBean.getResponsePositiveMarks() > maxScore)
							maxScore = responseMarkBean.getResponsePositiveMarks();

						if (responseMarkBean.getResponsePositiveMarks() < minScore)
							minScore = responseMarkBean.getResponsePositiveMarks();
					}
				}
				// addition of all min and max scores
				currentEntity.setMaxScore(currentEntity.getMaxScore() + maxScore);
				currentEntity.setMinScore(currentEntity.getMinScore() + minScore);
			} else if (rpsQuestion.getQuestionType().equals(QuestionType.FILL_IN_THE_BLANK.toString())
					|| rpsQuestion.getQuestionType().equals(QuestionType.DATA_ENTRY.toString())) {

				// subtract whatever was added above, as we will be counting
				// each of the blanks
				currentEntity.setAttempted(currentEntity.getAttempted() - 1);

				currentEntity.setAttempted(currentEntity.getAttempted() + candidateResponseEntity.getResponse().size());
				// questionsAttempted +=
				// candidateResponseEntity.getResponse().size();
				double maxScore = 0.0;
				double minScore = 9999.0;
				for (ResponseMarkBean responseMarkBean : responseMarkBeansForFitbDe) {
					if (candidateResponseEntity.getResponse().containsKey(responseMarkBean.getResponse())) {
						Map<String, String> responseMap = candidateResponseEntity.getResponse();
						String candidateAns = responseMap.get(responseMarkBean.getResponse());
						String responseAnswer = responseMarkBean.getResponseAnswer();
						Double answerKey = 0.0;
						boolean isDoubleValue = false;
						try {
							answerKey = Double.parseDouble(responseAnswer);
							isDoubleValue = true;
						} catch (NumberFormatException e) {
							// its String value
							isDoubleValue = false;
						}
						if (isDoubleValue) {
							// if double value
							Double cAns = 0.0;
							try {
								cAns = Double.parseDouble(candidateAns);
							} catch (NumberFormatException e) {
								// its wrong answer, expecting double value
								negativeMarkingForEachResponse(currentEntity, rpsQuestion, candidateResponseEntity,
										sectionLevelRulesMap, responseMarkBean);
								continue;
							}
							if (cAns.doubleValue() == answerKey.doubleValue()) {
								currentEntity.setCorrect(currentEntity.getCorrect() + 1);
								currentEntity.setScore(
										currentEntity.getScore() + responseMarkBean.getResponsePositiveMarks());
							} else {
								negativeMarkingForEachResponse(currentEntity, rpsQuestion, candidateResponseEntity,
										sectionLevelRulesMap, responseMarkBean);
							}
						}
						else{
							// if string
							if (candidateAns.equalsIgnoreCase(responseAnswer)) {
								currentEntity.setCorrect(currentEntity.getCorrect() + 1);
								currentEntity.setScore(
										currentEntity.getScore() + responseMarkBean.getResponsePositiveMarks());
							} else {
								negativeMarkingForEachResponse(currentEntity, rpsQuestion, candidateResponseEntity,
										sectionLevelRulesMap, responseMarkBean);
							}
						}
					} else {
						negativeMarkingForEachResponse(currentEntity, rpsQuestion, candidateResponseEntity,
								sectionLevelRulesMap, responseMarkBean);
					}
					// get min and max score for each question in a
					// topic-section
					if (responseMarkBean.getResponsePositiveMarks() > maxScore)
						maxScore = responseMarkBean.getResponsePositiveMarks();

					if (responseMarkBean.getResponsePositiveMarks() < minScore)
						minScore = responseMarkBean.getResponsePositiveMarks();
				}
				// addition of all min and max scores
				currentEntity.setMaxScore(currentEntity.getMaxScore() + maxScore);
				currentEntity.setMinScore(currentEntity.getMinScore() + minScore);
			} else if (rpsQuestion.getQuestionType().equals(QuestionType.WRITTEN_ENGLISH_TEST.toString())) {
				RpsWetScoreEvaluation rpScoreEvaluation = rpsWetDataEvaluationService
						.getByCandidateUniqueIdAndQId(uniqueCandidateId, rpsQuestion.getQuestId());
				if (rpScoreEvaluation.getEvaluated() == 1) {
					if (rpScoreEvaluation.getScore() > 0) {
						currentEntity.setCorrect(currentEntity.getCorrect() + 1);
						currentEntity.setScore(currentEntity.getScore() + rpScoreEvaluation.getScore());
					}
				}
			}
		} else {
			// Candidate has not answered the question then get option score
			if (rpsQuestion.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
				currentEntity.setMaxScore(currentEntity.getMaxScore() + rpsQuestion.getScore());
				currentEntity.setMinScore(0.0);
			}
			if (rpsQuestion.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString())
					|| rpsQuestion.getQuestionType().equals(QuestionType.FILL_IN_THE_BLANK.toString())
					|| rpsQuestion.getQuestionType().equals(QuestionType.DATA_ENTRY.toString())) {
				getMinMaxScoreForMCQW(currentEntity, rpsQuestion);
			}
		}
		currentEntity.setTotalPercentage(
				sharedMethodsUtility.calculatePercentage(currentEntity.getScore(), currentEntity.getMaxScore()));
		currentEntity.setPercentageCorrect(
				sharedMethodsUtility.calculatePercentage(currentEntity.getCorrect(), currentEntity.getTotalQuestion()));
		currentEntity.setPercentageAttempted(sharedMethodsUtility.calculatePercentage(currentEntity.getAttempted(),
				currentEntity.getTotalQuestion()));

		logger.debug("---OUT--getCandidateScoreInfoEntity()-----");

		return currentEntity;
	}

	private void getMinMaxScoreForMCQW(CandidateScoreInfoEntity currentEntity, RpsQuestion rpsQuestion) {
		Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
		}.getType();
		String responseMarkBeansJson = rpsQuestion.getQans();
		List<ResponseMarkBean> responseMarkBeans = new Gson().fromJson(responseMarkBeansJson, responseMarkType);
		double maxScore = 0.0;
		double minScore = 9999.0;
		if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
			for (ResponseMarkBean responseMarkBean : responseMarkBeans) {

				// get min and max score for each question in a topic-section
				if (responseMarkBean.getResponsePositiveMarks() > maxScore)
					maxScore = responseMarkBean.getResponsePositiveMarks();

				if (responseMarkBean.getResponsePositiveMarks() < minScore)
					minScore = responseMarkBean.getResponsePositiveMarks();
			}
		}
		currentEntity.setMaxScore(currentEntity.getMaxScore() + maxScore);
		currentEntity.setMinScore(currentEntity.getMinScore() + minScore);
	}

	/**
	 * negativeMarking For EachResponse
	 *
	 * @param currentEntity
	 * @param rpsQuestion
	 * @param candidateResponseEntity
	 * @param sectionLevelRulesMap
	 */
	private void negativeMarkingForEachResponse(CandidateScoreInfoEntity currentEntity, RpsQuestion rpsQuestion,
			CandidateResponseEntity candidateResponseEntity, Map<String, SectionLevelRules> sectionLevelRulesMap,
			ResponseMarkBean responseMarkBean) {
		boolean isNegativeMarks = false;
		if (candidateResponseEntity != null && sectionLevelRulesMap != null) {
			SectionLevelRules sectionLevelRules = sectionLevelRulesMap.get(candidateResponseEntity.getSectionID());
			if (sectionLevelRules != null) {
				MarksAndScoreRules marksAndScoreRules = sectionLevelRules.getMarksAndScoreRules();
				if (marksAndScoreRules != null) {
					isNegativeMarks = marksAndScoreRules.isNegativeMarks();
					if (isNegativeMarks) {
						currentEntity.setScore(currentEntity.getScore()
								+ applyNegativeMarking(marksAndScoreRules, rpsQuestion, responseMarkBean));
					}
				}
			}
		}
	}

	/**
	 * Gets the MIF form data.
	 *
	 * @param candidateId
	 *            the candidate id
	 * @param eventCode
	 *            the event code
	 * @return the MIF form data
	 */
	public String getMIFFormData(Integer uniqueCandidateId) {
		logger.debug("---IN--getMIFFormData()-----");
		String mifFormJson = RpsConstants.EMPTY_JSON;
		// get mif
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsCandidateMIFDetails != null)
			mifFormJson = rpsCandidateMIFDetails.getMifFormJson();
		if (mifFormJson != null) {
			logger.debug("-----getMIFFormData() = {} and cand id = {} -----", mifFormJson, uniqueCandidateId);
			return mifFormJson;
		} else {
			logger.error("Candidate mif doesn't exist in database, " + uniqueCandidateId);
			return mifFormJson;
		}
	}

	/**
	 * Gets the section level score report.
	 *
	 * @param candRespId
	 *            the cand resp id
	 * @param assessmentCode
	 *            the assessment code
	 * @return the section level score report
	 */
	public String getSectionLevelScoreReport(Integer candRespId, String assessmentCode) {
		logger.debug("---IN--getSectionLevelScoreReport()-----");
		List<SectionLevelInfoEntity> sectionLevelInfoEntities = new ArrayList<>();
		List<RpsSectionCandidateResponse> rpsSectionCandidateResponsesList = rpsSectionCandidateResponseService
				.findAllSecCandidateRespByCandRespId(candRespId);
		if (rpsSectionCandidateResponsesList == null || rpsSectionCandidateResponsesList.isEmpty()) {
			logger.error("Section Level score is not available");
			return gson.toJson(sectionLevelInfoEntities);
		}
		List<RpsQpSection> rpsQpSectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
		if (rpsQpSectionsList == null || rpsQpSectionsList.isEmpty()) {
			logger.error("Section Level questions weightage is not available");
			return gson.toJson(sectionLevelInfoEntities);
		}
		Map<String, RpsQpSection> secIdentifierToSectionMap = new HashMap<>();
		for (RpsQpSection rpsQpSection : rpsQpSectionsList)
			secIdentifierToSectionMap.put(rpsQpSection.getSecIdentifier(), rpsQpSection);

		for (RpsSectionCandidateResponse rpsSectionCandidateResponse : rpsSectionCandidateResponsesList) {
			RpsQpSection rpsQpSection = secIdentifierToSectionMap.get(rpsSectionCandidateResponse.getSecIdentifier());

			SectionLevelInfoEntity sectionLevelInfoEntity = new SectionLevelInfoEntity();
			if (rpsQpSection != null) {
				sectionLevelInfoEntity.setSectionScore(
						rpsQpSection.getSecScore() == null ? "0.00" : decimalFormat.format(rpsQpSection.getSecScore()));
				sectionLevelInfoEntity.setScorePercentage(sharedMethodsUtility
						.calculatePercentage(rpsSectionCandidateResponse.getScore(), rpsQpSection.getSecScore()));
				Long numberOfQuestionsByQPAndSections = rpsQuestionAssociationRepository
						.getNumberOfQuestionsByQPAndSections(
								rpsSectionCandidateResponse.getRpsCandidateResponse().getRpsQuestionPaper(),
								rpsQpSection.getQpSecId(), QuestionType.READING_COMPREHENSION.toString());
				sectionLevelInfoEntity.setQuestionsCount(
						numberOfQuestionsByQPAndSections == null ? 0 : numberOfQuestionsByQPAndSections.intValue());
			}
			sectionLevelInfoEntity.setSectionId(rpsSectionCandidateResponse.getSecIdentifier());
			sectionLevelInfoEntity.setCandidateScore(rpsSectionCandidateResponse.getScore() == null ? "0.00"
					: decimalFormat.format(rpsSectionCandidateResponse.getScore()));
			sectionLevelInfoEntities.add(sectionLevelInfoEntity);
		}

		logger.debug("---OUT--getSectionLevelScoreReport()-----");
		return gson.toJson(sectionLevelInfoEntities);
	}

	/**
	 * Gets the response report.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the response report
	 */
	public String getResponseReport(String eventCode, String startDate, String endDate, String assessmentCode,
			Integer qpId, Integer acsId, Integer bId, int start, int end, String orderType, String column) {
		logger.debug("---IN--getResponseReport()-----");
		String responseReportJson = RpsConstants.EMPTY_JSON;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			return responseReportJson;
		}

		List<CandidateDetails> candidateDetailsList = null;
		if (qpId == 0)
			candidateDetailsList = getCandidateInfoByDateAssessmentBatchAcs(startDate, endDate, assessmentCode, acsId,
					bId, start, end, orderType, column);
		else
			candidateDetailsList = findRpsCandidateInfoByAssessmentDateQpIdsByLimit(startDate, endDate, assessmentCode,
					qpId, start, end, orderType, column);

		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			return responseReportJson;
		}
		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new LinkedHashMap<>();
		for (CandidateDetails candidateDetails : candidateDetailsList)
			uniqueIdToMasterAssnMap.put(candidateDetails.getUniqueCandidateId(), candidateDetails);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return responseReportJson;
		}

		ResponseReportWrapper responseReportWrapper;
		responseReportWrapper = responseReportUtility.getResponseReportGridInformation(rpsCandidateResponseLitesList,
				uniqueIdToMasterAssnMap, false);
		responseReportJson = gson.toJson(responseReportWrapper);
		logger.debug("---OUT--getResponseReport()-----");
		return responseReportJson;
	}

	/**
	 * Export score report to excel.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the string
	 */
	@Transactional
	public String exportScoreReportToExcel(String eventCode, String startDate, String endDate, String assessmentCode,
			Integer qpId) {
		logger.debug("---IN--exportScoreReportToExcel()-----");
		FileExportExportEntity fileExportExportEntity = null;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Missing mandatory parameters, evenCode, startDate, endDate");
			return gson.toJson(fileExportExportEntity);
		}
		RpsDivision rpsDivision = rpsEventService.findRpsDivisionByEvent(eventCode);

		List<CandidateDetails> candidateDetailsList = rpsMasterAssociationService
				.findRpsCandidateInfoByAssessmentCodeDate(startDate, endDate, assessmentCode);
		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new HashMap<>();
		for (CandidateDetails rpsMasterAssociation : candidateDetailsList)
			uniqueIdToMasterAssnMap.put(rpsMasterAssociation.getUniqueCandidateId(), rpsMasterAssociation);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate responses have been received in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		Map<Integer, RpsCandidate> cidToCandidateMap = new HashMap<>();
		List<RpsCandidate> rpsCandidatesList = rpsMasterAssociationService
				.findAllCandidateByUniqueIds(uniqueIdToMasterAssnMap.keySet());
		if (rpsCandidatesList == null || rpsCandidatesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate responses have been received in selected date range");
			return gson.toJson(fileExportExportEntity);
		}
		for (RpsCandidate rpsCandidate : rpsCandidatesList)
			cidToCandidateMap.put(rpsCandidate.getCid(), rpsCandidate);

		List<ScoreReportEntity> scoreReportEntitiesList = new ArrayList<>();
		RpsQuestionPaper rpPaper = null;
		if (qpId != null && qpId != 0) {
			rpPaper = rpsQuestionPaperService.findOne(qpId);
		} else
			rpPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);

		Map<String, List<String>> qtypeSectionMap = rpsQuestionService
				.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());
		Map<String, Integer> maxSectionsCountMap = scoreReportUtility.getScoreReportGridData(
				rpsCandidateResponseLitesList, uniqueIdToMasterAssnMap, scoreReportEntitiesList, gradingSchemeDos,
				sectionWeightageCutOffEntityMap, qtypeSectionMap, null, false);
		int maxSectionsCount = maxSectionsCountMap.get(MAX_SECTION_COUNT).intValue();
		int foreignLanguagesMaxCount = 0;
		if (scoreReportEntitiesList == null || scoreReportEntitiesList.isEmpty()) {
			logger.error("There is no candidate data available in Score Report");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"There is no candidate data available in Score Report");
			return gson.toJson(fileExportExportEntity);
		}
		Iterator<ScoreReportEntity> sIterator = scoreReportEntitiesList.iterator();
		List<CandidateSectionScoreData> cList = null;
		while (sIterator.hasNext()) {
			ScoreReportEntity sReportEntity = sIterator.next();
			cList = sReportEntity.getCandidateSectionScoreDataList();
			if (maxSectionsCount == cList.size()) {
				break;
			}
		}
		List<String> sectionItentifierList = new ArrayList<>();
		Iterator<CandidateSectionScoreData> cIterator = cList.iterator();
		while (cIterator.hasNext()) {
			CandidateSectionScoreData cData = cIterator.next();
			sectionItentifierList.add(cData.getSectionTitle());
		}

		fileExportExportEntity = exportToExcelUtility.exportScoreReportGridData(scoreReportEntitiesList, rpsDivision,
				eventCode, maxSectionsCount, sectionItentifierList, foreignLanguagesMaxCount);
		logger.debug("---OUT--exportScoreReportToExcel()-----");
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * Export score report to excel for AcsMif.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the string
	 */
	@Transactional
	public String exportScoreReportToExcelForAcsMif(String eventCode, String startDate, String endDate,
			String assessmentCode, Integer qpId, Map<String, Long> sectionLevelCutOffFromUI, Integer acsId, Integer bId,
			int start, int end, String orderType, String column) {
		logger.debug("---IN--exportScoreReportToExcelForAcsMif()-----");
		FileExportExportEntity fileExportExportEntity = null;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Missing mandatory parameters, evenCode, startDate, endDate");
			return gson.toJson(fileExportExportEntity);
		}
		RpsEvent rpsEvent = rpsEventService.getRpsEventPerEventCode(eventCode);
		List<CandidateDetails> candidateDetailsList = null;
		if (qpId == 0)
			candidateDetailsList = getCandidateInfoByDateAssessmentBatchAcs(startDate, endDate, assessmentCode, acsId,
					bId, start, end, orderType, column);
		else
			candidateDetailsList = findRpsCandidateInfoByAssessmentDateQpIdsByLimit(startDate, endDate, assessmentCode,
					qpId, start, end, orderType, column);
		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		return getRawScoreReport(eventCode, assessmentCode, qpId, sectionLevelCutOffFromUI, rpsEvent,
				candidateDetailsList);
	}

	/**
	 * Export score report to excel for Unique Candidates.
	 *
	 * @param eventCode
	 *            the event code
	 * @param uniqueCandidateIds
	 * @param assessmentCode
	 * @param qpId
	 * @param sectionLevelCutOffFromUI
	 * @return the String Json
	 */
	@Transactional
	public String exportScoreReportToExcelForUniqueCandidates(String eventCode, Set<Integer> uniqueCandidateIds,
			String assessmentCode, Integer qpId, Map<String, Long> sectionLevelCutOffFromUI) {

		logger.debug("---IN--exportScoreReportToExcelForUniqueCandidates()-----");
		FileExportExportEntity fileExportExportEntity = null;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || uniqueCandidateIds == null || uniqueCandidateIds.isEmpty()
				|| assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, uniqueCandidateIds, assessmentCode");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Missing mandatory parameters, evenCode, uniqueCandidateIds");
			return gson.toJson(fileExportExportEntity);
		}
		RpsEvent rpsEvent = rpsEventService.getRpsEventPerEventCode(eventCode);
		List<CandidateDetails> candidateDetailsList = rpsMasterAssociationService
				.findRpsCandidateInfoByUniqueCandidateID(uniqueCandidateIds);
		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}
		return getRawScoreReport(eventCode, assessmentCode, qpId, sectionLevelCutOffFromUI, rpsEvent,
				candidateDetailsList);
	}

	private String getRawScoreReport(String eventCode, String assessmentCode, Integer qpId,
			Map<String, Long> sectionLevelCutOffFromUI, RpsEvent rpsEvent,
			List<CandidateDetails> rpsCandidateDetailsList) {
		FileExportExportEntity fileExportExportEntity;
		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new HashMap<>();
		for (CandidateDetails candidateDetail : rpsCandidateDetailsList)
			uniqueIdToMasterAssnMap.put(candidateDetail.getUniqueCandidateId(), candidateDetail);
		rpsCandidateDetailsList = null;
		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate responses have been received in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		List<ScoreReportEntity> scoreReportEntitiesList = new ArrayList<>();
		Map<String, List<String>> qtypeMap = null;
		RpsQuestionPaper rpPaper = null;
		Map<String, List<String>> qtypeSectionMap = null;
		if (qpId != null && qpId != 0) {
			qtypeMap = rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpID(qpId);
			rpPaper = rpsQuestionPaperService.findOne(qpId);
		} else {
			rpPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);
			qtypeMap = rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpID(rpPaper.getQpId());
		}

		// for testing don't uncomment this below line
		// addSectionLevelCutOffForTesting(rpPaper, assessmentCode);

		qtypeSectionMap = rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());

		RpsBrLr brLr = rpsQpTemplateService.getRpsBrLrByAssessment(assessmentCode);
		if (brLr == null || brLr.getBrRules() == null) {
			logger.error("No BR Rules exists");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "No BR Rules exists");
			return gson.toJson(fileExportExportEntity);
		}
		String brRules = brLr.getBrRules();
		int maxForeignLanguageCount = getForeignLanguageCount(brLr);
		Map<String, Integer> maxSectionsCountMap = scoreReportUtility.getScoreReportGridData(
				rpsCandidateResponseLitesList, uniqueIdToMasterAssnMap, scoreReportEntitiesList, gradingSchemeDos,
				sectionWeightageCutOffEntityMap, qtypeSectionMap, sectionLevelCutOffFromUI, true);
		int maxSectionsCount = maxSectionsCountMap.get(MAX_SECTION_COUNT).intValue();
		int maxWetCount = maxSectionsCountMap.get(WET_QUESTION_COUNT).intValue();
		if (scoreReportEntitiesList == null || scoreReportEntitiesList.isEmpty()) {
			logger.error("There is no candidate data available in Score Report");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"There is no candidate data available in Score Report");
			return gson.toJson(fileExportExportEntity);
		} else {
			uniqueIdToMasterAssnMap = null;
			rpsCandidateResponseLitesList = null;
		}
		Iterator<ScoreReportEntity> sIterator = scoreReportEntitiesList.iterator();
		List<CandidateSectionScoreData> cList = null;
		while (sIterator.hasNext()) {
			ScoreReportEntity sReportEntity = sIterator.next();
			cList = sReportEntity.getCandidateSectionScoreDataList();
			if (maxSectionsCount == cList.size()) {
				break;
			}
		}

		List<String> sectionItentifierList = new ArrayList<>();
		Iterator<CandidateSectionScoreData> cIterator = cList.iterator();
		while (cIterator.hasNext()) {
			CandidateSectionScoreData cData = cIterator.next();
			sectionItentifierList.add(cData.getSectionTitle());
		}

		String masterGroupName = null;
		List<RpsQpSection> sectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
		List<String> sectionIdsFromDB = new ArrayList<>();
		for (RpsQpSection rpsQpSection : sectionsList) {
			sectionIdsFromDB.add(rpsQpSection.getSecIdentifier());
		}

		// get section group list for excel header display dynemically
		List<String> groupNameList = new ArrayList<>();
		if (sectionGroupEnabled && sectionWeightageCutOffEntityMap != null
				&& !sectionWeightageCutOffEntityMap.isEmpty()) {
			for (Map.Entry<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntityMapValues : sectionWeightageCutOffEntityMap
					.entrySet()) {
				if (sectionWeightageCutOffEntityMapValues.getValue() != null
						&& !sectionWeightageCutOffEntityMapValues.getValue().isEmpty()) {
					groupNameList.add(sectionWeightageCutOffEntityMapValues.getKey());
					List<SectionWeightageCutOffEntity> sectionsCutOff =
							sectionWeightageCutOffEntityMapValues.getValue();
					List<String> sectionsCutOffIds = new ArrayList<>();
					for (SectionWeightageCutOffEntity sectionWeightageCutOffEntity : sectionsCutOff) {
						sectionsCutOffIds.add(sectionWeightageCutOffEntity.getSectionId());
					}
					if (sectionIdsFromDB.containsAll(sectionsCutOffIds)) {
						masterGroupName = sectionWeightageCutOffEntityMapValues.getKey();

					}
				}
			}
		}
		//
		getSectionWeightageEntity(assessmentCode);
		//
		List<com.merittrac.apollo.excel.ScoreReportEntity> commonScoreReportEntitiesList = new ArrayList<>();

		for (ScoreReportEntity scoreReportEntity : scoreReportEntitiesList) {
			List<com.merittrac.apollo.excel.CandidateSectionScoreData> cScoreDatas = new ArrayList<>();
			com.merittrac.apollo.excel.ScoreReportEntity scoreReportEntity2 = new com.merittrac.apollo.excel.ScoreReportEntity();
			try {
				BeanUtils.copyProperties(scoreReportEntity2, scoreReportEntity);
				//
				Map<String, GroupSectionsWeightageScore> groupWeightageMap = new LinkedHashMap<>();
				if (masterGroupName != null && sectionWeightageCutOffEntityMap != null
						&& !sectionWeightageCutOffEntityMap.isEmpty()
						&& groupWeightageCutOffEntityMap != null && !groupWeightageCutOffEntityMap.isEmpty()) {
					groupWeightageMap = this.calculateSectionWeightage(sectionWeightageCutOffEntityMap, assessmentCode,
							scoreReportEntity.getUniqueCandidateId());

					applyGroupCutOffForAllSectionsInaAssessment(groupWeightageMap, masterGroupName, scoreReportEntity2);

				}
				//
				List<CandidateSectionScoreData> canList = scoreReportEntity.getCandidateSectionScoreDataList();
				for (CandidateSectionScoreData canSectionScoreData : canList) {
					com.merittrac.apollo.excel.CandidateSectionScoreData canScoreData = new com.merittrac.apollo.excel.CandidateSectionScoreData();
					/**
					 * int noOfTypedWords, int noOfTotalWords, int correctWord,
					 * int errorWord, double grossSpeed, double netSpeed, int
					 * sectionDuration, int WPM, double accuracy
					 */

					if (canSectionScoreData.getTypingTestEntity() != null) {
						canScoreData.setTypingTestEntity(
								new TypingTestEntity(canSectionScoreData.getTypingTestEntity().getNoOfTypedWords(),
										canSectionScoreData.getTypingTestEntity().getNoOfTotalWords(),
										canSectionScoreData.getTypingTestEntity().getCorrectWord(),
										canSectionScoreData.getTypingTestEntity().getErrorWord(),
										canSectionScoreData.getTypingTestEntity().getGrossSpeed(),
										canSectionScoreData.getTypingTestEntity().getNetSpeed(),
										canSectionScoreData.getTypingTestEntity().getSectionDuration(),
										canSectionScoreData.getTypingTestEntity().getWPM(),
										canSectionScoreData.getTypingTestEntity().getAccuracy()));
					} else if (qtypeSectionMap.containsKey(canSectionScoreData.getSectionTitle())) {
						if (qtypeSectionMap.get(canSectionScoreData.getSectionTitle())
								.contains(QuestionType.TYPING_TEST.toString())) {
							canScoreData.setTypingTestEntity(new TypingTestEntity(0, 0, 0, 0, 0, 0, 0, 0, 0));
						}
					}
					if (canSectionScoreData.getWetDetails() != null) {
						canScoreData.setWetQuestLevelInfoEntityList(canSectionScoreData.getWetDetails());
					}
					canScoreData.setCandidateScore(canSectionScoreData.getCandidateScore());
					canScoreData.setSectionId(canSectionScoreData.getSectionTitle());
					canScoreData.setScorePercentage(canSectionScoreData.getScorePercentage());
					canScoreData.setSectionName(canSectionScoreData.getSectionTitle());
					canScoreData.setSectionIdentifier(canSectionScoreData.getSectionIdentifier());
					canScoreData
							.setSectionTotalQuestionCount(canSectionScoreData.getSectionTotalQuestionCount());
					canScoreData
							.setSectionAttemptedQuestionCount(canSectionScoreData.getSectionAttemptedQuestionCount());
					canScoreData.setSectionCorrectQuestionsCount(canSectionScoreData.getSectionCorrectQuestionsCount());
					canScoreData.setSectionMaxScore(canSectionScoreData.getSectionMaxScore() == null ? "0"
							: (decimalFormat.format(canSectionScoreData.getSectionMaxScore())));

					cScoreDatas.add(canScoreData);
				}

			} catch (IllegalAccessException e) {
				e.printStackTrace();
				logger.error("ERROR :: IllegalAccessException ---" + e);
			} catch (InvocationTargetException e) {
				e.printStackTrace();
				logger.error("ERROR :: InvocationTargetException ---" + e);
			}
			scoreReportEntity2.setCandidateSectionScoreDataList(cScoreDatas);
			scoreReportEntity2.setGrade(scoreReportEntity.getGrade());

			scoreReportEntity2.setGroupWeightageMap(scoreReportEntity.getGroupWeightageMap());
			// best score rule
			if (isEnabledBestScoresRule && listOfAssessmentsForBestScoreRule != null
					&& !listOfAssessmentsForBestScoreRule.isEmpty()) {
				for (String assessment : listOfAssessmentsForBestScoreRule) {
					if (assessment.equalsIgnoreCase(assessmentCode)) {
						applyBestScoreRuleOnScores(scoreReportEntity2);
					}
				}

			}
			commonScoreReportEntitiesList.add(scoreReportEntity2);
		}

		String exportToFolder = userHome + File.separator + ExcelConstants.SCORE_REPORT_FOLDER;
		File path = new File(exportToFolder);
		if (!path.isDirectory()) {
			if (path.mkdirs())
				logger.debug("path created for dir:-" + path);
			else
				logger.debug("Unable to Create Directoty for Path:-" + path);
		}

		path = new File(path + File.separator + ExcelConstants.SCORE_REPORT_SHEET_NAME + System.currentTimeMillis()
				+ ExcelConstants.XLSX);
		logger.debug("path is " + path);
		// common api(RPS and ACS) export score report in excel format

		try {
			com.merittrac.apollo.excel.FileExportExportEntity commonFileExportExportEntity = excelModel
					.exportScoreReportGridDataForAcsMif(commonScoreReportEntitiesList, rpsEvent, eventCode,
							maxSectionsCount, maxWetCount, sectionItentifierList, path, gradingEnabled,
							sectionGroupEnabled, groupNameList, qtypeMap,
							getReportSectionCutOff(sectionLevelCutOffFromUI), maxForeignLanguageCount, brRules,
							isEnabledBestScoresRule);

			return gson.toJson(commonFileExportExportEntity);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| IntrospectionException e) {
			e.printStackTrace();
			logger.error(
					"ERROR :: IllegalAccessException | IllegalArgumentException | InvocationTargetException | IntrospectionException e ---"
							+ e);
		}

		logger.error("Error Occured");
		logger.debug("---OUT--exportScoreReportToExcel()-----");
		fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Error Occured");
		return gson.toJson(fileExportExportEntity);
	}



	private void applyBestScoreRuleOnScores(com.merittrac.apollo.excel.ScoreReportEntity scoreReportEntity) {
		// section identification based on token from config file
		// and best scores will have scores, others NA
		List<com.merittrac.apollo.excel.CandidateSectionScoreData> sectionScores =
				scoreReportEntity.getCandidateSectionScoreDataList();

		List<com.merittrac.apollo.excel.CandidateSectionScoreData> partASections =
				new ArrayList<com.merittrac.apollo.excel.CandidateSectionScoreData>(4);
		List<com.merittrac.apollo.excel.CandidateSectionScoreData> partBSections =
				new ArrayList<com.merittrac.apollo.excel.CandidateSectionScoreData>(4);
		for (com.merittrac.apollo.excel.CandidateSectionScoreData candidateSectionScoreData : sectionScores) {
			if (candidateSectionScoreData.getSectionName().startsWith(sectionPartAIdentification)) {
				partASections.add(candidateSectionScoreData);
			} else if (candidateSectionScoreData.getSectionName().startsWith(sectionPartBIdentification)) {
				partBSections.add(candidateSectionScoreData);
			}
		}
		// release memory
		sectionScores = null;
		// sort on part A and B separately
		double partATotalScores =
				this.getSortedSections(partASections, sectionPartAIdentification, sectionCountForBestScoresPartA);
		double partBTotalScores =
				this.getSortedSections(partBSections, sectionPartBIdentification, sectionCountForBestScoresPartB);
		sectionScores = new ArrayList<com.merittrac.apollo.excel.CandidateSectionScoreData>(8);
		scoreReportEntity.setPartAScores(partATotalScores);
		scoreReportEntity.setPartBScores(partBTotalScores);
		sectionScores.addAll(partASections);
		sectionScores.addAll(partBSections);

		updateOverallScoreDetailsBasedOnRule(scoreReportEntity, sectionScores);
	}

	private void updateOverallScoreDetailsBasedOnRule(com.merittrac.apollo.excel.ScoreReportEntity scoreReportEntity,
			List<com.merittrac.apollo.excel.CandidateSectionScoreData> sectionScores) {
		int totalQuestions = 0;
		int attemptedCount = 0;
		int correctCount = 0;
		double totalMarks = 0;
		double candidateScore = 0;
		for (com.merittrac.apollo.excel.CandidateSectionScoreData candidateSectionScoreData : sectionScores) {
			Integer sectionMaxQuestionCount = candidateSectionScoreData.getSectionTotalQuestionCount();
			if (sectionMaxQuestionCount != null && sectionMaxQuestionCount != 0) {
				totalQuestions += (sectionMaxQuestionCount);
			}
			String sectionAttemptedQuestionCount = candidateSectionScoreData.getSectionAttemptedQuestionCount();
			if (sectionAttemptedQuestionCount != null && sectionAttemptedQuestionCount != RpsConstants.NA
					&& sectionAttemptedQuestionCount != "0") {
				attemptedCount += Double.parseDouble(sectionAttemptedQuestionCount);
			}
			String sectionCorrectQuestionsCount = candidateSectionScoreData.getSectionCorrectQuestionsCount();
			if (sectionCorrectQuestionsCount != null && sectionCorrectQuestionsCount != RpsConstants.NA
					&& sectionCorrectQuestionsCount != "0") {
				correctCount += Double.parseDouble(sectionCorrectQuestionsCount);
			}
			String sectionMaxScore = candidateSectionScoreData.getSectionMaxScore();
			if (sectionMaxScore != null && sectionMaxScore != RpsConstants.NA && sectionMaxScore != "0") {
				totalMarks += Double.parseDouble(sectionMaxScore);
			}
			String candSectionScore = candidateSectionScoreData.getCandidateScore();
			if (candSectionScore != null && candSectionScore != RpsConstants.NA && candSectionScore != "0"
					&& candSectionScore != "0.00") {
				candidateScore += Double.parseDouble(candSectionScore);
			}
		}
		scoreReportEntity.setTotalQuestions(totalQuestions);
		scoreReportEntity.setAttemptedQuestionsCount(attemptedCount);
		scoreReportEntity.setCorrectQuestionsCount(correctCount);
		scoreReportEntity.setTotalScore(decimalFormat.format(candidateScore));
		scoreReportEntity.setScorePercentage(sharedMethodsUtility.calculatePercentage(candidateScore, totalMarks));
	}

	private double getSortedSections(List<com.merittrac.apollo.excel.CandidateSectionScoreData> sections,
			final String partIdentity, final int cutoffSectionCount) {
		sortSections(sections, partIdentity);
		int i = 0;
		double partTotalScore = 0;
		for (com.merittrac.apollo.excel.CandidateSectionScoreData candidateSectionScoreData : sections) {
			if (i >= cutoffSectionCount) {
				candidateSectionScoreData.setCandidateScore(RpsConstants.NA);
				candidateSectionScoreData.setScorePercentage(RpsConstants.NA);
				candidateSectionScoreData.setSectionAttemptedQuestionCount(RpsConstants.NA);
				candidateSectionScoreData.setSectionCorrectQuestionsCount(RpsConstants.NA);
				candidateSectionScoreData.setSectionTotalQuestionCount(0);
				candidateSectionScoreData.setSectionMaxScore(RpsConstants.NA);
			} else {
				partTotalScore += Double.parseDouble(candidateSectionScoreData.getCandidateScore());
			}
			i++;
		}
		return partTotalScore;
	}

	private void sortSections(List<com.merittrac.apollo.excel.CandidateSectionScoreData> sections,
			final String partIdentity) {
		Collections.sort(sections, new Comparator<com.merittrac.apollo.excel.CandidateSectionScoreData>() {
			public int compare(com.merittrac.apollo.excel.CandidateSectionScoreData o1,
					com.merittrac.apollo.excel.CandidateSectionScoreData o2) {
				String sec1Score = o1.getCandidateScore();
				String sec2Score = o2.getCandidateScore();
				Double i1 = Double.parseDouble(sec1Score);
				Double i2 = Double.parseDouble(sec2Score);
				if (i1 < i2) {
					return 1;
				} else if (i1 > i2) {
					return -1;
				} else if (i1.doubleValue() == i2.doubleValue()) {
					// removing "Part A" text for sort
					String sec1Name = o1.getSectionName();
					String sec2Name = o2.getSectionName();
					sec1Name = sec1Name.substring(partIdentity.length());
					sec2Name = sec2Name.substring(partIdentity.length());
					if (getIndexSortOnName(sec1Name) < getIndexSortOnName(sec2Name)) {
						return -1;
					} else if (getIndexSortOnName(sec1Name) > getIndexSortOnName(sec2Name)) {
						return 1;
					} else
						return 0;
				} else
					// return (i1 < i2 ? -1 : (i1 == i2 ? 0 : 1));
					return 0;
			}
		});
	}

	private int getIndexSortOnName(String secName) {
		// String subjects = "PCMB"; // Arrays.asList("P", "C", "M", "B");
		Integer index = sortSubjectsOnTieBreaking.indexOf(secName.charAt(0));
		return index == null ? -1 : index;
	}

	public HashMap<String, Object> parseJSONtoMap(String json) {
		JsonObject object = (JsonObject) new com.google.gson.JsonParser().parse(json);
		Set<Map.Entry<String, JsonElement>> set = object.entrySet();
		Iterator<Map.Entry<String, JsonElement>> iterator = set.iterator();

		HashMap<String, Object> map = new HashMap<String, Object>();

		while (iterator.hasNext()) {
			Map.Entry<String, JsonElement> entry = iterator.next();

			Object value = new Gson().fromJson(entry.getValue(), Object.class);

			if (value != null) {
				map.put(entry.getKey(), value);
			}
		}
		return map;
	}

	public int getforeignLangCountFrombrulesJson(String json) {
		// convert the business rules json into map
		// Map<String, Object> map = parseJSONtoMap(json);
		// // iterate over the map and find the values for the required
		// properties
		// String propertyName = RpsConstants.QUALIFICATION_INFO_FROM_BRULES;
		// if (map.containsKey(propertyName)) {
		// String rules = (String) map.get(propertyName);
		// if (rules != null) {
		// Type rulesList = new TypeToken<List<MIFFieldBean>>() {
		// }.getType();
		// List<MIFFieldBean> beanList = new Gson().fromJson(rules, rulesList);
		// if (beanList != null && !beanList.isEmpty()) {
		// for (MIFFieldBean mifFieldBean : beanList) {
		// if (mifFieldBean.getFieldName().equalsIgnoreCase("foreign_language"))
		// {
		// return Integer.parseInt(mifFieldBean.getDescription());
		// }
		// }
		// }
		// }
		// }
		return 0;
	}

	private int getForeignLanguageCount(RpsBrLr rpsBrLr) {
		if (rpsBrLr != null && rpsBrLr.getBrRules() != null) {
			String brRules = rpsBrLr.getBrRules();
			return getforeignLangCountFrombrulesJson(brRules);
		}
		return 0;
	}

	private ReportSectionWeightageCutOffEntity getReportSectionCutOff(Map<String, Long> sectionLevelCutOffFromUI) {
		String suggestedCriteria = "";
		String usedCriteria = "";
		boolean isMatchedCriteria = true;
		if (sectionWeightageCutOffEntityMap != null) {
			for (Map.Entry<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntityMaps : sectionWeightageCutOffEntityMap
					.entrySet()) {
				for (SectionWeightageCutOffEntity sectionWeightageCutOffEntity : sectionWeightageCutOffEntityMaps
						.getValue()) {
					Integer valueFromUI = new Integer(
							sectionLevelCutOffFromUI.get(sectionWeightageCutOffEntity.getSectionName()).intValue());
					Integer cutOff = new Integer(sectionWeightageCutOffEntity.getCutOff().intValue());// (int)Math.ceil
					if (valueFromUI.intValue() != cutOff.intValue()) {
						isMatchedCriteria = false;
						usedCriteria = usedCriteria + "," + valueFromUI;
						suggestedCriteria = suggestedCriteria + "," + cutOff;
					}
				}
			}

		} else {
			isMatchedCriteria = false;
			for (Map.Entry<String, Long> sectionLevelCutOffFromUIMap : sectionLevelCutOffFromUI.entrySet()) {
				suggestedCriteria = suggestedCriteria + "," + RpsConstants.NA;
				usedCriteria = usedCriteria + "," + sectionLevelCutOffFromUIMap.getValue();
			}
		}
		if (!suggestedCriteria.isEmpty()) {
			suggestedCriteria = suggestedCriteria.substring(1);
		}
		if (!usedCriteria.isEmpty()) {
			usedCriteria = usedCriteria.substring(1);
		}
		return new ReportSectionWeightageCutOffEntity(suggestedCriteria, usedCriteria, isMatchedCriteria);
	}

	/**
	 * Export response report to excel.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the string
	 */
	public String exportResponseReportToExcel(String eventCode, String startDate, String endDate, String assessmentCode,
			Integer qpId, Integer acsId, Integer bId) {
		logger.debug("---IN--exportResponseReportToExcel()-----");
		FileExportExportEntity fileExportExportEntity = null;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Missing mandatory parameters, evenCode, startDate, endDate");
			return gson.toJson(fileExportExportEntity);
		}

		List<Integer> batchAcsIdList = getBatchAcsAssonLiteOnAcsAndBatchIds(eventCode, acsId, bId);
		// List<Integer> batchAcsIdList =
		// rpsBatchAcsAssociationService.findAllBatchAcsAssonLiteByEventCode(eventCode);
		if (batchAcsIdList == null || batchAcsIdList.isEmpty()) {
			logger.error("There are no batches scheduled for open event");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"There are no batches scheduled for open event");
			return gson.toJson(fileExportExportEntity);
		}

		List<CandidateDetails> candidateDetailsList = rpsMasterAssociationService
				.findRpsMasterAssociationByAssessmentCodeBatchAcsIdAndTime(assessmentCode,
						new HashSet<Integer>(batchAcsIdList) {
						}, startDate, endDate);
		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}
		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new HashMap<>();
		for (CandidateDetails candidateDetails : candidateDetailsList)
			uniqueIdToMasterAssnMap.put(candidateDetails.getUniqueCandidateId(), candidateDetails);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		ResponseReportWrapper responseReportWrapper;
		responseReportWrapper = responseReportUtility.getResponseReportGridInformation(rpsCandidateResponseLitesList,
				uniqueIdToMasterAssnMap, false);
		if (responseReportWrapper.getResponseReportEntitiesList() == null
				|| responseReportWrapper.getResponseReportEntitiesList().isEmpty()) {
			logger.error("No candidate responses are available to export in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate responses are available to export in selected date range");
			return gson.toJson(fileExportExportEntity);
		}
		fileExportExportEntity = exportToExcelUtility.exportResponseReportGridData(responseReportWrapper);
		logger.debug("---OUT--exportResponseReportToExcel()-----");
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * Export response report to excel.
	 *
	 * @param eventCode
	 *            the event code
	 * @param startDate
	 *            the start date
	 * @param endDate
	 *            the end date
	 * @return the string
	 */
	public String exportAceTracReportToExcel(String eventCode, String startDate, String endDate, String assessmentCode) {
		logger.debug("---IN--exportResponseReportToExcel()-----");
		FileExportExportEntity fileExportExportEntity = null;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Missing mandatory parameters, evenCode, startDate, endDate");
			return gson.toJson(fileExportExportEntity);
		}

		List<Integer> batchAcsIdList = getBatchAcsAssonLite(eventCode);
		if (batchAcsIdList == null || batchAcsIdList.isEmpty()) {
			logger.error("There are no batches scheduled for open event");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"There are no batches scheduled for open event");
			return gson.toJson(fileExportExportEntity);
		}

		Map<String, Map<String, String>> assessmentToSectionIdAndSectionNameMap = new HashMap<String, Map<String, String>>();
		List<CandidateDetails> candidateDetailsList = null;
		if (assessmentCode == null || assessmentCode.isEmpty()) {
			candidateDetailsList = rpsMasterAssociationService
					.findRpsMasterAssociationByBatchAcsIdAndTimeForAceTracReport(new HashSet<Integer>(batchAcsIdList),
							startDate, endDate);

			List<RpsAssessment> assessments = rpsAssessmentRepository.getAssessmentsForEvent(eventCode); 
			for (RpsAssessment assessment : assessments) {
				List<RpsQpSection> rpsQpSectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessment.getAssessmentCode());
				Map<String, String> sectionTitleAndSectionNameMap = new HashMap<String, String>();
				if (rpsQpSectionsList!= null) {
					for (RpsQpSection rpsQpSections : rpsQpSectionsList)
						sectionTitleAndSectionNameMap.put(rpsQpSections.getSecIdentifier(), rpsQpSections.getTitle());
					assessmentToSectionIdAndSectionNameMap.put(assessment.getAssessmentCode(), sectionTitleAndSectionNameMap);
				}
			}
		}
		else {
			candidateDetailsList = rpsMasterAssociationService
					.findRpsMasterAssociationByAssessmentCodeBatchAcsIdAndTimeForAceTracReport(assessmentCode,
							new HashSet<Integer>(batchAcsIdList), startDate, endDate);
			List<RpsQpSection> rpsQpSectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
			Map<String, String> sectionTitleAndSectionNameMap = new HashMap<String, String>();
			if (rpsQpSectionsList!= null) 
				for (RpsQpSection rpsQpSections : rpsQpSectionsList)
					sectionTitleAndSectionNameMap.put(rpsQpSections.getSecIdentifier(), rpsQpSections.getTitle());
			assessmentToSectionIdAndSectionNameMap.put(assessmentCode, sectionTitleAndSectionNameMap);
		}

		if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate have taken exam for open event in selected date range");
			return gson.toJson(fileExportExportEntity);
		}

		Map<Integer, CandidateDetails> uniqueIdToMasterAssnMap = new HashMap<>();
		for (CandidateDetails candidateDetails : candidateDetailsList)
			uniqueIdToMasterAssnMap.put(candidateDetails.getUniqueCandidateId(), candidateDetails);

		ResponseReportWrapper responseReportWrapper = responseReportUtility.getResponseReportGridInformationForAceTracReport(
				uniqueIdToMasterAssnMap);
		responseReportWrapper.setAssessmentSectionIdtoSectionNameMap(assessmentToSectionIdAndSectionNameMap);

		if (responseReportWrapper.getResponseReportEntitiesList() == null
				|| responseReportWrapper.getResponseReportEntitiesList().isEmpty()) {
			logger.error("No candidate responses are available to export in selected date range");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"No candidate responses are available to export in selected date range");
			return gson.toJson(fileExportExportEntity);
		}
		fileExportExportEntity = exportToExcelUtility.exportResponseReportGridDataForAceTracReport(responseReportWrapper);
		logger.debug("---OUT--exportResponseReportToExcel()-----");
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param uniqueCandidateIds
	 * @return
	 */
	public String generatePdfForScoreReport(String assessmentCode, Set<Integer> uniqueCandidateIds, Boolean isFromExcelUpload,
			List<CandidateDetails> candidateDetails) {
		logger.debug("---IN--generatePdfForScoreReport()-----");
		FileExportExportEntity fileExportExportEntity = null;
		if (!isFromExcelUpload) {
			candidateDetails = rpsMasterAssociationService.findRpsCandidateInfoByUniqueCandidateID(uniqueCandidateIds);
		} else {
			if (candidateDetails == null || candidateDetails.isEmpty()) {
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF as No candidates present in Excel");
				logger.error("ERROR: IOException -- ");
				return gson.toJson(fileExportExportEntity);
			}
		}
		String eventCode = null;
		String rpsCustomerName = null;

		List<CandidateScoreCard> candidateScoreCardList = new ArrayList<>();
		for (CandidateDetails candidateInfo : candidateDetails) {
			eventCode = candidateInfo.getEventCode();
			if (rpsCustomerName == null)
				rpsCustomerName = rpsEventService.findCustomersByEventCode(eventCode);
			RpsCandidateMIFDetails rpsCandidateMIFDetails = rpsCandidateMIFDetailsService
					.findByUniqueCandidateId(candidateInfo.getUniqueCandidateId());
			MIFForm mifForm = null;
			if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
				mifForm = gson.fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);

			// create Bean For CandidateDetails
			CandidateScoreCard candidateScoreCard =
					createBeanForCandidateDetails(assessmentCode, rpsCustomerName, candidateInfo);

			// populate MIFData For PDF Report
			populateMIFDataForPDFReport(mifForm, candidateScoreCard);

			// for section
			List<AptitudeTestScore> aptitudeTestScores = new ArrayList<>();
			List<CandidateSectionScoreDetails> candidateSectionScoreDataList =
					new ArrayList<CandidateSectionScoreDetails>();
			RpsCandidateResponse rpsCandidateResponse = rpsCandidateResponseService
					.findByUniqueCandidateId(candidateInfo.getUniqueCandidateId());
			if (rpsCandidateResponse == null) {
				logger.info("No Candidate Response");
				continue;
			}

			RpsWetScoreEvaluation rpScoreEvaluation = this.getQuesIdFromCandResponseByQtype(
					candidateInfo.getUniqueCandidateId(), rpsCandidateResponse.getResponse());
			if (rpScoreEvaluation != null) {
				candidateScoreCard.setWetScore(rpScoreEvaluation.getEvaluatedParmsScoreJson());
			}

			List<RpsSectionCandidateResponse> rpsSectionCandidateResponsesList = rpsSectionCandidateResponseService
					.findAllSecCandidateRespByCandRespId(rpsCandidateResponse.getCandidateResponseId());

			if (rQpSectionMap != null && !this.rQpSectionMap.containsKey(assessmentCode))
				this.populateQpSectionMap(assessmentCode);

			List<RpsQpSection> qpSectionList = this.rQpSectionMap.get(assessmentCode);
			Double candTotalScore = 0.0;
			Double totalAssessmentMarks = 0.0;
			if (qpSectionList != null && !qpSectionList.isEmpty()) {
				for (RpsQpSection currentQqpSection : qpSectionList) {
					AptitudeTestScore aptitudeTestScore = new AptitudeTestScore();
					CandidateSectionScoreDetails scoreData =
							new CandidateSectionScoreDetails(String.valueOf(currentQqpSection.getQpSecId()),
									currentQqpSection.getTitle(), String.valueOf(currentQqpSection.getSecScore()));
					totalAssessmentMarks += currentQqpSection.getSecScore();
					String sectionWithMaxMarks = currentQqpSection.getTitle()
							.concat("(Out of " + currentQqpSection.getSecScore() + " Marks)");
					aptitudeTestScore.setName(sectionWithMaxMarks);
					Double candSecScore = 0.0;
					if (rpsSectionCandidateResponsesList != null && !rpsSectionCandidateResponsesList.isEmpty()) {
						for (RpsSectionCandidateResponse rpsSectionCandidateResponse : rpsSectionCandidateResponsesList) {
							if (rpsSectionCandidateResponse.getSecIdentifier()
									.equals(currentQqpSection.getSecIdentifier())) {
								candSecScore = rpsSectionCandidateResponse.getScore();
							}
						}
					}
					candTotalScore += candSecScore;
					scoreData.setCandidateScore(Double.toString(candSecScore));
					aptitudeTestScore.setScore(Double.toString(candSecScore));
					aptitudeTestScores.add(aptitudeTestScore);
					candidateSectionScoreDataList.add(scoreData);
				}
			}
			candidateScoreCard.setTotalAssessmentMarks(Double.toString(totalAssessmentMarks));
			candidateScoreCard.setTotalScore(Double.toString(candTotalScore));
			candidateScoreCard.setAptitudeTestScores(aptitudeTestScores);
			candidateScoreCard.setCandidateSectionScoreDataList(candidateSectionScoreDataList);

			candidateScoreCardList.add(candidateScoreCard);
		}

		// Generate PDF On Report Type
		fileExportExportEntity = createFileEntityAndGeneratePDFOnReportType(eventCode, candidateScoreCardList);

		logger.debug("---OUT--generatePdfForScoreReport()-----");
		return gson.toJson(fileExportExportEntity);
	}

	private FileExportExportEntity createFileEntityAndGeneratePDFOnReportType(String eventCode,
			List<CandidateScoreCard> candidateScoreCardList) {
		FileExportExportEntity fileExportExportEntity;
		File path = formFilePathForPdfExport();

		try {
			String scoreCardFormatType = rpsEventService.findScoreCardFormatTypeByEventCode(eventCode);
			if (scoreCardFormatType != null) {
				switch (scoreCardFormatType) {
					case "STANDARD_REPORT":
						CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(path.toString(),
								candidateScoreCardList);
						break;
					case "COGNIZANT":
						CandidateScoreCardGenerator.pdfGenerator(path.toString(), candidateScoreCardList);
						break;

					default:
						CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(path.toString(),
								candidateScoreCardList);
						break;
				}
			} else {
				CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(path.toString(),
						candidateScoreCardList);
			}
		} catch (DocumentException e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			logger.error("ERROR: DocumentException -- " + e);
		} catch (IOException e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			logger.error("ERROR: IOException -- " + e);
		}
		fileExportExportEntity =
				new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, path.toString(), "PDF generation has success");
		return fileExportExportEntity;
	}

	private CandidateScoreCard createBeanForCandidateDetails(String assessmentCode, String rpsCustomerName,
			CandidateDetails candidateInfo) {

		CandidateScoreCard candidateScoreCard = new CandidateScoreCard();
		String candidateId1 = candidateInfo.getCandidateId1();
		String convertedDate = " ";
		if (candidateInfo.getTestStartTime() != null) {
			Calendar dateInCalendar = TimeUtil.convertStringToCalender(candidateInfo.getTestStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			convertedDate = TimeUtil.convertTimeAsString(dateInCalendar.getTimeInMillis(), "dd-MMM-yyyy");
		}
		candidateScoreCard.setTestTakenDate(convertedDate);

		candidateScoreCard.setLoginId(candidateInfo.getLoginId());
		if (candidateId1 != null) {
			candidateScoreCard.setMtId(candidateId1);
		} else {
			candidateScoreCard.setMtId(RpsConstants.NA);
		}

		if (customerLogoPathMapOnAssCode.containsKey(assessmentCode)) {
			String customerLogoPath = customerLogoPathMapOnAssCode.get(assessmentCode);
			candidateScoreCard.setLogoPath(customerLogoPath);
		} else {
			candidateScoreCard.setLogoPath("");
		}


		if (rpsCustomerName != null)
			candidateScoreCard.setCustomerName(rpsCustomerName);
		else
			candidateScoreCard.setCustomerName(RpsConstants.NA);

		candidateScoreCard.setCreationDate(Calendar.getInstance().getTime());
		return candidateScoreCard;
	}

	private File formFilePathForPdfExport() {
		String exportToFolder = userHome + File.separator + ExcelConstants.SCORE_REPORT_FOLDER;
		File path = new File(exportToFolder);
		if (!path.isDirectory()) {
			if (path.mkdirs())
				logger.debug("path created for dir:-" + path);
			else
				logger.debug("Unable to Create Directoty for Path:-" + path);
		}

		logger.debug("Final path created for dir:-" + path + File.separator + ExcelConstants.SCORE_REPORT_SHEET_NAME
				+ System.currentTimeMillis() + ".pdf");
		path = new File(
				path + File.separator + ExcelConstants.SCORE_REPORT_SHEET_NAME + System.currentTimeMillis() + ".pdf");
		if (path.isFile())
			logger.debug("path created for dir:-" + path);
		else
			logger.debug("Unable to Create Directoty for Path:-" + path);
		return path;
	}

	private void populateMIFDataForPDFReport(MIFForm mifForm, CandidateScoreCard candidateScoreCard) {
		if (mifForm != null) {
			PersonalInfo personalInfo = mifForm.getPersonalInfo();
			ContactInfo permanentAddress = mifForm.getPermanentAddress();
			ContactInfo communicationAddress = mifForm.getCommunicationAddress();
			Qualification qualification = mifForm.getQualification();
			List<TrainingDetails> trainingDetailses = mifForm.getTrainingDetails();
			Experience experienceDetails = mifForm.getExperience();

			if (personalInfo != null) {
				StringBuffer candidateName = new StringBuffer("");
				if (personalInfo.getFirstName() != null)
					candidateName.append(personalInfo.getFirstName());
				else
					candidateName.append("");

				if (personalInfo.getMiddleName() != null)
					candidateName.append(" " + personalInfo.getMiddleName());
				else
					candidateName.append("");

				if (personalInfo.getLastName() != null)
					candidateName.append(" " + personalInfo.getLastName());
				else
					candidateName.append("");
				candidateScoreCard.setName(candidateName.toString());
				candidateScoreCard.setCtsId(
						personalInfo.getUniqueIdNumber() == null ? " " : personalInfo.getUniqueIdNumber());
				candidateScoreCard
						.setGender(personalInfo.getGender() != null ? personalInfo.getGender() : RpsConstants.NA);

				if (personalInfo.getDob() != null) {
					SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd");
					try {
						Date date = formatter.parse(personalInfo.getDob());
						candidateScoreCard.setDob(dateFormat.format(date));
					} catch (ParseException e) {
						logger.error("ParseException while executing generatePdfForScoreReport...", e);
					}
				} else {
					candidateScoreCard.setDob("");
				}
				candidateScoreCard.setNationality(
						personalInfo.getNationality() != null ? personalInfo.getNationality() : RpsConstants.NA);
				try {
					int dob = CalendarUtil.calculateAge(personalInfo.getDob());
					candidateScoreCard
							.setAge(personalInfo.getDob() == null ? RpsConstants.NA : Integer.toString(dob));
				} catch (IllegalArgumentException e) {
					candidateScoreCard.setAge(RpsConstants.NA);
				}

			} else {
				candidateScoreCard.setName(RpsConstants.NA);
				candidateScoreCard.setGender(RpsConstants.NA);
				candidateScoreCard.setDob(RpsConstants.NA);
				candidateScoreCard.setNationality(RpsConstants.NA);
				candidateScoreCard.setAge(RpsConstants.NA);
			}

			if (experienceDetails != null && experienceDetails.getWorkedInCompany() != null) {
				candidateScoreCard.setWorkedInCompany(experienceDetails.getWorkedInCompany());
				if (experienceDetails.getWorkedInCompany().contains("Yes")) {
					candidateScoreCard.setWorkedInCompanyTime(experienceDetails.getWorkedInCompanyTime() == null
							? RpsConstants.NA : experienceDetails.getWorkedInCompanyTime());
				} else {
					candidateScoreCard.setWorkedInCompanyTime(RpsConstants.NA);
				}
			} else {
				candidateScoreCard.setWorkedInCompany(RpsConstants.NA);
				candidateScoreCard.setWorkedInCompanyTime(RpsConstants.NA);
			}

			if (trainingDetailses != null && !trainingDetailses.isEmpty()) {
				// certification details
				TrainingDetails trainingDetails = trainingDetailses.get(0);
				candidateScoreCard.setCertificationDuration(
						trainingDetails.getDuration() == null ? "NA" : trainingDetails.getDuration().toString());
				candidateScoreCard.setCertifiedSkill(
						trainingDetails.getProgramName() == null ? "NA" : trainingDetails.getProgramName());
				candidateScoreCard.setCertifiedCompany(trainingDetails.getInstituteOrganization() == null ? "NA"
						: trainingDetails.getInstituteOrganization());
			} else {
				candidateScoreCard.setCertificationDuration("NA");
				candidateScoreCard.setCertifiedSkill("NA");
				candidateScoreCard.setCertifiedCompany("NA");
			}
			if (permanentAddress != null) {
				candidateScoreCard.setMailId(
						permanentAddress.getEmailId1() != null ? permanentAddress.getEmailId1() : RpsConstants.NA);
				candidateScoreCard.setMobileNo(
						permanentAddress.getMobile() != null ? permanentAddress.getMobile() : RpsConstants.NA);
				// setting full permanent address
				StringBuffer permanentFullAddress = new StringBuffer("");
				if (permanentAddress.getAddress() != null)
					permanentFullAddress.append(permanentAddress.getAddress());
				else
					permanentFullAddress.append("");

				if (permanentAddress.getCity() != null)
					permanentFullAddress.append(" " + permanentAddress.getCity());
				else
					permanentFullAddress.append("");

				if (permanentAddress.getCountryState() != null)
					permanentFullAddress.append(" " + permanentAddress.getCountryState());
				else
					permanentFullAddress.append("");

				if (permanentAddress.getPinCode() != null)
					permanentFullAddress.append(" " + permanentAddress.getPinCode());
				else
					permanentFullAddress.append("");

				candidateScoreCard.setPermanentAddress(permanentFullAddress.toString());
			} else {
				candidateScoreCard.setMailId(RpsConstants.NA);
				candidateScoreCard.setMobileNo(RpsConstants.NA);
				candidateScoreCard.setPermanentAddress(RpsConstants.NA);
			}

			if (communicationAddress != null) {
				// setting full communication address
				StringBuffer communicationFullAddress = new StringBuffer("");
				if (communicationAddress.getAddress() != null)
					communicationFullAddress.append(communicationAddress.getAddress());
				else
					communicationFullAddress.append("");

				if (communicationAddress.getCity() != null)
					communicationFullAddress.append(" " + communicationAddress.getCity());
				else
					communicationFullAddress.append("");

				if (communicationAddress.getCountryState() != null)
					communicationFullAddress.append(" " + communicationAddress.getCountryState());
				else
					communicationFullAddress.append("");

				if (communicationAddress.getPinCode() != null)
					communicationFullAddress.append(" " + communicationAddress.getPinCode());
				else
					communicationFullAddress.append("");

				candidateScoreCard.setCurrentAddress(communicationFullAddress.toString());
			} else
				candidateScoreCard.setCurrentAddress(RpsConstants.NA);

			Map<String, CandidateEducationDetails> cMap = new HashMap<>();
			if (qualification != null) {
				candidateScoreCard.setUnivercityRegNo(qualification.getUniversityRegnNo() != null
						? qualification.getUniversityRegnNo() : RpsConstants.NA);
				Map<String, QualificationDetails> qMap = qualification.getQualificationLevelToDetailsMap();
				Set<java.util.Map.Entry<String, QualificationDetails>> qSet = qMap.entrySet();
				Iterator<java.util.Map.Entry<String, QualificationDetails>> qIterator = qSet.iterator();
				while (qIterator.hasNext()) {
					java.util.Map.Entry<String, QualificationDetails> qEntry = qIterator.next();
					QualificationDetails qualificationDetails = qEntry.getValue();
					CandidateEducationDetails candidateEducationDetails = new CandidateEducationDetails();
					candidateEducationDetails.setLevel(qEntry.getKey());
					candidateEducationDetails.setDegree(qualificationDetails.getEducation());
					candidateEducationDetails.setBranch(qualificationDetails.getSpecialization());
					candidateEducationDetails.setInstitution(qualificationDetails.getUniversityCollege());
					candidateEducationDetails
							.setYearOfPassing(Integer.toString(qualificationDetails.getPassingYear()));
					candidateEducationDetails.setPercentage(qualificationDetails.getPercentage());
					candidateEducationDetails.setUniversity(qualificationDetails.getUniversity());
					cMap.put(qEntry.getKey(), candidateEducationDetails);
				}
				candidateScoreCard.setCandidateEducationDetailsMap(cMap);
				candidateScoreCard.setStandingArrears(qualification.getStandingArrears());
				candidateScoreCard.setHistoryOfArrears(qualification.getHistoryArrears());

				String gapInYears = " ";
				if (mifForm.getQualification().getGapInEducation() != null) {
					if (mifForm.getQualification().getGapInEducation().equalsIgnoreCase("Yes"))
						gapInYears = mifForm.getQualification().getYrsOfGap() == null ? " "
								: mifForm.getQualification().getYrsOfGap();
					else
						gapInYears = "0";
				}
				candidateScoreCard.setEducationGapInYears(gapInYears);
			} else {
				candidateScoreCard.setUnivercityRegNo(RpsConstants.NA);
				candidateScoreCard.setCandidateEducationDetailsMap(cMap);
				candidateScoreCard.setStandingArrears(RpsConstants.NA);
				candidateScoreCard.setHistoryOfArrears(RpsConstants.NA);
				candidateScoreCard.setEducationGapInYears(RpsConstants.NA);
			}
			candidateScoreCard.setCandidateQuestionAndAnswers(mifForm.getQuestionsAnswersMap());
		} else {
			candidateScoreCard.setName(RpsConstants.NA);
			candidateScoreCard.setGender(RpsConstants.NA);
			candidateScoreCard.setDob(RpsConstants.NA);
			candidateScoreCard.setNationality(RpsConstants.NA);
			candidateScoreCard.setMailId(RpsConstants.NA);
			candidateScoreCard.setMobileNo(RpsConstants.NA);
			candidateScoreCard.setPermanentAddress(RpsConstants.NA);
			candidateScoreCard.setCurrentAddress(RpsConstants.NA);
			candidateScoreCard.setCandidateEducationDetailsMap(new HashMap<String, CandidateEducationDetails>());
			candidateScoreCard.setStandingArrears(RpsConstants.NA);
			candidateScoreCard.setHistoryOfArrears(RpsConstants.NA);
			candidateScoreCard.setEducationGapInYears(RpsConstants.NA);
			candidateScoreCard.setAge(RpsConstants.NA);
			List<CandidateQuestionAndAnswer> candidateQuestionAndAnswers = new ArrayList<>();
			candidateScoreCard.setCandidateQuestionAndAnswers(candidateQuestionAndAnswers);
		}
	}

	private void populateQpSectionMap(String assessmentCode) {
		List<RpsQpSection> rpsQpSectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
		this.rQpSectionMap.put(assessmentCode, rpsQpSectionsList);
	}

	// Behavioural reports code

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForSSPQNormalInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug(
				"---IN--generateBehavioralReportForSSPQNormalInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;

		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.SSPQN.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);

		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.SSPQN.toString(), "dd/MM/yyyy");

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}
			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.SSPQN.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
		
		}catch(Exception e){
			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
				path = new File(path + File.separator + BehavioralTestType.SSPQN.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");
			failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
			failedloginID.add(loginIdForCandidate);
			continue;
			}
		}
		try {
			SSPQNormalPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			SSPQNormalPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForSSPQPharmaInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired, BehavioralTestType behavioralTestType) {
		logger.debug(
				"---IN--generateBehavioralReportForSSPQPharmaInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		if (behavioralTestType.name().equals(BehavioralTestType.SSPQP.name()))
			pathForZip = new File(
					pathForZip + File.separator + BehavioralTestType.SSPQP.toString() + System.currentTimeMillis());
		else
			pathForZip = new File(
					pathForZip + File.separator + BehavioralTestType.SSPQI.toString() + System.currentTimeMillis());

		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.SSPQP.toString(), "dd/MM/yyyy");

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			if (behavioralTestType.name().equals(BehavioralTestType.SSPQP.name()))
				path = new File(path + File.separator + BehavioralTestType.SSPQP.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			else
				path = new File(path + File.separator + BehavioralTestType.SSPQI.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				if (behavioralTestType.name().equals(BehavioralTestType.SSPQP.name()))
					path = new File(path + File.separator + BehavioralTestType.SSPQP.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				else
					path = new File(path + File.separator + BehavioralTestType.SSPQI.toString() + "_"
							+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis()
							+ ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}
		try {
			SSPQPharmaPDFGenerator.failedPDFGenerator(failedBehaviouralReportEntitiesList);
			SSPQPharmaPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param behaviouralReportEntity
	 * @param behaviouralTestScoreMap
	 * @param rpCharacteristic
	 * @param totalScore
	 * @param gradeList
	 * @throws Exception
	 * @see
	 * @since Apollo v2.0
	 */
	private void getGradeListForBehaviouralReports(BehaviouralReportEntity behaviouralReportEntity,
			Map<String, Double> behaviouralTestScoreMap, RpsBehaviouralTestCharacteristic rpCharacteristic,
			double totalScore, List<BehaviouralGragingSchemaEntity> gradeList) throws RpsException {
		switch (rpCharacteristic.getcId()) {

			case "SSPQPF":
				String flexibilityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPF"));
				behaviouralReportEntity.setFlexibilityGrade(flexibilityGrade);
				behaviouralReportEntity.setFlexibilityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPF") == null ? 0 : behaviouralTestScoreMap.get("SSPQPF")));
				behaviouralReportEntity.setFlexibilityTotalMarks(decimalFormat.format(totalScore));
				break;

			case "SSPQPI":
				String integrityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPI"));
				behaviouralReportEntity.setIntegrityGrade(integrityGrade);
				behaviouralReportEntity.setIntegrityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPI") == null ? 0 : behaviouralTestScoreMap.get("SSPQPI")));
				behaviouralReportEntity.setIntegrityTotalMarks(decimalFormat.format(totalScore));
				break;

			case "SSPQPM":
				String motivationGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPM"));
				behaviouralReportEntity.setMotivationGrade(motivationGrade);
				behaviouralReportEntity.setMotivationScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPM") == null ? 0 : behaviouralTestScoreMap.get("SSPQPM")));
				behaviouralReportEntity.setMotivationTotalMarks(decimalFormat.format(totalScore));
				break;

			case "SSPQPO":
				String organizedGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPO"));
				behaviouralReportEntity.setBeingOrganizedatWorkGrade(organizedGrade);
				behaviouralReportEntity.setBeingOrganizedatWorkScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPO") == null ? 0 : behaviouralTestScoreMap.get("SSPQPO")));
				behaviouralReportEntity.setBeingOrganizedatWorkTotalMarks(decimalFormat.format(totalScore));
				break;

			case "SSPQPS":
				String sociabilityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPS"));
				behaviouralReportEntity.setSociablityGrade(sociabilityGrade);
				behaviouralReportEntity.setSociablityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPS") == null ? 0 : behaviouralTestScoreMap.get("SSPQPS")));
				behaviouralReportEntity.setSociablityTotalMarks(decimalFormat.format(totalScore));
				break;

			case "SSPQPT":
				String toleranceGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQPT"));
				behaviouralReportEntity.setTolerancetoStressGrade(toleranceGrade);
				behaviouralReportEntity.setTolerancetoStressScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQPT") == null ? 0 : behaviouralTestScoreMap.get("SSPQPT")));
				behaviouralReportEntity.setTolerancetoStressTotalMarks(decimalFormat.format(totalScore));
				break;


			case "SSPQNF":
				String normalFlexibilityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNF"));
				behaviouralReportEntity.setFlexibilityGrade(normalFlexibilityGrade);
				behaviouralReportEntity.setFlexibilityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNF") == null ? 0 : behaviouralTestScoreMap.get("SSPQNF")));
				behaviouralReportEntity.setFlexibilityTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity.setFlexibilityHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity.setFlexibilityAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity.setFlexibilityLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setFlexibilityHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setFlexibilityAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setFlexibilityLowRangeToMarks(RpsConstants.NA);
				}
				break;


			case "SSPQNI":
				String normalIntegrityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNI"));
				behaviouralReportEntity.setIntegrityGrade(normalIntegrityGrade);
				behaviouralReportEntity.setIntegrityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNI") == null ? 0 : behaviouralTestScoreMap.get("SSPQNI")));
				behaviouralReportEntity.setIntegrityTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity.setIntegrityHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity.setIntegrityAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity.setIntegrityLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setIntegrityHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setIntegrityAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setIntegrityLowRangeToMarks(RpsConstants.NA);
				}
				break;

			case "SSPQNM":
				String normalMotivationGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNM"));
				behaviouralReportEntity.setMotivationGrade(normalMotivationGrade);
				behaviouralReportEntity.setMotivationScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNM") == null ? 0 : behaviouralTestScoreMap.get("SSPQNM")));
				behaviouralReportEntity.setMotivationTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity.setMotivationHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity.setMotivationAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity.setMotivationLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setMotivationHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setMotivationAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setMotivationLowRangeToMarks(RpsConstants.NA);
				}
				break;

			case "SSPQNO":
				String normalOrganizedGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNO"));
				behaviouralReportEntity.setBeingOrganizedatWorkGrade(normalOrganizedGrade);
				behaviouralReportEntity.setBeingOrganizedatWorkScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNO") == null ? 0 : behaviouralTestScoreMap.get("SSPQNO")));
				behaviouralReportEntity.setBeingOrganizedatWorkTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity
									.setBeingOrganizedatWorkHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity
									.setBeingOrganizedatWorkAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity
									.setBeingOrganizedatWorkLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setBeingOrganizedatWorkHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setBeingOrganizedatWorkAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setBeingOrganizedatWorkLowRangeToMarks(RpsConstants.NA);
				}
				break;

			case "SSPQNS":
				String normalSociabilityGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNS"));
				behaviouralReportEntity.setSociablityGrade(normalSociabilityGrade);
				behaviouralReportEntity.setSociablityScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNS") == null ? 0 : behaviouralTestScoreMap.get("SSPQNS")));
				behaviouralReportEntity.setSociablityTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity.setSociablityHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity.setSociablityAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity.setSociablityLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setSociablityHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setSociablityAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setSociablityLowRangeToMarks(RpsConstants.NA);
				}
				break;

			case "SSPQNT":
				String normalToleranceGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("SSPQNT"));
				behaviouralReportEntity.setTolerancetoStressGrade(normalToleranceGrade);
				behaviouralReportEntity.setTolerancetoStressScore(decimalFormat.format(
						behaviouralTestScoreMap.get("SSPQNT") == null ? 0 : behaviouralTestScoreMap.get("SSPQNT")));
				behaviouralReportEntity.setTolerancetoStressTotalMarks(decimalFormat.format(totalScore));
				if (gradeList != null) {
					for (BehaviouralGragingSchemaEntity bEntity : gradeList) {
						if (bEntity.getGrade().equalsIgnoreCase("High"))
							behaviouralReportEntity
									.setTolerancetoStressHighRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Average"))
							behaviouralReportEntity
									.setTolerancetoStressAverageRangeToMarks(Float.toString(bEntity.getTo()));
						else if (bEntity.getGrade().equalsIgnoreCase("Low"))
							behaviouralReportEntity
									.setTolerancetoStressLowRangeToMarks(Float.toString(bEntity.getTo()));
					}
				} else {
					behaviouralReportEntity.setTolerancetoStressHighRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setTolerancetoStressAverageRangeToMarks(RpsConstants.NA);
					behaviouralReportEntity.setTolerancetoStressLowRangeToMarks(RpsConstants.NA);
				}
				break;

			case "CSOQHS":
				String helpfulGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CSOQHS"));
				behaviouralReportEntity.setHelpfulServiceGrade(helpfulGrade);
				behaviouralReportEntity.setHelpfulServiceScore(decimalFormat.format(
						behaviouralTestScoreMap.get("CSOQHS") == null ? 0 : behaviouralTestScoreMap.get("CSOQHS")));
				behaviouralReportEntity.setHelpfulServiceTotalMarks(decimalFormat.format(totalScore));
				break;

			case "CSOQPRSN":
				String personalizedGrade = this.generateBehaviouralGradeOnCandidateScore(gradeList,
						behaviouralTestScoreMap.get("CSOQPRSN"));
				behaviouralReportEntity.setPersonalizedServiceGrade(personalizedGrade);
				behaviouralReportEntity.setPersonalizedServiceScore(decimalFormat.format(
						behaviouralTestScoreMap.get("CSOQPRSN") == null ? 0 : behaviouralTestScoreMap.get("CSOQPRSN")));
				behaviouralReportEntity.setPersonalizedServiceTotalMarks(decimalFormat.format(totalScore));
				break;

			case "CSOQPRSV":
				String persuasiveGrade = this.generateBehaviouralGradeOnCandidateScore(gradeList,
						behaviouralTestScoreMap.get("CSOQPRSV"));
				behaviouralReportEntity.setPersuasiveServiceGrade(persuasiveGrade);
				behaviouralReportEntity.setPersuasiveServiceScore(decimalFormat.format(
						behaviouralTestScoreMap.get("CSOQPRSV") == null ? 0 : behaviouralTestScoreMap.get("CSOQPRSV")));
				behaviouralReportEntity.setPersuasiveServiceTotalMarks(decimalFormat.format(totalScore));
				break;

			case "LSQEO":
				String employeeOrientedGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("LSQEO"));
				behaviouralReportEntity.setEmployeeOrientedScore(decimalFormat.format(
						behaviouralTestScoreMap.get("LSQEO") == null ? 0 : behaviouralTestScoreMap.get("LSQEO")));
				behaviouralReportEntity.setEmployeeOrientedTotalMarks(decimalFormat.format(totalScore));
				behaviouralReportEntity.setEmployeeOrientedGrade(employeeOrientedGrade);
				break;

			case "LSQTO":
				String taskOrientedGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("LSQTO"));
				behaviouralReportEntity.setTaskOrientedScore(decimalFormat.format(
						behaviouralTestScoreMap.get("LSQTO") == null ? 0 : behaviouralTestScoreMap.get("LSQTO")));
				behaviouralReportEntity.setTaskOrientedTotalMarks(decimalFormat.format(totalScore));
				behaviouralReportEntity.setTaskOrientedGrade(taskOrientedGrade);
				break;

			case "GPQC":
				String conscientiousnessGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("GPQC"));
				behaviouralReportEntity.setConscientiousnessGrade(conscientiousnessGrade);
				behaviouralReportEntity.setConscientiousnessScore(decimalFormat
						.format(behaviouralTestScoreMap.get("GPQC") == null ? 0 : behaviouralTestScoreMap.get("GPQC")));
				behaviouralReportEntity.setConscientiousnessTotalMarks(decimalFormat.format(totalScore));
				break;

			case "GPQE":
				String experienceGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("GPQE"));
				behaviouralReportEntity.setClosedtoExperience_OpennesstoExperienceGrade(experienceGrade);
				behaviouralReportEntity.setClosedtoExperience_OpennesstoExperienceScore(decimalFormat
						.format(behaviouralTestScoreMap.get("GPQE") == null ? 0 : behaviouralTestScoreMap.get("GPQE")));
				behaviouralReportEntity
						.setClosedtoExperience_OpennesstoExperienceTotalMarks(decimalFormat.format(totalScore));
				break;

			case "GPQIE":
				String introversionGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("GPQIE"));
				behaviouralReportEntity.setIntroversion_ExtraversionGrade(introversionGrade);
				behaviouralReportEntity.setIntroversion_ExtraversionScore(decimalFormat.format(
						behaviouralTestScoreMap.get("GPQIE") == null ? 0 : behaviouralTestScoreMap.get("GPQIE")));
				behaviouralReportEntity.setIntroversion_ExtraversionTotalMarks(decimalFormat.format(totalScore));
				break;

			case "GPQN":
				String neuroticismGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("GPQN"));
				behaviouralReportEntity.setNeuroticism_EmotionalStabilityGrade(neuroticismGrade);
				behaviouralReportEntity.setNeuroticism_EmotionalStabilityScore(decimalFormat
						.format(behaviouralTestScoreMap.get("GPQN") == null ? 0 : behaviouralTestScoreMap.get("GPQN")));
				behaviouralReportEntity.setNeuroticism_EmotionalStabilityTotalMarks(decimalFormat.format(totalScore));
				break;

			case "GPQTM":
				String toughGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("GPQTM"));
				behaviouralReportEntity.setToughMindedness_AgreeablenessGrade(toughGrade);
				behaviouralReportEntity.setToughMindedness_AgreeablenessScore(decimalFormat.format(
						behaviouralTestScoreMap.get("GPQTM") == null ? 0 : behaviouralTestScoreMap.get("GPQTM")));
				behaviouralReportEntity.setToughMindedness_AgreeablenessTotalMarks(decimalFormat.format(totalScore));
				break;

			case "WPAATD":
				String atdGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("WPAATD"));
				behaviouralReportEntity.setBcjtATDType(atdGrade);
				break;

			case "WPAPS":
				String psGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("WPAPS"));
				behaviouralReportEntity.setBcjtPSType(psGrade);
				break;

			case "WPAR":
				String rGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("WPAR"));
				behaviouralReportEntity.setBcjtRType(rGrade);
				break;

			case "WPAI":
				String iGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("WPAI"));
				behaviouralReportEntity.setBcjtIType(iGrade);
				break;

			case "WPAAR":
				String arGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("WPAAR"));
				behaviouralReportEntity.setBcjtARType(arGrade);
				break;

			// Score interpretation grades
			case "CPAAAP":
				String apGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPAAAP"));
				behaviouralReportEntity.setCpaAPGrade(apGrade);
				break;

			case "CPAAHS":
				String hsGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPAAHS"));
				behaviouralReportEntity.setCpaHSGrade(hsGrade);
				break;

			case "CPAACC":
				String ccGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPAACC"));
				behaviouralReportEntity.setCpaCCGrade(ccGrade);
				break;

			case "CPAACE":
				String aceGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPAACE"));
				behaviouralReportEntity.setCpaCEGrade(aceGrade);
				break;

			case "CPAAMI":
				String amiGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPAAMI"));
				behaviouralReportEntity.setCpaMIGrade(amiGrade);
				break;

			case "CPABAP":
				String bapGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPABAP"));
				behaviouralReportEntity.setCpaAPGrade(bapGrade);
				break;

			case "CPABHS":
				String bhsGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPABHS"));
				behaviouralReportEntity.setCpaHSGrade(bhsGrade);
				break;

			case "CPABCC":
				String bccGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPABCC"));
				behaviouralReportEntity.setCpaCCGrade(bccGrade);
				break;

			case "CPABCE":
				String bceGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPABCE"));
				behaviouralReportEntity.setCpaCEGrade(bceGrade);
				break;

			case "CPABMI":
				String bmiGrade =
						this.generateBehaviouralGradeOnCandidateScore(gradeList, behaviouralTestScoreMap.get("CPABMI"));
				behaviouralReportEntity.setCpaMIGrade(bmiGrade);
				break;
			case "IDSGA":
				behaviouralReportEntity
						.setIdsAType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGA"))));
				break;
			case "IDSGS":
				behaviouralReportEntity
						.setIdsSType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGS"))));
				break;
			case "IDSGTW":
				behaviouralReportEntity
						.setIdsTWType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGTW"))));
				break;
			case "IDSGP":
				behaviouralReportEntity
						.setIdsPType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGP"))));
				break;
			case "IDSGC":
				behaviouralReportEntity
						.setIdsCType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGC"))));
				break;
			case "IDSGO":
				behaviouralReportEntity
						.setIdsOType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGO"))));
				break;
			case "IDSGPS":
				behaviouralReportEntity
						.setIdsPSType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGPS"))));
				break;
			case "IDSGCON":
				behaviouralReportEntity
						.setIdsCONType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSGCON"))));
				break;
			case "IDSBA":
				behaviouralReportEntity
						.setIdsAType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBA"))));
				behaviouralReportEntity
						.setIdsAPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBA%"))));
				break;
			case "IDSBS":
				behaviouralReportEntity
						.setIdsSPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBS%"))));
				behaviouralReportEntity
						.setIdsSType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBS"))));
				break;
			case "IDSBTW":
				behaviouralReportEntity
						.setIdsTWPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBTW%"))));
				behaviouralReportEntity
						.setIdsTWType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBTW"))));
				break;
			case "IDSBP":
				behaviouralReportEntity
						.setIdsPPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBP%"))));
				behaviouralReportEntity
						.setIdsPType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBP"))));
				break;
			case "IDSBC":
				behaviouralReportEntity
						.setIdsCPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBC%"))));
				behaviouralReportEntity
						.setIdsCType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBC"))));
				break;
			case "IDSBO":
				behaviouralReportEntity
						.setIdsOPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBO%"))));
				behaviouralReportEntity
						.setIdsOType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBO"))));
				break;
			case "IDSBPS":
				behaviouralReportEntity
						.setIdsPSPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBPS%"))));
				behaviouralReportEntity
						.setIdsPSType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBPS"))));
				break;
			case "IDSBCON":
				behaviouralReportEntity
						.setIdsCONPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSBCON%"))));
				behaviouralReportEntity
						.setIdsCONType(pickCellColor(Double.toString(behaviouralTestScoreMap.get("IDSBCON"))));
				break;

			// IDSA pecentage add values
			case "IDSAA":
				behaviouralReportEntity
						.setIdsaAPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAA%"))));
				break;
			case "IDSAS":
				behaviouralReportEntity
						.setIdsaSPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAS%"))));
				break;
			case "IDSATW":
				behaviouralReportEntity
						.setIdsaTWPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSATW%"))));
				break;
			case "IDSAP":
				behaviouralReportEntity
						.setIdsaPPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAP%"))));
				break;
			case "IDSAC":
				behaviouralReportEntity
						.setIdsaCPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAC%"))));

				break;
			case "IDSAO":
				behaviouralReportEntity
						.setIdsaOPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAO%"))));

				break;
			case "IDSAPS":
				behaviouralReportEntity
						.setIdsaPSPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSAPS%"))));

				break;
			case "IDSACON":
				behaviouralReportEntity
						.setIdsaCONPerc(Integer.toString(roundPercToInteger(behaviouralTestScoreMap.get("IDSACON%"))));

				break;

			case "WTLD":
				behaviouralReportEntity.setWtlDScore(behaviouralTestScoreMap.get("WTLD"));
				break;

			case "WTLP":
				behaviouralReportEntity.setWtlPScore(behaviouralTestScoreMap.get("WTLP"));
				break;

			case "WTLIP":
				behaviouralReportEntity.setWtlIPScore(behaviouralTestScoreMap.get("WTLIP"));
				break;

			case "WTLIA":
				behaviouralReportEntity.setWtlIAScore(behaviouralTestScoreMap.get("WTLIA"));
				break;

			case "WTLFD":
				String fdLevel = this.generateBehaviouralGradeOnCandidatePerc(gradeList,
						behaviouralTestScoreMap.get("WTLFD"), RpsConstants.MAX_WTL_FD_MARK);
				behaviouralReportEntity.setWtlFDType(fdLevel);
				break;
			default:
				break;
		}
	}

	private void getGradeListForDimension8BehaviouralReports(Dimension8BehaviouralReportEntity behaviouralReportEntity,
			Map<String, Double> behaviouralTestScoreMap, RpsBehaviouralTestCharacteristic rpCharacteristic,
			double totalScore, List<BehaviouralGragingSchemaEntity> gradeList) throws RpsException {
		switch (rpCharacteristic.getcId()) {

			// Dimension 8
			case "DADP":
				double score = behaviouralTestScoreMap.get("DADP");
				behaviouralReportEntity.setAdaptabilityScore(score);
				double perc = (score / 27) * 100;
				double roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setAdaptabilityPerc(roundedPerc);
				behaviouralReportEntity.setAdaptabilityGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DCON":
				score = behaviouralTestScoreMap.get("DCON");
				behaviouralReportEntity.setConscientiousScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setConscientiousnessPerc(roundedPerc);
				behaviouralReportEntity.setConscientiousnessGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DCRE":
				score = behaviouralTestScoreMap.get("DCRE");
				behaviouralReportEntity.setCreativityScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setCreativityPerc(roundedPerc);
				behaviouralReportEntity.setCreativityGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DORG":
				score = behaviouralTestScoreMap.get("DORG");
				behaviouralReportEntity.setOrganizedScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setOrganizedPerc(roundedPerc);
				behaviouralReportEntity.setOrganizedGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DPER":
				score = behaviouralTestScoreMap.get("DPER");
				behaviouralReportEntity.setPersistenceScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setPersistencePerc(roundedPerc);
				behaviouralReportEntity.setPersistenceGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DPSK":
				score = behaviouralTestScoreMap.get("DPSK");
				behaviouralReportEntity.setPersuasiveSkillsScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setPersuasiveSkillsPerc(roundedPerc);
				behaviouralReportEntity.setPersuasiveSkillsGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DRAQ":
				score = behaviouralTestScoreMap.get("DRAQ");
				behaviouralReportEntity.setFactualQuestionsScore(score);
				perc = (score / 5) * 100;
				roundedPerc = roundPercToInteger(perc);
				score = roundPercToInteger(score);
				behaviouralReportEntity.setFactualQuestionsPerc(roundedPerc);
				behaviouralReportEntity.setFactualQuestionsGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "DSOC":
				score = behaviouralTestScoreMap.get("DSOC");
				behaviouralReportEntity.setSociabilityScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setSociabilityPerc(roundedPerc);
				behaviouralReportEntity.setSociabilityGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "DTEW":
				score = behaviouralTestScoreMap.get("DTEW");
				behaviouralReportEntity.setTeamworkScore(score);
				perc = (score / 27) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setTeamworkPerc(roundedPerc);
				behaviouralReportEntity.setTeamworkGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			default:
				break;
		}
	}
	private int roundPercToInteger(Double value) {
		double roundOff = Math.round(value);
		return (int) roundOff;
	}

	private static String pickCellColor(String value) {
		String color = messagesReader.getProperty("IDS_INVALID_TEST");
		switch (value) {
			case "0.0":
				color = messagesReader.getProperty("IDS_LIKELY_WEAKNESS");
				break;
			case "1.0":
				color = messagesReader.getProperty("IDS_LIKELY_STRENGTH");
				break;
			default:
				break;
		}
		return color;
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForLSQInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForLSQInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.LSQ.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<BehaviouralReportEntity> failedLSPQReportEntities = new ArrayList<>();
		List<String> failedloginID = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = " ";
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.LSQ.toString(), "dd/MM/yyyy");

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}

				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.LSQ.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
		}catch (Exception e) {
			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
				path = new File(path + File.separator + BehavioralTestType.LSQ.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");
			
			failedLSPQReportEntities.add(behaviouralReportEntity);
			failedloginID.add(loginIdForCandidate);

			continue;
		}
	}

		try {
			LSQPDFGenerator.lspqReportForFailedCandidats(failedLSPQReportEntities);
			LSQPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedLSPQReportEntities)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
					behavioralDownloadingPath.toString(),
					"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
					failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")){
				if (behaviouralReportEntitiesList != null)
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
						behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedLSPQReportEntities != null) {
					failedLSPQReportEntities.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedLSPQReportEntities.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForGPQInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForGPQInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.GPQ.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.GPQ.toString(), "dd/MM/yyyy");

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.GPQ.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.GPQ.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}

		try {
			GPQPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			GPQPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForCSOQInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForCSOQInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.CSOQ.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		//

		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.CSOQ.toString(), "dd/MM/yyyy");

			// FRIN
			List<String> overallFRIN = getFrinScore(rpsMasterAssociation.getUniqueCandidateId(),
					BehavioralTestType.CSOQ);
			if (overallFRIN != null && overallFRIN.size() > 0)
				behaviouralReportEntity.setOverallFRIN(overallFRIN.get(0));

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.CSOQ.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			}catch(Exception e){
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CSOQ.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
				
		}

		try {
			CSOQPDFGenerator.csoqReportForFailedCandidats(failedBehaviouralReportEntitiesList);
			CSOQPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
					behavioralDownloadingPath.toString(),
					"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
					failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")){
				if (behaviouralReportEntitiesList != null)
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
						behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForWPAInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForWPAInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.WPA.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.WPA.toString(), "dd/MM/yyyy");

			// FRIN
			List<String> overallFRIN = getFrinScore(rpsMasterAssociation.getUniqueCandidateId(),
					BehavioralTestType.WPA);
			behaviouralReportEntity.setOverallFRIN(overallFRIN.get(0));
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						0.0, gradeList);
			}

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.WPA.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.WPA.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}

		try {
			BlueCollaredJobsPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			BlueCollaredJobsPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param uniqueCandidateId
	 * @return
	 */
	private List<String> getFrinScore(int uniqueCandidateId, BehavioralTestType behavioralTestType) {
		logger.debug("FRIN for unique candidate id = {}", uniqueCandidateId);
		List<String> overallFRIN = new ArrayList<String>(1);
		RpsCandidateResponses candidateResponses = rpsCandidateResponsesRepository
				.findByUniqueCandidateId(uniqueCandidateId);
		if (candidateResponses != null) {
			String quest1 = candidateResponses.getQNo_1();
			String quest2 = candidateResponses.getQNo_2();
			String quest3 = candidateResponses.getQNo_3();
			String quest4 = candidateResponses.getQNo_4();
			String quest5 = candidateResponses.getQNo_5();
			String quest7 = candidateResponses.getQNo_7();
			String quest8 = candidateResponses.getQNo_8();
			String quest9 = candidateResponses.getQNo_9();
			String quest10 = candidateResponses.getQNo_10();
			String quest12 = candidateResponses.getQNo_12();
			String quest13 = candidateResponses.getQNo_13();
			String quest15 = candidateResponses.getQNo_15();
			String quest16 = candidateResponses.getQNo_16();
			String quest17 = candidateResponses.getQNo_17();
			String quest18 = candidateResponses.getQNo_18();
			String quest19 = candidateResponses.getQNo_19();
			String quest21 = candidateResponses.getQNo_21();
			String quest22 = candidateResponses.getQNo_22();
			String quest23 = candidateResponses.getQNo_23();
			String quest24 = candidateResponses.getQNo_24();
			String quest25 = candidateResponses.getQNo_25();
			String quest26 = candidateResponses.getQNo_26();
			String quest27 = candidateResponses.getQNo_27();
			String quest28 = candidateResponses.getQNo_28();
			String quest29 = candidateResponses.getQNo_29();
			String quest30 = candidateResponses.getQNo_30();
			String quest32 = candidateResponses.getQNo_32();
			String quest34 = candidateResponses.getQNo_34();
			String quest35 = candidateResponses.getQNo_35();
			String quest36 = candidateResponses.getQNo_36();
			String quest37 = candidateResponses.getQNo_37();
			String quest38 = candidateResponses.getQNo_38();

			switch (behavioralTestType) {
				case CSOQ:
					int frinQ3Q4 = getFrinFor1001Inconsistent(quest3, quest4);
					logger.debug("FRIN for Q3 and Q4" + frinQ3Q4);

					int frinQ7Q8 = getFrinFor0110Inconsistent(quest7, quest8);
					logger.debug("FRIN for Q7 and Q7" + frinQ7Q8);

					int frinQ12Q13 = getFrinFor1001Inconsistent(quest12, quest13);
					logger.debug("FRIN for Q12 and Q13" + frinQ12Q13);

					int frinQ16Q18 = getFrinFor0110Inconsistent(quest16, quest18);
					logger.debug("FRIN for Q16 and Q18" + frinQ16Q18);

					int frinQ21Q26 = getFrinFor0110Inconsistent(quest21, quest26);
					logger.debug("FRIN for Q21 and Q26" + frinQ21Q26);

					int frinQ22Q28 = getFrinFor1001Inconsistent(quest22, quest28);
					logger.debug("FRIN for Q22 and Q28" + frinQ22Q28);

					int overAllFrin = frinQ3Q4 + frinQ7Q8 + frinQ12Q13 + frinQ16Q18 + frinQ21Q26 + frinQ22Q28;
					logger.debug("Overall FRIN " + overAllFrin);

					switch (overAllFrin) {
						case 6:
							overallFRIN.add(RpsConstants.HIGHLY_INCONSISTENT_FRIN_IMAGE);
							break;
						case 5:
						case 4:
							overallFRIN.add(RpsConstants.INCONSISTENT_FRIN_IMAGE);
							break;
						case 3:
						case 2:
						case 1:
						case 0:
							overallFRIN.add(RpsConstants.NO_INCONSISTENT_FRIN_IMAGE);
							break;
						default:
							break;
					}
					break;

				case WPA:
					int frinQ1Q4 = getFrinFor0110Inconsistent(quest1, quest4);
					logger.debug("FRIN for Q1 and Q4" + frinQ1Q4);

					int frinQ3Q5 = getFrinFor0110Inconsistent(quest3, quest5);
					logger.debug("FRIN for Q3 and Q5" + frinQ3Q5);

					int frinQ17Q19 = getFrinFor0110Inconsistent(quest17, quest19);
					logger.debug("FRIN for Q17 and Q19" + frinQ17Q19);

					int frinQ23Q24 = getFrinFor1001Inconsistent(quest23, quest24);
					logger.debug("FRIN for Q23 and Q24" + frinQ23Q24);

					int frinQ25Q26 = getFrinFor0110Inconsistent(quest25, quest26);
					logger.debug("FRIN for Q25 and Q26" + frinQ25Q26);

					int frinQ27Q28 = getFrinFor1001Inconsistent(quest27, quest28);
					logger.debug("FRIN for Q27 and Q28" + frinQ27Q28);

					int frinQ29Q37 = getFrinFor1001Inconsistent(quest29, quest37);
					logger.debug("FRIN for Q29 and Q37" + frinQ29Q37);

					int frinQ30Q35 = getFrinFor1001Inconsistent(quest30, quest35);
					logger.debug("FRIN for Q30 and Q35" + frinQ30Q35);

					int overAllFrinScore = frinQ1Q4 + frinQ3Q5 + frinQ17Q19 + frinQ23Q24 + frinQ25Q26 + frinQ27Q28
							+ frinQ29Q37 + frinQ30Q35;
					logger.debug("Overall FRIN " + overAllFrinScore);

					// calculate FRIN
					int maxFrinMarks = RpsConstants.MAX_WPA_FRIN_MARK;
					double frinPerctge = ((double) overAllFrinScore / maxFrinMarks) * 100;

					if (frinPerctge <= 50) {
						overallFRIN.add(RpsConstants.WPA_INCONSISTENT_FRIN_IMAGE);
					} else if (frinPerctge > 50) {
						overallFRIN.add(RpsConstants.WPA_HIGHLY_CONSISTENT_FRIN_IMAGE);
					}
					break;

				case WTL:
					// demo
					int frinQ1Q15 = getFrinFor0110Inconsistent(quest1, quest15);
					logger.debug("FRIN for Q1 and Q15" + frinQ1Q15);

					int frinQ2Q16 = getFrinFor0110Inconsistent(quest2, quest16);
					logger.debug("FRIN for Q2 and Q16" + frinQ2Q16);

					int frinQ3Q17 = getFrinFor0110Inconsistent(quest3, quest17);
					logger.debug("FRIN for Q3 and Q17" + frinQ3Q17);

					// insightful

					int friNQ7Q8 = getFrinFor1001Inconsistent(quest7, quest8);
					logger.debug("FRIN for Q7 and Q8" + friNQ7Q8);

					int frinQ9Q10 = getFrinFor0110Inconsistent(quest9, quest10);
					logger.debug("FRIN for Q9 and Q10" + frinQ9Q10);

					int frinQ21Q34 = getFrinFor1001Inconsistent(quest21, quest34);
					logger.debug("FRIN for Q21 and Q34" + frinQ21Q34);

					// integration

					int frinQ12Q37 = getFrinFor1001Inconsistent(quest12, quest37);
					logger.debug("FRIN for Q12 and Q37" + frinQ12Q37);

					int frinQ13Q38 = getFrinFor0110Inconsistent(quest13, quest38);
					logger.debug("FRIN for Q13 and Q38" + frinQ13Q38);

					int frinQ24Q36 = getFrinFor0110Inconsistent(quest24, quest36);
					logger.debug("FRIN for Q24 and Q36" + frinQ24Q36);

					// pursue
					int frinQ4Q18 = getFrinFor0110Inconsistent(quest4, quest18);
					logger.debug("FRIN for Q4 and Q18" + frinQ4Q18);

					int frinQ5Q32 = getFrinFor1001Inconsistent(quest5, quest32);
					logger.debug("FRIN for Q5 and Q32" + frinQ5Q32);

					int demoFrinForWTL = frinQ1Q15 + frinQ2Q16 + frinQ3Q17;

					int insightfulFrinForWTL = friNQ7Q8 + frinQ9Q10 + frinQ21Q34;

					int integrationFrinForWTL = frinQ12Q37 + frinQ13Q38 + frinQ24Q36;

					int pursueFrinForWTL = frinQ4Q18 + frinQ5Q32;

					int overAllFrinForWTL = frinQ1Q15 + frinQ2Q16 + frinQ3Q17 + frinQ4Q18 + frinQ5Q32 + friNQ7Q8
							+ frinQ9Q10 + frinQ12Q37 + frinQ13Q38 + frinQ21Q34 + frinQ24Q36;
					logger.debug("Overall FRIN " + overAllFrinForWTL);

					String frinWTLDPerctge = getPercentageOfFRINImage(demoFrinForWTL, RpsConstants.MAX_WTLD_FRIN_MARK);
					String frinWTLIPPerctge =
							getPercentageOfFRINImage(insightfulFrinForWTL, RpsConstants.MAX_WTLIP_FRIN_MARK);
					String frinWTLIAPerctge =
							getPercentageOfFRINImage(integrationFrinForWTL, RpsConstants.MAX_WTLIA_FRIN_MARK);
					String frinWTLPPerctge =
							getPercentageOfFRINImage(pursueFrinForWTL, RpsConstants.MAX_WTLP_FRIN_MARK);
					String overallFrinWTLPerctge =
							getPercentageOfFRINImage(overAllFrinForWTL, RpsConstants.MAX_WTL_OVERALL_FRIN_MARK);

					overallFRIN.add(frinWTLDPerctge);
					overallFRIN.add(frinWTLPPerctge);
					overallFRIN.add(frinWTLIPPerctge);
					overallFRIN.add(frinWTLIAPerctge);
					overallFRIN.add(overallFrinWTLPerctge);

					break;

				default:
					overallFRIN.add("");
					break;

			}
		}
		return overallFRIN;
	}

	private String getPercentageOfFRINImage(double score, double maxScore) {
		String candidateFrinShade = "";
		double perc = ((double) score / maxScore) * 100;
		if (perc <= 25)
			candidateFrinShade = RpsConstants.NO_INCONSISTENT_FRIN_IMAGE;
		else if (perc > 25 && perc <= 50)
			candidateFrinShade = RpsConstants.INCONSISTENT_FRIN_IMAGE;
		else if (perc > 50)
			candidateFrinShade = RpsConstants.HIGHLY_INCONSISTENT_FRIN_IMAGE;
		return candidateFrinShade;
	}

	/**
	 * @param question1
	 * @param question2
	 * @return
	 */
	private int getFrinFor0110Inconsistent(String question1, String question2) {
		if (question1 == null || question2 == null)
			return -1;
		String answers1[] = question1.split("--");
		String answers2[] = question2.split("--");
		if (answers1[2].equalsIgnoreCase("CHOICE1") && answers2[2].equalsIgnoreCase("CHOICE1")) {
			return 0;
		} else if (answers1[2].equalsIgnoreCase("CHOICE1") && answers2[2].equalsIgnoreCase("CHOICE2")) {
			return 1;
		} else if (answers1[2].equalsIgnoreCase("CHOICE2") && answers2[2].equalsIgnoreCase("CHOICE1")) {
			return 1;
		} else if (answers1[2].equalsIgnoreCase("CHOICE2") && answers2[2].equalsIgnoreCase("CHOICE2")) {
			return 0;
		}
		return -1;
	}

	/**
	 * @param question1
	 * @param question2
	 * @return
	 */
	private int getFrinFor1001Inconsistent(String question1, String question2) {
		if (question1 == null || question2 == null)
			return -1;
		String answers1[] = question1.split("--");
		String answers2[] = question2.split("--");
		if (answers1[2].equalsIgnoreCase("CHOICE1") && answers2[2].equalsIgnoreCase("CHOICE1")) {
			return 1;
		} else if (answers1[2].equalsIgnoreCase("CHOICE1") && answers2[2].equalsIgnoreCase("CHOICE2")) {
			return 0;
		} else if (answers1[2].equalsIgnoreCase("CHOICE2") && answers2[2].equalsIgnoreCase("CHOICE1")) {
			return 0;
		} else if (answers1[2].equalsIgnoreCase("CHOICE2") && answers2[2].equalsIgnoreCase("CHOICE2")) {
			return 1;
		}
		return -1;
	}

	/**
	 * generate Behavioural Grade On Candidate Score
	 *
	 * @param gradingSchemeDos
	 * @param candidateScore
	 * @return
	 * @throws Exception
	 */
	private String generateBehaviouralGradeOnCandidateScore(List<BehaviouralGragingSchemaEntity> gradingSchemeDos,
			Double candidateScore) throws RpsException {
		logger.info("--IN generateBehaviouralGrade -- with param gradingSchemeDos: {}, candidateScore: {}",
				gradingSchemeDos, candidateScore);
		String scoreGrade = null;
		if (gradingSchemeDos != null && candidateScore != null) {
			for (BehaviouralGragingSchemaEntity gradingSchemeDo : gradingSchemeDos) {
				if (candidateScore >= gradingSchemeDo.getFrom() && candidateScore <= gradingSchemeDo.getTo()) {
					scoreGrade = gradingSchemeDo.getGrade();
					break;
				}
			}
		}
		if (scoreGrade == null)
			throw new RpsException("In complete assessment");
		return scoreGrade;
	}

	/**
	 * generate Behavioural Grade On Candidate Perc
	 *
	 * @param gradingSchemeDos
	 * @param candidateScore
	 * @return
	 */
	private String generateBehaviouralGradeOnCandidatePerc(List<BehaviouralGragingSchemaEntity> gradingSchemeDos,
			Double candidateScore, Double maxScore) {
		logger.info("--IN generateBehaviouralGrade -- with param gradingSchemeDos: {}, candidateScore: {}",
				gradingSchemeDos, candidateScore);
		// calculate perc
		Double candPerctge = (candidateScore / maxScore) * 100;

		String scoreGrade = RpsConstants.NA;
		if (gradingSchemeDos != null && candPerctge != null) {
			for (BehaviouralGragingSchemaEntity gradingSchemeDo : gradingSchemeDos) {
				if (candPerctge >= gradingSchemeDo.getFrom() && candPerctge <= gradingSchemeDo.getTo()) {
					scoreGrade = gradingSchemeDo.getGrade();
					break;
				}
			}
		}
		return scoreGrade;
	}

	/**
	 * generateBehaviouralGradeByPercentage provide the functionality for
	 * calculating grade on the basis of percentage and not on the mark limits
	 * The limit for grade we can take by multiplying percentage grade with the
	 * total score of question like in the case of SSOPQ q no 5.10.15.20
	 *
	 * @param gradingSchemeDos
	 *            list of percentiles
	 * @param candidateScore
	 *            candidate Score
	 * @param behaviouralScore
	 *            contains the question score for which we have ti calculate the
	 *            fringe
	 * @return grade for the behaviouralScore
	 */
	private String generateBehaviouralGradeByPercentage(List<BehaviouralGragingSchemaEntity> gradingSchemeDos,
			Double candidateScore, Double behaviouralScore) {
		logger.info("--IN generateBehaviouralGrade -- with param gradingSchemeDos: {}, candidateScore: {}",
				gradingSchemeDos, candidateScore);
		String scoreGrade = RpsConstants.NA;
		if (gradingSchemeDos != null && candidateScore != null) {
			for (BehaviouralGragingSchemaEntity gradingSchemeDo : gradingSchemeDos) {
				if (candidateScore >= (behaviouralScore * gradingSchemeDo.getFrom())
						&& candidateScore <= (behaviouralScore * gradingSchemeDo.getTo())) {
					scoreGrade = gradingSchemeDo.getGrade();
					break;
				} else if (candidateScore == 0) {
					scoreGrade = gradingSchemeDo.getGrade();
					break;
				}
			}
		}
		return scoreGrade;
	}

	/**
	 * @param eventCode
	 * @param startDate
	 * @param endDate
	 * @param assessmentCode
	 * @param qpId
	 * @return
	 */
	@Transactional
	public String getBehaviouralReport(String eventCode, String startDate, String endDate, String assessmentCode,
			Integer qpId, Integer acsId, Integer bId) {
		logger.debug(
				"---IN--getBehaviouralReport()----- with param eventCode: {}, startDate: {}, endDate: {}, assessmentCode: {}, qpId: {}",
				eventCode, startDate, endDate, assessmentCode, qpId);
		String behaviouralReportJson = RpsConstants.EMPTY_JSON;
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, evenCode, startDate, endDate, assessmentCode");
			return behaviouralReportJson;
		}

		RpsQuestionPaper rpPaper = null;
		if (qpId != null && qpId != 0)
			rpPaper = rpsQuestionPaperService.findOne(qpId);

		ResultProcessingRuleDo resultProcessingRuleDo = null;
		resultProcessingRuleDo = scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper,
				assessmentCode);
		boolean behaviouralTest = false;
		BehavioralTestType behaviouralTestType = null;
		if (resultProcessingRuleDo != null) {
			behaviouralTest = resultProcessingRuleDo.isBehaviouralTest();
			behaviouralTestType = resultProcessingRuleDo.getBehaviouralTestType();
		}

		if (!behaviouralTest)
			return behaviouralReportJson;

		Date rangeStartDate = null, rangeEndDate = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		try {
			rangeStartDate = sdf.parse(startDate);
			rangeEndDate = sdf.parse(endDate);
		} catch (ParseException e) {
			logger.error("Error in parsing the date format");
			return behaviouralReportJson;
		}
		List<RpsMasterAssociation> rpsMasterAssociationsList = getRpsMasterAssoOnAcsAndBatchIds(eventCode,
				rangeStartDate, rangeEndDate, assessmentCode, acsId, bId);

		if (rpsMasterAssociationsList == null || rpsMasterAssociationsList.isEmpty()) {
			logger.error("No candidate have taken exam for open event in selected date range");
			return behaviouralReportJson;
		}

		Map<Integer, RpsMasterAssociation> uniqueIdToMasterAssnMap = new HashMap<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList)
			uniqueIdToMasterAssnMap.put(rpsMasterAssociation.getUniqueCandidateId(), rpsMasterAssociation);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = null;
		if (qpId == null || qpId == 0)
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIds(uniqueIdToMasterAssnMap.keySet());
		else
			rpsCandidateResponseLitesList = rpsCandidateResponseService
					.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);

		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return behaviouralReportJson;
		}

		List<RpsCandidate> rpsCandidatesList = rpsMasterAssociationService
				.findAllCandidateByUniqueIds(uniqueIdToMasterAssnMap.keySet());
		if (rpsCandidatesList == null || rpsCandidatesList.isEmpty()) {
			logger.error("No candidate is avaible in DB");
			return behaviouralReportJson;
		}
		// load a map of cid to candidate entity
		Map<Integer, RpsCandidate> cidToCandidateMap = new HashMap<>();
		for (RpsCandidate rpsCandidate : rpsCandidatesList)
			cidToCandidateMap.put(rpsCandidate.getCid(), rpsCandidate);

		BehaviouralReportWrapper behaviouralReportWrapper = new BehaviouralReportWrapper();
		List<BehaviouralReportPageEntity> behaviouralReportPageEntitiesList = new ArrayList<>();
		// fetch behavioral report grid data across all rows
		scoreReportUtility.getBehaviouralReportGridData(rpsCandidateResponseLitesList, uniqueIdToMasterAssnMap,
				cidToCandidateMap, behaviouralReportPageEntitiesList, behaviouralTestType);
		behaviouralReportWrapper.setBehaviouralReportPageEntitiesList(behaviouralReportPageEntitiesList);
		behaviouralReportJson = gson.toJson(behaviouralReportWrapper);

		logger.debug("---OUT--getBehaviouralReport()---with params behaviouralReportJson: {}", behaviouralReportJson);
		return behaviouralReportJson;
	}

	public String generateBehaviouralReportsBasidOnType(String assessmentCode, Integer qpID,
			Set<Integer> uniqueCandidateIds, Boolean isZipFileRequired) {
		String status = RpsConstants.NA;
		RpsQuestionPaper rpPaper = null;
		if (qpID != null && qpID != 0)
			rpPaper = rpsQuestionPaperService.findOne(qpID);

		ResultProcessingRuleDo resultProcessingRuleDo = scoreReportUtility
				.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper, assessmentCode);
		if (resultProcessingRuleDo != null && resultProcessingRuleDo.isBehaviouralTest()) {
			BehavioralTestType behavioralTestType = resultProcessingRuleDo.getBehaviouralTestType();

			switch (behavioralTestType) {
				case SSPQN:
					status = this.generateBehavioralReportForSSPQNormalInPDF(qpID, uniqueCandidateIds,
							isZipFileRequired);
					break;
				case SSPQP:
				case SSPQI:
					status = this.generateBehavioralReportForSSPQPharmaInPDF(qpID, uniqueCandidateIds,
							isZipFileRequired, behavioralTestType);
					break;
				case SSPQG:
					status = this.generateBehavioralReportForSSPQGenericInPDF(qpID, uniqueCandidateIds,
							isZipFileRequired);
					break;
				case GPQ:
					status = this.generateBehavioralReportForGPQInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case LSQ:
					status = this.generateBehavioralReportForLSQInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case CSOQ:
					status = this.generateBehavioralReportForCSOQInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case WPA:
					status = this.generateBehavioralReportForWPAInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case CPAA:
					status = this.generateBehavioralReportForCPAAdvancedInPDF(qpID, uniqueCandidateIds,
							isZipFileRequired);
					break;
				case CPAB:
					status = this.generateBehavioralReportForCPABasicInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case IDSG:
					status = this.generateBehavioralReportForIDSGInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case IDSB:
					status = this.generateBehavioralReportForIDSBInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case WTL:
					status = this.generateBehavioralReportForWTLInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case WTS:
					status = this.generateBehavioralReportForWTSInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case IDSA:
					status = this.generateBehavioralReportForIDSAInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case CGT:
					status = this.generateBehavioralReportForCGTInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case Dimension8:
					status = this.generateBehavioralReportForD8InPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
				case CSP:
					status = this.generateBehavioralReportForCSPInPDF(qpID, uniqueCandidateIds, isZipFileRequired);
					break;
			}
		}

		return status;
	}

	public String generateBehaviouralReportsBasidOnTypeInExcel(String assessmentCode, Integer qpID,
			Set<Integer> uniqueCandidateIds, Boolean isZipFileRequired) {
		RpsQuestionPaper rpPaper = null;
		String status = RpsConstants.NA;
		if (qpID != null && qpID != 0)
			rpPaper = rpsQuestionPaperService.findOne(qpID);

		ResultProcessingRuleDo resultProcessingRuleDo = scoreReportUtility
				.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper, assessmentCode);
		if (resultProcessingRuleDo != null && resultProcessingRuleDo.isBehaviouralTest()) {
			BehavioralTestType behavioralTestType = resultProcessingRuleDo.getBehaviouralTestType();
			if (behavioralTestType.equals(BehavioralTestType.CGT)) {
				status = (generateBehavioralReportForCGTInExcel(uniqueCandidateIds));
			} else {
				status = new Gson().toJson(behaviouralReport.generateBehavioralReportForExcel(qpID, uniqueCandidateIds,
						resultProcessingRuleDo.getBehaviouralTestType()));
			}
		}
		return status;
	}

	/**
	 * @param optionCount
	 * @param defaultAnswerOption
	 * @return
	 */
	public String getDefaultAnswer(int optionCount, DefaultAnswerOptionEnum defaultAnswerOption) {
		String defaultAnswer = "";
		switch (defaultAnswerOption) {
			case FIRST:
				defaultAnswer = RpsConstants.CHOICE + defaultAnswerOption.getValue();
				break;
			case LAST:
				defaultAnswer = RpsConstants.CHOICE + (optionCount + defaultAnswerOption.getValue());
				break;
			case SECOND:
				defaultAnswer = RpsConstants.CHOICE + defaultAnswerOption.getValue();
				break;
			case SECONDLAST:
				defaultAnswer = RpsConstants.CHOICE + (optionCount + defaultAnswerOption.getValue());
				break;
		}
		return defaultAnswer;
	}

	/**
	 * Gets view Qp = Multiple Questions Per Page Grid data.
	 *
	 * @param uniqueCandidateId
	 * @param assessmentCode
	 * @param qpId
	 * @return
	 */
	public String exportQuestionPaperView(Integer uniqueCandidateId, String assessmentCode, Integer qpId) {
		logger.debug("---IN--getQuestionPaperViewPerPageGrid()-----");
		String multiQsPerPageJson = RpsConstants.EMPTY_JSON;
		// check for the mandatory parameters
		if (uniqueCandidateId == null || uniqueCandidateId == 0 || assessmentCode == null) {
			logger.error("Missing mandatory parameters, uniqueCandidateId, assessmentCode");
			return multiQsPerPageJson;
		}
		RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationService
				.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsMasterAssociation == null) {
			logger.error("No candidate have taken exam for open event in selected date range");
			return multiQsPerPageJson;
		}
		if (qpId == null || qpId == 0) {
			if (rpsMasterAssociation.getCandidateResponses() != null
					|| rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
					|| rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
				qpId = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
			}
		}
		Map<Integer, RpsMasterAssociation> uniqueIdToMasterAssnMap = new HashMap<>();
		// for (RpsMasterAssociation rpsMasterAssociation :
		// rpsMasterAssociationsList)
		uniqueIdToMasterAssnMap.put(rpsMasterAssociation.getUniqueCandidateId(), rpsMasterAssociation);

		List<RpsCandidateResponseLite> rpsCandidateResponseLitesList = rpsCandidateResponseService
				.findAllCandRespLiteByMatserAssnIdsAndQpId(uniqueIdToMasterAssnMap.keySet(), qpId);
		if (rpsCandidateResponseLitesList == null || rpsCandidateResponseLitesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return multiQsPerPageJson;
		}

		Map<Integer, RpsCandidate> cidToCandidateMap = new HashMap<>();
		List<RpsCandidate> rpsCandidatesList = rpsMasterAssociationService
				.findAllCandidateByUniqueIds(uniqueIdToMasterAssnMap.keySet());
		if (rpsCandidatesList == null || rpsCandidatesList.isEmpty()) {
			logger.error("No candidate responses have been received in selected date range");
			return multiQsPerPageJson;
		}
		for (RpsCandidate rpsCandidate : rpsCandidatesList)
			cidToCandidateMap.put(rpsCandidate.getCid(), rpsCandidate);
		String eventCode = null;
		if (rpsCandidatesList.get(0) != null && rpsCandidatesList.get(0).getRpsEvent() != null
				&& rpsCandidatesList.get(0).getRpsEvent().getEventCode() != null)
			eventCode = rpsCandidatesList.get(0).getRpsEvent().getEventCode();
		ResponseReportWrapper responseReportWrapper;
		responseReportWrapper = responseReportUtility.getResponseReportGridData(rpsCandidateResponseLitesList,
				uniqueIdToMasterAssnMap, cidToCandidateMap, true);
		multiQsPerPageJson = gson.toJson(responseReportWrapper);
		logger.debug("---OUT--getQuestionPaperViewPerPageGrid()-----");
		return getQuestionPaperViewPerPageOnUniqueCandidateId(uniqueCandidateId, eventCode, assessmentCode,
				multiQsPerPageJson, qpId);
		// return multiQsPerPageJson;
	}

	/**
	 * get view QP = Multiple Questions Per Page On UniqueCandidateId
	 *
	 * @param uniqueCandidateId
	 * @param assessmentCode
	 * @return
	 * @throws org.json.simple.parser.ParseException
	 * @throws RpsException
	 * @see
	 * @since Apollo v2.0
	 */
	public String getQuestionPaperViewPerPageOnUniqueCandidateId(Integer uniqueCandidateId, String eventCode,
			String assessmentCode, String multiQsPerPageJson, Integer qpId) {
		logger.info("---IN--getQuestionPaperViewPerPageOnUniqueCandidateId()---- with params uniqueCandidateId: {}",
				uniqueCandidateId + " assessmentCode = " + assessmentCode);
		int questionCount = 0;
		List<ResponseMatrixEntity> responseMatrixEntitiesList = new ArrayList<>();
		// check for the mandatory parameters
		if (uniqueCandidateId == null || uniqueCandidateId == 0 || assessmentCode == null || assessmentCode.isEmpty()) {
			logger.error("Missing mandatory parameters, uniqueCandidateId, assessmentCode");
			return multiQsPerPageJson;
		}
		questionCount = getResponseMatrixForQP(uniqueCandidateId, assessmentCode, questionCount,
				responseMatrixEntitiesList);
		if (questionCount < 0)
			return (gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate DOCX, Because Extended QP is not present")));
		return downloadQPViewPdf(eventCode, assessmentCode, uniqueCandidateId, gson.toJson(responseMatrixEntitiesList),
				multiQsPerPageJson, qpId);
	}

	private int getResponseMatrixForQP(Integer uniqueCandidateId, String assessmentCode, int questionCount,
			List<ResponseMatrixEntity> responseMatrixEntitiesList) {
		RpsCandidateResponseLite rpsCandidateResponseLite = rpsCandidateResponseService
				.getCandidateResponseLiteByMasterAsson(uniqueCandidateId);
		try {
			RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService
					.findOne(rpsCandidateResponseLite.getRpsQuestionPaper());
			questionCount = responseReportUtility.getCandidateResponsesByShuffleSeq(rpsCandidateResponseLite,
					rpsQuestionPaper, responseMatrixEntitiesList, true);
		} catch (org.json.simple.parser.ParseException | RpsException e) {
			logger.error("Exception while executing getQuestionPaperViewPerPageOnUniqueCandidateId...", e);
		}
		return questionCount;
	}

	/**
	 * download QP View Pdf
	 *
	 * @param assessmentCode
	 * @param candidateId
	 * @param responseMatrixEntitiesList
	 * @return
	 * @see
	 * @since Apollo v2.0
	 */
	public String downloadQPViewPdf(String eventCode, String assessmentCode, Integer candidateId,
			String responseMatrixEntitiesList, String multiQsPerPageJson, Integer qpId) {
		logger.debug("---IN--downloadQPViewPdf()-----");
		FileExportExportEntity fileExportExportEntity = null;
		String divisionCode = null;
		String customerCode = null;
		String imagesFolderName = null;
		String packId = null;
		String apolloHome = FilenameUtils.separatorsToSystem(userHome);
		if (eventCode != null) {
			RpsDivision rpsDivision = rpsEventService.findRpsDivisionByEvent(eventCode);
			if (rpsDivision != null) {
				divisionCode = rpsDivision.getDivisionCode();
				if (rpsDivision.getRpsCustomer() != null) {
					customerCode = rpsDivision.getRpsCustomer().getCustomerCode();
				}
			}
		}
		if (qpId != null)
			packId = rpsQuestionPaperService.getQpPackIdqpCode(qpId);

		if (rQpSectionMap != null && !this.rQpSectionMap.containsKey(assessmentCode))
			this.populateQpSectionMap(assessmentCode);
		Map<String, String> sectionNameMap = new HashMap<>();
		List<RpsQpSection> qpSectionList = this.rQpSectionMap.get(assessmentCode);
		if (qpSectionList != null && !qpSectionList.isEmpty()) {
			for (RpsQpSection currentQqpSection : qpSectionList) {
				sectionNameMap.put(currentQqpSection.getSecIdentifier(), currentQqpSection.getTitle());
			}
		}
		// /apollo_home/rps/C000999/D000941/E005936/QPACK/Qpack_A2650_Qpt_10857_Group9_Version_1/questions_A00287210855_8_1/HINDI/resources/images
		imagesFolderName = apolloHome + File.separator + customerCode + File.separator + divisionCode + File.separator
				+ eventCode + File.separator + RpsConstants.PackType.QPACK + File.separator + packId;

		File imagesFolder = new File(imagesFolderName);
		if (imagesFolder.exists()) {
			File[] listOfFiles = imagesFolder.listFiles();
			for (File file : listOfFiles) {
				String fileName = file.getName();
				if (file.isDirectory() && fileName.contains(RpsConstants.questions)) {
					imagesFolderName = imagesFolderName + File.separator + fileName;
				}
			}
		}
		String exportToFolder = userHome + File.separator + ExcelConstants.QUESTION_PAPER_VIEW_FOLDER;
		String candidateFile = File.separator + File.separator + ExcelConstants.CANDIDATEID + candidateId + "_"
				+ System.currentTimeMillis();
		File path = new File(exportToFolder);
		if (!path.isDirectory()) {
			if (path.mkdirs())
				logger.debug("path created for dir:-" + path);
			else
				logger.debug("Unable to Create Directoty for Path:-" + path);
		}

		// find images containing folder

		logger.debug("Final path created for dir:-" + path + candidateFile + ".pdf");
		path = new File(path + candidateFile + ".pdf");
		if (path.isFile())
			logger.debug("path created for dir:-" + path);
		else
			logger.debug("Unable to Create Directoty for Path:-" + path);
		try {
			CandidateScoreCardGenerator.qpViewPdfGenerator(path.toString(), imagesFolderName,
					responseMatrixEntitiesList, multiQsPerPageJson, sectionNameMap, exportToFolder, scoreObtained,
					correctAnswer, candidateAnswer, htmltopdftool);
			/**
			 * commenting this line as docx convertion is not required
			 */
			// getQPViewDocxFileFromHTML(imageGerenationPath + File.separator + candidateFile + ".html");

		} catch (DocumentException | IOException | JAXBException | Docx4JException e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate pdf");
			logger.error("ERROR: Exception -- " + e);
		}
		if (deleteHtmlFiles.equalsIgnoreCase("yes"))
			deleteHtmlFilesIfSuccessfulInDocCreation(exportToFolder + candidateFile);
		fileExportExportEntity =
				new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, path.toString(), "DOCX generation has success");

		logger.debug("---OUT--downloadQPViewPdf()-----");
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * delete Html Files If Successful In Word Doc Creation
	 *
	 * @param exportToFolder
	 * @see
	 * @since Apollo v2.0
	 */
	private void deleteHtmlFilesIfSuccessfulInDocCreation(String exportToFolder) {
		String htmlFileName = exportToFolder + ".html";
		String xhtmlFileName = exportToFolder + ".xhtml";
		if (new File(htmlFileName).exists())
			FileUtils.deleteQuietly(new File(htmlFileName));
		if (new File(xhtmlFileName).exists())
			FileUtils.deleteQuietly(new File(xhtmlFileName));
	}

	/**
	 * import Rps Component Excel from UI on event code
	 *
	 * @param eventCode
	 * @param filePath
	 * @return
	 */
	public String importRpsCandidateMTids(String eventCode, String assessmentCode, String filePath) {
		logger.info("----IN--- importRpsCandidateMTids");
		FileExportExportEntity fileExportExportEntity = null;
		if (eventCode == null || eventCode.isEmpty() || assessmentCode == null || assessmentCode.isEmpty()
				|| filePath == null || filePath.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + "assessmentCode: " + assessmentCode
					+ "filePath: " + filePath);
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);
		}
		try {
			Set<String> candidateMTids = readComponentExcelFile(filePath);
			if (candidateMTids.isEmpty()) {
				logger.error("Candidate Excel has no MT ids");
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			} else {
				// fetch list of candidateIds for assessmentCode
				List<CandidateDetails> candidateDetailsList =
						rpsMasterAssociationService.findRpsCandidateInfoByCandidateId1s(candidateMTids, eventCode);
				if (candidateDetailsList == null || candidateDetailsList.isEmpty()) {
					logger.error("Candidate Excel has no MT ids");
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
							"Fail to generate PDF");
					return gson.toJson(fileExportExportEntity);
				}
				// IMP generation of PDF's
				logger.info("----OUT--- importRpsCandidateMTids");
				Set<CandidateDetails> uniqueRpsCandidates = new LinkedHashSet<>(candidateDetailsList);
				return generatePdfForScoreReport(assessmentCode, null, true, new ArrayList<>(uniqueRpsCandidates));
			}
		} catch (IOException e) {
			logger.error("Exception occured while importing Candidate Excel", e);
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);
		}
	}

	/**
	 * read Component Excel File
	 *
	 * @param filePath
	 * @throws IOException
	 */
	private Set<String> readComponentExcelFile(String filePath) throws IOException {
		Set<String> candidateMTids = new HashSet<>();
		FileInputStream file = new FileInputStream(new File(filePath));
		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		// Get first/desired sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);
		// Iterate through each rows one by one
		Iterator<Row> rowIterator = sheet.iterator();
		rowIterator.next();

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			String mtId = row.getCell(0).toString();
			candidateMTids.add(mtId);
		}
		file.close();
		return candidateMTids;
	}

	/**
	 * Return Negative mark for the question
	 *
	 * @see
	 * @since Apollo v2.0
	 */
	public static double applyNegativeMarking(MarksAndScoreRules marksAndScoreRules, RpsQuestion question,
			ResponseMarkBean responseMarkBean) {
		//logger.debug("apply Negative Marking !!" + question.getQid());
		double negativeScore = 0.0;
		switch (marksAndScoreRules.getNegativeMarkEnum()) {
			case AS_GIVEN_BY_AUTHOR:
				if (question.getQuestionType().equals(QuestionType.FILL_IN_THE_BLANK.toString())
						|| question.getQuestionType().equals(QuestionType.DATA_ENTRY.toString())) {
					if (responseMarkBean != null) {
						if (responseMarkBean.getResponseNegativeMarks() != null)
							negativeScore = -(responseMarkBean.getResponseNegativeMarks());
					}
				} else {
					if (question.getNegativeScore() != null)
						negativeScore = -(question.getNegativeScore());
				}
				break;

			case PERCENTAGE:
				if (question.getQuestionType().equals(QuestionType.FILL_IN_THE_BLANK.toString())
						|| question.getQuestionType().equals(QuestionType.DATA_ENTRY.toString())) {
					if (responseMarkBean != null) {
						if (responseMarkBean.getResponsePositiveMarks() != null
								&& marksAndScoreRules.getNegativeMark() != null)
							negativeScore = -((responseMarkBean.getResponsePositiveMarks()
									* marksAndScoreRules.getNegativeMark()) / 100);
					}
				} else {
					if (question.getScore() != null && marksAndScoreRules.getNegativeMark() != null)
						negativeScore = -((question.getScore() * marksAndScoreRules.getNegativeMark()) / 100);
				}
				break;

			case CUSTOM:
				if (marksAndScoreRules.getNegativeMark() != null)
					negativeScore = -(marksAndScoreRules.getNegativeMark());
				break;

			default:
				negativeScore = 0.0;
				break;
		}
		//logger.debug("Return apply Negative Marking -> question =" + question.getQid() + " for NegativeMarkEnum = "
		//		+ marksAndScoreRules.getNegativeMarkEnum() + " so negativeScore = " + negativeScore);

		return negativeScore;
	}

	/**
	 * get QPView Docx File From HTML
	 *
	 * @param fileName
	 * @return
	 * @throws RpsException
	 */
	public String getQPViewDocxFileFromHTML(String fileName) throws RpsException {
		HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
		String message = null;
		String docxFilepath = null;
		try {
			logger.info("Inside getQPViewDocxFileFromHTML()");
			if (exportDocxURI != null && !exportDocxURI.isEmpty() && exportDocxMethodName != null
					&& !exportDocxMethodName.isEmpty()) {
				FileDownloadEntityDO downloadEntityDO = new FileDownloadEntityDO();
				downloadEntityDO.setDocumentPath(fileName);
				String param = gson.toJson(downloadEntityDO);

				logger.info("Sending download file path = {} and requesting QPD to convert into docx file",
						param + " uri = {}" + exportDocxURI + exportDocxMethodName);
				try {
					// Gets the Converted QP in docx format from QPD.
					docxFilepath = httpClientFactory.requestPostWithJson(exportDocxURI, exportDocxMethodName, param);
					logger.info("response from QPD = {}", docxFilepath);
					return docxFilepath;
				} catch (Exception ex) {
					String msg = "Exception while executing getQPViewDocxFileFromHTML...";
					logger.error(msg, ex);
					message += msg + ex.getMessage() + " at " + fileName + "\n";
					throw new RpsException(message);
				}
			} else {
				message = "Please check QPD server is not configured...";
				logger.error(message);
				throw new RpsException(message);
			}
		} catch (Exception ex) {
			String msg = "Exception while executing getQPViewDocxFileFromHTML...";
			logger.error(msg, ex);
			throw new RpsException(msg);
		}
	}

	public String getCandidateTopicLevelScoreDetailsForReport(Set<Integer> candidateId) {
		try {
			return new Gson().toJson(topicWiseExcelReport.getCandidateTopicLevelScoreDetailsForReport(candidateId));
		} catch (ParseException ex) {
			String msg = "Exception while executing getQPViewDocxFileFromHTML...";
			logger.error(msg, ex);
			return null;
		} catch (RpsException ex) {
			String msg = "Exception while executing getQPViewDocxFileFromHTML...";
			logger.error(msg, ex);
			return null;
		}
	}

	public String getCandidateTopicLevelScoreDetailsForMcgReport(Set<Integer> candidateId) {
		try {
			return new Gson().toJson(topicWiseExcelReport.getCandidateTopicLevelScoreDetailsForMcgReport(candidateId));
		} catch (ParseException ex) {
			String msg = "Exception while executing getQPViewDocxFileFromHTML...";
			logger.error(msg, ex);
			return null;
		} catch (RpsException ex) {
			String msg = "Exception while executing getQPViewDocxFileFromHTML...";
			logger.error(msg, ex);
			return null;
		}
	}

	@Transactional
	public String generateReportForRPSCInPDF(Integer uniqueCandidateId) {
		logger.debug("---IN--generateBehavioralReportForCSOQInPDF()---with params  uniqueCandidateIds: {}",
				uniqueCandidateId);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + ExcelConstants.SCORE_REPORT_FOLDER;
		int questionCount = 0;
		List<ResponseMatrixEntity> responseMatrixEntitiesList = new ArrayList<>();
		LinkedHashSet<com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity> commonObjResponseMatrixEntitiesList = new LinkedHashSet<>();

		List<CandidateDetails> candidates = rpsMasterAssociationService
				.findRpsCandidateDetailsInfoByUniqCandID(new HashSet<Integer>(Arrays.asList(uniqueCandidateId)));

		if (candidates == null || candidates.isEmpty()) {
			logger.error("No Candidate exists with the specified id");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);

		}
		CandidateDetails candidateDetails = candidates.get(0);
		ProvisionalCertificateEntity rpscReportEntity = new ProvisionalCertificateEntity();
		constructRpscReportEntity(rpscReportEntity, candidateDetails);

		List<RpsQuestion> questionList = rpsQuestionAssociationService
				.getQuestionsFromUniqueCandidateId(candidateDetails.getUniqueCandidateId());
		if (questionList == null || questionList.isEmpty()) {
			logger.error("No questions found for the question paper with uniq cand id :: {} ",
					candidateDetails.getUniqueCandidateId());
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);
		}
		questionCount = getResponseMatrixForQP(uniqueCandidateId, candidateDetails.getAssessmentCode(), questionCount,
				responseMatrixEntitiesList);

		if (questionCount < 0)
			return (gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate DOCX, Because Extended QP is not present")));

		constructResponseMatrixEntity(commonObjResponseMatrixEntitiesList, responseMatrixEntitiesList);
		getQuestonSerialNumb(commonObjResponseMatrixEntitiesList, uniqueCandidateId);
		rpscReportEntity.setResponseMatrixEntities(commonObjResponseMatrixEntitiesList);

		File path = null;
		path = new File(behavioralFolder);
		if (!path.isDirectory()) {
			path.mkdirs();
		}
		path = new File(path + File.separator + RpsConstants.RPSC + "_" + candidateDetails.getLoginId() + ".pdf");
		try {
			CandidateQPDetailsPDFGenerator.pdfGenerator(path.toString(), rpscReportEntity);
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
		}
		fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, path.toString(),
				"PDF generation has success");

		return gson.toJson(fileExportExportEntity);
	}

	private void getQuestonSerialNumb(
			LinkedHashSet<com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity> commonObjResponseMatrixEntitiesList,
			Integer uniqueCandidateId) {

		List<RpsQuestionAssociationLite> questionAssociations = rpsQuestionAssociationRepository
				.getAllLiteAssosicationForQuestionPaperByUniqCandId(uniqueCandidateId);

		Map<Integer, RpsQuestionAssociationLite> questionAssociationsMap = new HashMap<>();

		if (questionAssociations != null) {
			for (RpsQuestionAssociationLite rpsQuestionAssociationLite : questionAssociations) {
				questionAssociationsMap.put(rpsQuestionAssociationLite.getRpsQuestion().getQid(),
						rpsQuestionAssociationLite);
			}
		}

		for (com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity responseMatrixEntity : commonObjResponseMatrixEntitiesList) {
			RpsQuestionAssociationLite questionAssociationLite = questionAssociationsMap
					.get(responseMatrixEntity.getQuestionId());
			responseMatrixEntity.setQuestionSerialId(questionAssociationLite.getQuestionSequence());
		}
	}

	private LinkedHashSet<com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity> constructResponseMatrixEntity(
			LinkedHashSet<com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity> commonObjResponseMatrixEntitiesList,
			List<ResponseMatrixEntity> responseMatrixEntitiesList) {

		for (ResponseMatrixEntity responseMatrixEntity : responseMatrixEntitiesList) {
			if (responseMatrixEntity.getQuestionType()
					.equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
				com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity commonObjResponseMatrixEntity = new com.merittrac.apollo.acsrps.candidatescore.entities.ResponseMatrixEntity();

				commonObjResponseMatrixEntity.setQuestionType(responseMatrixEntity.getQuestionType());
				commonObjResponseMatrixEntity.setQuestionSeqId(responseMatrixEntity.getQuestionSeqId());
				commonObjResponseMatrixEntity.setQuestionCode(responseMatrixEntity.getQuestionCode());
				commonObjResponseMatrixEntity.setQuestionId(responseMatrixEntity.getQuestionId());
				String actualChoice = responseMatrixEntity.getActualSelectedChoice(),
						choiceOnTP = responseMatrixEntity.getCandidateSelectedChoiceOnTP();

				if (actualChoice.toLowerCase().contains("choice")) {
					actualChoice = ABCDXOptionsEnum.valueOf(actualChoice.toLowerCase()).getValue();
				} else {
					actualChoice = "Not Attempted";
				}
				if (choiceOnTP.toLowerCase().contains("choice")) {
					choiceOnTP = ABCDXOptionsEnum.valueOf(choiceOnTP.toLowerCase()).getValue();
				} else {
					choiceOnTP = "Not Attempted";
				}
				commonObjResponseMatrixEntity.setActualSelectedChoice(actualChoice);
				commonObjResponseMatrixEntity.setCandidateSelectedChoiceOnTP(choiceOnTP);
				commonObjResponseMatrixEntitiesList.add(commonObjResponseMatrixEntity);
			}

		}
		return commonObjResponseMatrixEntitiesList;
	}

	private void constructRpscReportEntity(ProvisionalCertificateEntity rpscReportEntity,
			CandidateDetails candidateDetails) {
		candidateDetails =
				getFillCandidateDetailsFromMIF(candidateDetails);

		StringBuffer candidateName = new StringBuffer("");
		if (candidateDetails.getFirstName() != null)
			candidateName.append(candidateDetails.getFirstName());
		else
			candidateName.append("");

		if (candidateDetails.getMiddleName() != null)
			candidateName.append(" " + candidateDetails.getMiddleName());
		else
			candidateName.append("");

		if (candidateDetails.getLastName() != null)
			candidateName.append(" " + candidateDetails.getLastName());
		else
			candidateName.append("");

		rpscReportEntity.setName(candidateName.toString());
		rpscReportEntity.setLoginId(candidateDetails.getLoginId());
		rpscReportEntity.setMembershipNum(
				candidateDetails.getCandidateId1() == null ? RpsConstants.NA : candidateDetails.getCandidateId1());
		String qpTitle = null;
		// get qptitle from uniq cand id
		String testName = qpTitle == null ? candidateDetails.getAssessmentName() : qpTitle;
		rpscReportEntity.setSubjectName(testName);
		if (candidateDetails.getTestCenterCity() == null) {
			if (candidateDetails.getVenueName() != null && !candidateDetails.getVenueName().isEmpty()) {
				rpscReportEntity.setExamCenter(candidateDetails.getVenueName());
			} else
				rpscReportEntity.setExamCenter(RpsConstants.NA);
		} else {
			rpscReportEntity.setExamCenter(candidateDetails.getTestCenterCity());
		}
		String convertedDate = RpsConstants.NA;
		if (candidateDetails.getTestStartTime() != null
				&& !candidateDetails.getTestStartTime().equals(RpsConstants.NA)) {
			Calendar dateInCalendar = TimeUtil.convertStringToCalender(candidateDetails.getTestStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			convertedDate = TimeUtil.convertTimeAsString(dateInCalendar.getTimeInMillis(), "dd/MM/yyyy");
		}
		rpscReportEntity.setExamDateInString(convertedDate);
	}

	public String candidateLoginToViewReportsForRPSC(String candidateUsername, String candidatePassword) {
		logger.debug("--IN-- candidateLoginToViewReportsForRPSC() with candidateUsername : " + candidateUsername
				+ " and pwd : ****");
		CandidateReportViewPageEntityWrapper candidateReportViewPageEntityWrapper = null;
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findByBpackLoginID(candidateUsername);
		if (rpsMasterAssociations == null || rpsMasterAssociations.isEmpty()) {
			candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.FAILED_STATUS,
					"Invalid Username");
			return gson.toJson(candidateReportViewPageEntityWrapper);
		}
		RpsMasterAssociation rpsMasterAssociationEntity = rpsMasterAssociations.get(0);
		RpsCandidate candidate = rpsMasterAssociationEntity.getRpsCandidate();
		if (candidate == null) {
			candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.FAILED_STATUS,
					"No Candidate details found with the specified username");
			return gson.toJson(candidateReportViewPageEntityWrapper);

		}
		if (!isValidPassword(candidatePassword, rpsMasterAssociationEntity.getLoginPwd(),
				candidate.getRpsEvent().getEventCode())) {
			candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.FAILED_STATUS,
					"Invalid Password with the specified username");
			return gson.toJson(candidateReportViewPageEntityWrapper);
		}
		candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.SUCCESS_STATUS,
				"Successfully Logged In");
		return gson.toJson(candidateReportViewPageEntityWrapper);
	}

	public String getCandidateDetailsToViewReportsForRPSC(String candidateUsername) {
		logger.debug("--IN-- getCandidateDetailsToViewReportsForRPSC() with candidateUsername : " + candidateUsername);
		CandidateReportViewPageEntityWrapper candidateReportViewPageEntityWrapper = null;
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findByBpackLoginID(candidateUsername);
		if (rpsMasterAssociations == null || rpsMasterAssociations.isEmpty()) {
			candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.FAILED_STATUS,
					"No Candidate details found with the specified username");
			return gson.toJson(candidateReportViewPageEntityWrapper);
		}
		RpsMasterAssociation rpsMasterAssociationEntity = rpsMasterAssociations.get(0);
		RpsCandidate candidate = rpsMasterAssociationEntity.getRpsCandidate();
		if (candidate == null) {
			candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper(RpsConstants.FAILED_STATUS,
					"No Candidate details found with the specified username");
			return gson.toJson(candidateReportViewPageEntityWrapper);

		}
		candidateReportViewPageEntityWrapper = new CandidateReportViewPageEntityWrapper();
		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails = rpsCandidateMIFDetailsService
				.findByUniqueCandidateId(rpsMasterAssociationEntity.getUniqueCandidateId());
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);

		if (mifForm == null) {
			candidateReportViewPageEntityWrapper.setFirstName(candidate.getFirstName());
			candidateReportViewPageEntityWrapper.setLastName(candidate.getLastName());
		} else {
			PersonalInfo personalInfo = mifForm.getPersonalInfo();
			if (personalInfo != null) {
			candidateReportViewPageEntityWrapper.setFirstName(personalInfo.getFirstName());
			candidateReportViewPageEntityWrapper.setLastName(personalInfo.getLastName());
			} else {
				candidateReportViewPageEntityWrapper.setFirstName(RpsConstants.NA);
				candidateReportViewPageEntityWrapper.setLastName(RpsConstants.NA);
			}
		}
		List<CandidateReportViewPageEntity> candidateReportViewPageEntities = new ArrayList<CandidateReportViewPageEntity>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			CandidateReportViewPageEntity candidateReportViewPageEntity = new CandidateReportViewPageEntity();
			candidateReportViewPageEntity
					.setAssessmentCode(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
			candidateReportViewPageEntity
					.setBatchCode(rpsMasterAssociation.getRpsBatchAcsAssociation().getRpsBatch().getBatchCode());
			candidateReportViewPageEntity.setExamDate(rpsMasterAssociation.getLoginTime());
			candidateReportViewPageEntity.setUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
			candidateReportViewPageEntities.add(candidateReportViewPageEntity);
		}
		candidateReportViewPageEntityWrapper.setCandidateReportViewPageEntities(candidateReportViewPageEntities);
		candidateReportViewPageEntityWrapper.setStatus(RpsConstants.SUCCESS_STATUS);
		candidateReportViewPageEntityWrapper.setErrorMsg("Successfully Fetched all details");

		return gson.toJson(candidateReportViewPageEntityWrapper);
	}

	/**
	 * decrypts the password from the database and compares with the password
	 * provided
	 *
	 * @param candProvidedPassword
	 * @param password
	 * @param eventCode
	 * @return
	 * @see
	 * @since Apollo v2.0
	 */
	private boolean isValidPassword(String candProvidedPassword, String password, String eventCode) {
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
			isvalid = candProvidedPassword.equals(decryptedPasword);
		} catch (ApolloSecurityException e) {
			logger.error("ApolloSecurityException while executing isValidPassword...", e);
		} finally {
			logger.info("password validation {}", isvalid);
		}
		return isvalid;

	}

	/**
	 * get Candidate TopicAndSection Level Score Details For Report On Event
	 *
	 * @param
	 * @return
	 * @see
	 * @since Apollo v2.0
	 */
	public String getCandidateTopicAndSectionLevelScoreDetailsReportOnEvent(String eventCode, String startDate,
			String endDate) {
		try {
			return new Gson().toJson(topicWiseExcelReport
					.getCandidateTopicAndSectionLevelScoreDetailsReportOnEvent(eventCode, startDate, endDate));
		} catch (ParseException | IOException | RpsException ex) {
			String msg = "Exception while executing getCandidateTopicAndSectionLevelScoreDetailsForReportOnEvent...";
			logger.error(msg, ex);
			return null;
		}

	}

	public void getCandidateScoresOnThirdyPartyEventEnabled() {
		logger.debug("--IN-- getCandidateScoresOnThirdyPartyEventEnabled()");
		List<RpsEvent> rpsEvents = rpsEventService.getAllActiveRpsThirdPartyEvents();
		if (rpsEvents == null) {
			logger.debug("No events found with ThirdyPartyEventEnabled rule");
			return;
		}
		for (RpsEvent rpsEvent : rpsEvents) {
			getCandidateScoresOnEventId(rpsEvent);
		}

		logger.debug("--OUT-- getCandidateScoresOnThirdyPartyEventEnabled() ");
		return;
	}

	public void getCandidateScoresOnThirdyPartyEventEnabledOnEventCode(String eventCode) {
		logger.debug("--IN-- getCandidateScoresOnThirdyPartyEventEnabled()");
		RpsEvent rpsEvent = rpsEventService.getRpsEventPerEventCode(eventCode);
		if (rpsEvent == null) {
			logger.debug("No events found with ThirdyPartyEventEnabled rule");
			return;
		}
		if (rpsEvent.getIsThirdPartyEvent() == 0) {
			logger.debug("No events found with ThirdyPartyEventEnabled rule");
			return;

		}
		getCandidateScoresOnEventId(rpsEvent);

		logger.debug("--OUT-- getCandidateScoresOnThirdyPartyEventEnabled() ");
		return;
	}

	/**
	 * get Candidate Scores On EventId
	 *
	 * @return
	 * @throws RpsException
	 * @see
	 * @since Apollo v2.0
	 */
	public String getCandidateScoresOnEventId(RpsEvent rpsEvent) {

		String eventName = rpsEvent.getEventName();
		eventCode = rpsEvent.getEventCode();
		logger.debug("--IN-- getCandidateScoresOnEventId() with eventCode = {}", eventCode);
		Gson gson = new GsonBuilder().serializeNulls().create();
		List<CandidateSectionScoresEntity> candidateSectionScoresEntities = null;
		CandidateResultWrapScoresEntity candidateResultWrapScoresEntity = new CandidateResultWrapScoresEntity();
		// check for the mandatory parameters
		if (eventName == null || eventName.isEmpty()) {
			logger.error("Missing mandatory parameters, eventId, eventCode");
			return gson.toJson(candidateSectionScoresEntities);
		}

		List<Integer> candidateIds = rpsMasterAssociationService
				.getCandidateListByEventCodeOnTestScoresDelivery(eventCode);
		if (candidateIds.isEmpty()) {
			logger.error("Did not find candidates, whose test scores aren't delivered for specified eventCODE = {}",
					eventCode);
			return gson.toJson(candidateSectionScoresEntities);
		}

		Set<Integer> candidateIdSet = new HashSet<>(candidateIds);
		List<CandidateDetails> candidates = rpsMasterAssociationService
				.findRpsCandidateDetailsAdditionalInfoByUniqueCandidateId(candidateIdSet);

		logger.debug(" Candidates Size for section scores is : {}", candidates.size());
		Map<Integer, List<CandidateDetails>> cidToCandidateDetailsMap = new HashMap<>();

		if (candidates == null || candidates.isEmpty()) {
			logger.error("No Candidate details exists with the specified id");
			return gson.toJson(candidateSectionScoresEntities);

		}

		// prepare Map of same candidateids with assessment
		for (CandidateDetails candidateDetails : candidates) {
			if (!cidToCandidateDetailsMap.containsKey(candidateDetails.getcId())) {
				cidToCandidateDetailsMap.put(candidateDetails.getcId(), new ArrayList<CandidateDetails>());
			}
			List<CandidateDetails> candidateDetailsList = cidToCandidateDetailsMap.get(candidateDetails.getcId());
			if (!candidateDetailsList.contains(candidateDetails)) {
				candidateDetailsList.add(candidateDetails);
			}
		}

		// iterate Map over cid and fill details according to assessments
		for (Map.Entry<Integer, List<CandidateDetails>> cidToCandidateDetails : cidToCandidateDetailsMap.entrySet()) {
			List<CandidateDetails> candidateDetailsList = cidToCandidateDetails.getValue();
			CandidateSectionScoresEntity candidateSectionScoresEntity = new CandidateSectionScoresEntity();
			for (CandidateDetails candidateDetails : candidateDetailsList) {
				Integer uniqueCandidateId = candidateDetails.getUniqueCandidateId();
				// get status entity
				RpsCandidateResponseStatusForThirdParty responseStatusForThirdParty = rpsCandidateResponseStatusForThirdPartyService
						.getTestScoresDeliveryByUniqueCandidateId(uniqueCandidateId);

				if (responseStatusForThirdParty != null
						&& (responseStatusForThirdParty.getCountOfFailureAttempts()) >= Integer
								.parseInt(countOfFailureAttemptsForTestScoresDelivery)) {
					logger.debug("Candidate details posting is skipped for this candidate with uniq cand id",
							uniqueCandidateId);
					continue;
				}

				// fill Candidate Section Scores Entity
				getCandidateSectionScoresEntity(eventName, candidateSectionScoresEntity, candidateDetails);

				//
				getSectionWeightageEntity(candidateDetails.getAssessmentCode());

				//
				Map<String, GroupSectionsWeightageScore> groupWeightageMap = new LinkedHashMap<>();
				if (sectionWeightageCutOffEntityMap != null && !sectionWeightageCutOffEntityMap.isEmpty()) {
					groupWeightageMap = this.calculateSectionWeightage(sectionWeightageCutOffEntityMap,
							candidateDetails.getAssessmentCode(), uniqueCandidateId);

					CandidateResultScoreEntity candidateResultScoreEntity = candidateSectionScoresEntity
							.getTestResults();
					candidateResultScoreEntity.setStatus(RpsConstants.COMPLETED_STATUS);

					if (applyGroupCutOff(groupWeightageMap, groupWeightageCutOffEntityMap))
						candidateResultScoreEntity.setFinalResult(RpsConstants.PASS_STATUS);
					else
						candidateResultScoreEntity.setFinalResult(RpsConstants.FAIL_STATUS);

				}
				// always post for one candidate
				candidateSectionScoresEntities = new ArrayList<>();

				candidateSectionScoresEntities.add(candidateSectionScoresEntity);
				candidateResultWrapScoresEntity.setCandidates(candidateSectionScoresEntities);
				String param = gson.toJson(candidateResultWrapScoresEntity);

				postResultsToURL(param, uniqueCandidateId);

				/**
				 * update candidates with test scores delivered to synergy
				 */
				logger.debug(
						"added candidateSectionScoresEntity for uniqcandid = {} and candidateSectionScoresEntity= {} ",
						uniqueCandidateId, candidateSectionScoresEntity);
			}

		}

		logger.debug("Final return candidateSectionScoresEntities : {}", candidateSectionScoresEntities);
		return gson.toJson(candidateResultWrapScoresEntity);
	}

	private void applyGroupCutOffForAllSectionsInaAssessment(
			Map<String, GroupSectionsWeightageScore> groupWeightageMapForCandidate,
			String masterGroupName,
			com.merittrac.apollo.excel.ScoreReportEntity scoreReportEntity2) {
		GroupWeightageCutOffEntity groupWeightageCutOffEntity = null;

		if(groupWeightageCutOffEntityMap!=null)
			groupWeightageCutOffEntity = groupWeightageCutOffEntityMap.get(masterGroupName);

		if (groupWeightageCutOffEntity != null)
			scoreReportEntity2.setTotalcutOffPerc(String.valueOf(groupWeightageCutOffEntity.getCutOff()));

		if (groupWeightageMapForCandidate != null && !groupWeightageMapForCandidate.isEmpty()) {

			GroupSectionsWeightageScore groupSectionsWeightageScore =
					groupWeightageMapForCandidate.get(masterGroupName);

			if (groupWeightageCutOffEntity != null) {
				double perc = 0;
				if (groupSectionsWeightageScore.getScore() != null && !groupSectionsWeightageScore.getScore().isEmpty()
						&& groupSectionsWeightageScore.getMaxScore() != null
						&& !groupSectionsWeightageScore.getMaxScore().isEmpty()) {
					perc = (Double.parseDouble(groupSectionsWeightageScore.getScore())
							/ Double.parseDouble(groupSectionsWeightageScore.getMaxScore())) * 100;
				}
				if (perc < groupWeightageCutOffEntity.getCutOff()) {
					scoreReportEntity2.setCutOffstatus(RpsConstants.NOT_CLEARED);
					return;
				} else
					scoreReportEntity2.setCutOffstatus(RpsConstants.CLEARED);
			}

			for (SectionCutoffWithStatusData sectionCutoffWithStatusData : groupSectionsWeightageScore
					.getSectionCutoffWithStatus().values()) {
				if (sectionCutoffWithStatusData.getSectionStatus().equalsIgnoreCase(RpsConstants.NOT_CLEARED)) {
					scoreReportEntity2.setCutOffstatus(RpsConstants.NOT_CLEARED);
					return;
				}
			}
			// }
		}
		return;
	}

	private boolean applyGroupCutOff(Map<String, GroupSectionsWeightageScore> groupWeightageMapForCandidate,
			Map<String, GroupWeightageCutOffEntity> groupDetailsMap) {
		boolean cleared = true;
		if (groupWeightageMapForCandidate != null && !groupWeightageMapForCandidate.isEmpty()) {
			for (Map.Entry<String, GroupSectionsWeightageScore> groupCutOffMapForCandidate : groupWeightageMapForCandidate
					.entrySet()) {
				GroupWeightageCutOffEntity groupWeightageCutOffEntityRule = groupDetailsMap
						.get(groupCutOffMapForCandidate.getKey());
				GroupSectionsWeightageScore groupSectionsWeightageScore = groupCutOffMapForCandidate.getValue();

				if (groupWeightageCutOffEntityRule != null) {
					double perc = 0;
					if (groupSectionsWeightageScore.getScore() != null
							&& !groupSectionsWeightageScore.getScore().isEmpty()
							&& groupSectionsWeightageScore.getMaxScore() != null
							&& !groupSectionsWeightageScore.getMaxScore().isEmpty()) {
						perc = (Double.parseDouble(groupSectionsWeightageScore.getScore())
								/ Double.parseDouble(groupSectionsWeightageScore.getMaxScore())) * 100;
					}
					if (perc < groupWeightageCutOffEntityRule.getCutOff()) {
						cleared = false;
						return cleared;
					}
				}

				for (SectionCutoffWithStatusData sectionCutoffWithStatusData : groupSectionsWeightageScore
						.getSectionCutoffWithStatus().values()) {
					if (sectionCutoffWithStatusData.getSectionStatus().equalsIgnoreCase(RpsConstants.NOT_CLEARED)) {
						cleared = false;
						return cleared;
					}
				}
			}
		}
		return cleared;
	}

	/**
	 * @param sectionWeightageCutOffEntityMap
	 * @param sectionWeightageCutOffEntityMap
	 * @return
	 */
	public Map<String, GroupSectionsWeightageScore> calculateSectionWeightage(
			Map<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntityMap, String assessmentCode,
			Integer uniqueCandidateId) {
		Type type = new TypeToken<HashMap<String, Double>>() {
		}.getType();
		String jsonForSecWiseMarks = rpsCandidateResponseService.getQpSectionJsonByCandUniqueId(uniqueCandidateId);
		//
		Map<String, RpsQpSection> secIdentifierToSectionMap = new LinkedHashMap<>();
		List<RpsQpSection> rpsQpSectionsList = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
		if (rpsQpSectionsList != null && !rpsQpSectionsList.isEmpty()) {
			for (RpsQpSection rpsQpSection : rpsQpSectionsList)
				secIdentifierToSectionMap.put(rpsQpSection.getSecIdentifier(), rpsQpSection);
		}
		//
		List<RpsSectionCandidateResponse> rpsSectionCandidateResponsesList = rpsSectionCandidateResponseService
				.findAllSecCandidateRespByUniqCandId(uniqueCandidateId);
		Map<String, RpsSectionCandidateResponse> secIdentifierToCandRespMap = new LinkedHashMap<>();
		if (rpsSectionCandidateResponsesList != null && !rpsSectionCandidateResponsesList.isEmpty()) {
			for (RpsSectionCandidateResponse rpsSectionCandidateResponse : rpsSectionCandidateResponsesList) {
				RpsQpSection rpsQpSection = secIdentifierToSectionMap
						.get(rpsSectionCandidateResponse.getSecIdentifier());
				secIdentifierToCandRespMap.put(rpsQpSection.getTitle(), rpsSectionCandidateResponse);
			}
		}
		Map<String, Double> jsonForSecWiseMarksMap = new Gson().fromJson(jsonForSecWiseMarks, type);

		// take info about groups with weightage calculated marks with section
		// wise cutoff percentage and status like
		// cleared or not
		Map<String, GroupSectionsWeightageScore> groupWeightageMap = new LinkedHashMap<>();
		Set<Map.Entry<String, List<SectionWeightageCutOffEntity>>> sectionWeightageCutOffEntityMapEntrySet = sectionWeightageCutOffEntityMap
				.entrySet();
		Iterator<Entry<String, List<SectionWeightageCutOffEntity>>> sectionWeightageCutOffEntityMapEntrySetItr = sectionWeightageCutOffEntityMapEntrySet
				.iterator();

		while (sectionWeightageCutOffEntityMapEntrySetItr.hasNext()) {
			GroupSectionsWeightageScore groupSectionsWeightageScore = new GroupSectionsWeightageScore();
			Entry<String, List<SectionWeightageCutOffEntity>> sectionWeightageCutOffEntityMapEntry = sectionWeightageCutOffEntityMapEntrySetItr
					.next();
			String sectionGroup = sectionWeightageCutOffEntityMapEntry.getKey();
			List<SectionWeightageCutOffEntity> sectionWeightageCutOffEntityList = sectionWeightageCutOffEntityMapEntry
					.getValue();
			Map<String, SectionCutoffWithStatusData> SectionCutoffWithStatus = new HashMap<>();
			double allSectMaxScore = 0;
			double candMaxSectScore = 0;
			for (SectionWeightageCutOffEntity eWeightageCutOffEntity : sectionWeightageCutOffEntityList) {
				double sectMaxScore = 0;
				double candSectScore = 0;
				double cutoffPercentage = 0;
				double candPerctForSection = 0;
				SectionCutoffWithStatusData sectionCutoffWithStatusData = new SectionCutoffWithStatusData();
				cutoffPercentage = eWeightageCutOffEntity.getCutOff();
				sectionCutoffWithStatusData.setCutoffPercentage(decimalFormat.format(cutoffPercentage));
				RpsSectionCandidateResponse sectionCandidateResponse = secIdentifierToCandRespMap
						.get(eWeightageCutOffEntity.getSectionName());
				if (sectionCandidateResponse != null) {
					sectMaxScore = jsonForSecWiseMarksMap.get(sectionCandidateResponse.getSecIdentifier());
					candSectScore = sectionCandidateResponse.getScore();
				} else {
					RpsQpSection rpsQpSection = rpsQpSectionRepository
							.findRpsQpSectionByTitle(eWeightageCutOffEntity.getSectionId(), assessmentCode);
					sectMaxScore = rpsQpSection.getSecScore();
				}
				allSectMaxScore += sectMaxScore;
				candMaxSectScore += candSectScore;
				candPerctForSection = (candSectScore * 100) / sectMaxScore;
				if (candPerctForSection >= cutoffPercentage)
					sectionCutoffWithStatusData.setSectionStatus(RpsConstants.CLEARED);
				else
					sectionCutoffWithStatusData.setSectionStatus(RpsConstants.NOT_CLEARED);
				SectionCutoffWithStatus.put(eWeightageCutOffEntity.getSectionName(), sectionCutoffWithStatusData);
			}
			groupSectionsWeightageScore.setMaxScore(decimalFormat.format(allSectMaxScore));
			groupSectionsWeightageScore.setScore(decimalFormat.format(candMaxSectScore));
			groupSectionsWeightageScore.setSectionCutoffWithStatus(SectionCutoffWithStatus);
			groupWeightageMap.put(sectionGroup, groupSectionsWeightageScore);
		}
		return groupWeightageMap;
	}

	private void postResultsToURL(String param, Integer uniqueCandidateId) {
		logger.info("postResultsToURL with = {}", param);
		String status = null;
		String errorMsg = null;
		try {
			if (urlToPostCandidateResultScores != null && !urlToPostCandidateResultScores.isEmpty()
					&& methodUrlToPostCandidateResultScores != null
					&& !methodUrlToPostCandidateResultScores.isEmpty()) {
				if (proxyPort.equals(0))
					proxyHost = null;
				status = HttpClientFactory.getInstance().requestPostWithJsonUsernamePassword(
						urlToPostCandidateResultScores, methodUrlToPostCandidateResultScores, param, userNameForWipro,
						passwordForWipro, proxyHost, proxyPort);
				logger.info("Status postCandidateResultScores url = {} with param = {} and status = {}",
						urlToPostCandidateResultScores, param, status);
			} else {
				logger.debug("URL is not proper i.e, URL={} and Mathod={} ", urlToPostCandidateResultScores,
						methodUrlToPostCandidateResultScores);
			}
		} catch (Exception ex) {
			logger.error("Exception while executing postCandidateResultScores ....", ex);
			errorMsg = ex.getMessage();
		}

		markAcknowledgementStatus(param, status, uniqueCandidateId, errorMsg);

	}

	private void markAcknowledgementStatus(String candidateSectionScoresEntitiesJson,
			String candidateResultAcknowledgementEntitiesJson, Integer uniqueCandidateId, String errorMsg) {
		logger.debug(
				"--IN-- markAcknowledgementStatus() candidateSectionScoresEntitiesJson = {} and candidateResultAcknowledgementEntitiesJson = {} ",
				candidateSectionScoresEntitiesJson, candidateResultAcknowledgementEntitiesJson);

		// get status entity
		RpsCandidateResponseStatusForThirdParty responseStatusForThirdParty = rpsCandidateResponseStatusForThirdPartyService
				.getTestScoresDeliveryByUniqueCandidateId(uniqueCandidateId);

		if (responseStatusForThirdParty == null) {
			responseStatusForThirdParty = new RpsCandidateResponseStatusForThirdParty(uniqueCandidateId, 0);
		}

		if (candidateResultAcknowledgementEntitiesJson == null) {
			/**
			 * update candidates with test scores delivered to synergy
			 */

			failureStatusUpdate(candidateSectionScoresEntitiesJson, errorMsg, responseStatusForThirdParty);
			logger.debug("UPDATE responseStatusForThirdParty for uniqcandid = {}  ", responseStatusForThirdParty);

		} else {
			CandidateResultAcknowledgementEntities candidateResultAcknowledgementEntities = gson
					.fromJson(candidateResultAcknowledgementEntitiesJson, CandidateResultAcknowledgementEntities.class);
			// populate common values
			responseStatusForThirdParty.setAcknowledgementReceived(candidateResultAcknowledgementEntitiesJson);
			responseStatusForThirdParty.setResponseJson(candidateSectionScoresEntitiesJson);
			responseStatusForThirdParty.setErrorMsg(errorMsg);

			if (candidateResultAcknowledgementEntities != null
					&& candidateResultAcknowledgementEntities.getCandidates() != null) {
				CandidateResultAcknowledgementEntity candidateResultAcknowledgementEntity = candidateResultAcknowledgementEntities
						.getCandidates().get(0);
				if (candidateResultAcknowledgementEntity == null) {
					failureStatusUpdate(candidateSectionScoresEntitiesJson, errorMsg, responseStatusForThirdParty);
					logger.debug(
							"UPDATED responseStatusForThirdParty for uniqcandid = {} , candidateResultAcknowledgementEntity is NULL ",
							responseStatusForThirdParty);
					return;
				}

				CandidateResultAcknowledgementStatus candidateResultAcknowledgementStatus = candidateResultAcknowledgementEntity
						.getUploadResults();

				if (candidateResultAcknowledgementStatus == null) {
					failureStatusUpdate(candidateSectionScoresEntitiesJson, errorMsg, responseStatusForThirdParty);
					logger.debug(
							"UPDATED responseStatusForThirdParty for uniqcandid = {} , candidateResultAcknowledgementStatus is NULL ",
							responseStatusForThirdParty);
					return;
				}

				if (candidateResultAcknowledgementStatus.getUploadStatus()
						.equalsIgnoreCase(RpsConstants.FAILURE_STATUS)) {
					/**
					 * update candidates with test scores delivered to synergy
					 */
					responseStatusForThirdParty.setIsTestScoresDelivered(0);
					responseStatusForThirdParty
							.setCountOfFailureAttempts(responseStatusForThirdParty.getCountOfFailureAttempts() + 1);
					responseStatusForThirdParty.setResponsePostRetryTimeWithFailure(Calendar.getInstance().getTime());

				} else if (candidateResultAcknowledgementStatus.getUploadStatus()
						.equalsIgnoreCase(RpsConstants.SUCCESS_STATUS)) {
					/**
					 * update candidates with test scores delivered to synergy
					 */
					responseStatusForThirdParty.setIsTestScoresDelivered(1);
					responseStatusForThirdParty.setResponsePostedTimeSuccessfully(Calendar.getInstance().getTime());

				} else {
					responseStatusForThirdParty.setIsTestScoresDelivered(0);
					responseStatusForThirdParty
							.setCountOfFailureAttempts(responseStatusForThirdParty.getCountOfFailureAttempts() + 1);
					responseStatusForThirdParty.setResponsePostRetryTimeWithFailure(Calendar.getInstance().getTime());
				}
				logger.debug("UPDATE responseStatusForThirdParty for uniqcandid = {}  ", responseStatusForThirdParty);
				rpsCandidateResponseStatusForThirdPartyService
						.saveRpsCandidateResponseStatusForThirdParty(responseStatusForThirdParty);
			} else {
				failureStatusUpdate(candidateSectionScoresEntitiesJson, errorMsg, responseStatusForThirdParty);
				logger.debug(
						"UPDATED responseStatusForThirdParty for uniqcandid = {} , candidateResultAcknowledgementEntity is NULL ",
						responseStatusForThirdParty);
				return;
			}
		}
		logger.debug("--OUT-- markAcknowledgementStatus() ");

	}

	private void failureStatusUpdate(String candidateSectionScoresEntitiesJson, String errorMsg,
			RpsCandidateResponseStatusForThirdParty responseStatusForThirdParty) {
		responseStatusForThirdParty
				.setCountOfFailureAttempts(responseStatusForThirdParty.getCountOfFailureAttempts() + 1);
		responseStatusForThirdParty.setResponsePostRetryTimeWithFailure(Calendar.getInstance().getTime());
		responseStatusForThirdParty.setResponseJson(candidateSectionScoresEntitiesJson);
		responseStatusForThirdParty.setIsTestScoresDelivered(0);
		responseStatusForThirdParty.setErrorMsg(errorMsg);
		logger.debug("UPDATE responseStatusForThirdParty for uniqcandid = {}  ", responseStatusForThirdParty);
		rpsCandidateResponseStatusForThirdPartyService
				.saveRpsCandidateResponseStatusForThirdParty(responseStatusForThirdParty);
	}

	private void getSectionWeightageEntity(String assessmentCode) {
		// boolean isDefaultGroup = false;
		RpsQuestionPaper rpPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);
		// Map<String, List<String>> qtypeSectionMap =
		// rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());
		resultProcessingRuleDo = scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper,
				assessmentCode);

		gradingEnabled = false;
		sectionGroupEnabled = false;
		gradingSchemeDos = null;
		sectionWeightageCutOffEntityMap = null;
		groupWeightageCutOffEntityMap = null;
		if (resultProcessingRuleDo != null) {
			gradingSchemeDos = resultProcessingRuleDo.getGradingSchemeDos();
			sectionWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupSectionMap();
			groupWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupDetailsMap();
		}

		if (gradingSchemeDos != null && !gradingSchemeDos.isEmpty())
			gradingEnabled = true;
		if (sectionWeightageCutOffEntityMap != null && !sectionWeightageCutOffEntityMap.isEmpty())
			sectionGroupEnabled = true;

		if (sectionWeightageCutOffEntityMap != null && sectionWeightageCutOffEntityMap.size() == 1) {
			// if (sectionWeightageCutOffEntityMap.containsKey("DEFAULTGROUP"))
			// isDefaultGroup = true;
		}
	}

	/**
	 * fill Candidate Section Scores Entity
	 *
	 * @param candidateSectionScoresEntity
	 * @param candidateDetails
	 * @see
	 * @since Apollo v2.0
	 */
	private void getCandidateSectionScoresEntity(String eventName,
			CandidateSectionScoresEntity candidateSectionScoresEntity, CandidateDetails candidateDetails) {
		logger.debug("--IN-- getCandidateSectionScoresEntity for UniqueCandidateId : {} and AssessmentCode : {}",
				candidateDetails.getUniqueCandidateId(), candidateDetails.getAssessmentCode());

		List<RpsSectionCandidateResponseLite> rpsCandidateTechnicalSectionResponses = null;
		Map<String, RpsSectionCandidateResponseLite> rpsCandidateTechnicalSectionResponsesMap = null;
		// get updated data from MIF
		candidateDetails = this.getFillCandidateDetailsFromMIF(candidateDetails);

		/**
		 * firstName
		 */
		String firstName = candidateDetails.getFirstName() == null ? RpsConstants.NA
				: (candidateDetails.getFirstName().equals(RpsConstants.NA) ? " " : candidateDetails.getFirstName());
		/**
		 * lastName
		 */
		String lastName = candidateDetails.getLastName() == null ? RpsConstants.NA
				: (candidateDetails.getLastName().equals(RpsConstants.NA) ? " " : candidateDetails.getLastName());
		/**
		 * CandidateName
		 */
		candidateSectionScoresEntity.setName(firstName + " " + lastName);

		/**
		 * Emailid
		 */
		candidateSectionScoresEntity
				.setEmail(candidateDetails.getEmailID1() == null ? RpsConstants.NA : candidateDetails.getEmailID1());
		/**
		 * Eventid
		 */
		candidateSectionScoresEntity.setEventId(eventName == null ? RpsConstants.NA : eventName);

		/**
		 * Domain
		 */
		candidateSectionScoresEntity.setDomain(
				candidateDetails.getIdentifier1() == null ? RpsConstants.NA : candidateDetails.getIdentifier1());

		/**
		 * Eventid
		 */
		candidateSectionScoresEntity.setSource(sourceNameForWipro == null ? RpsConstants.NA : sourceNameForWipro);

		/**
		 * WiproRegistrationNumber
		 */
		candidateSectionScoresEntity.setWiproRegistrationNumber(
				candidateDetails.getLoginId() == null ? RpsConstants.NA : candidateDetails.getLoginId());

		/**
		 * get section level scores for technical+Abilities Assessments => If
		 * topperFlag=N
		 */
		rpsCandidateTechnicalSectionResponses = rpsSectionCandidateResponseService
				.findAllCandidateSectionScoresByUniqCandId(candidateDetails.getAssessmentCode(),
						candidateDetails.getUniqueCandidateId());

		/**
		 * Add section level scores to Map
		 */
		if (rpsCandidateTechnicalSectionResponses != null && !rpsCandidateTechnicalSectionResponses.isEmpty()) {
			rpsCandidateTechnicalSectionResponsesMap = new HashMap<>();
			for (RpsSectionCandidateResponseLite rpsCandidateTechnicalSectionResponse : rpsCandidateTechnicalSectionResponses) {
				rpsCandidateTechnicalSectionResponsesMap.put(rpsCandidateTechnicalSectionResponse.getSecIdentifier(),
						rpsCandidateTechnicalSectionResponse);
			}
		}

		/**
		 * Add Assessment section Beans to Map
		 */
		List<RpsQpSection> rpsQpSections = rpsQpSectionService
				.findAllQpSectionsByAssmentCode(candidateDetails.getAssessmentCode());
		Map<String, RpsQpSection> rpsQpSectionMap = new HashMap<>();
		for (RpsQpSection rpsQpSection : rpsQpSections) {
			rpsQpSectionMap.put(rpsQpSection.getTitle(), rpsQpSection);
		}

		CandidateResultScoreEntity testResults = candidateSectionScoresEntity.getTestResults();
		if (testResults == null) {
			testResults = new CandidateResultScoreEntity();
		}
		/**
		 * set All section level scores for technical+Abilities Assessments to
		 * Bean
		 */
		if (rpsCandidateTechnicalSectionResponsesMap != null && !rpsCandidateTechnicalSectionResponsesMap.isEmpty()) {
			// construct Technical Section Scores
			constructTechnicalSectionScores(rpsQpSectionMap, testResults, rpsCandidateTechnicalSectionResponsesMap,
					candidateDetails.getUniqueCandidateId(), candidateDetails.getIdentifier1());
		}

		// get WCT Essay Content
		/**
		 * get WCT section level scores and Essay text
		 */
		// if
		// (candidateDetails.getAssessmentCode().equalsIgnoreCase(wctAssessmentCode))
		getWCTEssayContent(candidateDetails.getUniqueCandidateId(), testResults);

		fillBeanWithNAForNullValues(testResults);

		candidateSectionScoresEntity.setTestResults(testResults);

		// get Overall Score
		/**
		 * set overall scores and Essay text
		 */
		// getOverallScore(candidateSectionScoresEntity.getTestResults(),
		// candidateDetails.getUniqueCandidateId());

		logger.debug("updated updateTestScoresDeliveryByuniqueCandidateId to 1 for UniqueCandidateId={}",
				candidateDetails.getUniqueCandidateId());
	}

	private void fillBeanWithNAForNullValues(CandidateResultScoreEntity testResults) {
		if (testResults.getWrittenCommunicationComments() == null)
			testResults.setWrittenCommunicationComments(RpsConstants.NA);

		if (testResults.getWrittenCommunicationScore() == null)
			testResults.setWrittenCommunicationScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

		if (testResults.getAnalyticalScore() == null)
			testResults.setAnalyticalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

		if (testResults.getTechnicalScore() == null)
			testResults.setTechnicalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

		if (testResults.getVerbalScore() == null)
			testResults.setVerbalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

		if (testResults.getMathematicalScore() == null)
			testResults.setMathematicalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

		if (testResults.getOverallScore() == null)
			testResults.setOverallScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));

	}

	/**
	 * get WCT Essay Content
	 *
	 * @param testResults
	 * @see
	 * @since Apollo v2.0
	 */
	private void getWCTEssayContent(Integer uniqueCandidateId, CandidateResultScoreEntity testResults) {

		logger.debug("--IN-- getWCTEssayContent(). with uniqueCandidateId={}", uniqueCandidateId);

		Double wctScore = rpsWetDataEvaluationService.getTotalWETScoreForUniqCand(uniqueCandidateId);
		logger.debug("wct Score for uniqueCandidateId={} and wctScore={}", uniqueCandidateId, wctScore);

		if (wctScore == null) {
			testResults.setWrittenCommunicationScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));
			logger.debug(" Candidate WCT Section Scores NOT FOUND. Hence WCT scores are NA");
		} else {
			logger.debug("wct Score for uniqueCandidateId={} and wctScore={}", uniqueCandidateId, wctScore);
			testResults.setWrittenCommunicationScore(Double.toString(wctScore));
		}

		String candResponse = rpsCandidateResponseService.getOnlyCandidateResponseByMasterAsson(uniqueCandidateId);
		logger.debug("get candResponse for uniqueCandidateId={} and candResponse={}", uniqueCandidateId, candResponse);

		if (candResponse == null || candResponse.trim().isEmpty()) {
			logger.error("candResponse not found for uniqueCandidateId={} and candResponse={}", uniqueCandidateId,
					candResponse);
			testResults.setWrittenCommunicationComments(RpsConstants.NA);
			return;
		}
		Type type = new TypeToken<List<CandidateResponseEntity>>() {
		}.getType();
		List<CandidateResponseEntity> CandidateResponselist = gson.fromJson(candResponse, type);
		for (CandidateResponseEntity candidateResponseEntity : CandidateResponselist) {
			if (candidateResponseEntity.getQuestionType()
					.equalsIgnoreCase(QuestionType.WRITTEN_ENGLISH_TEST.toString())) {
				// candidateResponseEntity.getResponse();
				Map<String, String> responseMap = candidateResponseEntity.getResponse();
				String wetResponse = null;
				if (responseMap != null)
					wetResponse = responseMap.get(RpsConstants.WET_TYPE_KEY);

				if (wetResponse != null) {
					wetResponse = StringEscapeUtils.unescapeHtml4(wetResponse);
					testResults.setWrittenCommunicationComments(wetResponse);
				}
				logger.debug("WCT Response for uniqueCandidateId={} and wetResponse={}", uniqueCandidateId,
						wetResponse);
				// Only 1 WCT question as WIPRO's requirement, Hence Break
				break;
			}
		}
		logger.debug("--OUT-- getWCTEssayContent(). with uniqueCandidateId={} and candidateSectionScoresEntity={}",
				uniqueCandidateId, testResults);
	}

	/**
	 * Set Technical Section Scores
	 *
	 * @param rpsQpSectionMap
	 * @param candidateSectionScoresEntity
	 * @param rpsCandidateTechnicalSectionResponsesMap
	 * @see
	 * @since Apollo v2.0
	 */
	private void constructTechnicalSectionScores(Map<String, RpsQpSection> rpsQpSectionMap,
			CandidateResultScoreEntity candidateSectionScoresEntity,
			Map<String, RpsSectionCandidateResponseLite> rpsCandidateTechnicalSectionResponsesMap, Integer uniquecandid,
			String domainName) {
		logger.debug(
				"--IN-- constructTechnicalSectionScores(). for uniqCandidateid={} and  rpsCandidateTechnicalSectionResponsesMap={} "
						+ " and candidateSectionScoresEntity={}",
				uniquecandid, rpsCandidateTechnicalSectionResponsesMap, candidateSectionScoresEntity);

		RpsQpSection rpsVerbalSection = rpsQpSectionMap.get(verbalSectionName);
		RpsQpSection rpsQuantitativeSection = rpsQpSectionMap.get(quantitativeSectionName);
		RpsQpSection rpsLogicalSection = rpsQpSectionMap.get(logicalSectionName);
		RpsQpSection rpsTechnicalSection = rpsQpSectionMap.get(technicalSectionName);

		Double cumulativeScore = 0.0;
		if (rpsVerbalSection != null) {
			RpsSectionCandidateResponseLite verbalSectionScore = rpsCandidateTechnicalSectionResponsesMap
					.get(rpsVerbalSection.getSecIdentifier());
			logger.debug("Adding rpsVerbalSection scores with rpsVerbalSection={} and verbalSectionScore={}",
					rpsVerbalSection, verbalSectionScore);
			if (verbalSectionScore != null) {
				candidateSectionScoresEntity.setVerbalScore(Double.toString(verbalSectionScore.getScore()));
				cumulativeScore = cumulativeScore + verbalSectionScore.getScore();
			} else {
				candidateSectionScoresEntity.setVerbalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));
			}

		}
		double quantScore = 0;
		if (rpsQuantitativeSection != null) {
			RpsSectionCandidateResponseLite quantitativeSectionScore = rpsCandidateTechnicalSectionResponsesMap
					.get(rpsQuantitativeSection.getSecIdentifier());
			logger.debug(
					"Adding rpsMathematicalSection scores with rpsMathematicalSection={} and MathematicalSectionScore={}",
					rpsQuantitativeSection, quantitativeSectionScore);
			if (quantitativeSectionScore != null) {
				candidateSectionScoresEntity.setMathematicalScore(Double.toString(quantitativeSectionScore.getScore()));
				quantScore = quantitativeSectionScore.getScore();
				cumulativeScore = cumulativeScore + quantitativeSectionScore.getScore();
			} else {
				candidateSectionScoresEntity.setMathematicalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));
			}

		}

		double logicalScore = 0;
		if (rpsLogicalSection != null) {
			RpsSectionCandidateResponseLite logicalSectionScore = rpsCandidateTechnicalSectionResponsesMap
					.get(rpsLogicalSection.getSecIdentifier());
			logger.debug(
					"Adding rpsAnanlyticalSection scores with rpsAnanlyticalSection={} and ananlyticalSectionScore={}",
					rpsLogicalSection, logicalSectionScore);
			if (logicalSectionScore != null) {
				logicalScore = logicalSectionScore.getScore();
				cumulativeScore = cumulativeScore + logicalSectionScore.getScore();
			}
		}

		// If Domain name is specified with Combined Analytical Scores
		if (domainName != null && domainName.equalsIgnoreCase(domainNameForWiproWithCombinedAnalyticalScores)) {
			logger.debug("analytical score = quantScore {} + logicalScore = {} ", quantScore, logicalScore);
			candidateSectionScoresEntity.setAnalyticalScore(Double.toString(quantScore + logicalScore));
		} else {
			// else only Logical Score
			logger.debug("analytical score = logicalScore = {} ", logicalScore);
			candidateSectionScoresEntity.setAnalyticalScore(Double.toString(logicalScore));
		}
		if (rpsTechnicalSection != null) {
			RpsSectionCandidateResponseLite technicalSectionScore = rpsCandidateTechnicalSectionResponsesMap
					.get(rpsTechnicalSection.getSecIdentifier());
			logger.debug("Adding rpsTechnicalSection scores with rpsTechnicalSection={} and technicalSectionScore={}",
					rpsTechnicalSection, technicalSectionScore);
			if (technicalSectionScore != null) {
				candidateSectionScoresEntity.setTechnicalScore(Double.toString(technicalSectionScore.getScore()));
				cumulativeScore = cumulativeScore + technicalSectionScore.getScore();
			} else {
				candidateSectionScoresEntity.setTechnicalScore(Double.toString(RpsConstants.DOUBLE_ZERO_VALUE));
			}

		}

		logger.debug("Calculating Cumulative_Score with Cumulative_Score={}", cumulativeScore);
		candidateSectionScoresEntity.setOverallScore(Double.toString(cumulativeScore));

		logger.debug(
				"--OUT-- constructTechnicalSectionScores(). for uniqCandidateid={} and  rpsCandidateTechnicalSectionResponsesMap={} "
						+ " and candidateSectionScoresEntity={}",
				uniquecandid, rpsCandidateTechnicalSectionResponsesMap, candidateSectionScoresEntity);
	}

	@Transactional
	public String generateReportForTTInPDF(Integer uniqueCandidateId) {
		logger.debug("---IN--generateReportForTTInPDF()---with params  uniqueCandidateIds: {}", uniqueCandidateId);
		FileExportExportEntity fileExportExportEntity = null;
		String scoreReportFolder = userHome + File.separator + ExcelConstants.SCORE_REPORT_FOLDER;
		List<CandidateDetails> candidates = rpsMasterAssociationService
				.findRpsCandidateDetailsInfoByUniqCandID(new HashSet<Integer>(Arrays.asList(uniqueCandidateId)));
		if (candidates == null || candidates.isEmpty()) {
			logger.error("No Candidate exists with the specified id");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);

		}
		CandidateDetails candidateDetails = candidates.get(0);
		ProvisionalCertificateEntity rpscReportEntity = new ProvisionalCertificateEntity();
		constructRpscReportEntity(rpscReportEntity, candidateDetails);
		Map<String, String> questionTextToResponseText = null;
		try {
			questionTextToResponseText = getTTEvaluationText(candidateDetails.getUniqueCandidateId());
		} catch (RpsException e) {
			logger.error("RpsException while executing getTTEvaluationText...", e);
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
			return gson.toJson(fileExportExportEntity);
		}
		rpscReportEntity.setQuestionTextToResponseText(questionTextToResponseText);

		File path = null;
		path = new File(scoreReportFolder);
		if (!path.isDirectory()) {
			path.mkdirs();
		}
		path = new File(path + File.separator + RpsConstants.TT_REPORT + "_" + candidateDetails.getLoginId() + "_"
				+ Calendar.getInstance().getTimeInMillis());
		try {
			CandidateTypingTestDetailsPDFGenerator.pdfGeneratorWithHTMLimageAndPDFText(path.toString(),
					rpscReportEntity);
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
					"Fail to generate PDF");
		}
		fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, path.toString() + ".pdf",
				"PDF generation has success");

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * get TT Evaluation Text
	 *
	 * @param uniqueCandidateId
	 * @throws RpsException
	 * @see
	 * @since Apollo v2.0
	 */
	private Map<String, String> getTTEvaluationText(Integer uniqueCandidateId) throws RpsException {
		logger.debug("--IN-- getTTEvaluationText() with uniq cand id :: {} ", uniqueCandidateId);
		Map<String, String> questionTextToResponseText = new HashMap<>();
		String candResponse = rpsCandidateResponseService.getOnlyCandidateResponseByMasterAsson(uniqueCandidateId);
		List<RpsQuestion> questionList = rpsQuestionAssociationService
				.getTTQuestionsFromUniqueCandidateId(uniqueCandidateId, QuestionType.TYPING_TEST.toString());
		if (questionList == null || questionList.isEmpty()) {
			logger.error("No TT questions found for the question paper with uniq cand id :: {} ", uniqueCandidateId);
			return questionTextToResponseText;
			// throw new RpsException("No TT questions found for the question
			// paper with uniq cand id ");
		}
		logger.debug("get candResponse for uniqueCandidateId={} and candResponse={}", uniqueCandidateId, candResponse);
		if (candResponse == null || candResponse.trim().isEmpty()) {
			logger.error("candResponse not found for uniqueCandidateId={} and candResponse={}", uniqueCandidateId,
					candResponse);
			return questionTextToResponseText;
			// throw new RpsException("candResponse not found for the
			// uniqueCandidateId");
		}
		Type type = new TypeToken<List<CandidateResponseEntity>>() {
		}.getType();
		List<CandidateResponseEntity> CandidateResponselist = gson.fromJson(candResponse, type);
		Map<String, CandidateResponseEntity> questToCandRespMap = new HashMap<>();
		for (CandidateResponseEntity candidateResponseEntity : CandidateResponselist) {
			questToCandRespMap.put(candidateResponseEntity.getQuestionID(), candidateResponseEntity);
		}

		for (RpsQuestion rpsQuestion : questionList) {
			CandidateResponseEntity candidateResponseEntity = questToCandRespMap.get(rpsQuestion.getQuestId());
			if (candidateResponseEntity == null) {
				logger.error("TT candResponse not found for uniqueCandidateId={} and candResponse={}",
						uniqueCandidateId, candResponse);
				return questionTextToResponseText;
				// throw new RpsException("candResponse not found for the
				// uniqueCandidateId");
			} else {
				/**
				 * If candidate response has a question language then we will
				 * check for specific rps language question if language is not
				 * there then will proceed it will only pick question with
				 * language
				 */
				if (candidateResponseEntity.getLanguage() != null
						&& (!candidateResponseEntity.getLanguage().equals(rpsQuestion.getLanguage())))
					continue;
				Map<String, String> responseMap = candidateResponseEntity.getResponse();
				String ttResponse = null;
				if (responseMap != null)
					ttResponse = responseMap.get(RpsConstants.TYPING_TEST_KEY);

				if (ttResponse != null) {
					ttResponse = StringEscapeUtils.unescapeHtml4(ttResponse);
				}

				questionTextToResponseText.put(rpsQuestion.getQtext(), ttResponse);
				logger.debug("TT Response for uniqueCandidateId={} and ttResponse={}", uniqueCandidateId, ttResponse);
			}
		}
		return questionTextToResponseText;
	}

	/**
	 * get Candidate Scores On EventId
	 *
	 * @return
	 * @throws Exception
	 * @throws RpsException
	 * @see
	 * @since Apollo v2.0
	 */
	public String getCandidateBehaviouralScores() {
		logger.debug("--IN-- getCandidateBehaviouralScores() for eventcode from property file : {} ", eventCode);
		List<CandidateBehaviouralScoresEntity> candidateBehaviouralScoresEntities = new ArrayList<>();
		// check for the mandatory parameters
		if (eventCode == null || eventCode.isEmpty()) {
			logger.error("Missing mandatory parameters, eventCode");
			return gson.toJson(candidateBehaviouralScoresEntities);
		}

		List<Integer> candidateIds = rpsMasterAssociationService
				.getCandidateListByEventCodeOnTestScoresDelivery(eventCode);
		if (candidateIds.isEmpty()) {
			logger.error("Did not find candidates, whose test scores aren't delivered for specified eventId");
			return gson.toJson(candidateBehaviouralScoresEntities);
		}

		Set<Integer> candidateIdSet = new HashSet<>(candidateIds);
		List<CandidateDetails> candidates = rpsMasterAssociationService
				.findRpsCandidateDetailsAdditionalInfoByUniqueCandidateId(candidateIdSet);

		logger.debug(" Candidates Size for section scores is : {}", candidates.size());
		Map<Integer, List<CandidateDetails>> cidToCandidateDetailsMap = new HashMap<>();

		if (candidates == null || candidates.isEmpty()) {
			logger.error("No Candidate details exists with the specified id");
			return gson.toJson(candidateBehaviouralScoresEntities);

		}

		// prepare Map of same candidateids with assessment
		for (CandidateDetails candidateDetails : candidates) {
			if (!cidToCandidateDetailsMap.containsKey(candidateDetails.getUniqueCandidateId())) {
				cidToCandidateDetailsMap.put(candidateDetails.getUniqueCandidateId(),
						new ArrayList<CandidateDetails>());
			}
			List<CandidateDetails> candidateDetailsList = cidToCandidateDetailsMap
					.get(candidateDetails.getUniqueCandidateId());
			if (!candidateDetailsList.contains(candidateDetails)) {
				candidateDetailsList.add(candidateDetails);
			}
		}

		// iterate Map over cid and fill details according to assessments
		for (Map.Entry<Integer, List<CandidateDetails>> cidToCandidateDetails : cidToCandidateDetailsMap.entrySet()) {
			List<CandidateDetails> candidateDetailsList = cidToCandidateDetails.getValue();
			CandidateBehaviouralScoresEntity candidateBehaviouralScoresEntity = new CandidateBehaviouralScoresEntity();
			for (CandidateDetails candidateDetails : candidateDetailsList) {
				// fill Candidate behavioural Section Scores Entity
				getCandidateBehaviouralScoresEntity(candidateBehaviouralScoresEntity, candidateDetails);
				Integer uniqueCandidateId = candidateDetails.getUniqueCandidateId();
				String reportJson = generateBehaviouralReportsBasidOnType(candidateDetails.getAssessmentCode(), 0,
						new HashSet<>(Arrays.asList(uniqueCandidateId)), false);
				try {
					candidateBehaviouralScoresEntity = getBehaviouralTestEntityDetails(reportJson,
							candidateBehaviouralScoresEntity, uniqueCandidateId);
				} catch (RpsException e) {
					logger.error("RpsException while executing getCandidateBehaviouralScores...", e);
				}

				if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
					// get status entity
					RpsCandidateResponseStatusForThirdParty responseStatusForThirdParty = rpsCandidateResponseStatusForThirdPartyService
							.getTestScoresDeliveryByUniqueCandidateId(uniqueCandidateId);
					if (responseStatusForThirdParty == null) {
						responseStatusForThirdParty = new RpsCandidateResponseStatusForThirdParty(uniqueCandidateId, 1,
								0, Calendar.getInstance().getTime(), null,
								gson.toJson(candidateBehaviouralScoresEntity), null, null);
					}
					rpsCandidateResponseStatusForThirdPartyService
							.saveRpsCandidateResponseStatusForThirdParty(responseStatusForThirdParty);
				}
			}
			candidateBehaviouralScoresEntities.add(candidateBehaviouralScoresEntity);
		}

		logger.debug("Final return candidateSectionScoresEntities : {}", candidateBehaviouralScoresEntities);
		return gson.toJson(candidateBehaviouralScoresEntities);
	}

	private CandidateBehaviouralScoresEntity getBehaviouralTestEntityDetails(String reportJson,
			CandidateBehaviouralScoresEntity candidateBehaviouralScoresEntity, Integer uniqueCandidateId)
			throws RpsException {
		if (reportJson == null || reportJson.isEmpty() || reportJson.equalsIgnoreCase(RpsConstants.NA)) {
			logger.debug("reportJson not found : ", reportJson);
			return candidateBehaviouralScoresEntity;
		}
		FileExportExportEntity fileExportExportEntity = gson.fromJson(reportJson, FileExportExportEntity.class);
		// get Base64 format for pdf
		String base64 = convertPdfToBase64Format(fileExportExportEntity);
		// set Base64 format to bean
		candidateBehaviouralScoresEntity.setReport(base64);
		logger.debug(
				"added candidateSectionScoresEntity for uniqcandid = {} and base64 = {} and candidateSectionScoresEntity= {} ",
				uniqueCandidateId, base64, candidateBehaviouralScoresEntity);
		List<CandidateBehaviouralSectionScoresEntity> sectionScores = new ArrayList<>();

		List<RpsBehaviouralTestCharacteristic> behaviouralTestCharacteristics = rpsBehaviouralTestCharacteristicService
				.findAll();
		Map<String, RpsBehaviouralTestCharacteristic> behaviouralTestCharacteristicsMap = new HashMap<String, RpsBehaviouralTestCharacteristic>();
		for (RpsBehaviouralTestCharacteristic rpsBehaviouralTestCharacteristic : behaviouralTestCharacteristics) {
			behaviouralTestCharacteristicsMap.put(rpsBehaviouralTestCharacteristic.getcId(),
					rpsBehaviouralTestCharacteristic);
		}
		String bTestScores = rpsCandidateResponseService.getCandBehaviouralScoresByUniqCandId(uniqueCandidateId);
		if (bTestScores == null || bTestScores.isEmpty()) {
			logger.debug("bTestScores not found in DB: ", bTestScores);
			return candidateBehaviouralScoresEntity;
		}
		Type type = new TypeToken<Map<String, Double>>() {
		}.getType();
		Map<String, Double> btScoreMap = gson.fromJson(bTestScores, type);
		Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
		}.getType();

		for (Map.Entry<String, Double> btScores : btScoreMap.entrySet()) {
			RpsBehaviouralTestCharacteristic rpCharacteristicSet = behaviouralTestCharacteristicsMap
					.get(btScores.getKey());
			CandidateBehaviouralSectionScoresEntity sectionScoresEntity = new CandidateBehaviouralSectionScoresEntity();
			sectionScoresEntity.setBehavioralCharacteristic(rpCharacteristicSet.getcId());
			sectionScoresEntity.setBehavioralCharacteristicName(rpCharacteristicSet.getcName());
			List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristicSet.getGrade(), gradeType);
			Double candidateScore = (btScores.getValue());
			String candidateGrade = this.generateBehaviouralGradeOnCandidateScore(gradeList, candidateScore);
			sectionScoresEntity.setCandidateGrade(candidateGrade);
			sectionScoresEntity.setCandidateScore(Double.toString(candidateScore));
			sectionScoresEntity.setGradingDetails(gradeList);
			sectionScores.add(sectionScoresEntity);
			candidateBehaviouralScoresEntity.setBehavioralTest(
					rpCharacteristicSet.getBtId().getBtName() + " (" + rpCharacteristicSet.getBtId().getBtId() + ")");
		}
		candidateBehaviouralScoresEntity.setSectionScores(sectionScores);

		return candidateBehaviouralScoresEntity;
	}

	private String convertPdfToBase64Format(FileExportExportEntity fileExportExportEntity) {
		String encodedBase64 = null;
		if (fileExportExportEntity != null && fileExportExportEntity.getStatus() != null
				&& fileExportExportEntity.getStatus().equalsIgnoreCase(RpsConstants.SUCCESS_STATUS)) {
			if (fileExportExportEntity.getFileLocation() == null
					|| fileExportExportEntity.getFileLocation().isEmpty()) {
				logger.debug("PDF File not found : ", fileExportExportEntity.getFileLocation());
				return encodedBase64;
			}
			File originalFile = new File(fileExportExportEntity.getFileLocation());
			try {
				FileInputStream fileInputStreamReader = new FileInputStream(originalFile);
				byte[] bytes = new byte[(int) originalFile.length()];
				fileInputStreamReader.read(bytes);
				encodedBase64 = new String(Base64.encodeBase64(bytes));

			} catch (FileNotFoundException e) {
				logger.error("FileNotFoundException at convertPdfToBase64Format() : ", e);
				e.printStackTrace();
				return encodedBase64;
			} catch (IOException e) {
				logger.error("IOException at convertPdfToBase64Format() : ", e);
				e.printStackTrace();
				return encodedBase64;
			}

		}
		return encodedBase64;
	}

	/**
	 * fill Candidate Section Scores Entity
	 *
	 * @param candidateBehaviouralScoresEntity
	 * @param candidateDetails
	 * @see
	 * @since Apollo v2.0
	 */
	private void getCandidateBehaviouralScoresEntity(CandidateBehaviouralScoresEntity candidateBehaviouralScoresEntity,
			CandidateDetails candidateDetails) {
		logger.debug("--IN-- getCandidateSectionScoresEntity for UniqueCandidateId : {} and AssessmentCode : {}",
				candidateDetails.getUniqueCandidateId(), candidateDetails.getAssessmentCode());
		// get updated data from MIF
		candidateDetails = getFillCandidateDetailsFromMIF(candidateDetails);
		/**
		 * firstName
		 */
		String firstName = candidateDetails.getFirstName() == null ? RpsConstants.NA
				: (candidateDetails.getFirstName().equals(RpsConstants.NA) ? " " : candidateDetails.getFirstName());
		/**
		 * lastName
		 */
		String lastName = candidateDetails.getLastName() == null ? RpsConstants.NA
				: (candidateDetails.getLastName().equals(RpsConstants.NA) ? " " : candidateDetails.getLastName());
		/**
		 * CandidateName
		 */
		candidateBehaviouralScoresEntity.setCandidateName(firstName + " " + lastName);
		/**
		 * DateofBirth
		 */
		candidateBehaviouralScoresEntity
				.setDateofBirth(candidateDetails.getDob() == null ? RpsConstants.NA : candidateDetails.getDob());
		/**
		 * emailId
		 */
		candidateBehaviouralScoresEntity
				.setEmailId(candidateDetails.getEmailID1() == null ? RpsConstants.NA : candidateDetails.getEmailID1());
		/**
		 * LoginId
		 */
		candidateBehaviouralScoresEntity
				.setLoginId(candidateDetails.getLoginId() == null ? RpsConstants.NA : candidateDetails.getLoginId());
		/**
		 * CompletionDate
		 */
		String completionDate = RpsConstants.NA;
		if (candidateDetails.getTestStartTime() != null
				&& !candidateDetails.getTestStartTime().equals(RpsConstants.NA)) {
			Calendar dateInCalendar = TimeUtil.convertStringToCalender(candidateDetails.getTestStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			completionDate = TimeUtil.convertTimeAsString(dateInCalendar.getTimeInMillis(), "dd/MM/yyyy");
		}
		candidateBehaviouralScoresEntity.setCompletionDate(completionDate);

		logger.debug("updated updateTestScoresDeliveryByuniqueCandidateId to 1 for UniqueCandidateId={}",
				candidateDetails.getUniqueCandidateId());
	}

	/**
	 * @param rpsCandidate
	 * @param rpsMasterAssociation
	 * @param rpsQuestionPaper
	 * @param behaviouralReportEntity
	 * @return
	 * @see
	 * @since Apollo v2.0
	 */
	private Set<RpsBehaviouralTestCharacteristic> getCandidateDetailsForBehaviouralReports(RpsCandidate rpsCandidate,
			RpsMasterAssociation rpsMasterAssociation, RpsQuestionPaper rpsQuestionPaper,
			BehaviouralReportEntity behaviouralReportEntity, String behavioralTestType, String dateInFormat) {

		behaviouralReportEntity.setBehaviouralType(behavioralTestType);
		behaviouralReportEntity.setLoginId(rpsMasterAssociation.getLoginID());
		behaviouralReportEntity.setMerittracId(
				rpsCandidate.getCandidateId1() == null ? RpsConstants.NA : rpsCandidate.getCandidateId1());

		String testName = RpsConstants.NA;
		if (rpsQuestionPaper != null)
			testName = rpsMasterAssociation.getRpsAssessment().getAssessmentName() + "_"
					+ rpsQuestionPaper.getSetCode();
		behaviouralReportEntity.setAssessmentName(testName);

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateInFormat);
		String convertedDate = RpsConstants.NA;
		if (rpsMasterAssociation.getTestStartTime() != null)
			convertedDate = dateFormat.format(rpsMasterAssociation.getTestStartTime());
		behaviouralReportEntity.setTestStartDate(convertedDate);
		RpsBehaviouralTest rpsBehaviouralTest = rpsBehaviouralTestRepository.findByBtId(behavioralTestType);
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = rpsBehaviouralTest
				.getRpsBehaviouralTestCharacteristics();
		String rpsCustomerName = rpsEventService.findCustomersByEventCode(rpsCandidate.getRpsEvent().getEventCode());
		if (rpsCustomerName != null)
			behaviouralReportEntity.setOrganization(rpsCustomerName);
		else
			behaviouralReportEntity.setOrganization(RpsConstants.NA);

		MIFForm mifForm = null;

		// get mif
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			behaviouralReportEntity.setTestCenter(
					rpsCandidate.getTestCenterCity() == null ? RpsConstants.NA : rpsCandidate.getTestCenterCity());
		} else {
			ContactInfo permanentAddress = mifForm.getPermanentAddress();
			if (permanentAddress != null) {
				behaviouralReportEntity.setTestCenter(
						permanentAddress.getTestCity() == null ? RpsConstants.NA : permanentAddress.getTestCity());
			}
			if (mifForm.getQualification() != null
					&& mifForm.getQualification().getQualificationLevelToDetailsMap() != null
					&& !mifForm.getQualification().getQualificationLevelToDetailsMap().isEmpty()) {
				NavigableMap<String, QualificationDetails> map = new TreeMap<String, QualificationDetails>(
						mifForm.getQualification().getQualificationLevelToDetailsMap());
				Entry<String, QualificationDetails> lastEntry = map.lastEntry();
				QualificationDetails qualificationDetails = lastEntry.getValue();
				if (qualificationDetails.getEducation() != null)
					behaviouralReportEntity.setEducationQualification(qualificationDetails.getEducation());
				else
					behaviouralReportEntity.setEducationQualification(RpsConstants.NA);
			} else
				behaviouralReportEntity.setEducationQualification(RpsConstants.NA);
		}
		String candidateName = pdfReportService.getCandidateFullName(rpsCandidate, mifForm);
		behaviouralReportEntity.setCandidateName(candidateName);
		return rpCharacteristicSet;
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForCPABasicInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForCPABasicInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.CPAB.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();
			behaviouralReportEntity.setBasic(true);
			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.CPAB.toString(), "dd-MMM-yyyy");

			// FRIN
			// String overallFRIN =

			// behaviouralReportEntity.setOverallFRIN(overallFRIN);
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());

			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, RpsQuestion> questionLanguageMap = rpsCandidateResponseService
					.getQuestionLanguageShuffleSequenseForCandidate(responseLite.getResponse(),
							responseLite.getShuffleSequence(),
							rpsMasterAssociation.getRpsAssessment().getAssessmentCode());
			behaviouralReportEntity.setCandidateCPAScore(Double.toString(responseLite.getCandidateScore()));

			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						0.0, gradeList);

			}

			getQuestionWiseScores(rpsMasterAssociation.getUniqueCandidateId(),
					rpsMasterAssociation.getRpsAssessment().getAssessmentCode(), BehavioralTestType.CPAB,
					behaviouralReportEntity, questionLanguageMap);

			findCandidateAlignment(behaviouralReportEntity);

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.CPAB.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CPAB.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}

		try {
			CPABasicAndAdvancedPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			CPABasicAndAdvancedPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForCPAAdvancedInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug(
				"---IN--generateBehavioralReportForCPAAdvancedInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.CPAA.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();
			behaviouralReportEntity.setBasic(false);
			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.CPAA.toString(), "dd-MMM-yyyy");

			// FRIN
			// String overallFRIN =

			// behaviouralReportEntity.setOverallFRIN(overallFRIN);
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());

			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, RpsQuestion> questionLanguageMap = rpsCandidateResponseService
					.getQuestionLanguageShuffleSequenseForCandidate(responseLite.getResponse(),
							responseLite.getShuffleSequence(),
							rpsMasterAssociation.getRpsAssessment().getAssessmentCode());

			behaviouralReportEntity.setCandidateCPAScore(Double.toString(responseLite.getCandidateScore()));

			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						0.0, gradeList);
			}

			getQuestionWiseScores(rpsMasterAssociation.getUniqueCandidateId(),
					rpsMasterAssociation.getRpsAssessment().getAssessmentCode(), BehavioralTestType.CPAA,
					behaviouralReportEntity, questionLanguageMap);

			findCandidateAlignment(behaviouralReportEntity);

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.CPAA.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CPAA.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}

		try {
			CPABasicAndAdvancedPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			CPABasicAndAdvancedPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	private void findCandidateAlignment(BehaviouralReportEntity behaviouralReportEntity) {

		Type gradeType = new TypeToken<ArrayList<BehaviouralCandidateAlignmentEntity>>() {
		}.getType();
		String json = messagesReader.getProperty("CPA_CANDIDATE_ALIGNMENT_GRADE");
		List<BehaviouralCandidateAlignmentEntity> gradeList = gson.fromJson(json, gradeType);

		behaviouralReportEntity
				.setCpaCandidateAlignment(generateCandidateAlignment(gradeList, behaviouralReportEntity));
	}

	private String getQuestionWiseScores(Integer uniqueCandidateId, String assessmentCode,
			BehavioralTestType behavioralTestType, BehaviouralReportEntity behaviouralReportEntity,
			Map<String, RpsQuestion> rpsQuestionsMap) throws Exception {
		logger.debug("getQuestionWiseScores() for unique candidate id = {}", uniqueCandidateId);
		String overallFRIN = "";
		RpsCandidateResponses candidateResponses = rpsCandidateResponsesRepository
				.findByUniqueCandidateId(uniqueCandidateId);

		if (candidateResponses != null) {
			String quest1 = candidateResponses.getQNo_1();
			String quest2 = candidateResponses.getQNo_2();
			String quest3 = candidateResponses.getQNo_3();
			String quest4 = candidateResponses.getQNo_4();
			String quest5 = candidateResponses.getQNo_5();
			String quest6 = candidateResponses.getQNo_6();
			String quest7 = candidateResponses.getQNo_7();
			String quest8 = candidateResponses.getQNo_8();
			String quest9 = candidateResponses.getQNo_9();
			String quest10 = candidateResponses.getQNo_10();
			String quest11 = candidateResponses.getQNo_11();
			String quest12 = candidateResponses.getQNo_12();
			String quest13 = candidateResponses.getQNo_13();
			String quest14 = candidateResponses.getQNo_14();
			String quest15 = candidateResponses.getQNo_15();
			String quest16 = candidateResponses.getQNo_16();
			String quest17 = candidateResponses.getQNo_17();
			String quest18 = candidateResponses.getQNo_18();
			String quest19 = candidateResponses.getQNo_19();
			String quest20 = candidateResponses.getQNo_20();
			String quest21 = candidateResponses.getQNo_21();
			String quest22 = candidateResponses.getQNo_22();
			String quest23 = candidateResponses.getQNo_23();
			String quest24 = candidateResponses.getQNo_24();
			String quest25 = candidateResponses.getQNo_25();

			switch (behavioralTestType) {

				case CPAA:
					getScoringForPL(quest1, quest6, quest11, quest16, quest21, rpsQuestionsMap, behaviouralReportEntity,
							"CPAAAP");
					getScoringForPL(quest2, quest7, quest12, quest17, quest22, rpsQuestionsMap, behaviouralReportEntity,
							"CPAAHS");
					getScoringForPL(quest3, quest8, quest13, quest18, quest23, rpsQuestionsMap, behaviouralReportEntity,
							"CPAACC");
					getScoringForPL(quest4, quest9, quest14, quest19, quest24, rpsQuestionsMap, behaviouralReportEntity,
							"CPAACE");
					getScoringForPL(quest5, quest10, quest15, quest20, quest25, rpsQuestionsMap,
							behaviouralReportEntity, "CPAAMI");
					break;

				case CPAB:
					getScoringForPL(quest1, quest6, quest11, quest16, null, rpsQuestionsMap, behaviouralReportEntity,
							"CPABAP");
					getScoringForPL(quest2, quest7, quest12, quest17, null, rpsQuestionsMap, behaviouralReportEntity,
							"CPABHS");
					getScoringForPL(quest3, quest8, quest13, quest18, null, rpsQuestionsMap, behaviouralReportEntity,
							"CPABCC");
					getScoringForPL(quest4, quest9, quest14, quest19, null, rpsQuestionsMap, behaviouralReportEntity,
							"CPABCE");
					getScoringForPL(quest5, quest10, quest15, quest20, null, rpsQuestionsMap, behaviouralReportEntity,
							"CPABMI");
					break;
			}

		}
		return overallFRIN;
	}

	public String findProficiencyLevel(String si, String ci) {
		Type gradeType = new TypeToken<ArrayList<BehaviouralProficiencyLevelEntity>>() {
		}.getType();
		String json = messagesReader.getProperty("CPA_PROFICIENCY_LEVEL_GRADE");
		List<BehaviouralProficiencyLevelEntity> gradeList = gson.fromJson(json, gradeType);
		return generatePLGrade(gradeList, si, ci);
	}

	public void getScoringForPL(String question1, String question2, String question3, String question4,
			String question5, Map<String, RpsQuestion> rpsQuestionsMap, BehaviouralReportEntity behaviouralReportEntity,
			String chrcsticType) throws RpsException {
		if (question1 == null || question2 == null || question3 == null || question4 == null) {
			return;
		}
		// Split responses
		String answers1[] = question1.split("--");
		String answers2[] = question2.split("--");
		String answers3[] = question3.split("--");
		String answers4[] = question4.split("--");
		// Get QuestId from responses
		// Get Question Bean for those quest Ids
		RpsQuestion rpsQuestion1 = rpsQuestionsMap.get(answers1[1]);
		RpsQuestion rpsQuestion2 = rpsQuestionsMap.get(answers2[1]);
		RpsQuestion rpsQuestion3 = rpsQuestionsMap.get(answers3[1]);
		RpsQuestion rpsQuestion4 = rpsQuestionsMap.get(answers4[1]);

		Double score1 = evaluateMCQW(rpsQuestion1, answers1[2]);
		Double score2 = evaluateMCQW(rpsQuestion2, answers2[2]);
		Double score3 = evaluateMCQW(rpsQuestion3, answers3[2]);
		Double score4 = evaluateMCQW(rpsQuestion4, answers4[2]);

		Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
		}.getType();

		Double stdDev = 0.0;
		String json = "[]";

		if (behaviouralReportEntity.isBasic()) {
			double[] data = { score1, score2, score3, score4 };
			stdDev = findStdDevForScores(data);
			json = messagesReader.getProperty("CPA_BASIC_SD");
		} else {
			if (question5 == null) {
				return;
			}
			String answers5[] = question5.split("--");
			RpsQuestion rpsQuestion5 = rpsQuestionsMap.get(answers5[1]);
			Double score5 = evaluateMCQW(rpsQuestion5, answers5[2]);
			double[] data = { score1, score2, score3, score4, score5 };
			stdDev = findStdDevForScores(data);
			json = messagesReader.getProperty("CPA_ADVANCED_SD");
		}
		List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(json, gradeType);

		switch (chrcsticType) {
			case "CPAAAP":
				String cpaAPType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaAPPL(findProficiencyLevel(behaviouralReportEntity.getCpaAPGrade(), cpaAPType));
				break;

			case "CPAACC":
				String cpaCCType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaCCPL(findProficiencyLevel(behaviouralReportEntity.getCpaCCGrade(), cpaCCType));
				break;

			case "CPAACE":
				String cpaCEType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaCEPL(findProficiencyLevel(behaviouralReportEntity.getCpaCEGrade(), cpaCEType));
				break;

			case "CPAAHS":
				String cpaHSType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaHSPL(findProficiencyLevel(behaviouralReportEntity.getCpaHSGrade(), cpaHSType));
				break;

			case "CPAAMI":
				String cpaMIType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaMIPL(findProficiencyLevel(behaviouralReportEntity.getCpaMIGrade(), cpaMIType));
				break;

			case "CPABAP":
				String cpabAPType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaAPPL(findProficiencyLevel(behaviouralReportEntity.getCpaAPGrade(), cpabAPType));
				break;

			case "CPABCC":
				String cpabCCType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaCCPL(findProficiencyLevel(behaviouralReportEntity.getCpaCCGrade(), cpabCCType));
				break;

			case "CPABCE":
				String cpabCEType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaCEPL(findProficiencyLevel(behaviouralReportEntity.getCpaCEGrade(), cpabCEType));
				break;

			case "CPABHS":
				String cpabHSType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaHSPL(findProficiencyLevel(behaviouralReportEntity.getCpaHSGrade(), cpabHSType));
				break;

			case "CPABMI":
				String cpabMIType = this.generateBehaviouralGradeOnCandidateScore(gradeList, stdDev);
				behaviouralReportEntity
						.setCpaMIPL(findProficiencyLevel(behaviouralReportEntity.getCpaMIGrade(), cpabMIType));
				break;

			default:
				break;
		}

		return;
	}

	public double findStdDevForScores(double[] data) {
		StdDevUtil stdDevUtil = new StdDevUtil(data);
		return stdDevUtil.getStdDev();
	}

	public Double evaluateMCQW(RpsQuestion rpsQuestion, String candidateResponseString) {
		Double charsticScore = 0.0;
		// parsing mcqw question qAns
		Type responseMarkType = new TypeToken<ArrayList<ResponseMarkBean>>() {
		}.getType();
		String responseMarkBeansJson = rpsQuestion.getQans();
		List<ResponseMarkBean> responseMarkBeans = new Gson().fromJson(responseMarkBeansJson, responseMarkType);
		if (responseMarkBeans != null && !responseMarkBeans.isEmpty()) {
			for (ResponseMarkBean responseMarkBean : responseMarkBeans) {
				if (responseMarkBean.getResponse().equalsIgnoreCase(candidateResponseString)) {
					charsticScore = responseMarkBean.getResponsePositiveMarks();
					break;
				}
			}
		}
		return charsticScore;
	}

	public com.merittrac.apollo.excel.FileExportExportEntity generateRevenueReportInEventStartAndEndDateExcel(
			String eventStart, String eventEnd) {
		return candidateScheduleReport.generateRevenueReportInEventStartAndEndDateExcel(eventStart, eventEnd);
	}

	/**
	 * @param gradingSchemeDos
	 * @param candidateScore
	 * @return
	 */
	private String generatePLGrade(List<BehaviouralProficiencyLevelEntity> gradingSchemeDos, String si, String ci) {
		logger.info("--IN generatePLGrade -- with param gradingSchemeDos: {}, candidateScore: {}", gradingSchemeDos, si,
				ci);
		String scoreGrade = RpsConstants.NA;
		if (gradingSchemeDos != null && si != null && ci != null) {
			for (BehaviouralProficiencyLevelEntity gradingSchemeDo : gradingSchemeDos) {
				if (si.equalsIgnoreCase(gradingSchemeDo.getScoreInterpretation())
						&& ci.equalsIgnoreCase(gradingSchemeDo.getConsistencyInterpretation())) {
					scoreGrade = gradingSchemeDo.getProficiencyLevel();
				}
			}
		}
		return scoreGrade;
	}

	/**
	 * @param gradingSchemeDos
	 * @return
	 */
	private String generateCandidateAlignment(List<BehaviouralCandidateAlignmentEntity> gradingSchemeDos,
			BehaviouralReportEntity behaviouralReportEntity) {
		logger.info("--IN generateCandidateAlignment -- with param gradingSchemeDos: {}, candidateScore: {}",
				gradingSchemeDos, behaviouralReportEntity);
		String scoreGrade = null;
		if (gradingSchemeDos != null && behaviouralReportEntity.getCpaAPPL() != null
				&& behaviouralReportEntity.getCpaCCPL() != null && behaviouralReportEntity.getCpaCEPL() != null
				&& behaviouralReportEntity.getCpaMIPL() != null && behaviouralReportEntity.getCpaHSPL() != null) {
			for (BehaviouralCandidateAlignmentEntity gradingSchemeDo : gradingSchemeDos) {
				if (behaviouralReportEntity.getCpaAPPL()
						.equalsIgnoreCase(gradingSchemeDo.getAskingPermissionBeforeActions())
						&& behaviouralReportEntity.getCpaCCPL()
								.equalsIgnoreCase(gradingSchemeDo.getClarityInCommunication())
						&& behaviouralReportEntity.getCpaCEPL().equalsIgnoreCase(gradingSchemeDo.getChatEtiquette())
						&& behaviouralReportEntity.getCpaMIPL()
								.equalsIgnoreCase(gradingSchemeDo.getManagingInformation())
						&& behaviouralReportEntity.getCpaHSPL().equalsIgnoreCase(gradingSchemeDo.getHelpfulService())) {
					return scoreGrade = gradingSchemeDo.getCandidateAlignment();
				}
			}
			if (scoreGrade == null)
				return scoreGrade = RpsConstants.CPA_WEAK;
		}
		return scoreGrade;
	}

	@Transactional
	public String generateBehavioralReportForIDSGInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForIDSInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.IDSG.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;

		List<String> failedloginID = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {

			String loginIdForCandidate = " ";

			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.IDSG.toString(), "dd-MMM-yyyy");

			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);

			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");

				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(),
							gradeType);
					getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, 0.0, gradeList);

				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSG.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			}

			catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSG.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);

				continue;
			}
		}
		try {
			IDSPDFGenerator.failedPdfGenerator(failedBehaviouralReportEntitiesList);
			IDSPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}

		} catch (Exception e) {

			logger.error("Error in Generating Report " + e.getMessage());
			return gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF"));

		}

		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
					behavioralDownloadingPath.toString(),
					"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
					failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}
		return gson.toJson(fileExportExportEntity);
	}

	@Transactional
	public String generateBehavioralReportForIDSBInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForIDSInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.IDSB.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {

			String loginIdForCandidate = " ";

			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.IDSB.toString(), "dd-MMM-yyyy");

			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);

			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");

				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(),
							gradeType);
					getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, 0.0, gradeList);
				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSB.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSB.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);

				continue;
			}

		}
		try {

			IDSBOBPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			IDSBOBPDFGenerator.failedPdfGenerator(failedBehaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());

			}

		} catch (Exception e) {
			logger.error("Error in Generating Report " + e.getMessage());

			return gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF"));
		}

		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
					behavioralDownloadingPath.toString(),
					"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
					failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * WTL reports
	 *
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @return
	 */
	@Transactional
	public String generateBehavioralReportForWTLInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForWTLInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.WTL.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.WTL.toString(), "dd MMM yyyy");

			// FRIN
			List<String> imagesForFRIN = getFrinScore(rpsMasterAssociation.getUniqueCandidateId(),
					BehavioralTestType.WTL);

			if (imagesForFRIN.size() == 5) {
				String wtlDType = imagesForFRIN.get(0);
				behaviouralReportEntity.setWtlDType(wtlDType == null ? "" : wtlDType);
				String wtlPType = imagesForFRIN.get(1);
				behaviouralReportEntity.setWtlPType(wtlPType == null ? "" : wtlPType);
				String wtlIPType = imagesForFRIN.get(2);
				behaviouralReportEntity.setWtlIPType(wtlIPType == null ? "" : wtlIPType);
				String wtlIAType = imagesForFRIN.get(3);
				behaviouralReportEntity.setWtlIAType(wtlIAType == null ? "" : wtlIAType);
				String overallFRIN = imagesForFRIN.get(4);
				behaviouralReportEntity.setOverallFRIN(overallFRIN == null ? "" : overallFRIN);
			}

			Type qustSeqList = new TypeToken<HashSet<Integer>>() {
			}.getType();
			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
			for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
				Set<Integer> qustSequences = gson.fromJson(rpCharacteristic.getQuestionNumbers(), qustSeqList);
				List<Double> rQuestionScoreList = rpsQuestionAssociationRepository
						.getLiteQuestionsFromQuestionPaperAndQuestSequence(qpID, qustSequences);
				double totalScore = 0;
				for (Double score : rQuestionScoreList) {
					totalScore += score;
				}
				List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
				getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap, rpCharacteristic,
						totalScore, gradeList);
			}

			Double wtlTotalScore = 0.0;
			for (Double score : behaviouralTestScoreMap.values()) {
				wtlTotalScore += score;
			}
			behaviouralReportEntity.setWtlTotalMarks(wtlTotalScore);

			File path = null;
			path = new File(behavioralFolder);
			if (!path.isDirectory()) {
				path.mkdirs();
			}
			path = new File(path + File.separator + BehavioralTestType.WTL.toString() + "_"
					+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
			behaviouralReportEntity.setPdfFileName(path.toString());
			behaviouralReportEntity.setLogoPath(" ");

			behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.WTL.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}
		try {
			WTLPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			WTLPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	public String generateBehavioralReportForWTSInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForWTSInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.WTS.toString() + System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		List<String> failedloginID = new ArrayList<>();
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();
			SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy");
			String convertedDate = RpsConstants.NA;
			if (rpsMasterAssociation.getTestStartTime() != null)
				convertedDate = dateFormat.format(rpsMasterAssociation.getTestStartTime());
			behaviouralReportEntity.setTestStartDate(convertedDate);
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			// qp
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			// get cand name
			String candidateFullName =
					this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId());

			behaviouralReportEntity.setCandidateName(candidateFullName);

			behaviouralReportEntity.setLoginId(rpsMasterAssociation.getLoginID());
			behaviouralReportEntity.setMerittracId(rpsCandidate.getCandidateId1());
			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			RpsBehaviouralTest rpsBehaviouralTest = rpsBehaviouralTestRepository
					.findByBtId(BehavioralTestType.WTS.name());
			RpsCandidateResponses candidateResponses = rpsCandidateResponsesRepository
					.findByUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);

			Double wtsTotalScore = 0.0;
			Map<String, Double> scoreMap = new HashMap<>();
			for (String scoreKey : behaviouralTestScoreMap.keySet()) {
				scoreMap.put(scoreKey, behaviouralTestScoreMap.get(scoreKey));

			}
			behaviouralReportEntity.setWtsTotalMarks(wtsTotalScore);
			Map<String, Map<String, String>> wtsMap = null;

			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				for (RpsBehaviouralTestCharacteristic rpsCharacteristic : rpsBehaviouralTest
						.getRpsBehaviouralTestCharacteristics()) {
					Map<String, String> levelMap = new HashMap<>();
					levelMap.put("FRIN", wtsService.getFrin(rpsCharacteristic.getcName(), candidateResponses));

					levelMap.put("FIRST_LEVEL", wtsService.getFirstLevelForWTS(scoreMap.get(rpsCharacteristic.getcId()),
							rpsCharacteristic.getcId()));

					Map<String, String> componantMap =
							wtsService.getScoreForWTS(rpsCharacteristic.getcName(), candidateResponses);
					levelMap.put("SECOND_LEVEL", componantMap.get(rpsCharacteristic.getcName()));
					if (rpsCharacteristic.getcName().equals("Fake Detector")) {
						String[] overAll =
								{ "Critical Thinking", "Creative Thinking", "Collaboration", "Customer Centricity" };
						levelMap.put("FRIN_ALL_OVER", wtsService.getOverAllFrin(overAll, candidateResponses));
					}
					Double componantScore = (behaviouralTestScoreMap.get(rpsCharacteristic.getcId()) / 40) * 100;
					levelMap.put("GRAPH_SCORE", String.valueOf(componantScore));
					wtsMap = behaviouralReportEntity.getWtsLevelMap();
					if (wtsMap != null) {
						wtsMap.put(rpsCharacteristic.getcName(), levelMap);
						behaviouralReportEntity.setWtsLevelMap(wtsMap);
					} else {
						wtsMap = new HashMap<>();
						wtsMap.put(rpsCharacteristic.getcName(), levelMap);
						behaviouralReportEntity.setWtsLevelMap(wtsMap);
					}
				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.WTS.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.WTS.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}
		try {
			WTSPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			WTSPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	@Transactional
	public String generateBehavioralReportForIDSAInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForIDSInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.IDSA.toString() + System.currentTimeMillis());

		List<RpsMasterAssociation> rpsMasterAssociations = rpsMasterAssociationService
				.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();

		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {


			String loginIdForCandidate="";
			RpsCandidate rpsCandidate = rpsMasterAssociationService
					.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.IDSA.toString(), "dd-MMM-yyyy");

			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap = gson.fromJson(responseLite.getBehaviouralTestScores(),
					behavTestScoreType);

			boolean isFullResponse = true;
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = responseLite.getQuestionsAttempted();

			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList =
							gson.fromJson(rpCharacteristic.getGrade(), gradeType);
					getGradeListForBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, 0.0, gradeList);
				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSA.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			}

			catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.IDSA.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);

				failedloginID.add(loginIdForCandidate);

				continue;
			}

		}
		try {
			IDSAPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			IDSAPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {

			logger.error("Error in Generating Report " + e.getMessage());
			return gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF"));
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(), "Incomplete report for loginId :" +failedloginID +" due to in-complete assessments",failedloginID);
		}
		else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true") ){
				if(behaviouralReportEntitiesList!=null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			}
			else{
				if(failedBehaviouralReportEntitiesList!=null){
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(), "In-complete report for loginId : " +failedloginID +" due to in-complete assessments",failedloginID);
				}

			}
		}
		return gson.toJson(fileExportExportEntity);
	}

	// void addSectionLevelCutOffForTesting(RpsQuestionPaper rpPaper, String assessmentCode) {
	// boolean isDefaultGroup = false;
	// // new implementation
	// Map<String, List<String>> qtypeSectionMap =
	// rpsQuestionService.findRpsQuestionTypeAndSecIdentifierByQpCode(rpPaper.getQpCode());
	//
	// resultProcessingRuleDo =
	// scoreReportUtility.getGradingSchemeDosAndSectionWeightageCutOff(rpPaper, assessmentCode);
	//
	// gradingEnabled = false;
	// sectionGroupEnabled = false;
	// gradingSchemeDos = null;
	// sectionWeightageCutOffEntityMap = null;
	// if (resultProcessingRuleDo != null) {
	// gradingSchemeDos = resultProcessingRuleDo.getGradingSchemeDos();
	// sectionWeightageCutOffEntityMap = resultProcessingRuleDo.getGroupSectionMap();
	// }
	//
	// if (gradingSchemeDos != null && !gradingSchemeDos.isEmpty())
	// gradingEnabled = true;
	// if (sectionWeightageCutOffEntityMap != null && !sectionWeightageCutOffEntityMap.isEmpty())
	// sectionGroupEnabled = true;
	//
	// if (sectionWeightageCutOffEntityMap != null && sectionWeightageCutOffEntityMap.size() == 1) {
	// if (sectionWeightageCutOffEntityMap.containsKey("DEFAULTGROUP"))
	// isDefaultGroup = true;
	// }
	// }

	public RpsWetScoreEvaluation getQuesIdFromCandResponseByQtype(Integer uniqueCandId, String response) {
		Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
		}.getType();
		List<CandidateResponseEntity> candidateResponseEntityList = new Gson().fromJson(response, type);
		for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntityList) {
			if (candidateResponseEntity.getQuestionType()
					.equalsIgnoreCase(QuestionType.WRITTEN_ENGLISH_TEST.toString())) {
				RpsWetScoreEvaluation rpScoreEvaluation = rpsWetDataEvaluationService
						.getByCandidateUniqueIdAndQId(uniqueCandId, candidateResponseEntity.getQuestionID());
				if (rpScoreEvaluation != null)
					return rpScoreEvaluation;
				else
					return null;
			}

		}
		return null;
	}


	public String getCandidateTopicLevelScoreDetailsForAssessmentLevelMarksReport(Set<Integer> candidateId) {
		try {
			return new Gson().toJson(
					topicWiseExcelReport.getCandidateTopicLevelScoreDetailsForAssessmentLevelMarksReport(candidateId));
		} catch (ParseException | RpsException ex) {
			String msg = "Exception while executing getCandidateTopicLevelScoreDetailsForAssessmentLevelMarksReport...";
			logger.error(msg, ex);
			return null;
		}
	}

	/**
	 * offline API for reading uniquecandidate ids to audit logs convertion
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<Integer> readCandIdExcelFile(String filePath) throws IOException {
		System.out.println("start");
		List<Integer> candidateMTids = new ArrayList<>();
		FileInputStream file = new FileInputStream(new File(filePath));
		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		// Get first/desired sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);
		// Iterate through each rows one by one
		Iterator<Row> rowIterator = sheet.iterator();
		rowIterator.next();

		while (rowIterator.hasNext()) {
			Row row = rowIterator.next();
			Cell currentCell = row.getCell(0);
			String mtId = currentCell.toString();
			candidateMTids.add(Integer.parseInt(mtId));
		}
		file.close();
		System.out.println("read candidate ids from excel");
		return candidateMTids;
	}

	/**
	 * offline API for reading uniquecandidate ids to audit logs convertion
	 * 
	 * @param filePath
	 * @param candidateDetails
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void writeCandDetailsToExcelFile(String filePath, List<List<String>> candidateDetails)
			throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet();
		int rowNum = 0;
		System.out.println("writing to excel");
		int i = 0;
		for (List<String> candidates : candidateDetails) {
			Row row = sheet.createRow(rowNum++);
			int colNum = 0;
			for (String detail : candidates) {
				Cell cell = row.createCell(colNum++);
				if (detail instanceof String) {
					cell.setCellValue((String) detail);
				}
			}
			System.out.println("candidate info write " + i++);
		}

		try {
			FileOutputStream outputStream = new FileOutputStream(filePath);
			workbook.write(outputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}


	public List<List<String>> readCandDetailsOnBatchCodeFormat1() {
		List<CandidateDetailsForResponseReport> candidateDetailsList = null;
		// rpsMasterAssociationService.findAssociationsByBatchAcsId();
		List<List<String>> candidates = new ArrayList<>();

		for (CandidateDetailsForResponseReport candidateDetailsForResponse : candidateDetailsList) {
			List<String> data = new ArrayList<>();
			data.add("" + candidateDetailsForResponse.getUniqueCandidateId());
			data.add(candidateDetailsForResponse.getLoginID());
			data.add(candidateDetailsForResponse.getAssessmentCode());

			String candidateJsonResponse = candidateDetailsForResponse.getResponses();
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList =
					new Gson().fromJson(candidateJsonResponse, type);

			for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntityList) {
				data.add(candidateResponseEntity.getSectionID());
				data.add(candidateResponseEntity.getQuestionID());

				Map<String, String> choicesMap = candidateResponseEntity.getResponse();
				if (choicesMap != null && !choicesMap.isEmpty()) {
					for (Map.Entry<String, String> choices : choicesMap.entrySet()) {
						data.add(choices.getValue());
					}
				}
			}
			candidates.add(data);
		}
		return candidates;
	}

	/**
	 * offline API for reading uniquecandidate ids to audit logs convertion
	 * 
	 * @param filePath
	 * @return
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<Integer> readBatchAcsIdExcelFile(String filePath) throws IOException {
		System.out.println("start");
		List<Integer> batchAcsIds = new ArrayList<>();
		FileInputStream file = new FileInputStream(new File(filePath));
		// Create Workbook instance holding reference to .xlsx file
		XSSFWorkbook workbook = new XSSFWorkbook(file);
		// Get first/desired sheet from the workbook
		XSSFSheet sheet = workbook.getSheetAt(0);
		// Iterate through each rows one by one
		// Row getSchool = firstSheet.getRow(10);

		Iterator<Row> rowIterator = sheet.iterator();
		rowIterator.next();
		// int i = 1;
		while (rowIterator.hasNext()) {
			// Row row = firstSheet.getRow(10);
			Row row = rowIterator.next();
			Cell currentCell = row.getCell(0);
			String mtId = currentCell.toString();
			System.out.println(mtId);
			batchAcsIds.add(Integer.parseInt(mtId));
			// if (i == 50)
			// break;
			// i++;
		}
		file.close();
		System.out.println("done with reading candidate ids from excel");
		return batchAcsIds;
	}

	public Map<List<String>, List<List<String>>> readCandDetailsOnBatchCodeFormat2(List<Integer> batchacsid) {

		List<CandidateDetailsForResponseReport> candidateDetailsList =
				rpsMasterAssociationService.findAssociationsByBatchAcsId(batchacsid);
		Map<List<String>, List<List<String>>> candidates = new HashMap<>();
		// unique cand id, login id, assessment name, set code, batch name, assessment code, section name, batch
		// date, question id, reponses
		for (CandidateDetailsForResponseReport candidateDetailsForResponse : candidateDetailsList) {
			List<String> data = new ArrayList<>();
			data.add("" + candidateDetailsForResponse.getUniqueCandidateId());
			data.add(candidateDetailsForResponse.getLoginID());
			data.add(candidateDetailsForResponse.getAssessmentName());
			String qpcode = candidateDetailsForResponse.getQpCode();
			String setcode[] = qpcode.split("_");
			data.add("set_" + setcode[8]);
			data.add(candidateDetailsForResponse.getBatchName());
			data.add(candidateDetailsForResponse.getAssessmentCode());
			data.add(candidateDetailsForResponse.getBatchDate());
			//
			String candidateJsonResponse = candidateDetailsForResponse.getResponses();
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList =
					new Gson().fromJson(candidateJsonResponse, type);
			// Map<String,List<String>> responseDataWrap = new HashMap<>();
			List<List<String>> responses = new ArrayList<>();
			for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntityList) {
				List<String> r = new ArrayList<>();
				r.add(candidateResponseEntity.getSectionID());
				r.add(candidateResponseEntity.getQuestionID());

				Map<String, String> choicesMap = candidateResponseEntity.getResponse();
				if (choicesMap != null && !choicesMap.isEmpty()) {
					boolean b = false;
					for (Map.Entry<String, String> choices : choicesMap.entrySet()) {
						// if (b)
						// r.add("," + choices.getValue());
						// else
						r.add(choices.getValue());
						// b = true;
					}
				}
				responses.add(r);
			}
			candidates.put(data, responses);
		}
		return candidates;

	}

	/**
	 * offline API for reading uniquecandidate ids to audit logs convertion
	 * 
	 * @param filePath
	 * @param candidateDetails
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void writeCandDetailsToExcelFileFormat2(String filePath,
			Map<List<String>, List<List<String>>> candidateDetails)
			throws IOException {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet();
		int rowNum = 0;
		System.out.println("writing to excel");
		int i = 0;
		// headers
		// unique cand id, login id, assessment name, set code, batch name, assessment code, section name, batch
		// date, question id, reponses
		Row row = sheet.createRow(rowNum++);
		int colNum = 0;
		Cell cell = row.createCell(colNum++);
		cell.setCellValue("Unique Candidate Id");
		cell = row.createCell(colNum++);
		cell.setCellValue("Login Id");
		cell = row.createCell(colNum++);
		cell.setCellValue("Assessment Name");
		cell = row.createCell(colNum++);
		cell.setCellValue("Set Code");
		cell = row.createCell(colNum++);
		cell.setCellValue("Batch Name");
		cell = row.createCell(colNum++);
		cell.setCellValue("Assessment Code");
		cell = row.createCell(colNum++);
		cell.setCellValue("Batch Date");
		cell = row.createCell(colNum++);
		cell.setCellValue("Section Id");
		cell = row.createCell(colNum++);
		cell.setCellValue("Question Id");
		cell = row.createCell(colNum++);
		cell.setCellValue("Responses");

		for (Map.Entry<List<String>, List<List<String>>> candidates : candidateDetails.entrySet()) {
			List<String> details = candidates.getKey();
			List<List<String>> response = candidates.getValue();
			for (List<String> resp : response) {
				row = sheet.createRow(rowNum++);
				colNum = 0;
				cell = row.createCell(colNum++);
				if (details.get(0) instanceof String) {
					cell.setCellValue((String) details.get(0));
				}
				cell = row.createCell(colNum++);
				if (details.get(1) instanceof String) {
					cell.setCellValue((String) details.get(1));
				}
				cell = row.createCell(colNum++);
				if (details.get(2) instanceof String) {
					cell.setCellValue((String) details.get(2));
				}
				//
				cell = row.createCell(colNum++);
				if (details.get(2) instanceof String) {
					cell.setCellValue((String) details.get(3));
				}
				cell = row.createCell(colNum++);
				if (details.get(2) instanceof String) {
					cell.setCellValue((String) details.get(4));
				}
				cell = row.createCell(colNum++);
				if (details.get(2) instanceof String) {
					cell.setCellValue((String) details.get(5));
				}
				cell = row.createCell(colNum++);
				if (details.get(2) instanceof String) {
					cell.setCellValue((String) details.get(6));
				}
				//
				//
				for (String string : resp) {
					cell = row.createCell(colNum++);
					if (string instanceof String) {
						cell.setCellValue((String) string);
					}
				}

			}
			System.out.println("candidate info write " + i++);
		}

		try {
			FileOutputStream outputStream = new FileOutputStream(filePath);
			workbook.write(outputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public String generateBehavioralReportForCGTInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForCGTInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.CGT.toString() + System.currentTimeMillis());
		List<CandidateDetails> candidateDetailsList = rpsMasterAssociationService
				.findRpsCandidateDetailsAdditionalInfoByUniqueCandidateId(uniqueCandidateIds);

		List<BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		List<BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();

		for (CandidateDetails candidateDetails : candidateDetailsList) {

			String loginIdForCandidate = "";
			BehaviouralReportEntity behaviouralReportEntity = new BehaviouralReportEntity();
			rpCharacteristicSet = getCandidateDetailsForBehaviouralReportsForCGT(candidateDetails,
					behaviouralReportEntity, BehavioralTestType.CGT.toString(), "dd-MMM-yyyy");
			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(candidateDetails.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity = new FileExportExportEntity(RpsConstants.FAILED_STATUS, null,
						"Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			BehaviouralTestScoresEntity behaviouralTestScoresEntity = gson
					.fromJson(responseLite.getBehaviouralTestScores(), BehaviouralTestScoresEntity.class);
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (candidateDetails.getQpId() != null) {
					qpID = candidateDetails.getQpId();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);
			try {
				Type sdType = new TypeToken<HashMap<String, Double>>() {
				}.getType();
				Type sumType = new TypeToken<HashMap<String, Integer>>() {
				}.getType();
				Type paramType = new TypeToken<HashMap<String, String>>() {
				}.getType();
				String cgtUniqCodesJson = messagesReader.getProperty("CGT_PARAMNAME_TO_PARAMCODE");
				Map<String, String> mapForUniqueCodes = gson.fromJson(cgtUniqCodesJson, paramType);

				String stdValues = behaviouralTestScoresEntity.getMapStdDevValuesToParam();
				String sumValues = behaviouralTestScoresEntity.getMapSummationValuesToParam();
				String sumOnUniqCodes = behaviouralTestScoresEntity.getMapSummationValuesToUniqCodes();
				Map<String, Double> mapStdDevValuesToParam = gson.fromJson(stdValues, sdType);
				Map<String, Integer> mapSummationValuesToParam = gson.fromJson(sumValues, sumType);
				Map<String, Integer> mapSummationValuesToUniqCodes = gson.fromJson(sumOnUniqCodes, sumType);

				loginIdForCandidate = candidateDetails.getLoginId();
				//
				// put here
				boolean isFullResponse = true;
				Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
				}.getType();
				List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
				String candidateJsonResponse = responseLite.getResponse();
				if (candidateJsonResponse != null)
					candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
				int questionsCount = rpsQuestionPaper.getTotalQuestions();
				int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
				if (questionsCount != responseCount)
					isFullResponse = false;


				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				//

				List<BehaviouralParamtersEntity> behaviouralParamtersEntities = arrangeParamsInDecendingOrder(
						rpCharacteristicSet, mapSummationValuesToParam, mapStdDevValuesToParam, mapForUniqueCodes,
						mapSummationValuesToUniqCodes);
				behaviouralReportEntity.setBehaviouralParamtersEntities(behaviouralParamtersEntities);

				Map<String, Double> scores = getVerbAnalScores(candidateDetails.getLoginId());
				behaviouralReportEntity.setAnalyticalScore(scores.get(RpsConstants.ANALITICAL_SCORE));
				behaviouralReportEntity.setVerbalScore(scores.get(RpsConstants.VERBAL_SCORE));

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CGT.toString() + "_"
						+ candidateDetails.getLoginId() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CGT.toString() + "_"
						+ candidateDetails.getLoginId() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");
				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);
				failedloginID.add(loginIdForCandidate);
				continue;
			}
		}

		try {
			CGTpdfgenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			CGTpdfgenerator.pdfGenerator(behaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {

			logger.error("Error in Generating Report " + e.getMessage());
			return gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF"));
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
					behavioralDownloadingPath.toString(),
					"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
					failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * ExcelReportForCGT
	 * 
	 * @param uniqueCandidateIds
	 * @return
	 */
	public String generateBehavioralReportForCGTInExcel(Set<Integer> uniqueCandidateIds) {
		logger.debug("---IN--generateBehavioralReportForCGTInExcel()---with uniqueCandidateIds: {}",
				uniqueCandidateIds);

		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;

		List<CandidateDetails> candidateDetailsList =
				rpsMasterAssociationService
				.findRpsCandidateDetailsAdditionalInfoByUniqueCandidateIdforCGTExcel(uniqueCandidateIds);

		List<BehaviouralExcelReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;

		File path = null;
		path = new File(behavioralFolder);
		if (!path.isDirectory()) {
			path.mkdirs();
		}
		String file=path + File.separator + BehavioralTestType.CGT.toString() + System.currentTimeMillis()+".xlsx";
		RpsQuestionPaper rpsQuestionPaper = null;
		for (CandidateDetails candidateDetails : candidateDetailsList) 
		{
			int qpID = candidateDetails.getQpId();

			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();

			try {
				BehaviouralExcelReportEntity behaviouralReportEntity = new BehaviouralExcelReportEntity();
				rpCharacteristicSet = getCandidateDetailsForBehaviouralReportsForCGTExcel(candidateDetails,
						behaviouralReportEntity, BehavioralTestType.CGT.toString(), "dd-MMM-yyyy");
				RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
						.findByMasterAssociationId(candidateDetails.getUniqueCandidateId());
				if (responseLite == null) {
					logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
					fileExportExportEntity =
							new FileExportExportEntity(RpsConstants.FAILED_STATUS, file, "Fail to generate excel");
					return gson.toJson(fileExportExportEntity);

				}

				int responseCount = candidateDetails.getQuestionsAttempted();
				if (questionsCount == responseCount) {
					behaviouralReportEntity.setFullResponse(true);
					Map<String, Double> scores = getVerbAnalScores(candidateDetails.getLoginId());
					behaviouralReportEntity.setAnalyticalScore(scores.get(RpsConstants.ANALITICAL_SCORE));
					behaviouralReportEntity.setVerbalScore(scores.get(RpsConstants.VERBAL_SCORE));

					BehaviouralTestScoresEntity behaviouralTestScoresEntity =
							gson.fromJson(responseLite.getBehaviouralTestScores(), BehaviouralTestScoresEntity.class);

					logger.debug("behaviouralTestScores data for uniqueCandidateIds: {}", behaviouralTestScoresEntity);

					Type sdType = new TypeToken<HashMap<String, Double>>() {
					}.getType();
					Type sumType = new TypeToken<HashMap<String, Integer>>() {
					}.getType();
					Type paramType = new TypeToken<HashMap<String, String>>() {
					}.getType();

					String listOfCandidateResponses = behaviouralTestScoresEntity.getListOfCandidateResponses();
					behaviouralReportEntity.setListOfCandidateResponses(listOfCandidateResponses);
					logger.debug("----list of candidate response----", listOfCandidateResponses);
					String cgtUniqCodesJson = behaviouralTestScoresEntity.getMapSummationValuesToUniqCodes();
					behaviouralReportEntity.setMapSummationValuesToUniqCodes(cgtUniqCodesJson);

					String cgtUniqCodesParamJson = messagesReader.getProperty("CGT_PARAMNAME_TO_PARAMCODE");
					Map<String, String> mapForUniqueCodes = gson.fromJson(cgtUniqCodesParamJson, paramType);


					String stdValues = behaviouralTestScoresEntity.getMapStdDevValuesToParam();
					String sumValues = behaviouralTestScoresEntity.getMapSummationValuesToParam();
					Map<String, Double> mapStdDevValuesToParam = gson.fromJson(stdValues, sdType);
					behaviouralReportEntity.setMapStdDevValuesToParam(stdValues);
					Map<String, Integer> mapSummationValuesToParam = gson.fromJson(sumValues, sumType);
					behaviouralReportEntity.setMapSummationValuesToParam(sumValues);

					Map<String, Integer> mapSummationValuesToUniqCodes = gson.fromJson(cgtUniqCodesJson, sumType);
					List<BehaviouralParamtersEntity> behaviouralParamtersEntities =
							arrangeParamsInDecendingOrder(rpCharacteristicSet, mapSummationValuesToParam,
									mapStdDevValuesToParam, mapForUniqueCodes, mapSummationValuesToUniqCodes);
					logger.debug(
							"---IN--arrangeParamsInDecendingOrder()---with params rpCharacteristicSet: {}, mapSummationValuesToParam: {},mapStdDevValuesToParam: {} ,mapForUniqueCodes: {}",
							rpCharacteristicSet, mapSummationValuesToParam, mapStdDevValuesToParam, mapForUniqueCodes);
					behaviouralReportEntity.setBehaviouralParamtersEntities(behaviouralParamtersEntities);
				} else
					behaviouralReportEntity.setFullResponse(false);
				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				continue;
			}
		}
		try {
			excelReport.excelGenerator(behaviouralReportEntitiesList,file);
		} catch (DocumentException | IOException e) {
			e.printStackTrace();
		}


		fileExportExportEntity =
				new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, file, "excel generation has success");


		return gson.toJson(fileExportExportEntity);

	}

	private Map<String, Double> getVerbAnalScores(String loginId) {
		Map<String, Double> scores = new HashMap<String, Double>();
		Integer uniqueCandidateId = null;
		List<RpsQpSection> qpSections = null;
		List<RpsMasterAssociationLite> ma = rpsMasterAssociationService.findAssessmentsByLoginId(loginId);
		if (ma != null && !ma.isEmpty()) {
			assessmentFor: for (RpsMasterAssociationLite mastAssolite : ma) {
				List<RpsQpSection> sectionsList =
						rpsQpSectionService.findAllQpSectionsByAssmentCode(mastAssolite.getAssessmentCode());
				for (RpsQpSection rpsQpSection : sectionsList) {
					if (rpsQpSection.getTitle().toLowerCase().contains(RpsConstants.VERBAL_SCORE)) {
						uniqueCandidateId = mastAssolite.getUniqueCandidateId();
						qpSections = sectionsList;
						break assessmentFor;
					}

				}
			}
		}

		if (uniqueCandidateId != null && qpSections != null) {
			Map<String, String> secIdToSecTitle = new HashMap<>();
			for (RpsQpSection qpSection : qpSections) {
				secIdToSecTitle.put(qpSection.getSecIdentifier(), qpSection.getTitle());
			}
			List<RpsSectionCandidateResponse> sectionScores = rpsCandidateResponseService
					.findCandSectionScoresByLoginId(uniqueCandidateId);
			if (sectionScores != null && !sectionScores.isEmpty()) {
				for (RpsSectionCandidateResponse rpsSectionCandidateResponse : sectionScores) {
					// assuming verbal , anal are the section names
					String title = secIdToSecTitle.get(rpsSectionCandidateResponse.getSecIdentifier());
					if (title.toLowerCase().contains(RpsConstants.VERBAL_SCORE)) {
						scores.put(RpsConstants.VERBAL_SCORE, rpsSectionCandidateResponse.getScore());
					} else if (title.toLowerCase().contains(RpsConstants.ANALITICAL_SCORE)) {
						scores.put(RpsConstants.ANALITICAL_SCORE, rpsSectionCandidateResponse.getScore());
					}
				}
			}
		}

		return scores;
	}

	private List<BehaviouralParamtersEntity> arrangeParamsInDecendingOrder(
			Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet, Map<String, Integer> mapSummationValuesToParam,
			Map<String, Double> mapStdDevValuesToParam, Map<String, String> mapForUniqueCodes,
			Map<String, Integer> mapSummationValuesToUniqCodes) {
		List<BehaviouralParamtersEntity> behaviouralParamtersEntities = new ArrayList<>();

		for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {
			String parameter = mapForUniqueCodes.get(rpCharacteristic.getcName());
			int count = mapSummationValuesToUniqCodes.get(RpsConstants.THIRD_SECTION_CODE + parameter);
			BehaviouralParamtersEntity behaviouralParamtersEntity = new BehaviouralParamtersEntity(
					mapSummationValuesToParam.get(parameter), parameter, mapStdDevValuesToParam.get(parameter),
					rpCharacteristic.getcName(), count);
			behaviouralParamtersEntities.add(behaviouralParamtersEntity);
		}
		// System.out.println("b4 " + behaviouralParamtersEntities);
		Collections.sort(behaviouralParamtersEntities);
		// System.out.println("after " + behaviouralParamtersEntities);
		Integer rank = 1;
		Double prevCountOnStdDev = null;
		Integer prevCountOnParam = null;
		Integer prevThirdSecCountOnParam = null;
		for (BehaviouralParamtersEntity behaviouralParamtersEntity : behaviouralParamtersEntities) {
			if (prevCountOnStdDev == null && prevCountOnParam == null && prevThirdSecCountOnParam == null) {
				prevCountOnStdDev = behaviouralParamtersEntity.getStdDevOfParam();
				prevCountOnParam = behaviouralParamtersEntity.getCountOnParam();
				prevThirdSecCountOnParam = behaviouralParamtersEntity.getThirdSectionCountOnParam();
				behaviouralParamtersEntity.setRankingOnParam(rank++);
			} else if ((Double.doubleToLongBits(prevCountOnParam) == Double
					.doubleToLongBits(behaviouralParamtersEntity.getCountOnParam()))
					&& (Double.doubleToLongBits(prevCountOnStdDev) == Double
							.doubleToLongBits(behaviouralParamtersEntity.getStdDevOfParam()))
					&& (Double.doubleToLongBits(prevThirdSecCountOnParam) == Double
							.doubleToLongBits(behaviouralParamtersEntity.getThirdSectionCountOnParam()))) {
				behaviouralParamtersEntity.setRankingOnParam(rank - 1);
			} else {
				behaviouralParamtersEntity.setRankingOnParam(rank++);
			}
			prevCountOnStdDev = behaviouralParamtersEntity.getStdDevOfParam();
			prevCountOnParam = behaviouralParamtersEntity.getCountOnParam();
			prevThirdSecCountOnParam = behaviouralParamtersEntity.getThirdSectionCountOnParam();
		}
		return behaviouralParamtersEntities;
	}

	private Set<RpsBehaviouralTestCharacteristic> getCandidateDetailsForBehaviouralReportsForCGT(
			CandidateDetails candidateDetails, BehaviouralReportEntity behaviouralReportEntity,
			String behavioralTestType, String dateInFormat) {
		// get updated data from MIF
		candidateDetails = this.getFillCandidateDetailsFromMIF(candidateDetails);

		StringBuffer candidateName = new StringBuffer("");
		if (candidateDetails.getFirstName() != null)
			candidateName.append(candidateDetails.getFirstName());
		else
			candidateName.append("");

		if (candidateDetails.getMiddleName() != null)
			candidateName.append(" " + candidateDetails.getMiddleName());
		else
			candidateName.append("");

		if (candidateDetails.getLastName() != null)
			candidateName.append(" " + candidateDetails.getLastName());
		else
			candidateName.append("");
		behaviouralReportEntity.setLoginId(candidateDetails.getLoginId());
		behaviouralReportEntity.setBehaviouralType(behavioralTestType);
		behaviouralReportEntity.setCandidateName(candidateName.toString());
		behaviouralReportEntity.setMerittracId(
				candidateDetails.getLoginId() == null ? RpsConstants.NA : candidateDetails.getLoginId() );

		String testName = RpsConstants.NA;

		behaviouralReportEntity.setAssessmentName(testName);
		behaviouralReportEntity.setTestCenter(
				candidateDetails.getTestCenterCity() == null ? RpsConstants.NA : candidateDetails.getTestCenterCity());

		SimpleDateFormat dateFormat = new SimpleDateFormat(dateInFormat);
		String convertedDate = RpsConstants.NA;
		if (candidateDetails.getTestStartTime() != null) {
			Calendar date = TimeUtil.convertStringToCalender(candidateDetails.getTestStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			if (date != null)
				convertedDate = dateFormat.format(date.getTime());
		}
		behaviouralReportEntity.setTestStartDate(convertedDate);
		RpsBehaviouralTest rpsBehaviouralTest = rpsBehaviouralTestRepository.findByBtId(behavioralTestType);
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = rpsBehaviouralTest
				.getRpsBehaviouralTestCharacteristics();
		String rpsCustomerName = rpsEventService.findCustomersByEventCode(candidateDetails.getEventCode());
		if (rpsCustomerName != null)
			behaviouralReportEntity.setOrganization(rpsCustomerName);
		else
			behaviouralReportEntity.setOrganization(RpsConstants.NA);
		return rpCharacteristicSet;
	}

	private Set<RpsBehaviouralTestCharacteristic> getCandidateDetailsForBehaviouralReportsForCGTExcel(
			CandidateDetails candidateDetails, BehaviouralExcelReportEntity behaviouralReportEntity,
			String behavioralTestType, String dateInFormat) {
		// get updated data from MIF
		candidateDetails = this.getFillCandidateDetailsFromMIF(candidateDetails);

		StringBuffer candidateName = new StringBuffer("");
		if (candidateDetails.getFirstName() != null)
			candidateName.append(candidateDetails.getFirstName());
		else
			candidateName.append("");

		if (candidateDetails.getMiddleName() != null)
			candidateName.append(" " + candidateDetails.getMiddleName());
		else
			candidateName.append("");

		if (candidateDetails.getLastName() != null)
			candidateName.append(" " + candidateDetails.getLastName());
		else
			candidateName.append("");

		behaviouralReportEntity.setCandidateName(candidateName.toString());
		behaviouralReportEntity.setMerittracId(
				candidateDetails.getLoginId() == null ? RpsConstants.NA : candidateDetails.getLoginId());

		// testName =
		// rpsMasterAssociation.getAssessmentName() + "_" +
		// rpsQuestionPaper.getSetCode();

		behaviouralReportEntity.setTestCenter(
				candidateDetails.getTestCenterCity() == null ? RpsConstants.NA : candidateDetails.getTestCenterCity());

		behaviouralReportEntity.setDob(candidateDetails.getDob() == null ? RpsConstants.NA : candidateDetails.getDob());
		SimpleDateFormat dateFormat = new SimpleDateFormat(dateInFormat);
		String convertedDate = RpsConstants.NA;
		if (candidateDetails.getTestStartTime() != null) {
			Calendar date = TimeUtil.convertStringToCalender(candidateDetails.getTestStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			if (date != null)
				convertedDate = dateFormat.format(date.getTime());
		}
		behaviouralReportEntity.setTestStartDate(convertedDate);
		behaviouralReportEntity
				.setQpId(candidateDetails.getQpId() == null ? RpsConstants.NA : candidateDetails.getQpId().toString());
		RpsBehaviouralTest rpsBehaviouralTest = rpsBehaviouralTestRepository.findByBtId(behavioralTestType);
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = rpsBehaviouralTest
				.getRpsBehaviouralTestCharacteristics();

		return rpCharacteristicSet;
	}

	@Transactional
	public String generateBehavioralReportForD8InPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForD8InPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip =
				new File(pathForZip + File.separator + BehavioralTestType.Dimension8.toString()
						+ System.currentTimeMillis());

		List<RpsMasterAssociation> rpsMasterAssociations =
				rpsMasterAssociationService.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<Dimension8BehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		List<Dimension8BehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();

		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {

			String loginIdForCandidate = "";
			RpsCandidate rpsCandidate =
					rpsMasterAssociationService.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			Dimension8BehaviouralReportEntity behaviouralReportEntity = new Dimension8BehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.Dimension8.toString(),
					"dd MMMM, yyyy");

			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity =
						new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap =
					gson.fromJson(responseLite.getBehaviouralTestScores(), behavTestScoreType);

			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList =
							gson.fromJson(rpCharacteristic.getGrade(), gradeType);
					getGradeListForDimension8BehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, 0.0, gradeList);
				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.Dimension8.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			}

			catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.Dimension8.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);

				failedloginID.add(loginIdForCandidate);

				continue;
			}

		}
		try {
			Dimension8PDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			Dimension8PDFGenerator.pdfGenerator(behaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {

			logger.error("Error in Generating Report " + e.getMessage());
			return gson.toJson(new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF"));
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}
		return gson.toJson(fileExportExportEntity);
	}

	/**
	 * SSPQ GENERIC
	 * 
	 * @param qpID
	 * @param uniqueCandidateIds
	 * @param isZipFileRequired
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	@Transactional
	public String generateBehavioralReportForSSPQGenericInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug(
				"---IN--generateBehavioralReportForSSPQGenericInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID, uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;

		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + // TODO:
						BehavioralTestType.SSPQG.toString() +
						System.currentTimeMillis());
		List<RpsMasterAssociation> rpsMasterAssociations =
				rpsMasterAssociationService.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<SSPQGenericBehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();
		List<SSPQGenericBehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
			String loginIdForCandidate = rpsMasterAssociation.getLoginID();
			RpsCandidate rpsCandidate =
					rpsMasterAssociationService.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			SSPQGenericBehaviouralReportEntity behaviouralReportEntity = new SSPQGenericBehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity,

					// TODO:
					BehavioralTestType.SSPQG.toString(),
					"dd/MM/yyyy");


			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity =
						new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap =
					gson.fromJson(responseLite.getBehaviouralTestScores(), behavTestScoreType);
			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList = gson.fromJson(rpCharacteristic.getGrade(), gradeType);
					getGradeListForSSPQGenericBehaviouralReport(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, gradeList);
				}
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + // TODO:
						BehavioralTestType.SSPQG.toString() +
						"_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			} catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.SSPQG.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);

				failedloginID.add(loginIdForCandidate);

				continue;
			}
		}
		try {
			SSPQGenericPDFGenerator.reportForFailedCandidats(failedBehaviouralReportEntitiesList);
			SSPQGenericPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);
			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	private void getGradeListForSSPQGenericBehaviouralReport(SSPQGenericBehaviouralReportEntity behaviouralReportEntity,
			Map<String, Double> behaviouralTestScoreMap, RpsBehaviouralTestCharacteristic rpCharacteristic,
			List<BehaviouralGragingSchemaEntity> gradeList) throws RpsException {
		switch (rpCharacteristic.getcId()) {

			// SSPQG
			case "SSPQGF":
				Double score = behaviouralTestScoreMap.get("SSPQGF");
				behaviouralReportEntity.setFlexibilityScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setFlexibilityGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "SSPQGI":
				score = behaviouralTestScoreMap.get("SSPQGI");
				behaviouralReportEntity.setIntegrityScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setIntegrityGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "SSPQGM":
				score = behaviouralTestScoreMap.get("SSPQGM");
				behaviouralReportEntity.setMotivationScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setMotivationGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "SSPQGO":
				score = behaviouralTestScoreMap.get("SSPQGO");
				behaviouralReportEntity.setOrganizedScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setOrganizedGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "SSPQGS":
				score = behaviouralTestScoreMap.get("SSPQGS");
				behaviouralReportEntity.setSociabilityScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setSociabilitygrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "SSPQGT":
				score = behaviouralTestScoreMap.get("SSPQGT");
				behaviouralReportEntity.setToleranceScore(String.valueOf(score.intValue()));
				behaviouralReportEntity
						.setToleranceGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			default:
				break;
		}
	}

	@Transactional
	public String generateBehavioralReportForCSPInPDF(Integer qpID, Set<Integer> uniqueCandidateIds,
			Boolean isZipFileRequired) {
		logger.debug("---IN--generateBehavioralReportForCSPInPDF()---with params qpID: {}, uniqueCandidateIds: {}",
				qpID,
				uniqueCandidateIds);
		FileExportExportEntity fileExportExportEntity = null;
		String behavioralFolder = userHome + File.separator + RpsConstants.BEHAVIORAL_REPORT_FOLDER;
		File pathForZip = new File(behavioralFolder);
		if (!pathForZip.isDirectory()) {
			pathForZip.mkdirs();
		}
		pathForZip = new File(
				pathForZip + File.separator + BehavioralTestType.CSP.toString() + System.currentTimeMillis());

		List<RpsMasterAssociation> rpsMasterAssociations =
				rpsMasterAssociationService.findAllMasterAssociationsByUniqueIds(uniqueCandidateIds);
		List<CSPBehaviouralReportEntity> failedBehaviouralReportEntitiesList = new ArrayList<>();
		List<CSPBehaviouralReportEntity> behaviouralReportEntitiesList = new ArrayList<>();
		Set<RpsBehaviouralTestCharacteristic> rpCharacteristicSet = null;
		List<String> failedloginID = new ArrayList<>();

		for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {

			String loginIdForCandidate = "";
			RpsCandidate rpsCandidate =
					rpsMasterAssociationService.getCandidateForUniqueId(rpsMasterAssociation.getUniqueCandidateId());
			RpsQuestionPaper rpsQuestionPaper = null;
			if (qpID == null || qpID == 0) {
				if (rpsMasterAssociation != null && rpsMasterAssociation.getCandidateResponses() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper() != null
						&& rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId() != null) {
					qpID = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper().getQpId();
					rpsQuestionPaper = rpsMasterAssociation.getCandidateResponses().getRpsQuestionPaper();
				}
			}
			if (rpsQuestionPaper == null)
				rpsQuestionPaper = rpsQuestionPaperService.findOne(qpID);

			CSPBehaviouralReportEntity behaviouralReportEntity = new CSPBehaviouralReportEntity();

			rpCharacteristicSet = getCandidateDetailsForBehaviouralReports(rpsCandidate, rpsMasterAssociation,
					rpsQuestionPaper, behaviouralReportEntity, BehavioralTestType.CSP.toString(), "dd MMMM, yyyy");

			Type gradeType = new TypeToken<ArrayList<BehaviouralGragingSchemaEntity>>() {
			}.getType();
			Type behavTestScoreType = new TypeToken<HashMap<String, Double>>() {
			}.getType();

			RpsCandidateResponseLite responseLite = rpsCandidateResponseRepository
					.findByMasterAssociationId(rpsMasterAssociation.getUniqueCandidateId());
			if (responseLite == null) {
				logger.debug("response data is null for uniqueCandidateIds: {}", uniqueCandidateIds);
				fileExportExportEntity =
						new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
				return gson.toJson(fileExportExportEntity);
			}
			Map<String, Double> behaviouralTestScoreMap =
					gson.fromJson(responseLite.getBehaviouralTestScores(), behavTestScoreType);

			// put here
			boolean isFullResponse = true;
			Type type = new TypeToken<ArrayList<CandidateResponseEntity>>() {
			}.getType();
			List<CandidateResponseEntity> candidateResponseEntityList = new ArrayList<>();
			String candidateJsonResponse = responseLite.getResponse();
			if (candidateJsonResponse != null)
				candidateResponseEntityList = new Gson().fromJson(candidateJsonResponse, type);
			int questionsCount = rpsQuestionPaper.getTotalQuestions();
			int responseCount = candidateResponseEntityList.isEmpty() ? 0 : candidateResponseEntityList.size();
			if (questionsCount != responseCount)
				isFullResponse = false;

			try {
				loginIdForCandidate = rpsMasterAssociation.getLoginID();

				if (!isFullResponse)
					throw new RpsException("in complete assessment");
				for (RpsBehaviouralTestCharacteristic rpCharacteristic : rpCharacteristicSet) {

					List<BehaviouralGragingSchemaEntity> gradeList =
							gson.fromJson(rpCharacteristic.getGrade(), gradeType);
					getGradeListForCSPBehaviouralReports(behaviouralReportEntity, behaviouralTestScoreMap,
							rpCharacteristic, 0.0, gradeList);
				}

				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CSP.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				behaviouralReportEntitiesList.add(behaviouralReportEntity);
			}

			catch (Exception e) {
				File path = null;
				path = new File(behavioralFolder);
				if (!path.isDirectory()) {
					path.mkdirs();
				}
				path = new File(path + File.separator + BehavioralTestType.CSP.toString() + "_"
						+ rpsMasterAssociation.getLoginID() + "_" + Calendar.getInstance().getTimeInMillis() + ".pdf");
				behaviouralReportEntity.setPdfFileName(path.toString());
				behaviouralReportEntity.setLogoPath(" ");

				failedBehaviouralReportEntitiesList.add(behaviouralReportEntity);

				failedloginID.add(loginIdForCandidate);

				continue;
			}

		}
		try {
			CSPPDFGenerator.failedpdfGenerator(failedBehaviouralReportEntitiesList);
			CSPPDFGenerator.pdfGenerator(behaviouralReportEntitiesList);

			if (isZipFileRequired) {
				List<String> pathsList = new ArrayList<>();
				for (BehaviouralReportEntity bReportEntity : behaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());
				for (BehaviouralReportEntity bReportEntity : failedBehaviouralReportEntitiesList)
					pathsList.add(bReportEntity.getPdfFileName());

				ZipUtility.archiveFiles(pathsList, pathForZip.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.FAILED_STATUS, null, "Fail to generate PDF");
		}
		if (isZipFileRequired) {
			File behavioralDownloadingPath = new File(pathForZip + ".zip");
			fileExportExportEntity =
					new FileExportExportEntity(RpsConstants.SUCCESS_STATUS, behavioralDownloadingPath.toString(),
							"Incomplete report for loginId :" + failedloginID + " due to in-complete assessments",
							failedloginID);
		} else {
			if (isBehaviouralSectionScoresRequired.equalsIgnoreCase("true")) {
				if (behaviouralReportEntitiesList != null)
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							behaviouralReportEntitiesList.get(0).getPdfFileName(), "PDF generation has success");
			} else {
				if (failedBehaviouralReportEntitiesList != null) {
					failedBehaviouralReportEntitiesList.addAll(behaviouralReportEntitiesList);
					fileExportExportEntity = new FileExportExportEntity(RpsConstants.SUCCESS_STATUS,
							failedBehaviouralReportEntitiesList.get(0).getPdfFileName(),
							"In-complete report for loginId : " + failedloginID + " due to in-complete assessments",
							failedloginID);
				}

			}
		}

		return gson.toJson(fileExportExportEntity);
	}

	private void getGradeListForCSPBehaviouralReports(CSPBehaviouralReportEntity behaviouralReportEntity,
			Map<String, Double> behaviouralTestScoreMap, RpsBehaviouralTestCharacteristic rpCharacteristic,
			double totalScore, List<BehaviouralGragingSchemaEntity> gradeList) throws RpsException {
		switch (rpCharacteristic.getcId()) {
			// Personalized=COL+CUC+RES
			// Personalized=COL+CUC+RES
			// Personalized=COL+CUC+RES

			// CSP
			case "CSP_COL":
				double score = behaviouralTestScoreMap.get("CSP_COL");
				behaviouralReportEntity.setColScore(score);
				double perc = (score / 28) * 100;
				double roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setColPerc(roundedPerc);
				behaviouralReportEntity
						.setColGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_CUC":
				score = behaviouralTestScoreMap.get("CSP_CUC");
				behaviouralReportEntity.setCucScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setCucPerc(roundedPerc);
				behaviouralReportEntity
						.setCucGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_RES":
				score = behaviouralTestScoreMap.get("CSP_RES");
				behaviouralReportEntity.setResScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setResPerc(roundedPerc);
				behaviouralReportEntity
						.setResGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_PRS":
				score = behaviouralTestScoreMap.get("CSP_PRS");
				behaviouralReportEntity.setPrsScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setPrsPerc(roundedPerc);
				behaviouralReportEntity
						.setPrsGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_ACO":
				score = behaviouralTestScoreMap.get("CSP_ACO");
				behaviouralReportEntity.setAcoScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setAcoPerc(roundedPerc);
				behaviouralReportEntity
						.setAcoGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_ASS":
				score = behaviouralTestScoreMap.get("CSP_ASS");
				behaviouralReportEntity.setAssScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setAssPerc(roundedPerc);
				behaviouralReportEntity
						.setAssGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_RR":
				score = behaviouralTestScoreMap.get("CSP_RR");
				behaviouralReportEntity.setFactualQuestionsScore(score);
				perc = (score / 5) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setFactualQuestionsPerc(roundedPerc);
				behaviouralReportEntity.setFactualQuestionsGrade(
						this.generateBehaviouralGradeOnCandidateScore(gradeList, score));
				break;
			case "CSP_POC":
				score = behaviouralTestScoreMap.get("CSP_POC");
				behaviouralReportEntity.setPocScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setPocPerc(roundedPerc);
				behaviouralReportEntity
						.setPocGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_EPY":
				score = behaviouralTestScoreMap.get("CSP_EPY");
				behaviouralReportEntity.setEpyScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setEpyPerc(roundedPerc);
				behaviouralReportEntity
						.setEpyGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			case "CSP_SEO":
				score = behaviouralTestScoreMap.get("CSP_SEO");
				behaviouralReportEntity.setSeoScore(score);
				perc = (score / 28) * 100;
				roundedPerc = roundPercToInteger(perc);
				behaviouralReportEntity.setSeoPerc(roundedPerc);
				behaviouralReportEntity
						.setSeoGrade(this.generateBehaviouralGradeOnCandidateScore(gradeList, roundedPerc));
				break;
			default:
				break;
		}
	}
}
