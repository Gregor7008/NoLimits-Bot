package commands.utilities;

import java.util.concurrent.TimeUnit;

import components.ResponseDetector;
import components.Toolbox;
import components.base.ConfigLoader;
import components.base.LanguageEngine;
import components.commands.Command;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

public class Language implements Command {

	@Override
	public void perform(SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		User user = event.getUser();
		SelectMenu menu = SelectMenu.create("sellang")
				.setPlaceholder("Select your language")
				.setRequiredRange(1, 1)
				.addOption("English", "en")
				.addOption("Deutsch", "de")
				.addOption("Español", "es")
				.addOption("Français", "fr")
				.addOption("Dutch", "nl")
				.build();
		InteractionHook reply = event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "chooselang").convert())
				.addActionRow(menu)
				.complete();
		ResponseDetector.waitForMenuSelection(guild, user, reply.retrieveOriginal().complete(), menu.getId(),
				e -> {e.editSelectMenu(menu.asDisabled()).queue();
					  switch (e.getSelectedOptions().get(0).getValue()) {
				      	case "en":
				      		ConfigLoader.getMemberConfig(guild, user).put("language", "en");
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "successen").convert()).queue();
				      		break;
				      	case "de":
				      		ConfigLoader.getMemberConfig(guild, user).put("language", "de");
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "successde").convert()).queue();
				      		break;
				      	case "es":
				      		ConfigLoader.getMemberConfig(guild, user).put("language", "es");
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "successes").convert()).queue();
				      		break;
				      	case "fr":
				      		ConfigLoader.getMemberConfig(guild, user).put("language", "fr");
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "successfr").convert()).queue();
				      		break;
				      	case "nl":
				      		ConfigLoader.getMemberConfig(guild, user).put("language", "nl");
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "successnl").convert()).queue();
				      		break;
				      	default:
				      		e.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "fatal").convert()).queue();
						}
					},
				() -> {Toolbox.disableActionRows(reply.retrieveOriginal().complete());
					   event.getChannel().sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "timeout").convert()).queue(r -> r.delete().queueAfter(3, TimeUnit.SECONDS));});
	}

	@Override
	public CommandData initialize() {
		CommandData command = Commands.slash("language", "Sets your preferred language in which the bot should answer you in on this server");
		return command;
	}

	@Override
	public boolean canBeAccessedBy(Member member) {
		return true;
	}
}