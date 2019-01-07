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
package com.superdashi.gosper.util;

import static java.lang.Character.isWhitespace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.tomgibara.bits.Bits;
import com.superdashi.gosper.color.Palette.LogicalColor;
import com.tomgibara.fundament.Mutability;

public class TextCanvas implements Mutability<TextCanvas> {

	private static final int BLANK = 32;
	private static long coord(int x, int y) {
		return (long) x << 32 | (long) y & 0xffffffffL;
	}

	private static int x(long coord) {
		return (int) (coord >> 32);
	}

	private static int y(long coord) {
		return (int) coord;
	}

	private static byte bgfg(LogicalColor bg, LogicalColor fg) {
		return (byte) ((bg.ordinal() << 4) | fg.ordinal());
	}

	private static LogicalColor fg(byte b) {
		return LogicalColor.valueOf(b & 0xf);
	}

	private static LogicalColor bg(byte b) {
		return LogicalColor.valueOf((b & 0xf0) >> 4);
	}

	public static TextCanvas blank(int cols, int rows) {
		if (cols < 0) throw new IllegalArgumentException("negative cols");
		if (rows < 0) throw new IllegalArgumentException("negative rows");
		TextCanvas canvas = new TextCanvas(true, cols, rows);
		Arrays.fill(canvas.codes, BLANK);
		return canvas;
	}

	private final boolean mutable;
	private final int[] codes;
	private final boolean[] bolds;
	private final byte[] bgfgs;
	private final int cols;
	private final int rows;
	private final int x1;
	private final int y1;
	private final int x2;
	private final int y2;

	private TextCanvas(boolean mutable, int cols, int rows) {
		this.mutable = mutable;
		int size = cols * rows;
		codes = new int[size];
		bolds = new boolean[size];
		bgfgs = new byte[size];
		this.cols = cols;
		this.rows = rows;
		x1 = 0;
		y1 = 0;
		x2 = cols;
		y2 = rows;
	}

