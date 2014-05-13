package org.fogbowcloud.sebal.parsers;

import java.io.File;
import java.util.Scanner;

public class EarthSunDistance {

	double[] allDistances = new double[366];
	
	public EarthSunDistance() throws Exception {
		Scanner scn = new Scanner(new File("earth-sun-distance"));
		while (scn.hasNext()) {
			int day = scn.nextInt();
			double distance = scn.nextDouble();
			allDistances[day - 1] = distance;
		}
		scn.close();
	}
	
	public double get(int day) {
		return allDistances[day - 1];
	}
	
}
