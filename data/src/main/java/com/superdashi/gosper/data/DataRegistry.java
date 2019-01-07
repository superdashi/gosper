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
package com.superdashi.gosper.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import com.superdashi.gosper.core.ComponentRegistry;
import com.superdashi.gosper.core.CoreContext;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.http.HttpReqRes;
import com.superdashi.gosper.http.HttpServer.ReqRes;
import com.superdashi.gosper.util.DashiUtil;
import com.superdashi.gosper.util.Schedule;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.Collect.Sets;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.collect.EquivalenceSet;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.StoreType;
import com.tomgibara.tries.Trie;
import com.tomgibara.tries.Tries;

public class DataRegistry implements ComponentRegistry<DataComponent> {

	private static final Storage<String> stringStorage = StoreType.of(String.class).storage();
	private static final Storage<Invoker> invokerStorage = StoreType.of(Invoker.class).storage();
	private static final Storage<Identity> identityStorage = StoreType.of(Identity.class).storage();
	private static final Storage<DataComponent> dataStorage = StoreType.of(DataComponent.class).storage();
	private static final Sets<DataComponent> dataSets = Collect.setsWithStorage(dataStorage);
	private static final Maps<Identity,DataComponent> dataMaps = Collect.setsWithStorage(identityStorage).mappedWithStorage(dataStorage);
	private static final Maps<DataComponent,Invoker> invokerMaps = dataSets.mappedWithStorage(invokerStorage);
	private static final Maps<String,Invoker> pathMaps = Collect.setsWithStorage(stringStorage).mappedWithStorage(invokerStorage);

	private static int pathPrefixLength(DataComponent component) {
		return 1 + component.details().identity().ns.toString().length();
	}

	private static String path(DataComponent component) {
		String path = component.path();
		return path == null ? null : "/" + component.details().identity().ns + path + (path.endsWith("/") ? "" : "/");
	}

	private final Object lock = new Object();
	private final DataTier dataTier;
	// (de)populated when invoker is (removed)added
	private final EquivalenceMap<DataComponent, Invoker> invokers = invokerMaps.newMap();
	private final EquivalenceMap<Identity, DataComponent> components = dataMaps.newMap();
	// populated during processing based on presence of path
	private final Trie<String> paths = Tries.serialStrings(DashiUtil.UTF8).nodeSource(Tries.sourceForCompactLookups()).newTrie();
	private final EquivalenceMap<String, Invoker> invokersByPath = pathMaps.newMap();

	DataRegistry(DataTier dataTier) {
		this.dataTier = dataTier;
	}

	public boolean addComponent(DataComponent component) {
		if (component == null) throw new IllegalArgumentException("null component");
		synchronized (lock) {
			return add(component);
		}
	}

	public boolean addComponents(Collection<DataComponent> components) {
		if (components == null) throw new IllegalArgumentException("null components");
		if (components.contains(null)) throw new IllegalArgumentException("null component");
		boolean modified = false;
		synchronized (lock) {
			for (DataComponent component : components) {
				modified = add(component) || modified;
			}
		}
		return modified;
	}

	public boolean removeComponent(DataComponent component) {
		if (component == null) throw new IllegalArgumentException("null component");
		synchronized (lock) {
			return remove(component);
		}
	}

	public boolean removeComponents(Collection<DataComponent> components) {
		if (components == null) throw new IllegalArgumentException("null components");
		if (components.contains(null)) throw new IllegalArgumentException("null component");
		boolean modified = false;
		synchronized (lock) {
			for (DataComponent component : components) {
				modified = remove(component) || modified;
			}
		}
		return modified;
	}

	public Optional<DataComponent> removeComponentWithDetails(Details details) {
		if (details == null) throw new IllegalArgumentException("null details");
		synchronized (lock) {
			return Optional.ofNullable( remove(details) );
		}
	}

	public Collection<DataComponent> removeComponentsWithDetails(Collection<Details> details) {
		if (details == null) throw new IllegalArgumentException("null details");
		if (details.contains(null)) throw new IllegalArgumentException("null details element");
		List<DataComponent> list = Collections.emptyList();
		synchronized (lock) {
			for (Details d : details) {
				DataComponent c = remove(d);
				if (c != null) {
					if (list.isEmpty()) list = new ArrayList<>();
					list.add(c);
				}
			}
		}
		return list;
	}

	public Collection<DataComponent> removeAllComponents() {
		synchronized (lock) {
			if (invokers.isEmpty()) return dataSets.emptySet();
			invokers.values().forEach(i -> unprocess(i));
			EquivalenceSet<DataComponent> removed = invokers.keySet().immutableCopy();
			invokers.clear();
			return removed;
		}
	}

	@Override
	public Optional<DataComponent> componentWithIdentity(Identity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		synchronized (lock) {
			return Optional.ofNullable(components.get(identity));
		}
	}

	void record(ReqRes reqres) {
		if (reqres == null) throw new IllegalArgumentException("null request");
		String path = reqres.request().path();
		//TODO could optimize this away with a 'smart' serializer
		if (!path.endsWith("/")) path += '/';
		Invoker invoker;
		synchronized (lock) {
			Optional<String> match;
			match = paths.parentOrSelf(path);
			if (!match.isPresent()) return;
			invoker = invokersByPath.get(match.get());
			reqres.trimPath(pathPrefixLength(invoker.component));
		}
		invoker.record(reqres);
	}

	private boolean add(DataComponent component) {
		Identity identity = component.details().identity();
		DataComponent existing = components.putIfAbsent(identity, component);
		if (existing != null) return false;
		Invoker invoker = new Invoker(component);
		invokers.put(component, invoker);
		process(invoker);
		return true;
	}

	private boolean remove(DataComponent component) {
		Identity identity = component.details().identity();
		return components.remove(identity, component);
	}

	private DataComponent remove(Details details) {
		Identity identity = details.identity();
		return components.remove(identity);
	}

	private void process(Invoker invoker) {
		Schedule schedule = invoker.component.schedule();
		if (schedule != null) {
			CoreContext context = dataTier.core.context();
			invoker.future = schedule.with(context.zoneId(), context.clock()).scheduling(invoker).on(dataTier.core.executor());
		}
		String path = path(invoker.component);
		if (path != null) {
			Invoker previous = invokersByPath.putIfAbsent(path, invoker);
			if (previous == null) {
				paths.add(path);
			} else {
				DashiLog.warn("data components {0} and {1} mapped to the same path: {2}", invoker.component.details(), previous.component.details(), path);
			}
		}
	}

	private void unprocess(Invoker invoker) {
		if (invoker.future != null) {
			invoker.future.cancel(true);
			invoker.future = null;
		}
		if (invoker.path != null) {
			paths.remove(invoker.path);
			invokersByPath.remove(invoker.path);
			invoker.path = null;
		}
	}

	//TODO look for something less heavyweight than taking the lock when recording
	private final class Invoker implements Runnable {

		final DataComponent component;
		Future<?> future = null;
		String path = null;

		Invoker(DataComponent component) {
			this.component = component;
		}

		@Override
		public void run() {
			synchronized (lock) {
				component.recorder().recordDataOnSchedule();
			}
		}

		void record(HttpReqRes reqres) {
			synchronized (lock) {
				component.recorder().recordDataOnHttp(reqres);
			}
		}
	}
}
