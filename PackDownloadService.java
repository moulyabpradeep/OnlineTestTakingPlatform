package com.merittrac.apollo.acs.services;

import java.util.Calendar;

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

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.quartz.jobs.PacksDownloader;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.entities.deliverymanager.SFTPCredentialBean;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * This class contains api's which initiates download process of a pack.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class PackDownloadService extends BasicService implements IPackDownloadService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	static PackDetailsService pdes = null;
	static ACSService as = null;

	private static void initialized() {
		if (pdes == null)
			pdes = new PackDetailsService();
		if (as == null)
			as = new ACSService();
	}

	public PackDownloadService() {
		initialized();
	}

	@Override
	public boolean startPacksDownloaderJob(SFTPCredentialBean sftpCredentialBean,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.AbstractPackEntity abstractPackEntity,
			String batchCode, String packIdentifier, PackContent packType,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap) {
		JobDetail job = null;
		Trigger trigger = null;
		Scheduler scheduler = null;

		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			switch (packType) {
				case Bpack:

					String BPACK_DOWNLOADER_NAME = ACSConstants.BPACK_DOWNLOADER_NAME;
					String BPACK_DOWNLOADER_GRP = ACSConstants.BPACK_DOWNLOADER_GRP;

					com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity =
							(com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity) abstractPackEntity;

					job =
							JobBuilder.newJob(PacksDownloader.class)
									.withIdentity(BPACK_DOWNLOADER_NAME + packIdentifier, BPACK_DOWNLOADER_GRP)
									.storeDurably(false).requestRecovery(true).build();
					job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
					job.getJobDataMap().put(ACSConstants.PACK_TYPE_CONST, packType);
					job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);
					job.getJobDataMap().put(ACSConstants.BPACK_EXPORT_ENTITY, bpackExportEntity);
					job.getJobDataMap().put(ACSConstants.SFTP_CREDENTIALS_BEAN, sftpCredentialBean);
					job.getJobDataMap().put(ACSConstants.BATCH_META_DATA, batchMap);

					trigger =
							scheduler.getTrigger(TriggerKey.triggerKey(BPACK_DOWNLOADER_NAME + packIdentifier,
									BPACK_DOWNLOADER_GRP));
					logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for downloading Bpack with specified packCode = {} and version number = {}",
								bpackExportEntity.getPackCode(), bpackExportEntity.getVersionNumber());
						return false;
						// scheduler.unscheduleJob(TriggerKey.triggerKey(BPACK_DOWNLOADER_NAME +
						// packIdentifier + "V" + bpackExportEntity.getVersionNumber(),
						// BPACK_DOWNLOADER_GRP));
					}
					if (bpackExportEntity.getDownloadTime() != null
							&& bpackExportEntity.getDownloadTime().after(Calendar.getInstance())) {
						// Trigger which fires according to the time specified.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(BPACK_DOWNLOADER_NAME + packIdentifier, BPACK_DOWNLOADER_GRP)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY)
										.startAt(bpackExportEntity.getDownloadTime().getTime()).build();
					} else {
						// Trigger which starts the job immediately only if activation time is null.
						trigger =
								TriggerBuilder.newTrigger()
										.withIdentity(BPACK_DOWNLOADER_NAME + packIdentifier, BPACK_DOWNLOADER_GRP)
										.withSchedule(SimpleScheduleBuilder.simpleSchedule()

										.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY).startNow().build();
					}

					scheduler.scheduleJob(job, trigger);
					logger.trace("Trigger for downloading bpack = {} " + trigger);

					break;

				case Apack:

					com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity =
							(com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity) abstractPackEntity;

					String APACK_DOWNLOADER_NAME = ACSConstants.APACK_DOWNLOADER_NAME;
					String APACK_DOWNLOADER_GRP = ACSConstants.APACK_DOWNLOADER_GRP;

					job =
							JobBuilder.newJob(PacksDownloader.class)
									.withIdentity(APACK_DOWNLOADER_NAME + packIdentifier, APACK_DOWNLOADER_GRP)
									.storeDurably(false).requestRecovery(true).build();
					job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
					job.getJobDataMap().put(ACSConstants.PACK_TYPE_CONST, packType);
					job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);
					job.getJobDataMap().put(ACSConstants.APACK_EXPORT_ENTITY, apackExportEntity);
					job.getJobDataMap().put(ACSConstants.SFTP_CREDENTIALS_BEAN, sftpCredentialBean);
					job.getJobDataMap().put(ACSConstants.BATCH_META_DATA, batchMap);

					trigger =
							scheduler.getTrigger(TriggerKey.triggerKey(APACK_DOWNLOADER_NAME + packIdentifier,
									APACK_DOWNLOADER_GRP));
					logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for downloading Apack with specified packCode = {} and version number = {}",
								apackExportEntity.getPackCode(), apackExportEntity.getVersionNumber());
						return false;
						// scheduler.unscheduleJob(TriggerKey.triggerKey(APACK_DOWNLOADER_NAME +
						// packIdentifier + "V" + apackExportEntity.getVersionNumber(),
						// APACK_DOWNLOADER_GRP));
					}
					if (apackExportEntity.getDownloadTime() != null
							&& apackExportEntity.getDownloadTime().after(Calendar.getInstance())) {
						// Trigger which fires according to the time specified.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(APACK_DOWNLOADER_NAME + packIdentifier, APACK_DOWNLOADER_GRP)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY)
										.startAt(apackExportEntity.getDownloadTime().getTime()).build();
					} else {
						// Trigger which starts the job immediately only if activation time is null.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(APACK_DOWNLOADER_NAME + packIdentifier, APACK_DOWNLOADER_GRP)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY).startNow().build();
					}

					scheduler.scheduleJob(job, trigger);
					logger.trace("Trigger for downloading apack = {} " + trigger);

					break;

				case Qpack:
					String QPACK_DOWNLOADER_NAME = ACSConstants.QPACK_DOWNLOADER_NAME;
					String QPACK_DOWNLOADER_GRP = ACSConstants.QPACK_DOWNLOADER_GRP;

					com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity =
							(com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity) abstractPackEntity;

					job =
							JobBuilder.newJob(PacksDownloader.class)
									.withIdentity(QPACK_DOWNLOADER_NAME + packIdentifier, QPACK_DOWNLOADER_GRP)
									.storeDurably(false).requestRecovery(true).build();
					job.getJobDataMap().put(ACSConstants.BATCH_CODES_CONST, batchCode);
					job.getJobDataMap().put(ACSConstants.PACK_TYPE_CONST, packType);
					job.getJobDataMap().put(ACSConstants.PACK_IDENTIFIER, packIdentifier);
					job.getJobDataMap().put(ACSConstants.QPACK_EXPORT_ENTITY, qpackExportEntity);
					job.getJobDataMap().put(ACSConstants.SFTP_CREDENTIALS_BEAN, sftpCredentialBean);
					job.getJobDataMap().put(ACSConstants.BATCH_META_DATA, batchMap);

					trigger =
							scheduler.getTrigger(TriggerKey.triggerKey(QPACK_DOWNLOADER_NAME + packIdentifier,
									QPACK_DOWNLOADER_GRP));
					logger.info("Received Trigger = {} which contain JobKey = {} ", trigger, job.getKey());

					if (trigger != null) {
						logger.info(
								"Already a trigger exists for downloading Qpack with specified packCode = {} and version number = {}",
								qpackExportEntity.getPackCode(), qpackExportEntity.getVersionNumber());
						return false;
						// scheduler.unscheduleJob(TriggerKey.triggerKey(QPACK_DOWNLOADER_NAME +
						// packIdentifier + "V" + qpackExportEntity.getVersionNumber(),
						// QPACK_DOWNLOADER_GRP));
					}
					if (qpackExportEntity.getDownloadTime() != null
							&& qpackExportEntity.getDownloadTime().after(Calendar.getInstance())) {
						// Trigger which fires according to the time specified.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(QPACK_DOWNLOADER_NAME + packIdentifier, QPACK_DOWNLOADER_GRP)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY)
										.startAt(qpackExportEntity.getDownloadTime().getTime()).build();
					} else {
						// Trigger which starts the job immediately only if activation time is null.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(QPACK_DOWNLOADER_NAME + packIdentifier
										// + "V"+ qpackExportEntity.getVersionNumber()
												, QPACK_DOWNLOADER_GRP)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withMisfireHandlingInstructionFireNow())
										.withPriority(ACSConstants.DOWNLOAD_JOB_PRIORITY).startNow().build();
					}

					scheduler.scheduleJob(job, trigger);
					logger.trace("Trigger for downloading qpack = {} " + trigger);

					break;

				default:
					logger.info("No such pack type specified = {} ", packType);
					return false;
			}
		} catch (SchedulerException ex) {
			logger.error("SchedulerException while executing PackDownloadService...", ex);
			try {
				pdes.updatePacksStatusByPackReqId(PacksStatusEnum.DOWNLOAD_FAILED, packIdentifier);
			} catch (GenericDataModelException e) {
				logger.error("GenericDataModelException while updating PackRequestStatus in PackDownloadService...", e);
			}
			return false;
		}

		try {
			pdes.updatePacksStatusByPackReqId(PacksStatusEnum.DOWNLOAD_START_TRIGGERED, packIdentifier);
		} catch (Exception ex) {
			logger.info("Exception occured while updating status of the pack = {} to = {}", packIdentifier,
					PacksStatusEnum.DOWNLOAD_FAILED);
			return false;
		}

		return true;
	}
}
