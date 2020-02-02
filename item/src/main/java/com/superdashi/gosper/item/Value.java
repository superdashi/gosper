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

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

public abstract class Value {

	// statics

	private static final double MAX_SAFE_INTEGER =  9007199254740991.0;
	private static final double MIN_SAFE_INTEGER = -9007199254740991.0;

	public enum Type {
		EMPTY   (void    .class, (a,b) -> 0                                         , (b,a) -> 0                                         ),
		STRING  (String  .class, (a,b) -> a.string().compareTo(b.string())          , (b,a) -> a.string().compareTo(b.string())          ),
		INTEGER (long    .class, (a,b) -> Long.compare(a.integer(), b.integer())    , (b,a) -> Long.compare(a.integer(), b.integer())    ),
		NUMBER  (double  .class, (a,b) -> Double.compare(a.number(), b.number())    , (b,a) -> Double.compare(a.number(), b.number())    ),
		INSTANT (Instant .class, (a,b) -> a.instant().compareTo(b.instant())        , (b,a) -> a.instant().compareTo(b.instant())        ),
		PRIORITY(Priority.class, (a,b) -> a.priority().compareTo(b.priority())      , (b,a) -> a.priority().compareTo(b.priority())      ),
		IMAGE   (Image   .class, (a,b) -> a.image().uri().compareTo(b.image().uri()), (b,a) -> a.image().uri().compareTo(b.image().uri()));

		private static final Type[] values = values();

		public static Type valueOf(int ordinal) {
			if (ordinal < 0 || ordinal >= values.length) throw new IllegalArgumentException("invalid ordinal");
			return values[ordinal];
		}

		public static Optional<Type> optionalValueOf(String str) {
			if (str == null) throw new IllegalArgumentException("null str");
			// done this way to avoid raising an IAE for invalid str
			switch (str) {
			case "EMPTY"   : return Optional.of(EMPTY   );
			case "STRING"  : return Optional.of(STRING  );
			case "INTEGER" : return Optional.of(INTEGER );
			case "NUMBER"  : return Optional.of(NUMBER  );
			case "INSTANT" : return Optional.of(INSTANT );
			case "PRIORITY": return Optional.of(PRIORITY);
			case "IMAGE"   : return Optional.of(IMAGE   );
			default: return Optional.empty();
			}
		}

		static Type forClass(Class<?> clss) {
			switch (clss.getName()) {
			case "long" : return INTEGER;
			case "java.lang.String" : return STRING;
			case "double" : return NUMBER;
			case "java.time.Instant" : return INSTANT;
			default:
				if (clss == Image.class) return IMAGE;
				if (clss == Priority.class) return Type.PRIORITY;
				return null;
			}
		}

		final Class<?> javaType;
		public final Comparator<Value> compareAscending;
		public final Comparator<Value> compareDescending;

		private Type(Class<?> javaType, Comparator<Value> compAsc, Comparator<Value> compDes) {
			this.javaType = javaType;
			this.compareAscending = compAsc;
			this.compareDescending = compDes;
		}

		public ValueOrder order() {
			return ValueOrder.order(this, true, false);
		}

		public ValueOrder order(boolean ascending, boolean emptyFirst) {
			return ValueOrder.order(this, ascending, emptyFirst);
		}

		boolean isEmpty()    { return this == EMPTY;    }
		boolean isString()   { return this == STRING;   }
		boolean isInteger()  { return this == INTEGER;  }
		boolean isNumber()   { return this == NUMBER;   }
		boolean isInstant()  { return this == INSTANT;  }
		boolean isPriority() { return this == PRIORITY; }
		boolean isImage()    { return this == IMAGE;    }

	}

	private static final Value EMPTY_VALUE = new Value() {
		@Override public Type type() { return Type.EMPTY; }
		@Override public Value as(Type type) { return this; }
		@Override public Object toObject()  { return null; }
		@Override public void serialize(WriteStream w) { w.writeByte((byte) Type.EMPTY.ordinal()); }
		@Override public String string()     { throw exception(); }
		@Override public long integer()      { throw exception(); }
		@Override public double number()     { throw exception(); }
		@Override public Instant instant()   { throw exception(); }
		@Override public Image image()       { throw exception(); }
		@Override public Priority priority() { throw exception(); }
		@Override public Value orElse(Value alternative) { if (alternative == null) throw new IllegalArgumentException("null alternative"); return alternative; }
		@Override public int hashCode() { return 0; }
		@Override public String toString() { return "<empty>"; }
		private NoSuchElementException exception() { return new NoSuchElementException("empty"); }
	};

	public static Value deserialize(ReadStream r) {
		Type type = Type.valueOf(r.readByte() & 0xff);
		switch (type) {
		case EMPTY:    return EMPTY_VALUE;
		case STRING:   { String s = r.readChars(); return s.isEmpty() ? EMPTY_VALUE : new StringValue(s); } // special case - empty strings disallowed
		case INTEGER:  return new IntegerValue(r.readLong());
		case NUMBER:   return new NumberValue(r.readDouble());
		case INSTANT:  return new InstantValue(Instant.ofEpochSecond(r.readLong(), r.readInt()));
		case PRIORITY: return new PriorityValue(Priority.valueOf(r.readInt()));
		case IMAGE:    return new ImageValue(new Image(r.readChars()));
		default: throw new IllegalArgumentException("Invalid type: " + type);
		}
	}

