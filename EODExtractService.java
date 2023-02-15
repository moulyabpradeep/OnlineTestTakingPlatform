package com.merittrac.apollo.rps.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.merittrac.apollo.common.SecuredZipUtil;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.entities.acs.EODReportBatchDetailEntity;
import com.merittrac.apollo.common.entities.acs.EODReportEntity;
import com.merittrac.apollo.common.entities.acs.EODReportMetaDataEntity;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsReportLog;
import com.merittrac.apollo.data.service.RpsReportLogService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.utility.RpsGeneralUtility;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class EODExtractService 
{
	
	@Autowired
	RpsGeneralUtility rpsGeneralUtility;
	
	@Autowired
	RpsReportLogService rpsReportLogService;
	
	@Value("${isEODPasswordProtected}")
	private String isEODPasswordProtected;

	/**
	 * loading the properties file
	 * @throws IOException 
	 */
	
	public EODExtractService() throws IOException 
	{
		super();
	}
	
	private static final Logger logger = LoggerFactory.getLogger(EODExtractService.class);
	private static final String eodLogPrename = "EOD_Audit-";
	private static final String extAuditPreName = "External_Devices_Audit-";

	/**
	 * 
	 * @param eodPackFilePath
	 * @param acsCode
	 * @throws IOException 
	 * @throws ParseException 
	 * @throws ZipException 
	 */
	@Transactional(rollbackFor=Exception.class, isolation = Isolation.SERIALIZABLE)
	public void extractEODReport(String eodPackFilePath, RpsAcsServer rpsAcsServer) throws IOException, ParseException, ZipException 
	{
		logger.info("-----IN extractEODReport method----");
		logger.info("Is EOD Pack password protected :: {}", isEODPasswordProtected);
		String folderName = rpsGeneralUtility.getFolderName(eodPackFilePath);
		
		int lastIndexFS = this.getlastIndexOfFileSeperator(eodPackFilePath);		
		
		String destEODExtractPath = eodPackFilePath.substring(0, lastIndexFS) + File.separator + folderName;
		try
		{
			ZipFile zipFile = new ZipFile(new File(eodPackFilePath));
			if(zipFile.isValidZipFile())
			{
				if(isEODPasswordProtected.equalsIgnoreCase("yes"))
				{
					SecuredZipUtil.extractArchiveWithPassword(eodPackFilePath, destEODExtractPath, rpsAcsServer.getAcsServerId());
				}else
					ZipUtility.extractAll(eodPackFilePath, destEODExtractPath);
			}else
				throw new ZipException("EOD Pack is not valid");
			
			File eodFilePath = new File(destEODExtractPath);
			String reportMetadataJsonFileName = "";
			String reportJsonFileName = "";
			for(String eodContent : eodFilePath.list())
			{
				if(eodContent.contains(".json"))
				{
					if(eodContent.contains("MetaData"))
					{
						reportMetadataJsonFileName = eodContent;
					}
					else
					{
						reportJsonFileName = eodContent;
					}
				}
			}
			
			String reportJsonFilePath = destEODExtractPath + File.separator + reportJsonFileName;
			
			this.readAndUpdateEODReport(reportJsonFilePath, rpsAcsServer);
			
			String reportMetadataJsonFilePath = destEODExtractPath + File.separator + reportMetadataJsonFileName;
			
			this.readAndUpdateMalpracticeReport(reportMetadataJsonFilePath, rpsAcsServer);
		}finally
		{
			String path = "";
			if(eodPackFilePath.contains("."))
				path = eodPackFilePath.substring(0, eodPackFilePath.lastIndexOf("."));
			else
				path = eodPackFilePath;
			if(new File(path).isDirectory() || new File(path).isDirectory())
				FileUtils.deleteDirectory(new File(path));
		}
		logger.info("-----OUT extractEODReport method-----");
	}

	/**
	 * 
	 * @param reportJsonFilePath
	 * @param rpsAcsServer
	 * @throws IOException 
	 */
	private void readAndUpdateEODReport(String reportJsonFilePath,	RpsAcsServer rpsAcsServer) throws IOException 
	{
		logger.info("-----IN readAndUpdateEODReport-----");
		
//		String logFileDetails = rpsGeneralUtility.readFile(reportJsonFilePath);		
		
		EODReportEntity eodReportEntity = (EODReportEntity) rpsGeneralUtility.getObjectFromJson(reportJsonFilePath, EODReportEntity.class);
		if(eodReportEntity!=null)
		{
			List<EODReportBatchDetailEntity> eodReportBatchDetailEntities = eodReportEntity.getEodReportBatchDetailEntities();
			String logFileDetails = new Gson().toJson(eodReportBatchDetailEntities);
			List<Calendar> applicableDates = eodReportEntity.getApplicableDates();
			if(applicableDates!=null && !applicableDates.isEmpty())
			{
				for(Calendar appCalendar : applicableDates)
				{
					Date date = appCalendar.getTime();
					
					//Calendar calendar = eodReportEntity.getReportGenerationTime();
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
					String logDate = "";
					if(date!=null)
						logDate= simpleDateFormat.format(date);
					
					RpsReportLog rpsReportLog = rpsReportLogService.findByDateAndAcsCodeAndPackType(logDate, rpsAcsServer.getAcsId(), RpsConstants.EOD_REPORT);
					
					if(rpsReportLog==null)		
						rpsReportLog = new RpsReportLog();
					
					rpsReportLog.setLogDate(logDate);
					rpsReportLog.setReportLogDetails(logFileDetails);
					rpsReportLog.setCreationDate(Calendar.getInstance());
					rpsReportLog.setRpsAcsServer(rpsAcsServer);
					rpsReportLog.setPackType(RpsConstants.EOD_REPORT);
					
					rpsReportLogService.save(rpsReportLog);
				}
			}
		}else
			logger.info("while converting EODReport json file to EodReportEntity, getting null");
		
	}
	
	private void updateEodLog(List<String> eodFileNameList, RpsAcsServer rpsAcsServer, String reportJsonFilePath) throws ParseException, IOException 
	{
		logger.info("----IN updateEODLog method-----");
		int lastIndexOfFS = this.getlastIndexOfFileSeperator(reportJsonFilePath);
		
		for(String logFileName :eodFileNameList)
		{
			String logDate = this.getLogdateFromLogFile(logFileName,eodLogPrename);
			String logFilePath = reportJsonFilePath.substring(0, lastIndexOfFS) + File.separator + logFileName;
			String logFileDetails = this.readLogFile(logFilePath);
			logger.info("logFile Details for date :: {}", logDate, "is :: {} ", logFileDetails);
			Calendar calendar = Calendar.getInstance();
			
			RpsReportLog rpsReportLog = rpsReportLogService.findByDateAndAcsCodeAndPackType(logDate, rpsAcsServer.getAcsId(), RpsConstants.RpackComponents.EOD_LOG.toString());
			
			if(rpsReportLog==null)		
				rpsReportLog = new RpsReportLog();
			
			rpsReportLog.setLogDate(logDate);
			rpsReportLog.setReportLogDetails(logFileDetails);
			rpsReportLog.setCreationDate(calendar);
			rpsReportLog.setRpsAcsServer(rpsAcsServer);
			rpsReportLog.setPackType(RpsConstants.RpackComponents.EOD_LOG.toString());
			
			rpsReportLogService.save(rpsReportLog);
			logger.info("EOD Log Details updated in database successfully");
			logger.info("----OUT updateEODLog method-----");
		}
	}


	/**
	 * 
	 * @param reportJsonFilePath
	 * @param rpsAcsServer
	 * @throws IOException
	 * @throws ParseException
	 */
	private void readAndUpdateMalpracticeReport(String reportJsonFilePath, RpsAcsServer rpsAcsServer) throws IOException, ParseException 
	{
		logger.info("----IN readAndUpdateEODReport------");
//		String eodJson = this.readJsonFile(reportJsonFilePath);
		
		EODReportMetaDataEntity eodReportMetaDataEntity = (EODReportMetaDataEntity)rpsGeneralUtility.getObjectFromJson(reportJsonFilePath, EODReportMetaDataEntity.class);
		
//		List<String> failureNameList = eodReportMetaDataEntity.getFailureLogFileNames();
		
		List<String> externalDeviceFileNameList = eodReportMetaDataEntity.getExternalDeviceLogFileNames();
				
//		int lastIndexOfFS = this.getlastIndexOfFileSeperator(reportJsonFilePath);
		
		this.updateExternalDeviceLog(externalDeviceFileNameList, rpsAcsServer, reportJsonFilePath);
		
		// Reading Failure log....
		
		/*
		for(String logFileName :failureNameList)
		{
			String logDate = this.getLogdateFromLogFile(logFileName,eodLogPrename);
			String logFilePath = reportJsonFilePath.substring(0, lastIndexOfFS) + File.separator + logFileName;
			String logFileDetails = this.readLogFile(logFilePath);
			LOGGER.info("logFile Details for date :: {}", logDate, "is :: {} ", logFileDetails);
			Calendar calendar = Calendar.getInstance();
			
			RpsReportLog rpsReportLog = rpsReportLogService.findByDateAndAcsCodeAndPackType(logDate, rpsAcsServer.getAcsId(), RpsConstants.RpackComponents.EOD_LOG.toString());
			
			if(rpsReportLog==null)		
				rpsReportLog = new RpsReportLog();
			
			rpsReportLog.setLogDate(logDate);
			rpsReportLog.setReportLogDetails(logFileDetails);
			rpsReportLog.setCreationDate(calendar);
			rpsReportLog.setRpsAcsServer(rpsAcsServer);
			rpsReportLog.setPackType(RpsConstants.RpackComponents.EOD_LOG.toString());
			
			rpsReportLogService.save(rpsReportLog);
			LOGGER.info("EOD Details updated in database successfully");
			LOGGER.info("----OUT readAndUpdateEODReport method-----");
		}*/
		logger.info("----OUT readAndUpdateEODReport------");
//		LOGGER.info("EOD Log Report JSON :: {} ", eodJson);
	}

	/**
	 * updating External device log in database
	 * @param externalDeviceFileNameList
	 * @param rpsAcsServer
	 * @param reportJsonFilePath
	 * @throws ParseException
	 * @throws IOException
	 */
	private void updateExternalDeviceLog(List<String> externalDeviceFileNameList, RpsAcsServer rpsAcsServer, String reportJsonFilePath) throws ParseException, IOException 
	{
		logger.info("----IN updateExternalDeviceLog method-----");
		int lastIndexOfFS = this.getlastIndexOfFileSeperator(reportJsonFilePath);
		
		for(String logFileName :externalDeviceFileNameList)
		{
			String logDate = this.getLogdateFromLogFile(logFileName,extAuditPreName);
			String logFilePath = reportJsonFilePath.substring(0, lastIndexOfFS) + File.separator + logFileName;
			String logFileDetails = this.readLogFile(logFilePath);
			logger.info("logFile Details for date :: {}", logDate, "is :: {} ", logFileDetails);
			Calendar calendar = Calendar.getInstance();
			
			RpsReportLog rpsReportLog = rpsReportLogService.findByDateAndAcsCodeAndPackType(logDate, rpsAcsServer.getAcsId(), RpsConstants.RpackComponents.EXTERNALDEVICEAUDIT_LOG.toString());
			
			if(rpsReportLog==null)		
				rpsReportLog = new RpsReportLog();
			
			rpsReportLog.setLogDate(logDate);
			rpsReportLog.setReportLogDetails(logFileDetails);
			rpsReportLog.setCreationDate(calendar);
			rpsReportLog.setRpsAcsServer(rpsAcsServer);
			rpsReportLog.setPackType(RpsConstants.RpackComponents.EXTERNALDEVICEAUDIT_LOG.toString());
			
			rpsReportLogService.save(rpsReportLog);
			logger.info("ExternalDeviceAudit Log Details updated in database successfully");
			logger.info("----OUT updateExternalDeviceLog method-----");
		}
	}

	/**
	 * 
	 * @param logFileName
	 * @return
	 * @throws ParseException
	 */
	private String getLogdateFromLogFile(String logFileName, String logPreName) throws ParseException 
	{
		logger.info("----IN getEODLogdateFromLogFile()---");
		logFileName = logFileName.substring(0,logFileName.lastIndexOf("."));
		String logDate = null;
		if(logFileName.contains(logPreName))
			logDate = logFileName.replace(logPreName, "");
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
		Date lDate = simpleDateFormat.parse(logDate);
		
		SimpleDateFormat siDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		
		logDate = siDateFormat.format(lDate);
		logger.info("----OUT getEODLogdateFromLogFile()---");
		return logDate;
	}

	/**
	 * 
	 * @param reportJsonFilePath
	 * @return
	 * @throws IOException
	 */
	private String readLogFile(String logFilePath) throws IOException 
	{
		logger.info("-----IN readJsonFile method----");
		BufferedReader bufferedReader =  new BufferedReader(new FileReader(logFilePath));
		try
		{
			StringBuilder stringBuilder = new StringBuilder();
			String line = bufferedReader.readLine();
			while(line!=null)
			{
				stringBuilder.append(line);
				stringBuilder.append("\n");
				line = bufferedReader.readLine();
			}
			logger.info("------OUT readJsonFile method-----");
			return stringBuilder.toString();
		}finally
		{
			bufferedReader.close();
		}
	}
	
	private int getlastIndexOfFileSeperator(String eodPackFilePath)
	{
		int lastIndexFS = 0;
		if(eodPackFilePath.contains(File.separator))			
			lastIndexFS = eodPackFilePath.lastIndexOf(File.separator);
		else if (eodPackFilePath.contains("\\"))
			lastIndexFS = eodPackFilePath.lastIndexOf("\\");
		else if(eodPackFilePath.contains("/"))
			lastIndexFS = eodPackFilePath.lastIndexOf("/");
		
		return lastIndexFS;
	}
}
