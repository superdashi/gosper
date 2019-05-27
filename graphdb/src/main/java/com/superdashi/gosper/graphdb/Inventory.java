/*
 * Copyright (C) 2019 Dashi Ltd.
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

import java.util.Random;
import java.util.Set;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVMap.Builder;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.framework.Namespace;
import com.tomgibara.fundament.Bijection;

public final class Inventory {

	static final String MAP_NAME_TYPES_BY_CODE = "typesByCode";
	static final String MAP_NAME_CODES_BY_TYPE = "codesByType";
	static final String MAP_NAME_TAGS_BY_CODE  = "tagsByCode" ;
	static final String MAP_NAME_CODES_BY_TAG  = "codesByTag" ;
	static final String MAP_NAME_ATTRS_BY_CODE = "attrsByCode";
	static final String MAP_NAME_CODES_BY_ATTR = "codesByAttr";
	static final String MAP_NAME_PERMS_BY_CODE = "permsByCode";
	static final String MAP_NAME_CODES_BY_PERM = "codesByPerm";
	static final String MAP_NAME_NSS_BY_CODE   = "nssByCode"  ;
	static final String MAP_NAME_CODES_BY_NS   = "codesByNs"  ;
	static final String MAP_NAME_IDS_BY_CODE   = "idsByCode"  ;
	static final String MAP_NAME_CODES_BY_ID   = "codesById"  ;

	final MVStore store;

	//TOOD probably needs proper random
	private final Random rand = new Random();

	final Lookup<Namespace> namespaceLookup;
	final Lookup<String> identityNameLookup;
	final Lookup<String> typeNameLookup;
	final Lookup<String> attrNameLookup;
	//TODO could merge with identityNameLookup
	final Lookup<String> permissionLookup;

	private final MVMap<Integer, String> tagsByCode;
	private final MVMap<String, Integer> codesByTag;

	private boolean locked = false;
	
	Inventory(MVStore store) {
		this.store = store;

		{
			Builder<Integer, String> builder = new MVMap.Builder<>();
			builder.keyType(IntType.instance);
			tagsByCode = store.openMap(MAP_NAME_TAGS_BY_CODE, builder);
		}
		{
			Builder<String, Integer> builder = new MVMap.Builder<>();
			builder.valueType(IntType.instance);
			codesByTag = store.openMap(MAP_NAME_CODES_BY_TAG, builder);
		}

		namespaceLookup = new Lookup<>(store, MAP_NAME_NSS_BY_CODE, MAP_NAME_CODES_BY_NS, Bijection.fromFunctions(String.class, Namespace.class, Namespace::new, Namespace::toString), () -> rand.nextInt() & 0x7ffffffe);
		identityNameLookup = new Lookup<>(store, MAP_NAME_IDS_BY_CODE, MAP_NAME_CODES_BY_ID, Bijection.identity(String.class), Lookup.positiveRandom(rand));
		typeNameLookup = new Lookup<>(store, MAP_NAME_TYPES_BY_CODE, MAP_NAME_CODES_BY_TYPE, Bijection.identity(String.class), Lookup.positiveRandom(rand));
		attrNameLookup = new Lookup<>(store, MAP_NAME_ATTRS_BY_CODE, MAP_NAME_CODES_BY_ATTR, Bijection.identity(String.class), () -> rand.nextInt() & 0x7ffffffe | 0x00000001);
		permissionLookup = new Lookup<>(store, MAP_NAME_PERMS_BY_CODE, MAP_NAME_CODES_BY_PERM, Bijection.identity(String.class), Lookup.positiveRandom(rand));

	}

	public long revision() {
		return store.getCurrentVersion();
	}

	//TODO how to handle synchronization
	public Set<String> permissions() {
		return permissionLookup.getByObj().keySet();
	}

	//TODO how to handle synchronization
	public Set<Namespace> namespaces() {
		return namespaceLookup.getByObj().keySet();
	}

	// returns true if store modified
	boolean lock() {
		return
				namespaceLookup   .lock() |
				identityNameLookup.lock() |
				typeNameLookup    .lock() |
				attrNameLookup    .lock() |
				permissionLookup  .lock() ;
	}

	void unlock() {
		namespaceLookup   .unlock();
		identityNameLookup.unlock();
		typeNameLookup    .unlock();
		attrNameLookup    .unlock();
		permissionLookup  .unlock();
	}

	long identityId(Identity identity) {
		int nsc = namespaceLookup.getByObj().getOrDefault(identity.ns, -1);
		int nmc = identityNameLookup.getByObj().getOrDefault(identity.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long typeId(Type type) {
		int nsc = namespaceLookup.getByObj().getOrDefault(type.namespace, -1);
		int nmc = typeNameLookup.getByObj().getOrDefault(type.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long permId(Identity perm) {
		int nsc = namespaceLookup.getByObj().getOrDefault(perm.ns, -1);
		int nmc = permissionLookup.getByObj().getOrDefault(perm.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	long tagId(Tag tag) {
		int nsc = namespaceLookup.getByObj().getOrDefault(tag.namespace, -1);
		int nmc = codesByTag.getOrDefault(tag.name, -1);
		return nsc < 0 || nmc < 0 ? -1L : Name.nsnId(nsc, nmc);
	}

	// creates if necessary
	int codeForTag(String tagName) {
		int code = codesByTag.getOrDefault(tagName, -1);
		if (code < 0) {
			do {
				code = rand.nextInt() & 0x7fffffff;
			} while (tagsByCode.containsKey(code));
			codesByTag.put(tagName, code);
			tagsByCode.put(code, tagName);
		}
		return code;
	}

	String tagForCode(int code) {
		String tagName = tagsByCode.get(code);
		if (tagName == null) throw new IllegalArgumentException("no tag name for code: " + code);
		return tagName;
	}


}
