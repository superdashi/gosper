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
package com.superdashi.gosper.graphdb;

import java.util.Comparator;
import java.util.function.Function;

import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.item.ValueOrder;

public abstract class Order {

	static final Comparator<Part> INTRINSIC_COMPARATOR = Part::compareTo;

	static final Order INTRINSIC = new Order() {
		@Override Comparator<Part> comparator(Visit visit) {
			return INTRINSIC_COMPARATOR;
		}
	};

	public static Order intrinsic() {
		return INTRINSIC;
	}

	public static Order byAttr(AttrName name) {
		if (name == null) throw new IllegalArgumentException("null name");
		return new ByAttr(name, null);
	}

	public static Order byAttr(AttrName name, ValueOrder order) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (order == null) throw new IllegalArgumentException("null order");
		return new ByAttr(name, order);
	}

	public static Order byAttr(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		return new ByAttrName(name, null);
	}

	public static Order byAttr(String name, ValueOrder order) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (order == null) throw new IllegalArgumentException("null order");
		return new ByAttrName(name, order);
	}

	private Order() {}

	abstract Comparator<Part> comparator(Visit visit);

	private static abstract class Extractive extends Order {

		abstract Function<Part, Value> extractor(Visit visit); // returns a function that can extract the sorted value from the part
		abstract ValueOrder order(Visit visit);
		abstract Comparator<Part> comparator(); // not null if there is a fixed comparator that can be used

		@Override
		Comparator<Part> comparator(Visit visit) {
			Comparator<Part> comparator = comparator();
			return comparator == null ? comparator(visit, order(visit)) : comparator;
		}

		Comparator<Part> comparator(Visit visit, ValueOrder order) {
			return Comparator.comparing(extractor(visit), order.comparator()).thenComparing(INTRINSIC_COMPARATOR);
		}

		ValueOrder defaultOrder(Visit visit, AttrName attr) {
			return visit.types.getOrDefault(attr, Value.Type.STRING).order();
		}
	}

	private static final class ByAttr extends Extractive {

		private final AttrName attr;
		private final Function<Part, Value> extractor;
		private final Comparator<Part> comparator;

		ByAttr(AttrName attr, ValueOrder order) {
			this.attr = attr;
			extractor = p -> p.attrs().get(attr);
			// order important, extractor must be set before method call
			comparator = order == null ? null : comparator(null, order);
		}

		@Override Function<Part, Value> extractor(Visit visit) { return extractor; }
		@Override ValueOrder order(Visit visit) { return defaultOrder(visit, attr); }
		@Override Comparator<Part> comparator() { return comparator; }

	}

	private static final class ByAttrName extends Extractive {

		private final String name;
		private final ValueOrder order;

		ByAttrName(String name, ValueOrder order) {
			this.name = name;
			this.order = order;
		}

		@Override Comparator<Part> comparator() {
			return null;
		}

		@Override Function<Part, Value> extractor(Visit visit) {
			// we create a dedicated extractor so that the name lookup is done once for a given comparator
			AttrName attr = attr(visit);
			return p -> p.attrs().get(attr);
		}

		@Override
		ValueOrder order(Visit visit) {
			return order == null ? defaultOrder(visit, attr(visit)) : order;
		}

		private AttrName attr(Visit visit) {
			return visit.view.attrName(name);
		}
	}

}
