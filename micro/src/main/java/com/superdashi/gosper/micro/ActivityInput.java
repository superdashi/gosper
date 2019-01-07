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
