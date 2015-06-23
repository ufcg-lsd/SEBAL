package org.fogbowcloud.sebal;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
import org.esa.beam.framework.dataio.ProductSubsetBuilder;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.ReferencingFactoryContainer;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;

public class SEBALHelper {
	
	private static final Map<Integer, Integer> ZONE_TO_LONG_ZONE_CENTER = new HashMap<Integer, Integer>();
	
	static {
		ZONE_TO_LONG_ZONE_CENTER.put(22, -51);
		ZONE_TO_LONG_ZONE_CENTER.put(23, -45);
		ZONE_TO_LONG_ZONE_CENTER.put(24, -39);
		ZONE_TO_LONG_ZONE_CENTER.put(25, -33);
		ZONE_TO_LONG_ZONE_CENTER.put(26, -27);
	}
	
    public static Product readProduct(String mtlFileName,
            List<BoundingBoxVertice> boundingBoxVertices) throws Exception {
        File mtlFile = new File(mtlFileName);
        LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
        LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
        Product product = reader.readProductNodes(mtlFile, null);
        ProductSubsetDef productSubsetDef = null;
        Product boundedProduct = product;
        if (boundingBoxVertices != null && !boundingBoxVertices.isEmpty()) {
			BoundingBox boundingBox = calculateBoundingBox(boundingBoxVertices, product);

			productSubsetDef = new ProductSubsetDef();
			productSubsetDef.setRegion(boundingBox.getX(), boundingBox.getY(), boundingBox.getW(),
					boundingBox.getH());
			boundedProduct = reader.readProductNodes(mtlFile, productSubsetDef);
			boundedProduct = ProductSubsetBuilder.createProductSubset(boundedProduct, productSubsetDef, mtlFileName, "");
        }
        return boundedProduct;
    }

    private static BoundingBox calculateBoundingBox(List<BoundingBoxVertice> boudingVertices,
            Product product) throws Exception {
        List<UTMCoordinate> utmCoordinates = new ArrayList<UTMCoordinate>();
    	
        MetadataElement metadataRoot = product.getMetadataRoot();
		int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
				.getElemInt();

		// TODO read zone from MTL file
		for (BoundingBoxVertice boundingBoxVertice : boudingVertices) {
			utmCoordinates.add(convertLatLonToUtm(boundingBoxVertice.getLat(),
					boundingBoxVertice.getLon(), zoneNumber,
					ZONE_TO_LONG_ZONE_CENTER.get(zoneNumber)));
		}

		double x0 = getMinimunX(utmCoordinates);
		double y0 = getMaximunY(utmCoordinates);

		double x1 = getMaximunX(utmCoordinates);
		double y1 = getMinimunY(utmCoordinates);
        
        double ULx = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("PRODUCT_METADATA")
                .getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData()
                .getElemDouble();
        double ULy = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("PRODUCT_METADATA")
                .getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData()
                .getElemDouble();

        int offsetX = (int) ((x0 - ULx) / 30);
        int offsetY = (int) ((ULy - y0) / 30);
        int w = (int) ((x1 - x0) / 30);
        int h = (int) ((y0 - y1) / 30);

        BoundingBox boundingBox = new BoundingBox(offsetX, offsetY, w, h);
        return boundingBox;
    }
    
