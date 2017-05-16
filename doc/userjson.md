# User documentation, the json parser
This give some examples of usage with json input and output.

All theses examples use curl to execute queries on the server.

Important: You can define Content-type to text/occi and accept-type to application/json.

Also, you can define Content-Type to application/json and accept-type to text/occi.

application/json and application/occi+json will give the same input/output content.

Note that default parser is set to application/json on input and output.

For the following usage, localhost and port 8080 is assumed, the examples use curl as http client tool.

## Get the query interface 

```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/ -H "accept: application/json"```
You can also use the path : "/-/".

```curl -v -X GET http://localhost:8080/-/ -H "accept: application/json"```


## Get the query interface for a single category

```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/?category=compute -H "accept: application/json"``` 

You can use interface query to describe a mixin tag :

```curl -v -X GET http://localhost:8080/.well-known/org/ogf/occi/-/?category=mymixinterm -H "accept: application/json"```

In case that the model to render is not defined, MartServer will render an empty json "{ }".

## Create resources

### Create one compute on location <i>/mycompute/webserver</i> using PUT method

Prepare a json file like this : 
<pre>
<code>
{
    "title": "webserver",
    "summary": "server webpage for business site",
    "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
    "attributes": {
        "occi.compute.speed": 3.0,
        "occi.compute.memory": 4.0,
        "occi.compute.cores": 8,
        "occi.compute.architecture": "x64",
        "occi.compute.state": "active"
    }
}
</code>
</pre>

Make it as json file and execute the query (this upload the file to the server, adapt it to your needs):

```curl -v -X PUT --data-binary "@/yourabsolutepath/webserver.json" http://localhost:8080/mycompute/webserver -H "Content-Type: application/json" -H "accept: application/json"```

or you can use curl with -d switch to define directly the resource (so without a file).
Like this :
<pre>
<code>
curl -v -X PUT -d '{
  "title": "webserver",
  "summary": "server webpage for business site",
  "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
  "attributes": {
      "occi.compute.speed": 3.0,
      "occi.compute.memory": 4.0,
      "occi.compute.cores": 8,
      "occi.compute.architecture": "x64",
      "occi.compute.state": "active"
  }
}' http://localhost:8080/mycompute/webserver -H "Content-Type: application/json" -H "accept: application/json"
</code>
</pre>

This query will create the compute resource titled <b>webserver</b> on location : <i><b>/mycompute/webserver</b></i> , the <b>result</b> give a json rendering accordingly to accept header value :
<pre>
<code>
{
  <b>"id" : "urn:uuid:49680b9e-0994-4b8c-ba95-6a35c5bff59c",</b>
  "title" : "webserver",
  "summary" : "server webpage for business site",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
  "mixins" : [ ],
  "attributes" : {
    "occi.compute.architecture" : "x64",
    "occi.compute.cores" : 8,
    "occi.compute.share" : 0,
    "occi.compute.speed" : 3.0,
    "occi.compute.memory" : 4.0,
    "occi.compute.state" : "active"
  },
  "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
  <b>"location" : "/mycompute/webserver"</b>
}
</code>
</pre>

<b>Note</b> that all resources (resource and links) have an id attribute (uuid), if "id" attribute is not set, MartServer will create a new uuid for you.

### Create a resource network using category location <i>/network/</i> and POST method 

You can create one resource using POST method but with category location, be aware that the category must be defined on resource(s) to create :
<pre>
<code>
curl -v -X POST -d '{
  "title": "The main switch",
  "summary": "This give a main network resource",
  "kind": "http://schemas.ogf.org/occi/infrastructure#network",
  "mixins": [
    "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
  ],
  "attributes": {
    "occi.network.vlan": 10,
    "occi.network.label": "private",
    "occi.network.address": "10.1.0.0/16",
    "occi.network.gateway": "10.1.255.254"
  }, 
  <b>"location":"/mainnetwork/network2"</b>
  }
}' http://localhost:8080/network/ -H "Content-Type: application/json" -H "accept: application/json"
</code>
</pre>

MartServer will control if category location is defined for the entity to create and if ok, will create the entity in two ways:
- Entity location is set (as made on our example) : create the entity based on location property
- Entity location is <b>not</b> set : create the entity based on category location added with entity uuid, this will be : <i><b>/network/59680b9e-0994-4b8c-ba95-6a35c5bff59e</b></i>

Result :
 
