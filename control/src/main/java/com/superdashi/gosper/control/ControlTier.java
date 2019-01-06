package com.superdashi.gosper.control;

import com.superdashi.gosper.data.DataTier;
import com.superdashi.gosper.display.DisplayTier;

public class ControlTier {

	private final Controller controller;

	public ControlTier(DataTier dataTier, DisplayTier displayTier) {
		controller = new Controller(dataTier, displayTier.getConduit());
	}

}
