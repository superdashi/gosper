package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.util.Map;

import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.fundament.Producer;

public class LocalGallery implements Gallery {

	private static final Maps<String, Frame> resourceMaps = Collect.setsOf(String.class).mappedTo(Frame.class);

	//TODO need a better place for this
	static void checkResourceId(String resourceId) {
		if (resourceId == null) throw new IllegalArgumentException("null resourceId");
		if (resourceId.isEmpty()) throw new IllegalArgumentException("empty resourceId");
	}

	final Studio studio;
	private final Map<String, Frame> resources = resourceMaps.newMap();

	LocalGallery(LocalStudio studio) {
		this.studio = studio;
	}

	@Override
	public Frame attachImage(String resourceId, BufferedImage image) {
		checkResourceId(resourceId);
		if (image == null) throw new IllegalArgumentException("null image");
		checkOpen();
		ImageSurface surface = ImageSurface.over(image);
		resources.put(resourceId, surface);
		return surface;
	}

	@Override
	public Frame attachImageIfAbsent(String resourceId, Producer<BufferedImage> producer) {
		checkResourceId(resourceId);
		if (producer == null) throw new IllegalArgumentException("null producer");
		checkOpen();
		Frame frame = resources.get(resourceId);
		if (frame == null) {
			BufferedImage image = producer.produce();
			frame = ImageSurface.over(image);
			resources.put(resourceId, frame);
		}
		return frame;
	}

	@Override
	public boolean detach(String resourceId) {
		checkResourceId(resourceId);
		checkOpen();
		Frame frame = resources.remove(resourceId);
		return frame != null;
	}

	void attach(String resourceId, Frame frame) {
		resources.put(resourceId, frame);
	}

	void close() {
		resources.clear();
	}

	private void checkOpen() {
		if (studio.isClosed()) throw new IllegalStateException("closed");
	}

}
