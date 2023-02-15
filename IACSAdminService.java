package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.List;

import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.acs.entities.ConfigIPRangeTO;
import com.merittrac.apollo.core.exception.ExceptionConstants;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.exception.ObjectAlreadyExistsException;

public interface IACSAdminService {
	/**
	 * Creates the ip range (range in between where test players to be allowed to connect to ACS).ip ranges can be
	 * multiple.
	 * 
	 * @param configIPRange
	 *            list of configIPRange objects where each object indicates one range
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see configIPRange,ExceptionConstants
	 */
	public int createIPAddRange(List<ConfigIPRangeTO> configIPRanges, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException;

	/**
	 * Updates already existing ip ranges.
	 * 
	 * @param ConfigIPRange
	 *            list of configIPRange objects where each object indicates one range
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigIPRangeTO,ExceptionConstants
	 */
	public int updateIPAddRange(List<ConfigIPRangeTO> configIPRanges, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException;

	/**
	 * Gets the ipRange based on the specified identifier(auto generated id for an ip range).
	 * 
	 * @param ipRangeId
	 *            auto generated id for an ip range
	 * @return {@link ConfigIPRangeTO}
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ConfigIPRangeTO
	 */
	public ConfigIPRangeTO getIPAddRange(Serializable ipRangeId) throws Exception;

	/**
	 * Gets all the available ip ranges.
	 * 
	 * @return list of {@link ConfigIPRangeTO}
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigIPRangeTO
	 */
	public List<ConfigIPRangeTO> getAllIPAddRange() throws GenericDataModelException;

	/**
	 * Deletes the ip range based on the specified identifier(auto generated id for an ip range).
	 * 
	 * @param ipRangeId
	 *            auto generated id for ip range
	 * @return integer error code on failure else returns 0
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ExceptionConstants
	 */
	public int deleteIPAddRange(List<Integer> ipRangeIds, String userName, String ipAddress) throws Exception;

	/**
	 * Configures DM ip address and port number to which the ACS should request for packs.
	 * 
	 * @param reqPack
	 *            list of ConfigReqPackTO object indicating the DM ip address and port number
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigReqPackTO,ExceptionConstants
	 */
	public int createRequestPack(List<AcsConfigReqPack> reqPacks, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException;

	/**
	 * Updates the already configured DM ip address nad port number.
	 * 
	 * @param reqPacks
	 *            list of ConfigReqPackTO objects indicating the DM ip address and port number
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigReqPackTO,ExceptionConstants
	 */
	public int updateRequestPack(List<AcsConfigReqPack> reqPacks, String userName, String ipAddress)
			throws GenericDataModelException, ObjectAlreadyExistsException;

	/**
	 * Gets the configured Dm ip address and port number based on the specified identifier.
	 * 
	 * @param reqId
	 *            auto generated id for ConfigReqPackTO
	 * @return ConfigReqPackTO indicating DM ip address and port number
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ConfigReqPackTO
	 */
	public AcsConfigReqPack getRequestPack(Serializable reqId) throws Exception;

	/**
	 * Gets all the configured DM ip address and port numbers.
	 * 
	 * @return list if ConfigReqPackTO indicating DM ip adress and port number
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigReqPackTO
	 */
	public AcsConfigReqPack getAllReqPack() throws GenericDataModelException;

	/**
	 * Deletes the configured DM ip address and port number for the specified identifier.
	 * 
	 * @param reqIds
	 *            list of auto generated identifiers for ConfigReqPackTO's
	 * @return integer error code on failure else returns 0
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ConfigReqPackTO,ExceptionConstants
	 */
	public int deleteRequestPack(List<Integer> reqIds, String userName, String ipAddress) throws Exception;

	/**
	 * Configures response server related information (ip address and port number of response server)
	 * 
	 * @param resPacks
	 *            list of ConfigResPackTO objects which indicates the response server info(ip address and port number)
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigResPackTO,ExceptionConstants
	 */
	/*public int createResponsetPack(List<ConfigResPackTO> resPacks) throws GenericDataModelException,
			ObjectAlreadyExistsException;*/

	/**
	 * Updates response server related information (ip address and port number of response server)
	 * 
	 * @param resPacks
	 *            list of ConfigResPackTO objects which indicates the response server info(ip address and port number)
	 * @return integer error code on failure else returns 0
	 * @throws GenericDataModelException
	 * @throws ObjectAlreadyExistsException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigResPackTO,ExceptionConstants
	 */
	/*public int updateResponsePack(List<ConfigResPackTO> resPacks) throws GenericDataModelException,
			ObjectAlreadyExistsException;*/

	/**
	 * Gets the response server info(ip address and port number) for the specified identifier.
	 * 
	 * @param resId
	 *            auto generated id for ConfigResPackTO object
	 * @return ConfigResPackTO object for the specified identifier
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ConfigResPackTO
	 */
	/*public ConfigResPackTO getResponsePack(Serializable resId) throws Exception;*/

	/**
	 * Gets all the configured response server related info at ACS
	 * 
	 * @return list of ConfigResPackTO objects
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see ConfigResPackTO
	 */
	/*public List<ConfigResPackTO> getAllResPack() throws GenericDataModelException;*/

	/**
	 * Deletes the response server configuration having the specified identifiers
	 * 
	 * @param resIds
	 *            list of auto generated ids of each response server configuration
	 * @return integer error code on failure else returns 0
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see ConfigResPackTO,ExceptionConstants
	 */
	/*public int deleteResponsePack(List<Integer> resIds) throws Exception;*/

	/**
	 * Stores information about the admin back up related activities.
	 * 
	 * @param adminBackup
	 *            AdminBackupTO object indicating the status about admin back up related activities
	 * @return boolean flag indicating the status
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see AdminBackupTO
	 */
	/*public boolean createAdminBackup(AdminBackupTO adminBackup) throws GenericDataModelException;*/

	/**
	 * Updates already stored information about the admin back up related activities.
	 * 
	 * @param adminBackup
	 *            AdminBackupTO object indicating the status about admin back up related activities
	 * @return boolean flag indicating the status
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see AdminBackupTO
	 */
	/*public boolean updateAdminBackup(AdminBackupTO adminBackup) throws GenericDataModelException,
			ObjectNotFoundException;*/

	/**
	 * Gets the adminBackUp status based on the specified identifier.
	 * 
	 * @param backupId
	 *            auto generated id for admin back up record
	 * @return AdminBackupTO object for the specified identifier
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see AdminBackupTO
	 */
//	public AdminBackupTO getAdminBackup(Serializable backupId) throws Exception;

	/**
	 * Gets all the admin back up related information.
	 * 
	 * @return list of AdminBackupTO objects indicating the admin back up status information
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see AdminBackupTO
	 */
//	public List<AdminBackupTO> getAllAdminBackup() throws GenericDataModelException;

	/**
	 * Deletes the admin back up associated with the specified identifier.
	 * 
	 * @param backupId
	 *            auto generated id for admin back up related information
	 * @return boolean flag indicating the status
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see AdminBackupTO
	 */
	//public boolean deleteAdminBackup(Serializable backupId) throws GenericDataModelException;
}