<pre>
<code>
{
  "id" : "urn:uuid:06934c34-796a-4c01-a82c-d68755952445",
  "title" : "The main switch",
  "summary" : "This give a main network resource",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
  "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
  "attributes" : {
    "occi.network.vlan" : 10,
    "occi.network.label" : "private",
    "occi.network.state" : "inactive",
    "occi.network.address" : "10.1.0.0/16",
    "occi.network.gateway" : "10.1.255.254"
  },
  "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
  "location" : "/mainnetwork/network2"
}
</code>
</pre>

### Create a network interface link between our "webserver" and "main switch network" on location <i>/eth0/webserver</i> using PUT method
<pre>
<code>curl -v -X PUT -d '{
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
  "target": {
    <b>"location": "/mainnetwork/network2"</b>
  },
  "source": {
    <b>"location": "/mycompute/webserver"</b>
  }
}' http://localhost:8080/eth0/webserver -H "Content-Type: application/json" -H "accept: application/json"
</code>
</pre>

Result :
<pre>
<code>
{
  "id" : "urn:uuid:47c44239-13ee-47e3-8746-4083e62a00bf",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
  "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
  "attributes" : {
    "occi.networkinterface.interface" : "eth0",
    "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
    "occi.networkinterface.address" : "192.168.0.100",
    "occi.networkinterface.gateway" : "192.168.0.1",
    "occi.networkinterface.allocation" : "dynamic"
  },
  "actions" : [ ],
  "location" : "/eth0/webserver",
  "source" : {
    "location" : "/mycompute/webserver",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
  },
  "target" : {
    "location" : "/mainnetwork/network2",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
  }
}
</code>
</pre>


Note: If you didn't create the target network before, this query will not work because the network target doesn't exist in your configuration. 

If PUT method is used and the resource is already created, this will cause a full entity update (mixins, attributes). 

### Create a collection of networks

You can create a collection of resources related to the category location used with POST method.

To create a collection of computes on location kind <i>/network/</i> :

<pre>
<code>curl -v -X POST -d '{
  "resources": [
    {
      "title": "network1",
      "summary": "first network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 1,
        "occi.network.label": "private",
        "occi.network.address": "10.1.0.0/16",
        "occi.network.gateway": "10.1.255.254"
      },
      "location": "/mainnetwork/network1"
    },
    {
      "title": "network2",
      "summary": "2 network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 12,
        "occi.network.label": "private",
        "occi.network.address": "10.2.0.0/16",
        "occi.network.gateway": "10.2.255.254"
      },
      "location": "/mainnetwork/network2"
    },
    {
      "title": "network3",
      "summary": "My third network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 10,
        "occi.network.label": "private",
        "occi.network.address": "10.3.0.0/16",
        "occi.network.gateway": "10.3.255.254"
      },
      "location": "/mainnetwork/network3"
    },
    {
      "title": "network4",
      "summary": "4 network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 10,
        "occi.network.label": "private",
        "occi.network.address": "10.4.0.0/16",
        "occi.network.gateway": "10.4.255.254"
      },
      "location": "/mainnetwork/network4"
    },
    {
      "title": "network5",
      "summary": "5 network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 9,
        "occi.network.label": "private",
        "occi.network.address": "10.5.0.0/16",
        "occi.network.gateway": "10.5.255.254"
      },
      "location": "/mainnetwork/network5"
    }
  ]
}' http://localhost:8080/network/ -H "Content-Type: application/json" -H "accept: application/json"
</code>
</pre>

Result :

<pre>
<code>
{
  "resources" : [ {
    "id" : "urn:uuid:3c76c03a-8c1e-4a5f-8ec5-8ccccd7932dc",
    "title" : "network1",
    "summary" : "first network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 1,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.1.0.0/16",
      "occi.network.gateway" : "10.1.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network1"
  }, {
    "id" : "urn:uuid:06934c34-796a-4c01-a82c-d68755952445",
    "title" : "network2",
    "summary" : "2 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 12,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.2.0.0/16",
      "occi.network.gateway" : "10.2.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network2"
  }, {
    "id" : "urn:uuid:97cb2eba-4ca4-4f9b-8666-3340316669ca",
    "title" : "network3",
    "summary" : "My third network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.3.0.0/16",
      "occi.network.gateway" : "10.3.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network3"
  }, {
    "id" : "urn:uuid:4a57153a-1e77-4a94-ae8c-1cccc6458e20",
    "title" : "network4",
    "summary" : "4 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.4.0.0/16",
      "occi.network.gateway" : "10.4.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network4"
  }, {
    "id" : "urn:uuid:a6af7443-a579-4249-b5f3-de2e68a10a24",
    "title" : "network5",
    "summary" : "5 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 9,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.5.0.0/16",
      "occi.network.gateway" : "10.5.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network5"
  } ]
}
</code>
</pre>
Each network will be created on its location defined, if no location is set, the resource will be created on location /network/ + resource unique identifier (uuid => id field). 
If you observe the json rendering result, network2 has been updated, therefore network2 resource was already created.

