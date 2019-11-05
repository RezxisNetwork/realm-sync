package net.rezxis.mchosting.sync;

import java.net.InetSocketAddress;

import net.rezxis.mchosting.databse.Database;
import net.rezxis.mchosting.databse.tables.PluginsTable;
import net.rezxis.mchosting.databse.tables.ServersTable;
import net.rezxis.mchosting.network.WSServer;
import net.rezxis.mchosting.sync.task.SecondRepeatingTask;

public class SyncServer {

	public static WSServer server;
	public static ServersTable sTable;
	public static PluginsTable plTable;
	public static SecondRepeatingTask rpTask;
	
	public static void main(String[] args) {
		Database.init();
		rpTask = new SecondRepeatingTask();
		sTable = new ServersTable();
		plTable = new PluginsTable();
		System.out.println("Listening to 9999 Sync Server");
		server = new WSServer(new InetSocketAddress(9999), new WSServerHandler());
		server.setConnectionLostTimeout(0);
		server.start();
	}
}
