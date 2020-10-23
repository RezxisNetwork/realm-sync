package net.rezxis.mchosting.sync.task.tasks;

import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.sync.SyncServer;

public class JDAGameTask implements Runnable {
	
	private int lastPlaying = 0;
	private int lastOnline = 0;
	private int count = 0;
	
	public void run() {
		if (count % 10 == 0) {
			int ii = Tables.getSTable().getOnlineServers().size();
			if (ii != lastOnline)  {
				SyncServer.jda.getPresence().setPresence(OnlineStatus.ONLINE, Activity.playing(ii+" servers online!"));
				this.lastOnline = ii;
			}
		}
		count += 1;
	}
}