Same operation with networkinterface links :
<pre>
<code>
curl -v -X POST -d '{
  "links": [
    {
      "title":"neteth1",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
      "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
      "attributes" : {
        "occi.networkinterface.interface" : "eth1",
        "occi.networkinterface.mac" : "00:81:41:ae:fd:7e",
        "occi.networkinterface.address" : "192.168.1.100",
        "occi.networkinterface.gateway" : "192.168.0.1",
        "occi.networkinterface.allocation" : "static"
      },
      "location" : "/eth1/webserver",
      "source" : {
        "location" : "/mycompute/webserver",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
      },
      "target" : {
        "location" : "/mainnetwork/network1",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
      }
    },
    {
      "title":"neteth2",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
      "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
      "attributes" : {
            "occi.networkinterface.interface" : "eth1",
            "occi.networkinterface.mac" : "00:81:41:ae:fd:7e",
            "occi.networkinterface.address" : "192.168.1.150",
            "occi.networkinterface.gateway" : "192.168.0.1",
            "occi.networkinterface.allocation" : "static"
      },
      "location" : "/eth2/webserver",
      "source" : {
         "location" : "/mycompute/webserver",
         "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
      },
      "target" : {
          "location" : "/mainnetwork/network3",
          "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
      }
    }
  ]
}' http://localhost:8080/networkinterface/ -H "Content-Type: application/json" -H "accept: application/json"
</code>
</pre>

Or you can create an entire collection with root location <b>/</b> :

<pre>
<code>
curl -v -X POST -d '{
  "resources": [
    {
      "id": "urn:uuid:f7d55bf4-7057-5113-85c8-141871bf7635",
      "title": "network3",
      "summary": "My third network",
      "kind": "http://schemas.ogf.org/occi/infrastructure#network",
      "mixins": [
        "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork"
      ],
      "attributes": {
        "occi.network.vlan": 10,
        "occi.network.label": "private",
        "occi.network.address": "10.1.0.0/16",
        "occi.network.gateway": "10.1.255.254"
      },
      "location": "/mynetwork/third/"
    },
    {
      "id": "urn:uuid:ffcf3896-500e-48d8-a3f5-a8b3601bcdd9",
      "title": "mycomputefortesting",
      "summary": "My other compute to test",
      "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
      "attributes": {
        "occi.compute.speed": 1,
        "occi.compute.memory": 2,
        "occi.compute.cores": 1,
        "occi.compute.architecture": "x86",
        "occi.compute.state": "active"
      },
      "location": "/compute/ffcf3896-500e-48d8-a3f5-a8b3601bcdd9",
      "links": [
        {
          "kind": "http://schemas.ogf.org/occi/infrastructure#networkinterface",
          "mixins": [
            "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface"
          ],
          "attributes": {
            "occi.networkinterface.interface": "eth0",
            "occi.networkinterface.mac": "00:80:41:ae:fd:7e",
            "occi.networkinterface.address": "192.168.0.50",
            "occi.networkinterface.gateway": "192.168.0.1",
            "occi.networkinterface.allocation": "dynamic"
          },
          "actions": [
            "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#up",
            "http://schemas.ogf.org/occi/infrastructure/networkinterface/action#down"
          ],
          "id": "urn:uuid:24fe83ae-a20f-54fc-b436-cec85c94c5e8",
          "target": {
            "location": "/mynetwork/third/",
            "kind": "http://schemas.ogf.org/occi/infrastructure#network"
          },
          "source": {
            "location": "/compute/ffcf3896-500e-48d8-a3f5-a8b3601bcdd9"
          }
        }
      ]
    }
  ]
}' http://localhost:8080/ -H "Content-Type: application/json" -H "accept: application/json"
</pre>
</code>

