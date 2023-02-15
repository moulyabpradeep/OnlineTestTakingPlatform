package com.merittrac.apollo.acs.services;

import java.util.Calendar;

public interface IACSService
{
	public Calendar getACSTime();

	public long getACSTimeWithTimeStamp();

	public boolean setACSDateTime(Calendar cal);
}
