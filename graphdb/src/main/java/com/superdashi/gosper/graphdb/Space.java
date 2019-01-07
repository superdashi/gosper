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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVMap.Builder;
import org.h2.mvstore.MVStore;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Value;
import com.tomgibara.fundament.Bijection;

public final class Space {

	//TODO base this on debug status flag
	private static final boolean DUMP_CHANGES = false;
	static final boolean DUMP_INDICES_ON_START = false;
	static final boolean DUMP_RESTRICTION = false;
	static final boolean FAIL_ON_SCAN = false;

	//TODO make configurable
	private static final int NODE_ID_RESERVATION_BATCH = 1000;
	private static final int EDGE_ID_RESERVATION_BATCH = 1000;

	static final int NO_EDGE_ID = -1;
	static final int NO_NODE_ID = -1;
	static final int NO_OWNER = -1;

	//TODO replace
	static final String MAP_NAME_META          = "meta"       ;
	static final String MAP_NAME_TYPES_BY_CODE = "typesByCode";
	static final String MAP_NAME_CODES_BY_TYPE = "codesByType";
	static final String MAP_NAME_TAGS_BY_CODE  = "tagsByCode" ;
	static final String MAP_NAME_CODES_BY_TAG  = "codesByTag" ;
	static final String MAP_NAME_ATTRS_BY_CODE = "attrsByCode";
	static final String MAP_NAME_CODES_BY_ATTR = "codesByAttr";
	static final String MAP_NAME_PERMS_BY_CODE = "permsByCode";
	static final String MAP_NAME_CODES_BY_PERM = "codesByPerm";
	static final String MAP_NAME_NSS_BY_CODE   = "nssByCode"  ;
	static final String MAP_NAME_CODES_BY_NS   = "codesByNs"  ;
	static final String MAP_NAME_IDS_BY_CODE   = "idsByCode"  ;
	static final String MAP_NAME_CODES_BY_ID   = "codesById"  ;

	static final String NEXT_NODE_ID = "next_node_id";
	static final String NEXT_EDGE_ID = "next_edge_id";

	static boolean isNsCode(int i) {
		return i >= 0 && (i & 1) == 0;
	}

	static boolean isNmCode(int i) {
		return i >= 0 && (i & 1) == 1;
	}

	// doesn't bother to eliminate case where i is MAX_VALUE
	static boolean isNodeId(int i) {
		return i >= 0 && (i & 1) == 0;
	}

	// doesn't bother to eliminate case where i is MAX_VALUE
	static boolean isEdgeId(int i) {
		return i >= 0 && (i & 1) == 1;
	}

	private final Map<Identity, Viewer> viewers = new HashMap<>();
	final Map<AttrName, Value.Type> types = new HashMap<>(); // maps attributes to their types
	final Map<AttrName, Value> defaults = new HashMap<>(); // maps attributes to their default values
	final Map<AttrName, Value.Type> indexTypes = new HashMap<>(); // a subset of types, for indices only
	final Set<AttrName> newIndices = new HashSet<>(); // records indices added since last indices last updated
	final MVStore store; // expose for convenience of testing
	private final MVMap<String, Object> meta;

	final Lookup<Namespace> namespaceLookup;
	final Lookup<String> identityNameLookup;
	final Lookup<String> typeNameLookup;
	final Lookup<String> attrNameLookup;
	//TODO could merge with identityNameLookup
	final Lookup<String> permissionLookup;
	final Map<Identity, Identity> canonIdentities = new HashMap<>();
	private final Map<Namespace, Set<Type>> availableTypes = new HashMap<>();

	private final MVMap<Integer, String> tagsByCode;
	private final MVMap<String, Integer> codesByTag;

	private final Indices indices;
	private Indices readOnlyIndices;

	private final ReentrantReadWriteLock spaceLock = new ReentrantReadWriteLock();
	private final ReentrantLock editLock = new ReentrantLock();
	private Edit currentEdit = null;

