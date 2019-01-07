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
