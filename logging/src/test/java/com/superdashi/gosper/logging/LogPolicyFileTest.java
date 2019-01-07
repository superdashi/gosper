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
package com.superdashi.gosper.logging;

import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

import com.superdashi.gosper.logging.LogIdentity;
import com.superdashi.gosper.logging.LogPolicyFile;
import com.tomgibara.tries.IndexedTrie;

public class LogPolicyFileTest {

	@Test
	public void testTrie() {
		IndexedTrie<LogIdentity> trie = LogPolicyFile.tries.newTrie();
		LogIdentity mammal = LogIdentity.create("animal", "mammal");
		LogIdentity dog = mammal.child("dog");
		trie.add(dog);
		Assert.assertTrue(trie.contains(dog));
		LogIdentity chihuahua = dog.child("chihuahua");
		Assert.assertEquals(dog, trie.ancestors(chihuahua).next());
		trie.add(chihuahua);
		Assert.assertTrue( trie.subTrie(dog).contains(chihuahua) );
		trie.add(mammal);
		Iterator<LogIdentity> ancestors = trie.ancestors(chihuahua);
		Assert.assertEquals(mammal, ancestors.next());
		Assert.assertEquals(dog, ancestors.next());
	}
}
