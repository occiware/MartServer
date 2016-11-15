# User documentation json parser.
This give some examples of usage with json input and output.

All theses examples use curl to execute queries on the server.

Important: You can define Content-type to text/occi and accept-type to application/json.

Also, you can define Content-Type to application/json and accept-type to text/occi.

application/json and application/occi+json will give the same input/output content.


## Get the query interface
```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/ -H "accept: application/json"```
You can also use the path : "/-/".

```curl -v -X GET http://localhost:8080/-/ -H "accept: application/json"```


## Get the query interface for a single category

```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/?category=compute -H "accept: application/json"```

## Create a resource
<pre>
<code>
{
    "id": "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
    "title": "compute4",
    "summary": "My only compute for test with single resource",
    "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
    "attributes": {
        "occi.compute.speed": 3.0,
        "occi.compute.memory": 4.0,
        "occi.compute.cores": 4,
        "occi.compute.architecture": "x86",
        "occi.compute.state": "active"
    }
}
</code>
</pre>

Make it as json file and execute the query (this upload the file to the server, adapt it to your needs):

```curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:8080/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"```

or you can use curl with -d switch to define directly the resource (so without a file).
Like this :
<pre>
<code>
curl -v -X PUT -d '{
  "id": "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
  "title": "compute4",
  "summary": "My only compute for test with single resource",
  "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
  "attributes": {
      "occi.compute.speed": 3.0,
      "occi.compute.memory": 4.0,
      "occi.compute.cores": 4,
      "occi.compute.architecture": "x86",
      "occi.compute.state": "active"
  }
}' http://localhost:8080/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"
</code>
</pre>

## Create a resource with links

<pre>
<code>
{
    "id": "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
    "title": "compute3",
    "summary": "My other compute 3",
    "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
    "attributes": {
        "occi.compute.speed": 1.0,
        "occi.compute.memory": 2.0,
        "occi.compute.cores": 1,
        "occi.compute.architecture": "x86",
        "occi.compute.state": "inactive"
    },
    "links": [
        {
            "kind": "http://schemas.ogf.org/occi/infrastructure#networkinterface",
            "mixins": [
            "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"
            ],
            "attributes": {
                "occi.networkinterface.interface": "eth0",
                "occi.networkinterface.mac": "00:80:41:ae:fd:7e",
                "occi.networkinterface.address": "192.168.0.100",
                "occi.networkinterface.gateway": "192.168.0.1",
                "occi.networkinterface.allocation": "dynamic"
            },
            "actions": [
                "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up",
                "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"
            ],
            "id": "urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
            "target": {
            "location": "/network/c7d55bf4-7057-5113-85c8-141871bf7636",
            "kind": "http://schemas.ogf.org/occi/infrastructure#network"
            },
            "source": {
                "location": "/compute/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8"
            }
        }
    ]
}
</code>
</pre>


Execute the query (adapt to your needs) :

```curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:8080/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"```

Note: If you didn't create the target network before, this query will not work because the network target doesn't exist in your configuration. 
You must create it before :
<pre>
<code>
curl -v -X PUT -d '{
    "id": "urn:uuid:c7d55bf4-7057-5113-85c8-141871bf7636",
    "title": "network2",
    "summary": "My second network",
    "kind": "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
    ],
    "attributes": {
        "occi.network.vlan": 14,
        "occi.network.label": "private",
        "occi.network.address": "10.1.0.0/16",
        "occi.network.gateway": "10.1.255.254"
    }
 }' -H 'Content-Type: application/occi+json' -H 'accept: application/occi+json' http://localhost:8080/
</code>
</pre>

