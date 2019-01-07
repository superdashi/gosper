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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.model.Normal;
import com.tomgibara.storage.Store;

public class ModeShaders {

	private static final int ANIM_BUCKETS = 20;

	public static final int INDEX_POSITION  = 0;
	public static final int INDEX_NORMAL    = 1;
	public static final int INDEX_COLOR_RG  = 2;
	public static final int INDEX_COLOR_BA  = 3;
	public static final int INDEX_TX_COORD  = 4;
	public static final int INDEX_HANDLE    = 5;
	public static final int INDEX_ANIMATION = 6;
	public static final int INDEX_SHADER    = 7;

	public static final int MAX_ANIMS = 10;

	private static FloatBuffer floatBuffer(int size) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(size * 4);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		// Note: call to mark() here is absolutely necessary, or JOGL doesn't bind the uniform
		FloatBuffer fb = buffer.asFloatBuffer();
		fb.mark();
		return fb;
	}

	private static FloatBuffer mat4Buffer(int size) {
		return floatBuffer(4 * 4 * size);
	}

	private static FloatBuffer vec4Buffer(int size) {
		return floatBuffer(4 * size);
	}

	private final ShaderState st;
	private final PMVMatrix pmv;
	private final FloatBuffer[] animBuckets = new FloatBuffer[ANIM_BUCKETS];
	private final FloatBuffer[] colorBuckets = new FloatBuffer[ANIM_BUCKETS];
	private final FloatBuffer ambientColor = floatBuffer(3);
	private final FloatBuffer lightDirection = floatBuffer(3);
	private final FloatBuffer lightColor = floatBuffer(3);
	private final FloatBuffer charScale = floatBuffer(2);
	private final GLUniformData pmvUniform;
	private final GLUniformData[] animUniforms = new GLUniformData[ANIM_BUCKETS];
	private final GLUniformData[] colorUniforms = new GLUniformData[ANIM_BUCKETS];
	private final GLUniformData timeUniform;
	private final GLUniformData texUniform;
	private final GLUniformData dataUniform;
	private final GLUniformData charsUniform;
	private final GLUniformData palettesUniform;
	private final GLUniformData ambientUniform;
	private final GLUniformData lightDirUniform;
	private final GLUniformData lightColUniform;
	private final GLUniformData paletteUniform;
	private final GLUniformData charScaleUniform;
	private final Map<Mode, ShaderProgram> sps = new EnumMap<>(Mode.class);
	private final long originTime = System.currentTimeMillis();

	private final Set<Mode> uniformedModes = EnumSet.noneOf(Mode.class);
	private Mode currentMode = null;
	private final int[] currentBuckets = new int[Mode.values().length]; //TODO store in a constant?

	public ModeShaders(GL2ES2 gl) {
		for (int i = 0; i < ANIM_BUCKETS; i++) {
			animBuckets[i]  = mat4Buffer(MAX_ANIMS);
			colorBuckets[i] = mat4Buffer(MAX_ANIMS);
		}

		ShaderCode vp0 = ShaderCode.create(
				gl,
				GL2ES2.GL_VERTEX_SHADER,
				this.getClass(),
				"",
				"bin",
				"vertex",
				true
				);

		Mode[] modes = Mode.values();
		ShaderCode[] fps = new ShaderCode[modes.length];
		for (Mode mode : modes) {
			ShaderCode fp = ShaderCode.create(
					gl,
					GL2ES2.GL_FRAGMENT_SHADER,
					this.getClass(),
					"",
					"bin",
					"fragment",
					true
					);
			// define MODE
			fp.insertShaderSource(0, 0, "#define MODE_" + mode.unlit().name() + "\n");
			// define TWO_SIDED
			if (mode.twoSided) fp.insertShaderSource(0, 0, "#define TWO_SIDED " + mode.twoSided + "\n");
			// define LIT
			if (mode.lit) fp.insertShaderSource(0, 0, "#define LIT " + mode.lit + "\n");
			if (DashiLog.isTrace()) {
				CharSequence[][] src = fp.shaderSource();
				for (CharSequence[] seqs : src) {
					DashiLog.trace("fragment shader source for {0}", mode);
					for (CharSequence seq : seqs) {
						DashiLog.trace(seq.toString());
					}
					DashiLog.trace("----------------------");
				}
			}
			fps[mode.ordinal()] = fp;
		}

		// define MAX_ANIMS
		vp0.insertShaderSource(0, 0, "#define MAX_ANIMS " + MAX_ANIMS + "\n");

		// OpenGL 3 core compatibility
		if (gl.isGL3core()) {
			DashiLog.debug("GL3 core detected: explicit add #version 130 to shaders");
			vp0.insertShaderSource(0, 0, "#version 130\n");
			for (ShaderCode fp : fps) fp.insertShaderSource(0, 0, "#version 130\n");
		}

		for (Mode mode : modes) {
			ShaderProgram sp = new ShaderProgram();
			sp.add(gl,vp0, System.err);
			sp.add(gl,fps[mode.ordinal()], System.err);
			sps.put(mode, sp);
			int program = sp.program();
			gl.glBindAttribLocation(program, INDEX_POSITION,  "a_position" );
			gl.glBindAttribLocation(program, INDEX_NORMAL,    "a_normal"   );
			gl.glBindAttribLocation(program, INDEX_COLOR_RG,  "a_color_rg" );
			gl.glBindAttribLocation(program, INDEX_COLOR_BA,  "a_color_ba" );
			gl.glBindAttribLocation(program, INDEX_TX_COORD,  "a_tx_coord" );
			gl.glBindAttribLocation(program, INDEX_HANDLE   , "a_handle"   );
			gl.glBindAttribLocation(program, INDEX_SHADER   , "a_shader"   );
			gl.glBindAttribLocation(program, INDEX_ANIMATION, "a_animation");
		}

		st = new ShaderState();
		for (ShaderProgram sp : sps.values()) {
			DashiLog.debug("Attaching shader program");
			st.attachShaderProgram(gl, sp, false);
		}

		pmv = new PMVMatrix();
		pmvUniform = new GLUniformData("u_projection", 4, 4, pmv.glGetPMvMvitMatrixf());
		for (int i = 0; i < ANIM_BUCKETS; i++) {
			animUniforms [i] = new GLUniformData("u_animation", 4, 4, animBuckets [i]);
			colorUniforms[i] = new GLUniformData("u_color"    , 4, 4, colorBuckets[i]);
		}

		texUniform = new GLUniformData("u_texture", 0);
		dataUniform = new GLUniformData("u_data", 1);
		charsUniform = new GLUniformData("u_chars", 2);
		palettesUniform = new GLUniformData("u_palettes", 3);
		timeUniform = new GLUniformData("u_time", 0f);
		ambientUniform = new GLUniformData("u_ambient_color", 3, ambientColor);
		lightDirUniform = new GLUniformData("u_light_direction", 3, lightDirection);
		lightColUniform = new GLUniformData("u_light_color", 3, lightColor);
		paletteUniform = new GLUniformData("u_palette", 0f);
		charScaleUniform = new GLUniformData("u_char_scale", 2, charScale);
		Arrays.fill(currentBuckets, -1);
	}

	public PMVMatrix getMatrix() {
		return pmv;
	}

	// only needs to be called if matrix is changed after program is attached
	public void updateMatrix(GL2ES2 gl) {
		st.uniform(gl, pmvUniform);
	}

	public void setAmbientColor(int color) {
		ambientColor.reset();
		Coloring.writeOpaqueColor(color, ambientColor);
		ambientColor.reset();
	}

	public void setLightDirection(Normal normal) {
		DashiLog.debug("light direction {0}", normal);
		lightDirection.reset();
		normal.asVector().writeToBuffer(lightDirection);
		lightDirection.reset();
	}

	public void setLightColor(int color) {
		lightColor.reset();
		Coloring.writeOpaqueColor(color, lightColor);
		lightColor.reset();
	}

	public void setPalette(RenderPalette palette) {
		paletteUniform.setData(palette.index * RenderPalettes.UNIFORM_SCALE);
	}

	public void setCharMap(CharMap charMap) {
		CharData data = charMap.data;
		charScale.put(0, 1f / data.cols());
		charScale.put(1, 1f / data.rows());
	}

	public float toShaderTime(long time) {
		return (time - originTime) / 1000f;
	}

	public long fromShaderTime(float t) {
		return originTime + (long) (t * 1000f);
	}

	public long millisBetweenTimes(float s, float t) {
		return (long) ((t - s) * 1000f);
	}

	public void prepare(long now, Store<Element> els) {
		DashiLog.trace("starting mode shaders");
		// assign animations buckets and index
		// do something simple initially

		// first identify all the active animations
		// map allows us to reuse anim assignments that fall into the same bucket
		Map<ElementAnim, Element> processed = new IdentityHashMap<>();
		for (Element el : els) {
			ElementAnim anim = el.anim;
			if (anim != null) processed.put(anim, null);
		}
		DashiLog.trace("number of anims is {0}", processed.size());

		// then update them all to the correct time
		for (ElementAnim anim : processed.keySet()) {
			anim.applyAtTime(now);
		}

		// now assign animations to buckets
		int[] bucketCounts = new int[ANIM_BUCKETS]; //we use an array and not just a counter in case we want a better algo in the future
		int currentBucket = 0;
		for (Element el : els) {
			ElementAnim anim = el.anim;
			if (anim == null || currentBucket == ANIM_BUCKETS) {
				el.animBucket = -1;
				el.animIndex = -1;
				continue;
			}
			Element pre = processed.get(anim);
			// note: this uses up more bucket space but reduces scope for switching
			// the intuition here is that it's probable that elements in close rendering may share the same animation
			// (eg. animation of multi-element models)
			// pre-identifying the mode change boundaries could lead to greater efficiency but is far more complex
			if (pre != null && pre.animBucket == currentBucket) {
				el.animIndex = pre.animIndex;
				el.animBucket = pre.animBucket;
				continue;
			}
			int count = bucketCounts[currentBucket];
			if (count == MAX_ANIMS) {
				if (++currentBucket == ANIM_BUCKETS) {
					DashiLog.warn("anim bucket capacity exausted");
					DashiLog.debug("anim bucket capacity exausted for element at index {0} with {1} elements and {2} anims", el.index, els.count(), processed.keySet().size());
					el.animBucket = -1;
					el.animIndex = -1;
					continue;
				}
				count = bucketCounts[currentBucket]; // will be zero (for now)
			}
			el.animBucket = currentBucket;
			el.animIndex = count;

			FloatBuffer animBuffer = animBuckets[currentBucket];
			animBuffer.position(count * 16);
			animBuffer.put(anim.mat4.getMatrix());

			FloatBuffer colorBuffer = colorBuckets[currentBucket];
			colorBuffer.position(count * 16);
			colorBuffer.put(anim.color.getMatrix());

			processed.put(anim, el);
			bucketCounts[currentBucket] = count + 1;
		}

		// finish by resetting buffers
		for (int i = 0; i < ANIM_BUCKETS; i++) {
			animBuckets[i].reset();
			colorBuckets[i].reset();
		}

		timeUniform.setData( toShaderTime(now) );
	}

	public void useMode(GL2ES2 gl, Mode mode) {
		if (currentMode == mode) {
			DashiLog.trace("shader already in mode {0}", mode);
			return;
		}
		DashiLog.trace("shader switched to mode {0}", mode);
		st.attachShaderProgram(gl, sps.get(mode), true);
		currentMode = mode;
		if (!uniformedModes.contains(mode)) {
			DashiLog.trace("assigning shader uniforms for mode {0}", mode);
			assignRegularUniforms(gl);
			uniformedModes.add(mode);
		}
	}

	public void useAnimBucket(GL2ES2 gl, int animBucket) {
		int m = currentMode.ordinal();
		if (currentBuckets[m] == animBucket) {
			DashiLog.trace("shader for mode {0} already using anim bucket {1}", currentMode, animBucket);
			return;
		}
		DashiLog.trace("shader for mode {0} switched to anim bucket {1}", currentMode, animBucket);
		assignAnimUniforms(gl, animBucket);
		currentBuckets[m] = animBucket;
	}

	public int currentBucket() {
		return currentMode == null ? -1 : currentBuckets[currentMode.ordinal()];
	}

	public void complete() {
		DashiLog.trace("finishing with mode shaders");
		uniformedModes.clear();
		currentMode = null;
		Arrays.fill(currentBuckets, -1);
	}

	public void dispose(GL2ES2 gl) {
		DashiLog.debug("disposing of mode shaders");
		st.useProgram(gl, false);
		st.destroy(gl);
	}

	private void assignRegularUniforms(GL2ES2 gl) {
		RenderStats.recordModeSwitch();
		// assign the time
		st.uniform(gl, timeUniform);
		// assign the textures
		st.uniform(gl, texUniform);
		st.uniform(gl, dataUniform);
		st.uniform(gl, charsUniform);
		st.uniform(gl, palettesUniform);
		st.uniform(gl, charScaleUniform);
		// assign the matrix
		st.uniform(gl, pmvUniform);
		// assign the ambient color
		st.uniform(gl, ambientUniform);
		// assign the light direction
		st.uniform(gl, lightDirUniform);
		// assign the light color
		st.uniform(gl, lightColUniform);
		// assign the palette
		st.uniform(gl, paletteUniform);
	}

	private void assignAnimUniforms(GL2ES2 gl, int animBucket) {
		RenderStats.recordAnimSwitch();
		// assign the animation matrices
		st.uniform(gl, animUniforms[animBucket]);
		// assign the colors
		st.uniform(gl, colorUniforms[animBucket]);
	}

}
