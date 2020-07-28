package piman.events.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.managers.AudioManager;
import piman.TestBot;
import piman.events.PasswordListener;
import piman.exceptions.NoPasswordException;
import piman.exceptions.SyntaxErrorException;
import piman.music.AudioLoadResultHandler;
import piman.music.AudioPlayerSendHandler;
import piman.playlist.PlayList;

public class CommandPlaylist extends CommandPasswordBase {

	public CommandPlaylist(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	protected String getPassword(Message message, String input) throws NoPasswordException {
		
		String args[] = this.getArgs(message, input);
		
		if (args[0].equals("view")) {
			return null;
		}
		
		PlayList playlist;
		
		if (!args[0].equals("open")) {
			playlist = TestBot.MUSIC_PLAYER.getPlaylist(message.getGuild().getId());
		}
		else {
			playlist = new PlayList(message.getGuild().getId(), args[1]);
			
			String password = playlist.readPassword(message.getTextChannel());
			if (password == null || password.contains("http")) {
				
				Message message1 = message.getChannel().sendMessage("Enter Password For Playlist").complete();
				
				PasswordListener listener = new PasswordListener(message.getTextChannel(), message.getAuthor());
				
				message.getJDA().addEventListener(listener);
				
				synchronized (listener) {
					
					try {
						listener.wait();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
										
				}
				
				message.getJDA().removeEventListener(listener);
				
				playlist.savePassword(message.getTextChannel(), listener.getPassword());
				
				message1.delete().queue();
				
			}
		}
		
		if (playlist == null) {
			return null;
		}
		
		String password = playlist.readPassword(message.getTextChannel());
		
		if (password == null) {
			throw new NoPasswordException("");
		}
	
		return password;
	}

	@Override
	public String getUsage() {
		return "[open|add|load|view|save] (open)[name] (add)[URL|\"Search Term\"]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String args[] = this.getArgs(message, input);
		
		if (args.length == 1) {
			
			switch (args[0]) {
			case "load":
				load(message);
				return;
			case "view":
				view(message);
				return;
			case "save":
				save(message);
				return;
			}
			
		}
		else if (args.length == 2) {
			switch (args[0]) {
			case "open":
				open(message, args[1]);
				return;
			case "add":
				add(message, args[1]);
				return;
			}
		}
		
		throw new SyntaxErrorException("");
		
	}
	
	private void open(Message message, String name) {
		PlayList playlist = new PlayList(message.getGuild().getId(), name);
		
		playlist.read(message.getTextChannel());
		
		TestBot.MUSIC_PLAYER.setPlaylist(playlist, message.getGuild().getId());
	}
	
	private void add(Message message, String track) {
		PlayList playlist = TestBot.MUSIC_PLAYER.getPlaylist(message.getGuild().getId());
		
		if (playlist == null) {
			message.getChannel().sendMessage("No Playlist Loaded").queue();
		}
		else {
			playlist.add(message.getTextChannel(), track);
			AudioPlayer audioPlayer = TestBot.getAudioPlayer(message.getGuild().getId());
			TestBot.playerManager.loadItem(track, new AudioLoadResultHandler(message.getTextChannel(), track));
			message.getGuild().getAudioManager().setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
		}
	}
	
	private void load(Message message) {
		String guildID = message.getGuild().getId();
		
		AudioManager audioManager = message.getGuild().getAudioManager();
		
		if (audioManager.isConnected() || audioManager.isAttemptingToConnect()) {
		
			List<AudioTrack> tracks = TestBot.MUSIC_PLAYER.getPlaylist(guildID).get();
			
			for (AudioTrack track : tracks) {
				TestBot.getAudioEventHandler(guildID).queue(track.makeClone());
			}
			
			audioManager.setSendingHandler(new AudioPlayerSendHandler(TestBot.getAudioPlayer(guildID)));
		}
		else {
			message.getChannel().sendMessage("Not Connected to a VoiceChannel").queue();
		}
	}
	
	private void view(Message message) {
		Integer i = 0;
		
		PlayList playlist = TestBot.MUSIC_PLAYER.getPlaylist(message.getGuild().getId());
		
		if (playlist == null) {
			message.getChannel().sendMessage("No Playlist Loaded");
			return;
		}
		
		String output = "";
		
		List<String> messages = new ArrayList<String>();
		
		int j = 0;
		
		for (AudioTrack track : playlist.get()) {
			
			String line = i.toString() + ") " + track.getInfo().title + "\n";
			
			if (output.length() + line.length() > 2000) {
				
				messages.add(output);
				
				output = line;
				
				j++;
			}
			else {
				output = output.concat(i.toString() + ") " + track.getInfo().title + "\n");
			}
			
			i++;
			
		}
		
		messages.add(output);
		
		for (String string : messages) {
			message.getChannel().sendMessage(string).queue();
		}
	}
	
	private void save(Message message) {
		try {
			TestBot.MUSIC_PLAYER.getPlaylist(message.getGuild().getId()).write();
		} 
		catch (IOException e) {
			message.getChannel().sendMessage("Unnable to Save").queue();
			e.printStackTrace();
		}
		catch (NullPointerException e) {
			message.getChannel().sendMessage("Nothing to Save").queue();
		}
	}

}
