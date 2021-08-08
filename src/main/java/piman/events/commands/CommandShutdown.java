package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandShutdown extends CommandPasswordAdmin {

	public CommandShutdown(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {
		TestBot.shutdown(message.getJDA());
	}

}
