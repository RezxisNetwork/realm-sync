package net.rezxis.mchosting.sync.task.tasks;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.DBServer;
import net.rezxis.mchosting.network.packet.bungee.BungServerStopped;
import net.rezxis.mchosting.network.packet.host.HostStopServer;
import net.rezxis.mchosting.sync.SyncServer;
import net.rezxis.mchosting.sync.managers.SyncManager;

public class CheckStoppedTask implements Runnable {

	//2min to timeout
	public static HashMap<Integer,Long> queue = new HashMap<>();
	
	@Override
	public void run() {
		long time = System.currentTimeMillis();
		for (Entry<Integer,Long> entry : queue.entrySet()) {
			if (time-entry.getValue() > 1000*60*2) {
				//force stop
				DBServer server = SyncServer.sTable.getByID(entry.getKey());
				SyncManager.hosts.get(server.getHost()).send(new Gson().toJson(new HostStopServer(server.getOwner().toString())));
				queue.remove(server.getID());
				SyncManager.bungee.send(new Gson().toJson(new BungServerStopped(server.getPort())));
			}
		}
	}
}
