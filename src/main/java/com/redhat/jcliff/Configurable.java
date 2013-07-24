/*
    Copyright 2013 Red Hat, Inc. and/or its affiliates.

    This file is part of jcliff.

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

import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import java.io.BufferedReader;
import java.io.StringReader;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author bserdar@redhat.com
 */
public class Configurable {

    private final String name;
    private Script contentsExpr;
    private Properties properties;
    private Postprocessor getContentPostprocessor=new DefaultPostprocessor();
    private Postprocessor scriptResultPostprocessor=new DefaultPostprocessor();

    public static final class DefaultPostprocessor implements Postprocessor {
        public ModelNode[] process(String output) {
            String[] sarr=parseArray(output);
            ModelNode[] result=new ModelNode[sarr.length];
            for(int i=0;i<result.length;i++) {
                if(sarr[i].startsWith("{") &&
                   sarr[i].endsWith("}")) {
                    result[i]=ModelNode.fromString(sarr[i]);
                    ModelNode outcome=result[i].get("outcome");
                    if(outcome!=null||outcome.asString().equals("success")) {
                        result[i]=result[i].get("result");
                    } else
                        throw new RuntimeException("Operation failed:"+result[i].asString());
                }
            }
            return result;
        }

        // When multiple commands are sent, the jboss client returns the 
        // results as a list of return values. Have to split the list
        // into individual elements
        private String[] parseArray(String s) {
            int depth=0;
            int state=0;
            boolean quote=false;
            List<String> output=new ArrayList<String>();
            StringBuffer buf=null;
            int n=s.length();
            for(int i=0;i<n;i++) {
                char c=s.charAt(i);
                switch(state) {
                case 0: // Beginning of line/object
                    if(c=='{') {
                        state=1;
                        buf=new StringBuffer();
                        buf.append(c);
                        depth=1;
                    } else if(Character.isWhitespace(c))
                        ;
                    else {
                        state=2;
                        buf=new StringBuffer();
                        buf.append(c);
                    }
                    break;
                    
                case 1: // Parsing object, match {}
                    buf.append(c);
                    if(c=='\"')
                        quote=!quote;
                    else if(c=='{'&&!quote)
                        depth++;
                    else if(c=='}'&&!quote) {
                        depth--;
                        if(depth==0) {
                            output.add(buf.toString());
                            buf=null;
                            state=0;
                        }
                    }
                    break;

                case 2: // Parsing text, go until the end of line
                    if(c=='\n') {
                        output.add(buf.toString());
                        buf=null;
                        state=0;
                    } else
                        buf.append(c);
                    break;
                }
            }
            return output.toArray(new String[output.size()]);
        }
    }


    private static final class StringPostprocessor implements Postprocessor {
        public ModelNode[] process(String output) {
            ModelNode node=new ModelNode();
            node.set(output);
            return new ModelNode[] {node};
        }
    }

    public Configurable(String name) {
        this.name=name;
    }

    public String getName() {
        return name;
    }

    public Script getContentsExpr() {
        return contentsExpr;
    }

    public ModelNode applyServerPreprocessingRules(ModelNode node) {
        return applyPreprocessingRules("server.preprocess",node);
    }

    public ModelNode applyClientPreprocessingRules(ModelNode node) {
        return applyPreprocessingRules("client.preprocess",node);
    }

    public Postprocessor getGetContentPostprocessor() {
        return getContentPostprocessor;
    }

    public Postprocessor getScriptResultPostprocessor() {
        return scriptResultPostprocessor;
    }

    public void setGetContentPostprocessor(Postprocessor p) {
        this.getContentPostprocessor=p;
    }
    public void setScriptResultPostprocessor(Postprocessor p) {
        this.scriptResultPostprocessor=p;
    }

    /**
     * Return the match rules for the given action, ordered by
     * precedence. If action is null, all rules are returned.
     *
     * Rules are of the form:
     *  match.<ruleName>=<action>:<mask>
     */
    public List<MatchRule> getMatchRules(Action action) {
        List<MatchRule> rules=new ArrayList<MatchRule>();
        for(Iterator itr=properties.keySet().iterator();itr.hasNext();) {
            String key=(String)itr.next();
            if(key.startsWith("match.")) {
                String ruleName=key.substring("match.".length());
                String precedenceStr=properties.getProperty(ruleName+".precedence");
                int precedence=-1;
                if(precedenceStr!=null)
                    precedence=Integer.valueOf(precedenceStr).intValue();
                String value=properties.getProperty(key);
                String[] str=splitMatchString(value);
                if(str!=null)
                    if(action==null||action.toString().equals(str[0])) {
                        rules.add(new MatchRule(Action.valueOf(str[0]),ruleName,precedence,str[1]));
                    }
            }
        }
        Collections.sort(rules,new Comparator<MatchRule> () {
                             public int compare(MatchRule s1,MatchRule s2) {
                                 if(s1.precedence==-1)
                                     if(s2.precedence==-1)
                                         return 0;
                                     else
                                         return 1;
                                 else if(s2.precedence==-1)
                                     return -1;
                                 else
                                     return s1.precedence>s2.precedence?1:-1;
                             }
                         });
        return rules;
                                 
    }

