package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.parsers.Elevation;
import org.junit.Assert;
import org.junit.Test;


public class ElevationTest {

	/**
	 * http://api.geonames.org/srtm3?lat=-6.38&lng=-37.2&username=demo
	 * @throws Exception
	 */
	@Test
	public void testNegativeLatLon() throws Exception {
		Assert.assertEquals(142, new Elevation().z(-6.38, -37.2).intValue());
	}
	
	/**
	 * http://api.geonames.org/srtm3?lat=1.38&lng=-58.2&username=demo
	 * @throws Exception
	 */
	@Test
	public void testPositiveLatLon() throws Exception {
		Assert.assertEquals(382, new Elevation().z(1.38, -58.2).intValue());
	}
	
	/**
	 * http://api.geonames.org/srtm3?lat=-6&lng=-37&username=demo
	 * @throws Exception
	 */
	@Test
	public void testRoundedLatLon() throws Exception {
		Assert.assertEquals(53, new Elevation().z(-6., -37.).intValue());
	}
	
}
