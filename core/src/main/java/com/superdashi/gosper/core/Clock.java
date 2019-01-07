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

import java.util.Optional;

import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Configurable;

public class Clock implements Configurable {

	private final ClockConfig givenStyle;
	public final ClockConfig style = new ClockConfig();

	public Clock(ClockConfig givenStyle) {
		this.givenStyle = givenStyle;
	}

	@Override
	public Type getStyleType() {
		return Type.CLOCK;
	}

	@Override
	public String getId() {
		// TODO
		return null;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

}
