package net.rezxis.mchosting.sync.discord;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.internal.requests.Route.Emotes;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.network.packet.all.ExecuteScriptPacket;
import net.rezxis.mchosting.sync.SyncServer;
import net.rezxis.mchosting.sync.managers.SyncManager;
import net.rezxis.mchosting.sync.task.tasks.JDAGameTask;
import net.rezxis.utils.scripts.ScriptEngineLauncher;

public class JDAListener implements EventListener {

	private Gson gson = new Gson();
	private HashMap<Long,Long> times = new HashMap<>();
	private String ticketChannel;
	private long msgId;
	private static final String ret = "\n";
	private static HashMap<String,String> emoji = new HashMap<>();
	
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
			for (TextChannel ch : event.getJDA().getGuildById("517992113124671508").getTextChannels()) {
				if (ch != null)
					if (ch.getTopic() != null)
						if (ch.getTopic().equalsIgnoreCase("rezxis-ticket")) {
							ticketChannel = ch.getId();
							EmbedBuilder eb = new EmbedBuilder();
							eb.setTitle("Ticket");
							String desc = "要件に該当するリアクションをクリックして、Ticketを作成できます。" + ret
									+ "Ticketの作成にはMinecraftアカウントとDiscordアカウントを連携している必要があります。"+ret+ret
									+ ":regional_indicator_a: 処罰解除申請" + ret
									+ ":regional_indicator_b: 「IPアドレスがブロックされています」と表示されてログインできない" + ret
									+ ":regional_indicator_c: サーバーが起動中のまま/終了中のまま" + ret
									+ ":regional_indicator_d: バグ報告" + ret
									+ ":regional_indicator_e: ルール違反者報告" +ret
									+ ":regional_indicator_f: その他(作成されたチャンネルで用件を話してください)" + ret
									+ ":regional_indicator_g: Adminのみが閲覧可能なTicketを作成する" +ret
									+ "Webmoneyでの寄付など、支払いについての問題は:regional_indicator_g:を使用してください。";
							eb.setDescription(desc);
							eb.setColor(Color.blue);
							eb.setFooter("[Rezxis Network]");
							ch.sendMessage(eb.build()).queue(message -> {
								msgId = message.getIdLong();
								message.addReaction(emoji.get("a")).queue();
								message.addReaction(emoji.get("b")).queue();
								message.addReaction(emoji.get("c")).queue();
								message.addReaction(emoji.get("d")).queue();
								message.addReaction(emoji.get("e")).queue();
								message.addReaction(emoji.get("f")).queue();
								message.addReaction(emoji.get("g")).queue();
								ch.getHistoryBefore(message.getIdLong(), 50).queue(h -> {
									for (Message m : h.getRetrievedHistory()) {
										if (m.getIdLong() != message.getIdLong()) {
											m.delete().queue();
										}
									}
								});
							});
						}
			}
		} else if (event instanceof MessageReactionAddEvent) {
			MessageReactionAddEvent e = (MessageReactionAddEvent) event;
			e.getReactionEmote();
			System.out.println(e.getReactionEmote().getAsCodepoints());
			if (System.currentTimeMillis() - times.getOrDefault(e.getUserIdLong(), 0L) < 3000) {
				e.getReaction().removeReaction();
				return;
			}
			times.put(e.getUserIdLong(), System.currentTimeMillis());
			if (e.getMessageIdLong() == msgId) {
				for (Entry<String,String> ee : emoji.entrySet()) {
					if (ee.getValue().equalsIgnoreCase(e.getReactionEmote().getAsCodepoints())) {
						DBPlayer p = Tables.getPTable().getByDiscordId(e.getUserIdLong());
						if (p == null) {
							e.getChannel().sendMessage("Ticketの作成にはMinecraftアカウントとDiscordアカウントを連携している必要があります。").queue(
									message -> {
										message.delete().delay(5, TimeUnit.SECONDS).queue();
									});
						} else {
							//creating channel
						}
					}
				}
			}
			e.getReaction().removeReaction();
		}
	}
	
	static {
		emoji.put("a", "U+1f1e6");
		emoji.put("b", "U+1f1e7");
		emoji.put("c", "U+1f1e8");
		emoji.put("d", "U+1f1e9");
		emoji.put("e", "U+1f1ea");
		emoji.put("f", "U+1f1eb");
		emoji.put("g", "U+1f1ec");
	}
}
