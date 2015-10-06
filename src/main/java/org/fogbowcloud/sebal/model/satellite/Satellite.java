package org.fogbowcloud.sebal.model.satellite;

public interface Satellite {
	
	String landsatName();

	double LLambdaMin(int band);
	
	double LLambdaMax(int band);
	
	double ESUNsat5(int band);
	
	double ESUNsat7(int band);
	
	double ESUNsat8(int band);

	double K1();
	
	double K2();
	
}
