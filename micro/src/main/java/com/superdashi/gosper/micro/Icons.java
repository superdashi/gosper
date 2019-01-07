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
import com.superdashi.gosper.device.EventMask;
import com.superdashi.gosper.micro.Display.Situation;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.IntFontMetrics;
import com.superdashi.gosper.studio.TextStyle;
import com.tomgibara.intgeom.IntRect;

//TODO doesn't render icons
//TODO doesn't handle lack of vertical space
//TODO rename to Pictures
//TODO move label display to a separate component
public class Icons extends ActionsComponent {

	// fields

	private final Eventing eventing = new Eventing() {

		@Override
		public EventMask eventMask() {
			return EventMask.anyKeyDown();
		}

		@Override
		public boolean handleEvent(Event event) {
			switch (event.key) {
			case Event.KEY_LEFT:
				previousAction();
				return true;
			case Event.KEY_RIGHT:
				nextAction();
				return true;
			case Event.KEY_CONFIRM:
				return activate();
			}
			return false;
		}
	};

	private IntRect iconRect;
	private int iconStep;

	private String lastText = "";

	// constructors

	// component methods

	@Override
	void place(Place place, int z) {
		super.place(place, z);
		VisualMetrics metrics = situation.visualSpec().metrics;
		int iconSize = metrics.iconSize;
		int iconBarGap = metrics.iconBarGap;
		int iconBarWidth = metrics.iconBarWidth;
		int offset = 2 * iconBarGap + iconBarWidth;
		int margin = metrics.componentHMargin;
		iconRect = IntRect.bounded(margin, offset, margin + iconSize, offset + iconSize);
		iconStep = computeIconStep();
	}

	@Override
	public Composition composition() {
		return Composition.FILL;
	}

	@Override
	void render() {
		RenderData data = new RenderData();
		Canvas canvas = situation.defaultPane().canvas();

		VisualSpec spec = situation.visualSpec();
		int bgColor = spec.theme.contentBgColor;
		int actColor = spec.theme.actionColor;
		int iconBarGap = spec.metrics.iconBarGap;
		int iconBarWidth = spec.metrics.iconBarWidth;

		boolean allChanged = situation.dirty() || data.displayCount != data.oldDisplayCount;
		if (allChanged) canvas.color(bgColor).fill();
		int topBarY = iconRect.minY - iconBarGap - iconBarWidth;
		int botBarY = iconRect.maxY + iconBarGap;

		data.render(() -> {
			if (allChanged || data.redraw) {
				//IntRect rect = area(data.index);
				IntRect rect = iconRect.translatedBy(data.index * iconStep, 0);
				if (!allChanged) canvas.color(bgColor).intOps().fillRect(rect);
				//TODO need to handle resizing icon
				data.action.itemModel().icon(true).ifPresent(p -> {
					canvas.intOps().drawFrame(p, rect.minimumCoords());
				});
				canvas.color(data.active ? actColor : bgColor);
				canvas.intOps().fillRect( IntRect.bounded(rect.minX,  topBarY, rect.maxX,  topBarY + iconBarGap) );
				canvas.intOps().fillRect( IntRect.bounded(rect.minX, botBarY, rect.maxX, botBarY + iconBarGap) );
			}
		});

		String label = data.activeAction == null ? "" : data.activeAction.action().label();
		TextStyle textStyle = TextStyle.regular();
		if (allChanged || !lastText.equals(label)) {
			int width = bounds.width();
			IntFontMetrics fontMetrics = spec.typeMetrics.fontMetrics(textStyle);
			IntRect textRect = IntRect.rectangle(spec.metrics.sideMargin, botBarY + iconBarWidth + 1, width - spec.metrics.sideMargin, fontMetrics.top + fontMetrics.bottom);
			// clear previous text - could limit to previous size if not all changed
			canvas.color(spec.theme.infoBgColor).intOps().fillRect(textRect);
			String line = spec.accommodatedString(textStyle, label, textRect.width());
			if (line.length() > 0) {
				canvas.color(spec.theme.infoTextColor).intOps().newText(spec.typeface).moveTo(textRect.minX, textRect.minY + fontMetrics.top).renderString(line);
			}
			lastText = label;
		}
	}

	@Override
	public Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	// super class abstracts

	@Override
	int computeMaxActionCount() {
		return (bounds.width() + situation.visualSpec().metrics.iconGap) / computeIconStep();
	}

	@Override
	IntRect[] areas(int displayCount) {
		IntRect[] areas = new IntRect[displayCount];
		int x = bounds.minX;
		int y = bounds.minY;
		for (int i = 0; i < areas.length; i++) {
			areas[i] = iconRect.translatedBy(x, y);
			x += iconStep;
		}
		return areas;
	}

	@Override
	IntRect area(int index) {
		return iconRect.translatedBy(bounds.minX + index * iconStep, bounds.minY);
	}

	// private helper methods

	private int computeIconStep() {
		VisualSpec spec = situation.visualSpec();
		return spec.metrics.iconSize + spec.metrics.iconGap;
	}

}
