package piman.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class BotConfig {
	private static final String PATH = "resources/assets/config/";
	
	private final String location;

	private Set<SettingsEntry> settings = new HashSet<SettingsEntry>();

	public BotConfig(String location) {
		this.location = location;
	}
	
	private File getFile() throws IOException {
		
		File path = new File(PATH);
		File file = new File(PATH + location);
		
		if (!path.isDirectory()) {
			path.mkdirs();
		}
		
		if (!file.isFile()) {
			file.createNewFile();
		}
		
		return file;
	}

	public void read() throws IOException {

		settings.clear();
		
		File file = getFile();

		BufferedReader bufferedReader = new BufferedReader(new FileReader(file));

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

		File file = getFile();
		
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));

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

			bufferedWriter.write(entry.getKey() + type + entry.getValue().toString() + "\n");

		}

		bufferedWriter.close();

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

				return this.getKey() == entry.getKey() && this.getValue() == entry.getValue();

			}

			return false;

		}

	}
}
