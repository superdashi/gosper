package com.superdashi.gosper.micro;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.superdashi.gosper.config.Config;
import com.superdashi.gosper.config.Config.ColorConfig;
import com.superdashi.gosper.core.Debug;
import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Qualifier;
import com.superdashi.gosper.item.ScreenClass;
import com.superdashi.gosper.item.ScreenColor;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.logging.Logger;
import com.superdashi.gosper.studio.Typeface;
import com.tomgibara.intgeom.IntDimensions;

public final class Visuals {

	private static final int CONTEXT_CACHE_LIMIT = 30;

	private static final String THEME_SUFFIX = ".theme";
	private static final String METRICS_SUFFIX = ".metrics";
	private static final String UNIVERSAL = "_";

	private static final String TAG_LANG = "lang";
	private static final String TAG_SCREEN = "screen";
	private static final String TAG_COLOR = "color";
	private static final String TAG_FLAVOR = "flavor";

	private static final Pattern linePattern = Pattern.compile("([^=\\s]+)\\s*=\\s*(.*)");
	private static final Pattern dimPattern = Pattern.compile("([1-9][0-9]*)x([1-9][0-9]*)");

	private static final Comparator<Path> pathComparator = (a,b) -> {
		String[] as = a.getFileName().toString().split("\\.");
		String[] bs = b.getFileName().toString().split("\\.");
		int lim = Math.min(as.length - 1, bs.length - 1); // exclude the suffix
		for (int i = 0; i < lim; i++) {
			int c = as[i].compareTo(bs[i]);
			if (c != 0) return c;
		}
		if (as.length == bs.length) return 0;
		return as.length < bs.length ? -1 : 1;
	};

	private static Font font(String name, float size) throws FontFormatException, IOException {
		return Font.createFont(Font.TRUETYPE_FONT, Visuals.class.getResourceAsStream("/fonts/" + name)).deriveFont(size);
	}

	private static int sourceSansProSize = -1;
	private static Typeface sourceSansPro = null;

	private static int interUISize = -1;
	private static Typeface interUI = null;

	//TODO this belongs elsewhere
	private static Typeface sourceSansPro(int size) {
		if (sourceSansProSize != size) {
			try {
				sourceSansPro = Typeface.fromFonts(
						font("SourceSansPro-Regular.ttf", size),
						font("SourceSansPro-Bold.ttf", size),
						font("SourceSansPro-Italic.ttf", size),
						font("SourceSansPro-BoldItalic.ttf", size)
						);
				sourceSansProSize = size;
			} catch (IOException | FontFormatException e) {
				Debug.logger().error().message("failed to load font-based typeface").stacktrace(e).log();
				sourceSansPro = Typeface.systemDefault(size);
			}
		}
		return sourceSansPro;
	}

	//TODO so does this
	private static Typeface interUI(int size) {
		if (interUISize != size) {
			try {
				interUI = Typeface.fromFonts(
						font("Inter-UI-Regular.ttf", size),
						font("Inter-UI-Bold.ttf", size),
						font("Inter-UI-Italic.ttf", size),
						font("Inter-UI-BoldItalic.ttf", size)
						);
				interUISize = size;
			} catch (IOException | FontFormatException e) {
				Debug.logger().error().message("failed to load font-based typeface").stacktrace(e).log();
				interUI = Typeface.systemDefault(size);
			}
		}
		return interUI;
	}

	private static final Map<Qualifier, VisualSpec> specCache = new LinkedHashMap<Qualifier, VisualSpec>(30, 0.75f, true) {
		protected boolean removeEldestEntry(Map.Entry<Qualifier,VisualSpec> eldest) {
			return size() > CONTEXT_CACHE_LIMIT;
		}
	};

