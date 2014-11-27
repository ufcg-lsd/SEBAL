package org.fogbowcloud.sebal;

import java.util.List;

import org.fogbowcloud.sebal.model.image.ImagePixel;

public interface PixelQuenteFrioChooser {
    
    public void choosePixelsQuenteFrio(List<ImagePixel> pixels);

    public ImagePixel getPixelQuente();

    public ImagePixel getPixelFrio();

}
