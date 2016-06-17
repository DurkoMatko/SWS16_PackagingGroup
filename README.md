SWS16 - Service Packaging
=========================

This project is part of the Proseminar in Semantic Web Services at the 
University of Innsbruck in the Summer Semester 2016. Our task is to 
package data from different sources into one service and serve it in a 
semantic way.

Instructions
------------

This application consists of two parts - a triple store built with 
Apache Jena Fuseki and a Http request handler using Jena and Jersey. The 
latter connects to the first to deliver data from the triple store as 
json-ld selected by parameters.

### Triple Store

Since the triple store is build on Fuseki, you first have to get a copy 
of it. I suggest doing something like the following.

```sh
mkdir ~/opt
cd ~/opt
wget \
https://archive.apache.org/dist/jena/binaries/apache-jena-fuseki-2.3.1.zip
unzip apache-jena-fuseki-2.3.1.zip
rm apache-jena-fuseki-2.3.1.zip
```

Our startup script needs to know, where it can find fuseki. Adapt the 
top lines of **startup-fuseki.sh** to fit your environment.

If everything went right, you should be able to start a data-filled 
fuseki with

```sh
./start-fuseki.sh
```

from within the repository's base folder. The script waits some time for 
the server starting up and finishing it's internal reasoning on the 
provided dataset and then executes the scripts found in *sparql/init*.  
This will take a serious amount of time.

### Request handler

Assuming, that you you have Apache maven installed, a simple

```sh
make run
```

should suffice to build an bring up the request handler. It will listen 
on port 8080 by default. A good point to start is [this overview][wadl] 
of endpoints.

[wadl]:http://localhost:8080/application.wadl
