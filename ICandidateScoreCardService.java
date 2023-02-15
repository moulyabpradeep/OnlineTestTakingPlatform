package com.merittrac.apollo.acs.services;

import java.util.List;

import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.excel.FileExportExportEntity;

public interface ICandidateScoreCardService {
	public FileExportExportEntity generateCandidateScoreCard(List<String> batchID, String assessmentID, String setID)
			throws GenericDataModelException;

	public FileExportExportEntity generateCandidateScoreCardOnCandidateIds(Integer[] CandidateIds)
			throws GenericDataModelException;

}