    public static String[] splitMatchString(String str) {
        if(str!=null) {
            str=str.trim();
            int index=str.indexOf(':');
            if(index!=-1) {
                String actionKey=str.substring(0,index).trim();
                String value=str.substring(index+1).trim();
                return new String[] {actionKey,value};
            }
        }
        return null;
    }

    public Script getScript(String ruleName,PathExpression matchedPath,List<NodePath> allPaths) {
        String[] script=getProperties(properties,ruleName+".rule");
        if(script!=null) {
            for(int i=0;i<script.length;i++)
                script[i]=sanitizeScript(resolve(matchedPath,allPaths,script[i]));
        }
        return new Script(script);
    }

    /**
     * Add \ at the end of lines
     *
     * @param script
     * @return
     */
    private String sanitizeScript(String script) {
	    return script.replaceAll("\\n", "\\\\\n");
    }

    private String resolve(PathExpression matchedPath,List<NodePath> allPaths,String str) {
        StringBuffer output=new StringBuffer();
        StringBuffer buf=null;
        int state=0;
        int n=str.length();
        for(int i=0;i<n;i++) {
            char c=str.charAt(i);
            switch(state) {
            case 0:
                if(c=='$')
                    state=1;
                else
                    output.append(c);
                break;

            case 1:
                if(c=='$') {
                    output.append(c);
                    state=0;
                } else if(c=='{') {
                    state=2;
                    buf=new StringBuffer();
                } else
                    throw new RuntimeException("Syntax error near offset "+i+":"+str);
                break;

            case 2:
                if(c=='}') {
                    String s=lookup(buf.toString(),matchedPath,allPaths);
                    if(s==null)
                        throw new RuntimeException("Cannot resolve "+buf.toString()+" in "+str);
                    output.append(s);
                    state=0;
                    buf=null;
                } else
                    buf.append(c);
                break;
            }
        }
        if(state!=0)
            throw new RuntimeException("Unterminated reference:"+str);
        return output.toString();
    }

    public boolean needsRefresh(String ruleName) {
        return "true".equals(properties.get(ruleName+".refresh"));
    }

    private String lookup(String str,PathExpression matchedPath,List<NodePath> allPaths) {
        int index=str.indexOf('(');
        int lastIndex=str.lastIndexOf(')');
        if(index==-1||lastIndex==-1)
            throw new RuntimeException("Cannot interpret "+str);
        String function=str.substring(0,index).trim();
        String pathExpr=str.substring(index+1,lastIndex).trim();

        PathExpression expr=PathExpression.parse(pathExpr);
        PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
        String ret;
        if(function.equals("name")) 
            ret=absolute.last();
        else if(function.equals("value")) {
            NodePath p=NodePath.find(allPaths,absolute);
            if(p==null)
                throw new RuntimeException("Cannot resolve "+str+" with respect to "+matchedPath);
            ret=p.node.toString();
        } else
            throw new RuntimeException("Cannot process "+function);
        return ret;
    }

    private ModelNode applyPreprocessingRules(String rule,ModelNode node) {
        String s=properties.getProperty(rule+".prepend");
        if(s!=null)
            return prepend(s,node);
        s=properties.getProperty(rule+".strip");
        if(s!=null)
            return strip(s,node);
        return node;
    }

    private ModelNode strip(String str,ModelNode node) {
        StringTokenizer tok=new StringTokenizer(str,"/");
        while(tok.hasMoreTokens()) {
            String s=tok.nextToken();
            ModelNode n=node.get(s);
            if(n==null)
                throw new RuntimeException("Cannot strip "+s+" from node:"+node);
            node=n;
        }
        return node;
    }

    private ModelNode prepend(String str,ModelNode node) {
        StringTokenizer tok=new StringTokenizer(str,"/");
        while(tok.hasMoreTokens()) {
            String s=tok.nextToken();
            ModelNode newNode=new ModelNode();
            newNode.setEmptyObject();
            if(node.getType().equals(ModelType.LIST))
                newNode.get(s).set(node.asList());
            else
                newNode.get(s).set(node.asObject());
            node=newNode;
        }
        return node;
    }

    static Configurable parseCfg(Properties p) {
        String name=p.getProperty("name");
        if(name==null)
            throw new RuntimeException("name required");
        Configurable c=new Configurable(name);
        String[] contents;
        contents=getProperties(p,"getContents");
        c.contentsExpr=new Script(contents);
            
        c.properties=p;
        return c;
    }

    /**
     * Given the key, reads either the single  property with that key,
     * or reads an array of properties with keys key.1, key.2,
     * etc.
     */
    public static String[] getProperties(Properties p,String key) {
        String x=p.getProperty(key);
        if(x!=null)
            return new String[] {x};
        else {
            List<String> values=new ArrayList<String>();
            for(int i=1;;i++) {
                x=p.getProperty(key+"."+i);
                if(x!=null)
                    values.add(x);
                else
                    break;
            }
            if(values.isEmpty())
                return null;
            else
                return values.toArray(new String[values.size()]);
        }
    }
    
}
