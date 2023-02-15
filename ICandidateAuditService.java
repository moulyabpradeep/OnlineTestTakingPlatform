package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.dataobject.AuditCandidateResponseDO;
import com.merittrac.apollo.acs.dataobject.ICandidateActionDO;
import com.merittrac.apollo.acs.dataobject.ImageMissingAuditDO;
import com.merittrac.apollo.acs.exception.CandidateRejectedException;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface ICandidateAuditService
{
	public boolean auditCandidateAction(ICandidateActionDO auditCandidateAction) throws GenericDataModelException, CandidateRejectedException;

	public boolean auditCandidateActionImageMissing(ImageMissingAuditDO auditCandidateAction) throws GenericDataModelException, CandidateRejectedException;

	public boolean auditCandidateActionChangeOption(AuditCandidateResponseDO auditCandidateAction) throws GenericDataModelException, CandidateRejectedException;
}
