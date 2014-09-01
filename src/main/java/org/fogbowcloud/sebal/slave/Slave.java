package org.fogbowcloud.sebal.slave;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.SEBAL;
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.Satellite;

public class Slave {

	private String mtlFile;
	private int iBegin;
	private int iFinal;
	private int jBegin;
	private int jFinal;

	public Slave(String mtlFile, int iBegin, int iFinal, int jBegin, int jFinal) {
		this.mtlFile = mtlFile;
		this.iBegin = iBegin;
		this.iFinal = iFinal;
		this.jBegin = jBegin;
		this.jFinal = jFinal;
	}

	public void doTask(String taskType) throws Exception {
		if (taskType.equalsIgnoreCase(TaskType.F1))  {
			String exitFileName = mtlFile + "." + iBegin + "." + iFinal + ".exit.F1";
			try {
				F1();
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("0");
				createResultsFile(exitFileName, stringBuilder);
			} catch (IOException e) {
				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("1");
				createResultsFile(exitFileName, stringBuilder);
			}
		}
			else if (taskType.equalsIgnoreCase(TaskType.F2))  {
				String exitFileName = mtlFile + "." + iBegin + "." + iFinal + ".exit.F2";
				try {
					F2();
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("0");
					createResultsFile(exitFileName, stringBuilder);
				} catch (IOException e) {
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("1");
					createResultsFile(exitFileName, stringBuilder);
				}
			}

			else if (taskType.equalsIgnoreCase(TaskType.F1F2))  {
				F1F2();
			}
		}

	public void F1() throws Exception {
		Product product = SEBALHelper.readProduct(mtlFile);
		Image image = SEBALHelper.readPixels(product, iBegin, iFinal, jBegin,
				jFinal);
		Satellite satellite = new JSONSatellite("landsat5");
		Image updatedImage = new SEBAL().processPixelQuentePixelFrio(image,
				satellite);
		saveProcessOutput(updatedImage);
		savePixelQuente(updatedImage);
		savePixelFrio(updatedImage);
	}

	private void savePixelFrio(Image updatedImage) {
		StringBuilder stringBuilder = new StringBuilder();

		ImagePixel pixelFrio = updatedImage.pixelFrio();
		String line = generatePixelFrioResultLine(pixelFrio);
		stringBuilder.append(line);

		createResultsFile(getPixelFrioFileName(), stringBuilder);
	}

	private String getPixelFrioFileName() {
		return mtlFile + "." + iBegin + "." + iFinal + ".frio.csv";
	}

	private String generatePixelFrioResultLine(ImagePixel pixelFrio) {
		ImagePixelOutput outputFrio = pixelFrio.output();
		String pixelFrioOutput = String.valueOf(outputFrio.getTs());
		return pixelFrioOutput;
	}

	private void savePixelQuente(Image updatedImage) {
		StringBuilder stringBuilder = new StringBuilder();

		ImagePixel pixelQuente = updatedImage.pixelQuente();
		String line = generatePixelQuenteResultLine(pixelQuente);
		stringBuilder.append(line);

		createResultsFile(getPixelQuenteFileName(), stringBuilder);
	}

	private String getPixelQuenteFileName() {
		return mtlFile + "." + iBegin + "." + iFinal + ".quente.csv";
	}

	private String generatePixelQuenteResultLine(ImagePixel pixelQuente) {
		ImagePixelOutput outputQuente = pixelQuente.output();
		String pixelQuenteOutput = pixelQuente.ux() + "," + pixelQuente.zx()
				+ "," + pixelQuente.hc() + "," + pixelQuente.d() + ","
				+ outputQuente.G() + "," + outputQuente.Rn() + ","
				+ outputQuente.SAVI() + "," + outputQuente.getTs();
		return pixelQuenteOutput;
	}

	private void saveProcessOutput(Image updatedImage) {
		List<ImagePixel> pixels = updatedImage.pixels();
		StringBuilder stringBuilder = new StringBuilder();
		for (ImagePixel imagePixel : pixels) {
			String line = generateResultLine(imagePixel);
			stringBuilder.append(line);
		}
		createResultsFile(getAllPixelsFileName(), stringBuilder);
	}

	private String getAllPixelsFileName() {
		return mtlFile + "." + iBegin + "." + iFinal + ".pixels.csv";
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
	}

