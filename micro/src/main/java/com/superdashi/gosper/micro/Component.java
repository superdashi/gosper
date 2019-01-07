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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.micro.Display.Situation;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public abstract class Component {

	// enums

	enum Composition {
		DRAW, // this is a rectangular component that requires its bounds to be cleared before rendering
		MASK, // this is a non-rectangular component that applies a mask to the background
		FILL, // this is a rectangular component that will fill its rectangular bounds
	}

	enum Changes {
		SHAPE,
		CONTENT,
		NONE,
	}

	// fields

	Situation situation;

	// constructors

	// public methods

	public boolean focusable() {
		return focusing().isPresent();
	}

	public boolean focus() {
		return situation.focus();
	}

	public void constrainLayout() {
		if (situation.isPlaced()) throw new IllegalStateException("component already placed");
		situation.constrain(minimumSize());
	}

	// methods for implementation

	//TODO needs corresponding de-situate method?
	void situate(Situation situation) {
		this.situation = situation;
	}

	IntDimensions minimumSize() { return IntDimensions.NOTHING; }

	abstract void place(Place place, int z);

	abstract Composition composition();

	IntRect bounds() {
		return situation.place().innerBounds;
	}

	abstract Changes changes();

	abstract void render();

	Optional<Focusing> focusing() {
		return Optional.empty();
	}

	Optional<Eventing> eventing() {
		return Optional.empty();
	}

	Optional<Scrolling> scrolling() {
		return Optional.empty();
	}

	Optional<Pointing> pointing() {
		return Optional.empty();
	}

	void receiveResponse(ActivityResponse response) { }

	// interfaces

	interface Focusing {

		// empty if no specific focus area is defined
		default List<IntRect> focusableAreas() {
			return Collections.emptyList();
		}

		// will only be called if component is focused
		// if no specific area is focused, should return the bounds
		IntRect focusArea();

		default void receiveFocus(int areaIndex) { }

		default void cedeFocus() { }

		default boolean focusableByDefault() { return true; }
	}

	interface Eventing {

		// by default, all components that receive events, receive all events
		default EventMask eventMask() {
			return EventMask.always();
		}

		boolean handleEvent(Event event);

	}

	interface Scrolling {

		ScrollbarModel scrollbarModel();

	}

	interface Pointing {

		default void pressed() { }

		default void released() { }

		// like focus, to make whole component clickable without concern, return an empty list
		default List<IntRect> clickableAreas() {
			return Collections.emptyList();
		}

		// return true, not if action was taken, but if click was 'on a target'
		default boolean clicked(int areaIndex, IntCoords coords) { return false; }

	}
}
