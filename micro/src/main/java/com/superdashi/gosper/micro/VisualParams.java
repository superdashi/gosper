package com.superdashi.gosper.micro;

import static com.superdashi.gosper.micro.Visuals.extract;

import java.util.Map;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Style;

public abstract class VisualParams {

	static final VisualStyles noStyles = new VisualStyles(Style.noStyle(), new Style().colorBg(Argb.WHITE), Style.noStyle(), new Style().colorBg(Argb.WHITE), new Style().colorBg(Argb.BLACK).colorFg(Argb.WHITE));

	final Map<String, Object> map;
	final VisualStyles styles;

	// may only be created by this package
	VisualParams(Map<String, Object> map) {
		this.map = map;
		styles = extract(map, Visuals.STYLES);
	}

}
