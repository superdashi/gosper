package com.superdashi.gosper.config;

import com.tomgibara.storage.Store;

public class ConfigSelectors {

	// properties

	public static ConfigSelector type(ConfigMatcher matcher) {
		checkMatcher(matcher);
		return s -> matcher.matches(s.getStyleType().toString());
	}

	public static ConfigSelector id(ConfigMatcher matcher) {
		checkMatcher(matcher);
		return s -> matcher.matches(s.getId());
	}

	public static ConfigSelector role(ConfigMatcher matcher) {
		checkMatcher(matcher);
		return s -> matcher.matches(s.getRole().orElse(null));
	}

	// children

	public static ConfigSelector firstChild(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> children = s.getStyleableChildren();
			return children.size() > 0 && selector.matches(children.get(0));
		};
	}

	public static ConfigSelector lastChild(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> children = s.getStyleableChildren();
			int size = children.size();
			return size > 0 && selector.matches(children.get(size - 1));
		};
	}

	public static ConfigSelector anyChild(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> children = s.getStyleableChildren();
			for (Configurable child : children) {
				if (selector.matches(child)) return true;
			}
			return false;
		};
	}

	public static ConfigSelector allChildren(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> children = s.getStyleableChildren();
			for (Configurable child : children) {
				if (!selector.matches(child)) return false;
			}
			return true;
		};
	}

	public static ConfigSelector child(int index, ConfigSelector selector) {
		checkSelector(selector);
		return s -> indexedMatch(s.getStyleableChildren(), index, selector);
	}

	// ancestry

	public static ConfigSelector parent(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> ancestors = s.getStyleableAncestors();
			int size = ancestors.size();
			return size > 0 && selector.matches( ancestors.get(0) );
		};
	}

	public static ConfigSelector anyAncestor(ConfigSelector selector) {
		checkSelector(selector);
		return s -> {
			Store<? extends Configurable> ancestors = s.getStyleableAncestors();
			for (Configurable ancestor : ancestors) {
				if (selector.matches(ancestor)) return true;
			}
			return false;
		};
	}

	public static ConfigSelector ancestor(int index, ConfigSelector selector) {
		checkSelector(selector);
		return s -> indexedMatch(s.getStyleableAncestors(), index, selector);
	}

	// logical

	public static ConfigSelector not(ConfigSelector selector) {
		checkSelector(selector);
		return s -> { return !selector.matches(s); };
	}

	public static ConfigSelector and(ConfigSelector s1, ConfigSelector s2) {
		if (s1 == null) throw new IllegalArgumentException("null s1");
		if (s2 == null) throw new IllegalArgumentException("null s2");
		return s -> { return s1.matches(s) && s2.matches(s); };
	}

	public static ConfigSelector or(ConfigSelector s1, ConfigSelector s2) {
		if (s1 == null) throw new IllegalArgumentException("null s1");
		if (s2 == null) throw new IllegalArgumentException("null s2");
		return s -> { return s1.matches(s) || s2.matches(s); };
	}

	// private helpers

	private static void checkMatcher(ConfigMatcher matcher) {
		if (matcher == null) throw new IllegalArgumentException("null matcher");
	}

	private static void checkSelector(ConfigSelector selector) {
		if (selector == null) throw new IllegalArgumentException("null selector");
	}

	private static boolean indexedMatch(Store<? extends Configurable> configurables, int index, ConfigSelector selector) {
		int size = configurables.size();
		int i;
		if (index < 0) {
			i = size + index;
			if (i < 0) return false;
		} else {
			i = index;
			if (i >= size) return false;
		}
		return selector.matches(configurables.get(i));
	}
}
