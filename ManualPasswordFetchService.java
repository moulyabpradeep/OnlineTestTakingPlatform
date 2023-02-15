/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 23-Aug-2013 4:15:19 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.PasswordRequestStatusEnum;
import com.merittrac.apollo.acs.dataobject.ManualPasswordRequestDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.exception.PasswordFetchException;
import com.merittrac.apollo.acs.quartz.jobs.PasswordFetcher;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PasswordRequestBean;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * Service for fetching the password from UI
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class ManualPasswordFetchService extends AbstractPasswordFetchService {

	/**
	 * Check whether password fetching from UI is required
	 * 
	 * @return
	 * @throws PasswordFetchException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ManualPasswordRequestDO isManualPasswordFetchRequired() throws PasswordFetchException {
		logger.debug("Checking whether the manual password Fetch is required...");
		initialize();
		ManualPasswordRequestDO manualPasswordRequestDO = null;
		try {
			List<AcsPacks> packDetailsTOs = packDetailsService.getPasswordFetchFailedPackDetails();
			if (packDetailsTOs == null || packDetailsTOs.isEmpty()) {
				getLogger().debug("No failed password fetch");
				return null;
			}
			// PackDetailsTO packDetailsTO = packDetailsTOs.iterator().next();
			for (AcsPacks packDetailsTO : packDetailsTOs) {
				if (packDetailsTO.isIsPasswordProtected()) {
					logger.debug("password is not rececieved for pack packcode:{}", packDetailsTO.getPackIdentifier());
					// activation time is not present. Hence now password is required from UI
					if (packDetailsTO.getActivationTime() == null
							|| Calendar.getInstance().after(
									buildPasswordFetcherEndTime(packDetailsTO.getActivationTime()))
							|| packDetailsTO.getIsManualUpload()) {
						List<AcsBatch> batchDetails =
								batchService.getBatchDetailsByPackId(packDetailsTO.getPackIdentifier());
						List<String> validBatches = validateBatches(batchDetails);
						if (validBatches != null && !validBatches.isEmpty()) {

							manualPasswordRequestDO =
									new ManualPasswordRequestDO(PasswordRequestStatusEnum.REQUEST,
											packDetailsTO.getPackIdentifier(), packDetailsTO.getPackCode(),
											packDetailsTO.getVersionNumber(), packDetailsTO.getActivationTime(),
											validBatches);
							getLogger().debug("isManualPasswordFetchRequired returning {}", manualPasswordRequestDO);
							return manualPasswordRequestDO;
						}
					}
				}
			}
		} catch (GenericDataModelException e) {
			logger.error("GenericDataModelException while executing isManualPasswordFetchRequired...", e);
			throw new PasswordFetchException(e);
		}
		getLogger().debug("isManualPasswordFetchRequired returning {}", manualPasswordRequestDO);
		return manualPasswordRequestDO;
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
			getLogger().debug("No batches running");
			return null;
		} else {
			validBatchCodes = new ArrayList<>();
			for (AcsBatch batchDetailsTO : batchDetails) {
				if (!Calendar.getInstance().after(batchDetailsTO.getMaxDeltaRpackGenerationTime())) {
					validBatchCodes.add(batchDetailsTO.getBatchCode());
				}
			}
		}
		getLogger().debug("Valid batches are {}", batchDetails);
		return validBatchCodes;
	}

	/**
	 * Validate the password
	 * 
	 * @param manualPasswordRequestDO
	 * @param userName
	 *            TODO
	 * @param ipAddress
	 *            TODO
	 * @return
	 * @throws PasswordFetchException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ManualPasswordRequestDO validateAndSavePassword(ManualPasswordRequestDO manualPasswordRequestDO,
			String userName, String ipAddress) throws PasswordFetchException {
		getLogger().debug("Validating the provided password {}", manualPasswordRequestDO);
		try {
			boolean isActivationTobeDoneNow = false;
			validateManualPasswordRequestDO(manualPasswordRequestDO);
			initialize();
			auditIt(manualPasswordRequestDO, AdminAuditActionEnum.MANUAL_PASSWORD_FETCH, userName, ipAddress);

			// AcsPacks packDetailsTO =
			// packDetailsService.getPackDetailsbyPackIdentifier(manualPasswordRequestDO.getPackIdentifier());
			// manualPasswordRequestDO.setPackCode(packDetailsTO.getPackCode());
			boolean isPasswordRecievedAlready = checkIfPasswordRecievedAlready(manualPasswordRequestDO);
			if (isPasswordRecievedAlready) {
				getLogger().debug("Password is receieved already. Not need to proceed furthur");
				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_SUCCESSFULL);
				return manualPasswordRequestDO;
			}
			String zipFile = getPackDownloadPath(manualPasswordRequestDO);
			boolean isValidPassword = validatePassword(manualPasswordRequestDO.getPassword(), zipFile);
			if (isValidPassword) {
				auditIt(manualPasswordRequestDO, AdminAuditActionEnum.MANUAL_PASSWORD_FETCH_VALIDATION, userName,
						ipAddress);
				String message = "Password receieved was valid.";
				getLogger().debug(message);

				String passwordEncrypted =
						encryptPassword(manualPasswordRequestDO.getPassword(),
								manualPasswordRequestDO.getPackIdentifier());
				packDetailsService.updatePassword(manualPasswordRequestDO.getPackIdentifier(), passwordEncrypted);
				getLogger().debug("Password saved to database");
				packDetailsService.updatePackStatusByPackIdentifier(manualPasswordRequestDO.getPackIdentifier(),
						PacksStatusEnum.PASSWORD_FETCHED, "password Fetched successfully");
				// packDetailsService.updatePackRequestorStatusByPackIdentifierAndVersion(
				// PacksStatusEnum.PASSWORD_FETCHED, manualPasswordRequestDO.getPackCode(),
				// manualPasswordRequestDO.getPackVersion());
				isActivationTobeDoneNow = checkActivationToBeDoneNow(manualPasswordRequestDO);
				// If the activation time is passed, start the activation now
				if (isActivationTobeDoneNow)
					initiateActivation(manualPasswordRequestDO);

				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_SUCCESSFULL);
			} else {
				auditIt(manualPasswordRequestDO, AdminAuditActionEnum.MANUAL_PASSWORD_FETCH_VALIDATION, userName,
						ipAddress);
				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_FAILED);
			}

		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing validateAndSavePassword...", e);
			throw new PasswordFetchException(e);
		}
		getLogger().debug("validateAndSavePassword is returning {}", manualPasswordRequestDO);
		return manualPasswordRequestDO;
	}

	/**
	 * Audit the manual intervention
	 * 
	 * @param manualPasswordRequestDO
	 * @param userName
	 * 
	 * @param ipAddress
	 *            *
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditIt(ManualPasswordRequestDO manualPasswordRequestDO, AdminAuditActionEnum adminAuditActionEnum,
			String userName, String ipAddress) {
		// added for audit trail
		String isAuditEnable = AutoConfigure.getAuditConfigureValue();
		String message = null;
		Object[] paramArray = null;
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			switch (adminAuditActionEnum) {
				case MANUAL_PASSWORD_FETCH:
					message = ACSConstants.AUDIT_MANUAL_PASSWORD_FETCH_MSG;
					paramArray =
							new Object[] { manualPasswordRequestDO.getPassword(),
									manualPasswordRequestDO.getPackCode(), manualPasswordRequestDO.getPackVersion() };
					break;
				case MANUAL_PASSWORD_FETCH_VALIDATION:
					message = ACSConstants.AUDIT_MANUAL_PASSWORD_FETCH_MSG_VALIDATION;
					paramArray =
							new Object[] { manualPasswordRequestDO.getPassword(),
									manualPasswordRequestDO.getPackCode(), manualPasswordRequestDO.getPackVersion(),
									manualPasswordRequestDO.getPasswordRequestStatusEnum() };
					break;
				case RETRY_DM_PASSWORD_FETCH:
					message = ACSConstants.AUDIT_RETRY_PASSWORD_FETCH;
					paramArray =
							new Object[] { manualPasswordRequestDO.getPackCode(),
									manualPasswordRequestDO.getPackVersion() };
					break;

				default:
					break;
			}

			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, adminAuditActionEnum.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
			logbackParams.put(ACSConstants.AUDIT_ADMIN_HOST_ADDRESS, ipAddress);
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams, message,
					paramArray);
		}
		// end for audit trail
	}

	/**
	 * If the password is already received and stored, return true.
	 * 
	 * @param manualPasswordRequestDO
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean checkIfPasswordRecievedAlready(ManualPasswordRequestDO manualPasswordRequestDO) {
		getLogger().debug("Checking if password receievd already");
		boolean isPasswordReceivedAlready = false;
		try {
			String password = packDetailsService.getPassword(manualPasswordRequestDO.getPackIdentifier());
			if (password == null || password.isEmpty()) {
				getLogger().debug("Password is not received");
				isPasswordReceivedAlready = false;
			} else {
				getLogger().debug("Password is already received");
				isPasswordReceivedAlready = true;
			}

		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing checkIfPasswordRecievedAlready...", e);
		}
		return isPasswordReceivedAlready;

	}

	/**
	 * @param manualPasswordRequestDO
	 * @throws PasswordFetchException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void validateManualPasswordRequestDO(ManualPasswordRequestDO manualPasswordRequestDO)
			throws PasswordFetchException {
		if (manualPasswordRequestDO == null || manualPasswordRequestDO.getPackCode() == null
				|| manualPasswordRequestDO.getPackVersion() == null
				|| manualPasswordRequestDO.getPactActivationTime() == null)
			throw new PasswordFetchException("Invalid Data!");
	}

	/**
	 * @param manualPasswordRequestDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private String getPackDownloadPath(ManualPasswordRequestDO manualPasswordRequestDO)
			throws GenericDataModelException {

		String packIdentifier = manualPasswordRequestDO.getPackIdentifier();
		AcsPacks packDetailsTO = packDetailsService.getPackDetailsbyPackIdentifier(packIdentifier);
		if (packDetailsTO == null) {
			logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
			throw new GenericDataModelException(ACSExceptionConstants.NO_PACK_INFO_EXISTS,
					"No pack info available for specified packIdentifier = " + packIdentifier);
		}
		String packDownloadPath =
				System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR + File.separator
						+ ACSConstants.ACS_TEMP_DIR + File.separator + packIdentifier;
		String packFileDownloadPathinDB = packDetailsTO.getPackFileDownloadPath();
		String fileName = new File(packFileDownloadPathinDB).getName();
		String zipFile = packDownloadPath + File.separator + fileName;
		return zipFile;
	}

	private String encryptPassword(String password, String packIdentifier) throws PasswordFetchException {
		getLogger().debug("encrypting the password...");
		String encryptedPassword = null;
		try {
			List<String> batchCodes = batchService.getBatchCodesByPackIdentifier(packIdentifier);
			String salt = cdeaService.getEventCodeByBatchCode(batchCodes.get(0));
			CryptUtil cryptUtil = new CryptUtil();
			encryptedPassword = cryptUtil.encryptTextUsingAES(password, salt);

		} catch (GenericDataModelException | ApolloSecurityException e) {
			getLogger().error("GenericDataModelException while executing encryptPassword...", e);
			throw new PasswordFetchException(e);
		}
		return encryptedPassword;

	}

	/**
	 * method for retrying again password from DM ..method returns the same DO with the passwordrequestStatusEnum set.
	 * If password is not valid it will be PasswordRequestStatusEnum.VALIDATION_SUCCESSFULL. Otherwise it is
	 * VALIDATION_FAILED
	 * 
	 * @param manualPasswordRequestDO
	 * @param userName
	 *            TODO
	 * @param ipAddress
	 *            TODO
	 * @return ManualPasswordRequestDO with passwordRequestStatusEnum set.
	 * @throws PasswordFetchException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ManualPasswordRequestDO retryDMPassword(ManualPasswordRequestDO manualPasswordRequestDO, String userName,
			String ipAddress) throws PasswordFetchException {
		getLogger().debug("retrying DM for password {}", manualPasswordRequestDO);
		initialize();
		validateManualPasswordRequestDO(manualPasswordRequestDO);
		auditIt(manualPasswordRequestDO, AdminAuditActionEnum.RETRY_DM_PASSWORD_FETCH, userName, ipAddress);
		PasswordRequestBean passwordRequestBean;
		try {

			// AcsPacks packDetailsTO =
			// packDetailsService.getPackDetailsbyPackIdentifier(manualPasswordRequestDO.getPackIdentifier());
			// manualPasswordRequestDO.setPackCode(packDetailsTO.getPackCode());
			boolean isPasswordRecievedAlready = checkIfPasswordRecievedAlready(manualPasswordRequestDO);
			if (isPasswordRecievedAlready) {
				getLogger().debug("Password is receieved already. Not need to proceed furthur");
				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_SUCCESSFULL);
				return manualPasswordRequestDO;
			}
			passwordRequestBean = getPasswordRequestBean(manualPasswordRequestDO);
			PasswordFetcher passwordFetcher = new PasswordFetcher();
			boolean isValidPassword =
					passwordFetcher.fetchAndValidatePassword(passwordRequestBean,
							getPackDownloadPath(manualPasswordRequestDO));
			if (isValidPassword) {
				getLogger().trace("valid password receieved. Saving it in database");
				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_SUCCESSFULL);
				boolean isActivationTobeDoneNow = checkActivationToBeDoneNow(manualPasswordRequestDO);
				// If the activation time is passed, start the activation now
				if (isActivationTobeDoneNow)
					initiateActivation(manualPasswordRequestDO);
			} else
				manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.VALIDATION_FAILED);

		} catch (GenericDataModelException e) {
			getLogger().error("GenericDataModelException while executing retryDMPassword...", e);
			throw new PasswordFetchException(e);
		}
		getLogger().debug("retryDMPassword retruning...{}", manualPasswordRequestDO);
		return manualPasswordRequestDO;
	}

	/**
	 * @param manualPasswordRequestDO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private PasswordRequestBean getPasswordRequestBean(ManualPasswordRequestDO manualPasswordRequestDO)
			throws GenericDataModelException {
		PasswordRequestBean passwordRequestBean = new PasswordRequestBean();
		String acsCode = cdeaService.getACSCodeByBatchCode(manualPasswordRequestDO.getBatchCodes().get(0));
		passwordRequestBean.setAcsCode(acsCode);
		passwordRequestBean.setBatchCode(manualPasswordRequestDO.getBatchCodes().get(0));
		passwordRequestBean.setPackCode(manualPasswordRequestDO.getPackCode());
		passwordRequestBean.setPackVersion(manualPasswordRequestDO.getPackVersion().toString());
		return passwordRequestBean;
	}

	/**
	 * Used for manual triggering of either activation of qpack or password fetch of qpack based on the pack status
	 * 
	 * @param packIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public ManualPasswordRequestDO validateQpackStatus(String packIdentifier) throws GenericDataModelException {
		initialize();
		getLogger().info("initiated validateQpackStatus where packIdentifier = {}", packIdentifier);
		ManualPasswordRequestDO manualPasswordRequestDO = new ManualPasswordRequestDO();
		AcsPacks packDetailsTO = packDetailsService.getPackDetailsbyPackIdentifier(packIdentifier);
		getLogger().info("packDetails = {} associated to the specified packIdentifier = {}", packDetailsTO,
				packIdentifier);
		if (packDetailsTO == null) {
			logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
			getLogger()
					.info("No packDetails info exist associated to the specifie packIdentifier = {}", packIdentifier);
			manualPasswordRequestDO.setErrorMessage("No packDetails info exist associated to the specifie packCode = "
					+ packIdentifier);
			manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
			return manualPasswordRequestDO;
		}

		// fill the pack details
		manualPasswordRequestDO.setPackCode(packDetailsTO.getPackCode());
		manualPasswordRequestDO.setPackVersion(packDetailsTO.getVersionNumber());
		manualPasswordRequestDO.setPactActivationTime(packDetailsTO.getActivationTime());

		List<AcsBatch> batchDetails = batchService.getBatchDetailsByPackId(packDetailsTO.getPackIdentifier());
		getLogger().info("batchDetails = {} associated to the specified packIdentifier = {}", batchDetails,
				packIdentifier);

		List<String> validBatches = validateBatches(batchDetails);
		getLogger().info("list valid batches = {}", validBatches);

		if (validBatches != null && !validBatches.isEmpty()) {
			manualPasswordRequestDO.setBatchCodes(validBatches);
			// String packIdentifier =
			// PackDetailsService.generatePackIdentifier(manualPasswordRequestDO.getPackCode(),
			// manualPasswordRequestDO.getPackVersion());
			manualPasswordRequestDO.setPackIdentifier(packIdentifier);
			switch (packDetailsTO.getPackStatus()) {
				case PASSWORD_FETCHING_FAILED:
					if (packDetailsTO.getActivationTime() == null
							|| Calendar.getInstance().after(
									buildPasswordFetcherEndTime(packDetailsTO.getActivationTime()))
							|| packDetailsTO.getIsManualUpload()) {
						getLogger().info(
								"initiating manual pop for fetching password where manualPasswordRequestDO = {}",
								manualPasswordRequestDO);

						manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.REQUEST);
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_SUCCESS);
					} else {
						getLogger()
								.info("Not initiating password request because automatic password fetcher end time is not elapsed");
						manualPasswordRequestDO
								.setErrorMessage("Not initiating password request because automatic password fetcher end time is not elapsed");
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
					}
					break;
				case PASSWORD_FETCHED:
					// check whether activation time is elapsed or not if elapsed initiate activation other wise
					// skip.
					if (checkActivationToBeDoneNow(manualPasswordRequestDO)) {
						getLogger().info("initiating activation where manualPasswordRequestDO = {}",
								manualPasswordRequestDO);

						// initiates activation process
						if (initiateActivation(manualPasswordRequestDO)) {
							getLogger().info("initiated activation successfully where manualPasswordRequestDO = {}",
									manualPasswordRequestDO);
							manualPasswordRequestDO
									.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.INITIATED_ACTIVATION);
							manualPasswordRequestDO.setStatus(ACSConstants.MSG_SUCCESS);
							manualPasswordRequestDO.setErrorMessage("Initiated activation successfully");
						} else {
							getLogger().info("unable to initiate activation where manualPasswordRequestDO = {}",
									manualPasswordRequestDO);
							manualPasswordRequestDO
									.setErrorMessage("Unable to initiate pack activation because already a job exists for activation which is in progress");
							manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
						}
					} else {
						getLogger().info(
								"Not initiating activation because scheduled pack activation time is not elapsed");
						manualPasswordRequestDO
								.setErrorMessage("Not initiating activation because scheduled pack activation time is not elapsed");
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
					}
					break;
				case DOWNLOADED:
					if (packDetailsTO.getActivationTime() == null
							|| Calendar.getInstance().after(
									buildPasswordFetcherEndTime(packDetailsTO.getActivationTime()))
							|| packDetailsTO.getIsManualUpload()) {
						getLogger().info(
								"initiating manual pop for fetching password where manualPasswordRequestDO = {}",
								manualPasswordRequestDO);

						manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.REQUEST);
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_SUCCESS);
					} else {
						getLogger()
								.info("Not initiating password request because automatic password fetcher end time is not elapsed");
						manualPasswordRequestDO
								.setErrorMessage("Not initiating password request because automatic password fetcher end time is not elapsed");
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
					}
					break;
				case ACTIVATION_FAILED:

					boolean isPasswordRecievedAlready = checkIfPasswordRecievedAlready(manualPasswordRequestDO);
					logger.error("Password recieved status:{}", isPasswordRecievedAlready);
					if (!isPasswordRecievedAlready) {
						getLogger().info(
								"initiating manual pop for fetching password where manualPasswordRequestDO = {}",
								manualPasswordRequestDO);

						manualPasswordRequestDO.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.REQUEST);
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_SUCCESS);

					} else if (checkActivationToBeDoneNow(manualPasswordRequestDO)) {
						getLogger().info("initiating activation where manualPasswordRequestDO = {}",
								manualPasswordRequestDO);

						// initiates activation process
						if (initiateActivation(manualPasswordRequestDO)) {
							getLogger().info("initiated activation successfully where manualPasswordRequestDO = {}",
									manualPasswordRequestDO);
							manualPasswordRequestDO
									.setPasswordRequestStatusEnum(PasswordRequestStatusEnum.INITIATED_ACTIVATION);
							manualPasswordRequestDO.setStatus(ACSConstants.MSG_SUCCESS);
							manualPasswordRequestDO.setErrorMessage("Initiated activation successfully");
						} else {
							getLogger().info("unable to initiate activation where manualPasswordRequestDO = {}",
									manualPasswordRequestDO);
							manualPasswordRequestDO
									.setErrorMessage("Unable to initiate pack activation because already a job exists for activation which is in progress");
							manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
						}
					} else {
						getLogger().info(
								"Not initiating activation because scheduled pack activation time is not elapsed");
						manualPasswordRequestDO
								.setErrorMessage("Not initiating activation because scheduled pack activation time is not elapsed");
						manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
					}
					break;

				default:
					manualPasswordRequestDO.setErrorMessage("Currently not handling for specified status = "
							+ packDetailsTO.getPackStatus());
					manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
					break;
			}
		} else {
			getLogger().info("Batches associated to the specified pack with code = {} are elapsed", packIdentifier);
			manualPasswordRequestDO.setErrorMessage("Batches associated to the specified pack with code = "
					+ packIdentifier + " are elapsed");
			manualPasswordRequestDO.setStatus(ACSConstants.MSG_FAILURE);
		}
		getLogger().info("validateQpackStatus returning {}", manualPasswordRequestDO);
		return manualPasswordRequestDO;
	}
}
