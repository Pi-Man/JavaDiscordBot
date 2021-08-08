package piman.music;

import java.util.ArrayList;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.TextChannel;

public class AudioLoadResultReturner extends AudioLoadResultHandler {

	public AudioLoadResultReturner(TextChannel channel, String trackName) {
		super(channel, trackName);
	}
	
	private List<AudioTrack> tracks = new ArrayList<>();
	
	public List<AudioTrack> getTracks() {
		return tracks;
	}
	
	@Override
	protected void useTrack(AudioTrack track) {
		tracks.add(track);
		System.out.println("Loaded Track");
	}
	
	@Override
	protected void finished() {
		synchronized (this) {
			notify();
		}
	}

}
