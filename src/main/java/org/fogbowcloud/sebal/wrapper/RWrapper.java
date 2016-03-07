package org.fogbowcloud.sebal.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.BoundingBoxVertice;
import org.fogbowcloud.sebal.ClusteredPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.PixelQuenteFrioChooser;
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;

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
        
		SEBALHelper.invalidatePixelsOutsideBoundingBox(image, boundingBoxVertices);
        
        LOGGER.debug("Pre process time read = " + (System.currentTimeMillis() - now));
        
        saveWeatherStationInfo(stationData);
        writeElevationTiff(product, image, boundingBox);        

        saveDadosOutput(rScriptFilePath);  
              
        LOGGER.info("Pre process execution time is " + (System.currentTimeMillis() - now));
    }

	private void rF1ScriptCaller() throws Exception {
		LOGGER.info("Calling F1 R script...");
		
		long now = System.currentTimeMillis();
		
		Process p = Runtime.getRuntime().exec("Rscript " + rScriptFilePath + rScriptFileName + " " + rScriptFilePath);
		p.waitFor();
		
		writeScriptLog(p);
		
		LOGGER.info("F1 R script execution time is " + (System.currentTimeMillis() - now));
	}
	
	private void writeScriptLog(Process p) throws Exception {
		String fileName = new File(mtlFile).getName();
		String imageFileName = fileName.substring(0, fileName.indexOf("_"));
		File fileOut = new File(outputDir + "/" + imageFileName + "_standard-out");
		File fileErr = new File(outputDir + "/" + imageFileName + "_standard-err");
		String s = new String();
		
		BufferedReader stdInput = new BufferedReader(new InputStreamReader(
				p.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(
				p.getErrorStream()));
		
		LOGGER.info("Writing standard output file...");
		while ((s = stdInput.readLine()) != null) {
			FileUtils.write(fileOut, s + "\n", true);
		}

		LOGGER.info("Writing standard error file...");
		while ((s = stdError.readLine()) != null) {
			FileUtils.write(fileErr, s + "\n", true);
		}
	}

	private void writeElevationTiff(Product product, Image image,
			BoundingBox boundingBox) throws Exception {
		gdal.AllRegister();

		Locale.setDefault(Locale.ROOT);
		org.esa.beam.framework.datamodel.Band bandAt = product.getBandAt(0);
		bandAt.ensureRasterData();

		if (boundingBox == null) {
			boundingBox = new BoundingBox(0, 0, bandAt.getRasterWidth(),
					bandAt.getRasterHeight());
		}
		
		int offSetX = boundingBox.getX();
		int offSetY = boundingBox.getY();
				
		
		int widthMax = Math.min(bandAt.getRasterWidth(),
				Math.min(iFinal, offSetX + boundingBox.getW()));
		int widthMin = Math.max(iBegin, offSetX);
		int maskWidth = Math.max(widthMax - widthMin, 0);
		
		int heightMax = Math.min(bandAt.getRasterHeight(),
				Math.min(jFinal, offSetY + boundingBox.getH()));
		int heightMin = Math.max(jBegin, offSetY);
		
		int maskHeight = Math.max(heightMax - heightMin, 0);	
		
		LOGGER.debug("mask width = " + maskWidth);
		LOGGER.debug("mask height = " + maskHeight);
		
		Double latMax = -360.;
		Double lonMin = +360.;

		for (ImagePixel pixel : image.pixels()) {
			latMax = Math.max(pixel.geoLoc().getLat(), latMax);
			lonMin = Math.min(pixel.geoLoc().getLon(), lonMin);
		}

		Band tiffBand;
		double[] rasterTiff;

		Driver tiffDriver = gdal.GetDriverByName("GTiff");
		String tiffFile = new File(getElevationFileName()).getAbsolutePath();


		Dataset dstTiff = tiffDriver.Create(tiffFile, maskWidth, maskHeight, 1,
				gdalconstConstants.GDT_Float64);
		tiffBand = createBand(product, dstTiff, lonMin, latMax);

		rasterTiff = new double[image.pixels().size()];

		
		int initialI = image.pixels().get(0).geoLoc().getI();
		int initialJ = image.pixels().get(0).geoLoc().getJ();
		
		for (int i = 0; i < image.pixels().size(); i++) {
			int iIdx = image.pixels().get(i).geoLoc().getI() - initialI;
			int jIdx = image.pixels().get(i).geoLoc().getJ() - initialJ;
			
			rasterTiff[jIdx * maskWidth + iIdx] = image.pixels().get(i).z();			
		}

		tiffBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterTiff);
		tiffBand.FlushCache();
	}
	
	private static Band createBand(Product product, Dataset dstNdviTiff, Double lonMin, Double latMax) {
		MetadataElement metadataRoot = product.getMetadataRoot();
		
		double ulLat = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_UL_LAT_PRODUCT").getData()
				.getElemDouble();
		double urLat = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_UR_LAT_PRODUCT").getData()
				.getElemDouble();
		double llLat = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_LL_LAT_PRODUCT").getData()
				.getElemDouble();
		double ulLon = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_UL_LON_PRODUCT").getData()
				.getElemDouble();
		double urLon = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_UR_LON_PRODUCT").getData()
				.getElemDouble();
		double llLon = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("CORNER_LL_LON_PRODUCT").getData()
				.getElemDouble();
		double lines = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("THERMAL_LINES").getData()
				.getElemDouble();
		double columns = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("THERMAL_SAMPLES").getData()
				.getElemDouble();
		
		double a = Math.abs(urLon) - Math.abs(ulLon);
		double b = Math.abs(ulLat) - Math.abs(urLat);
		double width = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		
		a = Math.abs(ulLat) - Math.abs(llLat);
		b = Math.abs(llLon) - Math.abs(ulLon);
		double heidth = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		
		double pixelSizeX = width/columns;
		double pixelSizeY = heidth/lines;		
				
		/*
		 * In case of north up images, the GT(2) and GT(4) coefficients are
		 * zero, and the GT(1) is pixel width, and GT(5) is pixel height.
		 * The (GT(0),GT(3)) position is the top left corner of the top left
		 * pixel of the raster.
		 */		
		
		if (pixelSizeX == -1 || pixelSizeY == -1) {
			throw new RuntimeException("Pixel size was not calculated propertly.");
		}
		
		LOGGER.debug("PIXEL_SIZE_X=" + pixelSizeX);
		LOGGER.debug("PIXEL_SIZE_Y=" + pixelSizeY);
		
		dstNdviTiff
				.SetGeoTransform(new double[] { lonMin, pixelSizeX, 0, latMax, 0, -pixelSizeY });
		SpatialReference srs = new SpatialReference();
		srs.SetWellKnownGeogCS("WGS84");
		dstNdviTiff.SetProjection(srs.ExportToWkt());
		Band bandNdvi = dstNdviTiff.GetRasterBand(1);
		return bandNdvi;
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
					resultLine = getRow("N", "File images", "MTL", "File Elevation",
							"File Station", "Bounding Box Path");
				} else {
					count++;
					resultLine = getRow(count, imagesPath, mtlFile, outputDir + "/"
							+ imageFileName + "_" + iBegin + "." + iFinal + "."
							+ jBegin + "." + jFinal + ".elevation.tiff",
							 outputDir + "/" + imageFileName + "_" + iBegin
									+ "." + iFinal + "." + jBegin + "."
									+ jFinal + ".station.csv", outputDir + "/" + "boundingbox_vertices");
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
