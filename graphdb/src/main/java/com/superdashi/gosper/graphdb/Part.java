package com.superdashi.gosper.graphdb;

import java.util.List;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Value;

//TODO could reuse flags of data?
public abstract class Part implements Comparable<Part> {

	private static final int FLAG_VIEWABLE         = 0b0000001;
	private static final int FLAG_VIEWABLE_KNOWN   = 0b0000010;
	private static final int FLAG_MODIFIABLE       = 0b0000100;
	private static final int FLAG_MODIFIABLE_KNOWN = 0b0001000;
	private static final int FLAG_DELETABLE        = 0b0010000;
	private static final int FLAG_DELETABLE_KNOWN  = 0b0100000;
	private static final int FLAG_ALL_PERM         = 0b0111111;
	private static final int FLAG_RECORDED_DIRTY   = 0b1000000;
	final Visit visit;
	final int id;
	final PartData data;

	// created by expanding data
	private Identity owner = null;
	private Type type = null;
	private Permissions permissions = null;
	private Tags tags = null;
	private Attrs attrs = null;

	private int flags;

	Part(Visit visit, int id, PartData data, boolean knownVisible) {
		this.visit = visit;
		this.id = id;
		this.data = data;
		flags = knownVisible ? FLAG_VIEWABLE_KNOWN | FLAG_VIEWABLE : 0;
	}

	public boolean viewable() {
		return checkPerm(FLAG_VIEWABLE, FLAG_VIEWABLE_KNOWN, p -> p.isViewableIn(visit.view));
	}

	public boolean modifiable() {
		return checkPerm(FLAG_MODIFIABLE, FLAG_MODIFIABLE_KNOWN, p -> p.isModifiableIn(visit.view));
	}

	public boolean deletable() {
		return checkPerm(FLAG_DELETABLE, FLAG_DELETABLE_KNOWN, p -> p.isDeletableIn(visit.view));
	}

	public PartRef ref() {
		return new PartRef(EdgeKey.id(edgeId(), sourceId()));
	}

	public Identity owner() {
		if (owner == null) {
			owner = visit.identityForId(data.owner);
		}
		return owner;
	}

	public Type type() {
		if (type == null) {
			type = data.extractType(this);
		}
		return type;
	}

	public boolean type(Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		checkNotDeleted();
		checkModifiable();
		if (type.equals(type())) return false;
		checkAvailable(type);
		typeImpl(type);
		return true;
	}

	public boolean type(String typeName) {
		return type(newType(typeName));
	}

	public Permissions permissions() {
		if (permissions == null) {
			permissions = new Permissions(this);
		}
		return permissions;
	}

	public Tags tags() {
		if (tags == null) {
			tags = new Tags(this);
		}
		return tags;
	}

	public Attrs attrs() {
		if (attrs == null) {
			attrs = new Attrs(this);
		}
		return attrs;
	}

