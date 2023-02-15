package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.dataobject.ComputeResultProcDO;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IResultService
{
	public void calcResultPerCandidateInQP(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerCandidateInBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerAssessmentInBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerBatch(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerBatches(String[] batchCodes, ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerAssessment(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public void calcResultPerEvent(ComputeResultProcDO cresultentity) throws GenericDataModelException;

	public Object generateResultsPerBatch(int eventID, int batchID) throws GenericDataModelException;

	public Object generateResultsPerAssessmentInBatch(int eventID, int batchID, int assessmentID) throws GenericDataModelException;
}
