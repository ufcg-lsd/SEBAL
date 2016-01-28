package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.junit.Test;

public class TestPreProcessImage {
	
	private String mtlFile;
    private int iBegin;
    private int iFinal;
    private int jBegin;
    private int jFinal;
    private PixelQuenteFrioChooser pixelQuenteFrioChooser;
    private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
    private String fmaskFilePath;
    
	private static final Logger LOGGER = Logger.getLogger(TestPreProcessImage.class);
	
	public void setProperties(Properties properties) throws IOException {
		String mtlFilePath = properties.getProperty("mtl_file_path");
		if (mtlFilePath == null || mtlFilePath.isEmpty()) {
			LOGGER.error("Property mtl_file_path must be set.");
			throw new IllegalArgumentException("Property mtl_file_path must be set.");
		}
		this.mtlFile = mtlFilePath;
		
		String iBeginStr = properties.getProperty("i_begin_interval");
		String iFinalStr = properties.getProperty("i_final_interval");
		String jBeginStr = properties.getProperty("j_begin_interval");
		String jFinalStr = properties.getProperty("j_final_interval");
		
		if (iBeginStr == null || iFinalStr == null || jBeginStr == null || jFinalStr == null) {
			LOGGER.error("Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
			throw new IllegalArgumentException(
					"Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
		}
		this.iBegin = Integer.parseInt(iBeginStr);
		this.iFinal = Integer.parseInt(iFinalStr);
		this.jBegin = Integer.parseInt(jBeginStr);
		this.jFinal = Integer.parseInt(jFinalStr);
		
		LOGGER.debug("i interval: (" + iBegin + ", " + iFinal + ")");
		LOGGER.debug("j interval: (" + jBegin + ", " + jFinal + ")");
		
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(properties.getProperty("bounding_box_file_path"));

		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
		
		fmaskFilePath = properties.getProperty("fmask_file_path");
	}
	
	@Test
	public void acceptanceTest() throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);
				
		setProperties(properties);
		
		LOGGER.info("Pre processing pixels...");
    	
        Product product = SEBALHelper.readProduct(mtlFile, boundingBoxVertices);
        
        BoundingBox boundingBox = null;
        if (boundingBoxVertices.size() > 3) {
        	boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
        	LOGGER.debug("bounding_box: X=" + boundingBox.getX() + " - Y=" + boundingBox.getY());
        	LOGGER.debug("bounding_box: W=" + boundingBox.getW() + " - H=" + boundingBox.getH());
        }             
                
        Image image = SEBALHelper.readPixels(product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser, boundingBox, fmaskFilePath);
        
		Image preProcessedImageElevation = SEBALHelper.readPreProcessedData(image, product,
				boundingBoxVertices, pixelQuenteFrioChooser);
		
		Image preProcessedImageWeather = SEBALHelper.readPreProcessedData(image, product,
				boundingBoxVertices, pixelQuenteFrioChooser);
		
		List<ImagePixel> pixelsElevation = preProcessedImageElevation.pixels();
		List<ImagePixel> pixelsWeather = preProcessedImageWeather.pixels();
		
		int i = 0;
		int count = 0;
		LOGGER.info("Making the asserts...");
		
		double none = Double.NaN;
		
		for(ImagePixel imagePixel : pixelsElevation) {
			if(imagePixel.z() == none) {				
				assertField(imagePixel.geoLoc().getI(), pixelsWeather.get(i).geoLoc().getI());
				assertField(imagePixel.geoLoc().getJ(), pixelsWeather.get(i).geoLoc().getJ());				
				assertField(imagePixel.z(), pixelsWeather.get(i).Ta());								
				assertField(imagePixel.z(), pixelsWeather.get(i).ux());				
				assertField(imagePixel.z(), pixelsWeather.get(i).zx());							
				assertField(imagePixel.z(), pixelsWeather.get(i).d());				
				assertField(imagePixel.z(), pixelsWeather.get(i).hc());
			}
			
			if(imagePixel.z() != none) {
				System.out.println("z: " +  imagePixel.z());
				count++;
			}
				
			i++;
		}		
		
		System.out.println("Number of elements (z): " + count);
		
		LOGGER.info("Asserts completed successfully...");
	}
	
	private void assertField(double expectedValue, double obtainedValue) {
		assertEquals(expectedValue, obtainedValue, Math.abs(expectedValue * 0.05));
	}

}
