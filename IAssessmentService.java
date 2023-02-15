package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IAssessmentService {

	/**
	 * Load {@link AcsAssessment} based on primary key assessmentCode.
	 * 
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 */
	public AcsAssessment loadAssessment(String assessmentCode)
			throws GenericDataModelException;
}
