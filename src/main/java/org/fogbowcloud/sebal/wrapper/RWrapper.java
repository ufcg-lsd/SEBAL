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
import org.fogbowcloud.sebal.util.SEBALAppConstants;

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

	public RWrapper(String outputDir, String imageName, String mtlFile, int iBegin, int iFinal,
			int jBegin, int jFinal, String boundingBoxFileName, Properties properties)
			throws IOException {

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
			File outputDirectory = new File(outputDir);
			if (!outputDirectory.exists() || !outputDirectory.isDirectory()) {
				outputDirectory.mkdirs();
			}
			this.outputDir = outputDir + imageName;
		}

		this.boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);
	}

	public void doTask(String taskType) throws Exception {
		try {
			if (taskType.equalsIgnoreCase(TaskType.PREPROCESS)) {
				this.preProcessingPixels();
			}
		} catch (Exception e) {
			LOGGER.error("Error while trying to get station data", e);
			throw e;
		}
	}

	public void preProcessingPixels() throws Exception {
		LOGGER.info("Pre processing pixels...");

		long now = System.currentTimeMillis();
		Product product = SEBALHelper.readProduct(this.mtlFilePath, this.boundingBoxVertices);

		BoundingBox boundingBox = null;
		if (this.boundingBoxVertices.size() > 3) {
			boundingBox = SEBALHelper.buildBoundingBox(this.boundingBoxVertices, product);
			
			LOGGER.debug("Bounding box: X=" + boundingBox.getX() + " - Y=" + boundingBox.getY());
			LOGGER.debug("Bounding box: W=" + boundingBox.getW() + " - H=" + boundingBox.getH());
		}

		String stationData = SEBALHelper.getStationData(this.properties, product, this.iBegin,
				this.iFinal, this.jBegin, this.jFinal, boundingBox);

		if (stationData != null && !stationData.isEmpty()) {
			LOGGER.debug("stationData: " + stationData);
			
			this.saveWeatherStationInfo(stationData);
			
			LOGGER.info(
					"Pre process execution time [" + (System.currentTimeMillis() - now) + "] ms");
		} else {
			LOGGER.error("Was not possible found a station data that fits the standards!");
		}
	}

	private void saveWeatherStationInfo(String stationData) {
		long now = System.currentTimeMillis();
		String weatherPixelsFileName = getWeatherFileName();

		LOGGER.info("stationFileName=" + weatherPixelsFileName);
		File outputFile = new File(weatherPixelsFileName);
		try {
			FileUtils.write(outputFile, stationData, SEBALAppConstants.FILE_ENCODING);
		} catch (IOException e) {
			LOGGER.error("Error while writing station file.", e);
		}
		
		LOGGER.debug("Saving station data output time=" + (System.currentTimeMillis() - now));
	}

	private String getWeatherFileName() {
		return SEBALHelper.getWeatherFilePath(this.outputDir, "", this.imageName);
	}

}
