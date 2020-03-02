package net.rezxis.mchosting.sync;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.java_websocket.WebSocket;

import com.google.gson.Gson;

import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.server.DBServer;
import net.rezxis.mchosting.database.object.server.DBThirdParty;
import net.rezxis.mchosting.network.packet.Packet;
import net.rezxis.mchosting.network.packet.PacketType;
import net.rezxis.mchosting.network.packet.ServerType;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerMessagePacket;
import net.rezxis.mchosting.network.packet.bungee.BungPlayerSendPacket;
import net.rezxis.mchosting.network.packet.bungee.BungServerStarted;
import net.rezxis.mchosting.network.packet.bungee.BungServerStopped;
import net.rezxis.mchosting.network.packet.host.HostBackupPacket;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket;
import net.rezxis.mchosting.network.packet.host.HostWorldPacket.Action;
import net.rezxis.mchosting.network.packet.sync.SyncBackupPacket;
import net.rezxis.mchosting.network.packet.sync.SyncCustomStarted;
import net.rezxis.mchosting.network.packet.sync.SyncFileLog;
import net.rezxis.mchosting.network.packet.sync.SyncPlayerMessagePacket;
import net.rezxis.mchosting.network.packet.sync.SyncPlayerSendPacket;
import net.rezxis.mchosting.network.packet.sync.SyncThirdPartyPacket;
import net.rezxis.mchosting.network.packet.sync.SyncWorldPacket;
import net.rezxis.mchosting.sync.managers.SyncManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

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
		} else if (type == PacketType.FileLog) {
			SyncFileLog p = gson.fromJson(message, SyncFileLog.class);
			String time = new Date().toString().replace(" ", "-").replace(":", "-");
			File file = new File(new File("files"),p.values.get("download")+"_"+time+"_"+p.values.get("file"));
			try {
				file.createNewFile();
				download(p.values.get("url"),file);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (type == PacketType.World) {
			SyncWorldPacket wp = gson.fromJson(message, SyncWorldPacket.class);
			DBServer server = Tables.getSTable().get(UUID.fromString(wp.values.get("uuid")));
			Action action = null;
			if (wp.action == SyncWorldPacket.Action.DOWNLOAD) {
				action = Action.DOWNLOAD;
			} else {
				action = Action.UPLOAD;
			}
			SyncManager.hosts.get(server.getHost()).send(gson.toJson(new HostWorldPacket(wp.values, action)));
		} else if (type == PacketType.Backup) {
			SyncBackupPacket bp = gson.fromJson(message, SyncBackupPacket.class);
			HostBackupPacket hp = new HostBackupPacket(bp.owner, bp.action, bp.value);
			DBServer server = Tables.getSTable().get(UUID.fromString(bp.owner));
			if (server == null) {
				System.out.println("tried to action back who has no server");
				return;
			}
			SyncManager.hosts.get(server.getHost()).send(gson.toJson(hp));
		} else if (type == PacketType.CustomStart) {
			SyncCustomStarted cp = gson.fromJson(message, SyncCustomStarted.class);
			DBServer target = Tables.getSTable().getByID(cp.getId());
			SyncManager.bungee.send(gson.toJson(new BungServerStarted(target.getDisplayName(),cp.getIp(),target.getPort())));
		} else if (type == PacketType.ThirdParty) {
			SyncThirdPartyPacket stp = gson.fromJson(message, SyncThirdPartyPacket.class);
			DBThirdParty dtp = Tables.getTTable().getByKey(stp.getKey());
			if (stp.getAction() == SyncThirdPartyPacket.Action.START) {
				SyncManager.bungee.send(gson.toJson(new BungServerStarted(dtp.getName(), dtp.getHost(), dtp.getPort())));
			} else {
				SyncManager.bungee.send(gson.toJson(new BungServerStopped(dtp.getName())));
			}
		} else if (type == PacketType.MESSAGE) {
			SyncPlayerMessagePacket mp = gson.fromJson(message, SyncPlayerMessagePacket.class);
			SyncManager.bungee.send(gson.toJson(new BungPlayerMessagePacket(mp.getTarget(),mp.getMessage())));
		}
	}
	
	public static void download(String url, File file) throws Exception {
		Response res = client.newCall(new Request.Builder().url(url).get().build()).execute();
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
			IOUtils.copy(res.body().byteStream(), fos);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			fos.close();
		}
	}
	
	private static OkHttpClient client;
	
	static {
		client = new OkHttpClient.Builder().build();
	}
}
