package com.superdashi.gosper.micro;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;

public final class FormModel extends Model {

	// statics

	private static final FieldModel[] NO_FIELDS = {};
	private static final Action[] NO_ACTIONS = {};
	private static final Maps<String, FieldModel> nameMaps = Collect.setsOf(String.class).mappedTo(FieldModel.class);
	private static final FormModel empty = new FormModel(null);

	static FormModel empty() { return empty; }

	// static inner classes

	public enum FieldType {

		STRING  (Value.Type.STRING),
		PASSWORD(Value.Type.STRING),
		SELECT  (Value.Type.STRING);

		final Value.Type valueType;

		private FieldType(Value.Type valueType) {
			this.valueType = valueType;
		}

	}

	// fields

	private final Mutations mutations;
	private final Map<String, FieldModel> fieldsByName;
	private FieldModel[] fields;
	private int[] nonEmpty = null; // indices of non-empty fields

	// constructors

	FormModel(ActivityContext context) {
		super(context);
		this.mutations = new Mutations();
		fieldsByName = nameMaps.newMap();
		fields = NO_FIELDS;
	}

	// public methods

	// modifying

	public FieldModel addField(String name, FieldType type, Item info) {
		if (name == null) throw new IllegalArgumentException("null name");
		if (name.isEmpty()) throw new IllegalArgumentException("empty name");
		if (fieldsByName.containsKey(name)) throw new IllegalArgumentException("duplicate name");
		if (type == null) throw new IllegalArgumentException("null type");
		if (info == null) throw new IllegalArgumentException("null info");
		if (isSnapshot()) throw new IllegalStateException("snapshot");
		FieldModel field = new FieldModel(name, type, models().itemModel(info, mutations));
		int oldLength = fields.length;
		fields = Arrays.copyOf(fields, oldLength + 1);
		fields[oldLength] = field;
		fieldsByName.put(name, field);
		field.index = oldLength;
		updateNonEmpty();
		mutations.count++;
		requestRedraw();
		return field;
	}

	public void sortFields(Comparator<FieldModel> order) {
		if (order == null) throw new IllegalArgumentException("null order");
		try {
			Arrays.sort(fields);
		} finally {
			for (int i = 0; i < fields.length; i++) {
				fields[i].index = i;
			}
			updateNonEmpty();
			mutations.count++;
			requestRedraw();
		}
	}

	// public access to fields

	public long revision() {
		return mutations.count;
	}

	public boolean isEmpty() {
		return fields.length == 0;
	}

	public int fieldCount() {
		return fields.length;
	}

	public List<FieldModel> fields() {
		return Collections.unmodifiableList(Arrays.asList(fields));
	}

	public List<String> fieldNames() {
		return new AbstractList<String>() {
			@Override public String get(int index) { return fields[index].name; }
			@Override public int size() { return fields.length; }
		};
	}

