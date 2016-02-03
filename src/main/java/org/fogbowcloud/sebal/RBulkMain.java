package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.RWrapper;
import org.fogbowcloud.sebal.wrapper.TaskType;

public class RBulkMain {

	public static void main(String[] args) throws Exception {
		String imagesPath = args[0];
		String mtlFilePath = args[1];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		String outputDir = args[2];

		int leftX = Integer.parseInt(args[3]);
		int upperY = Integer.parseInt(args[4]);
		int rightX = Integer.parseInt(args[5]);
		int lowerY = Integer.parseInt(args[6]);

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
		String rScriptFileName = args[13];

		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(
				leftX, rightX, numberOfPartitions, partitionIndex);

		RWrapper rwrapper = new RWrapper(imagesPath, mtlFilePath, outputDir,
				imagePartition.getIBegin(), imagePartition.getIFinal(), upperY,
				lowerY, mtlName, boundingBoxPath, properties, fmaskFilePath,
				rScriptFilePath, rScriptFileName);
		rwrapper.doTask(TaskType.PREPROCESS);
		rwrapper.doTask(TaskType.F1RCALL);
	}

}
