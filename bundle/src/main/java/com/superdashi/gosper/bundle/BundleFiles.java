package com.superdashi.gosper.bundle;

import java.net.URI;
import java.util.stream.Stream;

public interface BundleFiles {

	boolean available();

	URI uri();

	Stream<String> names();

	boolean isName(String name);

	BundleFile file(String name);

	default boolean isJar() { return false; }
}
