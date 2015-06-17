package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestClusteredPixelQuenteFrioChooser {

	static final int IMAGE_WIDTH = 50;
	static final int IMAGE_HEIGHT = 50;
	static final List<ImagePixel> IMAGE_PIXELS = new ArrayList<ImagePixel>();
	
	static List<ImagePixel> pixels = new ArrayList<ImagePixel>();	
	static ImagePixel p1 = new DefaultImagePixel();
	static ImagePixel p2 = new DefaultImagePixel();
	static ImagePixel p3 = new DefaultImagePixel();
	static ImagePixel p4 = new DefaultImagePixel();
	
	@BeforeClass
	public static void setUpClass(){
		for (int i = 1; i <= IMAGE_WIDTH; i++) {
			for (int j = 0; j < IMAGE_HEIGHT; j++) {
				DefaultImagePixel pixel = new DefaultImagePixel();
				pixel.z(i + j * IMAGE_WIDTH);
				IMAGE_PIXELS.add(pixel);
			}
		}
		
		for (int i = 0; i < IMAGE_PIXELS.size(); i++) {
			System.out.println("index=" + i + " ----- value=" + IMAGE_PIXELS.get(i).z());
		}

		ImagePixelOutput output1 = new ImagePixelOutput();        
		output1.setNDVI(0.1);
		output1.setTs(0.1);
		p1.setOutput(output1);
		
		ImagePixelOutput output2 = new ImagePixelOutput();        
		output2.setNDVI(0.2);
		output2.setTs(0.2);
		p2.setOutput(output2);
		
		ImagePixelOutput output3 = new ImagePixelOutput();        
		output3.setNDVI(0.3);
		output3.setTs(0.3);
		p3.setOutput(output3);
		
		ImagePixelOutput output4 = new ImagePixelOutput();        
		output4.setNDVI(0.4);
		output4.setTs(0.4);
		p4.setOutput(output4);
		
		pixels.add(p1);
		pixels.add(p2);
		pixels.add(p3);
		pixels.add(p4);
	}
	
	@Test
	public void testClusterSameSizeOfImage() {
		ClusteredPixelQuenteFrioChooser chooser = new ClusteredPixelQuenteFrioChooser();		
		List<ImagePixel> cluster = chooser.createCluster(IMAGE_PIXELS, 0, IMAGE_WIDTH, IMAGE_WIDTH, IMAGE_HEIGHT);
		
		Assert.assertEquals(IMAGE_PIXELS.size(), cluster.size());
		for (ImagePixel imagePixel : cluster) {
			Assert.assertTrue(IMAGE_PIXELS.contains(imagePixel));		
		}
	}
		
	@Test
	public void testCluster7x7InUpLeftBorder() {
		List<ImagePixel> expectedCluster = createExpectedCluster(0, 7);

		ClusteredPixelQuenteFrioChooser chooser = new ClusteredPixelQuenteFrioChooser();
		List<ImagePixel> obtainedCluster = chooser.createCluster(IMAGE_PIXELS, 0, IMAGE_WIDTH, 7, 7);

		Assert.assertEquals(expectedCluster.size(), obtainedCluster.size());
		for (ImagePixel imagePixel : expectedCluster) {
			Assert.assertTrue(obtainedCluster.contains(imagePixel));
		}
	}
	
	@Test
	public void testCluster7x7InUpMiddleBorder() {
		List<ImagePixel> expectedCluster = createExpectedCluster(20 * IMAGE_HEIGHT, 7);

		ClusteredPixelQuenteFrioChooser chooser = new ClusteredPixelQuenteFrioChooser();
		List<ImagePixel> obtainedCluster = chooser.createCluster(IMAGE_PIXELS, 20 * IMAGE_HEIGHT,
				IMAGE_WIDTH, 7, 7);

		Assert.assertEquals(expectedCluster.size(), obtainedCluster.size());
		for (ImagePixel imagePixel : expectedCluster) {
			Assert.assertTrue(obtainedCluster.contains(imagePixel));
		}
	}
	
	@Test
	public void testCluster7x7InTheMiddleOfImage() {
		List<ImagePixel> expectedCluster = createExpectedCluster(20 * IMAGE_HEIGHT + 20, 7);

		ClusteredPixelQuenteFrioChooser chooser = new ClusteredPixelQuenteFrioChooser();
		List<ImagePixel> obtainedCluster = chooser.createCluster(IMAGE_PIXELS, 20 * IMAGE_HEIGHT + 20,
				IMAGE_WIDTH, 7, 7);

		Assert.assertEquals(expectedCluster.size(), obtainedCluster.size());
		for (ImagePixel imagePixel : expectedCluster) {
			Assert.assertTrue(obtainedCluster.contains(imagePixel));
		}
	}

	private List<ImagePixel> createExpectedCluster(int firstIndex, int clusterWidth) {
		List<ImagePixel> expectedCluster = new ArrayList<ImagePixel>();		
		for (int i = firstIndex; i < firstIndex + clusterWidth; i++) {
			for (int j = 0; j < clusterWidth; j++) {
				expectedCluster.add(IMAGE_PIXELS.get(i + j * IMAGE_WIDTH));
			}
		}
		
		for (ImagePixel imagePixel : expectedCluster) {
			System.out.println(imagePixel.z());		
		}

		return expectedCluster;
	}
	
	@Test
	public void testFilterPercentSmallestNDVI() {
		ClusteredPixelQuenteFrioChooser cluster = new ClusteredPixelQuenteFrioChooser();

		// asserting 80% smallest NDVI
		List<ImagePixel> filteredByNDVI = cluster.filterSmallestNDVI(pixels, 80);
		Assert.assertEquals(4, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		Assert.assertTrue(filteredByNDVI.contains(p2));
		Assert.assertTrue(filteredByNDVI.contains(p3));
		Assert.assertTrue(filteredByNDVI.contains(p4));
		
		// asserting 75% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 75);
		Assert.assertEquals(3, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		Assert.assertTrue(filteredByNDVI.contains(p2));
		Assert.assertTrue(filteredByNDVI.contains(p3));

		// asserting 60% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 60);
		Assert.assertEquals(3, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		Assert.assertTrue(filteredByNDVI.contains(p2));
		Assert.assertTrue(filteredByNDVI.contains(p3));		
		
		// asserting 50% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 50);
		Assert.assertEquals(2, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		Assert.assertTrue(filteredByNDVI.contains(p2));

		// asserting 25% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 25);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));

		// asserting 20% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 20);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		
		// asserting 10% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 10);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));

		// asserting 5% smallest NDVI
		filteredByNDVI = cluster.filterSmallestNDVI(pixels, 5);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
	}
	
	
	@Test
	public void testFilterPercentBiggestNDVI() {
		ClusteredPixelQuenteFrioChooser cluster = new ClusteredPixelQuenteFrioChooser();

		// asserting 80% biggest NDVI
		List<ImagePixel> filteredByNDVI = cluster.filterBiggestNDVI(pixels, 80);
		Assert.assertEquals(4, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p1));
		Assert.assertTrue(filteredByNDVI.contains(p2));
		Assert.assertTrue(filteredByNDVI.contains(p3));
		Assert.assertTrue(filteredByNDVI.contains(p4));
		
		// asserting 75% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 75);
		Assert.assertEquals(3, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));
		Assert.assertTrue(filteredByNDVI.contains(p3));
		Assert.assertTrue(filteredByNDVI.contains(p2));

		// asserting 60% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 60);
		Assert.assertEquals(3, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));
		Assert.assertTrue(filteredByNDVI.contains(p3));
		Assert.assertTrue(filteredByNDVI.contains(p2));		
		
		// asserting 50% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 50);
		Assert.assertEquals(2, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));
		Assert.assertTrue(filteredByNDVI.contains(p3));

		// asserting 25% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 25);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));

		// asserting 20% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 20);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));
		
		// asserting 10% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 10);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));

		// asserting 5% biggest NDVI
		filteredByNDVI = cluster.filterBiggestNDVI(pixels, 5);
		Assert.assertEquals(1, filteredByNDVI.size());
		Assert.assertTrue(filteredByNDVI.contains(p4));
	}
	
	@Test
	public void testFilterPercentBiggestTS() {
		ClusteredPixelQuenteFrioChooser cluster = new ClusteredPixelQuenteFrioChooser();

		// asserting 80% biggest TS
		List<ImagePixel> filteredByTS = cluster.filterBiggestTS(pixels, 80);
		Assert.assertEquals(4, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		Assert.assertTrue(filteredByTS.contains(p2));
		Assert.assertTrue(filteredByTS.contains(p3));
		Assert.assertTrue(filteredByTS.contains(p4));
		
		// asserting 75% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 75);
		Assert.assertEquals(3, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));
		Assert.assertTrue(filteredByTS.contains(p3));
		Assert.assertTrue(filteredByTS.contains(p2));

		// asserting 60% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 60);
		Assert.assertEquals(3, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));
		Assert.assertTrue(filteredByTS.contains(p3));
		Assert.assertTrue(filteredByTS.contains(p2));		
		
		// asserting 50% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 50);
		Assert.assertEquals(2, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));
		Assert.assertTrue(filteredByTS.contains(p3));

		// asserting 25% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 25);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));

		// asserting 20% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 20);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));
		
		// asserting 10% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 10);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));

		// asserting 5% biggest TS
		filteredByTS = cluster.filterBiggestTS(pixels, 5);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p4));
	}
	
	@Test
	public void testFilterPercentSmallestTS() {
		ClusteredPixelQuenteFrioChooser cluster = new ClusteredPixelQuenteFrioChooser();

		// asserting 80% smallest TS
		List<ImagePixel> filteredByTS = cluster.filterSmallestTS(pixels, 80);
		Assert.assertEquals(4, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		Assert.assertTrue(filteredByTS.contains(p2));
		Assert.assertTrue(filteredByTS.contains(p3));
		Assert.assertTrue(filteredByTS.contains(p4));
		
		// asserting 75% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 75);
		Assert.assertEquals(3, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		Assert.assertTrue(filteredByTS.contains(p2));
		Assert.assertTrue(filteredByTS.contains(p3));

		// asserting 60% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 60);
		Assert.assertEquals(3, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		Assert.assertTrue(filteredByTS.contains(p2));
		Assert.assertTrue(filteredByTS.contains(p3));		
		
		// asserting 50% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 50);
		Assert.assertEquals(2, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		Assert.assertTrue(filteredByTS.contains(p2));

		// asserting 25% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 25);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));

		// asserting 20% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 20);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
		
		// asserting 10% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 10);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));

		// asserting 5% smallest TS
		filteredByTS = cluster.filterSmallestTS(pixels, 5);
		Assert.assertEquals(1, filteredByTS.size());
		Assert.assertTrue(filteredByTS.contains(p1));
	}

}
