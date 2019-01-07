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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Consumer;

import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.studio.ColorPlane.ColorFrame;
import com.superdashi.gosper.studio.Typeface.TextRenderer;
import com.tomgibara.geom.awt.AWTUtil;
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

class LocalCanvas implements Canvas {

	private final ImageSurface surface;
	private ImageIntOps intOps;
	private ImageFloatOps floatOps;
	private Graphics2D g;
	private Deque<GraphicsState> stack;

	LocalCanvas(ImageSurface surface) {
		this.surface = surface;
		g = surface.createGraphics();
	}

	@Override
	public IntDimensions dimensions() {
		return surface.dimensions();
	}

	@Override
	public IntOps intOps() {
		return intOps == null ? intOps = new ImageIntOps() : intOps;
	}

	@Override
	public FloatOps floatOps() {
		return floatOps == null ? floatOps = new ImageFloatOps() : floatOps;
	}

	@Override
	public Canvas erase() {
		//TODO check what happens for opaque image
		Composite tmp = g.getComposite();
		g.setComposite(AlphaComposite.Clear);
		g.fillRect(0, 0, surface.dimensions().width, surface.dimensions().height);
		g.setComposite(tmp);
		return this;
	}

	@Override
	public Canvas color(int argb) {
		checkNotDestroyed();
		g.setColor(new Color(argb, true));
		return this;
	}

	@Override
	public Canvas shader(Shader shader) {
		if (shader == null) throw new IllegalArgumentException("null shader");
		checkNotDestroyed();
		g.setPaint(shader.toPaint());
		return this;
	}

	@Override
	public Canvas composer(Composer composer) {
		if (composer == null) throw new IllegalArgumentException("null composer");
		checkNotDestroyed();
		//g.setComposite(composer.toComposite());
		composer.applyTo(g);
		return this;
	}

	@Override
	public Canvas fill() {
		checkNotDestroyed();
		g.fillRect(0, 0, surface.dimensions().width, surface.dimensions().height);
		return this;
	}

	@Override
	public Canvas drawFrame(Frame frame) {
		if (frame == null) throw new IllegalArgumentException("null frame");
		checkNotDestroyed();
		drawFrameImpl(frame, IntCoords.ORIGIN);
		return this;
	}

	@Override
	public Canvas fillFrame(Frame frame) {
		if (frame == null) throw new IllegalArgumentException("null frame");
		checkNotDestroyed();
		fillFrameImpl(frame, IntCoords.ORIGIN);
		return this;
	}

	@Override
	public State recordState() {
		checkNotDestroyed();
		if (stack == null) stack = new ArrayDeque<>();
		GraphicsState state = new GraphicsState(g, false);
		stack.addFirst(state);
		return state;
	}

	@Override
	public Canvas pushState() {
		checkNotDestroyed();
		if (stack == null) stack = new ArrayDeque<>();
		GraphicsState state = new GraphicsState(g, true);
		stack.addFirst(state);
		return this;
	}

	@Override
	public Canvas popState() {
		checkNotDestroyed();
		if (stack != null) {
			GraphicsState first = stack.peekFirst();
			if (first != null && first.poppable) {
				stack.removeFirst();
				first.applyTo(g);
			}
		}
		return this;
	}

	@Override
	public boolean popAllStates() {
		checkNotDestroyed();
		if (stack == null) return false;
		GraphicsState last = null;
		while (true) {
			GraphicsState state = stack.peekFirst();
			if (state == null || !state.poppable) break;
			stack.removeFirst();
			last = state;
		}
		if (last == null) return false;
		last.applyTo(g);
		return true;
	}

	@Override
	public void destroy() {
		if (g != null) {
			g.dispose();
			g = null;
		}
	}

	@Override
	public boolean destroyed() {
		return g == null;
	}

	// package scoped methods

	void applyFont(Font font) {
		checkNotDestroyed();
		g.setFont(font);
	}

	FontMetrics fontMetrics(Font font) {
		checkNotDestroyed();
		return g.getFontMetrics(font);
	}

	void doGraphics(Consumer<Graphics2D> op) {
		checkNotDestroyed();
		op.accept(g);
	}

	// private methods

	private void checkNotDestroyed() {
		if (g == null) throw new IllegalStateException("closed");
	}

