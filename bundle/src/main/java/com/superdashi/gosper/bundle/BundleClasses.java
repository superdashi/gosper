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
