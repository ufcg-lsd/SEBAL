package org.fogbowcloud.sebal;

import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.RWrapper;
import org.junit.Test;

public class TestRWrapper {
	
	@Test
	public void rWrapperTest() throws Exception {
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream("sebal.conf");
		properties.load(input);

		RWrapper rWrapper = new RWrapper(properties);
		rWrapper.doTask("F1RCALL");
	}

}
