package net.rezxis.mchosting.sync;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import net.rezxis.mchosting.network.ServerHandler;
import net.rezxis.mchosting.sync.managers.anni.AnniManager;
import net.rezxis.mchosting.sync.managers.anni.StatusSignInfo;


public class WSServerHandler implements ServerHandler {
	
	public static ExecutorService pool = null;
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("A connection was established");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println("closed / code : "+code+" / reason : "+reason+" / remote : "+remote);
		StatusSignInfo info = AnniManager.search(conn);
		if (info != null) {
			info.setJoinable(false);
			info.setOnline(false);
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		pool.submit(new WorkerTask(conn,message));
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public void onMessage(WebSocket conn, ByteBuffer buffer) {
	}

	@Override
	public void onStart() {
		pool = Executors.newFixedThreadPool(5);
		System.out.println("Started Sync Server.");
	}
}
