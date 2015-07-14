package org.fogbowcloud.sebal;

public class LatLonCoordinate {
	
	private double latitude;
	private double longitude;

	public LatLonCoordinate(double latitude, double longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}

	public double getLat() {
		return this.latitude;
	}

	public double getLon() {
		return this.longitude;
	}

}
