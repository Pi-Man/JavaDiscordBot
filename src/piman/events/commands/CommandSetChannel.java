package piman.events.commands;

import java.io.IOException;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.config.BotConfig;
import piman.exceptions.SyntaxErrorException;

public class CommandSetChannel extends CommandBase {

	public CommandSetChannel(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		List<TextChannel> channels = this.getTextChannels(message.getGuild(), input);
		
		String[] args = this.getArgs(message, input);

		if (channels.size() == 1) {
			this.setChannel(message, channels.get(0).getId());
		} 
		else if (channels.isEmpty() && args.length == 1) {
			if (args[0].toLowerCase().equals("none")) {
				this.setChannel(message, "none");
			} 
			else {
				throw new SyntaxErrorException("");
			}
		} 
		else {
			throw new SyntaxErrorException("");
		}
	}

	@Override
	public String getUsage() {
		return "[#channel-name|none]";
	}

	private void setChannel(Message message, String channel) {
		
		BotConfig config = TestBot.getConfig(message.getGuild().getId());
		
		config.setSetting("channel", channel);
		try {
			config.write();
		} 
		catch (IOException e) {
			e.printStackTrace();
		}
		if (channel.equals("none")) {
			message.getChannel().sendMessage("Reset Channel to \"none\"").queue();
		} 
		else {
			message.getJDA().getTextChannelById(channel).sendMessage("Set Channel to: " + message.getJDA().getTextChannelById(channel).getAsMention()).queue();
		}
	}

}
