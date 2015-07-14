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
	
	int width();
	
	void width(int width);
	
	int height();

	void height(int height);

}
