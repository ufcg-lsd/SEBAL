package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		
		for (int x0 = 0; x0 < image.width(); x0 += clusterWidth) {
			for (int y0 = 0; y0 < image.height(); y0 += image.height()) {
				List<ImagePixel> cluster = createCluster(image.pixels(),
						getIndex(x0, y0, image.width()), image.width(),
						Math.min(clusterWidth, image.width() - x0),
						Math.min(clusterHeight, image.height() - y0));

				proccessCluster(isPixelFrioChoosen, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
			}
		}
				
		if (!isPixelFrioChoosen) {
			selectPixelFrio(pixelFrioCandidates);
		}		
		selectPixelQuente(pixelQuenteCandidates);		
	}

	private int getIndex(int x, int y, int width) {
		return x + y * width;
	}

	private void proccessCluster(boolean isPixelFrioChoosen, List<ImagePixel> pixelFrioCandidates,
			List<ImagePixel> pixelQuenteCandidates, List<ImagePixel> cluster) {
		
		List<ImagePixel> preCandidatesFrio = new ArrayList<ImagePixel>();
		List<ImagePixel> preCandidatesQuente = new ArrayList<ImagePixel>();
		
		List<Double> cvsForNDVI = calcCVsForNDVI(cluster);
		List<Double> cvsForTS = calcCVsForTS(cluster);
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
		Map<String, PixelSample> samples = findWater(image);
		if (samples.isEmpty()) {
			return false;
		}
		
		
		
		return true;
	}

	private Map<String, PixelSample> findWater(Image image) {
		Map<String, PixelSample> samples = new HashMap<String, PixelSample>();
		List<ImagePixel> pixels = image.pixels();
		boolean[] visited = new boolean[pixels.size()];
		
		for (int i = 0; i < image.width(); i++) {
			for (int j = 0; j < image.height(); j++) {
				if (visited[getIndex(i, j, image.width())] == true) {
					continue;
				}
				visited[getIndex(i, j, image.width())] = true;
				if (pixels.get(getIndex(i, j, image.width())).output().getNDVI() < 0) {
					findWater(pixels, visited, image.width(), i, j, 0, 0, i + "_" + j, samples);
				}
			}
		}
		
		/*
		 *  refine map
		 *  eliminate not valid samples
		 */
		
		return samples;
	}

	private void findWater(List<ImagePixel> pixels, boolean[] visited, int i, int j, int width,
			int numberOfHorizontalPixel, int numberOfVerticalPixel, String sampleId, Map<String, PixelSample> samples) {
		if (visited[getIndex(i, j, width)] == true) {
			return;
		}
		visited[getIndex(i, j, width)] = true;
		if (pixels.get(getIndex(i, j, width)).output().getNDVI() < 0) {
			if (!samples.containsKey(sampleId)) {
				samples.put(sampleId, new PixelSample());
			}
			samples.get(sampleId).addPixel(pixels.get(getIndex(i, j, width)));
			samples.get(sampleId).updateHorizontalPixels(numberOfHorizontalPixel);
			samples.get(sampleId).updateVerticalPixels(numberOfVerticalPixel);
		} else {
			return;
		}
		findWater(pixels, visited, i + 1, j, numberOfHorizontalPixel + 1, numberOfVerticalPixel,
				width, sampleId, samples);
		findWater(pixels, visited, i, j + 1, numberOfHorizontalPixel, numberOfVerticalPixel + 1,
				width, sampleId, samples);
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
