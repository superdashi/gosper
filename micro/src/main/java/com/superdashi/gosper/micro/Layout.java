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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.function.Function;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.util.Geometry;
import com.tomgibara.geom.core.Point;
import com.tomgibara.intgeom.IntAxis;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntDir;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;
import com.tomgibara.storage.Stores;

//TODO add groups- have null directions, don't increase count, but can apply margins, weights and styles, group implicitly added in direction axis change, groups on groups is idempotent
public final class Layout {

	private static final boolean DEBUG_LAYOUT = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.Layout.DEBUG"));

	private static final Layout single = new Layout();

	public enum Direction {

		LEFT_OF (IntDir.LESS_X),
		RIGHT_OF(IntDir.MORE_X),
		ABOVE   (IntDir.LESS_Y),
		BELOW   (IntDir.MORE_Y);

		final IntDir dir;
		final boolean horizontal;
		final boolean vertical;

		private Direction(IntDir dir) {
			this.dir = dir;
			horizontal = dir.axis.horizontal;
			vertical = dir.axis.vertical;
		}

		Direction reverse() {
			switch (this) {
			case LEFT_OF : return RIGHT_OF;
			case RIGHT_OF: return LEFT_OF ;
			case ABOVE   : return BELOW   ;
			case BELOW   : return ABOVE   ;
			default: throw new IllegalStateException();
			}
		}
	}

	private static void assignMost(LocHolder[] holders, Function<LocHolder, Float> fn, Location loc, Location opp) {
		LocHolder most = findMost(holders, fn, false);
		if (most == null) return;
		if (most.location != null) {
			if (most.location == opp) {
				most.location = Location.center;
			} else if (most.location.canon) {
				throw new IllegalStateException("unexpected location clash");
			} else {
				/* ignore - externally assigned location */
			}
		} else {
			most.location = loc;
		}
	}

	private static void assignMost(LocHolder[] holders, Function<LocHolder, Float> fn, Location loc) {
		LocHolder most = findMost(holders, fn, true);
		if (most != null) most.location = loc;
	}

	private static LocHolder findMost(LocHolder[] holders, Function<LocHolder, Float> fn, boolean ignoreLocated) {
		LocHolder best = null;
		float value = Float.NEGATIVE_INFINITY;
		for (LocHolder holder : holders) {
			if (ignoreLocated && holder.location != null) continue;
			float v = fn.apply(holder);
			if (v == value) {
				best = null;
			} else if (v > value) {
				best = holder;
				value = v;
			}
		}
		return best;
	}

	private static void evaluateHolder(LocHolder holder, LocHolder[] holders) {
		if (holder.location != null) return;
		int ac = 0;
		int bc = 0;
		int lc = 0;
		int rc = 0;
		float x = holder.center.x;
		float y = holder.center.y;
		for (LocHolder h : holders) {
			if (h == holder) continue;
			float hx = h.center.x;
			float hy = h.center.y;
			Direction direction = h.layout.direction;
			boolean horizontal = direction == null || direction.horizontal;
			boolean vertical = direction == null || direction.vertical;
			if (horizontal) {
				if (hx < x) lc ++;
				if (hx > x) rc ++;
			}
			if (vertical) {
				if (hy < y) ac ++;
				if (hy > y) bc ++;
			}
		}
		Location location;
		if (lc == rc && ac == bc) {
			location = Location.center;
		} else if (lc == rc) {
			location = bc < ac ? Location.centerBottom : Location.centerTop;
		} else if (ac == bc) {
			location = rc < lc ? Location.centerRight : Location.centerLeft;
		} else if (ac == 0) {
			location = Location.centerTop;
		} else if (bc == 0) {
			location = Location.centerBottom;
		} else if (lc == 0) {
			location = Location.centerLeft;
		} else if (rc == 0) {
			location = Location.centerRight;
		} else {
			throw new IllegalStateException();
		}
		holder.location = location;
	}

//	private static IntRect computeAddOn(IntRect basis, IntRect maximum, Direction direction) {
//		switch (direction) {
//		case ABOVE   : return IntRect.bounded(basis.minX, basis.minY, maximum.maxX, maximum.minY);
//		case BELOW   : return IntRect.bounded(basis.minX, basis.maxY, maximum.maxX, maximum.maxY);
//		case LEFT_OF : return IntRect.bounded(basis.minX, basis.minY, maximum.minX, maximum.maxY);
//		case RIGHT_OF: return IntRect.bounded(basis.maxX, basis.minY, maximum.maxX, maximum.maxY);
//		default: throw new IllegalStateException();
//		}
//	}

