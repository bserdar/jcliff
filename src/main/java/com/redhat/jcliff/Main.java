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

import java.io.*;
import java.util.*;
import java.net.*;

import org.jboss.dmr.*;

/**
 * @author bserdar@redhat.com
 */
public class Main {

    private static final String VERSION="2.10.16";

    private static final HashSet<String> specialRules=new HashSet<String>();

    static {
        specialRules.add(Deployment.NAME);
    }

    private static final String HELP=
        "Usage:\n"+
        "    jcliff [options] file(s)\n"+
        "where options are:\n"+
        "  --cli=Path : jboss-cli.sh. Defaults to \n"+
        "               /usr/share/jbossas/bin/jboss-cli.sh\n"+
        "  --controller=host       : EAP6 host. Defaults to localhost.\n"+
        "  --user=username         : EAP6 admin user name\n"+
        "  --password=pwd          : EAP6 admin password\n"+
        "  --ruledir=Path          : Location of jcliff rules.\n"+
        "  --noop                  : Read-only mode\n"+
        "  --json                  : Use json to parse input files\n"+
        "  -v                      : Verbose output\n"+
        "  --timeout=timeout       : Command timeout in milliseconds\n"+
        "  --output=Path           : Log output file\n"+
        "  --reload                : Reload after each subsystem configuration if required\n"+
        "  --waitport=waitport     : Wait this many seconds for the port to be opened\n"+
        "  --nobatch               : Don't use batch mode of jboss-cli\n"+
        "  --redeploy              : Redeploy all apps\n"+
        "  --reconnect-delay=delay : Wait this many milliseconds after a :reload for the server to restart\n"+
        "  --leavetmp              : Don't erase temp files";

    public static void println(int indent,String s) {
        for(int i=0;i<indent;i++)
            System.out.print("  ");
        System.out.println(s);
    }

    public static void printNode(int indent,ModelNode node) {
        switch(node.getType()) {
        case LIST:
            List<ModelNode> l=node.asList();
            println(indent,"LIST:");
            for(ModelNode x:l)
                printNode(indent+1,x);
            break;
        case OBJECT:
            println(indent,"OBJECT:");
            Set<String> keys=node.keys();
            for(String x:keys) {
                println(indent+1,"Key:"+x);
                printNode(indent+1,node.get(x));
            }
            break;
        case PROPERTY:
            println(indent,"PROPERTY:"+node.asProperty().getName());
            printNode(indent+1,node.asProperty().getValue());
            break;
        default:
            println(indent,node.getType()+": "+node.toString());
            break;
        }
    }

    public static void printTree(ModelNode node) {
        System.out.println(node);
        printNode(0,node);
    }

    private static class RuleLoader implements RuleSet.RuleAccessor {
        private final File ruleDir;

        public RuleLoader(String dir) {
            System.out.println("Setting ruledir to:"+dir);
            ruleDir=new File(dir);
        }

        public Properties loadProperties(String name) {
            Properties p=new Properties();
            try {
                System.out.println("Reading properties from file: " + new File(ruleDir,name).toString());
                InputStream stream=new FileInputStream(new File(ruleDir,name));
                p.load(stream);
                stream.close();
            } catch (Exception e) {
                throw new RuntimeException("Cannot load "+name+":"+e);
            }
            return p;
        }
    }

