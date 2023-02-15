package com.merittrac.apollo.acs.services;

import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.hibernate.SQLQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.dataobject.AssessmentServerDetailsDO;
import com.merittrac.apollo.acs.dataobject.HealthDO;
import com.merittrac.apollo.acs.entities.AcsConfig;
import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.acs.utility.ACSPropertyUtil;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.common.exception.HttpClientException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/*
 * Any common API's which are at a ACS Server level can be added here..
 */
public class ACSService extends BasicService implements IACSService {
	/**
	 * 
	 */
	private static final String IP_ADDRESS_FIELD = "IP Address";
	private static final String MAC_ADDRESS_FIELD = "MAC Address";
	private static final String DX_REACHABLE_FIELD = "DX Reachable";
	private static final String JMS_REACHABLE = "JMS Reachable";
	private static Logger gen_logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static ACSAdminService as = null;
	private static ACSService acsService = null;
	private static ACSPropertyUtil acsPropertyUtil = null;

	static {
		if (as == null)
			as = new ACSAdminService();
		if (acsPropertyUtil == null) {
			acsPropertyUtil = new ACSPropertyUtil();
		}
	}

	public ACSService() {
	}

	/**
	 * To access the static service
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final ACSService getInstance() {
		if (acsService == null) {
			synchronized (ACSService.class) {
				if (acsService == null) {
					acsService = new ACSService();
				}
			}
		}
		return acsService;
	}

	// This will return the current ACS server time
	@Override
	public Calendar getACSTime() {
		StatsCollector statsCollector = StatsCollector.getInstance();
		long startTime = statsCollector.begin();
		Calendar calendar = Calendar.getInstance();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
		simpleDateFormat.setCalendar(calendar);
		statsCollector.log(startTime, "getACSTime", "ACSService", 0);
		return calendar;
	}

	@Override
	public long getACSTimeWithTimeStamp() {
		long startTime = StatsCollector.getInstance().begin();
		Calendar calender = getACSTime();
		Timestamp time = new Timestamp(calender.getTimeInMillis());
		StatsCollector.getInstance().log(startTime, "getACSTimeWithTimeStamp", "ACSService", 0);
		return time.getTime();
	}

	/*
	 * This method setACSDateTime() is used to update the ACS server time,this will accept calendar values as parameter
	 * and update the ACS server time. This method returns the boolean value,if it is true then ACS server time is
	 * synchronized with the passed value else false.
	 */
	@Override
	public boolean setACSDateTime(Calendar cal) {
		boolean value = false;
		Runtime rt = Runtime.getRuntime();
		Process proc;
		try {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
			SimpleDateFormat simpleTimeFormat = new SimpleDateFormat("HH:mm:ss");
			SimpleDateFormat simpleTimeSecFormat = new SimpleDateFormat("HH:mm");
			proc = rt.exec("cmd /C date " + simpleDateFormat.format(cal.getTime()));
			proc = rt.exec("cmd /C time " + simpleTimeFormat.format(cal.getTime()));
			Calendar calACS = getACSTime();

			if ((simpleDateFormat.format(calACS.getTime()).equals(simpleDateFormat.format(cal.getTime())))
					&& (simpleTimeSecFormat.format(calACS.getTime()).equals(simpleTimeSecFormat.format(cal.getTime())))) {
				value = true;
			} else {
				value = false;
			}
		} catch (Exception e) {
			gen_logger.error(e.getMessage(), e);
		}
		return value;
	}

	public String formURL() throws GenericDataModelException {
		AcsConfigReqPack configReqPack = as.getAllReqPack();
		String dmModuleName =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.DM_MODULE_NAME);

		if (configReqPack == null || dmModuleName == null)
			return null;

