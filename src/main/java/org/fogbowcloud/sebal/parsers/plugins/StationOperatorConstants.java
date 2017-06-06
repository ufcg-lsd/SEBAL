package org.fogbowcloud.sebal.parsers.plugins;

import java.text.SimpleDateFormat;

public class StationOperatorConstants {
	
	public static final double R = 6371; // km
	public static final long A_DAY = 1000 * 60 * 60 * 24;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"YYYYMMdd");
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
			"yyyyMMdd;hhmm");

}
