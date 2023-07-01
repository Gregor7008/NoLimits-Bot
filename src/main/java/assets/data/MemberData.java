package assets.data;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONObject;

import assets.data.single.ModMailData;
import assets.data.single.WarningData;
import base.Bot;
import engines.base.LanguageEngine.Language;
import engines.data.ConfigLoader;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class MemberData implements DataContainer {
    
    private final long guild_id;
    private int experience, last_penalty_index, level, levelcard_background = 0;
    private Language language = Language.ENGLISH;
    private OffsetDateTime last_experience, last_modmail, last_suggestion, temporarily_banned_until = OffsetDateTime.now().minusDays(1L);
    private ConcurrentHashMap<Integer, ModMailData> modmails = new ConcurrentHashMap<>();
    private boolean permanently_muted = false;
    private List<WarningData> warnings = new ArrayList<>();

//  Temporary runtime data
    private int spam_count = 0;
    
    public MemberData(Guild guild, JSONObject data) {
        this.guild_id = guild.getIdLong();
        this.instanciateFromJSON(data);
    }
    
    public MemberData(Guild guild) {
        this.guild_id = guild.getIdLong();
    }
    
    @Override
    public DataContainer instanciateFromJSON(JSONObject data) {
        this.experience = data.getInt(Key.EXPERIENCE);
        this.last_penalty_index = data.getInt(Key.LAST_PENALTY_INDEX);
        this.level = data.getInt(Key.LEVEL);
        this.levelcard_background = data.getInt(Key.LEVELCARD_BACKGROUND);
        
        this.language = Language.valueOf(data.getString(Key.LANGUAGE));
        
        this.last_experience = OffsetDateTime.parse(data.getString(Key.LAST_EXPERIENCE), ConfigLoader.DATA_TIME_SAVE_FORMAT);
        this.last_modmail = OffsetDateTime.parse(data.getString(Key.LAST_MODMAIL), ConfigLoader.DATA_TIME_SAVE_FORMAT);
        this.last_suggestion = OffsetDateTime.parse(data.getString(Key.LAST_SUGGESTION), ConfigLoader.DATA_TIME_SAVE_FORMAT);
        this.temporarily_banned_until = OffsetDateTime.parse(data.getString(Key.TEMPORARILY_BANNED_UNTIL), ConfigLoader.DATA_TIME_SAVE_FORMAT);
        
        permanently_muted = data.getBoolean(Key.PERMANENTLY_MUTED);
        
        JSONObject modmails_object = data.getJSONObject(Key.MODMAILS);
        modmails_object.keySet().forEach(key -> {
           TextChannel channel = this.getGuild().getTextChannelById(modmails_object.getLong(key));
           if (channel != null) {
               modmails.put(Integer.parseInt(key), ConfigLoader.get().getGuildData(this.getGuild()).getModMail(channel));
           }
        });
        modmails.values().removeAll(Collections.singleton(null));
        
        JSONArray warnings_array = data.getJSONArray(Key.WARNINGS);
        for (int i = 0; i < warnings_array.length(); i++) {
            JSONObject warning_object = warnings_array.getJSONObject(i);
            warnings.add(new WarningData(warning_object));
        }
        return this;
    }
    
    @Override
    public JSONObject compileToJSON() {
        JSONObject compiledData = new JSONObject();
        
        compiledData.put(Key.GUILD_NAME, this.getGuild().getName());
        
        compiledData.put(Key.EXPERIENCE, experience);
        compiledData.put(Key.LAST_PENALTY_INDEX, last_penalty_index);
        compiledData.put(Key.LEVEL, level);
        compiledData.put(Key.LEVELCARD_BACKGROUND, levelcard_background);
        
        compiledData.put(Key.LANGUAGE, language.toString());
        
        compiledData.put(Key.LAST_EXPERIENCE, last_experience.format(ConfigLoader.DATA_TIME_SAVE_FORMAT));
        compiledData.put(Key.LAST_MODMAIL, last_modmail.format(ConfigLoader.DATA_TIME_SAVE_FORMAT));
        compiledData.put(Key.LAST_SUGGESTION, last_suggestion.format(ConfigLoader.DATA_TIME_SAVE_FORMAT));
        compiledData.put(Key.TEMPORARILY_BANNED_UNTIL, temporarily_banned_until.format(ConfigLoader.DATA_TIME_SAVE_FORMAT));
        
        compiledData.put(Key.PERMANENTLY_MUTED, permanently_muted);
        
        JSONObject modmails_data = new JSONObject();
        modmails.forEach((ticket_id, modmail) -> modmails_data.put(String.valueOf(ticket_id), modmail.getGuildChannel().getIdLong()));
        compiledData.put(Key.MODMAILS, modmails_data);
        
        JSONArray warnings_data = new JSONArray();
        for (int i = 0; i < warnings.size(); i++) {
            warnings_data.put(i, warnings.get(i).compileToJSON());
        }
        compiledData.put(Key.WARNINGS, warnings_data);
        
        return compiledData;
    }
    
    public long getGuildId() {
        return this.guild_id;
    }
    
    public Guild getGuild() {
        return Bot.getAPI().getGuildById(guild_id);
    }
    
    public int getExperience() {
        return this.experience;
    }
    
    public void setExperience(int experience) {
        this.experience = experience;
    }
    
    public void addToExperience(int additional_experience) {
        this.experience += additional_experience; 
    }
    
    public void removeFromExperience(int deductional_experience) {
        this.experience -= deductional_experience;
    }
    
    public int getLastPenaltyIndex() {
        return this.last_penalty_index;
    }
    
    public void setLastPenaltyIndex(int index) {
        this.last_penalty_index = index; 
    }
    
    public void addToLastPenaltyIndex(int additional_indicies) {
        this.last_penalty_index += additional_indicies; 
    }
    
    public void removeFromLastPenaltyIndex(int deductional_indicies) {
        this.last_penalty_index -= deductional_indicies;
    }
    
    public int getLevel() {
        return this.level;
    }
    
    public void setLevel(int index) {
        this.level = index; 
    }
    
    public void addToLevel(int additional_levels) {
        this.level += additional_levels;  
    }
    
    public void removeFromLevel(int deductional_levels) {
        this.level -= deductional_levels; 
    }
    
    public int getLevelCardBackground() {
        return this.levelcard_background;
    }
    
    public void setLevelCardBackground(int index) {
        this.level = index; 
    }
    
    public void addToLevelCardBackground(int additional_levels) {
        this.level += additional_levels; 
    }
    
    public void removeFromLevelCardBackground(int deductional_levels) {
        this.level -= deductional_levels; 
    }
    
    public int getSpamCount() {
        return this.spam_count;
    }
    
    public void setSpamCount(int index) {
        this.spam_count = index;
    }
    
    public void addToSpamCount(int additional_count) {
        this.spam_count += additional_count;
    }
    
    public void removeFromSpamCount(int deductional_count) {
        this.spam_count -= deductional_count;
    }
    
    public Language getLanguage() {
        return this.language;
    }
    
    public void setLanguage(Language language) {
        this.language = language;
    }
    
    public OffsetDateTime getLastExperience() {
        return last_experience;
    }
    
    public void setLastExperience(OffsetDateTime last_experience) {
        this.last_experience = last_experience;
    }
    
    public void updateLastExperience() {
        this.last_experience = OffsetDateTime.now(); 
    }
    
    public OffsetDateTime getLastModmail() {
        return last_modmail;
    }
    
    public void setLastModmail(OffsetDateTime last_modmail) {
        this.last_modmail = last_modmail; 
    }
    
    public void updateLastModmail() {
        this.last_modmail = OffsetDateTime.now(); 
    }
    
    public OffsetDateTime getLastSuggestion() {
        return last_suggestion;
    }
    
    public void setLastSuggestion(OffsetDateTime last_suggestion) {
        this.last_suggestion = last_suggestion;  
    }
    
    public void updateLastSuggestion() {
        this.last_suggestion = OffsetDateTime.now(); 
    }
    
    public OffsetDateTime isTemporaryBannedUntil() {
        return temporarily_banned_until;
    }
    
    public void setTemporaryBannedUntil(OffsetDateTime temporarily_banned_until) {
        this.temporarily_banned_until = temporarily_banned_until;
    }
    
    public ConcurrentHashMap<Integer, ModMailData> getModmails() {
        return this.modmails;
    }
    
    public ModMailData getModmail(int ticket_id) {
        return this.modmails.get(ticket_id);
    }
    
    public void setModmails(ConcurrentHashMap<Integer, ModMailData> modmails) {
        DataTools.setMap(this.modmails, modmails);
    }
    
    public void addModmails(ModMailData... datas) {
        for (int i = 0; i < datas.length; i++) {
            modmails.put(datas[i].getTicketId(), datas[i]);
        }
    }
    
    public void removeModmails(int... ticket_ids) {
        for (int i = 0; i < ticket_ids.length; i++) {
            modmails.remove(ticket_ids[i]);
        }  
    }
    
    public void removeModmailsByData(ModMailData... datas) {
        for (int i = 0; i < datas.length; i++) {
            modmails.remove(datas[i].getTicketId());
        }
    }
    
    public boolean isPermanentlyMuted() {
        return permanently_muted;
    }
    
    public void setPermanentlyMuted(boolean permanently_muted) {
        this.permanently_muted = permanently_muted;   
    }
    
    public List<WarningData> getWarnings() {
        return this.warnings;
    }
    
    public WarningData getWarning(int index) {
        return warnings.get(index);
    }
    
    public void setWarnings(List<WarningData> warnings) {
        DataTools.setList(this.warnings, warnings);
    }

    public void addWarnings(WarningData... datas) {
        DataTools.addToList(this.warnings, datas);
    }

    public void removeWarnings(int... indicies) {
        DataTools.removeIndiciesFromList(this.warnings, indicies);
    }

    public void removeWarningsByData(WarningData... datas) {
        DataTools.removeValuesFromList(this.warnings, datas);
    }
    
    private static abstract class Key {
        public static final String GUILD_NAME = "name";
        public static final String EXPERIENCE = "experience";
        public static final String LANGUAGE = "language";
        public static final String LAST_EXPERIENCE = "last_experience";
        public static final String LAST_MODMAIL = "last_modmail";
        public static final String LAST_PENALTY_INDEX = "last_penalty_index";
        public static final String LAST_SUGGESTION = "last_suggestion";
        public static final String LEVEL = "level";
        public static final String LEVELCARD_BACKGROUND = "levelcard_background";
        public static final String MODMAILS = "modmails";
        public static final String PERMANENTLY_MUTED = "permanently_muted";
        public static final String TEMPORARILY_BANNED_UNTIL = "temporarily_banned_until";
        public static final String WARNINGS = "warnings";
    }
}