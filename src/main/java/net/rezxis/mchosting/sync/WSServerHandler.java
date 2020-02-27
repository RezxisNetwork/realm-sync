package net.rezxis.mchosting.sync;

import java.nio.ByteBuffer;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import net.rezxis.mchosting.network.ServerHandler;


public class WSServerHandler implements ServerHandler {
	
	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("A connection was established");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println("closed / code : "+code+" / reason : "+reason+" / remote : "+remote);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		new WorkerThread(conn,message).start();
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
	}
}
