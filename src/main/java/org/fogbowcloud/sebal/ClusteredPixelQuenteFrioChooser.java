package org.fogbowcloud.sebal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.math3.stat.descriptive.moment.Variance;
import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.python.google.common.primitives.Doubles;


public class ClusteredPixelQuenteFrioChooser extends AbstractPixelQuenteFrioChooser {

	private int clusterWidth = 5;  			// 5 is default value
	private int clusterHeight = 5; 			// 5 is default value
	private double maxCVForNDVI = 0.2; 		// 20% is default value
	private int maxInvalidNDVIValues = 10;	// 10 is default value
	private int minTotalWater = 1;			// 1 is default value
	private int minLatWater = 1;			// 1 is default value
	private int minLonWater = 1;			// 1 is default value
	private double maxDiffFromTSMean = 0.2;	// 0.2 is default value
	
	private static final Logger LOGGER = Logger.getLogger(ClusteredPixelQuenteFrioChooser.class);
	
	public ClusteredPixelQuenteFrioChooser(Properties properties) {
		if (properties == null) {
			LOGGER.debug("Properties were not set.");
			return;
		}
		if (properties.getProperty("cluster_width") != null) {
			clusterWidth = Integer.parseInt(properties.getProperty("cluster_width"));
		}

		if (properties.getProperty("cluster_height") != null) {
			clusterHeight = Integer.parseInt(properties.getProperty("cluster_height"));
		}

		if (properties.getProperty("cluster_max_cv_for_ndvi") != null) {
			maxCVForNDVI = Double.parseDouble(properties.getProperty("cluster_max_cv_for_ndvi"));
		}

		if (properties.getProperty("cluster_max_invalid_ndvi") != null) {
			maxInvalidNDVIValues = Integer.parseInt(properties
					.getProperty("cluster_max_invalid_ndvi"));
		}

		if (properties.getProperty("cluster_min_total_water_pixels") != null) {
			minTotalWater = Integer.parseInt(properties
					.getProperty("cluster_min_total_water_pixels"));
		}

		if (properties.getProperty("cluster_min_lat_water_pixels") != null) {
			minLatWater = Integer.parseInt(properties.getProperty("cluster_min_lat_water_pixels"));
		}

		if (properties.getProperty("cluster_min_lon_water_pixels") != null) {
			minLonWater = Integer.parseInt(properties.getProperty("cluster_min_lon_water_pixels"));
		}

		if (properties.getProperty("cluster_max_difference_from_ts_mean") != null) {
			maxDiffFromTSMean = Double.parseDouble(properties
					.getProperty("cluster_max_difference_from_ts_mean"));
		}
	}

	@Override
	public void selectPixelsQuenteFrioCandidates(Image image) {
		LOGGER.debug("image is null? " + (image == null));
		long now = System.currentTimeMillis();
		ImagePixel pixelFrioInTheWater = findPixelFrioInTheWater(image);
		if (pixelFrioInTheWater != null) {
			LOGGER.debug("Pixel frio in the water is null.");
			pixelFrioCandidates.add(pixelFrioInTheWater);
		}
		
		LOGGER.debug("PixelFrioInTheWater execution time=" + (System.currentTimeMillis() - now));
		now = System.currentTimeMillis();
		
		for (int x0 = 0; x0 < image.width(); x0 += clusterWidth) {
			for (int y0 = 0; y0 < image.height(); y0 += clusterHeight) {
				List<ImagePixel> cluster = createCluster(image.pixels(),
						linear(x0, y0, image.width()), image.width(),
						Math.min(clusterWidth, image.width() - x0),
						Math.min(clusterHeight, image.height() - y0));

				proccessClusterAndSelectCandidates(cluster);
			}
		}
		
		LOGGER.debug("Processing clusters execution time=" + (System.currentTimeMillis() - now));
	}

	@Override
	public void choosePixelsQuenteFrio() {
		LOGGER.debug("Chossing pixels hot and cold from candidates...");
		long now = System.currentTimeMillis();
		
		//selectPixelFrio();
		//selectPixelQuente();

		if (pixelFrio != null) {
			LOGGER.debug("TS of pixel frio: " + pixelFrio.output().getTs());
		}
		if (pixelQuente != null) {
			LOGGER.debug("TS of pixel quente: " + pixelQuente.output().getTs());
		}
		
		LOGGER.debug("Chossing pixels hot and cold from candidates execution time="
				+ (System.currentTimeMillis() - now));
	}

