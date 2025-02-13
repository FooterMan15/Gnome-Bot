package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.server.AuthLevel;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class DeleteChannelMessagesCommand {
	@LegacyDiscordCommand(name = "delete_channel_messages", help = "Deletes messages", arguments = "<channel/message_after> | (<channel[/message_before]> <number of messages>)", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		Optional<Pair<ChannelInfo, Snowflake>> cm = reader.readChannelAndMessage();
		long m = Math.min(reader.readLong().orElse(0L), 1000L);

		cm.ifPresent(pair -> context.handler.app.queueBlockingTask(task -> {
			App.info("Deleting starting from " + pair.a().getMention() + "/" + pair.b().asString());
			context.message.addReaction(Emojis.VOTENONE).block();

			Iterable<Message> iterable;

			if (m > 0L) {
				iterable = pair.a().getMessagesBefore(pair.b()).take(m, true).toIterable();
			} else {
				if (!pair.b().equals(context.message.getId())) {
					pair.a().getMessage(pair.b()).delete().block();
				}

				iterable = pair.a().getMessagesAfter(pair.b()).take(1000L, true).toIterable();
			}

			for (Message message : iterable) {
				if (task.cancelled) {
					break;
				} else if (message.getId().equals(context.message.getId())) {
					continue;
				}

				App.info("Deleting " + message.getId().asString());
				message.delete().block();
			}

			context.upvote();
			context.message.removeSelfReaction(Emojis.VOTENONE).block();
		}));
	};
}