	// public statics

	public static Value empty() {
		return EMPTY_VALUE;
	}

	public static Value ofString(String value) {
		return value == null || value.isEmpty() ? EMPTY_VALUE : new StringValue(value);
	}

	public static Value ofInteger(long value) {
		return new IntegerValue(value);
	}

	public static Value ofInteger(Long value) {
		return value == null ? EMPTY_VALUE : new IntegerValue(value);
	}

	public static Value ofNumber(double value) {
		return new NumberValue(value);
	}

	public static Value ofNumber(Double value) {
		return value == null ? EMPTY_VALUE : new NumberValue(value);
	}

	public static Value ofInstant(Instant value) {
		return value == null ? EMPTY_VALUE : new InstantValue(value);
	}

	public static Value ofImage(Image value) {
		return value == null ? EMPTY_VALUE : new ImageValue(value);
	}

	public static Value ofPriority(Priority value) {
		return value == null ? EMPTY_VALUE : new PriorityValue(value);
	}

	public static Value fromObject(Object obj) {
		if (obj == null) return EMPTY_VALUE;
		switch (obj.getClass().getName()) {
		case "java.lang.String"   : return ofString( (String)    obj);
		case "java.lang.Long"     : return ofInteger ((Long)     obj);
		case "java.lang.Integer"  : return ofInteger ((Integer)  obj);
		case "java.lang.Double"   : return ofNumber  ((Double)   obj);
		case "java.lang.Float"    : return ofNumber  ((Float)    obj);
		case "java.time.Instant"  : return ofInstant ((Instant)  obj);
		case "com.superdashi.gosper.item.Priority"       : return ofPriority((Priority) obj);
		case "com.superdashi.gosper.item.Image"          : return ofImage   ((Image)    obj);
		// special case - pass through values
		case "com.superdashi.gosper.core.data.Item$Value": return (Value) obj;
		default:
			return EMPTY_VALUE;
		}
	}

	// public methods

	public abstract Value.Type type();

	public abstract Value as(Value.Type type);

	public abstract Object toObject();

	public abstract void serialize(WriteStream w);

	public boolean isEmpty() {
		return type() == Type.EMPTY;
	}

	public String string() {
		return as(Type.STRING).string();
	}

