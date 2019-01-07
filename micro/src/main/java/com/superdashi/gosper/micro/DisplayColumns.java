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

import java.util.Arrays;
import java.util.List;

public final class DisplayColumns {

	public final boolean navigation;
	public final boolean label;
	public final List<Badge> badges;

	public DisplayColumns(boolean navigation, boolean label, Badge... badges) {
		if (badges == null) throw new IllegalArgumentException("null badges");
		this.badges = Arrays.asList(badges.clone());
		if (this.badges.contains(null)) throw new IllegalArgumentException("null badge");
		this.navigation = navigation;
		this.label = label;
	}
}