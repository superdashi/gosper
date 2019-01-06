package com.superdashi.gosper.core;

import java.util.Optional;

import com.superdashi.gosper.config.ConfigTarget;

public class Bar extends DesignChild {

	private final BarConfig DEFAULT_STYLE = new BarConfig();

	public final BarConfig style = new BarConfig();
	private BarConfig givenStyle = DEFAULT_STYLE;

	// styleable

	@Override
	public Type getStyleType() {
		return Type.BAR;
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

}
