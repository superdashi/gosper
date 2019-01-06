package com.superdashi.gosper.bundle;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;

public final class BundleFilesJar implements BundleFiles {

	private static final String NAME_PREFIX = "dashi/";

	private static boolean isValidEntry(JarEntry entry) {
		return entry != null && !entry.isDirectory() && entry.getName().startsWith(NAME_PREFIX);
	}

	private final Path path;
	private JarFile jar;

	public BundleFilesJar(Path path) {
		if (path == null) throw new IllegalArgumentException("null path");
		this.path = path;
	}

	@Override
	public boolean available() {
		ensureJar();
		return jar != null;
	}

	@Override
	public URI uri() {
		return path.toUri();
	}

	@Override
	public Stream<String> names() {
		ensureJar();
		checkAvailable();
		return jar.stream().filter(e -> !e.isDirectory()).map(e -> e.getName()).filter(n -> n.startsWith(NAME_PREFIX)).map(n -> n.substring(NAME_PREFIX.length()));
	}

	@Override
	public boolean isName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		return isValidEntry( jar.getJarEntry(NAME_PREFIX + name) );
	}

	@Override
	public BundleFile file(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		JarEntry entry = jar.getJarEntry(NAME_PREFIX + name);
		if (!isValidEntry(entry)) throw new IllegalArgumentException("invalid name");
		return new BundleFile() {
			@Override public String name() { return name; }
			@Override public ReadStream openAsReadStream() throws IOException {
				return Streams.streamInput(jar.getInputStream(entry));
			}
		};
	}

	@Override
	public boolean isJar() {
		return true;
	}

	private void ensureJar() {
		if (jar == null) try {
			jar = new JarFile(path.toFile());
		} catch (IOException e) {
			//TODO log
			e.printStackTrace();
		}
	}

	private void checkAvailable() {
		if (jar == null) throw new IllegalStateException("not available");
	}

}
