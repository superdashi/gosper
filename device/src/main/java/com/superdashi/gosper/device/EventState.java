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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.tomgibara.bits.BitStore.BitMatches;
import com.tomgibara.bits.BitStore.Positions;
import com.superdashi.gosper.device.Event.Type;
import com.tomgibara.bits.BitVector;

//TODO consider constraining x,y of events?
public class EventState {

	private boolean initial = true;

	private boolean shift;
	private boolean ctrl;
	private boolean alt;

	private BitVector downKeys = new BitVector(Event.MAX_KEY + 1);

	public void resuming() {
		initial = true;
	}

	// returns an event list That makes state consistent with received event
	public List<Event> apply(Event event) {
		if (event == null) throw new IllegalArgumentException("null event");
		try {
			Event preEvent = preEventFor(event);
			if (preEvent != null) applyImpl(preEvent);
			applyImpl(event);
			if (initial && preEvent != null && preEvent.isDown()) {
				// don't report a down click/press if we're resuming
				return Collections.emptyList();
			}
			if (preEvent == null) return Collections.singletonList(event);
			//TODO want a cheaper 2 list
			return Arrays.asList(preEvent, event);
		} finally {
			initial = false;
		}
	}

	public Stream<Event> cleanup() {
		BitMatches ones = downKeys.ones();
		// quick case to check
		if (ones.isNone()) {
			clearStates();
			return Stream.empty();
		}
		// currently only key events
		Positions ps = ones.positions();
		Event[] events = new Event[ones.count()];
		long now = System.currentTimeMillis();
		for (int i = 0; i < events.length; i++) {
			events[i] = Event.newKeyEvent(ps.next(), false, false, shift, ctrl, alt, now);
			ps.replace(false);
		}
		clearStates();
		return Arrays.stream(events);
	}

	private Event preEventFor(Event event) {
		Type type = event.type();
		switch (type) {
		case KEY :
		case MOVE :
			boolean wasDown = downKeys.getBit(event.key);
			if (event.isDown()) {
				if (event.isRepeat()) {
					if (!wasDown) return event.withKeyState(true, false); // case: repeating with no key down
				} else {
					//TODO or best just to filter?
					if (wasDown) return event.withKeyState(false, false); // case: key down with key already pressed
				}
			} else {
				if (!wasDown) return event.withKeyState(true, false); // case: key up when none pressed
			}
			return null;
			default: return null;
		}
	}

	private void applyImpl(Event event) {
		if (event.type() == Event.Type.KEY) {
			downKeys.setBit(event.key, event.isDown());
			shift = event.isShift();
			ctrl = event.isCtrl();
			alt = event.isAlt();
		}
	}

	private void clearStates() {
		shift = false;
		ctrl = false;
		alt = false;
	}
}
