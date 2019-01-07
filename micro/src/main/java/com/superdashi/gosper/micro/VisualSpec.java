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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.studio.ClearPlane;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.TypefaceMetrics;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.Streams;

//TODO margins should be assigned to computed bounds, not included by components
//TODO maintain a MRU cache of these?
//TODO consider giving a logger
public class VisualSpec {

	private static final Frame BAD_FRAME = ClearPlane.instance().frame(IntRect.UNIT_SQUARE);
	private static final int ACTIVE_FRAME_COUNT = 4;
	//TODO use collect
	private static final Map<String, Frame> resourceCache = new HashMap<>();

	//TODO should be package scoped
	public static VisualSpec create(VisualQualifier qualifier, boolean opaque, VisualTheme theme, VisualMetrics metrics) {
		if (qualifier == null) throw new IllegalArgumentException("null qualifier");
		if (!qualifier.isFullySpecified()) throw new IllegalArgumentException("qualifier not fully specified");
		if (theme == null) throw new IllegalArgumentException("null theme");
		if (metrics == null) throw new IllegalArgumentException("null metrics");
		return new VisualSpec(qualifier, opaque, theme, metrics);
	}

	public final VisualQualifier qualifier;
	public final boolean opaque;
	public final VisualTheme theme;
	public final VisualMetrics metrics;

	// computed
	public final Background background;
	public final IntRect bounds;
	public final VisualStyles styles;
	public final Typeface typeface;
	public final TypefaceMetrics typeMetrics;
	private final int ellipsisWidthRegular;
	private final int ellipsisWidthBold;

	// loaded
	private Frame barBack = null;
	private Frame barBackFocused = null;
	private Frame[] activeFrames = null;
	private IntDimensions activeDimensions = null;
	private Frame toggle2 = null;
	private Frame toggle3 = null;
	private Frame checkbox = null;

	private VisualSpec(VisualQualifier qualifier, boolean opaque, VisualTheme theme, VisualMetrics metrics) {
		this.qualifier = qualifier;
		this.opaque = opaque;
		this.theme = theme;
		this.metrics = metrics;

		// computed fields
		background = theme.background.adaptedFor(this);
		assert background != null;
		bounds = qualifier.dimensions.toRect();
		styles = new VisualStyles(theme).merge( theme.styles.merge(metrics.styles) );
		//TODO hacky assumption
		typeface = qualifier.qualifier.screen == ScreenClass.MICRO ? Typeface.ezo() : Visuals.typeface(theme.typefaceName, metrics.typefaceSize);
		typeMetrics = typeface.metrics();
		ellipsisWidthBold = typeMetrics.intRenderedWidthOfString(TextStyle.bold(), theme.ellipsisString);
		ellipsisWidthRegular = typeMetrics.intRenderedWidthOfString(TextStyle.regular(), theme.ellipsisString);
	}

	public int ellipsisWidth(TextStyle textStyle) {
		return textStyle.bold ? ellipsisWidthBold : ellipsisWidthRegular;
	}

	public String accommodatedString(TextStyle textStyle, String line, int width) {
		int count = typeface.metrics().accommodatedCharCount(textStyle, line, width, ellipsisWidth(textStyle));
		return count == line.length() ? line : line.substring(0, count) + theme.ellipsisString;
	}

	public int renderedWidthOfString(TextStyle textStyle, String line) {
		return typeface.metrics().intRenderedWidthOfString(textStyle, line);
	}

	//TODO not the best place for this, since it doesn't actually depend on these metrics
	public List<String> splitIntoLines(Typeface typeface, TextStyle style, String text, int width, int maxLines) {
		List<String> lines = new ArrayList<>();
		if (maxLines >= 1) {
			while (!text.isEmpty()) {
				if (lines.size() == maxLines) { // quit early - dump remaining text at end of list
					lines.add(text);
					return lines;
				}
				boolean lastLine = lines.size() == maxLines - 1;
				int ellipsisWidth = lastLine ? ellipsisWidth(style) : 0;
				int count = typeface.metrics().accommodatedCharCount(style, text, width, ellipsisWidth);
				boolean complete = count == text.length();
				if (complete) { // quit early, we've used all the text
					lines.add(text);
					return lines;
				}
				int i = text.lastIndexOf(' ', count - 1);
				if (i != -1) { // there's a space to split on
					count = i;
				}
				lines.add(text.substring(0, count));
				while (text.charAt(count) == ' ') count++;
				text = text.substring(count);
			}
		}
		return lines;
	}

	Frame barBack() {
		synchronized (resourceCache) {
			return barBack == null ? barBack = loadFrame("bar_back") : barBack;
		}
	}

	Frame barBackFocused() {
		synchronized (resourceCache) {
			return barBackFocused == null ? barBackFocused = loadFrame("bar_back-focused") : barBackFocused;
		}
	}

	Frame[] activeFrames() {
		synchronized (resourceCache) {
			if (activeFrames == null) {
				if (qualifier.qualifier.screen == ScreenClass.MICRO) {
					// we only have frames for micro
					activeFrames = new Frame[ACTIVE_FRAME_COUNT];
					activeDimensions = IntDimensions.NOTHING;
					for (int i = 0; i < ACTIVE_FRAME_COUNT; i++) {
						String name = "active-" + (i+1) + ".png";
						Frame frame = loadFrameFromResource(name);
						activeDimensions = activeDimensions.growToInclude(frame.dimensions());
						activeFrames[i] = frame;
					}
				} else {
					activeFrames = new Frame[0];
					activeDimensions = IntDimensions.NOTHING;
				}
			}
			return activeFrames;
		}
	}

	Frame toggle2() {
		return toggle2 == null ? toggle2 = loadFrameFromResource("toggle-2.png") : toggle2;
	}

	Frame toggle3() {
		return toggle3 == null ? toggle3 = loadFrameFromResource("toggle-3.png") : toggle3;
	}

	Frame checkbox() {
		return checkbox == null ? checkbox = loadFrameFromResource("checkbox.png") : checkbox;
	}

	IntDimensions activeDimensions() {
		activeFrames(); //TODO hacky: ensure dimensions are computed
		return activeDimensions;
	}

	private Frame loadFrame(String name) {
		Qualifier q = qualifier.qualifier;
		switch (q.screen) {
		case MICRO: name += ".micro"; break;
		case MINI : name += ".mini" ; break;
		default: /* do nothing */
		}
		switch (q.color) {
		case MONO : name += ".mono" ; break;
		default: /* do nothing */
		}
		name += ".png";
		return loadFrameFromResource(name);
	}

	private Frame loadFrameFromResource(String resourceName) {
		Frame frame = resourceCache.get(resourceName);
		if (frame == null) {
			InputStream in = Display.class.getClassLoader().getResourceAsStream(resourceName);
			if (in == null) {
				Debug.logger().error().message("missing resource {}").values(resourceName).log();
				frame = BAD_FRAME;
			} else try {
				frame = Surface.decode(Streams.streamInput(in)).immutable();
			} catch (IOException e) {
				Debug.logger().error().message("failed to load resource {}").values(resourceName).stacktrace(e).log();
				frame = BAD_FRAME;
			}
			resourceCache.put(resourceName, frame);
		}
		return frame;
	}

}
