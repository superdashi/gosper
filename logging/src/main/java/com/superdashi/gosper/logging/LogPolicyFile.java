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
package com.superdashi.gosper.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.tomgibara.fundament.Bijection;
import com.tomgibara.tries.IndexedTrie;
import com.tomgibara.tries.IndexedTries;
import com.tomgibara.tries.Tries;

public final class LogPolicyFile implements LogDomain.Policy {

	private static final boolean DEBUG = "true".equalsIgnoreCase(System.getProperty("com.superdashi.gosper.logging.LogPolicyFile.DEBUG"));

	private static final boolean DEFAULT_IDENTITY_VALID = true;

	private static final Pattern DEFAULT_PATTERN = null;
	private static final LogLevel DEFAULT_LOG_LEVEL = LogLevel.INFO; //TODO should be warn?
	private static final int DEFAULT_INITIAL_QUEUE_SIZE = 8;
	private static final int DEFAULT_MAX_QUEUE_SIZE = 256;

	// not private, for testing
	static IndexedTries<LogIdentity> tries = Tries.serialStrings(StandardCharsets.UTF_8).adaptedWith(new Bijection<String, LogIdentity>() {
		@Override public Class<String>      domainType() { return String.class     ; }
		@Override public Class<LogIdentity> rangeType()  { return LogIdentity.class; }
		@Override public LogIdentity apply   (String      s) { return LogIdentity.fromString(s); }
		@Override public String      disapply(LogIdentity i) { return i.toString()             ; }
	}).indexed();

	private final Path path;
	private long lastModified = -1L;

	private volatile Settings settings = null;

	public LogPolicyFile(Path path) {
		if (path == null) throw new IllegalArgumentException("null path");
		this.path = path;
		// initial configuration
		attemptUpdate(true);
	}

	// public methods

	public boolean update() {
		return attemptUpdate(true);
	}

	public boolean poll() {
		return attemptUpdate(false);
	}

	// policy methods

	@Override
	public boolean identityValid(LogIdentity identity) {
		Settings settings = this.settings;
		boolean value = settings == null ? DEFAULT_IDENTITY_VALID : settings.identityValid(identity);
		if (DEBUG) output(identity, "valid", value);
		return value;
	}

	@Override
	public int initialQueueSize(LogIdentity identity) {
		Settings settings = this.settings;
		int value = settings == null ? DEFAULT_INITIAL_QUEUE_SIZE : settings.initialQueueSize(identity);
		if (DEBUG) output(identity, "initial queue size", value);
		return value;
	}

	@Override
	public int maxQueueSize(LogIdentity identity) {
		Settings settings = this.settings;
		int value = settings == null ? DEFAULT_MAX_QUEUE_SIZE : settings.maxQueueSize(identity);
		if (DEBUG) output(identity, "max queue size", value);
		return value;
	}

	@Override
	public LogLevel logLevel(LogIdentity identity) {
		Settings settings = this.settings;
		LogLevel value = settings == null ? DEFAULT_LOG_LEVEL : settings.logLevel(identity);
		if (DEBUG) output(identity, "log level", value);
		return value;
	}

	private boolean attemptUpdate(boolean force) {
		try {
			boolean exists = Files.exists(path);
			if (exists) {
				long lastModified = Files.getLastModifiedTime(path).toMillis();
				if (!force && lastModified == this.lastModified) return false; // don't update, file hasn't changed
				settings = new Settings(path);
				this.lastModified = lastModified;
			} else {
				lastModified = -1L;
				settings = null;
			}
			return true;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return false;
		}
	}

	private void output(LogIdentity identity, String name, Object value) {
		System.out.println(identity + " " + name +  " is " + value);
	}

	private static final class Settings {

		private static Pattern property = Pattern.compile("([^=\\s]+)\\s*=\\s*(.+)");

		private final LogLevel level;
		private final Pattern pattern;
		private final int initialSize;
		private final int maxSize;

		private final IndexedTrie<LogIdentity> trie;

		private final LogLevel[] levels;
		private final Pattern[] patterns;
		private final int[] initialSizes;
		private final int[] maxSizes;

