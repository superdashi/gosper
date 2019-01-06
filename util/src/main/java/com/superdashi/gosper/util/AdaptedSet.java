package com.superdashi.gosper.util;

import java.lang.reflect.Array;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import com.tomgibara.fundament.Bijection;

public class AdaptedSet<T, R> extends AbstractSet<T> {

	private final Set<R> set;
	private final Bijection<R, T> adapter;

	public AdaptedSet(Set<R> set, Bijection<R, T> adapter) {
		if (set == null) throw new IllegalArgumentException("null set");
		if (adapter == null) throw new IllegalArgumentException("null adapter");
		this.set = set;
		this.adapter = adapter;
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public boolean contains(Object obj) {
		Optional<R> r = toR(obj);
		return r.isPresent() && set.contains(r.get());
	}

	@Override
	public Iterator<T> iterator() {
		return Iterators.map(set.iterator(), r -> adapter.apply(r));
	}

	@Override
	public Object[] toArray() {
		Object[] array = set.toArray();
		for (int i = 0; i < array.length; i++) {
			array[i] = adapter.apply((R)array[i]);
		}
		return array;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		int size = size();
		T[] array = a.length != size ? a : (T[]) Array.newInstance(a.getClass().getComponentType(), size);
		Iterator<R> it = set.iterator();
		for (int i = 0; i < array.length; i++) {
			array[i] = (T) adapter.apply(it.next());
		}
		return array;
	}

	@Override
	public boolean add(T obj) {
		return set.add( toR(obj).orElseThrow(IllegalArgumentException::new) );
	}

	@Override
	public boolean remove(Object obj) {
		return set.remove( toR(obj).orElseThrow(IllegalArgumentException::new) );
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object obj : c) {
			if (!contains(obj)) return false;
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		return c.stream().mapToInt(e -> add(e) ? 1 : 0).sum() > 0;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean modified = false;
		for (Iterator<T> it = iterator(); it.hasNext();) {
			if (!c.contains(adapter.disapply(it.next()))) {
				it.remove();
				modified = true;
			}
		}
		return modified;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return c.stream().mapToInt(e -> remove(e) ? 1 : 0).sum() > 0;
	}

	@Override
	public void clear() {
		set.clear();
	}

	private Optional<T> toT(Object obj) {
		return adapter.isInDomain(obj) ? Optional.of(adapter.apply((R)obj)) : Optional.empty();
	}

	private Optional<R> toR(Object obj) {
		return adapter.isInRange(obj) ? Optional.of(adapter.disapply((T)obj)) : Optional.empty();
	}

}
