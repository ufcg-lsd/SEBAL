package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.fogbowcloud.sebal.wrapper.Wrapper;

public class LookForStationDataMain {

	private static final Logger LOGGER = Logger.getLogger(Wrapper.class);

	public static void main(String[] args) throws Exception {
		long now = System.currentTimeMillis();

		String mtlFilePath = args[0];

		String outputDir = args[1];

		Product product = SEBALHelper.readProduct(mtlFilePath,
				null);

		now = System.currentTimeMillis();

		LOGGER.debug("Preparing metadata time="
				+ (System.currentTimeMillis() - now));

		now = System.currentTimeMillis();

		String stationData = getStationData(product);

		LOGGER.debug("Getting station data time="
				+ (System.currentTimeMillis() - now));

		now = System.currentTimeMillis();

		saveWeatherStationInfo(stationData, mtlFilePath, outputDir, 0, 8000, 0,
				8000);

		LOGGER.debug("Saving station data output time="
				+ (System.currentTimeMillis() - now));
	}

	public static String getStationData(Product product) throws URISyntaxException, HttpException,
			IOException {

		Locale.setDefault(Locale.ROOT);

		Band bandAt = product.getBandAt(0);
		bandAt.ensureRasterData();

		int i = bandAt.getRasterWidth() / 2 ;
		int j = bandAt.getRasterHeight() / 2 ;

		PixelPos pixelPos = new PixelPos(i, j);
		GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
		double latitude = Double.valueOf(String.format("%.10g%n",
				geoPos.getLat()));
		double longitude = Double.valueOf(String.format("%.10g%n",
				geoPos.getLon()));

		WeatherStation station = new WeatherStation();
		UTC startTime = product.getStartTime();
		return station.getStationData(latitude, longitude,
				startTime.getAsDate());
	}

	private static void saveWeatherStationInfo(String stationData,
			String mtlFilePath, String outputDir, int iBegin, int iFinal,
			int jBegin, int jFinal) {
		long now = System.currentTimeMillis();
		String weatherPixelsFileName = getWeatherFileName(mtlFilePath,
				outputDir, iBegin, iFinal, jBegin, jFinal);

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

	private static String getWeatherFileName(String mtlFilePath,
			String outputDir, int iBegin, int iFinal, int jBegin, int jFinal) {
		String fileName = new File(mtlFilePath).getName();
		String imageFileName = fileName.substring(0, fileName.indexOf("_"));
		return SEBALHelper.getWeatherFilePath(outputDir, "", imageFileName,
				iBegin, iFinal, jBegin, jFinal);
	}
}
