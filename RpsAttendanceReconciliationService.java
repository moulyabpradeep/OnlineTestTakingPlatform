package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
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

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateMIFDetails;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsCity;
import com.merittrac.apollo.data.entity.RpsMasterAssociation;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsVenue;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsCandidateMIFDetailsService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsCandidateService;
import com.merittrac.apollo.data.service.RpsCityService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsVenueService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.jms.receiver.AbstractReceiver;
import com.merittrac.apollo.rps.ui.entity.AttendanceReconcileDataEntity;
import com.merittrac.apollo.rps.ui.entity.CandidateInfoViewScreen;
import com.merittrac.apollo.rps.ui.entity.CityVenueAcsEntity;
import com.merittrac.apollo.rps.ui.entity.ReconciliationGridEntity;
import com.merittrac.apollo.rps.ui.entity.ScoreScreenDataEntity;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;

public class RpsAttendanceReconciliationService extends AbstractReceiver {

	private static Logger logger = LoggerFactory.getLogger(RpsAttendanceReconciliationService.class);

	@Autowired
	RpsAcsServerServices rpsAcsServerServices;

	@Autowired
	RpsCandidateMIFDetailsService rpsCandidateMIFDetailsService;

	@Autowired
	RpsCityService rpsCityService;

	@Autowired
	RpsVenueService rpsVenueService;

	@Autowired
	Gson gson;

	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

	@Autowired
	RpsBatchService rpsBatchService;

	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsCandidateService rpsCandidateService;

	@Autowired
	RpsCandidateResponseService rpsCandidateResponseService;

	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;

	public RpsAttendanceReconciliationService() throws RpsException {
		super();
	}

