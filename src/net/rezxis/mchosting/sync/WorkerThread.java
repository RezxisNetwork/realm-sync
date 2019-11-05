package net.rezxis.mchosting.sync;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerSendPacket;
import net.rezxis.mchosting.network.packet.sync.SyncPlayerSendPacket;
import net.rezxis.mchosting.sync.managers.SyncManager;

public class WorkerThread extends Thread {

	private static Gson gson = new Gson();
	
	private String message;
	private WebSocket conn;
	
	public WorkerThread(WebSocket conn,String message) {
		this.message = message;
		this.conn = conn;
	}
	
	public void run() {
		Packet packet = gson.fromJson(message, Packet.class);
		PacketType type = packet.type;
		if (packet.dest != ServerType.SYNC) {
			System.out.println("packet dest is not good.");
			System.out.println(message);
			System.out.println("-----------------------");
			return;
		}
		System.out.println("Received : "+message);
		if (type == PacketType.AuthSocketPacket) {
			SyncManager.authSocket(conn, message);
		} else if (type == PacketType.ServerCreated) {
			SyncManager.createdServer(conn, message);
		} else if (type == PacketType.CreateServer) {
			SyncManager.createServer(conn, message);
		} else if (type == PacketType.ServerStarted) {
			SyncManager.startedServer(conn, message);
		} else if (type == PacketType.ServerStopped) {
			SyncManager.stoppedServer(conn, message);
		} else if (type == PacketType.StartServer) {
			SyncManager.startServer(conn, message);
		} else if (type == PacketType.StopServer) {
			SyncManager.stopServer(conn, message);
		} else if (type == PacketType.PlayerSendPacket) {
			SyncPlayerSendPacket p = gson.fromJson(message, SyncPlayerSendPacket.class);
			SyncManager.bungee.send(gson.toJson(new BungPlayerSendPacket(p.uuid, p.server)));
		} else if (type == PacketType.RebootServer) {
			SyncManager.rebootServer(conn, message);
		} else if (type == PacketType.DeleteServer) {
			SyncManager.deleteServer(conn, message);
		}
	}
}
