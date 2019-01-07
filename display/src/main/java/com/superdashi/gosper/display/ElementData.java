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

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.display.ShaderParams.EmptyParams;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.Stores;

public class ElementData {

	static {
		DashiLog.debug("Native byte order is {0}", ByteOrder.nativeOrder());
	}

	private static final int SIZEOF_BYTE  = 1;
	private static final int SIZEOF_SHORT = 2;
	private static final int SIZEOF_FLOAT = 4;
	private static final int SIZEOF_INT   = 4;

	private static final int COMP_VRT = 3;
	private static final int COMP_NRM = 3;
	private static final int COMP_COL = 1; // 1 int = 4 bytes
	private static final int COMP_TEX = 2;
	private static final int COMP_ANM = 1;
	private static final int COMP_SHD = 4;

	private static ByteBuffer bytes(int capacity) {
		ByteBuffer b = ByteBuffer.allocateDirect(capacity * SIZEOF_BYTE);
		b.order(ByteOrder.nativeOrder());
		return b;
	}

	private static FloatBuffer floats(int capacity) {
		return bytes(capacity * SIZEOF_FLOAT).asFloatBuffer();
	}

	private static IntBuffer ints(int capacity) {
		ByteBuffer b = ByteBuffer.allocateDirect(capacity * SIZEOF_INT);
		b.order(ByteOrder.BIG_ENDIAN);
		return b.asIntBuffer();
	}

	private static ShortBuffer shorts(int capacity) {
		return bytes(capacity * SIZEOF_SHORT).asShortBuffer();
	}

	private static final int ATTR_COUNT = 8;

	private int capacity     = -1; // the maximum number of vertices that can be accommodated
	private int vertexSize   = -1; // the number of vertices for which data is to be stored
	private int triangleSize = -1; // the number of triangles to be drawn
	private int nextTriangle = -1; // index of next triangle to be drawn

	private final Attr[] attrs = new Attr[ATTR_COUNT];
	private final int triangleHandle;

	public FloatBuffer vertices;
	public FloatBuffer normals;
	public IntBuffer colors;
	public FloatBuffer texCoords;
	public ByteBuffer anims;
	public FloatBuffer handles;
	public FloatBuffer shaders;

	private ShortBuffer triangles;
	private short[] triangleBuffer = new short[30];
	private int triangleOffset; // the offset for element triangle indices
	private int triangleLimit; // the maximum permitted triangle index for the current element

	public ElementData(GL2ES2 gl) {
		int[] vboHandles = new int[ATTR_COUNT + 1];
		gl.glGenBuffers(ATTR_COUNT + 1, vboHandles, 0);
		attrs[0] = new Attr(vboHandles[0], ModeShaders.INDEX_POSITION,  COMP_VRT);
		attrs[1] = new Attr(vboHandles[1], ModeShaders.INDEX_NORMAL,    COMP_NRM);
		attrs[2] = new Attr(vboHandles[2], ModeShaders.INDEX_COLOR_RG,  COMP_COL);
		attrs[3] = attrs[2].secondary(vboHandles[3], ModeShaders.INDEX_COLOR_BA);
		attrs[4] = new Attr(vboHandles[4], ModeShaders.INDEX_TX_COORD,  COMP_TEX);
		attrs[5] = new Attr(vboHandles[5], ModeShaders.INDEX_ANIMATION, COMP_ANM);
		attrs[6] = new Attr(vboHandles[6], ModeShaders.INDEX_HANDLE,    COMP_VRT);
		attrs[7] = new Attr(vboHandles[7], ModeShaders.INDEX_SHADER,    COMP_SHD);
		vertexCapacity(0);
		sizeVertices(0);

		triangleHandle = vboHandles[ATTR_COUNT];
		triangleCapacity(0);
		sizeTriangles(0);
	}

	public void vertexCapacity(int newCapacity) {
		if (newCapacity < 0) throw new IllegalArgumentException("negative newCapacity");
		if (newCapacity == capacity) return;
		capacity = newCapacity;
		vertices  = attrs[0].newFloatBuffer();
		normals   = attrs[1].newFloatBuffer();
		colors    = attrs[2].newIntBuffer();
		//3 is secondary
		texCoords = attrs[4].newFloatBuffer();
		anims     = attrs[5].newByteBuffer();
		handles   = attrs[6].newFloatBuffer();
		shaders   = attrs[7].newFloatBuffer();
	}

	public void triangleCapacity(int newCapacity) {
		if (newCapacity < 0) throw new IllegalArgumentException("negative newCapacity");
		if (triangles != null && triangles.capacity() == newCapacity) return;
		triangles = shorts(newCapacity);
	}

