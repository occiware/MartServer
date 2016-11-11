#!/bin/sh

# Check if there are arguments...
if [ $# = 0 ]
then
 echo No dependencies !
 echo "$0 you must set arguments like : org.occiware.clouddesigner.occi.infrastructure otherargs etc."
 echo
 echo
 echo "Usage: ./installer.sh <dependencydirectory1> <dependencydirN> "
 echo
 exit 1
fi

# Shallow clone ecore.
git clone --depth 1 https://github.com/occiware/ecore.git

# Build only the dependencies given in arguments with maven (mvn clean install)

 # first the core.
echo "Building the Clouddesigner core module."
cd ./ecore/clouddesigner/org.occiware.clouddesigner.occi/
mvn clean install
cd ../

# the following ..
for depend in $@
do
 echo "Building the Clouddesigner $depend module"
 cd $depend
 mvn clean install
 cd ..
done

# Removing the ecore project.
cd ../../
# rm -R ecore

# Installing MartServer
git clone https://github.com/cgourdin/MartServer.git

# Update the pom.xml file.
cd MartServer
mv clouddesigner-deps-0.1.0.pom clouddesigner-deps-0.1.0.old.pom

# Add the lines to dependencies pom
# clouddesigner-deps-0.1.0.pom
header='<?xml version="1.0" encoding="UTF-8"?><project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd"> 

<modelVersion>4.0.0</modelVersion>

<groupId>Clouddesigner-deps</groupId>
<artifactId>clouddesigner-deps</artifactId>
<version>0.1.0</version>
<packaging>pom</packaging>

<dependencies>
'
footer='</dependencies>
</project>'

dependencies=''
for val in $@
do
dependencies+='
<dependency>
<groupId>Clouddesigner</groupId>
<artifactId>'$val'</artifactId>
<version>0.1.0-SNAPSHOT</version>
</dependency>
'
done

echo $dependencies

cat <<EOF > clouddesigner-deps-0.1.0.pom
$header
$dependencies
$footer
EOF


# build MartServer
mvn install:install-file -Dpackaging=pom -DpomFile=clouddesigner-deps-0.1.0.pom -DgroupId=Clouddesigner-deps -DartifactId=clouddesigner-deps -Dversion=0.1.0 -Dfile=clouddesigner-deps-0.1.0.pom
mvn initialize
mvn clean install
