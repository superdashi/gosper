package com.superdashi.gosper.graphdb;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;
import org.h2.mvstore.OffHeapStore;

public final class Store {

	private static final int FILE_PAGE_SPLIT_SIZE = 4096;

	private static Path pathFor(MVStore store) {
		String fileName = store.getFileStore().getFileName();
		if (fileName == null) return null;
		if (fileName.startsWith("nio:")) return Paths.get(fileName.substring(4));
		return null;
	}

	static Store wrap(MVStore store) {
		return new Store(store);
	}

	private static Builder newBuilder() {
		Builder mvBuilder = new MVStore.Builder();
		mvBuilder.autoCommitDisabled();
		mvBuilder.compress();
		return mvBuilder;
	}

	public static Store newMemStore() {
		Builder builder = newBuilder();
		builder.pageSplitSize(FILE_PAGE_SPLIT_SIZE);
		builder.fileStore(new OffHeapStore());
		return new Store(builder.open(), null);
	}

	public static Store fileStore(Path path) {
		Builder builder = newBuilder();
		builder.fileName(path.toString());
		return new Store(builder.open(), path);
	}

	final MVStore store;
	final Path path;

	private Store(MVStore store) {
		this.store = store;
		this.path = pathFor(store);
	}

	private Store(MVStore store, Path path) {
		this.store = store;
		this.path = path;
	}

	public void compact() {
		//TODO
	}
}
