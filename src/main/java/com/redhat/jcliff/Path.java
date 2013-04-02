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

import java.util.Stack;
import java.util.Iterator;

import org.jboss.dmr.ModelNode;

/**
 * author bserdar@redhat.com
 */
public class Path {

    private static class Item {
        final ModelNode node;
        final String name;

        public Item(String name,ModelNode node) {
            this.name=name;
            this.node=node;
        }

    }

    private final Stack<Item> nodes=new Stack<Item>();

    public Path push(ModelNode n) {
        nodes.push(new Item(null,n));
        return this;
    }

    public Path push(String name,ModelNode n) {
        nodes.push(new Item(name,n));
        return this;
    }

    public Path pop() {
        nodes.pop();
        return this;
    }

    public ModelNode topNode() {
        return nodes.peek().node;
    }

    public String topName() {
        return nodes.peek().name;
    }

    public Path snapshot() {
        Path p=new Path();
        p.nodes.addAll(nodes);
        return p;
    }

    public int size() {
        return nodes.size();
    }

    public ModelNode getNode(int n) {
        return nodes.get(n).node;
    }

    public String getName(int n) {
        return nodes.get(n).name;
    }

    public ModelNode getrNode(int n) {
        return nodes.get(nodes.size()-1-n).node;
    }

    public String getrName(int n) {
        return nodes.get(nodes.size()-1-n).name;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        for(Iterator<Item> itr=nodes.iterator();itr.hasNext();) {
            Item item=itr.next();
            buf.append('/');
            if(item.name==null)
                buf.append('.');
            else
                buf.append(item.name);
        }
        buf.append("=").append(topNode().toString());
        return buf.toString();
    }

}
