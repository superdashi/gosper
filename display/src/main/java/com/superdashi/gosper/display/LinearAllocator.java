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
package com.superdashi.gosper.display;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.superdashi.gosper.display.DynamicAtlas.Allocator;

public abstract class LinearAllocator implements Allocator<Void> {

	private static int dist(Span a, Span b) {
		return Math.min(Math.abs(b.x1 - a.x2), Math.abs(b.x2 - a.x1));
	}

	public static LinearAllocator newHorizontal() {
		return new LinearAllocator() {
			@Override int x(Rectangle r) { return r.x; }
			@Override int s(int w, int h) { return w; }
			@Override Rectangle rect(int x, int w, int h) { return new Rectangle(x, 0, w, h); }
		};
	}

	public static LinearAllocator newVertical() {
		return new LinearAllocator() {
			@Override int x(Rectangle r) { return r.y; }
			@Override int s(int w, int h) { return h; }
			@Override Rectangle rect(int x, int w, int h) { return new Rectangle(0, x, w, h); }
		};
	}

	private final List<Span> spans = new ArrayList<>();

	@Override
	public void init(int width, int height) {
		int s = s(width, height);
		spans.add(new Span(0, s));
	}

	@Override
	public Optional<Rectangle> allocate(int w, int h, Void hint) {
		int s = s(w, h);
		int x = take(s);
		if (x == -1) return Optional.empty();
		return Optional.of(rect(x,w,h));
	}

	@Override
	public void release(Rectangle rect) {
		int x = s(rect.x, rect.y);
		int s = s(rect.width, rect.height);
		release(x, s);
	}

	@Override
	public void destroy() {
		spans.clear();
	}

	abstract int x(Rectangle r);

	abstract int s(int w, int h);

	abstract Rectangle rect(int x, int w, int h);

	private int take(int s) {
		switch (spans.size()) {
		case 0: return -1;
		case 1: return takeLast(s);
		default:
			sortForSize(s);
			return takeLast(s);
		}

	}

	private void sortForSize(int s) {
		spans.sort((spn1, spn2) -> {
			int s1 = spn1.size();
			int s2 = spn2.size();
			if (s1 < s) {
				return s2 < s ? 0 : -1;
			} else if (s2 < s) {
				return 1;
			} else {
				int r1 = s1 - s;
				int r2 = s2 - s;
				if (r1 == r2) return 0;
				return r1 < r2 ? 1 : -1;
			}
		});
	}

	private int takeLast(int s) {
		int i = spans.size() - 1;
		Span old = spans.get(i);
		int os = old.size();
		if (s > os) return -1;
		spans.remove(i);
		if (s < os) spans.add(old.rightOf(s));
		return old.x1;
	}

	private void release(int x, int s) {
		Span spn = new Span(x, x + s);
		switch (spans.size()) {
		case 0:
			spans.add(spn);
			break;
		case 1:
			spans.add(spn);
			merge();
			break;
		default:
			sortForSize(spn);
			spans.add(spn);
			if (merge()) merge();
			break;
		}
	}


	private void sortForSize(Span spn) {
		spans.sort((spn1, spn2) -> {
			int d1 = dist(spn, spn1);
			int d2 = dist(spn, spn2);
			if (d1 == 0 && d2 == 0) return 0;
			if (d1 == 0) return 1;
			if (d2 == 0) return -1;
			return 0;
		});

	}

	private boolean merge() {
		int i1 = spans.size() - 1;
		int i2 = i1 - 1;
		Span spn1 = spans.get(i1);
		Span spn2 = spans.get(i2);
		if (spn1.x1 == spn2.x2) {
			spans.remove(i1);
			spans.remove(i2);
			spans.add(new Span(spn2.x1, spn1.x2));
			return true;
		}
		if (spn1.x2 == spn2.x1) {
			spans.remove(i1);
			spans.remove(i2);
			spans.add(new Span(spn1.x1, spn2.x2));
			return true;
		}
		return false;
	}


	private static final class Span {

		final int x1; // incl.
		final int x2; // excl.

		Span(int x1, int x2) {
			if (x1 == x2) throw new IllegalArgumentException();
			this.x1 = x1;
			this.x2 = x2;
		}

		int size() { return x2 - x1; }

		//Span leftOf(int s) { return new Span(x1, x1 + s); }

		Span rightOf(int s) { return new Span(x1 + s, x2); }

	}
}
