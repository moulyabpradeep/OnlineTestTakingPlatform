/**
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * <p/>
 * Last modified by: Jun 5, 2013 11:49:42 AM - Siddhant_S
 */
package com.merittrac.apollo.rps.services;

import java.lang.reflect.Type;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.data.bean.QuestionToTopicDO;
import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsBatchAcsAssociation;
import com.merittrac.apollo.data.entity.RpsCandidate;
import com.merittrac.apollo.data.entity.RpsCandidateMIFDetails;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociation;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsSectionCandidateResponse;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsCandidateMIFDetailsService;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsMasterAssociationService;
import com.merittrac.apollo.data.service.RpsQuestionAssociationService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.data.service.RpsQuestionService;
import com.merittrac.apollo.rps.common.ABCDXOptionsEnum;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.reportexport.entity.Parameters;
import com.merittrac.apollo.rps.viewresults.entity.CandReps1OX;
import com.merittrac.apollo.rps.viewresults.entity.CandRespRawScore;
import com.merittrac.apollo.rps.viewresults.entity.Export1OXEntity;
import com.merittrac.apollo.rps.viewresults.entity.ExportRawEntity;
import com.merittrac.apollo.rps.viewresults.entity.QSet1OXEntity;
import com.merittrac.apollo.rps.viewresults.entity.QSetRawEntity;
import com.merittrac.apollo.tp.mif.ContactInfo;
import com.merittrac.apollo.tp.mif.MIFForm;
import com.merittrac.apollo.tp.mif.PersonalInfo;

/**
 * @author Siddhant_S - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 */

public class ViewResultsServices {
    private static final Logger _logger = LoggerFactory.getLogger(ViewResultsServices.class);

    /**
     * @return the Logger
     */
    public static Logger getLogger() {
        return _logger;
    }

    final String batchCode = "batchCode";
    final String eventCode = "eventCode";
    final String acsCode = "acsCode";
    final String assessmentCode = "assessmentCode";
    final String setCode = "setCode";
    final DecimalFormat decimalFormat = new DecimalFormat("###.##");

    @Autowired
    RpsBatchService batchService;

    @Autowired
    RpsAcsServerServices acsServerServices;

    @Autowired
    RpsMasterAssociationService rpsMasterAssociationService;

	@Autowired
	RpsCandidateMIFDetailsService rpsCandidateMIFDetailsService;

    @Autowired
    RpsCandidateResponseService rpsCandidateResponseService;

    @Autowired
    RpsQuestionPaperService rpsQuestionPaperService;
    
    @Autowired
    RpsQuestionService rpsQuestionService;

    @Autowired
    RpsQuestionAssociationService rpsQuestionAssociationService;

    @Autowired
    Gson gson;

    @Autowired
    RpsBatchAcsAssociationService rpsBatchAcsAssociationService;

    @Autowired
    RpsEventService rpsEventService;

	/*public List<RpsCandidateResponse> getCandidateResponse(Integer batchId, Integer acsId, String assessmentCode,
            String qpCode) {
		return rpsCandidateResponseService.getCandidateResponseByAssessmentBatchAcsSetCodes(assessmentCode, batchId,
				acsId, qpCode);
	}*/

