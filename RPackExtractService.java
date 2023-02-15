package com.merittrac.apollo.rps.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.hibernate.exception.LockTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.ApolloConstants.DependentPackType;
import com.merittrac.apollo.common.ApolloConstants.PackStatus;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.JMSRequeException;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.calendarUtil.TimeUtil;
import com.merittrac.apollo.common.entities.acs.AttendanceExportEntity;
import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.common.entities.acs.CandidateAnswersEntity;
import com.merittrac.apollo.common.entities.acs.CandidateIdentifiersEntity;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.common.entities.acs.RPackExportEntity;
import com.merittrac.apollo.common.entities.acs.RpackMetaDataExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.entities.rps.BpackRequestEntity;
import com.merittrac.apollo.common.entities.rps.QppackRequestEntity;
import com.merittrac.apollo.common.entities.rps.QptDetailsEntity;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateMIFDetails;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsCumulativeResponses;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsMasterAssociation;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.entity.RpsPackFailedStatus;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsQuestionPaperPack;
import com.merittrac.apollo.data.entity.RpsRpackComponent;
import com.merittrac.apollo.data.entity.RpsWetScoreEvaluation;
import com.merittrac.apollo.data.repository.RpsWetDataEvaluationRepository;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsCandidateMIFDetailsService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsCandidateService;
import com.merittrac.apollo.data.service.RpsCumulativeResponsesService;
import com.merittrac.apollo.data.service.RpsDivisionService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsPackFailedStatusService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.data.service.RpsQuestionPaperPackService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsRpackComponentService;
import com.merittrac.apollo.data.service.RpsWetDataEvaluationService;
import com.merittrac.apollo.jms.rabbitmq.util.FileObject;
import com.merittrac.apollo.rps.common.PackAlreadyProcessedException;
import com.merittrac.apollo.rps.common.RPSAuthenticationException;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.RpsRpackException;
import com.merittrac.apollo.rps.jms.receiver.AbstractReceiver;
import com.merittrac.apollo.rps.log.processor.AdminAuditProcessor;
import com.merittrac.apollo.rps.log.processor.CandidateLogsProcessor;
import com.merittrac.apollo.tp.mif.ContactInfo;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * RPackExtractService process one or more RPacks associated with Meta Data json file and update necessary database
 * tables
 * 
 * @author Dharmendra Agrawal
 * @author Amar_K
 * @author Moulya_P
 * 
 */

