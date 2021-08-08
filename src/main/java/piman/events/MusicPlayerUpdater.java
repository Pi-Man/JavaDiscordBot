package piman.events;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import piman.TestBot;
import piman.config.BotConfig;
import piman.music.AudioEventHandler;
import piman.playlist.PlayList;
import piman.youtube.Captions;
import piman.youtube.YoutubeAudioTrackInfo;

public class MusicPlayerUpdater implements EventListener{
	
	private static final int BARSIZE = 50;
	
	private static final String 
		PREV = "\u23ee",
		BACK = "\u23ea",
		PLAY = "\u23ef",
		FORW = "\u23e9",
		NEXT = "\u23ed",
		CLER = "\u23F9",
		SHFL = "\ud83d\udd00",
		REPT = "\ud83d\udd01",
		RPT1 = "\uD83D\uDD02",
		RSET = "\uD83D\uDD04";
		
	private Map<String, PlayList> playlists = new HashMap<>();
	
	private String progressBar = "";
	
	public void finishSetup() {
		for (Guild guild : TestBot.jda.getGuilds()) {
			this.setup(guild.getId());
		}
	}
		
	@Override
	public void onEvent(GenericEvent event) {
			
		if (event instanceof GuildMessageReactionAddEvent) {
			
			GuildMessageReactionAddEvent reactionEvent = (GuildMessageReactionAddEvent) event;
							
			if (this.validate(reactionEvent.getUser(), reactionEvent.getChannel().getId(), reactionEvent.getGuild().getId())) {
				
				String reaction = reactionEvent.getReactionEmote().getName();
				
				BotConfig config = TestBot.getConfig(reactionEvent.getGuild().getId());
				
				TextChannel channel = getChannel(reactionEvent.getGuild().getId());
				
				if (reaction.equals(PREV)) {
					
					this.handlePrev(reactionEvent);
					
				}
				
				if (reaction.equals(BACK)) {
					
					AudioTrack track = TestBot.getAudioPlayer(reactionEvent.getGuild().getId()).getPlayingTrack();
					
					if (track != null && track.isSeekable()) {
						
						setPosition(track, track.getPosition() - track.getDuration() / BARSIZE, reactionEvent.getGuild().getId());
						
					}
					
				}
			
				if (reaction.equals(PLAY)) {
					
					this.handlePause(reactionEvent);
					
				}
				
				if (reaction.equals(FORW)) {
					
					AudioTrack track = TestBot.getAudioPlayer(reactionEvent.getGuild().getId()).getPlayingTrack();
					
					if (track != null && track.isSeekable()) {
						
						setPosition(track, track.getPosition() + track.getDuration() / BARSIZE, reactionEvent.getGuild().getId());
												
					}
					
				}
				
				if (reaction.equals(NEXT)) {
					
					this.handleNext(reactionEvent);
					
				}
				
				if (reaction.equals(CLER)) {
					TestBot.getAudioEventHandler(reactionEvent.getGuild().getId()).removeCurrentSong();
				}
				
				if (reaction.equals(SHFL)) {
					
					Boolean shuffle = config.getSetting("shuffle", Boolean.class);
					
					if (shuffle == null) {
						config.addSetting("shuffle", true);
						shuffle = false;
					}
					
					config.setSetting("shuffle", !shuffle);
					
					try {
						config.write();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				
				if (reaction.equals(REPT)) {
					
					Boolean shuffle = config.getSetting("repeat", Boolean.class);
					
					if (shuffle == null) {
						config.addSetting("repeat", true);
						shuffle = false;
					}
					
					config.setSetting("repeat", !shuffle);
					
					try {
						config.write();
					} catch (IOException e) {
						e.printStackTrace();
					}
					
				}
				
				if (reaction.equals(RPT1)) {
					TestBot.getAudioEventHandler(reactionEvent.getGuild().getId()).shuffleQueue();
				}
				
				if (reaction.equals(RSET)) {
					TestBot.getAudioEventHandler(reactionEvent.getGuild().getId()).reset();
				}
				
				channel.removeReactionById(reactionEvent.getMessageId(), reaction, reactionEvent.getUser()).queue();
				
			}
			
		}
		else if (event instanceof GuildMessageReceivedEvent) {
			GuildMessageReceivedEvent receiveEvent = (GuildMessageReceivedEvent) event;
			
			if (this.validate(receiveEvent.getAuthor(), receiveEvent.getChannel().getId(), receiveEvent.getGuild().getId())) {
				
				int length = receiveEvent.getMessage().getContentDisplay().length();
				
				AudioTrack track = TestBot.getAudioPlayer(receiveEvent.getGuild().getId()).getPlayingTrack();
				
				if (track != null) {
					
					setPosition(track, track.getDuration() * length / BARSIZE, receiveEvent.getGuild().getId());
					
				}
				
				receiveEvent.getChannel().deleteMessageById(receiveEvent.getMessageId()).queue();
			}
							
		}
			
	}
	
	private void setPosition(AudioTrack track, long position, String guildID) {
		
		if (position < 0) {
			position = 0;
		}
		
		AudioPlayer player = TestBot.getAudioPlayer(guildID);
		
		track.setPosition(position);
		
		System.out.println(position + " " + track.getPosition());
		
		boolean paused = player.isPaused();
		
		long start = System.currentTimeMillis();
		
		if (paused) {
			//player.setPaused(false);
			while(track.getPosition() == position && System.currentTimeMillis() < start + 100) {
				System.out.println(track.getPosition());
			}
			System.out.println(track.getPosition());
			if (track instanceof BaseAudioTrack) {
				while(Math.abs(track.getPosition() - position) > 40) {
					((BaseAudioTrack) track).provide();
					System.out.println(track.getPosition());
				}
			}
			//player.setPaused(true);
		}
		
	}
	
	private boolean validate(User user, String channelID, String guildID) {
		
		if (hasChannel(guildID)) {
			return !user.equals(user.getJDA().getSelfUser()) && channelID.equals(getChannel(guildID).getId());
		}
		else return false;
		
	}
	
	public void setup(String guildID) {
		
		if (hasChannel(guildID)) {
			
			if (!hasMessages(guildID)) {
			
				AudioTrack track = TestBot.getAudioPlayer(guildID).getPlayingTrack();
				
				MessageEmbed headerEmbed = getHeaderMessageEmbed(track, guildID);
				Message headerMessage = getChannel(guildID).sendMessage(headerEmbed).complete();
				
				MessageEmbed barEmbed = getBarMessageEmbed(track, guildID);
				Message barMessage = getChannel(guildID).sendMessage(barEmbed).complete();
				
				setHeaderMessage(headerMessage.getId(), guildID);
				setBarMessage(barMessage.getId(), guildID); 
				
				barMessage.addReaction(PREV).queue();
				barMessage.addReaction(BACK).queue();
				barMessage.addReaction(PLAY).queue();
				barMessage.addReaction(FORW).queue();
				barMessage.addReaction(NEXT).queue();
				barMessage.addReaction(CLER).queue();
				barMessage.addReaction(SHFL).queue();
				barMessage.addReaction(REPT).queue();
				barMessage.addReaction(RPT1).queue();
				barMessage.addReaction(RSET).queue();
			
			}
			
			Thread thread = new Thread("MusicPlayerInterface") {
				@Override
				public void run() {
					main(guildID);
				}
			};
			
			thread.start();
					
		}
		
	}
	
	private MessageEmbed getHeaderMessageEmbed(AudioTrack track, String guildID) {
		EmbedBuilder builder = new EmbedBuilder().setTitle(track == null ? "No Song Playing" : track.getInfo().title);
		if (track != null) {
			builder.addField("Link", track.getInfo().uri, false);
		}
		builder.addField("Playing", TestBot.getAudioPlayer(guildID).isPaused() ? ":pause_button:" : ":arrow_forward:", true)
				.addField("Shuffle", TestBot.getConfig(guildID).getSetting("shuffle", Boolean.class) ? ":white_check_mark:" : ":x:", true)
				.addField("Repeat", TestBot.getConfig(guildID).getSetting("repeat", Boolean.class) ? ":white_check_mark:" : ":x:", true)
				.setColor(TestBot.EMBED_COLOR_A);
		
		if (track != null && track.getUserData() instanceof YoutubeAudioTrackInfo) {
			
			YoutubeAudioTrackInfo ytTrackInfo = (YoutubeAudioTrackInfo) track.getUserData();
			
			if (ytTrackInfo.getThumbnail() != null && !ytTrackInfo.getThumbnail().isEmpty()) {
				builder.setThumbnail(ytTrackInfo.getThumbnail());
			}
			
		}
		
		return builder.build();
	}
	
	private MessageEmbed getBarMessageEmbed(AudioTrack track, String guildID) {
		
		String seeker = "";
		
		for (int i = 0; i < BARSIZE; i++) {
			seeker = seeker.concat("*");
		}
		
		EmbedBuilder builder = new EmbedBuilder()
				.addField("", String.format(
						"`%s`\n"
						+ "`%s`\n"
						+ "`%s`",
						getTime(track),
						seeker,
						fillProgressBar())
					, false)
				.setFooter("Blackberry Pi Music Player", TestBot.jda.getSelfUser().getAvatarUrl())
				.setColor(TestBot.EMBED_COLOR_B);
		
		if (track != null && track.getUserData() instanceof YoutubeAudioTrackInfo) {
			
			YoutubeAudioTrackInfo ytTrackInfo = (YoutubeAudioTrackInfo) track.getUserData();
			
			Captions captions = ytTrackInfo.getCaptions();
			List<String> capEvents = new ArrayList<>();
			int length = 0;
			for (Captions.Event event : captions.getEvents()) {
				if (event.isInRange(track.getPosition(), 2000)) {
					if (checkDuplicateCaptions(capEvents, event.getText())) {
						length += event.getText().length();
						if (length > MessageEmbed.VALUE_MAX_LENGTH) break;
						capEvents.add(event.getText());
					}
				}
			}
			
			if (!capEvents.isEmpty()) {
				String s = String.join("\n", capEvents);
				builder.addField("captions", s.substring(0, Math.min(s.length(), MessageEmbed.VALUE_MAX_LENGTH)), false);
			}
			
		}
		
		return builder.build();
		
	}
	
	private boolean checkDuplicateCaptions(List<String> capEvents, String caption) {
		for (String event : capEvents) {
			if (event.contains(caption) || caption.contains(event) || event.equals(caption)) {
				return false;
			}
		}
		return true;
	}
	
	private String getTime(AudioTrack track) {
		
		long time = 0L;
		long end = 0L;
		
		if (track != null) {
			time = track.getPosition();
			end = track.getDuration();
		}
		
		String current = milliToTime(time);
		
		String durration = milliToTime(end);
		
		int buffer = BARSIZE - current.length() - durration.length();
		
		for (int i = 0; i < buffer; i++) {
			current = current.concat(" ");
		}
		
		current = current.concat(durration);
		
		return current;
		
	}
	
	private String milliToTime(long milliseconds) {
		
		int hours = (int) (milliseconds / 3600000L);
		int minutes = (int) (milliseconds / 60000L) % 60;
		int seconds = (int) (milliseconds / 1000L) % 60;
		
		return hours == 0 ? String.format("%d:%02d", minutes, seconds) : String.format("%d%02d:%02d", hours, minutes, seconds);
		
	}
	
	public void remove(String guildID) {
		
		if (hasChannel(guildID)) {
			try {
				getChannel(guildID).deleteMessageById(getHeaderMessage(guildID)).queue();
				getChannel(guildID).deleteMessageById(getBarMessage(guildID)).queue();
			}
			catch (Exception e) {
				e.printStackTrace();
			}
			finally {
				removeChannel(guildID);
				removeMessages(guildID);
			}
		}
				
	}
	
	private void main(String guildID) {
		while(!getBarMessage(guildID).isEmpty()) {
							
			update(guildID);
			
			try {
				TimeUnit.SECONDS.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
	}
	private MessageEmbed oldHeaderEmbed;
	private MessageEmbed oldBarEmbed;
	private void update(String guildID) {
		
		AudioTrack track = TestBot.getAudioPlayer(guildID).getPlayingTrack();
		
		long trackpercent = track == null ? 0L : track.getPosition() * 100L / track.getDuration();
		
		setBar((int)trackpercent);
		
		try {
			TestBot.jda.awaitReady();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		MessageEmbed headerEmbed = getHeaderMessageEmbed(track, guildID);
		MessageEmbed barEmbed = getBarMessageEmbed(track, guildID);
		
		if (!barEmbed.equals(oldBarEmbed)) {
			getChannel(guildID).editMessageById(getBarMessage(guildID), barEmbed).queue();
			oldBarEmbed = barEmbed;
		}
		if (!headerEmbed.equals(oldHeaderEmbed)) {
			getChannel(guildID).editMessageById(getHeaderMessage(guildID), headerEmbed).queue();
			oldHeaderEmbed = headerEmbed;
		}
		
	}
	
	private void setBar(int percent) {
		
		this.progressBar = "";
		
		for (int i = 1; i < BARSIZE + 1; i++) {
			if (i * 2 < percent) {
				this.progressBar = this.progressBar.concat("=");
			}
			else if ((i-1) * 2 < percent && i * 2 >= percent) {
				this.progressBar = this.progressBar.concat(">");
			}
		}
	}
	
	private void handlePrev(GuildMessageReactionAddEvent event) {
		
		AudioPlayer player = TestBot.getAudioPlayer(event.getGuild().getId());
		
		AudioTrack track = player.getPlayingTrack();
		
		if (track != null && 100 * track.getPosition() / track.getDuration() > 10) {
			
			setPosition(track, 0, event.getGuild().getId());
						
		}
		else {
		
			AudioEventHandler handler = TestBot.getAudioEventHandler(event.getGuild().getId());
	
			handler.playPrev(track);
		
		}
		
	}
	
	public void handleSeek(String GuildID) {
		
	}
	
	private void handlePause(GuildMessageReactionAddEvent event) {
		AudioPlayer player = TestBot.getAudioPlayer(event.getGuild().getId());
		
		player.setPaused(!player.isPaused());
	}
	
	private void handleNext(GuildMessageReactionAddEvent event) {
		AudioPlayer player = TestBot.getAudioPlayer(event.getGuild().getId());
		
		AudioEventHandler handler = TestBot.getAudioEventHandler(event.getGuild().getId());
		
		handler.playNext(player.getPlayingTrack());
	}
	
	private String fillProgressBar() {
		
		String bar = "";
		
		bar = bar.concat(progressBar);
		
		for (int i = progressBar.length(); i < BARSIZE; i++) {
			bar = bar.concat("-");
		}
		
		return bar;
	}
	
	public TextChannel getChannel(String guildID) {
		return TestBot.jda.getTextChannelById(TestBot.getConfig(guildID).getSetting("playbarChannel", String.class));
	}
	
	public void setChannel(TextChannel channel, String guildID) {
		TestBot.getConfig(guildID).addSetting("playbarChannel", channel.getId());
	}
	
	public boolean hasChannel(String guildID) {
		return TestBot.getConfig(guildID).hasSetting("playbarChannel", String.class);
	}
	
	public boolean removeChannel(String guildID) {
		return TestBot.getConfig(guildID).removeSetting("playbarChannel", String.class);
	}
	
	public String getBarMessage(String guildID) {
		return TestBot.getConfig(guildID).getSetting("playbarMessage", String.class);
	}
	
	public void setBarMessage(String messageID, String guildID) {
		TestBot.getConfig(guildID).setSetting("playbarMessage", messageID);
	}
	
	public boolean hasMessages(String guildID) {
		boolean flag = TestBot.getConfig(guildID).hasSetting("playbarMessage", String.class)
				&& !TestBot.getConfig(guildID).getSetting("playbarMessage",  String.class).isEmpty()
				&& TestBot.getConfig(guildID).hasSetting("headerMessage", String.class)
				&& !TestBot.getConfig(guildID).getSetting("headerMessage", String.class).isEmpty();
		if (flag && hasChannel(guildID)) {
			flag &= getChannel(guildID).retrieveMessageById(getBarMessage(guildID)).complete() != null;
			flag &= getChannel(guildID).retrieveMessageById(getHeaderMessage(guildID)).complete() != null;
		}
		return flag;
	}
	
	public String getHeaderMessage(String guildID) {
		return TestBot.getConfig(guildID).getSetting("headerMessage", String.class);
	}
	
	public void setHeaderMessage(String messageID, String guildID) {
		TestBot.getConfig(guildID).setSetting("headerMessage", messageID);
	}
		
	public boolean removeMessages(String guildID) {
		return TestBot.getConfig(guildID).removeSetting("playbarMessage", String.class);
	}
	
	public PlayList getPlaylist(String guildID) {
		return playlists.get(guildID);
	}
	
	public void setPlaylist(PlayList playlist, String guildID) {
		this.playlists.put(guildID, playlist);
	}

}
