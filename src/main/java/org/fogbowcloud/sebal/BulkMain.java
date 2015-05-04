package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fogbowcloud.sebal.tiff.CreateTiff;
import org.fogbowcloud.sebal.wrapper.TaskType;
import org.fogbowcloud.sebal.wrapper.Wrapper;

public class BulkMain {

	public static void main(String[] args) throws Exception {
		String mtlFilePath = args[0];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		int leftX = Integer.parseInt(args[1]);
		int upperY = Integer.parseInt(args[2]);
		int rightX = Integer.parseInt(args[3]);
		int lowerY = Integer.parseInt(args[4]);

		int numberOfPartitions = Integer.parseInt(args[5]);
		int partitionIndex = Integer.parseInt(args[6]);

		int xPartitionInterval = calcXInterval(leftX, rightX, numberOfPartitions);
		
		XPartitionInterval imagePartition = getSelectedPartition(leftX, rightX, xPartitionInterval,
				numberOfPartitions, partitionIndex);
		
		String prefix = imagePartition.getIBegin() + "." + imagePartition.getIFinal() + "." + upperY + "." + lowerY;
		String prefixRaw = leftX + "." + rightX + "." + upperY + "." + lowerY;
		
		String csvFilePath = mtlName + "/" + prefix + ".pixels.csv";
		
		Wrapper wrapper = new Wrapper(mtlFilePath, imagePartition.getIBegin(), 
				imagePartition.getIFinal(), upperY, lowerY, mtlName, null);
		wrapper.doTask(TaskType.F1);
		CreateTiff.createTiff(csvFilePath, prefixRaw + "_" + numberOfPartitions + "_" + partitionIndex, 
				imagePartition.getIFinal() - imagePartition.getIBegin(), lowerY - upperY);
	}

	protected static int calcXInterval(int upperX, int lowerX, int numberOfPartitions) {
		if (upperX == lowerX && numberOfPartitions == 1) {
			return 0;
		}
		int xImageInterval = lowerX - upperX;
		if ((xImageInterval + 1) < numberOfPartitions) {
			throw new IllegalArgumentException("The interval [" + upperX + ", " + lowerX
					+ "] can't be splitted in " + numberOfPartitions + " partitions.");
		}
		int xPartitionInterval = xImageInterval / numberOfPartitions;		
		return xPartitionInterval;
	}

	protected static XPartitionInterval getSelectedPartition(int upperX, int lowerX,
			int xPartitionInterval, int numberOfPartitions, int partitionIndex) {
		int iBegin = upperX;
		int iFinal = upperX + xPartitionInterval;

		for (int i = 1; i < numberOfPartitions; i++) {
			if (partitionIndex == i) {
				break;
			}
			iBegin = iFinal;
			iFinal = iFinal + xPartitionInterval;
		}

		// last partition
		if (partitionIndex == numberOfPartitions) {
			iFinal = lowerX;
		}
		return new XPartitionInterval(iBegin, iFinal);
	}

	public void unZipIt(String zipFile, String outputFolder) {
		byte[] buffer = new byte[1024];

		try {
			// create output directory is not exists
			File folder = new File(outputFolder);
			if (!folder.exists()) {
				folder.mkdir();
			}

			// get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {
				String fileName = ze.getName();
				File newFile = new File(outputFolder + "/" + fileName);
				new File(newFile.getParent()).mkdirs();
				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
