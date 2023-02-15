package com.merittrac.apollo.rps.services;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.CandidateResponseEntity;
import com.merittrac.apollo.data.entity.RpsCandidateResponse;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.service.RpsCandidateResponseService;
import com.merittrac.apollo.data.service.RpsQuestionService;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.ui.entity.ShuffledCandidateResponses;

public class QShuffleSequenceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(QShuffleSequenceService.class);

    @Autowired
    private RpsCandidateResponseService rpsCandidateResponseService;

    @Autowired
    private RpsQuestionService rpsQuestionService;

    @Autowired
    private Gson gson;

    @Value("${totalRCRawScoreMarks}")
    private String isTotalRCRawScoreMarks;

    final String EMPTY = "";
    final String CHOICE = "CHOICE";

    @Transactional(readOnly = true)
    public List<ShuffledCandidateResponses> getCandidateResponsesByShuffleSeq(Integer uniqueCandId, Integer qPaperId,
                                                                              String assessmentCode) throws RpsException, ParseException {
        LOGGER.debug("------- getCandidateResponsesByShuffleSeq-------IN--");
        List<ShuffledCandidateResponses> shuffledCandidateResponsesList = new ArrayList<>();
        Map<String, String> shuffleMap = new LinkedHashMap<>();
        Map<String, String> quesToQTypeMapping = null;
        Map<String, CandidateResponseEntity> quesToCandRespMapping = null;
        Map<String, List<String>> optionShuffleMap = null;
        RpsCandidateResponse candResp = rpsCandidateResponseService.getByCandidateUniqueIdAndQpId(uniqueCandId, qPaperId);

        if (candResp == null) {
            LOGGER.warn("There are no candidate response in rps_candidate_response table for this candidate");
            return shuffledCandidateResponsesList;
        }
        //get the shuffle map
        String shuffleSeq = candResp.getShuffleSequence();
        if (shuffleSeq == null || shuffleSeq.isEmpty()) {
            LOGGER.warn("Shuffle sequence for the candidate is empty or null-- ");
            return shuffledCandidateResponsesList;
        }
        Map<String, RpsQuestion> questionMap = rpsCandidateResponseService.getQuestionLanguageShuffleSequenseForCandidate(candResp.getResponse()
                , candResp.getShuffleSequence(), assessmentCode);
        List<RpsQuestion> questionsList = new ArrayList<>(questionMap.values());;
        shuffleMap = getQShuffleMappingForCandAndQPaper(shuffleSeq, assessmentCode, questionMap);
        logShuffleSeqMap(shuffleMap);
        String optionShuffleSeq = candResp.getOptionShuffleSequence();
        optionShuffleMap = getOptionShuffleMap(optionShuffleSeq);
        Collection<String> questions = shuffleMap.values();
        LOGGER.info("questions list in the shuffle sequence is :" + questions);


        if (questionsList == null || questionsList.isEmpty())
            throw new RpsException("list of questions in shuffle sequence are not available in DB");
        //get question Type mapping with each question
        quesToQTypeMapping = getQuesToQTypeMapping(questionsList);
        quesToCandRespMapping = getQuesToCandRespMapping(candResp);
      
        //get shuffle candidate responses
        shuffledCandidateResponsesList = getShuffledCandidateResponsesList(shuffleMap, quesToQTypeMapping, quesToCandRespMapping, optionShuffleMap);
        LOGGER.debug("------- getCandidateResponsesByShuffleSeq-------OUT--");
        return shuffledCandidateResponsesList;

    }

    private Map<String, List<String>> getOptionShuffleMap(
            String optionShuffleSeq) {

        Map<String, List<String>> OptionShuffleMap = null;
        if (optionShuffleSeq != null && !optionShuffleSeq.isEmpty()) {
            Type mapType1 = new TypeToken<Map<String, List<String>>>() {
            }.getType();

            OptionShuffleMap = gson.fromJson(optionShuffleSeq, mapType1);
        }
        return OptionShuffleMap;
    }

    private void logShuffleSeqMap(Map<String, String> shuffleMap) {
        if (shuffleMap == null || shuffleMap.isEmpty()) {
            LOGGER.error("Shuffle Sequence Map is empty");
        } else {
            Set<String> seqs = shuffleMap.keySet();
            Iterator<String> seqIt = seqs.iterator();
            LOGGER.info("Shuffle Sequence of questions as rendered in TP is----");
            while (seqIt.hasNext()) {
                String seq = seqIt.next();
                LOGGER.info("Questions Shuffle Sequence : " + seq + " and Question ID : " + shuffleMap.get(seq));
            }
        }
    }

    private List<ShuffledCandidateResponses> getShuffledCandidateResponsesList(
            Map<String, String> shuffleMap,
            Map<String, String> quesToQTypeMapping,
            Map<String, CandidateResponseEntity> quesToCandRespMapping, Map<String, List<String>> optionShuffleMap) {
        LOGGER.debug("------- getShuffledCandidateResponsesList-------IN--");
        List<ShuffledCandidateResponses> shuffledCandidateResponsesList = new ArrayList<>();
        Set<String> sequences = shuffleMap.keySet();
        Iterator<String> seqIt = sequences.iterator();
        while (seqIt.hasNext()) {
            ShuffledCandidateResponses shuffleResp = new ShuffledCandidateResponses();
            String seqNumber = seqIt.next();
            shuffleResp.setShuffleSeq(seqNumber);
            String questId = shuffleMap.get(seqNumber);
            String questType = quesToQTypeMapping.get(questId);
            shuffleResp.setQuestionType(questType);
            String candChoice = EMPTY;
            double candMarks = 0.0;
            if (quesToCandRespMapping != null && !quesToCandRespMapping.isEmpty()) {
                CandidateResponseEntity respEntity = quesToCandRespMapping.get(questId);
                if (respEntity != null) {
                    Map<String, String> choicesMap = respEntity.getResponse();
                    if (choicesMap != null && !choicesMap.isEmpty()) {
                        Set<String> choiceOptions = choicesMap.keySet();
                        if (choiceOptions != null && !choiceOptions.isEmpty()) {
                            Iterator<String> chIt = choiceOptions.iterator();
                            candChoice = choicesMap.get(chIt.next());
                        }
                    }

                    candMarks = respEntity.getScore();
                }
            }

            String userSelChoice = candChoice;
			if (questType.equalsIgnoreCase(QuestionType.DESCRIPTIVE_QUESTION.toString())) {
                //escape all the html tags from candidate response
                userSelChoice = StringEscapeUtils.unescapeHtml(userSelChoice);
            } else if (optionShuffleMap != null && !optionShuffleMap.isEmpty())
                userSelChoice = getUserSelectedChoice(questId, optionShuffleMap, candChoice);
            shuffleResp.setCandidateResponse(userSelChoice);
            if (!
			(questType.equalsIgnoreCase(QuestionType.READING_COMPREHENSION.toString())
					|| questType.equalsIgnoreCase(QuestionType.SURVEY.toString())))
                shuffleResp.setMarks("" + candMarks);
            else {
                if (isTotalRCRawScoreMarks.equalsIgnoreCase("YES"))
                    shuffleResp.setMarks("" + candMarks);
                else
                    shuffleResp.setMarks("");
            }
            shuffledCandidateResponsesList.add(shuffleResp);
        }
        LOGGER.debug("------- getShuffledCandidateResponsesList-------OUT--");
        // Collections.sort(shuffledCandidateResponsesList);
        return shuffledCandidateResponsesList;
    }

    private String getUserSelectedChoice(String questId,
                                         Map<String, List<String>> optionShuffleMap, String candChoice) {

        String userSelChoice = candChoice;

        List<String> optionsList = optionShuffleMap.get(questId);
        if (optionsList != null && !optionsList.isEmpty()) {
            candChoice = candChoice.toUpperCase();
            candChoice = candChoice.replaceAll("\\s+", "");
            candChoice = candChoice.replaceAll(CHOICE, "");
            int index = optionsList.indexOf(candChoice);
            if (index >= 0) {
                index = index + 1;
                userSelChoice = CHOICE + index;
            }

        }
        return userSelChoice;
    }


    public List<String> getQShuffleMappingForCandAndQPaper_V2(Map<String, RpsQuestion> questionMap, String shuffleSeq, String assessmentCode) throws ParseException, RpsException {
        LOGGER.debug("------- getQShuffleMappingForCandAndQPaper-------IN--");
        List<String> shuffleSeqQuestIdList = new LinkedList<String>();
        Type mapType1 = new TypeToken<Map<String, List<String>>>() {
        }.getType();

        Map<String, List<String>> section = gson.fromJson(shuffleSeq, mapType1);
        Set<String> keys = section.keySet();
        LOGGER.info("value of Section:" + keys.size());
        for (Iterator<String> iterator = keys.iterator(); iterator.hasNext(); ) {
            String sectionKey = (String) iterator.next();

            List<String> quesSequences = section.get(sectionKey);
            parseShuffleSequence_V2(shuffleSeqQuestIdList, quesSequences, assessmentCode, questionMap);
        }
        LOGGER.debug("------- getQShuffleMappingForCandAndQPaper-------OUT--");
        return shuffleSeqQuestIdList;
    }

    private void parseShuffleSequence_V2(List<String> shuffleSeqQuestIdList, List<String> quesSequences, String assessmentCode, Map<String, RpsQuestion> questionMap) {
        LOGGER.debug("------- parseShuffleSequence-------IN--");
        Integer seqNumber = 1;
        //update seqNumber to current seqNumber when map already has elements added to it
        if (shuffleSeqQuestIdList != null)
            seqNumber += shuffleSeqQuestIdList.size();

        if (quesSequences != null && !quesSequences.isEmpty()) {

            for (String seq : quesSequences) {
                //RpsQuestion question = questionMap.get(seq);
                //if(question!=null && question.getParentqID() == null) {
                shuffleSeqQuestIdList.add(seq);
                /*}
                else {
					List<RpsQuestion> rpsChildQuestionList = rpsQuestionService.findRpsQuestionDetailsByParentID(question.getQid());
					shuffleSeqQuestIdList.add(rpsChildQuestionList.get(0).getQuestId());
				}*/
            }
        }
    }

    public Map<String, String> getQShuffleMappingForCandAndQPaper(String shuffleSeq, String assessmentCode, Map<String, RpsQuestion> questionLanguageMap) throws ParseException, RpsException {
        LOGGER.debug("------- getQShuffleMappingForCandAndQPaper-------IN--");
        Map<String, String> shuffleSeqQuestIdMapping = new LinkedHashMap<String, String>();
        Type mapType1 = new TypeToken<Map<String, List<String>>>() {
        }.getType();

        Map<String, List<String>> section = gson.fromJson(shuffleSeq, mapType1);
        Set<String> keys = section.keySet();
        List<String> keyList = new ArrayList<>(keys);
        Collections.sort(keyList);
        for (String sectionKey : keyList) {
            //String sectionKey = (String) iterator.next();
            LOGGER.info("value of Section:" + sectionKey + ":" + section.get(sectionKey));
            List<String> quesSequences = section.get(sectionKey);
            parseShuffleSequence(shuffleSeqQuestIdMapping, quesSequences, assessmentCode, questionLanguageMap);
        }

        LOGGER.debug("------- getQShuffleMappingForCandAndQPaper-------OUT--");
        return shuffleSeqQuestIdMapping;
    }

    private void parseShuffleSequence(Map<String, String> shuffleSeqQuestIdMapping, List<String> quesSequences, String assessmentCode, Map<String, RpsQuestion> questionLanguageMap) {
        LOGGER.debug("------- parseShuffleSequence-------IN--");
        Integer seqNumber = 1;
        //update seqNumber to current seqNumber when map already has elements added to it
        if (shuffleSeqQuestIdMapping != null && !shuffleSeqQuestIdMapping.isEmpty()) {
            Set<String> s = shuffleSeqQuestIdMapping.keySet();
            Iterator<String> iter = s.iterator();
            while (iter.hasNext()) {
                if (!iter.next().contains("."))
                    seqNumber++;
            }
        }
        if (quesSequences != null && !quesSequences.isEmpty()) {
            for (String seq : quesSequences) {
                RpsQuestion rpsQuestion =  questionLanguageMap.get(seq);
                shuffleSeqQuestIdMapping.put(Integer.toString(seqNumber), seq);
                if (rpsQuestion != null && rpsQuestion.getParentqID() != null) {
                    shuffleSeqQuestIdMapping.put(Integer.toString(seqNumber), rpsQuestion.getParentqID().getQuestId());
                    shuffleSeqQuestIdMapping.put(Integer.toString(seqNumber) + "." + rpsQuestion.getchildQuestionSeqNumber(), rpsQuestion.getQuestId());
                }
				if (rpsQuestion != null && rpsQuestion.getQuestionType()
						.equalsIgnoreCase(QuestionType.READING_COMPREHENSION.toString())) {
                    List<RpsQuestion> rpsChildQuestionList = rpsQuestionService.findRpsQuestionDetailsByParentIDByLanguage(rpsQuestion.getQid(), null);
                    Map<Short, RpsQuestion> childShuffleSeq = this.getChildShuffledSeq(rpsChildQuestionList);
                    if (childShuffleSeq != null && !childShuffleSeq.isEmpty()) {
                        Iterator<Short> it = childShuffleSeq.keySet().iterator();
                        while (it.hasNext()) {
                            Short seqNum = it.next();
                            RpsQuestion rpsChildQuestion = childShuffleSeq.get(seqNum);
                            shuffleSeqQuestIdMapping.put(Integer.toString(seqNumber) + "." + rpsChildQuestion.getchildQuestionSeqNumber(), rpsChildQuestion.getQuestId());
                        }
                    }

                }
				if (rpsQuestion != null
						&& rpsQuestion.getQuestionType().equalsIgnoreCase(QuestionType.SURVEY.toString())) {
                    List<RpsQuestion> rpsChildQuestionList = rpsQuestionService.findRpsQuestionDetailsByParentIDByLanguage(rpsQuestion.getQid(), null);
                    Map<Short, RpsQuestion> childShuffleSeq = this.getChildShuffledSeq(rpsChildQuestionList);
                    if (childShuffleSeq != null && !childShuffleSeq.isEmpty()) {
                        Iterator<Short> it = childShuffleSeq.keySet().iterator();
                        while (it.hasNext()) {
                            Short seqNum = it.next();
                            RpsQuestion rpsChildQuestion = childShuffleSeq.get(seqNum);
                            shuffleSeqQuestIdMapping.put(Integer.toString(seqNumber) + "." + rpsChildQuestion.getchildQuestionSeqNumber(), rpsChildQuestion.getQuestId());
                        }
                    }

                }

                seqNumber++;
            }
        }
        LOGGER.debug("------- parseShuffleSequence-------IN--");
    }


    private Map<Short, RpsQuestion> getChildShuffledSeq(
            List<RpsQuestion> rpsChildQuestionList) {
        Map<Short, RpsQuestion> childShuffleSeq = new LinkedHashMap<>();
        if (rpsChildQuestionList != null && rpsChildQuestionList != null) {
            Iterator<RpsQuestion> it = rpsChildQuestionList.iterator();
            while (it.hasNext()) {
                RpsQuestion question = it.next();
                childShuffleSeq.put(question.getchildQuestionSeqNumber(), question);
            }
        }
        return childShuffleSeq;
    }

    private Map<String, CandidateResponseEntity> getQuesToCandRespMapping(RpsCandidateResponse candResp) {
        LOGGER.debug("------- getQuesToCandRespMapping-------IN--");
        Map<String, CandidateResponseEntity> quesToCandRespMapping = new HashMap<>();
        String responseJson = candResp.getResponse();
        LOGGER.info("response json is ---" + responseJson);
        Type type = new TypeToken<List<CandidateResponseEntity>>() {
        }.getType();
        List<CandidateResponseEntity> candidateResponseEntityList = gson.fromJson(responseJson, type);
        if (candidateResponseEntityList == null || candidateResponseEntityList.isEmpty())
            return null;
        Iterator<CandidateResponseEntity> candRespEntityIt = candidateResponseEntityList.iterator();
        while (candRespEntityIt.hasNext()) {
            CandidateResponseEntity candidateResponseEntity = candRespEntityIt.next();
            quesToCandRespMapping.put(candidateResponseEntity.getQuestionID(), candidateResponseEntity);

        }
        LOGGER.debug("------- getQuesToCandRespMapping-------OUT--");
        return quesToCandRespMapping;
    }

    private Map<String, String> getQuesToQTypeMapping(
            List<RpsQuestion> questionsList) throws RpsException {
        LOGGER.debug("------- getQuesToQTypeMapping-------IN--");
        if (questionsList == null || questionsList.isEmpty()) {
            throw new RpsException("question list is empty");
        }
        Map<String, String> quesToQTypeMapping = new HashMap<String, String>();
        Iterator<RpsQuestion> quesit = questionsList.iterator();
        while (quesit.hasNext()) {
            RpsQuestion rpsQuestion = quesit.next();
            quesToQTypeMapping.put(rpsQuestion.getQuestId(), rpsQuestion.getQuestionType());
        }
        LOGGER.debug("------- getQuesToQTypeMapping-------OUT--");
        return quesToQTypeMapping;
    }


}
