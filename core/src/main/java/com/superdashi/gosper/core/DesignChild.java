package com.superdashi.gosper.core;

import com.superdashi.gosper.config.Configurable;

//TODO ditch?
abstract public class DesignChild implements Configurable {

	Design design;

	@Override
	public Design getStyleableParent() {
		return design;
	}

}
