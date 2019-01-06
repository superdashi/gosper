package com.superdashi.gosper.bundle;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class BundleFilesDir implements BundleFiles {

	private final Path root;

	private static Stream<Path> expand(Path path) {
		try {
			if (Files.isRegularFile(path)) return Stream.of(path);
			if (Files.isDirectory(path)) return Files.list(path).flatMap(BundleFilesDir::expand);
		} catch (IOException e) {
			//TODO log this properly
			e.printStackTrace();
		}
		return Stream.empty();
	}

	public BundleFilesDir(Path root) {
		if (root == null) throw new IllegalArgumentException("null root");
		this.root = root;
	}

	@Override
	public boolean available() {
		return Files.isDirectory(root);
	}

	@Override
	public URI uri() {
		return root.toUri();
	}

	@Override
	public Stream<String> names() {
		return expand(root).map(root::relativize).map(Path::toString);
	}

	@Override
	public boolean isName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		Path path = root.resolve(name);
		return Files.isRegularFile(path);
	}

	@Override
	public BundleFile file(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		//TODO should check for valid name here?
		Path path = root.resolve(name);
		// file may exist in future, so we return one, and don't check if exists now
		return BundleFileUtil.newSystemFile(name, path);
	}
}
