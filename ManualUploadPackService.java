package com.merittrac.apollo.rps.services;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.service.RpsCustomerService;
import com.merittrac.apollo.data.service.RpsDivisionService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.integration.qpd.serviceactivator.QPPackHandler;
import com.merittrac.apollo.rps.reports.ReportService;
import com.merittrac.apollo.rps.threads.manualupload.ManualUploadThread;
import com.merittrac.apollo.rps.uploadpack.PackUpload;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;



public class ManualUploadPackService implements IManualUploadPackService
{
	
	@Autowired
	ReportService reportService;

	@Autowired
	ManualUploadThread manualUploadThread;
	
	@Autowired
	RpsCustomerService rpsCustomerService;
	
	@Autowired
	RpsDivisionService rpsDivisionService;
	
	@Autowired
	RpsEventService rpsEventService;

    @Autowired
    QPPackHandler qPPackHandler;
    
    @Autowired
    QPackDetailsServices qPackDetailsServices;

    @Autowired
	PackUpload packUpload;
	
	final String ZIP=".zip" ;

//	private String packStatus = "packStatus";
	protected static final Logger LOGGER = LoggerFactory.getLogger(ManualUploadPackService.class);
	
	@Override
	public String validateQPPackInfo(String packFileDownloadPath, String eventcode, String password) 
	{		
/*		Gson gson = new Gson();
		String qpackStatus = "";
		Map<String, String> qpackStatusMap = new HashMap<String, String>();
		Type type = new TypeToken<Map<String, Object>>(){}.getType();
		Map<String, Object> jsonMap = gson.fromJson(qpackJsonInfo, type);*/
		
		Gson gson = new Gson();
		String qpackStatus = "";
		Map<String, String> qpackStatusMap = new HashMap<String, String>();
		
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put(eventCodeKey, eventcode);
		
		if(!jsonMap.containsKey(divisionCodeKey))
		{
			String eventCode = jsonMap.get(eventCodeKey).toString();
			RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
			if(rpsEvent!=null)
			{
				jsonMap.put(divisionCodeKey, rpsEvent.getRpsDivision().getDivisionCode());
				if(!jsonMap.containsKey(customerCodeKey))
				{
					String divisionCode = rpsEvent.getRpsDivision().getDivisionCode();
					RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(divisionCode);
					if(rpsDivision!=null)
						jsonMap.put(customerCodeKey, rpsDivision.getRpsCustomer().getCustomerCode());
					else
					{
						qpackStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
						LOGGER.info("Division Information is not Available in Database for Division Code ::{} " +divisionCode);
						qpackStatus = gson.toJson(qpackStatusMap);
						return qpackStatus;	
					}
				}
			}
			else
			{
				qpackStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
				LOGGER.info("Event Information is not Available in Database for EventCode :: {} ", eventCode);
				qpackStatus = gson.toJson(qpackStatusMap);
				return qpackStatus;	
			}
		}
		qpackStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.SUCCESSFUL.toString());
		qpackStatus = gson.toJson(qpackStatusMap);
		return qpackStatus;
	}

	@Override
	public String validateRPackInfo(String packFileDownloadPath, String eventCode, String password) 
	{
		Gson gson = new Gson();
		String rpackStatus = "";
		Map<String, String> rpackStatusMap = new HashMap<String, String>();
		Map<String, Object> jsonMap = new HashMap<String, Object>();
		jsonMap.put(eventCodeKey, eventCode);
		
		if(!jsonMap.containsKey(divisionCodeKey))
		{
//			String eventCode = jsonMap.get(eventCodeKey).toString();
			RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
			if(rpsEvent!=null)
			{
				jsonMap.put(divisionCodeKey, rpsEvent.getRpsDivision().getDivisionCode());
				if(!jsonMap.containsKey(customerCodeKey))
				{
					String divisionCode = rpsEvent.getRpsDivision().getDivisionCode();
					RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(divisionCode);
					if(rpsDivision!=null)
						jsonMap.put(customerCodeKey, rpsDivision.getRpsCustomer().getCustomerCode());
					else
					{
						rpackStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
						LOGGER.info("Division Information is not Available in Database for Division Code ::{} " +divisionCode);
						rpackStatus = gson.toJson(rpackStatusMap);
						return rpackStatus;
					}
				}				
			}
			else
			{
				rpackStatusMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.UN_SUCCESSFUL.toString());
				LOGGER.info("Event Information is not Available in Database for EventCode :: {} " ,eventCode);
				rpackStatus = gson.toJson(rpackStatusMap);
				return rpackStatus;				
			}
		}
		try
		{
//			rpackStatusMap = manualUploadThread.uploadRPackInfoThread(packFileDownloadPath, jsonMap);
			rpackStatusMap = packUpload.validateRPackInfo(packFileDownloadPath, jsonMap, password);
		}catch(Exception ex)
		{
			LOGGER.info(ex.getMessage());
			LOGGER.info(rpackStatusMap.get(RpsConstants.PACK_STATUS_MANUAL_UPLOAD));
		}
		rpackStatus = gson.toJson(rpackStatusMap);
		return rpackStatus;
	}

		
	@Override
	public String validateBPackInfo(String packFileDownloadPath, String password) 
	{
		Map<String, String> bpackStatusMap = new HashMap<String, String>();
		Gson gson = new Gson();
		try
		{
//			bpackStatusMap = manualUploadThread.uploadBPackInfoThread(packFileDownloadPath);
			bpackStatusMap = packUpload.validateBPackInfo(packFileDownloadPath, password);
		}catch(Exception ex)
		{
			LOGGER.info(ex.getMessage());
			LOGGER.info(bpackStatusMap.get(RpsConstants.PACK_STATUS_MANUAL_UPLOAD));
		}
		String rpackStatus = gson.toJson(bpackStatusMap);
		return rpackStatus;
	}

	@Override
	public String validatePackInfo(String packFileDownloadPath, String packType, String eventCode, String packPassword) 
	{

		String packStatus = "";
		packFileDownloadPath = FilenameUtils.separatorsToSystem(packFileDownloadPath);
		if(packType.equalsIgnoreCase(qppackType))
		{			
			packStatus = this.validateQPPackInfo(packFileDownloadPath, eventCode, packPassword);
		}
		
		if(packType.equalsIgnoreCase(rpackType))
		{
			packStatus = this.validateRPackInfo(packFileDownloadPath, eventCode, packPassword);
		}
		
		if(packType.equalsIgnoreCase(bpackType))
		{
			packStatus = this.validateBPackInfo(packFileDownloadPath, packPassword);
		}
		
		return packStatus;
	}

	@Override
	public String uploadPackInfo(String packFileDownloadPath, String packType, String eventCode, String packPassword, String Comments) throws IOException 
	{

		String packStatus = "";
		int index= packFileDownloadPath.lastIndexOf("_");
		String fileNameWtExtraInfo= packFileDownloadPath.substring(0, index)+ZIP;
		
		FileUtils.copyFile(new File(packFileDownloadPath), new File(fileNameWtExtraInfo));
		FileUtils.deleteQuietly(new File(packFileDownloadPath));
		
		if(packType.equalsIgnoreCase(qppackType))
		{			
			packStatus = this.uploadQPPackInfo(fileNameWtExtraInfo, eventCode, packPassword, Comments);
		}
		
		if(packType.equalsIgnoreCase(rpackType))
		{
			packStatus = this.uploadRPackInfo(fileNameWtExtraInfo, eventCode, packPassword, Comments);
		}
		
		if(packType.equalsIgnoreCase(bpackType))
		{
			packStatus = this.uploadBPackInfo(fileNameWtExtraInfo, packPassword, Comments);
		}
		
		return packStatus;
	}

	@Override
	public String uploadQPPackInfo(String packFileDownloadPath, String eventCode, String password, String comments)
	{
		Gson gson = new Gson();
		Map<String, String> qpackStatusMap = new HashMap<String, String>();
		qpackStatusMap = packUpload.uploadQPPackInfo(packFileDownloadPath, eventCode);
		
		String qpackStatus = gson.toJson(qpackStatusMap);
		return qpackStatus;
	}

	@Override
	public String uploadRPackInfo(String packFileDownloadPath, String eventCode, String password, String comments) 
	{
		Gson gson = new Gson();
		Map<String, String> rpackStatusMap = new HashMap<String, String>();
		Map<String, Object> eventMap = new HashMap<String, Object>();
		eventMap.put(eventCodeKey, eventCode);
		
		if(!eventMap.containsKey(divisionCodeKey))
		{
//			String eventCode = jsonMap.get(eventCodeKey).toString();
			RpsEvent rpsEvent = rpsEventService.findByEventCode(eventCode);
			if(rpsEvent!=null)
			{
				eventMap.put(divisionCodeKey, rpsEvent.getRpsDivision().getDivisionCode());
				if(!eventMap.containsKey(customerCodeKey))
				{
					String divisionCode = rpsEvent.getRpsDivision().getDivisionCode();
					RpsDivision rpsDivision = rpsDivisionService.findByDivisionCode(divisionCode);
					if(rpsDivision!=null)
						eventMap.put(customerCodeKey, rpsDivision.getRpsCustomer().getCustomerCode());
				}				
			}
		}		
		
		rpackStatusMap = packUpload.uploadRPackInfo(packFileDownloadPath, eventMap, password, comments);		
		String rpackStatus = gson.toJson(rpackStatusMap);
		return rpackStatus;
	}

	@Override
	public String uploadBPackInfo(String packFileDownloadPath, String password, String comments) 
	{
		Gson gson = new Gson();
		Map<String, String> bpackStatusMap = new HashMap<String, String>();
		bpackStatusMap = packUpload.uploadBPackInfo(packFileDownloadPath, password, comments);
		
		String bpackStatus = gson.toJson(bpackStatusMap);
		return bpackStatus;
	}

	public String getPackZipContentInfo(String packFileDownloadPath,
			String packType) {
		String zipContentMap = "";
		if(packType!=null && packType.equalsIgnoreCase(bpackType))
		{
			zipContentMap = packUpload.getDmExtBpackZipContent(packFileDownloadPath);
			
		}else if(packType!=null && packType.equalsIgnoreCase(rpackType)){
			zipContentMap = packUpload.getAcsExtPackContent(packFileDownloadPath);
		}
		return zipContentMap;
	}
	
	public String uploadZipForPacks(final String packFileDownloadPath,String packType, final String comments, final String eventCode) {
		
		LOGGER.debug("ManualUploadPackService.uploadZipForPacks() Comments"+packFileDownloadPath);
		Map<String,String> jsonResponse = new HashMap<String,String>();
		final String packFileDownloadPathTemp = FilenameUtils.separatorsToSystem(packFileDownloadPath);
        LOGGER.debug("ManualUploadPackService.uploadZipForPacks() packFileDownloadPathTemp"+packFileDownloadPathTemp);
		String response=null;
		if(eventCode == null || eventCode.equals("0")) {
			jsonResponse.put("packFailure","Unable to upload pack. Please select proper CUSTOMER, DIVISION, EVENT and retry");
			response = (new Gson()).toJson(jsonResponse);
			return response;
		}
		try {
			if(packType!=null && packType.equalsIgnoreCase(bpackType))
			{
				response = packUpload.uploadDmExtBpackZip(packFileDownloadPath,comments);
			}else if(packType!=null && packType.equalsIgnoreCase(rpackType))
			{
				response = packUpload.uploadAcsExtRpackZip(packFileDownloadPath,comments);
				Map<String,String>responseMap=new Gson().fromJson(response, new TypeToken<HashMap<String, String>>() {
				}.getType());
				reportService.computeAllResultsForBatchAcs(responseMap.get("BATCH_CODE"),responseMap.get("ACS_SERVER_CODE"));
			}else if(packType!=null && packType.equalsIgnoreCase(qppackType))
			{
                if (ZipUtility.isValidZipFile(FilenameUtils.separatorsToSystem(packFileDownloadPath))) {

                    new Thread(new Runnable() {
                        public void run() {
                            try {
                            	qPackDetailsServices.unZipQPack(packFileDownloadPathTemp, eventCode, false, comments);
                                //qPPackHandler.uploadQPPackZip(eventCode, packFileDownloadPathTemp, comments);
                            } catch (IOException e) {
                                LOGGER.error("IOException Uploading QPPack ", e);
                            } catch (ZipException e) {
                                LOGGER.error("ZipException Uploading QPPack ", e);
                            } catch (ApolloSecurityException e) {
                                LOGGER.error("ApolloSecurityException Uploading QPPack ", e);
                            } catch (Exception e) {
                            	LOGGER.error("Exception Uploading QPPack ", e);
							}
                        }
                    }).start();
                    jsonResponse.put("packStatus","Success");
                    response = (new Gson()).toJson(jsonResponse);
                }
                else {
                    jsonResponse.put("packStatus","Failed");
                    response = (new Gson()).toJson(jsonResponse);
                }

			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			jsonResponse.put("packStatus","Failed");
			response = (new Gson()).toJson(jsonResponse);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			jsonResponse.put("packStatus","Failed");
			response = (new Gson()).toJson(jsonResponse);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			jsonResponse.put("packStatus","Failed");
			response = (new Gson()).toJson(jsonResponse);
		}return response;

	}


}

