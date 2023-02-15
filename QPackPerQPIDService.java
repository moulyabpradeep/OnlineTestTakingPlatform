package com.merittrac.apollo.rps.services;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.jms.receiver.AbstractReceiver;

public class QPackPerQPIDService extends AbstractReceiver
{
	@Autowired
	QPackDetailsServices qPackDetailsServices;
	
	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;
	
	public HttpClientFactory httpClientFactory;
	public final String qpCode="qpId";
	
	private Logger LOGGER=LoggerFactory.getLogger(QPackPerQPIDService.class);
	
	@Value("${apollo_home_dir}")
	private String APOLLO_HOME;
	
	@Value("${qPackURI}")
	private String qPackURI;

	@Value("${qpackmethodName}")
	private String qpackmethodName;

	public QPackPerQPIDService() throws RpsException
	{
		httpClientFactory = httpClientFactory.getInstance();
	}
	
	public String getURIToDownloadQPackPerQPID(Map<String, String> qpId, String assessmentCode) throws RpsException
	{

		String downloadInfo = null;
		RpsQuestionPaper questionPaper= rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentCode,qpId.get(qpCode));
		try
		{
			downloadInfo = httpClientFactory.requestPostWithMap(qPackURI, qpackmethodName, qpId);
			//set qpack download to AVAILABLE as download completed
			if(questionPaper!=null)
			{
				questionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.DOWNLOAD_SUCCESSFUL.toString());
				rpsQuestionPaperService.save(questionPaper);
			}
			
		}catch(Exception ex)
		{  //set qpack download to download failled
			
			if(questionPaper!=null)
			{
				questionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.DOWNLOAD_FAILLED.toString());
				rpsQuestionPaperService.save(questionPaper);
			}
		}
		return downloadInfo;
	}
	
	private Object getObjectFromJson(String jsonFileName, Class className) throws IOException
	{
		Gson gson = new GsonBuilder().create();
		Object object = gson.fromJson(jsonFileName, className);
		return object;
	}
	
/*	public void downloadQpackFile(Map<String, String> qpId, String assessmentCode)
	{
		//set qpack download to in progress
		LOGGER.info("downloadQpackFile---IN---");
		Boolean isManualUpload = null;
		RpsQuestionPaper questionPaper= rpsQuestionPaperService.getQPaperByAssessmentAndqpCode(assessmentCode,qpId.get(qpCode));
		if(questionPaper!=null)
		{
			questionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.DOWNLOAD_IN_PROGRESS.toString());
			rpsQuestionPaperService.save(questionPaper);
		}
		String downloadInfo=null;
		 String qPackDownloadFile=null;
		try {
			downloadInfo= this.getURIToDownloadQPackPerQPID(qpId, assessmentCode);
			if(downloadInfo!=null)
			{
				
			    QpdRpsExportEntity qpdRpsExportInfo = (QpdRpsExportEntity)(this.getObjectFromJson(downloadInfo, QpdRpsExportEntity.class));
			    String eventCode= qpdRpsExportInfo.getEventCode();
			    int zipfileindex = qpdRpsExportInfo.getDownloadUrl().lastIndexOf("/");
			    String zipFileName = qpdRpsExportInfo.getDownloadUrl().substring(zipfileindex + 1, qpdRpsExportInfo.getDownloadUrl().length());
			    String zipFileNameWithoutExtension = FilenameUtils.removeExtension(zipFileName);
			    if(!new File(APOLLO_HOME+ File.separator + zipFileNameWithoutExtension).exists()) {
			    	new File(APOLLO_HOME + File.separator + zipFileNameWithoutExtension).mkdirs();
			    }
			    qPackDownloadFile = httpClientFactory.downloadFile(qpdRpsExportInfo.getDownloadUrl(), APOLLO_HOME + File.separator + zipFileNameWithoutExtension + File.separator + zipFileName);
			    Date date = new Date();
			    qPackDetailsServices.unZipQPack(FilenameUtils.separatorsToSystem(qPackDownloadFile), date,true, qpdRpsExportInfo, null, eventCode, isManualUpload);
			}
		} catch (RpsException e) {
            LOGGER.error("ERROR WHILE PROCESSING QPACK", e);
		} catch (IOException e) {
			LOGGER.error("ERROR WHILE PROCESSING QPACK", e);
		} catch (HttpException e) {
			 LOGGER.error(e.getMessage());
		} catch (Exception e) {
			LOGGER.error("ERROR WHILE PROCESSING QPACK", e);
		}finally{
			File fileTodelete= new File(qPackDownloadFile);
			if(fileTodelete.exists())
				FileUtils.deleteQuietly(fileTodelete);
		}
		LOGGER.info("downloadQpackFile---OUT---");
	}*/
	
}
