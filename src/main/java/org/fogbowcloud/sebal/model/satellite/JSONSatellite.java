package org.fogbowcloud.sebal.model.satellite;

import org.json.JSONObject;

public abstract class JSONSatellite implements Satellite {

	protected JSONObject json;
	protected String landsatName;
	
	@Override
	public String landsatName() {
		return this.landsatName;
	}
	
	@Override
	public double LLambdaMin(int band) {
		return json.optJSONArray("band" + band).optDouble(3);
	}

	@Override
	public double LLambdaMax(int band) {
		return json.optJSONArray("band" + band).optDouble(4);
	}

	@Override
	public abstract double ESUN(int band);

	@Override
	public double K1() {
		return json.optDouble("k1");
	}

	@Override
	public double K2() {
		return json.optDouble("k2");
	}
}
