package org.fogbowcloud.sebal.master;

import java.util.List;
import java.util.Random;

public class FakeResourceManager {

	private Master master;

	public FakeResourceManager(Master master) {
		this.master = master;
	}

	public void askForResource() {
		master.addResource(new FakeResource("resourceid"
				+ (new Random().nextInt())));
	}

	public void allocateResource() {
		List<Task> tasks = master.getTasks();
		List<Resource> resources = master.getResources();
		for (int i = 0; i < resources.size(); i++) {
			FakeResource resource = (FakeResource) resources.get(i);
			if (master.getResourceState(resource.getId()).equals(
					Resource.State.IDLE)) {
				for (int j = 0; j < tasks.size(); j++) {
					TaskImpl task = (TaskImpl) tasks.get(j);
					if (master.getTaskState(task.getId()).equals(
							Task.State.OPEN)) {
						task.setResource(resource);
						master.setTaskState(task.getId(), Task.State.RUNNING);
						master.setResourceState(resource.getId(),
								Resource.State.BUSY);
						resource.execute(task);
						break;
					}
				}
			}
		}
	}
}
