package com.superdashi.gosper.micro;

public final class ActivityResponse {

	public final String requestId;
	public final DataInput returnedData;

	ActivityResponse(String requestId, DataInput returnedData) {
		this.requestId = requestId;
		this.returnedData = returnedData;
	}

}
