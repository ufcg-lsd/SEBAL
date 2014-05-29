package org.fogbowcloud.sebal.model.image;

public class DefaultImagePixel implements ImagePixel {

	private double[] L;
	private double cosTheta;
	private Image image;
	private double z;
	private double Ta;
	private double ux;
	private double zx;
	private double d;
	private ImagePixelOutput output;
	private double hc;
	private GeoLoc geoLoc;
	
	public void L(double[] L) {
		this.L = L;
	}
	
	@Override
	public double[] L() {
		return L;
	}

	@Override
	public double cosTheta() {
		return cosTheta;
	}

	@Override
	public double z() {
		return z;
	}

	@Override
	public double Ta() {
		return Ta;
	}

	public void Ta(double Ta) {
		this.Ta = Ta;
	}
	
	@Override
	public double ux() {
		return ux;
	}
	
	public void ux(double ux) {
		this.ux = ux;
	}

	@Override
	public double zx() {
		return zx;
	}

	public void zx(double zx) {
		this.zx = zx;
	}
	
	public void cosTheta(double cosTheta) {
		this.cosTheta = cosTheta;
	}

	@Override
	public Image image() {
		return image;
	}
	
	public void image(Image image) {
		this.image = image;
	}

	public void z(double z) {
		this.z = z;
	}

	@Override
	public double d() {
		return d;
	}
	
	public void d(double d) {
		this.d = d;
	}

	@Override
	public ImagePixelOutput output() {
		return output;
	}

	@Override
	public void setOutput(ImagePixelOutput output) {
		this.output = output;
	}

	@Override
	public double hc() {
		return hc;
	}
	
	public void hc(double hc) {
		this.hc = hc;
	}

	@Override
	public GeoLoc geoLoc() {
		return this.geoLoc;
	}

	public void geoLoc(GeoLoc geoLoc) {
		this.geoLoc = geoLoc;
	}

}
