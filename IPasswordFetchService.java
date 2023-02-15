/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: 05-Aug-2013 6:45:54 pm - Madhukesh_G
 * 
 */
package com.merittrac.apollo.acs.services;


/**
 * Interface to Fetch password for Packs
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public interface IPasswordFetchService {

	public void fetchBpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception;

	public void fetchApackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception;

	public void fetchQpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity,
			String packDownloadedPath,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaDataBean)
			throws Exception;

	/**
	 * If the packDownloadPath is not present, This method forms the path.
	 * 
	 * @param bpackExportEntity
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void fetchBpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap)
			throws Exception;

	/**
	 * If the packDownloadPath is not present, This method forms the path.
	 * 
	 * @param apackExportEntity
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void fetchApackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap)
			throws Exception;

	/**
	 * If the packDownloadPath is not present, This method forms the path.
	 * 
	 * @param qpackExportEntity
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void fetchQpackPassword(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpackExportEntity,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap)
			throws Exception;

}
