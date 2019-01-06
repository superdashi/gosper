package com.superdashi.gosper.studio;

import java.awt.Composite;
import java.awt.Graphics2D;

public abstract class Composer {

	private Composite composite;

	Composer() { }

	// this API required instead of more natural Composite toComposite()
	// because setXORColor hides the composite instance.
	void applyTo(Graphics2D g) {
		g.setComposite(composite == null ? composite = createComposite() : composite);
	}

	abstract Composite createComposite();
}
