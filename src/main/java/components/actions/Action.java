package components.actions;

import java.util.Collection;

import javax.annotation.Nullable;

import components.base.CustomMessageEmbed;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.RestAction;

public class Action extends IOptionHolder {
	
	private Guild guild = null;
	private User user = null;
	private Member member = null;
	private Message message = null;
	private TextChannel channel = null;
	private SubAction subAction = null;

	public Action(Guild guild, User user, Message message, @Nullable SubAction subAction, @Nullable Object[] options) {
		this.guild = guild;
		this.user = user;
		this.member = guild.getMember(user);
		this.message = message;
		this.channel = message.getTextChannel();
		this.subAction = subAction;
		this.options = options;
	}
	
	public Action(Member member, Message message, @Nullable SubAction subAction, @Nullable Object[] options) {
		this.guild = member.getGuild();
		this.user = member.getUser();
		this.member = member;
		this.message = message;
		this.channel = message.getTextChannel();
		this.subAction = subAction;
		this.options = options;
	}
	
	public Guild getGuild() {
		return this.guild;
	}
	
	public User getUser() {
		return this.user;
	}
	
	public Member getMember() {
		return this.member;
	}
	
	public Message getMessage() {
		return this.message;
	}
	
	public TextChannel getChannel() {
		return this.channel;
	}
	
	public SubAction getSubAction() {
		return this.subAction;
	}
	
	public Object[] getOptions() {
		return this.options;
	}
	
	public RestAction<Message> replyEmbeds(MessageEmbed embed) {
		return this.message.editMessageEmbeds(embed);
	}
	
	public RestAction<Message> replyEmbeds(CustomMessageEmbed embed) {
		return this.message.editMessageEmbeds(embed.convert());
	}
	
	public RestAction<Message> replyEmbeds(Collection<MessageEmbed> embeds) {
		return this.message.editMessageEmbeds(embeds);
	}
}