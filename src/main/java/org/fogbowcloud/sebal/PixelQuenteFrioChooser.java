package org.fogbowcloud.sebal;

import java.util.List;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public interface PixelQuenteFrioChooser {
    
	public void selectPixelsQuenteFrioCandidates(Image image);
	
    public void choosePixelsQuenteFrio();

    public ImagePixel getPixelQuente();

    public ImagePixel getPixelFrio();
    
    public List<ImagePixel> getPixelQuenteCandidates();

    public List<ImagePixel> getPixelFrioCandidates();
    
    public void setPixelQuenteCandidates(List<ImagePixel> pixelQuenteCandidates);

    public void setPixelFrioCandidates(List<ImagePixel> pixelFrioCandidates);

}
