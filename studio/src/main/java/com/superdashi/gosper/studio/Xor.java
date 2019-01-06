package com.superdashi.gosper.studio;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;

public class Xor {

	public Composer asComposer() {
		return new Composer() {
			@Override
			void applyTo(Graphics2D g) {
				g.setXORMode(Color.WHITE);
			}
			@Override
			Composite createComposite() {
				throw new UnsupportedOperationException();
				//return AlphaComposite.getInstance(AlphaComposite.XOR);
			}
		};
	}

}
