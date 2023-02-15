package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.acs.entities.ConfigIPRangeTO;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.core.exception.ExceptionConstants;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.exception.ObjectAlreadyExistsException;
import com.merittrac.apollo.core.exception.ObjectNonDeletableException;
import com.merittrac.apollo.core.exception.ObjectNotFoundException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to ACS administrator activities.
 * 
 * @author Amar_k - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see BasicService
 */
@SuppressWarnings("unchecked")
public class ACSAdminService extends BasicService implements IACSAdminService {
	private static String isAuditEnable = null;
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);

	public ACSAdminService() {

		isAuditEnable = AutoConfigure.getAuditConfigureValue();
	}

	@Override
	public int createIPAddRange(List<ConfigIPRangeTO> configIPRanges, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException {
		try {
			logger.info("specified input : configIPRanges = {}", configIPRanges);
			if (configIPRanges != null && !configIPRanges.isEmpty()) {
				for (ConfigIPRangeTO cir : configIPRanges) {
					if (isAuditEnable != null && isAuditEnable.equals("true")) {
						Object[] paramArray = { cir.getStartIpAdd(), cir.getEndIpAdd() };
						HashMap<String, String> logbackParams = new HashMap<String, String>();
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
								AdminAuditActionEnum.IP_ADDRESS_RANGE_INSERT.toString());
						logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
						logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "CREATE");
						AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
								logbackParams, ACSConstants.INSERT_AUDIT_IP_RANGE_MSG, paramArray);
					}
					session.persist(cir);
				}
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing createIPAddRange...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing createIPAddRange...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public int updateIPAddRange(List<ConfigIPRangeTO> configIPRanges, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException {
		try {
			logger.info("specified input : configIPRange = {}", configIPRanges);
			if (configIPRanges != null && !configIPRanges.isEmpty()) {
				for (ConfigIPRangeTO cir : configIPRanges) {
					if (isAuditEnable != null && isAuditEnable.equals("true")) {
						String query = ACSQueryConstants.QUERY_FETCH_IPADD_RANGE_IPID;

						Map<String, Object> params = new HashMap<String, Object>();
						params.put(ACSConstants.IP_RANGE_ID, cir.getIpRangeId());

						ConfigIPRangeTO configIpRange = (ConfigIPRangeTO) session.getByQuery(query, params);
						logger.info("configIpRange = {}", configIpRange);

						Object[] paramArray =
								{ configIpRange.getStartIpAdd(), configIpRange.getEndIpAdd(), cir.getStartIpAdd(),
										cir.getEndIpAdd() };
						HashMap<String, String> logbackParams = new HashMap<String, String>();
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
								AdminAuditActionEnum.IP_ADDRESS_RANGE_UPDATE.toString());
						logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
						logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
						AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
								logbackParams, ACSConstants.UPDATE_AUDIT_IP_RANGE_MSG, paramArray);
					}
					// String query = ACSQueryConstants.QUERY_UPDATE_IPADD_RANGE_IPID;
					session.update(cir);
				}
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing updateIPAddRange...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing updateIPAddRange...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public ConfigIPRangeTO getIPAddRange(Serializable ipRangeId) throws Exception {
		logger.info("specified input : ipRangeId = {}", ipRangeId);
		ConfigIPRangeTO confIPRange = null;
		if (ipRangeId != null) {
			confIPRange = (ConfigIPRangeTO) session.get(ipRangeId, ConfigIPRangeTO.class.getCanonicalName());
			logger.info("configIPRange = {}", confIPRange);
		}
		if (confIPRange == null)
			throw new ObjectNotFoundException();
		else
			return confIPRange;
	}

	@Override
	public List<ConfigIPRangeTO> getAllIPAddRange() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ALL_IPADD_RANGE;
		List<ConfigIPRangeTO> configRanges = session.getListByQuery(query, null);
		logger.info("configRanges = {}", configRanges);
		return configRanges;
	}

	@Override
	public int deleteIPAddRange(List<Integer> ipRangeIds, String userName, String ipAddress) throws Exception {
		try {
			logger.info("specified input : ipRangeIds = {}", ipRangeIds);
			if (ipRangeIds != null && !ipRangeIds.isEmpty()) {
				String query = ACSQueryConstants.QUERY_FETCH_IPADD_RANGE_IPID;
				for (Integer ipId : ipRangeIds) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(ACSConstants.IP_RANGE_ID, ipId);

					ConfigIPRangeTO configIpRange = (ConfigIPRangeTO) session.getByQuery(query, params);
					logger.info("configIpRange = {}", configIpRange);
					if (configIpRange == null)
						throw new ObjectNonDeletableException();
					else {
						if (isAuditEnable != null && isAuditEnable.equals("true")) {
							Object[] paramArray = { configIpRange.getStartIpAdd(), configIpRange.getEndIpAdd() };
							HashMap<String, String> logbackParams = new HashMap<String, String>();
							logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
									AdminAuditActionEnum.IP_ADDRESS_RANGE_DELETE.toString());
							logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
							logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
							logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "DELETE");
							AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
									logbackParams, ACSConstants.DELETE_AUDIT_IP_RANGE_MSG, paramArray);
						}
						session.delete(ipId, ConfigIPRangeTO.class.getCanonicalName());
					}
				}
			}
		} catch (Exception ex) {
			logger.error("ERROR occured while executing deleteIPAddRange...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public int createRequestPack(List<AcsConfigReqPack> reqPacks, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException {
		try {
			logger.info("specified input : reqPacks = {}", reqPacks);
			if (reqPacks != null && !reqPacks.isEmpty()) {
				for (AcsConfigReqPack crp : reqPacks) {
					if (isAuditEnable != null && isAuditEnable.equals("true")) {
						Object[] paramArray =
								{ crp.getSrno(), crp.getIdentification(), crp.getIpAddress(), crp.getPortNo() };
						HashMap<String, String> logbackParams = new HashMap<String, String>();
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
								AdminAuditActionEnum.REQUEST_PACKET_INSERT.toString());
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "CREATE");
						logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
						logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
						AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
								logbackParams, ACSConstants.INSERT_AUDIT_REQUEST_PKT_MSG, paramArray);
					}
					session.persist(crp);
				}
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing createRequestPack...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing createRequestPack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public int updateRequestPack(List<AcsConfigReqPack> reqPacks, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException {
		try {
			logger.info("specified input : reqPacks = {}", reqPacks);
			if (reqPacks != null && !reqPacks.isEmpty()) {
				for (AcsConfigReqPack crp : reqPacks) {
					if (isAuditEnable != null && isAuditEnable.equals("true")) {
						String query = ACSQueryConstants.QUERY_FETCH_IPADD_REQ_BY_REQID;

						Map<String, Object> params = new HashMap<String, Object>();
						params.put(ACSConstants.REQ_ID, crp.getReqId());

						AcsConfigReqPack configReqPack = (AcsConfigReqPack) session.getByQuery(query, params);
						logger.info("configReqPack = {}", configReqPack);
						Object[] paramArray =
								{ configReqPack.getSrno(), configReqPack.getIdentification(),
										configReqPack.getIpAddress(), configReqPack.getPortNo(), crp.getSrno(),
										crp.getIdentification(), crp.getIpAddress(), crp.getPortNo() };
						HashMap<String, String> logbackParams = new HashMap<String, String>();
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
								AdminAuditActionEnum.REQUEST_PACKET_UPDATE.toString());
						logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "UPDATE");
						logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
						logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
						AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
								logbackParams, ACSConstants.UPDATE_AUDIT_REQUEST_PKT_MSG, paramArray);
					}
					// String query = ACSQueryConstants.QUERY_FETCH_IPADD_REQ_BY_REQID;
					session.update(crp);
				}
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing updateRequestPack...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing updateRequestPack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public AcsConfigReqPack getRequestPack(Serializable reqId) throws Exception {
		logger.info("specified input : reqId = {}", reqId);
		AcsConfigReqPack config = null;
		if (reqId != null) {
			config = (AcsConfigReqPack) session.get(reqId, AcsConfigReqPack.class.getCanonicalName());
		}
		logger.info("configReqPack = {}", config);
		return config;
	}

	@Override
	public AcsConfigReqPack getAllReqPack() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ALL_IPADD_REQ;
		List<AcsConfigReqPack> config = session.getListByQuery(query, null);
		if (config != null && !config.isEmpty()) {
			logger.info("configReqPack = {}", config);
			return config.get(0);
		} else
			return null;
	}

	@Override
	public int deleteRequestPack(List<Integer> reqIds, String userName, String ipAddress) throws Exception {
		try {
			logger.info("specified input : reqId = {}", reqIds);
			if (reqIds != null && !reqIds.isEmpty()) {
				String query = ACSQueryConstants.QUERY_FETCH_IPADD_REQ_BY_REQID;
				for (Serializable rpackId : reqIds) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(ACSConstants.REQ_ID, rpackId);

					AcsConfigReqPack configReq = (AcsConfigReqPack) session.getByQuery(query, params);
					logger.info("configReqPack = {}", configReq);
					if (configReq == null)
						throw new ObjectNonDeletableException();
					else {
						if (isAuditEnable != null && isAuditEnable.equals("true")) {
							Object[] paramArray =
									{ configReq.getSrno(), configReq.getIdentification(), configReq.getIpAddress(),
											configReq.getPortNo() };
							HashMap<String, String> logbackParams = new HashMap<String, String>();
							logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE,
									AdminAuditActionEnum.REQUEST_PACKET_DELETE.toString());
							logbackParams.put(ACSConstants.AUDIT_ADMIN_USER, userName);
							logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, ipAddress);
							logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONKEY, "DELETE");
							AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(
									logbackParams, ACSConstants.DELETE_AUDIT_REQUEST_PKT_MSG, paramArray);
						}
						session.delete(configReq.getReqId(), AcsConfigReqPack.class.getCanonicalName());
					}
				}
				return 0;
			}
		} catch (Exception ex) {
			logger.error("ERROR occured while executing deleteRequestPack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

/*	@Override
	public int createResponsetPack(List<ConfigResPackTO> resPacks) throws GenericDataModelException,
			ObjectAlreadyExistsException {
		try {
			logger.info("specified input : resPacks = {}", resPacks);
			if (resPacks != null && !resPacks.isEmpty()) {
				for (ConfigResPackTO crp : resPacks)
					session.persist(crp);
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing createResponsetPack...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing createResponsetPack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public int updateResponsePack(List<ConfigResPackTO> resPacks) throws GenericDataModelException,
			ObjectAlreadyExistsException {
		try {
			logger.info("specified input : resPacks = {}", resPacks);
			if (resPacks != null && !resPacks.isEmpty()) {
				for (ConfigResPackTO crpack : resPacks) {
					// String query = ACSQueryConstants.QUERY_FETCH_IPADD_RES_BY_RESID;
					session.update(crpack);
				}
			}
		} catch (GenericDataModelException ex) {
			logger.error("ERROR occured while executing updateResponsePack...", ex);
			return ExceptionConstants.OBJECT_ALREADY_EXISTS;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing updateResponsePack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
		return 0;
	}

	@Override
	public ConfigResPackTO getResponsePack(Serializable resId) throws Exception {
		logger.info("specified input : resId = {}", resId);
		ConfigResPackTO configRes = null;
		if (resId != null) {
			String query = ACSQueryConstants.QUERY_FETCH_IPADD_RES_BY_RESID;
			Map<String, Object> params = new HashMap<String, Object>();
			params.put(ACSConstants.RES_ID, resId);

			configRes = (ConfigResPackTO) session.getByQuery(query, params);
		}
		logger.info("configResPack = {}", configRes);
		if (configRes == null)
			throw new ObjectNotFoundException();
		return configRes;
	}

	@Override
	public List<ConfigResPackTO> getAllResPack() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ALL_IPADD_RES;
		List<ConfigResPackTO> configRes = session.getListByQuery(query, null);
		logger.info("configResPack = {}", configRes);
		return configRes;
	}

	@Override
	public int deleteResponsePack(List<Integer> resIds) throws Exception {
		try {
			logger.info("specified input : resIds = {}", resIds);
			if (resIds != null && !resIds.isEmpty()) {
				String query = ACSQueryConstants.QUERY_FETCH_IPADD_RES_BY_RESID;
				for (Serializable rId : resIds) {
					Map<String, Object> params = new HashMap<String, Object>();
					params.put(ACSConstants.RES_ID, rId);

					ConfigResPackTO configRes = (ConfigResPackTO) session.getByQuery(query, params);
					logger.info("configResPack = {}", configRes);
					if (configRes == null)
						throw new ObjectNonDeletableException();
					else {
						session.delete(configRes.getResId(), ConfigResPackTO.class.getCanonicalName());
					}
				}
			}
			return 0;
		} catch (Exception ex) {
			logger.error("ERROR occured while executing deleteResponsePack...", ex);
			return ExceptionConstants.INNNER_DM_ERROR;
		}
	}

	@Override
	public boolean createAdminBackup(AdminBackupTO adminBackup) throws GenericDataModelException {
		logger.info("specified input : adminBackUp = {}", adminBackup);
		if (adminBackup != null) {
			session.persist(adminBackup);
			return true;
		}
		return false;
	}

	@Override
	public boolean updateAdminBackup(AdminBackupTO adminBackup) throws GenericDataModelException,
			ObjectNotFoundException {
		logger.info("specified input : adminBackUp = {}", adminBackup);
		if (adminBackup != null) {
			String query = ACSQueryConstants.QUERY_UPDATE_BACKUP_BY_ID;

			Map<String, Object> params = new HashMap<String, Object>();
			params.put(ACSConstants.BACK_UP_ID, adminBackup.getBackupId());

			AdminBackupTO backup = (AdminBackupTO) session.getByQuery(query, params);
			logger.info("previous adminBackup = {}", backup);
			if (backup == null)
				throw new ObjectNotFoundException();
			else {
				// need to add audit trail
				session.merge(backup);
			}
			return true;
		}
		return false;
	}

	@Override
	public AdminBackupTO getAdminBackup(Serializable backupId) throws Exception {
		logger.info("specified input : backUpId = {}", backupId);
		AdminBackupTO adminBackup = null;
		if (backupId != null) {
			adminBackup = (AdminBackupTO) session.get(backupId, AdminBackupTO.class.getCanonicalName());
		}
		logger.info("adminBackup = {}", adminBackup);
		if (adminBackup == null)
			throw new ObjectNotFoundException();
		else
			return adminBackup;
	}

	@Override
	public List<AdminBackupTO> getAllAdminBackup() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ALL_BACKUP;
		List<AdminBackupTO> allBackup = session.getListByQuery(query, null);
		logger.info("AdminBackups = {}", allBackup);
		return allBackup;
	}

	@Override
	public boolean deleteAdminBackup(Serializable backupId) throws GenericDataModelException {
		logger.info("specified input : backupId = {}", backupId);
		session.delete(backupId, AdminBackupTO.class.getCanonicalName());
		return true;
	}*/
}
