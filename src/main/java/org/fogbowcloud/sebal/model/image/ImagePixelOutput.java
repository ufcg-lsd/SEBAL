package org.fogbowcloud.sebal.model.image;

public class ImagePixelOutput {

	private double NDVI;
	private double Ts;

	public double getNDVI() {
		return NDVI;
	}

	public void setNDVI(double nDVI) {
		NDVI = nDVI;
	}
	
	public void setTs(double ts) {
		Ts = ts;
	}
	
	public double getTs() {
		return Ts;
	}
}
