package com.superdashi.gosper.micro;

import com.superdashi.gosper.item.Value;
import com.superdashi.gosper.layout.Style;
import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Span;
import com.vladsch.flexmark.ast.Emphasis;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.StrongEmphasis;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;

import java.util.EnumMap;
import java.util.Optional;

public final class CommonMark {

    private static final Style STYLE_EMPHASIS = new Style().textItalic(1).immutable();
    private static final Style STYLE_LINK     = new Style().textUnderline(1).immutable();
    private static final Style STYLE_STRONG   = new Style().textWeight(1).immutable();

    private static final EnumMap<Key, Style> defaultStyles = new EnumMap<>(Key.class);
    static {
        defaultStyles.put(Key.EMPHASIS, STYLE_EMPHASIS);
        defaultStyles.put(Key.LINK    , STYLE_LINK);
        defaultStyles.put(Key.STRONG  , STYLE_STRONG);
    }

    static CommonMark defaultFor(VisualTheme theme) {
        EnumMap<Key, Style> styles = new EnumMap<>(defaultStyles);
        styles.put(Key.LINK, STYLE_LINK.mutable().colorFg(theme.linkTextColor).immutable());
        return new CommonMark(styles);
    }

    public enum Key {
        EMPHASIS,
        LINK,
        STRONG,
    }

    public final static class Builder {

        private final EnumMap<Key, Style> styles;

        private Builder(EnumMap<Key, Style> styles) {
            this.styles = styles;
        }

        public Builder setStyle(Key key, Style style) {
            if (key == null) throw new IllegalArgumentException("null key");
            if (style == null) throw new IllegalArgumentException("null style");
            styles.put(key, style.immutable());
            return this;
        }

        public Builder clearStyle(Key key) {
            if (key == null) throw new IllegalArgumentException("null key");
            styles.remove(key);
            return this;
        }

        public CommonMark build() {
            return new CommonMark(new EnumMap<>(styles));
        }
    }

    public static Builder newBuilder() {
        return new Builder(new EnumMap<>(Key.class));
    }

    private final Parser parser;

    private final EnumMap<Key, Style> styles;

    CommonMark(EnumMap<Key, Style> styles) {
        //TODO just share a parser if config does not change?
        parser = Parser.builder().build();
        this.styles = styles;
    }

    public Builder builder() {
        return new Builder(new EnumMap<>(styles));
    }

    public Optional<Style> styleFor(Key key) {
        if (key == null) throw new IllegalArgumentException("null key");
        return Optional.ofNullable(styles.get(key));
    }

    public StyledText parseAsStyledText(String commonmark) {
        if (commonmark == null) throw new IllegalArgumentException("null commonmark");
        return new StyledTextComposer().process( parser.parse(commonmark) );
    }

    public ItemContents parsedItemContents(String property) {
        if (property == null) throw new IllegalArgumentException("null property");
        return new ItemContents() {
            @Override
            public Content contentFrom(ItemModel model) {
                String text = model.item.value(property).as(Value.Type.STRING).optionalString().orElse("");
                StyledText styledText = CommonMark.this.parseAsStyledText(text);
                return Content.styledTextContent(styledText);
            }
        };
    }

    private class StyledTextComposer {

        private final NodeVisitor visitor;
        final StyledText styledText;
        private Span span;

        StyledTextComposer() {
            visitor = new NodeVisitor(
                    new VisitHandler<>(Text.class, this::visitText),
                    new VisitHandler<>(Emphasis.class, this::visitEmphasis),
                    new VisitHandler<>(StrongEmphasis.class, this::visitStrong),
                    new VisitHandler<>(Link.class, this::visitLink)
            );
            styledText = new StyledText();
            span = styledText.root();
        }

        StyledText process(Document document) {
            visitor.visit(document);
            return styledText;
        }

        private void visitText(Text text) {
            appendNodeText(text);
        }

        private void visitEmphasis(Emphasis emphasis) {
            visitWithSpan(Key.EMPHASIS, emphasis);
        }

        private void visitStrong(StrongEmphasis strong) {
            visitWithSpan(Key.STRONG, strong);
        }

        private void visitLink(Link link) {
            Style style = styles.getOrDefault(Key.LINK, Style.noStyle());
            // TODO need a good way of handling this
            Span parent = span;
            span = span.appendStyledText(link.getUrl().unescape(), style, "");
            visitor.visitChildren(link);
            span = parent;
        }

        private void appendNodeText(Node node) {
            String text = node.getChars().unescape();
            span.appendText(text);
        }

        private void visitWithSpan(Key key, Node node) {
            Style style = styles.get(key);
            if (style != null) {
                span = span.appendStyledText(style, "");
            }
            visitor.visitChildren(node);
            if (style != null) {
                span = span.parent().get();
            }
        }

    }
}
