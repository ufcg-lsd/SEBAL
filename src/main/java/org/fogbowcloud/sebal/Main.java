package org.fogbowcloud.sebal;

import java.io.File;

import org.fogbowcloud.sebal.wrapper.Wrapper;

public class Main {

	static String MTL_FILE;
	static String MTL_NAME;

	public static void main(String[] args) throws Exception {
		MTL_FILE = args[0];
		String fileName = new File(MTL_FILE).getName();
		MTL_NAME = fileName.substring(0, fileName.indexOf("_"));
		
		int iBegin = Integer.parseInt(args[1]);
		int iFinal = Integer.parseInt(args[2]);
		int jBegin = Integer.parseInt(args[3]);
		int jFinal = Integer.parseInt(args[4]);
		String taskType = args[5];

		Wrapper wrapper = new Wrapper(MTL_FILE, iBegin, iFinal, jBegin, jFinal, MTL_NAME);
		wrapper.doTask(taskType);
	}
}
