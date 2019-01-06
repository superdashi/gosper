package com.superdashi.gosper.studio;

import com.superdashi.gosper.layout.Style;

//TODO replace flags in Style with TextStyle?
public final class TextStyle {

	private static final int FLAG_BOLD       = 0b001;
	private static final int FLAG_ITALIC     = 0b010;
	private static final int FLAG_UNDERLINED = 0b100;

	private static final TextStyle[] styles = new TextStyle[8];

	private static final TextStyle REGULAR     = with(false, false, false);
	private static final TextStyle BOLD        = with(true , false, false);
	private static final TextStyle ITALIC      = with(false, true , false);
	private static final TextStyle BOLD_ITALIC = with(true , true , false);

	public static TextStyle regular    () { return REGULAR    ; }
	public static TextStyle bold       () { return BOLD       ; }
	public static TextStyle italic     () { return ITALIC     ; }
	public static TextStyle boldItalic () { return BOLD_ITALIC; }

	private static boolean styleFlag(int value) {
		return value != Style.NO_VALUE && value > 0;
	}

	private static int index(boolean bold, boolean italic, boolean underlined) {
		int index = 0;
		if (bold)       index |= FLAG_BOLD      ;
		if (italic)     index |= FLAG_ITALIC    ;
		if (underlined) index |= FLAG_UNDERLINED;
		return index;
	}

	public static TextStyle with(boolean bold, boolean italic, boolean underlined) {
		int i = index(bold, italic, underlined);
		TextStyle style = styles[i];
		return style == null ? styles[i] = new TextStyle(i, bold, italic, underlined) : style;
	}

	public static TextStyle fromStyle(Style style) {
		return with(
				styleFlag(style.textWeight()),
				styleFlag(style.textItalic()),
				styleFlag(style.textUnderline())
				);
	}

	final int index;
	public final boolean bold;
	public final boolean italic;
	public final boolean underlined;

	private final Style style;

	private TextStyle(int index, boolean bold, boolean italic, boolean underlined) {
		this.index = index;
		this.bold = bold;
		this.italic = italic;
		this.underlined = underlined;
		this.style = new Style()
				.textWeight(bold ? 1 : 0)
				.textItalic(italic ? 1 : 0)
				.textUnderline(underlined ? 1 : 0)
				.immutable();
	}

	public boolean isRegular() {
		return this == REGULAR;
	}

	public TextStyle withBold(boolean bold) {
		return bold == this.bold ? this : with(bold, italic, underlined);
	}

	public TextStyle withItalic(boolean italic) {
		return italic == this.italic ? this : with(bold, italic, underlined);
	}

	public TextStyle withUnderlined(boolean underlined) {
		return underlined == this.underlined ? this : with(bold, italic, underlined);
	}

	public Style asStyle() {
		return style;
	}

	@Override
	public int hashCode() {
		return index;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof TextStyle)) return false;
		TextStyle that = (TextStyle) obj;
		return this.index == that.index;
	}

	@Override
	public String toString() {
		switch (index) {
		case 0: return "regular";
		case 1: return "bold";
		case 2: return "italic";
		case 3: return "boldItalic";
		default: return "bold: " + bold + ", italic: " + italic + ", underlined: " + underlined;
		}
	}

}
