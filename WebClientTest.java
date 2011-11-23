package edu.cornell.georeference;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

public class WebClientTest {
	
	public static void main(String[] args) throws IOException {
		String parameters = "placeName=washington&nearbyPlaces=50,0,10000/0,0,1000000";
		URL url = new URL("http://localhost:8080/GeoreferenceWeb/geosearch?" + parameters);
		URLConnection conn = url.openConnection();
		String text = new Scanner(conn.getInputStream()).useDelimiter("\\Z").next();
		System.out.println(text);
	}

}
