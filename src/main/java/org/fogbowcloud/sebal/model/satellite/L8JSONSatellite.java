package org.fogbowcloud.sebal.model.satellite;

import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class L8JSONSatellite extends JSONSatellite {

	public L8JSONSatellite(String jsonPath) throws Exception {
		this.json = new JSONObject(IOUtils.toString(new FileInputStream(jsonPath)));
		this.landsatName = jsonPath;
	}

	@Override
	public double ESUN(int band) {
		return json.optJSONArray("band" + band).optDouble(11);
	}

}