And after execute the query :
<pre>
<code>
curl -v -X PUT -d '{
    "id": "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
    "title": "compute3",
    "summary": "My other compute 3",
    "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
    "attributes": {
        "occi.compute.speed": 1.0,
        "occi.compute.memory": 2.0,
        "occi.compute.cores": 1,
        "occi.compute.architecture": "x86",
        "occi.compute.state": "inactive"
    },
    "links": [
        {
            "kind": "http://schemas.ogf.org/occi/infrastructure#networkinterface",
            "mixins": [
            "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"
            ],
            "attributes": {
                "occi.networkinterface.interface": "eth0",
                "occi.networkinterface.mac": "00:80:41:ae:fd:7e",
                "occi.networkinterface.address": "192.168.0.100",
                "occi.networkinterface.gateway": "192.168.0.1",
                "occi.networkinterface.allocation": "dynamic"
            },
            "actions": [
                "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up",
                "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"
            ],
            "id": "urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
            "target": {
            "location": "/network/c7d55bf4-7057-5113-85c8-141871bf7636",
            "kind": "http://schemas.ogf.org/occi/infrastructure#network"
            },
            "source": {
                "location": "/compute/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8"
            }
        }
    ]
}' -H 'Content-Type: application/occi+json' -H 'accept: application/occi+json' http://localhost:8080/
</code>
</pre>


So, this will create the compute with a network interface linked to a previous created network.


## Create a full collection of resources (with association of mixins and mixin tag definitions).

<pre>
<code>
{
    "resources": [
        {
            "id": "urn:uuid:c7d55bf4-7057-5113-85c8-141871bf7636",
            "title": "network2",
            "summary": "My second network",
            "kind": "http://schemas.ogf.org/occi/infrastructure#network",
            "mixins": [
                "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
            ],
            "attributes": {
                "occi.network.vlan": 14,
                "occi.network.label": "private",
                "occi.network.address": "10.1.0.0/16",
                "occi.network.gateway": "10.1.255.254"
            }
        },
        {
            "id": "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
            "title": "compute3",
            "summary": "My other compute 3",
            "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
            "attributes": {
                "occi.compute.speed": 1.0,
                "occi.compute.memory": 2.0,
                "occi.compute.cores": 1,
                "occi.compute.architecture": "x86",
                "occi.compute.state": "inactive"
            },
            "links": [
                {
                    "kind": "http://schemas.ogf.org/occi/infrastructure#networkinterface",
                    "mixins": [
                        "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"
                    ],
                    "attributes": {
                        "occi.networkinterface.interface": "eth0",
                        "occi.networkinterface.mac": "00:80:41:ae:fd:7e",
                        "occi.networkinterface.address": "192.168.0.100",
                        "occi.networkinterface.gateway": "192.168.0.1",
                        "occi.networkinterface.allocation": "dynamic"
                    },
                    "actions": [
                        "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up",
                        "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"
                    ],
                    "id": "urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
                    "target": {
                        "location": "/network/c7d55bf4-7057-5113-85c8-141871bf7636",
                        "kind": "http://schemas.ogf.org/occi/infrastructure#network"
                    },
                    "source": {
                        "location": "/compute/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8"
                    }
                }
            ]
        }

    ],
    "mixins": [
        {
            "location": "/mixins/my_mixin/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin",
            "attributes": {},
            "title": "my mixin tag 1"
        },
        {
            "location": "/mixins/my_mixin2/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin2",
            "attributes": {},
            "title": "my mixin tag 2"
        }
    ]
}
</code>
</pre>

```curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:8080/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"```

The "mixins" section defines mixin tag on current configuration.

## Update resources attributes

You can define attributes with the same query as you created the resources but with a POST method.

```curl -v -X POST --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:8080/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"```

### Use case :

* Redefine an attribute of our network created before : 
        occi.network.vlan --< 14
    now it will be :
        occi.network.vlan --< 50
<pre>
<code>
  curl -v -X POST -d '{ 
      "id": "urn:uuid:c7d55bf4-7057-5113-85c8-141871bf7636",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "attributes": {
          "occi.network.vlan": 50
      }
  }' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/
</code>
</pre>    
* Redefine the title of our compute :
<pre>
<code>
  curl -v -X POST -d '{ 
      "id": "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
      "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
      "title": "This is our compute"
  }' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/
</code>
</pre> 

Note: Id, title and summary must be set out of attributes values.


## Retrieve your resources

Please note that the relative path of your resources to find is important.

You can search directly with the resource location path (including uuid) :

