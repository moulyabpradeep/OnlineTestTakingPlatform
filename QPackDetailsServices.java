package com.merittrac.apollo.rps.services;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.merittrac.apollo.common.ApolloConstants;
import com.merittrac.apollo.common.CryptUtil;
import com.merittrac.apollo.common.QuestionType;
import com.merittrac.apollo.common.ZipUtility;
import com.merittrac.apollo.common.exception.ApolloSecurityException;
import com.merittrac.apollo.common.multiplequestionsperpage.MultipleQuestionBean;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBrLr;
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
import com.merittrac.apollo.qpd.qpgenentities.BRulesExportEntity;
import com.merittrac.apollo.qpd.qpgenentities.LayoutRulesExportEntity;
import com.merittrac.apollo.qpdrpsentities.QpdRpsExportEntity;
import com.merittrac.apollo.qpdrpsentities.QpdRpsQpToQuestionMap;
import com.merittrac.apollo.qpdrpsentities.QuestionInfo;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.jms.receiver.AbstractReceiver;
import com.merittrac.apollo.rps.jqti.dao.JQTIQuestionEntity;
import com.merittrac.apollo.rps.jqti.utility.JQTIApolloUtility;
import com.merittrac.apollo.rps.jqti.utility.JQTIUtility;
import com.merittrac.apollo.rps.qpd.entity.QPPackAnswerMarksEntity;
import com.merittrac.apollo.rps.utility.RpsGeneralUtility;

/**
 * QPackDetailsServices process one or more QPPacks associated with Meta Data json file
 * and update necessary database tables
 *
 * @author Amar_K
 */
@Service
public class QPackDetailsServices extends AbstractReceiver implements IQPackDetailsServices {

    @Autowired
    QPackUploadService qPackUploadService;

    @Autowired
    RpsCustomerRepository rpsCustomerRepository;

    @Autowired
    RpsDivisionRepository rpsDivisionRepository;

    @Autowired
    RpsEventRepository rpsEventRepository;

    @Autowired
    RpsAssessmentRepository rpsAssessmentRepository;

    @Autowired
    RpsBrLrRepository rpsBrLrRepository;

    @Autowired
    RpsQuestionPaperPackRepository rpsQuestionPaperPackRepository;

    @Autowired
    RpsQpTemplateRepository rpsQpTemplateRepository;

    @Autowired
    RpsQuestionPaperRepository rpsQuestionPaperRepository;

    @Autowired
    RpsQpSectionRepository rpsQpSectionRepository;

    @Autowired
    RpsQuestionRepository rpsQuestionRepository;

    @Autowired
    RpsQuestionAssociationRepository rpsQuestionAssociationRepository;

    @Autowired
    JQTIUtility jqtiUtility;

    @Autowired
    JQTIApolloUtility jqtiApolloUtility;

    @Autowired
    CryptUtil cryptUtil;

    @Autowired
    RpsGeneralUtility rpsGeneralUtility;

    @Value("${apollo_home_dir}")
    private String APOLLO_HOME;

    @Value("${isQpackEncrypted}")
    private String isQpackEncrypted;

    @Value("${qpdMetaDataName}")
    private String qpdMetaDataName;

    protected static final Logger LOGGER = LoggerFactory.getLogger(QPackDetailsServices.class);
    private static final String setCode = "set";
    private static final String errorFolderName = "ERROR/QPACK";

    public QPackDetailsServices() throws RpsException {
        super();
    }

