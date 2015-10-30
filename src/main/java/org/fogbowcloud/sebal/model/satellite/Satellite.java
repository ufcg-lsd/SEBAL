package org.fogbowcloud.sebal.model.satellite;

public interface Satellite {
	
	public final String LANDSAT_L5 = "landsat5";
	public final String LANDSAT_L7 = "landsat7";
	public final String LANDSAT_L8 = "landsat8";
	
	String landsatName();

	double LLambdaMin(int band);
	
	double LLambdaMax(int band);
	
	double ESUN(int band);

	double K1();
	
	double K2();
	
}
