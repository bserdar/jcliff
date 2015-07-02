package com.redhat.jcliff;

import java.io.StringBufferInputStream;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;

public class ResolveTest {

    Ctx ctx=new Ctx();
    
    @Test
    public void longTest() throws Exception {
        StringBufferInputStream is=new StringBufferInputStream("{\"thread-pool\"=>{"+
                                                               "\"keepalive-time\"=>{"+
                                                               "\"time\"=>30L } } }");
        ModelNode node=ModelNode.fromStream(is); 
        List<NodePath> allPaths=new ArrayList<NodePath>();
        allPaths.add(new NodePath(new PathExpression("thread-pool","keepalive-time","time"),
                                                      node.get("thread-pool").get("keepalive-time")));
        
        String s=Configurable.resolve1(new PathExpression("thread-pool","keepalive-time","time"),
                                      allPaths,
                                      "/subsystem=blah:write-attribute(name=${name(.)},value=${value(.)}",ctx);
        Assert.assertEquals("/subsystem=blah:write-attribute(name=time,value={\"time\" => 30L}",s);
    }
    
    @Test
    public void childrenTest() throws Exception {
        StringBufferInputStream is=new StringBufferInputStream("{\"A\"=>{"+
                                                               "\"B\"=>{"+
                                                               " \"c\"=>1L ,"+
                                                               " \"d\"=>2L ,"+
                                                               " \"e\"=>3L ,"+
                                                               " \"f\"=>4L ,"+
                                                               " \"g\"=>5L  } } }");
        ModelNode node=ModelNode.fromStream(is); 
        List<NodePath> allPaths=NodePath.getPaths(node);
        PathExpression p=new PathExpression("A","B");
        List<String> list=Configurable.getChildren(allPaths,p);
        Assert.assertEquals(5,list.size());
        Assert.assertEquals("c",list.get(0));
        Assert.assertEquals("d",list.get(1));
        Assert.assertEquals("e",list.get(2));
        Assert.assertEquals("f",list.get(3));
        Assert.assertEquals("g",list.get(4));
    }

    @Test
    public void loopTest() throws Exception {
        StringBufferInputStream is=new StringBufferInputStream("{\"A\"=>{"+
                                                               "\"B\"=>{"+
                                                               " \"c\"=>1L ,"+
                                                               " \"d\"=>2L ,"+
                                                               " \"e\"=>3L ,"+
                                                               " \"f\"=>4L ,"+
                                                               " \"g\"=>5L  } } }");
        ModelNode node=ModelNode.fromStream(is); 
        ctx.configPaths=NodePath.getPaths(node);

        String[] script=Configurable.resolve(new PathExpression("A","B"),
                                        ctx.configPaths,
                                        "${foreach-cfg (/A/B),(/subsystem=test/do-something:${name(.)},${value(.)}) }",
                                        ctx);
        Assert.assertEquals("/subsystem=test/do-something:c,1L",script[0]);
        Assert.assertEquals("/subsystem=test/do-something:d,2L",script[1]);
        Assert.assertEquals("/subsystem=test/do-something:e,3L",script[2]);
        Assert.assertEquals("/subsystem=test/do-something:f,4L",script[3]);
        Assert.assertEquals("/subsystem=test/do-something:g,5L",script[4]);
    }
}
