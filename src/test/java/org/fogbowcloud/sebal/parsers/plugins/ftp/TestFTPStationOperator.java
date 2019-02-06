package org.fogbowcloud.sebal.parsers.plugins.ftp;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.fogbowcloud.sebal.parsers.plugins.StationOperatorConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TestFTPStationOperator {
	
	@Test
	public void testFindNearestStationCorrectCalculation() throws URISyntaxException, HttpException, IOException, ParseException {
		
		Properties properties = mock(Properties.class);
		String year = "2002";
		int numberOfDays = 0;
		double lat = -3.40; // Maranhao - Moncao
		double lon = -45.20;
		
		String stringDate = "26-01-2002";		
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);
		
		JSONObject stationOne = new JSONObject();
		stationOne.put("id", "82294");
		stationOne.put("lon", "-44.3068");
		stationOne.put("altitude", "16.5");
		stationOne.put("name", "SAO LUIS - MA");
		stationOne.put("lat", "-2.53073");
		
		JSONObject stationTwo = new JSONObject();
		stationTwo.put("id", "83096");
		stationTwo.put("lon", "-46.20");
		stationTwo.put("altitude", "4.72");
		stationTwo.put("name", "ALTO TURIACU - MA");
		stationTwo.put("lat", "-3.00");
		
		JSONObject stationThree = new JSONObject();
		stationThree.put("id", "80000");
		stationThree.put("lon", "-34.861");
		stationThree.put("altitude", "16.5");
		stationThree.put("name", "JOAO PESSOA - PB");
		stationThree.put("lat", "-7.11532");
		
		JSONArray stations = new JSONArray();
		stations.put(stationOne);
		stations.put(stationTwo);
		stations.put(stationThree);
		
		List<JSONObject> expectedStation = new ArrayList<JSONObject>();
		expectedStation.add(stationTwo);
		expectedStation.add(stationOne);
		
		FTPStationOperator stationOperator = spy(new FTPStationOperator(properties));
		doReturn(stations).when(stationOperator).getStations(year);
		
		List<JSONObject> chosenStation = stationOperator.findNearestStation(date, lat, lon, numberOfDays);
		
		Assert.assertEquals(expectedStation, chosenStation);
	}
	
	@Test
	public void testFindNearestStationWithoutNearStations() throws URISyntaxException, HttpException, IOException, ParseException {
		
		Properties properties = mock(Properties.class);
		String year = "2002";
		int numberOfDays = 0;
		double lat = -3.40; // Maranhao - Moncao
		double lon = -45.20;
		
		String stringDate = "26-01-2002";		
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);
		
		JSONObject stationOne = new JSONObject();
		stationOne.put("id", "80000");
		stationOne.put("lon", "-34.861");
		stationOne.put("altitude", "16.5");
		stationOne.put("name", "JOAO PESSOA - PB");
		stationOne.put("lat", "-7.11532");
		
		JSONArray stations = new JSONArray();
		stations.put(stationOne);
		
		List<JSONObject> expectedStation = new ArrayList<JSONObject>();
		
		FTPStationOperator stationOperator = spy(new FTPStationOperator(properties));
		doReturn(stations).when(stationOperator).getStations(year);
		
		List<JSONObject> chosenStation = stationOperator.findNearestStation(date, lat, lon, numberOfDays);
		
		Assert.assertEquals(expectedStation, chosenStation);
	}
	
	@Test
	public void testReadStation() throws Exception {
		// set up
		String stringDate = "26-01-2002";		
		SimpleDateFormat f = new SimpleDateFormat("dd-MM-yyyy");
		Date d = f.parse(stringDate);
		long milliseconds = d.getTime();
		Date date = new Date(milliseconds);		
		
		// Establishing local fixed attributes
		String year = "2002";
		String compressedUnformattedStationFilePath = "/tmp/2002/827910-99999-2002.gz";
		String stationFileUrl = "fake-station-file-url";
		File compressedUnformattedStationFile = new File(compressedUnformattedStationFilePath);
		Properties properties = mock(Properties.class);

		// Creating temporary year directory
		String baseUnformattedLocalStationFilePath = "/tmp/2002";
		File baseUnformattedLocalStationDir = new File(baseUnformattedLocalStationFilePath);
		baseUnformattedLocalStationDir.mkdirs();
		
		// Copying station file to be used from resource to /tmp
		String originalCompressedFilePath = "src/test/resource/827910-99999-2002.gz";
		File originalCompressedFile = new File(originalCompressedFilePath);
		FileUtils.copyFile(originalCompressedFile, compressedUnformattedStationFile);
		
		FTPStationOperator stationOperator = spy(new FTPStationOperator(properties));
		doReturn(baseUnformattedLocalStationFilePath).when(stationOperator).getBaseUnformattedLocalStationFilePath(year);
		doReturn(compressedUnformattedStationFile).when(stationOperator).getUnformattedStationFile("82791", year);
		doReturn(stationFileUrl).when(stationOperator).getStationFileUrl("82791", year);
		doReturn(true).when(stationOperator).downloadUnformattedStationFile(compressedUnformattedStationFile, stationFileUrl);
		
		// exercise
		JSONArray stationData = stationOperator.readStation("82791",
				StationOperatorConstants.DATE_FORMAT.format(date),
				StationOperatorConstants.DATE_FORMAT.format(date));
		
		// expect
		Assert.assertNotNull(stationData);
	}
	
	@Test
	public void testGetStations() throws IOException {
		// set up
		String year = "2002";
		String fakeUrl = "fake-url";
		Properties properties = mock(Properties.class);
				
		String localStationCSVFilePath = "src/test/resource/2002-stations.csv";				
				
		PrintWriter writer = new PrintWriter(localStationCSVFilePath, "UTF-8");
		writer.println("827910;-7.10;-37.26");
		writer.close();
		
		FTPStationOperator stationOperator = spy(new FTPStationOperator(properties));
		doReturn(localStationCSVFilePath).when(stationOperator).getStationCSVFilePath(year);
		doReturn(fakeUrl).when(stationOperator).getStationCSVFileURL(year);
		doReturn(true).when(stationOperator).doDownloadStationCSVFile(localStationCSVFilePath, fakeUrl);
		
		// exercise
		JSONArray stations = stationOperator.getStations(year);	
		
		// expect
		Assert.assertNotNull(stations);
	}
}
