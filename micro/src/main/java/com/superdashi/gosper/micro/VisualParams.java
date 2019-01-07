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

import static com.superdashi.gosper.micro.Visuals.extract;

import java.util.Map;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;

public abstract class VisualParams {

	static final VisualStyles noStyles = new VisualStyles(Style.noStyle(), new Style().colorBg(Argb.WHITE), Style.noStyle(), new Style().colorBg(Argb.WHITE), new Style().colorBg(Argb.BLACK).colorFg(Argb.WHITE));

	final Map<String, Object> map;
	final VisualStyles styles;

	// may only be created by this package
	VisualParams(Map<String, Object> map) {
		this.map = map;
		styles = extract(map, Visuals.STYLES);
	}

}
