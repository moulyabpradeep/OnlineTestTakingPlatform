package com.merittrac.apollo.rps.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.SecuredZipUtil;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.entities.acs.MalpracticePackMetadata;
import com.merittrac.apollo.common.entities.acs.MalpracticeReportMetadata;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.entity.RpsRpackComponent;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.data.service.RpsRpackComponentService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.utility.RpsGeneralUtility;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class MalpracticeExtractService {

	@Autowired
	RpsGeneralUtility rpsGeneralUtility;

	@Autowired
	RpsBatchService rpsBatchService;

	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	@Autowired
	RpsRpackComponentService rpsRpackComponentService;

	@Autowired
	RpsPackService rpsPackService;
	
	@Autowired
	RpsAcsServerServices rpsAcsServerServices;

	@Value("${isMalpracticePasswordProtected}")
	private String isMalpracticePasswordProtected;

	private static final Logger logger = LoggerFactory.getLogger(MalpracticeExtractService.class);
	private static final String malpracticePreContentName = "MalpracticeReport_";

	public MalpracticeExtractService() throws IOException {
		super();
	}

	/**
	 * 
	 * @param malpracticeFilePath
	 * @param rpsAcsServer
	 * @throws ZipException
	 * @throws IOException
	 * @throws RpsException
	 * @throws ApolloSecurityException
	 */
	@Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
	public void extractMalpracticeReport(String malpracticeFilePath, RpsAcsServer rpsAcsServer) throws ZipException,
			IOException, RpsException, ApolloSecurityException {
		logger.info("-----IN extractMalpracticeReport method----");
		logger.info("Is Malpractice Pack password protected :: {}", isMalpracticePasswordProtected);
		String folderName = rpsGeneralUtility.getFolderName(malpracticeFilePath);

		int lastIndexFS = this.getlastIndexOfFileSeperator(malpracticeFilePath);

		String destMalpracticeExtractPath = malpracticeFilePath.substring(0, lastIndexFS) + File.separator + folderName;

		try {
			ZipFile zipFile = new ZipFile(new File(malpracticeFilePath));
			if (zipFile.isValidZipFile()) {
				if (isMalpracticePasswordProtected.equalsIgnoreCase("yes")) {
					SecuredZipUtil.extractArchiveWithPassword(malpracticeFilePath, destMalpracticeExtractPath,
							rpsAcsServer.getAcsServerId());
					logger.info("Malpractice has extracted successfully----");
				} else {
					ZipUtility.extractAll(malpracticeFilePath, destMalpracticeExtractPath);
					logger.info("Malpractice has extracted successfully----");
				}
			} else
				throw new ZipException("Malpractice pact is not valid");

			File malpracticeFile = new File(destMalpracticeExtractPath);
			if (malpracticeFile.list().length == 1) {
				destMalpracticeExtractPath = destMalpracticeExtractPath + File.separator + malpracticeFile.list()[0];
				malpracticeFile = new File(destMalpracticeExtractPath);
			}
			String reportJsonFileName = "";
			for (String malpracticeContent : malpracticeFile.list()) {
				if (malpracticeContent.contains(".json")) {
					reportJsonFileName = malpracticeContent;
					break;
				}
			}

			String reportJsonFilePath = destMalpracticeExtractPath + File.separator + reportJsonFileName;
			logger.info("-----OUT extractMalpracticeReport method----");
			this.readAndUpdateMalpracticeReport(reportJsonFilePath);//, rpsAcsServer
		} finally {
			String path = "";
			if (malpracticeFilePath.contains("."))
				path = malpracticeFilePath.substring(0, malpracticeFilePath.lastIndexOf("."));
			else
				path = malpracticeFilePath;
			if (new File(path).isDirectory() || new File(path).isDirectory())
				FileUtils.deleteDirectory(new File(path));
		}

	}

	/**
	 * 
	 * @param reportJsonFilePath
	 * @param rpsAcsServer
	 * @throws IOException
	 * @throws RpsException
	 * @throws ApolloSecurityException
	 */
	private void readAndUpdateMalpracticeReport(String reportJsonFilePath)//, RpsAcsServer rpsAcsServer
			throws IOException, RpsException, ApolloSecurityException {
		logger.info("----IN readAndUpdateMalpracticeReport()-----");
		MalpracticePackMetadata malpracticePackMetadata =
				(MalpracticePackMetadata) rpsGeneralUtility.getObjectFromJson(reportJsonFilePath,
						MalpracticePackMetadata.class);
		Map<String, MalpracticeReportMetadata> mapOfreportIdentifierToMetadata =
				malpracticePackMetadata.getMapOfIdentifierToMetadata();
		Set<String> metadataKey = mapOfreportIdentifierToMetadata.keySet();

		List<RpsRpackComponent> rpsRpackComponentList = new ArrayList<RpsRpackComponent>();

		int lastIndexOfFS = this.getlastIndexOfFileSeperator(reportJsonFilePath);
		String malpracticeExtdPath = reportJsonFilePath.substring(0, lastIndexOfFS);
		for (String folderName : metadataKey) {
			malpracticeExtdPath = malpracticeExtdPath + File.separator + malpracticePreContentName + folderName;
			MalpracticeReportMetadata mReportMetadata = mapOfreportIdentifierToMetadata.get(folderName);
			String batchCode = mReportMetadata.getBatchCode();
			logger.info("BatchCode for malpractice content File :: {} ", folderName + " is ", batchCode);
			String eventCode = mReportMetadata.getEventCode();
			String acsCode=mReportMetadata.getAssessmentServerCode();
			RpsAcsServer rpsAcsServer = rpsAcsServerServices.findByAcsServerId(acsCode);
			logger.info("EventCode for malpractice content File :: {} ", folderName + " is ", eventCode);
			RpsBatch rpsBatch = rpsBatchService.findByBatchCode(batchCode);
			if (rpsBatch == null) {
				logger.info("Batch is not Available, BatchCode :: {} ", batchCode);
				throw new RpsException("Batch is not Available");
			}
			RpsBatchAcsAssociation rpsBatchAcsAssociation =
					rpsBatchAcsAssociationService.getAssociationByRpsBatchAndRpsAcsServer(rpsBatch, rpsAcsServer);
			if (rpsBatchAcsAssociation == null) {
				logger.info("in RpsBatchAcsAssociation batchCode :: {} ", batchCode + " and AcsCode :: {} ",
						rpsAcsServer.getAcsServerId() + " is not available");
				throw new RpsException("in RpsBatchAcsAssociation batchCode :: {} " + batchCode + " and AcsCode :: {} "
						+ rpsAcsServer.getAcsServerId() + " is not available");
			}
			CryptUtil cryptUtil = new CryptUtil(256);
			String malpracticeEncContentPath = null;
			File file = new File(malpracticeExtdPath);
			for (String mpContent : file.list()) {
				if (mpContent.contains(".html")) {
					malpracticeEncContentPath = malpracticeExtdPath + File.separator + "Dec_" + mpContent;
					String enc_malPracticeDetails =
							rpsGeneralUtility.readFile(malpracticeExtdPath + File.separator + mpContent).trim();
					String dec_malPracticeDetails = cryptUtil.decryptTextUsingAES(enc_malPracticeDetails, batchCode);
					logger.info("Malpractice Details :: {} ", dec_malPracticeDetails);
					File dec_malpracticeFile = new File(malpracticeEncContentPath);

					if (!dec_malpracticeFile.exists())
						dec_malpracticeFile.createNewFile();

					FileWriter fileWriter = new FileWriter(dec_malpracticeFile.getAbsoluteFile());
					BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);

					bufferedWriter.write(dec_malPracticeDetails);
					bufferedWriter.close();

					break;
				}
			}
			String malpracticeDetails = "";
			if (malpracticeEncContentPath != null) {
				malpracticeDetails = rpsGeneralUtility.readFile(malpracticeEncContentPath);
			}

			/*
			 * //updating Malpractice details in tp rpsBatchAcsAssociation table.
			 * rpsBatchAcsAssociation.setIncidentAuditDetails(malpracticeDetails);
			 * rpsBatchAcsAssociationService.addBatchAcsID(rpsBatchAcsAssociation);
			 */
			// setting RPack details ...
			RpsRpackComponent rpsRpackComponent = new RpsRpackComponent();

			RpsPack rpsPack = new RpsPack();
			rpsPack.setCreationDate(new Date());
			rpsPack.setPackId(folderName);
			rpsPack.setPackReceivingMode(RpsConstants.packReceiveMode.JMS_UPLOAD.toString());
			rpsPack.setPackStatus(RpsConstants.packStatus.UNPACKED.toString());
			rpsPack.setPackType(RpsConstants.PackType.MALPRACTICE_PACK.toString());
			rpsPack.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
			rpsPack.setLastModifiedDate(new Date());
			rpsPack.setVersionNumber("1");

			RpsPack rPack =
					rpsPackService.getRpsPack(rpsBatchAcsAssociation, rpsPack.getPackId(), rpsPack.getPackType(),
							rpsPack.getVersionNumber());
			// updating rpsPack into database..
			RpsRpackComponent rpackComponent = null;
			if (rPack == null) {
				rpsPack = rpsPackService.addRpsPack(rpsPack);
				rpsRpackComponent.setRpsPack(rpsPack);
				rpackComponent =
						rpsRpackComponentService.getComponentByPackIdAndTypeAndStatus(rpsPack.getPid(),
								RpsConstants.RpackComponents.MALPRACTICE_LOG.toString(),
								RpsConstants.packStatus.PACKS_RECEIVED.toString());
			} else {
				rpackComponent =
						rpsRpackComponentService.getComponentByPackIdAndTypeAndStatus(rPack.getPid(),
								RpsConstants.RpackComponents.MALPRACTICE_LOG.toString(),
								RpsConstants.packStatus.PACKS_RECEIVED.toString());
				rpsRpackComponent.setRpsPack(rPack);
			}
			if (rpackComponent == null) {
				rpsRpackComponent.setAdminAuditDetails(malpracticeDetails);
				rpsRpackComponent.setCreationDate(mReportMetadata.getGenerationTime().getTime());
				rpsRpackComponent.setRpackComponentName(RpsConstants.RpackComponents.MALPRACTICE_LOG.toString());
				rpsRpackComponent.setStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
				rpsRpackComponentList.add(rpsRpackComponent);
			}
			malpracticeExtdPath = reportJsonFilePath.substring(0, lastIndexOfFS);
		}
		// updating rpsRpackComponent into database...
		rpsRpackComponentService.saveRPackComponentDetails(rpsRpackComponentList);
		logger.info("----OUT readAndUpdateMalpracticeReport()-----");
	}

	/**
	 * 
	 * @param eodPackFilePath
	 * @return
	 */
	private int getlastIndexOfFileSeperator(String eodPackFilePath) {
		int lastIndexFS = 0;
		if (eodPackFilePath.contains(File.separator))
			lastIndexFS = eodPackFilePath.lastIndexOf(File.separator);
		else if (eodPackFilePath.contains("\\"))
			lastIndexFS = eodPackFilePath.lastIndexOf("\\");
		else if (eodPackFilePath.contains("/"))
			lastIndexFS = eodPackFilePath.lastIndexOf("/");

		return lastIndexFS;
	}
}
