## How to build and run it locally

If you want to build a jar with all the dependencies in order to dockerize do :

```bash
mvn clean compile assembly:single
```

To launch the jar, go to the target directory and execute it with java (with the config as first parameter) :

```bash
cd target/
java -jar org.occiware.mart.jetty-1.0-SNAPSHOT-jar-with-dependencies.jar ../martserver.config
```

To check if everything is working properly, execute the following request :

```bash
curl -v -X GET http://localhost:8081/.well-known/org/ogf/occi/-/ -H "accept: application/json"
```

## Creating a docker image

If you have Docker installed (if not, you can find how to do so [here](https://docs.docker.com/engine/installation/)), you may want to create a docker container.

Supposing you are currently in this README.md's directory, that is to say, "org.occiware.mart.jetty", and that you have followed all the previous steps so that you built a jar with all the dependencies, do :

``` bash
sudo docker build -t mart-server .
sudo docker run -p 8081:8081 mart-server
```
