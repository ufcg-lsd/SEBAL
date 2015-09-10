package org.fogbowcloud.sebal;

public class UTMCoordinate {

	private int easting;
	private int northing;

	public UTMCoordinate(int easting, int northing) {
		this.easting = easting;
		this.northing = northing;
	}

	public int getEasting() {
		return easting;
	}

	public int getNorthing() {
		return northing;
	}
	
	public String toString() {
		return "(easting=" + easting + ", northing=" + northing + ")";
	}
}
