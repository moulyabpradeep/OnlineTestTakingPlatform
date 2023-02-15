package com.merittrac.apollo.acs.services;

import com.merittrac.apollo.acs.entities.AcsAdmins;
import com.merittrac.apollo.core.exception.GenericDataModelException;

public interface IAdminService {
	public boolean createAdministrator(AcsAdmins admin) throws GenericDataModelException;

	public AcsAdmins getAdmin(String userName, String Password) throws GenericDataModelException;

	public boolean changeAdminPwd(String userName, String oldPwd, String newPwd) throws GenericDataModelException;

	public boolean deleteAdministrator(String userName, String Password) throws GenericDataModelException;
}
