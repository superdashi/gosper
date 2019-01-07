/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
