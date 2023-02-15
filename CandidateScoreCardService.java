package com.merittrac.apollo.acs.services;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCustomer;
import com.merittrac.apollo.acs.entities.AcsProperties;
import com.merittrac.apollo.acs.entities.AcsQpSections;
import com.merittrac.apollo.acs.entities.ResultProcReportTO;
import com.merittrac.apollo.acs.entities.SectionReportTO;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.acsrps.candidatescore.entities.AptitudeTestScore;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateEducationDetails;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateScoreCard;
import com.merittrac.apollo.acsrps.candidatescore.entities.CandidateSectionScoreDetails;
import com.merittrac.apollo.acsrps.candidatescore.services.CandidateScoreCardGenerator;
import com.merittrac.apollo.common.EventRule;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.excel.FileExportExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.GroupWeightageCutOffEntity;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.QualificationDetails;
import com.merittrac.apollo.tp.mif.TrainingDetails;

public class CandidateScoreCardService implements ICandidateScoreCardService {
	private static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static CandidateService candidateService = null;
	private static BatchService bs = null;
	private static QuestionService qs = null;
	private static AuthService as = null;
	private static BatchCandidateAssociationService batchCandidateAssociationService = null;
	private static Gson gson = null;
	private static ResultProcStatusService rpss = null;
	private static CDEAService cdeas = null;
	private static ACSEventRequestsService acsEventRequestsService = null;
	private static CustomerBatchService customerBatchService = null;
	static {
		if (candidateService == null)
			candidateService = CandidateService.getInstance();
		if (bs == null)
			bs = BatchService.getInstance();
		if (qs == null)
			qs = new QuestionService();
		if (as == null)
			as = new AuthService();
		if (rpss == null)
			rpss = new ResultProcStatusService();
		if (gson == null)
			gson = new Gson();
		if (batchCandidateAssociationService == null)
			batchCandidateAssociationService = new BatchCandidateAssociationService();
		acsEventRequestsService = new ACSEventRequestsService();
		customerBatchService = new CustomerBatchService();
		if (cdeas == null)
			cdeas = new CDEAService();

	}

	/**
	 * generates CandidateScoreCard from batchId
	 * 
	 * @param batchCode
	 * @param candidateScoreCards
	 * @return
	 * @throws GenericDataModelException
	 */
	private List<CandidateScoreCard> getCandidateScoreCardforBatch(String batchCode,
			List<CandidateScoreCard> candidateScoreCards) throws GenericDataModelException {
		List<String> assessmentCodes = batchCandidateAssociationService.getAssessmentsByBatchCode(batchCode);
		if (!assessmentCodes.isEmpty() && assessmentCodes != null) {
			for (String assessmentCode : assessmentCodes) {
				getCandidateScoreCardforAssessments(batchCode, assessmentCode, candidateScoreCards);
			}
		}
		return candidateScoreCards;
	}

