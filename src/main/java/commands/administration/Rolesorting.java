package commands.administration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import components.base.LanguageEngine;
import components.commands.Command;
import components.utilities.ResponseDetector;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class Rolesorting implements Command {
	
	private SlashCommandInteractionEvent oevent;
	private Role grouprole;
	private List<Role> subroles;
	private List<Member> members;
	private Guild guild;
	private User user;
	private TextChannel channel;
	private List<Message> messages = new ArrayList<>();
	
	@Override
	public void perform(SlashCommandInteractionEvent event) {
		oevent = event;
		guild = event.getGuild();
		user = event.getUser();
		channel = event.getTextChannel();
		this.definegroup();
	}

	@Override
	public CommandData initialize() {
		CommandData command = Commands.slash("rolesort", "Adds and removes roles by other roles (If member has a role, give him another role)");
		return command;
	}

	@Override
	public boolean canBeAccessedBy(Member member) {
		return member.hasPermission(Permission.MANAGE_ROLES);
	}

	private void definegroup() {
		messages.add(channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "definegroup").convert()).complete());
		ResponseDetector.waitForMessage(guild, user, channel,
				e -> {messages.add(e.getMessage());
				      grouprole = e.getMessage().getMentions().getRoles().get(0);
				      this.definesub();},
			   () -> {this.cleanup();
				   	  channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "timeout").convert()).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
	}
	
	private void definesub() {
		messages.add(channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "definesub").convert()).complete());
		ResponseDetector.waitForMessage(guild, user, channel,
							e -> {messages.add(e.getMessage());
								  subroles = e.getMessage().getMentions().getRoles();
								  this.definemember();},
							() -> {this.cleanup();
								   channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "timeout").convert()).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
	}

	private void definemember() {
		messages.add(channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "definemember").convert()).complete());
		ResponseDetector.waitForMessage(guild, user, channel,
							e -> {messages.add(e.getMessage());
								  members = e.getMessage().getMentions().getMembers();
								  this.rolesorter();},
							() -> {this.cleanup();
								   channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "timeout").convert()).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
	}
	
	private void rolesorter() {
		for (int e = 0; e<members.size(); e++) {
			this.sorter(guild, members.get(e), subroles, grouprole);
		}
		this.cleanup();
		channel.sendMessageEmbeds(LanguageEngine.fetchMessage(guild, user, this, "success").convert()).queue(response -> response.delete().queueAfter(10, TimeUnit.SECONDS));
	}
	
	public void sorter(Guild iguild, Member mb, List<Role> sr, Role gr) {
			int match = 0;
			for (int i = 0; i < mb.getRoles().size(); i++) {
				if (sr.contains(mb.getRoles().get(i))) {
					match++;
				}
			}
			if (match > 0 && !mb.getRoles().contains(gr)) {
				iguild.addRoleToMember(mb, gr).queue();
			}
			if (match == 0 && mb.getRoles().contains(gr)) {
				iguild.removeRoleFromMember(mb, gr).queue();
			}
	}
	
	private void cleanup() {
		oevent.getHook().deleteOriginal().queue();
		channel.deleteMessages(messages).queue();
	}
}