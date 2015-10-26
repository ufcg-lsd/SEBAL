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
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.fogbowcloud.sebal.wrapper.Wrapper;
import org.junit.Test;
import org.mockito.Mockito;

public class TestImageLandsat5 {
	
	private PixelQuenteFrioChooser pixelQuenteFrioChooser;
	private static final Logger LOGGER = Logger.getLogger(TestImageLandsat5.class);
	private List<BoundingBoxVertice> boundingBoxVertices;
	private TestImageHelper imageHelper;
	private String testDataFilePath;
	private Satellite satellite;
	private Properties properties;
	
	public TestImageLandsat5() throws IOException {
		this.properties = new Properties();
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(this.properties);
		imageHelper = new TestImageHelper();
		boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		testDataFilePath = "sebal-l5-test-data.csv";
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
        
        List<ImagePixel> expectedPixels;
        // Reading and storing data from .csv file to pixels in a image
//        if(valueFlag.equals("obtainedValues")) {
//        	pixels = processPixelsFromObtainedFile(dataFilePath);
//        } else
        expectedPixels = TestImageHelper.readExpectedPixelsFromFile(testDataFilePath);
        
        // Sun Elevation for Landsat 5
        //
        // Modify this to support multiple satellite types
        Double sunElevation = 49.00392091;
        // Sun Elevation for Landsat 7
        //
        //Double sunElevation = 53.52375;
        Date accquiredDate = Date.valueOf("2001-05-15");
        int day = 15;
        
		TestImageHelper.setInitialProperties(true, pixelQuenteFrioChooser,
				satellite, station, expectedPixels, sunElevation,
				accquiredDate, day);
	
		/*Image expectedImage = TestImageHelper.readExpectedPixelsFromCSV(testDataFilePath, this.pixelQuenteFrioChooser, 
				true, satellite);*/
		
/*		List<ImagePixel> expectedPixels = expectedImage.pixels();*/
		
		Image processedImage = F1(this.pixelQuenteFrioChooser, satellite, station, expectedPixels, sunElevation, 
				accquiredDate, day);
		
		List<ImagePixel> obtainedPixels = processedImage.pixels();
		
		// List<ImagePixel> obtainedValues = processPixelsFromFile(obtainedValuesFile);
		//DefaultImagePixel obtainedImagePixel = new DefaultImagePixel();
		// DefaultImagePixel obtainedImagePixel = processPixelsFromFile(obtainedValuesFile);
		// Modify processPixelsFromFile() to return a image instead of a list, this way is possible to use the code
		// beneath here
		
		// See if the arguments on 'for' are correct
		for (int i = 0; i < obtainedPixels.size(); i++) {
			//System.out.println(i);			
			GeoLoc desiredGeoLoc = expectedPixels.get(i).geoLoc();
			ImagePixelOutput desiredOutput = expectedPixels.get(i).output();

			// Verify how to fix this
			GeoLoc obtainedGeoLoc = obtainedPixels.get(i).geoLoc();
			ImagePixelOutput obtainedOutput = obtainedPixels.get(i).output();

			assertEquals(obtainedGeoLoc.getI(), desiredGeoLoc.getI());
			assertEquals(obtainedGeoLoc.getJ(), desiredGeoLoc.getJ());
			
			double[] desiredL = expectedPixels.get(i).L();
			double[] obtainedL = obtainedPixels.get(i).L();
			
			for(int j = 0; j < expectedPixels.get(i).L().length; j++) {
				System.out.println(desiredL[j] + " - " + obtainedL[j]);
				assertField(desiredL[j], obtainedL[j]);
			}
			
			double[] desiredRho = desiredOutput.getRho();
			double[] obtainedRho = obtainedOutput.getRho();
			
			for(int j = 0; j < desiredRho.length; j++) {
				assertField(desiredRho[j], obtainedRho[j]);
				System.out.println(desiredRho[j] + " - " + obtainedRho[j]);
			}
			
			assertField(desiredOutput.G(), obtainedOutput.G());
			assertField(desiredOutput.Rn(), obtainedOutput.Rn());
			assertField(desiredOutput.getLambdaE(), obtainedOutput.getLambdaE());
			assertField(desiredOutput.getTs(), obtainedOutput.getTs());
			assertField(desiredOutput.getNDVI(), obtainedOutput.getNDVI());
			assertField(desiredOutput.SAVI(), obtainedOutput.SAVI());
			assertField(desiredOutput.getAlpha(), obtainedOutput.getAlpha());
			assertField(desiredOutput.getZ0mxy(), obtainedOutput.getZ0mxy());
			assertField(desiredOutput.getEpsilonZero(), obtainedOutput.getEpsilonZero());
			assertField(desiredOutput.getEpsilonNB(), obtainedOutput.getEpsilonNB());
			assertField(desiredOutput.getRLDown(), obtainedOutput.getRLDown());
			assertField(desiredOutput.getEpsilonA(), obtainedOutput.getEpsilonA());
			assertField(desiredOutput.getRLUp(), obtainedOutput.getRLUp());
			assertField(desiredOutput.getIAF(), obtainedOutput.getIAF());
			assertField(desiredOutput.getEVI(), obtainedOutput.getEVI());
			assertField(desiredOutput.getRSDown(), obtainedOutput.getRSDown());
			assertField(desiredOutput.getTauSW(), obtainedOutput.getTauSW());
			assertField(desiredOutput.getAlphaToa(), obtainedOutput.getAlphaToa());
			// Verify how to fix this
			/*HOutput desiredHOutIncial = desiredOutput.gethOuts().get(0);
			HOutput obtainedHOutIncial = obtainedOutput.gethOuts().get(0);
			
			assertField(desiredHOutIncial.getH(), obtainedHOutIncial.getH());
			assertField(desiredHOutIncial.getA(), obtainedHOutIncial.getA());
			assertField(desiredHOutIncial.getB(), obtainedHOutIncial.getB());
			assertField(desiredHOutIncial.getRah(), obtainedHOutIncial.getRah());
			assertField(desiredHOutIncial.getuAsterisk(), obtainedHOutIncial.getuAsterisk());
			assertField(desiredHOutIncial.getL(), obtainedHOutIncial.getL());

			HOutput desiredHOutFinal = desiredOutput.gethOuts().get(1);
			HOutput obtainedHOutFinal = obtainedOutput.gethOuts().get(1);
			
			assertField(desiredHOutFinal.getH(), obtainedHOutFinal.getH());
			assertField(desiredHOutFinal.getA(), obtainedHOutFinal.getA());
			assertField(desiredHOutFinal.getB(), obtainedHOutFinal.getB());
			assertField(desiredHOutFinal.getRah(), obtainedHOutFinal.getRah());
			assertField(desiredHOutFinal.getuAsterisk(), obtainedHOutFinal.getuAsterisk());
			assertField(desiredHOutFinal.getL(), obtainedHOutFinal.getL());

			assertField(desiredOutput.G(), obtainedOutput.G());
			assertField(desiredOutput.Rn(), obtainedOutput.Rn());
			assertField(desiredOutput.getLambdaE(), obtainedOutput.getLambdaE());
			assertField(desiredOutput.getTs(), obtainedOutput.getTs());
			assertField(desiredOutput.getNDVI(), obtainedOutput.getNDVI());
			assertField(desiredOutput.SAVI(), obtainedOutput.SAVI());
			assertField(desiredOutput.getAlpha(), obtainedOutput.getAlpha());
			assertField(desiredOutput.getZ0mxy(), obtainedOutput.getZ0mxy());
			assertField(desiredOutput.getEpsilonZero(), obtainedOutput.getEpsilonZero());
			assertField(desiredOutput.getEpsilonNB(), obtainedOutput.getEpsilonNB());
			assertField(desiredOutput.getRLDown(), obtainedOutput.getRLDown());
			assertField(desiredOutput.getEpsilonA(), obtainedOutput.getEpsilonA());
			assertField(desiredOutput.getRLUp(), obtainedOutput.getRLUp());
			assertField(desiredOutput.getIAF(), obtainedOutput.getIAF());
			assertField(desiredOutput.getEVI(), obtainedOutput.getEVI());
			assertField(desiredOutput.getRSDown(), obtainedOutput.getRSDown());
			assertField(desiredOutput.getTauSW(), obtainedOutput.getTauSW());
			assertField(desiredOutput.getAlphaToa(), obtainedOutput.getAlphaToa());*/
		}
	}
	
