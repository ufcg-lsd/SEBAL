package org.fogbowcloud.sebal.model.image;

import java.util.LinkedList;
import java.util.List;

public class DefaultImage implements Image {

	private List<ImagePixel> pixels = new LinkedList<ImagePixel>();
	private int day;
	
	@Override
	public int getDay() {
		return day;
	}

	@Override
	public List<ImagePixel> pixels() {
		return pixels;
	}
	
	public void addPixel(ImagePixel pixel) {
		pixels.add(pixel);
	}

	public void choosePixelsQuenteFrio() {
		
	}
	
	@Override
	public ImagePixel pixelFrio() {
		return null;
	}

	@Override
	public ImagePixel pixelQuente() {
		return null;
	}

	public void setDay(int day) {
		this.day = day;
	}

}
