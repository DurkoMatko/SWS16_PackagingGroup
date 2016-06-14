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
	private String urlprefix = "http://localhost:8080/sws16/ServicePackaging/api";
	private String dbUri = "http://localhost:3030/ds";
	private String updateUri = "http://localhost:3030/ds/update";

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response hotels(@QueryParam("lon") String lon,
						   @QueryParam("lat") String lat,
						   @QueryParam("radius") String radius,
						   @QueryParam("city") String city,
						   @QueryParam("offset") String offset,
						   @QueryParam("limit") String limit){

		Model results = null;
		String query="";
		if(lon!=null && lat!=null){
			/* radius to lat/long */
			double latLongDelta[]=null;
			if(radius!=null){
				latLongDelta=Helpers.radiusToLonLat(radius, lat);
			}
			query = ""
					+ "PREFIX schema: <http://schema.org/>\n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "CONSTRUCT {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:makesOffer ?offerlnk;"
					+ "    schema:name ?name;"
					+ "}"
					+ "WHERE {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:name ?name;"
					+ "	   schema:geo ?geo."
					+ "	 ?geo schema:longitude ?longitude;"
					+ "		schema:latitude ?latitude;";
					if(radius!=null){
							query = query + "  FILTER (?longitude - "+ lon +"<" + String.valueOf(latLongDelta[1])+ " && ?longitude -"+lon+" >-" + String.valueOf(latLongDelta[1])+ "&& ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])+ " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ")";
					}
					query = query + "  OPTIONAL {"
					+ "    ?hotel schema:makesOffer ?offer."
					+ "    ?offer schema:serialNumber ?serialNumber."
					+ "    BIND(IRI(CONCAT(\"" + urlprefix + "/offers/\", ?serialNumber)) AS ?offerlnk)."
					+ "    BIND((?longitude - "+ lon +")*(?longitude-"+lon+") +  (?latitude - "+ lat +")*(?latitude - "+ lat +")  as ?distance)"
					+ "  }"
					+ "}"
					+"ORDER BY ASC(?distance)";
					if(offset!=null && limit !=null){
						query = query + "OFFSET "+offset+" LIMIT"+limit;
				}

			results = QueryExecutionFactory.sparqlService(dbUri,query).execConstruct();
		}
		else if(city!=null){
			query = "PREFIX schema: <http://schema.org/>\n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "CONSTRUCT {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:makesOffer ?offerlnk;"
					+ "    schema:name ?name;"
					+ "}"
					+ "WHERE {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:name ?name;"
					+ "	   schema:address ?address."
					+ "  ?address schema:addressLocality ?locality."
					+ "  FILTER (REGEX (?locality,\""+city+"\"))"
					+ "  OPTIONAL {"
					+ "    ?hotel schema:makesOffer ?offer."
					+ "    ?offer schema:serialNumber ?serialNumber."
					+ "    BIND(IRI(CONCAT(\"" + urlprefix + "/offers/\", ?serialNumber)) AS ?offerlnk)."
					+ "  }"
					+ "}";
			if(offset!=null && limit !=null){
				query = query + "OFFSET "+offset+" LIMIT"+limit;
			}
			results = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();
		}
		else if (lat!=null || lon!=null || radius!=null || city!=null || (limit!=null && offset==null) || (limit==null && offset!=null)){
			return Response.status(422).entity("The request was well-formed but was unable to be followed due to semantic errors - Unexpected parameters combination").build();
		}
		else{
			query = "PREFIX schema: <http://schema.org/>\n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "CONSTRUCT {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:makesOffer ?offerlnk;"
					+ "    schema:name ?name;"
					+ "}"
					+ "WHERE {"
					+ "  ?hotel rdf:type schema:LodgingBusiness;"
					+ "    schema:name ?name;"
					+ "	   schema:address ?address."
					+ "  ?address schema:addressLocality ?locality."
					+ "  OPTIONAL {"
					+ "    ?hotel schema:makesOffer ?offer."
					+ "    ?offer schema:serialNumber ?serialNumber."
					+ "    BIND(IRI(CONCAT(\"" + urlprefix + "/offers/\", ?serialNumber)) AS ?offerlnk)."
					+ "  }"
					+ "}";
			if(offset!=null && limit !=null){
				query = query + "OFFSET "+offset+" LIMIT"+limit;
			}
			results = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();
		}
		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("hotels/{id}")
	public Response hotel(@PathParam("id") String id){
		Model results = QueryExecutionFactory.sparqlService(dbUri, ""
				+ "PREFIX schema: <http://schema.org/>\n"
				+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
				+ "DESCRIBE ?subject WHERE {\n"
				+ "  ?subject rdf:type schema:LodgingBusiness;\n"
				+ "    schema:branchCode \"" + id + "\"."
				+ " "
				+ "}"
				).execDescribe();
		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
	}
}
