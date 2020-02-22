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

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;

public class RepeatedCommits {

	public static void main(String... args) {
		Namespace ns = new Namespace("www.example.com");
		Identity id = new Identity(ns, "app");

		while (true) {
			Store store = Store.newMemStore();
			Space space = new Space(store);
			Viewer viewer = Viewer.createBuilder(id).addType("Node").build();
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
