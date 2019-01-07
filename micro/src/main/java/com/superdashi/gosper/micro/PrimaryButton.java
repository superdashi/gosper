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

import java.util.Optional;

import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.Position.Fit;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.IntFontMetrics;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.TypefaceMetrics;
import com.superdashi.gosper.studio.Canvas.State;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

public class PrimaryButton extends Component {

	private static final ActionModel defaultModel = new ActionModel(Action.create(Action.ID_DIALOG_OPTION, "Button"));
	//TODO expose on visual context
	private static final IntMargins focusMargins = IntMargins.uniform(1);

	//TODO obtain from location
	private static final Position position = Position.from(Fit.FREE, Fit.FREE, Alignment2D.pair(Alignment.MID, Alignment.MID));

	private final Focusing focusing = new Focusing() {

		@Override
		public void cedeFocus() {
			shapeChanged = true;
			situation.requestRedrawNow();
		}

		@Override
		public void receiveFocus(int index) {
			shapeChanged = true;
			situation.requestRedrawNow();
		}

		@Override
		public IntRect focusArea() {
			return bounds;
		}
	};

	private final Eventing eventing = new Eventing() {
		@Override
		public boolean handleEvent(Event event) {
			if (event.isKey() && event.isDown() && event.key == Event.KEY_CONFIRM) {
				return activate();
			}
			return false;
		}
	};

	private final Pointing pointing = new Pointing() {
		@Override
		public boolean clicked(int areaIndex, IntCoords coords) {
			situation.focus();
			activate();
			return true;
		}
	};

	private IntMargins margins;
	private TextStyle textStyle;
	private Style style;

	private ActionModel model = defaultModel;
	private long revision = -1L;

	// updated on bounds computation
	private IntRect bounds;
	private IntDimensions labelDims;
	private boolean shapeChanged;

	public void model(ActionModel model) {
		if (model == null) model = defaultModel;
		if (model == this.model) return;
		this.model = model;
		situation.currentAction(model.action());
		if (situation.isPlaced()) computeBounds();
		revision = -1L;
		situation.requestRedrawNow();
	}

	public ActionModel model() {
		return model == defaultModel ? null : model;
	}

	// convenience method
	public ActionModel action(Action action) {
		ActionModel model = action == null ? null : situation.models().actionModel(action);
		model(model);
		return model;
	}

	@Override
	IntDimensions minimumSize() {
		String label = model.itemModel().label();
		TypefaceMetrics typeMetrics = situation.visualSpec().typeMetrics;
		int width = typeMetrics.intRenderedWidthOfString(textStyle, label);
		IntFontMetrics fontMetrics = typeMetrics.fontMetrics(textStyle);
		int height = fontMetrics.top + fontMetrics.bottom;
		IntDimensions labelDims = IntDimensions.of(width, height);
		return labelDims.plus(margins).plus(focusMargins);
	}

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		margins = situation.visualSpec().metrics.dialogButtonMargins;
		style = situation.visualSpec().theme.styles.defaultButtonStyle.mutable().apply(situation.style).immutable();
		textStyle = TextStyle.fromStyle(style);
	}

	@Override
	void place(Place place, int z) {
		computeBounds();
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
		if (shapeChanged) return Changes.SHAPE;
		return revision == model.revision() ? Changes.NONE : Changes.CONTENT;
	}

	@Override
	void render() {
		VisualSpec spec = situation.visualSpec();
		int bgColor = style.colorBg();
		int fgColor = style.colorFg();
		Canvas canvas = situation.defaultPane().canvas();

		IntRect bounds = this.bounds.translatedToOrigin();
		IntVector offset = focusMargins.offset();
		if (!situation.isFocused()) {
			bounds = bounds.minus(focusMargins);
		}

		State state = canvas.recordState();
		if (model.enabled()) {
			canvas.color(bgColor);
		} else {
			//TODO should come from context?
			canvas.shader(Background.mono(2).asShader().get());
		}
		canvas.erase().intOps().clipRect(bounds).canvas().fill().color(fgColor);
		int avail = bounds.width() - margins.minX - margins.maxX;
		String label = model.itemModel().label();
		String text = spec.accommodatedString(textStyle, label, avail);
		int baseline = spec.typeMetrics.fontMetrics(textStyle).baseline;
		canvas.intOps().newText(spec.typeface).moveTo(offset.x - margins.minX, offset.y + baseline - margins.minY).renderString(textStyle, text);
		state.restore();

		shapeChanged = false;
		revision = model.revision();
	}

	@Override
	Optional<Focusing> focusing() {
		return Optional.of(focusing);
	}

	@Override
	Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	@Override
	Optional<Pointing> pointing() {
		return Optional.of(pointing);
	}

	private void computeBounds() {
		IntDimensions dims = minimumSize();
		IntRect innerBounds = situation.place().innerBounds;
		IntRect newBounds = position.position(dims, innerBounds);
		if (!newBounds.equals(bounds)) {
			bounds = newBounds.intersectRect(innerBounds);
			shapeChanged = true;
		}
	}

	private boolean activate() {
		if (!model.enabled()) return false;
		situation.instigateCurrentAction();
		return true;
	}

}