	private String generateResultLine(ImagePixel imagePixel) {
		int i = imagePixel.geoLoc().getI();
		int j = imagePixel.geoLoc().getJ();
		double lat = imagePixel.geoLoc().getLat();
		double lon = imagePixel.geoLoc().getLon();
		ImagePixelOutput output = imagePixel.output();
		double g = output.G();
		double rn = output.Rn();
		String line = getRow(i, j, lat, lon, g, rn, output.getTs(),
				output.getNDVI(), output.SAVI(), output.getAlpha(),
				Arrays.toString(imagePixel.L()), output.getZ0mxy(),
				output.getEpsilonZero(), output.getEpsilonNB(),
				output.getRLDown(), output.getEpsilonA(), output.getRLUp(),
				output.getIAF(), output.getEVI(), output.getRSDown(),
				output.getTauSW(), output.getAlphaToa(), imagePixel.Ta(),
				imagePixel.d(), imagePixel.ux(), imagePixel.zx(),
				imagePixel.hc());

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

	public void F2() throws Exception {
		ImagePixel pixelQuente = processPixelQuenteFromFile();
		ImagePixel pixelFrio = processPixelFrioFromFile();
		List<ImagePixel> pixels = processPixelsFromFile();
		Image image = SEBALHelper.readPixels(pixels, pixelQuente, pixelFrio);
		image = new SEBAL().pixelHProcess(pixels, pixelQuente,
				pixelQuente.output(), pixelFrio.output(), image);
		saveFinalProcessOutput(image);
	}

	private void saveFinalProcessOutput(Image updatedImage) {
		List<ImagePixel> pixels = updatedImage.pixels();
		StringBuilder stringBuilder = new StringBuilder();
		String head = "i,j,lat,lon,hInicial,hFinal,aInicial,aFinal,bInicial,"
				+ "bFinal,rahInicial,rahFinal,uInicial,uFinal,lInicial,lFinal,g,"
				+ "rn,lambdaE,Ts,NDVI,SAVI,Alpha,L1,L2,L3,L4,L5,L6,L7,Z0mxy,"
				+ "EpsilonZero,getEpsilonNB,RLDown,EpsilonA,RLUp,IAF,EVI,"
				+ "RSDown,TauSW,AlphaToa,Ta,d,ux,zx,hc,fracao Evapo,tau24h,"
				+ "rn24h,et24h,le24h\n";
		stringBuilder.append(head);
		for (ImagePixel imagePixel : pixels) {
			String line = generateFinalResultLine(imagePixel);
			stringBuilder.append(line);
		}
		createResultsFile(getFinaLResultFileName(), stringBuilder);
	}

	public String getFinaLResultFileName() {
		return mtlFile + "." + iBegin + "." + iFinal + ".F2.csv";
	}

	public String generateFinalResultLine(ImagePixel imagePixel) {
		int i = imagePixel.geoLoc().getI();
		int j = imagePixel.geoLoc().getJ();
		double lat = imagePixel.geoLoc().getLat();
		double lon = imagePixel.geoLoc().getLon();
		ImagePixelOutput output = imagePixel.output();
		List<HOutput> hOuts = output.gethOuts();
		double g = output.G();
		double rn = output.Rn();
		double lambdaE = output.getLambdaE();

		double hFinal = hOuts.get(hOuts.size() - 1).getH();
		double hInicial = hOuts.get(0).getH();
		double aFinal = hOuts.get(hOuts.size() - 1).getA();
		double aInicial = hOuts.get(0).getA();
		double bFinal = hOuts.get(hOuts.size() - 1).getB();
		double bInicial = hOuts.get(0).getB();
		double uFinal = hOuts.get(hOuts.size() - 1).getuAsterisk();
		double uInicial = hOuts.get(0).getuAsterisk();
		double lFinal = hOuts.get(hOuts.size() - 1).getL();
		double lInicial = hOuts.get(0).getL();
		double rahFinal = hOuts.get(hOuts.size() - 1).getRah();
		double rahInicial = hOuts.get(0).getRah();
		double rn24h = output.getRn24h();
		double frEvapo = output.getFrEvapo();
		double le24h = output.getLambda24h();
		double et24h = output.getEvapo24h();
		return i + "," + j + "," + lat + "," + lon + "," + hInicial + ","
				+ hFinal + "," + aInicial + "," + aFinal + "," + bInicial + ","
				+ bFinal + "," + rahInicial + "," + rahFinal + "," + uInicial
				+ "," + uFinal + "," + lInicial + "," + lFinal + "," + g + ","
				+ rn + "," + lambdaE + "," + output.getTs() + ","
				+ output.getNDVI() + "," + output.SAVI() + ","
				+ output.getAlpha() + "," + Arrays.toString(imagePixel.L())
				+ "," + output.getZ0mxy() + "," + output.getEpsilonZero() + ","
				+ output.getEpsilonNB() + "," + output.getRLDown() + ","
				+ output.getEpsilonA() + "," + output.getRLUp() + ","
				+ output.getIAF() + "," + output.getEVI() + ","
				+ output.getRSDown() + "," + output.getTauSW() + ","
				+ output.getAlphaToa() + "," + imagePixel.Ta() + ","
				+ imagePixel.d() + "," + imagePixel.ux() + ","
				+ imagePixel.zx() + "," + imagePixel.hc() + "," + frEvapo + ","
				+ output.getTau24h() + "," + rn24h + "," + et24h + "," + le24h
				+ "\n";
	}

	private List<ImagePixel> processPixelsFromFile() throws IOException {
		return processPixelsFile(new PixelParser() {
			@Override
			public ImagePixel parseLine(String[] fields) {
				DefaultImagePixel imagePixel = new DefaultImagePixel();
				imagePixel.geoLoc(getGeoLoc(fields));
				imagePixel.setOutput(getImagePixelOutput(fields));
				double band1 = Double.valueOf(fields[10].substring(1));
				double band2 = Double.valueOf(fields[11]);
				double band3 = Double.valueOf(fields[12]);
				double band4 = Double.valueOf(fields[13]);
				double band5 = Double.valueOf(fields[14]);
				double band6 = Double.valueOf(fields[15]);
				double band7 = Double.valueOf(fields[16].substring(0,
						fields[16].length() - 1));
				double[] L = { band1, band2, band3, band4, band5, band6, band7 };
				imagePixel.L(L);
				imagePixel.Ta(Double.valueOf(fields[28]));
				imagePixel.d(Double.valueOf(fields[29]));
				imagePixel.ux(Double.valueOf(fields[30]));
				imagePixel.zx(Double.valueOf(fields[31]));
				imagePixel.hc(Double.valueOf(fields[32]));
				return imagePixel;
			}
		}, getAllPixelsFileName());
	}

	private ImagePixelOutput getImagePixelOutput(String[] fields) {
		ImagePixelOutput output = new ImagePixelOutput();
		output.setG(Double.valueOf(fields[4]));
		output.setRn(Double.valueOf(fields[5]));
		output.setTs(Double.valueOf(fields[6]));
		output.setNDVI(Double.valueOf(fields[7]));
		output.setSAVI(Double.valueOf(fields[8]));
		output.setAlpha(Double.valueOf(fields[9]));
		output.setZ0mxy(Double.valueOf(fields[17]));
		output.setEpsilonZero(Double.valueOf(fields[18]));
		output.setEpsilonNB(Double.valueOf(fields[19]));
		output.setRLDown(Double.valueOf(fields[20]));
		output.setEpsilonA(Double.valueOf(fields[21]));
		output.setRLUp(Double.valueOf(fields[22]));
		output.setIAF(Double.valueOf(fields[23]));
		output.setEVI(Double.valueOf(fields[24]));
		output.setRSDown(Double.valueOf(fields[25]));
		output.setTauSW(Double.valueOf(fields[26]));
		output.setAlphaToa(Double.valueOf(fields[27]));
		return output;
	}

	private GeoLoc getGeoLoc(String[] fields) {
		int i = Integer.valueOf(fields[0]);
		int j = Integer.valueOf(fields[1]);
		double lat = Double.valueOf(fields[2]);
		double lon = Double.valueOf(fields[3]);
		GeoLoc geoloc = new GeoLoc(i, j, lat, lon);
		return geoloc;
	}

	private ImagePixel processPixelFrioFromFile() throws IOException {
		return processSinglePixelFile(new PixelParser() {
			@Override
			public ImagePixel parseLine(String[] fields) {
				DefaultImagePixel pixelFrio = new DefaultImagePixel();
				ImagePixelOutput outputFrio = new ImagePixelOutput();
				outputFrio.setTs(Double.valueOf(fields[0]));
				pixelFrio.setOutput(outputFrio);
				return pixelFrio;
			}
		}, getPixelFrioFileName());
	}

	interface PixelParser {
		ImagePixel parseLine(String[] line);
	}

	private List<ImagePixel> processPixelsFile(PixelParser pixelParser,
			String file) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(file));
		List<ImagePixel> pixels = new ArrayList<ImagePixel>();
		String line = null;
		while ((line = br.readLine()) != null) {
			String[] fields = line.split(",");
			ImagePixel imagePixel = pixelParser.parseLine(fields);
			pixels.add(imagePixel);
		}
		br.close();
		return pixels;
	}

	private ImagePixel processSinglePixelFile(PixelParser pixelParser,
			String file) throws IOException {
		List<ImagePixel> allPixels = processPixelsFile(pixelParser, file);
		return allPixels.isEmpty() ? null : allPixels.get(0);
	}

	private ImagePixel processPixelQuenteFromFile() throws IOException {
		return processSinglePixelFile(new PixelParser() {
			@Override
			public ImagePixel parseLine(String[] fields) {
				DefaultImagePixel pixelQuente = new DefaultImagePixel();
				pixelQuente.ux(Double.valueOf(fields[0]));
				pixelQuente.zx(Double.valueOf(fields[1]));
				pixelQuente.hc(Double.valueOf(fields[2]));
				pixelQuente.d(Double.valueOf(fields[3]));

				ImagePixelOutput outputQuente = new ImagePixelOutput();
				outputQuente.setG(Double.valueOf(fields[4]));
				outputQuente.setRn(Double.valueOf(fields[5]));
				outputQuente.setSAVI(Double.valueOf(fields[6]));
				outputQuente.setTs(Double.valueOf(fields[7]));
				pixelQuente.setOutput(outputQuente);
				return pixelQuente;
			}
		}, getPixelQuenteFileName());
	}

	public void F1F2() {
		try {
			F1();
			F2();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