## Retrieve resources
To retrieve resources, you must use GET method.

### Retrieve one compute resource
You can search directly with the resource location <i><b>/mycompute/webserver</b></i> :

```curl -v -X GET http://localhost:8080/mycompute/webserver -H "accept: application/json" ```

Result:

<pre>
<code>
{
  "id" : "urn:uuid:49680b9e-0994-4b8c-ba95-6a35c5bff59c",
  "title" : "webserver",
  "summary" : "server webpage for business site",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
  "mixins" : [ ],
  "attributes" : {
    "occi.compute.architecture" : "x64",
    "occi.compute.cores" : 8,
    "occi.compute.share" : 0,
    "occi.compute.speed" : 3.0,
    "occi.compute.memory" : 4.0,
    "occi.compute.state" : "active"
  },
  "links" : [ {
    "id" : "urn:uuid:47c44239-13ee-47e3-8746-4083e62a00bf",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
    "attributes" : {
      "occi.networkinterface.interface" : "eth0",
      "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
      "occi.networkinterface.address" : "192.168.0.100",
      "occi.networkinterface.gateway" : "192.168.0.1",
      "occi.networkinterface.allocation" : "dynamic"
    },
    "actions" : [ ],
    "location" : "/eth0/webserver",
    "source" : {
      "location" : "/mycompute/webserver",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
    },
    "target" : {
      "location" : "/mainnetwork/network2",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
    }
  } ],
  "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
  "location" : "/mycompute/webserver"
}
</code>
</pre>

If resource is not found, this will give a json formatted message and HTTP 404 error (not found).
<pre>
<code>
{
  "message" : "Resource not found on location : /mycompute/webserver"
}
</code>
</pre>

### Retrieve all resources and links :
To retrieve all collection of resources use the root location "/" :

```curl -v -X GET http://localhost:8080/ -H "accept: application/json" ```

This will result :
<pre>
<code>
{
  "resources" : [ {
    "id" : "urn:uuid:3c76c03a-8c1e-4a5f-8ec5-8ccccd7932dc",
    "title" : "network1",
    "summary" : "first network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 1,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.1.0.0/16",
      "occi.network.gateway" : "10.1.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network1"
  }, {
    "id" : "urn:uuid:4a57153a-1e77-4a94-ae8c-1cccc6458e20",
    "title" : "network4",
    "summary" : "4 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.4.0.0/16",
      "occi.network.gateway" : "10.4.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network4"
  }, {
    "id" : "urn:uuid:06934c34-796a-4c01-a82c-d68755952445",
    "title" : "network2",
    "summary" : "2 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 12,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.2.0.0/16",
      "occi.network.gateway" : "10.2.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network2"
  }, {
    "id" : "urn:uuid:49680b9e-0994-4b8c-ba95-6a35c5bff59c",
    "title" : "webserver",
    "summary" : "server webpage for business site",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins" : [ ],
    "attributes" : {
      "occi.compute.architecture" : "x64",
      "occi.compute.cores" : 8,
      "occi.compute.share" : 0,
      "occi.compute.speed" : 3.0,
      "occi.compute.memory" : 4.0,
      "occi.compute.state" : "active"
    },
    "links" : [ {
      "id" : "urn:uuid:47c44239-13ee-47e3-8746-4083e62a00bf",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
      "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
      "attributes" : {
        "occi.networkinterface.interface" : "eth0",
        "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
        "occi.networkinterface.address" : "192.168.0.100",
        "occi.networkinterface.gateway" : "192.168.0.1",
        "occi.networkinterface.allocation" : "dynamic"
      },
      "actions" : [ ],
      "location" : "/eth0/webserver",
      "source" : {
        "location" : "/mycompute/webserver",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
      },
      "target" : {
        "location" : "/mainnetwork/network2",
        "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
      }
    } ],
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
    "location" : "/mycompute/webserver"
  }, {
    "id" : "urn:uuid:97cb2eba-4ca4-4f9b-8666-3340316669ca",
    "title" : "network3",
    "summary" : "My third network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.3.0.0/16",
      "occi.network.gateway" : "10.3.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network3"
  }, {
    "id" : "urn:uuid:a6af7443-a579-4249-b5f3-de2e68a10a24",
    "title" : "network5",
    "summary" : "5 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 9,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.5.0.0/16",
      "occi.network.gateway" : "10.5.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network5"
  } ],
  "links" : [ {
    "id" : "urn:uuid:47c44239-13ee-47e3-8746-4083e62a00bf",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
    "attributes" : {
      "occi.networkinterface.interface" : "eth0",
      "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
      "occi.networkinterface.address" : "192.168.0.100",
      "occi.networkinterface.gateway" : "192.168.0.1",
      "occi.networkinterface.allocation" : "dynamic"
    },
    "actions" : [ ],
    "location" : "/eth0/webserver",
    "source" : {
      "location" : "/mycompute/webserver",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
    },
    "target" : {
      "location" : "/mainnetwork/network2",
      "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
    }
  } ]
}
</code>
</pre>

