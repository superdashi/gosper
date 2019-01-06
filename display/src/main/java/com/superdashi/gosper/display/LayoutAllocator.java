package com.superdashi.gosper.display;

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Optional;

import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Layout;
import com.superdashi.gosper.core.Layout.Place;
import com.superdashi.gosper.display.DynamicAtlas.Allocator;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.tomgibara.geom.core.Rect;

//TODO could just extend Rectangle and store place to avoid all this coord munging
public class LayoutAllocator implements Allocator<Place> {

	// assumes coords won't be negative or exceed MAX_SHORT
//	private static int x(int c) { return c & 0xffff; }
//	private static int y(int c) { return c >>> 16; }
//	private static int c(int x, int y) { return (y << 16) | x; }
	private static int c(Rectangle r) { return (r.y << 16) | r.x; }

	private int width;
	private int height;

	// note layout places are ordered top-left to bottom-right, so packed y,x coords will be ordered as integers
	private final int[] coords = new int[Layout.MAX_PLACES];
	private final BitStore allocs = Bits.store(Layout.MAX_PLACES);
	private Layout layout = null;

	@Override
	public void init(int width, int height) {
		DashiLog.debug("Layout allocator initialized with dimensions: {0} x {1}", width, height);
		this.width = width;
		this.height = height;
	}

	public void setLayout(Layout layout) {
		if (layout == this.layout || layout != null && layout.equals(this.layout)) return; // nothing to do
		//TODO report actual allocations
		int count = count();
		if (count != 0) throw new IllegalStateException("Cannot change layout with outstanding allocations: " + count);
		this.layout = layout;
		if (layout == null) {
			Arrays.fill(coords, 0);
		} else {
			Rectangle r = new Rectangle();
			int length  = layout.places.size();
			for (int i = 0; i < length; i++) {
				computePlace(layout.places.get(i), r);
				coords[i] = c(r);
			}
		}
	}

	@Override
	public Optional<Rectangle> allocate(int w, int h, Place hint) {
		if (hint == null) throw new IllegalArgumentException("hint required");
		DashiLog.debug("attempting to allocate {0}x{1} for place {2}", w, h, hint);
		if (allocs.getBit(hint.index)) throw new IllegalStateException("already made allocation for place " + hint);
		Rectangle r = new Rectangle();
		computePlace(hint, r);
		//TODO this is ugly - both caller and this are computing bounds
		if (w > r.width) {
			DashiLog.warn("width {0} exceeds allocation for place {2}: {1}", w, r.width, hint);
			return Optional.empty();
		}
		if (h > r.height) {
			DashiLog.warn("height {0} exceeds allocation for place {2}: {1}", h, r.height, hint);
			return Optional.empty();
		}
		// give the caller the exact dimensions they want
		r.width = w;
		r.height = h;
		allocs.setBit(hint.index, true);
		return Optional.of(r);
	}

	@Override
	public void release(Rectangle rect) {
		int index = Arrays.binarySearch(coords, c(rect));
		if (index < 0) throw new IllegalStateException("unrecognized rectangle: " + rect);
		if (!allocs.getThenSetBit(index, false)) throw new IllegalStateException("Rectangle already deallocated: " + rect + " (index " + index + ")");
	}

	@Override
	public void destroy() { }

	private void computePlace(Place place, Rectangle r) {
		Rect rect = place.getRect();
		r.x = (int) (rect.minX * width);
		r.y = (int) (rect.minY * height);
		r.width = (int) (rect.maxX * width) - r.x;
		r.height = (int) (rect.maxY * height) - r.y;
		//TODO can we do something better about this situation
		if (r.width == 0 || r.height == 0) throw new IllegalStateException("place too small? " + place);
	}

	private int count() {
		return allocs.ones().count();
	}

}
