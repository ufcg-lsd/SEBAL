package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.NDVIComparator;
import org.fogbowcloud.sebal.model.image.TSComparator;

public class ClusteredPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	@Override
	public void choosePixelsQuenteFrio(Image image) {
		boolean isPixelFrioChoosen = choosePixelFrioInTheWater(image);
		
		int clusterWidth = 7;
		int clusterHeight = 7;
		List<ImagePixel> pixelFrioCandidates = new ArrayList<ImagePixel>();		
		List<ImagePixel> pixelQuenteCandidates = new ArrayList<ImagePixel>();

		int currentIndex = 0;
		int numberOfHorizontalClusters = image.width() / clusterWidth;
		int lastWidthCluster = image.width() - numberOfHorizontalClusters * clusterWidth;
		int numberOfVerticalClusters = image.height() / clusterHeight;
		int lastHeightCluster = image.height() - numberOfVerticalClusters * clusterHeight;
		int proccessedVerticalClusters = 0;
		
		while (proccessedVerticalClusters < numberOfVerticalClusters) {
			for (int i = 0; i < numberOfHorizontalClusters; i++) {
				List<ImagePixel> cluster = createCluster(image.pixels(), currentIndex,
						image.width(), clusterWidth, clusterHeight);
				proccessCluster(isPixelFrioChoosen, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
				currentIndex += clusterWidth;
			}
			
			if (lastWidthCluster > 0) {
				List<ImagePixel> cluster = createCluster(image.pixels(), currentIndex, image.width(),
						lastWidthCluster, clusterHeight);
				proccessCluster(isPixelFrioChoosen, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
			}
			
			proccessedVerticalClusters++;
			currentIndex += proccessedVerticalClusters * (clusterWidth * image.width());
		}
		
		if (lastHeightCluster > 0) {
			for (int i = 0; i < numberOfHorizontalClusters; i++) {
				List<ImagePixel> cluster = createCluster(image.pixels(), currentIndex, image.width(),
						clusterWidth, lastHeightCluster);
				proccessCluster(isPixelFrioChoosen, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
				
				currentIndex += clusterWidth;
			}
			
			if (lastWidthCluster > 0) {
				List<ImagePixel> cluster = createCluster(image.pixels(), currentIndex, image.width(),
						lastWidthCluster, lastHeightCluster);
				proccessCluster(isPixelFrioChoosen, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
			}
		}
				
		if (!isPixelFrioChoosen) {
			selectPixelFrio(pixelFrioCandidates);
		}		
		selectPixelQuente(pixelQuenteCandidates);		
	}

	private void proccessCluster(boolean isPixelFrioChoosen, List<ImagePixel> pixelFrioCandidates,
			List<ImagePixel> pixelQuenteCandidates, List<ImagePixel> cluster) {
		List<Double> cvsForNDVI = calcCVsForNDVI(cluster);
		List<Double> cvsForTS = calcCVsForTS(cluster);
		
		List<ImagePixel> preCandidatesFrio = new ArrayList<ImagePixel>();
		List<ImagePixel> preCandidatesQuente = new ArrayList<ImagePixel>();
		
		// selecting pixels with CVs smaller than 0.15 for both: NDVI and TS
		for (int index = 0; index < cluster.size(); index++) {
			if (cvsForNDVI.get(index) < 0.15 && cvsForTS.get(index) < 0.15) {
				preCandidatesFrio.add(cluster.get(index));
				preCandidatesQuente.add(cluster.get(index));
			}
		}
		
		if (!isPixelFrioChoosen) {
			/*
			 * Choosing pixel frio out of the water 
			 * Pixel Frio candidates: 5% biggest NDVI and 20% smallest TS
			 */
			preCandidatesFrio = filterBiggestNDVI(preCandidatesFrio, 5);
			preCandidatesFrio = filterSmallestTS(preCandidatesFrio, 20);
			pixelFrioCandidates.addAll(preCandidatesFrio);
		}
		
		/*
		 * Pixel Quente Candidates: 10% smallest NDVI and 20% biggest TS
		 */
		preCandidatesQuente = filterSmallestNDVI(preCandidatesQuente, 10);
		preCandidatesQuente = filterBiggestTS(preCandidatesQuente, 20);
		pixelQuenteCandidates.addAll(preCandidatesQuente);
	}

	private List<Double> calcCVsForTS(List<ImagePixel> cluster) {
		double[] tsValues = new double[cluster.size()];
		for (int index = 0; index < cluster.size(); index++) {
			tsValues[index] = cluster.get(index).output().getTs();
		}
		double mean = calcMean(tsValues);

		List<Double> cvsForTS = new ArrayList<Double>();
		for (ImagePixel pixel : cluster) {
			cvsForTS.add(Math.sqrt(Math.pow(pixel.output().getTs() - mean, 2) / mean) / mean);
		}
		return cvsForTS;
	}

	private List<Double> calcCVsForNDVI(List<ImagePixel> cluster) {
		double[] ndviValues = new double[cluster.size()];
		for (int index = 0; index < cluster.size(); index++) {
			ndviValues[index] = cluster.get(index).output().getNDVI();
		}
		double mean = calcMean(ndviValues);

		List<Double> cvsForNDVI = new ArrayList<Double>();
		for (ImagePixel pixel : cluster) {
			cvsForNDVI.add(Math.sqrt(Math.pow(pixel.output().getNDVI() - mean, 2) / mean) / mean);
		}
		return cvsForNDVI;
	}

	private boolean choosePixelFrioInTheWater(Image image) {
		// TODO Auto-generated method stub
		return false;
	}

	private void selectPixelQuente(List<ImagePixel> pixelQuenteCandidates) {
		double[] tsValuesQuenteCandidates = new double[pixelQuenteCandidates.size()];
		for (int i = 0; i < pixelQuenteCandidates.size(); i++) {
			tsValuesQuenteCandidates[i] = pixelQuenteCandidates.get(i).output().getTs();
		}
		double tsQuenteMean = calcMean(tsValuesQuenteCandidates);
		
		for (ImagePixel pixel : pixelQuenteCandidates) {
			if (pixel.output().getTs() >= (tsQuenteMean - 0.2)
					&& pixel.output().getTs() <= (tsQuenteMean + 0.2)){
				pixelQuente = pixel;
				break;
			}
		}
	}

	private void selectPixelFrio(List<ImagePixel> pixelFrioCandidates) {
		double[] tsValuesFrioCandidates = new double[pixelFrioCandidates.size()];
		double[] alphaValuesFrioCandidates = new double[pixelFrioCandidates.size()];
		for (int i = 0; i < pixelFrioCandidates.size(); i++) {
			tsValuesFrioCandidates[i] = pixelFrioCandidates.get(i).output().getTs();
			alphaValuesFrioCandidates[i] = pixelFrioCandidates.get(i).output().getAlpha();
		}
		double tsFrioMean = calcMean(tsValuesFrioCandidates);
		double alphaFrioMean = calcMean(alphaValuesFrioCandidates);
		
		for (ImagePixel pixel : pixelFrioCandidates) {
			if (pixel.output().getTs() >= (tsFrioMean - 0.2)
					&& pixel.output().getTs() <= (tsFrioMean + 0.2)
					&& pixel.output().getAlpha() >= (alphaFrioMean - 0.2)
					&& pixel.output().getAlpha() <= (alphaFrioMean + 0.2)) {
				pixelFrio = pixel;
				break;
			}
		}
	}
	
	protected List<ImagePixel> filterBiggestTS(List<ImagePixel> pixels, double percent) {
		Collections.sort(pixels, new TSComparator());
		Collections.reverse(pixels);
		List<ImagePixel> percentBiggestTS = new ArrayList<ImagePixel>();
		for (int index = 0; index < Math.round(pixels.size() * (percent / 100) + 0.4); index++) {
			percentBiggestTS.add(pixels.get(index));
		}
		return percentBiggestTS;
	}

	protected List<ImagePixel> filterSmallestNDVI(List<ImagePixel> pixels, double percent) {
		Collections.sort(pixels, new NDVIComparator());
		List<ImagePixel> percentSmallestNDVI = new ArrayList<ImagePixel>();
		for (int index = 0; index < Math.round(pixels.size() * (percent / 100) + 0.4); index++) {
			percentSmallestNDVI.add(pixels.get(index));
		}
		return percentSmallestNDVI;
	}

	protected List<ImagePixel> filterSmallestTS(List<ImagePixel> pixels, double percent) {
		Collections.sort(pixels, new TSComparator());
		List<ImagePixel> percentSmallestTS = new ArrayList<ImagePixel>();
		for (int index = 0; index < Math.round(pixels.size() * (percent / 100) + 0.4); index++) {
			percentSmallestTS.add(pixels.get(index));
		}
		return percentSmallestTS;
	}

	protected List<ImagePixel> filterBiggestNDVI(List<ImagePixel> pixels, double percent) {
		Collections.sort(pixels, new NDVIComparator());
		Collections.reverse(pixels);
		List<ImagePixel> percentBiggestNDVI = new ArrayList<ImagePixel>();
		for (int index = 0; index < Math.round(pixels.size() * (percent / 100) + 0.4); index++) {
			percentBiggestNDVI.add(pixels.get(index));
		}
		return percentBiggestNDVI;
	}

	private double calcMean(double[] values) {
		double total = 0d;		
		for (int i = 0; i < values.length; i++) {
			total += values[i];
		}
		return total / values.length;
	}

	protected List<ImagePixel> createCluster(List<ImagePixel> pixels, int firstIndex, int imageWidth,
			int clusterWidth, int clusterHeight) {
		List<ImagePixel> cluster = new ArrayList<ImagePixel>();
		for (int i = firstIndex; i < firstIndex + clusterWidth; i++) {
			for (int j = 0; j < clusterHeight; j++) {
				cluster.add(pixels.get(i + j * imageWidth));
			}
		}
		return cluster;
	}
}
