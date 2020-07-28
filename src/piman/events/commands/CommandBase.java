package piman.events.commands;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public abstract class CommandBase {

	private String identifier;

	private Visibility visibility;

	public CommandBase(String identifier, Visibility visibility) {
		this.identifier = identifier;
		this.visibility = visibility;
	}

	public boolean acceptes(TextChannel channel, String input) {

		boolean flag = getVisibility(channel);
		
		int index = input.indexOf(" ");
		
		if (index == -1) {
			index = input.length();
		}

		return flag && input.toLowerCase().substring(0, index).equals(TestBot.getPrefix(channel.getGuild().getId()) + identifier.toLowerCase());
	}

	public boolean getVisibility(TextChannel channel) {

		boolean flag = false;

		if (this.visibility == Visibility.ALL) {
			flag = true;
		} 
		else if (this.visibility == Visibility.NONE && TestBot.getConfig(channel.getGuild().getId()).getSetting("channel", String.class).equals("none")) {
			flag = true;
		} 
		else if ((this.visibility == Visibility.CHANNEL || this.visibility == Visibility.NONE) && TestBot.getConfig(channel.getGuild().getId()).getSetting("channel", String.class).equals(channel.getId())) {
			flag = true;
		}

		return flag;
	}

	protected String[] getArgs(Message message, String input) {
		
		List<String> args = new ArrayList<>();
		
		char[] chars = input.toCharArray();
		
		boolean quote = false;
				
		int lastindex = 0;
		
		for (int i = 0; i < input.length(); i++) {
			
			char c = chars[i];
						
			if (c == '"') {
				quote = !quote;
				continue;
			}
			
			if (c == ' ' && !quote) {
				args.add(input.substring(lastindex, i).replace('"', ' ').trim());
				lastindex = i + 1;
			}
			
		}
		
		args.add(input.substring(lastindex, input.length()).replace('"', ' ').trim());
		
		args.remove(0);
		
		//System.out.println(args);
				
		return args.toArray(new String[0]);
	}
	
	protected List<TextChannel> getTextChannels(Guild guild, String input) {
		
		List<TextChannel> list = new ArrayList<>();
		
		for (int fromIndex = 0; input.substring(fromIndex).contains("#");) {
			
			fromIndex = input.indexOf("#", fromIndex) + 1;
			
			int endIndex = input.indexOf(" ", fromIndex);
			if (endIndex == -1) {
				endIndex = input.length();
			}
			
			String name = input.substring(fromIndex, endIndex);
			
			list.add(guild.getTextChannelsByName(name, false).get(0));
		}
		
		return list;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public abstract String getUsage();

	public abstract void run(Message message, String input) throws SyntaxErrorException;

	public enum Visibility {
		ALL, NONE, CHANNEL;
	}
}
