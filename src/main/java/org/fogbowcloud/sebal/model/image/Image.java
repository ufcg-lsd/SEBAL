package org.fogbowcloud.sebal.model.image;

import java.util.List;

public interface Image {

	int getDay();
	
	List<ImagePixel> pixels();
	
	void pixels(List<ImagePixel> pixels);
	
	ImagePixel pixelFrio();
	
	void pixelFrio(ImagePixel pixelFrio);
	
	ImagePixel pixelQuente();
	
	void pixelQuente(ImagePixel pixelQuente);

	void choosePixelsQuenteFrio();
}
