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
package com.superdashi.gosper.micro;

import static com.superdashi.gosper.micro.Visuals.extract;
import static com.superdashi.gosper.micro.Visuals.extractColor;

import java.util.Collections;
import java.util.Map;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;

public final class VisualTheme extends VisualParams {

	// colors
	// the ambient color that may be applied to the display
	public final int ambientColor;
	// the color of the selector when it's being used to perform actions
	public final int contentBgColor;
	// the color of the selector when it's being used to perform actions
	public final int actionColor;
	// the color of the selector when it's being used only for selection
	public final int controlColor;
	// the color of text content
	public final int textualColor;
	// the background color of the top-bar
	public final int barBgColor;
	// the text color of the top-bar
	public final int barTextColor;
	// the colour of indicators on the bar
	public final int indicatorColor;
	// the background color of buttons
	public final int buttonBgColor;
	// the color of text on buttons
	public final int buttonTextColor;
	// the color of informational text
	public final int infoTextColor;
	// the color behind informational text
	public final int infoBgColor;
	// the color used to highlight active key
	public final int keyActiveColor;
	// the color used to render active caret
	public final int caretActiveColor;
	// the color used to render passive caret
	public final int caretPassiveColor;

	// other

	public final String ellipsisString;

	final String typefaceName;
	final Background background;
	// behind the keyboard (if visible)
	final Background keyboardBackground;

	//TODO eliminate, and provide alternative for tests
	VisualTheme() {
		super(Collections.singletonMap(Visuals.STYLES, noStyles));
		ambientColor = Argb.WHITE;
		contentBgColor = Argb.WHITE;
		actionColor = Argb.BLACK;
		controlColor = Argb.BLACK;
		textualColor = Argb.BLACK;
		barBgColor = Argb.BLACK;
		barTextColor = Argb.WHITE;
		indicatorColor = Argb.WHITE;
		buttonBgColor = Argb.BLACK;
		buttonTextColor = Argb.WHITE;
		infoTextColor = Argb.BLACK;
		infoBgColor = Argb.WHITE;
		keyActiveColor = Argb.BLACK;
		caretActiveColor = Argb.BLACK;
		caretPassiveColor = Argb.BLACK;

		typefaceName = Visuals.TYPEFACE_EZO;

		ellipsisString = "...";

		background = Background.color(0xff808080);
		keyboardBackground = background;
	}

	VisualTheme(Map<String, Object> map) {
		super(map);

		ambientColor = extractColor(map, Visuals.AMB_COLOR);
		contentBgColor = extractColor(map, Visuals.CNT_BG_COLOR);
		actionColor = extractColor(map, Visuals.ACT_COLOR);
		controlColor = extractColor(map, Visuals.CTL_COLOR);
		textualColor = extractColor(map, Visuals.TXT_COLOR);
		barBgColor = extractColor(map, Visuals.BAR_BG_COLOR);
		barTextColor = extractColor(map, Visuals.BAR_TXT_COLOR);
		indicatorColor = extractColor(map, Visuals.IND_COLOR);
		buttonBgColor = extractColor(map, Visuals.BTN_BG_COLOR);
		buttonTextColor = extractColor(map, Visuals.BTN_TXT_COLOR);
		infoTextColor = extractColor(map, Visuals.INF_TXT_COLOR);
		infoBgColor = extractColor(map, Visuals.INF_BG_COLOR);
		keyActiveColor = extractColor(map, Visuals.KEY_ACT_COLOR);
		caretActiveColor = extractColor(map, Visuals.CAR_ACT_COLOR);
		caretPassiveColor = extractColor(map, Visuals.CAR_PAS_COLOR);
		ellipsisString = extract(map, Visuals.ELLIPSIS_TEXT);
		typefaceName = extract(map, Visuals.TYPEFACE_NAME);
		background = extract(map, Visuals.BACKGROUND);
		keyboardBackground = extract(map, Visuals.KBD_BACKGROUND);
	}

}
