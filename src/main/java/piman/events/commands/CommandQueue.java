package piman.events.commands;

import java.util.Locale;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import piman.TestBot;
import piman.exceptions.SyntaxErrorException;

public class CommandQueue extends CommandBase {

	public CommandQueue(String identifier, Visibility visibility) {
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
				String text = TestBot.getAudioEventHandler(message.getGuild().getId()).printQueue();
				EmbedBuilder builder = new EmbedBuilder();
				builder.setTitle("Queue");
				builder.addField("next songs", text.isEmpty() ? "Queue is Empty" : text, false);
				builder.setColor(TestBot.EMBED_COLOR_B);
				message.getChannel().sendMessageEmbeds(builder.build()).queue();
			}
			else if (args[0].toLowerCase(Locale.ROOT).equals("clear")) {
				TestBot.getAudioEventHandler(message.getGuild().getId()).clearQueue();
				message.getChannel().sendMessage("Cleared Queue").queue();
			}
			else if (args[0].toLowerCase(Locale.ROOT).equals("remove")) {
				throw new SyntaxErrorException("Missing index");
			}
			else {
				throw new SyntaxErrorException("Incorrect argument: " + args[0]);
			}
		}
		else if (args.length == 2) {
			if (args[0].toLowerCase(Locale.ROOT).equals("remove")) {
				try {
					int i = Integer.valueOf(args[1]);
					AudioTrack track = TestBot.getAudioEventHandler(message.getGuild().getId()).removeFromQueue(i);
					message.getChannel().sendMessage(String.format("Removed %s from Queue", track.getInfo().title)).queue();
				}
				catch (NumberFormatException e) {
					throw new SyntaxErrorException("Invalid Integer: " + args[1]);
				}
				catch (IndexOutOfBoundsException e) {
					throw new SyntaxErrorException("Invalid Index: " + args[1]);
				}
			}
			else {
				throw new SyntaxErrorException("Incorrect argument: " + args[0]);
			}
		}
		else {
			throw new SyntaxErrorException("Too many arguments");
		}
		
	}

}
