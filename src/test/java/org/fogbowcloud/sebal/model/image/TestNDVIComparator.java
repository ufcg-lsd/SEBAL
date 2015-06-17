package org.fogbowcloud.sebal.model.image;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TestNDVIComparator {

	ImagePixel p1 = new DefaultImagePixel();
	ImagePixel p2 = new DefaultImagePixel();
	ImagePixel p3 = new DefaultImagePixel();

	@Before
	public void setUp() {
		ImagePixelOutput output1 = new ImagePixelOutput();        
		output1.setNDVI(0.1);
		p1.setOutput(output1);
		
		ImagePixelOutput output2 = new ImagePixelOutput();        
		output2.setNDVI(0.2);
		p2.setOutput(output2);
		
		ImagePixelOutput output3 = new ImagePixelOutput();        
		output3.setNDVI(0.3);
		p3.setOutput(output3);
	}
	
	@Test
	public void test1() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p1);
        pixels.add(p2);
        pixels.add(p3);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void test2() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p1);
        pixels.add(p3);
        pixels.add(p2);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void test3() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p2);
        pixels.add(p1);
        pixels.add(p3);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void test4() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p2);
        pixels.add(p3);
        pixels.add(p1);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void test5() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p3);
        pixels.add(p1);
        pixels.add(p2);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void test6() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p3);
        pixels.add(p2);
        pixels.add(p1);
    
        Collections.sort(pixels, new NDVIComparator());
        
        Assert.assertEquals(p1, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p3, pixels.get(2));
	}
	
	@Test
	public void testReverse() {		
        List<ImagePixel> pixels = new ArrayList<ImagePixel>();
        pixels.add(p2);
        pixels.add(p1);
        pixels.add(p3);
    
        Collections.sort(pixels, new NDVIComparator());
        Collections.reverse(pixels);
        
        Assert.assertEquals(p3, pixels.get(0));
        Assert.assertEquals(p2, pixels.get(1));
        Assert.assertEquals(p1, pixels.get(2));
	}
}
