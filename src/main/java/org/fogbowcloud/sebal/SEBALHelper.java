package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
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
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReader;
import org.esa.beam.dataio.landsat.geotiff.LandsatGeotiffReaderPlugin;
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
import org.fogbowcloud.sebal.wrapper.Wrapper;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
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
	
	private static Map<Integer, Integer> zoneToCentralMeridian = new HashMap<Integer, Integer>();
	
	private static final Logger LOGGER = Logger.getLogger(SEBALHelper.class);
	
	public static DefaultImage imageElevation;
	
    public static Product readProduct(String mtlFileName,
            List<BoundingBoxVertice> boundingBoxVertices) throws Exception {
        File mtlFile = new File(mtlFileName);
        LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
        LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
        return reader.readProductNodes(mtlFile, null);
    }

    public static BoundingBox calculateBoundingBox(List<BoundingBoxVertice> boudingVertices,
            Product product) throws Exception {
        List<UTMCoordinate> utmCoordinates = new ArrayList<UTMCoordinate>();
    	
        MetadataElement metadataRoot = product.getMetadataRoot();
        
		int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
				.getElemInt();
		
		int centralMeridian = findCentralMeridian(zoneNumber);
			
		for (BoundingBoxVertice boundingBoxVertice : boudingVertices) {
			utmCoordinates.add(convertLatLonToUtm(boundingBoxVertice.getLat(),
					boundingBoxVertice.getLon(), zoneNumber,
					centralMeridian));
		}
		
		LOGGER.debug("Boundingbox UTM coordinates: " + utmCoordinates);

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
        
        //TODO remove it
        LOGGER.debug("ULx=" + ULx);
        LOGGER.debug("ULy=" + ULy);
        
        LOGGER.debug("x0=" + x0);
        LOGGER.debug("y0=" + y0);
        
        LOGGER.debug("x1=" + x1);
        LOGGER.debug("y1=" + y1);

        int offsetX = (int) ((x0 - ULx) / 30);
        int offsetY = (int) ((ULy - y0) / 30);
        int w = (int) ((x1 - x0) / 30);
        int h = (int) ((y0 - y1) / 30);

        BoundingBox boundingBox = new BoundingBox(offsetX, offsetY, w, h);
        return boundingBox;
    }
    
	public static int findCentralMeridian(int zoneNumber) throws ClientProtocolException,
			IOException {
		if (zoneToCentralMeridian.get(zoneNumber) != null) {
			return zoneToCentralMeridian.get(zoneNumber);
		} else {

			CloseableHttpClient httpClient = HttpClients.createMinimal();

			HttpGet homeGet = new HttpGet("http://www.spatialreference.org/ref/epsg/327"
					+ zoneNumber + "/prettywkt/");
			HttpResponse response = httpClient.execute(homeGet);
			String responseStr = EntityUtils.toString(response.getEntity());

			StringTokenizer st1 = new StringTokenizer(responseStr, "\n");
			while (st1.hasMoreTokens()) {
				String line = st1.nextToken();
				if (line.contains("central_meridian")) {
					line = line.replaceAll(Pattern.quote("["), "");
					line = line.replaceAll(Pattern.quote("]"), "");
					StringTokenizer st2 = new StringTokenizer(line, ",");
					st2.nextToken();
					int centralMeridian = Integer.parseInt(st2.nextToken().trim());
					zoneToCentralMeridian.put(zoneNumber, centralMeridian);
					return centralMeridian;
				}
			}
		}
		throw new RuntimeException("The crentral_meridian was not found to zone number " + zoneNumber);
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

	protected static UTMCoordinate convertLatLonToUtm(double latitude, double longitude, double zoneNumber,
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
	
	public static LatLonCoordinate convertUtmToLatLon(double easting, double northing, double zoneNumber,
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

		MathTransform transform = CRS.findMathTransform(projCRS, geoCRS);

		double[] dest = new double[2];
		transform.transform(new double[] { easting, northing }, 0, dest, 0, 1);

		double longitude = dest[0];
		double latitude = dest[1];

		return new LatLonCoordinate(latitude, longitude);
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
    	pixelQuenteFrioChooser.setPixelFrioCandidates(pixelsFrio);
    	pixelQuenteFrioChooser.setPixelQuenteCandidates(pixelsQuente);
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.addAll(pixelsFrio);
        pixels.addAll(pixelsQuente);
        image.pixels(pixels);
        return image;
    }

	public static Image readPixels(Product product, int iBegin, int iFinal, int jBegin, int jFinal,
			PixelQuenteFrioChooser pixelQuenteFrioChooser, BoundingBox boundingBox,
			String fmaskFilePath) throws Exception {

        Locale.setDefault(Locale.ROOT);
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);     
        
        setImageElevation(pixelQuenteFrioChooser);
        
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

        if (boundingBox == null) {
			boundingBox = new BoundingBox(0, 0, bandAt.getRasterWidth(), bandAt.getRasterHeight());
        }
        
        int offSetX = boundingBox.getX();
        int offSetY = boundingBox.getY();
//        image.width(Math.min(iFinal, boundingBox.getW()) - iBegin);
//        image.height(Math.min(jFinal, boundingBox.getH()) - jBegin);
        
		int widthMax = Math.min(bandAt.getRasterWidth(),
				Math.min(iFinal, offSetX + boundingBox.getW()));
		int widthMin = Math.max(iBegin, offSetX);
		
		image.width(Math.max(widthMax - widthMin, 0));
		
		int heightMax = Math.min(bandAt.getRasterHeight(),
				Math.min(jFinal, offSetY + boundingBox.getH()));
		int heightMin = Math.max(jBegin, offSetY);
		
		image.height(Math.max(heightMax - heightMin, 0));
        
        LOGGER.debug("Image width is " + image.width());
        LOGGER.debug("Image height is " + image.height());
        
        double ULx = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PRODUCT_METADATA")
        		.getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData()
        		.getElemDouble();
        double ULy = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PRODUCT_METADATA")
        		.getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData()
        		.getElemDouble();

        int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
        		.getElemInt();
		
        int centralMeridian = findCentralMeridian(zoneNumber);
        
        double[] fmask = null;
        if (fmaskFilePath != null  && !fmaskFilePath.isEmpty() && image.width() > 0 && image.height() > 0){
        	LOGGER.debug("Fmask file is " + fmaskFilePath);
//        	fmask = readFmask(fmaskFilePath, Math.max(iBegin, offSetX),
//        			Math.min(iFinal, offSetX + boundingBox.getW()), Math.max(jBegin, offSetY),
//        			Math.min(jFinal, offSetY + boundingBox.getH()));
        	
			fmask = readFmask(fmaskFilePath, widthMin, widthMax, heightMin, heightMax);   
			LOGGER.debug("fmask size=" + fmask.length);
        }
		
		int maskWidth = Math.min(iFinal, offSetX + boundingBox.getW()) - Math.max(iBegin, offSetX);

		int fmaskI = 0;
//        for (int i = Math.max(iBegin, offSetX); i < Math.min(iFinal, offSetX + boundingBox.getW()); i++) {
//        	int fmaskJ = 0;
//            for (int j = Math.max(jBegin, offSetY); j < Math.min(jFinal, offSetY + boundingBox.getH()); j++) {
		for (int i = widthMin; i < widthMax; i++) {
        	int fmaskJ = 0;
            for (int j = heightMin; j < heightMax; j++) {
//            	LOGGER.debug(i + " " + j);
            	
            	DefaultImagePixel imagePixel = new DefaultImagePixel();

                double[] LArray = new double[product.getNumBands()];
                for (int k = 0; k < product.getNumBands(); k++) {
                    double L = product.getBandAt(k).getSampleFloat(i, j);
                    LArray[k] = L;
                }
                imagePixel.L(LArray);
                  
                imagePixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));
                
//                double easting = i * 30 + ULx;
//                double northing = (-1 * j * 30 + ULy);
//
//				LatLonCoordinate latLonCoordinate = convertUtmToLatLon(easting, northing,
//						zoneNumber, centralMeridian);
//                double latitude = Double.valueOf(String.format("%.10g%n",
//                      latLonCoordinate.getLat()));
//                double longitude = Double.valueOf(String.format("%.10g%n",
//                      latLonCoordinate.getLon()));
//                
                PixelPos pixelPos = new PixelPos(i, j);

                imagePixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));
                GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
                double latitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLat()));
                double longitude = Double.valueOf(String.format("%.10g%n",
                        geoPos.getLon()));
                