	private static double getMinimunX(List<UTMCoordinate> vertices) {
		double minimunX = vertices.get(0).getEasting(); //initializing with first value
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getEasting() < minimunX) {
				minimunX = utmCoordinate.getEasting();
			}
		}		
		return minimunX;
	}
	
	private static double getMaximunX(List<UTMCoordinate> vertices) {
		double maximunX = vertices.get(0).getEasting(); //initializing with first value
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getEasting() > maximunX) {
				maximunX = utmCoordinate.getEasting();
			}
		}		
		return maximunX;
	}
	
	private static double getMaximunY(List<UTMCoordinate> vertices) {
		double maximunY = vertices.get(0).getNorthing(); //initializing with first value
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getNorthing()> maximunY) {
				maximunY = utmCoordinate.getNorthing();
			}
		}		
		return maximunY;
	}
	
	private static double getMinimunY(List<UTMCoordinate> vertices) {
		double minimunY = vertices.get(0).getNorthing(); //initializing with first value
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getNorthing() < minimunY) {
				minimunY = utmCoordinate.getNorthing();
			}
		}		
		return minimunY;
	}

	private static UTMCoordinate convertLatLonToUtm(double latitude, double longitude, double zoneNumber,
			double utmZoneCenterLongitude) throws FactoryException, TransformException {
    	
		MathTransformFactory mtFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
		ReferencingFactoryContainer factories = new ReferencingFactoryContainer(null);

		GeographicCRS geoCRS = org.geotools.referencing.crs.DefaultGeographicCRS.WGS84;
		CartesianCS cartCS = org.geotools.referencing.cs.DefaultCartesianCS.GENERIC_2D;

		ParameterValueGroup parameters = mtFactory.getDefaultParameters("Transverse_Mercator");
		parameters.parameter("central_meridian").setValue(utmZoneCenterLongitude);
		parameters.parameter("latitude_of_origin").setValue(0.0);
		parameters.parameter("scale_factor").setValue(0.9996);
		parameters.parameter("false_easting").setValue(500000.0);
		parameters.parameter("false_northing").setValue(0.0);

		Map<String, String> properties = Collections.singletonMap("name", "WGS 84 / UTM Zone "
				+ zoneNumber);
		@SuppressWarnings("deprecation")
		ProjectedCRS projCRS = factories.createProjectedCRS(properties, geoCRS, null, parameters,
				cartCS);

		MathTransform transform = CRS.findMathTransform(geoCRS, projCRS);

		double[] dest = new double[2];
		transform.transform(new double[] { longitude, latitude }, 0, dest, 0, 1);

		int easting = (int) Math.round(dest[0]);
		int northing = (int) Math.round(dest[1]);
		
		return new UTMCoordinate(easting, northing);
    }

    public static Image readPixels(List<ImagePixel> pixels,
            ImagePixel pixelQuente, ImagePixel pixelFrio,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        image.pixels(pixels);
        image.pixelQuente(pixelQuente);
        image.pixelFrio(pixelFrio);
        return image;
    }

    public static Image readPixels(List<ImagePixel> pixelsQuente,
            List<ImagePixel> pixelsFrio,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.addAll(pixelsFrio);
        pixels.addAll(pixelsQuente);
        image.pixels(pixels);
        return image;
    }

    public static Image readPixels(Product product, int iBegin, int iFinal,
            int jBegin, int jFinal,
            PixelQuenteFrioChooser pixelQuenteFrioChooser) throws Exception {

        Locale.setDefault(Locale.ROOT);
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        Elevation elevation = new Elevation();
        WeatherStation station = new WeatherStation();

        UTC startTime = product.getStartTime();
        int day = startTime.getAsCalendar().get(Calendar.DAY_OF_YEAR);
        image.setDay(day);

        Band bandAt = product.getBandAt(0);
        bandAt.ensureRasterData();

        MetadataElement metadataRoot = product.getMetadataRoot();
        Double sunElevation = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("IMAGE_ATTRIBUTES").getAttribute("SUN_ELEVATION")
                .getData().getElemDouble();
        
        image.width(Math.min(iFinal, bandAt.getSceneRasterWidth()) - iBegin);
        image.height(Math.min(jFinal, bandAt.getSceneRasterHeight()) - jBegin);

        for (int i = iBegin; i < Math.min(iFinal, bandAt.getSceneRasterWidth()); i++) {
            for (int j = jBegin; j < Math.min(jFinal, bandAt.getSceneRasterHeight()); j++) {

                DefaultImagePixel imagePixel = new DefaultImagePixel();

                double[] LArray = new double[product.getNumBands()];
                for (int k = 0; k < product.getNumBands(); k++) {
                    double L = product.getBandAt(k).getSampleFloat(i, j);
                    LArray[k] = L;
                }
                imagePixel.L(LArray);

                PixelPos pixelPos = new PixelPos(i, j);

                imagePixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));

                // System.out.println(i + " " + j);
                
                GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
                double latitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLat()));
                double longitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLon()));
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

                geoPos = null;
            }
        }
        return image;
    }

	public static long getDaysSince1970(String mtlFilePath) throws Exception,
			ParseException {
		Product product = readProduct(mtlFilePath, null);
		MetadataElement metadataRoot = product.getMetadataRoot();
		String dateAcquiredStr = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("DATE_ACQUIRED").getData()
				.getElemString();
	
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Date date1970 = format.parse("1970-01-01");
		Date dateAcquired = format.parse(dateAcquiredStr);
	
		long daysSince1970 = (dateAcquired.getTime() - date1970.getTime()) / (24 * 60 * 60 * 1000);
		return daysSince1970;
	}

	public static String getAllPixelsFilePath(String outputDir, String mtlName, int iBegin, int iFinal,
			int jBegin, int jFinal) {
		if (mtlName == null || mtlName.isEmpty()) {
			return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin + "."
					+ jFinal + ".pixels.csv";
		}
		return outputDir + "/" + mtlName + "/" + iBegin + "." + iFinal + "." + jBegin + "."
				+ jFinal + ".pixels.csv";
	}
}
