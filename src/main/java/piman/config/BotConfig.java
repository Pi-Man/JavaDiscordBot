package piman.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import piman.TestBot;

public class BotConfig {	
	private final String guild;

	private Set<SettingsEntry> settings = new HashSet<SettingsEntry>();

	public BotConfig(String guild) {
		this.guild = guild;
	}

	public void read() throws IOException {

		settings.clear();
		
		List<TextChannel> channels = TestBot.jda.getGuildById(guild).getTextChannelsByName("blackberry-pi-data", true);
		
		if (channels.size() == 0) return;
		
		List<Message> messages = channels.get(0).getHistoryFromBeginning(10).complete().getRetrievedHistory();
		
		if (messages.size() == 0) return;
		
		StringBuilder sb = new StringBuilder();
		
		for (Message message : messages) {
			sb.append(message.getContentRaw());
		}

		BufferedReader bufferedReader = new BufferedReader(new StringReader(sb.toString()));

		String line;

		while ((line = bufferedReader.readLine()) != null) {

			String[] entry = line.split(":", 3);

			Object value = null;

			if (entry[1].equals("S")) {
				settings.add(new SettingsEntry<String>(entry[0], entry[2]));
			} 
			else if (entry[1].equals("I")) {
				settings.add(new SettingsEntry<Integer>(entry[0], Integer.valueOf(entry[2])));
			}
			else if (entry[1].equals("B")) {
				settings.add(new SettingsEntry<Boolean>(entry[0], Boolean.valueOf(entry[2])));
			}

		}

		bufferedReader.close();

	}

	public void write() throws IOException {
		
		List<TextChannel> channels = TestBot.jda.getGuildById(guild).getTextChannelsByName("blackberry-pi-data", true);
		
		if (channels.size() == 0) return;
		
		List<Message> messages = channels.get(0).getHistoryFromBeginning(10).complete().getRetrievedHistory();
		
		for (Message message : messages) {
			message.delete().complete();
		}
		
		List<StringBuilder> sb = new ArrayList<>();
		
		sb.add(new StringBuilder(2000));
		
		int index = 0;

		for (SettingsEntry entry : this.settings) {

			String type = null;

			if (entry.getValue() instanceof String) {
				type = ":S:";
			} 
			else if (entry.getValue() instanceof Integer) {
				type = ":I:";
			}
			else if (entry.getValue() instanceof Boolean) {
				type = ":B:";
			}
			
			String line = entry.getKey() + type + entry.getValue().toString() + "\n";
			
			if (sb.get(index).length() + line.length() > 2000) {
				sb.add(new StringBuilder(2000));
				index++;
			}
			
			sb.get(index).append(line);

		}
		
		for (StringBuilder builder : sb) {
			channels.get(0).sendMessage(builder.toString()).complete();
		}

	}

	public <T> void addSetting(String key, T value) {
		settings.add(new SettingsEntry<T>(key, value));
		try {
			this.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings({ "unchecked" })
	public <T> T getSetting(String key, Class<T> clazz) {

		for (SettingsEntry setting : settings) {
			if (setting.getKey().equals(key) && clazz.isInstance(setting.getValue())) {
				return (T) setting.getValue();
			}
		}
		
		addSetting(key, getDefaultValue(clazz));
		
		return getSetting(key, clazz);

	}
	
	@SuppressWarnings("unchecked")
	private <T> T getDefaultValue(Class<T> clazz) {
		
		if (clazz == String.class) {
			return (T) "";
		}
		else if (clazz == Integer.class) {
			return (T) new Integer(0);
		}
		else if (clazz == Boolean.class) {
			return (T) new Boolean(false);
		}
		
		else return null;
		
	}

	@SuppressWarnings("unchecked")
	public <T> T setSetting(String key, T value) {
		T oldVal = (T) this.getSetting(key, value.getClass());

		if (oldVal != null) {
			for (SettingsEntry setting : settings) {
				if (setting.getKey().equals(key) && value.getClass().isInstance(setting.getValue())) {
					setting.setValue(value);
					try {
						this.write();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		else {
			this.addSetting(key, value);
		}

		return oldVal;
	}
	
	public <T> boolean hasSetting(String key, Class<T> clazz) {
		
		for (SettingsEntry setting : settings) {
			if (setting.getKey().equals(key) && clazz.isInstance(setting.getValue())) {
				return true;
			}
		}
		
		return false;
		
	}
	
	public <T> boolean removeSetting(String key, Class<T> clazz) {
		boolean flag = settings.removeIf(setting -> {return setting.key.equals(key) && clazz.isInstance(setting.value);});
		try {
			this.write();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return flag;
	}

	private class SettingsEntry<T> {

		private String key;

		private T value;

		public SettingsEntry(String key, T value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			this.value = value;
		}

		@Override
		public boolean equals(Object obj) {

			if (obj instanceof SettingsEntry) {

				SettingsEntry entry = (SettingsEntry) obj;

				return this.key == entry.key && this.value.getClass() == entry.value.getClass();

			}

			return false;

		}
		
		@Override
		public int hashCode() {
			return this.key.hashCode() << 16 | this.value.getClass().hashCode();
		}

	}
}