	public static Layout single() {
		return single;
	}

	// fields

	private final Layout former;
	private final Direction direction;
	private final int count;

	private final Style style;
	private final IntDimensions minimumSize;
	private final float weight;
	private final Location location;

	// lazy computed
	//TODO could cache against list of directions
	private Location[] locations;

	// constructors

	private Layout() {
		former = null;
		direction = null;
		count = 1;
		style = Style.noStyle();
		minimumSize = IntDimensions.ONE_BY_ONE;
		weight = 0f;
		location = null;
	}

	private Layout(Layout former, Direction direction) {
		if (former == null) throw new IllegalArgumentException("null former");
		this.former = former;
		this.direction = direction;
		count = direction == null ? former.count : former.count + 1;
		style = Style.noStyle();
		minimumSize = IntDimensions.ONE_BY_ONE;
		weight = 0f;
		location = null;
	}

	private Layout(Layout former, Direction direction, Style style, IntDimensions minimumSize, float weight, Location location) {
		if (former == null) {
			this.former = null;
			this.direction = null;
			count = 1;
		} else if (direction == null) {
			this.former = former;
			this.direction = null;
			count = former.count;
		} else {
			this.former = former;
			this.direction = direction;
			count = former.count + 1;
		}
		this.style = style;
		this.minimumSize = minimumSize;
		this.weight = weight;
		this.location = location;
	}

	// accessors

	public int placeCount() {
		return count;
	}

	// building methods

	public Layout add(Direction direction) {
		if (direction == null) throw new IllegalArgumentException("null direction");
		if (isGroup() && former.direction.horizontal == direction.horizontal) throw new IllegalArgumentException("direction not perpendicular");
		checkComplexity();
		// if not single, not a group, and not parallel, make an intermediate group
		Layout basis = isRoot() || isGroup() || this.direction.horizontal == direction.horizontal ? this : new Layout(this, null);
		return new Layout(basis, direction);
	}

	public Layout group() {
		if (isRoot()) throw new IllegalStateException("cannot group single place");
		if (isGroup()) throw new IllegalStateException("cannot group a group");
		return new Layout(this, null);
	}

	public Layout withStyle(Style style) {
		if (style == null) throw new IllegalArgumentException("null style");
		return new Layout(former, direction, style, minimumSize, weight, location);
	}

	public Layout withMinimumSize(IntDimensions minimumSize) {
		if (minimumSize == null) throw new IllegalArgumentException("null minimumSize");
		if (minimumSize.width == 0 || minimumSize.height == 0) throw new IllegalArgumentException("minimum size is zero in one or more dimensions");
		return new Layout(former, direction, style, minimumSize, weight, location);
	}

	public Layout withWeight(float weight) {
		if (weight < 0f) throw new IllegalArgumentException("negative weight");
		return new Layout(former, direction, style, minimumSize, weight, location);
	}

	public Layout withLocation(String locationName) {
		if (locationName == null) throw new IllegalArgumentException("null locationName");
		Location location = Location.named(locationName);
		if (location.canon) throw new IllegalArgumentException("reserved locationName");
		if (isGroup()) throw new IllegalStateException("cannot assign group a location");
		return new Layout(former, direction, style, minimumSize, weight, location);
	}

	// convenience methods

	public Layout addAbove() {
		return add(Direction.ABOVE);
	}

	public Layout addBelow() {
		return add(Direction.BELOW);
	}

	public Layout addLeft() {
		return add(Direction.LEFT_OF);
	}

	public Layout addRight() {
		return add(Direction.RIGHT_OF);
	}

	public Layout withMinimumSize(int width, int height) {
		return withMinimumSize(IntDimensions.of(width, height));
	}

	public Layout withMinimumWidth(int width) {
		if (width == 0) throw new IllegalArgumentException("zero width");
		return withMinimumSize(minimumSize.withWidth(width));
	}

	public Layout withMinimumHeight(int height) {
		if (height == 0) throw new IllegalArgumentException("zero height");
		return withMinimumSize(minimumSize.withHeight(height));
	}

	// computational methods

	public Sizer sizer(VisualSpec spec) {
		if (spec == null) throw new IllegalArgumentException("null spec");
		return publicSizer(spec.styles.defaultPlaceStyle);
	}

