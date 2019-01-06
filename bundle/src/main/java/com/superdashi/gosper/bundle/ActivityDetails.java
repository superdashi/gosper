package com.superdashi.gosper.bundle;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Flavor;

//TODO support object methods?
public final class ActivityDetails {

	public final Details details;
	public final Flavor flavor;
	public final boolean launch;

	ActivityDetails(Details details, Flavor flavor, boolean launch) {
		this.details = details;
		this.flavor = flavor;
		this.launch = launch;
	}

}
