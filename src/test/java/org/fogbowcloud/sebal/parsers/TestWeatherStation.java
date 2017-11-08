package org.fogbowcloud.sebal.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.http.HttpException;
import org.junit.Before;
import org.junit.Test;

public class TestWeatherStation {
	
	Properties properties = new Properties();
	WeatherStation weatherStation;
	
	@Before
	public void setUp() throws IOException, URISyntaxException, HttpException {
		FileInputStream input = new FileInputStream("sebal.conf");
		this.properties.load(input);
		
		this.weatherStation = new WeatherStation(this.properties);
	}

	@Test
	public void testGetStationData() throws ParseException {
		
		String stringDate = "26-01-2002";		
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);
		
		double lat = -5.035041;
		double lon = -42.768209;
		String result = this.weatherStation.getStationData(lat, lon, date);
		
		System.out.println(result);
	}
	
}
