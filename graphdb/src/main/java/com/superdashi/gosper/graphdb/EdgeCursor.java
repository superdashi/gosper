package com.superdashi.gosper.graphdb;

import java.util.Comparator;
import java.util.stream.Stream;

public final class EdgeCursor implements PartCursor<Edge> {

	private final Order order;
	private final Comparator<Part> comparator;
	private final EdgeSequence seq;
	private final Resolver resolver;

	EdgeCursor(EdgeSequence seq, Resolver resolver) {
		assert seq != null;
		assert resolver != null;
		this.seq = resolver.restrictEdges(seq);
		this.resolver = resolver;
		this.order = Order.INTRINSIC;
		this.comparator = Order.INTRINSIC_COMPARATOR;
	}

	private EdgeCursor(EdgeCursor that, Order order) {
		this.seq = that.seq;
		this.resolver = that.resolver;
		this.order = that.order;
		this.comparator = order.comparator(resolver.visit);
	}

	@Override
	public Visit visit() {
		return resolver.visit;
	}

	@Override
	public Order order() {
		return order;
	}

	@Override
	public EdgeCursor order(Order order) {
		if (order == null) throw new IllegalArgumentException("null order");
		return order.equals(this.order) ? this : new EdgeCursor(this, order);
	}

	@Override
	public Stream<Edge> stream() {
		Stream<Edge> stream = seq.stream().mapToObj(resolver::resolveEdge);
		return comparator == Order.INTRINSIC_COMPARATOR ? stream : stream.sorted(comparator);
	}

	@Override
	public int count() {
		return (int) seq.stream().count();
	}

	public EdgeCursor intersect(EdgeCursor that) {
		checkThat(that);
		return intersect(that.seq);
	}

	public EdgeCursor union(EdgeCursor that) {
		checkThat(that);
		return union(that.seq);
	}

	EdgeCursor intersect(EdgeSequence seq) {
		return new EdgeCursor(this.seq.and(seq), resolver);
	}

	EdgeCursor union(EdgeSequence seq) {
		return new EdgeCursor(this.seq.or(seq), resolver);
	}

	private void checkThat(EdgeCursor that) {
		if (that == null) throw new IllegalArgumentException("null that");
		//TODO this actually isn't sufficient
		if (this == that) throw new IllegalArgumentException("cannot combine with self");
		if (!this.resolver.isFromSameVisitAs(that.resolver)) throw new IllegalArgumentException("mismatched visit");
		if (!this.order().equals(that.order())) throw new IllegalArgumentException("incompatible orders");
	}
}