### Retrieve all the networks defined
You can use a kind location to retrieve a list of resources.

If you need only entities location you can use text/uri-list in accept values.

```curl -v -X GET http://localhost:8080/network/ -H "accept: text/uri-list" ```

In result, you will have on header :

<pre>
<code>
X-OCCI-Location: /mainnetwork/network1
X-OCCI-Location: /mainnetwork/network4
X-OCCI-Location: /mainnetwork/network2
X-OCCI-Location: /mainnetwork/network3
X-OCCI-Location: /mainnetwork/network5
</code>
</pre>

### Retrieve network interface instances
To retrieve the network interface : 

```curl -v -X GET http://localhost:8080/networkinterface/ -H "accept: application/json" ```

Result :
<pre>
<code>
{
  "id" : "urn:uuid:47c44239-13ee-47e3-8746-4083e62a00bf",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#networkinterface",
  "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/networkinterface#ipnetworkinterface" ],
  "attributes" : {
    "occi.networkinterface.interface" : "eth0",
    "occi.networkinterface.mac" : "00:80:41:ae:fd:7e",
    "occi.networkinterface.address" : "192.168.0.100",
    "occi.networkinterface.gateway" : "192.168.0.1",
    "occi.networkinterface.allocation" : "dynamic"
  },
  "actions" : [ ],
  "location" : "/eth0/webserver",
  "source" : {
    "location" : "/mycompute/webserver",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute"
  },
  "target" : {
    "location" : "/mainnetwork/network2",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network"
  }
}
</code>
</pre>

The location /networkinterface/ is a category location, one entity was found, so rendering adapt it for one resource rendering and not collection rendering.

### And mixin ?
Yes, mixin location can be used as we already done with kind.

So we want to retrieve all resources associated to "ipnetwork" mixin :

```curl -v -X GET http://localhost:8080/ipnetwork/ -H "accept: application/json"```

Result:
<pre>
<code>
{
  "resources" : [ {
    "id" : "urn:uuid:3c76c03a-8c1e-4a5f-8ec5-8ccccd7932dc",
    "title" : "network1",
    "summary" : "first network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 1,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.1.0.0/16",
      "occi.network.gateway" : "10.1.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network1"
  }, {
    "id" : "urn:uuid:4a57153a-1e77-4a94-ae8c-1cccc6458e20",
    "title" : "network4",
    "summary" : "4 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.4.0.0/16",
      "occi.network.gateway" : "10.4.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network4"
  }, {
    "id" : "urn:uuid:06934c34-796a-4c01-a82c-d68755952445",
    "title" : "network2",
    "summary" : "2 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 12,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.2.0.0/16",
      "occi.network.gateway" : "10.2.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network2"
  }, {
    "id" : "urn:uuid:97cb2eba-4ca4-4f9b-8666-3340316669ca",
    "title" : "network3",
    "summary" : "My third network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 10,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.3.0.0/16",
      "occi.network.gateway" : "10.3.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network3"
  }, {
    "id" : "urn:uuid:a6af7443-a579-4249-b5f3-de2e68a10a24",
    "title" : "network5",
    "summary" : "5 network",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
    "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
    "attributes" : {
      "occi.network.vlan" : 9,
      "occi.network.label" : "private",
      "occi.network.state" : "inactive",
      "occi.network.address" : "10.5.0.0/16",
      "occi.network.gateway" : "10.5.255.254"
    },
    "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
    "location" : "/mainnetwork/network5"
  } ]
}
</code>
</pre>


### Filter collections to retrieve, and use paging and number of items per page to return
If you have a lot of resources, it may be interesting to filter the collection.

