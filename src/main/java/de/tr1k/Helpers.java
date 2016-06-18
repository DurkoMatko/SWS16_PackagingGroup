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

  public static String queryFromInnerSelect(String select){
    String query = ""
      + "PREFIX schema: <http://schema.org/>\n"
      + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"
      + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
      + "CONSTRUCT {\n"
      + "  ?s ?p ?o.\n"
      + "  ?img ?imgp ?imgo.\n"
      + "  ?address ?addressp ?addresso.\n"
      + "  ?country ?countryp ?countryo.\n"
      + "  ?price ?pricep ?priceo.\n"
      + "  ?geo ?geop ?geoo.\n"
      //+ "  ?offer ?offerp ?offero.\n"
      //+ "  ?price2 ?price2p ?price2o.\n"
      //+ "  ?image2 ?image2p ?image2o.\n"
      + "}\n"
      + "WHERE {\n"
      + "  {\n"
      +      select
      + "  }\n"
      + "  ?s ?p ?o.\n"
      + "  OPTIONAL { ?s schema:priceSpecification ?price. ?price ?pricep ?priceo. }\n"
      + "  OPTIONAL { ?s schema:address ?address. ?address ?addressp ?addresso. }\n"
      + "  OPTIONAL { ?s schema:address/schema:addressCountry ?country. ?country ?countryp ?countryo. }\n"
      + "  OPTIONAL { ?s schema:image ?img. ?img ?imgp ?imgo. }\n"
      + "  OPTIONAL { ?s schema:geo ?geo. ?geo ?geop ?geoo. }\n"
      // The makesOffer recursion makes my laptop explode
      //+ "  OPTIONAL { ?s schema:makesOffer ?offer. ?offer ?offerp ?offero. }\n"
      //+ "  OPTIONAL { ?s schema:makesOffer/schema:priceSpecification ?price2. ?price2 ?price2p ?price2o. }\n"
      //+ "  OPTIONAL { ?s schema:makesOffer/schema:image ?image2. ?image2 ?image2p ?image2o. }\n"
      + "}\n"
      ;

    return query;
  }

}
