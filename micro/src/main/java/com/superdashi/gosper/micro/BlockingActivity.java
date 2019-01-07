/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.micro;

import java.util.concurrent.Callable;

import com.superdashi.gosper.item.Flavor;
import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.layout.Style;

public final class BlockingActivity<T> implements Activity {

	public static void reportProgress(int progress, int limit) {
		//TODO currently ignored
	}

	public interface Task<T> extends Callable<T> {

		default void configure() {}

		// information about the task
		default Item info() { return ActivityContext.current().activityItem(); }

		void processSuccess(T result);

		void processFailure(Exception e);

	}

	private final Task<T> task;

	private ActivityContext context;

	public BlockingActivity(Task<T> task) {
		if (task == null) throw new IllegalArgumentException("null task");
		this.task = task;
	}

	@Override
	public void init() {
		this.context = ActivityContext.current();
		task.configure();
	}

	@Override
	public void open(DataInput savedState) {
		Layout layout = Layout.single().withWeight(0f).addAbove().withWeight(1f).addBelow().withWeight(1f);
		Display display = context.configureDisplay().flavor(Flavor.MODAL).layoutDisplay(layout);
		Item info = task.info();
		if (info == null) info = Item.nothing();
		ItemModel infoModel = context.models().itemModel(info);

		ActiveModel activeModel = context.models().activeModel();
		Active active = display.addActive(Location.center);
		active.constrainLayout();
		active.model(activeModel);

		Style labelStyle = new Style().colorFg(0xffffffff).textWeight(1);
		Card labelCard = display.addCard(Location.top);
		labelCard.design(new CardDesign(ItemContents.label(), labelStyle));
		labelCard.model(infoModel);

		Style descStyle = new Style().colorFg(0xffffffff);
		Card descCard = display.addCard(Location.bottom);
		descCard.design( new CardDesign(ItemContents.description(), descStyle));
		descCard.model(infoModel);

		//TODO need animating widget in centre
		//TODO need to start task
		context.environment().submitBackground(() -> {
			T success = null;
			Exception failure = null;
			context.perform(() -> activeModel.active(true));
			ActivityContext.setCurrent(context);
			try {
				success = task.call();
			} catch (Exception e) {
				failure = e;
			} finally {
				ActivityContext.clearCurrent();
				context.perform(() -> activeModel.active(false));
			}
			Runnable r;
			if (failure == null) {
				T t = success;
				r = () -> task.processSuccess(t);
			} else {
				Exception e = failure;
				r = () -> task.processFailure(e);
			}
			context.perform(r);
		});
	}

	public ActivityContext context() {
		return context;
	}
}
