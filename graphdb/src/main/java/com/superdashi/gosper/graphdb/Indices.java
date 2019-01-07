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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Value;

final class Indices {

	// statics

	static final String MAP_NAME_NODES_BY_ID   = "nodesById"  ;
	static final String MAP_NAME_EDGES_BY_SRC  = "nodesBySrc" ;
	static final String MAP_NAME_EDGES_BY_TRG  = "nodesByTrg" ;

	private static final String INDEX_PREFIX = "index:";

	private static final Builder<ValueKey, Value> indexBuilder(Value.Type type) {
		Builder<ValueKey, Value> builder = new MVMap.Builder<>();
		builder.keyType(ValueKeyType.instanceFor(type));
		builder.valueType(ValueType.instanceFor(Value.Type.EMPTY));
		return builder;
	}

	private static Value.Type indexType(MVMap<?,?> index) {
		String name = index.getName();
		return Value.Type.valueOf(name.substring(name.lastIndexOf(':') + 1));
	}

	// fields

	final boolean snapshot;
	final long version;
	private final Space space;

	final MVMap<Integer, PartData> nodesById;
	final PartIndices nodeIndices;

	final MVMap<EdgeKey, PartData> edgesBySource;
	final MVMap<EdgeKey, Value> edgesByTarget; // maps to empty
	final PartIndices edgeIndices;

	final Map<AttrName, MVMap<ValueKey, Value>> indicesByName = new HashMap<>(); // indices map to empty
	final Map<Long, MVMap<ValueKey, Value>> indicesById = new HashMap<>(); // as above, naturally

	// constructors

	Indices(Space space) {
		snapshot = false;
		this.space = space;
		MVStore store = space.store;
		version = store.getCurrentVersion();
		{
			Builder<Integer, PartData> builder = new MVMap.Builder<>();
			builder.keyType(IntType.instance);
			builder.valueType(PartDataType.instance);
			nodesById = store.openMap(MAP_NAME_NODES_BY_ID, builder);
		}
		nodeIndices = new PartIndices(store, "node");

		{
			Builder<EdgeKey, PartData> builder = new MVMap.Builder<>();
			builder.keyType(SourceKeyType.instance);
			builder.valueType(PartDataType.instance);
			edgesBySource = store.openMap(MAP_NAME_EDGES_BY_SRC, builder);
		}
		{
			Builder<EdgeKey, Value> builder = new MVMap.Builder<>();
			builder.keyType(TargetKeyType.instance);
			builder.valueType(EmptyType.instance);
			edgesByTarget = store.openMap(MAP_NAME_EDGES_BY_TRG, builder);
		}
		edgeIndices = new PartIndices(store, "edge");

		{
			store.getMapNames().stream().filter(n -> n.startsWith(INDEX_PREFIX)).forEach(n -> {
				String[] parts = n.split(":");
				Namespace ns = new Namespace(parts[1]);
				String nm = parts[2];
				AttrName name = new AttrName(ns, nm);
				Value.Type type = Value.Type.valueOf(parts[3]);
				MVMap<ValueKey, Value> index = store.openMap(n, indexBuilder(type));
				indicesByName.put(name, index);
			});
		}
	}

	private Indices(Indices that, long version) {
		snapshot = true;
		this.version = version;
		this.space = that.space;

		nodesById = that.nodesById.openVersion(version);
		nodeIndices = that.nodeIndices.version(version);

		edgesBySource = that.edgesBySource.openVersion(version);
		edgesByTarget = that.edgesByTarget.openVersion(version);
		edgeIndices = that.edgeIndices.version(version);

		Map<MVMap<ValueKey, Value>, MVMap<ValueKey, Value>> map = that.indicesByName.values().stream().collect(Collectors.toMap(m -> m, m -> m.openVersion(version)));
		that.indicesByName.forEach((n,m) -> {
			this.indicesByName.put(n, map.get(m));
		});
		that.indicesById.forEach((n,m) -> {
			this.indicesById.put(n, map.get(m));
		});
	}

	// methods

	// lifetime methods

