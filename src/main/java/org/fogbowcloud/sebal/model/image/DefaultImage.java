package org.fogbowcloud.sebal.model.image;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.PixelQuenteFrioChooser;

public class DefaultImage implements Image {

	private List<ImagePixel> pixels = new ArrayList<ImagePixel>(1000000);
	private int day;
	private int width;
	private int height;
	private List<ImagePixel> pixelQuenteCandidates = new ArrayList<ImagePixel>();
	private List<ImagePixel> pixelFrioCandidates = new ArrayList<ImagePixel>();
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
//	   pixelQuenteFrioChooser.selectPixelsQuenteFrioCandidates(this);
	   pixelQuenteFrioChooser.choosePixelsQuenteFrio();
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

	@Override
	public void selectPixelsQuenteFrioCandidates() {
		pixelQuenteFrioChooser.selectPixelsQuenteFrioCandidates(this);
		this.pixelFrioCandidates = pixelQuenteFrioChooser.getPixelFrioCandidates();
		this.pixelQuenteCandidates = pixelQuenteFrioChooser.getPixelQuenteCandidates();
	}

	@Override
	public List<ImagePixel> pixelQuenteCandidates() {
		return pixelQuenteCandidates;
	}

	@Override
	public List<ImagePixel> pixelFrioCandidates() {
		return pixelFrioCandidates;
	}
	
	
}
