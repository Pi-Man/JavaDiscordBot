package piman.events;

import java.util.ArrayList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import piman.TestBot;
import piman.events.commands.CommandBase;
import piman.events.commands.CommandGetPrefix;
import piman.events.commands.CommandHelp;
import piman.events.commands.CommandHistory;
import piman.events.commands.CommandJoin;
import piman.events.commands.CommandLeave;
import piman.events.commands.CommandPasswordBase;
import piman.events.commands.CommandPlay;
import piman.events.commands.CommandPlaylist;
import piman.events.commands.CommandQueue;
import piman.events.commands.CommandReloadSettings;
import piman.events.commands.CommandRemovePlayBar;
import piman.events.commands.CommandSetAdminPassword;
import piman.events.commands.CommandSetChannel;
import piman.events.commands.CommandSetPrefix;
import piman.events.commands.CommandShutdown;
import piman.events.commands.CommandBase.Visibility;
import piman.events.commands.CommandCreatePlayBar;
import piman.exceptions.InvalidAccessException;
import piman.exceptions.NoPasswordException;
import piman.exceptions.SyntaxErrorException;

public class TextChannelMessageEventHandler implements EventListener {

	private List<CommandBase> registeredCommands = new ArrayList<CommandBase>();

	public TextChannelMessageEventHandler() {
		registeredCommands.add(new CommandHelp("Help", Visibility.CHANNEL));
		registeredCommands.add(new CommandSetChannel("SetChannel", Visibility.NONE));
		registeredCommands.add(new CommandSetPrefix("SetPrefix", Visibility.CHANNEL));
		registeredCommands.add(new CommandGetPrefix("Prefix", Visibility.NONE));
		registeredCommands.add(new CommandJoin("Join", Visibility.CHANNEL));
		registeredCommands.add(new CommandLeave("Leave", Visibility.CHANNEL));
		registeredCommands.add(new CommandPlay("Play", Visibility.CHANNEL));
		registeredCommands.add(new CommandCreatePlayBar("CreatePlayBar", Visibility.CHANNEL));
		registeredCommands.add(new CommandRemovePlayBar("RemovePlayBar", Visibility.CHANNEL));
		registeredCommands.add(new CommandHistory("History", Visibility.CHANNEL));
		registeredCommands.add(new CommandReloadSettings("ReloadSettings", Visibility.NONE));
		registeredCommands.add(new CommandQueue("Queue", Visibility.CHANNEL));
		registeredCommands.add(new CommandSetAdminPassword("SetAdminPassword", Visibility.ALL));
		registeredCommands.add(new CommandPlaylist("Playlist", Visibility.CHANNEL));
		registeredCommands.add(new CommandShutdown("Shutdown", Visibility.CHANNEL));
	}
	
	public String getHelpString() {
		String help = "";
		for (CommandBase command : registeredCommands) {
			help = help.concat(command.getIdentifier() + " " + command.getUsage() + "\n");
		}
		return help;
	}

	@Override
	public void onEvent(GenericEvent event) {
		if (event instanceof GuildMessageReceivedEvent && !((GuildMessageReceivedEvent) event).getAuthor().equals(TestBot.jda.getSelfUser())) {

			Message message = ((GuildMessageReceivedEvent) event).getMessage();
			
			String[] messages = message.getContentDisplay().split("\n");
			
			runCommands(message, messages);

		}
	}
	
	private void runCommands(Message message, String[] commands) {
		new Thread() {
			@Override
			public void run() {
				List<Runnable> tasks = new ArrayList<>();
				for (String input : commands) {
					for (CommandBase command : registeredCommands) {
						if (command.acceptes(message.getTextChannel(), input)) {
							Runnable task = new Runnable() {
								@Override
								public void run() {
									try {
										if (command instanceof CommandPasswordBase) {
											((CommandPasswordBase) command).requestPassword(message, input);
										}
										command.run(message, input);
									}
									catch (SyntaxErrorException e) {
										message.getChannel().sendMessage("Syntax Error: `" + e.getMessage() + "`\n    Correct Usage: `" + command.getUsage() + "`").queue();
									}
									catch (InvalidAccessException e) {
										message.getChannel().sendMessage(e.getMessage()).queue();
									}
									catch (NoPasswordException e) {
										message.getChannel().sendMessage("No Password Set").queue();
									} 
									catch (Exception e) {
										e.printStackTrace();
									}
								}
							};
							tasks.add(task);
						}
					}
				}
				if (!tasks.isEmpty()) {
					TestBot.commandExcecutor.addTasks(tasks);
				}
			}
		}.start();
	}
}
