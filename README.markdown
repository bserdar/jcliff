# JCliff
## What does it do?

Jcliff configures a running instance of EAP6/JBoss7 using modular
configuration files.

## How to install it?

JCliff is simple Java app, you can build it from the source code:

    $ mvn clean install

If you are using Linux and RPM to manage your software (RHEL, CentOS, Fedora...), there is a copr repos available:

    $ yum install yum-plugin-copr  # to ensure copr is available to yum
    $  dnf copr enable ansiblemiddleware/jcliff && dnf install jcliff -y

### But I can configure EAP6/JBoss7 by editing the configuration XML file....

There are several problems with that:
  * It is not easily maintainable, especially if you have many hosts with different configurations.
  * It requires a bounce. JCliff uses jboss-cli.sh to invoke
configuration commands, so many configuration modifications can be
done without bouncing the server.

### I can use scripts that call jboss-cli.sh to configure the server

JCliff modifies the configuration incrementally. Only necessary parts
of the configuration is modified, and if the required changes are already 
applied, nothing is done.

### How do I use Puppet?

Use puppet to lay down configuration files, and then have puppet
execute jcliff. This way, you can define datasources, logging,
etc. using puppet code and templates, and configure only what's
necessary.

### How do I use it with Ansible

JCliff has been integrated into Ansible as part of its own
[JCliff Collection](https://github.com/wildfly-extras/ansible_collections_jcliff),
please refers to this project documentation.

### Can I use another configuration management tool?

There's nothing puppet specific in jcliff. You can use jcliff as a
command line tool, or from any configuration management system than
can install files, and execute other programs.

### What does jcliff configuration files look like?

JCliff uses the same JSON-like language used by jboss-cli.sh (jboss-dmr library).
A configuration file that sets the root level logging looks like

{
  "logging" => { 
    "root-logger" => {
       "ROOT" => {
          "level" => "debug"
       }
    }
  }
}

### How do I install JCliff on my system ?

First of all, build the project using Maven:

    $ mvn clean install

Then create a directory and associate a environment variable called JCLIFF_HOME
to it

    $ mkdir -p /usr/local/jcliff
    $ export JCLIFF_HOME=/usr/local/jcliff

Copy the jcliff jar and its dependency to this directory:

    $ cp -Rv target/dependency/ "${JCLIFF_HOME}"
    $ cp target/jcliff*.jar "${JCLIFF_HOME}"

From there, you can use the scripts provided in src/main/scripts to use jcliff.

# Operation

EAP6 configuration is difficult if you intend to use puppet. The
command line client provides an interactive front-end for
configuration tasks, not easily called from puppet scripts. There is
no easy way to retrieve what's already configured, find the
differences between the current state and the desired state, and come
up with a way to implement those. Jcliff does exactly that. User
supplies the desired configuration, jcliff executes the jboss client
to retrieve the current state, derive deltas, and apply them. What
kind of delta results in what kind of action is defined using a
property file based rule language.

## JBoss configuration model:

The JBoss DMR library is used to represent jboss configuration in a
hierarchical markup language. 

Look at this logging configuration fragment as example:

    > /subsystem=logging:read-resource(recursive=true)

    {
        "outcome" => "success",
        "result" => {
            "custom-handler" => undefined,
            "async-handler" => {
                "blah" => {
                    "encoding" => undefined,
                    "filter" => undefined,
                    "formatter" => "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n",
                    "level" => undefined,
                    "overflow-action" => "BLOCK",
                    "queue-length" => 1000,
                    "subhandlers" => undefined
                },
                "ASYNC" => {
                    "encoding" => undefined,
                    "filter" => undefined,
                    "formatter" => "%d{HH:mm:ss,SSS} %-5p [%c] (%t) %s%E%n",
                    "level" => undefined,
                    "overflow-action" => "BLOCK",
                    "queue-length" => 1000,
                    "subhandlers" => ["FILE"]
                }
            },
         ...
      }
    }

The command 

    /subsystem=logging:read-resource(recursive=true)

is parsed as follows:

  - Select the subsytem whose name is logging. That's a node in the
  configuration tree
  - `:read-resource` is an operation defined on that node. Invoke the
  operation with attributes `(recursive=true)`

As for the return result:

  - Anything between `{` and `}` is an object. So the return result is an
  object containing two items, `outcome` and `result`.
  - `result` is another object, containing `custom-handler`,
  `async-handler`, etc. all of which are objects
  - `async-handler` contains objects `ASYNC` and `blah`.
  - `ASYNC/subhandlers` is a list of strings. Anything between `[` and `]`
  are lists.

Every node in the configuration tree defines a set of operations that
can be run on that node. For instance, to add a new `async-handler`, you
have to invoke:

    /subsystem=logging/async-handler=newHandler:add(queue-length=someNumber)

Here's the primary reasons why it is not easy to automate the
configuration tasks:

   - Every node defines a separate set of operations.
   - Every operation has a different set of required parameters

That is, there is no standard way of adding/removing nodes to the
configuration tree. For instance, to add an element to the handlers
list of a logger, you have to call assign-subhandler on that node. To
assign a handler to root logger, you have to call
`root-logger-assign-subhandler` on that root logger.

`/subsystem` is not a universal prefix. That would be too easy. For
instance, to get system properties:

    /core-service=platform-mbean/type=runtime:read-attribute(name=system-properties)

## Puppet configuration model:

The idea is to have puppet lay configuration files to a given
directory, and then run jcliff on those files. Jcliff loads the
puppetized configuration files, talks to JBoss, determines what needs
to be changed, and changes them. Each puppetized configuration file
has to tell what it is configuring. For instance, a logging
configuration file looks like:

    { "logging" =>
       {
            "async-handler" => { 
              "ASYNC" => {
                "subhandlers" => [ "FILE" ],
                "queue-length" => 1000,
               },
            },
            "size-rotating-file-handler" => { 
              "SFILE" => { }
            }
        }
    }
          ...

Similarly, jdbc drivers:

    { "jdbc-driver" =>  
      { "oracle" => {
            "driver-name" => "oracle",
            "driver-module-name" => "oracle.jdbcx",
            "driver-xa-datasource-class-name" => "oracle.jdbc.XADataSource"
        }
      }
    }

Datasources:

    {
        "datasource" => {
               "BSProduct" => { 
                 "jndi-name" => "java:/BSProduct",
                 "connection-url" => "jdbc:oracle:oci@web",
                 "driver-name" => "oracle",
                 "user-name" => "web",
                 "password" => "web_dev0",
               }
         }
    }

System properties:

    { "system-property" => {
       "foo" => "bar",
       "bah" => "gah"
      }
    }

Jcliff does not delete anything from the existing configuration unless
explicitly required. Therefore, not specifying certain properties of
objects will leave them untouched. If you want to delete them, assign
objects/values to `"deleted"`, or undefine them, by assigning them to
`undefined`.

## Deployments:

Jcliff can be used to deploy applications. After applying all the
configuration changes, Jcliff attempts to process deployments of the form:

    { "deployments" => {
        "myApp-v2.1.ear" => {
         "NAME" => "myApp-v2.1.ear",
         "path" => "/var/lib/redhat/deploy/myApp-v2.1.ear",
         "replace-name-regex" => "\\QmyApp-v\\E\\..*",
         }
      }
    }

Jcliff runs `deploy -l` to retrieve the list of deployed packages. The
idea is to have Jcliff first undeploy older versions of the same
application, and then to redeploy the new version. The
`replace-name-regex` and `replace-runtime-name-regex` regular expressions
are used to locate applications that the new application will
replace. Any applications whose name matches `replace-name-regex`, or
whose runtime name matches `replace-runtime-name-regex` will be
undeployed before redeploying the new application. In the above
example, `myApp-v2.1.ear` would replace an existing `myApp-v2.0.ear`.

If `myApp-v2.1.ear` already is deployed, Jcliff will not attempt to
redeploy it, unless `--redeploy` flag is passed. So, after deploying all
the applications, running Jcliff without the `--redeploy` flag with the
existing deployment list will not alter any of the deployments.

## The delta and the rules:

Jcliff reads all configuration files, and builds a list of
paths. Every value in the configuration tree is represented by a path
containing all the object names up to that value. For instance:

    { "system-property" => {
       "foo" => "bar",
       "bah" => "gah"
      }
    }

Paths:

    system-property
    system-property/foo
    system-property/bah

For the datasource example:

    {
        "datasource" => {
               "BSProduct" => { 
                 "jndi-name" => "java:/BSProduct",
                 "connection-url" => "jdbc:oracle:oci@web",
                 "driver-name" => "oracle",
                 "user-name" => "web",
                 "password" => "web_dev0",
               }
         }
    }

Paths:

    /datasource
    /datasource/BSProduct
    /datasource/BSProduct/jndi-name
    /datasource/BSProduct/connection-url
    /datasource/BSProduct/driver-name
    /datasource/BSProduct/user-name
    /datasource/BSProduct/password

Same thing is also done for the configurations retrieved from JBoss. For instance, when datasource configuration is read, 

    "result" => {
        "ExampleDS" => {
                "allocation-retry" => undefined,
                "allocation-retry-wait-millis" => undefined,
                "allow-multiple-users" => undefined,
                "background-validation" => undefined,
                "background-validation-millis" => undefined,
                "blocking-timeout-wait-millis" => undefined,
                "check-valid-connection-sql" => undefined,
                "connection-properties" => undefined,
                "connection-url" => "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
                "datasource-class" => undefined,
                "driver-class" => undefined,
                "driver-name" => "h2",
                "enabled" => true,
                "exception-sorter-class-name" => undefined,
                "exception-sorter-properties" => undefined,
                "flush-strategy" => undefined,
                "idle-timeout-minutes" => undefined,
                "jndi-name" => "java:jboss/datasources/ExampleDS",
                "jta" => true,
                "max-pool-size" => undefined,
                "min-pool-size" => undefined,
                "new-connection-sql" => undefined,
                "password" => "sa",
                "pool-prefill" => undefined,
                "pool-use-strict-min" => undefined,
                "prepared-statements-cache-size" => undefined,
                "query-timeout" => undefined,
                "reauth-plugin-class-name" => undefined,
                "reauth-plugin-properties" => undefined,
                "security-domain" => undefined,
                "set-tx-query-timeout" => false,
                "share-prepared-statements" => false,
                "spy" => false,
                "stale-connection-checker-class-name" => undefined,
                "stale-connection-checker-properties" => undefined,
                "track-statements" => "NOWARN",
                "transaction-isolation" => undefined,
                "url-delimiter" => undefined,
                "url-selector-strategy-class-name" => undefined,
                "use-ccm" => true,
                "use-fast-fail" => false,
                "use-java-context" => true,
                "use-try-lock" => undefined,
                "user-name" => "sa",
                "valid-connection-checker-class-name" => undefined,
                "valid-connection-checker-properties" => undefined,
                "validate-on-match" => false,
                "statistics" => {
                    "jdbc" => undefined,
                    "pool" => undefined
                }
            }
      }

Jcliff uses the value of `result`, that is, the object containing the
`ExampleDS`. Some hacking is required here, because the configuration
tree starts with `datasource`, but the JBoss tree does not. We either
have to remove `datasource` from the configuration tree, or add
`datasource` to JBoss tree. The definition of datasource rules has
this property:

    server.preprocess.prepend=/datasource

That is, once loaded the jboss configuration is converted into:

    "datasource" => {
                      "ExampleDS" => {...}
                    }

All four options are possible:

    server.preprocess.strip
    server.preprocess.prepend
    client.preprocess.strip
    client.preprocess.prepend

These directives either insert, or remove levels from the client or
server configuration tree.

After the preprocessing, the paths are built, and a difference is
computed. The differences are:

   - `add`: Add a new configuration item to JBoss tree
   - `modify`: Modify an existing item in JBoss tree. Only the leaf
   modified paths appear in the delta. That is, if a datasource
   attribute is modified, the datasource node itself is not in the
   delta, only the modified attribute is.
   - `remove`: Remove an item from JBoss tree. This is done by assigning
   the value of an object to `"deleted"`.
   - `undefine`: Undefine item in JBoss tree.
   - `listAdd`: Add a new element in a list in JBoss tree.
   - `listRemove`: Remove an element from a JBoss list.
   - `reorder`: Reorder the attributes of an object

Hack: you can't add a nontrivial object, and set its attributes. The
attributes of the nontrivial object don't exist until the object is
created, which results in attribute add operations. Attributes cannot
be added, only modified. So,

  1) You have to run the rules that add objects before the ones that
  modify the objects properties
  2) Once you add a nontrivial object, you have to refresh the JBoss
  configuration tree, so that you have a representation of the
  existing configuration with all the default values of the newly
  added object.

