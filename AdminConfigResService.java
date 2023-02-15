package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to configuring response server ip address and port number.
 * 
 * @author Amar_k - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class AdminConfigResService extends BasicService implements IAdminConfigResService
{
	/*public boolean createResponsetPack(ConfigResPackTO resPack) throws GenericDataModelException
	{
		session.persist(resPack);
		return true;
	}

	@Override
	public boolean updateResponsePack(ConfigResPackTO configResPack) throws GenericDataModelException
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigResPackTO cfg where cfg.identification=:identification";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, configResPack.getIdentification());
		ConfigResPackTO configRes = (ConfigResPackTO) session.getByQuery(query, params);
		if (configRes == null)
			session.merge(configResPack);
		else
		{
			configRes.setIpAddress(configResPack.getIpAddress());
			configRes.setPortNo(configResPack.getPortNo());
			session.merge(configRes);
		}
		return true;
	}

	@Override
	public ConfigResPackTO getResponsePack(Serializable id) throws Exception
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigResPackTO cfg where cfg.identification=:identification";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, id);
		ConfigResPackTO configRes = (ConfigResPackTO) session.getByQuery(query, params);
		if (configRes == null)
			throw new ObjectNotFoundException();
		ConfigResPackTO config = (ConfigResPackTO) session.get(configRes.getResId(), ConfigResPackTO.class.getCanonicalName());
		return config;
	}

	@Override
	public boolean deleteResponsePack(Serializable id) throws Exception
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigResPackTO cfg where cfg.identification=:identification";
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, id);
		ConfigResPackTO configRes = (ConfigResPackTO) session.getByQuery(query, params);
		if (configRes == null)
			throw new ObjectNonDeletableException();
		session.delete(configRes.getResId(), ConfigResPackTO.class.getCanonicalName());
		return true;
	}

	@Override
	public List<ConfigResPackTO> getAllResPack() throws GenericDataModelException
	{
		String query = "From com.merittrac.apollo.acs.database.tableobject.ConfigResPackTO";
		List<ConfigResPackTO> configRes = session.getListByQuery(query, null);
		return configRes;
	}*/
}
