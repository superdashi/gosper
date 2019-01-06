package com.superdashi.gosper.display;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Optional;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.graphics.util.ImageUtil;

public class DynamicAtlas<H> {

	//NOTE allocators may go through multiple init/destory cycles
	public interface Allocator<H> {

		void init(int width, int height);

		// may return null if full
		Optional<Rectangle> allocate(int w, int h, H hint);

		void release(Rectangle rect);

		void destroy();
	}

	public static class TrivialAllocator implements Allocator<Void> {

		private Rectangle rect = null;

		@Override
		public void init(int width, int height) { }

		@Override
		public Optional<Rectangle> allocate(int w, int h, Void hint) {
			if (rect != null) return Optional.empty();
			rect = new Rectangle(0, 0, w, h);
			return Optional.of(rect);
		}

		@Override
		public void release(Rectangle rect) {
			if (rect == null) throw new IllegalArgumentException("null rect");
			if (this.rect == null) throw new IllegalStateException();
			if (this.rect != rect) throw new IllegalArgumentException("invalid rect");
			this.rect = null;
		}

		@Override
		public void destroy() {
			rect = null;
		}

	}

	private final Texture texture;
	private final BufferedImage image;
	private final Graphics2D graphics;
	private final AWTTextureData imgData;
	private final Allocator<H> allocator;
	private final float widthScale;
	private final float heightScale;

	public DynamicAtlas(GL gl, int width, int height, boolean alpha, Allocator<H> allocator) {
		if (gl == null) throw new IllegalArgumentException("null gl");
		if (allocator == null) throw new IllegalArgumentException("null allocator");

		GLProfile glp = gl.getGLProfile();
		this.allocator = allocator;
		int internalFormat = alpha ? (glp.isGL3() ? GL2ES2.GL_RED : GL.GL_LUMINANCE) : GL.GL_RGBA;
		int sampleSize = alpha ? 1 : 4;

		TextureData data = new TextureData(
				glp,
				internalFormat,
				width,
				height,
				0,
				new GLPixelAttributes(internalFormat, GL.GL_UNSIGNED_BYTE),
				false,
				false,
				true,
				ByteBuffer.allocateDirect(width * height * sampleSize),
				null);
		texture = new Texture(gl, data);
		if (alpha) {
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		} else {
			image = new GLBufferedImage(width, height);
		}
		graphics = image.createGraphics();
		imgData = new AWTTextureData(glp, internalFormat, 0, true, image);
		widthScale = 1f / image.getWidth();
		heightScale = 1f / image.getHeight();
		allocator.init(width, height);
	}

	public Graphics2D getGraphics() {
		return graphics;
	}

	public Optional<DynamicAtlas<H>.Updater> add(int w, int h, H hint) {
		if (w < 1) throw new IllegalArgumentException("non-positive w");
		if (w > image.getWidth()) throw new IllegalArgumentException("w exceeds image width");
		if (h < 1) throw new IllegalArgumentException("non-positive h");
		if (h > image.getHeight()) throw new IllegalArgumentException("h exceeds image height");

		return allocator.allocate(w, h, hint).map(r -> new Updater(r, hint));
	}

	public Texture getTexture() {
		return texture;
	}

	public void update(GL gl) {
		//TODO should this be done?
		// gl.glActiveTexture(GL.GL_TEXTURE0);
		texture.updateSubImage(gl, imgData, 0, 0, 0);
	}

	public void destroy(GL gl) {
		graphics.dispose();
		texture.destroy(gl);
		allocator.destroy();
	}

	// debug only - requires windows
	public void showImage() {
		ImageUtil.showImage("Atlas", image);
	}

	public class Updater {

		private final Rectangle rectangle;
		//TODO consider passing to release?
		private final H hint;
		private final Rect rect;
		private Graphics2D g;

		private Updater(Rectangle rectangle, H hint) {
			this.rectangle = rectangle;
			this.hint = hint;
			rect = Rect.atPoints(
					rectangle.x * widthScale,
					rectangle.y * heightScale,
					(rectangle.x + rectangle.width) * widthScale,
					(rectangle.y + rectangle.height) * heightScale
					);
			g = image.createGraphics();
			//g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.clip(rectangle);
			g.translate(rectangle.x, rectangle.y);
		}

		public Texture getTexture() {
			return texture;
		}

		public H getHint() {
			return hint;
		}

		public int getWidth() {
			return rectangle.width;
		}

		public int getHeight() {
			return rectangle.height;
		}

		public Rect getRect() {
			return rect;
		}

		public Graphics2D getGraphics() {
			checkClosed();
			return g;
		}

		public void apply(GL gl) {
			checkClosed();
			update(gl);
		}

		public void close(GL gl) {
			if (g != null) {
				update(gl);
				allocator.release(rectangle);
				g.dispose();
				g = null;
			}
		}

		private void checkClosed() {
			if (g == null) throw new IllegalStateException("closed");
		}

		private void update(GL gl) {
// DOESN'T WORK ON PI
//			texture.updateSubImage(
//					gl, imgData, 0,
//					rectangle.x, rectangle.y,
//					rectangle.x, rectangle.y,
//					rectangle.width, rectangle.height
//					);

// WORKS
//			texture.updateImage(
//					gl, imgData
//					);

// DOESN'T WORK ON PI
//			texture.bind(gl);
//			gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
//			gl.glPixelStorei(GL2ES2.GL_UNPACK_ROW_LENGTH, imgData.getRowLength());
//			gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_ROWS, rectangle.y);
//			gl.glPixelStorei(GL2ES2.GL_UNPACK_SKIP_PIXELS, rectangle.x);
//			gl.glTexSubImage2D(texture.getTarget(), 0,
//					rectangle.x, rectangle.y, rectangle.width, rectangle.height,
//					imgData.getPixelFormat(), imgData.getPixelType(),
//					imgData.getBuffer());
			DynamicAtlas.this.update(gl);
		}
	}


}
