package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.List;

import com.merittrac.apollo.acs.entities.ConfigIPRangeTO;
import com.merittrac.apollo.core.exception.GenericDataModelException;


public interface IAdminConfigIPAddService
{
	public boolean createIPAddRange(ConfigIPRangeTO configIPRange) throws GenericDataModelException;

	public boolean updateIPAddRange(ConfigIPRangeTO ConfigIPRange) throws GenericDataModelException;

	public ConfigIPRangeTO getIPAddRange(Serializable ipRangeId) throws Exception;

	public List<ConfigIPRangeTO> getAllIPAddRange() throws GenericDataModelException;

	public boolean deleteIPAddRange(Serializable ipRangeId) throws GenericDataModelException;
}
