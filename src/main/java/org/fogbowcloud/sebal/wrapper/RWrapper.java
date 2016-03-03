package org.fogbowcloud.sebal.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public class RWrapper {
	
	private String imagesPath;
	private String mtlFile;
    private int iBegin;
    private int iFinal;
    private int jBegin;
    private int jFinal;
    private String outputDir;
    private PixelQuenteFrioChooser pixelQuenteFrioChooser;
    private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
    private String fmaskFilePath;
    private String rScriptFilePath;
    private String rScriptFileName;
    
	private static final Logger LOGGER = Logger.getLogger(Wrapper.class);
    
	public RWrapper(Properties properties) throws IOException {
		String mtlFilePath = properties.getProperty("mtl_file_path");
		if (mtlFilePath == null || mtlFilePath.isEmpty()) {
			LOGGER.error("Property mtl_file_path must be set.");
			throw new IllegalArgumentException("Property mtl_file_path must be set.");
		}
		this.mtlFile = mtlFilePath;
		
		this.imagesPath = properties.getProperty("images_path");
		
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
		String rScriptFilePath = properties.getProperty("rScript_file_path");
		String rScriptFileName = properties.getProperty("rScript_file_name");

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
	
	public RWrapper(String imagesPath, String mtlFile, String outputDir, int iBegin, int iFinal, int jBegin,
			int jFinal, String mtlName, String boundingBoxFileName, Properties properties,
			String fmaskFilePath, String rScriptFilePath, String rScriptFileName) throws IOException {
		this.imagesPath = imagesPath;
		this.mtlFile = mtlFile;
		this.iBegin = iBegin;
		this.iFinal = iFinal;
		this.jBegin = jBegin;
		this.jFinal = jFinal;

		boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);

		// this.pixelQuenteFrioChooser = new RandomPixelQuenteFrioChooser();
		// this.pixelQuenteFrioChooser = new DefaultPixelQuenteFrioChooser();
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
		if (outputDir == null) {
			this.outputDir = mtlName;
		} else {
			if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
				new File(outputDir).mkdirs();
			}
			this.outputDir = outputDir + mtlName;
		}
		
		this.rScriptFilePath = rScriptFilePath;
		this.rScriptFileName = rScriptFileName;
		this.fmaskFilePath = fmaskFilePath;
	}
	
	public void doTask(String taskType) throws Exception {
		try {
        	if(taskType.equalsIgnoreCase(TaskType.PREPROCESS)) {
        		preProcessingPixels(pixelQuenteFrioChooser);
                return;
        	}        	
        	if(taskType.equalsIgnoreCase(TaskType.F1RCALL)) {
        		rF1ScriptCaller();
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
        
        String stationData = SEBALHelper.getStationData(product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser, boundingBox);
        LOGGER.debug("stationData: " + stationData);
       
        Image image = SEBALHelper.getElevationData(product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser, boundingBox, fmaskFilePath);
        
//        Image image = SEBALHelper.readPixels(product, iBegin, iFinal, jBegin,
//                jFinal, pixelQuenteFrioChooser, boundingBox, fmaskFilePath);
        
		SEBALHelper.invalidatePixelsOutsideBoundingBox(image, boundingBoxVertices);
        
        LOGGER.debug("Pre process time read = " + (System.currentTimeMillis() - now));
        
        saveElevationOutput(image);
        saveWeatherStationInfo(stationData);

        saveDadosOutput(rScriptFilePath);  
              
        LOGGER.info("Pre process execution time is " + (System.currentTimeMillis() - now));
    }

	private void rF1ScriptCaller() throws IOException, InterruptedException {
		LOGGER.info("Calling F1 R script...");
		
		long now = System.currentTimeMillis();	
		
		Process p = Runtime.getRuntime().exec("Rscript " + rScriptFilePath + rScriptFileName + " " + rScriptFilePath);
		p.waitFor();
		
		p.getErrorStream();
		p.getOutputStream();
		
		LOGGER.info("F1 R script execution time is " + (System.currentTimeMillis() - now));
	}
	
	private void saveDadosOutput(String rScriptFilePath) {
		long now = System.currentTimeMillis();
		String dadosFileName = getDadosFileName(rScriptFilePath);
		String resultLine = new String();
		String fileName = new File(mtlFile).getName();
		String imageFileName = fileName.substring(0, fileName.indexOf("_"));
		int count = 0;

		File outputFile = new File(dadosFileName);
		try {
			FileUtils.write(outputFile, "");
			for (int i = 0; i < 3; i++) {
				if (i == 0) {
					resultLine = getRow("N", "File images", "File Elevation",
							"MTL", "File Station", "Output Path", "Prefix");
				} else {
					count++;
					resultLine = getRow(count, imagesPath, outputDir + "/"
							+ imageFileName + "_" + iBegin + "." + iFinal + "."
							+ jBegin + "." + jFinal + ".elevation.csv",
							mtlFile, outputDir + "/" + imageFileName + "_" + iBegin
									+ "." + iFinal + "." + jBegin + "."
									+ jFinal + ".station.csv", outputDir, imageFileName
									+ "_" + iBegin + "." + iFinal + "."
									+ jBegin + "." + jFinal + ".");
				}
				FileUtils.write(outputFile, resultLine, true);
				i++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.debug("Saving dados output time="
				+ (System.currentTimeMillis() - now));
	}
    
	private void saveElevationOutput(Image image) {
		long now = System.currentTimeMillis();
		List<ImagePixel> pixels = image.pixels();
		String elevationPixelsFileName = getElevationFileName();
		int count = 0;
		String resultLine = new String();		

		File outputFile = new File(elevationPixelsFileName);
		try {
			FileUtils.write(outputFile, "");
			for (ImagePixel imagePixel : pixels) {
				if(count == 0) {
					resultLine = getRow("latitude", "longitude", "elevation");
				} else {
				resultLine = getRow(imagePixel.geoLoc().getLat(), imagePixel.geoLoc().getLon(),
						imagePixel.z());
				}
				FileUtils.write(outputFile, resultLine, true);
				count++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.debug("Saving elevation output time="
				+ (System.currentTimeMillis() - now));
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
    	return SEBALHelper.getWeatherFilePath(outputDir, "", imageFileName, iBegin, iFinal, jBegin, jFinal);
    }
    
    private String getElevationFileName() {
    	String fileName = new File(mtlFile).getName();
        String imageFileName = fileName.substring(0, fileName.indexOf("_"));
    	return SEBALHelper.getElevationFilePath(outputDir, "", imageFileName , iBegin, iFinal, jBegin, jFinal);
    }
    
	private String getDadosFileName(String rScriptFilePath) {
		return SEBALHelper.getDadosFilePath(rScriptFilePath);
	}
    
    private static String getRow(Object... rowItems) {
        StringBuilder sb = new StringBuilder();
        for (Object rowItem : rowItems) {
            sb.append(rowItem).append(",");
        }
        sb.setCharAt(sb.length() - 1, '\n');
        return sb.toString();
    }

}
