package com.superdashi.gosper.graphdb;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.graphdb.Edit;
import com.superdashi.gosper.graphdb.Space;
import com.superdashi.gosper.graphdb.Store;
import com.superdashi.gosper.graphdb.Viewer;

public class RepeatedCommits {

	public static void main(String... args) {
		Namespace ns = new Namespace("www.example.com");
		Identity id = new Identity(ns, "app");

		while (true) {
			Store store = Store.newMemStore();
			Space space = new Space(store);
			Viewer viewer = Viewer.createBuilder(id).addTypeName("Node").build();
			space.associate(viewer);
			space.open();

			int reps = 10000;
			for (int i = 0; i < reps; i++) {
				try (Edit edit = space.view(id).edit()) {
					edit.createNode("Node");
					edit.commit();
				}
			}

			space.close();
		}
	}
}
