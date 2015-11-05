package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.model.image.Image;

public class RandomPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	@Override
	public void selectPixelsQuenteFrioCandidates(Image image) {
		this.pixelFrioCandidates = this.pixelQuenteCandidates = image.pixels();
	}

	@Override
	public void choosePixelsQuenteFrio() {
		if (!pixelFrioCandidates.isEmpty()) {
			pixelFrio = pixelFrioCandidates.get((int) (Math.random() * pixelFrioCandidates.size()));
			pixelQuente = pixelQuenteCandidates.get((int) (Math.random() * pixelQuenteCandidates
					.size()));
		}
	}
}
