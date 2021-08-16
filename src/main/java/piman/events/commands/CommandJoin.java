package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.VoiceChannel;
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
				VoiceChannel vc = message.getMember().getVoiceState().getChannel();
				message.getGuild().getAudioManager().openAudioConnection(vc);
				message.getChannel().sendMessage(String.format("Connected to Voice Channel: %s", vc.getName())).queue();
			} 
			catch (IllegalArgumentException e) {
				message.getChannel().sendMessage("You are not in a Voice Channel").queue();
			}
		} 
		else if (args.length == 1) {
			try {
				VoiceChannel vc = message.getJDA().getVoiceChannelById(args[0]);
				message.getGuild().getAudioManager().openAudioConnection(vc);
				message.getChannel().sendMessage(String.format("Connected to Voice Channel: %s", vc.getName())).queue();
			} 
			catch (IllegalArgumentException e) {
				message.getChannel().sendMessage("Invalid Voice Channel").queue();
			}
		} 
		else {
			throw new SyntaxErrorException("Too many arguments");
		}

	}

}
