package org.fogbowcloud.sebal.wrapper;

import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.sebal.ClusteredPixelQuenteFrioChooser;
import org.fogbowcloud.sebal.PixelQuenteFrioChooser;

public class RWrapper {
	
    private PixelQuenteFrioChooser pixelQuenteFrioChooser;
    private String rScriptFilePath;
	private static final Logger LOGGER = Logger.getLogger(RWrapper.class);
	
	public RWrapper(Properties properties) {		
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(properties);			
		
		rScriptFilePath = properties.getProperty("rScript_file_path");
	}
	
	public RWrapper(String mtlFile, String mtlName, Properties properties,
			String rScriptFilePath) {
		this.pixelQuenteFrioChooser = new ClusteredPixelQuenteFrioChooser(
				properties);

		this.rScriptFilePath = rScriptFilePath;
	}
	
	public void doTask(String taskType) throws Exception {
		try {
        	if(taskType.equalsIgnoreCase(TaskType.F1RCALL)) {
        		rF1ScriptCaller(pixelQuenteFrioChooser);
        		return;
        	}
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(128);
        }
	}
	
    private void rF1ScriptCaller(PixelQuenteFrioChooser pixelQuenteFrioChooser2) throws IOException, InterruptedException {
    	LOGGER.info("Calling F1 R script...");
    	
    	long now = System.currentTimeMillis();
    	
    	//ProcessBuilder pb = new ProcessBuilder("Rscript", rScriptFilePath);    	
    	//Process p = pb.start();
    	
    	Process p = Runtime.getRuntime().exec("Rscript " + rScriptFilePath);
    	p.waitFor();
    	
    	LOGGER.info("F1 R script execution time is " + (System.currentTimeMillis() - now));
	}

}
