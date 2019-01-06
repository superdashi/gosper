package com.superdashi.gosper.display;

import java.util.Collection;
import java.util.Collections;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.display.ShaderParams.ConsoleParams;
import com.superdashi.gosper.display.Wrap.Wraps;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;

public class ConsoleDisplay implements ElementDisplay {

	private final Console console;
	private final Rect rect;
	private final float z;
	private final Position position;
	private final Wraps wraps;
	private final Coloring background;
	private final Palette palette;
	private RenderPalette renderPalette;
	private Texture texture;
	private TextureData data;
	private ConsoleElement el;

	// palette may be null to use default palette
	// position is position of console in rect
	public ConsoleDisplay(Console console, Rect rect, float z, Position position, Wraps wraps, Palette palette, Coloring background) {
		this.console = console;
		this.rect = rect;
		this.z = z;
		this.position = position;
		this.wraps = wraps;
		this.palette = palette;
		this.background = background;
	}

	@Override
	public void init(RenderContext context) {
		Palette palette = this.palette == null ? context.getDisplay().getDefaultPalette() : this.palette;
		renderPalette = context.getDisplay().createPalette(palette);

		GL2ES2 gl = context.getGL();
		data = console.asTextureData(gl.getGLProfile());
		createTexture(gl);
		el = new ConsoleElement();
	}

	@Override
	public void update(RenderContext context) {
		console.dirty().ifPresent(rect -> {
			GL2ES2 gl = context.getGL();
			gl.glActiveTexture(GL.GL_TEXTURE1);
			texture.bind(gl);
			//TODO reinstate when we have working sub image updates
			//Resolution r = rect.resolution();
			//texture.updateSubImage(gl, data, 0, rect.minX, rect.minY, rect.minX, rect.minY, r.h, r.v);
			// do this dumb thing instead
			texture.updateSubImage(gl, data, 0, 0, 0);
			console.clearDirt();
		});
	}

	@Override
	public Collection<Element> getElements() {
		return Collections.singleton(el);
	}

	@Override
	public void destroy(GL2ES2 gl) {
		renderPalette.destroy();
		renderPalette = null;
		texture.destroy(gl);
	}

	private void createTexture(GL2ES2 gl) {
		texture = new Texture(gl, data);
		texture.enable(gl);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		//TODO want a way to specify border
		texture.setTexParameterfv(gl, GL2ES2.GL_TEXTURE_BORDER_COLOR, new float[] {0f, 0f, 0f, 0f}, 0);
		texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, wraps.horizontal.glConstant(true));
		texture.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, wraps.vertical.glConstant(false));
	}

	private class ConsoleElement extends RectElement {

		private final RenderState required = new RenderState(Mode.CONSOLE);
		private final int[] colors;
		private final float[] texCoords;
		private final float[] params;

		private ConsoleElement() {
			super(rect, z);
			required.setData(texture);
			required.setPalette(renderPalette);
			required.setCharMap(console.charMap());
			colors = background.asQuadInts();
			params = ConsoleParams.instance(console.cols, console.rows).toFloats(4);

			// compute the tex coordinates
			Resolution res = console.charMap().getCharResolution();
			Rect src = Rect.atOrigin(console.cols * res.h, console.rows * res.v);
			Rect trg = position.transform(src, rect).transform(src);
			Rect txr = Transform.translateAndScale(trg, Rect.UNIT_SQUARE).transform(rect);
			//TODO want a convenience method for this?
			texCoords = new float[] {
					txr.minX, txr.minY,
					txr.maxX, txr.minY,
					txr.maxX, txr.maxY,
					txr.minX, txr.maxY,
			};
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
		public void appendTo(ElementData data) {
			super.appendTo(data);
			data.colors.put(colors);
			data.texCoords.put(texCoords);
			//data.texCoords.put(tmpCoords);
			data.shaders.put(params);
		}
	}
}
