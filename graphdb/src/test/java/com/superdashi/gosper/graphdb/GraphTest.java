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

import static com.superdashi.gosper.graphdb.Selector.ofType;
import static com.superdashi.gosper.graphdb.Selector.ownedBy;
import static com.superdashi.gosper.graphdb.Selector.withAttr;
import static com.superdashi.gosper.graphdb.Selector.withTag;
import static com.superdashi.gosper.graphdb.Selector.withValue;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.superdashi.gosper.item.Item;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;
import org.h2.mvstore.OffHeapStore;
import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Value;
import com.tomgibara.storage.Stores;

public class GraphTest {

	private static Namespace ns1 = new Namespace("www.superdashi.com");
	private static Namespace ns2 = new Namespace("www.example.com");
	private static Namespace ns3 = new Namespace("www.example.com/other");

	private static Identity id1 = new Identity(ns1, "app");
	private static Identity id2 = new Identity(ns2, "app");
	private static Identity id3 = new Identity(ns3, "app");

	private static Viewer.Builder stdBuild(Viewer.Builder builder) {
		return builder
			.addType("Company")
			.addType("Person")
			.addType("Employee")
			.addType("employs")
			.addType("likes")
			.addType("Pet", Item.newBuilder()
					.label("The pet called {v1:name}")
					.addExtra("gosper:interpolate", Value.ofString("label,v1:also"))
					.addExtra("v1:also", Value.ofString("Also know that the pet {v1:quirk}"))
					.build())
			.addType("is_employed_by")
			.addAttribute("name", Value.Type.STRING, Value.empty(), false)
			.addAttribute("count", Value.Type.INTEGER, Value.empty(), false)
			.addAttribute("index", Value.Type.INTEGER, Value.empty(), true)
			.addAttribute("random", Value.Type.NUMBER, Value.empty(), false)
			.addAttribute("defaulted", Value.Type.NUMBER, Value.ofString("12"), false)
			.addPrefix("v1", ns1)
			.addPrefix("v2", ns2)
			.addPrefix("v3", ns3)
			;
	}

	//TODO where to make types
	Type cmpType = new Type(ns1, "Company");
	Type empType = new Type(ns1, "Employee");
	Type perType = new Type(ns1, "Person");
	Type wrkType = new Type(ns1, "employs");
	Type iebType = new Type(ns1, "is_employed_by");
	Tag managerTag = new Tag(ns1, "manager");

	Viewer stdViewer1 = stdBuild( Viewer.createBuilder(id1) ).addDeclaredPermissions("pv","pm","pd","pn").build();
	Viewer stdViewer2 = stdBuild( Viewer.createBuilder(id2) ).addGrantedPermission(new Identity(ns1, "pv")).build();
	Viewer stdViewer3 = stdBuild( Viewer.createBuilder(id3) ).addGrantedPermissions(new Identity(ns1, "pv"), new Identity(ns1, "pm"), new Identity(ns1, "pd")).build();

	static enum SType {
		MEMORY,
		FILE,
		OFF_HEAP
	}

	static class Config {
		final SType sType;
		final boolean reuseStore;
		Config(SType sType, boolean reuseStore) {
			this.sType = sType;
			this.reuseStore = reuseStore;
		}
	}

	final Config[] configs = {
			/*new Config(SType.MEMORY, true),
			new Config(SType.MEMORY, false),*/
			new Config(SType.OFF_HEAP, true),
			new Config(SType.OFF_HEAP, false)/*,
			new Config(SType.FILE, true),
			new Config(SType.FILE, false)*/
		};


