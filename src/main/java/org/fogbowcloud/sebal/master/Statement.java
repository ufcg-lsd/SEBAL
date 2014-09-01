package org.fogbowcloud.sebal.master;

public interface Statement {

	public boolean apply(Master m);

	public void doAction(Master m);

}
