package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandGetPrefix extends CommandBase {

	public CommandGetPrefix(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public boolean acceptes(TextChannel channel, String input) {

		boolean flag = getVisibility(channel);

		return flag && input.toLowerCase().equals(this.getIdentifier().toLowerCase());
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {
		message.getChannel().sendMessage("Prefix: " + TestBot.getPrefix(message.getGuild().getId())).queue();
	}

}
