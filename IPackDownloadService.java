package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.entities.deliverymanager.SFTPCredentialBean;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IPackDownloadService {
	/**
	 * This method is used to start quartz job for downloading a pack.
	 * 
	 * @param downloadURL
	 *            Download URL for a pack.
	 * @param packType
	 *            Type of the pack(bpack,qpack,apack,mpack).
	 * @param batchCodesList
	 *            List of batchCodes to identify those batches for which this pack belongs and to update the pack status
	 *            for those batches accordingly.
	 * @param downloadTime
	 *            Calendar instance which specifies when to start the download job for a pack.
	 * @return {@link Boolean}
	 * @throws GenericDataModelException
	 */
	public boolean startPacksDownloaderJob(SFTPCredentialBean sftpCredentialBean,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.AbstractPackEntity abstractPackEntity,
			String batchCode, String packIdentifier, PackContent packType,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMap)
			throws GenericDataModelException;
}
