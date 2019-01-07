/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
