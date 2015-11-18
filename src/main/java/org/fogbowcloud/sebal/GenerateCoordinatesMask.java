package org.fogbowcloud.sebal;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.model.image.BoundingBox;


public class GenerateCoordinatesMask {

	private static final Logger LOGGER = Logger.getLogger(GenerateCoordinatesMask.class);

	public static void main(String[] args) throws Exception {

		String mtlFilePath = args[0];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));
		
		String outputDir = args[1];
		int iBegin = Integer.parseInt(args[2]);
		int iFinal = Integer.parseInt(args[3]);
		int jBegin = Integer.parseInt(args[4]);
		int jFinal = Integer.parseInt(args[5]);
		
		int numberOfPartitions = Integer.parseInt(args[6]);		

		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		if (args[7] != null) {
			String boundingboxFilePath = args[7];
			LOGGER.debug("bounding box fiel path = " + boundingboxFilePath);
			boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingboxFilePath);
		}
		Locale.setDefault(Locale.ROOT);

		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);
		MetadataElement metadataRoot = product.getMetadataRoot();

		for(int index = 1; index < numberOfPartitions; index++) {
			XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(iBegin, iFinal,
					numberOfPartitions, index);
	
			Band bandAt = product.getBandAt(0);
			bandAt.ensureRasterData();
	
			BoundingBox boundingBox = getBoundingBox(boundingBoxVertices, product, bandAt);
	
			int widthMax = Math.min(bandAt.getRasterWidth(),
					Math.min(imagePartition.getIFinal(), boundingBox.getX() + boundingBox.getW()));
			int widthMin = Math.max(imagePartition.getIBegin(), boundingBox.getX());
	
			//See how to do this with jBegin and jFinal
			int heightMax = Math.min(bandAt.getRasterHeight(),
					Math.min(jFinal, boundingBox.getY() + boundingBox.getH()));
			int heightMin = Math.max(jBegin, boundingBox.getY());
	
			double ULx = metadataRoot.getElement("L1_METADATA_FILE").getElement("PRODUCT_METADATA")
					.getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData().getElemDouble();
			double ULy = metadataRoot.getElement("L1_METADATA_FILE").getElement("PRODUCT_METADATA")
					.getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData().getElemDouble();
	
			int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
					.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
					.getElemInt();
	
			int centralMeridian = SEBALHelper.findCentralMeridian(zoneNumber);
	
			List<Coordinates> coordinates = new ArrayList<Coordinates>();
			LOGGER.debug("widthMin=" + widthMin + " and widthMax=" + widthMax);
			LOGGER.debug("heightMin=" + heightMin + " and heightMax=" + heightMax);
			for (int i = widthMin; i < widthMax; i++) {
				for (int j = heightMin; j < heightMax; j++) {
	
					double easting = i * 30 + ULx;
					double northing = (-1 * j * 30 + ULy);
	
					LatLonCoordinate latLonCoordinate = SEBALHelper.convertUtmToLatLon(easting,
							northing, zoneNumber, centralMeridian);
					double latitudeConv = Double.valueOf(String.format("%.10g%n",
							latLonCoordinate.getLat()));
					double longitudeConv = Double.valueOf(String.format("%.10g%n",
							latLonCoordinate.getLon()));
	
					coordinates.add(new Coordinates(i, j, longitudeConv, latitudeConv));
				}
			}
	
			LOGGER.debug("coordinates size: " + coordinates.size());
			
			if (outputDir == null || outputDir.isEmpty()) {
	    		outputDir = mtlName;
	    	} else {
	    		if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
	    			new File(outputDir).mkdirs();
	    		}
	    		outputDir = outputDir + "/" + mtlName + "_coord_mask_" + index + "_" + numberOfPartitions;
	    	}
			
	    	saveCoordinatesInFile(outputDir, coordinates);
		}
	}

	private static void saveCoordinatesInFile(String outputDir,
			List<Coordinates> coordinates) {
		long now = System.currentTimeMillis();

        File outputFile = new File(outputDir);
        StringBuilder sb = new StringBuilder();
        for (Coordinates coordinate : coordinates) {
			String resultLine = coordinate.getI() + "," + coordinate.getJ() + ","
					+ coordinate.getLat() + "," + coordinate.getLon() + "\n";
        	sb.append(resultLine);
        }
        try {
            FileUtils.write(outputFile, sb.toString().trim());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
		System.out.println("Saving process output time=" + (System.currentTimeMillis() - now));
	}

	private static BoundingBox getBoundingBox(List<BoundingBoxVertice> boundingBoxVertices,
			Product product, Band bandAt) throws Exception {
		BoundingBox boundingBox = null;
		if (boundingBoxVertices.size() > 3) {
			boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
		} else {
			boundingBox = new BoundingBox(0, 0, bandAt.getRasterWidth(), bandAt.getRasterHeight());
		}
		return boundingBox;
	}
	
	static class Coordinates {		
		int i;
		int j;
		double lon;
		double lat;
		
		public Coordinates(int i, int j, double lon, double lat) {
			this.i = i;
			this.j = j;
			this.lon = lon;
			this.lat = lat;
		}

		
		public int getI() {
			return i;
		}


		public int getJ() {
			return j;
		}

		public double getLon() {
			return lon;
		}

		public double getLat() {
			return lat;
		}
	}
}
