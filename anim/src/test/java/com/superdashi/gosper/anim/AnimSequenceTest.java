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
