/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
