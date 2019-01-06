package com.superdashi.gosper.graphdb;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Value;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;

public abstract class Visit implements AutoCloseable {

	private final Maps<Integer, Node> nodeMaps = Collect.setsOf(int.class).mappedTo(Node.class);
	private final Maps<Integer, Edge> edgeMaps = Collect.setsOf(int.class).mappedTo(Edge.class);

	final Space space;
	//TODO split view so that we don't depend on all of it?
	final View view; // a view that may limit visibility of the parts
	final Indices indices; // taken from space, depending on mutability

	final Resolver unrestrictedResolver = new Resolver(this, Selector.any());
	private final Graph graph; // a graph over all the parts acccessible in this visit

	//TODO use a better map
	private final Map<Long, Identity> identitiesById = new HashMap<>(); // a fast lookup cache of identities by their id

	//TODO make these weak
	private final Map<Integer, Node> nodes = nodeMaps.newMap();
	private final Map<Integer, Edge> edges = edgeMaps.newMap();

	private boolean closed = false;

	final Map<AttrName, Value.Type> types; // cached for efficiency
	final Map<AttrName, Value> defaults; // cached for efficiency
	final Map<AttrName, Value.Type> indexTypes; // cached for efficiency
	// all below cached for efficiency
	final Map<Namespace, Integer> codesByNs;
	final Map<Integer, Namespace> nssByCode;
	final Map<String, Integer> codesById;
	final Map<Integer, String> idsByCode;
	final Map<String, Integer> codesByType;
	final Map<Integer, String> typesByCode;
	final Map<String, Integer> codesByAttr;
	final Map<Integer, String> attrsByCode;

	Visit(Space space, View view, Indices indices) {
		this.space = space;
		this.view = view;
		this.indices = indices;

		space.takeLock();
		Restriction restriction = new Restriction(view.selector, this);
		this.graph = new Graph(this, restriction);

		// cached fields
		types = space.types;
		defaults = space.defaults;
		indexTypes = space.indexTypes; //TODO should build on view since can be restricted to the accessible attrs
		codesById = space.identityNameLookup.getByObj();
		idsByCode = space.identityNameLookup.getByCode();
		codesByNs = space.namespaceLookup.getByObj();
		nssByCode = space.namespaceLookup.getByCode();
		codesByType = space.typeNameLookup.getByObj();
		typesByCode = space.typeNameLookup.getByCode();
		codesByAttr = space.attrNameLookup.getByObj();
		attrsByCode = space.attrNameLookup.getByCode();
	}

	public long version() {
		return indices.version;
	}

	public Graph graph() {
		return graph;
	}

	public Optional<Part> part(PartRef ref) {
		if (ref == null) throw new IllegalArgumentException("null ref");
		int sourceId = EdgeKey.sourceId(ref.id);
		int edgeId = EdgeKey.edgeId(ref.id);
		return Optional.ofNullable(edgeId == Space.NO_EDGE_ID ? possibleNode(sourceId) : possibleEdge(edgeId, sourceId));
	}

	public Optional<Node> node(PartRef ref) {
		if (ref == null) throw new IllegalArgumentException("null ref");
		return EdgeKey.edgeId(ref.id) == Space.NO_EDGE_ID ? Optional.ofNullable(possibleNode(EdgeKey.sourceId(ref.id))) : Optional.empty();
	}

	public Optional<Edge> edge(PartRef ref) {
		if (ref == null) throw new IllegalArgumentException("null ref");
		int edgeId = EdgeKey.edgeId(ref.id);
		if (edgeId == Space.NO_EDGE_ID) return Optional.empty();
		int sourceId = EdgeKey.sourceId(ref.id);
		return Optional.ofNullable(possibleEdge(edgeId, sourceId));
	}

	final public void close() {
		if (!closed) {
			closeOnly();
			rollback();
		}
	}

	void closeOnly() {
		closed = true;
		space.releaseLock();
	}

	EdgeCursor incidentEdges(int nodeId) {
		Resolver r = unrestrictedResolver;
		return new EdgeCursor(indices.edgesWithSource(r, nodeId).or(indices.edgesWithTarget(r, nodeId)), unrestrictedResolver);
	}

	void addNode(Node node) {
		nodes.put(node.id, node);
	}

