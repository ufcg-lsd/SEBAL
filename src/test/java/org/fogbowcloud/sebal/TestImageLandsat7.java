package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.wrapper.Wrapper;
import org.junit.Test;

public class TestImageLandsat7 {
	
	private Properties properties;
	private static final Logger LOGGER = Logger.getLogger(Wrapper.class);
	private PixelQuenteFrioChooser pixelQuenteFrioChooser;
	private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
	Wrapper wrapper;
	TestImageHelper imageHelper;
	String desiredValuesFile = "result.csv";
	
	public TestImageLandsat7() throws IOException {
		properties = new Properties();
		imageHelper = new TestImageHelper();
		
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(properties.getProperty("bounding_box_file_path"));
		
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
	}
	
	@Test
	public void acceptanceTest(String outputDir, String boundingBoxFileName, Properties properties,
			String fmaskFilePath) throws Exception {
		
		LOGGER.info("Executing F1 phase...");
		long now = System.currentTimeMillis();
		
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);
		
        this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
		
		DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
		
		Satellite satellite = new JSONSatellite("landsat7");
		
		boolean cloudDetection = true;
		if (fmaskFilePath != null) {
			LOGGER.info("Fmask property was set.");
			cloudDetection = false;
		}
		
		Image updatedImage = new SEBAL().processPixelQuentePixelFrio(image,
                satellite, boundingBoxVertices, image.width(), image.height(), cloudDetection);
		
		// See if there's a way to modify this to List<ImagePixel> here and in TestImageHelper
		Image desiredValues = imageHelper.readPixelsFromCSV(desiredValuesFile, this.pixelQuenteFrioChooser);
		
		// List<ImagePixel> obtainedValues = processPixelsFromFile(obtainedValuesFile);
		List<ImagePixel> obtainedValues = updatedImage.pixels();
		//DefaultImagePixel obtainedImagePixel = new DefaultImagePixel();
		// DefaultImagePixel obtainedImagePixel = processPixelsFromFile(obtainedValuesFile);
		// Modify processPixelsFromFile() to return a image instead of a list, this way is possible to use the code
		// beneath here
		
		int counter = 0;
		
