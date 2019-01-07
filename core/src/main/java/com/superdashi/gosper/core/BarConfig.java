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
package com.superdashi.gosper.core;

import java.net.URI;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.AlignConfig;
import com.superdashi.gosper.config.Config.ColoringConfig;
import com.superdashi.gosper.config.Config.LengthConfig;
import com.superdashi.gosper.config.Config.ResourceConfig;
import com.superdashi.gosper.layout.Alignment;

public class BarConfig implements ConfigTarget {

	public static final LengthRange HEIGHT_RANGE = new LengthRange(0.1f, 0.5f, 0.25f);
	public static final LengthRange BG_HEIGHT_RANGE = new LengthRange(0f, 2f, 1f);
	public static final Alignment ALIGN_DEFAULT = Alignment.MID;
	public static final Alignment BG_ALIGN_DEFAULT = Alignment.MID;

	public Coloring coloring = null;
	public URI pattern = null;
	public float height = HEIGHT_RANGE.def;
	public Alignment align = ALIGN_DEFAULT;
	public float bgHeight = BG_HEIGHT_RANGE.def;
	public Alignment bgAlign = BG_ALIGN_DEFAULT;

	public void adopt(BarConfig that) {
		this.coloring = that.coloring;
		this.pattern = that.pattern;
	}

	@Override
	public void applyStyling(ConfigProperty property, AlignConfig align) {
		switch (property.name) {
		case "vertical-align" :
			this.align = align.asAlign();
			break;
		case "background-align" :
			this.bgAlign = align.asAlign();
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, ColoringConfig coloring) {
		switch (property.name) {
		case "coloring" :
			this.coloring = coloring.asColoring();
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, LengthConfig length) {
		switch (property.name) {
		case "height" :
			this.height = HEIGHT_RANGE.lengthOf(length);
			break;
		case "background-height" :
			this.bgHeight = BG_HEIGHT_RANGE.lengthOf(length);
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, ResourceConfig resource) {
		switch (property.name) {
		case "pattern" :
			this.pattern = resource.asURI();
			break;
		}
	}
}
