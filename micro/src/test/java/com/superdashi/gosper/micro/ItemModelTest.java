package com.superdashi.gosper.micro;

import java.util.Arrays;

import com.superdashi.gosper.item.Item;
import com.superdashi.gosper.item.Value;
import org.junit.Assert;
import org.junit.Test;
import static com.superdashi.gosper.micro.ItemModel.PROPERTY_COMMON_MARK;

public class ItemModelTest {

    @Test
    public void testCommonMarkProperties() {
        testCommonMarkProperties("label", "label");
        testCommonMarkProperties(",");
        testCommonMarkProperties(" ,");
        testCommonMarkProperties(", ");
        testCommonMarkProperties(",label", "label");
        testCommonMarkProperties(", label", "label");
        testCommonMarkProperties(",label ", "label");
        testCommonMarkProperties("label,", "label");
        testCommonMarkProperties("label ,", "label");
        testCommonMarkProperties("label, ", "label");
        testCommonMarkProperties("x:a,x:b", "x:a", "x:b");
        testCommonMarkProperties("x:a,,x:b", "x:a", "x:b");
        testCommonMarkProperties("x:a, ,x:b", "x:a", "x:b");
        testCommonMarkProperties("x:a, x:b", "x:a", "x:b");
    }

    private void testCommonMarkProperties(String str, String... properties) {
        Assert.assertEquals(Arrays.asList(properties), new ItemModel(Item.newBuilder().addExtra(PROPERTY_COMMON_MARK, Value.ofString(str)).build()).commonMarkProperties());
    }
}
