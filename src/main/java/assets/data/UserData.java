package assets.data;

import org.json.JSONObject;

import net.dv8tion.jda.api.entities.User;

public class UserData implements DataContainer {

	public UserData(User user, JSONObject rawData) {

	}

    @Override
    public JSONObject compileToJSON() {
        return null;
    }
}