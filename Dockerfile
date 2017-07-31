# Use an official openjdk base image
FROM ubuntu:latest

# Set the working directory to /app
WORKDIR /app

# Copy the application and its configuration files
COPY . /app

# Get all required packages and install the project
RUN apt-get update \
   && apt-get install -y git default-jre default-jdk maven \
   && rm -rf /var/lib/apt/lists/* \
   && cd /app \
   && mvn initialize \
   && mvn clean install -Pwithoccinterface

# Make ports 8080 (http) and 8180 (https) available to the world outside this container
EXPOSE 8080
EXPOSE 8180

# Activate remote debugging
ENV MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,address=9000,server=y,suspend=n"

# Start the MartServer
CMD cd org.occiware.mart.war && mvn jetty:run-war -Dorg.eclipse.jetty.annotations.maxWait=120