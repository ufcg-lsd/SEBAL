package org.fogbowcloud.sebal.model.image;

public class ImagePixelOutput {

	private double NDVI;
	private double Ts;
	private double sAVI;
	private double Rn;
	private double G;
	private double H;
	private double lambdaE;
	private double alpha;
	private double epsilonNB;
	private double epsilonZero;
	private double z0mxy;
	private double[] rho;

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

	public double getLambdaE() {
		return lambdaE;
	}

	public void setLambdaE(double lambdaE) {
		this.lambdaE = lambdaE;
	}

	public double getH() {
		return H;
	}

	public void setH(double h) {
		H = h;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public double getEpsilonNB() {
		return epsilonNB;
	}

	public void setEpsilonNB(double epsilonNB) {
		this.epsilonNB = epsilonNB;
	}

	public double getEpsilonZero() {
		return epsilonZero;
	}

	public void setEpsilonZero(double epsilonZero) {
		this.epsilonZero = epsilonZero;
	}

	public double getZ0mxy() {
		return z0mxy;
	}

	public void setZ0mxy(double z0mxy) {
		this.z0mxy = z0mxy;
	}

	public double[] getRho() {
		return rho;
	}

	public void setRho(double[] rho) {
		this.rho = rho;
	}
}
