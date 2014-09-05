package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;

public class SEBALHelper {
	
	public static Product readProduct(String mtlFile) throws IOException {
		File file = new File(mtlFile);
		LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
		LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
		Product product = reader.readProductNodes(file, null);
		
		return product;
	}
	
	public static Image readPixels(List<ImagePixel> pixels, ImagePixel pixelQuente, ImagePixel pixelFrio) { 
		DefaultImage image = new DefaultImage();
		image.pixels(pixels);
		image.pixelQuente(pixelQuente);
		image.pixelFrio(pixelFrio);
		return image;
	}
	
	public static Image readPixels(List<ImagePixel> pixelsQuente, List<ImagePixel> pixelsFrio) { 
		DefaultImage image = new DefaultImage();
		List<ImagePixel> pixels = new ArrayList<ImagePixel>();
		pixels.addAll(pixelsFrio);
		pixels.addAll(pixelsQuente);
		image.pixels(pixels);
		return image;
	}
	
	public static Image readPixels(Product product, int iBegin, int iFinal,
			int jBegin, int jFinal) throws Exception {
		
		Locale.setDefault(Locale.ROOT);
		DefaultImage image = new DefaultImage();
		Elevation elevation = new Elevation();
		WeatherStation station = new WeatherStation();

		UTC startTime = product.getStartTime();
		int day = startTime.getAsCalendar().get(Calendar.DAY_OF_YEAR);
		image.setDay(day);

		Band bandAt = product.getBandAt(0);
		bandAt.ensureRasterData();

		MetadataElement metadataRoot = product.getMetadataRoot();
		Double sunElevation = metadataRoot.getElement("L1_METADATA_FILE").getElement(
				"IMAGE_ATTRIBUTES").getAttribute("SUN_ELEVATION").getData().getElemDouble();
		
		for (int i = 0; i < bandAt.getSceneRasterWidth(); i++) {
			for (int j = 0; j < bandAt.getSceneRasterHeight(); j++) {
				
				if ((i < iBegin || i > iFinal) || (j < jBegin || j > jFinal)) {
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

//				System.out.println(i + " " + j);
				
				GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
				double latitude = Double.valueOf(String.format("%.10g%n", geoPos.getLat()));
				double longitude = Double.valueOf(String.format("%.10g%n",geoPos.getLon()));
				Double z = elevation.z(latitude, longitude);
				imagePixel.z(z == null ? 400 : z);
				
				GeoLoc geoLoc = new GeoLoc();
				geoLoc.setI(i);
				geoLoc.setJ(j);
				geoLoc.setLat(latitude);
				geoLoc.setLon(longitude);
				imagePixel.geoLoc(geoLoc);
				
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
				
				double hc = station.hc(geoPos.getLat(), geoPos.getLon());
				imagePixel.hc(hc);
				
				imagePixel.image(image);

				image.addPixel(imagePixel);
			}
		}
		
		return image;
	}

}
