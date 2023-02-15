package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IPackStatusService {
	/**
	 * Updates pack status for those batches corresponding to list of batchCodes.
	 * 
	 * @param packType
	 *            Type of the pack(bpack,qpack,apack,mpack).
	 * @param batchCodesList
	 *            List of batchCodes to update the packs status of those batches.
	 * @param packStatus
	 *            Status of the pack(any one of those defined in {@link PacksStatusEnum}).
	 * @return {@link Boolean}
	 * @throws GenericDataModelException
	 */
	public boolean updatePackStatus(PackContent packType, String batchCode, PacksStatusEnum packStatus,
			String errorMessage, String packIdentifier) throws GenericDataModelException;
}
