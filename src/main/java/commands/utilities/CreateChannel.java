package commands.utilities;

import java.util.Collection;
import java.util.LinkedList;

import org.json.JSONArray;

import commands.Command;
import components.base.AnswerEngine;
import components.base.ConfigLoader;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CreateChannel implements Command{
	
	@Override
	public void perform(SlashCommandInteractionEvent event) {
		Guild guild = event.getGuild();
		User user = event.getUser();
		String name = event.getOption("name").getAsString();
		if (ConfigLoader.run.getGuildConfig(guild).getJSONArray("customchannelroles").isEmpty()) {
			event.replyEmbeds(AnswerEngine.build.fetchMessage(guild, user, "/commands/utilities/createchannel:norole").convert()).queue();
			return;
		}
		JSONArray cccroles = ConfigLoader.run.getGuildConfig(guild).getJSONArray("customchannelroles");
		for (int i = 0; i < cccroles.length(); i++) {
			if (!event.getMember().getRoles().contains(guild.getRoleById(cccroles.getLong(i))) && !guild.getRoleById(cccroles.getLong(i)).isPublicRole()) {
				event.replyEmbeds(AnswerEngine.build.fetchMessage(guild, user, "/commands/utilities/createchannel:nopermission").convert()).queue();
				return;
			}
		}
		this.createTextChannel(guild, user, name);
		event.replyEmbeds(AnswerEngine.build.fetchMessage(guild, user,"/commands/utilities/createchannel:success").convert()).queue();
	}

	@Override
	public CommandData initialize() {
		CommandData command = Commands.slash("createchannel", "Creates a custom channel for you and your friends!")
				.addOption(OptionType.STRING, "name", "The name of the new channel", true);
		return command;
	}

	@Override
	public String getHelp(Guild guild, User user) {
		return AnswerEngine.build.getRaw(guild, user, "/commands/utilities/createchannel:help");
	}
	
	private void createTextChannel(Guild guild, User user, String name) {
		Collection<Permission> perms = this.setupPerms();
		Category cgy;
		if (ConfigLoader.run.getMemberConfig(guild, user).getLong("customchannelcategory") == 0) {
			cgy = guild.createCategory(user.getName() + "'s channels").complete();
			cgy.upsertPermissionOverride(guild.getPublicRole()).setDenied(Permission.VIEW_CHANNEL).queue();
			cgy.upsertPermissionOverride(guild.getMember(user)).setAllowed(perms).queue();
			ConfigLoader.run.getGuildConfig(guild).getJSONArray("customchannelcategories").put(cgy.getId());
			if (!ConfigLoader.run.getGuildConfig(guild).getJSONArray("customchannelaccessroles").isEmpty()) {
				JSONArray defroles = ConfigLoader.run.getGuildConfig(guild).getJSONArray("customchannelaccessroles");
				for (int i = 0; i < defroles.length(); i++) {
					cgy.upsertPermissionOverride(guild.getRoleById(defroles.getLong(i))).setAllowed(Permission.ALL_PERMISSIONS).queue();
				}
			}
			ConfigLoader.run.getMemberConfig(guild, user).put("customchannelcategory", cgy.getIdLong());
  	    } else {
  	    	cgy = guild.getCategoryById(ConfigLoader.run.getMemberConfig(guild, user).getLong("customchannelcategory"));
  	    }
		guild.createTextChannel(name, cgy).complete();
	}
	
	private LinkedList<Permission> setupPerms() {
		LinkedList<Permission> ll = new LinkedList<>();
		ll.add(Permission.VIEW_CHANNEL);
		ll.add(Permission.MANAGE_CHANNEL);
		ll.add(Permission.MANAGE_WEBHOOKS);
		Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS).forEach(e -> ll.add(e));
		Permission.getPermissions(Permission.ALL_VOICE_PERMISSIONS).forEach(e -> ll.add(e));
		return ll;
	}
}