package com.superdashi.gosper.display;

import static com.tomgibara.intgeom.IntRect.bounded;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.texture.TextureData;
import com.superdashi.gosper.color.Palette.LogicalColor;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.util.TextCanvas;
import com.tomgibara.intgeom.IntRect;

//TODO would be nice to be be able to snapshot as a canvas too
// would require that the char mapping could be reversed
public class Console {

	private static final int OFF_U = 0;
	private static final int OFF_V = 1;
	private static final int OFF_K = 2;
	private static final int OFF_Z = 3; // not currently used

	private static final int NO_COLOR = -1;
	private static final int NO_CHAR = -1;

	private static void checkC(int c) {
		if (!Character.isValidCodePoint(c))
			throw new IllegalArgumentException("invalid codepoint");
	}

	private static void checkS(CharSequence s) {
		if (s == null)
			throw new IllegalArgumentException("null s");
		int length = s.length();
		for (int i = 0; i < length; i++) {
			checkC(s.charAt(i));
		}
	}

	private static boolean isChared(int c) {
		return c >= 0 && c < 0x110000; // highest valid UTF8 codepoint
	}

	private static boolean isColored(int k) {
		return k != NO_COLOR;
	}

	public final int cols;
	public final int rows;
	private final CharMap charMap;
	private final CharLookup lookup; // cache direct reference for efficiency

	private final IntRect all;

	private final ByteBuffer chars;
	private IntRect dirty = null;
	private int tabSize = 4; // TODO make configurable

	public Console(int cols, int rows, CharMap charMap) {
		if (charMap == null)
			throw new IllegalArgumentException("null charMap");
		this.cols = cols;
		this.rows = rows;
		this.charMap = charMap;
		this.lookup = charMap.lookup;
		all = IntRect.bounded(0, 0, cols, rows);
		chars = ByteBuffer.allocate(cols * rows * 4);
		chars.order(ByteOrder.nativeOrder());
	}

	public Console(TextCanvas canvas, CharMap charMap) {
		this(canvas.width(), canvas.height(), charMap);
		createWindow().fillWithCanvas(canvas);
	}

	public CharMap charMap() {
		return charMap;
	}

	public Window createWindow(IntRect r) {
		checkR(r);
		if (r.isDegenerate())
			throw new IllegalArgumentException("degenerate r");
		return new Window(r);
	}

	public Window createWindow() {
		return new Window(all);
	}

	public Window createWindow(int x, int y, TextCanvas canvas) {
		IntRect r = IntRect.rectangle(x, y, canvas.width(), canvas.height());
		checkR(r);
		if (r.isDegenerate())
			throw new IllegalArgumentException("degenerate r");
		Window w = new Window(r);
		w.fillWithCanvasImpl(canvas);
		return w;
	}

	TextureData asTextureData(GLProfile profile) {
		return new TextureData(profile, GL.GL_RGBA, cols, rows, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, false, false, false,
				chars, null);
	}

	Optional<IntRect> dirty() {
		return Optional.ofNullable(dirty);
	}

	void clearDirt() {
		dirty = null;
	}

	private void checkR(IntRect r) {
		if (r == null)
			throw new IllegalArgumentException("null r");
		if (!all.containsRect(r))
			throw new IllegalArgumentException("r out of bounds");
	}

	private int index(int x, int y) {
		return (y * cols + x) * 4;
	}

	private void copy(int from, int to) {
		chars.put(to + OFF_U, chars.get(from + OFF_U));
		chars.put(to + OFF_V, chars.get(from + OFF_V));
		chars.put(to + OFF_K, chars.get(from + OFF_K));
		chars.put(to + OFF_Z, chars.get(from + OFF_Z));
	}

	private void putImpl(int x, int y, int c, int k) {
		int i = index(x, y);
		if (isColored(k)) {
			chars.put(i + OFF_K, (byte) k);
		}
		if (isChared(c)) {
			int uv = lookup.locate(c);
			chars.put(i + OFF_U, lookup.u(uv));
			chars.put(i + OFF_V, lookup.v(uv));
		}
		growDirty(x, y);
	}

