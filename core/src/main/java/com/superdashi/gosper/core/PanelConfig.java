package com.superdashi.gosper.core;

import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.LengthConfig;
import com.superdashi.gosper.config.Config.OffsetConfig;
import com.tomgibara.geom.core.Offset;

public final class PanelConfig implements ConfigTarget {

	public static final LengthRange GUTTER_RANGE = new LengthRange(0f, 0.1f, 0.015f);
	public static final Offset OFFSET_DEFAULT = Offset.IDENTITY;

	public float   gutter = GUTTER_RANGE.def;
	public Offset  offset = OFFSET_DEFAULT;

	public void adopt(PanelConfig that) {
		this.gutter = that.gutter;
		this.offset = that.offset;
	}

	// style target

	@Override
	public void applyStyling(ConfigProperty property, LengthConfig length) {
		switch (property.name) {
		case "gutter":
			gutter = GUTTER_RANGE.lengthOf(length);
			break;
		}
	}

	@Override
	public void applyStyling(ConfigProperty property, OffsetConfig offset) {
		switch (property.name) {
		case "offset":
			this.offset = offset.asOffset();
			break;
		}
	}

}