	private void drawFrameImpl(Frame frame, IntCoords coords) {
		int x = coords.x;
		int y = coords.y;
		if (frame instanceof ImageSurface) {
			ImageSurface is = (ImageSurface) frame;
			g.drawImage(is.image, x, y, null);
			return;
		}
		if (frame instanceof ImageMask) {
//			ImageMask im = (ImageMask) frame;
//			g.drawImage(im.image, x, y, null);
			//TODO what does drawing a mask mean?
			return;
		}
		if (frame instanceof ColorFrame) {
			ColorFrame cf = (ColorFrame) frame;
			Paint tmp = g.getPaint();
			g.setPaint(cf.plane.asShader().toPaint());
			g.fillRect(x, y, cf.dimensions.width, cf.dimensions.height);
			g.setPaint(tmp);
			return;
		}
		if (frame instanceof PlaneFrame) {
			PlaneFrame<?> pf = (PlaneFrame<?>) frame;
			Paint tmp = g.getPaint();
			IntRect bounds = pf.bounds;
			g.translate(-bounds.minX, -bounds.minY);
			g.setPaint(pf.plane.asShader().toPaint());
			g.fillRect(bounds.minX, bounds.minY, bounds.width(), bounds.height());
			g.setPaint(tmp);
			g.translate(bounds.minX, bounds.minY);
			return;
		}
		if (frame instanceof ClearFrame) {
			return; // no-op
		}
		if (frame instanceof EmptyMask) {
			return; // no-op
		}
		g.drawImage(frame.toImage(), x, y, null);
	}

	private void fillFrameImpl(Frame frame, IntCoords coords) {
		int x = coords.x;
		int y = coords.y;
		if (frame instanceof ImageSurface) {
			ImageSurface is = (ImageSurface) frame;
			fillImage(is.image, x, y, false);
			return;
		}
		if (frame instanceof ImageMask) {
			ImageMask im = (ImageMask) frame;
			fillImage(im.image, x, y, true);
			return;
		}
		if (frame instanceof EmptyMask) {
			return; // nothing to do
		}
		if (frame instanceof EntireMask) {
			IntDimensions dimensions = frame.dimensions();
			g.fillRect(x, y, dimensions.width, dimensions.height);
			return;
		}
		if (frame instanceof ColorFrame) {
			ColorFrame cf = (ColorFrame) frame;
			Paint op = g.getPaint();
			//TODO should optimize this
			g.setPaint(CombinedPaint.combine(cf.plane.asShader().toPaint(), op));
			g.translate(x, y);
			g.fillRect(0, 0, cf.dimensions.width, cf.dimensions.height);
			g.translate(-x, -y);
			g.setPaint(op);
			return;
		}
		if (frame instanceof PlaneFrame) {
			PlaneFrame<?> pf = (PlaneFrame<?>) frame;
			Paint op = g.getPaint();
			Paint pp = pf.plane.asShader().toPaint();
			IntRect bounds = pf.bounds;
			g.translate(-bounds.minX, -bounds.minY);
			g.setPaint(CombinedPaint.combine(pp, op));
			g.fillRect(bounds.minX, bounds.minY, bounds.width(), bounds.height());
			g.setPaint(op);
			g.translate(bounds.minX, bounds.minY);
			return;
		}
		if (frame instanceof ClearFrame) {
			return; // no-op
		}
		if (frame instanceof EmptyMask) {
			return; // no-op
		}
		g.drawImage(frame.toImage(), x, y, null);
	}

	private void fillImage(BufferedImage image, int x, int y, boolean mask) {
		int w = image.getWidth();
		int h = image.getHeight();
		//TODO could cache on surfaces that are truly immutable
		TexturePaint tp = new TexturePaint(image, new Rectangle(w, h));
		Paint op = g.getPaint();
		g.translate(x, y);
		g.setPaint( mask ? CombinedPaint.combineMask(tp, op) : CombinedPaint.combine(tp, op) );
		g.fillRect(0, 0, w, h);
		g.translate(-x, -y);
		g.setPaint(op);
	}

	// inner classes

	private final class ImageIntOps implements IntOps {

		@Override
		public LocalCanvas canvas() {
			return LocalCanvas.this;
		}

		@Override
		public IntOps translate(int dx, int dy) {
			checkNotDestroyed();
			g.translate(dx, dy);
			return this;
		}

		@Override
		public IntOps translate(IntVector vector) {
			checkNotDestroyed();
			g.translate(vector.x, vector.y);
			return this;
		}

		@Override
		public IntOps plotPixel(int x, int y) {
			checkNotDestroyed();
			g.fillRect(x, y, 1, 1);
			return this;
		}

		@Override
		public IntOps plotPixel(IntCoords coords) {
			if (coords == null) throw new IllegalArgumentException("null coords");
			return plotPixel(coords.x, coords.y);
		}

		@Override
		public IntOps strokeLine(IntCoords start, IntCoords finish) {
			if (start == null) throw new IllegalArgumentException("null start");
			if (finish == null) throw new IllegalArgumentException("null finish");
			return strokeLine(start.x, start.y, finish.x, finish.y);
		}

