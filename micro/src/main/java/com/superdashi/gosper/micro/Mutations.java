package com.superdashi.gosper.micro;

final class Mutations {

	long count;

	Mutations() {
		count = 0L;
	}

	private Mutations(long count) {
		this.count = count;
	}

	Mutations copy() {
		return new Mutations(count);
	}
}