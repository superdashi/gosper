package com.superdashi.gosper.control;

import java.util.Random;

import com.superdashi.gosper.color.Coloring;
import com.superdashi.gosper.color.Palette.LogicalColor;
import com.superdashi.gosper.config.ConfigMatchers;
import com.superdashi.gosper.config.ConfigProperty;
import com.superdashi.gosper.config.ConfigRule;
import com.superdashi.gosper.config.ConfigRules;
import com.superdashi.gosper.config.ConfigSelectors;
import com.superdashi.gosper.config.Configuration;
import com.superdashi.gosper.config.Configurator;
import com.superdashi.gosper.core.Background;
import com.superdashi.gosper.core.BackgroundConfig;
import com.superdashi.gosper.core.DashiLog;
import com.superdashi.gosper.core.Design;
import com.superdashi.gosper.core.InfoAcquirer;
import com.superdashi.gosper.data.DataTier;
import com.superdashi.gosper.display.Console;
import com.superdashi.gosper.display.Console.Window;
import com.superdashi.gosper.display.ConsoleDisplay;
import com.superdashi.gosper.display.DisplayConduit;
import com.superdashi.gosper.display.DisplayContext;
import com.superdashi.gosper.display.DisplayListener;
import com.superdashi.gosper.display.PictureBackgroundDisplay;
import com.superdashi.gosper.display.Wrap;
import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.layout.Alignment;
import com.superdashi.gosper.layout.Alignment2D;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Position.Fit;
import com.tomgibara.intgeom.IntRect;

class Controller implements DisplayListener {

	private final DataTier dataTier;
	private final DisplayConduit conduit;
	private DisplayContext context = null;

	private Design design = null;

	// test only
	Random crand = new Random();
	int count = 0;
	int charz = ' ';
	Window win1;
	Window win2;
	private Console console;
	private LogicalColor randCol() { return LogicalColor.valueOf(crand.nextInt(16)); }

	Controller(DataTier dataTier, DisplayConduit conduit) {
		this.dataTier = dataTier;
		this.conduit = conduit;
		conduit.setDisplayListener(this);
	}

	@Override
	public void displayStarted() {
		DashiLog.debug("display started");
		loadDesign();
	}

	@Override
	public void displayStopped() {
		DashiLog.debug("display stopped");
	}

	@Override
	public void newContext(DisplayContext context) {
		DashiLog.debug("display context");
		if (context != null) detachFromContext();
		this.context = context;
		attachToContext();
	}

	@Override
	public void update() {
		conduit.sync(this::updateConsole);
	}

	// contextualizing methods
	// TODO assemble into a ControlContext

	DisplayContext context() {
		if (context == null) throw new IllegalStateException("no context");
		return context;
	}

	//TODO what to do if dataTier or context not available?
	Inspector inspector(Details details) {
		return dataTier.dataContext(details.identity()).get();
	}
	// private helper methods

	private void attachToContext() {
		conduit.setDesign(design);
		Background background = design.getBackground();
		if (background != null) {
			InfoAcquirer acq = background.infoAcquirer();
			//TODO if empty acquirer, need to switch background display
			// note - this kills idea of factory inside display tier, since controller is tied to display type
			Inspector inspector = inspector(acq.details());
			PictureBackgroundDisplayController pbdc = new PictureBackgroundDisplayController(this, inspector, acq);
			BackgroundConfig style = background.style;
			PictureBackgroundDisplay display = new PictureBackgroundDisplay(pbdc, Coloring.flat(0xff000000), style.transDuration / 1000f, style.showDuration / 1000f);
			conduit.addDisplay(display);
		}
		// test consoles
		console = new Console(40, 20, context.getDefaultCharMap());
		console.createWindow(IntRect.rectangle(10, 5, 20, 10)).setBlank('X').setColors(LogicalColor.LGH_INF, LogicalColor.DRK_BCK).fill();
		win1 = console.createWindow(IntRect.bounded(2, 2, console.cols / 2 - 1, console.rows - 2)).setBlank(' ');
		win2 = console.createWindow(IntRect.bounded(console.cols / 2 + 1, 2, console.cols - 2, console.rows - 2)).setBlank(' ');
		ConsoleDisplay display = new ConsoleDisplay(console, context.getDashPlane().rect, 0f, Position.from(Fit.FREE, Fit.MATCH, Alignment2D.pair(Alignment.MIN, Alignment.MID)), Wrap.wraps(Wrap.MIRROR, Wrap.CLAMP), null, Coloring.BLACK_COLORING);
		conduit.addDisplay(display);
	}

	private void detachFromContext() {
		console = null;
	}

