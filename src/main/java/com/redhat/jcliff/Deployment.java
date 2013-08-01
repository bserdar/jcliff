/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of jbosscff.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.redhat.jcliff;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;

import java.util.regex.Pattern;

import java.io.BufferedReader;
import java.io.StringReader;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Server object:
 * <pre>
 * {
 *    "<name>" => {
 *        "NAME" => name,
 *        "RUNTIME-NAME" => runtime-name,
 *        "ENABLED" => true|false,
 *        "STATUS"  => OK
 *    }
 * }
 * </pre>
 * JCliff configuration:
 * <pre>
 *  {
 *    "deployments" => {
 *      "<name>" => {
 *        "NAME" => name,
 *        "path" => path to file to deploy,
 *        "RUNTIME-NAME" => runtime name (optional),
 *        "replace-name-regex"   => if NAME of a deployed app matches this regex, 
 *                                  the existing version of the app will be undeployed first
 *        "replace-runtime-name-regex" => If the RUNTIME-NAME of a deployed app matches this regex,
 *                                        the existing version of the app will be updeployed first
 *        
 *    }
 *  }
 * </pre>
 * 
 * If both replace-name-regex and replace-runtime-name-regex are
 * specified, either can match to replace the deployment.
 */
public class Deployment {

    public static final String NAME="deployments";

    private final Ctx ctx;
    private final boolean redeploy;


    /**
     * Returns an object containing elements of the list as members
     * that are assigned to themselves.
     */
    private static final class ColumnListPostprocessor implements Postprocessor {
        public ModelNode[] process(String output) {
            ModelNode node=new ModelNode();
            node.setEmptyObject();

            BufferedReader reader=new BufferedReader(new StringReader(output));
            String line;
            List<String> columnNames=null;
            
            try {
                while((line=reader.readLine())!=null) {
                    StringTokenizer tok=new StringTokenizer(line," \t");
                    List<String> values=new ArrayList<String>();
                    while(tok.hasMoreTokens())
                        values.add(tok.nextToken());
                    if(!values.isEmpty()) {
                        if(columnNames==null)
                            columnNames=values;
                        else {
                            ModelNode child=node.get(values.get(0));
                            for(String col:columnNames)
                                child.get(col);
                            int i=0;
                            for(String val:values) {
                                child.get(columnNames.get(i++)).set(val);
                            }
                        }
                    }
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
            return new ModelNode[] {node};
        }
    }

    public Deployment(Ctx ctx,boolean redeploy) {
        this.ctx=ctx;
        this.redeploy=redeploy;
    }

    /**
     * Retrieves current deployments from server by running deploy -l
     * returned node is an object, with each deployment a child with deployment name
     */
    public ModelNode getCurrentDeployments() {
        ctx.log("deploy -l");
        return ctx.runcmd(new Script("deploy -l"),new ColumnListPostprocessor())[0];
    }

    /**
     * Returns the deployments in existingDeployments that needs to be replaced
     * 
     * @param newDeployments Object with keys containing the deployment names, and its children
     * @param existingDeployments Object with keys,each of which are existing deployments
     */
    public String[] findDeploymentsToReplace(ModelNode newDeployments,
                                             ModelNode existingDeployments) {
        Set<String> replaceSet=new HashSet<String>();
        
        Set<String> existingDeploymentNames=existingDeployments.keys();
        Set<String> newDeploymentNames=newDeployments.keys();
        if(redeploy) {
            for(String newDeployment:newDeploymentNames)
                if(existingDeploymentNames.contains(newDeployment))
                    replaceSet.add(newDeployment);
            ctx.log("Apps to redeploy:"+replaceSet);
        }

        for(String newDeploymentName:newDeploymentNames) {
            ModelNode newDeployment=newDeployments.get(newDeploymentName);
            String namePattern=getPattern(newDeployment,"replace-name-regex");
            String runtimeNamePattern=getPattern(newDeployment,"replace-runtime-name-regex");
            if(namePattern!=null)
                for(String x:existingDeploymentNames) {
                    ctx.log("Checking "+x+" matches "+namePattern);
                    if(Pattern.matches(namePattern,x)&&!newDeploymentName.equals(x))
                        replaceSet.add(x);
                }
            if(runtimeNamePattern!=null)
                for(String x:existingDeploymentNames) {
                    ModelNode node=existingDeployments.get(x);
                    String rt=node.get("RUNTIME-NAME").asString();
                    if(rt!=null&&rt.length()>0)
                        if(Pattern.matches(runtimeNamePattern,rt)&&!newDeploymentName.equals(x))
                            replaceSet.add(x);
                }
            ctx.log("Apps to redeploy after checking regexes:"+replaceSet);
        }
        return replaceSet.toArray(new String[replaceSet.size()]);
    }

    public String[] getNewDeployments(ModelNode newDeployments,
                                      ModelNode existingDeployments) {
        Set<String> newNames=newDeployments.keys();
        Set<String> existingNames=existingDeployments.keys();
        newNames.removeAll(existingNames);
        return newNames.toArray(new String[newNames.size()]);
    }
    
    public void undeploy(String[] names) {
        for(String name:names) {
            ctx.log("undeploy "+name);
            ctx.queueCmd("undeploy "+name);
        }
        ctx.runQueuedCmds(new Configurable.DefaultPostprocessor());
    }
    
    public void deploy(String[] names, ModelNode newDeployments) {
        for(String name:names) {
            ModelNode deployment=newDeployments.get(name);
            String path=deployment.has("path")?deployment.get("path").asString():null;
            String runtimeName=deployment.has("RUNTIME-NAME")?deployment.get("RUNTIME-NAME").asString():null;
            if(path==null)
                throw new RuntimeException("path is required in "+deployment);
            ctx.log("deploy "+path+" runtime-name="+runtimeName);
            ctx.queueCmd(new Script(new String[] {"deploy "+path+
                                                  " --name="+name+
                                                  (runtimeName==null?"":" --runtime-name="+runtimeName)}));
        }
        ctx.runQueuedCmds(new Configurable.DefaultPostprocessor());
    }

    private static String getPattern(ModelNode node,String childName) {
        if(node.has(childName)) {
            String pattern=node.get(childName).asString();
            if(pattern!=null)
                pattern=pattern.trim();
            if(pattern!=null&&pattern.length()>0)
                return pattern;
        }
        return null;
    }
}
