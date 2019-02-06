package org.fogbowcloud.sebal.parsers;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

		String expected = "825790;20020126;0000;-5.05;-42.82;2.6;28.1;26.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0100;-5.05;-42.82;2.6;27.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0200;-5.05;-42.82;1.0;26.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0300;-5.05;-42.82;1.0;25.5;24.1;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0400;-5.05;-42.82;1.0;25.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0500;-5.05;-42.82;1.5;25.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0600;-5.05;-42.82;15.4;24.5;23.8;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0700;-5.05;-42.82;999.9;24.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0800;-5.05;-42.82;999.9;24.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0900;-5.05;-42.82;0.3;24.0;23.8;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1000;-5.05;-42.82;1.0;24.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1100;-5.05;-42.82;999.9;25.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1200;-5.05;-42.82;1.5;26.8;24.6;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1300;-5.05;-42.82;1.5;28.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1400;-5.05;-42.82;1.5;29.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1500;-5.05;-42.82;2.1;29.9;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1600;-5.05;-42.82;1.5;30.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1700;-5.05;-42.82;3.1;31.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1800;-5.05;-42.82;2.1;32.5;23.1;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1900;-5.05;-42.82;3.1;33.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2000;-5.05;-42.82;1.5;33.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2100;-5.05;-42.82;1.0;32.3;22.9;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2200;-5.05;-42.82;2.6;30.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2300;-5.05;-42.82;3.6;27.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;";

		String actual = this.weatherStation.getStationData(-5.035041, -42.768209, date, "1200");

		assertEquals(expected, actual);
		
		actual = this.weatherStation.getStationData(-5.035041, -42.768209, date, "0600");
		
		expected = "825790;20020126;0000;-5.05;-42.82;2.6;28.1;26.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0100;-5.05;-42.82;2.6;27.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0200;-5.05;-42.82;1.0;26.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0300;-5.05;-42.82;1.0;25.5;24.1;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0400;-5.05;-42.82;1.0;25.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0500;-5.05;-42.82;1.5;25.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0600;-5.05;-42.82;15.4;24.5;23.8;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0800;-5.05;-42.82;999.9;24.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;0900;-5.05;-42.82;0.3;24.0;23.8;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1000;-5.05;-42.82;1.0;24.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1100;-5.05;-42.82;999.9;25.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1200;-5.05;-42.82;1.5;26.8;24.6;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1300;-5.05;-42.82;1.5;28.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1400;-5.05;-42.82;1.5;29.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1500;-5.05;-42.82;2.1;29.9;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1600;-5.05;-42.82;1.5;30.0;25.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1700;-5.05;-42.82;3.1;31.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1800;-5.05;-42.82;2.1;32.5;23.1;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1900;-5.05;-42.82;3.1;33.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2000;-5.05;-42.82;1.5;33.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2100;-5.05;-42.82;1.0;32.3;22.9;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2200;-5.05;-42.82;2.6;30.0;24.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;2300;-5.05;-42.82;3.6;27.0;23.0;NA;NA;NA;NA;NA;5.9728933028073445;";
		
		assertEquals(expected, actual);
		
		assertNull(this.weatherStation.getStationData(-5.035041, -42.768209, date, "0700"));
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
				"[{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"26.0\",\"TempBulboSeco\":\"28.1\",\"Hora\":\"0000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"23.1\",\"TempBulboSeco\":\"32.5\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);

		String expected = "825790;20020126;0000;-5.05;-42.82;2.6;28.1;26.0;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1200;-5.05;-42.82;1.5;26.8;24.6;NA;NA;NA;NA;NA;5.9728933028073445;"
				+ System.lineSeparator()
				+ "825790;20020126;1800;-5.05;-42.82;2.1;32.5;23.1;NA;NA;NA;NA;NA;5.9728933028073445;";

		String actual = this.weatherStation.selectStation(date, nearStations, 0, "1200");

		Assert.equals(expected, actual);

		assertNull(this.weatherStation.selectStation(date, null, 0, "1200"));
	}

	@Test
	public void testSelectStationDataInvalid() throws Exception {
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
				"[{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"26.0\",\"TempBulboSeco\":\"28.1\",\"Hora\":\"0000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"0100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"26.0\",\"Hora\":\"0200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.1\",\"TempBulboSeco\":\"25.5\",\"Hora\":\"0300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"15.4\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.5\",\"Hora\":\"0600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"1000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"1100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.0\",\"Hora\":\"1400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.9\",\"Hora\":\"1500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"1600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"31.0\",\"Hora\":\"1700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"23.1\",\"TempBulboSeco\":\"32.5\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"1900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"2000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"22.9\",\"TempBulboSeco\":\"32.3\",\"Hora\":\"2100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"2200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.6\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"2300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);

		assertNull(this.weatherStation.selectStation(date, nearStations, 0, "1200"));

		assertNull(this.weatherStation.selectStation(date, null, 0, "1200"));
	}

	@Test
	public void testSelectStationDataInvalidUpperBound() throws Exception {
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
				"[{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"28.0\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);

		assertNull(this.weatherStation.selectStation(date, nearStations, 0, "1200"));
	}
	
	@Test
	public void testSelectStationDataInvalidLowerBound() throws Exception {
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
				"[{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"28.0\",\"Hora\":\"0600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");

		Mockito.when(ftp.readStation("825790", "20020126", "20020126")).thenReturn(stationRecords);

		assertNull(this.weatherStation.selectStation(date, nearStations, 0, "1200"));
	}

	@Test
	public void testValidateStationData() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		List<String> mainHours = new ArrayList<String>();
		mainHours.add("1200");
		mainHours.add("1300");

		JSONArray station1 = new JSONArray(
				"[{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"26.0\",\"TempBulboSeco\":\"28.1\",\"Hora\":\"0000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"0100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"26.0\",\"Hora\":\"0200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.1\",\"TempBulboSeco\":\"25.5\",\"Hora\":\"0300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"0500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"15.4\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.5\",\"Hora\":\"0600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"23.8\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"0900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"24.0\",\"Hora\":\"1000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"25.0\",\"Hora\":\"1100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.6\",\"TempBulboSeco\":\"26.8\",\"Hora\":\"1200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"28.0\",\"Hora\":\"1300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.0\",\"Hora\":\"1400\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"29.9\",\"Hora\":\"1500\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"25.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"1600\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"31.0\",\"Hora\":\"1700\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.1\",\"TempBulboUmido\":\"23.1\",\"TempBulboSeco\":\"32.5\",\"Hora\":\"1800\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.1\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"1900\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.5\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"33.0\",\"Hora\":\"2000\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"1.0\",\"TempBulboUmido\":\"22.9\",\"TempBulboSeco\":\"32.3\",\"Hora\":\"2100\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"2.6\",\"TempBulboUmido\":\"24.0\",\"TempBulboSeco\":\"30.0\",\"Hora\":\"2200\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"},{\"VelocidadeVento\":\"3.6\",\"TempBulboUmido\":\"23.0\",\"TempBulboSeco\":\"27.0\",\"Hora\":\"2300\",\"Data\":\"20020126\",\"Latitude\":\"-5.05\",\"Estacao\":\"825790\",\"Longitude\":\"-42.82\"}]");
		JSONArray station2 = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"4.6\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"}]");

		assertTrue(this.weatherStation.validateStationData(station1, mainHours));
		assertFalse(this.weatherStation.validateStationData(station2, mainHours));
		assertFalse(this.weatherStation.validateStationData(null, mainHours));

		mainHours = new ArrayList<String>();
		mainHours.add("2200");
		mainHours.add("2300");

		assertTrue(this.weatherStation.validateStationData(station1, mainHours));
		assertFalse(this.weatherStation.validateStationData(station2, mainHours));
	}

	@Test
	public void testWindSpeedCorrection() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		JSONArray station = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1800\"}]");

		List<String> mainHours = new ArrayList<String>();
		mainHours.add("1200");
		mainHours.add("1300");

		JSONArray actual = this.weatherStation.windSpeedCorrection(station, mainHours);
		JSONArray expected = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"0.3\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1800\"}]");

		Assert.equals(expected.length(), actual.length());
		for (int i = 0; i < actual.length(); i++) {
			JSONObject actualObj = actual.optJSONObject(i);
			JSONObject expectedObj = expected.optJSONObject(i);

			Assert.equals(expectedObj.optString("\"VelocidadeVento\""),
					actualObj.optString("\"VelocidadeVento\""));
		}

		actual = this.weatherStation.windSpeedCorrection(station, new ArrayList<String>());
		expected = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"0.3\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1800\"}]");

		assertEquals(actual.length(), expected.length());
		for (int i = 0; i < actual.length(); i++) {
			JSONObject actualObj = actual.optJSONObject(i);
			JSONObject expectedObj = expected.optJSONObject(i);

			Assert.equals(expectedObj.optString("\"VelocidadeVento\""),
					actualObj.optString("\"VelocidadeVento\""));
		}

		assertNull(this.weatherStation.windSpeedCorrection(null, mainHours));

		actual = this.weatherStation.windSpeedCorrection(new JSONArray(), mainHours);
		expected = new JSONArray();
		assertEquals(expected.toString(), actual.toString());
	}

	@Test
	public void testTemperatureCorrection() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		JSONArray station = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"0.0\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"999.9\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"4.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"60.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1800\"}]");

		station = this.weatherStation.temperatureCorrection(station);

		JSONArray expected = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"0.3\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"}]");

		Assert.equals(expected.length(), station.length());

		for (int i = 0; i < station.length(); i++) {
			JSONObject actualObj = station.optJSONObject(i);
			JSONObject expectedObj = expected.optJSONObject(i);

			Assert.equals(expectedObj.optString("\"TempBulboSeco\""),
					actualObj.optString("\"TempBulboSeco\""));
		}

		assertNull(this.weatherStation.temperatureCorrection(null));

		station = this.weatherStation.temperatureCorrection(new JSONArray());
		expected = new JSONArray();
		assertEquals(expected.toString(), station.toString());
	}

	@Test
	public void testCheckVariablesAndBuildString() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		JSONArray station = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"4.6\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"}]");

		String actual = this.weatherStation.checkVariablesAndBuildString(station.getJSONObject(0),
				100.0);
		String expected = "827980;20170815;0000;-7.148;-34.951;4.6;23.0;21.0;NA;NA;NA;NA;NA;100.0;"
				+ System.lineSeparator();

		Assert.equals(expected, actual);
	}

	@Test
	public void testRemoveNonRepresentativeRecords() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		JSONArray station = new JSONArray(
				"[{\"Data\":\"\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"4.6\",\"TempBulboUmido\":\"21.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"23.0\",\"Longitude\":\"-34.951\",\"Hora\":\"0000\"},{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"}]");

		station = this.weatherStation.removeNonRepresentativeRecords(station);

		JSONArray expected = new JSONArray(
				"[{\"Data\":\"20170815\",\"Estacao\":\"827980\",\"VelocidadeVento\":\"5.7\",\"TempBulboUmido\":\"20.0\",\"Latitude\":\"-7.148\",\"TempBulboSeco\":\"26.0\",\"Longitude\":\"-34.951\",\"Hora\":\"1200\"}]");

		Assert.equals(expected.length(), station.length());

		assertNull(this.weatherStation.removeNonRepresentativeRecords(null));

		station = new JSONArray();
		station = this.weatherStation.removeNonRepresentativeRecords(station);

		assertEquals(new JSONArray().toString(), station.toString());
	}

	@Test
	public void testGetMainHours() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		List<String> actual = this.weatherStation.getMainHours("1200");

		List<String> expected = new ArrayList<String>();
		expected.add("1200");
		expected.add("1300");

		assertEquals(expected.toString(), actual.toString());

		actual = this.weatherStation.getMainHours("2300");
		expected = new ArrayList<String>();
		expected.add("2300");
		expected.add("0000");

		assertEquals(expected.toString(), actual.toString());
	}

	@Test
	public void testGetHoursInterval() {
		FTPStationOperator ftp = Mockito.mock(FTPStationOperator.class);

		this.weatherStation = new WeatherStation(this.properties, ftp);

		List<String> actual = this.weatherStation.getHoursInterval("1200");
		List<String> expected = new ArrayList<String>();
		expected.add("1800");
		expected.add("1900");
		expected.add("2000");
		expected.add("2100");
		expected.add("2200");
		expected.add("2300");
		expected.add("0000");
		expected.add("0100");
		expected.add("0200");
		expected.add("0300");
		expected.add("0400");
		expected.add("0500");
		expected.add("0600");

		assertEquals(actual.toString(), expected.toString());

		actual = this.weatherStation.getHoursInterval("0700");
		expected = new ArrayList<String>();
		expected.add("1300");
		expected.add("1400");
		expected.add("1500");
		expected.add("1600");
		expected.add("1700");
		expected.add("1800");
		expected.add("1900");
		expected.add("2000");
		expected.add("2100");
		expected.add("2200");
		expected.add("2300");
		expected.add("0000");
		expected.add("0100");

		assertEquals(actual.toString(), expected.toString());

		actual = this.weatherStation.getHoursInterval("2200");
		expected = new ArrayList<String>();
		expected.add("0400");
		expected.add("0500");
		expected.add("0600");
		expected.add("0700");
		expected.add("0800");
		expected.add("0900");
		expected.add("1000");
		expected.add("1100");
		expected.add("1200");
		expected.add("1300");
		expected.add("1400");
		expected.add("1500");
		expected.add("1600");

		assertEquals(actual.toString(), expected.toString());
	}

}
