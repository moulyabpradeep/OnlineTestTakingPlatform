package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

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
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.quartz.jobs.PackNotificationer;
import com.merittrac.apollo.cdb.entity.PackActivatedStatus;
import com.merittrac.apollo.cdb.entity.PackNotificationEntity;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.DownloadStatus;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.ACSPackResponseBean;
import com.merittrac.apollo.common.entities.packgen.DownloadStatusBean;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * Api's related to notifying and updating the pack status.
 * 
 * @author siva_k - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class PackStatusService implements IPackStatusService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static ACSService as = null;
	private static CDEAService cdeaService = null;
	static ACSRequestsService ars = null;
	static ACSEventRequestsService acsEventRequestsService = null;
	static PackDetailsService packDetailsService = null;

	static {
		if (as == null)
			as = new ACSService();
		if (cdeaService == null) {
			cdeaService = new CDEAService();
		}
		if (ars == null)
			ars = new ACSRequestsService();
		if (packDetailsService == null)
			packDetailsService = new PackDetailsService();
		if (acsEventRequestsService == null)
			acsEventRequestsService = new ACSEventRequestsService();
	}

	public PackStatusService() {
	}

	@Override
	public boolean updatePackStatus(PackContent packType, String batchCode, PacksStatusEnum packStatus,
			String errorMessage, String packIdentifier) throws GenericDataModelException {
		logger.info(
				"updatePackStatus is initiated with packType:{} batchCode:{} PackStatus:{} errorMessage:{} packIdentifier:{}",
				packType, batchCode, packStatus, errorMessage, packIdentifier);

		// get assessment server code
		String assessmentServerCode = cdeaService.getACSCodeByBatchCode(batchCode);

		// get list of enabled status values for cdb pack notification
		List<PacksStatusEnum> packsStatusEnums = getEnabledStatusValuesForCDBPackNotification();

		// Iterate over the list of batchDetails objects and update the corresponding pack status.
		List<String> batchCodes = new ArrayList<>();
		batchCodes.add(batchCode);

		packDetailsService.updatePackStatusByPackIdentifier(packIdentifier, packStatus, errorMessage);
		// TODO: Get packs in parent method
		AcsPacks acsPacks = packDetailsService.getPackDetailsbyPackIdentifier(packIdentifier);
		if (acsPacks == null) {
			logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
			return false;
		}
		if (packsStatusEnums.contains(packStatus) && !packType.equals(PackContent.Rpack)) {
			PackNotificationEntity packNotificationEntity =
					createPackNotificationEntity(acsPacks, batchCodes, assessmentServerCode);

			startPackNotificationInitator(acsPacks.getPackIdentifier(), packNotificationEntity, packType);
		}
		logger.info("Updated Pack. packIdentifier:{} of type = {} status to = {}", packIdentifier, packType, packStatus);
		return true;
	}

	// Notifies DM about status of the pack.
	public boolean notifyPackStatusToDM(String packIdentifier, DownloadStatus status, String versionNumber) {
		logger.info("initiated notifyPackStatusToDM where packIdentifier={}, status={} and version={}", packIdentifier,
				status, versionNumber);
		DownloadStatusBean downloadStatusBean = new DownloadStatusBean();
		downloadStatusBean.setStatus(status);
		downloadStatusBean.setDeliveryPackID(Integer.parseInt(packIdentifier));
		downloadStatusBean.setVersionNumber(Integer.parseInt(versionNumber));
		as.sendAcknowledgement(new Gson().toJson(downloadStatusBean));
		return true;
	}

	/**
	 * The startPackNotificationInitator API is used to start the job for generating the packs related data required for
	 * CDB.
	 * 
	 * RESTful URI for initiating this job : http://localhost:8090/mtacs/rest/acs/startPackStatusNotifier(URI to start
	 * PackStatusNotifier job)
	 */
	public boolean startPackNotificationInitator(String packIdentifier, PackNotificationEntity packNotificationEntity,
			PackContent packType) throws GenericDataModelException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;

		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			switch (packType) {
				case Apack:

					String APACK_NOTIFIER_NAME =
							ACSConstants.APACK_NOTIFIER_NAME + "_" + packIdentifier + "_"
									+ packNotificationEntity.getVersionNumber() + "_"
									+ packNotificationEntity.getActivatedStatus();
					String APACK_NOTIFIER_GRP = ACSConstants.APACK_NOTIFIER_GRP;

					job =
							JobBuilder.newJob(PackNotificationer.class)
									.withIdentity(APACK_NOTIFIER_NAME, APACK_NOTIFIER_GRP).storeDurably(false)
									// .requestRecovery(true)
									.build();
					job.getJobDataMap().put(ACSConstants.PACK_ID, packIdentifier);
					job.getJobDataMap().put(ACSConstants.PACK_NOTIFICATION_ENTITY, packNotificationEntity);

					trigger = scheduler.getTrigger(TriggerKey.triggerKey(APACK_NOTIFIER_NAME, APACK_NOTIFIER_GRP));

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for notifying Apack with specified packId = {}, hence unscheduling the existing trigger and rescheduling with updated status",
								packIdentifier);
						scheduler.unscheduleJob(TriggerKey.triggerKey(APACK_NOTIFIER_NAME, APACK_NOTIFIER_GRP));
					}
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(APACK_NOTIFIER_NAME, APACK_NOTIFIER_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionNextWithRemainingCount()).startNow()
									.build();
					logger.trace("Trigger for APack Notificationer = {} " + trigger);
					scheduler.scheduleJob(job, trigger);

					break;

				case Bpack:

					String BPACK_NOTIFIER_NAME =
							ACSConstants.BPACK_NOTIFIER_NAME + "_" + packIdentifier + "_"
									+ packNotificationEntity.getVersionNumber() + "_"
									+ packNotificationEntity.getActivatedStatus();
					String BPACK_NOTIFIER_GRP = ACSConstants.BPACK_NOTIFIER_GRP;

					job =
							JobBuilder.newJob(PackNotificationer.class)
									.withIdentity(BPACK_NOTIFIER_NAME, BPACK_NOTIFIER_GRP).storeDurably(false)
									// .requestRecovery(true)
									.build();
					job.getJobDataMap().put(ACSConstants.PACK_ID, packIdentifier);
					job.getJobDataMap().put(ACSConstants.PACK_NOTIFICATION_ENTITY, packNotificationEntity);

					trigger = scheduler.getTrigger(TriggerKey.triggerKey(BPACK_NOTIFIER_NAME, BPACK_NOTIFIER_GRP));

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for notifying Bpack with specified packId = {}, hence unscheduling the existing trigger and rescheduling with updated status",
								packIdentifier);
						scheduler.unscheduleJob(TriggerKey.triggerKey(BPACK_NOTIFIER_NAME, BPACK_NOTIFIER_GRP));
					}
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(BPACK_NOTIFIER_NAME, BPACK_NOTIFIER_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionNextWithRemainingCount()).startNow()
									.build();
					logger.trace("Trigger for BPack Notificationer = {} " + trigger);
					scheduler.scheduleJob(job, trigger);

					break;

				case Qpack:

					String QPACK_NOTIFIER_NAME =
							ACSConstants.QPACK_NOTIFIER_NAME + "_" + packIdentifier + "_"
									+ packNotificationEntity.getVersionNumber() + "_"
									+ packNotificationEntity.getActivatedStatus();
					String QPACK_NOTIFIER_GRP = ACSConstants.QPACK_NOTIFIER_GRP;

					job =
							JobBuilder.newJob(PackNotificationer.class)
									.withIdentity(QPACK_NOTIFIER_NAME, QPACK_NOTIFIER_GRP).storeDurably(false)
									// .requestRecovery(true)
									.build();
					job.getJobDataMap().put(ACSConstants.PACK_ID, packIdentifier);
					job.getJobDataMap().put(ACSConstants.PACK_NOTIFICATION_ENTITY, packNotificationEntity);

					trigger = scheduler.getTrigger(TriggerKey.triggerKey(QPACK_NOTIFIER_NAME, QPACK_NOTIFIER_GRP));

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for notifying Qpack with specified packId = {}, hence unscheduling the existing trigger and rescheduling with updated status",
								packIdentifier);
						scheduler.unscheduleJob(TriggerKey.triggerKey(QPACK_NOTIFIER_NAME, QPACK_NOTIFIER_GRP));
					}
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(QPACK_NOTIFIER_NAME, QPACK_NOTIFIER_GRP)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionNextWithRemainingCount()).startNow()
									.build();
					logger.trace("Trigger for QPack Notificationer = {} " + trigger);
					scheduler.scheduleJob(job, trigger);

					break;

				default:
					logger.info("No such packType = {} specified", packType);
					break;
			}
		} catch (SchedulerException ex) {
			logger.error("SchedulerException while executing startPackNotificationInitator ...", ex);
			return false;
		}
		return true;
	}

	public AcsPacks updatePackStatus(AcsPacks packDetails) {
		Calendar currentTime = Calendar.getInstance();
		switch (packDetails.getPackStatus()) {
			case DOWNLOAD_IN_PROGRESS:
				packDetails.setActualDownloadStartTime(currentTime);
				break;
			case DOWNLOADED:
				packDetails.setActualDownloadEndTime(currentTime);
				break;
			case ACTIVATION_IN_PROGRESS:
				packDetails.setActualActivationStartTime(currentTime);
				break;
			case ACTIVATED:
				packDetails.setActualActivationEndTime(currentTime);
				break;
			case UPLOAD_IN_PROGRESS:
				packDetails.setActualDownloadStartTime(currentTime);
				break;
			case UPLOADED:
				packDetails.setActualDownloadEndTime(currentTime);
				break;
			case DOWNLOAD_FAILED:
				packDetails.setActualDownloadEndTime(currentTime);
				break;
			case ACTIVATION_FAILED:
				packDetails.setActualActivationEndTime(currentTime);
				break;

			default:
				break;
		}
		return packDetails;
	}

	public PackNotificationEntity createPackNotificationEntity(AcsPacks packDetailsTO, List<String> batchCodes,
			String assessmentServerCode) {
		logger.info(
				"initiated createPackNotificationEntity where packDetailsTO={},batchCodes={} and assessmentCode={}",
				packDetailsTO, batchCodes, assessmentServerCode);

		PackNotificationEntity packNotificationEntity = new PackNotificationEntity();
		packNotificationEntity.setGenerationTime(Calendar.getInstance().getTimeInMillis());
		packNotificationEntity.setPackCode(packDetailsTO.getPackIdentifier());
		packNotificationEntity.setVersionNumber(packDetailsTO.getVersionNumber().toString());
		packNotificationEntity.setAssessmentServerCode(assessmentServerCode);
		packNotificationEntity.setPackType(packDetailsTO.getPackType());
		packNotificationEntity.setBatchCode(new HashSet<String>(batchCodes));

		packNotificationEntity.setDispatchedTime(packDetailsTO.getActualDownloadStartTime());
		packNotificationEntity.setReceivedTime(packDetailsTO.getActualDownloadEndTime());
		packNotificationEntity.setActivatedTime(packDetailsTO.getActualActivationEndTime());
		packNotificationEntity.setActivatedStatus(getPackStatusForCDBPackNotification(packDetailsTO.getPackStatus()));

		logger.info("returning packNotificationEntity= {}", packNotificationEntity);
		return packNotificationEntity;
	}

	public List<PacksStatusEnum> getEnabledStatusValuesForCDBPackNotification() {
		List<PacksStatusEnum> packsStatusEnums = new ArrayList<PacksStatusEnum>();
		packsStatusEnums.add(PacksStatusEnum.DOWNLOAD_IN_PROGRESS);
		packsStatusEnums.add(PacksStatusEnum.DOWNLOADED);
		packsStatusEnums.add(PacksStatusEnum.DOWNLOAD_FAILED);
		packsStatusEnums.add(PacksStatusEnum.ACTIVATION_IN_PROGRESS);
		packsStatusEnums.add(PacksStatusEnum.ACTIVATED);
		packsStatusEnums.add(PacksStatusEnum.ACTIVATION_FAILED);

		return packsStatusEnums;
	}

	public PackActivatedStatus getPackStatusForCDBPackNotification(PacksStatusEnum packStatus) {
		PackActivatedStatus packActivatedStatus = null;
		switch (packStatus) {
			case DOWNLOAD_IN_PROGRESS:
				packActivatedStatus = PackActivatedStatus.DOWNLOAD_IN_PROGRESS;
				break;
			case DOWNLOADED:
			case PASSWORD_FETCHED:
			case PASSWORD_FETCHING_FAILED:
			case PASSWORDFETCH_IN_PROGRESS:
				packActivatedStatus = PackActivatedStatus.DOWNLOAD_SUCCESS;
				break;
			case DOWNLOAD_FAILED:
				packActivatedStatus = PackActivatedStatus.DOWNLOAD_FAILURE;
				break;
			case ACTIVATION_IN_PROGRESS:
				packActivatedStatus = PackActivatedStatus.ACTIVATION_IN_PROGRESS;
				break;
			case ACTIVATED:
				packActivatedStatus = PackActivatedStatus.ACTIVATION_SUCCESS;
				break;
			case ACTIVATION_FAILED:
				packActivatedStatus = PackActivatedStatus.ACTIVATION_FAILURE;
				break;
			default:
				logger.info(
						"currently cdb pack notification is not handled for status = {}, hence considering default status = {}",
						packStatus, PackActivatedStatus.YET_TO_BE_ACTIVATED);
				packActivatedStatus = PackActivatedStatus.YET_TO_BE_ACTIVATED;
				break;
		}
		return packActivatedStatus;
	}

	// Notifies DM about status of the pack.
	public boolean notifyACSPackResponseToDM(ACSPackResponseBean acsPackResponseBean) {
		logger.info("initiated notifyACSPackResponseToDM where ACSPackResponseBean={}", acsPackResponseBean);
		boolean isNotificationSuccess = false;
		isNotificationSuccess = as.notifyACSPackResponseToDM(new Gson().toJson(acsPackResponseBean));
		for (Long requestId : acsPackResponseBean.getResponseMap().keySet()) {
			logger.info("Marking IsCommunicatedToDx of requestId: {} as {}", requestId, isNotificationSuccess);
			ars.updateRequestActionIsCommunicatedToDx(requestId, isNotificationSuccess);
		}
		if (acsPackResponseBean.getEventActionResponseMap() != null) {
			for (Long requestId : acsPackResponseBean.getEventActionResponseMap().keySet()) {
				logger.info("Marking Event Rules IsCommunicatedToDx of requestId: {} as {}", requestId,
						isNotificationSuccess);
				acsEventRequestsService.updateEventRequestActionIsCommunicatedToDx(requestId, isNotificationSuccess);
			}
		}
		return true;
	}
}
