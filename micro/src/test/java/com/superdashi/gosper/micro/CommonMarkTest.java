package com.superdashi.gosper.micro;

import java.util.Collections;

import com.superdashi.gosper.layout.StyledText;
import com.superdashi.gosper.layout.StyledText.Span;
import org.junit.Assert;
import org.junit.Test;

public class CommonMarkTest {

    @Test
    public void testBasic() {
        CommonMark cm = CommonMark.defaultFor(new VisualTheme());
        StyledText text = cm.parseAsStyledText("*This **is** emphatic!*");
        StyledText copy = new StyledText("This is emphatic!");
        copy.root().applyStyle( cm.styleFor(CommonMark.Key.EMPHASIS).get(), 0, 17 );
        copy.root().applyStyle( cm.styleFor(CommonMark.Key.STRONG).get(),  5, 7);
        Assert.assertEquals(text, copy);
    }

    @Test
    public void testLink() {
        CommonMark cm = CommonMark.defaultFor(new VisualTheme());
        StyledText text = cm.parseAsStyledText("This is [a link](http://www.example.com)");
        Assert.assertEquals(1, text.root().children().size());
        Span span = text.root().children().get(0);
        Assert.assertEquals("http://www.example.com", span.id().get());
        Assert.assertEquals("a link", span.text());
        Assert.assertEquals(Collections.singletonList(span), text.spansWithId("http://www.example.com"));
    }

    @Test
    public void testRemoveStyle() {
        CommonMark cm = CommonMark.defaultFor(new VisualTheme()).builder().clearStyle(CommonMark.Key.STRONG).build();
        StyledText text = cm.parseAsStyledText("*This **is** emphatic!*");
        StyledText copy = new StyledText("This is emphatic!");
        copy.root().applyStyle( cm.styleFor(CommonMark.Key.EMPHASIS).get(), 0, 17 );
        Assert.assertEquals(text, copy);
    }


}