	@Test
	public void testAttrs() throws IOException {
		runTest(
			visit -> {
				Node cmp = visit.createNode();
				cmp.type(cmpType);
				cmp.attrs().string("name", "Dashi");
				cmp.attrs().integer("count", 1);
				try {
					cmp.attrs().string("index", "NaN");
					Assert.fail();
				} catch (ConstraintException e) {
					// expected //
				}
				cmp.attrs().string("index", "3");
			},
			visit -> {
				Graph graph = visit.graph();
				Node cmp = graph.nodes().iterator().next();
				Assert.assertEquals("Dashi", cmp.attrs().string("name"));
				Assert.assertEquals(1, cmp.attrs().integer("count"));
				Assert.assertEquals(3, cmp.attrs().integer("index"));
				Assert.assertTrue(graph.nodes(withAttr("count")).toSet().contains(cmp));
				Assert.assertTrue(graph.nodes(withValue("count", Value.ofInteger(1))).toSet().contains(cmp));
				Assert.assertTrue(graph.nodes(withValue("count", Value.ofInteger(2))).toSet().isEmpty());
				Assert.assertTrue(graph.nodes(withValue("index", Value.ofInteger(3))).toSet().contains(cmp));
				Assert.assertTrue(graph.nodes(withValue("index", Value.ofInteger(4))).toSet().isEmpty());
				Assert.assertTrue(graph.nodes(withAttr("name")).toSet().contains(cmp));
				Assert.assertTrue(graph.nodes(withAttr("nothing")).toSet().isEmpty());
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testDefault() throws IOException {
		runTest(
			visit -> {
				Node c1 = visit.createNode(cmpType);
				c1.tags().add("A");
				c1.attrs().integer("defaulted", 1);
				Node c2 = visit.createNode(cmpType);
				c2.tags().add("B");
			},
			visit -> {
				Graph graph = visit.graph();
				Node c1 = graph.nodes(Selector.withTag("A")).unique();
				Node c2 = graph.nodes(Selector.withTag("B")).unique();
				Assert.assertEquals(1, c1.attrs().integer("defaulted"));
				Assert.assertEquals(12, c2.attrs().integer("defaulted"));
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testTrivial() throws IOException {
		runTest(
			visit -> {
			},
			visit -> {
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testBasic() throws IOException {
		runTest(
			visit -> {
				Node cmp = visit.createNode();
				cmp.type(cmpType);
				cmp.attrs().string("name", "Dashi");
				Node emp = visit.createNode();
				emp.type(empType);
				emp.attrs().string("name", "Tom");
				emp.tags().add(managerTag);
				Edge edg = visit.createEdge(cmp, emp, wrkType);
				edg.tags().add(managerTag);
				edg.attrs().set("index", Value.ofInteger(34));
			},
			visit -> {
				Graph graph = visit.graph();
				Node emp = graph.nodes(ofType(empType)).unique();
				Assert.assertEquals(empType, emp.type());
				Assert.assertEquals("Tom", emp.attrs().string("name"));
				Assert.assertTrue( emp.tags().contains(managerTag));
				Assert.assertFalse( graph.edgesTo(emp).empty() );
				Assert.assertEquals(1, graph.edgesTo(emp).count() );

				Node cmp = graph.nodes(ofType(cmpType)).unique();
				Assert.assertEquals(cmpType, cmp.type());
				Assert.assertEquals("Dashi", cmp.attrs().string("name"));
				Assert.assertTrue( graph.edgesTo(cmp).empty() );
				Assert.assertEquals(0, graph.edgesTo(cmp).count() );

				Assert.assertEquals(1, graph.edgesFrom(cmp).count());
				Edge edg = graph.edgesFrom(cmp).cursor().unique();
				Assert.assertEquals(cmp, edg.source());
				Assert.assertEquals(emp, edg.target());
				Assert.assertEquals(wrkType, edg.type());
				Assert.assertEquals(edg, graph.edges(ofType("employs")).unique());
				Assert.assertEquals(edg, graph.edges(withTag("manager")).unique());
				Assert.assertEquals(edg, graph.edges(withValue("index", Value.ofInteger(34))).unique());
				Assert.assertTrue(graph.edges(withValue("index", Value.ofInteger(24))).empty());

				Assert.assertEquals(wrkType, graph.edgesTo(emp).cursor().iterator().next().type());
				Assert.assertTrue(graph.nodes(ownedBy(id1)).toSet().contains(emp));
				Assert.assertTrue(graph.nodes(ownedBy(id1)).toSet().contains(cmp));
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testEmptyTypeCursor() throws IOException {
		runTest(
				visit -> {
				},
				visit -> {
					Assert.assertFalse(visit.graph().types().iterator().hasNext());
				},
				id1,
				stdViewer1
				);
	}

	@Test
	public void testSingleTypeCursor() throws IOException {
		Function<Integer, Consumer<Edit>> builderProducer = n -> {
			return edit -> {
				for (int i = 0; i < n; i++) {
					edit.createNode(cmpType);
				}
			};
		};
		Consumer<Inspect> verifier = visit -> {
			TypeCursor cursor = visit.graph().types();
			Assert.assertEquals(1, cursor.stream().count());
			Assert.assertEquals(cmpType, cursor.stream().findFirst().get());
		};
		for (int i = 1; i < 10; i++) {
			runTest(builderProducer.apply(i), verifier, id1, stdViewer1);
		}
	}

	@Test
	public void testDoubleTypeCursor() throws IOException {
		Random r = new Random(0L);
		Function<Integer, Consumer<Edit>> builderProducer = n -> {
			return edit -> {
				for (int i = 0; i < n; i++) {
					edit.createNode(r.nextBoolean() ? cmpType : empType);
				}
			};
		};
		Consumer<Inspect> verifier = visit -> {
			Set<Type> expected = visit.graph().nodes().stream().map(Part::type).collect(toSet());
			Assert.assertEquals(expected.size(), visit.graph().types().stream().count());
			Assert.assertEquals(expected, visit.graph().types().toSet());
		};
		for (int i = 1; i < 10; i++) {
			runTest(builderProducer.apply(i), verifier, id1, stdViewer1);
		}
	}

	@Test
	public void testTypesInNamespace() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		{
			View view = buildView(mvBuilder, id1, stdViewer1);
			try (Edit edit = view.edit()) {
				{
					Node node = edit.createNode("Company");
					node.attrs().string("name", "SomeCo");
					node.permissions().allowViewWith("pv");
				}
				{
					Node node = edit.createNode("Person");
					node.attrs().string("name", "AnneOther");
				}
				edit.commit();
			}
		}

		{
			View view = buildView(mvBuilder, id2, stdViewer2);
			try (Edit edit = view.edit()) {
				Node node = edit.createNode("Employee");
				node.attrs().string("name", "Anon");
				edit.commit();
			}

			try (Inspect inspect = view.inspect()) {
				Set<Type> types = inspect.graph().types().toSet();
				Assert.assertEquals(2, types.size());
				Assert.assertTrue(types.contains( new Type(ns1, "Company") ));
				Assert.assertTrue(types.contains( new Type(ns2, "Employee") ));
			}
		}


//		runTest(edit -> {
//			edit.createNode("v1:Company");
//			edit.createNode("v1:Employee");
//			edit.createNode("v2:Person");
//		}, inspect -> {
//			Graph graph = inspect.graph();
//			Assert.assertEquals(2, graph.typesWithNamespace(ns1).count());
//		}, id1, stdViewer1);
	}

	@Test
	public void testDelete() throws IOException {
		runTest(
			visit -> {
				Graph graph = visit.graph();
				Node cmp = visit.createNode("Company");
				cmp.attrs().string("name", "Dashi");
				Node emp = visit.createNode("Employee");
				emp.attrs().string("name", "Tom");
				emp.attrs().integer("index", 55);
				visit.createEdge(cmp, emp, wrkType);
				emp.tags().add(managerTag);
				Assert.assertEquals(1, graph.nodes(ofType(empType)).count());
				Assert.assertEquals(1, graph.nodes(withTag(managerTag)).count());
				Assert.assertTrue(graph.nodes(withValue("index", Value.ofInteger(55))).toSet().contains(emp));

				emp.delete();
				expectIAE(() -> graph.edgesFrom(cmp).add(emp, wrkType));
				expectIAE(() -> graph.edgesTo(emp).add(cmp, wrkType));
				expectISE(() -> emp.tags().add("no!!"));
			},
			visit -> {
				Graph graph = visit.graph();
				Assert.assertEquals(1, graph.nodes().count());
				Node cmp = graph.nodes(ofType("v1:Company")).unique();
				Assert.assertTrue(graph.edgesFrom(cmp).empty());
				Assert.assertEquals(0, graph.nodes(ofType("v1:Employee")).count());
				Assert.assertEquals(0, graph.nodes(withTag(managerTag)).count());
				Assert.assertEquals(0, graph.nodes(withValue("v1:index", Value.ofInteger(55))).count());
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testRing() throws IOException {
		int spokeCount = 100;
		runTest(
			visit -> {
				Graph graph = visit.graph();
				Node cmp = visit.createNode();
				cmp.type(cmpType);
				cmp.attrs().string("name", "Dashi");
				for (int i = 0; i < spokeCount; i++) {
					Assert.assertEquals(graph.edgesTo(cmp).count(), i);
					Node emp = visit.createNode();
					emp.type(empType);
					emp.attrs().string("name", "Employee " + i);
					emp.attrs().integer("index", i);
					visit.createEdge(emp, cmp, iebType);
				}
			},
			visit -> {
				Graph graph = visit.graph();
				Node cmp = graph.nodes(ofType(cmpType)).iterator().next();
				Assert.assertEquals(spokeCount, graph.edgesTo(cmp).count());
				Set<Integer> set = IntStream.range(0, 100).mapToObj(Integer::valueOf).collect(Collectors.toSet());
				graph.edgesTo(cmp).cursor().iterator().forEachRemaining(e -> Assert.assertTrue(set.remove((int) e.source().attrs().integer("index"))));
				for (int i = 0; i < spokeCount; i++) {
					Node ni = graph.nodes(withValue("index", Value.ofInteger(i))).unique();
					Assert.assertEquals((long) i, ni.attrs().integer("index"));
				}
			},
			id1,
				stdViewer1
			);
	}

	@Test
	public void testPermissions() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view1 = buildView(mvBuilder, id1, stdViewer1, stdViewer2);
		try (Edit edit = view1.edit()) {
			Node cmp = edit.createNode("Company");
			cmp.permissions().allowViewWith("pv");
			expectIAE(() -> cmp.permissions().allowViewWith("px"));
			Node emp = edit.createNode("Employee");

			Node a = edit.createNode("Person");
			Node b = edit.createNode("Person");
			edit.createEdge(a, b, "likes").permissions().allowViewWith("pv");

			edit.commit();
		}
		View view2 = buildView(mvBuilder, id2, stdViewer1, stdViewer2);
		try (Visit visit = view2.inspect()) {
			Assert.assertEquals(1, visit.graph().nodes().count());
			Assert.assertEquals(1, visit.graph().edges().count());
			Node node = visit.graph().nodes().unique();
			Edge edge = visit.graph().edges().unique();
			Assert.assertNull(edge.source());
			Assert.assertNull(edge.target());
			expectISE(() -> node.attrs().integer("index", 1));
			expectISE(() -> node.tags().add("nope"));
			expectISE(() -> edge.delete());
			Assert.assertEquals(Collections.singleton(node.type()), visit.graph().types().stream().collect(Collectors.toSet()));
		}
	}

	@Test
	public void testModifiable() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view1 = buildView(mvBuilder, id1, stdViewer1, stdViewer2, stdViewer3);
		try (Edit edit = view1.edit()) {
			Node cmp = edit.createNode("Company");
			cmp.permissions().allowViewWith("pv");
			cmp.permissions().allowModifyWith("pm");
			cmp.tags().add("atag");
			cmp.attrs().integer("index", 5);
			Node emp = edit.createNode("Employee");
			emp.permissions().allowViewWith("pv");
			emp.attrs().integer("index", 3);
			emp.tags().add("btag");
			edit.commit();
		}
		View view2 = buildView(mvBuilder, id3, stdViewer1, stdViewer2, stdViewer3);
		try (Visit visit = view2.edit()) {
			Node cmp = visit.graph().nodes(ofType(cmpType)).unique();
			Node emp = visit.graph().nodes(ofType(empType)).unique();

			cmp.attrs().integer("index", 1);
			cmp.tags().add("yep");
			expectISE(() -> cmp.delete());

			expectISE(() -> emp.attrs().integer("index", 1));
			expectISE(() -> emp.tags().add("nope"));
			expectISE(() -> emp.delete());
		}
	}

	@Test
	public void testConcurrent() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);

		try (Edit edit = view.edit()) {
			Node cmp = edit.createNode("Company");
			edit.commit();
		}

		Visit visit1 = view.inspect();

		try (Edit edit = view.edit()) {
			Node emp = edit.createNode("Employee");
			edit.commit();
		}

		Assert.assertEquals(1, visit1.graph().nodes(Selector.any()).count());
		Visit visit2 = view.inspect();
		Assert.assertTrue(visit1.graph().nodes(ofType("Company")).unique().attrs().get("name").isEmpty());

		try (Edit edit = view.edit()) {
			edit.graph().nodes(ofType("Company")).unique().attrs().string("name", "Dashi");
			edit.commit();
		}

		Visit visit3 = view.inspect();
		Assert.assertTrue(visit1.graph().nodes(ofType("Company")).unique().attrs().get("name").isEmpty());
		Assert.assertTrue(visit2.graph().nodes(ofType("Company")).unique().attrs().get("name").isEmpty());
		Assert.assertEquals("Dashi", visit3.graph().nodes(ofType("Company")).unique().attrs().string("name"));
		visit1.close();
		visit2.close();
		visit3.close();
	}

	@Test
	public void testDeletability() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view1 = buildView(mvBuilder, id1, stdViewer1, stdViewer2, stdViewer3);
		try (Edit edit = view1.edit()) {
			Node cmp1 = edit.createNode("Company");
			cmp1.permissions().allowViewWith("pv");
			cmp1.permissions().allowDeleteWith("pd");
			cmp1.tags().add("atag");
			cmp1.attrs().integer("index", 5);

			Node cmp2 = edit.createNode("Company");
			cmp2.permissions().allowViewWith("pv");
			cmp2.permissions().allowDeleteWith("pd");
			cmp2.tags().add("btag");
			cmp2.attrs().integer("index", 5);

			Node emp = edit.createNode("Employee");
			emp.permissions().allowViewWith("pv");
			emp.attrs().integer("index", 3);
			emp.tags().add("ctag");

			edit.createEdge(emp, cmp2, wrkType);
			edit.commit();
		}
		View view2 = buildView(mvBuilder, id3, stdViewer1, stdViewer2, stdViewer3);
		try (Visit visit = view2.edit()) {
			Node cmp1 = visit.graph().nodes(ofType(cmpType).and(withTag(new Tag(ns1, "atag")))).first().get();
			cmp1.delete();
			expectISE(() -> cmp1.delete());

			Node emp = visit.graph().nodes(ofType(empType)).unique();
			expectISE(() -> emp.delete());

			Node cmp2 = visit.graph().nodes(ofType(cmpType).and(withTag(new Tag(ns1, "btag")))).unique();
			expectISE(() -> cmp2.delete());
		}
	}

	@Test
	public void testMutability() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);
		{
			Edit edit = view.edit();
			Node cmp = edit.createNode("Company");
			Node emp = edit.createNode("Employee");
			Edge edg = edit.createEdge(cmp, emp, "employs");
			edg.attrs().string("type", "fulltime");
			cmp.attrs().string("name", "Dashi");
			cmp.tags().add("primary");
			Node oth = edit.createNode("Employee");
			oth.tags().add("new");
			edit.commit();
		}
		view.space.close();
		view.space.store.close();
		view = buildView(mvBuilder, id1, stdViewer1);
		{
			Inspect inspect = view.inspect();
			Graph graph = inspect.graph();
			Node cmp = graph.nodes(ofType("Company")).unique();
			Node oth = graph.nodes(withTag("new")).unique();
			expectISE(() -> cmp.type("Employee"));
			expectISE(() -> cmp.attrs().string("name", "Other"));
			expectISE(() -> cmp.attrs().string("new", "value"));
			expectISE(() -> cmp.tags().add("secondary"));
			expectISE(() -> cmp.tags().remove("primary"));
			//expectISE(() -> cmp.edges().asList().get(0).remove());
		}
	}

	@Test
	public void testLateApproach() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);
		Space space = view.space;
		try (Edit edit = space.view(id1).edit()) {
			edit.createNode("Company");
		}
		expectIAE(() -> space.view(id2));
		space.associate(stdViewer2);
		try (Edit edit = space.view(id2).edit()) {
			edit.createNode("Company");
		}
		space.close();
	}

	@Test
	public void testVersion() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);
		final long v0;
		try (Inspect inspect = view.inspect()) {
			v0 = inspect.version();
		}
		final long v1;
		try (Inspect inspect = view.inspect()) {
			v1 = inspect.version();
		}
		Assert.assertEquals(v0, v1);
		final long v2;
		try (Edit edit = view.edit()) {
			v2 = edit.version();
		}
		Assert.assertEquals(v1, v2);
		final long v3;
		try (Edit edit = view.edit()) {
			v3 = edit.version();
			edit.commit();
		}
		Assert.assertEquals(v2, v3);
		final long v4;
		try (Inspect inspect = view.inspect()) {
			v4 = inspect.version();
		}
		Assert.assertTrue(v3 <= v4);
		final long v5;
		try (Edit edit = view.edit()) {
			v5 = edit.version();
			edit.createNode("Company");
			edit.commit();
			Assert.assertEquals(v5, edit.version());
		}
		Assert.assertEquals(v4, v5);
		final long v6;
		try (Inspect inspect = view.inspect()) {
			v6 = inspect.version();
		}
		Assert.assertTrue(v5 < v6);
	}

	@Test
	public void testRestriction() {
		runTest(
				visit -> {
					Node e1 = visit.createNode(empType);
					Node e2 = visit.createNode(empType);
					Node e3 = visit.createNode(empType);
					Node p1 = visit.createNode(perType);
					Node p2 = visit.createNode(perType);
					visit.createEdge(e1, e2, "likes");
					visit.createEdge(e1, e3, "likes");
					visit.createEdge(p1, p2, "likes");
					visit.createEdge(e3, p1, "likes");
				},
				visit -> {
					Graph graph = visit.graph().restrictionToNodes(ofType("Employee"));
					Assert.assertEquals(3, graph.nodes().count());
					Assert.assertEquals(2, graph.edges().count());
					//graph.edges().unique();
				},
				id1,
				stdViewer1
				);
	}

	@Test
	public void testRebuildIndex() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		Viewer a = Viewer.createBuilder(id1).addAttribute("rating", Value.Type.STRING, Value.empty(), true).addType("Film").build();
		Viewer b = Viewer.createBuilder(id1).addAttribute("rating", Value.Type.NUMBER, Value.empty(), true).addType("Film").build();
		View view = buildView(mvBuilder, id1, a);
		Edit edit = view.edit();
		Node filmX = edit.createNode("Film");
		Node filmY = edit.createNode("Film");
		Node filmZ = edit.createNode("Film");

		filmX.attrs().string("name", "Bride of Frankenstein");
		filmY.attrs().string("name", "The Old Dark House");
		filmZ.attrs().string("name", "The Invisible Man");

		filmY.attrs().set("rating", Value.ofString("1.0"));
		filmZ.attrs().set("rating", Value.ofString("3 Stars"));

		Graph graph = edit.graph();
		Assert.assertEquals(filmY, graph.nodes(withValue("rating", Value.ofString("1.0"))).unique());
		Assert.assertEquals(filmZ, graph.nodes(withValue("rating", Value.ofString("3 Stars"))).unique());
		Assert.assertEquals(2, graph.nodes(withAttr("rating")).count());

		view.space.close();
		view.space.store.close();
		view = buildView(mvBuilder, id1, b);

		edit = view.edit();
		graph = edit.graph();
		filmX = graph.nodes(withValue("name", Value.ofString("Bride of Frankenstein"))).unique();
		filmY = graph.nodes(withValue("name", Value.ofString("The Old Dark House"))).unique();
		filmZ = graph.nodes(withValue("name", Value.ofString("The Invisible Man"))).unique();

		Assert.assertEquals(filmY, graph.nodes(withValue("rating", Value.ofNumber(1.0))).unique());
		Assert.assertTrue(graph.nodes(withValue("rating", Value.ofString("3 Stars"))).toSet().isEmpty());
		Assert.assertEquals(1, graph.nodes(withAttr("rating")).toList().size());
	}

	@Test
	public void testNodeCursorLogic() {
		runTest(
				visit -> {
					Node scott = visit.createNode("Person");
					Node ramona = visit.createNode("Employee");
					Node knives = visit.createNode("Person");
					Node wallace = visit.createNode("Employee");

					scott.attrs().string("name", "Scott Pilgrim");
					ramona.attrs().string("name", "Ramona Flowers");
					knives.attrs().string("name", "Knives Chao");
					wallace.attrs().string("name", "Wallace Wells");

					scott.attrs().integer("index", 5);
					ramona.attrs().integer("index", 5);
					ramona.tags().add("hammer");
					knives.tags().add("knives");
				},
				visit -> {
					Graph graph = visit.graph();
					Assert.assertEquals("Scott Pilgrim", graph.nodes(ofType("Person")).intersect(graph.nodes(withValue("index", Value.ofInteger(5)))).unique().attrs().string("name"));
					Assert.assertEquals(3, graph.nodes(ofType("Person")).union(graph.nodes(withValue("index", Value.ofInteger(5)))).count());
				},
				id1,
				stdViewer1
				);
	}


	@Test
	public void testReentrantVisit() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);
		try (Edit edit = view.edit()) {
			expectCE(() -> view.edit());
			Visit inspect = view.inspect();
			Node cmp = edit.createNode("Company");
			edit.flush();
			Assert.assertTrue( inspect.graph().nodes().empty() );
		}
	}

