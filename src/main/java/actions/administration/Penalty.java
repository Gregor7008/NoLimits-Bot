package actions.administration;

import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.jagrosh.jdautilities.commons.waiter.EventWaiter;

import base.Bot;
import components.actions.Action;
import components.actions.ActionData;
import components.actions.ActionRequest;
import components.actions.SubActionData;
import components.base.ConfigLoader;
import components.base.LanguageEngine;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.SelectMenuInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;

public class Penalty implements ActionRequest {
	
	private final EventWaiter waiter = Bot.run.getWaiter();
	private Action event;
	private Message message;
	private User user;
	private Guild guild;

	@Override
	public void execute(Action event) {
		this.message = event.getMessage();
		this.user = event.getUser();
		this.guild = event.getGuild();
		this.event = event;
		if (event.getSubAction().getName().equals("add")) {
			this.addpenalties1(event);
		}
		if (event.getSubAction().getName().equals("remove")) {
			this.removepenalties(event);
		}
		if (event.getSubAction().getName().equals("list")) {
			String response = this.listpenalties(event);
			if (response != null) {
				event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:list").replaceDescription("{list}", response)).queue();
			}
		}
		
	}

	@Override
	public ActionData initialize() {
		ActionData actionData = new ActionData(this).setName("Penalty")
													.setInfo("Configure penalties for reaching a certain warning limit")
													.setMinimumPermission(Permission.BAN_MEMBERS)
													.setCategory(ActionData.ADMINISTRATION)
													.setSubActions(new SubActionData[] {
															new SubActionData("add"),
															new SubActionData("remove"),
															new SubActionData("list")
													});
		return actionData;
	}
	
