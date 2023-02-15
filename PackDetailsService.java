package com.merittrac.apollo.acs.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSExceptionConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.PackWipeOutPolicyEnum;
import com.merittrac.apollo.acs.constants.ReportStatusEnum;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsPacks;
import com.merittrac.apollo.acs.entities.AcsReportDetails;
import com.merittrac.apollo.acs.quartz.jobs.WipeOutPacks;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.entities.acs.EODReportPackDetailEntity;
import com.merittrac.apollo.common.entities.acs.PacksStatusEnum;
import com.merittrac.apollo.common.entities.acs.ReportTypeEnum;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Api's related to pack meta data those are sent by DM to ACS.
 * 
 * @author Siva_K - MeritTrac Services Pvt. Ltd.
 * @since Apollo v1.0
 */
public class PackDetailsService extends BasicService implements IPackDetailsService {
	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static BatchService bs = null;
	private static ManualPasswordFetchService manualPasswordFetchService = null;

	private static void initialized() {
		bs = BatchService.getInstance();
		if (manualPasswordFetchService == null) {
			manualPasswordFetchService = new ManualPasswordFetchService();
		}
	}

	public PackDetailsService() {
		initialized();
	}

	@Override
	public boolean setPackDetails(AcsPacks abstractPackDetailsEntity) throws GenericDataModelException {
		session.saveOrUpdate(abstractPackDetailsEntity);
		return true;
	}

	@Override
	public AcsPacks getPackDetailsbyPackIdentifier(String packIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);

