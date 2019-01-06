package com.superdashi.gosper.micro;

import java.util.concurrent.Future;

import com.superdashi.gosper.item.Item;

public interface Settings {

	Future<Item> saveSettings(Item settings);

	Item loadSettings();

	boolean loadingAvailable();

	boolean savingAvailable();

}
