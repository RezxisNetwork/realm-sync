package net.rezxis.mchosting.sync.managers.anni;

import java.util.ArrayList;
import java.util.TimerTask;

public class GameMonitor extends TimerTask {

	public static final char COLOR_CHAR = '§';
	
	@Override
	public void run() {
		int lobbyPlayers=-1;
		ArrayList<StatusSignInfo> games=AnniManager.searchContainsNameAndOnline("ANNI");
		int onlineGames=games.size();
		boolean hasJoinable=false;


		for(StatusSignInfo i:games) {
			if(i.getLine2()!=null) {
				if(i.getLine2().equals("on")||i.getLine2().equals("")) {
					hasJoinable=true;
				}
			}
			if(i.getLine4()!=null) {
				if(!i.getLine4().startsWith(COLOR_CHAR+"e")) {
					hasJoinable=true;
				}
			}
		}


		for(StatusSignInfo i:AnniManager.searchContainsNameAndOnline("LOBBY")) {
			if(lobbyPlayers==-1) {
				lobbyPlayers=0;
			}
			lobbyPlayers+=i.getOnlinePlayers();
		}

		if(lobbyPlayers!=-1) {

			if(AnniManager.searchNonConnectedServer("ANNI").size()==0)
			{

				if(onlineGames==0) {
					System.out.println("online games are zero. starting new server.");
					AnniManager.startGame();
				}else if(!hasJoinable) {
					System.out.println("joinable server not found. starting new server.");
					AnniManager.startGame();
				}
				if(onlineGames==0&&hasJoinable) {
					System.out.println("onlineGames are zero but db has joinable server.");
				}

			}else {
				System.out.println("Booting Server found. server monitor is temporarily disabled.");
			}

		}else {
			System.out.println("LOBBY Server not found.");
		}
		AnniManager.getServerTable().check();
		for(StatusSignInfo i:AnniManager.getServerList()) {
			if(i.isConnected()) {
				AnniManager.getServerTable().update(i);
			}
		}
	}
}