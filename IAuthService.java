package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.dataobject.CandidateAssessmentDO;
import com.merittrac.apollo.acs.dataobject.CandidateCredentialsDetailsDO;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IAuthService
{
	public boolean validateCandidateCredentials(CandidateCredentialsDetailsDO candCredentialsDetails) throws GenericDataModelException, CandidateRejectedException;

	public CandidateAssessmentDO authenticateCandidate(CandidateCredentialsDetailsDO candCredentialsDetails) throws GenericDataModelException, CandidateRejectedException;
}
