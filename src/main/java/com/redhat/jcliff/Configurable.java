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
import java.util.Set;
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
                sarr[i]=sarr[i].trim();
                if(sarr[i].startsWith("{") &&
                   sarr[i].endsWith("}")) {
                    result[i]=ModelNode.fromString(sarr[i]);
                    if(result[i].has("outcome")) {
                        ModelNode outcome=result[i].get("outcome");
                        if(outcome.asString().equals("failed")) 
                            throw new RuntimeException("Operation failed:"+result[i].asString());
                    } else {
                        Set<String> keys=result[i].keys();
                        for(String x:keys) 
                            if(x.startsWith("JBAS"))
                                throw new RuntimeException("Operation failed:"+result[i].asString());
                    }
                } else {
                    result[i]=new ModelNode();
                    result[i].set(sarr[i]);
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
     * Return the prefix rules for the given action. If action is
     * null, all rules are returned.
     *
     * Rules are of the form:
     *  prefix.<ruleName>=<action>:<mask>
     */
    public List<MatchRule> getPrefixRules(Action action) {
        List<MatchRule> rules=new ArrayList<MatchRule>();
        for(Iterator itr=properties.keySet().iterator();itr.hasNext();) {
            String key=(String)itr.next();
            if(key.startsWith("prefix.")) {
                String ruleName=key.substring("prefix.".length());
                String value=properties.getProperty(key);
                String[] str=splitMatchString(value);
                if(str!=null)
                    if(action==null||action.toString().equals(str[0])) {
                        rules.add(new MatchRule(Action.valueOf(str[0]),ruleName,0,str[1]));
                    }
            }
        }
        return rules;
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
                                     return s1.precedence>s2.precedence?1:s1.precedence==s2.precedence?0:-1;
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

    public Script getScript(String ruleName,PathExpression matchedPath,List<NodePath> allPaths, Ctx ctx) {
        String[] script=getProperties(properties,ruleName+".rule");
        List<String> out=new ArrayList<String>();
        if(script!=null) {
            for(int i=0;i<script.length;i++)
                for(String x:sanitizeScript(resolve(matchedPath,allPaths,script[i],ctx)))
                    out.add(x);
        }
        return new Script(out);
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
    
    private String[] sanitizeScript(String[] script) {
        for(int i=0;i<script.length;i++)
            script[i]=sanitizeScript(script[i]);
        return script;
    }

    

    public static String resolve1(PathExpression matchedPath,List<NodePath> allPaths,String str,Ctx ctx) {
        String[] arr=resolve(matchedPath,allPaths,str,ctx);
        if(arr.length!=1)
            throw new RuntimeException("Expected one result, got "+arr.length+" for "+str);
        return arr[0];
    }
    
    public static String[] resolve(PathExpression matchedPath,List<NodePath> allPaths,String str,Ctx ctx) {
        List<StringBuffer> ret=new ArrayList<StringBuffer>();
        StringBuffer output=new StringBuffer();
        StringBuffer buf=null;
        int state=0;
        int n=str.length();
        int depth=0;
        ret.add(output);
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
                    depth=1;
                    buf=new StringBuffer();
                } else
                    throw new RuntimeException("Syntax error near offset "+i+":"+str);
                break;

            case 2:
                if(c=='{') {
                    depth++;
                    buf.append(c);
                } else if(c=='}') {
                    depth--;
                    if(depth==0) {
                        String[] s=func(buf.toString(),matchedPath,allPaths,ctx);
                        if(s==null||s.length==0)
                            throw new RuntimeException("Cannot resolve "+buf.toString()+" in "+str);
                        for(int j=0;j<s.length;j++) {
                            output.append(s[j]);
                            if(j+1<s.length) {
                                output=new StringBuffer();
                                ret.add(output);
                            }
                        }
                        state=0;
                        buf=null;
                    } else
                        buf.append(c);
                } else
                    buf.append(c);
                break;
            }
        }
        if(state!=0)
            throw new RuntimeException("Unterminated reference:"+str);
        List<String> sret=new ArrayList<String>();
        for(StringBuffer x:ret) {
            String s=x.toString();
            if(s.length()>0)
                sret.add(s);
        }
        return sret.toArray(new String[sret.size()]);
    }

    public boolean needsRefresh(String ruleName) {
        return "true".equals(properties.get(ruleName+".refresh"));
    }

    private static String nodeToString(ModelNode node) {
        // If we're getting the string value of an object, we have to
        // remove the "deleted" nodes from that string value
        if(node.getType().equals(ModelType.OBJECT) ) {
            ModelNode copy=(ModelNode)node.clone();
            removeDeletedNodes(copy);
            return copy.toString();
        } else {
            return node.toString();
        }
    }

    private static void removeDeletedNodes(ModelNode node) {
        ModelType type=node.getType();
        if(type==ModelType.OBJECT) {
            List<String> removeKeys=new ArrayList<String>();
            Set<String> keys=node.keys();
            for(String x:keys) {
                ModelNode child=node.get(x);
                if(NodePath.isPrimitive(child.getType())) {
                    if(child.asString().equals("deleted"))
                        removeKeys.add(x);
                } else {
                    removeDeletedNodes(child);
                }
            }
            for(String x:removeKeys)
                node.remove(x);
        }
    }
    
    /**
     * str is of the form funcName(expr)
     */
    private static String[] func(String str,PathExpression matchedPath,List<NodePath> allPaths,Ctx ctx) {
        List<String> ret=new ArrayList<String>();
        int index=str.indexOf('(');
        int lastIndex=str.lastIndexOf(')');
        if(index==-1||lastIndex==-1)
            throw new RuntimeException("Cannot interpret "+str);
        String function=str.substring(0,index).trim();
        List<String> args=split(str.substring(index,lastIndex+1));
        if(function.equals("name")) {
            if(args.size()!=1)
                throw new RuntimeException("Syntax error in "+str);
            String pathExpr=resolve1(matchedPath,allPaths,args.get(0),ctx);
            PathExpression expr=PathExpression.parse(pathExpr);
            PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
            ret.add(absolute.last());
        } else if(function.equals("value")) {
            if(args.size()!=1)
                throw new RuntimeException("Syntax error in "+str);
            String pathExpr=resolve1(matchedPath,allPaths,args.get(0),ctx);
            PathExpression expr=PathExpression.parse(pathExpr);
            PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
            NodePath p=NodePath.find(allPaths,absolute);
            if(p==null)
                throw new RuntimeException("Cannot resolve "+str+" with respect to "+matchedPath);
            ret.add(nodeToString(p.node));
        } else if(function.equals("path")) {
            if(args.size()!=1)
                throw new RuntimeException("Syntax error in "+str);
            String pathExpr=resolve1(matchedPath,allPaths,args.get(0),ctx);
            PathExpression expr=PathExpression.parse(pathExpr);
            PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
            ret.add(absolute.toString());
        } else if(function.equals("cmdpath")) {
            if(args.size()!=1)
                throw new RuntimeException("Syntax error in "+str);
            String arg=resolve1(matchedPath,allPaths,args.get(0),ctx);
            boolean name;
            if(arg.startsWith("=")) {
                arg=arg.substring(1);
                name=false;
            } else
                name=true;
            PathExpression p=PathExpression.parse(arg);
            int n=p.size();
            StringBuilder buf=new StringBuilder();
            for(int i=0;i<n;i++) {
                if(name)  {
                    buf.append('/').append(p.get(i));
                } else {
                    buf.append('=').append(p.get(i));
                }
                name=!name;
            }
            ret.add(buf.toString());
        } else if(function.equals("if-defined")) {
            if(args.size()==2||args.size()==3) {
                String pathExpr=resolve1(matchedPath,allPaths,args.get(0),ctx);
                PathExpression expr=PathExpression.parse(pathExpr);
                PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
                NodePath p=NodePath.find(allPaths,absolute);
                String evalPart;
                if(p!=null) {
                    // evaluate the true part
                    evalPart=args.get(1);
                } else {
                    // evaluate the else part, if there is one
                    if(args.size()==3)
                        evalPart=args.get(2);
                    else
                        evalPart=null;
                }
                if(evalPart!=null) {
                    ret.add(resolve1(matchedPath,allPaths,evalPart,ctx));
                } else {
                    ret.add("");
                }
            } else
                throw new RuntimeException("if-defined needs 2 or 3 args in "+str);
        } else if(function.equals("foreach-server")||
                  function.equals("foreach-cfg")) {
            int nArgs=args.size();
                if(nArgs<2 )
                    throw new RuntimeException("foreach requires at least 2 args in "+str);
                String pathExpr=resolve1(matchedPath,allPaths,args.get(0),ctx);
                PathExpression expr=PathExpression.parse(pathExpr);
                PathExpression absolute=NodePath.getAbsolutePath(matchedPath,expr);
                List<String> children=getChildren(function.equals("foreach-server")?ctx.serverPaths:
                                                  ctx.configPaths,absolute);
                // Iterate all children
                for(String x:children) {
                    PathExpression loopMatchedPath=new PathExpression(absolute,x);
                    for(int i=1;i<nArgs;i++) {
                        String[] values=resolve(loopMatchedPath,allPaths,args.get(i),ctx);
                        for(String s:values)
                            ret.add(s);
                    }
                }
        } else
            throw new RuntimeException("Cannot process "+function);
        return ret.toArray(new String[ret.size()]);
    }


    public static List<String> getChildren(List<NodePath> paths,PathExpression path) {
        List<String> list=new ArrayList<String>();
        NodePath p=NodePath.find(paths,path);
        if(p!=null) {
            list.addAll(p.node.keys());
        }
        return list;
    }

    /**
     * Parse arguments in parantheses delimited by commas
     *
     * <pre>
     *   (arg1),(arg2),...
     * </pre>
     * 
     * Returns list containing arg1, arg2,...
     */
    private static List<String> split(String s) {
        int state=0;
        List<Character> stack=new ArrayList<Character>();
        int n=s.length();
        List<String> ret=new ArrayList<String>();
        StringBuilder buf=null;
        for(int i=0;i<n;i++) {
            char c=s.charAt(i);
            switch(state) {
            case 0: // Initial state, expecting an open paren
                if(Character.isWhitespace(c)) {
                } else if(c=='(') {
                    state=1;
                    buf=new StringBuilder();
                } else
                    throw new RuntimeException("Syntax error parsing "+s);
                break;

            case 1: // Parsing an arg, continue until an un-escaped close paren
                if(c=='\\') {
                    state=10;
                } else if(c=='{'||c=='(') {
                    buf.append(c);
                    stack.add(c);
                } else if(c=='}') {
                    if(stack.isEmpty()||
                       stack.get(stack.size()-1)!='{')
                        throw new RuntimeException("Mismatched "+c+" in "+s);
                    stack.remove(stack.size()-1);
                    buf.append(c);
                } else if(c==')') {
                    if(stack.isEmpty()) {
                        ret.add(buf.toString());
                        state=2;
                    } else if(stack.get(stack.size()-1)!='(') {
                        throw new RuntimeException("Mismatched "+c+" in "+s);
                    } else {
                        stack.remove(stack.size()-1);
                        buf.append(c);
                    }
                } else if(c=='\"') {
                    buf.append(c);
                    state=3;
                } else {
                    buf.append(c);
                }
                break;

            case 2: // Arg completed, waiting for a comma, or end
                if(c==',') {
                    state=0;
                } else if(!Character.isWhitespace(c))
                    throw new RuntimeException("Syntax error parsing "+s);
                break;

            case 3: // In a quoted string. Continue until next quote
                if(c=='\\') {
                    state=11;
                } else if(c=='\"') {
                    buf.append(c);
                    state=1;
                } else
                    buf.append(c);
                break;

            case 10: // Escape char seen, copy verbatim
                buf.append(c);
                state=1;
                break;

            case 11: // Escape char seen in string, copy verbatim
                buf.append(c);
                state=3;
                break;
            }
        }
        // End state is 2
        if(state!=2)
            throw new RuntimeException("Syntax error parsing "+s);
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
