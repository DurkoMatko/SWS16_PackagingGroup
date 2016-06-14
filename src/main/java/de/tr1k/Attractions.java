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

@Path("attractions")
public class Attractions{
	private String urlprefix = "http://localhost:8080/sws16/ServicePackaging/api";
	private String dbUri = "http://localhost:3030/ds";

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response attraction(@QueryParam("lon") String lon,
      @QueryParam("lat") String lat,
      @QueryParam("radius") String radius,
      @QueryParam("offset") String offset,
      @QueryParam("limit") String limit){

    if(offset!=null && limit==null){
      return Response.status(422).entity("The request was well-formed but was unable to be followed due to semantic errors - Unexpected parameters combination").build();
    }

    String query="";
    if(lat!=null && lon!=null){
      double latLongDelta[]=null;
      if(radius!=null){
        latLongDelta= Helpers.radiusToLonLat(radius, lat);
      }
      query="PREFIX schema: <http://schema.org/>\n"
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
        + "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
        + "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
        + "CONSTRUCT{\n"
        + " ?subject rdf:type schema:TouristAttraction;\n"
        + "          geo:lat ?latitude;\n"
        + "			 geo:long ?longitude.\n}"
        + "WHERE {\n"
        + "  ?subject rdf:type schema:TouristAttraction;\n"
        + "         geo:lat ?latitude;\n"
        + "			geo:long ?longitude.\n";
      if(radius!=null){
        query = query + "FILTER (?longitude - "+ lon +"<" + String.valueOf(latLongDelta[1])+ " && ?longitude -"+lon+" >-" + String.valueOf(latLongDelta[1])+ " && ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])+ " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ")\n";
      }
      query=query+ "} ORDER BY ASC((?longitude - "+ lon +")*(?longitude-"+lon+")"
        + "  +  (?latitude - "+ lat +")*(?latitude - "+ lat +"))";
      //pagination
      if(offset!=null && limit !=null){
        query = query + "OFFSET "+offset+" LIMIT"+limit;
      }
      else if(offset==null && limit !=null){
        query = query + " LIMIT " + limit;
      }
      else{
        query = query + " LIMIT 10\n";
      }
    }
    else if(lat!=null || lon !=null || radius!=null){
      return Response.status(422).entity("The request was well-formed but was unable to be followed due to semantic errors - Unexpected parameters combination").build();
    }
    else{
      query="PREFIX schema: <http://schema.org/>\n"
        + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
        + "PREFIX dbo: <http://dbpedia.org/ontology/>\n"
        + "PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n"
        + "CONSTRUCT{\n"
        + " ?subject rdf:type schema:TouristAttraction;\n"
        + "          geo:lat ?latitude;\n"
        + "			 geo:long ?longitude.\n}"
        + "WHERE {\n"
        + "  ?subject rdf:type schema:TouristAttraction;\n"
        + "         geo:lat ?latitude;\n"
        + "			geo:long ?longitude.\n"
        + "}";
      //pagination
      if(offset!=null && limit !=null){
        query = query + "OFFSET "+offset+" LIMIT"+limit;
      }
      else if(offset==null && limit !=null){
        query = query + " LIMIT " + limit;
      }
      else{
        query = query + " LIMIT 10\n";
      }
    }

    Model results = QueryExecutionFactory.sparqlService(dbUri, query).execDescribe();
    ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
    return Response.status(200).entity(outputStream.toString()).build();
  }

}
