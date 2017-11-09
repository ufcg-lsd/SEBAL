package org.fogbowcloud.sebal.parsers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.parsers.plugins.StationOperator;
import org.fogbowcloud.sebal.parsers.plugins.StationOperatorConstants;
import org.fogbowcloud.sebal.parsers.plugins.ftp.FTPStationOperator;
import org.fogbowcloud.sebal.util.SEBALAppConstants;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherStation {

	private Properties properties;
	private StationOperator stationOperator;

	private static final Logger LOGGER = Logger.getLogger(WeatherStation.class);

	private static final String[] WANTED_STATION_HOURS = new String[] { "0000", "1200", "1800" };
	private static final String MINIMUM_WIND_SPEED_VALUE = "0.3";

	public WeatherStation() throws URISyntaxException, HttpException, IOException {
		this(new Properties());
	}

	public WeatherStation(Properties properties)
			throws URISyntaxException, HttpException, IOException {
		this(properties, new FTPStationOperator(properties));
	}

	protected WeatherStation(Properties properties, FTPStationOperator stationOperator) {
		this.properties = properties;
		this.stationOperator = stationOperator;
	}

	public void persistStations() throws IOException {
		JSONArray stations = new JSONArray();
		for (String line : IOUtils.readLines(new FileInputStream("stations.html"))) {
			if (line.contains("var html")) {
				String[] split = line.split("<br />");
				JSONObject json = new JSONObject();
				String split0 = split[0];
				String split00 = split0
						.substring(split[0].lastIndexOf(":") + 1, split[0].indexOf("</b>")).trim();

				json.put("id", split00.split("-")[0].trim());
				json.put("name", split00.substring(split00.indexOf("-") + 1).trim());
				json.put("lat",
						Double.parseDouble(split[1].substring(split[1].indexOf(":") + 1).trim()));
				json.put("lon",
						Double.parseDouble(split[2].substring(split[2].indexOf(":") + 1).trim()));
				json.put("altitude", Double.parseDouble(
						split[3].substring(split[3].indexOf(":") + 1).replaceAll("m", "").trim()));
				stations.put(json);
			}
		}
		IOUtils.write(stations.toString(2), new FileOutputStream("stations.json"));
	}

	public double zx(double lat, double lon) {
		if (properties.getProperty("altitude_sensor_velocidade") != null) {
			return Double.parseDouble(properties.getProperty("altitude_sensor_velocidade"));
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
		LOGGER.debug("latitude: " + lat + " longitude: " + lon + " date: " + date);

		int daysWindow = 0;
		List<JSONObject> nearStations = stationOperator.findNearestStation(date, lat, lon,
				daysWindow);

		if (nearStations != null) {
			return selectStationRecords(date, nearStations, daysWindow);
		}

		return null;
	}

	protected String selectStationRecords(Date date, List<JSONObject> stations, int numberOfDays) {
		LOGGER.info("Near stations found... reading and selecting full record");
		Date begindate = new Date(date.getTime() - numberOfDays * StationOperatorConstants.A_DAY);
		Date endDate = new Date(date.getTime() + numberOfDays * StationOperatorConstants.A_DAY);

		if (stations != null && !stations.isEmpty()) {
			LOGGER.debug("beginDate: " + begindate + " endDate: " + endDate);

			for (JSONObject station : stations) {
				try {
					JSONArray stationData = stationOperator.readStation(station.optString("id"),
							StationOperatorConstants.DATE_FORMAT.format(begindate),
							StationOperatorConstants.DATE_FORMAT.format(endDate));

					windSpeedCorrection(stationData);

					if (checkRecords(stationData)) {
						return generateStationData(stationData);
					}

				} catch (Exception e) {
					LOGGER.error("Error while reading full record", e);
				}
			}
		} else {
			LOGGER.info("Stations list is empty");
		}

		return null;
	}

	private boolean checkRecords(JSONArray stationData) {
		boolean result = false;
		if (stationData != null) {

			for (int i = 0; i < stationData.length(); i++) {
				JSONObject stationDataRecord = stationData.optJSONObject(i);

				boolean isWanted = false;
				for (String hour : WeatherStation.WANTED_STATION_HOURS) {
					if (isRecord(stationDataRecord, SEBALAppConstants.JSON_STATION_TIME, hour)) {
						isWanted = true;
					}
				}

				if (!isWanted || !stationContainsAll(stationDataRecord)) {
					stationData.remove(i);
					i--;
				}
			}

			boolean hasAll = true;
			for (String hour : WeatherStation.WANTED_STATION_HOURS) {
				if (!hasRecord(stationData, SEBALAppConstants.JSON_STATION_TIME, hour)) {
					hasAll = false;
				}
			}
			result = hasAll;
		}
		return result;
	}

	private boolean hasRecord(JSONArray stationData, String key, String value) {
		boolean result = false;
		for (int i = 0; i < stationData.length() && !result; i++) {
			JSONObject stationDataRecord = stationData.optJSONObject(i);
			if (!stationDataRecord.optString(key).isEmpty()) {
				result = true;
			}
		}
		return result;
	}

	private boolean isRecord(JSONObject stationDataRecord, String key, String value) {
		boolean result = false;
		if (stationDataRecord.optString(key).equals(value)) {
			result = true;
		}
		return result;
	}

	private void windSpeedCorrection(JSONArray stationData) {
		if (stationData != null) {
			for (int i = 0; i < stationData.length(); i++) {
				JSONObject stationDataRecord = stationData.optJSONObject(i);

				if (Double.parseDouble(stationDataRecord
						.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)) < Double
								.parseDouble(WeatherStation.MINIMUM_WIND_SPEED_VALUE)) {

					stationDataRecord.remove(SEBALAppConstants.JSON_STATION_WIND_SPEED);
					stationDataRecord.put(SEBALAppConstants.JSON_STATION_WIND_SPEED,
							WeatherStation.MINIMUM_WIND_SPEED_VALUE);
				}
			}
		}
	}

	private boolean stationContainsAll(JSONObject station) {
		return !station.optString(SEBALAppConstants.JSON_STATION_DATE).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_STATION_TIME).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_STATION_LATITUDE).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_STATION_LONGITUDE).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE).isEmpty()
				&& !station.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED).isEmpty()
				&& Double.parseDouble(
						station.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED)) >= 0.3;
	}

	private String generateStationData(JSONArray stationData) {
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < stationData.length(); i++) {
			result.append(checkVariablesAndBuildString(stationData.optJSONObject(i)));
		}
		return result.toString().trim();
	}

	private String checkVariablesAndBuildString(JSONObject stationDataRecord) {

		String stationId = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_ID);
		String dateValue = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_DATE);
		String timeValue = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_TIME);
		String latitude = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_LATITUDE);
		String longitude = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_LONGITUDE);
		String windSpeed = stationDataRecord.optString(SEBALAppConstants.JSON_STATION_WIND_SPEED);
		String airTemp = stationDataRecord.optString(SEBALAppConstants.JSON_AIR_TEMPERATURE);
		String dewTemp = stationDataRecord.optString(SEBALAppConstants.JSON_DEWPOINT_TEMPERATURE);
		String avgAirTemp = stationDataRecord.optString(SEBALAppConstants.JSON_AVG_AIR_TEMPERATURE);
		String relativeHumidity = stationDataRecord
				.optString(SEBALAppConstants.JSON_RELATIVE_HUMIDITY);
		String minTemp = stationDataRecord.optString(SEBALAppConstants.JSON_MIN_TEMPERATURE);
		String maxTemp = stationDataRecord.optString(SEBALAppConstants.JSON_MAX_TEMPERATURE);
		String solarRad = stationDataRecord.optString(SEBALAppConstants.JSON_SOLAR_RADIATION);

		stationId = stationDataCorrection(stationId);
		avgAirTemp = stationDataCorrection(avgAirTemp);
		relativeHumidity = stationDataCorrection(relativeHumidity);
		minTemp = stationDataCorrection(minTemp);
		maxTemp = stationDataCorrection(maxTemp);
		solarRad = stationDataCorrection(solarRad);

		return stationId + ";" + dateValue + ";" + timeValue + ";" + latitude + ";" + longitude
				+ ";" + windSpeed + ";" + airTemp + ";" + dewTemp + ";" + avgAirTemp + ";"
				+ relativeHumidity + ";" + minTemp + ";" + maxTemp + ";" + solarRad + ";"
				+ System.lineSeparator();
	}

	private String stationDataCorrection(String data) {
		if (data.isEmpty() || data == null) {
			data = new String("NA");
		}
		return data;
	}

	public Properties getProperties() {
		return properties;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}
}
