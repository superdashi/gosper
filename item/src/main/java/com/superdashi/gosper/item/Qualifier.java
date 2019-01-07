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

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.WriteStream;

//TODO extend with more attributes as necessary
//NOTE permits impossible combinations of screen class & screen color
public final class Qualifier implements Comparable<Qualifier> {

	// statics

	private static final Qualifier universal = new Qualifier(null, null, null, null);

	private static <V extends Comparable<V>> int compare(V a, V b) {
		if (a == null && b == null) return 0;
		if (a == null) return -1;
		if (b == null) return 1;
		return a.compareTo(b);
	}

	public static Qualifier universal() {
		return universal;
	}

	public static Qualifier with(Locale lang, ScreenClass screen, ScreenColor color, Flavor flavor) {
		if (lang == null && screen == null && color == null && flavor == null) return universal;
		return new Qualifier(lang, screen, color, flavor);
	}

	public static Qualifier deserialize(ReadStream r) {
		String langTag = r.readChars();
		int screenOrd = r.readInt();
		int colorOrd = r.readInt();
		int flavorOrd = r.readInt();
		Locale lang = langTag.isEmpty() ? null : Locale.forLanguageTag(langTag);
		ScreenClass screen = screenOrd == -1 ? null : ScreenClass.valueOf(screenOrd);
		ScreenColor color = colorOrd == -1 ? null : ScreenColor.valueOf(colorOrd);
		Flavor flavor = flavorOrd == -1 ? null : Flavor.valueOf(flavorOrd);
		return with(lang, screen, color, flavor);
	}

	// fields

	public final Locale lang;
	public final ScreenClass screen;
	public final ScreenColor color;
	public final Flavor flavor;

	// constructors

	private Qualifier(Locale lang, ScreenClass screen, ScreenColor color, Flavor flavor) {
		this.lang   = lang  ;
		this.screen = screen;
		this.color  = color ;
		this.flavor = flavor;
	}

	// accessors

	public Optional<Locale     > lang  () { return Optional.ofNullable(lang  ); }
	public Optional<ScreenClass> screen() { return Optional.ofNullable(screen); }
	public Optional<ScreenColor> color () { return Optional.ofNullable(color ); }
	public Optional<Flavor     > flavor() { return Optional.ofNullable(flavor); }

	public boolean isUniversal() { return this == universal; }

	public boolean isFullySpecified() {
		return lang != null && screen != null && color != null && flavor != null;
	}

	// methods

	public Qualifier withLang(Locale lang) {
		return Objects.equals(lang, this.lang) ? this : Qualifier.with(lang, screen, color, flavor);
	}

	public Qualifier withLang(String lang) {
		return withLang( lang == null ? null : Locale.forLanguageTag(lang) );
	}

	public Qualifier withScreen(ScreenClass screen) {
		return Objects.equals(screen, this.screen) ? this : Qualifier.with(lang, screen, color, flavor);
	}

	public Qualifier withColor(ScreenColor color) {
		return Objects.equals(color, this.color) ? this : Qualifier.with(lang, screen, color, flavor);
	}

	public Qualifier withFlavor(Flavor flavor) {
		return Objects.equals(flavor, this.flavor) ? this : Qualifier.with(lang, screen, color, flavor);
	}

	// universal matches all qualifiers
	public boolean matches(Qualifier that) {
		if (this == universal) return true;
		if (this.lang   != null && !this.lang.equals(that.lang)) return false;
		if (this.screen != null &&  this.screen != that.screen ) return false;
		if (this.color  != null &&  this.color  != that.color  ) return false;
		if (this.flavor != null &&  this.flavor != that.flavor ) return false;
		return true;
	}

	public void serialize(WriteStream w) {
		String langStr = lang == null ? "" : lang.toLanguageTag();
		int screenOrd = screen == null ? -1 : screen.ordinal();
		int colorOrd = color == null ? -1 : color.ordinal();
		int flavorOrd = flavor == null ? -1 : flavor.ordinal();
		w.writeChars(langStr);
		w.writeInt(screenOrd);
		w.writeInt(colorOrd);
		w.writeInt(flavorOrd);
	}

	// comparable methods

	@Override
	public int compareTo(Qualifier that) {
		if (this == that) return 0;
		int c;
		String thisStr = this.lang == null ? null : this.lang.toString();
		String thatStr = that.lang == null ? null : that.lang.toString();
		c = compare(thisStr, thatStr);
		if (c != 0) return c;
		c = compare(this.screen, that.screen);
		if (c != 0) return c;
		c = compare(this.color, that.color);
		if (c != 0) return c;
		c = compare(this.flavor, that.flavor);
		return c;
	}

	// object methods

	@Override
	public int hashCode() {
		return Objects.hashCode(lang) + Objects.hashCode(screen) + Objects.hashCode(color) + Objects.hashCode(flavor);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof Qualifier)) return false;
		Qualifier that = (Qualifier) obj;
		return
				Objects.equals(this.lang  , that.lang  ) &&
				Objects.equals(this.screen, that.screen) &&
				Objects.equals(this.color , that.color ) &&
				Objects.equals(this.flavor, that.flavor);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Qualifier");
		if (lang != null) sb.append(" lang=" + lang.toLanguageTag());
		if (screen != null) sb.append(" screen=" + screen);
		if (color != null) sb.append(" color=" + color);
		if (flavor != null) sb.append(" flavor=" + flavor);
		return sb.append("]").toString();
	}

}
