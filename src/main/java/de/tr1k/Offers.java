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
	private String urlprefix = "http://localhost:8080/offers/";
	private String dbUri = "http://localhost:3030/ds";

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response offerList(
      @QueryParam("lon") String lon,
      @QueryParam("lat") String lat,
      @QueryParam("radius") String radius,
      @QueryParam("city") String city,
      @QueryParam("maxprice") String maxprice,
      @QueryParam("minprice") String minprice,
      @QueryParam("type") String type,
      @QueryParam("order") String order,
      @QueryParam("reverse") String reverse,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit
      ){

    // Location ?
    boolean geoGiven = lat!=null && lon!=null;
    String qBindDistance = "";
    if (geoGiven) {
      qBindDistance = qBindDistance
        + "?s schema:offeredBy/schema:geo/schema:longitude ?slon.\n"
        + "?s schema:offeredBy/schema:geo/schema:latitude ?slat.\n"
        + "BIND(" + lat + " AS ?lat).\n"
        + "BIND(" + lon + " AS ?lon).\n"
        + "BIND((?lon-?slon) AS ?a).\n"
        + "BIND((?lat-?slat) AS ?b).\n"
        + "BIND(?a*?a + ?b*?b AS ?distance).\n"
        ;
    }

    // Filtering
    String qFiltering = "";
    if(type!=null) qFiltering += "?s rdf:type schema:" + type + ".\n";

    if(geoGiven && radius!=null) {
      double dRadius = Double.parseDouble(radius);
      double dRSquared = dRadius * dRadius;
      qFiltering += "FILTER (?distance < " + String.valueOf(dRSquared) +").\n";
    }

    if(maxprice!=null || minprice!=null)
      qFiltering += "?s schema:priceSpecification/schema:price ?price.\n";
    if(maxprice!=null)
      qFiltering += "FILTER (?price < "+ maxprice+ ").\n";
    if(minprice!=null)
      qFiltering += "FILTER (?price > "+ minprice+ ").\n";

    if(city!=null) {
  		qFiltering += "?s schema:offeredBy/schema:address/schema:addressLocality ?locality.\n";
      qFiltering += "FILTER (REGEX (?locality,\""+city+"\"))\n";
    }

    // Ordering
    String qOrderBy = "ORDER BY ";
    if(reverse!=null && order.equals("true"))
      qOrderBy += "DESC";
    else
      qOrderBy += "ASC";
    if(order!=null){
      if(order.equals("serialNumber")) qOrderBy += "(?serialNumber)";
      if(order.equals("distance") && geoGiven) qOrderBy += "(?distance)";
    }
    else if (geoGiven) {
      qOrderBy += "(?distance)";
    }
    else {
      qOrderBy += "(?serialNumber)";
    }
    qOrderBy += "\n";

    // Pagination
    String qPagination = "";
    if(offset!=null) qPagination += "OFFSET "+ offset + " ";
    if(limit!=null) qPagination += "LIMIT " + limit;
    else qPagination += "LIMIT 5";

    // Build query
    String select = ""
      + "    SELECT ?s WHERE {\n"
      + "      ?s rdf:type schema:Offer.\n"
      + "      ?s schema:serialNumber ?serialNumber.\n"
      +        qFiltering
      +        qBindDistance
      + "    }\n"
      +      qOrderBy
      +      qPagination
      ;

    String query = Helpers.queryFromInnerSelect(select);

    Model results = QueryExecutionFactory.sparqlService(dbUri,query).execConstruct();
    ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
    return Response.status(200).entity(outputStream.toString()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("{serialNumber}")
	public Response offerDetail(
      @PathParam("serialNumber") String serialNumber
      ){

    // Build query
    String select = ""
      + "    SELECT ?s WHERE {\n"
      + "      ?s rdf:type schema:Offer.\n"
      + "      ?s schema:serialNumber \"" + serialNumber + "\".\n"
      + "    }\n"
      ;

    String query = Helpers.queryFromInnerSelect(select);

    Model results = QueryExecutionFactory.sparqlService(dbUri,query).execConstruct();
    ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
    return Response.status(200).entity(outputStream.toString()).build();
	}
}
