package org.fogbowcloud.sebal;

import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public abstract class AbstractPixelQuenteFrioChooser implements PixelQuenteFrioChooser{
    protected List<ImagePixel> pixels;
    protected ImagePixel pixelQuente;
    protected ImagePixel pixelFrio;
    
    public List<ImagePixel> getPixels() {
        return pixels;
    }
    
    public void setPixels(List<ImagePixel> pixels) {
        this.pixels = pixels;
    }
    
    public ImagePixel getPixelQuente() {
        return pixelQuente;
    }
    
    public void setPixelQuente(ImagePixel pixelQuente) {
        this.pixelQuente = pixelQuente;
    }
    
    public ImagePixel getPixelFrio() {
        return pixelFrio;
    }
    
    public void setPixelFrio(ImagePixel pixelFrio) {
        this.pixelFrio = pixelFrio;
    }
}
