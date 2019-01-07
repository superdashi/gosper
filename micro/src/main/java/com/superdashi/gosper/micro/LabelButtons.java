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
import java.util.Optional;
import java.util.stream.Stream;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.PorterDuff.Rule;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

//TODO must render disabled state
public class LabelButtons extends ActionsComponent {

	// statics

	private static TextStyle textStyle = TextStyle.regular();

	private static int recommendedWidth(VisualSpec spec, Stream<Action> stream) {
		Selector selector = new Selector(spec, false);
		return stream.mapToInt(a -> selector.width() + spec.metrics.sideMargin * 2 + spec.renderedWidthOfString(textStyle, a.label())).max().orElse(0);
	}

	// fields

	private final Eventing eventing = verticalEventing();

	// these are derived from the visual context
	private Selector selector;
	private IntRect buttonRect;
	private int buttonStep;
	private int baseline;

	@Override
	IntDimensions minimumSize() {
		return IntDimensions.horizontal(recommendedWidth());
	}

	@Override
	void place(Place place, int z) {
		super.place(place, z);
		VisualSpec spec = situation.visualSpec();
		buttonRect = IntRect.bounded(0, 1, bounds.width(), 1 + spec.metrics.buttonHeight);
		buttonStep = computeButtonStep();
		//TODO can instances like this be exposed on display?
		selector = new Selector(spec, true);
		baseline = spec.typeMetrics.fontMetrics(textStyle).baseline;
	}

	// component methods

	@Override
	public Composition composition() {
		return Composition.MASK;
	}

	@Override
	void render() {
		RenderData data = new RenderData();
		Canvas canvas = situation.defaultPane().canvas();

		VisualSpec spec = situation.visualSpec();

		//TODO cache these
		Composer clear = new PorterDuff(Rule.CLEAR).asComposer();
		Composer srcOver = new PorterDuff(Rule.SRC_OVER).asComposer();
		Composer srcAtop = new PorterDuff(Rule.SRC_ATOP).asComposer();

		boolean allChanged = situation.dirty() || data.displayCount != data.oldDisplayCount;
		if (allChanged) {
			int displayCount = data.displayCount;
			IntRect rect = buttonRect;
			canvas.erase();
			for (int i = 0; i < displayCount; i++) {
				canvas.color(Argb.WHITE)
					.intOps().fillRect(rect)
					.canvas().composer(clear)
					.intOps().plotPixel(rect.maxX - 1, rect.minY).plotPixel(rect.maxX - 1, rect.maxY - 1)
					.canvas().composer(srcOver);
				rect = rect.translatedBy(0, buttonStep);
			}
		}

		int selectorHeight = selector.height();
		int selectorWidth = selector.width();
		int labelInset = selectorWidth + spec.metrics.sideMargin;
		int labelWidth = buttonRect.width() - labelInset - spec.metrics.sideMargin; //TODO needs specific constant, or rename?
		boolean compFocused = situation.isFocused();
		canvas.composer(srcAtop);
		int textColor = spec.theme.buttonTextColor;
		int bkgrColor = spec.theme.buttonBgColor;
		data.render(() -> {
			if (allChanged || data.redraw) {
				IntRect rect = buttonRect.translatedBy(0, data.index * buttonStep);
				int labelBaseline = rect.minY + spec.metrics.buttonHeight - spec.metrics.buttonBaselineGap;
				IntRect selectorBounds = IntRect.rectangle(rect.minX, labelBaseline - selectorHeight, selectorWidth, selectorHeight);
				canvas.color(bkgrColor).intOps().fillRect(rect);
				canvas.color(textColor);
				selector.render(canvas, selectorBounds, Selector.State.of(data.enabled, data.active), compFocused);
				String label = data.action.action().label();
				String line = spec.accommodatedString(textStyle, label, labelWidth);
				if (line.length() > 0) {
					canvas.intOps().newText(spec.typeface)
						.moveTo(rect.minX + labelInset, labelBaseline)
						.renderString(textStyle, line);
				}
			}
		});
		canvas.composer(srcOver);
	}

	@Override
	public Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	// super class abstracts

	@Override
	int computeMaxActionCount() {
		return (bounds.height() + situation.visualSpec().metrics.buttonGap) / computeButtonStep();
	}

	@Override
	IntRect[] areas(int displayCount) {
		IntRect[] areas = new IntRect[displayCount];
		int x = bounds.minX;
		int y = bounds.minY;
		for (int i = 0; i < areas.length; i++) {
			areas[i] = buttonRect.translatedBy(x, y);
			y += buttonStep;
		}
		return areas;
	}

	@Override
	IntRect area(int index) {
		return buttonRect.translatedBy(bounds.minX, bounds.minY + index * buttonStep);
	}

	// private helper methods

	private int computeButtonStep() {
		VisualSpec spec = situation.visualSpec();
		return spec.metrics.buttonHeight + spec.metrics.buttonGap;
	}

	private int recommendedWidth() {
		ActionsModel model = model();
		return model.count() == 0 ? 0 : recommendedWidth(situation.visualSpec(), Arrays.stream(model.allPossibleModels()).map(ActionModel::action));
	}

}
