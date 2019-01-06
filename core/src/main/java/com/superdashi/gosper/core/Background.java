package com.superdashi.gosper.core;

import java.util.Optional;

import com.superdashi.gosper.config.ConfigTarget;

public class Background extends DesignChild {

	private final BackgroundConfig DEFAULT_STYLE = new BackgroundConfig();

	public final BackgroundConfig style = new BackgroundConfig();
	private BackgroundConfig givenStyle = DEFAULT_STYLE;

	private InfoAcquirer infoAcquirer = null;

	public void infoAcquirer(InfoAcquirer infoAcquirer) {
		this.infoAcquirer = infoAcquirer;
	}

	public InfoAcquirer infoAcquirer() {
		return infoAcquirer;
	}

	// styleable

	@Override
	public Type getStyleType() {
		return Type.BACKGROUND;
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
