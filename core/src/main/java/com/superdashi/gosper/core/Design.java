package com.superdashi.gosper.core;

import java.util.Arrays;
import java.util.Optional;

import com.superdashi.gosper.config.ConfigTarget;
import com.superdashi.gosper.config.ConfigUtil;
import com.superdashi.gosper.config.Configurable;
import com.tomgibara.bits.BitStore;
import com.tomgibara.bits.Bits;
import com.tomgibara.storage.AbstractStore;
import com.tomgibara.storage.Storage;
import com.tomgibara.storage.Store;
import com.tomgibara.storage.StoreType;

public class Design implements Configurable {

	private static final StoreType<Panel> panelType = StoreType.of(Panel.class).settingNullDisallowed();
	private static final Storage<Panel> panelStorage = panelType.storage().immutable();
	private static final DesignConfig DEFAULT_STYLE = new DesignConfig();
	private static final Store<Panel> DEFAULT_PANELS = panelType.emptyStore();

	public final DesignConfig style = new DesignConfig();

	private DesignConfig givenStyle = DEFAULT_STYLE;
	private Store<Panel> panels = DEFAULT_PANELS;
	private Bar bar = null;
	private Background background = null;

	public Design() {
	}

	public void setStyle(DesignConfig style) {
		if (style == null) throw new IllegalArgumentException("null style");
		givenStyle = style;
	}

	public Store<Panel> getPanels() {
		return panels;
	}

	public void setPanels(Store<Panel> panels) {
		setPanelsImpl( panelStorage.newCopyOf(panels) );
	}

	public void setPanels(Panel... panels) {
		setPanelsImpl( panelType.arrayAsStore(panels).immutableCopy() );
	}

	public Bar getBar() {
		return bar;
	}

	public void setBar(Bar bar) {
		if (this.bar != null) {
			this.bar.design = null;
		}
		this.bar = bar;
		if (bar != null) {
			bar.design = this;
		}
	}

	public Background getBackground() {
		return background;
	}

	public void setBackground(Background background) {
		if (this.background != null) {
			this.background.design = null;
		}
		this.background = background;
		if (background != null) {
			background.design = this;
		}
	}

	@Override
	public Type getStyleType() {
		return Configurable.Type.DESIGN;
	}

	@Override
	public ConfigTarget openTarget() {
		style.adopt(givenStyle);
		return style;
	}

	@Override
	public String getId() {
		// TODO
		return null;
	}

	@Override
	public Optional<String> getRole() {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public Store<? extends Configurable> getStyleableChildren() {
		int count = childCount();
		Store<Configurable> store = ConfigUtil.size(count);
		int i = 0;
		if (bar != null) store.set(i++, bar);
		if (background != null) store.set(i++, background);
		store.setStore(i, panels);
		return store.immutableView();
	}

	//panels.forEach(p -> p.design = this);
	// workaround for forEach implementation bug
	private void setPanelsImpl(Store<Panel> panels) {
		for (int i = 0; i < this.panels.size(); i++) {
			this.panels.get(i).design = null;
		}
		this.panels = panels;
		for (int i = 0; i < panels.size(); i++) {
			panels.get(i).design = this;
		}
	}

	private int childCount() {
		int count = panels.size();
		if (bar != null) count ++;
		if (background != null) count ++;
		return count;
	}
	// TODO make these generally available

	private static final class SingletonStore<T> extends AbstractStore<T> {

		private final T value;

		SingletonStore(T value) {
			this.value = value;
		}

		@Override
		public int size() {
			return 1;
		}

		@Override
		public int count() {
			return value == null ? 0 : 1;
		}

		@Override
		public BitStore population() {
			return value == null ? Bits.zeroBit() : Bits.oneBit();
		}

		@Override
		public T get(int index) {
			if (index != 0) throw new IllegalArgumentException("invalid index");
			return value;
		}
	}

	//TODO exclude zero sized stores
	private static final class JointStore<T> extends AbstractStore<T> {

		private final Store<? extends T>[] stores;
		private final int offset[];
		private final int size;
		private final StoreType<T> type;

		@SafeVarargs
		JointStore(Store<? extends T>... stores) {
			int length = stores.length;
			if (length == 0) throw new IllegalArgumentException();
			this.stores = stores.clone();
			offset = new int[length];
			StoreType<? extends T> type = stores[0].type();
			int size = 0;
			{ // populate cumSize
				int i = 0;
				for (Store<?> store : stores) {
					offset[i++] = size;
					if (store.isMutable()) throw new IllegalArgumentException("mutable store");
					size += store.size();
					StoreType<?> t = store.type();
					if (t.nullGettable() && !type.nullGettable()) type = type.settingNullDisallowed();
					//TODO merge value types
				}
			}
			this.size = size;
			this.type = (StoreType<T>) type;
		}

		@Override
		public StoreType<T> type() {
			return type;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public T get(int index) {
			if (index < 0 || index >= size) throw new IllegalArgumentException("invalid index");
			int i = Arrays.binarySearch(offset, index);
			if (i >= 0) return stores[i].get(0);
			i = -1 - i;
			return stores[i].get(index - offset[i]);
		}

	}
}
