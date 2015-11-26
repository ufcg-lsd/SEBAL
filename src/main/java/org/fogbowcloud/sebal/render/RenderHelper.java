package org.fogbowcloud.sebal.render;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.BoundingBoxVertice;
import org.fogbowcloud.sebal.BulkHelper;
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.XPartitionInterval;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;

import ucar.ma2.InvalidRangeException;

import org.apache.log4j.Logger;

public class RenderHelper {

	protected static double PIXEL_SIZE_X = - 1;
	protected static double PIXEL_SIZE_Y = -1;
	
	public static final String TIFF = "tiff";
	public static final String BMP = "bmp";
	public static final String NET_CDF = "netcdf";
	
	private static final Logger LOGGER = Logger.getLogger(RenderHelper.class);
	
	public static void main(String[] args) throws ParseException, Exception {
		String mtlFilePath = args[0];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		String outputDir = args[1];
		int leftX = Integer.parseInt(args[2]);
		int lowerY = Integer.parseInt(args[3]);
		int rightX = Integer.parseInt(args[4]);
		int upperY = Integer.parseInt(args[5]);

		int numberOfPartitions = Integer.parseInt(args[6]);
		int partitionIndex = Integer.parseInt(args[7]);
		
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		if (args[8] != null) {
			String boundingboxFilePath = args[8];
			LOGGER.info("Bounding box file path is " + boundingboxFilePath);
			boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingboxFilePath );			
		}
		
		String coordinatesMaskFilePath = null;
		if (args[9] != null) {
			coordinatesMaskFilePath = args[9];
			LOGGER.info("Coordinates mask file path is " + coordinatesMaskFilePath);
		}
		
		String pixelSizeFilePath = null;
		if (args[10] != null && new File(args[10]).exists()) {
			pixelSizeFilePath = args[10];
			LOGGER.info("Pixel size file path is " + pixelSizeFilePath);
			
			Properties p = new Properties();
			FileInputStream input = new FileInputStream(pixelSizeFilePath);
			p.load(input);
			PIXEL_SIZE_X = Double.parseDouble(p.getProperty("pixel_size_x"));
			PIXEL_SIZE_Y = Double.parseDouble(p.getProperty("pixel_size_y"));
		}
		
		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(leftX, rightX,
				numberOfPartitions, partitionIndex);

		String csvFilePath = SEBALHelper.getAllPixelsFilePath(outputDir, mtlName,
				imagePartition.getIBegin(), imagePartition.getIFinal(), lowerY, upperY);
		
		long daysSince1970 = SEBALHelper.getDaysSince1970(mtlFilePath);		
		String prefixRaw = leftX + "." + rightX + "." + lowerY + "." + upperY;
			
		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);
		
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
		
		if (PIXEL_SIZE_X == -1 && PIXEL_SIZE_Y == -1) {
			calculatePixelSize(ulLon, ulLat, urLon, urLat, llLon, llLat, columns, lines);
		}
		
		BoundingBox boundingBox = null;
		if (boundingBoxVertices.size() > 3) {
			boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
		}
		int offSetX = boundingBox.getX();
		int offSetY = boundingBox.getY();
		
		int maskWidth = Math.min(imagePartition.getIFinal(), offSetX + boundingBox.getW()) - Math.max(imagePartition.getIBegin(), offSetX);
		int maskHeight = Math.min(upperY, offSetY + boundingBox.getH()) - Math.max(lowerY, offSetY);
		
		LOGGER.debug("mask width = " + maskWidth);
		LOGGER.debug("mask height = " + maskHeight);
		
//		int maskWidth = Math.min(rightX, offSetX + boundingBox.getW()) - Math.max(leftX, offSetX);
//		int maskHeight = Math.min(upperY, offSetY + boundingBox.getH()) - Math.max(lowerY, offSetY);
		
		
		
