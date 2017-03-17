package io.pivotal.pde.sample.gemfire.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Dates {
	

	/**
	 * returns the yyyymmdd date string corresponding to 
	 * the given date.  Optionally, a day offset can be provided
	 * 
	 * @param date
	 * @param plusDays
	 * @return
	 */
	public static String dateString(Date date, int plusDays){
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTime(date);
		cal.add(Calendar.DATE, plusDays);
		
		return String.format("%04d%02d%02d",  cal.get(Calendar.YEAR), 1 + cal.get(Calendar.MONTH), cal.get(Calendar.DATE) );	
	}
	
	
	/**
	 * returns the time corresponding to 12 am (time zero) on the day which includes
	 * the given date in the given time zone.  Optionally, a day offset can be provided
	 * in which case, time zero on the day plusDays from the day that includes the 
	 * given date in the given time zone
	 * 
	 * @param date
	 * @param plusDays
	 * @param timeZone
	 * @return
	 */
	public static long timeZero(Date date, int plusDays, String timeZone){
		Calendar cal = new GregorianCalendar(TimeZone.getTimeZone(timeZone));
		cal.setTime(date);
		
		GregorianCalendar c1 = new GregorianCalendar();
		c1.clear();
		c1.setTimeZone(TimeZone.getTimeZone(timeZone));
		c1.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DATE));
		
		c1.add(Calendar.DATE, plusDays);
		
		return c1.getTimeInMillis();
	}
	
//	public static  String formatDate(long date, String tz) {
//		SimpleDateFormat fmt = new SimpleDateFormat("MMM dd yyyy");
//		fmt.setTimeZone(TimeZone.getTimeZone(tz));
//		return fmt.format(new Date(date));
//
//	}

	public static  String formatTimeOfDay(long timeOfDay) {
		long hour = timeOfDay/(60 * 60 * 1000);
		long minute = (timeOfDay%3600) / (60 * 1000);

		return String.format("%02d:%02d", hour, minute);
	}
	
	
}