	private TextCanvas(
			boolean mutable,
			int[] codes,
			boolean[] bolds,
			byte[] colors,
			int cols,
			int rows,
			int x1,
			int y1,
			int x2,
			int y2
			) {
		this.mutable = mutable;
		this.codes = codes;
		this.bolds = bolds;
		this.bgfgs = colors;
		this.cols = cols;
		this.rows = rows;
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	public int width() {
		return x2 - x1;
	}

	public int height() {
		return y2 - y1;
	}

	public boolean isValidCoord(int x, int y) {
		return x >= 0 && x < width() && y >= 0 && y < height();
	}

	public int charAt(int x, int y) {
		return codes[ indexAbs(absCoord(x, y)) ];
	}

	public boolean boldAt(int x, int y) {
		return bolds[ indexAbs(absCoord(x, y)) ];
	}

	public LogicalColor fgColorAt(int x, int y) {
		return fg( bgfgs[ indexAbs(absCoord(x, y)) ] );
	}

	public LogicalColor bgColorAt(int x, int y) {
		return bg( bgfgs[ indexAbs(absCoord(x, y)) ] );
	}

	public IntStream codePoints() {
		return IntStream.range(y1, y2).mapToObj(y -> Arrays.stream(codes, indexAbs(0,y), indexAbs(0,y+1))).flatMapToInt(s -> s);
	}

	public IntStream boldFlags() {
		return IntStream.range(y1, y2).mapToObj(y -> Bits.asStore(bolds).range(indexAbs(0,y), indexAbs(0,y+1))).flatMapToInt(s -> s.asList().stream().mapToInt(b -> b ? 0 : 1));
	}


	public void put(int x, int y, int code, boolean bold, LogicalColor bg, LogicalColor fg) {
		checkMutable();
		int i = indexAbs(absCoord(x, y));
		codes[i] = code;
		bolds[i] = bold;
		bgfgs[i] = bgfg(bg, fg);
	}

	public void putChar(int x, int y, int code) {
		checkMutable();
		codes[indexAbs(absCoord(x, y))] = code;
	}

	public void putBold(int x, int y, boolean bold) {
		checkMutable();
		bolds[indexAbs(absCoord(x, y))] = bold;
	}

	public void putColors(int x, int y, LogicalColor bg, LogicalColor fg) {
		checkMutable();
		bgfgs[indexAbs(absCoord(x, y))] = bgfg(bg, fg);
	}

	public void fill(int code, boolean bold, LogicalColor bg, LogicalColor fg) {
		checkMutable();
		byte bgfg = bgfg(bg, fg);
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				int i = indexAbs(x,y);
				codes[i] = code;
				bolds[i] = bold;
				bgfgs[i] = bgfg;
			}
		}
	}

	public List<String> flow(List<String> text, boolean bold, LogicalColor bg, LogicalColor fg) {
		if (text == null) throw new IllegalArgumentException("null text");
		checkMutable();
		byte bgfg = bgfg(bg, fg);
		int w = width();
		if (w == 0) return text;
		int y = y1;
		int x = x1;
		List<String> list = new ArrayList<>();
		for (String str : text) {
			if (y == y2) {
				list.add(str);
				continue;
			}
			int[] cs = str.codePoints().toArray();
			int len = cs.length;
			int off = 0;
			while (y < y2 && len > off + w) {
				int i;
				int max = off + w;
				for (i = max; i >= off && !isWhitespace(cs[i]); i--);
				if (i < off) i = max;
				int index = indexAbs(x,y);
				Arrays.fill(bolds, index, index + i - off, bold);
				Arrays.fill(bgfgs, index, index + i - off, bgfg);
				for (off = i; off <= len && isWhitespace(cs[off]); off++);
				y++;
			}
			if (y < y2) {
				int index = indexAbs(x,y);
				System.arraycopy(cs, off, codes, index, len - off);
				Arrays.fill(bolds, index, index + len - off, bold);
				Arrays.fill(bgfgs, index, index + len - off, bgfg);
				y++;
			} else {
				list.add( new String(cs, off, len - off) );
			}
		}
		return list;
	}

	public void copyTo(TextCanvas canvas) {
		int width = this.width();
		int height = this.height();
		if (width != canvas.width()) throw new IllegalArgumentException("mismatched width");
		if (height != canvas.height()) throw new IllegalArgumentException("mismatched height");
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int i = indexRel(x, y);
				int j = canvas.indexRel(x, y);
				canvas.codes[j] = this.codes[i];
				canvas.bolds[j] = this.bolds[i];
				canvas.bgfgs[j] = this.bgfgs[i];
			}
		}
	}

	public void putLines(List<String> lines, boolean bold, LogicalColor bg, LogicalColor fg) {
		if (lines == null) throw new IllegalArgumentException("null lines");
		checkMutable();
		byte bgfg = bgfg(bg, fg);
		int index = indexRel(0,0);
		int width = width();
		int height = height();
		for (int y = 0; y < height; y++) {
			if (y < lines.size()) {
				int[] cs = lines.get(y).codePoints().limit(width).toArray();
				System.arraycopy(cs, 0, codes, index, cs.length);
				Arrays.fill(bolds, index, index + cs.length, bold);
				Arrays.fill(bgfgs, index, index + cs.length, bgfg);
				index += cs.length;
			}
			int limit = indexRel(0, y+1);
			Arrays.fill(codes, index, limit, BLANK);
			index = limit;
		}
	}

	public TextCanvas rectView(int x1, int y1, int x2, int y2) {
		checkRows(y1, y2);
		checkCols(x1, x2);
		return view(x1, y1, x2, y2);
	}

	public TextCanvas rowsView(int y1, int y2) {
		checkRows(y1, y2);
		return view(x1, y1, x2, y2);
	}

	public TextCanvas rowView(int y) {
		checkRows(y, y + 1);
		return view(x1, y, x2, y + 1);
	}

	public Stream<TextCanvas> rowViews(int y1, int y2) {
		checkRows(y1, y2);
		return IntStream.range(y1, y2).mapToObj(y -> view(x1, y, x2, y+ 1));
	}

	public Stream<TextCanvas> rowViews() {
		return IntStream.range(y1, y2).mapToObj(y -> view(x1, y, x2, y+ 1));
	}

	// mutability

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public TextCanvas immutableCopy() {
		TextCanvas copy = new TextCanvas(false, width(), height());
		copyTo(copy);
		return copy;
	}

	@Override
	public TextCanvas mutableCopy() {
		TextCanvas copy = new TextCanvas(true, width(), height());
		copyTo(copy);
		return copy;
	}

	@Override
	public TextCanvas immutableView() {
		return new TextCanvas(false, codes, bolds, bgfgs, cols, rows, x1, y1, x2, y2);
	}

	// object methods

	@Override
	public int hashCode() {
		return x1 + 31 * (y1 + 31 * (x2 + 31 * y2)) + Arrays.hashCode(codes) + Arrays.hashCode(bolds) + Arrays.hashCode(bgfgs);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof TextCanvas)) return false;
		TextCanvas that = (TextCanvas) obj;
		int w = this.width();
		int h = this.height();
		if (w != that.width() || h != that.height()) return false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int i = this.indexRel(x, y);
				int j = that.indexRel(x, y);
				if (this.codes[i] != that.codes[j]) return false;
				if (this.bolds[i] != that.bolds[j]) return false;
				if (this.bgfgs[i] != that.bgfgs[j]) return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		//TODO precompute sb size
		StringBuilder sb = new StringBuilder();
		// chars
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				int code = codes[indexAbs(x, y)];
				char c = code >=0 && code < 65536 ? (char) code : '?';
				sb.append(c);
			}
			sb.append('\n');
		}
		sb.append('\n');
		// bolds
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				boolean bold = bolds[indexAbs(x, y)];
				char c = bold ? '*' : ' ';
				sb.append(c);
			}
			sb.append('\n');
		}
		sb.append('\n');
		// bgs
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				byte bgfg = bgfgs[indexAbs(x, y)];
				char c = bg(bgfg).c;
				sb.append(c);
			}
			sb.append('\n');
		}
		sb.append('\n');
		// fgs
		for (int y = y1; y < y2; y++) {
			for (int x = x1; x < x2; x++) {
				byte bgfg = bgfgs[indexAbs(x, y)];
				char c = fg(bgfg).c;
				sb.append(c);
			}
			sb.append('\n');
		}
		return sb.toString();
	}

	// private helper methods

	private TextCanvas view(int x1, int y1, int x2, int y2) {
		return new TextCanvas(mutable, codes, bolds, bgfgs, cols, rows, x1, y1, x2, y2);
	}

	private long absCoord(int x, int y) {
		if (x < 0) throw new IllegalArgumentException("negative x");
		if (y < 0) throw new IllegalArgumentException("negative y");
		x += x1;
		y += y1;
		if (x >= x2) throw new IllegalArgumentException("x too large");
		if (y >= y2) throw new IllegalArgumentException("y too large");
		return coord(x,y);
	}

	private int indexAbs(long coord) {
		return indexAbs(x(coord), y(coord));
	}

	private int indexRel(long coord) {
		return indexAbs(x1 + x(coord), y1 + y(coord));
	}

	private int indexRel(int x, int y) {
		return indexAbs(x1 + x, y1 + y);
	}

	private int indexAbs(int x, int y) {
		return y * cols + x;
	}

//	private int charAtRel(int x, int y) {
//		return codes[indexRel(x, y)];
//	}

	private int charAtAbs(int x, int y) {
		return codes[indexAbs(x, y)];
	}

	private void putCharAbs(int x, int y, int code) {
		codes[indexAbs(x, y)] = code;
	}

//	private void putCharRel(int x, int y, int code) {
//		codes[indexRel(x, y)] = code;
//	}

	private void checkRows(int y1, int y2) {
		if (y1 < 0) throw new IllegalArgumentException("negative y1");
		if (y2 < y1) throw new IllegalArgumentException("y1 > y2");
		if (y2 > height()) throw new IllegalArgumentException("y2 exceeds height");
	}

	private void checkCols(int x1, int x2) {
		if (x1 < 0) throw new IllegalArgumentException("negative x1");
		if (x2 < x1) throw new IllegalArgumentException("x1 > x2");
		if (x2 > width()) throw new IllegalArgumentException("x2 exceeds width");
	}

	private void checkMutable() {
		if (!mutable) throw new IllegalStateException("immutable");
	}
}
