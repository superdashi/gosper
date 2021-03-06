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
package com.superdashi.gosper.graphdb;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Item;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.fundament.Consumer;

// provides a view over a space
public final class View implements Editor {

	//TODO make configurable
	private static final int MAX_LOOKUP = 1024;

	final Space space;
	final Identity identity;
	final Namespace namespace;
	final Selector selector;
	final long identityId; // cached for efficiency - the id of the identity that is viewing
	final Set<Type> availableTypes;
	final Set<String> declaredPermissions;
	final Set<Identity> grantedPermissions;
	final Map<Namespace, Set<String>> grantedByNs; // null indicates universal access
	final EquivalenceMap<Namespace, String> prefixesByNs;
	final EquivalenceMap<String, Namespace> nsByPrefix;

	private final Map<String, AttrName> attrNames = new LookupMap<>();
	private final Map<String, Type> types = new LookupMap<>();

	View(Space space, Viewer viewer) {
		assert space != null;
		this.space = space;
		this.identity = space.canonIdentities.get(viewer.identity);
		this.namespace = viewer.namespace;
		this.selector = viewer.permSelector;

		identityId = space.inventory.identityId(identity);
		availableTypes = space.availableTypes(namespace);
		declaredPermissions = viewer.declaredPermissionNames;
		grantedPermissions = viewer.grantedPermissions;
		grantedByNs = viewer.grantedByNs;
		prefixesByNs = viewer.prefixesByNs;
		nsByPrefix = viewer.nsByPrefix;
	}

	// inspector methods

	@Override
	public AttrName attrName(String name) {
		AttrName attrName = attrNames.get(name);
		if (attrName == null) {
			if (name == null) throw new IllegalArgumentException("null name");
			attrName = parse(name, AttrName::create);
			attrNames.put(name, attrName);
		}
		return attrName;
	}

	@Override
	public Type type(String typeName) {
		Type type = types.get(typeName);
		if (type == null) {
			if (typeName == null) throw new IllegalArgumentException("null typeName");
			type = parse(typeName, Type::create);
			types.put(typeName, type);
		}
		return type;
	}

	@Override
	public Inspect inspect() {
		return space.newInspect(this);
	}

	// editor methods

	//NOTE: wary of exposing set
	@Override
	public boolean isTypeAvailable(Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return availableTypes.contains(type);
	}

	@Override
	public Edit edit() {
		return space.newEdit(this);
	}

	// additional methods

	public Inspector asInspectorOnly() {
		return new Inspector() {

			@Override
			public AttrName attrName(String name) {
				return View.this.attrName(name);
			}

			@Override
			public Type type(String typeName) {
				return View.this.type(typeName);
			}

			@Override
			public Inspect inspect() {
				return View.this.inspect();
			}

		};
	}

	public Editor asEditorOnly() {
		return new Editor() {

			@Override
			public Type type(String typeName) {
				return View.this.type(typeName);
			}

			@Override
			public Inspect inspect() {
				return View.this.inspect();
			}

			@Override
			public AttrName attrName(String name) {
				return View.this.attrName(name);
			}

			@Override
			public boolean isTypeAvailable(Type type) {
				return View.this.isTypeAvailable(type);
			}

			@Override
			public Edit edit() {
				return View.this.edit();
			}
		};
	};

	public Observation observe(Selector selector, Consumer<PartRef> notifier) {
		if (selector == null) throw new IllegalArgumentException("null selector");
		if (notifier == null) throw new IllegalArgumentException("null notifier");
		selector = selector.and(this.selector);
		return new Observation(this, selector, notifier); // auto registers
	}

	private <T> T parse(String name, BiFunction<Namespace, String, T> cons) {
		int i = name.indexOf(':');
		if (i == -1) return cons.apply(namespace, name); // no prefix, just use viewer's prefix
		String prefix = name.substring(0, i);
		Namespace ns = nsByPrefix.get(prefix);
		if (ns == null) {
			Namespace.checkValidNamespacePrefix(prefix);
			throw new IllegalArgumentException("unmapped namespace prefix: " + prefix);
		}
		return cons.apply(ns, name.substring(i + 1));
	}

	private static final class LookupMap<V> extends LinkedHashMap<String, V> {
		private static final long serialVersionUID = 5973271053027896925L;

		@Override
		protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
			return size() >= MAX_LOOKUP;
		}
	}
}