    private HashSet<QSetRawEntity> getCandidatesRawScore(List<RpsCandidateResponse> candidateResponse) throws Exception {
        HashSet<QSetRawEntity> qSetRawEntities = new HashSet<>();
        Set<CandRespRawScore> respRawScores = new HashSet<>();
        if (!candidateResponse.isEmpty()) {
            QSetRawEntity qSetRawEntity = new QSetRawEntity();
            getLogger().debug("Getting the candidate details");
            //RpsQuestionPaper rpsQuestionPaper = candidateResponse.get(0).getRpsQuestionPaper();
            RpsQuestionPaper paper = rpsQuestionPaperService.findOne(candidateResponse.get(0).getRpsQuestionPaper().getQpId());
            Set<RpsQuestionAssociation> questionSectionSet = paper.getRpsQuestionAssociations();
            Map<String, Double> sectionMarks = new LinkedHashMap<String, Double>();

            for (RpsQuestionAssociation association : questionSectionSet) {
                sectionMarks.put(association.getRpsQpSection().getSecIdentifier(), association.getRpsQpSection().getSecScore());
            }
            Double totalMarks = candidateResponse.get(0).getRpsQuestionPaper().getTotalScore();
            for (RpsCandidateResponse rpsCandidateResponse : candidateResponse) {
                Map<String, Double> sectionScore = new LinkedHashMap<String, Double>();
                CandRespRawScore candRespRawScore = new CandRespRawScore();
                Integer uniqueCandidateId = rpsCandidateResponse.getRpsMasterAssociation().getUniqueCandidateId();
                RpsCandidate candidate =
                        rpsMasterAssociationService.getCandidateForUniqueId(uniqueCandidateId);
                candRespRawScore.setUniqueCandId(uniqueCandidateId);
                candRespRawScore.setCandidateId1(candidate.getCandidateId1());
                candRespRawScore.setCandidateId2(candidate.getCandidateId2());
                candRespRawScore.setCandidateId3(candidate.getCandidateId3());
				//
				getCandidateDetailsFromMIF(candRespRawScore, candidate, uniqueCandidateId);
                rpsCandidateResponse.getCandidateScore();

                double totalScore = 0.0;
                if (rpsCandidateResponse.getCandidateScore() != null)
                    totalScore = rpsCandidateResponse.getCandidateScore();
                candRespRawScore.setTotalScore(totalScore);

                double percentage = 0.0;
                if (totalScore != 0.0 && totalMarks != null)
                    percentage = (100.0 * totalScore) / totalMarks.doubleValue();

                candRespRawScore.setPercentage(Double.valueOf(decimalFormat.format(percentage)));

                Set<RpsSectionCandidateResponse> candidateSectionScores = rpsCandidateResponse.getRpsSectionCandidateResponses();
                Iterator<RpsSectionCandidateResponse> rpsCandidateSectionItr = candidateSectionScores.iterator();
                while (rpsCandidateSectionItr.hasNext()) {
                    RpsSectionCandidateResponse rpsSectionResponse = rpsCandidateSectionItr.next();
                    sectionScore.put(rpsSectionResponse.getSecIdentifier(), rpsSectionResponse.getScore());
                }
                candRespRawScore.setSectionScore(sectionScore);
                candRespRawScore.setLoginTime(rpsCandidateResponse.getRpsMasterAssociation().getLoginTime());
                candRespRawScore.setTestStartTime(rpsCandidateResponse.getRpsMasterAssociation().getTestStartTime());
                getLogger().debug("Candidate first id {} and score {}" + candRespRawScore.getCandidateId1(),
                        candRespRawScore.getTotalScore());
                respRawScores.add(candRespRawScore);
            }
            qSetRawEntity.setSetCode(candidateResponse.get(0).getRpsQuestionPaper().getQpCode());
            Integer qpId = paper.getQpId();
            qSetRawEntity.setQpId(qpId);
            qSetRawEntity.setTotalMarks(totalMarks);
            qSetRawEntity.setSectionMarks(sectionMarks);
            qSetRawEntity.setCandRespRawScores(respRawScores);
            qSetRawEntities.add(qSetRawEntity);
        }
        return qSetRawEntities;
    }

