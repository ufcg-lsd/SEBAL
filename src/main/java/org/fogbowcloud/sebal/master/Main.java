package org.fogbowcloud.sebal.master;

import java.util.LinkedList;
import java.util.Random;

import org.fogbowcloud.sebal.slave.TaskType;

public class Main {

	static Master master;

	public static void main(String[] args) {
		LinkedList<Statement> statements = new LinkedList<Statement>();
//		statements.add(checkAllF1TasksFinishedStatement());
//		statements.add(checkF1TasksFailedStatement());
//		statements.add(checkF2TasksFailedStatement());
//		statements.add(checkAllCTaskFinishedStatement());
		master = new Master(statements);

		TaskImpl task = new TaskImpl();
		task.putMetadata("Phase", "F1");
		task.putMetadata("Range", "3000 3100 3000 3100");
		task.putMetadata("Exit file name",
				"LT52150652001135CUB00_MTL/3000.3100.exit.F1");
		task.setId("taskid" + (new Random().nextInt()));
		String JAVA = "/usr/lib/jvm/java-7-openjdk-amd64/bin/java";
		String command = JAVA
				+ " -cp .:./lib/* org.fogbowcloud.sebal.Main 3000 3100 3000 3100 "
				+ TaskType.F1;
		task.setCommand(command);
		master.addTask(task);

//		task = new TaskImpl();
//		task.putMetadata("Phase", "F1");
//		task.putMetadata("Range", "3100 3200 3100 3200");
//		task.putMetadata("Exit file name",
//				"LT52150652001135CUB00_MTL/3100.3200.exit.F1");
//		task.setId("taskid" + (new Random().nextInt()));
//		command = JAVA
//				+ " -cp .:./lib/* org.fogbowcloud.sebal.Main 3100 3200 3100 3200 "
//				+ TaskType.F1;
//		task.setCommand(command);
//		master.addTask(task);
//
//		task = new TaskImpl();
//		task.putMetadata("Phase", "F1");
//		task.putMetadata("Range", "3200 3300 3200 3300");
//		task.putMetadata("Exit file name",
//				"LT52150652001135CUB00_MTL/3200.3300.exit.F1");
//		task.setId("taskid" + (new Random().nextInt()));
//		command = JAVA
//				+ " -cp .:./lib/* org.fogbowcloud.sebal.Main 3200 3300 3200 3400 "
//				+ TaskType.F1;
//		task.setCommand(command);
//		master.addTask(task);

		Thread.currentThread().suspend();
	}

	private static Statement checkAllF1TasksFinishedStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				for (Task task : master.getTasks()) {
					if (master.getTaskState(task.getId()).equals(
							Task.State.FINISHED)
							&& (task.getMetadata("Phase").equals("F1")
									|| !task.getMetadata("Phase").equals("C") || !task
									.getMetadata("Phase").equals("F2"))) {
						TaskImpl taskC = new TaskImpl();
						taskC.putMetadata("Phase", "C");
						taskC.setId("taskid" + (new Random().nextInt()));
						taskC.putMetadata("Exit file name",
								"LT52150652001135CUB00_MTL/exit.cpixels");
						String JAVA = "/usr/lib/jvm/java-7-openjdk-amd64/bin/java";
						String command = JAVA
								+ " -cp .:./lib/* org.fogbowcloud.sebal.Main "
								+ task.getMetadata("Range") + " " + TaskType.C;
						taskC.setCommand(command);
						master.addTask(taskC);
					}
				}
			}

			@Override
			public boolean apply(Master m) {
				for (Task task : master.getTasks()) {
					if (!master.getTaskState(task.getId()).equals(
							Task.State.FINISHED)
							&& task.getMetadata("Phase").equals("F1")) {
						return false;
					}
					if (task.getMetadata("Phase").equals("C")) {
						return false;
					}
					if (task.getMetadata("Phase").equals("F2")) {
						return false;
					}
				}
				return true;
			}
		};
		return statement;
	}

	private static Statement checkAllCTaskFinishedStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				for (Task task : master.getTasks()) {
					if (master.getTaskState(task.getId()).equals(
							Task.State.FINISHED)
							&& task.getMetadata("Phase").equals("F1")) {
						TaskImpl taskF2 = new TaskImpl();
						taskF2.putMetadata("Phase", "F2");
						String[] range = task.getMetadata("Range").split(" ");
 						taskF2.putMetadata("Range", task.getMetadata("Range"));
						taskF2.setId("taskid" + (new Random().nextInt()));
						taskF2.putMetadata("Exit file name",
								"LT52150652001135CUB00_MTL/" + range[0] + "."
										+ range[range.length - 1] + ".exit.F2");
						String JAVA = "/usr/lib/jvm/java-7-openjdk-amd64/bin/java";
						String command = JAVA
								+ " -cp .:./lib/* org.fogbowcloud.sebal.Main "
								+ task.getMetadata("Range") + " " + TaskType.F2;
						taskF2.setCommand(command);
						master.addTask(taskF2);
					}
				}
			}

			@Override
			public boolean apply(Master m) {
				for (Task task : master.getTasks()) {
					if (!master.getTaskState(task.getId()).equals(
							Task.State.FINISHED)
							&& task.getMetadata("Phase").equals("F1")) {
						return false;
					}
					if (!master.getTaskState(task.getId()).equals(
							Task.State.FINISHED)
							&& task.getMetadata("Phase").equals("C")) {
						return false;
					}
					if (task.getMetadata("Phase").equals("F2")) {
						return false;
					}
				}
				return true;
			}
		};
		return statement;
	}

	private static Statement checkF1TasksFailedStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				// Recreate F1 task
			}

			@Override
			public boolean apply(Master m) {
				for (Task task : master.getTasks()) {
					if (master.getTaskState(task.getId()).equals(
							Task.State.FAILED)
							&& task.getMetadata("Phase").equals("F1")) {
						return true;
					}
				}
				return false;
			}
		};
		return statement;
	}

	private static Statement checkF2TasksFailedStatement() {
		Statement statement = new Statement() {

			@Override
			public void doAction(Master m) {
				// Send F1F2 Task
			}

			@Override
			public boolean apply(Master m) {
				for (Task task : master.getTasks()) {
					if (master.getTaskState(task.getId()).equals(
							Task.State.FAILED)
							&& task.getMetadata("Phase").equals("F2")) {
						return true;
					}
				}
				return false;
			}
		};
		return statement;
	}
}
