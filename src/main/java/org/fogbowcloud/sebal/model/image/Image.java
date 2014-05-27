package org.fogbowcloud.sebal.model.image;

import java.util.List;

public interface Image {

	int getDay();
	
	List<ImagePixel> pixels();
	
	ImagePixel pixelFrio();
	
	ImagePixel pixelQuente();

	void choosePixelsQuenteFrio();
}
