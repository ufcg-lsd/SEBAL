package org.fogbowcloud.sebal.model.satellite;

public interface Satellite {

	double LLambdaMin(int band);
	
	double LLambdaMax(int band);
	
	double ESUN(int band);

	double K1();
	
	double K2();
	
}