		AcsPacks packDetails =
				(AcsPacks) session.getByQuery(ACSQueryConstants.QUERY_FETCH_PACKDETAILS_BY_PACKIDENTIFIER, params);
		return packDetails;
	}

	public AcsPacks getPackDetailsbyPackCode(String packCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packCode", packCode);

		AcsPacks packDetails =
				(AcsPacks) session.getByQuery(ACSQueryConstants.QUERY_FETCH_PACKDETAILS_BY_PACKCODE, params);
		return packDetails;
	}

	@Override
	public AcsPacks updatePackDetails(AcsPacks packDetails) throws GenericDataModelException {
		packDetails = (AcsPacks) session.merge(packDetails);
		return packDetails;
	}

	// public boolean updatePackRequestor(AcsPackRequestor packsRequestor) throws GenericDataModelException {
	// session.merge(packsRequestor);
	// return true;
	// }
	//
	// public AcsPackRequestor getPackRequestor(String checksum, PackContent packType) throws GenericDataModelException
	// {
	// HashMap<String, Object> params = new HashMap<String, Object>();
	// params.put("checksum", checksum);
	// params.put("packType", packType);
	// AcsPackRequestor packRequestor =
	// (AcsPackRequestor) session.getByQuery(
	// ACSQueryConstants.QUERY_FETCH_PACKREQUESTOR_FOR_CHECKSUM_AND_PACKTYPE, params);
	// return packRequestor;
	// }
	//
	// public AcsPackRequestor getpackRequestorbyPackReqId(int packReqId) throws GenericDataModelException {
	// AcsPackRequestor packRequestor =
	// (AcsPackRequestor) session.get(packReqId, AcsPackRequestor.class.getCanonicalName());
	// return packRequestor;
	// }

	// public boolean updatePackRequestStatus(PacksStatusEnum status, int packReqId) throws GenericDataModelException {
	// AcsPackRequestor packRequestor = getpackRequestorbyPackReqId(packReqId);
	// if (packRequestor == null) {
	// return false;
	// }
	// packRequestor.setPackStatus(status);
	// session.merge(packRequestor);
	// return true;
	// }

	public boolean updatePackRequestStatus(PacksStatusEnum status, String packIdentifier, String packVersion)
			throws GenericDataModelException {
		AcsPacks pack = getPackByPackCodeAndVersion(packIdentifier, packVersion);
		if (pack == null) {
			return false;
		}
		pack.setPackStatus(status);
		session.merge(pack);
		return true;
	}

	public List<AcsPacks> getFailedPackRequestorDetails() throws GenericDataModelException {
		List<PacksStatusEnum> packStatusList = new ArrayList<PacksStatusEnum>();
		packStatusList.add(PacksStatusEnum.DOWNLOAD_FAILED);
		packStatusList.add(PacksStatusEnum.ACTIVATION_FAILED);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packStatus", packStatusList);

		List<AcsPacks> packRequestorList =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_PACKREQUESTOR_FOR_PACKTYPE, params, 0);

		if (packRequestorList.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return packRequestorList;
	}

	@Override
	public boolean updatePackStatusByPackId(String packId, PacksStatusEnum packsStatus, String errorMsg)
			throws GenericDataModelException {
		String query =
				"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) WHERE packIdentifier=(:packId)";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packId", packId);
		params.put("packsStatus", packsStatus);
		params.put("errorMsg", errorMsg);

		switch (packsStatus) {
			case DOWNLOAD_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadStartTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;
			case DOWNLOADED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadEndTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;
			case ACTIVATION_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualActivationStartTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;
			case ACTIVATED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualActivationEndTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;
			case UPLOAD_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadStartTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;
			case UPLOADED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadEndTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;

			case ACTIVATION_FAILED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualActivationEndTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;

			case UPLOAD_FAILED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadEndTime=(:actionTime)  WHERE packIdentifier=(:packId)";
				break;

			default:
				break;
		}

		session.updateByQuery(query, params);
		logger.info("Updated pack status to = {} for pack with id = {}", packsStatus, packId);
		return true;
	}

	@Override
	public AcsPacks getLatestRPackDetailsByBatchId(String batchCode, PackContent packType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("packType", packType);
		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(
						"From AcsPacks p left join fetch p.acsbatchdetailses as b WHERE p.packType = (:packType) and b.batchCode = (:batchCode) ORDER BY p.versionNumber ASC",
						// p.acsbatchdetailses.batchCode = (:batchCode) and
						params, 0);
		if (packDetails.equals(Collections.<Object> emptyList())) {
			return null;
		} else {
			return packDetails.get(packDetails.size() - 1);
		}
	}

	public boolean
			updatePackStatusByPackIdentifier(String packIdentifier, PacksStatusEnum packsStatus, String errorMsg)
					throws GenericDataModelException {
		logger.info(
				"updatePackStatusByPackIdentifier initiated with packIdentifier:{} and packStatus:{} and errorMessage:{}",
				packIdentifier, packsStatus, errorMsg);
		String query =
				"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) WHERE packIdentifier=(:packIdentifier)";

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("packsStatus", packsStatus);
		params.put("errorMsg", errorMsg);

		switch (packsStatus) {
			case DOWNLOAD_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadStartTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case DOWNLOADED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadEndTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case ACTIVATION_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualActivationStartTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case ACTIVATED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualActivationEndTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case UPLOAD_IN_PROGRESS:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadStartTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case UPLOADED:
				params.put("actionTime", Calendar.getInstance());
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) ,actualDownloadEndTime=(:actionTime)  WHERE packIdentifier=(:packIdentifier)";
				break;
			case PASSWORD_FETCHING_FAILED:
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) WHERE packIdentifier=(:packIdentifier)";
				break;
			case PASSWORD_FETCHED:
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) WHERE packIdentifier=(:packIdentifier)";
				break;
			case PASSWORDFETCH_IN_PROGRESS:
				query =
						"UPDATE AcsPacks set packStatus=(:packsStatus) , errorMessage=(:errorMsg) WHERE packIdentifier=(:packIdentifier)";
				break;
			default:
				break;
		}

		session.updateByQuery(query, params);
		return true;
	}

	public String getResponseMetadataByPackIdentifier(String packIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		String packRequestor =
				(String) session
						.getByQuery(
								"SELECT responseMetaData as responseMetaData from AcsPackRequestor where packIdentifier=(:packIdentifier) ",
								params);
		return packRequestor;
	}

	public String getResponseMetadataFromAcsPacksByPackIdentifier(String packIdentifier)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		String packRequestor =
				(String) session
						.getByQuery(
								"SELECT responseMetaData as responseMetaData from AcsPacks where packIdentifier=(:packIdentifier) ",
								params);
		return packRequestor;
	}

	public AcsPacks getPackByPackCodeAndVersion(String packCode, String versionNumber) throws GenericDataModelException {
		String packIdentifier = generatePackIdentifier(packCode, versionNumber);
		AcsPacks acsPacks = (AcsPacks) session.get(packIdentifier, AcsPacks.class.getCanonicalName());
		return acsPacks;
	}

	// public AcsPackRequestor getPackRequestorDetailsbyPackIdentifierAndVersion(String packIdentifier, String version)
	// throws GenericDataModelException {
	// HashMap<String, Object> params = new HashMap<String, Object>();
	// params.put("packIdentifier", packIdentifier);
	// params.put("version", version);
	// AcsPackRequestor packRequestor =
	// (AcsPackRequestor) session.getByQuery(
	// ACSQueryConstants.QUERY_FETCH_PACK_REQUESTOR_DETAILS_BY_PACKIDENTIFIER_AND_VERSION, params);
	// return packRequestor;
	// }

	public String getPackIdbyPackIdentifierAndVersion(String packIdentifier, String versionNumber)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("versionNumber", versionNumber);

		String query = "FROM AcsPacks WHERE packIdentifier=(:packIdentifier) and versionNumber=(:versionNumber)";

		AcsPacks packDetails = (AcsPacks) session.getByQuery(query, params);
		if (packDetails == null) {
			return null;
		}
		return packDetails.getPackIdentifier();
	}

	public boolean updatePackStatusesByPackIds(List<String> packIds, PacksStatusEnum status)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIds", packIds);
		params.put("status", status);

		String query = "UPDATE AcsPacks set packStatus=(:status) WHERE packIdentifier IN (:packIds)";
		session.updateByQuery(query, params);
		return true;
	}

	public AcsPacks getLatestPacksByPackIdentifier(String packIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);

		List<AcsPacks> acsPacks =
				session.getResultAsListByQuery(
						"FROM AcsPacks WHERE packIdentifier=(:packIdentifier) ORDER BY packIdentifier ASC", params, 0);

		if (acsPacks.equals(Collections.<Object> emptyList())) {
			return null;
		} else {
			return acsPacks.get(acsPacks.size() - 1);
		}
	}

	public List<AcsPacks> getFailedRpacks() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("PackStatus", PacksStatusEnum.UPLOAD_FAILED);
		params.put("PackType", PackContent.Rpack);

		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_RPACK_UPLOAD_FAILED_DATA, params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	public void updatePassword(String packIdentifier, String passwordEncrypted) throws GenericDataModelException {
		logger.debug("Updating password {} for packCode {}", passwordEncrypted, packIdentifier);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("password", passwordEncrypted);
		params.put("packIdentifier", packIdentifier);

		if (session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_PACK_PASSWORD, params) > 0) {
			logger.info("successfully updated the password for pack with packIdentifier = {}", packIdentifier);
		} else {
			logger.info("unable to update the password for pack with packIdentifier = {}", packIdentifier);
		}
	}

	public String getPassword(String packIdentifier) throws GenericDataModelException {
		logger.debug("Fetching password for packIdentifier:{}", packIdentifier);
		AcsPacks acsPacks = getPackDetailsbyPackIdentifier(packIdentifier);
		if (acsPacks == null) {
			logger.info("No pack info available for specified packIdentifier = {}", packIdentifier);
			throw new GenericDataModelException(ACSExceptionConstants.NO_PACK_INFO_EXISTS,
					"No pack info available for specified packIdentifier = " + packIdentifier);
		}
		return acsPacks.getPassword();
	}

	public List<String> getPackCodesByBatchIds(List<String> batchCodes, List<PackContent> packTypes)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCodes);
		params.put("PackTypes", packTypes);

		String query =
				"SELECT p.packIdentifier FROM AcsBatch b join b.acspackdetailses p WHERE b.batchCode IN (:batchCode) and p.packType IN (:PackTypes)";
		List<String> packIdentifiers = session.getResultAsListByQuery(query, params, 0);
		if (packIdentifiers.equals(Collections.<Object> emptyList()))
			return null;
		return packIdentifiers;
	}

	public boolean startWipeOutPacksJob(List<String> batchCodes, Calendar startTime, List<PackContent> packTypes,
			PackWipeOutPolicyEnum packWipeOutPolicy, boolean forceWipeOut) {
		logger.error(
				"Info::startWipeOutPacksJob is initiated with batchIds:{} starttime:{} packTypes:{} packWipeOutPolicy:{} forceWipeout:{}",
				batchCodes, startTime, packTypes, packWipeOutPolicy, forceWipeOut);
		JobDetail job = null;
		Scheduler scheduler = null;

		String WIPE_OUT_PACKS_JOB_NAME = ACSConstants.WIPE_OUT_PACKS_JOB_NAME;
		String WIPE_OUT_PACKS_JOB_GRP = ACSConstants.WIPE_OUT_PACKS_JOB_GRP;

		if (forceWipeOut) {
			WIPE_OUT_PACKS_JOB_NAME = ACSConstants.WIPE_OUT_CANCELLED_PACKS_JOB_NAME + startTime.getTimeInMillis();
		} else {
			if (packWipeOutPolicy.equals(PackWipeOutPolicyEnum.END_OF_BATCH)) {
				WIPE_OUT_PACKS_JOB_NAME = WIPE_OUT_PACKS_JOB_NAME + batchCodes.get(0);
			} else {
				WIPE_OUT_PACKS_JOB_NAME = WIPE_OUT_PACKS_JOB_NAME + startTime.getTimeInMillis();
			}
		}

		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();

			job =
					JobBuilder.newJob(WipeOutPacks.class).withIdentity(WIPE_OUT_PACKS_JOB_NAME, WIPE_OUT_PACKS_JOB_GRP)
							.storeDurably(false).requestRecovery(true).build();
			job.getJobDataMap().put(ACSConstants.BATCH_CODE, batchCodes);
			job.getJobDataMap().put(ACSConstants.PACK_TYPES_CONST, packTypes);
			job.getJobDataMap().put(ACSConstants.INITIATED_DATE_TIME, startTime);
			job.getJobDataMap().put(ACSConstants.FORCE_WIPE_OUT, forceWipeOut);

			Trigger trigger =
					scheduler.getTrigger(TriggerKey.triggerKey(WIPE_OUT_PACKS_JOB_NAME, WIPE_OUT_PACKS_JOB_GRP));

			if (trigger != null) {
				if (trigger.getStartTime().compareTo(startTime.getTime()) < 0) {
					logger.info(
							"Already a trigger exists for PackWipeOut for specified batchId = {} hence, Unscheduling it and initiating new job",
							batchCodes);
					scheduler.unscheduleJob(TriggerKey.triggerKey(WIPE_OUT_PACKS_JOB_NAME, WIPE_OUT_PACKS_JOB_GRP));
				} else {
					logger.info("already a trigger with max  time exists");
					return false;
				}
			}

			if (forceWipeOut || startTime.before(Calendar.getInstance())) {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(WIPE_OUT_PACKS_JOB_NAME, WIPE_OUT_PACKS_JOB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startNow().build();
			} else {
				trigger =
						TriggerBuilder
								.newTrigger()
								.withIdentity(WIPE_OUT_PACKS_JOB_NAME, WIPE_OUT_PACKS_JOB_GRP)
								.withSchedule(
										SimpleScheduleBuilder.simpleSchedule().withMisfireHandlingInstructionFireNow())
								.startAt(startTime.getTime()).build();
			}
			logger.trace("Trigger for WipeOutPacksJob = {} " + trigger);
			scheduler.scheduleJob(job, trigger);
		} catch (SchedulerException ex) {
			logger.error("SchedulerException while executing startWipeOutPacksJob...", ex);
			return false;
		}
		return true;
	}

	public boolean initiatePacksWipeOut(List<String> batchCode, Calendar dateTime, boolean forceWipeOut)
			throws GenericDataModelException {
		logger.info("initiatePacksWipeOut is initiated with batchIds: {} dateTime:{} forceWipeOut:{}", batchCode,
				dateTime, forceWipeOut);
		// initiate wipe out packs for this batch, first of all read the flag
		// which says whether wipe out is
		// enabled or not
		String propValue;
		boolean packWipeOutRequired = isPackWipeOutRequired();
		if (packWipeOutRequired) {
			String[] packTypes = ACSConstants.DEFAULT_PACK_TYPES_FOR_WIPE_OUT.split(",");
			propValue =
					PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
							ACSConstants.PACK_TYPES_FOR_WIPE_OUT);
			if (propValue != null) {
				packTypes = propValue.split(",");
			}
			List<PackContent> types = new ArrayList<PackContent>();
			for (int i = 0; i < packTypes.length; i++) {
				if (packTypes[i].equalsIgnoreCase(PackContent.Apack.name())) {
					types.add(PackContent.Apack);
				} else if (packTypes[i].equalsIgnoreCase(PackContent.Bpack.name())) {
					types.add(PackContent.Bpack);
				} else if (packTypes[i].equalsIgnoreCase(PackContent.Qpack.name())) {
					types.add(PackContent.Qpack);
				} else {
					logger.info("No such type = {}, Hence ignoring it", packTypes[i]);
				}
			}

			if (!types.isEmpty() && !forceWipeOut) {
				int additionalBufferTimeToAddForPackWipeOutInMins = getAdditionalBufferTimeForWipeOut();
				PackWipeOutPolicyEnum packWipeOutPolicy = ACSConstants.DEFAULT_PACK_WIPE_OUT_POLICY;
				propValue =
						PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
								ACSConstants.PACK_WIPE_OUT_POLICY);
				if (propValue != null) {
					if (propValue.equalsIgnoreCase(PackWipeOutPolicyEnum.END_OF_BATCH.name())) {
						packWipeOutPolicy = PackWipeOutPolicyEnum.END_OF_BATCH;
					} else {
						logger.info("No such policy exists = {} hence considering default policy", propValue);
					}
				}

				if (packWipeOutPolicy.equals(PackWipeOutPolicyEnum.END_OF_DAY)) {
					batchCode = bs.getCurrentDateBatchIds(dateTime);
				}
				if (batchCode != null) {
					AcsBatch batchDetails = bs.getBatchDetailsByBatchCode(batchCode.get(batchCode.size() - 1));
					if (batchDetails != null) {
						Calendar currentDate = Calendar.getInstance();
						currentDate.set(Calendar.HOUR_OF_DAY, 0);
						currentDate.set(Calendar.MINUTE, 0);
						currentDate.set(Calendar.SECOND, 0);
						currentDate.set(Calendar.MILLISECOND, 0);

						Calendar date = (Calendar) dateTime.clone();
						date.set(Calendar.HOUR_OF_DAY, 0);
						date.set(Calendar.MINUTE, 0);
						date.set(Calendar.SECOND, 0);
						date.set(Calendar.MILLISECOND, 0);

						if (date.compareTo(currentDate) < 0) {
							startWipeOutPacksJob(batchCode, dateTime, types, packWipeOutPolicy, forceWipeOut);
						} else {
							currentDate.setTime(batchDetails.getMaxDeltaRpackGenerationTime().getTime());
							currentDate.add(Calendar.MINUTE, additionalBufferTimeToAddForPackWipeOutInMins);

							startWipeOutPacksJob(batchCode, currentDate, types, packWipeOutPolicy, forceWipeOut);
						}
					} else {
						logger.info("unable to get batchDetails for batchId = {}, Hence skipping wipe out packs job",
								batchCode.get(batchCode.size() - 1));
					}
				} else {
					logger.info("No batches exists for current date");
				}
			} else if (!types.isEmpty() && forceWipeOut) {
				startWipeOutPacksJob(batchCode, dateTime, types, null, forceWipeOut);
			} else {
				logger.info("Pack types are empty, hence skipping wipe out packs job");
			}
		} else {
			logger.info("packWipeOut is not enabled, Hence skipping it");
		}
		return true;
	}

	/**
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private boolean isPackWipeOutRequired() {
		boolean packWipeOutRequired = ACSConstants.DEFAULT_PACK_WIPE_OUT_REQUIRED;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PACK_WIPE_OUT_REQUIRED);
		if (propValue != null) {
			if (propValue.equalsIgnoreCase(Boolean.TRUE.toString())) {
				packWipeOutRequired = true;
			}
		}
		return packWipeOutRequired;
	}

	/**
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	private int getAdditionalBufferTimeForWipeOut() {
		String propValue;
		int additionalBufferTimeToAddForPackWipeOutInMins =
				ACSConstants.DEFAULT_ADDITIONAL_TIME_FOR_PACK_WIPE_OUT_IN_MINS;
		propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.ADDITIONAL_TIME_FOR_PACK_WIPE_OUT_IN_MINS);
		if (propValue != null && StringUtils.isNumeric(propValue)) {
			additionalBufferTimeToAddForPackWipeOutInMins = Integer.parseInt(propValue);
		}
		return additionalBufferTimeToAddForPackWipeOutInMins;
	}

	public List<AcsPacks> getPasswordFetchFailedPackDetails() throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packStatus", PacksStatusEnum.PASSWORD_FETCHING_FAILED);

		List<AcsPacks> PackDetailsTOList =
				session.getResultAsListByQuery(
						"FROM AcsPacks p WHERE packStatus=(:packStatus) and p.versionNumber=(select max(ip.versionNumber) from AcsPacks ip where ip.packCode = p.packCode) order by activationTime desc",
						params, 0);

		if (PackDetailsTOList.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return PackDetailsTOList;
	}

	public String getBatchCodeByPackCode(String packCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packCode);

		String batchCode =
				(String) session
						.getByQuery(
								"SELECT b.batchCode FROM AcsBatch b join b.acspackdetailses p WHERE p.packIdentifier = (:packIdentifier)",
								params);
		return batchCode;
	}

	/**
	 * This API will get the latest pack details
	 * 
	 * @param batchId
	 * @param packType
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsPacks getLatestAttendancePackDetailsByBatchId(String batchCode, PackContent packType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("packType", packType);
		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(
						"From AcsPacks p left join fetch p.acsbatchdetailses as b WHERE p.packType = (:packType) and b.batchCode = (:batchCode) ORDER BY p.packIdentifier ASC",
						// p.acsbatchdetailses.batchCode=(:batchCode) and
						params, 0);
		if (packDetails.equals(Collections.<Object> emptyList())) {
			return null;
		} else {
			return packDetails.get(packDetails.size() - 1);
		}
	}

	/**
	 * getUploadFailedAttendancePacks API used to get list of upload failed Attendance packs info.
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsPacks> getUploadFailedAttendancePacks() throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("PackStatus", PacksStatusEnum.UPLOAD_FAILED);
		params.put("PackType", PackContent.Attendancepack);

		List<AcsPacks> packDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ATTENDANCE_PACK_UPLOAD_FAILED_DATA,
						params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	/**
	 * 
	 * @param packCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<String> getALLBatchCodesByPackCode(String packCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packCode);

		List<String> batchCodesList =
				session.getResultAsListByQuery(
						"SELECT b.batchCode FROM AcsBatch b join b.acspackdetailses p WHERE p.packIdentifier = (:packIdentifier)",
						params, 0);
		if (batchCodesList.equals(Collections.<Object> emptyList())) {
			return null;
		}
		return batchCodesList;
	}

	/**
	 * 
	 * @param batchIds
	 * @param packTypes
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<EODReportPackDetailEntity> getALLPackInfoByBatchIds(String batchCodes) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_IDS, batchCodes);

		String query =
				"SELECT p.packIdentifier as packCode,p.versionNumber as versionNumber,p.packType as packType, "
						+ " p.errorMessage as errorMessage,p.activationTime as activationTime,p.actualActivationStartTime as "
						+ " actualActivationStartTime,p.actualActivationEndTime as actualActivationEndTime, "
						+ " p.actualDownloadStartTime as actualDownloadStartTime,p.actualDownloadEndTime as actualDownloadEndTime, "
						+ " p.downloadTime as downloadTime,p.manualUploadDateTime as manualUploadDateTime,p.isManualUpload as "
						+ " isManualUpload,p.isPasswordProtected as isPasswordProtected,p.password as password,p.packStatus as "
						+ " packStatus FROM AcsBatch b join b.acspackdetailses p WHERE b.batchCode IN (:batchIds)";
		// "SELECT p.packIdentifier as packCode,cast(p.versionNumber as char(10)) as versionNumber,p.packType as packType, "
		// + " p.errorMessage as errorMessage,p.activationTime as activationTime,p.actualActivationStartTime as "
		// + " actualActivationStartTime,p.actualActivationEndTime as actualActivationEndTime, "
		// + " p.actualDownloadStartTime as actualDownloadStartTime,p.actualDownloadEndTime as actualDownloadEndTime, "
		// + " p.downloadTime as downloadTime,p.manualUploadDateTime as manualUploadDateTime,p.isManualUpload as "
		// + " isManualUpload,p.isPasswordProtected as isPasswordProtected,p.password as password,p.packStatus as "
		// + " packStatus FROM acspackdetails p join acspackdetailsbatchassociation pba on "
		// + " p.packidentifier=pba.packidentifier where pba.batchcode IN (:batchIds)";
		// cast(count(cr.responseoptions) as SIGNED) as candidateResponseCount
		List<EODReportPackDetailEntity> listEODReportPackDetailEntity =
				session.getResultAsListByQuery(query, params, 0, EODReportPackDetailEntity.class);

		if (listEODReportPackDetailEntity.equals(Collections.<Object> emptyList()))
			return null;
		return listEODReportPackDetailEntity;
	}

	/**
	 * 
	 * @param batchCodes
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<EODReportPackDetailEntity> getALLPackInfoByBatchCodeWithinSpecifiedTime(String batchCodes,
			Calendar specifiedTime) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_IDS, batchCodes);
		params.put("specifiedTime", specifiedTime);

		String query =
				"SELECT p.packIdentifier as packCode,p.versionNumber as versionNumber,p.packType as packType,p.errorMessage as "
						+ " errorMessage,p.activationTime as activationTime,p.actualActivationStartTime as actualActivationStartTime, "
						+ " p.actualActivationEndTime as actualActivationEndTime,p.actualDownloadStartTime as actualDownloadStartTime, "
						+ " p.actualDownloadEndTime as actualDownloadEndTime,p.downloadTime as downloadTime,p.manualUploadDateTime as "
						+ " manualUploadDateTime,p.isManualUpload as isManualUpload,p.isPasswordProtected as isPasswordProtected,p.password as "
						+ " password,p.packStatus as packStatus FROM AcsBatch b join b.acspackdetailses p WHERE b.batchCode IN (:batchIds) "
						+ "	and (p.actualDownloadStartTime >= (:specifiedTime) or p.actualDownloadEndTime >= (:specifiedTime) or "
						+ " p.actualActivationStartTime >= (:specifiedTime) or p.actualActivationEndTime >= (:specifiedTime) or "
						+ " p.manualUploadDateTime >= (:specifiedTime))";
		List<EODReportPackDetailEntity> listEODReportPackDetailEntity =
				session.getResultAsListByQuery(query, params, 0, EODReportPackDetailEntity.class);
		if (listEODReportPackDetailEntity.equals(Collections.<Object> emptyList()))
			return null;
		return listEODReportPackDetailEntity;
	}

	public List<EODReportPackDetailEntity> getALLPackInfoBypackCode(String packCode) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packCode", packCode);

		String query =
				"SELECT p.packIdentifier as packCode,p.versionNumber as versionNumber,p.packType as packType,p.errorMessage as "
						+ " errorMessage,p.activationTime as activationTime,p.actualActivationStartTime as actualActivationStartTime, "
						+ " p.actualActivationEndTime as actualActivationEndTime,p.actualDownloadStartTime as actualDownloadStartTime, "
						+ " p.actualDownloadEndTime as actualDownloadEndTime,p.downloadTime as downloadTime,p.manualUploadDateTime as "
						+ " manualUploadDateTime,p.isManualUpload as isManualUpload,p.isPasswordProtected as isPasswordProtected,p.password as "
						+ " password,p.packStatus as packStatus FROM AcsPacks p WHERE p.packIdentifier =(:packCode)";
		List<EODReportPackDetailEntity> listEODReportPackDetailEntity =
				session.getResultAsListByQuery(query, params, 0, EODReportPackDetailEntity.class);
		if (listEODReportPackDetailEntity.equals(Collections.<Object> emptyList()))
			return null;
		return listEODReportPackDetailEntity;
	}

	public boolean saveReportDetails(AcsReportDetails reportDetailsTO) throws GenericDataModelException {
		session.persist(reportDetailsTO);
		return true;
	}

	public boolean updateReportDetails(AcsReportDetails reportDetailsTO) throws GenericDataModelException {
		session.merge(reportDetailsTO);
		return true;
	}

	public String getResponseMetadataByReportIdentifier(String reportIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportIdentifier", reportIdentifier);
		String packRequestor =
				(String) session
						.getByQuery(
								"SELECT reportMetaData as reportMetaData from AcsReportDetails where reportIdentifier=(:reportIdentifier) ",
								params);
		return packRequestor;
	}

	/**
	 * getLatestReportsDetails API used to get the latest report details of the provided Type
	 * 
	 * @param reportType
	 * @param eventCode
	 *            TODO
	 * 
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsReportDetails getLatestReportDetails(ReportTypeEnum reportType, String eventCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportType", reportType);
		params.put("eventCode", eventCode);

		List<AcsReportDetails> reportDetails =
				session.getResultAsListByQuery(
						"FROM AcsReportDetails r join fetch r.acsbatchdetails where r.reportType=(:reportType) and r.reportStatus!=\'"
								+ ReportStatusEnum.MANUALLY_GENERATED.toString() + "\' and r.eventCode=(:eventCode) "
								+ " ORDER BY r.generatedStartTime ASC", params, 0);
		if (reportDetails == null || reportDetails.equals(Collections.<Object> emptyList())) {
			return null;
		} else {
			return reportDetails.get(reportDetails.size() - 1);
		}
	}

	/**
	 * getReportsTillCurrentDate provides all the reports generated till current date
	 * 
	 * @param reportType
	 *            TODO
	 * @param eventCode
	 *            TODO
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<AcsReportDetails> getReportsTillCurrentDate(ReportTypeEnum reportType, String eventCode)
			throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportType", reportType);
		params.put("eventCode", eventCode);

		List<AcsReportDetails> reportDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL__REPORTS_DATA_TILL_CURRENT_DATE,
						params, 0);
		if (reportDetails.equals(Collections.<Object> emptyList()))
			return null;
		return reportDetails;

	}

	/**
	 * Provides the list of Reports generated after provided time
	 * 
	 * @param reportType
	 * @param startDate
	 * @param eventCode
	 *            TODO
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsReportDetails> getLatestReportsSincePerticularTime(ReportTypeEnum reportType, Calendar startDate,
			String eventCode) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportType", reportType);
		params.put("startDate", startDate);
		params.put("eventCode", eventCode);

		List<AcsReportDetails> reportDetails =
				session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_ALL_REPORTS_DATA_STARTING_FROM_A_TIME,
						params, 0);
		if (reportDetails.equals(Collections.<Object> emptyList()))
			return null;
		return reportDetails;

	}

	/**
	 * @param reportIdentifier
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsReportDetails getReportDetailByReportIdentifier(String reportIdentifier) throws GenericDataModelException {
		AcsReportDetails reportDetails =
				(AcsReportDetails) session.get(reportIdentifier, AcsReportDetails.class.getCanonicalName());
		return reportDetails;
	}

	public AcsPacks getActivatedPackDetailsbyPackIdentifier(String packIdentifier) throws GenericDataModelException {
		AcsPacks packDetails = (AcsPacks) session.get(packIdentifier, AcsPacks.class.getCanonicalName());

		if (packDetails != null && packDetails.getPackStatus().toString().equals(PacksStatusEnum.ACTIVATED.toString()))
			return packDetails;
		return null;
	}

	/**
	 * validateABQPackForRpackGeneration API will check weather A,B,Q pack successfully activated or not for Rpack
	 * generation
	 * 
	 * @param batchCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isAllPacksActivatedForBatch(String batchCode) throws GenericDataModelException {
		List<PackContent> packTypes = new ArrayList<PackContent>();
		packTypes.add(PackContent.Bpack);
		packTypes.add(PackContent.Apack);
		packTypes.add(PackContent.Qpack);

		String query = ACSQueryConstants.QUERY_FETCH_ABQ_PACK_ACTIVATION_SUCESSFULL_COUNT_FOR_BATCH;

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("packStatus", PacksStatusEnum.ACTIVATED);
		params.put(ACSConstants.PACK_TYPE, packTypes);

		long totalResultsCount = (long) session.getByQuery(query, params);
		if (totalResultsCount >= 3) {
			return true;
		}
		return false;
	}

	@Deprecated
	public boolean updatePackRequestorStatusByPackIdentifierAndVersion(PacksStatusEnum packsStatus,
			String packIdentifier, String versionNumber) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("packsStatus", packsStatus);
		params.put("versionNumber", versionNumber);
		session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_PACKREQUESTOR_STATUS_BY_PACKIDENTIFIER_AND_VERSION, params);
		return true;
	}

	/**
	 * Update {@link AcsPackRequestor} status for pack request id.
	 * 
	 * @param status
	 * @param packReqId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean updatePacksStatusByPackReqId(PacksStatusEnum status, String packIdentifier)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("status", status);

		session.updateByQuery(ACSQueryConstants.QUERY_UPDATE_PACKREQUESTOR_BY_PACKREQID, params);
		return true;
	}

	public boolean updatePacksManualUploadStatusByPackIdentifier(boolean isManualUpload, String packIdentifier)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packIdentifier);
		params.put("isManualUpload", isManualUpload);
		params.put("manualUploadDateTime", Calendar.getInstance());

		String query =
				"UPDATE AcsPacks set isManualUpload=(:isManualUpload),manualUploadDateTime=(:manualUploadDateTime) WHERE packIdentifier=(:packIdentifier)";
		session.updateByQuery(query, params);
		return true;
	}

	/**
	 * isBQPacksActivatedForBatch API will check weather A,B,Q pack successfully activated or not for Rpack generation
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isBQPacksActivatedForBatch(String batchCode) throws GenericDataModelException {
		String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_BQ_PACK_ACTIVATION_SUCESSFULL_COUNT_FOR_BATCH;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("packStatus", PacksStatusEnum.ACTIVATED);
		List<PackContent> listofPacks = new ArrayList<PackContent>();
		listofPacks.add(PackContent.Bpack);
		listofPacks.add(PackContent.Qpack);
		params.put(ACSConstants.PACK_TYPE, listofPacks);
		long totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
		if (totalResultsCount == 2) {

			return true;
		}
		return false;
	}

	/**
	 * isBPacksActivatedForBatch API will check weather Bpack successfully activated
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isBPackActivatedForBatch(String batchCode) throws GenericDataModelException {
		String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_BQ_PACK_ACTIVATION_SUCESSFULL_COUNT_FOR_BATCH;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("packStatus", PacksStatusEnum.ACTIVATED);
		List<PackContent> listofPacks = new ArrayList<PackContent>();
		listofPacks.add(PackContent.Bpack);
		params.put(ACSConstants.PACK_TYPE, listofPacks);
		long totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
		if (totalResultsCount >= 1) {
			return true;
		}
		return false;
	}

	/**
	 * isAPackActivatedForBatch API will check weather Apack successfully activated
	 * 
	 * @param batchId
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean isAPackActivatedForBatch(String batchCode) throws GenericDataModelException {
		String resultsCountQuery = ACSQueryConstants.QUERY_FETCH_BQ_PACK_ACTIVATION_SUCESSFULL_COUNT_FOR_BATCH;
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("packStatus", PacksStatusEnum.ACTIVATED);
		List<PackContent> listofPacks = new ArrayList<PackContent>();
		listofPacks.add(PackContent.Apack);
		params.put(ACSConstants.PACK_TYPE, listofPacks);
		long totalResultsCount = (long) session.getByQuery(resultsCountQuery, params);
		if (totalResultsCount == 1) {

			return true;
		}
		return false;
	}

	/**
	 * update the rpack content json in database
	 * 
	 * @param rpackContent
	 * @param packId
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updateRpackContent(String rpackContent, String packId) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("packIdentifier", packId);
		params.put("rpackContent", rpackContent);

		String query = "UPDATE AcsPacks SET packContent=(:rpackContent) WHERE packIdentifier=(:packIdentifier)";
		session.updateByQuery(query, params);
	}

	/**
	 * updates status of packs whose activation is in progress to downloaded (currently we are using this api when ever
	 * there is a tomcat restart).
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int updateActivationInProgressPacksToDownloaded(List<String> packIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("downloadedStatus", PacksStatusEnum.DOWNLOADED);
		params.put("packIds", packIds);

		String query = "UPDATE AcsPacks set packStatus=(:downloadedStatus) WHERE packIdentifier IN (:packIds)";
		int count = session.updateByQuery(query, params);

		return count;
	}

	/**
	 * updates status of packs whose activation is in progress to downloaded (currently we are using this api when ever
	 * there is a tomcat restart).
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public int updateDownloadInProgressPacksToDownloadFailed(List<String> packIds) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("downloadFailedStatus", PacksStatusEnum.DOWNLOAD_FAILED);
		params.put("packIds", packIds);

		String query = "UPDATE AcsPacks set packStatus=(:downloadFailedStatus) WHERE packIdentifier IN (:packIds)";
		int count = session.updateByQuery(query, params);

		return count;
	}

	/**
	 * gets the list pack ids whose activation is in progress
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsPacks> getDownloadOrActivationInProgressPacks() throws GenericDataModelException {
		List<PacksStatusEnum> packStatus = new ArrayList<PacksStatusEnum>();
		packStatus.add(PacksStatusEnum.DOWNLOAD_IN_PROGRESS);
		packStatus.add(PacksStatusEnum.ACTIVATION_IN_PROGRESS);

		List<PackContent> packTypes = new ArrayList<PackContent>();
		packTypes.add(PackContent.Apack);
		packTypes.add(PackContent.Bpack);
		packTypes.add(PackContent.Qpack);

		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("inprogressStatus", packStatus);
		params.put("packTypes", packTypes);

		String query = "FROM AcsPacks WHERE packStatus IN (:inprogressStatus) and packType IN (:packTypes)";
		List<AcsPacks> packDetailsTOs = session.getResultAsListByQuery(query, params, 0);
		if (packDetailsTOs.isEmpty()) {
			return null;
		} else {
			return packDetailsTOs;
		}
	}

	/**
	 * unschedule the triggers associated to the specified job
	 * 
	 * @param jobName
	 * @param jobGroup
	 * @param scheduler
	 * @return
	 * @throws SchedulerException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public boolean isJobExists(String jobName, String jobGroup, Scheduler scheduler) throws SchedulerException {
		boolean isJobExists = false;
		logger.info("initiated unScheduleInProgressJobs where jobName = {} and jobGroup = {}", jobName, jobGroup);

		JobKey jobKey = new JobKey(jobName, jobGroup);
		List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
		if (triggers != null && !triggers.isEmpty()) {
			for (Iterator iterator = triggers.iterator(); iterator.hasNext();) {
				Trigger trigger = (Trigger) iterator.next();
				if (trigger != null) {
					isJobExists = true;
					logger.info("Trigger = {} exists with the specified job key(jobName, jobGroup) = {}", trigger,
							jobKey);
					break;
				}
			}
		} else {
			logger.info("No triggers associated with specified job details where jobName = {} and jobGroup = {}",
					jobName, jobGroup);
		}
		return isJobExists;
	}

	/**
	 * validates the packs status as in if any(A,B,Q) of the pack status is inconsistent this api will reset the status
	 * with some valid status validates the pack status and revert the in progress status if any as follows 1.
	 * DOWNLOAD_INPROGRESS to DOWNLOAD_FAILED 2. ACTIVATION_INPROGRESS to DOWNLOADED
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public List<AcsPacks> validatePackStatus() {
		List<AcsPacks> packDetailsTOs = null;
		try {
			// get the list of pack ids whose activation is in progress.
			// currently considering only Apack, Bpack and
			// Qpack
			packDetailsTOs = getDownloadOrActivationInProgressPacks();
			if (packDetailsTOs != null) {
				String jobName;
				String jobGroup;

				List<String> activationInprogressPackIds = new ArrayList<String>();
				List<String> downloadInprogressPackIds = new ArrayList<String>();

				Scheduler scheduler = new StdSchedulerFactory().getScheduler();
				scheduler.start();

				for (Iterator iterator = packDetailsTOs.iterator(); iterator.hasNext();) {
					AcsPacks packDetailsTO = (AcsPacks) iterator.next();

					switch (packDetailsTO.getPackType()) {
						case Apack:
							if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.DOWNLOAD_IN_PROGRESS)) {
								jobName = ACSConstants.APACK_DOWNLOADER_NAME + packDetailsTO.getPackIdentifier();
								// + "V" + packDetailsTO.getVersionNumber()
								jobGroup = ACSConstants.APACK_DOWNLOADER_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {
										downloadInprogressPackIds.add(packDetailsTO.getPackIdentifier());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.ACTIVATION_IN_PROGRESS)) {
								jobName = ACSConstants.APACK_ACTIVATION_NAME + packDetailsTO.getPackIdentifier();
								jobGroup = ACSConstants.APACK_ACTIVATION_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {
										activationInprogressPackIds.add(packDetailsTO.getPackIdentifier());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else {
								logger.info("invalid status = {}", packDetailsTO.getPackStatus());
							}

							break;
						case Bpack:
							if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.DOWNLOAD_IN_PROGRESS)) {
								jobName = ACSConstants.BPACK_DOWNLOADER_NAME + packDetailsTO.getPackIdentifier();
								// + "V" + packDetailsTO.getVersionNumber();
								jobGroup = ACSConstants.BPACK_DOWNLOADER_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {
										downloadInprogressPackIds.add(packDetailsTO.getPackIdentifier());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.ACTIVATION_IN_PROGRESS)) {
								jobName = ACSConstants.BPACK_ACTIVATION_NAME + packDetailsTO.getPackIdentifier();
								jobGroup = ACSConstants.BPACK_ACTIVATION_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {
										activationInprogressPackIds.add(packDetailsTO.getPackIdentifier());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else {
								logger.info("invalid status = {}", packDetailsTO.getPackStatus());
							}

							break;
						case Qpack:
							if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.DOWNLOAD_IN_PROGRESS)) {
								jobName = ACSConstants.QPACK_DOWNLOADER_NAME + packDetailsTO.getPackIdentifier();
								// + "V"+ packDetailsTO.getVersionNumber();
								jobGroup = ACSConstants.QPACK_DOWNLOADER_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {
										downloadInprogressPackIds.add(packDetailsTO.getPackIdentifier());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else if (packDetailsTO.getPackStatus().equals(PacksStatusEnum.ACTIVATION_IN_PROGRESS)) {
								jobName = ACSConstants.QPACK_ACTIVATION_NAME + packDetailsTO.getPackIdentifier();
								jobGroup = ACSConstants.QPACK_ACTIVATION_GRP;

								try {
									// check whether there is a job exists with
									// specified credentials
									if (!isJobExists(jobName, jobGroup, scheduler)) {

										// if password exists then initiate
										// activation else mark it as password fetch
										// failed
										if (packDetailsTO.getPassword() != null) {
											updatePackStatusByPackIdentifier(packDetailsTO.getPackIdentifier(),
													PacksStatusEnum.PASSWORD_FETCHED, "password Fetched successfully");

											// validates the qpack status and check
											// whether activation can be initiated
											manualPasswordFetchService.validateQpackStatus(packDetailsTO
													.getPackIdentifier());
										} else {
											updatePackStatusByPackIdentifier(packDetailsTO.getPackIdentifier(),
													PacksStatusEnum.PASSWORD_FETCHING_FAILED, "password Fetch failed");
										}

										// activationInprogressPackIds.add(packDetailsTO.getPackId());
									}
								} catch (SchedulerException e) {
									logger.error(
											"SchedulerException while executing unScheduleDownloadAndActivationInProgressJobs...",
											e);
								}
							} else {
								logger.info("invalid status = {}", packDetailsTO.getPackStatus());
							}

							break;
						default:
							break;
					}
				}

				if (!downloadInprogressPackIds.isEmpty()) {
					logger.info("list of downloadInprogress packIds = {} whose jobs doesn't exist after restart",
							downloadInprogressPackIds);

					// update the status to download failed for those packs
					// whose status is download in progress
					updateDownloadInProgressPacksToDownloadFailed(downloadInprogressPackIds);
				}

				// currently handling for Bpack abd Apack not for Qpack because
				// Qpack activation is dependent on
				// password
				if (!activationInprogressPackIds.isEmpty()) {
					logger.info("list of activationInprogress packIds = {} whose jobs doesn't exist after restart",
							activationInprogressPackIds);

					// update the status to downloaded for those packs whose
					// status is activation in progress
					updateActivationInProgressPacksToDownloaded(activationInprogressPackIds);
				}
			} else {
				logger.info("No packs exist whose activation is in progress");
			}
		} catch (GenericDataModelException e) {
			logger.error("GenericDataModelException while executing contextInitialized...", e);
		} catch (SchedulerException e) {
			logger.error("SchedulerException while executing contextInitialized...", e);
		}
		return packDetailsTOs;
	}

	/**
	 * get the pack details for the specified batch id and pack status
	 * 
	 * @param batchCode
	 * @param packType
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public AcsPacks getPackDetailsByBatchCodeAndPackType(String batchCode, PackContent packType)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put(ACSConstants.PACK_TYPE, packType);

		AcsPacks packDetailsTO =
				(AcsPacks) session.getByQuery(ACSQueryConstants.QUERY_FETCH_PACK_DETAILS_BY_BATCH_CODE, params);
		return packDetailsTO;
	}

	/**
	 * Load {@link AcsPacks} by itd identifier.
	 * 
	 * @param packId
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsPacks loadPackDetails(String packId) throws GenericDataModelException {
		return (AcsPacks) session.get(packId, AcsPacks.class.getName());
	}

	public void saveOrUpdatePackDetails(List<AcsPacks> packs) throws GenericDataModelException {
		session.saveOrUpdate(packs);
	}

	/*
	 * public List<AcsPacks> getPacksByBatchCode(String batchCode) throws GenericDataModelException { Map<String,
	 * Object> params = new HashMap<String, Object>(); params.put(ACSConstants.BATCH_CODE, batchCode); List<AcsPacks>
	 * packs = session.getResultAsListByQuery(ACSQueryConstants.QUERY_FETCH_SCHEDULEDBATCHDATADO_PACKS, params, 0, 0);
	 * 
	 * return packs; }
	 */

	/**
	 * Combines the packCode and version
	 * 
	 * @param packCode
	 * @param version
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static String generatePackIdentifier(String packCode, int version) {
		return packCode + "-V" + version;
	}

	/**
	 * 
	 * @param packCode
	 * @param version
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static String generatePackIdentifier(String packCode, String version) {
		return packCode + "-V" + version;
	}
}
