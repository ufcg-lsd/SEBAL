package org.fogbowcloud.sebal.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.BoundingBoxVertice;
import org.fogbowcloud.sebal.ClusteredPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.PixelQuenteFrioChooser;
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.model.image.BoundingBox;

public class RWrapper {
	
	private Properties properties;
	private String mtlFile;
    private int iBegin;
    private int iFinal;
    private int jBegin;
    private int jFinal;
    private String outputDir;
    private PixelQuenteFrioChooser pixelQuenteFrioChooser;
    private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();

	private static final Logger LOGGER = Logger.getLogger(RWrapper.class);

	public RWrapper(Properties properties) throws IOException {
		String mtlFilePath = properties.getProperty("mtl_file_path");
		if (mtlFilePath == null || mtlFilePath.isEmpty()) {
			LOGGER.error("Property mtl_file_path must be set.");
			throw new IllegalArgumentException("Property mtl_file_path must be set.");
		}
		this.mtlFile = mtlFilePath;
		this.properties = properties;

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
	}

	public RWrapper(String imagesPath, String outputDir, String imageName, String mtlFile, int iBegin, int iFinal, int jBegin,
			int jFinal, String boundingBoxFileName, Properties properties) throws IOException {
		this.mtlFile = mtlFile;
		this.iBegin = iBegin;
		this.iFinal = iFinal;
		this.jBegin = jBegin;
		this.jFinal = jFinal;
		this.properties = properties;
		
		if (outputDir == null) {
			this.outputDir = imageName;
		} else {
			if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
				new File(outputDir).mkdirs();
			}
			this.outputDir = outputDir + imageName;
		}
		
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);
	}
	
	public void doTask(String taskType) throws Exception {
		try {
        	if(taskType.equalsIgnoreCase(TaskType.PREPROCESS)) {
        		preProcessingPixels(pixelQuenteFrioChooser);
                return;
        	}
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(128);
        }
	}

	public void preProcessingPixels(PixelQuenteFrioChooser pixelQuenteFrioChooser) 
    		throws Exception{
    	LOGGER.info("Pre processing pixels...");
    	
    	long now = System.currentTimeMillis();
        Product product = SEBALHelper.readProduct(mtlFile, boundingBoxVertices);
                
        BoundingBox boundingBox = null;
        if (boundingBoxVertices.size() > 3) {
        	boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
        	LOGGER.debug("bounding_box: X=" + boundingBox.getX() + " - Y=" + boundingBox.getY());
        	LOGGER.debug("bounding_box: W=" + boundingBox.getW() + " - H=" + boundingBox.getH());
        }
        
        String stationData = SEBALHelper.getStationData(properties, product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser, boundingBox);
        
        if(stationData != null) {        	
        	LOGGER.debug("stationData: " + stationData);               
        	LOGGER.debug("Pre process time read = " + (System.currentTimeMillis() - now));
        	
        	saveWeatherStationInfo(stationData);              
        	LOGGER.info("Pre process execution time is " + (System.currentTimeMillis() - now));
        } else {
        	LOGGER.error("Error while getting station data");
        }
    }
	
	private void saveWeatherStationInfo(String stationData) {
		long now = System.currentTimeMillis();
		String weatherPixelsFileName = getWeatherFileName();
		
		File outputFile = new File(weatherPixelsFileName);
		try {
			FileUtils.write(outputFile, "");			
			FileUtils.write(outputFile, stationData, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.debug("Saving station data output time="
				+ (System.currentTimeMillis() - now));
	}
	
    private String getWeatherFileName() {
    	String fileName = new File(mtlFile).getName();
        String imageFileName = fileName.substring(0, fileName.indexOf("_"));
    	return SEBALHelper.getWeatherFilePath(outputDir, "", imageFileName);
    }

}
