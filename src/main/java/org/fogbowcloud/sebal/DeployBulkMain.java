package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.Wrapper;

public class DeployBulkMain {

	public static void main(String[] args) throws Exception {
		String mtlFilePath = args[0];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		String outputDir = args[1];

		int leftX = Integer.parseInt(args[2]);
		int upperY = Integer.parseInt(args[3]);
		int rightX = Integer.parseInt(args[4]);
		int lowerY = Integer.parseInt(args[5]);

		String phase = args[6];
		String landsat = null;
		
		int numberOfPartitions = Integer.parseInt(args[7]);
		int partitionIndex = Integer.parseInt(args[8]);

		String boundingBoxPath = args[9];
				
		String confFile = args[10];
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(confFile);
		properties.load(input);

		String fmaskFilePath = null;
		if (args.length > 10) {
			fmaskFilePath = args[11];
		}
		
		String rScriptFilePath = args[12];
		
		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(leftX, rightX,
				numberOfPartitions, partitionIndex);

		Wrapper wrapper = new Wrapper(mtlFilePath, outputDir,
				imagePartition.getIBegin(), imagePartition.getIFinal(), upperY,
				lowerY, mtlName, boundingBoxPath, properties, fmaskFilePath,
				rScriptFilePath);
		wrapper.doTask(phase);
	}
}
