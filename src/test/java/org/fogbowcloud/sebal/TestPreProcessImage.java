package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;

import java.io.File;
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
    private String outputDir;
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

		String fileName = new File(mtlFile).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));
		String outputDir = properties.getProperty("output_dir_path");

		if (outputDir == null || outputDir.isEmpty()) {
    		this.outputDir = mtlName;
    	} else {
    		if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
    			new File(outputDir).mkdirs();
    		}
    		this.outputDir = outputDir + "/" + mtlName;
    	}
		
		fmaskFilePath = properties.getProperty("fmask_file_path");
	}
	
	@Test
	public void acceptanceTest() throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);
				
		setProperties(properties);
		
		LOGGER.info("Pre processing pixels...");
    	
    	long now = System.currentTimeMillis();
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
		
		for(ImagePixel imagePixel : pixelsElevation) {
			if(imagePixel.z() == Double.NaN) {
				
				System.out.println(imagePixel.z() + " - " + pixelsWeather.get(i).Ta());
				assertField(imagePixel.z(), pixelsWeather.get(i).Ta());				
				
				System.out.println(imagePixel.z() + " - " + pixelsWeather.get(i).ux());
				assertField(imagePixel.z(), pixelsWeather.get(i).ux());
				
				System.out.println(imagePixel.z() + " - " + pixelsWeather.get(i).zx());
				assertField(imagePixel.z(), pixelsWeather.get(i).zx());
				
				System.out.println(imagePixel.z() + " - " + pixelsWeather.get(i).d());
				assertField(imagePixel.z(), pixelsWeather.get(i).d());
				
				System.out.println(imagePixel.z() + " - " + pixelsWeather.get(i).hc());
				assertField(imagePixel.z(), pixelsWeather.get(i).hc());
			}
			i++;
		}		
	}
	
	private void assertField(double expectedValue, double obtainedValue) {
		assertEquals(expectedValue, obtainedValue, Math.abs(expectedValue * 0.05));
	}

}
