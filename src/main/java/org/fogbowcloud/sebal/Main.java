package org.fogbowcloud.sebal;

import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.Wrapper;

public class Main {

	public static void main(String[] args) throws Exception {			
		
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);

		Wrapper wrapper = new Wrapper(properties);
        wrapper.doTask(args[1]);
        
    }
}
