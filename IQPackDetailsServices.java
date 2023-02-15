package com.merittrac.apollo.rps.services;

import java.util.Map;

/**
 * @author Amar_K
 *
 */
public interface IQPackDetailsServices 
{
	public Map<String, String> unZipQPack(String packFileDownloadPath, String eventCode, Boolean isManualUpload, String comments) throws Exception;
	String jsonExtn = ".json";
	String customerCode = "customerCode";
	String divisionCode = "divisionCode";
	String eventCode = "eventCode";
	String assessmentCode = "assessmentCode";
	String qpMapName = "qpIdName";
//	String qppackStatus = "packStatus";
	
}
