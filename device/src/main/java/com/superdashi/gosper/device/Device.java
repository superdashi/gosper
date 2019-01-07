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
package com.superdashi.gosper.device;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.superdashi.gosper.device.network.Wifi;
import com.tomgibara.fundament.Producer;

public interface Device {

	// may be called before capture
	DeviceSpec getSpec();

	// lifecycle
	void capture();
	void relinquish();

	Optional<Screen> getScreen();
	Optional<Wifi> getWifi();

	// events, only delivered after capture
	void setEventConsumer(Consumer<Event> eventConsumer);
	void setSpecConsumer(Consumer<DeviceSpec> specConsumer);

	// if non empty, the object from which events must be polled.
	Optional<Producer<List<Event>>> events();
}
