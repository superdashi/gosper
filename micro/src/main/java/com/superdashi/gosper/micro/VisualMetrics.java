package com.superdashi.gosper.micro;

import static com.superdashi.gosper.micro.Visuals.extract;

import java.util.Collections;
import java.util.Map;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;
import com.tomgibara.intgeom.IntMargins;

public final class VisualMetrics extends VisualParams {

	// metrics

	//TODO rename to topBarHeight?
	public final int barHeight;
	// gap between bottom of bar and label baseline
	public final int barBaselineGap;
	// gap between bottom of bar and indicators
	public final int barIndicatorGap;
	// gap between button divide and button/text
	public final int barButtonGap;
	// width of gaps in the bar
	public final int barGap;
	public final int sideMargin;
	public final int indicatorGap;
	public final int scrollbarWidth;
	public final int scrollbarMinHeight;
	public final int componentHMargin;
	public final int tableRowHeight;
	public final int badgeSize;
	// height of a button
	public final int buttonHeight;
	// gap between buttons
	public final int buttonGap;
	// gap between bottom of button and label baseline
	public final int buttonBaselineGap;
	public final int lineHeight;
	public final int iconSize;
	// gap between icons
	public final int iconGap;
	// thickness of icon highlight bar
	public final int iconBarWidth;
	// gap between bar and icons
	public final int iconBarGap;
	public final int symbolSize;
	// size of typeface (in unspecified coordinates!)
	public final int typefaceSize;

	public final IntMargins dialogButtonMargins;

	//TODO eliminate, and provide alternative for tests
	VisualMetrics() {
		super(Collections.singletonMap(Visuals.STYLES, noStyles));

		barHeight = 9;
		barBaselineGap = 3;
		barIndicatorGap = 1;
		barButtonGap = 2;
		barGap = 1;
		sideMargin = 2;
		indicatorGap = 1;
		scrollbarWidth = 9;
		scrollbarMinHeight = 6;
		componentHMargin = 2;
		badgeSize = 8;
		tableRowHeight = 9;
		buttonHeight = 10;
		buttonBaselineGap = 3;
		buttonGap = 1;
		lineHeight = 9;
		iconSize = 24;
		iconGap = 2;
		iconBarGap = 2;
		iconBarWidth = 2;
		symbolSize = 16;
		typefaceSize = 16;

		this.dialogButtonMargins = IntMargins.widths(2, 2, 1, 1);
	}

	VisualMetrics(Map<String, Object> map) {
		super(map);

		barHeight = extract(map, Visuals.BAR_HEIGHT);
		barBaselineGap = extract(map, Visuals.BAR_BASE_GAP);
		barIndicatorGap = extract(map, Visuals.BAR_IND_GAP);
		barButtonGap = extract(map, Visuals.BAR_BTN_GAP);
		barGap = extract(map, Visuals.BAR_GAP);
		sideMargin = extract(map, Visuals.SIDE_MARGIN);
		indicatorGap = extract(map, Visuals.IND_GAP);
		scrollbarWidth = extract(map, Visuals.SCR_WIDTH);
		scrollbarMinHeight = extract(map, Visuals.SCR_MIN_HEIGHT);
		componentHMargin = extract(map, Visuals.CMP_MARGIN);
		badgeSize = extract(map, Visuals.BDG_SIZE);
		tableRowHeight = extract(map, Visuals.TBL_ROW_HEIGHT);
		buttonHeight = extract(map, Visuals.BTN_HEIGHT);
		buttonBaselineGap = extract(map, Visuals.BTN_BASE_GAP);
		buttonGap = extract(map, Visuals.BTN_GAP);
		lineHeight = extract(map, Visuals.LINE_HEIGHT);
		iconSize = extract(map, Visuals.ICN_SIZE);
		iconGap = extract(map, Visuals.ICN_GAP);
		iconBarWidth = extract(map, Visuals.ICN_BAR_WIDTH);
		iconBarGap = extract(map, Visuals.ICN_BAR_GAP);
		symbolSize = extract(map, Visuals.SYM_SIZE);
		typefaceSize = extract(map, Visuals.TYPEFACE_SIZE);

		dialogButtonMargins = IntMargins.widths(2, 2, 1, 1);
	}

}