		// See if the arguments on 'for' are correct
		for (ImagePixel imagePixel : desiredValues.pixels()) {
			//System.out.println(i);
			GeoLoc desiredGeoLoc = imagePixel.geoLoc();
			ImagePixelOutput desiredOutput = imagePixel.output();

			GeoLoc obtainedGeoLoc = obtainedValues.get(counter).geoLoc();
			ImagePixelOutput obtainedOutput = obtainedValues.get(counter).output();
			counter++;

			assertEquals(obtainedGeoLoc.getI(), desiredGeoLoc.getI());
			assertEquals(obtainedGeoLoc.getJ(), desiredGeoLoc.getJ());
			
			HOutput desiredHOutIncial = desiredOutput.gethOuts().get(0);
			HOutput obtainedHOutIncial = obtainedOutput.gethOuts().get(0);
			
			assertField(desiredHOutIncial.getH(), obtainedHOutIncial.getH());
			assertField(desiredHOutIncial.getA(), obtainedHOutIncial.getA());
			assertField(desiredHOutIncial.getB(), obtainedHOutIncial.getB());
			assertField(desiredHOutIncial.getRah(), obtainedHOutIncial.getRah());
			assertField(desiredHOutIncial.getuAsterisk(), obtainedHOutIncial.getuAsterisk());
			assertField(desiredHOutIncial.getL(), obtainedHOutIncial.getL());

			HOutput desiredHOutFinal = desiredOutput.gethOuts().get(1);
			HOutput obtainedHOutFinal = obtainedOutput.gethOuts().get(1);
			
			assertField(desiredHOutFinal.getH(), obtainedHOutFinal.getH());
			assertField(desiredHOutFinal.getA(), obtainedHOutFinal.getA());
			assertField(desiredHOutFinal.getB(), obtainedHOutFinal.getB());
			assertField(desiredHOutFinal.getRah(), obtainedHOutFinal.getRah());
			assertField(desiredHOutFinal.getuAsterisk(), obtainedHOutFinal.getuAsterisk());
			assertField(desiredHOutFinal.getL(), obtainedHOutFinal.getL());

			assertField(desiredOutput.G(), obtainedOutput.G());
			assertField(desiredOutput.Rn(), obtainedOutput.Rn());
			assertField(desiredOutput.getLambdaE(), obtainedOutput.getLambdaE());
			assertField(desiredOutput.getTs(), obtainedOutput.getTs());
			assertField(desiredOutput.getNDVI(), obtainedOutput.getNDVI());
			assertField(desiredOutput.SAVI(), obtainedOutput.SAVI());
			assertField(desiredOutput.getAlpha(), obtainedOutput.getAlpha());
			assertField(desiredOutput.getZ0mxy(), obtainedOutput.getZ0mxy());
			assertField(desiredOutput.getEpsilonZero(), obtainedOutput.getEpsilonZero());
			assertField(desiredOutput.getEpsilonNB(), obtainedOutput.getEpsilonNB());
			assertField(desiredOutput.getRLDown(), obtainedOutput.getRLDown());
			assertField(desiredOutput.getEpsilonA(), obtainedOutput.getEpsilonA());
			assertField(desiredOutput.getRLUp(), obtainedOutput.getRLUp());
			assertField(desiredOutput.getIAF(), obtainedOutput.getIAF());
			assertField(desiredOutput.getEVI(), obtainedOutput.getEVI());
			assertField(desiredOutput.getRSDown(), obtainedOutput.getRSDown());
			assertField(desiredOutput.getTauSW(), obtainedOutput.getTauSW());
			assertField(desiredOutput.getAlphaToa(), obtainedOutput.getAlphaToa());
			assertField(desiredOutput.getFrEvapo(), obtainedOutput.getFrEvapo());
			assertField(desiredOutput.getTau24h(), obtainedOutput.getTau24h());
			assertField(desiredOutput.getRn24h(), obtainedOutput.getRn24h());
			assertField(desiredOutput.getEvapo24h(), obtainedOutput.getEvapo24h());
			assertField(desiredOutput.getLambda24h(), obtainedOutput.getLambda24h());
		}
	}

	private void assertField(Double desiredValue, Double obtainedValue) {
		assertEquals(desiredValue, obtainedValue, Math.abs(desiredValue * 0.05));
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

	private List<ImagePixel> processPixelsFromFile(String fileName) throws IOException {
		return processPixelsFile(new PixelParser() {
			@Override
			public ImagePixel parseLine(String[] fields) {
				DefaultImagePixel imagePixel = new DefaultImagePixel();
				imagePixel.geoLoc(getGeoLoc(fields));
				double[] L = new double[7];
				for (int i = 23; i < 30; i ++) {
					String value = fields[i].replace("[", "").replace("]","");
					L[i - 23] = Double.valueOf(value);
				}
				imagePixel.L(L);
				imagePixel.setOutput(getImagePixelOutput(fields));
				imagePixel.Ta(Double.valueOf(fields[41]));
				imagePixel.d(Double.valueOf(fields[42]));
				imagePixel.ux(Double.valueOf(fields[43]));
				imagePixel.zx(Double.valueOf(fields[44])); 
				imagePixel.hc(Double.valueOf(fields[45]));
				return imagePixel;
			}
		}, fileName);
	}

	private List<ImagePixel> processPixelsFile(PixelParser pixelParser, String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<ImagePixel> pixels = new ArrayList<ImagePixel>();
		String line = null;
		line = br.readLine();
		while ((line = br.readLine()) != null) {
			String[] fields = line.split(",");
			ImagePixel imagePixel = pixelParser.parseLine(fields);
			pixels.add(imagePixel);
		}
		br.close();
		return pixels;
	}

	private ImagePixelOutput getImagePixelOutput(String[] fields) {
		ImagePixelOutput output = new ImagePixelOutput();
		List<HOutput> hOuts = new ArrayList<HOutput>();
		HOutput hOutInicial = new HOutput();
		hOutInicial.setH(Double.valueOf(fields[4]));
		hOutInicial.setA(Double.valueOf(fields[6]));
		hOutInicial.setB(Double.valueOf(fields[8]));
		hOutInicial.setRah(Double.valueOf(fields[10]));
		hOutInicial.setuAsterisk(Double.valueOf(fields[12]));
		hOutInicial.setL(Double.valueOf(fields[14]));
		hOuts.add(hOutInicial);

		HOutput hOutFinal = new HOutput();
		hOutFinal.setH(Double.valueOf(fields[5]));
		hOutFinal.setA(Double.valueOf(fields[7]));
		hOutFinal.setB(Double.valueOf(fields[9]));
		hOutFinal.setRah(Double.valueOf(fields[11]));
		hOutFinal.setuAsterisk(Double.valueOf(fields[13]));
		hOutFinal.setL(Double.valueOf(fields[15]));
		hOuts.add(hOutFinal);
		output.sethOuts(hOuts);

		output.setG(Double.valueOf(fields[16]));
		output.setRn(Double.valueOf(fields[17]));
		output.setLambdaE(Double.valueOf(fields[18]));
		output.setTs(Double.valueOf(fields[19]));
		output.setNDVI(Double.valueOf(fields[20]));
		output.setSAVI(Double.valueOf(fields[21]));
		output.setAlpha(Double.valueOf(fields[22]));
		output.setZ0mxy(Double.valueOf(fields[30]));
		output.setEpsilonZero(Double.valueOf(fields[31]));
		output.setEpsilonNB(Double.valueOf(fields[32]));
		output.setRLDown(Double.valueOf(fields[33]));
		output.setEpsilonA(Double.valueOf(fields[34]));
		output.setRLUp(Double.valueOf(fields[35]));
		output.setIAF(Double.valueOf(fields[36]));
		output.setEVI(Double.valueOf(fields[37]));
		output.setRSDown(Double.valueOf(fields[38]));
		output.setTauSW(Double.valueOf(fields[39]));
		output.setAlphaToa(Double.valueOf(fields[40]));
		output.setFrEvapo(Double.valueOf(fields[46]));
		output.setTau24h(Double.valueOf(fields[47]));
		output.setRn24h(Double.valueOf(fields[48])); 
		output.setEvapo24h(Double.valueOf(fields[49]));
		output.setLambda24h(Double.valueOf(fields[50]));
		return output;
	}

	private GeoLoc getGeoLoc(String[] fields) {
		int i = Integer.valueOf(fields[0]);
		int j = Integer.valueOf(fields[1]);
		double lat = Double.valueOf(fields[2]);
		double lon = Double.valueOf(fields[3]);
		GeoLoc geoloc = new GeoLoc(i,j,lat,lon);
		return geoloc;
	}

}
