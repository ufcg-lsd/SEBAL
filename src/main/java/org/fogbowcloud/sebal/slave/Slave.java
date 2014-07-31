package org.fogbowcloud.sebal.slave;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.fogbowcloud.sebal.SEBAL;
import org.fogbowcloud.sebal.model.image.Image;
import org.fogbowcloud.sebal.model.image.ImagePixel;
import org.fogbowcloud.sebal.model.image.ImagePixelOutput;
import org.fogbowcloud.sebal.model.satellite.Satellite;

public class Slave {
	
	SEBAL sebal;
	Satellite satellite;
	Image image;
	String fileName;
	
	public Slave(Satellite satellite, Image image, String fileName) {
		this.satellite = satellite;
		this.image = image;
		this.fileName = fileName;
		
		try {
			sebal = new SEBAL();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void doTask(String taskType) {
		if (taskType.equals(TaskType.F1))  {
			F1();
		}

		else if (taskType.equals(TaskType.F2))  {
			F2();
		}

		else if (taskType.equals(TaskType.F1F2))  {
			F1F2();
		}
	}

	public void F1() {
		this.image = this.sebal.processPixelQuentePixelFrio(this.image, this.satellite);
		saveProcessOutput(fileName + "F1");
	}

	private void saveProcessOutput(String fileName) {
		List<ImagePixel> pixels = image.pixels();
		StringBuilder stringBuilder = new StringBuilder();
		for (ImagePixel imagePixel : pixels) {
			String line = generateResultLine(imagePixel);
			stringBuilder.append(line);
		}
		createResultsFile(fileName, stringBuilder);
	}
	
	private void createResultsFile(String fileName, StringBuilder stringBuilder) {
		File file = new File(fileName);
		if (file.exists()) {
			file.delete();
		}
		try {
			FileUtils.writeStringToFile(file, stringBuilder.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
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

		return i + "," + j + "," + lat + "," + lon
				+ "," + g + "," + rn + "," + output.getTs() + "," + output.getNDVI() +  
				"," + output.SAVI() + "," + output.getAlpha() + "," + 
				Arrays.toString(imagePixel.L()) + "," + output.getZ0mxy() + "," + 
				output.getEpsilonZero() + "," + output.getEpsilonNB() + "," + output.getRLDown() + "," + 
				output.getEpsilonA() + "," + output.getRLUp() + "," +  + output.getIAF() + "," + 
				output.getEVI() + "," + output.getRSDown() + "," + output.getTauSW() + "," + 
				output.getAlphaToa() + "," + imagePixel.Ta() + "," + imagePixel.d() + "," + 
				imagePixel.ux() + "," + imagePixel.zx() + "," + imagePixel.hc() + "\n";
	}

	public void F2() {
		ImagePixel pixelQuente = this.image.pixelQuente();
		ImagePixel pixelFrio = this.image.pixelFrio();
		this.sebal.pixelHProcess(this.image, this.fileName + "F2", 
				pixelQuente, pixelQuente.output(), pixelFrio.output());
	}
	
	private void processPixelFromFile(String fileName) {
		
	}
	
	private void processWeatherStationFromFile(String fileName) {
		
	}
	
	private void processCommumValuesFromFile(String fileName) {
		
	}
	
	private void processPixelQuentePixelFrioFromFile(String fileName) {
		
	}

	public void F1F2() {
		F1();
		F2();
	}
	
	public List<ImagePixel> pixelQuentePixelFrio() {
		return null;
	}
	
	public String output() {
		return null;
	}
}
