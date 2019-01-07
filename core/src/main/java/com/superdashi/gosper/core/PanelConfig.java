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

import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.LengthConfig;
import com.superdashi.gosper.config.Config.OffsetConfig;
import com.tomgibara.geom.core.Offset;

public final class PanelConfig implements ConfigTarget {

	public static final LengthRange GUTTER_RANGE = new LengthRange(0f, 0.1f, 0.015f);
	public static final Offset OFFSET_DEFAULT = Offset.IDENTITY;

	public float   gutter = GUTTER_RANGE.def;
	public Offset  offset = OFFSET_DEFAULT;

	public void adopt(PanelConfig that) {
		this.gutter = that.gutter;
		this.offset = that.offset;
	}

	// style target

	@Override
	public void applyStyling(ConfigProperty property, LengthConfig length) {
		switch (property.name) {
		case "gutter":
			gutter = GUTTER_RANGE.lengthOf(length);
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, OffsetConfig offset) {
		switch (property.name) {
		case "offset":
			this.offset = offset.asOffset();
			break;
		}
	}

}
