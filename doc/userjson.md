# User documentation json parser.
This give some examples of usage with json input and output.
All theses examples use curl to execute queries on the server.

## Get the query interface
```
curl -v -X GET http://localhost:9090/.well-known/org/ogf/occi/-/ -H "accept: application/json"
```
You can also use the path : "/-/".
```
curl -v -X GET http://localhost:9090/-/ -H "accept: application/json"
```


## Get the query interface for a single category
```
curl -v -X GET http://localhost:9090/.well-known/org/ogf/occi/-/?category=compute -H "accept: application/json"
```



## Create a resource


## Create a resource with links


## Define mixin tags


## Create a full collection of resources (with association of mixins and mixin tag definitions).


## Update entity (ies)


## execute an action


## execute a sequence of actions


## Delete entity (ies)



