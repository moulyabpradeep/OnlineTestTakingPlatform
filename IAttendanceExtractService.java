package com.merittrac.apollo.rps.services;

import java.io.IOException;
import java.text.ParseException;

import net.lingala.zip4j.exception.ZipException;

import org.springframework.integration.MessageHeaders;

import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.data.entity.RpsPack;
import com.merittrac.apollo.rps.common.RpsException;

public interface IAttendanceExtractService {

	public void extractAttendanceReport(String apolloHome, String filename, MessageHeaders messageHeaders, RpsPack rpsPack) throws ZipException, IOException, ApolloSecurityException, RpsException, ParseException ;
    String attendanceEncpt = "yes";
    String attendanceDirEncpt = "yes";
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
    
//    String rpackStatus = "packStatus";
    
    String CREATED_BY= "SYSTEM";
    String eventCodeHeader= "eventCode";
//    String eventCode="Event1059";
    
    String setCode = "set";
    
    String temp = "_encrypt";
    
    String VERSION_1="1";

    final String NO="N";
    final String YES="Y";
    final String NULL="null";
}
