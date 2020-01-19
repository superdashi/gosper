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
package com.superdashi.gosper.item;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public final class Item implements Serializable {

	private static final int FIELD_COUNT = 8;

	//NOTE: order dependency in these fields
	private static final Collect.Maps<String, Value> extrasMaps = Collect.setsOf(String.class).mappedTo(Value.class);
	private static final Item nothing = new Item();

	//TODO consider stronger constraint on valid keys - perhaps matching those of fields
	private static boolean isValidExtrasKey(String key) {
		return key.indexOf(':') != -1;
	}

	private static int appendFlag(int flags, boolean bit) {
		return (flags << 1) | (bit ? 1 : 0);
	}

	private static boolean hasFlag(int flags, int index) {
		return (flags & (1 << (FIELD_COUNT - 1 - index))) != 0;
	}

	public static Item fromLabel(String label) {
		return new Item(label, null, null, null, null, null, null, null);
	}

	public static Item fromPicture(Image picture) {
		return new Item(null, null, null, picture, null, null, null, null);
	}

	public static Item nothing() {
		return nothing;
	}

	public static Builder newBuilder() {
		return new Builder();
	}

	public static Item deserialize(ReadStream r) {
		Builder builder = new Builder();
		// fields
		int flags = r.readInt();
		if (hasFlag(flags, 0)) builder.label      (r.readChars()                                   );
		if (hasFlag(flags, 1)) builder.description(r.readChars()                                   );
		if (hasFlag(flags, 2)) builder.icon       (new Image(r.readChars())                        );
		if (hasFlag(flags, 3)) builder.picture    (new Image(r.readChars())                        );
		if (hasFlag(flags, 4)) builder.ordinal    (r.readLong()                                    );
		if (hasFlag(flags, 5)) builder.created    (Instant.ofEpochSecond(r.readLong(), r.readInt()));
		if (hasFlag(flags, 6)) builder.modified   (Instant.ofEpochSecond(r.readLong(), r.readInt()));
		if (hasFlag(flags, 7)) builder.priority   (Priority.valueOf(r.readInt())                   );
		// extras
		int size = r.readInt();
		for (int i = 0; i < size; i++) {
			builder.addExtra(r.readChars(), Value.deserialize(r));
		}
		// build
		Item item = builder.build();
		return item.isNothing() ? nothing : item;
	}

	public static class Builder {

		private EquivalenceMap<String, Value> extras;
		private String   label;
		private String   description;
		private Image    icon;
		private Image    picture;
		private Long     ordinal;
		private Instant  created;
		private Instant  modified;
		private Priority priority;
		private Qualifier qualifier;

		private Builder() {
			extras = extrasMaps.emptyMap();
			qualifier = Qualifier.universal();
		}

		private Builder(Item item) {
			extras = item.extras;
			label = item.label;
			description = item.description;
			icon = item.icon;
			picture = item.picture;
			ordinal = item.ordinal;
			created = item.created;
			modified = item.modified;
			priority = item.priority;
			qualifier = item.qualifier;
		}

		public Builder label(String label)             { this.label = label            ; return this; }
		public Builder description(String description) { this.description = description; return this; }
		public Builder icon(Image icon)                { this.icon = icon              ; return this; }
		public Builder picture(Image picture)          { this.picture = picture        ; return this; }
		public Builder ordinal(Long ordinal)           { this.ordinal = ordinal        ; return this; }
		public Builder created(Instant created)        { this.created = created        ; return this; }
		public Builder modified(Instant modified)      { this.modified = modified      ; return this; }
		public Builder priority(Priority priority)     { this.priority = priority      ; return this; }

		public Builder set(String key, Object value) {
			switch (key) {
			case "label"       : label       = Value.fromObject(value).as(Value.Type.STRING  ).optionalString()  .orElse(null); break;
			case "description" : description = Value.fromObject(value).as(Value.Type.STRING  ).optionalString()  .orElse(null); break;
			case "icon"        : icon        = Value.fromObject(value).as(Value.Type.IMAGE   ).optionalImage()   .orElse(null); break;
			case "picture"     : picture     = Value.fromObject(value).as(Value.Type.IMAGE   ).optionalImage()   .orElse(null); break;
			case "ordinal"     : ordinal     = Value.fromObject(value).as(Value.Type.INTEGER ).optionalInteger() .orElse(null); break;
			case "created"     : created     = Value.fromObject(value).as(Value.Type.INSTANT ).optionalInstant() .orElse(null); break;
			case "modified"    : modified    = Value.fromObject(value).as(Value.Type.INSTANT ).optionalInstant() .orElse(null); break;
			case "priority"    : priority    = Value.fromObject(value).as(Value.Type.PRIORITY).optionalPriority().orElse(null); break;
			default:
				if (isValidExtrasKey(key)) {
					Value v = Value.fromObject(value);
					if (v.isEmpty()) {
						extras().remove(key);
					} else {
						extras().put(key, v);
					}
				}
			}
			return this;
		}

		public Builder addExtra(String key, Value value) {
			if (key == null) throw new IllegalArgumentException("null key");
			if (!isValidExtrasKey(key)) throw new IllegalArgumentException("invalid key");
			if (value == null) throw new IllegalArgumentException("null value");
			if (value.isEmpty()) {
				extras().remove(key);
			} else {
				extras().put(key, value);
			}
			return this;
		}

		public <V> Builder addMap(Map<String, V> map) {
			if (map == null) throw new IllegalArgumentException("null map");
			//TODO consider a coerce method on Value mapping objects to objects directly?
			Value.fromObject(map.get("label"      )).as(Value.Type.STRING  ).optionalString()  .ifPresent(v -> label       = v);
			Value.fromObject(map.get("description")).as(Value.Type.STRING  ).optionalString()  .ifPresent(v -> description = v);
			Value.fromObject(map.get("icon"       )).as(Value.Type.IMAGE   ).optionalImage()   .ifPresent(v -> icon        = v);
			Value.fromObject(map.get("picture"    )).as(Value.Type.IMAGE   ).optionalImage()   .ifPresent(v -> picture     = v);
			Value.fromObject(map.get("ordinal"    )).as(Value.Type.INTEGER ).optionalInteger() .ifPresent(v -> ordinal     = v);
			Value.fromObject(map.get("created"    )).as(Value.Type.INSTANT ).optionalInstant() .ifPresent(v -> created     = v);
			Value.fromObject(map.get("modified"   )).as(Value.Type.INSTANT ).optionalInstant() .ifPresent(v -> modified    = v);
			Value.fromObject(map.get("priority"   )).as(Value.Type.PRIORITY).optionalPriority().ifPresent(v -> priority    = v);
			addExtras(map);
			return this;
		}

		public Builder addItem(Item item) {
			if (item == null) throw new IllegalArgumentException("null item");
			if (!qualifier.matches(item.qualifier)) throw new IllegalArgumentException("umatched qualifier");
			qualifier = item.qualifier;
			if (item.label       != null) label       = item.label      ;
			if (item.description != null) description = item.description;
			if (item.icon        != null) icon        = item.icon       ;
			if (item.picture     != null) picture     = item.picture    ;
			if (item.ordinal     != null) ordinal     = item.ordinal    ;
			if (item.created     != null) created     = item.created    ;
			if (item.modified    != null) modified    = item.modified   ;
			if (item.priority    != null) priority    = item.priority   ;
			extras().putAll(item.extras());
			return this;
		}

		public Builder qualifyWith(Qualifier qualifier) {
			if (qualifier == null) throw new IllegalArgumentException("null qualifier");
			this.qualifier = qualifier;
			return this;
		}

		public Builder qualifyWithLang(Locale lang) {
			qualifier = qualifier.withLang(lang);
			return this;
		}

		public Builder qualifyWithLang(String lang) {
			qualifier = qualifier.withLang(lang);
			return this;
		}

		public Builder qualifyWithScreen(ScreenClass screen) {
			qualifier = qualifier.withScreen(screen);
			return this;
		}

		public Builder qualifyWithColor(ScreenColor color) {
			qualifier = qualifier.withColor(color);
			return this;
		}

		public Builder qualifyWithFlavor(Flavor flavor) {
			qualifier = qualifier.withFlavor(flavor);
			return this;
		}

		public Item build() {
			extras = extras.isEmpty() ? extrasMaps.emptyMap() : extras.immutableView();
			return new Item(this);
		}

		private <V> void addExtras(Map<String, V> map) {
			map.forEach((k,v) -> {
				if (k == null || !isValidExtrasKey(k)) return;
				Value.fromObject(v).ifNotEmpty(value -> extras().put(k, value));
			});
		}

		private EquivalenceMap<String, Value> extras() {
			if (!extras.isMutable()) extras = extras.mutableCopy();
			return extras;
		}

	}

	private final EquivalenceMap<String, Value> extras;
	private final String   label;
	private final String   description;
	private final Image    icon;
	private final Image    picture;
	private final Long     ordinal;
	private final Instant  created;
	private final Instant  modified;
	private final Priority priority;
	private final Qualifier qualifier;

	private Item() {
		extras = extrasMaps.emptyMap();
		label       = null;
		description = null;
		icon        = null;
		picture     = null;
		ordinal     = null;
		created     = null;
		modified    = null;
		priority    = null;
		qualifier = Qualifier.universal();
	}

	private Item(
			String   label,
			String   description,
			Image    icon,
			Image    picture,
			Long     ordinal,
			Instant  created,
			Instant  modified,
			Priority priority
			) {
		this.extras = extrasMaps.emptyMap();
		this.label       = label      ;
		this.description = description;
		this.icon        = icon       ;
		this.picture     = picture    ;
		this.ordinal     = ordinal    ;
		this.created     = created    ;
		this.modified    = modified   ;
		this.priority    = priority   ;
		qualifier = Qualifier.universal();
	}

	private Item(Builder builder) {
		this.extras = builder.extras;
		this.label       = builder.label      ;
		this.description = builder.description;
		this.icon        = builder.icon       ;
		this.picture     = builder.picture    ;
		this.ordinal     = builder.ordinal    ;
		this.created     = builder.created    ;
		this.modified    = builder.modified   ;
		this.priority    = builder.priority   ;
		this.qualifier = builder.qualifier;
	}

	public Map<String, Value> extras() {
		return extras;
	}

	public Optional<String>   label()       { return Optional.ofNullable(label);       }
	public Optional<String>   description() { return Optional.ofNullable(description); }
	public Optional<Image>    icon()        { return Optional.ofNullable(icon);        }
	public Optional<Image>    picture()     { return Optional.ofNullable(picture);     }
	public Optional<Long>     ordinal()     { return Optional.ofNullable(ordinal);     }
	public Optional<Instant>  created()     { return Optional.ofNullable(created);     }
	public Optional<Instant>  modified()    { return Optional.ofNullable(modified);    }
	public Optional<Priority> priority()    { return Optional.ofNullable(priority);    }

	// never returns null
	public Value value(String valueName) {
		if (valueName == null) throw new IllegalArgumentException("null valueName");

		// extras guaranteed to contain keys with colons, so won't inadvertently match core field names
		Value value = extras.get(valueName);
		if (value != null) return value;

		// item field case
		switch (valueName) {
		case "label"      : return Value.ofString  (label      );
		case "description": return Value.ofString  (description);
		case "icon"       : return Value.ofImage   (icon       );
		case "picture"    : return Value.ofImage   (picture    );
		case "ordinal"    : return Value.ofInteger (ordinal    );
		case "created"    : return Value.ofInstant (created    );
		case "modified"   : return Value.ofInstant (modified   );
		case "priority"   : return Value.ofPriority(priority   );
		default: return Value.empty();
		}
	}

	public Qualifier qualifier() {
		return qualifier;
	}

	public boolean isNothing() {
		return
				extras.isEmpty() &&
				label       == null &&
				description == null &&
				icon        == null &&
				picture     == null &&
				ordinal     == null &&
				created     == null &&
				modified    == null &&
				priority    == null;
	}

	public Info asMeta() {
		return new Info(this);
	}

	public Info asMetaFor(List<Item> items) {
		if (items == null) throw new IllegalArgumentException("null items");
		if (items.isEmpty()) return asMeta();
		Item[] array = new Item[items.size()];
		items.toArray(array);
		return asMetaFor(array);
	}

	public Info asMetaFor(Item... items) {
		for (int i = 0; i < items.length; i++) {
			Item item = items[i];
			if (item == null) throw new IllegalArgumentException("null item");
		}
		return new Info(this, items);
	}

	public void serialize(WriteStream w) {
		// identify populated fields
		int flags = 0;
		flags = appendFlag(flags, label       != null);
		flags = appendFlag(flags, description != null);
		flags = appendFlag(flags, icon        != null);
		flags = appendFlag(flags, picture     != null);
		flags = appendFlag(flags, ordinal     != null);
		flags = appendFlag(flags, created     != null);
		flags = appendFlag(flags, modified    != null);
		flags = appendFlag(flags, priority    != null);
		w.writeInt(flags);
		// write fields
		if (label       != null)   w.writeChars(label);
		if (description != null)   w.writeChars(description);
		if (icon        != null)   w.writeChars(icon.uri().toString());
		if (picture     != null)   w.writeChars(picture.uri().toString());
		if (ordinal     != null)   w.writeLong(ordinal);
		if (created     != null) { w.writeLong(created.getEpochSecond()); w.writeInt(created.getNano()); };
		if (modified    != null) { w.writeLong(modified.getEpochSecond()); w.writeInt(modified.getNano()); };
		if (priority    != null)   w.writeInt(priority.ordinal());
		// write extras
		w.writeInt(extras.size());
		extras.forEach((k,v) -> {
			w.writeChars(k);
			v.serialize(w);
		});
	}

	public Builder builder() {
		return new Builder(this);
	}

	//TODO needs hashCode method

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Item)) return false;
		Item that = (Item) obj;
		if (!Objects.equals(this.label,       that.label      )) return false;
		if (!Objects.equals(this.description, that.description)) return false;
		if (!Objects.equals(this.icon,        that.icon       )) return false;
		if (!Objects.equals(this.picture,     that.picture    )) return false;
		if (!Objects.equals(this.ordinal,     that.ordinal    )) return false;
		if (!Objects.equals(this.created,     that.created    )) return false;
		if (!Objects.equals(this.modified,    that.modified   )) return false;
		if (!Objects.equals(this.priority,    that.priority   )) return false;
		return this.extras.equals(that.extras) && this.qualifier.equals(that.qualifier);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append(sb, "label"      , label      );
		append(sb, "description", description);
		append(sb, "icon"       , icon       );
		append(sb, "picture"    , picture    );
		append(sb, "ordinal"    , ordinal    );
		append(sb, "created"    , created    );
		append(sb, "modified"   , modified   );
		append(sb, "priority"   , priority   );
		if (!extras.isEmpty()) {
			if (sb.length() != 0) sb.append(' ');
			sb.append("extras: ").append(extras);
		}
		if (!qualifier.isUniversal()) {
			if (sb.length() != 0) sb.append(' ');
			sb.append(qualifier);
		}
		return sb.length() == 0 ? "<empty>" : sb.toString();
	}

	private void append(StringBuilder sb, String label, Object value) {
		if (value == null) return;
		if (sb.length() != 0) sb.append(", ");
		sb.append(label).append("=").append(value);
	}

	// serialization

	private Object writeReplace() throws ObjectStreamException {
		return new Serial(this);
	}

	// intermediary class used so that serialization format can change between releases
	private static final class Serial implements Serializable {

		private Item item;

		Serial(Item item) {
			this.item = item;
		}

		private void readObject(ObjectInputStream in) throws IOException {
			item = Item.deserialize(Streams.streamInput(in));
		}

		private void writeObject(ObjectOutputStream out) throws IOException {
			item.serialize(Streams.streamOutput(out));
		}

		private Object readResolve() throws ObjectStreamException {
			//TODO needs to validate item
			return item;
		}
	}
}
