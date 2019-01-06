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
