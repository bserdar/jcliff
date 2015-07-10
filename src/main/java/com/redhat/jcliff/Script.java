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

import java.util.ArrayList;
import java.util.List;

public class Script {
    private final List<String> cmds=new ArrayList<String>();

    public Script(String[] cmds) {
        if(cmds!=null)
            for(String x:cmds)
                this.cmds.add(x);
    }

    public Script(String cmd) {
        this(new String[] {cmd});
    }

    public Script(List<String> cmds) {
        this.cmds.addAll(cmds);
    }

    public Script(Script s) {
        this.cmds.addAll(s.cmds);
    }

    public int size() {
        return cmds.size();
    }

    public String[] getCmds() {
        return cmds.toArray(new String[cmds.size()]);
    }

    public void addToTail(String s) {
        cmds.add(s);
    }

    public void insertToHead(String s) {
        cmds.add(0,s);
    }

    public boolean equals(Object o) {
        try {
            return equals( (Script)o);
        } catch (Exception e) {}
        return false;
    }

    public boolean equals(Script o) {
        try {
            return cmds.equals(o.cmds);
        } catch (Exception e) {}
        return false;
    }

    public int hashCode() {
        int l=0;
        if(cmds!=null) 
            for(String x:cmds)
                l+=x.hashCode();
        
        return l;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        boolean first=true;
        if (cmds != null) {
            for(String x:cmds) {
                if(first)
                    first=false;
                else
                    buf.append('\n');
                buf.append(x);
            }
        }
        return buf.toString();
    }

    public boolean hasBatch() {
        return hasOneOf("run-batch");
    }

    public boolean hasReload() {
        return hasOneOf(":reload");
    }

    public boolean hasIf() {
        return hasOneOf("if ");
    }

    public boolean hasDeployments() {
        return hasOneOf("deploy ","undeploy ");
    }

    private boolean hasOneOf(String...args) {
        for(String x:cmds)
            for(String a:args)
                if(x.indexOf(a)!=-1)
                    return true;
        return false;
    }
    
    public Script[] splitByReloads() {
        List<Script> scripts=new ArrayList<Script>();
        List<String> current=new ArrayList<String>();
        for(String x:cmds) {
            if(x.indexOf(":reload")!=-1) {
                if(!current.isEmpty()) {
                    scripts.add(new Script(current));
                    current=new ArrayList<String>();
                }
                current.add(x);
                scripts.add(new Script(current));
                current=new ArrayList<String>();
            } else {
                current.add(x);
            }
        }
        if(!current.isEmpty())
            scripts.add(new Script(current));
        return scripts.toArray(new Script[scripts.size()]);
    }
}