So, after adding a new object, you have to re-read the relevant
configuration from JBoss. This is done by the `refresh` directive. For
instance:

    match.addConsoleHandler=add:/console-handler/*
    addConsoleHandler.precedence=50
    addConsoleHandler.rule=/subsystem=logging/console-handler:${name(.)}:add
    addConsoleHandler.refresh=true

    match.modifyConsoleHandler=modify:/console-handler/*/*
    modifyConsoleHandler.precedence=55
    modifyConsoleHandler.rule=/subsystem=logging/console-handler=${name(..)}:write-attribute(name=${name(.)},value=${value(.)})

`addConsoleHandler` will add a new console handler by only specifying
its name. Once added, all the attributes of the new handler will be
set to their default values. Jcliff will re-read the configuration,
retrieving all the attributes. Then, attribute modification rules are
run. Rules with lower precedence value run before rules with higher
precedence value.


Now the rules themselves:

 - The configurable subsytems are defined in the rules file:

        configurable.1=system-properties
        configurable.2=logging
        configurable.3=jdbc-driver
        configurable.4=datasource
        configurable.5=xadatasource

Each configurable defines a rule file defining the rules to deal with
that particular configurable. The explicit ordering defines the order
in which the subsystems will be configured.

Each rule file contains at least the following:

    name=xadatasource
    getContents=/subsystem=datasources:read-children-resources(child-type=xa-data-source)
    server.preprocess.prepend=/xadatasource

 - `name`: name of the configurable
 - `getContents`: The statement to run to retrieve the contents of this
 configurable from JBoss
 - preprocessing directives: optional
 `server/client.preprocess.prepend/strip`, defining what to add/remove
 from the server/client configuration so that a meaningfull delta can
 be computed. One server and one client operation can be specified,
 but you can't specify two server or client operations.

