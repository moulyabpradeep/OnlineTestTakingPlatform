/**
 *
 */
package com.merittrac.apollo.rps.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.merittrac.apollo.common.ApolloConstants;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.cemexportentities.Customer;
import com.merittrac.apollo.common.entities.deliverymanager.AssessmentBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.CustomerEventBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.PackEntity;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.PackInfoBean;
import com.merittrac.apollo.common.entities.rps.CEMDetailsRequestEntity;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.common.exception.HttpClientException;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCustomer;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsCustomerService;
import com.merittrac.apollo.data.service.RpsDivisionService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.jms.rabbitmq.util.FileObject;
import com.merittrac.apollo.rps.common.PackAlreadyProcessedException;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsConstants.PackType;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.SFTPException;
import com.merittrac.apollo.rps.dm.BPackProcessor;
import com.merittrac.apollo.rps.jms.receiver.CemReceiver;
import com.merittrac.apollo.rps.job.packdependencyresolver.PackDependencyResolverJob;
import com.merittrac.apollo.sftp.exception.SftpOperationException;

import net.lingala.zip4j.exception.ZipException;

/**
 * Provides service apis to process and load a Bpack. The Bpacks are received over the jms queue asynchronously.
 *
 * @author Mohammed_A
 *
 */
public class BpackReceiverService {

	private static final Logger logger = LoggerFactory.getLogger(BpackReceiverService.class);

	@Autowired
	BPackProcessor bPackProcessor;

	@Autowired
	CryptUtil cryptUtil;

	@Autowired
	RpsPackService rpsPackService;

	@Value("${apollo_home_dir}")
	private String APOLLO_HOME;

	@Value("${isBPackMetaDataEncrypted}")
	private String isBPackMetaDataEncrypted;

	@Value("${isBpackEncrypted}")
	private String isBpackEncrypted;

	@Autowired
	RpsCustomerService rpsCustomerService;

	@Autowired
	RpsDivisionService rpsDivisionService;

	@Autowired
	RpsEventService rpsEventService;

	@Autowired
	RpsAssessmentService rpsAssessmentService;

	@Value("${qpdURI}")
	String qpdURI;

	@Value("${cemInfoRequestMethodName}")
	String cemInfoRequestMethodName;

	@Value("${secureAPIHeaderIP}")
	private String secureAPIHeaderIP;

	@Value("${secureAPITokenPrefix}")
	private String secureAPITokenPrefix;

	@Autowired
	Gson gson;

	@Autowired
	PackDependencyResolverJob packDependencyResolverJob;

	@Autowired
	CemReceiver cemReceiver;

	final String USER_HOME= "user.home";
	final String ENCRYPTED_FOLDER="encrypted";
	final String JSON=".json";
	final String ZIP= ".zip";
	final String BATCH_EXTENDED="_batch_extended";

