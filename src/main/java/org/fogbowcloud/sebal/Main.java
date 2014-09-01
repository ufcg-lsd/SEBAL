package org.fogbowcloud.sebal;

import org.fogbowcloud.sebal.slave.Slave;

public class Main {

	static String MTL_FILE = "13520010515/LT52150652001135CUB00_MTL.txt";

	public static void main(String[] args) throws Exception {
		int iBegin = Integer.parseInt(args[0]);
		int iFinal = Integer.parseInt(args[1]);
		int jBegin = Integer.parseInt(args[2]);
		int jFinal = Integer.parseInt(args[3]);
		String taskType = args[4];

		long begin = System.currentTimeMillis();
		Slave slave = new Slave(MTL_FILE, iBegin, iFinal, jBegin, jFinal);
		slave.doTask(taskType);
		System.out.println(System.currentTimeMillis() - begin);
	}
}
