package com.superdashi.gosper.core;

import java.util.Arrays;

import com.tomgibara.geom.core.Offset;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public final class Layout {

	private static final Storage<Place> placeStorage = StoreType.of(Place.class).storage();
	private static final Storage<Splitter> splitterStorage = StoreType.of(Splitter.class).storage();
	private static final Storage<Merger> mergerStorage = StoreType.of(Merger.class).storage();

	public static final int MAX_ROWS = 5;
	public static final int MAX_COLS = 5;
	public static final int MAX_PLACES = MAX_ROWS * MAX_COLS;

	private static void checkDimensions(int rows, int cols) {
		if (rows < 1) throw new IllegalArgumentException("rows not positive");
		if (cols < 1) throw new IllegalArgumentException("cols not positive");
		if (rows > MAX_ROWS) throw new IllegalArgumentException("rows exceeds maximum");
		if (cols > MAX_ROWS) throw new IllegalArgumentException("cols exceeds maximum");
	}

	public static Layout grid(int rows, int cols) {
		checkDimensions(rows, cols);
		Area[] areas = new Area[rows * cols];
		int i = 0;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				Area area = new Area(c, r, c+1, r+1);
				areas[i++] = area;
			}
		}
		return new Layout(rows, cols, areas);
	}

	public static Layout entire(int rows, int cols) {
		checkDimensions(rows, cols);
		Area area = new Area(0, 0, cols, rows);
		return new Layout(rows, cols, area);
	}

	public final int rows;
	public final int cols;
	public final Store<Place> places;
	public final Store<Splitter> splitters;
	public final Store<Merger> mergers;

	//TODO support creating immutable stores directly from lists
	private Layout(int rows, int cols, Area... areas) {
		this.rows = rows;
		this.cols = cols;

		Store<Place> places = placeStorage.newStore(areas.length);
		Place[] aboves = new Place[cols];
		Store<Merger> mergers = mergerStorage.newStore(areas.length * 2); // no area can merge more than twice
		Store<Splitter> splitters = splitterStorage.newStore(2*(rows-1)*(cols-1));
		int mergeCount = 0;
		int splitCount = 0;
		for (int i = 0; i < areas.length; i++) {
			// create place
			Area area = areas[i];
			Place place = new Place(i, area);
			places.set(i, place);
			// create splitters (vertical)
			int w = area.width();
			for (int d = 1; d < w; d++) {
				splitters.set(splitCount++ , new Splitter(place, true, d));
			}
			// create splitters (horizontal)
			int h = area.height();
			for (int d = 1; d < h; d++) {
				splitters.set(splitCount++ , new Splitter(place, false, d));
			}
			// create merger (across vertical line)
			Place left = i == 0 ? null : places.get(i - 1);
			if (left != null && area.isMergableWithLeft(left.area)) {
				mergers.set(mergeCount++, new Merger(left, place));
			}
			// create merger (across horizontal line)
			Place above = aboves[area.x1];
			if (above != null && area.isMergableWithAbove(above.area)) {
				mergers.set(mergeCount++, new Merger(above, place));
			}
			// update aboves array
			for (int j = area.x1; j < area.x2; j++) {
				aboves[j] = place;
			}
		}

		this.places = places.immutableView();
		this.splitters = splitters.resizedCopy(splitCount).immutableView();
		this.mergers = mergers.resizedCopy(mergeCount).immutableView();
	}

	public Place placeContaining(int x, int y) {
		if (x < 0) throw new IllegalArgumentException("negative x");
		if (y < 0) throw new IllegalArgumentException("negative y");
		if (x >= cols) throw new IllegalArgumentException("x too large");
		if (y >= rows) throw new IllegalArgumentException("y too large");
		for (Place place : places) {
			if (place.area.contains(x,y)) return place;
		}
		throw new IllegalStateException("no place containing " + x + "," + y);
	}

	@Override
	public int hashCode() {
		return rows + 31 * cols + places.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Layout)) return false;
		Layout that = (Layout) obj;
		if (this.rows != that.rows) return false;
		if (this.cols != that.cols) return false;
		if (!this.places.equals(that.places)) return false;
		return true;
	}

	@Override
	public String toString() {
		return new Stringer().toString();
	}

	// inner classes

	private class Stringer {

		private final int r = 2 * rows + 1;
		private final int c = 2 * cols + 2;
		private final char[] cs = new char[r * c];

		@Override
		public String toString() {
			Arrays.fill(cs, ' ');
			for (int i = 1; i <= r; i++) {
				cs[c * i - 1] = '\n';
			}
			for (Place place : places) {
				Part a = place.area;
				hline(a.y1, a.x1, a.x2);
				hline(a.y2, a.x1, a.x2);
				vline(a.x1, a.y1, a.y2);
				vline(a.x2, a.y1, a.y2);
				cross(a.x1, a.y1);
				cross(a.x2, a.y1);
				cross(a.x1, a.y2);
				cross(a.x2, a.y2);
				number(a.x1, a.y1, place.index);
			}
			return new String(cs);
		}

		private void number(int x, int y, int index) {
			cs[charIndex(x, y) + c + 1] = (char) (index <= 9 ? 48 + index : 55 + index);
		}

		private int charIndex(int x, int y) {
			x = x * 2;
			y = y * 2;
			return y * c + x;
		}

		private void cross(int x, int y) {
			cs[charIndex(x, y)] = '+';
		}

		private void hline(int y, int x1, int x2) {
			int i1 = charIndex(x1, y);
			int i2 = charIndex(x2, y);
			Arrays.fill(cs, i1, i2 + 1, '-');
		}

		private void vline(int x, int y1, int y2) {
			int i1 = charIndex(x, y1);
			int i2 = charIndex(x, y2);
			for (int i = i1; i <= i2; i += c) {
				cs[i] = '|';
			}
		}

	}

	public class Place {

		public final int index;
		public final Area area;
		private Rect rect = null;

		private Place(int index, Area area) {
			this.index = index;
			this.area = area;
		}

		//TODO maybe get rid of this?
		public Rect getRect() {
			return rect == null ? rect = area.rect(1f / cols, 1f / rows) : rect;
		}

		// rectangle as a proportion of the unit square subject to the specified gutters
		public Rect getRect(Offset offset, float hGutter, float vGutter) {
			//TODO can we do this more elegantly?
			float uw = (1f - (offset.toMinX - offset.toMaxX) - hGutter * (cols - 1)) / cols; // unit width between gutters
			float uh = (1f - (offset.toMinY - offset.toMaxY) - vGutter * (rows - 1)) / rows; // ditto height
			float left   = offset.toMinX + (uw + hGutter) * area.x1;
			float top    = offset.toMinY + (uh + vGutter) * area.y1;
			float right  = offset.toMinX + (uw + hGutter) * area.x2 - hGutter;
			float bottom = offset.toMinY + (uh + vGutter) * area.y2 - vGutter;
			return Rect.atPoints(left, top, right, bottom);
		}

		@Override
		public int hashCode() {
			return area.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Place)) return false;
			Place that = (Place) obj;
			return this.area.equals(that.area);
		}

		@Override
		public String toString() {
			return area.toString();
		}

	}

	public static class Part {

		public final int x1;
		public final int y1;
		public final int x2;
		public final int y2;

		private Part(int x1, int y1, int x2, int y2) {
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

		@Override
		public int hashCode() {
			return x1 + 31 * (y1 + 31 * (x2 + 31 * y2));
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Part)) return false;
			Part that = (Part) obj;
			return
					this.x1 == that.x1 &&
					this.y1 == that.y1 &&
					this.x2 == that.x2 &&
					this.y2 == that.y2  ;
		}

		@Override
		public String toString() {
			return String.format("(%d,%d) to (%d,%d)", x1, y1, x2, y2);
		}
	}

	public static class Area extends Part implements Comparable<Area> {

		private Area(int x1, int y1, int x2, int y2) {
			super(x1, y1, x2, y2);
		}

		private Rect rect(float sx, float sy) {
			return Rect.atPoints(
					x1 * sx, y1 * sy,
					x2 * sx, y2 * sy
					);
		}

		private boolean isMergableWithLeft(Area a) {
			if (x1 == 0) return false;
			return
					a.x2 == x1 &&
					a.y1 == y1 &&
					a.y2 == y2  ;
		}

		private boolean isMergableWithAbove(Area a) {
			if (y1 == 0) return false;
			return
					a.y2 == y1 &&
					a.x1 == x1 &&
					a.x2 == x2  ;
		}

		public boolean contains(int x, int y) {
			return
					x >= this.x1 &&
					x <  this.x2 &&
					y >= this.y1 &&
					y <  this.y2  ;
		}

		@Override
		public int compareTo(Area that) {
			int c;
			c = this.y1 - that.y1;
			if (c != 0) return c;
			c = this.x1 - that.x1;
			if (c != 0) return c;
			c = this.y2 - that.y2;
			if (c != 0) return c;
			c = this.x2 - that.x2;
			return c;
		}

	}

	public class Splitter extends Part {

		// the part being split
		public final Place place;

		// whether the split is vertical
		public final boolean vertical;

		private Splitter(Place place, boolean vertical, int d) {
			super(
					vertical ? place.area.x1 + d : place.area.x1,
					vertical ? place.area.y1 : place.area.y1 + d,
					vertical ? place.area.x1 + d : place.area.x2,
					vertical ? place.area.y2 : place.area.y1 + d
					);
			this.place = place;
			this.vertical = vertical;
		}

		public Layout split() {
			Area[] areas = new Area[places.size() + 1];
			int i = 0;
			for (; i < place.index; i++) {
				areas[i] = places.get(i).area;
			}
			Area area = place.area;
			areas[i++] = new Area(
					area.x1,
					area.y1,
					vertical ? x1 : area.x2,
					vertical ? area.y2 : y1
					);
			areas[i++] = new Area(
					vertical ? x1 : area.x1,
					vertical ? area.y1 : y1,
					area.x2,
					area.y2
					);
			for (; i < areas.length; i++) {
				areas[i] = places.get(i - 1).area;
			}
			if (!vertical) Arrays.sort(areas);
			return new Layout(rows, cols, areas);
		}
	}

	public class Merger extends Part {

		public final Place place1;
		public final Place place2;

		private Merger(Place p1, Place p2) {
			super(
					p1.area.x1,
					p1.area.y1,
					p2.area.x2,
					p2.area.y2
					);
			this.place1 = p1;
			this.place2 = p2;
		}

		public Layout merge() {
			Area[] areas = new Area[places.size() - 1];
			int i = 0;
			for (Place place : places) {
				if (place == place1) {
					// insert merged
					areas[i++] = new Area(
							place1.area.x1,
							place1.area.y1,
							place2.area.x2,
							place2.area.y2
							);
				} else if (place == place2) {
					// skip
				} else {
					//duplicate
					areas[i++] = place.area;
				}
			}
			return new Layout(rows, cols, areas);
		}

	}
}
