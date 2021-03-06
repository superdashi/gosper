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

public class Background extends DesignChild {

	private final BackgroundConfig DEFAULT_STYLE = new BackgroundConfig();

	public final BackgroundConfig style = new BackgroundConfig();
	private BackgroundConfig givenStyle = DEFAULT_STYLE;

	private InfoAcquirer infoAcquirer = null;

	public void infoAcquirer(InfoAcquirer infoAcquirer) {
		this.infoAcquirer = infoAcquirer;
	}

	public InfoAcquirer infoAcquirer() {
		return infoAcquirer;
	}

	// styleable

	@Override
	public Type getStyleType() {
		return Type.BACKGROUND;
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

}
