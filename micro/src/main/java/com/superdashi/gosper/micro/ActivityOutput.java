package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Info;
import com.superdashi.gosper.item.Item;
import com.tomgibara.streams.StreamSerializer;
import com.tomgibara.streams.Streams;
import com.tomgibara.streams.WriteStream;

public final class ActivityOutput extends ActivityData {

	static final int INI_DATA = 64;
	static final int MAX_DATA = 10 * 1024;

	ActivityOutput(ActivityInput input) {
		super(input);
		//TODO do we need to do anything special with data field?
	}

	//note: safe for apps to make, it's activity input which are 'owned' by the framework
	public ActivityOutput() {
	}

	public ActivityOutput flag(boolean flag) {
		this.flag = flag;
		return this;
	}

	public ActivityOutput code(long code) {
		this.code = code;
		return this;
	}

	public ActivityOutput text(String text) {
		this.text = text;
		return this;
	}

	public ActivityOutput value(double value) {
		this.value = value;
		return this;
	}

	public ActivityOutput info(Item item) {
		info = Info.from(item);
		return this;
	}

	public ActivityOutput info(Info info) {
		this.info = info;
		return this;
	}

	public <D> ActivityOutput data(D object, StreamSerializer<D> serializer) {
		data = Streams.bytes(INI_DATA, MAX_DATA);
		try (WriteStream w = data.writeStream()) {
			serializer.serialize(object, w);
		}
		return this;
	}

	ActivityInput toInput() {
		return new ActivityInput(this);
	}
}
