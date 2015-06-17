package org.fogbowcloud.sebal.model.image;

import java.util.Comparator;

public class NDVIComparator implements Comparator<ImagePixel> {
	
	@Override
	public int compare(ImagePixel pixel1, ImagePixel pixel2) {
		return new Double(pixel1.output().getNDVI())
				.compareTo(new Double(pixel2.output().getNDVI()));
	}
}
