package com.merittrac.apollo.acs.services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.exception.ObjectNonDeletableException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to configuring DM ip address and port number.
 * 
 * @author Amar_k - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class AdminConfigReqService extends BasicService implements IAdminConfigReqService {
	public boolean createRequestPack(AcsConfigReqPack reqPack) throws GenericDataModelException {
		session.persist(reqPack);
		return true;
	}

	@Override
	public boolean updateRequestPack(AcsConfigReqPack configReqPack) throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, configReqPack.getIdentification());
		AcsConfigReqPack configReq =
				(AcsConfigReqPack) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACSCONFIGREQPACK_BY_IDENTIFICATION,
						params);
		if (configReq == null)
			session.merge(configReqPack);
		else {
			configReq.setIpAddress(configReqPack.getIpAddress());
			configReq.setPortNo(configReqPack.getPortNo());
			session.merge(configReq);
		}
		return true;
	}

	@Override
	public AcsConfigReqPack getRequestPack(Serializable identification) throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, identification);
		AcsConfigReqPack configReq =
				(AcsConfigReqPack) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACSCONFIGREQPACK_BY_IDENTIFICATION,
						params);
		if (configReq == null)
			throw new com.merittrac.apollo.core.exception.ObjectNotFoundException();
		AcsConfigReqPack config =
				(AcsConfigReqPack) session.get(configReq.getReqId(), AcsConfigReqPack.class.getCanonicalName());
		return config;
	}

	@Override
	public boolean deleteRequestPack(Serializable identification) throws Exception {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.IDENTIFICATION, identification);
		AcsConfigReqPack configReq =
				(AcsConfigReqPack) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACSCONFIGREQPACK_BY_IDENTIFICATION,
						params);
		if (configReq == null)
			throw new ObjectNonDeletableException();
		session.delete(configReq.getReqId(), AcsConfigReqPack.class.getCanonicalName());
		return true;
	}

	@Override
	public List<AcsConfigReqPack> getAllReqPack() throws GenericDataModelException {
		Map<String, Object> params = new HashMap<String, Object>();
		List<AcsConfigReqPack> config =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_ACSCONFIGREQPACK, null);
		return config;
	}
}