	// object methods

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	// package scoped methods

	Location[] locations() {
		return locations == null ? locations = computeLocations() : locations;
	}

	// private helper methods

	private void checkComplexity() {
		if (count == 5) throw new IllegalStateException("maximum layout complexity exceeded");
	}

	private boolean isRoot() {
		return former == null;
	}

	private boolean isGroup() {
		return former != null && direction == null;
	}

	private Style computeEffectiveStyle(Style baseStyle) {
		return baseStyle.mutableCopy().apply(style).immutable();
	}

	private IntDimensions computeEffectiveMinimum(Style effectiveStyle, IntDimensions minimumContentSize) {
		IntMargins margins = effectiveStyle.margins();
		if (!margins.isExpanding()) throw new IllegalArgumentException("invalid place style margins");
		return minimumSize.growToInclude(minimumContentSize).plus(margins);
	}

	private Location[] computeLocations() {
		// trivial case
		if (former == null) return new Location[] { location == null ? Location.center : location };

		// create convenient objects for conducting the algorithm
		LocHolder[] holders = createLocHolders();

		boolean allPopulated = true;
		for (LocHolder holder : holders) {
			if (holder.location != null) continue;
			allPopulated = false;
			break;
		}

		if (!allPopulated) {

			// first assign locations to obvious extremities
			assignMost(holders, h -> -h.center.x, Location.left, Location.right);
			assignMost(holders, h -> h.center.x, Location.right, Location.left);
			assignMost(holders, h -> -h.center.y, Location.top, Location.bottom);
			assignMost(holders, h -> h.center.y, Location.bottom, Location.top);

			// now identify if there are any remaining non-located places, and check if they lie in a line
			float x = Float.NaN;
			float y = Float.NaN;
			int count = 0;
			for (LocHolder holder : holders) {
				if (holder.location != null) continue;
				count ++;
				float hx = holder.center.x;
				float hy = holder.center.y;
				if (Float.isNaN(x) && Float.isNaN(y)) {
					x = hx;
					y = hy;
				} else if (hx == x) {
					y = Float.NaN;
				} else if (hy == y) {
					x = Float.NaN;
				}
			}

			if (count > 0) { // there is more work to be done; count will be 1,2 or 3
				if (count > 1) { // if there's more than one, they may lie in a line
					boolean xn = Float.isNaN(x);
					boolean yn = Float.isNaN(y);
					if (xn && !yn) { // horizontal
						assignMost(holders, h -> -h.center.x, Location.centerLeft);
						assignMost(holders, h -> h.center.x, Location.centerRight);
					} else if (!xn && yn) { // vertical
						assignMost(holders, h -> -h.center.y, Location.centerTop);
						assignMost(holders, h -> h.center.y, Location.centerBottom);
					} // non linear - fall through
				}
				// assign any remaining locations based on the number of places in each direction
				for (LocHolder holder : holders) {
					evaluateHolder(holder, holders);
				}
			}
		}

		// extract just the locations...
		Location[] locations = new Location[holders.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = holders[i].location;
		}
		// ... and return them
		return locations;
	}

	private LocHolder[] createLocHolders() {
		LocHolder[] holders = new LocHolder[count];
		populateLocHolders(holders, count - 1);
		return holders;
	}

	private IntRect populateLocHolders(LocHolder[] holders, int index) {
		if (isGroup()) return former.populateLocHolders(holders, index); // ignore groups
		IntRect bounds = direction == null ?
				bounds = IntRect.atOrigin(0, 0) :
				former.populateLocHolders(holders, index - 1);
		LocHolder holder = new LocHolder(bounds, this);
		holders[index] = holder;
		bounds = holder.bounds.growToIncludeRect(bounds);
		return bounds;
	}

