package org.fogbowcloud.sebal.tiff;

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

		Driver driver = gdal.GetDriverByName("GTiff");
		String ndviFile = tifFilePrefix + "ndvi.tif";
		Dataset dstNdvi = driver.Create(ndviFile, maskWidth, maskHeight, 1,
				gdalconstConstants.GDT_Float64);
		
		Double xMin = Double.MAX_VALUE;
		Double yMax = Double.MIN_VALUE;
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
			double x = Double.parseDouble(lineSplit[2]);
			double y = Double.parseDouble(lineSplit[3]);
			xMin = Math.min(x, xMin);
			yMax = Math.max(y, yMax);
		}
		
		dstNdvi.SetGeoTransform(new double[] { xMin, PIXEL_SIZE, 0, yMax, 0, -PIXEL_SIZE });
		
		SpatialReference srs = new SpatialReference();
		srs.SetWellKnownGeogCS("WGS84");
		dstNdvi.SetProjection(srs.ExportToWkt());
		
		Band bandNdvi = dstNdvi.GetRasterBand(1);
		double[] rasterNdvi = new double[maskHeight * maskWidth];
		
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
		}
		
		bandNdvi.WriteRaster(0, 0, maskWidth, maskHeight, rasterNdvi);
		
	}
}