	public String getCityVenuAcsList(String eventCode, String cityCode, String venueCode, String startDate,
			String endDate) {
		logger.info("----IN--- getCityVenuAcsList");
		CityVenueAcsEntity cityVenueAcsEntity = new CityVenueAcsEntity();
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + ",  startDate :" + startDate
					+ " ,endDate : " + endDate);
			return gson.toJson(cityVenueAcsEntity);
		}
		List<RpsAcsServer> rpsAcsServersList = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		try {
			Calendar rangeStartDate = Calendar.getInstance();
			Calendar rangeEndDate = Calendar.getInstance();
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
			// fetch list of acsServers for eventCode
			rpsAcsServersList =
					rpsBatchAcsAssociationService.getAllAcsServersByEventInDateRange(eventCode, rangeStartDate,
							rangeEndDate);
		} catch (ParseException e) {
			logger.error("Error in parsing date range selected--", e);
			return gson.toJson(cityVenueAcsEntity);
		}

		if (rpsAcsServersList == null || rpsAcsServersList.isEmpty()) {

			logger.warn("List of acsServers is empty for-- eventCode: " + eventCode);
			return gson.toJson(cityVenueAcsEntity);
		}

		if (cityCode == null || cityCode.isEmpty()) {
			// fetch list of city codes if it is not provided UI
			cityVenueAcsEntity = this.getListOfCityCodesByAcsList(rpsAcsServersList);
		} else if (venueCode == null || venueCode.isEmpty()) {
			// fetch list of venue codes for the given cityCode
			cityVenueAcsEntity = this.getListOfVenueCodesByAcsListCityCode(rpsAcsServersList, cityCode);
		} else {
			// fetch list of acs codes for the given cityCode, venueCode
			cityVenueAcsEntity = this.getListOfAcsCodesByVenue(rpsAcsServersList, venueCode);
		}

		logger.info("----OUT--- getCityVenuAcsList");
		return gson.toJson(cityVenueAcsEntity);
	}

	private CityVenueAcsEntity getListOfAcsCodesByVenue(List<RpsAcsServer> rpsAcsServersList, String venueCode) {

		CityVenueAcsEntity cityVenueAcsEntity = new CityVenueAcsEntity();
		RpsVenue rpsVenue = rpsVenueService.findByVenueCode(venueCode);
		if (rpsVenue == null) {
			logger.warn("Venue doesn't exist in database--" + venueCode);
			return cityVenueAcsEntity;
		}

		Set<String> acsCodes = new HashSet<>();
		for (RpsAcsServer rpsAcsServer : rpsAcsServersList) {
			if ((rpsAcsServer.getRpsVenue().getVenueId().intValue() == rpsVenue.getVenueId().intValue()))
				acsCodes.add(rpsAcsServer.getAcsServerId());
		}

		cityVenueAcsEntity.setAcsCodes(acsCodes);
		return cityVenueAcsEntity;
	}

	private CityVenueAcsEntity getListOfVenueCodesByAcsListCityCode(List<RpsAcsServer> rpsAcsServersList,
			String cityCode) {

		CityVenueAcsEntity cityVenueAcsEntity = new CityVenueAcsEntity();
		List<RpsVenue> rpsVenuesList =
				rpsAcsServerServices.getRpsVenueListByAcsListAndCityCode(rpsAcsServersList, cityCode);

		if (rpsVenuesList == null || rpsVenuesList.isEmpty()) {
			logger.warn("Venue List is empty for acs servers and city code : " + cityCode);
			return cityVenueAcsEntity;
		}

		Set<String> venueCodesList = new HashSet<>();
		for (RpsVenue rpsVenue : rpsVenuesList) {
			venueCodesList.add(rpsVenue.getVenueCode());
		}

		cityVenueAcsEntity.setVenueCodes(venueCodesList);
		return cityVenueAcsEntity;
	}

	private CityVenueAcsEntity getListOfCityCodesByAcsList(List<RpsAcsServer> rpsAcsServersList) {

		CityVenueAcsEntity cityVenueAcsEntity = new CityVenueAcsEntity();
		List<RpsVenue> rpsVenuesList = rpsAcsServerServices.getRpsVenueListByAcsList(rpsAcsServersList);

		if (rpsVenuesList == null || rpsVenuesList.isEmpty()) {
			logger.warn("Venue List is empty for acs servers");
			return cityVenueAcsEntity;
		}

		List<RpsCity> rpsCitiesList = rpsVenueService.getCityListByVenueList(rpsVenuesList);
		if (rpsCitiesList == null || rpsCitiesList.isEmpty()) {
			logger.warn("City List is empty for venues");
			return cityVenueAcsEntity;
		}

		Set<String> cityCodesList = new HashSet<>();
		for (RpsCity city : rpsCitiesList) {
			cityCodesList.add(city.getCityCode());
		}

		cityVenueAcsEntity.setCityCodes(cityCodesList);

		return cityVenueAcsEntity;
	}

	public List<AttendanceReconcileDataEntity> getReconciliationGridData(String acsCode, String batchDate) {

		logger.info("----IN--- getReconciliationGridData()");
		List<AttendanceReconcileDataEntity> attendanceReconcileDataEntitiesList = new ArrayList<>();
		if (acsCode == null || acsCode.isEmpty() || batchDate == null || batchDate.isEmpty()) {
			logger.warn("missing mandatory arguments-- acsCode, startDate or endDate");
			return attendanceReconcileDataEntitiesList;
		}

		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = this.getBatchListByAcsCode(acsCode, batchDate);
		if (rpsBatchAcsAssociationsList == null || rpsBatchAcsAssociationsList.isEmpty()) {
			logger.warn("There are no batche association for-- acsCode-" + acsCode + ", batchDate-" + batchDate);
			return attendanceReconcileDataEntitiesList;
		}

		attendanceReconcileDataEntitiesList =
				this.getAttendanceReconcileEntityByBatchAssonList(rpsBatchAcsAssociationsList);
		return attendanceReconcileDataEntitiesList;
	}

	private List<AttendanceReconcileDataEntity> getAttendanceReconcileEntityByBatchAssonList(
			List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList) {

		Map<Integer, String> qpIDToQpaperMap = new HashMap<Integer, String>();
		List<AttendanceReconcileDataEntity> attendanceReconcileDataEntityList = new ArrayList<>();
		for (RpsBatchAcsAssociation rpsBatchAcsAssociation : rpsBatchAcsAssociationsList) {
			// create AttendanceReconcileDataEntity for every batch code
			AttendanceReconcileDataEntity attendanceReconcileDataEntity = new AttendanceReconcileDataEntity();
			RpsBatch rpsBatch = rpsBatchService.find(rpsBatchAcsAssociation.getRpsBatch().getBid());
			attendanceReconcileDataEntity.setBatchCode(rpsBatch.getBatchCode());
			attendanceReconcileDataEntity.setBatchStartTime(rpsBatch.getBatchStartTime().getTime());
			List<RpsMasterAssociation> rpsMasterAssociationsList =
					rpsMasterAssociationService.getByrpsBatchAcsAssociation(rpsBatchAcsAssociation);
			List<ReconciliationGridEntity> reconciliationGridEntitiesList = new ArrayList<>();
			if (rpsMasterAssociationsList != null && !rpsBatchAcsAssociationsList.isEmpty()) {
				for (RpsMasterAssociation rpsMasterAssociation : rpsMasterAssociationsList) {
					// create ReconciliationGridEntity for every candidate in master association table
					ReconciliationGridEntity reconciliationGridEntity = new ReconciliationGridEntity();
					reconciliationGridEntity.setCandidateUniqueID(rpsMasterAssociation.getUniqueCandidateId());
					reconciliationGridEntity.setAssessmentID(rpsMasterAssociation.getRpsAssessment()
							.getAssessmentCode());
					RpsCandidate rpsCandidate =
							rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
					RpsCandidateResponse rpsCandidateResponse =
							rpsCandidateResponseService.findByUniqueCandidateId(rpsMasterAssociation
									.getUniqueCandidateId());
					this.setCandidatePresenceStatus(rpsCandidate, rpsCandidateResponse, rpsMasterAssociation,
							reconciliationGridEntity, qpIDToQpaperMap);
					reconciliationGridEntitiesList.add(reconciliationGridEntity);
				}
			}
			attendanceReconcileDataEntity.setReconciliationGridEntities(reconciliationGridEntitiesList);
			attendanceReconcileDataEntityList.add(attendanceReconcileDataEntity);
		}
		Collections.sort(attendanceReconcileDataEntityList);
		return attendanceReconcileDataEntityList;
	}

	private void setCandidatePresenceStatus(RpsCandidate rpsCandidate, RpsCandidateResponse rpsCandidateResponse,
			RpsMasterAssociation rpsMasterAssociation, ReconciliationGridEntity reconciliationGridEntity,
			Map<Integer, String> qpIDToQpaperMap) {

		reconciliationGridEntity.setCandidateScheduledID(rpsCandidate.getCandidateId1());
		reconciliationGridEntity.setLoginTime(this.convertTimeToString(rpsMasterAssociation.getLoginTime()));
		reconciliationGridEntity.setLogoutTime(this.convertTimeToString(rpsMasterAssociation.getTestEndTime()));
		if (rpsMasterAssociation.isPresent()) {
			reconciliationGridEntity.setCandidatePresentID(rpsCandidate.getCandidateId1());
		} else
			reconciliationGridEntity.setCandidatePresentID(RpsConstants.NA);

		if (rpsCandidateResponse == null) {
			reconciliationGridEntity.setCandidateAssessedID(RpsConstants.NA);
		} else {
			reconciliationGridEntity.setCandidateAssessedID(rpsCandidate.getCandidateId1());
			// check for question paper ID
			Integer qpId = rpsCandidateResponse.getRpsQuestionPaper().getQpId();
			if (qpIDToQpaperMap.get(qpId) == null) {
				// fetch qpid from database and add to map
				RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
				qpIDToQpaperMap.put(qpId, rpsQuestionPaper.getQpCode());
			}
			reconciliationGridEntity.setAssessmentSetID(qpIDToQpaperMap.get(qpId));
			reconciliationGridEntity.setQpID(qpId);
		}
		// if candidate has started the exam, then set candidate assessed id
		if (rpsMasterAssociation.getTestStartTime() != null && rpsCandidateResponse != null)
			reconciliationGridEntity.setCandidateAssessedID(rpsCandidate.getCandidateId1());
		else
			reconciliationGridEntity.setCandidateAssessedID(RpsConstants.NA);

		// calculate candidate status
		if (!reconciliationGridEntity.getCandidateScheduledID().equalsIgnoreCase(RpsConstants.NA)) {
			// if presenece and assessed are same
			if (reconciliationGridEntity.getCandidatePresentID().equalsIgnoreCase(
					reconciliationGridEntity.getCandidateAssessedID()))
				reconciliationGridEntity.setStatus(RpsConstants.NORMAL);
			else
				reconciliationGridEntity.setStatus(RpsConstants.CONFLICT);

		} else {
			// to do later
		}

	}

	private String convertTimeToString(Date loginTime) {

		String time = "";
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		if (loginTime != null)
			time = sdf.format(loginTime);
		return time;
	}

	private List<RpsBatchAcsAssociation> getBatchListByAcsCode(String acsCode, String batchDate) {
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		List<RpsBatchAcsAssociation> rpsBatchAcsAssociationsList = null;
		try {
			Calendar rangeStartDate = Calendar.getInstance();
			rangeStartDate.setTime(sdf.parse(batchDate));
			rpsBatchAcsAssociationsList =
					rpsBatchAcsAssociationService.getAllBatchAssociationsByAcsCodeAndDateRange(acsCode, rangeStartDate);

		} catch (ParseException e) {
			logger.error("Error in parsing date range selected--", e);
		}

		return rpsBatchAcsAssociationsList;
	}

	public CandidateInfoViewScreen getViewScreenDataForCandidate(Integer masterAssociationID, String imageFolder) {

		logger.info("---IN-- getViewScreenDataForCandidate()");
		CandidateInfoViewScreen candidateInfoViewScreen = new CandidateInfoViewScreen();

		RpsMasterAssociation rpsMasterAssociation =
				rpsMasterAssociationService.findByUniqueCandidateId(masterAssociationID);
		if (rpsMasterAssociation.getLoginID() != null) {
			candidateInfoViewScreen.setLoginID(rpsMasterAssociation.getLoginID());
			// to do set value for logout time
		}

		RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());
		candidateInfoViewScreen.setCandidateID(rpsCandidate.getCandidateId1());
		candidateInfoViewScreen
				.setCandidateName(this.getCandidateFullName(rpsCandidate, rpsMasterAssociation.getUniqueCandidateId()));

		String imageName = rpsCandidate.getImageName();
		byte[] photo = rpsCandidate.getPhoto();
		if (photo != null && photo.length != 0) {
			try {
				File candidatePhotoPath = new File(imageFolder);
				if (!candidatePhotoPath.exists())
					candidatePhotoPath.mkdirs();
				File imageFile = new File(candidatePhotoPath + File.separator + imageName);
				FileOutputStream fout = new FileOutputStream(imageFile);
				fout.write(photo);
				fout.close();
				candidateInfoViewScreen.setCandidatePhotoPath(FilenameUtils.separatorsToSystem(imageFile
						.getAbsolutePath()));
			} catch (IOException e) {
				logger.error("Error in reading candidate photo for candidate :" + rpsCandidate.getCandidateId1());
			}
		}

		candidateInfoViewScreen.setAssessmentID(rpsMasterAssociation.getRpsAssessment().getAssessmentCode());

		RpsQuestionPaper rpsQuestionPaper =
				rpsCandidateResponseService.getRpsQPaperByRpsMasterAssociationID(masterAssociationID);
		if (rpsQuestionPaper != null)
			candidateInfoViewScreen.setAssessmentSetID(rpsQuestionPaper.getQpCode());

		logger.info("---OUT-- getViewScreenDataForCandidate()");
		return candidateInfoViewScreen;
	}

	private String getCandidateFullName(RpsCandidate rpsCandidate, Integer uniquecanddiateid) {
		String fullName = "";
		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniquecanddiateid);
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			if (rpsCandidate.getFirstName() != null && !rpsCandidate.getFirstName().isEmpty()) {
				fullName = fullName.concat(rpsCandidate.getFirstName());
			}
			if (rpsCandidate.getMiddleName() != null && !rpsCandidate.getMiddleName().isEmpty()) {
				fullName = fullName.concat(" " + rpsCandidate.getMiddleName());
			}
			if (rpsCandidate.getLastName() != null && !rpsCandidate.getLastName().isEmpty()) {
				fullName = fullName.concat(" " + rpsCandidate.getLastName());
			}
		} else {
			PersonalInfo pInfo = mifForm.getPersonalInfo();
			if (pInfo != null) {
				if (pInfo.getFirstName() != null && !pInfo.getFirstName().isEmpty()) {
					fullName = fullName.concat(pInfo.getFirstName());
				}
				if (pInfo.getMiddleName() != null && !pInfo.getMiddleName().isEmpty()) {
					fullName = fullName.concat(" " + pInfo.getMiddleName());
				}
				if (pInfo.getLastName() != null && !pInfo.getLastName().isEmpty()) {
					fullName = fullName.concat(" " + pInfo.getLastName());
				}
			}
		}
		return fullName;
	}

	public ScoreScreenDataEntity getScoreScreenDataForCandidate(Integer masterAssociationID, Integer qpID) {

		logger.info("---IN-- getScoreScreenDataForCandidate()");
		ScoreScreenDataEntity scoreScreenDataEntity = new ScoreScreenDataEntity();
		RpsMasterAssociation rpsMasterAssociation =
				rpsMasterAssociationService.findByUniqueCandidateId(masterAssociationID);
		RpsCandidate rpsCandidate = rpsCandidateService.find(rpsMasterAssociation.getRpsCandidate().getCid());

		scoreScreenDataEntity.setCandidateID(rpsCandidate.getCandidateId1());

		Double score = 0.0;
		RpsCandidateResponse rpsCandidateResponse =
				rpsCandidateResponseService.findByUniqueCandidateId(masterAssociationID);
		if (rpsCandidateResponse != null && rpsCandidateResponse.getCandidateScore() != null)
			score = rpsCandidateResponse.getCandidateScore();

		scoreScreenDataEntity.setScore(score);

		logger.info("---OUT-- getScoreScreenDataForCandidate()");
		return scoreScreenDataEntity;
	}

	public CityVenueAcsEntity getListOfAllAcsCodes(List<RpsAcsServer> rpsAcsServersList,
			CityVenueAcsEntity cityVenueAcsEntity) {
		Set<String> acsCodes = new HashSet<>();
		for (RpsAcsServer rpsAcsServer : rpsAcsServersList)
			acsCodes.add(rpsAcsServer.getAcsServerId());
		cityVenueAcsEntity.setAcsCodes(acsCodes);
		return cityVenueAcsEntity;
	}

	public String getAcsListOneventCode(String eventCode, String startDate, String endDate) {
		logger.info("----IN--- getCityVenuAcsList");
		CityVenueAcsEntity cityVenueAcsEntity = new CityVenueAcsEntity();
		if (eventCode == null || eventCode.isEmpty() || startDate == null || startDate.isEmpty() || endDate == null
				|| endDate.isEmpty()) {
			logger.warn("Mandatory argument is missing-- eventCode: " + eventCode + ",  startDate :" + startDate
					+ " ,endDate : " + endDate);
			return gson.toJson(cityVenueAcsEntity);
		}
		List<RpsAcsServer> rpsAcsServersList = null;
		SimpleDateFormat sdf = new SimpleDateFormat(RpsConstants.DATE_FORMAT_WITH_TIME);
		try {
			Calendar rangeStartDate = Calendar.getInstance();
			Calendar rangeEndDate = Calendar.getInstance();
			rangeStartDate.setTime(sdf.parse(startDate));
			rangeEndDate.setTime(sdf.parse(endDate));
			// fetch list of acsServers for eventCode
			rpsAcsServersList =
					rpsBatchAcsAssociationService.getAllAcsServersByEventInDateRange(eventCode, rangeStartDate,
							rangeEndDate);
		} catch (ParseException e) {
			logger.error("Error in parsing date range selected--", e);
			return gson.toJson(cityVenueAcsEntity);
		}

		if (rpsAcsServersList == null || rpsAcsServersList.isEmpty()) {

			logger.warn("List of acsServers is empty for-- eventCode: " + eventCode);
			return gson.toJson(cityVenueAcsEntity);
		}

		Set<String> acsCodes = new HashSet<>();
		for (RpsAcsServer rpsAcsServer : rpsAcsServersList)
			acsCodes.add(rpsAcsServer.getAcsServerId());
		cityVenueAcsEntity.setAcsCodes(acsCodes);

		logger.info("----OUT--- getCityVenuAcsList");
		return gson.toJson(cityVenueAcsEntity);
	}
}