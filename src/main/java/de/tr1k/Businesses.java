package de.tr1k;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.InvalidParameterException;
import java.util.Calendar;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;

@Path("businesses")
public class Businesses {

	private static final String dbUri = "http://localhost:3030/ds";
	private static final String weatherApi = "http://api.openweathermap.org/data/2.5/weather";
	private static final String weatherApiKey = "cc9ec62379e78f43a7d6a0a57be412df";
	private static final String weatherNs = "http://purl.org/ns/meteo#";

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	public Response businessList(
			@QueryParam("lon") String lon,
			@QueryParam("lat") String lat,
			@QueryParam("radius") String radius,
			@QueryParam("city") String city,
			@QueryParam("order") String order,
			@QueryParam("type") String type,
			@QueryParam("reverse") String reverse,
			@QueryParam("offset") String offset,
			@QueryParam("limit") String limit) {

		// Location ?
		boolean geoGiven = lat != null && lon != null;
		String qBindDistance = "";
		if (geoGiven) {
			qBindDistance = qBindDistance
					+ "?s schema:geo/schema:longitude ?slon.\n"
					+ "?s schema:geo/schema:latitude ?slat.\n"
					+ "BIND(" + lat + " AS ?lat).\n"
					+ "BIND(" + lon + " AS ?lon).\n"
					+ "BIND((?lon-?slon) AS ?a).\n"
					+ "BIND((?lat-?slat) AS ?b).\n"
					+ "BIND(?a*?a + ?b*?b AS ?distance).\n";
		}

		// Filtering
		String qFiltering = "";
		if (type != null)
			qFiltering += "?s rdf:type schema:" + type + ".\n";

		if (geoGiven && radius != null) {
			double dRadius = Double.parseDouble(radius);
			double dRSquared = dRadius * dRadius;
			qFiltering += "FILTER (?distance < " + String.valueOf(dRSquared) + ").\n";
		}

		if (city != null) {
			qFiltering += "?s schema:address/schema:addressLocality ?locality.\n";
			qFiltering += "FILTER (REGEX (?locality,\"" + city + "\"))\n";
		}

		// Ordering
		String qOrderBy = "ORDER BY ";
		if (reverse != null && order.equals("true"))
			qOrderBy += "DESC";
		else
			qOrderBy += "ASC";
		if (order != null) {
			if (order.equals("branchCode"))
				qOrderBy += "(?branchCode)";
			if (order.equals("distance") && geoGiven)
				qOrderBy += "(?distance)";
		} else if (geoGiven) {
			qOrderBy += "(?distance)";
		} else {
			qOrderBy += "(?branchCode)";
		}
		qOrderBy += "\n";

		// Pagination
		String qPagination = "";
		if (offset != null)
			qPagination += "OFFSET " + offset + " ";
		if (limit != null)
			qPagination += "LIMIT " + limit;
		else
			qPagination += "LIMIT 5";

		// Build query
		String select = ""
				+ "    SELECT ?s WHERE {\n"
				+ "      ?s rdf:type schema:LocalBusiness.\n"
				+ "      ?s schema:branchCode ?branchCode.\n"
				+ qFiltering
				+ qBindDistance
				+ "    }\n"
				+ qOrderBy
				+ qPagination;

		String query = Helpers.queryFromInnerSelect(select);

		Model results = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();
		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(results);
		return Response.status(200).entity(new String(outputStream.toByteArray(), Charset.forName("UTF-8"))).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
	@Path("{branchCode}")
	public Response businessDetail(
			@PathParam("branchCode") String branchCode) {

		// Build query
		String select = ""
				+ "    SELECT ?s WHERE {\n"
				+ "      ?s rdf:type schema:LocalBusiness.\n"
				+ "      ?s schema:branchCode \"" + branchCode + "\".\n"
				+ "    }\n";

		String query = Helpers.queryFromInnerSelect(select);

		Model model = QueryExecutionFactory.sparqlService(dbUri, query).execConstruct();

		// Attempt to get weather information from external web service
		try {
			addWeatherInfo(model);
		} catch (Exception ioe) {
			// TODO: change to IOException
			System.err.println("Failed to get weather information. Exception: " + ioe);
			// TODO: remove printing stack trace
			ioe.printStackTrace();
		}

		ByteArrayOutputStream outputStream = Helpers.modelToJsonLD(model);
		return Response.status(200).entity(outputStream.toString()).build();
	}

	private static void addWeatherInfo(Model model) throws IOException {

		// get coordinations
		Resource root = null;
		Double lat = null;
		Double lon = null;

		StmtIterator iter = model.listStatements();

		while (iter.hasNext()) {
			Statement stmt = iter.nextStatement(); // get next statement
			Property predicate = stmt.getPredicate(); // get the predicate
			RDFNode object = stmt.getObject(); // get the object

			if (predicate.toString().equals("http://schema.org/latitude"))
				lat = object.asLiteral().getDouble();

			if (predicate.toString().equals("http://schema.org/longitude"))
				lon = object.asLiteral().getDouble();

			if (predicate.toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
					&& object.toString().equals("http://schema.org/Place"))
				root = stmt.getSubject();
		}

		// get weather data
		JsonObject obj = getWeatherInfo(lat, lon);

		Double temperature = obj.get("main").getAsObject().get("temp").getAsNumber().value().doubleValue() - 273.15;
		Double humidity = obj.get("main").getAsObject().get("humidity").getAsNumber().value().doubleValue();
		// Double precipitation =
		// obj.get("rain").getAsObject().get("3h").getAsNumber().value().doubleValue();
		Double pressure = obj.get("main").getAsObject().get("pressure").getAsNumber().value().doubleValue();
		Double wind = obj.get("wind").getAsObject().get("speed").getAsNumber().value().doubleValue();
		Double clouds = obj.get("clouds").getAsObject().get("all").getAsNumber().value().doubleValue();

		// create weather resource and add it as a property into the root
		Resource rForecast = model.createResource();
		Property pForecast = model.createProperty(weatherNs + "forecast");
		root.addProperty(pForecast, rForecast);

		// time predicted
		Property pPredicted = model.createProperty(weatherNs + "predicted");
		rForecast.addLiteral(pPredicted, new XSDDateTime(Calendar.getInstance()));

		// temperature
		Resource rTemperature = model.createResource();
		Property pTemperature = model.createProperty(weatherNs + "temperature");
		rForecast.addProperty(pTemperature, rTemperature);

		Property pCelsius = model.createProperty(weatherNs + "celsius");
		rTemperature.addLiteral(pCelsius, new BigDecimal(temperature).setScale(2, RoundingMode.CEILING));

		// humidity
		Resource rHumidity = model.createResource();
		Property pHumidity = model.createProperty(weatherNs + "humidity");
		rForecast.addProperty(pHumidity, rHumidity);

		Property pHumPercent = model.createProperty(weatherNs + "percent");
		rHumidity.addLiteral(pHumPercent, new BigDecimal(humidity).setScale(2, RoundingMode.CEILING));

		// TODO: precipitation
		// precipitation
		// Resource rPrecipitation = model.createResource();
		// Property pPrecipitation = model.createProperty(weatherNs +
		// "precipitation");
		// rForecast.addProperty(pPrecipitation, rPrecipitation);

		// Property pInches = model.createProperty(weatherNs + "inches");
		// rPrecipitation.addLiteral(pInches, new BigDecimal(precipitation));

		// cloudCover
		Resource rCloudCover = model.createResource();
		Property pCloudCover = model.createProperty(weatherNs + "cloudCover");
		rForecast.addProperty(pCloudCover, rCloudCover);

		Property pCldPercent = model.createProperty(weatherNs + "percent");
		rCloudCover.addLiteral(pCldPercent, new BigDecimal(clouds).setScale(2, RoundingMode.CEILING));

		// pressure
		Resource rPressure = model.createResource();
		Property pPressure = model.createProperty(weatherNs + "pressure");
		rForecast.addProperty(pPressure, rPressure);

		Property pMillibar = model.createProperty(weatherNs + "millibar");
		rPressure.addLiteral(pMillibar, new BigDecimal(pressure).setScale(2, RoundingMode.CEILING));

		// wind
		Resource rWind = model.createResource();
		Property pWind = model.createProperty(weatherNs + "wind");
		rForecast.addProperty(pWind, rWind);

		Property pMetresPerSecond = model.createProperty(weatherNs + "metresPerSecond");
		rWind.addLiteral(pMetresPerSecond, new BigDecimal(wind).setScale(2, RoundingMode.CEILING));

		model.setNsPrefix("meteo", weatherNs);
	}

	public static JsonObject getWeatherInfo(Double lat, Double lon) throws IOException {

		if (lat == null || lon == null) {
			throw new InvalidParameterException("Missing coordination.");
		}

		JsonObject obj;

		URL url = new URL(weatherApi + "?lat=" + lat + "&lon=" + lon + "&appid=" + weatherApiKey);

		// TODO: remove later
		System.out.println("Requesting weather data from: " + url.toString());

		URLConnection urlc = url.openConnection();

		BufferedReader bfr = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

		obj = JSON.parse(bfr.readLine());

		if (!obj.get("cod").toString().equals("200"))
			throw new IOException("Weather request failed with html code 200");

		return obj;
	}
}
