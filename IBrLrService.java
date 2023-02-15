/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Dec 5, 2014 10:19:10 AM - jai_s
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.Map;

import com.merittrac.apollo.core.exception.GenericDataModelException;

/**
 * 
 * 
 * @author jai_s - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public interface IBrLrService {

	/**
	 * Gives br in the form of map for batch and assessment <br>
	 * where key is name of rule and value is rule value.
	 * 
	 * @param batchCode
	 * @param assessmentCode
	 * @return
	 * @throws GenericDataModelException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public Map<String, Object> getBrMapForBatchAndAssessment(String batchCode, String assessmentCode)
			throws GenericDataModelException;
}
