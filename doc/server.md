# Server documentation

## Build the server and dependencies
Before all, to build the project, you must get Clouddesigner sources and compile them.

[You can get it here](https://github.com/occiware/ecore)

To build Clouddesigner : 

```
cd ecore/clouddesigner/org.occiware.clouddesigner.parent/
mvn clean install
```

After that, all occi extensions and connectors are in your local maven repository. We made that because there's no global maven repository for Clouddesigner at this time.

So before building MartServer project, update the file pom.xml with your desired extension and connectors and then :

```
cd MartServer/
mvn clean install
```

## Start the server
Launch the server with this command : 

```
cd MartServer/
mvn exec:java
```
And that's all !

You may have a result like this one :


```[INFO] Scanning for projects...```

```[INFO]                                                                         ```

```[INFO] ------------------------------------------------------------------------```

```[INFO] Building MartServer 1.0-SNAPSHOT```

```[INFO] ------------------------------------------------------------------------```

```[INFO] ```

```[INFO] --- exec-maven-plugin:1.5.0:java (default-cli) @ MartServer ---```

```INFO  log - Logging initialized @1696ms```

```WARN  ContextHandler - o.e.j.s.ServletContextHandler@4f60df4f{/,null,null} contextPath ends with /*```

```WARN  ContextHandler - Empty contextPath```

```INFO  MART - OCCIware MART initializing...```

```INFO  MART -   Scanning all plugin.xml found in the classpath...```

```INFO  MART -     - EMF package org.occiware.clouddesigner.occi.OCCIPackage at http://schemas.ogf.org/occi/core/ecore registered.```

```INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .occie registered.```

```INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .occic registered.```

```INFO  MART -     - OCCI extension http://schemas.ogf.org/occi/core# contained in model/Core.occie registered.```

```INFO  MART -     - EMF package org.occiware.clouddesigner.occi.infrastructure.InfrastructurePackage at http://schemas.ogf.org/occi/infrastructure/ecore registered.```

```INFO  MART -     - Ecore factory org.occiware.clouddesigner.occi.util.OCCIResourceFactoryImpl for file extension .infrastructure registered.```

```INFO  MART -     - OCCI extension http://schemas.ogf.org/occi/infrastructure# contained in model/Infrastructure.occie registered.```

```INFO  MART - OCCIware MART initialized.```

```INFO  ConfigurationManager - Configuration for user anonymous created```

```INFO  ConfigurationManager - Collection: [http://schemas.ogf.org/occi/infrastructure#, http://schemas.ogf.org/occi/core#]```

```INFO  ConfigurationManager - Loading model extension : http://schemas.ogf.org/occi/infrastructure#```

```INFO  ConfigurationManager - Loading model extension : http://schemas.ogf.org/occi/core#```

```INFO  ConfigurationManager - Extension : core added to user configuration.```

```INFO  ConfigurationManager - Extension : infrastructure added to user configuration.```

```INFO  Server - jetty-9.3.9.v20160517```

```INFO  ContextHandler - Started o.e.j.s.ServletContextHandler@4f60df4f{/,null,AVAILABLE}```

```INFO  AbstractConnector - Started ServerConnector@671f100a{HTTP/1.1,[http/1.1]}{0.0.0.0:9090}```

```INFO  Server - Started @2695ms```

The extensions you have registered in pom.xml must appear in the output log, here i have OCCI core extension and OCCI core infrastructure.

## Stop server
Simply kill the java main thread (pkill -9 java) or ctrl+c.


## Accessing the server.
The server by default is on port 9090. 
In a near future, you may edit a propertie file to define the port, and other things. This documentation will be updated accordingly.
If you have launch the server in localhost, you can check that the server is started correctly with this curl command:
```
curl -v -X GET http://localhost:9090/.well-known/org/ogf/occi/-/ -H "accept: application/json"
```

In content result, you will have a json string with the full interface supported by this server.
Like this:
```*   Trying ::1...```

```* Connected to localhost (::1) port 9090 (#0)```

```> GET /.well-known/org/ogf/occi/-/ HTTP/1.1```

```> Host: localhost:9090```

```> User-Agent: curl/7.43.0```

```> accept: application/json```

```> ```

```< HTTP/1.1 200 OK```

```< Date: Sat, 05 Nov 2016 09:48:55 GMT```

```< Server: OCCIWare MART Server v1.0 OCCI/1.2```

```< Content-Type: application/occi+json```

```< Accept: text/occi;application/json;application/occi+json;text/plain```

```< Transfer-Encoding: chunked```

``` < ```
 
``` { ```

```  "actions" : [ {```

```    "scheme" : "http://schemas.ogf.org/occi/infrastructure/network/action#",```

```    "term" : "up",```

```    "title" : ""```

```  }, {```

```    "scheme" : "http://schemas.ogf.org/occi/infrastructure/network/action#",```

```    "term" : "down",```

```    "title" : ""```

```  }, {```

```    "scheme" : "http://schemas.ogf.org/occi/infrastructure/compute/action#",```

```    "term" : "start",```

```    "title" : "Start the system"```

```  }, {```

```    "attributes" : {```

```      "method" : {```

```        "mutable" : true,```

```        "required" : true,```

```        "pattern" : {```

```          "$schema" : "http://json-schema.org/draft-04/schema#",```

```          "type" : "string"```

```        },```

```        "type" : "string"```

```      }```

```    },```

```    "scheme" : "http://schemas.ogf.org/occi/infrastructure/compute/action#",```

```    "term" : "stop",```

```    "title" : "Stop the system (graceful, acpioff or poweroff)"```

```  }, {```

``` ... ```


## Logs output
The output for now is directly done on standard output console, but you can redirect output to a file.
In a near future, the logs will appear in a log file. This documentation will be updated accordingly.

## Issues
Do not hesitate to input new issues if you found a bug or if you have suggestion to make it better.