	@Test
	public void testBasicObservation() {
		Builder mvBuilder = builder(SType.OFF_HEAP);
		View view = buildView(mvBuilder, id1, stdViewer1);
		List<PartRef> refs = new ArrayList<>();
		Observation obs = view.observe(Selector.ofType(cmpType), refs::add);
		try (Edit edit = view.edit()) {
			edit.createNode(cmpType).attrs().string("name", "Dashi");
			edit.createNode(empType);
			edit.commit();
		}
		try (Visit visit = view.edit()) {
			obs.deliver();
			Assert.assertEquals(1, refs.size());
			PartRef ref = refs.get(0);
			Assert.assertEquals("Dashi", visit.part(ref).get().attrs().string("name"));
		}
	}

	@Test
	public void testEmptyCommit() {
		Builder mvBuilder = builder(SType.FILE);
		View view = buildView(mvBuilder, id1, stdViewer1);
		view.space.close();
		String fileName = view.space.store.getFileStore().getFileName();
		Assert.assertTrue(fileName.startsWith("nio:"));
		fileName = fileName.substring(4);
		File file = new File(fileName);
		long startSize = file.length();
		System.out.println("start size " + startSize);
		Edit edit = view.edit();
		boolean changed = edit.commit();
		Assert.assertFalse(changed);
		Assert.assertEquals(startSize, file.length()); // check file didn't grow
		view.space.close();
		Assert.assertEquals(startSize, file.length()); // confirm no write required on shutdown
		System.out.println("finish size " + file.length());
	}

