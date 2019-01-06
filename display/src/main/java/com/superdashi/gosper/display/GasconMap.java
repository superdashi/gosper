package com.superdashi.gosper.display;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import javax.imageio.ImageIO;

import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.Streams;

final class GasconMap {

	private static InputStream resourceStream(String path) {
		return GasconMap.class.getClassLoader().getResourceAsStream(path);
	}

	public static CharMap loadFromResource(String resourcePath) {
		if (resourcePath == null) throw new IllegalArgumentException("null resourcePath");
		GasconLookup lookup = new GasconLookup(resourcePath + ".dat");
		GasconData data = new GasconData(resourcePath + ".png", lookup.indexLimit());
		return new CharMap(lookup, data);
	}

	private static class GasconLookup implements CharLookup {

		private final int[] codes;
		private final int[] indices;
		private final int count;

		GasconLookup(String path) {
			try (ReadStream in = Streams.streamInput(resourceStream(path))) {
				count = in.readInt();
				codes = new int[count];
				indices = new int[count + 1];
				indices[count] = in.readInt();
				for (int i = 0; i < count; i++) {
					codes[i] = in.readInt();
					indices[i] = in.readInt();
				}
			}
		}

		@Override
		public int locate(int codepoint) {
			if (codepoint < 0) return -1;
			int i = Arrays.binarySearch(codes, codepoint);
			if (i >= 0) return indices[i];
			i = -2 - i;
			int index = indices[i] + codepoint - codes[i];
			if (index >= indices[i + 1]) return -1;
			return index;
		}

		@Override
		public byte u(int location) {
			return (byte) location;
		}

		@Override
		public byte v(int location) {
			return (byte) (location >> 8);
		}


		int indexLimit() {
			return indices[count];
		}
	}

	private static class GasconData implements CharData {

		private final String path;
		private final int rows;

		GasconData(String path, int indexLimit) {
			this.path = path;
			rows = (indexLimit + 255) >> 8;
		}

		@Override
		public BufferedImage loadChars() {
			try (InputStream in = resourceStream(path)) {
				return ImageIO.read(in);
			} catch (IOException e) {
				throw new RuntimeException("Failed to load " + path);
			}
		}

		@Override
		public int cols() {
			return 256;
		}

		@Override
		public int rows() {
			return rows;
		}

	}
}