	private void getCandidateDetailsFromMIF(CandRespRawScore candRespRawScore, RpsCandidate candidate,
			Integer uniqueCandidateId) {
		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			candRespRawScore.setDob(candidate.getDob());
			candRespRawScore.setEmailId1(candidate.getEmailId1());
			candRespRawScore.setEmailId2(candidate.getEmailId2());
			candRespRawScore.setFirstName(candidate.getFirstName());
			candRespRawScore.setGender(candidate.getGender());
			candRespRawScore.setLastName(candidate.getLastName());
			candRespRawScore.setPhone1(candidate.getPhone1());
			candRespRawScore.setPhone2(candidate.getPhone2());
		} else {
			PersonalInfo pInfo = mifForm.getPersonalInfo();
			if (pInfo != null) {
				candRespRawScore.setFirstName(pInfo.getFirstName());
				candRespRawScore.setGender(pInfo.getGender());
				candRespRawScore.setLastName(pInfo.getLastName());

				if (pInfo.getDob() != null) {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd");
					try {
						Date date = formatter.parse(pInfo.getDob());
						Calendar c = Calendar.getInstance();
						c.setTime((date));
						candRespRawScore.setDob(c);
					} catch (ParseException e) {
						getLogger().error("ParseException while executing generatePdfForScoreReport...", e);
					}
				}
			}
			ContactInfo pAddress = mifForm.getPermanentAddress();
			if (pAddress != null) {
				candRespRawScore.setEmailId1(pAddress.getEmailId1());
				candRespRawScore.setEmailId2(pAddress.getEmailId2());
				candRespRawScore.setPhone1(pAddress.getMobile());
				candRespRawScore.setPhone2(pAddress.getMobile_o());
			}
		}
	}

    private ExportRawEntity getRawScore(RpsBatchAcsAssociation rpsBatchAcsAssociation, String assessmentCode, String qpCode) throws Exception {
        List<RpsCandidateResponse> candidateResponse =
                rpsCandidateResponseService.getCandidateResponseByAssessmentBatchAcsSetCodes(assessmentCode, rpsBatchAcsAssociation, qpCode);
        getLogger().debug("List of the candidate as per assessment and batch ={}");
        ExportRawEntity exportRawEntity = new ExportRawEntity();
        exportRawEntity.setSubjectCode(assessmentCode);
        HashSet<QSetRawEntity> qSetRawEntity = this.getCandidatesRawScore(candidateResponse);
        getLogger().debug("Row score of the candidate for the assessment ={} is ={}");
        exportRawEntity.setSetRawEntities(qSetRawEntity);
        return exportRawEntity;
    }

    @Transactional(readOnly = true)
    public ExportRawEntity getRawScore(String batchCode, String eventCode, String acsCode, String assessmentCode,
                                       String qpCode) {
        getLogger().debug(
                "Batch code ={},Event code ={},Acs code ={},Assessment code ={},Set code ={}" + batchCode + eventCode
                        + acsCode + assessmentCode + qpCode);
        ExportRawEntity exportRawEntity = new ExportRawEntity();
        try {
            RpsBatch batch = batchService.getBatchByBatchCodeAndEvent(batchCode, eventCode);
            getLogger().debug("Batch details ={}", batch.getBatchCode());
            RpsAcsServer acsServer = acsServerServices.getBatchbyAcsServerId(acsCode);
            getLogger().debug("Acs Server details ={}" + acsServer.getAcsServerId());

            RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.getAssociationByRpsBatchAndRpsAcsServer(batch, acsServer);
            if (rpsBatchAcsAssociation == null) {
                getLogger().warn("There is no batch acs combination available in RpsBatchAcsserverAssociation table--- for batchCode, AcsSercerCode--" + batchCode + " , " + acsCode);
                return null;
            }
            exportRawEntity =
                    this.getRawScore(rpsBatchAcsAssociation, assessmentCode, qpCode);
            getLogger().debug("Export entity details ={}" + exportRawEntity.getSubjectCode());
        } catch (Exception e) {
            getLogger().error("Exception occurred while processing raw score", e);
        }
        return exportRawEntity;
    }

    private QSet1OXEntity getResultsInCTTReportForResponseObjects(RpsCandidateResponse rpsCandidateResponses,
                                                                  boolean is10XReport) throws Exception {
        QSet1OXEntity qSet1OXEntity = new QSet1OXEntity();
        HashMap<Integer, String> rcQidToQuesIDMapping = new LinkedHashMap<>();
        HashMap<String, String> quesIDtoParentQuesIDMapping = new LinkedHashMap<>();
        HashMap<String, Short> quesToRpsQuestionMap = new LinkedHashMap<>();
        HashMap<String, String> quesIDtoSectionNameMapping = new LinkedHashMap<>();
        List<QuestionToTopicDO> quesIDtoSectionIdentifierMapping = new  ArrayList<>();    //g
        Double totalQPaper = 0.0;
        if (rpsCandidateResponses != null) {
            Set<String> questionIdSet= new LinkedHashSet<>();
            LinkedHashMap<String, String> questionAnswerList = new LinkedHashMap<String, String>();
            LinkedHashMap<String, RpsQuestion> questionDOMap = new LinkedHashMap<String, RpsQuestion>();
            RpsQuestionPaper questionPaper = rpsCandidateResponses.getRpsQuestionPaper();
            Map<String, RpsQuestion> questionLanguageMap = rpsCandidateResponseService.getQuestionLanguageShuffleSequenseForCandidate(rpsCandidateResponses.getResponse()
                    , rpsCandidateResponses.getShuffleSequence(),questionPaper.getRpsAssessment().getAssessmentCode());
            List<RpsQuestion> questionList = new ArrayList<>(questionLanguageMap.values());;
            if (questionList == null || questionList.isEmpty()) {
                getLogger().error("No questions found for the question paper::" + questionPaper.getQpCode());
            }
            // get section details
            quesIDtoSectionNameMapping = rpsQuestionService.findRpsQuestionToSecTitleByQpID(questionPaper.getQpId());
            quesIDtoSectionIdentifierMapping = rpsQuestionService.findRpsQuestionToSecDetailsByQpID(questionPaper.getQpId());  //g
             
         //  sorting questions id based on "section name" followed by "topic name" followed by "questionId"
            	Collections.sort(quesIDtoSectionIdentifierMapping, new Comparator<QuestionToTopicDO>() {
            	@Override
            	public int compare(QuestionToTopicDO q1, QuestionToTopicDO q2){
            		int comparision=0;
            		comparision = q1.getSecidentifier().compareTo(q2.getSecidentifier());
            		if(comparision==0){
            			comparision= q1.getTopic().compareTo(q2.getTopic());
            		}
            		if(comparision==0){
            			comparision=q1.getQuestid().compareTo(q2.getQuestid());
            		}
            		return comparision;
            	}
			});
            List<String> sortedQIds=new ArrayList<>();
            for(QuestionToTopicDO que:quesIDtoSectionIdentifierMapping){
                sortedQIds.add(que.getQuestid());
            }
            
            List<String> sortedKeys=new ArrayList(questionLanguageMap.keySet());
            Collections.sort(sortedKeys);
            for (String rpsQuestiId : sortedQIds) {
                RpsQuestion question= questionLanguageMap.get(rpsQuestiId);
				if (question.getQuestionType().equalsIgnoreCase(QuestionType.READING_COMPREHENSION.toString())
						|| question.getQuestionType().equalsIgnoreCase(QuestionType.SURVEY.toString())) {
                    if (rcQidToQuesIDMapping.get(question.getQid()) == null) {
                        //add the RC question to Map
                        rcQidToQuesIDMapping.put(question.getQid(), question.getQuestId());
                    }
                }
            }
            for (String rpsQuestiId : sortedQIds) {
                RpsQuestion question= questionLanguageMap.get(rpsQuestiId);
                if (question.getScore() != null)
                    totalQPaper += question.getScore();

                if (!is10XReport) {
					if (question.getQuestionType()
							.equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString())) {
                        if (!is10XReport)
                            questionDOMap.put(question.getQuestId(), question);
                        questionAnswerList.put(question.getQuestId(), question.getQans());
                        questionIdSet.add(question.getQuestId());
                        quesToRpsQuestionMap.put(question.getQuestId(), question.getchildQuestionSeqNumber());
                    }
                }

                //add only those questions which question type is other than READING_COMPREHENSION
				if (question.getQuestionType().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {
                    if (question.getParentqID() != null) {
                        //if MCQ is a child question
                        if (quesIDtoParentQuesIDMapping.get(question.getQuestId()) == null) {
                            //check if question to parent id mapping is added to DB, if not then add it
                            quesIDtoParentQuesIDMapping.put(question.getQuestId(), rcQidToQuesIDMapping.get(question.getParentqID().getQid()));
                        }
                    }
//                    if (!is10XReport)
                        questionDOMap.put(question.getQuestId(), question);
                    questionAnswerList.put(question.getQuestId(), question.getQans());
                    questionIdSet.add(question.getQuestId());
                    quesToRpsQuestionMap.put(question.getQuestId(), question.getchildQuestionSeqNumber());
                }

            }
//            if (!is10XReport)
                qSet1OXEntity.setQuestionDOMap(questionDOMap);
            qSet1OXEntity.setQuestionAnswerMap(questionAnswerList);
            qSet1OXEntity.setQuestionList(new ArrayList<>(questionIdSet));
            qSet1OXEntity.setQuesToParentQuesMap(quesIDtoParentQuesIDMapping);
            qSet1OXEntity.setQuesToRpsQuestionMap(quesToRpsQuestionMap);
            qSet1OXEntity.setQuesIDtoSectionNameMapping(quesIDtoSectionNameMapping);
            qSet1OXEntity.setSetCode(questionPaper.getSetCode());
            Set<CandReps1OX> CandReps1OXs = new HashSet<>();
            List<CandidateResponseEntity> candidateResponselist =
                    this.getCandidateResponseEntity(rpsCandidateResponses);
            if (is10XReport) {
                CandReps1OXs.add(this.getCandidate10X(candidateResponselist, new ArrayList<>(questionIdSet), rpsCandidateResponses));
            } else {
                CandReps1OXs.add(this.getCandidateABCDX(candidateResponselist, new ArrayList<>(questionIdSet), rpsCandidateResponses));
            }
            qSet1OXEntity.setCandReps(CandReps1OXs);
        }
        return qSet1OXEntity;
    }


    private CandReps1OX getCandidate10X(List<CandidateResponseEntity> candidateResponseEntitys,
                                        List<String> questionIdList, RpsCandidateResponse rpsCandidateResponse) {

        Map<String, String> questions = new HashMap<String, String>();
        for (String questionId : questionIdList) {
            questions.put(questionId, "X");
        }

        Map<String, CandidateResponseEntity> candidateResponseEntityMap = new HashMap<String, CandidateResponseEntity>();
        for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntitys) {
            candidateResponseEntityMap.put(candidateResponseEntity.getQuestionID(), candidateResponseEntity);
        }
        int count = 1, oddCount = 0, evenCount = 0;

        // CASE:: candidate has not attempted any questions at all
        for (String questionId : questionIdList) {

            if (candidateResponseEntityMap.get(questionId) != null) {
                CandidateResponseEntity candidateResponseEntity = candidateResponseEntityMap.get(questionId);


                if (candidateResponseEntity.getResponse() == null
                        || candidateResponseEntity.getResponse().isEmpty()) {
                    questions.put(candidateResponseEntity.getQuestionID(), "X");
                } else {
                    if (candidateResponseEntity.getScore() > 0) {
                        questions.put(candidateResponseEntity.getQuestionID(), "1");
                        if (count % 2 == 0) {
                            evenCount++;
                        } else {
                            ++oddCount;
                        }
                    } else {
                        questions.put(candidateResponseEntity.getQuestionID(), "0");

                    }
                }
            }
            ++count;

        }

        // Percentage calculation
        double percentage = 0.0;
        if (rpsCandidateResponse.getRpsQuestionPaper() != null) {
            percentage = (100.0 * (double) rpsCandidateResponse.getCandidateScore()) / (double) rpsCandidateResponse.getRpsQuestionPaper().getTotalScore();
        }
        CandReps1OX candReps1OX = new CandReps1OX();
        Integer uniqueCandidateId = rpsCandidateResponse.getRpsMasterAssociation().getUniqueCandidateId();
		RpsCandidate rpsCandidate =
                rpsMasterAssociationService.getCandidateForUniqueId(uniqueCandidateId);
        candReps1OX.setCandidateLoginID(rpsCandidate.getCandidateId1());

        candReps1OX.setBatchName(rpsCandidateResponse.getRpsMasterAssociation().getRpsBatchAcsAssociation().getRpsBatch().getBatchName());
