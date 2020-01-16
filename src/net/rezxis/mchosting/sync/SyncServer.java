package net.rezxis.mchosting.sync;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.rezxis.mchosting.database.Database;
import net.rezxis.mchosting.network.WSServer;
import net.rezxis.mchosting.sync.discord.JDAListener;
import net.rezxis.mchosting.sync.task.SecondRepeatingTask;
import net.rezxis.mchosting.sync.task.tasks.CheckStartedTask;
import net.rezxis.mchosting.sync.task.tasks.CheckStoppedTask;

public class SyncServer {

	public static JDA jda;
	public static WSServer server;
	public static SecondRepeatingTask rpTask;
	public static Props props;
	
	public static void main(String[] args) {
		boolean discord = true;
		
		for (String s : args) {
			if (s.equalsIgnoreCase("-jda"))
				discord = false;
		}
		props = new Props("sync.propertis");
		Database.init(props.DB_HOST,props.DB_USER,props.DB_PASS,props.DB_PORT,props.DB_NAME);
		if (!new File("files").exists()) {
			new File("files").mkdirs();
		}
		Runtime.getRuntime().addShutdownHook(new Thread(()->{try {
			server.stop();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}));
		rpTask = new SecondRepeatingTask();
		rpTask.register("start", new CheckStartedTask());
		rpTask.register("stop", new CheckStoppedTask());
		rpTask.start();
		if (discord) {
			try {
				buildJDA();
			} catch (Exception e) {
				e.printStackTrace();
			}
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