	@Test
	public void testBasicOrdering() {
		int count = 1000;
		int[] orderedInts = IntStream.range(0, count).toArray();
		String[] orderedStrs = IntStream.range(0, count).mapToObj(Integer::toString).sorted().toArray(String[]::new);
		runTest(
				edit -> {
					// insert in random order
					int[] values = orderedInts.clone();
					Collections.shuffle(Stores.ints(values).asList());
					for (int i = 0; i < 1000; i++) {
						Node node = edit.createNode("Company");
						node.attrs().integer("index", values[i]);
					}
				},
				inspect -> {
					NodeCursor nodes = inspect.graph().nodes();
					{
						List<Node> list = nodes.toList();
						boolean sorted = true;
						for (int i = 0; i < 1000; i++) {
							if (list.get(i).attrs().integer("index") != i) {
								sorted = false;
								break;
							}
						}
						if (sorted) Assert.fail("Naturally sorted!");
					}
					{
						List<Node> list = nodes.order(Order.byAttr("index")).toList();
						for (int i = 0; i < 1000; i++) {
							Assert.assertEquals(orderedInts[i], list.get(i).attrs().integer("index"));
						}
					}
					{
						List<Node> list = nodes.order(Order.byAttr("index", Value.Type.STRING.order())).toList();
						for (int i = 0; i < 1000; i++) {
							Assert.assertEquals(orderedStrs[i], list.get(i).attrs().string("index"));
						}
					}
				},
				id1,
				stdViewer1
				);
	}

