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
import java.util.Properties;

/**
 * A ruleset defines a list of configurables. Each configurable is a
 * subsystem in jboss configuration that has its own rules. The
 * ordering of configurables is significant. Configuration is
 * processed in the order configurables are defined.
 *
 * @author bserdar@redhat.com
 */
public class RuleSet {

    public interface RuleAccessor {
        public Properties loadProperties(String name);
    }

    private List<Configurable> systems=new ArrayList<Configurable>();

    public String[] getSystemNames() {
        String[] ret=new String[systems.size()];
        int i=0;
        for(Configurable x:systems)
            ret[i++]=x.getName();
        return ret;
    }
    
    public void add(Configurable c) {
        systems.add(c);
    }

    public Configurable get(String name) {
        for(Configurable x:systems)
            if(x.getName().equals(name))
                return x;
        return null;
    }

    public static RuleSet getRules(RuleAccessor accessor,
                                   String ruleFileName) {
        RuleSet ret=new RuleSet();
        
        Properties p=accessor.loadProperties(ruleFileName);
        String[] configurables=Configurable.getProperties(p,"configurable");
        if(configurables==null)
            throw new RuntimeException("Expecting configurables in "+ruleFileName);
        for(String x:configurables) {
            ret.add(Configurable.parseCfg(accessor.loadProperties(x)));
        }
        return ret;
    }
    
}
