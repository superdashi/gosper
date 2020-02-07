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
package com.superdashi.gosper.studio;

import java.util.Optional;

import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.tomgibara.geom.core.Point;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.core.Vector;
import com.tomgibara.geom.curve.Ellipse;
import com.tomgibara.geom.shape.Shape;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

public interface Canvas extends Destroyable {

	IntDimensions dimensions();

	IntOps intOps();

	FloatOps floatOps();

	// not subject to canvas state - zeros surface
	Canvas erase();

	// convenience method for setting shader
	Canvas color(int argb);

	Canvas shader(Shader shader);

	Canvas composer(Composer composer);

	Canvas fill();

	Canvas drawFrame(Frame frame);

	Canvas fillFrame(Frame frame);

	State recordState();

	Canvas pushState();

	Canvas popState();

	// pops all poppable states, returns true if any were popped
	boolean popAllStates();

	interface IntOps {

		Canvas canvas();

		IntOps translate(IntVector vector);

		IntOps translate(int dx, int dy);

		IntOps plotPixel(IntCoords coords);

		IntOps plotPixel(int x, int y);

		IntOps strokeLine(IntCoords start, IntCoords finish);

		IntOps strokeLine(int x1, int y1, int x2, int y2);

		IntOps strokeRect(IntRect rect);

		IntOps strokeEllipse(IntRect rect);

		IntOps fillRect(IntRect rect);

		IntOps fillEllipse(IntRect bounds);

		IntOps drawFrame(Frame frame, IntCoords coords);

		IntOps fillFrame(Frame frame, IntCoords coords);

		IntOps clipRect(IntRect rect);

		IntOps clipRectAndTranslate(IntRect rect);

		IntTextOps newText(Typeface typeface);

	}

	interface FloatOps {

		Canvas canvas();

		FloatOps translate(Vector vector);

		FloatOps translate(float x, float y);

		FloatOps transform(Transform transform);

		FloatOps fillRect(Rect rect);

		FloatOps fillEllipse(Ellipse ellipse);

		FloatOps fillShape(Shape shape);

		FloatTextOps newText(Typeface typeface);

	}

	interface TextOps {

		Canvas canvas();

		Typeface typeface();

		TextOps renderChar(int c);

		TextOps renderChar(TextStyle style, int c);

		TextOps renderString(String str);

		//TODO probably replace with styled string
		TextOps renderString(TextStyle style, String str);

		TextOps renderText(Style style, StyledText text);

	}

	interface IntTextOps extends TextOps {

		IntTextOps moveTo(IntCoords coords);

		IntTextOps moveTo(int x, int y);

		IntTextOps moveBy(IntVector vector);

		IntTextOps moveBy(int dx, int dy);

		@Override
		IntTextOps renderChar(int c);

		@Override
		IntTextOps renderChar(TextStyle style, int c);

		@Override
		IntTextOps renderString(String str);

		@Override
		IntTextOps renderString(TextStyle style, String str);

		@Override
		IntTextOps renderText(Style style, StyledText text);

	}

	interface FloatTextOps extends TextOps {

		FloatTextOps moveTo(Point point);

		FloatTextOps moveTo(float x, float y);

		FloatTextOps moveBy(Vector vector);

		FloatTextOps moveBy(float dx, float dy);

		@Override
		FloatTextOps renderChar(int c);

		@Override
		FloatTextOps renderChar(TextStyle style, int c);

		@Override
		FloatTextOps renderString(String str);

		@Override
		FloatTextOps renderString(TextStyle style, String str);

		@Override
		FloatTextOps renderText(Style style, StyledText text);

		//TODO no proper float support at present
		// int accommodatedCharCount(TextStyle style, String str, float width, int ellipsisWidth);

	}

	interface State {

		boolean invalid();

		void restore();

		void restoreStrictly();
	}
}
