#### Prefixes
```
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX redlink: <http://data.redlink.io/>
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
PREFIX foaf: <http://xmlns.com/foaf/0.1/>
PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX owl: <http://www.w3.org/2002/07/owl#>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX skos: <http://www.w3.org/2004/02/skos/core#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX context: <http://data.redlink.io/context/>
PREFIX schema: <http://schema.org/>
PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>
PREFIX sesame: <http://www.openrdf.org/schema/sesame#>
PREFIX fn: <http://www.w3.org/2005/xpath-functions#>
PREFIX gn: <http://www.geonames.org/ontology#>
PREFIX lode: <http://linkedevents.org/ontology/>
PREFIX cal: <http://www.w3.org/2002/12/cal#>
PREFIX vcard: <http://www.w3.org/2006/vcard/ns#>
PREFIX xs: <http://www.w3.org/2001/XMLSchema>
PREFIX acco: <http://purl.org/acco/ns#>
PREFIX time: <http://www.w3.org/2006/time#>
PREFIX gr: <http://purl.org/goodrelations/v1#>
PREFIX xml: <http://www.w3org/XML/1998/namespace>
```

#### List of Hotels plus name
```
SELECT ?subject ?name
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
    schema:name ?name.
}
```

#### List of addresses of Businesses
```
SELECT ?address ?street ?plz ?countryname ?countryiso ?locality ?region
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
   	schema:address ?address.
  ?address schema:streetAddress ?street;
    schema:postalCode ?plz;
    schema:addressLocality ?locality;
    schema:addressRegion ?region;
    schema:addressCountry ?country;
    rdf:type schema:PostalAddress.
  ?country schema:iso ?countryiso;
    schema:name ?countryname.
}
```

#### Details for one Hotel
```
SELECT ?subject ?name ?url ?longitude ?latitude
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
   	schema:geo ?geo;
    schema:name ?name;
    schema:url ?url;
    schema:address ?address.
  ?geo schema:longitude ?longitude;
    schema:latitude ?latitude.
  FILTER regex(str(?subject),"AT_FLA_TAUERNHOF").
}
```

#### List of telephone number of one hotel
```
SELECT ?subject ?telephone
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
    schema:telephone ?telephone.
  FILTER regex(str(?subject),"AT_FLA_TAUERNHOF").
}
```

### List of Hotels in Italy
```
SELECT DISTINCT ?subject ?name
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
    schema:name ?name;
    schema:address ?address.
  ?address schema:addressCountry ?country.
  ?country schema:iso ?iso.
  FILTER (?iso = "IT")
}
```

### List of Offers made by one Hotel in English
```
SELECT ?subject ?offer (STR(?name) as ?nameen) (STR(?desc) as ?descen)
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
    schema:makesOffer ?offer.
  ?offer schema:name ?name;
	schema:description ?desc.
  FILTER regex(str(?subject),"AT_FLA_TAUERNHOF").
  FILTER(langMatches(lang(?name), "EN")).
  FILTER(langMatches(lang(?desc), "EN")).
}
```

### List of pictures ordered by offers filtered by hotel with english caption
```
SELECT ?subject ?offer (str(?caption ) as ?en_caption) ?url
WHERE {
  ?subject rdf:type schema:LodgingBusiness;
    schema:makesOffer ?offer.
  ?offer schema:image ?image.
  ?image schema:contentUrl ?url;
    schema:caption ?caption.
  FILTER regex(str(?subject),"AT_FLA_TAUERNHOF").
  FILTER(langMatches(lang(?caption), "EN")).
}
```

### Include remote sparql endpoint
```
SELECT ?contact2 WHERE {
  SERVICE <http://www.myopenlink.com/sparql> {
     SELECT ?contact2
     WHERE {
       ?me foaf:name ?contact2
     }
     LIMIT 100
  }
 }
```

### Innsbrucks abstract in dbpedia
```
SELECT ?mname ?abstract
WHERE {
  SERVICE <http://dbpedia.org/sparql> {
  	?match rdf:type schema:Settlement;
    	dbp:name ?mname;
        dbo:abstract ?abstract.
    FILTER(str(?mname) = "Innsbruck")
  }
}
```

