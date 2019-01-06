package com.superdashi.gosper.bundle;

import java.net.URI;
import java.util.HashMap;
import java.util.stream.Stream;

import com.superdashi.gosper.util.DashiUtil;
import com.tomgibara.tries.IndexedTrie;
import com.tomgibara.tries.IndexedTries;
import com.tomgibara.tries.Tries;

public final class BundleFilesMem implements BundleFiles {

	private static final IndexedTries<String> tries = Tries.serialStrings(DashiUtil.UTF8).nodeSource(Tries.sourceForCompactLookups()).indexed();

	public static Builder newBuilder(URI uri) {
		return new Builder(uri);
	}

	public static Builder newBuilder() {
		return new Builder(null);
	}

	public static class Builder {

		private final URI uri;
		private final HashMap<String, BundleFile> files;

		private Builder(URI uri) {
			this.uri = uri;
			files = new HashMap<>();
		}

		private Builder(URI uri, BundleFile... files) {
			this.uri = uri;
			this.files = new HashMap<>(files.length);
			for (BundleFile file : files) {
				this.files.put(file.name(), file);
			}
		}

		public Builder addFile(BundleFile file) {
			if (file == null) throw new IllegalArgumentException("null file");
			String name = file.name();
			BundleFileUtil.checkValidName(name);
			files.put(name, file);
			return this;
		}

		public BundleFilesMem build() {
			IndexedTrie<String> names = tries.newTrie();
			for (String name : files.keySet()) {
				names.add(name);
			}
			names.compactStorage();
			BundleFile[] files = new BundleFile[names.size()];
			for (BundleFile file : this.files.values()) {
				files[names.indexOf(file.name())] = file;
			}
			return new BundleFilesMem(uri, names, files);
		}

	}

	private final URI uri;
	private final IndexedTrie<String> names;
	private final BundleFile[] files;

	private BundleFilesMem(URI uri, IndexedTrie<String> names, BundleFile[] files) {
		//TODO we need something better?
		this.uri = uri == null ? URI.create("mem:/" + System.identityHashCode(this)) : uri;
		this.names = names.immutable();
		this.files = files;
	}

	public Builder buiilder() {
		return new Builder(null, files);
	}

	@Override
	public boolean available() {
		return true;
	}

	@Override
	public URI uri() {
		return uri;
	}

	@Override
	public Stream<String> names() {
		return names.asSet().stream();
	}

	@Override
	public boolean isName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		return names.contains(name);
	}

	@Override
	public BundleFile file(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		int index = names.indexOf(name);
		if (index < 0) throw new IllegalArgumentException("unknown name");
		return files[index];
	}

}