		String url =
				ACSConstants.HTTP + configReqPack.getIpAddress() + ":" + configReqPack.getPortNo() + "/" + dmModuleName
						+ ACSConstants.DM_REST_CLASS_IDENTIFIER;
		return url;
	}

	public boolean sendAcknowledgement(String param) {
		try {
			String url = formURL();
			if (url != null && !url.isEmpty()) {
				String status =
						HttpClientFactory.getInstance().requestPostWithJson(url,
								ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_PACKS_ACKNOWLEDGEMENT, param);
				gen_logger.info("Status notification to DM = {} with param = {} and status = {}", url, param, status);
				return true;
			}
		} catch (Exception ex) {
			gen_logger.error("Exception while executing sendAcknowledgement ....", ex);
		}
		return false;
	}

	public boolean saveAssessmentServerConfig(AcsConfig assessmentServerConfig) throws GenericDataModelException {
		session.saveOrUpdate(assessmentServerConfig);
		return true;
	}

	public boolean isAssessmentServerConfigExists(String serverCode) throws GenericDataModelException {
		AcsConfig assessmentServerConfig = (AcsConfig) session.get(serverCode, AcsConfig.class.getCanonicalName());
		if (assessmentServerConfig == null) {
			return false;
		}
		return true;
	}

	public AcsConfig getAssessmentServerConfigByServerCode(String serverCode) throws GenericDataModelException {
		AcsConfig assessmentServerConfig = (AcsConfig) session.get(serverCode, AcsConfig.class.getCanonicalName());
		return assessmentServerConfig;
	}

	public AcsConfig getAssessmentServerConfigByEventCode(String eventCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.EVENT_CODE, eventCode);

		AcsConfig assessmentServerConfig =
				(AcsConfig) session.getByQuery("FROM AcsConfig a WHERE a.acseventdetails.eventCode = (:eventCode)",
						params);
		return assessmentServerConfig;
	}

	/*
	 * public boolean UpdateAssessmentServerConfig(AcsConfig assessmentServerConfig) throws GenericDataModelException {
	 * session.merge(assessmentServerConfig); return true; }
	 */

	public boolean updateSebConfigInTPConfig(String sebConfig) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("sebConfig", sebConfig);

		int count = session.updateByQuery("UPDATE TPConfigTO SET sebConfig = (:sebConfig)", params);
		if (count > 0) {
			return true;
		}
		return false;
	}

	/**
	 * fetch assessment server name and code, Version and IP Address
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AssessmentServerDetailsDO getACSServerCode() throws GenericDataModelException {
		String query = ACSQueryConstants.QUERY_FETCH_ACS_SERVER_DETAILS;
		AssessmentServerDetailsDO assessmentServerDetailsDO =
				(AssessmentServerDetailsDO) session.getByQuery(query, null, AssessmentServerDetailsDO.class);
		if (assessmentServerDetailsDO != null) {
			assessmentServerDetailsDO.setVersion(ACSConstants.VERSION + acsPropertyUtil.getACSVersion());
			// assessmentServerDetailsDO.setIpAddress(NetworkUtility.getIp());
		}
		gen_logger.debug("getACSServerCode is returning {}", assessmentServerDetailsDO);

		return assessmentServerDetailsDO;
	}

	public boolean notifyACSPackResponseToDM(String param) {
		try {
			String url = formURL();
			if (url != null && !url.isEmpty()) {
				String status =
						HttpClientFactory.getInstance().requestPostWithJson(url,
								ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_ACS_PACK_NOTIFICATION, param);
				gen_logger.info("Status notification to DM = {} with param = {} and status = {}", url, param, status);
				return true;
			}
		} catch (Exception ex) {
			gen_logger.error("Exception while executing notifyACSPackResponseToDM ....", ex);
		}
		return false;
	}

	/**
	 * This API calls SEB to regenerate launcher
	 * 
	 * @param ipAddress
	 * @return
	 * @throws Exception
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String saveNewIp(String ipAddress) throws Exception {
		gen_logger.debug("saveNewIp is called with ipAddress:{}", ipAddress);
		String generatedIP = NetworkUtility.getIp();
		if (generatedIP == null || generatedIP.isEmpty())
			throw new Exception("Could not generate IP address");
		if (!generatedIP.equals(ipAddress))
			gen_logger.error("Alas! Generated IP {} is not same as the IP provided: {}", generatedIP, ipAddress);
		String url = ACSConstants.HTTP + generatedIP + ACSConstants.SEB_RELATIVE_CREATE_LAUNCHER_URL;
		if (url != null && !url.isEmpty()) {
			org.apache.http.client.HttpClient client = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);
			HttpResponse httpResponse = client.execute(httpPost);
			gen_logger.info("Status of create laucher SEB with URL {} is = {}", url, httpResponse);
			return generatedIP;
		}

		return null;
	}

	private HealthDO validateMacAddress(AcsConfigReqPack dmServer, Set<String> macAddressses) {
		HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
		HealthDO healthDO =
				new HealthDO(DX_REACHABLE_FIELD, "Unable to connect DX Server(URL:" + dmServer.getIpAddress() + ":"
						+ dmServer.getPortNo() + ").", 2);
		try {
			Socket socket = new Socket(dmServer.getIpAddress(), dmServer.getPortNo());
			socket.close();
		} catch (Exception e) {
			healthDO =
					new HealthDO(DX_REACHABLE_FIELD, "Unable to connect DX Server(URL:" + dmServer.getIpAddress() + ":"
							+ dmServer.getPortNo() + ")." + e.getMessage(), 2);
			return healthDO;

		}

		Gson gson = new Gson();
		String dmModuleName =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.DM_MODULE_NAME);

		String url =
				ACSConstants.HTTP + dmServer.getIpAddress() + ":" + dmServer.getPortNo() + "/" + dmModuleName
						+ ACSConstants.DM_REST_CLASS_IDENTIFIER;
		String param = gson.toJson(macAddressses);
		try {
			gen_logger.debug("Calling DX for validating MAC Address: URL:" + url + " param:" + param + " Method:"
					+ ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_MAC_VALIDATION);
			String validMacAddressSet =
					httpClientFactory.requestPostWithJson(url,
							ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_MAC_VALIDATION, param);
			if (validMacAddressSet == null || validMacAddressSet.isEmpty()) {
				healthDO = new HealthDO(DX_REACHABLE_FIELD, "Could not validate MAC Address", 2);
				return healthDO;
			}
			gen_logger.debug("Rest call returning {}", validMacAddressSet);
			Set<String> validMacAddress = gson.fromJson(validMacAddressSet, HashSet.class);
			if (validMacAddress == null || validMacAddress.isEmpty()) {
				healthDO = new HealthDO(DX_REACHABLE_FIELD, "Invalid MAC Address ", 2);
			} else {
				healthDO =
						new HealthDO(DX_REACHABLE_FIELD, "Connected to:" + dmServer.getIpAddress() + ":"
								+ dmServer.getPortNo(), 0);
			}
		} catch (UnsupportedEncodingException | HttpClientException e) {
			gen_logger.error("Exception at validateMac address", e);
			healthDO =
					new HealthDO(DX_REACHABLE_FIELD, "DX Server validation failed(URL:" + dmServer.getIpAddress() + ":"
							+ dmServer.getPortNo() + ").", 2);

		}
		return healthDO;

	}

	/**
	 * checks jms connection
	 * 
	 * @param serverName
	 * @param serverPort
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private HealthDO CheckJMSStatus(String serverName, int serverPort) {
		HealthDO healthDO = new HealthDO(JMS_REACHABLE, "JMS connection failed", 2);
		try {
			Socket socket = new Socket(serverName, serverPort);
			healthDO = new HealthDO(JMS_REACHABLE, "JMS is reachable. (URL:" + serverName + ":" + serverPort, 0);
			// this.ErrorCause = "NA";
			socket.close();
		} catch (Exception e) {
			healthDO =
					new HealthDO(JMS_REACHABLE, "Unable to connect RPS Server(URL:" + serverName + ":" + serverPort
							+ ")." + e.getMessage(), 2);

		}
		return healthDO;
	}

	/**
	 * This provides the Report of the IP Address, MAC Address, DX reachabiility
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<HealthDO> getHealthReport() {
		gen_logger.debug("getHealthReport is initiated");
		List<HealthDO> healthReports = new ArrayList<>();
		String generatedIP = NetworkUtility.getIp();

		HealthDO healthDO;
		if (generatedIP == null) {
			healthDO = new HealthDO(IP_ADDRESS_FIELD, "Could not geenrate IP Address", 2);
		} else {
			healthDO = new HealthDO(IP_ADDRESS_FIELD, generatedIP, 0);
		}
		healthReports.add(healthDO);

		Set<String> allMacs = null;
		try {
			allMacs = NetworkUtility.getAllMacs();
		} catch (SocketException e) {
			healthDO = new HealthDO(MAC_ADDRESS_FIELD, e.getMessage(), 2);
			gen_logger.error("SocketException while executing getHealthReport...", e);
		}
		if (allMacs == null || allMacs.isEmpty()) {
			healthDO = new HealthDO(MAC_ADDRESS_FIELD, "Could not generate MAC Address", 2);
		}
		healthDO = new HealthDO(MAC_ADDRESS_FIELD, allMacs.toString(), 0);
		healthReports.add(healthDO);

		// GET DM Address

		AcsConfigReqPack configReqPack = null;
		try {
			configReqPack = as.getAllReqPack();
		} catch (GenericDataModelException e) {
			healthDO = new HealthDO(DX_REACHABLE_FIELD, "Could not fetch DX Details:" + e.getMessage(), 2);
			gen_logger.error("GenericDataModelException while executing getHealthReport...", e);
		}
		if (configReqPack == null)
			healthDO = new HealthDO(DX_REACHABLE_FIELD, "NO DX configured", 2);
		else
			healthDO = validateMacAddress(configReqPack, allMacs);
		healthReports.add(healthDO);

		// Check JMS connectivity
		String jmsUser = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, "UserName");
		String jmsPassword = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, "Password");
		String jmsServer = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, "Server");
		String jmsPort = PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, "Port");
		int port = Integer.parseInt(jmsPort);
		if (jmsUser == null || jmsUser.isEmpty() || jmsPassword == null || jmsPassword.isEmpty() || jmsServer == null
				|| jmsServer.isEmpty() || jmsPort == null || jmsPort.isEmpty()) {
			healthDO = new HealthDO(JMS_REACHABLE, "JMS is not configured properly", 2);
		} else {
			healthDO = CheckJMSStatus(jmsServer, port);
		}
		healthReports.add(healthDO);

		gen_logger.debug("getHealthReport is returning {}", healthReports);

		return healthReports;

	}

	/**
	 * This method is for the query execution
	 * 
	 * @param query
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int executeQuery(String query) throws GenericDataModelException {
		gen_logger.info("Executing query {}", query);
		SQLQuery createSQLQuery = session.getSession().createSQLQuery(query);
		return createSQLQuery.executeUpdate();
	}
}
