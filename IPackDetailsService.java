package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IPackDetailsService {
	public boolean setPackDetails(AcsPacks abstractPackDetailsEntity) throws GenericDataModelException;

	public AcsPacks getPackDetailsbyPackIdentifier(String packIdentifier) throws GenericDataModelException;

	public AcsPacks updatePackDetails(AcsPacks packDetails) throws GenericDataModelException;

	// public boolean savePackRequestor(AcsPackRequestor packsRequestor) throws GenericDataModelException;

	// public boolean updatePackRequestor(AcsPackRequestor packsRequestor) throws GenericDataModelException;

	// public AcsPackRequestor getPackRequestor(String checksum, PackContent packType) throws GenericDataModelException;

	// public AcsPackRequestor getpackRequestorbyPackReqId(int packReqId) throws GenericDataModelException;

	// public boolean updatePackRequestStatus(PacksStatusEnum status, int packReqId) throws GenericDataModelException;

	// public List<AcsPackRequestor> getFailedPackRequestorDetails() throws GenericDataModelException;

	public boolean updatePackStatusByPackId(String packId, PacksStatusEnum packsStatus, String errorMsg)
			throws GenericDataModelException;

	public AcsPacks getLatestRPackDetailsByBatchId(String batchCode, PackContent packType)
			throws GenericDataModelException;
}
