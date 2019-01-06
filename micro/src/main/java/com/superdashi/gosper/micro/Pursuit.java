package com.superdashi.gosper.micro;

public class Pursuit extends ItemComponent {

	// item component methods

	@Override
	ItemModel itemModel() {
		return situation.selectedAction().itemModel();
	}

}
