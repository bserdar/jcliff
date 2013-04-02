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

import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;

import java.util.List;
import java.util.ArrayList;

/**
 * @author bserdar@redhat.com
 */
public class Cli {
    private final String cli;
    private final String controller;
    private final String userName;
    private final String password;
    private final Ctx ctx;

    private static final class ReaderThread extends Thread {
        private final InputStream stream;
        private final StringBuffer buf=new StringBuffer();

        public ReaderThread(InputStream stream) {
            this.stream=stream;
        }

        public void run() {
            InputStreamReader reader=new InputStreamReader(stream);
            int i;
            try {
                while((i=reader.read())!=-1)
                    buf.append((char)i);
            } catch (Exception e) {}
        }

        public String toString() {
            return buf.toString();
        }
    }

    public Cli(String cli,
               String controller,
               String userName,
               String password,
               Ctx ctx) {
        this.cli=cli==null?"jboss-cli.sh":cli;
        this.controller=controller==null?"localhost":controller;
        this.userName=userName;
        this.password=password;
        this.ctx=ctx;
    }

    public String run(String command) {
        return run(new String[] {command});
    }

    public String run(String[] command) {
        Runtime runtime=Runtime.getRuntime();
        List<String> cmdArray=new ArrayList<String>();
        int ix=0;
        File tempFile;
        try {
            tempFile=File.createTempFile("jbosscfgtmp",null);
            cmdArray.add(cli);
            cmdArray.add("--controller="+controller);
            cmdArray.add("--connect");
            cmdArray.add("--file="+tempFile.getAbsolutePath());
            if(userName!=null) {
                cmdArray.add("--user="+userName);
                if(password!=null)
                    cmdArray.add("--password="+password);
            }
            FileWriter writer=new FileWriter(tempFile);
            for(String x:command) {
                writer.write(x,0,x.length());
                writer.write('\n');
            }
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        for(String x:cmdArray) {
            ctx.log("args:"+x);
        }
        for(String x:command) {
            ctx.log("cmds:"+x);
        }
        try {
            Process p=runtime.exec(cmdArray.toArray(new String[cmdArray.size()]));
            ReaderThread outReader=new ReaderThread(p.getInputStream());
            outReader.start();
            ReaderThread errReader=new ReaderThread(p.getErrorStream());
            errReader.start();
            
            int returnCode=p.waitFor();
            outReader.join();
            errReader.join();
            tempFile.delete();
            ctx.log("stderr:"+errReader.toString());
            ctx.log("stdout:"+outReader.toString());
            if(errReader.toString().length()>0)
                throw new RuntimeException(errReader.toString());
            if(returnCode==0) {
                String s=outReader.toString();
                ctx.log("Return:"+s);
                return s;
            } else
                return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
