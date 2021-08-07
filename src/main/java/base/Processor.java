package base;

import java.util.concurrent.TimeUnit;

import commands.CommandList;
import commands.Commands;
import components.AnswerEngine;
import components.Automatic;
import components.Test;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class Processor extends ListenerAdapter {
	
	String argument;
	
	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getMessage().getAuthor().isBot()) {return;}
		if (event.getMessage().getContentRaw().startsWith(Bot.INSTANCE.getConfig("prefix"))) {			
			String[] raw = event.getMessage().getContentRaw().split("\\s+", 2);
			String[] command = raw[0].split("(?<=" + Bot.INSTANCE.getConfig("prefix") + ")");
			try {argument = raw[1];
			} catch (Exception e) {argument = null;}
			
			CommandList commandList = new CommandList();
			Commands mdc;
			if((mdc = commandList.CommandList.get(command[1])) != null) {
				mdc.perform(event, argument);
				return;
			}
			
			switch(command[1]) {
				case("shutdown"):
					event.getMessage().delete().queue();
					Bot.INSTANCE.shutdown();
					break;
				case("test"):
					new Test(event, argument);
					break;
				default:
					AnswerEngine.getInstance().fetchMessage("/base/processor:unknown", event).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));
			}
		} else { 
				new Automatic(event);
		}
	}
}