	/**
	 * populates CandidateScoreCardDO for Candidate
	 * 
	 * @param mifForm
	 * @param candidateStatus
	 * @return
	 * @throws GenericDataModelException
	 */
	private CandidateScoreCard populateToCandidateScoreCardDO(MIFForm mifForm,
			AcsBatchCandidateAssociation batchCandidateAssociationTO) throws GenericDataModelException {
		CandidateScoreCard candidateScoreCard = new CandidateScoreCard();
		String candLoginId = batchCandidateAssociationTO.getCandidateLogin();
		if (mifForm.getPersonalInfo() != null) {
			candidateScoreCard.setCtsId(mifForm.getPersonalInfo().getUniqueIdNumber() == null ? "" : mifForm
					.getPersonalInfo().getUniqueIdNumber());
			candidateScoreCard.setGender(mifForm.getPersonalInfo().getGender());
			candidateScoreCard.setMtId(candLoginId);

			StringBuilder stringBuilder = new StringBuilder();
			stringBuilder.append(mifForm.getPersonalInfo().getFirstName());
			// stringBuilder.append(ACSConstants.EMPTY_FIELD);
			stringBuilder
					.append(mifForm.getPersonalInfo().getMiddleName() == null ? "" : " " + mifForm.getPersonalInfo()
					.getMiddleName());
			// stringBuilder.append(ACSConstants.EMPTY_FIELD);
			stringBuilder.append(mifForm.getPersonalInfo().getLastName() == null ? ""
					: " " + mifForm.getPersonalInfo()
					.getLastName());
			candidateScoreCard.setName(stringBuilder.toString());

			candidateScoreCard.setNationality(mifForm.getPersonalInfo().getNationality());
		}
		if (mifForm.getCommunicationAddress() != null) {
			/*
			 * candidateScoreCard.setMailId(mifForm.getCommunicationAddress(). getEmailId1());
			 * candidateScoreCard.setMobileNo(mifForm.getCommunicationAddress ().getMobile());
			 */
			candidateScoreCard.setCurrentAddress(mifForm.getCommunicationAddress().toString());
		}
		
		
		//adding workedInCompany and getWorkedInCompanyTime(EmpId) for cts report
		if (mifForm.getExperience() != null) {
			if (mifForm.getExperience().getWorkedInCompany() != null
					|| mifForm.getExperience().getWorkedInCompanyTime() != null) {
				candidateScoreCard.setWorkedInCompany(mifForm.getExperience().getWorkedInCompany());

				if (mifForm.getExperience().getWorkedInCompany().contains("Yes")) {
					candidateScoreCard.setWorkedInCompanyTime(mifForm.getExperience().getWorkedInCompanyTime());
				} else {
					candidateScoreCard.setWorkedInCompanyTime(ACSConstants.NA);
				}
			}
		}
		if (mifForm.getQualification() != null) {
			String gapInYears = ACSConstants.EMPTY_FIELD;
			if (mifForm.getQualification().getGapInEducation() != null) {
				if (mifForm.getQualification().getGapInEducation().equalsIgnoreCase("Yes"))
					gapInYears =
							mifForm.getQualification().getYrsOfGap() == null ? ACSConstants.EMPTY_FIELD : mifForm
									.getQualification().getYrsOfGap();
				else
					gapInYears = "0";
			}

			candidateScoreCard.setEducationGapInYears(gapInYears);
			candidateScoreCard.setStandingArrears(mifForm.getQualification().getStandingArrears());
			candidateScoreCard.setHistoryOfArrears(mifForm.getQualification().getHistoryArrears());
			candidateScoreCard.setCandidateEducationDetailsMap(populateEducationalDetails(mifForm.getQualification()
					.getQualificationLevelToDetailsMap()));
			if (mifForm != null && mifForm.getQualification() != null
					&& mifForm.getQualification().getUniversityRegnNo() != null) {
				candidateScoreCard.setUnivercityRegNo(mifForm.getQualification().getUniversityRegnNo());
			} else {
				candidateScoreCard.setUnivercityRegNo("N/A");
			}

			// certification details
			if (mifForm.getTrainingDetails() != null && !mifForm.getTrainingDetails().isEmpty()) {
				TrainingDetails trainingDetails = mifForm.getTrainingDetails().get(0);
				candidateScoreCard.setCertificationDuration(trainingDetails.getDuration() == null ? ACSConstants.NA
						: trainingDetails.getDuration().toString());
				candidateScoreCard.setCertifiedSkill(trainingDetails.getProgramName() == null ? ACSConstants.NA
						: trainingDetails.getProgramName());
				candidateScoreCard
						.setCertifiedCompany(trainingDetails.getInstituteOrganization() == null ? ACSConstants.NA
								: trainingDetails.getInstituteOrganization());
			} else {
				candidateScoreCard.setCertificationDuration(ACSConstants.NA);
				candidateScoreCard.setCertifiedSkill(ACSConstants.NA);
				candidateScoreCard.setCertifiedCompany(ACSConstants.NA);
			}
		}
		if (mifForm.getPermanentAddress() != null)
			candidateScoreCard.setPermanentAddress(mifForm.getPermanentAddress().toString());
		candidateScoreCard.setCreationDate(new Date());
		candidateScoreCard.setAptitudeTestScores(calculateScore(batchCandidateAssociationTO, candidateScoreCard));

		AcsCustomer custDetails =
				cdeas.getCustomerDetailsByAssessmentCode(batchCandidateAssociationTO.getAssessmentCode());
		if (custDetails != null)
			candidateScoreCard.setCustomerName(custDetails.getCustomerName());
		else
			candidateScoreCard.setCustomerName(ACSConstants.NA);

		candidateScoreCard.setCandidateQuestionAndAnswers(mifForm.getQuestionsAnswersMap());
		candidateScoreCard.setMailId(mifForm.getPermanentAddress().getEmailId1());
		candidateScoreCard.setMobileNo(mifForm.getPermanentAddress().getMobile());
		if (batchCandidateAssociationTO.getLoginTime() != null) {
			candidateScoreCard.setTestTakenDate(TimeUtil.convertTimeAsString(batchCandidateAssociationTO.getLoginTime()
					.getTimeInMillis(), TimeUtil.DD_MMM_YYYY));
		} else {
			candidateScoreCard.setTestTakenDate(ACSConstants.EMPTY_FIELD);
		}

		return candidateScoreCard;
	}

