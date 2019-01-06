package com.superdashi.gosper.graphdb;

import java.util.Comparator;

import com.superdashi.gosper.item.Value;

final public class Attribute {

	static final Comparator<Attribute> comparator = Comparator.comparing(a -> a.name);

	//TODO consider keeping name and namespace separate
	public final AttrName name;
	public final Value.Type type;
	public final Value value;
	public final boolean indexed;

	Attribute(AttrName name, Value.Type type, Value value, boolean indexed) {
		this.name = name;
		this.type = type;
		this.value = value;
		this.indexed = indexed;
	}
}
