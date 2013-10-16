package com.redhat.jcliff;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


public class EscapeTest {


    @Test
    public void doit() throws Exception {
        Assert.assertEquals("",NodeDiff.unescape(""));
        Assert.assertEquals("aqwert",NodeDiff.unescape("aqwert"));
        Assert.assertEquals("qwe\rt",NodeDiff.unescape("qwe\\rt"));
        Assert.assertEquals("qwe t",NodeDiff.unescape("qwe\\u0032t"));
        Assert.assertEquals("x",NodeDiff.unescape("\\u0x"));
    }

}
