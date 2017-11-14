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
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.model.image.BoundingBox;

public class RWrapper {
	
	private Properties properties;
	private String imageName;
	private String mtlFilePath;
    private int iBegin;
    private int iFinal;
    private int jBegin;
    private int jFinal;
    private String outputDir;
    private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();

	private static final Logger LOGGER = Logger.getLogger(RWrapper.class);

	public RWrapper(String imagesPath, String outputDir, String imageName, String mtlFile, int iBegin, int iFinal, int jBegin,
			int jFinal, String boundingBoxFileName, Properties properties) throws IOException {
		
		this.imageName = imageName;
		this.mtlFilePath = mtlFile;
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
		
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);
	}
	
	public void doTask(String taskType) throws Exception {
		try {
        	if(taskType.equalsIgnoreCase(TaskType.PREPROCESS)) {
        		preProcessingPixels();
                return;
        	}
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(128);
        }
	}

	public void preProcessingPixels()
			throws Exception {
		LOGGER.info("Pre processing pixels...");

		long now = System.currentTimeMillis();
		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);

		BoundingBox boundingBox = null;
		if (boundingBoxVertices.size() > 3) {
			boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
			LOGGER.debug("bounding_box: X=" + boundingBox.getX() + " - Y=" + boundingBox.getY());
			LOGGER.debug("bounding_box: W=" + boundingBox.getW() + " - H=" + boundingBox.getH());
		}

		String stationData = SEBALHelper.getStationData(properties, product, iBegin, iFinal, jBegin,
				jFinal, boundingBox);

		if (stationData != null && !stationData.isEmpty()) {
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

		LOGGER.info("stationFileName=" + weatherPixelsFileName);
		File outputFile = new File(weatherPixelsFileName);
		try {
			FileUtils.write(outputFile, "");
			FileUtils.write(outputFile, stationData, true);
		} catch (IOException e) {
			LOGGER.error("Error while writing station file.", e);
		}
		LOGGER.debug("Saving station data output time=" + (System.currentTimeMillis() - now));
	}
	
    private String getWeatherFileName() {
    	return SEBALHelper.getWeatherFilePath(outputDir, "", imageName);
    }

}
