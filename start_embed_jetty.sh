#!/usr/bin/env bash
cd org.occiware.mart.war/
## With occinterface frontend. to use the front, go to http://localhost:8080/occinterface/index.html
mvn jetty:run-war -Pwithoccinterface

## Without occinterface frontend.
## mvn jetty:run-war
