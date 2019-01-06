package com.superdashi.gosper.graphdb;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;

import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Value;

//TODO enforce max of 256 types & document present limitation
//TODO rename to Data?
final class PartData implements Cloneable {

	static final long     NO_TYPE       =    -1L       ;
	static final int   [] NO_PERMS      = new int   [0];
	static final long  [] NO_TAGS       = new long  [0];
	static final long  [] NO_NAMESPACES = new long  [0];
	static final int   [] NO_NAMES      = new int   [0];
	static final Value [] NO_VALUES     = new Value [0];
	static final String[] NO_STRINGS    = new String[0];

	static final int FLAG_DIRTY_OWNER   = 0b000000001; // indicates 'new'
	static final int FLAG_DIRTY_TYPE    = 0b000000010;
	static final int FLAG_DIRTY_PERMS   = 0b000000100;
	static final int FLAG_DIRTY_TAGS    = 0b000001000;
	static final int FLAG_DIRTY_NAMES   = 0b000010000; // names implies values, but flags are treated separately
	static final int FLAG_DIRTY_VALUES  = 0b000100000;
	static final int FLAG_DIRTY_STRINGS = 0b001000000;
	static final int FLAG_DIRTY_NODES   = 0b010000000; // edges only
	static final int FLAG_ALL_DIRTY     = 0b011111111;
	static final int FLAG_DELETED       = 0b100000000; //TODO consider whether this is the best way

	// the code of the owning namespace
	final long owner;
	// high int is namespace code, lower int is name code
	long type;
//TODO reflect in size estimate
	// the permissions, first integer consists of two shorts: 1st is visiblity perm count, 2nd is mutability count, number of delete is inferred
	int[] perms;
	// high int is namespace code, lower int is name code
	long[] tags;
	// high int is namespace code, lower int points to offset in names
	long[] namespaces;
	// +ve int is name id -ve is index in strings
	int[] names;
	// indices of values match those of the names
	Value[] values;
	// accumulates strings
	String[] strings;

	int flags = 0;

	PartData(long ownerId) {
		owner = ownerId;
		tags = NO_TAGS;
		type = NO_TYPE;
		perms = NO_PERMS;
		namespaces = NO_NAMESPACES;
		names = NO_NAMES;
		values = NO_VALUES;
		strings = NO_STRINGS;
	}

	PartData(ByteBuffer b) {
		// read owner
		long owner = b.getLong();

		// read type
		long type = b.getLong();

		// read perms
		int[] perms;
		{
			int vs = DataUtils.readVarInt(b);
			int ms = DataUtils.readVarInt(b);
			int ds = DataUtils.readVarInt(b);
			int len = 1 + vs + ms + ds;
			if (len == 1) {
				perms = NO_PERMS;
			} else {
				perms = new int[len];
				perms[0] = vs << 16 | ms & 0xffff;
				for (int i = 1; i < len; i++) {
					perms[i] = b.getInt();
				}
			}
		}

		// read tags
		int tagCount = b.get() & 0xff;
		long[] tags = new long[tagCount];
		for (int i = 0; i < tagCount; i++) {
			tags[i] = b.getLong();
		}

		// read attr data
		int nsCount = b.getInt();
		int nmCount = b.getInt();
		int stCount = b.getInt();
		long[] namespaces = new long[nsCount];
		int[] names = new int[nmCount];
		Value[] values = new Value[nmCount];
		String[] strings = new String[stCount];
		int nsi = 0; // index into namespaces;
		int sti = 0; // index into strings;
		for (int nmi = 0; nmi < names.length;) { // index into names
			int i = b.getInt();
			if (Space.isNsCode(i)) {
				namespaces[nsi++] = (long)i<<32 | (long)nmi & 0xffffffffL;
			} else {
				if (i < 0) {
					int len = (-i - 1) /2;
					String st = DataUtils.readString(b, len);
					strings[sti] = st;
					names[nmi] = -sti - 1;
					sti++;
				} else {
					names[nmi] = i;
				}
				values[nmi++] = GraphUtil.readValue(b);
			}
		}

		GraphUtil.checkMarker(b);

		// populate object
		this.owner = owner;
		this.type = type;
		this.perms = perms;
		this.tags = tags;
		this.namespaces = namespaces;
		this.names = names;
		this.values = values;
		this.strings = strings;
	}

