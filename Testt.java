/**
 * 
 * Copyright © MeritTrac Services Pvt. Ltd. All Rights Reserved.
 * 
 * Last modified by: Jun 10, 2015 5:39:19 PM - Moulya_P
 * 
 */
package com.merittrac.apollo.acs.services;

import java.text.ParseException;
import java.util.Calendar;

import com.merittrac.apollo.acs.utility.TimeUtil;

/**
 * 
 * 
 * @author Moulya_P - MeritTrac Services Pvt. Ltd.
 * @since Apollo v2.0
 * @see
 */
public class Testt {

	/**
	 * @param args
	 * @throws ParseException
	 * 
	 * @since Apollo v2.0
	 * @see
	 */
	public static void main(String[] args) throws ParseException {// 1968-Dec-19
		Calendar c = Calendar.getInstance();
		System.out.println(TimeUtil.convertTimeAsString(c.getTimeInMillis(), TimeUtil.DISPLAY_DATE_FORMAT));
	}

}
