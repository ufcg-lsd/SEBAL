package org.fogbowcloud.sebal;


public class BulkHelper {
	
	public static XPartitionInterval getSelectedPartition(int leftX, int rightX,
			int numberOfPartitions, int partitionIndex) {
		int xPartitionInterval = calcXInterval(leftX, rightX, numberOfPartitions);
		int iBegin = leftX;
		int iFinal = leftX + xPartitionInterval;

		for (int i = 1; i < numberOfPartitions; i++) {
			if (partitionIndex == i) {
				break;
			}
			iBegin = iFinal;
			iFinal = iFinal + xPartitionInterval;
		}

		// last partition
		if (partitionIndex == numberOfPartitions) {
			iFinal = rightX;
		}
		return new XPartitionInterval(iBegin, iFinal);		
	}
	
	protected static int calcXInterval(int leftX, int rightX, int numberOfPartitions) {
		if (leftX == rightX && numberOfPartitions == 1) {
			return 0;
		}
		int xImageInterval = rightX - leftX;
		if ((xImageInterval + 1) < numberOfPartitions) {
			throw new IllegalArgumentException("The interval [" + leftX + ", " + rightX
					+ "] can't be splitted in " + numberOfPartitions + " partitions.");
		}
		int xPartitionInterval = xImageInterval / numberOfPartitions;		
		return xPartitionInterval;
	}
}
