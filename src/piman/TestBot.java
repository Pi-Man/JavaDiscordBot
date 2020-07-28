package piman;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.security.auth.login.LoginException;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDA.Status;
import net.dv8tion.jda.api.JDABuilder;
import piman.config.BotConfig;
import piman.events.MainEventManager;
import piman.events.MusicPlayerUpdater;
import piman.events.TextChannelMessageEventHandler;
import piman.music.AudioEventHandler;

public class TestBot {
	
	private static Map<String, BotConfig> configlist = new HashMap<>();
	
	public static AudioPlayerManager playerManager;
	private static Map<String, Pair<AudioPlayer, AudioEventHandler>> audioPlayers = new HashMap<String, Pair<AudioPlayer, AudioEventHandler>>();
	public static final MusicPlayerUpdater MUSIC_PLAYER = new MusicPlayerUpdater();
	
	public static TaskExecutor commandExcecutor;
	
	public static JDA jda;
	
	private static TextChannelMessageEventHandler textChannelMessageHandler = new TextChannelMessageEventHandler();

	public static void main(String[] args) throws IOException, LoginException {
		
		File file = new File("resources/assets/auth/auth.txt");
		
		if (!file.exists()) {
			File directory = file.getParentFile();
			directory.mkdirs();
			file.createNewFile();
		}
		
		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
		
		String token = bufferedReader.readLine();
		
		bufferedReader.close();
		
		jda = new JDABuilder(token).setEventManager(new MainEventManager()).build();
		
		jda.addEventListener(
			textChannelMessageHandler,
			MUSIC_PLAYER
		);
		
		playerManager = new DefaultAudioPlayerManager();
		AudioSourceManagers.registerRemoteSources(playerManager);
				
		commandExcecutor = new TaskExecutor();
		
		Thread thread = new Thread(commandExcecutor, "commandExecutor");
		
		thread.start();
		
		try {
			jda.awaitStatus(Status.CONNECTED);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		MUSIC_PLAYER.finishSetup();
	}
	
	public static TextChannelMessageEventHandler getTextChannelMessageHandler() {
		return textChannelMessageHandler;
	}

	public static String getPrefix(String guildID) {
		return getConfig(guildID).getSetting("prefix", String.class);
	}
	
	public static AudioPlayer getAudioPlayer(String guildID) {
		
		AudioPlayer player;
		
		if (TestBot.audioPlayers.containsKey(guildID)) {
			player = TestBot.audioPlayers.get(guildID).getKey();
		}
		else {
			player = TestBot.playerManager.createPlayer();
			player.setFrameBufferDuration(10);
			AudioEventHandler handler = new AudioEventHandler(player, guildID);
			player.addListener(handler);
			TestBot.audioPlayers.put(guildID, new Pair<>(player, handler));
		}
		
		return player;
		
	}
	
	public static AudioEventHandler getAudioEventHandler(String guildID) {
		
		AudioEventHandler listener;
		
		if (TestBot.audioPlayers.containsKey(guildID)) {
			listener = TestBot.audioPlayers.get(guildID).getValue();
		}
		else {
			TestBot.getAudioPlayer(guildID);
			listener = getAudioEventHandler(guildID);
		}
		
		return listener;
		
	}
	
	public static BotConfig getConfig(String guildID) {
		
		if (configlist.containsKey(guildID)) {
			return configlist.get(guildID);
		}
		else {
			BotConfig config = new BotConfig(guildID);
			try {
				try {
					config.read();
				} 
				catch (FileNotFoundException e) {
					config.addSetting("prefix", "$");
					config.addSetting("channel", "none");
					config.write();
				}
				configlist.put(guildID, config);
				return config;
			}
			catch (IOException e1) {
				e1.printStackTrace();
				return null;
			}
		}
		
	}
	
	public static void shutdown(JDA jda) {
		
		jda.shutdown();
		
		commandExcecutor.shutdown();
		
		System.exit(0);
				
	}
	
	public static class Pair<K, V> implements Entry<K, V> {
		
		public Pair(K key, V value) {
			this.key = key;
			this.value = value;
		}

		private K key;
		
		private V value;
		
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			
			V oldval = this.value;
			
			this.value = value;
			
			return oldval;
			
		}
		
	}

}
