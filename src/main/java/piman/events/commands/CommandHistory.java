package piman.events.commands;

import java.util.Locale;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandHistory extends CommandBase {

	public CommandHistory(String identifier, Visibility visibility) {
		super(identifier, visibility);
	}

	@Override
	public String getUsage() {
		return "[view|clear|remove] (remove)[index]";
	}

	@Override
	public void run(Message message, String input) throws SyntaxErrorException {
		String[] args = getArgs(message, input);
		
		if (args.length == 0) {
			throw new SyntaxErrorException("Missing arguments");
		}
		else if (args.length == 1) {
			if (args[0].toLowerCase(Locale.ROOT).equals("view")) {
				String text = TestBot.getAudioEventHandler(message.getGuild().getId()).printHistory();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("History");
				builder.addField("previous songs", text.isEmpty() ? "History is Empty" : text, false);
				builder.setColor(TestBot.EMBED_COLOR_B);
				message.getChannel().sendMessageEmbeds(builder.build()).queue();
			}
			else if (args[0].toLowerCase(Locale.ROOT).equals("clear")) {
				TestBot.getAudioEventHandler(message.getGuild().getId()).clearHistory();
				message.getChannel().sendMessage("Cleared History").queue();
			}
			else if (args[0].toLowerCase(Locale.ROOT).equals("remove")) {
				throw new SyntaxErrorException("Missing index");
			}
			else {
				throw new SyntaxErrorException("Incorrect arg: " + args[0]);
			}
		}
		else if (args.length == 2) {
			if (args[0].toLowerCase(Locale.ROOT).equals("remove")) {
				try {
					int i = Integer.valueOf(args[1]);
					AudioTrack track = TestBot.getAudioEventHandler(message.getGuild().getId()).removeFromHistory(i);
					message.getChannel().sendMessage(String.format("Removed %s from History", track.getInfo().title)).queue();
				}
				catch (NumberFormatException e) {
					throw new SyntaxErrorException("Invalid Integer: " + args[1]);
				}
				catch (IndexOutOfBoundsException e) {
					throw new SyntaxErrorException("Invalid Index: " + args[1]);
				}
			}
			else {
				throw new SyntaxErrorException("Incorrect arg: " + args[0]);
			}
		}
		else {
			throw new SyntaxErrorException("Too many arguments");
		}
		
	}

}
