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
		@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
		public Response events(@QueryParam("lon") String lon,
							   @QueryParam("lat") String lat,
							   @QueryParam("radius") String radius,
							   @QueryParam("startDate") String startDate,
							   @QueryParam("endDate") String endDate,
							   @QueryParam("offset") String offset,
							   @QueryParam("limit") String limit,
							   @QueryParam("type") String type,
							   @QueryParam("reverse") String reverse){

			Model results = null;
			String radiusSubQ="";
			String startDateSubQ = "";
			String endDateSubQ = "";
			String orderSubQ="";
			String bindDistSubQ="";
			String limitSubQ="";
			String offsetSubQ="";
			String typeSubQ="";
			if(radius!=null && lon!=null && lat!=null){
				double latLongDelta[] =Helpers.radiusToLonLat(radius, lat);
				radiusSubQ = "schema:geo ?geo."
					      + "  ?geo schema:latitude ?latitude;"
					      + "       schema:longitude ?longitude."
						  + "  FILTER (?longitude - "+ lon +"<" + String.valueOf(latLongDelta[1])+ " && ?longitude -"+lon+" >-" + String.valueOf(latLongDelta[1])+ "&& ?latitude - "+ lat +" > -" + String.valueOf(latLongDelta[0])+ " && ?latitude- "+ lat +"<"+String.valueOf(latLongDelta[0])+ ")";
			}
			if(type!=null) {
				typeSubQ += "?s rdf:type schema:" + type + ";\n";
			}
			else {
				typeSubQ += "?s rdf:type schema:Event;\n";
			}
			
			if(startDate != null){
				startDateSubQ ="FILTER (xsd:date(?startDate) > \"" + startDate + "\"^^xsd:date)";
			}
			if(endDate != null){
				endDateSubQ ="FILTER (xsd:date(?endDate) < \"" + endDate + "\"^^xsd:date)";
			}
			if(lon!=null && lat!=null){
				bindDistSubQ = "   BIND((?longitude - "+ lon +")*(?longitude-"+lon+") +  (?latitude - "+ lat +")*(?latitude - "+ lat +")  as ?distance)";
				if(reverse=="false" || reverse==null)
					orderSubQ = "ORDER BY ASC(?distance)";
				else
					orderSubQ = "ORDER BY DESC(?distance)";
			}
			if(limit!=null){
				limitSubQ="LIMIT "+limit;
			}
			else{
				limitSubQ="LIMIT "+20;
			}
			if(offset!=null){
				offsetSubQ="OFFSET "+offset;
			}
			// Build query
		    String select = ""
		      + "    SELECT ?s WHERE {\n"
		      +        typeSubQ
		      +        radiusSubQ
		      +        bindDistSubQ
		      +        startDateSubQ
		      +        endDateSubQ
		      + "    }\n"
		      +      limitSubQ
		      +      offsetSubQ
		      ;
		    
		    String query = Helpers.queryFromInnerSelect(select);
		
		results = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();
		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
		return Response.status(200).entity(outputStream.toString()).build();
		}
}