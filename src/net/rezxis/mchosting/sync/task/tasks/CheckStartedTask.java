package net.rezxis.mchosting.sync.task.tasks;

import java.util.HashMap;
import java.util.Map.Entry;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.network.packet.host.HostStopServer;
import net.rezxis.mchosting.sync.SyncServer;
import net.rezxis.mchosting.sync.managers.SyncManager;

public class CheckStartedTask implements Runnable {

	public static HashMap<Integer,Long> queue = new HashMap<>();
	
	@Override
	public void run() {
		long time = System.currentTimeMillis();
		for (Entry<Integer,Long> entry : queue.entrySet()) {
			if (time-entry.getValue() > 1000*60*2) {
				//force stop
				DBServer server = Tables.getSTable().getByID(entry.getKey());
				SyncManager.hosts.get(server.getHost()).send(new Gson().toJson(new HostStopServer(server.getOwner().toString())));
				queue.remove(server.getId());
			}
		}
	}
}
