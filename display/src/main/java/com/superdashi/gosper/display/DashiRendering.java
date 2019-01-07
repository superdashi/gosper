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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import com.jogamp.nativewindow.awt.DirectDataBufferInt.BufferedImageInt;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureData;
import com.tomgibara.bits.Bits;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceSet;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette;
import com.superdashi.gosper.core.Bar;
import com.superdashi.gosper.core.Cache;
import com.superdashi.gosper.core.Clock;
import com.superdashi.gosper.core.ClockConfig;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Design;
import com.superdashi.gosper.core.DesignConfig;
import com.superdashi.gosper.core.Layout;
import com.superdashi.gosper.core.Panel;
import com.superdashi.gosper.core.Layout.Place;
import com.superdashi.gosper.core.worker.Worker;
import com.superdashi.gosper.display.ColorBackgroundDisplay.Effect;
import com.superdashi.gosper.display.ShaderParams.EmptyParams;
import com.superdashi.gosper.display.ShaderParams.PlasmaParams;
import com.superdashi.gosper.display.ShaderParams.PulseParams;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.model.Normal;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.fundament.Consumer;
import com.tomgibara.fundament.Producer;
import com.tomgibara.geom.core.Angles;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.storage.Store;

public class DashiRendering implements GLEventListener, DisplayConduit {

	private static final Sets<ElementDisplay> displaySets = Collect.setsOf(ElementDisplay.class).underIdentity();

	private static enum State {
		CREATED,
		INITIALIZED,
		SIZED,
		DESIGNED,
		PANELLING,
		PANELLED;
	}

	private static final boolean extraTextures = false;

	private final Cache cache;
	private final Worker<RenderContext> worker;
	private final Consumer<RenderContext> workerCollection;
	private final ElementRenderer elr = new ElementRenderer();
	private final LayoutAllocator layoutAllocator = new LayoutAllocator();
	private final EquivalenceSet<ElementDisplay> displays = displaySets.newSet();
	private final SortedMap<Float, Consumer<RenderContext>> deferrals = new TreeMap<>();

	// resources initialized as required
	private final CharMap gasconBold = GasconMap.loadFromResource("gascon-bold");
	private final RenderPalettes renderPalettes = new RenderPalettes();

	// resources created on initialization
	private ModeShaders mshdrs;
	private ElementData eld;
	private GLProfile glProfile;

	// resources assigned on reshape
	private DynamicAtlas<Place> panelAtlas = null;
	private DisplayFactory displayFactory = null;

	// display state
	private Design design = null;
	private PanelDisplay panelDisplay = null;

	// conduit state
	private DisplayListener listener = NullDisplayListener.instance();
	private boolean listenerUpdating = false;

	// strictly test only
	private DynamicAtlas<Void> tickerAtlas = null;
	//private DynamicAtlas<Void>.Updater updater = null;
	private Texture logoTex;
	private Texture testTex;

	private State state = State.CREATED;

	public DashiRendering(Worker<RenderContext> worker, Consumer<RenderContext> consumer, Cache cache) {
		this.worker = worker;
		this.workerCollection = consumer;
		this.cache = cache;
	}

	// GL Methods

	@Override
	public synchronized void init(GLAutoDrawable drawable) {
		GL2ES2 gl = drawable.getGL().getGL2ES2();
		glProfile = gl.getGLProfile();
		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_DEPTH_TEST);

