package piman.events.commands;

import net.dv8tion.jda.api.entities.Message;
import piman.events.PasswordListener;
import piman.exceptions.IncorrectPasswordException;
import piman.exceptions.NoPasswordException;

public abstract class CommandPasswordBase extends CommandBase {
	
	public CommandPasswordBase(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}
	
	public void requestPassword(Message message, String input) throws IncorrectPasswordException, NoPasswordException {
		
		String password = this.getPassword(message, input);
		
		if (password == null) {
			return;
		}
		
		Message message1 = message.getChannel().sendMessage("Enter Password").complete();
		
		PasswordListener passwordlistener = new PasswordListener(message.getTextChannel(), message.getAuthor());
		
		message.getJDA().addEventListener(passwordlistener);
		
		synchronized (passwordlistener) {
			
			try {
				passwordlistener.wait(30000L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
		}
		
		if (!passwordlistener.getPassword().equals(this.getPassword(message, input))) {
			throw new IncorrectPasswordException(passwordlistener.getPassword());
		}
		
		message1.delete().queue();
		
	}
	
	protected abstract String getPassword(Message message, String input) throws NoPasswordException;

}