	// lock must be taken when mutating node/edge id fields
	private final Object idLock = new Object();
	private int nextNodeId; // the next unused node id (prior to transformation)
	private int reservedNodeId; // if a node id higher than this is used, it must be recorded
	private int nextEdgeId; // the next unused node id (prior to transformation)
	private int reservedEdgeId; // if a node id higher than this is used, it must be recorded

	private boolean open = false;
	private boolean closed = false;

	//TOOD probably needs proper random
	private final Random rand = new Random();

	volatile Observer observer = new Observer();

	public Space(Store storage) {
		assert storage != null;
		this.store = storage.store;
		store.setAutoCommitDelay(0);
		meta = store.openMap(MAP_NAME_META);
		{
			Builder<Integer, String> builder = new MVMap.Builder<>();
			builder.keyType(IntType.instance);
			tagsByCode = store.openMap(MAP_NAME_TAGS_BY_CODE, builder);
		}
		{
			Builder<String, Integer> builder = new MVMap.Builder<>();
			builder.valueType(IntType.instance);
			codesByTag = store.openMap(MAP_NAME_CODES_BY_TAG, builder);
		}

		namespaceLookup = new Lookup<>(store, MAP_NAME_NSS_BY_CODE, MAP_NAME_CODES_BY_NS, Bijection.fromFunctions(String.class, Namespace.class, Namespace::new, Namespace::toString), () -> rand.nextInt() & 0x7ffffffe);
		identityNameLookup = new Lookup<>(store, MAP_NAME_IDS_BY_CODE, MAP_NAME_CODES_BY_ID, Bijection.identity(String.class), Lookup.positiveRandom(rand));
		typeNameLookup = new Lookup<>(store, MAP_NAME_TYPES_BY_CODE, MAP_NAME_CODES_BY_TYPE, Bijection.identity(String.class), Lookup.positiveRandom(rand));
		attrNameLookup = new Lookup<>(store, MAP_NAME_ATTRS_BY_CODE, MAP_NAME_CODES_BY_ATTR, Bijection.identity(String.class), () -> rand.nextInt() & 0x7ffffffe | 0x00000001);
		permissionLookup = new Lookup<>(store, MAP_NAME_PERMS_BY_CODE, MAP_NAME_CODES_BY_PERM, Bijection.identity(String.class), Lookup.positiveRandom(rand));

		indices = new Indices(this);
		readOnlyIndices = indices.snapshot();

		synchronized (idLock) {
			nextNodeId = (int) meta.getOrDefault(NEXT_NODE_ID, 0);
			reservedNodeId = nextNodeId;
			nextEdgeId = (int) meta.getOrDefault(NEXT_EDGE_ID, 0);
			reservedEdgeId = nextEdgeId;
		}
	}

