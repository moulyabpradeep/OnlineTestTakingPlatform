package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.common.entities.packgen.QpackExportEntity;
import com.merittrac.apollo.core.services.IBasicService;

public interface IQPackService extends IBasicService
{
	public void activateQPack(QpackExportEntity qpee);
}
