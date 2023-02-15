package com.merittrac.apollo.acs.services;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.dataobject.EventRuleBean;
import com.merittrac.apollo.acs.entities.AcsEventRequest;
import com.merittrac.apollo.acs.entities.AcsProperties;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.EventLevelRulesdefined;
import com.merittrac.apollo.common.EventRule;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.EventActionInfoBean;
import com.merittrac.apollo.common.entities.deliverymanager.optimized.RequestStatus;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * 
 *         Logging Event level actions
 * 
 */
public class ACSEventRequestsService extends BasicService {

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static CDEAService cdeaService = null;
	static BatchService batchService = null;
	static ACSPropertyUtil acsPropertyUtil = null;

	static {

		if (cdeaService == null)
			cdeaService = new CDEAService();
		if (batchService == null)
			batchService = BatchService.getInstance();
		if (acsPropertyUtil == null)
			acsPropertyUtil = new ACSPropertyUtil();
	}

	public ACSEventRequestsService() {

	}

	/**
	 * @param dxActionsTO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateEventRequestActionStatus(AcsEventRequest acsEventRequests) throws GenericDataModelException {

		AcsEventRequest request =
				(AcsEventRequest) session
						.get(acsEventRequests.getRequestId(), AcsEventRequest.class.getCanonicalName());
		request.setRequestStatus(acsEventRequests.getRequestStatus());
		request.setUpdatedDate(Calendar.getInstance());
		session.saveOrUpdate(request);
		return true;
	}

	/**
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsEventRequest getSuccessfullEventRequestActions() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.REQUEST_STATUS, RequestStatus.COMPLETED);
		@SuppressWarnings("unchecked")
		List<AcsEventRequest> acsEventRequests =
				session.getListByQuery("from " + AcsEventRequest.class.getName()
						+ " where requestStatus= (:requestStatus) order by requestId desc", params);
		if (acsEventRequests != null && !acsEventRequests.isEmpty()) {
			return acsEventRequests.get(0);
		} else
			return null;

	}

	/**
	 * @param communicationId
	 * @param eventRequestActionMap
	 * @param eventCode
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void saveOrUpdateEventRequestActions(long communicationId,
			Map<Long, EventActionInfoBean> eventRequestActionMap, String eventCode) throws GenericDataModelException {
		logger.info("processingEventRequestActions of details = {}");
		if (eventRequestActionMap != null) {
			for (Map.Entry<Long, EventActionInfoBean> entry : eventRequestActionMap.entrySet()) {
				EventActionInfoBean acsEventRequests = entry.getValue();
				AcsEventRequest acsEventRequestsFromDB = getEventRequestsByACSCode(acsEventRequests.getCode());
				// processing failed requests
				if (acsEventRequestsFromDB != null) {
					acsEventRequestsFromDB.setUpdatedDate(Calendar.getInstance());
					acsEventRequestsFromDB.setContent("");
				} else {
					acsEventRequestsFromDB = new AcsEventRequest();
					if (acsEventRequests.getTimeStampOfAction() != null) {
						Calendar cal = Calendar.getInstance();
						cal.setTimeInMillis(acsEventRequests.getTimeStampOfAction());
						acsEventRequestsFromDB.setTimeStampOfAction(cal);
					}
					acsEventRequestsFromDB.setEventAction(acsEventRequests.getAction());
					acsEventRequestsFromDB.setCode(acsEventRequests.getCode());
					acsEventRequestsFromDB.setCreatedDate(Calendar.getInstance());
					acsEventRequestsFromDB.setRequestId(entry.getKey());
					acsEventRequestsFromDB.setCommunicationId(communicationId);
				}
				acsEventRequestsFromDB.setIsCommunicatedToDx(false);
				acsEventRequestsFromDB.setRequestStatus(RequestStatus.NEW);
				session.saveOrUpdate(acsEventRequestsFromDB);
			}
		}
	}

	/**
	 * @param id
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsEventRequest getEventRequestsById(Long id) throws GenericDataModelException {
		AcsEventRequest acsEventRequests = (AcsEventRequest) session.get(id, AcsEventRequest.class.getCanonicalName());
		return acsEventRequests;
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
		String query = ACSQueryConstants.QUERY_FETCH_ACS_EVENT_REQUESTS_COMMUNICATIONID_BY_REQUESTID;
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
	public List<AcsEventRequest> getEventRequestsByIsCommunicatedToDx() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isCommunicatedToDX", false);
		String query = ACSQueryConstants.QUERY_FETCH_ACSEVENTREQUESTS_BY_COMMUNICATEDTODX;
		@SuppressWarnings("unchecked")
		List<AcsEventRequest> acsEventRequestsList = session.getResultAsListByQuery(query, params, 0);
		return acsEventRequestsList;
	}

	/**
	 * @param requestId
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateEventRequestActionIsCommunicatedToDx(long requestId, boolean isNotificationSuccesfull) {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("isCommunicatedToDX", isNotificationSuccesfull);
		params.put("requestId", requestId);
		params.put("updatedDate", Calendar.getInstance());
		int count = 0;
		try {
			count = session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_EVENT_REQUEST_ACTION_COMMUNICATEDTODX, params);
		} catch (GenericDataModelException e) {
			logger.error("GenericDataModelException while executing updateRequestActionIsCommunicatedToDx...", e);
		}
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * @param eventCode
	 * @param mapOfEventLevelRules
	 * @throws GenericDataModelException
	 * @throws IOException 
	 * @throws JsonMappingException 
	 * @throws JsonParseException 
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean updateAcsPropertiesForEventCode(String eventCode, Map<EventRule, Object> mapOfEventLevelRules)
			throws GenericDataModelException, JsonParseException, JsonMappingException, IOException {
		List<AcsProperties> allProperties = acsPropertyUtil.getAllProperties(eventCode);
		Map<String, AcsProperties> allPropertiesMapFromDB = new HashMap<String, AcsProperties>();
		if (allProperties != null) {
			for (AcsProperties existingProperty : allProperties) {
				allPropertiesMapFromDB.put(existingProperty.getPropertyName(), existingProperty);
			}
		}
		
		//getting data from "mttestserver.properties" for event rule which is editable and not_editable
		String eventRuleToBeUpdateOrNot =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.EVENT_RULE_UPDATE_INFO);
		ObjectMapper mapper = new ObjectMapper();
		Map<String, String> eventRuleMap = mapper.readValue(eventRuleToBeUpdateOrNot, new TypeReference<Map<String, String>>(){});
		
		List<AcsProperties> propertiesToBeSaved = new ArrayList<>();
		for (Map.Entry<EventRule, Object> eventRules : mapOfEventLevelRules.entrySet()) {
			AcsProperties acsProperties = null;
			if (eventRules.getKey()!=null) {
				String propertyName = eventRules.getKey().name();
				Object propertyValue = eventRules.getValue();
				if (!allPropertiesMapFromDB.isEmpty())
					acsProperties = allPropertiesMapFromDB.get(eventRules.getKey().name());
				if (acsProperties == null) {   
					acsProperties = new AcsProperties();
					acsProperties.setProfile(eventCode);
					acsProperties.setPropertyName(propertyName);
					acsProperties.setPropertyValue(propertyValue.toString());
					propertiesToBeSaved.add(acsProperties);
				} else {
					//updating event rule which is editable.
					if(eventRuleMap.get(eventRules.getKey().toString())=="true"){
					acsProperties.setPropertyValue(propertyValue.toString());
					propertiesToBeSaved.add(acsProperties);
					}
				}
			}
		}
		session.saveOrUpdate(propertiesToBeSaved);
		PropertiesUtil.reloadPropertiesForAProfile(eventCode);
		return true;
	}

	/**
	 * @param propName
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsProperties getAcsPropertiesOnPropNameAndEventCode(String propName, String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", eventCode);
		params.put("propName", propName);
		AcsProperties acsProperties =
				(AcsProperties) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ACS_PROPERTIES_BY_EVENT_PROPNAME,
						params);
		return acsProperties;
	}

	/**
	 * @param propName
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsProperties> getAcsPropertiesForEventCode(String propName, String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("profile", eventCode);
		List<AcsProperties> acsProperties =
				(List<AcsProperties>) session.getListByQuery(ACSQueryConstants.QUERY_FETCH_PROPERTIES, params);
		return acsProperties;
	}
	
	
	
	/**
	 * getting event rule from "acsproperties" table by event code
	 * @param propName
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<Object> getEventRulePropForEventCode(String propName, String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("profile", eventCode);
		List<Object> acsProperties =
				(List<Object>) session.getListByQuery(ACSQueryConstants.QUERY_FETCH_EVENT_RULE_PROPERTIES_BY_EVENTCODE, params);
		return acsProperties;
	}
	

	/**
	 * @param id
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getEventRequestsByEventCode(String eventCode) throws GenericDataModelException {
		// get ACS server code
		AcsEventRequest acsEventRequests = getEventRequestsByACSCode(eventCode);
		if (acsEventRequests != null && acsEventRequests.getContent() != null
				&& !acsEventRequests.getContent().isEmpty())
			return acsEventRequests.getContent();
		else
			return null;
	}

	
	/**
	 * getting event rule from "acsproperties" table by event code
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getEventRulesByEventCode(String eventCode) throws GenericDataModelException {
		// get ACS server code
		Gson gson = new Gson();
		String jsonAcsProperties=null;
		List<Object> acsProperties = this.getEventRulePropForEventCode(null, eventCode);
		
		Map<EventRule,Object> map = new HashMap<EventRule,Object>();
		for (Object acsProperty : acsProperties){
			String name= null;
			String value= null;
			if (acsProperty instanceof Object[]) {
				final Object[] propertyObject = (Object[]) acsProperty;
				name=	ObjectUtils.toString(propertyObject[0]);
				value=ObjectUtils.toString(propertyObject[1]);
			}
			map.put(EventRule.valueOf(name),value);
		}
		if (map != null){
			 jsonAcsProperties = gson.toJson(map);
			return jsonAcsProperties;
		}else
			return null;
	}
	
	
	/**
	 * @param eventCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private AcsEventRequest getEventRequestsByACSCode(String eventCode) throws GenericDataModelException {
		String acsServerCode = cdeaService.getAcsCodeForEventCode(eventCode);

		HashMap<String, Object> params = new HashMap<>();
		params.put("code", acsServerCode);
		params.put("status", RequestStatus.COMPLETED);
		List<AcsEventRequest> acsEventRequests =
				(List<AcsEventRequest>) session
						.getListByQuery(ACSQueryConstants.QUERY_FETCH_ACS_EVENT_REQUEST_BY_ACS_CODE,
						params);
		if (acsEventRequests != null && !acsEventRequests.isEmpty())
		return acsEventRequests.get(0);
		else
			return null;
	}

	/**
	 * get event level rules
	 * 
	 * @param id
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public HashMap<EventRule, Object> getACSAndTPEventLevelRulesByEventCode(String eventCode)
			throws GenericDataModelException {
		String eventRulesJson = getEventRulesByEventCode(eventCode);
		HashMap<EventRule, Object> mapOfEventLevelRules = null;
		if (eventRulesJson != null) {
			mapOfEventLevelRules = new Gson().fromJson(eventRulesJson, new TypeToken<HashMap<EventRule, Object>>() {
			}.getType());
		}
		return mapOfEventLevelRules;
	}

	public List<EventRuleBean> getEventRuleBeanByEventCodeForViewingRules(final String eventCode)
			throws GenericDataModelException {
		logger.info("Inside Method getEventRulesByEventCode ", eventCode);
		String eventRulesJson = getEventRulesByEventCode(eventCode);
		List<EventRuleBean> eventRuleBeans = null;
		if (eventRulesJson != null) {
			HashMap<EventRule, Object> retMap =
					new Gson().fromJson(eventRulesJson, new TypeToken<HashMap<EventRule, Object>>() {
					}.getType());
			eventRuleBeans = new ArrayList<>();
			EventRuleBean eventRuleBean;

			for (Map.Entry<EventRule, String> eventRuleStringMap : EventLevelRulesdefined.FIELD_MAP.entrySet()) {
				if (retMap.containsKey(eventRuleStringMap.getKey())) {
					eventRuleBean = new EventRuleBean();
					eventRuleBean.setFieldValue(retMap.get(eventRuleStringMap.getKey()).toString());
					eventRuleBean.setFieldName(eventRuleStringMap.getKey().name());
					/** "boolean#radio_button#True,False#True" */
					String[] allField = StringUtils.split(eventRuleStringMap.getValue(), '#');
					eventRuleBean.setField(allField[0]);
					eventRuleBean.setFieldDefaultValue(allField[3]);
					eventRuleBean.setFieldType(allField[1]);
					eventRuleBean.setOptionalValue(Arrays.asList(StringUtils.split(allField[2], ',')));
					eventRuleBeans.add(eventRuleBean);
				}
			}
		}
		return eventRuleBeans;
	}

}
