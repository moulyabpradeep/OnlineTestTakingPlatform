package com.merittrac.apollo.rps.services;

import java.util.List;

import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.rps.common.RpsException;



public interface IRpsUIDataService {
	String getAllCustomers(List<String> eventList,boolean isSuperUser);
	String getAllDivisionsPerCustomer(String customerCode,List<String> eventList,boolean isSuperUser);
	String getAllCustomers();
	String getAllDivisionsByCustomer(String customerCode);
	String getAllEventsPerDivision(String divisionCode,List<String> eventList,boolean isSuperUser);
	String getAllEventsByDivision(String divisionCode);
	//String getAllPackStatusInDateRange(String eventCode,String startDate, String endDate, Integer pageNo, Integer size);
	String getAllQPackStatusInDateRange(String eventCode,String startDate, String endDate, Integer pageNo, Integer size);
	//void  getQPaperforQPID(String qpId, String assessmentCode);
	String getAllAnswerKeysStatus(String eventCode, String startDate, String endDate,Integer pageNo, Integer size, String sortOrder, String sortColumnName);
	//String getAllProcessResultsStatus(String eventCode, String startDate, String endDate);
	void computeResultForTheBatchAndQp(String argsJson) throws RpsException;
	
	String getAllQPackStatusInDateRange(List<RpsEvent> eventCode,String startDate, String endDate, Integer pageNo, Integer size, String sortOrder, String sortColumnName);
	String getAllAnswerKeysStatus(List<RpsEvent> eventCode, String startDate, String endDate, Integer pageNo, Integer size, String sortOrder, String sortColumnName);
	String getAllProcessResultsStatus(List<RpsEvent> eventCode, String startDate, String endDate);
	String getAllPackStatusInDateRange(List<RpsEvent> eventCode,String startDate, String endDate, Integer pageNo, Integer size, String sortingOrder, String columnName);
	
	
	String batchCode = "batchCode";
	String batchName = "batchName";
	String acsServerId = "acsServerId";
	String acsServerName = "acsServerName";
	String ASC = "ASC";
	String DESC = "DESC";
	String questionPaperName=  "qpCode";
	String subjectCode="subjectCode";
	String subjectName = "subjectName";
	String venue = "venue";
	String assessmentName = "assessmentName";
	String setCode = "setCode";
	
	enum dashBoardColumnName
	{
		batchCode("rpsBatch.batchCode"),batchName("rpsBatch.batchName"),acsServerId("rpsAcsServer.acsServerId"),acsServerName("rpsAcsServer.acsServerName");
		
		private final String columName; 
		
		dashBoardColumnName(String columnName)
		{
			this.columName=  columnName;
		}
		
		public String getColumnName()
		{
			return columName;
		}
	}
	
	enum answerKeyStatusColumn
	{
		subjectCode("rpsAssessment.assessmentCode"), subjectName("rpsAssessment.assessmentName");
		
		private final String columnName;
		answerKeyStatusColumn(String columnName)
		{
			this.columnName=  columnName;
		}
		public String getColumnName()
		{
			return columnName;
		}
	}
	
	enum processResultColumn
	{
		batchCode("rpsBatchAcsAssociation.rpsBatch.batchCode"),batchName("rpsBatchAcsAssociation.rpsBatch.batchCode"),acsServerName("rpsBatchAcsAssociation.rpsAcsServer.acsServerName"),venue("rpsBatchAcsAssociation.rpsAcsServer.rpsVenue"),assessmentName("rpsAssessment.assessmentName"),setCode("rpsQuestionPaper.qpCode");

		private final String columnName;		
		processResultColumn(String columnName)
		{
			this.columnName = columnName;
		}
		public String getColumnName()
		{
			return columnName;
		}
	}

}
