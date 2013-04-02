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

/**
 * @author bserdar@redhat.com
 */
public class PathExpression {

    private final List<String> components=new ArrayList<String>();
    private boolean relative=false;

    public PathExpression() {
    }

    public PathExpression(PathExpression x) {
        for(String c:x.components)
            components.add(c);
        this.relative=x.relative;
    }

    public PathExpression(String... c) {
        for(String x:c)
            components.add(x);
    }

    public PathExpression(PathExpression ctx,String... c) {
        if(ctx!=null)
            components.addAll(ctx.components);
        for(String x:c)
            add(x);
    }

    public PathExpression(PathExpression ctx,PathExpression rel) {
        if(ctx!=null)
            components.addAll(ctx.components);
        if(rel!=null)
            components.addAll(rel.components);
    }

    public PathExpression add(String c) {
        components.add(c);
        return this;
    }

    public String get(int n) {
        return components.get(n);
    }

    public String last() {
        return components.get(components.size()-1);
    }

    public PathExpression removeLast() {
        components.remove(components.size()-1);
        return this;
    }

    public int size() {
        return components.size();
    }

    public boolean empty() {
        return components.isEmpty();
    }

    public boolean equals(Object o) {
        try {
            return equals((PathExpression)o);
        } catch(ClassCastException x) {
            return false;
        }
    }

    public boolean isRelative() {
        return relative;
    }

    public void setRelative(boolean rel) {
        this.relative=rel;
    }

    public boolean equals(PathExpression x) {
        if(x!=null)
            return relative==x.relative&&components.equals(x.components);
        return false;
    }

    public boolean prefixOf(PathExpression x) {
        if(x.relative==relative) {
            int n=components.size();
            if(x.components.size()>=n) {
                for(int i=0;i<n;i++)
                    if(!components.get(i).equals(x.components.get(i)))
                        return false;
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuffer buf=new StringBuffer();
        boolean first=true;
        for(String x:components) {
            if(first) {
                first=false;
                if(!relative)
                    buf.append('/');
            } else
                buf.append('/');
            buf.append(x.toString());
        }
        return buf.toString();
    }

    /**
     * Returns if this path matches the given expression
     */
    public boolean matches(PathExpression expr) {
        if(expr!=null&&expr.relative==relative) {
            int n=components.size();
            if(expr.components.size()==n) {
                for(int i=0;i<n;i++) {
                    String x1=components.get(i);
                    String x2=expr.components.get(i);
                    if(!x1.equals("*")&&
                       !x2.equals("*")&&
                       !x1.equals(x2))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    public PathExpression copy() {
        return new PathExpression(this);
    }

    public static PathExpression parse(String s) {
        PathExpression ret=new PathExpression();
        int state=0;
        int n=s.length();
        StringBuffer buf=null;
        boolean quote=false;
        for(int i=0;i<n;i++) {
            char c=s.charAt(i);
            switch(state) {
            case 0: 
                if(!Character.isWhitespace(c)) {
                    buf=new StringBuffer();
                    state=1;
                    if(c=='/')
                        ret.relative=false;
                    else {
                        buf.append(c);
                        ret.relative=true;
                    }
                }
                break;

            case 1: // Parsing a new component
                if(c=='\"') {
                    if(quote) {
                        quote=false;
                        if(buf.length()>0) {
                            ret.add(buf.toString());
                            buf=null;
                            state=2;
                        } else
                            throw new RuntimeException("Empty component:"+s);
                    } else
                        quote=true;
                } else if(quote) {
                    buf.append(c);
                } else if(c=='/') {
                    if(buf.length()>0) {
                        ret.add(buf.toString());
                        buf=new StringBuffer();
                        state=1;
                    } else
                        throw new RuntimeException("Empty component:"+s);
                } else
                    buf.append(c);
                break;                    
                    
            case 2: // Waiting /
                if(c=='/') {
                    state=1;
                    buf=new StringBuffer();
                } else if(Character.isWhitespace(c))
                    ;
                else
                    throw new RuntimeException("Syntax error:"+s);
                break;
            }
        }
        if(quote)
            throw new RuntimeException("No closing \":"+s);

        switch(state) {
        case 1:
            if(buf!=null&&buf.length()>0) {
                ret.add(buf.toString());
            }
            break;
        }

        return ret;
    }

    public static void main(String[] args) throws Exception {
        PathExpression x=PathExpression.parse(args[0]);
        System.out.println(x);
    }

}
