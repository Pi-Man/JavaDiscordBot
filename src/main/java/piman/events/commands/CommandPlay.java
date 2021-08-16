package piman.events.commands;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import net.dv8tion.jda.api.audio.hooks.ConnectionStatus;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.managers.AudioManager;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;
import piman.music.AudioLoadResultHandler;
import piman.music.AudioPlayerSendHandler;

public class CommandPlay extends CommandBase {

	public CommandPlay(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "[URL|\"Search Term\"]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);
		
		if (args.length == 0) {
			throw new SyntaxErrorException("Missing Arguments");
		}
		else if (args.length == 1) {
			AudioManager audioManager = message.getGuild().getAudioManager();
			if (audioManager.getConnectionStatus() == ConnectionStatus.CONNECTED) {
				AudioPlayer audioPlayer = TestBot.getAudioPlayer(message.getGuild().getId());
				TestBot.playerManager.loadItem(args[0], new AudioLoadResultHandler(message.getTextChannel(), args[0]));
				audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
			}
			else {
				message.getChannel().sendMessage("Not Connected to Voice Channel").queue();
			}
		}
		else {
			throw new SyntaxErrorException("Too many arguments");
		}
		
	}

}
