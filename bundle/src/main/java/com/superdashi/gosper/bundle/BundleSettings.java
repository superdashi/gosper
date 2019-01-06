package com.superdashi.gosper.bundle;

import com.superdashi.gosper.framework.Namespace;

class BundleSettings {

	final Namespace namespace;
	final BundleLanguage language;
	final String defaultSettingsId;

	BundleSettings(Namespace namespace, BundleLanguage language, String defaultSettingsId) {
		this.namespace = namespace;
		this.language = language;
		this.defaultSettingsId = defaultSettingsId;
	}

}