	void write(WriteBuffer b) {
		// write owner
		b.putLong(owner);

		// write type
		b.putLong(type);

		// write perms
		if (perms.length < 2) {
			// there are no perms
			b.putVarInt(0);
			b.putVarInt(0);
			b.putVarInt(0);
		} else {
			int vs = perms[0] >> 16 & 0xffff;
			int ms = perms[0]       & 0xffff;
			int ds = perms.length - vs - ms - 1;
			b.putVarInt(vs);
			b.putVarInt(ms);
			b.putVarInt(ds);
			for (int i = 1; i < perms.length; i++) {
				b.putInt(perms[i]);
			}
		}

		// write tags
		b.put((byte) tags.length);
		for (long tag : tags) {
			b.putLong(tag);
		}

		// write attrs
		b.putInt(namespaces.length);
		b.putInt(names.length);
		b.putInt(strings.length);

		int from = 0; // index into names
		for (int nsi = 0; nsi < namespaces.length; nsi++) {
			b.putInt((int) (namespaces[nsi] >> 32)); // write namespace code
			int to;
			if (nsi + 1 < namespaces.length) {
				to = (int) namespaces[nsi + 1];
			} else {
				to = names.length;
			}
			while (from < to) {
				int i = names[from]; // name code or (neg) strings offset
				if (i < 0) { // string
					String st = strings[-i - 1];
					int len = st.length();
					b.putInt(-len * 2 - 1); // record string length
					b.putStringData(st, len); // record string
				} else { // name
					b.putInt(i);
				}
				GraphUtil.writeValue(b, values[from]);
				from++;
			}
		}

		GraphUtil.writeMarker(b);
	}

	void setFlag(int flag) {
		flags |= flag;
	}

	void clearFlag(int flag) {
		flags &= ~flag;
	}

	void clearDirtyFlags() {
		flags &= ~FLAG_ALL_DIRTY;
	}

	boolean isFlagged(int flag) {
		return (flags & flag) != 0;
	}

	Type extractType(Part part) {
		if (type == NO_TYPE) return null;
		Visit visit = part.visit;
		Map<Integer, Namespace> nssByCode = visit.nssByCode;
		Map<Integer, String> typesByCode = visit.typesByCode;
		Namespace ns = nssByCode.get(Name.nsCode(type));
		String nm = typesByCode.get(Name.nmCode(type));
		return new Type(ns, nm);
	}

	void applyType(Type type, Part part, List<Change> changes) {
		// calculate new type id
		long newType;
		if (type == null) {
			newType = NO_TYPE;
		} else {
			Visit visit = part.visit;
			int nsc = visit.codesByNs.get(type.namespace);
			int nmc = visit.codesByType.get(type.name);
			newType = Name.nsnId(nsc, nmc);
		}

		// confirm modified
		long oldType = this.type;
		if (oldType == newType) return; // nothing to do

		// record changes
		int sourceId = part.sourceId();
		int edgeId = part.edgeId();
		if (oldType != NO_TYPE) new NSNKey(oldType, sourceId, edgeId).toTagRemoval().record(changes);
		if (newType != NO_TYPE) new NSNKey(newType, sourceId, edgeId).toTypeAddition().record(changes);

		// update type
		this.type = newType;
	}

	ArrayList<Tag> extractTagList(Part part) {
		if (tags == null) return new ArrayList<>();
		Visit visit = part.visit;
		ArrayList<Tag> list = new ArrayList<>(tags.length);
		for (int i = 0; i < tags.length; i++) {
			list.add(visit.tagForId(tags[i]));
		}
		return list;
	}

	void applyTagList(ArrayList<Tag> list, Part part, List<Change> changes) {
		Visit visit = part.visit;
		// build new array
		long[] newTags = new long[list.size()];
		for (int i = 0; i < newTags.length; i++) {
			newTags[i] = visit.idForTag(list.get(i));
		}
		Arrays.sort(newTags);

		// populate changes
		int sourceId = part.sourceId();
		int edgeId = part.edgeId();
		long[] oldTags = this.tags;
		if (oldTags.length == 0) { // no old tags
			for (long tag : newTags) {
				new NSNKey(tag, sourceId, edgeId).toTagAddition().record(changes);
			}
		} else if (newTags.length == 0) { // no new tags
			for (long tag : oldTags) {
				new NSNKey(tag, sourceId, edgeId).toTagRemoval().record(changes);
			}
		} else {
			int i = 0;
			int j = 0;
			while (i < oldTags.length && j < newTags.length) {
				long it = oldTags[i];
				long jt = newTags[j];
				if (it == jt) {
					i++;
					j++;
				} else if (it < jt) {
					new NSNKey(it, sourceId, edgeId).toTagRemoval().record(changes);
					i++;
				} else {
					new NSNKey(jt, sourceId, edgeId).toTagAddition().record(changes);
					j++;
				}
			}
			while (i < oldTags.length) {
				new NSNKey(oldTags[i++], sourceId, edgeId).toTagRemoval().record(changes);
			}
			while (j < newTags.length) {
				new NSNKey(newTags[j++], sourceId, edgeId).toTagAddition().record(changes);
			}
		}
		this.tags = newTags;
	}

