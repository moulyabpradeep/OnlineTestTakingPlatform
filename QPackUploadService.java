package com.merittrac.apollo.rps.services;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.qtitools.qti.node.item.AssessmentItem;
import org.qtitools.qti.node.test.AssessmentItemRef;
import org.qtitools.qti.node.test.AssessmentSection;
import org.qtitools.qti.node.test.AssessmentTest;
import org.qtitools.qti.node.test.SectionPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.entities.acs.ResponseMarkBean;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBrLr;
import com.merittrac.apollo.data.entity.RpsCustomer;
import com.merittrac.apollo.data.entity.RpsDivision;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsQpSection;
import com.merittrac.apollo.data.entity.RpsQpTemplate;
import com.merittrac.apollo.data.entity.RpsQuestion;
import com.merittrac.apollo.data.entity.RpsQuestionAssociation;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.entity.RpsQuestionPaperPack;
import com.merittrac.apollo.data.repository.RpsAssessmentRepository;
import com.merittrac.apollo.data.repository.RpsBrLrRepository;
import com.merittrac.apollo.data.repository.RpsCustomerRepository;
import com.merittrac.apollo.data.repository.RpsDivisionRepository;
import com.merittrac.apollo.data.repository.RpsEventRepository;
import com.merittrac.apollo.data.repository.RpsQpSectionRepository;
import com.merittrac.apollo.data.repository.RpsQpTemplateRepository;
import com.merittrac.apollo.data.repository.RpsQuestionAssociationRepository;
import com.merittrac.apollo.data.repository.RpsQuestionPaperPackRepository;
import com.merittrac.apollo.data.repository.RpsQuestionPaperRepository;
import com.merittrac.apollo.data.repository.RpsQuestionRepository;
import com.merittrac.apollo.data.service.RpsOptionService;
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.LayoutRulesExportEntity;
import com.merittrac.apollo.qpdrpsentities.OptionMetadata;
import com.merittrac.apollo.qpdrpsentities.QpdRpsExportEntity;
import com.merittrac.apollo.qpdrpsentities.QuestionInfo;
import com.merittrac.apollo.qpdrpsentities.QuestionMetadata;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.jqti.dao.JQTIQuestionEntity;
import com.merittrac.apollo.rps.jqti.utility.JQTIApolloUtility;
import com.merittrac.apollo.rps.jqti.utility.JQTIUtility;

/**
 * Created with IntelliJ IDEA.
 * User: Ajit_K
 * Date: 3/7/14
 * Time: 11:00 PM
 */
public class QPackUploadService {

    protected static final Logger logger = LoggerFactory.getLogger(QPPackParser.class);

    @Autowired
    private RpsEventRepository rpsEventRepository;

    @Autowired
    private RpsQuestionAssociationRepository rpsQuestionAssociationRepository;

    @Autowired
    private RpsQpTemplateRepository rpsQpTemplateRepository;

    @Autowired
    private RpsBrLrRepository rpsBrLrRepository;

    @Autowired
    private RpsCustomerRepository rpsCustomerRepository;

    @Autowired
    private RpsDivisionRepository rpsDivisionRepository;

    @Autowired
    private RpsAssessmentRepository rpsAssessmentRepository;

    @Autowired
    private RpsQuestionRepository rpsQuestionRepository;

    @Autowired
    private RpsQuestionPaperRepository rpsQuestionPaperRepository;

    @Autowired
    private RpsQuestionPaperPackRepository rpsQuestionPaperPackRepository;

    @Autowired
    private RpsQpSectionRepository rpsQpSectionRepository;

    @Autowired
    private JQTIUtility jqtiUtility;

    @Autowired
    private JQTIApolloUtility jqtiApolloUtility;

    @Autowired
    RpsOptionService rpsOptionService;

    private static final String setCode = "set";

    protected RpsQuestionPaper setQuestionPaperInfo(AssessmentTest assessmentTest, RpsQuestionPaper rpsQuestionPaper, Map<String, QuestionInfo> questionMap) {

        List<AssessmentSection> assessmentSections = jqtiUtility.getListOfSection(assessmentTest);
        String questionPaperCode = assessmentTest.getIdentifier();

        rpsQuestionPaper.setNumOfSections(assessmentSections.size());
        rpsQuestionPaper.setQpCode(questionPaperCode);
        rpsQuestionPaper.setCreationDate(Calendar.getInstance().getTime());
        rpsQuestionPaper.setQpTitle(assessmentTest.getTitle());
        rpsQuestionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.AVAILABLE.toString());
        int setIndex = questionPaperCode.lastIndexOf(setCode);
        String setCode = questionPaperCode.substring(setIndex, questionPaperCode.length());
        rpsQuestionPaper.setSetCode(setCode);
        Integer totalNoQuestion = 0;
        Integer totaldescQuestion = 0;
        Iterator<AssessmentSection> itAssmntPerSecForQNo = assessmentSections.iterator();

