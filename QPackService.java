package com.merittrac.apollo.acs.services;

import java.util.HashMap;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.common.entities.packgen.QpackExportEntity;
import com.merittrac.apollo.core.services.BasicService;

public class QPackService extends BasicService implements IQPackService
{
	private static Logger logger = LoggerFactory.getLogger(ACSConstants.QUARTZ_JOB_LOGGER);

	/*
	 * Method takes care of download and activation of QPack. Will initiate a quartz job which will take care
	 * of both.
	 * 
	 * @see com.merittrac.apollo.acs.services.IQPackService#downloadQPack()
	 * 
	 * TODO :: Passing the right entity job to the method and subsequently passing right data to job data map.
	 */
	public void activateQPack(QpackExportEntity qpee)
	{
		Map<String, Object> m = new HashMap<String, Object>();
		m.put(ACSConstants.QPACK_EXPORT_ENTITY, qpee);

		// Job Data is to pass the data object that is need as an input to job.
		JobDataMap jdm = new JobDataMap(m);
		JobDetail job = null;

		Scheduler scheduler;
		try
		{
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();
			JobKey jk = new JobKey("QPackActivator", "QPackActivatorGroup");
			job = scheduler.getJobDetail(jk);
			job.getJobBuilder().usingJobData(jdm);
			Trigger trigger = TriggerBuilder.newTrigger().withIdentity("QPackTrigger", "QPackTriggerGroup").withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(2).withRepeatCount(0)).forJob(job).build();
			scheduler.scheduleJob(trigger);
		}
		catch (SchedulerException e)
		{
			e.printStackTrace();
			// logger.info("Job "+job.getDescription()+" failed");
			logger.error(e.getMessage(), e);
		}
	}

}
