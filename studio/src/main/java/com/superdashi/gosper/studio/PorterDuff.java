package com.superdashi.gosper.studio;

import java.awt.AlphaComposite;
import java.awt.Composite;

public class PorterDuff {

	private static float clamp(float alpha) {
		return Math.min(Math.max(alpha, 0f), 1f);
	}

	private static int constantFor(Rule rule) {
		switch (rule) {
		case CLEAR:    return AlphaComposite.CLEAR;
		case DST:      return AlphaComposite.DST;
		case DST_ATOP: return AlphaComposite.DST_ATOP;
		case DST_IN:   return AlphaComposite.DST_IN;
		case DST_OUT:  return AlphaComposite.DST_OUT;
		case DST_OVER: return AlphaComposite.DST_OVER;
		case SRC:      return AlphaComposite.SRC;
		case SRC_ATOP: return AlphaComposite.SRC_ATOP;
		case SRC_IN:   return AlphaComposite.SRC_IN;
		case SRC_OUT:  return AlphaComposite.SRC_OUT;
		case SRC_OVER: return AlphaComposite.SRC_OVER;
		default:
			throw new IllegalStateException();
		}
	}

	public enum Rule {
		CLEAR,
		SRC,
		DST,
		SRC_OVER,
		DST_OVER,
		SRC_IN,
		DST_IN,
		SRC_OUT,
		DST_OUT,
		SRC_ATOP,
		DST_ATOP,
	}

	private final Rule rule;
	private final float alpha;

	public PorterDuff(Rule rule, float alpha) {
		if (rule == null) throw new IllegalArgumentException("null rule");
		this.rule = rule;
		this.alpha = clamp(alpha);
	}

	public PorterDuff(Rule rule) {
		if (rule == null) throw new IllegalArgumentException("null rule");
		this.rule = rule;
		alpha = 1f;
	}

	public Composer asComposer() {
		return new Composer() {
			@Override
			Composite createComposite() {
				int constant = constantFor(rule);
				return AlphaComposite.getInstance(constant, alpha);
			}
		};
	}
}