	public void populate(Store<Element> els) {
		{ // resize
			int vertexCount = 0;
			int triangleCount = 0;
			for (Element el : els) {
				vertexCount += el.getVertexCount();
				triangleCount += el.getTriangleCount();
			}
			sizeVertices(vertexCount);
			sizeTriangles(triangleCount * 3);
			reset();
		}
		int v = 0;
		int tp = triangles.position();
		int sp = shaders.position();
		for (Element el : els) {
			int vc = el.getVertexCount();
			// set up the information for potential calls to putTriangles();
			triangleOffset = v;
			triangleLimit = vc;
			el.appendTo(this);
			int ntp = triangles.position();
			int nsp = shaders.position();
			// shader parameters are little used, so pad them automatically for elements
			if (nsp == sp) {
				EmptyParams.instance().writeTo(shaders, el.getVertexCount());
				nsp = shaders.position();
			}
			// traditionally, triangles weren't used, cope with them not being supplied
			if (ntp == tp) {
				for (int i = v; i < v + vc; i++) {
					triangles.put((short) i);
				}
				ntp = triangles.position();
			}
			el.appendAnimTo(this);
			tp = ntp;
			sp = nsp;
			v += vc;
		}
		{ // checks
			for (Attr attr : attrs) {
				if (attr.hasRemaining()) throw new IllegalStateException("incorrect count (got " + attr.buffer.position() + ", expected " + attr.buffer.limit() + ") for attr: " + attr);
			}
			if (triangles.hasRemaining()) throw new IllegalStateException("incorrect triangle count (got " + triangles.position() + " vertices, expected " + triangles.limit() + ")");
		}
		// prep for rendering by rewinding the buffers
		reset();
	}

	public void putTriangles(short... indices) {
		int length = indices.length;
		if (length > triangleBuffer.length) {
			triangleBuffer = indices.clone();
		} else {
			System.arraycopy(indices, 0, triangleBuffer, 0, length);
		}
		//TODO consider dropping the index test
		for (int i = 0; i < length; i++) {
			short index = triangleBuffer[i];
			if (index < 0 || index >= triangleLimit) throw new IllegalArgumentException("invalid index " + index + ", limit is: " + triangleLimit);
			//TODO need to guard against overflow
			triangleBuffer[i] = (short) (index + triangleOffset);
		}
		triangles.put(triangleBuffer, 0, length);
	}

