package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.IBasicService;

public interface IQuestionService extends IBasicService {
	public void getAllQuestionsForQPID(int qpid);

	public void getAllQuestionsInSection(int qpid, int sectionID);

	public int getQPIdByIdentifier(String qpIdent) throws GenericDataModelException;

	public String getInstructionSheetByQPId(String qpIdent);

	public int getQuestionCountForQP(String qpIdent) throws GenericDataModelException;

}
