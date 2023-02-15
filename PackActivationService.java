package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.PasswordRequestStatusEnum;
import com.merittrac.apollo.acs.dataobject.ActivationRequestDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.exception.ActivationInitiationFailedException;
import com.merittrac.apollo.acs.quartz.jobs.APackActivator;
import com.merittrac.apollo.acs.quartz.jobs.BPackActivator;
import com.merittrac.apollo.acs.quartz.jobs.QPackActivator;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.PackEntity;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * This class contain api's related to initiating pack activation process.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class PackActivationService implements IPackActivationService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	static PackDetailsService pdes = null;
	static PackStatusService pss = null;
	static BatchService batchService = null;

	private static void initialize() {
		if (batchService == null)
			batchService = BatchService.getInstance();
		if (pdes == null)
			pdes = new PackDetailsService();
		if (pss == null)
			pss = new PackStatusService();
	}

	public PackActivationService() {
		initialize();
	}

	@Override
	public boolean activateBPack(BpackExportEntity bpackExportEntity, String batchCode, String packIdentifier)
			throws ActivationInitiationFailedException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String PACK_ACTIVATION_NAME = ACSConstants.BPACK_ACTIVATION_NAME;
		String PACK_ACTIVATION_GRP = ACSConstants.BPACK_ACTIVATION_GRP;
		String packCode = bpackExportEntity.getPackCode();
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP);

			trigger = scheduler.getTrigger(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP));

			if (trigger != null) {
				TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
				logger.info("Trigger state of trigger {} is {}", trigger.getKey(), triggerState);
				if (!triggerState.equals(TriggerState.COMPLETE)) {
					if (trigger.getStartTime().compareTo(new Date()) > 0) {
						logger.info(
								"Already a trigger exists for activating Bpack with specified packCode = {}, so unscheduling the existing trigger and initiating the new trigger",
								packCode);
						scheduler.unscheduleJob(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packCode,
								PACK_ACTIVATION_GRP));
					} else {
						logger.info("previous version of bpack activation is in process,hence skipping new or same version please try after some time");

						JobDetail jobDetail = scheduler.getJobDetail(jobKey);
						logger.trace("Recieved Job which contain JobKey = {} ", jobKey);
						Map<String, Object> map = jobDetail.getJobDataMap();
						if (map != null) {
							BpackExportEntity exportEntity =
									(BpackExportEntity) map.get(ACSConstants.BPACK_EXPORT_ENTITY);
							if (exportEntity != null
									&& exportEntity.getVersionNumber().equals(bpackExportEntity.getVersionNumber())) {
								return false;
							}
						}

						throw new ActivationInitiationFailedException(
								ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
								"previous version of bpack activation is in process,hence skipping new or same version please try after some time");
					}
				} else if (trigger != null && triggerState.equals(TriggerState.COMPLETE)) {
					logger.debug("Deleting completed job:{}", jobKey);
					scheduler.deleteJob(jobKey);

				}
			}

			job =
					JobBuilder.newJob(BPackActivator.class)
							.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BPACK_EXPORT_ENTITY, bpackExportEntity);
			job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
			job.getJobDataMap().put(ACSConstants.PACK_TYPE_CONST, PackContent.Bpack);
			job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);

			if (bpackExportEntity.getActivationTime() != null
					&& bpackExportEntity.getActivationTime().after(Calendar.getInstance())) {
				// Trigger which fires according to the time specified.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.BPACK_ACTIVATION_JOB_PRIORITY)
								.startAt(bpackExportEntity.getActivationTime().getTime()).build();
			} else {
				// Trigger which starts the job immediately only if activation time is null.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.BPACK_ACTIVATION_JOB_PRIORITY).startNow().build();
			}

			scheduler.scheduleJob(job, trigger);
			logger.trace("Trigger for activating bpack = {} " + trigger);

			pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_START_TRIGGERED, packIdentifier);
		} catch (SchedulerException | GenericDataModelException ex) {
			logger.error("Exception while executing activateBPack...", ex);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_FAILED, packIdentifier);
				pss.updatePackStatus(PackContent.Bpack, batchCode, PacksStatusEnum.ACTIVATION_FAILED, null,
						packIdentifier);
			} catch (Exception e) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { e,
						packCode, PacksStatusEnum.ACTIVATION_FAILED });
			}
			return false;
		} catch (ActivationInitiationFailedException e) {
			logger.error("Exception while executing activateBPack...", e);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_INITIATION_FAILED, packIdentifier);
				// pss.updatePackStatus(RequestAction.Bpack, batchCodesList, PacksStatusEnum.ACTIVATION_FAILED, null,
				// bpackExportEntity.getPackCode());
			} catch (Exception ex) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { ex,
						packCode, PacksStatusEnum.ACTIVATION_INITIATION_FAILED });
			}
			throw new ActivationInitiationFailedException(ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
					"previous version of bpack activation is in process,hence skipping new or same version please try after some time");
		}
		return true;
	}

	@Override
	public boolean activateAPack(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			String batchCode, String packIdentifier) throws ActivationInitiationFailedException {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;
		String PACK_ACTIVATION_NAME = ACSConstants.APACK_ACTIVATION_NAME;
		String PACK_ACTIVATION_GRP = ACSConstants.APACK_ACTIVATION_GRP;
		String packCode = apackExportEntity.getPackCode();
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP);

			trigger = scheduler.getTrigger(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP));

			if (trigger != null) {
				TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
				if (!triggerState.equals(TriggerState.COMPLETE)) {
					if (trigger.getStartTime().compareTo(new Date()) > 0) {
						logger.info(
								"Already a trigger exists for activating Apack with specified packCode = {}, so unscheduling the existing trigger and initiating the new trigger",
								apackExportEntity.getPackCode());
						scheduler.unscheduleJob(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packCode,
								PACK_ACTIVATION_GRP));
					} else {
						logger.info("previous version of apack activation is in process,hence skipping new or same version please try after some time");

						JobDetail jobDetail = scheduler.getJobDetail(jobKey);
						logger.trace("Recieved Job which contain JobKey = {} ", jobKey);
						Map<String, Object> map = jobDetail.getJobDataMap();
						if (map != null) {
							ApackExportEntity exportEntity =
									(ApackExportEntity) map.get(ACSConstants.APACK_EXPORT_ENTITY);
							if (exportEntity != null
									&& exportEntity.getVersionNumber().equals(apackExportEntity.getVersionNumber())) {
								return false;
							}
						}

						throw new ActivationInitiationFailedException(
								ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
								"previous version of apack activation is in process,hence skipping new or same version please try after some time");
					}
				} else if (trigger != null && triggerState.equals(TriggerState.COMPLETE)) {
					logger.debug("Deleting completed job:{}", jobKey);
					scheduler.deleteJob(jobKey);

				}
			}
			if (apackExportEntity.getActivationTime() != null
					&& apackExportEntity.getActivationTime().after(Calendar.getInstance())) {
				// Trigger which fires according to the time specified.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.APACK_ACTIVATION_JOB_PRIORITY)
								.startAt(apackExportEntity.getActivationTime().getTime()).build();
			} else {
				// Trigger which starts the job immediately only if activation time is null.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.APACK_ACTIVATION_JOB_PRIORITY).startNow().build();
			}

			job =
					JobBuilder.newJob(APackActivator.class)
							.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.APACK_EXPORT_ENTITY, apackExportEntity);
			job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
			job.getJobDataMap().put(ACSConstants.PACK_TYPE_CONST, PackContent.Apack);
			job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);

			scheduler.scheduleJob(job, trigger);
			logger.trace("Trigger for activating apack = {} " + trigger);

			pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_START_TRIGGERED, packIdentifier);
		} catch (SchedulerException | GenericDataModelException ex) {
			logger.error("Exception while executing activateAPack...", ex);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_FAILED, packIdentifier);
				pss.updatePackStatus(PackContent.Apack, batchCode, PacksStatusEnum.ACTIVATION_FAILED, null,
						packIdentifier);
			} catch (Exception e) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { e,
						packCode, PacksStatusEnum.ACTIVATION_FAILED });
			}
			return false;
		} catch (ActivationInitiationFailedException e) {
			logger.error("Exception while executing activateAPack...", e);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_INITIATION_FAILED, packIdentifier);
				// pss.updatePackStatus(RequestAction.Apack, batchCodesList, PacksStatusEnum.ACTIVATION_FAILED, null,
				// apackExportEntity.getPackCode());
			} catch (Exception ex) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { ex,
						packCode, PacksStatusEnum.ACTIVATION_INITIATION_FAILED });
			}
			throw new ActivationInitiationFailedException(ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
					"previous version of apack activation is in process,hence skipping new or same version please try after some time");
		}
		return true;
	}

	/*
	 * Method takes care of download and activation of QPack. Will initiate a quartz job which will take care of both.
	 * 
	 * @see com.merittrac.apollo.acs.services.IQPackService#downloadQPack()
	 * 
	 * TODO :: Passing the right entity job to the method and subsequently passing right data to job data map.
	 */
	@Override
	public boolean activateQPack(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity,
			String batchCode, String packIdentifier,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaData,
			boolean isforcedActivation) throws ActivationInitiationFailedException {
		JobDetail job = null;
		Scheduler scheduler = null;
		String PACK_ACTIVATION_NAME = ACSConstants.QPACK_ACTIVATION_NAME;
		String PACK_ACTIVATION_GRP = ACSConstants.QPACK_ACTIVATION_GRP;
		String packCode = qpackExportEntity.getPackCode();
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP);
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
			if (triggers != null && !triggers.isEmpty()) {
				for (Iterator<Trigger> iterator = triggers.iterator(); iterator.hasNext();) {
					Trigger trigger = (Trigger) iterator.next();
					TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
					logger.info("Trigger state of trigger {} is {}", trigger.getKey(), triggerState);
					if (trigger != null && !triggerState.equals(TriggerState.COMPLETE)) {
						if (trigger.getStartTime().compareTo(new Date()) > 0) {
							logger.info(
									"Already a trigger exists for activating Qpack with specified packCode = {}, so unscheduling the existing trigger and initiating the new trigger",
									qpackExportEntity.getPackCode());
							scheduler.unscheduleJob(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packCode,
									PACK_ACTIVATION_GRP));
						} else {
							logger.info("previous version of qpack activation is in process,hence skipping new or same version please try after some time");

							JobDetail jobDetail = scheduler.getJobDetail(jobKey);
							logger.trace("Recieved Job which contain JobKey = {} ", jobKey);
							Map<String, Object> map = jobDetail.getJobDataMap();
							if (map != null) {
								com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity exportEntity =
										(com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity) map
												.get(ACSConstants.QPACK_EXPORT_ENTITY);
								if (exportEntity != null
										&& exportEntity.getVersionNumber().equalsIgnoreCase(
												qpackExportEntity.getVersionNumber())) {
									return false;
								}
							}
							throw new ActivationInitiationFailedException(
									ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
									"previous version of qpack activation is in process,hence skipping new or same version please try after some time");
						}
					} else if (trigger != null && triggerState.equals(TriggerState.COMPLETE)) {
						logger.debug("Deleting completed job:{}", jobKey);
						scheduler.deleteJob(jobKey);

					}
				}
			}

			job =
					JobBuilder.newJob(QPackActivator.class)
							.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.QPACK_EXPORT_ENTITY, qpackExportEntity);
			job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
			job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);
			job.getJobDataMap().put(ACSConstants.BATCH_META_DATA, batchMetaData);
			job.getJobDataMap().put(ACSConstants.IS_FORCED_ACTIVATION, isforcedActivation);

			Trigger trigger = null;
			// trigger =
			// scheduler.getTrigger(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packIdentifier,
			// PACK_ACTIVATION_GRP));
			//
			// if (trigger != null) {
			// if (trigger.getStartTime().compareTo(new Date()) > 0) {
			// logger.info(
			// "Already a trigger exists for activating Qpack with specified packCode = {}, so unscheduling the existing trigger and initiating the new trigger",
			// qpackExportEntity.getPackCode());
			// scheduler.unscheduleJob(TriggerKey.triggerKey(PACK_ACTIVATION_NAME + packIdentifier,
			// PACK_ACTIVATION_GRP));
			// } else {
			// logger.info("previous version of qpack activation is in process,hence skipping new or same version please try after some time");
			//
			// JobKey jobKey = new JobKey(PACK_ACTIVATION_NAME + packIdentifier, PACK_ACTIVATION_GRP);
			// JobDetail jobDetail = scheduler.getJobDetail(jobKey);
			// logger.trace("Recieved Job which contain JobKey = {} ", jobKey);
			// Map<String, Object> map = jobDetail.getJobDataMap();
			// if (map != null) {
			// QpackExportEntity exportEntity = (QpackExportEntity) map.get(ACSConstants.QPACK_EXPORT_ENTITY);
			// if (exportEntity != null
			// && exportEntity.getVersionNumber().equals(qpackExportEntity.getVersionNumber())) {
			// return false;
			// }
			// }
			// throw new ActivationInitiationFailedException(ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
			// "previous version of qpack activation is in process,hence skipping new or same version please try after some time");
			//
			// }
			// }
			if (qpackExportEntity.getActivationTime() != null
					&& qpackExportEntity.getActivationTime().after(Calendar.getInstance())) {
				// Trigger which fires according to the time specified.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.QPACK_ACTIVATION_JOB_PRIORITY)
								.startAt(qpackExportEntity.getActivationTime().getTime()).build();
			} else {
				// Trigger which starts the job immediately only if activation time is null.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(PACK_ACTIVATION_NAME + packCode, PACK_ACTIVATION_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount())
								.withPriority(ACSConstants.QPACK_ACTIVATION_JOB_PRIORITY).startNow().build();
			}

			scheduler.scheduleJob(job, trigger);
			logger.trace("Trigger for activating qpack = {} " + trigger);

			pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_START_TRIGGERED, packIdentifier);
		} catch (SchedulerException | GenericDataModelException ex) {
			logger.error("Exception while executing activateQPack...", ex);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_FAILED, packIdentifier);
				pss.updatePackStatus(PackContent.Qpack, batchCode, PacksStatusEnum.ACTIVATION_FAILED, null,
						packIdentifier);
			} catch (Exception e) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { e,
						packCode, PacksStatusEnum.ACTIVATION_FAILED });
			}
			return false;
		} catch (ActivationInitiationFailedException e) {
			logger.error("Exception while executing activateQPack...", e);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.ACTIVATION_INITIATION_FAILED, packIdentifier);
				// pss.updatePackStatus(RequestAction.Qpack, batchCodesList, PacksStatusEnum.ACTIVATION_FAILED, null,
				// qpackExportEntity.getPackCode());
			} catch (Exception ex) {
				logger.info("Exception = {} occured while updating status of the pack = {} to = {}", new Object[] { ex,
						packCode, PacksStatusEnum.ACTIVATION_INITIATION_FAILED });
			}
			throw new ActivationInitiationFailedException(ACSExceptionConstants.ACTIVATION_INITIATION_FAILED,
					"previous version of qpack activation is in process,hence skipping new or same version please try after some time");
		}
		return true;
	}

	/**
	 * Used for manual triggering of activation of a Pack based on the pack status
	 * 
	 * @param packIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ActivationRequestDO activatePackManually(String packIdentifier) throws GenericDataModelException {
		logger.info("initiated validateQpackStatus where packCode = {}", packIdentifier);
		ActivationRequestDO activationRequestDo = new ActivationRequestDO();
		AcsPacks packDetailsTO = pdes.getPackDetailsbyPackIdentifier(packIdentifier);
		if (packDetailsTO == null) {
			logger.info("No packDetails info exist associated to the specifie packCode = {}", packIdentifier);
			activationRequestDo.setErrorMessage("No packDetails info exist associated to the specifie packCode = "
					+ packIdentifier);
			activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
			return activationRequestDo;
		}
		logger.info("packDetails = {} associated to the specified packCode = {}", packDetailsTO, packIdentifier);

		// fill the pack details
		activationRequestDo.setPackIdentifier(packIdentifier);
		activationRequestDo.setPackCode(packDetailsTO.getPackCode());
		activationRequestDo.setPackVersion(packDetailsTO.getVersionNumber());
		activationRequestDo.setPactActivationTime(packDetailsTO.getActivationTime());

		List<AcsBatch> batchDetails = batchService.getBatchDetailsByPackId(packDetailsTO.getPackIdentifier());
		logger.info("batchDetails = {} associated to the specified packCode = {}", batchDetails, packIdentifier);

		List<String> validBatches = validateBatches(batchDetails);
		logger.info("list valid batches = {}", validBatches);

		if (validBatches != null && !validBatches.isEmpty()) {
			activationRequestDo.setBatchCodes(validBatches);

			switch (packDetailsTO.getPackStatus()) {
				case PASSWORD_FETCHING_FAILED:
					logger.error("Password is not recieved");
					activationRequestDo.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.REQUEST);
					activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
					activationRequestDo.setErrorMessage("Password is not recieved");
					break;
				case DOWNLOADED:
				case ACTIVATION_FAILED:
				case ACTIVATION_START_TRIGGERED:
				case PASSWORD_FETCHED:
					// check whether activation time is elapsed or not if elapsed initiate activation other wise
					// skip.
					if (activationRequestDo.getPactActivationTime().before(Calendar.getInstance())) {

						// For manual upload case, password might not present
						if (packDetailsTO.isIsPasswordProtected()) {
							logger.debug("Pack {} is password protected", packDetailsTO.getPackIdentifier());
							if (packDetailsTO.getPassword() == null || packDetailsTO.getPassword().isEmpty()) {
								logger.error("Password is not recieved");
								activationRequestDo.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.REQUEST);
								activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
								activationRequestDo.setErrorMessage("Password is not recieved");
								return activationRequestDo;
							}

						}

						logger.info("initiating activation where manualPasswordRequestDO = {}", activationRequestDo);

						// initiates activation process
						if (initiateActivation(activationRequestDo)) {
							logger.info("initiated activation successfully where manualPasswordRequestDO = {}",
									activationRequestDo);
							activationRequestDo
									.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.INITIATED_ACTIVATION);
							activationRequestDo.setStatus(ACSConstants.MSG_SUCCESS);
							activationRequestDo.setErrorMessage("Initiated activation successfully");
						} else {
							logger.info("unable to initiate activation where manualPasswordRequestDO = {}",
									activationRequestDo);
							activationRequestDo
									.setErrorMessage("Unable to initiate pack activation because already a job exists for activation which is in progress");
							activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
						}
					} else {
						logger.info(
								"Not initiating activation because scheduled pack activation time is not elapsed. Activation time is {}",
								activationRequestDo.getPactActivationTime().getTime());
						activationRequestDo
								.setErrorMessage("Not initiating activation because scheduled pack activation time is not elapsed");
						activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
					}
					break;

				default:
					activationRequestDo.setErrorMessage("Currently not handling for specified status = "
							+ packDetailsTO.getPackStatus());
					activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
					break;
			}
		} else {
			logger.info("Batches associated to the specified pack with code = {} are elapsed", packIdentifier);
			activationRequestDo.setErrorMessage("Batches associated to the specified pack with code = "
					+ packIdentifier + " are elapsed");
			activationRequestDo.setStatus(ACSConstants.MSG_FAILURE);
		}

		logger.info("validateQpackStatus returning {}", activationRequestDo);
		return activationRequestDo;
	}

	/**
	 * @param batchCodes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private List<String> validateBatches(List<AcsBatch> batchDetails) throws GenericDataModelException {
		List<String> validBatchCodes = null;
		if (batchDetails == null || batchDetails.isEmpty()) {
			logger.debug("No batches running");
			return null;
		} else {
			validBatchCodes = new ArrayList<>();
			for (AcsBatch batchDetailsTO : batchDetails) {
				if (!Calendar.getInstance().after(batchDetailsTO.getMaxDeltaRpackGenerationTime())) {
					validBatchCodes.add(batchDetailsTO.getBatchCode());
				}
			}
		}
		logger.debug("Valid batches are {}", batchDetails);
		return validBatchCodes;
	}

	/**
	 * @param activationRequestDO
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected boolean initiateActivation(ActivationRequestDO activationRequestDO) {
		boolean status = false;
		try {
			logger.debug("Starting the activation..");
			String packIdentifier = activationRequestDO.getPackIdentifier();
			AcsPacks packDetailsTO = pdes.getPackDetailsbyPackIdentifier(packIdentifier);
			if (packDetailsTO == null) {
				logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
				throw new GenericDataModelException(ACSExceptionConstants.NO_PACK_INFO_EXISTS,
						"No pack info available for specified packIdentifier = " + packIdentifier);
			}
			String packsDetails = packDetailsTO.getResponseMetaData();
			// Converts to PackExportEntity object defined in common-objects.
			PackEntity packEntity = new Gson().fromJson(packsDetails, PackEntity.class);
			logger.debug("packExportEntity is {}", packEntity);
			switch (packDetailsTO.getPackType()) {
				case Apack:
					logger.trace("activating the {}", packDetailsTO.getPackType());

					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity> apackExportEntities : packEntity
							.getApackExportEntities().entrySet()) {
						if (apackExportEntities == null) {
							logger.error("apackExportEntities is null");
						} else {
							com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity =
									apackExportEntities.getValue();
							logger.info("apackExportEntity = {}", apackExportEntity);

							if (apackExportEntity.getPackCode().equals(packDetailsTO.getPackCode())) {
								try {

									for (String batchcode : activationRequestDO.getBatchCodes()) {

										status =
												activateAPack(apackExportEntity, batchcode,
														packDetailsTO.getPackIdentifier());
									}
								} catch (ActivationInitiationFailedException e) {
									logger.error(
											"ActivationInitiationFailedException while executing initiateActivation...",
											e);
								}
							}
						}
					}
					break;
				case Bpack:
					logger.trace("activating the {}", packDetailsTO.getPackType());
					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity> bpackExportEntities : packEntity
							.getBpackExportEntities().entrySet()) {
						if (bpackExportEntities == null) {
							logger.error("apackExportEntities is null");
						} else {
							com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity =
									bpackExportEntities.getValue();
							if (bpackExportEntity.getPackCode().equals(packDetailsTO.getPackCode())) {
								try {
									for (String batchcode : activationRequestDO.getBatchCodes()) {
										status =
												activateBPack(bpackExportEntity, batchcode,
														packDetailsTO.getPackIdentifier());
									}
								} catch (ActivationInitiationFailedException e) {
									logger.error(
											"ActivationInitiationFailedException while executing initiateActivation...",
											e);
								}
							}
						}
					}
					break;
				case Qpack:
					logger.trace("activating the {}", packDetailsTO.getPackType());
					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity> qpackExportEntities : packEntity
							.getQpackExportEntities().entrySet()) {
						logger.trace("insaid loop");
						if (qpackExportEntities == null) {
							logger.error("apackExportEntities is null");
						} else {
							logger.trace("Qpack Export Entities is not null");
							com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity =
									qpackExportEntities.getValue();
							if (qpackExportEntity.getPackCode().equals(packDetailsTO.getPackCode())) {
								logger.trace("Qpack Pack code =  Pack identifier");
								try {
									for (String batchcode : activationRequestDO.getBatchCodes()) {
										logger.trace("insaid Batch code loop..");
										com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap =
												null;
										for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean> batchBeanMaps : packEntity
												.getBatchMap().entrySet()) {
											logger.trace("insaid Batch Bean Map loop..");
											String batchDataCode = batchBeanMaps.getKey();
											if (batchDataCode.equals(batchcode)) {
												batchMap = batchBeanMaps.getValue();
												logger.trace("Calling  Pack activation..");
												status =
														activateQPack(qpackExportEntity, batchcode,
																packDetailsTO.getPackIdentifier(), batchMap, true);
											}
										}
									}
								} catch (ActivationInitiationFailedException e) {
									logger.error(
											"ActivationInitiationFailedException while executing initiateActivation...",
											e);
								}
							}
						}
					}
					break;
				default:
					break;
			}

		} catch (GenericDataModelException e) {
			logger.error("GenericDataModelException while executing InitiateActivation...", e);
		}
		return status;
	}
}
