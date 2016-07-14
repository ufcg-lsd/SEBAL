package org.fogbowcloud.sebal;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;

public class DefaultPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	private static final Logger LOGGER = Logger.getLogger(DefaultPixelQuenteFrioChooser.class);
	
	@Override
	public void selectPixelsQuenteFrioCandidates(Image image) {
		this.pixelFrioCandidates = this.pixelQuenteCandidates = image.pixels();
	}
	
	@Override
	public void choosePixelsQuenteFrio() {
		long now = System.currentTimeMillis();
		for (ImagePixel pixel : pixelFrioCandidates) {
			double ndvi = pixel.output().getNDVI();
			if (ndvi >= 0.1 && ndvi <= 0.2) {
				this.pixelQuente = pixel;
			}
			if (ndvi <= -0.1 && ndvi >= -0.2) {
				this.pixelFrio = pixel;
			}
			if (pixelFrio != null && pixelQuente != null) {
				break;
			}
		}
		LOGGER.debug("Choosing pixel quente frio time=" + (System.currentTimeMillis() - now));
		if (pixelFrio == null && !pixelFrioCandidates.isEmpty()) {
			pixelFrio = pixelFrioCandidates.get((int) (Math.random() * pixelFrioCandidates.size()));
		}
		if (pixelQuente == null && !pixelQuenteCandidates.isEmpty()) {
			pixelQuente = pixelQuenteCandidates.get((int) (Math.random() * pixelQuenteCandidates.size()));
		}
		if (pixelFrio != null) {
			LOGGER.debug("PixelFrio: " + pixelFrio.output().getTs());
		}
		if (pixelQuente != null) {
			LOGGER.debug("PixelQuente: " + pixelQuente.output().getTs());
		}
	}
}
