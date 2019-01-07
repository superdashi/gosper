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

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.layout.Style;

public class Badges {

	private static void checkProperty(String property) {
		if (property == null) throw new IllegalArgumentException("null property");
		if (property.isEmpty()) throw new IllegalArgumentException("empty property");
	}
	private final VisualSpec spec;
	private final Models models;
	private final Style style;

	Badges(VisualSpec spec, Models models) {
		this(spec, models, spec.styles.defaultBadgeStyle);
	}

	private Badges(VisualSpec spec, Models models, Style style) {
		assert spec != null;
		assert models != null;
		this.spec = spec;
		this.models = models;
		this.style = style;
	}

	public Badges withStyle(Style style) {
		if (style == null) throw new IllegalArgumentException("null style");
		style = this.style.mutable().apply(style).immutable();
		return new Badges(spec, models, style);
	}

	public Badge firstLetter(String property, boolean capitalize) {
		checkProperty(property);
		return new Badge.LetterBadge(spec, style, property, capitalize);
	}

	public Badge numberBadge(String property) {
		checkProperty(property);
		return new Badge.NumberBadge(spec, style, property);
	}

	public Badge indexedBadge(String property, Item... items) {
		checkProperty(property);
		// items checked by this method...
		ItemModel[] itemModels = models.itemModels(items);
		return new Badge.IndexedBadge(spec, style, property, itemModels);
	}

	public Badge checkboxBadge(String property, boolean checkedByDefault, Value switchValue) {
		checkProperty(property);
		return new Badge.CheckboxBadge(spec, style, property, checkedByDefault, switchValue);
	}

}
