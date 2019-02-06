package org.fogbowcloud.sebal.parsers.plugins;

import java.text.SimpleDateFormat;

public class StationOperatorConstants {

	// Calculation constants
	public static final double R = 6371; // km

	// Parsing constants
	public static final long A_DAY = 1000 * 60 * 60 * 24;
	public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYYMMdd");
	public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyyMMdd;hhmm");

	// URL constants
	public static final String STATION_CSV_SERVER_URL = "station_csv_server_url";
	public static final String NOAA_FTP_SERVER_URL = "noaa_ftp_server_url";

	// File constants
	public static final String UNFORMATTED_LOCAL_STATION_FILE_PATH = "unformatted_local_station_file_path";
	public static final String STATIONS_CSV_FROM_YEAR_FILE_PATH = "stations_csv_from_year_dir_path";

	// Properties constants
	public static final String SWIFT_CLIENT_PATH = "swift_client_path";
	public static final String SWIFT_URL_EXPIRATION_TIME = "url_expiration_time";
	public static final String SWIFT_CONTAINER_PREFIX = "swift_container_prefix";
	public static final String SWIFT_META_AUTH_KEY = "swift_meta_auth_key";
	public static final String SWIFT_STORAGE_URL = "swift_storage_url";

}
