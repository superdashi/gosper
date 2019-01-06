package com.superdashi.gosper.display;

import java.awt.Graphics2D;

import com.superdashi.gosper.core.Resolution;
import com.superdashi.gosper.layout.Position;
import com.tomgibara.intgeom.IntRect;

public interface Drawable {

	static Resolution optimalResolution(Resolution maxRes, Drawable... drawables) {
		maxRes = maxRes.potUpTo();
		Resolution res = null;
		for (Drawable drawable : drawables) {
			Resolution r = drawable.getResolution();
			res = res == null ? r : res.accommodating(r);
		}
		return res == null ? maxRes : res.constrainedBy(maxRes).potDownTo();
	}

	void drawTo(Graphics2D g, IntRect rect, Position pos);

	int getNumberOfChannels();

	Resolution getResolution();

}
