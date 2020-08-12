package net.rezxis.mchosting.sync.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBServer.GameType;
import net.rezxis.mchosting.database.object.server.ServerStatus;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.bungee.BungServerStarted;
import net.rezxis.mchosting.network.packet.bungee.BungServerStopped;
import net.rezxis.mchosting.network.packet.game.GameStopServer;
import net.rezxis.mchosting.network.packet.host.HostCreateServer;
import net.rezxis.mchosting.network.packet.host.HostDeleteServer;
import net.rezxis.mchosting.network.packet.host.HostRebootServer;
import net.rezxis.mchosting.network.packet.host.HostStartServer;
import net.rezxis.mchosting.network.packet.host.HostStoppedServer;
import net.rezxis.mchosting.network.packet.lobby.LobbyServerCreated;
import net.rezxis.mchosting.network.packet.lobby.LobbyServerStarted;
import net.rezxis.mchosting.network.packet.lobby.LobbyServerStopped;
import net.rezxis.mchosting.network.packet.sync.SyncAuthSocketPacket;
import net.rezxis.mchosting.network.packet.sync.SyncCreateServer;
import net.rezxis.mchosting.network.packet.sync.SyncDeleteServer;
import net.rezxis.mchosting.network.packet.sync.SyncRebootServer;
import net.rezxis.mchosting.network.packet.sync.SyncServerCreated;
import net.rezxis.mchosting.network.packet.sync.SyncServerStarted;
import net.rezxis.mchosting.network.packet.sync.SyncStartServer;
import net.rezxis.mchosting.network.packet.sync.SyncStopServer;
import net.rezxis.mchosting.network.packet.sync.SyncStoppedServer;
import net.rezxis.mchosting.sync.task.tasks.CheckStartedTask;
import net.rezxis.mchosting.sync.task.tasks.CheckStoppedTask;

public class SyncManager {

	public static Gson gson = new Gson();
	//HostID : socket
	public static HashMap<Integer, WebSocket> hosts = new HashMap<>();
	//ServerID : socket
	public static HashMap<Integer, WebSocket> games = new HashMap<>();
	
	public static ArrayList<Integer> rebooting = new ArrayList<>();
	
	public static WebSocket bungee;
	public static WebSocket lobby;
	
	public static void authSocket(WebSocket conn, String message) {
		SyncAuthSocketPacket packet = gson.fromJson(message, SyncAuthSocketPacket.class);
		if (packet.auth == ServerType.BUNGEE) {
			bungee = conn;
		} else if (packet.auth == ServerType.HOST) {
			hosts.put(Integer.valueOf(packet.options.get("id")), conn);
		} else if (packet.auth == ServerType.GAME) {
			games.put(Integer.valueOf(packet.options.get("id")),conn);
		} else if (packet.auth == ServerType.LOBBY) {
			lobby = conn;
		}
		System.out.println("A connection authed : "+packet.auth.name());
	}
	
	public static void createServer(WebSocket conn, String message) {
		SyncCreateServer packet = gson.fromJson(message, SyncCreateServer.class);
		//relay to Host server.
		WebSocket dest = hosts.values().iterator().next();
		HostCreateServer cPacket = new HostCreateServer(packet.player,packet.displayName,packet.world,packet.stype);
		dest.send(gson.toJson(cPacket));
	}
	
	public static void createdServer(WebSocket conn, String message) {
		SyncServerCreated packet = gson.fromJson(message, SyncServerCreated.class);
		lobby.send(gson.toJson(new LobbyServerCreated(packet.player)));
	}
	
	public static void startedServer(WebSocket conn, String message) {
		SyncServerStarted packet = gson.fromJson(message, SyncServerStarted.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.player));
		WebSocket host = hosts.get(server.getHost());
		bungee.send(gson.toJson(new BungServerStarted(server.getDisplayName(), server.getIp(), server.getPort())));
		lobby.send(gson.toJson(new LobbyServerStarted(server.getOwner().toString())));
		CheckStartedTask.queue.remove(server.getId());
	}
	
	public static void stopServer(WebSocket conn, String message) {
		SyncStopServer packet = gson.fromJson(message, SyncStopServer.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.player));
		if (server == null) {
			System.out.println("The server is not found.");
			return;
		}
		if (server.getStatus() != ServerStatus.RUNNING) {
			System.out.println("The server is not running");
			return;
		}
		if (server.getType() == GameType.CUSTOM) {
			bungee.send(gson.toJson(new BungServerStopped(server.getDisplayName())));
			WebSocket game = games.get(server.getId());
			game.send(gson.toJson(new GameStopServer()));
			return;
		}
		if (!games.containsKey(server.getId())) {
			System.out.println("the game is not sync! this is fatal error!");
			return;
		}
		server.setStatus(ServerStatus.STOPPING);
		server.update();
		//send stop signal to game
		bungee.send(gson.toJson(new BungServerStopped(server.getDisplayName())));
		WebSocket game = games.get(server.getId());
		game.send(gson.toJson(new GameStopServer()));
		CheckStoppedTask.queue.put(server.getId(), System.currentTimeMillis());
	}
	
	public static void stoppedServer(WebSocket conn, String message) {
		SyncStoppedServer packet = gson.fromJson(message, SyncStoppedServer.class);
		DBServer server = Tables.getSTable().getByID(packet.serverID);
		if (rebooting.contains(server.getId())) {
			//restart
			WebSocket dest = hosts.get(server.getHost());
			dest.send(gson.toJson(new HostRebootServer(server.getId())));
			rebooting.remove((Object)server.getId());
			return;
		}
		server.setPort(-1);
		server.setPlayers(0);
		server.setStatus(ServerStatus.STOP);
		server.update();
		CheckStoppedTask.queue.remove(server.getId());
		hosts.get(server.getHost()).send(gson.toJson(new HostStoppedServer(server.getOwner().toString())));
		lobby.send(gson.toJson(new LobbyServerStopped(server.getOwner().toString())));
	}
	
	public static void startServer(WebSocket conn, String message) {
		SyncStartServer packet = gson.fromJson(message, SyncStartServer.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.player));
		if (server == null) {
			System.out.println("The server is not found.");
			return;
		}
		if (server.getStatus() != ServerStatus.STOP) {
			System.out.println("The server is not stopped");
			return;
		}
		if (!hosts.containsKey(server.getHost())) {
			System.out.println("the host which has the server is not online!");
			return;
		}
		HostStartServer sPacket = new HostStartServer(packet.player);
		WebSocket dest = hosts.get(server.getHost());
		dest.send(gson.toJson(sPacket));
	}
	
	public static void rebootServer(WebSocket conn, String message) {
		SyncRebootServer packet = gson.fromJson(message, SyncRebootServer.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.owner));
		server.setStatus(ServerStatus.REBOOTING);
		server.update();
		games.get(server.getId()).send(gson.toJson(new GameStopServer()));
		rebooting.add(server.getId());
	}
	
	public static void deleteServer(WebSocket conn, String message) {
		SyncDeleteServer packet = gson.fromJson(message, SyncDeleteServer.class);
		DBServer server = Tables.getSTable().get(UUID.fromString(packet.player));
		hosts.get(server.getHost()).send(gson.toJson(new HostDeleteServer(server.getId())));
		Tables.getSTable().delete(server);
	}
}