//		int widthMax = Math.min(bandAt.getRasterWidth(),
//				Math.min(iFinal, offSetX + boundingBox.getW()));
//		int widthMin = Math.max(iBegin, offSetX);
//		
////		image.width(Math.max(widthMax - widthMin, 0));
//		
//		int heightMax = Math.min(bandAt.getRasterHeight(),
//				Math.min(jFinal, offSetY + boundingBox.getH()));
//		int heightMin = Math.max(jBegin, offSetY);
		
		
//		render(csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
//				maskHeight, daysSince1970, RenderHelper.TIFF, RenderHelper.BMP,
//				RenderHelper.NET_CDF);
		
//		render(csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
//				maskHeight, daysSince1970, args[9]);
		
		
//		vars.add(bandVariableBuilder.build("ndvi", 7));
//		vars.add(bandVariableBuilder.build("evi", 24));
//		vars.add(bandVariableBuilder.build("iaf", 23));
//		vars.add(bandVariableBuilder.build("ts", 6));
//		vars.add(bandVariableBuilder.build("alpha", 9));
//		vars.add(bandVariableBuilder.build("rn", 5));
//		vars.add(bandVariableBuilder.build("g", 4))
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "ndvi", 7, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "evi", 24, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "iaf", 23, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "ts", 6, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "alpha", 9, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "rn", 5, args[11]);
		
		render(coordinatesMaskFilePath, csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, maskWidth,
				maskHeight, daysSince1970, "g", 4, args[11]);
		