	/**
	 * Service api to receive the Bpack over the jms queue. Calls code to load the meta-data and the Bpack separately.
	 *
	 * @param fileObject
	 * @param isBatchCancelledOrExtnd
	 * @throws SftpOperationException
	 * @throws Exception
	 * @throws SFTPException
	 * @throws IOException
	 * @throws Exception
	 */
	@Transactional(rollbackFor=Exception.class, isolation=Isolation.SERIALIZABLE)
	public Map<String, String> receive(PackEntity bPackEntity, String packPath, boolean isManualUpload) throws IOException,
			RpsException, SFTPException, SftpOperationException, PackAlreadyProcessedException, Exception  {
		String apolloHome = FilenameUtils.separatorsToSystem(APOLLO_HOME);
		Map<String, String> packStatusMap = new HashMap<String, String>();
		String extractedFolder = null;
		try {
			logger.info("Processing metadata.");
			boolean isCEMDetailsAvailable = checkCEMDetails(bPackEntity);
			CustomerEventBean customerEventBean = bPackEntity.getCustomerEventBean();
			RpsCustomer rpsCustomer = bPackProcessor.getBPackCustomer(customerEventBean.getCustomerCode());
			RpsDivision rpsDivision = bPackProcessor.getBPackDivision(customerEventBean.getDivisionCode(), rpsCustomer);
			//here
			RpsEvent rpsEvent = bPackProcessor.getBPackEvent(customerEventBean.getEventCode(), rpsDivision);
			List<RpsAssessment> rpsAssessmentList =
					bPackProcessor.getBPackAssessments(customerEventBean.getAssessmentCodes());
			Map<String, RpsAssessment> assessmentMap = new HashMap<String, RpsAssessment> ();

			if (rpsAssessmentList != null) {
				if(rpsAssessmentList.size() != customerEventBean.getAssessmentCodes().size()) {
					String errMsg = "Complete List of Assessments not found.";
					throw new RpsException(errMsg);
				}

				for(RpsAssessment rpsAssessment : rpsAssessmentList){
					assessmentMap.put(rpsAssessment.getAssessmentCode(), rpsAssessment);
				}
			}

			Map<Long, PackInfoBean> requestActionMap = bPackEntity.getRequestActionMap();
			if(requestActionMap==null){
				logger.info("requestActionMap is null, Hence cannot process Bpack");
				return null;
			}
			Set<Long> requestActionMapKeySet =  requestActionMap.keySet();
			List<Long> requestActionMapKeyList = new ArrayList<Long>(requestActionMapKeySet);
			Collections.sort(requestActionMapKeyList);
			RpsBatch rpsBatch = null;
			RpsAcsServer rpsAssessmentServer = null;
			RpsBatchAcsAssociation rpsBatchAcsAssociation = null;

			for(Long requestActionMapKey : requestActionMapKeyList) {
				PackInfoBean packInfoBean = requestActionMap.get(requestActionMapKey);
				switch(packInfoBean.getAction()){
					case BATCH_ADDITION_REGULAR:
					case BATCH_ADDITION_EXTRA:
						BatchMetaDataBean batchMetaDataBean = bPackEntity.getBatchMap().get(packInfoBean.getCode());
						rpsBatch = bPackProcessor.updateBatchMetadata(bPackEntity, batchMetaDataBean, rpsEvent);
						rpsAssessmentServer = bPackProcessor.persistAcsServer(bPackEntity.getAssessmentServer(), rpsEvent);
						rpsBatchAcsAssociation =  bPackProcessor.persistBatchAcsAssociation(rpsBatch,
															rpsAssessmentServer, bPackEntity.getAssessmentServer());
						break;
					case BPACK_ACTIVATION:
						RpsPack rpsPack = null;
						try {
							String filePath = apolloHome + File.separator + customerEventBean.getCustomerCode() +
												File.separator + customerEventBean.getDivisionCode() +
												File.separator + customerEventBean.getEventCode() +
												File.separator + RpsConstants.PackType.BPACK;
							/** Below 3 if blocks will execute in case of Manual upload and the batch was extended.
							 *  i.e., BATCH_ADDITION_EXTRA was not available in the metadata.
							 */
							if(rpsBatch == null) {
								BpackExportEntity bpackExportEntity = bPackEntity.getBpackExportEntities().get(packInfoBean.getCode());
								Set<String> batchCodes = bpackExportEntity.getBatchCodes();
								Iterator<String> batchCodeIterator = batchCodes.iterator();
								String batchCode = null;
								if (batchCodeIterator != null && batchCodeIterator.hasNext())
									batchCode = batchCodeIterator.next();
								batchMetaDataBean = bPackEntity.getBatchMap().get(batchCode);
								rpsBatch = bPackProcessor.updateBatchMetadata(bPackEntity, batchMetaDataBean, rpsEvent);
							}
							if(rpsAssessmentServer == null)
								rpsAssessmentServer = bPackProcessor.persistAcsServer(bPackEntity.getAssessmentServer(), rpsEvent);
							if(rpsBatchAcsAssociation == null)
								rpsBatchAcsAssociation = bPackProcessor.persistBatchAcsAssociation(rpsBatch, rpsAssessmentServer, bPackEntity.getAssessmentServer());

							rpsPack = bPackProcessor.processBPack(bPackEntity, packInfoBean, rpsBatchAcsAssociation, filePath, isManualUpload);
							String localFilePath = FilenameUtils.separatorsToSystem(rpsPack.getLocalFileDestinationPath() + File.separator
									+ bPackEntity.getAssessmentServer().getAssessmentServerCode());
							if(!isManualUpload)
								bPackProcessor.downloadBPackFromSFTP(localFilePath, bPackEntity, packInfoBean);
							else
								localFilePath = packPath;


							String zipFilePath = localFilePath + File.separator + FilenameUtils.getName(rpsPack.getPackFileDownloadPath());
							extractedFolder = copyAndExtractZip(zipFilePath , rpsEvent, rpsPack,
											bPackEntity.getAssessmentServer().getAssessmentServerCode());

							logger.info("BPack extracted to {} ", extractedFolder);
							String bPackJSONString = bPackProcessor.getBPackJSONString(new File(extractedFolder + File.separator + RpsConstants.bPackJSON));
							bPackProcessor.processBpack(extractedFolder, bPackJSONString, rpsEvent, rpsAssessmentServer, assessmentMap);
							bPackProcessor.updatePackRow(rpsPack, RpsConstants.packStatus.UNPACKED.toString());
						} catch (RpsException e) {
							if(rpsPack != null)
								bPackProcessor.updatePackRow(rpsPack, RpsConstants.packStatus.UNPACK_ERROR.toString());
							throw e;
						}
						break;
					case BATCH_EXTENSION:
						batchMetaDataBean = bPackEntity.getBatchMap().get(packInfoBean.getCode());
						rpsBatch = bPackProcessor.updateBatchMetadata(bPackEntity, batchMetaDataBean, rpsEvent);
						rpsAssessmentServer = bPackProcessor.persistAcsServer(bPackEntity.getAssessmentServer(), rpsEvent);
						rpsBatchAcsAssociation =  bPackProcessor.persistBatchAcsAssociation(rpsBatch,
															rpsAssessmentServer, bPackEntity.getAssessmentServer());
						rpsBatchAcsAssociation = bPackProcessor.extendBatch(rpsBatchAcsAssociation, batchMetaDataBean);
						break;
					case BATCH_LATE_LOGIN_EXTENSION:
					case CANDIDATE_LATE_LOGIN_EXTENSION:
						break;
					case QPACK_ACTIVATION:
					case APACK_ACTIVATION:
					case BATCH_CANCELLATION:
					case EXIT_SEQUENCE_UPDATE:
					case RPACK_GENERATION_TIME:
						break;
				}
			}
		} catch (SFTPException se) {
			logger.error("SFTP exception");
			throw se;
		}
		catch(ZipException ze){
			logger.error("ZipException unpacking Bpack : " + ze.getMessage());
			packStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
			throw ze;
		} catch (RpsException re) {
			packStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
			logger.error("RpsException processing the bPack or Metadata : " +re.getMessage());
			throw re;
		} catch (Exception exception) {
			packStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
			logger.error("Exception processing the bPack or Metadata : " + exception.getMessage());
			throw exception;
		} finally {
			if(extractedFolder != null)
				FileUtils.deleteQuietly(new File(extractedFolder));
		}
		return packStatusMap;
	}

