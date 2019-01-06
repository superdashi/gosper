package com.superdashi.gosper.bundle;

// TODO want to avoid these forward references
public final class BundleClasses {

	private static ClassLoader classLoader = null;

	public static void configure(ClassLoader classLoader) {
		if (classLoader == null) throw new IllegalArgumentException("null classLoader");
		if (BundleClasses.classLoader != null) throw new IllegalStateException("class loader already set");
		BundleClasses.classLoader = classLoader;
	}

	private static Class<?> classForName(String name) {
		try {
			return classLoader == null ? Class.forName(name) : classLoader.loadClass(name);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	static final Class<?> APP_CLASS         = classForName("com.superdashi.gosper.micro.Application"       );
	static final Class<?> TRIVIAL_APP_CLASS = classForName("com.superdashi.gosper.micro.TrivialApplication");
	static final Class<?> JS_APP_CLASS      = classForName("com.superdashi.gosper.micro.JSApplication"     );
	static final Class<?> ACTIVITY_CLASS    = classForName("com.superdashi.gosper.micro.Activity"          );

	private BundleClasses() { }
}