This example shows how to retrieve all computes with attribute occi.compute.state equals to active
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=compute&attribute=occi.compute.state&value=active' -H 'accept: application/json'
</code>
</pre>

This example shows how to retrieve all networks with total number of elements per page equals to 2 and current page = 1 and attribute occi.network.vlan equals to 10
<pre>
<code>
curl -v -X GET 'http://localhost:8080/?category=network&attribute=occi.network.vlan&value=10&page=1&number=2&operator=1' -H 'accept: application/json'
</code>
</pre>
The result must give two network resource with vlan = 10.

This example shows how to retrieve all networks with a mixin user defined mytag :
<pre>
<code>
curl -v -X GET 'http://localhost:8080/network/?category=mytag' -H 'accept: application/json'
</code>
</pre>
The result will be 404 not found has the mixin tag is not defined for now.


## Update resources attributes

You can define or redefine attributes with POST method.

### Use case :

* Redefine an attribute of our network "network2" created before : 
        occi.network.vlan --< 10
    now it will be :
        occi.network.vlan --< 50
<pre>
<code>
curl -v -X POST -d '{ 
   "kind": "http://schemas.ogf.org/occi/infrastructure#network",
   "attributes": {
       "occi.network.vlan": 50
   }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mainnetwork/network2
</code>
</pre>
Result :
<pre>
<code>
{
  "id" : "urn:uuid:06934c34-796a-4c01-a82c-d68755952445",
  "title" : "network2",
  "summary" : "2 network",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#network",
  "mixins" : [ "http://schemas.ogf.org/occi/infrastructure/network#ipnetwork" ],
  "attributes" : {
    <b>"occi.network.vlan" : 50,</b>
    "occi.network.label" : "private",
    "occi.network.state" : "inactive",
    "occi.network.address" : "10.2.0.0/16",
    "occi.network.gateway" : "10.2.255.254"
  },
  "actions" : [ "http://schemas.ogf.org/occi/infrastructure/network/action#up", "http://schemas.ogf.org/occi/infrastructure/network/action#down" ],
  "location" : "/mainnetwork/network2"
}
</code>
</pre>


* Redefine the title of our compute on location <b><i>/mycompute/webserver</i></b> :
<pre>
<code>
curl -v -X POST -d '{ 
      "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
      "title": "This is our compute"
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mycompute/webserver
</code>
</pre> 

Note: Id, title and summary must be set out of attributes property, the json parser will map :

Id ==> occi.core.id

title ==> occi.core.title

summary ==> occi.core.summary

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
curl -v -X POST -d '{
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
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/-/
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
curl -v -X POST -d '{
    "location": "/mymixins/my_mixin3/",
    "scheme": "http://occiware.org/occi/tags#",
    "term": "my_mixin3",
    "attributes": {},
    "title": "my mixin tag 3"
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/-/
</code>
</pre>

## Associate a mixin tag to an entity
The mixin tag must be defined before associating it with an entity.

You have created before a compute resource, named "compute4".

You may tag it with my_mixin_first :

