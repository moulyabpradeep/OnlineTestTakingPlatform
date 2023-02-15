/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 28-Jan-2016 3:23:56 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.Calendar;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.dataobject.remoteproctoring.RemoteProctoringEvent;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsRemoteEvent;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Service for remote proctoring related implementation
 *
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class RemoteProctoringService extends BasicService {

	private static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	/**
	 * static Singleton instance
	 */
	private static RemoteProctoringService instance;

	/**
	 * Private constructor for singleton
	 */
	private RemoteProctoringService() {
	}

	/**
	 * Static getter method for retrieving the singleton instance
	 */
	public static RemoteProctoringService getInstance() {
		if (instance == null) {
			instance = new RemoteProctoringService();
		}
		return instance;
	}

	/**
	 * @param remoteProctoringEvent
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean saveEvent(RemoteProctoringEvent remoteProctoringEvent) {
		getLogger().debug("saveEvent : {}", remoteProctoringEvent);
		try {
			AcsBatchCandidateAssociation acsbatchcandidateassociation =
					(AcsBatchCandidateAssociation) session.get(Integer.parseInt(remoteProctoringEvent.getSession_id()),
							AcsBatchCandidateAssociation.class.getCanonicalName());

			Calendar timeOfEvent = DatatypeConverter.parseDateTime(remoteProctoringEvent.getTimestamp());

			AcsRemoteEvent acsRemoteEvent =
					new AcsRemoteEvent(remoteProctoringEvent.getId(), acsbatchcandidateassociation,
							remoteProctoringEvent.getEvent().getCode(), remoteProctoringEvent.getEvent().getName(),
							timeOfEvent, remoteProctoringEvent.getIp(), remoteProctoringEvent.getOs(),
							remoteProctoringEvent.getBrowser().toString());
			acsRemoteEvent.setReceivedTime(Calendar.getInstance());
			session.save(acsRemoteEvent);
			getLogger().debug("Event is saved successfully");
		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing saveEvent...", e);
			return false;
		}
		return true;
	}
}
