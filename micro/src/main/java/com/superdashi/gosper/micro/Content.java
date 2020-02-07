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

import static com.superdashi.gosper.util.Geometry.intRectToRect;
import static com.superdashi.gosper.util.Geometry.roundRectToIntRect;

import java.util.List;
import java.util.Optional;

import com.superdashi.gosper.color.Argb;
import com.superdashi.gosper.layout.Position;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.studio.Canvas;
import com.superdashi.gosper.studio.Frame;
import com.superdashi.gosper.studio.TextStyle;
import com.superdashi.gosper.studio.Typeface;
import com.superdashi.gosper.studio.Canvas.IntTextOps;
import com.tomgibara.fundament.Producer;
import com.tomgibara.geom.core.Rect;
import com.tomgibara.geom.transform.Transform;
import com.tomgibara.intgeom.IntDimensions;
import com.tomgibara.intgeom.IntRect;
import com.tomgibara.intgeom.IntVector;

//TODO should expose?
//TODO content ignores margins - assumes already applied - should this ambiguity be removed by zeroing them before passing in?
public abstract class Content {

	// static fields

	private static final NilContent nilContent = new NilContent();

	static Content noContent = new Content() {
		@Override SizedContent sizeForWidth(VisualSpec spec, Style style, int width) { return SizedContent.noContent(IntDimensions.of(width, 0)); }
		@Override SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) { return SizedContent.noContent(IntDimensions.NOTHING); }
	};

	// static methods

	//TODO move to visual context?
	public static Content textContent(String text) {
		if (text == null) throw new IllegalArgumentException("null text");
		return new TextContent(text);
	}

	public static Content styledTextContent(StyledText styledText) {
		if (styledText == null) throw new IllegalArgumentException("null styledText");
		return new StyledTextContent(styledText.immutable());
	}

	public static Content badgeContent(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return new ImageContent(model::badge);
	}

	public static Content symbolContent(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return new ImageContent(model::symbol);
	}

	public static Content iconContent(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return new ImageContent(model::enabledIcon);
	}

	public static Content pictureContent(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return new ImageContent(model::picture);
	}

	public static Content cardContent(CardDesign design, ItemModel model) {
		if (design == null) throw new IllegalArgumentException("null design");
		if (model == null) throw new IllegalArgumentException("null model");
		return new CardContent(design, model);
	}

	public static Content nilContent(ItemModel model) {
		if (model == null) throw new IllegalArgumentException("null model");
		return new ImageContent(model::symbol);
	}

	// only package may make instances
	Content() {}

	abstract SizedContent sizeForWidth(VisualSpec spec, Style style, int width);

	abstract SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions);

	// common implementations

	private static final class NilContent extends Content {

		@Override
		SizedContent sizeForWidth(VisualSpec spec, Style style, int width) {
			return new NilSizedContent( IntDimensions.horizontal(width) );
		}

		@Override
		SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) {
			return new NilSizedContent(dimensions);
		}

	}

	private static final class TextContent extends Content {

		private final String text;

		TextContent(String text) {
			this.text = text;
		}

		@Override
		SizedContent sizeForWidth(VisualSpec spec, Style style, int width) {
			return size(spec, style, width, Integer.MAX_VALUE);
		}

		@Override
		SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) {
			return size(spec, style, dimensions.width, dimensions.height);
		}

		private SizedContent size(VisualSpec spec, Style style, int width, int height) {
			// compute number of lines
			//TODO typeface should be configurable from style
			Typeface typeface = spec.typeface;
			TextStyle textStyle = TextStyle.fromStyle(style);
			boolean outline = style.textOutline() != 0; // we have to do outlining ourselves
			int lineLimit = style.lineLimit();
			int lineLength = outline ? width - 2 : width;
			List<String> lines = spec.splitIntoLines(typeface, textStyle, text, lineLength, lineLimit);
			int lineCount = Math.min(lines.size(), lineLimit);
			if (lineCount < lines.size()) {
				lines.subList(lineCount, lines.size()).clear();
				int lastIndex = lineCount - 1;
				String lastLine = lines.get(lastIndex);
				lines.set(lastIndex, lastLine + spec.theme.ellipsisString);
			}
			// obtain line height
			int lineHeight = spec.metrics.lineHeight;
			int textHeight =
					lineHeight * lineCount +               // line heights
					style.lineSpace() * (lineCount - 1);   // line spaces
			height = Math.min(height, textHeight);
			// assemble and return
			IntDimensions dimensions = IntDimensions.of(width, height);
			return new SizedContent() {
				@Override
				public IntDimensions dimensions() {
					return dimensions;
				}

				@Override
				public void render(Canvas canvas, IntRect contentArea) {
					int color = style.colorFg();
					int olColor = Argb.WHITE; //TODO how to compute this?
					IntTextOps text = canvas.intOps().newText(typeface);
					int baseline = typeface.metrics().fontMetrics(textStyle).baseline;
					int x = outline ? contentArea.minX + 1 : contentArea.minX;
					int y = contentArea.minY + baseline;
					int lineSpace = style.lineSpace();
					for (String line : lines) {
						text.moveTo(x, y);
						if (outline) {
							//TODO YUCK!
							canvas.color(olColor);
							text
								.moveBy( 1,  0).renderString(textStyle, line)
								.moveBy(-2,  0).renderString(textStyle, line)
								.moveBy( 1,  1).renderString(textStyle, line)
								.moveBy( 0, -2).renderString(textStyle, line)
								.moveBy( 0,  1);
						}
						canvas.color(color);
						text.renderString(textStyle, line);
						y+= spec.metrics.lineHeight + lineSpace;
					}
				}
			};
		}
	}

	private static final class StyledTextContent extends Content {

		private final StyledText styledText;

		StyledTextContent(StyledText styledText) {
			this.styledText = styledText;
		}

		@Override
		SizedContent sizeForWidth(VisualSpec spec, Style style, int width) {
			return size(spec, style, width, Integer.MAX_VALUE);
		}

		@Override
		SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) {
			return size(spec, style, dimensions.width, dimensions.height);
		}

		private SizedContent size(VisualSpec spec, Style style, int width, int height) {
			// compute number of lines
			//TODO typeface should be configurable from style
			Typeface typeface = spec.typeface;
			TextStyle textStyle = TextStyle.fromStyle(style);
			boolean outline = style.textOutline() != 0; // we have to do outlining ourselves
			int lineLimit = style.lineLimit();
			int lineLength = outline ? width - 2 : width;

			List<StyledText> lines = spec.splitIntoLines(typeface, style, styledText, lineLength, lineLimit);
			int lineCount = Math.min(lines.size(), lineLimit);
			if (lineCount < lines.size()) {
				lines.subList(lineCount, lines.size()).clear();
				int lastIndex = lineCount - 1;
				StyledText lastLine = lines.get(lastIndex).mutable();
				lastLine.appendText(spec.theme.ellipsisString);
				lines.set(lastIndex, lastLine);
			}
			// obtain line height
			int lineHeight = spec.metrics.lineHeight;
			int textHeight =
					lineHeight * lineCount +               // line heights
					style.lineSpace() * (lineCount - 1);   // line spaces
			height = Math.min(height, textHeight);
			// assemble and return
			IntDimensions dimensions = IntDimensions.of(width, height);
			return new SizedContent() {
				@Override
				public IntDimensions dimensions() {
					return dimensions;
				}

				@Override
				public void render(Canvas canvas, IntRect contentArea) {
					int color = style.colorFg();
					int olColor = Argb.WHITE; //TODO how to compute this?
					IntTextOps text = canvas.intOps().newText(typeface);
					int baseline = typeface.metrics().fontMetrics(textStyle).baseline;
					int x = outline ? contentArea.minX + 1 : contentArea.minX;
					int y = contentArea.minY + baseline;
					int lineSpace = style.lineSpace();
					for (StyledText line : lines) {
						text.moveTo(x, y);
						if (outline) {
							//TODO YUCK!
							canvas.color(olColor);
							text
									.moveBy( 1,  0).renderText(style, line)
									.moveBy(-2,  0).renderText(style, line)
									.moveBy( 1,  1).renderText(style, line)
									.moveBy( 0, -2).renderText(style, line)
									.moveBy( 0,  1);
						}
						canvas.color(color);
						text.renderText(style, line);
						y+= spec.metrics.lineHeight + lineSpace;
					}
				}
			};
		}
	}

	private static final class ImageContent extends Content {

		private final Producer<Optional<Frame>> source;

		ImageContent(Producer<Optional<Frame>> source) {
			this.source = source;
		}

		@Override
		SizedContent sizeForWidth(VisualSpec spec, Style style, int width) {
			Optional<Frame> optional = source.produce();
			if (!optional.isPresent()) return SizedContent.noContent(IntDimensions.horizontal(width));
			Frame image = optional.get();
			return size(spec, style, image.dimensions().withWidth(width), image);
		}

		@Override
		SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) {
			Optional<Frame> optional = source.produce();
			if (!optional.isPresent()) return SizedContent.noContent(dimensions.withHeight(0));
			return size(spec, style, dimensions, optional.get());
		}

		private SizedContent size(VisualSpec spec, Style style, IntDimensions dimensions, Frame image) {
			return new SizedContent() {
				@Override
				public IntDimensions dimensions() {
					return dimensions;
				}

				@Override
				public void render(Canvas canvas, IntRect contentArea) {
					canvas.intOps().drawFrame(image, contentArea.minimumCoords());
				}
			};
		}

	}

	private static final class CardContent extends Content {

		private final CardDesign design;
		private final ItemModel model;

		CardContent(CardDesign design, ItemModel model) {
			this.design = design;
			this.model = model;
		}

		@Override
		SizedContent sizeForWidth(VisualSpec spec, Style style, int width) {
			IntDimensions mindim = design.layout().sizer(spec).minimumDimensions(new Constraints(IntRect.atOrigin(width, Integer.MAX_VALUE)));
			if (mindim.width < width) return SizedContent.noContent(IntDimensions.horizontal(width));
			return size(spec, style, mindim.withWidth(width));
		}

		@Override
		SizedContent sizeForDimensions(VisualSpec spec, Style style, IntDimensions dimensions) {
			IntDimensions mindim = design.layout().sizer(spec).minimumDimensions(new Constraints(dimensions.toRect()));
			if (!dimensions.meets(mindim)) return SizedContent.noContent(IntDimensions.NOTHING);
			return size(spec, style, dimensions);
		}

		private SizedContent size(VisualSpec spec, Style style, IntDimensions dimensions) {
			// compute places here and translate later for possible efficiency gain where sized content gets reused
			Places places = design.layout().sizer(spec).computePlaces(new Constraints(dimensions.toRect())).get();
			return new SizedContent() {
				@Override
				public IntDimensions dimensions() {
					return dimensions;
				}

				@Override
				public void render(Canvas canvas, IntRect contentArea) {
					IntVector offset = contentArea.vectorToMaximumCoords();
					Optional<Position> optPos = design.backgroundPosition();
					Optional<Frame> optPic = model.picture();
					if (optPos.isPresent() && optPic.isPresent()) {
						Frame pic = optPic.get();
						IntRect picBounds = pic.dimensions().toRect();
						Rect src = intRectToRect(picBounds);
						Rect dst = intRectToRect(contentArea);
						Transform transform = optPos.get().transform(src, dst);
						Rect rect = transform.transform(src);
						IntRect target = roundRectToIntRect(rect);
						//TODO support scaling
						canvas.intOps().drawFrame(pic, target.minimumCoords());
					}
					places.stream().forEach(place -> {
						int colorBg = place.style.colorBg();
						if (!Argb.isTransparent(colorBg)) canvas.color(colorBg).intOps().fillRect(place.outerBounds.translatedBy(offset));
						Optional<ItemContents> optional = design.contentsAtLocation(place.location);
						optional.ifPresent(contents -> {
							contents.contentFrom(model)
								.sizeForWidth(spec, place.style, place.innerBounds.width())
								.render(canvas, place.innerBounds.translatedBy(offset));
						});
					});
				}
			};
		}

	}

	private static class NilSizedContent implements SizedContent {

		private final IntDimensions dimensions;

		NilSizedContent(IntDimensions dimensions) {
			this.dimensions = dimensions;
		}

		@Override
		public IntDimensions dimensions() {
			return dimensions;
		}

		@Override
		public void render(Canvas canvas, IntRect contentArea) { }
	}
}