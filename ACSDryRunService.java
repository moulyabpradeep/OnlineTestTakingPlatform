package com.merittrac.apollo.acs.services;

import java.util.List;

import com.merittrac.apollo.acs.entities.AcsDryRunConfig;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class ACSDryRunService extends BasicService {
	
	private static ACSDryRunService acsDryRunService;
	
	private ACSDryRunService() {
	}
	
	public static final ACSDryRunService getInstance() {
		if (acsDryRunService == null) {
			synchronized (ACSDryRunService.class) {
				if (acsDryRunService == null) {
					acsDryRunService = new ACSDryRunService();
				}
			}
		}
		return acsDryRunService;
	}
	
	public void saveOrUpdateDryRun(boolean isEnabled) throws GenericDataModelException {
		List<AcsDryRunConfig> acsDryRunConfig = (List<AcsDryRunConfig>) session.getResultAsList(AcsDryRunConfig.class.getCanonicalName(), null);
		AcsDryRunConfig acsdrConfig = null;
		if (acsDryRunConfig != null && acsDryRunConfig.size() > 0)
			acsdrConfig = acsDryRunConfig.get(0);
		if (acsdrConfig != null)
			acsdrConfig.setEnabled(isEnabled);
		else
			acsdrConfig = new AcsDryRunConfig(isEnabled);
		session.saveOrUpdate(acsdrConfig);
	}
	
	public boolean isDryRunEnabled() throws GenericDataModelException {
		List<AcsDryRunConfig> acsDryRunConfig = (List<AcsDryRunConfig>) session.getResultAsList(AcsDryRunConfig.class.getCanonicalName(), null);
		if (acsDryRunConfig != null && acsDryRunConfig.size() > 0)
			return acsDryRunConfig.get(0).isEnabled();
		return false;
	}
	

}
