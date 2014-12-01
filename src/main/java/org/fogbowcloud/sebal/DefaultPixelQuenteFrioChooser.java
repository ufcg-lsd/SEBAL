package org.fogbowcloud.sebal;

import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public class DefaultPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {
    
    @Override
    public void choosePixelsQuenteFrio(List<ImagePixel> pixels) {
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
        if (pixelFrio == null && !pixels.isEmpty()) {
            pixelFrio = pixels.get((int) (Math.random() * pixels.size()));
        }
        if (pixelQuente == null && !pixels.isEmpty()) {
            pixelQuente = pixels.get((int) (Math.random() * pixels.size()));
        }
    }
}
