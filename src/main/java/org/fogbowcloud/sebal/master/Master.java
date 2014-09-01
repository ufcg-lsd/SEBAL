package org.fogbowcloud.sebal.master;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Master implements MasterListener {

	private static final int PERIOD = 60;
	private static final int INITITAL_DELAY = 5;
	private final List<Statement> statements = new LinkedList<Statement>();
	private final MasterListener listener;
	private List<Resource> resources = new LinkedList<Resource>();
	private List<Task> tasks = new LinkedList<Task>();
	private Map<Resource, Resource.State> resourceState = new HashMap<Resource, Resource.State>();
	private Map<Task, Task.State> taskState = new HashMap<Task, Task.State>();
	private FakeResourceManager resourceManager;
	private ReentrantReadWriteLock resourceLock = new ReentrantReadWriteLock();
	private ReentrantReadWriteLock taskLock = new ReentrantReadWriteLock();
	private final ExecutorService scheduledExecutorService = Executors
			.newFixedThreadPool(30);
	public Master(List<Statement> statements) {
		this.statements.add(generateResourceStatement());
		this.statements.add(allocateResourceStatement());
		this.statements.add(checkTaskFinishedStatement());
		this.statements.addAll(statements);
		this.listener = this;
		this.resourceManager = new FakeResourceManager(this);
		ScheduledExecutorService scheduledExecutorService = Executors
				.newScheduledThreadPool(1);

		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				listener.stateChanged(Master.this);
			}
		}, INITITAL_DELAY, PERIOD, TimeUnit.SECONDS);
	}

	public List<Resource> getResources() {
		resourceLock.readLock().lock();
		LinkedList<Resource> resourcesCopy = new LinkedList<Resource>(resources);
		resourceLock.readLock().unlock();
		return resourcesCopy;
	}

	public List<Task> getTasks() {
		taskLock.readLock().lock();
		LinkedList<Task> tasksCopy = new LinkedList<Task>(tasks);
		taskLock.readLock().unlock();
		return tasksCopy;
	}

	public void addTask(Task t) {
		taskLock.writeLock().lock();
		tasks.add(t);
		taskState.put(t, Task.State.OPEN);
		taskLock.writeLock().unlock();
		listener.stateChanged(this);
	}

	public void addResource(Resource r) {
		resourceLock.writeLock().lock();
		resources.add(r);
		resourceState.put(r, Resource.State.IDLE);
		resourceLock.writeLock().unlock();
		listener.stateChanged(this);
	}

	@Override
	public void stateChanged(Master master) {
		for (final Statement st : statements) {
			if (st.apply(master)) {
				scheduledExecutorService.execute(new Runnable() {
					@Override
					public void run() {
						st.doAction(Master.this);
					}
				});
			}
		}
	}

	private Statement generateResourceStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				for (int i = 0; i < tasks.size() - getNumberOfIdleResources(); i++) {
					resourceManager.askForResource();
				}
			}

			@Override
			public boolean apply(Master m) {
				return tasks.size() > getNumberOfIdleResources();
			}
		};
		return statement;
	}
	
	private int getNumberOfIdleResources() {
		int i = 0;
		for (Resource resource : getResources()) {
			if (getResourceState(resource.getId()).equals(Resource.State.IDLE)) {
				i++;
			}
		}
		return i;
	}

	private Statement allocateResourceStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				resourceManager.allocateResource();
			}

			@Override
			public boolean apply(Master m) {
				for (Resource resource : getResources()) {
					if (getResourceState(resource.getId()).equals(
							Resource.State.IDLE)) {
						return true;
					}
				}
				return false;
			}
		};
		return statement;
	}

	private void chageResourceStateToIdle() {
		for (Resource resource : getResources()) {
			if (resource.getExitValue().equals(0)) {
				setResourceState(resource.getId(), Resource.State.IDLE);
			}
		}
	}

	private void changeTaskStateToFinished() {
		for (Task task : getTasks()) {
			if (task.getResource().getExitValue().equals(0)) {
				setTaskState(task.getId(), Task.State.FINISHED);
			}
		}
	}

	private Statement checkTaskFinishedStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				changeTaskStateToFinished();
				chageResourceStateToIdle();
			}

			@Override
			public boolean apply(Master m) {
				for (Resource resource : getResources()) {
					if (getResourceState(resource.getId()).equals(
							Resource.State.BUSY)) {
						return resource.getExitValue().equals(0);
					}
				}
				return false;
			}
		};
		return statement;
	}

	public Resource.State getResourceState(String id) {
		for (Resource resource : getResources()) {
			if (resource.getId().equals(id)) {
				return resourceState.get(resource);
			}
		}
		return null;
	}

	public void setResourceState(String id, Resource.State state) {
		for (Resource resource : getResources()) {
			if (resource.getId().equals(id)) {
				resourceLock.writeLock().lock();
				resourceState.put(resource, state);
				resourceLock.writeLock().unlock();
			}
		}
		listener.stateChanged(this);
	}

	public Task.State getTaskState(String id) {
		for (Task task : getTasks()) {
			if (task.getId().equals(id)) {
				return taskState.get(task);
			}
		}
		return null;
	}

	public void setTaskState(String id, Task.State state) {
		for (Task task : getTasks()) {
			if (task.getId().equals(id)) {
				taskLock.writeLock().lock();
				taskState.put(task, state);
				taskLock.writeLock().unlock();
			}
		}
		listener.stateChanged(this);
	}

}
