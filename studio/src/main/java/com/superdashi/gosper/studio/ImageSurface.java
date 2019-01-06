package com.superdashi.gosper.studio;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;

class ImageSurface implements Surface {

	// statics

	private final boolean mutable;
	private final IntDimensions dimensions;
	final BufferedImage image;

	static int imageType(boolean opaque) {
		return opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
	}

	static boolean isValidType(int type) {
		return type == BufferedImage.TYPE_INT_RGB || type == BufferedImage.TYPE_INT_ARGB;
	}

	static BufferedImage frameToImage(Frame frame) {
		return new ImageSurface(frame).image;
	}

	//TODO checks here should be moved into public facing methods
	static ImageSurface sized(IntDimensions dimensions, boolean opaque) {
		if (dimensions == null) throw new IllegalArgumentException("null dimensions");
		if (dimensions.isDegenerate()) throw new IllegalArgumentException("degenerate dimensions");
		return new ImageSurface(dimensions, opaque);
	}

	//TODO checks here should be moved into public facing methods
	static ImageSurface over(BufferedImage image) {
		if (image == null) throw new IllegalArgumentException("null image");
		if (!ImageSurface.isValidType(image.getType())) throw new IllegalArgumentException("invalid image type");
		return new ImageSurface(image, true);
	}

	// may not be expected image type - used only for compositing
	static ImageSurface overTarget(BufferedImage image) {
		return new ImageSurface(image, true);
	}

	// constructors

	// create a new mutable image surface
	private ImageSurface(IntDimensions dimensions, boolean opaque) {
		mutable = true;
		image = new BufferedImage(dimensions.width, dimensions.height, imageType(opaque));
		this.dimensions = dimensions;
	}

	//  wrap a buffered image
	private ImageSurface(BufferedImage image, boolean mutable) {
		this.mutable = mutable;
		this.image = image;
		dimensions = IntDimensions.of(image.getWidth(), image.getHeight());
	}

	// copy a frame
	ImageSurface(Frame frame) {
		mutable = true;
		dimensions = frame.dimensions();
		image = new BufferedImage(dimensions.width, dimensions.height, imageType(frame.opaque()));
		int width = dimensions.width;
		int height = dimensions.height;
		int[] scanline = new int[width];
		for (int y = 0; y < height; y++) {
			frame.readScanline(y, scanline);
			this.writeScanline(y, scanline);
		}
	}

	// to make a view or copy
	private ImageSurface(ImageSurface that, boolean mutable, boolean copy) {
		this.mutable = mutable;
		this.dimensions = that.dimensions;
		if (copy) {
			this.image = new BufferedImage(dimensions.width, dimensions.height, that.image.getType());
			that.image.copyData(image.getRaster());
		} else {
			this.image = that.image;
		}
	}

	// surface methods

	@Override
	public boolean opaque() {
		// note done this way to allow types other than expected two for compositing
		return image.getType() != BufferedImage.TYPE_INT_ARGB;
	}

	@Override
	public IntDimensions dimensions() {
		return dimensions;
	}

	// warning - slow!
	@Override
	public int readPixel(int x, int y) {
		return image.getRGB(x, y);
	}

	// warning - slow!
	@Override
	public void writePixel(int x, int y, int argb) {
		image.setRGB(x, y, argb);
	}

	@Override
	public void readScanline(int y, int[] scanline) {
		if (scanline == null) throw new IllegalArgumentException("null scanline");
		image.getRaster().getDataElements(0, y, dimensions.width, 1, scanline);
	}

	@Override
	public ImageSurface view(IntRect bounds) {
		if (bounds == null) throw new IllegalArgumentException("null bounds");
		if (bounds.isDegenerate()) throw new IllegalArgumentException("degenerate bounds");
		IntRect srcBounds = this.dimensions.toRect();
		if (!srcBounds.containsRect(bounds)) throw new IllegalArgumentException("invalid bounds");
		return srcBounds.equals(bounds) ? this : new ImageSurface(image.getSubimage(bounds.minX, bounds.minY, bounds.width(), bounds.height()), this.mutable);
	}

	@Override
	public LocalCanvas createCanvas() {
		return new LocalCanvas(this);
	}

	@Override
	//TODO a better approach should be possible here
	public Mask toMask() {
		return Surface.super.toMask();
	}

	// this is just a mutable copy for surfaces
	@Override
	public Surface materialize() {
		return mutableCopy();
	}

	@Override
	public BufferedImage toImage() {
		return immutableCopy().image;
	}

	// mutability methods

	@Override
	public boolean isMutable() {
		return mutable;
	}

	@Override
	public ImageSurface mutableCopy() {
		return new ImageSurface(this, true, true);
	}

	@Override
	public ImageSurface immutableCopy() {
		return new ImageSurface(this, false, true);
	}

	@Override
	public ImageSurface immutableView() {
		return new ImageSurface(this, false, false);
	}

	// package scoped methods

	Graphics2D createGraphics() {
		Graphics2D g = image.createGraphics();
		ImageUtil.configureGraphics(g);
		return g;
	}

	// private helper methods

	private void writeScanline(int y, int[] scanline) {
		image.getRaster().setDataElements(0, y, scanline.length, 1, scanline);
	}

}
