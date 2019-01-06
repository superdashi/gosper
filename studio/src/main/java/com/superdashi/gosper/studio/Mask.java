package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

public interface Mask extends Frame {

	//TODO introduce a Masks class?

	public static Mask overImage(BufferedImage image) {
		if (image == null) throw new IllegalArgumentException("null image");
		if (!ImageMask.isValidType(image.getType())) throw new IllegalArgumentException("invalid image type");
		return new ImageMask(image);
	}

	public static Mask overByteGray(IntDimensions dimensions, byte[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 8);
		return new ImageMask(ImageUtil.imageOverByteGray(dimensions, data));
	}

	public static Mask create(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new ImageMask(dimensions);
	}

	public static Mask empty(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new EmptyMask(dimensions);
	}

	public static Mask entire(IntDimensions dimensions) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new EntireMask(dimensions);
	}

	Mask view(IntRect bounds);

}
