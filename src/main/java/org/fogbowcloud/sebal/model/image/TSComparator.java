package org.fogbowcloud.sebal.model.image;

import java.util.Comparator;

public class TSComparator implements Comparator<ImagePixel> {

	@Override
	public int compare(ImagePixel pixel1, ImagePixel pixel2) {
		return new Double(pixel1.output().getTs()).compareTo(new Double(pixel2.output().getTs()));
	}

}