	@Test
	public void testItemization() {
		runTest(edit -> {
			{
				Node node = edit.createNode("Pet");
				node.attrs().string("name", "Fido");
				node.attrs().string("quirk", "eats ants");
				node.tags().add("a");
			}
			{
				Node node = edit.createNode("Pet");
				node.tags().add("b");
			}
		}, inspect -> {
			BiFunction<String, String, String> valueForTag = (t,p) -> inspect.graph().nodes(Selector.withTag(t)).unique().asItem().value(p).string();
			//Function<String, String> labelForTag = t -> inspect.graph().nodes(Selector.withTag(t)).unique().asItem().label().get();
			Assert.assertEquals("The pet called Fido", valueForTag.apply("a", "label"));
			Assert.assertEquals("The pet called ", valueForTag.apply("b","label"));
			Assert.assertEquals("Also know that the pet eats ants", valueForTag.apply("a", "v1:also"));
			Assert.assertEquals("Also know that the pet ", valueForTag.apply("b", "v1:also"));
		}, id1, stdViewer1);
	}
	//@Test
	public void testLarge() {
		int cmpCount = 1000;
		int empCount = 1000;
		int edgeCount = 2000;
		Builder mvBuilder = builder(SType.FILE);
		View view = buildView(mvBuilder, id1, stdViewer1);
		view.space.close();
		String fileName = view.space.store.getFileStore().getFileName();
		Assert.assertTrue(fileName.startsWith("nio:"));
		fileName = fileName.substring(4);
		File file = new File(fileName);
		long startSize = file.length();
		System.out.println("start size " + startSize);
		{
			Edit edit = view.edit();
			Graph graph = edit.graph();
			Random r = new Random(0L);
			String[] cmpNames = {"Cafebook", "Ooggle", "Paple", "Zamano", "Kiwipedia", "Daibu", "Beay", "Baotao", "Touyube", "Shadi"};
			String[] firstNames = {"Anna", "Bert", "Clara", "Diana", "Eric", "Florence", "Gary", "Henrietta", "Ivan", "Joanna"};
			String[] lastNames = {"Smith", "Jones", "Taylor", "Brown", "Williams", "Wilson", "Johnson", "Davies", "Robinson", "Wright"};
			String[] colors = {"red", "orange", "yellow", "green", "blue", "indigo", "violet"};
			// make companies
			Node[] cmps = new Node[cmpCount];
			for (int i = 0; i < cmpCount; i++) {
				Node cmp = edit.createNode(cmpType);
				cmp.attrs().string("name", cmpNames[r.nextInt(cmpNames.length)]);
				cmp.tags().add(colors[r.nextInt(colors.length)]);
				cmp.attrs().number("random", r.nextDouble());
				cmps[i] = cmp;
			}
			// make employees
			Node[] emps = new Node[empCount];
			for (int i = 0; i < empCount; i++) {
				Node emp = edit.createNode(cmpType);
				emp.attrs().string("name", firstNames[r.nextInt(firstNames.length)] + " " + lastNames[r.nextInt(lastNames.length)]);
				emp.tags().add(colors[r.nextInt(colors.length)]);
				emp.attrs().number("random", r.nextDouble());
				emps[i] = emp;
			}
			// make edges
			for (int i = 0; i < edgeCount; i++) {
				Edge edge = edit.createEdge(cmps[r.nextInt(cmpCount)], emps[r.nextInt(empCount)]);
				edge.type(wrkType);
				edge.attrs().integer("index", i);
				edge.attrs().number("random", r.nextDouble());
			}
			edit.commit();
		}
		view.space.close();
		{
			int reps = 1000;
			long s = System.currentTimeMillis();
			double result = 0.0;
			for (int i = 0; i < reps; i++) {
				Graph graph = view.inspect().graph();
				result += graph.nodes(withTag("red")).intersect(graph.nodes(ofType(cmpType))).stream().mapToDouble(n -> n.attrs().number("random")).sum();
			}
			long f = System.currentTimeMillis();
			System.out.println((f - s) + " for " + reps + " (" + result + ")");
		}
		long finishSize = file.length();
		System.out.println("finish size " + finishSize);
		System.out.println("growth per cmp " + (double)(finishSize - startSize)/(cmpCount + empCount));
	}

