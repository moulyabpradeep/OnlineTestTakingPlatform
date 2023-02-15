package com.merittrac.apollo.rps.services;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;

import com.merittrac.apollo.common.JMSRequeException;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.jms.rabbitmq.util.FileObject;
import com.merittrac.apollo.rps.common.PackAlreadyProcessedException;
import com.merittrac.apollo.rps.common.RPSAuthenticationException;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.common.RpsRpackException;

import net.lingala.zip4j.exception.ZipException;

public interface IRPackExtractService 
{
	public boolean extractRPack(final String localDownloadPath, final Date downloadTime,
			final String filename, final MessageHeaders messageHeaders, RpsPack rpsPack, boolean isManualUpload,
			final Message<FileObject> message)
			throws RpsException, ZipException, IOException, JMSRequeException, ApolloSecurityException, ParseException,
			RpsRpackException, PackAlreadyProcessedException, RPSAuthenticationException;
    String deltaPackInfo = "yes";
    String rpackEncrypt = "yes";
    String batchCodeName = "batchCode";
    String acsCode = "assessmentServerCode";
    String versionNo = "versionNo";
    String packCode = "packCode";
     String packType = "packType";
	final String customerCode="customerCode";
	final String divisionCode="divisionCode";
	final String eventCode="eventCode";
    
    String responses_file = "responses.json";
    String attendance_file = "attendance.json";
    String auditLogFolderName = "logs/audit_admin";
    String candLofFolderName = "logs/audit_tp";
    String incidentAudit_file = "incident_report.log";
    
//    String rpackStatus = "packStatus";
    
    String CREATED_BY= "SYSTEM";
    String eventCodeHeader= "eventCode";
//    String eventCode="Event1059";
    
    String setCode = "set";
    
    String temp = "_encrypt";
    
    String VERSION_1="1";
}
