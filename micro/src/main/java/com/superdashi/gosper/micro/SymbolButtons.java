package com.superdashi.gosper.micro;

import java.util.Optional;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Surface;
import com.superdashi.gosper.studio.TilingPlane;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntMargins;
import com.tomgibara.intgeom.IntRect;

//TODO must render disabled state
public class SymbolButtons extends ActionsComponent {

	private static final IntDimensions TWO_X_TWO = IntDimensions.of(2, 2);
	private static final TilingPlane tiling;
	static {
		Surface tile = Surface.create(TWO_X_TWO, true);
		tile.writePixel(0, 0, Argb.WHITE);
		tile.writePixel(1, 0, Argb.BLACK);
		tile.writePixel(0, 1, Argb.BLACK);
		tile.writePixel(1, 1, Argb.WHITE);
		tiling = new TilingPlane(tile);
	}

	private final Eventing eventing = verticalEventing();

	// these are derived from the visual context
	private IntRect symbolRect;
	private int symbolStep;

	// component methods

	@Override
	IntDimensions minimumSize() {
		return IntDimensions.horizontal(recommendedWidth());
	}

	@Override
	void place(Place place, int z) {
		super.place(place, z);
		symbolRect = IntRect.bounded(0, 1, bounds.width() - 1, 3 + situation.visualSpec().metrics.symbolSize);
		symbolStep = computeSymbolStep();
	}

	@Override
	public Composition composition() {
		return Composition.FILL;
	}

	@Override
	void render() {
		//TODO degeneracy means content is never filled
		if (symbolRect.isDegenerate()) return; // not sufficient space
		Canvas canvas = situation.defaultPane().canvas();
		RenderData data = new RenderData();

		VisualSpec spec = situation.visualSpec();

		// clear any lost icon slots
		boolean allChanged = situation.dirty() || data.displayCount < data.oldDisplayCount;
		if (allChanged) {
			canvas.color(Argb.WHITE);
			IntRect rect = symbolRect.translatedBy(0, data.displayCount * symbolStep);
			for (int i = data.displayCount; i < data.oldDisplayCount; i++) {
				canvas.intOps().fillRect(rect);
				rect = rect.translatedBy(0, symbolStep);
			}
		}

		boolean focused = situation.isFocused();
		IntMargins border = IntMargins.uniform(1);
		data.render(() -> {
			if (allChanged || data.redraw) {
				IntRect rect = symbolRect.translatedBy(0, data.index * symbolStep);
				IntRect inner = rect.minus(border);
				if (!data.active) { // WHITE
					canvas.color(Argb.WHITE);
				} else if (focused) { // BLACK
					canvas.color(Argb.BLACK);
				} else { // CHEQUERED
					canvas.shader(tiling.asShader());
				}
				canvas.intOps().fillRect(rect);
				if (data.active) {
					// we have a dark background - trim off the corners
					canvas.color(Argb.WHITE).intOps()
						.plotPixel(rect.minX    , rect.minY    )
						.plotPixel(rect.minX    , rect.maxY - 1)
						.plotPixel(rect.maxX - 1, rect.minY    )
						.plotPixel(rect.maxX - 1, rect.maxY - 1);
				}
				data.action.itemModel().icon(data.enabled || !focused).map(i -> !focused && data.enabled ? i : i).ifPresent(i -> canvas.color(data.active ? Argb.WHITE : Argb.BLACK).intOps().fillFrame(i, inner.minimumCoords()));
			}
		});
	}

	@Override
	public Optional<Eventing> eventing() {
		return Optional.of(eventing);
	}

	// super class abstracts

	@Override
	int computeMaxActionCount() {
		return (bounds.height() + situation.visualSpec().metrics.buttonGap) / computeSymbolStep();
	}

	@Override
	IntRect[] areas(int displayCount) {
		IntRect[] areas = new IntRect[displayCount];
		int x = bounds.minX;
		int y = bounds.minY;
		for (int i = 0; i < areas.length; i++) {
			areas[i] = symbolRect.translatedBy(x, y);
			y += symbolStep;
		}
		return areas;
	}

	@Override
	IntRect area(int index) {
		return symbolRect.translatedBy(bounds.minX, bounds.minY + index * symbolStep);
	}

	// private helper methods

	private int computeSymbolStep() {
		return situation.visualSpec().metrics.symbolSize + 2;
	}

	private int recommendedWidth() {
		return model().count() == 0 ? 0 : situation.visualSpec().metrics.symbolSize + 3;
	}


}
