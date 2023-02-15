package com.merittrac.apollo.rps.services;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.Gson;
import com.merittrac.apollo.common.ResultProcessingRuleDo;
import com.merittrac.apollo.common.calendarUtil.TimeUtil;
import com.merittrac.apollo.data.bean.CandidateMasterAssociationEntity;
import com.merittrac.apollo.data.bean.CandidateResponseDetailsEntity;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsQpSection;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsBrLrService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsQpSectionService;
import com.merittrac.apollo.excel.CandidateSectionalScoreDetails;
import com.merittrac.apollo.qpd.qpgenentities.GroupWeightageCutOffEntity;
import com.merittrac.apollo.qpd.qpgenentities.SectionWeightageCutOffEntity;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.rest.service.BatchDetailsEntity;
import com.merittrac.apollo.rps.rest.service.CandidateExamScoreDetails;
import com.merittrac.apollo.rps.rest.service.CandidateScoresEntity;

/**
 * Results are communicated from here to other third party server
 *
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class RpsResultCommunicationService {
	@Autowired
	RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsBrLrService rpsBrLrService;

	@Autowired
	RpsQpSectionService rpsQpSectionService;

	@Autowired
	RpsBatchService rpsBatchService;

	@Autowired
	Gson gson;

	private Logger logger = LoggerFactory.getLogger(RpsResultCommunicationService.class);

	/**
	 * get Candidate Test Scores By BatchCode
	 * 
	 * @param batchCode
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public String getCandidateTestScoresByBatchDetails(String batchDetailsEntityJson) {
		logger.info("getCandidateTestScoresByBatchDetails called {}:", batchDetailsEntityJson);
		List<CandidateScoresEntity> candidateScoresEntitities = new ArrayList<>();
		CandidateExamScoreDetails candidateExamScoreDetails = null;
		BatchDetailsEntity batchDetailsEntity = gson.fromJson(batchDetailsEntityJson, BatchDetailsEntity.class);
		// batch details validation
		if (batchDetailsEntity == null) {
			logger.debug("BatchDetails is not in proper format. Please check " + batchDetailsEntityJson);
			candidateExamScoreDetails =
					new CandidateExamScoreDetails(candidateScoresEntitities);
			String json = gson.toJson(candidateExamScoreDetails);
			return json;
		}
		// get batch from details provided
		String batchDateString = batchDetailsEntity.getBatchDate();
		Calendar batchDate = TimeUtil.convertStringToCalender(batchDateString, TimeUtil.YYYY_MM_DD_HH_MM_SS);
		RpsBatch rpsBatch = rpsBatchService.getRpsBatchOnDetails(batchDetailsEntity.getBatchName(),
				batchDetailsEntity.getEventCode(), batchDate);
		if (rpsBatch == null) {
			logger.debug("No batch exists with BatchDetails provided. Please check " + batchDetailsEntityJson);
			candidateExamScoreDetails = new CandidateExamScoreDetails(candidateScoresEntitities);
			String json = gson.toJson(candidateExamScoreDetails);
			return json;
		}
		String batchCode = rpsBatch.getBatchCode();
		// fetch candidate details
		List<CandidateMasterAssociationEntity> candidateMasterAssoDataList =
				rpsMasterAssociationService.findAssociationsByBatchCode(batchCode);
		// fetch candidate response details
		List<CandidateResponseDetailsEntity> candidateResponseDetailsEntities =
				rpsMasterAssociationService.findCandidateResponseDetailsByBatchCode(batchCode);
		// check whether all candidate responses received
		if ((candidateMasterAssoDataList == null || candidateResponseDetailsEntities == null)
				|| (candidateMasterAssoDataList != null && candidateResponseDetailsEntities != null
						&& candidateMasterAssoDataList.size() != candidateResponseDetailsEntities.size())) {
			logger.debug("Count of present candidates and Count of responses received for candidates "
					+ " are not matching hence not posting scores to magic integration platform ");
			candidateExamScoreDetails = new CandidateExamScoreDetails(candidateScoresEntitities);
			String json = gson.toJson(candidateExamScoreDetails);
			return json;
		}
		// get list of assessments from list of masterAsso's
		List<String> assessmentCodes=new ArrayList<>();
		for (CandidateMasterAssociationEntity candidateMasterAssociationEntity : candidateMasterAssoDataList) {
			if (!assessmentCodes.contains(candidateMasterAssociationEntity.getAssessmentCode()))
				assessmentCodes.add(candidateMasterAssociationEntity.getAssessmentCode());
		}
		// get group cut off do per assessment
		Map<String, ResultProcessingRuleDo> assessmentToResultProcessingRule =
				rpsBrLrService.getRpsBrLrInfoOnAssessmentCodes(assessmentCodes);

		// get list of sections per assessments
		// and get cutoff mark for assessments

		Map<String, Float> assessmentToCutOffMap = new HashMap<>();
		for (String assessmentCode : assessmentCodes) {
			List<RpsQpSection> qpSections = rpsQpSectionService.findAllQpSectionsByAssmentCode(assessmentCode);
			List<String> qpSectionIds = new ArrayList<>();
			for (RpsQpSection rpsQpSection : qpSections) {
				qpSectionIds.add(rpsQpSection.getSecIdentifier());
			}
			assessmentToCutOffMap.put(assessmentCode,
					getCutOffMarkForAssessment(assessmentToResultProcessingRule.get(assessmentCode), qpSectionIds));
		}


		
		// convert list of responses to Map
		Map<Integer, CandidateResponseDetailsEntity> candidateResponseDetailsMap =
				new HashMap<Integer, CandidateResponseDetailsEntity>(candidateResponseDetailsEntities.size());
		for (CandidateResponseDetailsEntity candidateResponseDetailsEntity : candidateResponseDetailsEntities) {
			candidateResponseDetailsMap.put(candidateResponseDetailsEntity.getUniqueCandidateId(),
					candidateResponseDetailsEntity);
		}
		// nullify the list and use map further
		candidateResponseDetailsEntities = null;

		// fill the details as required to data object entity
		for (CandidateMasterAssociationEntity candidateMasterAssociationEntity : candidateMasterAssoDataList) {
			CandidateScoresEntity candidateScoresEntity = new CandidateScoresEntity();
			// candidateMasterAssociationEntity.getLoginID(), candidate_name,
			// exam_appeared_date, exam_appeared_batch_time,assesment,
			// list_section_scores, qp_set, obtained_marks, total_marks, exam_status,

			/**
			 * reg no
			 */
			candidateScoresEntity.setReg_no(candidateMasterAssociationEntity.getLoginID());

			/**
			 * cand name
			 */
			StringBuffer candidateName = new StringBuffer();
			if (candidateMasterAssociationEntity.getFirstname() != null
					&& candidateMasterAssociationEntity.getFirstname() != RpsConstants.NA)
				candidateName.append(candidateMasterAssociationEntity.getFirstname());
			else
				candidateName.append("");

			if (candidateMasterAssociationEntity.getMiddlename() != null
					&& candidateMasterAssociationEntity.getMiddlename() != RpsConstants.NA)
				candidateName.append(" " + candidateMasterAssociationEntity.getMiddlename());
			else
				candidateName.append("");

			if (candidateMasterAssociationEntity.getLastname() != null
					&& candidateMasterAssociationEntity.getLastname() != RpsConstants.NA)
				candidateName.append(" " + candidateMasterAssociationEntity.getLastname());
			else
				candidateName.append("");

			candidateScoresEntity.setCandidate_name(candidateName.toString());

			/**
			 * assesment
			 */
			String assessment = candidateMasterAssociationEntity.getAssessmentCode();
			String split[] = assessment.split("_");
			candidateScoresEntity.setAssessment(split[0]);

			/**
			 * exam_appeared_date
			 */
			// format date
			Calendar loginTime = TimeUtil.convertStringToCalender(candidateMasterAssociationEntity.getLoginTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			String loginTimeInString =
					TimeUtil.convertTimeAsString(loginTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);
			candidateScoresEntity.setExam_appeared_date(loginTimeInString);

			/**
			 * exam_appeared_batch_time
			 */
			// format date
			Calendar batchTime = TimeUtil.convertStringToCalender(candidateMasterAssociationEntity.getBatchStartTime(),
					TimeUtil.YYYY_MM_DD_HH_MM_SS);
			String batchTimeInString =
					TimeUtil.convertTimeAsString(batchTime.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT);
			candidateScoresEntity.setExam_appeared_batch_time(batchTimeInString);

			// response details =>
			CandidateResponseDetailsEntity candidateResponseDetailsEntity =
					candidateResponseDetailsMap.get(candidateMasterAssociationEntity.getUniqueCandidateId());
			if (candidateResponseDetailsEntity != null) {
				/**
				 * qp_set
				 */
				String qpCode = candidateResponseDetailsEntity.getQpcode();
				String qp[] = qpCode.split("_");
				candidateScoresEntity.setQp_set("set_" + qp[8]);

				/**
				 * total_marks
				 */
				Double totalScore = candidateResponseDetailsEntity.getTotalscore();
				candidateScoresEntity.setTotal_marks(totalScore);

				/**
				 * obtained_marks
				 */
				Double obtainedMarks = candidateResponseDetailsEntity.getCandidatescore();
				candidateScoresEntity.setObtained_marks(obtainedMarks);

				/**
				 * exam_status
				 */
				// pass/Fail based on cut off mark specified in QPD
				if (assessmentToCutOffMap != null && assessmentToCutOffMap.get(assessment) != null) {
					boolean isPass = calculateCutOffForCandidate(assessmentToCutOffMap.get(assessment), obtainedMarks,
							totalScore);
					if (isPass)
						candidateScoresEntity.setExam_status("PASS");
					else
						candidateScoresEntity.setExam_status("FAIL");
				}
				/**
				 * section scores
				 */
				List<CandidateSectionalScoreDetails> sectionScores = candidateResponseDetailsEntity.getSectionScores();
				candidateScoresEntity.setSection_scores(sectionScores);
				}
			candidateScoresEntitities.add(candidateScoresEntity);
		}
		candidateExamScoreDetails = new CandidateExamScoreDetails(batchDetailsEntity.getBatchName(), 
				batchDetailsEntity.getBatchDate(), candidateScoresEntitities);
		String json = gson.toJson(candidateExamScoreDetails);
		logger.debug("Method Returning getCandidateTestScoresByBatchCode :- " + json);
		return json;
	}

	private boolean calculateCutOffForCandidate(Float cutOffPerc, Double obtainedMarks, Double totalMarks) {
		double cutOffMarks = (totalMarks * ((double) cutOffPerc)) / 100;
		if (obtainedMarks >= cutOffMarks)
			return true;
		else
			return false;
	}

	private float getCutOffMarkForAssessment(ResultProcessingRuleDo resultProcessingRuleDo, List<String> sectionsList) {
		float perc = 0;
		String groupName = null;
		if (resultProcessingRuleDo == null || sectionsList == null || sectionsList.isEmpty()
				|| resultProcessingRuleDo.getGroupSectionMap() == null
				|| resultProcessingRuleDo.getGroupSectionMap().isEmpty())
			return perc;
		
		Map<String, List<SectionWeightageCutOffEntity>> brLrGroupSectionMap =
				resultProcessingRuleDo.getGroupSectionMap();
		
		loop: for (Map.Entry<String, List<SectionWeightageCutOffEntity>> groupSectionMap : brLrGroupSectionMap
				.entrySet()) {
			String gName = groupSectionMap.getKey();
			List<SectionWeightageCutOffEntity> sectionWeightageCutOffEntities = groupSectionMap.getValue();
			Map<String, SectionWeightageCutOffEntity> sectionWeightageCutOffEntitiesMap = new HashMap<>();

			for (SectionWeightageCutOffEntity sectionWeightageCutOffEntity : sectionWeightageCutOffEntities) {
				sectionWeightageCutOffEntitiesMap.put(sectionWeightageCutOffEntity.getSectionId(),
						sectionWeightageCutOffEntity);
			}

			for (String sectionId : sectionsList) {
				SectionWeightageCutOffEntity sectionCutOffEntity = sectionWeightageCutOffEntitiesMap.get(sectionId);
				if (sectionCutOffEntity == null) {
					continue loop;
				}
			}
			groupName = gName;
			break;
		}

		if (groupName != null && resultProcessingRuleDo.getGroupDetailsMap() != null
				&& !resultProcessingRuleDo.getGroupDetailsMap().isEmpty()) {
			GroupWeightageCutOffEntity groupWeightageCutOffEntity =
					resultProcessingRuleDo.getGroupDetailsMap().get(groupName);
			if (groupWeightageCutOffEntity != null) {
				// perc
				perc = groupWeightageCutOffEntity.getCutOff();
			}
		}
		return perc;
	}
}
