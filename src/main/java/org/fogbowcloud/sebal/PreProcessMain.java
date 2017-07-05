package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.fogbowcloud.sebal.wrapper.RWrapper;
import org.fogbowcloud.sebal.wrapper.TaskType;

public class PreProcessMain {

	public static void main(String[] args) throws Exception {
		String imagesPath = args[0];
		String mtlFilePath = args[1];
		File mtlFile = new File(mtlFilePath);
		String imageName = mtlFile.getParentFile().getName();
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

		XPartitionInterval imagePartition = BulkHelper.getSelectedPartition(
				leftX, rightX, numberOfPartitions, partitionIndex);		

		RWrapper rwrapper = new RWrapper(imagesPath, outputDir, imageName, mtlFilePath, imagePartition.getIBegin(), imagePartition.getIFinal(), upperY,
				lowerY, boundingBoxPath, properties);
		rwrapper.doTask(TaskType.PREPROCESS);
	}

}
