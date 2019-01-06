package com.superdashi.gosper.data;

import com.superdashi.gosper.http.HttpReqRes;


public interface DataRecorder {

	default void init(DataContext context) { }

	default void recordDataOnSchedule() { }

	//TODO should this generate some sort of response
	default void recordDataOnHttp(HttpReqRes reqres) { }

	//void recordDataOnDatagram(... datagram);

	default void destroy() { }
}
