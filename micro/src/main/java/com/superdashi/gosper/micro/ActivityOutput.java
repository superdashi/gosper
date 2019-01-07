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