		//TODO eliminate this
		if (extraTextures) {
			try (InputStream in = getClass().getClassLoader().getResourceAsStream("dashi-texture.png")) {
				// annoying workaround to load single channel image
				BufferedImage tmp = ImageIO.read(in);
				BufferedImage img = new BufferedImage(tmp.getWidth(), tmp.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
				Graphics2D g = img.createGraphics();
				g.drawImage(tmp, 0, 0, null);
				g.dispose();
				int format = gl.isGL3() ? GL2ES2.GL_RED : GL.GL_LUMINANCE;
				AWTTextureData data = new AWTTextureData(gl.getGLProfile(), format, 0, false, img);
				logoTex = TextureIO.newTexture(data);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-texture.png")) {
				testTex = TextureIO.newTexture(in, false, "PNG");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		//TODO could defer until used if loading was done on a separate thread?
		gasconBold.init(gl);
		renderPalettes.init(gl);

		tickerAtlas = new DynamicAtlas<>(gl, 2048, 128, true, LinearAllocator.newHorizontal());

		mshdrs = new ModeShaders(gl);

		eld = new ElementData(gl);
		state = State.INITIALIZED;
		worker.run(listener::displayStarted, null);
	}

	@Override
	public synchronized void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		GL2ES2 gl = drawable.getGL().getGL2ES2();
		Viewport viewport = Viewport.from(IntRect.rectangle(x, y, w, h));
		viewport.shape(gl);
		// don't do anything further if bounds already match
		if (displayFactory != null && displayFactory.context.getViewport().equals(viewport)) {
			DashiLog.debug("Reshaped to identical viewport: {0}", viewport);
			return;
		}

		DashiLog.debug("Reshaped to viewport: {0}", viewport);
		displayFactory = new DisplayFactory( new DisplayingContext(viewport, 60f) );

		//TODO how to cope with replacing atlas - other resources will still reference it
		if (panelAtlas != null) {
			panelAtlas.destroy(gl);
		}
		panelAtlas = new DynamicAtlas<>(gl, w, h, false, layoutAllocator);
		state = State.SIZED;
		worker.run(() -> listener.newContext(displayFactory.context), null);
	}

	@Override
	public synchronized void display(GLAutoDrawable drawable) {
		DashiLog.trace("display called");

		GL2ES2 gl = drawable.getGL().getGL2ES2();

		gl.glDisable(GL.GL_SCISSOR_TEST);
		gl.glColorMask(true, true, true, true);
		gl.glDepthMask(true);
		gl.glStencilMask(0xffffffff);
		gl.glClear ( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

		renderPalettes.bind(gl);

		// start of display work
		long start = System.currentTimeMillis();
		RenderingContext context = new RenderingContext(gl);

		// run all the handlers in response to the tasks the worker has completed
		workerCollection.consume(context); // do this early - likely to do texture updates

		// run all deferred tasks who's times have come
		Collection<Consumer<RenderContext>> values = deferrals.headMap(timeNow()).values();
		for (Consumer<RenderContext> task : values) {
			task.consume(context);
		}
		values.clear();

		// issue an update call to the controller
		// we do it after consuming the results,
		// because one of these might be the update completion
		if (!listenerUpdating && state.compareTo(State.SIZED) >= 0) {
			listenerUpdating = true;
			worker.run(listener::update, result -> {
				listenerUpdating = false;
				Optional<Exception> e = result.getException();
				if (e.isPresent()) {
					DashiLog.warn("exception during update", e.get());
				}
			});
		}

		// offer each display the opportunity to update itself
		for (ElementDisplay display : displays) {
			display.update(context);
		}

		while (true) { // keep attempting state changes while we can
			State previousState = state;
			switch (state) {
			case SIZED:
				//elr.clear();
				addDisplay(() -> new DigitsDisplay(2, 0), null);
				break;
			case DESIGNED:
				Bar bar = design.getBar();
				if (bar != null) addDisplay(() -> displayFactory.createDisplay(bar), null);
				Store<Panel> panels = design.getPanels();
				if (panels.size() > 0) {
					Panel panel = panels.get(0);
					state = State.PANELLING;
					worker.call(() -> displayFactory.createDisplay(panel), result -> setPanelDisplay(result.getContext(), result.getResult().get()));
					addDisplay(() -> displayFactory.createDisplay(new Clock(new ClockConfig())), null /*SampleContent.spinWithColor*/);
					Effect plain = ColorBackgroundDisplay.specular(0);
					Effect noise = ColorBackgroundDisplay.noise(Coloring.vertical(0xffff0000, 0xff0000ff));
					Effect caustic = ColorBackgroundDisplay.caustic();
					//addDisplay(() -> new ColorBackgroundDisplay(Coloring.vertical(0xff33ff00, 0xff009966), noise), null);
					//addDisplay(() -> new HexBackgroundDisplay(10), null);
					//Palette palette = SampleContent.palette.builder().lighten().build();
				}
				break;
			case PANELLED:
				if (panelDisplay.requiresInfo()) {
					//TODO need inspector!!!
					Inspector inspector = null;
					worker.run(() -> panelDisplay.acquireInfo(inspector), null);
				}
				if (panelDisplay.requiresRender()) {
					worker.run(() -> panelDisplay.renderUpdate(), (result) -> panelDisplay.clean(result.getContext().getGL()));
				}
				break;
			}
			if (previousState == state) break;
			previousState = state;
		}

		elr.prepare(eld);

		context.invalidate();
		long finish = System.currentTimeMillis();
		RenderStats.recordDisplayTime((int) (finish - start));
		// end of display work

		// Render elements
		elr.render(gl, mshdrs, eld);
	}

	@Override
	public synchronized void dispose(GLAutoDrawable drawable){
		GL2ES2 gl = drawable.getGL().getGL2ES2();
		for (ElementDisplay display : displays) {
			display.destroy(gl);
		}
		mshdrs.dispose(gl);
		eld.delete(gl);
		if (panelAtlas != null) panelAtlas.destroy(gl);
		if (logoTex != null) logoTex.destroy(gl);
		gasconBold.destroy(gl);
		renderPalettes.destroy(gl);
		worker.run(listener::displayStopped, null);
	}

	// Conduit methods

	@Override
	public void setDisplayListener(DisplayListener listener) {
		this.listener = listener == null ? NullDisplayListener.instance() : listener;
	}

	@Override
	public void setDesign(Design design) {
		if (design == null) throw new IllegalArgumentException("null design");
		worker.enqueue(design, result -> {
			Design d = result.getResult().get();
			this.design = d;
			DesignConfig style = design.style;
			mshdrs.setAmbientColor(style.ambientColor);
			//TODO need move core 3D abstractions to somewhere more basic - util?
			mshdrs.setLightDirection(new Normal(style.lightDirection));
			mshdrs.setLightColor(style.lightColor);
			state = State.DESIGNED;
		});
	}

	@Override
	public void addDisplay(ElementDisplay display) {
		if (display == null) throw new IllegalArgumentException("null display");
		worker.enqueue(display, result -> {
			ElementDisplay d = result.getResult().get();
			d.init(result.getContext());
			elr.addAll(d.getElements());
			displays.add(d);
		});
	}

	@Override
	public synchronized void sync(Runnable r) {
		r.run();
	}

	// private methods

	//TODO can this be usefully optimized?
	private float timeNow() {
		return mshdrs.toShaderTime(System.currentTimeMillis());
	}

	private void addDisplay(Producer<ElementDisplay> producer, ElementAnim anim) {
		worker.call(() -> producer.produce(), result -> {
			Optional<Exception> optE = result.getException();
			if (optE.isPresent()) {
				DashiLog.warn("exception producing element display", optE.get());
				return;
			}
			Optional<ElementDisplay> optR = result.getResult();
			if (optR.isPresent()) {
				ElementDisplay display = optR.get();
				display.init(result.getContext());
				if (anim != null) display.setAnimation(anim);
				elr.addAll(display.getElements());
				displays.add(display);
			}
		});
	}

	private void setPanelDisplay(RenderContext context, PanelDisplay panelDisplay) {
		if (this.panelDisplay != null) throw new IllegalStateException("panelDisplay already set");

		this.panelDisplay = panelDisplay;
		//elr.add(new BackgroundElement(camera, Coloring.corners(0xff0033ff, 0xff009900, 0xff000000, 0xff000000)));
		//elr.add(new BackgroundElement(camera, Coloring.flat(0x80000000)));
		//elr.add(new TestElement(camera, updater.getRect(), atlas.getTexture()));
		// just testing model element
		int opt = 4;
//		if (opt == 0) {
//			RenderState rsa = new RenderState();
//			rsa.setMode(Mode.PLAIN_LIT);
//			ModelElement ela = new ModelElement(SampleContent.model, Bits.toStore(2L, 2), RenderPhase.OVERLAY, rsa, EmptyParams.instance());
//			//ela.anim = SampleContent.spinWithColor;
//			ela.anim = SampleContent.wobble;
//			elr.add(ela);
//			RenderState rsb = new RenderState();
//			rsb.setMode(Mode.PLASMA);
//			PlasmaParams params = PlasmaParams.creator.create();
//			params.setScale(10f);
//			ModelElement elb = new ModelElement(SampleContent.model, Bits.toStore(1L, 2), RenderPhase.OVERLAY, rsb, params);
//			//elb.anim = SampleContent.spinWithColor;
//			elb.anim = SampleContent.wobble;
//			elr.add(elb);
//		}
//		if (opt == 1) {
//			RenderState rs = new RenderState();
//			rs.setMode(Mode.PLAIN_LIT);
//			ModelElement el = new ModelElement(SampleContent.sphere, RenderPhase.OVERLAY, rs, EmptyParams.instance());
//			el.anim = SampleContent.spinWithColor;
//			elr.add(el);
//		}
		if (opt == 2) {
			PlasmaParams params = PlasmaParams.creator.create();
			params.setScale(10f);
			HexElement el = new HexElement(Transform.identity(), Transform.scale(0.5f / Angles.COS_PI_BY_THREE), 0.25f, Mode.PULSE, 0xff00ffff, 0x0000ffff, PulseParams.instance(0f, 2f, 4f, 0.5f));
			//HexElement el = new HexElement(Transform.identity(), Transform.identity(), 0.25f, Mode.PLASMA, 0xffffffff, 0xffffffff, params);
			//el.anim = SampleContent.cycle;
			elr.add(el);
		}
		// panel
		elr.addAll(panelDisplay.getElements(context));
		panelAtlas.update(context.getGL());
		state = State.PANELLED;
	}

	private class DisplayingContext implements DisplayContext {

		final Viewport viewport;
		final DashiCamera camera;

		DisplayingContext(Viewport viewport, float fov) {
			this.viewport = viewport;
			camera = new DashiCamera(viewport.resolution.aspectRatio(), fov);

			// we can configure the camera once, because it's static
			PMVMatrix m = mshdrs.getMatrix();
			camera.configure(m);
		}

		@Override
		public Viewport getViewport() {
			return viewport;
		}

		@Override
		public Font getDefaultFont() {
			return DisplayUtil.defaultFont;
		}

		@Override
		public Palette getDefaultPalette() {
			return design == null ? null : design.style.palette;
		}

		@Override
		public CharMap getDefaultCharMap() {
			return gasconBold;
		}

		@Override
		public DashiCamera.Params getParams() {
			return camera.params;
		}

		@Override
		public ArtPlane getDashPlane() {
			return camera.dash;
		}

		@Override
		public ArtPlane getBackPlane() {
			return camera.back;
		}

		@Override
		public ArtPlane getOverPlane() {
			return camera.over;
		}

		@Override
		public DynamicAtlas<Place> getPlaceAtlas() {
			return panelAtlas;
		}

		@Override
		public Cache getCache() {
			return cache;
		}

		@Override
		public float getTimeNow() {
			return timeNow();
		}

		@Override
		public long millisBetweenTimes(float s, float t) {
			return mshdrs.millisBetweenTimes(s, t);
		}

		@Override
		public TextureData createTextureData(BufferedImageInt image) {
			int w = image.getWidth();
			int h = image.getHeight();
			if (w != Integer.highestOneBit(w)) throw new IllegalArgumentException("width not pot");
			if (h != Integer.highestOneBit(h)) throw new IllegalArgumentException("height not pot");
			// construct texture data from the image
			int internalFormat = GL.GL_RGBA;
			TextureData data = new TextureData(
					glProfile,
					internalFormat,
					w,
					h,
					0,
					new GLPixelAttributes(internalFormat, GL.GL_UNSIGNED_BYTE),
					false,
					false,
					true,
					image.getDataBuffer().getData(),
					null
					);
			// return the data
			return data;
		}

		@Override
		public RenderPalette createPalette(Palette palette) {
			if (palette == null) throw new IllegalArgumentException("null palette");
			return renderPalettes.get(palette);
		}

	}

	private class RenderingContext implements RenderContext {

		private final GL2ES2 gl;
		private boolean valid = true;

		RenderingContext(GL2ES2 gl) {
			this.gl = gl;
		}

		@Override
		public DisplayContext getDisplay() {
			checkValidity();
			return displayFactory.context;
		}

		@Override
		public GL2ES2 getGL() {
			checkValidity();
			return gl;
		}

		@Override
		public Worker<RenderContext> getWorker() {
			checkValidity();
			return worker;
		}

		@Override
		public void notBefore(float displayTime, Consumer<RenderContext> renderTask) {
			if (renderTask == null) throw new IllegalArgumentException("null renderTask");
			checkValidity();
			float now = timeNow();
			if (displayTime <= now) {
				renderTask.consume(this);
			} else {
				deferrals.put(displayTime, renderTask);
			}
		}

		void invalidate() {
			valid = false;
		}

		private void checkValidity() {
			if (!valid) throw new IllegalStateException("attempt to use invalid render context");
		}
	}

}
