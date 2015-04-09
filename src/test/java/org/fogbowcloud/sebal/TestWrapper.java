package org.fogbowcloud.sebal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.wrapper.TaskType;
import org.fogbowcloud.sebal.wrapper.Wrapper;
import org.junit.Before;
import org.junit.Test;

public class TestWrapper {

	private static final int J_FINAL = 3100;
	private static final int J_BEGIN = 3000;
	private static final int I_FINAL = 3100;
	private static final int I_BEGIN = 3000;
	Wrapper wrapper;
	String MTL_FILE = "13520010515/LT52150652001135CUB00_MTL.txt";
	String MTL_NAME = "LT52150652001135CUB00_MTL";
	String desiredValuesFile = "result.csv";
	String obtainedValuesFile = MTL_NAME + "/" + I_BEGIN + "." + I_FINAL + ".F2.csv";

	@Before
	public void setUp() throws Exception {
		wrapper = new Wrapper(MTL_FILE, I_BEGIN, I_FINAL, J_BEGIN, J_FINAL, MTL_NAME, null);
		wrapper.setPixelQuenteFrioChooser(new TestPixelQuenteFrioChooser());
	}

	@Test
	public void testF1() throws Exception {
		wrapper.doTask(TaskType.F1);
		assertTrue(new File(MTL_NAME + "/" + I_BEGIN + "." + I_FINAL + ".frio.csv").exists());
		assertTrue(new File(MTL_NAME + "/" + I_BEGIN + "." + I_FINAL + ".quente.csv").exists()); 
		assertTrue(new File(MTL_NAME + "/" + I_BEGIN + "." + I_FINAL + ".pixels.csv").exists());
	}

	@Test
	public void testF2() throws Exception {
		wrapper.doTask(TaskType.C);
		wrapper.doTask(TaskType.F2);
		assertTrue(new File(MTL_NAME + "/" + I_BEGIN + "." + I_FINAL + ".F2.csv").exists());
	}

	@Test
	public void testF1F2() throws Exception {
		testF1();
		testF2();
	}

	@Test
	public void acceptanceTest() throws Exception {
		testF1();
		testF2();
		List<ImagePixel> desiredValues = processPixelsFromFile(desiredValuesFile);
		List<ImagePixel> obtainedValues = processPixelsFromFile(obtainedValuesFile);
		for (int i = 0; i < obtainedValues.size(); i++) {
			System.out.println(i);
			GeoLoc desiredGeoLoc = desiredValues.get(i).geoLoc();
			ImagePixelOutput desiredOutput = desiredValues.get(i).output();

			GeoLoc obtainedGeoLoc = obtainedValues.get(i).geoLoc();
			ImagePixelOutput obtainedOutput = obtainedValues.get(i).output();

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
