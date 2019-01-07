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
package com.superdashi.gosper.config;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

import com.superdashi.gosper.config.Config.Type;

//TODO should record type of elements that property can apply to?
public final class ConfigProperty {

	private static ConfigProperty[] properties = {
		Type.VALIGN.named("background-align"),
		Type.VALIGN.named("vertical-align"),
		Type.LENGTH.named("gutter"),
		Type.LENGTH.named("height"),
		Type.LENGTH.named("background-height"),
		Type.COLOR.named("ambient-color"),
		Type.VECTOR.named("light-direction"),
		Type.OFFSET.named("offset"),
		Type.PALETTE.named("palette"),
		Type.PATH.named("request-path"),
		Type.RESOURCE.named("pattern"),
		Type.SCHEDULE.named("schedule"),
		Type.COLORING.named("coloring"),
		Type.VALIGN.named("vertical-align"),
		Type.DURATION.named("transition-duration"),
		Type.DURATION.named("display-duration"),
	};

	private static String[] names = new String[ properties.length ];

	static {
		Arrays.sort(properties, (p1, p2) -> {return p1.name.compareTo(p2.name);});
		for (int i = 0; i < names.length; i++) {
			ConfigProperty p = properties[i];
			p.index = i;
			names[i] = p.name;
		}
	}

	public static Comparator<ConfigProperty> byName() {
		return (s,t) -> s.name.compareTo(t.name);
	}

	public static Optional<ConfigProperty> forName(String name) {
//		Stores.objectsAndNull(properties).asTransformedBy(Mapping.fromFunction(StyleProperty.class, String.class, p -> p.name));
//		Perfect<String> perfect = Perfect.over(Stores.objectsAndNull(properties).asTransformedBy(Mapping.fromFunction(StyleProperty.class, String.class, p -> p.name))).usingDefaults().perfect((v, s) -> { s.writeChars(v); });
		int index = Arrays.binarySearch(names, name);
		return index < 0 ? Optional.empty() : Optional.of(properties[index]);

	}

	public final String name;
	public final Type type;
	private final Configuration inherited;
	private int index;

	ConfigProperty(String name, Type type) {
		this.name = name;
		this.type = type;
		inherited = new Configuration(this, null);
	}

	public Configuration inherited() {
		return inherited;
	}

	public Configuration styling(Config config) {
		if (config == null) throw new IllegalArgumentException("null config");
		if (config.type() != this.type) throw new IllegalArgumentException("mismatched config type");
		return new Configuration(this, config);
	}

	//TODO needs to catch IAE and report
	public Configuration parse(String str) {
		return new Configuration(this, type.parse(str));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof ConfigProperty)) return false;
		ConfigProperty that = (ConfigProperty) obj;
		return this.name == that.name && this.type == that.type;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name + ':' + type;
	}
}
