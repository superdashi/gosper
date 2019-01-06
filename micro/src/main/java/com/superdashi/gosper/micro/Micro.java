package com.superdashi.gosper.micro;

import java.util.List;

import com.superdashi.gosper.framework.Details;
import com.superdashi.gosper.item.Qualifier;

public interface Micro {

	List<Details> allAppDetails();

	List<MicroAppLauncher> appLaunchers(Details appDetails, Qualifier qualifier);

}
