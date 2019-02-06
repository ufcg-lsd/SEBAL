package org.fogbowcloud.sebal;

import org.junit.Assert;
import org.junit.Test;

public class TestBulkHelper {

	@Test
	public void testCalcXPartitionInOnePartition() {
		Assert.assertEquals(0, BulkHelper.calcXInterval(0, 0, 1));
		Assert.assertEquals(0, BulkHelper.calcXInterval(10, 10, 1));
		Assert.assertEquals(1, BulkHelper.calcXInterval(10, 11, 1));
		Assert.assertEquals(10, BulkHelper.calcXInterval(10, 20, 1));
		Assert.assertEquals(15, BulkHelper.calcXInterval(10, 25, 1));
		Assert.assertEquals(100, BulkHelper.calcXInterval(100, 200, 1));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCalcXPartitionWithInvalidArgs1() {
		Assert.assertEquals(0, BulkHelper.calcXInterval(0, 0, 2));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testCalcXPartitionWithInvalidArgs2() {
		Assert.assertEquals(0, BulkHelper.calcXInterval(10, 10, 5));
	}
	
	@Test
	public void testCalcXPartitionInMoreThanOnePartition() {
		Assert.assertEquals(0, BulkHelper.calcXInterval(10, 11, 2));
		Assert.assertEquals(2, BulkHelper.calcXInterval(10, 20, 5));
		Assert.assertEquals(1, BulkHelper.calcXInterval(10, 25, 8));
		Assert.assertEquals(50, BulkHelper.calcXInterval(100, 200, 2));
		Assert.assertEquals(33, BulkHelper.calcXInterval(100, 200, 3));
	}
	
	@Test
	public void testGetSelectedXPartitionInterval2() {
		Assert.assertEquals(2, BulkHelper.calcXInterval(10, 20, 5));
		XPartitionInterval selectePartition = BulkHelper.getSelectedPartition(10, 20, 5, 1);
		Assert.assertEquals(10, selectePartition.getIBegin());
		Assert.assertEquals(12, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 20, 5, 2);
		Assert.assertEquals(12, selectePartition.getIBegin());
		Assert.assertEquals(14, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 20, 5, 3);
		Assert.assertEquals(14, selectePartition.getIBegin());
		Assert.assertEquals(16, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 20, 5, 4);
		Assert.assertEquals(16, selectePartition.getIBegin());
		Assert.assertEquals(18, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 20, 5, 5);
		Assert.assertEquals(18, selectePartition.getIBegin());
		Assert.assertEquals(20, selectePartition.getIFinal());
	}
	
	@Test
	public void testGetSelectedXPartitionInterval3() {
		Assert.assertEquals(1, BulkHelper.calcXInterval(10, 25, 8));
		XPartitionInterval selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 1);
		Assert.assertEquals(10, selectePartition.getIBegin());
		Assert.assertEquals(11, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 2);
		Assert.assertEquals(11, selectePartition.getIBegin());
		Assert.assertEquals(12, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 3);
		Assert.assertEquals(12, selectePartition.getIBegin());
		Assert.assertEquals(13, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 4);
		Assert.assertEquals(13, selectePartition.getIBegin());
		Assert.assertEquals(14, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 5);
		Assert.assertEquals(14, selectePartition.getIBegin());
		Assert.assertEquals(15, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 6);
		Assert.assertEquals(15, selectePartition.getIBegin());
		Assert.assertEquals(16, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 7);
		Assert.assertEquals(16, selectePartition.getIBegin());
		Assert.assertEquals(17, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(10, 25, 8, 8);
		Assert.assertEquals(17, selectePartition.getIBegin());
		Assert.assertEquals(25, selectePartition.getIFinal());
	}

	@Test
	public void testGetSelectedXPartitionInterval4() {
		Assert.assertEquals(50, BulkHelper.calcXInterval(100, 200, 2));
		XPartitionInterval selectePartition = BulkHelper.getSelectedPartition(100, 200, 2, 1);
		Assert.assertEquals(100, selectePartition.getIBegin());
		Assert.assertEquals(150, selectePartition.getIFinal());
		
		selectePartition = BulkHelper.getSelectedPartition(100, 200, 2, 2);
		Assert.assertEquals(150, selectePartition.getIBegin());
		Assert.assertEquals(200, selectePartition.getIFinal());
	}
	
	@Test
	public void testGetSelectedXPartitionInterval5() {
		Assert.assertEquals(33, BulkHelper.calcXInterval(100, 200, 3));		
		XPartitionInterval selectePartition = BulkHelper.getSelectedPartition(100, 200, 3, 1);
		Assert.assertEquals(100, selectePartition.getIBegin());
		Assert.assertEquals(133, selectePartition.getIFinal());
				
		selectePartition = BulkHelper.getSelectedPartition(100, 200, 3, 2);
		Assert.assertEquals(133, selectePartition.getIBegin());
		Assert.assertEquals(166, selectePartition.getIFinal());

		selectePartition = BulkHelper.getSelectedPartition(100, 200, 3, 3);
		Assert.assertEquals(166, selectePartition.getIBegin());
		Assert.assertEquals(200, selectePartition.getIFinal());
	}
}