	private void putAllImpl(int c, int k) {
		int size = chars.capacity();
		int uv = lookup.locate(c);
		byte u = lookup.u(uv);
		byte v = lookup.v(uv);
		boolean colored = isColored(k);
		boolean chared = isChared(c);
		for (int i = 0; i < size;) {
			if (chared) {
				chars.put(i++, u);
				chars.put(i++, v);
			} else {
				i += 2;
			}
			if (colored) {
				chars.put(i++, (byte) k);
			} else {
				i += 1;
			}
			i++;
		}
		growDirty(all);
	}

	private void putRectImpl(IntRect a, int c, int k) {
		if (a.isDegenerate())
			return;
		if (c == NO_CHAR && k == NO_COLOR)
			return;
		int uv = lookup.locate(c);
		byte u = lookup.u(uv);
		byte v = lookup.v(uv);
		boolean colored = isColored(k);
		boolean chared = isChared(c);
		for (int y = a.minY; y < a.maxY; y++) {
			for (int x = a.minX; x < a.maxX; x++) {
				int i = index(x, y);
				if (colored) {
					chars.put(i + OFF_K, (byte) k);
				}
				if (chared) {
					chars.put(i + OFF_U, u);
					chars.put(i + OFF_V, v);
				}
			}
		}
		growDirty(a);
	}

	private void scrollImpl(IntRect r, int dx, int dy) {
		checkR(r);
		if (r.isDegenerate())
			return; // nothing to do
		IntRect dst = r;
		int off = 0;

		{
			Resolution d = Resolution.ofRect(r);
			int ax = Math.abs(dx);
			int ay = Math.abs(dy);
			if (ax > 0 && ax < d.h) {
				off -= dx;
				dst = dx > 0 ? IntRect.bounded(dst.minX + dx, dst.minY, dst.maxX, dst.maxY) : // scroll
																								// rightwards
						IntRect.bounded(dst.minX, dst.minY, r.maxX + dx, dst.maxY); // scroll
																					// leftwards
			}
			if (ay > 0 && ay < d.v) {
				off -= dy * cols;
				dst = dy > 0 ? IntRect.bounded(dst.minX, dst.minY + dy, dst.maxX, dst.maxY) : // scroll
																								// downwards
						IntRect.bounded(dst.minX, dst.minY, dst.maxX, dst.maxY + dy); // scroll
																						// upwards
			}
		}

		if (off == 0)
			return;
		off *= 4;
		if (off > 0) {
			for (int y = dst.minY; y < dst.maxY; y++) {
				for (int x = dst.minX; x < dst.maxX; x++) {
					int i = index(x, y);
					copy(i + off, i);
				}
			}
		} else {
			for (int y = dst.maxY - 1; y >= dst.minY; y--) {
				for (int x = dst.maxX - 1; x >= dst.minX; x--) {
					int i = index(x, y);
					copy(i + off, i);
				}
			}
		}
		growDirty(r);
	}

	private void growDirty(int x, int y) {
		if (dirty == null) {
			dirty = IntRect.unit(x, y);
		} else {
			dirty = dirty.growToIncludeUnit(x, y);
		}
	}

	private void growDirty(IntRect rect) {
		dirty = dirty == null ? rect : dirty.growToIncludeRect(rect);
	}

	public final class Window {

		private final IntRect rect;
		private final int width;
		private final int height;
		private int cursorX = 0;
		private int cursorY = 0;
		private int color = NO_COLOR;
		private int blank = NO_CHAR;
		private boolean scrolling = false;

		private Window(IntRect rect) {
			if (rect.equals(all)) {
				this.rect = all;
				width = cols;
				height = rows;
			} else {
				this.rect = rect;
				Resolution r = Resolution.ofRect(rect);
				width = r.h;
				height = r.v;
			}
		}

