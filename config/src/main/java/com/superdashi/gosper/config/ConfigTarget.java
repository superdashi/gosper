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

import com.superdashi.gosper.config.Config.AlignConfig;
import com.superdashi.gosper.config.Config.ColorConfig;
import com.superdashi.gosper.config.Config.ColoringConfig;
import com.superdashi.gosper.config.Config.DurationConfig;
import com.superdashi.gosper.config.Config.LengthConfig;
import com.superdashi.gosper.config.Config.OffsetConfig;
import com.superdashi.gosper.config.Config.PaletteConfig;
import com.superdashi.gosper.config.Config.PathConfig;
import com.superdashi.gosper.config.Config.ResourceConfig;
import com.superdashi.gosper.config.Config.ScheduleConfig;
import com.superdashi.gosper.config.Config.VectorConfig;

public interface ConfigTarget extends AutoCloseable {

	@Override
	default public void close() {}

	default void applyStyling(ConfigProperty property, AlignConfig align)       {}
	default void applyStyling(ConfigProperty property, DurationConfig duration) {}
	default void applyStyling(ConfigProperty property, ColorConfig color)       {}
	default void applyStyling(ConfigProperty property, ColoringConfig coloring) {}
	default void applyStyling(ConfigProperty property, LengthConfig length)     {}
	default void applyStyling(ConfigProperty property, OffsetConfig offset)     {}
	default void applyStyling(ConfigProperty property, PaletteConfig palette)   {}
	default void applyStyling(ConfigProperty property, PathConfig path)         {}
	default void applyStyling(ConfigProperty property, ResourceConfig resource) {}
	default void applyStyling(ConfigProperty property, ScheduleConfig schedule) {}
	default void applyStyling(ConfigProperty property, VectorConfig vector)     {}
	default void inheritStyling(ConfigProperty property) {}

}
