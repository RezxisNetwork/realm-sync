package net.rezxis.mchosting.sync.discord;

import com.google.gson.Gson;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.network.packet.all.ExecuteScriptPacket;
import net.rezxis.mchosting.sync.SyncServer;
import net.rezxis.mchosting.sync.managers.SyncManager;
import net.rezxis.mchosting.sync.task.tasks.JDAGameTask;
import net.rezxis.utils.scripts.ScriptEngineLauncher;

public class JDAListener implements EventListener {

	private Gson gson = new Gson();
	
	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof MessageReceivedEvent) {
			MessageReceivedEvent me = (MessageReceivedEvent) event;
			if (me.getChannel().getName().equalsIgnoreCase("discord-link")) {
				String msg = me.getMessage().getContentRaw();
				if (msg.split(" ").length == 0) {
					me.getChannel().sendMessage("/link <Link Code>").queue();
					return;
				}
				String key = msg.split(" ")[1];
				DBPlayer player = Tables.getPTable().getByDiscordId(me.getAuthor().getIdLong());
				if (player != null) {
					me.getChannel().sendMessage("すでにDiscordとリンクされています。").queue();
					return;
				}
				player = Tables.getPTable().getByVerfiyKey(key);
				if (player == null) {
					me.getChannel().sendMessage("キーは存在しません。").queue();
					return;
				}
				if (player.getDiscordId() != -1) {
					me.getChannel().sendMessage("すでにこのキーはDiscordとリンクされています。").queue();
					return;
				}
				player.setDiscordId(me.getAuthor().getIdLong());
				player.update();
				me.getChannel().sendMessage(Tables.getUTable().get(player.getUUID()).getName()+"とリンクされました。").queue();
				return;
			}
			if (me.getChannel().getName().equalsIgnoreCase("rezxis-server-operation")) {
				String msg = me.getMessage().getContentRaw();
				/*
				 * eval target <script>
				 * eval game id
				 * eval host id
				 */
				String script;
				String url = me.getMessage().getJumpUrl();
				if (msg.startsWith("/eval ")) {
					msg = msg.replace("/eval ", "");
					if (msg.startsWith("sync ")) {
						ScriptEngineLauncher.run(url, msg.replace("sync ", ""));
					} else if (msg.startsWith("host ")) {
						script = msg.replace("host ", "");
						int id = Integer.valueOf(script.split(" ")[0]);
						script = script.replace(id+" ", "");
						SyncManager.hosts.get(id).send(gson.toJson(new ExecuteScriptPacket(url,script)));
					} else if (msg.startsWith("game ")) {
						script = msg.replace("game ", "");
						int id = Integer.valueOf(script.split(" ")[0]);
						script = script.replace(id+" ", "");
						SyncManager.games.get(id).send(gson.toJson(new ExecuteScriptPacket(url,script)));
					} else if (msg.startsWith("bungeecord ")) {
						script = msg.replace("bungeecord ", "");
						SyncManager.bungee.send(gson.toJson(new ExecuteScriptPacket(url,script)));
					} else if (msg.startsWith("lobby")) {
						script = msg.replace("lobby ", "");
						SyncManager.lobby.send(gson.toJson(new ExecuteScriptPacket(url,script)));
					}
				}
			}
		} else if (event instanceof ReadyEvent) {
			SyncServer.rpTask.register("jda", new JDAGameTask());
		}
	}
}
