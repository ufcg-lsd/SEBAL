package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.Image;

public class RandomPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	@Override
	public void choosePixelsQuenteFrio(Image image) {
		if (!image.pixels().isEmpty()) {
			pixelFrio = image.pixels().get((int) (Math.random() * image.pixels().size()));
			pixelQuente = image.pixels().get((int) (Math.random() * image.pixels().size()));
		}
	}
}