	private IntRect processPlaceHolders(PlaceHolder[] holders, Layout group, int initial, int minimum, IntRect maximum, boolean maximize) {
		if (DEBUG_LAYOUT) Debug.logging().message("Processing on layout {} to {}").values(this, maximize ? "maximize" : "minimize");
		if (isRoot() || former.isGroup()) {
			assert group != null;
			if (DEBUG_LAYOUT) Debug.logging().message("Reach end of group that started at index {}, layout {}").values(initial, holders[initial].layout).log();
			// the former layout changes direction, so process as a group
			Direction direction = holders[initial].layout.direction;
			IntAxis axis = direction.dir.axis;
			int index = count - 1;
			if (DEBUG_LAYOUT) Debug.logging().message("Starting processing step at index {} with direction: {} - axis {}").values(index, direction, axis).log();
			float totalWeight = 0f;
			int totalSize = 0;
			int newMinimum = group.minimumSize.projectAlongAxis(axis).max;
			IntRect newMaximum = maximum;
			LinkedList<PlaceHolder> list = new LinkedList<>();
			float groupWeight;
			// include in this list a null to mark the location of a group we're building on
			if (isRoot()) {
				groupWeight = 0f;
			} else {
				list.add(null);
				groupWeight = former.weight;
			}
			totalWeight += groupWeight;
			// do four things in this loop:
			//   reduce the maximum space
			//   calculate the total
			//   compute new minimum
			//   build a list holders ordered by position
			for (int i = index; i <= initial; i++) {
				PlaceHolder holder = holders[i];
				Layout layout = holder.layout;
				totalWeight += layout.weight;
				int size = holder.effectiveMinimum.projectAgainstAxis(axis).max;
				totalSize += size;
				newMinimum = Math.max(newMinimum, holder.effectiveMinimum.projectAlongAxis(axis).max);
				int maximumSize = newMaximum.projectAgainstAxis(axis).unitSize();
				newMaximum = newMaximum.resized(direction.dir, maximumSize - size);
				if (DEBUG_LAYOUT && size > 0) Debug.logging().message("New maximum became {} due to reducing {} by {}").values(newMaximum, maximumSize, size).log();
				IntVector v = layout.direction == null ? IntVector.ZERO : layout.direction.dir.unitVector;
				int d = v.x + v.y; // either +/- 1 -- or zero for root
				if (d < 0) {
					list.addFirst(holder);
				} else {
					list.addLast(holder);
				}
			}
			if (DEBUG_LAYOUT && newMinimum > 0) Debug.logging().message("New minimum is {}").values(newMinimum).log();

			// deduce how much space we need to add on across all places in this group
			IntRect basis; // null basis indicates there's no group that we're extending
			if (isRoot()) {
				// base case
				basis = null;
			} else {
				// recursive step
				if (DEBUG_LAYOUT) Debug.logging().message("Recursing to minimize with new maximum/minimum: {}/{}").values(newMaximum, newMinimum).log();
				// we always ask for minimization, in the first instance,
				// this allows weighted sharing of residual space
				basis = former.processPlaceHolders(holders, null, former.count - 1, newMinimum, newMaximum, false);
			}

			IntRect avail; // this is the space to be split by the places in this group
			if (maximize) {
				avail = maximum;
				if (DEBUG_LAYOUT) Debug.logging().message("Available space set to maximum: {}").values(avail).log();
			} else if (axis == IntAxis.X) {
				avail = maximum.vectorToMinimumCoords().translatedDimensions(maximum.width(), newMinimum);
				if (DEBUG_LAYOUT) Debug.logging().message("Available space set for horizontal group: {}").values(avail).log();
			} else {
				avail = maximum.vectorToMinimumCoords().translatedDimensions(newMinimum, maximum.height());
				if (DEBUG_LAYOUT) Debug.logging().message("Available space set for vertical group: {}").values(avail).log();
			}

			if (list.size() == 1) { // only possible at root, others will include null marked sub group
				// no splitting, just use all the available space
				holders[initial].bounds = avail;
			} else {
				// split the available space
				int size = avail.projectAgainstAxis(axis).unitSize(); // stores the rightmost edge
				int groupSize = basis == null ? 0 : basis.projectAgainstAxis(axis).unitSize();
				int overSize = size - totalSize - groupSize;
				if (DEBUG_LAYOUT) Debug.logging().message("Parameters for splitting are size: {}, totalSize: {}, groupSize: {}, overSize: {}").values(size, totalSize, groupSize, overSize).log();
				assert overSize >= 0;
				int parts = list.size();
				int[] splits = new int[parts];
				if (overSize == 0) {
					// there's no extra space to distribute
					if (DEBUG_LAYOUT) Debug.logging().message("No over-size to distribute.").log();
					int sizeSum = 0;
					for (int i = 0; i < parts - 1; i++) {
						PlaceHolder holder = list.get(i);
						sizeSum += holder == null ? groupSize : holder.effectiveMinimum.projectAgainstAxis(axis).max;
						splits[i] = sizeSum;
					}
				} else {
					if (totalWeight == 0f) { // split evenly
						if (DEBUG_LAYOUT) Debug.logging().message("No weights, splitting evenly.").log();
						int sizeSum = 0;
						for (int i = 0; i < parts - 1; i++) {
							PlaceHolder holder = list.get(i);
							sizeSum += holder == null ? groupSize : holder.effectiveMinimum.projectAgainstAxis(axis).max;
							splits[i] = sizeSum + overSize * (i+1) / parts;
						}
					} else { // split as per the weights
						if (DEBUG_LAYOUT) Debug.logging().message("Splitting by weight.").log();
						int sizeSum = 0;
						float weightSum = 0f;
						for (int i = 0; i < splits.length - 1; i++) {
							PlaceHolder holder = list.get(i);
							sizeSum += holder == null ? groupSize : holder.effectiveMinimum.projectAgainstAxis(axis).max;
							weightSum += holder == null ? groupWeight : holder.layout.weight;
							splits[i] = sizeSum + Math.round(overSize * weightSum / totalWeight);
						}
					}
				}
				splits[splits.length - 1] = size;
				if (DEBUG_LAYOUT) Debug.logging().message("Performing splits at {} for {}").values(Stores.ints(splits), list).log();
				// assign the bounds
				int previous = 0;
				for (int i = 0; i < splits.length; i++) {
					int split = splits[i];
					IntRect bounds = direction.horizontal ?
							IntRect.bounded(avail.minX + previous, avail.minY, avail.minX + split, avail.maxY) :
							IntRect.bounded(avail.minX, avail.minY + previous, avail.maxX, avail.minY + split);
					PlaceHolder holder = list.get(i);
					if (holder == null) {
						if (groupSize != split) {
							//we have to stretch the previously allocated group via maximization
							if (DEBUG_LAYOUT) Debug.logging().message("Recursing to maximize with new maximum/minimum: {}/{}").values(bounds, newMinimum).log();
							basis = former.processPlaceHolders(holders, null, former.count - 1, newMinimum, bounds, true);
							assert bounds.containsRect(basis);
						}
					} else {
						holder.bounds = bounds ;
					}
					previous = split;
				}
			}
			// return the space we filled
			if (DEBUG_LAYOUT) Debug.logging().message("Returning with the available space filled.").log();
			return avail;
		} else if (isGroup()) {
			// new direction, pseudo-recurse until group starts
			if (DEBUG_LAYOUT) Debug.logging().message("Start of group, passing through to find start.").log();
			return former.processPlaceHolders(holders, this, initial, minimum, maximum, maximize);
		} else {
			// direction continues, pseudo-recurse until group starts
			if (DEBUG_LAYOUT) Debug.logging().message("Continued group, passing through to find start.").log();
			return former.processPlaceHolders(holders, group, initial, minimum, maximum, maximize);
		}
	}

