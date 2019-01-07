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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.device.Event;
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.device.Event.Type;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.TypefaceMetrics;
import com.superdashi.gosper.studio.Canvas.IntOps;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.superdashi.gosper.studio.Canvas.State;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

public class Bar extends Component {

	private static final Composer clear = new PorterDuff(PorterDuff.Rule.CLEAR).asComposer();
	private static final Style boldStyle = new Style().textWeight(1).immutable();
	private static final Object buttonInd = new Object();

	// set in constructor
	private final boolean includeBack;
	private final EventMask eventMask;
	private final Focusing focusing;
	private final Eventing eventing;
	private final Pointing pointing;

	// set when situating
	private VisualSpec spec;
	private List<Indicator> indicators = new ArrayList<>();
	private IntRect bounds;
	private IntRect buttonBounds;
	private Frame buttonFrame;
	private Frame buttonFrameFocused;
	private Object focused; // set to the bounds if focused - in future, might be settable to indicators

	private String boldText = "";
	private String plainText = "";

	private boolean maskDirty = true; // if the mask has changed (often caused by new indicators)
	private boolean buttDirty = false; // if button has change (eg from focusing)
	private boolean textDirty = true; // if text has been changed
	private boolean indiDirty = true; // if indicators have been added/removed
	private boolean contDirty = false;// if indicator content has changed

	private int indiWidth;

	public Bar(boolean includeBack) {
		this.includeBack = includeBack;
		if (includeBack) {
			focusing = new Focusing() {
				@Override public IntRect focusArea() { return buttonBounds; }
				@Override public void receiveFocus(int areaIndex) {
					focused = buttonInd;
					buttDirty = true;
					situation.requestRedrawNow();
				}
				@Override
				public void cedeFocus() {
					focused = null;
					buttDirty = true;
					situation.requestRedrawNow();
				}
				@Override
				public boolean focusableByDefault() {
					return false;
				}
			};
			eventMask = EventMask.builder().type(Type.KEY).down(true).key(Event.KEY_CONFIRM).build();
			eventing = new Eventing() {
				@Override
				public EventMask eventMask() {
					return eventMask;
				}
				@Override
				public boolean handleEvent(Event event) {
					situation.concludeActivity();
					return true;
				}
			};
			pointing = new Pointing() {
				@Override
				public List<IntRect> clickableAreas() {
					return Collections.singletonList(buttonBounds);
				}
				@Override
				public boolean clicked(int areaIndex, IntCoords coords) {
					situation.concludeActivity();
					return true;
				}
			};
			buttDirty = true;
		} else {
			focusing = null;
			eventing = null;
			pointing = null;
			eventMask = null;
		}
	}

	public void setBoldText(String boldText) {
		if (boldText == null) throw new IllegalArgumentException("null boldText");
		if (boldText.equals(this.boldText)) return;
		this.boldText = boldText;
		textDirty = true;
	}

	public void setPlainText(String plainText) {
		if (plainText == null) throw new IllegalArgumentException("null plainText");
		if (plainText.equals(this.plainText)) return;
		this.plainText = plainText;
		textDirty = true;
	}

	// component methods

	@Override
	void situate(Situation situation) {
		this.situation = situation;
		spec = situation.visualSpec();
	}

