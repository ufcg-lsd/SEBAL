package org.fogbowcloud.sebal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.fogbowcloud.sebal.wrapper.TaskType;
import org.fogbowcloud.sebal.wrapper.Wrapper;

public class BulkMain {

	public static void main(String[] args) throws Exception {		
		String mtlFilePath = args[0];
		String fileName = new File(mtlFilePath).getName();
		String mtlName = fileName.substring(0, fileName.indexOf("_"));

		int upperX = Integer.parseInt(args[1]);
		int leftY = Integer.parseInt(args[2]);
		int lowerX = Integer.parseInt(args[3]);
		int rightY = Integer.parseInt(args[4]);

		int numberOfPartitions = Integer.parseInt(args[5]);
		int partition = Integer.parseInt(args[6]);

		int xImageInterval = lowerX - upperX;
		int xPartitionInterval = xImageInterval / numberOfPartitions;

		int iBegin = upperX;
		int iFinal = upperX + xPartitionInterval;

		for (int i = 1; i < numberOfPartitions; i++) {
			if (partition == i) {
				break;
			}
			iBegin = iFinal + 1;
			iFinal = iFinal + xPartitionInterval;
		}

		// last partition
		if (partition == numberOfPartitions) {
			iFinal = lowerX;
		}

		Wrapper wrapper = new Wrapper(mtlFilePath, iBegin, iFinal, leftY,
				rightY, mtlName, null);
		wrapper.doTask(TaskType.F1);
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
			ZipInputStream zis = new ZipInputStream(
					new FileInputStream(zipFile));
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
