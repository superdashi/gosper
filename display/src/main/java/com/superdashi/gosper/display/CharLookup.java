package com.superdashi.gosper.display;


public interface CharLookup {

	int locate(int codepoint);

	byte u(int location);

	byte v(int location);
}
