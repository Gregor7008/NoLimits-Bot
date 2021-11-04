package commands.utilities;

import commands.Command;
import components.base.AnswerEngine;
import components.base.Configloader;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class Levelbackground implements Command{

	@Override
	public void perform(SlashCommandEvent event) {
		if (event.getSubcommandName().equals("set")) {
			Level lv = new Level();
			if (Integer.parseInt(event.getOption("number").getAsString()) > 4 || Integer.parseInt(event.getOption("number").getAsString()) < 0) {
				event.replyEmbeds(AnswerEngine.getInstance().fetchMessage(event.getGuild(), event.getUser(),"/commands/utilities/levelbackground:wrongarg")).queue();
			} else {
				Configloader.INSTANCE.setUserConfig(event.getMember(), "levelbackground", event.getOption("number").getAsString());
				event.replyEmbeds(AnswerEngine.getInstance().fetchMessage(event.getGuild(), event.getUser(),"/commands/utilities/levelbackground:success")).addFile(lv.renderLevelcard(event.getMember())).queue();
			}
			return;
		}
		if (event.getSubcommandName().equals("list")) {
			this.listlevelcards(event);
		}
	}

	@Override
	public CommandData initialize() {
		CommandData command = new CommandData("levelbackground", "Configure your personal levelbackground")
									.addSubcommands(new SubcommandData("set", "Set your new levelbackground").addOption(OptionType.INTEGER, "number", "The number of your new levelbackground", true))
									.addSubcommands(new SubcommandData("list", "List all possible backgrounds"));
		return command;
	}

	@Override
	public String getHelp() {
		return "Configure your personal levelbackground so it will be displayed, whenever you use /level";
	}
	
	private void listlevelcards(SlashCommandEvent event) {
		EmbedBuilder eb0 = new EmbedBuilder();
		EmbedBuilder eb1 = new EmbedBuilder();
		EmbedBuilder eb2 = new EmbedBuilder();
		EmbedBuilder eb3 = new EmbedBuilder();
		EmbedBuilder eb4 = new EmbedBuilder();
		
		eb0.setTitle("Levelbackground 0 (default)");
		eb1.setTitle("Levelbackground 1");
		eb2.setTitle("Levelbackground 2");
		eb3.setTitle("Levelbackground 3");
		eb4.setTitle("Levelbackground 4");
		
		try {
			eb0.setImage("https://i.ibb.co/J72srcG/image.png");
			eb1.setImage("https://i.ibb.co/HNgMsQT/1.png");
			eb2.setImage("https://i.ibb.co/GttGfPZ/2.png");
			eb3.setImage("https://i.ibb.co/r3rHJzV/3.png");
			eb4.setImage("https://i.ibb.co/jDwc9PG/4.png");
		} catch (IllegalArgumentException e) {}
		
		event.replyEmbeds(eb0.build(), eb1.build(), eb2.build(), eb3.build(), eb4.build()).queue();
	}
}