/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Sep 16, 2013 4:39:17 PM - V_Praveen
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.dataobject.CustomerEventDivisionDO;
import com.merittrac.apollo.acs.dataobject.EODFailureLogsDO;
import com.merittrac.apollo.acs.dataobject.IEODFailureLogsDO;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * 
 * 
 * @author V_Praveen - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class EODFailureLogService {
	private static Logger gen_logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private final static Logger EOD_logger = LoggerFactory.getLogger(ACSConstants.EOD_LOGGER);
	static CDEAService cdeas = null;

	static {
		cdeas = new CDEAService();
	}

	/**
	 * logFailureInfo API used to log the packs failure info.
	 * 
	 * @param logbackParams
	 * @param msg
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final void saveLogFailureInfo(HashMap<String, String> logbackParams, String msg) {
		try {
			synchronized (MDC.class) {

				Iterator iterator = logbackParams.entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry pair = (Map.Entry) iterator.next();
					try {
						MDC.put((String) pair.getKey(), (String) pair.getKey() + " - " + (String) pair.getValue()
								+ " ::");
					} catch (Exception e) {
						gen_logger.error(e.getMessage(), e);
					}
				}
				EOD_logger.info(msg);
				MDC.clear();
			}
		} catch (Exception e) {
			gen_logger.error(e.getMessage(), e);
		}
	}

	/**
	 * logFailureInfo API used to log the failure info.
	 * 
	 * @param iaado
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final void logFailureInfo(IEODFailureLogsDO iaado) {
		try {
			final String msg = iaado.logMessage();
			HashMap<String, String> hmapValues = new HashMap<String, String>();
			EODFailureLogsDO eODFailureLogsDO = (EODFailureLogsDO) iaado;
			if (!(eODFailureLogsDO.getCustomerCode() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_CUSTOMER_CODE, eODFailureLogsDO.getCustomerCode());
			if (!(eODFailureLogsDO.getDivisionCode() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_DIVISON_CODE, eODFailureLogsDO.getDivisionCode());
			if (!(eODFailureLogsDO.getEventCode() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_EVENT_CODE, eODFailureLogsDO.getEventCode());
			if (!(eODFailureLogsDO.getPackIdentifier() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_PACK_CODE, eODFailureLogsDO.getPackIdentifier());
			if (!(eODFailureLogsDO.getVersionNumber() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_PACK_VERSION_NO, eODFailureLogsDO.getVersionNumber()
						.toString());
			if (!(eODFailureLogsDO.getBatchCodes() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_BATCH_CODES, eODFailureLogsDO.getBatchCodes().toString());

			if (!(eODFailureLogsDO.getPackType() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_PACK_TYPE, eODFailureLogsDO.getPackType());
			if (!(eODFailureLogsDO.getPackStatus() == null))
				hmapValues.put(ACSConstants.AUDIT_EOD_LOGKEY_PACK_STATUS, eODFailureLogsDO.getPackStatus().name());

			saveLogFailureInfo(hmapValues, msg);
		} catch (Exception e) {
			gen_logger.error(e.getMessage(), e);
		}
	}

	/**
	 * This API used to save pack Activation failure info to log file.
	 * 
	 * @param abstractPackEntity
	 * @param batchCodes
	 * @param message
	 * @param packType
	 * @param packStatus
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveFailureLogs(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.AbstractPackEntity abstractPackEntity,
			String batchCode, String message, PackContent packType, PacksStatusEnum packStatus) {
		try {
			List<String> batchCodes = new ArrayList<>();
			batchCodes.add(batchCode);
			gen_logger
					.debug("saveFailureLogs initiated for pack Info = {} , packType = {}, pack status = {} and batchCodes = {}",
							abstractPackEntity, packType, packStatus, batchCodes);
			if (batchCodes != null && !batchCodes.isEmpty()) {
				String packVersion =
						abstractPackEntity.getVersionNumber() == null ? null : abstractPackEntity.getVersionNumber();
				CustomerEventDivisionDO customerEventDivisionDO =
						cdeas.getEventDivisionCusDetailsbyBatchCode(batchCodes.get(0));
				EODFailureLogsDO eODFailureLogsDO = new EODFailureLogsDO();
				eODFailureLogsDO.setCustomerCode(customerEventDivisionDO.getCustomerCode());
				eODFailureLogsDO.setDivisionCode(customerEventDivisionDO.getDivisionCode());
				eODFailureLogsDO.setEventCode(customerEventDivisionDO.getEventCode());
				eODFailureLogsDO.setBatchCodes(batchCodes);
				eODFailureLogsDO.setPackIdentifier(abstractPackEntity.getPackCode());
				eODFailureLogsDO.setVersionNumber(Integer.parseInt(packVersion));
				eODFailureLogsDO.setFailureMessage(message);
				eODFailureLogsDO.setPackType(packType.name());
				eODFailureLogsDO.setPackStatus(packStatus);
				logFailureInfo(eODFailureLogsDO);
				gen_logger
						.debug("saveFailureLogs successfully executed for pack Info = {} , packType = {}, pack status = {} and batchCodes = {}",
								abstractPackEntity, packType, packStatus, batchCodes);
			} else {

				gen_logger.debug("Batch details are null or empty");

			}
		} catch (GenericDataModelException e) {
			gen_logger.error("GenericDataModelException while executing execute...", e);
		}
	}

	/**
	 * This API used to save pack uplaod failure info to log file.
	 * 
	 * @param packHeaderInfo
	 * @param errorMessage
	 * @param packStatus
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveUploadFailureLogs(Map<String, Object> packHeaderInfo, String errorMessage,
			PacksStatusEnum packStatus) {
		gen_logger.debug("saveUploadFailureLogs initiated for pack Info = {} and pack status = {}", packHeaderInfo,
				packStatus);
		if (packHeaderInfo != null && !packHeaderInfo.isEmpty()) {
			try {

				List<String> batchCodes = new ArrayList<String>();
				String batchCode = packHeaderInfo.get(ACSConstants.BATCH_CODE).toString();
				batchCodes.add(batchCode);
				CustomerEventDivisionDO customerEventDivisionDO =
						cdeas.getEventDivisionCusDetailsbyBatchCode(batchCode);
				EODFailureLogsDO eODFailureLogsDO = new EODFailureLogsDO();
				eODFailureLogsDO.setCustomerCode(customerEventDivisionDO.getCustomerCode());
				eODFailureLogsDO.setDivisionCode(customerEventDivisionDO.getDivisionCode());
				eODFailureLogsDO.setEventCode(customerEventDivisionDO.getEventCode());
				eODFailureLogsDO.setBatchCodes(batchCodes);

				eODFailureLogsDO.setPackIdentifier(packHeaderInfo.get(ACSConstants.PACK_CODE).toString());
				eODFailureLogsDO
						.setVersionNumber(Integer.valueOf((String) packHeaderInfo.get(ACSConstants.VERSION_NO)));
				eODFailureLogsDO.setFailureMessage(errorMessage);
				eODFailureLogsDO.setPackType(packHeaderInfo.get(ACSConstants.PACK_TYPE).toString());
				eODFailureLogsDO.setPackStatus(packStatus);
				logFailureInfo(eODFailureLogsDO);
				gen_logger.debug("saveUploadFailureLogs successfully executed for pack Info = {} and pack status = {}",
						packHeaderInfo, packStatus);

			} catch (GenericDataModelException e) {
				gen_logger.error("GenericDataModelException while executing saveUploadFailureLogs...", e);
			}

		}

	}
}
