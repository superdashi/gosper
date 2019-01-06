package com.superdashi.gosper.bundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.Test;

import com.superdashi.gosper.bundle.BundleFile;
import com.superdashi.gosper.bundle.BundleFilesTxt;

public class TestBundleFilesText {

	@Test
	public void testTextCons() throws IOException {
		String text = "This is a test.";
		BundleFilesTxt files = new BundleFilesTxt(text);
		String uri = files.uri().toString();
		int i = uri.indexOf(',');
		String data = uri.substring(i + 1);
		byte[] bytes = Base64.getDecoder().decode(data);
		String copy = new String(bytes, StandardCharsets.UTF_8);
		assertEquals(text, copy);
	}

	@Test
	public void testNoFile() throws IOException {
		BundleFilesTxt files = new BundleFilesTxt("dashi-bundle");
		assertTrue(files.available());
		assertFalse(files.names().findAny().isPresent());

		assertFalse(new BundleFilesTxt("bundle\n").available());
	}

	@Test
	public void testSoloFile() throws IOException {
		String text = "dashi-bundle\nSEP hello.txt\nHello World!";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile file = files.file("hello.txt");
		assertEquals("Hello World!", file.openAsIterator().next());
	}

	@Test
	public void testBinaryFile() throws IOException {
		String text = "dashi-bundle\nSEP hello.bin;base64\nSGVsbG8gV29ybGQh";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile file = files.file("hello.bin");
		assertEquals("Hello World!", file.openAsIterator().next());
	}

	@Test
	public void testMultilineBinaryFile() throws IOException {
		String text = "dashi-bundle\nSEP hello.bin;base64\nSGV\nsbG8\ngV29\nybGQ\nh";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile file = files.file("hello.bin");
		assertEquals("Hello World!", file.openAsIterator().next());
	}

	@Test
	public void testMultilineFile() throws IOException {
		String text = "dashi-bundle\nSEP count.txt\nOne\nTwo\nThree\n";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile file = files.file("count.txt");
		String[] lines = file.openAsStream().toArray(String[]::new);
		assertEquals("One", lines[0]);
		assertEquals("Two", lines[1]);
		assertEquals("Three", lines[2]);
		assertEquals(3, lines.length);
	}


	@Test
	public void testTwoFiles() throws IOException {
		String text = "dashi-bundle\nSEP robin.txt\nRobin Hood\nSEP john.txt\nLittle John";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile robin = files.file("robin.txt");
		BundleFile john = files.file("john.txt");
		String[] lines;

		lines = robin.openAsStream().toArray(String[]::new);
		assertEquals("Robin Hood", lines[0]);
		assertEquals(1, lines.length);

		lines = john.openAsStream().toArray(String[]::new);
		assertEquals("Little John", lines[0]);
		assertEquals(1, lines.length);

	}

	@Test
	public void testJsStyleBundle() throws IOException {
		String text = "/*dashi-bundle\n*//*FILE A\nA\n*//*FILE B\nB\n/**/";
		BundleFilesTxt files = new BundleFilesTxt(text);
		BundleFile a = files.file("A");
		BundleFile b = files.file("B");
		try (Reader r = a.openAsReader()) {
			assertEquals('A', r.read());
			assertEquals(-1, r.read());
		}
		try (Reader r = b.openAsReader()) {
			assertEquals('B', r.read());
		}
	}
}
