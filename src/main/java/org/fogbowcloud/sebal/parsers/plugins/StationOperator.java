package org.fogbowcloud.sebal.parsers.plugins;

import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public interface StationOperator {

	public JSONArray getStations(String year);

	public JSONArray readStationCSVFile(String localStationsCSVFilePath);

	public List<JSONObject> findNearestStation(Date date, double lat, double lon, int numberOfDays);

	public JSONArray readStation(String stationId, String beginDate, String endDate)
			throws Exception;
}
