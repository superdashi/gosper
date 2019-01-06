package com.superdashi.gosper.micro;

import java.util.Optional;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.micro.DocumentModel.BlockModel;
import com.superdashi.gosper.micro.FormModel.FieldModel;

public class Form extends DocumentComponent {

	private static final String EXTRA_FIELD_NAME = "form:fieldName";
	// statics

	private static final char REDACTED_CHAR = '*';
	private static final String REDACTED_STR = "********************************";

	private static String redact(String line) {
		int length = line.length();
		if (length == 0) return line;
		if (length == REDACTED_STR.length()) return REDACTED_STR;
		if (length < REDACTED_STR.length()) return REDACTED_STR.substring(0, length);
		StringBuffer sb = new StringBuffer(length);
		for (int i = 0; i < length; i++) {
			sb.append(REDACTED_CHAR);
		}
		return sb.toString();
	}

	// fields

	private FormModel model = FormModel.empty();
	private long docRevision = -1L;

	// constructors

	// accessors

	public FormModel model() {
		return model;
	}

	public void model(FormModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		if (this.model == model) return; // nothing to do
		this.model = model;
		docRevision = -1;
		updateDocument();
	}

	// accessors

	public Optional<FieldModel> activeField() {
		return Optional.ofNullable(situation.currentAction()).map(a -> model.fieldWithName( a.item.value(EXTRA_FIELD_NAME).toString() ));
	}

	// component methods

	@Override
	public Changes changes() {
		updateDocument();
		return super.changes();
	}

	@Override
	public void render() {
		updateDocument();
		super.render();
	}

	@Override
	void receiveResponse(ActivityResponse response) {
		FieldModel field = model.fieldWithName(response.requestId); //TODO should log error somehow?
		DataInput data = response.returnedData;
		switch (field.type) {
		case STRING:
		case PASSWORD:
			String text = data.getString(KeyboardActivity.DATA_TEXT);
			field.value(Value.ofString(text));
			break;
		case SELECT:
			if (data.hasKey(SelectActivity.DATA_SELECTED)) {
				int index = data.getInt(SelectActivity.DATA_SELECTED);
				field.selectIndex(index);
			} else {
				field.value(Value.empty());
			}
			break;
			default:
				throw new IllegalStateException("unsupported field type: " + field.type);
		}
	}

	// private helper methods

	private void updateDocument() {
		if (docRevision == model.revision()) return;
		DocumentModel doc = model.models().documentModel();
		doc.attachedClass("label").style(new Style().textWeight(1).lineLimit(1));
		doc.attachedClass("value").style(new Style().textUnderline(1).lineLimit(1));
		model.fields().forEach(field -> {
			Item item = field.info.item;
			Action action = actionForField(field);
			Optional<BlockModel> labelBlock = item.label().map(s -> doc.newBlock().classNames("label").textContent(s));
			BlockModel valueBlock = doc.newBlock().classNames("value");
			switch (field.type) {
			case PASSWORD:
				valueBlock.textContent(redact(field.value().optionalString().orElse("")));
				break;
			case STRING:
				valueBlock.textContent(field.value().optionalString().orElse(""));
				break;
			case SELECT:
				valueBlock.textContent(field.selectedOption().map(a -> a.item.label().orElse(null)).orElse(""));
				break;
			default:
				throw new UnsupportedOperationException("unsupported field type: " + field.type);
			}
			Optional<BlockModel> descBlock = item.description().map(s -> doc.newBlock().classNames("desc").textContent(s));
			BlockModel groupBlock;
			if (!labelBlock.isPresent() && !descBlock.isPresent()) {
				groupBlock = valueBlock;
			} else {
				groupBlock = doc.newBlock();
				labelBlock.ifPresent(groupBlock::appendBlock);
				groupBlock.appendBlock(valueBlock);
				descBlock.ifPresent(groupBlock::appendBlock);
			}
			//TODO this is lame
			groupBlock.action(action).action().get().enabled(!field.readOnly());
			doc.appendBlock(groupBlock);
		});
		documentModel(doc);
		docRevision = model.revision();
	}

	private Action actionForField(FieldModel field) {
		Item item = field.info.item;
		Activities activities = situation.activities();
		DeferredActivity deferred;
		switch (field.type) {
		case PASSWORD:
		case STRING:
			String text = field.value().optionalString().orElse("");
			Regex regex = field.regex().orElse(null);
			deferred = activities.keyboard(item, text, regex);
			break;
		case SELECT:
			int selected = field.selectedIndex();
			deferred = activities.select(item, selected, field.options());
			break;
			default:
				throw new IllegalStateException("unsupported field type: " + field.type);
		}
		deferred.respondToComponent(situation);
		deferred.requestId = field.name;
		return Action.create(Action.ID_EDIT_FIELD, item, deferred);
	}

}
