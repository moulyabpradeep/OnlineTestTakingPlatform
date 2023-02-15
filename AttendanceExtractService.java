package com.merittrac.apollo.rps.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.MessageHeaders;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.entities.acs.AttendanceExportEntity;
import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsMasterAssociation;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsCandidateService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsPackFailedStatusService;
import com.merittrac.apollo.data.service.RpsPackService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.RpsRpackException;
import com.merittrac.apollo.rps.jms.receiver.AbstractReceiver;
import com.merittrac.apollo.rps.utility.RpsGeneralUtility;

import net.lingala.zip4j.exception.ZipException;


public class AttendanceExtractService extends AbstractReceiver implements IAttendanceExtractService{
	
	@Autowired
	private RpsPackService rpsPackService;
	
	@Autowired
	RpsEventService rpsEventService;

	@Autowired
	CryptUtil cryptUtil;

	@Autowired
	Gson gson;
	
	@Autowired
	RpsPackFailedStatusService rpsPackFailedStatusService;

	@Value("${isAttendanceDirEncrypted}")
	private String isAttendanceDirEncrypted;
	
	@Value("${isAttendanceFileEncrypted}")
	private String isAttendanceFileEncrypted;
	
	@Value("${apollo_home_dir}")
	private String APOLLO_HOME;

	@Autowired
	private RpsCandidateService rpsCandidateService;
	
	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;
	
	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;
	
	@Autowired
	RpsGeneralUtility rpsGeneralUtility;
	
	@Autowired
	RpsAssessmentService rpsAssessmentService;
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(AttendanceExtractService.class);
	
	@Value("${attendance}")
	private String isAttendanceEncptd;
	
	public AttendanceExtractService() throws RpsException{
		super();
	}
	
	private String getFolderName(final String rPackFilename)
	{
		String foldername="";
		final int pos = rPackFilename.lastIndexOf('.');
		if (pos > 0) 
			foldername = rPackFilename.substring(0, pos);
		return foldername;
	}
	
	@Transactional(rollbackFor= Exception.class, isolation=Isolation.SERIALIZABLE)
	public void extractAttendanceReport(String apolloHome, String filename,
			MessageHeaders messageHeaders, RpsPack rpsPack) throws ZipException, IOException, ApolloSecurityException, RpsException, ParseException {
    	
		String customer="";
		String division="";
    	String event="";
    	//updating Pack Status as PACK_RECEIVED--
		if(messageHeaders!=null)
			{
		       	customer= messageHeaders.get(customerCode).toString().trim();
		       	division= messageHeaders.get(divisionCode).toString().trim();
				event= messageHeaders.get(eventCode).toString().trim();
			    if(rpsPack!=null)
			    {
			    	rpsPack.setPackStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());
					if(rpsPack.getRpsBatchAcsAssociation() != null && rpsPack.getPackId().length() != 0 && 
							rpsPack.getPackType().length() != 0 && rpsPack.getVersionNumber().length() != 0){
						RpsPack savedRpsPackFromDB=rpsPackService.getRpsPack(rpsPack.getRpsBatchAcsAssociation(), rpsPack.getPackId(),
																					rpsPack.getPackType(), rpsPack.getVersionNumber());
						if(savedRpsPackFromDB!=null)
							return;
						else
							rpsPack = rpsPackService.addRpsPack(rpsPack);
					}else {
						LOGGER.error("Attendance Headers are not correct, or there is no corresponding entries in database");
						throw new RpsException("Attendance Headers are not correct, or there is no corresponding entries in database");
					}
			    }
			}

