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
package com.superdashi.gosper.bundle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Kind;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.framework.Type;
import com.superdashi.gosper.graphdb.AttrName;
import com.superdashi.gosper.graphdb.Viewer;
import com.superdashi.gosper.graphdb.Viewer.Builder;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.logging.Logger;
import com.tomgibara.tries.IndexedTrie;
import com.tomgibara.tries.IndexedTries;
import com.tomgibara.tries.Tries;

public final class Bundle {

	private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.AppData.DEBUG"));

	// statics

	//TODO need a proper location for this
	static final Namespace NS_GOSPER = new Namespace("superdashi.com/gosper");
	private static final Type TYPE_GENERIC_APPLICATION = new Type(new Identity(NS_GOSPER, "Application"), Kind.APPLICATION);
	private static final Type TYPE_GENERIC_ACTIVITY = new Type(new Identity(NS_GOSPER, "Activity"), Kind.ACTIVITY);
	private static final Type TYPE_GENERIC_GRAPH_TYPE = new Type(new Identity(NS_GOSPER, "GraphType"), Kind.GRAPH_TYPE);
	private static final Type TYPE_GENERIC_GRAPH_ATTR = new Type(new Identity(NS_GOSPER, "NONE"), Kind.GRAPH_ATTR);

	private static final Map<Value.Type, Type> attrTypes;
	static {
		Map<Value.Type, Type> map = new EnumMap<>(Value.Type.class);
		for (Value.Type type : Value.Type.values()) {
			map.put(type, new Type(new Identity(NS_GOSPER, type.name()), Kind.GRAPH_ATTR));
		}
		attrTypes = Collections.unmodifiableMap(map);
	}

	private static final Class<?> DEFAULT_APP_CLASS = BundleClasses.TRIVIAL_APP_CLASS;
	private static final String DEFAULT_APP_ID_PREFIX = "application_";
	private static final AppRole DEFAULT_ROLE = AppRole.REGULAR;
	private static final boolean DEFAULT_LAUNCH = false;

	private static final IndexedTries<String> stringTries = Tries.serialStrings(StandardCharsets.UTF_8).nodeSource(Tries.sourceForCompactLookups()).indexed();

	private static final String PROPERTY_ROLE   = "gosper:role"  ;
	private static final String PROPERTY_TYPE   = "gosper:type"  ;
	private static final String PROPERTY_KIND   = "gosper:kind"  ;
	private static final String PROPERTY_FLAVOR = "gosper:flavor";
	private static final String PROPERTY_LAUNCH = "gosper:launch";
	private static final String PROPERTY_NAME   = "gosper:name"  ;
	private static final String PROPERTY_INDEX  = "gosper:index" ; // attributes only
	private static final String PROPERTY_VALUE  = "gosper:value" ; // attributes only

	private static Class<?> loadAppClass(ClassLoader classLoader, String className) {
		//TODO should require a privileges to honour this
		//TODO should be validated as a string first
		//TODO should be whitelisted to protect against maliciously launching system apps?
		Class<?> clss;
		try {
			clss = classLoader.loadClass(className);
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("no such application class: " + className, e);
		}
		if (!BundleClasses.APP_CLASS.isAssignableFrom(clss)) {
			throw new IllegalArgumentException("invalid application class: " + className);
		}
		return clss;
	}

	// fields

	public final String instanceId;
	private final Logger logger;
	private final BundleFiles files;
	private final BundleSettings settings;
	private final Map<String, Item[]> items;
	private final Map<String, Item[]> meta;
	private final ClassLoader classLoader;
	private final Class<?> appClass;
	private final AppData appData;
	private final Namespace namespace;
	private final String appId;
	private final IndexedTrie<String> activityIds;
	private final List<String> launchActivityIds;
	private final Map<String, Details> typeDetailsById;
	private final Map<String, Details> fieldDetailsById;
	private final Item defaultSettings;
	private final Viewer viewer;

