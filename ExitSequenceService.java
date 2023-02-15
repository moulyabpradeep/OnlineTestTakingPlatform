package com.merittrac.apollo.acs.services;

import java.util.HashMap;
import java.util.List;

import com.merittrac.apollo.acs.entities.AcsTpExitSeq;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class ExitSequenceService extends BasicService implements IExitSequence {
	public static final String ALL_EXIT_SEQUENCES = " from AcsTpExitSeq es";
	public static final String EXIT_SEQ_BY_EVENT_CODE = " from AcsTpExitSeq es where es.eventCode = (:eventCode)";

	public List<AcsTpExitSeq> getAllExitSequences() throws GenericDataModelException {
		List<AcsTpExitSeq> exitseqs = session.getListByQuery(ALL_EXIT_SEQUENCES, null);
		return exitseqs;
	}

	public boolean saveExitSequence(AcsTpExitSeq es) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put("eventCode", es.getEventCode());
		AcsTpExitSeq e = (AcsTpExitSeq) session.getByQuery(EXIT_SEQ_BY_EVENT_CODE, params);
		if (e != null) {
			e.setExitSequence(es.getExitSequence());
			e.setLastUpdated(es.getLastUpdated());
		}
		session.saveOrUpdate(es);
		return true;
	}
}