```curl -v -X GET http://localhost:8080/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8 -H "accept: application/json" ```

This must retrieve one resource for this uuid : a1cf3896-500e-48d8-a3f5-a8b3601bcdd8.

If you don't know the path of your resource, you can use its kind for example, for a compute, you can do this :
```curl -v -X GET http://localhost:8080/compute/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8 -H "accept: application/json" ```

This will return :
<pre>
<code>
> GET /compute/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: application/json
> 
< HTTP/1.1 200 OK
< Date: Tue, 15 Nov 2016 09:01:01 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Accept: text/occi;application/json;application/occi+json;text/plain
< Content-Type: application/json
< Content-Length: 1980
< 
{
  "resources" : [ {
    <b>"id" : "a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",</b>
    "title" : "compute3",
    "summary" : "My other compute 3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ ],
    "attributes" : {
      "occi.core.id" : "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 1,
      "occi.compute.speed" : 1.0,
      "occi.compute.memory" : 2.0,
      "occi.compute.state" : "inactive"
    },
    "links" : [ {
      "id" : "b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
      "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
      "attributes" : {
        "occi.core.id" : "urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
        "occi.networkinterface.interface" : "eth0",
        "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
        "occi.networkinterface.address" : "192.168.0.100",
        "occi.networkinterface.gateway" : "192.168.0.1",
        "occi.networkinterface.allocation" : "dynamic"
      },
      "actions" : [ ],
      "location" : "/b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
      "source" : {
        "location" : "/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
      },
      "target" : {
        "location" : "/c7d55bf4-7057-5113-85c8-141871bf7636",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
      }
    } ],
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8"
  } ]
}
</code>
</pre>

### Retrieve your collection of resources with filter path

- If you have defined your resources on a custom path : http://localhost:8080/myresources/ so to retrieve your resources :

```curl -v -X GET http://localhost:8080/myresources/ -H "accept: application/occi+json"```

- You can't remember where you have defined your resources but you know the category :

```curl -v -X GET http://localhost:8080/mycategory/ -H "accept: application/occi+json"```

This will give you all the resources for the mycategory. 

To have location only :

```curl -v -X GET http://localhost:8080/mycategory/ -H "accept: text/uri-list" ```

For all Compute kind :
```curl -v -X GET http://localhost:8080/compute/ -H "accept: text/uri-list" ```

So with an infrastructure Compute kind :

```curl -v -X GET http://localhost:8080/compute/ -H "accept: application/occi+json"```

The result must give a collection of computes like this: 
<pre>
<code>
< HTTP/1.1 200 OK
< Date: Mon, 14 Nov 2016 10:31:17 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Accept: text/occi;application/json;application/occi+json;text/plain
< Content-Type: application/json
< Content-Length: 2921
< 
{
  "resources" : [ {
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "title" : "compute4",
    "summary" : "My only compute for test with single resource",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ ],
    "attributes" : {
      "occi.core.id" : "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 4,
      "occi.compute.speed" : 3.0,
      "occi.compute.memory" : 4.0,
      "occi.compute.state" : "active"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/d99486b7-0632-482d-a184-a9195733ddd3"
  }, {
    "id" : "a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
    "title" : "This is our compute",
    "summary" : "My other compute 3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ ],
    "attributes" : {
      "occi.core.id" : "urn:uuid:a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 1,
      "occi.compute.speed" : 1.0,
      "occi.compute.memory" : 2.0,
      "occi.compute.state" : "inactive"
    },
    "links" : [ {
      "id" : "b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
      "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
      "attributes" : {
        "occi.core.id" : "urn:uuid:b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
        "occi.networkinterface.interface" : "eth0",
        "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
        "occi.networkinterface.address" : "192.168.0.100",
        "occi.networkinterface.gateway" : "192.168.0.1",
        "occi.networkinterface.allocation" : "dynamic"
      },
      "actions" : [ ],
      "location" : "/b2fe83ae-a20f-54fc-b436-cec85c94c5e9",
      "source" : {
        "location" : "/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
      },
      "target" : {
        "location" : "/c7d55bf4-7057-5113-85c8-141871bf7636",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
      }
    } ],
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8"
  } ]
}
</code>
</pre>