		@Override
		public IntOps strokeLine(int x1, int y1, int x2, int y2) {
			checkNotDestroyed();
			g.drawLine(x1, y1, x2, y2);
			return this;
		}

		@Override
		public IntOps strokeRect(IntRect rect) {
			checkNotDestroyed();
			g.drawRect(rect.minX, rect.minY, rect.width() - 1, rect.height() - 1);
			return this;
		}

		@Override
		public IntOps strokeEllipse(IntRect rect) {
			checkNotDestroyed();
			g.drawOval(rect.minX, rect.minY, rect.width() - 1, rect.height() - 1);
			return this;
		}

		@Override
		public IntOps fillRect(IntRect rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			checkNotDestroyed();
			g.fillRect(rect.minX, rect.minY, rect.width(), rect.height());
			return this;
		}

		@Override
		public IntOps fillEllipse(IntRect rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			checkNotDestroyed();
			g.fillOval(rect.minX, rect.minY, rect.width(), rect.height());
			return this;
		}

		@Override
		public IntOps drawFrame(Frame frame, IntCoords coords) {
			if (frame == null) throw new IllegalArgumentException("null frame");
			if (coords == null) throw new IllegalArgumentException("null coords");
			checkNotDestroyed();
			drawFrameImpl(frame, coords);
			return this;
		}

		@Override
		public IntOps fillFrame(Frame frame, IntCoords coords) {
			if (frame == null) throw new IllegalArgumentException("null frame");
			if (coords == null) throw new IllegalArgumentException("null coords");
			checkNotDestroyed();
			fillFrameImpl(frame, coords);
			return this;
		}

		@Override
		public IntOps clipRect(IntRect rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			checkNotDestroyed();
			g.clipRect(rect.minX, rect.minY, rect.width(), rect.height());
			return this;
		}

		@Override
		public IntOps clipRectAndTranslate(IntRect rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			checkNotDestroyed();
			g.clipRect(rect.minX, rect.minY, rect.width(), rect.height());
			g.translate(rect.minX, rect.minY);
			return this;
		}

		@Override
		public IntTextOps newText(Typeface typeface) {
			if (typeface == null) throw new IllegalArgumentException("null typeface");
			return new ImageIntTextOps(typeface);
		}

	}

	private final class ImageFloatOps implements FloatOps {

		@Override
		public LocalCanvas canvas() {
			return LocalCanvas.this;
		}

		@Override
		public FloatOps translate(float x, float y) {
			checkNotDestroyed();
			g.translate(x, y);
			return this;
		}

		@Override
		public FloatOps translate(Vector vector) {
			if (vector == null) throw new IllegalArgumentException("null vector");
			checkNotDestroyed();
			g.translate(vector.x, vector.y);
			return this;
		}

		@Override
		public FloatOps transform(Transform transform) {
			if (transform == null) throw new IllegalArgumentException("null transform");
			checkNotDestroyed();
			g.transform(AWTUtil.toAffineTransform(transform));
			return this;
		}

		@Override
		public FloatOps fillRect(Rect rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			checkNotDestroyed();
			g.fill(AWTUtil.toRectangle(rect));
			return this;
		}

		@Override
		public FloatOps fillEllipse(Ellipse ellipse) {
			if (ellipse == null) throw new IllegalArgumentException("null ellipse");
			checkNotDestroyed();
			g.fill(AWTUtil.toEllipse(ellipse));
			return this;
		}

		@Override
		public FloatOps fillShape(Shape shape) {
			if (shape == null) throw new IllegalArgumentException("null shape");
			checkNotDestroyed();
			g.fill(AWTUtil.toShape2D(shape));
			return this;
		}

		@Override
		public FloatTextOps newText(Typeface typeface) {
			if (typeface == null) throw new IllegalArgumentException("null typeface");
			return new ImageFloatTextOps(typeface);
		}

	}

	private final class ImageIntTextOps implements IntTextOps {

		private final TextRenderer renderer;

		private int x = 0;
		private int y = 0;

		ImageIntTextOps(Typeface typeface) {
			renderer = typeface.renderer(LocalCanvas.this);
		}

