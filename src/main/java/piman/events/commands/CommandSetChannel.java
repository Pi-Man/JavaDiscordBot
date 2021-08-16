package piman.events.commands;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
		
		String[] args = this.getArgs(message, input);
		
		if (args.length == 0) {
			throw new SyntaxErrorException("Missing argument");
		}
		else if (args.length == 1) {
			if (args[0].toLowerCase(Locale.ROOT).equals("none")) {
				this.setChannel(message, "");
			}
			else {
				List<TextChannel> channels = this.getTextChannels(message.getGuild(), input);
				if (channels.size() == 0) {
					throw new SyntaxErrorException("Could not find channel");
				}
				else if (channels.size() == 1) {
					this.setChannel(message, channels.get(0).getId());
				}
				else {
					throw new SyntaxErrorException("Found more than one Channel");
				}
			}
		}
		else {
			throw new SyntaxErrorException("Too many arguments");
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
		if (channel.isEmpty()) {
			message.getChannel().sendMessage("Reset Channel").queue();
		} 
		else {
			message.getJDA().getTextChannelById(channel).sendMessage("Set Channel to: " + message.getJDA().getTextChannelById(channel).getAsMention()).queue();
		}
	}

}