	/**
	 * calculates Age for Candidate
	 * 
	 * @param dob
	 * @return
	 */
	private int calculateAge(String dob) {
		int year = 0, month = 0, day = 0;
		String[] split = dob.split("-");
		try {
			year = Integer.parseInt(split[0]);
			month = ACSConstants.MONTH_ENUM.valueOf(split[1]).ordinal();
			day = Integer.parseInt(split[2]);
		} catch (NumberFormatException ex) {
			return 0;
		}
		Calendar birthCal = new GregorianCalendar(year, month, day);
		Calendar nowCal = new GregorianCalendar();
		int age = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);
		boolean isMonthGreater = birthCal.get(Calendar.MONTH) >= nowCal.get(Calendar.MONTH);
		boolean isMonthSameButDayGreater =
				birthCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
						&& birthCal.get(Calendar.DAY_OF_MONTH) > nowCal.get(Calendar.DAY_OF_MONTH);
		if (isMonthGreater || isMonthSameButDayGreater) {
			age = age - 1;
		}
		return age;
	}

	/**
	 * populates Educational Details for Candidate
	 * 
	 * @param qualificationLevelToDetailsMap
	 * @return
	 */
	private Map<String, CandidateEducationDetails> populateEducationalDetails(
			Map<String, QualificationDetails> qualificationLevelToDetailsMap) {
		Map<String, CandidateEducationDetails> educationDetails = new TreeMap<String, CandidateEducationDetails>();
		if (qualificationLevelToDetailsMap != null) {
			for (Map.Entry<String, QualificationDetails> qualificationLevelDetails : qualificationLevelToDetailsMap
					.entrySet()) {
				QualificationDetails qualificationDetails = qualificationLevelDetails.getValue();
				CandidateEducationDetails candidateEducationDetails = new CandidateEducationDetails();
				candidateEducationDetails.setLevel(qualificationLevelDetails.getKey());
				candidateEducationDetails.setBranch(qualificationDetails.getSpecialization());
				candidateEducationDetails.setDegree(qualificationDetails.getEducation());
				candidateEducationDetails.setInstitution(qualificationDetails.getUniversityCollege());
				candidateEducationDetails.setPercentage(qualificationDetails.getPercentage());
				candidateEducationDetails.setUniversity(qualificationDetails.getUniversity());
				candidateEducationDetails.setYearOfPassing(Integer.toString(qualificationDetails.getPassingYear()));
				educationDetails.put(qualificationLevelDetails.getKey(), candidateEducationDetails);
			}
		}
		return educationDetails;
	}

	/**
	 * calculates Score for candidate
	 * 
	 * @param candidateScoreCard
	 * 
	 * @return
	 * @throws GenericDataModelException
	 */
	private List<AptitudeTestScore> calculateScore(AcsBatchCandidateAssociation batchCandidateAssociationTO,
			CandidateScoreCard candidateScoreCard) throws GenericDataModelException {
		Set<String> sectionIdentifierSet = new LinkedHashSet<>();
		List<AptitudeTestScore> aptitudeTestScores = new ArrayList<AptitudeTestScore>();
		List<AcsQpSections> qpSections = qs.getSidsFromQpaperCode(batchCandidateAssociationTO.getQpaperCode());
		List<CandidateSectionScoreDetails> candidateSectionScoreDataList =
				new ArrayList<CandidateSectionScoreDetails>();
		ResultProcReportTO resultProcReportTO =
				rpss.findByBatchIDAssessmentIDCandID(batchCandidateAssociationTO.getBatchCode(),
						batchCandidateAssociationTO.getAssessmentCode(), batchCandidateAssociationTO.getCandidateId());
		candidateScoreCard.setLoginId(batchCandidateAssociationTO.getCandidateLogin());
		if (resultProcReportTO != null) {
			List<SectionReportTO> sectionReportTOList =
					new ArrayList<SectionReportTO>(resultProcReportTO.getSectionReports());
			Collections.sort(sectionReportTOList);
			long totalSecScore = 0;
			for (SectionReportTO sectionReportTO : sectionReportTOList) {
				if (null != qpSections && !qpSections.isEmpty()) {
					for (AcsQpSections qpSectionsDO : qpSections) {
						if (qpSectionsDO.getSecTitle().equals(sectionReportTO.getSection())) {
							totalSecScore = Math.round(qs.getSectionLevelMarksForASection(qpSectionsDO.getSid()));
							getSectionLevelScores(sectionIdentifierSet, candidateSectionScoreDataList,
									resultProcReportTO, qpSectionsDO, batchCandidateAssociationTO.getQpaperCode(),
									batchCandidateAssociationTO.getBcaid());
						}
					}
				}
				AptitudeTestScore aptitudeTestScore = new AptitudeTestScore();
				aptitudeTestScore.setName(sectionReportTO.getSection() + " (Out of " + totalSecScore + " Marks)");
				aptitudeTestScore.setScore(Double.toString(sectionReportTO.getMarksObtained()));
				aptitudeTestScores.add(aptitudeTestScore);
			}

			candidateScoreCard.setAttemptedQuestionsCount(resultProcReportTO.getAttemptedCount() == null ? "0" : String
					.valueOf(resultProcReportTO.getAttemptedCount()));
			candidateScoreCard.setCorrectQuestionsCount(resultProcReportTO.getCorrectCount() == null ? "0" : String
					.valueOf(resultProcReportTO.getCorrectCount()));
			candidateScoreCard.setTotalScore(String.valueOf(resultProcReportTO.getTotalMarks()));
			candidateScoreCard.setScorePercentage(String.valueOf(resultProcReportTO.getPercntgeMarks()));

		} else {
			candidateScoreCard.setTotalScore("0.0");
			candidateScoreCard.setScorePercentage("0.0");
			candidateScoreCard.setAttemptedQuestionsCount("0");
			candidateScoreCard.setCorrectQuestionsCount("0");
		}
		candidateScoreCard.setCandidateSectionScoreDataList(candidateSectionScoreDataList);
		candidateScoreCard.setAptitudeTestScores(aptitudeTestScores);
		int totalQuestions = qs.getTotalNumberOfQuestions(qpSections);
		candidateScoreCard.setTotalQuestions(Integer.toString(totalQuestions));
		// br rule
		double totalMarks = qs.getSectionLevelMarksForAllSections(qpSections);
		candidateScoreCard.setTotalAssessmentMarks(Double.toString(totalMarks));
		Integer cutOffMark = getCutOffMarkFromBrRule(batchCandidateAssociationTO, totalMarks);
		candidateScoreCard.setTotalQualifyingMark(Integer.toString(cutOffMark));
		return aptitudeTestScores;
	}

	private Integer
			getCutOffMarkFromBrRule(AcsBatchCandidateAssociation batchCandidateAssociationTO, double totalMarks)
					throws GenericDataModelException {
		Integer cutOffMark = 0;
		BRulesExportEntity bRulesExportEntity = customerBatchService.getBRulesExportEntity(
				batchCandidateAssociationTO.getBatchCode(), batchCandidateAssociationTO.getAssessmentCode());
		if (bRulesExportEntity != null && bRulesExportEntity.getResultProcessingRules() != null) {
			Map<String, GroupWeightageCutOffEntity> groupDetailsMap =
					bRulesExportEntity.getResultProcessingRules().getGroupDetailsMap();
			if (groupDetailsMap != null) {
				GroupWeightageCutOffEntity cutOffEntity = groupDetailsMap.get(ACSConstants.GROUP_LEVEL_CUT_OFF_KEY);
				if (cutOffEntity != null && cutOffEntity.getCutOff() != null) {
					double score = (totalMarks * ((double) cutOffEntity.getCutOff())) / 100;
					double roundOff = Math.round(score);
					cutOffMark = (int) roundOff;
				}
			}
		}
		return cutOffMark;
	}

	public void getSectionLevelScores(Set<String> sectionIdentifierSet,
			List<CandidateSectionScoreDetails> candidateSectionScoreDataList, ResultProcReportTO procReportTO,
			AcsQpSections qpSectionsDO, String qpapercode, int bcaid) throws GenericDataModelException {
		logger.debug("--IN-- getSectionLevelScores() for procReportTO :{}", procReportTO);
		sectionIdentifierSet.add(qpSectionsDO.getSecTitle());
		Double candSecScore = 0.0;
		double totalSecScore = qs.getSectionLevelMarksForASection(qpSectionsDO.getSid());
		Set<SectionReportTO> sectionReports = procReportTO.getSectionReports();
		if (sectionReports != null && !sectionReports.isEmpty()) {
			for (SectionReportTO sectionReportTO : sectionReports) {
				if (qpSectionsDO.getSecTitle().equals(sectionReportTO.getSection())) {
					candSecScore = sectionReportTO.getMarksObtained();
				}
			}
		}
		int totalQuestionsPerSection = qs.getTotalNumberOfQuestions(new ArrayList<>(Arrays.asList(qpSectionsDO)));
		int attemptedQuestionsPerSection =
				qs.getSectionLevelAttemptedQuestions(bcaid, qpSectionsDO.getSid(), qpapercode);
		int correctQuestionsPerSection = qs.getCorrectNumberOfQuestionsPerSection(qpSectionsDO.getSid(), bcaid);
		Double scorePercentage = 0.0;
		if (totalSecScore != 0.0 && candSecScore != 0.0) {
			scorePercentage = (candSecScore / totalSecScore) * (ACSConstants.PERCENTAGE);
			DecimalFormat df = new DecimalFormat("#.00");
			CandidateSectionScoreDetails scoreData =
					new CandidateSectionScoreDetails(String.valueOf(qpSectionsDO.getSid()), qpSectionsDO.getSecTitle(),
							String.valueOf(candSecScore), df.format(scorePercentage), String.valueOf(totalSecScore),
							String.valueOf(totalQuestionsPerSection), String.valueOf(attemptedQuestionsPerSection));
			if (candSecScore < 0)
				scoreData.setScorePercentage("0.0");
			scoreData.setSectionCorrectQuestionsCount(String.valueOf(correctQuestionsPerSection));
			candidateSectionScoreDataList.add(scoreData);
		}
		if (candSecScore == 0.0 || candSecScore == null) {
			CandidateSectionScoreDetails scoreData =
					new CandidateSectionScoreDetails(String.valueOf(qpSectionsDO.getSid()), qpSectionsDO.getSecTitle(),
							String.valueOf(candSecScore), String.valueOf(scorePercentage),
							String.valueOf(totalSecScore), String.valueOf(totalQuestionsPerSection),
							String.valueOf(attemptedQuestionsPerSection));
			scoreData.setSectionCorrectQuestionsCount(String.valueOf(correctQuestionsPerSection));
			candidateSectionScoreDataList.add(scoreData);
		}
		logger.debug("--OUT-- getSectionLevelScores() returning :{}", candidateSectionScoreDataList);
	}

	/**
	 * generates CandidateScoreCard from batchId assessmentID
	 * 
	 * @param batchCode
	 * @param assessmentCode
	 * @param candidateScoreCards
	 * @return
	 * @throws GenericDataModelException
	 */
	private List<CandidateScoreCard> getCandidateScoreCardforAssessments(String batchCode, String assessmentCode,
			List<CandidateScoreCard> candidateScoreCards) throws GenericDataModelException {
		List<String> setIds =
				batchCandidateAssociationService.getSetIdByBatchCodeAssessmentCode(batchCode, assessmentCode);
		if (setIds != null && !setIds.isEmpty()) {
			for (String setId : setIds) {
				getCandidateScoreCardforQPSets(batchCode, assessmentCode, setId, candidateScoreCards);
			}
		} else {
			getCandidateScoreCardforQPSets(batchCode, assessmentCode, null, candidateScoreCards);
		}
		return candidateScoreCards;
	}

	/**
	 * generates CandidateScoreCard from batchId assessmentID qpID
	 * 
	 * @param batchCode
	 * @param assessmentCode
	 * @param setID
	 * @param candidateScoreCards
	 * @return
	 * @throws GenericDataModelException
	 */
	private List<CandidateScoreCard> getCandidateScoreCardforQPSets(String batchCode, String assessmentCode,
			String setID, List<CandidateScoreCard> candidateScoreCards) throws GenericDataModelException {
		logger.info("inside generateCandidateScoreCard " + setID);
		List<AcsBatchCandidateAssociation> batchCandidateAssociations = null;
		if (setID == null) {
			logger.debug("setId is null hence just assessmnet and batch are considered");
			batchCandidateAssociations =
					batchCandidateAssociationService.getBatchCandAssociationByBatchCodeAndAssessmentCode(batchCode,
							assessmentCode);
		} else
			batchCandidateAssociations =
					batchCandidateAssociationService.getBatchCandAssociationByBatchCodeAssessmentCodeSetId(batchCode,
							assessmentCode, setID);
		if (batchCandidateAssociations != null) {
			for (AcsBatchCandidateAssociation acsBatchCandidateAssociation : batchCandidateAssociations) {
				AcsCandidate acsCandidate =
						candidateService.getCandidateDetailsFromCandId(acsBatchCandidateAssociation.getCandidateId());
				if (acsCandidate.getMifData() != null) {
					MIFForm mifForm = gson.fromJson(acsCandidate.getMifData(), MIFForm.class);

					CandidateScoreCard candidateScoreCard =
							populateToCandidateScoreCardDO(mifForm, acsBatchCandidateAssociation);
					AcsCandidate candidateTO =
							candidateService.getCandidateDetailsFromCandId(acsBatchCandidateAssociation
									.getCandidateId());

					candidateScoreCard.setLogoPath(as.getCustomerLogoPathLocation(assessmentCode, batchCode));

					if (candidateTO != null && candidateTO.getDob() != null) {
						Calendar dob = candidateTO.getDob();
						SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MMM-dd");
						String dateFormat = simpleDateFormat.format(new Date(dob.getTimeInMillis()));
						candidateScoreCard.setAge(Integer.toString(calculateAge(dateFormat)));
						candidateScoreCard.setDob(getFormatedDate(dateFormat));
						candidateScoreCard.setPhoto(candidateTO.getImageName() == null ? ACSConstants.EMPTY_FIELD
								: candidateTO.getImageName());
					}
					logger.info("candidateScoreCard for " + candidateScoreCard);
					candidateScoreCards.add(candidateScoreCard);
				}
			}
		}
		return candidateScoreCards;
	}

	/**
	 * generates CandidateScoreCard from batchId assessmentID setID
	 * 
	 * @param batchId
	 * @param assessmentCode
	 * @param setID
	 * @return
	 * @throws GenericDataModelException
	 */
	@Override
	public FileExportExportEntity generateCandidateScoreCard(List<String> batchCodes, String assessmentCode,
			String setID) throws GenericDataModelException {
		List<CandidateScoreCard> candidateScoreCards = new ArrayList<>();
		FileExportExportEntity exportExportEntity = null;
		String fileName = "";
		if (batchCodes.size() == 0) {
			logger.error("list of batchCodes is empty:" + batchCodes);
			exportExportEntity =
					new FileExportExportEntity("0", fileName,
							"Problem in Genarating Candidate Score Card as list of batchCodes is empty");
			return exportExportEntity;
		}
		for (String batchCode : batchCodes) {
			if (!batchCode.equals("0") && !assessmentCode.equals("0") && !setID.equals("0")) {
				candidateScoreCards =
						getCandidateScoreCardforQPSets(batchCode, assessmentCode, setID, candidateScoreCards);
			} else if (!batchCode.equals("0") && !assessmentCode.equals("0") && setID.equals("0")) {
				candidateScoreCards =
						getCandidateScoreCardforAssessments(batchCode, assessmentCode, candidateScoreCards);
			} else if (!batchCode.equals("0") && assessmentCode.equals("0")) {
				candidateScoreCards = getCandidateScoreCardforBatch(batchCode, candidateScoreCards);
			}
		}
		if (!candidateScoreCards.isEmpty()) {
			// Set<CandidateScoreCard> uniqueSet = new HashSet<CandidateScoreCard>(candidateScoreCards);
			// candidateScoreCards = new ArrayList<CandidateScoreCard>(uniqueSet);
			File path =
					new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
							+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
							+ ACSConstants.ACS_CANDIDATE_SCORE_DIR);
			if (!path.exists()) {
				path.mkdir();
			}
			fileName =
					path.getPath() + File.separator + ACSConstants.SCORE_CANDIDATE_SHEET_NAME
							+ System.currentTimeMillis() + ACSConstants.SCORE_CANDIDATE_PDF;

			String eventCode = cdeas.getEventCodeByBatchCode(batchCodes.get(0));

			AcsProperties acsProperties =
					acsEventRequestsService.getAcsPropertiesOnPropNameAndEventCode(
							EventRule.PRO_CERTIFICATE_FORMAT.toString(), eventCode);

			try {
				// CORPORATE#IIBF#IGBC#COGNIZANT
				if (acsProperties != null) {
					switch (acsProperties.getPropertyValue()) {
						case "COGNIZANT":
							CandidateScoreCardGenerator.pdfGenerator(fileName, candidateScoreCards);
							break;

						case "IGBC":
							CandidateScoreCardGenerator.pdfReportGeneratorForIGBC(fileName, candidateScoreCards);
							break;

						case "STANDARD_REPORT":
							CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName,
									candidateScoreCards);
							break;

						default:
							CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName,
									candidateScoreCards);
							break;
					}

				} else {
					logger.debug(" ACS properties EVENT RULES is NULL for eventcode:{} , hence displaying Corporate",
							eventCode);
					CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName, candidateScoreCards);
				}

				// CandidateScoreCardGenerator.pdfGenerator(fileName, candidateScoreCards);
				exportExportEntity =
						new FileExportExportEntity("Success", fileName, "Successfully Candidate Score Card Genarated");
			} catch (DocumentException | IOException e) {
				exportExportEntity =
						new FileExportExportEntity("Failure", fileName, "Problem in Genarating Candidate Score Card");
				return exportExportEntity;
			}
		} else {
			exportExportEntity = new FileExportExportEntity("Failure", fileName, "No MIF data for selected Candidates");
		}
		return exportExportEntity;
	}

	@Override
	public FileExportExportEntity generateCandidateScoreCardOnCandidateIds(Integer[] arrayOfBcaIds)
			throws GenericDataModelException {
		List<CandidateScoreCard> candidateScoreCards = new ArrayList<>();
		FileExportExportEntity exportExportEntity = null;
		String fileName = "";
		List<Integer> bcaIds = new ArrayList<>();
		if (arrayOfBcaIds != null && arrayOfBcaIds.length != 0)
			bcaIds = Arrays.asList(arrayOfBcaIds);

		if (bcaIds.size() == 0) {
			logger.error("CandidateIds cannot empty");
			exportExportEntity =
					new FileExportExportEntity("0", fileName,
							"Problem in Genarating Candidate Score Card as BatchId is 0");
			return exportExportEntity;
		}
		if (bcaIds.size() > 0) {
			for (Integer bcaId : bcaIds) {
				AcsBatchCandidateAssociation acsBatchCandidateAssociation =
						batchCandidateAssociationService.getBatchCandidateAssociationById(bcaId);
				// AcsCandidateStatus candidateStatusTO =
				// candidateService.getCandidateStatus(bcaId);
				getCandidateScoreCardforCandidateIds(acsBatchCandidateAssociation, candidateScoreCards);
			}
		}
		if (!candidateScoreCards.isEmpty()) {
			File path =
					new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
							+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
							+ ACSConstants.ACS_CANDIDATE_SCORE_DIR);
			if (!path.exists()) {
				path.mkdir();
			}
			fileName =
					path.getPath() + File.separator + ACSConstants.SCORE_CANDIDATE_SHEET_NAME
							+ System.currentTimeMillis() + ACSConstants.SCORE_CANDIDATE_PDF;
			String eventCode = cdeas.getEventCodeByBcaId(bcaIds.get(0));

			AcsProperties acsProperties =
					acsEventRequestsService.getAcsPropertiesOnPropNameAndEventCode(
							EventRule.PRO_CERTIFICATE_FORMAT.toString(), eventCode);

			try {
				// STANDARD_REPORT#IIBF#IGBC#COGNIZANT
				if (acsProperties != null) {
					switch (acsProperties.getPropertyValue()) {
						case "COGNIZANT":
							CandidateScoreCardGenerator.pdfGenerator(fileName, candidateScoreCards);
							break;

						case "IGBC":
							CandidateScoreCardGenerator.pdfReportGeneratorForIGBC(fileName, candidateScoreCards);
							break;

						case "STANDARD_REPORT":
							CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName,
									candidateScoreCards);
							break;

						default:
							CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName,
									candidateScoreCards);
							break;
					}

				} else {
					logger.debug(" ACS properties EVENT RULES is NULL for eventcode:{} , hence displaying Corporate",
							eventCode);
					CandidateScoreCardGenerator.candidateStandardPdfReportGenerator(fileName, candidateScoreCards);
				}
				exportExportEntity =
						new FileExportExportEntity("200", fileName, "Successfully Candidate Score Card Genarated");
			} catch (DocumentException | IOException e) {
				exportExportEntity =
						new FileExportExportEntity("0", fileName, "Problem in Genarating Candidate Score Card");
				return exportExportEntity;
			}
		} else {
			exportExportEntity = new FileExportExportEntity("Failure", fileName, "No MIF data for selected Candidates");
		}
		return exportExportEntity;
	}

	private List<CandidateScoreCard> getCandidateScoreCardforCandidateIds(
			AcsBatchCandidateAssociation acsBatchCandidateAssociation, List<CandidateScoreCard> candidateScoreCards)
			throws GenericDataModelException {
		logger.info("inside getCandidateScoreCardforCandidateIds ");
		AcsCandidate acsCandidate =
				candidateService.getCandidateDetailsFromCandId(acsBatchCandidateAssociation.getCandidateId());
		if (acsCandidate != null) {
			if (acsCandidate.getMifData() != null) {
				MIFForm mifForm = gson.fromJson(acsCandidate.getMifData(), MIFForm.class);

				CandidateScoreCard candidateScoreCard =
						populateToCandidateScoreCardDO(mifForm, acsBatchCandidateAssociation);
				AcsCandidate candidateTO =
						candidateService.getCandidateDetailsFromCandId(acsBatchCandidateAssociation.getCandidateId());
				candidateScoreCard.setLogoPath(as.getCustomerLogoPathLocation(
						acsBatchCandidateAssociation.getAssessmentCode(), acsBatchCandidateAssociation.getBatchCode()));

				if (candidateTO != null && candidateTO.getDob() != null) {
					Calendar dob = candidateTO.getDob();
					SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MMM-dd");
					String dateFormat = simpleDateFormat.format(new Date(dob.getTimeInMillis()));
					candidateScoreCard.setAge(Integer.toString(calculateAge(dateFormat)));
					candidateScoreCard.setDob(getFormatedDate(dateFormat));
					candidateScoreCard.setPhoto(candidateTO.getImageName() == null ? ACSConstants.EMPTY_FIELD
							: candidateTO.getImageName());
				}
				logger.info("candidateScoreCard for " + candidateScoreCard);
				candidateScoreCards.add(candidateScoreCard);
			}
		}
		return candidateScoreCards;
	}

	/**
	 * Format date from "yyyy-MMM-dd" to "dd-MMM-yyyy"
	 * 
	 * @throws ParseException
	 * @since Apollo v2.0
	 * @see
	 */
	private String getFormatedDate(String dateString) {
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd");
		try {
			Date date = formatter.parse(dateString);
			return dateFormat.format(date);
		} catch (ParseException e) {
			logger.error("ParseException while executing getFormatedDate...", e);
			return dateString;
		}
	}
}
