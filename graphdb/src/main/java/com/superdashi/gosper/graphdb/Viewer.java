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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.tomgibara.collect.Collect;
import com.tomgibara.collect.EquivalenceMap;
import com.tomgibara.collect.Collect.Maps;
import com.tomgibara.collect.Collect.Sets;

// frames a view
public final class Viewer {

	public static final String TYPE_NAME_PROPERTY = "gosper:name";
	public static final String TYPE_INTERPOLATE_PROPERTY = "gosper:interpolate";

	private static final Sets<String> stringSets = Collect.setsOf(String.class);
	private static final Sets<Namespace> namespaceSets = Collect.setsOf(Namespace.class);

	private static final Maps<Namespace,String> prefixMaps = namespaceSets.mappedTo(String.class);
	private static final Maps<String,Namespace> nsMaps = stringSets.mappedTo(Namespace.class);

	private static final EquivalenceMap<String, Namespace> emptyNs = nsMaps.newMap().immutableView();
	private static final EquivalenceMap<Namespace, String> emptyPrefixes = prefixMaps.newMap().immutableView();

	private static final Comparator<Identity> PERM_COMP = new Comparator<Identity>() {
		@Override
		public int compare(Identity a, Identity b) {
			int c;
			c = a.ns.compareTo(b.ns);
			if (c != 0) return c;
			c = a.name.compareTo(b.name);
			return c;
		}
	};

	public static class Builder {
		private final Identity identity;
		private SortedMap<String, Item> types = new TreeMap<>();
		private Map<String, Attribute> attributes = new HashMap<>();
		private Set<String> declaredPermissionNames = new HashSet<>();
		private SortedSet<Identity> grantedPermissions = new TreeSet<>(PERM_COMP); // nullification indicates all permissions granted
		private EquivalenceMap<Namespace, String> prefixesByNs = null;
		private EquivalenceMap<String, Namespace> nsByPrefix = null;

		public Builder(Identity identity) {
			this.identity = identity;
		}

		public Builder addType(String name) {
			Type.checkTypeName(name);
			types.put(name, Item.nothing());
			return this;
		}

		public Builder addType(String name, Item template) {
			Type.checkTypeName(name);
			if (template == null) throw new IllegalArgumentException("null template");
			types.put(name, template);
			return this;

		}

		public Builder addAttribute(String name, Value.Type type, Value value, boolean indexed) {
			if (name == null) throw new IllegalArgumentException("null name");
			if (type == null) throw new IllegalArgumentException("null type");
			if (value == null) throw new IllegalArgumentException("null value");
			attributes.put(name, new Attribute(new AttrName(identity.ns, name), type, value, indexed));
			return this;
		}

		public Builder addDeclaredPermission(String permissionName) {
			if (permissionName == null) throw new IllegalArgumentException("null permissionName");
			if (!Identity.isValidName(permissionName)) throw new IllegalArgumentException("invalid permissionName");
			declaredPermissionNames.add(permissionName);
			return this;
		}

		public Builder addDeclaredPermissions(String... permissionNames) {
			if (permissionNames == null) throw new IllegalArgumentException("null permissionNames");
			for (String permissionName : permissionNames) {
				addDeclaredPermission(permissionName);
			}
			return this;
		}

		public Builder grantAllPermissions() {
			grantedPermissions = null;
			return this;
		}

		public Builder addGrantedPermission(Identity permission) {
			if (permission == null) throw new IllegalArgumentException("null permission");
			if (grantedPermissions != null) {
				//TODO throw an IAE?
				if (!permission.ns.equals(identity.ns)) grantedPermissions.add(permission);
			}
			return this;
		}

		public Builder addGrantedPermissions(Identity... permissions) {
			if (permissions == null) throw new IllegalArgumentException("null permissions");
			for (Identity permission : permissions) {
				addGrantedPermission(permission);
			}
			return this;
		}

		public Builder addPrefix(String prefix, Namespace namespace) {
			Namespace.checkValidNamespacePrefix(prefix);
			if (namespace == null) throw new IllegalArgumentException("null namespace");
			if (prefixesByNs == null) {
				prefixesByNs = prefixMaps.newMap();
				nsByPrefix = nsMaps.newMap();
			}
			{
				String old = prefixesByNs.put(namespace, prefix);
				if (old != null && !prefix.equals(old))  {
					nsByPrefix.remove(old);
				}
			}
			{
				Namespace old = nsByPrefix.put(prefix, namespace);
				if (old != null && !namespace.equals(old)) {
					prefixesByNs.remove(old);
				}
			}
			return this;
		}

