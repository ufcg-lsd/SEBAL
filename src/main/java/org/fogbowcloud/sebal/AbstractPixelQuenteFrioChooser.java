package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public abstract class AbstractPixelQuenteFrioChooser implements PixelQuenteFrioChooser{
    
	protected List<ImagePixel> pixelQuenteCandidates = new ArrayList<ImagePixel>();
	protected List<ImagePixel> pixelFrioCandidates = new ArrayList<ImagePixel>();
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

    public List<ImagePixel> getPixelQuenteCandidates() {
    	return pixelQuenteCandidates;
    }
    
    public void setPixelQuenteCandidates(List<ImagePixel> pixelQuenteCandidates) {
    	this.pixelQuenteCandidates = pixelQuenteCandidates;
    }
    
    public List<ImagePixel> getPixelFrioCandidates() {
    	return pixelFrioCandidates;
    }
    
    public void setPixelFrioCandidates(List<ImagePixel> pixelFrioCandidates) {
    	this.pixelFrioCandidates = pixelFrioCandidates;
    }
}
