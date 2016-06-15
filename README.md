SWS16 - Service Packaging
=========================

This project is part of the Proseminar in Semantic Web Services at the 
University of Innsbruck in the Summer Semester 2016. Our task is to 
package data from different sources into one service.

Instructions
------------

This application consists of two parts - a triple store built with 
Apache Jena Fuseki and Http request handler using Jena and 
Jersey.

### Triple Store

TODO: Describe the startup process of fuseki

### Request handler

A simple `mvn clean install exec:java` should suffice to build an bring 
up the server. It will listen on port 8080 by default. A good point to 
start is [this overview][wadl] of endpoints.

[wadl]:http://localhost:8080/application.wadl