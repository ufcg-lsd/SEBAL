package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public class DefaultPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {
    
    @Override
    public void choosePixelsQuenteFrio(Image image) {
    	long now = System.currentTimeMillis();
        for (ImagePixel pixel : image.pixels()) {
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
        System.out.println("Choosing pixel quente frio time=" + (System.currentTimeMillis() - now));
		if (pixelFrio == null && !image.pixels().isEmpty()) {
			pixelFrio = image.pixels().get((int) (Math.random() * image.pixels().size()));
		}
		if (pixelQuente == null && !image.pixels().isEmpty()) {
			pixelQuente = image.pixels().get((int) (Math.random() * image.pixels().size()));
		}
		if (pixelFrio != null) {
			System.out.println("PixelFrio: " + pixelFrio.output().getTs());
		}
		if (pixelQuente != null) {
			System.out.println("PixelQuente: " + pixelQuente.output().getTs());
		}
    }
}
