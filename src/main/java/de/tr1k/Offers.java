package de.tr1k;

import de.tr1k.Helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.jena.atlas.lib.StrUtils;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.rdf.model.InfModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.reasoner.Reasoner;
import org.apache.jena.reasoner.rulesys.GenericRuleReasoner;
import org.apache.jena.reasoner.rulesys.Rule;
import org.apache.jena.riot.WebContent;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.tdb.TDBFactory;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.util.FileManager;

@Path("offers")
public class Offers{
	private String urlprefix = "http://localhost:8080/sws16/ServicePackaging/api";
	private String dbUri = "http://localhost:3030/ds";

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response offerGeneral(@QueryParam("lon") String lon,
							     @QueryParam("lat") String lat,
							     @QueryParam("radius") String radius,
							     @QueryParam("city") String city,
							     @QueryParam("maxprice") String maxprice,
							     @QueryParam("minprice") String minprice,
							     @QueryParam("order") String order,
							     @QueryParam("reverse") String reverse,
								 @QueryParam("offset") String offset,
								 @QueryParam("limit") String limit){

    boolean geoGiven = lat!=null && lon!=null;
    String qBindDistance = "";
    if (geoGiven) qBindDistance = "BIND((?longitude - "+ lon +")*(?longitude-"+lon+")+(?latitude - "+ lat +")*(?latitude - "+ lat +") AS ?distance)\n";

    boolean radiusGiven = radius!=null;
    double latLongDelta[] = null;
    if (geoGiven && radiusGiven) {
      latLongDelta=Helpers.radiusToLonLat(radius, lat);
    }

    // Filtering
    String qFiltering = "";
    if(geoGiven && radiusGiven) {
      qFiltering = qFiltering
        + "FILTER (?longitude - " + lon + "<" + String.valueOf(latLongDelta[1])
        + " && ?longitude -" + lon + " >- " + String.valueOf(latLongDelta[1])
        + " && ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])
        + " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ").\n";
    }
    if(maxprice!=null){
      qFiltering += "FILTER (?price < "+ maxprice+ ").\n";
    }
    if(minprice!=null){
      qFiltering += "FILTER (?price > "+ minprice+ ").\n";
    }

    // Ordering
    String qOrderBy = "ORDER BY ";
    if(reverse!=null && order.equals("true"))
      qOrderBy += "DESC";
    else
      qOrderBy += "ASC";
    if(order!=null){
      if(order.equals("price")) qOrderBy += "(?price)";
    }
    else if (geoGiven) {
      qOrderBy += "(?distance)";
    }
    else {
      qOrderBy += "(?serial)";
    }
    qOrderBy += "\n";

    // Pagination
    String qPagination = "";
    if(offset!=null) qPagination = "OFFSET "+ offset + " ";
    if(limit!=null) qPagination += "LIMIT " + limit;
    else qPagination += "LIMIT 10";

