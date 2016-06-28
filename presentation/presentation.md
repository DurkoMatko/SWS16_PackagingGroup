# Service Packaging

### Goals

  * Dataset on touristic offers made by hotels in triple format
  * Make it queryable for other services or applications
    * filter by location
    * list multiple results
    * detail view on specific offer/hotel
  * Integrate data from other sources into our results
    * use **dbpedia** for an abstract on the location
    * list nearby tourist attractions

## Technical Solution

### Database

 * Apache Jena Fuseki

 * loads different RDF sources
    * touristic data
    * mock data
    * dbpedia fetched data

 * adds reasoning
    * schema:org RDFS subclasses
    * manual rules

 * sparql scripts on startup for more complex stuff


### Service

  * Jersey
    * RESTlike endpoints
        * [offers](http://sws.tr1k.de/offers)
        * [businesses](http://sws.tr1k.de/businesses)
        * [attractions](http://sws.tr1k.de/attractions)
        * [events](http://sws.tr1k.de/events)
    * parameters used to influence query
  * Jena
    * connection to fuseki
    * JSON-LD generation

### Weather Integration

  * fetch JSON from openweathermap.org
  * put into ontology
    * http://purl.org/ns/meteo
  * integrate in business endpoint

### Future Work

  * integration of city descriptions from dbpedia
    * difficulties on matching the resources
    * ambiguous names

  * setting timeouts for fuseki
    * FE guys found a way to crash the server
    * ?limit=10000 leeds to timeout in webserver
    * fuseki will work forever

### Further Information

#### Git

* http://git.uibk.ac.at/csas7564/sws16

* contains dataset thus request needed

* patrik.keller@student.uibk.ac.at

#### Thank you!
