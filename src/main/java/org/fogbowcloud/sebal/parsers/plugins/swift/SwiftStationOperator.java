package org.fogbowcloud.sebal.parsers.plugins.swift;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.parsers.plugins.StationOperator;
import org.fogbowcloud.sebal.parsers.plugins.StationOperatorConstants;
import org.fogbowcloud.sebal.util.SEBALAppConstants;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SwiftStationOperator implements StationOperator{

	private Properties properties;	
	private String swiftStorageURL;
	private String swiftClientPath;
	private String swiftUrlExpirationTime; 
	private String swiftContainerPrefix; 
	private String swiftMetaAuthKey;
	private Map<String, String> cache = new HashMap<String, String>();
	
	private static final Logger LOGGER = Logger.getLogger(SwiftStationOperator.class);
	
	public SwiftStationOperator(Properties properties) {
		this.properties = properties;
		this.swiftStorageURL = properties.getProperty(StationOperatorConstants.SWIFT_STORAGE_URL);
		this.swiftClientPath = properties.getProperty(StationOperatorConstants.SWIFT_CLIENT_PATH);
		this.swiftUrlExpirationTime = properties.getProperty(StationOperatorConstants.SWIFT_URL_EXPIRATION_TIME);
		this.swiftContainerPrefix = properties.getProperty(StationOperatorConstants.SWIFT_CONTAINER_PREFIX);
		this.swiftMetaAuthKey = properties.getProperty(StationOperatorConstants.SWIFT_META_AUTH_KEY);
	}

	@Override
	public JSONArray getStations(String year) {
		
		String localStationsCSVFilePath = getStationCSVFilePath(year);
		String url = getStationCSVFileURL(year);
		
		if(doDownloadStationCSVFile(localStationsCSVFilePath, url)) {			
			return readStationCSVFile(localStationsCSVFilePath);
		}
		
		return null;
	}

	protected boolean doDownloadStationCSVFile(String localStationsCSVFilePath,
			String url) {
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
					localStationsCSVFilePath);
			IOUtils.copy(response.getEntity().getContent(), outStream);
			outStream.close();
		} catch(Exception e) {
			LOGGER.error("Error while downloading stations cvs file", e);
			return false;
		}
		
		return true;
	}
	
	protected String getStationCSVFilePath(String year) {
		
		return properties.getProperty(StationOperatorConstants.STATIONS_CSV_FROM_YEAR_FILE_PATH);
	}

	protected String getStationCSVFileURL(String year) {
		
		ProcessBuilder builder = new ProcessBuilder(swiftClientPath, "GET",
				swiftUrlExpirationTime, swiftContainerPrefix + File.separator + year
						+ File.separator + year + "-stations.csv", swiftMetaAuthKey);

		try {
			Process p = builder.start();
			p.waitFor();			
			String urlEndpoint = getProcessOutput(p);
			return swiftStorageURL + File.separator + urlEndpoint;
		} catch (Exception e) {
			LOGGER.error("Error while generating url for station", e);
		}
		
		return null;
	}
	
	private String getProcessOutput(Process p) throws IOException {
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		StringBuilder stringBuilder = new StringBuilder();
		String line = null;
		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append(System.getProperty("line.separator"));
		}
		
		return stringBuilder.toString();
  	}

	@Override
	public JSONArray readStationCSVFile(String localStationsCSVFilePath) {
		
		JSONArray stations = new JSONArray();
		
		try {
			File file = new File(localStationsCSVFilePath);
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				String[] lineSplit = line.split(";");
				JSONObject station = new JSONObject();
				station.put("id", lineSplit[0]);
				station.put("lat", lineSplit[1]);
				station.put("lon", lineSplit[2]);
				stations.put(station);
			}
			fileReader.close();
			file.delete();
		} catch (IOException e) {
			LOGGER.error("Error while reading stations csv file", e);
		}
		
		return stations;
	}

	@Override
	public List<JSONObject> findNearestStation(Date date, double lat,
			double lon, int numberOfDays) {
		
		Date begindate = new Date(date.getTime() - numberOfDays * StationOperatorConstants.A_DAY);
		String year = StationOperatorConstants.DATE_FORMAT.format(begindate).substring(0, 4);
		
		JSONArray stations = getStations(year);
		
		List<JSONObject> orderedStations = new LinkedList<JSONObject>();
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < stations.length(); i++) {
			JSONObject station = stations.optJSONObject(i);
			double d = d(lat, lon, station.optDouble("lat"), station.optDouble("lon"));
			if (d < minDistance) {
				minDistance = d;
				station.put("d", d);
				orderedStations.add(station);
			}
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
		return StationOperatorConstants.R * c;
	}

	@Override
	public JSONArray readStation(String stationId, String beginDate, String endDate) throws Exception {
		
		String year = beginDate.substring(0, 4);

		String baseUnformattedLocalStationFilePath = getBaseUnformattedLocalStationFilePath(year);
		File baseUnformattedLocalStationFile = new File(baseUnformattedLocalStationFilePath);
		baseUnformattedLocalStationFile.mkdirs();

		File compressedUnformattedLocalStationFile = getUnformattedStationFile(stationId, year);
		String url = getStationFileUrl(stationId, year);
		if (!downloadUnformattedStationFile(compressedUnformattedLocalStationFile, url)) {
			return null;
		}

		// Uncompressing station file
		File uncompressedUnformattedStationFile = unGzip(compressedUnformattedLocalStationFile, true);

		List<String> stationData = new ArrayList<String>();
		readStationFile(uncompressedUnformattedStationFile, stationData);
		
		compressedUnformattedLocalStationFile.delete();
		uncompressedUnformattedStationFile.delete();
		FileUtils.deleteDirectory(baseUnformattedLocalStationFile);

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
				compressedUnformattedLocalStationFile.delete();
				return dataArray;
			}
		}

		cache.put(url, "FAILED");
		throw new Exception();
	}
	
	protected String getBaseUnformattedLocalStationFilePath(String year) {
		
		return properties
				.getProperty(StationOperatorConstants.UNFORMATTED_LOCAL_STATION_FILE_PATH)
				+ File.separator + year;
	}

	protected String getStationFileUrl(String stationId, String year) {
		
		return properties.getProperty(StationOperatorConstants.STATION_CSV_SERVER_URL)
				+ File.separator + year + File.separator + stationId
				+ "-99999-" + year + ".tar.gz";
	}

	protected File getUnformattedStationFile(String stationId, String year) {
		
		String unformattedLocalStationFilePath = properties.getProperty(StationOperatorConstants.UNFORMATTED_LOCAL_STATION_FILE_PATH)
				+ File.separator + year + File.separator + stationId + "-99999-" + year;

		File unformattedLocalStationFile = new File(unformattedLocalStationFilePath);
		if (unformattedLocalStationFile.exists()) {
			LOGGER.info("File "
					+ unformattedLocalStationFile
					+ " already exists. Will be removed before repeating download");
			unformattedLocalStationFile.delete();
		}
		return unformattedLocalStationFile;
	}
	
	protected boolean downloadUnformattedStationFile(File unformattedLocalStationFile, String url) throws Exception {

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
	
	public static File unGzip(File file, boolean deleteGzipfileOnSuccess) throws IOException {
		
	    GZIPInputStream gin = new GZIPInputStream(new FileInputStream(file));
	    FileOutputStream fos = null;
	    try {
	        File outFile = new File(file.getParent(), file.getName().replaceAll("\\.gz$", ""));
	        fos = new FileOutputStream(outFile);
	        byte[] buf = new byte[100000];
	        int len;
	        while ((len = gin.read(buf)) > 0) {
	            fos.write(buf, 0, len);
	        }

	        fos.close();
	        if (deleteGzipfileOnSuccess) {
	            file.delete();
	        }
	        return outFile; 
	    } finally {
	        if (gin != null) {
	            gin.close();    
	        }
	        if (fos != null) {
	            fos.close();    
	        }
	    }       
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
		
		StringBuilder sb = new StringBuilder(latitude);
		if(latitude.contains("+")) {			
			sb.deleteCharAt(0);
		}
		latitude = sb.toString();
		double latitudeValue = Double.valueOf(latitude) / 1000.0;
		return String.valueOf(latitudeValue);
	}
	
	private String changeToLongitudeFormat(String longitude) {
		
		StringBuilder sb = new StringBuilder(longitude);
		if(longitude.contains("+")) {			
			sb.deleteCharAt(0);
		}
		longitude = sb.toString();
		double longitudeValue = Double.valueOf(longitude) / 1000.0;
		return String.valueOf(longitudeValue);
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
}