	private void runTest(Consumer<Edit> builder, Consumer<Inspect> verifier, Identity identity, Viewer... approaches) {
		for (Config config : configs) {
			runTest(config, builder, verifier, identity, approaches);
		}
	}

	private void runTest(Config config, Consumer<Edit> builder, Consumer<Inspect> verifier, Identity identity, Viewer... viewers) {
		Builder mvBuilder = builder(config.sType);
		View view = buildView(mvBuilder, identity, viewers);
		Edit edit = view.edit();
		int gid = System.identityHashCode(edit);
//		System.out.println("BUILD STARTING");
		builder.accept(edit);
//		System.out.println("BUILD FINISHED");
		edit.commit();
		if (!config.reuseStore) {
			view.space.close();
			view.space.store.close();
			view = buildView(mvBuilder, identity, viewers);
		}
		Inspect inspect = view.inspect();
		Assert.assertNotEquals(gid, System.identityHashCode(inspect));
//		System.out.println("VERIFY STARTING");
		verifier.accept(inspect);
//		System.out.println("VERIFY FINISHED");
	}

	private View buildView(Builder mvBuilder, Identity identity, Viewer... viewers) {
		MVStore store = mvBuilder.open();
		//MVStoreTool.dump(filename, true);
		Space space = new Space(Store.wrap(store));
		for (Viewer viewer : viewers) {
			space.associate(viewer);
		}
		space.open();
		return space.view(identity);
	}

	private Builder builder(SType sType) {
		Builder mvBuilder = new MVStore.Builder();
		mvBuilder.autoCommitDisabled();
		switch (sType) {
		case FILE:
			String filename;
			try {
				filename = File.createTempFile("graph-", ".db").getAbsolutePath();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			mvBuilder.fileName(filename);
			break;
		case MEMORY:
			mvBuilder.fileName(null);
			break;
		case OFF_HEAP:
			mvBuilder.fileStore(new OffHeapStore());
			break;
		default:
			throw new IllegalStateException();
		}
		return mvBuilder;
	}

	private void expectCE(Runnable r) {
		try {
			r.run();
			Assert.fail();
		} catch (ConstraintException e) {
			/* expected */
		}
	}

	private void expectISE(Runnable r) {
		try {
			r.run();
			Assert.fail();
		} catch (IllegalStateException e) {
			/* expected */
		}
	}

	private void expectIAE(Runnable r) {
		try {
			r.run();
			Assert.fail();
		} catch (IllegalArgumentException e) {
			/* expected */
		}
	}

}
