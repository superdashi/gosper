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
