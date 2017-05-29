# Use an official openjdk base image
FROM openjdk:8

# Set the working directory to /app
WORKDIR /app

# Copy the appropriate current directory contents into the container at /app
COPY target/org.occiware.mart.jetty-1.0-SNAPSHOT-jar-with-dependencies.jar /app/org.occiware.mart.jetty-1.0-SNAPSHOT-jar-with-dependencies.jar
COPY martserver.config /app/martserver.config

# Make ports 8081 (http) and 8181 (https) available to the world outside this container
EXPOSE 8081
EXPOSE 8181

# Run app when the container launches
CMD ["java", "-jar", "org.occiware.mart.jetty-1.0-SNAPSHOT-jar-with-dependencies.jar", "martserver.config"]

