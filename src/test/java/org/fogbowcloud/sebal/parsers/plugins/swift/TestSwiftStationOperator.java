package org.fogbowcloud.sebal.parsers.plugins.swift;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestSwiftStationOperator {
	
	@Test
	public void testFindNearestStationCorrectCalculation() throws URISyntaxException, HttpException, IOException, ParseException {
		
		// set up
		Properties properties = mock(Properties.class);
		String year = "2002";
		int numberOfDays = 0;
		double lat = -3.40;
		double lon = -45.20;
		
		String stringDate = "26-01-2002";		
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);
		
		JSONObject stationOne = new JSONObject();
		stationOne.put("id", "82294");
		stationOne.put("lon", "-40.13333333");
		stationOne.put("altitude", "16.5");
		stationOne.put("name", "ACARAU - CE");
		stationOne.put("lat", "-2.88333333");
		
		JSONObject stationTwo = new JSONObject();
		stationTwo.put("id", "83096");
		stationTwo.put("lon", "-37.05");
		stationTwo.put("altitude", "4.72");
		stationTwo.put("name", "ARACAJU - SE");
		stationTwo.put("lat", "-10.95");
		
		JSONArray stations = new JSONArray();
		stations.put(stationOne);
		stations.put(stationTwo);
		
		List<JSONObject> expectedStation = new ArrayList<JSONObject>();
		expectedStation.add(stationOne);
		
		SwiftStationOperator stationOperator = spy(new SwiftStationOperator(properties));
		doReturn(stations).when(stationOperator).getStations(year);
		
		// exercise
		List<JSONObject> chosenStation = stationOperator.findNearestStation(date, lat, lon, numberOfDays);
		
		// expect
		Assert.assertEquals(expectedStation, chosenStation);
	}
}
