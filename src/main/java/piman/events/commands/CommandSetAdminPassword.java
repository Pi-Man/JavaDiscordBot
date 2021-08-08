package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.events.PasswordListener;
import piman.exceptions.IncorrectPasswordException;
import piman.exceptions.InvalidAccessException;
import piman.exceptions.NoPasswordException;
import piman.exceptions.SyntaxErrorException;

public class CommandSetAdminPassword extends CommandPasswordAdmin {

	public CommandSetAdminPassword(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "[password]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {

		String[] args = this.getArgs(message, input);
		
		if (args.length == 1) {
			
			TestBot.getConfig(message.getGuild().getId()).setSetting("adminPassword", args[0]);
						
		}
		else {
			throw new SyntaxErrorException("");
		}
		
	}
	
	
	protected void requestPassword(Message message) throws InvalidAccessException, NoPasswordException {
		
		message.delete().queue();
		
		String password;
		
		try {
			password = this.getPassword(message);
		}
		catch (NoPasswordException e) {
			
			return;
			
		}
		
		Message enterMessage = message.getChannel().sendMessage("Enter Old Password").complete();
		
		PasswordListener passwordlistener = new PasswordListener(message.getTextChannel(), message.getAuthor());
		
		message.getJDA().addEventListener(passwordlistener);
				
		synchronized (passwordlistener) {
						
			try {
				passwordlistener.wait(30000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		enterMessage.delete().queue();
		
		message.getJDA().removeEventListener(passwordlistener);
			
		if (!passwordlistener.getPassword().equals(password)) {
			throw new IncorrectPasswordException();
		}
		
	}
	
	@Override
	public void requestPassword(Message message, String input) throws InvalidAccessException, NoPasswordException {
		requestPassword(message);
	}

}
