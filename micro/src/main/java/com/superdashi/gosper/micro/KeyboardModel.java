package com.superdashi.gosper.micro;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.superdashi.gosper.micro.KeyboardDesign.Key;
import com.superdashi.gosper.micro.KeyboardDesign.ModelOp;
import com.superdashi.gosper.micro.KeyboardDesign.State;
import com.superdashi.gosper.studio.Frame;

//TODO handle synchronization
class KeyboardModel extends Model {

	private final static int VALID_UNKNOWN = -1;
	private final static int VALID_FALSE   =  0;
	private final static int VALID_TRUE    =  1;

	final KeyboardDesign design;
	final Mutations mutations;

	//TODO use collect
	private final Map<State, Frame> backdrops = new HashMap<>();
	private State state;
	private Key activeKey;
	private Regex regex;
	private String text = "";
	private int caret = 0;
	private int valid = VALID_UNKNOWN;

	KeyboardModel(ActivityContext context, KeyboardDesign design, Mutations mutations) {
		super(context);
		this.design = design;
		this.mutations = mutations;
		state = design.initialState;
		//TODO how do we know whether to set this?
		activeKey = design.initialKeyForState(state);
		startLoading();
	}

//	KeyboardModel(ModelBase contextSource, Mutations mutations) {
//		super(contextSource);
//		this.mutations = mutations;
//	}

	KeyboardModel(KeyboardModel that) {
		super(that);
		this.design = that.design;
		this.state = that.state;
		this.backdrops.putAll(that.backdrops);
		this.activeKey = that.activeKey;
		this.text = that.text;
		this.caret = that.caret;
		this.valid = that.valid;
		this.mutations = null;
	}

	State state() {
		return state;
	}

	boolean state(State state) {
		if (state == null) throw new IllegalArgumentException("null state");
		if (state == this.state) return false;
		design.validateState(state);
		this.state = state;
		mutations.count ++;
		requestRedraw();
		return true;
	}

	Optional<Regex> regex() {
		return Optional.ofNullable(regex);
	}

	void regex(Regex regex) {
		this.regex = regex;
	}

	String text() {
		return text;
	}

	String textBeforeCaret() {
		return caret == 0 ? "" : text.substring(0, caret);
	}

	String textAfterCaret() {
		return caret == text.length() ? "" : text.substring(caret);
	}

	void text(String text) {
		if (text == null) throw new IllegalArgumentException("null text");
		if (text.equals(this.text)) return;
		boolean preserveEndPosition = caret == this.text.length();
		updateText(text);
		if (preserveEndPosition || caret > text.length()) caret = text.length();
	}

	boolean valid() {
		if (valid == VALID_UNKNOWN) {
			valid = regex == null || regex.pattern.matcher(text).matches() ? VALID_TRUE : VALID_FALSE;
		}
		return valid == VALID_TRUE;
	}

	Optional<Key> activeKey() {
		return Optional.ofNullable(activeKey);
	}

//	Optional<Integer> activeCharacter() {
//		return Optional.ofNullable(activeKey).map(k -> k.charInState(state));
//	}

	ModelOp activeOperation() {
		return activeKey == null ? ModelOp.NO_OP : activeKey.operationInState(state);
	}

	void activateKey(Key key) {
		if (key == null) throw new IllegalArgumentException("null key");
		if (key == activeKey) return; // nothing to do
		design.validateKey(key);
		activeKey = key;
		mutations.count ++;
		requestRedraw();
	}

	int caret() {
		return caret;
	}

	int caret(int caret) {
		if (caret < 0) {
			caret = 0;
		} else if (caret > text.length()) {
			caret = text.length();
		}
		if (caret != this.caret) {
			this.caret = caret;
			mutations.count ++;
			requestRedraw();
		}
		return caret;
	}

	boolean caretAtStart() {
		return caret == 0;
	}

	boolean caretAtEnd() {
		return caret == text.length();
	}

	int moveCaret(int delta) {
		return -caret + caret(caret + delta);
	}

	int moveCaretToHome() {
		return caret(0);
	}

	int moveCaretToEnd() {
		return caret(text.length());
	}

	void deactivateKey() {
		if (activeKey == null) return; // nothing to do
		activeKey = null;
		mutations.count ++;
		requestRedraw();
	}

	boolean insertCharAhead(int code) {
		//TODO support full unicode
		char c = (char) code;
		String newText;
		if (caret == 0) {
			newText = c + text;
		} else if (caret == text.length()) {
			newText = text + c;
		} else {
			newText = text.substring(0, caret) + c + text.substring(caret);
		}
		updateText(newText);
		return true;
	}

	boolean insertCharBehind(int code) {
		boolean inserted = insertCharAhead(code);
		if (inserted) caret ++;
		return inserted;
	}

	boolean deleteBehind() {
		if (caretAtStart()) return false;
		String newText;
		if (caretAtEnd()) {
			newText = text.substring(0, text.length() - 1);
		} else {
			newText = text.substring(0, caret - 1) + text.substring(caret);
		}
		caret --;
		updateText(newText);
		return true;
	}

	boolean deleteAhead() {
		if (caretAtEnd()) return false;
		String newText;
		if (caretAtStart()) {
			newText = text.substring(1);
		} else {
			newText = text.substring(0, caret) + text.substring(caret + 1);
		}
		updateText(newText);
		return true;
	}

	// current backdrop, may not be present if not yet loaded
	Optional<Frame> backdrop() {
		return Optional.ofNullable( backdrops.get(state) );
	}

	Diff differenceFrom(KeyboardModel that) {
		return that == null ? new Diff() : new Diff(that, this);
	}

	KeyboardModel snapshot() {
		return new KeyboardModel(this);
	}

	private void updateText(String text) {
		this.text = text;
		mutations.count ++;
		requestRedraw();
		valid = VALID_UNKNOWN;
	}

	private void image(State state, Frame image) {
		backdrops.put(state, image);
		mutations.count ++;
		requestRedraw();
	}

	private void startLoading() {
		//TODO avoid loading same resource twice (needed?)
		//TODO avoid loading unnecessary mask
		for (State state : design.states) {
			backgroundLoad(design.backdropForState(state), this::loadImageFromResource, img -> this.image(state, img));
		}
	}

	static class Diff {

		boolean backdrop;
		boolean text;
		boolean caret;
		boolean active;

		boolean any;

		Diff(KeyboardModel from, KeyboardModel to) {
			backdrop = !Objects.equals(from.backdrops.get(from.state), to.backdrops.get(to.state));
			text = !from.text.equals(to.text);
			caret = from.caret != to.caret;
			active = !Objects.equals(from.activeKey, to.activeKey);
			any = backdrop || text || caret || active;
		}

		Diff() {
			backdrop = true;
			text = true;
			caret = true;
			active = true;
			any = true;
		}
	}

}
