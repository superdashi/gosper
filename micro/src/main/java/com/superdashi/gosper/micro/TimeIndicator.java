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

import java.time.Clock;
import java.time.LocalTime;
import java.time.temporal.ChronoField;

import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.tomgibara.intgeom.IntDimensions;

public final class TimeIndicator extends Indicator {

	private static final TextStyle textStyle = TextStyle.regular();

	private final Typeface typeface;
	private final IntDimensions dimensions;
	private final int baseline;
	private final Clock clock;
	private LocalTime previous = null;

	//TODO need to eliminate Clock from public facing API
	TimeIndicator(VisualSpec spec, Clock clock) {
		this.clock = clock;
		VisualMetrics metrics = spec.metrics;
		int width = spec.renderedWidthOfString(textStyle, "00:00"); // assumed to be widest string
		int height = metrics.barHeight - metrics.barIndicatorGap;
		dimensions = IntDimensions.of(width, height);
		typeface = spec.typeface;
		baseline = metrics.barHeight - metrics.barBaselineGap;
	}

	// time indicator methods

	Clock clock() {
		return clock;
	}

	// indicator methods

	@Override
	IntDimensions dimensions() {
		return dimensions;
	}

	@Override
	int priority() {
		return 0;
	}

	@Override
	//TODO need a way to fix update when clock time changes
	long period() {
		return 60000L;
	}

	@Override
	boolean needsRender() {
		return previous == null || !previous.equals(LocalTime.now(clock));
	}

	@Override
	void recordRender(Object state) {
		this.previous = (LocalTime) state;
	}

	@Override
	boolean needsClearBeforeRender() {
		return true;
	}

	@Override
	Object render(Canvas canvas) {
		LocalTime time = LocalTime.now(clock);
		int h = time.get(ChronoField.HOUR_OF_DAY);
		int m = time.get(ChronoField.MINUTE_OF_HOUR);
		String text = String.format("%02d:%02d",h,m);
		canvas.intOps().newText(typeface).moveTo(0, baseline).renderString(textStyle, text);
		return time;
	}

}
