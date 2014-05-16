package org.fogbowcloud.sebal.parsers;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
import org.json.JSONArray;
import org.json.JSONObject;


public class WeatherStation {

	private static final String SEP = "--------------------";
	private static final double R = 6371; // km
	private static final long A_DAY = 1000 * 60 * 60 * 24;
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-YYYY");
	private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/YYYY;hhmm");
	
	private JSONArray stations;
	private HttpClient httpClient;
	
	public WeatherStation() throws URISyntaxException, HttpException,
			IOException {
		this.httpClient = initClient();
		this.stations = new JSONArray(IOUtils.toString(
				new FileInputStream("stations.json")));
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
			throws IOException, ClientProtocolException {
		HttpGet dataGet = new HttpGet(
				"http://www.inmet.gov.br/projetos/rede/pesquisa/gera_serie_txt.php?"
						+ "&mRelEstacao=" + id 
						+ "&btnProcesso=serie"
						+ "&mRelDtInicio=" + inicio
						+ "&mRelDtFim=" + fim
						+ "&mAtributos=1,1,,,1,1,,1,1,,,,,,,,");
		HttpResponse dataResponse = httpClient.execute(dataGet);
		String data = EntityUtils.toString(dataResponse.getEntity());
		data = data.substring(data.indexOf("<pre>") + 5, data.indexOf("</pre>"));
		String[] meta = data.split(SEP)[4].trim().split("\n");
		
		JSONArray dataArray = new JSONArray();
		
		String[] splitHeader = meta[0].split(";");
		
		for (int i = 1; i < meta.length; i++) {
			JSONObject jsonObject = new JSONObject();
			String[] lineSplit = meta[i].split(";");
			for (int j = 0; j < lineSplit.length; j++) {
				jsonObject.put(splitHeader[j], lineSplit[j]);
			}
			dataArray.put(jsonObject);
		}
		
		return dataArray;
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
					
					Date recordDate = DATE_TIME_FORMAT.parse(dateValue + ";" + timeValue);
					long diff = Math.abs(recordDate.getTime() - date.getTime());
					if (diff < smallestDiff) {
						smallestDiff = diff;
						closestRecord = stationDataRecord;
					}
				}
				
				return closestRecord;
			} catch (Exception e) {
//				return null;
			}
		}
		return null;
		
	}

	public double Ta(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		JSONObject record = findClosestRecord(date, station);
		if (record == null) {
			return Double.NaN;
		}
		return Double.parseDouble(record.optString("TempBulboSeco"));
	}
	
	public double ux(double lat, double lon, Date date) {
		List<JSONObject> station = findNearestStation(lat, lon);
		JSONObject record = findClosestRecord(date, station);
		if (record == null) {
			return Double.NaN;
		}
		return Double.parseDouble(record.optString("VelocidadeVento"));
	}

	public double zx(double lat, double lon) {
//		List<JSONObject> station = findNearestStation(lat, lon);
//		return station.get(0).optDouble("altitude");
		//TODO Procurar a altitude do sensor da velocidade do vento
		return 3;
	}

	public double d(double lat, double lon) {
		return 0;
	}
	
	public static void main(String[] args) throws URISyntaxException, HttpException, IOException {
		new WeatherStation();
	}
}
