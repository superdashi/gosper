package com.superdashi.gosper.data;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.graphdb.Editor;

public interface DataContext extends Editor {

	Details details();

	//TODO needs observation
}