	@Override
	void place(Place place, int z) {
		bounds = place.innerBounds;
		if (includeBack) {
			//TODO needs own metrics
			buttonBounds = IntRect.squareAtOrigin(spec.metrics.badgeSize).translatedBy(IntVector.toX(spec.metrics.sideMargin));
			buttonFrame = spec.barBack();
			buttonFrameFocused = spec.barBackFocused();
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
		updateDirties();
		if (maskDirty) return Changes.SHAPE;
		if (textDirty || indiDirty || contDirty || buttDirty) return Changes.CONTENT;
		return Changes.NONE;
	}

	@Override
	void render() {
		updateDirties();
		Canvas canvas = situation.defaultPane().canvas();
		if (maskDirty) {
			canvas.color(spec.theme.barBgColor).fill();
			boolean includeIndis = indiWidth != 0;
			if (includeIndis || includeBack) {
				canvas.pushState();
				canvas.composer(clear);
				int w = spec.metrics.barGap;
				int h = spec.metrics.barHeight - 1;
				IntOps intOps = canvas.intOps();
				if (includeBack) {
					int x = buttonX();
					intOps
						.fillRect(IntRect.rectangle(x, 0, w, h + 1))
						.plotPixel(x - 1, h)
						.plotPixel(x + w, h);
				}
				if (includeIndis) {
					int x = separatorX();
					intOps
						.fillRect(IntRect.rectangle(x, 0, w, h + 1))
						.plotPixel(x - 1, h)
						.plotPixel(x + w, h);
				}
				canvas.popState();
			}
			maskDirty = false;
		}
		if (buttDirty) {
			// dirty mask is the only thing that can force button redraw atm
			//TODO needs a theme value
			Frame frame = focused == buttonInd ? buttonFrameFocused : buttonFrame;
			canvas.color(spec.theme.barBgColor).intOps().fillRect(buttonBounds).canvas().color(Argb.WHITE).intOps().fillFrame(frame, buttonBounds.minimumCoords());
			buttDirty = false;
		}
		if (textDirty) {
			int x;
			if (includeBack) {
				x = buttonX() + spec.metrics.barGap + spec.metrics.barButtonGap;
			} else {
				x = 0; /* context.sideMargin - not necessary for black bar*/;
			}
			int limit = separatorX() - spec.metrics.sideMargin;
			canvas.color(spec.theme.barBgColor).intOps().fillRect(IntRect.bounded(x, 0, limit, spec.metrics.barHeight));
			boolean hasBoldText = !boldText.isEmpty();
			boolean hasPlainText = !plainText.isEmpty();
			if (hasBoldText || hasPlainText) {
				StyledText text = new StyledText();
				if (hasBoldText) text.appendStyledText(boldStyle, boldText);
				if (hasPlainText) text.appendText(plainText);

				IntTextOps ops = canvas.intOps().newText(spec.typeface);
				TypefaceMetrics typeMetrics = spec.typeMetrics;
				int ey = spec.metrics.barHeight - spec.metrics.barBaselineGap;
				int ex = x;
				//TODO small loss of fidelity here, bold overflow would have given a bold ellipsis
				int count = typeMetrics.accommodatedCharCount(text, limit - x, spec.ellipsisWidth(TextStyle.regular()));
				boolean renderEllipsis = count < text.length();
				if (renderEllipsis) {
					text.truncateText(count);
					text.appendText(spec.theme.ellipsisString);
				}
				canvas.color(spec.theme.barTextColor);
				ops.moveTo(ex, ey).renderText(text);
			}
			textDirty = false;
		}
		if (indiDirty) {
			int count = indicators.size();
			int maxX = spec.bounds.width(); /*- context.sideMargin  - not necessary for black bar */;
			int maxY = spec.metrics.barHeight - spec.metrics.barIndicatorGap;
			for (int i = 0; i < count; i++) {
				Indicator indicator = indicators.get(i);
				if (!indiDirty && !indicator.needsRender()) continue; // nothing to do
				IntDimensions indDim = indicator.dimensions();
				int minX = maxX - indDim.width;
				int minY = maxY - indDim.height;
				State canvasState = canvas.recordState();
				IntRect bounds = IntRect.bounded(minX, minY, maxX, maxY);
				canvas.intOps().clipRectAndTranslate(bounds);
				if (indicator.needsClearBeforeRender()) canvas.color(spec.theme.barBgColor).fill();
				canvas.color(spec.theme.indicatorColor);
				Object indState = indicator.render(canvas);
				canvasState.restoreStrictly();
				indicator.recordRender(indState);
				maxX = minX - spec.metrics.indicatorGap;
			}
			indiDirty = false;
		}
	}

	@Override
	Optional<Focusing> focusing() {
		return Optional.ofNullable(focusing);
	}

	@Override
	Optional<Eventing> eventing() {
		return Optional.ofNullable(eventing);
	}

	@Override
	Optional<Pointing> pointing() {
		return Optional.ofNullable(pointing);
	}

	// package scoped methods

	void addIndicator(Indicator indicator) {
		assert indicator != null;
		if (indicators.contains(indicator)) return;
		indicators.add(indicator);
		indicatorsChanged();
	}

	void removeIndicator(Indicator indicator) {
		assert indicator != null;
		if (!indicators.contains(indicator)) return;
		indicators.remove(indicator);
		indicatorsChanged();
	}

	// private helper methods

	private void indicatorsChanged() {
		//TODO optimize
		indicators.sort(Comparator.comparing(i -> i.priority()));
		long period = indicators.stream().mapToLong(i -> i.period()).filter(p -> p > 0).reduce(Long.MAX_VALUE, Long::min);
		if (period < Long.MAX_VALUE) situation.requestRedrawPeriodicallyDelayed(period, period);
		indiDirty = true;
	}

	private void updateDirties() {
		if (situation.dirty()) {
			contDirty = true;
			indiDirty = true;
			maskDirty = true;
			textDirty = true;
			return;
		}

		contDirty = false;
		for (Indicator indicator : indicators) {
			if (indicator.needsRender()) {
				contDirty = true;
				break;
			}
		}
		if (indiDirty) {
			// may invalidate mask
			int newIndiWidth = indicators.stream().mapToInt(i -> i.dimensions().width).sum();
			if (newIndiWidth != indiWidth) {
				indiWidth = newIndiWidth;
				maskDirty = true;
			}
		}
		if (maskDirty) {
			// assume text & indicators needs redrawing
			buttDirty = includeBack;
			textDirty = true;
			indiDirty = true;
		}
	}

	// TODO could precompute
	private int buttonX() {
		return spec.metrics.badgeSize + spec.metrics.barButtonGap + spec.metrics.sideMargin;
	}

	private int separatorX() {
		return spec.bounds.width() - indiWidth - spec.metrics.indicatorGap * (indicators.size() - 1) - 2 * spec.metrics.sideMargin;
	}

}
