package com.superdashi.gosper.display;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.Collections;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.display.ShaderParams.TextParams;
import com.superdashi.gosper.model.Vertex;
import com.tomgibara.geom.core.Rect;

public class DigitsDisplay implements ElementDisplay {

	private static final float[] NORMAL = {0f, 0f, 1f};

	private final int[] values;
	private final int digits;
	private final int limit;
	private float digitWidth;
	private float digitHeight;
	private Vertex position = Vertex.ORIGIN;
	private DynamicAtlas<Void> atlas;
	private final DynamicAtlas<Void>.Updater[] updaters = new DynamicAtlas.Updater[10];
	private int value;

	private DigitsElement element;

	public DigitsDisplay(int digits, int initialValue) {
		this.digits = digits;
		int pwr = 1;
		for (int i = 0; i < digits; i++) {
			pwr *= 10;
		}
		limit = pwr;
		values = new int[digits];

		setValue(initialValue);
	}

	public void setPosition(Vertex position) {
		this.position = position;
		element.setHandle(position);
		element.computeVertices();
	}

	public void setDigitSize(float w, float h) {
		this.digitWidth = w;
		this.digitHeight = h;
		element.computeVertices();
	}

	@Override
	public void init(RenderContext context) {
		GL2ES2 gl = context.getGL();
		atlas = new DynamicAtlas<>(gl, 1024, 256, true, LinearAllocator.newHorizontal());
		Graphics2D g = atlas.getGraphics();
		g.setColor(Color.BLACK);
		g.fillRect(0, 0, 1024, 256);
		Font f = context.getDisplay().getDefaultFont().deriveFont(100f);
		for (int i = 0; i < 10; i++) {
			updaters[i] = DisplayUtil.renderText(atlas, f, 4f, String.valueOf(i), null).get();
			updaters[i].apply(gl);
		}
		element = new DigitsElement();

		//TODO needs to be handled properly
		Rect rect = context.getDisplay().getOverPlane().trans.transform(Rect.atPoints(1f, 1f, 0.9f, 0.8f));
		setPosition(new Vertex(rect.minX, rect.minY, DashiCamera.OVER_DEPTH));
		setDigitSize(rect.getWidth() / digits, rect.getHeight());
	}

	@Override
	public Collection<Element> getElements() {
		if (element == null) throw new IllegalStateException();
		return Collections.singleton(element);
	}

	@Override
	public void update(RenderContext context) {
		setValue( Math.round(RenderStats.getFPS()) );
	}

	@Override
	public void destroy(GL2ES2 gl) {
		element = null;
		atlas.destroy(gl);
	}

	private void setValue(int value) {
		value = value % limit;
		if (value < 0) value += limit;
		this.value = value;

		//TODO avoid string creation
		String s = Integer.toString(value);
		for (int d = 0; d < digits; d++) {
			int i = s.length() - digits + d;
			values[d] = i < 0 ? 0 : s.charAt(i) - 48;
		}

	}

	private class DigitsElement extends Element {

		private final RenderState required;
		private final int vertexCount;
		private final int triangleCount;
		private final short[] triangles;
		private final float[] vertices;
		private int fgColor = Coloring.argbToRGBA( Argb.WHITE );
		private TextParams textParams = TextParams.creator.create();
		{ textParams.setColor(Argb.BLACK); }

		DigitsElement() {
			required = new RenderState();
			required.setBlend(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			required.setMode(Mode.TEXT);
			required.setTexture(atlas.getTexture());

			triangles = new short[3 * 2 * digits];
			for (int d = 0, i = 0; d < digits; d++) {
				int b = d * 4;
				triangles[i++] = (short) (b + 0);
				triangles[i++] = (short) (b + 1);
				triangles[i++] = (short) (b + 2);
				triangles[i++] = (short) (b + 2);
				triangles[i++] = (short) (b + 3);
				triangles[i++] = (short) (b + 0);
			}
			vertices = new float[3 * 4 * digits];
			vertexCount = 4 * digits;
			triangleCount = 2 * digits;
			setHandle(position);
		}

		@Override
		int getVertexCount() {
			return vertexCount;
		}

		@Override
		int getTriangleCount() {
			return triangleCount;
		}

		@Override
		RenderPhase getRenderPhase() {
			return RenderPhase.OVERLAY;
		}

		@Override
		RenderState getRequiredState() {
			return required;
		}

		@Override
		void appendTo(ElementData data) {
			// triangles
			data.putTriangles(triangles);
			// vertices
			data.vertices.put(vertices);

			// vertex invariants : normals, colours
			int count = vertexCount;
			FloatBuffer normals = data.normals;
			IntBuffer colors = data.colors;
			FloatBuffer shaders = data.shaders;
			for (int i = 0; i < count; i++) {
				normals.put(NORMAL);
				colors.put(fgColor);
				textParams.writeTo(shaders);
			}

			// tex coords
			FloatBuffer texCoords = data.texCoords;
			for (int d = 0; d < digits; d++) {
				Rect r = updaters[values[d]].getRect();
				texCoords.put(r.minX).put(r.minY);
				texCoords.put(r.maxX).put(r.minY);
				texCoords.put(r.maxX).put(r.maxY);
				texCoords.put(r.minX).put(r.maxY);
			}
		}

		private void computeVertices() {
			float z = position.z;
			float minY = position.y;
			float maxY = position.y + digitHeight;
			int i = 0;
			for (int d = 0; d < digits; d++) {
				float minX = position.x + d * digitWidth;
				float maxX = minX + digitWidth;
				vertices[i++] = minX;
				vertices[i++] = maxY;
				vertices[i++] = z;

				vertices[i++] = maxX;
				vertices[i++] = maxY;
				vertices[i++] = z;

				vertices[i++] = maxX;
				vertices[i++] = minY;
				vertices[i++] = z;

				vertices[i++] = minX;
				vertices[i++] = minY;
				vertices[i++] = z;
			}
		}

	}
}
