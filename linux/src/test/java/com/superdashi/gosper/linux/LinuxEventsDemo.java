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
package com.superdashi.gosper.linux;

import java.io.IOException;
import java.util.function.Function;

import com.superdashi.gosper.linux.LinuxEvents;

public class LinuxEventsDemo {

	public static void main(String... args) throws IOException {
		LinuxEvents events = new LinuxEvents(args[0], 5f, Function.identity());
		events.start(e -> System.out.println(e));
		int x = System.in.read();
		events.stop(100L);
		System.exit(0);
	}
}
