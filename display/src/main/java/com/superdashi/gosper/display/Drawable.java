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
package com.superdashi.gosper.display;

import java.awt.Graphics2D;

import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.intgeom.IntRect;

public interface Drawable {

	static Resolution optimalResolution(Resolution maxRes, Drawable... drawables) {
		maxRes = maxRes.potUpTo();
		Resolution res = null;
		for (Drawable drawable : drawables) {
			Resolution r = drawable.getResolution();
			res = res == null ? r : res.accommodating(r);
		}
		return res == null ? maxRes : res.constrainedBy(maxRes).potDownTo();
	}

	void drawTo(Graphics2D g, IntRect rect, Position pos);

	int getNumberOfChannels();

	Resolution getResolution();

}
