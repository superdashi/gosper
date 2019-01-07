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

import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.ColorConfig;
import com.superdashi.gosper.config.Config.PaletteConfig;
import com.superdashi.gosper.config.Config.VectorConfig;

public class DesignConfig implements ConfigTarget {

	// defaults

	public static final Palette PALETTE_DEFAULT = Palette.newBuilder(
			// darks
			0xff000000,
			0xff2168f4,
			0xff44869e,
			0xffff8200,
			0xff39b17b,
			0xffce9200,
			0xffc23500,
			0xff67676d,

			// lights
			0xffffffff,
			0xff00c7ff,
			0xffd1efff,
			0xffffb657,
			0xff5fffb6,
			0xffffcc00,
			0xffff5300,
			0xffd3d3e0

			).build();
	public static final int AMBIENT_COLOR_DEFAULT = 0xffffffff;
	// TODO unsafe
	public static final float[] LIGHT_DIRECTION_DEFAULT = {0f, 0f, 1f};
	public static final int LIGHT_COLOR_DEFAULT = 0xff404040;

	// fields

	public Palette palette = PALETTE_DEFAULT;
	public int     ambientColor = AMBIENT_COLOR_DEFAULT;
	public float[] lightDirection = LIGHT_DIRECTION_DEFAULT;
	public int     lightColor = LIGHT_COLOR_DEFAULT;

	public void adopt(DesignConfig that) {
		this.palette = that.palette;
		this.ambientColor = that.ambientColor;
		this.lightDirection = that.lightDirection;
		this.lightColor = that.lightColor;
	}

	@Override
	public void applyStyling(ConfigProperty property, PaletteConfig palette) {
		switch (property.name) {
		case "palette" :
			this.palette = palette.asPalette();
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, ColorConfig color) {
		switch (property.name) {
		case "ambient-color":
			ambientColor = color.asInt();
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, VectorConfig vector) {
		switch (property.name) {
		case "light-direction":
			lightDirection = vector.asFloats();
			break;
		}
	}

}
