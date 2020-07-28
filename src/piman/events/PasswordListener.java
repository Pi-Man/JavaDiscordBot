package piman.events;

import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class PasswordListener implements EventListener {
	
	private String password = "";
	
	private final TextChannel channel;
	private final User user;
	
	public PasswordListener(TextChannel channel, User user) {
		
		this.channel = channel;
		this.user = user;
		
	}

	@Override
	public void onEvent(GenericEvent event) {

		if (event instanceof GuildMessageReceivedEvent) {
			
			GuildMessageReceivedEvent receiveEvent = (GuildMessageReceivedEvent) event;
			
			if (receiveEvent.getChannel().equals(channel) && receiveEvent.getAuthor().equals(this.user)) {
			
				password = receiveEvent.getMessage().getContentDisplay();
				
				receiveEvent.getMessage().delete().complete();
				
				synchronized (this) {
				
					notify();
					
				}
				
				receiveEvent.getJDA().removeEventListener(this);
								
			}
			
		}
		
	}
	
	public String getPassword() {
		return password;
	}
	
}
