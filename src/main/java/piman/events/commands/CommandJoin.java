package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.exceptions.SyntaxErrorException;

public class CommandJoin extends CommandBase {

	public CommandJoin(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "<VoiceChannel ID>";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);

		if (args.length == 0) {
			try {
				message.getGuild().getAudioManager().openAudioConnection(message.getMember().getVoiceState().getChannel());
			} 
			catch (IllegalArgumentException e) {
				message.getChannel().sendMessage("You are not in a Voice Channel").queue();
			}
		} 
		else if (args.length == 1) {
			try {
				message.getGuild().getAudioManager().openAudioConnection(message.getJDA().getVoiceChannelById(args[0]));
			} 
			catch (IllegalArgumentException e) {
				message.getChannel().sendMessage("Invalid Voice Channel").queue();
			}
		} 
		else {
			throw new SyntaxErrorException("");
		}

	}

}
