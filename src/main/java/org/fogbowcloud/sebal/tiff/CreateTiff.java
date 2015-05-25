package org.fogbowcloud.sebal.tiff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;

public class CreateTiff {

	private static double PIXEL_SIZE = 0.00027;

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		String csvFile = args[1];
		String tifFilePrefix = args[2];
		int maskWidth = Integer.parseInt(args[3]);
		int maskHeight = Integer.parseInt(args[4]);

		createTiff(csvFile, tifFilePrefix, maskWidth, maskHeight);
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

		public BandVariableBuilder(String imgPrefix, String outputPath, int maskWidth, 
				int maskHeight, Double ulLon, Double ulLat, Integer initialI, Integer initialJ) {
			this.imgPrefix = imgPrefix;
			this.outputPath = outputPath;
			this.maskWidth = maskWidth;
			this.maskHeight = maskHeight;
			this.ulLon = ulLon;
			this.ulLat = ulLat;
			this.initialI = initialI;
			this.initialJ = initialJ;
		}
		
		public BandVariable build(String varName, int columnIdx) {
			return new BandVariable(varName, imgPrefix, outputPath, maskWidth, 
					maskHeight, ulLon, ulLat, initialI, initialJ, columnIdx);
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

		public BandVariable(String varName, String imgPrefix, String outputPath, int maskWidth, 
				int maskHeight, Double ulLon, Double ulLat, Integer initialI, Integer initialJ, int columnIdx) {
			
			this.maskWidth = maskWidth;
			this.maskHeight = maskHeight;
			this.initialI = initialI;
			this.initialJ = initialJ;
			this.columnIdx = columnIdx;
			
			Driver tiffDriver = gdal.GetDriverByName("GTiff");
			String tiffFile = new File(outputPath, imgPrefix + "_" + varName + ".tiff").getAbsolutePath();
			Dataset dstTiff = tiffDriver.Create(tiffFile, maskWidth, maskHeight, 1,
					gdalconstConstants.GDT_Float64);
			this.tiffBand = createBand(dstTiff, ulLon, ulLat);
			
			Driver netCDFDriver = gdal.GetDriverByName("NetCDF");
			String netCDFFile = new File(outputPath, imgPrefix + "_" + varName + ".nc").getAbsolutePath();
			Dataset dstNetCDF = netCDFDriver.Create(netCDFFile, maskWidth, maskHeight, 1,
					gdalconstConstants.GDT_Float64);
			this.netCDFBand = createBand(dstNetCDF, ulLon, ulLat);
			
			
			Driver bmpDriver = gdal.GetDriverByName("BMP");
			String bmpFile = new File(outputPath, imgPrefix + "_" + varName + ".bmp").getAbsolutePath();
			Dataset dstBmp = bmpDriver.Create(bmpFile, maskWidth, maskHeight, 1,
					gdalconstConstants.GDT_Byte);
			this.bmpBand = createBand(dstBmp, ulLon, ulLat);
			
			this.rasterTiff = new double[maskHeight * maskWidth];
			this.rasterBmp = new double[maskHeight * maskWidth];
			this.rasterNetCDF = new double[maskHeight * maskWidth];
		}
		
		public void read(String[] splitLine) {
			int i = Integer.parseInt(splitLine[0]);
			int j = Integer.parseInt(splitLine[1]);
			int iIdx = i - initialI;
			int jIdx = j - initialJ;
			double val = Double.parseDouble(splitLine[columnIdx]);
			rasterTiff[jIdx * maskWidth + iIdx] = val;
			rasterNetCDF[jIdx * maskWidth + iIdx] = val;
			rasterBmp[jIdx * maskWidth + iIdx] = val * 255;
		}
		
		public void render() {
			tiffBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterTiff);
			tiffBand.FlushCache();
			bmpBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterBmp);
			bmpBand.FlushCache();
			netCDFBand.WriteRaster(0, 0, maskWidth, maskHeight, rasterBmp);
			netCDFBand.FlushCache();
		}
		
		private static Band createBand(Dataset dstNdviTiff, Double ulLon, Double ulLat) {
			dstNdviTiff.SetGeoTransform(new double[] { ulLon, PIXEL_SIZE, 0, ulLat, 0, -PIXEL_SIZE });
			SpatialReference srs = new SpatialReference();
			srs.SetWellKnownGeogCS("WGS84");
			dstNdviTiff.SetProjection(srs.ExportToWkt());
			Band bandNdvi = dstNdviTiff.GetRasterBand(1);
			return bandNdvi;
		}
	}

	public static void createTiff(String csvFile, String tifFilePrefix,
			int maskWidth, int maskHeight) throws IOException,
			FileNotFoundException {
		gdal.AllRegister();
		
		Double latMax = -360.;
		Double lonMin = +360.;
		Integer initialI = null;
		Integer initialJ = null;
		
		LineIterator lineIterator = IOUtils.lineIterator(new FileInputStream(
				csvFile), Charsets.UTF_8);
		while (lineIterator.hasNext()) {
			String line = (String) lineIterator.next();
			String[] lineSplit = line.split(",");
			if (initialI == null && initialJ == null) {
				initialI = Integer.parseInt(lineSplit[0]);
				initialJ = Integer.parseInt(lineSplit[1]);
			}
			Double lat = Double.parseDouble(lineSplit[2]);
			Double lon = Double.parseDouble(lineSplit[3]);
			latMax = Math.max(lat, latMax);
			lonMin = Math.min(lon, lonMin);
		}
		
		BandVariableBuilder bandVariableBuilder = new BandVariableBuilder(tifFilePrefix, new File(csvFile).getParent(), 
				maskWidth, maskHeight, lonMin, latMax, initialI, initialJ);
		List<BandVariable> vars = new LinkedList<BandVariable>();
		vars.add(bandVariableBuilder.build("ndvi", 7));
		vars.add(bandVariableBuilder.build("evi", 18));
		vars.add(bandVariableBuilder.build("iaf", 17));
		vars.add(bandVariableBuilder.build("ts", 6));
		vars.add(bandVariableBuilder.build("alpha", 9));
		vars.add(bandVariableBuilder.build("rn", 5));
		vars.add(bandVariableBuilder.build("g", 4));
		
		lineIterator = IOUtils.lineIterator(new FileInputStream(
				csvFile), Charsets.UTF_8);
		
		while (lineIterator.hasNext()) {
			String line = (String) lineIterator.next();
			String[] lineSplit = line.split(",");
			for (BandVariable var : vars) {
				var.read(lineSplit);
			}
		}
		
		for (BandVariable var : vars) {
			var.render();
		}
	}
	
}
