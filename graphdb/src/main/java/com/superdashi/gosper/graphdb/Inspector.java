package com.superdashi.gosper.graphdb;

public interface Inspector {

	AttrName attrName(String name);

	Type type(String typeName);

	Inspect inspect();

}
