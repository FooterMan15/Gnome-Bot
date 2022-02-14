package dev.gnomebot.app.script;

import dev.gnomebot.app.data.GuildCollections;
import dev.latvian.mods.rhino.util.DynamicMap;
import dev.latvian.mods.rhino.util.HideFromJS;
import discord4j.common.util.Snowflake;
import discord4j.rest.service.GuildService;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class WrappedGuild implements WithId {
	public final transient DiscordJS discordJS;
	public final transient GuildCollections gc;
	public final WrappedId id;
	public final Map<String, WrappedChannel> channels;
	public final Map<String, WrappedRole> roles;
	public final Map<String, WrappedMember> members;

	public WrappedGuild(DiscordJS d, GuildCollections w) {
		discordJS = d;
		gc = w;
		id = new WrappedId(gc.guildId);

		channels = new DynamicMap<>(id -> new WrappedChannel(this, new WrappedId(Snowflake.of(id))));
		roles = Collections.unmodifiableMap(gc.getRoleMap().values().stream().map(r -> new WrappedRole(this, r)).collect(Collectors.toMap(k -> k.id.asString(), Function.identity())));
		members = new DynamicMap<>(id -> new WrappedMember(this, new WrappedId(Snowflake.of(id))));
	}

	@Override
	public WrappedId getWrappedId() {
		return id;
	}

	@Override
	public String toString() {
		return gc.toString();
	}

	@HideFromJS
	public GuildService getGuildService() {
		return gc.getClient().getRestClient().getGuildService();
	}

	public WrappedUser getUser(String id) {
		WrappedMember m = members.get(id);
		Snowflake snowflake = Snowflake.of(id);

		if (gc.getMember(snowflake) == null) {
			return new WrappedUser(this, new WrappedId(snowflake));
		}

		return m;
	}
}