### Retrieve your resources with filter parameters

#### Retrieve all computes with attributes occi.compute.state equals to active
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=compute&attribute=occi.compute.state&value=active' -H 'accept: application/json'
</code>
</pre>

#### Retrieve all networks with total number of elements per page equals to 5 and current page = 1 and attribute occi.network.label contains "priv"
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=network&attribute=occi.network.label&value=priv&page=1&number=5&operator=1' -H 'accept: application/json'
</code>
</pre>


## Define mixin tags

Mixin tags are user mixin definition without attributes.
This is to "tag" the resource and easily get it.
It must be defined in your configuration before using it.

You can define it directly in a collection like this:

<pre>
<code>
{
    "mixins": [
        {
            "location": "/mymixins/my_mixin_first/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin_first",
            "attributes": {},
            "title": "my mixin tag first"
        },
        {
            "location": "/mymixins/my_mixin_two/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin_two",
            "attributes": {},
            "title": "my mixin tag two"
        }
    ]
}
</code>
</pre>

To define them and add them to your configuration : 

<pre>
<code>
curl -v -X PUT -d '{
    "mixins": [
        {
            "location": "/mymixins/my_mixin_first/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin_first",
            "attributes": {},
            "title": "my mixin tag first"
        },
        {
            "location": "/mymixins/my_mixin_two/",
            "scheme": "http://occiware.org/occi/tags#",
            "term": "my_mixin_two",
            "attributes": {},
            "title": "my mixin tag two"
        }
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mymixins/
</code>
</pre>

To retrieve the definition :
<pre>
<code>
curl -v -X GET http://localhost:8080/-/?category=my_mixin_first -H 'accept: application/json'
curl -v -X GET http://localhost:8080/-/?category=my_mixin_two -H 'accept: application/json'
</code>
</pre>

You can also define one by one like this:
<pre>
<code>
curl -v -X PUT -d '{
    "location": "/mymixins/my_mixin3/",
    "scheme": "http://occiware.org/occi/tags#",
    "term": "my_mixin3",
    "attributes": {},
    "title": "my mixin tag 3"
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mymixins/
</code>
</pre>

To retrieve your mixin definition (other method) : 
<pre>
<code>
curl -v -X GET http://localhost:8080/mymixins/my_mixin3/-/ -H 'accept: application/json'
</code>
</pre>


## Associate a mixin tag to an entity
The mixin tag must be defined before associating it with an entity.

You have created before a compute resource, named "compute4".

You may tag it with my_mixin_first :

<pre>
<code>
curl -v -X POST http://localhost:8080/ -d '
{
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
                "http://occiware.org/occi/tags#my_mixin_first"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>

Result:

<pre>
<code>
{
  "resources" : [ {
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "title" : "compute4",
    "summary" : "My only compute for test with single resource",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    <b>"mixins" : [ "http://occiware.org/occi/tags#my_mixin_first" ]</b>,
    "attributes" : {
      "occi.core.id" : "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 4,
      "occi.compute.speed" : 3.0,
      "occi.compute.memory" : 4.0,
      "occi.compute.state" : "active"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/d99486b7-0632-482d-a184-a9195733ddd3"
  } ]
}
</code>
</pre>

## Get a resource via a mixin category

You can find your entity via your mixin tag, this is usefull if you have a lot of resources.

<pre>
<code>
curl -v -X GET http://localhost:8080/my_mixin_first/ -H 'accept: application/json'
</code>
</pre>

This give this result : 

<pre>
<code>
> GET /my_mixin_first/ HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> accept: application/json
> 
< HTTP/1.1 200 OK
< Date: Mon, 14 Nov 2016 16:22:38 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Accept: text/occi;application/json;application/occi+json;text/plain
< Content-Type: application/json
< Content-Length: 1001
< 
{
  "resources" : [ {
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "title" : "compute4",
    "summary" : "My only compute for test with single resource",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ "http://occiware.org/occi/tags#my_mixin_first" ],
    "attributes" : {
      "occi.core.id" : "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 4,
      "occi.compute.speed" : 3.0,
      "occi.compute.memory" : 4.0,
      "occi.compute.state" : "active"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/d99486b7-0632-482d-a184-a9195733ddd3"
  } ]
}
</code>
</pre>

## GET query with mixin tag

You can have the same result with a category filter :

<pre>
<code>
curl -v -X GET http://localhost:8080/?category=my_mixin_first -H 'accept: application/json'
</code>
</pre>


## Associate a mixin extension to an entity
In fact a mixin tag is a mixin, so to associate a mixin extension with a resource it's the same query.
But we don't have to define the mixin before, this is already done on extension level.
For example, we associate the mixin ssh_key to the "compute4" resource : 
<pre>
<code>
curl -v -X POST http://localhost:8080/ -d '
{
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
                "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>

You can also set a ssh key like :
<pre>
<code>
curl -v -X POST -d '
{ 
  "id": "d99486b7-0632-482d-a184-a9195733ddd3",
  "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
  "attributes": {
    "occi.credentials.ssh.publickey":"My ssh key to define"
  }      
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/
</code>
</pre>


In result:
<pre>
<code>
> POST / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.43.0
> Content-Type: application/json
> accept: application/json
> Content-Length: 205
> 
* upload completely sent off: 205 out of 205 bytes
< HTTP/1.1 200 OK
< Date: Mon, 14 Nov 2016 16:48:21 GMT
< Server: OCCIWare MART Server v1.0 OCCI/1.2
< Accept: text/occi;application/json;application/occi+json;text/plain
< Content-Type: application/json
< Content-Length: 1132
< 
{
  "resources" : [ {
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "title" : "compute4",
    "summary" : "My only compute for test with single resource",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ "http://occiware.org/occi/tags#my_mixin_first", "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key" ],
    "attributes" : {
      "occi.core.id" : "urn:uuid:d99486b7-0632-482d-a184-a9195733ddd3",
      "occi.compute.architecture" : "x86",
      "occi.compute.cores" : 4,
      "occi.compute.speed" : 3.0,
      "occi.compute.memory" : 4.0,
      "occi.compute.state" : "active",
      "occi.credentials.ssh.publickey" : "My ssh key to define"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/d99486b7-0632-482d-a184-a9195733ddd3"
  } ]
}
</code>
</pre>

## Dissociate a mixin tag from an entity
It's the same query as association but it's with DELETE method.

<pre>
<code>
curl -v -X DELETE http://localhost:8080/ -d '
{
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
                "http://occiware.org/occi/tags#my_mixin_first"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>


## Dissociate a mixin extension from entity
<pre>
<code>
curl -v -X DELETE http://localhost:8080/ -d '
{
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
         "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>


## Remove a mixin tag definition
<pre>
<code>
curl -v -X DELETE -d '{
  "location": "/mymixins/my_mixin_two/",
  "scheme": "http://occiware.org/occi/tags#",
  "term": "my_mixin_two"
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/-/
</code>
</pre>


## execute an action on a resource
This example illustrate a stop instance.

<pre>
<code>
curl -v -X POST -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/compute/d99486b7-0632-482d-a184-a9195733ddd3?action=stop
</code>
</pre>


## execute actions on a collection

For example stop all the computes : 
<pre>
<code>
curl -v -X POST -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/compute/?action=stop
</code>
</pre>

On custom instance collection path :

<pre>
<code>
curl -v -X POST -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/vms/foo/bar/?action=stop
</code>
</pre>


## Delete entity
<pre>
<code>
curl -v -X DELETE -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/vms/foo/bar/?action=stop
</code>
</pre>



## Delete all entities of a collection

### Category

This example illustrate a delete query on all computes.
<pre>
<code>
curl -v -X DELETE -H 'accept: application/json' http://localhost:8080/compute/
</code>
</pre>


### On custom path
<pre>
<code>
curl -v -X DELETE -H 'accept: application/json' http://localhost:8080/vms/foo/bar/
</code>
</pre>


