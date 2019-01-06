package com.superdashi.gosper.display;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.texture.Texture;

final class RenderState {

	private boolean blend;
	private int blendSrc;
	private int blendDst;

	private boolean texture;
	private int textureTarget;
	private int textureId;

	private boolean data;
	private int dataTarget;
	private int dataId;

	private RenderPalette palette;
	private CharMap charMap;

	private Mode mode;

	RenderState() { }

	RenderState(Mode mode) {
		if (mode == null) throw new IllegalArgumentException("null mode");
		this.mode = mode;
	}

	void setBlend(int blendSrc, int blendDst) {
		blend = true;
		this.blendSrc = blendSrc;
		this.blendDst = blendDst;
	}

	void clearBlend() {
		blend = false;
	}

	void setTexture(int textureTarget, int textureId) {
		texture = true;
		this.textureTarget = textureTarget;
		this.textureId = textureId;
	}

	void setTexture(Texture texture) {
		if (texture == null) throw new IllegalArgumentException("null texture");
		setTexture(texture.getTarget(), texture.getTextureObject());
	}

	void clearTexture() {
		texture = false;
	}

	void setData(int dataTarget, int dataId) {
		data = true;
		this.dataTarget = dataTarget;
		this.dataId = dataId;
	}

	void setData(Texture data) {
		if (data == null) throw new IllegalArgumentException("null data");
		setData(data.getTarget(), data.getTextureObject());
	}

	void clearData() {
		data = false;
	}

	void setPalette(RenderPalette palette) {
		if (palette == null) throw new IllegalArgumentException("null palette");
		if (!palette.isValid()) throw new IllegalArgumentException("invalid palette");
		this.palette = palette;
	}

	void clearPalette() {
		palette = null;
	}

	void setCharMap(CharMap charMap) {
		if (charMap == null) throw new IllegalArgumentException("null charMap");
		this.charMap = charMap;
	}

	void clearCharMap() {
		charMap = null;
	}

	void setMode(Mode mode) {
		if (mode == null) throw new IllegalArgumentException("null mode");
		this.mode = mode;
	}

	void clearMode() {
		mode = null;
	}

	Mode getMode() {
		return mode;
	}

	void applyTo(GL2ES2 gl, ModeShaders mshdrs) {
		if (blend) gl.glBlendFuncSeparate(blendSrc, blendDst, GL.GL_ONE, GL.GL_ONE);
		if (texture) {
			gl.glActiveTexture(GL.GL_TEXTURE0);
			gl.glBindTexture(textureTarget, textureId);
		}
		if (data) {
			gl.glActiveTexture(GL.GL_TEXTURE1);
			gl.glBindTexture(dataTarget, dataId);
		}
		if (palette != null) {
			mshdrs.setPalette(palette);
		}
		if (charMap != null) {
			charMap.bind(gl);
			mshdrs.setCharMap(charMap);
		}
		if (mode != null) mshdrs.useMode(gl, mode);
	}

	void applyTo(RenderState that) {
		if (blend) {
			that.blend = blend;
			that.blendSrc = blendSrc;
			that.blendDst = blendDst;
		}
		if (texture) {
			that.texture = texture;
			that.textureTarget = textureTarget;
			that.textureId = textureId;
		}
		if (data) {
			that.data = data;
			that.dataTarget = dataTarget;
			that.dataId = dataId;
		}
		if (palette != null) {
			that.palette = palette;
		}
		if (charMap != null) {
			that.charMap = charMap;
		}
		if (mode != null) {
			that.mode = mode;
		}
	}

	boolean isSatisfiedBy(RenderState that) {
		if (this.mode != null) {
			if (that.mode != this.mode) return false;
		}

		if (this.palette != null) {
			if (that.palette != this.palette) return false;
		}
		if (this.charMap != null) {
			if (that.charMap != this.charMap) return false;
		}

		if (this.blend) {
			if (!that.blend) return false;
			if (
					this.blendSrc != that.blendSrc ||
					this.blendDst != that.blendDst
					) return false;
		}
		if (this.texture) {
			if (!that.texture) return false;
			if (
					this.textureTarget != that.textureTarget ||
					this.textureId != that.textureId
					) return false;
		}
		if (this.data) {
			if (!that.data) return false;
			if (
					this.dataTarget != that.dataTarget ||
					this.dataId != that.dataId
					) return false;
		}
		return true;
	}

	boolean satisfies(RenderState that) {
		return that.isSatisfiedBy(this);
	}
}