	//TODO rename to something better?
	public void associate(Viewer viewer) {
		if (viewer == null) throw new IllegalArgumentException("null viewer");
		if (closed) throw new ConstraintException(ConstraintException.Type.SPACE_STATE, "space closed");
		boolean active = active();
		if (active) {
			spaceLock.writeLock().lock();
			deactivate();
		}
		try {
			Identity identity = viewer.identity;
			if (viewers.containsKey(identity)) throw new IllegalArgumentException("viewer already supplied for identity: " + identity);
			canonIdentities.putIfAbsent(identity, identity);
			// check that the viewer does not redefine existing type
			for (Attribute a : viewer.typedAttrs.values()) {
				Value.Type type = types.get(a.name);
				if (type == null) continue;
				if (!type.equals(a.type)) throw new IllegalArgumentException("redefines type of attribute " + a.name);
			}
			// coerce the types of the default values
			Map<AttrName, Value> defaultedAttrs = new HashMap<>();
			for (Attribute a : viewer.defaultedAttrs.values()) {
				Value.Type type = a.type;
				if (type == Value.Type.EMPTY) type = types.getOrDefault(a.name, Value.Type.EMPTY);
				Value value = type == Value.Type.EMPTY ? a.value : a.value.as(type);
				if (!value.isEmpty()) defaultedAttrs.put(a.name, value);
			}
			// check the defaulted values match
			for (Entry<AttrName, Value> entry : defaultedAttrs.entrySet()) {
				AttrName name = entry.getKey();
				Value oldValue = defaults.get(name);
				if (oldValue != null && !oldValue.equals(entry.getValue())) {
					throw new IllegalArgumentException("redefines default value of attribute " + name);
				}
			}
			viewers.put(identity, viewer);
			identityNameLookup.record(identity.name);
			Namespace namespace = viewer.namespace;
			namespaceLookup.record(namespace);
			for (String typeName : viewer.typeNames) {
				typeNameLookup.record(typeName);
			};
			for (Attribute a : viewer.typedAttrs.values()) {
				AttrName name = a.name;
				if (!indexTypes.containsKey(name)) {
					indexTypes.put(name, a.type);
					newIndices.add(name);
				}
				Value.Type type = types.get(name);
				if (type != null) continue; // if type is not null, we know it's the same because that is prechecked
				attrNameLookup.record(name.name);
				types.put(name, a.type);
			}
			for (Entry<AttrName, Value> entry : defaultedAttrs.entrySet()) {
				AttrName name = entry.getKey();
				if (!defaults.containsKey(name)) {
					defaults.put(name, entry.getValue());
				}
			}
			viewer.declaredPermissionNames.forEach(p -> {
				permissionLookup.record(p);
				Identity perm = new Identity(namespace, p);
				canonIdentities.putIfAbsent(perm, perm);
			});
		} finally {
			if (active) {
				activate();
				spaceLock.writeLock().unlock();
			}
		}
	}

	public void open() {
		if (closed) throw new ConstraintException(ConstraintException.Type.SPACE_STATE, "space closed");
		if (open) return;
		activate();
		open = true;
	}

	public View view(Identity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		Viewer viewer = viewers.get(identity);
		if (viewer == null) throw new IllegalArgumentException("namespace not associated with a viewer");
		if (!open || closed) throw new ConstraintException(ConstraintException.Type.SPACE_STATE, "space closed or not opened");
		return new View(this, viewer);
	}

	//TODO needs proper lock, or spin trying to observe
	synchronized void registerObservation(Observation obs) {
		observer = observer.with(obs);
	}

	synchronized void deregisterObservation(Observation obs) {
		observer = observer.without(obs);
	}

	synchronized boolean registeredObservation(Observation obs) {
		return observer.observes(obs);
	}

	public boolean active() {
		return open & !closed;
	}

	public void close() {
		if (!open) throw new ConstraintException(ConstraintException.Type.SPACE_STATE, "space not open");
		if (closed) return;
		deactivate();
		closed = true;
	}

	Inspect newInspect(View view) {
		return new Inspect(this, view, readOnlyIndices);
	}

	Edit newEdit(View view) {
		if (editLock.isHeldByCurrentThread()) {
			throw new ConstraintException(ConstraintException.Type.REENTRANT_EDIT);
		}
		editLock.lock();
		currentEdit = new Edit(this, view, indices);
		return currentEdit;
	}

	int allocateNodeId() {
		synchronized (idLock) {
			int id = nextNodeId ++;
			id = Integer.reverse(id << 2) << 1;
			//TODO deal with overflow case
			assert isNodeId(id);
			return id;
		}
	}

	int allocateEdgeId() {
		synchronized (idLock) {
			int id = nextEdgeId ++;
			id = (Integer.reverse(id << 2) << 1) | 1;
			//TODO deal with overflow case
			assert isEdgeId(id);
			return id;
		}
	}

	// creates if necessary
	int codeForTag(String tagName) {
		int code = codesByTag.getOrDefault(tagName, -1);
		if (code < 0) {
			do {
				code = rand.nextInt() & 0x7fffffff;
			} while (tagsByCode.containsKey(code));
			codesByTag.put(tagName, code);
			tagsByCode.put(code, tagName);
		}
		return code;
	}

