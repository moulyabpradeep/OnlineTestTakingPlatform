package com.merittrac.apollo.rps.services;

import java.io.IOException;

public interface IManualUploadPackService 
{
	public String validateQPPackInfo(String packFileDownloadPath, String eventCode, String password);
	
	public String validateRPackInfo(String packFileDownloadPath, String eventCode, String password);
	
	public String validateBPackInfo(String packFileDownloadPath, String password);
	
	public String uploadQPPackInfo(String packFileDownloadPath, String eventCode, String password, String comments);
	
	public String uploadRPackInfo(String packFileDownloadPath, String eventCode, String password, String comments);
	
	public String uploadBPackInfo(String packFileDownloadPath, String password, String comments);
	
	public String validatePackInfo(String packFileDownloadPath, String packType, String eventCode, String packPassword);
	
	public String uploadPackInfo(String packFileDownloadPath, String packType, String eventCode, String packPassword, String  Comments) throws IOException;
	
	public String eventCodeKey = "eventCode";
	public String customerCodeKey = "customerCode";
	public String divisionCodeKey = "divisionCode";
	
	public String bpackType = "BPACK";
	public String qppackType = "QPPACK";
	public String rpackType = "RPACK";
}
