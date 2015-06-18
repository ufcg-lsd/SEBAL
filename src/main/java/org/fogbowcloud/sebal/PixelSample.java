package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public class PixelSample {
		
	private static final int UNDEFINED = -1;
	private List<ImagePixel> pixels;
//	private int horizontalPixels;
//	private int verticalPixels;
	private int minX = UNDEFINED;
	private int maxX = UNDEFINED;
	private int minY = UNDEFINED;
	private int maxY = UNDEFINED;
	
	public PixelSample() {
		this.pixels = new ArrayList<ImagePixel>();	
	}

	public void addPixel(ImagePixel imagePixel, int x, int y) {
		pixels.add(imagePixel);
		updateX(x);
		updateY(y);
	}

	private void updateY(int y) {
		if (minY == UNDEFINED && maxY == UNDEFINED) {
			minY = maxY = y;
		} else if (y < minY) {
			minY = y;
		} else if (y > maxY) {
			maxY = y;
		}			
	}

	private void updateX(int x) {
		if (minX == UNDEFINED && maxX == UNDEFINED) {
			minX = maxX = x;
		} else if (x < minX) {
			minX = x;
		} else if (x > maxX) {
			maxX = x;
		}	
	}

//	public void updateHorizontalPixels(int horizontalPixel) {
//		horizontalPixels = Math.max(horizontalPixels, horizontalPixel);
//	}
//
//	public void updateVerticalPixels(int verticalPixel) {
//		verticalPixels = Math.max(verticalPixels, verticalPixel);		
//	}

	public List<ImagePixel> pixels() {
		return pixels;
	}
	
	public int getHorizontalPixels() {
		return maxX - minX + 1;
	}
	
	public int getVerticalPixels() {
		return maxY - minY + 1;
	}
}