	void addEdge(Edge edge) {
		edges.put(edge.id, edge);
	}

	Node createNode(long owner) {
		checkMutable();
		int id = space.allocateNodeId();
		Node node = new Node(this, owner, id);
		addNode(node);
		return node;
	}

	// used by edges to lookup nodes
	Node node(int nodeId) {
		Node node = nodes.get(nodeId);
		if (node == null) {
			node = indices.node(this, nodeId, false);
			nodes.put(nodeId, node);
		}
		return node;
	}

	// used if the node data is available to avoid obtaining it twice
	Node node(int nodeId, PartData data, boolean knownVisible) {
		assert data != null;
		Node node = nodes.get(nodeId);
		if (node == null) {
			node = new Node(this, nodeId, data, knownVisible);
			nodes.put(nodeId, node);
		}
		return node;
	}

	// used to resolve references
	Node possibleNode(int nodeId) {
		Node node = nodes.get(nodeId);
		if (node == null) {
			node = indices.possibleNode(this, nodeId);
			if (node != null) nodes.put(nodeId, node);
		}
		return node;
	}

	//TODO combine with below null data?
	Edge edge(int edgeId, int sourceId, boolean knownVisible) {
		assert Space.isEdgeId(edgeId);
		assert Space.isNodeId(sourceId);
		Edge edge = edges.get(edgeId);
		if (edge == null) {
			edge = indices.edge(this, edgeId, sourceId, knownVisible);
			edges.put(edgeId, edge);
		}
		return edge;
	}

	// used if the edge data is available to avoid obtaining it twice
	Edge edge(EdgeKey key, PartData data, boolean knownVisible) {
		assert data != null;
		Edge edge = edges.get(key.edgeId);
		if (edge == null) {
			edge = new Edge(this, key, data, knownVisible);
			edges.put(key.edgeId, edge);
		}
		return edge;
	}

	// used to resolve references
	Edge possibleEdge(int edgeId, int sourceId) {
		assert Space.isEdgeId(edgeId);
		assert Space.isNodeId(sourceId);
		Edge edge = edges.get(edgeId);
		if (edge == null) {
			edge = indices.possibleEdge(this, edgeId, sourceId);
			if (edge != null) edges.put(edgeId, edge);
		}
		return edge;
	}

	Tag tagForId(long tagId) {
		int nsc = Name.nsCode(tagId);
		int nmc = Name.nmCode(tagId);
		//TODO should maintain a cache
		return new Tag(nssByCode.get(nsc), space.tagForCode(nmc));
	}

	long idForTag(Tag tag) {
		int nsc = codesByNs.get(tag.namespace);
		int nmc = space.codeForTag(tag.name);
		//TODO should cache
		return Name.nsnId(nsc, nmc);
	}

	long idForAttrName(AttrName attrName) {
		int nsc = codesByNs.get(attrName.namespace);
		int nmc = codesByAttr.get(attrName.name);
		//TODO should cache
		return Name.nsnId(nsc, nmc);
	}

	Tag newTag(String tagName) {
		if (tagName == null) throw new IllegalArgumentException("null tagName");
		if (tagName.isEmpty()) throw new IllegalArgumentException("empty tagName");
		return new Tag(view.namespace, tagName);
	}

	Identity identityForId(long identityId) {
		Identity identity = identitiesById.get(identityId);
		if (identity == null) {
			int nsc = Name.nsCode(identityId);
			int nmc = Name.nmCode(identityId);
			Namespace ns = nssByCode.get(nsc);
			String name = idsByCode.get(nmc);
			identity = space.canonIdentities.get(new Identity(ns, name));
			identitiesById.put(identityId, identity);
		}
		return identity;
	}


	void checkNotClosed() {
		if (closed) throw new ConstraintException(ConstraintException.Type.VISIT_STATE, "visit closed");
	}

	void checkAvailable(Type type) {
		if (!view.availableTypes.contains(type)) throw new IllegalArgumentException("type not available");
	}

	void dumpIndices() {
		flush();
		indices.dump();
	}

	abstract void checkMutable();

	abstract Edge createEdge(Node source, Node target);

	abstract boolean flush();

	abstract void rollback();

	abstract void recordDirty(Part part);

	abstract void recordClean(Part part);

	abstract void flushAdditions();

}
