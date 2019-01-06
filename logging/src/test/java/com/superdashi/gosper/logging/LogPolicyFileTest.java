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
