package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public abstract class AbstractPixelQuenteFrioChooser implements PixelQuenteFrioChooser{
    
    protected ImagePixel pixelQuente;
    protected ImagePixel pixelFrio;
    
    public ImagePixel getPixelQuente() {
        return pixelQuente;
    }
    
    protected void setPixelQuente(ImagePixel pixelQuente) {
        this.pixelQuente = pixelQuente;
    }
    
    public ImagePixel getPixelFrio() {
        return pixelFrio;
    }
    
    protected void setPixelFrio(ImagePixel pixelFrio) {
        this.pixelFrio = pixelFrio;
    }
}
