package com.merittrac.apollo.acs.services;

import java.util.List;

import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface ICandidatePDFGeneratorService
{
	public boolean generateProvisionalScoreCardForBatch(String batchCode) throws GenericDataModelException;
	
	public boolean generateProvisionalScoreCardForCandidates(String batchCode, List<Integer> candidateIds, Boolean isPdfToBePrinted) throws GenericDataModelException;
	
}
