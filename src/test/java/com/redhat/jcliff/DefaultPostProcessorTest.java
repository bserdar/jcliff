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
        Assert.assertEquals(ModelType.OBJECT,ret[0].getType());
    }

    @Test
    public void failOut() throws Exception {
        ModelNode[] ret=processor.process("{\"JBAS014653: Composite operation failed and was rolled back. Steps that failed:\" => {\"Operation step-2\" => {\"JBAS014671: Failed services\" => {\"jboss.deployment.unit.\\\"assessment-service-ear-1.18.0.eap6.ear\\\".STRUCTURE\" => \"org.jboss.msc.service.StartException in service jboss.deployment.unit.\\\"assessment-service-ear-1.18.0.eap6.ear\\\".STRUCTURE: JBAS018733: Failed to process phase STRUCTURE of deployment \\\"assessment-service-ear-1.18.0.eap6.ear\\\"\n   Caused by: org.jboss.as.server.deployment.DeploymentUnitProcessingException: JBAS018746: Sub deployment assessment-service-util-1.18.0.eap6.jar in jboss-deployment-structure.xml was not found. Available sub deployments: assessment-service-ejb-1.18.0.eap6.jar, assessment-service-rest-1.18.0.eap6.war\"}}}}");
        Assert.assertEquals(1,ret.length);
        Assert.assertEquals(ModelType.OBJECT,ret[0].getType());
    }


                                          

}