		private Window(Window that) {
			this.rect = that.rect;
			this.width = that.width;
			this.height = that.height;
			this.cursorX = that.cursorX;
			this.cursorY = that.cursorY;
			this.color = that.color;
			this.blank = that.blank;
		}

		public Window copy() {
			return new Window(this);
		}

		public Window createWindow(IntRect rect) {
			rect = rect.translatedBy(rect.minX, rect.minY);
			if (!this.rect.containsRect(rect))
				throw new IllegalArgumentException("rect out of bounds");
			return new Window(rect);
		}

		public Window createWindow() {
			return new Window(rect);
		}

		public void setScrolling(boolean scrolling) {
			this.scrolling = scrolling;
		}

		public Window setCursor(int x, int y) {
			if (x < 0)
				throw new IllegalArgumentException("x too small");
			if (x >= width)
				throw new IllegalArgumentException("x too large");
			if (y < 0)
				throw new IllegalArgumentException("t too small");
			if (y >= height)
				throw new IllegalArgumentException("y too large");
			cursorX = x;
			cursorY = y;
			return this;
		}

		public Window resetCursor() {
			cursorX = 0;
			cursorY = 0;
			return this;
		}

		public Window setColors(LogicalColor fore, LogicalColor back) {
			this.color = (fore.ordinal() << 4) | (back.ordinal());
			return this;
		}

		public Window resetColors() {
			this.color = NO_COLOR;
			return this;
		}

		public Window setBlank(int c) {
			checkC(c);
			blank = c;
			return this;
		}

		public Window resetBlank() {
			blank = NO_CHAR;
			return this;
		}

		public Window fill() {
			if (rect == all) {
				putAllImpl(blank, color);
			} else {
				putRectImpl(rect, blank, color);
			}
			return this;
		}

		public void fillWithCanvas(TextCanvas canvas) {
			if (canvas == null)
				throw new IllegalArgumentException("null canvas");
			if (canvas.width() != width || canvas.height() != height)
				throw new IllegalArgumentException("mismatched dimensions");
			fillWithCanvasImpl(canvas);
		}

		public Window scroll(int dx, int dy) {
			scrollImpl(rect, dx, dy);
			return this;
		}

		// TODO slight inefficiency: writes corners twice - probably rare in
		// practice
		public Window scrollAndFill(int dx, int dy) {
			scrollImpl(rect, dx, dy);
			if (dy > 0)
				putRectImpl(bounded(rect.minX, rect.minY, rect.maxX, rect.minY + dy), blank, color);
			if (dy < 0)
				putRectImpl(bounded(rect.minX, rect.maxY + dy, rect.maxX, rect.maxY), blank, color);
			if (dx > 0)
				putRectImpl(bounded(rect.minX, rect.minY, rect.minX + dx, rect.maxY), blank, color);
			if (dx < 0)
				putRectImpl(bounded(rect.maxX + dx, rect.minY, rect.maxX, rect.maxY), blank, color);
			return this;
		}

		public Window printChar(int c) {
			checkC(c);
			printCharImpl(c);
			return this;
		}

		public Window print(CharSequence s) {
			checkS(s);
			printImpl(s);
			return this;
		}

		public void printNewline() {
			cursorX = 0;
			if (cursorY == height - 1) {
				if (scrolling) {
					scrollAndFill(0, -1);
				} else {
					cursorY = 0;
				}
			} else {
				cursorY++;
			}
		}

