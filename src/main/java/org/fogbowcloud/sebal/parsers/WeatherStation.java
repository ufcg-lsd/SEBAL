package org.fogbowcloud.sebal.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.util.SEBALAppConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WeatherStation {

	private static final double R = 6371; // km
	private static final long A_DAY = 1000 * 60 * 60 * 24;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat(
			"YYYYMMdd");
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
			"yyyyMMdd;hhmm");

	private static final Logger LOGGER = Logger.getLogger(WeatherStation.class);

	private Map<String, String> cache = new HashMap<String, String>();
	private JSONArray stations;
	private Properties properties;

	public WeatherStation() throws URISyntaxException, HttpException,
			IOException {
		this(new Properties());
	}

	public WeatherStation(Properties properties) throws URISyntaxException,
			HttpException, IOException {
		this.stations = new JSONArray(IOUtils.toString(new FileInputStream(
				"stations.json")));
		this.properties = properties;
	}

	private List<JSONObject> findNearestStation(double lat, double lon) {
		List<JSONObject> orderedStations = new LinkedList<JSONObject>();
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < stations.length(); i++) {
			JSONObject station = stations.optJSONObject(i);
			double d = d(lat, lon, station.optDouble("lat"),
					station.optDouble("lon"));
			if (d < minDistance) {
				minDistance = d;
			}
			station.put("d", d);
			orderedStations.add(station);
		}

		Collections.sort(orderedStations, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				return ((Double) o1.optDouble("d")).compareTo((Double) o2
						.optDouble("d"));
			}
		});

		return orderedStations;
	}

	private double d(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2)
				* Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}

	public void persistStations() throws IOException {
		JSONArray stations = new JSONArray();
		for (String line : IOUtils.readLines(new FileInputStream(
				"stations.html"))) {
			if (line.contains("var html")) {
				String[] split = line.split("<br />");
				JSONObject json = new JSONObject();
				String split0 = split[0];
				String split00 = split0
						.substring(split[0].lastIndexOf(":") + 1,
								split[0].indexOf("</b>")).trim();

				json.put("id", split00.split("-")[0].trim());
				json.put("name", split00.substring(split00.indexOf("-") + 1)
						.trim());
				json.put(
						"lat",
						Double.parseDouble(split[1].substring(
								split[1].indexOf(":") + 1).trim()));
				json.put(
						"lon",
						Double.parseDouble(split[2].substring(
								split[2].indexOf(":") + 1).trim()));
				json.put(
						"altitude",
						Double.parseDouble(split[3]
								.substring(split[3].indexOf(":") + 1)
								.replaceAll("m", "").trim()));
				stations.put(json);
			}
		}
		IOUtils.write(stations.toString(2), new FileOutputStream(
				"stations.json"));
	}

	private JSONArray readStation(String id, String beginDate, String endDate)
			throws Exception {

		String year = beginDate.substring(0, 4);

		String baseUnformattedLocalStationFilePath = getBaseUnformattedLocalStationFilePath(year);
		File baseUnformattedLocalStationFile = new File(
				baseUnformattedLocalStationFilePath);
		baseUnformattedLocalStationFile.mkdirs();

		File unformattedLocalStationFile = getUnformattedStationFile(id, year);
		String url = getStationFileUrl(id, year);		
		if (!downloadUnformattedStationFile(unformattedLocalStationFile, url)) {
			return null;
		}

		List<String> stationData = new ArrayList<String>();
		readStationFile(unformattedLocalStationFile, stationData);

		JSONArray dataArray = new JSONArray();
		getHourlyData(beginDate, stationData, dataArray);

		for (int i = 0; i < dataArray.length(); i++) {
			JSONObject stationDataRecord = dataArray.optJSONObject(i);
			String airTemp = stationDataRecord
					.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE);
			String dewTemp = stationDataRecord
					.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE);
			String windSpeed = stationDataRecord
					.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED);

			if (!airTemp.isEmpty() && !dewTemp.isEmpty()
					&& !windSpeed.isEmpty()) {
				unformattedLocalStationFile.delete();
				return dataArray;
			}
		}

		cache.put(url, "FAILED");
		throw new Exception();
	}

	protected String getStationFileUrl(String id, String year) {
		String url = properties.getProperty(SEBALAppConstants.PUBLIC_HTML_STATION_REPOSITORY)
				+ File.separator + year + File.separator + id + "0-99999-" + year;
		return url;
	}

	protected String getBaseUnformattedLocalStationFilePath(String year) {
		return properties
				.getProperty(SEBALAppConstants.UNFORMATTED_LOCAL_STATION_FILE_PATH)
				+ File.separator + year;
	}

	protected File getUnformattedStationFile(String stationId, String year) {
		String unformattedLocalStationFilePath = properties.getProperty(SEBALAppConstants.UNFORMATTED_LOCAL_STATION_FILE_PATH)
				+ File.separator + year + File.separator + stationId + "0-99999-" + year;

		File unformattedLocalStationFile = new File(
				unformattedLocalStationFilePath);
		if (unformattedLocalStationFile.exists()) {
			LOGGER.info("File "
					+ unformattedLocalStationFile
					+ " already exists. Will be removed before repeating download");
			unformattedLocalStationFile.delete();
		}
		return unformattedLocalStationFile;
	}

	private boolean downloadUnformattedStationFile(
			File unformattedLocalStationFile, String url) throws Exception {
		try {
			BasicCookieStore cookieStore = new BasicCookieStore();
			HttpClient httpClient = HttpClientBuilder.create()
					.setDefaultCookieStore(cookieStore).build();

			HttpGet fileGet = new HttpGet(url);
			HttpResponse response = httpClient.execute(fileGet);
			if (response.getStatusLine().getStatusCode() == 404) {
				return false;
			}

			OutputStream outStream = new FileOutputStream(
					unformattedLocalStationFile);
			IOUtils.copy(response.getEntity().getContent(), outStream);
			outStream.close();

			cache.put(url, "SUCCEEDED");
		} catch (Exception e) {
			cache.put(url, "FAILED");
			LOGGER.error("Setting URL " + url + " as FAILED.");
			throw e;
		}

		return true;
	}

	private void readStationFile(File unformattedLocalStationFile,
			List<String> stationData) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(
				unformattedLocalStationFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			stationData.add(line);
		}

		br.close();
	}

	// TODO: document these intervals
	private void getHourlyData(String beginDate, List<String> stationData,
			JSONArray dataArray) throws JSONException {
		for (String data : stationData) {
			if (data.contains(beginDate)) {
				JSONObject jsonObject = new JSONObject();

				String stationId = data.substring(4, 10);
				String date = data.substring(15, 23);
				String time = data.substring(23, 27);

				String latitude = data.substring(28, 34);
				latitude = changeToLatitudeFormat(latitude);

				String longitude = data.substring(34, 41);
				longitude = changeToLongitudeFormat(longitude);

				String windSpeed = data.substring(65, 69);
				windSpeed = changeToWindSpeedFormat(windSpeed);

				String airTemp = data.substring(87, 92);
				airTemp = changeToAirTempFormat(airTemp);

				String dewTemp = data.substring(93, 98);
				dewTemp = changeToDewTempFormat(dewTemp);

				jsonObject.put(SEBALAppConstants.JSON_STATION_ID, stationId);
				jsonObject.put(SEBALAppConstants.JSON_STATION_DATE, date);
				jsonObject.put(SEBALAppConstants.JSON_STATION_TIME, time);
				jsonObject.put(SEBALAppConstants.JSON_STATION_LATITUDE,
						latitude);
				jsonObject.put(SEBALAppConstants.JSON_STATION_LONGITUDE,
						longitude);
				jsonObject.put(SEBALAppConstants.JSON_STATION_WIND_SPEED,
						windSpeed);
				jsonObject.put(SEBALAppConstants.JSON_AIR_TEMPERATURE, airTemp);
				jsonObject.put(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE,
						dewTemp);

				dataArray.put(jsonObject);
			}
		}
	}

	private String changeToLatitudeFormat(String latitude) {
		latitude = latitude.substring(0, 4) + "."
				+ latitude.substring(4, latitude.length());
		StringBuilder sb = new StringBuilder(latitude);
		sb.deleteCharAt(1);
		latitude = sb.toString();
		return latitude;
	}

	private String changeToLongitudeFormat(String longitude) {
		StringBuilder sb;
		longitude = longitude.substring(0, 4) + "."
				+ longitude.substring(4, longitude.length());
		sb = new StringBuilder(longitude);
		sb.deleteCharAt(1);
		longitude = sb.toString();
		return longitude;
	}

	private String changeToWindSpeedFormat(String windSpeed)
			throws NumberFormatException {
		if (windSpeed.equals("99999")) {
			windSpeed = "***";
		} else {
			windSpeed = formatWindSpeed(windSpeed);
		}
		return windSpeed;
	}

	private String changeToAirTempFormat(String airTemp)
			throws NumberFormatException {
		StringBuilder sb;
		String airTempSign = airTemp.substring(0, 0);
		sb = new StringBuilder(airTemp);
		sb.deleteCharAt(0);
		airTemp = sb.toString();
		if (airTemp.equals("99999")) {
			airTemp = "****";
		} else {
			airTemp = formatAirTemp(airTemp, airTempSign);
		}
		return airTemp;
	}

	private String changeToDewTempFormat(String dewTemp)
			throws NumberFormatException {
		StringBuilder sb;
		String dewTempSign = dewTemp.substring(0, 0);
		sb = new StringBuilder(dewTemp);
		sb.deleteCharAt(0);
		dewTemp = sb.toString();
		if (dewTemp.equals("99999")) {
			dewTemp = "****";
		} else {
			dewTemp = formatDewTemp(dewTemp, dewTempSign);
		}
		return dewTemp;
	}

	private String formatWindSpeed(String windSpeed)
			throws NumberFormatException {
		double integerConvertion = Integer.parseInt(windSpeed);
		integerConvertion = integerConvertion / 10.0;
		return String.valueOf(integerConvertion);
	}

	private String formatAirTemp(String airTemp, String airTempSign)
			throws NumberFormatException {
		double integerConvertion = Integer.parseInt(airTemp);
		if (airTempSign.equals("-")) {
			integerConvertion *= -1;
		}

		integerConvertion = integerConvertion / 10.0;
		return String.valueOf(integerConvertion);
	}

	private String formatDewTemp(String dewTemp, String dewTempSign)
			throws NumberFormatException {
		double integerConvertion = Integer.parseInt(dewTemp);
		if (dewTempSign.equals("-")) {
			integerConvertion *= -1;
		}

		integerConvertion = integerConvertion / 10.0;
		return String.valueOf(integerConvertion);
	}

	protected String readFullRecord(Date date, List<JSONObject> stations,
			int numberOfDays) {
		Date begindate = new Date(date.getTime() - numberOfDays * A_DAY);
		Date endDate = new Date(date.getTime() + numberOfDays * A_DAY);

		for (JSONObject station : stations) {
			try {
				JSONArray stationData = readStation(station.optString("id"),
						DATE_FORMAT.format(begindate),
						DATE_FORMAT.format(endDate));

				if (stationData != null) {
					JSONObject closestRecord = null;
					Long smallestDiff = Long.MAX_VALUE;

					for (int i = 0; i < stationData.length(); i++) {
						JSONObject stationDataRecord = stationData
								.optJSONObject(i);
						String dateValue = stationDataRecord
								.optString(SEBALAppConstants.JSON_STATION_DATE);
						String timeValue = stationDataRecord
								.optString(SEBALAppConstants.JSON_STATION_TIME);

						Date recordDate = DATE_TIME_FORMAT.parse(dateValue
								+ ";" + timeValue);
						long diff = Math.abs(recordDate.getTime()
								- date.getTime());
						if (diff < smallestDiff) {
							smallestDiff = diff;
							closestRecord = stationDataRecord;
						}

						if (!closestRecord.optString(
								SEBALAppConstants.JSON_STATION_DATE).isEmpty()
								&& !closestRecord.optString(SEBALAppConstants.JSON_STATION_TIME)
										.isEmpty()
								&& !closestRecord.optString(SEBALAppConstants.JSON_STATION_LATITUDE)
										.isEmpty()
								&& !closestRecord.optString(SEBALAppConstants.JSON_STATION_LONGITUDE)
										.isEmpty()
								&& !closestRecord.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE)
										.isEmpty()
								&& !closestRecord.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE)
										.isEmpty()
								&& !closestRecord
										.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)
										.isEmpty()
								&& Double.parseDouble(closestRecord.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)) >= 0.3) {
							return generateStationData(stationData,
									closestRecord);
						} else if (Double.parseDouble(closestRecord.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)) < 0.3) {
							closestRecord.remove(SEBALAppConstants.JSON_STATION_WIND_SPEED);
							closestRecord.put(SEBALAppConstants.JSON_STATION_WIND_SPEED, "0.3");
						}
					}
				}
			} catch (Exception e) {
				LOGGER.error("Error while reading full record", e);
			}
		}

		return null;
	}

	private String generateStationData(JSONArray stationData,
			JSONObject closestRecord) {
		StringBuilder toReturn = new StringBuilder();
		for (int i = 0; i < stationData.length(); i++) {
			checkVariablesAndBuildString(stationData, closestRecord, toReturn,
					i);
		}
		return toReturn.toString().trim();
	}

	private void checkVariablesAndBuildString(JSONArray stationData,
			JSONObject closestRecord, StringBuilder toReturn, int i) {
		JSONObject stationDataRecord = stationData.optJSONObject(i);

		String stationId = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_ID);
		String dateValue = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_DATE);
		String timeValue = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_TIME);
		String latitude = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_LATITUDE);
		String longitude = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_LONGITUDE);
		String windSpeed = stationDataRecord
				.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED);
		String airTemp = stationDataRecord
				.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE);
		String dewTemp = stationDataRecord
				.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE);
		String avgAirTemp = stationDataRecord
				.optString(SEBALAppConstants.JSON_AVG_AIR_TEMPERATURE);
		String relativeHumidity = stationDataRecord
				.optString(SEBALAppConstants.JSON_RELATIVE_HUMIDITY);
		String minTemp = stationDataRecord
				.optString(SEBALAppConstants.JSON_MIN_TEMPERATURE);
		String maxTemp = stationDataRecord
				.optString(SEBALAppConstants.JSON_MAX_TEMPERATURE);
		String solarRad = stationDataRecord
				.optString(SEBALAppConstants.JSON_SOLAR_RADIATION);

		if (closestRecord.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED) == null) {
			windSpeed = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE) == null) {
			airTemp = "NA";
		}
		if (closestRecord
				.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE) == null) {
			dewTemp = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_AVG_AIR_TEMPERATURE)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_AVG_AIR_TEMPERATURE) == null) {
			avgAirTemp = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_RELATIVE_HUMIDITY)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_RELATIVE_HUMIDITY) == null) {
			relativeHumidity = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_MIN_TEMPERATURE)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_MIN_TEMPERATURE) == null) {
			minTemp = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_MAX_TEMPERATURE)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_MAX_TEMPERATURE) == null) {
			maxTemp = "NA";
		}
		if (closestRecord.optString(SEBALAppConstants.JSON_SOLAR_RADIATION)
				.isEmpty()
				|| closestRecord
						.optString(SEBALAppConstants.JSON_SOLAR_RADIATION) == null) {
			solarRad = "NA";
		}

		toReturn.append(stationId + ";" + dateValue + ";" + timeValue + ";"
				+ latitude + ";" + longitude + ";" + windSpeed + ";" + airTemp
				+ ";" + dewTemp + ";" + avgAirTemp + ";" + relativeHumidity
				+ ";" + minTemp + ";" + maxTemp + ";" + solarRad + ";\n");
	}

	public double zx(double lat, double lon) {
		if (properties.getProperty("altitude_sensor_velocidade") != null) {
			return Double.parseDouble(properties
					.getProperty("altitude_sensor_velocidade"));
		}
		return 6.;
	}

	public double d(double lat, double lon) {
		return 4. * 2 / 3;
	}

	public double hc(double lat, double lon) {
		if (properties.getProperty("hc") != null) {
			return Double.parseDouble(properties.getProperty("hc"));
		}
		return 4.0;
	}

	public String getStationData(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		return readFullRecord(date, station, 0);
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	public static void main(String[] args) throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);

		WeatherStation weatherStation = new WeatherStation();
		weatherStation.setProperties(properties);
		weatherStation.readStation("821980", "19840101", "19840101");
	}
}
