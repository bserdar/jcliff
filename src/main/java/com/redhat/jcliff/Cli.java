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
import java.io.FileReader;
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
    private final String timeout;
    private final Ctx ctx;

    public Cli(String cli,
               String controller,
               String userName,
               String password,
               String timeout,
               Ctx ctx) {
        this.cli=cli==null?"jboss-cli.sh":cli;
        this.controller=controller==null?"localhost":controller;
        this.userName=userName;
        this.password=password;
        this.timeout=timeout;
        this.ctx=ctx;
    }

    public String run(String command) {
        return run(new String[] {command});
    }

    public String run(Script command) {
        return run(command.getCmds());
    }

    /**
     * Execute the client in a separate thread, and keep the
     * exception, or the returncode.
     */
    private static final class Exec extends Thread {
        Integer returnCode;
        Exception exception;
        final String[] args;

        public Exec(String[] args) {
            this.args=args;
        }
        
        @Override
        public void run() {
            try {
                Process p=Runtime.getRuntime().exec(args);
                returnCode=p.waitFor();
            } catch (Exception e) {
                exception=e;
            }
        }
    }
    
    private static int runAndWait(String[] args,long timeout) throws Exception {
        // Run the command in a different thread with a timeout, so if it hangs, we won't hang
        Exec x=new Exec(args);
        x.start();
        x.join(timeout);

        if(x.isAlive())
            throw new RuntimeException ("Timeout while waiting for child process to complete");
        if(x.exception!=null)
            throw x.exception;
        else
            return x.returnCode;
    }

    /**
     * Runs the cli with a 20 sec timeout
     */
    public String run(String[] command) {
        return run(command,20000);
    }

    /**
     * Runs the cli with the given timeout, 0 means indefinite wait
     */
    public String run(String[] command,long execTimeout) {
        if (command == null) {
            ctx.log("cmds: null");
            return "";
        }

        List<String> cmdArray=new ArrayList<String>();
        int ix=0;
        File tempFile;
        File outFile;
        File errFile;
        File scriptFile;
            try {
                tempFile=File.createTempFile("jcliff-in",null);
                outFile=File.createTempFile("jcliff-out",null);
                errFile=File.createTempFile("jcliff-err",null);
                scriptFile=File.createTempFile("jcliff-script",null);
                ctx.log("in file:"+tempFile.getAbsolutePath()+" "+tempFile.exists());
                ctx.log("out file:"+outFile.getAbsolutePath()+" "+outFile.exists());
                ctx.log("err file:"+errFile.getAbsolutePath()+" "+errFile.exists());
                ctx.log("script file:"+scriptFile.getAbsolutePath()+" "+scriptFile.exists());

                cmdArray.add(cli);
                cmdArray.add("--controller="+controller);
                cmdArray.add("--connect");
                cmdArray.add("--file="+tempFile.getAbsolutePath());
                if(userName!=null) {
                    cmdArray.add("--user="+userName);
                    if(password!=null)
                        cmdArray.add("--password="+password);
                }
                if(timeout!=null) {
                        cmdArray.add("--timeout="+timeout);
                }
                FileWriter writer=new FileWriter(tempFile);
                for(String x:command) {
                    writer.write(x,0,x.length());
                    writer.write('\n');
                }
                writer.flush();
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
                StringBuffer buf=new StringBuffer();
                for(String x:cmdArray)
                    buf.append(x).append(' ');
                FileWriter scw=new FileWriter(scriptFile);
                scw.write(buf.toString()+">"+outFile.getAbsolutePath()+" 2>"+errFile.getAbsolutePath());
                scw.flush();
                ctx.log("Script file:"+scriptFile.getAbsolutePath()+" "+scriptFile.exists());
                ctx.log("In file:"+tempFile.getAbsolutePath()+" "+tempFile.exists());

                int returnCode=runAndWait(new String[] {"/bin/sh",scriptFile.getAbsolutePath()},execTimeout);


                    
                    ctx.log("return Code has :"+returnCode);
                    String errString=read(errFile);
                    String outString=read(outFile);
                    ctx.log("stderr:"+errString);
                    ctx.log("stdout:"+outString);
                    if(!ctx.leaveTmp) {
                        scriptFile.delete();
                        tempFile.delete();
                        errFile.delete();
                        outFile.delete();
                    }
                    if(errString.length()>0)
                        throw new RuntimeException(errString);
                    if(returnCode==0||returnCode==1) {
                        ctx.log("Return:"+outString);
                        return outString;
                    } else {
                        ctx.log("Return code="+returnCode);
                        return null;
                           }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
    }

    private String read(File f) throws Exception {
        FileReader r=new FileReader(f);
        StringBuffer buf=new StringBuffer();
        int i;
        while( (i=r.read()) !=-1 )
            buf.append((char)i);
        r.close();
        return buf.toString().trim();
    }
}
