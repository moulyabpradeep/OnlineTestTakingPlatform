package com.merittrac.apollo.acs.services;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.merittrac.apollo.acs.audit.StatsCollector;
import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.CandidateActionsEnum;
import com.merittrac.apollo.acs.dataobject.AuditCandidateResponseDO;
import com.merittrac.apollo.acs.dataobject.ICandidateActionDO;
import com.merittrac.apollo.acs.dataobject.ImageMissingAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.ChangeOptionAuditDO;
import com.merittrac.apollo.acs.dataobject.tp.ImageMissingDO;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.acs.utility.AuditTrailLogger;
import com.merittrac.apollo.acs.utility.TimeUtil;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/*
 * Will ensure all the auditing related API reside in a common location. Mostly act's like an adapter by converting the
 * data objects received from TP to audit objects
 */
public class CandidateAuditService extends BasicService implements ICandidateAuditService {
	private static CandidateService cs = null;
	private static CandidateAuditService candidateAuditService = null;
	private static Logger gen_logger = LoggerFactory.getLogger(ACSConstants.GENERAL_LOGGER);
	static {
		cs = CandidateService.getInstance();
	}

	private CandidateAuditService() {

	}

	/**
	 * To access the static service
	 * 
	 * @return
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static final CandidateAuditService getInstance() {
		if (candidateAuditService == null) {
			synchronized (CandidateService.class) {
				if (candidateAuditService == null) {
					candidateAuditService = new CandidateAuditService();
				}
			}
		}
		return candidateAuditService;
	}

	public boolean auditCandidateAction(ICandidateActionDO auditCandidateAction) throws GenericDataModelException,
			CandidateRejectedException {
		if (auditCandidateAction instanceof ImageMissingAuditDO) {
			buildImageMissingAuditDO((ImageMissingAuditDO) auditCandidateAction);
		}
		if (auditCandidateAction instanceof AuditCandidateResponseDO) {
			buildChangeOptionAuditDO((AuditCandidateResponseDO) auditCandidateAction);
		}
		return true;
	}

	// TODO :: Needed to write seperate method for each audit action because PHP-Java util doesn't support
	// methods with interface as an argument
	public boolean auditCandidateActionImageMissing(ImageMissingAuditDO auditCandidateAction)
			throws GenericDataModelException, CandidateRejectedException {
		long startTime = StatsCollector.getInstance().begin();
		buildImageMissingAuditDO((ImageMissingAuditDO) auditCandidateAction);
		StatsCollector.getInstance().log(startTime, "auditCandidateActionImageMissing", "CandidateAuditService",
				auditCandidateAction.getCandID());
		return true;
	}

	public boolean auditCandidateActionChangeOption(AuditCandidateResponseDO auditCandidateAction)
			throws GenericDataModelException, CandidateRejectedException {
		buildChangeOptionAuditDO((AuditCandidateResponseDO) auditCandidateAction);
		return true;
	}

	/*
	 * Builds ImageMissingDO and accordingly logs an audit message for the action
	 */
	private void buildImageMissingAuditDO(ImageMissingAuditDO imado) throws GenericDataModelException,
			CandidateRejectedException {
		// int candID = imado.getCandID();
		// int batchId = bs.getBatchIdbyCandId(candID);
		// int batchId = imado.getBatchID();
		// CandidateStatusTO candidateStatus = (CandidateStatusTO) cs.getCandidateStatus(candID, batchId);
		// String assessmentTypeValue = cdeas.getAssessmentCodebyCandIDAndBatchID(candID, batchId);
		long mill = imado.getDurationMillisecs();

		ImageMissingDO imgmissDO = new ImageMissingDO();
		// imgmissDO.setAssessmentID(assessmentTypeValue);
		// imgmissDO.setCandID(candID);
		// imgmissDO.setCandApplicationNumber(cs.getApplicationNumberByCandidateId(candID));
		imgmissDO.setBatchCandidateAssociation(imado.getBatchCandidateAssociationId());
		// imgmissDO.setBatchCode(bs.getBatchCodebyBatchId(batchId));
		imgmissDO.setHostAddress(imado.getIp());
		imgmissDO.setMissingImages(imado.getMissingImages());
		imgmissDO.setqID(imado.getqID());
		imgmissDO.setQpID(imado.getQpID());
		imgmissDO.setSecID(imado.getSecID());
		imgmissDO.setTpQID(imado.getTpQId());// new param
		imgmissDO.setClientTime(TimeUtil.convertTimeAsString(imado.getClientTime(), TimeUtil.DISPLAY_DATE_FORMAT));
		imgmissDO.setActionType(CandidateActionsEnum.MISSINGIMAGE);
		imgmissDO.setQuestionType(imado.getQuestionType());
		imgmissDO.setParentQuestionId(imado.getParentQuestionId());

		if (imado.getDurationMillisecs() != 0)
			imgmissDO.setActionTime(TimeUtil.formatTime(mill));
		AuditTrailLogger.auditSave(imgmissDO);
	}