	//TODO need to cope with invalid parameters
	Bundle(BundleCollation collation) {
		this.instanceId = collation.instanceId;
		this.logger = collation.logger;

		this.files = collation.files;
		this.settings = collation.settings;
		this.items = collation.itemsById;
		this.meta = collation.metaById;
		this.classLoader = collation.classLoader;

		// build public instance

		Privileges privileges = collation.privileges;
		Map<String, Namespace> namespaces = collation.namespaces;

		//TODO use collect
		//TODO could build externally during verification phase and pass in
		Map<Namespace, String> prefixes = new HashMap<>();
		for (Entry<String, Namespace> entry : namespaces.entrySet()) {
			prefixes.put(entry.getValue(), entry.getKey());
		}
		prefixes = Collections.unmodifiableMap(prefixes);

		Details appDetails;
		{
			//TODO should check for
			String appId = null;
			Item appItem = null;
			for (Entry<String,Item[]> entry : meta.entrySet()) {
				Item i = entry.getValue()[0]; // guaranteed to be universal if there is any
				if (!i.qualifier().isUniversal()) continue; // not universal, so ignore
				Value kindValue = i.value(PROPERTY_KIND);
				if (kindValue.isEmpty()) continue; // no kind specified, so ignore
				Optional<Kind> kindOpt = Kind.optionalValueOf(kindValue.string());
				if (!kindOpt.isPresent()) continue; // invalid kind, so ignore
				if (kindOpt.get() != Kind.APPLICATION) continue; // not the correct kind, so ignore
				if (appId != null) throw new IllegalArgumentException("multiple application items"); //TODO not supported yet
				appId = entry.getKey();
				appItem = i;
			}
			if (appId == null) {
				appId = DEFAULT_APP_ID_PREFIX + instanceId;
				appItem = defaultUniversalAppMetaItem();
			}
			appDetails = detailsFromItem(appId, appItem, Kind.APPLICATION, TYPE_GENERIC_APPLICATION);
			if (appDetails == null) {
				throw new IllegalArgumentException("invalid application details");
			}
		}

		AppRole appRole;
		{
			String role = appDetails.role().orElse("");
			if (role.isEmpty()) {
				appRole = DEFAULT_ROLE;
				logger.debug().message("defaulting role to {}").values(DEFAULT_ROLE).log();
			} else try {
				appRole = AppRole.valueOf(role.toUpperCase());
			} catch (IllegalArgumentException e) {
				logger.error().message("invalid application role defaulting to {}: {}").values(DEFAULT_ROLE, role).log();
				appRole = DEFAULT_ROLE;
			}
		}

		// activity related items
		IndexedTrie<String> activityIds = stringTries.newTrie();
		List<String> launchActivityIds = new ArrayList<>();
		Map<String, ActivityDetails> activityMap = new HashMap<>(); //TODO use collect
		// schema related items
		Map<String, Details> typeMap = new HashMap<>();
		Map<String, Details> attrMap = new HashMap<>();
		Set<String> indexedAttrs = new HashSet<>();
		Map<String, Value> attrDefaults = new HashMap<>();

		for (Entry<String, Item[]> entry : meta.entrySet()) {
			for (Item item : entry.getValue()) {
				if (!item.qualifier().isUniversal()) continue;
				String id = entry.getKey();
				Details details;
				//TODO should just switch on kind
				// activities
				details = detailsFromItem(id, item, Kind.ACTIVITY, TYPE_GENERIC_ACTIVITY);
				if (details != null) {
					String name = details.identity().name;
					Flavor flavor = flavorFromItem(name, item);
					boolean launch = launchFromItem(name, item);
					activityIds.add(name);
					activityMap.put(name, new ActivityDetails(details, flavor, launch));
					if (launch) launchActivityIds.add(name);
					continue;
				}
				// types
				details = detailsFromItem(id, item, Kind.GRAPH_TYPE, TYPE_GENERIC_GRAPH_TYPE);
				if (details != null) {
					String name = details.identity().name;
					if (!com.superdashi.gosper.graphdb.Type.isValidTypeName(name)) {
						logger.error().message("invalid graph type name: {}").values(name).log();
					} else {
						typeMap.put(name, details);
					}
					continue;
				}
				// attrs
				details = detailsFromItem(id, item, Kind.GRAPH_ATTR, TYPE_GENERIC_GRAPH_ATTR);
				if (details != null) {
					String name = details.identity().name;
					if (!AttrName.isValidAttrName(name)) {
						logger.error().message("invalid graph attribute name: {}").values(name).log();
					} else {
						attrMap.put(name, details);
						if (!item.value(PROPERTY_INDEX).isEmpty()) indexedAttrs.add(name);
						Value value = item.value(PROPERTY_VALUE);
						if (!value.isEmpty()) attrDefaults.put(name, value);
					}
				}
			}
		}
		activityIds.compactStorage();
		List<ActivityDetails> activityDetails = new ArrayList<>(activityIds.size());
		for (String id : activityIds) {
			activityDetails.add(activityMap.get(id));
		}
		activityDetails = Collections.unmodifiableList(activityDetails);
		List<Details> typeDetails = Collections.unmodifiableList(new ArrayList<>(typeMap.values()));
		List<Details> fieldDetails = Collections.unmodifiableList(new ArrayList<>(attrMap.values()));

		appData = new AppData(settings.namespace, appRole, privileges, namespaces, prefixes, appDetails, activityDetails, typeDetails, fieldDetails);

		// extract additional own fields

		namespace = appDetails.identity().ns;
		appId = appDetails.identity().name;
		this.activityIds = activityIds.immutableView();
		this.launchActivityIds = Collections.unmodifiableList(launchActivityIds);
		typeDetailsById = Collections.unmodifiableMap(typeMap);
		fieldDetailsById = Collections.unmodifiableMap(attrMap);
		if (classLoader == null) {
			switch (settings.language) {
			case JS:
				appClass = BundleClasses.JS_APP_CLASS;
				break;
			case JAVA:
			default:
				throw new IllegalStateException("unexpected bundle language: " + settings.language);
			}
		} else if (appDetails.type().equals(TYPE_GENERIC_APPLICATION)) {
			appClass = DEFAULT_APP_CLASS;
		} else {
			appClass = loadAppClass(classLoader, appDetails.type().identity.name);
		}
		{
			Item defaultSettings = null;
			String id = settings.defaultSettingsId;
			if (id != null) {
				//TODO is it necessary to walk all items?
				Item[] defaults = items.get(id);
				if (defaults != null) {
					for (Item item : defaults) {
						if (item.qualifier().isUniversal()) {
							defaultSettings = item;
							break;
						}
					}
				}
				if (defaultSettings == null) {
					logger.error().message("no universal item with id {} for settings").values(id).log();
				}
			}
			this.defaultSettings = defaultSettings;
		}

		{
			Builder builder = Viewer.createBuilder(appDetails.identity());
			typeMap.forEach((n,t) -> {
				builder.addTypeName(n);
			});
			//TODO inefficient, since it has to map type back again
			attrMap.forEach((n,d) -> {
				Type type = d.type();
				if (type != TYPE_GENERIC_GRAPH_ATTR) {
					builder.addAttribute(n,
							Value.Type.valueOf(type.identity.name),
							attrDefaults.getOrDefault(n, Value.empty()),
							indexedAttrs.contains(n)
							);
				}
			});
			prefixes.forEach((ns, p) -> builder.addPrefix(p, ns));
			if (privileges.accessDbUniversally) {
				builder.grantAllPermissions();
			} else {
				//TODO add declared/granted permissions
			}
			viewer = builder.build();
		}
	}

