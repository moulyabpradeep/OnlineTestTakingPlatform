/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Sep 2, 2013 11:41:31 AM - Siva_K
 * 
 */
package com.merittrac.apollo.acs.services;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.acs.audit.AdminAuditActionEnum;
import com.merittrac.apollo.acs.audit.AuditLoggerFactory;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSFilePaths;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.ReportStatusEnum;
import com.merittrac.apollo.acs.dataobject.PackExportInfoDO;
import com.merittrac.apollo.acs.dataobject.ReportExportInfoDO;
import com.merittrac.apollo.acs.entities.AcsReportDetails;
import com.merittrac.apollo.acs.quartz.jobs.ReportPackGenerator;
import com.merittrac.apollo.acs.utility.AutoConfigure;
import com.merittrac.apollo.acs.utility.DeleteFileOrDirectory;
import com.merittrac.apollo.acs.utility.NetworkUtility;
import com.merittrac.apollo.acs.utility.PropertiesUtil;
import com.merittrac.apollo.common.SecuredZipUtil;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.entities.acs.AttendanceMetaDataExportEntity;
import com.merittrac.apollo.common.entities.acs.EODReportMetaDataEntity;
import com.merittrac.apollo.common.entities.acs.ExportAttendancePackZipDetail;
import com.merittrac.apollo.common.entities.acs.ExportEventZipDetail;
import com.merittrac.apollo.common.entities.acs.ExportPackMetaDataEntity;
import com.merittrac.apollo.common.entities.acs.ExportRPackZipDetail;
import com.merittrac.apollo.common.entities.acs.ExportReportPacksMetaDataEntity;
import com.merittrac.apollo.common.entities.acs.MalpracticePackMetadata;
import com.merittrac.apollo.common.entities.acs.MalpracticeReportMetadata;
import com.merittrac.apollo.common.entities.acs.ReportTypeEnum;
import com.merittrac.apollo.common.entities.acs.RpackMetaDataExportEntity;
import com.merittrac.apollo.common.entities.deliverymanager.PackContent;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * api's related pack export
 * 
 * V_Praveen - MeritTrac Services Pvt. Ltd.
 * 
 * @since Apollo v2.0
 * @see
 */
public class ExportPackService extends BasicService {

	final static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static CDEAService cdeas = null;
	private static PackDetailsService packDetailsService = null;
	private static BatchService batchService = null;

	private static void initialized() {
		if (cdeas == null) {
			cdeas = new CDEAService();
		}
		if (packDetailsService == null) {
			packDetailsService = new PackDetailsService();
		}
		if (batchService == null)
			batchService = BatchService.getInstance();
	}

	public ExportPackService() {
		initialized();
	}

	/**
	 * This API will give pack export details info within the specified date range
	 * 
	 * @param eventCode
	 *            TODO
	 * @param packTypes
	 * @param packStatus
	 * 
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 */
	public List<PackExportInfoDO> getPackExportDetails(Calendar startDate, Calendar endDate, String packType,
			String eventCode) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		String query = "";
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		params.put("eventCode", eventCode);

		switch (packType) {
			case "Rpack":
				params.put("PackTypes", PackContent.Rpack);
				query = ACSQueryConstants.QUERY_FETCH_RPACKS_INFO_WITHIN_SPECIFIED_TIME;
				break;

			case "Attendancepack":
				params.put("PackTypes", PackContent.Attendancepack);
				query = ACSQueryConstants.QUERY_FETCH_PACKS_INFO_WITHIN_SPECIFIED_TIME;
				break;

			default:
				break;
		}

