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
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.Canvas.State;
import com.superdashi.gosper.studio.PorterDuff.Rule;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRect;

public class Active extends Component {

	//TODO return to package scope?
	public static int recommendedSize(VisualSpec spec) {
		switch (spec.qualifier.qualifier.screen) {
		case MICRO: return 16;
		case MINI: return 32;
		case PC: return 64;
		case NONE: throw new IllegalArgumentException("no screen class");
		default: throw new IllegalStateException();
		}
	}

	private static final long FRAME_DELAY = 150L;
	private static final long DRAW_PERIOD = 3000L;
	private static final long DRAW_DELAY = 50L;
	private static final ActiveModel defaulModel = new ActiveModel(null);

	// initialized during situation
	private IntRect bounds;
	private int size;

	private ActiveModel model = defaulModel;
	private ActiveModel visible = null;
	private long delay;

	private int lastFrame = 0;
	private Frame[] frames = null;
	private IntDimensions dimensions = null;

	public ActiveModel model() {
		return model;
	}

	public void model(ActiveModel model) {
		if (model == null) model = defaulModel;
		if (this.model == model) return;
		this.model = model;
		visible = null;
	}

	@Override
	void situate(Situation situation) {
		this.situation = situation;

		// load the animation frames if necessary
		VisualSpec spec = situation.visualSpec();
		if (spec.qualifier.qualifier.screen == ScreenClass.MICRO) {
			//TODO need to move onto background thread
			frames = spec.activeFrames();
			dimensions = spec.activeDimensions();
			delay = FRAME_DELAY;
		} else {
			delay = DRAW_DELAY;
		}
		situation.requestRedrawPeriodicallyDelayed(0, delay);
	}

	@Override
	IntDimensions minimumSize() {
		return IntDimensions.square( recommendedSize(situation.visualSpec()) );
	}

	@Override
	void place(Place place, int z) {
		// compute the largest square that fits into our bounds
		IntRect bounds = place.innerBounds;
		int width = bounds.width();
		int height = bounds.height();
		size = Math.min(width, height);
		IntDimensions dims = IntDimensions.square(size);
		if (size < width) {
			this.bounds = IntRect.rectangle(IntCoords.at(bounds.minX + (width - size)/2, bounds.minY), dims);
		} else if (size < height) {
			this.bounds = IntRect.rectangle(IntCoords.at(bounds.minX, bounds.minY + (width - size)/2), dims);
		} else {
			this.bounds = bounds;
		}
	}

	@Override
	Composition composition() {
		return Composition.MASK;
	}

	@Override
	IntRect bounds() {
		return bounds;
	}

	@Override
	Changes changes() {
		if (!model.equals(visible)) return Changes.SHAPE;
		if (frames != null) return currentFrame() == lastFrame ? Changes.NONE : Changes.SHAPE;
		return Changes.SHAPE;
	}

	@Override
	void render() {
		Canvas canvas = situation.defaultPane().canvas().erase();
		if (frames != null) {
			int frame = currentFrame();
			if (frame < 0) {
				/* we show nothing when inactive */
			} else {
				canvas.drawFrame(frames[frame]);
			}
			lastFrame = frame;
		} else {
			//TODO need stroking!
			IntRect rect = bounds.translatedToOrigin();
			float cx = rect.width() * 0.5f;
			float cy = rect.height() * 0.5f;
			Rect tmp = Rect.atPoints(0, cy - 3, rect.maxX, cy + 3);
			long t = System.currentTimeMillis() % DRAW_PERIOD;
			float angle = Angles.TWO_PI * t / DRAW_PERIOD;
			Transform xf1 = Transform.rotationAbout(new Point(cx, cy), angle);
			Transform xf2 = Transform.rotationAbout(new Point(cx, cy), Angles.PI_BY_TWO);
			State state = canvas.recordState();
			canvas.color(Argb.WHITE).intOps().fillEllipse(rect);
			canvas
				.composer(new PorterDuff(Rule.SRC_OUT).asComposer())
				.intOps()
				.fillEllipse(rect.minus(IntMargins.uniform(5)))
				.canvas()
				.composer(new PorterDuff(Rule.CLEAR).asComposer())
				.floatOps()
				.transform(xf1)
				.fillRect(tmp)
				.transform(xf2)
				.fillRect(tmp);
			state.restore();
		}
		visible = model.snapshot();
	}

	private int currentFrame() {
		return model.active() ? (int) ((System.currentTimeMillis() / FRAME_DELAY) % frames.length) : -1;
	}
}
