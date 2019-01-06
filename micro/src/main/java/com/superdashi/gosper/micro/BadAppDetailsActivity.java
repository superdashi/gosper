package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.layout.Style;

class BadAppDetailsActivity implements Activity {

	private final String instanceId;
	private final String reason;

	BadAppDetailsActivity(String instanceId, String reason) {
		this.instanceId = instanceId;
		this.reason = reason;
	}

	@Override
	public void open(DataInput savedState) {
		ActivityContext context = ActivityContext.current();
		Display display = context.configureDisplay().flavor(Flavor.ERROR).layoutDisplay(Layout.single());
		Bar bar = display.bar().get();
		bar.setPlainText("Failed application launch");
		Document document = display.addDocument(Location.center);

		DocumentModel docModel = context.models().documentModel();
		docModel.style(new Style().colorFg(0xffffffff).colorBg(0xff000000));
		docModel.attachedClass("title").style(new Style().textWeight(1).lineLimit(1));
		docModel.attachedClass("after").style(new Style().marginTop(2));
		docModel.appendNewBlock().textContent("Instance ID").classNames("title");
		docModel.appendNewBlock().textContent(instanceId);
		docModel.appendNewBlock().textContent("Reason").classNames("title", "after");
		docModel.appendNewBlock().textContent(reason);

		document.model(docModel);
	}

}
