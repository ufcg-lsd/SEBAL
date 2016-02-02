package org.fogbowcloud.sebal.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.fogbowcloud.sebal.BoundingBoxVertice;
import org.fogbowcloud.sebal.ClusteredPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.PixelQuenteFrioChooser;
import org.fogbowcloud.sebal.SEBAL;
import org.fogbowcloud.sebal.SEBALHelper;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.HOutput;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.L5JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.L7JSONSatellite;
import org.fogbowcloud.sebal.model.satellite.L8JSONSatellite;
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
    private String fmaskFilePath;
    
	private static final Logger LOGGER = Logger.getLogger(Wrapper.class);
    
	public Wrapper(Properties properties) throws IOException {
		String mtlFilePath = properties.getProperty("mtl_file_path");
		if (mtlFilePath == null || mtlFilePath.isEmpty()) {
			LOGGER.error("Property mtl_file_path must be set.");
			throw new IllegalArgumentException("Property mtl_file_path must be set.");
		}
		this.mtlFile = mtlFilePath;
		
		String iBeginStr = properties.getProperty("i_begin_interval");
		String iFinalStr = properties.getProperty("i_final_interval");
		String jBeginStr = properties.getProperty("j_begin_interval");
		String jFinalStr = properties.getProperty("j_final_interval");
		
		if (iBeginStr == null || iFinalStr == null || jBeginStr == null || jFinalStr == null) {
			LOGGER.error("Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
			throw new IllegalArgumentException(
					"Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
		}
		this.iBegin = Integer.parseInt(iBeginStr);
		this.iFinal = Integer.parseInt(iFinalStr);
		this.jBegin = Integer.parseInt(jBeginStr);
		this.jFinal = Integer.parseInt(jFinalStr);
		
		LOGGER.debug("i interval: (" + iBegin + ", " + iFinal + ")");
		LOGGER.debug("j interval: (" + jBegin + ", " + jFinal + ")");
		
		boundingBoxVertices = SEBALHelper.getVerticesFromFile(properties.getProperty("bounding_box_file_path"));

		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);

		String fileName = new File(mtlFile).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));
		String outputDir = properties.getProperty("output_dir_path");

		if (outputDir == null || outputDir.isEmpty()) {
    		this.outputDir = mtlName;
    	} else {
    		if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
    			new File(outputDir).mkdirs();
    		}
    		this.outputDir = outputDir + "/" + mtlName;
    	}
		
		fmaskFilePath = properties.getProperty("fmask_file_path");
	}
	
	public Wrapper(String mtlFile, String outputDir, int iBegin, int iFinal, int jBegin,
			int jFinal, String mtlName, String boundingBoxFileName, Properties properties,
			String fmaskFilePath, String rScriptFilePath) throws IOException {
		this.mtlFile = mtlFile;
		this.iBegin = iBegin;
		this.iFinal = iFinal;
		this.jBegin = jBegin;
		this.jFinal = jFinal;

		boundingBoxVertices = SEBALHelper.getVerticesFromFile(boundingBoxFileName);

		// this.pixelQuenteFrioChooser = new RandomPixelQuenteFrioChooser();
		// this.pixelQuenteFrioChooser = new DefaultPixelQuenteFrioChooser();
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);
		if (outputDir == null) {
			this.outputDir = mtlName;
		} else {
			if (!new File(outputDir).exists() || !new File(outputDir).isDirectory()) {
				new File(outputDir).mkdirs();
			}
			this.outputDir = outputDir + "/" + mtlName;
		}
		this.fmaskFilePath = fmaskFilePath;
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
    	LOGGER.info("Executing F1 phase...");
    	
    	long now = System.currentTimeMillis();
        Product product = SEBALHelper.readProduct(mtlFile, boundingBoxVertices);
        
        BoundingBox boundingBox = null;
        if (boundingBoxVertices.size() > 3) {
        	boundingBox = SEBALHelper.calculateBoundingBox(boundingBoxVertices, product);
        	LOGGER.debug("bounding_box: X=" + boundingBox.getX() + " - Y=" + boundingBox.getY());
        	LOGGER.debug("bounding_box: W=" + boundingBox.getW() + " - H=" + boundingBox.getH());
        }             
                
        Image image = SEBALHelper.readPixels(product, iBegin, iFinal, jBegin,
                jFinal, pixelQuenteFrioChooser, boundingBox, fmaskFilePath);                          
        
        MetadataElement metadataRoot = product.getMetadataRoot();
        String landsatType = metadataRoot.getElement("L1_METADATA_FILE")
                .getElement("PRODUCT_METADATA").getAttribute("SPACECRAFT_ID")
                .getData().getElemString();
        
        Satellite satellite;
        if(landsatType.equalsIgnoreCase("LANDSAT_5")) {
        	satellite = new L5JSONSatellite(Satellite.LANDSAT_L5);
        } else if(landsatType.equalsIgnoreCase("LANDSAT_7")) {
        	satellite = new L7JSONSatellite(Satellite.LANDSAT_L7);
        } else
        	satellite = new L8JSONSatellite(Satellite.LANDSAT_L8);
        
        LOGGER.debug("F1 phase time read = " + (System.currentTimeMillis() - now));
               		
		boolean cloudDetection = true;
		if (fmaskFilePath != null) {
			LOGGER.info("Fmask property was set.");
			cloudDetection = false;
		}
		
        Image updatedImage = new SEBAL().processPixelQuentePixelFrio(image,
                satellite, boundingBoxVertices, image.width(), image.height(), cloudDetection);        
        
        saveProcessOutput(updatedImage);
