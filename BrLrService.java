/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Dec 5, 2014 10:22:15 AM - jai_s
 * 
 */
package com.merittrac.apollo.acs.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRules;
import com.merittrac.apollo.acs.entities.AcsBussinessRulesAndLayoutRulesId;
import com.merittrac.apollo.acs.utility.JSONToMapConverter;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

/**
 * Provides APIs to get business and layout rules.
 * 
 * @author jai_s - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class BrLrService extends BasicService implements IBrLrService {

	private static BrLrService brLrService = null;

	public static synchronized BrLrService getInstance() {
		if (brLrService == null) {
			brLrService = new BrLrService();
		}
		return brLrService;
	}

	@Override
	public Map<String, Object> getBrMapForBatchAndAssessment(String batchCode, String assessmentCode)
			throws GenericDataModelException {
		Map<String, Object> brlr = new HashMap<>();
		Map<String, Object> params = new HashMap<>();
		params.put("batchCode", batchCode);
		params.put("assessmentCode", assessmentCode);
		AcsBussinessRulesAndLayoutRulesId id = new AcsBussinessRulesAndLayoutRulesId(assessmentCode, batchCode);
		AcsBussinessRulesAndLayoutRules acsBussinessRulesAndLayoutRules =
				(AcsBussinessRulesAndLayoutRules) session.get(id, AcsBussinessRulesAndLayoutRules.class.getName());
		String bRule = acsBussinessRulesAndLayoutRules.getBrRules();
		brlr = JSONToMapConverter.parseJSONtoMap(bRule);
		return brlr;
	}

	public AcsBussinessRulesAndLayoutRules getAcsBussinessRulesAndLayoutRulesByBatchCode(String batchCode)
			throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.BATCH_CODE, batchCode);
		List<AcsBussinessRulesAndLayoutRules> rulesList =
				session.getListByQuery(ACSQueryConstants.QUERY_FETCH_BRRULES_ENTITY_BY_BATCHCODE, params);
		if (rulesList.equals(Collections.<Object> emptyList()))
			return null;
		return rulesList.get(0);

	}
}
