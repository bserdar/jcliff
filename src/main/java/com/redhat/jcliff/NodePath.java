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
import java.util.Set;
import java.util.ArrayList;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author bserdar@redhat.com
 */
public class NodePath {
    public final ModelNode node;
    public final PathExpression path;
    public final boolean listElement;

    public NodePath(PathExpression path,ModelNode node) {
        this(path,node,false);
    }

    public NodePath(PathExpression path,ModelNode node,boolean listElement) {
        this.path=path;
        this.node=node;
        this.listElement=listElement;
    }

    public String toString() {
        return path.toString()+ " => "+ node.toString();
    }

    public static List<NodePath> getPaths(ModelNode root) {
        PathExpression context=new PathExpression();
        return getPaths(context,root);
    }

    public static List<NodePath> getPaths(PathExpression context,ModelNode root) {
        List<NodePath> list=new ArrayList<NodePath>();
        getPaths(list,context,root);
        return list;
    }

    public static boolean isPrimitive(ModelType type) {
        return type==ModelType.BIG_DECIMAL||
            type==ModelType.BIG_INTEGER||
            type==ModelType.BOOLEAN||
            type==ModelType.BYTES||
            type==ModelType.DOUBLE||
            type==ModelType.EXPRESSION||
            type==ModelType.INT||
            type==ModelType.LONG||
            type==ModelType.STRING||
            type==ModelType.TYPE||
            type==ModelType.UNDEFINED;
    }

    public boolean isPrimitive() {
        return isPrimitive(node.getType());
    }

    public static PathExpression getAbsolutePath(PathExpression context,PathExpression expr) {
        PathExpression eval=null;
        if(expr.isRelative()) {
            eval=new PathExpression(context);
            int n=expr.size();
            for(int i=0;i<n;i++) {
                String s=expr.get(i);
                if(s.equals("."))
                    ;
                else if(s.equals(".."))
                    eval.removeLast();
                else
                    eval.add(s);
            }
        } else
            eval=expr;
        return eval;
    }

    public static NodePath find(List<NodePath> allPaths,PathExpression path) {
        for(NodePath x:allPaths)
            if(x.path.equals(path))
                return x;
        return null;
    }

    private static void getPaths(List<NodePath> list,PathExpression context,ModelNode root) {
        ModelType type=root.getType();
        if(type==ModelType.OBJECT) {
            Set<String> keys=root.keys();
            for(String x:keys) {
                PathExpression newctx=context.copy();
                ModelNode node=root.get(x);
                newctx.add(x);
                list.add(new NodePath(newctx,node));
                getPaths(list,newctx,node);
            }
        } else if(type==ModelType.LIST) {
            List<ModelNode> l=root.asList();
            for(ModelNode x:l) {
                PathExpression newctx=context.copy();
                newctx.add(x.asString());
                list.add(new NodePath(newctx,x,true));
            }
        } else if(type==ModelType.PROPERTY) {
        } 
    }
}
