package org.fogbowcloud.sebal;

public class XPartitionInterval {
	
	private int iBegin;
	private int iFinal;

	public XPartitionInterval(int iBegin, int iFinal) {
		this.iBegin = iBegin;
		this.iFinal = iFinal;
	}

	public int getIBegin() {
		return iBegin;
	}

	public int getIFinal() {
		return iFinal;
	}
}