	TreeMap<AttrName, Value> extractValueMap(Part part) {
		TreeMap<AttrName, Value> map = new TreeMap<>();
		if (names == null) return map;
		Map<Integer, Namespace> nssByCode = part.visit.nssByCode;
		Map<Integer, String> attrsByCode = part.visit.attrsByCode;

		int nextNs = 0; // the index of the next namespace within namespaces
		int nextNsIndex = 0; // the name index at which the next ns starts
		Namespace currentNs = null; // caches the current namespace
		for (int i = 0; i < names.length; i++) {
			if (i == nextNsIndex) {
				long l = namespaces[nextNs++];
				nextNsIndex = (int) l;
				currentNs = nssByCode.get( (int) (l >> 32) );
			}
			int code = names[i];
			String name;
			if (code < 0) {
				// look up from strings
				name = strings[-code - 1];
			} else {
				name = attrsByCode.get(code);
			}
			AttrName attrName = new AttrName(currentNs, name);
			Value value = values[i];
			if (value.isEmpty()) throw new ConstraintException(ConstraintException.Type.INTERNAL_ERROR, "empty value persisted");
			map.put(attrName, value);
		}
		return map;
	}

	//TODO doesn't currently compute changes - done in Attrs for now
	void applyValueMap(TreeMap<AttrName, Value> map, Part part, List<Change> changes) {
		Visit visit = part.visit;
		Map<String, Integer> codesByAttr = visit.codesByAttr;
		Map<Namespace, Integer> codesByNs = visit.codesByNs;

		int nmCount = map.size();
		int nsCount = 0;
		int stCount = 0;
		{
			Namespace lastNs = null;
			for (AttrName name : map.keySet()) {
				Namespace ns = name.namespace;
				if (!ns.equals(lastNs)) {
					nsCount ++;
					lastNs = ns;
				}
				if (!codesByAttr.containsKey(name.name)) stCount ++;
			}
		}
		namespaces = new long[nsCount];
		names = new int[nmCount];
		values = new Value[nmCount];
		strings = new String[stCount];

		Namespace lastNs = null;
		int nsi = 0;
		int sti = 0;
		int nmi = 0;
		for (Entry<AttrName, Value> entry : map.entrySet()) {
			AttrName name = entry.getKey();
			Namespace ns = name.namespace;
			if (!ns.equals(lastNs)) {
				int nmc = codesByNs.get(ns);
				namespaces[nsi] = (long)nmc<<32 | (long)nmi & 0xffffffffL;
				nsi++;
				lastNs = ns;
			}
			int nmc = codesByAttr.getOrDefault(name.name, -1);
			if (nmc < 0) {
				strings[sti] = name.name;
				names[nmi] = -sti - 1;
				sti++;
			} else {
				names[nmi] = nmc;
			}
			values[nmi] = entry.getValue();
			nmi++;
		}
	}

	Value value(int nsc, String nm) {
		int stc = -1;
		for (int i = 0; i < strings.length; i++) {
			if (strings[i].equals(nm)) {
				stc = i;
				break;
			}
		}
		if (stc == -1) return null; // no matching name
		return value(nsc, -stc - 1);
	}

	Value value(int nsc, int nmc) {
		for (int i = 0; i < namespaces.length; i++) {
			if ((int) (namespaces[i] >> 32) == nsc) {
				int from = (int) namespaces[i];
				int to = i + 1 < namespaces.length ? (int) namespaces[i + 1] : names.length;
				for (int j = from; j < to; j++) {
					if (names[j] == nmc) return values[j];
				}
				return null;
			}
		}
		return null;
	}

	int estByteSize() {
		return
			12 +
			GraphUtil.arraySize(tags      ) +
			GraphUtil.arraySize(namespaces) +
			GraphUtil.arraySize(names     ) +
			GraphUtil.arraySize(values    ) +
			GraphUtil.arraySize(strings   ) ;
	}

	@Override
	protected PartData clone() {
		try {
			return (PartData) super.clone();
		} catch (CloneNotSupportedException e) {
			// not possible
			throw new IllegalStateException(e);
		}
	}
}
