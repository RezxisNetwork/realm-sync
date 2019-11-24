package net.rezxis.mchosting.sync.discord;

import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class JDAListener implements EventListener {

	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof MessageReceivedEvent) {
			MessageReceivedEvent me = (MessageReceivedEvent) event;
			String msg = me.getMessage().getContentRaw();
			if (msg.startsWith("*status")) {
				
			} else if (msg.startsWith("*help")) {
				
			}
		}
	}
}
