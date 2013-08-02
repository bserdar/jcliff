package com.redhat.jcliff;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

public class DefaultPostProcessorTest {

    Configurable.DefaultPostprocessor processor;

    @Before
    public void setup() {
        processor=new Configurable.DefaultPostprocessor();
    }

    @Test
    public void simpleOut() throws Exception {
        ModelNode[] ret=processor.process("{ \"result\" => \"success\"}");
        Assert.assertEquals(1,ret.length);
        Assert.assertEquals(ModelType.STRING,ret[0].getType());
    }
}