	public void bind(GL2ES2 gl) {
		for (Attr attr : attrs) {
			attr.bind(gl);
		}
		gl.glBindBuffer(GL2ES2.GL_ELEMENT_ARRAY_BUFFER, triangleHandle);
		gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, SIZEOF_SHORT * triangleSize, triangles, GL.GL_DYNAMIC_DRAW);
		nextTriangle = 0;
	}

	public void enable(GL2ES2 gl) {
		for (Attr attr : attrs) {
			attr.enable(gl);
		}
	}

	public void disable(GL2ES2 gl) {
		for (Attr attr : attrs) {
			attr.disable(gl);
		}
	}

	public boolean draw(GL2ES2 gl, int triangleCount) {
		if (nextTriangle == -1) throw new IllegalStateException("draw when not bound?");
		if (triangleCount == 0) return false;
		int limit = nextTriangle + triangleCount;
		//TODO store limit?
		if (limit > triangleSize) throw new IllegalArgumentException("triangle count (" + triangleCount + ") exceeds remaining (" + (triangleSize - nextTriangle) + ") of (" + vertexSize + ")");
		draw(gl, nextTriangle, triangleCount);
		nextTriangle = limit;
		return true;
	}

	public boolean finish(GL2ES2 gl) {
		if (nextTriangle == -1) throw new IllegalStateException("finish when not bound?");
		int remaining = triangleSize / 3 - nextTriangle;
		boolean draw = remaining != 0;
		if (draw) draw(gl, nextTriangle, remaining);
		nextTriangle = -1;
		return draw;
	}

	private void draw(GL2ES2 gl, int triangleIndex, int triangleCount) {
		gl.glDrawElements(GL.GL_TRIANGLES, triangleCount * 3, GL.GL_UNSIGNED_SHORT, triangleIndex * 3 * SIZEOF_SHORT);
	}

	public void delete(GL2ES2 gl) {
		int[] vboHandles = new int[ATTR_COUNT];
		for (int i = 0; i < ATTR_COUNT; i++) {
			vboHandles[i] = attrs[i].handle;
		}
		gl.glDeleteBuffers(ATTR_COUNT, vboHandles, 0);
	}

	private void sizeVertices(int newSize) {
		if (newSize < 0) throw new IllegalArgumentException("negative vertices size");
		if (newSize == vertexSize) return;
		if (newSize > capacity || newSize < capacity / 2) vertexCapacity(newSize);
		vertexSize = newSize;
		for (Attr attr : attrs) {
			attr.limit();
		}
		DashiLog.trace("vertex data resized to {0}", newSize);
	}

	private void sizeTriangles(int newSize) {
		if (newSize < 0) throw new IllegalArgumentException("negative triangles size");
		if (newSize == triangleSize) return;
		if (newSize > triangles.capacity() || newSize < capacity / 2) triangleCapacity(newSize);
		triangleSize = newSize;
		//NOTE: workaround for java change
		((Buffer) triangles).limit(newSize);
		DashiLog.trace("triangle data resized to {0}", newSize);
	}

	private void reset() {
		int count = 0;
		for (Attr attr : attrs) {
			count += attr.rewind();
		}
		RenderStats.recordVBOFloats(count);
		RenderStats.recordTriangles(triangles.position());
		triangles.rewind();
	}

	private class Attr {

		private final int handle; // vbo
		private final int index; // varying
		private final int components;
		private final Attr primary;
		private Buffer buffer;

		Attr(int handle, int index, int components) {
			this.handle = handle;
			this.index = index;
			this.components = components;
			primary = null;
			DashiLog.debug("Defining primary Attr: handle: {0}, index: {1}, components: {2}", handle, index, components);
		}

		private Attr(Attr primary, int handle, int index) {
			this.primary = primary;
			this.handle = handle;
			this.index = index;
			this.components = primary.components;
			this.buffer = null;
		}

		Attr secondary(int handle, int index) {
			return new Attr(this, handle, index);
		}

		ByteBuffer newByteBuffer() {
			ByteBuffer byteBuffer = bytes(capacity * components);
			buffer = byteBuffer;
			return byteBuffer;
		}

		FloatBuffer newFloatBuffer() {
			FloatBuffer b = floats(capacity * components);
			buffer = b;
			return b;
		}

		IntBuffer newIntBuffer() {
			IntBuffer b = ints(capacity * components);
			buffer = b;
			return b;
		}

		void limit() {
			if (buffer != null) buffer.limit(vertexSize * components);
		}

		boolean hasRemaining() {
			return buffer == null ? false : buffer.hasRemaining();
		}

		// returns number of floats
		int rewind() {
			if (buffer == null) return 0;
			int count = buffer.position();
			buffer.rewind();
			return count;
		}

		void bind(GL2ES2 gl) {
			boolean isPrimary = primary == null;
			Buffer buffer = isPrimary ? this.buffer : primary.buffer;

			gl.glBindBuffer(GL2ES2.GL_ARRAY_BUFFER, handle);
			if (buffer instanceof FloatBuffer) {
				gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexSize * SIZEOF_FLOAT * components, buffer, GL.GL_STATIC_DRAW);
				gl.glVertexAttribPointer(index, components, GL2ES2.GL_FLOAT, false, 0, 0);
			} else if (buffer instanceof IntBuffer) {
//				if (isPrimary && swapBytes) {
//					IntBuffer b = (IntBuffer) buffer;
//					int c = buffer.capacity();
//					for (int i = 0; i < c; i++) {
//						int v = b.get(i);
//						int x = (v >>> 24);
//						int y = (v  >> 16) & 0xff;
//						int z = (v  >>  8) & 0xff;
//						int w = (v       ) & 0xff;
//						b.put(i, (y << 24) | (x << 16) | (w << 8) | (z     ));
//					}
//				}
				gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexSize * SIZEOF_INT * components, buffer, GL.GL_STATIC_DRAW);
				gl.glVertexAttribPointer(index, 2, GL2ES2.GL_UNSIGNED_BYTE, true, 4, isPrimary ? 0 : 2);
			} else if (buffer instanceof ByteBuffer) {
				gl.glBufferData(GL.GL_ARRAY_BUFFER, vertexSize * SIZEOF_BYTE * components, buffer, GL.GL_STATIC_DRAW);
				gl.glVertexAttribPointer(index, components, GL2ES2.GL_BYTE, false, 0, 0);
			} else throw new IllegalStateException("Unsupported buffer type: " + buffer.getClass().getName());
		}

		void enable(GL2ES2 gl) {
			gl.glEnableVertexAttribArray(index);
		}

		void disable(GL2ES2 gl) {
			gl.glDisableVertexAttribArray(index);
		}

		@Override
		public String toString() {
			return "vbo handle: " + handle + " input index: " + index;
		}
	}

}
