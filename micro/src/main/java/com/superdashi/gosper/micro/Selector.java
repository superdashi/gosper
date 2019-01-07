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

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Canvas.IntOps;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

//TOD why public?
public class Selector {

	public enum State {
		NAVIGABLE,
		NAVIGATED,
		SELECTABLE,
		SELECTED;

		public static State of(boolean selectable, boolean active) {
			if (active) {
				return selectable ? State.SELECTED : State.NAVIGATED;
			} else {
				return selectable ? State.SELECTABLE : State.NAVIGABLE;
			}
		}

	}

	private final VisualSpec spec;
	private final int controlColor;
	private final int actionColor;

	public Selector(VisualSpec spec, boolean inverted) {
		this.spec = spec;
		if (inverted) {
			controlColor = Argb.inverse(spec.theme.controlColor);
			actionColor = Argb.inverse(spec.theme.actionColor);
		} else {
			controlColor = spec.theme.controlColor;
			actionColor = spec.theme.actionColor;
		}
	}

	public int width() {
		return 5;
	}

	public int height() {
		return 5;
	}

	public void render(Canvas canvas, State state, boolean focused) {
		switch (state) {
		case NAVIGABLE:
			plotNavigable(canvas, focused);
			break;
		case NAVIGATED:
			plotNavigated(canvas, focused);
			break;
		case SELECTABLE:
			plotSelectable(canvas, focused);
			break;
		case SELECTED:
			plotSelected(canvas, focused);
			break;
		default:
			// should be impossible
			throw new IllegalStateException();
		}
	}

	public void render(Canvas canvas, IntVector translation, State state, boolean focused) {
		canvas.intOps().translate(translation);
		try {
			render(canvas, state, focused);
		} finally {
			canvas.intOps().translate(translation.negate());
		}
	}

	public void render(Canvas canvas, IntRect bounds, State state, boolean focused) {
		//canvas.intOps().clipRectAndTranslate(bounds).canvas().recordState();
		//canvas.intOps().translate(bounds.vectorToMinimumCoords()).canvas().recordState();
		canvas.intOps().translate(bounds.vectorToMinimumCoords()).canvas();
		try {
			render(canvas, state, focused);
		} finally {
//			canvas.restorePreviousState();
			canvas.intOps().translate(bounds.vectorToMinimumCoords().negate());
		}
	}

	private void plotNavigable(Canvas canvas, boolean focused) {
		canvas.color(controlColor).intOps()
			.plotPixel(1, 0)
			.plotPixel(1, 0)
			.plotPixel(3, 0)
			.plotPixel(0, 1)
			.plotPixel(4, 1)
			.plotPixel(0, 3)
			.plotPixel(4, 3)
			.plotPixel(1, 4)
			.plotPixel(3, 4)
			;
	}

	private void plotNavigated(Canvas canvas, boolean focused) {
		plotNavigable(canvas, focused);
		IntOps ops = canvas.color(controlColor).intOps();
		if (focused) {
			ops.plotPixel(2, 1);
			ops.plotPixel(1, 2);
			ops.plotPixel(3, 2);
			ops.plotPixel(2, 3);
		}
		ops.plotPixel(2, 2);
	}

	private void plotSelectable(Canvas canvas, boolean focused) {
		canvas.color(actionColor).intOps()
			.fillRect(IntRect.rectangle(1, 0, 3, 1))
			.fillRect(IntRect.rectangle(0, 1, 1, 3))
			.fillRect(IntRect.rectangle(4, 1, 1, 3))
			.fillRect(IntRect.rectangle(1, 4, 3, 1));
	}

	private void plotSelected(Canvas canvas, boolean focused) {
		if (focused) {
			canvas.color(actionColor).intOps()
				.fillRect(IntRect.rectangle(1, 0, 3, 1))
				.fillRect(IntRect.rectangle(0, 1, 5, 1))
				.fillRect(IntRect.rectangle(0, 2, 5, 1))
				.fillRect(IntRect.rectangle(0, 3, 5, 1))
				.fillRect(IntRect.rectangle(1, 4, 3, 1));
		} else {
			plotSelectable(canvas, false);
			canvas.color(actionColor).intOps().plotPixel(2, 2);
		}
	}
}
