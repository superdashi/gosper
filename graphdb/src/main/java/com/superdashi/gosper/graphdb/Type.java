package com.superdashi.gosper.graphdb;

import java.util.Optional;
import java.util.regex.Pattern;

import com.superdashi.gosper.framework.Namespace;

//TODO rename to PartType?
public final class Type extends Name {

	//TODO this needs to be considered
	private static final Pattern VALID_NAME = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*");

	static void checkTypeName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (name.isEmpty()) throw new IllegalArgumentException("empty name");
		if (!isValidTypeName(name)) throw new IllegalArgumentException("invalid name");
	}

	public static boolean isValidTypeName(String str) {
		if (str == null) throw new IllegalArgumentException("null str");
		return VALID_NAME.matcher(str).matches();
	}

	public static Type create(Namespace namespace, String name) {
		if (namespace == null) throw new IllegalArgumentException("null namespace");
		checkTypeName(name);
		return new Type(namespace, name);
	}

	public static Optional<Type> optionalCreate(Namespace namespace, String name) {
		return namespace == null || name == null || !isValidTypeName(name) ? Optional.empty() : Optional.of(new Type(namespace, name));
	}

	Type(Namespace namespace, String name) {
		super(namespace, name);
	}

}
