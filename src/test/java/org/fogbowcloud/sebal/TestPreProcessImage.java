package org.fogbowcloud.sebal;


import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class TestPreProcessImage {

	private int iBegin;
	private int iFinal;
	private int jBegin;
	private int jFinal;

	private static final Logger LOGGER = Logger.getLogger(TestPreProcessImage.class);

	public void setProperties(Properties properties) throws IOException {
		String mtlFilePath = properties.getProperty("mtl_file_path");
		if (mtlFilePath == null || mtlFilePath.isEmpty()) {
			LOGGER.error("Property mtl_file_path must be set.");
			throw new IllegalArgumentException("Property mtl_file_path must be set.");
		}

		String iBeginStr = properties.getProperty("i_begin_interval");
		String iFinalStr = properties.getProperty("i_final_interval");
		String jBeginStr = properties.getProperty("j_begin_interval");
		String jFinalStr = properties.getProperty("j_final_interval");

		if (iBeginStr == null || iFinalStr == null || jBeginStr == null || jFinalStr == null) {
			LOGGER.error(
					"Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
			throw new IllegalArgumentException(
					"Interval properties (i_begin_interval, i_final_interval, j_begin_interval, and j_final_interval) must be set.");
		}
		this.iBegin = Integer.parseInt(iBeginStr);
		this.iFinal = Integer.parseInt(iFinalStr);
		this.jBegin = Integer.parseInt(jBeginStr);
		this.jFinal = Integer.parseInt(jFinalStr);

		LOGGER.debug("i interval: (" + iBegin + ", " + iFinal + ")");
		LOGGER.debug("j interval: (" + jBegin + ", " + jFinal + ")");

	}

}