public class RPackExtractService extends AbstractReceiver implements
		IRPackExtractService {

	@Autowired
	private RpsRpackComponentService rpsRpackComponentService;

	@Autowired
	private RpsPackService rpsPackService;

	@Autowired
	private RpsQuestionPaperService rpsQuestionPaperService;

	@Autowired
	private RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	private RpsCandidateResponseService rpsCandidateResponseService;

	@Autowired
	private RpsCandidateService rpsCandidateService;

	@Autowired
	private RpsCandidateMIFDetailsService rpsCandidateMIFDetailsService;

	@Autowired
	private RpsAssessmentService rpsAssessmentService;

	@Autowired
	private RpsEventService rpsEventService;

	@Autowired
	private RpsCumulativeResponsesService rpsCumulativeResponsesService;

	@Autowired
	RpsQuestionPaperPackService rpsQuestionPaperPackService;

	@Autowired
	MailTriggerService mailTriggerService;

	@Autowired
	Gson gson;

	@Autowired
	CryptUtil cryptUtil;

	@Autowired
	AdminAuditProcessor adminAuditProcessor;

	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	@Autowired
	AttendanceExtractService attendanceExtractService;

	@Autowired
	CandidateLogsProcessor candidateLogsProcessor;

	@Value("${isRPackDIREncrypted}")
	private String isRPackDIREncrypted;

	@Value("${is_email_trigger_enabled}")
	private String emailTriggerEnabled;

	@Value("${rpackMetaData}")
	private String isRpackMetaDataEncptd;

	@Value("${logs}")
	private String isLogsEncptd;

	@Value("${responses}")
	private String isResponsesEncptd;

	@Value("${attendance}")
	private String isAttendanceEncptd;
	
	@Value("${rpack_dependent_message_on_qppack}")
	private String rpackDependentMessageQPpack;

	@Value("${rpack_dependent_message_on_bpack}")
	private String rpackDependentMessageBpack;

	@Value("${responsejson}")
	private String responsejson;

	@Autowired
	@Qualifier("rpackDependencyMessageReaderChannel")
	private MessageChannel rpackDependencyMessageReaderChannel;

	@Autowired
	RpsPackFailedStatusService rpsPackFailedStatusService;

	@Autowired
	RpsWetDataEvaluationService rpsWetDataEvaluationService;

	@Value("${rpackMetaDatajson}")
	private String rpackMetaDatajson;

	@Autowired
	RpsDivisionService rpsDivisionService;

	@Value("${validateMacId}")
	private String validateMacId;

	@Value("${apollo_home_dir}")
	private String apollo_home_dir;

	@Autowired
	RpsWetDataEvaluationRepository rpsWetDataEvaluationRepository;

	private static final String DEFAULT_DELTA_PACK_INFO = "yes";
	private static final String KEY_BATCHCODE = "batchCode";
	private static final String KEY_ACSCODE = "assessmentServerCode";
	private static final String KEY_VERSION_NO = "versionNo";
	private static final String KEY_PACK_CODE = "packCode";
	private static final String KEY_DELTA_PACK = "isDeltaPackExists";
	private static final String KEY_PACK_TYPE = "packType";
	private static final String KEY_PACK_SUB_TYPE = "packSubType";
	private static final String MAC_ADDERESS = "macAddresses";
	private static final String KEY_CUSTOMERCODE = "customerCode";
	private static final String KEY_DIVISIONCODE = "divisionCode";
	private static final String KEY_EVENTCODE = "eventCode";
	protected static final Logger LOGGER = LoggerFactory.getLogger(RPackExtractService.class);

	public RPackExtractService() throws RpsException {
		super();
	}

	private String getFolderName(final String rPackFilename) {
		return FilenameUtils.removeExtension(rPackFilename);
	}

	/**
	 * checks the batch and acs
	 * 
	 * @param rpsPack
	 * @return
	 */

	/**
	 * Getting the RPacks and Extracting those Rpacks to local uesr-home After Extracting RPack zip file getting meta
	 * data json and using json updating necessary database tables
	 * 
	 * RPack is receiving from jms queue and that receiver class calling this service Api
	 * 
	 * @param localDownloadPath
	 *            downloaded path
	 * @param downloadTime
	 *            RPack download time
	 * @param filename
	 *            Rpack File name
	 * @param messageHeaders
	 *            the MessageHeaders which contains jms header content for RPack
	 * @param rpsPack2
	 * @throws JMSRequeException
	 * @throws ApolloSecurityException
	 * @throws ParseException
	 * @throws RpsRpackException
	 * @throws PackAlreadyProcessedException
	 * @throws RPSAuthenticationException
	 */

	@Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
	public boolean extractRPack(final String localDownloadPath,
			final Date downloadTime, final String filename,
			final MessageHeaders messageHeaders, RpsPack rpsPack,
			boolean isManualUpload, final Message<FileObject> message)
			throws RpsException, ZipException, IOException, JMSRequeException, ApolloSecurityException, ParseException,
			PackAlreadyProcessedException, RPSAuthenticationException, LockTimeoutException {

		LOGGER.info("--IN Extracting RPack --");

		String customer = "";
		String division = "";
		String event = messageHeaders.get(eventCode).toString().trim();

		boolean status = false;

		// Extract Rpack.zip
		try {
			String encryptedFilePath = localDownloadPath + File.separator + filename;
			String decryptedFilePath = localDownloadPath + File.separator + filename + "_decrypted.zip";
			LOGGER.debug("Extracting RPack : " + encryptedFilePath + "decrypting to :" + decryptedFilePath);

			if (isRPackDIREncrypted.equalsIgnoreCase(rpackEncrypt)) {
				try {
					cryptUtil.decryptFileUsingAES(new File(encryptedFilePath), new File(decryptedFilePath), event);
				} catch (Exception e) {
					LOGGER.error(" RPACK decryption failed : ", e.getMessage());
					throw e;
				}
			} else
				FileUtils.copyFile(new File(encryptedFilePath), new File(decryptedFilePath));

			if (new ZipFile(decryptedFilePath).isValidZipFile()) {
				String localExtractPath = "";
				String decryptLocalPath = localDownloadPath + File.separator + this.getFolderName(filename);

				if (isRPackDIREncrypted.equalsIgnoreCase(rpackEncrypt))
					localExtractPath = localDownloadPath + File.separator + this.getFolderName(filename) + temp;
				else
					localExtractPath = localDownloadPath + File.separator + this.getFolderName(filename);

				ZipUtility.extractAllOptimized(localDownloadPath + File.separator + filename + "_decrypted.zip",
						localExtractPath, true);
				LOGGER.info("RPack has been Extracted at the path = {} " + localDownloadPath);
				if (!isManualUpload) {
					try {
						rpsPack = constructRpsPackData(messageHeaders);
					} catch (RpsRpackException e) {
						LOGGER.error("Error in Rpack extraction, Bpack dependency {} ", e.getMessage());
						// check for qppack dependency as well
						doDependencyChecks(messageHeaders, localExtractPath, localDownloadPath);
						throw e;
					}
				}
				// updating Pack Status as PACK_RECEIVED--
				if (messageHeaders != null) { // Received thru JMS
					customer = messageHeaders.get(customerCode).toString().trim();
					division = messageHeaders.get(divisionCode).toString().trim();
					event = messageHeaders.get(eventCode).toString().trim();
					LOGGER.info(" RPACK Customer : {} \n Division : {} \n Event : {} \n for rpack {}.", customer,
							division, event, filename);
					LOGGER.info(" RPack encryption selected is :" + isRPackDIREncrypted);
					if (rpsPack != null && !isManualUpload) { // RPack received through JMS
						rpsPack.setPackStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
						if (rpsPack.getRpsBatchAcsAssociation().getRpsBatch() != null
								&& rpsPack.getRpsBatchAcsAssociation().getRpsAcsServer() != null
								&& rpsPack.getPackId().length() != 0 && rpsPack.getPackType().length() != 0
								&& rpsPack.getVersionNumber().length() != 0) {
							try {
							rpsPack = rpsPackService.addRpsPack(rpsPack);
							} catch (Exception e) {
								// save again if LockTimeoutException occurs
								LOGGER.error("ERROR :: Save again, while saving the addRpsPack :: {} ", e);
								rpsPack = rpsPackService.addRpsPack(rpsPack);
							}
						} else {
							LOGGER.error(
									"Rpack Headers are not correct, or there is no corresponding entries in database",
									filename);
							throw new RpsException(
									"Rpack Headers are not correct, or there is no corresponding entries in database");
						}
					}
				}

				// Read rpackMetaData.json from Rpack
				if (isManualUpload) { // RPack uploaded manually
					rpsPack.setPackStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
					try {
						rpsPack = rpsPackService.addRpsPack(rpsPack);
					} catch (Exception e) {
						// save again if LockTimeoutException occurs
						LOGGER.error("ERROR :: Save again, while saving the addRpsPack :: {} ", e);
						rpsPack = rpsPackService.addRpsPack(rpsPack);
					}
				}
				LOGGER.trace("Rpack rpsPack.getRpsBatchAcsAssociation() {} ",
						rpsPack.getRpsBatchAcsAssociation().getBatchAcsId());

				// add components info to database
				rpsPack = updateRPackComponentDetails(FilenameUtils.separatorsToSystem(localExtractPath), rpsPack,
						event, encryptedFilePath);

				RPackExportEntity rPackExportEntity =
						(RPackExportEntity) getObjectFromJSON(localExtractPath + File.separator + responsejson,
								RPackExportEntity.class);

				// Read response.json from Rpack_1
				rpsPack = validateResponsesJson(filename, localExtractPath, localDownloadPath, downloadTime, rpsPack,
						customer, division, event, messageHeaders, rPackExportEntity);

				// fetching master asso details for RpsBatchAcsAssociation
				List<RpsMasterAssociation> rpsMasterAssociations =
						rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsPack.getRpsBatchAcsAssociation());

				Map<String, RpsMasterAssociation> rpsMasterAssociationsMap =
						new HashMap<String, RpsMasterAssociation>();
				if (rpsMasterAssociations != null && !rpsMasterAssociations.isEmpty()) {
					LOGGER.trace("Rpack rpsMasterAssociations() {} ", rpsMasterAssociations.size());
					for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
						rpsMasterAssociationsMap.put(rpsMasterAssociation.getRpsCandidate().getCandidateId1(),
								rpsMasterAssociation);
					}
				}
				// releasing
				rpsMasterAssociations = null;
				try {
					// read candidate Attendance Details And CandidateLogs
					rpsPack =
							updateAttendanceDetailsAndCandidateLogs(FilenameUtils.separatorsToSystem(localExtractPath),
									rpsPack, event, rpsMasterAssociationsMap, encryptedFilePath);
					// read candidate Answers List
					readCandidateAnswers(rPackExportEntity, rpsPack.getRpsBatchAcsAssociation(),
							rpsMasterAssociationsMap, customer, division, event, rpsPack.getPackId(), localDownloadPath,
							messageHeaders);
				} catch (RpsRpackException e) {
					LOGGER.error("Error in Rpack extraction, Bpack dependency {} ", e.getMessage());
					// check for qppack dependency as well
					doDependencyChecks(messageHeaders, localExtractPath, localDownloadPath);
					throw e;
				}
				// releasing
				rpsMasterAssociationsMap = null;

				if (isManualUpload)
					rpsPack.setPackReceivingMode(RpsConstants.packReceiveMode.MANUAL_UPLOAD.toString());
				else
					rpsPack.setPackReceivingMode(RpsConstants.packReceiveMode.JMS_UPLOAD.toString());
				rpsPack.setPackStatus(RpsConstants.packStatus.UNPACKED.toString());
				rpsPack = rpsPackService.addRpsPack(rpsPack);

				// deleting Encrypted RPack unzip file
				deletePackAfterProcessing(encryptedFilePath, decryptedFilePath, localExtractPath, decryptLocalPath);
				status = true;
			} else {
				// unsuccessful pack
				try {
					if (rpsPack == null)
						rpsPack = constructRpsPackData(messageHeaders);
				} catch (RpsRpackException e) {
					LOGGER.error("Error in Rpack extraction, Bpack dependency {} ", e.getMessage());
					// check for qppack dependency as well
					doDependencyChecks(messageHeaders, null, localDownloadPath);
					throw e;
				}

				rpsPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());
				rpsPack = rpsPackService.addRpsPack(rpsPack);
				List<RpsRpackComponent> rpsRpackComponentList = new ArrayList<>();
				setComponentStatus(rpsRpackComponentList, RpsConstants.packStatus.UNKNOWN.toString(), rpsPack);
				rpsRpackComponentService.saveRPackComponentDetails(rpsRpackComponentList);
				throw new ZipException("ZIP exception occured while extracting file ");
			}
		}
		catch (RpsRpackException e) {
			LOGGER.error("Error in Rpack extraction, Bpack dependency" + filename, e.getMessage());
			if (!isManualUpload)
				rpackDependencyMessageReaderChannel.send(message);
		} catch (LockTimeoutException e) {
			LOGGER.error("LockTimeoutException in Rpack extraction" + filename, e.getMessage());
			throw e;
		} catch (Exception e) {
			LOGGER.error("Error in Rpack extraction" + filename, e.getMessage());
			throw e;
		}
		LOGGER.info("--OUT Extracting RPack --");
		return status;

	}

	public static <T> List<List<T>> getPaginatedResponses(Collection<T> c, Integer pageSize) {
		if (c == null)
			return Collections.emptyList();
		List<T> list = new ArrayList<T>(c);
		if (pageSize == null || pageSize <= 0 || pageSize > list.size())
			pageSize = list.size();
		int numPages = (int) Math.ceil((double) list.size() / (double) pageSize);
		List<List<T>> pages = new ArrayList<List<T>>(numPages);
		for (int pageNum = 0; pageNum < numPages;)
			pages.add(list.subList(pageNum * pageSize, Math.min(++pageNum * pageSize, list.size())));
		return pages;
	}

	private void deletePackAfterProcessing(String encryptedFilePath, String decryptedFilePath, String localExtractPath,
			String decryptLocalPath) throws IOException {
		File encryptFile = new File(localExtractPath);
		if (encryptFile.isDirectory())
			FileUtils.deleteDirectory(encryptFile);

		// deleting Decrypted RPack unzip file
		File decryptedFile = new File(decryptLocalPath);
		if (decryptedFile.isDirectory())
			FileUtils.deleteDirectory(decryptedFile);

		// delete the decrypted and encrypted zip file
		FileUtils.deleteQuietly(new File(decryptedFilePath));
		FileUtils.deleteQuietly(new File(encryptedFilePath));
	}

	private RpsPack constructRpsPackData(final MessageHeaders messageHeaders)
			throws PackAlreadyProcessedException, RPSAuthenticationException, RpsRpackException {
		LOGGER.debug("--IN-- constructRpsPackData : " + messageHeaders);
		final String batchCode = messageHeaders.get(KEY_BATCHCODE).toString();
		final String acsServerId = messageHeaders.get(KEY_ACSCODE).toString();

		RpsBatchAcsAssociation rpsBatchAcsAssociation =
				rpsBatchAcsAssociationService.getAssociationByBatchCodeAndAcsId(batchCode, acsServerId);

		if (rpsBatchAcsAssociation == null) {
			LOGGER.error(
					"RPack . No association between Batch {} and ACS server {}. Hence Requeueing.",
					batchCode, acsServerId);
			throw new RpsRpackException(
					"RPack . No association between Batch {} and ACS server {}. Hence not Requeueing, keeping in Apollo home");
		}

		if (validateMacId.equals("1")) {
			List<String> macAddresses = null;
			if (messageHeaders.get(MAC_ADDERESS) != null) {
				if (messageHeaders.get(MAC_ADDERESS) instanceof Set)
					macAddresses = new ArrayList((Set<String>) messageHeaders.get(MAC_ADDERESS));
				else
					macAddresses = (List<String>) messageHeaders.get(MAC_ADDERESS);
			}

			Set<String> macAddSet = new Gson().fromJson(rpsBatchAcsAssociation.getMacAddress(), Set.class);

			if (macAddSet == null || macAddSet.isEmpty()) {
				LOGGER.info("In Database MacAddress is not available, where batchAcsID :: {} ",
						rpsBatchAcsAssociation.getBatchAcsId() + " and MacAddress :: {} ",
						rpsBatchAcsAssociation.getMacAddress());
				throw new RpsRpackException(
						"RPack thru JMS. No In Database MacAddress is not available. Hence not Requeueing, keeping in Apollo home");
			}
			boolean isValidMac = false;
			for (Object macAddress : macAddresses) {
				if (macAddSet.contains(macAddress.toString())) {
					isValidMac = true;
					break;
				}
			}

			if (!isValidMac) {
				LOGGER.info("Mac Address is not available in database");
				throw new RpsRpackException(
						"RPack thru JMS. Mac Address is not available in database. Hence not Requeueing, keeping in Apollo home");
			}
		}

		RpsPack rpsPack = this.getRPackHeader(messageHeaders, rpsBatchAcsAssociation);

		return rpsPack;
	}

	private Set<String> checkQpPackDependency(RpackMetaDataExportEntity rpackMetaDataExportEntity) {
		LOGGER.debug("--IN-- checkQpPackDependency : " + rpackMetaDataExportEntity.getPackCode());
		try {
			Set<String> questionPaperCodes = rpackMetaDataExportEntity.getQuestionPaperCodes();
			if (questionPaperCodes == null) {
				LOGGER.debug(
						"checkQpPackDependency : questionPaperCodes dependency not received from ACS"
								+ questionPaperCodes);
				return null;
			}
			List<RpsQuestionPaper> qpapers = rpsQuestionPaperService.findByQpCodes(new ArrayList<>(questionPaperCodes));

			if (qpapers == null || qpapers.isEmpty() || questionPaperCodes.size() != qpapers.size()) {
				LOGGER.debug("checkQpPackDependency : qp from rpack : "
						+ questionPaperCodes.size() + " qp in db ");
				// count mismatch, hence qp pack dependency exists
				return questionPaperCodes;
			}
		} catch (Exception e) {
			LOGGER.error(
					"Cannot find dependency on QP's .. Exception while executing checkQpPackDependency...",
					e);
		}
		return null;
	}

	private RpackMetaDataExportEntity parseRpsMetaDataJson(String metadatapath) {
		LOGGER.debug("--IN-- parseRpsMetaDataJson : " + metadatapath);
		if (metadatapath == null)
			return null;
		try {
		RpackMetaDataExportEntity rpackMetaDataExportEntity =
				(RpackMetaDataExportEntity) getObjectFromJSON(metadatapath + File.separator + rpackMetaDatajson,
						RpackMetaDataExportEntity.class);
			return rpackMetaDataExportEntity;
		} catch (Exception e) {
			LOGGER.error("Cannot parse on rps metadata json file.. Exception while executing parseRpsMetaDataJson...",
					e);

		}
		return null;
	}

	private void setComponentStatus(
			List<RpsRpackComponent> rpsRpackComponentList, String status,
			RpsPack rpsPack) {
		RpsRpackComponent rpsRpackAdminLog = new RpsRpackComponent();
		rpsRpackAdminLog.setStatus(status);
		rpsRpackAdminLog.setRpsPack(rpsPack);
		rpsRpackAdminLog
				.setRpackComponentName(RpsConstants.RpackComponents.ADMIN_LOG
						.toString());
		rpsRpackComponentList.add(rpsRpackAdminLog);

		RpsRpackComponent rpsRpackAttendance = new RpsRpackComponent();
		rpsRpackAttendance.setStatus(status);
		rpsRpackAttendance.setRpsPack(rpsPack);
		rpsRpackAttendance
				.setRpackComponentName(RpsConstants.RpackComponents.ATTENDANCE_JSON
						.toString());
		rpsRpackComponentList.add(rpsRpackAttendance);

		RpsRpackComponent rpsRpackCandLog = new RpsRpackComponent();
		rpsRpackCandLog.setStatus(status);
		rpsRpackCandLog.setRpsPack(rpsPack);
		rpsRpackCandLog
				.setRpackComponentName(RpsConstants.RpackComponents.CANDIDATE_LOG
						.toString());
		rpsRpackComponentList.add(rpsRpackCandLog);

		RpsRpackComponent rpsRpackResponse = new RpsRpackComponent();
		rpsRpackResponse.setStatus(status);
		rpsRpackResponse.setRpsPack(rpsPack);
		rpsRpackResponse
				.setRpackComponentName(RpsConstants.RpackComponents.RESPONSE_JSON
						.toString());
		rpsRpackComponentList.add(rpsRpackResponse);

	}

	private RpsPack updateRPackComponentDetails(String rpackPath, RpsPack rpsPack,
			String event, String rpackDownloadLocation)
			throws ApolloSecurityException, IOException,
			RpsException, ParseException {
		LOGGER.info("--IN updating RPack components --");
		File file = new File(rpackPath);
		List<RpsRpackComponent> rpsPackComponentList = new ArrayList<>();
		if (file.list() == null) {
			List<RpsRpackComponent> rpsRpackComponentList = new ArrayList<>();
			setComponentStatus(rpsRpackComponentList, RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString(),
					rpsPack);
			rpsRpackComponentService.saveRPackComponentDetails(rpsRpackComponentList);
		} else {
			List<RpsRpackComponent> oldRpackComponentsList = rpsRpackComponentService.getRpackComponentListOnType(
					rpsPack, Arrays.asList(RpsConstants.RpackComponents.RESPONSE_JSON.toString(),
							RpsConstants.RpackComponents.INCIDENTAUDIT_LOG.toString()));
			if(oldRpackComponentsList!=null && !oldRpackComponentsList.isEmpty()){
				rpsRpackComponentService.deleteComponentsList(oldRpackComponentsList);
			}

			for (String fileName : file.list()) {
				if (responses_file.equals(fileName)) {
					RpsRpackComponent rpsRpackComponentResponse =new RpsRpackComponent();
					rpsRpackComponentResponse.setRpackComponentName(RpsConstants.RpackComponents.RESPONSE_JSON.toString());
					rpsRpackComponentResponse.setRpackComponentFilePath(rpackPath.replaceAll("//", "\\\\"));
					rpsRpackComponentResponse.setCreationDate(new Date());
					rpsRpackComponentResponse.setStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
					rpsRpackComponentResponse.setRpsPack(rpsPack);
					rpsPackComponentList.add(rpsRpackComponentResponse);
				}

				if (incidentAudit_file.equalsIgnoreCase(fileName)) {
					RpsRpackComponent rpsRpackComponentResponse =new RpsRpackComponent();
					rpsRpackComponentResponse.setRpackComponentName(RpsConstants.RpackComponents.INCIDENTAUDIT_LOG.toString());
					rpsRpackComponentResponse.setRpackComponentFilePath(rpackPath.replaceAll("//", "\\\\"));
					rpsRpackComponentResponse.setCreationDate(new Date());
					rpsRpackComponentResponse.setStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
					rpsRpackComponentResponse.setRpsPack(rpsPack);
					rpsPackComponentList.add(rpsRpackComponentResponse);
					String incidentAuditPath = rpackPath + File.separator + fileName;
					rpsPack = this.updateIncidentAuditDetails(incidentAuditPath, rpsPack);
				}

			}

			File auditLogFile = new File(rpackPath + File.separator
					+ auditLogFolderName);

			if (auditLogFile.list() != null && auditLogFile.list().length != 0) {
				String adminAuditFilename = auditLogFile.list()[0];
				String adminAuditpath = rpackPath + File.separator
						+ auditLogFolderName + File.separator
						+ adminAuditFilename;
				adminAuditProcessor.updateAdminAuditDetails(adminAuditpath,
						rpsPack);

			} else
				LOGGER.info(
						"Admin Audit Log file is not available in RPAck,  which Admin Audit path is :: {} ",
						auditLogFile.getAbsolutePath());

			try {
				rpsRpackComponentService.saveRPackComponentDetails(rpsPackComponentList);
			} catch (Exception e) {
				LOGGER.error(" Saving of RPACK component failed ", e);
				throw new RpsException(" RPack component save failed");
			}
		}
		LOGGER.info("--OUT updating RPack components --");
		return rpsPack;
	}

	private RpsPack updateAttendanceDetailsAndCandidateLogs(String rpackPath, RpsPack rpsPack, String event,
			Map<String, RpsMasterAssociation> rpsMasterAssociationsMap, String rpackDownloadLocation)
			throws ApolloSecurityException, IOException, RpsException, ParseException, RpsRpackException {
		LOGGER.info("--IN updating RPack components --");
		File file = new File(rpackPath);
		List<RpsRpackComponent> rpsPackComponentList = new ArrayList<>();
		if (file.list() == null) {
			List<RpsRpackComponent> rpsRpackComponentList = new ArrayList<>();
			setComponentStatus(rpsRpackComponentList, RpsConstants.packStatus.PACKS_NOT_RECEIVED.toString(), rpsPack);
			rpsRpackComponentService.saveRPackComponentDetails(rpsRpackComponentList);
		} else {
			List<RpsRpackComponent> oldRpackComponentsList = rpsRpackComponentService.getRpackComponentListOnType(
					rpsPack, Arrays.asList(RpsConstants.RpackComponents.ATTENDANCE_JSON.toString(),
							RpsConstants.RpackComponents.CANDIDATE_LOG.toString()));
			if (oldRpackComponentsList != null && !oldRpackComponentsList.isEmpty()) {
				rpsRpackComponentService.deleteComponentsList(oldRpackComponentsList);
			}

			for (String fileName : file.list()) {
				if (attendance_file.equals(fileName)) {
					RpsRpackComponent rpsRpackComponentAttendance = new RpsRpackComponent();
					rpsRpackComponentAttendance
							.setRpackComponentName(RpsConstants.RpackComponents.ATTENDANCE_JSON.toString());
					rpsRpackComponentAttendance.setRpackComponentFilePath(rpackPath.replaceAll("//", "\\\\"));
					rpsRpackComponentAttendance.setCreationDate(new Date());
					rpsRpackComponentAttendance.setStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
					rpsRpackComponentAttendance.setRpsPack(rpsPack);
					rpsPackComponentList.add(rpsRpackComponentAttendance);
					rpsPack = this.readAndProcessAttendanceReport(new File(rpackPath + File.separator + fileName),
							event, rpsPack, rpsMasterAssociationsMap, rpackDownloadLocation);
				}
			}

			File candLogFile = new File(rpackPath + File.separator + candLofFolderName);

			if (candLogFile.list() != null && candLogFile.list().length != 0) {
				RpsRpackComponent rpsRpackComponentCand = new RpsRpackComponent();
				rpsRpackComponentCand.setRpackComponentName(RpsConstants.RpackComponents.CANDIDATE_LOG.toString());
				rpsRpackComponentCand.setRpackComponentFilePath(rpackPath.replaceAll("//", "\\\\"));
				rpsRpackComponentCand.setCreationDate(new Date());
				rpsRpackComponentCand.setStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
				rpsRpackComponentCand.setRpsPack(rpsPack);
				rpsPackComponentList.add(rpsRpackComponentCand);
				candidateLogsProcessor.processCandidateLogs(candLogFile, rpsMasterAssociationsMap);
			}

			try {
				rpsRpackComponentService.saveRPackComponentDetails(rpsPackComponentList);
			} catch (Exception e) {
				LOGGER.error(" Saving of RPACK component failed ", e);
				throw new RpsException(" RPack component save failed");
			}
		}
		LOGGER.info("--OUT updating RPack components --");
		return rpsPack;
	}
	/**
	 * updating Incident Audit Log to the database
	 * 
	 * @param incidentAuditPath
	 * @param rpsPack
	 */
	private RpsPack updateIncidentAuditDetails(String incidentAuditPath, RpsPack rpsPack) {
		// reading incidentAudit Log file
		//BufferedReader bufferedReader = null;
		try (FileReader fileReader = new FileReader(incidentAuditPath); BufferedReader bufferedReader = new BufferedReader(fileReader);) {
			//FileReader fileReader = new FileReader(incidentAuditPath);
			//bufferedReader = new BufferedReader(fileReader);
			StringBuilder stringBuilder = new StringBuilder();
			String readLine = bufferedReader.readLine();

			while (readLine != null) {
				stringBuilder.append(readLine);
				stringBuilder.append("\n");
				readLine = bufferedReader.readLine();
			}

			String incidentAuditString = stringBuilder.toString();
			LOGGER.info("Incident Audit Log File :: {} ", incidentAuditString);
			// fetch the latest RpsBatchAcsAssociation
			RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsPack.getRpsBatchAcsAssociation();
			// rpsBatchAcsAssociationService
			// .find(rpsPack.getRpsBatchAcsAssociation().getBatchAcsId());
			if(!incidentAuditString.isEmpty()){
				rpsBatchAcsAssociation.setIncidentAuditDetails(incidentAuditString);
				rpsBatchAcsAssociation = rpsBatchAcsAssociationService.addBatchAcsID(rpsBatchAcsAssociation);
				rpsPack.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
				try {
					rpsPack = rpsPackService.addRpsPack(rpsPack);
				} catch (Exception e) {
					// save again if LockTimeoutException occurs
					LOGGER.error("ERROR :: Save again, while saving the addRpsPack :: {} ", e);
					rpsPack = rpsPackService.addRpsPack(rpsPack);
				}
			}
			LOGGER.info(
					"Incident Audit log file has been updated in RpsBatchAcsAssociation table where ID is :: {} ",
					rpsBatchAcsAssociation.getBatchAcsId());

		} catch (FileNotFoundException e) {
			LOGGER.error("ERROR :: IncidentAudit Log is not available in that location :: {} ",
					incidentAuditPath + " and :: {} ", e);
		} catch (IOException e) {
			LOGGER.error(
					"ERROR :: while reading the incidentAuditlog file :: {} ",
					e);
		}
		return rpsPack;
	}

	private RpsPack readAndProcessAttendanceReport(File filePath, String event,
			RpsPack rpsPack, Map<String, RpsMasterAssociation> rpsMasterAssociationsMap, String rpackDownloadLocation)
			throws ApolloSecurityException, IOException,
			RpsException, ParseException, RpsRpackException {

		LOGGER.info("---IN-- readAndProcessAttendanceReport()");
		
		String text = attendanceExtractService
				.getAndSaveObjectFromEncryptedJson(filePath);

		Type mapType1 = new TypeToken<AttendanceExportEntity>() {
		}.getType();
		
		
		AttendanceExportEntity attendanceExportEntity = null;
		try {
			attendanceExportEntity = gson.fromJson(text, mapType1);
		} catch (Exception e) {
			LOGGER.error(
					" Exception while processing the Attendance JSON file", e.getMessage());
			throw new RpsException(
					" Exception while processing the Attendance JSON file" + e.getMessage());

		}
		
		List<AttendanceReportEntity> attendanceReportEntityList = attendanceExportEntity.getAttendanceReportEntities();
		//save decrypted text to database
		RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsPack.getRpsBatchAcsAssociation();
		// rpsBatchAcsAssociationService.find(rpsPack.getRpsBatchAcsAssociation().getBatchAcsId());
		Calendar oldGenerationTime = rpsBatchAcsAssociation.getAttendanceGenerationTime();
		Calendar newGenerationTime = attendanceExportEntity.getGenerationTime();
		
		if (oldGenerationTime == null || oldGenerationTime.equals(newGenerationTime)
				|| oldGenerationTime.before(newGenerationTime)) {
			rpsBatchAcsAssociation.setAttendanceDetails(text);
			rpsBatchAcsAssociation.setAttendanceGenerationTime(newGenerationTime);
			rpsBatchAcsAssociation = rpsBatchAcsAssociationService.addBatchAcsID(rpsBatchAcsAssociation);
		}
		

		rpsPack.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
		try {
			rpsPack = rpsPackService.addRpsPack(rpsPack);
		} catch (Exception e) {
			// save again if LockTimeoutException occurs
			LOGGER.error("ERROR :: Save again, while saving the addRpsPack :: {} ", e);
			rpsPack = rpsPackService.addRpsPack(rpsPack);
		}
		LOGGER.info("---OUT-- getObjectFromEncryptedJson()");
		
		if (attendanceReportEntityList == null
				|| attendanceReportEntityList.isEmpty())
			throw new RpsException("Attendance.Json is empty: "
					+ filePath.getAbsolutePath());
		if (oldGenerationTime == null || oldGenerationTime.equals(newGenerationTime)
				|| oldGenerationTime.before(newGenerationTime))
			attendanceExtractService.processAttendanceReport(attendanceReportEntityList,
					rpsPack.getRpsBatchAcsAssociation(), event, rpsMasterAssociationsMap, rpsPack,
					rpackDownloadLocation);
		LOGGER.info("---OUT-- readAndProcessAttendanceReport()");
		return rpsPack;
	}

	private String storeRPackAtArchivePath(RPackExportEntity rPackExportEntity,
			String localDownloadPath, String customer, String division,
			String event) throws IOException {
		String localPath = FilenameUtils.separatorsToSystem(apollo_home_dir);
		File rpackArchivePath = null;

		rpackArchivePath = new File(localPath + File.separator
				+ PackContent.Rpack.toString().toUpperCase() + File.separator
				+ customer + File.separator + division + File.separator + event);
		if (!rpackArchivePath.isDirectory()) {
			rpackArchivePath.mkdirs();
		}
		FileUtils.copyFileToDirectory(new File(localDownloadPath),
				rpackArchivePath.getAbsoluteFile());

		return FilenameUtils.separatorsToSystem(rpackArchivePath
				.getAbsolutePath());
	}

	private RpsPack validateResponsesJson(String filename, String localExtractPath, String localDownloadPath,
			Date downloadTime, RpsPack rpsPack, String customer,
			String division, String event, MessageHeaders messageHeaders, RPackExportEntity rPackExportEntity)
			throws RpsException, IOException,
			JMSRequeException {
		LOGGER.trace("validateResponsesJson --IN-- ");

		if (rPackExportEntity == null) {
			LOGGER.error("Response.json in Rpack is empty");
			throw new RpsException("Response.json in Rpack is empty");
		}
		String rPackArchivePath = this.storeRPackAtArchivePath(
				rPackExportEntity,
				FilenameUtils.separatorsToSystem(localDownloadPath + File.separator + filename), customer, division,
				event);
		String batchCode = rPackExportEntity.getBatchIdent();
		String acsServerId = rPackExportEntity.getAcsServerID();

		RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsPack.getRpsBatchAcsAssociation();

		if (rpsBatchAcsAssociation == null) {
			LOGGER.error("There are no batches and acs combination available in database table-- rpsBatchAcsAssociation for : "
					+ "batchCode :"
					+ batchCode
					+ " and acsServerId"
					+ acsServerId);
			throw new RpsException(
					"There are no batches and acs combination available in database table-- rpsBatchAcsAssociation for : "
							+ "batchCode :"
							+ batchCode
							+ " and acsServerId"
							+ acsServerId);

		}

		rpsPack.setLocalFileDestinationPath(FilenameUtils
				.separatorsToSystem(rPackArchivePath + File.separator
						+ filename));

		rpsPack.setDownloadTime(downloadTime);

		rpsPack.setPackFileDownloadPath(rPackArchivePath);

		rpsPack.setCreationDate(new Date());
		rpsPack.setLastModifiedDate(new Date());

		return rpsPack;

	}

	@Transactional
	private boolean readCandidateAnswers(RPackExportEntity rPackExportEntity,
			RpsBatchAcsAssociation rpsBatchAcsAssociation, Map<String, RpsMasterAssociation> rpsMasterAssociationsMap,
			String customer, String division, String event, String rpackCode, String rPackDownloadPath,
			MessageHeaders messageHeaders)
			throws RpsException,
			JMSRequeException {
		LOGGER.trace("--IN-- readCandidateAnswers");
		List<CandidateAnswersEntity> candAnswers = rPackExportEntity
				.getCandAnswers();
		if (candAnswers == null || candAnswers.isEmpty())
			return false;
		Set<String> assessmentCodes = new HashSet<String>();
		Set<String> questionPapersUnavailble = new HashSet<String>();
		List<RpsWetScoreEvaluation> rpsWetScoreEvaluationList = new ArrayList<>();
		for (CandidateAnswersEntity ca : candAnswers) {
			RpsCumulativeResponses rpsCumulativeResponses = new RpsCumulativeResponses();
			RpsCumulativeResponses tempCumulative = null;
			String eventID = ca.getEventID();
			RpsEvent rpsEvent = rpsEventService.findByEventCode(eventID);
			String assessmentID = ca.getAssessmentID();
			RpsAssessment rpsAssessment= rpsAssessmentService.findByCode(assessmentID);
			String qpCode = ca.getQuestionPaperID();
			Map<String, List<String>> shuffleSequence = ca
					.getQpShuffleSequence();
			Map<String, List<String>> optionShuffleSequence = ca
					.getOptionShuffleSequence();
			if (qpCode == null || qpCode.isEmpty()) {
				LOGGER.error("qpCode is null for one of the Candidate, Incorrect Rpack version is used---");
				throw new RpsException(
						"qpCode is null for one of the Candidate, Incorrect Rpack version is used---");
			}
			RpsQuestionPaper rpsQuestionPaper = null;
			rpsQuestionPaper = rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentID, qpCode);
			if (rpsQuestionPaper == null) {
				// add question paper to DB with status not available
				rpsQuestionPaper = addQpaperToDummyQppack(rpsEvent, qpCode, assessmentID);
				LOGGER.warn("This Question paper is not available, So dummy question paper will be used : "
						+ rpsQuestionPaper.getQpId());
				// handle qp's unavailability
				questionPapersUnavailble.add(qpCode);
				assessmentCodes.add(assessmentID);
			}
			CandidateIdentifiersEntity candidateIdentifiersEntity = ca
					.getCandidateIdentifiers();

			String candidateId1 = candidateIdentifiersEntity.getCandidateId1();
			LOGGER.trace(" candidateId1 = {} ", candidateId1);
			RpsCandidate rpsCandidate =
					rpsCandidateService.findByCandidateIdentifier1AndEventCode(candidateId1, eventID);
			rpsCandidate= createCandidateEntry(candidateId1, rpsEvent, ca.getMifForm(), rpsCandidate);

			if(rpsCandidate==null){
				// this loop never gets executed??
				throw new JMSRequeException("Candidate Info is not available in database for candidateId1 :"+candidateId1);
			}
			
			// get master association for the candidate
			RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationsMap.get(candidateId1);
			
			//if it is OPEN event and rpsMasterAssociation entry is not available save and return rpsMasterAssociation Info from DB
			if (rpsMasterAssociation == null) {
				LOGGER.trace("rpsMasterAssociation = null");
				rpsMasterAssociation =
						createMasterAssociationEntry(rpsCandidate, rpsAssessment, rpsBatchAcsAssociation);
				rpsMasterAssociationsMap.put(candidateId1, rpsMasterAssociation);
			}

			if (rpsMasterAssociation == null) {
				// this loop never gets executed??
				LOGGER.trace(
						"Candidate doesnt have an entry in RpsMasterAssociation table -- candidateId1:"
						+ candidateId1);
				throw new JMSRequeException(
						"Candidate doesnt have an entry in RpsMasterAssociation table :"
								+ candidateId1);
			}

			// feedback format to String =>
			String feedBackResponses = ca.getFeedBackJson();
			rpsMasterAssociation.setCandidateFeedback(feedBackResponses);

			// time taken by candidate String format =>
			String candidateTimeTaken = ca.getCandidateTimeTaken();
			rpsMasterAssociation.setCandidateTimeTaken(candidateTimeTaken);

			// numb of times crashed by candidate =>
			rpsMasterAssociation.setCandidateCrashedCount(ca.getCandidateCrashedCount());

			// save once rpsMasterAssociation
			LOGGER.trace("saving rpsMasterAssociation = {}", rpsMasterAssociation.getLoginID());
			rpsMasterAssociation = rpsMasterAssociationService.addAssociation(rpsMasterAssociation);

			// add MIF json to another table
			if (ca.getMifForm() != null) {
				RpsCandidateMIFDetails rpsCandidateMIFDetails = rpsCandidateMIFDetailsService
						.findByUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());
				if (rpsCandidateMIFDetails == null)
					rpsCandidateMIFDetails = new RpsCandidateMIFDetails(rpsCandidate.getCid(),
						rpsMasterAssociation.getUniqueCandidateId(), ca.getMifForm());
				rpsCandidateMIFDetailsService.addCandidateMif(rpsCandidateMIFDetails);
			}

				// read the candidate responses
				List<CandidateResponseEntity> responses = ca.getResponses();
			RpsCandidateResponse rpsCandidateResponse =
					rpsCandidateResponseService.findByUniqueCandidateId(rpsMasterAssociation.getUniqueCandidateId());

			if (rpsCandidateResponse == null) {
				rpsCandidateResponse = new RpsCandidateResponse();
				if (responses != null)
					rpsCandidateResponse.setResponse(gson.toJson(responses));
				rpsCandidateResponse.setCreationDate(new Date());
				rpsCandidateResponse.setCronStatus(0);
			}

				rpsCandidateResponse
						.setRpsMasterAssociation(rpsMasterAssociation);
				rpsCandidateResponse.setRpsQuestionPaper(rpsQuestionPaper);

				rpsCandidateResponse.setCandidateLanguageSelection(ca.getCandidateLanguageSelection());
				
				Integer questionsAttempted = 0;

				if (responses != null) {

				String newResponseFromAcs = gson.toJson(responses);
				if (ca.getReEnabledCandidate() != null && ca.getReEnabledCandidate()) {
					rpsCandidateResponse.setCronStatus(0);
					rpsCandidateResponse.setResponse(newResponseFromAcs);
				}
					questionsAttempted = responses.size();

					// persist WET type question in rps_wet_score table
					try{
					// avoiding ConstraintViolationException
					List<RpsWetScoreEvaluation> evalScoreListFromDB =
							rpsWetDataEvaluationRepository
									.getByCandidateUniqueId(rpsMasterAssociation.getUniqueCandidateId());
					loop: for (CandidateResponseEntity cResponseEntity : responses) {
							if(cResponseEntity.getQuestionType().equalsIgnoreCase(QuestionType.WRITTEN_ENGLISH_TEST.toString())){

							if (evalScoreListFromDB != null && !evalScoreListFromDB.isEmpty()) {
								for (RpsWetScoreEvaluation rpsWetScoreEvaluationFrmDB : evalScoreListFromDB) {
									if (rpsWetScoreEvaluationFrmDB.getQuestId().equals(cResponseEntity.getQuestionID()))
										continue loop;
								}
							}
								RpsWetScoreEvaluation rpsWetScoreEvaluation = new RpsWetScoreEvaluation();
								rpsWetScoreEvaluation.setRpsMasterAssociation(rpsMasterAssociation);
								rpsWetScoreEvaluation.setQuestId(cResponseEntity.getQuestionID());
								rpsWetScoreEvaluationList.add(rpsWetScoreEvaluation);
							}							
						}
					}catch(Exception e){
						LOGGER.error("--IN RPackExtractService -> readCandidateAnswers -> error in persisting wet question in db "+e.getMessage());
						e.printStackTrace();
					}
				}
				rpsCandidateResponse.setQuestionsAttempted(questionsAttempted);

				rpsCandidateResponse.setShuffleSequence(gson
						.toJson(shuffleSequence));
				rpsCandidateResponse.setOptionShuffleSequence(gson
						.toJson(optionShuffleSequence));
				rpsCandidateResponseService
						.addCandidateResponses(rpsCandidateResponse);

			rpsCumulativeResponses
					.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
			rpsCumulativeResponses.setRpsAssessment(rpsAssessment);
			rpsCumulativeResponses.setRpsQuestionPaper(rpsQuestionPaper);
			rpsCumulativeResponses
					.setIsResultComputed(RpsConstants.RESULT_COMPUTE_STATUS.RESULT_NOT_COMPUTED
							.toString());
			tempCumulative = rpsCumulativeResponsesService
					.getResponseByUniqueIDs(rpsBatchAcsAssociation,
							rpsCumulativeResponses.getRpsAssessment(),
							rpsCumulativeResponses.getRpsQuestionPaper());
			if (tempCumulative == null)
				rpsCumulativeResponsesService
						.saveRpsCmltiveRes(rpsCumulativeResponses);
			else
				LOGGER.warn("An entry is already present for Cumulative response :"
						+ tempCumulative.toString());

		}// close for loop
		
		// handle unavailable packs
		// populating dependency table with qppack
		if (questionPapersUnavailble != null && !questionPapersUnavailble.isEmpty())
			populateFailedQppackInfoEntity(customer, division, event, assessmentCodes, questionPapersUnavailble,
					rpackCode, rPackDownloadPath, messageHeaders);


		// saving rpsWetScoreEvaluationList data in rps_wet_score table
		if(rpsWetScoreEvaluationList.size() > 0){
			rpsWetDataEvaluationService.addRpsWETQuestion(rpsWetScoreEvaluationList);
		}
		return true;
    }

	@Transactional
	private RpsMasterAssociation createMasterAssociationEntry(
			RpsCandidate rpsCandidate, RpsAssessment rpsAssessment,
			RpsBatchAcsAssociation rpsBatchAcsAssociation) {

		RpsMasterAssociation rpsMasterAssociation = new RpsMasterAssociation();
		rpsMasterAssociation.setRpsAssessment(rpsAssessment);
		rpsMasterAssociation.setRpsCandidate(rpsCandidate);
		rpsMasterAssociation.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
		// rpsMasterAssociation= rpsMasterAssociationService.addAssociation(rpsMasterAssociation);
		return rpsMasterAssociation;
	}

	//create candidate entry to database for open event
	private RpsCandidate createCandidateEntry(String candidateId1,
			RpsEvent rpsEvent, String mifJson, RpsCandidate rpsCandidate) {
		
		if(rpsCandidate == null) {
			rpsCandidate=new RpsCandidate();
			rpsCandidate.setCandidateId1(candidateId1);
			rpsCandidate.setRpsEvent(rpsEvent);
		}
		
		if(rpsCandidate.getFirstName()==null || rpsCandidate.getFirstName().isEmpty()){
			rpsCandidate.setFirstName(RpsConstants.NA);
		}
		
		
		//To Retain null values
		Gson gson = new GsonBuilder().serializeNulls().create();
		MIFForm mifForm= gson.fromJson(mifJson, MIFForm.class);
		mifJson=gson.toJson(mifForm);
		
		//update information as received from MIF Form
		if(mifForm!=null){
			PersonalInfo personalInfo = mifForm.getPersonalInfo();
			if(personalInfo!=null){
				rpsCandidate.setFirstName(personalInfo.getFirstName());
				rpsCandidate.setLastName(personalInfo.getLastName());
				rpsCandidate.setMiddleName(personalInfo.getMiddleName());
				rpsCandidate
						.setGender(personalInfo.getGender() == null ? null : personalInfo.getGender().toUpperCase());
				if (personalInfo.getDob() != null) {//1986-Jul-14
					rpsCandidate.setDob(TimeUtil.convertStringToCalender(personalInfo.getDob(), "yyyy-MMM-dd"));
				}
			}
			
			ContactInfo permanentAddress = mifForm.getPermanentAddress();
			if(permanentAddress!=null){
				rpsCandidate.setEmailId1(permanentAddress.getEmailId1());
				rpsCandidate.setPhone1(permanentAddress.getMobile());
				rpsCandidate.setTestCenterCity(permanentAddress.getTestCity());
			}
			// rpsCandidate.setMifFormJson(mifJson);
		}
		rpsCandidate = rpsCandidateService.addCandidate(rpsCandidate);
		return rpsCandidate;
	}

	private RpsQuestionPaper addQpaperToDummyQppack(RpsEvent rpsEvent,
			String qpCode, String assessmentID) {
		// save all question papers to DB with status as not available
		RpsQuestionPaperPack rpsQuestionPaperPack = rpsQuestionPaperPackService
				.findQpPackByEventAndQpId(rpsEvent.getEventCode(),
						RpsConstants.DUMMY_QUESTION_PAPER_PACK_ID_PER_EVENT);
		if (rpsQuestionPaperPack == null) {
			// no dummy paper pack id exists for the event , then create it
			rpsQuestionPaperPack = new RpsQuestionPaperPack();
			rpsQuestionPaperPack.setRpsEvent(rpsEvent);
			rpsQuestionPaperPack
					.setPackId(RpsConstants.DUMMY_QUESTION_PAPER_PACK_ID_PER_EVENT);
			rpsQuestionPaperPack = rpsQuestionPaperPackService
					.save(rpsQuestionPaperPack);
		}
		RpsQuestionPaper qpaper = new RpsQuestionPaper();
		qpaper.setQpCode(qpCode);
		qpaper.setRpsQuestionPaperPack(rpsQuestionPaperPack);
		qpaper.setNumOfSections(0);
		RpsAssessment rpsAssessment = rpsAssessmentService
				.findByCode(assessmentID);
		qpaper.setRpsAssessment(rpsAssessment);
		int setIndex = qpCode.lastIndexOf(setCode);
		String setCode = qpCode.substring(setIndex, qpCode.length());
		qpaper.setSetCode(setCode);
		qpaper.setIsAnswerKeyAvailable(false);
		qpaper.setTotalQuestions(0);
		qpaper.setQpStatus(RpsConstants.QPAPER_STATUS.NOT_AVAILABLE.toString());
		qpaper = rpsQuestionPaperService.save(qpaper);
		return qpaper;
	}

	/*
	 * private RpsMasterAssociation getUniqueIDForCandidate(String assessmentID,
	 * String candidateId1, RpsBatch rpsBatch, RpsAcsServer rpsAcsServer,
	 * RpsEvent rpsEvent) { // TODO Auto-generated method stub RpsCandidate
	 * rpsCandidate = rpsCandidateService.getCandidateInfo(candidateId1,
	 * rpsEvent);
	 * 
	 * RpsMasterAssociation rpsMasterAssociation=
	 * rpsMasterAssociationService.getRpsMasterAssociationPrimaryKey
	 * (rpsCandidate, assessmentID, rpsAcsServer, rpsBatch);
	 * 
	 * return rpsMasterAssociation; }
	 */

	public Object getObjectFromJSON(String fileName, Class className)
			throws IOException {
		Object object = null;
		BufferedReader bufferedReader = null;
		FileReader fileReader = null;
		fileReader = new FileReader(fileName);
		bufferedReader = new BufferedReader(fileReader);
		Gson gson = new GsonBuilder().create();
		object = gson.fromJson(bufferedReader, className);

		if (fileReader != null)
			fileReader.close();
		if (bufferedReader != null)
			bufferedReader.close();
		return object;
	}

	private boolean doDependencyChecks(MessageHeaders messageHeaders, String metadatapath,String fileDownLoadPath) {
		LOGGER.debug("--IN-- doDependencyChecks : filePath " + fileDownLoadPath);
		// inside bpack dependendancy, checking for qp pack as well
		String batchCode = messageHeaders.get(KEY_BATCHCODE).toString();
		String acsServerId = messageHeaders.get(KEY_ACSCODE).toString();
		String versionNumber = messageHeaders.get(KEY_VERSION_NO).toString();
		String packID = messageHeaders.get(KEY_PACK_CODE).toString();
		String customerCode = messageHeaders.get(KEY_CUSTOMERCODE).toString().trim();
		String divisionCode = messageHeaders.get(KEY_DIVISIONCODE).toString().trim();
		String eventCode = messageHeaders.get(KEY_EVENTCODE).toString().trim();

		RpackMetaDataExportEntity rpackMetaDataExportEntity = parseRpsMetaDataJson(metadatapath);
		if (rpackMetaDataExportEntity != null) {
			Set<String> questionPaperCodes = checkQpPackDependency(rpackMetaDataExportEntity);
			if (questionPaperCodes != null) {
				LOGGER.debug("doDependencyChecks : qp pack dependent " + questionPaperCodes);
				// dependency to qp pack
				populateFailedQppackInfoEntity(customerCode, divisionCode, eventCode,
						rpackMetaDataExportEntity.getAssessmentCodes(), questionPaperCodes, packID, fileDownLoadPath,
						messageHeaders);
			}
		}
		LOGGER.debug("doDependencyChecks : b pack dependent batch: " + batchCode + " acs: " + acsServerId);
		// save bpack dependency as well
		populateFailedBpackInfo(packID, versionNumber, eventCode, batchCode, acsServerId, fileDownLoadPath,
				messageHeaders);
		LOGGER.debug("--OUT-- doDependencyChecks : filePath " + fileDownLoadPath);
		return true;
	}

	public boolean populateFailedBpackInfo(String packId, String versionNumber, String eventCode, String batchCode,
			String acsCode, String packDownloadLocation, MessageHeaders messageHeaders) {
		LOGGER.debug(" populateFailedBpackInfo : packId " + packId);
		String messageFormat = rpackDependentMessageBpack + " : " + packId;
		if (emailTriggerEnabled.equalsIgnoreCase(RpsConstants.YES)) {
			boolean status = mailTriggerService.populateMailContentAndTriggerMailWithProxy(messageFormat);
			LOGGER.debug(" mailTriggerService : status " + status);
		} else
			LOGGER.debug(" mailTriggerService : not enabled ");

		BpackRequestEntity bpackRequestEntity =new BpackRequestEntity();
		bpackRequestEntity.setAcsServerCode(acsCode);
		bpackRequestEntity.setBatchCode(batchCode);
		String rpackMessageHeaders = gson.toJson(messageHeaders);
		String dependentPackInput = gson.toJson(bpackRequestEntity);
		RpsPackFailedStatus rpsPackFailedStatus =
				rpsPackFailedStatusService.getfailedPackByPackCode(packId, DependentPackType.BPACK.name());

		if (rpsPackFailedStatus == null) {
			rpsPackFailedStatus = new RpsPackFailedStatus(packId, dependentPackInput,
					DependentPackType.BPACK.name(), packDownloadLocation + File.separator + packId+".zip", PackStatus.YetToProcess.name(), messageFormat,
					new Date(), null, versionNumber, rpackMessageHeaders);
		} else {
			rpsPackFailedStatus.setDependencyStatus(PackStatus.YetToProcess.name());
			rpsPackFailedStatus.setDependentPackInput(dependentPackInput);
			rpsPackFailedStatus.setRpackMessageHeaders(rpackMessageHeaders);
		}
		LOGGER.debug(" addRpsPackFailedStatus : bpack input : " + dependentPackInput);
		rpsPackFailedStatusService.addRpsPackFailedStatus(rpsPackFailedStatus);

		return true;
	}

	private boolean populateFailedQppackInfoEntity(String customer,
			String division, String event, Set<String> assessmentCodes, Set<String> questionPaperUnavailble,
			String rpackCode, String packDownloadLocation, MessageHeaders messageHeaders) {
		LOGGER.debug(" populateFailedQppackInfoEntity : packcode : " + rpackCode);
		String messageFormat = rpackDependentMessageQPpack + " : " + rpackCode;
		if (emailTriggerEnabled.equalsIgnoreCase(RpsConstants.YES)) {
			boolean status = mailTriggerService.populateMailContentAndTriggerMailWithProxy(messageFormat);
			LOGGER.debug(" mailTriggerService : status " + status);
		} else
			LOGGER.debug(" mailTriggerService : not enabled ");
		QppackRequestEntity qppackRequestEntity =
				new QppackRequestEntity(customer, division, event, assessmentCodes);
		List<QptDetailsEntity> qptDetailsEntities = new ArrayList<QptDetailsEntity>();
		for (String questionPaperID : questionPaperUnavailble) {
			QptDetailsEntity qptDetailsEntity = populateQPTInfoEntity(questionPaperID);
			qptDetailsEntities.add(qptDetailsEntity);
		}
		qppackRequestEntity.setQptDetailsEntities(qptDetailsEntities);
		String rpackMessageHeaders = gson.toJson(messageHeaders);
		String dependentPackInput = gson.toJson(qppackRequestEntity);
		RpsPackFailedStatus rpsPackFailedStatus = rpsPackFailedStatusService
				.getfailedPackByPackCode(rpackCode, DependentPackType.QPPACK.name());
		if (rpsPackFailedStatus == null) {
			rpsPackFailedStatus =
					new RpsPackFailedStatus(rpackCode, dependentPackInput,
							DependentPackType.QPPACK.name(), packDownloadLocation + File.separator + rpackCode + ".zip",
							PackStatus.YetToProcess.name(), messageFormat, new Date(),
							null, null, rpackMessageHeaders);
		} else {
			rpsPackFailedStatus.setDependentPackInput(dependentPackInput);
			rpsPackFailedStatus.setRpackMessageHeaders(rpackMessageHeaders);
		}
		LOGGER.debug(" addRpsPackFailedStatus : qppack input : " + dependentPackInput);
		rpsPackFailedStatusService.addRpsPackFailedStatus(rpsPackFailedStatus);
		return true;
	}

	private QptDetailsEntity populateQPTInfoEntity(String questionPaperID) {

		// Sample QPCode = QuestionPaper_A1731_Qpt_5645_Group3_Version_1_set_3

		String splitQp = questionPaperID;
		String s[] = splitQp.split("_");
		Integer qptId = null;
		Integer qpGroup = null;
		Integer version = null;
		try {
			qptId = Integer.parseInt(s[3]);
			qpGroup = Integer.parseInt(s[4].substring(5));
			version = Integer.parseInt(s[6]);
		} catch (Exception e) {
			LOGGER.error("Exception while parsing Qp COde = " + questionPaperID);
		}
		return (new QptDetailsEntity(qptId, qpGroup, version));

	}

	private RpsPack getRPackHeader(final MessageHeaders messageHeaders, RpsBatchAcsAssociation rpsBatchAcsAssociation)
			throws RPSAuthenticationException, PackAlreadyProcessedException {

		RpsPack rpsPack = null;
		if (messageHeaders != null) {
			final String versionNumber = messageHeaders.get(KEY_VERSION_NO).toString();
			final String packID = messageHeaders.get(KEY_PACK_CODE).toString();
			final String deltaPackFlag = messageHeaders.get(KEY_DELTA_PACK).toString();
			final String packType = messageHeaders.get(KEY_PACK_TYPE).toString();
			final String packSubType = messageHeaders.get(KEY_PACK_SUB_TYPE).toString();

			rpsPack = rpsPackService.getRpsPackDetails(rpsBatchAcsAssociation, packID, packType, versionNumber);
			if (rpsPack == null) {
				rpsPack = new RpsPack();
				rpsPack.setCreationDate(Calendar.getInstance().getTime());
			} else {
				if (rpsPack != null && rpsPack.getPackStatus() != null
						&& (rpsPack.getPackStatus().equals(RpsConstants.packStatus.UNPACKED)
								|| rpsPack.getPackStatus().equals(RpsConstants.packStatus.PACKS_RECEIVED))) {
					LOGGER.info("RPack already available in database. Received with the header {}.", messageHeaders);
					throw new PackAlreadyProcessedException(
							"RPack already available in database. Received with the header {}.");
				}
			}

			rpsPack.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
			rpsPack.setVersionNumber(versionNumber);
			rpsPack.setPackId(packID);
			rpsPack.setIsDeltaPackExists(deltaPackFlag.equalsIgnoreCase(DEFAULT_DELTA_PACK_INFO) ? true : false);
			rpsPack.setPackType(packType.toUpperCase());
			rpsPack.setPackSubType(packSubType.toUpperCase());

			LOGGER.info("RPack version number: {}, packID : {}, Delta Rpack : {}, packSubType : {}", versionNumber,
					packID, deltaPackFlag, packSubType);
		}

		return rpsPack;
	}

	// private String requestTokenforSecureAuthorization() {
	// try {
	// // Invoke the REST api.
	// String response = HttpClientFactory.getInstance().requestPostWithJson(tokenRequestURI, tokenRequestMethod,
	// secureAutorizationDetails);
	// tokenUpdate = response;
	// } catch (HttpClientException e) {
	// LOGGER.error("HttpClientException with Secure API Autorization" + e.getMessage());
	// } catch (Exception e) {
	// LOGGER.error("handleQppackDependency() failed ", e.getMessage());
	// }
	// return tokenUpdate;
	// }
	//
	// public String getTokenforSecureAuthorization(boolean isRequestForNewToken) {
	// if (tokenUpdate == null || isRequestForNewToken)
	// return requestTokenforSecureAuthorization();
	// else
	// return tokenUpdate;
	// }
}