	/*
	 * Builds audit changing options into candidate audit trail.
	 */
	private void buildChangeOptionAuditDO(AuditCandidateResponseDO auditCandidateResponse)
			throws GenericDataModelException, CandidateRejectedException {
		Integer bcaid = auditCandidateResponse.getBatchCandidateAssociationId();
		// AcsCandidateStatus candidateStatus = (AcsCandidateStatus) cs.getCandidateStatus(bcaid);
		String assessmentTypeValue = auditCandidateResponse.getAssessmentCode();
		long mill = auditCandidateResponse.getDurationMillisecs();

		ChangeOptionAuditDO changeOptionAuditDO = new ChangeOptionAuditDO();
		changeOptionAuditDO.setAssessmentID(assessmentTypeValue);
		changeOptionAuditDO.setBatchCandidateAssociation(bcaid);
		String candidateIdentifier = cs.getCandidateIdentifierFromCandId(auditCandidateResponse.getCandID());
		changeOptionAuditDO.setCandApplicationNumber(candidateIdentifier);
		changeOptionAuditDO.setBatchCandidateAssociation(auditCandidateResponse.getBatchCandidateAssociationId());
		// changeOptionAuditDO.setBatchID(batchId);
		// changeOptionAuditDO.setBatchCode(bs.getBatchCodebyBatchId(batchId));
		changeOptionAuditDO.setHostAddress(auditCandidateResponse.getIp());
		changeOptionAuditDO.setPrevOptionID(auditCandidateResponse.getPreviousOption());
		changeOptionAuditDO.setNewOptionID(auditCandidateResponse.getCurrentOption());
		changeOptionAuditDO.setqID(auditCandidateResponse.getqID());
		changeOptionAuditDO.setQpID(auditCandidateResponse.getQpID());
		changeOptionAuditDO.setSecID(auditCandidateResponse.getSecID());
		changeOptionAuditDO.setClientTime(TimeUtil.convertTimeAsString(auditCandidateResponse.getClientTime(),
				TimeUtil.DISPLAY_DATE_FORMAT));
		changeOptionAuditDO.setQuestionType(auditCandidateResponse.getQuestionType());
		changeOptionAuditDO.setParentQuestionId(auditCandidateResponse.getParentQuestionId());
		changeOptionAuditDO.setTpQID(auditCandidateResponse.getTpQId());

		if (auditCandidateResponse.getPreviousOption() != null) {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.SWITCHOPTION);
		} else if (auditCandidateResponse.getPreviousOption() == null) {
			changeOptionAuditDO.setActionType(CandidateActionsEnum.OPTIONSELECTED);
		}

		if (auditCandidateResponse.getDurationMillisecs() != 0)
			changeOptionAuditDO.setActionTime(TimeUtil.formatTime(mill));
		AuditTrailLogger.auditSave(changeOptionAuditDO);
	}

	public void auditCandidateActions(int batchCandidateAssociation, String msg) throws GenericDataModelException {
//		AcsCandidatAudits candidateAuditsTO = new AcsCandidatAudits();
//		candidateAuditsTO.setAcsbatchcandidateassociation(batchCandidateAssociation);
//		candidateAuditsTO.setMessage(msg);
//		candidateAuditsTO.setTimestamp(Calendar.getInstance());
//		session.persist(candidateAuditsTO);
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("bcaid", batchCandidateAssociation);
		params.put("message", msg);
		params.put("timestamp", Calendar.getInstance());
		String query = ACSQueryConstants.QUERY_INSERT_CAND_AUDIT_DATA;
		DateFormat dateFormat = new SimpleDateFormat(ACSConstants.AUDIT_LOG_TABLE_NAME_FORMAT);
		Calendar date = Calendar.getInstance();
		query = query.replace("seqdate", dateFormat.format(date.getTime()));
		try {
			session.insertByNativeSql(query, params);
		} catch (HibernateException ex) {
			gen_logger.error("Unable to save Audit Data : {} : {} : {}", batchCandidateAssociation, msg);
		}
	}
}