		@Override
		public IntTextOps moveTo(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		@Override
		public IntTextOps moveTo(IntCoords coords) {
			if (coords == null) throw new IllegalArgumentException("null coords");
			x = coords.x;
			y = coords.y;
			return this;
		}

		@Override
		public IntTextOps moveBy(int dx, int dy) {
			x += dx;
			y += dy;
			return this;
		}

		@Override
		public IntTextOps moveBy(IntVector vector) {
			x += vector.x;
			y += vector.y;
			return this;
		}

		@Override
		public LocalCanvas canvas() {
			return LocalCanvas.this;
		}

		@Override
		public Typeface typeface() {
			return renderer.typeface();
		}

		@Override
		public ImageIntTextOps renderChar(int c) {
			renderer.renderChar(x, y, c);
			return this;
		}

		@Override
		public ImageIntTextOps renderChar(TextStyle style, int c) {
			if (style == null) throw new IllegalArgumentException("null style");
			renderer.renderChar(x, y, style, c);
			return this;
		}

		@Override
		public ImageIntTextOps renderString(String str) {
			if (str == null) throw new IllegalArgumentException("null str");
			if (str.isEmpty()) return this;
			renderer.renderString(x, y, str);
			return this;
		}

		@Override
		public ImageIntTextOps renderString(TextStyle style, String str) {
			if (style == null) throw new IllegalArgumentException("null style");
			if (str == null) throw new IllegalArgumentException("null str");
			if (str.isEmpty()) return this;
			renderer.renderString(x, y, style, str);
			return this;
		}

		@Override
		public IntTextOps renderText(StyledText text) {
			if (text == null) throw new IllegalArgumentException("null text");
			if (text.isEmpty()) return this;
			renderer.renderText(x, y, text);
			return this;
		}

	}

	private final class ImageFloatTextOps implements FloatTextOps {

		private final TextRenderer renderer;

		private float x = 0;
		private float y = 0;

		ImageFloatTextOps(Typeface typeface) {
			renderer = typeface.renderer(LocalCanvas.this);
		}

		@Override
		public ImageFloatTextOps moveTo(float x, float y) {
			this.x = x;
			this.y = y;
			return this;
		}

		@Override
		public ImageFloatTextOps moveTo(Point point) {
			if (point == null) throw new IllegalArgumentException("null point");
			x = point.x;
			y = point.y;
			return this;
		}

		@Override
		public FloatTextOps moveBy(float dx, float dy) {
			x += dx;
			y += dy;
			return this;
		}

		@Override
		public FloatTextOps moveBy(Vector vector) {
			x += vector.x;
			y += vector.y;
			return this;
		}

		@Override
		public LocalCanvas canvas() {
			return LocalCanvas.this;
		}

		@Override
		public Typeface typeface() {
			return renderer.typeface();
		}

		@Override
		public ImageFloatTextOps renderChar(int c) {
			renderer.renderChar(x, y, c);
			return this;
		}

		@Override
		public ImageFloatTextOps renderChar(TextStyle style, int c) {
			if (style == null) throw new IllegalArgumentException("null style");
			renderer.renderChar(x, y, style, c);
			return this;
		}

		@Override
		public ImageFloatTextOps renderString(String str) {
			if (str == null) throw new IllegalArgumentException("null str");
			if (str.isEmpty()) return this;
			renderer.renderString(x, y, str);
			return this;
		}

		@Override
		public ImageFloatTextOps renderString(TextStyle style, String str) {
			if (style == null) throw new IllegalArgumentException("null style");
			if (str == null) throw new IllegalArgumentException("null str");
			if (str.isEmpty()) return this;
			renderer.renderString(x, y, style, str);
			return this;
		}

		@Override
		public FloatTextOps renderText(StyledText text) {
			if (text == null) throw new IllegalArgumentException("null text");
			if (text.isEmpty()) return this;
			renderer.renderText(x, y, text);
			return this;
		}

	}

	private class GraphicsState implements State {

		private final boolean poppable;
		private final Paint paint;
		private final AffineTransform transform;
		private final Composite composite;
		private final java.awt.Shape clip;
		private boolean invalid = false;

		GraphicsState(Graphics2D g, boolean poppable) {
			this.poppable = poppable;
			paint = g.getPaint();
			transform = g.getTransform();
			composite = g.getComposite();
			clip = g.getClip();
		}

		@Override
		public boolean invalid() {
			return invalid;
		}

		@Override
		public void restore() {
			if (invalid) throw new IllegalStateException("invalid");
			checkNotDestroyed();
			GraphicsState state;
			do {
				state = stack.removeFirst();
				state.applyTo(g);
			} while (state != this);
		}

		@Override
		public void restoreStrictly() {
			if (invalid) throw new IllegalStateException("invalid");
			checkNotDestroyed();
			if (stack.peekFirst() != this) throw new IllegalStateException("unrestored states present");
			stack.removeFirst().applyTo(g);
		}

		void applyTo(Graphics2D g) {
			g.setComposite(composite);
			g.setTransform(transform);
			g.setClip(clip);
			g.setPaint(paint);
			invalid = true;
		}

	}
}
