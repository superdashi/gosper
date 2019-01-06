package com.superdashi.gosper.framework;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class Details {

	private static final Pattern PATTERN_ROLE = Pattern.compile("[a-zA-Z][a-zA-Z0-9_-]*");

	public static Details typeAndIdentity(Type type, Identity identity) {
		if (type == null) throw new IllegalArgumentException("null type");
		if (identity == null) throw new IllegalArgumentException("null identity");
		return new Details(type, identity, null);
	}

	public static Details typeIdentityAndRole(Type type, Identity identity, String role) {
		if (type == null) throw new IllegalArgumentException("null type");
		if (identity == null) throw new IllegalArgumentException("null identity");
		if (role == null) throw new IllegalArgumentException("null role");
		if (role.isEmpty()) throw new IllegalArgumentException("empty role");
		if (!PATTERN_ROLE.matcher(role).matches()) throw new IllegalArgumentException("invalid role");
		return new Details(type, identity, role);
	}

	private final Type type;
	private final Identity identity;
	private final String role;

	private Details(Type type, Identity identity, String role) {
		this.type = type;
		this.identity = identity;
		this.role = role;
	}

	public Type type() { return type; }
	public Identity identity() { return identity; }
	public Optional<String> role() { return Optional.ofNullable(role); }

	@Override
	public int hashCode() {
		return type.hashCode() + Objects.hashCode(role);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Details)) return false;
		Details that = (Details) obj;
		if (!this.type.equals(that.type)) return false;
		if (!this.identity.equals(that.identity)) return false;
		if (!Objects.equals(this.role, that.role)) return false;
		return true;
	}

	@Override
	public String toString() {
		return role == null ?
				type.toString() + '#' + identity :
				type.toString() + '#' + identity + '%' + role;
	}

}