		public Viewer build() {
			return new Viewer(this);
		}
	}

	public static Builder createBuilder(Identity identity) {
		if (identity == null) throw new IllegalArgumentException("null identity");
		return new Builder(identity);
	}

	// the identity for the viewer
	public final Identity identity;
	// the namespace of the viewer
	public final Namespace namespace;
	// list of type names
	public final List<String> typeNames;
	// map of types to their template items
	public final Map<String, Item> typeItems;
	// those attributes that are typed
	public final Map<String, Attribute> typedAttrs;
	// those attributes that are defaulted
	public final Map<String, Attribute> defaultedAttrs;
	// those attributes that are indexed
	public final Map<String, Attribute> indexedAttrs;
	// the permissions that this viewer declares
	public final Set<String> declaredPermissionNames;
	// the permissions that this viewer has been granted - empty if universal
	public final Set<Identity> grantedPermissions;
	// the granted permissions grouped by namespace for efficient comparison
	final Map<Namespace, Set<String>> grantedByNs;
	// the prefix mappings used by this viewer
	final EquivalenceMap<Namespace, String> prefixesByNs;
	final EquivalenceMap<String, Namespace> nsByPrefix;
	// selector that selects nodes based on the granted permissions
	final Selector permSelector;

	private Viewer(Builder builder) {
		identity = builder.identity;
		namespace = builder.identity.ns;
		SortedMap<String, Attribute> typedAttrs = new TreeMap<>();
		SortedMap<String, Attribute> defaultedAttrs = new TreeMap<>();
		SortedMap<String, Attribute> indexedAttrs = new TreeMap<>();
		for (Attribute attr : builder.attributes.values()) {
			boolean typed = attr.type != Value.Type.EMPTY;
			boolean defaulted = !(typed ? attr.value.as(attr.type) : attr.value).isEmpty();
			boolean indexed = attr.indexed;
			String name = attr.name.name;
			if (typed) typedAttrs.put(name, attr);
			if (defaulted) defaultedAttrs.put(name, attr);
			if (indexed) indexedAttrs.put(name, attr);
		}
		Map<Namespace, Set<String>> grantedByNs;
		if (builder.grantedPermissions == null) {
			grantedByNs = null;
		} else {
			grantedByNs = new HashMap<>();
			builder.grantedPermissions.forEach(p -> {
				Set<String> set = grantedByNs.get(p.ns);
				if (set == null) {
					set = Collections.singleton(p.name);
					grantedByNs.put(p.ns, set);
				} else if (set.size() == 1) {
					set = new HashSet<>(set);
					set.add(p.name);
					grantedByNs.put(p.ns, set);
				} else {
					set.add(p.name);
				}
			});
			for (Entry<Namespace, Set<String>> entry : grantedByNs.entrySet()) {
				Set<String> set = entry.getValue();
				if (set.size() > 1) set = Collections.unmodifiableSet(set);
				entry.setValue(set);
			}
		}

		this.typeNames = Collections.unmodifiableList(new ArrayList<>(builder.types.keySet()));
		this.typeItems = Collections.unmodifiableMap(new LinkedHashMap<>(builder.types));
		this.typedAttrs = Collections.unmodifiableMap(typedAttrs);
		this.defaultedAttrs = Collections.unmodifiableMap(defaultedAttrs);
		this.indexedAttrs = Collections.unmodifiableMap(indexedAttrs);
		declaredPermissionNames = Collections.unmodifiableSet(new TreeSet<>(builder.declaredPermissionNames));
		grantedPermissions = builder.grantedPermissions == null ? Collections.emptySortedSet() : Collections.unmodifiableSet(new TreeSet<>(builder.grantedPermissions));
		this.grantedByNs = grantedByNs == null ? null : Collections.unmodifiableMap(grantedByNs);
		prefixesByNs = builder.prefixesByNs == null ? emptyPrefixes : builder.prefixesByNs.immutableCopy();
		nsByPrefix = builder.nsByPrefix == null ? emptyNs : builder.nsByPrefix.immutableCopy();

		Selector permSelector;
		if (builder.grantedPermissions == null) {
			permSelector = Selector.any();
		} else {
			permSelector = Selector.ownedBy(identity);
			for (Identity permission: grantedPermissions) {
				permSelector = permSelector.or(Selector.viewableWithPermission(permission));
			}
		}
		this.permSelector = permSelector;
	}
}
