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
