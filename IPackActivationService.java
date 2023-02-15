package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.exception.ActivationInitiationFailedException;

public interface IPackActivationService {
	public boolean activateBPack(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BpackExportEntity bpackExportEntity,
			String batchCode, String packIdentifier) throws ActivationInitiationFailedException;

	public boolean activateAPack(
			com.merittrac.apollo.common.entities.deliverymanager.optimized.ApackExportEntity apackExportEntity,
			String batchCode, String packIdentifier) throws ActivationInitiationFailedException;

	public boolean activateQPack(com.merittrac.apollo.common.entities.deliverymanager.optimized.QpackExportEntity qpee,
			String batchCode, String packIdentifier,
			com.merittrac.apollo.common.entities.deliverymanager.optimized.BatchMetaDataBean batchMetaData, boolean isforcedActivation)
			throws ActivationInitiationFailedException;
}
