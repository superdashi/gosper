package com.superdashi.gosper.framework;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Namespace implements Comparable<Namespace> {

	//TODO path needs to be stricter
	private static final Pattern PATTERN = Pattern.compile("([a-zA-Z0-9][-a-zA-Z0-9]{0,62}(?:\\.[a-zA-Z0-9][-a-zA-Z0-9]{0,62}){0,126})((?:/[^/]+)*)");
	private static final Pattern PREFIX_PATTERN = Pattern.compile("[a-z][a-z0-9]{0,31}");

	private static boolean isHostnameReserved(String hostname) {
		return hostname.toLowerCase().contains(".superdashi.");
	}

	public static Optional<Namespace> maybe(String string) {
		if (string == null) throw new IllegalArgumentException("null string");
		if (string.isEmpty()) return Optional.empty();
		Matcher matcher = PATTERN.matcher(string);
		if (!matcher.matches()) return Optional.empty();
		String hostname = matcher.group(1).toLowerCase();
		if (hostname.length() > 253) return Optional.empty();
		String pathname = matcher.group(2);
		return Optional.of( new Namespace(
				pathname == null ? hostname : hostname + pathname,
				isHostnameReserved(hostname)
		) );
	}

	//TODO remove?
	public static void checkValidNamespacePrefix(String prefix) {
		if (prefix == null) throw new IllegalArgumentException("null prefix");
		if (prefix.isEmpty()) throw new IllegalArgumentException("empty prefix");
		if (!PREFIX_PATTERN.matcher(prefix).matches()) throw new IllegalArgumentException("invalid prefix");
	}

	public static boolean isValidNamespacePrefix(String prefix) {
		return prefix != null && !prefix.isEmpty() && PREFIX_PATTERN.matcher(prefix).matches();
	}

	private final String string;
	private final boolean reserved;

	//TODO replace with static only
	public Namespace(String string) {
		if (string == null) throw new IllegalArgumentException("null string");
		if (string.isEmpty()) throw new IllegalArgumentException("empty string");
		Matcher matcher = PATTERN.matcher(string);
		if (!matcher.matches()) throw new IllegalArgumentException("invalid string");
		String hostname = matcher.group(1).toLowerCase();
		if (hostname.length() > 253) throw new IllegalArgumentException("hostname too long");
		String pathname = matcher.group(2);
		this.string = pathname == null ? hostname : hostname + pathname;
		this.reserved = isHostnameReserved(hostname);
	}

	private Namespace(String string, boolean reserved) {
		this.string = string;
		this.reserved = reserved;
	}

	public boolean isReserved() {
		return reserved;
	}

	@Override
	public int compareTo(Namespace that) {
		return this.string.compareTo(that.string);
	}

	@Override
	public int hashCode() {
		return string.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Namespace)) return false;
		Namespace that = (Namespace) obj;
		return this.string.equals(that.string);
	}

	@Override
	public String toString() {
		return string;
	}

}
