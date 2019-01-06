package com.superdashi.gosper.display;

import java.util.Collection;
import java.util.Collections;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Coloring.Corner;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.worker.WorkerResult;
import com.superdashi.gosper.display.ShaderParams.EmptyParams;
import com.superdashi.gosper.display.ShaderParams.FadeParams;
import com.superdashi.gosper.display.ShaderParams.TimeParams;
import com.superdashi.gosper.display.ShaderParams.TransParams;

public class PictureBackgroundDisplay implements ElementDisplay {

	private static final float ages = 60 * 60 * 24 * 1000;
	private static final float retryInitDelay = 5;

	public interface Controller {
		TextureData init();
		void loadFirstImage();
		void loadTransition();
	}

	private final Controller controller;
	private final Coloring coloring;
	private float transDuration = 0.25f;
	private float showDuration = 5f;

	// created during initialization
	private BackElement element = null;

	// created in response to control
	private TextureData data;
	private Texture texture;

	// assigned while rendering
	private TimeParams params;
	private float dueTime;

	public PictureBackgroundDisplay(Controller controller, Coloring coloring, float transDuration, float showDuration) {
		this.controller = controller;
		this.coloring = coloring.opaque();
		this.transDuration = transDuration;
		this.showDuration = showDuration;
	}

	@Override
	public void init(RenderContext context) {
		element = new BackElement(context.getDisplay().getBackPlane());
		tryInit(context);
	}

	@Override
	public Collection<Element> getElements() {
		return Collections.singleton(element);
	}

	@Override
	public void destroy(GL2ES2 gl) {
		if (texture != null) {
			texture.destroy(gl);
			texture = null;
		}
		element = null;
	}

	private boolean errored(WorkerResult<?, RenderContext> result, String action) {
		DashiLog.debug(action);
		if (!result.getException().isPresent()) return false;
		DashiLog.warn("exception before " + action, result.getException().get());
		return true;
	}

	private void init(WorkerResult<TextureData, RenderContext> result) {
		if (errored(result, "initializing background")) return;
		RenderContext context = result.getContext();
		// check if we have data
		if (!result.getResult().isPresent()) { // defer a re-attempt
			//TODO use exponential backoff?
			float now = context.getDisplay().getTimeNow();
			result.getContext().notBefore(now + retryInitDelay, this::tryInit);
		} else {
			// record the texture data
			data = result.getResult().get();
			// create a texture
			texture = new Texture(context.getGL(), data);
			// proceed to load first image
			context.getWorker().run(controller::loadFirstImage, this::applyFirstImage);
		}
	}

	private void tryInit(RenderContext context) {
		DashiLog.debug("requesting initialization of controller");
		context.getWorker().call(controller::init, this::init);
	}

	private void applyFirstImage(WorkerResult<Void, RenderContext> result) {
		if (errored(result, "applying first background image")) return;
		RenderContext context = result.getContext();
		DisplayContext display = context.getDisplay();
		// configure the fade-in
		element.required.setTexture(texture);
		element.required.setMode(Mode.FADE);
		float now = display.getTimeNow();
		params = FadeParams.creator.create();
		params.setRangeAndDurations(now, transDuration, 0f, now + ages);
		// update current state
		dueTime = now + transDuration; // earliest time to switch to trans is at the end of the fade-in
		// start loading next image
		context.getWorker().run(controller::loadTransition, this::deferTransition);
	}

	private void deferTransition(WorkerResult<Void, RenderContext> result) {
		DashiLog.debug("deferring background transition");
		result.getContext().notBefore(dueTime, this::applyTransition);
	}

	private void applyTransition(RenderContext context) {
		DashiLog.debug("applying first background transition");
		DisplayContext display = context.getDisplay();
		// update the texture
		texture.updateImage(context.getGL(), data);
		// configure the transition
		float now = display.getTimeNow();
		if (!(params instanceof TransParams)) { // must be first transition
			element.required.setMode(Mode.TRANS);
			dueTime += (showDuration - 2 * transDuration); //putative time at which next trans occurs
			if (dueTime < now) dueTime = now;
			dueTime += transDuration; // now the time at which the switch to the subsequent trans pair should occur
			params = TransParams.creator.create();
			params.setRangeAndDurations(0f, 0f, transDuration, dueTime);
		} else {
			dueTime = now + showDuration;
			params.setRangeAndDurations(now, transDuration, transDuration, dueTime);
		}
		//schedule next switch
		context.getWorker().run(controller::loadTransition, this::deferTransition);
	}

	private class BackElement extends RectElement {

		private final RenderState required;

		private BackElement(ArtPlane plane) {
			super(plane.rect, -DashiCamera.BACK_DEPTH);
			required = new RenderState();
			required.setMode(Mode.PLAIN);
		}

		@Override
		RenderPhase getRenderPhase() {
			return RenderPhase.BACKGROUND;
		}

		@Override
		RenderState getRequiredState() {
			return required;
		}

		@Override
		public void appendTo(ElementData data) {
			super.appendTo(data);
			data.colors.put(coloring.asQuadInts());
			data.texCoords.put(RectElement.defaultTexCoords(Corner.BL));
			(params == null ? EmptyParams.instance() : params).writeTo(data.shaders, getVertexCount());
		}

	}

}
