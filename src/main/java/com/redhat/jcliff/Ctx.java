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

import java.io.PrintStream;

import java.util.List;
import java.util.ArrayList;

import org.jboss.dmr.ModelNode;

/**
 * @author bserdar@redhat.com
 */
public class Ctx {

    public Cli cli;
    public boolean log=false;
    public boolean noop=false;
    public PrintStream out=System.out;
    public ModelNode currentServerNode;
    public List<NodePath> configPaths;
    public List<NodePath> serverPaths;

    public void log(String s) {
        if(log)
            out.println(s);
    }

    public void error(Exception e) {
        e.printStackTrace(out);
    }

    public ModelNode[] runcmd(String[] cmds,Postprocessor p) {
        if(!noop) {
            String s=cli.run(cmds);
            if(s==null)
                throw new RuntimeException("Operation failed");
            return p.process(s);
        } else
            return null;
    }

}
