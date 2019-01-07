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
package com.superdashi.gosper.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.tomgibara.fundament.Bijection;

// extents abstract map for object method implementations
public class AdaptedKeyMap<K,H,V> extends AbstractMap<K, V> {

	private final Map<H,V> map;
	private final Bijection<H, K> adapter;

	public AdaptedKeyMap(Map<H,V> map, Bijection<H, K> adapter) {
		if (map == null) throw new IllegalArgumentException("null map");
		if (adapter == null) throw new IllegalArgumentException("null adapter");
		this.map = map;
		this.adapter = adapter;
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		Optional<H> h = toH(key);
		return h.isPresent() && map.containsKey(h.get());
	}

	@Override
	public boolean containsValue(Object value) {
		return map.containsValue(value);
	}

	@Override
	public V get(Object key) {
		return toH(key).map(h -> map.get(h)).orElse(null);
	}

	@Override
	public V put(K key, V value) {
		return map.put( toH(key).orElseThrow(IllegalArgumentException::new), value );
	}

	@Override
	public V remove(Object key) {
		return toH(key).map(h -> map.remove(h)).orElse(null);
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		m.forEach( (k,v) -> put(k,v) );
	}

	@Override
	public void clear() {
		map.clear();
	}

	@Override
	public Set<K> keySet() {
		return new AdaptedSet<>(map.keySet(), adapter);
	}

	@Override
	public Collection<V> values() {
		return map.values();
	}

	@Override
	public Set<Map.Entry<K, V>> entrySet() {
		Bijection<Map.Entry<H, V>, Map.Entry<K, V>> entryAdapter = (Bijection) Bijection.fromFunctions((Class)Map.Entry.class, (Class)AdaptedEntry.class, e -> new AdaptedEntry((Map.Entry)e, adapter), e -> ((AdaptedEntry)e).entry);
		return new AdaptedSet<>(map.entrySet(), entryAdapter);
	}

	private Optional<K> toK(Object key) {
		return adapter.isInDomain(key) ? Optional.of(adapter.apply((H)key)) : Optional.empty();
	}

	private Optional<H> toH(Object key) {
		return adapter.isInRange(key) ? Optional.of(adapter.disapply((K)key)) : Optional.empty();
	}

	private static class AdaptedEntry<K,H,V> implements Map.Entry<K,V> {

		private final Map.Entry<H, V> entry;
		private final Bijection<H, K> adapter;

		AdaptedEntry(Map.Entry<H, V> entry, Bijection<H, K> adapter) {
			this.entry = entry;
			this.adapter = adapter;
		}

		@Override
		public K getKey() {
			return adapter.apply(entry.getKey());
		}

		@Override
		public V getValue() {
			return entry.getValue();
		}

		@Override
		public V setValue(V value) {
			return entry.setValue(value);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.getKey()) ^ Objects.hashCode(this.getValue());
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == this) return true;
			if (!(obj instanceof Map.Entry)) return false;
			Map.Entry<?,?> that = (Map.Entry<?, ?>) obj;
			return
					Objects.equals(this.getKey(),   that.getKey()  ) &&
					Objects.equals(this.getValue(), that.getValue());
		}

		@Override
		public String toString() {
			return getKey() + "=" + getValue();
		}
	}
}