		private void fillWithCanvasImpl(TextCanvas canvas) {
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int c = canvas.charAt(x, y);
					LogicalColor bg = canvas.bgColorAt(x, y);
					LogicalColor fg = canvas.fgColorAt(x, y);
					int k = (fg.ordinal() << 4) | (bg.ordinal());
					putImpl(rect.minX + x, rect.minY + y, c, k);
				}
			}
		}

		private void printImpl(CharSequence s) {
			int len = s.length();
			int i = -1;
			int j = 0;
			while (j < len) {
				// examine next character
				char c = s.charAt(j);
				// flush accumulated characters
				if (c < 32 || c == 127) {
					printSafeImpl(s.subSequence(i + 1, j));
					i = j;
				}
				// print char
				printCharImpl(c);
				// advance position
				j++;
			}
			// flush remaining text
			if (i < len)
				printSafeImpl(s.subSequence(i + 1, len));
		}

		private void printSafeImpl(CharSequence s) {
			int len = s.length();
			if (len == 0)
				return;
			int nx = cursorX + len;
			int ny = cursorY + (nx / width);
			nx %= width;
			if (ny >= height) {
				if (scrolling) { // pre-scroll to make room
					int dy = height - ny - 1;
					scroll(0, dy);
					putRectImpl(bounded(rect.minX, rect.maxY + dy, rect.maxX, rect.maxY), blank, NO_COLOR);
					cursorY += dy;
					if (cursorY < 0) {
						int newlen = (height - 1) * width + nx;
						s = s.subSequence(len - newlen, len);
						cursorY = 0;
					}
				} else { // split the string and render bottom before the top
					int bLen = (width - cursorX) + (height - 1 - cursorY) * width;
					putStringImpl(s.subSequence(0, bLen));
					s = s.subSequence(bLen, len);
				}
			}
			putStringImpl(s);
		}

		private void printCharImpl(int c) {
			// deal with control characters
			switch (c) {
			// TODO differentiate BS and DEL?
			case 8: // BS
			case 127: // DEL
				cursorX--;
				if (cursorX == -1) {
					cursorY--;
					cursorX = width - 1;
				}
				break;
			case 10: // LF
				cursorX = 0;
				cursorY++;
				break;
			case 9: // HT
				cursorX += tabSize;
				cursorX -= cursorX % tabSize;
				if (cursorX >= width) {
					cursorX = 0;
					cursorY++;
				}
				break;
			}
			if (scrolling) {
				// deal with scrolling from control characters
				if (cursorY == -1) {
					scroll(0, 1);
					cursorY = 0;
				} else if (cursorY == height) {
					scroll(0, -1);
					// TODO cache rect?
					putRectImpl(bounded(rect.minX, rect.maxY - 1, rect.maxX, rect.maxY), blank, NO_COLOR);
					cursorY = height - 1;
				}
			} else { // wrap cursor position
				if (cursorY == -1) {
					cursorX = width - 1;
					cursorY = height - 1;
				} else if (cursorY == height) {
					cursorX = 0;
					cursorY = 0;
				}
			}
		}

		private void putStringImpl(CharSequence s) {
			int length = s.length();
			if (length == 0)
				return;
			boolean colored = isColored(color);
			int i = index(rect.minX + cursorX, rect.minY + cursorY);
			int skipSize = 4 * (cols - width);
			int nextSkip = skipSize == 0 ? -1 : index(rect.maxX, rect.minY + cursorY);
			for (int j = 0; j < length;) {
				char c = s.charAt(j++);

				int uv = lookup.locate(c);
				chars.put(i++, lookup.u(uv));
				chars.put(i++, lookup.v(uv));
				if (colored)
					chars.put(i, (byte) color);
				i += 2;

				if (i == nextSkip) {
					i += skipSize;
					nextSkip += cols * 4;
					// TODO handle nextSkip wraparound
				}
			}
			int nx = cursorX + length;
			int ny = cursorY + nx / width;
			IntRect d;
			if (nx < width) {
				d = bounded(rect.minX + cursorX, rect.minY + cursorY, rect.minX + nx + 1, rect.minY + cursorY + 1);
			} else {
				if (ny < height) {
					d = bounded(rect.minX, rect.minY + cursorY, rect.maxX, rect.minY + ny + 1);
				} else {
					d = rect;
				}
			}
			growDirty(d);
			cursorX = nx % width;
			cursorY = ny % height;
		}

	}
}
