package com.superdashi.gosper.display;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.core.DashiLog.Level;

class Screen {

	private final CoreContext context;
	private final Animator animator;

	Screen(CoreContext context, Resolution screenResolution, DashiRendering rendering) {
		this.context = context;

		GLCapabilities caps = new GLCapabilities(GLProfile.get(GLProfile.GL2ES2));
		// we want a translucent drawable because we are going to composite over video
		caps.setBackgroundOpaque(false);
		// explicitly setting alpha-bits is required, or RPi doesn't properly blend
		caps.setAlphaBits(8);
		GLWindow glWindow = GLWindow.create(caps);

		// Setup and view the GLWindow directly, this allows us to run headless
		glWindow.setTitle(context.title());
		glWindow.setSize(screenResolution.h, screenResolution.h);
		glWindow.setUndecorated(true);
		glWindow.setPointerVisible(false);
		glWindow.setVisible(true);

		// set up the listeners
		glWindow.addGLEventListener(new EventListener(rendering));
		//TODO key actions will need to be configurable for executing test actions
		glWindow.addKeyListener(new ShutdownKeyListener());

		// set up the animator
		animator = new Animator();
		animator.setExclusiveContext(true);
		animator.setRunAsFastAsPossible(true);
		animator.add(glWindow);

	}

	public void start() {
		if (!animator.isAnimating()) {
			DashiLog.debug("screen starting");
			animator.start();
			DashiLog.debug("screen started");
		}
	}

	public void stop() {
		if (animator.isAnimating()) {
			DashiLog.debug("screen stopping");
			animator.stop();
			DashiLog.debug("screen stopped");
		}
	}

	private class EventListener implements GLEventListener {


		private final GLEventListener listener;

		EventListener(GLEventListener listener) {
			this.listener = listener;
		}

		@Override
		public void init(GLAutoDrawable drawable) {
			GL gl = drawable.getGL();
			DashiLog.info("Chosen GLCapabilities: " + drawable.getChosenGLCapabilities());
			DashiLog.info("INIT GL IS: {0}", gl.getClass().getName());
			new GLDetails(gl).log();
			if (DashiLog.isDebug()) {
				int[] values = new int[3];
				String extensions = drawable.getContext().getPlatformExtensionsString();
				DashiLog.debug("OpenGL platform extensions: {0}", extensions);
				gl.glGetIntegerv(GL2ES2.GL_MAX_VERTEX_UNIFORM_VECTORS, values, 0);
				gl.glGetIntegerv(GL2ES2.GL_MAX_TEXTURE_IMAGE_UNITS, values, 1);
				gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, values, 2);
				DashiLog.debug("GL_MAX_VERTEX_UNIFORM_VECTORS: {0}", values[0]);
				DashiLog.debug("GL_MAX_TEXTURE_IMAGE_UNITS: {0}", values[1]);
				DashiLog.debug("GL_MAX_TEXTURE_SIZE: {0}", values[2]);
			}
			listener.init(drawable);
		}

		@Override
		public void display(GLAutoDrawable drawable) {
			listener.display(drawable);
			RenderStats.displayEnded();
		}

		@Override
		public void dispose(GLAutoDrawable drawable) {
			listener.dispose(drawable);
		}

		@Override
		public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
			DashiLog.info("reshaped to {2}x{3} @ {0},{1}", x, y, w, h);
			listener.reshape(drawable, x, y, w, h);
		}

	}

	private class ShutdownKeyListener implements KeyListener {

		@Override
		public void keyPressed(KeyEvent e) {
			switch (e.getKeySymbol()) {
			case KeyEvent.VK_ESCAPE:
				context.shutdown().run();
				break;
			case KeyEvent.VK_D:
				if (DashiLog.isDebug()) {
					DashiLog.setLevel(Level.INFO);
				} else {
					DashiLog.setLevel(Level.DEBUG);
				}
				break;
			default:
			}
		}

		@Override
		public void keyReleased(KeyEvent e) {
		}
	}
}