	static final String AMB_COLOR = "ambient-color";
	static final String CNT_BG_COLOR = "content-background-color";
	static final String ACT_COLOR = "action-color";
	static final String CTL_COLOR = "control-color";
	static final String TXT_COLOR = "textual-color";
	static final String BAR_BG_COLOR = "bar-background-color";
	static final String BAR_TXT_COLOR = "bar-text-color";
	static final String IND_COLOR = "indicator-color";
	static final String BTN_BG_COLOR = "button-background-color";
	static final String BTN_TXT_COLOR = "button-text-color";
	static final String INF_TXT_COLOR = "info-text-color";
	static final String INF_BG_COLOR = "info-background-color";
	static final String KEY_ACT_COLOR = "key-active-color";
	static final String CAR_PAS_COLOR = "caret-passive-color";
	static final String CAR_ACT_COLOR = "caret-active-color";
	static final String BACKGROUND = "background";
	static final String KBD_BACKGROUND = "keyboard-background";
	static final String ELLIPSIS_TEXT = "ellipsis-text";
	static final String TYPEFACE_NAME = "typeface-name";

	static final String BAR_HEIGHT = "bar-height";
	static final String BAR_BASE_GAP = "bar-baseline-gap";
	static final String BAR_IND_GAP = "bar-indicator-gap";
	static final String BAR_BTN_GAP = "bar-button-gap";
	static final String BAR_GAP = "bar-gap";
	static final String SIDE_MARGIN = "side-margin";
	static final String IND_GAP = "indicator-gap";
	static final String SCR_WIDTH = "scrollbar-width";
	static final String SCR_MIN_HEIGHT = "scrollbar-minimum-height";
	static final String CMP_MARGIN = "component-margin";
	static final String TBL_ROW_HEIGHT = "table-row-height";
	static final String BDG_SIZE = "badge-size";
	static final String BTN_HEIGHT = "button-height";
	static final String BTN_BASE_GAP = "button-baseline-gap";
	static final String BTN_GAP = "button-gap";
	static final String LINE_HEIGHT = "line-height";
	static final String ICN_SIZE = "icon-size";
	static final String ICN_GAP = "icon-gap";
	static final String ICN_BAR_GAP = "icon-bar-gap";
	static final String ICN_BAR_WIDTH = "icon-bar-width";
	static final String SYM_SIZE = "symbol-size";
	static final String TYPEFACE_SIZE = "typeface-size";

	static final String COLOR_FG = "foreground-color";
	static final String COLOR_BG = "background-color";

	static final String STYLES = "styles";

	static final String STYLE_BADGE    = "badge"   ;
	static final String STYLE_BUTTON   = "button"  ;
	static final String STYLE_CARD     = "card"    ;
	static final String STYLE_DOCUMENT = "document";
	static final String STYLE_PLACE    = "place"   ;

	static final String TYPEFACE_PIX = "pixel";
	static final String TYPEFACE_EZO = "ezo";
	static final String TYPEFACE_IOT = "iota";
	static final String TYPEFACE_IUI = "interui";
	static final String TYPEFACE_SSP = "sourcesanspro";
	static final String TYPEFACE_SYS = "system";

	static Typeface typeface(String name, int size) {
		switch (name) {
		case TYPEFACE_PIX : return size < 12 ? Typeface.ezo() : Typeface.iota();
		case TYPEFACE_EZO : return Typeface.ezo(); // only one size
		case TYPEFACE_IOT : return Typeface.iota(); // only one size
		case TYPEFACE_SSP : return sourceSansPro(size);
		case TYPEFACE_IUI : return interUI(size);
		case TYPEFACE_SYS : return Typeface.systemDefault(size);
		default:
			//TODO need the logger
			//logger.warning().message("unsupported typeface: {}").values(value).lineNumber(lineNo).filePath(path).log();
			return Typeface.systemDefault(size);
		}
	}
	static int extractColor(Map<String, Object> map, String name) {
		ColorConfig config = (ColorConfig) map.get(name);
		if (config == null) throw new IllegalArgumentException("no valid value configured for property " + name);
		return config.asInt();
	}

	static <V> V extract(Map<String, Object> map, String name) {
		V config = (V) map.get(name);
		if (config == null) throw new IllegalArgumentException("no valid value configured for property " + name);
		return config;
	}

	private final Logger logger;
	private final Path dir;
	//TODO use collect
	private final Map<VisualQualifier, VisualTheme> themes = new HashMap<>();
	private final Map<VisualQualifier, VisualMetrics> metrics = new HashMap<>();

