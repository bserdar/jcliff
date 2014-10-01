package com.redhat.jcliff;

import java.io.StringBufferInputStream;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;

public class ResolveTest {

    @Test
    public void longTest() throws Exception {
        StringBufferInputStream is=new StringBufferInputStream("{\"thread-pool\"=>{"+
                                                               "\"keepalive-time\"=>{"+
                                                               "\"time\"=>30L } } }");
        ModelNode node=ModelNode.fromStream(is); 
        List<NodePath> allPaths=new ArrayList<NodePath>();
        allPaths.add(new NodePath(new PathExpression("thread-pool","keepalive-time","time"),
                                                      node.get("thread-pool").get("keepalive-time")));
        
        String s=Configurable.resolve(new PathExpression("thread-pool","keepalive-time","time"),
                                      allPaths,
                                      "/subsystem=blah:write-attribute(name=${name(.)},value=${value(.)}");
        Assert.assertEquals("/subsystem=blah:write-attribute(name=time,value={\"time\" => 30L}",s);
    }
}
