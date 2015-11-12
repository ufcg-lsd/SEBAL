package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.render.RenderHelper;
import org.junit.Test;

public class TestRender {
	
	private String mtlFilePath;
	private int leftX;
	private int rightX;
	private int upperY;
	
	private int numberOfPartitions;
	private int partitionIndex;
	
	public TestRender(String[] args) {
		mtlFilePath = "/home/esdras/Documentos/Fogbow/Estudo/SEBAL/Image/LT52310622011243CUB00_MTL.txt";
		
		leftX = Integer.parseInt(args[0]);
		rightX = Integer.parseInt(args[1]);
		upperY = Integer.parseInt(args[2]);
		
		numberOfPartitions = Integer.parseInt(args[3]);
		partitionIndex = Integer.parseInt(args[4]);
	}
	
	@Test
	public void renderTest() throws Exception {
		
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
		
		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(leftX, rightX,
				numberOfPartitions, partitionIndex);
			
		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);
		
		new RenderHelper();
		RenderHelper.calculateLatLon(product, imagePartition.getIFinal(), upperY);
	}

}