	public Visuals(Logger logger, Path dir) {
		if (logger == null) throw new IllegalArgumentException("null logger");
		if (dir == null) throw new IllegalArgumentException("null dir");
		this.logger = logger;
		this.dir = dir;


		process("metrics", METRICS_SUFFIX, this::processMetrics);
		process("themes" , THEME_SUFFIX  , this::processTheme);

		if (metrics.isEmpty()) {
			logger.warning().message("no metrics").filePath(dir).log();
		}

		if (themes.isEmpty()) {
			logger.error().message("no themes").filePath(dir).log();
		}
	}

	// qualifier must be fully specified
	Optional<VisualSpec> contextFor(Qualifier qualifier, IntDimensions screenDimensions, boolean screenOpaque) {
		assert qualifier.isFullySpecified();
		synchronized (specCache) {
			VisualSpec spec = specCache.get(qualifier);
			if (spec != null) {
				logger.debug().message("returning cached visual context for qualifier {}").values(qualifier).log();
				return Optional.of(spec);
			}
		}
		VisualQualifier vq = new VisualQualifier(qualifier, screenDimensions);
		logger.debug().message("finding visual theme for qualifier {}").values(qualifier).log();
		Optional<VisualTheme> optTheme = bestParamsFor(vq, themes);
		if (!optTheme.isPresent()) {
			logger.warning().message("no theme matching screen color {}").values(qualifier.color).log();
			return Optional.empty();
		}
		logger.debug().message("finding visual metrics for qualifier {}").values(qualifier).log();
		Optional<VisualMetrics> optMetrics = bestParamsFor(vq, metrics);
		if (!optMetrics.isPresent()) {
			logger.warning().message("no metrics matching visual qualifier {}").values(vq).log();
			return Optional.empty();
		}
		synchronized (specCache) {
			logger.debug().message("creating new visual context for qualifier {}").values(qualifier).log();
			VisualSpec spec = VisualSpec.create(vq, screenOpaque, optTheme.get(), optMetrics.get());
			specCache.put(qualifier, spec);
			return Optional.of(spec);
		}
	}

	private <V> void process(String name, String suffix, Consumer<Path> consumer) {
		try {
			Files.find(dir, 1, (p,a) -> p.getFileName().toString().endsWith(suffix)).sorted(pathComparator).forEachOrdered(consumer::accept);
		} catch (IOException e) {
			logger.error().message("failed to scan visuals directory for {}").values(name).filePath(dir).log();
		}
	}

	private void processTheme(Path path) {
		process("theme", path, VisualTheme::new, themes);
	}

	private void processMetrics(Path path) {
		process("metrics", path, VisualMetrics::new, metrics);
	}

	private <V extends VisualParams> void process(String name, Path path, Function<Map<String, Object>, V> generator, Map<VisualQualifier, V> record) {
		logger.debug().message("processing {} file").values(name).filePath(path).log();
		// split the filename
		String filename = path.getFileName().toString();
		String[] parts = filename.split("\\.");
		assert parts.length > 1;
		// try to identify an ancestor
		Map<String, Object> defaults;
		{
			V ancestor = null;
			VisualQualifier qualifier = null;
			for (int i = parts.length - 2; i > 0; i--) {
				qualifier = qualifier(parts, 0, i, null);
				ancestor = record.get(qualifier);
				if (ancestor != null) break;
			}
			logger.debug().message("ancestor qualifier {}").values(ancestor == null ? "none" : qualifier).log();
			defaults = ancestor == null ? Collections.emptyMap() : ancestor.map;
		}
		// obtain the qualifier
		VisualQualifier q = qualifier(filename);
		if (q == null) return;
		// create the metrics from the file
		V params = create(path, defaults, generator);
		if (params == null) return;
		// record the metrics
		record.put(q, params);
	}

	private VisualQualifier qualifier(String filename) {
		String[] parts = filename.split("\\.");
		assert parts.length > 1;
		return qualifier(parts, 0, parts.length - 1, filename);
	}

