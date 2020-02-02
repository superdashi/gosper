package com.superdashi.gosper.micro;

import com.superdashi.gosper.layout.StyledText;
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
    public void testRemoveStyle() {
        CommonMark cm = CommonMark.defaultFor(new VisualTheme()).builder().clearStyle(CommonMark.Key.STRONG).build();
        StyledText text = cm.parseAsStyledText("*This **is** emphatic!*");
        StyledText copy = new StyledText("This is emphatic!");
        copy.root().applyStyle( cm.styleFor(CommonMark.Key.EMPHASIS).get(), 0, 17 );
        Assert.assertEquals(text, copy);
    }


}
