package com.merittrac.apollo.acs.services;

import java.util.HashMap;

import com.merittrac.apollo.acs.constants.ACSConstants;
import com.merittrac.apollo.acs.constants.ACSQueryConstants;
import com.merittrac.apollo.acs.entities.AcsAdmins;
import com.merittrac.apollo.core.exception.GenericDataModelException;
import com.merittrac.apollo.core.services.BasicService;

public class AdminService extends BasicService implements IAdminService {
	public boolean createAdministrator(AcsAdmins admin) throws GenericDataModelException {
		session.persist(admin);
		return true;
	}

	public AcsAdmins getAdmin(String userName, String Password) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.USER_NAME, userName);
		params.put(ACSConstants.PASSWORD, Password);

		AcsAdmins admin =
				(AcsAdmins) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ADMIN_BY_USERNAME_PASSWORD, params);
		return admin;
	}

	public boolean changeAdminPwd(String userName, String oldPwd, String newPwd) throws GenericDataModelException {
		AcsAdmins admin = this.getAdmin(userName, oldPwd);
		if (admin == null) {
			return false;
		} else {
			admin.setAdminPassword(newPwd);
			session.merge(admin);
		}
		return true;
	}

	public boolean deleteAdministrator(String userName, String Password) throws GenericDataModelException {
		AcsAdmins admin = this.getAdmin(userName, Password);
		if (admin == null) {
			return false;
		}
		session.delete(admin.getId(), AcsAdmins.class.getCanonicalName());
		return true;
	}

	public AcsAdmins getAdminByUserId(String userName) throws GenericDataModelException {
		HashMap<String, Object> params = new HashMap<String, Object>();
		params.put(ACSConstants.USER_NAME, userName);

		AcsAdmins admin = (AcsAdmins) session.getByQuery(ACSQueryConstants.QUERY_FETCH_ADMIN_BY_USERNAME, params);
		return admin;
	}

	public boolean updateAdmin(AcsAdmins adminTO) throws GenericDataModelException {
		session.merge(adminTO);
		return true;
	}
}