	//TODO want to somehow share common code with collator
	private VisualQualifier qualifier(String[] parts, int from, int to, String filename) {
		assert from != to;
		if (to - from == 1 && parts[from].equals(UNIVERSAL)) return VisualQualifier.universal();
		Qualifier qualifier = Qualifier.universal();
		IntDimensions dimensions = IntDimensions.NOTHING;
		for (int i = from; i < to; i++) {
			String part = parts[i];
			if (part.equals("_")) {
				// ignore universal
				continue;
			}
			int j = part.indexOf("__");
			if (j != -1) {
				// possible qualifier
				if (j == 0 || j == part.length() - 2) {
					if (filename != null) logger.warning().message("ignoring invalid segment in filename {}").values(filename).log();
					continue;
				}
				String key = part.substring(0, j);
				String value = part.substring(j + 2);
				switch(key.toLowerCase()) {
				case TAG_LANG:
					qualifier = qualifier.withLang(value);
					//TODO should identify invalid language tags
					break;
				case TAG_SCREEN :
					try {
						qualifier = qualifier.withScreen( ScreenClass.valueOf(value.toUpperCase()) );
					} catch (IllegalArgumentException e) {
						if (filename != null) logger.warning().message("ignoring invalid screen qualifier in filename {}").values(filename).log();
						continue;
					}
					break;
				case TAG_COLOR :
					try {
						qualifier = qualifier.withColor( ScreenColor.valueOf(value.toUpperCase()) );
					} catch (IllegalArgumentException e) {
						if (filename != null) logger.warning().message("ignoring invalid color qualifier in filename {}").values(filename).log();
						continue;
					}
					break;
				case TAG_FLAVOR :
					try {
						qualifier = qualifier.withFlavor( Flavor.valueOf(value.toUpperCase()) );
					} catch (IllegalArgumentException e) {
						if (filename != null) logger.warning().message("ignoring invalid flavor qualifier in filename {}").values(filename).log();
						continue;
					}
					break;
				}
			} else {
				// possible dimensions
				Matcher matcher = dimPattern.matcher(part);
				if (!matcher.matches()) {
					if (filename != null) logger.warning().message("ignoring invalid segment in filename {}").values(filename).log();
					continue;
				}
				if (!dimensions.isNothing()) {
					if (filename != null) logger.warning().message("ignoring second dimensions in filename {}").values(filename).log();
					continue;
				}
				int w = Integer.valueOf(matcher.group(1));
				int h = Integer.valueOf(matcher.group(2));
				dimensions = IntDimensions.of(w, h);
			}
		}
		return new VisualQualifier(qualifier, dimensions);
	}

	private <V> V create(Path path, Map<String, Object> defaults, Function<Map<String, Object>, V> generator) {
		List<String> lines;
		try {
			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			logger.error().message("failed to read visuals file").filePath(path).stacktrace(e).log();
			return null;
		}
		return create(lines, path, defaults, generator);
	}

