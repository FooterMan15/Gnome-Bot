package dev.gnomebot.app.util;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.EmbedColors;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.UserData;

public class UnmuteTask implements ScheduledTaskCallback {
	public final GuildCollections gc;
	public final Snowflake user;
	public final long seconds;
	public boolean cancelled;

	public UnmuteTask(GuildCollections g, Snowflake u, long s) {
		gc = g;
		user = u;
		seconds = s;
		cancelled = false;

		App.info("Muting " + gc + "/" + user);
		App.LOGGER.memberMuted();
	}

	@Override
	public void run(ScheduledTask task) throws Exception {
		if (!cancelled) {
			unmute();
		}
	}

	public void unmute() {
		App.info("Unmuting " + gc + "/" + user);

		if (gc.mutedRole.isSet()) {
			gc.mutedRole.remove(user, "Unmuted");

			UserData data = gc.db.app.discordHandler.getUserData(user);

			if (data != null) {
				gc.adminLogChannelEmbed(spec -> {
					spec.color(EmbedColors.GREEN);
					spec.author(data.username() + "#" + data.discriminator() + " has been unmuted!", null, Utils.getAvatarUrl(data));
				});
			}
		}

		gc.members.findFirst(user).update(Updates.unset("muted"));
		gc.unmuteMap.remove(user);
		cancelled = true;
		// App.error("Unmuted " + user.asString());
	}
}