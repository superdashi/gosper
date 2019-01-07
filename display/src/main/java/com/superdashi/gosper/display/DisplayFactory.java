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

import java.net.URI;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.core.Bar;
import com.superdashi.gosper.core.BarConfig;
import com.superdashi.gosper.core.CacheException;
import com.superdashi.gosper.core.Clock;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Panel;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Position.Fit;
import com.superdashi.gosper.util.TextCanvas;
import com.tomgibara.geom.core.Rect;

public class DisplayFactory {

	final DisplayContext context;

	public DisplayFactory(DisplayContext context) {
		if (context == null) throw new IllegalArgumentException("null context");
		this.context = context;
	}

	public ElementDisplay createDisplay(Bar bar) {
		BarConfig style = bar.style;
		Alignment alignment = style.align;
		ArtPlane plane = context.getOverPlane();
		float height = style.height * plane.height;
		float bgHeight = style.bgHeight * height;
		if (bgHeight == 0f) return null; // no display
		Rect pr = plane.rect;
		float off = pr.minY + (plane.height - height) * alignment.m;
		off += style.bgAlign.m * (height - bgHeight);
		Rect rect = Rect.atPoints(pr.minX, off, pr.maxX, off + bgHeight);
		TextCanvas canvas = null;
		URI pattern = style.pattern;
		if (pattern != null) {
			try {
				canvas = context.getCache().cachedTextCanvas(pattern);
			} catch (CacheException e) {
				DashiLog.warn("Failed to load bar pattern from {0}", e, pattern);
				canvas = null;
			}
		}
		if (canvas != null) {
			Console console = new Console(canvas, context.getDefaultCharMap());
			//TODO want configurable position
			Position pos = Position.from(Fit.FREE, Fit.MATCH, Alignment2D.pair(Alignment.MID, Alignment.MIN));
			//TODO want configurable palette
			Palette pal = context.getDefaultPalette();
			return new ConsoleDisplay(console, rect, plane.depth, pos, Wrap.wraps(Wrap.REPEAT, Wrap.CLAMP), pal, Coloring.BLACK_COLORING);
		}
		Coloring coloring = style.coloring;
		if (coloring != null) {
			return new AtomicDisplay(new ColoredRect(rect, plane.depth, coloring));
		}
		return null;
	}

	public ElementDisplay createDisplay(Clock clock) {
		//TODO how to determine phase;
		return new ClockDisplay(clock, RenderPhase.OVERLAY);
	}

	public PanelDisplay createDisplay(Panel panel) {
		return new PanelDisplay(panel, context);
	}
}
