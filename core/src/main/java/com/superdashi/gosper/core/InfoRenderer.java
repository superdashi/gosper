package com.superdashi.gosper.core;

import java.awt.Graphics2D;

import com.superdashi.gosper.item.Info;
import com.tomgibara.intgeom.IntRect;

public interface InfoRenderer extends Component {

	void renderInfo(Info info, Graphics2D g, IntRect rect, Cache cache);

}
