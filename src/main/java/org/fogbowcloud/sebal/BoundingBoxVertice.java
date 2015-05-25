package org.fogbowcloud.sebal;

public class BoundingBoxVertice {
	
	private double lat;
	private double lon;

	public BoundingBoxVertice(double lat, double lon) {
		this.lat = lat;
		this.lon = lon;
	}

	public double getLat() {
		return lat;
	}

	public double getLon() {
		return lon;
	}
}
