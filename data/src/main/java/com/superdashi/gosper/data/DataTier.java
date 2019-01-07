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

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStore.Builder;

import com.superdashi.gosper.config.ConfigRules;
import com.superdashi.gosper.config.Configurator;
import com.superdashi.gosper.core.CoreTier;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.graphdb.AttrName;
import com.superdashi.gosper.graphdb.Edit;
import com.superdashi.gosper.graphdb.Inspect;
import com.superdashi.gosper.graphdb.Space;
import com.superdashi.gosper.graphdb.Store;
import com.superdashi.gosper.graphdb.Type;
import com.superdashi.gosper.graphdb.View;
import com.superdashi.gosper.graphdb.Viewer;

public final class DataTier {

	private static final String DEFAULT_URI_PREFIX = "/data/";

	final CoreTier core;
	private final Map<Identity, Context> contexts = new HashMap<>();
	private final DataRegistry registry;
	private final Store store;
	private ConfigRules configRules = ConfigRules.none();
	private volatile Space space = null;

	public DataTier(CoreTier core, URI dbUri) {
		if (core == null) throw new IllegalArgumentException("null core");
		if (dbUri == null) throw new IllegalArgumentException("null dbUri");
		this.core = core;
		registry = new DataRegistry(this);
		Builder mvBuilder = new MVStore.Builder();
		mvBuilder.autoCommitDisabled();
		Store store;
		switch (dbUri.getScheme()) {
		case "mem":
			store = Store.newMemStore();
			break;
		case "file":
			Path path = Paths.get(dbUri);
			store = Store.fileStore(path);
			break;
		default: throw new IllegalArgumentException("invalid graph URI: " + dbUri);
		}
		this.store = store;
	}

	public void setConfigRules(ConfigRules configRules) {
		if (configRules == null) throw new IllegalArgumentException("null configRules");
		this.configRules = configRules;
	}

	public void start() {
		core.setHandler(DEFAULT_URI_PREFIX, registry::record);
	}

	public void stop() {
		core.clearHandler(DEFAULT_URI_PREFIX);
		if (space != null) {
			if (space.active()) {
				space.close();
			}
			space = null;
		}
	}

	//TODO should only be possible once started
	public DataContext registerViewer(Details details, Viewer viewer) {
		if (viewer == null) throw new IllegalArgumentException("null viewer");
		Context previous = contexts.get(viewer.namespace);
		if (previous != null) throw new IllegalArgumentException("viewer already registered for " + viewer.namespace);
		if (!viewer.namespace.equals(details.identity().ns)) throw new IllegalArgumentException("mismatched namespaces");
		space().associate(viewer);
		Context context = new Context(details);
		contexts.put(viewer.identity, context);
		return context;
	}

	public Optional<DataContext> dataContext(Identity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		return Optional.ofNullable( contexts.get(identity) );
	}

	//TODO need method to access space


	public void addRecorder(DataRecorder recorder, Details details) {
		if (recorder == null) throw new IllegalArgumentException("null recorder");
		if (details == null) throw new IllegalArgumentException("null details");
		Namespace ns = details.identity().ns;
		Context context = contexts.get(ns);
		if (context == null) throw new IllegalArgumentException("no viewer registered for recorder namespace: " + ns);
		DataComponent component = new DataComponent(recorder, details);
		component.init(context);
		new Configurator(component, configRules).configure();
		if (!registry.addComponent(component)) {
			DashiLog.warn("duplicated data recorder: {0}", details);
		}
	}

	public void removeRecorders(Details details) {
		if (details == null) throw new IllegalArgumentException("null details");
		registry.removeComponentWithDetails(details).ifPresent(this::destroy);
	}

	public void removeRecorders(Collection<Details> details) {
		if (details == null) throw new IllegalArgumentException("null details");
		destroy(registry.removeComponentsWithDetails(details));
	}

	public void removeAllRecorders() {
		destroy(registry.removeAllComponents());
	}

	public void destroy() {
		removeAllRecorders();
		if (space != null) {
			if (space.active()) space.close();
			space = null;
		}
	}

	// private utility methods

	private void destroy(DataComponent removed) {
		removed.destroy();
	}

	private void destroy(Collection<DataComponent> removed) {
		removed.forEach(this::destroy);
	}

	private Space space() {
		if (space == null) {
			space = new Space(store);
		}
		return space;
	}

	// defers creation of space
	// because attaching viewers is more efficient if space is not yet open
	private Space activeSpace() {
		Space space = space();
		space.open();
		//TODO attach the observers
		return space;
	}

	private final class Context implements DataContext {

		private final Details details;

		private View view = null;

		Context(Details details) {
			this.details = details;
		}

		@Override
		public Details details() {
			return details;
		}

		@Override
		public AttrName attrName(String name) {
			return view().attrName(name);
		}

		@Override
		public Type type(String typeName) {
			return view().type(typeName);
		}

		@Override
		public Inspect inspect() {
			return view().inspect();
		}

		@Override
		public Edit edit() {
			return view().edit();
		}

		@Override
		public boolean isTypeAvailable(Type type) {
			return view.isTypeAvailable(type);
		}

		private View view() {
			if (view == null) {
				view = activeSpace().view(details.identity());
			}
			return view;
		}
	}

}
