package com.superdashi.gosper.model;

import static com.superdashi.gosper.color.Coloring.argbToRGBA;

public class ModelMaterial {

	public static ModelMaterial fromColor(int color) {
		return new ModelMaterial(argbToRGBA(color));
	}

	// rgba
	public final int color;

	//TODO textures

	public ModelMaterial(int color) {
		this.color = color;
	}
}
