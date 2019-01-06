package com.superdashi.gosper.item;

import java.util.Comparator;

public final class ValueOrder {

	// statics

	private static Comparator<Value> EMPTY_FIRST = (a,b) -> a.isEmpty() ? (b.isEmpty() ? 0 : -1) : (b.isEmpty() ?  1 : 0);
	private static Comparator<Value> EMPTY_LAST  = (a,b) -> a.isEmpty() ? (b.isEmpty() ? 0 :  1) : (b.isEmpty() ? -1 : 0);

	private static Comparator<Value> emptyFirst(Value.Type type, Comparator<Value> c) {
		return (a,b) -> {
			a = a.as(type);
			b = b.as(type);
			return a.isEmpty() ? (b.isEmpty() ? 0 : -1) : (b.isEmpty() ?  1 : c.compare(a, b));
		};
	}

	private static Comparator<Value> emptyLast(Value.Type type, Comparator<Value> c) {
		return (a,b) -> {
			a = a.as(type);
			b = b.as(type);
			return a.isEmpty() ? (b.isEmpty() ? 0 :  1) : (b.isEmpty() ? -1 : c.compare(a, b));
		};
	}

	private static final int TYPE_COUNT = Value.Type.values().length;
	private static final ValueOrder[] sorts = new ValueOrder[TYPE_COUNT * 4];

	static ValueOrder order(Value.Type type, boolean ascending, boolean emptyFirst) {
		int index = type.ordinal();
		if (ascending) index += 2 * TYPE_COUNT;
		if (emptyFirst) index += TYPE_COUNT;
		return sorts[index] == null ? sorts[index] = new ValueOrder(index, type, ascending, emptyFirst) : sorts[index];
	}

	// fields

	public final boolean ascending;
	public final Value.Type type;
	public final boolean emptyFirst;

	private final int index;
	private final Comparator<Value> comparator;

	// constructors

	private ValueOrder(int index, Value.Type type, boolean ascending, boolean emptyFirst) {
		this.index = index;
		this.type = type;
		this.ascending = ascending;
		this.emptyFirst = emptyFirst;

		Comparator<Value> comparator;
		if (type == Value.Type.EMPTY) {
			comparator = emptyFirst ? EMPTY_FIRST : EMPTY_LAST;
		} else {
			comparator = ascending ? type.compareAscending : type.compareDescending;
			comparator = emptyFirst ? emptyFirst(type, comparator) : emptyLast(type, comparator);
		}
		this.comparator = comparator;
	}

	// accessor methods

	public Comparator<Value> comparator() {
		return comparator;
	}

	// 'builder' methods

	public ValueOrder ascending() {
		return ascending ? this : order(type, false, emptyFirst);
	}

	public ValueOrder descending() {
		return ascending ? order(type, false, emptyFirst) : this;
	}

	public ValueOrder typed(Value.Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return order(type, ascending, emptyFirst);
	}

	public ValueOrder emptyFirst() {
		return emptyFirst ? this : order(type, ascending, emptyFirst);
	}

	public ValueOrder emptyLast() {
		return emptyFirst ? order(type, ascending, false) : this;
	}

	// object methods

	@Override
	public int hashCode() {
		return index;
	}

	// equals is identity because instances are canonical

	@Override
	public String toString() {
		return String.format("type: %s, ascending: %b, emptyFirst: %d", type, ascending, emptyFirst);
	}

}
