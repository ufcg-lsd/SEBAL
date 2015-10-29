package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.junit.Test;
import org.mockito.Mockito;

public class TestImageLandsat5 {
	
	private PixelQuenteFrioChooser pixelQuenteFrioChooser;
	private static final Logger LOGGER = Logger.getLogger(TestImageLandsat5.class);
	private List<BoundingBoxVertice> boundingBoxVertices;
	private String testDataFilePath;
	private Satellite satellite;
	private Properties properties;
	
	public TestImageLandsat5() throws IOException {
		this.properties = new Properties();
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(this.properties);
		boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		testDataFilePath = "/local/esdras/git/SEBAL/src/test/resource/sebal-l5-test-data.csv";
		satellite = null;
	}

	@Test
	public void acceptanceTest() throws Exception {
		
		// Initializing image variables
        Locale.setDefault(Locale.ROOT);
        
        // Mocking WeatherStation class
        // Change the return values
        WeatherStation station = Mockito.mock(WeatherStation.class);
        Mockito.when(station.Ta(Mockito.anyDouble(), Mockito.anyDouble(),
        		Mockito.any(Date.class))).thenReturn(32.233);
        Mockito.when(station.zx(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(6.0);
        Mockito.when(station.ux(Mockito.anyDouble(), Mockito.anyDouble(),
        		Mockito.any(Date.class))).thenReturn(4.388);
        Mockito.when(station.d(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(4.0 * 2/3);
        Mockito.when(station.hc(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(4.0);
        
        // Reading and storing data from .csv file to pixels in a image
        List<ImagePixel> expectedPixels = TestImageHelper.readExpectedPixelsFromFile(testDataFilePath);
        
        // Sun Elevation for Landsat 5
        //
        // Modify this to support multiple satellite types
        Double sunElevation = 49.00392091;
        double cosTheta = 0.715517;
        // Sun Elevation for Landsat 7
        //
        //Double sunElevation = 53.52375;
        Date accquiredDate = Date.valueOf("2001-05-15");
        int day = 15;
        
		TestImageHelper.setProperties(true, pixelQuenteFrioChooser,
				satellite, station, expectedPixels, sunElevation,
				accquiredDate, day, cosTheta);
		
		satellite = new JSONSatellite("landsat5");
		
		List<ImagePixel> processedPixels = F1(this.pixelQuenteFrioChooser, satellite, station, sunElevation, 
				accquiredDate, day, cosTheta);
		
		// See if the arguments on 'for' are correct
		for (int i = 0; i < processedPixels.size(); i++) {
			//System.out.println(i);			
			GeoLoc expectedGeoLoc = expectedPixels.get(i).geoLoc();
			ImagePixelOutput expectedOutput = expectedPixels.get(i).output();

			// Verify how to fix this
			GeoLoc obtainedGeoLoc = processedPixels.get(i).geoLoc();
			ImagePixelOutput obtainedOutput = processedPixels.get(i).output();

			assertEquals(obtainedGeoLoc.getI(), expectedGeoLoc.getI());
			assertEquals(obtainedGeoLoc.getJ(), expectedGeoLoc.getJ());
			
			double[] expectedL = expectedPixels.get(i).L();
			double[] obtainedL = processedPixels.get(i).L();
			
			System.out.println("");
			
			for(int j = 0; j < expectedPixels.get(i).L().length; j++) {
				System.out.println(expectedL[j] + " - " + obtainedL[j]);
				assertField(expectedL[j], obtainedL[j]);
			}
			
			System.out.println("");
			
			double[] expectedRho = expectedOutput.getRho();
			double[] obtainedRho = obtainedOutput.getRho();
			
			for(int j = 0; j < expectedRho.length; j++) {
				//assertField(expectedRho[j], obtainedRho[j]);
				System.out.println(expectedRho[j] + " - " + obtainedRho[j]);
			}
			
			System.out.println("\n####AlphaToa####");
			assertField(expectedOutput.getAlphaToa(), obtainedOutput.getAlphaToa());
			System.out.println(expectedOutput.getAlphaToa() + " - " + obtainedOutput.getAlphaToa());
			
			System.out.println("\n####TauSW####");
			assertField(expectedOutput.getTauSW(), obtainedOutput.getTauSW());
			System.out.println(expectedOutput.getTauSW() + " - " + obtainedOutput.getTauSW());
			
			System.out.println("\n####Alpha####");
			assertField(expectedOutput.getAlpha(), obtainedOutput.getAlpha());
			System.out.println(expectedOutput.getAlpha() + " - " + obtainedOutput.getAlpha());

			System.out.println("\n####RSDown####");
			assertField(expectedOutput.getRSDown(), obtainedOutput.getRSDown());
			System.out.println(expectedOutput.getRSDown() + " - " + obtainedOutput.getRSDown());
			
			System.out.println("\n####NDVI####");
			assertField(expectedOutput.getNDVI(), obtainedOutput.getNDVI());
			System.out.println(expectedOutput.getNDVI() + " - " + obtainedOutput.getNDVI());

			System.out.println("\n####SAVI####");
			assertField(expectedOutput.SAVI(), obtainedOutput.SAVI());
			System.out.println(expectedOutput.SAVI() + " - " + obtainedOutput.SAVI());

			System.out.println("\n####LAI####");
			assertField(expectedOutput.getIAF(), obtainedOutput.getIAF());
			System.out.println(expectedOutput.getIAF() + " - " + obtainedOutput.getIAF());
			
			System.out.println("\n####EpsilonNB####");
			assertField(expectedOutput.getEpsilonNB(), obtainedOutput.getEpsilonNB());
			System.out.println(expectedOutput.getEpsilonNB() + " - " + obtainedOutput.getEpsilonNB());

			System.out.println("\n####EpsilonZero####");
			assertField(expectedOutput.getEpsilonZero(), obtainedOutput.getEpsilonZero());
			System.out.println(expectedOutput.getEpsilonZero() + " - " + obtainedOutput.getEpsilonZero());

			System.out.println("\n####Ts####");
			assertField(expectedOutput.getTs(), obtainedOutput.getTs());
			System.out.println(expectedOutput.getTs() + " - " + obtainedOutput.getTs());
			
			System.out.println("\n####RLUp####");
			assertField(expectedOutput.getRLUp(), obtainedOutput.getRLUp());
			System.out.println(expectedOutput.getRLUp() + " - " + obtainedOutput.getRLUp());

			System.out.println("\n####EpsilonA####");
			assertField(expectedOutput.getEpsilonA(), obtainedOutput.getEpsilonA());
			System.out.println(expectedOutput.getEpsilonA() + " - " + obtainedOutput.getEpsilonA());

			System.out.println("\n####RLDown####");
			assertField(expectedOutput.getRLDown(), obtainedOutput.getRLDown());
			System.out.println(expectedOutput.getRLDown() + " - " + obtainedOutput.getRLDown());

			System.out.println("\n####Rn####");
			assertField(expectedOutput.Rn(), obtainedOutput.Rn());
			System.out.println(expectedOutput.Rn() + " - " + obtainedOutput.Rn());
		}
	}
	
	public List<ImagePixel> F1(PixelQuenteFrioChooser pixelQuenteFrioChooser, Satellite satellite, WeatherStation station,
			double sunElevation, Date accquiredDate, int day, double cosTheta) throws Exception {
		
		LOGGER.info("Executing F1 phase...");
		long now = System.currentTimeMillis();
	
		List<ImagePixel> inputPixels = TestImageHelper.readInputPixelsFromFile(testDataFilePath);
	    DefaultImage inputImage = TestImageHelper.setProperties(false, pixelQuenteFrioChooser,
					satellite, station, inputPixels, sunElevation, accquiredDate, day, cosTheta);
				
		Image processedImage = new SEBAL().processPixelQuentePixelFrio(inputImage,
                satellite, boundingBoxVertices, 0, 0, false);
		
		List<ImagePixel> processedPixels = processedImage.pixels();

        LOGGER.info("F1 phase execution time is " + (System.currentTimeMillis() - now));
		
        return processedPixels;
	}

	private void assertField(double expectedValue, double obtainedValue) {
		assertEquals(expectedValue, obtainedValue, Math.abs(expectedValue * 0.05));
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

}