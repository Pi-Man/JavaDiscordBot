package piman.events.commands;

import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandCreatePlayBar extends CommandBase {
	

	public CommandCreatePlayBar(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "[#Text-Channel]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {
		
		String[] args = this.getArgs(message, input);
		
		List<TextChannel> channels = this.getTextChannels(message.getGuild(), input);
				
		if (channels.size() > 1) {
			throw new SyntaxErrorException("");
		}
				
		if (args.length == 1) {
			TextChannel channelIn = channels.get(0);
			TestBot.MUSIC_PLAYER.setChannel(channelIn, channelIn.getGuild().getId());
			TestBot.MUSIC_PLAYER.setup(channelIn.getGuild().getId());
		}
		else {
			throw new SyntaxErrorException("");
		}
	}
	
}
