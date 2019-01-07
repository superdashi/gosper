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
package com.superdashi.gosper.studio;

import java.awt.Composite;
import java.awt.Graphics2D;

public abstract class Composer {

	private Composite composite;

	Composer() { }

	// this API required instead of more natural Composite toComposite()
	// because setXORColor hides the composite instance.
	void applyTo(Graphics2D g) {
		g.setComposite(composite == null ? composite = createComposite() : composite);
	}

	abstract Composite createComposite();
}
