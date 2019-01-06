package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;

interface RenderResource {

	void init(GL gl);

	void bind(GL gl);

	void destroy(GL gl);
}
