package net.rezxis.mchosting.sync.managers.anni;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.network.packet.bungee.BungAnniStart;
import net.rezxis.mchosting.network.packet.host.HostAnniStart;
import net.rezxis.mchosting.network.packet.sync.SyncAnniServerStatusSigns;
import net.rezxis.mchosting.sync.managers.SyncManager;

public class AnniManager {

	private static final CopyOnWriteArrayList<StatusSignInfo> servers = new CopyOnWriteArrayList<StatusSignInfo>();
	private static Gson gson = new Gson();
	private static ServerTable sTable = new ServerTable();
	private static int current = 1;
	
	public static void packetAnniServerStatusSigns(WebSocket conn, String packet) {
		SyncAnniServerStatusSigns pack = gson.fromJson(packet, SyncAnniServerStatusSigns.class);
		StatusSignInfo target = search(pack.getServerName());
		if(target==null) {
			getServerList().add(new StatusSignInfo(pack.getServerName(),pack.isJoinable(),pack.getMaxPlayers(),pack.getOnlinePlayers(),true,pack.getIp(),pack.getPort(),pack.getIcon(),pack.getLine1(),pack.getLine2(),pack.getLine3(),pack.getLine4(),pack.getLastUpdated(),conn));
		}else {
			target.update(pack.getServerName(),pack.isJoinable(),pack.getMaxPlayers(),pack.getOnlinePlayers(),true,pack.getIp(),pack.getPort(),pack.getIcon(),pack.getLine1(),pack.getLine2(),pack.getLine3(),pack.getLine4(),pack.getLastUpdated(),conn);
		}
	}
	
	public static void startGame() {
		int port = current;
		++port;
		String serverName = "ANNI_"+port;
		if(search(port)==null) {
			StatusSignInfo stat=new StatusSignInfo(serverName,port);
			stat.setOnline(true);
			servers.add(stat);
		}else {
			StatusSignInfo stat=search(port);
			stat.setOnline(true);
			stat.setConnected(false);
		}
		System.out.println("Started game : "+serverName + " on "+port);
		SyncManager.hosts.get(1).send(gson.toJson(new HostAnniStart(port)));
		SyncManager.bungee.send(gson.toJson(new BungAnniStart(serverName)));
	}
	
	public static ServerTable getServerTable() {
		return sTable;
	}
	
	public static CopyOnWriteArrayList<StatusSignInfo> getServerList() {
		return servers;
	}

	public static StatusSignInfo search(WebSocket sock) {
		for(StatusSignInfo e:getServerList()) {
			if(e.getSocket()!=null) {
				if(e.getSocket().equals(sock)) {
					return e;
				}
			}
		}
		return null;
	}
	
	public static StatusSignInfo search(int port) {
		for(StatusSignInfo e:getServerList()) {
			if(e.getPort()==port) {
				return e;
			}
		}
		return null;
	}


	public static StatusSignInfo search(String s) {
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().equals(s)) {
				return e;
			}
		}
		return null;
	}

	public static ArrayList<StatusSignInfo> searchContainsName(String fillter) {
		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().contains(fillter)) {
				list.add(e);
			}
		}
		return list;
	}
	public static ArrayList<StatusSignInfo> searchContainsNameAndOnline(String fillter) {
		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:getServerList()) {
			if(e.getServerName().contains(fillter)&&e.isOnline()) {
				list.add(e);
			}
		}
		return list;
	}

	public static ArrayList<StatusSignInfo> searchNonConnectedServer(String fillter) {

		ArrayList<StatusSignInfo> list=new ArrayList<StatusSignInfo>();
		for(StatusSignInfo e:searchContainsName(fillter)) {
			if(!e.isConnected()) {
				list.add(e);
			}
		}
		return list;
	}

	public static StatusSignInfo findOffLine() {
		for(StatusSignInfo e:getServerList()) {
			if(!e.isOnline()) {
				return e;
			}
		}
		return null;
	}
}
