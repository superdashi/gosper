package com.superdashi.gosper.studio;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.tomgibara.fundament.Mutability;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.streams.ReadStream;

public interface Surface extends Frame, Mutability<Surface> {

	//TODO move methods to new Surfaces class?

	public static Surface overIntRGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return ImageSurface.over(ImageUtil.imageOverIntRGB(dimensions, data));
	}

	public static Surface overIntARGB(IntDimensions dimensions, int[] data) {
		ImageUtil.checkSurfaceData(dimensions, data, 32);
		return ImageSurface.over(ImageUtil.imageOverIntARGB(dimensions, data));
	}

	public static Surface overImage(BufferedImage image) {
		return ImageSurface.over(image);
	}

	public static Surface create(IntDimensions dimensions, boolean opaque) {
		return ImageSurface.sized(dimensions, opaque);
	}

	public static Surface decode(ReadStream stream) throws IOException {
		if (stream == null) throw new IllegalArgumentException("null stream");
		BufferedImage image = ImageIO.read(stream.asInputStream());
		int type = image.getType();
		boolean opaque = !image.getColorModel().hasAlpha();
		int reqType = ImageSurface.imageType(opaque);
		if (type != reqType) image = ImageUtil.convertImage(image, reqType);
		return ImageSurface.over(image);
	}

	void writePixel(int x, int y, int argb);

	Surface view(IntRect bounds);

	Canvas createCanvas();

}
