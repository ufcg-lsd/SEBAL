package org.fogbowcloud.sebal.tiff;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

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

	public static void createTiff(String csvFile, String tifFilePrefix,
			int maskWidth, int maskHeight) throws IOException,
			FileNotFoundException {
		gdal.AllRegister();
		
		Driver tiffDriver = gdal.GetDriverByName("GTiff");
		String ndviTiffFile = new File(new File(csvFile).getParentFile(), tifFilePrefix + "_ndvi.tiff").getAbsolutePath();
		Dataset dstNdviTiff = tiffDriver.Create(ndviTiffFile, maskWidth, maskHeight, 1,
				gdalconstConstants.GDT_Float64);
		
		Driver bmpDriver = gdal.GetDriverByName("BMP");
		String ndviBmpFile = new File(new File(csvFile).getParentFile(), tifFilePrefix + "_ndvi.bmp").getAbsolutePath();
		Dataset dstNdviBmp = bmpDriver.Create(ndviBmpFile, maskWidth, maskHeight, 1,
				gdalconstConstants.GDT_Byte);
		
		Double xMin = +360.;
		Double yMax = -360.;
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
			Double x = Double.parseDouble(lineSplit[2]);
			Double y = Double.parseDouble(lineSplit[3]);
			xMin = Math.min(x, xMin);
			yMax = Math.max(y, yMax);
		}
		
		Band bandNdviTiff = createBand(dstNdviTiff, xMin, yMax);
		Band bandNdviBmp = createBand(dstNdviBmp, xMin, yMax);
		
		double[] rasterNdvi = new double[maskHeight * maskWidth];
		double[] rasterNdvi255 = new double[maskHeight * maskWidth];
		
		lineIterator = IOUtils.lineIterator(new FileInputStream(
				csvFile), Charsets.UTF_8);
		
		while (lineIterator.hasNext()) {
			String line = (String) lineIterator.next();
			String[] lineSplit = line.split(",");
			
			int i = Integer.parseInt(lineSplit[0]);
			int j = Integer.parseInt(lineSplit[1]);
			int iIdx = i - initialI;
			int jIdx = j - initialJ;
			double ndvi = Double.parseDouble(lineSplit[7]);
			rasterNdvi[jIdx * maskWidth + iIdx] = ndvi;
			rasterNdvi255[jIdx * maskWidth + iIdx] = ndvi * 255;
		}
		
		bandNdviTiff.WriteRaster(0, 0, maskWidth, maskHeight, rasterNdvi);
		bandNdviTiff.FlushCache();
		
		bandNdviBmp.WriteRaster(0, 0, maskWidth, maskHeight, rasterNdvi255);
		bandNdviBmp.FlushCache();
	}

	private static Band createBand(Dataset dstNdviTiff, Double xMin, Double yMax) {
		dstNdviTiff.SetGeoTransform(new double[] { xMin, PIXEL_SIZE, 0, yMax, 0, -PIXEL_SIZE });
		
		SpatialReference srs = new SpatialReference();
		srs.SetWellKnownGeogCS("WGS84");
		dstNdviTiff.SetProjection(srs.ExportToWkt());
		
		Band bandNdvi = dstNdviTiff.GetRasterBand(1);
		return bandNdvi;
	}
}
