package org.fogbowcloud.sebal.master;

import java.util.HashMap;
import java.util.Map;

public class TaskImpl implements Task {

	private String command;
	private String id;
	private Resource resource;
	private Map<String, String> metadata = new HashMap<String, String>();

	@Override
	public String getCommand() {
		return command;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public String getMetadata(String key) {
		return metadata.get(key);
	}
	
	public void putMetadata(String key, String value) {
		metadata.put(key, value);
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	@Override
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
}
