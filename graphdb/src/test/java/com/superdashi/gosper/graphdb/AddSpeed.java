package com.superdashi.gosper.graphdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.graphdb.Edit;
import com.superdashi.gosper.graphdb.Space;
import com.superdashi.gosper.graphdb.Store;
import com.superdashi.gosper.graphdb.Viewer;

public class AddSpeed {

	public static void main(String... args) throws IOException {
		Namespace ns = new Namespace("www.example.com");
		Identity id = new Identity(ns, "app");

		Path path = Files.createTempFile("graphdb", ".test");
		//Store store = Store.newMemStore();
		Store store = Store.fileStore(path);
		Space space = new Space(store);
		Viewer viewer = Viewer.createBuilder(id).addTypeName("Node").build();
		space.associate(viewer);
		space.open();

		int adds = 10000;
		int reps = 10;
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < reps; i++) {
			try (Edit edit = space.view(id).edit()) {
				for (int j = 0; j < adds; j++) {
					edit.createNode("Node");
				}
				edit.commit();
			}
		}

		long finishTime = System.currentTimeMillis();
		long timeTaken = finishTime - startTime;
		long size = Files.size(path);
		int totalAdds = reps * adds;
		System.out.format("%d reps in %d ms (1 commit + %d adds = %3.3fms // %3.3fms)%n", reps, timeTaken, adds, (double) timeTaken / reps, (double) timeTaken / totalAdds);
		System.out.format("%d bytes written for %d adds (1 add = %3.3f bytes)", size, totalAdds, (double) size / totalAdds);

		space.close();
	}
}
