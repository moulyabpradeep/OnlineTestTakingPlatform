/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 24-Oct-2013 2:47:16 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSFilePaths;
import com.merittrac.apollo.acs.constants.InvigilatorReportsType;
import com.merittrac.apollo.acs.constants.ReportStatusEnum;
import com.merittrac.apollo.acs.dataobject.CustomerEventDivisionDO;
import com.merittrac.apollo.acs.dataobject.InvigilatorReportViewDo;
import com.merittrac.apollo.acs.dataobject.MalPracticeReportCreateDO;
import com.merittrac.apollo.acs.dataobject.MalpracticeReportDO;
import com.merittrac.apollo.acs.dataobject.PackExportInfoDO;
import com.merittrac.apollo.acs.dataobject.StatusDO;
import com.merittrac.apollo.acs.entities.AcsReportDetails;
import com.merittrac.apollo.acs.exception.ReportException;
import com.merittrac.apollo.acs.utility.ACSCommonUtility;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.entities.acs.MalpracticeReportMetadata;
import com.merittrac.apollo.common.entities.acs.ReportTypeEnum;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * Service to handle the Business Logic of Reports
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class ReportsService {
	private static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	static CDEAService cdeaService = null;
	static PackDetailsService packDetailsService = null;
	static BatchService batchService = null;

	static {
		cdeaService = new CDEAService();
		packDetailsService = new PackDetailsService();
		batchService = BatchService.getInstance();
	}

	/**
	 * 
	 */
	public ReportsService() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	/**
	 * This Service fetches the Reports (Invigilators) generated in the provided date range
	 * 
	 * @param startRange
	 * @param endRange
	 * @return
	 * @throws ReportException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<InvigilatorReportViewDo> fetchInvigilatorReportsInfo(Calendar startRange, Calendar endRange,
			String eventCode) throws ReportException {
		getLogger().debug("fetchInvigilatorReportsInfo for startRange:{} endRange:{}", startRange, endRange);
		List<InvigilatorReportViewDo> invigilatorReportViewDos = new ArrayList<>();
		try {
			List<AcsReportDetails> reportDetailsTOs = cdeaService.getReportDetailsByDateRange(startRange, endRange);
			if (reportDetailsTOs != null) {
				// throw new ReportException("No Reports exists for the selected range");

				for (AcsReportDetails reportDetailsTO : reportDetailsTOs) {
					InvigilatorReportsType invigilatorReportsType = null;
					try {
						invigilatorReportsType =
								InvigilatorReportsType.valueOf(reportDetailsTO.getReportType().toString());
					} catch (IllegalArgumentException exception) {
						continue;
					}

					String batchCode =
							batchService.getBatchCodeForReportOnEventCode(reportDetailsTO.getReportIdentifier(),
									eventCode);
					if (batchCode == null)
						continue;
					CustomerEventDivisionDO customerEventDivisionDO =
					// TODO:Lazy loading - Done
							cdeaService.getEventDivisionCusDetailsbyBatchCode(batchCode);
					// TODO:Lazy loading - Done
					InvigilatorReportViewDo invigilatorReportViewDo =
							new InvigilatorReportViewDo(reportDetailsTO.getReportIdentifier(), invigilatorReportsType,
									customerEventDivisionDO.getEventName(), batchCode,
									customerEventDivisionDO.getBatchName(), reportDetailsTO.getGeneratedStartTime(),
									reportDetailsTO.getReportStatus(), reportDetailsTO.getCreator());

					// get malpractice report content
					invigilatorReportViewDo.setReportContent(getMalpracticeReportContent(reportDetailsTO
							.getReportMetaData()));

					invigilatorReportViewDos.add(invigilatorReportViewDo);
				}
			}

			List<PackExportInfoDO> packExportInfoDOList =
					cdeaService.getRPackManaullyGeneratedtDetails(startRange, endRange, eventCode);
			if (packExportInfoDOList != null) {
				for (PackExportInfoDO packExportInfoDO : packExportInfoDOList) {
					ReportStatusEnum reportStatusEnum = null;
					try {
						reportStatusEnum = ReportStatusEnum.valueOf(packExportInfoDO.getPackStatus().toString());
					} catch (IllegalArgumentException exception) {
						continue;
					}
					CustomerEventDivisionDO customerEventDivisionDO =
							cdeaService.getEventDivisionCusDetailsbyBatchCode(packExportInfoDO.getBatchCode());
					InvigilatorReportViewDo invigilatorReportViewDo = new InvigilatorReportViewDo();
					invigilatorReportViewDo.setBatchCode(packExportInfoDO.getBatchCode());
					invigilatorReportViewDo.setInvigilatorReportsType(InvigilatorReportsType.RPACK_MANUALLY_GENERATED);
					invigilatorReportViewDo.setReportIdentifier(packExportInfoDO.getPackIdentifier());
					invigilatorReportViewDo.setGenerationTime(packExportInfoDO.getGenaratedTime());
					invigilatorReportViewDo.setReportStatus(reportStatusEnum);
					invigilatorReportViewDo.setEventName(customerEventDivisionDO.getEventName());
					invigilatorReportViewDo.setBatchName(customerEventDivisionDO.getBatchName());
					invigilatorReportViewDos.add(invigilatorReportViewDo);

				}
			}

		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing fetchInvigilatorReportsInfo...", e);
		}
		getLogger().debug("fetchInvigilatorReportsInfo is returning {}", invigilatorReportViewDos);
		return invigilatorReportViewDos;

	}

	/**
	 * This APi creates a malpractice report. This can be used for both drafting and submitting (logging) the report.
	 * Malpractice report is created with the provided identifier
	 * 
	 * @param malPracticeReportCreateDO
	 * @param user
	 * @param ipAddress
	 * @return {@link StatusDO}
	 * @since Apollo v2.0
	 * @see
	 */
	public StatusDO createMalpracticeReport(MalPracticeReportCreateDO malPracticeReportCreateDO, String user,
			String ipAddress) {
		getLogger().debug("createMalpracticeReport for {}", malPracticeReportCreateDO);
		StatusDO statusDO = new StatusDO();
		try {
			malPracticeReportCreateDO.setMalPracticeReportIdentifier(getMalpracticeIdentifier());
			// audit here
			auditIt(malPracticeReportCreateDO, user, ipAddress);
			// ends audit
			// Validate the DO
			CustomerEventDivisionDO customerEventDivisionDO = validateMalpracticeCreateDO(malPracticeReportCreateDO);

			// Get the identifier

			malPracticeReportCreateDO.getBatchCodesDO().setBatchCode(customerEventDivisionDO.getBatchCode());
			File malpracticeReportLocation =
					getMalPracticeReportLocation(customerEventDivisionDO.getBatchCode(),
							malPracticeReportCreateDO.getMalPracticeReportIdentifier());

			// process and copy the resources of the report
			processResources(malPracticeReportCreateDO, malpracticeReportLocation);

			// encrypt and write the content of the report to a file
			WriteContent(malPracticeReportCreateDO, malpracticeReportLocation);

			// TODO: Move to utility
			String metadata = makeMetadata(malPracticeReportCreateDO, customerEventDivisionDO);
			writeMetadata(metadata, malpracticeReportLocation, malPracticeReportCreateDO.getBatchCodesDO()
					.getBatchCode());
			updateDatabase(malPracticeReportCreateDO, user, metadata);
			// TODO: Audit Logg
			getLogger().debug("createMalpracticeReport returning true");
			statusDO.setIdentifier(malPracticeReportCreateDO.getMalPracticeReportIdentifier());
			statusDO.setStatus(true);
			statusDO.setMessage("Malpractice report created");
		} catch (ReportException | GenericDataModelException e) {
			getLogger().debug("Error! Create Malpractice report failed because of {}", e.getCause());
			getLogger().error("createMalpracticeReport failed  ", e);
			statusDO.setStatus(false);
			statusDO.setMessage(e.getMessage());
		} finally {
			getLogger().debug("createMalpracticeReport is returning {}", statusDO);
		}
		return statusDO;

	}

	/**
	 * @param metadata
	 * @param malpracticeReportLocation
	 * @throws ReportException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void writeMetadata(String metadata, File malpracticeReportLocation, String batchCode)
			throws ReportException {
		try {
			String textToBeWritten = metadata;
			if (ACSCommonUtility.isEncryptionRequired()) {
				getLogger().debug("Encrypting the content...");
				textToBeWritten = encrypt(batchCode, textToBeWritten);
			}
			ACSCommonUtility.writeIntoAFile(malpracticeReportLocation,
					ACSConstants.MALPRACTICE_REPORT_META_DATA_FILE_NAME, textToBeWritten);
		} catch (IOException | ApolloSecurityException e) {
			getLogger().error("Exception while executing writeMetadata...", e);
			throw new ReportException("Error in writing the metadata", e);
		}

	}

	/**
	 * @param malPracticeReportCreateDO
	 * @param customerEventDivisionDO
	 *            TODO
	 * @return
	 * @throws ReportException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String makeMetadata(MalPracticeReportCreateDO malPracticeReportCreateDO,
			CustomerEventDivisionDO customerEventDivisionDO) throws ReportException {
		getLogger().debug("Making the malpractice Report metadata");
		MalpracticeReportMetadata malpracticeReportMetadata = new MalpracticeReportMetadata();

		malpracticeReportMetadata.setBatchCode(malPracticeReportCreateDO.getBatchCodesDO().getBatchCode());
		try {
			malpracticeReportMetadata.setAssessmentServerCode(cdeaService
					.getACSCodeByBatchCode(malPracticeReportCreateDO.getBatchCodesDO().getBatchCode()));
		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing makeMetadata...", e);
			throw new ReportException("could not fetch ACS code", e);
		}
		malpracticeReportMetadata.setCustomerCode(customerEventDivisionDO.getCustomerCode());
		malpracticeReportMetadata.setDivisionCode(customerEventDivisionDO.getDivisionCode());
		malpracticeReportMetadata.setEventCode(customerEventDivisionDO.getEventCode());

		malpracticeReportMetadata.setMalpracticeReportIdentifier(malPracticeReportCreateDO
				.getMalPracticeReportIdentifier());
		malpracticeReportMetadata.setContentFileName(getContentFileName(malPracticeReportCreateDO));

		if (malPracticeReportCreateDO.getMalpracticeResourcePaths() == null
				|| malPracticeReportCreateDO.getMalpracticeResourcePaths().isEmpty()) {
			malpracticeReportMetadata.setResources(null);
		} else {
			List<String> resources = new ArrayList<>();
			for (String resourceFile : malPracticeReportCreateDO.getMalpracticeResourcePaths()) {
				resources.add(new File(resourceFile).getName());
			}
			malpracticeReportMetadata.setResources(resources);
		}
		malpracticeReportMetadata.setGenerationTime(Calendar.getInstance());
		// added for showing the content of malpractice report in UI
		MalpracticeReportDO malpracticeReportDO =
				new MalpracticeReportDO(malpracticeReportMetadata,
						malPracticeReportCreateDO.getMalpracticeReportContent());

		return new Gson().toJson(malpracticeReportDO);
	}

	/**
	 * Generates the Malpractice report identifier
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getMalpracticeIdentifier() {
		String malpracticeIdentifier =
				ACSConstants.MALPRACTICE_REPORT_IDENTIFIER_PREFIX + ACSCommonUtility.getPresentTimeStampTillSecond();
		getLogger().debug("Malpractice Identifier created = {}", malpracticeIdentifier);
		return malpracticeIdentifier;
	}

	/**
	 * @param malPracticeReportCreateDO
	 * @param creator
	 * @param metadata
	 *            TODO
	 * @throws ReportException
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void updateDatabase(MalPracticeReportCreateDO malPracticeReportCreateDO, String creator, String metadata)
			throws ReportException, GenericDataModelException {
		getLogger().debug("Updating the database");
		AcsReportDetails reportDetailsTO = new AcsReportDetails();
		reportDetailsTO.setAcsbatchdetails(batchService.getBatchDetailsByBatchCode(malPracticeReportCreateDO
				.getBatchCodesDO().getBatchCode()));
		String eventCode =
				batchService.getEventDetailsByBatchCode(malPracticeReportCreateDO.getBatchCodesDO().getBatchCode())
						.getEventCode();
		reportDetailsTO.setEventCode(eventCode);
		reportDetailsTO.setCreator(creator);
		reportDetailsTO.setGeneratedEndTime(Calendar.getInstance());
		reportDetailsTO.setGeneratedStartTime(Calendar.getInstance());
		reportDetailsTO.setReportIdentifier(malPracticeReportCreateDO.getMalPracticeReportIdentifier());
		reportDetailsTO.setReportStatus(malPracticeReportCreateDO.getReportStatusEnum());
		reportDetailsTO.setReportType(ReportTypeEnum.MALPRACTICE_REPORT);
		reportDetailsTO.setReportMetaData(metadata);
		try {
			cdeaService.saveReportDetails(reportDetailsTO);
		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing updateDatabase...", e);
			throw new ReportException(e);
		}
	}

	/**
	 * @param malPracticeReportCreateDO
	 * @param malpracticeReportLocation
	 *            TODO
	 * @throws ReportException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void WriteContent(MalPracticeReportCreateDO malPracticeReportCreateDO, File malpracticeReportLocation)
			throws ReportException {
		getLogger().debug("Writing the content");
		try {
			String textToBeWritten = malPracticeReportCreateDO.getMalpracticeReportContent();
			if (ACSCommonUtility.isEncryptionRequired()) {
				getLogger().debug("Encrypting the content...");
				textToBeWritten = encrypt(malPracticeReportCreateDO.getBatchCodesDO().getBatchCode(), textToBeWritten);
			}
			ACSCommonUtility.writeIntoAFile(malpracticeReportLocation, getContentFileName(malPracticeReportCreateDO),
					textToBeWritten);
		} catch (IOException | ApolloSecurityException e) {
			getLogger().error("Exception while executing WriteContent...", e);
			throw new ReportException("File Writing Failed!", e);
		}

	}

	/**
	 * @param batchCode
	 * @param textToBeWritten
	 * @return
	 * @throws ApolloSecurityException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String encrypt(String salt, String content) throws ApolloSecurityException {
		int keySize = ACSCommonUtility.getEncryptionKeySize();
		CryptUtil cryptUtil = new CryptUtil(keySize);
		return cryptUtil.encryptTextUsingAES(content, salt);
	}

	/**
	 * @param malPracticeReportCreateDO
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getContentFileName(MalPracticeReportCreateDO malPracticeReportCreateDO) {
		return malPracticeReportCreateDO.getMalPracticeReportIdentifier() + ACSConstants.FILE_HTML_EXT;
	}

	/**
	 * Move the images to Malpractice report location
	 * 
	 * @param malPracticeReportCreateDO
	 * @param malpracticeReportLocation
	 *            TODO
	 * @throws ReportException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void processResources(MalPracticeReportCreateDO malPracticeReportCreateDO, File malpracticeReportLocation)
			throws ReportException {
		getLogger().debug("Processing Resources.. ");
		if (malPracticeReportCreateDO.getMalpracticeResourcePaths() == null
				|| malPracticeReportCreateDO.getMalpracticeResourcePaths().isEmpty()) {
			getLogger().debug("No resource exists");
			return;
		}
		List<String> resources = malPracticeReportCreateDO.getMalpracticeResourcePaths();
		for (String resource : resources) {
			getLogger().debug("Copying the resource file {} to {}", resource,
					malpracticeReportLocation.getAbsolutePath());
			try {
				FileUtils.copyFileToDirectory(new File(resource), malpracticeReportLocation);
			} catch (IOException e) {
				getLogger().error("IOException while executing processResources...", e);
				throw new ReportException("Could not create the Report", e);
			}
		}

	}

	/**
	 * ACS Content Directory + MalpRactice+ Reports + batchCode+malpractice Identifier
	 * 
	 * @param malpracticeReportIdentifier
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public File getMalPracticeReportLocation(String batchCode, String malpracticeReportIdentifier) {
		File AcsContentMalpracticeDirectory =
				new File(ACSFilePaths.getACSMalpracticeDirectory() + File.separator + ACSConstants.REPORTS_DIRECTORY
						+ File.separator + batchCode + File.separator + ACSConstants.MALPRACTICE_REPORT + "_"
						+ malpracticeReportIdentifier);
		if (!AcsContentMalpracticeDirectory.exists()) {
			AcsContentMalpracticeDirectory.mkdirs();
		}
		return AcsContentMalpracticeDirectory;
	}

	/**
	 * ACS Content Directory + MalpRactice+ Packs
	 * 
	 * @param packIdentifier
	 * 
	 * @param malpracticeReportIdentifier
	 * @param batchCode
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public File getMalPracticePackLocation(String packIdentifier) {
		File AcsContentMalpracticeDirectory =
				new File(ACSFilePaths.getACSMalpracticeDirectory() + File.separator + ACSConstants.PACKS_DIRECTORY
						+ File.separator + packIdentifier);
		if (!AcsContentMalpracticeDirectory.exists()) {
			AcsContentMalpracticeDirectory.mkdirs();
		}
		return AcsContentMalpracticeDirectory;
	}

	/**
	 * ACS Content Directory + ReportPack+ ReportPack_XX
	 * 
	 * @param packIdentifier
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public File getReportPackLocation(String packIdentifier) {
		File acsReportPackDirectory = new File(ACSFilePaths.getAcsReportPackDir() + File.separator + packIdentifier);
		if (!acsReportPackDirectory.exists()) {
			acsReportPackDirectory.mkdirs();
		}
		return acsReportPackDirectory;
	}

	/**
	 * Validate the DO for null check and mandatory columns like BatchDetails and content etc and provides the
	 * {@link CustomerEventDivisionDO}
	 * 
	 * @param malPracticeReportCreateDO
	 * @return
	 * @throws ReportException
	 * @throws
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private CustomerEventDivisionDO validateMalpracticeCreateDO(MalPracticeReportCreateDO malPracticeReportCreateDO)
			throws ReportException {
		getLogger().debug("Validating..");
		CustomerEventDivisionDO customerEventDivisionDO = null;
		try {
			if (malPracticeReportCreateDO == null)
				throw new ReportException("Invalid MalPracticeReportCreateDO");
			if (malPracticeReportCreateDO.getMalpracticeReportContent() == null
					|| malPracticeReportCreateDO.getMalpracticeReportContent().isEmpty())
				throw new ReportException("No MalpracticeReportContent!");
			if (malPracticeReportCreateDO.getBatchCodesDO() == null
					|| malPracticeReportCreateDO.getBatchCodesDO().getBatchCode() == null)
				throw new ReportException("No Valid Batch Info()!");
			if (malPracticeReportCreateDO.getReportStatusEnum() == null)
				throw new ReportException("Invalid report status");

			customerEventDivisionDO =
					cdeaService.getEventDivisionCusDetailsbyBatchID(malPracticeReportCreateDO.getBatchCodesDO()
							.getBatchCode());
			if (customerEventDivisionDO == null)
				throw new ReportException("Invalid Batch ID");

		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing validateMalpracticeCreateDO...", e);
			throw new ReportException("Exception while validating the DO", e);
		} finally {
			getLogger().debug("Validation returning {}", customerEventDivisionDO);
		}
		return customerEventDivisionDO;
	}

	/**
	 * @param packIdentifier
	 * @param metadata
	 *            TODO
	 * @param reportType
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveReportPackInfo(String packIdentifier, String metadata, ReportTypeEnum reportType,
			boolean isForcedGeneration, String batchCode) throws GenericDataModelException {
		AcsReportDetails reportDetailsTO = new AcsReportDetails();
		reportDetailsTO.setReportIdentifier(packIdentifier);
		if (!isForcedGeneration) {
			reportDetailsTO.setCreator("Report Pack Job");
			reportDetailsTO.setReportStatus(ReportStatusEnum.GENERATED);
		} else {
			reportDetailsTO.setReportStatus(ReportStatusEnum.MANUALLY_GENERATED);
			reportDetailsTO.setCreator("User");
		}

		reportDetailsTO.setReportType(reportType);
		reportDetailsTO.setGeneratedStartTime(Calendar.getInstance());
		reportDetailsTO.setGeneratedEndTime(Calendar.getInstance());
		reportDetailsTO.setReportMetaData(metadata);
		if (batchCode != null)
			reportDetailsTO.setAcsbatchdetails(BatchService.getInstance().loadBatch(batchCode));
		else
			reportDetailsTO.setAcsbatchdetails(null);
		packDetailsService.saveReportDetails(reportDetailsTO);
	}

	/**
	 * auditIt API used to audit the log report screen
	 * 
	 * @param packIdentifiers
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditIt(MalPracticeReportCreateDO malPracticeReportCreateDO, String user, String ipAddress) {
		// added for audit trail
		String isAuditEnable = AutoConfigure.getAuditConfigureValue();
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			Object[] paramArray =
					{ user, ReportTypeEnum.MALPRACTICE_REPORT,
							malPracticeReportCreateDO.getMalPracticeReportIdentifier(),
							malPracticeReportCreateDO.getBatchCodesDO().getBatchName() };
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.REPORT_LOGGED.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, user);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_LOG_REPORT_MSG, paramArray);
		}
		// end for audit trail
	}

	/**
	 * Gets the malpractice report content
	 * 
	 * @param malpracticeReportMetaData
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getMalpracticeReportContent(String malpracticeReportMetaData) {
		logger.info("initiated getMalpracticeReportContent where malpracticeReportMetaData={}",
				malpracticeReportMetaData);

		String reportContent = null;
		if (malpracticeReportMetaData != null) {
			// convert json to object
			MalpracticeReportDO malpracticeReportDO =
					new Gson().fromJson(malpracticeReportMetaData, MalpracticeReportDO.class);
			reportContent = malpracticeReportDO.getMalpracticeContent();
		}

		logger.info("returning reportContent = {}", reportContent);
		return reportContent;
	}
}
