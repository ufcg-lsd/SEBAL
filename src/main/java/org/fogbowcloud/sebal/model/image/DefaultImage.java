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
//		for (ImagePixel pixel : pixels) {
//			double ndvi = pixel.output().getNDVI();
//			if (ndvi >= 0.1 && ndvi <= 0.2) {
//				this.pixelQuente = pixel;
//			}
//			if (ndvi <= -0.1 && ndvi >= -0.2) {
//				this.pixelFrio = pixel;
//			}
//			if (pixelFrio != null && pixelQuente != null) {
//				break;
//			}
//		}
//		if (pixelFrio == null) {
//			pixelFrio = pixels.get((int) (Math.random() * pixels.size()));
//		}
//		if (pixelQuente == null) {
//			pixelQuente = pixels.get((int) (Math.random() * pixels.size()));
//		}
		DefaultImagePixel pixelQuenteLocal = new DefaultImagePixel();
		pixelQuenteLocal.ux(4.388);
		pixelQuenteLocal.zx(6.);
		pixelQuenteLocal.hc(4.);
		pixelQuenteLocal.d(4.0* (2./3.));
		
		ImagePixelOutput outputQuente = new ImagePixelOutput();
		outputQuente.setG(89.352632);
		outputQuente.setRn(449.55188);
		outputQuente.setSAVI(0.148563);
		outputQuente.setTs(35.8928340 + 273.15);
		pixelQuenteLocal.setOutput(outputQuente);
		
		DefaultImagePixel pixelFrioLocal = new DefaultImagePixel();
		ImagePixelOutput outputFrio = new ImagePixelOutput();
		outputFrio.setTs(26.4577440 + 273.15);
		pixelFrioLocal.setOutput(outputFrio);
		
		this.pixelQuente = pixelQuenteLocal;
		this.pixelFrio = pixelFrioLocal;
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
