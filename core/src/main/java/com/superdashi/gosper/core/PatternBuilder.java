package com.superdashi.gosper.core;

import com.superdashi.gosper.color.Palette.LogicalColor;

public interface PatternBuilder<P> {

	void size(int cols, int rows);

	void putChar(int col, int row, LogicalColor bg, LogicalColor fg, int c);

	P finish();

	int sizeInBytes(P pattern);
}
