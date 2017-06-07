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

	public WeatherStation() throws URISyntaxException, HttpException,
			IOException {
		this(new Properties());
	}

	public WeatherStation(Properties properties) throws URISyntaxException,
			HttpException, IOException {
		this.properties = properties;
		this.stationOperator = new FTPStationOperator(properties);
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

	protected String readFullRecord(Date date, List<JSONObject> stations,
			int numberOfDays) {
		Date begindate = new Date(date.getTime() - numberOfDays * StationOperatorConstants.A_DAY);
		Date endDate = new Date(date.getTime() + numberOfDays * StationOperatorConstants.A_DAY);

		for (JSONObject station : stations) {
			try {
				JSONArray stationData = stationOperator.readStation(station.optString("id"),
						StationOperatorConstants.DATE_FORMAT.format(begindate),
						StationOperatorConstants.DATE_FORMAT.format(endDate));

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

						Date recordDate = StationOperatorConstants.DATE_TIME_FORMAT.parse(dateValue
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
		List<JSONObject> nearStations = stationOperator.findNearestStation(date, lat, lon, 0);
		if(nearStations != null) {			
			return readFullRecord(date, nearStations, 0);
		}
		
		return null;
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
		
		FTPStationOperator stationOperator = new FTPStationOperator(properties);
		stationOperator.readStation("821980", "19840101", "19840101");
	}
}
