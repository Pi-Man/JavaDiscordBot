package piman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TaskExecutor implements Runnable {

	private List<Runnable> tasks = new ArrayList<>();
	
	private boolean running = true;
	
	@Override
	public void run() {
		
		while(running) {
			synchronized (this) {
				for (Runnable task : tasks) {
					task.run();
				}
				tasks.clear();
				
				
				try {
					this.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void addTasks(Runnable... tasks) {
		synchronized (this) {
			this.tasks.addAll(Arrays.asList(tasks));
			this.notify();
		}
	}
	
	public void addTasks(List<Runnable> tasks) {
		synchronized (this) {
			this.tasks.addAll(tasks);
			this.notify();
		}
	}
	
	public void shutdown() {
		synchronized (this) {
			this.running = false;
			this.notify();
		}
	}
}