//		render(csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex,
//				imagePartition.getIFinal() - imagePartition.getIBegin(), lowerY - upperY,
//				daysSince1970, RenderHelper.TIFF);
	}

	protected static void calculatePixelSize(double ulLon, double ulLat,
			double urLon, double urLat, double llLon, double llLat,
			double columns, double lines) {
		double a = Math.abs(urLon) - Math.abs(ulLon);
		double b = Math.abs(ulLat) - Math.abs(urLat);
		double width = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		
		a = Math.abs(ulLat) - Math.abs(llLat);
		b = Math.abs(llLon) - Math.abs(ulLon);
		double heidth = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
		
		PIXEL_SIZE_X = width/columns;
		PIXEL_SIZE_Y = heidth/lines;		
	}

	private static class BandVariableBuilder {
		private String imgPrefix;
		private String outputPath;
		private int maskWidth;
		private int maskHeight;
		private Double ulLon;
		private Double ulLat;
		private Integer initialI;
		private Integer initialJ;
		private String[] drivers;

		public BandVariableBuilder(String imgPrefix, String outputPath, int maskWidth,
				int maskHeight, Double ulLon, Double ulLat, Integer initialI, Integer initialJ,
				String[] drivers) {
			this.imgPrefix = imgPrefix;
			this.outputPath = outputPath;
			this.maskWidth = maskWidth;
			this.maskHeight = maskHeight;
			this.ulLon = ulLon;
			this.ulLat = ulLat;
			this.initialI = initialI;
			this.initialJ = initialJ;
			if (drivers != null) {
				this.drivers = drivers;
			} else {
				this.drivers = new String[] { NET_CDF };
			}
		}

		public BandVariable build(String varName, int columnIdx) {
			return new BandVariable(varName, imgPrefix, outputPath, maskWidth, maskHeight, ulLon,
					ulLat, initialI, initialJ, columnIdx, drivers);
		}
	}

	private static class BandVariable {

		private int columnIdx;
		private Band tiffBand;
		private Band bmpBand;
		private Band netCDFBand;
		private double[] rasterTiff;
		private double[] rasterBmp;
		private Integer initialI;
		private Integer initialJ;
		private int maskWidth;
		private int maskHeight;
		private double[] rasterNetCDF;
		private String netCDFFile;
		private String varName;
		private String[] drivers;

		public BandVariable(String varName, String imgPrefix, String outputPath, int maskWidth,
				int maskHeight, Double ulLon, Double ulLat, Integer initialI, Integer initialJ,
				int columnIdx, String[] drivers) {

			this.varName = varName;
			this.maskWidth = maskWidth;
			this.maskHeight = maskHeight;
			this.initialI = initialI;
			this.initialJ = initialJ;
			this.columnIdx = columnIdx;
			this.drivers = drivers;

			
			for (String driver : drivers) {
				if (driver.equals(TIFF)) {
					Driver tiffDriver = gdal.GetDriverByName("GTiff");
					String tiffFile = new File(outputPath, imgPrefix + "_new_" + varName + ".tiff")
							.getAbsolutePath();
					Dataset dstTiff = tiffDriver.Create(tiffFile, maskWidth, maskHeight, 1,
							gdalconstConstants.GDT_Float64);
					this.tiffBand = createBand(dstTiff, ulLon, ulLat);
					this.rasterTiff = new double[maskHeight * maskWidth];
				} else if (driver.equals(BMP)) {
					Driver bmpDriver = gdal.GetDriverByName("BMP");
					String bmpFile = new File(outputPath, imgPrefix + "_new_" + varName + ".bmp")
							.getAbsolutePath();
					Dataset dstBmp = bmpDriver.Create(bmpFile, maskWidth, maskHeight, 1,
							gdalconstConstants.GDT_Byte);
					this.bmpBand = createBand(dstBmp, ulLon, ulLat);
					this.rasterBmp = new double[maskHeight * maskWidth];
				} else if (driver.equals(NET_CDF)) {
					Driver netCDFDriver = gdal.GetDriverByName("NetCDF");
					this.netCDFFile = new File(outputPath, imgPrefix + "_new_" + varName + ".nc")
							.getAbsolutePath();
					Dataset dstNetCDF = netCDFDriver.Create(netCDFFile, maskWidth, maskHeight, 1,
							gdalconstConstants.GDT_Float64);
					this.netCDFBand = createBand(dstNetCDF, ulLon, ulLat);
					this.rasterNetCDF = new double[maskHeight * maskWidth];
				}
			}
		}

//		public void read(String[] splitLine) {
		public void read(int i, int j, String[] splitLine) {
//			int i = Integer.parseInt(splitLine[0]);
//			int j = Integer.parseInt(splitLine[1]);
			int iIdx = i - initialI;
			int jIdx = j - initialJ;
//			double val;
//			try {
//				val = Double.parseDouble(splitLine[columnIdx]);				
//			} catch (Exception e) {
//				LOGGER.error("There was an error while reading var from csv.", e);
//				val = Double.NaN;
//			}
			double val;
			if (splitLine == null) {
				LOGGER.error("There was an error while reading var from csv. i=" + i + " and j="
						+ j);
				val = Double.NaN;
			} else {
				val = Double.parseDouble(splitLine[columnIdx]);
			}
			
			if (rasterTiff != null) {
				rasterTiff[jIdx * maskWidth + iIdx] = val;
			}
			if (rasterNetCDF != null) {
				rasterNetCDF[jIdx * maskWidth + iIdx] = val;
			}
			if (rasterBmp != null) {
				rasterBmp[jIdx * maskWidth + iIdx] = val * 255;
			}
		}

		public void render(double daysSince1970) {
			for (String format : drivers) {
				if (format.equals(TIFF)) {
					tiffBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterTiff);
					tiffBand.FlushCache();
				} else if (format.equals(BMP)) {
					bmpBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterBmp);
					bmpBand.FlushCache();
				} else if (format.equals(NET_CDF)) {
					netCDFBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterNetCDF);
					netCDFBand.FlushCache();

					try {
						NetCDFHelper.normalize(netCDFFile, varName, daysSince1970);
					} catch (IOException | InvalidRangeException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private static Band createBand(Dataset dstNdviTiff, Double ulLon, Double ulLat) {
//			double[] geoTransform = dstNdviTiff.GetGeoTransform();
//			for (double d : geoTransform) {
//				System.out.println(d);
//			}
			/*
			 * In case of north up images, the GT(2) and GT(4) coefficients are
			 * zero, and the GT(1) is pixel width, and GT(5) is pixel height.
			 * The (GT(0),GT(3)) position is the top left corner of the top left
			 * pixel of the raster.
			 */
			
			if (PIXEL_SIZE_X == -1 || PIXEL_SIZE_Y == -1) {
				throw new RuntimeException("Pixel size was not calculated propertly.");
			}
			
			System.out.println("PIXEL_SIZE_X=" + PIXEL_SIZE_X);
			System.out.println("PIXEL_SIZE_Y=" + PIXEL_SIZE_Y);
			
			dstNdviTiff
					.SetGeoTransform(new double[] { ulLon, PIXEL_SIZE_X, 0, ulLat, 0, -PIXEL_SIZE_Y });
			SpatialReference srs = new SpatialReference();
			srs.SetWellKnownGeogCS("WGS84");
			dstNdviTiff.SetProjection(srs.ExportToWkt());
			Band bandNdvi = dstNdviTiff.GetRasterBand(1);
			return bandNdvi;
		}
	}

//	public static void render(String csvFile, String outputFilePrefix, int maskWidth,
//			int maskHeight, double daysSince1970, String... drivers) throws IOException,
//			FileNotFoundException {
		
	public static void render(String coordinateMaskFile, String csvFile, String outputFilePrefix, int maskWidth,
			int maskHeight, double daysSince1970, String varName, int col, String... drivers)
			throws IOException, FileNotFoundException {
		gdal.AllRegister();
		
		Double latMax = -360.;
		Double lonMin = +360.;
		Integer initialI = null;
		Integer initialJ = null;
		
		LineIterator maskLineIterator;
		if (coordinateMaskFile != null && new File(coordinateMaskFile).exists()) {
			maskLineIterator = IOUtils.lineIterator(new FileInputStream(coordinateMaskFile), Charsets.UTF_8);
		} else {
			maskLineIterator = IOUtils.lineIterator(new FileInputStream(csvFile), Charsets.UTF_8);
		}
		
		int coordinatesCount = 0;
		while (maskLineIterator.hasNext()) {
			String line = (String) maskLineIterator.next();
			String[] lineSplit = line.split(",");
			if (initialI == null && initialJ == null) {
				initialI = Integer.parseInt(lineSplit[0]);
				initialJ = Integer.parseInt(lineSplit[1]);
				
				System.out.println("initialI=" + initialI + " ------ initialJ=" + initialJ);
				System.out.println("initialLat=" + Double.parseDouble(lineSplit[2]) + " ------ initialLon=" + Double.parseDouble(lineSplit[3]));
			}
			Double lat = Double.parseDouble(lineSplit[2]);
			Double lon = Double.parseDouble(lineSplit[3]);
			latMax = Math.max(lat, latMax);
			lonMin = Math.min(lon, lonMin);
			coordinatesCount++;
		}
		
		System.out.println("coordinatesSize=" + coordinatesCount);
		System.out.println("latMax=" + latMax + "lonMin=" + lonMin);
		
		BandVariableBuilder bandVariableBuilder = new BandVariableBuilder(outputFilePrefix,
				new File(csvFile).getParent(), maskWidth, maskHeight, lonMin, latMax, initialI,
				initialJ, drivers);
		List<BandVariable> vars = new LinkedList<BandVariable>();
		vars.add(bandVariableBuilder.build(varName, col));
//		vars.add(bandVariableBuilder.build("ndvi", 7));
//		vars.add(bandVariableBuilder.build("evi", 24));
//		vars.add(bandVariableBuilder.build("iaf", 23));
//		vars.add(bandVariableBuilder.build("ts", 6));
//		vars.add(bandVariableBuilder.build("alpha", 9));
//		vars.add(bandVariableBuilder.build("rn", 5));
//		vars.add(bandVariableBuilder.build("g", 4));

		LineIterator csvLineIterator = IOUtils.lineIterator(new FileInputStream(csvFile), Charsets.UTF_8);

		while (maskLineIterator.hasNext()) {
			String line = (String) maskLineIterator.next();
			String[] maskSplit = line.split(",");
			
			String[] csvSplit = null;
			if (csvLineIterator.hasNext()) {
				String csvLine = (String) csvLineIterator.next();
				csvSplit = csvLine.split(",");
			}
			
			for (BandVariable var : vars) {
				var.read(Integer.parseInt(maskSplit[0]), Integer.parseInt(maskSplit[1]), csvSplit);
			}
		}

		for (BandVariable var : vars) {
			var.render(daysSince1970);
		}
	}
}