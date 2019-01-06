package com.superdashi.gosper.awtd;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.device.Screen;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.studio.Composition;
import com.superdashi.gosper.studio.Target;
import com.tomgibara.intgeom.IntCoords;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntVector;

//TODO should support invert
public class AWTScreen extends JComponent implements Screen {

	private static final float MIN_CONTRAST = 0.1f;
	private static final float MAX_CONTRAST = 1.0f;

	private static final float MIN_BRIGHTNESS = 0.1f;
	private static final float MAX_BRIGHTNESS = 1.0f;

	private static final Color grey(float contrast) {
		int luminance = Math.round(255 * contrast);
		return new Color(0x00010101 * luminance);
	}

	private final Object lock = new Object();

	private final ScreenClass screenClass;
	private final ScreenColor screenColor;

	private final IntDimensions screenDimensions;
	private final int screenScale;
	private final BufferedImage background;

	private final int imageWidth;
	private final int imageHeight;
	private final IntVector offset;
	private final IntDimensions compDimensions;

	private boolean inverted = false;
	private float contrast = 1f;
	private float brightness = 1f;
	private Color ambience;
	private Color white = grey(contrast);

	private boolean begun = false;
	private BufferedImage surfaceT;
	private Target target;
	private BufferedImage surfaceA;
	private BufferedImage surfaceB;
	private Graphics2D graphicsA;
	private Graphics2D graphicsB;
	private boolean showingA = true;

	public AWTScreen(ScreenClass screenClass, ScreenColor screenColor, IntDimensions screenDimensions, int screenScale, IntMargins border, int borderColor, BufferedImage background) {
		if (screenClass == null) throw new IllegalArgumentException("null screenClass");
		if (screenColor == null) throw new IllegalArgumentException("null screenColor");
		if (screenDimensions == null) throw new IllegalArgumentException("null screenDimensions");
		if (screenDimensions.isDegenerate()) throw new IllegalArgumentException("degenerate screenDimensions");
		if (border == null) throw new IllegalArgumentException("null border");

		this.screenClass = screenClass;
		this.screenColor = screenColor;

		this.screenDimensions = screenDimensions;
		this.screenScale = screenScale;
		this.ambience = new Color(borderColor);
		this.background = background;
		offset = border.offset();

		surfaceT = new BufferedImage(screenDimensions.width, screenDimensions.height, background == null ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);
		target = Target.toImage(surfaceT);

		IntDimensions imageDimensions = screenDimensions.scale(screenScale);
		imageWidth = imageDimensions.width;
		imageHeight = imageDimensions.height;

		compDimensions = imageDimensions.plus(border);
		Dimension size = new Dimension(compDimensions.width, compDimensions.height);
		setSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setPreferredSize(size);
	}

	// package scoped methods

	ScreenClass screenClass() {
		return screenClass;
	}

	ScreenColor screenColor() {
		return screenColor;
	}

	// awt methods

	@Override
	protected void paintComponent(Graphics g) {
		g.setColor(ambience);
		g.fillRect(0, 0, compDimensions.width, compDimensions.height);
		synchronized (lock) {
			g.drawImage(showingA ? surfaceA : surfaceB, offset.x, offset.y, null);
		}
	}

	// screen methods

	@Override
	public IntDimensions dimensions() {
		return screenDimensions;
	}

	@Override
	public boolean opaque() {
		return background == null;
	}

	@Override
	public boolean inverted() {
		return inverted;
	}

	@Override
	public void inverted(boolean inverted) {
		if (inverted != this.inverted) {
			this.inverted = inverted;
			repaint();
		}
	}

	@Override
	public float contrast() {
		return contrast;
	}

	@Override
	public void contrast(float contrast) {
		if (contrast != this.contrast) {
			this.contrast = Math.min(Math.max(contrast, MIN_CONTRAST), MAX_CONTRAST);
			white = grey(this.contrast);
			repaint();
		}
	}

	@Override
	public float brightness() {
		return brightness;
	}

	@Override
	public void brightness(float brightness) {
		if (brightness != this.brightness) {
			this.brightness = Math.min(Math.max(brightness, MIN_BRIGHTNESS), MAX_BRIGHTNESS);
			repaint();
		}
	}

	@Override
	public int ambience() {
		return ambience.getRGB();
	}

	@Override
	public void ambience(int color) {
		ambience = new Color(Argb.premultiply(color));
	}

	@Override
	public void begin() {
		if (begun) return; // already begun

		GraphicsConfiguration config = getGraphicsConfiguration();
		int transparency = opaque() ? Transparency.OPAQUE : Transparency.TRANSLUCENT;
		surfaceA = config.createCompatibleImage(imageWidth, imageHeight, transparency);
		surfaceB = config.createCompatibleImage(imageWidth, imageHeight, transparency);
		graphicsA = surfaceA.createGraphics();
		graphicsB = surfaceB.createGraphics();
	}

	@Override
	public void end() {
		if (!begun) return; // not begun

		graphicsA.dispose();
		graphicsB.dispose();
		surfaceA = null;
		surfaceB = null;
		graphicsA = null;
		graphicsB = null;
	}

	@Override
	public void reset() {
		// no op?
	}

	@Override
	public void clear() {
		synchronized (lock) {
			Graphics2D g = graphics();
			g.setColor(black());
			g.fillRect(0, 0, imageWidth, imageHeight);
		}
	}

	@Override
	public void composite(Composition composition) {
		synchronized (lock) {
			composition.compositeTo(target);
			Graphics2D g = graphics();
			g.setPaintMode();
			g.drawImage(surfaceT, 0, 0, imageWidth, imageHeight, null);
			if (contrast < 1f) {
				g.setColor(new Color(0x80,0x80,0x80, 255 - (int) (contrast * 255)));
				g.fillRect(0, 0, imageWidth, imageHeight);
			}
			if (brightness < 1f) {
				g.setColor(new Color(0,0,0, 255 - (int) (brightness * 255)));
				g.fillRect(0, 0, imageWidth, imageHeight);
			}
		}
	}

	@Override
	public void blank() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				synchronized (lock) {
					Graphics2D g = graphics();
					g.setColor(Color.BLACK);
					g.fillRect(0, 0, imageWidth, imageHeight);
					showingA = !showingA;
					repaint();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void update() {
		try {
			SwingUtilities.invokeAndWait(() -> {
				synchronized (lock) {
					showingA = !showingA;
					repaint();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	// package scoped methods

	IntCoords translate(IntCoords coords) {
		return coords.translatedByNegative(offset).scaledDownBy(screenScale);
	}

	// private utility methods

	private Graphics2D graphics() {
		return showingA ? graphicsB : graphicsA;
	}

	private Color white() {
		return inverted ? Color.BLACK : white;
	}

	private Color black() {
		return inverted ? white : Color.BLACK;
	}
}
