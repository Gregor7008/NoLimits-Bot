package assets.data.single;

import org.json.JSONObject;

import assets.data.DataContainer;
import engines.base.Check;
import engines.base.LanguageEngine;
import engines.base.Toolbox;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

public class AutoMessageData implements DataContainer {

    private final Guild guild;
    private final TextChannel text_channel;
    private String title, message = "N/A";
    private boolean embedded, activated = false;
    
	public AutoMessageData(Guild guild, JSONObject data) {
	    this.guild = guild;
        this.text_channel = guild.getTextChannelById(data.getLong(Key.CHANNEL_ID));
	    this.instanciateFromJSON(data);
	}
	
	public AutoMessageData(Guild guild, TextChannel channel) {
	    this.guild = guild;
	    this.text_channel = channel;
	}

    @Override
    public DataContainer instanciateFromJSON(JSONObject data) {
        this.title = data.getString(Key.TITLE);
        this.message = data.getString(Key.MESSAGE);
        
        this.embedded = data.getBoolean(Key.EMBEDDED);
        this.activated = data.getBoolean(Key.ACTIVATED);
        
        return this;
    }

    @Override
    public JSONObject compileToJSON() {
        JSONObject compiledData = new JSONObject();
        
        if (Check.isValidChannel(text_channel)) {
            compiledData.put(Key.CHANNEL_ID, text_channel.getIdLong());
            
            compiledData.put(Key.TITLE, this.title);
            compiledData.put(Key.MESSAGE, this.message);
            
            compiledData.put(Key.EMBEDDED, this.embedded);
            compiledData.put(Key.ACTIVATED, this.activated);
        }
        
        return compiledData;
    }
    
    public Guild getGuild() {
        return this.guild;
    }
    
    public TextChannel getTextChannel() {
        return this.text_channel;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public AutoMessageData setTitle(String title) {
        this.title = title;
        return this;
    }
    
    public String getMessage() {
        return this.message;
    }
    
    public AutoMessageData setMessage(String message) {
        this.message = message;
        return this;
    }
    
    public boolean isEmbedded() {
        return this.embedded;
    }
    
    public AutoMessageData setEmbedded(boolean embedded) {
        this.embedded = embedded;
        return this;
    }
    
    public boolean isActivated() {
        return this.activated;
    }
    
    public AutoMessageData setActivated(boolean activated) {
        this.activated = activated;
        return this;
    }
    
    public MessageCreateAction buildMessage(User user) {
        if (activated && (!title.isBlank() || !message.isBlank()) && text_channel != null) {
            String title_edit = Toolbox.processAutoMessage(title, guild, user, false);
            String message_edit = Toolbox.processAutoMessage(message, guild, user, true);
            if (embedded) {
                return text_channel.sendMessageEmbeds(LanguageEngine.buildMessageEmbed(title_edit, message_edit));
            } else {
                return text_channel.sendMessage(LanguageEngine.buildMessage(title_edit, message_edit));
            }
        } else {
            return null;
        }
    }
    
    private static abstract class Key {
        public static final String CHANNEL_ID = "channel_id";
        public static final String TITLE = "title";
        public static final String MESSAGE = "message";
        public static final String EMBEDDED = "embedded";
        public static final String ACTIVATED = "activated";
    }
}