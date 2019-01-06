package com.superdashi.gosper.micro;

import java.util.function.Function;

//TODO name is slightly confusing
//TODO consider implications of making this an interface
//TODO construct instances lazily?
public abstract class ItemContents {

	// statics

	private static final ItemContents label       = new FunctionContents(model -> Content.textContent(model.item.label().orElse("")));
	private static final ItemContents description = new FunctionContents(model -> Content.textContent(model.item.description().orElse("")));
	private static final ItemContents picture     = new FunctionContents(Content::pictureContent);
	private static final ItemContents icon        = new FunctionContents(Content::iconContent);
	private static final ItemContents badge       = new FunctionContents(Content::badgeContent);
	private static final ItemContents symbol      = new FunctionContents(Content::symbolContent);
	private static final ItemContents nil         = new FunctionContents(Content::nilContent);

	public static ItemContents label()       { return label;       }
	public static ItemContents description() { return description; }
	public static ItemContents picture()     { return picture;     }
	public static ItemContents icon()        { return icon;        }
	public static ItemContents badge()       { return badge;       }
	public static ItemContents symbol()      { return symbol;      }
	public static ItemContents nil()         { return nil;         }

	public static ItemContents card(CardDesign design) {
		if (design == null) throw new IllegalArgumentException("null design");
		return new ItemContents() {
			@Override
			public Content contentFrom(ItemModel model) {
				return Content.cardContent(design, model);
			}
		};
	}

	// constructors

	private ItemContents() { }

	// methods

	public abstract Content contentFrom(ItemModel model);

	// inner classes

	private static class FunctionContents extends ItemContents {

		private final Function<ItemModel, Content> fn;

		FunctionContents(Function<ItemModel, Content> fn) {
			this.fn = fn;
		}

		@Override
		public Content contentFrom(ItemModel model) {
			return fn.apply(model);
		}

	}
}
