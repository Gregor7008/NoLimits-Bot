package slash_commands.music;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import base.engines.LanguageEngine;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import slash_commands.assets.SlashCommandEventHandler;
import slash_commands.engines.GuildMusicManager;
import slash_commands.engines.PlayerManager;

public class Queue implements SlashCommandEventHandler {

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		final Member member = event.getMember();
		final Guild guild = event.getGuild();
		final Member self = guild.getSelfMember();
		final User user = event.getUser();
		final GuildMusicManager musicManager = PlayerManager.getInstance().getMusicManager(guild);
		final BlockingQueue<AudioTrack> queue = musicManager.scheduler.queue;
		if (!self.getVoiceState().inAudioChannel()) {
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "notconnected")).queue();
			return;
		}
		if (member.getVoiceState().getChannel() != self.getVoiceState().getChannel()) {
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "nopermission")).queue();
			return;
		}
		if (queue.isEmpty()) {
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "noqueue")).queue();
			return;
		}
		
		final int trackCount = Math.min(queue.size(), 20);
		final List<AudioTrack> trackList = new ArrayList<>(queue);
		final StringBuilder sB = new StringBuilder();
		final String temp1[] = LanguageEngine.getRaw(guild, user, this, "list").split(";");
		
		for (int i = 0; i < trackCount; i++) {
			final AudioTrack track = trackList.get(i);
			final AudioTrackInfo info = track.getInfo();
			
			sB.append('#')
			  .append(String.valueOf(i+1) + " ")
			  .append(temp1[0].replace("{title}", info.title).replace("{author}", info.author))
			  .append("[")
			  .append(formatTime(track.getDuration()));
		   if (i+1 != trackCount) {
			   sB.append("]\n");
		   } else {
			   sB.append("]");
		   }
		}
		if(trackList.size() > trackCount) {
			sB.append(temp1[1].replace("{count}", String.valueOf(trackList.size() - trackCount)));
		}
		event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, this, "queue").replaceDescription("{list}", sB.toString())).queue();
	}

	@Override
	public CommandData initialize() {
		CommandData command = Commands.slash("queue", "Displays the current music queue!");
		command.setDefaultPermissions(DefaultMemberPermissions.ENABLED)
		   .setGuildOnly(true);
		return command;
	}

	private String formatTime(long timeInMillis) {
		final long hours = timeInMillis / TimeUnit.HOURS.toMillis(1);
		final long minutes = timeInMillis / TimeUnit.MINUTES.toMillis(1);
		final long seconds = timeInMillis % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}
}