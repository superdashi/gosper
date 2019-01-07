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
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.Surface;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public final class Scrollbar extends Component {

	// statics

	private static final IntDimensions TWO_X_TWO = IntDimensions.of(2, 2);

	// fields

	//TODO could elminate this reference to bounds
	private IntRect bounds;
	private Frame basis2;

	private ScrollbarModel model = null;
	private long revision = -2L; // -2 indicates change, -1 indicates blank rendered

	// constructors

	Scrollbar() { }

	// public accessors

	public void model(ScrollbarModel model) {
		if (model == this.model) return; // no change
		if (model != null && model.matches(this.model)) {
			this.model = model;
			revision = model.mutations.count;
			return; // no visible change
		}
		this.model = model;
		revision = -2L;
		situation.requestRedrawNow();
	}

	public ScrollbarModel model() {
		return model;
	}

	// component methods

	@Override
	void place(Place place, int z) {
		bounds = place.innerBounds;
		IntDimensions dimensions = bounds.dimensions();
		int w = dimensions.width;
		int h = dimensions.height;

		//TODO should be attached to the studio
		Surface surface = Surface.create(dimensions, true);
		Canvas canvas = surface.createCanvas();
		//TODO should come from context
		canvas.shader( Background.mono(2).asShader().get() ).fill();
		canvas.color(Argb.BLACK).intOps()
			.fillRect( IntRect.rectangle(0,     0,     w, 1) )
			.fillRect( IntRect.rectangle(0,     h - 1, w, 1) )
			.fillRect( IntRect.rectangle(0,     0,     1, h) )
			.fillRect( IntRect.rectangle(w - 1, 0,     1, h) )
			;
		canvas.destroy();
		basis2 = surface.immutableView();
	}

	@Override
	Composition composition() {
		return Composition.FILL;
	}

	@Override
	IntRect bounds() {
		return bounds;
	}

	@Override
	Changes changes() {
		return isDirty() ? Changes.CONTENT : Changes.NONE;
	}

	@Override
	void render() {
		if (!situation.dirty() && !isDirty()) return;
		Canvas canvas = situation.defaultPane().canvas();
		canvas.drawFrame(basis2);

		if (model != null) {
			int min = model.range().min;
			int max = model.range().max;
			int start = model.span().min;
			int finish = model.span().max;
			int r = max - min;
			int t = finish - start;
			if (t == r) return;

			int top = 2;
			int bottom = bounds.height() - 2;
			int s = bottom - top;
			VisualSpec spec = situation.visualSpec();
			//TODO handle case where bounds not big enough in a better way?
			if (s < spec.metrics.scrollbarMinHeight) return;

			//TODO guard against overflows
			int h = Math.max(Math.round(t * s / r), spec.metrics.scrollbarMinHeight);
			int minY = top + Math.round( start * (s - h) / (r - t) );
			int maxY = minY + h;

			// convenient values for rendering
			int left = 1;
			int right = bounds.width() - 1;
			canvas.color(Argb.BLACK).intOps().fillRect(IntRect.bounded(left, minY, right, minY + 1));
			canvas.color(Argb.BLACK).intOps().fillRect(IntRect.bounded(left, maxY - 1, right, maxY));
			canvas.color(Argb.WHITE).intOps().fillRect(IntRect.bounded(left + 1, minY + 1, right - 1, maxY - 1));

			// indicate max/min values reached
			if (start == min) {
				canvas.color(Argb.WHITE).intOps().fillRect(IntRect.bounded(left, minY - 1, right, minY));
			}
			if (finish == max) {
				canvas.color(Argb.WHITE).intOps().fillRect(IntRect.bounded(left, maxY, right, maxY + 1));
			}
		}

		revision = modelRevision();
	}

	// private helper methods

	private long modelRevision() {
		return model == null ? -1 : model.mutations.count;
	}

	private boolean isDirty() {
		long modelRevision = modelRevision();
		return revision != modelRevision;
	}
}
