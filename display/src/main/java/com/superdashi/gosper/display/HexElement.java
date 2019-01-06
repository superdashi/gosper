package com.superdashi.gosper.display;

import static com.superdashi.gosper.display.DisplayUtil.transformedCoords;

import java.nio.IntBuffer;

import com.superdashi.gosper.color.Coloring;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.transform.Transform;

public class HexElement extends Element {


	static final float X = Angles.SIN_PI_BY_THREE;
	static final float Y = Angles.COS_PI_BY_THREE;
	static final float[] COORDS = {
		0, 0,    1,  0,    X,  Y,
		0, 0,    X,  Y,   -X,  Y,
		0, 0,   -X,  Y,   -1,  0,
		0, 0,   -1,  0,   -X, -Y,
		0, 0,   -X, -Y,    X, -Y,
		0, 0,    X, -Y,    1,  0,
	};
	static final float[] NORMAL = {0f, 0f, 1f};

	private final RenderState required = new RenderState();
	private final float[] vertices;
	private final float[] texCoords;
	private final int[] colors = new int[6 * 3];
	private final ShaderParams params;

	public HexElement(Transform vt, Transform tt, float z, Mode mode, int innerColor, int outerColor, ShaderParams params) {
		required.setMode(mode);
		vertices = DisplayUtil.projectToZ(transformedCoords(vt, COORDS), z);
		texCoords = transformedCoords(tt, COORDS);
		int ic = Coloring.argbToRGBA(innerColor);
		int oc = Coloring.argbToRGBA(outerColor);
		IntBuffer bgb = IntBuffer.wrap(colors);
		for (int i = 0; i < 6; i++) {
			bgb.put(ic).put(oc).put(oc);
		}

		this.params = params;
	}

	@Override
	RenderPhase getRenderPhase() {
		return RenderPhase.PANEL;
	}

	@Override
	RenderState getRequiredState() {
		return required;
	}

	@Override
	int getVertexCount() {
		return 18;
	}

	@Override
	public void appendTo(ElementData data) {
		data.vertices.put(vertices);
		for (int i = 0; i < 18; i++) data.normals.put(NORMAL);
		data.colors.put(colors);
		data.texCoords.put(texCoords);
		params.writeTo(data.shaders, 18);
	}

}
