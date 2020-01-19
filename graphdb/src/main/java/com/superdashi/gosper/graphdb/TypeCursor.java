package com.superdashi.gosper.graphdb;

import java.util.stream.Stream;

public final class TypeCursor implements Cursor<Type> {

	private final TypeSequence seq;
	private final Resolver resolver;

	TypeCursor(TypeSequence seq, Resolver resolver) {
		this.resolver = resolver;
		this.seq = resolver.restrictTypes(seq);
	}

	@Override
	public Visit visit() {
		return resolver.visit;
	}

	@Override
	public Stream<Type> stream() {
		return seq.stream().mapToObj(resolver::resolveType);
	}

}