	// returns true if store modified by init
	boolean update() {
		// records whether modified
		//TODO use something better than atomic boolean?
		final AtomicBoolean modified = new AtomicBoolean();
		final MVStore store = space.store;

		Set<AttrName> newIndices = space.newIndices;
		Lookup<Namespace> namespaceLookup = space.namespaceLookup;
		Lookup<String> attrNameLookup = space.attrNameLookup;
		Map<AttrName, Value.Type> indexTypes = space.indexTypes;

		// remove unmatched indices
		newIndices.forEach(n -> {
			MVMap<ValueKey, Value> index = indicesByName.get(n);
			if (index != null) {
				Value.Type type = space.indexTypes.get(n);
				if (type != indexType(index)) { // viewer declared an inconsistent type, delete it
					store.removeMap(index);
					indicesByName.remove(n);
					//TODO confirm if this is needed for map removals
					modified.set(true);
				}
			}
		});

		// build missing indices
		//TODO should make one pass over parts
		newIndices.forEach(name -> {
			Value.Type type = space.indexTypes.get(name);
			String mapName = INDEX_PREFIX + name + ':' + type.name();
			MVMap<ValueKey, Value> index = store.openMap(mapName, indexBuilder(type));
			indicesByName.put(name, index);
			indexTypes.put(name, type);
			// build index
			Cursor<Integer, PartData> cursor = nodesById.cursor(null);
			int nsc = namespaceLookup.getByObj().get(name.namespace);
			int nmc = attrNameLookup.getByObj().get(name.name);
			while (cursor.hasNext()) {
				cursor.next();
				PartData data = cursor.getValue();
				Value value = data.value(nsc, nmc);
				if (value == null || value.isEmpty()) continue;
				if (type == Value.Type.EMPTY) {
					value = Value.empty();
				} else {
					value = value.as(type);
					if (value.isEmpty()) continue;
				}
				int partId = cursor.getKey();
				index.put(new ValueKey(value, partId, Space.NO_EDGE_ID), Value.empty());
				modified.set(true);
			}
		});

		// build a lookup for indices directly by attrId
		newIndices.forEach((n) -> {
			int nsc = namespaceLookup.getByObj().get(n.namespace);
			int nmc = attrNameLookup.getByObj().get(n.name);
			long attrId = Name.nsnId(nsc, nmc);
			indicesById.putIfAbsent(attrId, indicesByName.get(n));
		});

		// record the fact we've updated the indices
		newIndices.clear();

		if (Space.DUMP_INDICES_ON_START) dump();

		return modified.get();
	}

	void add(
			Viewer viewer,
			Map<AttrName, Value.Type> indexTypes,
			Lookup<Namespace> namespaceLookup,
			Lookup<String> attrNameLookup
			) {

	}

	Indices snapshot() {
		return new Indices(this, space.store.getCurrentVersion());
	}

	// basic part methods

	void putNode(Node node) {
		//TODO annoying that this clone is necessary
		nodesById.put(node.id, node.data.clone());
	}

	void removeNode(Node node) {
		boolean modified = nodesById.remove(node.id) != null;
		assert modified;
	}

	void putEdge(Edge edge) {
		//TODO annoying that this clone is necessary
		edgesBySource.put(new EdgeKey(edge.edgeId(), edge.sourceId(), edge.targetId()), edge.data.clone());
	}

	void removeEdge(Edge edge) {
		//TODO add key method to edge?
		boolean modified = edgesBySource.remove(new EdgeKey(edge.edgeId(), edge.sourceId(), edge.targetId())) != null;
		assert modified;
	}

