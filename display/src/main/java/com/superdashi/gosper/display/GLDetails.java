package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.superdashi.gosper.core.DashiLog;

final class GLDetails {

	public final String vendor;
	public final String renderer;
	public final String version;

	GLDetails(GL gl) {
		vendor = gl.glGetString(GL.GL_VENDOR);
		renderer = gl.glGetString(GL.GL_RENDERER);
		version = gl.glGetString(GL.GL_VERSION);
	}

	void log() {
		DashiLog.info("GL_VENDOR: {0}", vendor);
		DashiLog.info("GL_RENDERER: {0}", renderer);
		DashiLog.info("GL_VERSION: {0}", version);
	}
}
