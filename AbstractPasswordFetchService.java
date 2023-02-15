/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 23-Aug-2013 4:15:58 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.exception.ZipExceptionConstants;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.dataobject.ManualPasswordRequestDO;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.exception.ActivationInitiationFailedException;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.SecuredZipUtil;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.PackEntity;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * 
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public abstract class AbstractPasswordFetchService {

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	protected static CDEAService cdeaService;
	protected static PackDetailsService packDetailsService;
	protected static BatchService batchService;
	protected static PackActivationService packActivationService;

	/**
	 * @return the logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	protected static void initialize() {
		if (cdeaService == null)
			cdeaService = new CDEAService();
		if (packDetailsService == null)
			packDetailsService = new PackDetailsService();
		if (batchService == null)
			batchService = BatchService.getInstance();
		if (packActivationService == null)
			packActivationService = new PackActivationService();
	}

	/**
	 * Method provides the time when the password fetch job should start. (subtracts the configured values from the
	 * activation time)
	 * 
	 * @param activationTime
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected Calendar buildPasswordFetcherStartTime(Calendar activationTime) {
		Calendar localActivationTime = (Calendar) activationTime.clone();
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PASSWORD_FETCH_START_BUFFER);
		int startMinute;
		if (propValue != null && StringUtils.isNumeric(propValue))
			startMinute = Integer.parseInt(propValue);
		else {
			startMinute = ACSConstants.DEFAULT_PASSWORD_FETCH_START_TIME;
			getLogger().debug("configured passwordFetchSTARTBuffer is invalid! Hence considering the defualt value {}",
					ACSConstants.DEFAULT_PASSWORD_FETCH_START_TIME);
		}
		localActivationTime.add(Calendar.MINUTE, -startMinute);
		getLogger().debug("buildPasswordFetcherStartTime is returning {}", localActivationTime.getTime());
		return localActivationTime;

	}

	/**
	 * Method provides the time when the password fetch job should end. (subtracts the configured values from the
	 * activation time)
	 * 
	 * @param activationTime
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected Calendar buildPasswordFetcherEndTime(Calendar activationTime) {
		Calendar localActivationTime = (Calendar) activationTime.clone();
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PASSWORD_FETCH_END_BUFFER);
		int endMinute;
		if (propValue != null && StringUtils.isNumeric(propValue))
			endMinute = Integer.parseInt(propValue);
		else {
			endMinute = ACSConstants.DEFAULT_PASSWORD_FETCH_END_TIME;
			getLogger().debug("configured passwordFetchEndBuffer is invalid! Hence considering the defualt value {}",
					ACSConstants.DEFAULT_PASSWORD_FETCH_END_TIME);
		}
		localActivationTime.add(Calendar.MINUTE, -endMinute);
		getLogger().debug("buildPasswordFetcherEndTime is returning {}", localActivationTime.getTime());
		return localActivationTime;

	}

	/**
	 * This method validates the password by trying unzipping the archive. If unzip happens successfully, then temporary
	 * instance is deleted and status is returned
	 * 
	 * @param password
	 * @param packDownloadPath
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected boolean validatePassword(String password, String packDownloadPath) {

		getLogger().debug("Validating password password: {} packDownloadPath={}", password, packDownloadPath);
		String tempDestination = new File(packDownloadPath).getParent() + File.separator + "temp";
		try {
			SecuredZipUtil.extractArchiveWithPassword(packDownloadPath, tempDestination, password);
			getLogger().debug("PASSWORD IS VALID");
			// extract with no exception; So delete the extracted files
			// FileUtils.deleteDirectory(new File(tempDestination));
			return true;
		} catch (ZipException e) {
			if (e.getCode() == ZipExceptionConstants.WRONG_PASSWORD) {
				getLogger().error("Wrong password!!!");
			} else {
				getLogger().error("ZipException while executing validatePassword...", e);
			}
		} finally {
			// clean up the created temp directory
			try {
				FileUtils.deleteDirectory(new File(tempDestination));
			} catch (IOException e) {
				getLogger().error("IOException while executing validatePassword...", e);
			}
		}
		return false;
	}

	/**
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected String getSalt(String batchCode) throws GenericDataModelException {
		getLogger().debug("Getting salt for {}", batchCode);
		return cdeaService.getEventCodeByBatchCode(batchCode);
	}

	/**
	 * @param manualPasswordRequestDO
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected boolean checkActivationToBeDoneNow(final ManualPasswordRequestDO manualPasswordRequestDO) {

		getLogger().debug("Trying to check whether activation to be started... {}", manualPasswordRequestDO);
		boolean isActivationTobeDoneNow = false;
		if (manualPasswordRequestDO.getPactActivationTime().after(Calendar.getInstance())) {
			getLogger().debug("Activation time is not passed. Hence no need to initiate it now");
			return isActivationTobeDoneNow;
		}
		getLogger().debug(
				"Activation time is passed and valid password is receieved now. Hence initiating the activation now");
		return true;

	}

	/**
	 * @param manualPasswordRequestDO
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected void initiateActivationInAThread(final ManualPasswordRequestDO manualPasswordRequestDO) {
		new Thread(new Runnable() {

			@Override
			public void run() {
				getLogger().debug("Initiating activation...");
				String packIdentifier =
						PackDetailsService.generatePackIdentifier(manualPasswordRequestDO.getPackCode(),
								manualPasswordRequestDO.getPackVersion());
				manualPasswordRequestDO.setPackIdentifier(packIdentifier);
				initiateActivation(manualPasswordRequestDO);

			}
		}).start();
	}

	/**
	 * @param manualPasswordRequestDO
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected boolean initiateActivation(ManualPasswordRequestDO manualPasswordRequestDO) {
		boolean status = false;
		try {
			getLogger().debug("Starting the activation..");
			String packIdentifier = manualPasswordRequestDO.getPackIdentifier();
			AcsPacks packDetailsTO = packDetailsService.getPackDetailsbyPackIdentifier(packIdentifier);

			if (packDetailsTO == null) {
				logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
				throw new GenericDataModelException(ACSExceptionConstants.NO_PACK_INFO_EXISTS,
						"No pack info available for specified packIdentifier = " + packIdentifier);
			}

			List<String> batchCodes = batchService.getBatchCodesByPackIdentifier(packIdentifier);

			String packsDetails = packDetailsTO.getResponseMetaData();
			// Converts to PackExportEntity object defined in common-objects.
			PackEntity packEntity = new Gson().fromJson(packsDetails, PackEntity.class);
			getLogger().debug("packExportEntity is {}", packEntity);
			switch (packDetailsTO.getPackType()) {
				case Apack:
					getLogger().trace("activating the {}", packDetailsTO.getPackType());

					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity> apackExportEntities : packEntity
							.getApackExportEntities().entrySet()) {
						if (apackExportEntities == null) {
							getLogger().error("apackExportEntities is null");
						} else {
							com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity =
									apackExportEntities.getValue();
							logger.info("apackExportEntity = {}", apackExportEntity);

							if (apackExportEntity.getPackCode().equals(packDetailsTO.getPackIdentifier())) {
								try {

									for (String batchcode : manualPasswordRequestDO.getBatchCodes()) {

										status =
												packActivationService.activateAPack(apackExportEntity, batchcode,
														packDetailsTO.getPackIdentifier());
									}
								} catch (ActivationInitiationFailedException e) {
									getLogger()
											.error("ActivationInitiationFailedException while executing initiateActivation...",
													e);
								}
							}
						}
					}
					break;
				case Bpack:
					getLogger().trace("activating the {}", packDetailsTO.getPackType());
					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity> bpackExportEntities : packEntity
							.getBpackExportEntities().entrySet()) {
						if (bpackExportEntities == null) {
							getLogger().error("apackExportEntities is null");
						} else {
							com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity =
									bpackExportEntities.getValue();
							if (bpackExportEntity.getPackCode().equals(packDetailsTO.getPackIdentifier())) {
								try {
									for (String batchcode : manualPasswordRequestDO.getBatchCodes()) {
										status =
												packActivationService.activateBPack(bpackExportEntity, batchcode,
														packDetailsTO.getPackIdentifier());
									}
								} catch (ActivationInitiationFailedException e) {
									getLogger()
											.error("ActivationInitiationFailedException while executing initiateActivation...",
													e);
								}
							}
						}
					}
					break;
				case Qpack:
					getLogger().trace("activating the {}", packDetailsTO.getPackType());
					for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity> qpackExportEntities : packEntity
							.getQpackExportEntities().entrySet()) {
						getLogger().trace("insaid loop");
						if (qpackExportEntities == null) {
							getLogger().error("qpackExportEntities is null");
						} else {
							getLogger().trace("Qpack Export Entities is not null");
							com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity =
									qpackExportEntities.getValue();
							if (qpackExportEntity.getPackCode().equals(packDetailsTO.getPackCode())) {
								getLogger().trace("Qpack Pack code =  Pack identifier");
								try {
									for (String batchcode : batchCodes) {
										getLogger().trace("inside Batch code loop..BatchCode: {}", batchcode);
										com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap =
												null;
										for (Map.Entry<String, com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean> batchBeanMaps : packEntity
												.getBatchMap().entrySet()) {
											getLogger().trace("inside Batch Bean Map loop..");
											String batchDataCode = batchBeanMaps.getKey();
											if (batchDataCode.equals(batchcode)) {
												batchMap = batchBeanMaps.getValue();
												getLogger().trace("Calling  Pack activation..");
												status =
														packActivationService.activateQPack(qpackExportEntity,
																batchcode, packDetailsTO.getPackIdentifier(), batchMap,
																true);
											}
										}
									}
								} catch (ActivationInitiationFailedException e) {
									getLogger()
											.error("ActivationInitiationFailedException while executing initiateActivation...",
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
			getLogger().error("GenericDataModelException while executing InitiateActivation...", e);
		}
		return status;
	}

	/**
	 * @param manualPasswordRequestDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected String getPackDownloadPath(String packCode) throws GenericDataModelException {
		String packDownloadPath =
				System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR + File.separator
						+ ACSConstants.ACS_TEMP_DIR + File.separator + packCode;
		AcsPacks packDetailsTO = packDetailsService.getPackDetailsbyPackCode(packCode);
		if (packDetailsTO == null) {
			logger.info("No pack info available for specified packCode = {}", packCode);
			throw new GenericDataModelException(ACSExceptionConstants.NO_PACK_INFO_EXISTS,
					"No pack info available for specified packCode = " + packCode);
		}
		String packFileDownloadPathinDB = packDetailsTO.getPackFileDownloadPath();
		String fileName = new File(packFileDownloadPathinDB).getName();
		String zipFile = packDownloadPath + File.separator + fileName;
		return zipFile;
	}

}
