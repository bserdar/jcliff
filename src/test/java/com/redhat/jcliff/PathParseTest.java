package com.redhat.jcliff;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;

public class PathParseTest {

    Ctx ctx;
    
    private static String str(String s) {
        return s.replace("'","\"");
    }

    private static void add(List<NodePath> list,ModelNode node,String... levels) {
        List<String> l=new ArrayList<String>();
        for(String x:levels) {
            l.add(x);
            String[] arr=l.toArray(new String[l.size()]);
            list.add(new NodePath(new PathExpression(arr),node.get(arr)));
        }
    }

    @Test
    public void nameTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("e",Configurable.resolve1(matched,allNodes,"${name(.)}",ctx));
        Assert.assertEquals("d",Configurable.resolve1(matched,allNodes,"${name(..)}",ctx));
        Assert.assertEquals("c",Configurable.resolve1(matched,allNodes,"${name(../..)}",ctx));
        Assert.assertEquals("b",Configurable.resolve1(matched,allNodes,"${name(../../..)}",ctx));
        Assert.assertEquals("a",Configurable.resolve1(matched,allNodes,"${name(../../../..)}",ctx));
    }

    @Test
    public void valueTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals(str("'f'"),Configurable.resolve1(matched,allNodes,"${value(.)}",ctx));
        Assert.assertEquals(str("{'e' => 'f'}"),Configurable.resolve1(matched,allNodes,"${value(..)}",ctx));
    }

    @Test
    public void pathTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("/a/b/c/d/e",Configurable.resolve1(matched,allNodes,"${path(.)}",ctx));
        Assert.assertEquals("/a/b/c/d",Configurable.resolve1(matched,allNodes,"${path(..)}",ctx));
        Assert.assertEquals("/a/b/c",Configurable.resolve1(matched,allNodes,"${path(../..)}",ctx));
        Assert.assertEquals("/a/b",Configurable.resolve1(matched,allNodes,"${path(../../..)}",ctx));
        Assert.assertEquals("/a",Configurable.resolve1(matched,allNodes,"${path(../../../..)}",ctx));
    }

    @Test
    public void cmdPathTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("/a=b/c=d/e",Configurable.resolve1(matched,allNodes,"${cmdpath(${path(.)})}",ctx));
        Assert.assertEquals("/subsystem=x/a=b/c=d/e",Configurable.resolve1(matched,allNodes,"/subsystem=x${cmdpath(${path(.)})}",ctx));
        Assert.assertEquals("/subsystem=a/b=c/d=e",Configurable.resolve1(matched,allNodes,"/subsystem${cmdpath(=${path(.)})}",ctx));
    }

    @Test
    public void ifTest() {
        PathExpression matched=new PathExpression("a","b","c","d");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("/blah,\"f\"",Configurable.resolve1(matched,allNodes,"/blah${if-defined (e),(,${value(e)})}",ctx));
        Assert.assertEquals("/blah",Configurable.resolve1(matched,allNodes,"/blah${if-defined (g),(,${value(g)})}",ctx));
    }
}
