package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.entities.AcsAssessment;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Provides apis related to {@link AcsAssessment}
 * 
 * 
 * @author jai_s - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class AssessmentService extends BasicService implements IAssessmentService {

	@Override
	public AcsAssessment loadAssessment(String assessmentCode) throws GenericDataModelException {
		return (AcsAssessment) session.get(assessmentCode, AcsAssessment.class.getName());
	}

}
