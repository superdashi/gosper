package com.superdashi.gosper.studio;

public final class StudioPlan {

	public StudioPlan() {
	}

	public Studio createLocalStudio() {
		return new LocalStudio(this);
	}
}
