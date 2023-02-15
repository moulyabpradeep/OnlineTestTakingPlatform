package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.BatchExtensionEnum;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.BatchExtensionDO;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsEvent;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.quartz.jobs.BatchExtensionInfoNotifier;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.entities.acs.BatchExtensionInformationEntity;
import com.merittrac.apollo.common.entities.acs.BatchTypeEnum;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateExtensionWrapperDO;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateLateLoginTimeExtensionDO;
import com.merittrac.apollo.common.entities.deliverymanager.CandidateType;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * handles api's related batch extension at acs
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class BatchExtensionService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static BatchCandidateAssociationService batchCandidateAssociationService = null;
	static CustomerBatchService customerBatchService = null;
	static CDEAService cdeaService = null;
	static CandidateService candidateService = null;
	static BatchService batchService = null;
	private static String isAuditEnable = null;
	static ACSService acsService = null;
	static BrLrService brlrService = null;
	static AuthService authService = null;
	private static ACSPropertyUtil acsPropertyUtil = null;
	static CandidateLateLoginTimeExtensionService candidateLateLoginTimeExtensionService = null;
	static {
		if (batchCandidateAssociationService == null) {
			batchCandidateAssociationService = new BatchCandidateAssociationService();
		}
		acsPropertyUtil = new ACSPropertyUtil();
		candidateLateLoginTimeExtensionService = new CandidateLateLoginTimeExtensionService();
		brlrService = new BrLrService();
		if (customerBatchService == null) {
			customerBatchService = new CustomerBatchService();
		}
		authService = new AuthService();
		if (cdeaService == null) {
			cdeaService = new CDEAService();
		}

		if (candidateService == null) {
			candidateService = CandidateService.getInstance();
		}

		if (acsService == null) {
			acsService = new ACSService();
		}
		batchService = BatchService.getInstance();
		isAuditEnable = AutoConfigure.getAuditConfigureValue();
	}

	/**
	 * extends the late login time for a candidate
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 */
	public BatchExtensionDO[]
			extendBatchEndTime(BatchExtensionDO[] batchExtensionDOs, String userName, String ipAddress)
					throws GenericDataModelException, CandidateRejectedException {
		logger.info("initiated extendBatchEndTime where batchExtensionDOs= {}", batchExtensionDOs.toString());

		// list of batch extension info's which will be intimated to CDB
		Map<String, BatchExtensionInformationEntity> batchExtensionInfoMap =
				new HashMap<String, BatchExtensionInformationEntity>();

		// iterate over each BatchExtensionDO objects
		for (BatchExtensionDO batchExtensionDO : batchExtensionDOs) {
			boolean isRescheduleRequired = false;

			// handling invalid input
			if (batchExtensionDO.getExtendedTimeInmins() <= 0) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO
						.setErrorMessage("Invalid input (value greater than 0 is expected for batch extension)");
				continue;
			}

			// calculate extended batch end time based on extended time in mins
			Calendar prevMaxBatchEndTime =
					TimeUtil.convertStringToCalender(batchExtensionDO.getMaxBatchEndTime(),
							TimeUtil.YYYY_MM_DD_HH_MM_SS);
			Calendar extendedBatchEndTime = (Calendar) prevMaxBatchEndTime.clone();
			if (extendedBatchEndTime == null) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO.setErrorMessage("unable to convert current max batch end time to calender object");
				continue;
			}
			extendedBatchEndTime.add(Calendar.MINUTE, batchExtensionDO.getExtendedTimeInmins());

			// check whether extendedBatchEndTime is within current time or not
			if (extendedBatchEndTime.before(Calendar.getInstance())) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO.setErrorMessage("Extended batch end time is elapsed : "
						+ TimeUtil.convertTimeAsString(extendedBatchEndTime.getTimeInMillis(),
								TimeUtil.YYYY_MM_DD_HH_MM_SS));
				continue;
			}

			// get the batchCode
			String batchCode = batchExtensionDO.getBatchCode();

			AcsEvent eventDetails = cdeaService.getEventDetailsByBatchCode(batchCode);
			logger.info("eventDetails = {} for batch with id = {}", eventDetails, batchCode);

			// check whether extendedBatchEndTime is within event end time or not
			if (extendedBatchEndTime.after(eventDetails.getEventEndDate())) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO.setErrorMessage("Batch extension not allowed after Event End Date : "
						+ TimeUtil.convertTimeAsString(eventDetails.getEventEndDate().getTimeInMillis(),
								TimeUtil.YYYY_MM_DD_HH_MM_SS));
				continue;
			}

			// fetch batch details associated to the specified batchId
			AcsBatch batchDetailsTO = (AcsBatch) batchService.get(batchCode, AcsBatch.class.getCanonicalName());
			logger.info("batchdetails = {} for batch with id = {}", batchDetailsTO, batchCode);

			// check whether new late login time is within delat rpack generation time or not
			if (batchDetailsTO.getMaxDeltaRpackGenerationTime().before(Calendar.getInstance())) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO.setErrorMessage("Batch extension not allowed after delta rpack generation time : "
						+ TimeUtil.convertTimeAsString(batchDetailsTO.getMaxDeltaRpackGenerationTime()
								.getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
				continue;
			}

			// check whether a value greater than the specified time exist or not
			if (batchDetailsTO.getMaxBatchEndTime().after(extendedBatchEndTime)) {
				batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
				batchExtensionDO
						.setErrorMessage("Batch extension not allowed because value(extension time) greater than specified time exists: "
								+ TimeUtil.convertTimeAsString(batchDetailsTO.getMaxBatchEndTime().getTimeInMillis(),
										TimeUtil.YYYY_MM_DD_HH_MM_SS));
				continue;
			}

			// check whether extended batch end time is going beyond delta rpack generation time or not if so reschedule
			// the necessary jobs accordingly based on the flag
			if (batchDetailsTO.getMaxDeltaRpackGenerationTime().before(extendedBatchEndTime)) {
				isRescheduleRequired = true;
			}

			// audit in incident log
			IncidentAuditActionDO incidentAuditAction =
					new IncidentAuditActionDO(ipAddress, TimeUtil.convertTimeAsString(Calendar.getInstance()
							.getTimeInMillis()), IncidentAuditLogEnum.BATCH_EXTENSION_AT_ACS,
							batchExtensionDO.getBatchCode(), batchExtensionDO.getMaxBatchEndTime(),
							TimeUtil.convertTimeAsString(extendedBatchEndTime.getTimeInMillis()),
							batchExtensionDO.getExtendedTimeInmins());
			AuditTrailLogger.incidentAudit(incidentAuditAction);

			// admin audit
			if (isAuditEnable != null && isAuditEnable.equals("true")) {
				Object[] paramArray =
						{ batchExtensionDO.getBatchCode(), batchExtensionDO.getExtendedTimeInmins(),
								batchExtensionDO.getMaxBatchEndTime(),
								TimeUtil.convertTimeAsString(extendedBatchEndTime.getTimeInMillis()) };

				HashMap<String, String> logbackParams = new HashMap<String, String>();
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
						AdminAuditActionEnum.BATCH_EXTENSION_AT_ACS.toString());
				logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
				logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
				logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");

				AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
						ACSConstants.AUDIT_BATCH_EXTENSION_AT_ACS_MSG, paramArray);
			}

			batchDetailsTO.setIsBatchExtended(BatchExtensionEnum.BATCH_EXTENSION_AT_ACS);
			batchDetailsTO.setBatchEndTimeByAcs(extendedBatchEndTime);

			// check whether the delta rpack generation time at ACS is behind extendedBatchEndTime then update it
			if (isRescheduleRequired || batchDetailsTO.getDeltaRpackGenerationTimeByAcs() == null
					|| (batchDetailsTO.getDeltaRpackGenerationTimeByAcs().before(extendedBatchEndTime))) {
				batchDetailsTO.setDeltaRpackGenerationTimeByAcs(extendedBatchEndTime);
			}

			batchDetailsTO = batchService.updateBatchDetails(batchDetailsTO, true, null);
			// bs.updateBatchEndTime(batchMetaDataBean.getBatchEndTime(), batchDetails.getBatchId());

			if (isRescheduleRequired) {
				// reschedule all the necessary jobs
				candidateService.startCandidateForceSubmitJob(batchDetailsTO.getDeltaRpackGenerationTimeByAcs(),
						batchDetailsTO.getBatchCode(), false);
				// candidateService.startCandidateHeartBeatJob(batchDetailsTO.getBatchStartTime(),
				// batchDetailsTO.getDeltaRpackGenerationTimeByACS(), batchDetailsTO.getBatchId(), true);
				// candidateService.startPlayerHeartBeatJob(batchDetailsTO.getBatchStartTime(),
				// batchDetailsTO.getDeltaRpackGenerationTimeByACS(), batchDetailsTO.getBatchId(), true);
				// candidateService.startSystemHeartBeatJob(batchDetailsTO.getBatchStartTime(),
				// batchDetailsTO.getDeltaRpackGenerationTimeByACS(), batchDetailsTO.getBatchId(), true);
				candidateService.startCandidtaeLiveStatusInitiator(batchDetailsTO.getBatchCode(),
						batchDetailsTO.getMaxBatchStartTime(), batchDetailsTO.getDeltaRpackGenerationTimeByAcs(),
						false);
			}

			batchExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);

			BatchExtensionInformationEntity batchExtensionInformationEntity =
					cdeaService.getCustomerDivisionEventCodesByBatchCode(batchCode);
			if (batchExtensionInformationEntity != null) {
				batchExtensionInformationEntity.setBatchCode(batchDetailsTO.getBatchCode());
				batchExtensionInformationEntity.setBatchStartTime(batchDetailsTO.getMaxBatchStartTime());
				batchExtensionInformationEntity.setPrevBatchEndTime(prevMaxBatchEndTime);
				batchExtensionInformationEntity.setCurrentBatchEndTime(extendedBatchEndTime);

				batchExtensionInfoMap.put(batchCode, batchExtensionInformationEntity);
			} else {
				logger.info("unable to fetch CDE information for the specified batchId = {}", batchCode);
			}
		}

		if (!batchExtensionInfoMap.isEmpty()) {
			// iterate over the map and reshedule the dependent jobs accordingly
			for (Map.Entry<String, BatchExtensionInformationEntity> entry : batchExtensionInfoMap.entrySet()) {
				// initiate batch extension information notifier job
				startBatchExtensionInfoNotificationInitiator(entry.getKey(), entry.getValue());
			}
		}
		return batchExtensionDOs;
	}

	/**
	 * calculates and sends the current batch end time
	 * 
	 * @param candidateLateLoginTimeExtensionDOs
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<BatchExtensionDO> initiateBatchExtension(String[] batchCodes) throws GenericDataModelException {
		logger.info("initiated initiateBatchExtension where batchIds={}", batchCodes.toString());

		List<BatchExtensionDO> batchExtensionDOs = new ArrayList<BatchExtensionDO>();

		// iterate over the each batchId
		for (String batchCode : batchCodes) {
			// fetch batch details associated to the specified batchId
			AcsBatch batchDetailsTO = (AcsBatch) batchService.get(batchCode, AcsBatch.class.getCanonicalName());
			logger.info("batchdetails = {} for batch with id = {}", batchDetailsTO, batchCode);

			// check whether batch info exist or not
			if (batchDetailsTO == null) {
				logger.info("No batch details exists with specified batchId= {}", batchCode);
				continue;
			}

			BatchExtensionDO batchExtensionDO = getBatchExtensionEntity(batchDetailsTO);

			// add to the list
			batchExtensionDOs.add(batchExtensionDO);
		}
		logger.info("list of batchExtensionDOs returned = {}", batchExtensionDOs);
		return batchExtensionDOs;
	}

	private BatchExtensionDO getBatchExtensionEntity(AcsBatch batchDetailsTO) {
		// form the batch extension information object
		BatchExtensionDO batchExtensionDO = new BatchExtensionDO();
		batchExtensionDO.setBatchCode(batchDetailsTO.getBatchCode());
		batchExtensionDO.setBatchEndTime(batchDetailsTO.getBatchEndTime());
		batchExtensionDO.setBatchEndTimeByACS(batchDetailsTO.getBatchEndTimeByAcs());
		batchExtensionDO.setMaxBatchEndTime(batchDetailsTO.getMaxBatchEndTime());
		batchExtensionDO.setBatchName(batchDetailsTO.getBatchName());
		batchExtensionDO.setBatchStartTime(batchDetailsTO.getMaxBatchStartTime());
		return batchExtensionDO;
	}

	/**
	 * This api will initiate a job which will send the candidate information to cdb for a particular batch
	 * 
	 * @param batchId
	 * @param batchCode
	 * @param candidateStatusList
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public boolean startBatchExtensionInfoNotificationInitiator(String batchCode,
			BatchExtensionInformationEntity batchExtensionInformationEntity) throws GenericDataModelException {
		logger.info(
				"initiated startBatchExtensionInfoNotificationInitiator where batchId = {}, batchExtensionInformationEntity = {}",
				batchCode, batchExtensionInformationEntity);
		JobDetail job = null;
		Scheduler scheduler = null;

		String CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME =
				ACSConstants.CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME;
		String CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP = ACSConstants.CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP;
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			// create a job instance and add required information to job data map
			job =
					JobBuilder
							.newJob(BatchExtensionInfoNotifier.class)
							.withIdentity(CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_ID, batchCode);
			job.getJobDataMap().put(ACSConstants.BATCH_EXTENSION_INFO, batchExtensionInformationEntity);

			// check whether already a trigger exists for the specified batch or not
			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME
							+ batchCode, CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP));
			logger.info("trigger with specified key = {} and group = {}", CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME
					+ batchCode, CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP);

			// if trigger already exists unschedule the existing one and create a new trigger
			if (trigger != null) {
				scheduler.unscheduleJob(TriggerKey.triggerKey(
						CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME + batchCode,
						CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP));
				logger.info(
						"Already a trigger exists for BatchExtensionInfoNotifier job for specified batchId = {} hence, unscheduling it and initiating new trigger",
						batchCode);
			}

			// create a trigger new instance
			trigger =
					TriggerBuilder
							.newTrigger()
							.withIdentity(CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_NAME + batchCode,
									CDB_BATCH_EXTENSION_INFORMATION_NOTIFIER_GRP)
							.withSchedule(
									SimpleScheduleBuilder.simpleSchedule()
											.withMisfireHandlingInstructionNextWithRemainingCount()).startNow().build();

			// associate the trigger and job and add them to the scheduler
			scheduler.scheduleJob(job, trigger);
			logger.trace("Trigger for BatchExtensionInfoNotifier = {} " + trigger);
		} catch (SchedulerException ex) {
			logger.error("SchedulerException while executing startBatchExtensionInfoNotificationInitiator...", ex);
			return false;
		}
		return true;
	}

	/**
	 * postpone Batch Start Time for whole batch in case of any genuine failure
	 * 
	 * @param batchCode
	 * @param postponeBatchstartDate
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public BatchExtensionDO postponeBatchStartTime(String batchCode, Calendar postponeBatchstartTime)
			throws GenericDataModelException, CandidateRejectedException {
		BatchExtensionDO batchExtensionDO = null;
		if (postponeBatchstartTime == null || batchCode == null || batchCode.isEmpty()) {
			return new BatchExtensionDO(batchCode, ACSConstants.MSG_FAILURE, "Invalid input, can't process request");
		}
		logger.debug("--IN-- postponeBatchStartTime batchCode : {} and postponeBatchstartTime : {}", batchCode,
				postponeBatchstartTime.getTime());

		Calendar currentTime = Calendar.getInstance();
		// postpone late login, batch end time, delta rpack
		AcsBatch acsBatch = batchService.getBatchDetailsByBatchCode(batchCode);
		if (acsBatch == null) {
			return new BatchExtensionDO(batchCode, ACSConstants.MSG_FAILURE, "Invalid batch code = " + batchCode);
		}
		batchExtensionDO = getBatchExtensionEntity(acsBatch);
		// validate
		if (!batchCandidateAssociationService.getCountOfLoggedInCandidatesByBatchCode(batchCode)) {
			logger.debug("Failure, Few candidates have already logged-in for batch code = {}", batchCode);
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage(
					"Few candidates have already logged-in for batch code = " + batchCode);
			return batchExtensionDO;

		}
		if (!acsBatch.getBatchType().name().equals(BatchTypeEnum.NORMAL.name())) {
			logger.debug("Failure, Cannot process request for batch type = {} for batch code = {}",
					acsBatch.getBatchType().toString(), batchCode);
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage(
					"Cannot process request for batch type " + acsBatch.getBatchType().toString() + " Batch code "
							+ batchCode);
			return batchExtensionDO;
		}
		if (postponeBatchstartTime.before(currentTime)
				|| postponeBatchstartTime.before(acsBatch.getMaxBatchStartTime())) {
			logger.debug(
					"Failure, Cannot process request, the proposed time = {} is already expired for batch code = {}",
					postponeBatchstartTime.getTime(), batchCode);
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage(
					"Cannot process request, the proposed time is already expired");
			return batchExtensionDO;
		}

		int deltaTimeinMins = 0;
		if (postponeBatchstartTime.after(acsBatch.getMaxBatchStartTime()))
			// convert millis to mins hence divide by 1000(for secs) and by 60(for mins)
			deltaTimeinMins =
					(int) ((postponeBatchstartTime.getTimeInMillis()
							- acsBatch.getMaxBatchStartTime().getTimeInMillis()) / 60000);

		logger.debug("delta time in mins = {} to extend for batch code = {}", deltaTimeinMins, batchCode);

		if (deltaTimeinMins <= 0) {
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("Cannot process request, the delta time is neglegible");
			return batchExtensionDO;
		}
		// extend batch start time
		acsBatch.setPostponedBatchStartTime(postponeBatchstartTime);
		// don't update now
		// batchService.updateBatchStartTime(postponeBatchstartTime, batchCode);
		// batchExtensionDO.setMaxBatchEndTime(acsBatch.getMaxBatchEndTime());
		batchExtensionDO.setBatchStartTime(acsBatch.getMaxBatchStartTime());
		// set delta mins
		batchExtensionDO.setExtendedTimeInmins(deltaTimeinMins);
		
		// extend batch end time
		batchExtensionDO = extendBatchEndTimeWithPostpone(batchExtensionDO, acsBatch,
				ACSConstants.DEFAULT_ADMIN_USER_NAME,
				ACSConstants.DEFAULT_ADMIN_HOST_ADDRESS);
		if (batchExtensionDO.getStatus().equals(ACSConstants.MSG_SUCCESS)) {
			// get updated extended details
			acsBatch = batchService.getBatchDetailsByBatchCode(batchCode);
			batchExtensionDO.setBatchEndTime(acsBatch.getMaxBatchEndTime());
			// extend batch late login time and batch start time
			batchExtensionDO = extendBatchLateLoginTimeAndPostponeBatch(acsBatch, deltaTimeinMins, batchExtensionDO,
					postponeBatchstartTime);
		}

		logger.debug("--OUT-- postponeBatchStartTime batchCode : {} ", batchCode);
		return batchExtensionDO;
	}

	/**
	 * extend Batch Late Login Time
	 * 
	 * @param acsBatch
	 * @param deltaTimeinMins(not
	 *            used)
	 * @param postponeBatchstartDate
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public BatchExtensionDO extendBatchLateLoginTimeAndPostponeBatch(AcsBatch acsBatch, int deltaTimeinMins,
			BatchExtensionDO batchExtensionDO, Calendar postponeBatchstartTime) throws GenericDataModelException {
		logger.info("--IN-- extendBatchLateLoginTimeAndPostponeBatch() batchcode = {} ", acsBatch.getBatchCode());
		batchExtensionDO.setMaxBatchEndTime(acsBatch.getMaxBatchEndTime());
		batchExtensionDO.setBatchStartTime(acsBatch.getMaxBatchStartTime());
		if (acsBatch.getMaxDeltaRpackGenerationTime().after(Calendar.getInstance())) {
			// get assessment level late login
			AcsBussinessRulesAndLayoutRules acsBussinessRulesAndLayoutRules =
					brlrService.getAcsBussinessRulesAndLayoutRulesByBatchCode(acsBatch.getBatchCode());
			int currentLateLoginInMins = acsBussinessRulesAndLayoutRules.getLateLoginTime();

			Calendar currentExtendedBatchLateLoginTime = acsBatch.getMaxBatchStartTime();
			currentExtendedBatchLateLoginTime.add(Calendar.MINUTE, currentLateLoginInMins);

			logger.info("Logged Late Login Extension into with details = {} ", currentLateLoginInMins);

			// if late login time is not present in table or if it is not matching, update the late login time
			if (acsBatch.getExtendedLateLoginTime() == null
					|| acsBatch.getExtendedLateLoginTime().compareTo(currentExtendedBatchLateLoginTime) != 0) {
				// process late login
				// audit for incident log
				incidentLog(acsBatch, currentExtendedBatchLateLoginTime);

				// old Late Login Time
				Calendar oldLateLoginTime = acsBatch.getExtendedLateLoginTime();
				String oldLateLoginTimeAsString = "";
				if (oldLateLoginTime != null) {
					oldLateLoginTimeAsString = TimeUtil.convertTimeAsString(oldLateLoginTime.getTimeInMillis());
				}
				logger.info("Late login extended from {} to {}", oldLateLoginTimeAsString,
						TimeUtil.convertTimeAsString(currentExtendedBatchLateLoginTime.getTimeInMillis()));

				acsBatch.setExtendedLateLoginTime(currentExtendedBatchLateLoginTime);
				acsBatch.setIsLateLoginExtended(true);

				// update acs batch -> LateLoginTime And StartTime
				batchService.updateBatchLateLoginTimeAndStartTime(postponeBatchstartTime,
						currentExtendedBatchLateLoginTime, acsBatch.getBatchCode());

				// reschedule Late Login Dependent Jobs
				candidateService.startAttendanceReportGeneratorJob(acsBatch.getBatchCode(),
						acsBatch.getExtendedLateLoginTime(), false);
				batchExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
				batchExtensionDO.setErrorMessage("Request Processed successfully");
				logger.info(
						"--OUT-- extendBatchLateLoginTimeAndPostponeBatch() , Request Processed successfully, batchcode = {} ",
						acsBatch.getBatchCode());
				return batchExtensionDO;
			} else {
				// update only batch start
				batchService.updateBatchStartTime(postponeBatchstartTime, acsBatch.getBatchCode());
				batchExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);
				batchExtensionDO.setErrorMessage("Request Processed successfully");
				logger.info(
						"--OUT-- extendBatchLateLoginTimeAndPostponeBatch() , Request Processed successfully, batchcode = {} ",
						acsBatch.getBatchCode());
				return batchExtensionDO;
			}
		} else {
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("Cannot process request, the delta rpack generation time is expired");
			logger.info(
					"--OUT-- extendBatchLateLoginTimeAndPostponeBatch() , Cannot process request, "
							+ " the delta rpack generation time is expired, batchcode = {} ",
					acsBatch.getBatchCode());
			return batchExtensionDO;
		}

	}

	/**
	 * Logging incident logs for Batch Late Login Extension
	 * 
	 * @param batchMetaDataBean
	 * @param batchDetailsTO
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void incidentLog(AcsBatch batchDetailsTO, Calendar currentExtendedLateLoginTime) {
		logger.info("Inside incidentLog() for batchDetails = {} ", batchDetailsTO);
		IncidentAuditActionDO incidentAuditAction = new IncidentAuditActionDO(NetworkUtility.getIp(),
				TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT),
				IncidentAuditLogEnum.BATCH_LEVEL_LATE_LOGIN_EXTENSION);
		Calendar prevLateLoginTime = batchDetailsTO.getExtendedLateLoginTime();
		String prevLateLoginTimeString = "";

		if (prevLateLoginTime != null) {
			prevLateLoginTimeString =
					TimeUtil.convertTimeAsString(prevLateLoginTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);
		}
		incidentAuditAction.setBatchCode(batchDetailsTO.getBatchCode());
		incidentAuditAction.setPrevLateLoginTime(prevLateLoginTimeString);
		incidentAuditAction.setCurrentLateLoginTime(TimeUtil.convertTimeAsString(
				currentExtendedLateLoginTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT));
		AuditTrailLogger.incidentAudit(incidentAuditAction);
		logger.info("End of incidentLog()");
	}

	/**
	 * @param listOfbcaIds
	 * @param postponeCandidateBatchstartDate
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public CandidateExtensionWrapperDO postponeCandidateBatchStartTime(List<Integer> listOfbcaIds,
			Calendar postponeCandidateBatchstartDate) throws GenericDataModelException, CandidateRejectedException {
		CandidateExtensionWrapperDO candidateExtensionWrapperDO = null;
		List<CandidateLateLoginTimeExtensionDO> candidateBatchTimeExtensionDOs = null;
		if (postponeCandidateBatchstartDate == null || listOfbcaIds == null || listOfbcaIds.isEmpty()) {
			return candidateExtensionWrapperDO =
					new CandidateExtensionWrapperDO(ACSConstants.MSG_FAILURE, "Invalid input", listOfbcaIds);
		}
		Calendar currentTime = Calendar.getInstance();
		// postpone late login, batch end time
		List<AcsBatchCandidateAssociation> bcas =
				batchCandidateAssociationService.getBatchCandAssociationsByBcaIds(listOfbcaIds);
		if (bcas == null || bcas.isEmpty()) {
			return candidateExtensionWrapperDO =
					new CandidateExtensionWrapperDO(ACSConstants.MSG_FAILURE, "Invalid list of bcaIds", listOfbcaIds);
		}
		String batchCode = bcas.get(0).getBatchCode();
		AcsBatch acsBatch = batchService.getBatchDetailsByBatchCode(batchCode);
		int minsToExtendBatch = 0;
		boolean isExtendBatch = false;
		postponeCandidateBatchstartDate.clear(Calendar.MILLISECOND);
		candidateBatchTimeExtensionDOs = new ArrayList<CandidateLateLoginTimeExtensionDO>(bcas.size());
		for (AcsBatchCandidateAssociation bca : bcas) {
			CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO = new CandidateLateLoginTimeExtensionDO(
					bca.getCandidateId(), bca.getBcaid(), bca.getAssessmentCode(), bca.getBatchCode());

			// additional validations For Batch Postpone
			candidateLateLoginTimeExtensionDO = validationsForCandidatePostpone(acsBatch, bca,
					candidateLateLoginTimeExtensionDO, currentTime, postponeCandidateBatchstartDate);
			if (candidateLateLoginTimeExtensionDO.getStatus() != null
					&& candidateLateLoginTimeExtensionDO.getStatus().equals(ACSConstants.MSG_FAILURE)) {
				candidateBatchTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
				continue;
			}
			// check delta rpack time falls after candidate exam completion
			// allotedDuration in seconds
			// assessmentcode may differ for candidates hence fetching each time br rules
			String assessmentCode = bca.getAssessmentCode();
			Map<String, Object> brRules = brlrService.getBrMapForBatchAndAssessment(batchCode, assessmentCode);
			int allotedDuration =
					((Double) brRules.get(ACSConstants.ASSESSMENT_DURATION_BR_PROPERTY)).intValue() * 60;
			//
			Calendar duration = (Calendar) postponeCandidateBatchstartDate.clone();
			duration.add(Calendar.SECOND, allotedDuration);
			// if candidate not finishes exam by batch end time then extend batch time by delta mins
			if (duration.after(acsBatch.getMaxBatchEndTime())) {
				int minsToExtend =
						(int) ((duration.getTimeInMillis() - acsBatch.getMaxBatchEndTime().getTimeInMillis()) / 60000);
				if (minsToExtend > minsToExtendBatch)
					minsToExtendBatch = minsToExtend;

				// extend once for batch at the end =>
				isExtendBatch = true;
			}
			// just extend batch start time
			bca.setExtendedBatchStartTimePerCandidate(postponeCandidateBatchstartDate);
			// don't update now
			// batchCandidateAssociationService.updateBatchCandidateAssociation(bca);

			candidateBatchTimeExtensionDOs.add(candidateLateLoginTimeExtensionDO);
		}
		// extend batch
		if (isExtendBatch) {
			BatchExtensionDO batchExtensionDO = getBatchExtensionEntity(acsBatch);
			// set delta mins
			// add buffer time to extend batch end time, coz candidate may crash and login, hence buffer time
			int buffertime = acsPropertyUtil.getBufferTimeBatchEndTime();
			minsToExtendBatch = minsToExtendBatch + buffertime;
			batchExtensionDO.setExtendedTimeInmins(minsToExtendBatch);
			// extend batch end time
			batchExtensionDO = extendBatchEndTimeWithPostpone(batchExtensionDO, acsBatch,
					ACSConstants.DEFAULT_ADMIN_USER_NAME,
					ACSConstants.DEFAULT_ADMIN_HOST_ADDRESS);
			if (batchExtensionDO.getStatus().equals(ACSConstants.MSG_FAILURE)) {
				candidateExtensionWrapperDO =
						new CandidateExtensionWrapperDO(ACSConstants.MSG_FAILURE, batchExtensionDO.getErrorMessage(),
								listOfbcaIds);
				List<CandidateLateLoginTimeExtensionDO> candidateExtensionDOs = new ArrayList<>();
				for (CandidateLateLoginTimeExtensionDO candidateExtensionDO : candidateBatchTimeExtensionDOs) {
					candidateExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
					candidateExtensionDOs.add(candidateExtensionDO);
				}
				candidateExtensionWrapperDO.setCandidateLateLoginTimeExtensionDO(candidateExtensionDOs);
				return candidateExtensionWrapperDO;
			}
		}
		// initiate late login for candidadtes
		candidateBatchTimeExtensionDOs = candidateLateLoginTimeExtensionService
				.extendCandidateLateLoginTimeAndUpdatePostponeBatchStartOnBCAIdS(acsBatch, bcas, candidateBatchTimeExtensionDOs);
		candidateExtensionWrapperDO =
				new CandidateExtensionWrapperDO(ACSConstants.MSG_SUCCESS, "Successfully Processed", listOfbcaIds);
		candidateExtensionWrapperDO.setCandidateLateLoginTimeExtensionDO(candidateBatchTimeExtensionDOs);
		return candidateExtensionWrapperDO;
	}

	/**
	 * @param acsBatch
	 * @param bca
	 * @param candidateLateLoginTimeExtensionDO
	 * @param currentTime
	 * @param postponeCandidateBatchstartDate
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public CandidateLateLoginTimeExtensionDO validationsForCandidatePostpone(AcsBatch acsBatch,
			AcsBatchCandidateAssociation bca, CandidateLateLoginTimeExtensionDO candidateLateLoginTimeExtensionDO,
			Calendar currentTime, Calendar postponeCandidateBatchstartDate) {
		if (postponeCandidateBatchstartDate.before(currentTime)) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO.setErrorMessage("The proposed time is already expired");
			return candidateLateLoginTimeExtensionDO;
		}
		if (postponeCandidateBatchstartDate.before(acsBatch.getMaxBatchStartTime())) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO.setErrorMessage("Invalid Date");
			return candidateLateLoginTimeExtensionDO;
		}
		Calendar startTime = authService.getMaxCandidateBatchStartTime(bca.getExtendedBatchStartTimePerCandidate(),
				acsBatch.getMaxBatchStartTime());
		startTime.clear(Calendar.MILLISECOND);
		if (postponeCandidateBatchstartDate.before(startTime)
				|| postponeCandidateBatchstartDate.compareTo(startTime) == 0) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO.setErrorMessage("Invalid time,cannot process request");
			return candidateLateLoginTimeExtensionDO;
		}
		// validate
		if (bca.getCandidateType().name().equals(CandidateType.WALKIN)) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO
					.setErrorMessage("Extension will not be applicable only Walkin candidates");
			return candidateLateLoginTimeExtensionDO;
		}
		if (bca.getActualTestStartedTime() != null) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO
					.setErrorMessage("Extension will not be applicable for exam started candidates");
			return candidateLateLoginTimeExtensionDO;
		}
		if (bca.getActualTestEndTime() != null) {
			candidateLateLoginTimeExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			candidateLateLoginTimeExtensionDO
					.setErrorMessage("Extension will not be applicable for exam ended candidates");
			return candidateLateLoginTimeExtensionDO;
		}
		return candidateLateLoginTimeExtensionDO;
	}


	/**
	 * @param batchExtensionDO
	 * @param batchDetailsTO
	 * @param userName
	 * @param ipAddress
	 * @return
	 * @throws GenericDataModelException
	 * @throws CandidateRejectedException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public BatchExtensionDO extendBatchEndTimeWithPostpone(BatchExtensionDO batchExtensionDO, AcsBatch batchDetailsTO,
			String userName, String ipAddress) throws GenericDataModelException, CandidateRejectedException {
		logger.info("initiated extendBatchEndTime where batchExtensionDOs= {}", batchExtensionDO.toString());

		// list of batch extension info's which will be intimated to CDB
		Map<String, BatchExtensionInformationEntity> batchExtensionInfoMap =
				new HashMap<String, BatchExtensionInformationEntity>();

		boolean isRescheduleRequired = false;

		// handling invalid input
		if (batchExtensionDO.getExtendedTimeInmins() <= 0) {
			logger.debug(
					"Invalid input (value greater than 0 is expected for batch extension) = {} to extend for batch code = {}",
					batchExtensionDO.getExtendedTimeInmins(), batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("Invalid input (value greater than 0 is expected for batch extension)");
			return batchExtensionDO;
		}

		// calculate extended batch end time based on extended time in mins
		Calendar prevMaxBatchEndTime =
				TimeUtil.convertStringToCalender(batchExtensionDO.getMaxBatchEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS);
		Calendar extendedBatchEndTime = (Calendar) prevMaxBatchEndTime.clone();
		if (extendedBatchEndTime == null) {
			logger.debug(
					"unable to convert current max batch end time to calender object, to extend for batch code = {}",
					batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("unable to convert current max batch end time to calender object");
			return batchExtensionDO;
		}
		extendedBatchEndTime.add(Calendar.MINUTE, batchExtensionDO.getExtendedTimeInmins());

		logger.debug("prevMaxBatchEndTime = {} , extendedBatchEndTime = {}, for batch code = {}",
				prevMaxBatchEndTime.getTime(), extendedBatchEndTime.getTime(), batchExtensionDO.getBatchCode());

		// check whether extendedBatchEndTime is within current time or not
		if (extendedBatchEndTime.before(Calendar.getInstance())) {
			logger.debug("Extended batch end time is elapsed : = {}, for batch code = {}",
					extendedBatchEndTime.getTime(), batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("Extended batch end time is elapsed : " + TimeUtil
					.convertTimeAsString(extendedBatchEndTime.getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
			return batchExtensionDO;
		}

		// get the batchCode
		String batchCode = batchExtensionDO.getBatchCode();

		AcsEvent eventDetails = cdeaService.getEventDetailsByBatchCode(batchCode);
		logger.info("eventDetails = {} for batch with id = {}", eventDetails, batchCode);

		// check whether extendedBatchEndTime is within event end time or not
		if (extendedBatchEndTime.after(eventDetails.getEventEndDate())) {
			logger.debug("Batch extension not allowed after Event End Date :  {}, for batch code = {}",
					eventDetails.getEventEndDate().getTime(), batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage(
					"Batch extension not allowed after Event End Date : " + TimeUtil.convertTimeAsString(
							eventDetails.getEventEndDate().getTimeInMillis(), TimeUtil.YYYY_MM_DD_HH_MM_SS));
			return batchExtensionDO;
		}

		logger.info("batchdetails = {} for batch with id = {}", batchDetailsTO, batchCode);

		// check whether new late login time is within delat rpack generation time or not
		if (batchDetailsTO.getMaxDeltaRpackGenerationTime().before(Calendar.getInstance())) {
			logger.debug("Batch extension not allowed after delta rpack generation time : {}, for batch code = {}",
					batchDetailsTO.getMaxDeltaRpackGenerationTime().getTime(), batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage("Batch extension not allowed after delta rpack generation time : "
					+ TimeUtil.convertTimeAsString(batchDetailsTO.getMaxDeltaRpackGenerationTime().getTimeInMillis(),
							TimeUtil.YYYY_MM_DD_HH_MM_SS));
			return batchExtensionDO;
		}

		// check whether a value greater than the specified time exist or not
		if (batchDetailsTO.getMaxBatchEndTime().after(extendedBatchEndTime)) {
			logger.debug(
					"Batch extension not allowed because value(extension time) greater than specified time exists: {}, for batch code = {}",
					batchDetailsTO.getMaxBatchEndTime().getTime(), batchExtensionDO.getBatchCode());
			batchExtensionDO.setStatus(ACSConstants.MSG_FAILURE);
			batchExtensionDO.setErrorMessage(
					"Batch extension not allowed because value(extension time) greater than specified time exists: "
							+ TimeUtil.convertTimeAsString(batchDetailsTO.getMaxBatchEndTime().getTimeInMillis(),
									TimeUtil.YYYY_MM_DD_HH_MM_SS));
			return batchExtensionDO;
		}

		// check whether extended batch end time is going beyond delta rpack generation time or not if so reschedule
		// the necessary jobs accordingly based on the flag
		if (batchDetailsTO.getMaxDeltaRpackGenerationTime().before(extendedBatchEndTime)) {
			isRescheduleRequired = true;
		}

		// audit in incident log
		IncidentAuditActionDO incidentAuditAction = new IncidentAuditActionDO(ipAddress,
				TimeUtil.convertTimeAsString(Calendar.getInstance().getTimeInMillis()),
				IncidentAuditLogEnum.BATCH_EXTENSION_AT_ACS, batchExtensionDO.getBatchCode(),
				batchExtensionDO.getMaxBatchEndTime(),
				TimeUtil.convertTimeAsString(extendedBatchEndTime.getTimeInMillis()),
				batchExtensionDO.getExtendedTimeInmins());
		AuditTrailLogger.incidentAudit(incidentAuditAction);

		// admin audit
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			Object[] paramArray = { batchExtensionDO.getBatchCode(), batchExtensionDO.getExtendedTimeInmins(),
					batchExtensionDO.getMaxBatchEndTime(),
					TimeUtil.convertTimeAsString(extendedBatchEndTime.getTimeInMillis()) };

			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
					AdminAuditActionEnum.BATCH_EXTENSION_AT_ACS.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");

			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_BATCH_EXTENSION_AT_ACS_MSG, paramArray);
		}

		batchDetailsTO.setIsBatchExtended(BatchExtensionEnum.BATCH_EXTENSION_AT_ACS);
		batchDetailsTO.setBatchEndTimeByAcs(extendedBatchEndTime);

		// check whether the delta rpack generation time at ACS is behind extendedBatchEndTime then update it
		if (isRescheduleRequired
				|| batchDetailsTO.getDeltaRpackGenerationTimeByAcs() == null) {
			// set max of delta rpack gen time
			if (batchDetailsTO.getMaxDeltaRpackGenerationTime().before(extendedBatchEndTime))
			batchDetailsTO.setDeltaRpackGenerationTimeByAcs(extendedBatchEndTime);
		}

		batchDetailsTO = batchService.updateBatchDetails(batchDetailsTO, true, null);

		if (isRescheduleRequired) {
			// reschedule all the necessary jobs
			candidateService.startCandidateForceSubmitJob(batchDetailsTO.getDeltaRpackGenerationTimeByAcs(),
					batchDetailsTO.getBatchCode(), false);
			candidateService.startCandidtaeLiveStatusInitiator(batchDetailsTO.getBatchCode(),
					batchDetailsTO.getMaxBatchStartTime(), batchDetailsTO.getDeltaRpackGenerationTimeByAcs(), false);
		}

		batchExtensionDO.setStatus(ACSConstants.MSG_SUCCESS);

		logger.debug("Batch extension is success: {}, for batch code = {}", batchExtensionDO.getBatchCode());

		BatchExtensionInformationEntity batchExtensionInformationEntity =
				cdeaService.getCustomerDivisionEventCodesByBatchCode(batchCode);
		if (batchExtensionInformationEntity != null) {
			batchExtensionInformationEntity.setBatchCode(batchDetailsTO.getBatchCode());
			batchExtensionInformationEntity.setBatchStartTime(batchDetailsTO.getMaxBatchStartTime());
			batchExtensionInformationEntity.setPrevBatchEndTime(prevMaxBatchEndTime);
			batchExtensionInformationEntity.setCurrentBatchEndTime(extendedBatchEndTime);

			batchExtensionInfoMap.put(batchCode, batchExtensionInformationEntity);
		} else {
			logger.info("unable to fetch CDE information for the specified batchId = {}", batchCode);
		}

		if (!batchExtensionInfoMap.isEmpty()) {
			// iterate over the map and reshedule the dependent jobs accordingly
			for (Map.Entry<String, BatchExtensionInformationEntity> entry : batchExtensionInfoMap.entrySet()) {
				// initiate batch extension information notifier job
				startBatchExtensionInfoNotificationInitiator(entry.getKey(), entry.getValue());
			}
		}
		return batchExtensionDO;
	}


}
