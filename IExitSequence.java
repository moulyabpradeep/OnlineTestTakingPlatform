package com.merittrac.apollo.acs.services;

import java.util.List;

import com.merittrac.apollo.acs.entities.AcsTpExitSeq;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IExitSequence
{
	public List<AcsTpExitSeq> getAllExitSequences() throws GenericDataModelException;
	
	public boolean saveExitSequence(AcsTpExitSeq es) throws GenericDataModelException;
}