    private String populatePath(String localExtractPath, String tempFolderName, String fileName) {
        return FilenameUtils.separatorsToSystem(localExtractPath + File.separator + tempFolderName + File.separator + fileName);
    }


    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, String> unZipQPack(String packFileDownloadPath, String eventCode, Boolean isManualUpload, String comments) throws Exception {
        String qpackExtractionPath = FilenameUtils.separatorsToSystem(APOLLO_HOME + File.separator + RpsConstants.tempFolder);
        if (!new File(qpackExtractionPath).exists())
            new File(qpackExtractionPath).mkdirs();
        String imagePath = FilenameUtils.separatorsToSystem(APOLLO_HOME);
        boolean isMIFRequired = false;

        LOGGER.debug("unZipQPack()::IN ");
        RpsQuestionPaperPack rpsQuestionPaperPack = new RpsQuestionPaperPack();
        Map<String, String> cdeMap = new HashMap<String, String>();

        String fileName = FilenameUtils.getName(packFileDownloadPath);
        String tempFolderName = FilenameUtils.getBaseName(packFileDownloadPath);
        RpsEvent rpsEvent = rpsEventRepository.findOne(eventCode);

        try {
            //check flag whether Qpack is encrypted and unzip files to encrypted folder.
            if (isQpackEncrypted.equalsIgnoreCase(RpsConstants.YES)) {
                //files inside qpack is encrypted, so decrypt them
                cryptUtil.decryptFileUsingAES(new File(packFileDownloadPath), new File(qpackExtractionPath + File.separator + fileName), eventCode);
                ZipUtility.extractAllOptimized(qpackExtractionPath + File.separator + fileName, qpackExtractionPath, true);
                LOGGER.info("QPack file unzip successfully.....");
            } else {
                //qpack is not encrypted so just extract to required folder
                ZipUtility.extractAllOptimized(packFileDownloadPath, qpackExtractionPath, true);
                LOGGER.info("QPack file unzip successfully.....");
            }
            LOGGER.info("QPackFile will extract at local path = {}", qpackExtractionPath + File.separator + tempFolderName);
            String questionFolderName = this.getquestionFolderName(qpackExtractionPath + File.separator + tempFolderName);
            String rpsMetaDataPath = FilenameUtils.separatorsToSystem(qpackExtractionPath + File.separator + tempFolderName +
                    File.separator + qpdMetaDataName + ".json");

            QpdRpsExportEntity qPackDetails = (QpdRpsExportEntity) getObjectFromJson(rpsMetaDataPath, QpdRpsExportEntity.class);
            rpsQuestionPaperPack = rpsQuestionPaperPackRepository.findByEventCodePerPackID(eventCode, tempFolderName);

            if (rpsQuestionPaperPack != null &&
                    rpsQuestionPaperPack.getQpPackId() != null &&
                    rpsQuestionPaperPack.getVersionNumber() != null &&
                    !rpsQuestionPaperPack.getPackStatus().equals(RpsConstants.packStatus.UNPACK_ERROR.toString()) &&
                    !rpsQuestionPaperPack.getPackStatus().equals(RpsConstants.packStatus.PACKS_RECEIVED.toString())) {
                LOGGER.warn("QPACK is all ready extracted and it's PackID is = {} " + tempFolderName);
                return null;
            } else if (rpsQuestionPaperPack == null) {
                rpsQuestionPaperPack = new RpsQuestionPaperPack();
                rpsQuestionPaperPack.setCreationDate(Calendar.getInstance().getTime());
            }

            rpsQuestionPaperPack.setPackId(tempFolderName);
            rpsQuestionPaperPack.setPackStatus(RpsConstants.packStatus.PACKS_RECEIVED.toString());

            String businessRulesPath = populatePath(qpackExtractionPath, tempFolderName, qPackDetails.getBrfileName());
            String layoutRulespath = populatePath(qpackExtractionPath, tempFolderName, qPackDetails.getLrFileName());

            // converting json to BRulesExportEntity and LayoutRulesExportEntity
            BRulesExportEntity bRulesExportInfo = (BRulesExportEntity) getObjectFromJson(businessRulesPath, BRulesExportEntity.class);
            LayoutRulesExportEntity layoutRulesExportInfo = (LayoutRulesExportEntity) getObjectFromJson(layoutRulespath, LayoutRulesExportEntity.class);

            imagePath = FilenameUtils.separatorsToSystem(imagePath + File.separator + qPackDetails.getCustomerCode() +
                    File.separator + qPackDetails.getDivisionCode() + File.separator + qPackDetails.getEventCode() +
                    File.separator + qPackDetails.getAssessmentCode());

            // for customerLogo
            try {
                String customerLogoFullPath = layoutRulesExportInfo.getCustomerLogo();
                LOGGER.info("in unZipQPack customerLogoFullPath=" + customerLogoFullPath);
                Map<String, String> customerLogMap = new HashMap<>();
                Type type = new TypeToken<HashMap<String, String>>() {
                }.getType();
                customerLogMap = new Gson().fromJson(customerLogoFullPath, type);
                LOGGER.info("in unZipQPack customerLogMap=" + customerLogMap);
                String customerLogoDir = null;
                if (customerLogMap != null && !customerLogMap.isEmpty()) {
                    String customerLogoPath = customerLogMap.get("customerLogoLocation");
                    if (customerLogoPath != null && !customerLogoPath.isEmpty())
                        customerLogoDir = FilenameUtils.separatorsToSystem(qpackExtractionPath + File.separator + tempFolderName + File.separator + customerLogoPath);
                } else {
                    LOGGER.error("in unZipQPack : customerLogo customerLogMap is empty");
                }
                LOGGER.info("in unZipQPack customerLogoDir=" + customerLogoDir);
                if (customerLogoDir != null && !customerLogoDir.isEmpty() && !FilenameUtils.getExtension(customerLogoDir).isEmpty())
                    createCustomerLogo(imagePath, customerLogoDir);
                else
                    LOGGER.info("in unZipQPack image is not in lrRules");
            } catch (Exception e) {
                LOGGER.error("ERROR :: in unZipQPack Exception=" + e);
                e.printStackTrace();
            }

            if (bRulesExportInfo != null) {
                isMIFRequired = bRulesExportInfo.isMif();
            }

            rpsQuestionPaperPack.setLastModifiedDate(Calendar.getInstance().getTime());

            if (isManualUpload != null) {
                if (isManualUpload)
                    rpsQuestionPaperPack.setPackReceivingMode(RpsConstants.packReceiveMode.MANUAL_UPLOAD.toString());
                else
                    rpsQuestionPaperPack.setPackReceivingMode(RpsConstants.packReceiveMode.JMS_UPLOAD.toString());
            }

            rpsQuestionPaperPack.setPackFileDownloadPath(qPackDetails.getDownloadUrl());
            rpsQuestionPaperPack.setLocalFileDestinationPath(qpackExtractionPath + File.separator + tempFolderName);
            rpsQuestionPaperPack.setDownloadTime(new Date());
            rpsQuestionPaperPack.setRpsEvent(rpsEvent);
            rpsQuestionPaperPack.setIsActive(true);
            rpsQuestionPaperPack.setVersionNumber(String.valueOf(qPackDetails.getGroupVersionNumber()));
            rpsQuestionPaperPack.setNbOfQuestionPapers(qPackDetails.getQpFiles().size());
            rpsQuestionPaperPack = rpsQuestionPaperPackRepository.save(rpsQuestionPaperPack);

            RpsBrLr rpsBrLr = new RpsBrLr();
            rpsBrLr.setBrRules(this.getStringFromJson(businessRulesPath));
            rpsBrLr.setLrRules(this.getStringFromJson(layoutRulespath));
            rpsBrLr.setCreationDate(Calendar.getInstance().getTime());
            rpsBrLr.setHashCode(this.getHashCode(bRulesExportInfo, layoutRulesExportInfo,
                    qPackDetails.getAssessmentCode()));

            if ((rpsBrLrRepository.getRpsBrLrInfo(this.getHashCode(bRulesExportInfo, layoutRulesExportInfo,
                    qPackDetails.getAssessmentCode())) == null)) {
                rpsBrLrRepository.save(rpsBrLr);
            }

            RpsQpTemplate rpsQpTemplate = new RpsQpTemplate();
            rpsQpTemplate.setCreationDate(Calendar.getInstance().getTime());
            rpsQpTemplate.setTemplateGroup(String.valueOf(qPackDetails.getGroupNumber()));
            rpsQpTemplate.setVersion(String.valueOf(qPackDetails.getGroupVersionNumber()));

            RpsAssessment rpsAssessment = rpsAssessmentRepository.getAssessmentDetailsPerId(qPackDetails.getAssessmentCode());
            rpsQpTemplate.setRpsAssessment(rpsAssessment);
            rpsQpTemplate.setRpsQuestionPaperPack(rpsQuestionPaperPack);
            rpsQpTemplate.setRpsBrLr(rpsBrLr);
            rpsQpTemplateRepository.save(rpsQpTemplate);

            //get RpsEvent from RpsAssessment
            //get DUMMY question paper PackID by RpsEvent
            RpsQuestionPaperPack questionPaperPack = rpsQuestionPaperPackRepository.findByEventCodePerPackID(eventCode,
                    RpsConstants.DUMMY_QUESTION_PAPER_PACK_ID_PER_EVENT);

            //getAllQPs for the Question Paper Pack
            Map<String, RpsQuestionPaper> qpCodeRpsDummyQpMap = new HashMap<>();
            if (questionPaperPack != null) {
                List<RpsQuestionPaper> qPapers = rpsQuestionPaperRepository.getAllQPapersByQPackId(questionPaperPack);
                if (qPapers != null && !qPapers.isEmpty()) {
                    Iterator<RpsQuestionPaper> qpIterator = qPapers.iterator();
                    while (qpIterator.hasNext()) {
                        RpsQuestionPaper paper = qpIterator.next();
                        qpCodeRpsDummyQpMap.put(paper.getQpCode(), paper);
                    }
                }
            }

            Map<String, RpsQpSection> sectionIdRpsSectionMap = new HashMap<String, RpsQpSection>();
            List<RpsQpSection> qpSectionSetPerAssessment = rpsQpSectionRepository.findRpsQpSectionByAssessment(rpsAssessment);
            if (qpSectionSetPerAssessment != null && !qpSectionSetPerAssessment.isEmpty()) {
                Iterator<RpsQpSection> qpSectionSetPerAssessmentItr = qpSectionSetPerAssessment.iterator();
                while (qpSectionSetPerAssessmentItr.hasNext()) {
                    RpsQpSection section = qpSectionSetPerAssessmentItr.next();
                    sectionIdRpsSectionMap.put(section.getSecIdentifier(), section);
                }
            }

            Map<String, List<RpsQuestion>> questionIdRpsQuestionMap = new HashMap<String, List<RpsQuestion>>();
            List<RpsQuestion> questionList = rpsQuestionRepository.getAllQuestionPerAssessment(rpsAssessment);
            if (questionList != null && !questionList.isEmpty()) {
                Iterator<RpsQuestion> questionsPerAssessmentItr = questionList.iterator();
                while (questionsPerAssessmentItr.hasNext()) {
                    List<RpsQuestion> rpsQuestions = new ArrayList<RpsQuestion>();
                    RpsQuestion question = questionsPerAssessmentItr.next();
					if (!(question.getQuestionType().equals(QuestionType.READING_COMPREHENSION.toString())
							|| question.getQuestionType().equals(QuestionType.SURVEY.toString()))) {
                        if (question.getParentqID() != null) {
                            rpsQuestions = questionIdRpsQuestionMap.get(question.getParentqID().getQuestId());
                            if (rpsQuestions == null)
                                rpsQuestions = new ArrayList<RpsQuestion>();
                            rpsQuestions.add(question);
                            questionIdRpsQuestionMap.put(question.getParentqID().getQuestId(), rpsQuestions);
                        } else {
                            rpsQuestions.add(question);
                            questionIdRpsQuestionMap.put(question.getQuestId(), rpsQuestions);
                        }
                    } else {
                        // The map will have the RC type QuestID as the key and the value as a list of RpsQuestions
                        // out of which the first will always be the parent
                        rpsQuestions.add(question);
                        questionIdRpsQuestionMap.put(question.getQuestId(), rpsQuestions);
                    }
                }
            }

            Set<RpsQuestionAssociation> questionAssociationsSet = new HashSet<RpsQuestionAssociation>();
            List<RpsQuestionAssociation> questionAssociationsList = null;
            List<RpsQuestionPaper> questionPapers = rpsQuestionPaperRepository.getAllQuestionPapersPerAssessment(rpsAssessment);

            if (questionPapers != null && (!questionPapers.isEmpty()))
                questionAssociationsList = rpsQuestionAssociationRepository.getQuestionAssociationsFromQPsets(questionPapers);

            if (questionAssociationsList != null)
                questionAssociationsSet.addAll(questionAssociationsList);

            for (String qpFileName : qPackDetails.getQpFiles()) {
                RpsQuestionPaper rpsQuestionPaper = rpsQuestionPaperRepository.getQPaperByAssessmentAndqpCode
                        (rpsAssessment.getAssessmentCode(), FilenameUtils.getBaseName(qpFileName));
                if (rpsQuestionPaper == null)
                    rpsQuestionPaper = new RpsQuestionPaper();

                //TODO: revisit
                rpsQuestionPaper.setQpCode(FilenameUtils.getBaseName(qpFileName));
                rpsQuestionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.AVAILABLE.toString());
       
                AssessmentTest assessmentTest = jqtiUtility.loadAssessmentTestFile(qpackExtractionPath + File.separator + tempFolderName + File.separator + qpFileName);
                List<AssessmentSection> assessmentSections = jqtiUtility.getListOfSection(assessmentTest);
                rpsQuestionPaper.setNumOfSections(assessmentSections.size());
                rpsQuestionPaper.setUniqueQPID(qPackDetails.getQpIdName().get(qpFileName));
                rpsQuestionPaper.setQpFileName(qpFileName);
                rpsQuestionPaper.setRpsAssessment(rpsAssessment);
                rpsQuestionPaper.setRpsQuestionPaperPack(rpsQuestionPaperPack);
                rpsQuestionPaper.setIsMIFRequired(isMIFRequired);
                LOGGER.info("QP loaded successfully::" + qpFileName);
                //Load Extended QP
                JAXBContext jaxbContext = JAXBContext.newInstance(MultipleQuestionBean.class);
                Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
                File extendedQpFileName =
                        new File(qpackExtractionPath + File.separator + tempFolderName + File.separator
                                + ApolloConstants.QUESTIONPAPER_XML_PREFIX + qpFileName);
                LOGGER.info("Extended QP is loading....!! ::" + extendedQpFileName);
                MultipleQuestionBean multipleQuestionBean = null;
//                if(extendedQpFileName.exists())
//                	multipleQuestionBean = (MultipleQuestionBean) jaxbUnmarshaller.unmarshal(extendedQpFileName);
                if (extendedQpFileName.exists()) {
                    StringReader reader = new StringReader(
                            FileUtils.readFileToString(extendedQpFileName).replaceAll("<img", "&lt;img"));
                    multipleQuestionBean = (MultipleQuestionBean) jaxbUnmarshaller.unmarshal(reader);
                } else
                    LOGGER.info("Extended QP is not present. Hence skipping Extended QP persisting!! ::" + extendedQpFileName);
                if (multipleQuestionBean != null) {
                    try {
                        rpsQuestionPaper.setExtendedQpJson(new Gson().toJson(multipleQuestionBean));
                        LOGGER.info("Extended QP loaded successfully::" + rpsQuestionPaper.getExtendedQpJson());
                    } catch (Exception e) {
                        LOGGER.info("Exception while parsing Extended QP, Hence skipping extended QP save for QPCODE::"
                                + rpsQuestionPaper.getQpCode());
                    }
                }

                rpsQuestionPaper = qPackUploadService.setQuestionPaperInfo(assessmentTest, rpsQuestionPaper,
                        qPackDetails.getQpQuestionMap().getQpToQuestionMap().get(FilenameUtils.getBaseName(qpFileName)));

                rpsQuestionPaper = rpsQuestionPaperRepository.save(rpsQuestionPaper);

                QpdRpsQpToQuestionMap qpQuestionMap = qPackDetails.getQpQuestionMap();
                Map<String, QuestionInfo> questions = qpQuestionMap.getQpToQuestionMap().get(rpsQuestionPaper.getQpCode());

                //copy all images from Qpack to local images folder

                String srcImageFolder = questionFolderName;
                String destImageFolder = FilenameUtils.getFullPath(packFileDownloadPath);
                LOGGER.info("copy all images from Qpack to local images folder, SRC:: " + srcImageFolder + " DEST:: " + destImageFolder);
				// copyImagesFolder(srcImageFolder, destImageFolder);

                Map<String,Map<String, RpsQuestion>> rpsQuestionsMap = qPackUploadService.updateQuestionsforQP(
                        questions, rpsQuestionPaper, rpsAssessment, questionFolderName, packFileDownloadPath);

                qPackUploadService.saveSectionUsingJQTI(assessmentTest, sectionIdRpsSectionMap, rpsAssessment, rpsQuestionPaper, rpsQuestionsMap,
                        qPackDetails.getQpQuestionMap().getQpToQuestionMap().get(FilenameUtils.getBaseName(qpFileName)));
            }

            rpsQuestionPaperPack.setPackStatus(RpsConstants.packStatus.UNPACKED.toString());
            rpsQuestionPaperPack = rpsQuestionPaperPackRepository.save(rpsQuestionPaperPack);
        } catch (Exception e) {
            rpsQuestionPaperPack.setPackStatus(RpsConstants.packStatus.UNPACK_ERROR.toString());
            rpsQuestionPaperPack = rpsQuestionPaperPackRepository.save(rpsQuestionPaperPack);
            this.storeErrorPackFile(packFileDownloadPath);
            LOGGER.error("Exception : message = " + "QPack File is not well format or QPack file has corrupted......={}", e);
            e.printStackTrace();
        } finally {
            File encryptFile = new File(qpackExtractionPath + File.separator + tempFolderName);
            if (encryptFile.isDirectory())
                FileUtils.deleteDirectory(encryptFile);

            File decryptedFile = new File(qpackExtractionPath + File.separator + fileName);
            FileUtils.deleteQuietly(decryptedFile);
            LOGGER.debug("unZipQPack()::OUT ");
        }
        cdeMap.put(RpsConstants.PACK_STATUS_MANUAL_UPLOAD, RpsConstants.PACK_STATUS.SUCCESSFUL.toString());
        return cdeMap;
    }

    private void createCustomerLogo(String imagePath, String customerLogoPath) throws IOException {
        LOGGER.info("start in createCustomerLogo---eventCode=" + eventCode + " assessmentCode=" + assessmentCode + " customerLogoPath=" + customerLogoPath);
        imagePath = FilenameUtils.separatorsToSystem(imagePath + File.separator + "customerLogo");
        LOGGER.info("in createCustomerLogo---imagePath=" + imagePath);
        File destFile = new File(imagePath);
        if (!destFile.isDirectory())
            destFile.mkdirs();

        File srcFile = new File(customerLogoPath);
        if (!srcFile.isFile())
            throw new IOException("Image is not present at the path :: " + customerLogoPath);

        try {
            FileUtils.copyFileToDirectory(srcFile, destFile);
        } catch (IOException e) {
            LOGGER.error("ERROR :: IOException --" + e.getMessage());
            e.printStackTrace();
        }
        LOGGER.info("end in createCustomerLogo---");
    }

    private RpsQuestionPaper setQuestionPaperInfo(AssessmentTest assessmentTest, RpsQuestionPaper rpsQuestionPaper, Map<String, QuestionInfo> questionMap) {

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

    /**
     * decrypting the questionpaper pack content
     *
     * @param encryptFolderName
     * @param qPackFolder
     * @param eventCode
     * @throws IOException
     * @throws ApolloSecurityException
     * @throws RpsException
     */

    private void decryptQpack(String encryptFolderName, File qPackFolder, String eventCode) throws IOException, ApolloSecurityException, RpsException {
        //cryptUtil.decryptDirectoryUsingAES();
        if (!qPackFolder.exists())
            qPackFolder.mkdirs();
        File encryptFolder = new File(encryptFolderName);
        String[] files = encryptFolder.list();
        List<String> configFilesList = new ArrayList<>();
        configFilesList.add(RpsConstants.BusinessRule);
        configFilesList.add(RpsConstants.LayoutRule);
        configFilesList.add(RpsConstants.RpsMetaData);
        configFilesList.add(RpsConstants.QuestionPaper);
        if (files == null)
            throw new RpsException("extracted QPack is empty---and can not be decrypted-- qpack path: " + qPackFolder.getAbsolutePath());
        for (String fileName : files) {
            File srcFile = new File(encryptFolderName + File.separator + fileName);
            File destFile = new File(qPackFolder + File.separator + fileName);
            Iterator<String> configFileIterator = configFilesList.iterator();
            //loop through all files and decrypt if required
            while (configFileIterator.hasNext()) {
                String configFileName = configFileIterator.next();
                if (fileName.contains(configFileName)) {
                    //check the flag for encryption
                    if (configFileName.equalsIgnoreCase(RpsConstants.YES))
                        //decrypt the file
                        cryptUtil.decryptFileUsingAES(srcFile, destFile, eventCode);
                    else
                        //move file to qPackFolder folder
                        FileUtils.copyFileToDirectory(srcFile, qPackFolder);
                }

            }
            //check the folders and decrypt them if required
            if (fileName.contains(RpsConstants.questions)) {
                if (fileName.contains(RpsConstants.ZIP))
                    //copy the zip file to the qpack folder
                    FileUtils.copyFileToDirectory(srcFile, qPackFolder);
                else {
                    //check the flag for encryption
                    if ((RpsConstants.questions).equalsIgnoreCase(RpsConstants.YES))
                        //decrypt the entire directory
                        cryptUtil.decryptDirectoryUsingAES(srcFile, destFile, eventCode);
                    else
                        //move file to qPackFolder folder
                        FileUtils.copyDirectory(srcFile, destFile);
                }
            } else {
                if (fileName.endsWith(".xml")) {
                    cryptUtil.decryptFileUsingAES(srcFile, destFile, eventCode);
                }
            }
        }
    }

    /**
     * decrypting the questionpaper pack content
     *
     * @param encryptFolderName
     * @param eventCode
     * @throws IOException
     * @throws ApolloSecurityException
     * @throws RpsException
     */

    private void decryptQuestionpack(File encryptFolderName, File qPackFile, String eventCode) throws IOException, ApolloSecurityException, RpsException {
        LOGGER.info("----IN decryptQuestionpack----");
        if (!encryptFolderName.exists())
            encryptFolderName.mkdirs();

        cryptUtil.decryptFileUsingAES(qPackFile, encryptFolderName, eventCode);

        LOGGER.info("------OUT-decryptQuestionpack-----");
    }


    /**
     * getting header from jms queue and setting necessary content into RpsQuestionPaperPack
     *
     * @param messageHeaders
     * @return RpsQuestionPaperPack
     * @throws Exception
     */

    private RpsEvent getEvent(MessageHeaders messageHeaders) throws Exception {
        LOGGER.info("----IN-getHeaderInformation-----");
        String eventCode = (String) messageHeaders.get("eventCode");
        RpsEvent rpsEvent = rpsEventRepository.findOne(eventCode);
        if (rpsEvent == null)
            throw new Exception("could not process because Event code is not present in database eventCode::" + eventCode);
        LOGGER.info("----OUT-getHeaderInformation-----");
        return rpsEvent;
    }

    /**
     * storing corrupted file to error folder
     *
     * @param packFileDownloadPath
     */

    private void storeErrorPackFile(String packFileDownloadPath) {

        try {
            String localPath = APOLLO_HOME;
            File errorFolder = new File(localPath + File.separator + errorFolderName);
            if (!errorFolder.isDirectory())
                errorFolder.mkdirs();

            FileUtils.copyFileToDirectory(new File(packFileDownloadPath), errorFolder.getAbsoluteFile());

        } catch (IOException exception) {
            LOGGER.error("error in copying files to archive=", exception);
        }
    }

    /**
     * @param extractFileDestinationPath
     * @return question Folder Name
     */
    private String getquestionFolderName(String extractFileDestinationPath) {
        File qPackDirFile = new File(extractFileDestinationPath);
        for (File questionDirFile : qPackDirFile.listFiles()) {
            if (FilenameUtils.getExtension(questionDirFile.getName()).equalsIgnoreCase("zip")) {
                return FilenameUtils.removeExtension(questionDirFile.getAbsolutePath());
            }
        }
        return null;
    }

    /**
     * @param qPackFilename
     * @return Question Paper Pack Folder Name
     */

    private String getFolderName(final String qPackFilename) {
        String foldername = "";
        final int pos = qPackFilename.lastIndexOf('.');
        if (pos > 0)
            foldername = qPackFilename.substring(0, pos);
        return foldername;
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
                            + ((BRulesExportEntity) bRules).isMif()
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

    /**
     * @param jsonFileName
     * @param className
     * @return Object from Json
     * @throws RpsException
     * @throws IOException
     */

    private Object getObjectFromJson(String jsonFileName, Class className) throws RpsException, IOException {
        BufferedReader bufferedReader = null;
        Object object = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(jsonFileName));
            Gson gson = new GsonBuilder().create();
            object = gson.fromJson(bufferedReader, className);
            bufferedReader.close();
        } catch (FileNotFoundException e1) {
            LOGGER.error("File not found=" + jsonFileName, e1);
            throw new RpsException("Error in parsing JSON file");
        } catch (IOException e2) {
            LOGGER.error("Error in reading JSON file=" + jsonFileName, e2);
            throw new RpsException("Error in parsing JSON file");
        } catch (Exception e3) {
            throw new RpsException("JSON Format Exception");
        }
        if (bufferedReader != null)
            bufferedReader.close();
        return object;
    }

    /**
     * @param qPackDetails
     * @param packFileDownloadPath
     * @return Archive Path
     */

    private String storeZipFileAtArchivePath(QpdRpsExportEntity qPackDetails, String packFileDownloadPath) {
        String localPath = APOLLO_HOME;
        File archivePath = null;
        try {
            archivePath = new File(localPath + File.separator + qPackDetails.getCustomerCode() + File.separator + qPackDetails.getDivisionCode() + File.separator + qPackDetails.getEventCode() + File.separator + qPackDetails.getAssessmentCode());
            if (!archivePath.isDirectory()) {
                archivePath.mkdirs();
            }
            FileUtils.copyFileToDirectory(new File(packFileDownloadPath), archivePath.getAbsoluteFile());
            File packFileDownloadPathFile = new File(packFileDownloadPath);
            if (packFileDownloadPathFile.exists())
                FileUtils.deleteQuietly(packFileDownloadPathFile);
        } catch (IOException ex) {
            LOGGER.warn("Exception: message=" + ex.getMessage());
        }
        return archivePath.getAbsolutePath().substring(localPath.length() - 1, archivePath.getAbsolutePath().length());
    }

    /**
     * @param jsonFileName
     * @return String from json
     */

    private String getStringFromJson(String jsonFileName) {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(jsonFileName));
        } catch (FileNotFoundException e1) {
            LOGGER.error("Error reading from buffer while reading json for LR/BR.", e1);
        }
        String line = null;
        StringBuilder lines = new StringBuilder();
        try {
            while ((line = bufferedReader.readLine()) != null)
                lines.append(line);
        } catch (IOException e) {
            LOGGER.error("Error reading from buffer while reading json for LR/BR", e);
        } finally {
            if (bufferedReader != null)
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    LOGGER.error("ERROR while closing Buffered Reader Stream---");
                }
        }
        return lines.toString();
    }

    /**
     * @param extractFileDestinationPath
     * @param questionDirPath
     * @param qPaperMPNGXml
     * @param rpsAssessment
     * @param rpsQuestionPaperPack
     * @param sectionIdRpsSectionMap
     * @param questionIdRpsQuestionMap
     * @param questionAssociationsSet
     * @param qpCodeRpsDummyQpMap
     * @throws RpsException
     */

    private void updateQPDStatus(String extractFileDestinationPath, String questionDirPath, String qPaperMPNGXml, RpsAssessment rpsAssessment,
                                 RpsQuestionPaperPack rpsQuestionPaperPack, Map<String, RpsQpSection> sectionIdRpsSectionMap, Map<String, List<RpsQuestion>> questionIdRpsQuestionMap,
                                 Set<RpsQuestionAssociation> questionAssociationsSet, Map<String, RpsQuestionPaper> qpCodeRpsDummyQpMap, String localImagePath, Integer uniqueQPID
            , QpdRpsExportEntity qPackDetails) throws RpsException {
        QPPackAnswerMarksEntity qpMarksEntity = null;
        int qSeqNumber = 1;
        Double totalMarksPerPaper = 0.0;
        LOGGER.debug("IN for QP::" + qPaperMPNGXml + " assessment::" + rpsAssessment.getAssessmentCode() + " pack id::" + rpsQuestionPaperPack.getPackId());
        List<RpsQuestionAssociation> rpsQuestionAssociationList = new ArrayList<RpsQuestionAssociation>();
        String qpackmapngfilepath = FilenameUtils.separatorsToSystem(extractFileDestinationPath + File.separator + qPaperMPNGXml);

        AssessmentTest assessmentTest = jqtiUtility.loadAssessmentTestFile(qpackmapngfilepath);

        LOGGER.info("QP loaded successfully::" + qPaperMPNGXml);

        List<AssessmentSection> assessmentSections = jqtiUtility.getListOfSection(assessmentTest);

        String questionPaperCode = assessmentTest.getIdentifier();

        RpsQuestionPaper rpsQuestionPaper = qpCodeRpsDummyQpMap.get(questionPaperCode);

        if (rpsQuestionPaper == null) {
            LOGGER.info("QP does not exists::" + qPaperMPNGXml);
            rpsQuestionPaper = new RpsQuestionPaper();
        }
        //rpsQuestionPaper = this.setQuestionPaperInfo(assessmentSections, assessmentTest);
        this.setQuestionPaperInfo(assessmentSections, assessmentTest, rpsQuestionPaper);
        rpsQuestionPaper.setRpsAssessment(rpsAssessment);
        rpsQuestionPaper.setQpFileName(qPaperMPNGXml);
        rpsQuestionPaper.setUniqueQPID(uniqueQPID);
        rpsQuestionPaper.setRpsQuestionPaperPack(rpsQuestionPaperPack);

        rpsQuestionPaper = rpsQuestionPaperRepository.save(rpsQuestionPaper);
        LOGGER.info("Question Paper has been updated in database where qpCode = {}" + rpsQuestionPaper.getQpCode());

        Iterator<AssessmentSection> assessmentSectionIterator = assessmentSections.iterator();

        int qAnsCount = 0;
        while (assessmentSectionIterator.hasNext()) {
            Double totalMarksPerSection = 0.0;
            AssessmentSection assessmentSection = assessmentSectionIterator.next();
            String sectionCode = assessmentSection.getIdentifier();

            List<SectionPart> sectionParts = jqtiUtility.getListOfSectionPart(assessmentSection);
            RpsQpSection rpsQpSection = null;

            rpsQpSection = sectionIdRpsSectionMap.get(sectionCode);

            if (rpsQpSection == null) {
                rpsQpSection = this.setRpsQpSectionInfo(assessmentSection);
                rpsQpSection.setNumOfQuestions(sectionParts.size());
                rpsQpSection.setRpsAssessment(rpsAssessment);

                rpsQpSection = rpsQpSectionRepository.save(rpsQpSection);
                sectionIdRpsSectionMap.put(sectionCode, rpsQpSection);
                LOGGER.info("New QPSection has been updated in database where qpSectionId::" + rpsQpSection.getSecIdentifier() + " row id::" + rpsQpSection.getQpSecId());
            }

            Iterator<SectionPart> sectionPartIterator = sectionParts.iterator();
            while (sectionPartIterator.hasNext()) {
                SectionPart sectionPart = sectionPartIterator.next();
                AssessmentItemRef assessmentItemRef = (AssessmentItemRef) sectionPart;

                //  Allowing the same question for All question paper in database

                RpsQuestionAssociation rpsQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, null, rpsQpSection);
                rpsQuestionAssociation.setQuestionSequence(qSeqNumber);
                RpsQuestion rpsQuestion = new RpsQuestion();
                rpsQuestion = new RpsQuestion();
                rpsQuestion.setQtiFileName(FilenameUtils.separatorsToSystem(questionDirPath + File.separator + assessmentItemRef.getHref()));
                rpsQuestion.setRpsAssessment(rpsAssessment);

                qpMarksEntity = this.updateQuestionInfoUsingJqtiFile(rpsQuestion, extractFileDestinationPath,
                        assessmentItemRef, rpsQuestionAssociation, questionAssociationsSet, rpsQuestionAssociationList, localImagePath, qPackDetails);

                if (qpMarksEntity.getAnswer() != null && !qpMarksEntity.getAnswer().isEmpty())
                    qAnsCount++;

                qSeqNumber++;

                totalMarksPerSection = totalMarksPerSection + qpMarksEntity.getMarks();

                //   discarding the same question for All question paper in database
//				RpsQuestion rpsQuestion = null;	
                /*List<RpsQuestion> rpsQuestions = questionIdRpsQuestionMap.get(assessmentItemRef.getIdentifier());

				if(rpsQuestions == null)
				{					
					RpsQuestionAssociation rpsQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, null, rpsQpSection);
					
					LOGGER.info("Question does  not exist");
					rpsQuestion = new RpsQuestion();
					rpsQuestion.setQtiFileName(questionDirPath + RpsConstants.FileDelimiter +assessmentItemRef.getHref());
					rpsQuestion.setRpsAssessment(rpsAssessment);
					
					qAnswer = this.updateQuestionInfoUsingJqtiFile(rpsQuestion, extractFileDestinationPath, 
							assessmentItemRef, rpsQuestionAssociation, questionAssociationsSet, rpsQuestionAssociationList, questionIdRpsQuestionMap);
					 
					if(qAnswer!=null && !qAnswer.isEmpty())
							qAnsCount++;					
				}
				else if(rpsQuestions!=null)
				{					 
					 for(RpsQuestion rQuestion : rpsQuestions)
					 {
						 if(rQuestion.getQans()!=null && !rQuestion.getQans().isEmpty())
								qAnsCount++;
						 
						 RpsQuestionAssociation rpsQuestionAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionPaper, rQuestion, rpsQpSection);						
						 if(!questionAssociationsSet.contains(rpsQuestionAssociation))
						 {
							 rpsQuestionAssociationList.add(rpsQuestionAssociation);
							 questionAssociationsSet.add(rpsQuestionAssociation);
							 LOGGER.info("Association added in queue");
						 }
					 }					 
				}*/
            }
            if (totalMarksPerSection != null) {
                LOGGER.info("Total marks Per Serction :: {} ", totalMarksPerSection.doubleValue());
                rpsQpSection.setSecScore(totalMarksPerSection);
            }
            //updating total score per Section
            rpsQpSectionRepository.save(rpsQpSection);
            totalMarksPerPaper = totalMarksPerPaper + totalMarksPerSection;
        }

        if (totalMarksPerPaper != null) {
            LOGGER.info("Total marks per QuestionPaperSet :: {} ", totalMarksPerPaper);
            rpsQuestionPaper.setTotalScore(totalMarksPerPaper);
        }
        //updating total marks Per Paper set
        rpsQuestionPaperRepository.save(rpsQuestionPaper);
        if (qAnsCount > 0) {
            rpsQuestionPaper.setIsAnswerKeyAvailable(true);
            rpsQuestionPaperRepository.save(rpsQuestionPaper);
        }
        if (!rpsQuestionAssociationList.isEmpty()) {
            rpsQuestionAssociationRepository.save(rpsQuestionAssociationList);
            LOGGER.info("QuestionAssociation has been updated in database...");
        }
    }

    /**
     * @param assessmentSections
     * @param assessmentTest
     * @return RpsQuestionPaper
     */

    private RpsQuestionPaper setQuestionPaperInfo(List<AssessmentSection> assessmentSections, AssessmentTest assessmentTest, RpsQuestionPaper rpsQuestionPaper) {

        String questionPaperCode = assessmentTest.getIdentifier();

        //RpsQuestionPaper rpsQuestionPaper = new RpsQuestionPaper();
        rpsQuestionPaper.setNumOfSections(assessmentSections.size());
        rpsQuestionPaper.setQpCode(questionPaperCode);
        rpsQuestionPaper.setCreationDate(Calendar.getInstance().getTime());
        rpsQuestionPaper.setQpTitle(assessmentTest.getTitle());
        rpsQuestionPaper.setQpStatus(RpsConstants.QPAPER_STATUS.AVAILABLE.toString());
        int setIndex = questionPaperCode.lastIndexOf(setCode);
        String setCode = questionPaperCode.substring(setIndex, questionPaperCode.length());
        rpsQuestionPaper.setSetCode(setCode);
        Integer totalNoQuestion = 0;
        Iterator<AssessmentSection> itAssmntPerSecForQNo = assessmentSections.iterator();

        while (itAssmntPerSecForQNo.hasNext()) {
            AssessmentSection ass_sec = itAssmntPerSecForQNo.next();
            List<SectionPart> sec_part = ass_sec.getSectionParts();
            totalNoQuestion += sec_part.size();
        }
        rpsQuestionPaper.setTotalQuestions(totalNoQuestion);
        rpsQuestionPaper.setIsAnswerKeyAvailable(false);
        return rpsQuestionPaper;
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

    /**
     * @param rpsQuestion
     * @param questionDirPath
     * @param assessment_item_ref
     * @param rpsQuestionAssociation
     * @param questionAssociationsSet
     * @param rpsQuestionAssociationList
     * @param qPackDetails
     * @return Correct Response
     * @throws RpsException
     */
    // using questionIdRpsQuestionMap and discarding same question for All question paper
    /*private String updateQuestionInfoUsingJqtiFile(RpsQuestion rpsQuestion, String questionDirPath, AssessmentItemRef assessment_item_ref,
			RpsQuestionAssociation rpsQuestionAssociation, Set<RpsQuestionAssociation> questionAssociationsSet, 
			List<RpsQuestionAssociation> rpsQuestionAssociationList, Map<String, List<RpsQuestion>> questionIdRpsQuestionMap) throws RpsException*/
    private QPPackAnswerMarksEntity updateQuestionInfoUsingJqtiFile(RpsQuestion rpsQuestion, String questionDirPath, AssessmentItemRef assessment_item_ref,
                                                                    RpsQuestionAssociation rpsQuestionAssociation, Set<RpsQuestionAssociation> questionAssociationsSet,
                                                                    List<RpsQuestionAssociation> rpsQuestionAssociationList, String localImagePath, QpdRpsExportEntity qPackDetails) throws RpsException {
        //qpdRpsQpToQuestionMap.getQpToQuestionMap().get(key)
        LOGGER.debug("updateQuestionInfoUsingJqtiFile()::IN for loading qti xml");
        QPPackAnswerMarksEntity qpMarksEntity = null;
        String questionFolderName = this.getquestionFolderName(questionDirPath);
        String questionDirFullPath = "";
        if (questionFolderName != null) {
            questionDirFullPath = questionDirPath + File.separator + questionFolderName + File.separator + assessment_item_ref.getHref();
        } else {
            questionDirFullPath = questionDirPath + File.separator + assessment_item_ref.getHref();
        }
        questionDirFullPath = FilenameUtils.separatorsToSystem(questionDirFullPath);
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(FilenameUtils.separatorsToSystem(questionDirFullPath));

//		Question is applicable for MCQ, Reading Comprehension and Fill in the blank..		

		if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.MULTIPLE_CHOICE_QUESTION.toString())) {

            // using questionIdRpsQuestionMap and discarding same question for All question paper
			/*qAnswer = this.readMCQQuestion(rpsQuestion,rpsQuestionAssociation,
					assessmentItem,questionAssociationsSet,rpsQuestionAssociationList, questionIdRpsQuestionMap);*/

            qpMarksEntity = this.readMCQQuestion(rpsQuestion, rpsQuestionAssociation,
                    assessmentItem, questionAssociationsSet, rpsQuestionAssociationList);
        }

		if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.READING_COMPREHENSION.toString())
				|| assessmentItem.getTitle().equalsIgnoreCase(QuestionType.SURVEY.toString())) {
            // using questionIdRpsQuestionMap and discarding same question for All question paper
			/*qAnswer = this.readRCQuestion(rpsQuestion,rpsQuestionAssociation,assessmentItem,questionAssociationsSet, rpsQuestionAssociationList, questionIdRpsQuestionMap);*/

            qpMarksEntity = this.readRCQuestion(rpsQuestion, rpsQuestionAssociation, assessmentItem, questionAssociationsSet, rpsQuestionAssociationList);

        }

		if (assessmentItem.getTitle().equalsIgnoreCase(QuestionType.DESCRIPTIVE_QUESTION.toString())) {

            // using questionIdRpsQuestionMap and discarding same question for All question paper
		/*	qAnswer = this.readDescriptiveQuestion(rpsQuestion, rpsQuestionAssociation, assessmentItem, questionAssociationsSet, rpsQuestionAssociationList, questionIdRpsQuestionMap);*/

            qpMarksEntity = this.readDescriptiveQuestion(rpsQuestion, rpsQuestionAssociation, questionDirFullPath, questionAssociationsSet, rpsQuestionAssociationList, localImagePath);
        }
        LOGGER.info(" QAnswer for qPaperId = {} " + rpsQuestion.getQuestId() + "is " + qpMarksEntity.getAnswer());
        LOGGER.debug("updateQuestionInfoUsingJqtiFile()::OUT for loading qti xml for question" + rpsQuestion.getQuestId());

        return qpMarksEntity;
    }

    /**
     * @param rpsQuestion
     * @param rpsQuestionAssociation
     * @param assessmentItem
     * @param questionAssociationsSet
     * @param rpsQuestionAssociationList
     * @return
     * @throws RpsException
     */

    // using questionIdRpsQuestionMap and discarding same question for All question paper
	/*private String readMCQQuestion(RpsQuestion rpsQuestion, RpsQuestionAssociation rpsQuestionAssociation,
			AssessmentItem assessmentItem, Set<RpsQuestionAssociation> questionAssociationsSet, List<RpsQuestionAssociation> rpsQuestionAssociationList, Map<String, List<RpsQuestion>> questionIdRpsQuestionMap) throws RpsException*/
    private QPPackAnswerMarksEntity readMCQQuestion(RpsQuestion rpsQuestion, RpsQuestionAssociation rpsQuestionAssociation,
                                                    AssessmentItem assessmentItem, Set<RpsQuestionAssociation> questionAssociationsSet, List<RpsQuestionAssociation> rpsQuestionAssociationList) throws RpsException {

        //	List<RpsQuestion> rpsQuestionList = new ArrayList<RpsQuestion>();
        QPPackAnswerMarksEntity qpMarksEntity = new QPPackAnswerMarksEntity();
        JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getMultipleChoiceQuestionInfo(assessmentItem);

        String questionCode = assessmentItem.getIdentifier();
        //	checking the particular question from database
        RpsQuestion rQuestion = rpsQuestionRepository.findByQuestIdAndRpsAssessment(questionCode, rpsQuestion.getRpsAssessment());

        if (rQuestion == null) {
            this.setRpsQuestionInfo(jqtiQuestionEntity, rpsQuestion);

            rpsQuestion = rpsQuestionRepository.save(rpsQuestion);

            qpMarksEntity.setAnswer(rpsQuestion.getQans());
            qpMarksEntity.setMarks(rpsQuestion.getScore());

            LOGGER.info("Multiple Choice Question updated in database, where QuestionCode :: {} " + rpsQuestion.getQuestId());

            //	Added into list for discarding the question for All question paper
//			rpsQuestionList.add(rpsQuestion);

            rpsQuestionAssociation.setRpsQuestion(rpsQuestion);
        } else {
            this.setRpsQuestionInfo(jqtiQuestionEntity, rQuestion);
            rpsQuestion = rpsQuestionRepository.save(rQuestion);
            qpMarksEntity.setAnswer(rpsQuestion.getQans());
            qpMarksEntity.setMarks(rpsQuestion.getScore());
            LOGGER.info("Multiple Choice Question updated in database, where QuestionCode :: {} " + rpsQuestion.getQuestId());
            rpsQuestionAssociation.setRpsQuestion(rQuestion);
        }

        if (!questionAssociationsSet.contains(rpsQuestionAssociation)) {
            rpsQuestionAssociationList.add(rpsQuestionAssociation);
            questionAssociationsSet.add(rpsQuestionAssociation);
            LOGGER.info("new rpsQuestionAssociation added = {} " + rpsQuestionAssociation);
        }
        // using questionIdRpsQuestionMap and discarding same question for All question paper
        //	questionIdRpsQuestionMap.put(rpsQuestion.getQuestId(), rpsQuestionList);
        return qpMarksEntity;
    }

    /**
     * @param rpsQuestion
     * @param rpsQuestionAssociation
     * @param assessmentItem
     * @param questionAssociationsSet
     * @param rpsQuestionAssociationList
     * @return
     * @throws RpsException
     */

    // using questionIdRpsQuestionMap and discarding same question for All question paper
	/*private String readRCQuestion(RpsQuestion rpsQuestion,	RpsQuestionAssociation rpsQuestionAssociation, 
			AssessmentItem assessmentItem, Set<RpsQuestionAssociation> questionAssociationsSet,List<RpsQuestionAssociation> rpsQuestionAssociationList, Map<String, List<RpsQuestion>> questionIdRpsQuestionMap) throws RpsException 
*/
    private QPPackAnswerMarksEntity readRCQuestion(RpsQuestion rpsQuestion, RpsQuestionAssociation rpsQuestionAssociation,
                                                   AssessmentItem assessmentItem, Set<RpsQuestionAssociation> questionAssociationsSet, List<RpsQuestionAssociation> rpsQuestionAssociationList) throws RpsException

    {
        QPPackAnswerMarksEntity qpMarksEntity = new QPPackAnswerMarksEntity();
        Double totalMarksPerRcQ = 0.0;
//		List<RpsQuestionAssociation> rpsQuestionAssociationRCList = new ArrayList<RpsQuestionAssociation>();
        short childSequenceNumber = 1;

//		List<RpsQuestion> rpsQuestionList = new ArrayList<RpsQuestion>();

        //String questionCode = assessmentItem.getIdentifier();
        String questionPath = rpsQuestion.getQtiFileName();
        int lastIndexOfSeparator = questionPath.lastIndexOf("\\");
        if (lastIndexOfSeparator < 0)
            lastIndexOfSeparator = questionPath.lastIndexOf("/");

        String questionCode = rpsQuestion.getQtiFileName().substring(lastIndexOfSeparator + 1, rpsQuestion.getQtiFileName().length() - 4);

        //	checking the particular question from database
        RpsQuestion rQuestion = rpsQuestionRepository.findByQuestIdAndRpsAssessment(questionCode, rpsQuestion.getRpsAssessment());
        if (rQuestion == null) {
            rpsQuestion.setCreationDate(Calendar.getInstance().getTime());
            rpsQuestion.setQuestId(questionCode);
            rpsQuestion.setQuestionType(assessmentItem.getTitle());

            rpsQuestion = rpsQuestionRepository.save(rpsQuestion);
            LOGGER.info("RC Question has been updated in database, where QuestionCode :: {} " + rpsQuestion.getQuestId());
            rpsQuestionAssociation.setRpsQuestion(rpsQuestion);
        } else {
            rpsQuestion = rQuestion;
            rpsQuestionAssociation.setRpsQuestion(rpsQuestion);
        }
//		Added into list for discarding the question for All question paper
//		rpsQuestionList.add(rpsQuestion);

        if (!questionAssociationsSet.contains(rpsQuestionAssociation)) {
            rpsQuestionAssociationList.add(rpsQuestionAssociation);
            questionAssociationsSet.add(rpsQuestionAssociation);
            LOGGER.info("new rpsQuestionAssociation added = {} " + rpsQuestionAssociation);
        }


        List<JQTIQuestionEntity> jqtiQuestionEntities = jqtiApolloUtility.getReadingComprehensionInfo(assessmentItem);

        if (jqtiQuestionEntities != null) {
            for (JQTIQuestionEntity jqtiQuestionEntity : jqtiQuestionEntities) {
                RpsQuestion childRpsQuestion = new RpsQuestion();
                String childQuestionCode = jqtiQuestionEntity.getQpCode();
                RpsQuestion rChildQuestion = rpsQuestionRepository.findByQuestIdAndRpsAssessment(childQuestionCode, rpsQuestion.getRpsAssessment());
                if (rChildQuestion == null) {
                    this.setRpsQuestionInfo(jqtiQuestionEntity, childRpsQuestion);
                    childRpsQuestion.setchildQuestionSeqNumber(childSequenceNumber);
                    childRpsQuestion.setParentqID(rpsQuestion);
                    childRpsQuestion.setRpsAssessment(rpsQuestion.getRpsAssessment());
                    childRpsQuestion.setQtiFileName(rpsQuestion.getQtiFileName());
                    childSequenceNumber++;

                    childRpsQuestion = rpsQuestionRepository.save(childRpsQuestion);
                    totalMarksPerRcQ = totalMarksPerRcQ + childRpsQuestion.getScore();
                    LOGGER.info("RC Child Question has been updated in database, where QuestionCode :: {} " + childRpsQuestion.getQuestId());
                } else {
                    this.setRpsQuestionInfo(jqtiQuestionEntity, rChildQuestion);
                    totalMarksPerRcQ = totalMarksPerRcQ + rChildQuestion.getScore();
                    childRpsQuestion = rpsQuestionRepository.save(rChildQuestion);
                }

//				Added into list for discarding the question for All question paper
//				rpsQuestionList.add(childRpsQuestion);

                if (childRpsQuestion.getQans() != null) {
                    qpMarksEntity.setAnswer(rpsQuestion.getQans());
                }

                RpsQuestionAssociation rpsChildQuestAssociation = this.setRpsQuestionAssociationInfo(rpsQuestionAssociation.getRpsQuestionPaper(), childRpsQuestion, rpsQuestionAssociation.getRpsQpSection());
                if (!questionAssociationsSet.contains(rpsChildQuestAssociation)) {
                    rpsQuestionAssociationList.add(rpsChildQuestAssociation);
                    questionAssociationsSet.add(rpsChildQuestAssociation);
                    LOGGER.info("new rpsQuestionAssociation added = {} " + rpsChildQuestAssociation);
                }
            }

        }
        // using questionIdRpsQuestionMap and discarding same question for All question paper
//		questionIdRpsQuestionMap.put(rpsQuestion.getQuestId(), rpsQuestionList);
        qpMarksEntity.setMarks(totalMarksPerRcQ);
        return qpMarksEntity;
    }


    // using questionIdRpsQuestionMap and discarding same question for All question paper
	/*private String readDescriptiveQuestion(RpsQuestion rpsQuestion, RpsQuestionAssociation rpsQuestionAssociation,
			AssessmentItem assessmentItem, Set<RpsQuestionAssociation> questionAssociationsSet, List<RpsQuestionAssociation> rpsQuestionAssociationList, Map<String, List<RpsQuestion>> questionIdRpsQuestionMap) throws RpsException
*/
    private QPPackAnswerMarksEntity readDescriptiveQuestion(RpsQuestion rpsQuestion, RpsQuestionAssociation rpsQuestionAssociation,
                                                            String questionPath, Set<RpsQuestionAssociation> questionAssociationsSet, List<RpsQuestionAssociation> rpsQuestionAssociationList, String localImagePath) throws RpsException

    {
        QPPackAnswerMarksEntity qpMarksEntity = new QPPackAnswerMarksEntity();
        AssessmentItem assessmentItem = jqtiUtility.loadAssessmentItemFile(questionPath);
        String questionCode = assessmentItem.getIdentifier();
        File imagePath = new File(localImagePath + File.separator + questionCode + File.separator + RpsConstants.images);

        //	List<RpsQuestion> rpsQuestionList = new ArrayList<RpsQuestion>();

        JQTIQuestionEntity jqtiQuestionEntity = jqtiApolloUtility.getDescriptiveQuestionInfo(assessmentItem, questionPath, imagePath);


        //	checking the particular question from database
        RpsQuestion rQuestion = rpsQuestionRepository.findByQuestIdAndRpsAssessment(questionCode, rpsQuestion.getRpsAssessment());

        if (rQuestion == null) {
            this.setRpsQuestionInfo(jqtiQuestionEntity, rpsQuestion);

            rpsQuestion = rpsQuestionRepository.save(rpsQuestion);

            qpMarksEntity.setAnswer(rpsQuestion.getQans());
            qpMarksEntity.setMarks(rpsQuestion.getScore());
            LOGGER.info("Descriptive Question updated in database, where QuestionCode :: {} " + rpsQuestion.getQuestId());

            //	Added into list for discarding the question for All question paper
//				rpsQuestionList.add(rpsQuestion);

            rpsQuestionAssociation.setRpsQuestion(rpsQuestion);
        } else {
            this.setRpsQuestionInfo(jqtiQuestionEntity, rQuestion);
            rpsQuestion = rpsQuestionRepository.save(rQuestion);
            qpMarksEntity.setAnswer(rpsQuestion.getQans());
            qpMarksEntity.setMarks(rpsQuestion.getScore());
            LOGGER.info("Descriptive Question updated in database, where QuestionCode :: {} " + rpsQuestion.getQuestId());
            rpsQuestionAssociation.setRpsQuestion(rQuestion);
        }

        if (!questionAssociationsSet.contains(rpsQuestionAssociation)) {
            rpsQuestionAssociationList.add(rpsQuestionAssociation);
            questionAssociationsSet.add(rpsQuestionAssociation);
            LOGGER.info("new rpsQuestionAssociation added = {} " + rpsQuestionAssociation);
        }
        // using questionIdRpsQuestionMap and discarding same question for All question paper
//		questionIdRpsQuestionMap.put(rpsQuestion.getQuestId(), rpsQuestionList);
        return qpMarksEntity;

    }

    /**
     * @param jqtiQuestionEntity
     * @return RpsQuestion
     */

    private RpsQuestion setRpsQuestionInfo(JQTIQuestionEntity jqtiQuestionEntity, RpsQuestion rpsQuestion) {
        rpsQuestion.setQuestId(jqtiQuestionEntity.getQpCode());
        rpsQuestion.setCreationDate(Calendar.getInstance().getTime());
        rpsQuestion.setQuestionType(jqtiQuestionEntity.getQuestionTitle());
        rpsQuestion.setQans(jqtiQuestionEntity.getResponse());
        rpsQuestion.setScore(jqtiQuestionEntity.getMarks());
        rpsQuestion.setNegativeScore(jqtiQuestionEntity.getNegativeMarks());
        rpsQuestion.setQtext(jqtiQuestionEntity.getText());
        rpsQuestion.setTopic(jqtiQuestionEntity.getTopic());
        rpsQuestion.setDifficultyLevel(jqtiQuestionEntity.getDifficultyLevel());
        return rpsQuestion;
    }

    private void copyImagesFolder(String questionFolderName, String packdownloadPathWithImages) throws IOException {
		LOGGER.info("copy all images from Qpack to local images folder, SRC:: " + questionFolderName + " DEST:: "
				+ packdownloadPathWithImages);
		String[] directories = null;
		File imageFolderName = null;
		String[] resourcesWithoutLang = new File(questionFolderName).list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().contains(RpsConstants.resources);
			}
		});

		if (resourcesWithoutLang.length == 0) {
			// iterate thru all folders and get images
			directories = new File(questionFolderName).list(new FilenameFilter() {
				@Override
				public boolean accept(File current, String name) {
					return new File(current, name).isDirectory();
				}
			});
		} else {
			questionFolderName =
					questionFolderName + File.separator + RpsConstants.resources + File.separator + RpsConstants.images;
		}

		if (directories != null && directories.length > 0) {
			for (String LanguageFolderName : directories) {
				imageFolderName = new File(questionFolderName + File.separator + LanguageFolderName + File.separator
						+ RpsConstants.resources + File.separator
						+ RpsConstants.images);
				copyImageFilesToDestination(packdownloadPathWithImages, imageFolderName);
			}
		} else {
			imageFolderName = new File(questionFolderName);
			copyImageFilesToDestination(packdownloadPathWithImages, imageFolderName);
		}
		LOGGER.info("done all copying images from Qpack to local images folder, SRC:: " + questionFolderName
				+ " DEST:: " + packdownloadPathWithImages);
    }

	private void copyImageFilesToDestination(String packdownloadPathWithImages, File imageFolderName)
			throws IOException {
		Collection<File> listOfFiles =
				FileUtils.listFiles(imageFolderName, new String[] { "jpg", "bmp", "gif", "png" }, true);
		LOGGER.info("list of all image files from Qpack :: " + listOfFiles);
		for (File file : listOfFiles) {
			if (file.isFile() && file.getAbsolutePath().contains(RpsConstants.images)) {
				LOGGER.info("copying an image file from Qpack to local images folder, SRC:: " + file.getAbsolutePath()
						+ " DEST:: " + packdownloadPathWithImages);
				String baseNameQpack =
						FilenameUtils.getBaseName(new File(packdownloadPathWithImages).getAbsolutePath());
				int index = file.getAbsolutePath().lastIndexOf(baseNameQpack);
				String fileName = file.getAbsolutePath().substring(index);
				String fileNamepackdownloadPathWithImages = packdownloadPathWithImages
						+ FilenameUtils.getFullPathNoEndSeparator(fileName).replaceAll(baseNameQpack, "");
				FileUtils.copyFileToDirectory(file, new File(fileNamepackdownloadPathWithImages));
			}
		}
	}
}

