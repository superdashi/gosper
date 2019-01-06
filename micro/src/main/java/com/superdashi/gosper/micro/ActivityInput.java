package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Info;
import com.tomgibara.streams.ReadStream;
import com.tomgibara.streams.StreamDeserializer;

//TODO rename accessors
public class ActivityInput extends ActivityData {

	static final ActivityInput NULL = new ActivityInput();

	private ActivityInput() { }

	ActivityInput(ActivityOutput output) {
		super(output);
		// force reader to be detached
		if (data != null) data.readStream();
	}

	public boolean flag() {
		return flag;
	}

	public long code() {
		return code;
	}

	public String text() {
		return text;
	}

	public double value() {
		return value;
	}

	public Info info() {
		return info;
	}

	public <D> D data(StreamDeserializer<D> deserializer) {
		if (data == null) return null;
		try (ReadStream r = data.readStream()) {
			return deserializer.deserialize(r);
		}
	}

	public ActivityOutput toOutput() {
		return new ActivityOutput(this);
	}
}
