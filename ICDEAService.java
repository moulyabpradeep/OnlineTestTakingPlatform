package com.merittrac.apollo.acs.services;

import java.util.List;

import com.merittrac.apollo.acs.dataobject.CandidateIdDO;
import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsCustomer;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.cdb.entity.CandidateStatus;
import com.merittrac.apollo.cdb.entity.PackNotificationEntity;
import com.merittrac.apollo.common.entities.acs.AttendanceReportEntity;
import com.merittrac.apollo.common.entities.acs.CandidateIdentifiersEntity;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface ICDEAService {

	/**
	 * updateEventDetails API is used to update the event details object.
	 * 
	 * @param eventDetails
	 *            indicates the eventDetails object
	 * @throws GenericDataModelException
	 */
	public void updateEventDetails(AcsEvent eventDetails) throws GenericDataModelException;

	/**
	 * The updateAssessmentDetails API is used to update the assessmentDetails object.
	 * 
	 * @param assessmentDetails
	 *            indicates assessmentDetails object.
	 * @return boolean value
	 * @throws GenericDataModelException
	 */
	public AcsAssessment updateAssessmentDetails(AcsAssessment assessmentDetails) throws GenericDataModelException;

	/**
	 * The getBussinessRulesJSONByAssessmentId API is used to get the list of business rules for a specified
	 * asessmentId.
	 * 
	 * @param asessmentId
	 *            indicates unique Id which identifies an assessment.
	 * @return the BussinessRulesJSON as List.
	 * @throws GenericDataModelException
	 */
	public List<String> getBussinessRulesJSONByAssessmentIdAndBatchId(String assessmentCode, String batchCode)
			throws GenericDataModelException;

	/**
	 * The getLayoutRulesJSONByAssessmentId API is used to get the list of layout rules for a specified asessmentId.
	 * 
	 * @param asessmentId
	 *            indicates unique Id which identifies an assessment.
	 * @return LayoutRulesJSON as List.
	 * @throws GenericDataModelException
	 */
	public AcsBussinessRulesAndLayoutRules getBussinessRulesAndLayoutRulesByAssessmentCodeAndBatchCode(
			String assessmentCode, String batchCode) throws GenericDataModelException;

	/**
	 * The getBRRulePropertybyAssessmentId API is used to get the BRRuleProperty for a specified asessmentId and
	 * propertyName.
	 * 
	 * @param asessmentId
	 *            indicates unique Id which identifies an assessment.
	 * @param propertyName
	 * @return {@link Object}
	 * @throws GenericDataModelException
	 */
	public Object
			getBRRulePropertybyAssessmentIdAndBatchId(String assessmentCode, String batchCode, String propertyName)
					throws GenericDataModelException;

	/**
	 * The getAssessmentDetailsbyCandIDAndBatchID API is used to get the AssessmentDetailsTO object.
	 * 
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @param bid
	 *            indicates unique Id which identifies batchId.
	 * @return AssessmentDetailsTO as object.
	 * @throws GenericDataModelException
	 */
	public AcsAssessment getAssessmentDetailsbyCandIDAndBatchID(int candId, String batchCode)
			throws GenericDataModelException;

	/**
	 * The getAssessmentIdbyCandIDAndBatchID API is used to get the Assessment id for a specified candId and bid.
	 * 
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @param bid
	 *            indicates unique Id which identifies batchId.
	 * @return assessment Id as Integer.
	 * @throws GenericDataModelException
	 */
	public String getAssessmentIdbyCandIDAndBatchID(int candId, String batchCode) throws GenericDataModelException;

	/**
	 * The getAssessmenNamebyCandIDAndBatchID API is used to get the assessment name for a specified candId and bid.
	 * 
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @param bid
	 *            indicates unique Id which identifies batchId.
	 * @return assessment name as String.
	 * @throws GenericDataModelException
	 */

	public String getAssessmenNamebyCandIDAndBatchID(int candId, String batchCode) throws GenericDataModelException;

	/**
	 * The getBatchCandAssociationIdbyCandIDBatchIDAndAssessmentID API is used to get the BatchCandAssociationId for a
	 * specified candId,bid and aid.
	 * 
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @param bid
	 *            indicates unique Id which identifies batchId.
	 * @param aid
	 *            indicates unique Id which identifies assessment.
	 * @return BatchCandAssociationId as Integer.
	 * @throws GenericDataModelException
	 */
	public int getBatchCandAssociationIdbyCandIDBatchIDAndAssessmentID(int candId, String batchCode,
			String assessmentCode) throws GenericDataModelException;

	/**
	 * The saveBrAndLrRules API is used to save the bussinessRulesAndLayoutRules for a specified
	 * bussinessRulesAndLayoutRules object.
	 * 
	 * @param bussinessRulesAndLayoutRules
	 *            indicates bussinessRulesAndLayoutRules object.
	 * @return boolean value.
	 * @throws GenericDataModelException
	 */
	public boolean saveBrAndLrRules(AcsBussinessRulesAndLayoutRules bussinessRulesAndLayoutRules)
			throws GenericDataModelException;

	/**
	 * The getAssessmentCountbyCDEANames API is used to get the AssessmentCount for a specified
	 * customerName,divisionName,eventName and assessmentName.
	 * 
	 * @param customerName
	 *            indicates the customer name.
	 * @param divisionName
	 *            indicates the division name.
	 * @param eventName
	 *            indicates the event name.
	 * @param assessmentName
	 *            indicates the assessment name.
	 * @return AssessmentCount as integer.
	 * @throws GenericDataModelException
	 */
	// public int getAssessmentCountbyCDEANames(String customerName, String divisionName, String eventName,
	// String assessmentName) throws GenericDataModelException;

	/**
	 * The getAssessmentCountbyCDEANames API is used to save the assessmentDetails object.
	 * 
	 * @param assessmentDetails
	 *            indicates assessment Details
	 * @return boolean value.
	 * @throws GenericDataModelException
	 */
	public boolean saveAssessmentDetails(AcsAssessment assessmentDetails) throws GenericDataModelException;

	/**
	 * The getBrAndLrRulesbyCDEAnames API is used to get the BrAndLrRules for a specified
	 * customerName,divisionName,eventName and assessmentName.
	 * 
	 * @param customerName
	 *            indicates the customer name.
	 * @param divisionName
	 *            indicates the division name.
	 * @param eventName
	 *            indicates the event name.
	 * @param assessmentName
	 *            indicates the assessment name.
	 * @return boolean value.
	 * @throws GenericDataModelException
	 */
	public List<AcsBussinessRulesAndLayoutRules> getBrAndLrRulesbyCDEAnames(String customerName, String divisionName,
			String eventName, String assessmentName) throws GenericDataModelException;

	/**
	 * The deleteBrAndLrRules API is used to delete BrAndLrRules for a specified blId and assessmentId.
	 * 
	 * @param blId
	 *            indicates brAndLrRulesId.
	 * @param assessmentId
	 *            (auto generated) indicates unique Id which identifies Assessment.
	 * @return boolean value.
	 * @throws GenericDataModelException
	 */
	public boolean deleteBrAndLrRules(String batchCode, String assessmentCode) throws GenericDataModelException;

	/**
	 * The getPropertyValueFromJson API used to get the PropertyValue from Json for a specified jsonString and
	 * propertyName.
	 * 
	 * @param jsonString
	 *            indicates Json string
	 * @param propertyName
	 *            indicates property Name
	 * @return Object
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Object getPropertyValueFromJson(String jsonString, String propertyName);

	/**
	 * The getCandidateAttendence API is used to get the list of candidates attendance for a specified candidateIds and
	 * batchCode.
	 * 
	 * @param candidateIds
	 *            indicates list of CandidateIdDO object
	 * @param batchCode
	 *            indicates unique code which identifies batchCode.
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AttendanceReportEntity> getCandidateAttendence(List<CandidateIdDO> candidateIds,String batchCode)
			throws GenericDataModelException;

	/**
	 * The getCustomerDetailsByAssessmentID API is used to get the CustomerDetailsTO object for a specified assessmentId
	 * 
	 * @param assessmentId
	 *            indicates unique Id which identifies assessmentId.
	 * @return CustomerDetailsTO object.
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsCustomer getCustomerDetailsByAssessmentCode(String assessmentCode) throws GenericDataModelException;

	/**
	 * The getCustomerDetailsLogoByAssessmentID API is used to get the CustomerDetailsLogo for a specified assessmentId.
	 * 
	 * @param assessmentId
	 *            indicates unique Id which identifies assessmentId.
	 * @return CustomerDetailsLogo as String.
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public String getCustomerDetailsLogoByAssessmentID(String assessmentCode) throws GenericDataModelException;

	/**
	 * The getCandidateResponseCount API is used to get the responses count for each candidate.
	 * 
	 * @param batchId
	 *            indicates unique Id which identifies batchId
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @return responses count as long
	 * @throws GenericDataModelException
	 */
	public Long getCandidateResponseCount(String batchId, int candId) throws GenericDataModelException;

	/**
	 * The getBatchCodesListByPackID API is used to get the list of batches based upon the input provided as packId
	 * 
	 * @param packID
	 *            indicates unique Id which identifies pack.
	 * @return list of batchCodes
	 * @throws GenericDataModelException
	 */
	public List<String> getBatchCodesListByPackID(String packID) throws GenericDataModelException;

	/**
	 * The getAssessmentCodebyCandIDAndBatchID API is used to get the Assessment code based upon the input provided as
	 * candID and batchID.
	 * 
	 * @param candID
	 *            indicates unique Id which identifies candidate.
	 * @param batchID
	 *            indicates unique Id which identifies batchId
	 * @return Assessment code value as String
	 * @throws GenericDataModelException
	 */
	public String getAssessmentCodebyCandIDAndBatchID(int candID, String batchID) throws GenericDataModelException;

	/**
	 * The getPackStatus API is used to generate the status of the packs based on the input provided packId.
	 * 
	 * @param packId
	 *            indicates unique id which identifies the pack.
	 * @return PacksStatusEnum value based on status.
	 * @throws GenericDataModelException
	 */

	public PacksStatusEnum getPackStatus(String packId) throws GenericDataModelException;

	/**
	 * The getACSServerCode API is used to get the ACS Server code
	 * 
	 * @return the ACS Server code value as string
	 * @throws GenericDataModelException
	 */
	public String getACSServerCode() throws GenericDataModelException;

	/**
	 * The getPackNotificationData API is used to get the packs updated status data for the given packID
	 * 
	 * @param packId
	 *            indicates unique Id which identifies pack.
	 * @return PackNotificationEntity object
	 * @throws GenericDataModelException
	 */
	public PackNotificationEntity getPackNotificationData(String packId) throws GenericDataModelException;

	/**
	 * The getCandidateLiveStatusData API is used to get the updated candidate status data for the given candId and
	 * batchId.
	 * 
	 * @param batchId
	 *            indicates unique Id which identifies batchId
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @return CandidateStatus object
	 * @throws GenericDataModelException
	 */
	public CandidateStatus getCandidateLiveStatusData(int candId) throws GenericDataModelException;

	/**
	 * The getCandidateIDList API is to get list of all candidate Identifiers for the given candId
	 * 
	 * @param candId
	 *            indicates unique Id which identifies candidate.
	 * @return CandidateIdentifiersEntity object.
	 * @throws GenericDataModelException
	 */
	public CandidateIdentifiersEntity getCandidateIDList(int candId) throws GenericDataModelException;

	/**
	 * The getAssessmentDetailsByAssessmentCode API is used to get the Assessment details for given assessmentId.
	 * 
	 * @param assessmentCode
	 *            (auto generated) indicates unique Id which identifies Assessment.
	 * @return AssessmentDetailsTO object.
	 * @throws GenericDataModelException
	 */
	public AcsAssessment getAssessmentDetailsByAssessmentCode(String assessmentCode) throws GenericDataModelException;

	/**
	 * The getAssessmentIdByAssessmentCode API is used to get the Assessment Id for given assessmentCode.
	 * 
	 * @param assessmentCode
	 *            indicates unique Code which identifies Assessment.
	 * @return assessmentId value as Integer.
	 * @throws GenericDataModelException
	 */
	public int getAssessmentIdByAssessmentCode(String assessmentCode) throws GenericDataModelException;

	public AcsEvent getEventDetailsByAssessmentID(String assessmentCode) throws GenericDataModelException;

	public List<String> getAllEventCodes() throws GenericDataModelException;
}
