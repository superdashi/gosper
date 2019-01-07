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
package com.superdashi.gosper.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.junit.Test;

import com.superdashi.gosper.model.Model;
import com.superdashi.gosper.model.ModelIO;
import com.superdashi.gosper.model.ModelMaterial;

public class ModelIOTest {

	private static final ModelMaterial material = ModelMaterial.fromColor(0xffffffff);

	@Test
	public void testReadSTL() throws IOException {
		Model model;
		try (Reader r = new FileReader(new File("src/test/resources/logo.stl"))) {
			model = new ModelIO().readSTL(r, material);
		}
	}

}