<pre>
<code>
curl -v -X POST http://localhost:8080/mycompute/webserver -d '
{
    "id" : "d99486b7-0632-482d-a184-a9195733ddd3",
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
                "http://occiware.org/occi/tags#my_mixin_first"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>

### Associate a mixin tag to multiple entities

To associate a collection of entities with a mixin tag, you must use the mixin tag location :
<pre>
<code>curl -v -X POST http://localhost:8080/mymixins/my_mixin3/ -d '{
  "locations":[
    "/mycompute/webserver",
    "/mainnetwork/network1",
    "/mainnetwork/network5"
  ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>


## Get a resource via a mixin category

You can find your entity via your mixin tag, this is useful if you have a lot of resources.

<pre>
<code>
curl -v -X GET http://localhost:8080/my_mixin_first/ -H 'accept: application/json'
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
For example, we associate the mixin ssh_key to the "webserver" resource : 
<pre>
<code>
curl -v -X POST http://localhost:8080/mycompute/webserver -d '
{
    "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
    "mixins": [
       "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key"
    ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>

You can also set a ssh key like (dont forget mixins property) :
<pre>
<code>
curl -v -X POST -d '
{ 
  "kind": "http://schemas.ogf.org/occi/infrastructure#compute",
  "mixins": [
    "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key"
  ],
  "attributes": {
    "occi.credentials.ssh.publickey":"My ssh key to define"
  }      
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mycompute/webserver
</code>
</pre>
Note that mixins property will not override the mixins associations made earlier.

In result:
<pre>
<code>
{
  "id" : "urn:uuid:92297781-db80-467e-8520-c356fdebb05e",
  "title" : "webserver",
  "summary" : "server webpage for business site",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
  "mixins" : [ "http://occiware.org/occi/tags#my_mixin_first", "http://occiware.org/occi/tags#my_mixin3", "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key" ],
  "attributes" : {
    "occi.compute.architecture" : "x64",
    "occi.compute.cores" : 8,
    "occi.compute.speed" : 3.0,
    "occi.compute.memory" : 4.0,
    "occi.compute.state" : "active",
    "occi.compute.share" : 0,
    "occi.credentials.ssh.publickey" : "My ssh key to define"
  },
  "actions" : [ "http://schemas.ogf.org/occi/infrastructure/compute/action#start", "http://schemas.ogf.org/occi/infrastructure/compute/action#stop", "http://schemas.ogf.org/occi/infrastructure/compute/action#restart", "http://schemas.ogf.org/occi/infrastructure/compute/action#suspend", "http://schemas.ogf.org/occi/infrastructure/compute/action#save" ],
  "location" : "/mycompute/webserver"
}
</code>
</pre>

## Dissociate a mixin from an entity (include mixin tag)
To dissociate a mixin from an entity, you must use PUT method.

There is two way to remove a mixin association :
- With full resource rendering, delete mixin association.

Remove mixin association between "webserver" compute and my_mixin_first
<pre>
<code>curl -v -X PUT http://localhost:8080/mycompute/webserver -d '
{
  "title" : "webserver",
  "summary" : "server webpage for business site",
  "kind" : "http://schemas.ogf.org/occi/infrastructure#compute",
  "mixins" : [ "http://occiware.org/occi/tags#my_mixin3", "http://schemas.ogf.org/occi/infrastructure/credentials#ssh_key" ],
  "attributes" : {
    "occi.compute.architecture" : "x64",
    "occi.compute.cores" : 8,
    "occi.compute.speed" : 3.0,
    "occi.compute.memory" : 4.0,
    "occi.compute.state" : "active",
    "occi.compute.share" : 0,
    "occi.credentials.ssh.publickey" : "My ssh key to define"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>

- With mixin location and full collection replacement.
<pre>
<code>curl -v -X PUT http://localhost:8080/mymixins/my_mixin_first/ -d '{
  "locations":[
    "/mainnetwork/network4",
    "/mainnetwork/network5"
  ]
}' -H 'Content-Type: application/json' -H 'accept: application/json'
</code>
</pre>


## Delete a mixin tag

In this example we remove <b>my_mixin_first</b> from configuration model : 
<pre>
<code>curl -v -X DELETE http://localhost:8080/-/ -d '{
  "location": "/mymixins/my_mixin_first/",
  "scheme": "http://occiware.org/occi/tags#",
  "term": "my_mixin_first"
}' -H 'Content-Type: application/json' -H 'accept: application/json' 
</code>
</pre>
This will first remove the mixin association and delete definitively the mixin.


## Trigger action on resources

### Execute an action on single resource instance with entity location
This example illustrate a stop instance.
<pre>
<code>
curl -v -X POST -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mycompute/webserver?action=stop
</code>
</pre>


### Execute actions on a category collection

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

### Execute an action on a mixin tag collection
<pre>
<code>
curl -v -X POST -d '{
  "action": "http://schemas.ogf.org/occi/infrastructure/compute/action#stop",
  "attributes": {
    "method": "graceful"
  }
}' -H 'Content-Type: application/json' -H 'accept: application/json' http://localhost:8080/mymixins/my_mixin3/?action=stop
</code>
</pre>
Warning, mixin tags may have other resources kind than compute.


## Delete Resources

### Delete single resource using entity location

<pre>
<code>
curl -v -X DELETE -H 'accept: application/json' http://localhost:8080/mainnetwork/network4
</code>
</pre>


### Delete all entities of a collection

This example illustrate a delete query on all computes.
<pre>
<code>
curl -v -X DELETE -H 'accept: application/json' http://localhost:8080/compute/
</code>
</pre>

This works on the same manner for mixin tag :

<pre>
<code>
curl -v -X DELETE -H 'accept: application/json' http://localhost:8080/mymixins/my_mixin3/
</code>
</pre>