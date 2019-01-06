package com.superdashi.gosper.display;

import java.util.Collection;

import com.jogamp.opengl.GL2ES2;

public interface ElementDisplay {

	default void init(RenderContext context) { }

	Collection<Element> getElements();

	default void update(RenderContext context) { }

	default void setAnimation(ElementAnim anim) {
		for (Element el : getElements()) {
			el.anim = anim;
		}
	}

	default void destroy(GL2ES2 gl) { }
}
