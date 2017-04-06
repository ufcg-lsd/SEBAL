package org.fogbowcloud.sebal.parsers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class WeatherStation {

	private static final double R = 6371; // km
	private static final long A_DAY = 1000 * 60 * 60 * 24;
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("YYYYMMdd");
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/YYYY;hhmm");
	
	private static final Logger LOGGER = Logger.getLogger(WeatherStation.class);
	private static final String PUBLIC_HTML_STATION_REPOSITORY = "public_html_station_repository";
	private static final String UNFORMATTED_LOCAL_STATION_FILE_PATH = "unformatted_local_station_file_path";
	
	private Map<String, String> cache = new HashMap<String, String>();
	private JSONArray stations;
	private HttpClient httpClient;
	private Properties properties;
	
	public WeatherStation() throws URISyntaxException, HttpException, IOException {
		this (new Properties());
	}
	
	public WeatherStation(Properties properties) throws URISyntaxException, HttpException,
			IOException {
		this.httpClient = initClient();
		this.stations = new JSONArray(IOUtils.toString(
				new FileInputStream("stations.json")));
		this.properties = properties;
	}

	private List<JSONObject> findNearestStation(double lat, double lon) {
		List<JSONObject> orderedStations = new LinkedList<JSONObject>();
		double minDistance = Double.MAX_VALUE;
		for (int i = 0; i < stations.length(); i++) {
			JSONObject station = stations.optJSONObject(i);
			double d = d(lat, lon, station.optDouble("lat"), station.optDouble("lon"));
			if (d < minDistance) {
				minDistance = d;
			}
			station.put("d", d);
			orderedStations.add(station);
		}
		
		Collections.sort(orderedStations, new Comparator<JSONObject>() {

			@Override
			public int compare(JSONObject o1, JSONObject o2) {
				return ((Double)o1.optDouble("d")).compareTo((Double)o2.optDouble("d"));
			}
		});
		
		return orderedStations;
	}
	
	private double d(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2-lat1);
		double dLon = Math.toRadians(lon2-lon1);
		lat1 = Math.toRadians(lat1);
		lat2 = Math.toRadians(lat2);
		double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
		        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
		return R * c;
	}
	
	public void persistStations() throws IOException {
		JSONArray stations = new JSONArray();
		for (String line  : IOUtils.readLines(new FileInputStream("stations.html"))) {
			if (line.contains("var html")) {
				String[] split = line.split("<br />");
				JSONObject json = new JSONObject();
				String split0 = split[0];
				String split00 = split0.substring(split[0].lastIndexOf(":") + 1, 
						split[0].indexOf("</b>")).trim();
				
				json.put("id", split00.split("-")[0].trim());
				json.put("name", split00.substring(split00.indexOf("-") + 1).trim());
				json.put("lat", Double.parseDouble(split[1].substring(split[1].indexOf(":") + 1).trim()));
				json.put("lon", Double.parseDouble(split[2].substring(split[2].indexOf(":") + 1).trim()));
				json.put("altitude", Double.parseDouble(split[3].substring(
						split[3].indexOf(":") + 1).replaceAll("m", "").trim()));
				stations.put(json);
			}
		}
		IOUtils.write(stations.toString(2), new FileOutputStream("stations.json"));
	}

	private HttpClient initClient() throws IOException,
			ClientProtocolException, UnsupportedEncodingException {
		BasicCookieStore cookieStore = new BasicCookieStore();
		HttpClient httpClient = HttpClientBuilder.create().setDefaultCookieStore(cookieStore).build();
		
		HttpGet homeGet = new HttpGet(
				"http://www.inmet.gov.br/projetos/rede/pesquisa/inicio.php");
		httpClient.execute(homeGet);
		
		HttpPost homePost = new HttpPost(
				"http://www.inmet.gov.br/projetos/rede/pesquisa/inicio.php");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("mUsuario", ""));
		nvps.add(new BasicNameValuePair("mGerModulo", ""));
		nvps.add(new BasicNameValuePair("mCod", "abmargb@gmail.com"));
		nvps.add(new BasicNameValuePair("mSenha", "9oo9xyyd"));
		nvps.add(new BasicNameValuePair("mGerModulo", "PES"));
		nvps.add(new BasicNameValuePair("btnProcesso", " Acessar "));
		
		homePost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
		HttpResponse homePostResponse = httpClient.execute(homePost);
		EntityUtils.toString(homePostResponse.getEntity());
		return httpClient;
	}
	
	private JSONArray readStation(HttpClient httpClient, String id, String inicio, String fim)
			throws Exception {		

		String[] inicioSplit = inicio.split("-");
		String day = inicioSplit[0];
		String year = inicioSplit[2];
		
		File unformattedLocalStationFile = getUnformattedStationFile();		
		
		String url = properties.getProperty(PUBLIC_HTML_STATION_REPOSITORY) + File.separator + year + id + "0-99999-" + year;		
		downloadUnformattedStationFile(httpClient, unformattedLocalStationFile, url);
		
		List<String> stationData = new ArrayList<String>();		
		readStationFile(unformattedLocalStationFile, stationData);
		
		JSONArray dataArray = new JSONArray();		
		getHourlyData(day, stationData, dataArray);
		
		for (int i = 0; i < dataArray.length(); i++) {
			JSONObject stationDataRecord = dataArray.optJSONObject(i);
			String airTemp = stationDataRecord.optString("TempBulboSeco");
			String dewTemp = stationDataRecord.optString("TempBulboUmido");
			String windSpeed = stationDataRecord.optString("VelocidadeVento");
			
			if (!airTemp.isEmpty() && !dewTemp.isEmpty()
					&& !windSpeed.isEmpty()) {
				unformattedLocalStationFile.delete();
				return dataArray;
			}
		}
		
		cache.put(url, "FAILED");
		throw new Exception();
	}

	private File getUnformattedStationFile() {		
		String unformattedLocalStationFilePath = properties
				.getProperty(UNFORMATTED_LOCAL_STATION_FILE_PATH);
		
		File unformattedLocalStationFile = new File(unformattedLocalStationFilePath);
		if(unformattedLocalStationFile.exists()) {
			 LOGGER.info("File " + unformattedLocalStationFile + " already exists. Will be removed before repeating download");
			 unformattedLocalStationFile.delete();
		}
		return unformattedLocalStationFile;
	}

	private void downloadUnformattedStationFile(HttpClient httpClient,
			File unformattedLocalStationFile, String url) throws Exception {
		try {
			HttpGet fileGet = new HttpGet(url);
			HttpResponse response = httpClient.execute(fileGet);

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
	}
	
	private void readStationFile(File unformattedLocalStationFile,
			List<String> stationData) throws FileNotFoundException, IOException {
		BufferedReader br = new BufferedReader(new FileReader(unformattedLocalStationFile));
		String line = null;
		while ((line = br.readLine()) != null) {
			stationData.add(line);
		}

		br.close();
	}

	private void getHourlyData(String day, List<String> stationData,
			JSONArray dataArray) throws JSONException {
		for (String data : stationData) {
			if (data.contains(day)) {
				JSONObject jsonObject = new JSONObject();

				String stationId = data.substring(4, 9);
				String date = data.substring(15, 22);
				String time = data.substring(23, 26);
				String latitude = data.substring(28, 33);
				String longitude = data.substring(34, 40);
				String windSpeed = data.substring(65, 68);
				String airTemp = data.substring(87, 91);
				String dewTemp = data.substring(93, 97);

				jsonObject.put("Estacao", stationId);
				jsonObject.put("Data", date);
				jsonObject.put("Hora", time);
				jsonObject.put("Latitude", latitude);
				jsonObject.put("Longitude", longitude);
				jsonObject.put("VelocidadeVento", windSpeed);
				jsonObject.put("TempBulboSeco", airTemp);
				jsonObject.put("TempBulboUmido", dewTemp);

				dataArray.put(jsonObject);
			}
		}
	}
	
	private JSONObject findClosestRecord(Date date, List<JSONObject> stations) {
		Date inicio = new Date(date.getTime() - A_DAY);
		Date fim = new Date(date.getTime() + A_DAY);
		
		JSONObject closestRecord = null;
		Long smallestDiff = Long.MAX_VALUE;
		
		for (JSONObject station : stations) {
			try {
				JSONArray stationData = readStation(httpClient, station.optString("id"), 
						DATE_FORMAT.format(inicio), DATE_FORMAT.format(fim));
				for (int i = 0; i < stationData.length(); i++) {
					JSONObject stationDataRecord = stationData.optJSONObject(i);
					String dateValue = stationDataRecord.optString("Data");
					String timeValue = stationDataRecord.optString("Hora");
					
					if (!stationDataRecord.optString("TempBulboSeco").isEmpty() && !stationDataRecord.optString("VelocidadeVento").isEmpty()) {
						Date recordDate = DATE_TIME_FORMAT.parse(dateValue + ";" + timeValue);
						long diff = Math.abs(recordDate.getTime() - date.getTime());
						if (diff < smallestDiff) {
							smallestDiff = diff;
							closestRecord = stationDataRecord;
						}						
					}
					
				}
				
				return closestRecord;
			} catch (Exception e) {
				LOGGER.error("Error while reading station.", e);
			}
		}
		return null;
		
	}
	
	private String readFullRecord(Date date, List<JSONObject> stations, int numberOfDays) {
		Date inicio = new Date(date.getTime() - numberOfDays * A_DAY);
		Date fim = new Date(date.getTime() + numberOfDays * A_DAY);
		
		for (JSONObject station : stations) {
			try {
				JSONArray stationData = readStation(httpClient,
						station.optString("id"), DATE_FORMAT.format(inicio),
						DATE_FORMAT.format(fim));

				JSONObject closestRecord = null;
				Long smallestDiff = Long.MAX_VALUE;

				for (int i = 0; i < stationData.length(); i++) {
					JSONObject stationDataRecord = stationData.optJSONObject(i);
					String dateValue = stationDataRecord.optString("Data");
					String timeValue = stationDataRecord.optString("Hora");

					Date recordDate = DATE_TIME_FORMAT.parse(dateValue + ";"
							+ timeValue);
					long diff = Math.abs(recordDate.getTime() - date.getTime());
					if (diff < smallestDiff) {
						smallestDiff = diff;
						closestRecord = stationDataRecord;
					}
					
					if (!closestRecord.optString("Data").isEmpty()
							&& !closestRecord.optString("Hora").isEmpty()
							&& !closestRecord.optString("Latitude").isEmpty()
							&& !closestRecord.optString("Longitude").isEmpty()
							&& !closestRecord.optString("TempBulboSeco")
									.isEmpty()
							&& !closestRecord.optString("TempBulboUmido")
									.isEmpty()
							&& !closestRecord.optString("VelocidadeVento")
									.isEmpty()
							&& Double.parseDouble(closestRecord
									.optString("VelocidadeVento")) >= 0.3) {
						return generateStationData(stationData, closestRecord);
					} else if(Double.parseDouble(closestRecord
							.optString("VelocidadeVento")) < 0.3) {
						closestRecord.remove("VelocidadeVento");
						closestRecord.put("VelocidadeVento", "0.3");
					}
				} 
			} catch(Exception e) {
				LOGGER.error("Error while reading full record", e);
			}
		}		
		
		return null;
	}

	private String generateStationData(JSONArray stationData, JSONObject closestRecord) {
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

		String estacao = stationDataRecord.optString("Estacao");
		String dateValue = stationDataRecord.optString("Data");
		String timeValue = stationDataRecord.optString("Hora");
		String latitude = stationDataRecord.optString("Latitude");
		String longitude = stationDataRecord.optString("Longitude");
		String velocidadeVento = stationDataRecord.optString("VelocidadeVento");
		String temBulboSeco = stationDataRecord.optString("TempBulboSeco");
		String temBulboUmido = stationDataRecord.optString("TempBulboUmido");
		String mediaTemp = stationDataRecord.optString("MediaTemperatura");
		String umidadeRelativa = stationDataRecord.optString("UmidadeRelativa");
		String minTemp = stationDataRecord.optString("TemperaturaMinima");
		String maxTemp = stationDataRecord.optString("TemperaturaMaxima");
		String solarRad = stationDataRecord.optString("RadiacaoSolar");
		
		if (closestRecord.optString("VelocidadeVento").isEmpty()
				|| closestRecord.optString("VelocidadeVento") == null) {
			velocidadeVento = "NA";
		}
		if (closestRecord.optString("TempBulboSeco").isEmpty()
				|| closestRecord.optString("TempBulboSeco") == null) {
			temBulboSeco = "NA";
		}
		if (closestRecord.optString("TempBulboUmido").isEmpty()
				|| closestRecord.optString("TempBulboUmido") == null) {
			temBulboUmido = "NA";
		}
		if (closestRecord.optString("MediaTemperatura").isEmpty()
				|| closestRecord.optString("MediaTemperatura") == null) {
			mediaTemp = "NA";
		}
		if (closestRecord.optString("UmidadeRelativa").isEmpty()
				|| closestRecord.optString("UmidadeRelativa") == null) {
			umidadeRelativa = "NA";
		}
		if (closestRecord.optString("TemperaturaMinima").isEmpty()
				|| closestRecord.optString("TemperaturaMinima") == null) {
			minTemp = "NA";
		}
		if (closestRecord.optString("TemperaturaMaxima").isEmpty()
				|| closestRecord.optString("TemperaturaMaxima") == null) {
			maxTemp = "NA";
		}
		if (closestRecord.optString("RadiacaoSolar").isEmpty()
				|| closestRecord.optString("RadiacaoSolar") == null) {
			solarRad = "NA";
		}
		
		toReturn.append(estacao + ";" + dateValue + ";" + timeValue + ";"
				+ latitude + ";" + longitude + ";" + velocidadeVento + ";"
				+ temBulboSeco + ";" + temBulboUmido + ";" + mediaTemp
				+ ";" + umidadeRelativa + ";" + minTemp + ";" + maxTemp
				+ ";" + solarRad + ";\n");
	}

	public double Ta(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		JSONObject record = findClosestRecord(date, station);
		if (record == null) {
			return Double.NaN;
		}
//		System.out.println("record: " + record);
		//TODO review it
//		return Double.parseDouble(record.optString("TempBulboSeco"));
//		return 32.23;
//		return 18.21; //Europe
		if (properties.getProperty("temperatura_ar") != null) {
			return Double.parseDouble(properties.getProperty("temperatura_ar"));
		}
		return Double.parseDouble(record.optString("TempBulboSeco"));	
	}
	
	public double ux(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		JSONObject record = findClosestRecord(date, station);
		if (record == null) {
			return Double.NaN;
		}
		//TODO review it
//		return Math.max(Double.parseDouble(record.optString("VelocidadeVento")), 1.);
//		return 4.388;
//		return 2.73; //Europe
//		return Double.parseDouble(properties.getProperty("velocidade_vento"));
		if (properties.getProperty("velocidade_vento") != null) {
			return Double.parseDouble(properties.getProperty("velocidade_vento"));
		}
		return Math.max(Double.parseDouble(record.optString("VelocidadeVento")), 1.);
	}

	public double zx(double lat, double lon) {
//		List<JSONObject> station = findNearestStation(lat, lon);
//		return station.get(0).optDouble("altitude");
		//TODO Procurar a altitude do sensor da velocidade do vento
//		return 6.;
//		return 7.3; //Europe
		if (properties.getProperty("altitude_sensor_velocidade") != null){
			return Double.parseDouble(properties.getProperty("altitude_sensor_velocidade"));
		}
		return 6.;
	}

	public double d(double lat, double lon) {
		return 4. * 2/3;
	}
	
	public double hc(double lat, double lon) {
//		return 7.3; //Europe
//		return 4.0;
		if (properties.getProperty("hc") != null) {
			return Double.parseDouble(properties.getProperty("hc"));
		}
		return 4.0;
	}
	
	public String getNOAAStationData(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		return readFullRecord(date, station, 0);
	}

	public String getStationData(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		//return readClosestRecord(date, station, 0);
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
	}
}
