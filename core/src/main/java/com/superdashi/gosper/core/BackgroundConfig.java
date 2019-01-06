package com.superdashi.gosper.core;

import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.Config.DurationConfig;

public class BackgroundConfig implements ConfigTarget {

	public static final long TRANS_DURATION_DEFAULT = 250L;
	public static final long SHOW_DURATION_DEFAULT = 5000L;

	public long transDuration = TRANS_DURATION_DEFAULT;
	public long showDuration = SHOW_DURATION_DEFAULT;

	public void adopt(BackgroundConfig that) {
		this.transDuration = that.transDuration;
		this.showDuration = that.showDuration;
	}

	// style target

	@Override
	public void applyStyling(ConfigProperty property, DurationConfig duration) {
		switch (property.name) {
		case "transition-duration" :
			transDuration = duration.asMillis();
			break;
		case "display-duration" :
			showDuration = duration.asMillis();
			break;
		}
	}
}
