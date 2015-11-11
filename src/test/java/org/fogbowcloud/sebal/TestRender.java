package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.render.RenderHelper;
import org.junit.Test;

public class TestRender {
	
	private String mtlFilePath;
	private String outputDir;
	
	public TestRender() {
		mtlFilePath = "/home/esdras/Documentos/Fogbow/Estudo/SEBAL/Image/LT52310622011243CUB00_MTL.txt";
		outputDir = "/home/esdras/Documentos/Fogbow/Estudo/SEBAL/Output";
	}
	
	@Test
	public void renderTest() throws Exception {
		String fileName = new File(mtlFilePath).getName();
		System.out.println("FileName: " + fileName);
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		int leftX = 0;
		int lowerY = 0;
		int rightX = 0;
		int upperY = 0;

		int numberOfPartitions = 0;
		int partitionIndex = 0;
		
/*		int leftX = Integer.parseInt(args[2]);
		int lowerY = Integer.parseInt(args[3]);
		int rightX = Integer.parseInt(args[4]);
		int upperY = Integer.parseInt(args[5]);*/

/*		int numberOfPartitions = Integer.parseInt(args[6]);
		int partitionIndex = Integer.parseInt(args[7]);*/
		
		
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		
		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(leftX, rightX,
				numberOfPartitions, partitionIndex);

		String csvFilePath = SEBALHelper.getAllPixelsFilePath(outputDir, mtlName,
				imagePartition.getIBegin(), imagePartition.getIFinal(), lowerY, upperY);
		
/*		long daysSince1970 = SEBALHelper.getDaysSince1970(mtlFilePath);		
		String prefixRaw = leftX + "." + rightX + "." + lowerY + "." + upperY;*/
			
		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);
		
		BoundingBox boundingBox = null;
		if (boundingBoxVertices.size() > 3) {
			boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
		}
/*		int offSetX = boundingBox.getX();
		int offSetY = boundingBox.getY();
		
		int maskWidth = Math.min(rightX, offSetX + boundingBox.getW()) - Math.max(leftX, offSetX);
		int maskHeight = Math.min(upperY, offSetY + boundingBox.getH()) - Math.max(lowerY, offSetY);*/
		
		Double latMax = -360.;
		Double lonMin = +360.;
		Integer initialI = null;
		Integer initialJ = null;

		LineIterator lineIterator = IOUtils.lineIterator(new FileInputStream(csvFilePath),
				Charsets.UTF_8);
		
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
		
		new RenderHelper();
		RenderHelper.calculateLatLon(product, initialI, initialJ);
	}

}