	private int linear(int x, int y, int width) {
		return x + y * width;
	}

	private void proccessClusterAndSelectCandidates(List<ImagePixel> cluster) {

		double CVForNDVI = calcCVForNDVI(cluster);

		if (CVForNDVI < maxCVForNDVI) {
			List<ImagePixel> validPixels = removeCloud(cluster);
			LOGGER.debug("Adding " + validPixels.size() + " valid pixels in quente/frio candidates.");
			pixelFrioCandidates.addAll(validPixels);
			pixelQuenteCandidates.addAll(validPixels);
		}
	}
	
	private List<ImagePixel> removeCloud(List<ImagePixel> cluster) {
		List<ImagePixel> validPixels = new ArrayList<ImagePixel>();

		for (int index = 0; index < cluster.size(); index++) {
			ImagePixelOutput pixelOutput = cluster.get(index).output();
			if (!pixelOutput.isCloud() && pixelOutput.getNDVI() > 0) {
				validPixels.add(cluster.get(index));
			}
		}
		return validPixels;
	}

	private double calcCVForNDVI(List<ImagePixel> cluster) {
		List<Double> validNDVIValues = new ArrayList<Double>();
		int invalidNDVIValues = 0;
		for (int index = 0; index < cluster.size(); index++) {
			ImagePixelOutput pixelOutput = cluster.get(index).output();
			if (pixelOutput.isCloud() || pixelOutput.getNDVI() <= 0) {
				invalidNDVIValues++;
				if (invalidNDVIValues == maxInvalidNDVIValues) {
					return 1;
				}
				continue;
			}
			validNDVIValues.add(cluster.get(index).output().getNDVI());
		}
		
		double mean = calcMean(Doubles.toArray(validNDVIValues));
		double variance = new Variance().evaluate(Doubles.toArray(validNDVIValues));
		double standarDeviation = Math.sqrt(variance);
		
		return standarDeviation / mean;
	}

	private ImagePixel findPixelFrioInTheWater(Image image) {		
		Map<String, PixelSample> waterSamples = findWater(image);
		if (waterSamples.isEmpty()) {
			return null;
		}		
		refineSamples(waterSamples);		
		PixelSample bestSample = selectBestSample(waterSamples);
		
		if (bestSample == null) {
			return null;
		}
		return selectPixelFrioInTheWater(bestSample);
	}

	private ImagePixel selectPixelFrioInTheWater(PixelSample waterSample) {	
		double[] tsValues = new double[waterSample.pixels().size()];
		for (int index = 0; index < waterSample.pixels().size(); index++) {
			tsValues[index] = waterSample.pixels().get(index).output().getTs();
		}
		double mean = calcMean(tsValues);
	
		for (ImagePixel pixel : waterSample.pixels()) {
			if (pixel.output().getTs() >= (mean - maxDiffFromTSMean)
					&& pixel.output().getTs() <= (mean + maxDiffFromTSMean)) {
				return pixel;
			}
		}
		return null;
	}

	private void refineSamples(Map<String, PixelSample> waterSamples) {
		Collection<String> keys = new ArrayList<String>(waterSamples.keySet());
		for (String key : keys) {
			PixelSample pixelSample = waterSamples.get(key);
			if (pixelSample.pixels().size() < minTotalWater
					|| pixelSample.getNumberOfLonPixels() < minLonWater
					|| pixelSample.getNumberOfLatPixels() < minLatWater) {
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
		LOGGER.debug("pixels size=" + pixels.size());
		boolean[] visited = new boolean[pixels.size()];
		
		for (int i = 0; i < image.width(); i++) {
			for (int j = 0; j < image.height(); j++) {
				if (visited[linear(i, j, image.width())] == true) {
					continue;
				}

				ImagePixelOutput pixelOutput = pixels.get(linear(i, j, image.width())).output();
				if (!pixelOutput.isCloud() && pixelOutput.getWaterTest()) {
					findWater(pixels, visited, i, j, image.width(), image.height(), i + "_" + j,
							samples);
				}
			}
		}
		LOGGER.debug("number of water samples=" + samples.size());
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