	private void toString(StringBuilder sb) {
		if (isRoot()) {
			sb.append("ROOT");
		} else if (isGroup()) {
			sb.append('(');
			former.toString(sb);
			sb.append(')');
		} else {
			former.toString(sb);
			sb.append(',').append(direction);
		}
	}

	private Sizer publicSizer(Style baseStyle) {
		// fabricate a top-level group if necessary - this allows us to assume that everything is grouped when processing placeholders
		return !isRoot() && !isGroup() ? group().privateSizer(baseStyle) : privateSizer(baseStyle);
	}

	private Sizer privateSizer(Style baseStyle) {
		return new Sizer(baseStyle);
	}

	// inner classes

	public class Sizer {

		public final Layout layout = Layout.this;
		private final Style baseStyle;
		private final Style effectiveStyle;
		private Sizer formerLayouter = null; // computed lazily
		private PlaceHolder[] holders = null; // computed lazily

		private Sizer(Style baseStyle) {
			this.baseStyle = baseStyle;
			effectiveStyle = computeEffectiveStyle(baseStyle);
		}

		public boolean isEntire() {
			return effectiveStyle.margins().isVoid() && (former == null || formerSizer().isEntire());
		}

		public Location[] locations() {
			return Layout.this.locations();
		}

		public Style styleAtLocation(Location location) {
			if (location == null) throw new IllegalArgumentException("null location");
			PlaceHolder[] holders = holders();
			for (PlaceHolder holder : holders) {
				if (holder.location == location) return holder.effectiveStyle;
			}
			throw new IllegalArgumentException("unknown location");
		}

