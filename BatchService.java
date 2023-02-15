package com.merittrac.apollo.acs.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.SQLQuery;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.BatchCompletionStatusEnum;
import com.merittrac.apollo.acs.constants.CDBNotificationTypesEnum;
import com.merittrac.apollo.acs.dataobject.BatchCompletionInfo;
import com.merittrac.apollo.acs.dataobject.BatchInformationDO;
import com.merittrac.apollo.acs.dataobject.PaginationDataDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsCdbFailureNotifications;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.entities.AcsReportDetails;
import com.merittrac.apollo.acs.entities.AcsTpExitSeq;
import com.merittrac.apollo.acs.exception.BatchNotFoundException;
import com.merittrac.apollo.acs.quartz.jobs.BatchCompletionJob;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.cdb.entity.PackActivatedStatus;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class BatchService extends BasicService implements IBatchService {
	public final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static BatchService batchService = null;

	/**
	 * Using double check singleton pattern
	 * 
	 * @return instance of batch service
	 * 
	 * @since Apollo v2.0
	 */
	public static final BatchService getInstance() {
		if (batchService == null) {
			synchronized (BatchService.class) {
				if (batchService == null) {
					batchService = new BatchService();
				}
			}
		}
		return batchService;
	}

	/**
	 * hiding constructor because of singleton
	 */
	private BatchService() {
		// Not Used
	}

	@Override
	public boolean setBatchDetails(AcsBatch batchDetails) throws GenericDataModelException {
		session.persist(batchDetails);
		return true;
	}

	@Override
	public AcsBatch updateBatchDetails(AcsBatch batchDetails, boolean isMerge, List<String> packIds)
			throws GenericDataModelException {
		// refresh the child objects so that data is not stale
		// batchDetails = this.refreshPackDetails(batchDetails, packIds);

		if (isMerge) {
			batchDetails = (AcsBatch) session.merge(batchDetails);
		}
		return batchDetails;
	}

	public AcsBatch refreshPackDetails(AcsBatch batchDetails, List<String> packIds) throws GenericDataModelException {
		// refresh the child objects so that data is not stale
		/*
		 * if (batchDetails != null && batchDetails.get != null && !batchDetails.getPackDetails().isEmpty()) { Session s
		 * = session.getSession(); for (Iterator iterator = batchDetails.getPackDetails().iterator();
		 * iterator.hasNext();) { AcsPacks packDetails = (AcsPacks) iterator.next(); if (packIds != null &&
		 * !packIds.isEmpty() && packIds.contains(packDetails.getPackIdentifier())) { continue; } //
		 * logger.info("packDetails before refresh = {} and batchCode = {}", // packDetails, //
		 * batchDetails.getBatchCode()); s.refresh(packDetails); //
		 * logger.info("packDetails after refresh = {} and batchCode = {}", // packDetails, //
		 * batchDetails.getBatchCode()); } s.close(); }
		 */
		return batchDetails;
	}

	@Override
	public AcsBatch getBatchDetails(String customerName, String divisionName, String eventName, String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CUSTOMER_NAME, customerName);
		params.put(ACSConstants.DIVISION_NAME, divisionName);
		params.put(ACSConstants.EVENT_NAME, eventName);
		params.put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);

		// String query =
		// "SELECT b FROM CustomerDetailsTO c join c.divisiDetailsTO d join d.eventdetails e join e.batchDetails b WHERE c.customerName=(:customerName) and d.divisionName=(:divisionName) and e.eventName=(:eventName) and b.batchCode=(:batchCode)";
		String query = ACSQueryConstants.QUERY_FETCH_BATCH_DETAILS;

		AcsBatch batchDetails = (AcsBatch) session.getByQuery(query, params);
		return batchDetails;
	}

	@Override
	public List<AcsBatch> getBatchDetailsList(List<String> batchCodesList) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODES_CONST, batchCodesList);

		// String query =
		// "FROM BatchDetailsTO b WHERE b.batchCode IN (:batchCodesList)";
		String query = ACSQueryConstants.QUERY_FETCH_BATCH_DETAILS_LIST;
		List<AcsBatch> batchDetailsList = session.getResultAsListByQuery(query, params, 0);
		return batchDetailsList;
	}

	@Override
	public int getBatchDetailsCount(String customerName, String divisionName, String eventName, String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CUSTOMER_NAME, customerName);
		params.put(ACSConstants.DIVISION_NAME, divisionName);
		params.put(ACSConstants.EVENT_NAME, eventName);
		params.put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);

		// String query =
		// "FROM CustomerDetailsTO c join c.divisiDetailsTO d join d.eventdetails e join e.batchDetails b  WHERE c.customerName=(:customerName) and d.divisionName=(:divisionName) and e.eventName=(:eventName) and b.batchCode=(:batchCode)";
		String query = ACSQueryConstants.QUERY_FETCH_BATCH_DETAILS_COUNT;
		int count = session.getResultCountByQuery(query, params);
		return count;
	}

	@Override
	public PaginationDataDO getscheduledBatchData(int customerId, int divisionId, int eventId, Calendar fromDate,
			Calendar toDate, int startIndex, int count) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.CUSTOMER_ID, customerId);
		params.put(ACSConstants.DIVISION_ID, divisionId);
		params.put(ACSConstants.EVENT_ID, eventId);

		// String query =
		// "SELECT b.batchDate as batchDate,b.batchStartTime as batchStartTime,b.batchEndTime as batchEndTime,b.candidateCount as candidateCount,p.qpackStatus as qpack,p.mpackStatus as mpack,p.apackStatus as apack,p.bpackStatus as bpack,p.rpackStatus as rpack from CustomerDetailsTO c join c.divisiDetailsTO d join d.eventdetails e join e.batchDetails b join b.packsStatusDetailsTO p  WHERE c.customerid=(:customerId) and d.divisionId=(:divisionId) and e.eid=(:eventId)";
		String query = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO;
		String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_COUNT;
		if (fromDate != null && toDate != null) {
			params.put(ACSConstants.FROM_DATE, fromDate);
			params.put(ACSConstants.TO_DATE, toDate);
			query = query + " and b.batchDate >= (:fromDate) and b.batchDate <= (:toDate)";
			resultsCountQuery = resultsCountQuery + " and b.batchDate >= (:fromDate) and b.batchDate <= (:toDate)";
		}
		query = query + " order by b.batchStartTime asc";
		List<Object> scheduledBatchDataList = new ArrayList<Object>();
		List<AcsBatch> batchDetailsList = session.getResultAsListByQuery(query, params, startIndex, count);
		long totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
		PaginationDataDO paginationData = new PaginationDataDO(totalResultsCount, scheduledBatchDataList);
		return paginationData;
	}

	@Override
	public int getBatchIdbyBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);

		// String query =
		// "select bId from BatchDetailsTO where batchCode = (:batchCode)";
		String query = ACSQueryConstants.QUERY_BATCHDETAILS_FETCH_BATCHID_BY_BATCHCODE;

		Integer result = (Integer) session.getByQuery(query, params);
		if (result == null)
			return 0;
		else
			return result;
	}

	@Override
	public List<String> getBatchIdsbyTimeInstance() throws GenericDataModelException {
		String propValue = null;
		int preTestBufferTime = ACSConstants.DEFAULT_PRE_TEST_START_ALLOWED_LOGIN_TIME;
		propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PRE_TEST_START_ALLOWED_LOGIN_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			preTestBufferTime = Integer.parseInt(propValue);
		}

		int postTestBufferTime = ACSConstants.DEFAULT_POST_TEST_BUFFER_TIME;
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.POST_TEST_BUFFER_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			postTestBufferTime = Integer.parseInt(propValue);
		}

		Calendar currentDateTime = Calendar.getInstance();
		// currentDateTime.set(2013, Calendar.JANUARY, 4, 10, 58);

		SQLQuery sqlQuery =
				session.getSession().createSQLQuery(ACSQueryConstants.QUERY_BATCHDETAILS_FETCH_BATCHID_BY_TIMEINSTANCE);
		sqlQuery.setInteger(ACSConstants.PRE_TEST_BUFFER_TIME, preTestBufferTime);
		sqlQuery.setInteger(ACSConstants.POST_TEST_BUFFER_TIME_CONST, postTestBufferTime);
		sqlQuery.setCalendar(ACSConstants.DATE_TIME, currentDateTime);

		List<String> result = sqlQuery.list();
		session.getSession().close();
		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else
			return result;
	}

	public List<AcsBatch> getBatchesbyTimeInstance() throws GenericDataModelException {

		String propValue = null;
		int preTestBufferTime = ACSConstants.DEFAULT_PRE_TEST_START_ALLOWED_LOGIN_TIME;
		propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PRE_TEST_START_ALLOWED_LOGIN_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			preTestBufferTime = Integer.parseInt(propValue);
		}

		int postTestBufferTime = ACSConstants.DEFAULT_POST_TEST_BUFFER_TIME;
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.POST_TEST_BUFFER_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			postTestBufferTime = Integer.parseInt(propValue);
		}

		Calendar currentDateTime = Calendar.getInstance();
		// currentDateTime.set(2013, Calendar.JANUARY, 4, 10, 58);

		SQLQuery sqlQuery =
				session.getSession().createSQLQuery(
						ACSQueryConstants.QUERY_BATCHDETAILS_FETCH_BATCHCODES_BY_TIMEINSTANCE);
		sqlQuery.setInteger(ACSConstants.PRE_TEST_BUFFER_TIME, preTestBufferTime);
		sqlQuery.setInteger(ACSConstants.POST_TEST_BUFFER_TIME_CONST, postTestBufferTime);
		sqlQuery.setCalendar(ACSConstants.DATE_TIME, currentDateTime);
		sqlQuery.addEntity(AcsBatch.class);
		List<AcsBatch> result = sqlQuery.list();
		session.getSession().close();
		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else
			return result;
	}

	/**
	 * Scope of this method is to test whether a batch exists at current time instance, if exists it checks whether this
	 * candidate is authorized for that batch or not if authorized returns batchCode if not returns 0.
	 */

	public AcsBatch getBatchDetailsByBatchCode(String batchCode) throws GenericDataModelException {
		AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
		if (batchDetails != null && batchDetails.getIsBatchCancelled())
			return null;
		return batchDetails;
	}

	/*
	 * Delete all the batch candidate Question Paper Allocation for a given batch code
	 */
	@Override
	public boolean deleteQPAllocationByBatchCode(String batchCode) throws GenericDataModelException,
			BatchNotFoundException {
		int batchID = getBatchIdbyBatchCode(batchCode);
		if (batchID == 0)
			throw new BatchNotFoundException(ACSExceptionConstants.BATCH_NOT_FOUND,
					"No batch exists with the specified batch code :: " + batchCode);
		String query = "delete from BatchCandidateAssociationTO WHERE batchId = (:batchId)";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_ID, batchID);

		if (session.updateByQuery(query, params) != 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Will return the exit sequence based on the current batch detected.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	@Override
	public String getExitSequence() throws GenericDataModelException {
		List<AcsBatch> batch = getBatchesbyTimeInstance();
		if (batch != null && batch.size() > 0) {
			AcsEvent eventDetails = getEventDetailsByBatchCode(batch.get(0).getBatchCode());
			HashMap<String, Object> params = new HashMap<String, Object>();
			params.put("eventCode", eventDetails.getEventCode());
			AcsTpExitSeq exitseq =
					(AcsTpExitSeq) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EXIT_SEQ_BY_EVENT_CODE, params);
			if (exitseq != null)
				return exitseq.getExitSequence();
		}
		String propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.EXIT_SEQUENCE);
		if (propValue != null) {
			return propValue;
		}
		return ACSConstants.DEFAULT_EXIT_SEQUENCE;
	}

	public AcsEvent getEventDetailsByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		AcsEvent eventDetails = (AcsEvent) session.getByQuery(ACSQueryConstants.QUERY_FETCH_EVENT_BY_BATCHID, params);
		return eventDetails;
	}

	public List<AcsBatch> getBatchDetailsByPackId(String packId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);
		logger.debug("getBatchDetailsByPackId for {}", packId);
		List<AcsBatch> batchDetails =
				(List<AcsBatch>) session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BATCH_FOR_PACK, params);
		logger.debug("BatchDetailsTO is {}", batchDetails);
		return batchDetails;
	}

	public List<String> getCurrentDateBatchIds(Calendar currentDate) throws GenericDataModelException {
		Calendar date = (Calendar) currentDate.clone();

		date.set(Calendar.HOUR_OF_DAY, 0);
		date.set(Calendar.MINUTE, 0);
		date.set(Calendar.SECOND, 0);
		date.set(Calendar.MILLISECOND, 0);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("currentDate", date);
		params.put("isBatchCancelled", false);

		List<String> batchCodes = session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BATCH_BY_CURRENTDATE, params);
		if (batchCodes.equals(Collections.<Object> emptyList()))
			return null;
		return batchCodes;
	}

	public AcsBatch getBatchDetailsByBatchId(String batchCode) throws GenericDataModelException {
		AcsBatch batchDetails = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
		return batchDetails;
	}

	public int disableCancelledBatches(List<String> batchCodes) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODES_CONST, batchCodes);
		params.put("isBatchCancelled", true);
		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_BATCH_FOR_CANCELLATION, params);
		return count;
	}

	public boolean updateBatchEndTime(Calendar batchEndTime, int batchId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_END_TIME, batchEndTime);
		params.put(ACSConstants.BATCH_ID, batchId);
		params.put("isBatchCancelled", false);

		String query =
				"UPDATE BatchDetailsTO set batchEndTime=(:BatchEndTime) WHERE batchId = (:batchId) and isBatchCancelled = (:isBatchCancelled)";
		int count = session.updateByQuery(query, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	public boolean updateBatchStartTime(Calendar batchstartTime, String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchstartTime", batchstartTime);
		params.put(ACSConstants.BATCH_CODE, batchCode);

		String query =
				"UPDATE AcsBatch set postponedBatchStartTime=(:batchstartTime) WHERE batchCode = (:batchCode) ";
		int count = session.updateByQuery(query, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	public boolean updateBatchLateLoginTimeAndStartTime(Calendar batchstartTime, Calendar batchLateloginTime, String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchLateloginTime", batchLateloginTime);
		params.put("batchstartTime", batchstartTime);
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("value", true);
		String query =
				"UPDATE AcsBatch set extendedLateLoginTime=(:batchLateloginTime), isLateLoginExtended=(:value) , "
						+ " postponedBatchStartTime=(:batchstartTime) "
						+ " WHERE batchCode = (:batchCode) ";
		int count = session.updateByQuery(query, params);
		if (count == 0) {
			return false;
		}
		return true;
	}

	public AcsBatch getBatchDetailsbyBatchCodeAndEndTime(String batchCode, Calendar endTime)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSQueryConstants.PARAM_BATCH_CODE, batchCode);
		params.put(ACSConstants.BATCH_END_TIME, endTime);
		String query = ACSQueryConstants.QUERY_BATCHDETAILS_FETCH_BATCHID_BY_BATCHCODE_AND_END_TIME;
		AcsBatch batchDetails = (AcsBatch) session.getByQuery(query, params);
		return batchDetails;
	}

	public List<Integer> getBatchIdsListByBatchCodes(List<String> batchCodesList) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODES_CONST, batchCodesList);

		// String query =
		// "FROM BatchDetailsTO b WHERE b.batchCode IN (:batchCodesList)";
		String query = ACSQueryConstants.QUERY_FETCH_BATCH_IDS_LIST_BY_BATCH_CODES;

		List<Integer> batchDetailsList = session.getResultAsListByQuery(query, params, 0);
		return batchDetailsList;
	}

	public List<String> getBatchCodesbyBatchIds(List<Integer> batchIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_IDS, batchIds);

		String query = "SELECT batchCode FROM BatchDetailsTO WHERE batchId IN (:batchIds)";

		List<String> result = session.getResultAsListByQuery(query, params, 0);
		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else
			return result;
	}

	public boolean isLastBatchEnded(Calendar initiatedDateTime) throws GenericDataModelException {
		logger.info("isLastBatchEnded initiated {}", initiatedDateTime);
		int additionalBufferTimeToAddForPackWipeOutInMins =
				ACSConstants.DEFAULT_ADDITIONAL_TIME_FOR_PACK_WIPE_OUT_IN_MINS;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.ADDITIONAL_TIME_FOR_PACK_WIPE_OUT_IN_MINS);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			additionalBufferTimeToAddForPackWipeOutInMins = Integer.parseInt(propValue);
		}

		Calendar initiatedDate = (Calendar) initiatedDateTime.clone();
		initiatedDate.set(Calendar.HOUR_OF_DAY, 0);
		initiatedDate.set(Calendar.MINUTE, 0);
		initiatedDate.set(Calendar.SECOND, 0);
		initiatedDate.set(Calendar.MILLISECOND, 0);

		initiatedDateTime.add(Calendar.MINUTE, -additionalBufferTimeToAddForPackWipeOutInMins);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.INITIATED_DATE_TIME, initiatedDateTime);
		params.put(ACSConstants.INITIATED_DATE, initiatedDate);
		params.put("isBatchCancelled", false);

		int count = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_BATCH_TOCHECK_LASTBATCHENDED, params);
		logger.info("Number of Batches ended are {}", count);
		if (count > 0)
			return false;
		return true;
	}

	public List<String> getBatchCodesByPackIdentifier(String packIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		List<String> batchCodes =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BATCHCODES_FOR_PACKIDENTIFIER, params);
		return batchCodes;
	}

	public boolean isCancelledBatch(int batchID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchId", batchID);
		Boolean value =
				(Boolean) session.getByQuery("SELECT isBatchCancelled FROM BatchDetailsTO WHERE batchId=(:batchId)",
						params);
		if (value == null) {
			return false;
		}
		return value;
	}

	/**
	 * 
	 * @param initiatedDateTime
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isLastBatchEndedEOD(Calendar initiatedDateTime) throws GenericDataModelException {

		Calendar initiatedDate = (Calendar) initiatedDateTime.clone();
		initiatedDate.set(Calendar.HOUR_OF_DAY, 0);
		initiatedDate.set(Calendar.MINUTE, 0);
		initiatedDate.set(Calendar.SECOND, 0);
		initiatedDate.set(Calendar.MILLISECOND, 0);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.INITIATED_DATE_TIME, initiatedDateTime);
		params.put(ACSConstants.INITIATED_DATE, initiatedDate);
		params.put("isBatchCancelled", false);
		int count = session.getResultCountByQuery(ACSQueryConstants.QUERY_FETCH_BATCH_FOR_EOD_BATCHENDED, params);
		if (count > 0)
			return false;
		return true;
	}

	public BatchInformationDO getBatchInformationByBatchId(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		BatchInformationDO batchInformation =
				(BatchInformationDO) session.getByQuery(ACSQueryConstants.QUERY_FETCHINFO_BY_BATCHCODE, params,
						BatchInformationDO.class);
		return batchInformation;
	}

	public AcsCdbFailureNotifications getCDBFailureNotificationByBatchIdAndNotificationType(String batchCode,
			CDBNotificationTypesEnum notificationType) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("notificationType", notificationType);
		AcsCdbFailureNotifications cdbFailureNotifications =
				(AcsCdbFailureNotifications) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_CDBFAILURENOTIFICATION_FOR_BATCH, params);
		return cdbFailureNotifications;
	}

	public AcsCdbFailureNotifications getCDBFailureNotificationByPackIdAndPackActivationStatus(String packId,
			PackActivatedStatus packActivatedStatus) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);
		params.put("packActivatedStatus", packActivatedStatus);
		AcsCdbFailureNotifications cdbFailureNotifications =
				(AcsCdbFailureNotifications) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_CDBFAILURENOTIFICATION_FOR_PACK, params);
		return cdbFailureNotifications;
	}

	public boolean saveCDBFailureNotification(AcsCdbFailureNotifications cdbFailureNotification)
			throws GenericDataModelException {
		session.persist(cdbFailureNotification);
		return true;
	}

	public int updateCDBFailureNotificationByBatchIdAndNotificationType(String batchCode,
			CDBNotificationTypesEnum notificationType, String notificationMessage) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("notificationType", notificationType);
		params.put("notificationMessage", notificationMessage);
		params.put("lastUpdatedTime", Calendar.getInstance());

		int count =
				session.updateByQuery(
						"UPDATE AcsCdbFailureNotifications SET notificationMessage = (:notificationMessage),lastUpdatedTime = (:lastUpdatedTime) WHERE batchCode=(:batchCode) and notificationType = (:notificationType)",
						params);
		return count;
	}

	public int updateCDBFailureNotificationByPackIdAndPackActivationStatus(String cdbFailureNotificationId,
			PackActivatedStatus packActivatedStatus, String notificationMessage) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("cdbFailureNotificationId", cdbFailureNotificationId);
		params.put("packActivatedStatus", packActivatedStatus);
		params.put("notificationMessage", notificationMessage);
		params.put("lastUpdatedTime", Calendar.getInstance());
		int count =
				session.updateByQuery(
						"UPDATE AcsCdbFailureNotifications SET notificationMessage = (:notificationMessage),lastUpdatedTime = (:lastUpdatedTime) WHERE id=(:cdbFailureNotificationId) and packActivatedStatus = (:packActivatedStatus)",
						params);
		return count;
	}

	public List<AcsCdbFailureNotifications> getAllFailedCDBNotifications() throws GenericDataModelException {
		List<AcsCdbFailureNotifications> cdbFailureNotifications =
				session.getListByQuery("FROM AcsCdbFailureNotifications order by id asc", null);
		return cdbFailureNotifications;
	}

	public int deleteSuccessfulNotificationByBatchIdAndNotificationType(String batchCode,
			CDBNotificationTypesEnum notificationType, Calendar lastUpdatedTime, boolean forceDelete)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("notificationType", notificationType);

		String query = ACSQueryConstants.QUERY_DELETE_CDBNOTIFICATIONS_FOR_BATCH;

		if (!forceDelete) {
			params.put("lastUpdatedTime", lastUpdatedTime);
			query = query + " and lastUpdatedTime = (:lastUpdatedTime)";
		}
		int count = session.updateByQuery(query, params);
		return count;
	}

	/**
	 * Delete notification entries from {@link AcsCdbFailureNotifications} for which notifications were sent
	 * successfully sent.
	 * 
	 * @param packId
	 * @param packActivatedStatus
	 * @param lastUpdatedTime
	 * @param forceDelete
	 * @return
	 * @throws GenericDataModelException
	 */
	public int deleteSuccessfulNotificationByPackIdAndPackActivationStatus(String packId,
			PackActivatedStatus packActivatedStatus, Calendar lastUpdatedTime, boolean forceDelete)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);
		params.put("packActivatedStatus", packActivatedStatus);

		String query =
				"DELETE FROM AcsCdbFailureNotifications  WHERE packIdentifier=(:packId) and packActivatedStatus = (:packActivatedStatus)";

		if (!forceDelete) {
			params.put("lastUpdatedTime", lastUpdatedTime);
			query = query + " and lastUpdatedTime = (:lastUpdatedTime)";
		}
		int count = session.updateByQuery(query, params);
		return count;
	}

	/**
	 * isBatchActive API used to check weather batch is active or not
	 * 
	 * @param batchID
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean isBatchActive(String batchCode) throws GenericDataModelException {
		AcsBatch batchDetailsTO = getBatchDetailsByBatchCode(batchCode);

		if (Calendar.getInstance().after(batchDetailsTO.getMaxBatchEndTime())) {
			return false;
		}
		return true;
	}

	public String getBatchStatus(String batchCode) throws GenericDataModelException {
		AcsBatch batchDetailsTO = getBatchDetailsByBatchCode(batchCode);

		if (batchDetailsTO == null) {
			return null;
		} else {
			if (batchDetailsTO.getMaxBatchEndTime() != null
					&& Calendar.getInstance().after(batchDetailsTO.getMaxBatchEndTime())) {
				return "BatchExpired";

			} else if (batchDetailsTO.getIsBatchCancelled()) {
				return "BatchCancelled";

			} else {
				return null;
			}

		}
	}

	/**
	 * saveBatchDetailsForAttendanceAndRpack API for doing batch level merge for Rapck and Attendance pack at same place
	 * 
	 * @param batchId
	 * @param packDetailsSet
	 * @param deltaPackStatus
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveBatchDetailsForAttendanceAndRpack(String batchCode, Set<AcsPacks> packDetailsSet,
			String deltaRPackStatus, List<String> packIdList) throws GenericDataModelException {
		AcsBatch batchDetails = getBatchDetailsByBatchCode(batchCode);

		updateBatchDetails(batchDetails, true, packIdList);
	}

	public void saveBatchDetailsForAttendanceAndRpack(String batchCode, Set<AcsPacks> packDetailsSet,
			List<String> packIdList) throws GenericDataModelException {
		AcsBatch batchDetails = getBatchDetailsByBatchCode(batchCode);

		updateBatchDetails(batchDetails, true, packIdList);
	}

	/**
	 * Merge entity {@link AcsBatch}
	 * 
	 * @param acsBatch
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void merge(AcsBatch acsBatch) throws GenericDataModelException {
		session.merge(acsBatch);
	}

	/**
	 * Checks weather batch represented by given batch code is cancelled or not.
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isBatchCancelled(String batchCode) throws GenericDataModelException {
		AcsBatch batch = (AcsBatch) session.get(batchCode, AcsBatch.class.getCanonicalName());
		if (batch == null) {
			return false;
		}
		return batch.getIsBatchCancelled();
	}

	/**
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatch> getActiveBatchDetails() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isBatchCancelled", false);

		List<AcsBatch> batchDetailsList =
				session.getResultAsListByQuery("FROM AcsBatch WHERE  isBatchCancelled = (:isBatchCancelled)", params, 0);
		return batchDetailsList;
	}

	public long getMaxDeltaRpackGenerationTime(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);

		Timestamp maxDelataRpackGenTime =
				(Timestamp) session
						.getUniqueResultByNativeSQLQuery(
								"select STR_TO_DATE(greatest(IFNULL(deltarpackgenerationtime,0),IFNULL(deltarpackgenerationtimebyacs,0)),'%Y-%m-%d %T') from acsbatchdetails where batchcode=(:batchCode)",
								params);
		if (maxDelataRpackGenTime != null) {
			return maxDelataRpackGenTime.getTime();
		} else {
			return 0;
		}
	}

	public BatchMetaDataBean getBatchMetaDataBeanByBatchCode(String batchCode) throws GenericDataModelException {
		AcsBatch batchDetailsTO = this.getBatchDetailsByBatchCode(batchCode);
		BatchMetaDataBean batchMetaDataBean = null;
		if (batchDetailsTO != null) {
			batchMetaDataBean = new BatchMetaDataBean();
			batchMetaDataBean.setBatchCode(batchDetailsTO.getBatchCode());
			batchMetaDataBean
					.setBatchDate(batchDetailsTO.getBatchDate() == null ? null : batchDetailsTO.getBatchDate());
			batchMetaDataBean.setBatchDuration(batchDetailsTO.getBatchDuration() == 0l ? 0 : (int) batchDetailsTO
					.getBatchDuration());
			batchMetaDataBean.setBatchEndTime(batchDetailsTO.getBatchEndTime() == null ? null : batchDetailsTO
					.getBatchEndTime());
			batchMetaDataBean.setBatchName(batchDetailsTO.getBatchName());
			batchMetaDataBean.setBatchStartTime(
					batchDetailsTO.getMaxBatchStartTime() == null ? null : batchDetailsTO.getMaxBatchStartTime());
			batchMetaDataBean.setCandidateCount(batchDetailsTO.getCandidateCount() == 0l ? 0 : (int) batchDetailsTO
					.getCandidateCount());
			batchMetaDataBean.setDeltaRpackGenerationTime(batchDetailsTO.getDeltaRpackGenerationTime() == null ? null
					: batchDetailsTO.getDeltaRpackGenerationTime());
			batchMetaDataBean.setLateLoginTime(batchDetailsTO.getExtendedLateLoginTime() == null ? null
					: batchDetailsTO.getDeltaRpackGenerationTime());
			batchMetaDataBean.setRpackGenerationTime(batchDetailsTO.getRpackGenerationTime() == null ? null
					: batchDetailsTO.getRpackGenerationTime());
		}
		return batchMetaDataBean;
	}

	@Override
	public AcsBatch loadBatch(String batchCode) throws GenericDataModelException {
		return (AcsBatch) session.get(batchCode, AcsBatch.class.getName());
	}

	public List<AcsBatchCandidateAssociation> getCurrentBatchCandidateAssociations(String username,
			List<String> activeBatches) throws GenericDataModelException {

		Map<String, Object> params = new HashMap<>();
		params.put("batchCodes", activeBatches);
		params.put("username", username);

		List<AcsBatchCandidateAssociation> result =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_BCA_FOR_CANDIDATE_IN_CURRENT_BATCH,
						params, 0);
		// session.getSession().close();
		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else
			return result;
	}

	/**
	 * Fetches the AcsBatchCandidateAssociation where provided candidateLogin name exists
	 * 
	 * @param username
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationsForEncryptedUserName(String username)
			throws GenericDataModelException {

		Map<String, Object> params = new HashMap<>();
		params.put("username", username);

		@SuppressWarnings("unchecked")
		List<AcsBatchCandidateAssociation> result =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_FOR_ENCRYPTED_CANDIDATE_IN_ANY_BATCH, params, 0);
		// List<AcsBatchCandidateAssociation> result =
		// (List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
		// ACSQueryConstants.QUERY_FETCH_BCA_FOR_CANDIDATE_IN_ANY_BATCH, params,
		// AcsBatchCandidateAssociation.class);

		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else {
			return result;
		}
	}
	
	
	/**
	 * Fetches the AcsBatchCandidateAssociation where provided ENCRYPTED candidateLogin name exists
	 * @param username
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsBatchCandidateAssociation> getBatchCandidateAssociationsForUserName(String username)
			throws GenericDataModelException {

		Map<String, Object> params = new HashMap<>();
		params.put("username", username);

		@SuppressWarnings("unchecked")
		List<AcsBatchCandidateAssociation> result =
				(List<AcsBatchCandidateAssociation>) session.getResultAsListByQuery(
						ACSQueryConstants.QUERY_FETCH_BCA_FOR_CANDIDATE_IN_ANY_BATCH, params, 0);
		// List<AcsBatchCandidateAssociation> result =
		// (List<AcsBatchCandidateAssociation>) session.getResultListByNativeSQLQuery(
		// ACSQueryConstants.QUERY_FETCH_BCA_FOR_CANDIDATE_IN_ANY_BATCH, params,
		// AcsBatchCandidateAssociation.class);

		if (result.equals(Collections.<Object> emptyList()))
			return null;
		else {
			return result;
		}
	}

	/**
	 * Get batch code for report identifier which is primary key for {@link AcsReportDetails}
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getBatchCodeForReport(String reportIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("reportIdentifier", reportIdentifier);
		String batchCode =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_BATCHCODE_BY_REPORT_IDENTIFIER, params);
		return batchCode;
	}

	/**
	 * Get batch code On eventCode for report identifier which is primary key for {@link AcsReportDetails}
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getBatchCodeForReportOnEventCode(String reportIdentifier, String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<>();
		params.put("reportIdentifier", reportIdentifier);
		params.put("eventCode", eventCode);
		String batchCode =
				(String) session.getByQuery(ACSQueryConstants.QUERY_FETCH_BATCHCODE_BY_REPORT_IDENTIFIER_ON_EVENTCODE,
						params);
		return batchCode;
	}

	public void saveOrUpdateBatchDetails(AcsBatch batchDetails) throws GenericDataModelException {
		session.saveOrUpdate(batchDetails);
	}

	public AcsBatch getBatchDetailsWithPackDetailsByBatchCode(String batchCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);

		String query = ACSQueryConstants.QUERY_FETCH_BATCH_DETAILS_BY_BATCHCODE;
		AcsBatch acsBatch = (AcsBatch) session.getByQuery(query, params);
		return acsBatch;
	}

	public List<AcsBatch> getListOfBatchesWithDateRange(Calendar startDate, Calendar endDate)
			throws GenericDataModelException {
		Map<String, Calendar> params = new HashMap<String, Calendar>();
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		List<AcsBatch> batchDetails =
				session.getListByQuery(ACSQueryConstants.QUERY_BATCHDETAILS_FETCH_BATCHDETAILS_BY_DATE_RANGE, params);
		return batchDetails;

	}

	/**
	 * This API triggers the batch Completion Job
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean completeTheBatch(String batchCode) throws GenericDataModelException {

		logger.debug("completeTheBatch Job creation initiated with batchCode:{}", batchCode);
		AcsBatch acsBatch = getBatchDetailsByBatchCode(batchCode);
		if (acsBatch == null) {
			logger.error("No such batch with batchCode:{}", batchCode);
			return false;
		}
		BatchCompletionStatusEnum batchCompletionStatus = acsBatch.getBatchCompletionStatus();
		logger.debug("batchCompletionStatus of batchCode:{} is {}", batchCode, batchCompletionStatus);
		if (batchCompletionStatus != null
				&& batchCompletionStatus != BatchCompletionStatusEnum.BATCH_COMPLETION_JOB_FAILED) {
			logger.debug("completeTheBatch Job creation initiatation CANCELLED as it is running now with batchCode:{}",
					batchCode);
			return false;
		}

		JobDetail job = null;
		Scheduler scheduler = null;
		boolean reschedule = true;

		String BATCH_COMPLETION_JOB_NAME = ACSConstants.BATCH_COMPLETION_JOB_NAME;
		String BATCH_COMPLETION_JOB_GROUP = ACSConstants.BATCH_COMPLETION_JOB_GROUP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(BatchCompletionJob.class)
							.withIdentity(BATCH_COMPLETION_JOB_NAME + batchCode, BATCH_COMPLETION_JOB_GROUP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCode);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(BATCH_COMPLETION_JOB_NAME + batchCode,
							BATCH_COMPLETION_JOB_GROUP));

			if (trigger != null) {
				if (reschedule) {
					scheduler.unscheduleJob(TriggerKey.triggerKey(BATCH_COMPLETION_JOB_NAME + batchCode,
							BATCH_COMPLETION_JOB_GROUP));
				} else {
					logger.info("Already a trigger exists for CandidateForceSubmitJob for specified batchId = {}",
							batchCode);
					return false;
				}
			}

			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(BATCH_COMPLETION_JOB_NAME + batchCode, BATCH_COMPLETION_JOB_GROUP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
							.startNow().build();

			logger.trace("Trigger for batch completion = {} " + trigger);
			scheduler.scheduleJob(job, trigger);

		} catch (SchedulerException ex) {
			logger.error("SchedulerException while executing startCandidateHeartBeatJob...", ex);
			return false;
		}
		return true;
	}

	/**
	 * Converts the batchCompletionInfo to Json and Updates it in Batch table
	 * 
	 * @param batchCompletionStatus
	 * @param batchCompletionInfo
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateBatchCompletionStatus(BatchCompletionStatusEnum batchCompletionStatus,
			BatchCompletionInfo batchCompletionInfo, String batchCode) throws GenericDataModelException {
		String batchCompletionInfoJson = new Gson().toJson(batchCompletionInfo);
		logger.debug("updateBatchCompletionStatus batchCode:{} status: {}, batchCompletionInfo:{}", batchCode,
				batchCompletionStatus, batchCompletionInfo);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchcode", batchCode);
		params.put("batchCompletionStatus", batchCompletionStatus);
		params.put("batchCompletionInfo", batchCompletionInfoJson);

		int count = 0;
		count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_BATCH_COMPLETION_STATUS, params);
		return count > 0 ? true : false;

	}
	
	/**
	 * gets the list of current running batch information
	 * 
	 * @param string
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<String> getAllActiveBatchCodes() throws GenericDataModelException {
		String propValue = null;
		int preTestBufferTime = ACSConstants.DEFAULT_PRE_TEST_START_ALLOWED_LOGIN_TIME;
		propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PRE_TEST_START_ALLOWED_LOGIN_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			preTestBufferTime = Integer.parseInt(propValue);
		}

		int postTestBufferTime = ACSConstants.DEFAULT_POST_TEST_BUFFER_TIME;
		propValue = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.POST_TEST_BUFFER_TIME);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			postTestBufferTime = Integer.parseInt(propValue);
		}

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("dateTime", Calendar.getInstance());
		params.put("postTestBufferTime", postTestBufferTime);
		params.put("preTestBufferTime", preTestBufferTime);

		List<String> batchCodess =
				session.getResultListByNativeSQLQuery(ACSQueryConstants.QUERY_FETCH_ACTIVE_BATCHCODES_BY_TIMEINSTANCE,
						params);
		return batchCodess;
	}
}