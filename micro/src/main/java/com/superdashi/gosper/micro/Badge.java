package com.superdashi.gosper.micro;

import java.util.Optional;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.item.Value.Type;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Composer;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.PorterDuff;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.PorterDuff.Rule;
import com.tomgibara.intgeom.IntRect;

public abstract class Badge {

	private static final Composer src = new PorterDuff(Rule.SRC).asComposer();
	private static final Composer srcOver = new PorterDuff(Rule.SRC_OVER).asComposer();

	final VisualSpec spec;
	final Style style; // computed style
	final String property; // assume every badge will be keyed off a property
	final IntRect bounds; // precomputed

	Badge(VisualSpec spec, Style style, String property) {
		assert spec != null;
		assert property != null && !property.isEmpty();
		this.spec = spec;
		this.style = style == null ? spec.styles.defaultBadgeStyle : spec.styles.defaultBadgeStyle.mutable().apply(style).immutable();
		this.property = property;

		int size = spec.metrics.badgeSize;
		bounds = IntRect.atOrigin(size, size);
	}

	abstract void render(Item item, Canvas canvas);

	static final class LetterBadge extends Badge {

		private final TextStyle textStyle;
		private final int baseline;
		private final boolean capitalize;

		LetterBadge(VisualSpec spec, Style style, String property, boolean capitalize) {
			super(spec, style, property);
			this.capitalize = capitalize;
			textStyle = TextStyle.fromStyle(style);
			baseline = spec.typeMetrics.fontMetrics(textStyle).baseline;
		}

		@Override
		void render(Item item, Canvas canvas) {
			Optional<String> value = item.value(property).optionalString();
			if (!value.isPresent()) return; // no value, so no badge
			char c = value.get().charAt(0);
			if (capitalize) c = Character.toUpperCase(c);
			canvas.composer(src).color(style.colorBg()).intOps().fillRect(bounds).canvas().composer(srcOver).color(style.colorFg());
			canvas.intOps().newText(spec.typeface).moveTo(1, baseline).renderChar(textStyle, c);
		}
	}

	static final class NumberBadge extends Badge {

		private final TextStyle textStyle;
		private final int baseline;

		NumberBadge(VisualSpec spec, Style style, String property) {
			super(spec, style, property);
			textStyle = TextStyle.fromStyle(style);
			baseline = spec.typeMetrics.fontMetrics(textStyle).baseline;
		}

		@Override
		void render(Item item, Canvas canvas) {
			Optional<Long> value = item.value(property).optionalInteger();
			if (!value.isPresent()) return; // no value, so no badge

			canvas.composer(src).color(style.colorBg()).intOps().fillRect(bounds).canvas().composer(srcOver).color(style.colorFg());
			canvas.intOps().newText(spec.typeface).moveTo(1, baseline).renderString(value.get().toString());
		}

	}

	static final class IndexedBadge extends Badge {

		private final ItemModel[] models;

		IndexedBadge(VisualSpec spec, Style style, String property, ItemModel... models) {
			super(spec, style, property);
			this.models = models;
		}

		@Override
		void render(Item item, Canvas canvas) {
			Value value = item.value(property).as(Type.INTEGER);
			if (value.isEmpty()) return; // no value, no badge

			long index = value.integer();
			//TODO should optionally clamp to range?
			if (index < 0 || index >= models.length) return;
			ItemModel model = models[(int) index];
			Optional<Frame> optional = model.badge();
			if (!optional.isPresent()) return;

			canvas
				.intOps().clipRectAndTranslate(bounds).canvas()
				.composer(src).color(style.colorBg()).fill()
				.composer(srcOver).color(style.colorFg()).drawFrame(optional.get());
		}
	}

	static final class CheckboxBadge extends Badge {

		private final boolean checkedByDefault;
		private final Value switchValue;
		private final Frame checkbox;
		private final IntRect off;
		private final IntRect on;

		CheckboxBadge(VisualSpec spec, Style style, String property, boolean checkedByDefault, Value switchValue) {
			super(spec, style, property);
			this.checkedByDefault = checkedByDefault;
			this.switchValue = switchValue;
			checkbox = spec.checkbox();

			int size = spec.metrics.badgeSize;
			off = IntRect.rectangle(0, 0, 1, 1).scaled(size);
			on  = IntRect.rectangle(0, 2, 1, 1).scaled(size);
		}

		@Override
		void render(Item item, Canvas canvas) {
			Value value = item.value(property).as(switchValue.type());
			boolean checked = checkedByDefault ^ value.equals(switchValue);
			canvas.drawFrame(checkbox.view(checked ? on : off));
		}

	}
}