	private <V> V create(List<String> lines, Path path, Map<String, Object> defaults, Function<Map<String, Object>, V> generator) {
		if (lines == null) throw new IllegalArgumentException("null lines");
		Map<String, Object> map = new HashMap<>(defaults);
		Map<String, Style> styles = new HashMap<>();
		{
			VisualStyles vs = (VisualStyles) defaults.get(STYLES);
			if (vs != null) {
				styles.put(STYLE_BADGE   , vs.defaultBadgeStyle   .mutableCopy());
				styles.put(STYLE_BUTTON  , vs.defaultButtonStyle  .mutableCopy());
				styles.put(STYLE_CARD    , vs.defaultCardStyle    .mutableCopy());
				styles.put(STYLE_DOCUMENT, vs.defaultDocumentStyle.mutableCopy());
				styles.put(STYLE_PLACE   , vs.defaultPlaceStyle   .mutableCopy());
			}
		}
		int lineNo = 0;
		for (String line : lines) {
			line = line.trim();
			lineNo++;
			if (line.isEmpty() || line.charAt(0) == '#') continue;
			Matcher matcher = linePattern.matcher(line);
			if (!matcher.matches()) {
				logger.warning().message("invalid line").lineNumber(lineNo).filePath(path).log();
				continue;
			}
			String name = matcher.group(1);
			String value = matcher.group(2);
			if (value.isEmpty()) continue;
			int i = name.indexOf('.');
			if (i == -1) {
				try {
					switch(name) {
					case AMB_COLOR:
					case CNT_BG_COLOR:
					case ACT_COLOR:
					case CTL_COLOR:
					case TXT_COLOR:
					case BAR_BG_COLOR:
					case BAR_TXT_COLOR:
					case IND_COLOR:
					case BTN_BG_COLOR:
					case BTN_TXT_COLOR:
					case INF_TXT_COLOR:
					case INF_BG_COLOR:
					case KEY_ACT_COLOR:
					case CAR_PAS_COLOR:
					case CAR_ACT_COLOR:
						map.put(name, Config.ColorConfig.parse(value));
						break;
					case ELLIPSIS_TEXT:
					case TYPEFACE_NAME:
						map.put(name, value);
						break;
					case KBD_BACKGROUND:
					case BACKGROUND:
						map.put(name, Background.coloring(Config.ColoringConfig.parse(value).asColoring()));
						break;
					case BAR_HEIGHT:
					case SCR_WIDTH:
					case SCR_MIN_HEIGHT:
					case TBL_ROW_HEIGHT:
					case BDG_SIZE:
					case BTN_HEIGHT:
					case LINE_HEIGHT:
					case ICN_SIZE:
					case SYM_SIZE:
					case ICN_BAR_WIDTH:
					case TYPEFACE_SIZE:
						int size = Integer.parseInt(value);
						if (size <= 0) throw new IllegalArgumentException();
						map.put(name, size);
						break;
					case BAR_BASE_GAP:
					case BAR_IND_GAP:
					case BAR_BTN_GAP:
					case BAR_GAP:
					case SIDE_MARGIN:
					case IND_GAP:
					case CMP_MARGIN:
					case BTN_BASE_GAP:
					case BTN_GAP:
					case ICN_GAP:
					case ICN_BAR_GAP:
						int gap = Integer.parseInt(value);
						if (gap < 0) throw new IllegalArgumentException();
						map.put(name, gap);
						break;
					default:
						logger.warning().message("unknown property: {}").values(name).lineNumber(lineNo).filePath(path).log();
						continue;
					}
				} catch (IllegalArgumentException e) {
					logger.warning().message("invalid value {} for property {}: {}").values(value, name, e.getMessage()).lineNumber(lineNo).filePath(path).log();
					continue;
				}
			} else {
				String style = name.substring(0, i);
				String property = name.substring(i + 1);
				Style s;
				switch (style) {
				case STYLE_BADGE:
				case STYLE_BUTTON:
				case STYLE_CARD:
				case STYLE_DOCUMENT:
				case STYLE_PLACE:
					s = styles.computeIfAbsent(style, k -> new Style());
					break;
					default:
						logger.warning().message("unknown style: {}").values(style).lineNumber(lineNo).filePath(path).log();
						continue;
				}
				try {
					switch(property) {
					case COLOR_BG:
						s.colorBg(Config.ColorConfig.parse(value).asInt());
						break;
					case COLOR_FG:
						s.colorFg(Config.ColorConfig.parse(value).asInt());
						break;
					}
				} catch (IllegalArgumentException e) {
					logger.warning().message("invalid value {} for style property{}.{}: {}").values(value, style, property, e.getMessage()).lineNumber(lineNo).filePath(path).log();
					continue;
				}
			}
			map.put(STYLES, new VisualStyles(styles));
		}
		try {
			return generator.apply(map);
		} catch (IllegalArgumentException e) {
			logger.error().message(e.getMessage()).filePath(path).log();
			return null;
		}
	}

	private <V extends VisualParams> Optional<V> bestParamsFor(VisualQualifier qualifier, Map<VisualQualifier, V> params) {
		return params.entrySet().stream().filter(e -> e.getKey().matches(qualifier)).reduce((a,b) -> a.getKey().matches(b.getKey()) ? b : a).map(e -> {
			logger.debug().message("selected visual parameters with qualifier {}").values(e.getKey()).log();
			return e.getValue();
		});
	}
}