	public Optional<String> optionalString() {
		Value value = as(Type.STRING);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.string());
	}

	public long integer() {
		return as(Type.INTEGER).integer();
	}

	public Optional<Long> optionalInteger() {
		Value value = as(Type.INTEGER);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.integer());
	}

	public double number() {
		return as(Type.NUMBER).number();
	}

	public Optional<Double> optionalNumber() {
		Value value = as(Type.NUMBER);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.number());
	}

	public Instant instant() {
		return as(Type.INSTANT).instant();
	}

	public Optional<Instant> optionalInstant() {
		Value value = as(Type.INSTANT);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.instant());
	}

	public Image image() {
		return as(Type.IMAGE).image();
	}

	public Optional<Image> optionalImage() {
		Value value = as(Type.IMAGE);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.image());
	}

	public Priority priority() {
		return as(Type.PRIORITY).priority();
	}

	public Optional<Priority> optionalPriority() {
		Value value = as(Type.PRIORITY);
		return value.isEmpty() ? Optional.empty() : Optional.of(value.priority());
	}

	public void ifNotEmpty(Consumer<Value> consumer) {
		if (!isEmpty()) consumer.accept(this);
	}

	public Value orElse(Value alternative) {
		if (alternative == null) throw new IllegalArgumentException("null alternative");
		return this;
	}

	// object methods

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Value)) return false;
		Value that = (Value) obj;
		Value.Type type = this.type();
		if (that.type() != type) return false;
		switch (type) {
		case EMPTY: return true;
		case IMAGE: return this.image().equals(that.image());
		case INSTANT: return this.instant().equals(that.instant());
		case INTEGER: return this.integer() == that.integer();
		case NUMBER: return this.number() == that.number();
		case PRIORITY: return this.priority() == that.priority();
		case STRING: return this.string().equals(that.string());
		default: throw new IllegalStateException("no equality test for " + type);
		}
	}

	@Override
	public String toString() {
		return type() + ":" + as(Type.STRING).string();
	}

	// implementations

	static final class StringValue extends Value {

		private final String value;

		private StringValue(String value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.STRING; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.STRING.ordinal());
			w.writeChars(value);
		}

		@Override
		public String string() { return value; }

		@Override
		public Optional<String> optionalString() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch (type) {
			case EMPTY: return EMPTY_VALUE;
			case STRING: return this;
			case IMAGE:
				try {
					return new ImageValue( new Image( new URI(value) ) );
				} catch (URISyntaxException | IllegalArgumentException e) {
					return EMPTY_VALUE;
				}
			case INSTANT:
				try {
					return new InstantValue(Instant.parse(value));
				} catch (DateTimeException e) {
					return EMPTY_VALUE;
				}
			case INTEGER:
				try {
					return new IntegerValue(Long.parseLong(value));
				} catch (NumberFormatException e) {
					return EMPTY_VALUE;
				}
			case NUMBER:
				try {
					return new NumberValue(Double.parseDouble(value));
				} catch (NumberFormatException e) {
					return EMPTY_VALUE;
				}
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	static final class IntegerValue extends Value {

		private final long value;

		private IntegerValue(long value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.INTEGER; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.INTEGER.ordinal());
			w.writeLong(value);
		}

		@Override
		public long integer() { return value; }

		@Override
		public Optional<Long> optionalInteger() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch (type) {
			case EMPTY: return EMPTY_VALUE;
			case IMAGE: return EMPTY_VALUE;
			case INSTANT: return new InstantValue(Instant.ofEpochMilli(value));
			case INTEGER: return this;
			case NUMBER: return new NumberValue(value);
			case PRIORITY: return value < 0 || value > Priority.HIGHEST.ordinal() ? EMPTY_VALUE : new PriorityValue(Priority.valueOf((int) value));
			case STRING: return new StringValue(Long.toString(value));
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return Long.hashCode(value);
		}
	}

	//TODO disallow NaNs?
	static final class NumberValue extends Value {

		private final double value;

		private NumberValue(double value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.NUMBER; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.NUMBER.ordinal());
			w.writeDouble(value);
		}

		@Override
		public double number() { return value; }

		@Override
		public Optional<Double> optionalNumber() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch (type) {
			case EMPTY: return EMPTY_VALUE;
			case IMAGE: return EMPTY_VALUE;
			case INSTANT: return value > MAX_SAFE_INTEGER || value < MIN_SAFE_INTEGER ? EMPTY_VALUE : new InstantValue( Instant.ofEpochMilli((long) value) );
			case INTEGER: return value > Long.MAX_VALUE || value < Long.MIN_VALUE ? EMPTY_VALUE : new IntegerValue((long) value);
			case NUMBER: return this;
			case PRIORITY: return value < 0 || value > Priority.HIGHEST.ordinal() ? EMPTY_VALUE : new PriorityValue(Priority.valueOf((int) value));
			case STRING: return new StringValue(Double.toString(value));
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return Double.hashCode(value);
		}
	}

	static final class InstantValue extends Value {

		private final Instant value;

		private InstantValue(Instant value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.INSTANT; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.INSTANT.ordinal());
			w.writeLong(value.getEpochSecond());
			w.writeInt(value.getNano());
		}

		@Override
		public Instant instant() { return value; }

		@Override
		public Optional<Instant> optionalInstant() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch(type) {
			case EMPTY: return EMPTY_VALUE;
			case IMAGE: return EMPTY_VALUE;
			case INSTANT: return this;
			case INTEGER: return new IntegerValue(value.toEpochMilli());
			case NUMBER: return new NumberValue(value.toEpochMilli());
			case PRIORITY: return EMPTY_VALUE;
			case STRING: return new StringValue(value.toString());
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	static final class ImageValue extends Value {

		private final Image value;

		private ImageValue(Image value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.IMAGE; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.IMAGE.ordinal());
			w.writeChars(value.uri().toString());
		}

		@Override
		public Image image() { return value; }

		@Override
		public Optional<Image> optionalImage() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch(type) {
			case EMPTY: return EMPTY_VALUE;
			case IMAGE: return this;
			case INSTANT: return EMPTY_VALUE;
			case INTEGER: return EMPTY_VALUE;
			case NUMBER: return EMPTY_VALUE;
			case PRIORITY: return EMPTY_VALUE;
			case STRING: return new StringValue(value.uri().toString());
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

	static final class PriorityValue extends Value {

		private final Priority value;

		private PriorityValue(Priority value) {
			this.value = value;
		}

		@Override
		public Type type() { return Type.PRIORITY; }

		@Override
		public Object toObject() {
			return value;
		}

		@Override
		public void serialize(WriteStream w) {
			w.writeByte((byte) Type.PRIORITY.ordinal());
			w.writeInt(value.ordinal());
		}

		@Override
		public Priority priority() { return value; }

		@Override
		public Optional<Priority> optionalPriority() {
			return Optional.of(value);
		}

		@Override
		public Value as(Type type) {
			switch(type) {
			case EMPTY: return EMPTY_VALUE;
			case IMAGE: return EMPTY_VALUE;
			case INSTANT: return EMPTY_VALUE;
			case INTEGER: return new IntegerValue(value.ordinal());
			case NUMBER: return new NumberValue(value.ordinal());
			case PRIORITY: return this;
			case STRING: return new StringValue(value.name());
			default: return EMPTY_VALUE;
			}
		}

		@Override
		public int hashCode() {
			return value.hashCode();
		}
	}

}