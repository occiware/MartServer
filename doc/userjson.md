# User documentation json parser.
This give some examples of usage with json input and output.

All theses examples use curl to execute queries on the server.

Important: You can define Content-type to text/occi and accept-type to application/json.

Also, you can define Content-Type to application/json and accept-type to text/occi.

application/json and application/occi+json will give the same input/output content.


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

```
curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:9090/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"
```

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
        "occi.compute.state": "active"
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

```
curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:9090/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"
```

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
                "occi.compute.state": "active"
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

```
curl -v -X PUT --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:9090/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"
```

## Update resources attributes

You can define attributes with the same query as you created the resources but with a POST method.

```
curl -v -X POST --data-binary "@/yourabsolutepath/resourcefile.json" http://localhost:9090/ -H "Content-Type: application/occi+json" -H "accept: application/occi+json"
```

## Retrieve your resources

Please notice that the relative path of your resources to find is important.

You can search directly with the resource location path (including uuid) :

```curl -v -X GET http://localhost:9090/vms/foo/a1cf3896-500e-48d8-a3f5-a8b3601bcdd8 -H "accept: application/json" ```
This must retrieve one resource for this uuid : a1cf3896-500e-48d8-a3f5-a8b3601bcdd8.


### Retrieve your resources with filter path

- You have defined your resources on a custom path : http://localhost:9090/myresources/ so to retrieve your resources :

```curl -v -X GET http://localhost:9090/myresources/ -H "accept: application/occi+json"```

- You can't remember where you have defined your resources but you known the category :

```curl -v -X GET http://localhost:9090/mycategory/ -H "accept: application/occi+json"```

This will give you all the resources for the mycategory. 

To have location only :

```curl -v -X GET http://localhost:9090/mycategory/ -H "accept: text/uri-list" ```


### Retrieve your resources with filter parameters





## Define mixin tags



## execute an action


## execute a sequence of actions


## Delete entity (ies)



