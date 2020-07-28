package piman.events.commands;

import java.util.HashMap;
import java.util.Map;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.NoPasswordException;

public abstract class CommandPasswordAdmin extends CommandPasswordBase {
	
	private static Map<String, String> passwords = new HashMap<>();

	public CommandPasswordAdmin(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	protected String getPassword(Message message) throws NoPasswordException {

		if (TestBot.getConfig(message.getGuild().getId()).hasSetting("adminPassword", String.class)) {
			
			return TestBot.getConfig(message.getGuild().getId()).getSetting("adminPassword", String.class);
			
		}
		
		throw new NoPasswordException("");
		
	}
		
	@Override
	protected String getPassword(Message message, String input) throws NoPasswordException {
		return getPassword(message);
	}

}
