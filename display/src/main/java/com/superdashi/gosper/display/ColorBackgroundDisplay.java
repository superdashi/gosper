package com.superdashi.gosper.display;

import java.util.Collection;
import java.util.Collections;

import com.jogamp.opengl.GL2ES2;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.superdashi.gosper.core.Geometry;
import com.superdashi.gosper.display.ShaderParams.EmptyParams;
import com.superdashi.gosper.display.ShaderParams.NoiseParams;
import com.superdashi.gosper.display.ShaderParams.OpaqueColorParams;
import com.superdashi.gosper.display.ShaderParams.PlainParams;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntRect;

public class ColorBackgroundDisplay implements ElementDisplay {

	interface Effect {

		Mode getMode();

		float[] getShaderParams(DisplayContext context);

		float[] getTextureCoords(DisplayContext context);
	}

	public static Effect specular(int specular) {
		return new Effect() {
			@Override
			public Mode getMode() { return specular == 0 ? Mode.PLAIN : Mode.PLAIN_LIT; }

			@Override
			public float[] getShaderParams(DisplayContext context) {
				return PlainParams.instance(specular).toFloats(4);
			}

			@Override
			public float[] getTextureCoords(DisplayContext context) {
				return RectElement.defaultTexCoords(Corner.BL);
			}
		};
	}

	public static Effect noise(Coloring c) {
		return new Effect() {
			@Override
			public Mode getMode() { return Mode.NOISE; }

			@Override
			public float[] getShaderParams(DisplayContext context) {
				NoiseParams[] ps = NoiseParams.creator.create(4);
				OpaqueColorParams.quadColors(c, ps);
				return ShaderParams.toFloats(ps);
			}

			@Override
			public float[] getTextureCoords(DisplayContext context) {
				Rect rect = Geometry.asRect(context.getViewport().area);
				return RectElement.rectTexCoords(rect.apply(Transform.scale(0.25f)));
			}
		};
	}

	public static Effect caustic() {
		return new Effect() {
			@Override
			public Mode getMode() { return Mode.CAUSTIC; }

			@Override
			public float[] getTextureCoords(DisplayContext context) {
				return RectElement.defaultTexCoords(Corner.BL);
			}

			@Override
			public float[] getShaderParams(DisplayContext context) {
				return EmptyParams.instance().toFloats(4);
			}
		};
	}

	private final Effect effect;
	private final RenderState required;
	private final int[] colors;
	private Element element;

	public ColorBackgroundDisplay(Coloring coloring, Effect effect) {
		this.effect = effect;
		required = new RenderState(effect.getMode());
		colors = coloring.asQuadInts();
	}

	@Override
	public void init(RenderContext context) {
		ArtPlane plane = context.getDisplay().getBackPlane();
		DisplayContext display = context.getDisplay();
		element = new RectElement(plane.rect, -DashiCamera.BACK_DEPTH) {

			private final float[] params;
			private final float[] texCoords;

			{
				params = effect.getShaderParams(display);
				texCoords = effect.getTextureCoords(display);
			}
			@Override
			RenderState getRequiredState() {
				return required;
			}

			@Override
			RenderPhase getRenderPhase() {
				return RenderPhase.BACKGROUND;
			}

			@Override
			public void appendTo(ElementData data) {
				super.appendTo(data);
				data.colors.put(colors);
				data.texCoords.put(texCoords);
				data.shaders.put(params);
			}
		};
	}

	@Override
	public void destroy(GL2ES2 gl) {
		element = null;
	}

	@Override
	public Collection<Element> getElements() {
		return Collections.singleton(element);
	}
}
