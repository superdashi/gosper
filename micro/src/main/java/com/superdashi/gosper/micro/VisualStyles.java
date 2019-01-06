package com.superdashi.gosper.micro;

import java.util.Map;

import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Style;

public final class VisualStyles {

	private final Style baseStyle = new Style()
			.marginLeft(0)
			.marginTop(0)
			.marginRight(0)
			.marginBottom(0)
			.textUnderline(0)
			.textWeight(0)
			.textItalic(0)
			.textOutline(0)
			.lineLimit(Integer.MAX_VALUE)
			.lineSpace(0)
			.colorBg(0x00000000)
			.colorFg(0x00000000)
			.alignmentX(Alignment.MIN)
			.alignmentY(Alignment.MIN)
			.immutable();

	private static Style merge(Style a, Style b) {
		return a.mutableCopy().apply(b);
	}

	private static Style extract(Map<String, Style> styles, String name) {
		Style style = styles.get(name);
		return style == null ? Style.noStyle() : style.immutable();
	}

	public final Style defaultPlaceStyle;
	public final Style defaultDocumentStyle;
	public final Style defaultCardStyle;
	public final Style defaultBadgeStyle;
	public final Style defaultButtonStyle;

	VisualStyles(VisualTheme theme) {
		defaultPlaceStyle    = baseStyle;
		defaultDocumentStyle = baseStyle;
		defaultCardStyle     = baseStyle;
		defaultBadgeStyle    = baseStyle;
		defaultButtonStyle   = baseStyle.mutable().colorBg(theme.buttonBgColor).colorFg(theme.buttonTextColor).immutable();
	}

	VisualStyles(Style defaultPlaceStyle, Style defaultDocumentStyle, Style defaultCardStyle, Style defaultBadgeStyle, Style defaultButtonStyle) {
		this.defaultPlaceStyle    = defaultPlaceStyle   .immutable();
		this.defaultDocumentStyle = defaultDocumentStyle.immutable();
		this.defaultCardStyle     = defaultCardStyle    .immutable();
		this.defaultBadgeStyle    = defaultBadgeStyle   .immutable();
		this.defaultButtonStyle   = defaultButtonStyle  .immutable();
	}

	VisualStyles(Map<String, Style> styles) {
		this.defaultPlaceStyle    = extract(styles, Visuals.STYLE_PLACE   );
		this.defaultDocumentStyle = extract(styles, Visuals.STYLE_DOCUMENT);
		this.defaultCardStyle     = extract(styles, Visuals.STYLE_CARD    );
		this.defaultBadgeStyle    = extract(styles, Visuals.STYLE_BADGE   );
		this.defaultButtonStyle   = extract(styles, Visuals.STYLE_BUTTON  );
	}

	VisualStyles merge(VisualStyles that) {
		return new VisualStyles(
				merge(this.defaultPlaceStyle   , that.defaultPlaceStyle   ),
				merge(this.defaultDocumentStyle, that.defaultDocumentStyle),
				merge(this.defaultCardStyle    , that.defaultCardStyle    ),
				merge(this.defaultBadgeStyle   , that.defaultBadgeStyle   ),
				merge(this.defaultButtonStyle  , that.defaultButtonStyle  )
				);
	}

}
