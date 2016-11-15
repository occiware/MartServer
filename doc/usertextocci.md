# User documentation text/occi parser.
This give some examples of usage with json input and output.

All theses examples use curl to execute queries on the server.

Important: You can define Content-type to text/occi and accept-type to application/json.

Also, you can define Content-Type to application/json and accept-type to text/occi.


## Get the query interface
```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/ -H "accept: text/occi"```
You can also use the path : "/-/".

```curl -v -X GET http://localhost:8080/-/ -H "accept: text/occi"```


## Get the query interface for a single category

```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/?category=compute -H "accept: text/occi"```

## Create a resource

<pre>
<code>
curl -v -X PUT http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0 -H 'Content-Type: text/occi' -H 'Category: compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";' -H 'X-OCCI-Attribute: occi.core.id="f88486b7-0632-482d-a184-a9195733ddd0"' -H 'X-OCCI-Attribute: occi.core.title="compute1"' -H 'X-OCCI-Attribute: occi.core.summary="Test compute summary"' -H 'X-OCCI-Attribute: occi.compute.architecture="x64"' -H 'X-OCCI-Attribute: occi.compute.cores=2' -H 'X-OCCI-Attribute: occi.compute.hostname="hostCompute"' -H 'X-OCCI-Attribute: occi.compute.share=0' -H 'X-OCCI-Attribute: occi.compute.speed=1.0' -H 'X-OCCI-Attribute: occi.compute.memory=2.0' -H 'X-OCCI-Attribute: occi.compute.state="inactive"'
</code>
</pre>

## Create a resource with links

First create the target resource of the link, here this is a network resource:
<pre>
<code>
curl -v -X PUT http://localhost:8080/network/network1 -H 'Content-Type: text/occi' -H 'Category: network; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind", ipnetwork; scheme="http://schemas.ogf.org/occi/infrastructure/network#"; class="mixin";' -H 'X-OCCI-Attribute: occi.network.vlan=12' -H 'X-OCCI-Attribute: occi.network.label="private"' -H 'X-OCCI-Attribute: occi.network.address="10.1.0.0/16"' -H 'X-OCCI-Attribute: occi.network.gateway="10.1.255.254"'
</code>
</pre>

The source of the link has been created before with a compute resource. Now the link network interface:

<pre>
<code>
curl -v -X PUT http://localhost:8080/networkinterface/networkadapter1 -H 'Content-Type: text/occi' -H 'Category: networkinterface; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind", ipnetworkinterface; scheme="http://schemas.ogf.org/occi/infrastructure/networkinterface#"; class="mixin";' -H 'X-OCCI-Attribute: occi.networkinterface.interface="eth0"' -H 'X-OCCI-Attribute: occi.core.title="Adaptateur réseau 1"' -H 'X-OCCI-Attribute: occi.core.source="/vm/f88486b7-0632-482d-a184-a9195733ddd0", occi.core.target="/network/network1"'
</code>
</pre>


Note: If you didn't create the target network before, this query will not work because the network target doesn't exist in your configuration. 
That's why the create resource order is important.
So, this will create the compute with a network interface linked to a previous created network.
In fact, in network interface create query we have added a mixin "ipnetworkinterface" in Category section.


## Update resources attributes

You can update attributes definitions with a POST method.

### Use case :

* Redefine an attribute of our network created before : 
        occi.network.vlan --< 14
    now it will be :
        occi.network.vlan --< 50        
<pre>
<code>
curl -v -X POST  http://localhost:8080/network/network1/ -H 'Content-Type: text/occi' -H 'Category: network; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";' -H 'X-OCCI-Attribute: occi.network.vlan=50'
</code>
</pre>    
* Redefine the title of our compute :
<pre>
<code>
curl -v -X POST  http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0 -H 'Content-Type: text/occi' -H 'Category: compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";' -H 'X-OCCI-Attribute: occi.core.title="testOcciwareRenamed"'
</code>
</pre> 

Note: Id, title and summary must be set out of attributes values.


## Retrieve your resources

Please note that the relative path of your resources to find is important.

You can search directly with the resource location path (including uuid) :

```curl -v -X GET http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0 -H "accept: text/occi" ```

This must retrieve one resource for this uuid : f88486b7-0632-482d-a184-a9195733ddd0.

If you don't know the path of your resource, you can use its kind for example, for a compute, you can do this :
```curl -v -X GET http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0 -H "accept: application/json" ```