	// public accessors

	public AppData appData() {
		return appData;
	}

	public boolean compatibleWith(Qualifier qualifier) {
		//TODO implement so that apps can declare limits to the representations they can provide
		return true;
	}

	public BundleFiles files() {
		return files;
	}

	public List<String> launchActivityIds() {
		return launchActivityIds;
	}

	public Optional<ActivityDetails> optionalActivityDetails(String activityId) {
		int index = activityIds.indexOf(activityId);
		return index < 0 ? Optional.empty() : Optional.of(appData.activityDetails().get(index));
	}

	public ActivityDetails activityDetails(String activityId) {
		int index = activityIds.indexOf(activityId);
		if (index < 0) throw new IllegalArgumentException("unknown activityId");
		return appData.activityDetails().get(index);
	}

	public Optional<Details> optionalRecordTypeDetails(String typeName) {
		if (typeName == null) throw new IllegalArgumentException("null typeName");
		return Optional.ofNullable( typeDetailsById.get(typeName) );
	}

	public Details recordTypeDetails(String typeName) {
		if (typeName == null) throw new IllegalArgumentException("null typeName");
		Details details = typeDetailsById.get(typeName);
		if (details == null) throw new IllegalArgumentException("unknown typeName");
		return details;
	}

	// note: unprefixed field name, since always in this namespace
	public Optional<Details> optionalRecordFieldDetails(String fieldName) {
		if (fieldName == null) throw new IllegalArgumentException("null fieldName");
		return Optional.ofNullable( typeDetailsById.get(fieldName) );
	}

