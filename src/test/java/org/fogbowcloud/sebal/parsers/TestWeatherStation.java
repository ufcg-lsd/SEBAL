package org.fogbowcloud.sebal.parsers;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.fogbowcloud.sebal.parsers.plugins.ftp.FTPStationOperator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vividsolutions.jts.util.Assert;

public class TestWeatherStation {

	Properties properties = new Properties();
	WeatherStation weatherStation;

	@Before
	public void setUp() throws IOException {
		FileInputStream input = new FileInputStream("sebal.conf");
		this.properties.load(input);
	}

	@Test
	public void testGetStationData() throws Exception {

		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		String stringDate = "26-01-2002";
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);

		List<JSONObject> nearStations = new LinkedList<JSONObject>();
		JSONObject station1 = new JSONObject(
				"{\"distance\":5.9728933028073445,\"lon\":\"-42.82\",\"id\":\"825790\",\"lat\":\"-5.05\"}");
		JSONObject station2 = new JSONObject(
				"{\"distance\":6.929536290402324,\"lon\":\"-42.82\",\"id\":\"825780\",\"lat\":\"-5.07\"}");
		JSONObject station3 = new JSONObject(
				"{\"distance\":64.49814878855975,\"lon\":\"-43.32\",\"id\":\"824760\",\"lat\":\"-4.85\"}");

		nearStations.add(station1);
		nearStations.add(station2);
		nearStations.add(station3);

		JSONArray stationRecords = new JSONArray(
				"[{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"26.0\",\"TempBulboSeco\":\"28.1\",\"Hora\":\"0000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"0100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"26.0\",\"Hora\":\"0200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.1\",\"TempBulboSeco\":\"25.5\",\"Hora\":\"0300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"15.4\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.5\",\"Hora\":\"0600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"1000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"1100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"28.0\",\"Hora\":\"1300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.0\",\"Hora\":\"1400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.9\",\"Hora\":\"1500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"1600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"31.0\",\"Hora\":\"1700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"23.1\",\"TempBulboSeco\":\"32.5\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"1900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"2000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"22.9\",\"TempBulboSeco\":\"32.3\",\"Hora\":\"2100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"2200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.6\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"2300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.findNearestStation(Mockito.any(Date.class), Mockito.anyDouble(),
				Mockito.anyDouble(), Mockito.anyInt())).thenReturn(nearStations);

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);
		
		String expected = "825790;20020126;0000;-5.05;-42.82;2.6;28.1;26.0;NA;NA;NA;NA;NA;"
				+ System.lineSeparator()
				+ "825790;20020126;1200;-5.05;-42.82;1.5;26.8;24.6;NA;NA;NA;NA;NA;"
				+ System.lineSeparator()
				+ "825790;20020126;1800;-5.05;-42.82;2.1;32.5;23.1;NA;NA;NA;NA;NA;";

		String actual = weatherStation.getStationData(-5.035041, -42.768209, date);

		Assert.equals(expected, actual);
	}

	@Test
	public void testSelectStationRecords() throws Exception {

		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		String stringDate = "26-01-2002";
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);

		List<JSONObject> nearStations = new LinkedList<JSONObject>();
		JSONObject station1 = new JSONObject(
				"{\"distance\":5.9728933028073445,\"lon\":\"-42.82\",\"id\":\"825790\",\"lat\":\"-5.05\"}");
		JSONObject station2 = new JSONObject(
				"{\"distance\":6.929536290402324,\"lon\":\"-42.82\",\"id\":\"825780\",\"lat\":\"-5.07\"}");
		JSONObject station3 = new JSONObject(
				"{\"distance\":64.49814878855975,\"lon\":\"-43.32\",\"id\":\"824760\",\"lat\":\"-4.85\"}");

		nearStations.add(station1);
		nearStations.add(station2);
		nearStations.add(station3);

		JSONArray stationRecords = new JSONArray(
				"[{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"26.0\",\"TempBulboSeco\":\"28.1\",\"Hora\":\"0000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"0100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"26.0\",\"Hora\":\"0200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.1\",\"TempBulboSeco\":\"25.5\",\"Hora\":\"0300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"15.4\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.5\",\"Hora\":\"0600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"1000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"1100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"28.0\",\"Hora\":\"1300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.0\",\"Hora\":\"1400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.9\",\"Hora\":\"1500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"1600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"31.0\",\"Hora\":\"1700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"23.1\",\"TempBulboSeco\":\"32.5\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"1900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"2000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"22.9\",\"TempBulboSeco\":\"32.3\",\"Hora\":\"2100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"2200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.6\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"2300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);

		String expected = "825790;20020126;0000;-5.05;-42.82;2.6;28.1;26.0;NA;NA;NA;NA;NA;"
				+ System.lineSeparator()
				+ "825790;20020126;1200;-5.05;-42.82;1.5;26.8;24.6;NA;NA;NA;NA;NA;"
				+ System.lineSeparator()
				+ "825790;20020126;1800;-5.05;-42.82;2.1;32.5;23.1;NA;NA;NA;NA;NA;";

		String actual = weatherStation.selectStationRecords(date, nearStations, 0);

		Assert.equals(expected, actual);

		assertNull(this.weatherStation.selectStationRecords(date, null, 0));
	}

}
