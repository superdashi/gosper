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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.framework.Namespace;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Image;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.logging.LogLevel;
import com.superdashi.gosper.logging.Logger;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.Csv;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class BundleCollation {

	private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.micro.AppCollation.DEBUG"));

	public static final String NAME_REQ_PRIV = "requested-privileges";
	public static final String NAME_BUNDLE_SETTINGS = "bundle.properties";
	public static final String NAME_NAMESPACES = "namespace.properties";
	private static final List<String> rootNames = Arrays.asList(NAME_REQ_PRIV, NAME_BUNDLE_SETTINGS, NAME_NAMESPACES);

	private static final Pattern COMMENT = Pattern.compile("\\s*#.*");
	private static final Pattern FILE_SEP = Pattern.compile("/");

	private static final String SETTING_NAMESPACE = "namespace";
	private static final String SETTING_LANGUAGE = "language";
	private static final String SETTING_SETTINGS = "settings";

	private static final String TAG_LANG = "lang";
	private static final String TAG_SCREEN = "screen";
	private static final String TAG_COLOR = "color";
	private static final String TAG_FLAVOR = "flavor";

	private static final String DIR_BADGES   = "badges";
	private static final String DIR_SYMBOLS  = "symbols";
	private static final String DIR_ICONS    = "icons";
	private static final String DIR_PICTURES = "pictures";

	private static final String EXT_CSV = "csv";
	private static final String EXT_ITEMS = "items";
	private static final List<String> ITEM_EXTS = Arrays.asList(EXT_CSV, EXT_ITEMS);

	private static final CsvParserSettings csvSettings = Csv.parseRfc4180();
	{
		csvSettings.setHeaderExtractionEnabled(true);
		csvSettings.setLineSeparatorDetectionEnabled(true);
		csvSettings.trimValues(true);
	}

	private static final BundleSettings badAppSettings = new BundleSettings(Bundle.NS_GOSPER, BundleLanguage.JAVA, null);

	private static String validIdOrNull(String id) {
		if (id == null || id.isEmpty()) return null;
		//TODO decide what constitutes a valid id
		// eg no dots and no spaces?
		return id;
	}

	private static String extension(String name) {
		int i = name.lastIndexOf('.');
		return i == -1 ? "" : name.substring(i + 1).toLowerCase();
	}

	private static Stream<String> winnow(Stream<String> lines) {
		return lines.filter(line -> !COMMENT.matcher(line).matches()).map(line -> line.trim()).filter(line -> !line.isEmpty());
	}

	//TODO codepoint safe?
	private static void trimTrailingWS(StringBuilder sb) {
		int i = sb.length();
		while (i > 0) {
			if (Character.isWhitespace(sb.charAt(i - 1))) {
				i --;
			} else {
				sb.setLength(i);
				return;
			}
		}
	}

	private static String id(String filename) {
		int i = filename.indexOf('.');
		switch (i) {
		case -1 : return validIdOrNull(filename);
		case 0  : return null;
		default : return validIdOrNull(filename.substring(0, i));
		}
	}

	public final String instanceId;
	public final Logger logger;
	public final ClassLoader parentClassLoader;
	public final boolean system;

	// declare the variables we need to populate to build bundle
	BundleFiles files;
	BundleSettings settings = badAppSettings;
	Privileges privileges;
	Map<String, Item[]> itemsById;
	Map<String, Item[]> metaById;
	Map<String, Namespace> namespaces = new HashMap<>();

	// for jar packaged apps
	ClassLoader classLoader = null;

	public final Bundle bundle;

	public BundleCollation(String collationId, BundleFiles files, Logger logger, ClassLoader parentClassLoader, boolean system) {
		this.instanceId = collationId;
		this.logger = logger;
		this.parentClassLoader = parentClassLoader;
		this.system = system;

		// first check whether the files are available

		this.files = files;
		if (files.available()) {
			// collect state
			privileges = Privileges.basic();

			// create a class loader if necessary
			if (files.isJar()) {
				try {
					classLoader = URLClassLoader.newInstance(new URL[] {files.uri().toURL()}, parentClassLoader);
				} catch (IOException e) {
					recordIssue(files.uri().toString(), -1, LogLevel.ERROR, "Unable to open jar file", e);
				}
			} else if (system) {
				classLoader = parentClassLoader;
			}

			// identify bundle settings
			{
				Iterator<String> lines = open(files, NAME_BUNDLE_SETTINGS, BundleFile::openAsIterator, Collections.emptyIterator());
				if (lines == null) {
					recordIssue(NAME_BUNDLE_SETTINGS, 0, LogLevel.ERROR, "no application settings found");
				} else if (lines.hasNext()) {
					Map<String, String> properties = parseProperties(NAME_BUNDLE_SETTINGS, lines);
					try {
						settings = settingsFromMap(properties);
					} catch (IllegalArgumentException e) {
						recordIssue(NAME_BUNDLE_SETTINGS, 0, LogLevel.ERROR, "invalid application settings", e);
					}
				}
			}

			// identify privileges
			{
				Stream<String> lines = open(files, NAME_REQ_PRIV, BundleFile::openAsStream, Stream.empty());
				if (lines != null) {
					List<String> privilegeNames = winnow(lines).collect(Collectors.toList());
					try {
						Privileges additional = Privileges.fromNames(privilegeNames);
						privileges = privileges.augment(additional);
					} catch (IllegalArgumentException e) {
						//TODO should probably just try to drop single invalid privilege
						recordIssue(NAME_REQ_PRIV, 0, LogLevel.ERROR, "invalid privilege");
					}
				}
			}

			// identify namespaces
			{
				Iterator<String> lines = open(files, NAME_NAMESPACES, BundleFile::openAsIterator, Collections.emptyIterator());
				if (lines != null && lines.hasNext()) {
					Map<String, String> properties = parseProperties(NAME_NAMESPACES, lines);
					Set<Namespace> set = new HashSet<>();
					for (Entry<String, String> entry : properties.entrySet()) {
						String prefix = entry.getKey();
						if (!Namespace.isValidNamespacePrefix(prefix)) {
							logger.error().message("dropping invalid namespace prefix {}").values(prefix).filePath(NAME_NAMESPACES).log();
						} else try {
							Namespace ns = new Namespace(entry.getValue());
							if (set.add(ns)) {
								namespaces.put(prefix, ns);
							} else {
								logger.error().message("dropping duplicate namespace {}").values(ns).filePath(NAME_NAMESPACES).log();
							}
						} catch (IllegalArgumentException e) {
							logger.error().message("dropping invalid namespace {}").values(entry.getValue()).filePath(NAME_NAMESPACES).log();
						}
					}
				}
				namespaces = Collections.unmodifiableMap(namespaces);
			}

//			if (!rootNames.contains(subName)) {
//				// ignore with warning
//				recordIssue(name, LogLevel.WARNING, "ignoring file in root directory");
//			} else {
//				// just ignore
//			}

			// collate general items
			itemsById = collateItems(files, "internal/");
			metaById = collateItems(files, "external/");
		} else {
			recordIssue("", LogLevel.ERROR, "Application files unavailable");
			//TODO need a way to indicate that appData is 'hollow'
			privileges = Privileges.none();
			itemsById = Collections.emptyMap();
			metaById = Collections.emptyMap();
		}

		// finally, create bundle
		Bundle bundle;
		try {
			bundle = new Bundle(this);
		} catch (IllegalArgumentException e) {
			recordIssue(null, LogLevel.ERROR, e.getMessage());
			//TODO this is ugly
			privileges = Privileges.none();
			itemsById = Collections.emptyMap();
			metaById = Collections.emptyMap();
			bundle = new Bundle(this);
		}
		this.bundle = bundle;
	}

	// private helper methods

	private <T> T open(BundleFiles files, String name, Opener<T> opener, T fallback) {
		if (!files.isName(name)) return null;
		try {
			return opener.open(files.file(name));
		} catch (IOException e) {
			recordIssue(name, 0, LogLevel.ERROR, "failed to read file", e);
			return fallback;
		}
	}

	// root must be empty or include trailing slash
	private Map<String, Item[]> collateItems(BundleFiles files, String root) {
		if (DEBUG) Debug.logging().message("collating items from {}").values(root).log();

		final Map<String, List<Item>> badges = new HashMap<>();
		final Map<String, List<Item>> symbols = new HashMap<>();
		final Map<String, List<Item>> icons = new HashMap<>();
		final Map<String, List<Item>> pictures = new HashMap<>();
		final Map<String, List<Item>> general = new HashMap<>();

		final Function<Map<String, List<Item>>, String> collName = selected -> {
			if (selected == badges  ) return "badges"  ;
			if (selected == symbols ) return "symbols" ;
			if (selected == icons   ) return "icons"   ;
			if (selected == pictures) return "pictures";
			if (selected == general ) return "general" ;
			return "unknown";
		};
		//TODO in future, support documents as resources
		int rootLength = root.length();
		URI baseUri = URI.create("internal://app/" + instanceId + "/");
		files.names().filter(name -> name.startsWith(root)).forEach(name -> {
			String subName = name.substring(rootLength);
			String[] parts = subName.split("/");
			if (parts.length == 1) {
				// special case, can generate many items
				String ext = extension(subName);
				if (!ITEM_EXTS.contains(ext)) {
					// ignore unknown extensions
					recordIssue(name, LogLevel.WARNING, "ignoring file with unsupported extension");
					return; // early return
				}
				Qualifier qualifier = qualifier(parts);
				BundleFile file = files.file(name);
				try {
					switch (ext) {
					case EXT_ITEMS:
						processItems(general, qualifier, file);
						return;
					case EXT_CSV:
						processCSV(general, qualifier, file);
						return;
					}
				} catch (IOException e) {
					recordIssue(name, LogLevel.ERROR, "error processing items file");
					return; // early return, no items
				}

				return; // early return, we've already added the items
			} else {
				// classify according to top level path
				Map<String, List<Item>> selected;
				Item item;
				String id;

				String filename = parts[parts.length - 1];
				switch (parts[0]) {
				case DIR_BADGES: {
					selected = badges;
					item = qualifiedBuilder(parts).addExtra("gosper:badge",Value.ofImage(resolved(baseUri, name))).build();
					id = id(filename);
					break;
				}
				case DIR_SYMBOLS: {
					selected = symbols;
					item = qualifiedBuilder(parts).addExtra("gosper:symbol",Value.ofImage(resolved(baseUri, name))).build();
					id = id(filename);
					break;
				}
				case DIR_ICONS: {
					selected = icons;
					item = qualifiedBuilder(parts).icon(resolved(baseUri, name)).build();
					id = id(filename);
					break;
				}
				case DIR_PICTURES : {
					selected = pictures;
					item = qualifiedBuilder(parts).picture(resolved(baseUri, name)).build();
					id = id(filename);
					break;
				}
				default:
					selected = null;
					item = null;
					id = null;
				}
				if (id != null) {
					if (DEBUG) Debug.logging().message("identified {} item with id {} {}").values(collName.apply(selected), id, item).log();
					selected.computeIfAbsent(id, x -> new ArrayList<>()).add(item);
				}
			}
		});

		List<Map<String, List<Item>>> collections = Arrays.asList(general, badges, symbols, icons, pictures);
		if (DEBUG) {
			for (Map<String,List<Item>> collection : collections) {
				Debug.logging().message("collation collection {} for {}").values(collName.apply(collection), root).log();
				collection.forEach((k,v) -> {
					Debug.logging().message("{} {}").values(k, v).log();
				});
			}
		}
		// collect like items
		Map<String, List<Item>> combined = new HashMap<>();
		for (Map<String,List<Item>> derived : collections) {
			for (Entry<String, List<Item>> entry : derived.entrySet()) {
				List<Item> list = combined.get(entry.getKey());
				if (list == null) {
					combined.put(entry.getKey(), new ArrayList<>(entry.getValue()));
				} else {
					list.addAll(entry.getValue());
				}
			}
		}
		// create sorted item arrays
		Map<String, Item[]> itemsById = new HashMap<>();
		for (Entry<String, List<Item>> entry : combined.entrySet()) {
			List<Item> list = entry.getValue();
			Item[] items = (Item[]) list.toArray(new Item[list.size()]);
			Arrays.sort(items, Comparator.comparing(Item::qualifier));
			itemsById.put(entry.getKey(), items);
		}
		if (DEBUG) {
			Debug.logging().message("fully collated items for {}").values(root).log();
			itemsById.forEach((k,v) -> {
				Debug.logging().message("{} {}").values(k, v).log();
			});
		}
		return itemsById;
	}

	private void processItems(Map<String, List<Item>> general, Qualifier qualifier, BundleFile file) throws IOException {
		String name = file.name();
		//TODO need a way to track line number
		winnow(file.openAsStream()).forEachOrdered(line -> {
			Item.Builder builder = Item.newBuilder().qualifyWith(qualifier);
			String id = parseItemLine(name, 0, builder, line);
			if (id == null) return; // cannot record without id
			Item item = builder.build();
			general.computeIfAbsent(id, x -> new ArrayList<>()).add(item);
		});
	}

	private void processCSV(Map<String, List<Item>> general, Qualifier qualifier, BundleFile file) throws IOException {
		CsvParser parser = new CsvParser(csvSettings);
		parser.beginParsing(file.openAsReader());
		Record record;
		while ((record = parser.parseNextRecord()) != null) {
			String id = record.getString("id");
			if (id == null || id.isEmpty()) {
				int lineNumber = (int) parser.getContext().currentLine();
				recordIssue(file.name(), lineNumber, LogLevel.ERROR, "Missing id for item");
				continue;
			}
			id = validIdOrNull(id);
			if (id == null) {
				int lineNumber = (int) parser.getContext().currentLine();
				recordIssue(file.name(), lineNumber, LogLevel.ERROR, "Invalid id for item");
				continue;
			}
			Map<String, String> fields = record.toFieldMap();
			Item item = Item.newBuilder().qualifyWith(qualifier).addMap(fields).build();
			general.computeIfAbsent(id, x -> new ArrayList<>()).add(item);
		}
		parser.stopParsing();
	}

	Qualifier qualifier(String... parts) {
		return qualifiedBuilder(parts).build().qualifier();
	}

	Item.Builder qualifiedBuilder(String... parts) {
		Item.Builder builder = Item.newBuilder();
		// ignore first (classifier) and last (filename) {
		for (int i = 1; i < parts.length - 1; i++) {
			String part = parts[i];
			int j = part.indexOf("__");
			if (j <= 0 || j == part.length() - 2) continue;
			String key = part.substring(0, j);
			String value = part.substring(j + 2);
			switch (key.toLowerCase()) {
			case TAG_LANG :
				builder.qualifyWithLang(value);
				//TODO should identify invalid language tags
				break;
			case TAG_SCREEN :
				try {
					builder.qualifyWithScreen( ScreenClass.valueOf(value.toUpperCase()) );
				} catch (IllegalArgumentException e) {
					recordIssue(String.join("/", parts), LogLevel.ERROR, "Invalid screen qualifier");
				}
				break;
			case TAG_COLOR :
				try {
					builder.qualifyWithColor( ScreenColor.valueOf(value.toUpperCase()) );
				} catch (IllegalArgumentException e) {
					recordIssue(String.join("/", parts), LogLevel.ERROR, "Invalid color qualifier");
				}
				break;
			case TAG_FLAVOR :
				try {
					builder.qualifyWithFlavor( Flavor.valueOf(value.toUpperCase()) );
				} catch (IllegalArgumentException e) {
					recordIssue(String.join("/", parts), LogLevel.ERROR, "Invalid flavor qualifier");
				}
				break;
			}
		}
		return builder;
	}

	// returns id
	private String parseItemLine(String name, int lineNumber, Item.Builder builder, String line) {
		StringBuilder sb = new StringBuilder();
		String id = null;
		String key = null;
		boolean equalled = false; // true indicates equals has been read
		int del = 0; // character that delimits value
		boolean escaped = false; // true indicates previous was escaping backslash
		String value = null; // stores the value associated with the key

		int[] codepoints = line.codePoints().toArray();
		//TODO collapse if/elses into state indicator
		for (int c : codepoints) {
			if (id == null) { // currently processing id
				if (Character.isWhitespace(c)) {
					if (sb.length() == 0) continue; // leading whitespace, ignore and proceed
					// end of id
					id = validIdOrNull(sb.toString()); // record the id
					if (id == null) { // invalid id
						recordIssue(name, lineNumber, LogLevel.ERROR, "Invalid id.");
						return null;
					}
					sb.setLength(0); // reset buffer, ready to accumulate first key
				} else {
					// accumulate character in id
					sb.appendCodePoint(c);
				}
			} else if (key == null) { // currently reading key
				if (Character.isWhitespace(c)) {
					if (sb.length() == 0) continue; // leading whitespace, ignore and proceed
					// end of key
					key = sb.toString();
					sb.setLength(0); // reset buffer, ready to accumulate value
				} else if (c == '=') {
					if (sb.length() == 0) {
						recordIssue(name, lineNumber, LogLevel.ERROR, "Missing property key.");
						return null;
					}
					key = sb.toString();
					sb.setLength(0);
					equalled = true;
				} else {
					// accumulate character in key
					sb.appendCodePoint(c);
				}
			} else if (!equalled) { // currently waiting for key/value delimiter
				if (Character.isWhitespace(c)) continue; // ignore, spacing before equals
				if (c == '=') { // the equals we're looking for
					equalled = true;
				} else { // a character we don't expect
					recordIssue(name, lineNumber, LogLevel.ERROR, "Unexpected character before '='.");
					return null;
				}
			} else if (value == null) { // currently reading value
				if (escaped) {
					int k;
					switch (c) {
					case 'n' : k = '\n'; break;
					case 'r' : k = '\r'; break;
					case 't' : k = '\t'; break;
					default: k = c;
					}
					sb.appendCodePoint(k);
					escaped = false;
				} else if (Character.isWhitespace(c)) {
					if (sb.length() == 0 && del == 0) continue; // leading whitespace, ignore and proceed
					// this is whitespace inside the value (or possibly trailing whitespace - unknown)
					sb.appendCodePoint(c);
				} else if (c == ',') {
					if (del == 0) { // it's an undelimited value, so it terminates here
						trimTrailingWS(sb);
						value = sb.toString();
						sb.setLength(0);
						builder.set(key, value);
						key = null;
						equalled = false;
						value = null;
					} else { // the comma is within a delimited string, just accumulate it
						sb.appendCodePoint(c);
					}
				} else if (c == '"' || c == '\'') {
					if (del == 0) {
						if (sb.length() == 0) { // a leading quote, it's a delimiter
							del = c;
						} else { // it's part of the value
							sb.append(c);
						}
					} else if (del == c) { // a trailing quote
						value = sb.toString();
						sb.setLength(0);
					} else { // inside another delimiter
						sb.appendCodePoint(c);
					}
				} else if (c == '\\') {
					// the next character is escaped
					escaped = true;
				} else {
					// just a character inside the value, accumulate it
					sb.appendCodePoint(c);
				}
			} else { // waiting for possible comma terminator
				if (Character.isWhitespace(c)) continue; // ignore, spacing before comma
				if (c == ',') { // the comma we're looking for
					builder.set(key, value);
					key = null;
					equalled = false;
					value = null;
					del = 0;
				} else { // a character we don't expect
					recordIssue(name, lineNumber, LogLevel.ERROR, "Unexpected character before ','.");
					return null;
				}
			}
		}

		if (escaped) { // line ended mid-escape - we don't support line continuations
			recordIssue(name, lineNumber, LogLevel.ERROR, "Unfinished escape sequence.");
			return null;
		}

		if (id == null) {
			if (sb.length() == 0) {
				// whitespace-only line, nothing to return
				return null;
			}
			id = validIdOrNull(sb.toString());
			if (id == null) { // invalid id
				recordIssue(name, lineNumber, LogLevel.ERROR, "Invalid id.");
				return null;
			}
			return id;
		}

		if (key == null) {
			if (sb.length() == 0) return id; // ended after trailing comma - this is permitted
			// ended part-way through key
			recordIssue(name, lineNumber, LogLevel.ERROR, "Missing value for key.");
			return null;
		}

		if (!equalled) { // ended without value
			recordIssue(name, lineNumber, LogLevel.ERROR, "Missing value for key.");
			return null;
		}

		if (value == null) {
			if (del != 0) { // ended without closing value
				recordIssue(name, lineNumber, LogLevel.ERROR, "Unclosed quoted value.");
				return null;
			}
			trimTrailingWS(sb);
			value = sb.toString();
		}

		// we have a key/value pair left to set
		builder.set(key, value);
		return id;
	}

	private Map<String, String> parseProperties(String path, Iterator<String> lines) {
		Map<String, String> properties = new HashMap<>();
		int lineNo = 0;
		for (Iterator<String> it = lines; it.hasNext();) {
			String line = it.next();
			lineNo ++;
			int len = line.length();
			if (len == 0) continue; // skip blank line
			int beg = 0;
			while ((beg < len) && (line.charAt(beg) <= ' ')) beg ++;
			if (beg == len) continue; // skip whitespace only line
			if (line.charAt(beg) == '#') continue; // skip comment
			int i = line.indexOf('=');
			if (i == -1) {
				recordIssue(path, lineNo, LogLevel.ERROR, "ignored property without '='");
				continue;
			}
			String key;
			try {
				key = unescape( line.substring(beg, i) );
			} catch (IllegalArgumentException e) {
				recordIssue(path, lineNo, LogLevel.ERROR, "invalid property key");
				continue;
			}
			if (key.isEmpty()) {
				recordIssue(path, lineNo, LogLevel.ERROR, "empty property key");
				continue;
			}
			String value;
			try {
				value = unescape( line.substring(i + 1) );
			} catch (IllegalArgumentException e) {
				recordIssue(path, lineNo, LogLevel.ERROR, "invalid property value");
				continue;
			}
			if (properties.containsKey(key)) {
				//TODO log properly including key & value
				recordIssue(path, lineNo, LogLevel.WARNING, "dropping duplicate key value");
			} else {
				properties.put(key, value);
			}
		}
		return properties;
	}

	@SuppressWarnings("unchecked")
	private BundleSettings settingsFromMap(Map<String, String> map) {
		// namespace
		Namespace namespace;
		if (system) {
			namespace = Bundle.NS_GOSPER;
		} else {
			String namespaceStr = map.get(SETTING_NAMESPACE);
			if (namespaceStr == null) throw new IllegalArgumentException("no namespace specified");
			namespace = Namespace.maybe(namespaceStr).orElseThrow(() -> new IllegalArgumentException("invalid namespace"));
			if (namespace.isReserved()) throw new IllegalArgumentException("reserved namespace"); //TODO this may actually be needed
		}
		// language
		BundleLanguage language;
		{
			String lang = map.get(SETTING_LANGUAGE);
			if (lang == null || lang.isEmpty()) {
				language = classLoader == null ? BundleLanguage.JS : BundleLanguage.JAVA;
				logger.debug().message("defaulting bundle language to {}").values(language).log();
			} else {
				try {
					language = BundleLanguage.valueOf(lang.toUpperCase());
				} catch (IllegalArgumentException e) {
					logger.debug().message("unrecognized bundle language detected: {}").values(lang).log();
					throw new IllegalArgumentException("unrecognized bundle language specified");
				}
				if (language == BundleLanguage.JAVA && classLoader == null) {
					logger.debug().message("java bundle language specified without classloader").log();
					throw new IllegalArgumentException("invalid bundle language specified");
				}
			}
		}
		// default settings
		String defaultSettingsId;
		{
			String id = map.get(SETTING_SETTINGS);
			if (id == null) {
				defaultSettingsId = null;
			} else {
				defaultSettingsId = validIdOrNull(id);
				if (defaultSettingsId == null) {
					logger.error().message("ignoring invalid settings id {}").values(id).log();
				}
			}
		}
		// package into settings
		return new BundleSettings(namespace, language, defaultSettingsId);
	}

	private String extractIdProperty(Map<String, String> map, String key, String defaultId) {
		String id = map.get(key);
		if (id == null) {
			if (logger != null) logger.debug().message("no item id set for {}").values(key).log();
		} else {
			id = validIdOrNull(id);
			if (id == null) {
				//TODO need more descriptive logging
				if (logger != null) logger.error().message("invalid item id for property {}").values(key).log();
			}
		}
		if (id == null && defaultId != null) {
			if (logger != null) logger.debug().message("defaulting {} to {}").values(key, defaultId).log();
			id = defaultId;
		}
		return id;
	}

	private String unescape(String str) {
		int i = str.indexOf('\\');
		if (i == -1) return str;
		//TODO need to support unescaping
		throw new UnsupportedOperationException("escaping not supported");
	}

	private Image resolved(URI baseUri, String name) {
		try {
			return new Image(baseUri.resolve(name));
		} catch (IllegalArgumentException e) {
			recordIssue(name, LogLevel.ERROR, "invalid resource URI");
			return null;
		}
	}

	private void recordIssue(String filePath, LogLevel level, String message) {
		logger.at(level).filePath(filePath).message(message).log();
	}

	private void recordIssue(String filePath, int lineNumber, LogLevel level, String message) {
		logger.at(level).filePath(filePath).message(message).lineNumber(lineNumber).log();
	}

	private void recordIssue(String filePath, int lineNumber, LogLevel level, String message, Throwable t) {
		logger.at(level).filePath(filePath).message(message).lineNumber(lineNumber).stacktrace(t).log();
	}

	//TODO define within BundleFile itself?
	private interface Opener<T> {
		T open(BundleFile file) throws IOException;
	}

}