	private void removepenalties(Action event) {
		String response = this.listpenalties(event);
		if (response != null) {
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:remlist").replaceDescription("{list}", response)).queue();
			JSONObject penalties = ConfigLoader.getGuildConfig(guild).getJSONObject("penalties");
			waiter.waitForEvent(MessageReceivedEvent.class,
					e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;} 
					  	  return e.getAuthor().getIdLong() == user.getIdLong();},
					e -> {try {
						penalties.getJSONArray(e.getMessage().getContentRaw());
						penalties.remove(e.getMessage().getContentRaw());
					} catch (JSONException ex) {
						event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:norem")).queue();
					}},
					1, TimeUnit.MINUTES,
					() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"general:timeout")).queue(r -> r.delete().queueAfter(3, TimeUnit.SECONDS));});
		}
	}
	
	private void addpenalties1(Action event) {
		SelectMenu menu = SelectMenu.create("menu:class")
				.addOption("Removal of role", "rr")
				.addOption("Temporary mute", "tm")
				.addOption("Permanent mute", "pm")
				.addOption("Kick", "ki")
				.addOption("Temporary ban", "tb")
				.addOption("Permanent ban", "pm")
				.build();
		event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:add1")).complete()
			 .editMessageComponents(ActionRow.of(menu)).queue();
		waiter.waitForEvent(SelectMenuInteractionEvent.class,
				e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;} 
				  	  return e.getUser().getIdLong() == user.getIdLong();},
				e -> {String plannedpunish = e.getSelectedOptions().get(0).getValue();
					  this.addpenalties2(plannedpunish);},
				1, TimeUnit.MINUTES,
				() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "general:timeout")).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
	}
	
	private void addpenalties2(String plannedpunish) {
		event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:add2")).queue();
		waiter.waitForEvent(MessageReceivedEvent.class,
				e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;}
					  try {Integer.valueOf(e.getMessage().getContentRaw());} catch (NumberFormatException ex) {return false;}
				  	  return e.getAuthor().getIdLong() == user.getIdLong();},
				e -> {this.addpenalties3(plannedpunish, e.getMessage().getContentRaw());},
				1, TimeUnit.MINUTES,
				() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "general:timeout")).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
	}
	
	private void addpenalties3(String plannedpunish, String warnings) {
		JSONObject penalties = ConfigLoader.getGuildConfig(guild).getJSONObject("penalties");
		try {
			penalties.getJSONArray(warnings);
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:error")).queue();
			return;
		} catch (JSONException e) {}
		switch (plannedpunish) {
		case "rr":
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:add3role")).queue();
			waiter.waitForEvent(MessageReceivedEvent.class,
					e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;} 
						  if(e.getMessage().getMentions().getRoles().isEmpty()) {return false;}
					  	  return e.getAuthor().getIdLong() == user.getIdLong();},
					e -> {penalties.put(warnings, new JSONArray().put(plannedpunish).put(e.getMessage().getMentions().getRoles().get(0).getId()));
						  event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:successrole")).queue();},
					1, TimeUnit.MINUTES,
					() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "general:timeout")).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
			break;
		case "tm":
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:add3time")).queue();
			waiter.waitForEvent(MessageReceivedEvent.class,
					e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;}
					 	  try {Integer.valueOf(e.getMessage().getContentRaw());} catch (NumberFormatException ex) {return false;}
					  	  return e.getAuthor().getIdLong() == user.getIdLong();},
					e -> {penalties.put(warnings, new JSONArray().put(plannedpunish).put(e.getMessage().getContentRaw()));
						  event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:successtempmute")).queue();},
					1, TimeUnit.MINUTES,
					() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"general:timeout")).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
			break;
		case "pm":
			penalties.put(warnings, new JSONArray().put(plannedpunish).put("0"));
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:successmute")).queue();
			break;
		case "ki":
			penalties.put(warnings, new JSONArray().put(plannedpunish).put("0"));
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:successkick")).queue();
			break;
		case "tb":
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:add3time")).queue();
			waiter.waitForEvent(MessageReceivedEvent.class,
					e -> {if(!e.getChannel().getId().equals(message.getChannel().getId())) {return false;}
						  try {Integer.valueOf(e.getMessage().getContentRaw());} catch (NumberFormatException ex) {return false;}
					  	  return e.getAuthor().getIdLong() == user.getIdLong();},
					e -> {penalties.put(warnings, new JSONArray().put(plannedpunish).put(e.getMessage().getContentRaw()));
						  event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:successtempban")).queue();},
					1, TimeUnit.MINUTES,
					() -> {event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"general:timeout")).queue(response -> response.delete().queueAfter(3, TimeUnit.SECONDS));});
			break;
		case "pb":
			penalties.put(warnings, new JSONArray().put(plannedpunish).put("0"));
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user,"/commands/moderation/penalty:successban")).queue();
			break;
		default:
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "general:fatal")).queue();
		}
	}
	
	private String listpenalties(Action event) {
		StringBuilder sB = new StringBuilder();
		JSONObject current = ConfigLoader.getGuildConfig(guild).getJSONObject("penalties");
		if (current.isEmpty()) {
			event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "/commands/moderation/penalty:nopenalties"));
			return null;
		}
		current.keySet().forEach(e -> {
			sB.append("• " + e + ": ");
			JSONArray penalty = current.getJSONArray(e);
			switch (penalty.getString(0)) {
				case "rr":
					sB.append("Remove role " + guild.getRoleById(penalty.getString(1)).getAsMention());
					break;
				case "tm":
					sB.append("Mute for " + penalty.getString(1) + " days");
					break;
				case "pm":
					sB.append("Permanent mute");
					break;
				case "ki":
					sB.append("Kick from server");
					break;
				case "tb":
					sB.append("Ban from server for " + penalty.getString(1) + " days");
					break;
				case "pb":
					sB.append("Permanent ban from server");
					break;
				default:
					event.replyEmbeds(LanguageEngine.fetchMessage(guild, user, "general:fatal")).queue();
			}
			sB.append("\n");
		});
		return sB.toString();
	}
}