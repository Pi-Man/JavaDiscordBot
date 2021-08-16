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

	private List<CommandBase> visibleCommands = new ArrayList<>();
	private List<CommandBase> hiddenCommands = new ArrayList<>();
	
	private List<CommandBase> allCommands = new ArrayList<>();

	public TextChannelMessageEventHandler() {
		visibleCommands.add(new CommandHelp("Help", Visibility.CHANNEL));
		hiddenCommands.add(new CommandSetChannel("SetChannel", Visibility.NONE));
		hiddenCommands.add(new CommandSetPrefix("SetPrefix", Visibility.CHANNEL));
		visibleCommands.add(new CommandGetPrefix("Prefix", Visibility.NONE));
		visibleCommands.add(new CommandJoin("Join", Visibility.CHANNEL));
		visibleCommands.add(new CommandLeave("Leave", Visibility.CHANNEL));
		visibleCommands.add(new CommandPlay("Play", Visibility.CHANNEL));
		hiddenCommands.add(new CommandCreatePlayBar("CreatePlayBar", Visibility.CHANNEL));
		hiddenCommands.add(new CommandRemovePlayBar("RemovePlayBar", Visibility.CHANNEL));
		visibleCommands.add(new CommandHistory("History", Visibility.CHANNEL));
		hiddenCommands.add(new CommandReloadSettings("ReloadSettings", Visibility.NONE));
		visibleCommands.add(new CommandQueue("Queue", Visibility.CHANNEL));
		hiddenCommands.add(new CommandSetAdminPassword("SetAdminPassword", Visibility.ALL));
		visibleCommands.add(new CommandPlaylist("Playlist", Visibility.CHANNEL));
		hiddenCommands.add(new CommandShutdown("Shutdown", Visibility.CHANNEL));
		
		allCommands.addAll(visibleCommands);
		allCommands.addAll(hiddenCommands);
	}
	
	public String getHelpString() {
		String help = "";
		for (CommandBase command : visibleCommands) {
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
					for (CommandBase command : allCommands) {
						if (command.acceptes(message.getTextChannel(), input)) {
							tasks.add(() -> {
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
							});
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