	// note: unprefixed field name, since always in this namespace
	public Details recordFieldDetails(String fieldName) {
		if (fieldName == null) throw new IllegalArgumentException("null fieldName");
		Details details = fieldDetailsById.get(fieldName);
		if (details == null) throw new IllegalArgumentException("unknown fieldName");
		return details;
	}

	public Class<?> appClass() {
		return appClass;
	}

	public Item appMetaItem(Qualifier qualifier) {
		return item(appId, qualifier, true).orElseGet(() -> defaultAppMetaItem(qualifier));
	}

	public Item activityMetaItem(Qualifier qualifier, String activityId) {
		return item(activityId, qualifier, true).orElseThrow(() -> new IllegalArgumentException("unknown activity id"));
	}

	public boolean isActivityId(String activityId) {
		return activityIds.contains(activityId);
	}

	public Optional<Item> item(String id, Qualifier qualifier, boolean meta) {
		return item(id, qualifier, meta ? this.meta : this.items);
	}

	public Optional<Item> defaultSettings() {
		return Optional.ofNullable(defaultSettings);
	}

	public Viewer graphViewer() {
		return viewer;
	}

	// empty if cannot be instantiated because defined in a different namespace, throws an exception otherwise
	public Optional<Object> instantiate(Details details) throws ReflectiveOperationException {
		// can only instantiate details that are in the ns of this app
		if (!details.identity().ns.equals(namespace)) return Optional.empty();
		Type type = details.type();
		Identity identity = type.identity;
		// can only instantiate types that belong to this app
		if (!identity.ns.equals(namespace)) return Optional.empty();
		// we can't instantiate without a class loader (at this time)
		if (classLoader == null) return Optional.empty();
		Class<?> clss = classLoader.loadClass(identity.name);
		switch (type.kind) {
		case ACTIVITY:
			if (!BundleClasses.ACTIVITY_CLASS.isAssignableFrom(clss)) throw new ReflectiveOperationException(clss.getName() + " not an Activity implementation");
			break;
		case APPLICATION:
			if (!BundleClasses.APP_CLASS.isAssignableFrom(clss)) throw new ReflectiveOperationException(clss.getName() + " not an Application implementation");
			break;
		case DATA_RECORDER:
		case INFO_ACQUIRER:
		case INFO_RENDERER:
		default:
			throw new ReflectiveOperationException("instantiation of " + type.kind + " not supported");
		}
		return Optional.of(clss.newInstance());
	}

