package com.superdashi.gosper.core;

import com.superdashi.gosper.graphdb.Inspector;
import com.superdashi.gosper.item.Info;

public interface InfoAcquirer extends Component {

	Info acquireInfo(Inspector inspector);

	boolean isNewInfoAvailable();
}