		List<PackExportInfoDO> packDetails = session.getResultAsListByQuery(query, params, 0);
		if (packDetails.equals(Collections.<Object> emptyList()))
			return null;
		return packDetails;
	}

	/**
	 * This API will give report export details info within the specified date range
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 * @throws GenericDataModelException
	 */

	public List<ReportExportInfoDO> getReportExportDetails(Calendar startDate, Calendar endDate, String reportType,
			String eventCode) throws GenericDataModelException {

		HashMap<String, Object> params = new HashMap<String, Object>();
		String query = "";
		params.put("startDate", startDate);
		params.put("endDate", endDate);
		params.put("eventCode", eventCode);
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
		params.put("reportType", ReportTypeEnum.REPORT_PACK);
		query = ACSQueryConstants.QUERY_FETCH_REPORTS_INFO_WITHIN_SPECIFIED_TIME;

		/*
		 * switch (reportType) { case "EODReport": params.put("reportType", ReportTypeEnum.EODReport); query =
		 * ACSQueryConstants.QUERY_FETCH_REPORTS_INFO_WITHIN_SPECIFIED_TIME; break; case "REPORT_PACK":
		 * params.put("reportType", ReportTypeEnum.REPORT_PACK); query =
		 * ACSQueryConstants.QUERY_FETCH_REPORTS_INFO_WITHIN_SPECIFIED_TIME; break; default: break; }
		 */

		List<ReportExportInfoDO> reportDetails =
				session.getResultAsListByQuery(query, params, 0, ReportExportInfoDO.class);
		if (reportDetails.equals(Collections.<Object> emptyList())
				&& ((Calendar.getInstance().getTime()).after(startDate.getTime()) && (Calendar.getInstance().getTime())
						.before(endDate.getTime()))) {
			List<String> batchCodesList = getMalpracticeBatchDetails(eventCode);
			List<String> subReportIdentifiersList = getMalpracticeReportIdentifierDetails(eventCode);
			logger.info("Bacth info ={} and reportIdentifier info ={} for for manual creation of report packs",
					batchCodesList, subReportIdentifiersList);
			if (batchCodesList != null && batchCodesList.size() > 0) {
				reportDetails.add(getDummyDetailsForCurrentDate(batchCodesList, subReportIdentifiersList));
				return reportDetails;
			}
		} else if ((Calendar.getInstance().getTime()).after(startDate.getTime())
				&& (Calendar.getInstance().getTime()).before(endDate.getTime())) {
			boolean draftRecordRequied = false;
			for (ReportExportInfoDO reportExportInfoDO : reportDetails) {

				if (sdf.format(reportExportInfoDO.getGenaratedTime().getTime()).equals(
						sdf.format(Calendar.getInstance().getTime()))) {
					break;
				} else {
					draftRecordRequied = true;
				}
			}
			List<String> batchCodesList = getMalpracticeBatchDetails(eventCode);
			List<String> subReportIdentifiersList = getMalpracticeReportIdentifierDetails(eventCode);
			logger.info(
					"Bacth info ={} and reportIdentifier info ={} for for manual creation of report packs and force generation required value={}",
					batchCodesList, subReportIdentifiersList, draftRecordRequied);
			if (draftRecordRequied && (batchCodesList != null && batchCodesList.size() > 0)) {

				reportDetails.add(getDummyDetailsForCurrentDate(batchCodesList, subReportIdentifiersList));
			}
		}
		ReportExportInfoDO tempReportExportInfoDO = null;
		for (ReportExportInfoDO reportExportInfoDO1 : reportDetails) {

			if (!reportExportInfoDO1.getReportIdentifier().equalsIgnoreCase(
					ACSConstants.CONST_MSG_FORCE_REPORT_PACK_NEED)) {
				tempReportExportInfoDO =
						setBacthInfoAndIdentfierNamesForReportPack(reportExportInfoDO1.getResponseMetaData());
				if (tempReportExportInfoDO != null) {

					reportExportInfoDO1.setBatchCodes(tempReportExportInfoDO.getBatchCodes());
					reportExportInfoDO1.setSubReportIdentifiersSet(tempReportExportInfoDO.getSubReportIdentifiersSet());

				}

			}

		}
		return reportDetails;
	}

	/**
	 * forms urls for exporting packs
	 * 
	 * @param packIdentifiers
	 * @param passwordToZIP
	 * @return
	 * @throws GenericDataModelException
	 * @throws IOException
	 * @throws ZipException
	 * @throws net.lingala.zip4j.exception.ZipException
	 * @throws ApolloSecurityException
	 * @throws net.lingala.zip4j.exception.ZipException
	 * 
	 * @since Apollo v2.0
	 */
	public String exportPackDetailsLink(String[] packIdentifiers) throws GenericDataModelException, IOException,
			ZipException, ApolloSecurityException, net.lingala.zip4j.exception.ZipException {
		if (packIdentifiers == null) {
			logger.info("Skipping Rpack export since no pack details found ");
			return null;
		}
		auditIt(packIdentifiers);
		DateFormat dFormat = new SimpleDateFormat(ACSConstants.DATE_FORMAT);
		String timeStampFormat = dFormat.format(new Date());

		Map<String, ExportEventZipDetail> mapEventToExportEventZipDetail = new HashMap<String, ExportEventZipDetail>();
		List<ExportEventZipDetail> listExportEventZipDetail1 = new ArrayList<ExportEventZipDetail>();

		String acsCode = "";
		// TODO: Move to common method
		File packExportDir =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.ACS_PACKS_TEMP_DIR);

		File desDir =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.ACS_TEMP_DIR
						+ timeStampFormat);

		for (int i = 0; i < packIdentifiers.length; i++) {
			String packCode = packIdentifiers[i];
			List<String> batchCodes = batchService.getBatchCodesByPackIdentifier(packCode);
			if (batchCodes == null || batchCodes.isEmpty()) {
				logger.info("Skipping pack export for batch={} since no batch details found for pack={} ", batchCodes,
						packIdentifiers);
				continue;
			}
			String batchCode = batchCodes.get(0);
			String eventCode = cdeas.getEventCodeByBatchCode(batchCode);
			acsCode = cdeas.getAcsCodeForEventCode(eventCode);
			if (eventCode == null) {
				logger.info("Skipping pack export for batch={} since no eventCode details found for pack ={}",
						batchCode, packIdentifiers);
				continue;
			}
			//
			/*
			 * String headerInfo = packDetailsService.getResponseMetadataByPackIdentifier(packCode);
			 * ExportRPackZipDetail exportRPackZipDetail = new ExportRPackZipDetail(); RpackMetaDataExportEntity
			 * rpackMetaDataExportEntity = new Gson().fromJson(headerInfo, RpackMetaDataExportEntity.class);
			 * exportRPackZipDetail.setBatchCode(rpackMetaDataExportEntity.getBatchCode());
			 * exportRPackZipDetail.setIsDeltaPackExists(rpackMetaDataExportEntity.getIsDeltaPackExists());
			 * exportRPackZipDetail.setPackCode(rpackMetaDataExportEntity.getPackCode());
			 * exportRPackZipDetail.setPackType(rpackMetaDataExportEntity.getPackType());
			 * exportRPackZipDetail.setVersionNo(rpackMetaDataExportEntity.getVersionNo());
			 * 
			 * List<ExportRPackZipDetail> listExportRPackZipDetail = new ArrayList<ExportRPackZipDetail>();
			 * listExportRPackZipDetail.add(exportRPackZipDetail);
			 * 
			 * if (mapEventToExportEventZipDetail.containsKey(eventCode)) { ExportEventZipDetail detail =
			 * mapEventToExportEventZipDetail.get(eventCode);
			 * detail.getExportRPackZipDetails().addAll(listExportRPackZipDetail);
			 * 
			 * } else { RpackMetaDataExportEntity rpackMetaDataExportEntityNew =
			 * cdeas.getEventDivCusDetailsbyBatchCode(eventCode); ExportEventZipDetail exportEventZipDetail2 = new
			 * ExportEventZipDetail();
			 * exportEventZipDetail2.setCustomerCode(rpackMetaDataExportEntityNew.getCustomerCode());
			 * exportEventZipDetail2.setDivisionCode(rpackMetaDataExportEntityNew.getDivisionCode());
			 * exportEventZipDetail2.setEventCode(eventCode);
			 * exportEventZipDetail2.setExportRPackZipDetails(listExportRPackZipDetail);
			 * 
			 * mapEventToExportEventZipDetail.put(eventCode, exportEventZipDetail2); }
			 */
			if (packCode.startsWith(ACSConstants.ACS_RPACK_TEMP_DIR)) {

				mapEventToExportEventZipDetail =
						prepareRpackExportDetails(packCode, eventCode, mapEventToExportEventZipDetail);
				boolean value =
						copyRpackFiles(batchCode, packCode, desDir.getAbsolutePath() + File.separator + eventCode);
				if (!value) {
					logger.info("Skipping Rpack export since R-Pack file ={} not found for batch={}", packCode,
							batchCode);
					// throw new ManualExportException(ACSExceptionConstants.MANUAL_EXPORT_NO_RPACKS,
					// "R-Packs are not available for selecetd Batch");
				}

			} else if (packCode.startsWith(ACSConstants.ACS_ATTENDANCE_TEMP_DIR)) {
				mapEventToExportEventZipDetail =
						prepareAttendanceExportDetails(packCode, eventCode, mapEventToExportEventZipDetail);
				boolean value =
						copyAttendanceFiles(batchCode, packCode, desDir.getAbsolutePath() + File.separator + eventCode);
				if (!value) {
					logger.info("Skipping attendence export since Pack file ={} not found for batch={}", packCode,
							batchCode);
					// throw new ManualExportException(ACSExceptionConstants.MANUAL_EXPORT_NO_RPACKS,
					// "R-Packs are not available for selecetd Batch");
				}

			}

		}
		File packExportDirWithAcsCode =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.ACS_PACKS_TEMP_DIR
						+ File.separator + acsCode + timeStampFormat);

		boolean isPasswordProtectiopnRequired = isExportToBePasswordProtected();
		for (String event : mapEventToExportEventZipDetail.keySet()) {
			String path = null;
			path =
					zipRpackFiles(desDir.getAbsolutePath() + File.separator + event, packExportDirWithAcsCode, event,
							isPasswordProtectiopnRequired);

			ExportEventZipDetail exportEventZipDetail3 = mapEventToExportEventZipDetail.get(event);
			exportEventZipDetail3.setZipName(path);
			listExportEventZipDetail1.add(exportEventZipDetail3);

		}

		writeMetadataFile(acsCode, packExportDirWithAcsCode, listExportEventZipDetail1);
		ZipUtility.archiveFiles(new String[] { packExportDirWithAcsCode.toString() },
				packExportDirWithAcsCode.getAbsolutePath());

		String fileExtensiion = acsCode + timeStampFormat + ACSConstants.FILE_ZIP_EXT;

		String urlPath = makeURL(acsCode, packExportDir, fileExtensiion);
		// String encodedUrl = URLEncoder.encode(urlPath, "UTF-8");
		DeleteFileOrDirectory.delete(desDir, true);
		DeleteFileOrDirectory.delete(packExportDirWithAcsCode, true);
		logger.debug("exportPackDetailsLink returning ", urlPath);
		return urlPath;
	}

	/**
	 * prepareRpackExportDetails API used to prepare Rpack Export details.
	 * 
	 * @param packCode
	 * @param eventCode
	 * @param mapEventToExportEventZipDetail
	 * @return
	 * @throws GenericDataModelException
	 */
	public Map<String, ExportEventZipDetail> prepareRpackExportDetails(String packCode, String eventCode,
			Map<String, ExportEventZipDetail> mapEventToExportEventZipDetail) throws GenericDataModelException {

		String headerInfo = packDetailsService.getResponseMetadataFromAcsPacksByPackIdentifier(packCode);
		ExportRPackZipDetail exportRPackZipDetail = new ExportRPackZipDetail();
		RpackMetaDataExportEntity rpackMetaDataExportEntity =
				new Gson().fromJson(headerInfo, RpackMetaDataExportEntity.class);
		exportRPackZipDetail.setBatchCode(rpackMetaDataExportEntity.getBatchCode());
		exportRPackZipDetail.setIsDeltaPackExists(rpackMetaDataExportEntity.getIsDeltaPackExists());
		exportRPackZipDetail.setPackCode(rpackMetaDataExportEntity.getPackCode());
		exportRPackZipDetail.setPackType(rpackMetaDataExportEntity.getPackType());
		exportRPackZipDetail.setVersionNo(rpackMetaDataExportEntity.getVersionNo());
		exportRPackZipDetail.setPackSubType(rpackMetaDataExportEntity.getPackSubType());

		List<ExportRPackZipDetail> listExportRPackZipDetail = new ArrayList<ExportRPackZipDetail>();
		listExportRPackZipDetail.add(exportRPackZipDetail);

		if (mapEventToExportEventZipDetail.containsKey(eventCode)) {
			ExportEventZipDetail detail = mapEventToExportEventZipDetail.get(eventCode);
			if (detail.getExportRPackZipDetails() != null) {
				detail.getExportRPackZipDetails().addAll(listExportRPackZipDetail);
			} else {
				detail.setExportRPackZipDetails(listExportRPackZipDetail);
			}

		} else {
			RpackMetaDataExportEntity rpackMetaDataExportEntityNew = cdeas.getEventDivCusDetailsbyBatchCode(eventCode);
			ExportEventZipDetail exportEventZipDetail2 = new ExportEventZipDetail();
			exportEventZipDetail2.setCustomerCode(rpackMetaDataExportEntityNew.getCustomerCode());
			exportEventZipDetail2.setDivisionCode(rpackMetaDataExportEntityNew.getDivisionCode());
			exportEventZipDetail2.setEventCode(eventCode);
			exportEventZipDetail2.setExportRPackZipDetails(listExportRPackZipDetail);

			mapEventToExportEventZipDetail.put(eventCode, exportEventZipDetail2);
		}
		return mapEventToExportEventZipDetail;

	}

	/**
	 * prepareAttendanceExportDetails API used to prepare Attendance Export Details of give pack code.
	 * 
	 * @param packCode
	 * @param eventCode
	 * @param mapEventToExportEventZipDetail
	 * @return
	 * @throws GenericDataModelException
	 */
	public Map<String, ExportEventZipDetail> prepareAttendanceExportDetails(String packCode, String eventCode,
			Map<String, ExportEventZipDetail> mapEventToExportEventZipDetail) throws GenericDataModelException {

		String headerInfo = packDetailsService.getResponseMetadataFromAcsPacksByPackIdentifier(packCode);
		ExportAttendancePackZipDetail exportAttendancePackZipDetail = new ExportAttendancePackZipDetail();
		AttendanceMetaDataExportEntity rpackMetaDataExportEntity =
				new Gson().fromJson(headerInfo, AttendanceMetaDataExportEntity.class);
		exportAttendancePackZipDetail.setBatchCode(rpackMetaDataExportEntity.getBatchCode());
		exportAttendancePackZipDetail.setPackCode(rpackMetaDataExportEntity.getPackCode());
		exportAttendancePackZipDetail.setPackType(rpackMetaDataExportEntity.getPackType());
		exportAttendancePackZipDetail.setVersionNo(rpackMetaDataExportEntity.getVersionNo());

		List<ExportAttendancePackZipDetail> listExportAttendancePackZipDetail =
				new ArrayList<ExportAttendancePackZipDetail>();
		listExportAttendancePackZipDetail.add(exportAttendancePackZipDetail);

		if (mapEventToExportEventZipDetail.containsKey(eventCode)) {
			ExportEventZipDetail detail = mapEventToExportEventZipDetail.get(eventCode);
			if (detail.getExportAttendancePackZipDetails() != null) {
				detail.getExportAttendancePackZipDetails().addAll(listExportAttendancePackZipDetail);
			} else {
				detail.setExportAttendancePackZipDetails(listExportAttendancePackZipDetail);
				// detail.setExportAttendancePackZipDetails(exportEventZipDetail12);
			}

		} else {
			AttendanceMetaDataExportEntity rpackMetaDataExportEntityNew =
					cdeas.getEventDivCusDetailsbyBatchCodeForAttendance(eventCode);
			ExportEventZipDetail exportEventZipDetail2 = new ExportEventZipDetail();
			exportEventZipDetail2.setCustomerCode(rpackMetaDataExportEntityNew.getCustomerCode());
			exportEventZipDetail2.setDivisionCode(rpackMetaDataExportEntityNew.getDivisionCode());
			exportEventZipDetail2.setEventCode(eventCode);
			exportEventZipDetail2.setExportAttendancePackZipDetails(listExportAttendancePackZipDetail);

			mapEventToExportEventZipDetail.put(eventCode, exportEventZipDetail2);
		}
		return mapEventToExportEventZipDetail;

	}

	/**
	 * @param acsCode
	 * 
	 * @param rpackExportDir
	 * @param mapEventToDownloadPath
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	protected void writeMetadataFile(String acsCode, File rpackExportDir,
			List<ExportEventZipDetail> listExportEventZipDetail) throws IOException {
		ExportPackMetaDataEntity exportPackMetaDataEntity = new ExportPackMetaDataEntity();
		exportPackMetaDataEntity.setAcsCode(acsCode);
		exportPackMetaDataEntity.setExportEventZipDetails(listExportEventZipDetail);
		// exportPackMetaDataEntity.setMapEventToZipFile(mapEventToDownloadPath);

		FileWriter fileMetaData =
				new FileWriter(rpackExportDir.getAbsolutePath() + File.separator
						+ ACSConstants.EXPORT_PACK_META_DATA_FILE_NAME);
		String exportPackMetaData = new Gson().toJson(exportPackMetaDataEntity);
		fileMetaData.write(exportPackMetaData);
		fileMetaData.close();
	}

	/**
	 * Copy the rpack files those are selected to be exported to the selected destination
	 * 
	 * @param batchCode
	 * @param rfile
	 * @param desDir
	 * @return
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 */
	private boolean copyRpackFiles(String batchCode, String rfile, String desDirTemp) throws IOException {
		File srcDir =
				new File(ACSFilePaths.getACSContentDirectory() + File.separator + ACSConstants.ACS_RPACK_TEMP_DIR
						+ File.separator + batchCode + File.separator + rfile + ACSConstants.FILE_ZIP_EXT);
		// File desDir = new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR +
		// File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator + batchCode ) ;
		File desDir = new File(desDirTemp);
		if (!srcDir.exists()) {
			return false;
		}
		if (!desDir.exists()) {
			desDir.mkdirs();
		}
		// File source = new File(srcDir.getAbsolutePath() + File.separator + candAppNum + ACSConstants.FILE_LOG_EXT);

		FileUtils.copyFileToDirectory(srcDir, desDir);
		return true;
	}

	/**
	 * will be used to zip all pack details
	 * 
	 * @param sourceDir
	 * @param finalDesDir
	 * @param isPasswordProtectionRequired
	 *            TODO
	 * @throws GenericDataModelException
	 * @throws net.lingala.zip4j.exception.ZipException
	 * @throws net.lingala.zip4j.exception.ZipException
	 * @throws ZipException
	 * @throws net.lingala.zip4j.exception.ZipException
	 * @throws IOException
	 * 
	 * @since Apollo v2.0
	 */
	private String zipRpackFiles(String sourceDir, File finalDesDir, String eventCode,
			boolean isPasswordProtectionRequired) throws GenericDataModelException, ZipException, IOException {
		String destination = finalDesDir.getAbsolutePath() + File.separator + eventCode;
		DateFormat dateFormat = new SimpleDateFormat(ACSConstants.DATE_FORMAT);
		String timeStamp = dateFormat.format(new Date());
		String finalPath = destination + timeStamp;
		File finalPathUrl = new File(finalPath);
		if (!finalPathUrl.exists()) {
			finalPathUrl.mkdirs();
		}

		if (isPasswordProtectionRequired) {
			SecuredZipUtil.archiveFilesWithMD5Password(new String[] { sourceDir }, finalPath, eventCode);

		} else {

			ZipUtility.archiveFiles(new String[] { sourceDir }, finalPath);
		}
		DeleteFileOrDirectory.delete(new File(finalPath), true);
		return finalPathUrl.getName().concat(ACSConstants.FILE_ZIP_EXT);
	}

	/**
	 * Reads the value from properties file to check whether exported packs are to be password protected or not
	 * 
	 * @return
	 * @since Apollo v2.0
	 */
	private boolean isExportToBePasswordProtected() {
		boolean isExportToBePasswordProtected = false;
		String propValue =
				PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
						ACSConstants.PASSSWORD_PROTECTED_FOR_EXPORTING);
		if (propValue == null || propValue.isEmpty()) {
			isExportToBePasswordProtected = ACSConstants.DEFAULT_PASSSWORD_PROTECTED_FOR_EXPORTING;
			logger.trace("As property isExportRequiresPasswordProtection is not set, default {} is used",
					ACSConstants.DEFAULT_PASSSWORD_PROTECTED_FOR_EXPORTING);
		} else {

			isExportToBePasswordProtected = Boolean.valueOf(propValue);
			logger.debug("isExportToBePasswordProtected = {}", isExportToBePasswordProtected);
		}
		return isExportToBePasswordProtected;
	}

	/**
	 * will create a export packs url to download
	 * 
	 * @param acsCode
	 * @param finalPath
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	private String makeURL(String acsCode, File finalPath, String fileExtensiion) {
		String rpackFilePath = null;
		File finalDesPath = new File(finalPath.getAbsolutePath() + File.separator + fileExtensiion);
		if (finalDesPath.exists()) {

			rpackFilePath = ACSConstants.ACS_PACKS_TEMP_DIR + File.separator + fileExtensiion;
			if (rpackFilePath != null) {
				// String preFixDownloadURL =
				// PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES,
				// ACSConstants.PROP_PREFIX_DOWNLOAD_URL);
				// if (preFixDownloadURL == null)
				// preFixDownloadURL = ACSConstants.DEFAULT_ACS_DOWNLOAD_URL;
				String moduleName =
						PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
				if (moduleName == null)
					moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;

				// rpackFilePath = preFixDownloadURL + moduleName + "/" + rpackFilePath;
				rpackFilePath =
						"/" + ACSConstants.MT_DOWNLOAD_SERVLET_MODULE_NAME + "?" + moduleName + "/" + rpackFilePath;
				// replace all backward slashes with forward
				rpackFilePath = rpackFilePath.replace("\\", "/");
			}
		}
		return rpackFilePath;
	}

	/**
	 * 
	 * 
	 * @param packIdentifiers
	 * @since Apollo v2.0
	 * @see
	 */
	private void auditIt(String[] packIdentifiers) {
		// added for audit trail
		String isAuditEnable = AutoConfigure.getAuditConfigureValue();
		if (isAuditEnable != null && isAuditEnable.equals("true")) {
			Object[] paramArray = { packIdentifiers };
			HashMap<String, String> logbackParams = new HashMap<String, String>();
			logbackParams.put(ACSConstants.AUDIT_ADMIN_ACTIONTYPE, AdminAuditActionEnum.EXPORT_PACKS.toString());
			logbackParams.put(ACSConstants.AUDIT_LOGKEY_HOSTADDRESS, NetworkUtility.getIp());
			AuditLoggerFactory.getAuditLoggerByType(ACSConstants.AUDIT_ADMIN_TYPE).logAuditData(logbackParams,
					ACSConstants.AUDIT_EXPORT_PACKS_MSG, paramArray);
		}
		// end for audit trail

	}

	/**
	 * Copy the attendance pack files those are selected to be exported to the selected destination
	 * 
	 * @param batchCode
	 * @param attendancefile
	 * @param desDirTemp
	 * @return
	 * @throws IOException
	 */
	private boolean copyAttendanceFiles(String batchCode, String attendancefile, String desDirTemp) throws IOException {
		File srcDir =
				new File(ACSFilePaths.getACSContentDirectory() + File.separator + ACSConstants.ACS_ATTENDANCE_TEMP_DIR
						+ File.separator + batchCode + File.separator + attendancefile + ACSConstants.FILE_ZIP_EXT);
		// File desDir = new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR +
		// File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator + batchCode ) ;
		File desDir = new File(desDirTemp);
		if (!srcDir.exists()) {
			return false;
		}
		if (!desDir.exists()) {
			desDir.mkdirs();
		}
		// File source = new File(srcDir.getAbsolutePath() + File.separator + candAppNum + ACSConstants.FILE_LOG_EXT);

		FileUtils.copyFileToDirectory(srcDir, desDir);
		return true;
	}

	/**
	 * forms urls for exporting report packs
	 * 
	 * @param reportPackIdentifiers
	 * @param eventCode
	 *            TODO
	 * @return
	 * @throws GenericDataModelException
	 * @throws IOException
	 * @throws ZipException
	 * @throws ApolloSecurityException
	 * @throws net.lingala.zip4j.exception.ZipException
	 */
	public String exportReportPackDetailsLink(String[] reportPackIdentifiers, String eventCode)
			throws GenericDataModelException, IOException, ZipException, ApolloSecurityException,
			net.lingala.zip4j.exception.ZipException {
		if (reportPackIdentifiers == null) {
			logger.info("Skipping reportPack export since no reportPack details found ");
			return null;
		}
		// auditing here
		auditIt(reportPackIdentifiers);
		// ends here auditing
		String acsCode = cdeas.getAcsCodeForEventCode(eventCode);
		logger.info("ACS server code details={}", acsCode);
		ExportReportPacksMetaDataEntity exportReportPacksMetaDataEntity = new ExportReportPacksMetaDataEntity();
		Map<String, String> reportPackDetails = new HashMap<String, String>();
		DateFormat dFormat = new SimpleDateFormat(ACSConstants.DATE_FORMAT);
		String timeStampFormat = dFormat.format(new Date());
		File reportPackExportDir =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.REPORT_PACK_DIR);

		File reportPackExportDirWithAcsCode =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.REPORT_PACK_DIR
						+ File.separator + acsCode + timeStampFormat);
		File tempDesDir =
				new File(ACSFilePaths.getACSDownloadDirectory() + File.separator + ACSConstants.ACS_TEMP_DIR
						+ timeStampFormat);

		for (int i = 0; i < reportPackIdentifiers.length; i++) {
			String reportPackCode = reportPackIdentifiers[i];
			if (reportPackCode.equalsIgnoreCase(ACSConstants.CONST_MSG_FORCE_REPORT_PACK_NEED)) {
				ReportPackGenerator reportPackGenerator = new ReportPackGenerator();
				reportPackCode = reportPackGenerator.generateReportPack(true, eventCode);
				if (reportPackIdentifiers.length == 1 && reportPackCode == null) {
					logger.info(
							"Skipping Report Pack export for file identifier ={} since report Pack not found/already generated",
							reportPackCode);
					return null;

				}

			}
			boolean copiedStatus = copyReportPackDetails(reportPackCode, reportPackExportDirWithAcsCode);

			if (!copiedStatus) {
				logger.info("Skipping Report Pack export for file identifier ={} since report Pack not found",
						reportPackCode);
			} else {
				logger.info("Successfully copied Report Pack export for file identifier ={}", reportPackCode);
				reportPackDetails.put(reportPackCode, reportPackCode + ACSConstants.FILE_ZIP_EXT);
			}

			// ReportDetailsTO reportDetailsTO = packDetailsService.getReportDetailByReportIdentifier(reportPackCode);
		}
		// starts here for preparing meta data
		exportReportPacksMetaDataEntity.setAcsCode(acsCode);
		exportReportPacksMetaDataEntity.setReportPacksInfo(reportPackDetails);
		writeMetadataForReportPack(exportReportPacksMetaDataEntity, reportPackExportDirWithAcsCode);
		// ends here for preparing meta data

		// starts making final zip with all selected Report packs
		if (!reportPackExportDir.exists()) {
			reportPackExportDir.mkdirs();
		}
		ZipUtility.archiveFiles(new String[] { reportPackExportDirWithAcsCode.toString() },
				reportPackExportDirWithAcsCode.getAbsolutePath());
		// end here for making final zip with all selected Report packs

		String fileExtensiion = acsCode + timeStampFormat + ACSConstants.FILE_ZIP_EXT;

		// starts here prepare url to download final zip
		String urlPath = makeURLForReportPacks(acsCode, reportPackExportDir, fileExtensiion);
		// end here for prepare url to download final zip

		// String encodedUrl = URLEncoder.encode(urlPath, "UTF-8");

		// starts here for deleting temp dir
		DeleteFileOrDirectory.delete(tempDesDir, true);
		DeleteFileOrDirectory.delete(reportPackExportDirWithAcsCode, true);
		// end here deleting temp dir

		logger.debug("exportreportPackDetailsLink returning ", urlPath);
		return urlPath;

	}

	/**
	 * copyReportPackDetails API used to copy report pack details from source to destination
	 * 
	 * @param reportPackfile
	 * @param desDirTemp
	 * @return
	 * @throws IOException
	 */
	private boolean copyReportPackDetails(String reportPackfile, File desDirTemp) throws IOException {
		File sourceReportPackDir =
				new File(ACSFilePaths.getAcsReportPackDir() + File.separator + reportPackfile
						+ ACSConstants.FILE_ZIP_EXT);
		// File desDir = new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR +
		// File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator + batchCode ) ;
		// File desDir = new File(desDirTemp);
		if (!sourceReportPackDir.exists()) {
			return false;
		}
		if (!desDirTemp.exists()) {
			desDirTemp.mkdirs();
		}
		// File source = new File(srcDir.getAbsolutePath() + File.separator + candAppNum + ACSConstants.FILE_LOG_EXT);

		FileUtils.copyFileToDirectory(sourceReportPackDir, desDirTemp);
		return true;

	}

	/**
	 * will create a export report packs url to download
	 * 
	 * @param acsCode
	 * @param finalPath
	 * @return
	 * 
	 * @since Apollo v2.0
	 */
	private String makeURLForReportPacks(String acsCode, File finalPath, String fileExtensiion) {
		String rpackFilePath = null;
		File finalDesPath = new File(finalPath.getAbsolutePath() + File.separator + fileExtensiion);
		if (finalDesPath.exists()) {

			rpackFilePath = ACSConstants.REPORT_PACK_DIR + File.separator + fileExtensiion;
			if (rpackFilePath != null) {

				String moduleName =
						PropertiesUtil.getProperty(ACSConstants.DEFAULT_ACS_PROPERTIES, ACSConstants.PROP_MODULE_NAME);
				if (moduleName == null)
					moduleName = ACSConstants.DEFAULT_ACS_MODULE_NAME;

				rpackFilePath =
						"/" + ACSConstants.MT_DOWNLOAD_SERVLET_MODULE_NAME + "?" + moduleName + "/" + rpackFilePath;
				rpackFilePath = rpackFilePath.replace("\\", "/");
			}
		}
		return rpackFilePath;
	}

	/**
	 * writeMetadataForReportPack API used to prepare report pack meta data.
	 * 
	 * @param exportReportPacksMetaDataEntity
	 * @param reportPackExportDir
	 * @throws IOException
	 */
	private void writeMetadataForReportPack(ExportReportPacksMetaDataEntity exportReportPacksMetaDataEntity,
			File reportPackExportDir) throws IOException {

		FileWriter fileMetaData =
				new FileWriter(reportPackExportDir.getAbsolutePath() + File.separator
						+ ACSConstants.EXPORT_REPORT_PACK_META_DATA_FILE_NAME);
		String exportPackMetaData = new Gson().toJson(exportReportPacksMetaDataEntity);
		logger.info("Meta data info about export report pack={}", exportPackMetaData);
		fileMetaData.write(exportPackMetaData);
		fileMetaData.close();
	}

	/**
	 * getDummyDetailsForCurrentDate API used to display the report pack details need to generate and export
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */

	public ReportExportInfoDO getDummyDetailsForCurrentDate(List<String> batchCodesList,
			List<String> subReportIdentifiersList) throws GenericDataModelException {
		logger.info(
				"Batch details ={} and report Identifiers ={} for to display the report pack details need to generate and export",
				batchCodesList, subReportIdentifiersList);
		ReportExportInfoDO exportInfoDO = new ReportExportInfoDO();
		exportInfoDO.setGenaratedTime(Calendar.getInstance());
		exportInfoDO.setReportStatus(ReportStatusEnum.YET_TO_GENERATE);
		exportInfoDO.setReportType(ReportTypeEnum.REPORT_PACK);
		exportInfoDO.setReportIdentifier(ACSConstants.CONST_MSG_FORCE_REPORT_PACK_NEED);
		Set<String> batchCodeSet = new HashSet<String>(batchCodesList);
		Set<String> subReportIdentifierSet = new HashSet<String>(subReportIdentifiersList);
		exportInfoDO.setBatchCodes(batchCodeSet.toString());
		exportInfoDO.setSubReportIdentifiersSet(subReportIdentifierSet.toString());
		return exportInfoDO;

	}

	/**
	 * getMalpracticeBatchDetails API used to get the batch details list for malpractise report
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<String> getMalpracticeBatchDetails(String eventCode) throws GenericDataModelException {
		List<String> batchCodesList = new ArrayList<String>();
		// 1. Get the Latest generated Malpractice Pack.
		AcsReportDetails malpracticePack =
				packDetailsService.getLatestReportDetails(ReportTypeEnum.MALPRACTICE_PACK, eventCode);
		List<AcsReportDetails> malpracticeReports = new ArrayList<>();

		// 2. Get the malpractice reports generated after the latest Malpractice Pack.
		// (if no Pack, then all reports should be considered)
		if (malpracticePack == null || malpracticePack.getGeneratedEndTime() == null) {
			malpracticeReports =
					packDetailsService.getReportsTillCurrentDate(ReportTypeEnum.MALPRACTICE_REPORT, eventCode);
		} else {
			malpracticeReports =
					packDetailsService.getLatestReportsSincePerticularTime(ReportTypeEnum.MALPRACTICE_REPORT,
							malpracticePack.getGeneratedEndTime(), eventCode);
		}

		// check if any reports exists which is not packed.
		if (malpracticeReports == null) {
			logger.debug("No Reports exists");
			return null;
		}
		for (AcsReportDetails reportDetailsTO : malpracticeReports) {
			// TODO:Lazy loading
			batchCodesList.add(reportDetailsTO.getAcsbatchdetails().getBatchCode());

		}
		return batchCodesList;

	}

	/**
	 * getResponseMetaDataofReportdetails API used to get report meta data for given report identifier
	 * 
	 * @param reportIdentifier
	 * @return
	 * @throws GenericDataModelException
	 */
	public String getResponseMetaDataofReportdetails(String reportIdentifier) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("reportIdentifier", reportIdentifier);
		String query = ACSQueryConstants.QUERY_FETCH_META_DATA_FROM_REPORT_DETAILS_BY_REPORTIDENTIFIER;
		String reportDetails = (String) session.getByQuery(query, params);
		logger.info("report meta ={} data for batchid is={}", reportDetails, reportIdentifier);
		if (reportDetails == null)
			return null;
		return reportDetails;
	}

	/**
	 * setBacthInfoAndIdentfierNamesForReportPack API used to set all the batch details and reportIdentfiers details for
	 * a report pack meta data
	 * 
	 * @param responseMetajson
	 * @return
	 * @throws GenericDataModelException
	 */
	public ReportExportInfoDO setBacthInfoAndIdentfierNamesForReportPack(String responseMetajson)
			throws GenericDataModelException {

		logger.info("recieved meta data to set batch Info is={}", responseMetajson);
		Type mapType1 = new TypeToken<Map<String, Object>>() {
		}.getType();
		if (responseMetajson != null) {

			Map<String, Object> headers = new Gson().fromJson(responseMetajson, mapType1);
			Set<String> batchCodeSet = null;
			Set<String> subReportIdentfiers = null;
			Map<String, String> dataAboutReports = (Map) headers.get(ACSConstants.CONST_MAP_REPORT_TYPE_REPORTS);
			if (dataAboutReports.get(ACSConstants.CONST_EOD_REPORT) != null) {
				String eod_report_Identifier = dataAboutReports.get(ACSConstants.CONST_EOD_REPORT).replace(".zip", "");
				String eod_metaData = getResponseMetaDataofReportdetails(eod_report_Identifier);
				logger.info("eod meta data={} for identifier={}", eod_metaData, eod_report_Identifier);
				Type type = new TypeToken<EODReportMetaDataEntity>() {
				}.getType();
				EODReportMetaDataEntity eODReportMetaDataEntity = new Gson().fromJson(eod_metaData, type);
				List<String> bactCodes1 = eODReportMetaDataEntity.getBatchCodes();
				batchCodeSet = new HashSet<String>(bactCodes1);
				subReportIdentfiers = new HashSet<String>();
				subReportIdentfiers.add(eod_report_Identifier);

			}
			if (dataAboutReports.get(ACSConstants.CONST_MAL_PRACTISE_PACK) != null) {
				if (batchCodeSet == null) {
					batchCodeSet = new HashSet<String>();

				}
				if (subReportIdentfiers == null) {
					subReportIdentfiers = new HashSet<String>();

				}
				String malpractise_report_Identifier =
						dataAboutReports.get(ACSConstants.CONST_MAL_PRACTISE_PACK).replace(ACSConstants.FILE_ZIP_EXT,
								"");
				String malpractise_metaData = getResponseMetaDataofReportdetails(malpractise_report_Identifier);
				logger.info("malpractise pack meta data={} for identifier={}", malpractise_metaData,
						malpractise_report_Identifier);
				Type type = new TypeToken<MalpracticePackMetadata>() {
				}.getType();
				MalpracticePackMetadata malpracticePackMetadata = new Gson().fromJson(malpractise_metaData, type);
				Map<String, MalpracticeReportMetadata> map1 = malpracticePackMetadata.getMapOfIdentifierToMetadata();
				for (Map.Entry<String, MalpracticeReportMetadata> entry : map1.entrySet()) {
					MalpracticeReportMetadata malpracticeReportMetadata = entry.getValue();
					batchCodeSet.add(malpracticeReportMetadata.getBatchCode());
					subReportIdentfiers.add(malpracticeReportMetadata.getMalpracticeReportIdentifier());

				}

			}
			logger.info("batch Info for recieved meta data={}", batchCodeSet);
			logger.info("Identifiers Info for recieved meta data={}", subReportIdentfiers);
			ReportExportInfoDO reportExportInfoDO = new ReportExportInfoDO();
			reportExportInfoDO.setBatchCodes(batchCodeSet.toString());
			reportExportInfoDO.setSubReportIdentifiersSet(subReportIdentfiers.toString());
			return reportExportInfoDO;
		} else
			return null;
	}

	/**
	 * getMalpracticeReportIdentifierDetails API used to get the report identifiers list for malpractise report
	 * 
	 * @param eventCode
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<String> getMalpracticeReportIdentifierDetails(String eventCode) throws GenericDataModelException {
		List<String> reportIdentifierList = new ArrayList<String>();
		// 1. Get the Latest generated Malpractice Pack.
		AcsReportDetails malpracticePack =
				packDetailsService.getLatestReportDetails(ReportTypeEnum.MALPRACTICE_PACK, eventCode);
		List<AcsReportDetails> malpracticeReports = new ArrayList<>();

		// 2. Get the malpractice reports generated after the latest Malpractice Pack.
		// (if no Pack, then all reports should be considered)
		if (malpracticePack == null || malpracticePack.getGeneratedEndTime() == null) {
			malpracticeReports =
					packDetailsService.getReportsTillCurrentDate(ReportTypeEnum.MALPRACTICE_REPORT, eventCode);
		} else {
			malpracticeReports =
					packDetailsService.getLatestReportsSincePerticularTime(ReportTypeEnum.MALPRACTICE_REPORT,
							malpracticePack.getGeneratedEndTime(), eventCode);
		}

		// check if any reports exists which is not packed.
		if (malpracticeReports == null) {
			logger.debug("No Reports exists");
			return null;
		}
		for (AcsReportDetails reportDetailsTO : malpracticeReports) {
			reportIdentifierList.add(reportDetailsTO.getReportIdentifier());

		}
		logger.debug("Report Identifier list for getMalpracticeReportIdentifierDetails API is{}", reportIdentifierList);
		return reportIdentifierList;

	}

}
