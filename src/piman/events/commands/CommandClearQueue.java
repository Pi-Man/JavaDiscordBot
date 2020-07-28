package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandClearQueue extends CommandBase {

	public CommandClearQueue(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);
		
		if (args.length == 0) {
			
			TestBot.getAudioEventHandler(message.getGuild().getId()).clearQueue();
			
		}
		else {
			throw new SyntaxErrorException("");
		}
		
	}

}
