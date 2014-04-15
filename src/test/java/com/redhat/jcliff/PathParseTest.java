package com.redhat.jcliff;

import java.util.List;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;

public class PathParseTest {

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

        Assert.assertEquals("e",Configurable.resolve(matched,allNodes,"${name(.)}"));
        Assert.assertEquals("d",Configurable.resolve(matched,allNodes,"${name(..)}"));
        Assert.assertEquals("c",Configurable.resolve(matched,allNodes,"${name(../..)}"));
        Assert.assertEquals("b",Configurable.resolve(matched,allNodes,"${name(../../..)}"));
        Assert.assertEquals("a",Configurable.resolve(matched,allNodes,"${name(../../../..)}"));
    }

    @Test
    public void valueTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals(str("'f'"),Configurable.resolve(matched,allNodes,"${value(.)}"));
        Assert.assertEquals(str("{'e' => 'f'}"),Configurable.resolve(matched,allNodes,"${value(..)}"));
    }

    @Test
    public void pathTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("/a/b/c/d/e",Configurable.resolve(matched,allNodes,"${path(.)}"));
        Assert.assertEquals("/a/b/c/d",Configurable.resolve(matched,allNodes,"${path(..)}"));
        Assert.assertEquals("/a/b/c",Configurable.resolve(matched,allNodes,"${path(../..)}"));
        Assert.assertEquals("/a/b",Configurable.resolve(matched,allNodes,"${path(../../..)}"));
        Assert.assertEquals("/a",Configurable.resolve(matched,allNodes,"${path(../../../..)}"));
    }

    @Test
    public void cmdPathTest() {
        PathExpression matched=new PathExpression("a","b","c","d","e");
        List<NodePath> allNodes=new ArrayList<NodePath>();
        ModelNode node=ModelNode.fromString(str("{'a' => { 'b' => { 'c' => { 'd' => { 'e' => 'f' } } } } }"));
        add(allNodes,node,"a","b","c","d","e");

        Assert.assertEquals("/a=b/c=d/e",Configurable.resolve(matched,allNodes,"${cmdpath(${path(.)})}"));
        Assert.assertEquals("/subsystem=x/a=b/c=d/e",Configurable.resolve(matched,allNodes,"/subsystem=x${cmdpath(${path(.)})}"));
        Assert.assertEquals("/subsystem=a/b=c/d=e",Configurable.resolve(matched,allNodes,"/subsystem${cmdpath(=${path(.)})}"));
    }
}
