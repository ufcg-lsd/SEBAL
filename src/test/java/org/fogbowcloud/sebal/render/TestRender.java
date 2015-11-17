package org.fogbowcloud.sebal.render;

import org.fogbowcloud.sebal.render.RenderHelper;
import org.junit.Assert;
import org.junit.Test;

public class TestRender {

	@Test
	public void calculationPixelSizeTest() throws Exception {
		RenderHelper r = new RenderHelper();
		
		double ulLat = -1.97265;
		double urLat = -1.96951;
		double llLat = -3.85842;
		double ulLon = -61.63424;
		double urLon = -59.50249;
		double llLon = -61.63196;
		double lines = 6951.0;
		double columns = 7911.0;
		
		r.calculatePixelSize(ulLon, ulLat, urLon, urLat, llLon, llLat, columns, lines);

		Assert.assertEquals(new Double(2.702704E-4), r.PIXEL_SIZE_X, 0.00001);
		Assert.assertEquals(new Double(2.718243E-4), r.PIXEL_SIZE_Y, 0.00001);
	}

}