package com.superdashi.gosper.bundle;

import java.util.List;
import java.util.Map;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.framework.Namespace;

public final class AppData {

	private final Namespace namespace;
	private final AppRole role;
	private final Privileges privileges;
	private final Map<String, Namespace> namespaces;
	private final Map<Namespace, String> prefixes;
	private final Details appDetails;
	private final List<ActivityDetails> activityDetails;
	private final List<Details> typeDetails;
	private final List<Details> fieldDetails;


	public AppData(
			Namespace namespace,
			AppRole role,
			Privileges privileges,
			Map<String, Namespace> namespaces,
			Map<Namespace, String> prefixes,
			Details appDetails,
			List<ActivityDetails> activityDetails,
			List<Details> typeDetails,
			List<Details> fieldDetails
			) {
		this.namespace = namespace;
		this.role = role;
		this.privileges = privileges;
		this.namespaces = namespaces;
		this.prefixes = prefixes;
		this.appDetails = appDetails;
		this.activityDetails = activityDetails;
		this.typeDetails = typeDetails;
		this.fieldDetails = fieldDetails;
	}

	//TODO should return new a AppDetails class
	public Details appDetails() {
		return appDetails;
	}

	public List<ActivityDetails> activityDetails() {
		return activityDetails;
	}

	public List<Details> typeDetails() {
		return typeDetails;
	}

	public List<Details> fieldDetails() {
		return fieldDetails;
	}

	public Namespace namespace() {
		return namespace;
	}

	public AppRole role() {
		return role;
	}

	//TODO should differentiate between privileges requested, and those granted
	public Privileges privileges() {
		return privileges;
	}

	public Map<String, Namespace> namespacesByPrefix() {
		return namespaces;
	}

	public Map<Namespace, String> prefixesByNamespace() {
		return prefixes;
	}

}
