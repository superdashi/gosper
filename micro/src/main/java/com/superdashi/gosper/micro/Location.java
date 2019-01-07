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
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

//note: this is not an enum in case more more general locations (place names) become supported
// if this becomes the case we can add a from(String) method and generalize equality etc.
public final class Location {

	private static final Pattern pattern = Pattern.compile("[a-z]+(-[a-z]+)*");

	private static final Map<String, Location> canons = new HashMap<>();
	private static final Map<String, Location> nonCanons = new WeakHashMap<>();

	static Map<String, Location> canons() {
		return Collections.unmodifiableMap(canons);
	}

	public static Location named(String name) {
		Location location = canons.get(name);
		if (location != null) return location;
		location = nonCanons.get(name);
		if (location != null) return location;
		if (!pattern.matcher(name).matches()) throw new IllegalArgumentException("invalid location name");
		return new Location(name, false);
	}

	public static final Location center = new Location("center", true);
	public static final Location left = new Location("left", true);
	public static final Location right = new Location("right", true);
	public static final Location top = new Location("top", true);
	public static final Location bottom = new Location("bottom", true);
	public static final Location centerLeft = new Location("center-left", true);
	public static final Location centerRight = new Location("center-right", true);
	public static final Location centerTop = new Location("center-top", true);
	public static final Location centerBottom = new Location("center-bottom", true);

	public static final Location topbar = new Location("topbar", true);
	public static final Location scrollbar = new Location("scrollbar", true);

	final String name;
	final boolean canon;

	private Location(String name, boolean canon) {
		this.name = name;
		this.canon = canon;
		Map<String, Location> map = canon ? canons : nonCanons;
		map.put(name, this);
	}

	@Override
	public String toString() {
		return name;
	}
}
