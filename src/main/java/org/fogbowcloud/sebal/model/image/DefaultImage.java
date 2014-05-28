package org.fogbowcloud.sebal.model.image;

import java.util.LinkedList;
import java.util.List;

public class DefaultImage implements Image {

	private List<ImagePixel> pixels = new LinkedList<ImagePixel>();
	private int day;
	private ImagePixel pixelQuente;
	private ImagePixel pixelFrio;
	
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

	@Override
	public void choosePixelsQuenteFrio() {
		for (ImagePixel pixel : pixels) {
			double ndvi = pixel.output().getNDVI();
			if (ndvi >= 0.1 && ndvi <= 0.2) {
				this.pixelQuente = pixel;
			}
			if (ndvi <= -0.1 && ndvi >= -0.2) {
				this.pixelFrio = pixel;
			}
			if (pixelFrio != null && pixelQuente != null) {
				break;
			}
		}
	}
	
	@Override
	public ImagePixel pixelFrio() {
		return pixelFrio;
	}

	@Override
	public ImagePixel pixelQuente() {
		return pixelQuente;
	}

	public void setDay(int day) {
		this.day = day;
	}

}
