package com.merittrac.apollo.acs.services;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpUtils;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.IncidentAuditLogEnum;
import com.merittrac.apollo.acs.dataobject.audit.IncidentAuditActionDO;
import com.merittrac.apollo.acs.entities.AcsConfigReqPack;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.common.HttpClientFactory;
import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * This Service provides methods to sync up the client time to Server time
 * 
 * @author Madhukesh_G - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class ServerTimeSyncService {

	private static final int MAX_ALLOWED_TIME_GAP_IN_MINS = 5;
	private static final Logger _logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static final NumberFormat numberFormat = new DecimalFormat("0.00");
	HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
	public Calendar getServerTime(TimeInfo info) {
		NtpV3Packet message = info.getMessage();
		int stratum = message.getStratum();
		String refType;
		if (stratum <= 0)
			refType = "(Unspecified or Unavailable)";
		else if (stratum == 1)
			refType = "(Primary Reference; e.g., GPS)"; // GPS, radio clock, etc.
		else
			refType = "(Secondary Reference; e.g. via NTP or SNTP)";
		// stratum should be 0..15...
		getLogger().info(" Stratum: " + stratum + " " + refType);
		int version = message.getVersion();
		int li = message.getLeapIndicator();
		getLogger().info(" leap=" + li + ", version=" + version + ", precision=" + message.getPrecision());

		getLogger().info(" mode: " + message.getModeName() + " (" + message.getMode() + ")");
		int poll = message.getPoll();
		// poll value typically btwn MINPOLL (4) and MAXPOLL (14)
		getLogger().info(" poll: " + (poll <= 0 ? 1 : (int) Math.pow(2, poll)) + " seconds" + " (2 ** " + poll + ")");
		double disp = message.getRootDispersionInMillisDouble();
		getLogger().info(
				" rootdelay=" + numberFormat.format(message.getRootDelayInMillisDouble()) + ", rootdispersion(ms): "
						+ numberFormat.format(disp));

		int refId = message.getReferenceId();
		String refAddr = NtpUtils.getHostAddress(refId);
		String refName = null;
		if (refId != 0) {
			if (refAddr.equals("127.127.1.0")) {
				refName = "LOCAL"; // This is the ref address for the Local Clock
			} else if (stratum >= 2) {
				// If reference id has 127.127 prefix then it uses its own reference clock
				// defined in the form 127.127.clock-type.unit-num (e.g. 127.127.8.0 mode 5
				// for GENERIC DCF77 AM; see refclock.htm from the NTP software distribution.
				if (!refAddr.startsWith("127.127")) {
					try {
						InetAddress addr = InetAddress.getByName(refAddr);
						String name = addr.getHostName();
						if (name != null && !name.equals(refAddr))
							refName = name;
					} catch (UnknownHostException e) {
						// some stratum-2 servers sync to ref clock device but fudge stratum level higher... (e.g. 2)
						// ref not valid host maybe it's a reference clock name?
						// otherwise just show the ref IP address.
						refName = NtpUtils.getReferenceClock(message);
					}
				}
			} else if (version >= 3 && (stratum == 0 || stratum == 1)) {
				refName = NtpUtils.getReferenceClock(message);
				// refname usually have at least 3 characters (e.g. GPS, WWV, LCL, etc.)
			}
			// otherwise give up on naming the beast...
		}
		if (refName != null && refName.length() > 1)
			refAddr += " (" + refName + ")";
		getLogger().info(" Reference Identifier:\t" + refAddr);

		TimeStamp refNtpTime = message.getReferenceTimeStamp();
		getLogger().info(" Reference Timestamp:\t" + refNtpTime + "  " + refNtpTime.toDateString());

		// Originate Time is time request sent by client (t1)
		TimeStamp origNtpTime = message.getOriginateTimeStamp();
		getLogger().info(" Originate Timestamp:\t" + origNtpTime + "  " + origNtpTime.toDateString());

		long destTime = info.getReturnTime();
		// Receive Time is time request received by server (t2)
		TimeStamp rcvNtpTime = message.getReceiveTimeStamp();
		getLogger().info(" Receive Timestamp:\t" + rcvNtpTime + "  " + rcvNtpTime.toDateString());

		// Transmit time is time reply sent by server (t3)
		TimeStamp xmitNtpTime = message.getTransmitTimeStamp();
		getLogger().info(" Transmit Timestamp:\t" + xmitNtpTime + "  " + xmitNtpTime.toDateString());

		// Destination time is time reply received by client (t4)
		TimeStamp destNtpTime = TimeStamp.getNtpTime(destTime);
		getLogger().info(" Destination Timestamp:\t" + destNtpTime + "  " + destNtpTime.toDateString());

		info.computeDetails(); // compute offset/delay if not already done
		Long offsetValue = info.getOffset();
		Long delayValue = info.getDelay();
		String delay = (delayValue == null) ? "N/A" : delayValue.toString();
		String offset = (offsetValue == null) ? "N/A" : offsetValue.toString();

		getLogger().info(" Roundtrip delay(ms)=" + delay + ", clock offset(ms)=" + offset); // offset in ms
		long second = (offsetValue / 1000) % 60;
		long minute = (offsetValue / (1000 * 60)) % 60;
		long hour = (offsetValue / (1000 * 60 * 60)) % 24;
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(Calendar.getInstance().getTimeInMillis() + offsetValue + delayValue);

		String time = String.format("%02d:%02d:%02d:%d", hour, minute, second, offsetValue);

		getLogger().info(time);
		getLogger().debug("New time {}", calendar.getTime());
		return calendar;

	}

	public void resetSystemTime(Calendar serverTime) throws IOException {
		getLogger().info("Resetting time to {}", serverTime);
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");

		String time = simpleDateFormat.format(serverTime.getTime());
		String timeChangeCommand = "cmd /C time ";
		getLogger().info("Command to be executed {}", timeChangeCommand + time);
		// Runtime.getRuntime().exec("cmd /C date ");
		Runtime.getRuntime().exec(timeChangeCommand + time);
		getLogger().info("reset is done");

	}

	/**
	 * Syncs the system time with the time of provided IP
	 * 
	 * @param ipAddress
	 * @param isTimeResetTobeInitiated
	 *            TODO
	 * @param batchCodes
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean SyncUp(String ipAddress, boolean isTimeResetTobeInitiated, String batchCodes) {
		getLogger().info("Syncing with the Server with IP Address: {}", ipAddress);
		boolean isSyncUpSuccessful = true;
		long diffMinutes = 0;
		Calendar dxServerTime = null;
		Calendar localTime = null;
		NTPUDPClient client = new NTPUDPClient();
		// We want to timeout if a response takes longer than 10 seconds
		client.setDefaultTimeout(10000);
		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(ipAddress);
			getLogger().info("Server Host Address: {}", hostAddr);
			TimeInfo info = client.getTime(hostAddr);
			dxServerTime = getServerTime(info);
			getLogger().info("Server Time is {}", dxServerTime.getTime());

			long serverMilliSecond = dxServerTime.getTimeInMillis();
			localTime = Calendar.getInstance();
			long localTimeInMilliSecond = localTime.getTimeInMillis();
			long diff = localTimeInMilliSecond - serverMilliSecond;
			diffMinutes = diff / (60 * 1000);
			getLogger().info("Diff in munutes: {} milliseconds: {}", diffMinutes, diff);
			long absoluteDiff = Math.abs(diffMinutes);
			if (absoluteDiff >= MAX_ALLOWED_TIME_GAP_IN_MINS) {
				getLogger().info("Time Sync is requires as the difference is more than {} mins", diffMinutes);
				// Audit log it..
				auditTimeSyncIncident(dxServerTime, localTime, ACSConstants.TIME_SYNC_INITIATED, absoluteDiff,
						batchCodes, IncidentAuditLogEnum.TIME_SYNC);
				if (isTimeResetTobeInitiated) {
					getLogger().info("*****SYSTEM TIME WILL BE RESET TO {}   *************", dxServerTime);
					resetSystemTime(dxServerTime);
					getLogger().info("Time Sync is done... Rechecking the diff again..");
					TimeInfo timeInfoAgain = client.getTime(hostAddr);
					timeInfoAgain.computeDetails();
					getLogger().info("Time now is {}", timeInfoAgain);
					Long offset = timeInfoAgain.getOffset();
					getLogger().info("Offset now is {}", offset);
					if (offset > 60000) {
						getLogger().info("Sync did not happen. Still there exists a offset of {}", offset);
						// Audit Log
						isSyncUpSuccessful = false;
						auditTimeSyncIncident(dxServerTime, localTime, ACSConstants.TIME_SYNC_FAILED,
								TimeUnit.MILLISECONDS.toSeconds(offset), batchCodes, IncidentAuditLogEnum.TIME_SYNC);
					} else {
						isSyncUpSuccessful = true;
						getLogger().info("Offset is {}. System time reset is Successfull", offset);
						auditTimeSyncIncident(dxServerTime, localTime, ACSConstants.TIME_SYNC_SUCCESS, absoluteDiff,
								batchCodes, IncidentAuditLogEnum.TIME_SYNC);
					}
				} else {
					getLogger().trace("System time reset is not done as some batches are running");
					isSyncUpSuccessful = true;
					auditTimeSyncIncident(dxServerTime, localTime,
							ACSConstants.TIME_SYNC_FAILED_BECAUSE_BATCH_IS_ACTIVE, absoluteDiff, batchCodes,
							IncidentAuditLogEnum.TIME_SYNC);
				}
			} else {
				getLogger().info(
						"As the Server and client time difference is less than {}, No time reset is initiated",
						MAX_ALLOWED_TIME_GAP_IN_MINS);
				isSyncUpSuccessful = true;
			}

		} catch (IOException e) {
			getLogger().error("UnknownHostException while executing SyncUp...", e);
			isSyncUpSuccessful = false;
			auditTimeSyncIncident(null, null, null, 0, batchCodes, IncidentAuditLogEnum.TIME_SYNC_FAILED);
		} finally {
			getLogger().info("SyncUp is returning {}", isSyncUpSuccessful);
		}
		return isSyncUpSuccessful;

	}

	public static Logger getLogger() {
		return _logger;
	}

	public void auditTimeSyncIncident(Calendar dxServerTime, Calendar localTime, String syncStatus,
			long differenceTimeInMins, String batchCode, IncidentAuditLogEnum incidentAuditLogEnum) {
		// for (String batchCode : batchCodes) {

		// audit in incident log
		IncidentAuditActionDO incidentAuditAction =
				new IncidentAuditActionDO(NetworkUtility.getIp(), TimeUtil.convertTimeAsString(Calendar.getInstance()
						.getTimeInMillis()), incidentAuditLogEnum);
		incidentAuditAction.setBatchCode(batchCode);

		if (incidentAuditLogEnum.equals(IncidentAuditLogEnum.TIME_SYNC)) {
			incidentAuditAction.setDxServerTime(TimeUtil.convertTimeAsString(dxServerTime.getTimeInMillis()));
			incidentAuditAction.setLocalTime(TimeUtil.convertTimeAsString(localTime.getTimeInMillis()));
			incidentAuditAction.setDifferenceTimeInMins(differenceTimeInMins);
			incidentAuditAction.setSyncStatus(syncStatus);
			// }

			AuditTrailLogger.incidentAudit(incidentAuditAction);
		}
	}
	/**
	 * Syncs the system time with the time of provided IP
	 * 
	 * @param ipAddress
	 * @param isTimeResetTobeInitiated
	 *            TODO
	 * @param batchCodes
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean SyncUp(String ipAddress, Integer port, boolean isTimeResetTobeInitiated, String batchCodes) {
		getLogger().info("Syncing with the Server with IP Address: {}", ipAddress);
		boolean isSyncUpSuccessful = true;
		long diffMinutes = 0;
		Calendar dxServerTime = null;
		Calendar localTime = null;
		InetAddress hostAddr;
		try {
			hostAddr = InetAddress.getByName(ipAddress);
			getLogger().info("Server Host Address: {}", hostAddr);
			String dmModuleName = /* "DxWeb" */
			PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.DM_MODULE_NAME);
			String timeSyncResponse =
					httpClientFactory.requestGet("http://" + ipAddress + ":" + port.toString() + "/" + dmModuleName
							+ ACSConstants.DM_REST_CLASS_IDENTIFIER,
							ACSConstants.DM_REST_METHOD_IDENTIFIER_FOR_TIME_SYNC);
			dxServerTime = new Gson().fromJson(timeSyncResponse, Calendar.class);
			getLogger().info("Server Time is {}", dxServerTime.getTime());
			long serverMilliSecond = dxServerTime.getTimeInMillis();
			localTime = Calendar.getInstance();
			long localTimeInMilliSecond = localTime.getTimeInMillis();
			long diff = localTimeInMilliSecond - serverMilliSecond;
			diffMinutes = diff / (60 * 1000);
			getLogger().info("Diff in munutes: {} milliseconds: {}", diffMinutes, diff);
			long absoluteDiff = Math.abs(diffMinutes);
			if (absoluteDiff >= MAX_ALLOWED_TIME_GAP_IN_MINS) {
				getLogger().info("Time Sync is requires as the difference is more than {} mins", diffMinutes);
				// Audit log it..
				auditTimeSyncIncident(dxServerTime, localTime, ACSConstants.TIME_SYNC_INITIATED, absoluteDiff,
						batchCodes, IncidentAuditLogEnum.TIME_SYNC);
				if (isTimeResetTobeInitiated) {
					getLogger().info("*****SYSTEM TIME WILL BE RESET TO {}   *************", dxServerTime);
					resetSystemTime(dxServerTime);
					getLogger().info("Time Sync is done...");
					isSyncUpSuccessful = true;
					getLogger().info("Offset is {}. System time reset is Successfull");
					auditTimeSyncIncident(dxServerTime, localTime, ACSConstants.TIME_SYNC_SUCCESS, absoluteDiff,
							batchCodes, IncidentAuditLogEnum.TIME_SYNC);
				} else {
					getLogger().trace("System time reset is not done as some batches are running");
					isSyncUpSuccessful = true;
					auditTimeSyncIncident(dxServerTime, localTime,
							ACSConstants.TIME_SYNC_FAILED_BECAUSE_BATCH_IS_ACTIVE, absoluteDiff, batchCodes,
							IncidentAuditLogEnum.TIME_SYNC);
				}
			} else {
				getLogger().info(
						"As the Server and client time difference is less than {}, No time reset is initiated",
						MAX_ALLOWED_TIME_GAP_IN_MINS);
				isSyncUpSuccessful = true;
			}

		} catch (Exception e) {
			getLogger().error("UnknownHostException while executing SyncUp...", e);
			isSyncUpSuccessful = false;
			auditTimeSyncIncident(null, null, null, 0, batchCodes, IncidentAuditLogEnum.TIME_SYNC_FAILED);
		} finally {
			getLogger().info("SyncUp is returning {}", isSyncUpSuccessful);
		}
		return isSyncUpSuccessful;

	}

	/**
	 * Called from UI. Forcefully synchronizes the time with DX
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean syncTimeForcefully() throws GenericDataModelException {
		String errorMsg = "";
		getLogger().debug("syncing time forcefully");
		ACSAdminService acsAdminService = new ACSAdminService();
		AcsConfigReqPack configReqPackTO = acsAdminService.getAllReqPack();
		if (configReqPackTO == null) {
			errorMsg = "No DX configured";
			getLogger().error(errorMsg);
			return false;
		}

		boolean isSyncSuccess =
				SyncUp(configReqPackTO.getIpAddress(), configReqPackTO.getPortNo(), true, ACSConstants.NO_BATCH);
		getLogger().info("syncTimeForcefully returning {}", isSyncSuccess);
		return isSyncSuccess;
	}
}
