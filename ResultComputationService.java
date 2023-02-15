/**
 * 
 */
package com.merittrac.apollo.rps.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.merittrac.apollo.data.entity.RpsAcsServer;
import com.merittrac.apollo.data.entity.RpsAssessment;
import com.merittrac.apollo.data.entity.RpsBatch;
import com.merittrac.apollo.data.entity.RpsCumulativeResponses;
import com.merittrac.apollo.data.entity.RpsEvent;
import com.merittrac.apollo.data.entity.RpsQuestionPaper;
import com.merittrac.apollo.data.service.RpsAcsServerServices;
import com.merittrac.apollo.data.service.RpsAssessmentService;
import com.merittrac.apollo.data.service.RpsBatchAcsAssociationService;
import com.merittrac.apollo.data.service.RpsBatchService;
import com.merittrac.apollo.data.service.RpsCumulativeResponsesService;
import com.merittrac.apollo.data.service.RpsEventService;
import com.merittrac.apollo.data.service.RpsQuestionPaperService;
import com.merittrac.apollo.rps.common.RpsConstants;
import com.merittrac.apollo.rps.common.RpsException;
import com.merittrac.apollo.rps.core.ResultProcessor;

/**
 * Service class interfaces with php and provides a set of apis to begin the result processing.
 * API's include results processing at event, at batch, at an assessment, or at a set of batches.
 * @author Mohammed_A
 *
 */
public class ResultComputationService {
	
	@Autowired
	ResultProcessor resultProcessor;
	
	@Autowired
	RpsEventService rpsEventService;
	
	@Autowired
	RpsBatchService rpsBatchService; 
	
	@Autowired
	RpsAssessmentService rpsAssessmentService;
	
	@Autowired
	RpsAcsServerServices rpsAcsServerServices;

	@Autowired
	RpsQuestionPaperService rpsQuestionPaperService;
	
	@Autowired
	RpsCumulativeResponsesService rpsCumulativeResponsesService;
	
	@Autowired
	RpsBatchAcsAssociationService rpsBatchAcsAssociationService;
	
	private Logger logger = LoggerFactory.getLogger(ResultComputationService.class);
	
	/**
	 * Service api to compute results for an event
	 * The api checks if the eventCode exists before proceeding.
	 * @param eventCode
	 * @throws RpsException 
	 */
	public void computeResultForTheEvent(String eventCode) throws RpsException{
		logger.debug("--IN-- for eventCode::"+eventCode);
		RpsEvent event = checkForEvent(eventCode);		
		logger.info("Found!! eventName::"+event.getEventName());
		resultProcessor.processResultsForEvent(eventCode);
		logger.debug("--OUT--");		
	}
	
	/**
	 * Service api to compute results for an assessment belonging to an event.
	 * The api checks if the eventCode and the (batchCode, acsServerCode) exists before proceeding.
	 * @param eventCode
	 * @param assessmentCode
	 * @throws RpsException 
	 */
	public void computeResultForTheAssessment(String eventCode, String assessmentCode) throws RpsException{
		logger.debug("--IN-- for eventCode::"+eventCode+" assessmentCode::"+assessmentCode);
		RpsEvent event = checkForEvent(eventCode);
		RpsAssessment assessment = checkForAssessment(assessmentCode);
		logger.info("Found!! eventName::"+event.getEventName()+"; assessmentName::"+assessment.getAssessmentName());
		resultProcessor.processResultsForAssessment(eventCode, assessmentCode);
		logger.debug("--OUT--");
	}
	
	/**
	 * Service api to compute results for a batch belonging to an event.
	 * The api checks if the eventCode and the (batchCode, acsServerCode) exists before proceeding.
	 * @param eventCode
	 * @param batchCode
	 * @throws RpsException
	 */
	public void computeResultForTheBatch(String eventCode, String batchCode, String acsServerCode) throws RpsException{
		logger.debug("--IN-- for eventCode::"+eventCode+"; batchCode::"+batchCode+"; acsCode::"+acsServerCode);
		RpsEvent event = checkForEvent(eventCode);
		RpsBatch batch = checkForBatch(batchCode);
		RpsAcsServer acsServer = checkForAcsServer(acsServerCode);		
		logger.info("Found!! eventName::"+event.getEventName()+"; batchCode::"+batch.getBatchCode()+"; acsCode::"+acsServer.getAcsServerId());				
		resultProcessor.processResultsForBatch(eventCode, batch.getBid(), acsServer.getAcsId());		
		logger.debug("--OUT--");
	}
	
	/**
	 * Service api to compute results for a batch/assessment combination belonging to an event.
	 * The Api checks if the eventCode and the (batchCode, acsServerCode) and the assessmentCode exists before proceeding.
	 * @param eventCode
	 * @param batchCode
	 * @param assessmentCode
	 * @throws RpsException
	 */
	public void computeResultForTheBatchAndAssessment(String eventCode, String batchCode, String acsServerCode, String assessmentCode) throws RpsException{
		logger.debug("--IN-- for eventCode::"+eventCode+"; batchCode::"+batchCode+"; acsCode::"+acsServerCode+"; assessmentCode::"+assessmentCode);
		RpsEvent event = checkForEvent(eventCode);
		RpsBatch batch = checkForBatch(batchCode);
		RpsAcsServer acsServer = checkForAcsServer(acsServerCode);		
		RpsAssessment assessment = checkForAssessment(assessmentCode);
		logger.info("Found!! eventName::"+event.getEventName()+"; batchCode::"+batch.getBatchCode()+"; acsCode::"+acsServer.getAcsServerId()+"; assessmentCode::"+assessment.getAssessmentCode());				
		resultProcessor.processResultsForBatchAndAssessment(eventCode, batch.getBid(), acsServer.getAcsId(), assessmentCode);
		logger.debug("--OUT--");
	}
	
