package dev.gnomebot.app.discord.command;

/**
 * @author LatvianModder
 */
public class SettingsCommands extends ApplicationCommands {
	//todo move everything to json files
	@RootCommand
	public static final CommandBuilder COMMAND = root("settings")
			.description("Configure Gnome bot")
			.add(sub("set")
					.description("Change a setting")
					.add(string("key").required().suggest(SettingsCommands::suggestKey))
					.add(string("value").required().suggest(SettingsCommands::suggestValue))
					.run(SettingsCommands::set)
			)
			.add(sub("get")
					.description("Print a setting value")
					.add(string("key").required().suggest(SettingsCommands::suggestKey))
					.run(SettingsCommands::get)
			);

	private static void set(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderOwner();
		event.respond("WIP!");
	}

	private static void get(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		event.respond("WIP!");
	}

	private static void suggestKey(ChatCommandSuggestionEvent event) {
	}

	private static void suggestValue(ChatCommandSuggestionEvent event) {
	}
}