/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 05-Aug-2013 6:48:02 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.quartz.jobs.PasswordFetcher;
import com.merittrac.apollo.acs.utility.PropertiesUtil;

/**
 * 
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class AutomaticPasswordFetchService extends AbstractPasswordFetchService implements IPasswordFetchService {

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	@Override
	public void fetchBpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception {
		initialize();
		String passwordFetcherName = "BPackPasswordFetcher";
		String passwordFetcherGroup = passwordFetcherName + "Group";

		createJob(bpackExportEntity, passwordFetcherName, passwordFetcherGroup, packDownloadedPath, batchMetaDataBean);
	}

	@Override
	public void fetchApackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception {
		initialize();
		String passwordFetcherName = "APackPasswordFetcher";
		String passwordFetcherGroup = passwordFetcherName + "Group";

		createJob(apackExportEntity, passwordFetcherName, passwordFetcherGroup, packDownloadedPath, batchMetaDataBean);
	}

	@Override
	public void fetchQpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception {
		initialize();
		if (qpackExportEntity.getActivationTime().before(Calendar.getInstance())) {
			getLogger().error("Not scheduling password fetch job as the activation time is over");
			return;
		}
		String passwordFetcherName = "QPackPasswordFetcher";
		String passwordFetcherGroup = passwordFetcherName + "Group";

		createJob(qpackExportEntity, passwordFetcherName, passwordFetcherGroup, packDownloadedPath, batchMetaDataBean);

	}

	/**
	 * @param packEntity
	 * @param passwordFetcherName
	 * @param passwordFetcherGroup
	 * @param packDownloadPath
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean createJob(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.AbstractPackEntity abstractpackEntity,
			String passwordFetcherName, String passwordFetcherGroup, String packDownloadPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception {
		getLogger().info("creating password protection job  {} Group: {} ", passwordFetcherName, passwordFetcherGroup);
		JobDetail job = null;
		Scheduler scheduler = null;

		String packCode = abstractpackEntity.getPackCode();

		try {
			String acsCode = cdeaService.getACSCodeByBatchCode(batchMetaDataBean.getBatchCode());

			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			JobKey jobKey = new JobKey(passwordFetcherName + packCode, passwordFetcherGroup);
			List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
			if (triggers != null && !triggers.isEmpty()) {
				for (Iterator iterator = triggers.iterator(); iterator.hasNext();) {
					Trigger trigger = (Trigger) iterator.next();
					if (trigger != null) {
						getLogger().info(
								"Already a trigger exists for Fetching password for the specified packCode = {}",
								packCode);
						return false;
					}
				}
			}

			job =
					JobBuilder.newJob(PasswordFetcher.class)
							.withIdentity(passwordFetcherName + packCode, passwordFetcherGroup).storeDurably(false)
							// .requestRecovery(true)
							.build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchMetaDataBean.getBatchCode());
			job.getJobDataMap().put(ACSConstants.PACK_CODE, packCode);
			job.getJobDataMap().put(ACSConstants.ACS_CODE, acsCode);
			job.getJobDataMap().put(ACSConstants.PACK_REF_NUMBER, abstractpackEntity.getPackStatusUpdateId());
			job.getJobDataMap().put(ACSConstants.PACK_DOWNLOAD_PATH, packDownloadPath);
			job.getJobDataMap().put(ACSConstants.PACK_VERSION, abstractpackEntity.getVersionNumber());

			Trigger trigger = null;

			if (abstractpackEntity.getActivationTime() != null) {
				getLogger().info("Pack activationtion time is {}", abstractpackEntity.getActivationTime());
				Calendar PasswordFetchServiceEndTime =
						buildPasswordFetcherEndTime(abstractpackEntity.getActivationTime());

				if (PasswordFetchServiceEndTime.after(Calendar.getInstance())) {
					Calendar PasswordFetchServiceStartTime =
							buildPasswordFetcherStartTime(abstractpackEntity.getActivationTime());

					int passwordFetchInterval = getPasswordFetchJobInterval();
					getLogger().info(
							"Password Fetch job {} is created with start Time: {} End Time: repeat Interval: {}",
							new Object[] { passwordFetcherName + packCode, PasswordFetchServiceStartTime,
									PasswordFetchServiceEndTime, passwordFetchInterval });

					if (PasswordFetchServiceStartTime.after(Calendar.getInstance())) {
						// Trigger which fires according to the time specified.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(passwordFetcherName + packCode, passwordFetcherGroup)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withIntervalInMinutes(passwordFetchInterval)
														.withMisfireHandlingInstructionNextWithRemainingCount())
										.endAt(PasswordFetchServiceEndTime.getTime())
										.startAt(PasswordFetchServiceStartTime.getTime()).build();
					} else {
						// Trigger which fires according to the time specified.
						trigger =
								TriggerBuilder
										.newTrigger()
										.withIdentity(passwordFetcherName + packCode, passwordFetcherGroup)
										.withSchedule(
												SimpleScheduleBuilder.simpleSchedule()
														.withIntervalInMinutes(passwordFetchInterval)
														.withMisfireHandlingInstructionNextWithRemainingCount())
										.endAt(PasswordFetchServiceEndTime.getTime()).startNow().build();
					}
				} else {
					// Trigger which starts the job immediately is the start time is over
					trigger =
							TriggerBuilder
									.newTrigger()
									.withIdentity(passwordFetcherName + packCode, passwordFetcherGroup)
									.withSchedule(
											SimpleScheduleBuilder.simpleSchedule()
													.withMisfireHandlingInstructionNextWithRemainingCount()).startNow()
									.build();
				}
			} else {
				// Trigger which starts the job immediately if activation time is null.
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(passwordFetcherName + packCode, passwordFetcherGroup)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule()
												.withMisfireHandlingInstructionNextWithRemainingCount()).startNow()
								.build();
			}

			scheduler.scheduleJob(job, trigger);
			logger.info("Trigger for downloading bpack = {} " + trigger);
		} catch (Exception e) {
			getLogger().error("Password fetch job creation failed");
			throw e;
		}
		return true;
	}

	/**
	 * 
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private int getPasswordFetchJobInterval() {
		int fetchInterval = 1;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PASSWORD_FETCH_INTERVAL);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			fetchInterval = Integer.parseInt(propValue);
		} else
			fetchInterval = ACSConstants.DEFAULT_PASSWORD_FETCH_INTERVAL;
		getLogger().trace("Password Fetch interval is " + fetchInterval);
		return fetchInterval;
	}

	@Override
	public void fetchBpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap) throws Exception {

		fetchBpackPassword(bpackExportEntity, getPackDownloadPath(bpackExportEntity.getPackCode()), batchMap);
	}

	@Override
	public void fetchApackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap) throws Exception {

		fetchApackPassword(apackExportEntity, getPackDownloadPath(apackExportEntity.getPackCode()), batchMap);

	}

	@Override
	public void fetchQpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap) throws Exception {

		fetchQpackPassword(qpackExportEntity, getPackDownloadPath(qpackExportEntity.getPackCode()), batchMap);

	}
}
