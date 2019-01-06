package com.superdashi.gosper.http;

public interface HttpReqRes {

	HttpRequest request();

	//TODO expose full request too

	void respondStatus(int statusCode, String statusMessage);

	void respondText(int statusCode, String mimeType, String content);

	void respondNotFound();

	void respondOkay();

}