	public Image F1(PixelQuenteFrioChooser pixelQuenteFrioChooser, Satellite satellite, WeatherStation station,
			List<ImagePixel> pixels, double sunElevation, Date accquiredDate, int day) throws Exception {
		
		System.out.println("Executing F1 phase...");
		//LOGGER.info("Executing F1 phase...");
		long now = System.currentTimeMillis();
		
		satellite = new JSONSatellite("landsat5");
		
		String obtainedFlag = "obtainedValues";
		// See if processPixelsFromFile can be used instead
		List<ImagePixel> inputPixels = imageHelper.processPixelsFromObtainedFile(testDataFilePath);
	    DefaultImage inputImage = TestImageHelper.setInitialProperties(false, pixelQuenteFrioChooser,
					satellite, station, inputPixels, sunElevation, accquiredDate, day);
	    inputImage.width(0);
	    inputImage.height(0);
		
				
		boolean cloudDetection = false;
		
		
		Image processedImage = new SEBAL().processPixelQuentePixelFrio(inputImage,
                satellite, boundingBoxVertices, inputImage.width(), inputImage.height(), cloudDetection);
		
		/*saveProcessOutput(updatedImage);
        savePixelQuente(updatedImage, getPixelQuenteFileName());
        savePixelFrio(updatedImage, getPixelFrioFileName());*/
        LOGGER.info("F1 phase execution time is " + (System.currentTimeMillis() - now));
		
        return processedImage;
		
	}

	private void assertField(double expectedValue, double obtainedValue) {
		assertEquals(expectedValue, obtainedValue, Math.abs(expectedValue * 0.05));
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

}