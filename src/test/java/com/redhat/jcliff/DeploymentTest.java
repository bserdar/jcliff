package com.redhat.jcliff;

import java.io.StringBufferInputStream;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.jboss.dmr.ModelNode;

public class DeploymentTest {

    @Test
    public void replace() throws Exception {
        ModelNode newDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>\"deleted\", \"app2\"=>{\"NAME\"=>\"app2\",\"path\"=>\"blah\"} }")); 
        ModelNode existingDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>{\"NAME\"=>\"app1\"},\"app2\"=>{\"NAME\"=>\"app2\"}}"));

        Deployment d=new Deployment(new Ctx(),true);
        Map<String,Set<String>> map=d.findDeploymentsToReplace(newDeployments,existingDeployments);
        Assert.assertEquals("app2",map.get("app2").iterator().next());
        Assert.assertNull(map.get("app1"));
        Assert.assertEquals(1,map.size());
    }

    @Test
    public void newdeployments() throws Exception {
        ModelNode newDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>\"deleted\", \"app2\"=>{\"NAME\"=>\"app2\",\"path\"=>\"blah\"},\"app3\"=>{\"NAME\"=>\"app3\"} }")); 
        ModelNode existingDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>{\"NAME\"=>\"app1\"},\"app2\"=>{\"NAME\"=>\"app2\"}}"));

        Deployment d=new Deployment(new Ctx(),true);
        String[] news=d.getNewDeployments(newDeployments,existingDeployments.keys());
        Assert.assertEquals(1,news.length);
        Assert.assertEquals("app3",news[0]);
    }

    @Test
    public void undeployments() throws Exception {
        ModelNode newDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>\"deleted\", \"app2\"=>{\"NAME\"=>\"app2\",\"path\"=>\"blah\"},\"app3\"=>{\"NAME\"=>\"app3\"}, \"app4\"=>\"deleted\" }")); 
        ModelNode existingDeployments=ModelNode.fromStream(new StringBufferInputStream("{\"app1\"=>{\"NAME\"=>\"app1\"},\"app2\"=>{\"NAME\"=>\"app2\"}}"));

        Deployment d=new Deployment(new Ctx(),true);
        String[] und=d.getUndeployments(newDeployments,existingDeployments);
        Assert.assertEquals(1,und.length);
        Assert.assertEquals("app1",und[0]);
    }
}