//        savePixelQuente(updatedImage, getPixelQuenteFileName());
//        savePixelFrio(updatedImage, getPixelFrioFileName());
        savePixelQuenteCandidates(updatedImage, getPixelQuenteCandidatesFileName());
        savePixelFrioCandidates(updatedImage, getPixelFrioCandidatesFileName());
        LOGGER.info("F1 phase execution time is " + (System.currentTimeMillis() - now));
    }

    private void savePixelFrioCandidates(Image updatedImage, String fileName) {
		LOGGER.debug("Saving pixel " + updatedImage.pixelFrioCandidates().size()
				+ " frio candidates in file " + fileName);
		
		List<ImagePixel> pixelFrioCandidates = updatedImage.pixelFrioCandidates();
		long now = System.currentTimeMillis();
		//TODO LOg here
        File outputFile = new File(fileName);
        try {
            FileUtils.write(outputFile, "");
            for (ImagePixel imagePixel : pixelFrioCandidates) {
                String resultLine = generatePixelFrioResultLine(imagePixel);
                FileUtils.write(outputFile, resultLine, true);
            }
        } catch (IOException e) {
            LOGGER.error("Error while saving file.", e);
        }  
        LOGGER.debug("Saving pixel frio candidates time=" + (System.currentTimeMillis() - now));
	}

	private void savePixelQuenteCandidates(Image updatedImage, String fileName) {
		LOGGER.debug("Saving pixel " + updatedImage.pixelQuenteCandidates().size()
				+ " quente candidates in file " + fileName);
    	long now = System.currentTimeMillis();
        List<ImagePixel> pixelQuenteCandidates = updatedImage.pixelQuenteCandidates();
        
        //TODO lOG here
        File outputFile = new File(fileName);
        try {
            FileUtils.write(outputFile, "");
            for (ImagePixel imagePixel : pixelQuenteCandidates) {
                String resultLine = generatePixelQuenteResultLine(imagePixel);
                FileUtils.write(outputFile, resultLine, true);
            }
        } catch (IOException e) {
            LOGGER.error("Error while saving file.", e);
        }
        LOGGER.debug("Saving pixel quente candidates time=" + (System.currentTimeMillis() - now));
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

    private String getPixelFrioCandidatesFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".frio.csv";
    }

    private String getPixelFrioFinalFileName() {
        return outputDir + "/" + "frio.csv";
    }

    private String generatePixelFrioResultLine(ImagePixel pixelFrio) {
		ImagePixelOutput outputFrio = getPixelOutput(pixelFrio);
//		String pixelFrioOutput = String.valueOf(outputFrio.getTs()) + ","
//				+ pixelFrio.geoLoc().getLat() + "," + pixelFrio.geoLoc().getLon();		
		String line = getRow(String.valueOf(outputFrio.getTs()), String.valueOf(outputFrio.getNDVI()), pixelFrio
				.geoLoc().getLat(), pixelFrio.geoLoc().getLon());
        return line;
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

    private List<ImagePixel> getAllPixelQuenteCandidates() throws IOException {
        File folder = new File(outputDir);
        File[] listOfFiles = folder.listFiles();
        List<ImagePixel> pixelQuenteCandidates = new ArrayList<ImagePixel>();

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().contains("quente.csv")) {
                List<ImagePixel> pixelsQuente = processPixelQuenteCandidatesFromFile(file
                        .getAbsolutePath());
                if (pixelsQuente != null) {
                    pixelQuenteCandidates.addAll(pixelsQuente);
                }
            }
        }
        return pixelQuenteCandidates;
    }

    private List<ImagePixel> getAllPixelFrioCandidates() throws IOException {
        File folder = new File(outputDir);
        File[] listOfFiles = folder.listFiles();
        List<ImagePixel> pixelFrioCandidates = new ArrayList<ImagePixel>();

        for (File file : listOfFiles) {
            if (file.isFile() && file.getName().contains("frio.csv")) {
                List<ImagePixel> pixelsFrio = processPixelFrioFromFile(file
                        .getAbsolutePath());
                if (pixelsFrio != null) {
                    pixelFrioCandidates.addAll(pixelsFrio);
                }
            }
        }
        return pixelFrioCandidates;
    }

    private String getPixelQuenteCandidatesFileName() {
        return outputDir + "/" + iBegin + "." + iFinal + "." + jBegin
                + "." + jFinal + ".quente.csv";
    }

    private String getFinalPixelQuenteFileName() {
        return outputDir + "/" + "quente.csv";
    }

    private String generatePixelQuenteResultLine(ImagePixel pixelQuente) {
        ImagePixelOutput outputQuente = getPixelOutput(pixelQuente);
//		String pixelQuenteOutput = pixelQuente.ux() + "," + pixelQuente.zx() + ","
//				+ pixelQuente.hc() + "," + pixelQuente.d() + "," + outputQuente.G() + ","
//				+ outputQuente.Rn() + "," + outputQuente.SAVI() + "," + outputQuente.getTs() + ","
//				+ pixelQuente.geoLoc().getLat() + "," + pixelQuente.geoLoc().getLon();
		
		String line = getRow(pixelQuente.ux(), pixelQuente.zx(), pixelQuente.hc(), pixelQuente.d(),
				outputQuente.G(), outputQuente.Rn(), outputQuente.SAVI(), outputQuente.getTs(),
				outputQuente.getNDVI(), pixelQuente.geoLoc().getLat(), pixelQuente.geoLoc()
						.getLon());
		
        return line;
    }

    private void saveProcessOutput(Image updatedImage) {
    	long now = System.currentTimeMillis();
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
		LOGGER.debug("Saving process output time=" + (System.currentTimeMillis() - now));
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

    public void F2(PixelQuenteFrioChooser pixelQuenteFrioChooser)
            throws Exception {
		LOGGER.info("Executing F2 phase...");
		long now = System.currentTimeMillis();
		if (processPixelQuenteCandidatesFromFile(getFinalPixelQuenteFileName()) == null
				|| processPixelQuenteCandidatesFromFile(getFinalPixelQuenteFileName()).isEmpty()
				|| processPixelFrioFromFile(getPixelFrioFinalFileName()) == null
				|| processPixelFrioFromFile(getPixelFrioFinalFileName()).isEmpty()) {
			LOGGER.error("There is not pixel quente and/or frio choosen.");
			return;
		}
		
		ImagePixel pixelQuente = processPixelQuenteCandidatesFromFile(getFinalPixelQuenteFileName())
				.get(0);
		ImagePixel pixelFrio = processPixelFrioFromFile(getPixelFrioFinalFileName()).get(0);
		List<ImagePixel> pixels = processPixelsFromFile();
		LOGGER.debug("Number of pixels is " + pixels.size());
		Image image = SEBALHelper
				.readPixels(pixels, pixelQuente, pixelFrio, pixelQuenteFrioChooser);
		image = new SEBAL().pixelHProcess(pixels, pixelQuente, getPixelOutput(pixelQuente),
				getPixelOutput(pixelFrio), image);
		saveFinalProcessOutput(image);
		LOGGER.info("F2 phase execution time is " + (System.currentTimeMillis() - now));
    }

    private ImagePixelOutput getPixelOutput(ImagePixel pixel) {
        if (pixel != null) {
            return pixel.output();
        }
        return null;
    }

    public void C(PixelQuenteFrioChooser pixelQuenteFrioChooser) {
    	LOGGER.info("Executing C phase...");

        try {
        	long now = System.currentTimeMillis();
        	
            Image image = SEBALHelper.readPixels(getAllPixelQuenteCandidates(),
                    getAllPixelFrioCandidates(), pixelQuenteFrioChooser);
            image.choosePixelsQuenteFrio();
            savePixelFrio(image, outputDir + "/" + "frio.csv");
            savePixelQuente(image, outputDir + "/" + "quente.csv");
            LOGGER.info("C phase execution time is " + (System.currentTimeMillis() - now));
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
        //TODO make this code better
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

    private List<ImagePixel> processPixelFrioFromFile(String filePath)
            throws IOException {
        return processPixelsFile(new PixelParser() {
            @Override
            public ImagePixel parseLine(String[] fields) {
                DefaultImagePixel pixelFrioCandidate = new DefaultImagePixel();
                ImagePixelOutput outputFrio = new ImagePixelOutput();
                outputFrio.setTs(Double.valueOf(fields[0]));
                outputFrio.setNDVI(Double.valueOf(fields[1]));
                pixelFrioCandidate.setOutput(outputFrio);

                double latitude = Double.valueOf(fields[2]);
                double longitude = Double.valueOf(fields[3]);
                GeoLoc geoLoc = new GeoLoc();
                geoLoc.setLat(latitude);
                geoLoc.setLon(longitude);
                pixelFrioCandidate.geoLoc(geoLoc);
                
                return pixelFrioCandidate;
            }
        }, filePath);
    }

    public interface PixelParser {
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

    private List<ImagePixel> processPixelQuenteCandidatesFromFile(String fileName)
            throws IOException {
        return processPixelsFile(new PixelParser() {
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
                outputQuente.setNDVI(Double.valueOf(fields[8]));
                pixelQuente.setOutput(outputQuente);
                
                double latitude = Double.valueOf(fields[9]);
                double longitude = Double.valueOf(fields[10]);
                GeoLoc geoLoc = new GeoLoc();
                geoLoc.setLat(latitude);
                geoLoc.setLon(longitude);
                pixelQuente.geoLoc(geoLoc);
                
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
