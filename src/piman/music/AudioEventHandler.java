package piman.music;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import piman.TestBot;

public class AudioEventHandler implements AudioEventListener {
	
	private List<AudioTrack> playedqueue = new ArrayList<AudioTrack>();

	private List<AudioTrack> queue = new ArrayList<AudioTrack>();
		
	private AudioPlayer audioPlayer;
	
	private String guildID;
	
	public AudioEventHandler(AudioPlayer player, String guildID) {
		this.audioPlayer = player;
		this.guildID = guildID;
	}
	
	@Override
	public void onEvent(AudioEvent event) {

		if (event instanceof TrackEndEvent) {
			TrackEndEvent endEvent = (TrackEndEvent) event;
			
			if (endEvent.endReason.mayStartNext || endEvent.endReason == AudioTrackEndReason.STOPPED) {
				
				if (endEvent.endReason != AudioTrackEndReason.STOPPED) {
					playNext(endEvent.track);
				}
				
				Boolean shouldRepeat = TestBot.getConfig(guildID).getSetting("repeat", Boolean.class);
				
				if (shouldRepeat != null && shouldRepeat && audioPlayer.getPlayingTrack() == null) {
					
					Boolean shouldShuffle = TestBot.getConfig(guildID).getSetting("shuffle", Boolean.class);

					this.repeat();
					
					if (shouldShuffle != null && shouldShuffle) {
						
						this.shuffle();
						
					}
					this.playNext(null);
				}
			}
		}
	}

	public void queue(AudioTrack track) {
		queue.add(track);
		
		if (audioPlayer.getPlayingTrack() == null) {
			playNext(null);
		}
	}

	public void playNext(AudioTrack oldtrack) {
		
		if (oldtrack == null) {
			if (TestBot.getConfig(guildID).getSetting("repeat", Boolean.class)) {
				this.repeat();
				if (TestBot.getConfig(guildID).getSetting("shuffle", Boolean.class)) {
					this.shuffle();
				}
			}
		}
		else {
			playedqueue.add(0, oldtrack.makeClone());
		}
		
		if (queue.isEmpty()) {
			audioPlayer.startTrack(null, false);
			return;
		}
		
		AudioTrack track = queue.remove(0);
		
		audioPlayer.startTrack(track, false);
	}
	
	public void playPrev(AudioTrack oldTrack) {
		if (playedqueue.isEmpty()) {
			AudioTrack track = audioPlayer.getPlayingTrack();
			if (track != null) {
				track.setPosition(0);
			}
			return;
		}
		
		AudioTrack track = playedqueue.remove(0);
		
		audioPlayer.startTrack(track, false);
		
		if (oldTrack != null) {
			queue.add(0, oldTrack.makeClone());
		}
	}
	
	public void clearHistory() {
		playedqueue.clear();
	}
	
	public void clearQueue() {
		queue.clear();
	}
	
	private void repeat() {
		
		int size = playedqueue.size();
		
		for (int i = 0; i < size; i++) {
			queue.add(0, playedqueue.remove(0));
		}
		
	}
	
	private void shuffle() {
		Random rand = new Random();
		
		List<AudioTrack> list = new ArrayList<>();
		
		while (!queue.isEmpty()) {
			list.add(queue.remove(rand.nextInt(queue.size())));
		}
		
		queue = list;
		
		for (AudioTrack track : queue) {
			System.out.println(track.getInfo().title);
		}
	}
	
}