    public static void main(String[] args) throws Exception {
        boolean log=false;
        boolean noop=false;
        boolean json=false;
        String cli="/usr/share/jbossas/bin/jboss-cli.sh";
        String controller="localhost";
        String user=null;
        String password=null;
        String logOutput=null;
        List<String> files=new ArrayList<String>();
        String ruleDir=null;
        String timeout=null;
        int     waitport=0;
        boolean reload=false;
        boolean redeploy=false;
        boolean batch=true;
        String reconnectDelay="20000";
        boolean leaveTmp=false;

        System.out.println("Jcliff version "+VERSION);
        for(int i=0;i<args.length;i++) {
            if(args[i].startsWith("--cli="))
                cli=args[i].substring("--cli=".length());
            else if(args[i].startsWith("--controller="))
                controller=args[i].substring("--controller=".length());
            else if(args[i].startsWith("--user="))
                user=args[i].substring("--user=".length());
            else if(args[i].startsWith("--password="))
                password=args[i].substring("--password=".length());
            else if(args[i].equals("-v"))
                log=true;
            else if(args[i].startsWith("--output="))
                logOutput=args[i].substring("--output=".length());
            else if(args[i].equals("--noop"))
                noop=true;
            else if(args[i].equals("--json"))
                json=true;
            else if(args[i].startsWith("--ruledir="))
                ruleDir=args[i].substring("--ruledir=".length());
            else if(args[i].equals("--nobatch"))
                batch=false;
            else if(args[i].startsWith("--timeout=")) {
                timeout = args[i].substring("--timeout=".length());
                if ("0".equals(timeout.trim())) {
                    timeout = null;
                }
            } else if(args[i].startsWith("--reconnect-delay=")) {
                reconnectDelay=args[i].substring("--reconnect-delay=".length());
            }  else if(args[i].startsWith("--waitport="))
                waitport=Integer.parseInt(args[i].substring("--waitport=".length()));
            else if(args[i].equals("--reload"))
                reload=true;
            else if(args[i].equals("--redeploy"))
                redeploy=true;
            else if(args[i].equals("--leavetmp"))
                leaveTmp=true;
            else
                files.add(args[i]);
        }
        RuleSet rules=RuleSet.getRules(new RuleLoader(ruleDir),"rules");
        if(!files.isEmpty()) {
            Ctx ctx=new Ctx();
            ctx.noop=noop;
            ctx.log=log;
            ctx.batch=batch;
            ctx.leaveTmp=leaveTmp;
            ctx.reconnectDelay=Long.valueOf(reconnectDelay);
            if(logOutput!=null)
                ctx.out=new PrintStream(new File(logOutput));
            if ( waitport != 0 ) {
            Socket s = null;
            String host = controller.split(":")[0];
            int    port = Integer.parseInt(controller.split(":")[1]);
            try {
                Socket client = new Socket(host, port);
                client.close();
            } catch (Exception e) {
                System.out.println("waiting "+waitport+" seconds for server to be up up");
                try { Thread.sleep(waitport*1000); } catch(InterruptedException f) {}
            } ;
            }
            ctx.log("Jcliff version "+VERSION+" running");
            ctx.cli=new Cli(cli,controller,user,password,timeout,ctx);
            try {
                List<ModelNode> nodes=new ArrayList<ModelNode>();
                Set<String> systemNames=new HashSet<String>();
                for(String file:files) {
                    ctx.log("Opening "+file);
                    FileInputStream is=new FileInputStream(file);
                    ModelNode node;
                    if(json) {
                        node=ModelNode.fromJSONStream(is);
                    } else {
                        node=ModelNode.fromStream(is);
                    }
                    is.close();
                    // We expect an object from the file
                    if(node.getType()!=ModelType.OBJECT)
                        throw new RuntimeException("Expecting an object in "+file);
                    for(String x:node.keys())
                        if(specialRules.contains(x)==false&&rules.get(x)==null)
                            throw new RuntimeException("Unknown object:"+x+" in "+file);
                        else if(!specialRules.contains(x))
                            systemNames.add(x);
                    nodes.add(node);
                }

                for(String system:rules.getSystemNames()) {
                    if(systemNames.contains(system)) {
                        ctx.cmdsRun=new HashSet<Script>();
                        ctx.log("Processing "+system);
                        Configurable cfg=rules.get(system);
                        refreshServerNode(ctx,system,rules);
                        for(ModelNode configNode:nodes) {
                            if(configNode.hasDefined(system)) {

                                ModelNode newNode=new ModelNode();
                                newNode.setEmptyObject();
                                newNode.get(system).set(configNode.get(system));
                                configNode=newNode;
                                configNode=cfg.applyClientPreprocessingRules(configNode);
                                ctx.log("Configuration node after preprocessing:"+configNode);
                                ctx.currentConfigNode=configNode;

                                ctx.configPaths=NodePath.getPaths(configNode);
                                boolean refresh=false;
                                do {
                                    refresh=false;
                                    ctx.serverPaths=NodePath.getPaths(ctx.currentServerNode);
                                    List<NodeDiff> difference=NodeDiff.computeDifference(ctx,ctx.configPaths,
                                                                                         ctx.serverPaths);
                                    for(NodeDiff x:difference)
                                        ctx.log("Diff:"+x.toString());
                                    refresh=executeRules(ctx,cfg,difference);
                                    if(refresh)
                                        ctx.runQueuedCmds(cfg.getScriptResultPostprocessor());
                                    if(refresh)
                                        refreshServerNode(ctx,system,rules);
                                } while(refresh);
                                if(ctx.hasQueuedCmds())
                                    ctx.runQueuedCmds(cfg.getScriptResultPostprocessor());
                            }
                        }
                    }
                    if(reload)
                        if(reloadRequired(ctx))
                            ctx.reloadConf();
                    
                }
                
                // Deal with deployments
                ctx.log("Processing deployments");
                ctx.batch=false; // Don't batch them
                Deployment deployment=new Deployment(ctx,redeploy);
                ModelNode allRequestedDeployments=new ModelNode();
                allRequestedDeployments.setEmptyObject();
                boolean noDeployments=true;
                for(ModelNode requestedDeployments:nodes) {
                    if(requestedDeployments.hasDefined(Deployment.NAME)) {
                        ModelNode root=requestedDeployments.get(Deployment.NAME);
                        Set<String> deploymentNames=root.keys();
                        for(String x:deploymentNames) {
                            allRequestedDeployments.get(x).set(root.get(x));
                            noDeployments=false;
                        }
                    }
                }
                ModelNode originalReq=(ModelNode)allRequestedDeployments.clone();
                ctx.log("All requested deployments:"+allRequestedDeployments);
                if(!noDeployments) {
                    ModelNode currentDeployments=deployment.getCurrentDeployments();
                    Set<String> currentDeploymentNames=currentDeployments.keys();
                    ctx.log("Current deployments:"+currentDeployments);
                    Map<String,Set<String>> replaceList=deployment.
                        findDeploymentsToReplace(allRequestedDeployments,currentDeployments);
                    if(replaceList!=null&&replaceList.size()>0) {
                        for(Set<String> x:replaceList.values())
                            currentDeploymentNames.removeAll(x);
                    }

                    String[] names=deployment.getNewDeployments(allRequestedDeployments,
                                                                currentDeploymentNames);
                    for(String x:names)
                        if(!replaceList.containsKey(x))
                            replaceList.put(x,null);
                    if(replaceList.size()>0)
                        deployment.deployUpdate(replaceList,allRequestedDeployments);

                    ctx.log("Checking undeploy");
                    ctx.log("All requested deployments:"+originalReq);
                    ctx.log("Current deployments:"+currentDeployments);
                    names=deployment.getUndeployments(originalReq,currentDeployments);
                    ctx.log("There are "+names.length+" apps to undeploy");
                    for(String x:names)
                        ctx.log("undeploy:"+x);
                    if(names.length>0)
                        deployment.undeploy(names);
                }

                if(reload)
                    if(reloadRequired(ctx))
                        ctx.reloadConf();
            } catch (Exception t) {
                ctx.error(t);
                throw t;
            }
        } else
            System.out.println(HELP);
    }


