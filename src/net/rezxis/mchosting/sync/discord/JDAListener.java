package net.rezxis.mchosting.sync.discord;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.rezxis.mchosting.database.Tables;
import net.rezxis.mchosting.database.object.player.DBPlayer;
import net.rezxis.mchosting.sync.SyncServer;
import net.rezxis.mchosting.sync.task.tasks.JDAGameTask;

public class JDAListener implements EventListener {

	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof MessageReceivedEvent) {
			MessageReceivedEvent me = (MessageReceivedEvent) event;
			if (me.getChannel().getName().equalsIgnoreCase("discord-link")) {
				String msg = me.getMessage().getContentRaw();
				if (!msg.startsWith("/link")) {
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
					me.getChannel().sendMessage("���łɂ��Ȃ���Discord�̓����N����Ă��܂��B�����N������Ticket�Ŏ󂯕t���܂��B").queue();
					return;
				}
				player = Tables.getPTable().getByVerfiyKey(key);
				if (player == null) {
					me.getChannel().sendMessage("���̃R�[�h�͑��݂��܂���B").queue();
					return;
				}
				if (player.getDiscordId() != -1) {
					me.getChannel().sendMessage("���łɂ��Ȃ���Discord�̓����N����Ă��܂��B�����N������Ticket�Ŏ󂯕t���܂��B").queue();
					return;
				}
				player.setDiscordId(me.getAuthor().getIdLong());
				player.update();
				me.getChannel().sendMessage(Tables.getUTable().get(player.getUUID()).getName()+"�ƃ����N����܂����B").queue();
				return;
			}
		} else if (event instanceof ReadyEvent) {
			SyncServer.rpTask.register("jda", new JDAGameTask());
		}
	}
}