//                LOGGER.debug("lat diff=" + Math.abs(latitude - latitudeConv));
//                LOGGER.debug("lon diff=" + Math.abs(longitude - longitudeConv));

                Double z = elevation.z(latitude, longitude);                              
               
                imagePixel.z(z == null ? 400 : z);                                                    
                
                GeoLoc geoLoc = new GeoLoc();
                geoLoc.setI(i);
                geoLoc.setJ(j);
                geoLoc.setLat(latitude);
                geoLoc.setLon(longitude);
                imagePixel.geoLoc(geoLoc);
                                
				double Ta = station.Ta(latitude, longitude, startTime.getAsDate());
				imagePixel.Ta(Ta);

				double ux = station.ux(latitude, longitude, startTime.getAsDate());
				imagePixel.ux(ux);

				double zx = station.zx(latitude, longitude);
				imagePixel.zx(zx);

				double d = station.d(latitude, longitude);
				imagePixel.d(d);

				double hc = station.hc(latitude, longitude);
				imagePixel.hc(hc);
                
				if (fmask != null && fmask[fmaskJ * maskWidth + fmaskI] > 1) {
					imagePixel.isValid(false);
				}
				
                imagePixel.image(image);
                image.addPixel(imagePixel);
                
                fmaskJ++;
            }
            fmaskI++;
        }
        
        if (fmask != null) {
        	LOGGER.debug("FMask size=" + fmask.length);
        }
        LOGGER.debug("Pixels size=" + image.pixels().size());             
        
        return image;
    }

	private static double[] readFmask(String fmaskFilePath, int iInitial, int iFinal, int jInitial,
			int jFinal) {

		int maskWidth = iFinal - iInitial;
		int maskHeight = jFinal - jInitial;

		gdal.AllRegister();

		Dataset dst = gdal.Open(fmaskFilePath, gdalconstConstants.GA_ReadOnly);
		org.gdal.gdal.Band band = dst.GetRasterBand(1);

		double[] fmask = new double[maskWidth * maskHeight];
		band.ReadRaster(iInitial, jInitial, maskWidth, maskHeight, fmask);

		return fmask;
	}
	
	public static DefaultImagePixel readElevation(ImagePixel imagePixel) {

		DefaultImagePixel defaultImagePixel = new DefaultImagePixel();

		defaultImagePixel.z(imagePixel.z());
		GeoLoc geoLoc = new GeoLoc();

		geoLoc.setLat(imagePixel.geoLoc().getLat());
		geoLoc.setLon(imagePixel.geoLoc().getLon());

		defaultImagePixel.geoLoc(geoLoc);

		return defaultImagePixel;
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
	
	public static String getElevationFilePath(String outputDir, String mtlName, int iBegin, int iFinal,
			int jBegin, int jFinal) {
		if (mtlName == null || mtlName.isEmpty()) {
			return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin + "."
					+ jFinal + ".elevation.csv";
		}
		return outputDir + "/" + mtlName + "/" + iBegin + "." + iFinal + "." + jBegin + "."
				+ jFinal + ".elevation.csv";
	}

	public static List<BoundingBoxVertice> getVerticesFromFile(String boundingBoxFileName) throws IOException {
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		
		// TODO check number of args inside bounding box file name
		if (boundingBoxFileName != null && new File(boundingBoxFileName).exists()) {
			String boundingBoxInfo = FileUtils.readFileToString(new File(boundingBoxFileName));
			String[] boundingBoxValues = boundingBoxInfo.split(",");

			for (int i = 0; i < boundingBoxValues.length; i += 2) {
				boundingBoxVertices.add(new BoundingBoxVertice(Double
						.parseDouble(boundingBoxValues[i]), Double
						.parseDouble(boundingBoxValues[i + 1])));
			}
			if (boundingBoxVertices.size() < 3) {				
				LOGGER.debug("Invalid bounding box! Only " + boundingBoxVertices.size()
						+ " vertices set.");
			}
		} else {
			LOGGER.debug("Invalid bounding box file path: " + boundingBoxFileName);
		}
		return boundingBoxVertices;
	}
	
	public static void setImageElevation(PixelQuenteFrioChooser pixelQuenteFrioChooser) {
		imageElevation = new DefaultImage(pixelQuenteFrioChooser);
	}
	
	public static Image getImageElevation() {
		return imageElevation;
	}
}
