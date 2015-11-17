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

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;

import org.jboss.dmr.ModelType;
import org.jboss.dmr.ModelNode;

/**
 * @author bserdar@redhat.com
 * @author mpatercz@redhat.com
 * @author brose@redhat.com
 */
public class NodeDiff {
    public final NodePath configPath;
    public final NodePath serverPath;
    public final Action action;

    public NodeDiff(Action action,NodePath configPath,NodePath serverPath) {
        this.action=action;
        this.configPath=configPath;
        this.serverPath=serverPath;
    }

    public NodeDiff(NodePath configPath) {
        this.action=Action.add;
        this.configPath=configPath;
        this.serverPath=null;
    }

    public boolean equals(NodeDiff n) {
        if(n!=null)
            try {
                return action==n.action&&
                    ( (configPath==null&&n.configPath==null)||(configPath.equals(n.configPath)) )&&
                    ( (serverPath==null&&n.serverPath==null)||(serverPath.equals(n.serverPath)));
            } catch (Exception e) {}
        return false;
    }

    public boolean equals(Object o) {
        try {
            return equals( (NodeDiff)o);
        } catch (Exception e) {}
        return false;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        buf.append(action.toString()).append(':');
        if(configPath!=null)
            buf.append(configPath.toString());
        if(serverPath!=null)
            buf.append('(').append(serverPath.toString()).append(')');
        return buf.toString();
    }

    public static List<NodeDiff> computeDifference(Ctx ctx,List<NodePath> config,List<NodePath> server) {
        List<NodeDiff> diff=new ArrayList<NodeDiff>();
        // Keep modify rules in a different list. Once all
        // modifications are determined, filter out parent node
        // modifications, and leave only leaf levels
        List<NodeDiff> modifyList=new ArrayList<NodeDiff>();
        ctx.log("computeDiff");
        for(NodePath x:config)
            ctx.log("Config path:"+x);
        for(NodePath x:server)
            ctx.log("Server path:"+x);
        for(NodePath x:config) {
            boolean found=false;
            for(NodePath s:server)
                if(s.path.equals(x.path)) {
                    ctx.log("Processing path "+x.path+":"+x.node.getType());

                    found=true;
                    if(x.node.asString().equals("deleted")) {
                        ctx.log("Node deleted");
                        diff.add(new NodeDiff(Action.remove,x,null));
                    } else if(x.node.getType().equals(ModelType.UNDEFINED)) {
                        ctx.log("Node undefined");
                        if(!s.node.getType().equals(ModelType.UNDEFINED))
                            diff.add(new NodeDiff(Action.undefine,x,null));
                    } else if(x.node.getType().equals(ModelType.LIST)&&!isSubsetOf(x.node,s.node)) {
                        ctx.log("Node is list");
                        List<ModelNode> configList=x.node.asList();
                        List<ModelNode> serverList=s.node.getType().equals(ModelType.UNDEFINED)?
                            new ArrayList<ModelNode>():s.node.asList();
                        boolean listModified=false;
                        for(ModelNode cx:configList) {
                            boolean fnd=false;
                            for(ModelNode sx:serverList)
                                if(isSubsetOf(cx,sx)) {
                                    fnd=true;
                                    break;
                                }
                            if(!fnd) {
                                NodePath p=new NodePath(new PathExpression(x.path,cx.asString()),cx);
                                diff.add(new NodeDiff(Action.listAdd,p,p));
                                listModified=true;
                            }
                        }
                        for(ModelNode sx:serverList) {
                            boolean fnd=false;
                            for(ModelNode cx:configList) 
                                if(isSubsetOf(cx,sx)) {
                                    fnd=true;
                                    break;
                                }
                            if(!fnd) {
                                NodePath p=new NodePath(new PathExpression(s.path,sx.asString()),sx);
                                diff.add(new NodeDiff(Action.listRemove,p,p));
                                listModified=true;
                            }
                        }
                        if(listModified)
                            modifyList.add(new NodeDiff(Action.modify,x,s));
                    //Uncommenting check that the node type is NOT OBJECT when detecting differences.  
                    //I have tested this against the precipitating issue configuration (security).
                    //I found uncommenting this necessary for nested node configuration rules for infinispan.
                    } else if(!x.node.getType().equals(ModelType.OBJECT)&&!isSubsetOf(x.node,s.node)) {
                        ctx.log("Adding to modify list");
                        modifyList.add(new NodeDiff(Action.modify,x,s));
                    } else if(x.node.getType().equals(ModelType.OBJECT)&&
                              s.node.getType().equals(ModelType.OBJECT) ) {
                        ctx.log("Comparing for reorder: "+x.path+" cfg:"+x.node.asObject().keys()+" s:"+s.node.asObject().keys());
                        // Compare object member order
                        Iterator<String> itr1=x.node.asObject().keys().iterator();
                        Iterator<String> itr2=s.node.asObject().keys().iterator();
                        for(;itr1.hasNext()&&itr2.hasNext();) {
                            if(!itr1.next().equals(itr2.next())) {
                                modifyList.add(new NodeDiff(Action.reorder,x,s));
                                break;
                            }
                        }
                    }
                }
            if (!found) {
                // path not found on the server
                // add to diff list only if it's not marked as "deleted" or undefined
                if (!x.node.asString().equals("deleted") && !x.node.getType().equals(ModelType.UNDEFINED))
                    diff.add(new NodeDiff(x));
            }
        }
        for(NodeDiff x:modifyList) {
//             boolean hasChild=false;
//             for(NodeDiff y:modifyList)
//                 if(x.configPath.path.prefixOf(y.configPath.path)&&!x.configPath.path.equals(y.configPath.path)) {
//                     hasChild=true;
//                     break;
//                 }
//             if(!hasChild)
//                 for(NodeDiff y:diff)
//                     if(y.configPath!=null)
//                         if(x.configPath.path.prefixOf(y.configPath.path)&&!x.configPath.path.equals(y.configPath.path)) {
//                             hasChild=true;
//                             break;
//                         }
            //if(!hasChild)
                diff.add(x);
        }
        return diff;
    }

    
    /**
     * JBoss DMR library does not take into account escape sequences when comparing two strings.
     * So, we manually compare
     */
    private static boolean valueEquals(ModelNode n1,ModelNode n2) {
        if(n1.getType().equals(ModelType.STRING)&&
           n2.getType().equals(ModelType.STRING)) {
            return stringCompare(n1.asString(),n2.asString());
        } else if( (n1.getType().equals(ModelType.INT) ||
                    n1.getType().equals(ModelType.LONG) ||
                    n1.getType().equals(ModelType.STRING) ) &&
                   (n2.getType().equals(ModelType.INT) ||
                    n2.getType().equals(ModelType.LONG) ||
                    n2.getType().equals(ModelType.STRING)) ) {
            try {
                return n1.asLong()==n2.asLong();
            } catch (NumberFormatException x) {
                return n1.equals(n2);
            }
        } else {
            return n1.equals(n2);
        }
    }

