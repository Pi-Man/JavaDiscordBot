package piman.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.music.AudioLoadResultReturner;

public class PlayList {
	
	private static final String PATH = "resources/assets/playlist/";

	private final String name;
	
	private final String guildID;
	
	private List<AudioTrack> tracks = Collections.unmodifiableList(new ArrayList<>());
	
	private String password;
	
	public PlayList(String guildID, String name) {
		this.name = name;
		this.guildID = guildID + "/";
	}
	
	public void read(TextChannel channel) {
		
		File file = new File(PATH + guildID + name);
		
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			this.password = reader.readLine();
			int i = 0;
			TreeMap<Integer, AudioTrack> map = new TreeMap<>();
			List<Runnable> runnables = new ArrayList<>();
			while(reader.ready()) {
				int k = i;
				String trackURL = reader.readLine();
				Runnable runnable = new Runnable() {
					public void run() {
						System.out.println(trackURL);
						load(k, map, channel, trackURL);
					};
				};
				i++;
				runnables.add(runnable);
			}
			
			ExecutorService threadPool = Executors.newFixedThreadPool(100);
			
			for (Runnable runnable: runnables) {
				threadPool.execute(runnable);
			}
						
			threadPool.shutdown();
			
			while(!threadPool.isTerminated());
			
			List<AudioTrack> list = new ArrayList<>(map.values());
			
			System.out.println(map);
			
			save(list);
			
			reader.close();
			
		} 
		catch (FileNotFoundException e) {
			try {
				write();
			} catch (IOException e1) {
				channel.sendMessage(e1.getLocalizedMessage()).queue();
			}
		}
		catch (IOException e) {
			channel.sendMessage(e.getLocalizedMessage()).queue();
		}
		
	}
	
	public String readPassword(TextChannel channel) {
		
		File file = new File(PATH + guildID + name);
		
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			String password = reader.readLine();
			
			reader.close();
			
			return password;
			
		} 
		catch (FileNotFoundException e) {
			try {
				write();
			} catch (IOException e1) {
				channel.sendMessage(e1.getLocalizedMessage()).queue();
			}
		}
		catch (IOException e) {
			channel.sendMessage(e.getLocalizedMessage()).queue();
		}
		
		return null;
		
	}
	
	public void savePassword(TextChannel channel, String password) {
		
		File directory = new File(PATH + guildID);
		
		if (!directory.exists()) {
			directory.mkdir();
		}
		
		File file = new File(PATH + guildID + name);
		
		try {
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			
			char[] buffer = new char[1000];
						
			int i = reader.read(buffer);
			
			reader.close();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(file));
			
			writer.write(password + "\n");
			
			if (i != -1) {
				buffer[i] = '\0';
								
				for (int j = 0; j < 1000 && buffer[j] != '\0'; j++) {
					writer.write(buffer[j]);
				}
			}
			writer.close();
		
		}
		catch (IOException e) {
			channel.sendMessage(e.getMessage()).queue();
		}
		
	}
	
	public void add(TextChannel channel, String identifier) {
		
		AudioLoadResultReturner returner = new AudioLoadResultReturner(channel, identifier);
		
		synchronized (returner) {
		
			Thread thread = new Thread("loader") {
				public void run() {
					TestBot.playerManager.loadItem(identifier, returner);
				};
			};
			
			thread.start();
			
			try {
				returner.wait(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
				
		System.out.println("Load Finnished");
		
		List<AudioTrack> list = get();
		
		list.addAll(returner.getTracks());
		
		save(list);
	}
	
	private void load(int i, TreeMap<Integer, AudioTrack> map, TextChannel channel, String identifier) {
		
		AudioLoadResultReturner returner = new AudioLoadResultReturner(channel, identifier);
		
		synchronized (returner) {
		
			Thread thread = new Thread("loader") {
				public void run() {
					TestBot.playerManager.loadItem(identifier, returner);
				};
			};
			
			thread.start();
			
			try {
				returner.wait(30000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
				
		System.out.println("Load Finnished");
		
		map.put(i, returner.getTracks().get(0));
		
	}
	
	public void write() throws IOException {
		
		File directory = new File(PATH + guildID);
		
		if (!directory.exists()) {
			directory.mkdir();
		}
		
		File file = new File(PATH + guildID + name);
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));
		
		if (password != null) {
			writer.write(password + "\n");
		}
		
		for (AudioTrack track : tracks) {
			
			writer.write(track.getInfo().uri + "\n");
			
		}
		
		writer.close();
		
	}
	
	public void save(List<AudioTrack> tracks) {
		
		this.tracks = Collections.unmodifiableList(tracks);
		
	}
	
	public List<AudioTrack> get() {
		
		List<AudioTrack> list = new ArrayList<>();
		
		list.addAll(tracks);
		
		return list;
		
	}
	
	public String getPassword() {
		return password;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}
	
}
