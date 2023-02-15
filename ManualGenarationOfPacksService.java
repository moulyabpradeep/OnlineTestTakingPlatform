/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Jan 16, 2014 12:53:15 PM - V_Praveen
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.dataobject.BatchCodesDO;
import com.merittrac.apollo.acs.dataobject.BatchDetailsDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.quartz.jobs.RPackGenerator;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * api's for generating packs manually.
 * 
 * @author V_Praveen - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class ManualGenarationOfPacksService {
	private static BatchService bs = null;
	private static CustomerBatchService cbs = null;
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	static {
		bs = BatchService.getInstance();
		cbs = new CustomerBatchService();
	}

	/**
	 * generateRpackManually API used to generate R-pack Manually for a specified batch.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean generateRpackManually(String batchCode, String user, String ipAddress)
			throws GenericDataModelException {
		logger.info("generateRpackManually API initiated for batchCode={}", batchCode);

		if (batchCode == null) {
			logger.info("BatchCode is null");
			return false;
		}
		// audit changes
		auditIt(batchCode, user, ipAddress);
		// end audit changes

		boolean forceStart = true;
		boolean isBETJob = false;
		boolean generatedFlag = RPackGenerator.generateRpack(batchCode, forceStart, null, isBETJob);

		logger.info("generateRpackManually API successfully executed for batchCode={} and generated status ={}",
				batchCode, generatedFlag);

		return generatedFlag;
	}

	/**
	 * getBatchDetailsForRpackGenerationManually API used to get all the batch details for Rapck generation manually.
	 * 
	 * @param customerCode
	 * @param divisionCode
	 * @param eventCode
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchDetailsDO> getBatchDetailsForRpackGenerationManually(String customerCode, String divisionCode,
			String eventCode, Calendar startDate, Calendar endDate) throws GenericDataModelException {

		List<BatchDetailsDO> batchDetailsDOList =
				cbs.getBatchDetails(customerCode, divisionCode, eventCode, startDate, endDate);
		List<BatchDetailsDO> batchDetailsDOList2 = new ArrayList<BatchDetailsDO>();
		if (batchDetailsDOList != null) {

			for (BatchDetailsDO batchDetailsDO : batchDetailsDOList) {

				if (batchDetailsDO.getBatchStartTime().before(Calendar.getInstance())) {
					batchDetailsDOList2.add(batchDetailsDO);

				}

			}
		}
		return batchDetailsDOList2;
	}

	/**
	 * getBatchDetailsForRpackGenerationManually API used to get all the batch details for Rapck generation manually.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<BatchCodesDO> getBatchDetailsForRpackGenerationManually1() throws GenericDataModelException {
		List<AcsBatch> batchDetailsList = bs.getActiveBatchDetails();
		List<BatchCodesDO> batchCodesDOList = new ArrayList<BatchCodesDO>();
		BatchCodesDO batchCodesDO = new BatchCodesDO();
		for (AcsBatch batchDetailsTO : batchDetailsList) {
			if (batchDetailsTO.getMaxBatchStartTime().before(Calendar.getInstance())) {
				batchCodesDO.setBatchCode(batchDetailsTO.getBatchCode());
				batchCodesDO.setBatchName(batchDetailsTO.getBatchName());
				batchCodesDOList.add(batchCodesDO);

			}

		}
		return batchCodesDOList;
	}

	/**
	 * auditIt API used to audit
	 * 
	 * @param packIdentifiers
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditIt(String batchCode, String user, String ipAddress) {
		// added for audit trail
		String isAuditEnable = AutoConfigure.getAuditConfigureValue();
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			Object[] paramArray = { user, batchCode };
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams
					.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.FORCE_GENERATE_RPACK.toString());
			logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, user);
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_LOG_FORC_RPACK_GENERATE_MSG, paramArray);
		}
		// end for audit trail

	}

	/**
	 * generateRpackManually API used to generate R-pack Manually for a specified batch.
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean generateRpackManuallyOnDateRange(String batchCode, String user, String ipAddress, Calendar startDate,
			Calendar endDate) throws GenericDataModelException {
		logger.info("generateRpackManually API initiated for batchCode={}", batchCode);

		if (batchCode == null) {
			logger.info("BatchCode is null");
			return false;
		}
		// audit changes
		auditIt(batchCode, user, ipAddress);
		// end audit changes

		boolean forceStart = true;
		boolean isBETJob = false;
		boolean generatedFlag =
				RPackGenerator.generateRpackOnDateRange(batchCode, forceStart, null, isBETJob, startDate, endDate);

		logger.info("generateRpackManually API successfully executed for batchCode={} and generated status ={}",
				batchCode, generatedFlag);

		return generatedFlag;
	}
}