This will return :
<pre>
<code>
> GET /compute/f88486b7-0632-482d-a184-a9195733ddd0 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: text/occi
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 15:31:22 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Category: compute;  scheme="http://schemas.ogf.org/occi/infrastructure#";  class="kind";
< X-OCCI-Attribute: occi.core.id="urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0",  occi.core.title="testOcciwareRenamed",  occi.core.summary="Test compute summary",  occi.compute.architecture="x64",  occi.compute.cores=2,  occi.compute.hostname="hostCompute",  occi.compute.share=0,  occi.compute.speed=1.0,  occi.compute.memory=2.0,  occi.compute.state="inactive",
< X-OCCI-Location: http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0
< Content-Type: text/occi
< Accept: text/occi;application/json;application/occi+json;text/plain
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; title="compute"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#start"; title="start"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#stop"; title="stop"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#restart"; title="restart"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#suspend"; title="suspend"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#save"; title="save"
< Content-Length: 2
< 
* Connection #0 to host localhost left intact
OK
</code>
</pre>

### Retrieve your collection of resources with filter path

- If you have defined your resources on a custom path : http://localhost:8080/myresources/ so to retrieve your resources :

```curl -v -X GET http://localhost:8080/myresources/ -H "accept: text/occi"```

- You can't remember where you have defined your resources but you know the category :

```curl -v -X GET http://localhost:8080/mycategory/ -H "accept: text/occi"```

This will give you all the resources for the mycategory. 

To have location only :

```curl -v -X GET http://localhost:8080/mycategory/ -H "accept: text/uri-list" ```

For all Compute kind :
```curl -v -X GET http://localhost:8080/compute/ -H "accept: text/uri-list" ```

So with an infrastructure Compute kind :

```curl -v -X GET http://localhost:8080/compute/ -H "accept: text/occi"```

With text/occi parser, collection rendering is limited to the first occurrence.
There is a limited buffer (8kB) for header values. It's also better to use text/uri-list or json rendering for collection. 

The result give this: 
<pre>
<code>
> GET /compute/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: text/occi
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 15:33:00 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Category: compute;  scheme="http://schemas.ogf.org/occi/infrastructure#";  class="kind";
< X-OCCI-Attribute: occi.core.id="urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0",  occi.core.title="testOcciwareRenamed",  occi.core.summary="Test compute summary",  occi.compute.architecture="x64",  occi.compute.cores=2,  occi.compute.hostname="hostCompute",  occi.compute.share=0,  occi.compute.speed=1.0,  occi.compute.memory=2.0,  occi.compute.state="inactive",
< X-OCCI-Location: http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0
< Content-Type: text/occi
< Accept: text/occi;application/json;application/occi+json;text/plain
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; title="compute"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#start"; title="start"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#stop"; title="stop"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#restart"; title="restart"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#suspend"; title="suspend"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#save"; title="save"
< Content-Length: 2
< 
* Connection #0 to host localhost left intact
OK
</code>
</pre>

### Retrieve your resources with filter parameters

#### Retrieve all computes with attributes occi.compute.state equals to active
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=compute&attribute=occi.compute.state&value=active' -H 'accept: text/occi'
</code>
</pre>
If the server render a 404 Not found, try with value=inactive
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=compute&attribute=occi.compute.state&value=inactive' -H 'accept: text/occi'
</code>
</pre>

#### Retrieve all networks with total number of elements per page equals to 5 and current page = 1 and attribute occi.network.label contains "priv"
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=network&attribute=occi.network.label&value=priv&page=1&number=5&operator=1' -H 'accept: text/occi'
</code>
</pre>


## Define mixin tags

Mixin tags are user mixin definition without attributes.
This is to "tag" the resource and easily get it.
It must be defined in your configuration before using it.

To define one and add it to your configuration : 

<pre>
<code>
curl -v -X PUT -H 'Category: my_stuff; scheme="http://example.com/occi/my_stuff#"; class="mixin"; location="/my_stuff/"' http://localhost:8080/-/
</code>
</pre>

To retrieve the definition :

<pre>
<code>
curl -v -X GET http://localhost:8080/-/?category=my_stuff -H 'accept: text/occi'
</code>
</pre>

To retrieve your mixin definition (other method) : 
<pre>
<code>
curl -v -X GET http://localhost:8080/my_stuff/my_stuff/-/ -H 'accept: text/occi'
</code>
</pre>

## Associate a mixin tag to an entity
The mixin tag must be defined before associating it with an entity.

You have created before a compute resource.

You may tag it with my_stuff :

<pre>
<code>
curl -v -X POST http://localhost:8080/my_stuff/ -H 'X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0'
</code>
</pre>

Result:

<pre>
<code>
> POST /my_stuff/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> Accept: */*
> X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 15:43:27 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< content: ok
< Accept: text/occi;application/json;application/occi+json;text/plain
< Content-Type: text/occi
< Content-Length: 4
< 
OK
</code>
</pre>

## Get a resource via a mixin category

You can find your entity via your mixin tag, this is useful if you have a lot of resources.

<pre>
<code>
curl -v -X GET http://localhost:8080/my_stuff/ -H 'accept: text/occi'
</code>
</pre>

This give this result : 

<pre>
<code>
> GET /my_stuff/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: text/occi
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 15:45:43 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Category: compute;  scheme="http://schemas.ogf.org/occi/infrastructure#";  class="kind";my_stuff;  scheme="http://example.com/occi/my_stuff#";  class="mixin";
< X-OCCI-Attribute: occi.core.id="urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0",  occi.core.title="testOcciwareRenamed",  occi.core.summary="Test compute summary",  occi.compute.architecture="x64",  occi.compute.cores=2,  occi.compute.hostname="hostCompute",  occi.compute.share=0,  occi.compute.speed=1.0,  occi.compute.memory=2.0,  occi.compute.state="inactive",
< X-OCCI-Location: http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0
< Content-Type: text/occi
< Accept: text/occi;application/json;application/occi+json;text/plain
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; title="compute"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#start"; title="start"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#stop"; title="stop"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#restart"; title="restart"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#suspend"; title="suspend"
< Link: <http://localhost:8080/vm/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#save"; title="save"
< Content-Length: 2
< 
* Connection #0 to host localhost left intact
OK
</code>
</pre>

## GET query with mixin tag

You can have the same result with a category filter :

<pre>
<code>
curl -v -X GET http://localhost:8080/?category=my_stuff -H 'accept: text/occi'
</code>
</pre>


## Associate a mixin extension to an entity
In fact a mixin tag is a mixin, so to associate a mixin extension with a resource it's the same query.
But we don't have to define the mixin before, this is already done on extension level.
For example, we associate the mixin ssh_key to the "testOcciwareRenamed" resource : 
<pre>
<code>
curl -v -X POST http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0 -H 'Category: compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind", ssh_key; scheme="http://schemas.ogf.org/occi/infrastructure/credentials#"; class="mixin";'
</code>
</pre>

You can also set a ssh key like :
<pre>
<code>
curl -v -X POST http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0 -H 'Category: compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";' -H 'X-OCCI-Attribute: occi.credentials.ssh.publickey="My ssh key to define"'
</code>
</pre>


In result:
<pre>
<code>
> POST /compute/f88486b7-0632-482d-a184-a9195733ddd0 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> Accept: */*
> Category: compute; scheme="http://schemas.ogf.org/occi/infrastructure#"; class="kind";
> X-OCCI-Attribute: occi.credentials.ssh.publickey="My ssh key to define"
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 16:08:02 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Category: compute;  scheme="http://schemas.ogf.org/occi/infrastructure#";  class="kind";my_stuff;  scheme="http://example.com/occi/my_stuff#";  class="mixin";ssh_key;  scheme="http://schemas.ogf.org/occi/infrastructure/credentials#";  class="mixin";
< X-OCCI-Attribute: occi.core.id="urn:uuid:f88486b7-0632-482d-a184-a9195733ddd0",  occi.core.title="compute1",  occi.core.summary="Test compute summary",  occi.compute.architecture="x64",  occi.compute.cores=2,  occi.compute.hostname="hostCompute",  occi.compute.speed=1.0,  occi.compute.memory=2.0,  occi.compute.state="inactive",  occi.credentials.ssh.publickey="My ssh key to define",
< X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0
< Content-Type: text/occi
< Accept: text/occi;application/json;application/occi+json;text/plain
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; title="compute"
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#start"; title="start"
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#stop"; title="stop"
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#restart"; title="restart"
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#suspend"; title="suspend"
< Link: <http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0>; rel="http://schemas.ogf.org/occi/infrastructure/compute/action#save"; title="save"
< Content-Length: 2
< 
* Connection #0 to host localhost left intact
OK
</code>
</pre>

## Dissociate a mixin tag from an entity
It's the same query as association but it's with DELETE method.

<pre>
<code>
curl -v -X DELETE http://localhost:8080/my_stuff/ -H 'X-OCCI-Location: http://localhost:8080/compute/f88486b7-0632-482d-a184-a9195733ddd0'
</code>
</pre>


## Dissociate a mixin extension from entity
<pre>
<code>
TODO
</code>
</pre>


## Remove a mixin tag definition
<pre>
<code>
TODO
</code>
</pre>


## execute an action on a resource
This example illustrate a stop instance.

<pre>
<code>
TODO
</code>
</pre>


## execute actions on a collection

For example stop all the computes : 
<pre>
<code>
TODO
</code>
</pre>

On custom instance collection path :

<pre>
<code>
TODO
</code>
</pre>


## Delete entity
<pre>
<code>
TODO
</code>
</pre>



## Delete all entities of a collection

### Category

This example illustrate a delete query on all computes.
<pre>
<code>
curl -v -X DELETE -H 'accept: text/occi' http://localhost:8080/compute/
</code>
</pre>


### On custom path
<pre>
<code>
curl -v -X DELETE -H 'accept: text/occi' http://localhost:8080/vms/foo/bar/
</code>
</pre>



