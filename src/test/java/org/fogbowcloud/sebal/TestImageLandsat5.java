package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.wrapper.Wrapper;
import org.junit.Test;

public class TestImageLandsat5 {
	
	private PixelQuenteFrioChooser pixelQuenteFrioChooser;
	private static final Logger LOGGER = Logger.getLogger(Wrapper.class);
	private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
	TestImageHelper imageHelper;
	String obtainedValuesFilePath = "/home/result.csv";
	String desiredValuesFilePath = "/home/desired.csv";
	Satellite satellite = null;
	
	public TestImageLandsat5(Properties properties) throws IOException {
		imageHelper = new TestImageHelper();
		
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
	}

	@Test
	public void acceptanceTest(String outputDir, String boundingBoxFileName, Properties properties) throws Exception {
		
		Image updatedImage = F1(this.pixelQuenteFrioChooser);
		
		List<ImagePixel> obtainedValues = updatedImage.pixels();
		
		String desiredFlag = "desiredValues";
		Image desiredValues = imageHelper.readPixelsFromCSV(desiredValuesFilePath, this.pixelQuenteFrioChooser, 
				desiredFlag, satellite);
		
		// List<ImagePixel> obtainedValues = processPixelsFromFile(obtainedValuesFile);
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
	
	public Image F1(PixelQuenteFrioChooser pixelQuenteFrioChooser) throws Exception {
		
		LOGGER.info("Executing F1 phase...");
		long now = System.currentTimeMillis();
		
		satellite = new JSONSatellite("landsat5");
		
		String obtainedFlag = "obtainedValues";
		// See if processPixelsFromFile can be used instead
		Image image = imageHelper.readPixelsFromCSV(obtainedValuesFilePath, pixelQuenteFrioChooser, obtainedFlag, 
				satellite);
		
		boolean cloudDetection = false;
		
		image.width(0);
		image.height(0);
		
		Image updatedImage = new SEBAL().processPixelQuentePixelFrio(image,
                satellite, boundingBoxVertices, image.width(), image.height(), cloudDetection);
		
		/*saveProcessOutput(updatedImage);
        savePixelQuente(updatedImage, getPixelQuenteFileName());
        savePixelFrio(updatedImage, getPixelFrioFileName());*/
        LOGGER.info("F1 phase execution time is " + (System.currentTimeMillis() - now));
        
        return updatedImage;
		
	}

	private void assertField(Double desiredValue, Double obtainedValue) {
		assertEquals(desiredValue, obtainedValue, Math.abs(desiredValue * 0.05));
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

	/*private List<ImagePixel> processPixelsFromFile(String fileName) throws IOException {
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
	}*/
	
	/*private void saveProcessOutput(Image updatedImage) {
        List<ImagePixel> pixels = updatedImage.pixels();
        String allPixelsFileName = getAllPixelsFileName();

        File outputFile = new File(allPixelsFileName);
        try {
            FileUtils.write(outputFile, "");
            for (ImagePixel imagePixel : pixels) {
                String resultLine = generateResultLine(imagePixel);
                FileUtils.write(outputFile, resultLine, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	
	private void savePixelFrio(Image updatedImage, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        ImagePixel pixelFrio = updatedImage.pixelFrio();
        if (pixelFrio != null) {
            String line = generatePixelFrioResultLine(pixelFrio);
            stringBuilder.append(line);
        }

        createResultsFile(fileName, stringBuilder);
    }
	
	private String getPixelFrioFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".frio.csv";
    }
	
	private void savePixelQuente(Image updatedImage, String fileName) {
        StringBuilder stringBuilder = new StringBuilder();

        ImagePixel pixelQuente = updatedImage.pixelQuente();
        if (pixelQuente != null) {
            String line = generatePixelQuenteResultLine(pixelQuente);
            stringBuilder.append(line);
        }

        createResultsFile(fileName, stringBuilder);
    }
	
	private String getPixelQuenteFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".quente.csv";
    }
	
	private String getAllPixelsFileName() {
    	return SEBALHelper.getAllPixelsFilePath(outputDir, "", iBegin, iFinal, jBegin, jFinal);
    }
	
	private String generateResultLine(ImagePixel imagePixel) {
        int i = imagePixel.geoLoc().getI();
        int j = imagePixel.geoLoc().getJ();
        double lat = imagePixel.geoLoc().getLat();
        double lon = imagePixel.geoLoc().getLon();
        ImagePixelOutput output = getPixelOutput(imagePixel);
        double g = output.G();
        double rn = output.Rn();
        
		String line = getRow(i, j, lat, lon, g, rn, output.getTs(), output.getNDVI(),
				output.SAVI(), output.getAlpha(), Arrays.toString(imagePixel.L()),
				output.getZ0mxy(), output.getEpsilonZero(), output.getEpsilonNB(),
				output.getRLDown(), output.getEpsilonA(), output.getRLUp(), output.getIAF(),
				output.getEVI(), output.getRSDown(), output.getTauSW(), output.getAlphaToa(),
				imagePixel.Ta(), imagePixel.d(), imagePixel.ux(), imagePixel.zx(), imagePixel.hc());

        return line;
    }
	
	private static String getRow(Object... rowItems) {
        StringBuilder sb = new StringBuilder();
        for (Object rowItem : rowItems) {
            sb.append(rowItem).append(",");
        }
        sb.setCharAt(sb.length() - 1, '\n');
        return sb.toString();
    }
	
	private ImagePixelOutput getPixelOutput(ImagePixel pixel) {
        if (pixel != null) {
            return pixel.output();
        }
        return null;
    }
	
	private String generatePixelFrioResultLine(ImagePixel pixelFrio) {
		ImagePixelOutput outputFrio = getPixelOutput(pixelFrio);
		String pixelFrioOutput = String.valueOf(outputFrio.getTs()) + ","
				+ pixelFrio.geoLoc().getLat() + "," + pixelFrio.geoLoc().getLon();
        return pixelFrioOutput;
    }
	
	private String generatePixelQuenteResultLine(ImagePixel pixelQuente) {
        ImagePixelOutput outputQuente = getPixelOutput(pixelQuente);
		String pixelQuenteOutput = pixelQuente.ux() + "," + pixelQuente.zx() + ","
				+ pixelQuente.hc() + "," + pixelQuente.d() + "," + outputQuente.G() + ","
				+ outputQuente.Rn() + "," + outputQuente.SAVI() + "," + outputQuente.getTs() + ","
				+ pixelQuente.geoLoc().getLat() + "," + pixelQuente.geoLoc().getLon();
        return pixelQuenteOutput;
    }
	
	private void createResultsFile(String fileName, StringBuilder stringBuilder) {
        File file = new File(fileName);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileUtils.writeStringToFile(file, stringBuilder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

}
