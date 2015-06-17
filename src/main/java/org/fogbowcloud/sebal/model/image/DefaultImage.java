package org.fogbowcloud.sebal.model.image;

import java.util.LinkedList;
import java.util.List;

import org.fogbowcloud.sebal.PixelQuenteFrioChooser;

public class DefaultImage implements Image {

	private List<ImagePixel> pixels = new LinkedList<ImagePixel>();
	private int day;
	private int width;
	private int height;
	private ImagePixel pixelQuente;
	private ImagePixel pixelFrio;
	private PixelQuenteFrioChooser pixelQuenteFrioChooser;
	
	public DefaultImage(PixelQuenteFrioChooser pixelQuenteFrioChooser) {
	    this.pixelQuenteFrioChooser = pixelQuenteFrioChooser;
    }

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
	   pixelQuenteFrioChooser.choosePixelsQuenteFrio(this);
	   this.pixelFrio = pixelQuenteFrioChooser.getPixelFrio();
       this.pixelQuente = pixelQuenteFrioChooser.getPixelQuente();
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

	@Override
	public void pixels(List<ImagePixel> pixels) {
		this.pixels = pixels;
	}

	@Override
	public void pixelFrio(ImagePixel pixelFrio) {
		this.pixelFrio = pixelFrio;
	}

	@Override
	public void pixelQuente(ImagePixel pixelQuente) {
		this.pixelQuente = pixelQuente;
	}

	public void width(int width) {
		this.width = width;		
	}

	public void height(int height) {
		this.height = height;		
	}
	
	public int width() {
		return this.width;		
	}

	public int height() {
		return height;		
	}
}
