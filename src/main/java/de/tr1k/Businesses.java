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

@Path("businesses")
public class Businesses{
  private String urlprefix = "http://localhost:8080/";
  private String dbUri = "http://localhost:3030/ds";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response businessList(@QueryParam("lon") String lon,
      @QueryParam("lat") String lat,
      @QueryParam("radius") String radius,
      @QueryParam("city") String city,
      @QueryParam("order") String order,
      @QueryParam("type") String type,
      @QueryParam("reverse") String reverse,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit){

    // Location ?
    boolean geoGiven = lat!=null && lon!=null;
    String qBindDistance = "";
    if (geoGiven) qBindDistance =
      "BIND((?longitude - "+ lon +")*(?longitude-"+lon
        + ")+(?latitude - "+ lat +")*(?latitude - "+ lat +") AS ?distance)\n";

    // Filtering
    String qFiltering = "";
    if(type!=null) qFiltering += "?subject rdf:type schema:" + type + ".\n";

    boolean radiusGiven = radius!=null;
    double latLongDelta[] = null;
    if (geoGiven && radiusGiven) {
      latLongDelta=Helpers.radiusToLonLat(radius, lat);
    }
    if(geoGiven && radiusGiven) {
      qFiltering = qFiltering
        + "FILTER (?longitude - " + lon + "<" + String.valueOf(latLongDelta[1])
        + " && ?longitude -" + lon + " >- " + String.valueOf(latLongDelta[1])
        + " && ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])
        + " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ").\n";
    }

    // Ordering
    String qOrderBy = "ORDER BY ";
    if(reverse!=null && order.equals("true"))
      qOrderBy += "DESC";
    else
      qOrderBy += "ASC";
    if(order!=null){
      if(order.equals("branchCode")) qOrderBy += "(?branchCode)";
      if(order.equals("distance")) qOrderBy += "(?distance)";
    }
    else if (geoGiven) {
      qOrderBy += "(?distance)";
    }
    else {
      qOrderBy += "(?branchCode)";
    }
    qOrderBy += "\n";

    // Pagination
    String qPagination = "";
    if(offset!=null) qPagination = "OFFSET "+ offset + " ";
    if(limit!=null) qPagination += "LIMIT " + limit;
    else qPagination += "LIMIT 100";

    // Build query
    String query=""
      + "PREFIX schema: <http://schema.org/>\n"
      + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
      + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
      + "CONSTRUCT {"
      + "  ?subject rdf:type ?type;"
      + "    schema:makesOffer ?offerlnk;"
      + "    schema:name ?name;"
      + "}"
      + "WHERE {"
      + "  ?subject rdf:type schema:LocalBusiness, ?type;"
      + "    schema:name ?name;"
      + "    schema:branchCode ?branchCode;"
      + "	   schema:geo ?geo."
      + "	 ?geo schema:longitude ?longitude;"
      + "		schema:latitude ?latitude."
      + qFiltering
      + "  OPTIONAL {"
      + "    ?subject schema:makesOffer ?offer."
      + "    ?offer schema:serialNumber ?serialNumber."
      + "    BIND(IRI(CONCAT(\"" + urlprefix + "offers/\", ?serialNumber)) AS ?offerlnk)."
      + qBindDistance
      + "  }"
      + "}"
      + qOrderBy
      + qPagination;

    Model results = QueryExecutionFactory.sparqlService(dbUri,query).execConstruct();
    ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
    return Response.status(200).entity(outputStream.toString()).build();
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Path("{id}")
  public Response businessDetail(@PathParam("id") String id){
    Model results = QueryExecutionFactory.sparqlService(dbUri, ""
        + "PREFIX schema: <http://schema.org/>\n"
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
        + "DESCRIBE ?subject WHERE {\n"
        + "  ?subject rdf:type schema:LocalBusiness;\n"
        + "    schema:branchCode \"" + id + "\"."
        + " "
        + "}"
        ).execDescribe();
    ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
    return Response.status(200).entity(outputStream.toString()).build();
  }
}
