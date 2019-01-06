package com.superdashi.gosper.graphdb;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.item.Value.Type;

public final class Attrs {

	private final Part part;
	private final TreeMap<AttrName, Value> map;
	private final boolean accessible;
	private TreeMap<AttrName, Value> orig = null; // used to identify changes
	private AttrMap attrMap = null; // lazily created

	Attrs(Part part) {
		assert part != null;
		this.part = part;
		accessible = part.accessibleByViewer();
		map = part.data.extractValueMap(part);
	}

	public int count() {
		return accessible ? map.size() : (int) map.keySet().stream().filter(Attrs.this::isVisible).count();
	}

	public Map<AttrName, Value> asMap() {
		if (attrMap == null) {
			attrMap = new AttrMap();
		}
		return attrMap;
	}

	// accessors
	public Value get(AttrName attrName) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		return getImpl(attrName);
	}

	public Value get(String name) {
		return getImpl( attrName(name) );
	}

	//TODO check mutability (& permissions!!)
	public void set(AttrName attrName, Value value) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		if (value == null) throw new IllegalArgumentException("null value");
		part.checkNotDeleted();
		part.checkModifiable();
		setImpl(attrName, value);
	}

	public void set(String name, Value value) {
		set(attrName(name), value);
	}
	// direct type getters

	public String string(AttrName attrName) {
		return get(attrName).string();
	}

	public String string(String name) {
		return get(name).string();
	}

	public long integer(AttrName attrName) {
		return get(attrName).integer();
	}

	public long integer(String name) {
		return get(name).integer();
	}

	public double number(AttrName attrName) {
		return get(attrName).number();
	}

	public double number(String name) {
		return get(name).number();
	}

	public Image image(AttrName attrName) {
		return get(attrName).image();
	}

	public Image image(String name) {
		return get(name).image();
	}

	public Instant instant(AttrName attrName) {
		return get(attrName).instant();
	}

	public Instant instant(String name) {
		return get(name).instant();
	}

	//TODO other types

	// direct type setters

	public void empty(AttrName attrName) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		part.checkModifiable();
		part.checkNotDeleted();
		setImpl(attrName, Value.empty());
	}

	public void string(AttrName attrName, String value) {
		set(attrName, Value.ofString(value));
	}

	public void string(String name, String value) {
		set(name, Value.ofString(value));
	}

	public void integer(AttrName attrName, long value) {
		set(attrName, Value.ofInteger(value));
	}

	public void integer(String name, long value) {
		set(name, Value.ofInteger(value));
	}

	public void number(AttrName attrName, double value) {
		set(attrName, Value.ofNumber(value));
	}

	public void number(String name, double value) {
		set(name, Value.ofNumber(value));
	}

	public void image(AttrName attrName, Image image) {
		set(attrName, Value.ofImage(image));
	}

	public void image(String name, Image image) {
		set(name, Value.ofImage(image));
	}

	public void instant(AttrName attrName, Instant instant) {
		set(attrName, Value.ofInstant(instant));
	}

	public void instant(String name, Instant instant) {
		set(name, Value.ofInstant(instant));
	}

	//TODO other types

	// package methods

	void populateData(List<Change> changes) {
		part.data.applyValueMap(map, part, changes);
		if (orig != null) {
			int sourceId = part.sourceId();
			int edgeId = part.edgeId();
			part.visit.indexTypes.forEach((n,t) -> {
				Value newValue = map.get(n);
				Value oldValue = orig.get(n);
				if (newValue == oldValue) {
					/* cannot be any change */
				} else if (t == Value.Type.EMPTY) { // special case - index just wants presence
					long attrId = part.visit.idForAttrName(n);
					if (newValue == null) { // must be a removal
						ValueKey.attr(sourceId, edgeId, attrId, Value.empty(), null).record(changes);
					} else if (oldValue == null) { // must be an addition
						ValueKey.attr(sourceId, edgeId, attrId, null, Value.empty()).record(changes);
					}
				} else { // we need to coerce the types to check for an indexable value
					if (newValue != null) {
						newValue = newValue.as(t);
						if (newValue.isEmpty()) {
							newValue = null;
						}
					}
					if (oldValue != null) {
						oldValue = oldValue.as(t);
						if (oldValue.isEmpty()) {
							oldValue = null;
						}
					}
					if (Objects.equals(newValue, oldValue)) {
						/* no change */
					} else {
						long attrId = part.visit.idForAttrName(n);
						ValueKey.attr(sourceId, edgeId, attrId, oldValue, newValue).record(changes);
					}
				}
			});
			orig = null;
		}
	}

	// private helper methods

	private Value getImpl(AttrName attrName) {
		Value value = map.get(attrName);
		if (value == null) {
			value = Value.empty();
		} else {
			Type type = part.visit.types.get(attrName);
			if (type != null) value = value.as(type);
		}
		return value.isEmpty() ? part.visit.defaults.getOrDefault(attrName, Value.empty()) : value;
	}

	private void setImpl(AttrName attrName, Value value) {
		// coerce typed attribute
		if (!value.isEmpty()) {
			 Type type = part.visit.types.get(attrName);
			 if (type != null) {
				 value = value.as(type);
				 if (value.isEmpty()) throw new ConstraintException(ConstraintException.Type.INCOMPATIBLE_TYPE, "cannot convert supplied value into " + type + " for attribute " + attrName);
			 }
		}
		// ensure we have a copy of the original values
		if (orig == null) orig = new TreeMap<>(map);
		// apply changes
		int flag;
		if (value.isEmpty()) {
			flag = map.remove(attrName) == null ? 0 : PartData.FLAG_DIRTY_NAMES; // because name has been removed
		} else {
			Value previous = map.put(attrName, value);
			if (previous == null) {
				flag = PartData.FLAG_DIRTY_NAMES; // because name has been added
			} else {
				flag = previous.equals(value) ? 0 : PartData.FLAG_DIRTY_VALUES; // because value has changed
			}
		}
		if (flag != 0) part.recordDirty(flag);
	}

	private AttrName attrName(String name) {
		return part.visit.view.attrName(name);
	}

	private boolean isVisible(AttrName attrName) {
		//TODO this needs to be controllable on an attribute-by-attribute basis by changing true
		return accessible || true;
	}

	// inner classes

	private final class AttrMap extends AbstractMap<AttrName, Value> {

		private final Set<Map.Entry<AttrName, Value>> entrySet = new AbstractSet<Map.Entry<AttrName,Value>>() {

			@Override
			public Iterator<java.util.Map.Entry<AttrName, Value>> iterator() {
				//TODO needs to wrap entry to control setting value
				if (accessible) return map.entrySet().iterator();
				return map.entrySet().stream().filter(e -> isVisible(e.getKey())).iterator();
			}

			@Override
			public int size() {
				return count();
			}

		};

		@Override
		public Set<Map.Entry<AttrName, Value>> entrySet() {
			return entrySet;
		}

		@Override
		public Value put(AttrName key, Value value) {
			Value old = get(key);
			set(key, value);
			return old;
		}

		@Override
		public Value get(Object key) {
			if (!(key instanceof AttrName)) return null;
			Value value = getImpl( (AttrName) key );
			return value.isEmpty() ? null : value;
		}
	}

}