    private static boolean stringCompare(String s1,String s2) {
        return unescape(s1).equals(unescape(s2));
    }

    public static String unescape(String s) {
        StringBuilder ret=new StringBuilder();
        int num=0;
        int n=s.length();
        int state=0;
        // Code copied/modified from jboss dmr ModelNodeParser.java
        for (int i = 0; i < n; i++) {
            char ch = s.charAt(i);
            switch(state) {
            case 0: 
                if(ch=='\\')
                    state=1;
                else
                    ret.append(ch);
                break;

            case 1:
                if(ch=='n')
                    ret.append('\n');
                else if(ch=='r')
                    ret.append('\r');
                else if(ch=='b')
                    ret.append('\b');
                else if(ch=='f')
                    ret.append('\f');
                else if(ch=='u')
                    state=2;
                else {
                    ret.append(ch);
                    state=0;
                }
                break;
                
            case 2:
                if(Character.isDigit(ch)) {
                    num=Character.digit(ch,10);
                    state=3;
                } else {
                    ret.append(ch);
                    state=0;
                }
                break;
                
            case 3:
            case 4:
            case 5:
                if(Character.isDigit(ch)) {
                    num*=10;
                    num+=Character.digit(ch,10);
                    state++;
                    if(state>5) {
                        ret.append((char)num);
                        state=0;
                    }
                } else {
                    ret.append(ch);
                    state=0;
                }
                break;

            }
        }
        return ret.toString();
    }
    
    /**
     * Checks if everything that is set in subset is also so in the superset.
     */
    public static boolean isSubsetOf(ModelNode subset,
                                     ModelNode superset) {
        if(subset==null)
            if(superset==null)
                return true;
            else
                return false;
        else if(superset==null)
            return subset.asString().equals("deleted");
        else if(!valueEquals(subset,superset)) {
            if(subset.asString().equals("deleted")&&superset.getType().equals(ModelType.UNDEFINED))
                return true;
            if(subset.getType()==superset.getType()) {
                switch(subset.getType()) {
                case LIST:
                    List<ModelNode> subsetList=subset.asList();
                    List<ModelNode> supersetList=superset.asList();
                    if(subsetList.size()!=supersetList.size())
                        return false;
                    for(ModelNode x:subsetList) {
                        boolean found=false;
                        for(ModelNode y:supersetList)
                            if(isSubsetOf(x,y)) {
                                found=true;
                                break;
                            }
                        if(!found)
                            return false;
                    }
                    break;
                case OBJECT:
                    Set<String> subsetKeys=subset.keys();
                    Set<String> supersetKeys=superset.keys();
                    if(supersetKeys.containsAll(subsetKeys)) {
                        for(String key:subsetKeys) {
                            if(!isSubsetOf(subset.get(key),superset.get(key)))
                                return false;
                        }
                    } else
                        return false;
                    break;
                default:
                    return false;
                }
            } else
                return false;
        } 
        return true;
    }
}