	/**
	 * Backs up the meta-data string onto a file as retrieved from the jms header.
	 *
	 * @param destinationDir
	 * @param bPackMetadataString
	 * @throws RpsException
	 */
	private void storeMetadataAsFile(RpsPack pack, String destinationDir, String metaDataFileName, String bPackMetadataString,
			PackEntity packExportEntity) throws RpsException {
		logger.debug("--IN storeMetadataAsFile--");
		String errMsg = "Error writing the metadata to file. ";
		if (bPackMetadataString == null) {
			throw new RpsException(errMsg);
		}
		logger.debug("BPACK_STRING " + bPackMetadataString);

		boolean isArchivalReqrd= true;
		//String metadataFileName = bPackProcessor.getbPackFileObject().getFileName();
		File destinationDirFolder= new File(destinationDir);
		if(metaDataFileName!=null && !metaDataFileName.isEmpty())
			metaDataFileName = metaDataFileName.replace(ZIP, JSON);
		else
			{
			  String appendFile= "";
			  Calendar cal = Calendar.getInstance();
			  cal.setTime(new Date());
			  String timeStamp = cal.get(Calendar.DAY_OF_MONTH) + "" +  cal.get(Calendar.DAY_OF_WEEK) + cal.get(Calendar.YEAR) + cal.get(Calendar.HOUR_OF_DAY) + cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND);

			  if(pack == null)
				  isArchivalReqrd= false;
			  else
				  metaDataFileName= pack.getPackId()+ appendFile+JSON;
			}

