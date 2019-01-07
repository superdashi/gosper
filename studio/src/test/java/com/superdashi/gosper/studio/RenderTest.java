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
package com.superdashi.gosper.studio;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.superdashi.gosper.studio.Frame;

public class RenderTest {

	final Path dir = Paths.get("target", "image-results");
	final Path out = Paths.get("target", "image-diffs");

	public void recordResult(Frame frame, String name) {
		try {
			Files.createDirectories(dir);
			ImageIO.write(frame.toImage(), "PNG", new File(dir.toFile(), name + ".png"));
		} catch (IOException e) {
			throw new RuntimeException("Failed to record " + name, e);
		}
	}

}
