package com.superdashi.gosper.micro;

import java.util.Arrays;
import java.util.List;

public final class DisplayColumns {

	public final boolean navigation;
	public final boolean label;
	public final List<Badge> badges;

	public DisplayColumns(boolean navigation, boolean label, Badge... badges) {
		if (badges == null) throw new IllegalArgumentException("null badges");
		this.badges = Arrays.asList(badges.clone());
		if (this.badges.contains(null)) throw new IllegalArgumentException("null badge");
		this.navigation = navigation;
		this.label = label;
	}
}