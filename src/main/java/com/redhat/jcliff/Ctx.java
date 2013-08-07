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
import java.util.Set;
import java.util.Date;

import java.text.SimpleDateFormat;

import org.jboss.dmr.ModelNode;

/**
 * @author bserdar@redhat.com
 */
public class Ctx {

    public static final SimpleDateFormat timestampFormat=
        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSSS");

    public Cli cli;
    public boolean log=false;
    public boolean noop=false;
    public PrintStream out=System.out;
    public ModelNode currentServerNode;
    public List<NodePath> configPaths;
    public List<NodePath> serverPaths;
    public Set<Script> cmdsRun;
    public final List<String> cmdQueue=new ArrayList<String>();

    public void log(String s) {
        if(log)
            out.println(timestamp()+": "+s);
    }

    public void msg(String s) {
        out.println(timestamp()+": "+s);
    }

    public void error(Exception e) {
        out.print(timestamp()+": ");
        e.printStackTrace(out);
    }

    public ModelNode[] runcmd(Script cmds,Postprocessor p) {
        if(!noop) {
            msg(cmds.toString());
            String s=cli.run(cmds);
            if(s==null)
                throw new RuntimeException("Operation failed");
            return p.process(s);
        } else
            return null;
    }

    public void queueCmd(Script s) {
        if(s!=null)
            for(String x:s.cmds)
                queueCmd(x);
    }

    public void queueCmd(String s) {
        if(s!=null)
            cmdQueue.add(s);
    }

    public Script getQueuedCmds() {
        if(cmdQueue.isEmpty())
            return null;
        else
            return new Script(cmdQueue);
    }

    public boolean hasQueuedCmds() {
        return !cmdQueue.isEmpty();
    }

    public ModelNode[] runQueuedCmds(Postprocessor p) {
        if(!cmdQueue.isEmpty()) {
            ModelNode[] ret=runcmd(getQueuedCmds(),p);
            cmdQueue.clear();
            return ret;
        }  else
            return new ModelNode[0];
    }

    public static String timestamp() {
        return timestampFormat.format(new Date());
    }
}