	public boolean matches(Selector selector) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		return selector.matches(this);
	}

	public abstract boolean isNode();
	public abstract boolean isEdge();

	public abstract void delete();

	public boolean deleted() {
		return data.isFlagged(PartData.FLAG_DELETED);
	}

	// comparable methods

	@Override
	public int compareTo(Part that) {
		int c = Integer.compare(this.sourceId(), that.sourceId());
		return c == 0 ? Integer.compare(this.edgeId(), that.edgeId()) : c;
	}

	abstract int edgeId();
	abstract int sourceId();
	abstract int targetId();

	abstract EdgeKey edgeKey();

	abstract void updateIndices(Indices indices);

	boolean accessibleByViewer() {
		return (visit.view.identityId & 0xffffffff00000000L) == (data.owner & 0xffffffff00000000L);
	}

	// for when type has been vetted as good
	boolean typeImpl(Type type) {
		this.type = type;
		recordDirty(PartData.FLAG_DIRTY_TYPE);
		return true;
	}

	// updates data so that it reflects the modified part state
	void cleanData(List<Change> changes) {
		if (deleted()) {
			int sourceId = sourceId();
			int edgeId = edgeId();
			{ // remove owner
				new NSNKey(data.owner, sourceId, edgeId).toOwnerRemoval().record(changes);
			}
			{ // remove incident nodes if we are an edge
				EdgeKey edgeKey = edgeKey();
				if (edgeKey != null) edgeKey().toNodesRemoval().record(changes);
			}
			{ // remove type
				new NSNKey(data.type, sourceId, edgeId).toTypeRemoval().record(changes);
			}
			{ // remove perms
				int[] perms = data.perms;
				if (perms.length > 1) {
					int nsCode = Name.nsCode(data.owner);
					int vs = (perms[0] >> 16) & 0xff;
					for (int i = 1; i <= vs; i++) {
						new NSNKey(nsCode, perms[i], sourceId, edgeId).toPermRemoval().record(changes);
					}
				}
			}
			for (long tag : data.tags) { // remove tags
				new NSNKey(tag, sourceId, edgeId).toTagRemoval().record(changes);
			}
			{ // remove attrs
				TreeMap<AttrName, Value> values = data.extractValueMap(this);
				visit.indexTypes.forEach((n,t) -> {
					Value value = values.get(n);
					if (value == null) return;
					value = value.as(t); // coerce to match index
					if (t != Value.Type.EMPTY && value.isEmpty()) return; // has value, but is not in index
					long attrId = visit.idForAttrName(n);
					ValueKey.attr(sourceId, edgeId, attrId, value, null).record(changes);
				});
			}
		} else {
			if (data.isFlagged(PartData.FLAG_DIRTY_OWNER)) { // only possible on creation
				new NSNKey(data.owner, sourceId(), edgeId()).toOwnerAddition().record(changes);
			}
			if (data.isFlagged(PartData.FLAG_DIRTY_NODES)) { // only possible on creation
				edgeKey().toNodesAddition().record(changes);
			}
			if (data.isFlagged(PartData.FLAG_DIRTY_TYPE)) {
				data.applyType(type, this, changes);
			}
			if (data.isFlagged(PartData.FLAG_DIRTY_PERMS)) {
				//TODO call may be inefficient for new node
				permissions().populateData(changes);
			}
			if (data.isFlagged(PartData.FLAG_DIRTY_TAGS)) {
				//TODO call may be inefficient for new node
				tags().populateData(changes);
			}
			if (data.isFlagged(PartData.FLAG_DIRTY_NAMES | PartData.FLAG_DIRTY_VALUES)) {
				//TODO call may be inefficient for new node
				attrs().populateData(changes);
			}
		}
		recordClean();
	}

	void recordDirty(int flag) {
		data.setFlag(flag);
		if ((flags & FLAG_RECORDED_DIRTY) == 0) {
			visit.recordDirty(this);
			flags |= FLAG_RECORDED_DIRTY;
		}
	}

	void recordClean() {
		if ((flags & FLAG_RECORDED_DIRTY) != 0) {
			data.clearDirtyFlags();
			visit.recordClean(this);
			flags &= ~FLAG_RECORDED_DIRTY;
		}
	}

	Type newType(String typeName) {
		return visit.view.type(typeName);
	}

	boolean added() {
		return data.isFlagged(PartData.FLAG_DIRTY_OWNER);
	}

	void checkNotDeleted() {
		if (deleted()) throw new ConstraintException(ConstraintException.Type.DELETED_PART);
	}

	void checkModifiable() {
		visit.checkMutable();
		if (!modifiable()) throw new ConstraintException(ConstraintException.Type.UNMODIFIABLE_PART);
	}

	void checkDeletable() {
		visit.checkMutable();
		if (!deletable()) throw new ConstraintException(ConstraintException.Type.UNDELETABLE_PART);
	}

	void checkMatchedGraph(Part that) {
		if (this.visit != that.visit) throw new IllegalArgumentException("not parts of the same graph");
	}

	void checkAvailable(Type type) {
		visit.checkAvailable(type);
	}

	StringBuilder toString(String prefix, StringBuilder sb) {
		//TODO include some form of graph identity
		sb.append(prefix).append("ID: ").append(id).append("\n");
		sb.append(prefix).append("Owner: ").append(owner()).append("\n");
		sb.append(prefix).append("Type: ").append(type()).append("\n");
		sb.append(prefix).append("Tags: ").append(tags().toStringImpl()).append("\n");
		sb.append(prefix).append("Attrs:\n");
		String p = prefix + "  ";
		attrs().asMap().forEach((k,v) -> {
			sb.append(p).append(k).append(" = ").append(v).append("\n");
		});
		sb.append(prefix).append("Flags: ").append(data.flags).append("\n"); //TODO format nicely
		return sb;
	}

	private boolean checkPerm(int flag, int metaFlag, Predicate<Permissions> check) {
		if ((flags & metaFlag) != 0) return (flags & flag) != 0;
		if (accessibleByViewer()) {
			flags |= FLAG_ALL_PERM;
			return true;
		}
		if (check.test(permissions())) {
			flags |= metaFlag | flag;
			return true;
		}
		flags |= metaFlag;
		return false;
	}

}
