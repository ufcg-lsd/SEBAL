package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public class PixelSample {
		
	private static final int UNDEFINED = -1;
	private List<ImagePixel> pixels;
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

	public List<ImagePixel> pixels() {
		return pixels;
	}
	
	public int getNumberOfLonPixels() {
		return maxX - minX + 1;
	}
	
	public int getNumberOfLatPixels() {
		return maxY - minY + 1;
	}
}
