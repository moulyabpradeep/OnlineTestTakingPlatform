package com.merittrac.apollo.acs.services;

import java.util.Calendar;
import java.util.List;

import com.merittrac.apollo.acs.dataobject.AuditCandidateResponseDO;
import com.merittrac.apollo.acs.dataobject.CandidateActionDO;
import com.merittrac.apollo.acs.dataobject.CandidateIdDO;
import com.merittrac.apollo.acs.dataobject.CandidateResponsesDO;
import com.merittrac.apollo.acs.dataobject.CandidateStatusDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateDetailsDO;
import com.merittrac.apollo.acs.dataobject.tp.CandidateLastViewedDO;
import com.merittrac.apollo.acs.entities.AcsBatch;
import com.merittrac.apollo.acs.entities.AcsBatchCandidateAssociation;
import com.merittrac.apollo.acs.entities.AcsCandidate;
import com.merittrac.apollo.acs.entities.AcsCandidateStatus;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface ICandidateService {
	public void insertCandidateAuditData(Integer batchCandidateAssociationId, String auditMessage)
			throws GenericDataModelException;

	public AcsCandidateStatus getCandidateStatus(Integer batchCandidateAssociationId) throws GenericDataModelException;

	/*
	 * To start candidate exam. In-case if the candidate is not allowed to start the exam i.e currently he is not
	 * associated to an batch or exceeded late allow time then CandidateRejectedException is thrown with approriate
	 * error code.
	 */
	public boolean startCandidateExam(int batchCandidateAssociationId, int candidateId, long clientTime,
			String batchCode) throws GenericDataModelException, CandidateRejectedException;

	/*
	 * To submited candidate exam. Once the candidate exam is ended he can never start the exam again. In-case if the
	 * candidate failed to submit an exam, quartz job on the ACS server would force-submit the candidate once batch-end
	 * time is reached
	 */
	public boolean endCandidateExam(int batchCandidateAssociationId, String batchCode, boolean forceSubmit)
			throws GenericDataModelException, CandidateRejectedException;

	/*
	 * To submited candidate exam. Once the candidate exam is ended he can never start the exam again. In-case if the
	 * candidate failed to submit an exam, quartz job on the ACS server would force-submit the candidate once batch-end
	 * time is reached
	 */
	public boolean endCandidateExamTP(int batchCandidateAssociationId, boolean forceSubmit, long clientTime,
			boolean acsCall, String batchCode,int timeTaken,int qid) throws GenericDataModelException, CandidateRejectedException;

	public boolean isExamEnded(int candidateId) throws GenericDataModelException;

	public boolean isExamStarted(int candidateId) throws GenericDataModelException;

	public void setCandidate(AcsCandidate candidateDetails) throws GenericDataModelException;

	public boolean
			startCandidateHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
					throws GenericDataModelException;

	/*
	 * To clear an answer which is submited for a question by a candidate
	 */
	public boolean clearAnswer(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException;

	/*
	 * To submit an answer for a question by a candidate from TP
	 */
	public boolean submitAnswer(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException;

	/*
	 * To mark an item for review from TP
	 */
	public boolean markItemForReview(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException;

	/*
	 * To retrieve all candidate responses for a given candidate ID and Question paper ID
	 */
	public CandidateResponsesDO[] getCandidateResponses(int batchCandidateAssociationId)
			throws GenericDataModelException, CandidateRejectedException;

	/*
	 * To retrieve the count of attempted questions for a candidate on a given question paper
	 */
	public int getAttemptedQuestions(int bcaId) throws GenericDataModelException, CandidateRejectedException;

	/*
	 * To retrive the candidate details for a given candidate ID for candidate confirmation page in TP
	 */
	public CandidateDetailsDO getCandidateByID(int candID) throws GenericDataModelException;

	/*
	 * To update candidate last heart beat time for a candidate from TP
	 */
	public AcsCandidateStatus updateCandidatelastHeartBeatTime(int batchCandidateAssociationId)
			throws GenericDataModelException, CandidateRejectedException;

	// public void updateCandidateHBTimeInMemory(CandidateStatusTO cs);

	/*
	 * public CandidateStatusDO getCandidateStatusLiteByCandIdAndBatchId(int candId, String batchCode) throws
	 * GenericDataModelException;
	 */

	/*
	 * To retrieve a candidate by hall-ticket number
	 */
	public int getCandidateIDbyHallTicket(String htID) throws GenericDataModelException;

	/*
	 * To retrieve a candidate by application number
	 */
	public int getCandidateIDbyApplicationNumber(String appNum) throws GenericDataModelException;

	/*
	 * To get the remaining time for a candidate for an assessment. Used in-case of crash scenario for showing the
	 * remaining time in TP.
	 */
	public long getRemainingTimeForCandidate(int batchCandidateAssociationId) throws GenericDataModelException,
			CandidateRejectedException;

	public long trimDurationOnBatchEndTime(Calendar currentDateTime, long allotedDuration, Calendar batchEndTime);

	public Calendar getActualTestStartTimebyCandId(int candId) throws GenericDataModelException;

	public boolean isCandidateAllowedForLogin(AcsBatchCandidateAssociation batchCandidateAssociation,
			AcsCandidateStatus candidateStatus, AcsBatch batchDetails, Calendar lateLoginTime)
			throws GenericDataModelException, CandidateRejectedException;

	/*
	 * To update player status from TP to ACS
	 */
	public boolean updatePlayerStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException;

	public boolean updateCandTestStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException;

	public boolean updateSystemStatus(CandidateStatusDO candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException;

	public String getCandHostAddress(String batchCode, int candId) throws GenericDataModelException;

	public boolean validateCandidateIPAddress(String ipAddress) throws GenericDataModelException,
			CandidateRejectedException;

	public boolean startSystemHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
			throws GenericDataModelException;

	public boolean startPlayerHeartBeatJob(Calendar startTime, Calendar endTime, String batchCode, boolean reschedule)
			throws GenericDataModelException;

	public boolean startCandidateForceSubmitJob(Calendar batchEndTime, String batchCode, boolean reschedule)
			throws GenericDataModelException;

	// public AcsTPConfig getTPSetting() throws GenericDataModelException;

	/*
	 * To store the candidate current viewed question. Used to restore to the same question post crash
	 */
	public boolean updateCandCurrentViewedQuestion(CandidateActionDO candAction) throws GenericDataModelException,
			CandidateRejectedException;

	public CandidateLastViewedDO getCandidateCurrentQuestionViewedByBatch(Integer batchCandidateAssociationId)
			throws GenericDataModelException;

	// Used by test player to retrieve the last viewed question and section by a candidate
	public CandidateLastViewedDO getCandidateCurrentQuestionViewed(Integer batchCandidateAssociationId)
			throws GenericDataModelException;

	public boolean candidateClearLogin(int batchCandidateAssociationId) throws GenericDataModelException,
			CandidateRejectedException;

	public String candidateClearLoginByApplicationNum(int batchCandidateAssociationId, String candAppNum,
			String batchCode, String userName, String ipAddress) throws GenericDataModelException,
			CandidateRejectedException;

	public boolean candidateCancelLogin(int bcaId) throws GenericDataModelException, CandidateRejectedException;

	public String getCandidateImage(int candId) throws GenericDataModelException;

	public List<CandidateIdDO> getCandIdsbyApplicationNumber(String appNum) throws GenericDataModelException;

	public Integer getCandIdByAppNumAndBatchCode(String candAppNum, String batchCode) throws GenericDataModelException;

	public boolean isCandAllowedToStartExam(Integer candId, String batchCode) throws GenericDataModelException,
			CandidateRejectedException;

	public void auditCandidateRespone(AuditCandidateResponseDO auditCandidateResponse)
			throws GenericDataModelException, CandidateRejectedException;

	public void saveCandSuffleSequence(int candID, String shuffleSequence, long clientTime, String batchCode,
			String ip, int bcaId, String assessmentCode) throws GenericDataModelException, CandidateRejectedException;

	public String getApplicationNumberByCandidateId(int candId) throws GenericDataModelException;

	// public void insertAdminAuditData(AcsAdminAudits auditTO) throws GenericDataModelException;

	public boolean startRpackGenerationInitiatorJob(String batchCode, Calendar batchStartTime, Calendar batchEndTime)
			throws GenericDataModelException;

	public boolean startAttendanceReportGeneratorJob(String batchCode, Calendar startTime, boolean forceStart);

	public boolean isAllCandidatesSubmittedTest(String batchCode, Calendar startTime, Calendar endTime)
			throws GenericDataModelException;

	public boolean startRpackReportGenerationJobAtAET(String batchCode, Calendar assessmentEndTime)
			throws GenericDataModelException;

	public boolean startRpackReportGenerationJobAtBET(String batchCode, boolean forceStart)
			throws GenericDataModelException;

	public boolean updateCandRpackStatus(List<Integer> candIDs, boolean status, String batchCode)
			throws GenericDataModelException;

	public boolean isAllCandsSubmittedTestByBatch(String batchCode) throws GenericDataModelException;

	public boolean startRpackUploader(String batchCode, String packIdentifier, String path)
			throws GenericDataModelException;

	/**
	 * @param batchId
	 * 
	 * @param packIdentifier
	 * @param path
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	boolean startAttendancePackUploader(String batchCode, String packIdentifier, String path)
			throws GenericDataModelException;

	List<AcsBatchCandidateAssociation> getAvailableCandidateStatus(String batchCode) throws GenericDataModelException;

	boolean updatePlayerStatus(AcsCandidateStatus candidateStatusTO, int batchCandidateAssociationId)
			throws GenericDataModelException;

	boolean updateSystemStatus(AcsCandidateStatus candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException;

	boolean updateCandTestStatus(AcsCandidateStatus candidateStatusDO, int batchCandidateAssociationId)
			throws GenericDataModelException;
	
	public String auditCandidateResponse(AuditCandidateResponseDO auditCandidateResponse)
			throws GenericDataModelException, CandidateRejectedException;

	public boolean startRpackGeneratorForEndedCandidatesJob(String batchCode, Calendar startTime,
			Calendar endTime, int repeatInterval) throws GenericDataModelException;

}