    // Build query
    String query = ""
      + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
      + "PREFIX schema: <http://schema.org/>\n"
      + "	CONSTRUCT {  \n"
      + "	  ?offerlnk rdf:type schema:Offer;    \n"
      + "	    schema:offeredBy ?hotel;\n"
      + "	    schema:teaser ?teaser;\n"
      + "	    schema:image ?img;\n"
      + "	    schema:priceSpecification ?priceSpec.\n"
      + "	  ?img schema:contentUrl ?imgUrl.\n"
      + "	  ?priceSpec schema:priceCurrency ?currency;\n"
      + "	    schema:price ?price.\n"
      + "	} WHERE { \n"
      + "	  ?offer rdf:type schema:Offer; \n"
      + "	    schema:serialNumber ?serial;	\n"
      + "	    schema:offeredBy ?hotel.\n"
      + "   ?hotel schema:geo ?geo.\n"
      + "   ?geo schema:latitude ?latitude;\n"
      + "        schema:longitude ?longitude. \n"
      + qFiltering
      + "	OPTIONAL {   \n"
      + "	    ?offer schema:priceSpecification ?priceSpec. \n"
      + "	    ?priceSpec schema:priceCurrency ?currency.\n"
      + "	    ?priceSpec schema:price ?price.\n"
      + "	}  "
      + "	OPTIONAL {   \n "
      + "	    ?offer schema:image ?img.\n"
      + "	    ?img schema:contentUrl ?imgUrl.\n"
      + "	} \n"
      + "	OPTIONAL{ \n"
      + "	    ?offer schema:teaser ?teaser.\n"
      + "	}\n"
      + "	BIND(IRI(CONCAT(\"http://localhost:8080/sws16/ServicePackaging/api/offers/\",?serial)) AS ?offerlnk)\n"
      + qBindDistance
      + "}"
      + qOrderBy
      + qPagination ;

//		//	}
//
//		//api/offers?city<>&maxprice=<>
//	//	else if(city!=null){
//			query = ""
//					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//					+ "	PREFIX schema: <http://schema.org/>\n"
//					+ "	CONSTRUCT {  \n"
//					+ "	  ?offerlnk rdf:type schema:Offer;    \n"
//					+ "	    schema:offeredBy ?hotel;\n"
//					+ "	    schema:teaser ?teaser;\n"
//					+ "	    schema:image ?img;\n"
//					+ "	    schema:priceSpecification ?priceSpec.\n"
//					+ "	  ?img schema:contentUrl ?imgUrl.\n"
//					+ "	  ?priceSpec schema:priceCurrency ?currency;\n"
//					+ "	    schema:price ?price.\n"
//					+ "	}WHERE { \n"
//					+ "	  ?offer rdf:type schema:Offer; \n"
//					+ "	    schema:serialNumber ?serial;	\n"
//					+ "	    schema:offeredBy ?hotel.\n"
//					+ "   ?hotel schema:address ?address."
//					+ "   ?address schema:addressLocality ?locality."
//					+ "  FILTER (REGEX (?locality,\""+city+"\"))";
//					if(maxprice!=null){
//						query = query + "FILTER (?price < "+ maxprice+ ")\n";
//					}
//					query = query + "	OPTIONAL {   \n"
//					+ "	    ?offer schema:priceSpecification ?priceSpec. \n"
//					+ "	    ?priceSpec schema:priceCurrency ?currency.\n"
//					+ "	    ?priceSpec schema:price ?price.\n"
//					+ "	}  "
//					+ "	OPTIONAL {   \n "
//					+ "	    ?offer schema:image ?img.\n"
//					+ "	    ?img schema:contentUrl ?imgUrl.\n"
//					+ "	} \n"
//					+ "	OPTIONAL{ \n"
//					+ "	    ?offer schema:teaser ?teaser.\n"
//					+ "	}\n"
//					+ "	BIND(IRI(CONCAT(\"http://localhost:8080/sws16/ServicePackaging/api/offers/\",?serial)) AS ?offerlnk)\n"
//					+ "}";
//
//					if(order!=null && order.equals("price")){
//						if(reverse!=null && reverse.equals("true"))
//							query=query+"ORDER BY DESC(?price)";
//						else{
//							query=query+"ORDER BY ASC(?price)";
//						}
//					}
//					else if(order!=null && order.equals("distance")){
//						return Response.status(422).entity("The request was well-formed but was unable to be followed due to semantic errors - Unexpected parameters combination").build();
//					}
//					//pagination
//					if(offset!=null && limit !=null){
//						query = query + "OFFSET "+offset+" LIMIT"+limit;
//					}
//					else if(offset==null && limit !=null){
//						query = query + " LIMIT " + limit;
//					}
//					else{
//						query = query + " LIMIT 10";
//					}
//		}
//
////		else if(lat!=null || lon!=null || radius!=null || maxprice!=null || order!=null || city !=null || reverse != null){
//			return Response.status(422).entity("The request was well-formed but was unable to be followed due to semantic errors - Unexpected parameters combination").build();
//		}
//		// api/offers
////		else {
//			query = ""
//					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
//					+ "	PREFIX schema: <http://schema.org/>\n"
//					+ "	CONSTRUCT {  \n"
//					+ "	  ?offerlnk rdf:type schema:Offer;    \n"
//					+ "	    schema:offeredBy ?hotel;\n"
//					+ "	    schema:teaser ?teaser;\n"
//					+ "	    schema:image ?img;\n"
//					+ "	    schema:priceSpecification ?priceSpec.\n"
//					+ "	  ?img schema:contentUrl ?imgUrl.\n"
//					+ "	  ?priceSpec schema:priceCurrency ?currency;\n"
//					+ "	    schema:price ?price.\n"
//					+ "	}WHERE { \n"
//					+ "	  ?offer rdf:type schema:Offer; \n"
//					+ "	    schema:serialNumber ?serial;	\n"
//					+ "	    schema:offeredBy ?hotel.\n"
//					+ "   ?hotel schema:geo ?geo.\n"
//					+ "   ?geo schema:latitude ?latitude;\n"
//					+ "        schema:longitude ?longitude. \n"
//					+ "	OPTIONAL {   \n"
//					+ "	    ?offer schema:priceSpecification ?priceSpec. \n"
//					+ "	    ?priceSpec schema:priceCurrency ?currency.\n"
//					+ "	    ?priceSpec schema:price ?price.\n"
//					+ "	}  "
//					+ "	OPTIONAL {   \n "
//					+ "	    ?offer schema:image ?img.\n"
//					+ "	    ?img schema:contentUrl ?imgUrl.\n"
//					+ "	} \n"
//					+ "	OPTIONAL{ \n"
//					+ "	    ?offer schema:teaser ?teaser.\n"
//					+ "	}\n"
//					+ "	BIND(IRI(CONCAT(\"http://localhost:8080/sws16/ServicePackaging/api/offers/\",?serial)) AS ?offerlnk)\n"
//					+ "}";
//			//pagination
//			if(offset!=null && limit !=null){
//				query = query + "OFFSET "+offset+" LIMIT"+limit;
//			}
//			else if(offset==null && limit !=null){
//				query = query + " LIMIT " + limit;
//			}
//			else{
//				query = query + " LIMIT 10";
//			}
//		}

		Model results = QueryExecutionFactory.sparqlService(dbUri, query).execDescribe();
		ByteArrayOutputStream outputStream =Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("offers/{serial}")
	public Response offer(@PathParam("serial") String serial){
		Model results = QueryExecutionFactory.sparqlService(dbUri, ""
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
				+ "PREFIX schema: <http://schema.org/>"
				+ ""
				+ "CONSTRUCT {"
				+ "  ?offerlnk ?p ?o."
				+ "    ?img ?imgp ?imgo."
				+ "    ?price ?pricep ?priceo."
				+ "}"
				+ "WHERE {"
				+ "  ?offer rdf:type schema:Offer;"
				+ "       schema:serialNumber \"" + serial + "\";"
				+ "  ?p ?o;"
				+ "  OPTIONAL {"
				+ "    ?offer schema:priceSpecification ?price."
				+ "      ?price ?pricep ?priceo."
				+ "  }"
				+ "  OPTIONAL {"
				+ "    ?offer schema:image ?img."
				+ "      ?img ?imgp ?imgo."
				+ "  }"
				+ "  BIND(IRI(\"" + urlprefix + "/" + serial + "\") AS ?offerlnk)"
				+ "}"
				).execDescribe();
		ByteArrayOutputStream outputStream =Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
	}
}
