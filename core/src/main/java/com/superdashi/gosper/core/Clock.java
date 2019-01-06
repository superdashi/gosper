package com.superdashi.gosper.core;

import java.util.Optional;

import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Configurable;

public class Clock implements Configurable {

	private final ClockConfig givenStyle;
	public final ClockConfig style = new ClockConfig();

	public Clock(ClockConfig givenStyle) {
		this.givenStyle = givenStyle;
	}

	@Override
	public Type getStyleType() {
		return Type.CLOCK;
	}

	@Override
	public String getId() {
		// TODO
		return null;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

}
