package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.List;

import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IAdminConfigReqService
{
	public boolean createRequestPack(AcsConfigReqPack reqPack) throws GenericDataModelException;

	public boolean updateRequestPack(AcsConfigReqPack reqPack) throws GenericDataModelException;

	public AcsConfigReqPack getRequestPack(Serializable identification) throws Exception;

	public List<AcsConfigReqPack> getAllReqPack() throws GenericDataModelException;

	public boolean deleteRequestPack(Serializable identification) throws Exception;
}
