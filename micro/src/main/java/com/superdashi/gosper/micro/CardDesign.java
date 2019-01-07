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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Style;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.fundament.Mutability;
import com.tomgibara.storage.StoreType;

// combines a layout with the contents to put in the locations
public final class CardDesign implements Mutability<CardDesign> {

	// statics

	private static final Sets<Location> sets = Collect.setsWithStorage(StoreType.of(Location.class).storage().immutable());
	private static final Maps<Location, ItemContents> maps = Collect.setsOf(Location.class).mappedTo(ItemContents.class);

	private static Layout checkedLayout(Layout layout) {
		if (layout == null) throw new IllegalArgumentException("null layout");
		return layout;
	}

	private static Set<Location> layoutLocations(Layout layout) {
		return sets.newSet(Arrays.asList(layout.locations()));
	}

	// fields

	private final Layout layout;
	private final Set<Location> locations;
	private final EquivalenceMap<Location, ItemContents> contentsByLocation;
	private Position position; // of background picture

	// constructors

	//TODO switch to static constructors
	public CardDesign(Position position) {
		if (position == null) throw new IllegalArgumentException("null position");
		layout = Layout.single();
		Location location = layout.locations()[0];
		locations = sets.singletonSet(location);
		contentsByLocation = maps.newMap();
		contentsByLocation.put(location, ItemContents.nil());
		this.position = position;
	}

	// convenience constructor makes a single location with the give content
	public CardDesign(ItemContents contents, Style style) {
		if (contents == null) throw new IllegalArgumentException("null contents");
		Layout single = Layout.single();
		layout = style == null ? single : single.withStyle(style);
		Location location = layout.locations()[0];
		//TODO needs fixing in collect
		locations = sets.singletonSet(location);
		contentsByLocation = maps.newMap();
		contentsByLocation.put(location, contents);
	}

	public CardDesign(Layout layout) {
		this(checkedLayout(layout), layoutLocations(layout), maps.newMap(), null);
	}

	private CardDesign(Layout layout, Set<Location> locations, EquivalenceMap<Location, ItemContents> contents, Position position) {
		this.layout = layout;
		this.locations = locations;
		this.contentsByLocation = contents;
		this.position = position;
	}

	// accessors

	public Layout layout() {
		return layout;
	}

	public Collection<Location> locations() {
		return Collections.unmodifiableCollection(locations);
	}

	public Optional<Position> backgroundPosition() {
		return Optional.ofNullable(position);
	}

	// methods

	public Optional<ItemContents> contentsAtLocation(Location location) {
		if (location == null) throw new IllegalArgumentException("null location");
		if (!locations.contains(location)) throw new IllegalArgumentException("unsupported location");
		return Optional.ofNullable(contentsByLocation.get(location));
	}

	public CardDesign setContentsAtLocation(Location location, ItemContents contents) {
		if (location == null) throw new IllegalArgumentException("null location");
		if (!locations.contains(location)) throw new IllegalArgumentException("unsupported location");
		if (contents == null) throw new IllegalArgumentException("null contents");
		checkMutable();
		contentsByLocation.put(location, contents);
		return this;
	}

	public CardDesign clearContentsAtLocation(Location location) {
		if (location == null) throw new IllegalArgumentException("null location");
		if (!locations.contains(location)) throw new IllegalArgumentException("unsupported location");
		checkMutable();
		contentsByLocation.remove(location);
		return this;
	}

	public CardDesign setBackgroundPosition(Position position) {
		if (position == null) throw new IllegalArgumentException("null position");
		checkMutable();
		this.position = position;
		return this;
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return contentsByLocation.isMutable();
	}

	@Override
	public CardDesign mutableCopy() {
		return new CardDesign(layout, locations, contentsByLocation.mutableCopy(), position);
	}

	@Override
	public CardDesign immutableCopy() {
		return new CardDesign(layout, locations, contentsByLocation.immutableCopy(), position);
	}

	@Override
	public CardDesign immutableView() {
		return new CardDesign(layout, locations, contentsByLocation.immutableView(), position);
	}

	// private utility methods

	private void checkMutable() {
		if (!contentsByLocation.isMutable()) throw new IllegalStateException("immutable");
	}
}
