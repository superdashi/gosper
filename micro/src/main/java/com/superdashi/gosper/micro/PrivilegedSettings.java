package com.superdashi.gosper.micro;

import java.util.concurrent.Future;

import com.superdashi.gosper.bundle.Privileges;
import com.superdashi.gosper.item.Item;

final class PrivilegedSettings implements Settings {

	private final Settings settings;
	private final Privileges privileges;

	public PrivilegedSettings(Settings settings, Privileges privileges) {
		this.settings = settings;
		this.privileges = privileges;
	}

	@Override
	public Future<Item> saveSettings(Item item) {
		privileges.check(Privileges.WRITE_SETTINGS);
		return settings.saveSettings(item);
	}

	@Override
	public Item loadSettings() {
		privileges.check(Privileges.READ_SETTINGS);
		return settings.loadSettings();
	}

	@Override
	public boolean loadingAvailable() {
		return privileges.readSettings && settings.loadingAvailable();
	}

	@Override
	public boolean savingAvailable() {
		return privileges.writeSettings && settings.savingAvailable();
	}


}
