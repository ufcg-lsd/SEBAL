package org.fogbowcloud.sebal.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.BoundingBoxVertice;
import org.fogbowcloud.sebal.ClusteredPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.DefaultPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.PixelQuenteFrioChooser;
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

public class Wrapper {

    private String mtlFile;
    private int iBegin;
    private int iFinal;
    private int jBegin;
    private int jFinal;
    private String outputDir;
    private PixelQuenteFrioChooser pixelQuenteFrioChooser;
    private List<BoundingBoxVertice> boundingBoxVertices = new ArrayList<BoundingBoxVertice>();
    
	public Wrapper(String mtlFile, int iBegin, int iFinal, int jBegin, int jFinal, String mtlName,
			String boundingBoxFileName) throws IOException {
		this(mtlFile, null, iBegin, iFinal, jBegin, jFinal, mtlName, boundingBoxFileName);
	}
    
    public Wrapper(String mtlFile, String outputDir, int iBegin, int iFinal, int jBegin,
            int jFinal, String mtlName, String boundingBoxFileName) throws IOException {
    	this.mtlFile = mtlFile;
    	this.iBegin = iBegin;
    	this.iFinal = iFinal;
    	this.jBegin = jBegin;
    	this.jFinal = jFinal;

    	getBoundingBoxVertices(boundingBoxFileName);
    	
//    	this.pixelQuenteFrioChooser = new DefaultPixelQuenteFrioChooser();
    	this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser();
    	if (outputDir == null) {
    		this.outputDir = mtlName;
    	} else {
    		if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
    			new File(outputDir).mkdirs();
    		}
    		this.outputDir = outputDir + "/" + mtlName;
    	}
    }

	private void getBoundingBoxVertices(String boundingBoxFileName) throws IOException {
		//TODO check number of args inside bounding box file name 
		if (boundingBoxFileName != null && new File(boundingBoxFileName).exists()) {
			String boundingBoxInfo = FileUtils.readFileToString(new File(boundingBoxFileName));
			String[] boundingBoxValues = boundingBoxInfo.split(",");

			for (int i = 0; i < boundingBoxValues.length; i += 2) {
				boundingBoxVertices.add(new BoundingBoxVertice(Double
						.parseDouble(boundingBoxValues[i]), Double
						.parseDouble(boundingBoxValues[i + 1])));
			}
			if (boundingBoxVertices.size() < 3){
				System.out.println("Invalid bounding box! Only " + boundingBoxVertices.size()
						+ " vertices set.");
			}
		} else {
			System.out.println("Invalid bounding box file path: " + boundingBoxFileName);
		}
	}
    
    public void doTask(String taskType) throws Exception {
        try {
            if (taskType.equalsIgnoreCase(TaskType.F1)) {
                F1(pixelQuenteFrioChooser);
                return;
            }
            if (taskType.equalsIgnoreCase(TaskType.C)) {
                C(pixelQuenteFrioChooser);
                return;
            }
            if (taskType.equalsIgnoreCase(TaskType.F2)) {
                F2(pixelQuenteFrioChooser);
                return;
            }
            if (taskType.equalsIgnoreCase(TaskType.F1F2)) {
                F1F2(pixelQuenteFrioChooser);
                return;
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(128);
        }
    }

    public void F1(PixelQuenteFrioChooser pixelQuenteFrioChooser)
            throws Exception {
        Product product = SEBALHelper.readProduct(mtlFile, boundingBoxVertices);
        Image image = SEBALHelper.readPixels(product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser);
        Satellite satellite = new JSONSatellite("landsat5");
        Image updatedImage = new SEBAL().processPixelQuentePixelFrio(image,
                satellite, boundingBoxVertices);
        saveProcessOutput(updatedImage);
        savePixelQuente(updatedImage, getPixelQuenteFileName());
        savePixelFrio(updatedImage, getPixelFrioFileName());
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

    private String getPixelFrioFinalFileName() {
        return outputDir + "/" + "frio.csv";
    }

    private String generatePixelFrioResultLine(ImagePixel pixelFrio) {
        ImagePixelOutput outputFrio = getPixelOutput(pixelFrio);
        String pixelFrioOutput = String.valueOf(outputFrio.getTs());
        return pixelFrioOutput;
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

    private List<ImagePixel> getAllPixelsQuente() throws IOException {
        File folder = new File(outputDir);
        File[] listOfFiles = folder.listFiles();
        List<ImagePixel> pixelsQuente = new ArrayList<ImagePixel>();

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().contains("quente.csv")) {
                ImagePixel pixelQuente = processPixelQuenteFromFile(file
                        .getAbsolutePath());
                if (pixelQuente != null) {
                    pixelsQuente.add(pixelQuente);
                }
            }
        }
        return pixelsQuente;
    }

    private List<ImagePixel> getAllPixelsFrio() throws IOException {
        File folder = new File(outputDir);
        File[] listOfFiles = folder.listFiles();
        List<ImagePixel> pixelsFrio = new ArrayList<ImagePixel>();

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().contains("frio.csv")) {
                ImagePixel pixelFrio = processPixelFrioFromFile(file
                        .getAbsolutePath());
                if (pixelFrio != null) {
                    pixelsFrio.add(pixelFrio);
                }
            }
        }
        return pixelsFrio;
    }

    private String getPixelQuenteFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".quente.csv";
    }

    private String getFinalPixelQuenteFileName() {
        return outputDir + "/" + "quente.csv";
    }

    private String generatePixelQuenteResultLine(ImagePixel pixelQuente) {
        ImagePixelOutput outputQuente = getPixelOutput(pixelQuente);
        String pixelQuenteOutput = pixelQuente.ux() + "," + pixelQuente.zx()
                + "," + pixelQuente.hc() + "," + pixelQuente.d() + ","
                + outputQuente.G() + "," + outputQuente.Rn() + ","
                + outputQuente.SAVI() + "," + outputQuente.getTs();
        return pixelQuenteOutput;
    }

    private void saveProcessOutput(Image updatedImage) {
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

    private String getAllPixelsFileName() {
    	return SEBALHelper.getAllPixelsFilePath(outputDir, "", iBegin, iFinal, jBegin, jFinal);
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
        ImagePixelOutput output = getPixelOutput(imagePixel);
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

    public void F2(PixelQuenteFrioChooser pixelQuenteFrioChooser)
            throws Exception {
        ImagePixel pixelQuente = processPixelQuenteFromFile(getFinalPixelQuenteFileName());
        ImagePixel pixelFrio = processPixelFrioFromFile(getPixelFrioFinalFileName());
        List<ImagePixel> pixels = processPixelsFromFile();
        Image image = SEBALHelper.readPixels(pixels, pixelQuente, pixelFrio,
                pixelQuenteFrioChooser);
        image = new SEBAL().pixelHProcess(pixels, pixelQuente,
                getPixelOutput(pixelQuente), getPixelOutput(pixelFrio), image);
        saveFinalProcessOutput(image);
    }

    private ImagePixelOutput getPixelOutput(ImagePixel pixel) {
        if (pixel != null) {
            return pixel.output();
        }
        return null;
    }

    public void C(PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        try {
            Image image = SEBALHelper.readPixels(getAllPixelsQuente(),
                    getAllPixelsFrio(), pixelQuenteFrioChooser);
            image.choosePixelsQuenteFrio();
            savePixelFrio(image, outputDir + "/" + "frio.csv");
            savePixelQuente(image, outputDir + "/" + "quente.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveFinalProcessOutput(Image updatedImage) {
        List<ImagePixel> pixels = updatedImage.pixels();
        String head = "i,j,lat,lon,hInicial,hFinal,aInicial,aFinal,bInicial,bFinal,rahInicial,rahFinal,uInicial,uFinal,"
                + "lInicial,lFinal,g,rn,lambdaE,Ts,NDVI,SAVI,Alpha,L1,L2,L3,L4,L5,L6,L7,Z0mxy,EpsilonZero,getEpsilonNB,RLDown,"
                + "EpsilonA,RLUp,IAF,EVI,RSDown,TauSW,AlphaToa,Ta,d,ux,zx,hc,fracao Evapo,tau24h,rn24h,et24h,le24h\n";

        File outputFile = new File(getFinaLResultFileName());
        if (outputFile.exists()) {
            outputFile.delete();
        }

        try {
            FileUtils.write(outputFile, head, true);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        for (ImagePixel imagePixel : pixels) {
            try {
                FileUtils.write(outputFile,
                        generateFinalResultLine(imagePixel), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFinaLResultFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".F2.csv";
    }

    public String generateFinalResultLine(ImagePixel imagePixel) {
        int i = imagePixel.geoLoc().getI();
        int j = imagePixel.geoLoc().getJ();
        double lat = imagePixel.geoLoc().getLat();
        double lon = imagePixel.geoLoc().getLon();
        ImagePixelOutput output = getPixelOutput(imagePixel);
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

    private ImagePixel processPixelFrioFromFile(String filePath)
            throws IOException {
        return processSinglePixelFile(new PixelParser() {
            @Override
            public ImagePixel parseLine(String[] fields) {
                DefaultImagePixel pixelFrio = new DefaultImagePixel();
                ImagePixelOutput outputFrio = new ImagePixelOutput();
                outputFrio.setTs(Double.valueOf(fields[0]));
                pixelFrio.setOutput(outputFrio);
                return pixelFrio;
            }
        }, filePath);
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

    private ImagePixel processPixelQuenteFromFile(String fileName)
            throws IOException {
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
        }, fileName);
    }

    public void F1F2(PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        try {
            F1(pixelQuenteFrioChooser);
            F2(pixelQuenteFrioChooser);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PixelQuenteFrioChooser getPixelQuenteFrioChooser() {
        return pixelQuenteFrioChooser;
    }

    public void setPixelQuenteFrioChooser(
            PixelQuenteFrioChooser pixelQuenteFrioChooser) {
        this.pixelQuenteFrioChooser = pixelQuenteFrioChooser;
    }

}
