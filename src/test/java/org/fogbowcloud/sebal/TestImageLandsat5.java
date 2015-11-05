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
import org.fogbowcloud.sebal.model.satellite.L5JSONSatellite;
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
		testDataFilePath = "src/test/resource/sebal-l5-test-data.csv";
		satellite = null;
	}

	@Test
	public void acceptanceTest() throws Exception {
		
		// Initializing image variables
        Locale.setDefault(Locale.ROOT);
        
        satellite = new L5JSONSatellite(Satellite.LANDSAT_L5);
        
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
        List<ImagePixel> expectedPixels = TestImageHelper.readExpectedPixelsFromFile(testDataFilePath,
        		satellite);
        
        Double sunElevation = 49.00392091;
        double cosTheta = 0.715517;
        Date accquiredDate = Date.valueOf("2001-05-15");
        
		TestImageHelper.setProperties(pixelQuenteFrioChooser,
				satellite, station, expectedPixels, sunElevation,
				accquiredDate, cosTheta);
		
		List<ImagePixel> processedPixels = F1(this.pixelQuenteFrioChooser, satellite, station, sunElevation, 
				accquiredDate, cosTheta);
		
		for (int i = 0; i < processedPixels.size(); i++) {		
			GeoLoc expectedGeoLoc = expectedPixels.get(i).geoLoc();
			ImagePixelOutput expectedOutput = expectedPixels.get(i).output();

			GeoLoc obtainedGeoLoc = processedPixels.get(i).geoLoc();
			ImagePixelOutput obtainedOutput = processedPixels.get(i).output();

			assertEquals(obtainedGeoLoc.getI(), expectedGeoLoc.getI());
			assertEquals(obtainedGeoLoc.getJ(), expectedGeoLoc.getJ());
			
			double[] expectedL = expectedPixels.get(i).L();
			double[] obtainedL = processedPixels.get(i).L();			
			
			for(int j = 0; j < expectedPixels.get(i).L().length; j++) {
				assertField(expectedL[j], obtainedL[j]);
			}					
			
			double[] expectedRho = expectedOutput.getRho();
			double[] obtainedRho = obtainedOutput.getRho();
			
			for(int j = 0; j < expectedRho.length; j++) {
				assertField(expectedRho[j], obtainedRho[j]);
			}
						
			assertField(expectedOutput.getAlphaToa(), obtainedOutput.getAlphaToa());						
			assertField(expectedOutput.getTauSW(), obtainedOutput.getTauSW());								
			assertField(expectedOutput.getAlpha(), obtainedOutput.getAlpha());					
			assertField(expectedOutput.getRSDown(), obtainedOutput.getRSDown());					
			assertField(expectedOutput.getNDVI(), obtainedOutput.getNDVI());	
			assertField(expectedOutput.SAVI(), obtainedOutput.SAVI());
			assertField(expectedOutput.getIAF(), obtainedOutput.getIAF());
			assertField(expectedOutput.getEpsilonNB(), obtainedOutput.getEpsilonNB());	
			assertField(expectedOutput.getEpsilonZero(), obtainedOutput.getEpsilonZero());		
			assertField(expectedOutput.getTs(), obtainedOutput.getTs());
			assertField(expectedOutput.getRLUp(), obtainedOutput.getRLUp());
			assertField(expectedOutput.getEpsilonA(), obtainedOutput.getEpsilonA());
			assertField(expectedOutput.getRLDown(), obtainedOutput.getRLDown());
			assertField(expectedOutput.Rn(), obtainedOutput.Rn());
		}
	}
	
	public List<ImagePixel> F1(PixelQuenteFrioChooser pixelQuenteFrioChooser, Satellite satellite, 
			WeatherStation station, double sunElevation, Date accquiredDate, double cosTheta) throws Exception {
		LOGGER.debug("Executing F1 phase...");

		List<ImagePixel> inputPixels = TestImageHelper.readInputPixelsFromFile(testDataFilePath,
				satellite);
	    DefaultImage inputImage = TestImageHelper.setProperties(pixelQuenteFrioChooser,
					satellite, station, inputPixels, sunElevation, accquiredDate, cosTheta);
				
		Image processedImage = new SEBAL().processPixelQuentePixelFrio(inputImage,
                satellite, boundingBoxVertices, 0, 0, false);
		
		List<ImagePixel> processedPixels = processedImage.pixels();
        return processedPixels;
	}

	private void assertField(double expectedValue, double obtainedValue) {
		assertEquals(expectedValue, obtainedValue, Math.abs(expectedValue * 0.05));
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

}