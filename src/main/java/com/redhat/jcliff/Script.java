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

import java.util.Arrays;
import java.util.List;

public class Script {
    public final String[] cmds;

    public Script(String[] cmds) {
        this.cmds=cmds==null?new String[0]:cmds;
    }

    public Script(List<String> cmds) {
        this(cmds.toArray(new String[cmds.size()]));
    }


    public boolean equals(Object o) {
        try {
            return equals( (Script)o);
        } catch (Exception e) {}
        return false;
    }

    public boolean equals(Script o) {
        try {
            Arrays.equals(cmds,o.cmds);
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
}
