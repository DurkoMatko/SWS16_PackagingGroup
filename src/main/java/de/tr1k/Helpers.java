package de.tr1k;

import java.io.ByteArrayOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;

public class Helpers{
	private static String updateUri = "http://localhost:3030/ds/update";

	/*    Help private methods  */

	public static double[] radiusToLonLat(String radius,String lat){
		//general approximate transformation
		double latDelta = Double.parseDouble(radius)/110.574;
		double longDelta = Double.parseDouble(radius)/(111.3*Math.cos(Math.toRadians(Double.parseDouble(lat)))); //1 deg = 111.320*cos(latitude) km

		return new double[] {latDelta, longDelta};
	}

	public static void executeUpdateQuery(String query){
		UpdateRequest ur = UpdateFactory.create(query, updateUri);
        UpdateProcessor up = UpdateExecutionFactory.createRemoteForm(ur, updateUri);
        up.execute();
	}

	public static ByteArrayOutputStream modelToJsonLD(Model model){
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		model.write(outputStream, "JSON-LD");
		return outputStream;
	}

}
