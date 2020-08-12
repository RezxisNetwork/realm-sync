package net.rezxis.mchosting.sync.discord;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.internal.requests.Route.Emotes;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.database.object.server.DBServer;
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
	private static HashMap<String,String> descs = new HashMap<>();
	
	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof MessageReceivedEvent) {
			MessageReceivedEvent me = (MessageReceivedEvent) event;
			if (me.getTextChannel().getTopic() != null) {
				if (me.getTextChannel().getTopic().equalsIgnoreCase("ticket")) {
					if (me.getMessage().getContentRaw().startsWith("/close")) {
						me.getTextChannel().delete().queue();
					}
				}
			}
			if (me.getChannel().getName().equalsIgnoreCase("discord-link")) {
				String msg = me.getMessage().getContentRaw();
				if (!msg.startsWith("/link")) {
					if (me.getAuthor().getIdLong() != me.getJDA().getSelfUser().getIdLong())
						me.getMessage().delete().queue();
					return;
				}
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
				me.getChannel().sendMessage(Tables.getUTable().get(player.getUUID()).getName()+"とリンクされました。").queue(
						message -> {
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							message.delete().queue();
						});
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
			if (e.getUserIdLong() == e.getJDA().getSelfUser().getIdLong())
				return;
			System.out.println(e.getReactionEmote().getAsCodepoints());
			if (System.currentTimeMillis() - times.getOrDefault(e.getUserIdLong(), 0L) < 3000) {
				e.retrieveUser().queue(u -> {e.getReaction().removeReaction(u).queue();});
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
										try {
											Thread.sleep(5000);
											message.delete().queue();
										} catch (InterruptedException e1) {
											e1.printStackTrace();
										}
									});
						} else {
							e.retrieveUser().queue(u -> {
								for (TextChannel tch : e.getGuild().getTextChannels()) {
									if (tch.getName().equalsIgnoreCase(u.getName()+"-"+u.getDiscriminator())) {
										e.getTextChannel().sendMessage(u.getAsMention()+"すでにTicketが作成されています。").queue(messagee -> {
											try {
												Thread.sleep(5000);
											} catch (InterruptedException e1) {
												// TODO Auto-generated catch block
												e1.printStackTrace();
											}
											messagee.delete().queue();
										});
										e.getReaction().removeReaction(u).queue();
										return;
									}
								}
								ChannelAction<TextChannel> ca = e.getGuild().createTextChannel(u.getName()+"-"+u.getDiscriminator())
								.setTopic("ticket")
								.setParent(e.getGuild().getCategoryById(743260506529202286L));
								ArrayList<Permission> deny = new ArrayList<>();
								deny.add(Permission.MESSAGE_READ);
								deny.add(Permission.MESSAGE_WRITE);
								ArrayList<Permission> allow = new ArrayList<>();
								allow.add(Permission.MESSAGE_READ);
								allow.add(Permission.MESSAGE_WRITE);
								allow.add(Permission.MESSAGE_ATTACH_FILES);
								Long adminRole = 573179356273442817L;
								ca.addRolePermissionOverride(adminRole, allow, new ArrayList<>());
								ca.addRolePermissionOverride(517992113124671508L, new ArrayList<>(), deny);
								ca.queue(tch -> {
									EmbedBuilder eb = new EmbedBuilder();
									eb.setDescription("チケットを作成しました。");
									eb.setColor(0x00ff00);
									eb.addField("作成者", u.getAsMention(), true);
									eb.addField("チケット", tch.getAsMention(), true);
									eb.addField("要件", descs.get(ee.getKey()), true);
									eb.addField("MCID", Tables.getUTable().get(p.getUUID()).getName(), false);
									eb.addField("UUID", p.getUUID().toString(), false);
									eb.addField("PID", String.valueOf(p.getId()), false);
									DBServer s = Tables.getSTable().get(p.getUUID());
									eb.addField("SID", s != null ? String.valueOf(s.getId()+":"+s.getDisplayName()) : "サーバーなし", false);
									StringBuilder sb = new StringBuilder();
									sb.append("Ticketを作成しました。要件を入力し、運営の対応をお待ちください。Ticketをクローズするには、`/close`と送信してください。");
									tch.sendMessage(sb.toString()).queue();
									tch.sendMessage(eb.build()).queue();
								});
							});
						}
					}
				}
			}
			e.retrieveUser().queue(u -> {e.getReaction().removeReaction(u).queue();});
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
		descs.put("a", "処罰解除申請");
		descs.put("b", "「IPアドレスがブロックされています」と表示されてログインできない");
		descs.put("c", "サーバーが起動中のまま/終了中のまま");
		descs.put("d", "バグ報告");
		descs.put("e", "ルール違反者報告");
		descs.put("f", "その他(作成されたチャンネルで用件を話してください)");
		descs.put("g", "Adminのみが閲覧可能なTicketを作成する");
	}
}
