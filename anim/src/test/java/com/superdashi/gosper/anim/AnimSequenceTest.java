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
package com.superdashi.gosper.anim;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.anim.AnimEffects;
import com.superdashi.gosper.anim.AnimSequence;
import com.superdashi.gosper.anim.AnimState;
import com.superdashi.gosper.anim.Animator;
import com.superdashi.gosper.anim.Animator.Terminal;


public class AnimSequenceTest {

	@Test
	public void testBasic() {
		TestSequence seq = new TestSequence();
		Animator animator = AnimEffects.translate(1, 0, 0, 4, 0, 0).interpolate().animator(10L, 20L, Terminal.NONE, Terminal.BOUNCE);
		seq.addAnimator(animator);
		seq.applyAtTime(15L);
		Assert.assertEquals(2.5f, seq.x, 0.0001f);
	}

	private class TestSequence extends AnimSequence {

		float x;
		float y;
		float z;

		@Override
		protected void apply(AnimState state) {
			float[] mat4 = new float[16];
			float[] color = new float[16];
			state.populate(mat4, color);
			x = mat4[12];
			y = mat4[13];
			z = mat4[14];
		}

		@Override
		public String toString() {
			return String.format("x: %3.3f, y: %3.3f, z: %3.3f", x, y, z);
		}
	}
}
