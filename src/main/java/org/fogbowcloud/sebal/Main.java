package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;

public class Main {

	static String MTL_FILE = "13520010515/LT52150652001135CUB00_MTL.txt";

	public static void main(String[] args) throws Exception {
		File file = new File(MTL_FILE);
		LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
		LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
		Product product = reader.readProductNodes(file, null);
//		readPixels(product);

		long begin = System.currentTimeMillis();
		try {
			Image image = readPixels(product);
			SEBAL sebal = new SEBAL();
			sebal.run(new JSONSatellite("landsat5"), image);
		} catch (Exception e) {
			// TODO: handle exception
		}
		System.out.println(System.currentTimeMillis() - begin);
	}

	private static Image readPixels(Product product) throws Exception {

		DefaultImage image = new DefaultImage();
		Elevation elevation = new Elevation();
		WeatherStation station = new WeatherStation();

		UTC startTime = product.getStartTime();
		int day = startTime.getAsCalendar().get(Calendar.DAY_OF_YEAR);
		image.setDay(day);

		Band bandAt = product.getBandAt(0);
		bandAt.ensureRasterData();

		Double sunElevation = null;
		List<String> mtlLines = IOUtils
				.readLines(new FileInputStream(MTL_FILE));
		for (String mtlLine : mtlLines) {
			if (mtlLine.contains("SUN_ELEVATION")) {
				sunElevation = Double.valueOf(mtlLine.split("=")[1].trim());
			}
		}

		for (int i = 0; i < bandAt.getSceneRasterWidth(); i++) {
			for (int j = 0; j < bandAt.getSceneRasterHeight(); j++) {

				if (i != 1260 || j != 1160) {
					continue;
				}

				DefaultImagePixel imagePixel = new DefaultImagePixel();

				double[] LArray = new double[product.getNumBands()];
				for (int k = 0; k < product.getNumBands(); k++) {
					double L = product.getBandAt(k).getSampleFloat(i, j);
					LArray[k] = L;
				}
				imagePixel.L(LArray);

				PixelPos pixelPos = new PixelPos(i, j);

				imagePixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));

				GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);

				double z = elevation.z(Double.valueOf(geoPos.getLat()),
						Double.valueOf(geoPos.getLon()));
				imagePixel.z(z);

				double Ta = station.Ta(geoPos.getLat(), geoPos.getLon(),
						startTime.getAsDate());
				imagePixel.Ta(Ta);

				double ux = station.ux(geoPos.getLat(), geoPos.getLon(),
						startTime.getAsDate());
				imagePixel.ux(ux);

				double zx = station.zx(geoPos.getLat(), geoPos.getLon());
				imagePixel.zx(zx);

				double d = station.d(geoPos.getLat(), geoPos.getLon());
				imagePixel.d(d);

				imagePixel.image(image);

				image.addPixel(imagePixel);
			}
		}

		image.choosePixelsQuenteFrio();
		return image;
	}
}
