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
package com.superdashi.gosper.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.color.Palette.LogicalColor;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.util.Schedule;


public abstract class Config {

	public enum Type {

		COLOR,
		COLORING,
		DURATION,
		HALIGN,
		LENGTH,
		OFFSET,
		PALETTE,
		PATH,
		RESOURCE,
		SCHEDULE,
		VALIGN,
		VECTOR,
		;

		ConfigProperty named(String name) {
			return new ConfigProperty(name, this);
		}

		public Config parse(String str) {
			if (str == null) throw new IllegalArgumentException("null str");
			switch (this) {
			case COLOR: return ColorConfig.parse(str);
			case COLORING: return ColoringConfig.parse(str);
			case DURATION: return DurationConfig.parse(str);
			case HALIGN: return AlignConfig.parse(true, str);
			case LENGTH: return LengthConfig.parse(str);
			case OFFSET: return OffsetConfig.parse(str);
			case PALETTE: return PaletteConfig.parse(str);
			case PATH: return PathConfig.parse(str);
			case RESOURCE: return ResourceConfig.parse(str);
			case SCHEDULE: return ScheduleConfig.parse(str);
			case VALIGN: return AlignConfig.parse(false, str);
			case VECTOR: return VectorConfig.parse(str);
			default: throw new UnsupportedOperationException("no parsing for config type: " + this);
			}
		}

	}

	private static int hex(char c) {
		if (c >= '0' && c <= '9') return c - 48;
		if (c >= 'A' && c <= 'F') return c - 55;
		if (c >= 'a' && c <= 'f') return c - 87;
		throw new IllegalArgumentException("invalid hex character: " + c);
	}

	private static int component2(String str, int i) {
		char c0 = str.charAt(i    );
		char c1 = str.charAt(i + 1);
		return (hex(c0) << 4) + hex(c1);
	}

	private static int component1(String str, int i) {
		return hex(str.charAt(i)) * 17;
	}

	private Config() {}

	public abstract Type type();

	abstract void applyTo(ConfigTarget target, ConfigProperty property);

	public static final class LengthConfig extends Config {

		public static LengthConfig parse(String str) {
			if (str.isEmpty()) throw new IllegalArgumentException("empty length");
			//TODO need strategy for length unit conversion
			float length;
			try {
				length = Float.parseFloat(str);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("length not a valid number: " + str);
			}
			return new LengthConfig(length);
		}

		//TODO support units
		private final float length;

		private LengthConfig(float length) {
			this.length = length;
		}

		@Override
		public Type type() {
			return Type.LENGTH;
		}

