package org.fogbowcloud.sebal.parsers;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestWeatherStation {
	
	private Date date;
	
	@Before
	public void setUp() {
		long dateInLong = 758257200; // timestamp correspondent to 1994/01/11
		date = new Date(TimeUnit.SECONDS.toMillis(dateInLong));
	}
	
	@Test
	public void testReadFullRecordProperly() throws URISyntaxException, HttpException, IOException {
		// set up
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);
		
		JSONObject stationOne = new JSONObject();
		stationOne.put("id", "82294");
		stationOne.put("lon", "-40.13333333");
		stationOne.put("altitude", "16.5");
		stationOne.put("name", "ACARAU - CE");
		stationOne.put("lat", "-2.88333333");
		
		JSONObject stationTwo = new JSONObject();
		stationOne.put("id", "83096");
		stationOne.put("lon", "-37.05");
		stationOne.put("altitude", "4.72");
		stationOne.put("name", "ARACAJU - SE");
		stationOne.put("lat", "-10.95");
		
		List<JSONObject> stations = new ArrayList<JSONObject>();
		stations.add(stationOne);
		stations.add(stationTwo);
		
		WeatherStation weatherStation = new WeatherStation(properties);
		
		// exercise
		String actualStationData = weatherStation.readFullRecord(date, stations, 0);
				
		// expect
		Assert.assertNotNull(actualStationData);
	}
}