    private static boolean reloadRequired(Ctx ctx) {
        Script script=new Script(new String[] {"ls"});
        String output=ctx.cli.run(script);
        return output.indexOf("server-state=reload-required")!=-1;
    }

    private static boolean executeRules(Ctx ctx,Configurable cfg,List<NodeDiff> ldiff) {
        List<MatchRule> matchRules=cfg.getMatchRules(null);
        List<MatchRule> prefixRules=cfg.getPrefixRules(null);
        List<NodeDiff> norule=new ArrayList<NodeDiff>();
        for(NodeDiff diff:ldiff)
            norule.add(diff);

        if(execute(ctx,cfg,ldiff,matchRules,norule,RULE_MATCHER))
            return true;
        if(!norule.isEmpty()) {
            if(execute(ctx,cfg,ldiff,prefixRules,norule,PREFIX_MATCHER))
                return true;
        }
        if(!norule.isEmpty()) {
            StringBuffer buf=new StringBuffer();
            for(NodeDiff x:norule)
                buf.append(x.toString()).append('\n');
            ctx.log("No rules for diffs:"+buf.toString());
        }
        return false;
    }

    interface Matcher {
        boolean matches(PathExpression rulePath,PathExpression diffPath);
    }

    private static final class RuleMatcher implements Matcher {
        public boolean matches(PathExpression rulePath,PathExpression diffPath) {
            return rulePath.matches(diffPath);
        }
    }


    private static final class PrefixRuleMatcher implements Matcher {
        public boolean matches(PathExpression rulePath,PathExpression diffPath) {
            return rulePath.prefixOf(diffPath);
        }
    }

    private static final Matcher RULE_MATCHER=new RuleMatcher();
    private static final Matcher PREFIX_MATCHER=new PrefixRuleMatcher();

    private static boolean execute(Ctx ctx,Configurable cfg,List<NodeDiff> ldiff,List<MatchRule> rules,List<NodeDiff> norule,Matcher matcher) {
        for(MatchRule rule:rules) {
            ctx.log("Checking rule "+rule.name);
            for(NodeDiff diff:ldiff) {
                if(diff.action==rule.action) {
                    if(matcher.matches(rule.expr,diff.configPath.path)) {
                        norule.remove(diff);
                        ctx.log(rule.name+" will be run on "+diff);
                        boolean rerun=false;
                        Script script=cfg.getScript(rule.name,diff.configPath.path,ctx.configPaths,ctx);
                        if(script!=null) {
                            ctx.log("run:"+script);
                            if(ctx.cmdsRun!=null)
                                if(ctx.cmdsRun.contains(script)) {
                                    rerun=true;
                                    System.err.println("re-run:"+script);
                                }
                            ctx.queueCmd(script);
                        }
                        // Check if we need to refresh
                        if(!rerun&&cfg.needsRefresh(rule.name))
                            return true;
                    }
                }
            }
        }
        return false;
    }

    private static void refreshServerNode(Ctx ctx,String system,RuleSet rules) {
        ctx.log("Reading current status of "+system);
        Configurable cfg=rules.get(system);
        ModelNode[] nodearr=ctx.runcmd(cfg.getContentsExpr(),cfg.getGetContentPostprocessor());
        ModelNode node=nodearr[nodearr.length-1];
        if(node.has("result"))
            node=node.get("result");
        else
            throw new RuntimeException("Cannot get node result from "+node);
        ctx.log("Node from server:"+node);
        ctx.currentServerNode=cfg.applyServerPreprocessingRules(node);
        ctx.log("After preprocessing:"+ctx.currentServerNode);
    }
}
