package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public class DefaultPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {
    
    @Override
    public void choosePixelsQuenteFrio(Image image) {
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
		if (pixelFrio == null && !image.pixels().isEmpty()) {
			pixelFrio = image.pixels().get((int) (Math.random() * image.pixels().size()));
		}
		if (pixelQuente == null && !image.pixels().isEmpty()) {
			pixelQuente = image.pixels().get((int) (Math.random() * image.pixels().size()));
		}
    }
}
