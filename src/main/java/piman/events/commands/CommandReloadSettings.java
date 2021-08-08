package piman.events.commands;

import java.io.IOException;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandReloadSettings extends CommandBase {

	public CommandReloadSettings(String identifier, Visibility visibility) {
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
			try {
				TestBot.getConfig(message.getGuild().getId()).read();
			} catch (IOException e) {
				message.getChannel().sendMessage("Unnable to Read Settings").queue();
				e.printStackTrace();
			}
		}
		
	}
	
	@Override
	public boolean acceptes(TextChannel channel, String input) {
		
		boolean flag = getVisibility(channel);

		return flag && input.toLowerCase().equals(getIdentifier().toLowerCase());
		
	}

}
