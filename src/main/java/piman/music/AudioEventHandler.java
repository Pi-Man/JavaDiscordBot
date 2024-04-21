package piman.music;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEvent;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventListener;
import com.sedmelluq.discord.lavaplayer.player.event.TrackEndEvent;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import piman.TestBot;
import piman.youtube.Captions;
import piman.youtube.YoutubeAudioTrackInfo;

public class AudioEventHandler implements AudioEventListener {
	
	private List<AudioTrack> playedqueue = new ArrayList<AudioTrack>();

	private List<AudioTrack> queue = new ArrayList<AudioTrack>();
		
	private AudioPlayer audioPlayer;
	
	private String guildID;
	
	private final HttpInterfaceManager httpInterfaceManager;
	
	public AudioEventHandler(AudioPlayer player, String guildID) {
		this.audioPlayer = player;
		this.guildID = guildID;
	    httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
	    httpInterfaceManager.setHttpContextFilter(new YoutubeHttpContextFilter());
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
			if (!playedqueue.isEmpty() && TestBot.getConfig(guildID).getSetting("repeat", Boolean.class)) {
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
		
		play(track);
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
		
		play(track);
		
		if (oldTrack != null) {
			queue.add(0, oldTrack.makeClone());
		}
	}
	
	private void play(AudioTrack track) {
		Captions captions = Captions.CreateCaptions();
		String thumbnail = "";
		try(HttpInterface httpInterface = httpInterfaceManager.getInterface()) {
			JsonBrowser videoInfo = YoutubeAudioTrackInfo.getVideoData(httpInterface, track.getIdentifier());
			captions = Captions.CreateCaptions(httpInterface, videoInfo);
			thumbnail = videoInfo.get("videoDetails").get("thumbnail").get("thumbnails").index(3).get("url").text();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		catch (Throwable t) {
			t.printStackTrace();
		}
		track.setUserData(new YoutubeAudioTrackInfo(captions, thumbnail));
		audioPlayer.startTrack(track, false);
	}
	
	public void clearHistory() {
		playedqueue.clear();
	}
	
	public void clearQueue() {
		queue.clear();
	}
	
	public String printQueue() {
		if (queue.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		StringBuilder last = new StringBuilder();
		
		int size = queue.size();
		last.append(size);
		last.append(") ");
		last.append(queue.get(size - 1).getInfo().title);
		
		for (int i = 0; i < size; i++) {
			
			StringBuilder temp = new StringBuilder();
			temp.append(i + 1);
			temp.append(") ");
			temp.append(queue.get(i).getInfo().title);
			temp.append('\n');
			
			if (sb.length() + temp.length() + last.length()> 1000) {
				sb.append("...\n");
				sb.append(last);
				break;
			}
			
			sb.append(temp);
		}
		return sb.toString();
	}
	
	public String printHistory() {
		if (playedqueue.size() == 0) return "";
		StringBuilder sb = new StringBuilder();
		StringBuilder last = new StringBuilder();
		
		int size = playedqueue.size();
		last.append(size);
		last.append(") ");
		last.append(playedqueue.get(size - 1).getInfo().title);
		
		for (int i = 0; i < size; i++) {
			
			StringBuilder temp = new StringBuilder();
			temp.append(i + 1);
			temp.append(") ");
			temp.append(playedqueue.get(i).getInfo().title);
			temp.append('\n');
			
			if (sb.length() + temp.length() + last.length()> 1000) {
				sb.append("...\n");
				sb.append(last);
				break;
			}
			
			sb.append(temp);
		}
		return sb.toString();
	}
	
	public AudioTrack removeFromQueue(int index) {
		return queue.remove(index - 1);
	}
	
	public AudioTrack removeFromHistory(int index) {
		return playedqueue.remove(index - 1);
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
	
	public void reset() {
		AudioTrack track = audioPlayer.getPlayingTrack();
		if (track != null) {
			queue.add(0, track.makeClone());
		}
		repeat();
		if (TestBot.getConfig(guildID).getSetting("shuffle", Boolean.class)) {
			shuffle();
		}
		playNext(null);
	}
	
	public void shuffleQueue() {
		shuffle();
	}
	
	public void removeCurrentSong() {
		if (queue.isEmpty()) {
			audioPlayer.startTrack(null, false);
			return;
		}
		
		AudioTrack track = queue.remove(0);
		
		play(track);
	}
	
}