//		candReps1OX.setCustomerName(rpsCandidate.getRpsEvent().getRpsDivision().getRpsCustomer().getCustomerName());
        String rpsCustomerName = rpsEventService.findCustomersByEventCode(rpsCandidate.getRpsEvent().getEventCode());
        if (rpsCustomerName != null)
            candReps1OX.setCustomerName(rpsCustomerName);
        else
            candReps1OX.setCustomerName(RpsConstants.NA);
        candReps1OX.setEventName(rpsCandidate.getRpsEvent().getEventName());
        
        MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			candReps1OX.setCandidateName(rpsCandidate.getFirstName() + " " + rpsCandidate.getLastName());
			candReps1OX.setFirstName(rpsCandidate.getFirstName());
			candReps1OX.setLastName(rpsCandidate.getLastName());
		} else {
			PersonalInfo personalinfo = mifForm.getPersonalInfo();
			if (personalinfo != null) {
				candReps1OX.setCandidateName(personalinfo.getFirstName() + " " + personalinfo.getLastName());
				candReps1OX.setFirstName(personalinfo.getFirstName());
				candReps1OX.setLastName(personalinfo.getLastName());
			} else {
				candReps1OX.setCandidateName(RpsConstants.NA);
				candReps1OX.setFirstName(RpsConstants.NA);
				candReps1OX.setLastName(RpsConstants.NA);
			}
		}
        candReps1OX.setTestCenterAcsName(rpsCandidateResponse.getRpsMasterAssociation().getRpsBatchAcsAssociation().getRpsAcsServer().getAcsServerName());
        candReps1OX.setUniqueCandidateID(uniqueCandidateId);
        candReps1OX.setTotal(evenCount + oddCount);
        candReps1OX.setQuestions(questions);
        candReps1OX.setEvenCount(evenCount);
        candReps1OX.setOddCount(oddCount);
        candReps1OX.setPercentage(Double.valueOf(decimalFormat.format(percentage)));
        candReps1OX.setQuestionCount(questions.size());
        if (rpsCandidateResponse.getRpsMasterAssociation().getLoginTime() != null)
            candReps1OX.setLoginTime(rpsCandidateResponse.getRpsMasterAssociation().getLoginTime());
        if (rpsCandidateResponse.getRpsMasterAssociation().getTestStartTime() != null)
            candReps1OX.setTestStartTime(rpsCandidateResponse.getRpsMasterAssociation().getTestStartTime());
        return candReps1OX;
    }

    private CandReps1OX getCandidateABCDX(List<CandidateResponseEntity> candidateResponseEntitys,
                                          List<String> questionIdList, RpsCandidateResponse rpsCandidateResponse) {

        Map<String, String> questions = new HashMap<String, String>();
        for (String questionId : questionIdList) {
            questions.put(questionId, "X");
        }

        Map<String, CandidateResponseEntity> candidateResponseEntityMap = new HashMap<String, CandidateResponseEntity>();
        for (CandidateResponseEntity candidateResponseEntity : candidateResponseEntitys) {
            candidateResponseEntityMap.put(candidateResponseEntity.getQuestionID(), candidateResponseEntity);
        }

        // CASE:: candidate has not attempted any questions at all
        for (String questionId : questionIdList) {

            if (candidateResponseEntityMap.get(questionId) != null) {
                CandidateResponseEntity candidateResponseEntity = candidateResponseEntityMap.get(questionId);


                if (candidateResponseEntity.getResponse() == null
                        || candidateResponseEntity.getResponse().isEmpty()) {
                    questions.put(candidateResponseEntity.getQuestionID(), "X");
                } else {
                    String candidateResponseString = null;
                    Map<String, String> choicesMap = candidateResponseEntity.getResponse();
                    if (choicesMap != null && !choicesMap.isEmpty()) {
                        Set<String> choiceOptions = choicesMap.keySet();
                        if (choiceOptions != null && !choiceOptions.isEmpty()) {
                            Iterator<String> chIt = choiceOptions.iterator();
                            candidateResponseString = choicesMap.get(chIt.next());
                        }
                    }
                    if (candidateResponseString != null && !candidateResponseString.isEmpty()) {
                        if (candidateResponseString.toLowerCase().contains("choice")) {
                            String abcd = ABCDXOptionsEnum.valueOf(candidateResponseString.toLowerCase()).getValue();
                            questions.put(candidateResponseEntity.getQuestionID(), abcd);
                        }
                    } else {
                        questions.put(candidateResponseEntity.getQuestionID(), "X");

                    }
                }
            }

        }

        CandReps1OX candReps1OX = new CandReps1OX();
        Integer uniqueCandidateId = rpsCandidateResponse.getRpsMasterAssociation()
		        .getUniqueCandidateId();
		RpsCandidate rpsCandidate =
                rpsMasterAssociationService.getCandidateForUniqueId(uniqueCandidateId);
        candReps1OX.setCandidateLoginID(rpsCandidate.getCandidateId1());
		// candReps1OX.setCandidateName(rpsCandidate.getFirstName() + " " + rpsCandidate.getLastName());
        candReps1OX.setBatchName(rpsCandidateResponse.getRpsMasterAssociation().getRpsBatchAcsAssociation().getRpsBatch().getBatchName());
        String rpsCustomerName = rpsEventService.findCustomersByEventCode(rpsCandidate.getRpsEvent().getEventCode());
        if (rpsCustomerName != null)
            candReps1OX.setCustomerName(rpsCustomerName);
        else
            candReps1OX.setCustomerName(RpsConstants.NA);
        candReps1OX.setEventName(rpsCandidate.getRpsEvent().getEventName());
		// candReps1OX.setFirstName(rpsCandidate.getFirstName());
		// candReps1OX.setLastName(rpsCandidate.getLastName());
		//

		MIFForm mifForm = null;
		RpsCandidateMIFDetails rpsCandidateMIFDetails =
				rpsCandidateMIFDetailsService.findByUniqueCandidateId(uniqueCandidateId);
		if (rpsCandidateMIFDetails != null && rpsCandidateMIFDetails.getMifFormJson() != null)
			mifForm = new Gson().fromJson(rpsCandidateMIFDetails.getMifFormJson(), MIFForm.class);
		if (mifForm == null) {
			candReps1OX.setCandidateName(rpsCandidate.getFirstName() + " " + rpsCandidate.getLastName());
			candReps1OX.setFirstName(rpsCandidate.getFirstName());
			candReps1OX.setLastName(rpsCandidate.getLastName());
		} else {
			PersonalInfo personalinfo = mifForm.getPersonalInfo();
			if (personalinfo != null) {
				candReps1OX.setCandidateName(personalinfo.getFirstName() + " " + personalinfo.getLastName());
				candReps1OX.setFirstName(personalinfo.getFirstName());
				candReps1OX.setLastName(personalinfo.getLastName());
			} else {
				candReps1OX.setCandidateName(RpsConstants.NA);
				candReps1OX.setFirstName(RpsConstants.NA);
				candReps1OX.setLastName(RpsConstants.NA);
			}
		}

		//
        candReps1OX.setTestCenterAcsName(rpsCandidateResponse.getRpsMasterAssociation().getRpsBatchAcsAssociation().getRpsAcsServer().getAcsServerName());
        candReps1OX.setUniqueCandidateID(uniqueCandidateId);
        candReps1OX.setQuestions(questions);
        if (rpsCandidateResponse.getRpsMasterAssociation().getLoginTime() != null)
            candReps1OX.setLoginTime(rpsCandidateResponse.getRpsMasterAssociation().getLoginTime());
        if (rpsCandidateResponse.getRpsMasterAssociation().getTestStartTime() != null)
            candReps1OX.setTestStartTime(rpsCandidateResponse.getRpsMasterAssociation().getTestStartTime());
        return candReps1OX;
    }

    private List<CandidateResponseEntity> getCandidateResponseEntity(RpsCandidateResponse rpsCandidateResponse) {
        if (rpsCandidateResponse.getResponse() == null || rpsCandidateResponse.getResponse().trim().isEmpty()) {
            return null;
        }
        Type type = new TypeToken<List<CandidateResponseEntity>>() {
        }.getType();
        List<CandidateResponseEntity> CandidateResponselist = gson.fromJson(rpsCandidateResponse.getResponse(), type);
        return CandidateResponselist;
    }

    private Export1OXEntity getCandidateResultsInCTTReport(RpsBatchAcsAssociation rpsBatchAcsAssociation, String assessmentCode,
                                                           String qpCode, boolean is10XReport) throws Exception {
        Export1OXEntity export1oxEntity = new Export1OXEntity();
        export1oxEntity.setSubjectCode(assessmentCode);
        HashSet<QSet1OXEntity> qSet1OXEntities = new HashSet<>();
        List<RpsCandidateResponse> candidateResponses =
                rpsCandidateResponseService.getCandidateResponseByAssessmentBatchAcsSetCodes(assessmentCode, rpsBatchAcsAssociation, qpCode);
        getLogger().debug("List of the candidate as per assessment and batch ={}");
        for (RpsCandidateResponse rpsCandidateResponses : candidateResponses) {
            QSet1OXEntity qSet1OXEntity = this.getResultsInCTTReportForResponseObjects(rpsCandidateResponses, is10XReport);
            getLogger().debug("QSet1OXEntity entity={}");
            qSet1OXEntity.setSetCode(qpCode);
            qSet1OXEntities.add(qSet1OXEntity);
            export1oxEntity.setQSet1OXEntities(qSet1OXEntities);
        }
        getLogger().debug("10X Export Entity ={}");
        return export1oxEntity;
    }

    public Export1OXEntity getCandidateResultsInCTTReport(String batchCode, String eventCode, String acsCode,
                                                          String assessmentCode, String qpCode, boolean is10XReport) {
        getLogger().debug(
                "Batch code ={},Event code ={}, acs server code ={}, Assessment code ={},set code ={}" + batchCode
                        + eventCode + acsCode + assessmentCode + qpCode);
        Export1OXEntity export1oxEntity = new Export1OXEntity();
        try {
            RpsBatch batch = batchService.getBatchByBatchCodeAndEvent(batchCode, eventCode);
            getLogger().debug("Batch details ={}");
            RpsAcsServer acsServer = acsServerServices.getBatchbyAcsServerId(acsCode);
            getLogger().debug("Acs server details ={}");
            getLogger().debug(batch.getBid() + "  " + acsServer.getAcsId() + "  " + assessmentCode + "  " + qpCode);
            RpsBatchAcsAssociation rpsBatchAcsAssociation = rpsBatchAcsAssociationService.getAssociationByRpsBatchAndRpsAcsServer(batch, acsServer);
            if (rpsBatchAcsAssociation == null) {
                getLogger().warn("There is no batch acs combination available in RpsBatchAcsserverAssociation table--- for batchCode, AcsSercerCode--" + batchCode + " , " + acsCode);
                return null;
            }
            export1oxEntity =
                    this.getCandidateResultsInCTTReport(rpsBatchAcsAssociation, assessmentCode, qpCode, is10XReport);
        } catch (Exception e) {
            getLogger().error("Exception occured while processing candidate 10X score ", e);
        }
        return export1oxEntity;
    }


    public List<ExportRawEntity> getCandidateRawScoreList(List<Parameters> parametersList) {

        if (parametersList == null || parametersList.isEmpty())
            return null;
        List<ExportRawEntity> exportEntities = new ArrayList<>();
        String batch = null;
        String event = null;
        String acs = null;
        String assessment = null;
        String set = null;
        Iterator<Parameters> it = parametersList.iterator();
        while (it.hasNext()) {
            ExportRawEntity entity = null;
            Parameters parameter = it.next();
            event = parameter.getEventCode();
            batch = parameter.getBatchCode();
            acs = parameter.getAcsCode();
            assessment = parameter.getAssessmentCode();
            set = parameter.getSetCode();
            _logger.info("calling getRawScore() method for batch, event, acs, assessment, set" + batch + " , " + event + " , " + acs + " , " + assessment + " , " + set);
            entity = this.getRawScore(batch, event, acs, assessment, set);
            exportEntities.add(entity);
        }
        _logger.info(gson.toJson(exportEntities));
        return exportEntities;

    }

    /**
     * @param
     * @return
     * @see
     * @since Apollo v2.0
     */
    public List<Export1OXEntity> getCandidatesResultsIn10X(List<Parameters> parametersList) {
        if (parametersList == null || parametersList.isEmpty())
            return null;
        List<Export1OXEntity> exportEntities = new ArrayList<>();
        String batch = null;
        String event = null;
        String acs = null;
        String assessment = null;
        String set = null;
        Iterator<Parameters> it = parametersList.iterator();
        while (it.hasNext()) {
            Export1OXEntity entity = null;
            Parameters parameter = it.next();
            event = parameter.getEventCode();
            batch = parameter.getBatchCode();
            acs = parameter.getAcsCode();
            assessment = parameter.getAssessmentCode();
            set = parameter.getSetCode();
            _logger.info("calling getCandidateResultsIn10X() method for batch, event, acs, assessment, set" + batch + " , " + event + " , " + acs + " , " + assessment + " , " + set);
            entity = this.getCandidateResultsInCTTReport(batch, event, acs, assessment, set, true);
            exportEntities.add(entity);
        }
        _logger.info(gson.toJson(exportEntities));
        return exportEntities;
    }

    @Transactional(readOnly = true)
    public Export1OXEntity generate1OXForAssessmentsAndSetsSingleAction(
            String batchCode2, String eventCode2, String acsCode2,
            String assessmentCode2, String qpCode) {

        Export1OXEntity export1OXEntity = this.getCandidateResultsInCTTReport(batchCode2, eventCode2, acsCode2, assessmentCode2, qpCode, true);
        HashSet<QSet1OXEntity> sets = export1OXEntity.getQSet1OXEntities();
        if (sets != null && !sets.isEmpty()) {
            Iterator<QSet1OXEntity> qSet1OXEntityIt = sets.iterator();
            while (qSet1OXEntityIt.hasNext()) {
                QSet1OXEntity qSet1OXEntity = qSet1OXEntityIt.next();
                Set<CandReps1OX> candResps = qSet1OXEntity.getCandReps();
                Map<String, Short> quesToRpsQuestionMap = qSet1OXEntity.getQuesToRpsQuestionMap();
                HashMap<String, String> quesToParentQuesMap = qSet1OXEntity.getQuesToParentQuesMap();
                if (candResps != null && !candResps.isEmpty()) {
                    Iterator<CandReps1OX> respIt = candResps.iterator();
                    while (respIt.hasNext()) {
                        CandReps1OX candReps1OX = respIt.next();
                        Map<String, String> tempQuesRespMap = candReps1OX.getQuestions();
                        Map<String, String> quesRespMap = new TreeMap<String, String>();
                        Set<String> questionSet = tempQuesRespMap.keySet();
                        Iterator<String> it = questionSet.iterator();
                        String questId = "";
                        while (it.hasNext()) {
                            questId = it.next();
                            String questionId = questId;
                            String parentQuesId = quesToParentQuesMap.get(questId);
                            if (parentQuesId != null) {
                                questionId = parentQuesId + "." + quesToRpsQuestionMap.get(questId) + "(" + questId + ")";
                            }
                            quesRespMap.put(questionId, tempQuesRespMap.get(questId));
                        }
                        candReps1OX.setQuestions(quesRespMap);
                    }
                }
            }
        }
        return export1OXEntity;
    }

    public Export1OXEntity getCandidateResultsInCTTReport(Set<Integer> uniqueCandidateIds, String assessmentCode, Integer qpId,
                                                          boolean is10XReport) {
        Export1OXEntity export1oxEntity = new Export1OXEntity();
        String subjectCode = assessmentCode;
        RpsQuestionPaper rpsQuestionPaper = null;
        if (qpId != null&& qpId!=0)
            rpsQuestionPaper = rpsQuestionPaperService.findOne(qpId);
        else
            rpsQuestionPaper = rpsQuestionPaperService.getAnyQPForAssessmentCode(assessmentCode);
        if (rpsQuestionPaper != null) {
            subjectCode = rpsQuestionPaper.getRpsAssessment().getAssessmentName();
        }
        export1oxEntity.setSubjectCode(subjectCode);
        HashSet<QSet1OXEntity> qSet1OXEntities = new HashSet<>();
        List<RpsCandidateResponse> candidateResponses = null;
        if (qpId != null&& qpId!=0)
            candidateResponses = rpsCandidateResponseService.findAllCandRespByMatserAssnIdsAndQpId(uniqueCandidateIds, qpId);
        else
            candidateResponses = rpsCandidateResponseService.findAllCandRespByMatserAssnIds(uniqueCandidateIds);

        getLogger().debug("List of the candidate as per assessment and batch ={}",uniqueCandidateIds);
        try {
            for (RpsCandidateResponse rpsCandidateResponses : candidateResponses) {
                QSet1OXEntity qSet1OXEntity = this.getResultsInCTTReportForResponseObjects(rpsCandidateResponses, is10XReport);
                getLogger().debug("QSet1OXEntity entity={}");
                qSet1OXEntities.add(qSet1OXEntity);
            }
            export1oxEntity.setQSet1OXEntities(qSet1OXEntities);
            getLogger().debug("10X Export Entity ={}");
        } catch (Exception e) {
            getLogger().error("Exception occured while processing candidate 10X score ", e);
        }
        return export1oxEntity;
    }

}
