package dev.gnomebot.app.discord;

import com.google.gson.JsonObject;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.util.HashMap;
import java.util.Optional;

/**
 * @author LatvianModder
 */
public class UserCache {
	private final DiscordHandler handler;
	private final HashMap<Snowflake, Optional<User>> map = new HashMap<>();

	public UserCache(DiscordHandler h) {
		handler = h;
	}

	public int getCacheSize() {
		return map.size();
	}

	public Optional<User> get(final Snowflake id) {
		return map.computeIfAbsent(id, snowflake -> Optional.ofNullable(handler.getUser(snowflake)));
	}

	public String getUsername(final Snowflake id) {
		return get(id).map(User::getUsername).orElse("Deleted User");
	}

	public String getTag(final Snowflake id) {
		return get(id).map(User::getTag).orElse("Deleted User#0000");
	}

	public JsonObject getJson(final Snowflake id) {
		JsonObject json = new JsonObject();
		json.addProperty("id", id.asString());
		json.addProperty("name", getUsername(id));
		return json;
	}
}