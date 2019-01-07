/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.display;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import com.superdashi.gosper.core.Resolution;

public final class CharMap {

	final CharLookup lookup;
	final CharData data;
	private Resolution charResolution;

	CharMap(CharLookup lookup, CharData data) {
		this.lookup = lookup;
		this.data = data;
	}

	private Texture texture = null;

	void init(GL gl) {
		if (texture != null) throw new IllegalStateException("initialized");
		BufferedImage img;
		{ // produce image
			BufferedImage loaded = data.loadChars();
			img = new BufferedImage(loaded.getWidth(), loaded.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
			Graphics2D g = img.createGraphics();
			g.drawImage(loaded, 0, 0, null);
			g.dispose();
		}

		{ // set up texture
			int format = gl.isGL3() ? GL2ES2.GL_RED : GL.GL_LUMINANCE;
			TextureData data = new AWTTextureData(gl.getGLProfile(), format, 0, false, img);
			//data.setPixelAttributes(new GLPixelAttributes(GL2ES2.GL_RED, GL.GL_UNSIGNED_BYTE));
			texture = new Texture(gl, data);
			texture.enable(gl);
			texture.setMustFlipVertically(false);
			texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
			texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		}

		{ // compute resolution
			System.out.println(img.getWidth() + " " + data.cols());
			System.out.println(img.getHeight() + " " + data.rows());
			charResolution = Resolution.sized(img.getWidth() / data.cols(), img.getHeight() / data.rows());
		}
	}

	void bind(GL gl) {
		gl.glActiveTexture(GL.GL_TEXTURE2);
		texture.bind(gl);
	}

	boolean isInitialized() {
		return texture != null;
	}

	void destroy(GL gl) {
		checkInitialized();
		texture.destroy(gl);
	}

	Resolution getCharResolution() {
		checkInitialized();
		return charResolution;
	}

	private void checkInitialized() {
		if (!isInitialized()) throw new IllegalStateException("not initialized");
	}
}
