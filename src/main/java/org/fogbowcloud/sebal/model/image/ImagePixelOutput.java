package org.fogbowcloud.sebal.model.image;

public class ImagePixelOutput {

	private double NDVI;
	private double Ts;
	private double sAVI;
	private double Rn;
	private double G;

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

	public double SAVI() {
		return sAVI;
	}
	
	public void setSAVI(double SAVI) {
		sAVI = SAVI;
	}

	public void setRn(double rn) {
		this.Rn = rn;
	}

	public void setG(double g) {
		this.G = g;
	}
	
	public double Rn() {
		return Rn;
	}
	
	public double G() {
		return G;
	}
}
