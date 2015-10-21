package org.fogbowcloud.sebal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.model.image.DefaultImage;
import org.fogbowcloud.sebal.model.image.DefaultImagePixel;
import org.fogbowcloud.sebal.model.image.GeoLoc;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.Satellite;
import org.fogbowcloud.sebal.parsers.Elevation;
import org.fogbowcloud.sebal.parsers.WeatherStation;
import org.fogbowcloud.sebal.wrapper.Wrapper.PixelParser;
import org.mockito.Mockito;

public class TestImageHelper {
	
	private static String filePath;
	
	private static final Logger LOGGER = Logger.getLogger(TestImageHelper.class);
	
	private static Map<Integer, Integer> zoneToCentralMeridian = new HashMap<Integer, Integer>();

	protected static Image readPixelsFromCSV(String filePath,
			PixelQuenteFrioChooser pixelQuenteFrioChooser, String valueFlag, Satellite satellite) throws Exception {
		
		// Initializing image variables
        Locale.setDefault(Locale.ROOT);
        setFilePath(filePath);
        //DefaultImagePixel imagePixel = new DefaultImagePixel();
//        DefaultImage imageCSV = new DefaultImage(pixelQuenteFrioChooser);
        Elevation elevation = new Elevation();
        
        // Mocking WeatherStation class
        // Change the return values
        WeatherStation station = Mockito.mock(WeatherStation.class);
        Mockito.when(station.Ta(Mockito.anyDouble(), Mockito.anyDouble(),
        		Mockito.any(Date.class))).thenReturn(32.233);
        Mockito.when(station.zx(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(6.0);
        Mockito.when(station.ux(Mockito.anyDouble(), Mockito.anyDouble(),
        		Mockito.any(Date.class))).thenReturn(4.388);
        Mockito.when(station.d(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(4.0 * 2/3);
        Mockito.when(station.hc(Mockito.anyDouble(), 
        		Mockito.anyDouble())).thenReturn(4.0);
        
        List<ImagePixel> pixels;
        // Reading and storing data from .csv file to pixels in a image
        if(valueFlag.equals("obtainedValues")) {
        	pixels = processPixelsFromObtainedFile();
        } else
        	pixels = processPixelsFromFile();
        
        // Sun Elevation for Landsat 5
        //
        // Modify this to support multiple satellite types
        //Double sunElevation = 49.00392091;
        Double sunElevation = 0.0;
        // Sun Elevation for Landsat 7
        //
        //Double sunElevation = 53.52375;
        String date = "2001-05-15";
        Date D = null;
        D.valueOf(date);
        
        
        int counter = 0;
        
        DefaultImage image = new DefaultImage(pixelQuenteFrioChooser);
        // Scanning csv image to calculate and store values in another image
		for (ImagePixel pixelFromCSV : pixels) {
			
			DefaultImagePixel currentPixel = new DefaultImagePixel();			
			currentPixel.L(pixelFromCSV.L());
			
			currentPixel.geoLoc(pixelFromCSV.geoLoc());
			
			currentPixel.cosTheta(Math.sin(Math.toRadians(sunElevation)));
			                               
			double latitude = pixelFromCSV.geoLoc().getLat();
			double longitude = pixelFromCSV.geoLoc().getLon();

			// Setting elevation
			currentPixel.z(pixelFromCSV.z());
			
			// Calculate Ta based on image coordinates and date/time
			double Ta = station.Ta(latitude, longitude, D);
			currentPixel.Ta(Ta);

			// Calculate ux based on image coordinates and date/time
			double ux = station.ux(latitude, longitude, D);
			currentPixel.ux(ux);

			// Calculate rho based on the satellite and imagePixelCSV
			double[] rho = new SEBAL().calcRhosat5(satellite, pixelFromCSV);
			currentPixel.output().setRho(rho);

			// Calculate zx based on image coordinates
			double zx = station.zx(latitude, longitude);
			currentPixel.zx(zx);

			// Calculate ux based on image coordinates
			double d = station.d(latitude, longitude);
			currentPixel.d(d);

			// Calculate ux based on image coordinates
			double hc = station.hc(latitude, longitude);
			currentPixel.hc(hc);

			/*if (valueFlag.equals("desiredValues")) {
				// Calculate G based on obtained Rn
				double G = new SEBAL().G(pixelFromCSV.output().getTs(),
						pixelFromCSV.output().getAlpha(), pixelFromCSV.output()
								.getNDVI(), pixelFromCSV.output().Rn());
				currentPixel.output().setG(G);

				// Calculate z0mxy based on obtained SAVI
				double z0mxy = new SEBAL().z0mxy(pixelFromCSV.output().SAVI());
				currentPixel.output().setZ0mxy(z0mxy);

				// Calculate IAF based on obtained SAVI
				double IAF = new SEBAL().IAF(pixelFromCSV.output().SAVI());
				currentPixel.output().setIAF(IAF);
			}

			rho = pixelFromCSV.output().getRho();
			double EVI = new SEBAL().EVI(rho[0], rho[2], rho[3]);
			currentPixel.output().setEVI(EVI);*/

			// Add image csv to variable image from imagePixel
			// The csv pixel is then add to the other image pixel
			currentPixel.image(image);
			image.addPixel(currentPixel);
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
                double elevation = Double.valueOf(fields[9]);
                imagePixel.z(elevation);
                double band1 = Double.valueOf(fields[11].substring(1));
                double band2 = Double.valueOf(fields[12]);
                double band3 = Double.valueOf(fields[13]);
                double band4 = Double.valueOf(fields[14]);
                double band5 = Double.valueOf(fields[15]);
                double band6 = Double.valueOf(fields[16]);
                double band7 = Double.valueOf(fields[17].substring(0,
                		fields[8].length() - 1));
                double[] L = { band1, band2, band3, band4, band5, band6, band7 };
                imagePixel.L(L);
                double[] rho = { Double.valueOf(fields[17]), Double.valueOf(fields[18]),
                		Double.valueOf(fields[19]), Double.valueOf(fields[20]),
                		Double.valueOf(fields[21]), Double.valueOf(fields[23]) };
                imagePixel.output().setRho(rho);
                return imagePixel;
            }
        }, getAllPixelsFileName());
    }
	
	private static List<ImagePixel> processPixelsFromObtainedFile() throws IOException {
        return processPixelsFile(new PixelParser() {
            @Override
            public ImagePixel parseLine(String[] fields) {
                DefaultImagePixel imagePixel = new DefaultImagePixel();
                imagePixel.geoLoc(getGeoLoc(fields));
                double elevation = Double.valueOf(fields[9]);
                imagePixel.z(elevation);
                double band1 = Double.valueOf(fields[10].substring(1));
                double band2 = Double.valueOf(fields[11]);
                double band3 = Double.valueOf(fields[12]);
                double band4 = Double.valueOf(fields[13]);
                double band5 = Double.valueOf(fields[14]);
                double band6 = Double.valueOf(fields[15]);
                double band7 = Double.valueOf(fields[16].substring(0,
                		fields[8].length() - 1));
                double[] L = { band1, band2, band3, band4, band5, band6, band7 };
                imagePixel.L(L);
                return imagePixel;
            }
        }, getAllPixelsFileName());
    }
	
	private static GeoLoc getGeoLoc(String[] fields) {
        int i = 0;
        int j = 0;
        
        // Modify this later based on John's data
        int zoneNumber = 24;
        
        int centralMeridian = -39;
        
        
        // Converting UTM to latitude and longitude
		LatLonCoordinate latLonCoordinate;
		try {
			latLonCoordinate = SEBALHelper.convertUtmToLatLon(Double.valueOf(fields[0]), 
					Double.valueOf(fields[1]),
					zoneNumber, centralMeridian);
			
			// Setting latitude in imagePixel
	        double latitude = Double.valueOf(String.format("%.10g%n",
	              latLonCoordinate.getLat()));
	        
	        // Setting longitude in imagePixel
	        double longitude = Double.valueOf(String.format("%.10g%n",
	              latLonCoordinate.getLon()));
	        
	        GeoLoc geoloc = new GeoLoc(i, j, latitude, longitude);
	        return geoloc;
		} catch (Exception e) {
			LOGGER.error("Error while converting coordinates.", e);
		}	
		
		return null;       
    }
	
	private static ImagePixelOutput getImagePixelOutput(String[] fields) {
	        ImagePixelOutput output = new ImagePixelOutput();
	        output.setRn(Double.valueOf(fields[37]));
	        output.setTs(Double.valueOf(fields[33]));
	        output.setNDVI(Double.valueOf(fields[28]));
	        output.setSAVI(Double.valueOf(fields[29]));
	        output.setAlpha(Double.valueOf(fields[26]));
	        output.setEpsilonZero(Double.valueOf(fields[32]));
	        output.setEpsilonNB(Double.valueOf(fields[31]));
	        output.setRLDown(Double.valueOf(fields[36]));
	        output.setEpsilonA(Double.valueOf(fields[35]));
	        output.setRLUp(Double.valueOf(fields[34]));
	        output.setRSDown(Double.valueOf(fields[27]));
	        output.setTauSW(Double.valueOf(fields[25]));
	        output.setAlphaToa(Double.valueOf(fields[24]));
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
	
	// Analyze to make sure that the file path is right
	private static String getAllPixelsFilePath(String filePath, String mtlName) {
		if (mtlName == null || mtlName.isEmpty()) {
			return filePath + "/" + "desired.csv";
		}
		return filePath + "/" + mtlName + "/" + "desired.csv";
	}    
	
	public static String getFilePath() {
		return filePath;
	}

	public static void setFilePath(String anotherfilePath) {
		filePath = anotherfilePath;
	}

}
