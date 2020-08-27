package piman.music;

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;

public class AudioLoadResultHandler implements com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler {

	public static final String YTSEARCH = "ytsearch: ";
	
	private String trackName;
	
	private TextChannel channel;
	
	public AudioLoadResultHandler(TextChannel channel, String trackName) {
		this.channel = channel;
		this.trackName = trackName;
	}
	
	@Override
	public void trackLoaded(AudioTrack track) {
		useTrack(track);
		finished();
	}
	
	@Override
	public void playlistLoaded(AudioPlaylist playlist) {
		if (this.trackName.startsWith(YTSEARCH)) {
			useTrack(playlist.getTracks().get(0));
		}
		else {
			for (AudioTrack track : playlist.getTracks()) {
				useTrack(track);
			}
		}
		finished();
	}
	
	@Override
	public void noMatches() {
		if (this.trackName.startsWith(YTSEARCH)) {
			channel.sendMessage("Track Not Found: " + trackName).queue();
		}
		else {
			this.trackName = YTSEARCH + trackName;
			System.out.println("serching youtube for: " + trackName);
			TestBot.playerManager.loadItem(trackName, this);
		}
	}
	
	@Override
	public void loadFailed(FriendlyException exception) {
		
		String info;
		
		if (exception.severity == Severity.COMMON) {
			info = exception.getMessage();
		}
		else {
			info = "LavaPlayer would like to inform you that everything has EXPLODED: " + exception.getMessage();
			exception.printStackTrace();
		}
		channel.sendMessage(info).queue();
		
	}
	
	protected void useTrack(AudioTrack track) {
		TestBot.getAudioEventHandler(channel.getGuild().getId()).queue(track);
	}
	
	protected void finished() {};

}
