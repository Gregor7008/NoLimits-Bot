package commands.moderation;

import commands.Command;
import components.base.AnswerEngine;
import components.base.Configloader;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class SetSuggestionChannel implements Command{

	@Override
	public void perform(SlashCommandEvent event) {
		final Guild guild = event.getGuild();
		final User user = event.getUser();
		if (!event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
			event.replyEmbeds(AnswerEngine.getInstance().fetchMessage(guild, user,"/commands/moderation/setsuggestionchannel:nopermission")).queue();
			return;
		}
		Configloader.INSTANCE.setGuildConfig(guild, "suggest", event.getOption("channel").getAsGuildChannel().getId());
		event.replyEmbeds(AnswerEngine.getInstance().fetchMessage(guild, user,"/commands/moderation/setsuggestionchannel:successset")).queue();
	}

	@Override
	public CommandData initialize() {
		CommandData command = new CommandData("setsuggestionchannel", "Sets a suggestion channel for your server!")
										.addOptions(new OptionData(OptionType.CHANNEL, "channel", "Mention the channel that should be used", true));	
		return command;
	}

	@Override
	public String getHelp(Guild guild, User user) {
		return AnswerEngine.getInstance().getRaw(guild, user, "/commands/moderation/setsuggestionchannel:help");
	}
}