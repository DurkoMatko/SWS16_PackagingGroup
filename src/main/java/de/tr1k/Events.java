package de.tr1k;

import java.io.ByteArrayOutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;

@Path("events")
public class Events {
		private String urlprefix = "http://localhost:8080/sws16/ServicePackaging/api";
		private String dbUri = "http://localhost:3030/ds";
		private String updateUri = "http://localhost:3030/ds/update";
		
		@GET
		@Produces(MediaType.APPLICATION_JSON)
		public Response events(@QueryParam("lon") String lon,
							   @QueryParam("lat") String lat,
							   @QueryParam("radius") String radius,
							   @QueryParam("startDate") String startDate,
							   @QueryParam("endDate") String endDate,
							   @QueryParam("offset") String offset,
							   @QueryParam("limit") String limit){

			Model results = null;
			String radiusQuery="";
			String startDateQuery = "";
			String endDateQuery = "";
			if(radius!=null && lon!=null && lat!=null){
				double latLongDelta[] =Helpers.radiusToLonLat(radius, lat);
				radiusQuery = "  FILTER (?longitude - "+ lon +"<" + String.valueOf(latLongDelta[1])+ " && ?longitude -"+lon+" >-" + String.valueOf(latLongDelta[1])+ "&& ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])+ " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ")";
			}
			if(startDate != null){
				startDateQuery ="FILTER (xsd:date(?startDate) > \"" + startDate + "\"^^xsd:date)";
			}
			if(endDate != null){
				endDateQuery ="FILTER (xsd:date(?endDate) < \"" + endDate + "\"^^xsd:date)";
			}
			
			String query = ""
					+ "PREFIX schema: <http://schema.org/>\n"
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
					+ "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+ "CONSTRUCT {"
					+ "  ?event rdf:type schema:Event;"
					+ "    schema:name ?name;"
					+ "	   schema:geo ?geo."
					+ "	 ?geo schema:longitude ?longitude;"
					+ "		schema:latitude ?latitude."
					+ "}"
					+ "WHERE {"
					+ "  ?event rdf:type schema:Event;"
					+ "    schema:name ?name;"
					+ "	   schema:geo ?geo;"
					+ "    schema:startDate ?startDate;"
					+ "    schema:endDate ?endDate."
					+ "	 ?geo schema:longitude ?longitude;"
					+ "		schema:latitude ?latitude."
					+ "  " 
					+ radiusQuery
					+ startDateQuery 
					+ endDateQuery;
					query = query + "}";
		
		results = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();
		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
		}
}