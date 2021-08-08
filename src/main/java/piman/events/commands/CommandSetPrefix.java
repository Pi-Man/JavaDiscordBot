package piman.events.commands;

import java.io.IOException;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.config.BotConfig;
import piman.exceptions.SyntaxErrorException;

public class CommandSetPrefix extends CommandBase {

	public CommandSetPrefix(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "[prefix]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);

		if (args.length == 1) {
			this.setPrefix(message, args[0]);
		} 
		else {
			throw new SyntaxErrorException("");
		}

	}

	private void setPrefix(Message message, String newPrefix) {
		
		BotConfig config = TestBot.getConfig(message.getGuild().getId());
		
		config.setSetting("prefix", newPrefix);
		
		try {
			config.write();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		message.getChannel().sendMessage("Set Prefix to: " + newPrefix).queue();
	}

}
