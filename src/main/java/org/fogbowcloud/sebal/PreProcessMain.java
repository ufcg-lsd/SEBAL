package org.fogbowcloud.sebal;

import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.RWrapper;
import org.fogbowcloud.sebal.wrapper.TaskType;

public class PreProcessMain {

	public static void main(String[] args) throws Exception {
		String imageName = args[0];
		String imagesPath = args[1];
		String mtlFilePath = args[2];
		String outputDir = args[3];

		int leftX = Integer.parseInt(args[4]);
		int upperY = Integer.parseInt(args[5]);
		int rightX = Integer.parseInt(args[6]);
		int lowerY = Integer.parseInt(args[7]);

		int numberOfPartitions = Integer.parseInt(args[8]);
		int partitionIndex = Integer.parseInt(args[9]);

		String boundingBoxPath = args[10];

		String confFile = args[11];
		Properties properties = new Properties();
		FileInputStream input = new FileInputStream(confFile);
		properties.load(input);

		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(leftX, rightX,
				numberOfPartitions, partitionIndex);

		RWrapper rwrapper = new RWrapper(outputDir, imageName, mtlFilePath,
				imagePartition.getIBegin(), imagePartition.getIFinal(), upperY, lowerY,
				boundingBoxPath, properties);
		rwrapper.doTask(TaskType.PREPROCESS);
	}

}
