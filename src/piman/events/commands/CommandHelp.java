package piman.events.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandHelp extends CommandBase {

	public CommandHelp(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {
		EmbedBuilder builder = new EmbedBuilder();
		builder.setTitle("Help");
		builder.addField("commands", TestBot.getTextChannelMessageHandler().getHelpString(), false);
		builder.setColor(TestBot.EMBED_COLOR);
		message.getTextChannel().sendMessage(builder.build()).complete();
	}

}