	/**
	 * Service api to compute results for a batch/Qp combination belonging to an event.
	 * The Api checks if the eventCode and the (batchCode, acsServerCode) and the assessmentCode/Qp exists before proceeding.
	 * @param eventCode
	 * @param batchCode
	 * @param assessmentCode
	 * @throws RpsException
	 */
	public void computeResultForTheBatchAndQp(String eventCode, String batchCode, String acsServerCode, String assessmentCode, Integer qpId) throws RpsException{
		logger.debug("--IN-- for eventCode::"+eventCode+"; batchCode::"+batchCode+"; acsCode::"+acsServerCode+"; assessmentCode::"+assessmentCode);
		//update result computaion status to in progress in cumulative response table
		String resultComputationStatus;
		RpsBatch rpsBatch= rpsBatchService.getBatchByBatchCodeAndEvent(batchCode, eventCode);
		RpsAcsServer rpsAcsServer= rpsAcsServerServices.getByAcsServerIdAndEventCode(acsServerCode, eventCode);
		RpsCumulativeResponses rpsCumulativeResponses= rpsCumulativeResponsesService.getResponseByBatchAcsAssmntsAndQp(rpsBatch, rpsAcsServer, assessmentCode, qpId);
		resultComputationStatus= rpsCumulativeResponses.getIsResultComputed();
		rpsCumulativeResponses.setIsResultComputed(RpsConstants.RESULT_COMPUTE_STATUS.RESULT_IN_PROGRESS.toString());
		rpsCumulativeResponsesService.saveRpsCmltiveRes(rpsCumulativeResponses);
		RpsEvent event = checkForEvent(eventCode);
		RpsBatch batch = checkForBatch(batchCode);
		RpsAcsServer acsServer = checkForAcsServer(acsServerCode);		
		RpsAssessment assessment = checkForAssessment(assessmentCode);
		RpsQuestionPaper qp = checkForQp(qpId);
		logger.info("Found!! eventName::"+event.getEventName()+"; batchCode::"+batch.getBatchCode()+"; acsCode::"+acsServer.getAcsServerId()+"; assessmentCode::"+assessment.getAssessmentCode()+"; qpCode::"+qp.getQpCode());				
		try {
			resultProcessor.processResultsForBatchAndQp(eventCode, batch.getBid(), acsServer.getAcsId(), assessmentCode, qpId, resultComputationStatus);
			//update result computaion status to in computed in cumulative response table
			rpsCumulativeResponses.setIsResultComputed(RpsConstants.RESULT_COMPUTE_STATUS.RESULT_COMPUTED.toString());
			rpsCumulativeResponsesService.saveRpsCmltiveRes(rpsCumulativeResponses);
		} catch (RpsException e) {
			rpsCumulativeResponses.setIsResultComputed(RpsConstants.RESULT_COMPUTE_STATUS.RESULT_FAILLED.toString());
			rpsCumulativeResponsesService.saveRpsCmltiveRes(rpsCumulativeResponses);
			throw new RpsException(e.getMessage());
		}
		logger.debug("--OUT--");
	}


	
	private RpsQuestionPaper checkForQp(Integer qpId) throws RpsException {
		RpsQuestionPaper qp = rpsQuestionPaperService.findOne(qpId);
		if(qpId == null){
			String errMsg = "Question paper not found by quetsion paper id::"+qpId;
			logger.error(errMsg);
			throw new RpsException(errMsg);			
		}	
		return qp;
	}

	private RpsEvent checkForEvent(String eventCode) throws RpsException{
		RpsEvent event = rpsEventService.findByEventCode(eventCode);
		if(event == null){
			String errMsg = "Event not found by eventCode::"+eventCode;
			logger.error(errMsg);
			throw new RpsException(errMsg);			
		}	
		return event;
	}
	
	private RpsBatch checkForBatch(String batchCode) throws RpsException{
		RpsBatch batch = rpsBatchService.findByBatchCode(batchCode);
		if(batch == null){
			String errMsg = "Batch not found by batchCode::"+batchCode;
			logger.error(errMsg);
			throw new RpsException(errMsg);			
		}
		return batch;
	}
	
	private RpsAcsServer checkForAcsServer(String acsServerCode) throws RpsException{
		RpsAcsServer acsServer = rpsAcsServerServices.findByAcsServerId(acsServerCode);
		if(acsServer == null){
			String errMsg = "Acs server not found by acsServerCode::"+acsServerCode;
			logger.error(errMsg);
			throw new RpsException(errMsg);
		}
		return acsServer;
	}
	
	private RpsAssessment checkForAssessment(String assessmentCode) throws RpsException{
		RpsAssessment assessment = rpsAssessmentService.findByCode(assessmentCode);
		if(assessment == null){
			String errMsg = "Assessment not found by acsServerCode::"+assessmentCode;
			logger.error(errMsg);
			throw new RpsException(errMsg);
		}
		return assessment;
	}
}
