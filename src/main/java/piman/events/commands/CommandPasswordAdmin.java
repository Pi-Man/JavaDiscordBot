package piman.events.commands;

import java.util.HashMap;
import java.util.Map;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.InvalidAccessException;
import piman.exceptions.NoPasswordException;

public abstract class CommandPasswordAdmin extends CommandPasswordBase {
	
	private static Map<String, String> passwords = new HashMap<>();

	public CommandPasswordAdmin(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	protected String getPassword(Message message) throws NoPasswordException, InvalidAccessException {

		if (message.getMember().hasPermission(Permission.ADMINISTRATOR)) {
			if (TestBot.getConfig(message.getGuild().getId()).hasSetting("adminPassword", String.class)) {
				
				return TestBot.getConfig(message.getGuild().getId()).getSetting("adminPassword", String.class);
				
			}
		}
		else {
			throw new InvalidAccessException("User is not Admin");
		}
		
		throw new NoPasswordException("");
		
	}
	
	@Override
	protected String getPassword(Message message, String input) throws NoPasswordException, InvalidAccessException {
		return getPassword(message);
	}

}
