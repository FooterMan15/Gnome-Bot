package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.AppTaskCancelledException;
import dev.latvian.apps.webutils.TimeUtils;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class ImportMessagesCommand {
	@LegacyDiscordCommand(name = "import_messages", help = "Imports messages from all channels into DB", arguments = "[channel|channel:message]", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		var messageChannels = new ArrayList<Pair<ChannelInfo, Snowflake>>();

		Optional<Pair<ChannelInfo, Snowflake>> c;

		while ((c = reader.readChannelAndMessage()).isPresent()) {
			messageChannels.add(c.get());
		}

		if (messageChannels.isEmpty()) {
			for (ChannelInfo channel : context.gc.getChannelList()) {
				if (channel.getChannelData() != null) {
					Snowflake lm = channel.getLastMessageId();

					if (lm != null) {
						messageChannels.add(Pair.of(channel, lm));
					}
				}
			}
		}

		context.handler.app.queueBlockingTask(task -> {
			String channelNames = messageChannels.stream().map(mc -> mc.a().getMention()).collect(Collectors.joining(" "));
			App.info("Importing messages from " + messageChannels.stream().map(mc -> "#" + mc.a().getName() + ":" + mc.b().asString()).collect(Collectors.joining(" ")));
			context.reply("Importing messages from " + channelNames);

			int mId = 0;
			long now = System.currentTimeMillis();
			MemberCache memberCache = context.gc.createMemberCache();

			var channelsLeft = new ArrayList<>(messageChannels);

			for (var pair : messageChannels) {
				var ch = pair.a();
				var lastId = pair.b();

				long nowChannel = System.currentTimeMillis();

				try {
					for (Message message : ch.getMessagesBefore(lastId)
							.delayElements(Duration.ofMillis(10))
							.onErrorContinue((throwable, o) -> App.info("Error! " + o + ": " + throwable))
							.filter(m -> m.getType() == Message.Type.DEFAULT && m.getAuthor().isPresent())
							.toIterable()) {
						mId++;
						lastId = message.getId();
						User user = message.getAuthor().get();
						MessageHandler.messageCreated(context.handler, ch, message, user, memberCache.getAndUpdate(user).orElse(null), true);

						if (task.cancelled) {
							throw new AppTaskCancelledException();
						}
					}

					context.reply("Imported " + ch.getMention() + " in " + TimeUtils.prettyTimeString((System.currentTimeMillis() - nowChannel) / 1000L));
					channelsLeft.remove(pair);
				} catch (AppTaskCancelledException ex) {
					context.reply("Imported " + ch.getMention() + " in " + TimeUtils.prettyTimeString((System.currentTimeMillis() - nowChannel) / 1000L));
					channelsLeft.remove(pair);
					channelsLeft.add(0, Pair.of(ch, lastId));
					break;
				} catch (Exception ex) {
					App.error("Error! " + ch.getName() + ": " + ex);
					context.reply("Can't read " + ch.getMention() + ", skipping");
				}
			}

			int count = mId;
			long totalTime = (System.currentTimeMillis() - now) / 1000L;
			context.reply("Imported " + count + " messages from " + channelNames + " from " + memberCache.getCacheSize() + " members in " + TimeUtils.prettyTimeString(totalTime) + " @ " + (int) (count / (double) totalTime) + " m/s");

			if (task.cancelled) {
				context.reply("Importing was cancelled! To continue run\n```" + context.gc.legacyPrefix + "import_messages " + channelsLeft.stream().map(ch -> ch.a().id.asString() + ":" + ch.b().asString()).collect(Collectors.joining(" ")) + "```");
			}
		});
	};
}
