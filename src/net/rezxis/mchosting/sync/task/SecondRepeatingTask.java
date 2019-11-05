package net.rezxis.mchosting.sync.task;

import java.util.HashMap;
import java.util.Map.Entry;

public class SecondRepeatingTask extends Thread {

	private HashMap<String,Runnable> tasks = new HashMap<>();
	
	public void run() {
		while (true) {
			//do tasks
			for (Entry<String,Runnable> task : tasks.entrySet()) {
				try {
					task.getValue().run();
				} catch (Exception ex) {
					System.out.println("An exception in "+task.getKey()+" task!");
					ex.printStackTrace();
					System.out.println("---------------------------------------");
				}
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void register(String name, Runnable runnable) {
		tasks.put(name, runnable);
	}
}
