package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;

import com.tomgibara.fundament.Producer;

public interface Gallery {

	Frame attachImage(String resourceId, BufferedImage image);

	Frame attachImageIfAbsent(String resourceId, Producer<BufferedImage> producer);

	boolean detach(String resourceId);

}
