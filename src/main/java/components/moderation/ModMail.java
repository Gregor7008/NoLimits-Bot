package components.moderation;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import base.Bot;
import components.base.AnswerEngine;
import components.base.ConfigLoader;
import components.base.assets.ConfigManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class ModMail {
	
	public static final Guild guild = Bot.INSTANCE.jda.getGuildById(Bot.homeID);

	public ModMail(MessageReceivedEvent event, boolean direction) {
		new Thread(() -> {
			this.processEvent(event, direction);
		}).start();
	}
	
	private void processEvent(MessageReceivedEvent event, boolean direction) {
		User user = event.getAuthor();
		if (user.isBot()) {
			return;
		}
		if (guild == null) {
			return;
		}
		if (direction) {
			if (ConfigLoader.run.getModMailOfChannel(event.getChannel().getId()) != null) {
				PrivateChannel pc = Bot.INSTANCE.jda.openPrivateChannelById(ConfigLoader.run.getModMailOfChannel(event.getChannel().getId())).complete();
				this.resendMessage(pc, event.getMessage());
			}
			return;
		}
		boolean member = false;
		try {
			if (guild.retrieveMember(user).complete() != null) {
				member = true;
			}
		} catch (ErrorResponseException e) {}
		if (guild.retrieveBan(user).complete() == null && !member) {
			event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(null, null, "/components/moderation/modmail:nosupport").convert()).queue();
			return;
		}
		if (ConfigLoader.run.getModMailOfUser(user.getId()) != null) {
			TextChannel channel = guild.getTextChannelById(ConfigLoader.run.getModMailOfUser(user.getId()));
			this.resendMessage(channel, event.getMessage());
			return;
		}
		OffsetDateTime lastmail = OffsetDateTime.parse(ConfigLoader.run.getUserConfig(guild, user).getString("lastmail"), ConfigManager.dateTimeFormatter);
		if (Duration.between(lastmail, OffsetDateTime.now()).toSeconds() > 300) {
			event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:success").convert()).queue();
			ConfigLoader.run.getUserConfig(guild, user).put("lastmail", OffsetDateTime.now().format(ConfigManager.dateTimeFormatter));
			this.processMessage(event);
		} else {
			int timeleft = (int) (300 - Duration.between(lastmail, OffsetDateTime.now()).toSeconds());
			event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:timelimit")
					.replaceDescription("{timeleft}", String.valueOf(timeleft)).convert()).queue();
		}
	}
	
	private void processMessage(MessageReceivedEvent event) {
		if (ConfigLoader.run.getGuildConfig(guild).getLong("supportcategory") == 0) {
			Category ctg = guild.createCategory("----------📝 Tickets ------------").complete();
			ConfigLoader.run.getGuildConfig(guild).put("supportcategory", ctg.getIdLong());
		}
		TextChannel nc = guild.createTextChannel(event.getAuthor().getName(), guild.getCategoryById(ConfigLoader.run.getGuildConfig(guild).getLong("supportcategory"))).complete();
		this.resendMessage(nc, event.getMessage());
		nc.sendMessage(guild.getPublicRole().getAsMention());
		nc.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
		ConfigLoader.run.setModMailConfig(nc.getId(), event.getAuthor().getId());
	}
	
	private void resendMessage(MessageChannel channel, Message message) {
		List<Attachment> attachements = message.getAttachments();
		List<File> files = new ArrayList<>();
		for (int i = 0; i < attachements.size(); i++) {
			File file = null;
			try {file = File.createTempFile(attachements.get(i).getFileName(), null);
			} catch (IOException e) {}
			Boolean deleted = true;
			if (file.exists()) {
				deleted = file.delete();
			}
			if (deleted) {
				try {
					attachements.get(i).downloadToFile(file).get();
				} catch (InterruptedException | ExecutionException e) {}
				files.add(file);
			}
		}
		channel.sendMessage(message).queue();
		for (int i = 0; i < files.size(); i++) {
			File file = files.get(i);
			channel.sendFile(file).queue(e -> file.delete());
		}
	}
}