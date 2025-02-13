package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SimpleStringReader;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.util.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class Macro extends WrappedDocument<Macro> {
	public static final Pattern REMOVE_MD_LINKS = Pattern.compile("\\[.+?]\\((.+?)\\)");

	private ChatCommandSuggestion chatCommandSuggestion;

	public Macro(WrappedCollection<Macro> c, MapWrapper d) {
		super(c, d);
	}

	public long getAuthor() {
		return document.getLong("author");
	}

	@Override
	public Date getDate() {
		return document.getDate("created");
	}

	public String getContent() {
		return document.getString("content");
	}

	public List<String> getExtra() {
		return document.getList("extra");
	}

	public int getUses() {
		return document.getInt("uses");
	}

	public long getSlashCommand() {
		return document.getLong("slash_command");
	}

	public MessageBuilder createMessage(Snowflake sender, boolean removeLinks) {
		return createMessage(getContent(), getExtra(), sender, removeLinks);
	}

	public static MessageBuilder createMessage(String contentString, List<String> extraList, Snowflake sender, boolean removeLinks) {
		MessageBuilder builder = MessageBuilder.create();
		EmbedBuilder embedBuilder = EmbedBuilder.create();

		List<String> lines = new ArrayList<>();

		boolean hasEmbed = false;
		List<ActionComponent> components = new ArrayList<>();

		for (String extra : extraList) {
			SimpleStringReader reader = new SimpleStringReader(extra);
			String command = reader.readString().orElse("");

			switch (command) {
				case "newrow" -> {
					if (!components.isEmpty()) {
						builder.addComponent(ActionRow.of(components));
					}

					components = new ArrayList<>();
				}
				case "macro", "edit_macro" -> {
					String name = reader.readString().orElse("");
					String macro = reader.readString().orElse("");
					String color = reader.readString().orElse("gray");
					ReactionEmoji emoji = reader.readEmoji().orElse(null);

					if (!name.isEmpty() && !macro.isEmpty()) {
						String id = command + "/" + macro + "/" + sender.asString();

						switch (color) {
							case "gray" -> components.add(Button.secondary(id, emoji, name));
							case "blurple" -> components.add(Button.primary(id, emoji, name));
							case "green" -> components.add(Button.success(id, emoji, name));
							case "red" -> components.add(Button.danger(id, emoji, name));
						}
					}
				}
				case "url" -> {
					String name = reader.readString().orElse("");
					String url = reader.readString().orElse("");
					ReactionEmoji emoji = reader.readEmoji().orElse(null);

					if (!name.isEmpty() && !url.isEmpty()) {
						components.add(Button.link(url, emoji, name));
					}
				}
				case "embed" -> {
					hasEmbed = true;
					embedBuilder.title(reader.readString().orElse(""));

					String s = reader.readString().orElse("");

					if (s.startsWith("#") && s.length() == 7) {
						embedBuilder.color(Color.of(Integer.decode(s)));
					}
				}
				case "embed_field" -> {
					hasEmbed = true;
					embedBuilder.field(reader.readString().orElse(""), reader.readString().orElse(""));
				}
				case "inline_embed_field" -> {
					hasEmbed = true;
					embedBuilder.inlineField(reader.readString().orElse(""), reader.readString().orElse(""));
				}
			}
		}

		if (!components.isEmpty()) {
			builder.addComponent(ActionRow.of(components));
		} else {
			builder.noComponents();
		}

		Collections.addAll(lines, contentString
				.replaceAll("role:(\\d+)", "<@&$1>")
				.replaceAll("user:(\\d+)", "<@$1>")
				.replace("mention:here", "@here")
				.replace("mention:everyone", "@everyone")
				.split("\n"));

		if (lines.isEmpty()) {
			lines.add("Missingno");
		}

		if (hasEmbed) {
			builder.content("");
			embedBuilder.description(lines);
			builder.addEmbed(embedBuilder);
		} else {
			builder.content(lines);
			builder.noEmbeds();
		}

		return builder;
	}

	public void rename(String rename) {
		long l = setSlashCommand(false);

		document.map.put("name", rename);
		update("name", rename);

		if (l != 0L) {
			setSlashCommand(true);
		}
	}

	public void updateContent(String content, List<String> extra) {
		if (!content.equals(getContent())) {
			document.map.put("content", content);
			update("content", content);
		}

		if (!extra.isEmpty() && !extra.equals(getExtra())) {
			if (extra.contains("clear")) {
				document.map.remove("extra");
				update(Updates.unset("extra"));
			} else {
				document.map.put("extra", extra);
				update("extra", extra);
			}
		}
	}

	public long setSlashCommand(boolean b) {
		var gc = collection.gc;

		if (b) {
			String author = gc.db.app.discordHandler.getUserName(Snowflake.of(getAuthor())).orElse("Deleted User");

			ApplicationCommandData data = gc.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), ApplicationCommandRequest.builder()
					.name(getName().toLowerCase())
					.description("Macro created by " + author)
					.build()
			).block();

			long id = data == null ? 0L : Snowflake.of(data.id()).asLong();

			if (data != null) {
				document.map.put("slash_command", id);
				update("slash_command", id);
			}

			return id;
		} else {
			long id = getSlashCommand();

			if (id != 0L) {
				gc.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(gc.db.app.discordHandler.applicationId, gc.guildId.asLong(), id).block();
				document.map.remove("slash_command");
				update(Updates.unset("slash_command"));
			}

			return id;
		}
	}

	public ChatCommandSuggestion getChatCommandSuggestion() {
		if (chatCommandSuggestion == null) {
			chatCommandSuggestion = new ChatCommandSuggestion(getName(), getName(), getName().toLowerCase(), 0);
		}

		return chatCommandSuggestion;
	}
}