		public float asFloat() {
			return length;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static final class ColorConfig extends Config {

		static int parseImpl(String str) {
			if (str.isEmpty()) throw new IllegalArgumentException("empty color");
			if (str.charAt(0) == '#') {
				int r,g,b,a;
				switch (str.length()) {
				case 4:
					a = 0xff;
					r = component1(str, 1);
					g = component1(str, 2);
					b = component1(str, 3);
					break;
				case 5:
					a = component1(str, 1);
					r = component1(str, 2);
					g = component1(str, 3);
					b = component1(str, 4);
					break;
				case 7:
					a = 0xff;
					r = component2(str, 1);
					g = component2(str, 3);
					b = component2(str, 5);
					break;
				case 9:
					a = component2(str, 1);
					r = component2(str, 3);
					g = component2(str, 5);
					b = component2(str, 7);
					break;
					default:
						throw new IllegalArgumentException("invalid number of characters in color definition: " + str);
				}
				return (a << 24) | (r << 16) | (g << 8) | b;
			}
			// TODO support color names
			// TODO support other conventions?
			throw new UnsupportedOperationException("TODO");
		}

		public static ColorConfig parse(String str) {
			return new ColorConfig(parseImpl(str));
		}

		private final int color;

		private ColorConfig(int color) {
			this.color = color;
		}

		@Override
		public Type type() {
			return Type.COLOR;
		}

		public int asInt() {
			return color;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static class ColoringConfig extends Config {

		private static final Pattern COLORING = Pattern.compile("(flat|horizontal|vertical|corners)\\s*\\(([^)]+)\\)");

		private static Coloring parseImpl(String str) {
			Matcher matcher = COLORING.matcher(str);
			if (!matcher.matches()) throw new IllegalArgumentException("invalid coloring");
			String type = matcher.group(1);
			String colors = matcher.group(2);
			String[] parts = colors.split(",");
			int[] cs = Arrays.stream(parts).mapToInt(p -> ColorConfig.parseImpl(p.trim())).toArray();
			switch (type) {
			case "flat":
				if (cs.length != 1) throw new IllegalArgumentException("expected one color");
				return Coloring.flat(cs[0]);
			case "horizontal" :
				if (cs.length != 2) throw new IllegalArgumentException("expected two colors");
				return Coloring.horizontal(cs[0], cs[1]);
			case "vertical" :
				if (cs.length != 2) throw new IllegalArgumentException("expected two colors");
				return Coloring.vertical(cs[0], cs[1]);
			case "corners" :
				if (cs.length != 4) throw new IllegalArgumentException("expected four colors");
				return Coloring.corners(cs[0], cs[1], cs[2], cs[3]);
				default: throw new IllegalStateException(type);
			}
		}

		public static ColoringConfig parse(String str) {
			return new ColoringConfig(parseImpl(str));
		}

		private final Coloring coloring;

		private ColoringConfig(Coloring coloring) {
			this.coloring = coloring;
		}

		public Coloring asColoring() {
			return coloring;
		}

		@Override
		public Type type() {
			return Type.COLORING;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static class VectorConfig extends Config {

		private static float parseFloat(String str) {
			try {
				return Float.parseFloat(str.trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid vector component: " + str);
			}
		}

		public static VectorConfig parse(String str) {
			int length = str.length();
			if (length == 0) throw new IllegalArgumentException("empty vector");
			boolean normalize;
			if (str.startsWith("u")) {
				str = str.substring(1);
				length --;
				normalize = true;
			} else {
				normalize = false;
			}
			if (str.charAt(0) != '(') throw new IllegalArgumentException("vector missing '('");
			if (str.charAt(length -1) != ')') throw new IllegalArgumentException("vector missing ')'");
			String[] parts = str.substring(1, length -1).split(",");
			if (parts.length != 3) throw new IllegalArgumentException("expected 3 vector components");
			float x = parseFloat(parts[0]);
			float y = parseFloat(parts[1]);
			float z = parseFloat(parts[2]);
			if (normalize) {
				float mag = (float) Math.sqrt(x * x + y * y + z * z);
				x /= mag;
				y /= mag;
				z /= mag;
			}
			return new VectorConfig(x, y, z);
		}

		private final float x;
		private final float y;
		private final float z;

		private VectorConfig(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		@Override
		public Type type() {
			return Type.VECTOR;
		}

		public float[] asFloats() {
			return new float[] {x, y, z};
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static class OffsetConfig extends Config {

		private static float parseFloat(String str) {
			try {
				return Float.parseFloat(str.trim());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("invalid offset value: " + str);
			}
		}

		public static OffsetConfig parse(String str) {
			int length = str.length();
			if (length == 0) throw new IllegalArgumentException("empty offset");
			if (str.charAt(0) != '(') throw new IllegalArgumentException("offset missing '('");
			if (str.charAt(length - 1) != ')') throw new IllegalArgumentException("offset missing ')'");
			String[] parts = str.substring(1, length -1).split(",");
			final com.tomgibara.geom.core.Offset offset;
			switch (parts.length) {
			case 0:
				offset = com.tomgibara.geom.core.Offset.IDENTITY;
				break;
			case 1:
				offset = com.tomgibara.geom.core.Offset.uniform(parseFloat(parts[0]));
				break;
			case 2:
				offset = com.tomgibara.geom.core.Offset.symmetric(parseFloat(parts[0]), parseFloat(parts[1]));
				break;
			case 4:
				offset = com.tomgibara.geom.core.Offset.offset(
						parseFloat(parts[0]),
						parseFloat(parts[1]),
						parseFloat(parts[2]),
						parseFloat(parts[3])
						);
				break;
				default: throw new IllegalArgumentException("invalid number of offset values");
			}
			return new OffsetConfig(offset);
		}

		private final com.tomgibara.geom.core.Offset offset;

		private OffsetConfig(com.tomgibara.geom.core.Offset offset) {
			this.offset = offset;
		}

		@Override
		public Type type() {
			return Type.OFFSET;
		}

		public com.tomgibara.geom.core.Offset asOffset() {
			return offset;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static final class PaletteConfig extends Config {

		private final Palette palette;

		public static PaletteConfig parse(String str) {
			if (str.isEmpty()) throw new IllegalArgumentException("empty palette");
			String[] parts = str.split(",");
			if (parts.length != Palette.SIZE) throw new IllegalArgumentException("expected " + Palette.SIZE + " colors");
			Palette.Builder builder = Palette.newBuilder();
			for (int i = 0; i < Palette.SIZE; i++) {
				builder.color(LogicalColor.valueOf(i), ColorConfig.parseImpl(parts[i]));
			}
			return new PaletteConfig( builder.build() );
		}

		private PaletteConfig(Palette palette) {
			this.palette = palette;
		}

		public Palette asPalette() {
			return palette;
		}

		@Override
		public Type type() {
			return Type.PALETTE;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static final class PathConfig extends Config {

		public static PathConfig parse(String str) {
			if (!str.startsWith("/")) str = '/' + str;
			return new PathConfig(str);
		}

		private final String path;

		private PathConfig(String path) {
			this.path = path;
		}

		@Override
		public Type type() {
			return Type.PATH;
		}

		public String asString() {
			return path;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static final class ScheduleConfig extends Config {

		private static final Pattern PATTERN = Pattern.compile("EVERY\\s+(?:(MILLI|SECOND|MINUTE|HOUR|DAY|WEEK|MONTH)|(?:([1-9]\\d*)\\s*(MILLIS|SECONDS|MINUTES|HOURS|DAYS|WEEKS|MONTHS)))");
		private static final ScheduleConfig ONCE = new ScheduleConfig(1L, ChronoUnit.FOREVER, Duration.ZERO);
		private static final ScheduleConfig HOURLY = new ScheduleConfig(1L, ChronoUnit.HOURS, Duration.ZERO);
		private static final ScheduleConfig DAILY = new ScheduleConfig(1L, ChronoUnit.HOURS, Duration.ZERO);
		private static final ScheduleConfig WEEKLY = new ScheduleConfig(1L, ChronoUnit.WEEKS, Duration.ZERO);
		private static final ScheduleConfig MONTHLY = new ScheduleConfig(1L, ChronoUnit.MONTHS, Duration.ZERO);

		private static TemporalUnit toUnit(String unit) {
			switch (unit) {
			case "MILLI"  :
			case "MILLIS" : return ChronoUnit.MILLIS;
			case "SECOND" :
			case "SECONDS": return ChronoUnit.SECONDS;
			case "MINUTE" :
			case "MINUTES": return ChronoUnit.MINUTES;
			case "HOUR"   :
			case "HOURS"  : return ChronoUnit.HOURS;
			case "DAY"    :
			case "DAYS"   : return ChronoUnit.DAYS;
			case "WEEK"   :
			case "WEEKS"  : return ChronoUnit.WEEKS;
			case "MONTH"  :
			case "MONTHS" : return ChronoUnit.MONTHS;
			default: throw new IllegalStateException("Unsupported unit: " + unit);
			}
		}

		public static ScheduleConfig parse(String str) {
			if (str.isEmpty()) throw new IllegalArgumentException("empty schedule");
			str = str.toUpperCase().trim();
			switch (str) {
			case "ONCE"   : return ONCE;
			case "HOURLY" : return HOURLY;
			case "DAILY"  : return DAILY;
			case "WEEKLY" : return WEEKLY;
			case "MONTHLY": return MONTHLY;
			default:
				// split into interval & offset
				String intervalStr;
				String offsetStr;
				{
					int i = str.indexOf('@');
					if (i == -1) {
						intervalStr = str;
						offsetStr = null;
					} else {
						intervalStr = str.substring(0, i).trim();
						offsetStr = str.substring(i + 1).trim();
					}
				}

				// parse interval
				Matcher matcher = PATTERN.matcher(intervalStr);
				if (!matcher.matches()) throw new IllegalArgumentException("invalid interval");
				long duration;
				TemporalUnit unit;
				String unitStr = matcher.group(1);
				if (unitStr == null) {
					duration = Long.parseLong(matcher.group(2));
					unitStr = matcher.group(3);
				} else {
					duration = 1L;
				}
				unit = toUnit(unitStr);

				// parse offset
				TemporalAmount offset;
				if (offsetStr == null) {
					offset = Duration.ZERO;
				} else try {
					offset = Duration.parse("P" + offsetStr);
				} catch (DateTimeParseException e) {
					throw new IllegalArgumentException("invalid offset", e);
				}

				// return
				return new ScheduleConfig(duration, unit, offset);
			}
		}

		private final Schedule schedule;

		private ScheduleConfig(long duration, TemporalUnit unit, TemporalAmount offset) {
			schedule = Schedule.at(duration, unit, offset);
		}

		@Override
		public Type type() {
			return Type.SCHEDULE;
		}

		public Schedule asSchedule() {
			return schedule;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static class ResourceConfig extends Config {

		private static final Pattern PATTERN = Pattern.compile("uri\\s*\\(\\s*([^) ]+)\\s*\\)");

		private static ResourceConfig parse(String str) {
			Matcher matcher = PATTERN.matcher(str);
			if (!matcher.matches()) throw new IllegalArgumentException("invalid resource");
			String uriStr = matcher.group(1);
			URI uri;
			try {
				uri = new URI(uriStr);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("invalid URI syntax");
			}
			//TODO check scheme?
			return new ResourceConfig(uri);
		}

		private final URI uri;

		private ResourceConfig(URI uri) {
			this.uri = uri;
		}

		public URI asURI() {
			return uri;
		}

		@Override
		public Type type() {
			return Type.RESOURCE;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}

	public static class AlignConfig extends Config {

		private static final Pattern PATTERN = Pattern.compile("(0|(?:[1-9]\\d*)(?:\\.\\d+)?)\\s*(%?)|(left|center|right)|(bottom|middle|top)", Pattern.CASE_INSENSITIVE);

		private static final AlignConfig LEFT =   new AlignConfig(true,  Alignment.MIN);
		private static final AlignConfig CENTER = new AlignConfig(true,  Alignment.MID);
		private static final AlignConfig RIGHT =  new AlignConfig(true,  Alignment.MAX);
		private static final AlignConfig BOTTOM = new AlignConfig(false, Alignment.MIN);
		private static final AlignConfig MIDDLE = new AlignConfig(false, Alignment.MID);
		private static final AlignConfig TOP =    new AlignConfig(false, Alignment.MAX);

		private static AlignConfig parse(boolean horiz, String str) {
			Matcher matcher = PATTERN.matcher(str);
			if (!matcher.matches()) throw new IllegalArgumentException("invalid alignment");
			String num = matcher.group(1);
			if (num != null) {
				float m = Float.parseFloat(num);
				if (matcher.group(2) != null) m /= 100f;
				return new AlignConfig(horiz, Alignment.atClamped(m));
			}
			String name = matcher.group(horiz ? 3 : 4);
			if (name == null) throw new IllegalArgumentException("invalid alignment direction");
			switch (name) {
			case "left" : return AlignConfig.LEFT;
			case "center" : return AlignConfig.CENTER;
			case "right" : return AlignConfig.RIGHT;
			case "bottom" : return AlignConfig.BOTTOM;
			case "middle" : return AlignConfig.MIDDLE;
			case "top" : return AlignConfig.TOP;
			default: throw new IllegalStateException();
			}
		}

		private final boolean horiz;
		private final Alignment align;

		private AlignConfig(boolean horiz, Alignment align) {
			this.horiz = horiz;
			this.align = align;
		}

		public Alignment asAlign() {
			return align;
		}

		@Override
		public Type type() {
			return horiz ? Type.HALIGN : Type.VALIGN;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}
	}

	public static class DurationConfig extends Config {

		static DurationConfig parse(String str) {
			return new DurationConfig(com.superdashi.gosper.util.Duration.parseMillis(str));
		}

		private final long duration;

		DurationConfig(long duration) {
			this.duration = duration;
		}

		public long asMillis() {
			return duration;
		}

		@Override
		public Type type() {
			return Type.DURATION;
		}

		@Override
		void applyTo(ConfigTarget target, ConfigProperty property) {
			target.applyStyling(property, this);
		}

	}
}