	String tagForCode(int code) {
		String tagName = tagsByCode.get(code);
		if (tagName == null) throw new IllegalArgumentException("no tag name for code: " + code);
		return tagName;
	}

	Set<Type> availableTypes(Namespace namespace) {
		return availableTypes.get(namespace);
	}

	void flush(List<Change> changes) {
		if (DUMP_CHANGES) {
			System.out.println("---- CHANGES START ----");
			changes.stream().forEachOrdered(e -> System.out.format("%s%n", e));
			System.out.println("----- CHANGES END -----");
		}
		// apply changes to indices
		for (Change change : changes) {
			change.applyTo(indices);
		}
	}

	void commit(boolean changesMade) {
		try {
			if (changesMade) {
				try {
					// commit changes and possible id lease increment
					synchronized (idLock) {
						// allocate more node ids if necessary
						int newReservedNodeId;
						if (nextNodeId >= reservedNodeId) {
							newReservedNodeId = nextNodeId + NODE_ID_RESERVATION_BATCH;
							meta.put(NEXT_NODE_ID, newReservedNodeId);
						} else {
							newReservedNodeId = reservedNodeId;
						}
						// allocate more node ids if necessary
						int newReservedEdgeId;
						if (nextEdgeId >= reservedEdgeId) {
							newReservedEdgeId = nextEdgeId + EDGE_ID_RESERVATION_BATCH;
							meta.put(NEXT_EDGE_ID, newReservedEdgeId);
						} else {
							newReservedEdgeId = reservedEdgeId;
						}
						store.commit();
						// update state to match committed values
						reservedNodeId = newReservedNodeId;
						reservedEdgeId = newReservedEdgeId;
					}
				} finally {
					// clear previous readonly indices
					readOnlyIndices = indices.snapshot();
				}
			}
		} finally {
			currentEdit = null;
			editLock.unlock();
		}
	}

	void rollback() {
		currentEdit = null;
		editLock.unlock();
	}

	void takeLock() {
		spaceLock.readLock().lock();
	}

	void releaseLock() {
		spaceLock.readLock().unlock();
	}

	long identityId(Identity identity) {
		int nsc = namespaceLookup.getByObj().getOrDefault(identity.ns, -1);
		int nmc = identityNameLookup.getByObj().getOrDefault(identity.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long typeId(Type type) {
		int nsc = namespaceLookup.getByObj().getOrDefault(type.namespace, -1);
		int nmc = typeNameLookup.getByObj().getOrDefault(type.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long permId(Identity perm) {
		int nsc = namespaceLookup.getByObj().getOrDefault(perm.ns, -1);
		int nmc = permissionLookup.getByObj().getOrDefault(perm.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long tagId(Tag tag) {
		int nsc = namespaceLookup.getByObj().getOrDefault(tag.namespace, -1);
		int nmc = codesByTag.getOrDefault(tag.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	private void activate() {
		// process lookups & indices
		boolean modified =
				namespaceLookup.lock() | identityNameLookup.lock() | typeNameLookup.lock() | attrNameLookup.lock() | permissionLookup.lock() | indices.update();

		// compute the available types
		viewers.keySet().stream().map(i -> i.ns).distinct().forEach(
				ns -> availableTypes.put(ns, Collections.unmodifiableSet(
						viewers.values().stream().filter(v -> v.namespace.equals(ns)).flatMap(v -> v.typeNames.stream()).map(n -> new Type(ns, n)).collect(Collectors.toSet())
		))); // no need to clear first since associating viewers is additive

		// commit changes
		if (modified) {
			store.commit();
		}
	}

	private void deactivate() {
		namespaceLookup.unlock();
		identityNameLookup.unlock();
		typeNameLookup.unlock();
		attrNameLookup.unlock();
		permissionLookup.unlock();
	}

}