	Node node(Visit visit, int nodeId, boolean knownVisible) {
		PartData data = nodesById.get(nodeId);
		if (data == null) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "no node for id " + nodeId);
		//TODO annoying that this clone is necessary
		return new Node(visit, nodeId, data.clone(), knownVisible);
	}

	Node possibleNode(Visit visit, int nodeId) {
		PartData data = nodesById.get(nodeId);
		//TODO annoying that this clone is necessary
		return data == null ? null : new Node(visit, nodeId, data.clone(), false);
	}

	Edge edge(Visit visit, int edgeId, int sourceId, boolean knownVisible) {
		EdgeKey key = edgesBySource.ceilingKey(new EdgeKey(edgeId, sourceId, 0));
		if (key == null) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "no edge for id: " + edgeId + " on (" + sourceId + ")");
		//TODO annoying that this clone is necessary
		return new Edge(visit, key, edgesBySource.get(key).clone(), knownVisible);
	}

	Edge possibleEdge(Visit visit, int edgeId, int sourceId) {
		EdgeKey key = edgesBySource.ceilingKey(new EdgeKey(edgeId, sourceId, 0));
		//TODO annoying that this clone is necessary
		return key == null ? null : new Edge(visit, key, edgesBySource.get(key).clone(), false);
	}

	// node sequences

	NodeSequence allNodes(Resolver resolver) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			return nodesById.entrySet().stream().mapToInt(e -> {
				int id = e.getKey();
				resolver.observeNode(id, e.getValue());
				return id;
			}).iterator();
		};
	}

	NodeSequence nodesWithOwner(Resolver resolver, Identity owner) {
		return () -> {
			long identityId = space.identityId(owner);
			if (identityId < 0L) return NodeSequence.emptyIterator; // identity not known, so can't own anything
			return nodeIndices.nodeIteratorOverOwner(identityId);
		};
	}

	NodeSequence nodesWithType(Resolver resolver, Type type) {
		return () -> {
			long typeId = space.typeId(type);
			if (typeId < 0L) return NodeSequence.emptyIterator; // type or NS not known to db, so there can't be a node
			resolver.visit.flush(); // using index, needs flush
			return nodeIndices.nodeIteratorOverType(typeId);
		};
	}

	NodeSequence nodesWithPermission(Resolver resolver, Identity permission) {
		return () -> {
			long permId = space.permId(permission);
			if (permId < 0L) return NodeSequence.emptyIterator; // permission or NS not known to db, so there can't be a node
			resolver.visit.flush(); // using index, needs flush
			return nodeIndices.nodeIteratorOverPerm(permId);
		};
	}

	NodeSequence nodesWithTag(Resolver resolver, Tag tag) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush - we have to flush before looking for tag id, otherwise it may not have been added
			long tagId = space.tagId(tag);
			if (tagId < 0L) return NodeSequence.emptyIterator; // tag is unknown, so can't exist in graph
			return nodeIndices.nodeIteratorOverTag(tagId);
		};
	}

	// null value indicates any value
	NodeSequence nodesWithValue(Resolver resolver, AttrName attrName, Value value) {
		Value.Type type = space.types.get(attrName);
		if (type != null && value != null) {
			value = value.as(type);
			// if the value cannot be coerced to the declared type, there can be no matches
			if (value.isEmpty()) return () -> NodeSequence.emptyIterator;
		}

		MVMap<ValueKey, Value> index = indicesByName.get(attrName);
		if (index == null || (type == Value.Type.EMPTY) != (value == null)) { // we have to scan
			return nodesWithValueViaScan(resolver, attrName, value);
		}
		return value == null ? // we any node with a value
				nodesWithAnyValueViaIndex(resolver, index) :
				nodesWithValueViaIndex(resolver, index, value);
	}

	private NodeSequence nodesWithAnyValueViaIndex(Resolver resolver, MVMap<ValueKey, Value> index) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			return new MappedIntIterator<>(
					index.cursor(null),
					vk -> vk.sourceId,
					null);
		};
	}

	private NodeSequence nodesWithValueViaIndex(Resolver resolver, MVMap<ValueKey, Value> index, Value value) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			ValueKey from = index.ceilingKey(new ValueKey(value, 0, Space.NO_EDGE_ID));
			if (from == null || !from.value.equals(value)) return NodeSequence.emptyIterator;
			//TODO must prohibit MAX_VALUE as a valid partId
			ValueKey to = index.ceilingKey(new ValueKey(value, Integer.MAX_VALUE, Space.NO_EDGE_ID));
			//TODO can optimize for from == to
			return new MappedIntIterator<>(
					index.cursor(from),
					vk -> vk.sourceId,
					to);
		};
	}

	private NodeSequence nodesWithValueViaScan(Resolver resolver, AttrName attrName, Value value) {
		if (Space.FAIL_ON_SCAN) throw new AssertionError("scan");
		return () -> {
			int nsc = space.namespaceLookup.getByObj().getOrDefault(attrName.namespace, -1);
			if (nsc == -1) return NodeSequence.emptyIterator;
			String nm = attrName.name;
			int nmc = space.attrNameLookup.getByObj().getOrDefault(nm, -1);
			resolver.visit.flush(); // using index, needs flush
			Value.Type type = space.types.get(attrName);
			return nodesById.entrySet().stream().mapToInt(e -> {
				int id = e.getKey();
				PartData data = e.getValue();
				//TODO this seems very unsafe
				resolver.observeNode(id, data);
				Value v = nmc == -1 ? data.value(nsc, nm) : data.value(nsc, nmc);
				if (v == null) { // cannot possibly match - there's no value
					return -1;
				}
				if (value == null) { // value must still match any declared attr type
					return type != null && v.as(type).isEmpty() ? -1 : id;
				}
				if (type != null) v = v.as(type);
				return v.equals(value) ? id : -1;
			}).filter(n -> n >= 0).iterator();
		};
	}

	// edge sequences

	EdgeSequence allEdges(Resolver resolver) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			return edgesBySource.entrySet().stream().mapToLong(e -> {
				EdgeKey key = e.getKey();
				long id = key.id();
				resolver.observeEdge(key, e.getValue());
				return id;
			}).iterator();
		};
	}

	int countOfEdgesWithSource(Graph graph, int sourceId) {
		if (graph.restriction.unrestricted()) {
			graph.visit.flush(); // using index, needs flush
			MVMap<EdgeKey, ?> index = edgesBySource;
			EdgeKey from = index.ceilingKey(new EdgeKey(0, 0, sourceId));
			if (from == null || from.sourceId != sourceId) return 0;
			EdgeKey to = index.ceilingKey(new EdgeKey(Integer.MAX_VALUE, Integer.MAX_VALUE, sourceId));
			int j = to == null ? index.size() : (int) index.getKeyIndex(to);
			int i = (int) index.getKeyIndex(from);
			return j - i;
		} else {
			return (int) edgesWithSource(new Resolver(graph), sourceId).stream().count();
		}
	}

	EdgeSequence edgesWithSource(Resolver resolver, int sourceId) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			MVMap<EdgeKey, PartData> index = edgesBySource;
			EdgeKey from = index.ceilingKey(new EdgeKey(0, sourceId, 0));
			if (from == null || from.sourceId != sourceId) return EdgeSequence.emptyIterator;
			EdgeKey to = index.ceilingKey(new EdgeKey(Integer.MAX_VALUE, sourceId, Integer.MAX_VALUE));
			Cursor<EdgeKey, PartData> cursor = index.cursor(from);
			//TODO cannot currently observe edge
			return new MappedLongIterator<>(cursor, EdgeKey::id, to);
		};
	}

	int countOfEdgesWithTarget(Graph graph, int targetId) {
		if (graph.restriction.unrestricted()) {
			graph.visit.flush(); // using index, needs flush
			MVMap<EdgeKey, ?> index = edgesByTarget;
			EdgeKey from = index.ceilingKey(new EdgeKey(0, 0, targetId));
			if (from == null || from.targetId != targetId) return 0;
			EdgeKey to = index.ceilingKey(new EdgeKey(Integer.MAX_VALUE, Integer.MAX_VALUE, targetId));
			int j = to == null ? index.size() : (int) index.getKeyIndex(to);
			int i = (int) index.getKeyIndex(from);
			return j - i;
		} else {
			return (int) edgesWithTarget(new Resolver(graph), targetId).stream().count();
		}
	}

	EdgeSequence edgesWithTarget(Resolver resolver, int targetId) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			MVMap<EdgeKey, Value> index = edgesByTarget;
			EdgeKey from = index.ceilingKey(new EdgeKey(0, 0, targetId));
			if (from == null || from.targetId != targetId) return EdgeSequence.emptyIterator;
			EdgeKey to = index.ceilingKey(new EdgeKey(Integer.MAX_VALUE, Integer.MAX_VALUE, targetId));
			Cursor<EdgeKey, Value> cursor = index.cursor(from);
			return new MappedLongIterator<>(cursor, EdgeKey::id, to);
		};
	}

	EdgeSequence edgesWithOwner(Resolver resolver, Identity owner) {
		return () -> {
			long identityId = space.identityId(owner);
			if (identityId < 0L) return EdgeSequence.emptyIterator; // identity not known, so can't own anything
			return edgeIndices.edgeIteratorOverOwner(identityId);
		};
	}

	EdgeSequence edgesWithType(Resolver resolver, Type type) {
		return () -> {
			long typeId = space.typeId(type);
			if (typeId < 0) return EdgeSequence.emptyIterator; // type or NS not known to db, so there can't be a node
			resolver.visit.flush(); // using index, needs flush
			return edgeIndices.edgeIteratorOverType(typeId);
		};
	}

	EdgeSequence edgesWithPermission(Resolver resolver, Identity permission) {
		return () -> {
			long permId = space.permId(permission);
			if (permId < 0L) return EdgeSequence.emptyIterator; // permission or NS not known to db, so there can't be a node
			resolver.visit.flush(); // using index, needs flush
			return edgeIndices.edgeIteratorOverPerm(permId);
		};
	}

	EdgeSequence edgesWithTag(Resolver resolver, Tag tag) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush - we have to flush before looking for tag id, otherwise it may not have been added
			long tagId = space.tagId(tag);
			if (tagId < 0L) return EdgeSequence.emptyIterator; // tag is unknown, so can't exist in graph
			return edgeIndices.edgeIteratorOverTag(tagId);
		};
	}

	// null value indicates any value
	EdgeSequence edgesWithValue(Resolver resolver, AttrName attrName, Value value) {
		Value.Type type = space.types.get(attrName);
		if (type != null && value != null) {
			value = value.as(type);
			// if the value cannot be coerced to the declared type, there can be no matches
			if (value.isEmpty()) return () -> EdgeSequence.emptyIterator;
		}

		MVMap<ValueKey, Value> index = indicesByName.get(attrName);
		if (index == null || (type == Value.Type.EMPTY) != (value == null)) { // we have to scan
			return edgesWithValueViaScan(resolver, attrName, value);
		}
		return value == null ? // we any node with a value
				edgesWithAnyValueViaIndex(resolver, index) :
				edgesWithValueViaIndex(resolver, index, value);
	}

	private EdgeSequence edgesWithAnyValueViaIndex(Resolver resolver, MVMap<ValueKey, Value> index) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			return new MappedLongIterator<>(
					index.cursor(null),
					ValueKey::id,
					null);
		};
	}

	private EdgeSequence edgesWithValueViaIndex(Resolver resolver, MVMap<ValueKey, Value> index, Value value) {
		return () -> {
			resolver.visit.flush(); // using index, needs flush
			ValueKey from = index.ceilingKey(new ValueKey(value, 0, Space.NO_EDGE_ID));
			if (from == null || !from.value.equals(value)) return EdgeSequence.emptyIterator;
			//TODO must prohibit MAX_VALUE as a valid partId
			ValueKey to = index.ceilingKey(new ValueKey(value, Integer.MAX_VALUE, Space.NO_EDGE_ID));
			//TODO can optimize for from == to
			return new MappedLongIterator<>(
					index.cursor(from),
					ValueKey::id,
					to);
		};
	}

	private EdgeSequence edgesWithValueViaScan(Resolver resolver, AttrName attrName, Value value) {
		if (Space.FAIL_ON_SCAN) throw new AssertionError("scan");
		return () -> {
			int nsc = space.namespaceLookup.getByObj().getOrDefault(attrName.namespace, -1);
			if (nsc == -1) return EdgeSequence.emptyIterator;
			String nm = attrName.name;
			int nmc = space.attrNameLookup.getByObj().getOrDefault(nm, -1);
			resolver.visit.flush(); // using index, needs flush
			Value.Type type = space.types.get(attrName);
			return edgesBySource.entrySet().stream().mapToLong(e -> {
				EdgeKey key = e.getKey();
				long id = key.id();
				PartData data = e.getValue();
				//TODO this seems very unsafe
				resolver.observeEdge(key, data);
				Value v = nmc == -1 ? data.value(nsc, nm) : data.value(nsc, nmc);
				if (v == null) { // cannot possibly match - there's no value
					return -1L;
				}
				if (value == null) { // value must still match any declared attr type
					return type != null && v.as(type).isEmpty() ? -1L : id;
				}
				if (type != null) v = v.as(type);
				return v.equals(value) ? id : -1L;
			}).filter(n -> n >= 0L).iterator();
		};
	}

	// debug methods

	void dump() {
		indicesByName.forEach((n,m) -> dumpKeys(n.toString(), m));
		nodeIndices.dump();
		edgeIndices.dump();
		dumpKeys("edgesBySource", edgesBySource);
		dumpKeys("edgesByTarget", edgesByTarget);
	}

	// private helper methods

	private void dumpKeys(String n, Map<?,?> m) {
		System.out.println(n + "(" + m.size() + ")");
		m.keySet().forEach(k -> System.out.println(k));
		System.out.println();
	}
}