		Settings(Path path) throws IOException {
			List<String> lines = Files.readAllLines(path);

			Map<LogIdentity, LogLevel> levelMap = new HashMap<>();
			Map<LogIdentity, Pattern> patternMap = new HashMap<>();
			Map<LogIdentity, Integer> initialSizeMap = new HashMap<>();
			Map<LogIdentity, Integer> maxSizeMap = new HashMap<>();

			levelMap.put(null, DEFAULT_LOG_LEVEL);
			patternMap.put(null, DEFAULT_PATTERN);
			initialSizeMap.put(null, DEFAULT_INITIAL_QUEUE_SIZE);
			maxSizeMap.put(null, DEFAULT_MAX_QUEUE_SIZE);

			int lineNo = 0;
			for (String line : lines) {
				line = line.trim();
				lineNo++;
				if (line.isEmpty() || line.startsWith("#")) continue;
				Matcher matcher = property.matcher(line);
				if (!matcher.matches()) {
					System.err.println("invalid logging directive " +path + ":" + lineNo);
					continue;
				}
				String name = matcher.group(1);
				String value = matcher.group(2);

				Setting setting;
				try {
					setting = new Setting(name);
				} catch (IllegalArgumentException e) {
					System.err.println("invalid log identity " +path + ":" + lineNo);
					continue;
				}

				LogIdentity identity = setting.identity;
				switch (setting.name) {
				case "level":
					LogLevel level;
					try {
						level = LogLevel.valueOf(value.toUpperCase());
					} catch (IllegalArgumentException e) {
						System.err.println("invalid log level " +path + ":" + lineNo);
						continue;
					}
					levelMap.put(identity, level);
					break;
				case "pattern":
					Pattern pattern;
					try {
						pattern = value.isEmpty() ? null : Pattern.compile(value);
					} catch (IllegalArgumentException e) {
						System.err.println("invalid pattern " +path + ":" + lineNo);
						continue;
					}
					patternMap.put(identity, pattern);
					continue;
				case "initialQueueSize":
					int initialSize;
					try {
						initialSize = Integer.parseInt(value);
						if (initialSize <= 0) throw new IllegalArgumentException();
					} catch (IllegalArgumentException e) {
						System.err.println("invalid initialQueueSize " +path + ":" + lineNo);
						continue;
					}
					initialSizeMap.put(identity, initialSize);
					continue;
				case "maxQueueSize":
					int maxSize;
					try {
						maxSize = Integer.parseInt(value);
						if (maxSize <= 0) throw new IllegalArgumentException();
					} catch (IllegalArgumentException e) {
						System.err.println("invalid maxQueueSize " +path + ":" + lineNo);
						continue;
					}
					maxSizeMap.put(identity, maxSize);
					continue;
				}
			}

			level       = levelMap      .remove(null);
			pattern     = patternMap    .remove(null);
			initialSize = initialSizeMap.remove(null);
			maxSize     = maxSizeMap    .remove(null);

			Set<LogIdentity> set = new HashSet<>();
			set.addAll(levelMap      .keySet());
			set.addAll(patternMap    .keySet());
			set.addAll(initialSizeMap.keySet());
			set.addAll(maxSizeMap    .keySet());

			IndexedTrie<LogIdentity> trie = tries.newTrie();
			trie.addAll(set);
			trie.compactStorage();
			this.trie = trie.immutable();

			int size     = trie.size();
			levels       = new LogLevel[size];
			patterns     = new Pattern [size];
			initialSizes = new int     [size];
			maxSizes     = new int     [size];

			int index = 0;
			for (LogIdentity identity : trie) {
				levels[index]       = levelMap      .get         (identity   );
				patterns[index]     = patternMap    .get         (identity   );
				initialSizes[index] = initialSizeMap.getOrDefault(identity, 0);
				maxSizes[index]     = maxSizeMap    .getOrDefault(identity, 0);
				index ++;
			}
		}

		boolean identityValid(LogIdentity identity) {
			synchronized (trie) {
				int index = trie.indexOf(identity);
				Pattern pattern = index < 0 ? null : patterns[index];
				if (pattern == null) {
					pattern = this.pattern;
					Iterator<LogIdentity> i = trie.ancestors(identity);
					while(i.hasNext()) {
						Pattern p = patterns[ trie.indexOf(i.next()) ];
						if (p != null) pattern = p;
					}
				}
				return pattern == null || pattern.matcher(identity.toString()).matches();
			}
		}

		int initialQueueSize(LogIdentity identity) {
			synchronized (trie) {
				int index = trie.indexOf(identity);
				int initialSize = index < 0 ? 0 : initialSizes[index];
				if (initialSize == 0) {
					initialSize = this.initialSize;
					Iterator<LogIdentity> i = trie.ancestors(identity);
					while(i.hasNext()) {
						int s = initialSizes[ trie.indexOf(i.next()) ];
						if (s > 0) initialSize = s;
					}
				}
				return initialSize;
			}
		}

		int maxQueueSize(LogIdentity identity) {
			synchronized (trie) {
				int index = trie.indexOf(identity);
				int maxSize = index < 0 ? 0 : maxSizes[index];
				if (maxSize == 0) {
					maxSize = this.maxSize;
					Iterator<LogIdentity> i = trie.ancestors(identity);
					while(i.hasNext()) {
						int s = maxSizes[ trie.indexOf(i.next()) ];
						if (s > 0) maxSize = s;
					}
				}
				return maxSize;
			}
		}

		LogLevel logLevel(LogIdentity identity) {
			synchronized (trie) {
				int index = trie.indexOf(identity);
				LogLevel level = index < 0 ? null : levels[index];
				if (level == null) {
					level = this.level;
					Iterator<LogIdentity> i = trie.ancestors(identity);
					while(i.hasNext()) {
						LogLevel l = levels[ trie.indexOf(i.next()) ];
						if (l != null) level = l;
					}
				}
				return level;
			}
		}

		private static class Setting {

			String name;
			LogIdentity identity;

			public Setting(String str) {
				int i = str.indexOf(".");
				if (i == -1) {
					name = str;
					identity = null;
				} else {
					name = str.substring(0, i);
					identity = LogIdentity.fromString(str.substring(i + 1));
				}
			}
		}
	}
}
