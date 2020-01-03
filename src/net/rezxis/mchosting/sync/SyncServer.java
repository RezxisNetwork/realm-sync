package net.rezxis.mchosting.sync;

import java.io.File;
import java.net.InetSocketAddress;

import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.database.tables.PluginsTable;
import net.rezxis.mchosting.database.tables.ServersTable;
import net.rezxis.mchosting.network.WSServer;
import net.rezxis.mchosting.sync.discord.JDAListener;
import net.rezxis.mchosting.sync.task.SecondRepeatingTask;
import net.rezxis.mchosting.sync.task.tasks.CheckStartedTask;
import net.rezxis.mchosting.sync.task.tasks.CheckStoppedTask;

public class SyncServer {

	public static JDA jda;
	public static WSServer server;
	public static ServersTable sTable;
	public static PluginsTable plTable;
	public static SecondRepeatingTask rpTask;
	
	public static void main(String[] args) {
		Database.init();
		if (!new File("files").exists()) {
			new File("files").mkdirs();
		}
		rpTask = new SecondRepeatingTask();
		rpTask.register("start", new CheckStartedTask());
		rpTask.register("stop", new CheckStoppedTask());
		rpTask.start();
		sTable = new ServersTable();
		plTable = new PluginsTable();
		try {
			buildJDA();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Listening to 9999 Sync Server");
		server = new WSServer(new InetSocketAddress(9999), new WSServerHandler());
		server.setConnectionLostTimeout(0);
		server.start();
	}
	
	private static void buildJDA() throws Exception {
		jda = new JDABuilder("NTI4MTMwNDg2MjU0NDM2Mzcz.Xg58rA.JWKA22qfeQSXzJFNN8WVoT2oplk").addEventListeners(new JDAListener()).build();
	}
}
