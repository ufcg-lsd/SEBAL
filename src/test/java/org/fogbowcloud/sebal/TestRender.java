package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.render.RenderHelper;
import org.junit.Test;

public class TestRender {
	
	private String mtlFilePath;
	
	public TestRender() {
		mtlFilePath = "/home/esdras/Documentos/Fogbow/Estudo/SEBAL/Image/LT52310622011243CUB00_MTL.txt";
	}
	
	@Test
	public void renderTest() throws Exception {
		List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
			
		Product product = SEBALHelper.readProduct(mtlFilePath, boundingBoxVertices);
		
		new RenderHelper();
		RenderHelper.calculatePixelSize(product);
	}

}