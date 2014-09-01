package org.fogbowcloud.sebal.master;

public interface Task {
	public enum State {
		OPEN, FAILED, RUNNING, FINISHED
	}

	public String getCommand();

	public Resource getResource();

	public String getMetadata(String key);

	public String getId();
	
}