	private Details detailsFromItem(String id, Item item, Kind requiredKind, Type genericType) {
		String kind = item.value(PROPERTY_KIND).optionalString().orElse("");
		if (requiredKind != Kind.optionalValueOf(kind).orElse(null)) {
			return null;
		}

		// name defaults to the id, but can be overidden using gosper:name property
		String name = item.value(PROPERTY_NAME).optionalString().orElse(id);

		Identity identity;
		try {
			identity = new Identity(settings.namespace, name);
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid identity for {} details: {}").values(requiredKind, id).log();
			return null;
		}

		String role = item.value(PROPERTY_ROLE).optionalString().orElse(null);
		String typeStr = item.value(PROPERTY_TYPE).optionalString().orElse(null);
		Type type;
		switch (requiredKind) {
		case GRAPH_TYPE:
			// special case, type cannot be modified
			if (typeStr != null) {
				logger.warning().message("custom type ignored for {} with id {}").values(requiredKind, id).log();
			}
			type = genericType;
			break;
		case GRAPH_ATTR:
			// special case for graph attributes - type name currently maps to defined types
			if (typeStr == null) {
				type = genericType;
			} else {
				Optional<Value.Type> opt = Value.Type.optionalValueOf(typeStr);
				if (opt.isPresent()) {
					type = attrTypes.get(opt.get());
				} else {
					logger.error().message("invalid type for {} with id {}: {}").values(requiredKind, id, typeStr).log();
					return null;
				}
			}
			break;
		default:
			// default case
			if (typeStr == null) {
				type = genericType;
			} else try {
				type = new Type(new Identity(settings.namespace, typeStr), requiredKind);
			} catch (IllegalArgumentException e) {
				logger.error().message("invalid type for {} details: {}").values(requiredKind, typeStr).log();
				return null;
			}
		}

		Details details;
		try {
			details = role == null ? Details.typeAndIdentity(type, identity) : Details.typeIdentityAndRole(type, identity, role);
		} catch (IllegalArgumentException e) {
			logger.error().message("invalid role for {} with id {}: {}").values(requiredKind, id, role).log();
			return null;
		}

		return details;
	}

	private Flavor flavorFromItem(String id, Item item) {
		Optional<String> opt = item.value(PROPERTY_FLAVOR).optionalString();
		if (opt.isPresent()) try {
			return Flavor.valueOf(opt.get().toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.warning().message("invalid flavor for {} activity with id {}").values(opt.get(), id).log();
		}
		return Flavor.GENERIC;
	}

	private boolean launchFromItem(String id, Item item) {
		String launch = item.value(PROPERTY_LAUNCH).optionalString().orElse("");
		if (launch.isEmpty()) return false;
		if (launch.equalsIgnoreCase("true")) return true;
		if (launch.equalsIgnoreCase("false")) return false;
		logger.warning().message("value for {} on item with id {} not \"true\" or \"false\", defaulting to {}").values(PROPERTY_LAUNCH, id, DEFAULT_LAUNCH).log();
		return false;
	}

	private Optional<Item> item(String id, Qualifier qualifier, Map<String, Item[]> source) {
		if (id == null) throw new IllegalArgumentException("null id");
		if (qualifier == null) throw new IllegalArgumentException("null qualifier");
		if (!qualifier.isFullySpecified()) throw new IllegalArgumentException("qualifier not fully specified");

		if (DEBUG) logger.debug().message("request for item with id {} and qualifier {} from source {}").values(id, qualifier, source == meta ? "meta" : "items").log();

		// there may be no item with the given ID whatsoever
		Item[] items = source.get(id);
		if (items == null) {
			if (DEBUG) logger.debug().message("no item with id {}").values(id).log();
			return Optional.empty();
		}

		Item single = null;
		Item.Builder builder = null;
		for (Item item : items) {
			Qualifier q = item.qualifier();
			if (q.matches(qualifier)) {
				// if we've already matched one, switch to using a builder
				if (single != null) {
					builder = single.builder();
					single = null;
				}
				// if we're using a builder, add to it
				if (builder != null) {
					if (DEBUG) logger.debug().message("found subsequent match {}").values(item).log();
					builder.addItem(item);
				} else { // assign it as a single match
					if (DEBUG) logger.debug().message("found first match {}").values(item).log();
					single = item;
				}
			}
		}
		if (builder != null) single = builder.qualifyWith(qualifier).build();
		if (DEBUG) logger.debug().message("returned result for id {} is {}").values(id, single).log();
		return Optional.ofNullable(single);
	}

	private Optional<Item> genericMeta(String id) {
		Item[] items = meta.get(id);
		if (items != null) {
			for (Item item : items) {
				if (item.qualifier().isUniversal()) return Optional.of(item);
			}
		}
		return Optional.empty();
	}

	private Item defaultUniversalAppMetaItem() {
		return defaultAppMetaItem(Qualifier.universal());
	}

	private Item defaultAppMetaItem(Qualifier qualifier) {
		//TODO should be i18n
		return Item.newBuilder().label("Anonymous").addExtra(PROPERTY_KIND, Value.ofString(Kind.APPLICATION.toString())).build();
	}

}
