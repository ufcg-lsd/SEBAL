package org.fogbowcloud.sebal.master;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;

public class FakeResource implements Resource {

	private String id;
	private String exitFileName = "";

	public FakeResource(String id) {
		this.id = id;
	}

	@Override
	public void execute(Task t) {
		ProcessBuilder processBuilder = new ProcessBuilder(t.getCommand()
				.split(" "));
		File log = new File("processbuilderlog");
		processBuilder.redirectErrorStream(true);
		processBuilder.redirectOutput(Redirect.appendTo(log));
		exitFileName = t.getMetadata("Exit file name");

		try {
			processBuilder.start();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public Integer getExitValue() {
		File file = new File(exitFileName);
		if (file.exists()) {
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(file));
				String line = null;
				String lastLine = null;
				while ((line = br.readLine()) != null) {
					lastLine = line;
				}
				br.close();
				return Integer.parseInt(lastLine);
			} catch (IOException e) {
				return -1;
			}
		}
		return 1;
	}

	@Override
	public String getId() {
		return id;
	}

}
