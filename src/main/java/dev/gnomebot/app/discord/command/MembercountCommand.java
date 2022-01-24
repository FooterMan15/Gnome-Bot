package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;

import java.util.Optional;

/**
 * @author LatvianModder
 */
public class MembercountCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("membercount")
			.description("Displays member count")
			.add(role("role"))
			.run(MembercountCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledge();
		Optional<CachedRole> role = event.get("role").asRole();

		if (role.isPresent()) {
			CachedRole wr = role.get();
			long count = event.context.gc.getGuild()
					.getMembers()
					.filter(member -> member.getRoleIds().contains(wr.id))
					.count()
					.block();

			event.respond(Utils.format(count) + " members with role " + wr);
		} else {
			int max = event.context.gc.getGuild().getMaxMembers().orElse(0);

			long count = event.context.gc.getGuild()
					.getMembers()
					.count()
					.block();

			event.respond(Utils.format(count) + " / " + (max == 0 ? "?" : Utils.format(max)) + " members");
		}
	}
}