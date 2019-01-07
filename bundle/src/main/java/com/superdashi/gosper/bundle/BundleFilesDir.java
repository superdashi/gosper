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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class BundleFilesDir implements BundleFiles {

	private final Path root;

	private static Stream<Path> expand(Path path) {
		try {
			if (Files.isRegularFile(path)) return Stream.of(path);
			if (Files.isDirectory(path)) return Files.list(path).flatMap(BundleFilesDir::expand);
		} catch (IOException e) {
			//TODO log this properly
			e.printStackTrace();
		}
		return Stream.empty();
	}

	public BundleFilesDir(Path root) {
		if (root == null) throw new IllegalArgumentException("null root");
		this.root = root;
	}

	@Override
	public boolean available() {
		return Files.isDirectory(root);
	}

	@Override
	public URI uri() {
		return root.toUri();
	}

	@Override
	public Stream<String> names() {
		return expand(root).map(root::relativize).map(Path::toString);
	}

	@Override
	public boolean isName(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		Path path = root.resolve(name);
		return Files.isRegularFile(path);
	}

	@Override
	public BundleFile file(String name) {
		if (name == null) throw new IllegalArgumentException("null name");
		//TODO should check for valid name here?
		Path path = root.resolve(name);
		// file may exist in future, so we return one, and don't check if exists now
		return BundleFileUtil.newSystemFile(name, path);
	}
}
