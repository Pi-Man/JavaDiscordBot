package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
import piman.exceptions.SyntaxErrorException;

public class CommandLeave extends CommandBase {

	public CommandLeave(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);
		
		if (args.length != 0) {
			throw new SyntaxErrorException("Too many arguments");
		}

		VoiceChannel vc = message.getGuild().getAudioManager().getConnectedChannel();
		message.getGuild().getAudioManager().closeAudioConnection();
		message.getChannel().sendMessage(String.format("Left Voice Chat: %s", vc.getName())).queue();

	}

}
