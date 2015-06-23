package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.NDVIComparator;
import org.fogbowcloud.sebal.model.image.TSComparator;

public class ClusteredPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	@Override
	public void choosePixelsQuenteFrio(Image image) {
		boolean isPixelFrioInTheWater = checkIfPixelFrioInTheWater(image);
		
		int clusterWidth = 7;
		int clusterHeight = 7;
		List<ImagePixel> pixelFrioCandidates = new ArrayList<ImagePixel>();		
		List<ImagePixel> pixelQuenteCandidates = new ArrayList<ImagePixel>();
		
		for (int x0 = 0; x0 < image.width(); x0 += clusterWidth) {
			for (int y0 = 0; y0 < image.height(); y0 += image.height()) {
				List<ImagePixel> cluster = createCluster(image.pixels(),
						linear(x0, y0, image.width()), image.width(),
						Math.min(clusterWidth, image.width() - x0),
						Math.min(clusterHeight, image.height() - y0));

				proccessCluster(isPixelFrioInTheWater, pixelFrioCandidates, pixelQuenteCandidates,
						cluster);
			}
		}
				
		if (!isPixelFrioInTheWater) {
			selectPixelFrioOutOfWater(pixelFrioCandidates);
		}		
		selectPixelQuente(pixelQuenteCandidates);		
	}

	private int linear(int x, int y, int width) {
		return x + y * width;
	}

	private void proccessCluster(boolean isPixelFrioChoosen, List<ImagePixel> pixelFrioCandidates,
			List<ImagePixel> pixelQuenteCandidates, List<ImagePixel> cluster) {
		
		List<ImagePixel> preCandidatesFrio = new ArrayList<ImagePixel>();
		List<ImagePixel> preCandidatesQuente = new ArrayList<ImagePixel>();
		
		double CVForNDVI = calcCVForNDVI(cluster);
		double CVForTS = calcCVForTS(cluster);
		
		if (CVForNDVI < 0.15 && CVForTS < 0.15) {
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
	}

	private double calcCVForTS(List<ImagePixel> cluster) {
		double[] tsValues = new double[cluster.size()];
		for (int index = 0; index < cluster.size(); index++) {
			tsValues[index] = cluster.get(index).output().getTs();
		}
		double mean = calcMean(tsValues);
		double variance = new Variance().evaluate(tsValues);
		double standarDeviation = Math.sqrt(variance);
		
		return standarDeviation / mean;
	}

	private double calcCVForNDVI(List<ImagePixel> cluster) {
		double[] ndviValues = new double[cluster.size()];
		for (int index = 0; index < cluster.size(); index++) {
			ndviValues[index] = cluster.get(index).output().getNDVI();
		}
		double mean = calcMean(ndviValues);
		double variance = new Variance().evaluate(ndviValues);
		double standarDeviation = Math.sqrt(variance);
		
		return standarDeviation / mean;
	}

	private boolean checkIfPixelFrioInTheWater(Image image) {		
		Map<String, PixelSample> waterSamples = findWater(image);
		if (waterSamples.isEmpty()) {
			return false;
		}		
		refineSamples(waterSamples);		
		PixelSample bestSample = selectBestSample(waterSamples);
		
		if (bestSample == null) {
			return false;
		}
		return selectPixelFrioInTheWater(bestSample);
	}

	private boolean selectPixelFrioInTheWater(PixelSample waterSample) {
		double[] tsValues = new double[waterSample.pixels().size()];
		for (int index = 0; index < waterSample.pixels().size(); index++) {
			tsValues[index] = waterSample.pixels().get(index).output().getTs();
		}
		double mean = calcMean(tsValues);
				
		for (ImagePixel pixel : waterSample.pixels()) {
			if (pixel.output().getTs() >= (mean - 0.2)
					&& pixel.output().getTs() <= (mean + 0.2)) {
				pixelFrio = pixel;
				return true;
			}
		}
		return false;
	}

	private void refineSamples(Map<String, PixelSample> waterSamples) {
		int B = 50;
		int D = 5;
		
		//TODO check these rules with John
		Collection<String> keys = new ArrayList<String>(waterSamples.keySet()); 
		for (String key : keys) {
			PixelSample pixelSample = waterSamples.get(key);
			if (pixelSample.pixels().size() < B || pixelSample.getNumberOfLonPixels() < D
					|| pixelSample.getNumberOfLatPixels() < D) {
				waterSamples.remove(key);
			}
			
		}
	}

	protected PixelSample selectBestSample(Map<String, PixelSample> samples) {
		PixelSample bestSample = null;
		for (PixelSample sample : samples.values()) {
			if (bestSample == null || (sample.pixels().size() > bestSample.pixels().size())) {
				bestSample = sample;
			}
		}		
		return bestSample;
	}

	protected Map<String, PixelSample> findWater(Image image) {
		Map<String, PixelSample> samples = new HashMap<String, PixelSample>();
		List<ImagePixel> pixels = image.pixels();
		boolean[] visited = new boolean[pixels.size()];
		
		for (int i = 0; i < image.width(); i++) {
			for (int j = 0; j < image.height(); j++) {
				if (visited[linear(i, j, image.width())] == true) {
					continue;
				}

				if (pixels.get(linear(i, j, image.width())).output().getNDVI() < 0) {
					findWater(pixels, visited, i, j, image.width(), image.height(), i + "_" + j,
							samples);
				}
			}
		}
		return samples;
	}

	private void findWater(List<ImagePixel> pixels, boolean[] visited, int i, int j, int width,
			int height, String sampleId, Map<String, PixelSample> samples) {
		if (i < 0 || i > width -1 || j < 0 || j > height -1) {
			return;
		}
		
		if (visited[linear(i, j, width)] == true) {
			return;
		}
		visited[linear(i, j, width)] = true;

		if (pixels.get(linear(i, j, width)).output().getNDVI() < 0) {
			if (!samples.containsKey(sampleId)) {				
				samples.put(sampleId, new PixelSample());
			}
			samples.get(sampleId).addPixel(pixels.get(linear(i, j, width)), i, j);
		} else {
			return;
		}
		
		findWater(pixels, visited, i + 1, j, width, height, sampleId, samples);
		findWater(pixels, visited, i - 1, j, width, height, sampleId, samples);
		findWater(pixels, visited, i, j + 1, width, height, sampleId, samples);
		findWater(pixels, visited, i, j - 1, width, height, sampleId, samples);
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

	private void selectPixelFrioOutOfWater(List<ImagePixel> pixelFrioCandidates) {
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
