package com.superdashi.gosper.graphdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.h2.mvstore.MVStoreTool;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.graphdb.Edit;
import com.superdashi.gosper.graphdb.PartRef;
import com.superdashi.gosper.graphdb.Space;
import com.superdashi.gosper.graphdb.Store;
import com.superdashi.gosper.graphdb.Viewer;
import com.superdashi.gosper.item.Value;

public class Compaction {

	public static void main(String... args) throws IOException {
		Namespace ns = new Namespace("www.example.com");
		Identity id = new Identity(ns, "app");

		Path path = Files.createTempFile("graphdb", ".test");
		int modifies = 1000;
		Consumer<String> reportSize = msg -> {
			System.out.println(msg);
			long size;
			try {
				size = Files.size(path);
			} catch (IOException e) {
				size = -1L;
			}
			System.out.format("%d bytes written for %d modifies (1 modify = %3.3f bytes)%n", size, modifies, (double) size / modifies);
		};

		//Store store = Store.newMemStore();
		Store store = Store.fileStore(path);
		Space space = new Space(store);
		Viewer viewer = Viewer.createBuilder(id).addTypeName("Node").addAttribute("index", Value.Type.INTEGER, Value.empty(), false).build();
		space.associate(viewer);
		space.open();

		reportSize.accept("after open");

		PartRef ref;
		try (Edit edit = space.view(id).edit()) {
			ref = edit.createNode("Node").ref();
			edit.commit();
		}

		reportSize.accept("after create");

		for (int j = 0; j < modifies; j++) {
			try (Edit edit = space.view(id).edit()) {
				edit.node(ref).get().attrs().integer("index", j);
				edit.commit();
			}
		}

		reportSize.accept("after modifies");

		space.store.compactMoveChunks();

		reportSize.accept("after compact rewrite");

		space.close();
		store.store.close();

		reportSize.accept("after close");

		MVStoreTool.compact(path.toString(), true);

		reportSize.accept("after compact");

	}

}