		//TODO this is problematic, because constraints are not applied
		public IntDimensions minimumDimensions(Constraints constraints) {
			IntDimensions minimum = computeEffectiveMinimum(effectiveStyle, IntDimensions.NOTHING);
			// base-case
			if (isRoot()) return minimum;
			// recursive step
			IntDimensions dimensions = formerSizer().minimumDimensions(constraints);
			if (isGroup()) return dimensions; //TODO shoud apply minima applied by group
			return direction.horizontal ?
				IntDimensions.of(dimensions.width + minimum.width, Math.max(dimensions.height, minimum.height)) :
				IntDimensions.of(Math.max(dimensions.width, minimum.width), dimensions.height + minimum.height);
		}

		public Optional<Places> computePlaces(Constraints constraints, Place... existing) {
			if (constraints == null) throw new IllegalArgumentException("null constraints");
			if (DEBUG_LAYOUT) Debug.logger().debug().message("Starting computePlaces on {} with constraints").values(this, constraints).log();

			// first compute minimum dimensions, and check the layout will fit
			IntRect bounds = constraints.bounds();
			IntDimensions minimum = minimumDimensions(constraints);
			if (!bounds.dimensions().meets(minimum)) return Optional.empty();

			// create holders for algorithm state
			PlaceHolder[] holders = holders();
			for (PlaceHolder holder : holders) {
				holder.applyConstraints(constraints);
			}

			IntRect placesBounds;
			if (isRoot()) {
				// special case for single layout (to avoid direction resolution ambiguity)
				holders[0].bounds = placesBounds = bounds;
			} else {
				// compute the dimensions and store in the holders
				placesBounds = processPlaceHolders(holders, null, count - 1, 0, bounds, true);
			}

			// build the places
			Place[] places = Arrays.copyOf(existing, existing.length + count);
			for (int i = 0; i < count; i++) {
				PlaceHolder holder = holders[i];
				Place place = holder.toPlace();
				places[existing.length + i] = place;
				if (DEBUG_LAYOUT) Debug.logging().message("Computed place at index {} is {}").values(i, place).log();
			}

			// return the places
			return Optional.of(new Places(placesBounds, places));
		}

		private Sizer formerSizer() {
			if (former == null) return null;
			// groups apply their effective style, not their base style
			return formerLayouter == null ? formerLayouter = former.privateSizer(isGroup() ? effectiveStyle : baseStyle) : formerLayouter;
		}

		private PlaceHolder[] holders() {
			if (holders != null) return holders;
			Location[] locations = locations();
			PlaceHolder[] holders = new PlaceHolder[count];
			Sizer layouter = this;
			{
				int i = count - 1;
				while (layouter != null) {
					if (!layouter.layout.isGroup()) {
						holders[i] = new PlaceHolder(locations[i], layouter.layout, layouter.effectiveStyle);
						i--;
					}
					layouter = layouter.formerSizer();
				}
			}
			return holders;
		}
	}

	// temporary object used for calculating layout locations
	private static class LocHolder {
		final Layout layout;
		final IntRect bounds;
		final Point center;
		Location location;

		LocHolder(IntRect previous, Layout layout) {
			this.layout = layout;
			if (previous.isDegenerate()) {
				bounds = IntRect.UNIT_SQUARE;
			} else {
				IntDir dir = layout.direction.dir;
				int size = previous.projectAgainstAxis(dir.axis).unitSize();
				bounds = previous.translatedBy(dir.unitVector.scaled(size)).resized(dir, 1);
			}
			center = Geometry.exactCenter(bounds);
			location = layout.location;
		}

		@Override
		public String toString() {
			return bounds + " " + center + (location == null ? "" : " " + location) ;
		}
	}

	private static class PlaceHolder {
		final Location location;
		final Layout layout;
		final Style effectiveStyle;
		IntDimensions effectiveMinimum;
		IntRect bounds;

		PlaceHolder(Location location, Layout layout, Style effectiveStyle) {
			this.location = location;
			this.layout = layout;
			this.effectiveStyle = effectiveStyle;
		}

		void applyConstraints(Constraints constraints) {
			effectiveMinimum = layout.computeEffectiveMinimum(effectiveStyle, constraints.getMinimumSize(location));
		}

		Place toPlace() {
			return new Place(location, bounds, effectiveStyle);
		}

		@Override
		public String toString() {
			return layout + " " + bounds;
		}
	}
}