	public FieldModel fieldWithName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		FieldModel field = fieldsByName.get(name);
		if (field == null) throw new IllegalArgumentException("unknown field name");
		return field;
	}

	public FieldModel fieldAtIndex(int index) {
		if (index < 0) throw new IllegalArgumentException("negative index");
		if (index >= fields.length) throw new IllegalArgumentException("invalid index");
		return fields[index];
	}

	// views

	public Map<String, Value> asMap() {
		ensureNonEmpty();
		return new AbstractMap<String, Value>() {

			@Override
			public Value get(Object key) {
				if (!(key instanceof String)) return null;
				String name = (String) key;
				FieldModel field = fieldsByName.get(name);
				if (field == null) return null;
				Value value = field.value;
				return value.isEmpty() ? null : value;
			}

			@Override
			public Set<java.util.Map.Entry<String, Value>> entrySet() {
				return new AbstractSet<Map.Entry<String,Value>>() {

					@Override
					public boolean contains(Object key) {
						if (!(key instanceof String)) return false;
						String name = (String) key;
						FieldModel field = fieldsByName.get(name);
						return field != null && !field.value.isEmpty();
					}

					@Override
					public Iterator<Map.Entry<String, Value>> iterator() {
						return Arrays.stream(nonEmpty).mapToObj(i -> entry(i)).iterator();
					}

					@Override
					public int size() {
						return nonEmpty.length;
					}
				};
			}
		};
	}

	// private helper methods

	private void remove(FieldModel field) {
		int index = field.index;
		int oldLength = fields.length;
		int newLength = oldLength - 1;
		// primary removal
		FieldModel[] newFields = new FieldModel[newLength];
		if (index != 0) System.arraycopy(fields, 0, newFields, 0, index);
		if (index != newLength) System.arraycopy(fields, index + 1, newFields, index, newLength - index);
		fields = newFields;
		// notify field
		field.index = -1;
		// notify possible map
		updateNonEmpty();
		// notify observers
		mutations.count++;
		requestRedraw();
	}

	private void ensureNonEmpty() {
		if (nonEmpty != null) return; // we've already done the work
		computeNonEmpty();
	}

	private void updateNonEmpty() {
		if (nonEmpty == null) return; // we haven't already computed it, so no update needed
		computeNonEmpty();
	}

	private void computeNonEmpty() {
		// count up the number of non-empty (ie. non-null) values
		int count = 0;
		for (FieldModel field : fields) {
			if (!field.value.isEmpty()) count++;
		}
		// then map to the array
		nonEmpty = new int[count];
		for (int j = 0, i = 0; j < count; j++) {
			for (i++; fields[i].value.isEmpty(); i++);
			nonEmpty[j] = i;
		}
	}

	// must have ensured nonEmpty is populated
	//TODO could make live over field object
	private Map.Entry<String, Value> entry(int index) {
		FieldModel field = fieldAtIndex(nonEmpty[index]);
		return new AbstractMap.SimpleImmutableEntry<>(field.name, field.value);
	}

	// inner classes

	public class FieldModel {

		public final String name;
		public final FieldType type;
		public final ItemModel info;
		private int index;
		private Value value = Value.empty();
		private boolean readOnly = false;
		private Regex regex = null;
		//TODO what is the advantage of keeping actions as models?
		private ActionsModel options = null;

		FieldModel(String name, FieldType type, ItemModel info) {
			this.name = name;
			this.type = type;
			this.info = info;
		}

		// public accessors

		public FormModel form() {
			return FormModel.this;
		}

		public int index() {
			return index;
		}

		void index(int index) {
			this.index = index;
		}

		public Value value() {
			return value;
		}

		public void value(Value value) {
			if (value == null) throw new IllegalArgumentException("null value");
			if (this.value.equals(value)) return;
			this.value = value;
			mutations.count++;
			requestRedraw();
		}

		public boolean readOnly() {
			return readOnly;
		}

		public void readOnly(boolean readOnly) {
			if (this.readOnly == readOnly) return;
			this.readOnly = readOnly;
			mutations.count++;
			requestRedraw();
		}

		public Optional<Regex> regex() {
			return Optional.ofNullable(regex);
		}

		public void regex(Regex regex) {
			if (Objects.equals(this.regex, regex)) return;
			this.regex = regex;
			mutations.count++;
			requestRedraw();
		}

		public void options(Action... options) {
			this.options = options == null ? null : models().actionsModel(options);
		}

		public Action[] options() {
			//TODO want something nicer than this
			return options == null ? NO_ACTIONS : options.actionsWithAvailability(true).stream().map(ActionModel::action).toArray(Action[]::new);
		}

		public void selectIndex(int index) {
			if (options == null) return;
			List<ActionModel> list = options.actionsWithAvailability(true);
			value( index < 0 || index >= list.size() ? Value.empty() : Value.ofString(list.get(index).action().id) );
		}

		public Optional<Action> selectedOption() {
			if (options == null) return Optional.empty();
			return value.optionalString().map(id -> options.modelWithId(id)).map(ActionModel::action);
		}

		public int selectedIndex() {
			if (options == null) return -1;
			return value.optionalString().map(id -> options.modelWithId(id)).map(m -> options.modelIndex(m)).orElse(-1);
		}

		// public methods

		public void remove() {
			if (index == -1) return;
			FormModel.this.remove(this);
		}

		// object methods

		@Override
		public String toString() {
			return name + " " + type + " " + value + " " + info;
		}
	}

}