		rpsPack.setPackReceivingMode(RpsConstants.packReceiveMode.JMS_UPLOAD.toString());
		rpsPack.setCreationDate(Calendar.getInstance().getTime());
		rpsPack.setLastModifiedDate(Calendar.getInstance().getTime());
		// fetching master asso details for RpsBatchAcsAssociation
		List<RpsMasterAssociation> rpsMasterAssociations =
				rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsPack.getRpsBatchAcsAssociation());

		Map<String, RpsMasterAssociation> rpsMasterAssociationsMap = new HashMap<String, RpsMasterAssociation>();
		if (rpsMasterAssociations != null && !rpsMasterAssociations.isEmpty()) {
			for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociations) {
				rpsMasterAssociationsMap.put(rpsMasterAssociation.getRpsCandidate().getCandidateId1(),
						rpsMasterAssociation);
			}
		}
		try
		{
			List<AttendanceReportEntity> attendanceReportEntityList =
					this.extractAndDecryptAttendanceZip(apolloHome, filename, event, rpsPack, rpsMasterAssociationsMap);
				rpsPack.setPackStatus(RpsConstants.packStatus.UNPACKED.toString());
		}catch(RpsException rpsException){
			LOGGER.error("Error in processing Attendance Pack", rpsException);
			throw rpsException;
		}catch(IOException ioException){
			LOGGER.error("Error in processing Attendance Pack", ioException);
			throw ioException;
		}catch(ParseException parseException){
			LOGGER.error("Error in processing Attendance Pack", parseException);
			throw parseException;
		}catch(ZipException zipException){
			LOGGER.error("Unable to unzip the file :"+ apolloHome+File.separator+filename);
			rpsPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());			
		}catch(Exception ex){
				LOGGER.error("Unable to unzip the file :"+ apolloHome+File.separator+filename);
				rpsPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());
		}finally
		{
			String archivePath = this.storePackInArchive(customer, division, event, apolloHome, filename);
			//TODO: Added By Amar, deleting the attendance pack from C drive.
			String folderName = this.getFolderName(filename);
			
			//rpsGeneralUtility.deleteZipFileFromLocation(apolloHome + File.separator + folderName+ File.separator + filename);
			rpsGeneralUtility.deleteFileFromLocation(apolloHome + File.separator + folderName);
			File attendanceFile = new File(filename);
			if(attendanceFile.exists())
				rpsGeneralUtility.deleteZipFileFromLocation(apolloHome + File.separator + filename);
			
			rpsPack.setLocalFileDestinationPath(archivePath);
			rpsPackService.addRpsPack(rpsPack);
		}
    }

	private String storePackInArchive(String customer, String division,
			String event, String downloadPath, String filename) throws IOException {
		
		String archiveDir = APOLLO_HOME;
		
		File archivePath = null;
		
			archivePath = new File(archiveDir+File.separator + customer+ File.separator + division + File.separator + event);
			if(!archivePath.isDirectory())
			{
				archivePath.mkdirs();
			}
			FileUtils.copyFileToDirectory(new File(downloadPath+File.separator+filename), archivePath.getAbsoluteFile());
		
		return FilenameUtils.separatorsToSystem(archivePath.getAbsolutePath());
	}

	// @Transactional(rollbackFor= Exception.class, isolation=Isolation.SERIALIZABLE)
	public void processAttendanceReport(List<AttendanceReportEntity> attendanceReportEntityList,
			RpsBatchAcsAssociation rpsBatchAcsAssociation, String eventCode,
			Map<String, RpsMasterAssociation> rpsMasterAssociationsMap, RpsPack rpsPack, String packDownloadLocation)
			throws IOException, RpsException, ParseException, RpsRpackException {

		LOGGER.info("---IN-- processAttendanceReport()");
		if(attendanceReportEntityList== null || attendanceReportEntityList.isEmpty())
			throw new RpsException("Attendance.JSON is empty ");
		
		for(AttendanceReportEntity attendanceReportEntity: attendanceReportEntityList){

			String candId = attendanceReportEntity.getCandidateIdentifier();
			String loginId = attendanceReportEntity.getLoginID();
			// String assessmentCode= attendanceReportEntity.getAssessmentCode();
			RpsMasterAssociation rpsMasterAssociation = rpsMasterAssociationsMap.get(candId);
			// RpsCandidate rpsCandidate= rpsCandidateService.getRpsCandidateByCandIdAndEventCode(candId, event);
			// if(rpsCandidate== null){
			// LOGGER.error("Candidate Information is not available in rps_candidate candidate table :"+ candId);
			// throw new RpsException("Candidate Information is not available in rps_candidate candidate table :"+
			// candId);
			// }
			
			// RpsMasterAssociation rpsMasterAssociation=
			// rpsMasterAssociationService.getRpsMasterAssociationPrimaryKey(rpsCandidate, assessmentCode,
			// rpsBatchAcsAssociation);

			if(rpsMasterAssociation== null){
				LOGGER.error("Candidate has no association in rps_master_association table loginid: " + loginId+" candId : "+candId);
				// add association
				RpsCandidate rpsCandidate = rpsCandidateService.getRpsCandidateByCandAndEventCode(loginId, eventCode);
				// check if candidate present
				if (rpsCandidate == null) {
					LOGGER.error("Candidate Information is not available in rps_candidate candidate table loginid: " + loginId+" candId : "+candId);
					throw new RpsRpackException(
							"Candidate Information is not available in rps_candidate candidate table. Hence saved the Rpack for future :"
									+ loginId);
				}
				RpsAssessment rpsAssessment =
						rpsAssessmentService.findByCode(attendanceReportEntity.getAssessmentCode());
				// if cand present, then add association
				rpsMasterAssociation =
						createMasterAssociationEntry(rpsCandidate, rpsAssessment, rpsBatchAcsAssociation);
			}
			
			rpsMasterAssociation.setIpAddress(attendanceReportEntity.getIpAddress());
			rpsMasterAssociation.setLoginID(attendanceReportEntity.getLoginID());
			String loginTime= attendanceReportEntity.getLoginTime();
			//check for "null" string, need to be removed after acs gives proper build
			if(loginTime!=null && !loginTime.isEmpty() && !loginTime.equalsIgnoreCase(NULL))
				rpsMasterAssociation.setLoginTime(new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME).parse(loginTime));
			String testStartTime = attendanceReportEntity.getTestStartTime();
			if(testStartTime!=null && !testStartTime.isEmpty()  && !testStartTime.equalsIgnoreCase(NULL))
				rpsMasterAssociation.setTestStartTime(new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME).parse(testStartTime));
			String testEndTime= attendanceReportEntity.getActualTestEndTime();
			if(testEndTime!=null && !testEndTime.isEmpty()  && !testEndTime.equalsIgnoreCase(NULL))
				rpsMasterAssociation.setTestEndTime(new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME).parse(testEndTime));
		
			String present= attendanceReportEntity.getIsPresent();
			if(present.equalsIgnoreCase(YES))
				rpsMasterAssociation.setPresent(true);
			else {
				if (rpsMasterAssociation.getLoginTime() == null)
					rpsMasterAssociation.setPresent(false);
				else
					rpsMasterAssociation.setPresent(true);
			}
			rpsMasterAssociationsMap.put(candId, rpsMasterAssociation);
			// try{
			// rpsMasterAssociationService.addAssociation(rpsMasterAssociation);
			// }catch(Exception e){
			// LOGGER.error("Exception while adding the master association :", e.getMessage());
			// throw new RpsException("Exception while adding the master association :" + e.getMessage());
			// }

		}
		LOGGER.info("---OUT-- processAttendanceReport()");
	}

	@Transactional
	private RpsMasterAssociation createMasterAssociationEntry(RpsCandidate rpsCandidate, RpsAssessment rpsAssessment,
			RpsBatchAcsAssociation rpsBatchAcsAssociation) {

		RpsMasterAssociation rpsMasterAssociation = new RpsMasterAssociation();
		rpsMasterAssociation.setRpsAssessment(rpsAssessment);
		rpsMasterAssociation.setRpsCandidate(rpsCandidate);
		rpsMasterAssociation.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
		// rpsMasterAssociation= rpsMasterAssociationService.addAssociation(rpsMasterAssociation);
		return rpsMasterAssociation;
	}
	private List<AttendanceReportEntity> extractAndDecryptAttendanceZip(String apolloHome,
			String fileName, String event, RpsPack rpsPack, Map<String, RpsMasterAssociation> rpsMasterAssociationsMap)
			throws IOException, ApolloSecurityException, ZipException, RpsException, ParseException, RpsRpackException {
		
		File decyrptPath=new File(apolloHome+File.separator+ this.getFolderName(fileName));
		
		if(!decyrptPath.exists())
			decyrptPath.mkdirs();
			
		List<AttendanceReportEntity> attendanceReportEntityList= null;
		if(isAttendanceDirEncrypted.equalsIgnoreCase(attendanceDirEncpt))
		{		
			//first decrypt the entire ZIP file to the folder
			cryptUtil.decryptFileUsingAES(new File(apolloHome+File.separator + fileName), new File(decyrptPath+File.separator+fileName), event);
			File extractDirectory= new File(decyrptPath+File.separator+ this.getFolderName(fileName));
			if(!extractDirectory.exists())
				extractDirectory.mkdirs();
			
			ZipUtility.extractAll(decyrptPath+File.separator + fileName, extractDirectory.getAbsolutePath());
			LOGGER.info("RPack has been Extracted at the path = {} "+extractDirectory.getAbsolutePath());
			
			if(extractDirectory.isDirectory())
			{
				String[] files = extractDirectory.list();
				if(files!=null && files.length!=0)
				{
					for(String fName: files)
					{
						if(fName.equalsIgnoreCase(attendance_file))
						{
							File file= new File(extractDirectory+File.separator+fName);
							
							String text =this.getAndSaveObjectFromEncryptedJson(file);

							Type mapType1 = new TypeToken<AttendanceExportEntity>() {
							}.getType();
							
							
							AttendanceExportEntity attendanceExportEntity = null;
							try {
								attendanceExportEntity = new Gson().fromJson(text, mapType1);
							} catch (Exception e) {
								LOGGER.error(
										" Exception while processing the Attendance JSON file", e);
								throw new RpsException(
										" Exception while processing the Attendance JSON file" + e);

							}
							//save decrypted text to database
							attendanceReportEntityList = attendanceExportEntity.getAttendanceReportEntities();
							if(attendanceReportEntityList== null|| attendanceReportEntityList.isEmpty())
								throw new RpsException("Attendance.Json is empty or not present in attendace report zip :" + apolloHome+ File.separator + fileName);

							RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsPack.getRpsBatchAcsAssociation();
							// rpsBatchAcsAssociationService.find(rpsPack.getRpsBatchAcsAssociation().getBatchAcsId());

							Calendar oldGenerationTime = rpsBatchAcsAssociation.getAttendanceGenerationTime();
							Calendar newGenerationTime = attendanceExportEntity.getGenerationTime();
							
							if(oldGenerationTime == null || oldGenerationTime.before(newGenerationTime)) {
								rpsBatchAcsAssociation.setAttendanceDetails(text);
								rpsBatchAcsAssociation.setAttendanceGenerationTime(newGenerationTime);
							}
							rpsBatchAcsAssociation = rpsBatchAcsAssociationService.addBatchAcsID(rpsBatchAcsAssociation);
							rpsPack.setRpsBatchAcsAssociation(rpsBatchAcsAssociation);
							rpsPack = rpsPackService.addRpsPack(rpsPack);
							if(oldGenerationTime == null || oldGenerationTime.before(newGenerationTime))
								this.processAttendanceReport(attendanceReportEntityList,
										rpsPack.getRpsBatchAcsAssociation(), event, rpsMasterAssociationsMap, rpsPack,
										apolloHome + File.separator + this.getFolderName(fileName));
							break;
						}
					}
				}
			}
		}
		else
		{
			LOGGER.info("Rpack is not encrypted, so decryption is not required----");
		}
		return attendanceReportEntityList;
		
	}

	public List<AttendanceReportEntity> adjustNullValuesInReport(
			List<AttendanceReportEntity> attendanceReportEntityList) {

		String empty="";
		List<AttendanceReportEntity> listWtNulls= new ArrayList<>();
		if(attendanceReportEntityList!=null && !attendanceReportEntityList.isEmpty()){
			for(AttendanceReportEntity attendanceReportEntity: attendanceReportEntityList){
				if(attendanceReportEntity.getIpAddress()== null)
					attendanceReportEntity.setIpAddress(empty);
				if(attendanceReportEntity.getLoginID()== null)
					attendanceReportEntity.setLoginID(empty);
				if(attendanceReportEntity.getLoginTime()== null)
					attendanceReportEntity.setLoginTime(null);
				if(attendanceReportEntity.getTestStartTime()== null)
					attendanceReportEntity.setTestStartTime(null);
				if(attendanceReportEntity.getActualTestEndTime()==null)
					attendanceReportEntity.setActualTestEndTime(null);
				
				listWtNulls.add(attendanceReportEntity);
			}
		}
		return listWtNulls;
	}

	public String getAndSaveObjectFromEncryptedJson(File file) throws ApolloSecurityException, IOException, RpsException {
		
		LOGGER.info("---IN-- getObjectFromEncryptedJson()");
		String text="";
		FileReader fileReader = new FileReader(file);
		BufferedReader reader= new BufferedReader(fileReader);
		String temp="";
		while((temp=reader.readLine())!=null){
			text= text+temp;
		}
		if(fileReader != null)
			fileReader.close();
		if(reader != null)
			reader.close();
		return text;
	}

	
	public  Object getObjectFromJSON(String fileName, Class className) throws IOException{
		Object object=null;
		BufferedReader bufferedReader = null;
		try {
			bufferedReader =new BufferedReader(new FileReader(fileName));
			Gson gson =new GsonBuilder().create();
			object= gson.fromJson(bufferedReader, className);
		} catch (FileNotFoundException e) {
			LOGGER.error(" Error while getting the object from JSON ",e);
		}
		if(bufferedReader!=null)
			bufferedReader.close();
		return object;
		
	}
}