        while (itAssmntPerSecForQNo.hasNext()) {
            AssessmentSection ass_sec = itAssmntPerSecForQNo.next();
            List<SectionPart> sec_part = ass_sec.getSectionParts();
            for (SectionPart sectionPart : sec_part) {
                String questionlabel = sectionPart.getIdentifier();
                QuestionInfo questionInfo = questionMap.get(questionlabel);
                totalNoQuestion++;
                if (questionInfo.getqMetadata().getQuestionType().toString().equals(QuestionType.READING_COMPREHENSION.toString())) {
                    totalNoQuestion = totalNoQuestion + questionInfo.getChildQuestionInfo().size();
                    //Excluding the parent from the total question count.
                    totalNoQuestion = totalNoQuestion - 1;
                }
                if (questionInfo.getqMetadata().getQuestionType().toString().equals(QuestionType.SURVEY.toString())) {

                    totalNoQuestion = totalNoQuestion + questionInfo.getChildQuestionInfo().size();
                    //Excluding the parent from the total question count.
                    totalNoQuestion = totalNoQuestion - 1;

                }
                if (questionInfo.getqMetadata().getQuestionType().toString().equals(QuestionType.DESCRIPTIVE_QUESTION.toString())) {
                    totaldescQuestion++;
                }
            }
        }
        rpsQuestionPaper.setTotalDescriptiveQuestions(totaldescQuestion);
        rpsQuestionPaper.setTotalQuestions(totalNoQuestion);
        rpsQuestionPaper.setIsAnswerKeyAvailable(false);
        return rpsQuestionPaper;
    }


    protected Map<String, Map<String, RpsQuestion>> updateQuestionsforQP(Map<String, QuestionInfo> questions, RpsQuestionPaper rpsQuestionPaper,
                                                                         RpsAssessment rpsAssessment, String questionFileFolder, String packFileDownloadPath) throws RpsException {


        Map<String, Map<String, RpsQuestion>> languageQuestionMap = new HashMap<>();
        Set<Map.Entry<String, QuestionInfo>> questionsEntrySet = questions.entrySet();
        Iterator<Map.Entry<String, QuestionInfo>> questionsEntrySetIterator = questionsEntrySet.iterator();

        while (questionsEntrySetIterator.hasNext()) {
            Map.Entry<String, QuestionInfo> questionsEntry = questionsEntrySetIterator.next();
            QuestionInfo questionInfo = questionsEntry.getValue();
            String questionXmlPath = null;
            String questionImagePath = null;
            Map<String, String> questionXmlImagePath = new HashMap<>();
            Map<String, String> languageXml = new HashMap<>();
            QuestionMetadata questionMetaData = questionInfo.getqMetadata();
            if (questionInfo.getqMetadata().getQuestionLanguages() != null) {
                /**Adding Question Language level xma and language map*/
                List<String> questionLanguage = questionMetaData.getQuestionLanguages();
                for (String languageName : questionLanguage) {
                    questionXmlPath = questionFileFolder + File.separator + languageName + File.separator + questionMetaData.getQuestionLabel() + ".xml";
                    questionImagePath = questionFileFolder + File.separator + languageName + File.separator + RpsConstants.resources + File.separator + RpsConstants.images;
                    questionXmlImagePath.put(questionXmlPath, questionImagePath);
                    languageXml.put(questionXmlPath, languageName);
                }
            } else {
                /**For Qpt without language */
                questionXmlPath = questionFileFolder + File.separator + questionMetaData.getQuestionLabel() + ".xml";
                questionImagePath = FilenameUtils.getFullPath(packFileDownloadPath) + File.separator + "images";
                questionXmlImagePath.put(questionXmlPath, questionImagePath);
            }

            for (Map.Entry<String, String> entry : questionXmlImagePath.entrySet()) {
                questionXmlPath = entry.getKey();
                questionImagePath = entry.getValue();
                RpsQuestion rpsQuestion = null;
                /**Creating question based on Language */
                if (languageXml.containsKey(questionXmlPath)) {
                    rpsQuestion = rpsQuestionRepository.
                            findByQuesIDAndLanguageAndAssessmentCode(questionMetaData.getQuestionLabel(), rpsAssessment.getAssessmentCode(), languageXml.get(questionXmlPath));
                    if (rpsQuestion == null) {
                        rpsQuestion = new RpsQuestion();
                        rpsQuestion.setLanguage(languageXml.get(questionXmlPath));
                        rpsQuestion.setQuestId(rpsQuestion.getQuestId());
                    }
                } else {
                    /** Creating question based witout language */
                    rpsQuestion = rpsQuestionRepository.
                            findByQuesIDAndAssessmentCode(questionMetaData.getQuestionLabel(), rpsAssessment.getAssessmentCode());
                    if (rpsQuestion == null)
                        rpsQuestion = new RpsQuestion();
                }
                rpsQuestion.setQuestId(questionMetaData.getQuestionLabel());
                rpsQuestion.setQuestionType(questionMetaData.getQuestionType().toString());
                //AssessmentItem assessmentItem = null;
                if (questionMetaData.getQuestionType().toString().equals(QuestionType.DESCRIPTIVE_QUESTION.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.WRITTEN_ENGLISH_TEST.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.TYPING_TEST.toString())) {
                    AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(FilenameUtils.separatorsToSystem(questionXmlPath));
                    JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getDescriptiveQuestionInfo(assessmentItem, questionFileFolder, new File(questionImagePath));
                    rpsQuestion.setQtext(jqtiQuestionEntity.getText());
                    rpsQuestion.setQans(jqtiQuestionEntity.getResponse());
                } else if (questionMetaData.getQuestionType().toString().equals(QuestionType.DATA_ENTRY.toString())) {
                    AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(FilenameUtils.separatorsToSystem
                            (questionXmlPath));
                    JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getDataEntryQuestionInfo(assessmentItem,
                            questionFileFolder, new File(questionImagePath));
                    rpsQuestion.setScore(jqtiQuestionEntity.getMarks());
					rpsQuestion.setNegativeScore(questionMetaData.getNegativeScore());
                    rpsQuestion.setQans(jqtiQuestionEntity.getResponse());
                    rpsQuestion.setQtext(jqtiQuestionEntity.getText());
                } else if (questionMetaData.getQuestionType().toString().equals(QuestionType.FILL_IN_THE_BLANK.toString())) {
                    AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(FilenameUtils.separatorsToSystem
                            (questionXmlPath));
                    JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getFillInTheBlankQuestionInfo(assessmentItem,
                            questionFileFolder, new File(questionImagePath));
                    rpsQuestion.setScore(jqtiQuestionEntity.getMarks());
					rpsQuestion.setNegativeScore(questionMetaData.getNegativeScore());
                    rpsQuestion.setQans(jqtiQuestionEntity.getResponse());
                    rpsQuestion.setQtext(jqtiQuestionEntity.getText());
                } else if (questionMetaData.getQuestionType().toString().equals(QuestionType.MULTIPLE_CHOICE_QUESTION_WEIGHTED_OPTION.toString())) {
                    Double maxScore = null;
                    List<OptionMetadata> optionMetadatas = questionMetaData.getOptionMetadatas();
                    List<Double> scoresForMax = new ArrayList<>();
                    List<ResponseMarkBean> responseMarkBeans = new ArrayList<>();
                    if (optionMetadatas != null) {
                        for (OptionMetadata qMetadata : optionMetadatas) {
                            ResponseMarkBean responseMarkBean = new ResponseMarkBean();
                            responseMarkBean.setResponse(qMetadata.getOptionIdentifier());
                            responseMarkBean.setResponseAnswer(RpsConstants.NA);
                            responseMarkBean.setResponsePositiveMarks(qMetadata.getMarks());
                            responseMarkBean.setResponseNegativeMarks(qMetadata.getNegativeMarks());
                            responseMarkBean.setCaseSensitive(false);
                            scoresForMax.add(qMetadata.getMarks());
                            responseMarkBeans.add(responseMarkBean);
                        }
                        maxScore = Collections.max(scoresForMax);
                    } else
                        logger.error("there are no MCQW options in question id=" + questionMetaData.getQuestionId());

                    String responseMarkBeansJson = new Gson().toJson(responseMarkBeans);
                    rpsQuestion.setQans(responseMarkBeansJson);
                    rpsQuestion.setScore(maxScore);
					rpsQuestion.setNegativeScore(questionMetaData.getNegativeScore());
                }  else if (questionMetaData.getQuestionType().toString().equals(QuestionType.MULTIPLE_OPTIONS_QUESTION.toString())) {
                    Double maxScore = null;
                    List<OptionMetadata> optionMetadatas = questionMetaData.getOptionMetadatas();
                    List<ResponseMarkBean> responseMarkBeans = new ArrayList<>();
                    if (optionMetadatas != null) {
                        for (OptionMetadata qMetadata : optionMetadatas) {
                            ResponseMarkBean responseMarkBean = new ResponseMarkBean();
                            responseMarkBean.setResponse(qMetadata.getOptionIdentifier());
                            responseMarkBean.setResponseAnswer(RpsConstants.NA);
                            responseMarkBean.setResponsePositiveMarks(qMetadata.getMarks());
                            responseMarkBean.setResponseNegativeMarks(qMetadata.getNegativeMarks());
                            responseMarkBean.setCaseSensitive(false);
							responseMarkBean.setIsCorrectAnswer(qMetadata.getIsCorrectAnswer());
                            responseMarkBeans.add(responseMarkBean);
                        }
                    } else
                        logger.error("there are no MOQ options in question id=" + questionMetaData.getQuestionId());

                    String responseMarkBeansJson = new Gson().toJson(responseMarkBeans);
                    rpsQuestion.setQans(responseMarkBeansJson);
                    rpsQuestion.setScore(maxScore);
					rpsQuestion.setNegativeScore(questionMetaData.getNegativeScore());
                }
                // setting topic and difficulty level from question meta data
                rpsQuestion.setTopic(questionMetaData.getTopic() == null ? RpsConstants.NA : questionMetaData.getTopic());
                if (questionMetaData.getComplexityLevel() != null)
                    rpsQuestion.setDifficultyLevel(Integer.toString(questionMetaData.getComplexityLevel().getValue()));
                else
                    rpsQuestion.setDifficultyLevel(Integer.toString(0));
                if (questionMetaData.getQuestionType().toString().equals(QuestionType.MULTIPLE_CHOICE_QUESTION.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.DESCRIPTIVE_QUESTION.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.WRITTEN_ENGLISH_TEST.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.TYPING_TEST.toString()) ||
                        questionMetaData.getQuestionType().toString().equals(QuestionType.MULTIPLE_OPTIONS_QUESTION.toString())) {
                    rpsQuestion.setScore(questionMetaData.getScore());
                    rpsQuestion.setNegativeScore(questionMetaData.getNegativeScore());
                    if (questionMetaData.getQuestionType().toString().equals(QuestionType.MULTIPLE_CHOICE_QUESTION.toString()))
                        rpsQuestion.setQans(questionMetaData.getCorrectAnswer());
                }
                rpsQuestion.setRpsAssessment(rpsAssessment);
                rpsQuestion.setCreationDate(new Date());
                rpsQuestion = rpsQuestionRepository.save(rpsQuestion);
                /**Creating Language based Map list where Its Key will be Language name for without language question NO_LANGUAGE will be used as key*/
                if (rpsQuestion != null && languageXml.containsKey(questionXmlPath) && languageQuestionMap.containsKey(languageXml.get(questionXmlPath))) {
                    Map<String, RpsQuestion> languageQMap = languageQuestionMap.get(languageXml.get(questionXmlPath));
                    languageQMap.put(rpsQuestion.getQuestId(), rpsQuestion);
                    languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
                } else if (languageXml.containsKey(questionXmlPath)) {
                    Map<String, RpsQuestion> languageQMap = new HashMap<>();
                    languageQMap.put(rpsQuestion.getQuestId(), rpsQuestion);
                    languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
                } else {
                    Map<String, RpsQuestion> languageQMap = null;
                    if (languageQuestionMap.containsKey("NO_LANGUAGE")) {
                        languageQMap = languageQuestionMap.get("NO_LANGUAGE");
                        languageQMap.put(rpsQuestion.getQuestId(), rpsQuestion);
                    } else {
                        languageQMap = new HashMap<>();
                        languageQMap.put(rpsQuestion.getQuestId(), rpsQuestion);
                    }
                    languageQuestionMap.put("NO_LANGUAGE", languageQMap);
                }
                if (questionMetaData.getQuestionType().equals(QuestionType.READING_COMPREHENSION) ||
                        questionMetaData.getQuestionType().equals(QuestionType.SURVEY)) {
                    Map<String, QuestionMetadata> childQuestionsMap = questionInfo.getChildQuestionInfo();
                    Set<Map.Entry<String, QuestionMetadata>> childQuestionsMapEntrySet = childQuestionsMap.entrySet();
                    Iterator<Map.Entry<String, QuestionMetadata>> childQuestionsMapEntrySetIterator = childQuestionsMapEntrySet.iterator();
                    short index = 1;
                    while (childQuestionsMapEntrySetIterator.hasNext()) {
						Map.Entry<String, QuestionMetadata> childQuestionsMapEntry =
								childQuestionsMapEntrySetIterator.next();
						QuestionMetadata childQuestionMetadata = childQuestionsMapEntry.getValue();
						if (childQuestionMetadata.getQuestionType().equals(QuestionType.MULTIPLE_CHOICE_QUESTION)) {
							// Assuming Child questions are only of type MCQ
							RpsQuestion rpsChildQuestion = rpsQuestionRepository.findByQIDAndAssessmentCodeByLanguage(
									childQuestionMetadata.getQuestionLabel(), rpsAssessment.getAssessmentCode(),
									languageXml.get(questionXmlPath));
							if (rpsChildQuestion == null)
								rpsChildQuestion = new RpsQuestion();
							rpsChildQuestion.setLanguage(languageXml.get(questionXmlPath));
							rpsChildQuestion.setQuestId(childQuestionMetadata.getQuestionLabel());
							rpsChildQuestion.setQuestionType(childQuestionMetadata.getQuestionType().toString());
							rpsChildQuestion.setScore(childQuestionMetadata.getScore());
							rpsChildQuestion.setNegativeScore(childQuestionMetadata.getNegativeScore());
							rpsChildQuestion.setQans(childQuestionMetadata.getCorrectAnswer());
							rpsChildQuestion.setTopic(childQuestionMetadata.getTopic() == null ? RpsConstants.NA
									: childQuestionMetadata.getTopic());
							if (childQuestionMetadata.getComplexityLevel() != null)
								rpsChildQuestion.setDifficultyLevel(
										Integer.toString(childQuestionMetadata.getComplexityLevel().getValue()));
							else
								rpsChildQuestion.setDifficultyLevel(Integer.toString(0));
							rpsChildQuestion.setRpsAssessment(rpsAssessment);
							rpsChildQuestion.setCreationDate(new Date());
							rpsChildQuestion.setParentqID(rpsQuestion);
							rpsChildQuestion.setchildQuestionSeqNumber(index++);
							rpsChildQuestion = rpsQuestionRepository.save(rpsChildQuestion);
							/**
							 * Creating RC and MCQ Language based Map list where Its Key will be Language name for
							 * without language question NO_LANGUAGE will be used as key
							 */
							if (languageXml.containsKey(questionXmlPath)
									&& languageQuestionMap.containsKey(languageXml.get(questionXmlPath))) {
								Map<String, RpsQuestion> languageQMap =
										languageQuestionMap.get(languageXml.get(questionXmlPath));
								languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
							} else if (languageXml.containsKey(questionXmlPath)) {
								Map<String, RpsQuestion> languageQMap = new HashMap<>();
								languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
							} else {
								Map<String, RpsQuestion> languageQMap = null;
								if (languageQuestionMap.containsKey("NO_LANGUAGE")) {
									languageQMap = languageQuestionMap.get("NO_LANGUAGE");
									languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								} else {
									languageQMap = new HashMap<>();
									languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								}
								languageQuestionMap.put("NO_LANGUAGE", languageQMap);
							}
						}
						else if (childQuestionMetadata.getQuestionType()
								.equals(QuestionType.MULTIPLE_OPTIONS_QUESTION)) {
							// Child questions are only of type MOQ
							RpsQuestion rpsChildQuestion = rpsQuestionRepository.findByQIDAndAssessmentCodeByLanguage(
									childQuestionMetadata.getQuestionLabel(), rpsAssessment.getAssessmentCode(),
									languageXml.get(questionXmlPath));
							if (rpsChildQuestion == null)
								rpsChildQuestion = new RpsQuestion();
							List<OptionMetadata> optionMetadatas = childQuestionMetadata.getOptionMetadatas();
							List<ResponseMarkBean> responseMarkBeans = new ArrayList<>();
							rpsChildQuestion.setLanguage(languageXml.get(questionXmlPath));

							if (optionMetadatas != null) {
								for (OptionMetadata qMetadata : optionMetadatas) {
									ResponseMarkBean responseMarkBean = new ResponseMarkBean();
									responseMarkBean.setResponse(qMetadata.getOptionIdentifier());
									responseMarkBean.setResponseAnswer(RpsConstants.NA);
									responseMarkBean.setResponsePositiveMarks(qMetadata.getMarks());
									responseMarkBean.setResponseNegativeMarks(qMetadata.getNegativeMarks());
									responseMarkBean.setCaseSensitive(false);
									responseMarkBean.setIsCorrectAnswer(qMetadata.getIsCorrectAnswer());
									responseMarkBeans.add(responseMarkBean);
								}
							} else
								logger.error(
										"there are no MOQ options in question id="
												+ childQuestionMetadata.getQuestionId());
							String responseMarkBeansJson = new Gson().toJson(responseMarkBeans);
							rpsChildQuestion.setQans(responseMarkBeansJson);
							rpsChildQuestion.setScore(childQuestionMetadata.getScore());
							rpsChildQuestion.setNegativeScore(childQuestionMetadata.getNegativeScore());
							rpsChildQuestion.setQuestId(childQuestionMetadata.getQuestionLabel());
							rpsChildQuestion.setQuestionType(childQuestionMetadata.getQuestionType().toString());
							rpsChildQuestion.setTopic(childQuestionMetadata.getTopic() == null ? RpsConstants.NA
									: childQuestionMetadata.getTopic());
							if (childQuestionMetadata.getComplexityLevel() != null)
								rpsChildQuestion.setDifficultyLevel(
										Integer.toString(childQuestionMetadata.getComplexityLevel().getValue()));
							else
								rpsChildQuestion.setDifficultyLevel(Integer.toString(0));
							rpsChildQuestion.setRpsAssessment(rpsAssessment);
							rpsChildQuestion.setCreationDate(new Date());
							rpsChildQuestion.setParentqID(rpsQuestion);
							rpsChildQuestion.setchildQuestionSeqNumber(index++);
							rpsChildQuestion = rpsQuestionRepository.save(rpsChildQuestion);
							/**
							 * Creating RC and MOQ Language based Map list where Its Key will be Language name for
							 * without language question NO_LANGUAGE will be used as key
							 */
							if (languageXml.containsKey(questionXmlPath)
									&& languageQuestionMap.containsKey(languageXml.get(questionXmlPath))) {
								Map<String, RpsQuestion> languageQMap =
										languageQuestionMap.get(languageXml.get(questionXmlPath));
								languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
							} else if (languageXml.containsKey(questionXmlPath)) {
								Map<String, RpsQuestion> languageQMap = new HashMap<>();
								languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								languageQuestionMap.put(languageXml.get(questionXmlPath), languageQMap);
							} else {
								Map<String, RpsQuestion> languageQMap = null;
								if (languageQuestionMap.containsKey("NO_LANGUAGE")) {
									languageQMap = languageQuestionMap.get("NO_LANGUAGE");
									languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								} else {
									languageQMap = new HashMap<>();
									languageQMap.put(rpsChildQuestion.getQuestId(), rpsChildQuestion);
								}
								languageQuestionMap.put("NO_LANGUAGE", languageQMap);
							}
						}
                    }
                }
            }
        }
        return languageQuestionMap;
    }

    @Transactional
    public Object call(String qpPackFolder) throws RpsException, IOException {
        Reader fileReader = new FileReader(qpPackFolder + File.separator + "RpsMetaData.json");
        QpdRpsExportEntity qpdRpsExportEntity = new Gson().fromJson(fileReader, QpdRpsExportEntity.class);
        fileReader.close();

        String eventCode = qpdRpsExportEntity.getEventCode();
        String customerCode = qpdRpsExportEntity.getCustomerCode();
        String divisionCode = qpdRpsExportEntity.getDivisionCode();
        String assessmentCode = qpdRpsExportEntity.getAssessmentCode();
        Integer versionNumber = qpdRpsExportEntity.getGroupVersionNumber();

        RpsCustomer rpsCustomer = rpsCustomerRepository.findOne(customerCode);
        RpsDivision rpsDivision = rpsDivisionRepository.findOne(divisionCode);
        RpsEvent rpsEvent = rpsEventRepository.findOne(eventCode);
        RpsAssessment rpsAssessment = rpsAssessmentRepository.findOne(assessmentCode);


        if (rpsCustomer != null && rpsDivision != null && rpsEvent != null
                && rpsAssessment != null) {

            Map<String, RpsQpSection> sectionIdRpsSectionMap = new HashMap<>();
            List<RpsQpSection> qpSectionSetPerAssessment = rpsQpSectionRepository.findRpsQpSectionByAssessment(rpsAssessment);
            if (qpSectionSetPerAssessment != null && !qpSectionSetPerAssessment.isEmpty()) {
                Iterator<RpsQpSection> qpSectionSetPerAssessmentItr = qpSectionSetPerAssessment.iterator();
                while (qpSectionSetPerAssessmentItr.hasNext()) {
                    RpsQpSection section = qpSectionSetPerAssessmentItr.next();
                    sectionIdRpsSectionMap.put(section.getSecIdentifier(), section);
                }
            }

            int noOfQps = qpdRpsExportEntity.getQpFiles().size();

            RpsQuestionPaperPack rpsQuestionPaperPack = rpsQuestionPaperPackRepository.findByEventCodePerPackID(eventCode, FilenameUtils.getBaseName(qpPackFolder));

            if (rpsQuestionPaperPack == null)
                rpsQuestionPaperPack = new RpsQuestionPaperPack();
            rpsQuestionPaperPack.setPackId(FilenameUtils.getBaseName(qpPackFolder));
            rpsQuestionPaperPack.setNbOfQuestionPapers(noOfQps);
            rpsQuestionPaperPack.setRpsEvent(rpsEvent);
            rpsQuestionPaperPack.setIsActive(true);
            rpsQuestionPaperPack.setPackStatus(RpsConstants.packStatus.UNPACKED.toString());
            rpsQuestionPaperPack.setPackReceivingMode("MANUAL_UPLOAD");
            rpsQuestionPaperPack.setCreationDate(new Date());
            rpsQuestionPaperPack.setLastModifiedDate(new Date());
            rpsQuestionPaperPack.setVersionNumber(versionNumber.toString());
            rpsQuestionPaperPack = rpsQuestionPaperPackRepository.save(rpsQuestionPaperPack);

            String businessRulesPath = qpPackFolder + File.separator + qpdRpsExportEntity.getBrfileName();
            String layoutRulesPath = qpPackFolder + File.separator + qpdRpsExportEntity.getLrFileName();

            RpsBrLr rpsBrLr = new RpsBrLr();
            //try {
            String br = FileUtils.readFileToString(new File(businessRulesPath));
            String lr = FileUtils.readFileToString(new File(layoutRulesPath));
            Integer hashCode =
                    this.getHashCode(new Gson().fromJson(br, BRulesExportEntity.class),
                            new Gson().fromJson(lr, LayoutRulesExportEntity.class), assessmentCode);
            rpsBrLr = rpsBrLrRepository.getRpsBrLrInfo(hashCode);

            if ((rpsBrLr == null)) {
                rpsBrLr = new RpsBrLr();
                rpsBrLr.setBrRules(br);
                rpsBrLr.setLrRules(lr);
                rpsBrLr.setCreationDate(Calendar.getInstance().getTime());
                rpsBrLr.setHashCode(hashCode);
                rpsBrLr = rpsBrLrRepository.save(rpsBrLr);
            }

            //} catch (IOException e) {
            //    logger.error("Error during Reading Json File", e);
            //}

            RpsQpTemplate rpsQpTemplate = rpsQpTemplateRepository.findByRpsBrLr(rpsBrLr);

            if (rpsQpTemplate == null) {
                rpsQpTemplate = new RpsQpTemplate();
            }
            rpsQpTemplate.setCreationDate(Calendar.getInstance().getTime());
            rpsQpTemplate.setTemplateGroup(String.valueOf(qpdRpsExportEntity.getGroupNumber()));
            rpsQpTemplate.setVersion(String.valueOf(qpdRpsExportEntity.getGroupVersionNumber()));
            rpsQpTemplate.setRpsAssessment(rpsAssessment);
            rpsQpTemplate.setRpsQuestionPaperPack(rpsQuestionPaperPack);
            rpsQpTemplate.setRpsBrLr(rpsBrLr);
            rpsQpTemplateRepository.save(rpsQpTemplate);


            for (String qpFileName : qpdRpsExportEntity.getQpFiles()) {
                RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperRepository.getQPaperByAssessmentAndqpCode
                        (rpsAssessment.getAssessmentCode(), FilenameUtils.getBaseName(qpFileName));
                if (rpsQuestionPaper == null)
                    rpsQuestionPaper = new RpsQuestionPaper();

                //TODO: revisit
                rpsQuestionPaper.setQpCode(FilenameUtils.getBaseName(qpFileName));
                rpsQuestionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.AVAILABLE.toString());
                AssessmentTest assessmentTest = jqtiUtility.loadAssessmentTestFile(qpPackFolder + File.separator + qpFileName);
                List<AssessmentSection> assessmentSections = jqtiUtility.getListOfSection(assessmentTest);
                rpsQuestionPaper.setNumOfSections(assessmentSections.size());
                rpsQuestionPaper.setUniqueQPID(qpdRpsExportEntity.getQpIdName().get(qpFileName));
                rpsQuestionPaper.setQpFileName(qpFileName);
                rpsQuestionPaper.setRpsAssessment(rpsAssessment);
                rpsQuestionPaper.setRpsQuestionPaperPack(rpsQuestionPaperPack);
                logger.info("QP loaded successfully::" + qpFileName);

                rpsQuestionPaper = setQuestionPaperInfo(assessmentTest, rpsQuestionPaper,
                        qpdRpsExportEntity.getQpQuestionMap().getQpToQuestionMap().get(FilenameUtils.getBaseName(qpFileName)));

                rpsQuestionPaper = rpsQuestionPaperRepository.save(rpsQuestionPaper);

				// QpdRpsQpToQuestionMap qpQuestionMap = qpdRpsExportEntity.getQpQuestionMap();
				// Map<String, QuestionInfo> questions =
				// qpQuestionMap.getQpToQuestionMap().get(rpsQuestionPaper.getQpCode());
                Map<String, Map<String, RpsQuestion>> rpsQuestionsMap = null; //updateQuestionsforQP(questions, rpsQuestionPaper, rpsAssessment);

                saveSectionUsingJQTI(assessmentTest, sectionIdRpsSectionMap, rpsAssessment, rpsQuestionPaper, rpsQuestionsMap,
                        qpdRpsExportEntity.getQpQuestionMap().getQpToQuestionMap().get(FilenameUtils.getBaseName(qpFileName)));


            }
        }
        return null;
    }

    /**
     * @param bRules
     * @param layoutRules
     * @return hasCode for business rule and layoutRule
     */

    private Integer getHashCode(Object bRules, Object layoutRules, String assessmentCode) {
        String rulesExportinfo = null;
        if ((bRules.getClass().getName().equals(BRulesExportEntity.class.getName()))
                && (layoutRules.getClass().getName().equals(LayoutRulesExportEntity.class.getName()))) {
            rulesExportinfo =
                    assessmentCode + ((BRulesExportEntity) bRules).getDescription()
                            + ((BRulesExportEntity) bRules).getDuration()
                            + ((BRulesExportEntity) bRules).getEndTimeAlertInMins()
                            + ((BRulesExportEntity) bRules).getId()
                            + ((BRulesExportEntity) bRules).getLateStartTimeInMins()
                            + ((BRulesExportEntity) bRules).getName() + ((BRulesExportEntity) bRules).getNumQPSets()
                            + ((LayoutRulesExportEntity) layoutRules).getDescription()
                            + ((LayoutRulesExportEntity) layoutRules).getId()
                            + ((LayoutRulesExportEntity) layoutRules).getName()
                            + ((LayoutRulesExportEntity) layoutRules).getPanelTheme()
                            + ((LayoutRulesExportEntity) layoutRules).getPostInstructionFile()
                            + ((LayoutRulesExportEntity) layoutRules).getPostInstructions()
                            + ((LayoutRulesExportEntity) layoutRules).getPreInstructionFile()
                            + ((LayoutRulesExportEntity) layoutRules).getPreInstructions()
                            + ((LayoutRulesExportEntity) layoutRules).getQuestionLayout()
                            + ((LayoutRulesExportEntity) layoutRules).getTermsAndConditions();
        }
        return rulesExportinfo.hashCode();
    }

    protected boolean saveSectionUsingJQTI(AssessmentTest assessmentTest,
                                           Map<String, RpsQpSection> sectionIdRpsSectionMap,
                                           RpsAssessment rpsAssessment, RpsQuestionPaper rpsQuestionPaper,
                                           Map<String, Map<String, RpsQuestion>> rpsQuestionsLanguageMap,
                                           Map<String, QuestionInfo> qpdQuestionMap) {

        List<AssessmentSection> assessmentSections = jqtiUtility.getListOfSection(assessmentTest);
        Iterator<AssessmentSection> assessmentSectionIterator = assessmentSections.iterator();
        List<RpsQuestionAssociation> rpsQuestionAssociationList = new ArrayList<>();
        // for storing section with marks in one question paper
        Map<String, Double> sectionsMarksForQPaperMap = new HashMap<>();
        double totalMarksPerPaper = 0;
        int qAnsCount = 0;
        Map.Entry<String, Map<String, RpsQuestion>> entry = rpsQuestionsLanguageMap.entrySet().iterator().next();
        Map<String, RpsQuestion> rpsQuestionsMap = entry.getValue();
		int index = 0;
        while (assessmentSectionIterator.hasNext()) {
            Double totalMarksPerSection = 0.0;
            AssessmentSection assessmentSection = assessmentSectionIterator.next();
            String sectionCode = assessmentSection.getIdentifier();
            List<SectionPart> sectionParts = jqtiUtility.getListOfSectionPart(assessmentSection);
            RpsQpSection rpsQpSection = sectionIdRpsSectionMap.get(sectionCode);

            if (rpsQpSection == null) {
                rpsQpSection = this.setRpsQpSectionInfo(assessmentSection);
                rpsQpSection.setNumOfQuestions(sectionParts.size());
                rpsQpSection.setRpsAssessment(rpsAssessment);

                rpsQpSection = rpsQpSectionRepository.save(rpsQpSection);
                sectionIdRpsSectionMap.put(sectionCode, rpsQpSection);
                logger.info("New QPSection has been updated in database where qpSectionId::" + rpsQpSection.getSecIdentifier()
                        + " row id::" + rpsQpSection.getQpSecId());
            }



            Map<String, Double> totalmarksMap = new HashMap<>();
            for (SectionPart sectionPart : sectionParts) {
                AssessmentItemRef assessmentItemRef = (AssessmentItemRef) sectionPart;
                QuestionInfo qpdQuestion = qpdQuestionMap.get(assessmentItemRef.getIdentifier());
                //  Allowing the same question for All question paper in database
                if (qpdQuestion.getqMetadata().getQuestionLanguages() != null) {
                    List<String> qpdQuestionLanguage = qpdQuestion.getqMetadata().getQuestionLanguages();
                    index++;
                    for (String language : qpdQuestionLanguage) {
                        Map<String, RpsQuestion> languageQuestionsMap = rpsQuestionsLanguageMap.get(language);
                        RpsQuestion rpsQuestion = languageQuestionsMap.get(assessmentItemRef.getIdentifier());
                        RpsQuestionAssociation rpsQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, rpsQuestion, rpsQpSection);
                        if (rpsQuestion.getParentqID() == null) {
                            rpsQuestionAssociation.setQuestionSequence(index);
                            if (rpsQuestion.getQuestionType().equals(QuestionType.READING_COMPREHENSION.toString()) ||
                                    rpsQuestion.getQuestionType().equals(QuestionType.SURVEY.toString())) {
                                Map<String, QuestionMetadata> qpdChildQuestionsMap = qpdQuestion.getChildQuestionInfo();
                                Set<Map.Entry<String, QuestionMetadata>> qpdChildQuestionsMapEntries = qpdChildQuestionsMap.entrySet();
                                Iterator<Map.Entry<String, QuestionMetadata>> qpdChildQuestionsMapEntriesIterator = qpdChildQuestionsMapEntries.iterator();
                                while (qpdChildQuestionsMapEntriesIterator.hasNext()) {
                                    Map.Entry<String, QuestionMetadata> qpdChildQuestionsMapEntry = qpdChildQuestionsMapEntriesIterator.next();
                                    String qpdChildQuestionId = qpdChildQuestionsMapEntry.getKey();
                                    //QuestionMetadata qpdChildQuestionMetaData = qpdChildQuestionsMapEntry.getValue();

                                    RpsQuestion rpschildQuestion = languageQuestionsMap.get(qpdChildQuestionId);
                                    RpsQuestionAssociation rpsChildQuestionAssociation = rpsQuestionAssociationRepository.getQuestionAssociationFromIds
                                            (rpsQuestionPaper, rpsQpSection, rpschildQuestion);
                                    if (rpsChildQuestionAssociation == null)
                                        rpsChildQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, rpschildQuestion, rpsQpSection);

                                    rpsChildQuestionAssociation.setQuestionSequence(rpsQuestionAssociation.getQuestionSequence());
                                    rpsQuestionAssociationList.add(rpsChildQuestionAssociation);
                                    totalmarksMap.put(rpschildQuestion.getQuestId(), rpschildQuestion.getScore());
                                }
                            }
                        }
                        if (rpsQuestionAssociationRepository.getQuestionAssociationFromIds(rpsQuestionPaper, rpsQpSection, rpsQuestion) == null)
                            rpsQuestionAssociationList.add(rpsQuestionAssociation);
                        if (rpsQuestion.getScore() != null)
                            totalmarksMap.put(rpsQuestion.getQuestId(), rpsQuestion.getScore());

                    }
                } else {
                    RpsQuestion rpsQuestion = rpsQuestionsMap.get(assessmentItemRef.getIdentifier());
                    RpsQuestionAssociation rpsQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, rpsQuestion, rpsQpSection);
                    if (rpsQuestion.getParentqID() == null) {
                        rpsQuestionAssociation.setQuestionSequence(index++);
                        if (rpsQuestion.getQuestionType().equals(QuestionType.READING_COMPREHENSION.toString()) ||
                                rpsQuestion.getQuestionType().equals(QuestionType.SURVEY.toString())) {
                            Map<String, QuestionMetadata> qpdChildQuestionsMap = qpdQuestion.getChildQuestionInfo();
                            Set<Map.Entry<String, QuestionMetadata>> qpdChildQuestionsMapEntries = qpdChildQuestionsMap.entrySet();
                            Iterator<Map.Entry<String, QuestionMetadata>> qpdChildQuestionsMapEntriesIterator = qpdChildQuestionsMapEntries.iterator();
                            while (qpdChildQuestionsMapEntriesIterator.hasNext()) {
                                Map.Entry<String, QuestionMetadata> qpdChildQuestionsMapEntry = qpdChildQuestionsMapEntriesIterator.next();
                                String qpdChildQuestionId = qpdChildQuestionsMapEntry.getKey();
                                //QuestionMetadata qpdChildQuestionMetaData = qpdChildQuestionsMapEntry.getValue();

                                RpsQuestion rpschildQuestion = rpsQuestionsMap.get(qpdChildQuestionId);
                                RpsQuestionAssociation rpsChildQuestionAssociation = rpsQuestionAssociationRepository.getQuestionAssociationFromIds
                                        (rpsQuestionPaper, rpsQpSection, rpschildQuestion);
                                if (rpsChildQuestionAssociation == null)
                                    rpsChildQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, rpschildQuestion, rpsQpSection);

                                rpsChildQuestionAssociation.setQuestionSequence(rpsQuestionAssociation.getQuestionSequence());
                                rpsQuestionAssociationList.add(rpsChildQuestionAssociation);
                                totalmarksMap.put(rpschildQuestion.getQuestId(), rpschildQuestion.getScore());
                            }
                        }
                    }
                    if (rpsQuestionAssociationRepository.getQuestionAssociationFromIds(rpsQuestionPaper, rpsQpSection, rpsQuestion) == null)
                        rpsQuestionAssociationList.add(rpsQuestionAssociation);
                    if (rpsQuestion.getScore() != null)
                        totalmarksMap.put(rpsQuestion.getQuestId(), rpsQuestion.getScore());
                }
            }
            if (totalMarksPerSection != null) {
                logger.info("Total marks Per Serction :: {} ", totalMarksPerSection.doubleValue());
                if (totalmarksMap.size() > 0) {
                    for (Entry<String, Double> questionScore : totalmarksMap.entrySet()) {
                        if (questionScore.getValue() != null)
                            totalMarksPerSection = totalMarksPerSection + questionScore.getValue();
                    }
                }
                rpsQpSection.setSecScore(totalMarksPerSection);
            }
            //updating total score per Section
            rpsQpSectionRepository.save(rpsQpSection);
            totalMarksPerPaper = totalMarksPerPaper + totalMarksPerSection;
            sectionsMarksForQPaperMap.put(sectionCode, totalMarksPerSection);
        }
        logger.info("Total marks per QuestionPaperSet :: {} ", totalMarksPerPaper);
        rpsQuestionPaper.setTotalScore(totalMarksPerPaper);
        String sectionWiseMarksJson = new Gson().toJson(sectionsMarksForQPaperMap);
        rpsQuestionPaper.setJsonForSecWiseMarks(sectionWiseMarksJson);
        //updating total marks Per Paper set
        rpsQuestionPaperRepository.save(rpsQuestionPaper);
        if (qAnsCount > 0) {
            rpsQuestionPaper.setIsAnswerKeyAvailable(true);
            rpsQuestionPaperRepository.save(rpsQuestionPaper);
        }
        if (!rpsQuestionAssociationList.isEmpty()) {
            rpsQuestionAssociationRepository.save(rpsQuestionAssociationList);
            logger.info("QuestionAssociation has been updated in database...");
        }
        return true;
    }


    /**
     * @param assessmentSection
     * @return RpsQpSection
     */

    private RpsQpSection setRpsQpSectionInfo(AssessmentSection assessmentSection) {
        RpsQpSection rpsQpSection = new RpsQpSection();
        rpsQpSection.setSecIdentifier(assessmentSection.getIdentifier());
        rpsQpSection.setTitle(assessmentSection.getTitle());
        rpsQpSection.setCreationDate(Calendar.getInstance().getTime());
        rpsQpSection.setDuration((int) assessmentSection.getDuration());
        rpsQpSection.setIsMandatory(assessmentSection.getRequired());
        return rpsQpSection;
    }

    /**
     * @param rpsQuestionPaper
     * @param rpsQuestion
     * @param rpsQpSection
     * @return RpsQuestionAssociation
     */

    private RpsQuestionAssociation setRpsQuestionAssociationInfo(RpsQuestionPaper rpsQuestionPaper, RpsQuestion rpsQuestion, RpsQpSection rpsQpSection) {
        RpsQuestionAssociation rpsQuestionAssociation = new RpsQuestionAssociation();

        if (rpsQuestionPaper != null)
            rpsQuestionAssociation.setRpsQuestionPaper(rpsQuestionPaper);

        if (rpsQuestion != null)
            rpsQuestionAssociation.setRpsQuestion(rpsQuestion);

        if (rpsQpSection != null)
            rpsQuestionAssociation.setRpsQpSection(rpsQpSection);
        return rpsQuestionAssociation;
    }
}
