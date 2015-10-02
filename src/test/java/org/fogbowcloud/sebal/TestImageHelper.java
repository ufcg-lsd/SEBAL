package org.fogbowcloud.sebal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData.UTC;
import org.fogbowcloud.sebal.model.image.BoundingBox;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.fogbowcloud.sebal.wrapper.Wrapper.PixelParser;
import org.geotools.swing.process.ProcessParameterPage;
import org.apache.log4j.Logger;

public class TestImageHelper {
	
	private static String filePath;
	
	public TestImageHelper() {
		
	}
	
	private static final Logger LOGGER = Logger.getLogger(TestImageHelper.class);

	public static Image readPixelsFromCSV(PixelParser pixelParser, String filePath,
			PixelQuenteFrioChooser pixelQuenteFrioChooser) throws Exception {
		
        Locale.setDefault(Locale.ROOT);
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        DefaultImagePixel imagePixel = new DefaultImagePixel();
        DefaultImage imageCSV = new DefaultImage(pixelQuenteFrioChooser);
        Elevation elevation = new Elevation();
        WeatherStation station = new WeatherStation();
        
        imageCSV.pixels(processPixelsFromFile());
        
        //image.pixels(processPixelsFromFile());
	
        //MetadataElement metadataRoot = product.getMetadataRoot();
        Double sunElevation = 49.00392091;
        /*
        double ULx = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PRODUCT_METADATA")
        		.getAttribute("CORNER_UL_PROJECTION_X_PRODUCT").getData()
        		.getElemDouble();
        double ULy = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PRODUCT_METADATA")
        		.getAttribute("CORNER_UL_PROJECTION_Y_PRODUCT").getData()
        		.getElemDouble();

        int zoneNumber = metadataRoot.getElement("L1_METADATA_FILE")
        		.getElement("PROJECTION_PARAMETERS").getAttribute("UTM_ZONE").getData()
        		.getElemInt();
		
        int centralMeridian = findCentralMeridian(zoneNumber);*/
		
        double[] LArray = new double[imageCSV.pixelFrio().L().length];
        double L = 0.0;
        int counter = 0;
        
		for (ImagePixel imagePixelCSV : imageCSV.pixels()) {
	            L = imagePixelCSV.output().getLambdaE();
	            LArray[counter] = L;
	            counter++;
	            
	            imagePixelCSV.L(LArray);
	            
                imagePixelCSV.cosTheta(Math.sin(Math.toRadians(sunElevation)));      

                Double z = elevation.z(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon());
               
				double Ta = station.Ta(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon(),
						startTime.getAsDate());
				imagePixelCSV.Ta(Ta);

				double ux = station.ux(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon(),
						startTime.getAsDate());
				imagePixelCSV.ux(ux);

				double zx = station.zx(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon());
				imagePixelCSV.zx(zx);

				double d = station.d(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon());
				imagePixelCSV.d(d);

				double hc = station.hc(imagePixelCSV.geoLoc().getLat(), imagePixelCSV.geoLoc().getLon());
				imagePixelCSV.hc(hc);
				
				//if (fmask != null && fmask[fmaskJ * maskWidth + fmaskI] > 1) {
				//	imagePixel.isValid(false);
				//}

                imagePixel.image(imageCSV);
                image.addPixel(imagePixelCSV);
        }
        
        LOGGER.debug("Pixels size=" + image.pixels().size());
        
        return image;
    }
	
	private static List<ImagePixel> processPixelsFromFile() throws IOException {
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
	
	private static GeoLoc getGeoLoc(String[] fields) {
        int i = Integer.valueOf(fields[0]);
        int j = Integer.valueOf(fields[1]);
        double lat = Double.valueOf(fields[2]);
        double lon = Double.valueOf(fields[3]);
        GeoLoc geoloc = new GeoLoc(i, j, lat, lon);
        return geoloc;
    }
	
	private static ImagePixelOutput getImagePixelOutput(String[] fields) {
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
	
	private static List<ImagePixel> processPixelsFile(PixelParser pixelParser,
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
	
	private static String getAllPixelsFileName() {
    	return getAllPixelsFilePath(getFilePath(), "");
    }
	
	private static String getAllPixelsFilePath(String filePath, String mtlName) {
		if (mtlName == null || mtlName.isEmpty()) {
			return filePath + "/" + ".pixels.csv";
		}
		return filePath + "/" + mtlName + "/" + ".pixels.csv";
	}
	
	public static String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}
