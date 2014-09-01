package org.fogbowcloud.sebal;

import java.text.DecimalFormat;
import java.util.Locale;

import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HTest {

	private ImagePixel pixelQuente;
	private ImagePixel pixelFrio;
	private SEBAL sebal;
	private DecimalFormat elevenDForm;
	private DecimalFormat twoDForm;
	private ImagePixelOutput pixelQuenteOutput;
	private ImagePixelOutput pixelFrioOutput;
	private DecimalFormat fiveDForm;
	private DecimalFormat sixDForm;
	private double uAsterisk;
	private double z0m;
	private double uAsteriskxy;
	private double u200;
	private double d0;
	private double z0mxy;
	private double Hcal;
	private double rahxy;
//	
//	@Before
//	public void setUp() throws Exception {
//		DefaultImage defaultImage = new DefaultImage();
//		defaultImage.choosePixelsQuenteFrio();
//		pixelQuente = defaultImage.pixelQuente();
//		pixelQuenteOutput = pixelQuente.output();
//		pixelFrio = defaultImage.pixelFrio();
//		pixelFrioOutput = pixelFrio.output();
//		sebal = new SEBAL();
//		
//		uAsterisk = 0.;
//		z0m = 0.;
//		uAsteriskxy = 0.;
//		u200 = 0.;
//		z0mxy = 0.;
//		Hcal = 0.;
//		d0 = pixelQuente.d();
//		rahxy = 0.;
//		
//		Locale.setDefault(Locale.ROOT);
//		
//		twoDForm = new DecimalFormat(".##");
//		fiveDForm = new DecimalFormat(".#####");
//		sixDForm = new DecimalFormat(".######");
//		elevenDForm = new DecimalFormat(".###########");
//	}
//	
//	@Test
//	public void testZ0m() {
//		z0m = sebal.z0m(pixelQuente.hc());
//		Assert.assertEquals(0.48,z0m, 0.);
//	}
//	
//	@Test
//	public void testUAsterisk() {
//		testZ0m();
//		uAsterisk = sebal.uAsterisk(pixelQuente.ux(), 
//				pixelQuente.zx(), pixelQuente.d(), z0m);
//	    Double uAsteriskRound = Double.valueOf(twoDForm.format(uAsterisk));
//		Assert.assertEquals(0.93,uAsteriskRound, 0.);
//	}
//
//	@Test
//	public void testU200() {
//		testUAsterisk();
//		u200 = sebal.u200(uAsterisk, pixelQuente.d(), z0m);
//	    Double u200Round= Double.valueOf(elevenDForm.format(u200));
//		Assert.assertEquals(13.62825792109, u200Round, 0.);
//	}
//	
//	@Test
//	public void testHcal() {
//		testU200();
//		
//		Hcal = sebal.HQuente(pixelQuenteOutput.Rn(), pixelQuenteOutput.G());
//		Double HcalRound = Double.valueOf(elevenDForm.format(Hcal));
//		Assert.assertEquals(378.27664, HcalRound, 0.);
//	}
//	
//	@Test
//	public void testZ0mxy() {
//		testHcal();
//		
//		z0mxy = sebal.z0mxy(pixelQuenteOutput.SAVI());
//		Double z0mxyRound = Double.valueOf(sixDForm.format(z0mxy));
//		Assert.assertEquals(0.007482, z0mxyRound, 0.);
//	}
//	
//	@Test
//	public void testUAsteriskxy() {
//		testZ0mxy();
//		
//		uAsteriskxy = sebal.uAsteriskxy(u200, d0, z0mxy);
//		Double uAsteriskxyRound = Double.valueOf(twoDForm.format(uAsteriskxy));
//		Assert.assertEquals(0.55, uAsteriskxyRound, 0.);
//	}
//	
//	@Test
//	public void testRahxy() {
//		testUAsteriskxy();
//		
//		
//		rahxy = sebal.rahxy(uAsteriskxy);
//		Double rahxyRound = Double.valueOf(twoDForm.format(rahxy));
//		
//		Assert.assertEquals(13.31, rahxyRound, 0.);
//	}
}