	private void loadDesign() {
//		Styling palette = StyleProperty.forName("palette").get().parse("#000000,#ffffff,#000000,#ffffff,#000000,#ffffff,#000000,#ffffff,#000000,#ffffff,#000000,#ffffff,#000000,#ffffff,#000000,#ffffff");
		//Styling gutter = StyleProperty.forName("gutter").get().parse("0.02");
		Configuration gutter = ConfigProperty.forName("gutter").get().parse("0.02");
		Configuration offset = ConfigProperty.forName("offset").get().parse("(0.02,-0.2,0.05,-0.05)");
//		Styling gutter = StyleProperty.forName("gutter").get().parse("0");
//		Styling offset = StyleProperty.forName("offset").get().parse("(0.01,-0.01,0.01,-0.01)");
//		Styling gutter = StyleProperty.forName("gutter").get().parse("0.04");
//		Styling offset = StyleProperty.forName("offset").get().parse("(0.08,-0.08,0.08,-0.08)");
		//Styling ambient = StyleProperty.forName("ambient-color").get().parse("#ffffff00");
		Configuration ambient = ConfigProperty.forName("ambient-color").get().parse("#ffffffff");
		//Styling light = StyleProperty.forName("light-direction").get().parse("u(1,-2,0)");
		Configuration light = ConfigProperty.forName("light-direction").get().parse("u(1,0,-1)");
		Configuration coloring = ConfigProperty.forName("coloring").get().parse("vertical(#ff000000,#00000000)");
		//Styling coloring = StyleProperty.forName("coloring").get().parse("corners(#ffff0000,#ff00ff00,#ff0000ff,#ffffff00)");
		Configuration height = ConfigProperty.forName("height").get().parse("0.25");
		Configuration bgHeight = ConfigProperty.forName("background-height").get().parse("0.5");
		Configuration bgAlign = ConfigProperty.forName("background-align").get().parse("middle");
		Configuration vAlign = ConfigProperty.forName("vertical-align").get().parse("middle");
		Configuration bgTrans = ConfigProperty.forName("transition-duration").get().parse("3s");
		Configuration bgShow = ConfigProperty.forName("display-duration").get().parse("15s");
		ConfigRules rules = new ConfigRules(
//				new StyleRule(StyleSelectors.type(StyleMatchers.equals("design")),1f, palette),
				new ConfigRule(ConfigSelectors.type(ConfigMatchers.equals("panel")),1f, gutter, offset, ambient, light),
				new ConfigRule(ConfigSelectors.type(ConfigMatchers.equals("bar")), 1f, coloring, height, bgHeight, bgAlign, vAlign),
				new ConfigRule(ConfigSelectors.type(ConfigMatchers.equals("background")), 1f, bgTrans, bgShow)
				);
		Configurator stylist = new Configurator(design, rules);
		stylist.configure();
	}

	private void updateConsole() {
//		if (count++ == 10) {
//		char c = (char) (32 + crand.nextInt(94));
//		StringBuilder sb = new StringBuilder();
//		int len = 1 + crand.nextInt(10);
//		for (int i = 0; i < len; i++) sb.append(c);
//		console.putString(sb, crand.nextInt());
//		//console.putChar(32 + crand.nextInt(94), crand.nextInt());
//		count = 0;
//	}

//	if (count++ == 60) {
//		StringBuilder sb = new StringBuilder();
//		for (int i = 0; i < 11; i++) sb.append((char) charz);
//		charz++;
//		console.putString(sb, crand.nextInt());
//		//console.putChar(32 + crand.nextInt(94), crand.nextInt());
//		count = 0;
//	}

//	// Clear screen
//	gl.glClearColor(1f, 1f, 1f, 0f);
//	gl.glClear(
//			GL2ES2.GL_COLOR_BUFFER_BIT   |
//			GL2ES2.GL_DEPTH_BUFFER_BIT   );

//	if (count++ == 30) {
//		console.scroll(1, -1);
//		console.putStringAt(0, console.rows - 1, String.valueOf(System.currentTimeMillis()), crand.nextInt());
//		count = 0;
//	}

		if (count++ == 0) {
			win1.setColors(randCol(), randCol()).print("This is\na new test with a super long string to check that scrolling over multiple lines works okay.");
			win1.setColors(randCol(), randCol()).print("Mit solchen Straßen bin ich gut bekannt.");
			win1.printNewline();
			win2.setColors(randCol(), randCol()).print(String.valueOf(System.currentTimeMillis()));
			console.createWindow().setCursor(console.cols - 1, console.rows/2).setColors(LogicalColor.LGH_BCK, LogicalColor.LGH_FOR).print("M");
			count = 0;
		}
	}
}
