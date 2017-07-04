# Server documentation

## Quickstart with Docker

If you have Docker installed (if not, you can find how to do so [here](https://docs.docker.com/engine/installation/)), you may want to create a docker container. To do so, simply execute the following commands :

``` bash
sudo docker build -t mart-server .
sudo docker run -p 8080:8080 mart-server
```

You may now start to play with your MartServer instance through the OCCInterface : **http://localhost:8080/occinterface/**.

## Build the application
Go to the application's directory and then:
<pre>
<code>mvn initialize
mvn clean install</code>
</pre>

## Integration tests
Be aware that this will execute real operations like create compute, start/stop compute etc.
In MART core module, dependencies must be set on infrastructure and its dummy connector (as described in pom.xml on this repository).

If you want to execute integration tests execute :
<pre>
<code>mvn clean verify -Pintegration-test</code>
</pre>

Some dependencies are in lib/ directory, mvn initialize reference them in your local maven repository.
The file pom.xml declare them via the plugin maven-install-plugin, if you need to customize what library is required, you can update this file with your own dependencies.
Note that the server use Model@Runtime from Clouddesigner libs, if you have designed your extension model and developped your own connector, you may update the file pom.xml.

## Start the server

Launch the server with an embedded jetty :

Three options :
<pre>
<code>cd org.occiware.mart.jetty
mvn compile exec:exec</code>
</pre>

You can launch the server with an embedded jetty using occinterface integration :
<pre>
<code>mvn clean install -Pwithoccinterface
cd org.occiware.mart.war
mvn jetty:run-war</code>
</pre>

Launch the server with an embedded tomcat :
<pre>
<code>mvn clean install -Pwithoccinterface
cd org.occiware.mart.war
mvn tomcat7:run-war</code>
</pre>

The http address to occinterface is by default : <b>http://yourserver:port/occinterface/</b>
For example : <b>http://localhost:8080/occinterface/</b>

You may have a result like this one for jetty :

<pre>
<code>
INFO  log - Logging initialized @372ms
WARN  ContextHandler - o.e.j.s.ServletContextHandler@58651fd0{/,null,null} contextPath ends with /*
WARN  ContextHandler - Empty contextPath
INFO  MART - OCCIware MART initializing...
INFO  MART -   Scanning all plugin.xml found in the classpath...
INFO  MART -     - EMF package org.occiware.clouddesigner.occi.OCCIPackage at http://schemas.ogf.org/occi/core/ecore registered.
INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .occie registered.
INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .occic registered.
INFO  MART -     - OCCI extension http://schemas.ogf.org/occi/core# contained in model/Core.occie registered.
INFO  MART -     - EMF package org.occiware.clouddesigner.occi.infrastructure.InfrastructurePackage at http://schemas.ogf.org/occi/infrastructure/ecore registered.
INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .infrastructure registered.
INFO  MART -     - OCCI extension http://schemas.ogf.org/occi/infrastructure# contained in model/Infrastructure.occie registered.
INFO  MART - OCCIware MART initialized.
INFO  ConfigurationManager - Collection: [http://schemas.ogf.org/occi/infrastructure#, http://schemas.ogf.org/occi/core#]
INFO  ConfigurationManager - Loading model extension : http://schemas.ogf.org/occi/infrastructure#
INFO  ConfigurationManager - Loading model extension : http://schemas.ogf.org/occi/core#
INFO  ConfigurationManager - Extension : core added to user configuration.
INFO  ConfigurationManager - Extension : infrastructure added to user configuration.
Mart server will log in path : /Users/myuser/workspace/MartServer/logs/mart_server_debug.log
Mart server will log in path : /Users/myuser/workspace/MartServer/logs/mart_server_info.log
Mart server will log in path : /Users/myuser/workspace/MartServer/logs/mart_server_warn.log
Mart server will log in path : /Users/myuser/workspace/MartServer/logs/mart_server_error.log
Mart server will log in path : /Users/myuser/workspace/MartServer/logs/mart_server_fatal.log
2016-11-23 08:40:26.764 INFO  jetty-9.3.9.v20160517
2016-11-23 08:40:27.250 INFO  Started o.e.j.s.ServletContextHandler@58651fd0{/,null,AVAILABLE}
2016-11-23 08:40:27.275 INFO  Started ServerConnector@2262b621{HTTP/1.1,[http/1.1]}{0.0.0.0:8080}
2016-11-23 08:40:27.276 INFO  Started @1574ms
</code>
</pre>

The extensions you have registered in pom.xml must appear in the output log, here i have OCCI core extension and OCCI core infrastructure.

If the option server.load.onstart is set to true, all the models will load on start.

## Stop server

Simply kill the java main thread (pkill -9 java) or ctrl+c in current server terminal.

If the option server.save.onterminate is set to true, This will save all the models in your home directory. 
You can configure that in the config properties file.

## Configuring the server.

The martserver config file is only useable with org.occiware.mart.jetty package submodule.

The server port is by default on 8080.
Https server port is by default on 8181.

By default, the server use the packaged configuration file located in module security : 
<b>./org.occiware.mart.security/src/main/resources/config.properties</b>, if you use the packaged war, you must update and use this file.


You can add a property file with the name as you want, and located in a directory of your choice.
You could create for example a file server.config and set it to my_folder_config.
You can also launch the server with this command line :
<pre>
<code>
mvn compile exec:exec -Dexec.args="/my_folder_config/server.config"
</code>
</pre>
This feature is only available for standalone mode (using jetty module).

For now there are some parameters :

 - server.http.port=8080
 
 Where the port is between 1 and 9999 a good practice to set the port is to assume that all port before 1000 are not ok. So you can choose a port like 1001.

 - server.log.directory=/logging/application/logs
 
 The directory where are located the application logs.

 - server.protocol=http
 
 The protocol, <b>http</b> and <b>https</b> support will be plan in a near future.

 - server.https.port=8181

 - admin.username=admin
 
 Temporary username for default administrator.
 
 - admin.password=1234
 
 Temporary password for default administrator.

 - <b>server.model.directory</b>=/yourmodelfolder/
 
 Path of your model directory. By default, if not set, the application will use: <b>/homedir/models/</b>
 
 This is used by save and load model api feature.

 - <b>server.save.onterminate</b>=true

This parameter give the ability to save model when stopping the server.

 - <b>server.load.onstart</b>=true

This parameter give the ability to load model when starting the server.

 - <b>server.plugins.directory</b>=/your_extension/and/connector/plugins/folder/

This parameter give the extension model jar and connector jar plugins to use with the server,
  those libraries will be loaded at runtime (only tested with jetty module).
  By default the server will use the directory : <b>/homedir/martserver-plugins</b>

## Using the server

If you have launch the server in localhost, you can check that the server is started correctly with this curl command:
<pre>
<code>curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/ -H "accept: application/json"</code>
</pre>

In content result, you will have a json string with the full interface supported by this server.
Like this:
<pre>
<code>
*   Trying ::1...
* Connected to localhost (::1) port 8080 (#0)
> GET /.well-known/org/ogf/occi/-/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: application/json
>
< HTTP/1.1 200 OK
< Date: Sat, 05 Nov 2016 09:48:55 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Content-Type: application/occi+json
< Accept: text/occi;application/json;application/occi+json;text/plain
< Transfer-Encoding: chunked
<
{
  "actions" : [ {
    "scheme" : "http://schemas.ogf.org/occi/infrastructure/network/action#",
    "term" : "up",
    "title" : ""
  }, {
    "scheme" : "http://schemas.ogf.org/occi/infrastructure/network/action#",
    "term" : "down",
    "title" : ""
  }, {
    "scheme" : "http://schemas.ogf.org/occi/infrastructure/compute/action#",
    "term" : "start",
    "title" : "Start the system"
  }, {
    "attributes" : {
      "method" : {
        "mutable" : true,
        "required" : true,
        "pattern" : {
          "$schema" : "http://json-schema.org/draft-04/schema#",
          "type" : "string"
        },
        "type" : "string"
      }
    },
    "scheme" : "http://schemas.ogf.org/occi/infrastructure/compute/action#",
    "term" : "stop",
    "title" : "Stop the system (graceful, acpioff or poweroff)"
}, {
 ...
</code>
</pre>

## Logs output
There is two output :

- standard output will log all logs with level INFO in console output.
- file log output, contains all application / dependencies logged per level.

The subdirectory logs/ contains all output logs level (rolling mode with a maximum of 2 Mo size) :

- mart_server_debug.log (DEBUG level)
- mart_server_info.log (INFO level)
- mart_server_warn.log (WARN level)
- mart_server_error.log (ERROR level)
- mart_server_fatal.log (FATAL level)


## Special features
We have added the feature to save or load model for current user (using basic auth).

There is special uri for that : /mart/save/ and /mart/load/

To validate the model : /mart/validate/


## Issues
Do not hesitate to create new issues if you find a bug or if you have suggestion to make it better.
