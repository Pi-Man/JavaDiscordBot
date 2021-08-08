package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandRemovePlayBar extends CommandBase {

	public CommandRemovePlayBar(String identifier, Visibility visibility) {
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
		
			TestBot.MUSIC_PLAYER.remove(message.getGuild().getId());
		
		}
		else {
			throw new SyntaxErrorException("");
		}
		
	}

}