Each rule is defined using a `match` property:

    match.addDatasource=add:/datasource/*

This match property defines a rule name `addDatasource`. It matches
the node on the delta where a path of the form `/datasource/<Name>` is
added to the JBoss configuration. That is, a datasource exists in the
puppetized configuration, but not in JBoss. So, you define how to add
a new datasource:

    addDatasource.rule.1=data-source add --name=${name(.)} --jndi-name=${value(jndi-name)} --driver-name=${value(driver-name)} --connection-url=${value(connection-url)}
    addDatasource.rule.2=data-source enable --name=${name(.)}
    addDatasource.refresh=true

The construct `${name(<path>)}` evaluates to the name of the last
element in `<path>`. Path can be relative. A relative path is evaluated
with respect to the matched node. Above, if the matching node is
`/datasource/BSProduct`, `${name(.)}` evaluates to `BSProduct`.

The construct `${value(<path>)}` evaluates to the value of the node
denoted by `<path>`. In the above example, the expression
`${value(jndi-name)}` evaluates to the content of the path
`/datasource/BSProduct/jndi-name`, which should give the JNDI name of
the datasource in the puppetized configuration.

Multiple commands can be provided in a rule. In the above example,
`addDatasource.rule.1` will create the datasource, and
`addDatasource.rule.2` will enable it.

`addDatasource.refresh` will reload the JBoss configuration for
datasources. This is required after an add operation. The refresh
configuration will have all the datasource attributes initialized to
their default values, and any datasource attribute modification rule
will match after a refresh.

## Conditionals

It is possible, as of v2.6, to have conditionals in rules:

  ${if-defined (path), (true-case) <, (false-case) > }

If `path` is defined, then `true-case` is evaluated, otherwise
`false-case` is evaluated if it exists. For example:

{ 
  "jdbc-driver" => {
   { "MyDriver" => {
      ...
      "module-slot" => "2.0"
    }
  }

Above, "module-slot" is an optional parameter. The rule for inserting
a jdbc driver is written taking this into account:

  addDriver.rule.1=/subsystem=datasources/jdbc-driver=${name(.)}:add( \
      driver-name=${value(driver-name)}, \
      driver-module-name=${value(driver-module-name)}, \
      driver-xa-datasource-class-name=${value(driver-xa-datasource-class-name)} \
      ${if-defined (module-slot),(,module-slot=${value(module-slot)})} \
      ${if-defined (driver-class-name),(,driver-class-name=${value(driver-class-name)})})

Note that the argument has a starting comma. This is required to build a correct command.

## Iteration

 ${foreach-cfg (path), (cmd1), (cmd2),...}
 ${foreach-server (path), (cmd1), (cmd2),...}

In foreach-cfg, "path" refers to a path in the input configuration
file. In foreach-server, "path" refers to a path in the current server
configuration.  The "path" is evaluated based on the matched path of
the rule. The $foreach command iterates the immediate children of
"path", and executes all commands for each of the elements. The
commands "cmdN" are evaluated based on the iterated path. For example:

{
   "jgroups" => {
      "stack" => {
         "udp" => {
            "protocol" => {
               "PING" => {...},
               "MERGE3" => {...},
               "FD_SOCK" => {...}.

There is no way to insert or reorder protocols, so we remove all protocols from jgroups/stack/udp/protocol, and reinsert them:

match.addProtocol=add:/stack/*/protocol/*
addProtocol.rule.1=batch
addProtocol.rule.2=${foreach-server (/stack/${name(../..)}/protocol), (/subsystem=jgroups/stack=${name(../..)}:remove-protocol(type=${name(.)}))}
addProtocol.rule.3=${foreach-cfg (/stack/${name(../..)}/protocol), (/subsystem=jgroups/stack=${name(../..)}:add-protocol(type=${name(.)} ${if-defined (socket-binding),(,socket-binding=${value(socket-binding)})} ${if-defined (property), (,properties=${value(properties)})} ))}
addProtocol.rule.4=run-batch

 * The "match" rule matches any insertions to the protocol object. This rule will be evaluated when a new node is added to protocol.
 * The first rule starts a batch (jboss-cli directive)
 * The second rule iterates already defined protocols at the server, and removes them one by one.
 * The third rul iterates the protocols defined in the input configuration file, and inserts them one by one
 * The fourth rule runs the batch


## Prefix Rules

It is inefficient and error prone to keep writing rules of the form:

  match.modifyProp=modify:/webservices/*
  modifyProp.rule.1=/subsystem=webservices:write-attribute(name=${name(.)},value=${value(.)})

Changing an attribute is done the same way for all configuration
levels. So, instead of repeating this rule for every configuration
level, it can be written as a prefix rule that applies to a set of
nodes:

  prefix.modifyProp=modify:/webservices

The above rule declaration will make modifyProp match any
modifications of a path with prefix '/webservices'.

  modifyProp.rule.1=/subsystem=webservices${cmdpath(${path(..)})}:write-attribute(name=${name(.)},value=${value(.)})

${cmdpath($path(..))} writes the path in the form /componen1=component2/component3=component4/...

${cmdpath(=$path(..))} writes the path in the form =component1/component2=component3/component4=component5...
