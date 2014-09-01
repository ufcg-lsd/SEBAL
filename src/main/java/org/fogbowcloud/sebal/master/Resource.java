package org.fogbowcloud.sebal.master;

public interface Resource {

	public enum State {
		IDLE, BUSY
	}

	public void execute(Task t);

	public Integer getExitValue();

	public String getId();

}
