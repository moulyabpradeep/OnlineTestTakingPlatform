package com.merittrac.apollo.acs.services;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.constants.ResultStatusEnum;
import com.merittrac.apollo.acs.dataobject.ComputeResultProcDO;
import com.merittrac.apollo.acs.entities.ResultProcReportTO;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class ResultProcStatusService extends BasicService
{
	public boolean updateResultProcStatus(ComputeResultProcDO crProc, ResultStatusEnum resultStatusEnum)
	{
		if (crProc.getBatchCode() != null)
		{
			if ((resultStatusEnum.equals(ResultStatusEnum.RESULTPROCESSING_PER_BATCH_IN_PROGRESS)) || (resultStatusEnum.equals(ResultStatusEnum.RESULTPROCESSING_FAILED)))
			{
				String hql_query = "update com.merittrac.apollo.acs.database.tableobject.BatchDetailsTO set scoreStatus=(:scoreStatus) where batchId=(:batchId)";
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("batchId", crProc.getBatchCode());
				params.put("scoreStatus", resultStatusEnum);

				try
				{
					session.updateByQuery(hql_query, params);
				}
				catch (GenericDataModelException ex)
				{
					ex.printStackTrace();
				}
			}
			else if (resultStatusEnum.equals(ResultStatusEnum.RESULTPROCESSING_SUCCESS))
			{
				String hql_query = "update com.merittrac.apollo.acs.database.tableobject.BatchDetailsTO set scoreStatus=(:scoreStatus) where batchId=(:batchId)";
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("batchId", crProc.getBatchCode());
				params.put("scoreStatus", resultStatusEnum);
				try
				{
					session.updateByQuery(hql_query, params);
				}
				catch (GenericDataModelException ex)
				{
					ex.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * @param batchID
	 * @param candID
	 * @return
	 * @throws GenericDataModelException
	 */
	public ResultProcReportTO findByBatchIDCandID(String batchCode, int candID) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candID", candID);
		ResultProcReportTO resultProcReportTO =
				(ResultProcReportTO) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_RESULT_REPORT_DETAILS_BY_CANDID_BATCHID, params);
		return resultProcReportTO;

	}

	/**
	 * @param batchId
	 * @param candidateId
	 * @param pdfFilePath
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updatePdfGeneratedTimeByBatchIdCandidateId(String batchCode, int candidateId, String pdfFilePath)
			throws GenericDataModelException {
		String query =
				"UPDATE ResultProcReportTO SET pdfGenerationTime=(:date), pdfFilePath=(:pdfFilePath) WHERE batchCode = (:batchCode) and candID = (:candidateId)";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateId", candidateId);
		params.put("date", Calendar.getInstance());
		params.put("pdfFilePath", pdfFilePath);
		session.updateByQuery(query, params);
	}

	/**
	 * @param batchId
	 * @param candidateIds
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public void updatePdfGeneratedTimeByBatchIdCandidateIds(String batchCode, List<Integer> candidateIds)
			throws GenericDataModelException {
		String query =
				"UPDATE ResultProcReportTO SET pdfGenerationTime=(:date) WHERE batchCode = (:batchCode) and candID in (:candidateIds)";
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		params.put("candidateIds", candidateIds);
		params.put("date", Calendar.getInstance());
		session.updateByQuery(query, params);
	}

	/**
	 * @param resultProcReportTO
	 * @return
	 * @throws GenericDataModelException
	 */
	public boolean saveResultProcReport(ResultProcReportTO resultProcReportTO) throws GenericDataModelException {
		session.merge(resultProcReportTO);
		return true;
	}
	
	/**
	 * @param batchID
	 * @param assessmentID
	 * @param candID
	 * @return
	 * @throws GenericDataModelException
	 */
	public ResultProcReportTO findByBatchIDAssessmentIDCandID(String batchCode, String assessmentCode, int candID)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("assessmentCode", assessmentCode);
		params.put("batchCode", batchCode);
		params.put("candID", candID);
		ResultProcReportTO resultProcReportTO =
				(ResultProcReportTO) session.getByQuery(
						ACSQueryConstants.QUERY_FETCH_RESULT_REPORT_DETAILS_BY_CANDID_BATCHID_ASSEID, params);

		return resultProcReportTO;

	}

	/**
	 * @param batchID
	 * @param assessmentID
	 * @param candID
	 * @return
	 * @throws GenericDataModelException
	 */
	public List<ResultProcReportTO> findByBatchIDCandIDs(String batchCode, List<Integer> candIDs)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("batchCode", batchCode);
		params.put("candID", candIDs);
		List<ResultProcReportTO> resultProcReportTO = session.getResultAsListByQuery(
				ACSQueryConstants.QUERY_FETCH_RESULT_REPORT_WITH_SECTION_DETAILS_BY_CANDIDS_BATCHID, params, 0);
		return resultProcReportTO;

	}
}
