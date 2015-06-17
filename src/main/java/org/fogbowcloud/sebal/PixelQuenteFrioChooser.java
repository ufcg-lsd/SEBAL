package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public interface PixelQuenteFrioChooser {
    
    public void choosePixelsQuenteFrio(Image image);

    public ImagePixel getPixelQuente();

    public ImagePixel getPixelFrio();

}
