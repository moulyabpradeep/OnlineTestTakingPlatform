package com.merittrac.apollo.acs.services;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.entities.AcsRequests;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.PackInfoBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.RequestStatus;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * 
 *         Logging DX actions BatchExtn and LateLoginExtn done at DX side
 * 
 */
public class ACSRequestsService extends BasicService implements IACSRequests {

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static CDEAService cdeaService = null;
	static BatchService batchService = null;
	static {

		if (cdeaService == null)
			cdeaService = new CDEAService();
		if (batchService == null)
			batchService = BatchService.getInstance();
	}

	public ACSRequestsService() {

	}

	/**
	 * @param dxActionsTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateRequestActionStatus(AcsRequests dxActionsTO) throws GenericDataModelException {

		AcsRequests request =
				(AcsRequests) session.get(dxActionsTO.getRequestId(), AcsRequests.class.getCanonicalName());
		request.setRequestStatus(dxActionsTO.getRequestStatus());
		request.setUpdatedDate(Calendar.getInstance());
		session.saveOrUpdate(request);
		return true;
	}

	// TODO: Query needs to be optimized
	/**
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsRequests getSuccessfullRequest() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.REQUEST_STATUS, RequestStatus.COMPLETED);
		List<AcsRequests> acsRequestsTOs =
				session.getListByQuery(
						"from AcsRequests where requestStatus= (:requestStatus) order by requestId desc", params);
		if (acsRequestsTOs != null && !acsRequestsTOs.isEmpty()) {
			return acsRequestsTOs.get(0);
		} else
			return null;

	}

	/**
	 * @param communicationId
	 * @param requestActionMap
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void loggingAcsDxActions(long communicationId, Map<Long, PackInfoBean> requestActionMap)
			throws GenericDataModelException {
		logger.info("loggingAcsRequestActions of details = {}");
		for (Map.Entry<Long, PackInfoBean> entry : requestActionMap.entrySet()) {
			PackInfoBean packInfoBean = entry.getValue();

			AcsRequests acsRequestsFromDB = getById(entry.getKey());
			// processing failed requests
			if (acsRequestsFromDB != null) {
				if (acsRequestsFromDB.getRequestStatus().name().equals(RequestStatus.FAILED.name())) {
					acsRequestsFromDB.setRequestStatus(RequestStatus.NEW);
					acsRequestsFromDB.setIsCommunicatedToDx(false);
					acsRequestsFromDB.setUpdatedDate(Calendar.getInstance());
					logger.info("UPDATE loggingAcsDxActions into acsRequestsTO Table with details = {} ",
							acsRequestsFromDB);
					session.update(acsRequestsFromDB);
				}
			} else {
				AcsRequests acsRequestsTO = new AcsRequests();
				if (packInfoBean.getTimeStampOfAction() != null) {
					Calendar cal = Calendar.getInstance();
					cal.setTimeInMillis(packInfoBean.getTimeStampOfAction());
					acsRequestsTO.setTimeStampOfAction(cal);
				}
				acsRequestsTO.setIsCommunicatedToDx(false);
				acsRequestsTO.setRequestId(entry.getKey());
				acsRequestsTO.setCommunicationId(communicationId);
				acsRequestsTO.setRequestStatus(RequestStatus.NEW);
				acsRequestsTO.setContentType(packInfoBean.getAction());
				acsRequestsTO.setCode(packInfoBean.getCode());
				logger.info("SAVE loggingAcsDxActions into acsRequestsTO Table with details = {} ", acsRequestsTO);
				session.save(acsRequestsTO);
			}
		}
	}

	/**
	 * @param requestId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsRequests getRequestById(Long requestId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("requestId", requestId);
		String query = ACSQueryConstants.QUERY_FETCH_ACSREQUESTS_BY_REQUESTID;
		AcsRequests dxActionsTO = (AcsRequests) session.getByQuery(query, params);
		if (dxActionsTO == null)
			return null;
		else
			return dxActionsTO;
	}

	/**
	 * @param id
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsRequests getById(Long id) throws GenericDataModelException {
		AcsRequests acsRequestsTO = (AcsRequests) session.get(id, AcsRequests.class.getCanonicalName());
		return acsRequestsTO;
	}

	/**
	 * @param packIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */

	public AcsRequests getRequestIdByPackIdentifier(String packIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		AcsRequests acsRequestsTO =
				(AcsRequests) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACSREQUEST_BY_PACKREQUESTOR_ID, params);
		return acsRequestsTO;
	}

	/**
	 * @param dxActionsTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateActionStatusByReqId(String packIdentifier, RequestStatus requestStatus)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("requestStatus", requestStatus);
		params.put("updatedDate", Calendar.getInstance());
		int count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_ACS_ACTION_STATUS_BY_REQUEST_ID, params);
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * @param requestId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public long getCommunicationIdById(Long requestId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("requestId", requestId);
		String query = ACSQueryConstants.QUERY_FETCH_COMMUNICATIONID_BY_REQUESTID;
		long communicationId = (long) session.getByQuery(query, params);
		return communicationId;
	}

	/**
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsRequests> getRequestsByIsCommunicatedToDx() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isCommunicatedToDX", false);
		String query = ACSQueryConstants.QUERY_FETCH_ACSREQUESTS_BY_COMMUNICATEDTODX;
		List<AcsRequests> acsActionsList = session.getResultAsListByQuery(query, params, 0);
		return acsActionsList;
	}

	/**
	 * @param requestId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateRequestActionIsCommunicatedToDx(long requestId, boolean isNotificationSuccesfull) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isCommunicatedToDX", isNotificationSuccesfull);
		params.put("requestId", requestId);
		params.put("updatedDate", Calendar.getInstance());
		int count = 0;
		try {
			count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_REQUEST_ACTION_COMMUNICATEDTODX, params);
		} catch (GenericDataModelException e) {
			logger.error("GenericDataModelException while executing updateRequestActionIsCommunicatedToDx...", e);
		}
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Updates the requests as failures
	 * 
	 * @param requestActionMap
	 * @param l
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateFailures(Map<Long, PackInfoBean> requestActionMap, long communicationId)
			throws GenericDataModelException {
		logger.info("updateFailures initiated with {}", requestActionMap);
		boolean isSuccess = true;
		Set<Long> failedRequestIds = requestActionMap.keySet();
		for (Long requestId : failedRequestIds) {
			AcsRequests request = (AcsRequests) session.get(requestId, AcsRequests.class.getCanonicalName());
			request.setRequestStatus(RequestStatus.FAILED);
			request.setUpdatedDate(Calendar.getInstance());
			session.saveOrUpdate(request);
		}
		return isSuccess;
	}

	/**
	 * @param requestIds
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsRequests> getRequestsById(Set<Long> requestIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("requestId", requestIds);
		String query = ACSQueryConstants.QUERY_FETCH_ACS_REQUESTS_BY_REQUESTID;
		List<AcsRequests> dxActionsTO = session.getResultAsListByQuery(query, params, 0);
		if (dxActionsTO == null)
			return null;
		else
			return dxActionsTO;
	}

}
