package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.entities.ConfigIPRangeTO;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.exception.ObjectNotFoundException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to configuring IP rages.
 * 
 * @author Amar_k - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */

public class AdminConfigIPAddService extends BasicService implements IAdminConfigIPAddService
{
	@Override
	public boolean createIPAddRange(ConfigIPRangeTO configIPRange) throws GenericDataModelException
	{
		session.persist(configIPRange);
		return true;
	}

	@Override
	public boolean updateIPAddRange(ConfigIPRangeTO configIPRange) throws GenericDataModelException
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigIPRangeTO cfg where cfg.ipRangeId=:ipRangeId";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IP_RANGE_ID, configIPRange.getIpRangeId());
		ConfigIPRangeTO configRange = (ConfigIPRangeTO) session.getByQuery(query, params);
		if (configRange == null)
			session.merge(configIPRange);
		else
		{
			configRange.setStartIpAdd(configIPRange.getStartIpAdd());
			configRange.setEndIpAdd(configIPRange.getEndIpAdd());
			session.merge(configRange);
		}
		return true;
	}

	@Override
	public ConfigIPRangeTO getIPAddRange(Serializable ipRangeId) throws Exception
	{
		ConfigIPRangeTO confIPRange = (ConfigIPRangeTO) session.get(ipRangeId, ConfigIPRangeTO.class.getCanonicalName());
		if (confIPRange == null)
			throw new ObjectNotFoundException();
		return confIPRange;
	}

	@Override
	public boolean deleteIPAddRange(Serializable ipRangeId) throws GenericDataModelException
	{
		session.delete(ipRangeId, ConfigIPRangeTO.class.getCanonicalName());
		return true;
	}

	@Override
	public List<ConfigIPRangeTO> getAllIPAddRange() throws GenericDataModelException
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigIPRangeTO";
		List<ConfigIPRangeTO> configRange = session.getListByQuery(query, null);
		return configRange;
	}
}
