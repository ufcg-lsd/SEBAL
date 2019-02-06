package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
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
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.fogbowcloud.sebal.util.SEBALAppConstants;
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

	public static Product readProduct(String mtlFileName,
			List<BoundingBoxVertice> boundingBoxVertices) throws IOException {
		File mtlFile = new File(mtlFileName);
		LandsatGeotiffReaderPlugin readerPlugin = new LandsatGeotiffReaderPlugin();
		LandsatGeotiffReader reader = new LandsatGeotiffReader(readerPlugin);
		return reader.readProductNodes(mtlFile, null);
	}

	public static BoundingBox buildBoundingBox(List<BoundingBoxVertice> boudingVertices,
			Product product) throws Exception {

		List<UTMCoordinate> utmCoordinates = new ArrayList<UTMCoordinate>();

		MetadataElement metadataRoot = product.getMetadataRoot();

		int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
				.getElemInt();

		int centralMeridian = SEBALHelper.centralMeridian(zoneNumber);

		for (BoundingBoxVertice boundingBoxVertice : boudingVertices) {
			utmCoordinates.add(convertLatLonToUtm(boundingBoxVertice.getLat(),
					boundingBoxVertice.getLon(), zoneNumber, centralMeridian));
		}

		LOGGER.debug("Boundingbox UTM coordinates: " + utmCoordinates);

		double x0 = SEBALHelper.getMinimunX(utmCoordinates);
		double y0 = SEBALHelper.getMaximunY(utmCoordinates);

		double x1 = SEBALHelper.getMaximunX(utmCoordinates);
		double y1 = SEBALHelper.getMinimunY(utmCoordinates);

		double ULx = metadataRoot.getElement("L1_METADATA_FILE").getElement("PRODUCT_METADATA")
				.getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData().getElemDouble();
		double ULy = metadataRoot.getElement("L1_METADATA_FILE").getElement("PRODUCT_METADATA")
				.getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData().getElemDouble();

		int offsetX = (int) ((x0 - ULx) / 30);
		int offsetY = (int) ((ULy - y0) / 30);
		int w = (int) ((x1 - x0) / 30);
		int h = (int) ((y0 - y1) / 30);

		BoundingBox boundingBox = new BoundingBox(offsetX, offsetY, w, h);
		return boundingBox;
	}

	public static int centralMeridian(int zoneNumber) throws Exception {
		Integer result = null;

		if (zoneToCentralMeridian.get(zoneNumber) != null) {
			result = zoneToCentralMeridian.get(zoneNumber);
		} else {

			CloseableHttpClient httpClient = HttpClients.createMinimal();

			HttpGet homeGet = new HttpGet(
					"http://www.spatialreference.org/ref/epsg/327" + zoneNumber + "/prettywkt/");
			HttpResponse response = httpClient.execute(homeGet);
			String responseStr = EntityUtils.toString(response.getEntity());

			StringTokenizer tokenizedResponse = new StringTokenizer(responseStr, "\n");
			while (tokenizedResponse.hasMoreTokens()) {
				String line = tokenizedResponse.nextToken();

				if (line.contains("central_meridian")) {
					line = line.replaceAll(Pattern.quote("["), "");
					line = line.replaceAll(Pattern.quote("]"), "");

					StringTokenizer tokenizedLine = new StringTokenizer(line, ",");
					tokenizedLine.nextToken();

					int centralMeridian = Integer.parseInt(tokenizedLine.nextToken().trim());
					zoneToCentralMeridian.put(zoneNumber, centralMeridian);
					result = centralMeridian;
				}
			}
		}

		if (result == null) {
			throw new Exception(
					"The central meridian was not found to zone number [" + zoneNumber + "]");
		}
		return result;
	}

	private static double getMinimunX(List<UTMCoordinate> vertices) {
		double minimunX = vertices.get(0).getEasting(); // initializing with first value
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getEasting() < minimunX) {
				minimunX = utmCoordinate.getEasting();
			}
		}
		return minimunX;
	}

	private static double getMaximunX(List<UTMCoordinate> vertices) {
		double maximunX = vertices.get(0).getEasting();
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getEasting() > maximunX) {
				maximunX = utmCoordinate.getEasting();
			}
		}
		return maximunX;
	}

	private static double getMaximunY(List<UTMCoordinate> vertices) {
		double maximunY = vertices.get(0).getNorthing();
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getNorthing() > maximunY) {
				maximunY = utmCoordinate.getNorthing();
			}
		}
		return maximunY;
	}

	private static double getMinimunY(List<UTMCoordinate> vertices) {
		double minimunY = vertices.get(0).getNorthing();
		for (UTMCoordinate utmCoordinate : vertices) {
			if (utmCoordinate.getNorthing() < minimunY) {
				minimunY = utmCoordinate.getNorthing();
			}
		}
		return minimunY;
	}

	protected static UTMCoordinate convertLatLonToUtm(double latitude, double longitude,
			double zoneNumber, double utmZoneCenterLongitude)
			throws FactoryException, TransformException {

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

		Map<String, String> properties = Collections.singletonMap("name",
				"WGS 84 / UTM Zone " + zoneNumber);

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

	public static String getWeatherFilePath(String outputDir, String mtlName,
			String imageFileName) {
		if (mtlName == null || mtlName.isEmpty()) {
			return outputDir + File.separator + imageFileName + "_station.csv";
		} else {
			return outputDir + File.separator + mtlName + File.separator + imageFileName
					+ "_station.csv";
		}
	}

	public static List<BoundingBoxVertice> getVerticesFromFile(String boundingBoxFileName)
			throws IOException {
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();

		if (boundingBoxFileName != null && new File(boundingBoxFileName).exists()) {

			String boundingBoxInfo = FileUtils.readFileToString(new File(boundingBoxFileName),
					SEBALAppConstants.FILE_ENCODING);

			String[] boundingBoxValues = boundingBoxInfo.split(",");

			for (int i = 0; i < boundingBoxValues.length; i += 2) {
				boundingBoxVertices
						.add(new BoundingBoxVertice(Double.parseDouble(boundingBoxValues[i]),
								Double.parseDouble(boundingBoxValues[i + 1])));
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

	public static String getStationData(Properties properties, Product product, int iBegin,
			int iFinal, int jBegin, int jFinal, BoundingBox boundingBox)
			throws URISyntaxException, HttpException, IOException {

		LOGGER.info("Starting station collect...");

		Locale.setDefault(Locale.ROOT);

		Band bandAt = product.getBandAt(0);
		bandAt.ensureRasterData();

		if (boundingBox == null) {
			boundingBox = new BoundingBox(0, 0, bandAt.getRasterWidth(), bandAt.getRasterHeight());
		}

		int offSetX = boundingBox.getX();
		int offSetY = boundingBox.getY();

		int widthMax = Math.min(bandAt.getRasterWidth(),
				Math.min(iFinal, offSetX + boundingBox.getW()));
		int widthMin = Math.max(iBegin, offSetX);

		int heightMax = Math.min(bandAt.getRasterHeight(),
				Math.min(jFinal, offSetY + boundingBox.getH()));
		int heightMin = Math.max(jBegin, offSetY);

		int i = (widthMax - widthMin) / 2 + widthMin;
		int j = (heightMax - heightMin) / 2 + heightMin;

		PixelPos pixelPos = new PixelPos(i, j);
		GeoPos geoPos = bandAt.getGeoCoding().getGeoPos(pixelPos, null);
		double latitude = Double.valueOf(String.format("%.10g%n", geoPos.getLat()));
		double longitude = Double.valueOf(String.format("%.10g%n", geoPos.getLon()));

		String sceneCenterTime = SEBALHelper.getSceneCenterTime(product);

		WeatherStation station = new WeatherStation(properties);
		UTC startTime = product.getStartTime();
		return station.getStationData(latitude, longitude, startTime.getAsDate(), sceneCenterTime);
	}

	public static String getSceneCenterTime(Product product) {
		MetadataElement metadataRoot = product.getMetadataRoot();

		String sceneCenterTime = metadataRoot.getElement("L1_METADATA_FILE")
				.getElement("PRODUCT_METADATA").getAttribute("SCENE_CENTER_TIME").getData()
				.toString();

		LOGGER.info("Scene Center Time: [" + sceneCenterTime + "]");

		String[] splitedTime = sceneCenterTime.split(":");

		String parsedSceneCenterTime = splitedTime[0] + splitedTime[1];

		LOGGER.info("Parsed Scene Center Time: [" + parsedSceneCenterTime + "]");

		return parsedSceneCenterTime;
	}

}
