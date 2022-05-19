package components.moderation;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import base.Bot;
import components.base.AnswerEngine;
import components.base.ConfigLoader;
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
	
	public static final Guild guild = Bot.INSTANCE.jda.getGuildById("958763161362772069");

	public ModMail(MessageReceivedEvent event, boolean direction) {
		User user = event.getAuthor();
		if (user.isBot()) {
			return;
		}
		if (guild == null) {
			return;
		}
		if (direction) {
			if (ConfigLoader.run.getMailConfig1(event.getChannel().getId()) != null) {
				PrivateChannel pc = Bot.INSTANCE.jda.openPrivateChannelById(ConfigLoader.run.getMailConfig1(event.getChannel().getId())).complete();
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
		if (ConfigLoader.run.getMailConfig2(user.getId()) != null) {
			TextChannel channel = guild.getTextChannelById(ConfigLoader.run.getMailConfig2(user.getId()));
			this.resendMessage(channel, event.getMessage());
			return;
		}
		if (!event.getMessage().getContentRaw().contains("anonym")) {
			OffsetDateTime lastmail = OffsetDateTime.parse(ConfigLoader.run.getUserConfig(guild, user, "lastmail"));
			if (Duration.between(lastmail, OffsetDateTime.now()).toSeconds() > 300) {
				event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:success").convert()).queue();
				ConfigLoader.run.setUserConfig(guild, user, "lastmail", OffsetDateTime.now().toString());
				this.processMessage(event);
			} else {
				int timeleft = (int) (300 - Duration.between(lastmail, OffsetDateTime.now()).toSeconds());
				event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:timelimit")
						.replaceDescription("{timeleft}", String.valueOf(timeleft)).convert()).queue();
			}
		} else {
			OffsetDateTime lastmail = OffsetDateTime.parse(ConfigLoader.run.getUserConfig(guild, user, "lastmail"));
			if (Duration.between(lastmail, OffsetDateTime.now()).toSeconds() > 1) {
				event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:successanonym").convert()).queue();
				ConfigLoader.run.setUserConfig(guild, user, "lastmail", OffsetDateTime.now().toString());
				this.processAnonymousMessage(event);
			} else {
				int timeleft = (int) (300 - Duration.between(lastmail, OffsetDateTime.now()).toSeconds());
				event.getChannel().sendMessageEmbeds(AnswerEngine.ae.fetchMessage(guild, user, "/components/moderation/modmail:timelimit")
						.replaceDescription("{timeleft}", String.valueOf(timeleft)).convert()).queue();
			}
		}
	}
	
	private void processMessage(MessageReceivedEvent event) {
		if (ConfigLoader.run.getGuildConfig(guild, "supportcategory").equals("")) {
			Category ctg = guild.createCategory("----------📝 Tickets ------------").complete();
			ConfigLoader.run.setGuildConfig(guild, "supportcategory", ctg.getId());
		}
		TextChannel nc = guild.createTextChannel(event.getAuthor().getName(), guild.getCategoryById(ConfigLoader.run.getGuildConfig(guild, "supportcategory"))).complete();
		nc.sendMessage(event.getMessage()).queue();
		nc.sendMessage(guild.getPublicRole().getAsMention());
		nc.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
		ConfigLoader.run.setMailConfig(nc.getId(), event.getAuthor().getId());
	}
	
	private void processAnonymousMessage(MessageReceivedEvent event) {
		int rn = new Random().nextInt(100);
		if (ConfigLoader.run.getGuildConfig(guild, "supportcategory").equals("")) {
			Category ctg = guild.createCategory("----------📝 Tickets ------------").complete();
			ConfigLoader.run.setGuildConfig(guild, "supportcategory", ctg.getId());
		}
		TextChannel nc = guild.createTextChannel(String.valueOf(rn), guild.getCategoryById(ConfigLoader.run.getGuildConfig(guild, "supportcategory"))).complete();
		this.resendMessage(nc, event.getMessage());
		nc.sendMessage(guild.getPublicRole().getAsMention());
		nc.upsertPermissionOverride(guild.getPublicRole()).deny(Permission.VIEW_CHANNEL).queue();
		ConfigLoader.run.setMailConfig(nc.getId(), event.getAuthor().getId());
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