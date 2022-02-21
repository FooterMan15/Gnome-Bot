package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.cli.CLI;
import dev.gnomebot.app.cli.CLICommand;
import dev.gnomebot.app.cli.CLIEvent;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class CLIApplicationCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("cli")
			.description("Run a CLI command")
			.add(string("command")
					.suggest(CLIApplicationCommand::suggestCommands)
					.required()
			)
			.add(string("arguments")
					.suggest(CLIApplicationCommand::suggestArguments)
			)
			.run(CLIApplicationCommand::run);

	public static class CLIEventFromCommand extends CLIEvent {
		public final ApplicationCommandEventWrapper event;
		private final List<String> responseText = new ArrayList<>();

		public CLIEventFromCommand(ApplicationCommandEventWrapper e, CommandReader r) {
			super(e.context.gc, e.context.sender, r);
			event = e;
		}

		@Override
		public void respond(String text) {
			responseText.add(text);
		}
	}

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		CLICommand command = CLI.COMMANDS.get(event.get("command").asString());

		if (command == null) {
			throw error("Command not found!");
		}

		if (command.ephemeral) {
			event.acknowledgeEphemeral();
		} else {
			event.acknowledge();
		}

		if (command.trusted) {
			if (!event.context.isTrusted()) {
				throw new GnomeException("Only trusted users can use this command!");
			}
		} else if (command.admin) {
			event.context.checkSenderAdmin();
		}

		CommandReader reader = new CommandReader(event.context.gc, event.get("arguments").asString());
		CLIEventFromCommand event1 = new CLIEventFromCommand(event, reader);

		try {
			command.callback.run(event1);
		} catch (GnomeException ex) {
			event1.respond(ex.getMessage());
		} catch (Exception ex) {
			event1.respond(ex.toString());
		}

		if (event1.response != null) {
			if (!event1.responseText.isEmpty()) {
				event1.response.content(String.join("\n", event1.responseText));
			}

			event.respond(event1.response);
		}
	}

	private static void suggestCommands(ChatCommandSuggestionEvent event) {
		for (CLICommand command : CLI.COMMANDS.values()) {
			if (command.trusted && !event.context.isTrusted()) {
				continue;
			}

			if (command.admin && !event.context.isAdmin()) {
				continue;
			}

			event.suggest(command.name);
		}
	}

	private static void suggestArguments(ChatCommandSuggestionEvent event) {
	}
}