		if(isArchivalReqrd){
			File metaDataFile = new File(FilenameUtils.separatorsToSystem(destinationDirFolder.getParent() + File.separator + metaDataFileName));
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(metaDataFile));
				bw.write(bPackMetadataString);
				bw.close();
			} catch (IOException e) {
				errMsg += e.getMessage();
				logger.error("Bpack Failed "+e.getMessage(), e);
				throw new RpsException(errMsg);
			}
			logger.info("Exported metadata @::" + metaDataFile.getAbsolutePath());
		}

		logger.debug("--OUT storeMetadataAsFile--");
	}

	/**
	 * Unzips the Bpack from packFileDownloadPath. Parses and returns the extracted folder name.
	 *
	 * @param packFileDownloadPath
	 * @param rpsPack
	 * @param zipFile
	 * @return
	 * @throws RpsException
	 * @throws ApolloSecurityException
	 * @throws IOException
	 * @throws ZipException
	 */
	private String copyAndExtractZip(String packFileDownloadPath, RpsEvent rpsEvent,
			RpsPack rpsPack, String acsCode) throws
				RpsException, IOException, ApolloSecurityException, ZipException {
		String tempPath = FilenameUtils.separatorsToSystem(APOLLO_HOME + File.separator + "Temp" + File.separator + acsCode);

		if(!new File(tempPath).exists())
			new File(tempPath).mkdirs();

		String extractedBPackFolder = FilenameUtils.separatorsToSystem(tempPath + File.separator + FilenameUtils.getBaseName(packFileDownloadPath));

		File dir = new File(packFileDownloadPath);
		try {
			if(isBpackEncrypted.equalsIgnoreCase(RpsConstants.YES)){
				//TODO: Revisit the paths
				cryptUtil.decryptFileUsingAES(new File(packFileDownloadPath), new File(tempPath + File.separator
						+ FilenameUtils.getName(packFileDownloadPath)), rpsEvent.getEventCode());

				ZipUtility.extractAllOptimized(tempPath + File.separator + FilenameUtils.getName(packFileDownloadPath), tempPath, false);
				FileUtils.deleteQuietly(new File(tempPath));
			}
			else {
				ZipUtility.extractAllOptimized(packFileDownloadPath, extractedBPackFolder, false);
				extractedBPackFolder= FilenameUtils.separatorsToSystem(extractedBPackFolder + File.separator + FilenameUtils.getBaseName(packFileDownloadPath));
			}

			if(isBPackMetaDataEncrypted.equalsIgnoreCase("yes")) {
				cryptUtil.decryptFileUsingAES(new File(extractedBPackFolder + File.separator + RpsConstants.bPackJSON),  new File(extractedBPackFolder + File.separator + "dec_" + RpsConstants.bPackJSON),
						rpsEvent.getEventCode());
				FileUtils.deleteQuietly(new File(extractedBPackFolder + File.separator + RpsConstants.bPackJSON));
				new File(extractedBPackFolder + File.separator + "dec_" + RpsConstants.bPackJSON).renameTo(new File(extractedBPackFolder + File.separator + RpsConstants.bPackJSON));
			}
		} catch (ZipException e1 ) {
			logger.error("Bpack Failed "+e1.getMessage(), e1);
			String errMsg = "Error while unzipping the bPack::" + packFileDownloadPath + " in directory::" + dir.getName() + "; Reason::" + e1.getMessage();
			rpsPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());
			rpsPackService.addRpsPack(rpsPack);
			throw new RpsException("Error while unzipping the bPack");
		} catch(Exception ex)
		{
			logger.error("Bpack Failed "+ex.getMessage(), ex);
			String errMsg =
					"Error while unzipping the bPack::" + packFileDownloadPath + " in directory::" + dir.getName()
							+ "; Reason::" + ex.getMessage();
			rpsPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());
			rpsPackService.addRpsPack(rpsPack);
			throw new ZipException(errMsg);

		}

		return extractedBPackFolder;
	}

	/**
	 * Backup the Zip file into APOLLO_HOME/eventCode/Bpack/acsCode directory. Called after metadata is read
	 * successfully.
	 *
	 * @param packFileDownloadPath
	 * @param zipFile
	 * @throws RpsException
	 */
	private void copyFileToLocation(String packFileDownloadPath, FileObject fileObject) throws RpsException {
		logger.debug("--IN copyFileToLocation--");
		File file = new File(packFileDownloadPath);
		try {
			byte[] byteArr = fileObject.getByteArray();
			if (byteArr == null) {
				String errMsg = "No file found in the jms message!!";
				throw new RpsException(errMsg);
			}

			FileOutputStream fileOutputStream = new FileOutputStream(file);
			fileOutputStream.write(byteArr);
			fileOutputStream.close();
			logger.debug("--OUT--");
		} catch (IOException ioe) {
			String errMsg =
					"Error while copying file to custom location::" + packFileDownloadPath + "; Reason::"
							+ ioe.getMessage();
			logger.error("Bpack Failed "+ ioe.getMessage());
			logger.error("Bpack Failed ", ioe);
			throw new RpsException(errMsg);
		}
		logger.debug("--OUT copyFileToLocation--");
	}

	/**
	 * 
	 * @param packExportEntity
	 * @return true or false, If CEM details available in database,return true else return false
	 */
	public boolean checkCEMDetails(final PackEntity bPackExportEntity) {
		CustomerEventBean customerEventBean = bPackExportEntity.getCustomerEventBean();
		String custCode = customerEventBean.getCustomerCode();
		String divCode = customerEventBean.getDivisionCode();
		String evntCode = customerEventBean.getEventCode();

		RpsCustomer rpsCustomer = rpsCustomerService.findByCustomerCode(custCode);
		RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(divCode);
		RpsEvent rpsEvent = rpsEventService.findByEventCode(evntCode);
		List<RpsAssessment> rpsAssessmentList =
				bPackProcessor.getBPackAssessments(customerEventBean.getAssessmentCodes());

		if (rpsCustomer == null || rpsDivision == null || rpsEvent == null || rpsAssessmentList == null
				|| rpsAssessmentList.size() != customerEventBean.getAssessmentCodes().size()) {
			return handleCemInfoDependency(customerEventBean);
		} else
			return true;
	}

	/**
	 * Handles the CEM info dependency and resolves it by making a REST call to QPD and gets the metadata.
	 */
	private boolean handleCemInfoDependency(CustomerEventBean customerEventBean) {
		String token = null;
		try {
			String inputData = populateCDEAInfo(customerEventBean);
			// Invoke the REST api.
			token = packDependencyResolverJob.getTokenforSecureAuthorization(false);
			Map<String, String> mapOfFormParams = new HashMap<>();
			mapOfFormParams.put(HttpHeaders.AUTHORIZATION, secureAPITokenPrefix + token);
			mapOfFormParams.put(ApolloConstants.HEADER_IP, secureAPIHeaderIP);
			String response =
					HttpClientFactory.getInstance().requestPostWithJson(qpdURI, cemInfoRequestMethodName, inputData,
							mapOfFormParams);
			if (response != null && !response.equalsIgnoreCase("null")) {
				// convert the response to bean
				Customer customer = gson.fromJson(response, Customer.class);
				if (cemReceiver.checkCEMDetails(customer, PackType.BPACK))
					return true;
				else
					return false;

			}
		} catch (HttpClientException e) {
			logger.error("Communication with QPD for CEM info had HttpClientException "
					+ e.getMessage());
			int statusCode = e.getCode();
			if (statusCode == (1001) || statusCode == (1002)) {
				// String customMessage = e.getMessage();
				// if (customMessage.equalsIgnoreCase("Token Expired") || customMessage.equalsIgnoreCase("Invalid
				// Token")) {
				token = packDependencyResolverJob.getTokenforSecureAuthorization(true);
				handleCemInfoDependency(customerEventBean);
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}

	private String populateCDEAInfo(CustomerEventBean customerEventBean) {
		CEMDetailsRequestEntity cemDetailsRequestEntity =
				new CEMDetailsRequestEntity(customerEventBean.getCustomerCode(), customerEventBean.getDivisionCode(),
						customerEventBean.getEventCode());
		List<String> assessmentCodes = new ArrayList<String>();
		for (AssessmentBean assessmentBean : customerEventBean.getAssessmentCodes()) {
			assessmentCodes.add(assessmentBean.getAssessmentCode());
		}
		cemDetailsRequestEntity.setAssessmentCodes(assessmentCodes);
		return gson.toJson(cemDetailsRequestEntity);
	}
}