package com.merittrac.apollo.acs.services;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.DocumentException;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.ResultProcReportTO;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.reports.ProvisionalCertificateEntity;
import com.merittrac.apollo.reports.ProvisionalCertificatePDFGenerator;

/**
 * @author Moulya_P
 * 
 */
public class CandidatePDFGeneratorService implements ICandidatePDFGeneratorService {
	private static Logger logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	private static BatchCandidateAssociationService bca = null;
	private static AuthService as = null;
	private static CDEAService cdeas = null;
	private static ResultProcStatusService rpss = null;
	static {
		if (bca == null)
			bca = new BatchCandidateAssociationService();
		if (rpss == null)
			rpss = new ResultProcStatusService();
		if (as == null)
			as = new AuthService();
		if (cdeas == null)
			cdeas = new CDEAService();
	}

	/**
	 * generates CandidateScoreCard from batchId
	 * 
	 * @param batchID
	 * @throws GenericDataModelException
	 */
	@Override
	public boolean generateProvisionalScoreCardForBatch(String batchCode) throws GenericDataModelException {
		if (batchCode.isEmpty() || batchCode.equals("0")) {
			logger.error("batchId cannot be 0");
			return false;
		} else {
			logger.info("generate Provisional Score Card for the batch = " + batchCode);
			List<ProvisionalCertificateEntity> provisionalCertificateEntities =
					bca.getProvisionalCertificateEntityByBatchId(batchCode);
			try {
				logger.info("Provisional Score Card entities = " + provisionalCertificateEntities);
				if (provisionalCertificateEntities != null) {
					// List<Integer> listOfcandidateIds=new ArrayList<>();
					for (ProvisionalCertificateEntity provisionalCertificateEntity : provisionalCertificateEntities) {
						// get customer logo and populate
						provisionalCertificateEntity.setCustomerLogoPath(as.getCustomerLogoPathLocation(
								provisionalCertificateEntity.getAssessmentCode(),
								provisionalCertificateEntity.getBatchCode()));

						// get apollo_home and create pdf
						File path =
								new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
										+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
										+ ACSConstants.SCORECARD + File.separator
										+ provisionalCertificateEntity.getBatchCode());
						if (!path.exists()) {
							path.mkdirs();
						}
						String fileName =
								path.getPath() + File.separator + provisionalCertificateEntity.getMembershipNum()
										+ ACSConstants.SCORE_CANDIDATE_PDF;
						// generate pdf's only for the candidates where pdf isnt generated
						ResultProcReportTO reportTO =
								rpss.findByBatchIDCandID(provisionalCertificateEntity.getBatchCode(),
										provisionalCertificateEntity.getCandidateId());
						if (reportTO != null && reportTO.getPdfGenerationTime() == null) {
							ProvisionalCertificatePDFGenerator.pdfGenerator(fileName, provisionalCertificateEntity);
							// update for candidates whose pdf's are generated
							rpss.updatePdfGeneratedTimeByBatchIdCandidateId(
									provisionalCertificateEntity.getBatchCode(),
									provisionalCertificateEntity.getCandidateId(), fileName);
							// listOfcandidateIds.add(provisionalCertificateEntity.getCandidateId());
							logger.info("generated Provisional Score Card for the candidate = "
									+ provisionalCertificateEntity.getMembershipNum());
						} else {
							logger.info("Already Provisional Score Card is generated for the candidate = "
									+ provisionalCertificateEntity.getMembershipNum());
						}
					}
					// if(listOfcandidateIds.size()!=0 && batchID!=0)
					// rpss.updatePdfGeneratedTimeByBatchIdCandidateIds(batchID, listOfcandidateIds);
				} else
					logger.info("No Provisional Score Cards generated as there are no candidates");
			} catch (DocumentException | IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	/**
	 * generates CandidateScoreCard from Candidates
	 * 
	 * @param candidateIds
	 * @throws GenericDataModelException
	 */
	@Override
	public boolean generateProvisionalScoreCardForCandidates(String batchCode, List<Integer> candidateIds,
			Boolean isPdfToBePrinted) throws GenericDataModelException {
		logger.debug("generateProvisionalScoreCardForCandidates for batchCode:{} candidateIds:{} ipdfToBePrinted:{}",
				batchCode, candidateIds, isPdfToBePrinted);
		if (candidateIds.size() == 0) {
			logger.error("candidateIds cannot be empty");
			return false;
		} else {
			logger.info("generate Provisional Score Card for the candidateIds = " + candidateIds);
			List<ProvisionalCertificateEntity> provisionalCertificateEntities =
					bca.getProvisionalCertificateEntityByCandidateIds(batchCode, candidateIds);
			try {
				logger.info("Provisional Score Card entities = " + provisionalCertificateEntities);
				if (provisionalCertificateEntities != null) {
					// List<Integer> listOfcandidateIds=new ArrayList<>();
					for (ProvisionalCertificateEntity provisionalCertificateEntity : provisionalCertificateEntities) {
						// get customer logo and populate
						provisionalCertificateEntity.setCustomerLogoPath(as.getCustomerLogoPathLocation(
								provisionalCertificateEntity.getAssessmentCode(),
								provisionalCertificateEntity.getBatchCode()));

						// get apollo_home and create pdf
						File path =
								new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
										+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
										+ ACSConstants.SCORECARD + File.separator
										+ provisionalCertificateEntity.getBatchCode());
						if (!path.exists()) {
							path.mkdirs();
						}
						String fileName =
								path.getPath() + File.separator + provisionalCertificateEntity.getMembershipNum()
										+ ACSConstants.SCORE_CANDIDATE_PDF;
						ResultProcReportTO reportTO =
								rpss.findByBatchIDCandID(provisionalCertificateEntity.getBatchCode(),
										provisionalCertificateEntity.getCandidateId());
						// generate pdf's only for the candidates where pdf isnt generated
						if (reportTO != null && reportTO.getPdfGenerationTime() == null) {
							ProvisionalCertificatePDFGenerator.pdfGenerator(fileName, provisionalCertificateEntity);
							if (isPdfToBePrinted) {
								logger.debug("Printing PDF: {}", fileName);
								boolean isPrintSuccess = printPdf(fileName);
								logger.debug("Printing is successful:{}", isPrintSuccess);
							}
							// update for candidates whose pdf's are generated
							rpss.updatePdfGeneratedTimeByBatchIdCandidateId(
									provisionalCertificateEntity.getBatchCode(),
									provisionalCertificateEntity.getCandidateId(), fileName);
							// listOfcandidateIds.add(provisionalCertificateEntity.getCandidateId());
							logger.info("generated Provisional Score Card for the candidate = "
									+ provisionalCertificateEntity.getMembershipNum());
						} else {
							logger.info("Already Provisional Score Card is generated for the candidate = "
									+ provisionalCertificateEntity.getMembershipNum());
						}
					}
					// if(listOfcandidateIds.size()!=0 && batchId!=0)
					// rpss.updatePDFReportGenByBatchIdCandidateIds(batchId, listOfcandidateIds);
				} else
					logger.info("No Provisional Score Cards generated as there are no candidates");
			} catch (DocumentException | IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}

	private boolean printPdf(String fileName) {
		boolean isPrintSuccesfull = false;
		try {
			Desktop.getDesktop().print(new File(fileName));
			isPrintSuccesfull = true;

		} catch (IOException e) {
			logger.error("IOException while executing printPdf...", e);
		}
		return isPrintSuccesfull;
	}

	public String viewProvisionalScoreCardForCandidate(String batchCode, int candidateId)
			throws GenericDataModelException {
		if (candidateId == 0 || batchCode.equals("0") || batchCode.isEmpty()) {
			logger.error("Either batchCode or candidateId is 0");
			return "";
		} else {
			AcsBatchCandidateAssociation batchCandidateDO =
					bca.getBatchCandidateAssociationBybatchIdAndCandidateId(batchCode, candidateId);
			if (batchCandidateDO != null) {
				logger.info(
						"view Provisional Score Card for the candidate = {} of batch = {} for batchCandidateDO = {} "
								+ candidateId, batchCode, batchCandidateDO);
				File file =
						new File(System.getenv("APOLLO_HOME") + File.separator + ACSConstants.ACS_MODULE_DIR
								+ File.separator + ACSConstants.ACS_DOWNLOAD_DIR + File.separator
								+ ACSConstants.SCORECARD + File.separator + batchCandidateDO.getBatchCode()
								+ File.separator + batchCandidateDO.getCandidateLogin()
								+ ACSConstants.SCORE_CANDIDATE_PDF);
				if (file.exists()) {
					logger.info(
							"view Provisional Score Card for the candidate = {} in the filepath = {}" + candidateId,
							file.getPath());
					return file.getPath();
				} else {
					logger.info("No Provisional Score Card generated for the candidate = " + candidateId);
					return "";
				}
			} else {
				logger.info("No Candidate = {} exists for this batch = {} " + candidateId, batchCode);
				return "";
			}
		}
	}

}
